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

package nsk.jdi.ClassType.newInstance;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

//    THIS TEST IS LINE NUMBER SENSITIVE
/**
 *  <code>newinstance007a</code> is deugee's part of the newinstance007.
 */
public class newinstance007a {

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
        pipe.println(newinstance007.SGNL_READY);

        String instr = pipe.readln();
        while (!instr.equals(newinstance007.SGNL_QUIT)) {

            // create new thread and start it
            if (instr.equals(newinstance007.SGNL_STRTHRD)) {
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
            pipe.println(newinstance007.SGNL_READY);
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

        if (instr.equals(newinstance007.SGNL_QUIT)) {
            log.display("completed succesfully.");
            System.exit(Consts.TEST_PASSED + Consts.JCK_STATUS_BASE);
        }

        log.complain("DEBUGEE> unexpected signal of debugger.");
        System.exit(Consts.TEST_FAILED + Consts.JCK_STATUS_BASE);
    }

    private static long invokingTime;
    newinstance007a() {
        log.display("constructor> starting...");
        invokingTime = 0;
        try {
            synchronized(im007aThread01.waitFinishing) {
                synchronized(im007aThread01.waitInvoking) {
                    log.display("constructor> notifying to thread...");
                    im007aThread01.waitInvoking.notify();
                }

                log.display("constructor> waiting for thread's response...");
                long startTime = System.currentTimeMillis();
                im007aThread01.waitFinishing.wait(waitTime);
                invokingTime = System.currentTimeMillis() - startTime;
                return;
            }
        } catch (InterruptedException e) {
            log.display("constructor> it was interrupted.");
        }
        invokingTime = waitTime + 1;
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

            newinstance007a.log.display(getName() + "> waiting for the invoked method...");
            try {
                long startTime = System.currentTimeMillis();
                waitInvoking.wait(newinstance007a.waitTime);
                if ((System.currentTimeMillis() - startTime) < newinstance007a.waitTime) {
                    newinstance007a.log.display(getName() + "> got a response from the invoked method");
                } else {
                    newinstance007a.log.display(getName() + "*** no response from invoked method");
                }
            } catch (Exception e) {
                newinstance007a.log.display(getName() + e);
            }
        }
        synchronized(waitFinishing) {
            newinstance007a.log.display(getName() + "> notifying to invoked method...");
            waitFinishing.notify();
        }
        newinstance007a.log.display(getName() + "> thread finished");
    }
}
