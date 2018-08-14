package ch.awae.netcode;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;

final class NetcodeServerImpl extends Thread implements NetcodeServer {

	private final ServerSocket serverSocket;
	private final AtomicBoolean open = new AtomicBoolean(true);
	private final ChannelManager manager;
	private ServerCapabilities features;

	public NetcodeServerImpl(ServerSocket serverSocket, Predicate<String> appIdValidator,
			Supplier<String> channelIdProvider, ServerCapabilities features) {
		this.serverSocket = serverSocket;
		this.features = features;
		this.manager = new ChannelManager(appIdValidator, channelIdProvider);
	}

	@Override
	public void run() {
		while (!Thread.interrupted()) {
			try {
				Socket s = serverSocket.accept();
				if (open.get())
					new ClientHandler(manager, s, features).start();
			} catch (SocketException e) {
				// ignore socket exception
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
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