/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.scenarios.bcinstr.BI04;

import java.io.PrintStream;

import nsk.share.Log;
import nsk.share.Consts;

import nsk.share.jvmti.ArgumentHandler;
import nsk.share.jvmti.DebugeeClass;

public class bi04t002 extends DebugeeClass {

    // run test from command line
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    // run test from JCK-compatible environment
    public static int run(String argv[], PrintStream out) {
        return new bi04t002().runIt(argv, out);
    }

    /* =================================================================== */

    // scaffold objects
    ArgumentHandler argHandler = null;
    Log.Logger logger;

    /* =================================================================== */

    // run debuggee
    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        Log log = new Log(out, argHandler);
        logger = new Log.Logger(log,"debuggee> ");

        if (checkStatus(Consts.TEST_PASSED) != Consts.TEST_PASSED) {
            return Consts.TEST_FAILED;
        }

        //checking that instrumenetation code works
        if (!checkInstrumentedMethods()) {
            logger.complain("Test FAILED");
            return Consts.TEST_FAILED;
        }

        logger.display("Test PASSED");
        return Consts.TEST_PASSED;
    }

    /** Checks instrumented methods. */
    boolean checkInstrumentedMethods() {
        //checking that instrumenetation code works
        boolean result = true;

        logger.display("Checking instrumented methods");
        bi04t002b thrd =  new bi04t002b();

        synchronized(bi04t002b.started) {
            thrd.start();
            try {
                bi04t002b.started.wait();
                thrd.join();
            } catch (InterruptedException e) {
            }

        }

        for (int i = 0; i < bi04t002a.TOTAL_INSTRUMENTED_METHODS; i++) {

            logger.display("instrumented method " + methodName(i)
                                    + " was invoked "
                                    + bi04t002a.invocationsCount[i] + " times");
            if (bi04t002a.invocationsCount[i] <= 0) {
                logger.complain("Unexpected value " + bi04t002a.invocationsCount[i]);
                result = false;
            }

        }
        return result;
    }

    String methodName(int idx) {
        switch (idx) {
        case bi04t002a.INSTR_EQUALS: return "equals(Object)";
        case bi04t002a.INSTR_TOSTRING: return "toString()";
        case bi04t002a.INSTR_WAIT_JI: return "wait(long, int)";
        case bi04t002a.INSTR_WAIT: return "wait()";
        }
        logger.complain("unknown method for " + idx + " index");
        return null;
    }
}

class bi04t002b extends Thread {

    Object obj = new Object();
    static Object started = new Object();

    public void run() {

        synchronized(started) {

            started.notify();

            Object obj1 = new Object();
            obj.equals(obj1);
            obj.toString();

            synchronized (obj) {
                try {
                    obj.wait(1, 1);
                } catch (InterruptedException e) {
                    // do nothing
                }
            }
        }
    }
}
