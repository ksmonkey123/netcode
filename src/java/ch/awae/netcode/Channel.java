package ch.awae.netcode;

import java.io.IOException;

import ch.awae.netcode.exception.ConnectionException;

interface Channel {

	void join(String userId, ClientHandler handler) throws IOException, ConnectionException;

	void quit(String userId) throws IOException;

	void close() throws IOException;

	void send(MessageImpl msg) throws IOException;

}
