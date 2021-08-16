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
 * the nsk/jdi/TypeComponent/genericSignature/genericSignature002 JDI test.
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

    // methods without generic signature
    // for check TypeComponent.genericSignature() method

    void testMethod_001()
    {
    }

    void testMethod_002 (
        boolean booleanArg,
        byte    byteArg,
        char    charArg,
        double  doubleArg,
        float   floatArg,
        int     intArg,
        long    longArg,
        Object  objectArg)
    {
    }

    boolean testMethod_003 (
        boolean booleanArg,
        byte    byteArg,
        char    charArg,
        double  doubleArg,
        float   floatArg,
        int     intArg,
        long    longArg,
        Object  objectArg)
    {
        return booleanArg;
    }

    byte testMethod_004 (
        boolean booleanArg,
        byte    byteArg,
        char    charArg,
        double  doubleArg,
        float   floatArg,
        int     intArg,
        long    longArg,
        Object  objectArg)
    {
        return byteArg;
    }

    char testMethod_005 (
        boolean booleanArg,
        byte    byteArg,
        char    charArg,
        double  doubleArg,
        float   floatArg,
        int     intArg,
        long    longArg,
        Object  objectArg)
    {
        return charArg;
    }

    double testMethod_006 (
        boolean booleanArg,
        byte    byteArg,
        char    charArg,
        double  doubleArg,
        float   floatArg,
        int     intArg,
        long    longArg,
        Object  objectArg)
    {
        return doubleArg;
    }

    float testMethod_007 (
        boolean booleanArg,
        byte    byteArg,
        char    charArg,
        double  doubleArg,
        float   floatArg,
        int     intArg,
        long    longArg,
        Object  objectArg)
    {
        return floatArg;
    }

    int testMethod_008 (
        boolean booleanArg,
        byte    byteArg,
        char    charArg,
        double  doubleArg,
        float   floatArg,
        int     intArg,
        long    longArg,
        Object  objectArg)
    {
        return intArg;
    }

    long testMethod_009 (
        boolean booleanArg,
        byte    byteArg,
        char    charArg,
        double  doubleArg,
        float   floatArg,
        int     intArg,
        long    longArg,
        Object  objectArg)
    {
        return longArg;
    }

    Object testMethod_010 (
        boolean booleanArg,
        byte    byteArg,
        char    charArg,
        double  doubleArg,
        float   floatArg,
        int     intArg,
        long    longArg,
        Object  objectArg)
    {
        return objectArg;
    }

    // methods with generic signature
    // for check TypeComponent.genericSignature() method

    void testMethod_011 (
        GS002_Class06<GS002_Class01> GS002_Class06_Arg)
    {
    }

    GS002_Class06<GS002_Class01> testMethod_012 (
        GS002_Class06<GS002_Class01> GS002_Class06_Arg)
    {
        return GS002_Class06_Arg;
    }

    void testMethod_013 (
        GS002_Class07<GS002_Class01, GS002_Class02> GS002_Class07_Arg)
    {
    }

    GS002_Class07<GS002_Class01, GS002_Class02> testMethod_014 (
        GS002_Class07<GS002_Class01, GS002_Class02> GS002_Class07_Arg)
    {
        return GS002_Class07_Arg;
    }

    void testMethod_015 (
        GS002_Class08<GS002_Class03> GS002_Class08_Arg)
    {
    }

    GS002_Class08<GS002_Class03> testMethod_016 (
        GS002_Class08<GS002_Class03> GS002_Class08_Arg)
    {
        return GS002_Class08_Arg;
    }

    void testMethod_017 (
        GS002_Class09<GS002_Class04> GS002_Class09_Arg)
    {
    }

    GS002_Class09<GS002_Class04> testMethod_018 (
        GS002_Class09<GS002_Class04> GS002_Class09_Arg)
    {
        return GS002_Class09_Arg;
    }

    void testMethod_019 (
        GS002_Class10<GS002_Class04, GS002_Class05> GS002_Class10_Arg)
    {
    }

    GS002_Class10<GS002_Class04, GS002_Class05> testMethod_020 (
        GS002_Class10<GS002_Class04, GS002_Class05> GS002_Class10_Arg)
    {
        return GS002_Class10_Arg;
    }


    <C> void testMethod_021 ()
    {
    }

    <C> void testMethod_022 (C C_Arg)
    {
    }

    <C> C testMethod_023 (C C_Arg)
    {
        return C_Arg;
    }

    <C1, C2> void testMethod_024 ()
    {
    }

    <C1, C2> void testMethod_025 (C1 C1_Arg, C2 C2_Arg)
    {
    }

    <C1, C2> C2 testMethod_026 (C1 C1_Arg, C2 C2_Arg)
    {
        return C2_Arg;
    }

    <C1 extends GS002_Class01 & GS002_Interf01, C2 extends GS002_Class02 & GS002_Interf02> void testMethod_027 ()
    {
    }

    <C1 extends GS002_Class01 & GS002_Interf01, C2 extends GS002_Class02 & GS002_Interf02> void testMethod_028 (
        C2 C2_Arg, C1 C1_Arg)
    {
    }


    <C1 extends GS002_Class01 & GS002_Interf01, C2 extends GS002_Class02 & GS002_Interf02> C1 testMethod_029 (
        C2 C2_Arg, C1 C1_Arg)
    {
        return C1_Arg;
    }


    public static void main (String argv[]) {

        argsHandler = new ArgumentHandler(argv);
        logHandler = new Log(System.err, argsHandler);
        logHandler.enableErrorsSummary(false);
        IOPipe pipe = argsHandler.createDebugeeIOPipe();

        logOnVerbose(infoLogPrefixHead + "Debugee started!");

        genericSignature002a genericSignature002aDebugee = new genericSignature002a();

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


// non generic classes:

class GS002_Class01 {}

class GS002_Class02 {}

interface GS002_Interf01 {}

interface GS002_Interf02 {}

class GS002_Class03 extends GS002_Class01 {}

class GS002_Class04 extends GS002_Class01 implements GS002_Interf01 {}

class GS002_Class05 extends GS002_Class02 implements GS002_Interf02 {}


// generic classes:

class GS002_Class06<T> {}

class GS002_Class07<T1, T2> {}

class GS002_Class08<T extends GS002_Class01> {}

class GS002_Class09<T extends GS002_Class01 & GS002_Interf01> {}

class GS002_Class10<T1 extends GS002_Class01 & GS002_Interf01, T2 extends GS002_Class02 & GS002_Interf02> {}
