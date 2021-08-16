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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Properties;
import java.util.PropertyPermission;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.LoggingPermission;

/**
 * @test
 * @bug 8033661
 * @summary tests LogManager.updateConfiguration(Function) method
 * @run main/othervm SimpleUpdateConfigurationTest UNSECURE
 * @run main/othervm -Djava.security.manager=allow SimpleUpdateConfigurationTest SECURE
 * @author danielfuchs
 */
public class SimpleUpdateConfigurationTest {

    /**
     * We will test updateConfiguration in
     * two configurations:
     * UNSECURE: No security manager.
     * SECURE: With the security manager present - and the required
     *         permissions granted.
     */
    public static enum TestCase {
        UNSECURE, SECURE;
        public void execute(Runnable run) {
            System.out.println("Running test case: " + name());
            try {
               Configure.setUp(this);
               Configure.doPrivileged(run, SimplePolicy.allowControl);
            } finally {
               Configure.doPrivileged(() -> {
                   try {
                       setSystemProperty("java.util.logging.config.file", null);
                       LogManager.getLogManager().readConfiguration();
                       System.gc();
                   } catch (Exception x) {
                       throw new RuntimeException(x);
                   }
               }, SimplePolicy.allowAll);
            }
        }
    }

    public static class MyHandler extends Handler {
        static final AtomicLong seq = new AtomicLong();
        long count = seq.incrementAndGet();

        @Override
        public void publish(LogRecord record) {
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }

        @Override
        public String toString() {
            return super.toString() + "("+count+")";
        }

    }

    static String storePropertyToFile(String name, Properties props)
        throws Exception {
        return Configure.callPrivileged(() -> {
            String scratch = System.getProperty("user.dir", ".");
            Path p = Paths.get(scratch, name);
            try (FileOutputStream fos = new FileOutputStream(p.toFile())) {
                props.store(fos, name);
            }
            return p.toString();
        }, SimplePolicy.allowAll);
    }

    static void setSystemProperty(String name, String value)
        throws Exception {
        Configure.doPrivileged(() -> {
            if (value == null)
                System.clearProperty(name);
            else
                System.setProperty(name, value);
        }, SimplePolicy.allowAll);
    }

    static String trim(String value) {
        return value == null ? null : value.trim();
    }


    /**
     * Tests one of the configuration defined above.
     * <p>
     * This is the main test method (the rest is infrastructure).
     */
    static void testUpdateConfiguration() {
        String configFile = null;
        try {
            // manager initialized with default configuration.
            LogManager manager = LogManager.getLogManager();

            // Test default configuration. It should not have
            // any value for "com.foo.level" and "com.foo.handlers"
            assertEquals(null, manager.getProperty("com.foo.level"),
                "com.foo.level in default configuration");
            assertEquals(null, manager.getProperty("com.foo.handlers"),
                "com.foo.handlers in default configuration");

            // Create a logging configuration file that contains
            // com.foo.level=FINEST
            // and set "java.util.logging.config.file" to this file.
            Properties props = new Properties();
            props.setProperty("com.foo.level", "FINEST");
            configFile = storePropertyToFile("config1", props);
            setSystemProperty("java.util.logging.config.file", configFile);

            // Update configuration with configFile
            // then test that the new configuration has
            // com.foo.level=FINEST
            // and nothing for com.foo.handlers
            manager.updateConfiguration(null);
            assertEquals("FINEST", manager.getProperty("com.foo.level"),
                "com.foo.level in " + configFile);
            assertEquals(null, manager.getProperty("com.foo.handlers"),
                "com.foo.handlers in " + configFile);

            // clear ("java.util.logging.config.file" system property,
            // and call updateConfiguration again.
            // check that the new configuration no longer has
            // any value for com.foo.level, and still no value
            // for com.foo.handlers
            setSystemProperty("java.util.logging.config.file", null);
            manager.updateConfiguration(null);
            assertEquals(null, manager.getProperty("com.foo.level"),
                    "com.foo.level in default configuration");
            assertEquals(null, manager.getProperty("com.foo.handlers"),
                "com.foo.handlers in default configuration");

            // creates the com.foo logger, check it has
            // the default config: no level, and no handlers
            final Logger logger = Logger.getLogger("com.foo");
            assertEquals(null, logger.getLevel(),
                "Logger.getLogger(\"com.foo\").getLevel()");
            assertDeepEquals(new Handler[0], logger.getHandlers(),
                    "Logger.getLogger(\"com.foo\").getHandlers()");

            // set "java.util.logging.config.file" to configFile and
            // call updateConfiguration.
            // check that the configuration has
            // com.foo.level=FINEST
            // and nothing for com.foo.handlers
            // check that the logger has now a FINEST level and still
            // no handlers
            setSystemProperty("java.util.logging.config.file", configFile);
            manager.updateConfiguration(null);
            assertEquals("FINEST", manager.getProperty("com.foo.level"),
                "com.foo.level in " + configFile);
            assertEquals(Level.FINEST, logger.getLevel(),
                "Logger.getLogger(\"com.foo\").getLevel()");
            assertDeepEquals(new Handler[0], logger.getHandlers(),
                    "Logger.getLogger(\"com.foo\").getHandlers()");
            assertEquals(null, manager.getProperty("com.foo.handlers"),
                "com.foo.handlers in " + configFile);

            // Calls updateConfiguration with a lambda whose effect should
            // be to set the FINER level on the "com.foo" logger.
            // Check that the new configuration has
            // com.foo.level=FINER
            // and nothing for com.foo.handlers
            // check that the logger has now a FINER level and still
            // no handlers
            manager.updateConfiguration(
                    (k) -> ("com.foo.level".equals(k) ? (o, n) -> "FINER" : (o, n) -> n));
            assertEquals("FINER", manager.getProperty("com.foo.level"),
                "com.foo.level set to FINER by updateConfiguration");
            assertEquals(Level.FINER, logger.getLevel(),
                "Logger.getLogger(\"com.foo\").getLevel()");
            assertDeepEquals(new Handler[0], logger.getHandlers(),
                    "Logger.getLogger(\"com.foo\").getHandlers()");
            assertEquals(null, manager.getProperty("com.foo.handlers"),
                "com.foo.handlers in " + configFile);

            // Calls updateConfiguration with a lambda whose effect is a noop.
            // This should not change the configuration, so
            // check that the new configuration still has
            // com.foo.level=FINER
            // and nothing for com.foo.handlers
            // check that the logger still has FINER level and still
            // no handlers
            manager.updateConfiguration(
                    (k) -> ((o, n) -> o));
            assertEquals("FINER", manager.getProperty("com.foo.level"),
                "com.foo.level preserved by updateConfiguration");
            assertEquals(Level.FINER, logger.getLevel(),
                "Logger.getLogger(\"com.foo\").getLevel()");
            assertDeepEquals(new Handler[0], logger.getHandlers(),
                    "Logger.getLogger(\"com.foo\").getHandlers()");
            assertEquals(null, manager.getProperty("com.foo.handlers"),
                "com.foo.handlers in " + configFile);

            // Calls updateConfiguration with a lambda whose effect is to
            // take all values from the new configuration.
            // This should update the configuration to what is in configFile, so
            // check that the new configuration has
            // com.foo.level=FINEST
            // and nothing for com.foo.handlers
            // check that the logger now has FINEST level and still
            // no handlers
            manager.updateConfiguration(
                    (k) -> ((o, n) -> n));
            assertEquals("FINEST", manager.getProperty("com.foo.level"),
                "com.foo.level updated by updateConfiguration");
            assertEquals(Level.FINEST, logger.getLevel(),
                "Logger.getLogger(\"com.foo\").getLevel()");
            assertDeepEquals(new Handler[0], logger.getHandlers(),
                    "Logger.getLogger(\"com.foo\").getHandlers()");
            assertEquals(null, manager.getProperty("com.foo.handlers"),
                "com.foo.handlers in " + configFile);

            // now set a handler on the com.foo logger.
            MyHandler h = new MyHandler();
            logger.addHandler(h);
            assertDeepEquals(new Handler[] {h}, logger.getHandlers(),
                    "Logger.getLogger(\"com.foo\").getHandlers()");

            // Calls updateConfiguration with a lambda whose effect should
            // be to set the FINER level on the "com.foo" logger, and
            // take the value from configFile for everything else.
            // Check that the new configuration has
            // com.foo.level=FINER
            // and nothing for com.foo.handlers
            // check that the logger has now a FINER level, but that its
            // handlers are still present and have not been reset
            // since neither the old nor new configuration defined them.
            manager.updateConfiguration(
                    (k) -> ("com.foo.level".equals(k) ? (o, n) -> "FINER" : (o, n) -> n));
            assertEquals("FINER", manager.getProperty("com.foo.level"),
                "com.foo.level set to FINER by updateConfiguration");
            assertEquals(Level.FINER, logger.getLevel(),
                "Logger.getLogger(\"com.foo\").getLevel()");
            assertDeepEquals(new Handler[] {h}, logger.getHandlers(),
                    "Logger.getLogger(\"com.foo\").getHandlers()");
            assertEquals(null, manager.getProperty("com.foo.handlers"),
                "com.foo.handlers in " + configFile);

            // now add some configuration for com.foo.handlers in the
            // configuration file.
            props.setProperty("com.foo.handlers", MyHandler.class.getName());
            storePropertyToFile("config1", props);

            // we didn't call updateConfiguration, so just changing the
            // content of the file should have had no no effect yet.
            assertEquals("FINER", manager.getProperty("com.foo.level"),
                "com.foo.level set to FINER by updateConfiguration");
            assertEquals(Level.FINER, logger.getLevel(),
                "Logger.getLogger(\"com.foo\").getLevel()");
            assertEquals(null,
                    manager.getProperty("com.foo.handlers"),
                    "manager.getProperty(\"com.foo.handlers\")");
            assertDeepEquals(new Handler[] {h}, logger.getHandlers(),
                    "Logger.getLogger(\"com.foo\").getHandlers()");

            // Calls updateConfiguration with a lambda whose effect is a noop.
            // This should not change the current configuration, so
            // check that the new configuration still has
            // com.foo.level=FINER
            // and nothing for com.foo.handlers
            // check that the logger still has FINER level and still
            // has its handlers and that they haven't been reset.
            manager.updateConfiguration((k) -> ((o, n) -> o));
            assertEquals("FINER", manager.getProperty("com.foo.level"),
                "com.foo.level set to FINER by updateConfiguration");
            assertEquals(Level.FINER, logger.getLevel(),
                "Logger.getLogger(\"com.foo\").getLevel()");
            assertEquals(null,
                    manager.getProperty("com.foo.handlers"),
                    "manager.getProperty(\"com.foo.handlers\")");
            assertDeepEquals(new Handler[] {h}, logger.getHandlers(),
                    "Logger.getLogger(\"com.foo\").getHandlers()");

            // Calls updateConfiguration with a lambda whose effect is to
            // take all values from the new configuration.
            // This should update the configuration to what is in configFile, so
            // check that the new configuration has
            // com.foo.level=FINEST
            // com.foo.handlers=SimpleUpdateConfigurationTest$MyHandler
            // check that the logger now has FINEST level
            // and a new handler instance, since the old config
            // had no handlers for com.foo and the new config has one.
            manager.updateConfiguration((k) -> ((o, n) -> n));
            assertEquals("FINEST", manager.getProperty("com.foo.level"),
                "com.foo.level updated by updateConfiguration");
            assertEquals(Level.FINEST, logger.getLevel(),
                "Logger.getLogger(\"com.foo\").getLevel()");
            assertEquals(MyHandler.class.getName(),
                    manager.getProperty("com.foo.handlers"),
                    "manager.getProperty(\"com.foo.handlers\")");
            Handler[] loggerHandlers = logger.getHandlers().clone();
            assertEquals(1, loggerHandlers.length,
                    "Logger.getLogger(\"com.foo\").getHandlers().length");
            assertEquals(MyHandler.class, loggerHandlers[0].getClass(),
                    "Logger.getLogger(\"com.foo\").getHandlers()[0].getClass()");
            assertEquals(h.count + 1, ((MyHandler)logger.getHandlers()[0]).count,
                    "Logger.getLogger(\"com.foo\").getHandlers()[0].count");

            // Calls updateConfiguration with a lambda whose effect is a noop.
            // This should not change the current configuration, so
            // check that the new configuration still has
            // com.foo.level=FINEST
            // com.foo.handlers=SimpleUpdateConfigurationTest$MyHandler
            // check that the logger still has FINEST level and still
            // has its handlers and that they haven't been reset.
            manager.updateConfiguration((k) -> ((o, n) -> o));
            assertDeepEquals(loggerHandlers, logger.getHandlers(),
                    "Logger.getLogger(\"com.foo\").getHandlers()");
            assertEquals("FINEST", manager.getProperty("com.foo.level"),
                "com.foo.level updated by updateConfiguration");
            assertEquals(Level.FINEST, logger.getLevel(),
                "Logger.getLogger(\"com.foo\").getLevel()");
            assertEquals(MyHandler.class.getName(),
                    manager.getProperty("com.foo.handlers"),
                    "manager.getProperty(\"com.foo.handlers\")");

            // Calls updateConfiguration with a lambda whose effect is to
            // take all values from the new configuration.
            // Because the content of the configFile hasn't changed, then
            // it should also be a noop.
            // check that the new configuration still has
            // com.foo.level=FINEST
            // com.foo.handlers=SimpleUpdateConfigurationTest$MyHandler
            // check that the logger still has FINEST level and still
            // has its handlers and that they haven't been reset.
            manager.updateConfiguration((k) -> ((o, n) -> n));
            assertDeepEquals(loggerHandlers, logger.getHandlers(),
                    "Logger.getLogger(\"com.foo\").getHandlers()");
            assertEquals("FINEST", manager.getProperty("com.foo.level"),
                "com.foo.level updated by updateConfiguration");
            assertEquals(Level.FINEST, logger.getLevel(),
                "Logger.getLogger(\"com.foo\").getLevel()");
            assertEquals(MyHandler.class.getName(),
                    manager.getProperty("com.foo.handlers"),
                    "manager.getProperty(\"com.foo.handlers\")");

            // Calls updateConfiguration with a null lambda, whose effect is to
            // take all values from the new configuration.
            // Because the content of the configFile hasn't changed, then
            // it should also be a noop.
            // check that the new configuration still has
            // com.foo.level=FINEST
            // com.foo.handlers=SimpleUpdateConfigurationTest$MyHandler
            // check that the logger still has FINEST level and still
            // has its handlers and that they haven't been reset.
            manager.updateConfiguration((k) -> ((o, n) -> n));
            assertDeepEquals(loggerHandlers, logger.getHandlers(),
                    "Logger.getLogger(\"com.foo\").getHandlers()");
            assertEquals("FINEST", manager.getProperty("com.foo.level"),
                "com.foo.level updated by updateConfiguration");
            assertEquals(Level.FINEST, logger.getLevel(),
                "Logger.getLogger(\"com.foo\").getLevel()");
            assertEquals(MyHandler.class.getName(),
                    manager.getProperty("com.foo.handlers"),
                    "manager.getProperty(\"com.foo.handlers\")");

            // no remove com.foo.handlers=SimpleUpdateConfigurationTest$MyHandler
            // from the configuration file.
            props.remove("com.foo.handlers");
            storePropertyToFile("config1", props);

            // Calls updateConfiguration with a lambda whose effect is a noop.
            // This should not change the current configuration, so
            // check that the new configuration still has
            // com.foo.level=FINEST
            // com.foo.handlers=SimpleUpdateConfigurationTest$MyHandler
            // check that the logger still has FINEST level and still
            // has its handlers and that they haven't been reset.
            manager.updateConfiguration((k) -> ((o, n) -> o));
            assertDeepEquals(loggerHandlers, logger.getHandlers(),
                    "Logger.getLogger(\"com.foo\").getHandlers()");
            assertEquals("FINEST", manager.getProperty("com.foo.level"),
                "com.foo.level updated by updateConfiguration");
            assertEquals(Level.FINEST, logger.getLevel(),
                "Logger.getLogger(\"com.foo\").getLevel()");
            assertEquals(MyHandler.class.getName(),
                    manager.getProperty("com.foo.handlers"),
                    "manager.getProperty(\"com.foo.handlers\")");

            // Calls updateConfiguration with a lambda whose effect is to
            // take all values from the new configuration.
            // This should update the configuration to what is in configFile, so
            // check that the new configuration has
            // com.foo.level=FINEST
            // and nothing for com.foo.handlers
            // check that the logger still has FINEST level
            // and no handlers, since the old config
            // had an handler for com.foo and the new config doesn't.
            manager.updateConfiguration((k) -> ((o, n) -> n));
            assertDeepEquals(new Handler[0], logger.getHandlers(),
                    "Logger.getLogger(\"com.foo\").getHandlers()");
            assertEquals("FINEST", manager.getProperty("com.foo.level"),
                "com.foo.level updated by updateConfiguration");
            assertEquals(Level.FINEST, logger.getLevel(),
                "Logger.getLogger(\"com.foo\").getLevel()");
            assertEquals(null,
                    manager.getProperty("com.foo.handlers"),
                    "manager.getProperty(\"com.foo.handlers\")");


        } catch (RuntimeException | Error r) {
            throw r;
        } catch (Exception x) {
            throw new RuntimeException(x);
        } finally {
            if (configFile != null) {
                // cleanup
                final String file = configFile;
                Configure.doPrivileged(() -> {
                    try {
                        Files.delete(Paths.get(file));
                    } catch (RuntimeException | Error r) {
                        throw r;
                    } catch (Exception x) {
                        throw new RuntimeException(x);
                    }
                }, SimplePolicy.allowAll);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args == null || args.length == 0) {
            args = new String[] { "UNSECURE", "SECURE" };
        }
        for (String test : args) {
            TestCase.valueOf(test).execute(SimpleUpdateConfigurationTest::testUpdateConfiguration);
        }
    }

    static class Configure {
        static Policy policy = null;
        static void setUp(TestCase test) {
            switch (test) {
                case SECURE:
                    if (policy == null && System.getSecurityManager() != null) {
                        throw new IllegalStateException("SecurityManager already set");
                    } else if (policy == null) {
                        policy = new SimplePolicy(TestCase.SECURE);
                        Policy.setPolicy(policy);
                        System.setSecurityManager(new SecurityManager());
                    }
                    if (System.getSecurityManager() == null) {
                        throw new IllegalStateException("No SecurityManager.");
                    }
                    if (policy == null) {
                        throw new IllegalStateException("policy not configured");
                    }
                    break;
                case UNSECURE:
                    if (System.getSecurityManager() != null) {
                        throw new IllegalStateException("SecurityManager already set");
                    }
                    break;
                default:
                    throw new InternalError("No such testcase: " + test);
            }
        }

        static void updateConfigurationWith(Properties propertyFile,
                Function<String,BiFunction<String,String,String>> remapper) {
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                propertyFile.store(bytes, propertyFile.getProperty("test.name"));
                ByteArrayInputStream bais = new ByteArrayInputStream(bytes.toByteArray());
                LogManager.getLogManager().updateConfiguration(bais, remapper);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        static void doPrivileged(Runnable run, ThreadLocal<AtomicBoolean> granter) {
            final boolean old = granter.get().getAndSet(true);
            try {
                run.run();
            } finally {
                granter.get().set(old);
            }
        }
        static <T> T callPrivileged(Callable<T> call,
                ThreadLocal<AtomicBoolean> granter) throws Exception {
            final boolean old = granter.get().getAndSet(true);
            try {
                return call.call();
            } finally {
                granter.get().set(old);
            }
        }
    }

    static final class TestAssertException extends RuntimeException {
        TestAssertException(String msg) {
            super(msg);
        }
    }

    private static void assertEquals(long expected, long received, String msg) {
        if (expected != received) {
            throw new TestAssertException("Unexpected result for " + msg
                    + ".\n\texpected: " + expected
                    +  "\n\tactual:   " + received);
        } else {
            System.out.println("Got expected " + msg + ": " + received);
        }
    }

    private static void assertEquals(String expected, String received, String msg) {
        if (!Objects.equals(expected, received)) {
            throw new TestAssertException("Unexpected result for " + msg
                    + ".\n\texpected: " + expected
                    +  "\n\tactual:   " + received);
        } else {
            System.out.println("Got expected " + msg + ": " + received);
        }
    }

    private static void assertEquals(Object expected, Object received, String msg) {
        if (!Objects.equals(expected, received)) {
            throw new TestAssertException("Unexpected result for " + msg
                    + ".\n\texpected: " + expected
                    +  "\n\tactual:   " + received);
        } else {
            System.out.println("Got expected " + msg + ": " + received);
        }
    }

    public static String deepToString(Object o) {
        if (o == null) {
            return "null";
        } else if (o.getClass().isArray()) {
            String s;
            if (o instanceof Object[])
                s = Arrays.deepToString((Object[]) o);
            else if (o instanceof byte[])
                s = Arrays.toString((byte[]) o);
            else if (o instanceof short[])
                s = Arrays.toString((short[]) o);
            else if (o instanceof int[])
                s = Arrays.toString((int[]) o);
            else if (o instanceof long[])
                s = Arrays.toString((long[]) o);
            else if (o instanceof char[])
                s = Arrays.toString((char[]) o);
            else if (o instanceof float[])
                s = Arrays.toString((float[]) o);
            else if (o instanceof double[])
                s = Arrays.toString((double[]) o);
            else if (o instanceof boolean[])
                s = Arrays.toString((boolean[]) o);
            else
                s = o.toString();
            return s;
        } else {
            return o.toString();
        }
    }

    private static void assertDeepEquals(Object expected, Object received, String msg) {
        if (!Objects.deepEquals(expected, received)) {
            throw new TestAssertException("Unexpected result for " + msg
                    + ".\n\texpected: " + deepToString(expected)
                    +  "\n\tactual:   " + deepToString(received));
        } else {
            System.out.println("Got expected " + msg + ": " + deepToString(received));
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

        final Permissions basic;
        final Permissions control;
        final Permissions all;
        public final static ThreadLocal<AtomicBoolean> allowAll =
                new ThreadLocal<AtomicBoolean>() {
            @Override
            protected AtomicBoolean initialValue() {
                return new AtomicBoolean();
            }
        };
        public final static ThreadLocal<AtomicBoolean> allowControl =
                new ThreadLocal<AtomicBoolean>() {
            @Override
            protected AtomicBoolean initialValue() {
                return new AtomicBoolean();
            }
        };
        public SimplePolicy(TestCase test) {
            basic = new Permissions();
            control = new Permissions();
            control.add(new LoggingPermission("control", null));

            // These permissions are required to call updateConfiguration(Function)
            control.add(new PropertyPermission("java.util.logging.config.file", "read"));
            control.add(new PropertyPermission("java.home", "read"));
            control.add(new FilePermission(
                    Paths.get(System.getProperty("user.dir", "."),"-").toString(), "read"));
            control.add(new FilePermission(
                    Paths.get(System.getProperty("java.home"),"conf","-").toString(), "read"));

            // these are used for configuring the test itself...
            all = new Permissions();
            all.add(new java.security.AllPermission());

        }

        @Override
        public boolean implies(ProtectionDomain domain, Permission permission) {
            return getPermissions(domain).implies(permission) || DEFAULT_POLICY.implies(domain, permission);
        }

        public PermissionCollection permissions() {
            PermissionsBuilder builder = new PermissionsBuilder();
            if (allowAll.get().get()) {
                builder.addAll(all);
            } else {
                builder.addAll(basic);
                if (allowControl.get().get()) {
                    builder.addAll(control);
                }
            }
            return builder.toPermissions();
        }

        @Override
        public PermissionCollection getPermissions(CodeSource codesource) {
            return permissions();
        }

        @Override
        public PermissionCollection getPermissions(ProtectionDomain domain) {
            return permissions();
        }
    }

}
