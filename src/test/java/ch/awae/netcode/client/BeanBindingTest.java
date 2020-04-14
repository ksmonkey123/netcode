package ch.awae.netcode.client;

import ch.awae.netcode.server.NetcodeServer;
import ch.awae.netcode.server.NetcodeServerFactory;
import org.junit.After;
import org.junit.Before;

public class BeanBindingTest {

    private NetcodeServer server;

    private NetcodeClient alice, bob;

    @Before
    public void setUp() throws Exception {
        server = new NetcodeServerFactory(8000).start();
        NetcodeClientFactory clientFactory = new NetcodeClientFactory("localhost", 8000, "testApp");

        bob = clientFactory.createChannel("bob");
        alice = clientFactory.joinChannel(bob.getChannelInformation().getChannelId(), "alice", null);
    }

    @After
    public void tearDown() throws Exception {
        server.terminateAndJoin();
    }

}
