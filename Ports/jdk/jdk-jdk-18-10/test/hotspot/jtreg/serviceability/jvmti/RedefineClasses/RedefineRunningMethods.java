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

/*
 * @test
 * @bug 8055008 8197901 8010319
 * @summary Redefine EMCP and non-EMCP methods that are running in an infinite loop
 * @requires vm.jvmti
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @modules java.compiler
 *          java.instrument
 *          jdk.jartool/sun.tools.jar
 * @run main RedefineClassHelper
 * @run main/othervm/timeout=180 -javaagent:redefineagent.jar -Xlog:redefine+class+iklass+add=trace,redefine+class+iklass+purge=trace,class+loader+data=debug,safepoint+cleanup,gc+phases=debug:rt.log RedefineRunningMethods
 */


// package access top-level class to avoid problem with RedefineClassHelper
// and nested types.
class RedefineRunningMethods_B {
    static int count1 = 0;
    static int count2 = 0;
    public static volatile boolean stop = false;
    static void localSleep() {
        try{
            Thread.currentThread().sleep(10);//sleep for 10 ms
        } catch(InterruptedException ie) {
        }
    }

    public static void infinite() {
        while (!stop) { count1++; localSleep(); }
    }
    public static void infinite_emcp() {
        while (!stop) { count2++; localSleep(); }
    }
}

public class RedefineRunningMethods {

    public static String newB =
                "class RedefineRunningMethods_B {" +
                "   static int count1 = 0;" +
                "   static int count2 = 0;" +
                "   public static volatile boolean stop = false;" +
                "  static void localSleep() { " +
                "    try{ " +
                "      Thread.currentThread().sleep(10);" +
                "    } catch(InterruptedException ie) { " +
                "    } " +
                " } " +
                "   public static void infinite() { " +
                "       System.out.println(\"infinite called\");" +
                "   }" +
                "   public static void infinite_emcp() { " +
                "       while (!stop) { count2++; localSleep(); }" +
                "   }" +
                "}";

    public static String evenNewerB =
                "class RedefineRunningMethods_B {" +
                "   static int count1 = 0;" +
                "   static int count2 = 0;" +
                "   public static volatile boolean stop = false;" +
                "  static void localSleep() { " +
                "    try{ " +
                "      Thread.currentThread().sleep(1);" +
                "    } catch(InterruptedException ie) { " +
                "    } " +
                " } " +
                "   public static void infinite() { }" +
                "   public static void infinite_emcp() { " +
                "       System.out.println(\"infinite_emcp now obsolete called\");" +
                "   }" +
                "}";


    public static void main(String[] args) throws Exception {

        new Thread() {
            public void run() {
                RedefineRunningMethods_B.infinite();
            }
        }.start();

        new Thread() {
            public void run() {
                RedefineRunningMethods_B.infinite_emcp();
            }
        }.start();

        RedefineClassHelper.redefineClass(RedefineRunningMethods_B.class, newB);

        System.gc();

        RedefineRunningMethods_B.infinite();

        // Start a thread with the second version of infinite_emcp running
        new Thread() {
            public void run() {
                RedefineRunningMethods_B.infinite_emcp();
            }
        }.start();

        for (int i = 0; i < 20 ; i++) {
            String s = new String("some garbage");
            System.gc();
        }

        RedefineClassHelper.redefineClass(RedefineRunningMethods_B.class, evenNewerB);
        System.gc();

        for (int i = 0; i < 20 ; i++) {
            RedefineRunningMethods_B.infinite();
            String s = new String("some garbage");
            System.gc();
        }

        RedefineRunningMethods_B.infinite_emcp();

        // purge should clean everything up.
        RedefineRunningMethods_B.stop = true;

        for (int i = 0; i < 20 ; i++) {
            RedefineRunningMethods_B.infinite();
            String s = new String("some garbage");
            System.gc();
        }
    }
}
