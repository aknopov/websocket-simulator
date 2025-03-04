package com.aknopov.wssimulator.injection;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.aknopov.wssimulator.EventListener;
import com.aknopov.wssimulator.SessionConfig;

/**
 * This class provides access to a centralized service locator
 */
public class ServiceLocator {

    private static final Map<Class<?>, Object> BINDINGS = new ConcurrentHashMap<>();

    private ServiceLocator()
    {
    }

    /**
     * Finds instance of registered class. Creates a new instance if not found using default c-tor.
     *
     * @param klaz the class
     * @return class instance
     * @param <T> class type
     */
    public static <T> T findOrCreate(Class<T> klaz) {
        return getOrCreateInstance(klaz);
    }

    @SuppressWarnings("unchecked")
    private static synchronized <T> T getOrCreateInstance(Class<T> klaz) {
        Object value = BINDINGS.getOrDefault(klaz, klaz);

        // if value is an Object - return it
        if (!(value instanceof Class)) {
            return klaz.cast(value);
        }

        // If value is class - create instance
        try {
            return ((Class<T>)value).getDeclaredConstructor().newInstance();
        }
        catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalStateException("Implementation error - can't create instance of class " + klaz.getName(), e);
        }
    }

    /**
     * Initiates binding to the new objects
     * @param config session configuration
     * @param listener event listener
     */
    public static void init(SessionConfig config, EventListener listener) {
        BINDINGS.clear();
        bind(listener, EventListener.class);
        bind(config, SessionConfig.class);
    }

    /**
     * Binds a service class to its interface
     *
     * @param subClass service class
     * @param contract interface class
     * @param <T> interface type
     */
    public static <T> void bind(Class<T> subClass, Class<? super T> contract) {
        BINDINGS.put(contract, subClass);
    }

    /**
     * Binds a service instance to its interface
     *
     * @param instance service instance
     * @param contract interface class
     * @param <T> interface type
     */
    public static <T> void bind(T instance, Class<? super T> contract) {
        BINDINGS.put(contract, instance);
    }
}
