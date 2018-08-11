package ch.awae.netcode;

public class DuplicateUserIdException extends ConnectionException {

	public DuplicateUserIdException(String message) {
		super(message);
	}

	private static final long serialVersionUID = 1L;

}
