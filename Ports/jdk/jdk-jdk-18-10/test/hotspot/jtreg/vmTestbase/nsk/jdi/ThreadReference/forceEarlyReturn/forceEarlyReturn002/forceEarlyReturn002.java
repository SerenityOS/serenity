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
 * @summary converted from VM Testbase nsk/jdi/ThreadReference/forceEarlyReturn/forceEarlyReturn002.
 * VM Testbase keywords: [quick, jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *         The test checks that a result of the method com.sun.jdi.forceEarlyReturn(Value value)
 *         complies with its specification. The test checks:
 *                 - attempt to call forceEarlyReturn for the type which has not yet been loaded throws ClassNotLoadedException
 *                 - MethodExitEvent is generated as it would be in a normal return
 *         Test scenario:
 *         Debugger VM enable breakpoint in test method which return type is 'nsk.share.jdi.TestClass1',
 *         force debugee call this method and when debugee VM stop at breakpoint, call forceEarlyReturn().
 *         ClassNotLoadedException should be thrown (expect that nsk.share.jdi.TestClass1 isn't loaded in debuggee VM).
 *         Debugger VM force debuggee VM create instance of 'nsk.share.jdi.TestClass1' and call test method again.
 *         When debugee VM stop at breakpoint, call forceEarlyReturn() and check that no exception is thrown.
 *         Debugee checks that correct value is returned from test method after force return.
 *         Debugger checks that MethodExitEvent is generated after forceEarlyReturn.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn002.forceEarlyReturn002
 *        nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn002.forceEarlyReturn002a
 * @run main/othervm
 *      nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn002.forceEarlyReturn002
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn002;

import java.io.PrintStream;
import com.sun.jdi.*;
import com.sun.jdi.request.*;
import com.sun.jdi.event.*;
import nsk.share.Consts;
import nsk.share.jdi.ForceEarlyReturnDebugger;

public class forceEarlyReturn002 extends ForceEarlyReturnDebugger {
    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public String debuggeeClassName() {
        return forceEarlyReturn002a.class.getName();
    }

    public static int run(String argv[], PrintStream out) {
        return new forceEarlyReturn002().runIt(argv, out);
    }

    public void doTest() {
        // initialize breakpoint

        ReferenceType referenceType = debuggee.classByName(ClassUsingTestClass.class.getName());

        BreakpointRequest breakpointRequest = debuggee.makeBreakpoint(referenceType,
                ClassUsingTestClass.breakpointMethodName,
                ClassUsingTestClass.breakpointLine);

        breakpointRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        breakpointRequest.enable();

        pipe.println(forceEarlyReturn002a.COMMAND_CALL_OBJECT_METHOD);

        BreakpointEvent breakPointEvent = waitForBreakpoint(breakpointRequest);

        // if no breakpoint happened then test failed, stop testing
        if (breakPointEvent == null)
            return;

        /*
         * Test can't guarantee that TestClass1 isn't loaded in the debuggee VM (it isn't loaded
         * if VM implements lazy loading). Here test checks that TestClass1 isn't loaded and
         * if class is really absent in the debuggee VM it is possible to check ClassNotLoadedException.
         */
        boolean testClassIsLoaded = false;

        ClassLoaderReference classLoader = debuggee.classByName(forceEarlyReturn002a.class.getName()).classLoader();
        for (ReferenceType loadedClass : classLoader.visibleClasses()) {
            if (loadedClass.name().equals("nsk.share.jdi.TestClass1")) {
                log.display("WARNING: TestClass1 is loaded in the debuggee VM, can't test ClassNotLoadedException");
                testClassIsLoaded = true;
                break;
            }
        }

        ThreadReference threadReference = debuggee.threadByName(forceEarlyReturn002a.mainThreadName);

        try {
            if (testClassIsLoaded) {
                threadReference.forceEarlyReturn(null);
            } else {
                try {
                    threadReference.forceEarlyReturn(threadReference);
                    setSuccess(false);
                    log.complain("Expected 'ClassNotLoadedException' was not thrown");
                } catch (ClassNotLoadedException e) {
                    log.display("Got expected ClassNotLoadedException");
                }
            }
        } catch (Exception e) {
            unexpectedException(e);
        }

        debuggee.resume();

        if (!isDebuggeeReady())
            return;

        // after this command test class should be loaded
        pipe.println(forceEarlyReturn002a.COMMAND_LOAD_CLASS_AND_CALL_OBJECT_METHOD);

        breakPointEvent = waitForBreakpoint(breakpointRequest);

        // if no breakpoint happened then test failed, stop testing
        if (breakPointEvent == null)
            return;

        // get value for early return
        ObjectReference returnValue = (ObjectReference) referenceType.getValue(referenceType.fieldByName("expectedValue"));

        try {
            // don't expect any exception
            threadReference.forceEarlyReturn(returnValue);
        } catch (Exception e) {
            setSuccess(false);
            log.complain("Unexpected exception: " + e);
            e.printStackTrace(log.getOutStream());
        }

        testMethodExitEvent(threadReference, ClassUsingTestClass.breakpointMethodName);

        if (!isDebuggeeReady())
            return;
    }
}
