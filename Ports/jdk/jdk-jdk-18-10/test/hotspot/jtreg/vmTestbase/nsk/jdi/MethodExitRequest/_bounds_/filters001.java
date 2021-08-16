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

package nsk.jdi.MethodExitRequest._bounds_;

import nsk.share.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.request.*;

import java.io.*;

/**
 * The test checks up the                <br>
 *  - addThreadFilter(ThreadReference)   <br>
 *  - addInstanceFilter(ObjectReference) <br>
 *  - addClassFilter(ReferenceType)      <br>
 *  - addClassFilter(String)             <br>
 *  - addClassExclusionFilter(String)    <br>
 * methods with <code>null</code> argument value.
 * In any cases <code>NullPointerException</code> is expected.
 */
public class filters001 {

    private final static String prefix = "nsk.jdi.MethodExitRequest._bounds_.";
    private final static String debuggerName = prefix + "filters001";
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

        display("");
        display(">>>creating MethodExitRequest");

        MethodExitRequest request =
                    debugee.getEventRequestManager().createMethodExitRequest();

        display("");
        addThreadFilter(request, null);

        display("");
        addInstanceFilter(request, null);

        display("");
        addClassFilter(request, (ReferenceType )null);

        display("");
        addClassFilter(request, (String )null);

        display("");
        addClassExclusionFilter(request, (String )null);

        display("");
        debugee.quit();
    }

    private void addThreadFilter(MethodExitRequest request, ThreadReference thread) {
        String tmp = "addThreadFilter         :thread name> ";
        tmp += (thread == null) ? "<null>" : thread.name();
        display(tmp);

        try {
            request.addThreadFilter(thread);
            if (thread==null){
                complain("*****NullPointerException is not thrown");
                exitStatus = Consts.TEST_FAILED;
            }
        } catch (NullPointerException e) {
            if (thread == null){
                display("!!!Expected " + e);
            } else {
                complain("*****Unexpected " + e);
                exitStatus = Consts.TEST_FAILED;
            }
        } catch (Exception e) {
            complain("Unexpected " + e);
            exitStatus = Consts.TEST_FAILED;
        }
    }

    private void addInstanceFilter(MethodExitRequest request,
                                            ObjectReference instance) {
        String tmp = "addInstanceFilter       :object value> ";
        tmp += (instance == null) ? "<null>" : instance.toString();
        display(tmp);

        try {
            request.addInstanceFilter(instance);
            if (instance==null){
                complain("*****NullPointerException is not thrown");
                exitStatus = Consts.TEST_FAILED;
            }
        } catch (NullPointerException e) {
            if (instance == null){
                display("!!!Expected " + e);
            } else {
                complain("*****Unexpected " + e);
                exitStatus = Consts.TEST_FAILED;
            }
        } catch (Exception e) {
            complain("Unexpected " + e);
            exitStatus = Consts.TEST_FAILED;
        }
    }

    private void addClassFilter(MethodExitRequest request, ReferenceType refType) {

        display("addClassFilter          :ReferenceType> <" + refType + ">");

        try {
            request.addClassFilter(refType);
            if (refType==null){
                complain("*****NullPointerException is not thrown");
                exitStatus = Consts.TEST_FAILED;
            }
        } catch (NullPointerException e) {
            if (refType==null){
                display("!!!Expected " + e);
            } else {
                complain("*****Unexpected " + e);
                exitStatus = Consts.TEST_FAILED;
            }
        } catch (Exception e) {
            complain("*****Unexpected " + e);
            exitStatus = Consts.TEST_FAILED;
        }
    }

    private void addClassFilter(MethodExitRequest request, String classPattern) {

        display("addClassFilter          :classPattern> <" + classPattern + ">");
        try {
            request.addClassFilter(classPattern);
            if (classPattern==null){
                complain("*****NullPointerException is not thrown");
                exitStatus = Consts.TEST_FAILED;
            }
        } catch (NullPointerException e) {
            if (classPattern==null){
                display("!!!Expected " + e);
            } else {
                complain("*****Unexpected " + e);
                exitStatus = Consts.TEST_FAILED;
            }
        } catch (Exception e) {
            complain("*****Unexpected " + e);
            exitStatus = Consts.TEST_FAILED;
        }
    }

    private void addClassExclusionFilter(MethodExitRequest request,
                                                    String classPattern) {
        display("addExclusionClassFilter :classPattern> <" + classPattern + ">");
        try {
            request.addClassExclusionFilter(classPattern);
            if (classPattern==null){
                complain("*****NullPointerException is not thrown");
                exitStatus = Consts.TEST_FAILED;
            }
        } catch (NullPointerException e) {
            if (classPattern==null){
                display("!!!Expected " + e);
            } else {
                complain("*****Unexpected " + e);
                exitStatus = Consts.TEST_FAILED;
            }
        } catch (Exception e) {
            complain("*****Unexpected " + e);
            exitStatus = Consts.TEST_FAILED;
        }
    }

}
