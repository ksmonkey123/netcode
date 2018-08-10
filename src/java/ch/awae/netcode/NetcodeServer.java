package ch.awae.netcode;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class NetcodeServer extends Thread {

	private final ServerSocket serverSocket;
	private final AtomicBoolean open = new AtomicBoolean(true);
	private final ChannelManager manager;

	public NetcodeServer(ServerSocket serverSocket, Predicate<String> appIdValidator,
			Supplier<String> channelIdProvider) {
		this.serverSocket = serverSocket;
		this.manager = new ChannelManager(appIdValidator, channelIdProvider);
	}

	@Override
	public void run() {
		while (!Thread.interrupted()) {
			try {
				Socket s = serverSocket.accept();
				if (open.get())
					new ClientHandler(manager, s).start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void close() {
		if (!open.compareAndSet(true, false))
			throw new IllegalStateException("NetcodeServer instance already closed");
		this.interrupt();
		this.manager.closeAll();
		try {
			this.serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}