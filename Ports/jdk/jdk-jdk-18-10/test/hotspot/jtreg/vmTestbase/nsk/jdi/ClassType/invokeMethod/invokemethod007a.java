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
 *  <code>invokemethod007a</code> is deugee's part of the invokemethod007.
 */
public class invokemethod007a {

    public final static String brkpMethodName = "main";
    public final static int brkpLineNumber = 70;
    public final static String testedThread = "im007aThread01";

    public static Log log;
    public static long waitTime;
    private static IOPipe pipe;

    public static void main (String argv[]) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(System.err, argHandler);
        waitTime = argHandler.getWaitTime() * 60000;
        pipe = argHandler.createDebugeeIOPipe(log);
        im007aThread01 thread = null;
        pipe.println(invokemethod007.SGNL_READY);

        String instr = pipe.readln();
        while (!instr.equals(invokemethod007.SGNL_QUIT)) {

            // create new thread and start it
            if (instr.equals(invokemethod007.SGNL_STRTHRD)) {
                thread = new im007aThread01("im007aThread01");
                synchronized(im007aThread01.waitStarting) {
                    thread.start();
                    try {
                        im007aThread01.waitStarting.wait(waitTime);
                        log.display("checked thread started");
                    } catch (InterruptedException e) {
                        throw new Failure("Unexpected InterruptedException while waiting for checked thread start.");
                    }
                }
            }
            log.display("sending ready signal...");
            pipe.println(invokemethod007.SGNL_READY);
            log.display("waiting signal from debugger..."); // brkpLineNumber
            instr = pipe.readln();   // this is a line for breakpoint

            if (thread.isAlive()) {
                log.display("waiting for join of : " + thread.getName());
                try {
                    thread.join(waitTime);
                } catch (InterruptedException e) {
                    throw new Failure("Unexpected InterruptedException while waiting for join of " + thread.getName());
                }
                if (thread.isAlive()) {
                    try {
                        thread.interrupt();
                    } catch (SecurityException e) {
                        throw new Failure("Cannot interrupt checked thread.");
                    }
                }
            }
        }

        if (instr.equals(invokemethod007.SGNL_QUIT)) {
            log.display("completed succesfully.");
            System.exit(Consts.TEST_PASSED + Consts.JCK_STATUS_BASE);
        }

        log.complain("DEBUGEE> unexpected signal of debugger.");
        System.exit(Consts.TEST_FAILED + Consts.JCK_STATUS_BASE);
    }

    public static long invokedMethod() {
        log.display("invokedMethod> starting...");
        try {
            synchronized(im007aThread01.waitFinishing) {
                synchronized(im007aThread01.waitInvoking) {
                    log.display("invokedMethod> notifying to thread...");
                    im007aThread01.waitInvoking.notifyAll();
                }

                log.display("invokedMethod> waiting for thread's response...");
                long startTime = System.currentTimeMillis();
                im007aThread01.waitFinishing.wait(waitTime);
                return System.currentTimeMillis() - startTime;
            }
        } catch (InterruptedException e) {
            log.display("invokedMethod> it was interrupted.");
        }
        return waitTime + 1;
    }
}

class im007aThread01 extends Thread {
    public static Object waitInvoking = new Object();
    public static Object waitStarting = new Object();
    public static Object waitFinishing = new Object();

    im007aThread01(String threadName) {
        super(threadName);
    }

    public void run() {
        synchronized(waitInvoking) {
            synchronized(waitStarting) {
                waitStarting.notifyAll();
            }

            invokemethod007a.log.display(getName() + "> waiting for the invoked method...");
            try {
                long startTime = System.currentTimeMillis();
                waitInvoking.wait(invokemethod007a.waitTime);
                if ((System.currentTimeMillis() - startTime) < invokemethod007a.waitTime) {
                    invokemethod007a.log.display(getName() + "> got a response from the invoked method");
                } else {
                    invokemethod007a.log.display(getName() + "*** no response from invoked method");
                }
            } catch (Exception e) {
                invokemethod007a.log.display(getName() + e);
            }
        }
        synchronized(waitFinishing) {
            invokemethod007a.log.display(getName() + "> notifying the invoked method...");
            waitFinishing.notifyAll();
        }
        invokemethod007a.log.display(getName() + "> thread finished");
    }
}
