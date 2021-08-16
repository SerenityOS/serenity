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
import java.util.*;

public class ispackageprivate002 {

    private static Log log;
    private final static String prefix = "nsk.jdi.TypeComponent.isPackagePrivate.";
    private final static String debuggerName = prefix + "ispackageprivate002";
    private final static String debuggeeName = debuggerName + "a";

    private static ReferenceType debuggeeClass;

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


        display("Checking isPackagePrivate() method for debuggee's methods...");

        display("Total count of methods read from debuggee: "
                  + debuggeeClass.allFields().size() + ", expected count : "
                  + checkedMethods.length+1);

        // Check all methods from debuggee
        for (int i = 0; i < checkedMethods.length-1; i++) {
            List listOfMethods;
            int totalMethodsByName;
            Method method;
            String name;
            boolean isPackagePrivate;
            String expectedValue;

            try {
                listOfMethods = debuggeeClass.methodsByName(checkedMethods[i][0]);
            } catch (Exception e) {
                complain("Can't get method by name "  + checkedMethods[i][0]);
                complain("Unexpected Exception: " + e);
                exitStatus = Consts.TEST_FAILED;
                continue;
            }

            totalMethodsByName = listOfMethods.size();
            if (totalMethodsByName != 1) {
                log.complain("debuger FAILURE 2> Number of methods by name "
                           + checkedMethods[i][0] + " is " + totalMethodsByName
                           + ", should be 1.");
                exitStatus = Consts.TEST_FAILED;
                continue;
            }
            method = (Method)listOfMethods.get(0);

            name = method.name();
            isPackagePrivate = ((TypeComponent)method).isPackagePrivate();  // cast to TypeComponent interface
            expectedValue = checkedMethods[i][1];
            if ((isPackagePrivate && !expectedValue.equals(IS_PPRIVATE)) ||
                (!isPackagePrivate && expectedValue.equals(IS_PPRIVATE)) ) {
                complain("isPackagePrivate() returned wrong value: " + isPackagePrivate
                    + " for method " + name
                    + "; expected value : " + expectedValue);
                exitStatus = Consts.TEST_FAILED;
            } else {
                display("isPackagePrivate() returned expected " + isPackagePrivate
                    + " for method " + name);
            }
        }

        display("Checking debuggee's methods completed!");
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

    //--------------------------------------------- test specific fields

    final static String IS_PPRIVATE = "true";
    final static String NOT_PPRIVATE = "false";

    /** debuggee's methods for check **/
    private final static String checkedMethods[][] = {
        {"Mv", IS_PPRIVATE},
        {"Mz", IS_PPRIVATE},
        {"Mz1", IS_PPRIVATE},
        {"Mz2", IS_PPRIVATE},
        {"Mb", IS_PPRIVATE},
        {"Mb1", IS_PPRIVATE},
        {"Mb2", IS_PPRIVATE},
        {"Mc", IS_PPRIVATE},
        {"Mc1", IS_PPRIVATE},
        {"Mc2", IS_PPRIVATE},
        {"Md", IS_PPRIVATE},
        {"Md1", IS_PPRIVATE},
        {"Md2", IS_PPRIVATE},
        {"Mf1", IS_PPRIVATE},
        {"Mf2", IS_PPRIVATE},
        {"Mi", IS_PPRIVATE},
        {"Mi1", IS_PPRIVATE},
        {"Mi2", IS_PPRIVATE},
        {"Ml", IS_PPRIVATE},
        {"Ml1", IS_PPRIVATE},
        {"Ml2", IS_PPRIVATE},
        {"Mr", IS_PPRIVATE},
        {"Mr1", IS_PPRIVATE},
        {"Mr2", IS_PPRIVATE},

        {"MvM", NOT_PPRIVATE},
        {"MzM", NOT_PPRIVATE},
        {"Mz1M", NOT_PPRIVATE},
        {"Mz2M", NOT_PPRIVATE},
        {"MbM", NOT_PPRIVATE},
        {"Mb1M", NOT_PPRIVATE},
        {"Mb2M", NOT_PPRIVATE},
        {"McM", NOT_PPRIVATE},
        {"Mc1M", NOT_PPRIVATE},
        {"Mc2M", NOT_PPRIVATE},
        {"MdM", NOT_PPRIVATE},
        {"Md1M", NOT_PPRIVATE},
        {"Md2M", NOT_PPRIVATE},
        {"MfM", NOT_PPRIVATE},
        {"Mf1M", NOT_PPRIVATE},
        {"Mf2M", NOT_PPRIVATE},
        {"MiM", NOT_PPRIVATE},
        {"Mi1M", NOT_PPRIVATE},
        {"Mi2M", NOT_PPRIVATE},
        {"MlM", NOT_PPRIVATE},
        {"Ml1M", NOT_PPRIVATE},
        {"Ml2M", NOT_PPRIVATE},
        {"MrM", NOT_PPRIVATE},
        {"Mr1M", NOT_PPRIVATE},
        {"Mr2M", NOT_PPRIVATE},

        {"MvS", IS_PPRIVATE},
        {"MlS", IS_PPRIVATE},
        {"MlS1", IS_PPRIVATE},
        {"MlS2", IS_PPRIVATE},
        {"MvN", IS_PPRIVATE},
        {"MlN", IS_PPRIVATE},
        {"MlN1", IS_PPRIVATE},
        {"MlN2", IS_PPRIVATE},
        {"MvI", IS_PPRIVATE},
        {"MlI", IS_PPRIVATE},
        {"MlI1", IS_PPRIVATE},
        {"MlI2", IS_PPRIVATE},
        {"MvY", IS_PPRIVATE},
        {"MlY", IS_PPRIVATE},
        {"MlY1", IS_PPRIVATE},
        {"MlY2", IS_PPRIVATE},
        {"MvU", NOT_PPRIVATE},
        {"MlU", NOT_PPRIVATE},
        {"MlU1", NOT_PPRIVATE},
        {"MlU2", NOT_PPRIVATE},
        {"MvR", NOT_PPRIVATE},
        {"MlR", NOT_PPRIVATE},
        {"MlR1", NOT_PPRIVATE},
        {"MlR2", NOT_PPRIVATE},
        {"MvP", NOT_PPRIVATE},
        {"MlP", NOT_PPRIVATE},
        {"MlP1", NOT_PPRIVATE},
        {"MlP2", NOT_PPRIVATE},

        {"MvSM", NOT_PPRIVATE},
        {"MlSM", NOT_PPRIVATE},
        {"MlS1M", NOT_PPRIVATE},
        {"MlS2M", NOT_PPRIVATE},
        {"MvNM", NOT_PPRIVATE},
        {"MlNM", NOT_PPRIVATE},
        {"MlN1M", NOT_PPRIVATE},
        {"MlN2M", NOT_PPRIVATE},
        {"MvIM", NOT_PPRIVATE},
        {"MlIM", NOT_PPRIVATE},
        {"MlI1M", NOT_PPRIVATE},
        {"MlI2M", NOT_PPRIVATE},
        {"MvYM", NOT_PPRIVATE},
        {"MlYM", NOT_PPRIVATE},
        {"MlY1M", NOT_PPRIVATE},
        {"MlY2M", NOT_PPRIVATE},
        {"MvPM", NOT_PPRIVATE},
        {"MlPM", NOT_PPRIVATE},
        {"MlP1M", NOT_PPRIVATE},
        {"MlP2M", NOT_PPRIVATE},

        {"MX", IS_PPRIVATE},
        {"MX1", IS_PPRIVATE},
        {"MX2", IS_PPRIVATE},
        {"MO", IS_PPRIVATE},
        {"MO1", IS_PPRIVATE},
        {"MO2", IS_PPRIVATE},

        {"MXM", NOT_PPRIVATE},
        {"MX1M", NOT_PPRIVATE},
        {"MX2M", NOT_PPRIVATE},
        {"MOM", NOT_PPRIVATE},
        {"MO1M", NOT_PPRIVATE},
        {"MO2M", NOT_PPRIVATE},

        {"MLS", IS_PPRIVATE},
        {"MLS1", IS_PPRIVATE},
        {"MLS2", IS_PPRIVATE},
        {"MLN", IS_PPRIVATE},
        {"MLN1", IS_PPRIVATE},
        {"MLN2", IS_PPRIVATE},
        {"MLI", IS_PPRIVATE},
        {"MLI1", IS_PPRIVATE},
        {"MLI2", IS_PPRIVATE},
        {"MLY", IS_PPRIVATE},
        {"MLY1", IS_PPRIVATE},
        {"MLY2", IS_PPRIVATE},
        {"MLU", NOT_PPRIVATE},
        {"MLU1", NOT_PPRIVATE},
        {"MLU2", NOT_PPRIVATE},
        {"MLR", NOT_PPRIVATE},
        {"MLR1", NOT_PPRIVATE},
        {"MLR2", NOT_PPRIVATE},
        {"MLP", NOT_PPRIVATE},
        {"MLP1", NOT_PPRIVATE},
        {"MLP2", NOT_PPRIVATE},

        {"MLSM", NOT_PPRIVATE},
        {"MLS1M", NOT_PPRIVATE},
        {"MLS2M", NOT_PPRIVATE},
        {"MLNM", NOT_PPRIVATE},
        {"MLN1M", NOT_PPRIVATE},
        {"MLN2M", NOT_PPRIVATE},
        {"MLIM", NOT_PPRIVATE},
        {"MLI1M", NOT_PPRIVATE},
        {"MLI2M", NOT_PPRIVATE},
        {"MLYM", NOT_PPRIVATE},
        {"MLY1M", NOT_PPRIVATE},
        {"MLY2M", NOT_PPRIVATE},
        {"MLPM", NOT_PPRIVATE},
        {"MLP1M", NOT_PPRIVATE},
        {"MLP2M", NOT_PPRIVATE},

        {"ME", IS_PPRIVATE},
        {"ME1", IS_PPRIVATE},
        {"ME2", IS_PPRIVATE},
        {"MEM", NOT_PPRIVATE},
        {"ME1M", NOT_PPRIVATE},
        {"ME2M", NOT_PPRIVATE},

        {"MES", IS_PPRIVATE},
        {"MES1", IS_PPRIVATE},
        {"MES2", IS_PPRIVATE},
        {"MEN", IS_PPRIVATE},
        {"MEN1", IS_PPRIVATE},
        {"MEN2", IS_PPRIVATE},
        {"MEI", IS_PPRIVATE},
        {"MEI1", IS_PPRIVATE},
        {"MEI2", IS_PPRIVATE},
        {"MEY", IS_PPRIVATE},
        {"MEY1", IS_PPRIVATE},
        {"MEY2", IS_PPRIVATE},
        {"MEU", NOT_PPRIVATE},
        {"MEU1", NOT_PPRIVATE},
        {"MEU2", NOT_PPRIVATE},
        {"MER", NOT_PPRIVATE},
        {"MER1", NOT_PPRIVATE},
        {"MER2", NOT_PPRIVATE},
        {"MEP", NOT_PPRIVATE},
        {"MEP1", NOT_PPRIVATE},
        {"MEP2", NOT_PPRIVATE},

        {"MESM", NOT_PPRIVATE},
        {"MES1M", NOT_PPRIVATE},
        {"MES2M", NOT_PPRIVATE},
        {"MENM", NOT_PPRIVATE},
        {"MEN1M", NOT_PPRIVATE},
        {"MEN2M", NOT_PPRIVATE},
        {"MEIM", NOT_PPRIVATE},
        {"MEI1M", NOT_PPRIVATE},
        {"MEI2M", NOT_PPRIVATE},
        {"MEYM", NOT_PPRIVATE},
        {"MEY1M", NOT_PPRIVATE},
        {"MEY2M", NOT_PPRIVATE},
        {"MEPM", NOT_PPRIVATE},
        {"MEP1M", NOT_PPRIVATE},
        {"MEP2M", NOT_PPRIVATE}
    };
}
