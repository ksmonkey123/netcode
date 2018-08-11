package ch.awae.netcode;

/**
 * Exception indicating that the provided application id was rejected by the
 * server.
 * 
 * @since netcode 0.2.0
 * @author Andreas Wälchli
 */
public class InvalidAppIdException extends ConnectionException {

	public InvalidAppIdException(String message) {
		super(message);
	}

	private static final long serialVersionUID = 1L;

}
