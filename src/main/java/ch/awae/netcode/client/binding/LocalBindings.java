package ch.awae.netcode.client.binding;

import java.util.List;

public interface LocalBindings {

    <T> void register(String qualifier, T bean);
    <T> void register(String qualifier, T bean, Class<? super T> beanInterface, Class<?>... additionalInterfaces);

    void unregister(String qualifier, Class<?> beanInterface);
    <T> void unregister(T bean);

    <T> T getBean(String qualifier, Class<T> beanInterface);
    <T> List<T> getBeans(Class<T> beanInterface);
    List<Object> getAllBeans();

    default <T> void register(T bean) {
        register(null, bean);
    }

    default <T> void register(T bean, Class<? super T> beanInterface, Class<?>... additionalInterfaces) {
        register(null, bean, beanInterface, additionalInterfaces);
    }

    default <T> T getBean(Class<T> beanInterface) {
        return getBean(null, beanInterface);
    }

    default <T> void unregister(Class<?> beanInterface) {
        unregister(null, beanInterface);
    }

    static LocalBindings createInstance() {
        return new LocalBindingsImpl();
    }

}
