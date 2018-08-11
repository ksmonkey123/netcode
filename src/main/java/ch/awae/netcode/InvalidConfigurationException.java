package ch.awae.netcode;

/**
 * Exception indicating the channel configuration provided by the client was
 * invalid.
 * 
 * @since netcode 0.2.0
 * @author Andreas Wälchli
 */
public class InvalidConfigurationException extends ConnectionException {

	public InvalidConfigurationException(String message) {
		super(message);
	}

	private static final long serialVersionUID = 1L;

}
