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


package nsk.jdi.TypeComponent.isProtected;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.io.*;

public class isprotected001 {

    private static Log log;
    private final static String prefix = "nsk.jdi.TypeComponent.isProtected.";
    private final static String debuggerName = prefix + "isprotected001";
    private final static String debuggeeName = debuggerName + "a";

    private static ReferenceType debuggeeClass;

    final static String IS_PROTECTED = "true";
    final static String NOT_PROTECTED = "false";
    final static int TOTAL_FIELDS = 132;

    /** debuggee's fields for check **/
    private final static String checkedFields[][] = {
        {"z0", NOT_PROTECTED}, {"z1", NOT_PROTECTED}, {"z2", NOT_PROTECTED},
        {"b0", NOT_PROTECTED}, {"b1", NOT_PROTECTED}, {"b2", NOT_PROTECTED},
        {"c0", NOT_PROTECTED}, {"c1", NOT_PROTECTED}, {"c2", NOT_PROTECTED},
        {"d0", NOT_PROTECTED}, {"d1", NOT_PROTECTED}, {"d2", NOT_PROTECTED},
        {"f0", NOT_PROTECTED}, {"f1", NOT_PROTECTED}, {"f2", NOT_PROTECTED},
        {"i0", NOT_PROTECTED}, {"i1", NOT_PROTECTED}, {"i2", NOT_PROTECTED},
        {"l0", NOT_PROTECTED}, {"l1", NOT_PROTECTED}, {"l2", NOT_PROTECTED},
        {"r0", NOT_PROTECTED}, {"r1", NOT_PROTECTED}, {"r2", NOT_PROTECTED},

        {"zP0", IS_PROTECTED}, {"zP1", IS_PROTECTED}, {"zP2", IS_PROTECTED},
        {"bP0", IS_PROTECTED}, {"bP1", IS_PROTECTED}, {"bP2", IS_PROTECTED},
        {"cP0", IS_PROTECTED}, {"cP1", IS_PROTECTED}, {"cP2", IS_PROTECTED},
        {"dP0", IS_PROTECTED}, {"dP1", IS_PROTECTED}, {"dP2", IS_PROTECTED},
        {"fP0", IS_PROTECTED}, {"fP1", IS_PROTECTED}, {"fP2", IS_PROTECTED},
        {"iP0", IS_PROTECTED}, {"iP1", IS_PROTECTED}, {"iP2", IS_PROTECTED},
        {"lP0", IS_PROTECTED}, {"lP1", IS_PROTECTED}, {"lP2", IS_PROTECTED},
        {"rP0", IS_PROTECTED}, {"rP1", IS_PROTECTED}, {"rP2", IS_PROTECTED},

        {"Z0", NOT_PROTECTED}, {"Z1", NOT_PROTECTED}, {"Z2", NOT_PROTECTED},
        {"B0", NOT_PROTECTED}, {"B1", NOT_PROTECTED}, {"B2", NOT_PROTECTED},
        {"C0", NOT_PROTECTED}, {"C1", NOT_PROTECTED}, {"C2", NOT_PROTECTED},
        {"D0", NOT_PROTECTED}, {"D1", NOT_PROTECTED}, {"D2", NOT_PROTECTED},
        {"F0", NOT_PROTECTED}, {"F1", NOT_PROTECTED}, {"F2", NOT_PROTECTED},
        {"I0", NOT_PROTECTED}, {"I1", NOT_PROTECTED}, {"I2", NOT_PROTECTED},
        {"L0", NOT_PROTECTED}, {"L1", NOT_PROTECTED}, {"L2", NOT_PROTECTED},
        {"R0", NOT_PROTECTED}, {"R1", NOT_PROTECTED}, {"R2", NOT_PROTECTED},

        {"ZP0", IS_PROTECTED}, {"ZP1", IS_PROTECTED}, {"ZP2", IS_PROTECTED},
        {"BP0", IS_PROTECTED}, {"BP1", IS_PROTECTED}, {"BP2", IS_PROTECTED},
        {"CP0", IS_PROTECTED}, {"CP1", IS_PROTECTED}, {"CP2", IS_PROTECTED},
        {"DP0", IS_PROTECTED}, {"DP1", IS_PROTECTED}, {"DP2", IS_PROTECTED},
        {"FP0", IS_PROTECTED}, {"FP1", IS_PROTECTED}, {"FP2", IS_PROTECTED},
        {"IP0", IS_PROTECTED}, {"IP1", IS_PROTECTED}, {"IP2", IS_PROTECTED},
        {"LP0", IS_PROTECTED}, {"LP1", IS_PROTECTED}, {"LP2", IS_PROTECTED},
        {"RP0", IS_PROTECTED}, {"RP1", IS_PROTECTED}, {"RP2", IS_PROTECTED},

        {"s0", NOT_PROTECTED}, {"s1", NOT_PROTECTED}, {"s2", NOT_PROTECTED},
        {"o0", NOT_PROTECTED}, {"o1", NOT_PROTECTED}, {"o2", NOT_PROTECTED},
        {"S0", IS_PROTECTED},  {"S1", IS_PROTECTED},  {"S2", IS_PROTECTED},
        {"O0", IS_PROTECTED},  {"O1", IS_PROTECTED},  {"O2", IS_PROTECTED},

        {"u0", NOT_PROTECTED}, {"u1", NOT_PROTECTED}, {"u2", NOT_PROTECTED},
        {"v0", IS_PROTECTED},  {"v1", IS_PROTECTED},  {"v2", IS_PROTECTED},
        {"w0", NOT_PROTECTED}, {"w1", NOT_PROTECTED}, {"w2", NOT_PROTECTED},
        {"p0", NOT_PROTECTED}, {"p1", NOT_PROTECTED}, {"p2", NOT_PROTECTED},

        {"h0", NOT_PROTECTED}, {"h1", NOT_PROTECTED}, {"h2", NOT_PROTECTED},
        {"j0", IS_PROTECTED},  {"j1", IS_PROTECTED},  {"j2", IS_PROTECTED},
        {"k0", NOT_PROTECTED}, {"k1", NOT_PROTECTED}, {"k2", NOT_PROTECTED},
        {"m0", NOT_PROTECTED}, {"m1", NOT_PROTECTED}, {"m2", NOT_PROTECTED}
    };

    /**
     * Re-call to <code>run(args,out)</code>, and exit with
     * either status 95 or 97 (JCK-like exit status).
     */
    public static void main (String argv[]) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    /**
     * JCK-like entry point to the test: perform testing, and
     * return exit code 0 (Consts.TEST_PASSED) or either 2 (Consts.TEST_FAILED).
     */
    public static int run (String argv[], PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        Binder binder = new Binder(argHandler, log);
        Debugee debuggee = binder.bindToDebugee(debuggeeName
                              + (argHandler.verbose() ? " -verbose" : ""));
        IOPipe pipe = debuggee.createIOPipe();
        debuggee.redirectStderr(log, "debugger > ");

        debuggee.resume();
        display("Waiting debuggee's \"ready\" signal...");
        String line = pipe.readln();

        if (line == null) {
            complain("UNEXPECTED debuggee's signal - null");
            return Consts.TEST_FAILED;
        }
        if (!line.equals("ready")) {
            complain("UNEXPECTED debuggee's signal - " + line);
            return Consts.TEST_FAILED;
        } else {
            display("debuggee's \"ready\" signal recieved.");
        }

        int exitStatus = Consts.TEST_PASSED;

        debuggeeClass = debuggee.classByName(debuggeeName);
        if ( debuggeeClass == null ) {
            complain("Class '" + debuggeeName + "' not found.");
            return Consts.TEST_FAILED;
        }


        display("Checking isProtected() method for debuggee's fields...");

        display("Total count of fields read from debuggee: "
                  + debuggeeClass.allFields().size() + ", expected count : "
                  + TOTAL_FIELDS);

        // Check all fields from debuggee
        for (int i = 0; i < TOTAL_FIELDS; i++) {
            Field field;
            String name;
            boolean isProtected;
            String expectedValue;

            try {
                field = debuggeeClass.fieldByName(checkedFields[i][0]);
            } catch (Exception e) {
                complain("Can't get field by name "  + checkedFields[i][0]);
                complain("Unexpected Exception: " + e);
                exitStatus = Consts.TEST_FAILED;
                continue;
            }

            name = field.name();
            isProtected = ((TypeComponent)field).isProtected();  // cast to TypeComponent interface
            expectedValue = checkedFields[i][1];
            if ((isProtected && !expectedValue.equals(IS_PROTECTED)) ||
                (!isProtected && expectedValue.equals(IS_PROTECTED)) ) {
                complain("isProtected() returned wrong value: " + isProtected
                    + " for field " + name
                    + "; expected value : " + expectedValue);
                exitStatus = Consts.TEST_FAILED;
            } else {
                display("isProtected() returned expected " + isProtected
                    + " for field " + name);
            }
        }

        display("Checking debuggee's fields completed!");
        display("Waiting for debuggee's finish...");
        pipe.println("quit");
        debuggee.waitFor();

        int status = debuggee.getStatus();
        if (status != Consts.TEST_PASSED + Consts.JCK_STATUS_BASE) {
            complain("UNEXPECTED debuggee's exit status (not 95) - " + status);
            exitStatus = Consts.TEST_FAILED;
        }  else {
            display("Got expected debuggee's exit status - " + status);
        }

        return exitStatus;
    }

    private static void display(String msg) {
        log.display("debugger > " + msg);
    }

    private static void complain(String msg) {
        log.complain("debugger FAILURE > " + msg);
    }
}
