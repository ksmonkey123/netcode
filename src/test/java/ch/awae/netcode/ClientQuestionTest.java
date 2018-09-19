package ch.awae.netcode;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Test;

public class ClientQuestionTest {

	@Test
	public void testEcho() throws IOException, ConnectionException, InterruptedException, TimeoutException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			NetcodeClient client1 = ncf.createChannel("test1", ChannelConfiguration.getDefault());
			NetcodeClient client2 = ncf.joinChannel("test2", client1.getChannelConfiguration().getChannelId());

			client2.setQuestionHandler((x, y) -> {
				return y;
			});

			Thread.sleep(500);
			
			Assert.assertEquals("test", client1.ask("test2", "test"));

		} finally {
			server.close();
			Thread.sleep(1000);
		}
	}

	@Test
	public void testEchoThroughUserRef()
			throws IOException, ConnectionException, InterruptedException, TimeoutException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			NetcodeClient client1 = ncf.createChannel("test1", ChannelConfiguration.getDefault());
			NetcodeClient client2 = ncf.joinChannel("test2", client1.getChannelConfiguration().getChannelId());

			client2.setQuestionHandler((x, y) -> {
				return y;
			});

			Thread.sleep(500);

			Assert.assertEquals("test", client1.getUserRef("test2").ask("test"));

		} finally {
			server.close();
			Thread.sleep(1000);
		}
	}

	@Test
	public void testTimeoutCleared() throws IOException, ConnectionException, InterruptedException, TimeoutException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			ncf.setTimeout(2000);
			NetcodeClient client1 = ncf.createChannel("test1", ChannelConfiguration.getDefault());
			NetcodeClient client2 = ncf.joinChannel("test2", client1.getChannelConfiguration().getChannelId());
			client2.setQuestionHandler((x, y) -> {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				return y;
			});

			Thread.sleep(500);

			try {
				client1.ask("test2", "test");
				Assert.fail();
			} catch (TimeoutException e) {
			}

			client1.setTimeout(0);

			Assert.assertEquals("test", client1.ask("test2", "test"));

		} finally {
			server.close();
			Thread.sleep(1000);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testMissingRecipient() throws IOException, ConnectionException, InterruptedException, TimeoutException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			ncf.setTimeout(5000);
			NetcodeClient client1 = ncf.createChannel("test1", ChannelConfiguration.getDefault());
			
			client1.ask("test2", "test");
		} finally {
			server.close();
			Thread.sleep(1000);
		}
	}
	
	@Test(expected = NullPointerException.class)
	public void testEmptyRecipient() throws IOException, ConnectionException, InterruptedException, TimeoutException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			ncf.setTimeout(5000);
			NetcodeClient client1 = ncf.createChannel("test1", ChannelConfiguration.getDefault());
			
			client1.ask(null, "test");
		} finally {
			server.close();
			Thread.sleep(1000);
		}
	}

	@Test(expected = TimeoutException.class)
	public void testTimeout() throws IOException, ConnectionException, InterruptedException, TimeoutException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			ncf.setTimeout(2000);
			NetcodeClient client1 = ncf.createChannel("test1", ChannelConfiguration.getDefault());
			NetcodeClient client2 = ncf.joinChannel("test2", client1.getChannelConfiguration().getChannelId());

			client2.setQuestionHandler((x, y) -> {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				return y;
			});
			
			Thread.sleep(500);

			client1.ask("test2", "test");

		} finally {
			server.close();
			Thread.sleep(1000);
		}
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testNoHandler() throws IOException, ConnectionException, InterruptedException, TimeoutException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			ncf.setTimeout(2000);
			NetcodeClient client1 = ncf.createChannel("test1", ChannelConfiguration.getDefault());
			ncf.joinChannel("test2", client1.getChannelConfiguration().getChannelId());

			Thread.sleep(500);
			
			client1.ask("test2", "test");

		} finally {
			server.close();
			Thread.sleep(1000);
		}
	}

	@Test(expected = NullPointerException.class)
	public void testFailingHandler() throws IOException, ConnectionException, InterruptedException, TimeoutException {
		NetcodeServer server = new NetcodeServerFactory(8888).start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			ncf.setTimeout(2000);
			NetcodeClient client1 = ncf.createChannel("test1", ChannelConfiguration.getDefault());
			NetcodeClient client2 = ncf.joinChannel("test2", client1.getChannelConfiguration().getChannelId());

			client2.setQuestionHandler((x, y) -> {
				throw new NullPointerException();
			});

			Thread.sleep(500);
			
			client1.ask("test2", "test");

		} finally {
			server.close();
			Thread.sleep(1000);
		}
	}

}
