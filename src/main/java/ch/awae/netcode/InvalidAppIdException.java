package ch.awae.netcode;

public class InvalidAppIdException extends ConnectionException {

	public InvalidAppIdException(String message) {
		super(message);
	}

	private static final long serialVersionUID = 1L;

}
