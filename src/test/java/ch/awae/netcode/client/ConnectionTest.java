package ch.awae.netcode.client;

import ch.awae.netcode.server.NetcodeServer;
import ch.awae.netcode.server.NetcodeServerFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConnectionTest {

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
    public void testCreateChannel() throws IOException {
        NetcodeClient alice = clientFactory.createChannel("alice");

        assertEquals(1, alice.getUsers().length);
        assertEquals("alice", alice.getUsers()[0]);
    }

    @Test
    public void testCanJoinChannel() throws IOException {
        NetcodeClient alice = clientFactory.createChannel("alice");
        NetcodeClient bob = clientFactory.joinChannel(alice.getChannelId(), "bob", null);

        List<String> users = Arrays.asList(bob.getUsers());
        assertEquals(2, users.size());
        assertTrue(users.contains("alice"));
        assertTrue(users.contains("bob"));
    }

    @Test
    public void testUserCanLeaveChannel() throws IOException, InterruptedException {
        NetcodeClient alice = clientFactory.createChannel("alice");
        NetcodeClient bob = clientFactory.joinChannel(alice.getChannelId(), "bob", null);
        bob.disconnect();
        Thread.sleep(1000);
        NetcodeClient carol = clientFactory.joinChannel(alice.getChannelId(), "carol", null);

        List<String> users = Arrays.asList(carol.getUsers());
        assertEquals(2, users.size());
        assertTrue(users.contains("alice"));
        assertTrue(users.contains("carol"));
    }

}
