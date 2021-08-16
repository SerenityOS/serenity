/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.ThreadReference.isSuspended;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


/**
 * This class is used as debuggee application for the issuspended002 JDI test.
 */

public class issuspended002a {

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
            System.err.println("!!**> mainThread: " + message);
    }

    //====================================================== test program
    //----------------------------------------------------   main method

    public static void main (String argv[]) {

        for (int i = 0; i < argv.length; i++) {
            if (argv[i].equals("-vbs") || argv[i].equals("-verbose")) {
                verbMode = true;
                break;
            }
        }
        log1("debuggee started!");

        // informing a debugger of readyness
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        IOPipe pipe = argHandler.createDebugeeIOPipe();
        pipe.println("ready");

        int exitCode = PASSED;
        for (int i = 0; ; i++) {
            String instruction;

            log1("waiting for an instruction from the debugger ...");
            instruction = pipe.readln();
            if (instruction.equals("quit")) {
                log1("'quit' recieved");
                break ;

            } else if (instruction.equals("newcheck")) {
                switch (i) {

    //------------------------------------------------------  section tested

                case 0:
                    Threadissuspended002a test_thread =
                        new Threadissuspended002a("testedThread");
                    log1("       thread2 is created");
                label:
                    synchronized (Threadissuspended002a.lockingObject) {
                        synchronized (Threadissuspended002a.waitnotifyObj) {
                            log1("       synchronized (waitnotifyObj) { enter");
                            log1("       before: test_thread.start()");
                            test_thread.start();

                            try {
                                log1("       before:   waitnotifyObj.wait();");
                                Threadissuspended002a.waitnotifyObj.wait();
                                log1("       after:    waitnotifyObj.wait();");
                                pipe.println("checkready");
                                instruction = pipe.readln();
                                if (!instruction.equals("continue")) {
                                    logErr("ERROR: unexpected instruction: " + instruction);
                                    exitCode = FAILED;
                                    break label;
                                }
                            } catch ( Exception e2) {
                                log1("       Exception e2 exception: " + e2 );
                                pipe.println("waitnotifyerr");
                            }
                        }
                    }
                    log1("mainThread is out of: synchronized (lockingObject) {");

                    // To avoid contention for the logging print stream lock
                    // the main thread will wait until the debuggee test thread
                    // has completed to the breakpoint.
                    // If the debugger suspends the main thread while it is holding
                    // the logging lock, the test is deadlocked on the test thread log
                    // calls.
                    synchronized (Threadissuspended002a.waitnotifyObj) {
                        try {
                            // Main debuggee thread is out of synchronized(lockingObject)
                            // so the test thread will continue.
                            // Send the debugger the "docontinue" command
                            // so it will enable the breakpoint in the test thread.
                            pipe.println("docontinue");

                            Threadissuspended002a.waitnotifyObj.wait();
                        } catch (Exception e3) {
                            pipe.println("waitnotifyerr");
                        }
                    }

                    break ;

    //-------------------------------------------------    standard end section

                default:
                    pipe.println("checkend");
                    break ;
                }

            } else {
                logErr("ERROR: unexpected instruction: " + instruction);
                exitCode = FAILED;
                break ;
            }
        }
        System.exit(exitCode + PASS_BASE);
    }
}

class Threadissuspended002a extends Thread {

    public Threadissuspended002a(String threadName) {
        super(threadName);
    }

    public static Object waitnotifyObj = new Object();
    public static Object lockingObject = new Object();

    private int i1 = 0, i2 = 10;

    public void run() {
        log("method 'run' enter");
        synchronized (waitnotifyObj) {
            log("entered into block:  synchronized (waitnotifyObj)");
            waitnotifyObj.notify();
        }
        log("exited from block:  synchronized (waitnotifyObj)");
        synchronized (lockingObject) {
            log("entered into block:  synchronized (lockingObject)");
        }
        log("exited from block:  synchronized (lockingObject)");
        i1++;
        i2--;
        log("call to the method 'runt1'");
        runt1();
        log("returned from the method 'runt1'");
        log("method 'run' exit");

        // Allow the main thread to continue
        synchronized (waitnotifyObj) {
            waitnotifyObj.notify();
        }
        return;
    }

    public void runt1() {
        log("method 'runt1' enter");
        i1++;
        i2--;
        log("call to the method 'runt2'");
        runt2();
        log("returned from the method 'runt2'");
        i1++;
        i2--;
        log("method 'runt1' exit");
        return;
    }

    public void runt2() {
        log("method 'runt2' enter 1");
        i1++;
        i2--;
        log("method 'run2t' exit");
        return;
    }

    public static final int breakpointLineNumber1 = 3;
    public static final int breakpointLineNumber3 = 7;

    public static final int breakpointLineNumber2 = 2;

    void log(String str) {
        issuspended002a.log2("thread2: " + str);
    }

}
