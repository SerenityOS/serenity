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


package nsk.jdi.TypeComponent.isPublic;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.io.*;
import java.util.*;

public class ispublic002 {

    private static Log log;
    private final static String prefix = "nsk.jdi.TypeComponent.isPublic.";
    private final static String debuggerName = prefix + "ispublic002";
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


        display("Checking isPublic() method for debuggee's methods...");

        display("Total count of methods read from debuggee: "
                  + debuggeeClass.allFields().size() + ", expected count : "
                  + checkedMethods.length+1);

        // Check all methods from debuggee
        for (int i = 0; i < checkedMethods.length-1; i++) {
            List listOfMethods;
            int totalMethodsByName;
            Method method;
            String name;
            boolean isPublic;
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
            isPublic = ((TypeComponent)method).isPublic();  // cast to TypeComponent interface
            expectedValue = checkedMethods[i][1];
            if ((isPublic && !expectedValue.equals(IS_PUBLIC)) ||
                (!isPublic && expectedValue.equals(IS_PUBLIC)) ) {
                complain("isPublic() returned wrong value: " + isPublic
                    + " for method " + name
                    + "; expected value : " + expectedValue);
                exitStatus = Consts.TEST_FAILED;
            } else {
                display("isPublic() returned expected " + isPublic
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

    final static String IS_PUBLIC = "true";
    final static String NOT_PUBLIC = "false";

    /** debuggee's methods for check **/
    private final static String checkedMethods[][] = {
        {"Mv", NOT_PUBLIC},
        {"Mz", NOT_PUBLIC},
        {"Mz1", NOT_PUBLIC},
        {"Mz2", NOT_PUBLIC},
        {"Mb", NOT_PUBLIC},
        {"Mb1", NOT_PUBLIC},
        {"Mb2", NOT_PUBLIC},
        {"Mc", NOT_PUBLIC},
        {"Mc1", NOT_PUBLIC},
        {"Mc2", NOT_PUBLIC},
        {"Md", NOT_PUBLIC},
        {"Md1", NOT_PUBLIC},
        {"Md2", NOT_PUBLIC},
        {"Mf1", NOT_PUBLIC},
        {"Mf2", NOT_PUBLIC},
        {"Mi", NOT_PUBLIC},
        {"Mi1", NOT_PUBLIC},
        {"Mi2", NOT_PUBLIC},
        {"Ml", NOT_PUBLIC},
        {"Ml1", NOT_PUBLIC},
        {"Ml2", NOT_PUBLIC},
        {"Mr", NOT_PUBLIC},
        {"Mr1", NOT_PUBLIC},
        {"Mr2", NOT_PUBLIC},

        {"MvM", IS_PUBLIC},
        {"MzM", IS_PUBLIC},
        {"Mz1M", IS_PUBLIC},
        {"Mz2M", IS_PUBLIC},
        {"MbM", IS_PUBLIC},
        {"Mb1M", IS_PUBLIC},
        {"Mb2M", IS_PUBLIC},
        {"McM", IS_PUBLIC},
        {"Mc1M", IS_PUBLIC},
        {"Mc2M", IS_PUBLIC},
        {"MdM", IS_PUBLIC},
        {"Md1M", IS_PUBLIC},
        {"Md2M", IS_PUBLIC},
        {"MfM", IS_PUBLIC},
        {"Mf1M", IS_PUBLIC},
        {"Mf2M", IS_PUBLIC},
        {"MiM", IS_PUBLIC},
        {"Mi1M", IS_PUBLIC},
        {"Mi2M", IS_PUBLIC},
        {"MlM", IS_PUBLIC},
        {"Ml1M", IS_PUBLIC},
        {"Ml2M", IS_PUBLIC},
        {"MrM", IS_PUBLIC},
        {"Mr1M", IS_PUBLIC},
        {"Mr2M", IS_PUBLIC},

        {"MvS", NOT_PUBLIC},
        {"MlS", NOT_PUBLIC},
        {"MlS1", NOT_PUBLIC},
        {"MlS2", NOT_PUBLIC},
        {"MvN", NOT_PUBLIC},
        {"MlN", NOT_PUBLIC},
        {"MlN1", NOT_PUBLIC},
        {"MlN2", NOT_PUBLIC},
        {"MvI", NOT_PUBLIC},
        {"MlI", NOT_PUBLIC},
        {"MlI1", NOT_PUBLIC},
        {"MlI2", NOT_PUBLIC},
        {"MvY", NOT_PUBLIC},
        {"MlY", NOT_PUBLIC},
        {"MlY1", NOT_PUBLIC},
        {"MlY2", NOT_PUBLIC},
        {"MvU", IS_PUBLIC},
        {"MlU", IS_PUBLIC},
        {"MlU1", IS_PUBLIC},
        {"MlU2", IS_PUBLIC},
        {"MvR", NOT_PUBLIC},
        {"MlR", NOT_PUBLIC},
        {"MlR1", NOT_PUBLIC},
        {"MlR2", NOT_PUBLIC},
        {"MvP", NOT_PUBLIC},
        {"MlP", NOT_PUBLIC},
        {"MlP1", NOT_PUBLIC},
        {"MlP2", NOT_PUBLIC},

        {"MvSM", IS_PUBLIC},
        {"MlSM", IS_PUBLIC},
        {"MlS1M", IS_PUBLIC},
        {"MlS2M", IS_PUBLIC},
        {"MvNM", IS_PUBLIC},
        {"MlNM", IS_PUBLIC},
        {"MlN1M", IS_PUBLIC},
        {"MlN2M", IS_PUBLIC},
        {"MvIM", IS_PUBLIC},
        {"MlIM", IS_PUBLIC},
        {"MlI1M", IS_PUBLIC},
        {"MlI2M", IS_PUBLIC},
        {"MvYM", IS_PUBLIC},
        {"MlYM", IS_PUBLIC},
        {"MlY1M", IS_PUBLIC},
        {"MlY2M", IS_PUBLIC},
        {"MvPM", IS_PUBLIC},
        {"MlPM", IS_PUBLIC},
        {"MlP1M", IS_PUBLIC},
        {"MlP2M", IS_PUBLIC},

        {"MX", NOT_PUBLIC},
        {"MX1", NOT_PUBLIC},
        {"MX2", NOT_PUBLIC},
        {"MO", NOT_PUBLIC},
        {"MO1", NOT_PUBLIC},
        {"MO2", NOT_PUBLIC},

        {"MXM", IS_PUBLIC},
        {"MX1M", IS_PUBLIC},
        {"MX2M", IS_PUBLIC},
        {"MOM", IS_PUBLIC},
        {"MO1M", IS_PUBLIC},
        {"MO2M", IS_PUBLIC},

        {"MLS", NOT_PUBLIC},
        {"MLS1", NOT_PUBLIC},
        {"MLS2", NOT_PUBLIC},
        {"MLN", NOT_PUBLIC},
        {"MLN1", NOT_PUBLIC},
        {"MLN2", NOT_PUBLIC},
        {"MLI", NOT_PUBLIC},
        {"MLI1", NOT_PUBLIC},
        {"MLI2", NOT_PUBLIC},
        {"MLY", NOT_PUBLIC},
        {"MLY1", NOT_PUBLIC},
        {"MLY2", NOT_PUBLIC},
        {"MLU", IS_PUBLIC},
        {"MLU1", IS_PUBLIC},
        {"MLU2", IS_PUBLIC},
        {"MLR", NOT_PUBLIC},
        {"MLR1", NOT_PUBLIC},
        {"MLR2", NOT_PUBLIC},
        {"MLP", NOT_PUBLIC},
        {"MLP1", NOT_PUBLIC},
        {"MLP2", NOT_PUBLIC},

        {"MLSM", IS_PUBLIC},
        {"MLS1M", IS_PUBLIC},
        {"MLS2M", IS_PUBLIC},
        {"MLNM", IS_PUBLIC},
        {"MLN1M", IS_PUBLIC},
        {"MLN2M", IS_PUBLIC},
        {"MLIM", IS_PUBLIC},
        {"MLI1M", IS_PUBLIC},
        {"MLI2M", IS_PUBLIC},
        {"MLYM", IS_PUBLIC},
        {"MLY1M", IS_PUBLIC},
        {"MLY2M", IS_PUBLIC},
        {"MLPM", IS_PUBLIC},
        {"MLP1M", IS_PUBLIC},
        {"MLP2M", IS_PUBLIC},

        {"ME", NOT_PUBLIC},
        {"ME1", NOT_PUBLIC},
        {"ME2", NOT_PUBLIC},
        {"MEM", IS_PUBLIC},
        {"ME1M", IS_PUBLIC},
        {"ME2M", IS_PUBLIC},

        {"MES", NOT_PUBLIC},
        {"MES1", NOT_PUBLIC},
        {"MES2", NOT_PUBLIC},
        {"MEN", NOT_PUBLIC},
        {"MEN1", NOT_PUBLIC},
        {"MEN2", NOT_PUBLIC},
        {"MEI", NOT_PUBLIC},
        {"MEI1", NOT_PUBLIC},
        {"MEI2", NOT_PUBLIC},
        {"MEY", NOT_PUBLIC},
        {"MEY1", NOT_PUBLIC},
        {"MEY2", NOT_PUBLIC},
        {"MEU", IS_PUBLIC},
        {"MEU1", IS_PUBLIC},
        {"MEU2", IS_PUBLIC},
        {"MER", NOT_PUBLIC},
        {"MER1", NOT_PUBLIC},
        {"MER2", NOT_PUBLIC},
        {"MEP", NOT_PUBLIC},
        {"MEP1", NOT_PUBLIC},
        {"MEP2", NOT_PUBLIC},

        {"MESM", IS_PUBLIC},
        {"MES1M", IS_PUBLIC},
        {"MES2M", IS_PUBLIC},
        {"MENM", IS_PUBLIC},
        {"MEN1M", IS_PUBLIC},
        {"MEN2M", IS_PUBLIC},
        {"MEIM", IS_PUBLIC},
        {"MEI1M", IS_PUBLIC},
        {"MEI2M", IS_PUBLIC},
        {"MEYM", IS_PUBLIC},
        {"MEY1M", IS_PUBLIC},
        {"MEY2M", IS_PUBLIC},
        {"MEPM", IS_PUBLIC},
        {"MEP1M", IS_PUBLIC},
        {"MEP2M", IS_PUBLIC}
    };
}
