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

package nsk.jdi.VirtualMachine.dispose;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


/**
 * This class is used as debuggee application for the dispose003 JDI test.
 */

public class dispose003a {

    //----------------------------------------------------- templete section

    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //--------------------------------------------------   log procedures

    static boolean verbMode = false;

    private static void log1(String message) {
        if (verbMode)
            System.err.println("**> dispose003a: " + message);
    }

    public static void log2(String message) {
        if (verbMode)
            System.err.println("**> " + message);
    }

    private static void logErr(String message) {
        if (verbMode)
            System.err.println("!!**> dispose003a: " + message);
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
                         Threaddispose003a test_thread =
                             new Threaddispose003a("testedThread");
                         log1("       thread2 is created");

                         label:
                         synchronized (Threaddispose003a.lockingObject) {
                             synchronized (Threaddispose003a.waitnotifyObj) {
                                 log1("       synchronized (waitnotifyObj) { enter");
                                 log1("       before: test_thread.start()");
                                 test_thread.start();

                                 try {
                                     log1("       before:   waitnotifyObj.wait();");
                                     Threaddispose003a.waitnotifyObj.wait();
                                     log1("       after:    waitnotifyObj.wait();");
                                     pipe.println("checkready");
                                     instruction = pipe.readln();
                                     if (!instruction.equals("continue")) {
                                         logErr("ERROR: unexpected instruction: " + instruction);
                                         exitCode = FAILED;
                                         break label;
                                     }
                                     pipe.println("docontinue");
                                 } catch ( Exception e2) {
                                     log1("       Exception e2 exception: " + e2 );
                                     pipe.println("waitnotifyerr");
                                 }
                             }
                         }
                         log1("mainThread is out of: synchronized (lockingObject)");

                         while (true) {
                             instruction = pipe.readln();
                             if (instruction.equals("check_done")) {
                                 if (test_thread.isAlive()) {
                                     logErr("ERROR: thread2 thread is still alive");
                                     exitCode = FAILED;
                                 }
                                 break;
                             } else if (instruction.equals("check_alive")) {
                                 log1("checking on: thread2.isAlive");
                                 if (test_thread.isAlive()) {
                                     test_thread.resume();
                                     pipe.println("alive");
                                 } else {
                                     pipe.println("not_alive");
                                 }
                             } else {
                                 logErr("ERROR: unexpected instruction: " + instruction);
                                 exitCode = FAILED;
                                 break;
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


class Threaddispose003a extends Thread {

    public Threaddispose003a(String threadName) {
        super(threadName);
    }

    public static Object waitnotifyObj  = new Object();
    public static Object lockingObject  = new Object();


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
        log("call to the method 'runt1'");
        runt1();
        log("returned from the method 'runt1'");
        log("method 'run' exit");
        return;
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


    void log(String str) {
        dispose003a.log2("thread2: " + str);
    }
}
