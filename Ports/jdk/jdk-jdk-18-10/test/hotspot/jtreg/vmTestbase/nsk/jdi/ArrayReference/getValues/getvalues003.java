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

package nsk.jdi.ArrayReference.getValues;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.io.*;
import java.util.*;

public class getvalues003 {

    // exit code when test failed
    public final static int TEST_FAILED = 2;
    // exit code when test passed
    public final static int TEST_PASSED = 0;
    // shift of exit code
    public final static int JCK_STATUS_BASE = 95;

    private final static String prefix = "nsk.jdi.ArrayReference.getValues.";
    private final static String className = "getvalues003";
    private final static String debuggerName = prefix + className;
    private final static String debugeeName = debuggerName + "a";
    private final static String fieldToCheck = "testedObj";

    private int exitStatus;
    private Log log;
    private Debugee debugee;
    private IOPipe pipe;

    private getvalues003() {
        log = null;
        debugee = null;
        pipe = null;
    }

    public static void main(String argv[]) {
        System.exit(JCK_STATUS_BASE + run(argv, System.out));
    }

    public static int run(String argv[], PrintStream out) {

        getvalues003 tstObj = new getvalues003();

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
        String name = field.name();
        try {
            value = object.getValue(field);
        } catch (IllegalArgumentException e) {
            complain("checkFieldValue:: can not get value for field " + name);
            complain("checkFieldValue:: " + e);
            return false;
        }

        display("checkFieldValue:: ***" + field.name() + " = " + value);

        // non-initialized fields hav not to be
        if ( value == null ) {
            complain("checkFieldValue:: value is null.");
            return false;
        }
        display("checkFieldValue:: *** type of " + field.name() + " = " + value.type());

        // check up type of value. it has to be ArrayType
        if ( ! (value.type() instanceof ArrayType) ) {
            display("checkFieldValue:: type of value is not ArrayType.");
            return true;
        }

        // Cast to ArrayReference. All fields in debugee are
        // arrays, so ClassCastException should not be thrown
        return checkValue(name, (ArrayReference )value);
    }

    private boolean checkValue(String name, ArrayReference arrayRef) {

        // Get all components from array
        List listOfValues;
        try {
            listOfValues = arrayRef.getValues();
        } catch (Exception e) {
            complain("checkValue:: Unexpected exception: " + e);
            return false;
        }
        display("checkValue:: length of ArrayReference object  - " + arrayRef.length());
        display("checkValue:: size of values list              - " + listOfValues.size());

        if ( listOfValues.size() == 0 ) {
            display("checkValue:: <" + name + "> field has been checked.\n");
            return true;
        }
        complain("checkValue:: <" + name + "> field has non-zero length.\n");
        return false;
    }

}
