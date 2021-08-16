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
import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;
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
import java.util.PropertyPermission;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.FileHandler;
import java.util.logging.LogManager;
import java.util.logging.LoggingPermission;

/**
 * @test
 * @bug 8059269
 * @summary tests that using a simple (non composite) pattern does not lead
 *        to NPE when the lock file already exists.
 * @run main/othervm FileHandlerPath UNSECURE
 * @run main/othervm -Djava.security.manager=allow FileHandlerPath SECURE
 * @author danielfuchs
 * @key randomness
 */
public class FileHandlerPath {

    /**
     * We will test the simple pattern in two configurations.
     * UNSECURE: No security manager.
     * SECURE: With the security manager present - and the required
     *         permissions granted.
     */
    public static enum TestCase {
        UNSECURE, SECURE;
        public void run(Properties propertyFile) throws Exception {
            System.out.println("Running test case: " + name());
            Configure.setUp(this, propertyFile);
            test(this.name() + " " + propertyFile.getProperty("test.name"), propertyFile);
        }
    }


    // Use a random name provided by UUID to avoid collision with other tests
    static final String logFile = FileHandlerPath.class.getSimpleName() + "_"
                + UUID.randomUUID().toString() + ".log";
    static final String tmpLogFile;
    static final String userDir = System.getProperty("user.dir");
    static final String tmpDir = System.getProperty("java.io.tmpdir");
    private static final List<Properties> properties;
    static {
        tmpLogFile = new File(tmpDir, logFile).toString();
        Properties props1 = new Properties();
        Properties props2 = new Properties();
        props1.setProperty("test.name", "relative file");
        props1.setProperty("test.file.name", logFile);
        props1.setProperty(FileHandler.class.getName() + ".pattern", logFile);
        props1.setProperty(FileHandler.class.getName() + ".count", "1");
        props2.setProperty("test.name", "absoluste file");
        props2.setProperty("test.file.name", tmpLogFile);
        props2.setProperty(FileHandler.class.getName() + ".pattern", "%t/" + logFile);
        props2.setProperty(FileHandler.class.getName() + ".count", "1");
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

        // Sanity checks

        if (!Files.isWritable(Paths.get(userDir))) {
            throw new RuntimeException(userDir +
                    ": user.dir is not writable - can't run test.");
        }
        if (!Files.isWritable(Paths.get(tmpDir))) {
            throw new RuntimeException(tmpDir +
                    ": java.io.tmpdir is not writable - can't run test.");
        }

        File[] files = {
            new File(logFile),
            new File(tmpLogFile),
            new File(logFile+".1"),
            new File(tmpLogFile+".1"),
            new File(logFile+".lck"),
            new File(tmpLogFile+".lck"),
            new File(logFile+".1.lck"),
            new File(tmpLogFile+".1.lck")
        };

        for (File log : files) {
            if (log.exists()) {
                throw new Exception(log +": file already exists - can't run test.");
            }
        }

        // Now start the real test

        try {
            for (String testName : args) {
                for (Properties propertyFile : properties) {
                    TestCase test = TestCase.valueOf(testName);
                    test.run(propertyFile);
                }
            }
        } finally {
            // Cleanup...
            Configure.doPrivileged(() -> {
                for(File log : files) {
                    try {
                        final boolean isLockFile = log.getName().endsWith(".lck");
                        // lock file should already be deleted, except if the
                        // test failed in exception.
                        // log file should all be present, except if the test
                        // failed in exception.
                        if (log.exists()) {
                            if (!isLockFile) {
                                System.out.println("deleting "+log.toString());
                            } else {
                                System.err.println("deleting lock file "+log.toString());
                            }
                            log.delete();
                        } else {
                            if (!isLockFile) {
                                System.err.println(log.toString() + ": not found.");
                            }
                        }
                    } catch (Throwable t) {
                        // should not happen
                        t.printStackTrace();
                    }
                }
            });
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

    public static void test(String name, Properties props) throws Exception {
        System.out.println("Testing: " + name);
        String file = props.getProperty("test.file.name");
        // create the lock files first - in order to take the path that
        // used to trigger the NPE
        Files.createFile(Paths.get(file + ".lck"));
        Files.createFile(Paths.get(file + ".1.lck"));
        final FileHandler f1 = new FileHandler();
        final FileHandler f2 = new FileHandler();
        f1.close();
        f2.close();
        System.out.println("Success for " + name);
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
            permissions = new Permissions();
            permissions.add(new LoggingPermission("control", null)); // needed by new FileHandler()
            permissions.add(new FilePermission("<<ALL FILES>>", "read")); // needed by new FileHandler()
            permissions.add(new FilePermission(logFile, "write,delete")); // needed by new FileHandler()
            permissions.add(new FilePermission(logFile+".lck", "write,delete")); // needed by FileHandler.close()
            permissions.add(new FilePermission(logFile+".1", "write,delete")); // needed by new FileHandler()
            permissions.add(new FilePermission(logFile+".1.lck", "write,delete")); // needed by FileHandler.close()
            permissions.add(new FilePermission(tmpLogFile, "write,delete")); // needed by new FileHandler()
            permissions.add(new FilePermission(tmpLogFile+".lck", "write,delete")); // needed by FileHandler.close()
            permissions.add(new FilePermission(tmpLogFile+".1", "write,delete")); // needed by new FileHandler()
            permissions.add(new FilePermission(tmpLogFile+".1.lck", "write,delete")); // needed by FileHandler.close()
            permissions.add(new FilePermission(userDir, "write")); // needed by new FileHandler()
            permissions.add(new FilePermission(tmpDir, "write")); // needed by new FileHandler()
            permissions.add(new PropertyPermission("user.dir", "read"));
            permissions.add(new PropertyPermission("java.io.tmpdir", "read"));
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
