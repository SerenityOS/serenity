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
 * @modules java.base/jdk.internal.misc:+open
 *
 * @summary converted from VM Testbase nsk/jdi/ThreadReference/forceEarlyReturn/forceEarlyReturn008.
 * VM Testbase keywords: [quick, jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *         The test checks that a result of the method com.sun.jdi.forceEarlyReturn(Value value)
 *         complies with its specification. The test checks:
 *                 - after force return no extra StepEvent events are generated
 *                 - MethodExitEvent is generated as it would be in a normal return
 *         Test scenario:
 *         Following thread class is used in debuggee VM:
 * 1       class TestThread
 * 2               extends Thread
 * 3       {
 * 4               public void run()
 * 5               {
 * 6                       methodForEarlyReturn();
 * 7               }
 * 8
 * 9               public int methodForEarlyReturn()
 * 10              {
 * 11                      // next line for breakpoint
 * 12                      int i = 0;
 * 13
 * 14                      // dummy code, test thread shouldn't execute this code and StepEvents shouldn't be generated for this code
 * 15                      for(i = 0; i < 100; i++)
 * 16                      {
 * 17                              int j = 0;
 * 18                              j = j + i;
 * 19                      }
 * 20
 * 21                      return 0;
 * 22              }
 * 23      }
 *         Debugger set breakpoint in TestThread.methodForEarlyReturn() at line marked in this description as 12 and force debugee
 *         start this thread.
 *         When test thread stop at breakpoint debuggeer initializes StepRequest for this thread, starts event listener thread
 *         which counting StepEvents, call forceEarlyReturn for test thread and resume debuggee VM.
 *         Then, debugger waits when debuggee's test thread finish execution and check that number of received step events
 *         is equal 2:
 *                 - 1st step: thread exit from methodForEarlyReturn()
 *                 - 2ns step: thread exit from run()
 *         Also debugger checks that MethodExitEvent is generated after forceEarlyReturn.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn008.forceEarlyReturn008
 *        nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn008.forceEarlyReturn008a
 * @run main/othervm
 *      nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn008.forceEarlyReturn008
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn008;

import java.io.PrintStream;
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import nsk.share.Consts;
import nsk.share.jdi.EventHandler;
import nsk.share.jdi.ForceEarlyReturnDebugger;

public class forceEarlyReturn008 extends ForceEarlyReturnDebugger {
    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public String debuggeeClassName() {
        return nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn008.forceEarlyReturn008a.class.getName();
    }

    public static int run(String argv[], PrintStream out) {
        return new forceEarlyReturn008().runIt(argv, out);
    }

    // event listener which counts StepEvents and checks is MethodExitEvent was generated
    public class EventsListener extends EventHandler.EventListener {
        int totalStepEvents;

        boolean methodExitEventReceived;

        public boolean eventReceived(Event event) {
            if (event instanceof StepEvent) {
                log.display(++totalStepEvents + " " + ((StepEvent) event));

                if (!(((StepEvent) event).location().method().name().equals("run"))) {
                    setSuccess(false);
                    log.complain("Event was generated for unexpected method: " + ((StepEvent) event).location().method());
                }

                vm.resume();

                return true;
            }
            if (event instanceof MethodExitEvent) {
                if (((MethodExitEvent) event).method().name().equals(forceEarlyReturn008a.breakpointMethod)) {
                    methodExitEventReceived = true;
                }

                return true;
            }

            return false;
        }
    }

    public void doTest() {
        // set breakpoint in TestThread.intMethod
        BreakpointRequest breakpointRequest;

        breakpointRequest = debuggee.makeBreakpoint(debuggee.classByName(forceEarlyReturn008a.TestThread.class.getName()),
                forceEarlyReturn008a.breakpointMethod, forceEarlyReturn008a.breakpointLine);

        breakpointRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        breakpointRequest.enable();

        // force debuggee start TestThread
        pipe.println(forceEarlyReturn008a.COMMAND_START_TEST_THREAD);

        waitForBreakpoint(breakpointRequest);

        ThreadReference threadReference = debuggee.threadByName(forceEarlyReturn008a.testThreadName);

        // initialize step request for TestThread
        StepRequest stepRequest = debuggee.getEventRequestManager().createStepRequest(threadReference, StepRequest.STEP_LINE, StepRequest.STEP_INTO);
        stepRequest.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        stepRequest.addClassFilter(debuggee.classByName(forceEarlyReturn008a.TestThread.class.getName()));
        stepRequest.enable();

        EventHandler eventHandler = new EventHandler(debuggee, log);
        eventHandler.startListening();

        // add event listener
        EventsListener eventListener = new EventsListener();
        eventHandler.addListener(eventListener);

        // create MethodExitRequest for tested thread
        MethodExitRequest methodExitRequest;
        methodExitRequest = debuggee.getEventRequestManager().createMethodExitRequest();
        methodExitRequest.addThreadFilter(threadReference);
        methodExitRequest.setSuspendPolicy(EventRequest.SUSPEND_NONE);
        methodExitRequest.enable();

        try {
            threadReference.forceEarlyReturn(vm.mirrorOf(0));
        } catch (Throwable t) {
            setSuccess(false);
            t.printStackTrace(log.getOutStream());
            log.complain("Unexpected exception: " + t);
        }

        vm.resume();

        // wait READY signal for previous command 'forceEarlyReturn008a.COMMAND_START_TEST_THREAD'
        if (!isDebuggeeReady())
            return;

        pipe.println(forceEarlyReturn008a.COMMAND_JOIN_TEST_THREAD);

        if (!isDebuggeeReady())
            return;

        // expect only 2 step events:
        // - exit from methodForEarlyReturn()
        // - exit from run()
        if (eventListener.totalStepEvents != 2) {
            setSuccess(false);
            log.complain("Unexpected number of step events: " + eventListener + ", only 2 is expected");
        }

        if (!eventListener.methodExitEventReceived) {
            setSuccess(false);
            log.complain("MethodExit event wasn't generated");
        }

        eventHandler.stopEventHandler();
    }
}
