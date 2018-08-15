package ch.awae.netcode;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.TimeoutException;

/**
 * The client instance for the Netcode system.
 * 
 * The client can receive messages synchronously and asynchronously. In any case
 * the messages are read from the network asynchronously. If a
 * {@link MessageHandler} is defined it gets invoked. If no such handler is
 * specified the messages enter the message queue for synchronous reception
 * using {@link #receive()} or {@link #tryReceive()}.
 * 
 * The client can be switched between synchronous and asynchronous operation at
 * any time. When switching from synchronous to asynchronous mode all the
 * messages from the synchronous message queue are pushed into the message
 * handler. During this phase it is possible that messages arrive out of order
 * or multiple concurrent invocations of the handler exit. It is therefore
 * recommended to avoid frequent mode switching. Threads blocked in
 * {@link #receive()} during the transition to asynchronous operation remain
 * blocked indefinitely.
 * 
 * Channel events can only be processed asynchronously. For this a
 * {@link ChannelEventHandler} must be specified.
 * 
 * @since netcode 0.1.0
 * @see NetcodeClientFactory
 * 
 * @author Andreas Wälchli
 */
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
	 * Only the specified user will receive the message. It will not be
	 * transmitted to any other clients.
	 * 
	 * @throws NullPointerException
	 *             the userId is null
	 * @throws IllegalArgumentException
	 *             the userId is not known
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

	void setMessageHandler(MessageHandler handler);

	void setEventHandler(ChannelEventHandler handler);

	void setQuestionHandler(ClientQuestionHandler handler);

	/**
	 * Receive the next message synchronously and wait if no message is ready.
	 * 
	 * Note: if this method is called while a {@link MessageHandler} is present
	 * the call will block indefinitely.
	 */
	Message receive() throws InterruptedException;

	/**
	 * Receive the next message synchronously if it exists. Otherwise returns
	 * {@code null}.
	 */
	Message tryReceive();

	Serializable ask(String userId, Serializable data) throws InterruptedException, TimeoutException;

	/**
	 * Requests channel information for the current channel from the server.
	 * This requires that server commands are enabled on the server.
	 * 
	 * This blocks the thread until the data becomes available.
	 * 
	 * @since netcode 2.0.0
	 * @throws UnsupportedFeatureException
	 *             the server does not support server commands
	 * @throws InterruptedException
	 *             the thread has been interrupted while waiting on the data
	 * @throws TimeoutException 
	 */
	ChannelInformation getChannelInformation() throws InterruptedException, ConnectionException, TimeoutException;

	/**
	 * Requests a list of all public channels from the server. This requires
	 * that server commands are enabled on the server.
	 * 
	 * This blocks the thread until the data becomes available.
	 * 
	 * @since netcode 2.0.0
	 * @throws UnsupportedFeatureException
	 *             the server does not support server commands
	 * @throws InterruptedException
	 *             the thread has been interrupted while waiting on the data
	 * @throws TimeoutException 
	 */
	ChannelInformation[] getPublicChannels() throws InterruptedException, ConnectionException, TimeoutException;

}
