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

package nsk.jdi.ReferenceType._bounds_;

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import com.sun.jdi.request.*;

import java.io.*;
import java.util.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


/**
 * The test checks up the following methods of <code>com.sun.jdi.ReferenceType</code>:

 *  - <code>locationsOfLine(int)</code> for boundary values of <code>Integer</code>

 *  - <code>locationsOfLine(String, String, int)</code> for boundary values of
 * Integer and various combinations of value of String arguments such as
 * <code>null, "", <bad_string></code>, where <bad_string> means the some
 * names of nonexisting object.
 *
 *  - <code>equals(Object)</code> for null-value
 */
public class bounds001 {

    private final static String prefix = "nsk.jdi.ReferenceType._bounds_.";
    private final static String debuggerName = prefix + "bounds001";
    private final static String debugeeName = debuggerName + "a";

    public final static String SGNL_READY = "ready";
    public final static String SGNL_QUIT = "quit";

    public static int exitStatus;
    public static Log log;
    public static Debugee debugee;

    private static String propertyValue = "something";
    private static int lineNumbers[] = {
                                        Integer.MIN_VALUE,
                                        -1,
                                        Integer.MAX_VALUE
    };

    private static long codeIndexes[] = {
                                        Long.MIN_VALUE,
                                        -1,
                                        Long.MAX_VALUE
    };

    private static String[][] strParams = {
        {null,               null                  },
        {null,               ""                    },
        {null,               "bounds001_hotchpotch"},
        {"",                 null                  },
        {"",                 ""                    },
        {"",                 "bounds001_hotchpotch"},
        {"bounds001_jumble", null                  },
        {"bounds001_jumble", ""                    },
        {"bounds001_jumble", "bounds001_hotchpotch"}
    };

//----------------------------------------------------

    public static void display(String msg) {
        log.display(msg);
    }

    public static void complain(String msg) {
        log.complain("debugger FAILURE> " + msg + "\n");
    }

    public static void main(String argv[]) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    public static int run(String argv[], PrintStream out) {

        exitStatus = Consts.TEST_PASSED;

        bounds001 thisTest = new bounds001();

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        debugee = Debugee.prepareDebugee(argHandler, log, debugeeName);

        thisTest.execTest();
        display("Test finished. exitStatus = " + exitStatus);

        return exitStatus;
    }

    private void execTest() {

        ReferenceType testedClass = debugee.classByName(debugeeName);

        display("\nTEST BEGINS");
        display("===========");

        List list = null;

        display("");
        display("invoking locationsOfLine(int):");
        display("------------------------------");
        for (int i = 0; i < lineNumbers.length; i++) {
            display("\tparameter: " + lineNumbers[i]);
            try {
                list = testedClass.locationsOfLine(lineNumbers[i]);
                display("\tsize of locations list: " + list.size());
                if (list.size() > 0) {
                    complain("\twrong size");
                    exitStatus = Consts.TEST_FAILED;
                }
            } catch (Exception e) {
                complain("\tUnexpected: " + e);
                exitStatus = Consts.TEST_FAILED;
            }
            display("");
        }

        display("invoking locationsOfLine(String, String, int):");
        display("----------------------------------------------");
        for (int i = 0; i < lineNumbers.length; i++) {
            for (int j = 0; j < strParams.length; j++) {
                display("\tparameters: \"" + strParams[j][0] + "\", \""
                            + strParams[j][1] + "\", " + lineNumbers[i]);
                try {
                    list = testedClass.locationsOfLine(strParams[j][0],
                                                    strParams[j][1], lineNumbers[i]);
                    display("\tsize of locations list: " + list.size());
                    if (list.size() > 0) {
                        complain("\twrong size");
                        exitStatus = Consts.TEST_FAILED;
                    }
                } catch (Exception e) {
                    complain("\tUnexpected: " + e);
                    exitStatus = Consts.TEST_FAILED;
                }
                display("");
            }
        }

        display("invoking equals(Object):");
        display("----------------------");
        display("\tparameter: <null>");
        try {
            if (!testedClass.equals(null)) {
                display("\tis not equal to <null>");
            } else {
                complain("\tis equal to <null>");
                exitStatus = Consts.TEST_FAILED;
            }
        } catch (Exception e) {
            complain("\tUnexpected: " + e);
            exitStatus = Consts.TEST_FAILED;
        }
        display("");

        display("=============");
        display("TEST FINISHES\n");

        debugee.resume();
        debugee.quit();
    }
}
