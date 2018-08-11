package ch.awae.netcode;

/**
 * Exception indicating that a channel could not be joined because a user with
 * the requested userId does already exist.
 * 
 * @since netcode 0.2.0
 * @author Andreas Wälchli
 */
public class DuplicateUserIdException extends ConnectionException {

	public DuplicateUserIdException(String message) {
		super(message);
	}

	private static final long serialVersionUID = 1L;

}
