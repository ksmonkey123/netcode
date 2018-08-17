package ch.awae.netcode;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.val;

/**
 * Factory for creating Netcode clients.
 * 
 * By default clients are created in synchronous mode, without a
 * {@link ChannelEventHandler} and using an anonymous TLS cipher.
 * 
 * Every client provides an application id to the Netcode server. This
 * application id is used to ensure that only compatible clients join a single
 * channel. The server may also choose to accept only certain application ids.
 * 
 * Most methods return the object itself to allow easy method chaining.
 * 
 * @since netcode 0.1.0
 * @author Andreas Wälchli
 * @see NetcodeClient
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class NetcodeClientFactory {

	private SocketMode socketMode = SocketMode.TLS;
	private SecurityMode securityMode = SecurityMode.ANONYMOUS;

	@Getter(AccessLevel.NONE)
	private List<Consumer<Socket>> afterBind = new ArrayList<>();

	private MessageHandler messageHandler;
	private ChannelEventHandler eventHandler;
	private ClientQuestionHandler questionHandler;
	private long timeout = 60000;

	private final String appId;
	private final String host;
	private final int port;

	/**
	 * Creates a new factory with the given host and port information and appId.
	 * 
	 * @param host  the server address. may not be null.
	 * @param port  the port number of the server. must be in the range 0-65535.
	 * @param appId the application id to use for this client. may not be null.
	 */
	public NetcodeClientFactory(String host, int port, String appId) {
		Objects.requireNonNull(host);
		Objects.requireNonNull(appId);
		if (port < 0 || port > 65535)
			throw new IllegalArgumentException("port " + port + " is outside the legal range (0-65535)");
		this.host = host;
		this.port = port;
		this.appId = appId;
	}

	/**
	 * Adds a function to the post-bind queue.
	 * 
	 * The post-bind queue allows access to the {@link Socket} or {@link SSLSocket}
	 * as soon as it is created (but for SSLSockets before the handshake). This
	 * allows arbitrary modification of the socket configuration. This is especially
	 * useful for SSLSockets where more control over the security configuration may
	 * be desired.
	 * 
	 * If the socket mode is set to {@link SocketMode#PLAIN}, the passed socket will
	 * be of type {@link Socket}, for all other modes it will be a
	 * {@link SSLSocket}.
	 * 
	 * @param runner the runner to add to the post-bind queue. may not be null.
	 */
	public NetcodeClientFactory runAfterBind(Consumer<Socket> runner) {
		Objects.requireNonNull(runner);
		afterBind.add(runner);
		return this;
	}

	/**
	 * Specifies both the socket mode and the security mode to use for new
	 * connections. The socket mode specifies if a plain (unencrypted) connection,
	 * SSL, TLS or both (SSL or TLS) should be used. The security mode specifies for
	 * secured (TLS/SSL) sockets if an anonymous cipher, a certificate-based cipher
	 * (or both) is acceptable. This information is used to negociate a cipher that
	 * is acceptable to both the server and the client.
	 * 
	 * If socketMode is set to {@link SocketMode#PLAIN}, then the securityMode must
	 * be set to {@link SecurityMode#ANY}.
	 * 
	 * @throws IllegalArgumentException an illegal combination has been provided
	 * @throws NullPointerException     any parameter is null
	 */
	public NetcodeClientFactory setMode(SocketMode socketMode, SecurityMode securityMode) {
		Objects.requireNonNull(socketMode);
		Objects.requireNonNull(securityMode);
		if (socketMode == SocketMode.PLAIN && securityMode != SecurityMode.ANY)
			throw new IllegalArgumentException("incompatible securityMode");
		this.socketMode = socketMode;
		this.securityMode = securityMode;
		return this;
	}

	/**
	 * Creates a new Netcode channel. This does not consume this factory and it can
	 * therefore be re-used.
	 * 
	 * @param userId        the userId for this client. may not be null.
	 * @param configuration the channel configuration to use.
	 * @return an initialised client instance.
	 * @throws IOException         an exception occured in the unterlying I/O
	 *                             elements.
	 * @throws ConnectionException a netcode connection could not be established.
	 *                             Usually this indicates that some client data was
	 *                             rejected by the server. See the exception type
	 *                             and message for more information.
	 */
	public NetcodeClient createChannel(String userId, ChannelConfiguration configuration)
			throws IOException, ConnectionException {
		Objects.requireNonNull(userId);
		Objects.requireNonNull(configuration);
		if (userId.isEmpty())
			throw new IllegalArgumentException("userId may not be empty");
		NetcodeClientImpl client = initSocket();
		client.open(new NetcodeHandshakeRequest(appId, null, userId, true, configuration));
		return client;
	}

	/**
	 * Joins an existing Netcode channel. This does not consume this factory and it
	 * can therefore be re-used.
	 * 
	 * @param userId    the userId for this client. may not be null.
	 * @param channelId the id of the channel to join. Channel IDs are generated by
	 *                  the server upon channel generation.
	 * @return an initialised client instance.
	 * @throws IOException         an exception occured in the unterlying I/O
	 *                             elements.
	 * @throws ConnectionException a netcode connection could not be established.
	 *                             Usually this indicates that some client data was
	 *                             rejected by the server. See the exception type
	 *                             and message for more information.
	 */
	public NetcodeClient joinChannel(String userId, String channelId) throws IOException, ConnectionException {
		Objects.requireNonNull(userId);
		Objects.requireNonNull(channelId);
		if (userId.isEmpty())
			throw new IllegalArgumentException("userId may not be empty");
		NetcodeClientImpl client = initSocket();
		client.open(new NetcodeHandshakeRequest(appId, channelId, userId, false, null));
		return client;
	}

	/**
	 * get a list of all public Netcode channels with at least one free slot.
	 * 
	 * @since netcode 2.0.0
	 * @return
	 * @throws IOException
	 * @throws ConnectionException if the server does not support SIMPLE_QUERY (v1
	 *                             servers) or the server has disabled public
	 *                             channels (v2 servers and above)
	 */
	public ChannelInformation[] listPublicChannels() throws IOException, ConnectionException {
		NetcodeClientImpl client = initSocket();
		return (ChannelInformation[]) client.simpleQuery("channel_list:" + appId);
	}

	private NetcodeClientImpl initSocket() throws IOException {
		SocketFactory sf = (socketMode == SocketMode.PLAIN) ? SocketFactory.getDefault()
				: SSLSocketFactory.getDefault();
		Socket s = sf.createSocket(this.host, this.port);
		s.setKeepAlive(true);
		if (socketMode != SocketMode.PLAIN)
			applySecuritySettings((SSLSocket) s);
		for (val f : afterBind)
			f.accept(s);
		if (socketMode != SocketMode.PLAIN)
			((SSLSocket) s).startHandshake();
		return new NetcodeClientImpl(s, messageHandler, eventHandler, questionHandler, timeout);
	}

	private void applySecuritySettings(SSLSocket socket) {
		List<String> ciphers = new ArrayList<>();
		for (val c : socket.getSupportedCipherSuites()) {
			if (socketMode == SocketMode.SSL && !c.startsWith("SSL"))
				continue;
			if (socketMode == SocketMode.TLS && !c.startsWith("TLS"))
				continue;
			if (securityMode == SecurityMode.CERTIFICATE && c.contains("_anon_"))
				continue;
			if (securityMode == SecurityMode.ANONYMOUS && !c.contains("_anon_"))
				continue;
			ciphers.add(c);
		}
		socket.setEnabledCipherSuites(ciphers.toArray(new String[0]));
	}

	/**
	 * Sets a new timeout value to be used for server commands and client questions.
	 * If this is set to 0, the timeout is disabled.
	 * 
	 * @param timeout the new timeout in milliseconds
	 * @throws IllegalArgumentException the timeout is negative
	 * @see NetcodeClient#ask(String, java.io.Serializable)
	 */
	public NetcodeClientFactory setTimeout(long timeout) {
		if (timeout < 0)
			throw new IllegalArgumentException("Timeout may not be negative!");
		this.timeout = timeout;
		return this;
	}

	public NetcodeClientFactory setMessageHandler(MessageHandler messageHandler) {
		this.messageHandler = messageHandler;
		return this;
	}

	public NetcodeClientFactory setEventHandler(ChannelEventHandler eventHandler) {
		this.eventHandler = eventHandler;
		return this;
	}

	public NetcodeClientFactory setQuestionHandler(ClientQuestionHandler questionHandler) {
		this.questionHandler = questionHandler;
		return this;
	}

	public NetcodeClientFactory copy() {
		return new NetcodeClientFactory(socketMode, securityMode, new ArrayList<>(afterBind), messageHandler,
				eventHandler, questionHandler, timeout, appId, host, port);
	}

}
