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

import nsk.share.Wicket;
import java.io.PrintStream;
import nsk.share.Consts;

/**
 * <code>ForceEarlyReturn()</code>.<br>
 * The test creates an instance of inner class <code>earlyretThread</code>
 * and starts it in a separate thread. Then the test forces early return from
 * the top frame of the thread's stack of the class <code>earlyretThread</code>.
 */
public class earlyretbase {
    static final long JAVA_BIRTH_YEAR = 1995;

    static volatile boolean earlyretDone = false;
    static volatile int errCode = Consts.TEST_PASSED;
    private PrintStream out;
    private earlyretThread earlyretThr;
    static Object barrier = new Object();
    static Wicket startingBarrier;

    static {
        try {
            System.loadLibrary("earlyretbase");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Could not load earlyretbase library");
            System.err.println("java.library.path:" +
                System.getProperty("java.library.path"));
            throw e;
        }
    }

    native static int doForceEarlyReturn(Thread earlyretThr, long valToRet);
    native static int suspThread(earlyretThread earlyretThr);
    native static int resThread(earlyretThread earlyretThr);
    native static int check();

    public static void main(String[] argv) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new earlyretbase().runIt(argv, out);
    }

    private int runIt(String argv[], PrintStream out) {
        int retCode = 0;

        this.out = out;

        earlyretThr = new earlyretThread("Tested Thread");
        startingBarrier = new Wicket();

        // start the child thread
        earlyretThr.start();
        startingBarrier.waitFor();

        // pause until the child thread exits notification-block
        synchronized (barrier) {
        }

        out.println("Going to suspend the thread...");
        retCode = suspThread(earlyretThr);
        if (retCode != Consts.TEST_PASSED) {
            out.println("TEST: failed to suspend thread");
            return Consts.TEST_FAILED;
        }

        out.println("Forcing early return...");

        // force return from a top frame of the child thread
        retCode = doForceEarlyReturn(earlyretThr, JAVA_BIRTH_YEAR);
        earlyretDone = true;
        earlyretThr.letItGo();
        if (retCode != Consts.TEST_PASSED) {
            out.println("TEST: failed to force early return");
            resThread(earlyretThr);
            return Consts.TEST_FAILED;
        }

        out.println("Going to resume the thread...");

        retCode = resThread(earlyretThr);
        if (retCode != Consts.TEST_PASSED) {
            out.println("TEST: failed to resume thread");
            return Consts.TEST_FAILED;
        }

        try {
            earlyretThr.join();
        } catch (InterruptedException e) {
            out.println("TEST INCOMPLETE: caught " + e);
            return Consts.TEST_FAILED;
        }
        return errCode + check();
    }

    // Tested thread class
    class earlyretThread extends Thread {
        private volatile boolean flag = true;

        earlyretThread(String name) {
            super(name);
        }

        public void run() {
            long retValue = 0L; // Initialized for sure
            retValue = activeMethod();
            out.println("Expected result: " + JAVA_BIRTH_YEAR
                                            + " - Java Birth Year");
            out.println("Returned result: " + retValue);
            if (retValue != JAVA_BIRTH_YEAR) {
                out.println("Wrong result returned");
                errCode = Consts.TEST_FAILED;
            }
            out.println("earlyretThread (" + this + "): exiting...");
        }

        public long activeMethod() {
            boolean complain = true; // complain in a finally block

            try {
                // notify the main thread
                synchronized (earlyretbase.barrier) {
                    out.println("earlyretThread (" + this +
                                "): notifying main thread");

                    earlyretbase.startingBarrier.unlock();
                    out.println("earlyretThread (" + this +
                                "): inside activeMethod()");
                }
                // loop until the main thread forces early return
                int i = 0;
                int n = 1000;
                while (flag) {
                    if (n <= 0) {
                        n = 1000;
                    }
                    if (i > n) {
                        i = 0;
                        n--;
                    }
                    i++;
                }
                if (earlyretbase.earlyretDone) {
                    out.println("TEST FAILED: a tested frame wasn't returned");
                    earlyretbase.errCode = Consts.TEST_FAILED;
                    complain = false;
                }
            } catch (Exception e) {
                // Should not be here after ForceEarlyReturn
                out.println("TEST FAILED: earlyretThread (" + this + "): caught " + e);
                earlyretbase.errCode = Consts.TEST_FAILED;
                complain = false;
            } finally {
                // Should not be here after ForceEarlyReturn
                if (complain) {
                    out.println("TEST FAILED: finally block was " +
                                "executed after ForceEarlyReturn()");
                    earlyretbase.errCode = Consts.TEST_FAILED;
                }
            }

            return 0L;
        }

        public void letItGo() {
            flag = false;
        }
    }
}
