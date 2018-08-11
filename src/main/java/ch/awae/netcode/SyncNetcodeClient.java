package ch.awae.netcode;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SyncNetcodeClient implements NetcodeClientBase, MessageHandler {

	private final NetcodeClient backer;
	private final BlockingQueue<Message> messageQueue;

	public SyncNetcodeClient(NetcodeClient backer) {
		this.backer = Objects.requireNonNull(backer);
		this.messageQueue = new LinkedBlockingQueue<>();
		this.backer.setMessageHandler(this);
	}

	@Override
	public void handleMessage(Message msg) {
		messageQueue.offer(msg);
	}

	@Override
	public void disconnect() throws IOException {
		this.backer.disconnect();
	}

	@Override
	public void send(Serializable payload) {
		this.backer.send(payload);
	}

	@Override
	public void sendPrivately(String userId, Serializable payload) {
		this.backer.sendPrivately(userId, payload);
	}

	@Override
	public String getUserId() {
		return this.backer.getUserId();
	}

	@Override
	public ChannelConfiguration getChannelConfiguration() {
		return this.backer.getChannelConfiguration();
	}

	@Override
	public String[] getUsers() {
		return this.backer.getUsers();
	}

}
