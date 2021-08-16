/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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


package nsk.jdi.Field.equals;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

public class equals005 {
    private static Log log;
    private final static String prefix = "nsk.jdi.Field.equals.";
    private final static String className = "equals005";
    private final static String debugerName = prefix + className;
    private final static String debugeeName = debugerName + "a";

    public static void main(String argv[]) {
        System.exit(95 + run(argv, System.out));
    }

    public static int run(String argv[], PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);
        Debugee debugee = binder.bindToDebugee(debugeeName
                              + (argHandler.verbose() ? " -verbose" : ""));
        IOPipe pipe = new IOPipe(debugee);
        boolean testFailed = false;
        List fields;

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

        // Get all fields from debugee
        ReferenceType refType = debugee.classByName(debugeeName);
        if (refType == null) {
           log.complain("debuger FAILURE> Class " + debugeeName
                      + " not found.");
           return 2;
        }
        try {
            fields = refType.allFields();
        } catch (Exception e) {
            log.complain("debuger FAILURE> Can't get fields from class");
            log.complain("debuger FAILURE> Exception: " + e);
            return 2;
        }
        int totalFields = fields.size();
        if (totalFields < 1) {
            log.complain("debuger FAILURE> Total number of fields read "
                       + totalFields);
            return 2;
        }
        log.display("debuger> Total fields found: " + totalFields);
        Iterator fieldsIterator = fields.iterator();
        for (int i = 0; fieldsIterator.hasNext(); i++) {
            Field srcField = (Field)fieldsIterator.next();
            String name = srcField.name();
            Field checkField;
            boolean fieldsEqual;

            if (name == null) {
                log.complain("debuger FAILURE 1> Name is null for " + i
                           + " field");
                testFailed = true;
                continue;
            }
            try {
                checkField = refType.fieldByName(name);
            } catch (Exception e) {
                log.complain("debuger FAILURE 2> Can't create field to check "
                           + "for field " + name);
                testFailed = true;
                continue;
            }
            fieldsEqual = srcField.equals(checkField);
            log.display("debuger> Fields " + name + " and " + checkField.name()
                      + " compared, result " + fieldsEqual);
            if (!fieldsEqual) {
                // Fields declared in the same class and mirror the same
                // fields are equal
                log.complain("debuger FAILURE 3> Same fields with name " + name
                           + " are not equal. Expected result: equal.");
                testFailed = true;
                continue;
            }
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
