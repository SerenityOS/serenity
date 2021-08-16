/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.ObjectReference.invokeMethod;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

//    THIS TEST IS LINE NUMBER SENSITIVE

/**
 * This is a debuggee class.
 */
public class invokemethod007t {
    public static void main(String args[]) {
        System.exit(run(args) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String args[]) {
        return new invokemethod007t().runIt(args);
    }

    private int runIt(String args[]) {
        ArgumentHandler argHandler = new ArgumentHandler(args);
        IOPipe pipe = argHandler.createDebugeeIOPipe();
        invokemethod007tDummyClass invokemethod007tdummyCls = new invokemethod007tDummyClass();
        Thread.currentThread().setName(invokemethod007.DEBUGGEE_THRNAME);

        pipe.println(invokemethod007.COMMAND_READY);
        String cmd = pipe.readln();
        if (cmd.equals(invokemethod007.COMMAND_QUIT)) {
            System.err.println("Debuggee: exiting due to the command "
                + cmd);
            return Consts.TEST_PASSED;
        }

        int stopMeHere = 0; // invokemethod007.DEBUGGEE_STOPATLINE

        cmd = pipe.readln();
        if (!cmd.equals(invokemethod007.COMMAND_QUIT)) {
            System.err.println("TEST BUG: unknown debugger command: "
                + cmd);
            System.exit(Consts.JCK_STATUS_BASE +
                Consts.TEST_FAILED);
        }
        return Consts.TEST_PASSED;
    }
}

// Dummy reference types used to provoke InvalidTypeException
// in the debugger
class invokemethod007tDummyType {}
class invokemethod007tWrongDummyType {}

// Dummy class used to provoke InvalidTypeException
// in the debugger
class invokemethod007tDummyClass {
    // dummy fields used to obtain their values for method invocation
    static byte    sByteFld = 127;
    static short   sShortFld = -32768;
    int            intFld = 2147483647;
    static long    sLongFld = 9223372036854775807L;
    float          floatFld = 5.1F;
    static double  sDoubleFld = 6.2D;
    static char    sCharFld = 'a';
    static boolean sBooleanFld = true;
    static String  sStrFld = "static field";
    static invokemethod007tDummyType dummyType = new invokemethod007tDummyType();
    static invokemethod007tWrongDummyType wrongDummyType = new invokemethod007tWrongDummyType();

    byte byteMeth(byte b) {
        System.err.println("the method \"byteMeth\" was invoked!");
        return b;
    }

    short shortMeth(short sh) {
        System.err.println("the method \"shortMeth\" was invoked!");
        return sh;
    }

    int intMeth(int i) {
        System.err.println("the method \"intMeth\" was invoked!");
        return i;
    }

    long longMeth(long l) {
        System.err.println("the method \"longMeth\" was invoked!");
        return l;
    }

    float floatMeth(float f) {
        System.err.println("the method \"floatMeth\" was invoked!");
        return f;
    }

    double doubleMeth(double d) {
        System.err.println("the method \"doubleMeth\" was invoked!");
        return d;
    }

    char charMeth(char c) {
        System.err.println("the method \"charMeth\" was invoked!");
        return c;
    }

    boolean booleanMeth(boolean bool) {
        System.err.println("the method \"booleanMeth\" was invoked!");
        return bool;
    }

    String strMeth(String str) {
        System.err.println("the method \"strMeth\" was invoked!");
        return str;
    }

    void dummyTMeth(invokemethod007tDummyType dummyT) {
        System.err.println("the method \"dummyTMeth\" was invoked!");
    }
}
