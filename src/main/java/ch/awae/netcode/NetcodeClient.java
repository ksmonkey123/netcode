package ch.awae.netcode;

import java.io.IOException;
import java.io.Serializable;

public interface NetcodeClient {

	/**
	 * Disconnect and terminate the client. After this method returns this
	 * instance may no longer be used.
	 * 
	 * @throws IOException
	 */
	void disconnect() throws IOException;

	/**
	 * Send the given object to all members of the channel
	 */
	void send(Serializable payload);

	/**
	 * Send the given object to the member of the channel with the given userId.
	 * Only the specified user will (if he exists) receive the message. It will
	 * not be transmitted to any other clients.
	 */
	void sendPrivately(String userId, Serializable payload);

	/**
	 * Get the userId of this client.
	 */
	String getUserId();

	/**
	 * Get the configuration of the connected channel.
	 */
	ChannelConfiguration getChannelConfiguration();

	/**
	 * Get a list of all members of the channel. This list is automatically kept
	 * up to date.
	 */
	String[] getUsers();

	/**
	 * Replace the message handler. If no message handler has been set before
	 * the new handler will receive the full backlog of all previous messages.
	 * 
	 * @param handler
	 * @throws NullPointerException
	 *             handler is null
	 */
	void setMessageHandler(MessageHandler handler);

}
