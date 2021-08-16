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

package nsk.jdi.VirtualMachine.dispose;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

/**
 * This class is used as debuggee application for the dispose005 JDI test.
 */

public class dispose005a {

    //----------------------------------------------------- templete section

    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //--------------------------------------------------   log procedures

    static boolean verbMode = false;

    private static void log1(String message) {
        if (verbMode)
            System.err.println("**> dispose005a: " + message);
    }

    public static void log2(String message) {
        if (verbMode)
            System.err.println("**> " + message);
    }

    private static void logErr(String message) {
        if (verbMode)
            System.err.println("!!**> dispose005a: " + message);
    }

    //====================================================== test program
    //----------------------------------------------------   main method

    public static void main (String argv[]) {

        for (int i=0; i<argv.length; i++) {
            if ( argv[i].equals("-vbs") || argv[i].equals("-verbose") ) {
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
                         Threaddispose005a test_thread =
                             new Threaddispose005a("testedThread");
                         log2("testedThread is created");

                         label:
                         synchronized (Threaddispose005a.lockingObject) {
                             synchronized (Threaddispose005a.waitnotifyObj) {
                                 log2("main thread entered synchronized (waitnotifyObj)");
                                 log2("main thread before: test_thread.start()");
                                 test_thread.start();

                                 try {
                                     log2("main thread before:   waitnotifyObj.wait();");
                                     Threaddispose005a.waitnotifyObj.wait();
                                     log2("main thread after:    waitnotifyObj.wait();");

                                     pipe.println("checkready");
                                     instruction = pipe.readln();
                                     if (!instruction.equals("continue")) {
                                         logErr("ERROR: unexpected instruction: " + instruction);
                                         exitCode = FAILED;
                                         break label;
                                     }
//                                   pipe.println("docontinue");
                                 } catch ( Exception e2) {
                                     log2("       Exception e2 exception: " + e2 );
                                     pipe.println("waitnotifyerr");
                                 }
                             }
                         }
                         log2("mainThread is out of: synchronized (lockingObject)");


                         label2:
                         synchronized (Threaddispose005a.lockingObject2) {
                             synchronized (Threaddispose005a.waitnotifyObj2) {
                                 log2("main thread entered synchronized (waitnotifyObj2)");

                                 pipe.println("docontinue");
                                 try {
                                     log2("main thread before:   waitnotifyObj2.wait();");
                                     Threaddispose005a.waitnotifyObj2.wait();
                                     log2("main thread after:    waitnotifyObj2.wait();");

                                     pipe.println("method_invoked");
                                     instruction = pipe.readln();
                                 } catch ( Exception e2) {
                                     log2("       Exception e2 exception: " + e2 );
                                     pipe.println("waitnotifyerr");
                                 }
                             }
                         }

                         synchronized (Threaddispose005a.waitnotifyObj3) {
                             if (!instruction.equals("check_alive")) {
                                 logErr("ERROR: unexpected instruction: " + instruction);
                                 exitCode = FAILED;
                             } else {
                                 log1("checking on: testedThread.isAlive");
                                 if (!test_thread.done) {
                                     test_thread.resume();
                                     pipe.println("alive");
                                 } else {
                                     pipe.println("not_alive");
                                 }
                             }
                         }

                         break ;

    //-------------------------------------------------    standard end section

                default:
                                pipe.println("checkend");
                                break ;
                }

            } else {
                logErr("ERRROR: unexpected instruction: " + instruction);
                exitCode = FAILED;
                break ;
            }
        }

        System.exit(exitCode + PASS_BASE);
    }
}

class Threaddispose005a extends Thread {

    public Threaddispose005a(String threadName) {
        super(threadName);
    }

    public static Object waitnotifyObj  = new Object();
    public static Object lockingObject  = new Object();

    public static Object waitnotifyObj3  = new Object();

    public boolean done = false;

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


        synchronized (waitnotifyObj3) {
            runt1();

            log("method 'run' exit");

            done = true;

            return;
        }
    }

    public void runt1() {

        int i0 = 0;
        log("method 'runt1': enter");
        i0 = 1;
        log("method 'runt1': body: i0 == " + i0);
        log("method 'runt1': exit");
        return;
    }

    public static final int breakpointLineNumber1 = 3;


    public static Object waitnotifyObj2  = new Object();
    public static Object lockingObject2  = new Object();

    public static void runt2() {

        log("invoked method: 'runt2' enter");

        synchronized (waitnotifyObj2) {
            log("invoked method: entered into block:  synchronized (waitnotifyObj2)");
            waitnotifyObj2.notify();
        }
        log("invoked method: exited from block:  synchronized (waitnotifyObj2)");
        synchronized (lockingObject2) {
            log("invoked method: entered into block:  synchronized (lockingObject2)");
        }
        log("invoked method: exited from block:  synchronized (lockingObject2)");

        log("invoked method: 'runt2' exit");
        return;
    }


    static void log(String str) {
        dispose005a.log2("testedThread: " + str);
    }
}
