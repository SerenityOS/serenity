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

package nsk.jdi.ThreadReference.popFrames;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

/**
 * This class is used as debuggee application for the popframes001 JDI test.
 */

public class popframes001a {

    //----------------------------------------------------- templete section

    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    static ArgumentHandler argHandler;
    static Log log;

    //--------------------------------------------------   log procedures

    private static void log1(String message) {
        log.display("**> debuggee: " + message);
    }

    private static void logErr(String message) {
        log.complain("**> debuggee: " + message);
    }

    //====================================================== test program

    static Threadpopframes001a thread2 = null;

    //------------------------------------------------------ common section
    static int instruction = 1;
    static int end         = 0;
                                   //    static int quit        = 0;
                                   //    static int continue    = 2;
    static int maxInstr    = 1;    // 2;

    static int lineForComm = 2;

    private static void methodForCommunication() {
        int i1 = instruction;
        int i2 = i1;
        int i3 = i2;
    }
    //----------------------------------------------------   main method

    public static void main (String argv[]) {

        argHandler = new ArgumentHandler(argv);
        log = argHandler.createDebugeeLog();

        log1("debuggee started!");

        int exitCode = PASSED;


        label0:
            for (int i = 0; ; i++) {

                if (instruction > maxInstr) {
                    logErr("ERROR: unexpected instruction: " + instruction);
                    exitCode = FAILED;
                    break ;
                }

                switch (i) {

    //------------------------------------------------------  section tested

                    case 0:
                               thread2 =  new Threadpopframes001a("thread2");
                               log1("       thread2 is created");

                               synchronized (lockingObject) {
                                   synchronized (waitnotifyObj) {
                                       log1("       thread2.start()");
                                       thread2.start();

                                       try {
                                           log1("       before:   waitnotifyObj.wait();");
                                           waitnotifyObj.wait();
                                           log1("       after:    waitnotifyObj.wait();");
                                       } catch ( Exception e2) {
                                           log1("       Exception e2 exception: " + e2 );
                                           exitCode = FAILED;
                                           break label0;
                                       }
                                   }
                                   methodForCommunication();
                               }

                               synchronized (lockingObject2) {
                                   log1("mainThread is in: synchronized (lockingObject2)");
                               }

                                break ;

    //-------------------------------------------------    standard end section

                    default:
                                instruction = end;

                                log1("before: methodForCommunication();");
                                methodForCommunication();
                                log1("after:  methodForCommunication();");

                                break label0;
                }
            }

        System.exit(exitCode + PASS_BASE);
    }

    static int testVar1 = 0;
    static int testVar2 = 0;
    static int breakpointLine = 4;

    static void poppedMethod() {
        log1("poppedMethod:  enter");
        testVar1 +=1;
        testVar2 +=1;
        testVar2 +=1;

        log1("poppedMethod:  exit");
        return;
    }

    static Object waitnotifyObj = new Object();
    static Object lockingObject = new Object();

    static Object lockingObject2 = new Object();

    static class Threadpopframes001a extends Thread {

        public Threadpopframes001a(String threadName) {
            super(threadName);
        }

        public void run() {
            synchronized (lockingObject2) {
                log1("thread2: method 'run' enter");
                synchronized (waitnotifyObj) {
                    log1("thread2: entered into block:  synchronized (waitnotifyObj)");
                    waitnotifyObj.notify();
                }
                log1("thread2: exited from block:  synchronized (waitnotifyObj)");
                synchronized (lockingObject) {
                    log1("thread2: entered into block:  synchronized (lockingObject)");
                }
                log1("thread2: exited from block:  synchronized (lockingObject)");

                log1("thread2: before: 'poppedMethod()'");
                poppedMethod();
                log1("thread2: after:  'poppedMethod()'");
            }
            return;
        }
    }

}
