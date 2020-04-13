package ch.awae.netcode.internal;

import java.io.Serializable;

public interface HandshakeRequest extends Serializable {
    String getAppId();
}
