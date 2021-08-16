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


package nsk.jdi.TypeComponent.name;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

public class name002 {
    final static String METHOD_NAME[] = {
        "Mv",
        "Mz", "Mz1", "Mz2",
        "Mb", "Mb1", "Mb2",
        "Mc", "Mc1", "Mc2",
        "Md", "Md1", "Md2",
        "Mf", "Mf1", "Mf2",
        "Mi", "Mi1", "Mi2",
        "Ml", "Ml1", "Ml2",
        "Mr", "Mr1", "Mr2",

        "MvF", "MlF", "MlF1", "MlF2",
        "MvN", "MlN", "MlN1", "MlN2",
        "MvS", "MlS", "MlS1", "MlS2",
        "MvI", "MlI", "MlI1", "MlI2",
        "MvY", "MlY", "MlY1", "MlY2",
        "MvU", "MlU", "MlU1", "MlU2",
        "MvR", "MlR", "MlR1", "MlR2",
        "MvP", "MlP", "MlP1", "MlP2",

        "MX", "MX1", "MX2",
        "MO", "MO1", "MO2",

        "MLF", "MLF1", "MLF2",
        "MLN", "MLN1", "MLN2",
        "MLS", "MLS1", "MLS2",
        "MLI", "MLI1", "MLI2",
        "MLY", "MLY1", "MLY2",
        "MLU", "MLU1", "MLU2",
        "MLR", "MLR1", "MLR2",
        "MLP", "MLP1", "MLP2",

        "ME", "ME1", "ME2",
        "MEF", "MEF1", "MEF2",
        "MEN", "MEN1", "MEN2",
        "MES", "ME1S", "ME2S",
        "MEI", "MEI1", "MEI2",
        "MEY", "MEY1", "MEY2",
        "MEU", "MEU1", "MEU2",
        "MER", "MER1", "MER2",
        "MEP", "MEP1", "MEP2"
    };

    private static Log log;
    private final static String prefix = "nsk.jdi.TypeComponent.name.";
    private final static String className = "name002";
    private final static String debugerName = prefix + className;
    private final static String debugeeName = debugerName + "a";
    private final static String classToCheckName = prefix + "name002aClassToCheck";

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

        // Check all methods from debugee
        for (int i = 0; i < METHOD_NAME.length; i++) {
            Method method;
            List listOfMethods;
            int totalMethodsByName;
            String name;

            try {
                listOfMethods = refType.methodsByName(METHOD_NAME[i]);
            } catch (Exception e) {
                log.complain("debuger FAILURE 1> Can't get method by name "
                           + METHOD_NAME[i]);
                log.complain("debuger FAILURE 1> Exception: " + e);
                testFailed = true;
                continue;
            }
            totalMethodsByName = listOfMethods.size();
            if (totalMethodsByName != 1) {
                log.complain("debuger FAILURE 2> Number of methods by name "
                           + METHOD_NAME[i] + " is " + totalMethodsByName
                           + ", should be 1.");
                testFailed = true;
                continue;
            }
            method = (Method)listOfMethods.get(0);
            name = method.name();
            if (name == null) {
                log.complain("debuger FAILURE 3> Name is null for " + i
                           + " method (" + METHOD_NAME[i] + ")");
                testFailed = true;
                continue;
            }
            log.display("debuger> " + i + " name of method (" + METHOD_NAME[i]
                      + ") " + name + " read.");
            if (!name.equals(METHOD_NAME[i])) {
                log.complain("debuger FAILURE 4> Returned name for method ("
                           + METHOD_NAME[i] + ") is " + name);
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
