package ch.awae.netcode;

/**
 * Defines the security constraints for the selected {@link SocketMode}.
 * 
 * @since netcode 0.1.0
 * @author Andreas Wälchli
 */
public enum SecurityMode {

	/**
	 * Use any supported cipher
	 */
	ANY,

	/**
	 * Use anonymous ciphers only
	 */
	ANONYMOUS,

	/**
	 * Use certificate based ciphers only
	 */
	CERTIFICATE

}
