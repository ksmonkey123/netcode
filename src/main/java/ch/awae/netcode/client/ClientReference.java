package ch.awae.netcode.client;

import ch.awae.netcode.client.binding.RemoteBindings;

import java.io.Serializable;
import java.util.concurrent.Future;

public interface ClientReference {

    void sendPrivateMessage(Serializable message);

    Future<Serializable> askQuestion(Serializable message);

    <T extends Serializable> Future<T> askQuestion(Serializable message, Class<? extends T> responseClass);

    boolean isActive();

    RemoteBindings getRemoteBindings();

}
