package ch.awae.netcode.client;

@FunctionalInterface
public interface ChannelEventHandler {

    void userChange(String userId, boolean joined);

}
