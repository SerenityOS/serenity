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
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.lang.System.LoggerFinder;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.stream.Stream;

/**
 * @test
 * @bug     8140364
 * @summary Tests the default implementation of System.Logger, when
 *          JUL is the default backend.
 * @modules java.logging
 * @build AccessSystemLogger DefaultLoggerFinderTest
 * @run  driver AccessSystemLogger
 * @run  main/othervm -Xbootclasspath/a:boot DefaultLoggerFinderTest NOSECURITY
 * @run  main/othervm -Xbootclasspath/a:boot -Djava.security.manager=allow DefaultLoggerFinderTest NOPERMISSIONS
 * @run  main/othervm -Xbootclasspath/a:boot -Djava.security.manager=allow DefaultLoggerFinderTest WITHPERMISSIONS
 * @author danielfuchs
 */
public class DefaultLoggerFinderTest {

    static final RuntimePermission LOGGERFINDER_PERMISSION =
                new RuntimePermission("loggerFinder");
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

    static final AccessSystemLogger accessSystemLogger = new AccessSystemLogger();

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
                    DefaultLoggerFinderTest.class.getName(),
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

    static final java.util.logging.Level[] julLevels = {
        java.util.logging.Level.ALL,
        new java.util.logging.Level("FINER_THAN_FINEST", java.util.logging.Level.FINEST.intValue() - 10) {},
        java.util.logging.Level.FINEST,
        new java.util.logging.Level("FINER_THAN_FINER", java.util.logging.Level.FINER.intValue() - 10) {},
        java.util.logging.Level.FINER,
        new java.util.logging.Level("FINER_THAN_FINE", java.util.logging.Level.FINE.intValue() - 10) {},
        java.util.logging.Level.FINE,
        new java.util.logging.Level("FINER_THAN_CONFIG", java.util.logging.Level.FINE.intValue() + 10) {},
        java.util.logging.Level.CONFIG,
        new java.util.logging.Level("FINER_THAN_INFO", java.util.logging.Level.INFO.intValue() - 10) {},
        java.util.logging.Level.INFO,
        new java.util.logging.Level("FINER_THAN_WARNING", java.util.logging.Level.INFO.intValue() + 10) {},
        java.util.logging.Level.WARNING,
        new java.util.logging.Level("FINER_THAN_SEVERE", java.util.logging.Level.SEVERE.intValue() - 10) {},
        java.util.logging.Level.SEVERE,
        new java.util.logging.Level("FATAL", java.util.logging.Level.SEVERE.intValue() + 10) {},
        java.util.logging.Level.OFF,
    };

    static final Level[] mappedLevels = {
        Level.ALL,     // ALL
        Level.DEBUG,   // FINER_THAN_FINEST
        Level.DEBUG,   // FINEST
        Level.DEBUG,   // FINER_THAN_FINER
        Level.TRACE,   // FINER
        Level.TRACE,   // FINER_THAN_FINE
        Level.DEBUG,   // FINE
        Level.DEBUG,   // FINER_THAN_CONFIG
        Level.DEBUG,   // CONFIG
        Level.DEBUG,   // FINER_THAN_INFO
        Level.INFO,    // INFO
        Level.INFO,    // FINER_THAN_WARNING
        Level.WARNING, // WARNING
        Level.WARNING, // FINER_THAN_SEVERE
        Level.ERROR,   // SEVERE
        Level.ERROR,   // FATAL
        Level.OFF,     // OFF
    };

    final static Map<java.util.logging.Level, Level> julToSpiMap;
    static {
        Map<java.util.logging.Level, Level> map = new HashMap<>();
        if (mappedLevels.length != julLevels.length) {
            throw new ExceptionInInitializerError("Array lengths differ"
                + "\n\tjulLevels=" + Arrays.deepToString(julLevels)
                + "\n\tmappedLevels=" + Arrays.deepToString(mappedLevels));
        }
        for (int i=0; i<julLevels.length; i++) {
            map.put(julLevels[i], mappedLevels[i]);
        }
        julToSpiMap = Collections.unmodifiableMap(map);
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

    public static class MyLoggerBundle extends MyBundle {

    }


    static enum TestCases {NOSECURITY, NOPERMISSIONS, WITHPERMISSIONS};

    static void setSecurityManager() {
        if (System.getSecurityManager() == null) {
            Policy.setPolicy(new SimplePolicy(allowAll, allowControl));
            System.setSecurityManager(new SecurityManager());
        }
    }

    public static void main(String[] args) {
        if (args.length == 0)
            args = new String[] {
                "NOSECURITY",
                "NOPERMISSIONS",
                "WITHPERMISSIONS"
            };

        final java.util.logging.Logger appSink = java.util.logging.Logger.getLogger("foo");
        final java.util.logging.Logger sysSink = accessSystemLogger.demandSystemLogger("foo");
        final java.util.logging.Logger sink = java.util.logging.Logger.getLogger("foo");
        sink.addHandler(new MyHandler());
        sink.setUseParentHandlers(VERBOSE);

        Stream.of(args).map(TestCases::valueOf).forEach((testCase) -> {
            LoggerFinder provider;
            switch (testCase) {
                case NOSECURITY:
                    System.out.println("\n*** Without Security Manager\n");
                    provider = LoggerFinder.getLoggerFinder();
                    test(provider, true, appSink, sysSink);
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
                    test(provider, false, appSink, sysSink);
                    System.out.println("Tetscase count: " + sequencer.get());
                    break;
                case WITHPERMISSIONS:
                    System.out.println("\n*** With Security Manager, with control permission\n");
                    setSecurityManager();
                    final boolean control = allowControl.get().get();
                    try {
                        allowControl.get().set(true);
                        provider = LoggerFinder.getLoggerFinder();
                        test(provider, true, appSink, sysSink);
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

    public static void test(LoggerFinder provider,
            boolean hasRequiredPermissions,
            java.util.logging.Logger appSink,
            java.util.logging.Logger sysSink) {

        ResourceBundle loggerBundle = ResourceBundle.getBundle(MyLoggerBundle.class.getName());
        final Map<Logger, String> loggerDescMap = new HashMap<>();


        Logger appLogger1 = null;
        try {
            appLogger1 = provider.getLogger("foo", DefaultLoggerFinderTest.class.getModule());
            loggerDescMap.put(appLogger1, "provider.getLogger(\"foo\", DefaultLoggerFinderTest.class.getModule())");
            if (!hasRequiredPermissions) {
                throw new RuntimeException("Managed to obtain a logger without permission");
            }
        } catch (AccessControlException acx) {
            if (hasRequiredPermissions) {
                throw new RuntimeException("Unexpected security exception: ", acx);
            }
            if (!acx.getPermission().equals(LOGGERFINDER_PERMISSION)) {
                throw new RuntimeException("Unexpected permission in exception: " + acx, acx);
            }
            System.out.println("Got expected exception for logger: " + acx);
            boolean old = allowControl.get().get();
            allowControl.get().set(true);
            try {
                appLogger1 =provider.getLogger("foo", DefaultLoggerFinderTest.class.getModule());
                loggerDescMap.put(appLogger1, "provider.getLogger(\"foo\", DefaultLoggerFinderTest.class.getModule())");
            } finally {
                allowControl.get().set(old);
            }
        }

        Logger sysLogger1 = null;
        try {
            sysLogger1 = provider.getLogger("foo", Thread.class.getModule());
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
            boolean old = allowControl.get().get();
            allowControl.get().set(true);
            try {
                sysLogger1 = provider.getLogger("foo", Thread.class.getModule());
                loggerDescMap.put(sysLogger1, "provider.getLogger(\"foo\", Thread.class.getModule())");
            } finally {
                allowControl.get().set(old);
            }
        }
        if (appLogger1 == sysLogger1) {
            throw new RuntimeException("identical loggers");
        }

        Logger appLogger2 = null;
        try {
            appLogger2 = provider.getLocalizedLogger("foo", loggerBundle, DefaultLoggerFinderTest.class.getModule());
            loggerDescMap.put(appLogger2, "provider.getLocalizedLogger(\"foo\", loggerBundle, DefaultLoggerFinderTest.class.getModule())");
            if (!hasRequiredPermissions) {
                throw new RuntimeException("Managed to obtain a logger without permission");
            }
        } catch (AccessControlException acx) {
            if (hasRequiredPermissions) {
                throw new RuntimeException("Unexpected security exception: ", acx);
            }
            if (!acx.getPermission().equals(LOGGERFINDER_PERMISSION)) {
                throw new RuntimeException("Unexpected permission in exception: " + acx, acx);
            }
            System.out.println("Got expected exception for logger: " + acx);
            boolean old = allowControl.get().get();
            allowControl.get().set(true);
            try {
                appLogger2 = provider.getLocalizedLogger("foo", loggerBundle, DefaultLoggerFinderTest.class.getModule());
                loggerDescMap.put(appLogger2, "provider.getLocalizedLogger(\"foo\", loggerBundle, DefaultLoggerFinderTest.class.getModule())");
            } finally {
                allowControl.get().set(old);
            }
        }

        Logger sysLogger2 = null;
        try {
            sysLogger2 = provider.getLocalizedLogger("foo", loggerBundle, Thread.class.getModule());
            loggerDescMap.put(sysLogger2, "provider.getLocalizedLogger(\"foo\", loggerBundle, Thread.class.getModule())");
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
            boolean old = allowControl.get().get();
            allowControl.get().set(true);
            try {
                sysLogger2 = provider.getLocalizedLogger("foo", loggerBundle, Thread.class.getModule());
                loggerDescMap.put(sysLogger2, "provider.getLocalizedLogger(\"foo\", loggerBundle, Thread.class.getModule())");
            } finally {
                allowControl.get().set(old);
            }
        }
        if (appLogger2 == sysLogger2) {
            throw new RuntimeException("identical loggers");
        }
        if (appLogger2 == appLogger1) {
            throw new RuntimeException("identical loggers");
        }
        if (sysLogger2 == sysLogger1) {
            throw new RuntimeException("identical loggers");
        }


        testLogger(provider, loggerDescMap, "foo", null, appLogger1, appSink);
        testLogger(provider, loggerDescMap, "foo", null, sysLogger1, sysSink);
        testLogger(provider, loggerDescMap, "foo", loggerBundle, appLogger2, appSink);
        testLogger(provider, loggerDescMap, "foo", loggerBundle, sysLogger2, sysSink);


        Logger appLogger3 = System.getLogger("foo");
        loggerDescMap.put(appLogger3, "System.getLogger(\"foo\")");

        testLogger(provider, loggerDescMap, "foo", null, appLogger3, appSink);

        Logger appLogger4 =
                System.getLogger("foo", loggerBundle);
        loggerDescMap.put(appLogger4, "System.getLogger(\"foo\", loggerBundle)");

        if (appLogger4 == appLogger1) {
            throw new RuntimeException("identical loggers");
        }

        testLogger(provider, loggerDescMap, "foo", loggerBundle, appLogger4, appSink);

        Logger sysLogger3 = accessSystemLogger.getLogger("foo");
        loggerDescMap.put(sysLogger3, "AccessSystemLogger.getLogger(\"foo\")");

        testLogger(provider, loggerDescMap, "foo", null, sysLogger3, sysSink);

        Logger sysLogger4 =
                accessSystemLogger.getLogger("foo", loggerBundle);
        loggerDescMap.put(appLogger4, "AccessSystemLogger.getLogger(\"foo\", loggerBundle)");

        if (sysLogger4 == sysLogger1) {
            throw new RuntimeException("identical loggers");
        }

        testLogger(provider, loggerDescMap, "foo", loggerBundle, sysLogger4, sysSink);

    }

    public static class Foo {

    }

    static void verbose(String msg) {
       if (VERBOSE) {
           System.out.println(msg);
       }
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


    // Calls the 8 methods defined on Logger and verify the
    // parameters received by the underlying Logger Impl
    // logger.
    private static void testLogger(LoggerFinder provider,
            Map<Logger, String> loggerDescMap,
            String name,
            ResourceBundle loggerBundle,
            Logger logger,
            java.util.logging.Logger sink) {

        System.out.println("Testing " + loggerDescMap.get(logger) + " [" + logger + "]");
        final java.util.logging.Level OFF = java.util.logging.Level.OFF;

        Foo foo = new Foo();
        String fooMsg = foo.toString();
        for (java.util.logging.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (Level messageLevel : Level.values()) {
                java.util.logging.Level julLevel = mapToJul(messageLevel);
                String desc = "logger.log(messageLevel, foo): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            julLevel.intValue() >= loggerLevel.intValue(),
                            name, julLevel, (ResourceBundle)null,
                            fooMsg, (Throwable)null, (Object[])null);
                logger.log(messageLevel, foo);
                if (loggerLevel == OFF || julLevel.intValue() < loggerLevel.intValue()) {
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

        String msg = "blah";
        for (java.util.logging.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (Level messageLevel : Level.values()) {
                java.util.logging.Level julLevel = mapToJul(messageLevel);
                String desc = "logger.log(messageLevel, \"blah\"): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            julLevel.intValue() >= loggerLevel.intValue(),
                            name, julLevel, loggerBundle,
                            msg, (Throwable)null, (Object[])null);
                logger.log(messageLevel, msg);
                if (loggerLevel == OFF || julLevel.intValue() < loggerLevel.intValue()) {
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

        for (java.util.logging.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (Level messageLevel : Level.values()) {
                java.util.logging.Level julLevel = mapToJul(messageLevel);
                String desc = "logger.log(messageLevel, fooSupplier): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            julLevel.intValue() >= loggerLevel.intValue(),
                            name, julLevel, (ResourceBundle)null,
                            fooSupplier.get(),
                            (Throwable)null, (Object[])null);
                logger.log(messageLevel, fooSupplier);
                if (loggerLevel == OFF || julLevel.intValue() < loggerLevel.intValue()) {
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
        for (java.util.logging.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (Level messageLevel : Level.values()) {
                java.util.logging.Level julLevel = mapToJul(messageLevel);
                String desc = "logger.log(messageLevel, format, params...): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            julLevel.intValue() >= loggerLevel.intValue(),
                            name, julLevel, loggerBundle,
                            format, (Throwable)null, new Object[] {arg1, arg2});
                logger.log(messageLevel, format, arg1, arg2);
                if (loggerLevel == OFF || julLevel.intValue() < loggerLevel.intValue()) {
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
        for (java.util.logging.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (Level messageLevel : Level.values()) {
                java.util.logging.Level julLevel = mapToJul(messageLevel);
                String desc = "logger.log(messageLevel, \"blah\", thrown): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            julLevel.intValue() >= loggerLevel.intValue(),
                            name, julLevel, loggerBundle,
                            msg, thrown, (Object[]) null);
                logger.log(messageLevel, msg, thrown);
                if (loggerLevel == OFF || julLevel.intValue() < loggerLevel.intValue()) {
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


        for (java.util.logging.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (Level messageLevel : Level.values()) {
                java.util.logging.Level julLevel = mapToJul(messageLevel);
                String desc = "logger.log(messageLevel, thrown, fooSupplier): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            julLevel.intValue() >= loggerLevel.intValue(),
                            name, julLevel, (ResourceBundle)null,
                            fooSupplier.get(),
                            (Throwable)thrown, (Object[])null);
                logger.log(messageLevel, fooSupplier, thrown);
                if (loggerLevel == OFF || julLevel.intValue() < loggerLevel.intValue()) {
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
        for (java.util.logging.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (Level messageLevel : Level.values()) {
                java.util.logging.Level julLevel = mapToJul(messageLevel);
                String desc = "logger.log(messageLevel, bundle, format, params...): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            julLevel.intValue() >= loggerLevel.intValue(),
                            name, julLevel, bundle,
                            format, (Throwable)null, new Object[] {foo, msg});
                logger.log(messageLevel, bundle, format, foo, msg);
                if (loggerLevel == OFF || julLevel.intValue() < loggerLevel.intValue()) {
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

        for (java.util.logging.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (Level messageLevel : Level.values()) {
                java.util.logging.Level julLevel = mapToJul(messageLevel);
                String desc = "logger.log(messageLevel, bundle, \"blah\", thrown): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            julLevel.intValue() >= loggerLevel.intValue(),
                            name, julLevel, bundle,
                            msg, thrown, (Object[]) null);
                logger.log(messageLevel, bundle, msg, thrown);
                if (loggerLevel == OFF || julLevel.intValue() < loggerLevel.intValue()) {
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

        final Permissions permissions;
        final Permissions withControlPermissions;
        final Permissions allPermissions;
        final ThreadLocal<AtomicBoolean> allowAll;
        final ThreadLocal<AtomicBoolean> allowControl;
        public SimplePolicy(ThreadLocal<AtomicBoolean> allowAll,
                ThreadLocal<AtomicBoolean> allowControl) {
            this.allowAll = allowAll;
            this.allowControl = allowControl;
            permissions = new Permissions();

            withControlPermissions = new Permissions();
            withControlPermissions.add(LOGGERFINDER_PERMISSION);

            // these are used for configuring the test itself...
            allPermissions = new Permissions();
            allPermissions.add(new java.security.AllPermission());
        }

        @Override
        public boolean implies(ProtectionDomain domain, Permission permission) {
            if (allowAll.get().get()) return allPermissions.implies(permission);
            if (allowControl.get().get()) return withControlPermissions.implies(permission);
            return permissions.implies(permission) || DEFAULT_POLICY.implies(domain, permission);
        }

        @Override
        public PermissionCollection getPermissions(CodeSource codesource) {
            return new PermissionsBuilder().addAll(
                    allowAll.get().get() ? allPermissions :
                    allowControl.get().get()
                    ? withControlPermissions : permissions).toPermissions();
        }

        @Override
        public PermissionCollection getPermissions(ProtectionDomain domain) {
            return new PermissionsBuilder().addAll(
                    allowAll.get().get() ? allPermissions :
                    allowControl.get().get()
                    ? withControlPermissions : permissions).toPermissions();
        }
    }
}
