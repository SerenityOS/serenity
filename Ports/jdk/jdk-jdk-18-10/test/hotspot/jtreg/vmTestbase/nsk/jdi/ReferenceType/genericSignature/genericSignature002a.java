/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.ReferenceType.genericSignature;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


/**
 * This class is used as debugee application for
 * the nsk/jdi/ReferenceType/genericSignature/genericSignature002 JDI test.
 */

public class genericSignature002a {

    static final int STATUS_PASSED = 0;
    static final int STATUS_FAILED = 2;
    static final int STATUS_TEMP = 95;

    static final String errorLogPrefixHead = "genericSignature002(Debugee): ";
    static final String errorLogPrefix     = "                              ";
    static final String infoLogPrefixHead = "--> genericSignature002(Debugee): ";
    static final String infoLogPrefix     = "-->                               ";

    static ArgumentHandler  argsHandler;
    static Log logHandler;

    private static void logOnVerbose(String message) {
        logHandler.display(message);
    }

    private static void logOnError(String message) {
        logHandler.complain(message);
    }

    private static void logAlways(String message) {
        logHandler.println(message);
    }

    // instantiating of non-generic interface types and arrays of non-generic interface types
    // for check ReferenceType.genericSignature() method

    GS002_Class00 GS002_Class00_Obj = new  GS002_Class00();

    GS002_Interf01 GS002_Interf01_Obj0 = new GS002_Class01();
    GS002_Interf01[] GS002_Interf01_Obj1 = {GS002_Interf01_Obj0};
    GS002_Interf01[][] GS002_Interf01_Obj2 = {GS002_Interf01_Obj1};

    GS002_Interf02 GS002_Interf02_Obj0 = new GS002_Class02();
    GS002_Interf02[] GS002_Interf02_Obj1 = {GS002_Interf02_Obj0};
    GS002_Interf02[][] GS002_Interf02_Obj2 = {GS002_Interf02_Obj1};

    // instantiating of generic interface types for check ReferenceType.genericSignature() method

    GS002_Interf03<GS002_Class00> GS002_Interf03_Obj = new GS002_Class03();
    GS002_Interf04<GS002_Class00, GS002_Interf01> GS002_Interf04_Obj = new GS002_Class04();
    GS002_Interf05<GS002_Interf01> GS002_Interf05_Obj = new GS002_Class05();
    GS002_Interf06<GS002_Class00> GS002_Interf06_Obj = new GS002_Class06();
    GS002_Interf07<GS002_Class00, GS002_Interf02> GS002_Interf07_Obj = new GS002_Class07();
    GS002_Interf08 GS002_Interf08_Obj = new GS002_Class08();


    public static void main (String argv[]) {

        argsHandler = new ArgumentHandler(argv);
        logHandler = new Log(System.err, argsHandler);
        logHandler.enableErrorsSummary(false);
        IOPipe pipe = argsHandler.createDebugeeIOPipe();

        logOnVerbose(infoLogPrefixHead + "Debugee started!");

        genericSignature002a genericSignature002aDrbugee = new genericSignature002a();

        String readySignal = "ready";
        pipe.println(readySignal);
        String quitSignal = "quit";
        logOnVerbose(infoLogPrefixHead + "Wait for '" + quitSignal + "' signal...");
        String signalFromDebugger = pipe.readln();
        if ( ! (quitSignal.equals(signalFromDebugger)) ) {
            logOnError(errorLogPrefixHead + "UNEXPECTED debugger's signal:");
            logOnError(errorLogPrefix + "Expected signal = '" + quitSignal + "'");
            logOnError(errorLogPrefix + "Actual signal = '" + signalFromDebugger + "'");
            logOnError(errorLogPrefix + "Exiting with Exit Status = '" + (STATUS_FAILED + STATUS_TEMP) + "'");
            System.exit(STATUS_FAILED + STATUS_TEMP);
        }
        logOnVerbose(infoLogPrefixHead + "'" + quitSignal + "' signal from debugger is received.");
        logOnVerbose(infoLogPrefix + "Exiting with Exit Status = '" + (STATUS_PASSED + STATUS_TEMP) + "'");
        System.exit(STATUS_PASSED + STATUS_TEMP);
    }
} // end of genericSignature002a class


// non generic interfaces:

interface GS002_Interf01 {}

interface GS002_Interf02 {}

// generic interfaces:

interface GS002_Interf03<I> {}

interface GS002_Interf04<I1, I2> {}

interface GS002_Interf05<I extends GS002_Interf01> {}

interface GS002_Interf06<I extends GS002_Class00 & GS002_Interf02> {}

interface GS002_Interf07<I1 extends GS002_Class00 & GS002_Interf02, I2 extends GS002_Interf02> {}

interface GS002_Interf08 extends GS002_Interf03<GS002_Class00> {}



// Auxiliary classes

class GS002_Class00 implements GS002_Interf02 {}

class GS002_Class01 implements GS002_Interf01 {}

class GS002_Class02 implements GS002_Interf02 {}

class GS002_Class03 implements GS002_Interf03<GS002_Class00> {}

class GS002_Class04 implements GS002_Interf04<GS002_Class00, GS002_Interf01> {}

class GS002_Class05 implements GS002_Interf05<GS002_Interf01> {}

class GS002_Class06 implements GS002_Interf06<GS002_Class00> {}

class GS002_Class07 implements GS002_Interf07<GS002_Class00, GS002_Interf02> {}

class GS002_Class08 implements GS002_Interf08 {}
