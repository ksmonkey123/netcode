package ch.awae.netcode.server;

public interface NetcodeServer {

    void terminate();

    void join() throws InterruptedException;

    default void terminateAndJoin() throws InterruptedException {
        terminate();
        join();
    }

}
