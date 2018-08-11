package ch.awae.netcode;

/**
 * Exception indicating that a connection with the server could not be
 * established.
 * 
 * @since netcode 0.1.0
 * @author Andreas Wälchli
 */
public class ConnectionException extends Exception {

	private static final long serialVersionUID = 1L;

	public ConnectionException(String message) {
		super(message);
	}

}
