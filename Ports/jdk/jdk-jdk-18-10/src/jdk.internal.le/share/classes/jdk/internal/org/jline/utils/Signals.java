/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.Objects;

/**
 * Signals helpers.
 *
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 * @since 3.0
 */
public final class Signals {

    private Signals() {
    }

    /**
     *
     * @param name the signal, CONT, STOP, etc...
     * @param handler the callback to run
     *
     * @return an object that needs to be passed to the {@link #unregister(String, Object)}
     *         method to unregister the handler
     */
    public static Object register(String name, Runnable handler) {
        Objects.requireNonNull(handler);
        return register(name, handler, handler.getClass().getClassLoader());
    }

    public static Object register(String name, final Runnable handler, ClassLoader loader) {
        try {
            Class<?> signalHandlerClass = Class.forName("sun.misc.SignalHandler");
            // Implement signal handler
            Object signalHandler = Proxy.newProxyInstance(loader,
                    new Class<?>[]{signalHandlerClass}, (proxy, method, args) -> {
                        // only method we are proxying is handle()
                        if (method.getDeclaringClass() == Object.class) {
                            if ("toString".equals(method.getName())) {
                                return handler.toString();
                            }
                        } else if (method.getDeclaringClass() == signalHandlerClass) {
                            Log.trace(() -> "Calling handler " + toString(handler) + " for signal " + name);
                            handler.run();
                        }
                        return null;
                    });
            return doRegister(name, signalHandler);
        } catch (Exception e) {
            // Ignore this one too, if the above failed, the signal API is incompatible with what we're expecting
            Log.debug("Error registering handler for signal ", name, e);
            return null;
        }
    }

    public static Object registerDefault(String name) {
        try {
            Class<?> signalHandlerClass = Class.forName("sun.misc.SignalHandler");
            return doRegister(name, signalHandlerClass.getField("SIG_DFL").get(null));
        } catch (Exception e) {
            // Ignore this one too, if the above failed, the signal API is incompatible with what we're expecting
            Log.debug("Error registering default handler for signal ", name, e);
            return null;
        }
    }

    public static void unregister(String name, Object previous) {
        try {
            // We should make sure the current signal is the one we registered
            if (previous != null) {
                doRegister(name, previous);
            }
        } catch (Exception e) {
            // Ignore
            Log.debug("Error unregistering handler for signal ", name, e);
        }
    }

    private static Object doRegister(String name, Object handler) throws Exception {
        Log.trace(() -> "Registering signal " + name + " with handler " + toString(handler));
        Class<?> signalClass = Class.forName("sun.misc.Signal");
        Constructor<?> constructor = signalClass.getConstructor(String.class);
        Object signal;
        try {
            signal = constructor.newInstance(name);
        } catch (IllegalArgumentException e) {
            Log.trace(() -> "Ignoring unsupported signal " + name);
            return null;
        }
        Class<?> signalHandlerClass = Class.forName("sun.misc.SignalHandler");
        return signalClass.getMethod("handle", signalClass, signalHandlerClass)
                .invoke(null, signal, handler);
    }

    @SuppressWarnings("")
    private static String toString(Object handler) {
        try {
            Class<?> signalHandlerClass = Class.forName("sun.misc.SignalHandler");
            if (handler == signalHandlerClass.getField("SIG_DFL").get(null)) {
                return "SIG_DFL";
            }
            if (handler == signalHandlerClass.getField("SIG_IGN").get(null)) {
                return "SIG_IGN";
            }
        } catch (Throwable t) {
            // ignore
        }
        return handler != null ? handler.toString() : "null";
    }

}
