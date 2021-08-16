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

package nsk.jdi.ObjectReference.waitingThreads;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

/**
 * The debugged applcation of the test.
 */
public class waitingthreads004a {

    //------------------------------------------------------- immutable common fields

    private static int exitStatus;
    private static ArgumentHandler argHandler;
    private static Log log;
    private static IOPipe pipe;

    //------------------------------------------------------- immutable common methods

    static void display(String msg) {
        log.display("debuggee > " + msg);
    }

    static void complain(String msg) {
        log.complain("debuggee FAILURE > " + msg);
    }

    public static void receiveSignal(String signal) {
        String line = pipe.readln();

        if ( !line.equals(signal) )
            throw new Failure("UNEXPECTED debugger's signal " + line);

        display("debuger's <" + signal + "> signal received.");
    }

    //------------------------------------------------------ mutable common fields

    //------------------------------------------------------ test specific fields

    static Object waitnotifyObj = new Object();
    static Object lockingObject = new Object();
    static final int threadCount = 5;
    static final String threadNamePrefix = "MyThread-";
    static waitingthreads004aThread[] threads = new waitingthreads004aThread[threadCount];
    static long waitTime;

    //------------------------------------------------------ mutable common method

    public static void main (String argv[]) {

        exitStatus = Consts.TEST_PASSED;
        argHandler = new ArgumentHandler(argv);
        log = new Log(System.err, argHandler);
        pipe = argHandler.createDebugeeIOPipe(log);
        waitTime = argHandler.getWaitTime() * 60000;

        pipe.println(waitingthreads004.SIGNAL_READY);

        try {
            synchronized (waitnotifyObj) {
                display("entered: synchronized (waitnotifyObj) {}");

                for (int i = 0; i < threadCount; i++) {
                    threads[i] = new waitingthreads004aThread(threadNamePrefix + i);
                    threads[i].start();
                    try {
                        waitnotifyObj.wait();
                    } catch (InterruptedException e) {
                        throw new Failure("Unexpected InterruptedException while waiting for " + threadNamePrefix + i + " start");
                    }
                }

                pipe.println(waitingthreads004.SIGNAL_GO);
                receiveSignal(waitingthreads004.SIGNAL_QUIT);
            }
            display("exited: synchronized (waitnotifyObj) {}");

            synchronized (lockingObject) {
                display("entered and notifyAll: synchronized (lockingObject) {}");
                lockingObject.notifyAll();
            }
            display("exited: synchronized (lockingObject) {}");

            for (int i = 0; i < threadCount; i++) {
                if (threads[i].isAlive()) {
                    try {
                        threads[i].join(waitTime);
                    } catch (InterruptedException e) {
                        throw new Failure("Unexpected InterruptedException while waiting for " + threadNamePrefix + i + " join");
                    }
                }
            }

//            receiveSignal(waitingthreads004.SIGNAL_QUIT);
            display("completed succesfully.");
            System.exit(exitStatus + Consts.JCK_STATUS_BASE);
        } catch (Failure e) {
            log.complain(e.getMessage());
            System.exit(Consts.TEST_FAILED + Consts.JCK_STATUS_BASE);
        }
    }

    //--------------------------------------------------------- test specific methods

}

//--------------------------------------------------------- test specific classes

class waitingthreads004aThread extends Thread {
    public waitingthreads004aThread(String threadName) {
        super(threadName);
    }

    public void run() {
        synchronized (waitingthreads004a.lockingObject) {
            display("entered and waiting: synchronized (lockingObject)");

            synchronized (waitingthreads004a.waitnotifyObj) {
                waitingthreads004a.waitnotifyObj.notifyAll();
            }

            try {
                waitingthreads004a.lockingObject.wait(waitingthreads004a.waitTime);
            } catch (InterruptedException e) {
                throw new Failure("Unexpected InterruptedException while waiting in " + Thread.currentThread().getName());
            }
        }
        display("exited:  synchronized (lockingObject)");
    }

    private void display (String str) {
        waitingthreads004a.display(Thread.currentThread().getName() + " : " + str);
    }
}
