package ch.awae.netcode.server;

import ch.awae.netcode.client.ChannelFeatures;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

class ChannelManager {

    private final static Logger LOG = Logger.getLogger(ChannelManager.class.getName());

    private final Map<ChannelID, Channel> channels = new HashMap<>();

    private Semaphore shutdownSemaphore = null;

    synchronized Channel createChannel(String appId, ChannelFeatures features) {
        ChannelID id;
        do {
            id = new ChannelID(appId, createChannelId());
        } while (channels.containsKey(id));
        Channel channel = new Channel(id, features, this);
        channels.put(id, channel);
        LOG.info("created new channel: " + id + " " + features);
        return channel;
    }

    private String createChannelId() {
        String hexString = Long.toHexString(System.currentTimeMillis());
        if (hexString.length() > 8) {
            return hexString.substring(hexString.length() - 8);
        } else {
            return hexString;
        }
    }

    synchronized Channel getChannel(String appId, String channelId) {
        return channels.get(new ChannelID(appId, channelId));
    }

    synchronized void removeChannel(ChannelID channelId) {
        LOG.info("removed channel " + channelId);
        channels.remove(channelId);

        if (shutdownSemaphore != null && channels.isEmpty()) {
            shutdownSemaphore.release();
        }
    }

    void shutdownChannels() throws InterruptedException {
        synchronized (this) {
            shutdownSemaphore = new Semaphore(0);
            Channel[] channels = this.channels.values().toArray(new Channel[0]);
            for (Channel channel : channels) {
                channel.shutdown();
            }
        }
        shutdownSemaphore.acquire();
    }
}
