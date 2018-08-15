package ch.awae.netcode;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;

import lombok.Getter;
import lombok.Setter;
import lombok.val;

class PromiseManager {

	private final AtomicLong index = new AtomicLong();
	private final ConcurrentHashMap<Long, Promise> registry = new ConcurrentHashMap<>();
	private @Getter @Setter long timeout;

	public PromiseManager(long timeout) {
		this.timeout = timeout;
	}

	Serializable create(LongConsumer runner) throws InterruptedException, ConnectionException, TimeoutException {
		long idx = index.incrementAndGet();
		try {
			val promise = new Promise();
			registry.put(idx, promise);
			runner.accept(idx);
			synchronized (promise) {
				if (timeout > 0) {
					long end = System.currentTimeMillis() + timeout;
					while (promise.data == null && System.currentTimeMillis() < end) {
						long delta = end - System.currentTimeMillis();
						promise.wait(delta);
					}
				} else {
					while (promise.data == null)
						promise.wait();
				}
			}
			if (promise.data == null)
				throw new TimeoutException();
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