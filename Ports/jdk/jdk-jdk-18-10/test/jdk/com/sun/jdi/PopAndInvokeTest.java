/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6517249
 * @summary JDWP: Cannot do an invokeMethod after a popFrames operation
 * @author jjh
 *
 * @ignore 6951287
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g PopAndInvokeTest.java
 * @run driver PopAndInvokeTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import java.util.*;

class PopAndInvokeTarg {
    static boolean waiting = false;

    public static void A() {
        System.out.println("    debuggee: in A");
    }

    public static void invokeee() {
        System.out.println("    debuggee: invokee");
    }

    public static void waiter() {
        if (waiting) {
            return;
        }
        waiting = true;
        System.out.println("    debuggee: in waiter");
        while (true) {
        }
    }

    public static void main(String[] args) {
        System.out.println("    debuggee: Howdy!");
        /*
         * Debugger will bkpt in A, popFrames back to here
         * and then do an invokeMethod on invokeee.
         * This should work.
         */
        A();

        /*
         * Debugger will resume and we will enter
         * waiter().  Debugger will then do a suspend,
         * a popFrames back to here, and an invoke
         * which should fail.
         */
        waiter();
        System.out.println("    debuggee: Goodbye from PopAndInvokeTarg!");
    }
}


    /********** test program **********/

public class PopAndInvokeTest extends TestScaffold {
    ClassType targetClass;
    ThreadReference mainThread;

    PopAndInvokeTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new PopAndInvokeTest(args).startTests();
    }

    StackFrame frameFor(String methodName) throws Exception {
        Iterator it = mainThread.frames().iterator();

        while (it.hasNext()) {
            StackFrame frame = (StackFrame)it.next();
            if (frame.location().method().name().equals(methodName)) {
                return frame;
            }
        }
        failure("FAIL: " + methodName + " not on stack");
        return null;
    }

    /********** test core **********/

    protected void runTests() throws Exception {
        /*
         * Get to the top of main()
         * to determine targetClass and mainThread
         */
        runOnce();
    }

    void runOnce() throws Exception {

        BreakpointEvent bpe = startTo("PopAndInvokeTarg", "A", "()V");
        targetClass = (ClassType)bpe.location().declaringType();
        mainThread = bpe.thread();

        /*
         * Verify that an invokeMethod works ok after a popFrames
         * in a thread suspended by an event.
         */
        mainThread.popFrames(frameFor("A"));

        System.out.println("Debugger: Popped back to the call to A()");
        System.out.println("Debugger: Doing invoke");

        Method invokeeeMethod = (Method)targetClass.methodsByName("invokeee").get(0);
        try {
            targetClass.invokeMethod(mainThread, invokeeeMethod,
                                     new ArrayList(), 0);
        } catch (Exception ex) {
            failure("failure: invoke got unexpected exception: " + ex);
            ex.printStackTrace();
        }
        System.out.println("Debugger: invoke done");

        /*
         * Verify that an invokeMethod gets an IncompatibleThreadStateException
         * after a popFrames in a thread that is _not_ suspended by an event.
         */
        System.out.println("Debugger: Resuming debuggee");
        vm().resume();

        Field waiting = targetClass.fieldByName("waiting");
        while (true) {
            // Wait until debuggee enters the 'waiting' method.
            BooleanValue bv= (BooleanValue)targetClass.getValue(waiting);
            if (!bv.value()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ee) {
                }
                continue;
            }

            // debuggee has entered the waiting method
            System.out.println("Debugger: Suspending debuggee");
            vm().suspend();
            System.out.println("Debugger: Popping frame for waiter");
            mainThread.popFrames(frameFor("waiter"));
            System.out.println("Debugger: Invoking method");
            try {
                targetClass.invokeMethod(mainThread, invokeeeMethod,
                                         new ArrayList(), 0);
            } catch (IncompatibleThreadStateException ee) {
                System.out.println("Debugger: Success: Got expected IncompatibleThreadStateException");
                break;
            } catch (Exception ee) {
                failure("FAIL: Got unexpected exception: " + ee);
                break;
            }
            failure("FAIL: Did not get IncompatibleThreadStateException " +
                    "when debuggee is not suspended by an event");
        }
        listenUntilVMDisconnect();
        if (testFailed) {
            throw new Exception("PopAndInvokeTest failed");
        }
        System.out.println("Passed:");
    }
}
