package ch.awae.netcode;

@FunctionalInterface
public interface MessageHandler {

	void handleMessage(Message msg);


}
