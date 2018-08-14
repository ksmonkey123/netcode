package ch.awae.netcode;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A single communications channel.
 * 
 * This class manages channel members, member limits, joining of new members
 * as well as message transmission.
 */
final class Channel {

	private final ChannelConfiguration config;
	private final ChannelManager owner;
	private final String appId;
	private final String creator;

	private HashMap<String, ClientHandler> clients = new HashMap<>();
	private AtomicInteger member = new AtomicInteger(0);
	private AtomicBoolean open = new AtomicBoolean(true);

	Channel(String appId, ChannelConfiguration config, ChannelManager owner, String userId) {
		this.config = config;
		this.owner = owner;
		this.appId = appId;
		this.creator = userId;
	}

	boolean isFull() {
		return member.get() >= config.getMaxClients();
	}

	synchronized void join(String userId, ClientHandler handler) throws IOException, ConnectionException {
		if (!open.get())
			throw new IllegalStateException();
		if (isFull())
			throw new ChannelUserLimitReachedException("channel limit reached: " + config.getMaxClients());
		ClientHandler old = clients.putIfAbsent(userId, handler);
		if (old != null)
			throw new DuplicateUserIdException("duplicate username: '" + userId + "'");
		member.incrementAndGet();
		sendGreetingMessage(handler);
		notifyUserJoined(userId);
	}
	
	private void sendGreetingMessage(ClientHandler handler) throws IOException {
	    String[] users = clients.keySet().toArray(new String[0]);
		handler.send(MessageFactory.serverMessage(new GreetingMessage(config, users)));
	}
	
	private void notifyUserJoined(String userId) {
	    Message msg = MessageFactory.serverMessage(new UserChange(userId, true));
		clients.values().forEach(c -> {
			if (!c.getUserId().equals(userId))
				try {
					c.send(msg);
				} catch (IOException e) {
					e.printStackTrace();
				}
		});
	}

	synchronized void quit(String userId) throws IOException {
		ClientHandler client = clients.remove(userId);
		if (client == null)
			return;
		client.close();
		send(MessageFactory.serverMessage(new UserChange(userId, false)));
		if (member.decrementAndGet() <= 0)
			owner.closeChannel(appId, config.getChannelId());
	}

	synchronized void close() throws IOException {
		if (!open.compareAndSet(true, false))
			return;
		for (String user : clients.keySet().toArray(new String[0]))
			quit(user);
	}

	ChannelInformation getInfo() {
		return new ChannelInformation(config.getChannelId(), config.getChannelName(), creator, member.get(),
				config.getMaxClients(), config);
	}

	synchronized void send(MessageImpl msg) throws IOException {
		if (msg.isPrivateMessage())
		    sendPrivateMessage(msg);
		else
			sendPublicMessage(msg);
	}
	
	private void sendPrivateMessage(MessageImpl msg) {
	    ClientHandler client = clients.get(msg.getTargetId());
	    if (client != null)
	        try {
				client.send(msg);
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
	
	private void sendPublicMessage(MessageImpl msg) {
	    clients.values().forEach(c -> {
			if (config.isBounceMessages() || !c.getUserId().equals(msg.getUserId()))
				try {
					c.send(msg);
				} catch (IOException e) {
					e.printStackTrace();
				}
		});
	}

	String[] getMembers() {
		return clients.keySet().toArray(new String[0]);
	}

}
