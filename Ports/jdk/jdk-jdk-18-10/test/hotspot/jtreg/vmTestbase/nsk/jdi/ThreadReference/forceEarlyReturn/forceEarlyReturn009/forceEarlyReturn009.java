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
 * @summary converted from VM Testbase nsk/jdi/ThreadReference/forceEarlyReturn/forceEarlyReturn009.
 * VM Testbase keywords: [quick, jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *         The test checks that a result of the method com.sun.jdi.forceEarlyReturn(Value value)
 *         complies with its specification. The test checks:
 *                 - forcing return on a thread with only one frame on the stack causes the thread to exit when resumed
 *                 - MethodExitEvent is generated as it would be in a normal return
 *         Test scenario:
 *         Debugger forces debuggee start test thread which executes infinite loop in it's run() method, so it always has
 *         only one frame on stack. Debugger suspends debuggee VM, call forceEarlyReturn() for test thread, resumes debuggee
 *         VM and checks that test thread has status ThreadReference.THREAD_STATUS_ZOMBIE(thread has completed execution) and
 *         checks that ThreadDeathEvent and MethodExitEvent was generated for test thread.
 *         Used in this test classes can be used to check forceEarlyReturn() behavior when
 *         thread has single frame on the stack because of methods which this thread calls were got inlined,
 *         in this case thread in it's run() in infinite loop calls methods which should be inlined.
 *         To check this case run test with parameter -inlineType <INLINE_TYPE>, where <INLINE_TYPE> is one of:
 *                 - INLINE_METHOD_RETURNING_CONST, thread calls methods which return const values
 *                 - INLINE_METHOD_ACCESSIN_INTERNAL_FIELDS, thread calls methods which access only internal fields
 *                 - INLINE_HOT_METHOD, thread calls method which should be executed a certain number of times before it is compiled
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn009.forceEarlyReturn009
 *        nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn009.forceEarlyReturn009a
 * @run main/othervm
 *      nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn009.forceEarlyReturn009
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn009;

import java.io.PrintStream;
import java.util.ArrayList;
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import nsk.share.Consts;
import nsk.share.jdi.ForceEarlyReturnDebugger;

public class forceEarlyReturn009 extends ForceEarlyReturnDebugger {
    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public String debuggeeClassName() {
        return forceEarlyReturn009a.class.getName();
    }

    public static int run(String argv[], PrintStream out) {
        return new forceEarlyReturn009().runIt(argv, out);
    }

    private String inlineType;

    protected String[] doInit(String args[], PrintStream out) {
        ArrayList<String> standardArgs = new ArrayList<String>();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-inlineType") && (i < args.length - 1)) {
                inlineType = args[i + 1];
                i++;
            } else
                standardArgs.add(args[i]);
        }

        return standardArgs.toArray(new String[] {});
    }

    public void doTest() {
        if (inlineType != null)
            pipe.println(forceEarlyReturn009a.COMMAND_RUN_THREAD_WITH_SINGLE_FRAME + ":" + inlineType);
        else
            pipe.println(forceEarlyReturn009a.COMMAND_RUN_THREAD_WITH_SINGLE_FRAME);

        if (!isDebuggeeReady())
            return;

        // this thread has one frame on stack
        ThreadReference threadReference = debuggee.threadByName(forceEarlyReturn009a.testThreadWithSingleFrameName);

        Value voidValue = createVoidValue();

        vm.suspend();

        try {
            // thread should exit when resumed
            threadReference.forceEarlyReturn(voidValue);
        } catch (InvalidTypeException e) {
            // this exception could be thrown if inlining was not done, in this case stop test execution
            if (inlineType != null) {
                log.display("WARNING: InvalidTypeException was caught, possible inlining was not done, stop test execution");
                vm.resume();
                pipe.println(forceEarlyReturn009a.COMMAND_JOIN_THREAD_WITH_SINGLE_FRAME);
                isDebuggeeReady();
                return;
            } else {
                    setSuccess(false);
                    log.complain("Unexpected exception: " + e);
                    e.printStackTrace(log.getOutStream());
            }
        } catch (Throwable e) {
            setSuccess(false);
            log.complain("Unexpected exception: " + e);
            e.printStackTrace(log.getOutStream());
        }

        // create request for ThreadDeathEvent
        ThreadDeathRequest threadDeathRequest;
        threadDeathRequest = debuggee.getEventRequestManager().createThreadDeathRequest();
        threadDeathRequest.addThreadFilter(threadReference);
        threadDeathRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        threadDeathRequest.enable();

        testMethodExitEvent(threadReference, "run", false);

        vm.resume();

        EventListenerThread threadDeathEventListeningThread = new EventListenerThread(threadDeathRequest);
        threadDeathEventListeningThread.start();
        threadDeathEventListeningThread.waitStartListen();

        Event event = threadDeathEventListeningThread.getEvent();

        if (event == null) {
            setSuccess(false);
            log.complain("ThreadDeath event wasn't generated");
        }

        vm.resume();

        pipe.println(forceEarlyReturn009a.COMMAND_JOIN_THREAD_WITH_SINGLE_FRAME);

        if (!isDebuggeeReady())
            return;

        // check thread status
        int threadStatus = threadReference.status();

        if (threadStatus != ThreadReference.THREAD_STATUS_ZOMBIE) {
            setSuccess(false);
            log.complain("Unexpected status of test thread: " + threadStatus + ", expected is 'THREAD_STATUS_ZOMBIE'("
                    + ThreadReference.THREAD_STATUS_ZOMBIE + ")");
        }

    }
}
