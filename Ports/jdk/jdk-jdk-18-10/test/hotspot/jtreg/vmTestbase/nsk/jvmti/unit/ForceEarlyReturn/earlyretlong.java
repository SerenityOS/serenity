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

public class earlyretlong {

    final static int  NESTING_DEPTH = 8;
    final static long RETVAL_BASE   = 100001000010000L;

    static {
        try {
            System.loadLibrary("earlyretlong");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load earlyretlong library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    static boolean failed = false;

    native static void getReady(Class cls, int depth, long retval_base);
    native static int check();
    native static void printLong(long val);


    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        int errCode = run(args, System.out);
        if (failed) {
            errCode = Consts.TEST_FAILED;
        }
        System.exit(errCode + Consts.JCK_STATUS_BASE);
    }

    public static int run(String args[], PrintStream out) {
        earlyretThread earlyretThr = new earlyretThread();
        getReady(earlyretThread.class, NESTING_DEPTH + 1, RETVAL_BASE);

        earlyretThr.start();
        try {
            earlyretThr.join();
        } catch (InterruptedException e) {
            throw new Error("Unexpected " + e);
        }

        return check();
    }

    // Tested thread class
    static class earlyretThread extends Thread {
        public void run() {
            long val0 = 9000009; // arbitrary value
            /* Start a chain of recursive calls with NESTING_DEPTH.
             * Then ForceEarlyReturn will be called in the JVMTI native
             * agent NESTING_DEPTH times to return from all the frames.
             * The chain of the ForceEarlyReturn calls starts at the
             * JVMTI Breakpoint event and continues at each Step event
             * until return from the first frame of the countDownLong.
             * The breakpoint is set in the checkPoint() method.
             */
            long val1 = countDownLong(NESTING_DEPTH);
            long val2 = 2222222; // arbitrary value

            earlyretlong.printLong(val1);

            if (val1 != RETVAL_BASE + NESTING_DEPTH + 1) {
                System.out.println(" Falure: wrong value returned: " + val1);
                failed = true;
            } else {
                System.out.println(" Success: right value returned: " + val1);
            }
        }

        public long countDownLong(int nestingCount) {
            long ret = 0;
            if (nestingCount > 0) {
                ret = countDownLong(nestingCount - 1);
            } else {
                checkPoint(); // A breakpoint is set in this method
            }
            return ret;
        }

        // dummy method to be breakpointed in the JVMTI agent
        void checkPoint() {
        }
    }
}
