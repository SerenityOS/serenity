/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8205608
 * @summary Test that getting the stack trace for a very large stack does
 * not take too long
 *
 * @author Ralf Schmelter
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g Frames2Test.java
 * @run driver Frames2Test -Xss4M
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;
import java.util.stream.*;


    /********** target program **********/

class Frames2Targ {

    public static void main(String[] args) {
        recurse(1_000_000);
    }

    static void notifyRecursionEnded() {
        // We moved this to a method instead of doing it directly in the
        // exception handler for the StackOverflowError, since we don't
        // need to know the line number (which might change later) when
        // requesting the breakpoint. Additionally this method is
        // used as a marker method to check for the correct top of stack
        // in the stack trace queried by JDI.
        System.out.println("SOE occurred as expected");
    }

    static int recurse(int depth) {
        if (depth == 0) {
            // Should have seen a stack overflow by now.
            System.out.println("Exited without creating SOE");
            System.exit(0);
        }

        try {
            int newDepth = recurse(depth - 1);

            if (newDepth == -1_000) {
                // Pop some frames so there is room on the stack for the
                // call (including println()).
                notifyRecursionEnded();
            }

            return newDepth - 1;
        } catch (StackOverflowError e) {
            // Use negative depth to indicate the recursion has ended.
            return -1;
        }
    }
}

    /********** test program **********/

public class Frames2Test extends TestScaffold {

    public Frames2Test(String args[]) {
        super(args);
    }

    public static void main(String[] args) throws Exception {
        new Frames2Test(args).startTests();
    }


    /********** test core **********/

    protected void runTests() throws Exception {
        BreakpointEvent bpe = startToMain("Frames2Targ");
        List<Method> frames1 = bpe.thread().frames().stream().map(
                StackFrame::location).map(Location::method).
                collect(Collectors.toList());

        bpe = resumeTo("Frames2Targ", "notifyRecursionEnded", "()V");
        List<Method> frames2 = bpe.thread().frames().stream().map(
                StackFrame::location).map(Location::method).
                collect(Collectors.toList());
        System.out.println("Got stack of " + frames2.size() + " frames");

        // Check that the stack looks as follows:
        // notifyRecursionEnded()
        // recurse()
        // ....
        // recurse()
        // main()
        // <whatever the VM likes to add>

        // Check the bottom of the stack.
        for (int i = 0; i < frames1.size(); ++i) {
            int i2 = frames2.size() - frames1.size() + i;
            if (!frames1.get(i).equals(frames2.get(i2))) {
                failure("Bottom methods do not match: " +
                        frames1.get(i) + " vs " + frames2.get(i2));
            }
        }

        // Check the recurse() calls on the stack.
        for (int i = 1; i < frames2.size() - frames1.size(); ++i) {
            if (!frames2.get(i).name().equals("recurse")) {
                failure("Expected recurse() but got " + frames2.get(i));
            }
        }

        // Check the top method of the stack.
        if (!frames2.get(0).name().equals("notifyRecursionEnded")) {
            failure("Expected notifyRecursionEnded() but got " + frames2.get(0));
        }

        listenUntilVMDisconnect();

        if (!testFailed) {
            println("Frames2Test: passed");
        } else {
            throw new Exception("Frames2Test: failed");
        }
    }
}
