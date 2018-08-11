package ch.awae.netcode;

/**
 * Exception indicating that a channel cannot be joined because is already full.
 * 
 * @since netcode 0.2.0
 * @author Andreas Wälchli
 */
public class ChannelUserLimitReachedException extends ConnectionException {

	public ChannelUserLimitReachedException(String message) {
		super(message);
	}

	private static final long serialVersionUID = 1L;

}
