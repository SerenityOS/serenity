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

public class equals003 {
    private static Log log;
    private final static String prefix = "nsk.jdi.Field.equals.";
    private final static String className = "equals003";
    private final static String debugerName = prefix + className;
    private final static String debugeeName = debugerName + "a";
    private final static String mainClassName = prefix + "MainClass";
    private final static String sameClass1Name = prefix + "SameClass1";
    private final static String sameClass2Name = prefix + "SameClass2";
    private final static String overridenClassName = prefix + "OverridenClass";

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
        List mainFields;
        Field mainField;
        Field same1Field;
        Field same2Field;
        Field overridenField;

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

        // Get all fields from all classes
        ReferenceType mainClassRefType = debugee.classByName(mainClassName);
        if (mainClassRefType == null) {
           log.complain("debuger FAILURE> Class " + mainClassName + " not "
                      + "found.");
           return 2;
        }
        ReferenceType sameClass1RefType = debugee.classByName(sameClass1Name);
        if (sameClass1RefType == null) {
           log.complain("debuger FAILURE> Class " + sameClass1Name + " not "
                      + "found.");
           return 2;
        }
        ReferenceType sameClass2RefType = debugee.classByName(sameClass2Name);
        if (sameClass2RefType == null) {
           log.complain("debuger FAILURE> Class " + sameClass2Name + " not "
                      + "found.");
           return 2;
        }
        ReferenceType overridenClassRefType =
            debugee.classByName(overridenClassName);
        if (overridenClassRefType == null) {
           log.complain("debuger FAILURE> Class " + overridenClassName
                      + " not found.");
           return 2;
        }

        try {
            mainFields = mainClassRefType.allFields();
        } catch (Exception e) {
            log.complain("debuger FAILURE> Can't get fields from class "
                       + mainClassName);
            log.complain("debuger FAILURE> Exception: " + e);
            return 2;
        }
        int totalFields = mainFields.size();
        if (totalFields < 1) {
            log.complain("debuger FAILURE> Total number of fields in class "
                       + mainClassName + " read: " + totalFields);
            return 2;
        }
        log.display("debuger> Total number of fields in class "
                  + mainClassName + " read: " + totalFields);
        Iterator fieldsIterator = mainFields.iterator();

        // Check each field of MainClass with fields from classes
        // SameClass1, SameClass2, OverridenClass
        for (int i = 0; fieldsIterator.hasNext(); i++) {
            mainField = (Field)fieldsIterator.next();
            String name = mainField.name();

            if (name == null) {
                log.complain("debuger FAILURE 1> Name is null for " + i
                           + " field");
                testFailed = true;
                continue;
            }
            log.display("debuger> " + i + " field " + name + " from class "
                      + mainClassName + " read.");

            // Compare fields from classes MainClass and SameClass1
            log.display("debuger> Compare classes " + mainClassName + " and "
                      + sameClass1Name);
            try {
                same1Field = sameClass1RefType.fieldByName(name);
            } catch (Exception e) {
                log.complain("debuger FAILURE 2> Can't get field by name "
                           + name + " in class " + sameClass1Name);
                log.complain("debuger FAILURE 2> Exception: " + e);
                testFailed = true;
                continue;
            }
            log.display("debuger> Field " + name + " from class "
                      + sameClass1Name + " read.");
            // Fields in different classes but defined in same class are equal
            if (! mainField.equals(same1Field)) {
                log.complain("debuger FAILURE 3> Fields " + name + " from "
                           + "classes " + mainClassName + " and "
                           + sameClass1Name + " are not equal. Expected "
                           + "result: equal.");
                testFailed = true;
                continue;
            }

            // Compare fields from classes MainClass and OverridenClass
            log.display("debuger> Compare classes " + mainClassName
                      + " and " + overridenClassName);
            try {
                overridenField = overridenClassRefType.fieldByName(name);
            } catch (Exception e) {
                log.complain("debuger FAILURE 4> Can't get field by name "
                           + name + " in class " + overridenClassName);
                log.complain("debuger FAILURE 4> Exception: " + e);
                testFailed = true;
                continue;
            }
            log.display("debuger> Field " + name + " from class "
                      + overridenClassName + " read.");
            // Fields in different classes are not equal
            if (mainField.equals(overridenField)) {
                log.complain("debuger FAILURE 5> Fields " + name
                           + " from classes " + mainClassName + " and "
                           + overridenClassName + " are equal. Expected "
                           + "result: not equal.");
                testFailed = true;
                continue;
            }

            // Compare fields from classes SameClass1 and SameClass2
            log.display("debuger> Compare classes " + sameClass1Name
                      + " and " + sameClass2Name);
            try {
                same2Field = sameClass2RefType.fieldByName(name);
            } catch (Exception e) {
                log.complain("debuger FAILURE 6> Can't get field by name "
                           + name + " in class " + sameClass2Name);
                log.complain("debuger FAILURE 6> Exception: " + e);
                testFailed = true;
                continue;
            }
            log.display("debuger> Field " + name + " from class "
                      + sameClass2Name + " read.");
            // Fields in different classes but defined in same class are equal
            if (!same1Field.equals(same2Field)) {
                log.complain("debuger FAILURE 7> Fields " + name
                           + " from classes " + sameClass1Name + " and "
                           + sameClass2Name + " are not equal. Expected "
                           + "result: equal.");
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
