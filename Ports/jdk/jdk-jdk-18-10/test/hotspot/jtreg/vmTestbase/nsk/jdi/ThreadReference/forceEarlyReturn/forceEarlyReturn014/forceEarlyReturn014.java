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
 * @summary converted from VM Testbase nsk/jdi/ThreadReference/forceEarlyReturn/forceEarlyReturn014.
 * VM Testbase keywords: [quick, jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *     Test scenario:
 *     Debuggee contains following test method:
 *     public int publicField1 = 1;
 *     public int publicField2 = 2;
 *     public int hotMethod()
 *     {
 *         return publicField1++ + publicField2 * 2;
 *     }
 *     Fisrt, debugger set breakpoint in test method, forces debuggee call this method and wait breakpoint event.
 *     When debuggee stop at breakpoint debugger call forceEarlyReturn() for suspended thread, checks that no
 *     any exceptions was thrown and resumes debuggee. Then, debugger disables breakpoint request and forces debuggee
 *     call test method 20000 times, after this test method should be compiled with JIT. Then, debugger enables
 *     breakpoint, forces debuggee call test method and when debuggee stop at breakpoint checks that behavior of
 *     forceEarlyReturn() didn't change.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn014.forceEarlyReturn014
 *        nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn014.forceEarlyReturn014a
 * @run main/othervm
 *      nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn014.forceEarlyReturn014
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn014;

import java.io.PrintStream;
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import nsk.share.Consts;
import nsk.share.jdi.ForceEarlyReturnDebugger;

public class forceEarlyReturn014 extends ForceEarlyReturnDebugger {
    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public String debuggeeClassName() {
        return nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn014.forceEarlyReturn014a.class.getName();
    }

    public static int run(String argv[], PrintStream out) {
        return new forceEarlyReturn014().runIt(argv, out);
    }

    private void testForceEarlyReturn(ReferenceType debuggeeClass, BreakpointRequest breakpointRequest) {
        EventListenerThread eventListeningThread = new EventListenerThread(breakpointRequest);
        eventListeningThread.start();
        eventListeningThread.waitStartListen();

        pipe.println(forceEarlyReturn014a.COMMAND_EXECUTE_HOT_METHOD);

        BreakpointEvent event = (BreakpointEvent) eventListeningThread.getEvent();

        Value valueToReturn = debuggeeClass.getValue(debuggeeClass.fieldByName("expectedHotMethodReturnValue"));

        try {
            event.thread().forceEarlyReturn(valueToReturn);
        } catch (Throwable t) {
            t.printStackTrace(log.getOutStream());
            setSuccess(false);
            log.complain("Unexpected exception: " + t);
        }

        testMethodExitEvent(event.thread(), forceEarlyReturn014a.breakpointMethodName);

        if (!isDebuggeeReady())
            return;
    }

    public void doTest() {
        ReferenceType debuggeeClass = debuggee.classByName(debuggeeClassName());

        BreakpointRequest breakpointRequest = debuggee.makeBreakpoint(debuggeeClass,
                forceEarlyReturn014a.breakpointMethodName,
                forceEarlyReturn014a.breakpointLine);

        breakpointRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        breakpointRequest.enable();

        // call forceEarlyReturn for hotMethod() first time
        testForceEarlyReturn(debuggeeClass, breakpointRequest);

        breakpointRequest.disable();

        // fire hotMethod(), it should be compiled
        pipe.println(forceEarlyReturn014a.COMMAND_FIRE_HOT_METHOD);

        if (!isDebuggeeReady())
            return;

        breakpointRequest.enable();

        // call forceEarlyReturn for hotMethod() second time
        testForceEarlyReturn(debuggeeClass, breakpointRequest);
    }
}
