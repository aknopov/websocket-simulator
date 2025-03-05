package com.aknopov.wssimulator.injection;

import org.junit.jupiter.api.Test;

import com.aknopov.wssimulator.tyrus.BaseTest;
import com.aknopov.wssimulator.EventListener;
import com.aknopov.wssimulator.SessionConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceLocatorTest extends BaseTest {

    interface TestInterface {
    }

    private static class TestClass implements TestInterface {
        private final String content;

        TestClass() {
            this.content = "";
        }

        TestClass(String content) {
            this.content = content;
        }

        public String getContent() {
            return content;
        }
    }

    private record TestRecord(String content) implements TestInterface {
    }

    @Test
    void testSingleEventListener() {
        EventListener listener1 = ServiceLocator.findOrCreate(EventListener.class);
        EventListener listener2 = ServiceLocator.findOrCreate(EventListener.class);
        assertSame(listener1, listener2);
    }

    @Test
    void testSessionConfig() {
        SessionConfig config = ServiceLocator.findOrCreate(SessionConfig.class);

        assertEquals(A_PATH, config.path());
        assertEquals(IDLE_SECS, config.idleTimeout().toSeconds());
        assertEquals(BUFFER_SIZE, config.bufferSize());
    }

    @Test
    void testClassBinding() {
        ServiceLocator.bind(TestClass.class, TestInterface.class);

        TestInterface obj1 = ServiceLocator.findOrCreate(TestInterface.class);
        assertInstanceOf(TestClass.class, obj1);
        assertEquals("", ((TestClass)obj1).getContent());

        TestInterface obj2 = ServiceLocator.findOrCreate(TestInterface.class);
        assertInstanceOf(TestClass.class, obj2);
        assertEquals("", ((TestClass)obj2).getContent());
        assertNotSame(obj1, obj2);
    }

    @Test
    void testObjectBinding() {
        TestInterface obj0 = new TestClass("content");
        ServiceLocator.bind(obj0, TestInterface.class);

        // Singleton behavior
        TestInterface obj1 = ServiceLocator.findOrCreate(TestInterface.class);
        assertSame(obj0, obj1);
        TestInterface obj2 = ServiceLocator.findOrCreate(TestInterface.class);
        assertSame(obj0, obj2);
    }

    @Test
    void testMandatoryDefaultConstructor() {
        ServiceLocator.bind(TestRecord.class, TestInterface.class);

        assertThrows(IllegalStateException.class, () -> ServiceLocator.findOrCreate(TestInterface.class));
    }
}