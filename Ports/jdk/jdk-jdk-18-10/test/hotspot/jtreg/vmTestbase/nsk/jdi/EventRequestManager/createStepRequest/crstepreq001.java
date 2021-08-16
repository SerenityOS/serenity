/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.EventRequestManager.createStepRequest;

import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.request.StepRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.DuplicateRequestException;
import com.sun.jdi.ObjectCollectedException;
import com.sun.jdi.VMMismatchException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.io.*;
import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


/**
 * The test checks that only one pending JDI step request is
 * allowed per thread, i.e. the JDI method
 * <code>com.sun.jdi.request.EventRequestManager.createStepRequest()</code>
 * properly throws a <code>DuplicateRequestException</code> if there
 * is already a pending step request for the specified thread.
 */
public class crstepreq001 {
    public static final int PASSED = 0;
    public static final int FAILED = 2;
    public static final int JCK_STATUS_BASE = 95;
    static final String DEBUGGEE_CLASS =
        "nsk.jdi.EventRequestManager.createStepRequest.crstepreq001t";
    static final String DEBUGGEE_THRD = "debuggee_thr";
    static final String COMMAND_READY = "ready";
    static final String COMMAND_QUIT = "quit";

    static final int RSTS_NUM = 6;
    static final int RESTRICTIONS[][] = {
        {StepRequest.STEP_MIN,  StepRequest.STEP_INTO},
        {StepRequest.STEP_MIN,  StepRequest.STEP_OVER},
        {StepRequest.STEP_MIN,  StepRequest.STEP_OUT},
        {StepRequest.STEP_LINE, StepRequest.STEP_INTO},
        {StepRequest.STEP_LINE, StepRequest.STEP_OVER},
        {StepRequest.STEP_LINE, StepRequest.STEP_OUT}
    };

    private Log log;
    private IOPipe pipe;
    private Debugee debuggee;
    private int tot_res = PASSED;

    public static void main (String argv[]) {
        System.exit(run(argv,System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new crstepreq001().runIt(argv, out);
    }

    private int runIt(String args[], PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        Binder binder = new Binder(argHandler, log);
        ThreadReference thR = null;
        List threads;
        List<StepRequest> enabledStepRequests = new LinkedList<>();
        String cmd;

        debuggee = binder.bindToDebugee(DEBUGGEE_CLASS);
        pipe = debuggee.createIOPipe();
        debuggee.redirectStderr(log, "crstepreq001t.err> ");
        VirtualMachine vm = debuggee.VM();
        EventRequestManager erManager = vm.eventRequestManager();
        debuggee.resume();
        cmd = pipe.readln();
        if (!cmd.equals(COMMAND_READY)) {
            log.complain("TEST BUG: unknown debuggee's command: "
                + cmd);
            return quitDebuggee(FAILED);
        }

        try {
            threads = vm.allThreads();
        } catch (Exception e) {
            log.complain("TEST FAILURE: allThreads: " + e);
            return quitDebuggee(FAILED);
        }
        Iterator iter = threads.iterator();
        while (iter.hasNext()) {
            thR = (ThreadReference) iter.next();
            if (thR.name().equals(DEBUGGEE_THRD)) {
                log.display("\nCreating StepRequest for the debuggee's thread \""
                    + thR.name() + "\"");
                try {
                    StepRequest sReq = erManager.createStepRequest(thR,
                        RESTRICTIONS[0][0],RESTRICTIONS[0][1]);
                    sReq.enable();
                    enabledStepRequests.add(sReq);
                } catch (DuplicateRequestException e) {
                    log.complain("TEST FAILURE: createStepRequest: caught " + e);
                    return quitDebuggee(FAILED);
                } catch (ObjectCollectedException e) {
                    log.complain("TEST FAILURE: createStepRequest: caught " + e);
                    return quitDebuggee(FAILED);
                } catch (VMMismatchException e) {
                    log.complain("TEST FAILURE: createStepRequest: caught " + e);
                    return quitDebuggee(FAILED);
                }
                break;
            }
        }

// Check that createStepRequest() throws DuplicateRequestException
// to indicate a duplicate event request per thread
        for(int i=0; i<RSTS_NUM; i++) {
            log.display("\n" + (i+1)
                + ") Trying to create a duplicate StepRequest object\n\twith size="
                + RESTRICTIONS[i][0] + "; depth="
                + RESTRICTIONS[i][1] + " for the debuggee's thread \""
                + thR.name() + "\"");
            try {
                StepRequest sReq = erManager.createStepRequest(thR,
                    RESTRICTIONS[i][0], RESTRICTIONS[i][1]);
                log.complain("TEST CASE #" + (i+1)
                    + " FAILED: createStepRequest successfully done\n"
                    + "\tfor a duplicate StepRequest object per the specified thread \""
                    + thR.name() + "\"\n"
                    + "\tbut it should throw DuplicateRequestException");
                tot_res = FAILED;
            } catch (DuplicateRequestException e) {
                log.display("TEST CASE #" + (i+1)
                    + " PASSED: createStepRequest: caught " + e);
            } catch (ObjectCollectedException e) {
                log.complain("TEST CASE #" + (i+1)
                    + " FAILED: createStepRequest: caught " + e);
                log.complain("\tbut it should throw DuplicateRequestException");
                tot_res = FAILED;
            } catch (VMMismatchException e) {
                log.complain("TEST CASE #" + (i+1)
                    + " FAILED: createStepRequest: caught " + e);
                log.complain("\tbut it should throw DuplicateRequestException");
                tot_res = FAILED;
            }
        }

        enabledStepRequests.forEach(s -> erManager.deleteEventRequest(s));
        // There is a chance that a single step event had been posted after
        // the step request was created and before it was deleted. In this
        // case the debuggee VM is in the suspended state.
        vm.resume();
        return quitDebuggee(tot_res);
    }

    private int quitDebuggee(int stat) {
        pipe.println(COMMAND_QUIT);
        debuggee.waitFor();
        return stat;
    }
}
