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

package nsk.jdi.ConstantField;

import nsk.share.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.request.*;

import java.io.*;
import java.util.*;

/**
 *   Test checks up static fields of JDI interfaces according to specification
 */
public class values001 {

    private static int exitStatus;
    private static Log log;

    public static void main(String argv[]) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    public static int run(String argv[], PrintStream out) {

        exitStatus = Consts.TEST_PASSED;

        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        execTest();

        log.display("execTest finished. exitStatus = " + exitStatus);

        return exitStatus;
    }

    private static void execTest() {
        exitStatus = Consts.TEST_PASSED;

        if (ClassType.INVOKE_SINGLE_THREADED != 1) {
            log.complain("***wrong value*** : ClassType.INVOKE_SINGLE_THREADED = "
                                + ClassType.INVOKE_SINGLE_THREADED);
            exitStatus = Consts.TEST_FAILED;
        }

        if (ObjectReference.INVOKE_NONVIRTUAL != 2) {
            log.complain("***wrong value*** : ObjectReference.INVOKE_NONVIRTUAL = "
                                + ObjectReference.INVOKE_NONVIRTUAL);
            exitStatus = Consts.TEST_FAILED;
        }

        if (ObjectReference.INVOKE_SINGLE_THREADED != 1) {
            log.complain("***wrong value*** : ObjectReference.INVOKE_SINGLE_THREADED = "
                                + ObjectReference.INVOKE_SINGLE_THREADED);
            exitStatus = Consts.TEST_FAILED;
        }

        if (ThreadReference.THREAD_STATUS_MONITOR != 3) {
            log.complain("***wrong value*** : ThreadReference.THREAD_STATUS_MONITOR = "
                                + ThreadReference.THREAD_STATUS_MONITOR);
            exitStatus = Consts.TEST_FAILED;
        }

        if (ThreadReference.THREAD_STATUS_NOT_STARTED != 5) {
            log.complain("***wrong value*** : ThreadReference.THREAD_STATUS_NOT_STARTED = "
                                + ThreadReference.THREAD_STATUS_NOT_STARTED);
            exitStatus = Consts.TEST_FAILED;
        }

        if (ThreadReference.THREAD_STATUS_RUNNING != 1) {
            log.complain("***wrong value*** : ThreadReference.THREAD_STATUS_RUNNING = "
                                + ThreadReference.THREAD_STATUS_RUNNING);
            exitStatus = Consts.TEST_FAILED;
        }

        if (ThreadReference.THREAD_STATUS_SLEEPING != 2) {
            log.complain("***wrong value*** : ThreadReference.THREAD_STATUS_SLEEPING = "
                                + ThreadReference.THREAD_STATUS_SLEEPING);
            exitStatus = Consts.TEST_FAILED;
        }

        if (ThreadReference.THREAD_STATUS_UNKNOWN != -1) {
            log.complain("***wrong value*** : ThreadReference.THREAD_STATUS_UNKNOWN = "
                                + ThreadReference.THREAD_STATUS_UNKNOWN);
            exitStatus = Consts.TEST_FAILED;
        }

        if (ThreadReference.THREAD_STATUS_WAIT != 4) {
            log.complain("***wrong value*** : ThreadReference.THREAD_STATUS_WAIT = "
                                + ThreadReference.THREAD_STATUS_WAIT);
            exitStatus = Consts.TEST_FAILED;
        }

        if (ThreadReference.THREAD_STATUS_ZOMBIE != 0) {
            log.complain("***wrong value*** : ThreadReference.THREAD_STATUS_ZOMBIE = "
                                + ThreadReference.THREAD_STATUS_ZOMBIE);
            exitStatus = Consts.TEST_FAILED;
        }


        // VirtualMachine
        if (VirtualMachine.TRACE_ALL != 16777215) {
            log.complain("***wrong value*** : VirtualMachine.TRACE_ALL = "
                                + VirtualMachine.TRACE_ALL);
            exitStatus = Consts.TEST_FAILED;
        }

        if (VirtualMachine.TRACE_EVENTS != 4) {
            log.complain("***wrong value*** : VirtualMachine.TRACE_EVENTS = "
                                + VirtualMachine.TRACE_EVENTS);
            exitStatus = Consts.TEST_FAILED;
        }

        if (VirtualMachine.TRACE_NONE != 0) {
            log.complain("***wrong value*** : VirtualMachine.TRACE_NONE = "
                                + VirtualMachine.TRACE_NONE);
            exitStatus = Consts.TEST_FAILED;
        }

        if (VirtualMachine.TRACE_OBJREFS != 16) {
            log.complain("***wrong value*** : VirtualMachine.TRACE_OBJREFS = "
                                + VirtualMachine.TRACE_OBJREFS);
            exitStatus = Consts.TEST_FAILED;
        }

        if (VirtualMachine.TRACE_RECEIVES != 2) {
            log.complain("***wrong value*** : VirtualMachine.TRACE_RECEIVES = "
                                + VirtualMachine.TRACE_RECEIVES);
            exitStatus = Consts.TEST_FAILED;
        }

        if (VirtualMachine.TRACE_REFTYPES != 8) {
            log.complain("***wrong value*** : VirtualMachine.TRACE_REFTYPES = "
                                + VirtualMachine.TRACE_REFTYPES);
            exitStatus = Consts.TEST_FAILED;
        }

        if (VirtualMachine.TRACE_SENDS != 1) {
            log.complain("***wrong value*** : VirtualMachine.TRACE_SENDS = "
                                + VirtualMachine.TRACE_SENDS);
            exitStatus = Consts.TEST_FAILED;
        }


        if (EventRequest.SUSPEND_ALL != 2) {
            log.complain("***wrong value*** : EventRequest.SUSPEND_ALL = "
                                + EventRequest.SUSPEND_ALL);
            exitStatus = Consts.TEST_FAILED;
        }

        if (EventRequest.SUSPEND_EVENT_THREAD != 1) {
            log.complain("***wrong value*** : EventRequest.SUSPEND_EVENT_THREAD = "
                                + EventRequest.SUSPEND_EVENT_THREAD);
            exitStatus = Consts.TEST_FAILED;
        }

        if (EventRequest.SUSPEND_NONE != 0) {
            log.complain("***wrong value*** : EventRequest.SUSPEND_NONE = "
                                + EventRequest.SUSPEND_NONE);
            exitStatus = Consts.TEST_FAILED;
        }


        if (StepRequest.STEP_INTO != 1) {
            log.complain("***wrong value*** : StepRequest.STEP_INTO = "
                                + StepRequest.STEP_INTO);
            exitStatus = Consts.TEST_FAILED;
        }

        if (StepRequest.STEP_LINE != -2) {
            log.complain("***wrong value*** : StepRequest.STEP_LINE = "
                                + StepRequest.STEP_LINE);
            exitStatus = Consts.TEST_FAILED;
        }

        if (StepRequest.STEP_MIN != -1) {
            log.complain("***wrong value*** : StepRequest.STEP_MIN = "
                                + StepRequest.STEP_MIN);
            exitStatus = Consts.TEST_FAILED;
        }

        if (StepRequest.STEP_OUT != 3) {
            log.complain("***wrong value*** : StepRequest.STEP_OUT = "
                                + StepRequest.STEP_OUT);
            exitStatus = Consts.TEST_FAILED;
        }

        if (StepRequest.STEP_OVER != 2) {
            log.complain("***wrong value*** : StepRequest.STEP_OVER = "
                                + StepRequest.STEP_OVER);
            exitStatus = Consts.TEST_FAILED;
        }
        log.display("");
    }

}
