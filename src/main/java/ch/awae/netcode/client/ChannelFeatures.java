package ch.awae.netcode.client;

import java.io.Serializable;

public class ChannelFeatures implements Serializable {

    private int clientLimit = -1;
    private String password = null;

    public ChannelFeatures() {

    }

    public ChannelFeatures copy() {
        ChannelFeatures clone = new ChannelFeatures();

        clone.clientLimit = clientLimit;
        clone.password = password;

        return clone;
    }
    
    public int getClientLimit() {
        return clientLimit;
    }

    public void setClientLimit(int clientLimit) {
        if (clientLimit != -1 && clientLimit < 2) {
            throw new IllegalArgumentException("client limit must be at least 2 (or -1 to disable limit)");
        }
        this.clientLimit = clientLimit;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "ChannelFeatures{" +
                "clientLimit=" + clientLimit +
                ", password=" + (password != null ? "*******" : "null" ) +
                '}';
    }
}
