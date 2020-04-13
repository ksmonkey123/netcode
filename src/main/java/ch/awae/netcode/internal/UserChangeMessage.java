package ch.awae.netcode.internal;

import java.io.Serializable;

public interface UserChangeMessage extends Serializable {

    String getUser();
    boolean isEntering();

}
