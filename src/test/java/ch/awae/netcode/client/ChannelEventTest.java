package ch.awae.netcode.client;

import ch.awae.netcode.server.NetcodeServer;
import ch.awae.netcode.server.NetcodeServerFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class ChannelEventTest {

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
    public void testJoinIsBroadcast() throws IOException, InterruptedException {
        NetcodeClient alice = clientFactory.createChannel("alice");

        Semaphore semaphore = new Semaphore(0);

        alice.setEventHandler((userId, joined) -> {
            if (joined) semaphore.release();
        });

        NetcodeClient bob = clientFactory.joinChannel(alice.getChannelId(), "bob", null);

        assertTrue(semaphore.tryAcquire(10, TimeUnit.SECONDS));

        assertEquals(2, alice.getUsers().length);
        assertEquals(2, bob.getUsers().length);
        assertArrayEquals(alice.getUsers(), bob.getUsers());
    }

    @Test
    public void testLeaveIsBroadcast() throws IOException, InterruptedException {
        NetcodeClient alice = clientFactory.createChannel("alice");
        NetcodeClient bob = clientFactory.joinChannel(alice.getChannelInformation().getChannelId(), "bob", null);        Semaphore semaphore = new Semaphore(0);

        bob.setEventHandler((userId, joined) -> {
            if (!joined) semaphore.release();
        });

        assertEquals(2, bob.getUsers().length);

        alice.disconnect();

        assertTrue(semaphore.tryAcquire(10, TimeUnit.SECONDS));

        assertEquals(1, bob.getUsers().length);
    }

}
