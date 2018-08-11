package ch.awae.netcode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import lombok.Getter;

final class ClientHandler extends Thread {

	private final Socket socket;
	private final ChannelManager manager;
	private final BufferedReader in;
	private final PrintWriter out;
	private Channel channel;
	private @Getter String userId;

	ClientHandler(ChannelManager manager, Socket socket) throws IOException {
		this.manager = manager;
		this.socket = socket;
		try {
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream());
		} catch (IOException e) {
			socket.close();
			throw e;
		}
	}

	public void run() {
		try {
			channel = performHandshake();
			try {
				runLoop();
			} finally {
				channel.quit(userId);
			}
		} catch (Exception e) {
			try {
				out.println(Parser.pojo2json(MessageFactory.serverMessage(e)));
				out.flush();
				socket.close();
			} catch (IOException e2) {
				e2.printStackTrace();
			}
		}
	}

	private void runLoop() throws IOException {
		while (!Thread.interrupted()) {
			String l = in.readLine();
			if (l == null)
				break;
			MessageImpl msg = Parser.json2pojo(l, MessageImpl.class);
			if (Thread.interrupted())
				break;
			if (!msg.isManagementMessage())
				channel.send(msg);
		}
	}

	void send(Message msg) throws IOException {
		out.println(Parser.pojo2json(msg));
		out.flush();
	}

	private Channel performHandshake() throws IOException, ConnectionException {
		out.println(Parser.PROTOCOL_VERSION);
		out.flush();
		NetcodeHandshakeRequest request = Parser.json2pojo(in.readLine(), NetcodeHandshakeRequest.class);
		this.userId = request.getUserId();
		Channel channel = request.isMaster() ? manager.createChannel(request.getAppId(), request.getConfig())
				: manager.getChannel(request.getAppId(), request.getChannelId());
		if (channel == null)
			throw new ConnectionException("unknown channel id: '" + request.getChannelId() + "'");
		channel.join(request.getUserId(), this);
		return channel;
	}

	public void close() throws IOException {
		this.interrupt();
		socket.close();
	};

}
