package ch.awae.netcode.client;

import java.io.Serializable;
import java.sql.Timestamp;

@FunctionalInterface
public interface MessageHandler {

    void handleMessage(String sender, Timestamp timestamp, Serializable message);

    default void handlePrivateMessage(String sender, Timestamp timestamp, Serializable message) {
        handleMessage(sender, timestamp, message);
    }
}
