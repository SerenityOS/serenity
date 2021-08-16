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

package nsk.jdi.ReferenceType.sourceDebugExtension;

import com.sun.jdi.ReferenceType;
import com.sun.jdi.ObjectCollectedException;
import com.sun.jdi.AbsentInformationException;
import java.io.*;
import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


/**
 * The test checks that the <code>SourceDebugExtension class file
 * attribute</code> can be obtained by the JDI method
 * <code>ReferenceType.sourceDebugExtension()</code>,
 * otherwise the method should throw the <code>AbsentInformationException</code>.
 */
public class srcdebugx001 {
    public static final int PASSED = 0;
    public static final int FAILED = 2;
    public static final int JCK_STATUS_BASE = 95;
    static final String DEBUGGEE_CLASS =
        "nsk.jdi.ReferenceType.sourceDebugExtension.srcdebugx001t";
    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT = "quit";

    private IOPipe pipe;
    private Debugee debuggee;

    public static void main (String argv[]) {
        System.exit(run(argv,System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new srcdebugx001().runIt(argv, out);
    }

    private int runIt(String args[], PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(args);
        Log log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);
        ReferenceType rType;

        debuggee = binder.bindToDebugee(DEBUGGEE_CLASS);
        pipe = debuggee.createIOPipe();
        debuggee.redirectStderr(log, "srcdebugx001t.err> ");
        debuggee.resume();

        log.display("Waiting for debuggee readiness...");
        String cmd = pipe.readln();
        if (!cmd.equals(COMMAND_READY)) {
            log.complain("TEST BUG: unknown debuggee's command: " + cmd);
            return quitDebuggee(FAILED);
        }

        if ((rType = debuggee.classByName(DEBUGGEE_CLASS)) == null) {
            log.complain("TEST FAILURE: Method Debugee.classByName() returned null");
            return quitDebuggee(FAILED);
        }

        if (!debuggee.VM().canGetSourceDebugExtension()) {

            log.display("          VirtualMachine.canGetSourceDebugExtension() == false");
            log.display("          UnsupportedOperationException is expected");
            try {
                String debugX = rType.sourceDebugExtension();
                log.complain("TEST FAILED: no UnsupportedOperationException thrown");
                return quitDebuggee(FAILED);
            } catch ( UnsupportedOperationException e1 ) {
                log.display("          UnsupportedOperationException thrown");
                return quitDebuggee(PASSED);
            } catch ( Exception e2) {
                log.complain("TEST FAILED: unexpected Exception thrown : " + e2);
                return quitDebuggee(FAILED);
            }

        } else {

            log.display("          VirtualMachine.canGetSourceDebugExtension() == true");
            log.display("          UnsupportedOperationException is not expected");

            try {
                String debugX = rType.sourceDebugExtension();
                log.display("TEST PASSED: successfully obtained the SourceDebugExtension attribute: \""
                    + debugX + "\"");
                return quitDebuggee(PASSED);
            } catch(AbsentInformationException e) {
                log.display("TEST PASSED: caught the expected exception: " + e);
                return quitDebuggee(PASSED);
            } catch (Exception e) {
                log.complain("TEST FAILED: caught " + e);
                return quitDebuggee(FAILED);
            }
        }
    }

    private int quitDebuggee(int stat) {
        pipe.println(COMMAND_QUIT);
        debuggee.waitFor();
        return stat;
    }
}
