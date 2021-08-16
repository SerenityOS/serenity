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
import java.lang.System.LoggerFinder;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.stream.Stream;

/**
 * @test
 * @bug     8140364 8145686
 * @summary Tests default loggers returned by System.getLogger, and in
 *          particular the implementation of the the System.Logger method
 *          performed by the default binding.
 * @modules java.logging
 * @build DefaultLoggerTest AccessSystemLogger
 * @run driver AccessSystemLogger
 * @run main/othervm -Xbootclasspath/a:boot DefaultLoggerTest NOSECURITY
 * @run main/othervm -Xbootclasspath/a:boot -Djava.security.manager=allow DefaultLoggerTest NOPERMISSIONS
 * @run main/othervm -Xbootclasspath/a:boot -Djava.security.manager=allow DefaultLoggerTest WITHPERMISSIONS
 * @run main/othervm -Xbootclasspath/a:boot -Djava.security.manager=allow DefaultLoggerTest WITHCUSTOMWRAPPERS
 * @run main/othervm -Xbootclasspath/a:boot -Djava.security.manager=allow DefaultLoggerTest WITHREFLECTION
 * @author danielfuchs
 */
public class DefaultLoggerTest {

    final static AtomicLong sequencer = new AtomicLong();
    final static boolean VERBOSE = false;
    static final ThreadLocal<AtomicBoolean> allowControl = new ThreadLocal<AtomicBoolean>() {
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

    public static final class LogEvent {

        public LogEvent() {
            this(sequencer.getAndIncrement());
        }

        LogEvent(long sequenceNumber) {
            this.sequenceNumber = sequenceNumber;
        }

        long sequenceNumber;
        boolean isLoggable;
        String loggerName;
        java.util.logging.Level level;
        ResourceBundle bundle;
        Throwable thrown;
        Object[] args;
        String msg;
        String className;
        String methodName;

        Object[] toArray() {
            return new Object[] {
                sequenceNumber,
                isLoggable,
                loggerName,
                level,
                bundle,
                thrown,
                args,
                msg,
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
        public static LogEvent of(long sequenceNumber,
                boolean isLoggable, String name,
                java.util.logging.Level level, ResourceBundle bundle,
                String key, Throwable thrown, Object... params) {
            return LogEvent.of(sequenceNumber, isLoggable, name,
                    DefaultLoggerTest.class.getName(),
                    "testLogger", level, bundle, key,
                    thrown, params);
        }
        public static LogEvent of(long sequenceNumber,
                boolean isLoggable, String name,
                String className, String methodName,
                java.util.logging.Level level, ResourceBundle bundle,
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

    }

    static java.util.logging.Level mapToJul(Level level) {
        switch (level) {
            case ALL: return java.util.logging.Level.ALL;
            case TRACE: return java.util.logging.Level.FINER;
            case DEBUG: return java.util.logging.Level.FINE;
            case INFO: return java.util.logging.Level.INFO;
            case WARNING: return java.util.logging.Level.WARNING;
            case ERROR: return java.util.logging.Level.SEVERE;
            case OFF: return java.util.logging.Level.OFF;
        }
        throw new InternalError("No such level: " + level);
    }

    static void setLevel(java.util.logging.Logger sink, java.util.logging.Level loggerLevel) {
        boolean before = allowAll.get().get();
        try {
            allowAll.get().set(true);
            sink.setLevel(loggerLevel);
        } finally {
            allowAll.get().set(before);
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
                    record.getLevel(),
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
    public static class MyLoggerBundle extends MyBundle {

    }

    static final AccessSystemLogger accessSystemLogger = new AccessSystemLogger();

    static enum TestCases {NOSECURITY, NOPERMISSIONS, WITHPERMISSIONS,
            WITHCUSTOMWRAPPERS, WITHREFLECTION};

    static void setSecurityManager() {
        if (System.getSecurityManager() == null) {
            Policy.setPolicy(new SimplePolicy(allowControl, allowAll));
            System.setSecurityManager(new SecurityManager());
        }
    }

    /**
     * The CustomLoggerWrapper makes it possible to verify that classes
     * which implements System.Logger will be skipped when looking for
     * the calling method.
     */
    static class CustomLoggerWrapper implements Logger {

        Logger impl;
        public CustomLoggerWrapper(Logger logger) {
            this.impl = Objects.requireNonNull(logger);
        }


        @Override
        public String getName() {
            return impl.getName();
        }

        @Override
        public boolean isLoggable(Level level) {
            return impl.isLoggable(level);
        }

        @Override
        public void log(Level level, ResourceBundle rb, String string, Throwable thrwbl) {
            impl.log(level, rb, string, thrwbl);
        }

        @Override
        public void log(Level level, ResourceBundle rb, String string, Object... os) {
            impl.log(level, rb, string, os);
        }

        @Override
        public void log(Level level, Object o) {
            impl.log(level, o);
        }

        @Override
        public void log(Level level, String string) {
            impl.log(level, string);
        }

        @Override
        public void log(Level level, Supplier<String> splr) {
            impl.log(level, splr);
        }

        @Override
        public void log(Level level, String string, Object... os) {
           impl.log(level, string, os);
        }

        @Override
        public void log(Level level, String string, Throwable thrwbl) {
            impl.log(level, string, thrwbl);
        }

        @Override
        public void log(Level level, Supplier<String> splr, Throwable thrwbl) {
            Logger.super.log(level, splr, thrwbl);
        }

        @Override
        public String toString() {
            return super.toString() + "(impl=" + impl + ")";
        }

    }

    /**
     * The ReflectionLoggerWrapper additionally makes it possible to verify
     * that code which use reflection to call System.Logger will be skipped
     * when looking for the calling method.
     */
    static class ReflectionLoggerWrapper implements Logger {

        Logger impl;
        public ReflectionLoggerWrapper(Logger logger) {
            this.impl = Objects.requireNonNull(logger);
        }

        private Object invoke(Method m, Object... params) {
            try {
                return m.invoke(impl, params);
            } catch (IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public String getName() {
            return impl.getName();
        }

        @Override
        public boolean isLoggable(Level level) {
            return impl.isLoggable(level);
        }

        @Override
        public void log(Level level, ResourceBundle rb, String string, Throwable thrwbl) {
            try {
                invoke(System.Logger.class.getMethod(
                        "log", Level.class, ResourceBundle.class, String.class, Throwable.class),
                        level, rb, string, thrwbl);
            } catch (NoSuchMethodException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public void log(Level level, ResourceBundle rb, String string, Object... os) {
            try {
                invoke(System.Logger.class.getMethod(
                        "log", Level.class, ResourceBundle.class, String.class, Object[].class),
                        level, rb, string, os);
            } catch (NoSuchMethodException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public void log(Level level, String string) {
            try {
                invoke(System.Logger.class.getMethod(
                        "log", Level.class, String.class),
                        level, string);
            } catch (NoSuchMethodException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public void log(Level level, String string, Object... os) {
            try {
                invoke(System.Logger.class.getMethod(
                        "log", Level.class, String.class, Object[].class),
                        level, string, os);
            } catch (NoSuchMethodException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public void log(Level level, String string, Throwable thrwbl) {
            try {
                invoke(System.Logger.class.getMethod(
                        "log", Level.class, String.class, Throwable.class),
                        level, string, thrwbl);
            } catch (NoSuchMethodException ex) {
                throw new RuntimeException(ex);
            }
        }


        @Override
        public String toString() {
            return super.toString() + "(impl=" + impl + ")";
        }

    }

    public static void main(String[] args) {
        if (args.length == 0)
            args = new String[] {
                "NOSECURITY",
                "NOPERMISSIONS",
                "WITHPERMISSIONS",
                "WITHCUSTOMWRAPPERS",
                "WITHREFLECTION"
            };

        // 1. Obtain destination loggers directly from the LoggerFinder
        //   - LoggerFinder.getLogger("foo", type)


        Stream.of(args).map(TestCases::valueOf).forEach((testCase) -> {
            switch (testCase) {
                case NOSECURITY:
                    System.out.println("\n*** Without Security Manager\n");
                    test(true);
                    System.out.println("Tetscase count: " + sequencer.get());
                    break;
                case NOPERMISSIONS:
                    System.out.println("\n*** With Security Manager, without permissions\n");
                    setSecurityManager();
                    test(false);
                    System.out.println("Tetscase count: " + sequencer.get());
                    break;
                case WITHPERMISSIONS:
                    System.out.println("\n*** With Security Manager, with control permission\n");
                    setSecurityManager();
                    final boolean control = allowControl.get().get();
                    try {
                        allowControl.get().set(true);
                        test(true);
                    } finally {
                        allowControl.get().set(control);
                    }
                    break;
                case WITHCUSTOMWRAPPERS:
                    System.out.println("\n*** With Security Manager, with control permission, using custom Wrappers\n");
                    setSecurityManager();
                    final boolean previous = allowControl.get().get();
                    try {
                        allowControl.get().set(true);
                        test(CustomLoggerWrapper::new, true);
                    } finally {
                        allowControl.get().set(previous);
                    }
                    break;
                case WITHREFLECTION:
                    System.out.println("\n*** With Security Manager,"
                            + " with control permission,"
                            + " using reflection while logging\n");
                    setSecurityManager();
                    final boolean before = allowControl.get().get();
                    try {
                        allowControl.get().set(true);
                        test(ReflectionLoggerWrapper::new, true);
                    } finally {
                        allowControl.get().set(before);
                    }
                    break;

                default:
                    throw new RuntimeException("Unknown test case: " + testCase);
            }
        });
        System.out.println("\nPASSED: Tested " + sequencer.get() + " cases.");
    }

    public static void test(boolean hasRequiredPermissions) {
        test(Function.identity(), hasRequiredPermissions);
    }

    public static void test(Function<Logger, Logger> wrapper, boolean hasRequiredPermissions) {

        ResourceBundle loggerBundle = ResourceBundle.getBundle(MyLoggerBundle.class.getName());
        final Map<Logger, String> loggerDescMap = new HashMap<>();


        // 1. Test loggers returned by:
        //   - System.getLogger("foo")
        //   - and AccessSystemLogger.getLogger("foo")
        Logger sysLogger1 = null;
        try {
            sysLogger1 = wrapper.apply(accessSystemLogger.getLogger("foo"));
            loggerDescMap.put(sysLogger1, "AccessSystemLogger.getLogger(\"foo\")");
        } catch (AccessControlException acx) {
            if (hasRequiredPermissions) {
                throw new RuntimeException("Unexpected security exception: ", acx);
            }
            if (!acx.getPermission().equals(SimplePolicy.LOGGERFINDER_PERMISSION)) {
                throw new RuntimeException("Unexpected permission in exception: " + acx, acx);
            }
            throw new RuntimeException("unexpected exception: " + acx, acx);
        }

        Logger appLogger1 = wrapper.apply(System.getLogger("foo"));
        loggerDescMap.put(appLogger1, "System.getLogger(\"foo\");");

        if (appLogger1 == sysLogger1) {
            throw new RuntimeException("identical loggers");
        }

        // 2. Test loggers returned by:
        //   - System.getLogger(\"foo\", loggerBundle)
        //   - and AccessSystemLogger.getLogger(\"foo\", loggerBundle)
        Logger appLogger2 = wrapper.apply(
                System.getLogger("foo", loggerBundle));
        loggerDescMap.put(appLogger2, "System.getLogger(\"foo\", loggerBundle)");

        Logger sysLogger2 = null;
        try {
            sysLogger2 = wrapper.apply(accessSystemLogger.getLogger("foo", loggerBundle));
            loggerDescMap.put(sysLogger2, "AccessSystemLogger.getLogger(\"foo\", loggerBundle)");
        } catch (AccessControlException acx) {
            if (hasRequiredPermissions) {
                throw new RuntimeException("Unexpected security exception: ", acx);
            }
            if (!acx.getPermission().equals(SimplePolicy.LOGGERFINDER_PERMISSION)) {
                throw new RuntimeException("Unexpected permission in exception: " + acx, acx);
            }
            throw new RuntimeException("unexpected exception: " + acx, acx);
        }
        if (appLogger2 == sysLogger2) {
            throw new RuntimeException("identical loggers");
        }

        final java.util.logging.Logger sink;
        final java.util.logging.Logger appSink;
        final java.util.logging.Logger sysSink;
        final java.util.logging.Handler appHandler;
        final java.util.logging.Handler sysHandler;
        final  LoggerFinder provider;
        allowAll.get().set(true);
        try {
            appSink = java.util.logging.Logger.getLogger("foo");
            sysSink = accessSystemLogger.demandSystemLogger("foo");
            sink = java.util.logging.Logger.getLogger("foo");
            sink.addHandler(appHandler = sysHandler = new MyHandler());
            sink.setUseParentHandlers(false);
            provider = LoggerFinder.getLoggerFinder();
        } finally {
            allowAll.get().set(false);
        }
        try {
            testLogger(provider, loggerDescMap, "foo", null, sysLogger1, sysSink);
            testLogger(provider, loggerDescMap, "foo", null, appLogger1, appSink);
            testLogger(provider, loggerDescMap, "foo", loggerBundle, sysLogger2, sysSink);
            testLogger(provider, loggerDescMap, "foo", loggerBundle, appLogger2, appSink);
        } finally {
            allowAll.get().set(true);
            try {
                appSink.removeHandler(appHandler);
                sysSink.removeHandler(sysHandler);
                sysSink.setLevel(null);
                appSink.setLevel(null);
            } finally {
                allowAll.get().set(false);
            }
        }
    }

    public static class Foo {

    }

    static void verbose(String msg) {
       if (VERBOSE) {
           System.out.println(msg);
       }
    }

    // Calls the 8 methods defined on Logger and verify the
    // parameters received by the underlying BaseLoggerFinder.LoggerImpl
    // logger.
    private static void testLogger(LoggerFinder provider,
            Map<Logger, String> loggerDescMap,
            String name,
            ResourceBundle loggerBundle,
            Logger logger,
            java.util.logging.Logger sink) {

        System.out.println("Testing " + loggerDescMap.get(logger));

        Foo foo = new Foo();
        String fooMsg = foo.toString();
        for (Level loggerLevel : Level.values()) {
            setLevel(sink, mapToJul(loggerLevel));
            for (Level messageLevel : Level.values()) {
                String desc = "logger.log(messageLevel, foo): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;

                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            messageLevel.compareTo(loggerLevel) >= 0,
                            name, mapToJul(messageLevel), (ResourceBundle)null,
                            fooMsg, (Throwable)null, (Object[])null);
                logger.log(messageLevel, foo);
                if (loggerLevel == Level.OFF || messageLevel.compareTo(loggerLevel) < 0) {
                    if (eventQueue.poll() != null) {
                        throw new RuntimeException("unexpected event in queue for " + desc);
                    }
                } else {
                    LogEvent actual = eventQueue.poll();
                    if (!expected.equals(actual)) {
                        throw new RuntimeException("mismatch for " + desc
                                + "\n\texpected=" + expected
                                + "\n\t  actual=" + actual);
                    } else {
                        verbose("Got expected results for "
                                + desc + "\n\t" + expected);
                    }
                }
            }
        }

        String msg = "blah";
        for (Level loggerLevel : Level.values()) {
            setLevel(sink, mapToJul(loggerLevel));
            for (Level messageLevel : Level.values()) {
                String desc = "logger.log(messageLevel, \"blah\"): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            messageLevel.compareTo(loggerLevel) >= 0 && loggerLevel != Level.OFF,
                            name, mapToJul(messageLevel), loggerBundle,
                            msg, (Throwable)null, (Object[])null);
                logger.log(messageLevel, msg);
                if (loggerLevel == Level.OFF || messageLevel.compareTo(loggerLevel) < 0) {
                    if (eventQueue.poll() != null) {
                        throw new RuntimeException("unexpected event in queue for " + desc);
                    }
                } else {
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
            }
        }

        Supplier<String> fooSupplier = new Supplier<String>() {
            @Override
            public String get() {
                return this.toString();
            }
        };

        for (Level loggerLevel : Level.values()) {
            setLevel(sink, mapToJul(loggerLevel));
            for (Level messageLevel : Level.values()) {
                String desc = "logger.log(messageLevel, fooSupplier): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            messageLevel.compareTo(loggerLevel) >= 0,
                            name, mapToJul(messageLevel), (ResourceBundle)null,
                            fooSupplier.get(),
                            (Throwable)null, (Object[])null);
                logger.log(messageLevel, fooSupplier);
                if (loggerLevel == Level.OFF || messageLevel.compareTo(loggerLevel) < 0) {
                    if (eventQueue.poll() != null) {
                        throw new RuntimeException("unexpected event in queue for " + desc);
                    }
                } else {
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
            }
        }

        String format = "two params [{1} {2}]";
        Object arg1 = foo;
        Object arg2 = msg;
        for (Level loggerLevel : Level.values()) {
            setLevel(sink, mapToJul(loggerLevel));
            for (Level messageLevel : Level.values()) {
                String desc = "logger.log(messageLevel, format, params...): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            messageLevel.compareTo(loggerLevel) >= 0 && loggerLevel != Level.OFF,
                            name, mapToJul(messageLevel), loggerBundle,
                            format, (Throwable)null, new Object[] {arg1, arg2});
                logger.log(messageLevel, format, arg1, arg2);
                if (loggerLevel == Level.OFF || messageLevel.compareTo(loggerLevel) < 0) {
                    if (eventQueue.poll() != null) {
                        throw new RuntimeException("unexpected event in queue for " + desc);
                    }
                } else {
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
            }
        }

        Throwable thrown = new Exception("OK: log me!");
        for (Level loggerLevel : Level.values()) {
            setLevel(sink, mapToJul(loggerLevel));
            for (Level messageLevel : Level.values()) {
                String desc = "logger.log(messageLevel, \"blah\", thrown): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            messageLevel.compareTo(loggerLevel) >= 0 && loggerLevel != Level.OFF,
                            name, mapToJul(messageLevel), loggerBundle,
                            msg, thrown, (Object[]) null);
                logger.log(messageLevel, msg, thrown);
                if (loggerLevel == Level.OFF || messageLevel.compareTo(loggerLevel) < 0) {
                    if (eventQueue.poll() != null) {
                        throw new RuntimeException("unexpected event in queue for " + desc);
                    }
                } else {
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
            }
        }


        for (Level loggerLevel : Level.values()) {
            setLevel(sink, mapToJul(loggerLevel));
            for (Level messageLevel : Level.values()) {
                String desc = "logger.log(messageLevel, thrown, fooSupplier): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            messageLevel.compareTo(loggerLevel) >= 0,
                            name, mapToJul(messageLevel), (ResourceBundle)null,
                            fooSupplier.get(),
                            (Throwable)thrown, (Object[])null);
                logger.log(messageLevel, fooSupplier, thrown);
                if (loggerLevel == Level.OFF || messageLevel.compareTo(loggerLevel) < 0) {
                    if (eventQueue.poll() != null) {
                        throw new RuntimeException("unexpected event in queue for " + desc);
                    }
                } else {
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
            }
        }

        ResourceBundle bundle = ResourceBundle.getBundle(MyBundle.class.getName());
        for (Level loggerLevel : Level.values()) {
            setLevel(sink, mapToJul(loggerLevel));
            for (Level messageLevel : Level.values()) {
                String desc = "logger.log(messageLevel, bundle, format, params...): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            messageLevel.compareTo(loggerLevel) >= 0 && loggerLevel != Level.OFF,
                            name, mapToJul(messageLevel), bundle,
                            format, (Throwable)null, new Object[] {foo, msg});
                logger.log(messageLevel, bundle, format, foo, msg);
                if (loggerLevel == Level.OFF || messageLevel.compareTo(loggerLevel) < 0) {
                    if (eventQueue.poll() != null) {
                        throw new RuntimeException("unexpected event in queue for " + desc);
                    }
                } else {
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
            }
        }

        for (Level loggerLevel : Level.values()) {
            setLevel(sink, mapToJul(loggerLevel));
            for (Level messageLevel : Level.values()) {
                String desc = "logger.log(messageLevel, bundle, \"blah\", thrown): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            messageLevel.compareTo(loggerLevel) >= 0 && loggerLevel != Level.OFF,
                            name, mapToJul(messageLevel), bundle,
                            msg, thrown, (Object[]) null);
                logger.log(messageLevel, bundle, msg, thrown);
                if (loggerLevel == Level.OFF || messageLevel.compareTo(loggerLevel) < 0) {
                    if (eventQueue.poll() != null) {
                        throw new RuntimeException("unexpected event in queue for " + desc);
                    }
                } else {
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

        static final Policy DEFAULT_POLICY = Policy.getPolicy();

        static final RuntimePermission LOGGERFINDER_PERMISSION =
                new RuntimePermission("loggerFinder");
        final Permissions permissions;
        final Permissions allPermissions;
        final Permissions controlPermissions;
        final ThreadLocal<AtomicBoolean> allowControl;
        final ThreadLocal<AtomicBoolean> allowAll;
        public SimplePolicy(ThreadLocal<AtomicBoolean> allowControl, ThreadLocal<AtomicBoolean> allowAll) {
            this.allowControl = allowControl;
            this.allowAll = allowAll;
            permissions = new Permissions();

            // these are used for configuring the test itself...
            controlPermissions = new Permissions();
            controlPermissions.add(LOGGERFINDER_PERMISSION);
            allPermissions = new Permissions();
            allPermissions.add(new java.security.AllPermission());

        }

        @Override
        public boolean implies(ProtectionDomain domain, Permission permission) {
            if (allowAll.get().get()) return allPermissions.implies(permission);
            if (allowControl.get().get()) return controlPermissions.implies(permission);
            return permissions.implies(permission) || DEFAULT_POLICY.implies(domain, permission);
        }

        @Override
        public PermissionCollection getPermissions(CodeSource codesource) {
            return new PermissionsBuilder().addAll(allowAll.get().get()
                    ? allPermissions : allowControl.get().get()
                    ? controlPermissions : permissions).toPermissions();
        }

        @Override
        public PermissionCollection getPermissions(ProtectionDomain domain) {
            return new PermissionsBuilder().addAll(allowAll.get().get()
                    ? allPermissions : allowControl.get().get()
                    ? controlPermissions : permissions).toPermissions();
        }
    }
}
