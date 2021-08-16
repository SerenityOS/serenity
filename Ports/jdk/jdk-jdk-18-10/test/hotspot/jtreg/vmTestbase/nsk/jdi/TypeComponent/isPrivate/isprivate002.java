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


package nsk.jdi.TypeComponent.isPrivate;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.io.*;
import java.util.*;

public class isprivate002 {

    private static Log log;
    private final static String prefix = "nsk.jdi.TypeComponent.isPrivate.";
    private final static String debuggerName = prefix + "isprivate002";
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


        display("Checking isPrivate() method for debuggee's methods...");

        display("Total count of methods read from debuggee: "
                  + debuggeeClass.allFields().size() + ", expected count : "
                  + checkedMethods.length+1);

        // Check all methods from debuggee
        for (int i = 0; i < checkedMethods.length-1; i++) {
            List listOfMethods;
            int totalMethodsByName;
            Method method;
            String name;
            boolean isPrivate;
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
            isPrivate = ((TypeComponent)method).isPrivate();  // cast to TypeComponent interface
            expectedValue = checkedMethods[i][1];
            if ((isPrivate && !expectedValue.equals(IS_PRIVATE)) ||
                (!isPrivate && expectedValue.equals(IS_PRIVATE)) ) {
                complain("isPrivate() returned wrong value: " + isPrivate
                    + " for method " + name
                    + "; expected value : " + expectedValue);
                exitStatus = Consts.TEST_FAILED;
            } else {
                display("isPrivate() returned expected " + isPrivate
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

    final static String IS_PRIVATE = "true";
    final static String NOT_PRIVATE = "false";

    /** debuggee's methods for check **/
    private final static String checkedMethods[][] = {
        {"Mv", NOT_PRIVATE},
        {"Mz", NOT_PRIVATE},
        {"Mz1", NOT_PRIVATE},
        {"Mz2", NOT_PRIVATE},
        {"Mb", NOT_PRIVATE},
        {"Mb1", NOT_PRIVATE},
        {"Mb2", NOT_PRIVATE},
        {"Mc", NOT_PRIVATE},
        {"Mc1", NOT_PRIVATE},
        {"Mc2", NOT_PRIVATE},
        {"Md", NOT_PRIVATE},
        {"Md1", NOT_PRIVATE},
        {"Md2", NOT_PRIVATE},
        {"Mf1", NOT_PRIVATE},
        {"Mf2", NOT_PRIVATE},
        {"Mi", NOT_PRIVATE},
        {"Mi1", NOT_PRIVATE},
        {"Mi2", NOT_PRIVATE},
        {"Ml", NOT_PRIVATE},
        {"Ml1", NOT_PRIVATE},
        {"Ml2", NOT_PRIVATE},
        {"Mr", NOT_PRIVATE},
        {"Mr1", NOT_PRIVATE},
        {"Mr2", NOT_PRIVATE},

        {"MvM", IS_PRIVATE},
        {"MzM", IS_PRIVATE},
        {"Mz1M", IS_PRIVATE},
        {"Mz2M", IS_PRIVATE},
        {"MbM", IS_PRIVATE},
        {"Mb1M", IS_PRIVATE},
        {"Mb2M", IS_PRIVATE},
        {"McM", IS_PRIVATE},
        {"Mc1M", IS_PRIVATE},
        {"Mc2M", IS_PRIVATE},
        {"MdM", IS_PRIVATE},
        {"Md1M", IS_PRIVATE},
        {"Md2M", IS_PRIVATE},
        {"MfM", IS_PRIVATE},
        {"Mf1M", IS_PRIVATE},
        {"Mf2M", IS_PRIVATE},
        {"MiM", IS_PRIVATE},
        {"Mi1M", IS_PRIVATE},
        {"Mi2M", IS_PRIVATE},
        {"MlM", IS_PRIVATE},
        {"Ml1M", IS_PRIVATE},
        {"Ml2M", IS_PRIVATE},
        {"MrM", IS_PRIVATE},
        {"Mr1M", IS_PRIVATE},
        {"Mr2M", IS_PRIVATE},

        {"MvS", NOT_PRIVATE},
        {"MlS", NOT_PRIVATE},
        {"MlS1", NOT_PRIVATE},
        {"MlS2", NOT_PRIVATE},
        {"MvN", NOT_PRIVATE},
        {"MlN", NOT_PRIVATE},
        {"MlN1", NOT_PRIVATE},
        {"MlN2", NOT_PRIVATE},
        {"MvI", NOT_PRIVATE},
        {"MlI", NOT_PRIVATE},
        {"MlI1", NOT_PRIVATE},
        {"MlI2", NOT_PRIVATE},
        {"MvY", NOT_PRIVATE},
        {"MlY", NOT_PRIVATE},
        {"MlY1", NOT_PRIVATE},
        {"MlY2", NOT_PRIVATE},
        {"MvU", NOT_PRIVATE},
        {"MlU", NOT_PRIVATE},
        {"MlU1", NOT_PRIVATE},
        {"MlU2", NOT_PRIVATE},
        {"MvR", NOT_PRIVATE},
        {"MlR", NOT_PRIVATE},
        {"MlR1", NOT_PRIVATE},
        {"MlR2", NOT_PRIVATE},
        {"MvP", IS_PRIVATE},
        {"MlP", IS_PRIVATE},
        {"MlP1", IS_PRIVATE},
        {"MlP2", IS_PRIVATE},

        {"MvSM", IS_PRIVATE},
        {"MlSM", IS_PRIVATE},
        {"MlS1M", IS_PRIVATE},
        {"MlS2M", IS_PRIVATE},
        {"MvNM", IS_PRIVATE},
        {"MlNM", IS_PRIVATE},
        {"MlN1M", IS_PRIVATE},
        {"MlN2M", IS_PRIVATE},
        {"MvIM", IS_PRIVATE},
        {"MlIM", IS_PRIVATE},
        {"MlI1M", IS_PRIVATE},
        {"MlI2M", IS_PRIVATE},
        {"MvYM", IS_PRIVATE},
        {"MlYM", IS_PRIVATE},
        {"MlY1M", IS_PRIVATE},
        {"MlY2M", IS_PRIVATE},
        {"MvPM", IS_PRIVATE},
        {"MlPM", IS_PRIVATE},
        {"MlP1M", IS_PRIVATE},
        {"MlP2M", IS_PRIVATE},

        {"MX", NOT_PRIVATE},
        {"MX1", NOT_PRIVATE},
        {"MX2", NOT_PRIVATE},
        {"MO", NOT_PRIVATE},
        {"MO1", NOT_PRIVATE},
        {"MO2", NOT_PRIVATE},

        {"MXM", IS_PRIVATE},
        {"MX1M", IS_PRIVATE},
        {"MX2M", IS_PRIVATE},
        {"MOM", IS_PRIVATE},
        {"MO1M", IS_PRIVATE},
        {"MO2M", IS_PRIVATE},

        {"MLS", NOT_PRIVATE},
        {"MLS1", NOT_PRIVATE},
        {"MLS2", NOT_PRIVATE},
        {"MLN", NOT_PRIVATE},
        {"MLN1", NOT_PRIVATE},
        {"MLN2", NOT_PRIVATE},
        {"MLI", NOT_PRIVATE},
        {"MLI1", NOT_PRIVATE},
        {"MLI2", NOT_PRIVATE},
        {"MLY", NOT_PRIVATE},
        {"MLY1", NOT_PRIVATE},
        {"MLY2", NOT_PRIVATE},
        {"MLU", NOT_PRIVATE},
        {"MLU1", NOT_PRIVATE},
        {"MLU2", NOT_PRIVATE},
        {"MLR", NOT_PRIVATE},
        {"MLR1", NOT_PRIVATE},
        {"MLR2", NOT_PRIVATE},
        {"MLP", IS_PRIVATE},
        {"MLP1", IS_PRIVATE},
        {"MLP2", IS_PRIVATE},

        {"MLSM", IS_PRIVATE},
        {"MLS1M", IS_PRIVATE},
        {"MLS2M", IS_PRIVATE},
        {"MLNM", IS_PRIVATE},
        {"MLN1M", IS_PRIVATE},
        {"MLN2M", IS_PRIVATE},
        {"MLIM", IS_PRIVATE},
        {"MLI1M", IS_PRIVATE},
        {"MLI2M", IS_PRIVATE},
        {"MLYM", IS_PRIVATE},
        {"MLY1M", IS_PRIVATE},
        {"MLY2M", IS_PRIVATE},
        {"MLPM", IS_PRIVATE},
        {"MLP1M", IS_PRIVATE},
        {"MLP2M", IS_PRIVATE},

        {"ME", NOT_PRIVATE},
        {"ME1", NOT_PRIVATE},
        {"ME2", NOT_PRIVATE},
        {"MEM", IS_PRIVATE},
        {"ME1M", IS_PRIVATE},
        {"ME2M", IS_PRIVATE},

        {"MES", NOT_PRIVATE},
        {"MES1", NOT_PRIVATE},
        {"MES2", NOT_PRIVATE},
        {"MEN", NOT_PRIVATE},
        {"MEN1", NOT_PRIVATE},
        {"MEN2", NOT_PRIVATE},
        {"MEI", NOT_PRIVATE},
        {"MEI1", NOT_PRIVATE},
        {"MEI2", NOT_PRIVATE},
        {"MEY", NOT_PRIVATE},
        {"MEY1", NOT_PRIVATE},
        {"MEY2", NOT_PRIVATE},
        {"MEU", NOT_PRIVATE},
        {"MEU1", NOT_PRIVATE},
        {"MEU2", NOT_PRIVATE},
        {"MER", NOT_PRIVATE},
        {"MER1", NOT_PRIVATE},
        {"MER2", NOT_PRIVATE},
        {"MEP", IS_PRIVATE},
        {"MEP1", IS_PRIVATE},
        {"MEP2", IS_PRIVATE},

        {"MESM", IS_PRIVATE},
        {"MES1M", IS_PRIVATE},
        {"MES2M", IS_PRIVATE},
        {"MENM", IS_PRIVATE},
        {"MEN1M", IS_PRIVATE},
        {"MEN2M", IS_PRIVATE},
        {"MEIM", IS_PRIVATE},
        {"MEI1M", IS_PRIVATE},
        {"MEI2M", IS_PRIVATE},
        {"MEYM", IS_PRIVATE},
        {"MEY1M", IS_PRIVATE},
        {"MEY2M", IS_PRIVATE},
        {"MEPM", IS_PRIVATE},
        {"MEP1M", IS_PRIVATE},
        {"MEP2M", IS_PRIVATE}
    };
}
