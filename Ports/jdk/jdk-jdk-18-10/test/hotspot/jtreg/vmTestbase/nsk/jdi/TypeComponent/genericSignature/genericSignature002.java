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
 * for Method objects.                                                 <BR>
 *                                                                     <BR>
 * The first: the test checks that genericSignature() method returns   <BR>
 * null for methods which have not generic signature. The methods with <BR>
 * different sets of arguments and with different returned types are   <BR>
 * used.                                                               <BR>
 *                                                                     <BR>
 * The second: the test checks that genericSignature() method returns  <BR>
 * a corresponding signature string for methods which have generic     <BR>
 * signature. The methods with different sets of generic types         <BR>
 * arguments and with different returned generic types are used.       <BR>
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
    static final String packagePrefix = "nsk.jdi.TypeComponent.genericSignature.";
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


    private final static String primitiveTypeSign = "primitiveType";
    private final static String referenceTypeSign = "referenceType";

    /**
     * Debugee's methods for check:
     * methodsForCheck[i][0] - method name
     * methodsForCheck[i][1] - expected string returned by genericSignature()
     */
    private final static String methodsForCheck[][] = {
        {"testMethod_001", null},
        {"testMethod_002", null},
        {"testMethod_003", null},
        {"testMethod_004", null},
        {"testMethod_005", null},
        {"testMethod_006", null},
        {"testMethod_007", null},
        {"testMethod_008", null},
        {"testMethod_009", null},
        {"testMethod_010", null},
        {"testMethod_011",
            "(Lnsk/jdi/TypeComponent/genericSignature/GS002_Class06<Lnsk/jdi/TypeComponent/genericSignature/GS002_Class01;>;)V"},
        {"testMethod_012",
            "(Lnsk/jdi/TypeComponent/genericSignature/GS002_Class06<Lnsk/jdi/TypeComponent/genericSignature/GS002_Class01;>;)Lnsk/jdi/TypeComponent/genericSignature/GS002_Class06<Lnsk/jdi/TypeComponent/genericSignature/GS002_Class01;>;"},
        {"testMethod_013",
            "(Lnsk/jdi/TypeComponent/genericSignature/GS002_Class07<Lnsk/jdi/TypeComponent/genericSignature/GS002_Class01;Lnsk/jdi/TypeComponent/genericSignature/GS002_Class02;>;)V"},
        {"testMethod_014",
            "(Lnsk/jdi/TypeComponent/genericSignature/GS002_Class07<Lnsk/jdi/TypeComponent/genericSignature/GS002_Class01;Lnsk/jdi/TypeComponent/genericSignature/GS002_Class02;>;)Lnsk/jdi/TypeComponent/genericSignature/GS002_Class07<Lnsk/jdi/TypeComponent/genericSignature/GS002_Class01;Lnsk/jdi/TypeComponent/genericSignature/GS002_Class02;>;"},
        {"testMethod_015",
            "(Lnsk/jdi/TypeComponent/genericSignature/GS002_Class08<Lnsk/jdi/TypeComponent/genericSignature/GS002_Class03;>;)V"},
        {"testMethod_016",
            "(Lnsk/jdi/TypeComponent/genericSignature/GS002_Class08<Lnsk/jdi/TypeComponent/genericSignature/GS002_Class03;>;)Lnsk/jdi/TypeComponent/genericSignature/GS002_Class08<Lnsk/jdi/TypeComponent/genericSignature/GS002_Class03;>;"},
        {"testMethod_017",
            "(Lnsk/jdi/TypeComponent/genericSignature/GS002_Class09<Lnsk/jdi/TypeComponent/genericSignature/GS002_Class04;>;)V"},
        {"testMethod_018",
            "(Lnsk/jdi/TypeComponent/genericSignature/GS002_Class09<Lnsk/jdi/TypeComponent/genericSignature/GS002_Class04;>;)Lnsk/jdi/TypeComponent/genericSignature/GS002_Class09<Lnsk/jdi/TypeComponent/genericSignature/GS002_Class04;>;"},
        {"testMethod_019",
            "(Lnsk/jdi/TypeComponent/genericSignature/GS002_Class10<Lnsk/jdi/TypeComponent/genericSignature/GS002_Class04;Lnsk/jdi/TypeComponent/genericSignature/GS002_Class05;>;)V"},
        {"testMethod_020",
            "(Lnsk/jdi/TypeComponent/genericSignature/GS002_Class10<Lnsk/jdi/TypeComponent/genericSignature/GS002_Class04;Lnsk/jdi/TypeComponent/genericSignature/GS002_Class05;>;)Lnsk/jdi/TypeComponent/genericSignature/GS002_Class10<Lnsk/jdi/TypeComponent/genericSignature/GS002_Class04;Lnsk/jdi/TypeComponent/genericSignature/GS002_Class05;>;"},
        {"testMethod_021",
            "<C:Ljava/lang/Object;>()V"},
        {"testMethod_022",
            "<C:Ljava/lang/Object;>(TC;)V"},
        {"testMethod_023",
            "<C:Ljava/lang/Object;>(TC;)TC;"},
        {"testMethod_024",
            "<C1:Ljava/lang/Object;C2:Ljava/lang/Object;>()V"},
        {"testMethod_025",
            "<C1:Ljava/lang/Object;C2:Ljava/lang/Object;>(TC1;TC2;)V"},
        {"testMethod_026",
            "<C1:Ljava/lang/Object;C2:Ljava/lang/Object;>(TC1;TC2;)TC2;"},
        {"testMethod_027",
            "<C1:Lnsk/jdi/TypeComponent/genericSignature/GS002_Class01;:Lnsk/jdi/TypeComponent/genericSignature/GS002_Interf01;C2:Lnsk/jdi/TypeComponent/genericSignature/GS002_Class02;:Lnsk/jdi/TypeComponent/genericSignature/GS002_Interf02;>()V"},
        {"testMethod_028",
            "<C1:Lnsk/jdi/TypeComponent/genericSignature/GS002_Class01;:Lnsk/jdi/TypeComponent/genericSignature/GS002_Interf01;C2:Lnsk/jdi/TypeComponent/genericSignature/GS002_Class02;:Lnsk/jdi/TypeComponent/genericSignature/GS002_Interf02;>(TC2;TC1;)V"},
        {"testMethod_029",
            "<C1:Lnsk/jdi/TypeComponent/genericSignature/GS002_Class01;:Lnsk/jdi/TypeComponent/genericSignature/GS002_Interf01;C2:Lnsk/jdi/TypeComponent/genericSignature/GS002_Class02;:Lnsk/jdi/TypeComponent/genericSignature/GS002_Interf02;>(TC2;TC1;)TC1;"},

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
            logAlways("\n##> nsk/jdi/TypeComponent/genericSignature/genericSignature002 test FAILED");
        }
        else {
            logAlways("\n==> nsk/jdi/TypeComponent/genericSignature/genericSignature002 test PASSED");
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

        logAlways("==> nsk/jdi/TypeComponent/genericSignature/genericSignature002 test...");
        logOnVerbose
            ("==> Test checks the genericSignature() method of TypeComponent interface");
        logOnVerbose
            ("==> of the com.sun.jdi package for Method objects.");

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

        logOnVerbose(infoLogPrefixHead + "check TypeComponent.genericSignature() method for debugee's methods...");
        String brackets[] = {"", "[]", "[][]"};
        for (int i=0; i < methodsForCheck.length; i++) {
            String checkedMethodName = methodsForCheck[i][0];

            List checkedMethodList = null;
            try {
                checkedMethodList = targetVMClass.methodsByName(checkedMethodName);
            } catch (Throwable thrown) {
                logOnError(errorLogPrefixHead + "ReferenceType.methodsByName() throws unexpected exception: ");
                logOnError(errorLogPrefix + "Class name = '" + targetVMClassName + "'");
                logOnError(errorLogPrefix + "Requested method name = '" + checkedMethodName + "'");
                testResult = STATUS_FAILED;
                continue;
            }

            if (checkedMethodList.size() != 1) {
                logOnError(errorLogPrefixHead + "ReferenceType.methodsByName() returns unexpected number of methods:");
                logOnError(errorLogPrefix + "Debugee's class name = '" + targetVMClassName + "'");
                logOnError(errorLogPrefix + "Requested method name = '" + checkedMethodName + "'");
                logOnError(errorLogPrefix + "Expected number of methods = 1");
                logOnError(errorLogPrefix + "Actual number of methods = " + checkedMethodList.size());
                testResult = STATUS_FAILED;
                continue;
            }

            TypeComponent checkedMethod = (TypeComponent)(checkedMethodList.get(0));

            String expectedGenericSignature = methodsForCheck[i][1];
            String actualGenericSignature = null;
            try {
                actualGenericSignature = checkedMethod.genericSignature();
            } catch (Throwable thrown) {
                logOnError(errorLogPrefixHead + "TypeComponent.genericSignature() throws unexpected exception: ");
                logOnError(errorLogPrefix + "TypeComponent = '" + checkedMethod + "'");
                logOnError(errorLogPrefix + "Method name for this TypeComponent = '" + checkedMethodName + "'");
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
                logOnError(errorLogPrefix + "TypeComponent = '" + checkedMethod + "'");
                logOnError(errorLogPrefix + "Method name for this TypeComponent = '" + checkedMethodName + "'");
                logOnError(errorLogPrefix + "Expected generic signature = '" + expectedGenericSignature + "'");
                logOnError(errorLogPrefix + "Actual generic signature = '" + actualGenericSignature + "'");
                testResult = STATUS_FAILED;
            }

        }
        logOnVerbose(infoLogPrefixHead
            + "Check TypeComponent.genericSignature() method for debugee's methods completed.");

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
