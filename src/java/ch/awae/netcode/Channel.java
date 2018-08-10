package ch.awae.netcode;

import java.io.IOException;

interface Channel {

	void join(String userId, ClientHandler handler) throws IOException;

	void quit(String userId) throws IOException;

	void close() throws IOException;

	void send(MessageImpl msg) throws IOException;

}
