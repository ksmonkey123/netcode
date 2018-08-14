package ch.awae.netcode;

/**
 * Interface for asynchronously handling messages.
 * 
 * @since netcode 0.1.0
 * @author Andreas Wälchli
 */
@FunctionalInterface
public interface MessageHandler {

	void handleMessage(Message msg);
	
	/**
	 * handles private messages
	 * 
	 * Since 2.0.0 it is possible to define separate handlers for private messages.
	 * By default this simply forwards to {@link #handleMessage(Message)}.
	 */
	default void handlePrivateMessage(Message msg, String from) {
	    handleMessage(msg);
	}

}
