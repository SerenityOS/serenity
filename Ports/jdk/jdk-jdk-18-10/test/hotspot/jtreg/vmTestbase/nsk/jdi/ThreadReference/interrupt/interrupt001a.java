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

package nsk.jdi.ThreadReference.interrupt;

import nsk.share.jpda.*;
import nsk.share.jdi.*;

/**
 * This class is used as debuggee application for the interrupt001 JDI test.
 */

public class interrupt001a {

    //----------------------------------------------------- templete section

    static final int PASSED = 0;

    static final int FAILED = 2;

    static final int PASS_BASE = 95;

    //--------------------------------------------------   log procedures

    static boolean verbMode = false;

    public static void log1(String message) {
        if (verbMode)
            System.err.println("**> mainThread: " + message);
    }

    public static void log2(String message) {
        if (verbMode)
            System.err.println("**> " + message);
    }

    private static void logErr(String message) {
        if (verbMode)
            System.err.println("ERROR: " + message);
    }

    //====================================================== test program

    // scaffold objects
    private static volatile ArgumentHandler argumentHandler = null;

    private static interrupt001aThread thread2 = null;

    private static interrupt001aThread thread3 = null;

    private static IOPipe pipe;

    //----------------------------------------------------   main method

    public static void main(String argv[]) {

        int exitCode = PASSED;

        argumentHandler = new ArgumentHandler(argv);

        for (int i = 0; i < argv.length; i++) {
            if (argv[i].equals("-vbs") || argv[i].equals("-verbose")) {
                verbMode = true;
                break;
            }
        }
        log1("debuggee started!");

        // informing a debugger of readyness
        pipe = argumentHandler.createDebugeeIOPipe();
        pipe.println("ready");

        label0: for (int i = 0;; i++) {

            log1("waiting for an instruction from the debugger ...");
            String instruction = pipe.readln();

            if (instruction.equals("quit")) {
                log1("'quit' recieved");
                break;

            } else if (instruction.equals("newcheck")) {

                switch (i) {
                case 0:
                    synchronized (interrupt001aThread.lockingObject) {
                        thread2 = threadStart("Thread02");
                        thread3 = threadStart("Thread03");

                        pipe.println("checkready");
                        if (checkInterruptStatus() == FAILED) {
                            exitCode = FAILED;
                            break label0;
                        }
                    }
                    log1("mainThread is out of: synchronized (lockingObject)");

                    if (waitThreadJoin(thread2) == FAILED) {
                        exitCode = FAILED;
                    }
                    if (waitThreadJoin(thread3) == FAILED) {
                        exitCode = FAILED;
                    }

                    instruction = pipe.readln();
                    if (!instruction.equals("continue")) {
                        logErr("Unexpected instruction #1: " + instruction);
                        exitCode = FAILED;
                        break label0;
                    }
                    pipe.println("docontinue");
                    break;

                case 1:
                    synchronized (interrupt001aThread.lockingObject) {
                        thread2 = threadStart("Thread12");
                        thread3 = threadStart("Thread13");

                        log1("suspending Thread2");
                        thread2.suspend();

                        log1("suspending Thread3");
                        thread3.suspend();

                        log1("interrupting the Thread3");
                        thread3.interrupt();

                        pipe.println("checkready");
                        if (checkInterruptStatus() == FAILED) {
                            exitCode = FAILED;
                            break label0;
                        }
                    }
                    log1("mainThread is out of: synchronized (lockingObject)");

                    log1("resuming Thread2");
                    thread2.resume();
                    if (waitThreadJoin(thread2) == FAILED) {
                        exitCode = FAILED;
                    }
                    log1("resuming Thread3");
                    thread3.resume();
                    if (waitThreadJoin(thread3) == FAILED) {
                        exitCode = FAILED;
                    }

                    instruction = pipe.readln();
                    if (!instruction.equals("continue")) {
                        logErr("Unexpected instruction #2: " + instruction);
                        exitCode = FAILED;
                        break label0;
                    }
                    pipe.println("docontinue");
                    break;

                //-------------------------------------------------    standard end section
                }

            } else {
                logErr("Unexpected instruction #0: " + instruction);
                exitCode = FAILED;
                break;
            }
        }

        System.exit(exitCode + PASS_BASE);
    }

    private static interrupt001aThread threadStart(String threadName) {
        interrupt001aThread resultThread = new interrupt001aThread(threadName);
        synchronized (resultThread.waitnotifyObj) {
            resultThread.start();
            try {
                log1("       before:   waitnotifyObj.wait();");
                while (!resultThread.ready)
                    resultThread.waitnotifyObj.wait();
                log1("       after:    waitnotifyObj.wait();");
            } catch (InterruptedException e) {
                logErr("Unexpected InterruptedException while waiting for start of : " + threadName);
            }
        }
        return resultThread;
    }

    private static int waitThreadJoin(Thread testedThread) {
        long timeout = argumentHandler.getWaitTime() * 60 * 1000; // milliseconds

        if (testedThread.isAlive()) {
            // wait for thread finished in a waittime interval
            log1("Waiting for tested thread " + testedThread.getName() + " finished for timeout: " + timeout);
            try {
                testedThread.join(timeout);
            } catch (InterruptedException e) {
                logErr("Interruption while waiting for tested thread " + testedThread.getName() + " finished:\n\t" + e);
                return interrupt001a.FAILED;
            }
        }
        return interrupt001a.PASSED;
    }

    private static int checkInterruptStatus() {
        log1("waiting for 'check_interruption'");
        String instruction = pipe.readln();
        if (!instruction.equals("check_interruption")) {
            logErr("Unexpected instruction #3: " + instruction);
            return FAILED;
        }

        log1("interrupting thread3");
        thread3.interrupt();

        log1("checking up statuses of (checked) Thread2 and Thread3");
        if (thread2.isInterrupted()) {
            if (thread3.isInterrupted()) {
                pipe.println(interrupt001.EQUALS_INTERRUPTED);
                log1("    " + interrupt001.EQUALS_INTERRUPTED);
            } else {
                pipe.println(interrupt001.ONLY_CHECKED_INTERRUPTED);
                log1("    " + interrupt001.ONLY_CHECKED_INTERRUPTED);
            }
        } else if (thread3.isInterrupted()) {
            pipe.println(interrupt001.CHECKED_NOT_INTERRUPTED);
            log1("    " + interrupt001.CHECKED_NOT_INTERRUPTED);
        } else {
            pipe.println(interrupt001.EQUALS_NOT_INTERRUPTED);
            log1("    " + interrupt001.EQUALS_NOT_INTERRUPTED);
        }
        return PASSED;
    }
}

class interrupt001aThread extends Thread {

    public interrupt001aThread(String threadName) {
        super(threadName);
    }

    public boolean ready;

    public Object waitnotifyObj = new Object();

    public static Object lockingObject = new Object();

    public void run() {
        log("started");
        synchronized (waitnotifyObj) {
            log("entered into block:  synchronized (waitnotifyObj)");
            ready = true;
            waitnotifyObj.notifyAll();
        }

        synchronized (lockingObject) {
            log("entered into block:  synchronized (lockingObject)");
        }
        log("exited from block:  synchronized (lockingObject)");

        log("exited");
        return;
    }

    void log(String str) {
        interrupt001a.log2(Thread.currentThread().getName() + " : " + str);
    }
}
