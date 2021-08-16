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


package nsk.jdi.TypeComponent.signature;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

public class sign003 {
    final static String CONSTR_SIGN[] = {
        "()V",
        "(ID)V", "([I[D)V", "([[I[[D)V",

        "(Ljava/lang/Long;Ljava/lang/String;Ljava/lang/Object;)V",
        "([Ljava/lang/Long;[Ljava/lang/String;[Ljava/lang/Object;)V",
        "([[Ljava/lang/Long;[[Ljava/lang/String;[[Ljava/lang/Object;)V",

        "(J)V", "(Ljava/lang/Object;)V", "([J)V"
    };
    private static Log log;
    private final static String prefix = "nsk.jdi.TypeComponent.signature.";
    private final static String className = "sign003";
    private final static String debugerName = prefix + className;
    private final static String debugeeName = debugerName + "a";
    private final static String classToCheckName = prefix + "sign003aClassToCheck";

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
        List methods;

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

        // Get all methods, find constructors and static initializers and
        // check them
        try {
            methods = refType.methods();
        } catch (Exception e) {
            log.complain("debuger FAILURE> Can't get methods from class");
            log.complain("debuger FAILURE> Exception: " + e);
            return 2;
        }
        int totalMethods = methods.size();
        if (totalMethods < 1) {
            log.complain("debuger FAILURE> Total number of methods read "
                       + totalMethods);
            return 2;
        }
        log.display("debuger> Total methods found: " + totalMethods);
        Iterator methodsIterator = methods.iterator();
        for (int i = 0; methodsIterator.hasNext(); i++) {
            Method method = (Method)methodsIterator.next();
            String decTypeName = method.declaringType().name();
            String name = method.name();
            String signature = method.signature();

            if (method.isConstructor()) {
                boolean signFound = false;

                log.display("debuger> " + i + " method (constructor) " + name
                          + " from " + decTypeName + " with signature "
                          + signature + " read.");
                for (int j = 0; j < CONSTR_SIGN.length; j++) {
                    // If signature does not exist in list then failure
                    String realSign = CONSTR_SIGN[j];

                    if (realSign.equals(signature)) {
                        // Signature found - OK
                        signFound = true;
                        log.display("debuger> Signature " + signature
                                  + " found in list - " + j + " element.");
                        break;
                    }
                }
                if (!signFound) {
                    log.complain("debuger FAILURE 1> Signature " + signature
                               + " not found in expected list.");
                    testFailed = true;
                    continue;
                }
            } else {
                if (method.isStaticInitializer()) {
                    log.display("debuger> " + i + " method (static "
                              + "initializer) " + name + " from "
                              + decTypeName + " with signature " + signature
                              + " read.");
                    if (signature == null) {
                        log.complain("debuger FAILURE 2> Static initializer "
                                   + "from " + decTypeName + " has signature "
                                   + "null.");
                        testFailed = true;
                        continue;
                    }
                    if (signature.length() == 0) {
                        log.complain("debuger FAILURE 3> Static initializer "
                                   + "from " + decTypeName + " has empty "
                                   + "signature.");
                        testFailed = true;
                        continue;
                    }
                } else {
                    log.display("debuger> " + i + " method " + name + " from "
                              + decTypeName + " passed.");
                }
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
