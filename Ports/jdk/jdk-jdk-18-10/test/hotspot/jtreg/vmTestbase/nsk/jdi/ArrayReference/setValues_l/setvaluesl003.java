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

package nsk.jdi.ArrayReference.setValues_l;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.io.*;
import java.util.*;

public class setvaluesl003 {

    private int[] failedTypes;

    private final static String prefix = "nsk.jdi.ArrayReference.setValues_l.";
    private final static String className = "setvaluesl003";
    private final static String debuggerName = prefix + className;
    private final static String debugeeName = debuggerName + "a";
    private final static String objectToCheck = "testedObj";

    private static Log log;

    private int exitStatus;
    private Debugee debugee;
    private IOPipe pipe;
    private ReferenceType refType;
    private ObjectReference testedObject;

    public static void main(String argv[]) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    public static int run(String argv[], PrintStream out) {

        setvaluesl003 tstObj = new setvaluesl003();

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        tstObj.debugee = Debugee.prepareDebugee(argHandler, log, debugeeName);

        tstObj.execTest();
        tstObj.debugee.quit();

        return tstObj.exitStatus;
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
        exitStatus = Consts.TEST_FAILED;

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

        boolean res = checkObjectFields(objectValue);
        exitStatus = res ? Consts.TEST_PASSED : Consts.TEST_FAILED;

        if ( exitStatus ==  Consts.TEST_FAILED )
            complain("run:: TEST FAILED");
        else
            display("run:: TEST PASSED");

        return res;
    }

    public boolean checkObjectFields(Value objectValue) {
        List fieldList;
        if ( ! (objectValue instanceof ObjectReference) )
            return false;

        testedObject = (ObjectReference )objectValue;
        fieldList = ((ClassType )testedObject.type()).allFields();

        // Check all array fields from debugee
        display("checkObjectFields:: Tests starts >>>");
        boolean res = true;
        failedTypes = new int[7];
        for ( int i = 0; i < fieldList.size(); i++) {
            res = checkFieldValue((Field )fieldList.get(i)) && res;
        }

        return res;
    }

    private boolean checkFieldValue(Field field) {
        Value fieldValue;
        ArrayReference arrayRef;
        String fieldName = field.name();
        log.display("");
//        log.display("<" + fieldName + "> field is being checked.");
        try {
            fieldValue = testedObject.getValue(field);
        } catch (IllegalArgumentException e) {
            log.complain("checkFieldValue:: can not get value for field " + fieldName);
            log.complain("checkFieldValue:: " + e);
            return false;
        }

        log.display("***<" + fieldName + "> = " + fieldValue);
        log.display("-----------------------------------------------------");

        // Checking up of value type.
        // Tested object doesn't have other fields than be ArrayType
        if ( ! (fieldValue.type() instanceof ArrayType) ) {
            log.display("type of value is not ArrayType.");
            return false;
        }

        boolean res = true;

        Type itemType;
        try {
            itemType = ((ArrayType )fieldValue.type()).componentType();
        } catch(Exception e) {
            log.complain("Unexpected "  + e.getClass().getName() );
            return false;
        }

        // Checking up of test cases.

        for ( int i = 0; i < failedTypes.length; i++ ) failedTypes[i] = i;

        List<? extends com.sun.jdi.Value> valuesList;
        // for every type
        for ( int i = 0; i < failedTypes.length; i++) {
            valuesList = generateValuesList(i);
            if ( valuesList == null ) {
                log.complain("no values list for <" + fieldName + "> "
                                + setvaluesl003a.COMBINE[i]);
                res = false;
                continue;
            }
            res = checkValueUpdating(fieldName, (ArrayReference )fieldValue,
                                            valuesList, i) && res;
        }

        // for all passed types
        valuesList = generateValuesList(-1);
        if ( valuesList == null ) {
            log.complain("no values list for <" + fieldName + "> ");
            return false;
        }
        res = checkValueUpdating(fieldName, (ArrayReference )fieldValue,
                                        valuesList, -1) && res;
        res = checkValueUpdating(fieldName, (ArrayReference )fieldValue,
                                        null, -1) && res;
        log.display("");
        return res;
    }

    // when index >= failedTypes.length(), all passed types are added into generated list
    private List<? extends com.sun.jdi.Value> generateValuesList(int index) {
        ArrayReference values;
        Field fieldOfValues = null;
        if ( index < setvaluesl003a.COMBINE.length && index >= 0 ) {
            fieldOfValues = refType.fieldByName(setvaluesl003a.COMBINE[index]);
            try {
                if ( fieldOfValues == null ||
                        !(fieldOfValues.type() instanceof ArrayType) ) {
                    log.complain("fieldOfValues " + fieldOfValues + " "
                                    + setvaluesl003a.COMBINE[index]);
                    return null;
                }
            } catch (ClassNotLoadedException e) {
                log.complain("unexpected exception " + e);
                return null;
            }
            values = (ArrayReference )refType.getValue(fieldOfValues);
            return  values.getValues();
        }

        List<com.sun.jdi.Value> valuesList = new Vector<com.sun.jdi.Value>();
        for ( int i = 0; i < failedTypes.length; i++) {
            if ( failedTypes[i] < 0 ) continue;
            fieldOfValues = refType.fieldByName(setvaluesl003a.COMBINE[i]);
            try {
                if ( fieldOfValues == null ||
                        !(fieldOfValues.type() instanceof ArrayType) ) {
                    log.complain("fieldOfValues " + fieldOfValues + " "
                                        + setvaluesl003a.COMBINE[i]);
                    return null;
                }
            } catch (ClassNotLoadedException e) {
                log.complain("unexpected exception " + e);
                return null;
            }
            values = (ArrayReference )refType.getValue(fieldOfValues);
            for ( int j = 0; j < values.length(); j++ ) {
                valuesList.add(values.getValue(j));
            }
        }
        if ( valuesList.size() == 0 ) valuesList = null;

        return valuesList;
    }

    private boolean checkValueUpdating(String name, ArrayReference arrayRef,
                                            List<? extends com.sun.jdi.Value> values, int index) {
        Value itemValue;
        List list;

        boolean validConversion = true;

        String valuesStr = "";
        if ( values != null ) {
            for ( int i = 0; i < values.size(); i++ ) {
                valuesStr = valuesStr + (Value )values.get(i) + "; ";
                validConversion = validConversion &&
                                    isValidConversion((ArrayType )arrayRef.type(),
                                                        ((Value )values.get(i)).type());
            }
        }

        if ( index < 0 )
            log.display("values list: (mixed)" + valuesStr);
        else
            log.display("values list: (" + ((Value )values.get(0)).type() + ")" + valuesStr);

        try {
            arrayRef.setValues(values);

            if ( !validConversion ) {
                log.complain("     InvalidTypeException is expected");
                log.display("\n");
                return false;
            }

            if ( values == null ) {
                log.complain("     NullPointerException is expected");
                log.display("\n");
                return false;
            }
            log.display("     expected NullPointerException");

            if ( !checkValues(arrayRef, values) ) {
                log.complain("     Wrong result of setting");
                log.display("\n");
                return false;
            }

            log.display("     Values were correctly set");

        } catch (InvalidTypeException e) {

            if ( index >= 0 ) failedTypes[index] = -1;

            if ( validConversion ) {
                log.complain("     unexpected InvalidTypeException");
                log.display("\n");
                return false;
            }
            log.display("     expected InvalidTypeException");

        } catch (ClassNotLoadedException e) {

            log.complain("     unexpected ClassNotLoadedException");
            log.display("\n");
            return false;
        } catch (NullPointerException e) {

            if ( values != null ) {
                log.complain("     Unexpected NullPointerException");
                return false;
            }
            log.display("     expected NullPointerException");
        } catch (Exception e) {
            log.complain("     Unexpected exception: " + e);
            log.display("\n");
            return false;
        }
        return true;
    }

    private boolean isValidConversion(ArrayType arrType, Type valueType) {
        String typeSignature = "BCDFIJS";
        Type type = null;
        try {
            type = arrType.componentType();
        } catch(ClassNotLoadedException e) {
            return true; // have to be true always
        }
        int i = typeSignature.indexOf(type.signature());
        return -1 != setvaluesl003a.VALID_CONVERSIONS[i].indexOf(valueType.signature());
    }

    private boolean checkValues(ArrayReference array, List list) {
        if ( list == null ) return true;
        int length = array.length() < list.size() ? array.length()
                                                      : list.size();
        PrimitiveValue val1, val2;
        for ( int i = 0; i < length; i++ ) {
            val1 = (PrimitiveValue )array.getValue(i);
            val2 = (PrimitiveValue  )list.get(i);
            if ( !val1.equals(val2) ) {
                Type type = val1.type();
                if ( type instanceof ByteType )
                    return val1.byteValue() == val2.byteValue();
                else if ( type instanceof CharType )
                    return val1.charValue() == val2.charValue();
                else if ( type instanceof DoubleType )
                    return val1.doubleValue() == val2.doubleValue();
                else if ( type instanceof FloatType )
                    return val1.floatValue() == val2.floatValue();
                else if ( type instanceof IntegerType )
                    return val1.intValue() == val2.intValue();
                else if ( type instanceof LongType )
                    return val1.longValue() == val2.longValue();
                else if ( type instanceof ShortType )
                    return val1.shortValue() == val2.shortValue();
                else
                    return false;
            }
        }
        return true;
    }
}
