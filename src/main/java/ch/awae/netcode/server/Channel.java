package ch.awae.netcode.server;

import ch.awae.netcode.client.ChannelFeatures;
import ch.awae.netcode.internal.FullChannelInformation;
import ch.awae.netcode.internal.ObjectStreams;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

class Channel {

    private final static Logger LOG = Logger.getLogger(Channel.class.getName());

    private final ChannelID id;
    private final ChannelFeatures features;
    private final ChannelManager channelManager;

    private final Lock USE_LOCK;
    private final Lock UPDATE_LOCK;

    private final ConcurrentMap<String, Client> clients = new ConcurrentHashMap<>();

    public Channel(ChannelID id, ChannelFeatures features, ChannelManager channelManager) {
        this.id = id;
        this.features = features;
        this.channelManager = channelManager;

        ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
        USE_LOCK = readWriteLock.readLock();
        UPDATE_LOCK = readWriteLock.writeLock();
    }

    void validatePassword(String password) {
        String passwd = features.getPassword();
        if (passwd != null) {
            if (!passwd.equals(password)) {
                throw new IllegalArgumentException("unauthorized");
            }
        }
    }

    private FullChannelInformation getChannelInformation() {
        return new ChannelInformationImpl(getUserList(), id.getChannelId(), features.getClientLimit());
    }

    private String[] getUserList() {
        return clients.keySet().toArray(new String[0]);
    }

    void sendPrivately(String userId, Serializable message) {
        USE_LOCK.lock();
        try {
            Client client = clients.get(userId);
            if (client == null) {
                throw new IllegalArgumentException("target user does not exist");
            }
            client.send(message);
        } finally {
            USE_LOCK.unlock();
        }
    }

    void sendPublicly(Serializable message) {
        USE_LOCK.lock();
        try {
            clients.forEach((id, client) -> client.send(message));
        } finally {
            USE_LOCK.unlock();
        }
    }

    void addClient(String userId, Socket clientSocket, ObjectStreams streams) {
        UPDATE_LOCK.lock();
        try {
            enforceUniqueUserIds(userId);
            enforceClientLimit();
            Client client = new Client(userId, clientSocket, streams, this);
            clients.put(userId, client);
            LOG.info("client " + userId + " entered channel " + this.id);
            client.send(getChannelInformation());
            sendPublicly(new UserChangeMessageImpl(userId, true));
        } finally {
            UPDATE_LOCK.unlock();
        }
    }

    private void enforceUniqueUserIds(String userId) {
        if (clients.containsKey(userId)) {
            throw new IllegalArgumentException("userId already exists in channel");
        }
    }

    private void enforceClientLimit() {
        int newCount = clients.size();
        int clientLimit = features.getClientLimit();
        if (clientLimit > 0 && clientLimit < newCount) {
            throw new IllegalStateException("channel full");
        }
    }

    void removeClient(Client client) {
        UPDATE_LOCK.lock();
        try {
            String userId = client.getUserId();
            if (clients.containsKey(userId)) {
                LOG.info("client " + userId + " left channel " + this.id);
                doRemoveClient(userId);
            }
        } finally {
            UPDATE_LOCK.unlock();
        }
    }

    private void doRemoveClient(String userId) {
        clients.remove(userId);
        sendPublicly(new UserChangeMessageImpl(userId, false));
        if (clients.isEmpty()) {
            closeChannel();
        }
    }

    private void closeChannel() {
        channelManager.removeChannel(id);
    }

    void shutdown() {
        UPDATE_LOCK.lock();
        try {
            clients.forEach((id, client) -> {
                client.terminate();
                client.interrupt();
                try {
                    client.getSocket().close();
                    client.closeStreams();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } finally {
            UPDATE_LOCK.unlock();
        }
    }
}
