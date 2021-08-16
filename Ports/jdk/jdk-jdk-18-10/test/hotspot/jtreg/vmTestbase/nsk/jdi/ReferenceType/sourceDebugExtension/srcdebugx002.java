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
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ObjectCollectedException;
import java.io.*;
import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


/**
 * The test checks that the JDI method
 * <code>ReferenceType.sourceDebugExtension()</code> properly returns
 * the SourceDebugExtension Class File attribute. String obtained from
 * the <code>sourceDebugExtension()</code> is compared with the expected
 * attribute string.
 */
public class srcdebugx002 {
    public static final int PASSED = 0;
    public static final int FAILED = 2;
    public static final int JCK_STATUS_BASE = 95;
    static final String DEBUGGEE_CLASS =
        "nsk.jdi.ReferenceType.sourceDebugExtension.srcdebugx002t";
    static final String SRCDEBUGX_CLASS =
        "nsk.jdi.ReferenceType.sourceDebugExtension.srcdebugx002x";
    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT = "quit";
    static final String SRCDEBUGXSTR = "Hello world!";

    private IOPipe pipe;
    private Debugee debuggee;

    public static void main (String argv[]) {
        System.exit(run(argv,System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new srcdebugx002().runIt(argv, out);
    }

    private int runIt(String args[], PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(args);
        Log log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);
        ReferenceType rType;
        String debugX = null;

        debuggee = binder.bindToDebugee(DEBUGGEE_CLASS);
        pipe = debuggee.createIOPipe();
        debuggee.redirectStderr(log, "srcdebugx002t.err> ");
        debuggee.resume();

        log.display("Waiting for debuggee readiness...");
        String cmd = pipe.readln();
        if (!cmd.equals(COMMAND_READY)) {
            log.complain("TEST BUG: unknown debuggee's command: " + cmd);
            return quitDebuggee(FAILED);
        }

        if ((rType = debuggee.classByName(SRCDEBUGX_CLASS)) == null) {
            log.complain("TEST FAILURE: Method Debugee.classByName() returned null");
            return quitDebuggee(FAILED);
        }

        if (!debuggee.VM().canGetSourceDebugExtension()) {
            log.display("TEST CANCELLED: VirtualMachine.canGetSourceDebugExtension() == false");
            return quitDebuggee(PASSED);
        }

        try {
            debugX = rType.sourceDebugExtension();
            log.display("Check #1 PASSED: successfully obtained the SourceDebugExtension attribute");
        } catch(AbsentInformationException e) {
            log.display("TEST FAILED: caught the exception: " + e);
            return quitDebuggee(FAILED);
        } catch (ObjectCollectedException e) {
            log.complain("TEST FAILED: caught the exception: " + e);
            return quitDebuggee(FAILED);
        }

        if (debugX.equals(SRCDEBUGXSTR)) {
            log.display("Check #2 PASSED: obtained the expected SourceDebugExtension attribute \""
                + debugX + "\"");
            return quitDebuggee(PASSED);
        } else {
            log.complain("TEST FAILED: the SourceDebugExtension attribute is \""
                + debugX + "\",\n\texpected: \"" + SRCDEBUGXSTR + "\"");
            return quitDebuggee(FAILED);
        }
    }

    private int quitDebuggee(int stat) {
        pipe.println(COMMAND_QUIT);
        debuggee.waitFor();
        return stat;
    }
}
