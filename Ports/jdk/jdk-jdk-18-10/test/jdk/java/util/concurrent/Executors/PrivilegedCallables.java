/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 6552961 6558429
 * @summary Test privilegedCallable, privilegedCallableUsingCurrentClassLoader
 * @run main/othervm -Djava.security.manager=allow PrivilegedCallables
 * @author Martin Buchholz
 */

import static java.util.concurrent.Executors.privilegedCallable;
import static java.util.concurrent.Executors.privilegedCallableUsingCurrentClassLoader;
import static java.util.concurrent.Executors.privilegedThreadFactory;

import java.security.AccessControlException;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.util.Random;
import java.util.concurrent.Callable;

public class PrivilegedCallables {
    Callable<Integer> real;

    final Callable<Integer> realCaller = new Callable<>() {
        public Integer call() throws Exception {
            return real.call(); }};

    final Random rnd = new Random();

    @SuppressWarnings("serial")
    final Throwable[] throwables = {
        new Exception() {},
        new RuntimeException() {},
        new Error() {}
    };
    Throwable randomThrowable() {
        return throwables[rnd.nextInt(throwables.length)];
    }
    void throwThrowable(Throwable t) throws Exception {
        if (t instanceof Error) throw (Error) t;
        if (t instanceof RuntimeException) throw (RuntimeException) t;
        throw (Exception) t;
    }

    //----------------------------------------------------------------
    // A Policy class designed to make permissions fiddling very easy.
    //----------------------------------------------------------------
    static class Policy extends java.security.Policy {
        static final java.security.Policy DEFAULT_POLICY = java.security.Policy.getPolicy();

        private Permissions perms;

        public void setPermissions(Permission...permissions) {
            perms = new Permissions();
            for (Permission permission : permissions)
                perms.add(permission);
        }

        public Policy() { setPermissions(/* Nothing */); }

        public PermissionCollection getPermissions(CodeSource cs) {
            return perms;
        }

        public PermissionCollection getPermissions(ProtectionDomain pd) {
            return perms;
        }

        public boolean implies(ProtectionDomain pd, Permission p) {
            return perms.implies(p) || DEFAULT_POLICY.implies(pd, p);
        }

        public void refresh() {}
    }

    void test(String[] args) {
        testPrivileged();

        final Policy policy = new Policy();
        Policy.setPolicy(policy);
        policy.setPermissions(new RuntimePermission("getClassLoader"),
                              new RuntimePermission("setContextClassLoader"));
        System.setSecurityManager(new SecurityManager());

        testPrivileged();

        policy.setPermissions(/* Nothing */);

        THROWS(AccessControlException.class,
               new F() {void f(){ privilegedCallableUsingCurrentClassLoader(realCaller); }},
               new F() {void f(){ privilegedThreadFactory(); }});

        policy.setPermissions(new RuntimePermission("setSecurityManager"));
        System.setSecurityManager(null);
    }

    void testPrivileged() {
        try { test(privilegedCallable(realCaller)); }
        catch (Throwable t) { unexpected(t); }

        try { test(privilegedCallableUsingCurrentClassLoader(realCaller)); }
        catch (Throwable t) { unexpected(t); }

        try { privilegedThreadFactory(); }
        catch (Throwable t) { unexpected(t); }
    }

    void test(final Callable<Integer> c) throws Throwable {
        for (int i = 0; i < 20; i++)
            if (rnd.nextBoolean()) {
                final Throwable t = randomThrowable();
                real = new Callable<>() {
                    public Integer call() throws Exception {
                        throwThrowable(t);
                        return null; }};
                try {
                    c.call();
                    fail("Expected exception not thrown");
                } catch (Throwable tt) { check(t == tt); }
            } else {
                final int n = rnd.nextInt();
                real = new Callable<>() {
                    public Integer call() { return n; }};
                equal(c.call(), n);
            }
    }

    //--------------------- Infrastructure ---------------------------
    volatile int passed = 0, failed = 0;
    void pass() {passed++;}
    void fail() {failed++; Thread.dumpStack();}
    void fail(String msg) {System.err.println(msg); fail();}
    void unexpected(Throwable t) {failed++; t.printStackTrace();}
    void check(boolean cond) {if (cond) pass(); else fail();}
    void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        new PrivilegedCallables().instanceMain(args);}
    void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
    abstract class F {abstract void f() throws Throwable;}
    void THROWS(Class<? extends Throwable> k, F... fs) {
        for (F f : fs)
            try {f.f(); fail("Expected " + k.getName() + " not thrown");}
            catch (Throwable t) {
                if (k.isAssignableFrom(t.getClass())) pass();
                else unexpected(t);}}
}
