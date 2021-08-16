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

package nsk.jdi.ExceptionRequest.addClassFilter_rt;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

/**
 * This class is used as debuggee application for the filter_rt001 JDI test.
 */

public class filter_rt001a {

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

    static filter_rt001aThread1 thread1 = new filter_rt001aThread1("thread1");
    static filter_rt001aThread2 thread2 = new filter_rt001aThread2("thread2");

    static filter_rt001aTestClass11 obj = new filter_rt001aTestClass11();
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
                log1("new filter_rt001a().run1(thread1);");
                new filter_rt001a().run1(thread1);

                log1("new filter_rt001a().run1(thread2);");
                new filter_rt001a().run1(thread2);

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


class filter_rt001aTestClass10{
    void m10() {
        throw new NullPointerException("m10");
    }
}
class filter_rt001aTestClass11 extends filter_rt001aTestClass10{
    void m11() {

        try {
            (new filter_rt001aTestClass10()).m10();
        } catch ( NullPointerException e ) {
        }
        throw new NullPointerException("m11");
    }
}

class filter_rt001aThread1 extends Thread {

    String tName = null;

    public filter_rt001aThread1(String threadName) {
        super(threadName);
        tName = threadName;
    }

    public void run() {
        filter_rt001a.log1("  'run': enter  :: threadName == " + tName);
        try {
            (new filter_rt001aTestClass11()).m11();
        } catch ( NullPointerException e) {
        }
        filter_rt001a.log1("  'run': exit   :: threadName == " + tName);
        return;
    }
}

class filter_rt001aThread2 extends Thread {

    class filter_rt001aTestClass20{
        void m20() {
            throw new NullPointerException("m20");
        }
    }
    class filter_rt001aTestClass21 extends filter_rt001aTestClass20{
        void m21() {

            try {
                (new filter_rt001aTestClass20()).m20();
            } catch ( NullPointerException e ) {
            }
            throw new NullPointerException("m11");
        }
    }

    String tName = null;

    public filter_rt001aThread2(String threadName) {
        super(threadName);
        tName = threadName;
    }

    public void run() {
        filter_rt001a.log1("  'run': enter  :: threadName == " + tName);
        try {
            (new filter_rt001aTestClass21()).m21();
        } catch ( NullPointerException e) {
        }
        filter_rt001a.log1("  'run': exit   :: threadName == " + tName);
        return;
    }
}
