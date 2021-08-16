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

package nsk.jdi.ThreadReference.ownedMonitors;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;
import java.io.*;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

/**
 * The test checks that the JDI method:<br>
 * <code>com.sun.jdi.ThreadReference.ownedMonitors()</code><br>
 * properly throws <i>UnsupportedOperationException</i>, if
 * the target VM does not support the retrieval of the monitor for
 * which a thread is currently waiting (determinated by calling
 * <i>VirtualMachine.canGetOwnedMonitorInfo()</i>), and vise versa
 * otherwise.
 */
public class ownedmonitors002 {
    static final String DEBUGGEE_CLASS =
        "nsk.jdi.ThreadReference.ownedMonitors.ownedmonitors002t";

    // name of debuggee's main thread
    static final String DEBUGGEE_THRNAME = "ownedmonitors002tThr";

    static final int ATTEMPTS = 5;
    static final int DELAY = 500; // in milliseconds

    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT = "quit";

    private ArgumentHandler argHandler;
    private Log log;
    private IOPipe pipe;
    private Debugee debuggee;
    private VirtualMachine vm;
    private int tot_res = Consts.TEST_PASSED;

    public static void main (String argv[]) {
        System.exit(run(argv,System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new ownedmonitors002().runIt(argv, out);
    }

    private int runIt(String args[], PrintStream out) {
        argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);

        debuggee = binder.bindToDebugee(DEBUGGEE_CLASS);
        pipe = debuggee.createIOPipe();
        vm = debuggee.VM();
        debuggee.redirectStderr(log, "ownedmonitors002t.err> ");
        debuggee.resume();
        String cmd = pipe.readln();
        if (!cmd.equals(COMMAND_READY)) {
            log.complain("TEST BUG: unknown debuggee command: " + cmd);
            tot_res = Consts.TEST_FAILED;
            return quitDebuggee();
        }

        ThreadReference thrRef;
        if ((thrRef =
                debuggee.threadByName(DEBUGGEE_THRNAME)) == null) {
            log.complain("TEST FAILURE: method Debugee.threadByName() returned null for debuggee thread "
                + DEBUGGEE_THRNAME);
            tot_res = Consts.TEST_FAILED;
            return quitDebuggee();
        }

        int num = 0;
        thrRef.suspend();
        while(!thrRef.isSuspended()) {
            num++;
            if (num > ATTEMPTS) {
                log.complain("TEST FAILURE: Unable to suspend debuggee thread after "
                    + ATTEMPTS + " attempts");
                tot_res = Consts.TEST_FAILED;
                return quitDebuggee();
            }
            log.display("Waiting for debuggee thread suspension ...");
            try {
                Thread.currentThread().sleep(DELAY);
            } catch(InterruptedException ie) {
                ie.printStackTrace();
                log.complain("TEST FAILURE: caught: " + ie);
                tot_res = Consts.TEST_FAILED;
                return quitDebuggee();
            }
        }

// Check the tested assersion
        try {
            List mons = thrRef.ownedMonitors();
            if (vm.canGetOwnedMonitorInfo()) {
                log.display("CHECK PASSED: got a List of monitors owned by the thread,"
                    + "\n\tand VirtualMachine.canGetOwnedMonitorInfo() shows, that the target VM"
                    + "supports the retrieval of the monitors owned by a thread as well: "
                    + vm.canGetOwnedMonitorInfo());
            } else {
                log.complain("TEST FAILED: got a List of monitors owned by the thread,"
                    + "\n\thowever, VirtualMachine.canGetOwnedMonitorInfo() shows, that the target VM"
                    + "\n\tdoes not support the retrieval of the monitors owned by a thread: "
                    + vm.canGetOwnedMonitorInfo());
                tot_res = Consts.TEST_FAILED;
            }
        } catch(UnsupportedOperationException une) {
            if (vm.canGetOwnedMonitorInfo()) {
                une.printStackTrace();
                log.complain("TEST FAILED: caught exception: " + une
                    + "\n\tHowever, VirtualMachine.canGetOwnedMonitorInfo() shows, that the target VM"
                    + "\n\tsupports the retrieval of the monitors owned by a thread: "
                    + vm.canGetOwnedMonitorInfo());
                tot_res = Consts.TEST_FAILED;
            } else {
                log.display("CHECK PASSED: caught expected exception: " + une
                    + "\n\tand VirtualMachine.canGetOwnedMonitorInfo() shows, that the target VM"
                    + "\n\tdoes not support the retrieval of the monitors owned by a thread as well: "
                    + vm.canGetOwnedMonitorInfo());
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.complain("TEST FAILED: caught unexpected exception: " + e);
            tot_res = Consts.TEST_FAILED;
        }

// Finish the test
        return quitDebuggee();
    }

    private int quitDebuggee() {
        log.display("Resuming debuggee ...");
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
