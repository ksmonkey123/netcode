package ch.awae.netcode.exception;

public class HandshakeException extends NetcodeException {

    public HandshakeException(Throwable cause) {
        super("handshake failed: " + cause.getMessage(), cause);
    }

}
