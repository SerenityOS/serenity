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

package nsk.jdi.ClassType.newInstance;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;
import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

/**
 * The test checks that the JDI method
 * <code>com.sun.jdi.ClassType.newInstance()</code>
 * properly throws <i>IncompatibleThreadStateException</i> - if the
 * specified thread has not been suspended by an event.<p>
 *
 * The test works as follows. Debugger part of the test attempts
 * to construct a new instance of main debuggee class <i>newinstance009t</i>
 * using thread <i>newinstance009tThr</i> being previously suspended
 * by the JDI method <i>ThreadReference.suspend()</i> instead of by
 * an event. The exception is expected to be thrown.
 */
public class newinstance009 {
    static final String DEBUGGEE_CLASS =
        "nsk.jdi.ClassType.newInstance.newinstance009t";

    // name of debuggee main thread
    static final String DEBUGGEE_THRNAME = "newinstance009tThr";

    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT = "quit";

    static final int ATTEMPTS = 5;
    static final int DELAY = 500; // in milliseconds

    private ArgumentHandler argHandler;
    private Log log;
    private IOPipe pipe;
    private Debugee debuggee;
    private VirtualMachine vm;
    private int tot_res = Consts.TEST_PASSED;

    public static void main (String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new newinstance009().runIt(argv, out);
    }

    private int runIt(String args[], PrintStream out) {
        argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);
        int num = 0;

        debuggee = binder.bindToDebugee(DEBUGGEE_CLASS);
        pipe = debuggee.createIOPipe();
        vm = debuggee.VM();
        debuggee.redirectStderr(log, "newinstance009t.err> ");
        debuggee.resume();
        String cmd = pipe.readln();
        if (!cmd.equals(COMMAND_READY)) {
            log.complain("TEST BUG: unknown debuggee command: " + cmd);
            tot_res = Consts.TEST_FAILED;
            return quitDebuggee();
        }

        ThreadReference thrRef = null;
        if ((thrRef =
                debuggee.threadByName(DEBUGGEE_THRNAME)) == null) {
            log.complain("TEST FAILURE: method Debugee.threadByName() returned null for debuggee thread "
                + DEBUGGEE_THRNAME);
            tot_res = Consts.TEST_FAILED;
            return quitDebuggee();
        }
        thrRef.suspend();
        while(!thrRef.isSuspended()) {
            num++;
            if (num > ATTEMPTS) {
                log.complain("TEST FAILED: unable to suspend debuggee thread");
                tot_res = Consts.TEST_FAILED;
                return quitDebuggee();
            }
            log.display("Waiting for debuggee thread suspension ...");
            try {
                Thread.currentThread().sleep(DELAY);
            } catch(InterruptedException ie) {
                ie.printStackTrace();
                log.complain("TEST FAILED: caught: " + ie);
                tot_res = Consts.TEST_FAILED;
                return quitDebuggee();
            }
        }

        try {
            // debuggee main class
            ReferenceType rType = debuggee.classByName(DEBUGGEE_CLASS);
            ClassType clsType = (ClassType) rType;

            List methList = rType.methodsByName("<init>");
            if (methList.isEmpty()) {
                log.complain("TEST FAILURE: the expected constructor "
                    + " not found through the JDI method ReferenceType.methodsByName()");
                tot_res = Consts.TEST_FAILED;
                return quitDebuggee();
            }
            Method meth = (Method) methList.get(0);
            if (!meth.isConstructor()) {
                log.complain("TEST FAILURE: found method \""
                    + meth.name() + " " + meth.signature()
                    + "\" is not a constructor: Method.isConstructor()="
                    + meth.isConstructor());
                tot_res = Consts.TEST_FAILED;
                return quitDebuggee();
            }

            // Check the tested assersion
            try {
                log.display("\n Trying to construct a new instance of debuggee class \""
                    + clsType + "\"\n\tusing constructor \""
                    + meth.name() + " " + meth.signature()
                    + "\"\n\tgot from reference type \"" + rType
                    + "\" and thread which has not been suspended by an event ...");

                clsType.newInstance(thrRef, meth, Collections.<com.sun.jdi.Value>emptyList(), 0);

                log.complain("TEST FAILED: expected IncompatibleThreadStateException was not thrown"
                    + "\n\twhen attempted to construct a new instance of debuggee class \""
                    + clsType + "\"\n\tusing constructor \""
                    + meth.name() + " " + meth.signature()
                    + "\"\n\tgot from reference type \"" + rType
                    + "\" and thread which has not been suspended by an event");
                tot_res = Consts.TEST_FAILED;
            } catch (IncompatibleThreadStateException is) {
                log.display("CHECK PASSED: caught expected " + is);
            } catch (Exception ee) {
                ee.printStackTrace();
                log.complain("TEST FAILED: ClassType.newInstance(): caught unexpected "
                    + ee + "\n\tinstead of expected IncompatibleThreadStateException"
                    + "\n\twhen attempted to construct a new instance of debuggee class \""
                    + clsType + "\"\n\tusing constructor \""
                    + meth.name() + " " + meth.signature()
                    + "\"\n\tgot from reference type \"" + rType
                    + "\" and thread which has not been suspended by an event");
                tot_res = Consts.TEST_FAILED;
            }

        } catch (Exception e) {
            e.printStackTrace();
            log.complain("TEST FAILURE: caught unexpected exception: " + e);
            tot_res = Consts.TEST_FAILED;
        }

// Finish the test
        return quitDebuggee();
    }

    private int quitDebuggee() {
        log.display("\nFinal resumption of the debuggee VM");
        vm.resume();
        pipe.println(COMMAND_QUIT);
        debuggee.waitFor();
        int debStat = debuggee.getStatus();
        if (debStat != (Consts.JCK_STATUS_BASE + Consts.TEST_PASSED)) {
            log.complain("TEST FAILED: debuggee process finished with status: "
                + debStat);
            tot_res = Consts.TEST_FAILED;
        } else
            log.display("\nDebuggee process finished with the status: "
                + debStat);

        return tot_res;
    }
}
