/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessControlException;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.ResourceBundle;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.lang.System.LoggerFinder;
import java.lang.System.Logger;
import java.util.stream.Stream;
import sun.util.logging.PlatformLogger;

/**
 * @test
 * @bug     8140364
 * @summary JDK implementation specific unit test for JDK internal artifacts.
 *          Tests all bridge methods with the a custom backend whose
 *          loggers implement PlatformLogger.Bridge.
 * @modules java.base/sun.util.logging
 *          java.base/jdk.internal.logger
 *          java.logging
 * @build CustomSystemClassLoader LogProducerFinder LoggerBridgeTest
 * @run  main/othervm -Djava.system.class.loader=CustomSystemClassLoader LoggerBridgeTest NOSECURITY
 * @run  main/othervm -Djava.security.manager=allow -Djava.system.class.loader=CustomSystemClassLoader LoggerBridgeTest NOPERMISSIONS
 * @run  main/othervm -Djava.security.manager=allow -Djava.system.class.loader=CustomSystemClassLoader LoggerBridgeTest WITHPERMISSIONS
 * @author danielfuchs
 */
public class LoggerBridgeTest {

    public static final RuntimePermission LOGGERFINDER_PERMISSION =
                new RuntimePermission("loggerFinder");

    final static AtomicLong sequencer = new AtomicLong();
    final static boolean VERBOSE = false;
    static final ThreadLocal<AtomicBoolean> allowControl = new ThreadLocal<AtomicBoolean>() {
        @Override
        protected AtomicBoolean initialValue() {
            return  new AtomicBoolean(false);
        }
    };
    static final ThreadLocal<AtomicBoolean> allowAccess = new ThreadLocal<AtomicBoolean>() {
        @Override
        protected AtomicBoolean initialValue() {
            return  new AtomicBoolean(false);
        }
    };
    static final ThreadLocal<AtomicBoolean> allowAll = new ThreadLocal<AtomicBoolean>() {
        @Override
        protected AtomicBoolean initialValue() {
            return  new AtomicBoolean(false);
        }
    };

    public static final Queue<LogEvent> eventQueue = new ArrayBlockingQueue<>(128);

    public static final class LogEvent implements Cloneable {

        public LogEvent() {
            this(sequencer.getAndIncrement());
        }

        LogEvent(long sequenceNumber) {
            this.sequenceNumber = sequenceNumber;
        }

        long sequenceNumber;
        boolean isLoggable;
        String loggerName;
        sun.util.logging.PlatformLogger.Level level;
        ResourceBundle bundle;
        Throwable thrown;
        Object[] args;
        String msg;
        Supplier<String> supplier;
        String className;
        String methodName;

        Object[] toArray() {
            return new Object[] {
                sequenceNumber,
                loggerName,
                level,
                isLoggable,
                bundle,
                msg,
                supplier,
                thrown,
                args,
                className,
                methodName,
            };
        }

        @Override
        public String toString() {
            return Arrays.deepToString(toArray());
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof LogEvent
                    && Objects.deepEquals(this.toArray(), ((LogEvent)obj).toArray());
        }

        @Override
        public int hashCode() {
            return Objects.hash(toArray());
        }

        public LogEvent cloneWith(long sequenceNumber)
                throws CloneNotSupportedException {
            LogEvent cloned = (LogEvent)super.clone();
            cloned.sequenceNumber = sequenceNumber;
            return cloned;
        }

        public static LogEvent of(long sequenceNumber,
                boolean isLoggable, String name,
                sun.util.logging.PlatformLogger.Level level, ResourceBundle bundle,
                String key, Throwable thrown, Object... params) {
            return LogEvent.of(sequenceNumber, isLoggable, name,
                    null, null, level, bundle, key,
                    thrown, params);
        }

        public static LogEvent of(long sequenceNumber,
                boolean isLoggable, String name,
                sun.util.logging.PlatformLogger.Level level, ResourceBundle bundle,
                Supplier<String> supplier, Throwable thrown, Object... params) {
            return LogEvent.of(sequenceNumber, isLoggable, name,
                    null, null, level, bundle, supplier,
                    thrown, params);
        }

        public static LogEvent of(long sequenceNumber,
                boolean isLoggable, String name,
                String className, String methodName,
                sun.util.logging.PlatformLogger.Level level, ResourceBundle bundle,
                String key, Throwable thrown, Object... params) {
            LogEvent evt = new LogEvent(sequenceNumber);
            evt.loggerName = name;
            evt.level = level;
            evt.args = params;
            evt.bundle = bundle;
            evt.thrown = thrown;
            evt.msg = key;
            evt.isLoggable = isLoggable;
            evt.className = className;
            evt.methodName = methodName;
            return evt;
        }

        public static LogEvent of(boolean isLoggable, String name,
                String className, String methodName,
                sun.util.logging.PlatformLogger.Level level, ResourceBundle bundle,
                String key, Throwable thrown, Object... params) {
            return LogEvent.of(sequencer.getAndIncrement(), isLoggable, name,
                    className, methodName, level, bundle, key, thrown, params);
        }

        public static LogEvent of(long sequenceNumber,
                boolean isLoggable, String name,
                String className, String methodName,
                sun.util.logging.PlatformLogger.Level level, ResourceBundle bundle,
                Supplier<String> supplier, Throwable thrown, Object... params) {
            LogEvent evt = new LogEvent(sequenceNumber);
            evt.loggerName = name;
            evt.level = level;
            evt.args = params;
            evt.bundle = bundle;
            evt.thrown = thrown;
            evt.supplier = supplier;
            evt.isLoggable = isLoggable;
            evt.className = className;
            evt.methodName = methodName;
            return evt;
        }

        public static LogEvent of(boolean isLoggable, String name,
                String className, String methodName,
                sun.util.logging.PlatformLogger.Level level, ResourceBundle bundle,
                Supplier<String> supplier, Throwable thrown, Object... params) {
            return LogEvent.of(sequencer.getAndIncrement(), isLoggable, name,
                    className, methodName, level, bundle, supplier, thrown, params);
        }

    }
    static final Class<?> providerClass;
    static {
        try {
            // Preload classes before the security manager is on.
            providerClass = ClassLoader.getSystemClassLoader().loadClass("LogProducerFinder");
            ((LoggerFinder)providerClass.newInstance()).getLogger("foo", providerClass.getModule());
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static class LoggerImpl implements System.Logger, PlatformLogger.Bridge {
        private final String name;
        private PlatformLogger.Level level = PlatformLogger.Level.INFO;
        private PlatformLogger.Level OFF = PlatformLogger.Level.OFF;
        private PlatformLogger.Level FINE = PlatformLogger.Level.FINE;
        private PlatformLogger.Level FINER = PlatformLogger.Level.FINER;
        private PlatformLogger.Level FINEST = PlatformLogger.Level.FINEST;
        private PlatformLogger.Level CONFIG = PlatformLogger.Level.CONFIG;
        private PlatformLogger.Level INFO = PlatformLogger.Level.INFO;
        private PlatformLogger.Level WARNING = PlatformLogger.Level.WARNING;
        private PlatformLogger.Level SEVERE = PlatformLogger.Level.SEVERE;

        public LoggerImpl(String name) {
            this.name = name;
        }

        public void configureLevel(PlatformLogger.Level level) {
            this.level = level;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isLoggable(Level level) {
            return this.level != OFF && this.level.intValue() <= level.getSeverity();
        }

        @Override
        public void log(Level level, ResourceBundle bundle,
                        String key, Throwable thrown) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void log(Level level, ResourceBundle bundle,
                        String format, Object... params) {
            throw new UnsupportedOperationException();
        }

        void log(LogEvent event) {
            eventQueue.add(event);
        }

        @Override
        public void log(Level level, Supplier<String> msgSupplier) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void log(Level level, Supplier<String> msgSupplier,
                        Throwable thrown) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void log(PlatformLogger.Level level, String msg) {
            log(LogEvent.of(isLoggable(level), name, null, null,
                    level, null, msg, null, (Object[]) null));
        }

        @Override
        public void log(PlatformLogger.Level level,
                        Supplier<String> msgSupplier) {
            log(LogEvent.of(isLoggable(level), name, null, null,
                    level, null, msgSupplier, null, (Object[]) null));
        }

        @Override
        public void log(PlatformLogger.Level level, String msg,
                        Object... params) {
            log(LogEvent.of(isLoggable(level), name, null, null,
                    level, null, msg, null, params));
        }

        @Override
        public void log(PlatformLogger.Level level, String msg,
                        Throwable thrown) {
            log(LogEvent.of(isLoggable(level), name, null, null,
                    level, null, msg, thrown, (Object[]) null));
        }

        @Override
        public void log(PlatformLogger.Level level, Throwable thrown,
                        Supplier<String> msgSupplier) {
            log(LogEvent.of(isLoggable(level), name, null, null,
                    level, null, msgSupplier, thrown, (Object[]) null));
        }

        @Override
        public void logp(PlatformLogger.Level level, String sourceClass,
                         String sourceMethod, String msg) {
            log(LogEvent.of(isLoggable(level), name,
                    sourceClass, sourceMethod,
                    level, null, msg, null, (Object[]) null));
        }

        @Override
        public void logp(PlatformLogger.Level level, String sourceClass,
                         String sourceMethod, Supplier<String> msgSupplier) {
            log(LogEvent.of(isLoggable(level), name,
                    sourceClass, sourceMethod,
                    level, null, msgSupplier, null, (Object[]) null));
        }

        @Override
        public void logp(PlatformLogger.Level level, String sourceClass,
                         String sourceMethod, String msg, Object... params) {
            log(LogEvent.of(isLoggable(level), name,
                    sourceClass, sourceMethod,
                    level, null, msg, null, params));
        }

        @Override
        public void logp(PlatformLogger.Level level, String sourceClass,
                         String sourceMethod, String msg, Throwable thrown) {
            log(LogEvent.of(isLoggable(level), name,
                    sourceClass, sourceMethod,
                    level, null, msg, thrown, (Object[]) null));
        }

        @Override
        public void logp(PlatformLogger.Level level, String sourceClass,
                         String sourceMethod, Throwable thrown,
                         Supplier<String> msgSupplier) {
            log(LogEvent.of(isLoggable(level), name,
                    sourceClass, sourceMethod,
                    level, null, msgSupplier, thrown, (Object[]) null));
        }

        @Override
        public void logrb(PlatformLogger.Level level, String sourceClass,
                          String sourceMethod, ResourceBundle bundle, String msg,
                          Object... params) {
            log(LogEvent.of(isLoggable(level), name,
                    sourceClass, sourceMethod,
                    level, bundle, msg, null, params));
        }

        @Override
        public void logrb(PlatformLogger.Level level, ResourceBundle bundle,
                          String msg, Object... params) {
            log(LogEvent.of(isLoggable(level), name, null, null,
                    level, bundle, msg, null, params));
        }

        @Override
        public void logrb(PlatformLogger.Level level, String sourceClass,
                          String sourceMethod, ResourceBundle bundle, String msg,
                          Throwable thrown) {
            log(LogEvent.of(isLoggable(level), name,
                    sourceClass, sourceMethod,
                    level, bundle, msg, thrown, (Object[]) null));
        }

        @Override
        public void logrb(PlatformLogger.Level level, ResourceBundle bundle,
                          String msg, Throwable thrown) {
            log(LogEvent.of(isLoggable(level), name, null, null,
                    level, bundle, msg, thrown, (Object[]) null));
        }

        @Override
        public boolean isLoggable(PlatformLogger.Level level) {
            return this.level != OFF && level.intValue()
                    >= this.level.intValue();
        }

        @Override
        public boolean isEnabled() {
            return this.level != OFF;
        }

    }

    static ClassLoader getClassLoader(Module m) {
        final boolean before = allowAll.get().getAndSet(true);
        try {
            return m.getClassLoader();
        } finally {
            allowAll.get().set(before);
        }
    }

    static final sun.util.logging.PlatformLogger.Level[] julLevels = {
        sun.util.logging.PlatformLogger.Level.ALL,
        sun.util.logging.PlatformLogger.Level.FINEST,
        sun.util.logging.PlatformLogger.Level.FINER,
        sun.util.logging.PlatformLogger.Level.FINE,
        sun.util.logging.PlatformLogger.Level.CONFIG,
        sun.util.logging.PlatformLogger.Level.INFO,
        sun.util.logging.PlatformLogger.Level.WARNING,
        sun.util.logging.PlatformLogger.Level.SEVERE,
        sun.util.logging.PlatformLogger.Level.OFF,
    };

    public static class MyBundle extends ResourceBundle {

        final ConcurrentHashMap<String,String> map = new ConcurrentHashMap<>();

        @Override
        protected Object handleGetObject(String key) {
            if (key.contains(" (translated)")) {
                throw new RuntimeException("Unexpected key: " + key);
            }
            return map.computeIfAbsent(key, k -> k + " (translated)");
        }

        @Override
        public Enumeration<String> getKeys() {
            return Collections.enumeration(map.keySet());
        }

    }

    public static class MyHandler extends Handler {

        @Override
        public java.util.logging.Level getLevel() {
            return java.util.logging.Level.ALL;
        }

        @Override
        public void publish(LogRecord record) {
            eventQueue.add(LogEvent.of(sequencer.getAndIncrement(),
                    true, record.getLoggerName(),
                    record.getSourceClassName(),
                    record.getSourceMethodName(),
                    PlatformLogger.Level.valueOf(record.getLevel().getName()),
                    record.getResourceBundle(), record.getMessage(),
                    record.getThrown(), record.getParameters()));
        }
        @Override
        public void flush() {
        }
        @Override
        public void close() throws SecurityException {
        }

    }

    public static class MyLoggerBundle extends MyBundle {

    }

    final static Method lazyGetLogger;
    static {
        // jdk.internal.logging.LoggerBridge.getLogger(name, caller)
        try {
            Class<?> bridgeClass = Class.forName("jdk.internal.logger.LazyLoggers");
            lazyGetLogger = bridgeClass.getDeclaredMethod("getLogger",
                    String.class, Module.class);
            lazyGetLogger.setAccessible(true);
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    static Logger getLogger(LoggerFinder provider, String name, Module caller) {
        Logger logger;
        try {
            logger = Logger.class.cast(lazyGetLogger.invoke(null, name, caller));
        } catch (Throwable x) {
            Throwable t = (x instanceof InvocationTargetException) ?
                    ((InvocationTargetException)x).getTargetException() : x;
            if (t instanceof RuntimeException) {
                throw (RuntimeException)t;
            } else if (t instanceof Exception) {
                throw new RuntimeException(t);
            } else {
                throw (Error)t;
            }
        }
        // The method above does not throw exception...
        // call the provider here to verify that an exception would have
        // been thrown by the provider.
        if (logger != null && caller == Thread.class.getModule()) {
            Logger log = provider.getLogger(name, caller);
        }
        return logger;
    }

    static Logger getLogger(LoggerFinder provider, String name, ResourceBundle bundle, Module caller) {
        if (getClassLoader(caller) != null) {
            return System.getLogger(name,bundle);
        } else {
            return provider.getLocalizedLogger(name, bundle, caller);
        }
    }

    static PlatformLogger.Bridge convert(Logger logger) {
        return PlatformLogger.Bridge.convert(logger);
    }

    static enum TestCases {NOSECURITY, NOPERMISSIONS, WITHPERMISSIONS};

    static void setSecurityManager() {
        if (System.getSecurityManager() == null) {
            Policy.setPolicy(new SimplePolicy(allowControl, allowAccess, allowAll));
            System.setSecurityManager(new SecurityManager());
        }
    }

    public static void main(String[] args) {
        if (args.length == 0)
            args = new String[] {
                //"NOSECURITY",
                "NOPERMISSIONS",
                "WITHPERMISSIONS"
            };


        Stream.of(args).map(TestCases::valueOf).forEach((testCase) -> {
            LoggerFinder provider;
            switch (testCase) {
                case NOSECURITY:
                    System.out.println("\n*** Without Security Manager\n");
                    provider = LoggerFinder.getLoggerFinder();
                    test(provider, true);
                    System.out.println("Tetscase count: " + sequencer.get());
                    break;
                case NOPERMISSIONS:
                    System.out.println("\n*** With Security Manager, without permissions\n");
                    setSecurityManager();
                    try {
                        provider = LoggerFinder.getLoggerFinder();
                        throw new RuntimeException("Expected exception not raised");
                    } catch (AccessControlException x) {
                        if (!LOGGERFINDER_PERMISSION.equals(x.getPermission())) {
                            throw new RuntimeException("Unexpected permission check", x);
                        }
                        final boolean control = allowControl.get().get();
                        try {
                            allowControl.get().set(true);
                            provider = LoggerFinder.getLoggerFinder();
                        } finally {
                            allowControl.get().set(control);
                        }
                    }
                    test(provider, false);
                    System.out.println("Tetscase count: " + sequencer.get());
                    break;
                case WITHPERMISSIONS:
                    System.out.println("\n*** With Security Manager, with control permission\n");
                    setSecurityManager();
                    final boolean control = allowControl.get().get();
                    try {
                        allowControl.get().set(true);
                        provider = LoggerFinder.getLoggerFinder();
                        test(provider, true);
                    } finally {
                        allowControl.get().set(control);
                    }
                    break;
                default:
                    throw new RuntimeException("Unknown test case: " + testCase);
            }
        });
        System.out.println("\nPASSED: Tested " + sequencer.get() + " cases.");
    }

    public static void test(LoggerFinder provider, boolean hasRequiredPermissions) {

        ResourceBundle loggerBundle = ResourceBundle.getBundle(MyLoggerBundle.class.getName());
        final Map<Object, String> loggerDescMap = new HashMap<>();


        Logger appLogger1 = System.getLogger("foo");
        loggerDescMap.put(appLogger1, "System.getLogger(\"foo\")");

        Logger sysLogger1 = null;
        try {
            sysLogger1 = getLogger(provider, "foo", Thread.class.getModule());
            loggerDescMap.put(sysLogger1, "provider.getLogger(\"foo\", Thread.class.getModule())");
            if (!hasRequiredPermissions) {
                throw new RuntimeException("Managed to obtain a system logger without permission");
            }
        } catch (AccessControlException acx) {
            if (hasRequiredPermissions) {
                throw new RuntimeException("Unexpected security exception: ", acx);
            }
            if (!acx.getPermission().equals(LOGGERFINDER_PERMISSION)) {
                throw new RuntimeException("Unexpected permission in exception: " + acx, acx);
            }
            System.out.println("Got expected exception for system logger: " + acx);
        }


        Logger appLogger2 =
                System.getLogger("foo", loggerBundle);
        loggerDescMap.put(appLogger2, "System.getLogger(\"foo\", loggerBundle)");

        Logger sysLogger2 = null;
        try {
            sysLogger2 = getLogger(provider, "foo", loggerBundle, Thread.class.getModule());
            loggerDescMap.put(sysLogger2, "provider.getLogger(\"foo\", loggerBundle, Thread.class.getModule())");
            if (!hasRequiredPermissions) {
                throw new RuntimeException("Managed to obtain a system logger without permission");
            }
        } catch (AccessControlException acx) {
            if (hasRequiredPermissions) {
                throw new RuntimeException("Unexpected security exception: ", acx);
            }
            if (!acx.getPermission().equals(LOGGERFINDER_PERMISSION)) {
                throw new RuntimeException("Unexpected permission in exception: " + acx, acx);
            }
            System.out.println("Got expected exception for localized system logger: " + acx);
        }
        if (hasRequiredPermissions && appLogger2 == sysLogger2) {
            throw new RuntimeException("identical loggers");
        }
        if (appLogger2 == appLogger1) {
            throw new RuntimeException("identical loggers");
        }
        if (hasRequiredPermissions && sysLogger2 == sysLogger1) {
            throw new RuntimeException("identical loggers");
        }


        final LoggerImpl appSink;
        final LoggerImpl sysSink;
        boolean old = allowControl.get().get();
        allowControl.get().set(true);
        try {
           appSink = LoggerImpl.class.cast(
                   provider.getLogger("foo",  LoggerBridgeTest.class.getModule()));
           sysSink = LoggerImpl.class.cast(
                        provider.getLogger("foo", Thread.class.getModule()));
        } finally {
            allowControl.get().set(old);
        }

        testLogger(provider, loggerDescMap, "foo", null, convert(appLogger1), appSink);
        if (hasRequiredPermissions) {
            testLogger(provider, loggerDescMap, "foo", null, convert(sysLogger1), sysSink);
        }
        testLogger(provider, loggerDescMap, "foo", loggerBundle, convert(appLogger2), appSink);
        if (hasRequiredPermissions) {
            testLogger(provider, loggerDescMap, "foo", loggerBundle, convert(sysLogger2), sysSink);
        }
    }

    public static class Foo {

    }

    static void verbose(String msg) {
       if (VERBOSE) {
           System.out.println(msg);
       }
    }

    static void checkLogEvent(LoggerFinder provider, String desc,
            LogEvent expected) {
        LogEvent actual =  eventQueue.poll();
        if (!expected.equals(actual)) {
            throw new RuntimeException("mismatch for " + desc
                    + "\n\texpected=" + expected
                    + "\n\t  actual=" + actual);
        } else {
            verbose("Got expected results for "
                    + desc + "\n\t" + expected);
        }
    }

    static void checkLogEvent(LoggerFinder provider, String desc,
            LogEvent expected, boolean expectNotNull) {
        LogEvent actual =  eventQueue.poll();
        if (actual == null && !expectNotNull) return;
        if (actual != null && !expectNotNull) {
            throw new RuntimeException("Unexpected log event found for " + desc
                + "\n\tgot: " + actual);
        }
        if (!expected.equals(actual)) {
            throw new RuntimeException("mismatch for " + desc
                    + "\n\texpected=" + expected
                    + "\n\t  actual=" + actual);
        } else {
            verbose("Got expected results for "
                    + desc + "\n\t" + expected);
        }
    }

    static void setLevel(LoggerImpl sink,
            sun.util.logging.PlatformLogger.Level loggerLevel) {
        sink.configureLevel(loggerLevel);
    }

    // Calls the methods defined on PlatformLogger.Bridge and verify the
    // parameters received by the underlying LoggerImpl
    // logger.
    private static void testLogger(LoggerFinder provider,
            Map<Object, String> loggerDescMap,
            String name,
            ResourceBundle loggerBundle,
            PlatformLogger.Bridge logger,
            LoggerImpl sink) {

        System.out.println("Testing " + loggerDescMap.get(logger) + "[" + logger + "]");
        final sun.util.logging.PlatformLogger.Level OFF = sun.util.logging.PlatformLogger.Level.OFF;

        Foo foo = new Foo();
        String fooMsg = foo.toString();
        System.out.println("\tlogger.log(messageLevel, fooMsg)");
        System.out.println("\tlogger.<level>(fooMsg)");
        for (sun.util.logging.PlatformLogger.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (sun.util.logging.PlatformLogger.Level messageLevel :julLevels) {
                String desc = "logger.log(messageLevel, fooMsg): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            loggerLevel != OFF && messageLevel.intValue() >= loggerLevel.intValue(),
                            name, messageLevel, loggerBundle,
                            fooMsg, (Throwable)null, (Object[])null);
                logger.log(messageLevel, fooMsg);
                checkLogEvent(provider, desc, expected);
            }
        }

        Supplier<String> supplier = new Supplier<String>() {
            @Override
            public String get() {
                return this.toString();
            }
        };
        System.out.println("\tlogger.log(messageLevel, supplier)");
        System.out.println("\tlogger.<level>(supplier)");
        for (sun.util.logging.PlatformLogger.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (sun.util.logging.PlatformLogger.Level messageLevel :julLevels) {
                String desc = "logger.log(messageLevel, supplier): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            loggerLevel != OFF && messageLevel.intValue() >= loggerLevel.intValue(),
                            name, messageLevel, null,
                            supplier, (Throwable)null, (Object[])null);
                logger.log(messageLevel, supplier);
                checkLogEvent(provider, desc, expected);
            }
        }

        String format = "two params [{1} {2}]";
        Object arg1 = foo;
        Object arg2 = fooMsg;
        System.out.println("\tlogger.log(messageLevel, format, arg1, arg2)");
        for (sun.util.logging.PlatformLogger.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (sun.util.logging.PlatformLogger.Level messageLevel :julLevels) {
                String desc = "logger.log(messageLevel, format, foo, fooMsg): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            loggerLevel != OFF && messageLevel.intValue() >= loggerLevel.intValue(),
                            name, messageLevel, loggerBundle,
                            format, (Throwable)null, arg1, arg2);
                logger.log(messageLevel, format, arg1, arg2);
                checkLogEvent(provider, desc, expected);
            }
        }

        Throwable thrown = new Exception("OK: log me!");
        System.out.println("\tlogger.log(messageLevel, fooMsg, thrown)");
        for (sun.util.logging.PlatformLogger.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (sun.util.logging.PlatformLogger.Level messageLevel :julLevels) {
                String desc = "logger.log(messageLevel, fooMsg, thrown): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            loggerLevel != OFF && messageLevel.intValue() >= loggerLevel.intValue(),
                            name, messageLevel, loggerBundle,
                            fooMsg, thrown, (Object[])null);
                logger.log(messageLevel, fooMsg, thrown);
                checkLogEvent(provider, desc, expected);
            }
        }

        System.out.println("\tlogger.log(messageLevel, thrown, supplier)");
        for (sun.util.logging.PlatformLogger.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (sun.util.logging.PlatformLogger.Level messageLevel :julLevels) {
                String desc = "logger.log(messageLevel, thrown, supplier): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            loggerLevel != OFF && messageLevel.intValue() >= loggerLevel.intValue(),
                            name, messageLevel, null,
                            supplier, thrown, (Object[])null);
                logger.log(messageLevel, thrown, supplier);
                checkLogEvent(provider, desc, expected);
            }
        }

        String sourceClass = "blah.Blah";
        String sourceMethod = "blih";
        System.out.println("\tlogger.logp(messageLevel, sourceClass, sourceMethod, fooMsg)");
        for (sun.util.logging.PlatformLogger.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (sun.util.logging.PlatformLogger.Level messageLevel :julLevels) {
                String desc = "logger.logp(messageLevel, sourceClass, sourceMethod, fooMsg): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            loggerLevel != OFF && messageLevel.intValue() >= loggerLevel.intValue(),
                            name, sourceClass, sourceMethod, messageLevel, loggerBundle,
                            fooMsg, (Throwable)null, (Object[])null);
                logger.logp(messageLevel, sourceClass, sourceMethod, fooMsg);
                checkLogEvent(provider, desc, expected);
            }
        }

        System.out.println("\tlogger.logp(messageLevel, sourceClass, sourceMethod, supplier)");
        for (sun.util.logging.PlatformLogger.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (sun.util.logging.PlatformLogger.Level messageLevel :julLevels) {
                String desc = "logger.logp(messageLevel, sourceClass, sourceMethod, supplier): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            loggerLevel != OFF && messageLevel.intValue() >= loggerLevel.intValue(),
                            name, sourceClass, sourceMethod, messageLevel, null,
                            supplier, (Throwable)null, (Object[])null);
                logger.logp(messageLevel, sourceClass, sourceMethod, supplier);
                checkLogEvent(provider, desc, expected);
            }
        }

        System.out.println("\tlogger.logp(messageLevel, sourceClass, sourceMethod, format, arg1, arg2)");
        for (sun.util.logging.PlatformLogger.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (sun.util.logging.PlatformLogger.Level messageLevel :julLevels) {
                String desc = "logger.logp(messageLevel, sourceClass, sourceMethod, format, arg1, arg2): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            loggerLevel != OFF && messageLevel.intValue() >= loggerLevel.intValue(),
                            name, sourceClass, sourceMethod, messageLevel, loggerBundle,
                            format, (Throwable)null, arg1, arg2);
                logger.logp(messageLevel, sourceClass, sourceMethod, format, arg1, arg2);
                checkLogEvent(provider, desc, expected);
            }
        }

        System.out.println("\tlogger.logp(messageLevel, sourceClass, sourceMethod, fooMsg, thrown)");
        for (sun.util.logging.PlatformLogger.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (sun.util.logging.PlatformLogger.Level messageLevel :julLevels) {
                String desc = "logger.logp(messageLevel, sourceClass, sourceMethod, fooMsg, thrown): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            loggerLevel != OFF && messageLevel.intValue() >= loggerLevel.intValue(),
                            name, sourceClass, sourceMethod, messageLevel, loggerBundle,
                            fooMsg, thrown, (Object[])null);
                logger.logp(messageLevel, sourceClass, sourceMethod, fooMsg, thrown);
                checkLogEvent(provider, desc, expected);
            }
        }

        System.out.println("\tlogger.logp(messageLevel, sourceClass, sourceMethod, thrown, supplier)");
        for (sun.util.logging.PlatformLogger.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (sun.util.logging.PlatformLogger.Level messageLevel :julLevels) {
                String desc = "logger.logp(messageLevel, sourceClass, sourceMethod, thrown, supplier): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            loggerLevel != OFF && messageLevel.intValue() >= loggerLevel.intValue(),
                            name, sourceClass, sourceMethod, messageLevel, null,
                            supplier, thrown, (Object[])null);
                logger.logp(messageLevel, sourceClass, sourceMethod, thrown, supplier);
                checkLogEvent(provider, desc, expected);
            }
        }

        ResourceBundle bundle = ResourceBundle.getBundle(MyBundle.class.getName());
        System.out.println("\tlogger.logrb(messageLevel, bundle, format, arg1, arg2)");
        for (sun.util.logging.PlatformLogger.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (sun.util.logging.PlatformLogger.Level messageLevel :julLevels) {
                String desc = "logger.logrb(messageLevel, bundle, format, arg1, arg2): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            loggerLevel != OFF && messageLevel.intValue() >= loggerLevel.intValue(),
                            name, messageLevel, bundle,
                            format, (Throwable)null, arg1, arg2);
                logger.logrb(messageLevel, bundle, format, arg1, arg2);
                checkLogEvent(provider, desc, expected);
            }
        }

        System.out.println("\tlogger.logrb(messageLevel, bundle, msg, thrown)");
        for (sun.util.logging.PlatformLogger.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (sun.util.logging.PlatformLogger.Level messageLevel :julLevels) {
                String desc = "logger.logrb(messageLevel, bundle, msg, thrown): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            loggerLevel != OFF && messageLevel.intValue() >= loggerLevel.intValue(),
                            name, messageLevel, bundle,
                            fooMsg, thrown, (Object[])null);
                logger.logrb(messageLevel, bundle, fooMsg, thrown);
                checkLogEvent(provider, desc, expected);
            }
        }

        System.out.println("\tlogger.logrb(messageLevel, sourceClass, sourceMethod, bundle, format, arg1, arg2)");
        for (sun.util.logging.PlatformLogger.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (sun.util.logging.PlatformLogger.Level messageLevel :julLevels) {
                String desc = "logger.logrb(messageLevel, sourceClass, sourceMethod, bundle, format, arg1, arg2): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            loggerLevel != OFF && messageLevel.intValue() >= loggerLevel.intValue(),
                            name, sourceClass, sourceMethod, messageLevel, bundle,
                            format, (Throwable)null, arg1, arg2);
                logger.logrb(messageLevel, sourceClass, sourceMethod, bundle, format, arg1, arg2);
                checkLogEvent(provider, desc, expected);
            }
        }

        System.out.println("\tlogger.logrb(messageLevel, sourceClass, sourceMethod, bundle, msg, thrown)");
        for (sun.util.logging.PlatformLogger.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (sun.util.logging.PlatformLogger.Level messageLevel :julLevels) {
                String desc = "logger.logrb(messageLevel, sourceClass, sourceMethod, bundle, msg, thrown): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            loggerLevel != OFF && messageLevel.intValue() >= loggerLevel.intValue(),
                            name, sourceClass, sourceMethod, messageLevel, bundle,
                            fooMsg, thrown, (Object[])null);
                logger.logrb(messageLevel, sourceClass, sourceMethod, bundle, fooMsg, thrown);
                checkLogEvent(provider, desc, expected);
            }
        }
    }

    final static class PermissionsBuilder {
        final Permissions perms;
        public PermissionsBuilder() {
            this(new Permissions());
        }
        public PermissionsBuilder(Permissions perms) {
            this.perms = perms;
        }
        public PermissionsBuilder add(Permission p) {
            perms.add(p);
            return this;
        }
        public PermissionsBuilder addAll(PermissionCollection col) {
            if (col != null) {
                for (Enumeration<Permission> e = col.elements(); e.hasMoreElements(); ) {
                    perms.add(e.nextElement());
                }
            }
            return this;
        }
        public Permissions toPermissions() {
            final PermissionsBuilder builder = new PermissionsBuilder();
            builder.addAll(perms);
            return builder.perms;
        }
    }

    public static class SimplePolicy extends Policy {
        final static RuntimePermission CONTROL = LOGGERFINDER_PERMISSION;
        final static RuntimePermission ACCESS_LOGGER = new RuntimePermission("accessClassInPackage.jdk.internal.logger");
        final static RuntimePermission ACCESS_LOGGING = new RuntimePermission("accessClassInPackage.sun.util.logging");

        static final Policy DEFAULT_POLICY = Policy.getPolicy();

        final Permissions permissions;
        final Permissions allPermissions;
        final ThreadLocal<AtomicBoolean> allowControl;
        final ThreadLocal<AtomicBoolean> allowAccess;
        final ThreadLocal<AtomicBoolean> allowAll;
        public SimplePolicy(ThreadLocal<AtomicBoolean> allowControl,
                ThreadLocal<AtomicBoolean> allowAccess,
                ThreadLocal<AtomicBoolean> allowAll) {
            this.allowControl = allowControl;
            this.allowAccess = allowAccess;
            this.allowAll = allowAll;
            permissions = new Permissions();
            allPermissions = new PermissionsBuilder()
                    .add(new java.security.AllPermission())
                    .toPermissions();
        }

        Permissions getPermissions() {
            if (allowControl.get().get() || allowAccess.get().get() || allowAll.get().get()) {
                PermissionsBuilder builder =  new PermissionsBuilder()
                        .addAll(permissions);
                if (allowControl.get().get()) {
                    builder.add(CONTROL);
                }
                if (allowAccess.get().get()) {
                    builder.add(ACCESS_LOGGER);
                    builder.add(ACCESS_LOGGING);
                }
                if (allowAll.get().get()) {
                    builder.addAll(allPermissions);
                }
                return builder.toPermissions();
            }
            return permissions;
        }

        @Override
        public boolean implies(ProtectionDomain domain, Permission permission) {
            return getPermissions().implies(permission) || DEFAULT_POLICY.implies(domain, permission);
        }

        @Override
        public PermissionCollection getPermissions(CodeSource codesource) {
            return new PermissionsBuilder().addAll(getPermissions()).toPermissions();
        }

        @Override
        public PermissionCollection getPermissions(ProtectionDomain domain) {
            return new PermissionsBuilder().addAll(getPermissions()).toPermissions();
        }
    }
}
