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

package nsk.jdi.ObjectReference.owningThread;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

//    THIS TEST IS LINE NUMBER SENSITIVE

/**
 * The debugged application of the test.
 */
public class owningthread002a {

    //----------------------------------------------------- immutable common fields

    static final int PASSED    = 0;
    static final int FAILED    = 2;
    static final int PASS_BASE = 95;
    static final int quit      = -1;

    static int instruction = 1;
    static int lineForComm = 2;
    static int exitCode    = PASSED;

    private static ArgumentHandler argHandler;
    private static Log log;

    //---------------------------------------------------------- immutable common methods

    static void display(String msg) {
        log.display("debuggee > " + msg);
    }

    static void complain(String msg) {
        log.complain("debuggee FAILURE > " + msg);
    }

    private static void methodForCommunication() {
        int i = instruction; // owningthread002.lineForBreak
        int curInstruction = i;
    }

    //------------------------------------------------------ mutable common fields

    //------------------------------------------------------ test specific fields

    static owningthread002aLock lockObj = new owningthread002aLock();
//    static Object waitnotifyObj = new Object();
    static final int threadCount = 5;
    static final String threadNamePrefix = "MyThread-";
    static owningthread002aThread[] threads = new owningthread002aThread[threadCount];

    //------------------------------------------------------ mutable common method

    public static void main (String argv[]) {

        argHandler = new ArgumentHandler(argv);
        log = argHandler.createDebugeeLog();
        long waitTime = argHandler.getWaitTime() * 60000;

        display("debuggee started!");

        label0:
        for (int testCase = 0; instruction != quit; testCase++) {

            switch (testCase) {
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ test case section
                case 0:
                    display("call methodForCommunication() #0");
                    methodForCommunication();

                    for (int i = 0; i < threadCount; i++) {
                        threads[i] = new owningthread002aThread(threadNamePrefix + i);
                        threads[i].start();
/*
                        try {
                            waitnotifyObj.wait();
                        } catch (InterruptedException e) {
                            throw new Failure("Unexpected InterruptedException while waiting for " + threadNamePrefix + i + " start");
                        }
*/
                    }

                    for (int i = 0; i < threadCount; i++) {
                        if (threads[i].isAlive()) {
                            try {
                                threads[i].join(waitTime);
                            } catch (InterruptedException e) {
                                throw new Failure("Unexpected InterruptedException while waiting for " + threadNamePrefix + i + " join");
                            }
                        }
                    }

                    break;
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ end of section

                default:
                    instruction = quit;
                    break;
            }

            display("call methodForCommunication() #1");
            methodForCommunication();
            if (instruction == quit)
                break;
        }

        display("debuggee exits");
        System.exit(PASSED + PASS_BASE);
    }

    //--------------------------------------------------------- test specific methodss

}

//--------------------------------------------------------- test specific classes

class owningthread002aLock {
    synchronized void foo () {
    }
}

class owningthread002aThread extends Thread {
    public owningthread002aThread(String threadName) {
        super(threadName);
    }

    public void run() {
/*
        synchronized (owningthread002a.waitnotifyObj) {
            display("entered: synchronized (waitnotifyObj)");
            owningthread002a.waitnotifyObj.notifyAll();
        }
        display("exited:  synchronized (waitnotifyObj)");
*/
        owningthread002a.lockObj.foo();
        display("exited:  synchronized method foo");
    }

    private void display (String str) {
        owningthread002a.display(Thread.currentThread().getName() + " : " + str);
    }
}
