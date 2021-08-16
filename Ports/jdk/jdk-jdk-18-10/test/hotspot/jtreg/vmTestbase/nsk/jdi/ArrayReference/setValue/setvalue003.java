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

package nsk.jdi.ArrayReference.setValue;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.io.*;
import java.util.*;

public class setvalue003 {

    // exit code when test failed
    public final static int TEST_FAILED = 2;
    // exit code when test passed
    public final static int TEST_PASSED = 0;
    // shift of exit code
    public final static int JCK_STATUS_BASE = 95;

    // parameters arrays to call com.sum.jdi.ArrayReference.setValue(int, Value)
    private static long[] INDEX_PARAM = { Integer.MAX_VALUE + 1,
                                            -1,
                                             0,
                                             Integer.MAX_VALUE
    };
    private final static String BYTE_VALUES_FIELD = "BYTE_VALUE_PARAM";
    private final static String CHAR_VALUES_FIELD = "CHAR_VALUE_PARAM";
    private final static String DBL_VALUES_FIELD  = "DBL_VALUE_PARAM";
    private final static String FLT_VALUES_FIELD  = "FLT_VALUE_PARAM";
    private final static String INT_VALUES_FIELD  = "INT_VALUE_PARAM";
    private final static String LNG_VALUES_FIELD  = "LNG_VALUE_PARAM";
    private final static String SHORT_VALUES_FIELD= "SHORT_VALUE_PARAM";

    private final static String prefix = "nsk.jdi.ArrayReference.setValue.";
    private final static String className = "setvalue003";
    private final static String debuggerName = prefix + className;
    private final static String debugeeName = debuggerName + "a";
    private final static String objectToCheck = "testedObj";

    private int exitStatus;
    private Log log;
    private Debugee debugee;
    private IOPipe pipe;
    private ReferenceType refType;

    public static void main(String argv[]) {
        System.exit(JCK_STATUS_BASE + run(argv, System.out));
    }

    public static int run(String argv[], PrintStream out) {

        setvalue003 tstObj = new setvalue003();

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

        refType = debugee.classByName(debugeeName);
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
        display("<" + fieldName + "> field is being checked.");
        try {
            fieldValue = object.getValue(field);
        } catch (IllegalArgumentException e) {
            complain("checkFieldValue:: can not get value for field " + fieldName);
            complain("checkFieldValue:: " + e);
            return false;
        }

        display("***" + fieldName + " = " + fieldValue);

        // Tested object doesn't have non-initialized fields!
        if ( fieldValue == null ) {
            complain("unexpected field value <"
                            + fieldValue + ">");
            return false;
        }

        display("*** type of " + fieldName + " = " + fieldValue.type());

        // Checking up of value type.
        // Tested object doesn't have other fields than be ArrayType
        if ( ! (fieldValue.type() instanceof ArrayType) ) {
            display("type of value is not ArrayType.");
            return false;
        }

        boolean res = true;

        Type itemType;
        try {
            itemType = ((ArrayType )fieldValue.type()).componentType();
        } catch(Exception e) {
            complain("Unexpected "  + e.getClass().getName() );
            return false;
        }

        // getting value to set from debugee with defined type
        Field fieldOfValues = null;
        if ( itemType instanceof ByteType ) {
            fieldOfValues = refType.fieldByName(BYTE_VALUES_FIELD);
            if ( fieldOfValues == null ) {
                complain("Field '" + BYTE_VALUES_FIELD + "' not found.");
                return false;
            }
        } else if ( itemType instanceof CharType ) {
            fieldOfValues = refType.fieldByName(CHAR_VALUES_FIELD);
            if ( fieldOfValues == null ) {
                complain("Field '" + CHAR_VALUES_FIELD + "' not found.");
                return false;
            }
        } else if ( itemType instanceof DoubleType ) {
            fieldOfValues = refType.fieldByName(DBL_VALUES_FIELD);
            if ( fieldOfValues == null ) {
                complain("Field '" + DBL_VALUES_FIELD + "' not found.");
                return false;
            }
        } else if ( itemType instanceof FloatType ) {
            fieldOfValues = refType.fieldByName(FLT_VALUES_FIELD);
            if ( fieldOfValues == null ) {
                complain("Field '" + FLT_VALUES_FIELD + "' not found.");
                return false;
            }
        } else if ( itemType instanceof IntegerType ) {
            fieldOfValues = refType.fieldByName(INT_VALUES_FIELD);
            if ( fieldOfValues == null ) {
                complain("Field '" + INT_VALUES_FIELD + "' not found.");
                return false;
            }
        } else if ( itemType instanceof LongType ) {
            fieldOfValues = refType.fieldByName(LNG_VALUES_FIELD);
            if ( fieldOfValues == null ) {
                complain("Field '" + LNG_VALUES_FIELD + "' not found.");
                return false;
            }
        } else if ( itemType instanceof ShortType ) {
            fieldOfValues = refType.fieldByName(SHORT_VALUES_FIELD);
            if ( fieldOfValues == null ) {
                complain("Field '" + SHORT_VALUES_FIELD + "' not found.");
                return false;
            }
        }

        ArrayReference values = (ArrayReference )refType.getValue(fieldOfValues);

        // Checking up of test cases.
        for ( int i = 0; i < INDEX_PARAM.length; i++ ) {
            for ( int j = 0; j < values.length(); j++ ) {
                res = checkValueUpdating(fieldName,
                                (ArrayReference )fieldValue, INDEX_PARAM[i],
                                values.getValue(j)) && res;
            }
        }
        return res;
    }

    private boolean checkValueUpdating(String name,
                        ArrayReference arrayRef, long index, Value value) {
        Value itemValue;
        List list;
        String il2Str =  "for index=" + index + " value=" + value;
        int arrayLength = arrayRef.length();

        try {
            arrayRef.setValue((int )index, value);
            Value v1 = arrayRef.getValue((int )index);

            if ( !value.equals(v1) ) {
                complain("not correct value " + v1
                                + " " + il2Str);
                return false;
            }

            if ( index < 0 || index >= arrayLength ) {
                complain("IndexOutOfBoundsException is expected " + il2Str);
                return false;
            } else {
                display("Value " + v1 + " is set " + il2Str);
            }

        } catch (IndexOutOfBoundsException e) {

            // checking specification conditions
            if ( index < 0 || index >= arrayLength ) {
                display("expected IndexOutOfBoundsException "
                            + il2Str);
            } else {
                complain("unexpected IndexOutOfBoundsException "
                            + il2Str);
                return false;
            }

        } catch (Exception e) {
            complain("Unexpected exception: "
                            + e + " " + il2Str);
                return false;
        }
        return true;
    }
}
