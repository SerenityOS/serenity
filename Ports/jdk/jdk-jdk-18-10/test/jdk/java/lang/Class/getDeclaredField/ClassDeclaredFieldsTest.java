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

import java.lang.reflect.Field;
import java.lang.reflect.ReflectPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @test
 * @bug 8065552
 * @summary test that all fields returned by getDeclaredFields() can be
 *          set accessible if the right permission is granted; this test
 *          also verifies that Class.classLoader final private field is
 *          hidden from reflection access.
 * @modules java.base/java.lang:open
 * @run main/othervm ClassDeclaredFieldsTest UNSECURE
 * @run main/othervm -Djava.security.manager=allow ClassDeclaredFieldsTest SECURE
 *
 * @author danielfuchs
 */
public class ClassDeclaredFieldsTest {

    // Test with or without a security manager
    public static enum TestCase {
        UNSECURE, SECURE;
        public void run() throws Exception {
            System.out.println("Running test case: " + name());
            Configure.setUp(this);
            test(this);
        }
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        System.out.println(System.getProperty("java.version"));
        if (args == null || args.length == 0) {
            args = new String[] { "SECURE" };
        } else if (args.length != 1) {
            throw new IllegalArgumentException("Only one arg expected: "
                    + Arrays.asList(args));
        }
        TestCase.valueOf(args[0]).run();
    }

    static void test(TestCase test) {
        for (Field f : Class.class.getDeclaredFields()) {
            f.setAccessible(true);
            System.out.println("Field "+f.getName()+" is now accessible.");
            if (f.getName().equals("classLoader")) {
                throw new RuntimeException("Found "+f.getName()+" field!");
            }
        }
        try {
            Class.class.getDeclaredField("classLoader");
            throw new RuntimeException("Expected NoSuchFieldException for"
                    + " 'classLoader' field not raised");
        } catch(NoSuchFieldException x) {
            System.out.println("Got expected exception: " + x);
        }
        System.out.println("Passed "+test);
    }

    // A helper class to configure the security manager for the test,
    // and bypass it when needed.
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
                    throw new InternalError("No such testcase: " + test);
            }
        }
        static void doPrivileged(Runnable run) {
            allowAll.get().set(true);
            try {
                run.run();
            } finally {
                allowAll.get().set(false);
            }
        }
    }

    // A Helper class to build a set of permissions.
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

    // Policy for the test...
    public static class SimplePolicy extends Policy {

        static final Policy DEFAULT_POLICY = Policy.getPolicy();
        final Permissions permissions;
        final Permissions allPermissions;
        final ThreadLocal<AtomicBoolean> allowAll; // actually: this should be in a thread locale
        public SimplePolicy(TestCase test, ThreadLocal<AtomicBoolean> allowAll) {
            this.allowAll = allowAll;
            // we don't actually need any permission to create our
            // FileHandlers because we're passing invalid parameters
            // which will make the creation fail...
            permissions = new Permissions();
            permissions.add(new RuntimePermission("accessDeclaredMembers"));
            permissions.add(new ReflectPermission("suppressAccessChecks"));

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
