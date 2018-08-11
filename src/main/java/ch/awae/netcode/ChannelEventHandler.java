package ch.awae.netcode;

/**
 * Event handler receiving channel events such as clients joining or leaving.
 * 
 * All methods have an empty default implementation
 * 
 * Usually (always except during client initialisation) the methods of this
 * interface are invoked asynchronously.
 * 
 * @since netcode 0.2.0
 * @author Andreas Wälchli
 */
public interface ChannelEventHandler {

	default void clientJoined(String userId) {
	}

	default void clientLeft(String userId) {
	}
}
