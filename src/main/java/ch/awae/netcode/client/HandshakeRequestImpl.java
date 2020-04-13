package ch.awae.netcode.client;

import ch.awae.netcode.internal.HandshakeRequest;

class HandshakeRequestImpl implements HandshakeRequest {
    private final String appId;

    HandshakeRequestImpl(String appId) {
        this.appId = appId;
    }

    @Override
    public String getAppId() {
        return appId;
    }
}
