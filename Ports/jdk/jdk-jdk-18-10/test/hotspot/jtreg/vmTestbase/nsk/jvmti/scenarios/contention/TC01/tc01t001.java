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

package nsk.jvmti.scenarios.contention.TC01;

import java.io.PrintStream;

import nsk.share.*;
import nsk.share.jvmti.*;

public class tc01t001 extends DebugeeClass {

    // run test from command line
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    // run test from JCK-compatible environment
    public static int run(String argv[], PrintStream out) {
        return new tc01t001().runIt(argv, out);
    }

    /* =================================================================== */

    // scaffold objects
    ArgumentHandler argHandler = null;
    Log log = null;
    int status = Consts.TEST_PASSED;
    static long timeout = 0;

    // tested thread
    tc01t001Thread thread = null;

    // run debuggee
    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000;
        log.display("Timeout = " + timeout + " msc.");

        thread = new tc01t001Thread("Debuggee Thread");
        thread.start();
        thread.startingBarrier.waitFor();

        status = checkStatus(status);
        thread.endingBarrier.unlock();

        // wait for thread finish
        try {
            thread.join(timeout);
        } catch (InterruptedException e) {
            throw new Failure(e);
        }

        log.display("Debugee finished");
        return status;
    }
}

/* =================================================================== */

class tc01t001Thread extends Thread {
    public Wicket startingBarrier = new Wicket();
    public Wicket endingBarrier = new Wicket();
    public Object M1 = new Object();
    public Object M2 = new Object();

    public tc01t001Thread(String name) {
        super(name);
    }

    public void run() {
        synchronized (M1) {
            synchronized (M2) {
                synchronized (M1) {
                    synchronized (M2) {
                        // notify main thread about starting
                        startingBarrier.unlock();
                        // wait until main thread let us go
                        endingBarrier.waitFor();
                    }
                }
            }
        }
    }
}
