package ch.awae.netcode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;

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
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		});
	}

	Channel getChannel(String appId, String channelId) throws ConnectionException {
		if (!appIdValidator.test(appId))
			throw new InvalidAppIdException("invalid application id: '" + appId + "'");
		return channels.get().get(appId + "/" + channelId);
	}

	Channel createChannel(String appId, ChannelConfiguration config, String creator) throws ConnectionException {
		if (!appIdValidator.test(appId))
			throw new InvalidAppIdException("invalid application id: '" + appId + "'");
		Channel c;
		while (true) {
			String id = channelIdProvider.get();
			config.setChannelId(id);
			c = new Channel(appId, config, this, creator);
			if (channels.get().putIfAbsent(appId + "/" + id, c) == null)
				break;
		}
		return c;
	}

	List<ChannelInformation> getPublicChannels(String appId) throws InvalidAppIdException {
		if (!appIdValidator.test(appId))
			throw new InvalidAppIdException("invalid application id: '" + appId + "'");
		List<ChannelInformation> list = new ArrayList<>();
		for (Entry<String, Channel> entry : channels.get().entrySet()) {
			if (!entry.getKey().startsWith(appId))
				continue;
			Channel c = entry.getValue();
			ChannelInformation info = c.getInfo();
			if (info.getConfiguration().isPublicChannel() && !c.isFull())
				list.add(info);
		}
		return list;
	}

	void closeChannel(String appId, String channelId) throws IOException, InterruptedException {
		Channel channel = channels.get().remove(appId + "/" + channelId);
		if (channel != null)
			channel.close();
	}

}
