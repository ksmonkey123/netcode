package ch.awae.netcode;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

final class Channel {

	private final ChannelConfiguration config;
	private final ChannelManager owner;

	private HashMap<String, ClientHandler> clients = new HashMap<>();
	private AtomicInteger member = new AtomicInteger(0);
	private final String appId;
	private AtomicBoolean open = new AtomicBoolean(true);

	Channel(String appId, ChannelConfiguration config, ChannelManager owner) {
		this.config = config;
		this.owner = owner;
		this.appId = appId;
	}

	synchronized void join(String userId, ClientHandler handler) throws IOException, ConnectionException {
		if (!open.get())
			throw new IllegalStateException();
		int count = member.incrementAndGet();
		if (count > config.getMaxClients()) {
			member.decrementAndGet();
			throw new ConnectionException("channel limit reached: " + config.getMaxClients());
		}
		ClientHandler old = clients.putIfAbsent(userId, handler);
		if (old != null) {
			member.decrementAndGet();
			throw new ConnectionException("duplicate username: '" + userId + "'");
		}
		String[] users = clients.keySet().toArray(new String[0]);
		handler.send(MessageFactory.serverMessage(new GreetingMessage(config, users)));
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
		if (member.decrementAndGet() <= 0) {
			owner.closeChannel(appId, config.getChannelId());
		}
	}

	synchronized void close() throws IOException {
		if (!open.compareAndSet(true, false))
			return;
		String[] users = clients.keySet().toArray(new String[0]);
		for (String user : users)
			quit(user);
	}

	synchronized void send(MessageImpl msg) throws IOException {
		if (msg.isPrivateMessage()) {
			ClientHandler client = clients.get(msg.getTargetId());
			if (client != null) {
				client.send(msg);
			}
		} else {
			clients.values().forEach(c -> {
				if (config.isBounceMessages() || !c.getUserId().equals(msg.getUserId()))
					try {
						c.send(msg);
					} catch (IOException e) {
						e.printStackTrace();
					}
			});
		}
	}

}
