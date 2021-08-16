/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.ReferenceType.isStatic;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

/**
 * This class is used as debuggee application for the isstatic001 JDI test.
 */

public class isstatic001a {

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
                                isstatic001aTestClass check = new isstatic001aTestClass();
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
}


class isstatic001aTestClass {

    class NestedClass implements NestedIface {
        boolean bnc = true;
    }
    NestedClass  nestedClass        = new NestedClass();
    NestedClass  nestedClassArray[] = { new NestedClass() };

    interface NestedIface {
        boolean bnf = true;
    }
    NestedIface nestedIface        = new NestedClass();
//    NestedIface nestedIfaceArray[] = { new NestedClass() };


    static class StaticNestedClass {
        boolean bsnc = true;
    }
    StaticNestedClass staticNestedClass        = new StaticNestedClass();
//    StaticNestedClass staticNestedClassArray[] = { new StaticNestedClass() };


    static isstatic001aOuterClass outerClass        = new isstatic001aOuterClass();
    static isstatic001aOuterClass outerClassArray[] = { new isstatic001aOuterClass() };
    static isstatic001aOuterIface outerIface        = new isstatic001aOuterClass();
    static isstatic001aOuterIface outerIfaceArray[] = { new isstatic001aOuterClass() };


    boolean bl[][][][] = {{{{true}}}};
    byte    bt[][][][] = {{{{0}}}};
    char    ch[][][][] = {{{{0}}}};
    double  db[][][][] = {{{{0.0}}}};
    float   fl[][][][] = {{{{0.0f}}}};
    int     in[][][][] = {{{{0}}}};
    long    ln[][][][] = {{{{0}}}};
    short   sh[][][][] = {{{{0}}}};


    Boolean   blBl = Boolean.valueOf(true);
    Byte      btBt = Byte.valueOf((byte)1);
    Character chCh = Character.valueOf('c');
    Double    dbDb = Double.valueOf(0);
    Float     flFl = Float.valueOf(0.0f);
    Integer   inIn = Integer.valueOf(0);
    Long      lnLn = Long.valueOf(0);
    Short     shSh = Short.valueOf((short)1);
}

interface isstatic001aOuterIface {
    static boolean b1 = false;

    interface InnerIface {}

    static interface StaticInnerIface {}
}

class isstatic001aOuterClass implements isstatic001aOuterIface {
    static final boolean b2 = true;

    class InnerClass implements isstatic001aOuterIface.InnerIface {
        boolean bOiIi = true;
    }
    InnerClass innerClass = new InnerClass();

    class StaticInnerClass implements isstatic001aOuterIface.StaticInnerIface {
        boolean bOiSii = true;
    }
    StaticInnerClass staticInnerClass = new StaticInnerClass();
}

final class isstatic001aFinalOuterClass implements isstatic001aOuterIface {
    static final boolean b3 = true;
}
