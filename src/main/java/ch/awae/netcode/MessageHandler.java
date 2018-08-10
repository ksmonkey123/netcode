package ch.awae.netcode;

@FunctionalInterface
public interface MessageHandler {

	void handleMessage(Message msg);

	default void clientJoined(String userId) {
	}

	default void clientLeft(String userId) {
	}

}
