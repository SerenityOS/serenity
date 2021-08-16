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
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Stream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.lang.System.LoggerFinder;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

/**
 * @test
 * @bug     8140364
 * @summary Tests a naive implementation of LoggerFinder, and in particular
 *   the default body of System.Logger methods.
 * @build AccessSystemLogger BaseLoggerFinderTest CustomSystemClassLoader BaseLoggerFinder TestLoggerFinder
 * @run  driver AccessSystemLogger
 * @run  main/othervm -Xbootclasspath/a:boot -Djava.system.class.loader=CustomSystemClassLoader BaseLoggerFinderTest NOSECURITY
 * @run  main/othervm -Xbootclasspath/a:boot -Djava.security.manager=allow -Djava.system.class.loader=CustomSystemClassLoader BaseLoggerFinderTest NOPERMISSIONS
 * @run  main/othervm -Xbootclasspath/a:boot -Djava.security.manager=allow -Djava.system.class.loader=CustomSystemClassLoader BaseLoggerFinderTest WITHPERMISSIONS
 * @author danielfuchs
 */
public class BaseLoggerFinderTest {

    static final RuntimePermission LOGGERFINDER_PERMISSION =
                new RuntimePermission("loggerFinder");
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

    final static AccessSystemLogger accessSystemLogger = new AccessSystemLogger();
    static final Class<?> providerClass;
    static {
        try {
            providerClass = ClassLoader.getSystemClassLoader().loadClass("BaseLoggerFinder");
        } catch (ClassNotFoundException ex) {
            throw new ExceptionInInitializerError(ex);
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

    static enum TestCases {NOSECURITY, NOPERMISSIONS, WITHPERMISSIONS};

    static void setSecurityManager() {
        if (System.getSecurityManager() == null) {
            Policy.setPolicy(new SimplePolicy(allowControl, allowAccess));
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

        System.out.println("Using provider class: " + providerClass + "[" + providerClass.getClassLoader() + "]");

        Stream.of(args).map(TestCases::valueOf).forEach((testCase) -> {
            TestLoggerFinder provider;
            switch (testCase) {
                case NOSECURITY:
                    System.out.println("\n*** Without Security Manager\n");
                    provider = TestLoggerFinder.class.cast(LoggerFinder.getLoggerFinder());
                    test(provider, true);
                    System.out.println("Tetscase count: " + TestLoggerFinder.sequencer.get());
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
                    System.out.println("Tetscase count: " + TestLoggerFinder.sequencer.get());
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
        System.out.println("\nPASSED: Tested " + TestLoggerFinder.sequencer.get() + " cases.");
    }

    public static void test(TestLoggerFinder provider, boolean hasRequiredPermissions) {

        ResourceBundle loggerBundle = ResourceBundle.getBundle(MyLoggerBundle.class.getName());
        final Map<Logger, String> loggerDescMap = new HashMap<>();


        // 1. Test loggers returned by LoggerFinder, both for system callers
        //    and not system callers.
        TestLoggerFinder.LoggerImpl appLogger1 = null;
        try {
            appLogger1 =
                TestLoggerFinder.LoggerImpl.class.cast(provider.getLogger("foo", BaseLoggerFinderTest.class.getModule()));
            loggerDescMap.put(appLogger1, "provider.getLogger(\"foo\", BaseLoggerFinderTest.class.getModule())");
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
            final boolean old = allowControl.get().get();
            allowControl.get().set(true);
            try {
                appLogger1 =
                    TestLoggerFinder.LoggerImpl.class.cast(provider.getLogger("foo", BaseLoggerFinderTest.class.getModule()));
                    loggerDescMap.put(appLogger1, "provider.getLogger(\"foo\", BaseLoggerFinderTest.class.getModule())");
            } finally {
                allowControl.get().set(old);
            }
        }

        TestLoggerFinder.LoggerImpl sysLogger1 = null;
        try {
            sysLogger1 = TestLoggerFinder.LoggerImpl.class.cast(provider.getLogger("foo", Thread.class.getModule()));
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
            final boolean old = allowControl.get().get();
            allowControl.get().set(true);
            try {
                sysLogger1 = TestLoggerFinder.LoggerImpl.class.cast(provider.getLogger("foo", Thread.class.getModule()));
                loggerDescMap.put(sysLogger1, "provider.getLogger(\"foo\", Thread.class.getModule())");
            } finally {
                allowControl.get().set(old);
            }
        }
        if (appLogger1 == sysLogger1) {
            throw new RuntimeException("identical loggers");
        }

        if (provider.system.contains(appLogger1)) {
            throw new RuntimeException("app logger in system map");
        }
        if (!provider.user.contains(appLogger1)) {
            throw new RuntimeException("app logger not in appplication map");
        }
        if (provider.user.contains(sysLogger1)) {
            throw new RuntimeException("sys logger in appplication map");
        }
        if (!provider.system.contains(sysLogger1)) {
            throw new RuntimeException("sys logger not in system map");
        }

        testLogger(provider, loggerDescMap, "foo", null, appLogger1, appLogger1);
        testLogger(provider, loggerDescMap, "foo", null, sysLogger1, sysLogger1);

        // 2. Test localized loggers returned LoggerFinder, both for system
        //   callers and non system callers
        Logger appLogger2 = null;
        try {
            appLogger2 = provider.getLocalizedLogger("foo", loggerBundle, BaseLoggerFinderTest.class.getModule());
            loggerDescMap.put(appLogger2, "provider.getLocalizedLogger(\"foo\", loggerBundle, BaseLoggerFinderTest.class.getModule())");
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
            final boolean old = allowControl.get().get();
            allowControl.get().set(true);
            try {
                appLogger2 = provider.getLocalizedLogger("foo", loggerBundle, BaseLoggerFinderTest.class.getModule());
                loggerDescMap.put(appLogger2, "provider.getLocalizedLogger(\"foo\", loggerBundle, BaseLoggerFinderTest.class.getModule())");
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
            final boolean old = allowControl.get().get();
            allowControl.get().set(true);
            try {
                sysLogger2 = provider.getLocalizedLogger("foo", loggerBundle, Thread.class.getModule());
                loggerDescMap.put(sysLogger2, "provider.getLocalizedLogger(\"foo\", loggerBundle, Thread.class.getModule()))");
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

        if (provider.system.contains(appLogger2)) {
            throw new RuntimeException("localized app logger in system map");
        }
        if (provider.user.contains(appLogger2)) {
            throw new RuntimeException("localized app logger  in appplication map");
        }
        if (provider.user.contains(sysLogger2)) {
            throw new RuntimeException("localized sys logger in appplication map");
        }
        if (provider.system.contains(sysLogger2)) {
            throw new RuntimeException("localized sys logger not in system map");
        }

        testLogger(provider, loggerDescMap, "foo", loggerBundle, appLogger2, appLogger1);
        testLogger(provider, loggerDescMap, "foo", loggerBundle, sysLogger2, sysLogger1);

        // 3 Test loggers returned by:
        //   3.1: System.getLogger("foo")
        Logger appLogger3 = System.getLogger("foo");
        loggerDescMap.put(appLogger3, "System.getLogger(\"foo\")");
        testLogger(provider, loggerDescMap, "foo", null, appLogger3, appLogger1);

        //   3.2: System.getLogger("foo")
        //        Emulate what System.getLogger() does when the caller is a
        //        platform classes
        Logger sysLogger3 = accessSystemLogger.getLogger("foo");
        loggerDescMap.put(sysLogger3, "AccessSystemLogger.getLogger(\"foo\")");

        if (appLogger3 == sysLogger3) {
            throw new RuntimeException("identical loggers");
        }

        testLogger(provider, loggerDescMap, "foo", null, sysLogger3, sysLogger1);

        // 4. Test loggers returned by:
        //    4.1 System.getLogger("foo", loggerBundle)
        Logger appLogger4 =
                System.getLogger("foo", loggerBundle);
        loggerDescMap.put(appLogger4, "System.getLogger(\"foo\", loggerBundle)");
        if (appLogger4 == appLogger1) {
            throw new RuntimeException("identical loggers");
        }

        testLogger(provider, loggerDescMap, "foo", loggerBundle, appLogger4, appLogger1);

        //   4.2: System.getLogger("foo", loggerBundle)
        //        Emulate what System.getLogger() does when the caller is a
        //        platform classes
        Logger sysLogger4 = accessSystemLogger.getLogger("foo", loggerBundle);
        loggerDescMap.put(sysLogger4, "AccessSystemLogger.getLogger(\"foo\", loggerBundle)");
        if (appLogger4 == sysLogger4) {
            throw new RuntimeException("identical loggers");
        }

        testLogger(provider, loggerDescMap, "foo", loggerBundle, sysLogger4, sysLogger1);

    }

    public static class Foo {

    }

    static void verbose(String msg) {
       if (VERBOSE) {
           System.out.println(msg);
       }
    }

    // Calls the 8 methods defined on Logger and verify the
    // parameters received by the underlying TestProvider.LoggerImpl
    // logger.
    private static void testLogger(TestLoggerFinder provider,
            Map<Logger, String> loggerDescMap,
            String name,
            ResourceBundle loggerBundle,
            Logger logger,
            TestLoggerFinder.LoggerImpl sink) {

        System.out.println("Testing " + loggerDescMap.get(logger) + " [" + logger +"]");
        AtomicLong sequencer = TestLoggerFinder.sequencer;

        Foo foo = new Foo();
        String fooMsg = foo.toString();
        for (Level loggerLevel : Level.values()) {
            sink.level = loggerLevel;
            for (Level messageLevel : Level.values()) {
                String desc = "logger.log(messageLevel, foo): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                TestLoggerFinder.LogEvent expected =
                        TestLoggerFinder.LogEvent.of(
                            sequencer.get(),
                            messageLevel.compareTo(loggerLevel) >= 0,
                            name, messageLevel, (ResourceBundle)null,
                            fooMsg, null, (Throwable)null, (Object[])null);
                logger.log(messageLevel, foo);
                if (loggerLevel == Level.OFF || messageLevel.compareTo(loggerLevel) < 0) {
                    if (provider.eventQueue.poll() != null) {
                        throw new RuntimeException("unexpected event in queue for " + desc);
                    }
                } else {
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
            }
        }

        String msg = "blah";
        for (Level loggerLevel : Level.values()) {
            sink.level = loggerLevel;
            for (Level messageLevel : Level.values()) {
                String desc = "logger.log(messageLevel, \"blah\"): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                TestLoggerFinder.LogEvent expected =
                        TestLoggerFinder.LogEvent.of(
                            sequencer.get(),
                            messageLevel.compareTo(loggerLevel) >= 0 && loggerLevel != Level.OFF,
                            name, messageLevel, loggerBundle,
                            msg, null, (Throwable)null, (Object[])null);
                logger.log(messageLevel, msg);
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
        }

        Supplier<String> fooSupplier = new Supplier<String>() {
            @Override
            public String get() {
                return this.toString();
            }
        };

        for (Level loggerLevel : Level.values()) {
            sink.level = loggerLevel;
            for (Level messageLevel : Level.values()) {
                String desc = "logger.log(messageLevel, fooSupplier): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                TestLoggerFinder.LogEvent expected =
                        TestLoggerFinder.LogEvent.of(
                            sequencer.get(),
                            messageLevel.compareTo(loggerLevel) >= 0,
                            name, messageLevel, (ResourceBundle)null,
                            fooSupplier.get(), null,
                            (Throwable)null, (Object[])null);
                logger.log(messageLevel, fooSupplier);
                if (loggerLevel == Level.OFF || messageLevel.compareTo(loggerLevel) < 0) {
                    if (provider.eventQueue.poll() != null) {
                        throw new RuntimeException("unexpected event in queue for " + desc);
                    }
                } else {
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
            }
        }

        String format = "two params [{1} {2}]";
        Object arg1 = foo;
        Object arg2 = msg;
        for (Level loggerLevel : Level.values()) {
            sink.level = loggerLevel;
            for (Level messageLevel : Level.values()) {
                String desc = "logger.log(messageLevel, format, params...): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                TestLoggerFinder.LogEvent expected =
                        TestLoggerFinder.LogEvent.of(
                            sequencer.get(),
                            messageLevel.compareTo(loggerLevel) >= 0 && loggerLevel != Level.OFF,
                            name, messageLevel, loggerBundle,
                            format, null, (Throwable)null, new Object[] {foo, msg});
                logger.log(messageLevel, format, foo, msg);
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
        }

        Throwable thrown = new Exception("OK: log me!");
        for (Level loggerLevel : Level.values()) {
            sink.level = loggerLevel;
            for (Level messageLevel : Level.values()) {
                String desc = "logger.log(messageLevel, \"blah\", thrown): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                TestLoggerFinder.LogEvent expected =
                        TestLoggerFinder.LogEvent.of(
                            sequencer.get(),
                            messageLevel.compareTo(loggerLevel) >= 0 && loggerLevel != Level.OFF,
                            name, messageLevel, loggerBundle,
                            msg, null, thrown, (Object[]) null);
                logger.log(messageLevel, msg, thrown);
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
        }


        for (Level loggerLevel : Level.values()) {
            sink.level = loggerLevel;
            for (Level messageLevel : Level.values()) {
                String desc = "logger.log(messageLevel, thrown, fooSupplier): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                TestLoggerFinder.LogEvent expected =
                        TestLoggerFinder.LogEvent.of(
                            sequencer.get(),
                            messageLevel.compareTo(loggerLevel) >= 0,
                            name, messageLevel, (ResourceBundle)null,
                            fooSupplier.get(), null,
                            (Throwable)thrown, (Object[])null);
                logger.log(messageLevel, fooSupplier, thrown);
                if (loggerLevel == Level.OFF || messageLevel.compareTo(loggerLevel) < 0) {
                    if (provider.eventQueue.poll() != null) {
                        throw new RuntimeException("unexpected event in queue for " + desc);
                    }
                } else {
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
            }
        }

        ResourceBundle bundle = ResourceBundle.getBundle(MyBundle.class.getName());
        for (Level loggerLevel : Level.values()) {
            sink.level = loggerLevel;
            for (Level messageLevel : Level.values()) {
                String desc = "logger.log(messageLevel, bundle, format, params...): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                TestLoggerFinder.LogEvent expected =
                        TestLoggerFinder.LogEvent.of(
                            sequencer.get(),
                            messageLevel.compareTo(loggerLevel) >= 0 && loggerLevel != Level.OFF,
                            name, messageLevel, bundle,
                            format, null, (Throwable)null, new Object[] {foo, msg});
                logger.log(messageLevel, bundle, format, foo, msg);
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
        }

        for (Level loggerLevel : Level.values()) {
            sink.level = loggerLevel;
            for (Level messageLevel : Level.values()) {
                String desc = "logger.log(messageLevel, bundle, \"blah\", thrown): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                TestLoggerFinder.LogEvent expected =
                        TestLoggerFinder.LogEvent.of(
                            sequencer.get(),
                            messageLevel.compareTo(loggerLevel) >= 0 && loggerLevel != Level.OFF,
                            name, messageLevel, bundle,
                            msg, null, thrown, (Object[]) null);
                logger.log(messageLevel, bundle, msg, thrown);
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
        final static RuntimePermission ACCESS = new RuntimePermission("accessClassInPackage.jdk.internal.logger");

        static final Policy DEFAULT_POLICY = Policy.getPolicy();

        final Permissions permissions;
        final ThreadLocal<AtomicBoolean> allowControl;
        final ThreadLocal<AtomicBoolean> allowAccess;
        public SimplePolicy(ThreadLocal<AtomicBoolean> allowControl, ThreadLocal<AtomicBoolean> allowAccess) {
            this.allowControl = allowControl;
            this.allowAccess = allowAccess;
            permissions = new Permissions();
        }

        Permissions getPermissions() {
            if (allowControl.get().get() || allowAccess.get().get()) {
                PermissionsBuilder builder =  new PermissionsBuilder()
                        .addAll(permissions);
                if (allowControl.get().get()) {
                    builder.add(CONTROL);
                }
                if (allowAccess.get().get()) {
                    builder.add(ACCESS);
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
