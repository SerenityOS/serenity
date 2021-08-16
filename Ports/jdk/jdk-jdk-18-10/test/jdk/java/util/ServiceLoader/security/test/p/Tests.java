/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package p;

import java.security.AccessControlContext;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import static java.security.AccessController.doPrivileged;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeTest;
import static org.testng.Assert.*;

/**
 * Tests ServiceLoader when running with a security manager, specifically
 * tests to ensure that provider code is run with permissions restricted by
 * the creater of ServiceLoader, and also testing of exceptions thrown
 * when loading or initializing a provider.
 */

public class Tests {

    static final Permission PERM = new RuntimePermission("eatMuffin");

    static <T> PrivilegedAction<ServiceLoader<T>> loadAction(Class<T> service) {
        return () -> ServiceLoader.load(service);
    }

    static AccessControlContext withPermissions(Permission... perms) {
        Permissions p = new Permissions();
        for (Permission perm : perms) {
            p.add(perm);
        }
        ProtectionDomain pd = new ProtectionDomain(null, p);
        return new AccessControlContext(new ProtectionDomain[]{ pd });
    }

    static AccessControlContext noPermissions() {
        return withPermissions(/*empty*/);
    }

    @BeforeTest
    public void setSecurityManager() {
        class Policy extends java.security.Policy {
            private final Permissions perms;
            public Policy(Permission... permissions) {
                perms = new Permissions();
                for (Permission permission : permissions) {
                    perms.add(permission);
                }
            }
            public PermissionCollection getPermissions(CodeSource cs) {
                return perms;
            }
            public PermissionCollection getPermissions(ProtectionDomain pd) {
                return perms;
            }
            public boolean implies(ProtectionDomain pd, Permission p) {
                return perms.implies(p);
            }
            public void refresh() { }
        }
        Policy policy = new Policy(new AllPermission());
        Policy.setPolicy(policy);
        System.setSecurityManager(new SecurityManager());
    }

    @Test
    public void testConstructorUsingIteratorWithPermission() {
        ServiceLoader<S1> sl = doPrivileged(loadAction(S1.class), withPermissions(PERM));
        S1 obj = sl.iterator().next();
    }

    @Test
    public void testConstructorUsingStreamWithPermission() {
        ServiceLoader<S1> sl = doPrivileged(loadAction(S1.class), withPermissions(PERM));
        assertTrue(sl.stream().map(Provider::get).count() == 1);
    }

    @Test
    public void testConstructorUsingIteratorNoPermission() {
        ServiceLoader<S1> sl = doPrivileged(loadAction(S1.class), noPermissions());
        try {
            sl.iterator().next();
            assertTrue(false);
        } catch (ServiceConfigurationError e) {
            assertTrue(e.getCause() instanceof AccessControlException);
        }
    }

    @Test
    public void testConstructorUsingStreamNoPermission() {
        ServiceLoader<S1> sl = doPrivileged(loadAction(S1.class), noPermissions());
        try {
            sl.stream().map(Provider::get).count();
            assertTrue(false);
        } catch (ServiceConfigurationError e) {
            assertTrue(e.getCause() instanceof AccessControlException);
        }
    }

    @Test
    public void testFactoryMethodUsingIteratorWithPermission() {
        ServiceLoader<S2> sl = doPrivileged(loadAction(S2.class), withPermissions(PERM));
        S2 obj = sl.iterator().next();
    }

    @Test
    public void testFactoryMethodUsingStreamWithPermission() {
        ServiceLoader<S2> sl = doPrivileged(loadAction(S2.class), withPermissions(PERM));
        assertTrue(sl.stream().map(Provider::get).count() == 1);
    }

    @Test
    public void testFactoryMethodUsingIteratorNoPermission() {
        ServiceLoader<S2> sl = doPrivileged(loadAction(S2.class), noPermissions());
        try {
            sl.iterator().next();
            assertTrue(false);
        } catch (ServiceConfigurationError e) {
            assertTrue(e.getCause() instanceof AccessControlException);
        }
    }

    @Test
    public void testFactoryMethodUsingStreamNoPermission() {
        ServiceLoader<S2> sl = doPrivileged(loadAction(S2.class), noPermissions());
        try {
            sl.stream().map(Provider::get).count();
            assertTrue(false);
        } catch (ServiceConfigurationError e) {
            assertTrue(e.getCause() instanceof AccessControlException);
        }
    }

    @DataProvider(name = "failingServices")
    public Object[][] failingServices() {
        return new Object[][] {
            { S3.class,    P3.Error3.class },
            { S4.class,    P4.Error4.class },
            { S5.class,    P5.Error5.class },
            { S6.class,    P6.Error6.class },
        };
    }

    @Test(dataProvider = "failingServices")
    public void testFailingService(Class<?> service, Class<? extends Error> errorClass) {
        ServiceLoader<?> sl = ServiceLoader.load(service);
        try {
            sl.iterator().next();
            assertTrue(false);
        } catch (ServiceConfigurationError e) {
            assertTrue(e.getCause().getClass() == errorClass);
        }
    }

    // service types and implementations

    public static interface S1 { }
    public static interface S2 { }
    public static interface S3 { }
    public static interface S4 { }
    public static interface S5 { }
    public static interface S6 { }

    public static class P1 implements S1 {
        public P1() {
            AccessController.getContext().checkPermission(PERM);
        }
    }
    public static class P2 implements S2 {
        private P2() {
            AccessController.getContext().checkPermission(PERM);
        }
        public static S2 provider() {
            return new P2();
        }
    }

    public static class P3 implements S3 {
        static class Error3 extends Error { }
        static {
            if (1==1) throw new Error3();  // fail
        }
        public P3() { }
    }

    public static class P4 implements S4 {
        static class Error4 extends Error { }
        static {
            if (1==1) throw new Error4();  // fail
        }
        public static S4 provider() {
            return new P4();
        }
    }

    public static class P5 implements S5 {
        static class Error5 extends Error { }
        public P5() {
            throw new Error5();  // fail
        }
    }

    public static class P6 implements S6 {
        static class Error6 extends Error { }
        public static S6 provider() {
            throw new Error6();   // fail
        }
    }
}
