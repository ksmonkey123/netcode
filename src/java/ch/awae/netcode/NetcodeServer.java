package ch.awae.netcode;

public interface NetcodeServer {

	/**
	 * Terminates the server and disconnects all connected clients.
	 */
	public void close();

}
