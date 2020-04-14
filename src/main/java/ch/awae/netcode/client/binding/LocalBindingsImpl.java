package ch.awae.netcode.client.binding;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class LocalBindingsImpl implements LocalBindings {

    private final Map<Class<?>, Map<String, Object>> map;
    private final Lock readLock, writeLock;

    LocalBindingsImpl() {
        map = new HashMap<>();
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
        readLock = lock.readLock();
        writeLock = lock.writeLock();
    }

    @Override
    public <T> void register(String qualifier, T bean) {
        Objects.requireNonNull(bean);

        Class<?>[] interfaces = bean.getClass().getInterfaces();

        if (interfaces.length == 0) {
            throw new IllegalArgumentException("bean must implement at least one interface: " + bean);
        }

        writeLock.lock();
        try {
            verifyNoCollisions(qualifier, interfaces);
            insertBean(qualifier, bean, interfaces);
        } finally {
            writeLock.unlock();
        }

    }

    private <T> void insertBean(String qualifier, T bean, Class<?>[] interfaces) {
        for (Class<?> anInterface : interfaces) {
            Map<String, Object> interfaceMap = map.computeIfAbsent(anInterface, k -> new HashMap<>());
            interfaceMap.put(qualifier, bean);
        }
    }

    private void verifyNoCollisions(String qualifier, Class<?>[] interfaces) {
        for (Class<?> anInterface : interfaces) {
            if (getBean(qualifier, anInterface) != null) {
                throw new IllegalStateException("cannot bind bean. there already exists an active binding for the interface " + anInterface);
            }
        }
    }

    @Override
    public <T> void register(String qualifier, T bean, Class<? super T> beanInterface, Class<?>... additionalInterfaces) {
        Objects.requireNonNull(bean);

        Class<?>[] beanInterfaces = bean.getClass().getInterfaces();

        if (beanInterfaces.length == 0) {
            throw new IllegalArgumentException("bean needs at least one interface");
        }

        List<Class<?>> classes = Arrays.asList(beanInterfaces);

        validateIsImplementedInterface(beanInterface, classes);
        for (Class<?> additionalInterface : additionalInterfaces) {
            validateIsImplementedInterface(additionalInterface, classes);
        }

        Class<?>[] explicitInterfaces = new Class[additionalInterfaces.length + 1];

        explicitInterfaces[0] = beanInterface;
        System.arraycopy(additionalInterfaces, 0, explicitInterfaces, 1, additionalInterfaces.length );

        writeLock.lock();
        try {
            verifyNoCollisions(qualifier, explicitInterfaces);
            insertBean(qualifier, bean, explicitInterfaces);
        } finally {
            writeLock.unlock();
        }
    }

    private void validateIsImplementedInterface(Class<?> interfaceClass, List<Class<?>> classes) {
        validateIsInterface(interfaceClass);
        if (!classes.contains(interfaceClass)) {
            throw new IllegalArgumentException(interfaceClass + " is not implemented by the bean");
        }
    }

    private void validateIsInterface(Class<?> interfaceClass) {
        if (!interfaceClass.isInterface()) {
            throw new IllegalArgumentException(interfaceClass + " is no interface");
        }
    }

    @Override
    public void unregister(String qualifier, Class<?> beanInterface) {
        Objects.requireNonNull(beanInterface);
        validateIsInterface(beanInterface);
        long deletions = 0;

        writeLock.lock();
        try {
                Map<String, Object> interfaceMap = map.get(beanInterface);
                if (interfaceMap != null) {
                    if (interfaceMap.remove(qualifier) != null) {
                        deletions++;
                    }
                }
        } finally {
            writeLock.unlock();
        }

        if (deletions == 0) {
            throw new IllegalStateException("cannot unbind interface that is not bound");
        }
    }

    @Override
    public <T> void unregister(T bean) {
        Objects.requireNonNull(bean);
        long deletions = 0;

        writeLock.lock();
        try {
            Class<?>[] interfaces = bean.getClass().getInterfaces();
            for (Class<?> anInterface : interfaces) {
                List<String> toDelete = new ArrayList<>();
                Map<String, Object> interfaceMap = map.get(anInterface);
                if (interfaceMap == null) {
                    continue;
                }
                for (Map.Entry<String, Object> entry : interfaceMap.entrySet()) {
                    if (entry.getValue() == bean) {
                        toDelete.add(entry.getKey());
                    }
                }
                deletions += toDelete.size();
                toDelete.forEach(interfaceMap::remove);
                if (interfaceMap.isEmpty()) {
                    map.remove(anInterface);
                }
            }
        } finally {
            writeLock.unlock();
        }

        if (deletions == 0) {
            throw new IllegalStateException("cannot unbind bean that is not bound");
        }
    }

    @Override
    public <T> T getBean(String qualifier, Class<T> beanInterface) {
        Objects.requireNonNull(beanInterface);
        validateIsInterface(beanInterface);
        readLock.lock();
        try {
            Map<String, Object> interfaceMap = map.get(beanInterface);
            if (interfaceMap != null) {
                return beanInterface.cast(interfaceMap.get(qualifier));
            }
            return null;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public <T> List<T> getBeans(Class<T> beanInterface) {
        Objects.requireNonNull(beanInterface);
        validateIsInterface(beanInterface);
        readLock.lock();
        try {
            Map<String, Object> interfaceMap = map.get(beanInterface);
            if (interfaceMap == null) {
                return Collections.emptyList();
            }
            Set<T> set = new HashSet<>();

            for (Object x : interfaceMap.values()) {
                set.add(beanInterface.cast(x));
            }

            return new ArrayList<>(set);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public List<Object> getAllBeans() {
        readLock.lock();
        try {
            Set<Object> set = new HashSet<>();
            map.forEach((type, beans) -> set.addAll(beans.values()));
            return new ArrayList<>(set);
        } finally {
            readLock.unlock();
        }
    }
}
