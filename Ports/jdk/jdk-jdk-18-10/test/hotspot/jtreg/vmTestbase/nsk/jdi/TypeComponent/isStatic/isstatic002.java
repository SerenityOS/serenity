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


package nsk.jdi.TypeComponent.isStatic;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

public class isstatic002 {
    final static String IS_STATIC = "true";
    final static String NOT_STATIC = "false";
    final static int TOTAL_METHODS = 208;
    final static String METHOD_NAME[][] = {
        {"Mv", NOT_STATIC},
        {"Mz", NOT_STATIC},
        {"Mz1", NOT_STATIC},
        {"Mz2", NOT_STATIC},
        {"Mb", NOT_STATIC},
        {"Mb1", NOT_STATIC},
        {"Mb2", NOT_STATIC},
        {"Mc", NOT_STATIC},
        {"Mc1", NOT_STATIC},
        {"Mc2", NOT_STATIC},
        {"Md", NOT_STATIC},
        {"Md1", NOT_STATIC},
        {"Md2", NOT_STATIC},
        {"Mf", NOT_STATIC},
        {"Mf1", NOT_STATIC},
        {"Mf2", NOT_STATIC},
        {"Mi", NOT_STATIC},
        {"Mi1", NOT_STATIC},
        {"Mi2", NOT_STATIC},
        {"Ml", NOT_STATIC},
        {"Ml1", NOT_STATIC},
        {"Ml2", NOT_STATIC},
        {"Mr", NOT_STATIC},
        {"Mr1", NOT_STATIC},
        {"Mr2", NOT_STATIC},

        {"MvS", IS_STATIC},
        {"MzS", IS_STATIC},
        {"Mz1S", IS_STATIC},
        {"Mz2S", IS_STATIC},
        {"MbS", IS_STATIC},
        {"Mb1S", IS_STATIC},
        {"Mb2S", IS_STATIC},
        {"McS", IS_STATIC},
        {"Mc1S", IS_STATIC},
        {"Mc2S", IS_STATIC},
        {"MdS", IS_STATIC},
        {"Md1S", IS_STATIC},
        {"Md2S", IS_STATIC},
        {"MfS", IS_STATIC},
        {"Mf1S", IS_STATIC},
        {"Mf2S", IS_STATIC},
        {"MiS", IS_STATIC},
        {"Mi1S", IS_STATIC},
        {"Mi2S", IS_STATIC},
        {"MlS", IS_STATIC},
        {"Ml1S", IS_STATIC},
        {"Ml2S", IS_STATIC},
        {"MrS", IS_STATIC},
        {"Mr1S", IS_STATIC},
        {"Mr2S", IS_STATIC},

        {"MvF", NOT_STATIC},
        {"MlF", NOT_STATIC},
        {"MlF1", NOT_STATIC},
        {"MlF2", NOT_STATIC},
        {"MvN", NOT_STATIC},
        {"MlN", NOT_STATIC},
        {"MlN1", NOT_STATIC},
        {"MlN2", NOT_STATIC},
        {"MvI", NOT_STATIC},
        {"MlI", NOT_STATIC},
        {"MlI1", NOT_STATIC},
        {"MlI2", NOT_STATIC},
        {"MvY", NOT_STATIC},
        {"MlY", NOT_STATIC},
        {"MlY1", NOT_STATIC},
        {"MlY2", NOT_STATIC},
        {"MvU", NOT_STATIC},
        {"MlU", NOT_STATIC},
        {"MlU1", NOT_STATIC},
        {"MlU2", NOT_STATIC},
        {"MvR", NOT_STATIC},
        {"MlR", NOT_STATIC},
        {"MlR1", NOT_STATIC},
        {"MlR2", NOT_STATIC},
        {"MvP", NOT_STATIC},
        {"MlP", NOT_STATIC},
        {"MlP1", NOT_STATIC},
        {"MlP2", NOT_STATIC},

        {"MvFS", IS_STATIC},
        {"MlFS", IS_STATIC},
        {"MlF1S", IS_STATIC},
        {"MlF2S", IS_STATIC},
        {"MvNS", IS_STATIC},
        {"MlNS", IS_STATIC},
        {"MlN1S", IS_STATIC},
        {"MlN2S", IS_STATIC},
        {"MvIS", IS_STATIC},
        {"MlIS", IS_STATIC},
        {"MlI1S", IS_STATIC},
        {"MlI2S", IS_STATIC},
        {"MvYS", IS_STATIC},
        {"MlYS", IS_STATIC},
        {"MlY1S", IS_STATIC},
        {"MlY2S", IS_STATIC},
        {"MvUS", IS_STATIC},
        {"MlUS", IS_STATIC},
        {"MlU1S", IS_STATIC},
        {"MlU2S", IS_STATIC},
        {"MvRS", IS_STATIC},
        {"MlRS", IS_STATIC},
        {"MlR1S", IS_STATIC},
        {"MlR2S", IS_STATIC},
        {"MvPS", IS_STATIC},
        {"MlPS", IS_STATIC},
        {"MlP1S", IS_STATIC},
        {"MlP2S", IS_STATIC},

        {"MX", NOT_STATIC},
        {"MX1", NOT_STATIC},
        {"MX2", NOT_STATIC},
        {"MO", NOT_STATIC},
        {"MO1", NOT_STATIC},
        {"MO2", NOT_STATIC},

        {"MXS", IS_STATIC},
        {"MX1S", IS_STATIC},
        {"MX2S", IS_STATIC},
        {"MOS", IS_STATIC},
        {"MO1S", IS_STATIC},
        {"MO2S", IS_STATIC},

        {"MLF", NOT_STATIC},
        {"MLF1", NOT_STATIC},
        {"MLF2", NOT_STATIC},
        {"MLN", NOT_STATIC},
        {"MLN1", NOT_STATIC},
        {"MLN2", NOT_STATIC},
        {"MLI", NOT_STATIC},
        {"MLI1", NOT_STATIC},
        {"MLI2", NOT_STATIC},
        {"MLY", NOT_STATIC},
        {"MLY1", NOT_STATIC},
        {"MLY2", NOT_STATIC},
        {"MLU", NOT_STATIC},
        {"MLU1", NOT_STATIC},
        {"MLU2", NOT_STATIC},
        {"MLR", NOT_STATIC},
        {"MLR1", NOT_STATIC},
        {"MLR2", NOT_STATIC},
        {"MLP", NOT_STATIC},
        {"MLP1", NOT_STATIC},
        {"MLP2", NOT_STATIC},

        {"MLFS", IS_STATIC},
        {"MLF1S", IS_STATIC},
        {"MLF2S", IS_STATIC},
        {"MLNS", IS_STATIC},
        {"MLN1S", IS_STATIC},
        {"MLN2S", IS_STATIC},
        {"MLIS", IS_STATIC},
        {"MLI1S", IS_STATIC},
        {"MLI2S", IS_STATIC},
        {"MLYS", IS_STATIC},
        {"MLY1S", IS_STATIC},
        {"MLY2S", IS_STATIC},
        {"MLUS", IS_STATIC},
        {"MLU1S", IS_STATIC},
        {"MLU2S", IS_STATIC},
        {"MLRS", IS_STATIC},
        {"MLR1S", IS_STATIC},
        {"MLR2S", IS_STATIC},
        {"MLPS", IS_STATIC},
        {"MLP1S", IS_STATIC},
        {"MLP2S", IS_STATIC},

        {"ME", NOT_STATIC},
        {"ME1", NOT_STATIC},
        {"ME2", NOT_STATIC},
        {"MES", IS_STATIC},
        {"ME1S", IS_STATIC},
        {"ME2S", IS_STATIC},

        {"MEF", NOT_STATIC},
        {"MEF1", NOT_STATIC},
        {"MEF2", NOT_STATIC},
        {"MEN", NOT_STATIC},
        {"MEN1", NOT_STATIC},
        {"MEN2", NOT_STATIC},
        {"MEI", NOT_STATIC},
        {"MEI1", NOT_STATIC},
        {"MEI2", NOT_STATIC},
        {"MEY", NOT_STATIC},
        {"MEY1", NOT_STATIC},
        {"MEY2", NOT_STATIC},
        {"MEU", NOT_STATIC},
        {"MEU1", NOT_STATIC},
        {"MEU2", NOT_STATIC},
        {"MER", NOT_STATIC},
        {"MER1", NOT_STATIC},
        {"MER2", NOT_STATIC},
        {"MEP", NOT_STATIC},
        {"MEP1", NOT_STATIC},
        {"MEP2", NOT_STATIC},

        {"MEFS", IS_STATIC},
        {"MEF1S", IS_STATIC},
        {"MEF2S", IS_STATIC},
        {"MENS", IS_STATIC},
        {"MEN1S", IS_STATIC},
        {"MEN2S", IS_STATIC},
        {"MEIS", IS_STATIC},
        {"MEI1S", IS_STATIC},
        {"MEI2S", IS_STATIC},
        {"MEYS", IS_STATIC},
        {"MEY1S", IS_STATIC},
        {"MEY2S", IS_STATIC},
        {"MEUS", IS_STATIC},
        {"MEU1S", IS_STATIC},
        {"MEU2S", IS_STATIC},
        {"MERS", IS_STATIC},
        {"MER1S", IS_STATIC},
        {"MER2S", IS_STATIC},
        {"MEPS", IS_STATIC},
        {"MEP1S", IS_STATIC},
        {"MEP2S", IS_STATIC},
    };

    private static Log log;
    private final static String prefix = "nsk.jdi.TypeComponent.isStatic.";
    private final static String className = "isstatic002";
    private final static String debugerName = prefix + className;
    private final static String debugeeName = debugerName + "a";
    private final static String classToCheckName = prefix + "isstatic002aClassToCheck";

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
            boolean isStatic;
            String realIsStatic;

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
            isStatic = method.isStatic();
            realIsStatic = METHOD_NAME[i][1];
            log.display("debuger> " + i + " method (" + name + "), "
                      + "isStatic = " + isStatic + " read.");

            // isStatic() returns true if this type component is declared
            // final, returns false otherwise
            if ((isStatic && !realIsStatic.equals(IS_STATIC)) ||
                (!isStatic && realIsStatic.equals(IS_STATIC))
               ) {
                log.display("debuger FAILURE 3> " + i + " method " + name
                          + ": read method.isStatic() = " + isStatic
                          + "; real isStatic should be " + realIsStatic);
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
