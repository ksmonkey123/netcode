package ch.awae.netcode.client;

import ch.awae.netcode.server.NetcodeServer;
import ch.awae.netcode.server.NetcodeServerFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class NetcodeClientTest {

    private NetcodeServer start;
    private NetcodeClientFactory clientFactory;

    @Before
    public void setUp() throws Exception {
        start = new NetcodeServerFactory(8000).start();
        clientFactory = new NetcodeClientFactory("localhost", 8000, "testApp");
    }

    @After
    public void tearDown() throws Exception {
        start.terminateAndJoin();
    }

    @Test
    public void testCanConnectToServer() throws IOException {
        NetcodeClient client = clientFactory.createChannel("user1");

        assertEquals(1, client.getUsers().length);
        assertEquals("user1", client.getUsers()[0]);
    }

    @Test
    public void testCanJoinChannel() throws IOException, InterruptedException {
        NetcodeClient client1 = clientFactory.createChannel("user1");

        Semaphore semaphore = new Semaphore(0);

        client1.setEventHandler((userId, joined) -> {
            if (joined) semaphore.release();
        });

        NetcodeClient client2 = clientFactory.joinChannel(client1.getChannelInformation().getChannelId(), "user2", null);

        assertTrue(semaphore.tryAcquire(10, TimeUnit.SECONDS));

        assertEquals(2, client1.getUsers().length);
        assertEquals(2, client2.getUsers().length);
        assertArrayEquals(client1.getUsers(), client2.getUsers());
    }

    @Test
    public void testUserCanLeave() throws IOException, InterruptedException {
        NetcodeClient client1 = clientFactory.createChannel("user1");
        NetcodeClient client2 = clientFactory.joinChannel(client1.getChannelInformation().getChannelId(), "user2", null);

        Semaphore semaphore = new Semaphore(0);

        client2.setEventHandler((userId, joined) -> {
            if (!joined) semaphore.release();
        });

        assertEquals(2, client2.getUsers().length);

        client1.disconnect();

        assertTrue(semaphore.tryAcquire(10, TimeUnit.SECONDS));

        assertEquals(1, client2.getUsers().length);
    }

}
