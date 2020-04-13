package ch.awae.netcode.client;

import ch.awae.netcode.internal.ObjectStreams;

import java.io.IOException;
import java.net.Socket;

class ProtoClient {

    private final ObjectStreams streams;
    private final Socket socket;

    ProtoClient(Socket socket, ObjectStreams streams) throws IOException {
        this.streams = streams;
        this.socket = socket;
    }

    ObjectStreams getStreams() {
        return streams;
    }

    Socket getSocket() {
        return socket;
    }
}
