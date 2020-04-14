package ch.awae.netcode.client;

import ch.awae.netcode.client.binding.LocalBindings;
import ch.awae.netcode.exception.NetcodeException;
import ch.awae.netcode.internal.FullChannelInformation;
import ch.awae.netcode.internal.NetcodePacket;
import ch.awae.netcode.internal.ObjectStreams;
import ch.awae.netcode.internal.UserChangeMessage;
import org.apache.commons.lang3.SerializationException;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

class NetcodeClientImpl extends Thread implements NetcodeClient {

    private final static Logger LOG = Logger.getLogger(NetcodeClientImpl.class.getName());

    private final ObjectStreams streams;
    private final Socket socket;
    private final ChannelInformation channelInformation;
    private final Set<String> users = new HashSet<>();

    private final ExecutorService threadPool;
    private final String userId;

    private final PromiseManager<Serializable> promiseManager = new PromiseManager<>();
    private final HashMap<String, ClientReferenceImpl> clientRefs = new HashMap<>();

    private MessageHandler messageHandler;
    private QuestionHandler questionHandler;
    private ChannelEventHandler eventHandler;

    private volatile boolean active = true;

    NetcodeClientImpl(String userId, ProtoClient client, FullChannelInformation channelInformation, MessageHandler messageHandler, QuestionHandler questionHandler, ChannelEventHandler eventHandler) {
        streams = client.getStreams();
        socket = client.getSocket();
        this.userId = userId;
        this.messageHandler = messageHandler;
        this.questionHandler = questionHandler;
        this.eventHandler = eventHandler;
        this.channelInformation = channelInformation;
        users.addAll(Arrays.asList(channelInformation.getUsers()));
        this.threadPool = Executors.newCachedThreadPool();

        start();
    }

    @Override
    public void setMessageHandler(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    @Override
    public void setEventHandler(ChannelEventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    @Override
    public ChannelInformation getChannelInformation() {
        return channelInformation;
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                processMessage(streams.read());
            } catch (IOException e) {
                try {
                    socket.close();
                    return;
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } catch (ClassNotFoundException | SerializationException e) {
                LOG.log(Level.WARNING, "an error occured while processsing incoming message", e);
            }
        }
        active = false;
        threadPool.shutdown();
    }

    @Override
    public void setQuestionHandler(QuestionHandler questionHandler) {
        this.questionHandler = questionHandler;
    }

    @Override
    public String[] getUsers() {
        synchronized (users) {
            return users.toArray(new String[0]);
        }
    }

    private void processMessage(Serializable message) {
        if (message instanceof UserChangeMessage) {
            processUserChange((UserChangeMessage) message);
        } else if (message instanceof NetcodePacketImpl) {
            processPacket((NetcodePacketImpl) message);
        }
    }

    private void processPacket(NetcodePacketImpl message) {
        switch (message.getType()) {
            case MESSAGE:
                handleMessage(message);
                break;
            case QUESTION:
                handleQuestion(message);
                break;
            case RESPONSE:
                handleResponse(message);
                break;
        }
    }

    private void handleResponse(NetcodePacketImpl message) {
        long correlationId = message.getCorrelationId();
        try {
            Serializable response = message.getPayload();
            if (response instanceof Throwable) {
                promiseManager.fail(correlationId, (Throwable) response);
            } else {
                promiseManager.fulfill(correlationId, response);
            }
        } catch (Exception e) {
            promiseManager.fail(correlationId, e);
        }
    }

    private void handleQuestion(NetcodePacketImpl message) {
        QuestionHandler handler = this.questionHandler;
        if (handler != null) {
            threadPool.submit(() -> {
                Serializable answer;
                try {
                    answer = handler.handleQuestion(message.getSenderId(), message.getTimestamp(), message.getPayload());
                    writeToStream(buildPacket(message.getSenderId(), message.getCorrelationId(), NetcodePacketType.RESPONSE, answer));
                } catch (Exception e) {
                    writeToStream(buildPacket(message.getSenderId(), message.getCorrelationId(), NetcodePacketType.RESPONSE, e));
                }
            });
        } else {
            writeToStream(buildPacket(message.getSenderId(), message.getCorrelationId(), NetcodePacketType.RESPONSE, new NetcodeException("could not handle question - no question handler exists on the remote", null)));
        }
    }

    private NetcodePacket buildPacket(String destinationId, long correlationId, NetcodePacketType packetType, Serializable payload) {
        return new NetcodePacketImpl(Timestamp.from(Instant.now()), this.userId, destinationId, correlationId, packetType, payload);
    }

    private void handleMessage(NetcodePacketImpl message) {
        MessageHandler handler = this.messageHandler;
        if (handler != null) {
            if (message.getDestinationId() == null) {
                handler.handleMessage(message.getSenderId(), message.getTimestamp(), message.getPayload());
            } else {
                handler.handlePrivateMessage(message.getSenderId(), message.getTimestamp(), message.getPayload());
            }
        }
    }

    private void processUserChange(UserChangeMessage message) {
        synchronized (users) {
            ChannelEventHandler eventHandler = this.eventHandler;
            if (message.isEntering()) {
                users.add(message.getUser());
                if (eventHandler != null) {
                    threadPool.submit(() -> eventHandler.userChange(message.getUser(), true));
                }
            } else {
                users.remove(message.getUser());
                ClientReferenceImpl ref = clientRefs.remove(message.getUser());
                if (ref != null) {
                    ref.disable();
                }
                if (eventHandler != null) {
                    threadPool.submit(() -> eventHandler.userChange(message.getUser(), false));
                }
            }
        }
    }

    @Override
    public void sendToChannel(Serializable message) {
        verifyState();
        writeToStream(buildPacket(null, -1, NetcodePacketType.MESSAGE, message));
    }

    private void writeToStream(Serializable message) {
        try {
            streams.write(message);
        } catch (IOException e) {
            throw new NetcodeException("could not send message: " + e.getMessage(), e);
        }
    }

    void sendPacket(String destinationId, long correlationId, NetcodePacketType packetType, Serializable payload) {
        writeToStream(buildPacket(destinationId, correlationId, packetType, payload));
    }

    @Override
    public void sendPrivately(String userId, Serializable message) {
        verifyState();
        verifyUserKnown(userId);
        writeToStream(buildPacket(userId, -1, NetcodePacketType.MESSAGE, message));
    }

    @Override
    public void disconnect() {
        interrupt();
        try {
            this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        active = false;
        for (ClientReferenceImpl reference : clientRefs.values()) {
            reference.disable();
        }
        threadPool.shutdown();
    }

    @Override
    public ClientReference getClientReference(String userId) {
        verifyState();
        synchronized (users) {
            verifyUserKnown(userId);
            if (clientRefs.containsKey(userId)) {
                return clientRefs.get(userId);
            } else {
                ClientReferenceImpl clientReference = new ClientReferenceImpl(userId, this, promiseManager);
                clientRefs.put(userId, clientReference);
                return clientReference;
            }
        }
    }

    void verifyState() {
        if (!active) {
            throw new IllegalStateException("client is inactive and can no longer be used");
        }
    }

    @Override
    public LocalBindings getLocalBindings() {
        return null;
    }

    private void verifyUserKnown(String userId) {
        synchronized (users) {
            if (!users.contains(userId)) {
                throw new IllegalArgumentException("unkown user: " + userId);
            }
        }
    }
}
