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

}
