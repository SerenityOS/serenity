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

package nsk.jvmti.ThreadEnd;

import java.io.PrintStream;

import nsk.share.jvmti.*;
import nsk.share.*;

public class threadend002 extends DebugeeClass {

    // run test from command line
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    // run test from JCK-compatible environment
    public static int run(String argv[], PrintStream out) {
        return new threadend002().runIt(argv, out);
    }

    Log.Logger logger;


    // run debuggee
    public int runIt(String argv[], PrintStream out) {

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        logger = new Log.Logger(new Log(out, argHandler), "debuggee> ");

        int status = threadend002.checkStatus(Consts.TEST_PASSED);

        threadend002Thread thrd = new threadend002Thread();
        thrd.start();

        try {
            thrd.join();
        } catch(InterruptedException e) {
            logger.complain("Unexpected exception " + e);
            e.printStackTrace();
            return Consts.TEST_FAILED;
        }

        int currStatus = threadend002.checkStatus(Consts.TEST_PASSED);
        if (currStatus != Consts.TEST_PASSED)
            status = currStatus;

        return status;
    }

    class threadend002Thread extends Thread {

        public void run() {
            logger.display("thread finished");
        }
    }

}
