package ch.awae.netcode.client;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

class PromiseManager<T> {

    private final AtomicLong nextCorrelationId = new AtomicLong(1);
    private final ConcurrentHashMap<Long, CompletableFuture<T>> promises = new ConcurrentHashMap<>();

    long nextCorrelationId() {
        return nextCorrelationId.getAndIncrement();
    }

    CompletableFuture<T> createPromise(long correlationId) {
        CompletableFuture<T> promise = new CompletableFuture<>();
        promises.put(correlationId, promise);
        return promise;
    }

    void fulfill(long correlationId, T result) {
        CompletableFuture<T> promise = promises.get(correlationId);
        promise.complete(result);
        promises.remove(correlationId);
    }

    void fail(long correlationId, Throwable ex) {
        CompletableFuture<T> promise = promises.get(correlationId);
        promise.completeExceptionally(ex);
        promises.remove(correlationId);
    }

}
