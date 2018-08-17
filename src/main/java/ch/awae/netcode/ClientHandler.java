package ch.awae.netcode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.Socket;

import lombok.Getter;

/**
 * Server-Side Thread handling a single client connection.
 * 
 * Since netcode 2.0.0 this thread also handles server commands as well as
 * short-lived connections (simple queries).
 */
final class ClientHandler extends Thread {

    /* Fixed Fields (socket, I/O streams, ...) */
	private final Socket socket;
	private final ChannelManager manager;
	private final BufferedReader in;
	private final PrintWriter out;
	private final ServerCapabilities features;
	
	/*
	 * (Fixed) Fields populated only after the initial netcode handshake has
	 * concluded. They are populated AFTER request validation. If the client
	 * request contains a simple query these fields DO NOT get populated!
	 */
	private @Getter String userId;
	private Channel channel;
	private String appId;

	ClientHandler(ChannelManager manager, Socket socket, ServerCapabilities features) throws IOException {
		this.manager = manager;
		this.socket = socket;
		this.features = features;
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
				if (channel != null)
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
			SSMessageImpl msg = Parser.json2pojo(l, SSMessageImpl.class);
			if (Thread.interrupted())
				break;
			if (!msg.isManagementMessage())
				channel.send(msg);
			else if (msg.getPayload() instanceof ServerCommand) {
				ServerCommand command = (ServerCommand) msg.getPayload();
				try {
					Serializable data = processServerCommand(command.getVerb(), command.getData());
					out.println(Parser.pojo2json(
							MessageFactory.serverMessage(new ServerCommandResponse(command.getCommandId(), data))));
					out.flush();
				} catch (Exception e) {
					out.println(Parser.pojo2json(
							MessageFactory.serverMessage(new ServerCommandResponse(command.getCommandId(), e))));
					out.flush();
				}
			}
		}
	}

	private Serializable processServerCommand(String verb, Serializable data)
			throws UnsupportedFeatureException, InvalidAppIdException, InvalidRequestException {
		if (!features.isEnableServerCommands())
			throw new UnsupportedFeatureException("server commands are disabled on this server");
		if (verb == null)
			throw new InvalidRequestException("unknown command: " + verb);
		// client list
		switch (verb) {
		case "get_channel_info":
			return channel.getInfo();
		case "get_channel_list":
			if (!features.isEnablePublicChannels())
				throw new UnsupportedFeatureException("public channels are disabled on this server");
			return manager.getPublicChannels(appId).toArray(new ChannelInformation[0]);
		default:
			throw new InvalidRequestException("unknown command: " + verb);
		}
	}

	void send(Message msg) throws IOException {
		out.println(Parser.pojo2json(msg));
		out.flush();
	}

	private Channel performHandshake() throws IOException, ConnectionException, InterruptedException {
		out.println(features.getFeaturesString(Parser.SERVER_VERSION));
		out.flush();
		NetcodeHandshakeRequest request = Parser.json2pojo(in.readLine(), NetcodeHandshakeRequest.class);
		if (!request.isMaster() && request.getAppId() == null && request.getChannelId() == null
				&& request.getUserId() == null && request.getConfig() == null) {
			processSimpleRequest();
			this.interrupt();
			return null;
		}
		validate(request);
		this.userId = request.getUserId();
		this.appId = request.getAppId();
		Channel channel = request.isMaster()
				? manager.createChannel(request.getAppId(), request.getConfig(), request.getUserId())
				: manager.getChannel(request.getAppId(), request.getChannelId());
		if (channel == null)
			throw new InvalidChannelIdException("unknown channel id: '" + request.getChannelId() + "'");
		channel.join(request.getUserId(), this);
		return channel;
	}

	private void processSimpleRequest()
			throws IOException, InvalidAppIdException, UnsupportedFeatureException, InvalidRequestException {
		String request = in.readLine();
		if (request.startsWith("channel_list:")) {
			if (!features.isEnablePublicChannels())
				throw new UnsupportedFeatureException("public channels are disabled on this server");
			String appId = request.substring(13);
			out.println(Parser.pojo2json(
					MessageFactory.serverMessage(manager.getPublicChannels(appId).toArray(new ChannelInformation[0]))));
			out.flush();
		} else {
			throw new InvalidRequestException("unsupported simple query: " + request);
		}
	}

	private void validate(NetcodeHandshakeRequest request) throws InvalidRequestException, InvalidAppIdException {
		if (request.getUserId() == null)
			throw new InvalidRequestException("invalid request: userId may not be null");
		if (request.getAppId() == null)
			throw new InvalidRequestException("invalid request: appId may not be null");
		if (!request.getAppId().matches("[a-zA-Z0-9_]+"))
			throw new InvalidAppIdException("app id must be of the pattern [a-zA-Z0-9_]+");
		if (request.isMaster() && request.getChannelId() != null)
			throw new InvalidRequestException(
					"invalid request: requested channel creation but sent along a channel id");
		if (!request.isMaster() && request.getChannelId() == null)
			throw new InvalidRequestException("invalid request: requested channel joining but missing channel id");
		if (request.isMaster() && request.getConfig() == null)
			throw new InvalidRequestException("invalid request: channel configuration missing");
		if (!request.isMaster())
			return;
		ChannelConfiguration config = request.getConfig();
		if (config.getMaxClients() < 2)
			throw new InvalidRequestException("invalid request: at least 2 clients must be allowed");
		if (config.isPublicChannel() && !features.isEnablePublicChannels())
			throw new InvalidRequestException("invalid request: public channels are disabled on this server");
	}

	public void close() throws IOException {
		this.interrupt();
		socket.close();
	};

}
