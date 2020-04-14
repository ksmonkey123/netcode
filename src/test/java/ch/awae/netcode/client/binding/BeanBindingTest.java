package ch.awae.netcode.client.binding;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class BeanBindingTest {

    private LocalBindings bindings;

    @Before
    public void setUp() {
        bindings = new LocalBindingsImpl();
    }

    @Test(expected = IllegalArgumentException.class)
    public void bBeanWithoutInterfaceCannotBeBound() {
        bindings.register(new Object());
    }

    @Test(expected = NullPointerException.class)
    public void betrievalNonNull() {
        bindings.getBean(null);
    }

    @Test
    public void nonExistingBeanRetrievalSafe() {
        assertNull(bindings.getBean(Interface_A.class));
    }

    @Test
    public void beanWithOneInterfaceCanBeBound() {
        Implementation_A bean = new Implementation_A();
        bindings.register(bean);

        assertEquals(bean, bindings.getBean(Interface_A.class));
    }

    @Test
    public void beanBoundToAllInterfaces() {
        CommonImplementation bean = new CommonImplementation();
        bindings.register(bean);

        assertEquals(bean, bindings.getBean(Interface_A.class));
        assertEquals(bean, bindings.getBean(Interface_B.class));
        assertEquals(bean, bindings.getBean(Interface_C.class));
    }

    @Test
    public void beanBoundOnlyToExplicits() {
        CommonImplementation bean = new CommonImplementation();
        bindings.register(bean, Interface_A.class, Interface_B.class);

        assertEquals(bean, bindings.getBean(Interface_A.class));
        assertEquals(bean, bindings.getBean(Interface_B.class));
        assertNull(bindings.getBean(Interface_C.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void explicitClassMustBeInterface() {
        bindings.register(new Implementation_A(), Object.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void additionalClassesMustBeImplemented() {
        bindings.register(new Implementation_A(), Interface_A.class, Interface_B.class);
    }

    @Test(expected = IllegalStateException.class)
    public void cannotBindMultipleOnSameQualifier() {
        Implementation_A a = new Implementation_A();
        Implementation_A b = new Implementation_A();

        bindings.register(a);
        bindings.register(b);
    }

    @Test
    public void canBindOnDifferentQualifiers() {
        Implementation_A a = new Implementation_A();
        Implementation_A b = new Implementation_A();

        bindings.register("a", a);
        bindings.register("b", b);

        assertEquals(a, bindings.getBean("a", Interface_A.class));
        assertEquals(b, bindings.getBean("b", Interface_A.class));
    }

    @Test
    public void sameQualifierDifferentTypes() {
        Implementation_A a = new Implementation_A();
        Implementation_B b = new Implementation_B();

        bindings.register("a", a);
        bindings.register("a", b);

        assertEquals(a, bindings.getBean("a", Interface_A.class));
        assertEquals(b, bindings.getBean("a", Interface_B.class));
    }

    @Test(expected = IllegalStateException.class)
    public void cannotBindIfOnlyOneInterfaceIsMapped() {
        Implementation_A blocker = new Implementation_A();
        CommonImplementation bean = new CommonImplementation();

        bindings.register(blocker);
        bindings.register(bean);
    }

    @Test(expected = IllegalStateException.class)
    public void cannotUnbindNotBound() {
        Implementation_A a = new Implementation_A();
        Implementation_A b = new Implementation_A();

        bindings.register(a);
        bindings.unregister(b);
    }

    @Test(expected = IllegalStateException.class)
    public void cannotUnbindNotBoundByInterface() {
        bindings.register(new Implementation_B());
        bindings.unregister(Interface_A.class);
    }

    @Test
    public void beanWithMultipleQualifiersOnlyExistsOnce() {
        Implementation_A bean = new Implementation_A();

        bindings.register("a", bean);
        bindings.register("b", bean);

        assertEquals(1, bindings.getAllBeans().size());
        assertEquals(1, bindings.getBeans(Interface_A.class).size());
    }

    @Test
    public void canUnbind() {
        Implementation_A bean = new Implementation_A();

        bindings.register(bean);
        bindings.unregister(bean);

        assertNull(bindings.getBean(Interface_A.class));
        assertTrue(bindings.getAllBeans().isEmpty());
    }

    @Test
    public void unbindingMultiboundBeanUnbindsItForAllInterfaces() {
        CommonImplementation bean = new CommonImplementation();

        bindings.register(bean);
        bindings.unregister(bean);

        assertNull(bindings.getBean(Interface_A.class));
        assertNull(bindings.getBean(Interface_B.class));
        assertNull(bindings.getBean(Interface_C.class));
        assertTrue(bindings.getAllBeans().isEmpty());
    }

    @Test
    public void unbindingBeanByValueRemovesItFromAllQualifiers() {
        Implementation_A bean = new Implementation_A();

        bindings.register("a", bean);
        bindings.register("b", bean);
        bindings.unregister(bean);

        assertTrue(bindings.getAllBeans().isEmpty());
    }

    @Test
    public void unbindingBeanFromOneQualifierByInterfaceRemovesItFromAll() {
        Implementation_A bean = new Implementation_A();

        bindings.register("a", bean);
        bindings.register("b", bean);
        bindings.unregister("a", Interface_A.class);

        assertEquals(bean, bindings.getBean("b", Interface_A.class));
        assertEquals(1, bindings.getAllBeans().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void retrievalMustUseInterfaceAsClass() {
        bindings.getBean(Object.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void unbindingByTypeMustUseInterface() {
        bindings.unregister(Object.class);
    }

}
