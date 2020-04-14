package ch.awae.netcode.client;

import ch.awae.netcode.exception.HandshakeException;
import ch.awae.netcode.internal.FullChannelInformation;
import ch.awae.netcode.internal.ObjectStreams;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.util.Objects;

public class NetcodeClientFactory {

    private String host;
    private int port = -1;
    private String appId;

    private MessageHandler messageHandler;
    private QuestionHandler questionHandler;
    private ChannelEventHandler channelEventHandler;

    public NetcodeClientFactory() {
    }

    public NetcodeClientFactory(String host, int port, String appId) {
        setHost(host);
        setPort(port);
        setAppId(appId);
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = Objects.requireNonNull(host);
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        validatePort(port);
        this.port = port;
    }

    private void validatePort(int port) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("invalid port number: " + port);
        }
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    public void setMessageHandler(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    public QuestionHandler getQuestionHandler() {
        return questionHandler;
    }

    public void setQuestionHandler(QuestionHandler questionHandler) {
        this.questionHandler = questionHandler;
    }

    public ChannelEventHandler getChannelEventHandler() {
        return channelEventHandler;
    }

    public void setChannelEventHandler(ChannelEventHandler channelEventHandler) {
        this.channelEventHandler = channelEventHandler;
    }

    public NetcodeClient createChannel(String user) throws IOException {
        return createChannel(user, new ChannelFeatures());
    }

    public NetcodeClient createChannel(String user, ChannelFeatures features) {
        ProtoClient client = null;
        try {
            client = createProtoClient();
            return establishChannel(client, new CreateChannelRequestImpl(user, features), user);
        } catch (IOException | ClassNotFoundException e) {
            handleNetcodeException(client, e);
            return null;
        }
    }

    private NetcodeClient establishChannel(ProtoClient client, Serializable request, String user) throws IOException, ClassNotFoundException {
        client.getStreams().write(request);
        Serializable response = client.getStreams().read();
        if (response instanceof FullChannelInformation) {
            return new NetcodeClientImpl(user, client, (FullChannelInformation) response, messageHandler, questionHandler, channelEventHandler);
        } else if (response instanceof Exception) {
            throw new HandshakeException((Exception) response);
        } else {
            throw new AssertionError("unexpected response to handshake 2");
        }
    }

    private void handleNetcodeException(ProtoClient client, Exception e) {
        if (client != null) {
            try {
                client.getSocket().close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        throw new HandshakeException(e);
    }

    public NetcodeClient joinChannel(String channelId, String user, String password) {
        ProtoClient client = null;
        try {
            client = createProtoClient();
            return establishChannel(client, new JoinChannelRequestImpl(user, password, channelId), user);
        } catch (IOException | ClassNotFoundException e) {
            handleNetcodeException(client, e);
            return null;
        }
    }

    private ProtoClient createProtoClient() throws IOException, ClassNotFoundException {
        validatePort(port);
        Socket socket = new Socket(host, port);
        ObjectStreams streams = new ObjectStreams(socket);

        streams.write(new HandshakeRequestImpl(appId));
        Exception exception = streams.read(Exception.class);
        if (exception != null) {
            throw new HandshakeException(exception);
        }
        return new ProtoClient(socket, streams);
    }

}
