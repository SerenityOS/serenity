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


package nsk.jdi.TypeComponent.isSynthetic;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

public class issynthetic002 {


    final static String SYNTHETIC_METHOD_NAME = "test";
    final static String SYNTHETIC_METHOD_SIGNATURE = "(Ljava/lang/Object;)Ljava/lang/Object;";

    private static Log log;
    private final static String prefix = "nsk.jdi.TypeComponent.isSynthetic.";
    private final static String className = "issynthetic002";
    private final static String debuggerName = prefix + className;
    private final static String debuggeeName = debuggerName + "a";
    private final static String classToCheckName = prefix + "issynthetic002aClassToCheck";

    public static void main(String argv[]) {
        System.exit(95 + run(argv, System.out));
    }

    public static int run(String argv[], PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);
        Debugee debuggee = binder.bindToDebugee(debuggeeName
                              + (argHandler.verbose() ? " -verbose" : ""));
        VirtualMachine vm = debuggee.VM();
        boolean canGetSynthetic = vm.canGetSyntheticAttribute();
        IOPipe pipe = new IOPipe(debuggee);
        boolean testFailed = false;
        List methods;
        int totalSyntheticMethods = 0;

        log.display("debugger> Value of canGetSyntheticAttribute in current "
                  + "VM is " + canGetSynthetic);

        // Connect with debuggee and resume it
        debuggee.redirectStderr(out);
        debuggee.resume();
        String line = pipe.readln();
        if (line == null) {
            log.complain("debugger FAILURE> UNEXPECTED debuggee's signal - null");
            return 2;
        }
        if (!line.equals("ready")) {
            log.complain("debugger FAILURE> UNEXPECTED debuggee's signal - "
                      + line);
            return 2;
        }
        else {
            log.display("debugger> debuggee's \"ready\" signal received.");
        }

        ReferenceType refType = debuggee.classByName(classToCheckName);
        if (refType == null) {
            log.complain("debugger FAILURE> Class " + classToCheckName
                       + " not found.");
            return 2;
        }

        // Check methods from debuggee
        try {
            methods = refType.methods();
        } catch (Exception e) {
            log.complain("debugger FAILURE> Can't get methods from "
                       + classToCheckName);
            log.complain("debugger FAILURE> Exception: " + e);
            return 2;
        }
        int totalMethods = methods.size();
        if (totalMethods < 1) {
            log.complain("debugger FAILURE> Total number of methods in debuggee "
                       + "read " + totalMethods);
            return 2;
        }
        log.display("debugger> Total methods in debuggee read: "
                  + totalMethods);
        for (int i = 0; i < totalMethods; i++) {
            Method method = (Method)methods.get(i);
            String name = method.name();
            String signature = method.signature();
            boolean isSynthetic;

            try {
                isSynthetic = method.isSynthetic();

                if (!canGetSynthetic) {
                    log.complain("debugger FAILURE 1> Value of "
                               + "canGetSyntheticAttribute in current VM is "
                               + "false, so UnsupportedOperationException was "
                               + "expected for " + i + " method " + name);
                    testFailed = true;
                    continue;
                } else {
                    log.display("debugger> " + i + " method " + name + " with "
                              + "synthetic value " + isSynthetic + " read "
                              + "without UnsupportedOperationException");
                }
            } catch (UnsupportedOperationException e) {
                if (canGetSynthetic) {
                    log.complain("debugger FAILURE 2> Value of "
                               + "canGetSyntheticAttribute in current VM is "
                               + "true, but cannot get synthetic for method "
                               + "name.");
                    log.complain("debugger FAILURE 2> Exception: " + e);
                    testFailed = true;
                } else {
                    log.display("debugger> UnsupportedOperationException was "
                              + "thrown while getting isSynthetic for " + i
                              + " method " + name + " because value "
                              + "canGetSynthetic is false.");
                }
                continue;
            }


            if (isSynthetic) {
                if (SYNTHETIC_METHOD_NAME.equals(name) && SYNTHETIC_METHOD_SIGNATURE.equals(signature)) {
                    totalSyntheticMethods++;
                } else {
                    testFailed = true;
                    log.complain("debugger FAILURE 3> Found unexpected synthetic method " + name
                            + signature);
                }
            }
        }

        if (totalSyntheticMethods == 0) {
            log.complain("debugger FAILURE 4> Synthetic methods not found.");
            testFailed = true;
        } else if (totalSyntheticMethods > 1 ) {
            log.complain("debugger FAILURE 5> More than one Synthetic method is found.");
            testFailed = true;
        }

        pipe.println("quit");
        debuggee.waitFor();
        int status = debuggee.getStatus();
        if (testFailed) {
            log.complain("debugger FAILURE> TEST FAILED");
            return 2;
        } else {
            if (status == 95) {
                log.display("debugger> expected Debuggee's exit "
                          + "status - " + status);
                return 0;
            } else {
                log.complain("debugger FAILURE> UNEXPECTED Debuggee's exit "
                           + "status (not 95) - " + status);
                return 2;
            }
        }
    }
}
