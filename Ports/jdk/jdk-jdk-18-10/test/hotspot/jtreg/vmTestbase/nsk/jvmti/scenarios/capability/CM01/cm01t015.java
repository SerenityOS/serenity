/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.scenarios.capability.CM01;

import java.io.PrintStream;

import nsk.share.*;
import nsk.share.jvmti.*;

public class cm01t015 extends DebugeeClass {

    // load native library if required
    static {
        loadLibrary("cm01t015");
    }

    // run test from command line
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    // run test from JCK-compatible environment
    public static int run(String argv[], PrintStream out) {
        return new cm01t015().runIt(argv, out);
    }

    /* =================================================================== */

    // scaffold objects
    ArgumentHandler argHandler = null;
    Log log = null;
    int status = Consts.TEST_PASSED;
    static long timeout = 0;

    // tested thread
    cm01t015Thread thread = null;

    // run debuggee
    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60000; // milliseconds
        log.display("Timeout = " + timeout + " msc.");

        thread = new cm01t015Thread("Debuggee Thread");

        // run thread
        try {
            // start thread
            synchronized (thread.startingMonitor) {
                thread.start();
                thread.startingMonitor.wait(timeout);
            }
            if (!thread.checkReady()) {
                throw new Failure("Unable to run thread " + thread);
            }

            // testing sync
            log.display("Testing sync: thread ready");
            if ((status = checkStatus(status)) != Consts.TEST_PASSED)
                return status;
        } catch (InterruptedException e) {
            throw new Failure(e);
        }

        log.display("Testing sync: thread finish");
        thread.letFinish();

        // wait for thread finish
        try {
            thread.join(timeout);
        } catch (InterruptedException e) {
            throw new Failure(e);
        }

        log.display("Testing sync: debuggee exit");
        return status;
    }
}

/* =================================================================== */

class cm01t015Thread extends Thread {
    public Object startingMonitor = new Object();
    private Object waitingMonitor = new Object();

    public cm01t015Thread(String name) {
        super(name);
    }

    public void run() {
        synchronized (waitingMonitor) {

            Object o = new Object();
            int i = 1;
            long l = 2;
            float f = 3.0F;
            double d = 4.0;

            // notify about starting
            synchronized (startingMonitor) {
                startingMonitor.notify();
            }

            // wait on monitor
            try {
                waitingMonitor.wait(cm01t015.timeout);
            } catch (InterruptedException ignore) {
                // just finish
            }
        }
    }

    public boolean checkReady() {
        // wait until waitingMonitor released on wait()
        synchronized (waitingMonitor) {
        }
        return true;
    }

    public void letFinish() {
        synchronized (waitingMonitor) {
            waitingMonitor.notify();
        }
    }
}
