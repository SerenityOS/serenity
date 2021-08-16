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

package nsk.jdi.Type.hashCode;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.request.*;
import com.sun.jdi.event.*;
import com.sun.jdi.connect.*;
import java.io.*;
import java.util.*;

/**
 * The debugger application of the test.
 */
public class hashcode001 {

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

    private final static String prefix = "nsk.jdi.Type.hashCode.";
    private final static String className = "hashcode001";
    private final static String debuggerName = prefix + className;
    private final static String debuggeeName = debuggerName + "a";

    //------------------------------------------------------- test specific fields

    private final static String[] checkedFields = {
        "z0", "z1", "z2",
        "b0", "b1", "b2",
        "c0", "c1", "c2",
        "d0", "d1", "d2",
        "f0", "f1", "f2",
        "i0", "i1", "i2",
        "l0", "l1", "l2",
        "r0", "r1", "r2",
        "s0", "s1", "s2",
        "o0", "o1", "o2"
                                                  };

    private final static String[] checkedMethods = {
        "Mv",  "MvS", "MvI",
        "MvY", "MvU", "MvR",
        "MvP", "MvN",
        "Mz",  "Mb",  "Mc",
        "Md",  "Mf",  "Mi",
        "Ml",  "Mr",  "Ms", "Mo"
                                                   };

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

        debuggee = Debugee.prepareDebugee(argHandler, log, debuggeeName);

        debuggeeClass = debuggee.classByName(debuggeeName);
        if ( debuggeeClass == null ) {
            complain("Class '" + debuggeeName + "' not found.");
            exitStatus = Consts.TEST_FAILED;
        }

        execTest();

        debuggee.quit();

        return exitStatus;
    }

    //------------------------------------------------------ mutable common method

    private static void execTest() {

        display("Checking hashCode() method for Type mirrors of debuggee's fields");

        for (int i = 0; i < checkedFields.length; i++) {
            checkHashCodeForField(i);
        }

        display("Checking hashCode() method for Type mirrors of debuggee's void methods");

        for (int i = 0; i < checkedMethods.length; i++) {
            checkHashCodeForMethod(i);
        }

        display("Checking completed!");
        debuggee.resume();
    }

    //--------------------------------------------------------- test specific methods

    private static void checkHashCodeForField (int ind) {
        Type type;
        try {
            type = (Type)debuggeeClass.fieldByName(checkedFields[ind]).type();
        } catch (ClassNotLoadedException e) {
            throw new Failure("Unexpected ClassNotLoadedException was thrown while first getting Type " +
                "mirror for field : " + checkedFields[ind]);
        }
        int hCode = type.hashCode();

        if (hCode == 0) {
            complain("hashCode() returns 0 for debuggee's field : " + checkedFields[ind] );
            exitStatus = Consts.TEST_FAILED;
        }

        int hCode1 = type.hashCode();
        if (hCode != hCode1) {
            complain("hashCode() is not consistent for debuggee's field : " + checkedFields[ind] +
                "\n\t first value :" + hCode + " ; second value : " + hCode1);
            exitStatus = Consts.TEST_FAILED;
        }

        // get new reference to the same debuggee's field and get hash code.
        try {
            type = (Type)debuggeeClass.fieldByName(checkedFields[ind]).type();
        } catch (ClassNotLoadedException e) {
            throw new Failure("Unexpected ClassNotLoadedException was thrown while second getting Type " +
                "mirror for field : " + checkedFields[ind]);
        }
        hCode1 = type.hashCode();
        if (hCode != hCode1) {
            complain("hashCode() does not return same value for equal mirror of debuggee's field : " + checkedFields[ind] +
                "\n\t first value :" + hCode + " ; second value : " + hCode1);
            exitStatus = Consts.TEST_FAILED;
        }

        display("hashCode() returns for debuggee's field : " + checkedFields[ind] + " : " + hCode);
    }

    private static void checkHashCodeForMethod (int ind) {
        Type type;
        try {
            type = (Type)methodByName(debuggeeClass, checkedMethods[ind]).returnType();
        } catch (ClassNotLoadedException e) {
            throw new Failure("Unexpected ClassNotLoadedException was thrown while first getting Type " +
                "mirror for method : " + checkedMethods[ind]);
        }
        int hCode = type.hashCode();

        if (hCode == 0) {
            complain("hashCode() returns 0 for debuggee's method : " + checkedMethods[ind] );
            exitStatus = Consts.TEST_FAILED;
        }

        int hCode1 = type.hashCode();
        if (hCode != hCode1) {
            complain("hashCode() is not consistent for debuggee's method : " + checkedMethods[ind] +
                "\n\t first value :" + hCode + " ; second value : " + hCode1);
            exitStatus = Consts.TEST_FAILED;
        }

        // get new reference to the same debuggee's method and get hash code.
        try {
            type = (Type)methodByName(debuggeeClass, checkedMethods[ind]).returnType();
        } catch (ClassNotLoadedException e) {
            throw new Failure("Unexpected ClassNotLoadedException was thrown while second getting Type " +
                "mirror for method : " + checkedMethods[ind]);
        }
        hCode1 = type.hashCode();
        if (hCode != hCode1) {
            complain("hashCode() does not return same value for equal mirror of debuggee's method : " + checkedMethods[ind] +
                "\n\t first value :" + hCode + " ; second value : " + hCode1);
            exitStatus = Consts.TEST_FAILED;
        }

        display("hashCode() returns for debuggee's method : " + checkedMethods[ind] + " : " + hCode);
    }

    private static Method methodByName(ReferenceType refType, String methodName) {
        List methodList = refType.methodsByName(methodName);
        if (methodList == null) return null;

        Method method = (Method) methodList.get(0);
        return method;
    }

}
//--------------------------------------------------------- test specific classes
