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


package nsk.jdi.TypeComponent.isPackagePrivate;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.io.*;

public class ispackageprivate001 {

    private static Log log;
    private final static String prefix = "nsk.jdi.TypeComponent.isPackagePrivate.";
    private final static String debuggerName = prefix + "ispackageprivate001";
    private final static String debuggeeName = debuggerName + "a";

    private static ReferenceType debuggeeClass;

    final static String IS_PPRIVATE = "true";
    final static String NOT_PPRIVATE = "false";
    final static int TOTAL_FIELDS = 132;

    /** debuggee's fields for check **/
    private final static String checkedFields[][] = {
        {"z0", IS_PPRIVATE}, {"z1", IS_PPRIVATE}, {"z2", IS_PPRIVATE},
        {"b0", IS_PPRIVATE}, {"b1", IS_PPRIVATE}, {"b2", IS_PPRIVATE},
        {"c0", IS_PPRIVATE}, {"c1", IS_PPRIVATE}, {"c2", IS_PPRIVATE},
        {"d0", IS_PPRIVATE}, {"d1", IS_PPRIVATE}, {"d2", IS_PPRIVATE},
        {"f0", IS_PPRIVATE}, {"f1", IS_PPRIVATE}, {"f2", IS_PPRIVATE},
        {"i0", IS_PPRIVATE}, {"i1", IS_PPRIVATE}, {"i2", IS_PPRIVATE},
        {"l0", IS_PPRIVATE}, {"l1", IS_PPRIVATE}, {"l2", IS_PPRIVATE},
        {"r0", IS_PPRIVATE}, {"r1", IS_PPRIVATE}, {"r2", IS_PPRIVATE},

        {"zP0", NOT_PPRIVATE}, {"zP1", NOT_PPRIVATE}, {"zP2", NOT_PPRIVATE},
        {"bP0", NOT_PPRIVATE}, {"bP1", NOT_PPRIVATE}, {"bP2", NOT_PPRIVATE},
        {"cP0", NOT_PPRIVATE}, {"cP1", NOT_PPRIVATE}, {"cP2", NOT_PPRIVATE},
        {"dP0", NOT_PPRIVATE}, {"dP1", NOT_PPRIVATE}, {"dP2", NOT_PPRIVATE},
        {"fP0", NOT_PPRIVATE}, {"fP1", NOT_PPRIVATE}, {"fP2", NOT_PPRIVATE},
        {"iP0", NOT_PPRIVATE}, {"iP1", NOT_PPRIVATE}, {"iP2", NOT_PPRIVATE},
        {"lP0", NOT_PPRIVATE}, {"lP1", NOT_PPRIVATE}, {"lP2", NOT_PPRIVATE},
        {"rP0", NOT_PPRIVATE}, {"rP1", NOT_PPRIVATE}, {"rP2", NOT_PPRIVATE},

        {"Z0", IS_PPRIVATE}, {"Z1", IS_PPRIVATE}, {"Z2", IS_PPRIVATE},
        {"B0", IS_PPRIVATE}, {"B1", IS_PPRIVATE}, {"B2", IS_PPRIVATE},
        {"C0", IS_PPRIVATE}, {"C1", IS_PPRIVATE}, {"C2", IS_PPRIVATE},
        {"D0", IS_PPRIVATE}, {"D1", IS_PPRIVATE}, {"D2", IS_PPRIVATE},
        {"F0", IS_PPRIVATE}, {"F1", IS_PPRIVATE}, {"F2", IS_PPRIVATE},
        {"I0", IS_PPRIVATE}, {"I1", IS_PPRIVATE}, {"I2", IS_PPRIVATE},
        {"L0", IS_PPRIVATE}, {"L1", IS_PPRIVATE}, {"L2", IS_PPRIVATE},
        {"R0", IS_PPRIVATE}, {"R1", IS_PPRIVATE}, {"R2", IS_PPRIVATE},

        {"ZP0", NOT_PPRIVATE}, {"ZP1", NOT_PPRIVATE}, {"ZP2", NOT_PPRIVATE},
        {"BP0", NOT_PPRIVATE}, {"BP1", NOT_PPRIVATE}, {"BP2", NOT_PPRIVATE},
        {"CP0", NOT_PPRIVATE}, {"CP1", NOT_PPRIVATE}, {"CP2", NOT_PPRIVATE},
        {"DP0", NOT_PPRIVATE}, {"DP1", NOT_PPRIVATE}, {"DP2", NOT_PPRIVATE},
        {"FP0", NOT_PPRIVATE}, {"FP1", NOT_PPRIVATE}, {"FP2", NOT_PPRIVATE},
        {"IP0", NOT_PPRIVATE}, {"IP1", NOT_PPRIVATE}, {"IP2", NOT_PPRIVATE},
        {"LP0", NOT_PPRIVATE}, {"LP1", NOT_PPRIVATE}, {"LP2", NOT_PPRIVATE},
        {"RP0", NOT_PPRIVATE}, {"RP1", NOT_PPRIVATE}, {"RP2", NOT_PPRIVATE},

        {"s0", NOT_PPRIVATE}, {"s1", NOT_PPRIVATE}, {"s2", NOT_PPRIVATE},
        {"o0", NOT_PPRIVATE}, {"o1", NOT_PPRIVATE}, {"o2", NOT_PPRIVATE},
        {"S0", IS_PPRIVATE},  {"S1", IS_PPRIVATE},  {"S2", IS_PPRIVATE},
        {"O0", IS_PPRIVATE},  {"O1", IS_PPRIVATE},  {"O2", IS_PPRIVATE},

        {"u0", NOT_PPRIVATE}, {"u1", NOT_PPRIVATE}, {"u2", NOT_PPRIVATE},
        {"v0", IS_PPRIVATE},  {"v1", IS_PPRIVATE},  {"v2", IS_PPRIVATE},
        {"w0", NOT_PPRIVATE}, {"w1", NOT_PPRIVATE}, {"w2", NOT_PPRIVATE},
        {"p0", NOT_PPRIVATE}, {"p1", NOT_PPRIVATE}, {"p2", NOT_PPRIVATE},

        {"h0", NOT_PPRIVATE}, {"h1", NOT_PPRIVATE}, {"h2", NOT_PPRIVATE},
        {"j0", IS_PPRIVATE},  {"j1", IS_PPRIVATE},  {"j2", IS_PPRIVATE},
        {"k0", NOT_PPRIVATE}, {"k1", NOT_PPRIVATE}, {"k2", NOT_PPRIVATE},
        {"m0", NOT_PPRIVATE}, {"m1", NOT_PPRIVATE}, {"m2", NOT_PPRIVATE}
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


        display("Checking isPackagePrivate() method for debuggee's fields...");

        display("Total count of fields read from debuggee: "
                  + debuggeeClass.allFields().size() + ", expected count : "
                  + TOTAL_FIELDS);

        // Check all fields from debuggee
        for (int i = 0; i < TOTAL_FIELDS; i++) {
            Field field;
            String name;
            boolean isPackagePrivate;
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
            isPackagePrivate = ((TypeComponent)field).isPackagePrivate();  // cast to TypeComponent interface
            expectedValue = checkedFields[i][1];
            if ((isPackagePrivate && !expectedValue.equals(IS_PPRIVATE)) ||
                (!isPackagePrivate && expectedValue.equals(IS_PPRIVATE)) ) {
                complain("isPackagePrivate() returned wrong value: " + isPackagePrivate
                    + " for field " + name
                    + "; expected value : " + expectedValue);
                exitStatus = Consts.TEST_FAILED;
            } else {
                display("isPackagePrivate() returned expected " + isPackagePrivate
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
