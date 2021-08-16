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

package nsk.jdi.ClassLoaderReference.definedClasses;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import java.io.*;
import java.util.*;
import java.util.jar.*;

/**
 */
public class definedclasses005 {

    //------------------------------------------------------- immutable common fields

    final static String SIGNAL_READY = "ready";
    final static String SIGNAL_GO    = "go";
    final static String SIGNAL_QUIT  = "quit";

    private static int waitTime;
    private static int exitStatus;
    private static ArgumentHandler     argHandler;
    private static Log                 log;
    private static Debugee             debuggee;
    private static ReferenceType       debuggeeClass;

    //------------------------------------------------------- mutable common fields

    private final static String prefix = "nsk.jdi.ClassLoaderReference.definedClasses.";
    private final static String className = "definedclasses005";
    private final static String debuggerName = prefix + className;
    private final static String debuggeeName = debuggerName + "a";

    //------------------------------------------------------- test specific fields

    private final static String classLoaderName = prefix + "fields002aClassLoader";
    private final static String classFieldName = "loadedClass";

    //------------------------------------------------------- immutable common methods

    public static void main(String argv[]) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    private static void display(String msg) {
        log.display("debugger > " + msg);
    }

    private static void complain(String msg) {
        log.complain("debugger FAILURE > " + msg);
    }

    public static int run(String argv[], PrintStream out) {

        exitStatus = Consts.TEST_PASSED;

        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        waitTime = argHandler.getWaitTime() * 60000;

        try {
            debuggee = Debugee.prepareDebugee(argHandler, log, debuggeeName);
            debuggeeClass = debuggee.classByName(debuggeeName);
            if ( debuggeeClass == null ) {
                throw new Failure("Class '" + debuggeeName + "' not found.");
            } else {
                execTest();
            }
        } catch (Exception e) {
            exitStatus = Consts.TEST_FAILED;
            complain("Unexpected Exception: " + e.getMessage());
            e.printStackTrace(out);
        } finally {
            debuggee.quit();
        }

        return exitStatus;
    }

    //------------------------------------------------------ mutable common method

    private static void execTest() {

        while (true) {
            Field classField = debuggeeClass.fieldByName(classFieldName);
            if ( classField == null) {
                complain("Checked field is not found in the debuggee: " + classFieldName);
                exitStatus = Consts.TEST_FAILED;
                break;
            }

            Value classValue = debuggeeClass.getValue(classField);

            ClassObjectReference classObjRef = null;
            try {
                classObjRef = (ClassObjectReference)classValue;
            } catch (Exception e) {
                complain("Unexpected exception while getting ClassObjectReference : " + e.getMessage());
                exitStatus = Consts.TEST_FAILED;
                break;
            }

            ReferenceType refType1 = classObjRef.reflectedType();
            display("isPrepared() returned " + refType1.isPrepared() + " for " + refType1);

            display("Getting ClassLoaderReference of ReferenceType: " + refType1 );
            ClassLoaderReference classLoader = refType1.classLoader();
            if (classLoader == null) {
                complain("classLoader() method returned null for ReferenceType: " + refType1);
                exitStatus = Consts.TEST_FAILED;
                break;
            }

            display("Getting definedClasses list.");
            Iterator definedClasses = classLoader.definedClasses().iterator();
            display("Checking returned list...");
            while (definedClasses.hasNext()) {
                ReferenceType refType = (ReferenceType)definedClasses.next();
                if (refType.equals(refType1)) {
                    if (refType.isPrepared()) {
                        display("isPrepared() returned true for " + refType);
                    } else {
                        complain("Not-prepared ReferenceType : " + refType + "\n\t is found in definedClasses() list of " + classLoaderName + " mirror.");
                        exitStatus = Consts.TEST_FAILED;
                    }
                } else {
                    complain("Unexpected ReferenceType : " + refType + "\n\t is found in definedClasses() list of " + classLoaderName + " mirror.");
                    exitStatus = Consts.TEST_FAILED;
                }
            }
            break;
        }
    }

    //--------------------------------------------------------- test specific methods

}
//--------------------------------------------------------- test specific classes
