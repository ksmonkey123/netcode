package ch.awae.netcode;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class ClientTest {

	static ChannelConfiguration bouncing = ChannelConfiguration.builder().bounceMessages(true).build();

	@Test
	public void canUpdateTimeout() throws IOException, ConnectionException, InterruptedException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			NetcodeClient c = ncf.createChannel("test1", bouncing);
			c.setTimeout(1000);
			Assert.assertEquals(1000, c.getTimeout());
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void cantSetNegativeTimeout() throws IOException, ConnectionException, InterruptedException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			NetcodeClient c = ncf.createChannel("test1", bouncing);
			c.setTimeout(-1);
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test
	public void usersMustSyncWithoutHandler() throws IOException, ConnectionException, InterruptedException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			NetcodeClient client1 = ncf.createChannel("test1", bouncing);
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
			NetcodeClient client = ncf.createChannel("test1", bouncing);
			client.send("Hello There");
			Thread.sleep(500);
			Assert.assertEquals("Hello There", (String) client.receive().getPayload());
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
			NetcodeClient client = ncf.createChannel("test1", bouncing);
			client.send(null);
			Thread.sleep(500);
			Assert.assertNull(client.receive().getPayload());
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
			NetcodeClient client1 = ncf.createChannel("test1", bouncing);
			NetcodeClient client2 = ncf.joinChannel("test2", client1.getChannelConfiguration().getChannelId());
			Thread.sleep(500);
			client1.sendPrivately("test2", "Hello There");
			Assert.assertEquals("Hello There", (String) client2.receive().getPayload());
			Thread.sleep(500);
			Assert.assertNull(client1.tryReceive());
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
			Assert.assertEquals("Hello There", (String) client2.receive().getPayload());
			Thread.sleep(500);
			Assert.assertNull(client1.tryReceive());
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test
	public void privateMessagesIgnoreBouncingConfig() throws IOException, ConnectionException, InterruptedException {
		Thread.sleep(1000);
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			ChannelConfiguration config = ChannelConfiguration.builder().bounceMessages(false).build();
			NetcodeClient client1 = ncf.createChannel("test1", config);
			Thread.sleep(500);
			client1.sendPrivately("test1", "Hello There");
			Thread.sleep(500);
			Assert.assertEquals("Hello There", (String) client1.receive().getPayload());
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test
	public void handlerCanBeUnset() throws IOException, ConnectionException, InterruptedException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			NetcodeClient client = ncf.createChannel("test1", bouncing);
			client.setMessageHandler(System.out::println);
			client.setMessageHandler(null);
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test
	public void eventHandlerCanSetAndBeUnset() throws IOException, ConnectionException, InterruptedException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			NetcodeClient client = ncf.createChannel("test1", bouncing);
			client.setEventHandler(new ChannelEventHandler() {
			});
			client.setMessageHandler(null);
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test
	public void userRefCanBeGeneratedAndAccessed() throws InterruptedException, IOException, ConnectionException {
		Thread.sleep(1000);
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			ChannelConfiguration config = ChannelConfiguration.builder().bounceMessages(false).build();
			NetcodeClient client1 = ncf.createChannel("test1", config);
			Thread.sleep(500);
			client1.getUserRef("test1").send("Hello There");
			Thread.sleep(500);
			Assert.assertEquals("Hello There", (String) client1.receive().getPayload());
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test(expected = NullPointerException.class)
	public void nullUserRefIsNotOK() throws InterruptedException, IOException, ConnectionException {
		Thread.sleep(1000);
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			ChannelConfiguration config = ChannelConfiguration.builder().bounceMessages(false).build();
			NetcodeClient client1 = ncf.createChannel("test1", config);
			Thread.sleep(500);
			client1.getUserRef(null);
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void invalidUserRefIsNotOK() throws InterruptedException, IOException, ConnectionException {
		Thread.sleep(1000);
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			ChannelConfiguration config = ChannelConfiguration.builder().bounceMessages(false).build();
			NetcodeClient client1 = ncf.createChannel("test1", config);
			Thread.sleep(500);
			client1.getUserRef("test2");
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}
	
	

	@Test(expected = NullPointerException.class)
	public void privateMessageToNullIsNotAllowed() throws IOException, ConnectionException, InterruptedException {
		Thread.sleep(1000);
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			NetcodeClient client = ncf.createChannel("test1", bouncing);
			client.sendPrivately(null, "test");
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void privateMessageToMissingClientIsNotAllowed()
			throws IOException, ConnectionException, InterruptedException {
		Thread.sleep(1000);
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			NetcodeClient client = ncf.createChannel("test1", bouncing);
			client.sendPrivately("test2", "test");
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
			NetcodeClient client1 = ncf.createChannel("test1", bouncing);
			ClientJoinTrackingMH handler = new ClientJoinTrackingMH("test2");
			client1.setEventHandler(handler);
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
			ncf.setEventHandler(handler);
			ncf.createChannel("test1", bouncing);
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
			NetcodeClient client = ncf.createChannel("test1", bouncing);

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
			NetcodeClient client1 = ncf.createChannel("test1", bouncing);
			NetcodeClient client2 = ncf.joinChannel("test2", client1.getChannelConfiguration().getChannelId());
			Assert.assertEquals(client2.getChannelConfiguration(), client2.getChannelConfiguration());
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test(expected = InvalidRequestException.class)
	public void rejectBadMaxMemberValue() throws IOException, ConnectionException, InterruptedException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			ncf.createChannel("test1", ChannelConfiguration.builder().maxClients(-1).build());
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test
	public void configIsNotChangedByServer() throws IOException, ConnectionException, InterruptedException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			ChannelConfiguration config1 = bouncing;
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
			NetcodeClient client1 = ncf1.createChannel("test1", bouncing);

			NetcodeClientFactory ncf2 = new NetcodeClientFactory("localhost", 8888, "myOtherApp");
			ncf2.joinChannel("test2", client1.getChannelConfiguration().getChannelId());
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test(timeout = 5000)
	public void ifNoHandlerExistsQueueCanBeRead() throws IOException, ConnectionException, InterruptedException {
		Thread.sleep(1000);
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf1 = new NetcodeClientFactory("localhost", 8888, "myApp");
			NetcodeClient client1 = ncf1.createChannel("test1", bouncing);
			client1.send("hello there");
			String s = (String) client1.receive().getPayload();
			Assert.assertEquals("hello there", s);
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test
	public void inAsyncModeSynchronousReadsBlock() throws IOException, ConnectionException, InterruptedException {
		Thread w = new Thread(() -> {
			try {
				NetcodeServer server = new NetcodeServerFactory(8888).start();
				try {
					NetcodeClientFactory ncf1 = new NetcodeClientFactory("localhost", 8888, "myApp");
					ncf1.setMessageHandler(m -> {
					});
					NetcodeClient client1 = ncf1.createChannel("test1", bouncing);
					client1.send("hello there");
					client1.receive();
				} finally {
					server.close();
					Thread.sleep(500);
				}
			} catch (Exception e) {
			}
		});
		try {
			w.start();
			Thread.sleep(5000);
			Assert.assertTrue(w.isAlive());
		} finally {
			w.interrupt();
		}
	}

	@Test(timeout = 5000)
	public void syncQueueIsReactivatedOnHandlerSetToNull()
			throws InterruptedException, IOException, ConnectionException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf1 = new NetcodeClientFactory("localhost", 8888, "myApp");
			ncf1.setMessageHandler(m -> {
			});
			NetcodeClient client = ncf1.createChannel("test1", bouncing);
			client.send("hello there");
			Thread.sleep(500);
			client.setMessageHandler(null);
			client.send("hi there");
			String s = client.receive().getPayload().toString();
			Assert.assertEquals("hi there", s);
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test
	public void aBadHandlerCanNotCrashThreads() throws InterruptedException, IOException, ConnectionException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf1 = new NetcodeClientFactory("localhost", 8888, "myApp");
			NetcodeClient client = ncf1.createChannel("test1", bouncing);
			client.send("hello there");
			Thread.sleep(500);
			client.setMessageHandler(m -> {
				throw new RuntimeException();
			});
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test
	public void aBadEventHandlerCanNotCrashThreads() throws InterruptedException, IOException, ConnectionException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			CrashingTrackingEH eh = new CrashingTrackingEH();
			NetcodeClient client1 = ncf.createChannel("test1", bouncing);
			client1.setEventHandler(eh);
			String channel = client1.getChannelConfiguration().getChannelId();
			ncf.joinChannel("test2", channel);
			ncf.joinChannel("test3", channel);
			Thread.sleep(1000);
			Assert.assertEquals(2, eh.runs);
		} finally {
			server.close();
			Thread.sleep(500);
		}

	}

	@Test
	public void canCreateAChannelFromProvidedConfig() throws InterruptedException, IOException, ConnectionException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			ChannelConfiguration configuration = ncf.createChannel("test1", bouncing).getChannelConfiguration();
			ncf.createChannel("test2", configuration);

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

class CrashingTrackingEH implements ChannelEventHandler {
	public volatile int runs = 0;

	@Override
	public void clientJoined(String userId) {
		runs++;
		throw new RuntimeException();
	}
}

class ClientJoinTrackingMH implements ChannelEventHandler {

	public volatile boolean hasJoined = false;
	public volatile boolean hasLeft = false;

	private final String expectedUser;

	ClientJoinTrackingMH(String user) {
		expectedUser = user;
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
