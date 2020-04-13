package ch.awae.netcode.client;

import ch.awae.netcode.server.NetcodeServer;
import ch.awae.netcode.server.NetcodeServerFactory;
import org.junit.*;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class MessagingTest {

    private NetcodeServer server;

    private NetcodeClient alice, bob, carol;

    @Before
    public void setUp() throws Exception {
        server = new NetcodeServerFactory(8000).start();
        NetcodeClientFactory clientFactory = new NetcodeClientFactory("localhost", 8000, "testApp");

        alice = clientFactory.createChannel("alice");

        Semaphore semaphore = new Semaphore(-1);

        alice.setEventHandler((userId, joined) -> {
            if (joined) semaphore.release();
        });

        bob = clientFactory.joinChannel(alice.getChannelInformation().getChannelId(), "bob", null);
        carol = clientFactory.joinChannel(alice.getChannelInformation().getChannelId(), "carol", null);

        if (!semaphore.tryAcquire(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("clients not connected properly");
        }

        alice.setEventHandler(null);
    }

    @After
    public void tearDown() throws Exception {
        server.terminateAndJoin();
    }

    @Test
    public void testPublicMessagesReachEveryone() throws InterruptedException {
        Semaphore semaphore = new Semaphore(0);

        MessageHandler countingHandler = (sender, timestamp, message) -> semaphore.release();

        alice.setMessageHandler(countingHandler);
        bob.setMessageHandler(countingHandler);
        carol.setMessageHandler(countingHandler);

        alice.sendToChannel("hello world");

        assertTrue(semaphore.tryAcquire(3, 10, TimeUnit.SECONDS));
    }

    @Test
    public void testPrivateMessageOnlyReachesTarget() throws InterruptedException {
        Semaphore badSemaphore = new Semaphore(0);
        Semaphore goodSemaphore = new Semaphore(0);

        MessageHandler badHandler = (sender, timestamp, message) -> badSemaphore.release();
        MessageHandler goodHandler = (sender, timestamp, message) -> goodSemaphore.release();

        alice.setMessageHandler(badHandler);
        bob.setMessageHandler(goodHandler);
        carol.setMessageHandler(badHandler);

        alice.sendPrivately("bob", "hello world");

        assertTrue(goodSemaphore.tryAcquire(1, TimeUnit.SECONDS));
        assertFalse(badSemaphore.tryAcquire(1, TimeUnit.SECONDS));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCannotPrivatelyMessageMissingUser() {
        Semaphore badSemaphore = new Semaphore(0);
        Semaphore goodSemaphore = new Semaphore(0);

        MessageHandler badHandler = (sender, timestamp, message) -> badSemaphore.release();
        MessageHandler goodHandler = (sender, timestamp, message) -> goodSemaphore.release();

        alice.setMessageHandler(badHandler);
        bob.setMessageHandler(goodHandler);
        carol.setMessageHandler(badHandler);

        alice.sendPrivately("dave", "hello world");
    }

}
