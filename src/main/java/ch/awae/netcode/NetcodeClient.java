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
	 *             an i/o exception occurred while disconnecting the client.
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
	
	/**
	 * Get a userRef for a specific user.
	 * @param userId
	 * @return
	 */
	UserRef getUserRef(String userId);

	/**
	 * Returns the current message handler or {@code null} if none is currently
	 * set.
	 * 
	 * @since netcode 2.0.0
	 */
	MessageHandler getMessageHandler();

	/**
	 * Replaces the message handler. If the handler is set to {@code null} the
	 * client switches into synchronous operation and messages can be accessed
	 * using {@link #receive()} or {@link #tryReceive()}.
	 * 
	 * @param handler
	 *            the new message handler. may be null.
	 */
	void setMessageHandler(MessageHandler handler);

	/**
	 * Returns the current event handler or {@code null} if none is currently
	 * set.
	 * 
	 * @since netcode 2.0.0
	 */
	ChannelEventHandler getEventHandler();

	/**
	 * Replaces the event handler. may be null.
	 */
	void setEventHandler(ChannelEventHandler handler);

	/**
	 * Returns the current question handler or {@code null} if none is currently
	 * set.
	 * 
	 * @since netcode 2.0.0
	 */
	ClientQuestionHandler getQuestionHandler();

	/**
	 * Replaces the question handler. may be null. If the handler is set to
	 * {@code null} client questions will always be responded to with an
	 * {@link UnsupportedOperationException}.
	 * 
	 * @since netcode 2.0.0
	 * @see #ask(String, Serializable)
	 */
	void setQuestionHandler(ClientQuestionHandler handler);

	/**
	 * Receive the next message synchronously and wait if no message is ready.
	 * 
	 * Note: if this method is called while a {@link MessageHandler} is present
	 * the call will block indefinitely.
	 * 
	 * @throws InterruptedException
	 *             the calling thread has been interrupted before a message
	 *             became available
	 */
	Message receive() throws InterruptedException;

	/**
	 * Receive the next message synchronously if it exists. Otherwise returns
	 * {@code null}.
	 * 
	 * @see #receive()
	 */
	Message tryReceive();

	/**
	 * returns the current timeout value in milliseconds
	 * 
	 * @since netcode 2.0.0
	 */
	long getTimeout();

	/**
	 * Sets a new timeout for server commands and client questions. If this is
	 * set to {@code 0} the timeout will be disabled.
	 * 
	 * @param millis
	 *            the new timeout in milliseconds
	 * @throws IllegalArgumentException
	 *             the timeout value is negative
	 * @since netcode 2.0.0
	 */
	void setTimeout(long millis);

	/**
	 * Sends a question to another client and awaits its response. Questions are
	 * handled through a {@link ClientQuestionHandler}.
	 * 
	 * @param userId
	 *            the client to send the question to
	 * @param data
	 *            the question payload
	 * @return the response
	 * @throws InterruptedException
	 *             the calling thread was interrupted before a response has been
	 *             received.
	 * @throws TimeoutException
	 *             the timeout has expired before a response has been received.
	 * @throws IllegalArgumentException
	 *             the provided userId is unknown
	 * @throws NullPointerException
	 *             the provided userId is null.
	 * 
	 * @see ClientQuestionHandler
	 * @see #setQuestionHandler(ClientQuestionHandler)
	 * @see #setTimeout(long)
	 */
	Serializable ask(String userId, Serializable data) throws InterruptedException, TimeoutException;

    /**
     * Sends a question to another client, awaits its repsonse and casts the
     * response to the provided type. Questions are handled through a
     * {@link ClientQuestionHandler}.
     * 
     * @param userId the client to send the question to
     * @param data the question payload
     * @param responseType the type of the response payload
     * @return the response cast to the responseType
     * @throws InterruptedException
	 *             the calling thread was interrupted before a response has been
	 *             received.
	 * @throws TimeoutException
	 *             the timeout has expired before a response has been received.
	 * @throws IllegalArgumentException
	 *             the provided userId is unknown
	 * @throws NullPointerException
	 *             the provided userId is null.
	 * 
	 * @see ClientQuestionHandler
	 * @see #setQuestionHandler(ClientQuestionHandler)
	 * @see #setTimeout(long)
     */
    <T extends Serializable> T ask(String userId, Serializable data, Class<T> responseType) throws InterruptedException, TimeoutException;

	/**
	 * Requests channel information for the current channel from the server.
	 * This requires that server commands are enabled on the server.
	 * 
	 * This blocks the thread until the data becomes available or the timeout
	 * has expired.
	 * 
	 * @since netcode 2.0.0
	 * @throws UnsupportedFeatureException
	 *             the server does not support server commands
	 * @throws InterruptedException
	 *             the thread has been interrupted while waiting on the data
	 * @throws TimeoutException
	 *             the client has not received a response before the timeout has
	 *             expired
	 * @throws ConnectionException
	 *             the request has been rejected by the server
	 * @see #setTimeout(long)
	 */
	ChannelInformation getChannelInformation() throws InterruptedException, ConnectionException, TimeoutException;

	/**
	 * Requests a list of all public channels from the server. This requires
	 * that server commands are enabled on the server.
	 * 
	 * This blocks the thread until the data becomes available or the timeout
	 * has expired.
	 * 
	 * @since netcode 2.0.0
	 * @throws UnsupportedFeatureException
	 *             the server does not support server commands
	 * @throws InterruptedException
	 *             the thread has been interrupted while waiting on the data
	 * @throws TimeoutException
	 *             the client has not received a response before the timeout has
	 *             expired.
	 * @throws ConnectionException
	 *             the request has been rejected by the server
	 * @see #setTimeout(long)
	 */
	ChannelInformation[] getPublicChannels() throws InterruptedException, ConnectionException, TimeoutException;

}
