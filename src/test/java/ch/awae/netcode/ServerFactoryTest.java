package ch.awae.netcode;

import java.io.IOException;
import java.net.ServerSocket;

import org.junit.Assert;
import org.junit.Test;

public class ServerFactoryTest {

	@Test(expected = IllegalArgumentException.class)
	public void zeroClients() {
		NetcodeServerFactory nsf = new NetcodeServerFactory(8888);
		nsf.setMaxClients(0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void negativeClients() {
		NetcodeServerFactory nsf = new NetcodeServerFactory(8888);
		nsf.setMaxClients(-10);
	}

	@Test(expected = IllegalArgumentException.class)
	public void negativePort() {
		new NetcodeServerFactory(-1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void tooLargePort() {
		new NetcodeServerFactory(65536);
	}

	@Test(expected = NullPointerException.class)
	public void illegalAppIdValidator() {
		NetcodeServerFactory nsf = new NetcodeServerFactory(8888);
		nsf.setAppIdValidator(null);
	}

	@Test(expected = NullPointerException.class)
	public void illegalChannelProvider() {
		NetcodeServerFactory nsf = new NetcodeServerFactory(8888);
		nsf.setChannelIdProvider(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void badModeConfiguration() {
		new NetcodeServerFactory(8888).setMode(SocketMode.PLAIN, SecurityMode.CERTIFICATE);
	}

	@Test
	public void startAndShutdownTLS() throws IOException, InterruptedException {
		NetcodeServerFactory nsf = new NetcodeServerFactory(8888);
		nsf.setMode(SocketMode.TLS, SecurityMode.ANY);
		NetcodeServer server = nsf.start();
		Thread.sleep(300);
		server.close();
		Thread.sleep(300);
	}

	@Test
	public void startAndShutdownSecure() throws IOException, InterruptedException {
		NetcodeServerFactory nsf = new NetcodeServerFactory(8888);
		nsf.setMode(SocketMode.SECURE, SecurityMode.ANY);
		NetcodeServer server = nsf.start();
		Thread.sleep(300);
		server.close();
		Thread.sleep(300);
	}

	@Test
	public void startAndShutdownSSL() throws IOException, InterruptedException {
		NetcodeServerFactory nsf = new NetcodeServerFactory(8888);
		nsf.setMode(SocketMode.SSL, SecurityMode.ANY);
		NetcodeServer server = nsf.start();
		Thread.sleep(300);
		server.close();
		Thread.sleep(300);
	}

	@Test
	public void startAndShutdownPlain() throws IOException, InterruptedException {
		NetcodeServerFactory nsf = new NetcodeServerFactory(8888);
		nsf.setMode(SocketMode.PLAIN, SecurityMode.ANY);
		NetcodeServer server = nsf.start();
		Thread.sleep(300);
		server.close();
		Thread.sleep(300);
	}

	@Test(expected = NullPointerException.class)
	public void cannotAddNullHandler() {
		new NetcodeServerFactory(8888).runAfterBind(null);
	}

	public void bindersAreRun() throws IOException, ConnectionException, InterruptedException {
		NetcodeServer server = null;
		try {
			NetcodeServerFactory nsf = new NetcodeServerFactory(8888);

			InvocationTrackingConsumer<ServerSocket> f = new InvocationTrackingConsumer<>();
			nsf.runAfterBind(f);
			server = nsf.start();
			Thread.sleep(500);
			Assert.assertTrue(f.run);
		} finally {
			if (server != null)
				server.close();
			Thread.sleep(500);
		}
	}

}
