package ch.awae.netcode.client;

import java.io.Serializable;

public interface ChannelInformation extends Serializable {
    String getChannelId();
    int getClientLimit();
}
