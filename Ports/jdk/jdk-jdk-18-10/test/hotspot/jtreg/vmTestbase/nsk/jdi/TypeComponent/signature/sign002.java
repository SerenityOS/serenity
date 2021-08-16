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

public class sign002 {
    final static int TOTAL_METHODS = 114;
    final static String METHOD_NAME[][] = {
        {"Mv", "()V"},
        {"Mz", "(Z[[S)Z"},
        {"Mz1", "([ZZ)[Z"},
        {"Mz2", "([[Z[Z)[[Z"},
        {"Mb", "(B[[Z)B"},
        {"Mb1", "([BB)[B"},
        {"Mb2", "([[B[B)[[B"},
        {"Mc", "(C[[B)C"},
        {"Mc1", "([CC)[C"},
        {"Mc2", "([[C[C)[[C"},
        {"Md", "(D[[C)D"},
        {"Md1", "([DD)[D"},
        {"Md2", "([[D[D)[[D"},
        {"Mf", "(F[[D)F"},
        {"Mf1", "([FF)[F"},
        {"Mf2", "([[F[F)[[F"},
        {"Mi", "(I[[F)I"},
        {"Mi1", "([II)[I"},
        {"Mi2", "([[I[I)[[I"},
        {"Ml", "(J[[I)J"},
        {"Ml1", "([JJ)[J"},
        {"Ml2", "([[J[J)[[J"},
        {"Mr", "(S[[J)S"},
        {"Mr1", "([SS)[S"},
        {"Mr2", "([[S[S)[[S"},
        {"MvF", "()V"},
        {"MlF", "(J)J"},
        {"MlF1", "([J)[J"},
        {"MlF2", "([[J)[[J"},
        {"MvN", "()V"},
        {"MlN", "(J)J"},
        {"MlN1", "([J)[J"},
        {"MlN2", "([[J)[[J"},
        {"MvS", "()V"},
        {"MlS", "(J)J"},
        {"MlS1", "([J)[J"},
        {"MlS2", "([[J)[[J"},
        {"MvI", "()V"},
        {"MlI", "(J)J"},
        {"MlI1", "([J)[J"},
        {"MlI2", "([[J)[[J"},
        {"MvY", "()V"},
        {"MlY", "(J)J"},
        {"MlY1", "([J)[J"},
        {"MlY2", "([[J)[[J"},
        {"MvU", "()V"},
        {"MlU", "(J)J"},
        {"MlU1", "([J)[J"},
        {"MlU2", "([[J)[[J"},
        {"MvR", "()V"},
        {"MlR", "(J)J"},
        {"MlR1", "([J)[J"},
        {"MlR2", "([[J)[[J"},
        {"MvP", "()V"},
        {"MlP", "(J)J"},
        {"MlP1", "([J)[J"},
        {"MlP2", "([[J)[[J"},
        {"MX", "(Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Class;[[Ljava/lang/Object;)Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Class;"},
        {"MX1", "([Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Class;Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Class;)[Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Class;"},
        {"MX2", "([[Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Class;[Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Class;)[[Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Class;"},
        {"MO", "(Ljava/lang/Object;[[Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Class;)Ljava/lang/Object;"},
        {"MO1", "([Ljava/lang/Object;Ljava/lang/Object;)[Ljava/lang/Object;"},
        {"MO2", "([[Ljava/lang/Object;[Ljava/lang/Object;)[[Ljava/lang/Object;"},
        {"MLF", "(Ljava/lang/Long;)Ljava/lang/Long;"},
        {"MLF1", "([Ljava/lang/Long;)[Ljava/lang/Long;"},
        {"MLF2", "([[Ljava/lang/Long;)[[Ljava/lang/Long;"},
        {"MLN", "(Ljava/lang/Long;)Ljava/lang/Long;"},
        {"MLN1", "([Ljava/lang/Long;)[Ljava/lang/Long;"},
        {"MLN2", "([[Ljava/lang/Long;)[[Ljava/lang/Long;"},
        {"MLS", "(Ljava/lang/Long;)Ljava/lang/Long;"},
        {"MLS1", "([Ljava/lang/Long;)[Ljava/lang/Long;"},
        {"MLS2", "([[Ljava/lang/Long;)[[Ljava/lang/Long;"},
        {"MLI", "(Ljava/lang/Long;)Ljava/lang/Long;"},
        {"MLI1", "([Ljava/lang/Long;)[Ljava/lang/Long;"},
        {"MLI2", "([[Ljava/lang/Long;)[[Ljava/lang/Long;"},
        {"MLY", "(Ljava/lang/Long;)Ljava/lang/Long;"},
        {"MLY1", "([Ljava/lang/Long;)[Ljava/lang/Long;"},
        {"MLY2", "([[Ljava/lang/Long;)[[Ljava/lang/Long;"},
        {"MLU", "(Ljava/lang/Long;)Ljava/lang/Long;"},
        {"MLU1", "([Ljava/lang/Long;)[Ljava/lang/Long;"},
        {"MLU2", "([[Ljava/lang/Long;)[[Ljava/lang/Long;"},
        {"MLR", "(Ljava/lang/Long;)Ljava/lang/Long;"},
        {"MLR1", "([Ljava/lang/Long;)[Ljava/lang/Long;"},
        {"MLR2", "([[Ljava/lang/Long;)[[Ljava/lang/Long;"},
        {"MLP", "(Ljava/lang/Long;)Ljava/lang/Long;"},
        {"MLP1", "([Ljava/lang/Long;)[Ljava/lang/Long;"},
        {"MLP2", "([[Ljava/lang/Long;)[[Ljava/lang/Long;"},
        {"ME", "(IJLnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;)Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;"},
        {"ME1", "([I[Ljava/lang/Long;[Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;)[Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;"},
        {"ME2", "([[I[[Ljava/lang/Long;[[Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;)[[Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;"},
        {"MEF", "(Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;)Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;"},
        {"MEF1", "([Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;)[Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;"},
        {"MEF2", "([[Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;)[[Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;"},
        {"MEN", "(Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;)Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;"},
        {"MEN1", "([Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;)[Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;"},
        {"MEN2", "([[Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;)[[Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;"},
        {"MES", "(Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;)Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;"},
        {"ME1S", "([Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;)[Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;"},
        {"ME2S", "([[Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;)[[Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;"},
        {"MEI", "(Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;)Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;"},
        {"MEI1", "([Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;)[Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;"},
        {"MEI2", "([[Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;)[[Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;"},
        {"MEY", "(Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;)Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;"},
        {"MEY1", "([Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;)[Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;"},
        {"MEY2", "([[Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;)[[Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;"},
        {"MEU", "(Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;)Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;"},
        {"MEU1", "([Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;)[Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;"},
        {"MEU2", "([[Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;)[[Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;"},
        {"MER", "(Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;)Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;"},
        {"MER1", "([Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;)[Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;"},
        {"MER2", "([[Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;)[[Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;"},
        {"MEP", "(Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;)Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;"},
        {"MEP1", "([Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;)[Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;"},
        {"MEP2", "([[Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;)[[Lnsk/jdi/TypeComponent/signature/sign002aClassToCheck$Inter;"},
    };

    private static Log log;
    private final static String prefix = "nsk.jdi.TypeComponent.signature.";
    private final static String className = "sign002";
    private final static String debugerName = prefix + className;
    private final static String debugeeName = debugerName + "a";
    private final static String classToCheckName = prefix + "sign002aClassToCheck";

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
        for (int i = 0; i < TOTAL_METHODS; i++) {
            Method method;
            List listOfMethods;
            int totalMethodsByName;
            String signature;
            String realSign;

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
            signature = method.signature();
            realSign = METHOD_NAME[i][1];
            log.display("debuger> " + i + " method (" + METHOD_NAME[i][0]
                      + ") with signature " + signature + " read.");
            if (!realSign.equals(signature)) {
                log.complain("debuger FAILURE 3> Returned signature for method "
                           + " (" + METHOD_NAME[i][0] + ") is " + signature
                           + " expected " + realSign);
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
