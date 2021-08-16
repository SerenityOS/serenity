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
import java.util.logging.LogManager;
import sun.util.logging.PlatformLogger;
import java.util.logging.LogRecord;
import java.lang.System.LoggerFinder;
import java.lang.System.Logger;
import java.util.stream.Stream;
import sun.util.logging.internal.LoggingProviderImpl;

/**
 * @test
 * @bug     8140364
 * @summary JDK implementation specific unit test for JDK internal artifacts.
 *          Tests all internal bridge methods with the default LoggerFinder
 *          JUL backend.
 * @modules java.base/sun.util.logging
 *          java.base/jdk.internal.logger
 *          java.logging/sun.util.logging.internal
 * @run  main/othervm DefaultLoggerBridgeTest NOSECURITY
 * @run  main/othervm -Djava.security.manager=allow DefaultLoggerBridgeTest NOPERMISSIONS
 * @run  main/othervm -Djava.security.manager=allow DefaultLoggerBridgeTest WITHPERMISSIONS
 * @author danielfuchs
 */
public class DefaultLoggerBridgeTest {

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

        public LogEvent cloneWith(long sequenceNumber)
                throws CloneNotSupportedException {
            LogEvent cloned = (LogEvent)super.clone();
            cloned.sequenceNumber = sequenceNumber;
            return cloned;
        }

        public static LogEvent of(long sequenceNumber,
                boolean isLoggable, String name,
                java.util.logging.Level level, ResourceBundle bundle,
                String key, Throwable thrown, Object... params) {
            return LogEvent.of(sequenceNumber, isLoggable, name,
                    DefaultLoggerBridgeTest.class.getName(),
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

    static final java.util.logging.Level[] julLevels = {
        java.util.logging.Level.ALL,
        java.util.logging.Level.FINEST,
        java.util.logging.Level.FINER,
        java.util.logging.Level.FINE,
        java.util.logging.Level.CONFIG,
        java.util.logging.Level.INFO,
        java.util.logging.Level.WARNING,
        java.util.logging.Level.SEVERE,
        java.util.logging.Level.OFF,
    };


    public static class MyBundle extends ResourceBundle {

        final ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();

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

    static PlatformLogger.Bridge convert(Logger logger) {
        boolean old = allowAccess.get().get();
        allowAccess.get().set(true);
        try {
            return PlatformLogger.Bridge.convert(logger);
        } finally {
            allowAccess.get().set(old);
        }
    }

    static Logger getLogger(String name, Module caller) {
        boolean old = allowAccess.get().get();
        allowAccess.get().set(true);
        try {
            return jdk.internal.logger.LazyLoggers.getLogger(name, caller);
        } finally {
            allowAccess.get().set(old);
        }
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
                "NOSECURITY",
                "NOPERMISSIONS",
                "WITHPERMISSIONS"
            };

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
                default:
                    throw new RuntimeException("Unknown test case: " + testCase);
            }
        });
        System.out.println("\nPASSED: Tested " + sequencer.get() + " cases.");
    }

    public static void test(boolean hasRequiredPermissions) {

        ResourceBundle loggerBundle =
                ResourceBundle.getBundle(MyLoggerBundle.class.getName());
        final Map<Object, String> loggerDescMap = new HashMap<>();

        Logger sysLogger1a = getLogger("foo", Thread.class.getModule());
        loggerDescMap.put(sysLogger1a, "jdk.internal.logger.LazyLoggers.getLogger(\"foo\", Thread.class.getModule())");

        Logger appLogger1 = System.getLogger("foo");
        loggerDescMap.put(appLogger1, "System.getLogger(\"foo\")");

        LoggerFinder provider;
        try {
            provider = LoggerFinder.getLoggerFinder();
            if (!hasRequiredPermissions) {
                throw new RuntimeException("Expected exception not raised");
            }
        } catch (AccessControlException x) {
            if (hasRequiredPermissions) {
                throw new RuntimeException("Unexpected permission check", x);
            }
            if (!SimplePolicy.LOGGERFINDER_PERMISSION.equals(x.getPermission())) {
                throw new RuntimeException("Unexpected permission in exception: " + x, x);
            }
            final boolean control = allowControl.get().get();
            try {
                allowControl.get().set(true);
                provider = LoggerFinder.getLoggerFinder();
            } finally {
                allowControl.get().set(control);
            }
        }

        Logger sysLogger1b = null;
        try {
            sysLogger1b = provider.getLogger("foo", Thread.class.getModule());
            if (sysLogger1b != sysLogger1a) {
                loggerDescMap.put(sysLogger1b, "provider.getLogger(\"foo\", Thread.class.getModule())");
            }
            if (!hasRequiredPermissions) {
                throw new RuntimeException("Managed to obtain a system logger without permission");
            }
        } catch (AccessControlException acx) {
            if (hasRequiredPermissions) {
                throw new RuntimeException("Unexpected security exception: ", acx);
            }
            if (!acx.getPermission().equals(SimplePolicy.LOGGERFINDER_PERMISSION)) {
                throw new RuntimeException("Unexpected permission in exception: " + acx, acx);
            }
            System.out.println("Got expected exception for system logger: " + acx);
        }

        Logger appLogger2 = System.getLogger("foo", loggerBundle);
        loggerDescMap.put(appLogger2, "System.getLogger(\"foo\", loggerBundle)");

        if (appLogger2 == appLogger1) {
            throw new RuntimeException("identical loggers");
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
            if (!acx.getPermission().equals(SimplePolicy.LOGGERFINDER_PERMISSION)) {
                throw new RuntimeException("Unexpected permission in exception: " + acx, acx);
            }
            System.out.println("Got expected exception for localized system logger: " + acx);
        }
        if (hasRequiredPermissions && appLogger2 == sysLogger2) {
            throw new RuntimeException("identical loggers");
        }
        if (hasRequiredPermissions && sysLogger2 == sysLogger1a) {
            throw new RuntimeException("identical loggers");
        }

        final java.util.logging.Logger sink;
        final java.util.logging.Logger appSink;
        final java.util.logging.Logger sysSink;
        final MyHandler appHandler;
        final MyHandler sysHandler;
        final boolean old = allowAll.get().get();
        allowAll.get().set(true);
        try {
            appSink = LoggingProviderImpl.getLogManagerAccess().demandLoggerFor(
                    LogManager.getLogManager(), "foo", DefaultLoggerBridgeTest.class.getModule());
            sysSink = LoggingProviderImpl.getLogManagerAccess().demandLoggerFor(
                    LogManager.getLogManager(), "foo", Thread.class.getModule());
            if (appSink == sysSink) {
                throw new RuntimeException("identical backend loggers");
            }
            sink = java.util.logging.Logger.getLogger("foo");
            if (appSink != sink) {
                throw new RuntimeException("expected same application logger");
            }

            sink.addHandler(appHandler = sysHandler = new MyHandler());
            sink.setUseParentHandlers(VERBOSE);
        } finally {
            allowAll.get().set(old);
        }

        try {
            testLogger(provider, loggerDescMap, "foo", null, convert(sysLogger1a), sysSink);
            testLogger(provider, loggerDescMap, "foo", null, convert(appLogger1), appSink);
            testLogger(provider, loggerDescMap, "foo", loggerBundle, convert(appLogger2), appSink);
            if (sysLogger1b != null && sysLogger1b != sysLogger1a) {
                testLogger(provider, loggerDescMap, "foo", null, convert(sysLogger1b), sysSink);
            }
            if (sysLogger2 != null) {
                testLogger(provider, loggerDescMap, "foo", loggerBundle, convert(sysLogger2), sysSink);
            }
        } finally {
            allowAll.get().set(true);
            try {
                appSink.removeHandler(appHandler);
                sysSink.removeHandler(sysHandler);
            } finally {
                allowAll.get().set(old);
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

    static void setLevel(java.util.logging.Logger sink, java.util.logging.Level loggerLevel) {
        boolean before = allowAll.get().get();
        try {
            allowAll.get().set(true);
            sink.setLevel(loggerLevel);
        } finally {
            allowAll.get().set(before);
        }
    }

    static sun.util.logging.PlatformLogger.Level toPlatformLevel(java.util.logging.Level level) {
        boolean old = allowAccess.get().get();
        allowAccess.get().set(true);
        try {
            return sun.util.logging.PlatformLogger.Level.valueOf(level.getName());
        } finally {
            allowAccess.get().set(old);
        }
    }

    // Calls the methods defined on LogProducer and verify the
    // parameters received by the underlying logger.
    private static void testLogger(LoggerFinder provider,
            Map<Object, String> loggerDescMap,
            String name,
            ResourceBundle loggerBundle,
            PlatformLogger.Bridge logger,
            java.util.logging.Logger sink) {

        if (loggerDescMap.get(logger) == null) {
            throw new RuntimeException("Missing description for " + logger);
        }
        System.out.println("Testing " + loggerDescMap.get(logger) + " [" + logger + "]");
        final java.util.logging.Level OFF = java.util.logging.Level.OFF;

        Foo foo = new Foo();
        String fooMsg = foo.toString();
        System.out.println("\tlogger.log(messageLevel, fooMsg)");
        System.out.println("\tlogger.<level>(fooMsg)");
        for (java.util.logging.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (java.util.logging.Level messageLevel :julLevels) {
                String desc = "logger.log(messageLevel, fooMsg): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            loggerLevel != OFF && messageLevel.intValue() >= loggerLevel.intValue(),
                            name, messageLevel, loggerBundle,
                            fooMsg, (Throwable)null, (Object[])null);
                logger.log(toPlatformLevel(messageLevel), fooMsg);
                checkLogEvent(provider, desc, expected, expected.isLoggable);
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
        for (java.util.logging.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (java.util.logging.Level messageLevel :julLevels) {
                String desc = "logger.log(messageLevel, supplier): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            loggerLevel != OFF && messageLevel.intValue() >= loggerLevel.intValue(),
                            name, messageLevel, null,
                            supplier.get(), (Throwable)null, (Object[])null);
                logger.log(toPlatformLevel(messageLevel), supplier);
                checkLogEvent(provider, desc, expected, expected.isLoggable);
            }
        }

        String format = "two params [{1} {2}]";
        Object arg1 = foo;
        Object arg2 = fooMsg;
        System.out.println("\tlogger.log(messageLevel, format, arg1, arg2)");
        for (java.util.logging.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (java.util.logging.Level messageLevel :julLevels) {
                String desc = "logger.log(messageLevel, format, foo, fooMsg): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            loggerLevel != OFF && messageLevel.intValue() >= loggerLevel.intValue(),
                            name, messageLevel, loggerBundle,
                            format, (Throwable)null, arg1, arg2);
                logger.log(toPlatformLevel(messageLevel), format, arg1, arg2);
                checkLogEvent(provider, desc, expected, expected.isLoggable);
            }
        }

        Throwable thrown = new Exception("OK: log me!");
        System.out.println("\tlogger.log(messageLevel, fooMsg, thrown)");
        for (java.util.logging.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (java.util.logging.Level messageLevel :julLevels) {
                String desc = "logger.log(messageLevel, fooMsg, thrown): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            loggerLevel != OFF && messageLevel.intValue() >= loggerLevel.intValue(),
                            name, messageLevel, loggerBundle,
                            fooMsg, thrown, (Object[])null);
                logger.log(toPlatformLevel(messageLevel), fooMsg, thrown);
                checkLogEvent(provider, desc, expected, expected.isLoggable);
            }
        }

        System.out.println("\tlogger.log(messageLevel, thrown, supplier)");
        for (java.util.logging.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (java.util.logging.Level messageLevel :julLevels) {
                String desc = "logger.log(messageLevel, thrown, supplier): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            loggerLevel != OFF && messageLevel.intValue() >= loggerLevel.intValue(),
                            name, messageLevel, null,
                            supplier.get(), thrown, (Object[])null);
                logger.log(toPlatformLevel(messageLevel), thrown, supplier);
                checkLogEvent(provider, desc, expected, expected.isLoggable);
            }
        }

        String sourceClass = "blah.Blah";
        String sourceMethod = "blih";
        System.out.println("\tlogger.logp(messageLevel, sourceClass, sourceMethod, fooMsg)");
        for (java.util.logging.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (java.util.logging.Level messageLevel :julLevels) {
                String desc = "logger.logp(messageLevel, sourceClass, sourceMethod, fooMsg): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            loggerLevel != OFF && messageLevel.intValue() >= loggerLevel.intValue(),
                            name, sourceClass, sourceMethod, messageLevel, loggerBundle,
                            fooMsg, (Throwable)null, (Object[])null);
                logger.logp(toPlatformLevel(messageLevel), sourceClass, sourceMethod, fooMsg);
                checkLogEvent(provider, desc, expected, expected.isLoggable);
            }
        }

        System.out.println("\tlogger.logp(messageLevel, sourceClass, sourceMethod, supplier)");
        for (java.util.logging.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (java.util.logging.Level messageLevel :julLevels) {
                String desc = "logger.logp(messageLevel, sourceClass, sourceMethod, supplier): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            loggerLevel != OFF && messageLevel.intValue() >= loggerLevel.intValue(),
                            name, sourceClass, sourceMethod, messageLevel, null,
                            supplier.get(), (Throwable)null, (Object[])null);
                logger.logp(toPlatformLevel(messageLevel), sourceClass, sourceMethod, supplier);
                checkLogEvent(provider, desc, expected, expected.isLoggable);
            }
        }

        System.out.println("\tlogger.logp(messageLevel, sourceClass, sourceMethod, format, arg1, arg2)");
        for (java.util.logging.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (java.util.logging.Level messageLevel :julLevels) {
                String desc = "logger.logp(messageLevel, sourceClass, sourceMethod, format, arg1, arg2): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            loggerLevel != OFF && messageLevel.intValue() >= loggerLevel.intValue(),
                            name, sourceClass, sourceMethod, messageLevel, loggerBundle,
                            format, (Throwable)null, arg1, arg2);
                logger.logp(toPlatformLevel(messageLevel), sourceClass, sourceMethod, format, arg1, arg2);
                checkLogEvent(provider, desc, expected, expected.isLoggable);
            }
        }

        System.out.println("\tlogger.logp(messageLevel, sourceClass, sourceMethod, fooMsg, thrown)");
        for (java.util.logging.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (java.util.logging.Level messageLevel :julLevels) {
                String desc = "logger.logp(messageLevel, sourceClass, sourceMethod, fooMsg, thrown): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            loggerLevel != OFF && messageLevel.intValue() >= loggerLevel.intValue(),
                            name, sourceClass, sourceMethod, messageLevel, loggerBundle,
                            fooMsg, thrown, (Object[])null);
                logger.logp(toPlatformLevel(messageLevel), sourceClass, sourceMethod, fooMsg, thrown);
                checkLogEvent(provider, desc, expected, expected.isLoggable);
            }
        }

        System.out.println("\tlogger.logp(messageLevel, sourceClass, sourceMethod, thrown, supplier)");
        for (java.util.logging.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (java.util.logging.Level messageLevel :julLevels) {
                String desc = "logger.logp(messageLevel, sourceClass, sourceMethod, thrown, supplier): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            loggerLevel != OFF && messageLevel.intValue() >= loggerLevel.intValue(),
                            name, sourceClass, sourceMethod, messageLevel, null,
                            supplier.get(), thrown, (Object[])null);
                logger.logp(toPlatformLevel(messageLevel), sourceClass, sourceMethod, thrown, supplier);
                checkLogEvent(provider, desc, expected, expected.isLoggable);
            }
        }

        ResourceBundle bundle = ResourceBundle.getBundle(MyBundle.class.getName());
        System.out.println("\tlogger.logrb(messageLevel, bundle, format, arg1, arg2)");
        for (java.util.logging.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (java.util.logging.Level messageLevel :julLevels) {
                String desc = "logger.logrb(messageLevel, bundle, format, arg1, arg2): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            loggerLevel != OFF && messageLevel.intValue() >= loggerLevel.intValue(),
                            name, messageLevel, bundle,
                            format, (Throwable)null, arg1, arg2);
                logger.logrb(toPlatformLevel(messageLevel), bundle, format, arg1, arg2);
                checkLogEvent(provider, desc, expected, expected.isLoggable);
            }
        }

        System.out.println("\tlogger.logrb(messageLevel, bundle, msg, thrown)");
        for (java.util.logging.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (java.util.logging.Level messageLevel :julLevels) {
                String desc = "logger.logrb(messageLevel, bundle, msg, thrown): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            loggerLevel != OFF && messageLevel.intValue() >= loggerLevel.intValue(),
                            name, messageLevel, bundle,
                            fooMsg, thrown, (Object[])null);
                logger.logrb(toPlatformLevel(messageLevel), bundle, fooMsg, thrown);
                checkLogEvent(provider, desc, expected, expected.isLoggable);
            }
        }

        System.out.println("\tlogger.logrb(messageLevel, sourceClass, sourceMethod, bundle, format, arg1, arg2)");
        for (java.util.logging.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (java.util.logging.Level messageLevel :julLevels) {
                String desc = "logger.logrb(messageLevel, sourceClass, sourceMethod, bundle, format, arg1, arg2): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            loggerLevel != OFF && messageLevel.intValue() >= loggerLevel.intValue(),
                            name, sourceClass, sourceMethod, messageLevel, bundle,
                            format, (Throwable)null, arg1, arg2);
                logger.logrb(toPlatformLevel(messageLevel), sourceClass, sourceMethod, bundle, format, arg1, arg2);
                checkLogEvent(provider, desc, expected, expected.isLoggable);
            }
        }

        System.out.println("\tlogger.logrb(messageLevel, sourceClass, sourceMethod, bundle, msg, thrown)");
        for (java.util.logging.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (java.util.logging.Level messageLevel :julLevels) {
                String desc = "logger.logrb(messageLevel, sourceClass, sourceMethod, bundle, msg, thrown): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            loggerLevel != OFF && messageLevel.intValue() >= loggerLevel.intValue(),
                            name, sourceClass, sourceMethod, messageLevel, bundle,
                            fooMsg, thrown, (Object[])null);
                logger.logrb(toPlatformLevel(messageLevel), sourceClass, sourceMethod, bundle, fooMsg, thrown);
                checkLogEvent(provider, desc, expected, expected.isLoggable);
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
        public static final RuntimePermission LOGGERFINDER_PERMISSION =
                new RuntimePermission("loggerFinder");
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
                    builder.add(LOGGERFINDER_PERMISSION);
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
