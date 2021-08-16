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

package nsk.jdi.CharValue.compareTo;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.io.*;
import java.util.*;


public class compareto001 {
    //------------------------------------------------------- immutable common fields

    final static String SIGNAL_READY = "ready";
    final static String SIGNAL_GO    = "go";
    final static String SIGNAL_QUIT  = "quit";

    private static int waitTime;
    private static int exitStatus;
    private static ArgumentHandler     argHandler;
    private static Log                 log;
    private static IOPipe pipe;
    private static Debugee             debuggee;
    private static ReferenceType       debuggeeClass;

    //------------------------------------------------------- mutable common fields

    private final static String prefix = "nsk.jdi.CharValue.compareTo";
    private final static String className = ".compareto001";
    private final static String debuggerName = prefix + className;
    private final static String debuggeeName = debuggerName + "a";

    //------------------------------------------------------- test specific fields

    private final static String objectToCheck = "testedObj";
    private final static String arrPrimitives = "testedFields";
    private static Value objectValue;
    private static List fieldList;

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

        exitStatus = Consts.TEST_FAILED;

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
        debuggeeClass = debuggee.classByName(debuggeeName);
        if ( debuggeeClass == null ) {
            complain("Class '" + debuggeeName + "' not found.");
            return;
        }

        // getting of object to check
        Field field = debuggeeClass.fieldByName(objectToCheck);
        if ( field == null ) {
            complain("Field '" + objectToCheck + "' not found.");
            return;
        }

        objectValue = debuggeeClass.getValue(field);
        if ( objectValue == null ) {
            complain("Field '" + objectToCheck + "' not initialized.");
            return;
        }

        // geting of array of primitive types
        field = debuggeeClass.fieldByName(arrPrimitives);
        if ( field == null ) {
            complain("Field '" + arrPrimitives + "' not found.");
            return;
        }
        Value arrValue = debuggeeClass.getValue(field);
        if ( arrValue == null || !(arrValue instanceof ArrayReference) ) {
            complain("Field '" + arrValue + "' is wrong.");
            return;
        }
        ArrayReference primitiveValues = (ArrayReference)arrValue;

        fieldList = ((ClassType )objectValue.type()).allFields();

        Value v1, currentValue;
        CharValue value;
        Field fldOtherType;

        exitStatus = Consts.TEST_PASSED;

        // comparing loop
        for (int i = 0; i < fieldList.size(); i++ ) {
            field = (Field )fieldList.get(i);
            v1 = ((ObjectReference )objectValue).getValue(field);
            if ( !(v1 instanceof CharValue) ) {
                exitStatus = Consts.TEST_FAILED;
                continue;
            }
            value = (CharValue)v1;

            // comparing with debuggee's fields
            display("Checking compateTo(Object object) method for CharValue: " + value);

            for (int j = 0; j < primitiveValues.length(); j++) {
                arrValue = primitiveValues.getValue(j);

                fldOtherType = debuggeeClass.fieldByName(((StringReference)arrValue).value());
                if ( fldOtherType == null ) {
                    complain("Field '" + arrValue + "' not found.");
                    exitStatus = Consts.TEST_FAILED;
                    continue;
                }

                currentValue = debuggeeClass.getValue(fldOtherType);

                if ( !PerformComparing(value, currentValue) )
                    exitStatus = Consts.TEST_FAILED;
            }
        }

    }

    //--------------------------------------------------------- test specific methods


    private static boolean PerformComparing(CharValue value, Object object ) {
        boolean result = true;

        // assertion [ x.compareTo(x) == 0 ]
        if (value.compareTo(value) != 0) {
            complain("Failed assertion [ x.compareTo(x) == 0 ] for value: " + value.toString());
            result = false;
        }

        if (object instanceof CharValue) {
            CharValue charObject = (CharValue)object;
            try {
                // assertion [ x.compareTo(y) == 0 <==> x.equals(y) ]
                if ( ((value.equals(object)) && (value.compareTo(charObject) != 0)) ||
                     (!(value.equals(object)) && (value.compareTo(charObject) == 0)) ) {
                    complain("Failed assertion [ (x.compareTo(y) == 0) is identical to (x.equals(y) == true) ] \n\t" +
                             "where 'x' is CharValue: " + value + " and 'y' is CharValue : " + charObject + " \n\t" +
                             "result of (x.compareTo(y)): " + value.compareTo(charObject) + "\n\t" +
                             "result of (x.equals(y)): " + value.equals(object) );
                    result = false;
                }

                // assertion [ x.compareTo(y) == 0 <==> y.compareTo(x) == 0 ]
                if ( ((value.compareTo(charObject) == 0) && (charObject.compareTo(value) != 0)) ||
                     ((value.compareTo(charObject) != 0) && (charObject.compareTo(value) == 0)) ) {
                    complain("Failed assertion [ (x.compareTo(y) == 0) is identical to (y.compareTo(x) == 0) ] \n\t" +
                             "where 'x' is CharValue: " + value + " and 'y' is CharValue : " + charObject + " \n\t" +
                             "result of (x.compareTo(y)): " + value.compareTo(charObject) + "\n\t" +
                             "result of (y.compareTo(x)): " + charObject.compareTo(value) );
                    result = false;
                }
                if (value.compareTo(charObject) != 0) {
                    // assertion [ if (x.compareTo(y) == i) then (y.compareTo(x) == -i) ]
                    if (value.compareTo(charObject) != -(charObject.compareTo(value))) {
                        complain("Failed assertion [ if (x.compareTo(y) == i) then (y.compareTo(x) == -i) ] \n\t" +
                             "where 'x' is CharValue: " + value + " and 'y' is CharValue : " + charObject + " \n\t" +
                             "result of (x.compareTo(y)): " + value.compareTo(charObject) + "\n\t" +
                             "result of (y.compareTo(x)): " + charObject.compareTo(value) );
                        result = false;
                    }
                }

                // assertion [ if (x.compareTo(y) > 0) and (y.compareTo(z) > 0), then (x.compareTo(z) > 0) ]
                if (value.compareTo(charObject) > 0) {
                     CharValue lessValue = FindLessCharValue(charObject);
                     if (lessValue != null) {
                         if (value.compareTo(lessValue) <= 0) {
                              complain("Failed assertion [ if (x.compareTo(y) > 0) and (y.compareTo(z) > 0), then (x.compareTo(z) > 0) ] \n\t" +
                             "where 'x' is CharValue: " + value + " , 'y' is CharValue : " + charObject + " , 'z' is CharValue : " + lessValue + " \n\t" +
                             "result of (x.compareTo(y)): " + value.compareTo(charObject) + "\n\t" +
                             "result of (y.compareTo(z)): " + charObject.compareTo(lessValue)  + "\n\t" +
                             "result of (x.compareTo(z)): " + value.compareTo(lessValue) );
                              result = false;
                         }
                     }
                }

            } catch (Exception e) {
                complain("Caught unexpected " + e + " when comparing \n\t" +
                         "CharValue: " + value + " and CharValue argument: " + object);
                result = false;
            }

        } else if (object == null) {
            try {
                value.compareTo(null);
                complain("Does not throw expected NullPointerException when comparing \n\t" +
                         "CharValue: " + value + " and null argument");
                result = false;
            } catch (NullPointerException ne) {
                // continue
            } catch (Exception e) {
                complain("Caught unexpected " + e + " when comparing \n\t" +
                         "CharValue: " + value + " and null argument");
                result = false;
            }
        } else {
            try {
                value.compareTo((CharValue)object);
                complain("Does not throw expected ClassCastException when comparing \n\t" +
                         "CharValue: " + value + " and argument: " + object);
                result = false;
            } catch (ClassCastException ne) {
                // continue
            } catch (Exception e) {
                complain("Caught unexpected " + e + " when comparing \n\t" +
                         "CharValue: " + value + " and argument: " + object);
                result = false;
            }
        }

        return result;
    }

    /**
     *  This function searches the static <i>fieldList</i> - the list of mirrored
     *  fields of debuggee's <i>compareto001aClassToCheck</i> class. Search is aimed
     *  to find another CharValue field which is less then method's argument via
     *  <i>compareTo</i> method.
     */

    private static CharValue FindLessCharValue (CharValue currentValue) {
        CharValue result = null;

        for (int i = 0; i < fieldList.size(); i++ ) {

            Field field = (Field )fieldList.get(i);
            CharValue newValue = (CharValue)((ObjectReference )objectValue).getValue(field);

            if (currentValue.compareTo(newValue) > 0) {
                result = newValue;
                break;
            }
        }
        return result;
    }
}
//--------------------------------------------------------- test specific classes
