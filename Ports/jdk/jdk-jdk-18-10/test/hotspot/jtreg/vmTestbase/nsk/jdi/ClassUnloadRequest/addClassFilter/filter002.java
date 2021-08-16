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

package nsk.jdi.ClassUnloadRequest.addClassFilter;

import nsk.share.*;
import nsk.share.jdi.*;

import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import java.io.*;

/**
 * Debugger checks up assertion: <br>
 *   <code>InvalidRequestStateException</code> - if this request is currently
 *   enabled or has been deleted. <br>
 * Debugger performs the following steps:<br>
 *  - creates a <code>ClassUnloadRequests</code> and enables it;<br>
 *  - invokes the method <code>addClassFilter()</code>.<br>
 *  It is expected, <code>InvalidRequestStateException</code> will be thrown;<br>
 *  - deletes this request and invokes the method <code>addClassFilter()</code><br>
 *  Once again it is expected, <code>InvalidRequestStateException</code> will be thrown;<br>
 */
public class filter002 {

    final static String prefix = "nsk.jdi.ClassUnloadRequest.addClassFilter.";
    private final static String className = "filter002";
    private final static String debuggerName = prefix + className;
    private final static String debugeeName = debuggerName + "a";

    private static int exitStatus;
    private static Log log;
    private static Debugee debugee;

    private static String pattern = prefix + "Sub*";

    private static void display(String msg) {
        log.display("debugger> " + msg);
    }

    private static void complain(String msg) {
        log.complain("debugger> " + msg + "\n");
    }

    public static void main(String argv[]) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    public static int run(String argv[], PrintStream out) {

        filter002 tstObj = new filter002();

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        debugee = Debugee.prepareDebugee(argHandler, log, debugeeName);
        tstObj.execTest();

        display("execTest finished. exitStatus = " + exitStatus);

        return tstObj.exitStatus;
    }

    private void execTest() {

        exitStatus = Consts.TEST_PASSED;

        EventRequestManager evm = debugee.getEventRequestManager();

        display(">>>creating ClassUnloadRequest");
        ClassUnloadRequest request = evm.createClassUnloadRequest();

        display("enabled request--------------------");
        display(">>>enabling of the created request");
        request.enable();
        addClassFilter(request);

        display("deleted request--------------------");
        display(">>>disabling of the created request");
        request.disable();
        display(">>>deleting of the created request");
        evm.deleteEventRequest(request);
        addClassFilter(request);

        debugee.quit();
    }

    private void addClassFilter(ClassUnloadRequest request) {
        display(">>>adding a class filter");
        display("");
        try {
            request.addClassFilter(pattern);
        } catch(InvalidRequestStateException e) {
            display(">>>>>EXPECTED InvalidRequestStateException");
        } catch(Exception e) {
            complain("******UNEXPECTED " + e);
            exitStatus = Consts.TEST_FAILED;
        }
        display("");
    }
}
