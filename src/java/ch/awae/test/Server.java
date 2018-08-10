package ch.awae.test;

import java.io.IOException;
import java.util.Arrays;

import ch.awae.netcode.ChannelConfiguration;
import ch.awae.netcode.Message;
import ch.awae.netcode.MessageHandler;
import ch.awae.netcode.NetcodeClient;
import ch.awae.netcode.NetcodeClientFactory;
import ch.awae.netcode.NetcodeServer;
import ch.awae.netcode.NetcodeServerFactory;
import ch.awae.netcode.exception.ConnectionException;

public class Server {

	public static void main(String[] args) throws IOException, InterruptedException, ConnectionException {
		// start server
		NetcodeServer server = new NetcodeServerFactory().bind(7777);

		// configure client
		ChannelConfiguration config = ChannelConfiguration.builder().maxClients(5).build();
		NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 7777, "app1");
		ncf.setMessageHandler(msg -> {
			System.out.println(Thread.currentThread().getName() + ": " + msg.getPayload());
		});

		// first client creates the channel
		NetcodeClient client = ncf.createChannel("test1", config);

		// other clients join the channel
		String channel = client.getChannelConfiguration().getChannelId();
		NetcodeClient client2 = ncf.joinChannel("test2", channel);
		NetcodeClient client3 = ncf.joinChannel("test3", channel);

		client.sendPrivately("test2", "hello user 2");
		client.send("hello all");
		
		System.out.println(Arrays.toString(client.getUsers()));

	}

}