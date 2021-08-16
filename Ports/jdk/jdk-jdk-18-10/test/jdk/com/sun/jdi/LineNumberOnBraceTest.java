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
 * @bug 4952629 4870514
 * @summary REGRESSION: javac generates a spurious line number entry on } else {
 * @author jjh
 *
 * @run build VMConnection TargetListener TargetAdapter
 * @run compile -g LineNumberOnBraceTest.java
 * @run driver LineNumberOnBraceTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

class LineNumberOnBraceTarg {

    public final static int STOP_LINE = 50;    // THIS MUST BE THE LINE NUMBER OF // STOP_LINE LINE
    public final static int STOP_LINE_2 = 56;  // THIS MUST BE THE LINE NUMBER OF // STOP_LINE_2 LINE

    public static void main(String[] args){
        System.out.println("Howdy!");
        if (args.length == 0) {
            System.out.println("No args to debuggee");             // STOP_LINE
        } else {
            System.out.println("Some args to debuggee");
        }
        if (args.length == 0) {                                    // STOP_LINE + 4
            boolean b1 = false;
            if (b1) {                                              // STOP_LINE_2
                System.out.println("In 2nd else");                 // bug 4870514 is that we stop here.
            }
        } else {
            System.out.println("In 2nd else");
        }
        System.out.println("Goodbye from LineNumberOnBraceTarg!");
    }

    // This isn't part of the test; it is just here
    // so one can see what line numbers are generated for a finally.
    public void exampleOfThrow() {
        try {
            throw new Exception();
        } catch (Exception e) {
            System.out.println("caught exception");
        } finally {
            System.out.println("finally");
        }
    }

}

    /********** test program **********/

public class LineNumberOnBraceTest extends TestScaffold {
    ReferenceType targetClass;
    ThreadReference mainThread;

    LineNumberOnBraceTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new LineNumberOnBraceTest(args).startTests();
    }
    /********** test core **********/

    protected void runTests() throws Exception {
        /*
         * Get to the top of main()
         * to determine targetClass and mainThread
         */
        BreakpointEvent bpe = startToMain("LineNumberOnBraceTarg");
        targetClass = bpe.location().declaringType();
        mainThread = bpe.thread();

        resumeTo("LineNumberOnBraceTarg", LineNumberOnBraceTarg.STOP_LINE);
        StepEvent stepev = stepOverLine(mainThread);       // step to 2nd if (args.length

        // Bug 4952629 is that javac outputs a line number
        // on the goto around the else which causes us to
        // be stopped at that goto instead of the println("Goodbye ...")

        int ln = stepev.location().lineNumber();
        System.out.println("Debuggee is stopped at line " + ln);
        if (ln != LineNumberOnBraceTarg.STOP_LINE + 4) {
            failure("FAIL: Bug 4952629: Should be at line " +
                    (LineNumberOnBraceTarg.STOP_LINE + 4) +
                    ", am at " + ln);
        } else {
            System.out.println("Passed test for 4952629");
        }

        // Test for bug 4870514
        System.out.println("Resuming to " + LineNumberOnBraceTarg.STOP_LINE_2);
        resumeTo("LineNumberOnBraceTarg", LineNumberOnBraceTarg.STOP_LINE_2);
        System.out.println("Stopped at " + LineNumberOnBraceTarg.STOP_LINE_2);
        stepev = stepOverLine(mainThread);
        ln = stepev.location().lineNumber();
        System.out.println("Debuggee is stopped at line " + ln);
        if (ln <= LineNumberOnBraceTarg.STOP_LINE_2 + 1) {
            failure("FAIL: bug 4870514: Incorrectly stopped at " + ln);
        } else {
            System.out.println("Passed test for 4870514");
        }


        /*
         * resume the target listening for events
         */
        listenUntilVMDisconnect();

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("LineNumberOnBraceTest: passed");
        } else {
            throw new Exception("LineNumberOnBraceTest: failed");
        }
    }
}
