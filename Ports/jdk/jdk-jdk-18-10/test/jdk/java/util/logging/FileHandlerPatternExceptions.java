/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.FileHandler;
import java.util.logging.LogManager;

/**
 * @test
 * @bug 8025690
 * @summary tests that an empty or null pattern always result in an exception.
 * @run main/othervm FileHandlerPatternExceptions UNSECURE
 * @run main/othervm -Djava.security.manager=allow FileHandlerPatternExceptions SECURE
 * @author danielfuchs
 * @key randomness
 */
public class FileHandlerPatternExceptions {

    /**
     * We will test null/empty pattern in two configurations.
     * UNSECURE: No security manager.
     * SECURE: With the security manager present - and the required
     *         permissions granted.
     */
    public static enum TestCase {
        UNSECURE, SECURE;
        public void run(Properties propertyFile) throws Exception {
            System.out.println("Running test case: " + name());
            Configure.setUp(this, propertyFile);
            test(this.name() + " " + propertyFile.getProperty("test.name"));
        }
    }


    private static final String PREFIX =
            "FileHandler-" + UUID.randomUUID() + ".log";
    private static final String userDir = System.getProperty("user.dir", ".");
    private static final boolean userDirWritable = Files.isWritable(Paths.get(userDir));

    private static final List<Properties> properties;
    static {
        Properties props1 = new Properties();
        Properties props2 = new Properties();
        props1.setProperty("test.name", "with count=1");
        props1.setProperty(FileHandler.class.getName() + ".pattern", "");
        props1.setProperty(FileHandler.class.getName() + ".count", "1");
        props2.setProperty("test.name", "with count=2");
        props2.setProperty(FileHandler.class.getName() + ".pattern", "");
        props2.setProperty(FileHandler.class.getName() + ".count", "2");
        properties = Collections.unmodifiableList(Arrays.asList(
                    props1,
                    props2));
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
                for (Properties propertyFile : properties) {
                    TestCase test = TestCase.valueOf(testName);
                    test.run(propertyFile);
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
        static final AtomicBoolean allowAll = new AtomicBoolean(false);
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
                try {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    propertyFile.store(bytes, propertyFile.getProperty("test.name"));
                    ByteArrayInputStream bais = new ByteArrayInputStream(bytes.toByteArray());
                    LogManager.getLogManager().readConfiguration(bais);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
        }
        static void doPrivileged(Runnable run) {
            allowAll.set(true);
            try {
                run.run();
            } finally {
                allowAll.set(false);
            }
        }
    }

    @FunctionalInterface
    public static interface FileHandlerSupplier {
        public FileHandler test() throws Exception;
    }

    private static void checkException(Class<? extends Exception> type, FileHandlerSupplier test) {
        Throwable t = null;
        FileHandler f = null;
        try {
            f = test.test();
        } catch (Throwable x) {
            t = x;
        }
        try {
            if (type != null && t == null) {
                throw new RuntimeException("Expected " + type.getName() + " not thrown");
            } else if (type != null && t != null) {
                if (type.isInstance(t)) {
                    System.out.println("Recieved expected exception: " + t);
                } else {
                    throw new RuntimeException("Exception type mismatch: "
                        + type.getName() + " expected, "
                        + t.getClass().getName() + " received.", t);
                }
            } else if (t != null) {
                throw new RuntimeException("Unexpected exception received: " + t, t);
            }
        } finally {
            if (f != null) {
                // f should always be null when an exception is expected,
                // but in case the test doesn't behave as expected we will
                // want to close f.
                try { f.close(); } catch (Throwable x) {};
            }
        }
    }

    public static void test(String name) throws Exception {
        System.out.println("Testing: " + name);
        checkException(RuntimeException.class, () -> new FileHandler());
        checkException(IllegalArgumentException.class, () -> new FileHandler(""));
        checkException(NullPointerException.class, () -> new FileHandler(null));

        checkException(IllegalArgumentException.class, () -> new FileHandler("", true));
        checkException(IllegalArgumentException.class, () -> new FileHandler("", false));
        checkException(NullPointerException.class, () -> new FileHandler(null, true));
        checkException(NullPointerException.class, () -> new FileHandler(null, false));

        checkException(IllegalArgumentException.class, () -> new FileHandler("", 1, 1));
        checkException(IllegalArgumentException.class, () -> new FileHandler(PREFIX, 0, 0));
        checkException(IllegalArgumentException.class, () -> new FileHandler(PREFIX, -1, 1));
        checkException(IllegalArgumentException.class, () -> new FileHandler("", 0, 0));
        checkException(IllegalArgumentException.class, () -> new FileHandler("", -1, 1));

        checkException(IllegalArgumentException.class, () -> new FileHandler("", 1, 1, true));
        checkException(IllegalArgumentException.class, () -> new FileHandler(PREFIX, 0, 0, true));
        checkException(IllegalArgumentException.class, () -> new FileHandler(PREFIX, -1, 1, true));
        checkException(IllegalArgumentException.class, () -> new FileHandler("", 0, 0, true));
        checkException(IllegalArgumentException.class, () -> new FileHandler("", -1, 1, true));

        checkException(IllegalArgumentException.class, () -> new FileHandler("", 1, 1, false));
        checkException(IllegalArgumentException.class, () -> new FileHandler(PREFIX, 0, 0, false));
        checkException(IllegalArgumentException.class, () -> new FileHandler(PREFIX, -1, 1, false));
        checkException(IllegalArgumentException.class, () -> new FileHandler("", 0, 0, false));
        checkException(IllegalArgumentException.class, () -> new FileHandler("", -1, 1, false));

        final Class<? extends Exception> expectedException =
                System.getSecurityManager() != null ? AccessControlException.class : null;

        if (userDirWritable || expectedException != null) {
            // These calls will create files in user.dir in the UNSECURE case.
            // The file name contain a random UUID (PREFIX) which identifies them
            // and allow us to remove them cleanly at the end (see finally block
            // in main()).
            checkException(expectedException,
                           () -> new FileHandler(PREFIX, 0, 1, true));
            checkException(expectedException,
                           () -> new FileHandler(PREFIX, 1, 2, true));
            checkException(expectedException,
                           () -> new FileHandler(PREFIX, 0, 1, false));
            checkException(expectedException,
                           () -> new FileHandler(PREFIX, 1, 2, false));
        }
    }


    static final class PermissionsBuilder {
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
        final AtomicBoolean allowAll;
        public SimplePolicy(TestCase test, AtomicBoolean allowAll) {
            this.allowAll = allowAll;
            // we don't actually need any permission to create our
            // FileHandlers because we're passing invalid parameters
            // which will make the creation fail...
            permissions = new Permissions();

            // these are used for configuring the test itself...
            allPermissions = new Permissions();
            allPermissions.add(new java.security.AllPermission());

        }

        @Override
        public boolean implies(ProtectionDomain domain, Permission permission) {
            if (allowAll.get()) return allPermissions.implies(permission);
            return permissions.implies(permission) || DEFAULT_POLICY.implies(domain, permission);
        }

        @Override
        public PermissionCollection getPermissions(CodeSource codesource) {
            return new PermissionsBuilder().addAll(allowAll.get()
                    ? allPermissions : permissions).toPermissions();
        }

        @Override
        public PermissionCollection getPermissions(ProtectionDomain domain) {
            return new PermissionsBuilder().addAll(allowAll.get()
                    ? allPermissions : permissions).toPermissions();
        }
    }

}
