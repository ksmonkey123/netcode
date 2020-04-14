package ch.awae.netcode.client.binding;

public interface RemoteBindings {

    <T> T bindBean(Class<T> interfaceClass);

    <T> T bindBean(String qualifier, Class<T> interfaceClass);

}
