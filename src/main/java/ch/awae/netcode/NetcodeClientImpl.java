package ch.awae.netcode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import lombok.Getter;
import lombok.Setter;

final class NetcodeClientImpl extends Thread implements NetcodeClient {

    // network
	private final Socket socket;
	private final BufferedReader in;
	private final PrintWriter out;
	
	// client/channel information
	private final List<String> users = new ArrayList<>();
	private @Getter String userId;
	private ChannelConfiguration config;
	
	// optional server features
	private boolean supportsPC = false;
	private boolean supportsSC = false;
	
	// server commands
	private ConcurrentHashMap<Long, Promise> pendingCommands = new ConcurrentHashMap<>();
	private AtomicLong commandIndex = new AtomicLong();
	
	// message handling
	private final Object HANDLER_LOCK = new Object();
	private final BlockingQueue<MessageImpl> backlog = new LinkedBlockingQueue<>();
	private MessageHandler messageHandler;
	private @Setter ChannelEventHandler eventHandler;
	private @Setter ClientQuestionHandler questionHandler;

	NetcodeClientImpl(Socket s, MessageHandler messageHandler, ChannelEventHandler eventHandler)
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
			processServerVersionInfo(line);
			if (!supportsPC && request.getConfig().isPublicChannel())
				throw new UnsupportedFeatureException("public channels not supported by server");
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

	private void processServerVersionInfo(String line) throws IncompatibleServerException {
		List<String> versions = Arrays.asList(line.split(","));

		if (!versions.contains("NETCODE_1"))
			throw new IncompatibleServerException("incompatible server version: expected '" + Parser.SERVER_VERSION
					+ "' but received '" + line + "'");

		if (versions.contains(Parser.PUBLIC_CHANNELS))
			supportsPC = true;

		if (versions.contains(Parser.SERVER_COMMANDS))
			supportsSC = true;
	}

	private void handleManagementMessage(MessageImpl msg) {
		try {
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
			} else if (payload instanceof ServerCommandResponse) {
				handleResponseToSC((ServerCommandResponse) payload);
			}
		} catch (Exception e) {
			System.err.println("an error occured while processing event: " + msg);
			e.printStackTrace();
		}
	}

	private void handleResponseToSC(ServerCommandResponse payload) {
		Long command = Long.valueOf(payload.getCommandId());
		pendingCommands.remove(command).fulfill(payload.getData());
	}

	private void process(MessageImpl m) {
		if (m.isManagementMessage()) {
			handleManagementMessage(m);
		} else {
		    // wait if handler is being reconfigured and catching up...
		    synchronized(HANDLER_LOCK) {
	        	if (messageHandler != null) {
			        try {
				        if (m.isPrivateMessage()) {
				            if (m.getPayload() instanceof ClientQuestion)
				                handleQuestion(m);
				            else
				                messageHandler.handlePrivateMessage(m, m.getUserId());
				        } else
					        messageHandler.handleMessage(m);
			        } catch (Exception e) {
				        System.err.println("an error occured while processing a message: " + m);
				        e.printStackTrace();
			        }
		        } else {
			        backlog.add(m);
		        }
		    }
		}
	}
	
	private void handleQuestion(MessageImpl m) {
	    ClientQuestion q = (ClientQuestion) m.getPayload();
	    String from = m.getUserId();
	    Object payload = null;
	    try {
	        ClientQuestionHandler cqh = this.questionHandler;
	        if (cqh == null)
	            throw new UnsupportedOperationException("no question handler defined");
	        else
	            q = cqh.handleQuestion(from, q.getData());
	    } catch (Exception e) {
	        payload = e;    
	    }
	    out.println(Parser.pojo2json(MessageFactory.privateMessage(this.userId, from, q.response(payload))));
		out.flush();
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
		out.println(Parser.pojo2json(MessageFactory.normalMessage(userId, payload)));
		out.flush();
	}

	public String[] getUsers() {
		return users.toArray(new String[0]);
	}

	@Override
	public void sendPrivately(String userId, Serializable payload) {
		Objects.requireNonNull(userId);
		if (!this.users.contains(userId))
			throw new IllegalArgumentException("unknown client: " + userId);
		out.println(Parser.pojo2json(MessageFactory.privateMessage(this.userId, userId, payload)));
		out.flush();
	}

	@Override
	public void setMessageHandler(MessageHandler handler) {
	    synchronized(HANDLER_LOCK) {
		    this.messageHandler = handler;
		    if (this.messageHandler != null) {
			    while (!backlog.isEmpty()) {
				    MessageImpl m = backlog.poll();
				    try {
				        if (m.isPrivateMessage()) {
				            if (m.getPayload() instanceof ClientQuestion)
				                handleQuestion(m);
				            else
				                messageHandler.handlePrivateMessage(m, m.getUserId());
				        } else
					        messageHandler.handleMessage(m);
				    } catch (Exception e) {
					    System.err.println("an error occured while processing a message: " + m);
					    e.printStackTrace();
				    }
			    }
	        }
	    }
	}
	
	@Override
	public Message receive() throws InterruptedException {
		return this.backlog.take();
	}

	@Override
	public Message tryReceive() {
		return this.backlog.poll();
	}

	Object simpleQuery(String string) throws IOException, ConnectionException {
		try {
			String line = in.readLine();
			List<String> versions = Arrays.asList(line.split(","));
			if (!versions.contains(Parser.SIMPLE_TALK))
				throw new UnsupportedFeatureException("simple queries not supported by server");
			out.println(Parser.pojo2json(new NetcodeHandshakeRequest(null, null, null, false, null)));
			out.println(string);
			out.flush();
			MessageImpl msg = Parser.json2pojo(in.readLine(), MessageImpl.class);
			if (msg.getPayload() instanceof Throwable) {
				if (msg.getPayload() instanceof ConnectionException)
					throw (ConnectionException) msg.getPayload();
				throw new RuntimeException((Throwable) msg.getPayload());
			}
			return msg.getPayload();
		} finally {
			socket.close();
		}
	}

	private Serializable runServerCommand(String verb, Serializable data)
			throws InterruptedException, ConnectionException {
		if (!supportsSC)
			throw new UnsupportedFeatureException("server commands not supported by server");
		long id = commandIndex.incrementAndGet();
		MessageImpl message = MessageFactory.serverMessage(new ServerCommand(id, verb, data));
		Promise promise = new Promise();
		if (pendingCommands.putIfAbsent(id, promise) != null)
			throw new AssertionError("duplicate server command id");
		out.println(Parser.pojo2json(message));
		out.flush();
		return promise.get();
	}

	@Override
	public ChannelInformation[] getPublicChannels() throws InterruptedException, ConnectionException {
		return (ChannelInformation[]) runServerCommand("get_channel_list", null);
	}

	@Override
	public ChannelInformation getChannelInformation() throws InterruptedException, ConnectionException {
		return (ChannelInformation) runServerCommand("get_channel_info", null);
	}

}
