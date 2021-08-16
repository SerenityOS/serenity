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


package nsk.jdi.TypeComponent.declaringType;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

public class decltype004 {
    final static String METHOD_NAME_CLASS[] = {
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
    final static String METHOD_NAME_INTER[] = {
        "Mv",
        "Mz", "Mz1", "Mz2",
        "Mb", "Mb1", "Mb2",
        "Mc", "Mc1", "Mc2",
        "Md", "Md1", "Md2",
        "Mf", "Mf1", "Mf2",
        "Mi", "Mi1", "Mi2",
        "Ml", "Ml1", "Ml2",
        "Mr", "Mr1", "Mr2",

        "MvU", "MlU", "MlU1", "MlU2",

        "MX", "MX1", "MX2",
        "MO", "MO1", "MO2",

        "MLU", "MLU1", "MLU2",

        "ME", "ME1", "ME2",
        "MEU", "MEU1", "MEU2",
    };

    private static Log log;
    private final static String prefix = "nsk.jdi.TypeComponent.declaringType.";
    private final static String className = "decltype004";
    private final static String debugerName = prefix + className;
    private final static String debugeeName = debugerName + "a";
    private final static String mainClassName = prefix + "decltype004aMainClass";
    private final static String mainInterName = prefix + "decltype004aMainInter";

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
        ReferenceType mainClass;
        ReferenceType mainInter;

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

        // Get MainClass and decltype004aMainInter from debugee
        mainClass = debugee.classByName(mainClassName);
        if (mainClass == null) {
           log.complain("debuger FAILURE> Class " + mainClassName + " not "
                      + "found.");
           return 2;
        }
        mainInter = debugee.classByName(mainInterName);
        if (mainInter == null) {
           log.complain("debuger FAILURE> Interface " + mainInterName + " not "
                      + "found.");
           return 2;
        }

        // Check all methods from MainClass
        log.display("debuger> Total methods in " + mainClassName + ": " +
                  + mainClass.methods().size());
        for (int i = 0; i < METHOD_NAME_CLASS.length; i++) {
            Method method;
            List listOfMethods;
            int totalMethodsByName;
            String name;
            ReferenceType declType;
            boolean equal;

            try {
                listOfMethods = mainClass.methodsByName(METHOD_NAME_CLASS[i]);
            } catch (Exception e) {
                log.complain("debuger FAILURE 1> Can't get method by name "
                           + METHOD_NAME_CLASS[i]);
                log.complain("debuger FAILURE 1> Exception: " + e);
                testFailed = true;
                continue;
            }
            totalMethodsByName = listOfMethods.size();
            if (totalMethodsByName != 1) {
                log.complain("debuger FAILURE 2> Number of methods by name "
                           + METHOD_NAME_CLASS[i] + " is " + totalMethodsByName
                           + ", should be 1.");
                testFailed = true;
                continue;
            }
            method = (Method)listOfMethods.get(0);
            name = method.name();
            declType = method.declaringType();
            if (declType == null) {
                log.complain("debuger FAILURE 3> Declaring type is null "
                               + " for method " + name + " in class "
                               + mainClassName);
                testFailed = true;
                continue;
            }
            log.display("debuger> " + i + " method " + name + " from class "
                      + mainClassName + " read.");
            try {
                equal = declType.equals(mainClass);
            } catch (ObjectCollectedException e) {
                log.complain("debuger FAILURE 4> Cannot compare reference "
                           + " types " + declType.name() + " and "
                           + mainClassName);
                log.complain("debuger FAILURE 4> Exception: " + e);
                testFailed = true;
                continue;
            }
            if (!equal) {
                log.complain("debuger FAILURE 5> Declaring type of method "
                           + name + " is " + declType.name() + ", but should "
                           + "be " + mainClassName);
                testFailed = true;
            }
        }

        // Check all methods from MainInter
        log.display("debuger> Total methods in " + mainInterName + ": " +
                  + mainInter.methods().size());
        for (int i = 0; i < METHOD_NAME_INTER.length; i++) {
            Method method;
            List listOfMethods;
            int totalMethodsByName;
            String name;
            ReferenceType declType;
            boolean equal;

            try {
                listOfMethods = mainInter.methodsByName(METHOD_NAME_INTER[i]);
            } catch (Exception e) {
                log.complain("debuger FAILURE 6> Can't get method by name "
                           + METHOD_NAME_INTER[i]);
                log.complain("debuger FAILURE 6> Exception: " + e);
                testFailed = true;
                continue;
            }
            totalMethodsByName = listOfMethods.size();
            if (totalMethodsByName != 1) {
                log.complain("debuger FAILURE 7> Number of methods by name "
                           + METHOD_NAME_INTER[i] + " is " + totalMethodsByName
                           + ", should be 1.");
                testFailed = true;
                continue;
            }
            method = (Method)listOfMethods.get(0);
            name = method.name();
            declType = method.declaringType();
            if (declType == null) {
                log.complain("debuger FAILURE 8> Declaring type is null "
                               + " for method " + name + " in class "
                               + mainInterName);
                testFailed = true;
                continue;
            }
            log.display("debuger> " + i + " method " + name + " from interface "
                      + mainInterName + " read.");
            try {
                equal = declType.equals(mainInter);
            } catch (ObjectCollectedException e) {
                log.complain("debuger FAILURE 9> Cannot compare reference "
                           + " types " + declType.name() + " and "
                           + mainInterName);
                log.complain("debuger FAILURE 9> Exception: " + e);
                testFailed = true;
                continue;
            }
            if (!equal) {
                log.complain("debuger FAILURE 10> Declaring type of method "
                           + name + " is " + declType.name() + ", but should "
                           + "be " + mainInterName);
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
