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
import lombok.Getter;
import lombok.Setter;
import lombok.val;

@Getter
public final class NetcodeClientFactory {

	@Getter(AccessLevel.NONE)
	private List<Consumer<Socket>> afterBind = new ArrayList<>();
	private SocketMode socketMode = SocketMode.TLS;
	private SecurityMode securityMode = SecurityMode.ANONYMOUS;
	@Setter
	@Getter
	private MessageHandler messageHandler;
	@Setter
	@Getter
	private ChannelEventHandler eventHandler;
	private final String appId;
	private final String host;
	private final int port;

	public NetcodeClientFactory(String host, int port, String appId) {
		Objects.requireNonNull(host);
		Objects.requireNonNull(appId);
		if (port < 0 || port > 65535)
			throw new IllegalArgumentException("port " + port + " is outside the legal range (0-65535)");
		this.host = host;
		this.port = port;
		this.appId = appId;
	}

	public void runAfterBind(Consumer<Socket> runner) {
		Objects.requireNonNull(runner);
		afterBind.add(runner);
	}

	public void setMode(SocketMode socketMode, SecurityMode securityMode) {
		Objects.requireNonNull(socketMode);
		Objects.requireNonNull(securityMode);
		if (socketMode == SocketMode.PLAIN && securityMode != SecurityMode.ANY)
			throw new IllegalArgumentException("incompatible securityMode");
		this.socketMode = socketMode;
		this.securityMode = securityMode;
	}

	public NetcodeClient createChannel(String userId, ChannelConfiguration configuration)
			throws IOException, ConnectionException {
		Objects.requireNonNull(userId);
		Objects.requireNonNull(configuration);
		NetcodeClientImpl client = initSocket();
		client.open(new NetcodeHandshakeRequest(appId, null, userId, true, configuration));
		return client;
	}

	public NetcodeClient joinChannel(String userId, String channelId) throws IOException, ConnectionException {
		Objects.requireNonNull(userId);
		Objects.requireNonNull(channelId);
		NetcodeClientImpl client = initSocket();
		client.open(new NetcodeHandshakeRequest(appId, channelId, userId, false, null));
		return client;
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
		return new NetcodeClientImpl(s, messageHandler, eventHandler);
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

}
