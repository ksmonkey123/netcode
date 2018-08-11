package ch.awae.netcode;

/**
 * Exception indicating that the connection could not be established due to an
 * incompatibility between the client and the server version.
 * 
 * @since netcode 0.2.0
 * @author Andreas Wälchli
 */
public class IncompatibleServerException extends ConnectionException {

	public IncompatibleServerException(String message) {
		super(message);
	}

	private static final long serialVersionUID = 1L;

}
