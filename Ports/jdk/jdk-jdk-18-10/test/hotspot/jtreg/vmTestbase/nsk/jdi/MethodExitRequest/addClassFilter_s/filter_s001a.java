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

package nsk.jdi.MethodExitRequest.addClassFilter_s;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

/**
 * This class is used as debuggee application for the filter_s001 JDI test.
 */

public class filter_s001a {

    //----------------------------------------------------- templete section

    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    static ArgumentHandler argHandler;
    static Log log;

    //--------------------------------------------------   log procedures

    public static void log1(String message) {
        log.display("**> debuggee: " + message);
    }

    private static void logErr(String message) {
        log.complain("**> debuggee: " + message);
    }

    //====================================================== test program

    static Thread1filter_s001a thread1 = null;
    static Thread2filter_s001a thread2 = null;

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


        for (int i = 0; ; i++) {

            log1("methodForCommunication();");
            methodForCommunication();
            if (instruction == end)
                break;

            if (instruction > maxInstr) {
                logErr("ERROR: unexpected instruction: " + instruction);
                exitCode = FAILED;
                break ;
            }

            switch (i) {

//------------------------------------------------------  section tested

                case 0:
                thread1 = new Thread1filter_s001a("thread1");
                log1("new filter_s001a().run1(thread1);");
                new filter_s001a().run1(thread1);

                break;

                case 1:
                thread2 = new Thread2filter_s001a("thread2");
                log1("new filter_s001a().run1(thread2);");
                new filter_s001a().run1(thread2);

//-------------------------------------------------    standard end section

                default:
                instruction = end;
                break;
            }
        }

        log1("debuggee exits");
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

    public void run1(Thread t) {
        t.start();
        try {
            t.join();
        } catch ( InterruptedException e ) {
        }
    }
}

class TestClass10{
    static void m10() {
        filter_s001a.log1(" entered: m10()");
    }
}
class TestClass11 extends TestClass10{
    static void m11() {
        filter_s001a.log1(" entered: m11()");
        TestClass10.m10();
    }
}

class Thread1filter_s001a extends Thread {

    String tName = null;

    public Thread1filter_s001a(String threadName) {
        super(threadName);
        tName = threadName;
    }

    public void run() {
        filter_s001a.log1("  'run': enter  :: threadName == " + tName);
        TestClass11.m11();
        filter_s001a.log1("  'run': exit   :: threadName == " + tName);
        return;
    }
}

class Thread2filter_s001a extends Thread {

    String tName = null;

    public Thread2filter_s001a(String threadName) {
        super(threadName);
        tName = threadName;
    }

    public void run() {
        filter_s001a.log1("  'run': enter  :: threadName == " + tName);
        TestClass21.m21();
        filter_s001a.log1("  'run': exit   :: threadName == " + tName);
        return;
    }

    static class TestClass20{
        static void m20() {
            filter_s001a.log1(" entered: m20()");
        }
    }
    static class TestClass21 extends TestClass20{
        static void m21() {
            filter_s001a.log1(" entered: m21()");
            TestClass20.m20();
        }
    }
}
