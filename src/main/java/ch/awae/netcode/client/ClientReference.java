package ch.awae.netcode.client;

import java.io.Serializable;
import java.util.concurrent.Future;

public interface ClientReference {

    void sendPrivateMessage(Serializable message);

    Future<Serializable> askQuestion(Serializable message);

    <T extends Serializable> Future<T> askQuestion(Serializable message, Class<? extends T> responseClass);

    boolean isActive();

}
