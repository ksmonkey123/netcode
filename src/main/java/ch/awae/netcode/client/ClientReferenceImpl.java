package ch.awae.netcode.client;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class ClientReferenceImpl implements ClientReference {

    private final String userId;
    private final NetcodeClientImpl netcodeClient;
    private final PromiseManager<Serializable> promiseManager;
    private final ConcurrentHashMap<Long, CompletableFuture<Serializable>> promises = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    private volatile boolean active = true;

    ClientReferenceImpl(String userId, NetcodeClientImpl netcodeClient, PromiseManager<Serializable> promiseManager) {
        this.userId = userId;
        this.netcodeClient = netcodeClient;
        this.promiseManager = promiseManager;
    }

    @Override
    public void sendPrivateMessage(Serializable message) {
        verifyActive();
        netcodeClient.sendPacket(userId, -1, NetcodePacketType.MESSAGE, message);
    }

    @Override
    public CompletableFuture<Serializable> askQuestion(Serializable message) {
        rwLock.readLock().lock();
        try {
            verifyActive();

            long id = promiseManager.nextCorrelationId();
            CompletableFuture<Serializable> promise = promiseManager.createPromise(id);
            registerPromise(id, promise);
            promise.whenComplete((serializable, throwable) -> unregisterPromise(id));

            try {
                netcodeClient.sendPacket(userId, id, NetcodePacketType.QUESTION, message);
            } catch (Exception e) {
                promiseManager.fail(id, e);
            }

            return promise;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    private void unregisterPromise(long id) {
        promises.remove(id);
    }

    private void registerPromise(long id, CompletableFuture<Serializable> promise) {
        promises.put(id, promise);
    }

    private void verifyActive() {
        netcodeClient.verifyState();
        if (!active) {
            throw new IllegalStateException("user no longer present: " + userId);
        }
    }

    @Override
    public <T extends Serializable> Future<T> askQuestion(Serializable message, Class<? extends T> responseClass) {
        CompletableFuture<Serializable> raw = askQuestion(message);
        return raw.thenApply(responseClass::cast);
    }

    @Override
    public boolean isActive() {
        return active;
    }

    void disable() {
        active = false;
        rwLock.writeLock().lock();
        try {
            for (CompletableFuture<Serializable> future : promises.values()) {
                future.cancel(false);
            }
            promises.clear();
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
