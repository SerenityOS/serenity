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

package nsk.jdi.ArrayReference.getValue;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.io.*;
import java.util.*;

public class getvalue003 {

    // exit code when test failed
    public final static int TEST_FAILED = 2;
    // exit code when test passed
    public final static int TEST_PASSED = 0;
    // shift of exit code
    public final static int JCK_STATUS_BASE = 95;

    private final static String prefix = "nsk.jdi.ArrayReference.getValue.";
    private final static String className = "getvalue003";
    private final static String debuggerName = prefix + className;
    private final static String debugeeName = debuggerName + "a";
    private final static String fieldToCheck = "testedObj";

    private int exitStatus;
    private Log log;
    private Debugee debugee;
    private IOPipe pipe;

    private getvalue003() {
        log = null;
        debugee = null;
        pipe = null;
    }

    public static void main(String argv[]) {
        System.exit(JCK_STATUS_BASE + run(argv, System.out));
    }

    public static int run(String argv[], PrintStream out) {

        getvalue003 tstObj = new getvalue003();

        if ( tstObj.prepareDebugee(argv, out) ) {
            tstObj.execTest();
            tstObj.disposeOfDebugee();
        }

        if ( tstObj.exitStatus ==  TEST_FAILED )
            tstObj.complain("run:: TEST FAILED");
        else
            tstObj.display("run:: TEST PASSED");
        return tstObj.exitStatus;
    }

    private boolean prepareDebugee(String argv[], PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);
        display("prepareDebugee:: binder created.");

        debugee = binder.bindToDebugee(debugeeName);
        log.display("prepareDebugee:: binded to debugee.");
        pipe = debugee.createIOPipe();
        log.display("prepareDebugee:: pipe created.");

        debugee.redirectStderr(out);
        debugee.resume();

        String line = pipe.readln();
        if ( line == null ) {
            complain("prepareDebugee:: UNEXPECTED debugee's signal - null");
            return false;
        }
        if ( !line.equals("ready") ) {
            complain("prepareDebugee:: UNEXPECTED debugee's signal - "
                      + line);
            return false;
        }

        display("prepareDebugee:: debugee's \"ready\" signal recieved.");
        return true;
    }

    private boolean disposeOfDebugee() {
        pipe.println("quit");
        debugee.waitFor();
        int status = debugee.getStatus();

        if ( status != JCK_STATUS_BASE ) {
            complain("disposeOfDebugee:: UNEXPECTED Debugee's exit "
                       + "status (not " + JCK_STATUS_BASE + ") - " + status);
            return false;
        }
        display("disposeOfDebugee:: expected Debugee's exit "
                  + "status - " + status);
        return true;
    }

    private void display(String msg) {
        if ( log != null )
            log.display("debugger> " + msg);
    }

    private void complain(String msg) {
        if ( log != null )
            log.complain("debugger FAILURE> " + msg);
    }

    private boolean execTest() {
        exitStatus = TEST_FAILED;

        ReferenceType refType = debugee.classByName(debugeeName);
        if ( refType == null ) {
            complain("eventHandler:: Class '" + debugeeName + "' not found.");
            return false;
        }

        Field field = refType.fieldByName(fieldToCheck);
        if ( field == null ) {
            complain("eventHandler:: Field '" + fieldToCheck + "' not found.");
            return false;
        }

        Value value = refType.getValue(field);
        if ( value == null ) {
            complain("eventHandler:: Field '" + fieldToCheck + "' not initialized.");
            return false;
        }

        return checkObjectFields(value);
    }

    public boolean checkObjectFields(Value value) {
        List fieldList;
        if ( ! (value instanceof ObjectReference) )
            return false;

        fieldList = ((ClassType)value.type()).allFields();

        // Check all array fields from debugee
        Field field;
        display("\ncheckObjectFields:: Tests starts >>>");
        for (int i = 0; i < fieldList.size(); i++) {
            field = (Field)fieldList.get(i);

            display("");
            display("checkObjectFields:: <" + field.name()  + "> field is being "
                        + " checked.");

            // Check getting of item from field-array
            if ( !checkFieldValue((ObjectReference)value, field) )
                return false;
        }
        exitStatus = TEST_PASSED;
        return true;
    }

    private boolean checkFieldValue(ObjectReference object, Field field) {
        Value value;
        ArrayReference arrayRef;
        String fieldName = field.name();
        try {
            value = object.getValue(field);
        } catch (IllegalArgumentException e) {
            complain("checkFieldValue:: can not get value for field " + fieldName);
            complain("checkFieldValue:: " + e);
            return false;
        }

        display("checkFieldValue:: ***" + fieldName + " = " + value);

        boolean checkNULL = false;
        // scaning of non-initialized arrays
        for ( int i = 0; i < getvalue003a.NON_INIT_FIELDS.length; i++ )
        {
            if ( fieldName.compareTo(getvalue003a.NON_INIT_FIELDS[i]) == 0 ) {
                checkNULL = true;
                break;
            }
        }

        // checking of field value
        if ( checkNULL ) {

            // value is not null, but array has not to be initialized.
            if ( value != null ) {
                complain("checkFieldValue:: Value of '" + fieldName + "' is " + value
                            + ", but IndexOutOfBoundsException expected.");
                return false;

            // array is not initialized. Expected value is null
            } else {
                display("checkFieldValue:: Expected value is null.");
                return true;
            }
        } else {

            // value is null, but array has to be initialized.
            if ( value == null ) {
                complain("checkFieldValue:: Unexpected value of '" + fieldName
                                + "'" + value);
                return false;
            }
        }

        display("checkFieldValue:: *** type of " + fieldName + " = " + value.type());

        // check up type of value. it has to be ArrayType
        if ( ! (value.type() instanceof ArrayType) ) {
            display("checkFieldValue:: type of value is not ArrayType.");
            return false;
        }

        // Cast to ArrayReference. All fields in debugee are
        // arrays, so ClassCastException should not be thrown
        return checkValue(0, fieldName, (ArrayReference )value, ((ArrayReference )value).length() + 1) &&
                    checkValue(0, fieldName, (ArrayReference )value, Integer.MAX_VALUE) &&
                    checkValue(0, fieldName, (ArrayReference )value, Integer.MAX_VALUE + 1);
    }

    private boolean checkValue(int depth, String name, ArrayReference arrayRef,
                                    long itemIndex) {

        Value itemValue;
        int length = arrayRef.length();
        try {
            itemValue = arrayRef.getValue(0);
            if ( itemValue != null ) {
                if ( itemValue.type() instanceof ArrayType ) {

                    // itemValue has array type, check it by the same way
                    long index =  (length + 1 != itemIndex) ? itemIndex :
                                        ((ArrayReference )itemValue).length() + 1;
                    if ( !checkValue(depth + 1, name, (ArrayReference )itemValue, index) )
                        return false;
                }
            }
            itemValue = arrayRef.getValue((int)itemIndex);
            if ( itemIndex > length || itemIndex < 0 ) {
                complain("checkValue[" + depth + "]:: " + name + "[" + itemIndex + "] = "
                               + itemValue + ", but IndexOutOfBoundsException expected.");
                return false;
            }

        } catch (IndexOutOfBoundsException e) {
            /* Index is always out of bounds, so
             * IndexOutOfBoundsException is expected
             */
            display("checkValue[" + depth + "]:: expected IndexOutOfBoundsException " +
                        "is thrown for " + itemIndex + " item.");
        } catch (Exception e) {
            complain("checkValue[" + depth + "]:: Unexpected exception: " + e);
            return false;
        }
        return true;
    }

}
