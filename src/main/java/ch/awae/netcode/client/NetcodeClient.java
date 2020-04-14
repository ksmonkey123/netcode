package ch.awae.netcode.client;

import java.io.Serializable;

public interface NetcodeClient {

    ChannelInformation getChannelInformation();

    String[] getUsers();

    default String getChannelId() {
        return getChannelInformation().getChannelId();
    }

    void sendToChannel(Serializable message);

    void sendPrivately(String userId, Serializable message);

    void setMessageHandler(MessageHandler messageHandler);

    void setQuestionHandler(QuestionHandler questionHandler);

    void setEventHandler(ChannelEventHandler eventHandler);

    void disconnect();

    ClientReference getClientReference(String userId);

}
