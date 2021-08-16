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

package nsk.jdi.IntegerValue.equals;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import java.io.*;
import java.util.*;
import java.util.jar.*;

/**
 *  The test check up case when parameter has boundary values of primitive <br>
 *  types and when parameter is <code>null</code>.<br>
 *  Analyse of the method executing is performed by <code>PerformComparing</code>
 *  method.<br>
 *  First parameter of <code>PerformComparing</code> is static field of <code>testedObj</code>,<br>
 *  which is placed onto debugee's side. <br>
 *
 *  Second parameter is got from debugee too:<br>
 *  Debugee has array of boundary values of each primitive type, <code>execTest</code> reads<br>
 *  them and calls <code>PerformComparing</code> for each of them.<br>
 */
public class equals002 {

    private final static String prefix = "nsk.jdi.IntegerValue.equals.";
    private final static String className = "equals002";
    private final static String debuggerName = prefix + className;
    private final static String debugeeName = debuggerName + "a";
    private final static String objectToCheck = "testedObj";
    private final static String arrPrimitives = "testedFields";

    private static int exitStatus;
    private static Log log;
    private static Debugee debugee;
    private static ReferenceType refType;

    public static void main(String argv[]) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    public static int run(String argv[], PrintStream out) {

        exitStatus = Consts.TEST_FAILED;

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        debugee = Debugee.prepareDebugee(argHandler, log, debugeeName);
        execTest();
        debugee.quit();

        return exitStatus;
    }

    private static void display(String msg) {
        log.display("debugger> " + msg);
    }

    private static void complain(String msg) {
        log.complain("debugger FAILURE> " + msg + "\n");
    }

    private static void execTest() {

        exitStatus = Consts.TEST_FAILED;

        refType = debugee.classByName(debugeeName);
        if ( refType == null ) {
            complain("Class '" + debugeeName + "' not found.");
            return;
        }

        // getting of object to check
        Field field = refType.fieldByName(objectToCheck);
        if ( field == null ) {
            complain("Field '" + objectToCheck + "' not found.");
            return;
        }
        Value objectValue = refType.getValue(field);
        if ( objectValue == null ) {
            complain("Field '" + objectToCheck + "' not initialized.");
            return;
        }

        // geting of array of primitive types
        field = refType.fieldByName(arrPrimitives);
        if ( field == null ) {
            complain("Field '" + arrPrimitives + "' not found.");
            return;
        }
        Value arrValue = refType.getValue(field);
        if ( arrValue == null || !(arrValue instanceof ArrayReference) ) {
            complain("Field '" + arrValue + "' is wrong.");
            return;
        }
        ArrayReference primitivValues = (ArrayReference )arrValue;

        List fieldList = ((ClassType )objectValue.type()).allFields();

        Value v1, currentValue;
        IntegerValue value;
        Field fldOtherType;
        String msg;

        exitStatus = Consts.TEST_PASSED;

        // comparing loop
        for (int i = 0; i < fieldList.size(); i++ ) {
            field = (Field )fieldList.get(i);
            log.display("");
            msg = "***" + field;
            v1 = ((ObjectReference )objectValue).getValue(field);
            if ( !(v1 instanceof IntegerValue) ) {
                msg += " is not IntegerValue (skipped)";
                exitStatus = Consts.TEST_FAILED;
                continue;
            }
            value = (IntegerValue )v1;

            // comparing with debugee's fields
            for (int j = 0; j < primitivValues.length(); j++) {
                arrValue = primitivValues.getValue(j);

                fldOtherType = refType.fieldByName(((StringReference )arrValue).value());
                if ( fldOtherType == null ) {
                    complain("Field '" + arrValue + "' not found.");
                    exitStatus = Consts.TEST_FAILED;
                    continue;
                }

                currentValue = refType.getValue(fldOtherType);

                if ( !PerformComparing(value, currentValue) )
                    exitStatus = Consts.TEST_FAILED;
            }
        }
    }

    private static boolean PerformComparing(IntegerValue value, Object object ) {
        boolean res = true;
        String msg = "";
        try {
            if ( value.equals(object) ) {
                if ( object instanceof IntegerValue ) {
                    if ( value.value() == ((PrimitiveValue )object).intValue() ) {
                        msg += "--> " + value + " == "  + object;
                    } else {
                        msg += "##> " + value + " == "  + object;
                        res = false;
                    }
                }
                else {
                    msg += "##> " + value + " == "  + object
                                + " : are different types " + value.type() + " - "
                                + ((Value )object).type();
                    res = false;
                }
                if ( object == null ) {
                    msg += " ***Wrong result!!!***";
                    res = false;
                }
            } else {
                if ( object instanceof IntegerValue ) {
                    if ( value.value() != ((PrimitiveValue )object).intValue() ) {
                        msg += "--> " + value + " != "  + object;
                    } else {
                        msg += "##> " + value + " != "  + object;
                        res = false;
                    }
                }
                else {
                    msg += "--> " + value + " != "  + object;
                }
            }
        } catch (Exception e) {
            msg += " ***Unexpected " + e + "***";
            res = false;
        }

        if ( !res )
            complain(msg);
        else
            display(msg);

        return res;
    }
}
