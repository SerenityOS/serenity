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
 * @summary converted from VM Testbase nsk/jdi/StackFrame/getArgumentValues/getArgumentValues002.
 * VM Testbase keywords: [quick, jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *         The test checks that method 'StackFrame.getArgumentValues()' returns the values of all arguments in this frame.
 *         The test checks case when thread has stack with 300 frames and checks result of 'StackFrame.getArgumentValues()' for
 *         all thread's frames.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.StackFrame.getArgumentValues.getArgumentValues002.getArgumentValues002
 *        nsk.jdi.StackFrame.getArgumentValues.getArgumentValues002.getArgumentValues002a
 * @run main/othervm
 *      nsk.jdi.StackFrame.getArgumentValues.getArgumentValues002.getArgumentValues002
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdi.StackFrame.getArgumentValues.getArgumentValues002;

import java.io.*;
import java.util.*;
import com.sun.jdi.*;
import nsk.share.Consts;
import nsk.share.jdi.*;

/*
 * Test checks that method 'StackFrame.getArgumentValues()' returns the values of all arguments in this frame.
 * Test checks case when thread has stack with many frames:
 * - debuggee starts test thread which using recursion creates stack frame containing 300 frames
 * - debugger suspends test thread and for each frame of this thread calls method 'StackFrame.getArgumentValues()' and
 * compares returned values with expected.
 */
public class getArgumentValues002 extends TestDebuggerType2 {
    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new getArgumentValues002().runIt(argv, out);
    }

    protected String debuggeeClassName() {
        return getArgumentValues002a.class.getName();
    }

    public void doTest() {
        pipe.println(getArgumentValues002a.COMMAND_START_TEST_THREAD);

        if (!isDebuggeeReady())
            return;

        ThreadReference testThread = debuggee.threadByName(getArgumentValues002a.testThreadName);
        testThread.suspend();
        try {
            for (int i = 0; i < testThread.frameCount(); i++) {
                boolean success = true;
                StackFrame frame = testThread.frame(i);
                List<Value> values = frame.getArgumentValues();

                // last frame if method Thread.run()
                if (i == testThread.frameCount() - 1) {
                    if (values.size() != 0) {
                        success = false;
                        log.complain("ERROR: unexpected values count: " + values.size() + ", expected is 0");
                    }
                } else {
                    // StackFrame should have argument equals to it serial number (look at code of getArgumentValues002a.TreadThread for details)
                    Value expectedValue = vm.mirrorOf((int) i);

                    log.display("Expected value: " + expectedValue);

                    if (values.size() != 1) {
                        success = false;
                        log.complain("ERROR: unexpected values count: " + values.size() + ", expected is 1");
                    } else {
                        if (!values.get(0).equals(expectedValue)) {
                            success = false;
                            log.complain("ERROR: unexpected value: " + values.get(0) + ", expected is " + expectedValue);
                        }
                    }
                }

                if (!success) {
                    setSuccess(false);
                    log.complain("Returned values:");
                    for (Value value : values) {
                        log.complain("" + value);
                    }
                } else {
                    log.display("OK");
                }
            }
        } catch (Throwable t) {
            setSuccess(false);
            log.complain("Unexpected exception: " + t);
            t.printStackTrace(log.getOutStream());
        } finally {
            testThread.resume();
        }

        pipe.println(getArgumentValues002a.COMMAND_STOP_TEST_THREAD);

        if (!isDebuggeeReady())
            return;
    }
}
