package ch.awae.netcode;

import java.io.IOException;

public interface NetcodeClient {

	void disconnect() throws IOException;

	void send(Object payload) throws IOException;

	void sendPrivately(String userId, Object payload) throws IOException;

	String getUserId();

	ChannelConfiguration getChannelConfiguration();
	
	String[] getUsers();
	
	void setMessageHandler(MessageHandler handler);

}
