/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8143214
 * @summary Verify outputs of Thread.dumpStack() and Throwable.printStackTrace().
 *          This test should also been run against jdk9 successfully except of
 *          VM option MemberNameInStackFrame.
 * @run main/othervm DumpStackTest
 */

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Consumer;

public class DumpStackTest {

    public static void main(String args[]) {
        test();
        testThread();
        testLambda();
        testMethodInvoke();
        testMethodHandle();
    }

    static class CallFrame {
        final String classname;
        final String methodname;
        CallFrame(Class<?> c, String methodname) {
            this(c.getName(), methodname);
        }
        CallFrame(String classname, String methodname) {
            this.classname = classname;
            this.methodname = methodname;
        }

        String getClassName() {
            return classname;
        }
        String getMethodName() {
            return methodname;
        }
        String getFileName() {
            int i = classname.lastIndexOf('.');
            int j = classname.lastIndexOf('$');
            String name = classname.substring(i+1, j >= 0 ? j : classname.length());
            return name + ".java";
        }
        @Override
        public String toString() {
            return classname + "." + methodname + "(" + getFileName() + ")";
        }
    }

    static void test() {
        CallFrame[] callStack = new CallFrame[] {
                new CallFrame(Thread.class, "getStackTrace"),
                new CallFrame(DumpStackTest.class, "test"),
                new CallFrame(DumpStackTest.class, "main"),
                // if invoked from jtreg
                new CallFrame("jdk.internal.reflect.NativeMethodAccessorImpl", "invoke0"), // non-public class
                new CallFrame("jdk.internal.reflect.NativeMethodAccessorImpl", "invoke"),
                new CallFrame("jdk.internal.reflect.DelegatingMethodAccessorImpl", "invoke"),
                new CallFrame(Method.class, "invoke"),
                new CallFrame(Thread.class, "run"),
        };

        assertStackTrace(Thread.currentThread().getStackTrace(), callStack);
        getStackTrace(callStack);
    }

    static void getStackTrace(CallFrame[] callStack) {
        // this method is the top of the stack
        callStack[0] = new CallFrame(DumpStackTest.class, "getStackTrace");

        try {
            throw new RuntimeException();
        } catch(RuntimeException ex) {
            assertStackTrace(ex.getStackTrace(), callStack);
        }
    }
    static void testThread() {
        Thread t1 = new Thread() {
            public void run() {
                c();
            }

            void c() {
                CallFrame[] callStack = new CallFrame[] {
                        new CallFrame(Thread.class, "getStackTrace"),
                        new CallFrame(this.getClass(), "c"),
                        new CallFrame(this.getClass(), "run")
                };
                assertStackTrace(Thread.currentThread().getStackTrace(), callStack);
                DumpStackTest.getStackTrace(callStack);
            }
        };
        t1.start();
        try {
            t1.join();
        } catch(InterruptedException e) {}
    }

    static void testLambda() {
        Consumer<Void> c = (x) -> consumeLambda();
        c.accept(null);
    }

    static void consumeLambda() {
        CallFrame[] callStack = new CallFrame[]{
                new CallFrame(Thread.class, "getStackTrace"),
                new CallFrame(DumpStackTest.class, "consumeLambda"),
                new CallFrame(DumpStackTest.class, "lambda$testLambda$0"),
                new CallFrame(DumpStackTest.class, "testLambda"),
                new CallFrame(DumpStackTest.class, "main"),
                // if invoked from jtreg
                new CallFrame("jdk.internal.reflect.NativeMethodAccessorImpl", "invoke0"),
                new CallFrame("jdk.internal.reflect.NativeMethodAccessorImpl", "invoke"),
                new CallFrame("jdk.internal.reflect.DelegatingMethodAccessorImpl", "invoke"),
                new CallFrame(Method.class, "invoke"),
                new CallFrame(Thread.class, "run")
        };
        assertStackTrace(Thread.currentThread().getStackTrace(), callStack);
        DumpStackTest.getStackTrace(callStack);
    }

    static void testMethodInvoke() {
        try {
            Method m = DumpStackTest.class.getDeclaredMethod("methodInvoke");
            m.invoke(null);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void methodInvoke() {
        CallFrame[] callStack = new CallFrame[] {
                new CallFrame(Thread.class, "getStackTrace"),
                new CallFrame(DumpStackTest.class, "methodInvoke"),
                new CallFrame("jdk.internal.reflect.NativeMethodAccessorImpl", "invoke0"),
                new CallFrame("jdk.internal.reflect.NativeMethodAccessorImpl", "invoke"),
                new CallFrame("jdk.internal.reflect.DelegatingMethodAccessorImpl", "invoke"),
                new CallFrame(Method.class, "invoke"),
                new CallFrame(DumpStackTest.class, "testMethodInvoke"),
                new CallFrame(DumpStackTest.class, "main"),
                // if invoked from jtreg
                new CallFrame("jdk.internal.reflect.NativeMethodAccessorImpl", "invoke0"),
                new CallFrame("jdk.internal.reflect.NativeMethodAccessorImpl", "invoke"),
                new CallFrame("jdk.internal.reflect.DelegatingMethodAccessorImpl", "invoke"),
                new CallFrame(Method.class, "invoke"),
                new CallFrame(Thread.class, "run")
        };
        assertStackTrace(Thread.currentThread().getStackTrace(), callStack);
        DumpStackTest.getStackTrace(callStack);
    }

    static void testMethodHandle() {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            MethodHandle handle = lookup.findStatic(DumpStackTest.class, "methodHandle",
                                                    MethodType.methodType(void.class));
            handle.invoke();
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }

    static void methodHandle() {
        CallFrame[] callStack = new CallFrame[]{
                new CallFrame(Thread.class, "getStackTrace"),
                new CallFrame(DumpStackTest.class, "methodHandle"),
                new CallFrame(DumpStackTest.class, "testMethodHandle"),
                new CallFrame(DumpStackTest.class, "main"),
                // if invoked from jtreg
                new CallFrame("jdk.internal.reflect.NativeMethodAccessorImpl", "invoke0"),
                new CallFrame("jdk.internal.reflect.NativeMethodAccessorImpl", "invoke"),
                new CallFrame("jdk.internal.reflect.DelegatingMethodAccessorImpl", "invoke"),
                new CallFrame(Method.class, "invoke"),
                new CallFrame(Thread.class, "run")
        };
        assertStackTrace(Thread.currentThread().getStackTrace(), callStack);
        DumpStackTest.getStackTrace(callStack);
    }

    static void assertStackTrace(StackTraceElement[] actual, CallFrame[] expected) {
        System.out.println("--- Actual ---");
        Arrays.stream(actual).forEach(e -> System.out.println(e));
        System.out.println("--- Expected ---");
        Arrays.stream(expected).forEach(e -> System.out.println(e));

        for (int i = 0, j = 0; i < actual.length; i++) {
            // filter test framework classes
            if (actual[i].getClassName().startsWith("com.sun.javatest.regtest"))
                continue;
            assertEquals(actual[i], expected[j++], i);
        }

    }
    static void assertEquals(StackTraceElement actual, CallFrame expected, int idx) {
        if (!actual.getClassName().equals(expected.getClassName()) ||
                !actual.getFileName().equals(expected.getFileName()) ||
                !actual.getMethodName().equals(expected.getMethodName())) {
            throw new RuntimeException("StackTraceElements mismatch at index " + idx +
                ". Expected [" + expected + "], but get [" + actual + "]");
        }
    }
}
