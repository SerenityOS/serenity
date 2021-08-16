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

public class earlyretvoid {

    final static int NESTING_DEPTH = 8;

    static {
        try {
            System.loadLibrary("earlyretvoid");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load earlyretvoid library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native static void getReady(Class cls, int depth);
    native static int check();

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

    public static int run(String args[], PrintStream out) {
        earlyretThread earlyretThr = new earlyretThread();
        getReady(earlyretThread.class, NESTING_DEPTH + 1);

        earlyretThr.start();
        try {
            earlyretThr.join();
        } catch (InterruptedException e) {
            throw new Error("Unexpected " + e);
        }

        return check();
    }

    static class Monitor {
    }


    // Tested thread class
    static class earlyretThread extends Thread {
        Monitor mntr = null;

        public void run() {
            mntr = new Monitor();
            /* Start a chain of recursive calls with NESTING_DEPTH.
             * Then ForceEarlyReturn will be called in the JVMTI native
             * agent NESTING_DEPTH times to return from all the frames.
             * The chain of the ForceEarlyReturn calls starts at the
             * JVMTI Breakpoint event and continues at each Step event
             * until return from the first frame of the countDown.
             * The breakpoint is set in the checkPoint() method.
             */
            countDown(NESTING_DEPTH);
            checkMonitor(this, "Implicit monitor");
            checkMonitor(mntr, "Explicit monitor");
        }

        public void countDown(int nestingCount) {
            if (nestingCount > 0) {
                countDown(nestingCount - 1);
            } else {
                // This explicitly locked monitor must be unlocked after
                // ForceEarlyReturnVoid from the last countDown frame
                synchronized(mntr) {
                    checkPoint(); // A breakpoint is set in this method
                }
            }
        }

        // Dummy method to be breakpointed in the JVMTI agent.
        // The monitor 'this' is locked here implicitly
        synchronized void checkPoint() {
        }

        // Monitor must be released after last ForceEarlyReturn,
        // so IllegalMonitorState exception is expected here.
        void checkMonitor(Object obj, String monitorId) {
            try {
                obj.wait(500); // milliseconds
                System.out.println(
                  "Erorr: Strange state, didn't expect to be notified: "
                   + monitorId);
                failed = true;
            }
            catch(InterruptedException ex) {
                System.out.println(
                  "Time-out failure: Monitor was NOT released: "
                   + monitorId);
                failed = true;
            }
            catch(IllegalMonitorStateException ex) {
                System.out.println(
                  "Success: Monitor was released: "
                   + monitorId);
            }
        }
    }
}
