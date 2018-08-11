package ch.awae.netcode;

public interface ChannelEventHandler {

	default void clientJoined(String userId) {
	}

	default void clientLeft(String userId) {
	}
}
