/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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


package nsk.jdi.ArrayReference.length;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.io.*;

public class length001 {
    final static String FIELD_NAME[][] = {
        {"z1", "0"},
        {"z2", "7"},
        {"b1", "1"},
        {"b2", "6"},
        {"c1", "2"},
        {"c2", "5"},
        {"d1", "3"},
        {"d2", "4"},
        {"f1", "4"},
        {"f2", "3"},
        {"i1", "5"},
        {"i2", "2"},
        {"l1", "6"},
        {"l2", "1"},
        {"r1", "7"},
        {"r2", "0"},

        {"lF1", "3"},
        {"lP1", "3"},
        {"lU1", "2"},
        {"lR1", "2"},
        {"lT1", "1"},
        {"lV1", "1"},

        {"E1", "0"},
        {"E2", "2"},
        {"X1", "1"},
        {"X2", "1"},
        {"O1", "2"},
        {"O2", "0"},

        {"LF1", "3"},
        {"LP1", "3"},
        {"LU1", "2"},
        {"LR1", "2"},
        {"LT1", "1"},
        {"LV1", "1"},

        {"EF1", "0"},
        {"EP1", "1"},
        {"EU1", "1"},
        {"ER1", "1"},
        {"ET1", "1"},
        {"EV1", "1"},
    };

    private static Log log;
    private final static String prefix = "nsk.jdi.ArrayReference.length.";
    private final static String className = "length001";
    private final static String debugerName = prefix + className;
    private final static String debugeeName = debugerName + "a";
    private final static String classToCheckName = prefix + "ClassToCheck";

    public static void main(String argv[]) {
        System.exit(95 + run(argv, System.out));
    }

    public static int run(String argv[], PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);
        Debugee debugee = binder.bindToDebugee(debugeeName
                              + (argHandler.verbose() ? " -verbose" : ""));
        IOPipe pipe = debugee.createIOPipe();
        boolean testFailed = false;

        // Connect with debugee and resume it
        debugee.redirectStderr(out);
        debugee.resume();
        String line = pipe.readln();
        if (line == null) {
            log.complain("debuger FAILURE> UNEXPECTED debugee's signal - null");
            return 2;
        }
        if (!line.equals("ready")) {
            log.complain("debuger FAILURE> UNEXPECTED debugee's signal - "
                      + line);
            return 2;
        }
        else {
            log.display("debuger> debugee's \"ready\" signal recieved.");
        }

        ReferenceType refType = debugee.classByName(classToCheckName);
        if (refType == null) {
           log.complain("debuger FAILURE> Class " + classToCheckName
                      + " not found.");
           return 2;
        }
        log.display("debuger> Total fields in debugee read: "
                  + refType.allFields().size() + " total fields in debuger: "
                  + FIELD_NAME.length + "\n");

        // Check all array fields from debugee
        for (int i = 0; i < FIELD_NAME.length; i++) {
            Field field;
            String name = FIELD_NAME[i][0];
            String realLength = FIELD_NAME[i][1];
            Value value;
            ArrayReference arrayRef;
            int length;
            String lengthStr;

            // Get field from debuggee by name
            try {
                field = refType.fieldByName(name);
            } catch (ClassNotPreparedException e) {
                log.complain("debuger FAILURE 1> Can't get field by name "
                           + name);
                log.complain("debuger FAILURE 1> Exception: " + e);
                testFailed = true;
                continue;
            } catch (ObjectCollectedException e) {
                log.complain("debuger FAILURE 1> Can't get field by name "
                           + name);
                log.complain("debuger FAILURE 1> Exception: " + e);
                testFailed = true;
                continue;
            }
            log.display("debuger> " + i + " field " + field + " read.");

            // Get field's value
            try {
                value = refType.getValue(field);
            } catch (IllegalArgumentException e) {
                log.complain("debuger FAILURE 2> Cannot get value for field "
                           + name);
                log.complain("debuger FAILURE 2> Exception: " + e);
                testFailed = true;
                continue;
            } catch (ObjectCollectedException e) {
                log.complain("debuger FAILURE 2> Cannot get value for field "
                           + name);
                log.complain("debuger FAILURE 2> Exception: " + e);
                testFailed = true;
                continue;
            }
            log.display("debuger> " + i + " field value is " + value);

            // Cast to ArrayReference. All fields in debugee are
            // arrays, so ClassCastException should not be thrown
            try {
                arrayRef = (ArrayReference)value;
            } catch (ClassCastException e) {
                log.complain("debuger FAILURE 3> Cannot cast value for field "
                           + name + " to ArrayReference.");
                log.complain("debuger FAILURE 3> Exception: " + e);
                testFailed = true;
                continue;
            }

            // Get length of ArrayReference object
            try {
                length = arrayRef.length();
            } catch (ObjectCollectedException e) {
                log.complain("debuger FAILURE 4> Cannot get length for array "
                           + name);
                log.complain("debuger FAILURE 4> Exception: " + e);
                testFailed = true;
                continue;
            }
            log.display("debuger> " + i + " field length is " + length);
            lengthStr = realLength.valueOf(length);

            // Check array's length
            if (!realLength.equals(lengthStr)) {
                log.complain("debuger FAILURE 5> Length of array " + name
                           + " is " + length + ", but expected " + realLength);
                testFailed = true;
            }
            log.display("debuger> " + i + " field checked.\n");
        }

        pipe.println("quit");
        debugee.waitFor();
        int status = debugee.getStatus();
        if (testFailed) {
            log.complain("debuger FAILURE> TEST FAILED");
            return 2;
        } else {
            if (status == 95) {
                log.display("debuger> expected Debugee's exit "
                          + "status - " + status);
                return 0;
            } else {
                log.complain("debuger FAILURE> UNEXPECTED Debugee's exit "
                           + "status (not 95) - " + status);
                return 2;
            }
        }
    }
}
