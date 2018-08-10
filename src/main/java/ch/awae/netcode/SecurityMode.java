package ch.awae.netcode;

/**
 * Defines the security constraints for the selected {@link SocketMode}.
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
