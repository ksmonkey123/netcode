package ch.awae.netcode;

import java.io.IOException;
import java.net.Socket;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Test;

public class ClientFactoryTest {

	@Test(expected = NullPointerException.class)
	public void hostMayNotBeNull() {
		new NetcodeClientFactory(null, 9999, "myApp");
	}

	@Test(expected = IllegalArgumentException.class)
	public void portMayNotBeNegative() {
		new NetcodeClientFactory("localhost", -1, "myApp");
	}

	@Test(expected = IllegalArgumentException.class)
	public void portMustNotBeTooLarge() {
		new NetcodeClientFactory("localhost", 65536, "myApp");
	}

	@Test(expected = NullPointerException.class)
	public void appIdMayNotBeNull() {
		new NetcodeClientFactory("localhost", 8888, null);
	}

	@Test(expected = NullPointerException.class)
	public void socketModeNotNull() {
		NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
		ncf.setMode(null, SecurityMode.ANY);
	}

	@Test(expected = NullPointerException.class)
	public void securityModeNotNull() {
		NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
		ncf.setMode(SocketMode.PLAIN, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void illegalModeCombination() {
		NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
		ncf.setMode(SocketMode.PLAIN, SecurityMode.CERTIFICATE);
	}

	@Test(expected = NullPointerException.class)
	public void userNotNull() throws IOException, ConnectionException {
		NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
		ncf.createChannel(null, ChannelConfiguration.getDefault());
	}

	@Test(expected = NullPointerException.class)
	public void channelConfigNotNull() throws IOException, ConnectionException {
		NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
		ncf.createChannel("test", null);
	}

	@Test
	public void testConnection() throws IOException, ConnectionException, InterruptedException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			NetcodeClient client = ncf.createChannel("test", ChannelConfiguration.getDefault());
			Assert.assertEquals("test", client.getUserId());
			Thread.sleep(500);
			Assert.assertArrayEquals(new String[] { "test" }, client.getUsers());
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test
	public void testJoining() throws IOException, ConnectionException, InterruptedException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			NetcodeClient client1 = ncf.createChannel("test1", ChannelConfiguration.getDefault());
			NetcodeClient client2 = ncf.joinChannel("test2", client1.getChannelConfiguration().getChannelId());
			Assert.assertEquals(2, client2.getUsers().length);
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test(expected = NullPointerException.class)
	public void cannotJoinAsNull() throws IOException, ConnectionException, InterruptedException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			NetcodeClient client1 = ncf.createChannel("test1", ChannelConfiguration.getDefault());
			ncf.joinChannel(null, client1.getChannelConfiguration().getChannelId());
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test(expected = NullPointerException.class)
	public void cannotJoinNullChannel() throws IOException, ConnectionException, InterruptedException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			ncf.createChannel("test1", ChannelConfiguration.getDefault());
			ncf.joinChannel("test2", null);
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test(expected = ConnectionException.class)
	public void cannotJoinFullChannel() throws IOException, ConnectionException, InterruptedException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			String channel = ncf.createChannel("test1", ChannelConfiguration.builder().maxClients(2).build())
					.getChannelConfiguration().getChannelId();
			ncf.joinChannel("test2", channel);
			ncf.joinChannel("test3", channel);
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test(expected = ConnectionException.class)
	public void cannotJoinWithDuplicateUserId() throws IOException, ConnectionException, InterruptedException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			String channel = ncf.createChannel("test1", ChannelConfiguration.getDefault()).getChannelConfiguration()
					.getChannelId();
			ncf.joinChannel("test1", channel);
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test(expected = ConnectionException.class)
	public void cannotJoinMissingChannel() throws IOException, ConnectionException, InterruptedException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			ncf.createChannel("test1", ChannelConfiguration.getDefault());
			ncf.joinChannel("test2", "CHANEL");
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test(expected = ConnectionException.class)
	public void cannotCreateChannelOnIllegalAppId() throws IOException, ConnectionException, InterruptedException {
		NetcodeServerFactory nsf = new NetcodeServerFactory(8888);
		nsf.setAppIdValidator(s -> false);
		NetcodeServer server = nsf.start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			String channel = ncf.createChannel("test1", ChannelConfiguration.getDefault()).getChannelConfiguration()
					.getChannelId();
			ncf.joinChannel("test1", channel);
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test(expected = ConnectionException.class)
	public void cannotJoinChannelWithIllegalAppId() throws IOException, ConnectionException, InterruptedException {
		NetcodeServerFactory nsf = new NetcodeServerFactory(8888);
		nsf.setAppIdValidator(s -> false);
		NetcodeServer server = nsf.start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			ncf.joinChannel("test1", "testas");
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test(expected = NullPointerException.class)
	public void cannotAddNullHandler() {
		new NetcodeClientFactory("localhost", 8888, "myApp").runAfterBind(null);
	}

	public void bindersAreRun() throws IOException, ConnectionException, InterruptedException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			InvocationTrackingConsumer<Socket> f = new InvocationTrackingConsumer<>();
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			ncf.runAfterBind(f);
			ncf.createChannel("test", ChannelConfiguration.getDefault());
			Thread.sleep(500);
			Assert.assertTrue(f.run);
		} finally {
			server.close();
		}
	}

}

class InvocationTrackingConsumer<T> implements Consumer<T> {
	public volatile boolean run = false;

	@Override
	public void accept(T t) {
		System.out.println("run");
		run = true;
	}
}