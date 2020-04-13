package ch.awae.netcode.client;

import ch.awae.netcode.internal.NetcodePacket;
import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;
import java.sql.Timestamp;

class NetcodePacketImpl implements NetcodePacket {

    private Timestamp timestamp;
    private String senderId, destinationId;
    private long correlationId;
    private NetcodePacketType type;
    private byte[] payload;

    NetcodePacketImpl(Timestamp timestamp, String senderId, String destinationId, long correlationId, NetcodePacketType type, Serializable payload) {
        this.timestamp = timestamp;
        this.senderId = senderId;
        this.destinationId = destinationId;
        this.correlationId = correlationId;
        this.type = type;
        this.payload = SerializationUtils.serialize(payload);
    }

    Timestamp getTimestamp() {
        return timestamp;
    }

    String getSenderId() {
        return senderId;
    }

    @Override
    public String getDestinationId() {
        return destinationId;
    }

    long getCorrelationId() {
        return correlationId;
    }

    NetcodePacketType getType() {
        return type;
    }

    Serializable getPayload() {
        return SerializationUtils.deserialize(payload);
    }

}
