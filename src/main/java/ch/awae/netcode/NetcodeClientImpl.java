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

final class NetcodeClientImpl extends Thread implements NetcodeClient {

	private final Socket socket;
	private final BufferedReader in;
	private final PrintWriter out;
	private @Getter String userId;
	private ChannelConfiguration config;
	private MessageHandler messageHandler;
	private final Object WRITE_LOCK = new Object();
	private List<String> users = new ArrayList<>();
	private BlockingQueue<MessageImpl> backlog = new LinkedBlockingQueue<>();

	public NetcodeClientImpl(Socket s, MessageHandler messageHandler) throws IOException {
		this.messageHandler = messageHandler;
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
			out.println(Parser.pojo2json(request));
			out.flush();
			userId = request.getUserId();

			// wait for initialization data to arrive
			while (true) {
				MessageImpl msg = Parser.json2pojo(in.readLine(), MessageImpl.class);
				if (msg.isManagementMessage() && msg.getPayload() instanceof GreetingMessage) {
					GreetingMessage gm = (GreetingMessage) msg.getPayload();
					config = gm.getConfig();
					if (messageHandler != null)
						for (String user : gm.getUsers())
							messageHandler.clientJoined(user);
					break;
				} else if (msg.isManagementMessage() && msg.getPayload() instanceof Throwable) {
					if (msg.getPayload() instanceof ConnectionException)
						throw new ConnectionException(((ConnectionException) msg.getPayload()).getMessage());
					throw new RuntimeException((Throwable) msg.getPayload());
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

	private void process(MessageImpl m) {
		if (m.isManagementMessage() && m.getPayload() instanceof UserChange) {
			UserChange data = (UserChange) m.getPayload();
			if (data.isJoined()) {
				if (!users.contains(data.getUserId()))
					users.add(data.getUserId());
				messageHandler.clientJoined(data.getUserId());
			} else {
				users.remove(data.getUserId());
				messageHandler.clientLeft(data.getUserId());
			}
		} else {
			messageHandler.handleMessage(m);
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
					if (messageHandler != null)
						process(msg);
					else
						backlog.add(msg);
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
	public void send(Serializable payload) throws IOException {
		synchronized (WRITE_LOCK) {
			out.println(Parser.pojo2json(MessageFactory.normalMessage(userId, payload)));
			out.flush();
		}
	}

	public String[] getUsers() {
		return users.toArray(new String[0]);
	}

	@Override
	public void sendPrivately(String userId, Serializable payload) throws IOException {
		synchronized (WRITE_LOCK) {
			out.println(Parser.pojo2json(MessageFactory.privateMessage(this.userId, userId, payload)));
			out.flush();
		}
	}

	@Override
	public void setMessageHandler(MessageHandler handler) {
		Objects.requireNonNull(handler);
		while (!backlog.isEmpty()) {
			process(backlog.poll());
		}
	}

}
