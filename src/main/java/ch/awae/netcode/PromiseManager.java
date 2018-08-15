package ch.awae.netcode;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;

import lombok.val;

class PromiseManager {

	private final AtomicLong index = new AtomicLong();
	private final ConcurrentHashMap<Long, Promise> registry = new ConcurrentHashMap<>();

	Serializable create(LongConsumer runner) throws InterruptedException, ConnectionException {
		long idx = index.incrementAndGet();
		try {
			val promise = new Promise();
			registry.put(idx, promise);
			runner.accept(idx);
			synchronized (promise) {
				while (promise.data == null)
					promise.wait();
			}
			if (promise.data instanceof RuntimeException)
				throw (RuntimeException) promise.data;
			if (promise.data instanceof ConnectionException)
				throw (ConnectionException) promise.data;
			if (promise.data instanceof Throwable)
				throw new RuntimeException((Throwable) promise.data);
			return promise.data;
		} finally {
			registry.remove(idx);
		}
	}

	void fulfill(long idx, Serializable data) {
		val promise = registry.get(idx);
		if (promise == null)
			return;
		synchronized (promise) {
			promise.data = data;
			promise.notifyAll();
		}
	}

}

class Promise {
	volatile Serializable data = null;
}