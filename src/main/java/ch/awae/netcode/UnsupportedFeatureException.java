package ch.awae.netcode;

/**
 * Exception indicating that a required feature is not supported by the server.
 * 
 * @since netcode 0.2.0
 * @author Andreas Wälchli
 */
public class UnsupportedFeatureException extends ConnectionException {

	public UnsupportedFeatureException(String message) {
		super(message);
	}

	private static final long serialVersionUID = 1L;

}
