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
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.lang.System.LoggerFinder;
import java.util.logging.Logger;
import sun.util.logging.PlatformLogger;
import sun.util.logging.internal.LoggingProviderImpl;

/**
 * @test
 * @bug     8140364
 * @summary Tests all PlatformLogger methods with the default LoggerFinder JUL backend.
 * @modules java.base/sun.util.logging java.logging/sun.util.logging.internal
 * @run  main/othervm -Djava.security.manager=allow DefaultPlatformLoggerTest
 * @author danielfuchs
 */
public class DefaultPlatformLoggerTest {

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
                    DefaultPlatformLoggerTest.class.getName(),
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

    static final java.util.logging.Level[] julPlatformLevels = {
        java.util.logging.Level.FINEST,
        java.util.logging.Level.FINER,
        java.util.logging.Level.FINE,
        java.util.logging.Level.CONFIG,
        java.util.logging.Level.INFO,
        java.util.logging.Level.WARNING,
        java.util.logging.Level.SEVERE,
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

    public static void main(String[] args) throws Exception {
        LoggerFinder provider = LoggerFinder.getLoggerFinder();
        java.util.logging.Logger appSink = LoggingProviderImpl.getLogManagerAccess()
                .demandLoggerFor(LogManager.getLogManager(), "foo",
                        DefaultPlatformLoggerTest.class.getModule());
        java.util.logging.Logger sysSink = LoggingProviderImpl.getLogManagerAccess()
                .demandLoggerFor(LogManager.getLogManager(),"foo", Thread.class.getModule());
        java.util.logging.Logger sink = Logger.getLogger("foo");
        sink.addHandler(new MyHandler());
        sink.setUseParentHandlers(VERBOSE);

        System.out.println("\n*** Without Security Manager\n");
        test(provider, true, appSink, sysSink);
        System.out.println("Tetscase count: " + sequencer.get());

        Policy.setPolicy(new SimplePolicy(allowAll, allowControl));
        System.setSecurityManager(new SecurityManager());

        System.out.println("\n*** With Security Manager, without permissions\n");
        test(provider, false, appSink, sysSink);
        System.out.println("Tetscase count: " + sequencer.get());

        System.out.println("\n*** With Security Manager, with control permission\n");
        allowControl.get().set(true);
        test(provider, true, appSink, sysSink);

        System.out.println("\nPASSED: Tested " + sequencer.get() + " cases.");
    }

    public static void test(LoggerFinder provider, boolean hasRequiredPermissions,
            java.util.logging.Logger appSink, java.util.logging.Logger sysSink) throws Exception {

        // No way to give a resource bundle to a platform logger.
        // ResourceBundle loggerBundle = ResourceBundle.getBundle(MyLoggerBundle.class.getName());
        final Map<PlatformLogger, String> loggerDescMap = new HashMap<>();

        PlatformLogger platform = PlatformLogger.getLogger("foo");
        loggerDescMap.put(platform, "PlatformLogger.getLogger(\"foo\")");

        testLogger(provider, loggerDescMap, "foo", null, platform, sysSink);
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

    // Calls the methods defined on LogProducer and verify the
    // parameters received by the underlying logger.
    private static void testLogger(LoggerFinder provider,
            Map<PlatformLogger, String> loggerDescMap,
            String name,
            ResourceBundle loggerBundle,
            PlatformLogger logger,
            java.util.logging.Logger sink) throws Exception {

        System.out.println("Testing " + loggerDescMap.get(logger));
        final java.util.logging.Level OFF = java.util.logging.Level.OFF;

        Foo foo = new Foo();
        String fooMsg = foo.toString();
        System.out.println("\tlogger.<level>(fooMsg)");
        for (java.util.logging.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (java.util.logging.Level messageLevel : julPlatformLevels) {
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            loggerLevel != OFF && messageLevel.intValue() >= loggerLevel.intValue(),
                            name, messageLevel, loggerBundle,
                            fooMsg, (Throwable)null, (Object[])null);
                String desc2 = "logger." + messageLevel.toString().toLowerCase()
                        + "(fooMsg): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                if (messageLevel == java.util.logging.Level.FINEST) {
                    logger.finest(fooMsg);
                    checkLogEvent(provider, desc2, expected, expected.isLoggable);
                } else if (messageLevel == java.util.logging.Level.FINER) {
                    logger.finer(fooMsg);
                    checkLogEvent(provider, desc2, expected, expected.isLoggable);
                } else if (messageLevel == java.util.logging.Level.FINE) {
                    logger.fine(fooMsg);
                    checkLogEvent(provider, desc2, expected, expected.isLoggable);
                } else if (messageLevel == java.util.logging.Level.CONFIG) {
                    logger.config(fooMsg);
                    checkLogEvent(provider, desc2, expected, expected.isLoggable);
                } else if (messageLevel == java.util.logging.Level.INFO) {
                    logger.info(fooMsg);
                    checkLogEvent(provider, desc2, expected, expected.isLoggable);
                } else if (messageLevel == java.util.logging.Level.WARNING) {
                    logger.warning(fooMsg);
                    checkLogEvent(provider, desc2, expected, expected.isLoggable);
                } else if (messageLevel == java.util.logging.Level.SEVERE) {
                    logger.severe(fooMsg);
                    checkLogEvent(provider, desc2, expected, expected.isLoggable);
                }
            }
        }

        Throwable thrown = new Exception("OK: log me!");
        System.out.println("\tlogger.<level>(msg, thrown)");
        for (java.util.logging.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (java.util.logging.Level messageLevel :julPlatformLevels) {
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            loggerLevel != OFF && messageLevel.intValue() >= loggerLevel.intValue(),
                            name, messageLevel, loggerBundle,
                            fooMsg, thrown, (Object[])null);
                String desc2 = "logger." + messageLevel.toString().toLowerCase()
                        + "(msg, thrown): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                if (messageLevel == java.util.logging.Level.FINEST) {
                    logger.finest(fooMsg, thrown);
                    checkLogEvent(provider, desc2, expected, expected.isLoggable);
                } else if (messageLevel == java.util.logging.Level.FINER) {
                    logger.finer(fooMsg, thrown);
                    checkLogEvent(provider, desc2, expected, expected.isLoggable);
                } else if (messageLevel == java.util.logging.Level.FINE) {
                    logger.fine(fooMsg, thrown);
                    checkLogEvent(provider, desc2, expected, expected.isLoggable);
                } else if (messageLevel == java.util.logging.Level.CONFIG) {
                    logger.config(fooMsg, thrown);
                    checkLogEvent(provider, desc2, expected, expected.isLoggable);
                } else if (messageLevel == java.util.logging.Level.INFO) {
                    logger.info(fooMsg, thrown);
                    checkLogEvent(provider, desc2, expected, expected.isLoggable);
                } else if (messageLevel == java.util.logging.Level.WARNING) {
                    logger.warning(fooMsg, thrown);
                    checkLogEvent(provider, desc2, expected, expected.isLoggable);
                } else if (messageLevel == java.util.logging.Level.SEVERE) {
                    logger.severe(fooMsg, thrown);
                    checkLogEvent(provider, desc2, expected, expected.isLoggable);
                }
            }
        }

        String format = "two params [{1} {2}]";
        Object arg1 = foo;
        Object arg2 = fooMsg;
        System.out.println("\tlogger.<level>(format, arg1, arg2)");
        for (java.util.logging.Level loggerLevel : julLevels) {
            setLevel(sink, loggerLevel);
            for (java.util.logging.Level messageLevel : julPlatformLevels) {
                LogEvent expected =
                        LogEvent.of(
                            sequencer.get(),
                            loggerLevel != OFF && messageLevel.intValue() >= loggerLevel.intValue(),
                            name, messageLevel, loggerBundle,
                            format, (Throwable)null, foo, fooMsg);
                String desc2 = "logger." + messageLevel.toString().toLowerCase()
                        + "(format, foo, fooMsg): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                if (messageLevel == java.util.logging.Level.FINEST) {
                    logger.finest(format, foo, fooMsg);
                    checkLogEvent(provider, desc2, expected, expected.isLoggable);
                } else if (messageLevel == java.util.logging.Level.FINER) {
                    logger.finer(format, foo, fooMsg);
                    checkLogEvent(provider, desc2, expected, expected.isLoggable);
                } else if (messageLevel == java.util.logging.Level.FINE) {
                    logger.fine(format, foo, fooMsg);
                    checkLogEvent(provider, desc2, expected, expected.isLoggable);
                } else if (messageLevel == java.util.logging.Level.CONFIG) {
                    logger.config(format, foo, fooMsg);
                    checkLogEvent(provider, desc2, expected, expected.isLoggable);
                } else if (messageLevel == java.util.logging.Level.INFO) {
                    logger.info(format, foo, fooMsg);
                    checkLogEvent(provider, desc2, expected, expected.isLoggable);
                } else if (messageLevel == java.util.logging.Level.WARNING) {
                    logger.warning(format, foo, fooMsg);
                    checkLogEvent(provider, desc2, expected, expected.isLoggable);
                } else if (messageLevel == java.util.logging.Level.SEVERE) {
                    logger.severe(format, foo, fooMsg);
                    checkLogEvent(provider, desc2, expected, expected.isLoggable);
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
        public static final RuntimePermission LOGGERFINDER_PERMISSION =
                new RuntimePermission("loggerFinder");

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
