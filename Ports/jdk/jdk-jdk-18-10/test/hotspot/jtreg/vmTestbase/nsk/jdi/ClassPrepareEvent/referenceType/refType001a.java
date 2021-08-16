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

package nsk.jdi.ClassPrepareEvent.referenceType;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import java.io.*;

// This class is the debugged application in the test

class refType001a {
    public static Object threadStartNotification = new Object();
    public static Object threadExitLock = new Object();

    public static void main(String args[]) {
        refType001a _refType001a = new refType001a();
        System.exit(refType001.JCK_STATUS_BASE + _refType001a.runIt(args, System.err));
    }

    int runIt(String args[], PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(args);
        IOPipe pipe = argHandler.createDebugeeIOPipe();
        Log log = new Log(out, argHandler);

//        long threadStartTimeout = 10 * 100; // milliseconds

        // notify debugger that debugge started
        pipe.println(refType001.COMMAND_READY);

        // wait for command RUN from debugger to create another thread
        String command = pipe.readln();
        if (!command.equals(refType001.COMMAND_RUN)) {
            log.complain("TEST BUG: Debugee: unknown command: " + command);
            return refType001.FAILED;
        }

        // prevent further started thread from exit
        synchronized (threadExitLock) {

            // create new thread
            AnotherThread anotherThread = new AnotherThread("AnotherThread");

            // start a thread and wait for notification it starts
            synchronized (threadStartNotification) {
                anotherThread.start();
                try {
                    threadStartNotification.wait();
                } catch (InterruptedException e) {
                    log.complain("Unexpected InterruptedException while waiting for thread started: " + e);
                    return refType001.FAILED;
                }
            }

            // check if thread really started and notify debugger
            if (anotherThread.started) {
                log.display("Another thread started in debuggee: " + anotherThread.getName());
                pipe.println(refType001.COMMAND_DONE);
            } else {
                log.complain("Requested thread NOT started in debuggee: " + anotherThread.getName());
                anotherThread.interrupt();
                pipe.println(refType001.COMMAND_ERROR);
            }

            // wait for command QUIT from debugger to release started threads and exit
            command = pipe.readln();
            if (!command.equals(refType001.COMMAND_QUIT)) {
                log.complain("TEST BUG: Debugee: unknown command: " + command);
                return refType001.FAILED;
            }

            // release monitor to permit started thread to finish
        }

        return refType001.PASSED;
    }
}

class AnotherThread extends Thread {
    public volatile boolean started = false;

    AnotherThread (String name) {
        super(name);
    }

    public void run() {
        ClassForAnotherThread a = new ClassForAnotherThread();

        // notify main thread that another thread started
        synchronized (refType001a.threadStartNotification) {
            started = true;
            refType001a.threadStartNotification.notify();
        }

        // wait for main thread releases mionitor to permint this thread to finish
        synchronized (refType001a.threadExitLock) {
        }

    }
}

class ClassForAnotherThread implements Inter {
    static boolean z0, z1[]={z0}, z2[][]={z1};
    static byte    b0, b1[]={b0}, b2[][]={b1};
    static char    c0, c1[]={c0}, c2[][]={c1};
    static double  d0, d1[]={d0}, d2[][]={d1};
    static float   f0, f1[]={f0}, f2[][]={f1};
    static int     i0, i1[]={i0}, i2[][]={i1};
    static long    l0, l1[]={l0}, l2[][]={l1};
    static short   s0, s1[]={s0}, s2[][]={s1};

    static private   long lP0, lP1[]={lP0}, lP2[][]={lP1};
    static public    long lU0, lU1[]={lU0}, lU2[][]={lU1};
    static protected long lR0, lR1[]={lR0}, lR2[][]={lR1};
    static transient long lT0, lT1[]={lT0}, lT2[][]={lT1};
    static volatile  long lV0, lV1[]={lV0}, lV2[][]={lV1};

    static           Inter ES0, ES1[]={ES0}, ES2[][]={ES1};
    static private   Inter EP0, EP1[]={EP0}, EP2[][]={EP1};
    static public    Inter EU0, EU1[]={EU0}, EU2[][]={EU1};
    static protected Inter ER0, ER1[]={ER0}, ER2[][]={ER1};
    static transient Inter ET0, ET1[]={ET0}, ET2[][]={ET1};
    static volatile  Inter EV0, EV1[]={EV0}, EV2[][]={EV1};
}

interface Inter {
    static final long lF0 = 999, lF1[]={lF0}, lF2[][]={lF1};
    static final Inter EF0 = null, EF1[]={EF0}, EF2[][]={EF1};
}
