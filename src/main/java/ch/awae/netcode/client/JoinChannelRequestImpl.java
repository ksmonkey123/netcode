package ch.awae.netcode.client;

import ch.awae.netcode.internal.JoinChannelRequest;

class JoinChannelRequestImpl implements JoinChannelRequest {

    private final String userId;
    private final String password;
    private final String channelId;

    JoinChannelRequestImpl(String userId, String password, String channelId) {
        this.userId = userId;
        this.password = password;
        this.channelId = channelId;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getChannelId() {
        return channelId;
    }
}
