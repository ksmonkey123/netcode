package ch.awae.test;

import java.io.IOException;

import ch.awae.netcode.ChannelConfiguration;
import ch.awae.netcode.Message;
import ch.awae.netcode.MessageHandler;
import ch.awae.netcode.NetcodeClient;
import ch.awae.netcode.NetcodeClientFactory;
import ch.awae.netcode.NetcodeServer;
import ch.awae.netcode.NetcodeServerFactory;

public class Server {

	public static void main(String[] args) throws IOException {
		// start server
		new NetcodeServerFactory().bind(7777);

		// configure client
		ChannelConfiguration config = ChannelConfiguration.getDefault();
		NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 7777, "app1");
		ncf.setMessageHandler(new Handler());
		
		// first client creates the channel
		NetcodeClient client = ncf.createChannel("test1", config);
		
		// other clients join the channel
		String channel = client.getChannelConfiguration().getChannelId();
		NetcodeClient client2 = ncf.joinChannel("test2", channel);

		// start sending data
		client.send("hello there");
		client2.send("hello there");
	}

}

class Handler implements MessageHandler {

	@Override
	public void handleMessage(Message msg) {
		System.out.println(msg);
	}

	@Override
	public void clientJoined(String userId) {
		System.out.println(userId + " joined");
	}

	@Override
	public void clientLeft(String userId) {
		System.out.println(userId + " left");
	}

}