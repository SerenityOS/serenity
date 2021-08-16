/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 *
 * @summary converted from VM Testbase nsk/jdi/ThreadReference/forceEarlyReturn/forceEarlyReturn013.
 * VM Testbase keywords: [quick, jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *         The test checks that a result of the method com.sun.jdi.forceEarlyReturn(Value value)
 *         complies with its specification. The test checks:
 *                 - after force return occurred any locks acquired by calling the called method(if it is a synchronized method)
 *                 and locks acquired by entering synchronized blocks within the called method are released in Debuggee VM.
 *                 - MethodExitEvent is generated as it would be in a normal return
 *         Test scenario:
 *         Special class 'TestThread' implementing 'Runnable' is used in debugee VM. TestThread has following 'run' method:
 *                 - it is synchronized method
 *                 - it contains 5 nested synchronized blocks for 5 different objects
 *         Debugger initializes breakpoint in TestThread's run method(inside the deepest synchronized block) and
 *         forces debugee VM create thread using instance of 'TestThread' and run this thread. When debuggee's test
 *         thread stops at breakpoint debugger calls forceEarlyReturn() and resumes debuggee VM.
 *         Then, debuggee creates new thread object using the same instance of 'TestThread' as for first test thread,
 *         starts new thread and wait when it finish execution. If first test thread released all locks
 *         new thread can acquire the same locks and finish, otherwise new thread will be blocked and will be unable to complete
 *         execution.
 *         Also debugger checks that MethodExitEvent is generated after forceEarlyReturn.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn013.forceEarlyReturn013
 *        nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn013.forceEarlyReturn013a
 * @run main/othervm
 *      nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn013.forceEarlyReturn013
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn013;

import java.io.PrintStream;
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import nsk.share.Consts;
import nsk.share.jdi.ForceEarlyReturnDebugger;

public class forceEarlyReturn013 extends ForceEarlyReturnDebugger {
    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public String debuggeeClassName() {
        return nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn013.forceEarlyReturn013a.class.getName();
    }

    public static int run(String argv[], PrintStream out) {
        return new forceEarlyReturn013().runIt(argv, out);
    }

    public void doTest() {
        Value voidValue = createVoidValue();

        // init breakpont in tested method
        ReferenceType referenceType = debuggee.classByName(TestThread.class.getName());

        BreakpointRequest breakpointRequest = debuggee.makeBreakpoint(referenceType, TestThread.BREAKPOINT_METHOD_NAME, TestThread.BREAKPOINT_LINE);

        breakpointRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        breakpointRequest.enable();

        // start test thread
        pipe.println(forceEarlyReturn013a.COMMAND_RUN_TEST_THREAD);

        BreakpointEvent event = waitForBreakpoint(breakpointRequest);

        breakpointRequest.disable();

        ThreadReference threadReference = event.thread();

        try {
            threadReference.forceEarlyReturn(voidValue);
        } catch (Exception e) {
            setSuccess(false);
            log.complain("Unexpected exception: " + e);
            e.printStackTrace(log.getOutStream());
        }

        testMethodExitEvent(threadReference, TestThread.BREAKPOINT_METHOD_NAME);

        if (!isDebuggeeReady())
            return;
    }
}
