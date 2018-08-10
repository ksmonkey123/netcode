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
	private MessageHandler messageHandler;
	private final String appId;
	private final String host;
	private final int port;

	public NetcodeClientFactory(String host, int port, String appId) {
		this.host = host;
		this.port = port;
		this.appId = appId;
	}

	public void runAfterBind(Consumer<Socket> runner) {
		afterBind.add(runner);
	}

	public void setMode(SocketMode socketMode, SecurityMode securityMode) {
		if (socketMode == SocketMode.PLAIN && securityMode != SecurityMode.ANY)
			throw new IllegalArgumentException("incompatible securityMode");
		this.socketMode = socketMode;
		this.securityMode = securityMode;
	}

	public NetcodeClient createChannel(String userId, ChannelConfiguration configuration) throws IOException, ConnectionException {
		Objects.requireNonNull(messageHandler);
		NetcodeClient client = initSocket();
		client.open(new NetcodeHandshakeRequest(appId, null, userId, true, configuration));
		return client;
	}

	public NetcodeClient joinChannel(String userId, String channelId) throws IOException, ConnectionException {
		Objects.requireNonNull(messageHandler);
		NetcodeClient client = initSocket();
		client.open(new NetcodeHandshakeRequest(appId, channelId, userId, false, null));
		return client;
	}

	private NetcodeClient initSocket() throws IOException {
		SocketFactory sf = (socketMode == SocketMode.PLAIN) ? SocketFactory.getDefault()
				: SSLSocketFactory.getDefault();
		Socket s = sf.createSocket(this.host, this.port);
		s.setKeepAlive(true);
		if (socketMode != SocketMode.PLAIN) {
			SSLSocket ssls = (SSLSocket) s;
			applySecuritySettings(ssls);
			ssls.startHandshake();
		}
		for (val f : afterBind)
			f.accept(s);
		return new NetcodeClient(s, messageHandler);
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
