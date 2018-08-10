package ch.awae.netcode;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;

import ch.awae.netcode.exception.ConnectionException;

final class ChannelManager {

	private final Predicate<String> appIdValidator;
	private final Supplier<String> channelIdProvider;
	private AtomicReference<ConcurrentHashMap<String, Channel>> channels = new AtomicReference<>(
			new ConcurrentHashMap<>());

	ChannelManager(Predicate<String> appIdValidator, Supplier<String> channelIdProvider) {
		this.appIdValidator = appIdValidator;
		this.channelIdProvider = channelIdProvider;
	}

	void closeAll() {
		ConcurrentHashMap<String, Channel> oldMap = channels.getAndSet(new ConcurrentHashMap<>());
		oldMap.forEachValue(1000, c -> {
			try {
				c.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	Channel getChannel(String appId, String channelId) throws ConnectionException {
		if (!appIdValidator.test(appId))
			throw new ConnectionException("invalid application id: '" + appId + "'");
		return channels.get().get(appId + "/" + channelId);
	}

	Channel createChannel(String appId, ChannelConfiguration config) throws ConnectionException {
		if (!appIdValidator.test(appId))
			throw new ConnectionException("invalid application id: '" + appId + "'");
		Channel c;
		while (true) {
			String id = channelIdProvider.get();
			config.setChannelId(id);
			c = new Channel(appId, config, this);
			if (channels.get().putIfAbsent(appId + "/" + id, c) == null)
				break;
		}
		return c;
	}

	void closeChannel(String appId, String channelId) throws IOException {
		Channel channel = channels.get().remove(appId + "/" + channelId);
		if (channel != null)
			channel.close();
	}

}
