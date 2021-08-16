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


package nsk.jdi.TypeComponent.isFinal;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

public class isfinal002 {
    final static String IS_FINAL = "true";
    final static String NOT_FINAL = "false";
    final static int TOTAL_METHODS = 208;
    final static String METHOD_NAME[][] = {
        {"Mv", NOT_FINAL},
        {"Mz", NOT_FINAL},
        {"Mz1", NOT_FINAL},
        {"Mz2", NOT_FINAL},
        {"Mb", NOT_FINAL},
        {"Mb1", NOT_FINAL},
        {"Mb2", NOT_FINAL},
        {"Mc", NOT_FINAL},
        {"Mc1", NOT_FINAL},
        {"Mc2", NOT_FINAL},
        {"Md", NOT_FINAL},
        {"Md1", NOT_FINAL},
        {"Md2", NOT_FINAL},
        {"Mf", NOT_FINAL},
        {"Mf1", NOT_FINAL},
        {"Mf2", NOT_FINAL},
        {"Mi", NOT_FINAL},
        {"Mi1", NOT_FINAL},
        {"Mi2", NOT_FINAL},
        {"Ml", NOT_FINAL},
        {"Ml1", NOT_FINAL},
        {"Ml2", NOT_FINAL},
        {"Mr", NOT_FINAL},
        {"Mr1", NOT_FINAL},
        {"Mr2", NOT_FINAL},

        {"MvF", IS_FINAL},
        {"MzF", IS_FINAL},
        {"Mz1F", IS_FINAL},
        {"Mz2F", IS_FINAL},
        {"MbF", IS_FINAL},
        {"Mb1F", IS_FINAL},
        {"Mb2F", IS_FINAL},
        {"McF", IS_FINAL},
        {"Mc1F", IS_FINAL},
        {"Mc2F", IS_FINAL},
        {"MdF", IS_FINAL},
        {"Md1F", IS_FINAL},
        {"Md2F", IS_FINAL},
        {"MfF", IS_FINAL},
        {"Mf1F", IS_FINAL},
        {"Mf2F", IS_FINAL},
        {"MiF", IS_FINAL},
        {"Mi1F", IS_FINAL},
        {"Mi2F", IS_FINAL},
        {"MlF", IS_FINAL},
        {"Ml1F", IS_FINAL},
        {"Ml2F", IS_FINAL},
        {"MrF", IS_FINAL},
        {"Mr1F", IS_FINAL},
        {"Mr2F", IS_FINAL},

        {"MvS", NOT_FINAL},
        {"MlS", NOT_FINAL},
        {"MlS1", NOT_FINAL},
        {"MlS2", NOT_FINAL},
        {"MvN", NOT_FINAL},
        {"MlN", NOT_FINAL},
        {"MlN1", NOT_FINAL},
        {"MlN2", NOT_FINAL},
        {"MvI", NOT_FINAL},
        {"MlI", NOT_FINAL},
        {"MlI1", NOT_FINAL},
        {"MlI2", NOT_FINAL},
        {"MvY", NOT_FINAL},
        {"MlY", NOT_FINAL},
        {"MlY1", NOT_FINAL},
        {"MlY2", NOT_FINAL},
        {"MvU", NOT_FINAL},
        {"MlU", NOT_FINAL},
        {"MlU1", NOT_FINAL},
        {"MlU2", NOT_FINAL},
        {"MvR", NOT_FINAL},
        {"MlR", NOT_FINAL},
        {"MlR1", NOT_FINAL},
        {"MlR2", NOT_FINAL},
        {"MvP", NOT_FINAL},
        {"MlP", NOT_FINAL},
        {"MlP1", NOT_FINAL},
        {"MlP2", NOT_FINAL},

        {"MvSF", IS_FINAL},
        {"MlSF", IS_FINAL},
        {"MlS1F", IS_FINAL},
        {"MlS2F", IS_FINAL},
        {"MvNF", IS_FINAL},
        {"MlNF", IS_FINAL},
        {"MlN1F", IS_FINAL},
        {"MlN2F", IS_FINAL},
        {"MvIF", IS_FINAL},
        {"MlIF", IS_FINAL},
        {"MlI1F", IS_FINAL},
        {"MlI2F", IS_FINAL},
        {"MvYF", IS_FINAL},
        {"MlYF", IS_FINAL},
        {"MlY1F", IS_FINAL},
        {"MlY2F", IS_FINAL},
        {"MvUF", IS_FINAL},
        {"MlUF", IS_FINAL},
        {"MlU1F", IS_FINAL},
        {"MlU2F", IS_FINAL},
        {"MvRF", IS_FINAL},
        {"MlRF", IS_FINAL},
        {"MlR1F", IS_FINAL},
        {"MlR2F", IS_FINAL},
        {"MvPF", IS_FINAL},
        {"MlPF", IS_FINAL},
        {"MlP1F", IS_FINAL},
        {"MlP2F", IS_FINAL},

        {"MX", NOT_FINAL},
        {"MX1", NOT_FINAL},
        {"MX2", NOT_FINAL},
        {"MO", NOT_FINAL},
        {"MO1", NOT_FINAL},
        {"MO2", NOT_FINAL},

        {"MXF", IS_FINAL},
        {"MX1F", IS_FINAL},
        {"MX2F", IS_FINAL},
        {"MOF", IS_FINAL},
        {"MO1F", IS_FINAL},
        {"MO2F", IS_FINAL},

        {"MLS", NOT_FINAL},
        {"MLS1", NOT_FINAL},
        {"MLS2", NOT_FINAL},
        {"MLN", NOT_FINAL},
        {"MLN1", NOT_FINAL},
        {"MLN2", NOT_FINAL},
        {"MLI", NOT_FINAL},
        {"MLI1", NOT_FINAL},
        {"MLI2", NOT_FINAL},
        {"MLY", NOT_FINAL},
        {"MLY1", NOT_FINAL},
        {"MLY2", NOT_FINAL},
        {"MLU", NOT_FINAL},
        {"MLU1", NOT_FINAL},
        {"MLU2", NOT_FINAL},
        {"MLR", NOT_FINAL},
        {"MLR1", NOT_FINAL},
        {"MLR2", NOT_FINAL},
        {"MLP", NOT_FINAL},
        {"MLP1", NOT_FINAL},
        {"MLP2", NOT_FINAL},

        {"MLSF", IS_FINAL},
        {"MLS1F", IS_FINAL},
        {"MLS2F", IS_FINAL},
        {"MLNF", IS_FINAL},
        {"MLN1F", IS_FINAL},
        {"MLN2F", IS_FINAL},
        {"MLIF", IS_FINAL},
        {"MLI1F", IS_FINAL},
        {"MLI2F", IS_FINAL},
        {"MLYF", IS_FINAL},
        {"MLY1F", IS_FINAL},
        {"MLY2F", IS_FINAL},
        {"MLUF", IS_FINAL},
        {"MLU1F", IS_FINAL},
        {"MLU2F", IS_FINAL},
        {"MLRF", IS_FINAL},
        {"MLR1F", IS_FINAL},
        {"MLR2F", IS_FINAL},
        {"MLPF", IS_FINAL},
        {"MLP1F", IS_FINAL},
        {"MLP2F", IS_FINAL},

        {"ME", NOT_FINAL},
        {"ME1", NOT_FINAL},
        {"ME2", NOT_FINAL},
        {"MEF", IS_FINAL},
        {"ME1F", IS_FINAL},
        {"ME2F", IS_FINAL},

        {"MES", NOT_FINAL},
        {"MES1", NOT_FINAL},
        {"MES2", NOT_FINAL},
        {"MEN", NOT_FINAL},
        {"MEN1", NOT_FINAL},
        {"MEN2", NOT_FINAL},
        {"MEI", NOT_FINAL},
        {"MEI1", NOT_FINAL},
        {"MEI2", NOT_FINAL},
        {"MEY", NOT_FINAL},
        {"MEY1", NOT_FINAL},
        {"MEY2", NOT_FINAL},
        {"MEU", NOT_FINAL},
        {"MEU1", NOT_FINAL},
        {"MEU2", NOT_FINAL},
        {"MER", NOT_FINAL},
        {"MER1", NOT_FINAL},
        {"MER2", NOT_FINAL},
        {"MEP", NOT_FINAL},
        {"MEP1", NOT_FINAL},
        {"MEP2", NOT_FINAL},

        {"MESF", IS_FINAL},
        {"MES1F", IS_FINAL},
        {"MES2F", IS_FINAL},
        {"MENF", IS_FINAL},
        {"MEN1F", IS_FINAL},
        {"MEN2F", IS_FINAL},
        {"MEIF", IS_FINAL},
        {"MEI1F", IS_FINAL},
        {"MEI2F", IS_FINAL},
        {"MEYF", IS_FINAL},
        {"MEY1F", IS_FINAL},
        {"MEY2F", IS_FINAL},
        {"MEUF", IS_FINAL},
        {"MEU1F", IS_FINAL},
        {"MEU2F", IS_FINAL},
        {"MERF", IS_FINAL},
        {"MER1F", IS_FINAL},
        {"MER2F", IS_FINAL},
        {"MEPF", IS_FINAL},
        {"MEP1F", IS_FINAL},
        {"MEP2F", IS_FINAL}
    };

    private static Log log;
    private final static String prefix = "nsk.jdi.TypeComponent.isFinal.";
    private final static String className = "isfinal002";
    private final static String debugerName = prefix + className;
    private final static String debugeeName = debugerName + "a";
    private final static String classToCheckName = prefix + "isfinal002aClassToCheck";

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
            boolean isFinal;
            String realIsFinal;

            try {
                listOfMethods = refType.methodsByName(METHOD_NAME[i][0]);
            } catch (Exception e) {
                log.complain("debuger FAILURE 1> Can't get method by name "
                           + METHOD_NAME[i][0]);
                log.complain("debuger FAILURE 1> Exception: " + e);
                testFailed = true;
                continue;
            }
            totalMethodsByName = listOfMethods.size();
            if (totalMethodsByName != 1) {
                log.complain("debuger FAILURE 2> Number of methods by name "
                           + METHOD_NAME[i][0] + " is " + totalMethodsByName
                           + ", should be 1.");
                testFailed = true;
                continue;
            }
            method = (Method)listOfMethods.get(0);
            name = method.name();
            isFinal = method.isFinal();
            realIsFinal = METHOD_NAME[i][1];
            log.display("debuger> " + i + " method (" + name + "), "
                      + "isFinal = " + isFinal + " read.");

            // isFinal() returns true if this type component is declared
            // final, returns false otherwise
            if ((isFinal && !realIsFinal.equals(IS_FINAL)) ||
                (!isFinal && realIsFinal.equals(IS_FINAL))
               ) {
                log.display("debuger FAILURE 3> " + i + " method " + name
                          + ": read method.isFinal() = " + isFinal
                          + "; real isFinal should be " + realIsFinal);
                testFailed = true;
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
