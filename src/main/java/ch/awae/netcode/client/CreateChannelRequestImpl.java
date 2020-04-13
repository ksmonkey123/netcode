package ch.awae.netcode.client;

import ch.awae.netcode.internal.CreateChannelRequest;

class CreateChannelRequestImpl implements CreateChannelRequest {

    private final String userId;
    private final ChannelFeatures features;

    CreateChannelRequestImpl(String userId, ChannelFeatures features) {
        this.userId = userId;
        this.features = features;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public ChannelFeatures getFeatures() {
        return features;
    }
}
