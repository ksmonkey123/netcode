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
import lombok.val;

@Getter
public final class NetcodeServerFactory {

	private final SocketMode socketMode;
	private final SecurityMode securityMode;
	private int maxClients = 50;
	private Predicate<String> appIdValidator = s -> true;
	private Supplier<String> channelIdProvider = new RandomStringGenerator(6);
	@Getter(AccessLevel.NONE)
	private List<Consumer<ServerSocket>> afterBind = new ArrayList<>();
	private final int port;

	/**
	 * Creates a new factory instance
	 */
	public NetcodeServerFactory(int port, SocketMode socketMode, SecurityMode securityMode) {
		Objects.requireNonNull(socketMode, "socketMode may not be null");
		Objects.requireNonNull(securityMode, "securityMode may not be null");
		this.socketMode = socketMode;
		if (socketMode == SocketMode.PLAIN && securityMode != SecurityMode.ANY)
			throw new IllegalArgumentException("incompatible securityMode");
		this.securityMode = securityMode;
		this.port = port;
	}

	public NetcodeServerFactory(int port) {
		this(port, SocketMode.TLS, SecurityMode.ANONYMOUS);
	}

	public void runAfterBind(Consumer<ServerSocket> runner) {
		Objects.requireNonNull(runner);
		afterBind.add(runner);
	}

	public void setMaxClients(int max) {
		if (max <= 0)
			throw new IllegalArgumentException("backlog must be positive");
		this.maxClients = max;
	}

	public NetcodeServer start() throws IOException {
		ServerSocketFactory ssf = (socketMode == SocketMode.PLAIN) ? ServerSocketFactory.getDefault()
				: SSLServerSocketFactory.getDefault();
		ServerSocket ss = ssf.createServerSocket(port, maxClients);
		if (socketMode != SocketMode.PLAIN)
			applySecuritySettings((SSLServerSocket) ss);
		for (val f : afterBind)
			f.accept(ss);
		NetcodeServerImpl ns = new NetcodeServerImpl(ss, appIdValidator, channelIdProvider);
		ns.start();
		return ns;
	}

	public void setAppIdValidator(Predicate<String> appIdValidator) {
		Objects.requireNonNull(appIdValidator);
		this.appIdValidator = appIdValidator;
	}

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