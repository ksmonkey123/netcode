package ch.awae.netcode;

/**
 * Exception indicating the channel creation / joining request provided by the
 * client was invalid.
 * 
 * @since netcode 0.2.0
 * @author Andreas Wälchli
 */
public class InvalidRequestException extends ConnectionException {

	public InvalidRequestException(String message) {
		super(message);
	}

	private static final long serialVersionUID = 1L;

}
