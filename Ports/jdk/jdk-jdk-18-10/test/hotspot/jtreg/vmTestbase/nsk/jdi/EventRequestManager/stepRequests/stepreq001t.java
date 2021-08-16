/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.EventRequestManager.stepRequests;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


/**
 * This is a debuggee class creating several dummy user and
 * daemon threads with own names.
 */
public class stepreq001t {
    private static ArgumentHandler argHandler;

    public static void main(String args[]) {
        System.exit(new stepreq001t().runThis(args));
    }

    private int runThis(String args[]) {
        argHandler = new ArgumentHandler(args);
        IOPipe pipe = argHandler.createDebugeeIOPipe();
        Thread  thrs[] = new Thread[stepreq001.THRDS_NUM];
        Object lockObj = new Object();
        Object readyObj = new Object();
        String cmd;

// Create several dummy threads and give them new names
        thrs[0] = Thread.currentThread();
        try {
            thrs[0].setName(stepreq001.DEBUGGEE_THRDS[0]);
        } catch (SecurityException e) {
            System.err.println("TEST FAILURE: setName: caught in debuggee "
                + e);
            pipe.println("failed");
            return stepreq001.JCK_STATUS_BASE +
                stepreq001.FAILED;
        }
// Get a monitor in order to prevent the threads from exiting
        synchronized(lockObj) {
            for (int i=1; i<stepreq001.THRDS_NUM; i++) {
                thrs[i] = new stepreq001a(readyObj, lockObj,
                    stepreq001.DEBUGGEE_THRDS[i]);
                thrs[i].setDaemon(stepreq001.DAEMON_THRDS[i]);
                if (argHandler.verbose())
                    System.out.println("Debuggee: starting thread #"
                        + i + " \"" + thrs[i].getName() + "\"");
                synchronized(readyObj) {
                    thrs[i].start();
                    try {
                        readyObj.wait(); // wait for the thread's readiness
                    } catch (InterruptedException e) {
                        System.out.println("TEST FAILURE: Debuggee: waiting for the thread "
                            + thrs[i].toString() + ": caught " + e);
                        pipe.println("failed");
                        return stepreq001.JCK_STATUS_BASE +
                            stepreq001.FAILED;
                    }
                }
                if (argHandler.verbose())
                    System.out.println("Debuggee: the thread #"
                        + i + " \"" + thrs[i].getName() + "\" started");
            }
// Now the debuggee is ready for testing
            pipe.println(stepreq001.COMMAND_READY);
            cmd = pipe.readln();
        }

// The debuggee exits
        for (int i=1; i<stepreq001.THRDS_NUM ; i++) {
            try {
                thrs[i].join(argHandler.getWaitTime()*60000);
                if (argHandler.verbose())
                    System.out.println("Debuggee: thread #"
                        + i + " \"" + thrs[i].getName() + "\" done");
            } catch (InterruptedException e) {
                System.err.println("Debuggee: joining the thread #"
                    + i + " \"" + thrs[i].getName() + "\": " + e);
            }
        }
        if (!cmd.equals(stepreq001.COMMAND_QUIT)) {
            System.err.println("TEST BUG: unknown debugger command: "
                + cmd);
            return stepreq001.JCK_STATUS_BASE +
                stepreq001.FAILED;
        }
        return stepreq001.JCK_STATUS_BASE +
            stepreq001.PASSED;
    }

    class stepreq001a extends Thread {
        private Object readyObj;
        private Object lockObj;

        stepreq001a(Object readyObj, Object obj,
                String name) {
            super(name);
            this.readyObj = readyObj;
            lockObj = obj;
        }

        public void run() {
            Thread thr = Thread.currentThread();

            synchronized(readyObj) {
                readyObj.notify(); // notify the main thread
            }
            if (argHandler.verbose())
                System.out.println("Debuggee's thread \""
                    + thr.getName() + "\": going to be blocked");
            synchronized(lockObj) {
                if (argHandler.verbose()) {
                    Thread.currentThread();
                    System.out.println("Debuggee's thread \""
                        + thr.getName() + "\": unblocked, exiting...");
                }
                return;
            }
        }
    }
}
