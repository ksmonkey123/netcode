package ch.awae.netcode.server;

import ch.awae.netcode.internal.NetcodePacket;
import ch.awae.netcode.internal.ObjectStreams;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;

class Client extends Thread {

    private final String userId;
    private final Socket socket;
    private final ObjectStreams streams;
    private final Channel channel;
    private volatile boolean active = true;

    Client(String userId, Socket socket, ObjectStreams streams, Channel channel) {
        this.userId = userId;
        this.socket = socket;
        this.streams = streams;
        this.channel = channel;
        setName("Server-Side Client: " + channel.getId().getChannelId() + "/" + userId);

        start();
    }

    @Override
    public void run() {
        while(!Thread.interrupted() && active) {
            try {
                NetcodePacket packet = streams.read(NetcodePacket.class);
                if (packet != null) {
                    try {
                        processPacket(packet);
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                // stream issue - kill client
                e.printStackTrace();
                break;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        channel.removeClient(this);
    }

    void send(Serializable message) {
        try {
            streams.write(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    String getUserId() {
        return this.userId;
    }

    private void processPacket(NetcodePacket message) {
        if (message.getDestinationId() != null) {
            channel.sendPrivately(message.getDestinationId(), message);
        } else {
            channel.sendPublicly(message);
        }
    }

    Socket getSocket() {
        return socket;
    }

    void closeStreams() {
        streams.close();
    }

    void terminate() {
        active = false;
    }
}
