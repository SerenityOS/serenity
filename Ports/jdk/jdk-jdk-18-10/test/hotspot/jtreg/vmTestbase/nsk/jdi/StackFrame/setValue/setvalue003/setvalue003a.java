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

package nsk.jdi.StackFrame.setValue.setvalue003;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

/**
 * This class is used as debuggee application for the setvalue003 JDI test.
 */

public class setvalue003a {

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
                         Threadsetvalue003a test_thread =
                             new Threadsetvalue003a("testedThread");
                         log1("       thread2 is created");

                         label:
                         synchronized (Threadsetvalue003a.lockingObject) {
                             synchronized (Threadsetvalue003a.waitnotifyObj) {
                                 log1("       synchronized (waitnotifyObj) { enter");
                                 log1("       before: test_thread.start()");
                                 test_thread.start();

                                 try {
                                     log1("       before:   waitnotifyObj.wait();");
                                     Threadsetvalue003a.waitnotifyObj.wait();
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
                         log1("mainThread is out of: synchronized (lockingObject) {");

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



class Threadsetvalue003a extends Thread {

    public Threadsetvalue003a(String threadName) {
        super(threadName);
    }

    public static Object waitnotifyObj = new Object();
    public static Object lockingObject = new Object();

    private int i1 = 0, i2 = 10;

    public void run() {
        log("method 'run' enter");
        synchronized (waitnotifyObj)                                    {
            log("entered into block:  synchronized (waitnotifyObj)");
            waitnotifyObj.notify();                                     }
        log("exited from block:  synchronized (waitnotifyObj)");
        synchronized (lockingObject)                                    {
            log("entered into block:  synchronized (lockingObject)");   }
        log("exited from block:  synchronized (lockingObject)");
        i1++;
        i2--;
        log("call to 'runt1'");
        runt1();
        log("returned from 'runt1'");
        log("method 'run' exit");
        return;
    }


    int i0 = 0;

    public void runt1() {
    // case 0
    ClassForCheck_2   class1[] = { new ClassForCheck_2() };
    ClassForCheck_2   class2[] = null;
    InterfaceForCheck iface1[] = {new ClassForCheck_2()};
    InterfaceForCheck iface2[] = null;
    // case 1
    ClassForCheck_2   class3   = new ClassForCheck_2();
    ClassForCheck_2   class4   = null;
    InterfaceForCheck iface3   = new ClassForCheck_2();
    InterfaceForCheck iface4   = null;
    // case 2
    boolean array1[][] = {{true,false}, {false,true} };
    boolean array2[][] = null;

        log("method 'runt1' enter");
        log("call to 'runt2' ");
        runt2();
        log("returned from 'runt2' ");
        i0++;
        i0++;
        i0++;
        i0++;
        i0++;
        i0++;
        i0++;
        log("method 'runt1' body");
        log("method 'runt1' exit");
        return;
    }

    public void runt2() {

        boolean bl3 = true, bl4 = false;

        log("method 'runt2' enter");
        bl3 = false;
        bl4 = true;
        log("method 'runt2' body");
        log("method 'runt2' exit");
        return;
    }

    public static final int breakpointLineNumber1 = 15;
    public static final int breakpointLineNumber2 = 2;
//    public static final int breakpointLineNumber3 = 7;


    void log(String str) {
        setvalue003a.log2("thread2: " + str);
    }

}


interface InterfaceForCheck {

    static final boolean s_iface_boolean = true;
    static final byte    s_iface_byte    = (byte)1;
    static final char    s_iface_char    = '1';
    static final double  s_iface_double  = 999;
    static final float   s_iface_float   = 99;
    static final int     s_iface_int     = 100;
    static final long    s_iface_long    = 1000;
    static final Object  s_iface_object  = new Object();
}


class ClassForCheck_2 implements InterfaceForCheck {
}
