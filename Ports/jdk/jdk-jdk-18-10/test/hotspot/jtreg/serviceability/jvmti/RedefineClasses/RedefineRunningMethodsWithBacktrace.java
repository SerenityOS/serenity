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

/*
 * @test
 * @bug 8087315 8010319
 * @summary Get old method's stack trace elements after GC
 * @requires vm.jvmti
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @modules java.compiler
 *          java.instrument
 *          jdk.jartool/sun.tools.jar
 * @run main RedefineClassHelper
 * @run main/othervm -javaagent:redefineagent.jar RedefineRunningMethodsWithBacktrace
 */

import static jdk.test.lib.Asserts.*;

// package access top-level class to avoid problem with RedefineClassHelper
// and nested types.

class RedefineRunningMethodsWithBacktrace_B {
    static int count1 = 0;
    static int count2 = 0;
    public static volatile boolean stop = false;
    static void localSleep() {
        try {
            Thread.sleep(10);//sleep for 10 ms
        } catch(InterruptedException ie) {
        }
    }

    public static void infinite() {
        while (!stop) { count1++; localSleep(); }
    }
    public static void throwable() {
        // add some stuff to the original constant pool
        String s1 = new String ("string1");
        String s2 = new String ("string2");
        String s3 = new String ("string3");
        String s4 = new String ("string4");
        String s5 = new String ("string5");
        String s6 = new String ("string6");
        String s7 = new String ("string7");
        String s8 = new String ("string8");
        String s9 = new String ("string9");
        String s10 = new String ("string10");
        String s11 = new String ("string11");
        String s12 = new String ("string12");
        String s13 = new String ("string13");
        String s14 = new String ("string14");
        String s15 = new String ("string15");
        String s16 = new String ("string16");
        String s17 = new String ("string17");
        String s18 = new String ("string18");
        String s19 = new String ("string19");
        throw new RuntimeException("throwable called");
    }
}

public class RedefineRunningMethodsWithBacktrace {

    public static String newB =
                "class RedefineRunningMethodsWithBacktrace_B {" +
                "   static int count1 = 0;" +
                "   static int count2 = 0;" +
                "   public static volatile boolean stop = false;" +
                "  static void localSleep() { " +
                "    try{ " +
                "      Thread.sleep(10);" +
                "    } catch(InterruptedException ie) { " +
                "    } " +
                " } " +
                "   public static void infinite() { " +
                "       System.out.println(\"infinite called\");" +
                "   }" +
                "   public static void throwable() { " +
                "       throw new RuntimeException(\"throwable called\");" +
                "   }" +
                "}";

    public static String evenNewerB =
                "class RedefineRunningMethodsWithBacktrace_B {" +
                "   static int count1 = 0;" +
                "   static int count2 = 0;" +
                "   public static volatile boolean stop = false;" +
                "  static void localSleep() { " +
                "    try{ " +
                "      Thread.sleep(1);" +
                "    } catch(InterruptedException ie) { " +
                "    } " +
                " } " +
                "   public static void infinite() { }" +
                "   public static void throwable() { " +
                "       throw new RuntimeException(\"throwable called\");" +
                "   }" +
                "}";

    private static void touchRedefinedMethodInBacktrace(Throwable throwable) {
        System.out.println("touchRedefinedMethodInBacktrace: ");
        throwable.printStackTrace();  // this actually crashes with the bug in
                                      // java_lang_StackTraceElement::create()

        // Make sure that we can convert the backtrace, which is referring to
        // the redefined method, to a  StrackTraceElement[] without crashing.
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        for (int i = 0; i < stackTrace.length; i++) {
            StackTraceElement frame = stackTrace[i];
            assertNotNull(frame.getClassName(),
              "\nTest failed: trace[" + i + "].getClassName() returned null");
            assertNotNull(frame.getMethodName(),
              "\nTest failed: trace[" + i + "].getMethodName() returned null");
        }
    }

    private static Throwable getThrowableInB() {
        Throwable t = null;
        try {
            RedefineRunningMethodsWithBacktrace_B.throwable();
        } catch (Exception e) {
            t = e;
            // Don't print here because Throwable will cache the constructed stacktrace
            // e.printStackTrace();
        }
        return t;
    }


    public static void main(String[] args) throws Exception {

        new Thread() {
            public void run() {
                RedefineRunningMethodsWithBacktrace_B.infinite();
            }
        }.start();

        Throwable t1 = getThrowableInB();

        RedefineClassHelper.redefineClass(RedefineRunningMethodsWithBacktrace_B.class, newB);

        System.gc();

        Throwable t2 = getThrowableInB();

        RedefineRunningMethodsWithBacktrace_B.infinite();

        for (int i = 0; i < 20 ; i++) {
            String s = new String("some garbage");
            System.gc();
        }

        RedefineClassHelper.redefineClass(RedefineRunningMethodsWithBacktrace_B.class, evenNewerB);
        System.gc();

        Throwable t3 = getThrowableInB();

        for (int i = 0; i < 20 ; i++) {
            RedefineRunningMethodsWithBacktrace_B.infinite();
            String s = new String("some garbage");
            System.gc();
        }

        touchRedefinedMethodInBacktrace(t1);
        touchRedefinedMethodInBacktrace(t2);
        touchRedefinedMethodInBacktrace(t3);

        // purge should clean everything up.
        RedefineRunningMethodsWithBacktrace_B.stop = true;

        for (int i = 0; i < 20 ; i++) {
            RedefineRunningMethodsWithBacktrace_B.infinite();
            String s = new String("some garbage");
            System.gc();
        }
    }
}
