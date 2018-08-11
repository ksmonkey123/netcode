package ch.awae.netcode;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.function.Consumer;

import javax.net.ssl.SSLHandshakeException;

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

	@Test(expected = SSLHandshakeException.class)
	public void testIncompatibleSocketModes() throws IOException, ConnectionException, InterruptedException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			ncf.setMode(SocketMode.SSL, SecurityMode.ANY);
			ncf.createChannel("test", ChannelConfiguration.getDefault());
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test(expected = SSLHandshakeException.class)
	public void testIncompatibleSecurityModes() throws IOException, ConnectionException, InterruptedException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			ncf.setMode(SocketMode.TLS, SecurityMode.CERTIFICATE);
			ncf.createChannel("test", ChannelConfiguration.getDefault());
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

	@Test(expected = ChannelUserLimitReachedException.class)
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

	@Test(expected = DuplicateUserIdException.class)
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

	@Test(expected = InvalidChannelIdException.class)
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

	@Test(expected = UnsupportedFeatureException.class)
	public void cannotCreatePublicChannelIfDisabled() throws IOException, ConnectionException, InterruptedException {
		NetcodeServerFactory nsf = new NetcodeServerFactory(8888);
		nsf.setEnablePublicChannels(false);
		NetcodeServer server = nsf.start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			ncf.createChannel("test1", ChannelConfiguration.builder().publicChannel(true).build());
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test
	public void canCreatePrivateChannelIfPublicChannelsAreDisabled()
			throws IOException, ConnectionException, InterruptedException {
		NetcodeServerFactory nsf = new NetcodeServerFactory(8888);
		nsf.setEnablePublicChannels(false);
		NetcodeServer server = nsf.start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			ncf.createChannel("test1", ChannelConfiguration.getDefault());
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test(expected = InvalidAppIdException.class)
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

	@Test(expected = InvalidAppIdException.class)
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

	@Test
	public void canGetChannelList() throws IOException, ConnectionException, InterruptedException {
		NetcodeServerFactory nsf = new NetcodeServerFactory(8888);
		NetcodeServer server = nsf.start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			Assert.assertArrayEquals(new ChannelConfiguration[0], ncf.listPublicChannels());
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test(expected = InvalidAppIdException.class)
	public void cantGetChannelListForBadAppId() throws IOException, ConnectionException, InterruptedException {
		NetcodeServerFactory nsf = new NetcodeServerFactory(8888);
		nsf.setAppIdValidator(s -> false);
		NetcodeServer server = nsf.start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			ncf.listPublicChannels();
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test(expected = InvalidRequestException.class)
	public void serverHandlesUnsupportedSimpleQueries()
			throws IOException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException, ConnectionException, InterruptedException {
		NetcodeServerFactory nsf = new NetcodeServerFactory(8888);
		nsf.setAppIdValidator(s -> false);
		NetcodeServer server = nsf.start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			Method m = NetcodeClientFactory.class.getDeclaredMethod("initSocket");
			m.setAccessible(true);
			NetcodeClientImpl client = (NetcodeClientImpl) m.invoke(ncf);
			client.simpleQuery("this is a stupid query");
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test
	public void canSeeChannelListContents() throws IOException, ConnectionException, InterruptedException {
		NetcodeServerFactory nsf = new NetcodeServerFactory(8888);
		NetcodeServer server = nsf.start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			ncf.createChannel("test", ChannelConfiguration.builder().channelName("asdf").publicChannel(true).build());
			ChannelConfiguration[] list = ncf.listPublicChannels();
			Assert.assertEquals(1, list.length);
			ChannelConfiguration config = list[0];
			Assert.assertEquals("asdf", config.getChannelName());
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test
	public void canJoinChannelFromChannelList() throws IOException, ConnectionException, InterruptedException {
		NetcodeServerFactory nsf = new NetcodeServerFactory(8888);
		NetcodeServer server = nsf.start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			ncf.createChannel("test", ChannelConfiguration.builder().channelName("asdf").publicChannel(true).build());
			ChannelConfiguration[] list = ncf.listPublicChannels();
			Assert.assertEquals(1, list.length);
			Assert.assertEquals(list[0], ncf.joinChannel("test1", list[0].getChannelId()).getChannelConfiguration());
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test
	public void cantSeePrivateChannelsInChannelListContents()
			throws IOException, ConnectionException, InterruptedException {
		NetcodeServerFactory nsf = new NetcodeServerFactory(8888);
		NetcodeServer server = nsf.start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			ncf.createChannel("test", ChannelConfiguration.getDefault());
			Assert.assertArrayEquals(new ChannelConfiguration[0], ncf.listPublicChannels());
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test
	public void cantSeeFullChannelsInChannelListContents()
			throws IOException, ConnectionException, InterruptedException {
		NetcodeServerFactory nsf = new NetcodeServerFactory(8888);
		NetcodeServer server = nsf.start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			ChannelConfiguration config = ncf.createChannel("test",
					ChannelConfiguration.builder().channelName("asdf").publicChannel(true).maxClients(2).build())
					.getChannelConfiguration();
			Assert.assertEquals(1, ncf.listPublicChannels().length);
			ncf.joinChannel("test2", config.getChannelId());
			Assert.assertArrayEquals(new ChannelConfiguration[0], ncf.listPublicChannels());
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test(expected = UnsupportedFeatureException.class)
	public void cantGetChannelListIfDisabled() throws IOException, ConnectionException, InterruptedException {
		NetcodeServerFactory nsf = new NetcodeServerFactory(8888);
		nsf.setEnablePublicChannels(false);
		NetcodeServer server = nsf.start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			ncf.listPublicChannels();
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test
	public void canSetNullHandler() {
		new NetcodeClientFactory("localhost", 8888, "myApp").setMessageHandler(null);
	}

	@Test
	public void canSetNonNullHandler() {
		new NetcodeClientFactory("localhost", 8888, "myApp").setMessageHandler(System.out::println);
	}

	@Test
	public void canSetNullEventHandler() {
		new NetcodeClientFactory("localhost", 8888, "myApp").setEventHandler(null);
	}

	@Test
	public void canSetNonNullEventHandler() {
		new NetcodeClientFactory("localhost", 8888, "myApp").setEventHandler(new ChannelEventHandler() {
		});
	}

	@Test(expected = NullPointerException.class)
	public void canNotAddNullRunner() {
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
			Thread.sleep(500);
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