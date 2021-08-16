/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicLong;
import java.lang.StackWalker.StackFrame;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Objects;

import static java.lang.StackWalker.Option.*;

/**
 * @test
 * @bug 8140450 8197901
 * @summary Verify stack trace information obtained with respect to StackWalker
 *          options, when the stack contains lambdas, method handle invoke
 *          virtual calls, and reflection.
 * @run main/othervm VerifyStackTrace
 * @run main/othervm/java.security.policy=stackwalk.policy VerifyStackTrace
 * @author danielfuchs
 */
public class VerifyStackTrace {

    static interface TestCase {
        StackWalker walker();
        String description();
        String expected();
    }
    static final class TestCase1 implements TestCase {
        private final StackWalker walker = StackWalker.getInstance(RETAIN_CLASS_REFERENCE);

        private final String description = "StackWalker.getInstance(" +
            "StackWalker.Option.RETAIN_CLASS_REFERENCE)";

        // Note: line numbers and lambda hashes will be erased when
        //       comparing stack traces. However, the stack may change
        //       if some methods are being renamed in the code base.
        // If the  JDKcode base changes and the test fails because of that,
        // then after validating that the actual stack trace obtained
        // is indeed correct (no frames are skipped that shouldn't)
        // then you can cut & paste the <-- actual --> stack printed in the
        // test output in here:
        private final String expected =
            "1: VerifyStackTrace.lambda$test$1(VerifyStackTrace.java:209)\n" +
            "2: VerifyStackTrace$Handle.execute(VerifyStackTrace.java:145)\n" +
            "3: VerifyStackTrace$Handle.run(VerifyStackTrace.java:158)\n" +
            "4: VerifyStackTrace.invoke(VerifyStackTrace.java:188)\n" +
            "5: VerifyStackTrace$1.run(VerifyStackTrace.java:218)\n" +
            "6: java.base/java.security.AccessController.doPrivileged(AccessController.java:310)\n" +
            "7: VerifyStackTrace.test(VerifyStackTrace.java:227)\n" +
            "8: VerifyStackTrace.main(VerifyStackTrace.java:182)\n";

        @Override public StackWalker walker() { return walker;}
        @Override public String description() { return description;}
        @Override public String expected()    { return expected;}
    }
    static final class TestCase2 implements TestCase {
        private final StackWalker walker = StackWalker.getInstance(
                EnumSet.of(RETAIN_CLASS_REFERENCE, SHOW_REFLECT_FRAMES));

        private final String description = "nStackWalker.getInstance(" +
            "StackWalker.Option.RETAIN_CLASS_REFERENCE, " +
            "StackWalker.Option.SHOW_REFLECT_FRAMES)";

        // Note: line numbers and lambda hashes will be erased when
        //       comparing stack traces. However, the stack may change
        //       if some methods are being renamed in the code base.
        // If the JDK code base changes and the test fails because of that,
        // then after validating that the actual stack trace obtained
        // is indeed correct (no frames are skipped that shouldn't)
        // then you can cut & paste the <-- actual --> stack printed in the
        // test output in here (don't forget the final \n):
        private final String expected =
            "1: VerifyStackTrace.lambda$test$1(VerifyStackTrace.java:211)\n" +
            "2: VerifyStackTrace$Handle.execute(VerifyStackTrace.java:147)\n" +
            "3: VerifyStackTrace$Handle.run(VerifyStackTrace.java:160)\n" +
            "4: VerifyStackTrace.invoke(VerifyStackTrace.java:190)\n" +
            "5: java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n" +
            "6: java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n" +
            "7: java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n" +
            "8: java.base/java.lang.reflect.Method.invoke(Method.java:520)\n" +
            "9: VerifyStackTrace$1.run(VerifyStackTrace.java:220)\n" +
            "10: java.base/java.security.AccessController.doPrivileged(AccessController.java:310)\n" +
            "11: VerifyStackTrace.test(VerifyStackTrace.java:229)\n" +
            "12: VerifyStackTrace.main(VerifyStackTrace.java:185)\n";

        @Override public StackWalker walker() { return walker;}
        @Override public String description() { return description;}
        @Override public String expected()    { return expected;}
    }
    static class TestCase3 implements TestCase {
        private final StackWalker walker = StackWalker.getInstance(
                EnumSet.of(RETAIN_CLASS_REFERENCE, SHOW_HIDDEN_FRAMES));

        private final String description = "StackWalker.getInstance(" +
            "StackWalker.Option.RETAIN_CLASS_REFERENCE, " +
            "StackWalker.Option.SHOW_HIDDEN_FRAMES)";

        // Note: line numbers and lambda hashes will be erased when
        //       comparing stack traces. However, the stack may change
        //       if some methods are being renamed in the code base.
        // If the JDK code base changes and the test fails because of that,
        // then after validating that the actual stack trace obtained
        // is indeed correct (no frames are skipped that shouldn't)
        // then you can cut & paste the <-- actual --> stack printed in the
        // test output in here (don't forget the final \n):
        private final String expected =
            "1: VerifyStackTrace.lambda$test$1(VerifyStackTrace.java:213)\n" +
            "2: VerifyStackTrace$$Lambda$1/0x00000007c0089430.run(Unknown Source)\n" +
            "3: VerifyStackTrace$Handle.execute(VerifyStackTrace.java:149)\n" +
            "4: java.base/java.lang.invoke.LambdaForm$DMH/0x00000007c008a830.invokeVirtual_LL_V(LambdaForm$DMH)\n" +
            "5: java.base/java.lang.invoke.LambdaForm$MH/0x00000007c008a830.invoke_MT(LambdaForm$MH)\n" +
            "6: VerifyStackTrace$Handle.run(VerifyStackTrace.java:162)\n" +
            "7: VerifyStackTrace.invoke(VerifyStackTrace.java:192)\n" +
            "8: java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)\n" +
            "9: java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)\n" +
            "10: java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)\n" +
            "11: java.base/java.lang.reflect.Method.invoke(Method.java:520)\n" +
            "12: VerifyStackTrace$1.run(VerifyStackTrace.java:222)\n" +
            "13: java.base/java.security.AccessController.executePrivileged(AccessController.java:759)\n" +
            "14: java.base/java.security.AccessController.doPrivileged(AccessController.java:310)\n" +
            "15: VerifyStackTrace.test(VerifyStackTrace.java:231)\n" +
            "16: VerifyStackTrace.main(VerifyStackTrace.java:188)\n";

        @Override public StackWalker walker() { return walker;}
        @Override public String description() { return description;}
        @Override public String expected()    { return expected;}
    }

    static final class TestCase4 extends TestCase3 {
        private final StackWalker walker = StackWalker.getInstance(
                EnumSet.allOf(StackWalker.Option.class));

        private final String description = "StackWalker.getInstance(" +
            "StackWalker.Option.RETAIN_CLASS_REFERENCE, " +
            "StackWalker.Option.SHOW_HIDDEN_FRAMES, " +
            "StackWalker.Option.SHOW_REFLECT_FRAMES)";

        @Override public StackWalker walker() {return walker;}
        @Override public String description() {return description;}
    }

    public static class Handle implements Runnable {

        Runnable impl;
        public Handle(Runnable run) {
            this.impl = run;
        }

        public void execute(Runnable run) {
            run.run();
        }

        public void run() {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle handle = null;
            try {
                handle = lookup.findVirtual(Handle.class, "execute",
                        MethodType.methodType(void.class, Runnable.class));
            } catch(NoSuchMethodException | IllegalAccessException x) {
                throw new RuntimeException(x);
            }
            try {
                handle.invoke(this, impl);
            } catch(Error | RuntimeException x) {
                throw x;
            } catch(Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }

    static String prepare(String produced, boolean eraseSensitiveInfo) {
        if (eraseSensitiveInfo) {
            // Erase sensitive information before comparing:
            // comparing line numbers is too fragile, so we just erase them
            // out before comparing. We also erase the hash-like names of
            // synthetic frames introduced by lambdas & method handles
            return produced.replaceAll(":[1-9][0-9]*\\)", ":00)")
                    .replaceAll("/0x[0-9a-f]+\\.run", "/xxxxxxxx.run")
                    .replaceAll("/0x[0-9a-f]+\\.invoke", "/xxxxxxxx.invoke")
                    // LFs may or may not be pre-generated, making frames differ
                    .replaceAll("DirectMethodHandle\\$Holder", "LambdaForm\\$DMH")
                    .replaceAll("Invokers\\$Holder", "LambdaForm\\$MH")
                    .replaceAll("MH\\.invoke", "MH/xxxxxxxx.invoke")
                    // invoke frames may or may not have basic method type
                    // information encoded for diagnostic purposes
                    .replaceAll("xx\\.invoke([A-Za-z]*)_[A-Z_]+", "xx.invoke$1")
                    .replaceAll("\\$[0-9]+", "\\$??");
        } else {
            return produced;
        }
    }


    public static void main(String[] args) {
        test(new TestCase1());
        test(new TestCase2());
        test(new TestCase3());
        test(new TestCase4());
    }

    public static void invoke(Runnable run) {
        run.run();
    }

    static final class Recorder {
        boolean found; // stop recording after main
        public void recordSTE(long counter, StringBuilder s, StackFrame f) {
            if (found) return;
            found = VerifyStackTrace.class.equals(f.getDeclaringClass()) &&
                    "main".equals(f.getMethodName());
            String line = String.format("%d: %s", counter, f.toStackTraceElement());
            s.append(line).append('\n');
            System.out.println(line);
        }
    }


    static void test(TestCase test) {
        System.out.println("\nTesting: " + test.description());
        final AtomicLong counter = new AtomicLong();
        final StringBuilder builder = new StringBuilder();
        final Recorder recorder = new Recorder();
        final Runnable run = () -> test.walker().forEach(
                f -> recorder.recordSTE(counter.incrementAndGet(), builder, f));
        final Handle handle = new Handle(run);

        // We're not using lambda on purpose here. We want the anonymous
        // class on the stack.
        PrivilegedAction<Object> pa = new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                try {
                    return VerifyStackTrace.class
                            .getMethod("invoke", Runnable.class)
                            .invoke(null, handle);
                } catch (NoSuchMethodException
                        | IllegalAccessException
                        | InvocationTargetException ex) {
                    System.out.flush();
                    throw new RuntimeException(ex);
                }
            }
        };
        AccessController.doPrivileged(pa);
        System.out.println("Main found: " + recorder.found);
        if (!Objects.equals(prepare(test.expected(), true), prepare(builder.toString(), true))) {
            System.out.flush();
            try {
                // sleep to make it less likely that System.out & System.err will
                // interleave.
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
            }
            System.err.println("\nUnexpected stack trace: "
                    + "\n<!-- expected -->\n"
                    + prepare(test.expected(), true)
                    + "\n<--  actual -->\n"
                    + prepare(builder.toString(), false));
            throw new RuntimeException("Unexpected stack trace  for: " + test.description());
        }
    }


}
