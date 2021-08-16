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

package nsk.jdi.TypeComponent.genericSignature;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


/**
 * This class is used as debugee application for
 * the nsk/jdi/TypeComponent/genericSignature/genericSignature001 JDI test.
 */

public class genericSignature001a {

    static final int STATUS_PASSED = 0;
    static final int STATUS_FAILED = 2;
    static final int STATUS_TEMP = 95;

    static final String errorLogPrefixHead = "genericSignature001(Debugee): ";
    static final String errorLogPrefix     = "                              ";
    static final String infoLogPrefixHead = "--> genericSignature001(Debugee): ";
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

    // primitive type fields and arrays of primitive types
    // for check TypeComponent.genericSignature() method

    boolean z0, z1[]={z0}, z2[][]={z1};
    byte    b0, b1[]={b0}, b2[][]={b1};
    char    c0, c1[]={c0}, c2[][]={c1};
    double  d0, d1[]={d0}, d2[][]={d1};
    float   f0, f1[]={f0}, f2[][]={f1};
    int     i0, i1[]={i0}, i2[][]={i1};
    long    l0, l1[]={l0}, l2[][]={l1};

    // non-generic reference type fields and arrays of non-generic reference types
    // for check TypeComponent.genericSignature() method

    GS001_Class01 GS001_Class01_Obj0 = new GS001_Class01(),
                  GS001_Class01_Obj1[]={GS001_Class01_Obj0},
                  GS001_Class01_Obj2[][]={GS001_Class01_Obj1};

    GS001_Class02 GS001_Class02_Obj0 = new GS001_Class02(),
                  GS001_Class02_Obj1[]={GS001_Class02_Obj0},
                  GS001_Class02_Obj2[][]={GS001_Class02_Obj1};

    GS001_Class03 GS001_Class03_Obj0 = new GS001_Class03(),
                  GS001_Class03_Obj1[]={GS001_Class03_Obj0},
                  GS001_Class03_Obj2[][]={GS001_Class03_Obj1};

    GS001_Class04 GS001_Class04_Obj0 = new GS001_Class04(),
                  GS001_Class04_Obj1[]={GS001_Class04_Obj0},
                  GS001_Class04_Obj2[][]={GS001_Class04_Obj1};

    GS001_Class05 GS001_Class05_Obj0 = new GS001_Class05(),
                  GS001_Class05_Obj1[]={GS001_Class05_Obj0},
                  GS001_Class05_Obj2[][]={GS001_Class05_Obj1};

    // generic type fields for check TypeComponent.genericSignature() method

    GS001_Class06<GS001_Class01> GS001_Class06_Obj = new GS001_Class06<GS001_Class01>();

    GS001_Class07<GS001_Class01, GS001_Class02> GS001_Class07_Obj =
            new GS001_Class07<GS001_Class01, GS001_Class02>();

    GS001_Class08<GS001_Class03> GS001_Class08_Obj = new GS001_Class08<GS001_Class03>();

    GS001_Class09<GS001_Class04> GS001_Class09_Obj = new GS001_Class09<GS001_Class04>();

    GS001_Class10<GS001_Class04, GS001_Class05> GS001_Class10_Obj =
            new GS001_Class10<GS001_Class04, GS001_Class05>();


    public static void main (String argv[]) {

        argsHandler = new ArgumentHandler(argv);
        logHandler = new Log(System.err, argsHandler);
        logHandler.enableErrorsSummary(false);
        IOPipe pipe = argsHandler.createDebugeeIOPipe();

        logOnVerbose(infoLogPrefixHead + "Debugee started!");

        genericSignature001a genericSignature001aDebugee = new genericSignature001a();

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
} // end of genericSignature001a class


// non generic classes:

class GS001_Class01 {}

class GS001_Class02 {}

interface GS001_Interf01 {}

interface GS001_Interf02 {}

class GS001_Class03 extends GS001_Class01 {}

class GS001_Class04 extends GS001_Class01 implements GS001_Interf01 {}

class GS001_Class05 extends GS001_Class02 implements GS001_Interf02 {}


// generic classes:

class GS001_Class06<T> {}

class GS001_Class07<T1, T2> {}

class GS001_Class08<T extends GS001_Class01> {}

class GS001_Class09<T extends GS001_Class01 & GS001_Interf01> {}

class GS001_Class10<T1 extends GS001_Class01 & GS001_Interf01, T2 extends GS001_Class02 & GS001_Interf02> {}
