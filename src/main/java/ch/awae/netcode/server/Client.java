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

    Client(String userId, Socket socket, ObjectStreams streams, Channel channel) {
        this.userId = userId;
        this.socket = socket;
        this.streams = streams;
        this.channel = channel;

        start();
    }

    @Override
    public void run() {
        while(!Thread.interrupted()) {
            try {
                NetcodePacket packet = streams.read(NetcodePacket.class);
                if (packet != null) {
                    try {
                        processPacket(packet);
                    } catch (RuntimeException e) {
                        // TODO: logging
                    }
                }
            } catch (IOException e) {
                // stream issue - kill client
                channel.removeClient(this);
            } catch (ClassNotFoundException e) {
                // TODO: logging
            }

        }
    }

    void send(Serializable message) {
        try {
            streams.write(message);
        } catch (IOException e) {
            // TODO: logging
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

    public Socket getSocket() {
        return socket;
    }
}
