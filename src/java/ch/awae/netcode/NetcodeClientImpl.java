package ch.awae.netcode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

final class NetcodeClientImpl extends Thread implements NetcodeClient {

	private final Socket socket;
	private final BufferedReader in;
	private final PrintWriter out;
	private @Getter String userId;
	private ChannelConfiguration config;
	private final MessageHandler messageHandler;
	private final Object WRITE_LOCK = new Object();

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

	public void open(NetcodeHandshakeRequest request) throws IOException {
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
		//System.out.println(" > processing " + m);
		if (m.isManagementMessage() && m.getPayload() instanceof UserChange) {
			UserChange data = (UserChange) m.getPayload();
			if (data.isJoined())
				messageHandler.clientJoined(data.getUserId());
			else
				messageHandler.clientLeft(data.getUserId());
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
					process(Parser.json2pojo(in.readLine(), MessageImpl.class));
				} catch (IOException e) {
					e.printStackTrace();
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
	public void close() throws IOException {
		socket.close();
	}

	@Override
	public void send(Object payload) throws IOException {
		synchronized (WRITE_LOCK) {
			out.println(Parser.pojo2json(MessageFactory.normalMessage(userId, payload)));
			out.flush();
		}
	}

	@Override
	public void sendPrivately(String userId, Object payload) throws IOException {
		synchronized (WRITE_LOCK) {
			out.println(Parser.pojo2json(MessageFactory.privateMessage(this.userId, userId, payload)));
			out.flush();
		}
	}

}
