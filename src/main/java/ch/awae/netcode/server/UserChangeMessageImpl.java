package ch.awae.netcode.server;

import ch.awae.netcode.internal.UserChangeMessage;

class UserChangeMessageImpl implements UserChangeMessage {

    private final String user;
    private final boolean entering;

    UserChangeMessageImpl(String user, boolean entering) {
        this.user = user;
        this.entering = entering;
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public boolean isEntering() {
        return entering;
    }
}
