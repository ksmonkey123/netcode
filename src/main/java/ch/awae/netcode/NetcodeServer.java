package ch.awae.netcode;

/**
 * A Netcode server instance.
 * 
 * @since netcode 0.1.0
 * @author Andreas Wälchli
 * @see NetcodeServerFactory
 */
public interface NetcodeServer {

	/**
	 * Terminates the server and disconnects all connected clients.
	 */
	public void close();

}
