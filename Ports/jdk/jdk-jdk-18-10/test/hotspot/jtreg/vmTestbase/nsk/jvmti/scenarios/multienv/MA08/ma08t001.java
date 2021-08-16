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

package nsk.jvmti.scenarios.multienv.MA08;

import java.io.PrintStream;

import nsk.share.*;
import nsk.share.jvmti.*;

public class ma08t001 extends DebugeeClass {

    // run test from command line
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    // run test from JCK-compatible environment
    public static int run(String argv[], PrintStream out) {
        return new ma08t001().runIt(argv, out);
    }

    /* =================================================================== */

    // scaffold objects
    ArgumentHandler argHandler = null;
    int status = Consts.TEST_PASSED;
    Log log = null;
    long timeout = 0;

    // tested threads
    ma08t001Thread threadForStop = null;
    ma08t001Thread threadForInterrupt = null;

    // run debuggee
    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000;

        log.display("Debugee started");

        threadForStop =
            new ma08t001Thread("DebuggeeThreadForStop", timeout);
        threadForInterrupt =
            new ma08t001Thread("DebuggeeThreadForInterrupt", timeout);

        log.display("Starting tested threads");
        threadForStop.start();
        threadForInterrupt.start();

        threadForStop.startingBarrier.waitFor();
        threadForInterrupt.startingBarrier.waitFor();
        status = checkStatus(status);

        log.display("Finishing tested threads");
        try {
            threadForStop.join(timeout);
            threadForInterrupt.join(timeout);
        } catch (InterruptedException e) {
            throw new Failure(e);
        }

        log.display("Debugee finished");

        return checkStatus(status);
    }
}

/* =================================================================== */

class ma08t001Thread extends Thread {
    Wicket startingBarrier = new Wicket();
    private long timeout = 0;

    public ma08t001Thread(String name, long tout) {
        super(name);
        timeout = tout;
    }

    public synchronized void run() {
        startingBarrier.unlock();
        try {
            wait(timeout);
        } catch (InterruptedException e) {
        }
    }
}
