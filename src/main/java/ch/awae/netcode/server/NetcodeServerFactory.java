package ch.awae.netcode.server;

import java.io.IOException;
import java.util.function.Predicate;

public class NetcodeServerFactory {

    private int port = -1;
    private Predicate<String> appIdValidator;

    public NetcodeServerFactory() {}

    public NetcodeServerFactory(int port) {
        setPort(port);
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        validatePort(port);
        this.port = port;
    }

    private void validatePort(int port) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("port number out of range: " + port);
        }
    }

    public Predicate<String> getAppIdValidator() {
        return appIdValidator;
    }

    public void setAppIdValidator(Predicate<String> appIdValidator) {
        this.appIdValidator = appIdValidator;
    }

    public NetcodeServer start() throws IOException {
        validateAndComplete();
        return new NetcodeServerImpl(port, appIdValidator);
    }

    private void validateAndComplete() {
        validatePort(port);
        if (appIdValidator == null) {
            appIdValidator = x -> true;
        }
    }

}
