package ch.awae.netcode;

public interface MessageHandler {

	void handleMessage(Message msg);
	
	void clientJoined(String userId);
	
	void clientLeft(String userId);
	
}
