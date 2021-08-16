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

package nsk.jvmti.scenarios.events.EM02;

import java.io.PrintStream;

import nsk.share.*;
import nsk.share.jvmti.*;

public class em02t008 extends DebugeeClass {

    // run test from command line
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    // run test from JCK-compatible environment
    public static int run(String argv[], PrintStream out) {
        return new em02t008().runIt(argv, out);
    }

    /* =================================================================== */

    static final int STEP_NUMBER = 3;
    static Log.Logger logger;

    // run debuggee
    public int runIt(String args[], PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(args);
        logger = new Log.Logger(new Log(out, argHandler),"debuggee> ");
        int status = Consts.TEST_PASSED;
        long  timeout = argHandler.getWaitTime() * 60000; // milliseconds

        Thread thrd1, thrd2;

        for (int i = 0; i < STEP_NUMBER; i++) {
            thrd1 = new em02t008a();
            thrd1.start();

            thrd2 = new em02t008b();
            thrd2.start();

            try {
                thrd1.join(timeout);
                thrd2.join(timeout);
            } catch (InterruptedException e) {
            }

            if (checkStatus(Consts.TEST_PASSED) == Consts.TEST_FAILED) {
                status = Consts.TEST_FAILED;
            }

        }

        return status;
    }

    class em02t008a extends Thread{

        em02t008a() {
            setName("em02t008a");
        }

        public void run() {
            throw new NullPointerException("checking1");
        }
    }

    class em02t008b extends Thread{

        em02t008b() {
            setName("em02t008b");
        }


        public void run() {
            try {
                throw new NullPointerException("checking2");
            } catch (NullPointerException e) {
                logger.display("Caught exception: " + e);
            }
        }
    }
}
