package ch.awae.netcode.client;

import ch.awae.netcode.server.NetcodeServer;
import ch.awae.netcode.server.NetcodeServerFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.util.concurrent.*;

import static org.junit.Assert.*;

public class ClientReferenceTest {

    private NetcodeServer server;

    private NetcodeClient alice, bob;

    @Before
    public void setUp() throws Exception {
        server = new NetcodeServerFactory(8000).start();
        NetcodeClientFactory clientFactory = new NetcodeClientFactory("localhost", 8000, "testApp");

        alice = clientFactory.createChannel("alice");

        Semaphore semaphore = new Semaphore(0);

        alice.setEventHandler((userId, joined) -> {
            if (joined) semaphore.release();
        });

        bob = clientFactory.joinChannel(alice.getChannelInformation().getChannelId(), "bob", null);

        if (!semaphore.tryAcquire(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("clients not connected properly");
        }

        alice.setEventHandler(null);
    }

    @After
    public void tearDown() throws Exception {
        server.terminateAndJoin();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReferenceMustBeKnownUser() {
        alice.getClientReference("carol");
    }

    @Test
    public void testCanSendMessageThroughReference() throws InterruptedException {
        ClientReference bob_ref = alice.getClientReference("bob");
        Semaphore semaphore = new Semaphore(0);
        bob.setMessageHandler(((sender, timestamp, message) -> semaphore.release()));
        bob_ref.sendPrivateMessage("hello there");
        assertTrue(bob_ref.isActive());
        assertTrue(semaphore.tryAcquire(1, TimeUnit.SECONDS));
    }

    @Test(expected = IllegalStateException.class)
    public void testReferenceCannotBeUsedAfterRemoteDisconnect() throws InterruptedException {
        ClientReference bob_ref = alice.getClientReference("bob");
        Semaphore semaphore = new Semaphore(0);
        alice.setEventHandler((userId, joined) -> {
            if (!joined) {
                semaphore.release();
            }
        });
        bob.disconnect();
        assertTrue(semaphore.tryAcquire(1, TimeUnit.SECONDS));
        assertFalse(bob_ref.isActive());
        bob_ref.sendPrivateMessage("hello there");
    }

}
