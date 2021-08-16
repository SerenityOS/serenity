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
 * @summary converted from VM Testbase nsk/jdi/StackFrame/getArgumentValues/getArgumentValues003.
 * VM Testbase keywords: [quick, jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test checks that method 'StackFrame.getArgumentValues()' throws InvalidStackFrameException if this stack
 *     frame has become invalid.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.StackFrame.getArgumentValues.getArgumentValues003.getArgumentValues003
 *        nsk.jdi.StackFrame.getArgumentValues.getArgumentValues003.getArgumentValues003a
 * @run main/othervm
 *      nsk.jdi.StackFrame.getArgumentValues.getArgumentValues003.getArgumentValues003
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdi.StackFrame.getArgumentValues.getArgumentValues003;

import java.io.*;
import com.sun.jdi.*;
import nsk.share.Consts;
import nsk.share.jdi.*;

/*
 * Test checks that method 'StackFrame.getArgumentValues()' throws InvalidStackFrameException if this stack
 * frame has become invalid (once the frame's thread is resumed, the stack frame is no longer valid).
 *
 * Test obtains ThreadReference for debuggee test thread, suspends test thread, obtains StackFrame instance for current
 * thread frame, calls StackFrame.getArgumentValues() first time and checks that no exception is thrown. Then debugger
 * resumes test thread, calls getArgumentValues again and checks that in this case InvalidStackFrameException is thrown.
 */
public class getArgumentValues003 extends TestDebuggerType2 {
    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new getArgumentValues003().runIt(argv, out);
    }

    protected String debuggeeClassName() {
        return getArgumentValues003a.class.getName();
    }

    public void doTest() {
        pipe.println(getArgumentValues003a.COMMAND_START_TEST_THREAD);

        if (!isDebuggeeReady())
            return;

        ThreadReference testThread = debuggee.threadByName(getArgumentValues003a.testThreadName);
        testThread.suspend();
        try {
            StackFrame frame = testThread.frame(0);
            log.display("Call getArgumentValues()");
            frame.getArgumentValues();
            log.display("OK");
            log.display("Resume thread");
            testThread.resume();
            log.display("Call getArgumentValues()");
            try {
                frame.getArgumentValues();
                setSuccess(false);
                log.complain("Expected InvalidStackFrameException was not thrown");
            } catch (InvalidStackFrameException e) {
                log.display("Expected InvalidStackFrameException was thrown");
            }
        } catch (Throwable t) {
            setSuccess(false);
            log.complain("Unexpected exception: " + t);
            t.printStackTrace(log.getOutStream());
        } finally {
            if (testThread.isSuspended())
                testThread.resume();
        }

        pipe.println(getArgumentValues003a.COMMAND_STOP_TEST_THREAD);

        if (!isDebuggeeReady())
            return;
    }
}
