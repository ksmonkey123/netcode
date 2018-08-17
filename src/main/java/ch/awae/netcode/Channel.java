package ch.awae.netcode;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;

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

	private final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
	private final AtomicInteger memberCount = new AtomicInteger(0);
	private final AtomicBoolean open = new AtomicBoolean(true);
	
	private final Lock JOIN_LOCK;
	private final Lock CLOSE_LOCK;

	Channel(String appId, ChannelConfiguration config, ChannelManager owner, String userId) {
		this.config = config;
		this.owner = owner;
		this.appId = appId;
		this.creator = userId;
		// create R/W lock
		ReadWriteLock lock = new ReentrantReadWriteLock();
        this.JOIN_LOCK = lock.readLock();
        this.CLOSE_LOCK = lock.writeLock();
	}

	boolean isFull() {
		return memberCount.get() >= config.getMaxClients();
	}

	void join(String userId, ClientHandler handler) throws IOException, ConnectionException, InterruptedException {
		if (!open.get())
			throw new IllegalStateException();
		JOIN_LOCK.lock();
		try {
		    if (!open.get())
    			throw new IllegalStateException();
		    if (memberCount.getAndIncrement() >= config.getMaxClients()) {
			    memberCount.getAndDecrement();
			    throw new ChannelUserLimitReachedException("channel limit reached: " + config.getMaxClients());
		    }
		    // try to insert
		    if (clients.putIfAbsent(userId, handler) != null) {
		        memberCount.getAndDecrement();
			    throw new DuplicateUserIdException("duplicate username: '" + userId + "'");
		    }
		    // send base data and alert members
		    sendGreetingMessage(handler);
		    notifyUserJoined(userId);
		} finally {
		    JOIN_LOCK.unlock();
		}
	}
	
	void quit(String userId) throws IOException, InterruptedException {
		ClientHandler client = clients.remove(userId);
		if (client == null)
			return;
		client.close();
		sendPublicMessage(MessageFactory.serverMessage(new UserChange(userId, false)));
		if (memberCount.decrementAndGet() <= 0)
			owner.closeChannel(appId, config.getChannelId());
	}

	void close() throws IOException, InterruptedException {
		if (!open.compareAndSet(true, false))
			return;
		CLOSE_LOCK.lock();
		try {
		    for (String user : clients.keySet().toArray(new String[0]))
			    quit(user);
		} finally {
		    CLOSE_LOCK.unlock();
		}
	}

	void send(SSMessageImpl msg) {
		if (msg.isPrivateMessage())
		    sendPrivateMessage(msg);
		else
			sendPublicMessage(msg);
	}

	String[] getMembers() {
		return clients.keySet().toArray(new String[0]);
	}
	
	ChannelInformation getInfo() {
		return new ChannelInformation(config.getChannelId(), config.getChannelName(),
		            creator, memberCount.get(), config.getMaxClients(), config);
	}

    /* ### MESSAGE SENDING SUBROUTINES ### */
    
	private void sendPrivateMessage(SSMessageImpl msg) {
	    ClientHandler client = clients.get(msg.getTargetId());
	    if (client != null)
	        try {
				client.send(msg);
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
	
	private void sendPublicMessage(Message msg) {
	    clients.values().forEach(c -> {
			if (config.isBounceMessages() || !c.getUserId().equals(msg.getUserId()))
				try {
					c.send(msg);
				} catch (IOException e) {
					e.printStackTrace();
				}
		});
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

}
