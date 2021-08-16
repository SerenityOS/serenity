/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.reflect.Field;
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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.FileHandler;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.LoggingPermission;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @test
 * @bug 8033661 8189291
 * @summary tests LogManager.updateConfiguration(bin)
 * @modules java.logging/java.util.logging:open
 * @run main/othervm UpdateConfigurationTest UNSECURE
 * @run main/othervm -Djava.security.manager=allow UpdateConfigurationTest SECURE
 * @author danielfuchs
 */
public class UpdateConfigurationTest {

    static final Policy DEFAULT_POLICY = Policy.getPolicy();

    /**
     * We will test the handling of abstract logger nodes with file handlers in
     * two configurations:
     * UNSECURE: No security manager.
     * SECURE: With the security manager present - and the required
     *         permissions granted.
     */
    public static enum TestCase {
        UNSECURE, SECURE;
        public void run(Properties propertyFile, boolean last) throws Exception {
            System.out.println("Running test case: " + name());
            Configure.setUp(this);
            test(this.name() + " " + propertyFile.getProperty("test.name"),
                    propertyFile, last);
        }
    }


    private static final String PREFIX =
            "FileHandler-" + UUID.randomUUID() + ".log";
    private static final String userDir = System.getProperty("user.dir", ".");
    private static final boolean userDirWritable = Files.isWritable(Paths.get(userDir));

    static enum ConfigMode { APPEND, REPLACE, DEFAULT;
        boolean append() { return this == APPEND; }
        Function<String, BiFunction<String,String,String>> remapper() {
            switch(this) {
                case APPEND:
                    return (k) -> ((o,n) -> (n == null ? o : n));
                case REPLACE:
                    return (k) -> ((o,n) -> n);
            }
            return null;
        }
    }

    private static final List<Properties> properties;
    static {
        // The test will be run with each of the configurations below.
        // The 'child' logger is forgotten after each test

        Properties props1 = new Properties();
        props1.setProperty("test.name", "props1");
        props1.setProperty("test.config.mode", ConfigMode.REPLACE.name());
        props1.setProperty(FileHandler.class.getName() + ".pattern", PREFIX);
        props1.setProperty(FileHandler.class.getName() + ".limit", String.valueOf(Integer.MAX_VALUE));
        props1.setProperty(FileHandler.class.getName() + ".level", "ALL");
        props1.setProperty(FileHandler.class.getName() + ".formatter", "java.util.logging.SimpleFormatter");
        props1.setProperty("com.foo.handlers", FileHandler.class.getName());
        props1.setProperty("com.bar.level", "FINEST");

        Properties props2 = new Properties();
        props2.setProperty("test.name", "props2");
        props2.setProperty("test.config.mode", ConfigMode.DEFAULT.name());
        props2.setProperty(FileHandler.class.getName() + ".pattern", PREFIX);
        props2.setProperty(FileHandler.class.getName() + ".limit", String.valueOf(Integer.MAX_VALUE));
        props2.setProperty(FileHandler.class.getName() + ".level", "ALL");
        props2.setProperty(FileHandler.class.getName() + ".formatter", "java.util.logging.SimpleFormatter");
        props2.setProperty("com.foo.handlers", FileHandler.class.getName());
        props2.setProperty("com.foo.handlers.ensureCloseOnReset", "true");
        props2.setProperty("com.level", "FINE");

        Properties props3 = new Properties();
        props3.setProperty("test.name", "props3");
        props3.setProperty("test.config.mode", ConfigMode.APPEND.name());
        props3.setProperty(FileHandler.class.getName() + ".pattern", PREFIX);
        props3.setProperty(FileHandler.class.getName() + ".limit", String.valueOf(Integer.MAX_VALUE));
        props3.setProperty(FileHandler.class.getName() + ".level", "ALL");
        props3.setProperty(FileHandler.class.getName() + ".formatter", "java.util.logging.SimpleFormatter");
        props3.setProperty("com.foo.handlers", ""); // specify "" to override the value in the previous conf
        props3.setProperty("com.foo.handlers.ensureCloseOnReset", "false");
        props3.setProperty("com.bar.level", "FINER");


        properties = Collections.unmodifiableList(Arrays.asList(
                    props1, props2, props3, props1));
    }

    static Properties previous;
    static Properties current;
    static final Field propsField;
    static {
        LogManager manager = LogManager.getLogManager();
        try {
            propsField = LogManager.class.getDeclaredField("props");
            propsField.setAccessible(true);
            previous = current = (Properties) propsField.get(manager);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    static Properties getProperties() {
        try {
            return (Properties) propsField.get(LogManager.getLogManager());
        } catch (IllegalAccessException x) {
            throw new RuntimeException(x);
        }
    }

    static String trim(String value) {
        return value == null ? null : value.trim();
    }


    /**
     * Tests one of the configuration defined above.
     * <p>
     * This is the main test method (the rest is infrastructure).
     * <p>
     * Creates a child of the com.foo logger (com.foo.child), resets
     * the configuration, and verifies that com.foo has no handler.
     * Then reapplies the configuration and verifies that the handler
     * for com.foo has been reestablished, depending on whether
     * java.util.logging.LogManager.reconfigureHandlers is present and
     * true.
     * <p>
     * Finally releases the logger com.foo.child, so that com.foo can
     * be garbage collected, and the next configuration can be
     * tested.
     */
    static void test(ConfigMode mode, String name, Properties props, boolean last)
            throws Exception {

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
        int max = 10;
        barChild = null;
        System.gc();
        while ((ref2 = queue.poll()) == null) {
            System.gc();
            Thread.sleep(100);
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
            // Now lets try to  check that ref2 has expected handlers, and
            // attempt to configure again.
            String p = current.getProperty("com.foo.handlers", "").trim();
            assertEquals(p.isEmpty() ? 0 : 1, fooChild.getParent().getHandlers().length,
                    "["+name+"] fooChild.getParent().getHandlers().length");
            Configure.doPrivileged(() -> Configure.updateConfigurationWith(props, mode.remapper()));
            String p2 = previous.getProperty("com.foo.handlers", "").trim();
            assertEquals(p, p2, "["+name+"] com.foo.handlers");
            String n = trim(props.getProperty("com.foo.handlers", null));
            boolean hasHandlers = mode.append()
                    ? (n == null ? !p.isEmpty() : !n.isEmpty())
                    : n != null && !n.isEmpty();
            assertEquals( hasHandlers ? 1 : 0,
                    fooChild.getParent().getHandlers().length,
                    "["+name+"] fooChild.getParent().getHandlers().length"
                    + "[p=\""+p+"\", n=" + (n==null?null:"\""+n+"\"") + "]");

            checkProperties(mode, previous, current, props);

        } catch (Throwable t) {
            failed = t;
        } finally {
            if (last || failed != null) {
                final Throwable suppressed = failed;
                Configure.doPrivileged(LogManager.getLogManager()::reset);
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

                if (suppressed == null) {
                    // Now we need to forget the child, so that loggers are released,
                    // and so that we can run the test with the next configuration...
                    // No need to do that if failed!=null however, as the first
                    // ref might not have been cleared yet and failing here would
                    // hide the original failure.
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
                }
            }
        }
        if (failed != null) {
            // should rarely happen...
            throw new RuntimeException(failed);
        }

    }

    private static void checkProperties(ConfigMode mode,
            Properties previous, Properties current, Properties props) {
        Set<String> set = new HashSet<>();

        // Check that all property names from 'props' are in current.
        set.addAll(props.stringPropertyNames());
        set.removeAll(current.keySet());
        if (!set.isEmpty()) {
            throw new RuntimeException("Missing properties in current: " + set);
        }
        set.clear();
        set.addAll(current.stringPropertyNames());
        set.removeAll(previous.keySet());
        set.removeAll(props.keySet());
        if (!set.isEmpty()) {
            throw new RuntimeException("Superfluous properties in current: " + set);
        }
        set.clear();
        Stream<String> allnames =
                Stream.concat(
                    Stream.concat(previous.stringPropertyNames().stream(),
                                  props.stringPropertyNames().stream()),
                    current.stringPropertyNames().stream())
                        .collect(Collectors.toCollection(TreeSet::new))
                        .stream();
        if (mode.append()) {
            // Check that all previous property names are in current.
            set.addAll(previous.stringPropertyNames());
            set.removeAll(current.keySet());
            if (!set.isEmpty()) {
                throw new RuntimeException("Missing properties in current: " + set
                    + "\n\tprevious: " + previous
                    + "\n\tcurrent:  " + current
                    + "\n\tprops:    " + props);

            }
            allnames.forEach((k) -> {
                    String p = previous.getProperty(k, "").trim();
                    String n = current.getProperty(k, "").trim();
                    if (props.containsKey(k)) {
                        assertEquals(props.getProperty(k), n, k);
                    } else {
                        assertEquals(p, n, k);
                    }
                });
        } else {
            // Check that only properties from 'props' are in current.
            set.addAll(current.stringPropertyNames());
            set.removeAll(props.keySet());
            if (!set.isEmpty()) {
                throw new RuntimeException("Superfluous properties in current: " + set);
            }
            allnames.forEach((k) -> {
                    String p = previous.getProperty(k, "");
                    String n = current.getProperty(k, "");
                    if (props.containsKey(k)) {
                        assertEquals(props.getProperty(k), n, k);
                    } else {
                        assertEquals("", n, k);
                    }
                });
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
                for (int i=0; i<properties.size();i++) {
                    Properties propertyFile = properties.get(i);
                    test.run(propertyFile, i == properties.size() - 1);
                }
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
        static void setUp(TestCase test) {
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

        static void doPrivileged(Runnable run) {
            final boolean old = allowAll.get().getAndSet(true);
            try {
                Properties before = getProperties();
                try {
                    run.run();
                } finally {
                    Properties after = getProperties();
                    if (before != after) {
                        previous = before;
                        current = after;
                    }
                }
            } finally {
                allowAll.get().set(old);
            }
        }
        static <T> T callPrivileged(Callable<T> call) throws Exception {
            final boolean old = allowAll.get().getAndSet(true);
            try {
                Properties before = getProperties();
                try {
                    return call.call();
                } finally {
                    Properties after = getProperties();
                    if (before != after) {
                        previous = before;
                        current = after;
                    }
                }
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

    private static void assertEquals(String expected, String received, String msg) {
        if (!Objects.equals(expected, received)) {
            throw new TestAssertException("Unexpected result for " + msg
                    + ".\n\texpected: " + expected
                    +  "\n\tactual:   " + received);
        } else {
            System.out.println("Got expected " + msg + ": " + received);
        }
    }


    public static void test(String name, Properties props, boolean last) throws Exception {
        ConfigMode configMode = ConfigMode.valueOf(props.getProperty("test.config.mode"));
        System.out.println("\nTesting: " + name + " mode=" + configMode);
        if (!userDirWritable) {
            throw new RuntimeException("Not writable: "+userDir);
        }
        switch(configMode) {
            case REPLACE:
            case APPEND:
            case DEFAULT:
                test(configMode, name, props, last); break;
            default:
                throw new RuntimeException("Unknwown mode: " + configMode);
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
            return permissions.implies(permission) ||
                   DEFAULT_POLICY.implies(domain, permission);
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
