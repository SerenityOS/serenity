/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.unit.ForceEarlyReturn;

import java.io.PrintStream;
import nsk.share.Consts;

public class earlyretobj {

    final static int NESTING_DEPTH = 4;

    static {
        try {
            System.loadLibrary("earlyretobj");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load earlyretobj library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native static void getReady(Class cls, int depth, Object retObj);
    native static int check();
    native static void printObject(Object obj);

    static boolean failed = false;

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        int errCode = run(args, System.out);
        if (failed) {
            errCode = Consts.TEST_FAILED;
        }
        System.exit(errCode + Consts.JCK_STATUS_BASE);
    }

    static RetObj expObj = new RetObj("Expected object");
    public static int run(String args[], PrintStream out) {
        earlyretThread earlyretThr = new earlyretThread();
        getReady(earlyretThread.class, NESTING_DEPTH + 1, expObj);

        earlyretThr.start();
        try {
            earlyretThr.join();
        } catch (InterruptedException e) {
            throw new Error("Unexpected " + e);
        }

        return check();
    }

    static class RetObj {
        String objName = null;
        RetObj(String str) {
            this.objName = str;
        }
        boolean equals(RetObj obj) {
           return objName.equals(obj.objName);
        }
        void printName(String str) {
            System.out.println(str + ": \"" + objName + "\"");
        }
    }

    // Tested thread class
    static class earlyretThread extends Thread {
        public void run() {
            /* Start a chain of recursive calls with NESTING_DEPTH.
             * Then ForceEarlyReturn will be called in the JVMTI native
             * agent NESTING_DEPTH times to return from all the frames.
             * The chain of the ForceEarlyReturn calls starts at the
             * JVMTI Breakpoint event and continues at each Step event
             * until return from the first frame of the countDownObject.
             * The breakpoint is set in the checkPoint() method.
             */
            RetObj retObj = countDownObject(NESTING_DEPTH);
            earlyretobj.printObject((Object) retObj);
            System.out.println();
            retObj.printName("Returned object name");
            if (retObj.equals(expObj)) {
                System.out.println("Expected object returned");
            } else {
                failed = true;
                System.out.println("Unexpected object returned");
            }
        }

        public RetObj countDownObject(int nestingCount) {
            RetObj obj = null;
            if (nestingCount > 0) {
                obj = countDownObject(nestingCount - 1);
            } else {
                checkPoint(); // A breakpoint is set in this method
            }
            return obj;
        }

        // dummy method to be breakpointed in the JVMTI agent
        void checkPoint() {
        }
    }
}
