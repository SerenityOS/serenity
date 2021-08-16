/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.ObjectReference.disableCollection;

import nsk.share.*;
import nsk.share.jdi.*;

/**
 * This class is used as debuggee application for the disablecollection002 JDI test.
 */

public class disablecollection002a {

    //----------------------------------------------------- templete section

    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    static ArgumentHandler argHandler;
    static Log log;

    //--------------------------------------------------   log procedures

    static String oom_message = "**> debuggee: caught:  OutOfMemoryError";
    private static void log_oom() {
        log.display(oom_message);
    }

    private static void log1(String message) {
        log.display("**> debuggee: " + message);
    }

    private static void logErr(String message) {
        log.complain("**> debuggee: " + message);
    }

    //====================================================== test program

    static Runtime runTime = Runtime.getRuntime();

    static array1 arr1   = null;
    static array2 arr2[] = null;

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
                           log1("runTime.maxMemory()   == " + runTime.maxMemory());
                           log1("runTime.totalMemory() == " + runTime.totalMemory());
                           log1("runTime.freeMemory()  == " + runTime.freeMemory());

                           try {
                               arr1 = new array1();
                               arr2 = new array2[100];
                           } catch (OutOfMemoryError e) {
                               log1("caught:  OutOfMemoryError while creating objects");
                               instruction = end;
                               methodForCommunication();
                               break label0;
                           }

                           methodForCommunication();
                           break ;

                    case 1:
                           log1("arr1 = null;");
                           arr1 = null;
                           try {
                               for (int k = 0; k < 100; k++) {
                                   log1("> " + k + " runTime.maxMemory()   == " + runTime.maxMemory());
                                   log1("  " + k + " runTime.totalMemory() == " + runTime.totalMemory());
                                   log1("  " + k + " runTime.freeMemory()  == " + runTime.freeMemory());
                                   arr2[k] = new array2();
                               }
                           } catch (OutOfMemoryError e) {
                               for (int k = 0; k < 100; k++) {
                                   arr2[k] = null;
                               }
                               log_oom();
                           }
                           methodForCommunication();
                           break ;

                    case 2:
                           log1("runTime.gc(); called");
                           runTime.gc();
                           methodForCommunication();
                           break ;

    //-------------------------------------------------    standard end section

                    default:
                                instruction = end;
                                methodForCommunication();
                                break label0;
                }
            }

        System.exit(exitCode + PASS_BASE);
    }

    static int getArraySize(long longValue) {
        int arraySize;
        if (longValue > Integer.MAX_VALUE)
            arraySize = Integer.MAX_VALUE;
        else
            arraySize = (int)longValue;

        return arraySize;
    }

    static class array1 {
        int a1[] = new int[getArraySize(runTime.maxMemory() / 10)];
    }

    static class array2 {
        int a2[] = new int[getArraySize(runTime.maxMemory() / 100)];
    }

}
