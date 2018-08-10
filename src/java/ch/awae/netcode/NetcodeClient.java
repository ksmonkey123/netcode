package ch.awae.netcode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

public final class NetcodeClient extends Thread {

	private final Socket socket;
	private final BufferedReader in;
	private final PrintWriter out;
	private @Getter String userId;
	private ChannelConfiguration config;
	private final MessageHandler messageHandler;
	private final Object WRITE_LOCK = new Object();
	private List<String> users = new ArrayList<>();

	public NetcodeClient(Socket s, MessageHandler messageHandler) throws IOException {
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

			List<MessageImpl> backlog = new ArrayList<>();
			// wait for initialization data to arrive

			while (true) {
				MessageImpl msg = Parser.json2pojo(in.readLine(), MessageImpl.class);
				if (msg.isManagementMessage() && msg.getPayload() instanceof GreetingMessage) {
					GreetingMessage gm = (GreetingMessage) msg.getPayload();
					config = gm.getConfig();
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
			for (MessageImpl m : backlog)
				process(m);

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
					process(Parser.json2pojo(m, MessageImpl.class));
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

	public void close() throws IOException {
		socket.close();
	}

	public void send(Object payload) throws IOException {
		synchronized (WRITE_LOCK) {
			out.println(Parser.pojo2json(MessageFactory.normalMessage(userId, payload)));
			out.flush();
		}
	}

	public String[] getUsers() {
		return users.toArray(new String[0]);
	}

	public void sendPrivately(String userId, Object payload) throws IOException {
		synchronized (WRITE_LOCK) {
			out.println(Parser.pojo2json(MessageFactory.privateMessage(this.userId, userId, payload)));
			out.flush();
		}
	}

}
