package ch.awae.netcode.internal;

import java.io.Serializable;

public interface JoinChannelRequest extends Serializable {
    String getUserId();
    String getPassword();
    String getChannelId();
}
