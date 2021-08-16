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

package nsk.jdi.ClassUnloadRequest._bounds_;

import nsk.share.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.request.*;

import java.io.*;

/**
 * The test checks up the                          <br>
 *  - <code>addClassFilter(ReferenceType)</code>,  <br>
 *  - <code>addClassFilter(String)</code>,         <br>
 *  - <code>addClassExclusionFilter(String)</code> <br>
 * methods with <code>null</code> argument value.
 * In any cases <code>NullPointerException</code> is expected.
 */
public class filters001 {

    final static String prefix = "nsk.jdi.ClassUnloadRequest._bounds_.";
    private final static String className = "filters001";
    private final static String debuggerName = prefix + className;
    private final static String debugeeName = debuggerName + "a";

    private static int exitStatus;
    private static Log log;
    private static Debugee debugee;

    private static void display(String msg) {
        log.display("debugger> " + msg);
    }

    private static void complain(String msg) {
        log.complain("debugger FAILURE> " + msg + "\n");
    }

    public static void main(String argv[]) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    public static int run(String argv[], PrintStream out) {

        exitStatus = Consts.TEST_PASSED;

        filters001 thisTest = new filters001();

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        debugee = Debugee.prepareDebugee(argHandler, log, debugeeName);

        thisTest.execTest();
        display("execTest finished. exitStatus = " + exitStatus);

        return exitStatus;
    }

    private void execTest() {

        ReferenceType debugeeRef = debugee.classByName(debugeeName);

        display("");
        display(">>>creating ClassUnloadRequest");
        ClassUnloadRequest request =
                debugee.getEventRequestManager().createClassUnloadRequest();

        display("");
        addClassFilter(request, (String )null);
        display("");
        addClassExclusionFilter(request, (String )null);

        display("");
        debugee.quit();
    }

    private void addClassFilter(ClassUnloadRequest request, String classPattern) {

        display("addClassFilter          :classPattern> null");
        try {
            request.addClassFilter(classPattern);
            complain("*****NullPointerException is not thrown");
            exitStatus = Consts.TEST_FAILED;
        } catch (NullPointerException e) {
            display("!!!Expected " + e);
        } catch (Exception e) {
            complain("*****Unexpected " + e);
            exitStatus = Consts.TEST_FAILED;
        }
    }

    private void addClassExclusionFilter(ClassUnloadRequest request,
                                                    String classPattern) {
        display("addExclusionClassFilter :classPattern> null");
        try {
            request.addClassExclusionFilter(classPattern);
            complain("*****NullPointerException is not thrown");
            exitStatus = Consts.TEST_FAILED;
        } catch (NullPointerException e) {
            display("!!!Expected " + e);
        } catch (Exception e) {
            complain("*****Unexpected " + e);
            exitStatus = Consts.TEST_FAILED;
        }
    }
}
