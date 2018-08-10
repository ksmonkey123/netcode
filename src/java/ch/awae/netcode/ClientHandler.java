package ch.awae.netcode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import lombok.Getter;

class ClientHandler extends Thread {

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
			e.printStackTrace();
			try {
				socket.close();
			} catch (IOException e2) {
				e.printStackTrace();
			}
		}
	}

	private void runLoop() throws IOException {
		while (!Thread.interrupted()) {
			MessageImpl msg = Parser.json2pojo(in.readLine(), MessageImpl.class);
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

	private Channel performHandshake() throws IOException {
		NetcodeHandshakeRequest request = Parser.json2pojo(in.readLine(), NetcodeHandshakeRequest.class);
		this.userId = request.getUserId();
		Channel channel = request.isMaster()
				? manager.createChannel(request.getAppId(), request.getConfig())
				: manager.getChannel(request.getAppId(), request.getChannelId());
		channel.join(request.getUserId(), this);
		return channel;
	};

}
