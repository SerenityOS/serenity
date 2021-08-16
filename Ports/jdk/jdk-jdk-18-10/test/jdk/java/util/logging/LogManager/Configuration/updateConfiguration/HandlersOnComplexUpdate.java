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
import java.io.FilePermission;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.FileHandler;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.LoggingPermission;

/**
 * @test
 * @bug 8033661
 * @summary tests that FileHandlers configured on abstract nodes in logging.properties
 *          will be properly closed and reopened on updateConfiguration().
 *          Test a complex reconfiguration where a logger with handlers
 *          suddenly appears in the hierarchy between a child logger and the
 *          root logger.
 * @run main/othervm HandlersOnComplexUpdate UNSECURE
 * @run main/othervm -Djava.security.manager=allow HandlersOnComplexUpdate SECURE
 * @author danielfuchs
 */
public class HandlersOnComplexUpdate {

    /**
     * We will test the handling of abstract logger nodes with file handlers in
     * two configurations:
     * UNSECURE: No security manager.
     * SECURE: With the security manager present - and the required
     *         permissions granted.
     */
    public static enum TestCase {
        UNSECURE, SECURE;
        public void run(List<Properties> properties) throws Exception {
            System.out.println("Running test case: " + name());
            Configure.setUp(this, properties.get(0));
            test(this.name(), properties);
        }
    }

    public static final double TIMEOUT_FACTOR;
    static {
        String toFactor = System.getProperty("test.timeout.factor", "1.0");
        TIMEOUT_FACTOR = Double.parseDouble(toFactor);
    }
    static int adjustCount(int count) {
        return (int) Math.ceil(TIMEOUT_FACTOR * count);
    }

    private static final String PREFIX =
            "FileHandler-" + UUID.randomUUID() + ".log";
    private static final String userDir = System.getProperty("user.dir", ".");
    private static final boolean userDirWritable = Files.isWritable(Paths.get(userDir));

    private static final List<Properties> properties;
    static {
        // The test will call updateConfiguration() with each of these
        // properties in sequence. The child logger is not released between each
        // configuration. What is interesting here is mostly what happens between
        // props4 and props5:
        //
        // In step 4 (props4) the configuration defines a handler for the
        // logger com.foo (the direct parent of com.foo.child - which is the
        // logger we hold on to).
        //
        // In step 5 (props5) the configuration has nothing concerning
        // 'com.foo', but the handler has been migrated to 'com'.
        // Since there doesn't exist any logger for 'com' (the previous
        // configuration didn't have any configuration for 'com'), then
        // 'com' will not be found when we process the existing loggers named
        // in the configuration.
        //
        // So if we didn't also process the existing loggers not named in the
        // configuration (such as com.foo.child) then no logger for 'com'
        // would be created, which means that com.foo.child would not be
        // able to inherit its configuration for 'com' until someone explicitely
        // creates a logger for 'com'.
        //
        // This test check that a logger for 'com' will be created because
        // 'com.foo.child' still exists when updateConfiguration() is called.

        Properties props1 = new Properties();
        props1.setProperty("test.name", "parent logger with handler");
        props1.setProperty(FileHandler.class.getName() + ".pattern", PREFIX);
        props1.setProperty(FileHandler.class.getName() + ".limit", String.valueOf(Integer.MAX_VALUE));
        props1.setProperty(FileHandler.class.getName() + ".level", "ALL");
        props1.setProperty(FileHandler.class.getName() + ".formatter", "java.util.logging.SimpleFormatter");
        props1.setProperty("com.foo.handlers", FileHandler.class.getName());
        props1.setProperty("test.checkHandlersOnParent", "true");
        props1.setProperty("test.checkHandlersOn", "com.foo");
        props1.setProperty("com.bar.level", "FINEST");

        Properties props2 = new Properties();
        props2.setProperty("test.name", "parent logger with handler");
        props2.setProperty(FileHandler.class.getName() + ".pattern", PREFIX);
        props2.setProperty(FileHandler.class.getName() + ".limit", String.valueOf(Integer.MAX_VALUE));
        props2.setProperty(FileHandler.class.getName() + ".level", "ALL");
        props2.setProperty(FileHandler.class.getName() + ".formatter", "java.util.logging.SimpleFormatter");
        props2.setProperty("com.foo.handlers", FileHandler.class.getName());
        props2.setProperty("test.checkHandlersOnParent", "true");
        props2.setProperty("test.checkHandlersOn", "com.foo");
        props2.setProperty("com.bar.level", "FINEST");

        Properties props3 = new Properties();
        props3.setProperty("test.name", "parent logger with handler");
        props3.setProperty(FileHandler.class.getName() + ".pattern", PREFIX);
        props3.setProperty(FileHandler.class.getName() + ".limit", String.valueOf(Integer.MAX_VALUE));
        props3.setProperty(FileHandler.class.getName() + ".level", "ALL");
        props3.setProperty(FileHandler.class.getName() + ".formatter", "java.util.logging.SimpleFormatter");
        props3.setProperty("com.foo.handlers", FileHandler.class.getName());
        props3.setProperty("test.checkHandlersOnParent", "true");
        props3.setProperty("test.checkHandlersOn", "com.foo");
        props3.setProperty("com.bar.level", "FINEST");

        Properties props4 = new Properties();
        props4.setProperty("test.name", "parent logger with handler");
        props4.setProperty(FileHandler.class.getName() + ".pattern", PREFIX);
        props4.setProperty(FileHandler.class.getName() + ".limit", String.valueOf(Integer.MAX_VALUE));
        props4.setProperty(FileHandler.class.getName() + ".level", "ALL");
        props4.setProperty(FileHandler.class.getName() + ".formatter", "java.util.logging.SimpleFormatter");
        props4.setProperty("test.checkHandlersOnParent", "true");
        props4.setProperty("test.checkHandlersOn", "com.foo");
        props4.setProperty("com.foo.handlers", FileHandler.class.getName());

        Properties props5 = new Properties();
        props5.setProperty("test.name", "parent logger with handler");
        props5.setProperty(FileHandler.class.getName() + ".pattern", PREFIX);
        props5.setProperty(FileHandler.class.getName() + ".limit", String.valueOf(Integer.MAX_VALUE));
        props5.setProperty(FileHandler.class.getName() + ".level", "ALL");
        props5.setProperty(FileHandler.class.getName() + ".formatter", "java.util.logging.SimpleFormatter");
        props5.setProperty("test.checkHandlersOnParent", "false");
        props5.setProperty("test.checkHandlersOn", "com");
        props5.setProperty("com.handlers", FileHandler.class.getName());

        properties = Collections.unmodifiableList(Arrays.asList(
                    props1, props2, props3, props4, props5));
    }

    /**
     * This is the main test method. The rest is infrastructure.
     * Creates a child of the 'com.foo' logger (com.foo.child) and holds on to
     * it.
     * <p>
     * Then applies all given configurations in sequence and verifies assumptions
     * about the handlers that com.foo should have, or not have.
     * In the last configuration (props5) it also verifies that the
     * logger 'com' has been created and has now the expected handler.
     * <p>
     * Finally releases the child logger after all configurations have been
     * applied.
     *
     * @param name
     * @param properties
     * @throws Exception
     */
    static void test(String name, List<Properties> properties)
            throws Exception {

        System.out.println("\nTesting: " + name);
        if (!userDirWritable) {
            throw new RuntimeException("Not writable: "+userDir);
        }

        // Then create a child of the com.foo logger.
        Logger fooChild = Logger.getLogger("com.foo.child");
        fooChild.info("hello world");
        Logger barChild = Logger.getLogger("com.bar.child");
        barChild.info("hello world");

        ReferenceQueue<Logger> queue = new ReferenceQueue();
        WeakReference<Logger> fooRef = new WeakReference<>(Logger.getLogger("com.foo"), queue);
        if (fooRef.get() != fooChild.getParent()) {
            throw new RuntimeException("Unexpected parent logger: "
                    + fooChild.getParent() +"\n\texpected: " + fooRef.get());
        }
        WeakReference<Logger> barRef = new WeakReference<>(Logger.getLogger("com.bar"), queue);
        if (barRef.get() != barChild.getParent()) {
            throw new RuntimeException("Unexpected parent logger: "
                    + barChild.getParent() +"\n\texpected: " + barRef.get());
        }
        Reference<? extends Logger> ref2;
        int max = adjustCount(3);
        barChild = null;
        while ((ref2 = queue.poll()) == null) {
            System.gc();
            Thread.sleep(1000);
            if (--max == 0) break;
        }

        Throwable failed = null;
        try {
            if (ref2 != null) {
                String refName = ref2 == fooRef ? "fooRef" : ref2 == barRef ? "barRef" : "unknown";
                if (ref2 != barRef) {
                    throw new RuntimeException("Unexpected logger reference cleared: " + refName);
                } else {
                    System.out.println("Reference " + refName + " cleared as expected");
                }
            } else if (ref2 == null) {
                throw new RuntimeException("Expected 'barRef' to be cleared");
            }
            // Now lets try to check handlers, and
            // attempt to update the configuration again.
            Properties previousProps  = properties.get(0);
            int expectedHandlersCount = 1;
            boolean checkHandlersOnParent = Boolean.parseBoolean(
                    previousProps.getProperty("test.checkHandlersOnParent", "true"));
            String checkHandlersOn = previousProps.getProperty("test.checkHandlersOn", null);
            for (int i=1; i<properties.size(); i++) {
                System.out.println("\n*** Reconfiguration with properties["+i+"]\n");
                Properties nextProps = properties.get(i);
                boolean reconfigureHandlers = true;

                if (checkHandlersOnParent) {
                    assertEquals(expectedHandlersCount,
                            fooChild.getParent().getHandlers().length,
                            "fooChild.getParent().getHandlers().length");
                }
                if (checkHandlersOn != null) {
                    Logger loggerWithHandlers = LogManager.getLogManager().getLogger(checkHandlersOn);
                    if (loggerWithHandlers == null) {
                        throw new RuntimeException("Logger with handlers not found: " + checkHandlersOn);
                    }
                    assertEquals(expectedHandlersCount,
                            loggerWithHandlers.getHandlers().length,
                            checkHandlersOn + ".getHandlers().length");
                }

                if (i == 4) {
                    System.out.println("Last configuration...");
                }
                // Read configuration
                Configure.doPrivileged(() -> Configure.updateConfigurationWith(nextProps, false));

                expectedHandlersCount = reconfigureHandlers ? 1 : 0;
                checkHandlersOnParent = Boolean.parseBoolean(
                    nextProps.getProperty("test.checkHandlersOnParent", "true"));
                checkHandlersOn = nextProps.getProperty("test.checkHandlersOn", null);

                if (checkHandlersOnParent) {
                    assertEquals(expectedHandlersCount,
                        fooChild.getParent().getHandlers().length,
                        "fooChild.getParent().getHandlers().length");
                } else {
                    assertEquals(0,
                        fooChild.getParent().getHandlers().length,
                        "fooChild.getParent().getHandlers().length");
                }
                if (checkHandlersOn != null) {
                    Logger loggerWithHandlers = LogManager.getLogManager().getLogger(checkHandlersOn);
                    if (loggerWithHandlers == null) {
                        throw new RuntimeException("Logger with handlers not found: " + checkHandlersOn);
                    }
                    assertEquals(expectedHandlersCount,
                            loggerWithHandlers.getHandlers().length,
                            checkHandlersOn + ".getHandlers().length");
                }
            }
        } catch (Throwable t) {
            failed = t;
        } finally {
            final Throwable suppressed = failed;
            Configure.doPrivileged(() -> LogManager.getLogManager().reset());
            Configure.doPrivileged(() -> {
                try {
                    StringBuilder builder = new StringBuilder();
                    Files.list(Paths.get(userDir))
                        .filter((f) -> f.toString().contains(PREFIX))
                        .filter((f) -> f.toString().endsWith(".lck"))
                        .forEach((f) -> {
                                builder.append(f.toString()).append('\n');
                        });
                    if (!builder.toString().isEmpty()) {
                        throw new RuntimeException("Lock files not cleaned:\n"
                                + builder.toString());
                    }
                } catch(RuntimeException | Error x) {
                    if (suppressed != null) x.addSuppressed(suppressed);
                    throw x;
                } catch(Exception x) {
                    if (suppressed != null) x.addSuppressed(suppressed);
                    throw new RuntimeException(x);
                }
            });
            try {
                fooChild = null;
                System.out.println("Setting fooChild to: " + fooChild);
                while ((ref2 = queue.poll()) == null) {
                    System.gc();
                    Thread.sleep(1000);
                }
                if (ref2 != fooRef) {
                    throw new RuntimeException("Unexpected reference: "
                            + ref2 +"\n\texpected: " + fooRef);
                }
                if (ref2.get() != null) {
                    throw new RuntimeException("Referent not cleared: " + ref2.get());
                }
                System.out.println("Got fooRef after reset(), fooChild is " + fooChild);
           } catch (Throwable t) {
               if (failed != null) t.addSuppressed(failed);
               throw t;
           }
        }
        if (failed != null) {
            // should rarely happen...
            throw new RuntimeException(failed);
        }

    }

    public static void main(String... args) throws Exception {


        if (args == null || args.length == 0) {
            args = new String[] {
                TestCase.UNSECURE.name(),
                TestCase.SECURE.name(),
            };
        }

        try {
            for (String testName : args) {
                TestCase test = TestCase.valueOf(testName);
                test.run(properties);
            }
        } finally {
            if (userDirWritable) {
                Configure.doPrivileged(() -> {
                    // cleanup - delete files that have been created
                    try {
                        Files.list(Paths.get(userDir))
                            .filter((f) -> f.toString().contains(PREFIX))
                            .forEach((f) -> {
                                try {
                                    System.out.println("deleting " + f);
                                    Files.delete(f);
                                } catch(Throwable t) {
                                    System.err.println("Failed to delete " + f + ": " + t);
                                }
                            });
                    } catch(Throwable t) {
                        System.err.println("Cleanup failed to list files: " + t);
                        t.printStackTrace();
                    }
                });
            }
        }
    }

    static class Configure {
        static Policy policy = null;
        static final ThreadLocal<AtomicBoolean> allowAll = new ThreadLocal<AtomicBoolean>() {
            @Override
            protected AtomicBoolean initialValue() {
                return  new AtomicBoolean(false);
            }
        };
        static void setUp(TestCase test, Properties propertyFile) {
            switch (test) {
                case SECURE:
                    if (policy == null && System.getSecurityManager() != null) {
                        throw new IllegalStateException("SecurityManager already set");
                    } else if (policy == null) {
                        policy = new SimplePolicy(TestCase.SECURE, allowAll);
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
                    new InternalError("No such testcase: " + test);
            }
            doPrivileged(() -> {
                configureWith(propertyFile);
            });
        }

        static void configureWith(Properties propertyFile) {
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                propertyFile.store(bytes, propertyFile.getProperty("test.name"));
                ByteArrayInputStream bais = new ByteArrayInputStream(bytes.toByteArray());
                LogManager.getLogManager().readConfiguration(bais);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        static void updateConfigurationWith(Properties propertyFile, boolean append) {
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                propertyFile.store(bytes, propertyFile.getProperty("test.name"));
                ByteArrayInputStream bais = new ByteArrayInputStream(bytes.toByteArray());
                Function<String, BiFunction<String,String,String>> remapper =
                        append ? (x) -> ((o, n) -> n == null ? o : n)
                               : (x) -> ((o, n) -> n);
                LogManager.getLogManager().updateConfiguration(bais, remapper);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        static void doPrivileged(Runnable run) {
            final boolean old = allowAll.get().getAndSet(true);
            try {
                run.run();
            } finally {
                allowAll.get().set(old);
            }
        }
        static <T> T callPrivileged(Callable<T> call) throws Exception {
            final boolean old = allowAll.get().getAndSet(true);
            try {
                return call.call();
            } finally {
                allowAll.get().set(old);
            }
        }
    }

    @FunctionalInterface
    public static interface FileHandlerSupplier {
        public FileHandler test() throws Exception;
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
        final Permissions allPermissions;
        final ThreadLocal<AtomicBoolean> allowAll; // actually: this should be in a thread locale
        public SimplePolicy(TestCase test, ThreadLocal<AtomicBoolean> allowAll) {
            this.allowAll = allowAll;
            permissions = new Permissions();
            permissions.add(new LoggingPermission("control", null));
            permissions.add(new FilePermission(PREFIX+".lck", "read,write,delete"));
            permissions.add(new FilePermission(PREFIX, "read,write"));

            // these are used for configuring the test itself...
            allPermissions = new Permissions();
            allPermissions.add(new java.security.AllPermission());

        }

        @Override
        public boolean implies(ProtectionDomain domain, Permission permission) {
            if (allowAll.get().get()) return allPermissions.implies(permission);
            return permissions.implies(permission) || DEFAULT_POLICY.implies(domain, permission);
        }

        @Override
        public PermissionCollection getPermissions(CodeSource codesource) {
            return new PermissionsBuilder().addAll(allowAll.get().get()
                    ? allPermissions : permissions).toPermissions();
        }

        @Override
        public PermissionCollection getPermissions(ProtectionDomain domain) {
            return new PermissionsBuilder().addAll(allowAll.get().get()
                    ? allPermissions : permissions).toPermissions();
        }
    }

}
