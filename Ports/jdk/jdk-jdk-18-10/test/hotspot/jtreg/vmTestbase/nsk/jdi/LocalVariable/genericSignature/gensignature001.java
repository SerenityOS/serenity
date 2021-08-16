/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.LocalVariable.genericSignature;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import java.util.*;
import java.io.*;

/**
 * The test for the LocalVariable.genericSignature() method.
 *
 * This class is used as debugger part of the test.
 */

public class gensignature001 {

    // communication signals constants
    static final String READY = "ready";
    static final String QUIT = "quit";

    static final String PACKAGE_NAME = "nsk.jdi.LocalVariable.genericSignature.";
    static final String DEBUGEE_CLASS_NAME = PACKAGE_NAME + "gensignature001a";

    // tested class name and signature constants
    static final String TESTED_CLASS_NAME = DEBUGEE_CLASS_NAME + "$" + "TestedClass";
    static final String TESTED_CLASS_SIGNATURE = "L" + TESTED_CLASS_NAME.replace('.', '/') + ";";
    static final String THIS_GENERIC_SIGNATURE = "L" + TESTED_CLASS_NAME.replace('.', '/') + "<TT;TN;>" + ";";

    // tested method name constant
    static final String TESTED_METHOD_NAME = "testedMethod";

    private static Debugee             debugee;
    private static ReferenceType       debugeeClass;

    // list of tested variables names and signatures
    static final String varsList[][] = {

                // not generic arguments
                {"arg11PrimBoolean",    null},
                {"arg12PrimInt",        null},
                {"arg13Object",         null},
                {"arg14String",         null},
                {"arg15PrimArrShort",   null},
                {"arg16ObjArrObject",   null},

                // generic arguments
                {"arg21GenObject",            "TT;"},
                {"arg22GenNumber",            "TN;"},
                {"arg23GenObjectArr",         "[TT;"},
                {"arg24GenNumberArr",         "[TN;"},
                {"arg25GenObjectList",        "Ljava/util/List<TT;>;"},
                {"arg26GenNumberList",        "Ljava/util/List<TN;>;"},
                {"arg27GenObjectDerivedList", "Ljava/util/List<+TT;>;"},
                {"arg28GenNumberDerivedList", "Ljava/util/List<+TN;>;"},

                // not generic variables
                {"var11PrimBoolean",   null},
                {"var12PrimInt",       null},
                {"var13Object",        null},
                {"var14String",        null},
                {"var15PrimArrShort",  null},
                {"var16ObjArrObject",  null},

                // generic variables
                {"var21GenObject",            "TT;"},
                {"var22GenNumber",            "TN;"},
                {"var23GenObjectArr",         "[TT;"},
                {"var24GenNumberArr",         "[TN;"},
                {"var25GenObjectList",        "Ljava/util/List<TT;>;"},
                {"var26GenNumberList",        "Ljava/util/List<TN;>;"},
                {"var27GenObjectDerivedList", "Ljava/util/List<+TT;>;"},
                {"var28GenNumberDerivedList", "Ljava/util/List<+TN;>;"},


    };

    static ArgumentHandler argHandler;
    static Log log;

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + Consts.JCK_STATUS_BASE);
    }

    public static int run (String argv[], PrintStream out) {
        return new gensignature001().runThis(argv, out);
    }

    private int runThis (String argv[], PrintStream out) {

        int testResult = Consts.TEST_PASSED;

        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        debugee = Debugee.prepareDebugee(argHandler, log, DEBUGEE_CLASS_NAME);

        debugeeClass = debugee.classByName(DEBUGEE_CLASS_NAME);
        if ( debugeeClass == null ) {
            log.complain("Class '" + DEBUGEE_CLASS_NAME + "' not found.");
            testResult = Consts.TEST_FAILED;
        }


        log.display("Checking started.");
        do {
            ReferenceType testedClass = debugee.classByName(TESTED_CLASS_NAME);
            log.display("Found tested class: " + testedClass.name());

            Method testedMethod = debugee.methodByName(testedClass, TESTED_METHOD_NAME);
            log.display("Found tested method: " + testedMethod.name());

            for (int i = 0; i < varsList.length; i++) {

                List localVars = null;
                try {
                    localVars = testedMethod.variablesByName(varsList[i][0]);
                } catch (AbsentInformationException e) {
                    log.complain("Unexpected AbsentInformationException while calling variablesByName() for " +
                        varsList[i][0]);
                    testResult = Consts.TEST_FAILED;
                    continue;
                }
                if (localVars.size() != 1) {
                    log.complain("Not unique local variable '" + varsList[i][0] + "' : " + localVars.size());
                    testResult = Consts.TEST_FAILED;
                    continue;
                }
                log.display("Found local variable: " + varsList[i][0]);

                LocalVariable var = (LocalVariable)localVars.get(0);
                String expSignature = varsList[i][1];
                log.display("Getting generic signature for local variable: " + varsList[i][0]);

                String actSignature = "";
                try {
                    actSignature = var.genericSignature();
                } catch (Exception e) {
                    log.complain("Unexpected exception while calling genericSignature() for " +
                        varsList[i][0] + " : " + e.getMessage());
                    e.printStackTrace(out);
                    testResult = Consts.TEST_FAILED;
                    continue;
                }

                if ((expSignature == null && actSignature != null) ||
                        (expSignature != null && !expSignature.equals(actSignature))) {
                    log.complain("Unexpected generic signature for local variable '" + varsList[i][0] + "': " +
                        actSignature + "\n\tExpected generic signature: " + expSignature);
                    testResult = Consts.TEST_FAILED;
                } else {
                    log.display("\tgot expected generic signature: " + actSignature);
                }
            }

        } while (false);
        log.display("All checking completed.");

        debugee.quit();
        return testResult;
    }
} // end of gensignature001 class
