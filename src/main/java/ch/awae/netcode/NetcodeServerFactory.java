package ch.awae.netcode;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.val;

/**
 * Factory for creating netcode server instances.
 * 
 * The following configuration is assumed by default:
 * 
 * <ul>
 * <li>socketMode: TLS</li>
 * <li>securityMode: ANONYMOUS</li>
 * <li>appId validation: appIds of the pattern [a-zA-Z0-9_]+ are accepted</li>
 * <li>channelId generation: random String of the pattern [a-zA-Z0-9]{6}</li>
 * <li>enable public channels: true</li>
 * <li>enable server commands: true</li>
 * </ul>
 * 
 * @since netcode 0.1.0
 * @author Andreas Wälchli
 * @see NetcodeServer
 * @see RandomStringGenerator
 */
@Getter
public final class NetcodeServerFactory {

	private SocketMode socketMode = SocketMode.TLS;
	private SecurityMode securityMode = SecurityMode.ANONYMOUS;
	private int maxClients = 50;
	private Predicate<String> appIdValidator = s -> true;
	private Supplier<String> channelIdProvider = new RandomStringGenerator(6);
	@Getter(AccessLevel.NONE)
	private List<Consumer<ServerSocket>> afterBind = new ArrayList<>();
	private final int port;
	private @Setter boolean enablePublicChannels = true;
	private @Setter boolean enableServerCommands = true;

	/**
	 * Creates a new factory instance with a specified port number. must be in
	 * the range 0-65535.
	 */
	public NetcodeServerFactory(int port) {
		if (port < 0 || port > 65535)
			throw new IllegalArgumentException("port " + port + " is outside the legal range (0-65535)");
		this.port = port;
	}

	/**
	 * Specifies both the socket mode and the security mode to use for new
	 * connections. The socket mode specifies if a plain (unencrypted)
	 * connection, SSL, TLS or both (SSL or TLS) should be used. The security
	 * mode specifies for secured (TLS/SSL) sockets if an anonymous cipher, a
	 * certificate-based cipher (or both) is acceptable. This information is
	 * used to negociate a cipher that is acceptable to both the server and the
	 * client.
	 * 
	 * If socketMode is set to {@link SocketMode#PLAIN}, then the securityMode
	 * must be set to {@link SecurityMode#ANY}.
	 * 
	 * @throws IllegalArgumentException
	 *             an illegal combination has been provided
	 * @throws NullPointerException
	 *             any parameter is null
	 */
	public void setMode(SocketMode socketMode, SecurityMode securityMode) {
		Objects.requireNonNull(socketMode, "socketMode may not be null");
		Objects.requireNonNull(securityMode, "securityMode may not be null");
		this.socketMode = socketMode;
		if (socketMode == SocketMode.PLAIN && securityMode != SecurityMode.ANY)
			throw new IllegalArgumentException("incompatible securityMode");
		this.securityMode = securityMode;
	}

	/**
	 * Adds a function to the post-bind queue.
	 * 
	 * The post-bind queue allows access to the {@link ServerSocket} or
	 * {@link SSLServerSocket} as soon as it is created (but for SSLSockets
	 * before the handshake). This allows arbitrary modification of the socket
	 * configuration. This is especially useful for SSLServerSockets where more
	 * control over the security configuration may be desired.
	 * 
	 * If the socket mode is set to {@link SocketMode#PLAIN}, the passed socket
	 * will be of type {@link ServerSocket}, for all other modes it will be a
	 * {@link SSLServerSocket}.
	 * 
	 * @param runner
	 *            the runner to add to the post-bind queue. may not be null.
	 */
	public void runAfterBind(Consumer<ServerSocket> runner) {
		Objects.requireNonNull(runner);
		afterBind.add(runner);
	}

	/**
	 * specifies the connection backlog.
	 * 
	 * @see ServerSocket#ServerSocket(int, int)
	 */
	public void setMaxClients(int max) {
		if (max <= 0)
			throw new IllegalArgumentException("backlog must be positive");
		this.maxClients = max;
	}

	/**
	 * Start the server
	 */
	public NetcodeServer start() throws IOException {
		ServerSocketFactory ssf = (socketMode == SocketMode.PLAIN) ? ServerSocketFactory.getDefault()
				: SSLServerSocketFactory.getDefault();
		ServerSocket ss = ssf.createServerSocket(port, maxClients);
		if (socketMode != SocketMode.PLAIN)
			applySecuritySettings((SSLServerSocket) ss);
		for (val f : afterBind)
			f.accept(ss);
		ServerCapabilities features = new ServerCapabilities(enablePublicChannels, enableServerCommands);
		NetcodeServerImpl ns = new NetcodeServerImpl(ss, appIdValidator, channelIdProvider, features);
		ns.start();
		return ns;
	}

	/**
	 * Defines an application id validator.
	 * 
	 * this validator will be used whenever a client connects to decide wether
	 * or not to reject that client.
	 * 
	 * @param appIdValidator
	 *            may not be null.
	 */
	public void setAppIdValidator(Predicate<String> appIdValidator) {
		Objects.requireNonNull(appIdValidator);
		this.appIdValidator = appIdValidator;
	}

	/**
	 * Defines a generator for channel ids.
	 * 
	 * this generator will be used whenever a channel gets created.
	 * 
	 * @param appIdValidator
	 *            may not be null.
	 */
	public void setChannelIdProvider(Supplier<String> channelIdProvider) {
		Objects.requireNonNull(channelIdProvider);
		this.channelIdProvider = channelIdProvider;
	}

	private void applySecuritySettings(SSLServerSocket ss) {
		List<String> ciphers = new ArrayList<>();
		for (val c : ss.getSupportedCipherSuites()) {
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
		ss.setEnabledCipherSuites(ciphers.toArray(new String[0]));
	}

}