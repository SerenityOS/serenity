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

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * The test for the TypeComponent.genericSignature() method.           <BR>
 *                                                                     <BR>
 * The test checks up TypeComponent.genericSignature() method          <BR>
 * for Field objects.                                                  <BR>
 *                                                                     <BR>
 * The first: the test checks that genericSignature() method returns   <BR>
 * null for fields of all primitive types and for fields which are     <BR>
 * arrays of primitive types.                                          <BR>
 *                                                                     <BR>
 * The second: the test checks that genericSignature() method returns  <BR>
 * null for fields whivh are arrays of non-generic reference types.    <BR>
 *                                                                     <BR>
 * The third: the test checks that genericSignature() method returns   <BR>
 * null for fields of non-generic reference types.                     <BR>
 *                                                                     <BR>
 * At last: the test checks that genericSignature() method returns     <BR>
 * a corresponding signature string for fields of generic reference    <BR>
 * types.                                                              <BR>
 *                                                                     <BR>
 */

public class genericSignature001 {

    static final int STATUS_PASSED = 0;
    static final int STATUS_FAILED = 2;
    static final int STATUS_TEMP = 95;

    static final String errorLogPrefixHead = "genericSignature001: ";
    static final String errorLogPrefix     = "                     ";
    static final String infoLogPrefixHead = "--> genericSignature001: ";
    static final String infoLogPrefix     = "-->                      ";
    static final String packagePrefix = "nsk.jdi.TypeComponent.genericSignature.";
    static final String targetVMClassName = packagePrefix + "genericSignature001a";

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


    private final static String primitiveTypeSign = "primitiveType";
    private final static String referenceTypeSign = "referenceType";

    /**
     * Debugee's fields for check:
     * fieldsForCheck[i][0] - field name
     * fieldsForCheck[i][1] - expected string returned by genericSignature()
     */
    private final static String fieldsForCheck[][] = {
        {"z0", null},
        {"b0", null},
        {"c0", null},
        {"d0", null},
        {"f0", null},
        {"i0", null},
        {"l0", null},

        {"z1", null},
        {"b1", null},
        {"c1", null},
        {"d1", null},
        {"f1", null},
        {"i1", null},
        {"l1", null},

        {"z2", null},
        {"b2", null},
        {"c2", null},
        {"d2", null},
        {"f2", null},
        {"i2", null},
        {"l2", null},

        {"GS001_Class01_Obj0", null},
        {"GS001_Class02_Obj0", null},
        {"GS001_Class03_Obj0", null},
        {"GS001_Class04_Obj0", null},
        {"GS001_Class05_Obj0", null},

        {"GS001_Class01_Obj1", null},
        {"GS001_Class02_Obj1", null},
        {"GS001_Class03_Obj1", null},
        {"GS001_Class04_Obj1", null},
        {"GS001_Class05_Obj1", null},

        {"GS001_Class01_Obj2", null},
        {"GS001_Class02_Obj2", null},
        {"GS001_Class03_Obj2", null},
        {"GS001_Class04_Obj2", null},
        {"GS001_Class05_Obj2", null},

        {"GS001_Class06_Obj",
            "Lnsk/jdi/TypeComponent/genericSignature/GS001_Class06<Lnsk/jdi/TypeComponent/genericSignature/GS001_Class01;>;"},
        {"GS001_Class07_Obj",
             "Lnsk/jdi/TypeComponent/genericSignature/GS001_Class07<Lnsk/jdi/TypeComponent/genericSignature/GS001_Class01;Lnsk/jdi/TypeComponent/genericSignature/GS001_Class02;>;"},
        {"GS001_Class08_Obj",
            "Lnsk/jdi/TypeComponent/genericSignature/GS001_Class08<Lnsk/jdi/TypeComponent/genericSignature/GS001_Class03;>;"},
        {"GS001_Class09_Obj",
            "Lnsk/jdi/TypeComponent/genericSignature/GS001_Class09<Lnsk/jdi/TypeComponent/genericSignature/GS001_Class04;>;"},
        {"GS001_Class10_Obj",
            "Lnsk/jdi/TypeComponent/genericSignature/GS001_Class10<Lnsk/jdi/TypeComponent/genericSignature/GS001_Class04;Lnsk/jdi/TypeComponent/genericSignature/GS001_Class05;>;"}

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

        int result =  new genericSignature001().runThis(argv, out);
        if ( result == STATUS_FAILED ) {
            logAlways("\n##> nsk/jdi/TypeComponent/genericSignature/genericSignature001 test FAILED");
        }
        else {
            logAlways("\n==> nsk/jdi/TypeComponent/genericSignature/genericSignature001 test PASSED");
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

        logAlways("==> nsk/jdi/TypeComponent/genericSignature/genericSignature001 test...");
        logOnVerbose
            ("==> Test checks the genericSignature() method of TypeComponent interface");
        logOnVerbose
            ("==> of the com.sun.jdi package for Field objects.");

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

        logOnVerbose(infoLogPrefixHead + "Request ReferenceType object for debugee's class...");
        ReferenceType targetVMClass = null;
        try {
            targetVMClass = debugee.classByName(targetVMClassName);
        } catch (Throwable thrown) {
            logOnError(errorLogPrefixHead + "Unexpected exception during requesting ReferenceType: ");
            logOnError(errorLogPrefix + "Requested class name = '" + targetVMClassName + "'");
            logOnError(errorLogPrefix + "Exception = '" + thrown + "'");
            return STATUS_FAILED;
        }
        if (targetVMClass == null) {
            logOnError(errorLogPrefixHead + "Could NOT find ReferenceType object for debugee's class: ");
            logOnError(errorLogPrefix + "Requested class name = '" + targetVMClassName + "'");
            return STATUS_FAILED;
        }

        logOnVerbose(infoLogPrefixHead + "check TypeComponent.genericSignature() method for debugee's fields...");
        String brackets[] = {"", "[]", "[][]"};
        for (int i=0; i < fieldsForCheck.length; i++) {
            String checkedFieldName = fieldsForCheck[i][0];

            TypeComponent checkedField = null;
            try {
                checkedField = targetVMClass.fieldByName(checkedFieldName);
            } catch (Throwable thrown) {
                logOnError(errorLogPrefixHead + "ReferenceType.fieldByName() throws unexpected exception: ");
                logOnError(errorLogPrefix + "Class name = '" + targetVMClassName + "'");
                logOnError(errorLogPrefix + "Requested field name = '" + checkedFieldName + "'");
                testResult = STATUS_FAILED;
                continue;
            }
            if (checkedField == null) {
                logOnError(errorLogPrefixHead + "Could NOT find Field object in debugee's class: ");
                logOnError(errorLogPrefix + "Debugee's class name = '" + targetVMClassName + "'");
                logOnError(errorLogPrefix + "Requested field name = '" + checkedFieldName + "'");
                testResult = STATUS_FAILED;
                continue;
            }
            String expectedGenericSignature = fieldsForCheck[i][1];
            String actualGenericSignature = null;
            try {
                actualGenericSignature = checkedField.genericSignature();
            } catch (Throwable thrown) {
                logOnError(errorLogPrefixHead + "TypeComponent.genericSignature() throws unexpected exception: ");
                logOnError(errorLogPrefix + "TypeComponent = '" + checkedField + "'");
                logOnError(errorLogPrefix + "Field name for this TypeComponent = '" + checkedFieldName + "'");
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
                logOnError(errorLogPrefixHead + "TypeComponent.genericSignature() returns unexpected signature: ");
                logOnError(errorLogPrefix + "TypeComponent = '" + checkedField + "'");
                logOnError(errorLogPrefix + "Field name for this TypeComponent = '" + checkedFieldName + "'");
                logOnError(errorLogPrefix + "Expected generic signature = '" + expectedGenericSignature + "'");
                logOnError(errorLogPrefix + "Actual generic signature = '" + actualGenericSignature + "'");
                testResult = STATUS_FAILED;
            }

        }
        logOnVerbose(infoLogPrefixHead
            + "Check TypeComponent.genericSignature() method for debugee's fields completed.");

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
} // end of genericSignature001 class
