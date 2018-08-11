package ch.awae.netcode;

/**
 * Exception indicating that there does not exist a channel with the provided
 * channel id for the provided application id.
 * 
 * @since netcode 0.2.0
 * @author Andreas Wälchli
 */
public class InvalidChannelIdException extends ConnectionException {

	public InvalidChannelIdException(String message) {
		super(message);
	}

	private static final long serialVersionUID = 1L;

}
