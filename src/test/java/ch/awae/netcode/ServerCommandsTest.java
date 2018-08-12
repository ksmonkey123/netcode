package ch.awae.netcode;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;

public class ServerCommandsTest {

	@Test
	public void canGetChannelInfo() throws IOException, ConnectionException, InterruptedException {
		NetcodeServerFactory nsf = new NetcodeServerFactory(8888);
		NetcodeServer server = nsf.start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			NetcodeClient client = ncf.createChannel("test", ChannelConfiguration.getDefault());
			client.getChannelInformation();
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test(expected = UnsupportedFeatureException.class)
	public void cannotGetChannelInfoIfSCdisabled() throws IOException, ConnectionException, InterruptedException {
		NetcodeServerFactory nsf = new NetcodeServerFactory(8888);
		nsf.setEnableServerCommands(false);
		NetcodeServer server = nsf.start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			NetcodeClient client = ncf.createChannel("test", ChannelConfiguration.getDefault());
			client.getChannelInformation();
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test(expected = UnsupportedFeatureException.class)
	public void cannotGetChannelListWithoutPublicSupport()
			throws IOException, ConnectionException, InterruptedException {
		NetcodeServerFactory nsf = new NetcodeServerFactory(8888);
		nsf.setEnablePublicChannels(false);
		NetcodeServer server = nsf.start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			NetcodeClient client = ncf.createChannel("test", ChannelConfiguration.getDefault());
			client.getPublicChannels();
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
			NetcodeClient client = ncf.createChannel("test", ChannelConfiguration.getDefault());
			Assert.assertEquals(0, client.getPublicChannels().length);
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test
	public void channelListShowsPublicChannels() throws IOException, ConnectionException, InterruptedException {
		NetcodeServerFactory nsf = new NetcodeServerFactory(8888);
		NetcodeServer server = nsf.start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			NetcodeClient client = ncf.createChannel("test",
					ChannelConfiguration.builder().publicChannel(true).maxClients(2).build());
			ChannelInformation[] list = client.getPublicChannels();
			Assert.assertEquals(1, list.length);
			Assert.assertEquals(client.getChannelConfiguration().getChannelId(), list[0].getId());
			Assert.assertEquals(1, list[0].getMemberCount());
			Assert.assertEquals(2, list[0].getMemberLimit());
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test
	public void channelInformationMatchesChannelList() throws IOException, ConnectionException, InterruptedException {
		NetcodeServerFactory nsf = new NetcodeServerFactory(8888);
		NetcodeServer server = nsf.start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			NetcodeClient client = ncf.createChannel("test",
					ChannelConfiguration.builder().publicChannel(true).maxClients(2).build());
			ChannelInformation[] list = client.getPublicChannels();
			ChannelInformation myChannel = client.getChannelInformation();
			Assert.assertEquals(1, list.length);
			Assert.assertEquals(myChannel, list[0]);
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test
	public void channelInformationIncludesCreator() throws IOException, ConnectionException, InterruptedException {
		NetcodeServerFactory nsf = new NetcodeServerFactory(8888);
		NetcodeServer server = nsf.start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			NetcodeClient client = ncf.createChannel("test",
					ChannelConfiguration.builder().publicChannel(true).maxClients(2).build());
			ChannelInformation myChannel = client.getChannelInformation();
			Assert.assertEquals("test", myChannel.getCreatedBy());
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

	@Test(expected = InvalidRequestException.class)
	public void nullCommandShouldYieldIRE() throws Throwable {
		NetcodeServerFactory nsf = new NetcodeServerFactory(8888);
		NetcodeServer server = nsf.start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			NetcodeClient client = ncf.createChannel("test", ChannelConfiguration.getDefault());
			Method method = client.getClass().getDeclaredMethod("runServerCommand", String.class, Serializable.class);
			method.setAccessible(true);
			try {
				method.invoke(client, null, null);
			} catch (InvocationTargetException ite) {
				throw ite.getTargetException();
			}
		} finally {
			server.close();
			Thread.sleep(500);
		}
	}

}
