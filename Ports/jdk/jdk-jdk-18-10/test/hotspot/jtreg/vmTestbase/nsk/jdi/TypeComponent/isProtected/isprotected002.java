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
import java.util.*;

public class isprotected002 {

    private static Log log;
    private final static String prefix = "nsk.jdi.TypeComponent.isProtected.";
    private final static String debuggerName = prefix + "isprotected002";
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


        display("Checking isProtected() method for debuggee's methods...");

        display("Total count of methods read from debuggee: "
                  + debuggeeClass.allFields().size() + ", expected count : "
                  + checkedMethods.length+1);

        // Check all methods from debuggee
        for (int i = 0; i < checkedMethods.length-1; i++) {
            List listOfMethods;
            int totalMethodsByName;
            Method method;
            String name;
            boolean isProtected;
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
            isProtected = ((TypeComponent)method).isProtected();  // cast to TypeComponent interface
            expectedValue = checkedMethods[i][1];
            if ((isProtected && !expectedValue.equals(IS_PROTECTED)) ||
                (!isProtected && expectedValue.equals(IS_PROTECTED)) ) {
                complain("isProtected() returned wrong value: " + isProtected
                    + " for method " + name
                    + "; expected value : " + expectedValue);
                exitStatus = Consts.TEST_FAILED;
            } else {
                display("isProtected() returned expected " + isProtected
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

    final static String IS_PROTECTED = "true";
    final static String NOT_PROTECTED = "false";

    /** debuggee's methods for check **/
    private final static String checkedMethods[][] = {
        {"Mv", NOT_PROTECTED},
        {"Mz", NOT_PROTECTED},
        {"Mz1", NOT_PROTECTED},
        {"Mz2", NOT_PROTECTED},
        {"Mb", NOT_PROTECTED},
        {"Mb1", NOT_PROTECTED},
        {"Mb2", NOT_PROTECTED},
        {"Mc", NOT_PROTECTED},
        {"Mc1", NOT_PROTECTED},
        {"Mc2", NOT_PROTECTED},
        {"Md", NOT_PROTECTED},
        {"Md1", NOT_PROTECTED},
        {"Md2", NOT_PROTECTED},
        {"Mf1", NOT_PROTECTED},
        {"Mf2", NOT_PROTECTED},
        {"Mi", NOT_PROTECTED},
        {"Mi1", NOT_PROTECTED},
        {"Mi2", NOT_PROTECTED},
        {"Ml", NOT_PROTECTED},
        {"Ml1", NOT_PROTECTED},
        {"Ml2", NOT_PROTECTED},
        {"Mr", NOT_PROTECTED},
        {"Mr1", NOT_PROTECTED},
        {"Mr2", NOT_PROTECTED},

        {"MvM", IS_PROTECTED},
        {"MzM", IS_PROTECTED},
        {"Mz1M", IS_PROTECTED},
        {"Mz2M", IS_PROTECTED},
        {"MbM", IS_PROTECTED},
        {"Mb1M", IS_PROTECTED},
        {"Mb2M", IS_PROTECTED},
        {"McM", IS_PROTECTED},
        {"Mc1M", IS_PROTECTED},
        {"Mc2M", IS_PROTECTED},
        {"MdM", IS_PROTECTED},
        {"Md1M", IS_PROTECTED},
        {"Md2M", IS_PROTECTED},
        {"MfM", IS_PROTECTED},
        {"Mf1M", IS_PROTECTED},
        {"Mf2M", IS_PROTECTED},
        {"MiM", IS_PROTECTED},
        {"Mi1M", IS_PROTECTED},
        {"Mi2M", IS_PROTECTED},
        {"MlM", IS_PROTECTED},
        {"Ml1M", IS_PROTECTED},
        {"Ml2M", IS_PROTECTED},
        {"MrM", IS_PROTECTED},
        {"Mr1M", IS_PROTECTED},
        {"Mr2M", IS_PROTECTED},

        {"MvS", NOT_PROTECTED},
        {"MlS", NOT_PROTECTED},
        {"MlS1", NOT_PROTECTED},
        {"MlS2", NOT_PROTECTED},
        {"MvN", NOT_PROTECTED},
        {"MlN", NOT_PROTECTED},
        {"MlN1", NOT_PROTECTED},
        {"MlN2", NOT_PROTECTED},
        {"MvI", NOT_PROTECTED},
        {"MlI", NOT_PROTECTED},
        {"MlI1", NOT_PROTECTED},
        {"MlI2", NOT_PROTECTED},
        {"MvY", NOT_PROTECTED},
        {"MlY", NOT_PROTECTED},
        {"MlY1", NOT_PROTECTED},
        {"MlY2", NOT_PROTECTED},
        {"MvU", NOT_PROTECTED},
        {"MlU", NOT_PROTECTED},
        {"MlU1", NOT_PROTECTED},
        {"MlU2", NOT_PROTECTED},
        {"MvR", IS_PROTECTED},
        {"MlR", IS_PROTECTED},
        {"MlR1", IS_PROTECTED},
        {"MlR2", IS_PROTECTED},
        {"MvP", NOT_PROTECTED},
        {"MlP", NOT_PROTECTED},
        {"MlP1", NOT_PROTECTED},
        {"MlP2", NOT_PROTECTED},

        {"MvSM", IS_PROTECTED},
        {"MlSM", IS_PROTECTED},
        {"MlS1M", IS_PROTECTED},
        {"MlS2M", IS_PROTECTED},
        {"MvNM", IS_PROTECTED},
        {"MlNM", IS_PROTECTED},
        {"MlN1M", IS_PROTECTED},
        {"MlN2M", IS_PROTECTED},
        {"MvIM", IS_PROTECTED},
        {"MlIM", IS_PROTECTED},
        {"MlI1M", IS_PROTECTED},
        {"MlI2M", IS_PROTECTED},
        {"MvYM", IS_PROTECTED},
        {"MlYM", IS_PROTECTED},
        {"MlY1M", IS_PROTECTED},
        {"MlY2M", IS_PROTECTED},
        {"MvPM", IS_PROTECTED},
        {"MlPM", IS_PROTECTED},
        {"MlP1M", IS_PROTECTED},
        {"MlP2M", IS_PROTECTED},

        {"MX", NOT_PROTECTED},
        {"MX1", NOT_PROTECTED},
        {"MX2", NOT_PROTECTED},
        {"MO", NOT_PROTECTED},
        {"MO1", NOT_PROTECTED},
        {"MO2", NOT_PROTECTED},

        {"MXM", IS_PROTECTED},
        {"MX1M", IS_PROTECTED},
        {"MX2M", IS_PROTECTED},
        {"MOM", IS_PROTECTED},
        {"MO1M", IS_PROTECTED},
        {"MO2M", IS_PROTECTED},

        {"MLS", NOT_PROTECTED},
        {"MLS1", NOT_PROTECTED},
        {"MLS2", NOT_PROTECTED},
        {"MLN", NOT_PROTECTED},
        {"MLN1", NOT_PROTECTED},
        {"MLN2", NOT_PROTECTED},
        {"MLI", NOT_PROTECTED},
        {"MLI1", NOT_PROTECTED},
        {"MLI2", NOT_PROTECTED},
        {"MLY", NOT_PROTECTED},
        {"MLY1", NOT_PROTECTED},
        {"MLY2", NOT_PROTECTED},
        {"MLU", NOT_PROTECTED},
        {"MLU1", NOT_PROTECTED},
        {"MLU2", NOT_PROTECTED},
        {"MLR", IS_PROTECTED},
        {"MLR1", IS_PROTECTED},
        {"MLR2", IS_PROTECTED},
        {"MLP", NOT_PROTECTED},
        {"MLP1", NOT_PROTECTED},
        {"MLP2", NOT_PROTECTED},

        {"MLSM", IS_PROTECTED},
        {"MLS1M", IS_PROTECTED},
        {"MLS2M", IS_PROTECTED},
        {"MLNM", IS_PROTECTED},
        {"MLN1M", IS_PROTECTED},
        {"MLN2M", IS_PROTECTED},
        {"MLIM", IS_PROTECTED},
        {"MLI1M", IS_PROTECTED},
        {"MLI2M", IS_PROTECTED},
        {"MLYM", IS_PROTECTED},
        {"MLY1M", IS_PROTECTED},
        {"MLY2M", IS_PROTECTED},
        {"MLPM", IS_PROTECTED},
        {"MLP1M", IS_PROTECTED},
        {"MLP2M", IS_PROTECTED},

        {"ME", NOT_PROTECTED},
        {"ME1", NOT_PROTECTED},
        {"ME2", NOT_PROTECTED},
        {"MEM", IS_PROTECTED},
        {"ME1M", IS_PROTECTED},
        {"ME2M", IS_PROTECTED},

        {"MES", NOT_PROTECTED},
        {"MES1", NOT_PROTECTED},
        {"MES2", NOT_PROTECTED},
        {"MEN", NOT_PROTECTED},
        {"MEN1", NOT_PROTECTED},
        {"MEN2", NOT_PROTECTED},
        {"MEI", NOT_PROTECTED},
        {"MEI1", NOT_PROTECTED},
        {"MEI2", NOT_PROTECTED},
        {"MEY", NOT_PROTECTED},
        {"MEY1", NOT_PROTECTED},
        {"MEY2", NOT_PROTECTED},
        {"MEU", NOT_PROTECTED},
        {"MEU1", NOT_PROTECTED},
        {"MEU2", NOT_PROTECTED},
        {"MER", IS_PROTECTED},
        {"MER1", IS_PROTECTED},
        {"MER2", IS_PROTECTED},
        {"MEP", NOT_PROTECTED},
        {"MEP1", NOT_PROTECTED},
        {"MEP2", NOT_PROTECTED},

        {"MESM", IS_PROTECTED},
        {"MES1M", IS_PROTECTED},
        {"MES2M", IS_PROTECTED},
        {"MENM", IS_PROTECTED},
        {"MEN1M", IS_PROTECTED},
        {"MEN2M", IS_PROTECTED},
        {"MEIM", IS_PROTECTED},
        {"MEI1M", IS_PROTECTED},
        {"MEI2M", IS_PROTECTED},
        {"MEYM", IS_PROTECTED},
        {"MEY1M", IS_PROTECTED},
        {"MEY2M", IS_PROTECTED},
        {"MEPM", IS_PROTECTED},
        {"MEP1M", IS_PROTECTED},
        {"MEP2M", IS_PROTECTED}
    };
}
