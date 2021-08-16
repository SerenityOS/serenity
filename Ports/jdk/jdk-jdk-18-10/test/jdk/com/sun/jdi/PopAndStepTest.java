/*
 * Copyright (c) 2007, 2017, Oracle and/or its affiliates. All rights reserved.
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

//    THIS TEST IS LINE NUMBER SENSITIVE

/**
 * @test
 * @bug 4530424
 * @summary Hin says that doing a step over after a popframe acts like a resume.
 * @author jjh
 *
 * @library ..
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g PopAndStepTest.java
 * @run driver PopAndStepTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

class PopAndStepTarg {
    public void B() {
        System.out.println("debuggee: in B");             // B_LINE_1
        System.out.println("debuggee: in B, back to A");  // B_LINE_2
    }

    public void A() {
        System.out.println("debuggee: in A, about to call B");  // A_LINE_1
        B();                                                    // A_LINE_2
        System.out.println("debuggee: in A, back from B");      // A_LINE_3
        throw new RuntimeException("debuggee: Got to line A_LINE_4:" + PopAndStepTest.A_LINE_4); // A_LINE_4
    }

    public static void main(String[] args) {
        System.out.println("debuggee: Howdy!");      // MAIN_LINE_1
        PopAndStepTarg xxx = new PopAndStepTarg();   // MAIN_LINE_2
        xxx.A();                                     // MAIN_LINE_3
        System.out.println("debugee: Goodbye from PopAndStepTarg!");
    }
}


    /********** test program **********/

public class PopAndStepTest extends TestScaffold {
    static final int B_LINE_1 = 46;
    static final int B_LINE_2 = B_LINE_1 + 1;

    static final int A_LINE_1 = 51;
    static final int A_LINE_2 = A_LINE_1 + 1;
    static final int A_LINE_3 = A_LINE_1 + 2;
    static final int A_LINE_4 = A_LINE_1 + 3;

    static final int MAIN_LINE_1 = 58;
    static final int MAIN_LINE_2 = MAIN_LINE_1 + 1;
    static final int MAIN_LINE_3 = MAIN_LINE_1 + 2;

    ReferenceType targetClass;
    ThreadReference mainThread;

    PopAndStepTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new PopAndStepTest(args).startTests();
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

    int getDebuggeeLineNum(int expectedLine) throws Exception {
        List allFrames = mainThread.frames();
        if ( allFrames == null) {
            return -1;
        }
        Iterator it = allFrames.iterator();
        StackFrame frame = (StackFrame)it.next();
        Location loc = frame.location();
        int theLine = loc.lineNumber();
        if (expectedLine != theLine) {
            failure("FAIL: Should be at " + expectedLine + ", are at " +
                    theLine + ", method = " + loc.method().name());
        } else {
            println("Should be at, and am at: " + expectedLine);
        }
        return theLine;
    }


    public void vmDied(VMDeathEvent event) {
        println("Got VMDeathEvent");
    }

    public void vmDisconnected(VMDisconnectEvent event) {
        println("Got VMDisconnectEvent");
    }

    /********** test core **********/

    protected void runTests() throws Exception {
        /*
         * Get to the top of main()
         * to determine targetClass and mainThread
         */
        runOnce();
    }

    void runOnce() throws Exception{
        /*
         * Get to the top of main()
         * to determine targetClass and mainThread
         */
        BreakpointEvent bpe = startToMain("PopAndStepTarg");
        targetClass = bpe.location().declaringType();
        mainThread = bpe.thread();
        getDebuggeeLineNum(MAIN_LINE_1);

        println("Resuming to line B_LINE_2 : " + B_LINE_2);
        bpe = resumeTo("PopAndStepTarg", B_LINE_2); getDebuggeeLineNum(B_LINE_2);

        // The failure is this:
        //   create step request
        //   enable step request
        //   pop frame
        //   do the step
        //   do another step - This step runs to completion
        EventRequestManager erm = eventRequestManager();
        StepRequest srInto = erm.createStepRequest(mainThread, StepRequest.STEP_LINE,
                                                   StepRequest.STEP_INTO);
        srInto.addClassExclusionFilter("java.*");
        srInto.addClassExclusionFilter("javax.*");
        srInto.addClassExclusionFilter("sun.*");
        srInto.addClassExclusionFilter("com.sun.*");
        srInto.addClassExclusionFilter("com.oracle.*");
        srInto.addClassExclusionFilter("oracle.*");
        srInto.addClassExclusionFilter("jdk.internal.*");
        srInto.addCountFilter(1);
        srInto.enable(); // This fails
        mainThread.popFrames(frameFor("A"));
        //srInto.enable();   // if the enable is moved here, it passes
        println("Popped back to line MAIN_LINE_3(" + MAIN_LINE_3 + ") in main, the call to A()");
        println("Stepping into line A_LINE_1:" + A_LINE_1);
        waitForRequestedEvent(srInto);  // println
        srInto.disable();

        getDebuggeeLineNum(A_LINE_1);

        // The failure occurs here.
        println("Stepping over to line A_LINE_2:" + A_LINE_2);
        stepOverLine(mainThread);       // println
        getDebuggeeLineNum(A_LINE_2);

        println("Stepping over to line A_LINE_3:" + A_LINE_3);
        stepOverLine(mainThread);       // call to B()
        getDebuggeeLineNum(A_LINE_3);

        vm().exit(0);

        if (testFailed) {
            throw new Exception("PopAndStepTest failed");
        }
        println("Passed:");
    }
}
