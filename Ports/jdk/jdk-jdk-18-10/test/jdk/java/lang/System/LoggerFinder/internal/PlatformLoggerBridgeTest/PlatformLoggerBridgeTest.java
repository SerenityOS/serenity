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
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.lang.System.LoggerFinder;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.stream.Stream;
import sun.util.logging.PlatformLogger;

/**
 * @test
 * @bug     8140364
 * @summary JDK implementation specific unit test for JDK internal artifacts.
 *          Tests all bridge methods from PlatformLogger with the a custom
 *          backend whose loggers implement PlatformLogger.Bridge.
 * @modules java.base/sun.util.logging
 *          java.logging
 * @build CustomSystemClassLoader LogProducerFinder PlatformLoggerBridgeTest
 * @run  main/othervm -Djava.system.class.loader=CustomSystemClassLoader PlatformLoggerBridgeTest NOSECURITY
 * @run  main/othervm -Djava.security.manager=allow -Djava.system.class.loader=CustomSystemClassLoader PlatformLoggerBridgeTest NOPERMISSIONS
 * @run  main/othervm -Djava.security.manager=allow -Djava.system.class.loader=CustomSystemClassLoader PlatformLoggerBridgeTest WITHPERMISSIONS
 * @author danielfuchs
 */
public class PlatformLoggerBridgeTest {

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

    public static class LoggerImpl implements System.Logger, PlatformLogger.Bridge {
        private final String name;
        private sun.util.logging.PlatformLogger.Level level = sun.util.logging.PlatformLogger.Level.INFO;
        private sun.util.logging.PlatformLogger.Level OFF = sun.util.logging.PlatformLogger.Level.OFF;
        private sun.util.logging.PlatformLogger.Level FINE = sun.util.logging.PlatformLogger.Level.FINE;
        private sun.util.logging.PlatformLogger.Level FINER = sun.util.logging.PlatformLogger.Level.FINER;
        private sun.util.logging.PlatformLogger.Level FINEST = sun.util.logging.PlatformLogger.Level.FINEST;
        private sun.util.logging.PlatformLogger.Level CONFIG = sun.util.logging.PlatformLogger.Level.CONFIG;
        private sun.util.logging.PlatformLogger.Level INFO = sun.util.logging.PlatformLogger.Level.INFO;
        private sun.util.logging.PlatformLogger.Level WARNING = sun.util.logging.PlatformLogger.Level.WARNING;
        private sun.util.logging.PlatformLogger.Level SEVERE = sun.util.logging.PlatformLogger.Level.SEVERE;

        public LoggerImpl(String name) {
            this.name = name;
        }

        public void configureLevel(sun.util.logging.PlatformLogger.Level level) {
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

        void log(PlatformLoggerBridgeTest.LogEvent event) {
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
        public void log(sun.util.logging.PlatformLogger.Level level, String msg) {
            log(LogEvent.of(isLoggable(level), name, null, null,
                    level, null, msg, null, (Object[]) null));
        }

        @Override
        public void log(sun.util.logging.PlatformLogger.Level level,
                        Supplier<String> msgSupplier) {
            log(LogEvent.of(isLoggable(level), name, null, null,
                    level, null, msgSupplier, null, (Object[]) null));
        }

        @Override
        public void log(sun.util.logging.PlatformLogger.Level level, String msg,
                        Object... params) {
            log(LogEvent.of(isLoggable(level), name, null, null,
                    level, null, msg, null, params));
        }

        @Override
        public void log(sun.util.logging.PlatformLogger.Level level, String msg,
                        Throwable thrown) {
            log(LogEvent.of(isLoggable(level), name, null, null,
                    level, null, msg, thrown, (Object[]) null));
        }

        @Override
        public void log(sun.util.logging.PlatformLogger.Level level, Throwable thrown,
                        Supplier<String> msgSupplier) {
            log(LogEvent.of(isLoggable(level), name, null, null,
                    level, null, msgSupplier, thrown, (Object[]) null));
        }

        @Override
        public void logp(sun.util.logging.PlatformLogger.Level level, String sourceClass,
                         String sourceMethod, String msg) {
            log(LogEvent.of(isLoggable(level), name,
                    sourceClass, sourceMethod,
                    level, null, msg, null, (Object[]) null));
        }

        @Override
        public void logp(sun.util.logging.PlatformLogger.Level level, String sourceClass,
                         String sourceMethod, Supplier<String> msgSupplier) {
            log(LogEvent.of(isLoggable(level), name,
                    sourceClass, sourceMethod,
                    level, null, msgSupplier, null, (Object[]) null));
        }

        @Override
        public void logp(sun.util.logging.PlatformLogger.Level level, String sourceClass,
                         String sourceMethod, String msg, Object... params) {
            log(LogEvent.of(isLoggable(level), name,
                    sourceClass, sourceMethod,
                    level, null, msg, null, params));
        }

        @Override
        public void logp(sun.util.logging.PlatformLogger.Level level, String sourceClass,
                         String sourceMethod, String msg, Throwable thrown) {
            log(LogEvent.of(isLoggable(level), name,
                    sourceClass, sourceMethod,
                    level, null, msg, thrown, (Object[]) null));
        }

        @Override
        public void logp(sun.util.logging.PlatformLogger.Level level, String sourceClass,
                         String sourceMethod, Throwable thrown,
                         Supplier<String> msgSupplier) {
            log(LogEvent.of(isLoggable(level), name,
                    sourceClass, sourceMethod,
                    level, null, msgSupplier, thrown, (Object[]) null));
        }

        @Override
        public void logrb(sun.util.logging.PlatformLogger.Level level, String sourceClass,
                          String sourceMethod, ResourceBundle bundle, String msg,
                          Object... params) {
            log(LogEvent.of(isLoggable(level), name,
                    sourceClass, sourceMethod,
                    level, bundle, msg, null, params));
        }

        @Override
        public void logrb(sun.util.logging.PlatformLogger.Level level, ResourceBundle bundle,
                          String msg, Object... params) {
            log(LogEvent.of(isLoggable(level), name, null, null,
                    level, bundle, msg, null, params));
        }

        @Override
        public void logrb(sun.util.logging.PlatformLogger.Level level, String sourceClass,
                          String sourceMethod, ResourceBundle bundle, String msg,
                          Throwable thrown) {
            log(LogEvent.of(isLoggable(level), name,
                    sourceClass, sourceMethod,
                    level, bundle, msg, thrown, (Object[]) null));
        }

        @Override
        public void logrb(sun.util.logging.PlatformLogger.Level level, ResourceBundle bundle,
                          String msg, Throwable thrown) {
            log(LogEvent.of(isLoggable(level), name, null, null,
                    level, bundle, msg, thrown, (Object[]) null));
        }

        @Override
        public boolean isLoggable(sun.util.logging.PlatformLogger.Level level) {
            return this.level != OFF && level.intValue()
                    >= this.level.intValue();
        }

        @Override
        public boolean isEnabled() {
            return this.level != OFF;
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

        final ConcurrentHashMap map = new ConcurrentHashMap();

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
                    System.out.println("\n*** With Security Manager, with access permission\n");
                    setSecurityManager();
                    final boolean control = allowControl.get().get();
                    try {
                        allowControl.get().set(true);
                        provider = LoggerFinder.getLoggerFinder();
                    } finally {
                        allowControl.get().set(control);
                    }
                    final boolean access = allowAccess.get().get();
                    try {
                        allowAccess.get().set(true);
                        test(provider, true);
                    } finally {
                        allowAccess.get().set(access);
                    }
                    break;
                default:
                    throw new RuntimeException("Unknown test case: " + testCase);
            }
        });
        System.out.println("\nPASSED: Tested " + sequencer.get() + " cases.");
    }

    public static void test(LoggerFinder provider, boolean hasRequiredPermissions) {

        final Map<PlatformLogger, String> loggerDescMap = new HashMap<>();

        PlatformLogger sysLogger1 = null;
        try {
            sysLogger1 = PlatformLogger.getLogger("foo");
            loggerDescMap.put(sysLogger1, "PlatformLogger.getLogger(\"foo\")");
            if (!hasRequiredPermissions) {
                throw new RuntimeException("Managed to obtain a system logger without permission");
            }
        } catch (AccessControlException acx) {
            if (hasRequiredPermissions) {
                throw new RuntimeException("Unexpected security exception: ", acx);
            }
            if (!acx.getPermission().equals(SimplePolicy.ACCESS_LOGGING)) {
                throw new RuntimeException("Unexpected permission in exception: " + acx, acx);
            }
            final boolean old = allowAccess.get().get();
            allowAccess.get().set(true);
            try {
                sysLogger1 = PlatformLogger.getLogger("foo");
                loggerDescMap.put(sysLogger1, "PlatformLogger.getLogger(\"foo\")");
            } finally {
                allowAccess.get().set(old);
            }
            System.out.println("Got expected exception for system logger: " + acx);
        }

        final LoggerImpl sysSink;
        boolean old = allowControl.get().get();
        allowControl.get().set(true);
        try {
           sysSink = LoggerImpl.class.cast(
                        provider.getLogger("foo", Thread.class.getModule()));
        } finally {
            allowControl.get().set(old);
        }

        testLogger(provider, loggerDescMap, "foo", null, sysLogger1, sysSink);
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

    // Calls the methods defined on LogProducer and verify the
    // parameters received by the underlying LoggerImpl
    // logger.
    private static void testLogger(LoggerFinder provider,
            Map<PlatformLogger, String> loggerDescMap,
            String name,
            ResourceBundle loggerBundle,
            PlatformLogger logger,
            LoggerImpl sink) {

        System.out.println("Testing " + loggerDescMap.get(logger) + " [" + logger +"]");
        final sun.util.logging.PlatformLogger.Level OFF = sun.util.logging.PlatformLogger.Level.OFF;
        final sun.util.logging.PlatformLogger.Level ALL = sun.util.logging.PlatformLogger.Level.OFF;

        Foo foo = new Foo();
        String fooMsg = foo.toString();
        System.out.println("\tlogger.log(messageLevel, fooMsg)");
        System.out.println("\tlogger.<level>(fooMsg)");
        for (sun.util.logging.PlatformLogger.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (sun.util.logging.PlatformLogger.Level messageLevel :julLevels) {
                if (messageLevel == ALL || messageLevel == OFF) continue;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            loggerLevel != OFF && messageLevel.intValue() >= loggerLevel.intValue(),
                            name, messageLevel, loggerBundle,
                            fooMsg, (Throwable)null, (Object[])null);
                String desc2 = "logger." + messageLevel.toString().toLowerCase()
                        + "(fooMsg): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                if (messageLevel == sun.util.logging.PlatformLogger.Level.FINEST) {
                    logger.finest(fooMsg);
                    checkLogEvent(provider, desc2, expected);
                } else if (messageLevel == sun.util.logging.PlatformLogger.Level.FINER) {
                    logger.finer(fooMsg);
                    checkLogEvent(provider, desc2, expected);
                } else if (messageLevel == sun.util.logging.PlatformLogger.Level.FINE) {
                    logger.fine(fooMsg);
                    checkLogEvent(provider, desc2, expected);
                } else if (messageLevel == sun.util.logging.PlatformLogger.Level.CONFIG) {
                    logger.config(fooMsg);
                    checkLogEvent(provider, desc2, expected);
                } else if (messageLevel == sun.util.logging.PlatformLogger.Level.INFO) {
                    logger.info(fooMsg);
                    checkLogEvent(provider, desc2, expected);
                } else if (messageLevel == sun.util.logging.PlatformLogger.Level.WARNING) {
                    logger.warning(fooMsg);
                    checkLogEvent(provider, desc2, expected);
                } else if (messageLevel == sun.util.logging.PlatformLogger.Level.SEVERE) {
                    logger.severe(fooMsg);
                    checkLogEvent(provider, desc2, expected);
                }
            }
        }

        String format = "two params [{1} {2}]";
        Object arg1 = foo;
        Object arg2 = fooMsg;
        System.out.println("\tlogger.log(messageLevel, format, arg1, arg2)");
        for (sun.util.logging.PlatformLogger.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (sun.util.logging.PlatformLogger.Level messageLevel :julLevels) {
                if (messageLevel == ALL || messageLevel == OFF) continue;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            loggerLevel != OFF && messageLevel.intValue() >= loggerLevel.intValue(),
                            name, messageLevel, loggerBundle,
                            format, (Throwable)null, arg1, arg2);
                String desc2 = "logger." + messageLevel.toString().toLowerCase()
                        + "(format, foo, fooMsg): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                if (messageLevel == sun.util.logging.PlatformLogger.Level.FINEST) {
                    logger.finest(format, arg1, arg2);
                    checkLogEvent(provider, desc2, expected);
                } else if (messageLevel == sun.util.logging.PlatformLogger.Level.FINER) {
                    logger.finer(format, arg1, arg2);
                    checkLogEvent(provider, desc2, expected);
                } else if (messageLevel == sun.util.logging.PlatformLogger.Level.FINE) {
                    logger.fine(format, arg1, arg2);
                    checkLogEvent(provider, desc2, expected);
                } else if (messageLevel == sun.util.logging.PlatformLogger.Level.CONFIG) {
                    logger.config(format, arg1, arg2);
                    checkLogEvent(provider, desc2, expected);
                } else if (messageLevel == sun.util.logging.PlatformLogger.Level.INFO) {
                    logger.info(format, arg1, arg2);
                    checkLogEvent(provider, desc2, expected);
                } else if (messageLevel == sun.util.logging.PlatformLogger.Level.WARNING) {
                    logger.warning(format, arg1, arg2);
                    checkLogEvent(provider, desc2, expected);
                } else if (messageLevel == sun.util.logging.PlatformLogger.Level.SEVERE) {
                    logger.severe(format, arg1, arg2);
                    checkLogEvent(provider, desc2, expected);
                }
            }
        }

        Throwable thrown = new Exception("OK: log me!");
        System.out.println("\tlogger.log(messageLevel, fooMsg, thrown)");
        for (sun.util.logging.PlatformLogger.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (sun.util.logging.PlatformLogger.Level messageLevel :julLevels) {
                if (messageLevel == ALL || messageLevel == OFF) continue;
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            loggerLevel != OFF && messageLevel.intValue() >= loggerLevel.intValue(),
                            name, messageLevel, loggerBundle,
                            fooMsg, thrown, (Object[])null);
                String desc2 = "logger." + messageLevel.toString().toLowerCase()
                        + "(fooMsg, thrown): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                if (messageLevel == sun.util.logging.PlatformLogger.Level.FINEST) {
                    logger.finest(fooMsg, thrown);
                    checkLogEvent(provider, desc2, expected);
                } else if (messageLevel == sun.util.logging.PlatformLogger.Level.FINER) {
                    logger.finer(fooMsg, thrown);
                    checkLogEvent(provider, desc2, expected);
                } else if (messageLevel == sun.util.logging.PlatformLogger.Level.FINE) {
                    logger.fine(fooMsg, thrown);
                    checkLogEvent(provider, desc2, expected);
                } else if (messageLevel == sun.util.logging.PlatformLogger.Level.CONFIG) {
                    logger.config(fooMsg, thrown);
                    checkLogEvent(provider, desc2, expected);
                } else if (messageLevel == sun.util.logging.PlatformLogger.Level.INFO) {
                    logger.info(fooMsg, thrown);
                    checkLogEvent(provider, desc2, expected);
                } else if (messageLevel == sun.util.logging.PlatformLogger.Level.WARNING) {
                    logger.warning(fooMsg, thrown);
                    checkLogEvent(provider, desc2, expected);
                } else if (messageLevel == sun.util.logging.PlatformLogger.Level.SEVERE) {
                    logger.severe(fooMsg, thrown);
                    checkLogEvent(provider, desc2, expected);
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
