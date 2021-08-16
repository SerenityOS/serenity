/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

public class equals001 {
    private static Log log;
    private final static String prefix = "nsk.jdi.Field.equals.";
    private final static String className = "equals001";
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
        IOPipe pipe = new IOPipe(debugee);
        boolean testFailed = false;
        List fieldsDebugee;
        List methods;
        Iterator methodsIterator;
        java.lang.reflect.Field[] fieldsDebuger;
        ClassOfFields classOfFields = new ClassOfFields();

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
        ReferenceType refType = debugee.classByName(classToCheckName);
        if (refType == null) {
           log.complain("debuger FAILURE> Class " + classToCheckName
                      + " not found.");
           return 2;
        }
        try {
            fieldsDebugee = refType.allFields();
        } catch (Exception e) {
            log.complain("debuger FAILURE> Can't get fields from class");
            log.complain("debuger FAILURE> Exception: " + e);
            return 2;
        }
        int totalFields = fieldsDebugee.size();
        if (totalFields < 1) {
            log.complain("debuger FAILURE> Total number of fields read "
                       + totalFields);
            return 2;
        }
        log.display("debuger> Total fields found: " + totalFields);

        // Compare each Field and Method from debugee
        try {
            methods = refType.allMethods();
        } catch (Exception e) {
            log.complain("debuger FAILURE 1> Can't get methods from class");
            log.complain("debuger FAILURE 1> Exception: " + e);
            return 2;
        }
        log.display("debuger> Total methods found: " + methods.size());
        Iterator fieldsIterator = fieldsDebugee.iterator();
        for (int i = 0; fieldsIterator.hasNext(); i++) {
            Field fieldDebugee = (Field)fieldsIterator.next();
            String name = fieldDebugee.name();
            methodsIterator = methods.iterator();

            for (int j = 0; methodsIterator.hasNext(); j++) {
                Method method = (Method)methodsIterator.next();
                String methodName = method.name();

                log.display("debuger> Field " + name + " and method "
                           + methodName + " read from debugee.");
                if (fieldDebugee.equals(method)) {
                    // Field is not equal to any Object which type is not Field
                    log.complain("debuger FAILURE 2> Field " + name
                               + " and method " + methodName + " are equal. "
                               + "Expected result: not equal.");
                    testFailed = true;
                    continue;
                }
            }
        }

        // Compare Field (com.sun.jdi.Field) from debugee and Field
        // from debuger (java.lang.reflect.Field)
        Class classDebuger = classOfFields.getClass();
        try {
            fieldsDebuger = classDebuger.getDeclaredFields();
        } catch (SecurityException e) {
            log.complain("debuger FAILURE 3> Can't get fields from debuger's"
                       + " class");
            log.complain("debuger FAILURE 3> Exception: " + e);
            return 2;
        }
        int totalDebugerFields = fieldsDebuger.length;
        log.display("debuger> Total fields in debuger found: "
                  + totalDebugerFields);
        fieldsIterator = fieldsDebugee.iterator();
        for (int i = 0; fieldsIterator.hasNext(); i++) {
            Field fieldDebugee = (Field)fieldsIterator.next();
            String name = fieldDebugee.name();

            for (int j = 1; j < totalDebugerFields; j++) {
                java.lang.reflect.Field fieldDebuger = fieldsDebuger[j];
                String nameDebuger = fieldDebuger.getName();

                log.display("debuger> Field from debugee " + name + " and "
                          + "field from debuger " + nameDebuger + " read.");
                if (fieldDebugee.equals(fieldDebuger)) {
                    // Field is not equal to any Object which type
                    // is not not com.sun.jdi.Field
                    log.complain("debuger FAILURE 4> Field from debugee "
                               + name + " and field from debuger "
                               + nameDebuger + " are equal. Expected "
                               + "result: not equal.");
                    testFailed = true;
                    continue;
                }
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

class ClassOfFields {
    boolean z0, z1[]={z0}, z2[][]={z1};
    byte    b0, b1[]={b0}, b2[][]={b1};
    char    c0, c1[]={c0}, c2[][]={c1};
    double  d0, d1[]={d0}, d2[][]={d1};
    float   f0, f1[]={f0}, f2[][]={f1};
    int     i0, i1[]={i0}, i2[][]={i1};
    long    l0, l1[]={l0}, l2[][]={l1};

    static    long lS0, lS1[]={lS0}, lS2[][]={lS1};
    private   long lP0, lP1[]={lP0}, lP2[][]={lP1};
    public    long lU0, lU1[]={lU0}, lU2[][]={lU1};
    protected long lR0, lR1[]={lR0}, lR2[][]={lR1};
    transient long lT0, lT1[]={lT0}, lT2[][]={lT1};
    volatile  long lV0, lV1[]={lV0}, lV2[][]={lV1};
    final     long lF0 = 999, lF1[]={lF0}, lF2[][]={lF1};

    Boolean   Z0, Z1[]={Z0}, Z2[][]={Z1};
    Byte      B0, B1[]={B0}, B2[][]={B1};
    Character C0, C1[]={C0}, C2[][]={C1};
    Double    D0, D1[]={D0}, D2[][]={D1};
    Float     F0, F1[]={F0}, F2[][]={F1};
    Integer   I0, I1[]={I0}, I2[][]={I1};
    Long      L0, L1[]={L0}, L2[][]={L1};
    String    S0, S1[]={S0}, S2[][]={S1};
    Object    O0, O1[]={O0}, O2[][]={O1};

    static    Long LS0, LS1[]={LS0}, LS2[][]={LS1};
    private   Long LP0, LP1[]={LP0}, LP2[][]={LP1};
    public    Long LU0, LU1[]={LU0}, LU2[][]={LU1};
    protected Long LR0, LR1[]={LR0}, LR2[][]={LR1};
    transient Long LT0, LT1[]={LT0}, LT2[][]={LT1};
    volatile  Long LV0, LV1[]={LV0}, LV2[][]={LV1};
    final     Long LF0 = Long.valueOf(999), LF1[]={LF0}, LF2[][]={LF1};

    interface Inter {}
    Inter E0, E1[]={E0}, E2[][]={E1};
    static    Inter ES0, ES1[]={ES0}, ES2[][]={ES1};
    private   Inter EP0, EP1[]={EP0}, EP2[][]={EP1};
    public    Inter EU0, EU1[]={EU0}, EU2[][]={EU1};
    protected Inter ER0, ER1[]={ER0}, ER2[][]={ER1};
    transient Inter ET0, ET1[]={ET0}, ET2[][]={ET1};
    volatile  Inter EV0, EV1[]={EV0}, EV2[][]={EV1};
    final     Inter EF0 = null, EF1[]={EF0}, EF2[][]={EF1};
}
