package ch.awae.netcode.server;

import ch.awae.netcode.internal.FullChannelInformation;

class ChannelInformationImpl implements FullChannelInformation {

    private final String[] users;
    private final String channelId;
    private final int clientLimit;

    ChannelInformationImpl(String[] users, String channelId, int clientLimit) {
        this.users = users;
        this.channelId = channelId;
        this.clientLimit = clientLimit;
    }

    @Override
    public String[] getUsers() {
        return users;
    }

    @Override
    public String getChannelId() {
        return channelId;
    }

    @Override
    public int getClientLimit() {
        return clientLimit;
    }
}
