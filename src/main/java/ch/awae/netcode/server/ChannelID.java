package ch.awae.netcode.server;

import java.util.Objects;

final class ChannelID {

    private final String appId, channelId;

    ChannelID(String appId, String channelId) {
        this.appId = appId;
        this.channelId = channelId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChannelID channelID = (ChannelID) o;
        return Objects.equals(appId, channelID.appId) &&
                Objects.equals(channelId, channelID.channelId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appId, channelId);
    }

    @Override
    public String toString() {
        return "ChannelID{" +
                "appId='" + appId + '\'' +
                ", channelId='" + channelId + '\'' +
                '}';
    }

    public String getChannelId() {
        return channelId;
    }
}
