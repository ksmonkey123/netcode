package ch.awae.netcode.server;

import ch.awae.netcode.internal.*;

import java.io.IOException;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.Logger;

class NetcodeServerImpl extends Thread implements NetcodeServer {

    private final static Logger LOG = Logger.getLogger(NetcodeServerImpl.class.getName());

    private final Predicate<String> appIdValidator;
    private final ServerSocket serverSocket;
    private final ChannelManager channelManager = new ChannelManager();

    public NetcodeServerImpl(int port, Predicate<String> appIdValidator) throws IOException {
        this.appIdValidator = Objects.requireNonNull(appIdValidator);
        this.serverSocket = new ServerSocket(port);
        LOG.info("started netcode server on port " + port);
        start();
    }

    @Override
    public void terminate() {
        LOG.info("terminating netcode server");
        synchronized (this) {
            interrupt();
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            acceptAndHandleClient();
        }
        try {
            shutdownServer();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void shutdownServer() throws InterruptedException {
        channelManager.shutdownChannels();
        LOG.info("netcode server shut down");
    }

    private void acceptAndHandleClient() {
        try {
            Socket client = serverSocket.accept();
            handleClient(client);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClient(Socket clientSocket) throws IOException {
        LOG.info("handling new user: " + clientSocket.getRemoteSocketAddress());
        ObjectStreams streams = new ObjectStreams(clientSocket);
        String appId = null;
        try {
            appId = validateHandshake(streams.read(HandshakeRequest.class));
        } catch (Exception e) {
            streams.write(e);
            clientSocket.close();
        }
        streams.write(null);
        try {
            Serializable request = streams.read();
            Client client;
            if (request instanceof CreateChannelRequest) {
                CreateChannelRequest createRequest = (CreateChannelRequest) request;
                Channel channel = channelManager.createChannel(appId, createRequest.getFeatures());
                channel.addClient(createRequest.getUserId(), clientSocket, streams);
            } else if (request instanceof JoinChannelRequest) {
                JoinChannelRequest joinRequest = (JoinChannelRequest) request;
                Channel channel = channelManager.getChannel(appId, joinRequest.getChannelId());
                channel.validatePassword(joinRequest.getPassword());
                channel.addClient(joinRequest.getUserId(), clientSocket, streams);
            } else {
                throw new UnsupportedOperationException("cannot process request");
            }
        } catch (Exception e) {
            streams.write(e);
            clientSocket.close();
        }
    }

    private String validateHandshake(HandshakeRequest request) {
        if (!appIdValidator.test(request.getAppId())) {
            throw new IllegalArgumentException("bad appId");
        }
        return request.getAppId();
    }

}
