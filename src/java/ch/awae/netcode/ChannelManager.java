package ch.awae.netcode;

import java.io.IOException;

interface ChannelManager {

	void closeAll();

	Channel getChannel(String appId, String channelId);

	Channel createChannel(String appId, ChannelConfiguration config);

	void closeChannel(String appId, String channelId) throws IOException;

}
