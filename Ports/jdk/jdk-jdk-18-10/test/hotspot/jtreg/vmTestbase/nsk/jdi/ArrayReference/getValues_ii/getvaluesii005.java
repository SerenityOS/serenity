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

package nsk.jdi.ArrayReference.getValues_ii;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.io.*;
import java.util.*;

/** The test cases include static and instance fields of int and Object types,
 *  which are one- and two- dimensional arrays (non-initialized arrays are
 *  considered as well). Values of the method parameter are presented as static
 *  two-dimensional array onto debuger, which includes the combinations of
 *  <code>Integer.MAX_VALUE + 1</code>, <code>-1</code>, <code>0</code>,
 *  <code>1</code>, <code>Integer.MAX_VALUE</code>. <br>
 *
 *  Expected results: <br>
 *   - when index is <code>0</code> and length is <code>-1</code>,
 *   size of returned list has to be array length; <br>
 *   - when index is <code>0</code> and length is <code>0</code>,
 *   size if returned list has to be zero; <br>
 *   - otherwise, <code>IndexOutOfBoundsException</code> is expected.<br>
 */
public class getvaluesii005 {

    // exit code when test failed
    public final static int TEST_FAILED = 2;
    // exit code when test passed
    public final static int TEST_PASSED = 0;
    // shift of exit code
    public final static int JCK_STATUS_BASE = 95;

    // parameters array to call com.sum.jdi.ArrayReference.getValues(int, int)
    private static long[][] PARAM_ARRS = {
                     /* index */                   /* length */
                {Integer.MAX_VALUE + 1,   Integer.MAX_VALUE + 1},
                {Integer.MAX_VALUE + 1,                      -1},
                {Integer.MAX_VALUE + 1,                       0},
                {Integer.MAX_VALUE + 1,                       1},
                {Integer.MAX_VALUE + 1,       Integer.MAX_VALUE},
                {-1,                      Integer.MAX_VALUE + 1},
                {-1,                                         -1},
                {-1,                                          0},
                {-1,                          Integer.MAX_VALUE},
                {0,                       Integer.MAX_VALUE + 1},
                {0,                                          -1},
                {0,                                           0},
                {0,                           Integer.MAX_VALUE},
                {Integer.MAX_VALUE,       Integer.MAX_VALUE + 1},
                {Integer.MAX_VALUE,                          -1},
                {Integer.MAX_VALUE,                           0},
                {Integer.MAX_VALUE,                           1},
                {Integer.MAX_VALUE,           Integer.MAX_VALUE}
    };

    private final static String prefix = "nsk.jdi.ArrayReference.getValues_ii.";
    private final static String className = "getvaluesii005";
    private final static String debuggerName = prefix + className;
    private final static String debugeeName = debuggerName + "a";
    private final static String objectToCheck = "testedObj";

    private int exitStatus;
    private Log log;
    private Debugee debugee;
    private IOPipe pipe;

    public static void main(String argv[]) {
        System.exit(JCK_STATUS_BASE + run(argv, System.out));
    }

    public static int run(String argv[], PrintStream out) {

        getvaluesii005 tstObj = new getvaluesii005();

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

        debugee.redirectStderr(log,"");
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

        display("prepareDebugee:: debugee's \"ready\" signal received.");
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

        Field field = refType.fieldByName(objectToCheck);
        if ( field == null ) {
            complain("eventHandler:: Field '" + objectToCheck + "' not found.");
            return false;
        }

        Value objectValue = refType.getValue(field);
        if ( objectValue == null ) {
            complain("eventHandler:: Field '" + objectToCheck
                            + "' not initialized.");
            return false;
        }

        return checkObjectFields(objectValue);
    }

    public boolean checkObjectFields(Value objectValue) {
        List fieldList;
        if ( ! (objectValue instanceof ObjectReference) )
            return false;

        fieldList = ((ClassType )objectValue.type()).allFields();

        // Check all array fields from debugee
        display("checkObjectFields:: Tests starts >>>");
        boolean res = true;
        for (int i = 0; i < fieldList.size(); i++) {
            res = checkFieldValue((ObjectReference )objectValue,
                                        (Field )fieldList.get(i)) && res;
        }

        exitStatus = res ? TEST_PASSED : TEST_FAILED;
        return res;
    }

    private boolean checkFieldValue(ObjectReference object, Field field) {
        Value fieldValue;
        ArrayReference arrayRef;
        String fieldName = field.name();
        log.display("");
        display("checkObjectFields:: <" + fieldName + "> field is being "
                        + " checked.");
        try {
            fieldValue = object.getValue(field);
        } catch (IllegalArgumentException e) {
            complain("checkFieldValue:: can not get value for field " + fieldName);
            complain("checkFieldValue:: " + e);
            return false;
        }

        display("checkFieldValue:: ***" + fieldName + " = " + fieldValue);

        // Tested object doesn't have non-initialized fields!
        if ( fieldValue == null ) {
            complain("checkFieldValue:: unexpected field value <"
                            + fieldValue + ">");
            return false;
        }

        display("checkFieldValue:: *** type of " + fieldName + " = " + fieldValue.type());

        // Checking up of value type.
        // Tested object doesn't have other fields than be ArrayType
        if ( ! (fieldValue.type() instanceof ArrayType) ) {
            display("checkFieldValue:: type of value is not ArrayType.");
            return false;
        }

        boolean res = true;

        // Checking up of test cases.
        for ( int i = 0; i < PARAM_ARRS.length; i++ ) {
            res = checkValue(0, fieldName, (ArrayReference )fieldValue,
                                     PARAM_ARRS[i][0], PARAM_ARRS[i][1]) && res;
        }
        return res;
    }

    /** The metod returns the first item of the given ArrayReference
     *  as an array Value; or returns <tt>null</tt> if that item is
     *  not an array.
     */
    private Value getFirstItemAsArray(ArrayReference arrayRef) {
        Value itemValue = null;
        try {
            itemValue = arrayRef.getValue(0);
            if ((itemValue != null) && !(itemValue.type() instanceof ArrayType))
                itemValue = null;

        } catch (IndexOutOfBoundsException e) {
        }
        return itemValue;
    }

    private boolean checkValue(int depth, String name, ArrayReference arrayRef,
                                    long index, long length) {
        Value itemValue;
        List list;
        String il2Str =  "for index=" + index + ", length=" + length;
        int arrayLength = arrayRef.length();

        itemValue = getFirstItemAsArray(arrayRef);

        if ( itemValue != null ) {

            // itemValue has array type, check it by the same way
            long k =  (arrayLength + 1 != index) ? index :
                                ((ArrayReference )itemValue).length() + 1;
            if ( !checkValue(depth + 1, name, (ArrayReference )itemValue, k, length) )
                return false;
        }

        try {
            list = arrayRef.getValues((int )index, (int )length);

            if ( length == 0 ) {

                // expected list with zero-size
                if ( list.size() != 0 )
                    complain("checkValue[" + depth + "]:: List size is "
                                    + list.size() + ", but list with zero-size "
                                    + "expected " + il2Str);
                else {
                    display("checkValue[" + depth + "]:: expected list zero-size "
                                + il2Str);
                    return true;
                }
            } else if ( length == -1 && index == 0 ) {

                // expected list size to be equal array length
                if ( list.size() != arrayLength )
                    complain("checkValue[" + depth + "]:: unexpected list size("
                                    + list.size() + ") != array length("
                                    + arrayLength + ") " + il2Str);
                else {
                    display("checkValue[" + depth + "]:: expected list size("
                                        + list.size() + ") = array length("
                                        + arrayLength + ") " + il2Str);
                    return true;
                }
            }

            // ERR: IndexOutOfBoundsException was expected
            complain("checkValue[" + depth + "]:: IndexOutOfBoundsException "
                            + "expected " + il2Str);
            return false;

        } catch (IndexOutOfBoundsException e) {

            // checking specification conditions
            if ( (index < 0 || index > arrayLength)  ||
                    ( (length != -1) && (length < 0 || index + length > arrayLength) ) ) {
                display("checkValue[" + depth + "]:: expected IndexOutOfBoundsException "
                            + il2Str);
            } else {
                complain("checkValue[" + depth + "]:: unexpected IndexOutOfBoundsException "
                            + il2Str);
                return false;
            }

        } catch (Exception e) {

            complain("checkValue[" + depth + "]:: Unexpected exception: " + e);
            return false;
        }
        return true;
    }

}
