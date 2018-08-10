package ch.awae.netcode;

import java.io.IOException;

import ch.awae.netcode.exception.ConnectionException;

interface ChannelManager {

	void closeAll();

	Channel getChannel(String appId, String channelId) throws ConnectionException;

	Channel createChannel(String appId, ChannelConfiguration config) throws ConnectionException;

	void closeChannel(String appId, String channelId) throws IOException;

}
