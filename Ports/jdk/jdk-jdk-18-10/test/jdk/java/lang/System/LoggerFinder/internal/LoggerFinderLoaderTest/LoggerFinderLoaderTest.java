/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.lang.System.LoggerFinder;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @test
 * @bug     8140364 8189291
 * @summary JDK implementation specific unit test for LoggerFinderLoader.
 *          Tests the behavior of LoggerFinderLoader with respect to the
 *          value of the internal diagnosability switches. Also test the
 *          DefaultLoggerFinder and SimpleConsoleLogger implementation.
 * @modules java.base/sun.util.logging
 *          java.base/jdk.internal.logger
 * @build AccessSystemLogger LoggerFinderLoaderTest CustomSystemClassLoader BaseLoggerFinder BaseLoggerFinder2
 * @run  driver AccessSystemLogger
 * @run  main/othervm -Xbootclasspath/a:boot -Djava.system.class.loader=CustomSystemClassLoader LoggerFinderLoaderTest NOSECURITY
 * @run  main/othervm -Xbootclasspath/a:boot -Djava.security.manager=allow -Djava.system.class.loader=CustomSystemClassLoader LoggerFinderLoaderTest NOPERMISSIONS
 * @run  main/othervm -Xbootclasspath/a:boot -Djava.security.manager=allow -Djava.system.class.loader=CustomSystemClassLoader LoggerFinderLoaderTest WITHPERMISSIONS
 * @run  main/othervm -Xbootclasspath/a:boot -Djava.system.class.loader=CustomSystemClassLoader -Dtest.fails=true LoggerFinderLoaderTest NOSECURITY
 * @run  main/othervm -Xbootclasspath/a:boot -Djava.security.manager=allow -Djava.system.class.loader=CustomSystemClassLoader -Dtest.fails=true LoggerFinderLoaderTest NOPERMISSIONS
 * @run  main/othervm -Xbootclasspath/a:boot -Djava.security.manager=allow -Djava.system.class.loader=CustomSystemClassLoader -Dtest.fails=true LoggerFinderLoaderTest WITHPERMISSIONS
 * @run  main/othervm -Xbootclasspath/a:boot -Djava.system.class.loader=CustomSystemClassLoader -Dtest.fails=true -Djdk.logger.finder.error=ERROR LoggerFinderLoaderTest NOSECURITY
 * @run  main/othervm -Xbootclasspath/a:boot -Djava.security.manager=allow -Djava.system.class.loader=CustomSystemClassLoader -Dtest.fails=true -Djdk.logger.finder.error=ERROR LoggerFinderLoaderTest NOPERMISSIONS
 * @run  main/othervm -Xbootclasspath/a:boot -Djava.security.manager=allow -Djava.system.class.loader=CustomSystemClassLoader -Dtest.fails=true -Djdk.logger.finder.error=ERROR LoggerFinderLoaderTest WITHPERMISSIONS
 * @run  main/othervm -Xbootclasspath/a:boot -Djava.system.class.loader=CustomSystemClassLoader -Dtest.fails=true -Djdk.logger.finder.error=DEBUG LoggerFinderLoaderTest NOSECURITY
 * @run  main/othervm -Xbootclasspath/a:boot -Djava.security.manager=allow -Djava.system.class.loader=CustomSystemClassLoader -Dtest.fails=true -Djdk.logger.finder.error=DEBUG LoggerFinderLoaderTest NOPERMISSIONS
 * @run  main/othervm -Xbootclasspath/a:boot -Djava.security.manager=allow -Djava.system.class.loader=CustomSystemClassLoader -Dtest.fails=true -Djdk.logger.finder.error=DEBUG LoggerFinderLoaderTest WITHPERMISSIONS
 * @run  main/othervm -Xbootclasspath/a:boot -Djava.system.class.loader=CustomSystemClassLoader -Dtest.fails=true -Djdk.logger.finder.error=QUIET LoggerFinderLoaderTest NOSECURITY
 * @run  main/othervm -Xbootclasspath/a:boot -Djava.security.manager=allow -Djava.system.class.loader=CustomSystemClassLoader -Dtest.fails=true -Djdk.logger.finder.error=QUIET LoggerFinderLoaderTest NOPERMISSIONS
 * @run  main/othervm -Xbootclasspath/a:boot -Djava.security.manager=allow -Djava.system.class.loader=CustomSystemClassLoader -Dtest.fails=true -Djdk.logger.finder.error=QUIET LoggerFinderLoaderTest WITHPERMISSIONS
 * @run  main/othervm -Xbootclasspath/a:boot -Djava.system.class.loader=CustomSystemClassLoader -Djdk.logger.finder.singleton=true LoggerFinderLoaderTest NOSECURITY
 * @run  main/othervm -Xbootclasspath/a:boot -Djava.security.manager=allow -Djava.system.class.loader=CustomSystemClassLoader -Djdk.logger.finder.singleton=true LoggerFinderLoaderTest NOPERMISSIONS
 * @run  main/othervm -Xbootclasspath/a:boot -Djava.security.manager=allow -Djava.system.class.loader=CustomSystemClassLoader -Djdk.logger.finder.singleton=true LoggerFinderLoaderTest WITHPERMISSIONS
 * @run  main/othervm -Xbootclasspath/a:boot -Djava.system.class.loader=CustomSystemClassLoader -Djdk.logger.finder.singleton=true -Djdk.logger.finder.error=ERROR LoggerFinderLoaderTest NOSECURITY
 * @run  main/othervm -Xbootclasspath/a:boot -Djava.security.manager=allow -Djava.system.class.loader=CustomSystemClassLoader -Djdk.logger.finder.singleton=true -Djdk.logger.finder.error=ERROR LoggerFinderLoaderTest NOPERMISSIONS
 * @run  main/othervm -Xbootclasspath/a:boot -Djava.security.manager=allow -Djava.system.class.loader=CustomSystemClassLoader -Djdk.logger.finder.singleton=true -Djdk.logger.finder.error=ERROR LoggerFinderLoaderTest WITHPERMISSIONS
 * @run  main/othervm -Xbootclasspath/a:boot -Djava.system.class.loader=CustomSystemClassLoader -Djdk.logger.finder.singleton=true -Djdk.logger.finder.error=DEBUG LoggerFinderLoaderTest NOSECURITY
 * @run  main/othervm -Xbootclasspath/a:boot -Djava.security.manager=allow -Djava.system.class.loader=CustomSystemClassLoader -Djdk.logger.finder.singleton=true -Djdk.logger.finder.error=DEBUG LoggerFinderLoaderTest NOPERMISSIONS
 * @run  main/othervm -Xbootclasspath/a:boot -Djava.security.manager=allow -Djava.system.class.loader=CustomSystemClassLoader -Djdk.logger.finder.singleton=true -Djdk.logger.finder.error=DEBUG LoggerFinderLoaderTest WITHPERMISSIONS
 * @run  main/othervm -Xbootclasspath/a:boot -Djava.system.class.loader=CustomSystemClassLoader -Djdk.logger.finder.singleton=true -Djdk.logger.finder.error=QUIET LoggerFinderLoaderTest NOSECURITY
 * @run  main/othervm -Xbootclasspath/a:boot -Djava.security.manager=allow -Djava.system.class.loader=CustomSystemClassLoader -Djdk.logger.finder.singleton=true -Djdk.logger.finder.error=QUIET LoggerFinderLoaderTest NOPERMISSIONS
 * @run  main/othervm -Xbootclasspath/a:boot -Djava.security.manager=allow -Djava.system.class.loader=CustomSystemClassLoader -Djdk.logger.finder.singleton=true -Djdk.logger.finder.error=QUIET LoggerFinderLoaderTest WITHPERMISSIONS
 * @author danielfuchs
 */
public class LoggerFinderLoaderTest {

    static final Policy DEFAULT_POLICY = Policy.getPolicy();
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
    static final Class<?>[] providerClass;
    static {
        try {
            providerClass = new Class<?>[] {
                ClassLoader.getSystemClassLoader().loadClass("BaseLoggerFinder"),
                ClassLoader.getSystemClassLoader().loadClass("BaseLoggerFinder2")
            };
        } catch (ClassNotFoundException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    /**
     * What our test provider needs to implement.
     */
    public static interface TestLoggerFinder {
        public final static AtomicBoolean fails = new AtomicBoolean();
        public final static AtomicReference<String> conf = new AtomicReference<>("");
        public final static AtomicLong sequencer = new AtomicLong();
        public final ConcurrentHashMap<String, LoggerImpl> system = new ConcurrentHashMap<>();
        public final ConcurrentHashMap<String, LoggerImpl> user = new ConcurrentHashMap<>();

        public class LoggerImpl implements System.Logger {
            final String name;
            final Logger logger;

            public LoggerImpl(String name, Logger logger) {
                this.name = name;
                this.logger = logger;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public boolean isLoggable(Logger.Level level) {
                return logger.isLoggable(level);
            }

            @Override
            public void log(Logger.Level level, ResourceBundle bundle, String key, Throwable thrown) {
                logger.log(level, bundle, key, thrown);
            }

            @Override
            public void log(Logger.Level level, ResourceBundle bundle, String format, Object... params) {
                logger.log(level, bundle, format, params);
            }

        }

        public Logger getLogger(String name, Module caller);
        public Logger getLocalizedLogger(String name, ResourceBundle bundle, Module caller);
    }

    public static class MyBundle extends ResourceBundle {

        final ConcurrentHashMap<String,String> map = new ConcurrentHashMap<>();

        @Override
        protected Object handleGetObject(String key) {
            if (key.contains(" (translated)")) {
                throw new RuntimeException("Unexpected key: " + key);
            }
            return map.computeIfAbsent(key, k -> k.toUpperCase(Locale.ROOT) + " (translated)");
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

    private static String withoutWarning(String in) {
        return in.lines().filter(s -> !s.startsWith("WARNING:")).collect(Collectors.joining());
    }

    static LoggerFinder getLoggerFinder(Class<?> expectedClass,
            String errorPolicy, boolean singleton) {
        LoggerFinder provider = null;
        try {
            TestLoggerFinder.sequencer.incrementAndGet();
            provider = LoggerFinder.getLoggerFinder();
            if (TestLoggerFinder.fails.get() || singleton) {
                if ("ERROR".equals(errorPolicy.toUpperCase(Locale.ROOT))) {
                    throw new RuntimeException("Expected exception not thrown");
                } else if ("WARNING".equals(errorPolicy.toUpperCase(Locale.ROOT))) {
                    String warning = ErrorStream.errorStream.peek();
                    if (!warning.contains("WARNING: Failed to instantiate LoggerFinder provider; Using default.")) {
                        throw new RuntimeException("Expected message not found. Error stream contained: " + warning);
                    }
                } else if ("DEBUG".equals(errorPolicy.toUpperCase(Locale.ROOT))) {
                    String warning = ErrorStream.errorStream.peek();
                    if (!warning.contains("WARNING: Failed to instantiate LoggerFinder provider; Using default.")) {
                        throw new RuntimeException("Expected message not found. Error stream contained: " + warning);
                    }
                    if (!warning.contains("WARNING: Exception raised trying to instantiate LoggerFinder")) {
                        throw new RuntimeException("Expected message not found. Error stream contained: " + warning);
                    }
                    if (TestLoggerFinder.fails.get()) {
                        if (!warning.contains("java.util.ServiceConfigurationError: java.lang.System$LoggerFinder: Provider BaseLoggerFinder could not be instantiated")) {
                            throw new RuntimeException("Expected message not found. Error stream contained: " + warning);
                        }
                    } else if (singleton) {
                        if (!warning.contains("java.util.ServiceConfigurationError: More than on LoggerFinder implementation")) {
                            throw new RuntimeException("Expected message not found. Error stream contained: " + warning);
                        }
                    }
                } else if ("QUIET".equals(errorPolicy.toUpperCase(Locale.ROOT))) {
                    String warning = ErrorStream.errorStream.peek();
                    warning = withoutWarning(warning);
                    if (!warning.isEmpty()) {
                        throw new RuntimeException("Unexpected error message found: "
                                + ErrorStream.errorStream.peek());
                    }
                }
            }
        } catch(AccessControlException a) {
            throw a;
        } catch(Throwable t) {
            if (TestLoggerFinder.fails.get() || singleton) {
                // must check System.err
                if ("ERROR".equals(errorPolicy.toUpperCase(Locale.ROOT))) {
                    provider = LoggerFinder.getLoggerFinder();
                } else {
                    Throwable orig = t.getCause();
                    while (orig != null && orig.getCause() != null) orig = orig.getCause();
                    if (orig != null) orig.printStackTrace(ErrorStream.err);
                    throw new RuntimeException("Unexpected exception: " + t, t);
                }
            } else {
                throw new RuntimeException("Unexpected exception: " + t, t);
            }
        }
        expectedClass.cast(provider);
        ErrorStream.errorStream.store();
        System.out.println("*** Actual LoggerFinder class is: " + provider.getClass().getName());
        return provider;
    }


    static class ErrorStream extends PrintStream {

        static AtomicBoolean forward = new AtomicBoolean();
        ByteArrayOutputStream out;
        String saved = "";
        public ErrorStream(ByteArrayOutputStream out) {
            super(out);
            this.out = out;
        }

        @Override
        public void write(int b) {
            super.write(b);
            if (forward.get()) err.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            super.write(b);
            if (forward.get()) err.write(b);
        }

        @Override
        public void write(byte[] buf, int off, int len) {
            super.write(buf, off, len);
            if (forward.get()) err.write(buf, off, len);
        }

        public String peek() {
            flush();
            return out.toString();
        }

        public String drain() {
            flush();
            String res = out.toString();
            out.reset();
            return res;
        }

        public void store() {
            flush();
            saved = out.toString();
            out.reset();
        }

        public void restore() {
            out.reset();
            try {
                out.write(saved.getBytes());
            } catch(IOException io) {
                throw new UncheckedIOException(io);
            }
        }

        static final PrintStream err = System.err;
        static final ErrorStream errorStream = new ErrorStream(new ByteArrayOutputStream());
    }

    private static StringBuilder appendProperty(StringBuilder b, String name) {
        String value = System.getProperty(name);
        if (value == null) return b;
        return b.append(name).append("=").append(value).append('\n');
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[] {
                "NOSECURITY",
                "NOPERMISSIONS",
                "WITHPERMISSIONS"
            };
        }
        Locale.setDefault(Locale.ENGLISH);
        System.setErr(ErrorStream.errorStream);
        System.setProperty("jdk.logger.packages", TestLoggerFinder.LoggerImpl.class.getName());
        //System.setProperty("jdk.logger.finder.error", "ERROR");
        //System.setProperty("jdk.logger.finder.singleton", "true");
        //System.setProperty("test.fails", "true");
        TestLoggerFinder.fails.set(Boolean.getBoolean("test.fails"));
        StringBuilder c = new StringBuilder();
        appendProperty(c, "jdk.logger.packages");
        appendProperty(c, "jdk.logger.finder.error");
        appendProperty(c, "jdk.logger.finder.singleton");
        appendProperty(c, "test.fails");
        TestLoggerFinder.conf.set(c.toString());
        try {
            test(args);
        } finally {
            try {
                System.setErr(ErrorStream.err);
            } catch (Error | RuntimeException x) {
                x.printStackTrace(ErrorStream.err);
            }
        }
    }


    public static void test(String[] args) {

        final String errorPolicy =  System.getProperty("jdk.logger.finder.error", "WARNING");
        final Boolean ensureSingleton = Boolean.getBoolean("jdk.logger.finder.singleton");

        final Class<?> expectedClass =
                TestLoggerFinder.fails.get() || ensureSingleton
                ? jdk.internal.logger.DefaultLoggerFinder.class
                : TestLoggerFinder.class;

        System.out.println("Declared provider class: " + providerClass[0]
                + "[" + providerClass[0].getClassLoader() + "]");

        if (!TestLoggerFinder.fails.get()) {
            ServiceLoader<LoggerFinder> serviceLoader =
                ServiceLoader.load(LoggerFinder.class, ClassLoader.getSystemClassLoader());
            Iterator<LoggerFinder> iterator = serviceLoader.iterator();
            Object firstProvider = iterator.next();
            if (!firstProvider.getClass().getName().equals("BaseLoggerFinder")) {
                throw new RuntimeException("Unexpected provider: " + firstProvider.getClass().getName());
            }
            if (!iterator.hasNext()) {
                throw new RuntimeException("Expected two providers");
            }
        }

        Stream.of(args).map(TestCases::valueOf).forEach((testCase) -> {
            LoggerFinder provider;
            ErrorStream.errorStream.restore();
            switch (testCase) {
                case NOSECURITY:
                    System.out.println("\n*** Without Security Manager\n");
                    System.out.println(TestLoggerFinder.conf.get());
                    provider = getLoggerFinder(expectedClass, errorPolicy, ensureSingleton);
                    test(provider, true);
                    System.out.println("Tetscase count: " + TestLoggerFinder.sequencer.get());
                    break;
                case NOPERMISSIONS:
                    System.out.println("\n*** With Security Manager, without permissions\n");
                    System.out.println(TestLoggerFinder.conf.get());
                    setSecurityManager();
                    try {
                        provider = getLoggerFinder(expectedClass, errorPolicy, ensureSingleton);
                        throw new RuntimeException("Expected exception not raised");
                    } catch (AccessControlException x) {
                        if (!LOGGERFINDER_PERMISSION.equals(x.getPermission())) {
                            throw new RuntimeException("Unexpected permission check", x);
                        }
                        final boolean control = allowControl.get().get();
                        try {
                            allowControl.get().set(true);
                            provider = getLoggerFinder(expectedClass, errorPolicy, ensureSingleton);
                        } finally {
                            allowControl.get().set(control);
                        }
                    }
                    test(provider, false);
                    System.out.println("Tetscase count: " + TestLoggerFinder.sequencer.get());
                    break;
                case WITHPERMISSIONS:
                    System.out.println("\n*** With Security Manager, with control permission\n");
                    System.out.println(TestLoggerFinder.conf.get());
                    setSecurityManager();
                    final boolean control = allowControl.get().get();
                    try {
                        allowControl.get().set(true);
                        provider = getLoggerFinder(expectedClass, errorPolicy, ensureSingleton);
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

    public static void test(LoggerFinder provider, boolean hasRequiredPermissions) {

        ResourceBundle loggerBundle = ResourceBundle.getBundle(MyLoggerBundle.class.getName());
        final Map<Logger, String> loggerDescMap = new HashMap<>();

        System.Logger sysLogger = accessSystemLogger.getLogger("foo");
        loggerDescMap.put(sysLogger, "accessSystemLogger.getLogger(\"foo\")");
        System.Logger localizedSysLogger = accessSystemLogger.getLogger("fox", loggerBundle);
        loggerDescMap.put(localizedSysLogger, "accessSystemLogger.getLogger(\"fox\", loggerBundle)");
        System.Logger appLogger = System.getLogger("bar");
        loggerDescMap.put(appLogger,"System.getLogger(\"bar\")");
        System.Logger localizedAppLogger = System.getLogger("baz", loggerBundle);
        loggerDescMap.put(localizedAppLogger,"System.getLogger(\"baz\", loggerBundle)");

        testLogger(provider, loggerDescMap, "foo", null, sysLogger);
        testLogger(provider, loggerDescMap, "foo", loggerBundle, localizedSysLogger);
        testLogger(provider, loggerDescMap, "foo", null, appLogger);
        testLogger(provider, loggerDescMap, "foo", loggerBundle, localizedAppLogger);
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
    private static void testLogger(LoggerFinder provider,
            Map<Logger, String> loggerDescMap,
            String name,
            ResourceBundle loggerBundle,
            Logger logger) {

        System.out.println("Testing " + loggerDescMap.get(logger) + " [" + logger +"]");
        AtomicLong sequencer = TestLoggerFinder.sequencer;

        Foo foo = new Foo();
        String fooMsg = foo.toString();
        for (Level loggerLevel : EnumSet.of(Level.INFO)) {
            for (Level messageLevel : Level.values()) {
                ErrorStream.errorStream.drain();
                String desc = "logger.log(messageLevel, foo): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                sequencer.incrementAndGet();
                logger.log(messageLevel, foo);
                if (loggerLevel == Level.OFF || messageLevel == Level.OFF || messageLevel.compareTo(loggerLevel) < 0) {
                    if (!ErrorStream.errorStream.peek().isEmpty()) {
                        throw new RuntimeException("unexpected event in queue for "
                                + desc +": " + "\n\t" + ErrorStream.errorStream.drain());
                    }
                } else {
                    String logged = ErrorStream.errorStream.drain();
                    if (!logged.contains("LoggerFinderLoaderTest testLogger")
                        || !logged.contains(messageLevel.getName() + ": " + fooMsg)) {
                        throw new RuntimeException("mismatch for " + desc
                                + "\n\texpected:" + "\n<<<<\n"
                                + "[date] LoggerFinderLoaderTest testLogger\n"
                                + messageLevel.getName() + " " + fooMsg
                                + "\n>>>>"
                                + "\n\t  actual:"
                                + "\n<<<<\n" + logged + ">>>>\n");
                    } else {
                        verbose("Got expected results for "
                                + desc + "\n<<<<\n" + logged + ">>>>\n");
                    }
                }
            }
        }

        String msg = "blah";
        for (Level loggerLevel : EnumSet.of(Level.INFO)) {
            for (Level messageLevel : Level.values()) {
                String desc = "logger.log(messageLevel, \"blah\"): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                sequencer.incrementAndGet();
                logger.log(messageLevel, msg);
                if (loggerLevel == Level.OFF || messageLevel == Level.OFF || messageLevel.compareTo(loggerLevel) < 0) {
                    if (!ErrorStream.errorStream.peek().isEmpty()) {
                        throw new RuntimeException("unexpected event in queue for "
                                + desc +": " + "\n\t" + ErrorStream.errorStream.drain());
                    }
                } else {
                    String logged = ErrorStream.errorStream.drain();
                    String msgText = loggerBundle == null ? msg : loggerBundle.getString(msg);
                    if (!logged.contains("LoggerFinderLoaderTest testLogger")
                        || !logged.contains(messageLevel.getName() + ": " + msgText)) {
                        throw new RuntimeException("mismatch for " + desc
                                + "\n\texpected:" + "\n<<<<\n"
                                + "[date] LoggerFinderLoaderTest testLogger\n"
                                + messageLevel.getName() + " " + msgText
                                + "\n>>>>"
                                + "\n\t  actual:"
                                + "\n<<<<\n" + logged + ">>>>\n");
                    } else {
                        verbose("Got expected results for "
                                + desc + "\n<<<<\n" + logged + ">>>>\n");
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

        for (Level loggerLevel : EnumSet.of(Level.INFO)) {
            for (Level messageLevel : Level.values()) {
                String desc = "logger.log(messageLevel, fooSupplier): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                sequencer.incrementAndGet();
                logger.log(messageLevel, fooSupplier);
                if (loggerLevel == Level.OFF || messageLevel == Level.OFF || messageLevel.compareTo(loggerLevel) < 0) {
                    if (!ErrorStream.errorStream.peek().isEmpty()) {
                        throw new RuntimeException("unexpected event in queue for "
                                + desc +": " + "\n\t" + ErrorStream.errorStream.drain());
                    }
                } else {
                    String logged = ErrorStream.errorStream.drain();
                    if (!logged.contains("LoggerFinderLoaderTest testLogger")
                        || !logged.contains(messageLevel.getName() + ": " + fooSupplier.get())) {
                        throw new RuntimeException("mismatch for " + desc
                                + "\n\texpected:" + "\n<<<<\n"
                                + "[date] LoggerFinderLoaderTest testLogger\n"
                                + messageLevel.getName() + " " + fooSupplier.get()
                                + "\n>>>>"
                                + "\n\t  actual:"
                                + "\n<<<<\n" + logged + ">>>>\n");
                    } else {
                        verbose("Got expected results for "
                                + desc + "\n<<<<\n" + logged + ">>>>\n");
                    }
                }
            }
        }


        String format = "two params [{1} {2}]";
        Object arg1 = foo;
        Object arg2 = msg;
        for (Level loggerLevel : EnumSet.of(Level.INFO)) {
            for (Level messageLevel : Level.values()) {
                String desc = "logger.log(messageLevel, format, params...): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                sequencer.incrementAndGet();
                logger.log(messageLevel, format, foo, msg);
                if (loggerLevel == Level.OFF || messageLevel == Level.OFF || messageLevel.compareTo(loggerLevel) < 0) {
                    if (!ErrorStream.errorStream.peek().isEmpty()) {
                        throw new RuntimeException("unexpected event in queue for "
                                + desc +": " + "\n\t" + ErrorStream.errorStream.drain());
                    }
                } else {
                    String logged = ErrorStream.errorStream.drain();
                    String msgFormat = loggerBundle == null ? format : loggerBundle.getString(format);
                    String text = java.text.MessageFormat.format(msgFormat, foo, msg);
                    if (!logged.contains("LoggerFinderLoaderTest testLogger")
                        || !logged.contains(messageLevel.getName() + ": " + text)) {
                        throw new RuntimeException("mismatch for " + desc
                                + "\n\texpected:" + "\n<<<<\n"
                                + "[date] LoggerFinderLoaderTest testLogger\n"
                                + messageLevel.getName() + " " + text
                                + "\n>>>>"
                                + "\n\t  actual:"
                                + "\n<<<<\n" + logged + ">>>>\n");
                    } else {
                        verbose("Got expected results for "
                                + desc + "\n<<<<\n" + logged + ">>>>\n");
                    }
                }
            }
        }

        Throwable thrown = new Exception("OK: log me!");
        for (Level loggerLevel : EnumSet.of(Level.INFO)) {
            for (Level messageLevel : Level.values()) {
                String desc = "logger.log(messageLevel, \"blah\", thrown): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                sequencer.incrementAndGet();
                logger.log(messageLevel, msg, thrown);
                if (loggerLevel == Level.OFF || messageLevel == Level.OFF || messageLevel.compareTo(loggerLevel) < 0) {
                    if (!ErrorStream.errorStream.peek().isEmpty()) {
                        throw new RuntimeException("unexpected event in queue for "
                                + desc +": " + "\n\t" + ErrorStream.errorStream.drain());
                    }
                } else {
                    String logged = ErrorStream.errorStream.drain();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    thrown.printStackTrace(new PrintStream(baos));
                    String text = baos.toString();
                    String msgText = loggerBundle == null ? msg : loggerBundle.getString(msg);
                    if (!logged.contains("LoggerFinderLoaderTest testLogger")
                        || !logged.contains(messageLevel.getName() + ": " + msgText)
                        || !logged.contains(text)) {
                        throw new RuntimeException("mismatch for " + desc
                                + "\n\texpected:" + "\n<<<<\n"
                                + "[date] LoggerFinderLoaderTest testLogger\n"
                                + messageLevel.getName() + " " + msgText +"\n"
                                + text
                                + ">>>>"
                                + "\n\t  actual:"
                                + "\n<<<<\n" + logged + ">>>>\n");
                    } else {
                        verbose("Got expected results for "
                                + desc + "\n<<<<\n" + logged + ">>>>\n");
                    }
                }
            }
        }


        for (Level loggerLevel : EnumSet.of(Level.INFO)) {
            for (Level messageLevel : Level.values()) {
                String desc = "logger.log(messageLevel, thrown, fooSupplier): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                sequencer.incrementAndGet();
                logger.log(messageLevel, fooSupplier, thrown);
                if (loggerLevel == Level.OFF || messageLevel == Level.OFF || messageLevel.compareTo(loggerLevel) < 0) {
                    if (!ErrorStream.errorStream.peek().isEmpty()) {
                        throw new RuntimeException("unexpected event in queue for "
                                + desc +": " + "\n\t" + ErrorStream.errorStream.drain());
                    }
                } else {
                    String logged = ErrorStream.errorStream.drain();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    thrown.printStackTrace(new PrintStream(baos));
                    String text = baos.toString();
                    if (!logged.contains("LoggerFinderLoaderTest testLogger")
                        || !logged.contains(messageLevel.getName() + ": " + fooSupplier.get())
                        || !logged.contains(text)) {
                        throw new RuntimeException("mismatch for " + desc
                                + "\n\texpected:" + "\n<<<<\n"
                                + "[date] LoggerFinderLoaderTest testLogger\n"
                                + messageLevel.getName() + " " + fooSupplier.get() +"\n"
                                + text
                                + ">>>>"
                                + "\n\t  actual:"
                                + "\n<<<<\n" + logged + ">>>>\n");
                    } else {
                        verbose("Got expected results for "
                                + desc + "\n<<<<\n" + logged + ">>>>\n");
                    }
                }
            }
        }

        ResourceBundle bundle = ResourceBundle.getBundle(MyBundle.class.getName());
        for (Level loggerLevel : EnumSet.of(Level.INFO)) {
            for (Level messageLevel : Level.values()) {
                String desc = "logger.log(messageLevel, bundle, format, params...): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                sequencer.incrementAndGet();
                logger.log(messageLevel, bundle, format, foo, msg);
                if (loggerLevel == Level.OFF || messageLevel == Level.OFF || messageLevel.compareTo(loggerLevel) < 0) {
                    if (!ErrorStream.errorStream.peek().isEmpty()) {
                        throw new RuntimeException("unexpected event in queue for "
                                + desc +": " + "\n\t" + ErrorStream.errorStream.drain());
                    }
                } else {
                    String logged = ErrorStream.errorStream.drain();
                    String text = java.text.MessageFormat.format(bundle.getString(format), foo, msg);
                    if (!logged.contains("LoggerFinderLoaderTest testLogger")
                        || !logged.contains(messageLevel.getName() + ": " + text)) {
                        throw new RuntimeException("mismatch for " + desc
                                + "\n\texpected:" + "\n<<<<\n"
                                + "[date] LoggerFinderLoaderTest testLogger\n"
                                + messageLevel.getName() + " " + text
                                + "\n>>>>"
                                + "\n\t  actual:"
                                + "\n<<<<\n" + logged + ">>>>\n");
                    } else {
                        verbose("Got expected results for "
                                + desc + "\n<<<<\n" + logged + ">>>>\n");
                    }
                }
            }
        }

        for (Level loggerLevel : EnumSet.of(Level.INFO)) {
            for (Level messageLevel : Level.values()) {
                String desc = "logger.log(messageLevel, bundle, \"blah\", thrown): loggerLevel="
                        + loggerLevel+", messageLevel="+messageLevel;
                sequencer.incrementAndGet();
                logger.log(messageLevel, bundle, msg, thrown);
                if (loggerLevel == Level.OFF || messageLevel == Level.OFF || messageLevel.compareTo(loggerLevel) < 0) {
                    if (!ErrorStream.errorStream.peek().isEmpty()) {
                        throw new RuntimeException("unexpected event in queue for "
                                + desc +": " + "\n\t" + ErrorStream.errorStream.drain());
                    }
                } else {
                    String logged = ErrorStream.errorStream.drain();
                    String textMsg = bundle.getString(msg);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    thrown.printStackTrace(new PrintStream(baos));
                    String text = baos.toString();
                    if (!logged.contains("LoggerFinderLoaderTest testLogger")
                        || !logged.contains(messageLevel.getName() + ": " + textMsg)
                        || !logged.contains(text)) {
                        throw new RuntimeException("mismatch for " + desc
                                + "\n\texpected:" + "\n<<<<\n"
                                + "[date] LoggerFinderLoaderTest testLogger\n"
                                + messageLevel.getName() + " " + textMsg +"\n"
                                + text
                                + ">>>>"
                                + "\n\t  actual:"
                                + "\n<<<<\n" + logged + ">>>>\n");
                    } else {
                        verbose("Got expected results for "
                                + desc + "\n<<<<\n" + logged + ">>>>\n");
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
        final static RuntimePermission CONTROL = LOGGERFINDER_PERMISSION;
        final static RuntimePermission ACCESS = new RuntimePermission("accessClassInPackage.jdk.internal.logger");

        final Permissions permissions;
        final ThreadLocal<AtomicBoolean> allowControl;
        final ThreadLocal<AtomicBoolean> allowAccess;
        public SimplePolicy(ThreadLocal<AtomicBoolean> allowControl, ThreadLocal<AtomicBoolean> allowAccess) {
            this.allowControl = allowControl;
            this.allowAccess = allowAccess;
            permissions = new Permissions();
            permissions.add(new RuntimePermission("setIO"));
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
            return getPermissions().implies(permission) ||
                   DEFAULT_POLICY.implies(domain, permission);
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
