package ch.awae.netcode.client;

import ch.awae.netcode.server.NetcodeServer;
import ch.awae.netcode.server.NetcodeServerFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.*;

public class QuestionTest {

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

    @Test(expected = ExecutionException.class)
    public void testAliceCannotAskIfBobIsNotConfigured() throws InterruptedException, TimeoutException, ExecutionException {
        ClientReference bob = alice.getClientReference("bob");
        Future<Serializable> response = bob.askQuestion("hello");
        response.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void testAskQuestion() throws InterruptedException, TimeoutException, ExecutionException {
        ClientReference bob = alice.getClientReference("bob");

        this.bob.setQuestionHandler((sender, time, question) -> question.toString().toUpperCase());

        Future<Serializable> resp = bob.askQuestion("hello");
        Serializable response = resp.get(10, TimeUnit.SECONDS);

        assertEquals("HELLO", response);
    }

    @Test(expected = IllegalStateException.class)
    public void testAskQuestionAfterDisconnect() throws InterruptedException {
        ClientReference bob = alice.getClientReference("bob");

        this.bob.setQuestionHandler((sender, time, question) -> question.toString().toUpperCase());
        Semaphore semaphore = new Semaphore(0);
        alice.setEventHandler((userId, joined) -> {
            if (!joined) {
                semaphore.release();
            }
        });
        this.bob.disconnect();
        assertTrue(semaphore.tryAcquire(1, TimeUnit.SECONDS));

        bob.askQuestion("hello");
    }

    @Test(expected = CancellationException.class)
    public void testAskQuestionDuringDisconnect() throws InterruptedException, TimeoutException, ExecutionException {
        ClientReference bob = alice.getClientReference("bob");

        Semaphore semaphore = new Semaphore(0);
        this.bob.setQuestionHandler((sender, time, question) -> {
            try {
                semaphore.release();
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return question;
        });
        alice.setEventHandler((userId, joined) -> {
            if (!joined) {
                semaphore.release();
            }
        });
        Future<Serializable> future = bob.askQuestion("hello");
        assertTrue(semaphore.tryAcquire(1, TimeUnit.SECONDS));
        semaphore.release();
        this.bob.disconnect();
        assertTrue(semaphore.tryAcquire(1, TimeUnit.SECONDS));
        future.get(1, TimeUnit.SECONDS);
    }

    @Test
    public void testAskTypedQuestion() throws InterruptedException, TimeoutException, ExecutionException {
        ClientReference bob = alice.getClientReference("bob");

        this.bob.setQuestionHandler((sender, time, question) -> question.toString().toUpperCase());

        Future<String> resp = bob.askQuestion("hello", String.class);
        String response = resp.get(10, TimeUnit.SECONDS);

        assertEquals("HELLO", response);
    }

    @Test(expected = ExecutionException.class)
    public void testAskTypedQuestionBadType() throws InterruptedException, TimeoutException, ExecutionException {
        ClientReference bob = alice.getClientReference("bob");

        this.bob.setQuestionHandler((sender, time, question) -> question.toString().toUpperCase());

        Future<String[]> resp = bob.askQuestion("hello", String[].class);
        resp.get(10, TimeUnit.SECONDS);
    }

    @Test(expected = IllegalStateException.class)
    public void testAskQuestionAfterLocalDisconnect() {
        ClientReference bob_ref = alice.getClientReference("bob");

        alice.disconnect();
        bob_ref.askQuestion("hi");
    }

}
