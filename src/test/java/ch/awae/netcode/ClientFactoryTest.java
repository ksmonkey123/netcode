package ch.awae.netcode;

import java.io.IOException;
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
	public void negativeTimeoutIllegal() {
		new NetcodeClientFactory("localhost", 8888, "myApp").setTimeout(-1);
	}

	@Test
	public void noTimeoutIsFine() {
		NetcodeClientFactory f = new NetcodeClientFactory("localhost", 8888, "myApp");
		f.setTimeout(0);
		Assert.assertEquals(0, f.getTimeout());
	}

	@Test
	public void positiveTimeoutIsFine() {
		NetcodeClientFactory f = new NetcodeClientFactory("localhost", 8888, "myApp");
		f.setTimeout(1000);
		Assert.assertEquals(1000, f.getTimeout());
	}

	@Test
	public void canSetQuestionHandler() {
		NetcodeClientFactory f = new NetcodeClientFactory("localhost", 8888, "myApp");
		f.setQuestionHandler((x, y) -> y);
	}

	@Test
	public void canSetNullQuestionHandler() {
		NetcodeClientFactory f = new NetcodeClientFactory("localhost", 8888, "myApp");
		f.setQuestionHandler(null);
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
	public void userNotNull2() throws IOException, ConnectionException {
		NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
		ncf.joinChannel(null, "stuff");
	}

	@Test(expected = IllegalArgumentException.class)
	public void userNotEmpty() throws IOException, ConnectionException {
		NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
		ncf.createChannel("", ChannelConfiguration.getDefault());
	}

	@Test(expected = IllegalArgumentException.class)
	public void userNotEmpty2() throws IOException, ConnectionException {
		NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
		ncf.joinChannel("", "stuff");
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
			ncf.createChannel("test1", ChannelConfiguration.getDefault());
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test(expected = InvalidAppIdException.class)
	public void cannotCreateChannelWithMalformedAppId() throws IOException, ConnectionException, InterruptedException {
		NetcodeServerFactory nsf = new NetcodeServerFactory(8888);
		nsf.setAppIdValidator(s -> false);
		NetcodeServer server = nsf.start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp-isSoGreat!");
			ncf.createChannel("test1", ChannelConfiguration.getDefault());
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test
	public void serverAcceptsValidCustomAppIdValidation()
			throws IOException, ConnectionException, InterruptedException {
		NetcodeServerFactory nsf = new NetcodeServerFactory(8888);
		nsf.setAppIdValidator(s -> s.length() < 10);
		NetcodeServer server = nsf.start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "my");
			ncf.createChannel("test1", ChannelConfiguration.getDefault());
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test(expected = InvalidAppIdException.class)
	public void serverRejectsMalformedAppIdEvenWithCustomValidator()
			throws IOException, ConnectionException, InterruptedException {
		NetcodeServerFactory nsf = new NetcodeServerFactory(8888);
		nsf.setAppIdValidator(s -> s.length() < 10);
		NetcodeServer server = nsf.start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "my-great");
			ncf.createChannel("test1", ChannelConfiguration.getDefault());
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

	@Test(expected = InvalidAppIdException.class)
	public void cannotJoinChannelWithMalformedAppId() throws IOException, ConnectionException, InterruptedException {
		NetcodeServerFactory nsf = new NetcodeServerFactory(8888);
		nsf.setAppIdValidator(s -> false);
		NetcodeServer server = nsf.start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp-isSoGreat!");
			ncf.joinChannel("test1", "testas");
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