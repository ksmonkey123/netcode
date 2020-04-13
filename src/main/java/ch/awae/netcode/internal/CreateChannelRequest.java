package ch.awae.netcode.internal;

import ch.awae.netcode.client.ChannelFeatures;

import java.io.Serializable;

public interface CreateChannelRequest extends Serializable {
    String getUserId();
    ChannelFeatures getFeatures();
}
