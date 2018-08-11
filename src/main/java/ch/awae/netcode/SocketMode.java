package ch.awae.netcode;

/**
 * Defines the mode of operation for the underlying sockets.
 * 
 * The {@link #PLAIN} mode uses a {@link java.net.Socket}, all other modes use
 * different configurations of a {@link javax.net.ssl.SSLSocket}.
 * 
 * @since netcode 0.1.0
 * @author Andreas Wälchli
 */
public enum SocketMode {
	/**
	 * Use a plain {@link java.net.Socket}. If this is used, the
	 * {@link SecurityMode} must be set to {@link SecurityMode#ANY}.
	 */
	PLAIN,

	/**
	 * Uses SSL or TLS
	 */
	SECURE,

	/**
	 * Uses SSL only
	 */
	SSL,

	/**
	 * Uses TLS only
	 */
	TLS

}
