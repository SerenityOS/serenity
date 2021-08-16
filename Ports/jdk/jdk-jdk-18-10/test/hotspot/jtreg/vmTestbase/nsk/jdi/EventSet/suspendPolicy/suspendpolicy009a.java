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

package nsk.jdi.EventSet.suspendPolicy;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

/**
 * This class is used as debuggee application for the suspendpolicy009 JDI test.
 */

public class suspendpolicy009a {

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

    static Threadsuspendpolicy009a thread0 = null;
    static Threadsuspendpolicy009a thread1 = null;
    static Threadsuspendpolicy009a thread2 = null;
    static Threadsuspendpolicy009a thread3 = null;
    static Threadsuspendpolicy009a thread4 = null;
    static Threadsuspendpolicy009a thread5 = null;
    static Threadsuspendpolicy009a thread6 = null;

    //------------------------------------------------------ common section

    static int exitCode = PASSED;

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
                            thread0 = new Threadsuspendpolicy009a("thread0");
                            methodForCommunication();

                            threadStart(thread0);
                            try {
                                thread0.join();
                            } catch (InterruptedException e) {
                            }
                            thread1 = new Threadsuspendpolicy009a("thread1");
                            methodForCommunication();
                            break;

                    case 1:
                            threadStart(thread1);
                            try {
                                thread1.join();
                            } catch (InterruptedException e) {
                            }
                            thread2 = new Threadsuspendpolicy009a("thread2");
                            methodForCommunication();
                            break;

                    case 2:
                            threadStart(thread2);
                            try {
                                thread2.join();
                            } catch (InterruptedException e) {
                            }
                            thread3 = new Threadsuspendpolicy009a("thread3");
                            methodForCommunication();
                            break;

                    case 3:
                            threadStart(thread3);
                            try {
                                thread3.join();
                            } catch (InterruptedException e) {
                            }
                            thread4 = new Threadsuspendpolicy009a("thread4");
                            methodForCommunication();
                            break;

                    case 4:
                            threadStart(thread4);
                            try {
                                thread4.join();
                            } catch (InterruptedException e) {
                            }
                            thread5 = new Threadsuspendpolicy009a("thread5");
                            methodForCommunication();
                            break;

                    case 5:
                            threadStart(thread5);
                            try {
                                thread5.join();
                            } catch (InterruptedException e) {
                            }
                            thread6 = new Threadsuspendpolicy009a("thread6");
                            methodForCommunication();
                            break;

                    case 6:
                            threadStart(thread6);
                            try {
                                thread6.join();
                            } catch (InterruptedException e) {
                            }
    //-------------------------------------------------    standard end section

                    default:
                                instruction = end;
                                methodForCommunication();
                                break label0;
                }
            }

        System.exit(exitCode + PASS_BASE);
    }

    static Object waitnotifyObj = new Object();

    static int threadStart(Thread t) {
        synchronized (waitnotifyObj) {
            t.start();
            try {
                waitnotifyObj.wait();
            } catch ( Exception e) {
                exitCode = FAILED;
                logErr("       Exception : " + e );
                return FAILED;
            }
        }
        return PASSED;
    }

    static class Threadsuspendpolicy009a extends Thread {

        String tName = null;

        public Threadsuspendpolicy009a(String threadName) {
            super(threadName);
            tName = threadName;
        }

        public void run() {
            log1("  'run': enter  :: threadName == " + tName);
            synchronized (waitnotifyObj) {
                    waitnotifyObj.notify();
            }
            log1("  'run': exit   :: threadName == " + tName);
            return;
        }
    }

}
