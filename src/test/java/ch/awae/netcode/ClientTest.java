package ch.awae.netcode;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class ClientTest {

	@Test
	public void usersMustSyncWithoutHandler() throws IOException, ConnectionException, InterruptedException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			NetcodeClient client1 = ncf.createChannel("test1", ChannelConfiguration.getDefault());
			NetcodeClient client2 = ncf.joinChannel("test2", client1.getChannelConfiguration().getChannelId());
			Thread.sleep(500);
			List<String> client1Users = Arrays.asList(client1.getUsers());
			List<String> client2Users = Arrays.asList(client2.getUsers());
			Assert.assertEquals(2, client2Users.size());
			Assert.assertTrue(client2Users.contains("test1"));
			Assert.assertTrue(client2Users.contains("test2"));
			Assert.assertEquals(2, client1Users.size());
			Assert.assertTrue(client1Users.contains("test1"));
			Assert.assertTrue(client1Users.contains("test2"));
			client2.disconnect();
			Thread.sleep(500);
			Assert.assertArrayEquals(new String[] { "test1" }, client1.getUsers());
		} finally {
			server.close();
			Thread.sleep(500);
		}

	}

	@Test
	public void messagesAreStoredUntilHandlerExists() throws IOException, ConnectionException, InterruptedException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			NetcodeClient client = ncf.createChannel("test1", ChannelConfiguration.getDefault());
			client.send("Hello There");
			Thread.sleep(500);
			client.setMessageHandler(m -> {
				Assert.assertEquals("Hello There", (String) m.getPayload());
			});
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test
	public void nullMessagesAreHandledWell() throws IOException, ConnectionException, InterruptedException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			NetcodeClient client = ncf.createChannel("test1", ChannelConfiguration.getDefault());
			client.send(null);
			Thread.sleep(500);
			client.setMessageHandler(m -> {
				Assert.assertNull(m.getPayload());
			});
		} finally {
			server.close();
			Thread.sleep(1000);
		}
	}

	@Test
	public void clientJoinsAreNotStoredIfHandlerIsNull() throws IOException, ConnectionException, InterruptedException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			NetcodeClient client = ncf.createChannel("test1", ChannelConfiguration.getDefault());
			client.send("Hello There");
			Thread.sleep(500);
			client.setMessageHandler(new MessageHandler() {

				@Override
				public void handleMessage(Message msg) {
					Assert.assertEquals("Hello There", msg.getPayload());
				}

				@Override
				public void clientJoined(String userId) {
					Assert.fail();
				}

			});
		} finally {
			server.close();
			Thread.sleep(1000);
		}
	}

	@Test
	public void privateMessagesAreInvisibleToOthers() throws IOException, ConnectionException, InterruptedException {
		Thread.sleep(1000);
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			NetcodeClient client1 = ncf.createChannel("test1", ChannelConfiguration.getDefault());
			NetcodeClient client2 = ncf.joinChannel("test2", client1.getChannelConfiguration().getChannelId());
			Thread.sleep(500);
			client1.sendPrivately("test2", "Hello There");
			Thread.sleep(500);
			client1.setMessageHandler(m -> Assert.fail());
			client2.setMessageHandler(m -> Assert.assertEquals("Hello There", (String) m.getPayload()));
		} finally {
			server.close();
			Thread.sleep(1000);
		}
	}

	@Test
	public void sentMessagesDontBounceIfDisabled() throws IOException, ConnectionException, InterruptedException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			ChannelConfiguration config = ChannelConfiguration.builder().bounceMessages(false).build();
			NetcodeClient client1 = ncf.createChannel("test1", config);
			NetcodeClient client2 = ncf.joinChannel("test2", client1.getChannelConfiguration().getChannelId());
			Thread.sleep(500);
			client1.send("Hello There");
			Thread.sleep(500);
			client1.setMessageHandler(m -> Assert.fail());
			client2.setMessageHandler(m -> Assert.assertEquals("Hello There", (String) m.getPayload()));
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test
	public void privateMessagesIgnoreBouncingConfig() throws IOException, ConnectionException, InterruptedException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			ChannelConfiguration config = ChannelConfiguration.builder().bounceMessages(false).build();
			NetcodeClient client1 = ncf.createChannel("test1", config);
			Thread.sleep(500);
			client1.sendPrivately("test1", "Hello There");
			Thread.sleep(500);
			client1.setMessageHandler(m -> Assert.assertEquals("Hello There", (String) m.getPayload()));
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test(expected = NullPointerException.class)
	public void onceSetHandlerCannotBeNulledAgain() throws IOException, ConnectionException, InterruptedException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			NetcodeClient client = ncf.createChannel("test1", ChannelConfiguration.getDefault());
			client.setMessageHandler(System.out::println);
			client.setMessageHandler(null);
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test(expected = NullPointerException.class)
	public void privateMessageToNullIsNotAllowed() throws IOException, ConnectionException, InterruptedException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			NetcodeClient client = ncf.createChannel("test1", ChannelConfiguration.getDefault());
			client.sendPrivately(null, "test");
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test
	public void clientJoinAndLeaveCreateEvents() throws IOException, ConnectionException, InterruptedException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			NetcodeClient client1 = ncf.createChannel("test1", ChannelConfiguration.getDefault());
			ClientJoinTrackingMH handler = new ClientJoinTrackingMH("test2");
			client1.setMessageHandler(handler);
			NetcodeClient client2 = ncf.joinChannel("test2", client1.getChannelConfiguration().getChannelId());
			Thread.sleep(500);
			Assert.assertTrue(handler.hasJoined);
			Assert.assertFalse(handler.hasLeft);
			client2.disconnect();
			Thread.sleep(500);
			Assert.assertTrue(handler.hasLeft);

		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test
	public void clientDoesNotSeeHisOwnJoinEvent() throws IOException, ConnectionException, InterruptedException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			ClientJoinTrackingMH handler = new ClientJoinTrackingMH("test1");
			ncf.setMessageHandler(handler);
			ncf.createChannel("test1", ChannelConfiguration.getDefault());
			Thread.sleep(500);
			Assert.assertFalse(handler.hasJoined);
			Assert.assertFalse(handler.hasLeft);
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test
	public void messagesAreReceivedInOrder() throws IOException, ConnectionException, InterruptedException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			CountingHandler ch = new CountingHandler();
			ncf.setMessageHandler(ch);
			NetcodeClient client = ncf.createChannel("test1", ChannelConfiguration.getDefault());

			Thread.sleep(500);

			for (int i = 0; i < 50000; i++)
				client.send(Integer.valueOf(i));

			Thread.sleep(10000);
			Assert.assertTrue(ch.ok);

		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test
	public void allClientsReceiveTheSameChannelConfig() throws IOException, ConnectionException, InterruptedException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			NetcodeClient client1 = ncf.createChannel("test1", ChannelConfiguration.getDefault());
			NetcodeClient client2 = ncf.joinChannel("test2", client1.getChannelConfiguration().getChannelId());
			Assert.assertEquals(client2.getChannelConfiguration(), client2.getChannelConfiguration());
		} finally {
			server.close();
		}
	}

	@Test
	public void configIsNotChangedByServer() throws IOException, ConnectionException, InterruptedException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			ChannelConfiguration config1 = ChannelConfiguration.getDefault();
			ChannelConfiguration config2 = ncf.createChannel("test1", config1).getChannelConfiguration();
			Assert.assertEquals(config1.getMaxClients(), config2.getMaxClients());
			Assert.assertEquals(config1.isBounceMessages(), config2.isBounceMessages());
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test(expected = InvalidChannelIdException.class)
	public void channelIdDoesNotLeakBetweenApps() throws IOException, ConnectionException, InterruptedException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf1 = new NetcodeClientFactory("localhost", 8888, "myApp");
			NetcodeClient client1 = ncf1.createChannel("test1", ChannelConfiguration.getDefault());

			NetcodeClientFactory ncf2 = new NetcodeClientFactory("localhost", 8888, "myOtherApp");
			ncf2.joinChannel("test2", client1.getChannelConfiguration().getChannelId());
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

}

class CountingHandler implements MessageHandler {

	volatile int counter = 0;
	volatile boolean ok = true;

	@Override
	public void handleMessage(Message msg) {
		int next = ((Integer) msg.getPayload()).intValue();
		if (counter != next)
			ok = false;
		counter++;
	}
}

class ClientJoinTrackingMH implements MessageHandler {

	public volatile boolean hasJoined = false;
	public volatile boolean hasLeft = false;

	private final String expectedUser;

	ClientJoinTrackingMH(String user) {
		expectedUser = user;
	}

	@Override
	public void handleMessage(Message msg) {
		// ignore
	}

	@Override
	public void clientJoined(String userId) {
		if (expectedUser.equals(userId))
			hasJoined = true;
	}

	@Override
	public void clientLeft(String userId) {
		if (expectedUser.equals(userId))
			hasLeft = true;
	}

}
