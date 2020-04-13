package ch.awae.netcode.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;

public class ObjectStreams {

    private final ObjectInputStream inputStream;
    private final ObjectOutputStream outputStream;

    public ObjectStreams(Socket socket) throws IOException {
        // order is important: if both sides first open the input stream, they will deadlock
        outputStream = new ObjectOutputStream(socket.getOutputStream());
        inputStream = new ObjectInputStream(socket.getInputStream());
    }

    public void write(Serializable object) throws IOException {
        outputStream.writeObject(object);
    }

    public Serializable read() throws IOException, ClassNotFoundException {
        return (Serializable) inputStream.readObject();
    }

    public <T extends Serializable> T read(Class<T> clazz) throws IOException, ClassNotFoundException {
        return clazz.cast(read());
    }

}
