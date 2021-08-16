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
import java.lang.System.LoggerFinder;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.security.AccessControlException;
import java.util.stream.Stream;
import sun.util.logging.PlatformLogger;

/**
 * @test
 * @bug     8140364
 * @summary JDK implementation specific unit test for JDK internal API.
 *   Tests a naive implementation of System.Logger, and in particular
 *   the default mapping provided by PlatformLogger.
 * @modules java.base/sun.util.logging
 * @build CustomSystemClassLoader BaseLoggerFinder BasePlatformLoggerTest
 * @run  main/othervm -Djava.system.class.loader=CustomSystemClassLoader BasePlatformLoggerTest NOSECURITY
 * @run  main/othervm -Djava.security.manager=allow -Djava.system.class.loader=CustomSystemClassLoader BasePlatformLoggerTest NOPERMISSIONS
 * @run  main/othervm -Djava.security.manager=allow -Djava.system.class.loader=CustomSystemClassLoader BasePlatformLoggerTest WITHPERMISSIONS
 * @author danielfuchs
 */
public class BasePlatformLoggerTest {

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

    static final Class<?> providerClass;
    static {
        try {
            providerClass = ClassLoader.getSystemClassLoader().loadClass("BaseLoggerFinder");
        } catch (ClassNotFoundException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    static final PlatformLogger.Level[] julLevels = {
        PlatformLogger.Level.ALL,
        PlatformLogger.Level.FINEST,
        PlatformLogger.Level.FINER,
        PlatformLogger.Level.FINE,
        PlatformLogger.Level.CONFIG,
        PlatformLogger.Level.INFO,
        PlatformLogger.Level.WARNING,
        PlatformLogger.Level.SEVERE,
        PlatformLogger.Level.OFF,
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

    final static Map<PlatformLogger.Level, Level> julToSpiMap;
    static {
        Map<PlatformLogger.Level, Level> map = new HashMap<>();
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


    public static interface TestLoggerFinder  {
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

            long sequenceNumber;
            boolean isLoggable;
            String loggerName;
            Level level;
            ResourceBundle bundle;
            Throwable thrown;
            Object[] args;
            Supplier<String> supplier;
            String msg;

            Object[] toArray() {
                return new Object[] {
                    sequenceNumber,
                    isLoggable,
                    loggerName,
                    level,
                    bundle,
                    thrown,
                    args,
                    supplier,
                    msg,
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
            public void log(Level level,  Supplier<String> msgSupplier, Throwable thrown) {
                log(LogEvent.of(isLoggable(level), name, level, thrown, msgSupplier));
            }
        }

        public Logger getLogger(String name, Module caller);
    }

    static PlatformLogger getPlatformLogger(String name) {
        boolean old = allowAccess.get().get();
        allowAccess.get().set(true);
        try {
            return PlatformLogger.getLogger(name);
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

        final Map<PlatformLogger, String> loggerDescMap = new HashMap<>();

        TestLoggerFinder.LoggerImpl appSink;
        boolean before = allowControl.get().get();
        try {
            allowControl.get().set(true);
            appSink = TestLoggerFinder.LoggerImpl.class.cast(
                        provider.getLogger("foo", BasePlatformLoggerTest.class.getModule()));
        } finally {
            allowControl.get().set(before);
        }

        TestLoggerFinder.LoggerImpl sysSink = null;
        before = allowControl.get().get();
        try {
            allowControl.get().set(true);
            sysSink = TestLoggerFinder.LoggerImpl.class.cast(
                        provider.getLogger("foo", Thread.class.getModule()));
        } finally {
            allowControl.get().set(before);
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

        PlatformLogger platform = getPlatformLogger("foo");
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

    static void checkLogEvent(TestLoggerFinder provider, String desc,
            TestLoggerFinder.LogEvent expected) {
        TestLoggerFinder.LogEvent actual =  provider.eventQueue.poll();
        if (!expected.equals(actual)) {
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

    // Calls the methods defined on LogProducer and verify the
    // parameters received by the underlying TestLoggerFinder.LoggerImpl
    // logger.
    private static void testLogger(TestLoggerFinder provider,
            Map<PlatformLogger, String> loggerDescMap,
            String name,
            ResourceBundle loggerBundle,
            PlatformLogger logger,
            TestLoggerFinder.LoggerImpl sink) {

        System.out.println("Testing " + loggerDescMap.get(logger));

        Foo foo = new Foo();
        String fooMsg = foo.toString();
        System.out.println("\tlogger.<level>(fooMsg)");
        for (Level loggerLevel : Level.values()) {
            sink.level = loggerLevel;
            for (PlatformLogger.Level messageLevel :julLevels) {
                Level expectedMessageLevel = julToSpiMap.get(messageLevel);
                TestLoggerFinder.LogEvent expected =
                        TestLoggerFinder.LogEvent.of(
                            sequencer.get(),
                            loggerLevel != Level.OFF && expectedMessageLevel.compareTo(loggerLevel) >= 0,
                            name, expectedMessageLevel, loggerBundle,
                            fooMsg, null, (Throwable)null, (Object[])null);
                String desc2 = "logger." + messageLevel.toString().toLowerCase()
                        + "(fooMsg): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                if (messageLevel == PlatformLogger.Level.FINEST) {
                    logger.finest(fooMsg);
                    checkLogEvent(provider, desc2, expected);
                } else if (messageLevel == PlatformLogger.Level.FINER) {
                    logger.finer(fooMsg);
                    checkLogEvent(provider, desc2, expected);
                } else if (messageLevel == PlatformLogger.Level.FINE) {
                    logger.fine(fooMsg);
                    checkLogEvent(provider, desc2, expected);
                } else if (messageLevel == PlatformLogger.Level.CONFIG) {
                    logger.config(fooMsg);
                    checkLogEvent(provider, desc2, expected);
                } else if (messageLevel == PlatformLogger.Level.INFO) {
                    logger.info(fooMsg);
                    checkLogEvent(provider, desc2, expected);
                } else if (messageLevel == PlatformLogger.Level.WARNING) {
                    logger.warning(fooMsg);
                    checkLogEvent(provider, desc2, expected);
                } else if (messageLevel == PlatformLogger.Level.SEVERE) {
                    logger.severe(fooMsg);
                    checkLogEvent(provider, desc2, expected);
                }
            }
        }

        Throwable thrown = new Exception("OK: log me!");
        System.out.println("\tlogger.<level>(msg, thrown)");
        for (Level loggerLevel : Level.values()) {
            sink.level = loggerLevel;
            for (PlatformLogger.Level messageLevel :julLevels) {
                Level expectedMessageLevel = julToSpiMap.get(messageLevel);
                TestLoggerFinder.LogEvent expected =
                        TestLoggerFinder.LogEvent.of(
                            sequencer.get(),
                            loggerLevel != Level.OFF && expectedMessageLevel.compareTo(loggerLevel) >= 0,
                            name, expectedMessageLevel, (ResourceBundle) null,
                            fooMsg, null, (Throwable)thrown, (Object[])null);
                String desc2 = "logger." + messageLevel.toString().toLowerCase()
                        + "(msg, thrown): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                if (messageLevel == PlatformLogger.Level.FINEST) {
                    logger.finest(fooMsg, thrown);
                    checkLogEvent(provider, desc2, expected);
                } else if (messageLevel == PlatformLogger.Level.FINER) {
                    logger.finer(fooMsg, thrown);
                    checkLogEvent(provider, desc2, expected);
                } else if (messageLevel == PlatformLogger.Level.FINE) {
                    logger.fine(fooMsg, thrown);
                    checkLogEvent(provider, desc2, expected);
                } else if (messageLevel == PlatformLogger.Level.CONFIG) {
                    logger.config(fooMsg, thrown);
                    checkLogEvent(provider, desc2, expected);
                } else if (messageLevel == PlatformLogger.Level.INFO) {
                    logger.info(fooMsg, thrown);
                    checkLogEvent(provider, desc2, expected);
                } else if (messageLevel == PlatformLogger.Level.WARNING) {
                    logger.warning(fooMsg, thrown);
                    checkLogEvent(provider, desc2, expected);
                } else if (messageLevel == PlatformLogger.Level.SEVERE) {
                    logger.severe(fooMsg, thrown);
                    checkLogEvent(provider, desc2, expected);
                }
            }
        }

        String format = "two params [{1} {2}]";
        Object arg1 = foo;
        Object arg2 = fooMsg;
        System.out.println("\tlogger.<level>(format, arg1, arg2)");
        for (Level loggerLevel : Level.values()) {
            sink.level = loggerLevel;
            for (PlatformLogger.Level messageLevel :julLevels) {
                Level expectedMessageLevel = julToSpiMap.get(messageLevel);
                TestLoggerFinder.LogEvent expected =
                        TestLoggerFinder.LogEvent.of(
                            sequencer.get(),
                            loggerLevel != Level.OFF && expectedMessageLevel.compareTo(loggerLevel) >= 0,
                            name, expectedMessageLevel, (ResourceBundle) null,
                            format, null, (Throwable)null, foo, fooMsg);
                String desc2 = "logger." + messageLevel.toString().toLowerCase()
                        + "(format, foo, fooMsg): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                if (messageLevel == PlatformLogger.Level.FINEST) {
                    logger.finest(format, foo, fooMsg);
                    checkLogEvent(provider, desc2, expected);
                } else if (messageLevel == PlatformLogger.Level.FINER) {
                    logger.finer(format, foo, fooMsg);
                    checkLogEvent(provider, desc2, expected);
                } else if (messageLevel == PlatformLogger.Level.FINE) {
                    logger.fine(format, foo, fooMsg);
                    checkLogEvent(provider, desc2, expected);
                } else if (messageLevel == PlatformLogger.Level.CONFIG) {
                    logger.config(format, foo, fooMsg);
                    checkLogEvent(provider, desc2, expected);
                } else if (messageLevel == PlatformLogger.Level.INFO) {
                    logger.info(format, foo, fooMsg);
                    checkLogEvent(provider, desc2, expected);
                } else if (messageLevel == PlatformLogger.Level.WARNING) {
                    logger.warning(format, foo, fooMsg);
                    checkLogEvent(provider, desc2, expected);
                } else if (messageLevel == PlatformLogger.Level.SEVERE) {
                    logger.severe(format, foo, fooMsg);
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
