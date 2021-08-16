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
 * @summary converted from VM Testbase nsk/jdi/ThreadReference/forceEarlyReturn/forceEarlyReturn005.
 * VM Testbase keywords: [quick, jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *         The test checks that a result of the method com.sun.jdi.forceEarlyReturn(Value value)
 *         complies with its specification. The test checks:
 *                 - after force return occurred any locks acquired by calling the called method(if it is a synchronized method)
 *                 and locks acquired by entering synchronized blocks within the called method are released in Debuggee VM.
 *                 Also checks that this does not apply to JNI locks or java.util.concurrent.locks locks.
 *                 - MethodExitEvent is generated as it would be in a normal return
 *         Test scenario:
 *         Special thread class 'TestThread' is used in debugee VM. This class contains following test method:
 *                 - it is synchronized method
 *                 - JNI lock is acquired in this method
 *                 - java.util.concurrent.locks.ReentrantLock is acquired in this method
 *                 - at the end the method contains synchronized block
 *         Debugger initialize breakpoint in test method(inside the synchronized block) and force debugee VM run test
 *         thread which call this method. When debuggee's test thread stop at breakpoint debugger call forceEarlyReturn()
 *         and resume debuggee VM.
 *         Debugee's test thread after force return check that locks acquired through synchronized method and synchronized block
 *         was     freed and JNI lock and java.util.concurrent.locks.ReentrantLock still locked.
 *         Also debugger checks that MethodExitEvent is generated after forceEarlyReturn.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn005.forceEarlyReturn005
 *        nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn005.forceEarlyReturn005a
 * @run main/othervm/native
 *      nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn005.forceEarlyReturn005
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn005;

import java.io.PrintStream;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequest;
import nsk.share.Consts;
import nsk.share.jdi.ForceEarlyReturnDebugger;

public class forceEarlyReturn005 extends ForceEarlyReturnDebugger {
    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public String debuggeeClassName() {
        return nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn005.forceEarlyReturn005a.class.getName();
    }

    public static int run(String argv[], PrintStream out) {
        return new forceEarlyReturn005().runIt(argv, out);
    }

    public void doTest() {
        // init breakpont in tested method
        ReferenceType referenceType = debuggee.classByName(
                nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn005.TestThread.class.getName());

        BreakpointRequest breakpointRequest = debuggee.makeBreakpoint(referenceType,
                TestThread.BREAKPOINT_METHOD_NAME,
                TestThread.BREAKPOINT_LINE);

        breakpointRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        breakpointRequest.enable();

        // start test thread
        pipe.println(forceEarlyReturn005a.COMMAND_RUN_TEST_THREAD);

        BreakpointEvent event = waitForBreakpoint(breakpointRequest);

        ThreadReference threadReference = event.thread();

        try {
            // call forceEarlyReturn, all asserts should be done in debuggee
            threadReference.forceEarlyReturn(vm.mirrorOf(0));
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
