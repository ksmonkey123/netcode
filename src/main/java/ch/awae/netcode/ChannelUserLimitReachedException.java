package ch.awae.netcode;

public class ChannelUserLimitReachedException extends ConnectionException {

	public ChannelUserLimitReachedException(String message) {
		super(message);
	}

	private static final long serialVersionUID = 1L;

}
