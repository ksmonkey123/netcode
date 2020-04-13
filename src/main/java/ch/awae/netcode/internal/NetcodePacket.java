package ch.awae.netcode.internal;

import java.io.Serializable;

public interface NetcodePacket extends Serializable {

    String getDestinationId();

}
