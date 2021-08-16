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

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * The test for the ReferenceType.genericSignature() method.           <BR>
 *                                                                     <BR>
 * The test checks up ReferenceType.genericSignature() method          <BR>
 * for InterfaceType objects.                                          <BR>
 *                                                                     <BR>
 * The first: the test checks that genericSignature() method returns   <BR>
 * null for arrays of reference types which have a InterfaceType as    <BR>
 * its type.                                                           <BR>
 *                                                                     <BR>
 * The second: the test checks that genericSignature() method returns  <BR>
 * null for InterfaceType types which are not generic types.           <BR>
 *                                                                     <BR>
 * At last: the test checks that genericSignature() method returns     <BR>
 * a corresponding signature string for InterfaceType types which are  <BR>
 * generic types.                                                      <BR>
 *                                                                     <BR>
 */

public class genericSignature002 {

    static final int STATUS_PASSED = 0;
    static final int STATUS_FAILED = 2;
    static final int STATUS_TEMP = 95;

    static final String errorLogPrefixHead = "genericSignature002: ";
    static final String errorLogPrefix     = "                     ";
    static final String infoLogPrefixHead = "--> genericSignature002: ";
    static final String infoLogPrefix     = "-->                      ";
    static final String packagePrefix = "nsk.jdi.ReferenceType.genericSignature.";
    static final String targetVMClassName = packagePrefix + "genericSignature002a";

    static ArgumentHandler  argsHandler;
    static Log logHandler;
    static boolean verboseMode = false;  // test argument -verbose switches to true
                                         // - for more easy failure evaluation

    private static void logOnVerbose(String message) {
        logHandler.display(message);
    }

    private static void logOnError(String message) {
        logHandler.complain(message);
    }

    private static void logAlways(String message) {
        logHandler.println(message);
    }


    /**
     * Debugee's classes for check:
     * classesForCheck[i][0] - basic interface name (without arrays' brackets)
     * classesForCheck[i][1] - expected string returned by genericSignature()
     */
    /** Debugee's classes for check **/
    private final static String classesForCheck[][] = {

        {packagePrefix + "GS002_Interf01", null},
        {packagePrefix + "GS002_Interf02", null},

        {packagePrefix + "GS002_Interf03", "<I:Ljava/lang/Object;>Ljava/lang/Object;"},
        {packagePrefix + "GS002_Interf04", "<I1:Ljava/lang/Object;I2:Ljava/lang/Object;>Ljava/lang/Object;"},
        {packagePrefix + "GS002_Interf05",
            "<I::Lnsk/jdi/ReferenceType/genericSignature/GS002_Interf01;>Ljava/lang/Object;"},
        {packagePrefix + "GS002_Interf06",
            "<I:Lnsk/jdi/ReferenceType/genericSignature/GS002_Class00;:Lnsk/jdi/ReferenceType/genericSignature/GS002_Interf02;>Ljava/lang/Object;"},
        {packagePrefix + "GS002_Interf07",
            "<I1:Lnsk/jdi/ReferenceType/genericSignature/GS002_Class00;:Lnsk/jdi/ReferenceType/genericSignature/GS002_Interf02;I2::Lnsk/jdi/ReferenceType/genericSignature/GS002_Interf02;>Ljava/lang/Object;"},
        {packagePrefix + "GS002_Interf08",
            "Ljava/lang/Object;Lnsk/jdi/ReferenceType/genericSignature/GS002_Interf03<Lnsk/jdi/ReferenceType/genericSignature/GS002_Class00;>;"}

    };


    /**
     * Re-call to <code>run(args,out)</code>, and exit with
     * either status 95 or 97 (JCK-like exit status).
     */
    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + STATUS_TEMP);
    }

    /**
     * JCK-like entry point to the test: perform testing, and
     * return exit code 0 (PASSED) or either 2 (FAILED).
     */
    public static int run (String argv[], PrintStream out) {

        int result =  new genericSignature002().runThis(argv, out);
        if ( result == STATUS_FAILED ) {
            logAlways("\n##> nsk/jdi/ReferenceType/genericSignature/genericSignature002 test FAILED");
        }
        else {
            logAlways("\n==> nsk/jdi/ReferenceType/genericSignature/genericSignature002 test PASSED");
        }
        return result;
    }

    /**
     * Non-static variant of the method <code>run(args,out)</code>
     */
    private int runThis (String argv[], PrintStream out) {

        int testResult = STATUS_PASSED;

        argsHandler = new ArgumentHandler(argv);
        verboseMode = argsHandler.verbose();
        logHandler = new Log(out, argsHandler);
        logHandler.enableErrorsSummary(false);

        logAlways("==> nsk/jdi/ReferenceType/genericSignature/genericSignature002 test...");
        logOnVerbose
            ("==> Test checks the genericSignature() method of ReferenceType interface");
        logOnVerbose
            ("==> of the com.sun.jdi package for InterfaceType.");

        Binder binder = new Binder(argsHandler,logHandler);
        Debugee debugee = binder.bindToDebugee(targetVMClassName);
        IOPipe pipe = new IOPipe(debugee);

        debugee.redirectStderr(out);
        logOnVerbose(infoLogPrefixHead + "Debugee (" + targetVMClassName + ") launched...");
        debugee.resume();

        String readySignal = "ready";
        String signalFromDebugee = pipe.readln();
        if ( ! readySignal.equals(signalFromDebugee) ) {
            logOnError(errorLogPrefixHead + "Uexpected debugee's signal:");
            logOnError(errorLogPrefix + "Expected signal = '" + readySignal + "'");
            logOnError(errorLogPrefix + "Actual signal = '" + signalFromDebugee + "'");
            return STATUS_FAILED;
        }
        logOnVerbose(infoLogPrefixHead + "Debugee's signal recieved = '" + readySignal + "'");

        logOnVerbose(infoLogPrefixHead + "check ReferenceType.genericSignature() method for debugee's classes...");
        String brackets[] = {"", "[]", "[][]"};
        for (int i=0; i<classesForCheck.length; i++) {
            String basicName = classesForCheck[i][0];
            for (int arrayDimension=0; arrayDimension < 3; arrayDimension++) {
                if ( arrayDimension > 0 ) {  // array type
                    if ( classesForCheck[i][1] != null ) { // it is generic type
                        continue; // arrays of generic types are not allowed
                    }
                }
                String className = basicName + brackets[arrayDimension];
                ReferenceType referenceType = debugee.classByName(className);
                if (referenceType == null) {
                    logOnError(errorLogPrefixHead + "Could NOT find ReferenceType object for debugee's class: ");
                    logOnError(errorLogPrefix + "Requested class name = '" + className + "'");
                    testResult = STATUS_FAILED;
                    continue;
                }
                String expectedGenericSignature = classesForCheck[i][1];
                if ( arrayDimension > 0 ) {
                    expectedGenericSignature = null;
                }
                String actualGenericSignature = null;
                try {
                    actualGenericSignature = referenceType.genericSignature();
                } catch (Throwable thrown) {
                    logOnError(errorLogPrefixHead + "ReferenceType.genericSignature() throws unexpected exception: ");
                    logOnError(errorLogPrefix + "ReferenceType = '" + referenceType + "'");
                    logOnError(errorLogPrefix + "Class name for this ReferenceType = '" + className + "'");
                    logOnError(errorLogPrefix + "Exception = '" + thrown + "'");
                    testResult = STATUS_FAILED;
                    continue;
                }
                boolean isFailure = false;
                if ( expectedGenericSignature == null ) {
                    if ( actualGenericSignature != null ) {
                        isFailure = true;
                    }
                } else {
                    if ( ! expectedGenericSignature.equals(actualGenericSignature) ) {
                        isFailure = true;
                    }
                }
                if ( isFailure ) {
                    logOnError(errorLogPrefixHead + "ReferenceType.genericSignature() returns unexpected signature: ");
                    logOnError(errorLogPrefix + "ReferenceType = '" + referenceType + "'");
                    logOnError(errorLogPrefix + "Class name for this ReferenceType = '" + className + "'");
                    logOnError(errorLogPrefix + "Expected generic signature = '" + expectedGenericSignature + "'");
                    logOnError(errorLogPrefix + "Actual generic signature = '" + actualGenericSignature + "'");
                    testResult = STATUS_FAILED;
                }
            }
        }
        logOnVerbose(infoLogPrefixHead
            + "Check ReferenceType.genericSignature() method for debugee's classes completed.");

        logOnVerbose(infoLogPrefixHead + "Waiting for debugee finish...");
        String quitSignal = "quit";
        pipe.println(quitSignal);
        debugee.waitFor();

        int debugeeExitStatus = debugee.getStatus();
        if (debugeeExitStatus != (STATUS_PASSED + STATUS_TEMP) ) {
            logOnError(errorLogPrefixHead + "Unexpected Debugee's exit status: ");
            logOnError(errorLogPrefix + "Expected status = '" + (STATUS_PASSED + STATUS_TEMP) + "'");
            logOnError(errorLogPrefix + "Actual status = '" + debugeeExitStatus + "'");
            testResult = STATUS_FAILED;
        }

        return testResult;
    }
} // end of genericSignature002 class
