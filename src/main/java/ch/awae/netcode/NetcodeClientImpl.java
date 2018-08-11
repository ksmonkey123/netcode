package ch.awae.netcode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import lombok.Getter;
import lombok.Setter;

final class NetcodeClientImpl extends Thread implements NetcodeClient {

	private final Socket socket;
	private final BufferedReader in;
	private final PrintWriter out;
	private @Getter String userId;
	private ChannelConfiguration config;
	private MessageHandler messageHandler;
	private @Setter ChannelEventHandler eventHandler;
	private final Object WRITE_LOCK = new Object();
	private List<String> users = new ArrayList<>();
	private BlockingQueue<MessageImpl> backlog = new LinkedBlockingQueue<>();

	public NetcodeClientImpl(Socket s, MessageHandler messageHandler, ChannelEventHandler eventHandler)
			throws IOException {
		this.messageHandler = messageHandler;
		this.eventHandler = eventHandler;
		try {
			this.socket = s;
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream());
		} catch (IOException e) {
			s.close();
			throw e;
		}
	}

	public void open(NetcodeHandshakeRequest request) throws IOException, ConnectionException {
		try {
			String line = in.readLine();
			if (!Parser.PROTOCOL_VERSION.equals(line))
				throw new ConnectionException("incompatible server version: expected '" + Parser.PROTOCOL_VERSION
						+ "' but received '" + line + "'");
			out.println(Parser.pojo2json(request));
			out.flush();
			userId = request.getUserId();

			// wait for initialization data to arrive
			while (true) {
				MessageImpl msg = Parser.json2pojo(in.readLine(), MessageImpl.class);
				if (msg.isManagementMessage() && msg.getPayload() instanceof GreetingMessage) {
					GreetingMessage gm = (GreetingMessage) msg.getPayload();
					config = gm.getConfig();
					for (String user : gm.getUsers())
						users.add(user);
					break;
				} else if (msg.isManagementMessage() && msg.getPayload() instanceof Throwable) {
					if (msg.getPayload() instanceof ConnectionException)
						throw (ConnectionException) msg.getPayload();
					throw new RuntimeException((Throwable) msg.getPayload());
				} else if (msg.isManagementMessage()) {
					handleManagementMessage(msg);
				} else {
					backlog.add(msg);
				}
			}

			// work through backlog
			if (messageHandler != null)
				setMessageHandler(messageHandler);

			this.start();

		} catch (IOException e) {
			socket.close();
			throw e;
		}

	}

	private void handleManagementMessage(MessageImpl msg) {
		Serializable payload = msg.getPayload();
		ChannelEventHandler eventHandler = this.eventHandler;
		if (payload instanceof UserChange) {
			UserChange data = (UserChange) payload;
			if (data.isJoined()) {
				if (!users.contains(data.getUserId()))
					users.add(data.getUserId());
				if (eventHandler != null)
					eventHandler.clientJoined(data.getUserId());
			} else {
				users.remove(data.getUserId());
				if (eventHandler != null)
					eventHandler.clientLeft(data.getUserId());
			}
		}
	}

	private void process(MessageImpl m) {
		if (m.isManagementMessage()) {
			handleManagementMessage(m);
		} else if (messageHandler != null) {
			messageHandler.handleMessage(m);
		} else {
			backlog.add(m);
		}
	}

	@Override
	public ChannelConfiguration getChannelConfiguration() {
		return this.config;
	}

	@Override
	public void run() {
		try {
			while (!Thread.interrupted()) {
				try {
					String m = in.readLine();
					if (m == null)
						continue;
					MessageImpl msg = Parser.json2pojo(m, MessageImpl.class);
					process(msg);
				} catch (IOException e) {
					break;
				}
			}
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void disconnect() throws IOException {
		socket.close();
	}

	@Override
	public void send(Serializable payload) {
		synchronized (WRITE_LOCK) {
			out.println(Parser.pojo2json(MessageFactory.normalMessage(userId, payload)));
			out.flush();
		}
	}

	public String[] getUsers() {
		return users.toArray(new String[0]);
	}

	@Override
	public void sendPrivately(String userId, Serializable payload) {
		Objects.requireNonNull(userId);
		synchronized (WRITE_LOCK) {
			out.println(Parser.pojo2json(MessageFactory.privateMessage(this.userId, userId, payload)));
			out.flush();
		}
	}

	@Override
	public void setMessageHandler(MessageHandler handler) {
		this.messageHandler = handler;
		if (this.messageHandler != null)
			while (!backlog.isEmpty())
				messageHandler.handleMessage(backlog.poll());
	}

	@Override
	public Message receive() throws InterruptedException {
		return this.backlog.take();
	}

}
