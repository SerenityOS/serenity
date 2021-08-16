/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.ClassObjectReference.toString;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

//    THIS TEST IS LINE NUMBER SENSITIVE

/**
 * The debugged applcation of the test.
 */
public class tostring001a {

    //------------------------------------------------------- immutable common fields

    private static int exitStatus;
    private static ArgumentHandler argHandler;
    private static Log log;
    private static IOPipe pipe;

    //------------------------------------------------------- immutable common methods

    static void display(String msg) {
        log.display("debuggee > " + msg);
    }

    static void complain(String msg) {
        log.complain("debuggee FAILURE > " + msg);
    }

    public static void receiveSignal(String signal) {
        String line = pipe.readln();

        if ( !line.equals(signal) )
            throw new Failure("UNEXPECTED debugger's signal " + line);

        display("debuger's <" + signal + "> signal received.");
    }

    //------------------------------------------------------ mutable common fields

    //------------------------------------------------------ test specific fields

    public final static String brkpMethodName = "main";
    public final static int brkpLineNumber = 121;

    // Classes must be loaded and linked, so all fields must be
    // initialized
    Boolean   Z0 = Boolean.valueOf(true),       Z1[]={Z0}, Z2[][]={Z1};
    Byte      B0 = Byte.valueOf((byte)1),       B1[]={B0}, B2[][]={B1};
    Character C0 = Character.valueOf('\u00ff'), C1[]={C0}, C2[][]={C1};
    Double    D0 = Double.valueOf(1.0),         D1[]={D0}, D2[][]={D1};
    Float     F0 = Float.valueOf(1.0f),         F1[]={F0}, F2[][]={F1};
    Integer   I0 = Integer.valueOf(-1),         I1[]={I0}, I2[][]={I1};
    Long      L0 = Long.valueOf(-1l),           L1[]={L0}, L2[][]={L1};
    Short     H0 = Short.valueOf((short)-1),    H1[]={H0}, H2[][]={H1};
    String    S0 = new String("4434819"),       S1[]={S0}, S2[][]={S1};
    Object    O0 = new Object(),                O1[]={O0}, O2[][]={O1};

    // Interfaces must be loaded and linked, so classes that implement
    // interfaces must be initialized.
    static class  innerClass {}
    static interface  innerInterf {}
    protected static class innerInterfImpl implements innerInterf {}
    innerInterfImpl sii0 = new innerInterfImpl();

    innerClass innerClass0 = new innerClass();
    innerClass innerClass1[] = {innerClass0};
    innerClass innerClass2[][] = {innerClass1};

    innerInterf innerInterf0;
    innerInterf innerInterf1[] = {innerInterf0};
    innerInterf innerInterf2[][] = {innerInterf1};

    tostring001aClass tostring001aClass0 = new tostring001aClass();
    tostring001aClass tostring001aClass1[] = {tostring001aClass0};
    tostring001aClass tostring001aClass2[][] = {tostring001aClass1};

    class tostring001aInterfImpl implements tostring001aInterf {}
    tostring001aInterfImpl pii0 = new tostring001aInterfImpl();
    tostring001aInterf tostring001aInterf0;
    tostring001aInterf tostring001aInterf1[] = {tostring001aInterf0};
    tostring001aInterf tostring001aInterf2[][] = {tostring001aInterf1};

    //------------------------------------------------------ mutable common method

    public static void main (String argv[]) {
        exitStatus = Consts.TEST_FAILED;
        argHandler = new ArgumentHandler(argv);
        log = new Log(System.err, argHandler);
        pipe = argHandler.createDebugeeIOPipe(log);

        tostring001a dummy = new tostring001a();

        try {
            pipe.println(tostring001.SIGNAL_READY);
            receiveSignal(tostring001.SIGNAL_GO);
            log.display("breakpoint line"); // brkpLineNumber
            receiveSignal(tostring001.SIGNAL_QUIT);
            display("completed succesfully.");
            System.exit(Consts.TEST_PASSED + Consts.JCK_STATUS_BASE);
        } catch (Failure e) {
            log.complain(e.getMessage());
            System.exit(Consts.TEST_FAILED + Consts.JCK_STATUS_BASE);
        }
    }

    //--------------------------------------------------------- test specific methods
}

//--------------------------------------------------------- test specific classes
class tostring001aClass {}
interface tostring001aInterf {}
