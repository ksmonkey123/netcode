package ch.awae.netcode;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

public class ChannelListTest {

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

	@Test
	public void canSeeChannelListContents() throws IOException, ConnectionException, InterruptedException {
		NetcodeServerFactory nsf = new NetcodeServerFactory(8888);
		NetcodeServer server = nsf.start();
		try {
			NetcodeClientFactory ncf = new NetcodeClientFactory("localhost", 8888, "myApp");
			ncf.createChannel("test", ChannelConfiguration.builder().channelName("asdf").publicChannel(true).build());
			ChannelInformation[] list = ncf.listPublicChannels();
			Assert.assertEquals(1, list.length);
			ChannelInformation info = list[0];
			Assert.assertEquals("asdf", info.getName());
			Assert.assertEquals("test", info.getCreatedBy());
			Assert.assertEquals(info.getId(), info.getConfiguration().getChannelId());
			Assert.assertEquals("asdf", info.getConfiguration().getChannelName());
			Assert.assertEquals(1, info.getMemberCount());
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
			ChannelInformation[] list = ncf.listPublicChannels();
			Assert.assertEquals(1, list.length);
			Assert.assertEquals(list[0].getConfiguration(),
					ncf.joinChannel("test1", list[0].getId()).getChannelConfiguration());
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

}
