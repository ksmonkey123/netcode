package ch.awae.netcode;

import java.io.Serializable;

final class Promise {

	private volatile Serializable data;

	synchronized void fulfill(Serializable obj) {
		if (data != null)
			throw new IllegalStateException("promise already fulfilled");
		data = obj;
		notifyAll();
	}

	synchronized Serializable get() throws InterruptedException, ConnectionException {
		while (data == null)
			wait();
		if (data instanceof Throwable) {
			if (data instanceof ConnectionException)
				throw (ConnectionException) data;
			if (data instanceof RuntimeException)
				throw (RuntimeException) data;
			else
				throw new RuntimeException((Throwable) data);
		}
		return data;
	}

}
