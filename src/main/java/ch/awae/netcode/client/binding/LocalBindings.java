package ch.awae.netcode.client.binding;

public interface LocalBindings {

    <T> void register(T bean);

    <T> void register(String qualifier, T bean);

    <T> void register(T bean, Class<? super T> beanInterface);

    <T> void register(String qualifier, T bean, Class<? super T> beanInterface);

}
