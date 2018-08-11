package ch.awae.netcode;

public class InvalidChannelIdException extends ConnectionException {

	public InvalidChannelIdException(String message) {
		super(message);
	}

	private static final long serialVersionUID = 1L;

}
