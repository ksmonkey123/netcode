package ch.awae.netcode;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;

final class ChannelManagerImpl implements ChannelManager {

	private final Predicate<String> appIdValidator;
	private final Supplier<String> channelIdProvider;
	private AtomicReference<ConcurrentHashMap<String, Channel>> channels = new AtomicReference<>(
			new ConcurrentHashMap<>());

	public ChannelManagerImpl(Predicate<String> appIdValidator, Supplier<String> channelIdProvider) {
		this.appIdValidator = appIdValidator;
		this.channelIdProvider = channelIdProvider;
	}

	@Override
	public void closeAll() {
		ConcurrentHashMap<String, Channel> oldMap = channels.getAndSet(new ConcurrentHashMap<>());
		oldMap.forEachValue(1000, c -> {
			try {
				c.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	@Override
	public Channel getChannel(String appId, String channelId) {
		if (!appIdValidator.test(appId))
			throw new IllegalArgumentException();
		return channels.get().get(appId + "/" + channelId);
	}

	@Override
	public Channel createChannel(String appId, ChannelConfiguration config) {
		if (!appIdValidator.test(appId))
			throw new IllegalArgumentException();
		Channel c;
		while (true) {
			String id = channelIdProvider.get();
			config.setChannelId(id);
			c = new ChannelImpl(appId, config, this);
			if (channels.get().putIfAbsent(appId + "/" + id, c) == null)
				break;
		}
		return c;
	}

	@Override
	public void closeChannel(String appId, String channelId) throws IOException {
		Channel channel = channels.get().remove(appId + "/" + channelId);
		if (channel != null)
			channel.close();
	}

}
