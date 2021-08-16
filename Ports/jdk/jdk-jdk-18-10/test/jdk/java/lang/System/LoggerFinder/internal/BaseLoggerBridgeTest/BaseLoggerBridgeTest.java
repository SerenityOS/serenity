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
import java.security.AccessController;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.PrivilegedAction;
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
import sun.util.logging.PlatformLogger;
import java.lang.System.LoggerFinder;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.stream.Stream;

/**
 * @test
 * @bug     8140364
 * @summary JDK implementation specific unit test for JDK internal artifacts.
 *   Tests a naive implementation of System.Logger, and in particular
 *   the default mapping provided by PlatformLogger.Bridge.
 * @modules java.base/sun.util.logging java.base/jdk.internal.logger
 * @build CustomSystemClassLoader BaseLoggerFinder BaseLoggerBridgeTest
 * @run  main/othervm -Djava.system.class.loader=CustomSystemClassLoader BaseLoggerBridgeTest NOSECURITY
 * @run  main/othervm -Djava.security.manager=allow -Djava.system.class.loader=CustomSystemClassLoader BaseLoggerBridgeTest NOPERMISSIONS
 * @run  main/othervm -Djava.security.manager=allow -Djava.system.class.loader=CustomSystemClassLoader BaseLoggerBridgeTest WITHPERMISSIONS
 * @author danielfuchs
 */
public class BaseLoggerBridgeTest {

    static final RuntimePermission LOGGERFINDER_PERMISSION =
                new RuntimePermission("loggerFinder");
    final static AtomicLong sequencer = new AtomicLong();
    final static boolean VERBOSE = false;
    // whether the implementation of Logger try to do a best
    // effort for logp... Our base logger finder stub doesn't
    // support logp, and thus the logp() implementation comes from
    // LoggerWrapper - which does a best effort.
    static final boolean BEST_EFFORT_FOR_LOGP = true;
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

    static final Class<?> providerClass;
    static {
        try {
            providerClass = ClassLoader.getSystemClassLoader().loadClass("BaseLoggerFinder");
        } catch (ClassNotFoundException ex) {
            throw new ExceptionInInitializerError(ex);
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

    static final Level[] mappedLevels = {
        Level.ALL,     // ALL
        Level.TRACE,   // FINEST
        Level.TRACE,   // FINER
        Level.DEBUG,   // FINE
        Level.DEBUG,   // CONFIG
        Level.INFO,    // INFO
        Level.WARNING, // WARNING
        Level.ERROR,   // SEVERE
        Level.OFF,     // OFF
    };

    final static Map<sun.util.logging.PlatformLogger.Level, Level> julToSpiMap;
    static {
        Map<sun.util.logging.PlatformLogger.Level, Level> map = new HashMap<>();
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
    public static class MyLoggerBundle extends MyBundle {

    }

    public static interface TestLoggerFinder {
        final ConcurrentHashMap<String, LoggerImpl> system = new ConcurrentHashMap<>();
        final ConcurrentHashMap<String, LoggerImpl> user = new ConcurrentHashMap<>();
        public Queue<LogEvent> eventQueue = new ArrayBlockingQueue<>(128);

        public static final class LogEvent implements Cloneable {

            public LogEvent() {
                this(sequencer.getAndIncrement());
            }

            LogEvent(long sequenceNumber) {
                this.sequenceNumber = sequenceNumber;
            }

            boolean callSupplier = false;
            long sequenceNumber;
            boolean isLoggable;
            String loggerName;
            Level level;
            ResourceBundle bundle;
            Throwable thrown;
            Object[] args;
            Supplier<String> supplier;
            String msg;

            Object[] toArray(boolean callSupplier) {
                return new Object[] {
                    sequenceNumber,
                    isLoggable,
                    loggerName,
                    level,
                    bundle,
                    thrown,
                    args,
                    callSupplier && supplier != null ? supplier.get() : supplier,
                    msg,
                };
            }

            boolean callSupplier(Object obj) {
                return callSupplier || ((LogEvent)obj).callSupplier;
            }

            @Override
            public String toString() {
                return Arrays.deepToString(toArray(false));
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof LogEvent
                        && Objects.deepEquals(toArray(callSupplier(obj)), ((LogEvent)obj).toArray(callSupplier(obj)));
            }

            @Override
            public int hashCode() {
                return Objects.hash(toArray(true));
            }

            public LogEvent cloneWith(long sequenceNumber)
                    throws CloneNotSupportedException {
                LogEvent cloned = (LogEvent)super.clone();
                cloned.sequenceNumber = sequenceNumber;
                return cloned;
            }

            public static LogEvent of(boolean isLoggable, String name,
                    Level level, ResourceBundle bundle,
                    String key, Throwable thrown) {
                LogEvent evt = new LogEvent();
                evt.isLoggable = isLoggable;
                evt.loggerName = name;
                evt.level = level;
                evt.args = null;
                evt.bundle = bundle;
                evt.thrown = thrown;
                evt.supplier = null;
                evt.msg = key;
                return evt;
            }

            public static LogEvent of(boolean isLoggable, String name,
                    Level level, Throwable thrown, Supplier<String> supplier) {
                LogEvent evt = new LogEvent();
                evt.isLoggable = isLoggable;
                evt.loggerName = name;
                evt.level = level;
                evt.args = null;
                evt.bundle = null;
                evt.thrown = thrown;
                evt.supplier = supplier;
                evt.msg = null;
                return evt;
            }

            public static LogEvent of(boolean isLoggable, String name,
                    Level level, ResourceBundle bundle,
                    String key, Object... params) {
                LogEvent evt = new LogEvent();
                evt.isLoggable = isLoggable;
                evt.loggerName = name;
                evt.level = level;
                evt.args = params;
                evt.bundle = bundle;
                evt.thrown = null;
                evt.supplier = null;
                evt.msg = key;
                return evt;
            }

            public static LogEvent of(long sequenceNumber,
                    boolean isLoggable, String name,
                    Level level, ResourceBundle bundle,
                    String key, Supplier<String> supplier,
                    Throwable thrown, Object... params) {
                LogEvent evt = new LogEvent(sequenceNumber);
                evt.loggerName = name;
                evt.level = level;
                evt.args = params;
                evt.bundle = bundle;
                evt.thrown = thrown;
                evt.supplier = supplier;
                evt.msg = key;
                evt.isLoggable = isLoggable;
                return evt;
            }

            public static LogEvent ofp(boolean callSupplier, LogEvent evt) {
                evt.callSupplier = callSupplier;
                return evt;
            }
        }

        public class LoggerImpl implements Logger {
            private final String name;
            private Level level = Level.INFO;

            public LoggerImpl(String name) {
                this.name = name;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public boolean isLoggable(Level level) {
                return this.level != Level.OFF && this.level.getSeverity() <= level.getSeverity();
            }

            @Override
            public void log(Level level, ResourceBundle bundle, String key, Throwable thrown) {
                log(LogEvent.of(isLoggable(level), this.name, level, bundle, key, thrown));
            }

            @Override
            public void log(Level level, ResourceBundle bundle, String format, Object... params) {
                log(LogEvent.of(isLoggable(level), name, level, bundle, format, params));
            }

            void log(LogEvent event) {
                eventQueue.add(event);
            }

            @Override
            public void log(Level level, Supplier<String> msgSupplier) {
                log(LogEvent.of(isLoggable(level), name, level, null, msgSupplier));
            }

            @Override
            public void log(Level level, Supplier<String> msgSupplier, Throwable thrown) {
                log(LogEvent.of(isLoggable(level), name, level, thrown, msgSupplier));
            }

        }

        public Logger getLogger(String name, Module caller);
        public Logger getLocalizedLogger(String name, ResourceBundle bundle, Module caller);
    }

    static PlatformLogger.Bridge convert(Logger logger) {
        boolean old = allowAll.get().get();
        allowAccess.get().set(true);
        try {
            return PlatformLogger.Bridge.convert(logger);
        } finally {
            allowAccess.get().set(old);
        }
    }

    static Logger getLogger(String name, Module caller) {
        boolean old = allowAll.get().get();
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
            // Ugly test hack: preload the resources needed by String.format
            //   We need to do that before setting the security manager
            //   because our implementation of CustomSystemClassLoader
            //   doesn't have the required permission.
            System.out.println(String.format("debug: %s", "Setting security manager"));
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
            TestLoggerFinder provider;
            switch (testCase) {
                case NOSECURITY:
                    System.out.println("\n*** Without Security Manager\n");
                    provider = TestLoggerFinder.class.cast(LoggerFinder.getLoggerFinder());
                    test(provider, true);
                    System.out.println("Tetscase count: " + sequencer.get());
                    break;
                case NOPERMISSIONS:
                    System.out.println("\n*** With Security Manager, without permissions\n");
                    setSecurityManager();
                    try {
                        provider = TestLoggerFinder.class.cast(LoggerFinder.getLoggerFinder());
                        throw new RuntimeException("Expected exception not raised");
                    } catch (AccessControlException x) {
                        if (!LOGGERFINDER_PERMISSION.equals(x.getPermission())) {
                            throw new RuntimeException("Unexpected permission check", x);
                        }
                        final boolean control = allowControl.get().get();
                        try {
                            allowControl.get().set(true);
                            provider = TestLoggerFinder.class.cast(LoggerFinder.getLoggerFinder());
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
                        provider = TestLoggerFinder.class.cast(LoggerFinder.getLoggerFinder());
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

    public static void test(TestLoggerFinder provider, boolean hasRequiredPermissions) {

        ResourceBundle loggerBundle = ResourceBundle.getBundle(MyLoggerBundle.class.getName());
        final Map<Object, String> loggerDescMap = new HashMap<>();


        TestLoggerFinder.LoggerImpl appSink = null;
        try {
            appSink = TestLoggerFinder.LoggerImpl.class.cast(provider.getLogger("foo", BaseLoggerBridgeTest.class.getModule()));
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
            System.out.println("Got expected exception for logger: " + acx);
            boolean old = allowControl.get().get();
            allowControl.get().set(true);
            try {
                appSink = TestLoggerFinder.LoggerImpl.class.cast(provider.getLogger("foo", BaseLoggerBridgeTest.class.getModule()));
            } finally {
                allowControl.get().set(old);
            }
        }


        TestLoggerFinder.LoggerImpl sysSink = null;
        try {
            sysSink = TestLoggerFinder.LoggerImpl.class.cast(provider.getLogger("foo", Thread.class.getModule()));
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
        if (hasRequiredPermissions && appSink == sysSink) {
            throw new RuntimeException("identical loggers");
        }

        if (provider.system.contains(appSink)) {
            throw new RuntimeException("app logger in system map");
        }
        if (!provider.user.contains(appSink)) {
            throw new RuntimeException("app logger not in appplication map");
        }
        if (hasRequiredPermissions && provider.user.contains(sysSink)) {
            throw new RuntimeException("sys logger in appplication map");
        }
        if (hasRequiredPermissions && !provider.system.contains(sysSink)) {
            throw new RuntimeException("sys logger not in system map");
        }

        Logger appLogger1 = System.getLogger("foo");
        loggerDescMap.put(appLogger1, "System.getLogger(\"foo\")");
        PlatformLogger.Bridge bridge = convert(appLogger1);
        loggerDescMap.putIfAbsent(bridge, "PlatformLogger.Bridge.convert(System.getLogger(\"foo\"))");
        testLogger(provider, loggerDescMap, "foo", null, bridge, appSink);

        Logger sysLogger1 = null;
        try {
            sysLogger1 = getLogger("foo", Thread.class.getModule());
            loggerDescMap.put(sysLogger1,
                    "jdk.internal.logger.LazyLoggers.getLogger(\"foo\", Thread.class.getModule())");

            if (!hasRequiredPermissions) {
                // check that the provider would have thrown an exception
                provider.getLogger("foo", Thread.class.getModule());
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

        if (hasRequiredPermissions) {
            // if we don't have permissions sysSink will be null.
            testLogger(provider, loggerDescMap, "foo", null,
                PlatformLogger.Bridge.convert(sysLogger1), sysSink);
        }

        Logger appLogger2 =
                System.getLogger("foo", loggerBundle);
        loggerDescMap.put(appLogger2, "System.getLogger(\"foo\", loggerBundle)");

        if (appLogger2 == appLogger1) {
            throw new RuntimeException("identical loggers");
        }

        if (provider.system.contains(appLogger2)) {
            throw new RuntimeException("localized app logger in system map");
        }
        if (provider.user.contains(appLogger2)) {
            throw new RuntimeException("localized app logger  in appplication map");
        }

        testLogger(provider, loggerDescMap, "foo", loggerBundle,
                PlatformLogger.Bridge.convert(appLogger2), appSink);

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
        }
        if (hasRequiredPermissions && appLogger2 == sysLogger2) {
            throw new RuntimeException("identical loggers");
        }
        if (hasRequiredPermissions && sysLogger2 == sysLogger1) {
            throw new RuntimeException("identical loggers");
        }
        if (hasRequiredPermissions && provider.user.contains(sysLogger2)) {
            throw new RuntimeException("localized sys logger in appplication map");
        }
        if (hasRequiredPermissions && provider.system.contains(sysLogger2)) {
            throw new RuntimeException("localized sys logger not in system map");
        }

        if (hasRequiredPermissions) {
            // if we don't have permissions sysSink will be null.
            testLogger(provider, loggerDescMap, "foo", loggerBundle,
                PlatformLogger.Bridge.convert(sysLogger2), sysSink);
        }

    }

    public static class Foo {

    }

    static void verbose(String msg) {
       if (VERBOSE) {
           System.out.println(msg);
       }
    }

    static void checkLogEvent(TestLoggerFinder provider, String desc,
            TestLoggerFinder.LogEvent expected) {
        TestLoggerFinder.LogEvent actual =  provider.eventQueue.poll();
        if (!Objects.equals(expected, actual)) {
            throw new RuntimeException("mismatch for " + desc
                    + "\n\texpected=" + expected
                    + "\n\t  actual=" + actual);
        } else {
            verbose("Got expected results for "
                    + desc + "\n\t" + expected);
        }
    }

    static void checkLogEvent(TestLoggerFinder provider, String desc,
            TestLoggerFinder.LogEvent expected, boolean expectNotNull) {
        TestLoggerFinder.LogEvent actual =  provider.eventQueue.poll();
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

        static Supplier<String> logpMessage(ResourceBundle bundle,
                String className, String methodName, Supplier<String> msg) {
            if (BEST_EFFORT_FOR_LOGP && bundle == null
                    && (className != null || methodName != null)) {
                final String cName = className == null ? "" :  className;
                final String mName = methodName == null ? "" : methodName;
                return () -> String.format("[%s %s] %s", cName, mName, msg.get());
            } else {
                return msg;
            }
        }

        static String logpMessage(ResourceBundle bundle,
                String className, String methodName, String msg) {
            if (BEST_EFFORT_FOR_LOGP && bundle == null
                    && (className != null || methodName != null)) {
                final String cName = className == null ? "" :  className;
                final String mName = methodName == null ? "" : methodName;
                return String.format("[%s %s] %s", cName, mName, msg == null ? "" : msg);
            } else {
                return msg;
            }
        }

    // Calls the methods defined on LogProducer and verify the
    // parameters received by the underlying TestLoggerFinder.LoggerImpl
    // logger.
    private static void testLogger(TestLoggerFinder provider,
            Map<Object, String> loggerDescMap,
            String name,
            ResourceBundle loggerBundle,
            PlatformLogger.Bridge logger,
            TestLoggerFinder.LoggerImpl sink) {

        if (loggerDescMap.get(logger) == null) {
            throw new RuntimeException("Test bug: Missing description");
        }
        System.out.println("Testing " + loggerDescMap.get(logger) +" [" + logger + "]");

        Foo foo = new Foo();
        String fooMsg = foo.toString();
        System.out.println("\tlogger.log(messageLevel, fooMsg)");
        System.out.println("\tlogger.<level>(fooMsg)");
        for (Level loggerLevel : Level.values()) {
            sink.level = loggerLevel;
            for (sun.util.logging.PlatformLogger.Level messageLevel :julLevels) {
                String desc = "logger.log(messageLevel, fooMsg): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                Level expectedMessageLevel = julToSpiMap.get(messageLevel);
                TestLoggerFinder.LogEvent expected =
                        TestLoggerFinder.LogEvent.of(
                            sequencer.get(),
                            loggerLevel != Level.OFF && expectedMessageLevel.compareTo(loggerLevel) >= 0,
                            name, expectedMessageLevel, loggerBundle,
                            fooMsg, null, (Throwable)null, (Object[])null);
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
        for (Level loggerLevel : Level.values()) {
            sink.level = loggerLevel;
            for (sun.util.logging.PlatformLogger.Level messageLevel :julLevels) {
                String desc = "logger.log(messageLevel, supplier): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                Level expectedMessageLevel = julToSpiMap.get(messageLevel);
                TestLoggerFinder.LogEvent expected =
                        TestLoggerFinder.LogEvent.of(
                            sequencer.get(),
                            loggerLevel != Level.OFF && expectedMessageLevel.compareTo(loggerLevel) >= 0,
                            name, expectedMessageLevel, (ResourceBundle) null,
                            null, supplier, (Throwable)null, (Object[])null);
                logger.log(messageLevel, supplier);
                checkLogEvent(provider, desc, expected);
            }
        }

        String format = "two params [{1} {2}]";
        Object arg1 = foo;
        Object arg2 = fooMsg;
        System.out.println("\tlogger.log(messageLevel, format, arg1, arg2)");
        for (Level loggerLevel : Level.values()) {
            sink.level = loggerLevel;
            for (sun.util.logging.PlatformLogger.Level messageLevel :julLevels) {
                String desc = "logger.log(messageLevel, format, foo, fooMsg): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                Level expectedMessageLevel = julToSpiMap.get(messageLevel);
                TestLoggerFinder.LogEvent expected =
                        TestLoggerFinder.LogEvent.of(
                            sequencer.get(),
                            loggerLevel != Level.OFF && expectedMessageLevel.compareTo(loggerLevel) >= 0,
                            name, expectedMessageLevel, loggerBundle,
                            format, null, (Throwable)null, arg1, arg2);
                logger.log(messageLevel, format, arg1, arg2);
                checkLogEvent(provider, desc, expected);
            }
        }

        Throwable thrown = new Exception("OK: log me!");
        System.out.println("\tlogger.log(messageLevel, fooMsg, thrown)");
        for (Level loggerLevel : Level.values()) {
            sink.level = loggerLevel;
            for (sun.util.logging.PlatformLogger.Level messageLevel :julLevels) {
                String desc = "logger.log(messageLevel, fooMsg, thrown): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                Level expectedMessageLevel = julToSpiMap.get(messageLevel);
                TestLoggerFinder.LogEvent expected =
                        TestLoggerFinder.LogEvent.of(
                            sequencer.get(),
                            loggerLevel != Level.OFF && expectedMessageLevel.compareTo(loggerLevel) >= 0,
                            name, expectedMessageLevel, loggerBundle,
                            fooMsg, null, thrown, (Object[])null);
                logger.log(messageLevel, fooMsg, thrown);
                checkLogEvent(provider, desc, expected);
            }
        }

        System.out.println("\tlogger.log(messageLevel, thrown, supplier)");
        for (Level loggerLevel : Level.values()) {
            sink.level = loggerLevel;
            for (sun.util.logging.PlatformLogger.Level messageLevel :julLevels) {
                String desc = "logger.log(messageLevel, thrown, supplier): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                Level expectedMessageLevel = julToSpiMap.get(messageLevel);
                TestLoggerFinder.LogEvent expected =
                        TestLoggerFinder.LogEvent.of(
                            sequencer.get(),
                            loggerLevel != Level.OFF && expectedMessageLevel.compareTo(loggerLevel) >= 0,
                            name, expectedMessageLevel, (ResourceBundle)null,
                            null, supplier, thrown, (Object[])null);
                logger.log(messageLevel, thrown, supplier);
                checkLogEvent(provider, desc, expected);
            }
        }

        String sourceClass = "blah.Blah";
        String sourceMethod = "blih";
        System.out.println("\tlogger.logp(messageLevel, sourceClass, sourceMethod, fooMsg)");
        for (Level loggerLevel : Level.values()) {
            sink.level = loggerLevel;
            for (sun.util.logging.PlatformLogger.Level messageLevel :julLevels) {
                String desc = "logger.logp(messageLevel, sourceClass, sourceMethod, fooMsg): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                Level expectedMessageLevel = julToSpiMap.get(messageLevel);
                boolean isLoggable = loggerLevel != Level.OFF && expectedMessageLevel.compareTo(loggerLevel) >= 0;
                TestLoggerFinder.LogEvent expected =
                    isLoggable || loggerBundle != null && BEST_EFFORT_FOR_LOGP?
                        TestLoggerFinder.LogEvent.of(
                            sequencer.get(),
                            isLoggable,
                            name, expectedMessageLevel, loggerBundle,
                            logpMessage(loggerBundle, sourceClass, sourceMethod, fooMsg),
                            null, (Throwable)null, (Object[]) null) : null;
                logger.logp(messageLevel, sourceClass, sourceMethod, fooMsg);
                checkLogEvent(provider, desc, expected);
            }
        }

        System.out.println("\tlogger.logp(messageLevel, sourceClass, sourceMethod, supplier)");
        for (Level loggerLevel : Level.values()) {
            sink.level = loggerLevel;
            for (sun.util.logging.PlatformLogger.Level messageLevel :julLevels) {
                String desc = "logger.logp(messageLevel, sourceClass, sourceMethod, supplier): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                Level expectedMessageLevel = julToSpiMap.get(messageLevel);
                boolean isLoggable = loggerLevel != Level.OFF && expectedMessageLevel.compareTo(loggerLevel) >= 0;
                TestLoggerFinder.LogEvent expected = isLoggable ?
                    TestLoggerFinder.LogEvent.ofp(BEST_EFFORT_FOR_LOGP,
                        TestLoggerFinder.LogEvent.of(
                            sequencer.get(),
                            isLoggable,
                            name, expectedMessageLevel, null, null,
                            logpMessage(null, sourceClass, sourceMethod, supplier),
                            (Throwable)null, (Object[]) null)) : null;
                logger.logp(messageLevel, sourceClass, sourceMethod, supplier);
                checkLogEvent(provider, desc, expected);
            }
        }

        System.out.println("\tlogger.logp(messageLevel, sourceClass, sourceMethod, format, arg1, arg2)");
        for (Level loggerLevel : Level.values()) {
            sink.level = loggerLevel;
            for (sun.util.logging.PlatformLogger.Level messageLevel :julLevels) {
                String desc = "logger.logp(messageLevel, sourceClass, sourceMethod, format, arg1, arg2): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                Level expectedMessageLevel = julToSpiMap.get(messageLevel);
                boolean isLoggable = loggerLevel != Level.OFF && expectedMessageLevel.compareTo(loggerLevel) >= 0;
                TestLoggerFinder.LogEvent expected =
                    isLoggable || loggerBundle != null && BEST_EFFORT_FOR_LOGP?
                        TestLoggerFinder.LogEvent.of(
                            sequencer.get(),
                            loggerLevel != Level.OFF && expectedMessageLevel.compareTo(loggerLevel) >= 0,
                            name, expectedMessageLevel, loggerBundle,
                            logpMessage(loggerBundle, sourceClass, sourceMethod, format),
                            null, (Throwable)null, arg1, arg2) : null;
                logger.logp(messageLevel, sourceClass, sourceMethod, format, arg1, arg2);
                checkLogEvent(provider, desc, expected);
            }
        }

        System.out.println("\tlogger.logp(messageLevel, sourceClass, sourceMethod, fooMsg, thrown)");
        for (Level loggerLevel : Level.values()) {
            sink.level = loggerLevel;
            for (sun.util.logging.PlatformLogger.Level messageLevel :julLevels) {
                String desc = "logger.logp(messageLevel, sourceClass, sourceMethod, fooMsg, thrown): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                Level expectedMessageLevel = julToSpiMap.get(messageLevel);
                boolean isLoggable = loggerLevel != Level.OFF && expectedMessageLevel.compareTo(loggerLevel) >= 0;
                TestLoggerFinder.LogEvent expected =
                    isLoggable || loggerBundle != null && BEST_EFFORT_FOR_LOGP ?
                        TestLoggerFinder.LogEvent.of(
                            sequencer.get(),
                            loggerLevel != Level.OFF && expectedMessageLevel.compareTo(loggerLevel) >= 0,
                            name, expectedMessageLevel, loggerBundle,
                            logpMessage(loggerBundle, sourceClass, sourceMethod, fooMsg),
                            null, thrown, (Object[])null) : null;
                logger.logp(messageLevel, sourceClass, sourceMethod, fooMsg, thrown);
                checkLogEvent(provider, desc, expected);
            }
        }

        System.out.println("\tlogger.logp(messageLevel, sourceClass, sourceMethod, thrown, supplier)");
        for (Level loggerLevel : Level.values()) {
            sink.level = loggerLevel;
            for (sun.util.logging.PlatformLogger.Level messageLevel :julLevels) {
                String desc = "logger.logp(messageLevel, sourceClass, sourceMethod, thrown, supplier): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                Level expectedMessageLevel = julToSpiMap.get(messageLevel);
                boolean isLoggable = loggerLevel != Level.OFF && expectedMessageLevel.compareTo(loggerLevel) >= 0;
                TestLoggerFinder.LogEvent expected = isLoggable ?
                    TestLoggerFinder.LogEvent.ofp(BEST_EFFORT_FOR_LOGP,
                        TestLoggerFinder.LogEvent.of(
                            sequencer.get(),
                            loggerLevel != Level.OFF && expectedMessageLevel.compareTo(loggerLevel) >= 0,
                            name, expectedMessageLevel, null, null,
                            logpMessage(null, sourceClass, sourceMethod, supplier),
                            thrown, (Object[])null)) : null;
                logger.logp(messageLevel, sourceClass, sourceMethod, thrown, supplier);
                checkLogEvent(provider, desc, expected);
            }
        }

        ResourceBundle bundle = ResourceBundle.getBundle(MyBundle.class.getName());
        System.out.println("\tlogger.logrb(messageLevel, bundle, format, arg1, arg2)");
        for (Level loggerLevel : Level.values()) {
            sink.level = loggerLevel;
            for (sun.util.logging.PlatformLogger.Level messageLevel :julLevels) {
                String desc = "logger.logrb(messageLevel, bundle, format, arg1, arg2): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                Level expectedMessageLevel = julToSpiMap.get(messageLevel);
                TestLoggerFinder.LogEvent expected =
                        TestLoggerFinder.LogEvent.of(
                            sequencer.get(),
                            loggerLevel != Level.OFF && expectedMessageLevel.compareTo(loggerLevel) >= 0,
                            name, expectedMessageLevel, bundle,
                            format, null, (Throwable)null, arg1, arg2);
                logger.logrb(messageLevel, bundle, format, arg1, arg2);
                checkLogEvent(provider, desc, expected);
            }
        }

        System.out.println("\tlogger.logrb(messageLevel, bundle, msg, thrown)");
        for (Level loggerLevel : Level.values()) {
            sink.level = loggerLevel;
            for (sun.util.logging.PlatformLogger.Level messageLevel :julLevels) {
                String desc = "logger.logrb(messageLevel, bundle, msg, thrown): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                Level expectedMessageLevel = julToSpiMap.get(messageLevel);
                TestLoggerFinder.LogEvent expected =
                        TestLoggerFinder.LogEvent.of(
                            sequencer.get(),
                            loggerLevel != Level.OFF && expectedMessageLevel.compareTo(loggerLevel) >= 0,
                            name, expectedMessageLevel, bundle,
                            fooMsg, null, thrown, (Object[])null);
                logger.logrb(messageLevel, bundle, fooMsg, thrown);
                checkLogEvent(provider, desc, expected);
            }
        }

        System.out.println("\tlogger.logrb(messageLevel, sourceClass, sourceMethod, bundle, format, arg1, arg2)");
        for (Level loggerLevel : Level.values()) {
            sink.level = loggerLevel;
            for (sun.util.logging.PlatformLogger.Level messageLevel :julLevels) {
                String desc = "logger.logrb(messageLevel, sourceClass, sourceMethod, bundle, format, arg1, arg2): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                Level expectedMessageLevel = julToSpiMap.get(messageLevel);
                TestLoggerFinder.LogEvent expected =
                        TestLoggerFinder.LogEvent.of(
                            sequencer.get(),
                            loggerLevel != Level.OFF && expectedMessageLevel.compareTo(loggerLevel) >= 0,
                            name, expectedMessageLevel, bundle,
                            format, null, (Throwable)null, arg1, arg2);
                logger.logrb(messageLevel, sourceClass, sourceMethod, bundle, format, arg1, arg2);
                checkLogEvent(provider, desc, expected);
            }
        }

        System.out.println("\tlogger.logrb(messageLevel, sourceClass, sourceMethod, bundle, msg, thrown)");
        for (Level loggerLevel : Level.values()) {
            sink.level = loggerLevel;
            for (sun.util.logging.PlatformLogger.Level messageLevel :julLevels) {
                String desc = "logger.logrb(messageLevel, sourceClass, sourceMethod, bundle, msg, thrown): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                Level expectedMessageLevel = julToSpiMap.get(messageLevel);
                TestLoggerFinder.LogEvent expected =
                        TestLoggerFinder.LogEvent.of(
                            sequencer.get(),
                            loggerLevel != Level.OFF && expectedMessageLevel.compareTo(loggerLevel) >= 0,
                            name, expectedMessageLevel, bundle,
                            fooMsg, null, thrown, (Object[])null);
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
