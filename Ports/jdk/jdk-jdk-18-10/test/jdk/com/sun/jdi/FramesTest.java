/*
 * Copyright (c) 2001, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4434232
 * @summary Test ThreadReference.frames(int,int)
 * @author Robert Field
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g FramesTest.java
 * @run driver FramesTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

    /********** target program **********/

class FramesTarg {
    static void foo3() {
        System.out.println("executing foo3");
    }
    static void foo2() {
        foo3();
    }
    static void foo1() {
        foo2();
    }
    public static void main(String[] args){
        System.out.println("Howdy!");
        foo1();
    }
}

    /********** test program **********/

public class FramesTest extends TestScaffold {
    ReferenceType targetClass;
    ThreadReference mainThread;

    static String[] expectedNames = {"foo3", "foo2", "foo1", "main"};

    FramesTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new FramesTest(args).startTests();
    }

    /********** test assist **********/

    void exceptionTest(int start, int length) {
        boolean gotException = false;
        try {
            mainThread.frames(start, length);
        } catch (IndexOutOfBoundsException exc) {
            gotException = true;
        } catch (Exception exc) {
            failure("unexpected exception thrown for: " +
                    "start = " + start + ", length = " + length +
                    " - " + exc);
            gotException = true;
        }
        if (!gotException) {
            failure("expected IndexOutOfBoundsException " +
                    "not thrown for: " +
                    "start = " + start + ", length = " + length);
        }
    }

    void nameTest(int start, int length) {
        try {
            List fs = mainThread.frames(start, length);
            if (fs.size() != length) {
                failure("wrong length for: " +
                        "start = " + start + ", length = " + length);
            }
            for (int i = 0; i < length; ++i) {
                StackFrame sf = (StackFrame)(fs.get(i));
                String name = sf.location().method().name();
                String expected = expectedNames[start+i];
                if (!name.equals(expected)) {
                    failure("bad frame entry (" + start + "," + length +
                            ") - expected " + expected +
                            ", got " + name);
                }
            }
        } catch (Exception exc) {
            failure("unexpected exception thrown for: " +
                    "start = " + start + ", length = " + length +
                    " - " + exc);
        }
    }

    /********** test core **********/

    protected void runTests() throws Exception {
        /*
         * Get to the top of main()
         * to determine targetClass and mainThread
         */
        BreakpointEvent bpe = startToMain("FramesTarg");
        targetClass = bpe.location().declaringType();
        mainThread = bpe.thread();

        int initialSize = mainThread.frames().size();

        resumeTo("FramesTarg", "foo3", "()V");

        if (!mainThread.frame(0).location().method().name()
                        .equals("foo3")) {
            failure("frame failed");
        }

        if (mainThread.frames().size() != (initialSize + 3)) {
            failure("frames size failed");
        }

        if (mainThread.frames().size() != mainThread.frameCount()) {
            failure("frames size not equal to frameCount");
        }

        exceptionTest(-1, 1);
        exceptionTest(mainThread.frameCount(), 1);
        exceptionTest(0, -1);
        exceptionTest(0, -2);
        exceptionTest(0, mainThread.frameCount()+1);

        nameTest(0, 0);
        nameTest(0, 1);
        nameTest(0, 4);
        nameTest(2, 2);
        nameTest(1, 1);

        /*
         * resume until end
         */
        listenUntilVMDisconnect();

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("FramesTest: passed");
        } else {
            throw new Exception("FramesTest: failed");
        }
    }
}
