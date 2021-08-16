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

/*
 * @test
 * @bug 8140450 8152893 8189291
 * @summary Basic test for StackWalker.getCallerClass()
 * @run main/othervm GetCallerClassTest
 * @run main/othervm -Djava.security.manager=allow GetCallerClassTest sm
 */

import static java.lang.StackWalker.Option.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

public class GetCallerClassTest {
    static final Policy DEFAULT_POLICY = Policy.getPolicy();
    private final StackWalker walker;
    private final boolean expectUOE;

    public GetCallerClassTest(StackWalker sw, boolean expect) {
        this.walker = sw;
        this.expectUOE = expect;
    }
    public static void main(String... args) throws Exception {
        if (args.length > 0 && args[0].equals("sm")) {
            PermissionCollection perms = new Permissions();
            perms.add(new RuntimePermission("getStackWalkerWithClassReference"));
            Policy.setPolicy(new Policy() {
                @Override
                public boolean implies(ProtectionDomain domain, Permission p) {
                    return perms.implies(p) ||
                        DEFAULT_POLICY.implies(domain, p);
                }
            });
            System.setSecurityManager(new SecurityManager());
        }
        new GetCallerClassTest(StackWalker.getInstance(), true).test();
        new GetCallerClassTest(StackWalker.getInstance(RETAIN_CLASS_REFERENCE), false).test();
        new GetCallerClassTest(StackWalker.getInstance(EnumSet.of(RETAIN_CLASS_REFERENCE,
                                                                  SHOW_HIDDEN_FRAMES)), false).test();
    }

    public void test() {
        new TopLevelCaller().run();
        new LambdaTest().run();
        new Nested().createNestedCaller().run();
        new InnerClassCaller().run();
        new ReflectionTest().run();

        List<Thread> threads = Arrays.asList(
                new Thread(new TopLevelCaller()),
                new Thread(new LambdaTest()),
                new Thread(new Nested().createNestedCaller()),
                new Thread(new InnerClassCaller()),
                new Thread(new ReflectionTest())
        );
        threads.stream().forEach(Thread::start);
        threads.stream().forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void staticGetCallerClass(StackWalker stackWalker,
                                            Class<?> expected,
                                            boolean expectUOE) {
        try {
            Class<?> c = stackWalker.getCallerClass();
            assertEquals(c, expected);
            if (expectUOE) { // Should have thrown
                throw new RuntimeException("Didn't get expected exception");
            }
        } catch (RuntimeException e) { // also catches UOE
            if (expectUOE && causeIsUOE(e)) {
                return; /* expected */
            }
            System.err.println("Unexpected exception:");
            throw e;
        }
    }

    public static void reflectiveGetCallerClass(StackWalker stackWalker,
                                                Class<?> expected,
                                                boolean expectUOE) {
        try {
            Method m = StackWalker.class.getMethod("getCallerClass");
            Class<?> c = (Class<?>) m.invoke(stackWalker);
            assertEquals(c, expected);
            if (expectUOE) { // Should have thrown
                throw new RuntimeException("Didn't get expected exception");
            }
        } catch (Throwable e) {
            if (expectUOE && causeIsUOE(e)) {
                return; /* expected */
            }
            System.err.println("Unexpected exception:");
            throw new RuntimeException(e);
        }
    }

    public static void methodHandleGetCallerClass(StackWalker stackWalker,
                                                  Class<?> expected,
                                                  boolean expectUOE) {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            MethodHandle mh = lookup.findVirtual(StackWalker.class, "getCallerClass",
                                                 MethodType.methodType(Class.class));
            Class<?> c = (Class<?>) mh.invokeExact(stackWalker);
            assertEquals(c, expected);
            if (expectUOE) { // Should have thrown
                throw new RuntimeException("Didn't get expected exception");
            }
        } catch (Throwable e) {
            if (expectUOE && causeIsUOE(e)) {
                return; /* expected */
            }
            System.err.println("Unexpected exception:");
            throw new RuntimeException(e);
        }
    }

    public static void assertEquals(Class<?> c, Class<?> expected) {
        if (expected != c) {
            throw new RuntimeException("Got " + c + ", but expected " + expected);
        }
    }

    /** Is there an UnsupportedOperationException in there? */
    public static boolean causeIsUOE(Throwable t) {
        while (t != null) {
            if (t instanceof UnsupportedOperationException) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    class TopLevelCaller implements Runnable {
        public void run() {
            GetCallerClassTest.staticGetCallerClass(walker, this.getClass(), expectUOE);
            GetCallerClassTest.reflectiveGetCallerClass(walker, this.getClass(), expectUOE);
            GetCallerClassTest.methodHandleGetCallerClass(walker, this.getClass(), expectUOE);
        }
    }

    class LambdaTest implements Runnable {
        public void run() {
            Runnable lambdaRunnable = () -> {
                try {
                    Class<?> c = walker.getCallerClass();

                    assertEquals(c, LambdaTest.class);
                    if (expectUOE) { // Should have thrown
                        throw new RuntimeException("Didn't get expected exception");
                    }
                } catch (Throwable e) {
                    if (expectUOE && causeIsUOE(e)) {
                        return; /* expected */
                    }
                    System.err.println("Unexpected exception:");
                    throw new RuntimeException(e);
                }
            };
            lambdaRunnable.run();
        }
    }

    class Nested {
        NestedClassCaller createNestedCaller() { return new NestedClassCaller(); }
        class NestedClassCaller implements Runnable {
            public void run() {
                GetCallerClassTest.staticGetCallerClass(walker, this.getClass(), expectUOE);
                GetCallerClassTest.reflectiveGetCallerClass(walker, this.getClass(), expectUOE);
                GetCallerClassTest.methodHandleGetCallerClass(walker, this.getClass(), expectUOE);
            }
        }
    }

    class InnerClassCaller implements Runnable {
        public void run() {
            new Inner().test();
        }
        class Inner {
            void test() {
                GetCallerClassTest.staticGetCallerClass(walker, this.getClass(), expectUOE);
                GetCallerClassTest.reflectiveGetCallerClass(walker, this.getClass(), expectUOE);
                GetCallerClassTest.methodHandleGetCallerClass(walker, this.getClass(), expectUOE);
            }
        }
    }

    class ReflectionTest implements Runnable {
        final MethodType methodType =
            MethodType.methodType(void.class, StackWalker.class, Class.class, boolean.class);

        public void run() {
            callMethodHandle();
            callMethodHandleRefl();
            callMethodInvoke();
            callMethodInvokeRefl();
        }
        void callMethodHandle() {
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            try {
                MethodHandle mh = lookup.findStatic(GetCallerClassTest.class,
                                                    "staticGetCallerClass",
                                                    methodType);
                mh.invokeExact(walker, ReflectionTest.class, expectUOE);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
        void callMethodHandleRefl() {
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            try {
                MethodHandle mh = lookup.findStatic(GetCallerClassTest.class,
                                                    "reflectiveGetCallerClass",
                                                    methodType);
                mh.invokeExact(walker, ReflectionTest.class, expectUOE);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
        void callMethodInvoke() {
            try {
                Method m = GetCallerClassTest.class.getMethod("staticGetCallerClass",
                               StackWalker.class, Class.class, boolean.class);
                m.invoke(null, walker, ReflectionTest.class, expectUOE);
            } catch (NoSuchMethodException|IllegalAccessException|InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
        void callMethodInvokeRefl() {
            try {
                Method m = GetCallerClassTest.class.getMethod("reflectiveGetCallerClass",
                               StackWalker.class, Class.class, boolean.class);
                m.invoke(null, walker, ReflectionTest.class, expectUOE);
            } catch (UnsupportedOperationException e) {
                throw e;
            } catch (NoSuchMethodException|IllegalAccessException|InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
