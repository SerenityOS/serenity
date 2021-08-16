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

public class em02t001 extends DebugeeClass {

    static Object startingMonitor = new Object();
    static Object endingMonitor = new Object();
    static Thread debuggeeThread = null;

    // run test from command line
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    // run test from JCK-compatible environment
    public static int run(String argv[], PrintStream out) {
        return new em02t001().runIt(argv, out);
    }

    /* =================================================================== */

    // scaffold objects
    ArgumentHandler argHandler = null;
    static Log log = null;
    Log.Logger logger;
    long timeout = 0;
    int status = Consts.TEST_PASSED;

    // run debuggee
    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        logger = new Log.Logger(log,"debuggee> ");
        timeout = argHandler.getWaitTime() * 60000; // milliseconds
        logger.display("Timeout = " + timeout + " msc.");

        for (int i = 0; i < 3; i++) {
            debuggeeThread = new em02t001Thread("Debuggee Thread");

            generateEvents();

            int currStatus = em02t001.checkStatus(Consts.TEST_PASSED);
            if (currStatus != Consts.TEST_PASSED)
                status = currStatus;
        }

        return status;
    }

    public void generateEvents() {

        em02t001.checkStatus(Consts.TEST_PASSED);

        logger.display("generating events");
        synchronized (endingMonitor) {

            // run thread
            try {
                // start thread
                synchronized (startingMonitor) {
                    debuggeeThread.start();
                    startingMonitor.wait(timeout);
                }
            } catch (InterruptedException e) {
                throw new Failure(e);
            }

            while (debuggeeThread.getState() != Thread.State.BLOCKED) {
                Thread.yield();
            }

            em02t001.checkStatus(Consts.TEST_PASSED);
        }

        // wait for thread finish
        try {
            debuggeeThread.join(timeout);
        } catch (InterruptedException e) {
            throw new Failure(e);
        }

        logger.display("Sync: thread finished");
    }

    // tested threads
    class em02t001Thread extends Thread {

        public em02t001Thread(String name) {
            super(name);
        }

        public void run() {

            // notify about starting
            synchronized (startingMonitor) {
                startingMonitor.notify();
            }

            // wait until main thread release monitor
            synchronized (endingMonitor) {
            }
        }
    }
}
