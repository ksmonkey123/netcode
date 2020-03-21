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

	private int maxClients = 50;
	private Predicate<String> appIdValidator = s -> true;
	private Supplier<String> channelIdProvider = new RandomStringGenerator(6);
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

	public void setMaxClients(int max) {
		if (max <= 0)
			throw new IllegalArgumentException("backlog must be positive");
		this.maxClients = max;
	}

	public NetcodeServer start() throws IOException {
		ServerSocketFactory ssf = ServerSocketFactory.getDefault();
		ServerSocket ss = ssf.createServerSocket(port, maxClients);
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
	 * @param channelIdProvider
	 *            may not be null.
	 */
	public void setChannelIdProvider(Supplier<String> channelIdProvider) {
		Objects.requireNonNull(channelIdProvider);
		this.channelIdProvider = channelIdProvider;
	}

}