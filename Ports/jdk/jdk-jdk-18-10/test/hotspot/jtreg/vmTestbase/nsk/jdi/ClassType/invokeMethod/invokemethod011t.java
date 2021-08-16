/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.ClassType.invokeMethod;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

//    THIS TEST IS LINE NUMBER SENSITIVE

/**
 * This is debuggee class.
 */
public class invokemethod011t {
    static Log log;
    private invokemethod011Thr thrs[] =
        new invokemethod011Thr[invokemethod011.THRDS_NUM-1];
    private IOPipe pipe;

    public static void main(String args[]) {
        System.exit(run(args) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String args[]) {
        return new invokemethod011t().runIt(args);
    }

    private int runIt(String args[]) {
        ArgumentHandler argHandler = new ArgumentHandler(args);
        pipe = argHandler.createDebugeeIOPipe();
        log = argHandler.createDebugeeLog();
        Thread.currentThread().setName(invokemethod011.DEBUGGEE_THRNAMES[0]);
        // start several dummy threads with given names
        startThreads();

        pipe.println(invokemethod011.COMMAND_READY);
        String cmd = pipe.readln();
        if (cmd.equals(invokemethod011.COMMAND_QUIT)) {
            log.complain("Debuggee: exiting due to the command "
                + cmd);
            killThreads(argHandler.getWaitTime()*60000);
            return Consts.TEST_PASSED;
        }

        int stopMeHere = 0; // invokemethod011.DEBUGGEE_STOPATLINE

        cmd = pipe.readln();
        killThreads(argHandler.getWaitTime()*60000);
        if (!cmd.equals(invokemethod011.COMMAND_QUIT)) {
            log.complain("TEST BUG: unknown debugger command: "
                + cmd);
            System.exit(Consts.JCK_STATUS_BASE +
                Consts.TEST_FAILED);
        }

        return Consts.TEST_PASSED;
    }

    /*
     * Dummy fields and method used to check the flag INVOKE_SINGLE_THREADED
     * in the debugger.
     */
    // used by the debugger to let the invoked method exit
    static volatile boolean doExit = false;
    // indicator of the method invocation
    static volatile boolean isInvoked = false;

    static long dummyMeth(long l) throws InterruptedException {
        invokemethod011t.log.display("dummyMeth: going to loop");
        isInvoked = true;
        while(!doExit) {
            l--; l++;
            Thread.currentThread().sleep(400);
        }
        invokemethod011t.log.display("dummyMeth: exiting");
        isInvoked = false;
        doExit = false;
        return l;
    }

    private void startThreads() {
        Object readyObj = new Object();

        for (int i=0; i<invokemethod011.THRDS_NUM-1; i++) {
            thrs[i] = new invokemethod011Thr(readyObj,
                invokemethod011.DEBUGGEE_THRNAMES[i+1]);
            thrs[i].setDaemon(true);
            log.display("Debuggee: starting thread #"
                + i + " \"" + thrs[i].getName() + "\" ...");
            synchronized(readyObj) {
                thrs[i].start();
                try {
                    readyObj.wait(); // wait for the thread's readiness
                } catch (InterruptedException e) {
                    log.complain("TEST FAILURE: Debuggee: waiting for the thread "
                        + thrs[i].toString() + " start: caught " + e);
                    pipe.println("failed");
                    System.exit(Consts.JCK_STATUS_BASE +
                        Consts.TEST_FAILED);
                }
            }
            log.display("Debuggee: the thread #"
                + i + " \"" + thrs[i].getName() + "\" started");
        }
    }

    private void killThreads(int waitTime) {
        for (int i=0; i < invokemethod011.THRDS_NUM-1 ; i++) {
            thrs[i].doExit = true;
            try {
                thrs[i].join(waitTime);
                log.display("Debuggee: thread #"
                    + i + " \"" + thrs[i].getName() + "\" done");
            } catch (InterruptedException e) {
                log.complain("TEST FAILURE: Debuggee: joining the thread #"
                    + i + " \"" + thrs[i].getName() + "\": caught " + e);
            }
        }
    }

   /**
    * This is an auxiliary thread class used to check the flag
    * INVOKE_SINGLE_THREADED in the debugger.
    */
    class invokemethod011Thr extends Thread {
        volatile boolean doExit = false;
        private Object readyObj;

        invokemethod011Thr(Object readyObj, String name) {
            super(name);
            this.readyObj = readyObj;
        }

        public void run() {
            Thread thr = Thread.currentThread();
            Object waitObj = new Object();

            synchronized(readyObj) {
                readyObj.notify(); // notify the main thread
            }
            log.display("Debuggee thread \""
                    + thr.getName() + "\": going to loop");
            while(!doExit) {
                int i = 0;
                i++; i--;

                // reliable analogue of Thread.yield()
                synchronized(waitObj) {
                    try {
                        waitObj.wait(30);
                    } catch (InterruptedException e) {
                        e.printStackTrace(log.getOutStream());
                        log.complain("TEST FAILURE: Debuggee thread \""
                            + thr.getName()
                            + "\" interrupted while sleeping:\n\t" + e);
                        break;
                    }
                }
            }
            log.display("Debuggee thread \""
                + thr.getName() + "\" exiting ...");
        }
    }
/////////////////////////////////////////////////////////////////////////////

}
