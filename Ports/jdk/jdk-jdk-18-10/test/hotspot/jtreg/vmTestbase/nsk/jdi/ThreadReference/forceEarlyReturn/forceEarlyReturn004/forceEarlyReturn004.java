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
 * @summary converted from VM Testbase nsk/jdi/ThreadReference/forceEarlyReturn/forceEarlyReturn004.
 * VM Testbase keywords: [quick, jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *         The test checks that a result of the method com.sun.jdi.forceEarlyReturn(Value value)
 *         complies with its specification. The test checks:
 *             - attempt to call com.sun.jdi.forceEarlyReturn for native Java method throws NativeMethodException
 *         Test scenario:
 *         Debugger VM force debuggee VM start test thread which execute infinite loop in native method, debugger suspend
 *         debuggee VM and try call forceEarlyReturn() for test thread, NativeMethodException should be thrown.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn004.forceEarlyReturn004
 *        nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn004.forceEarlyReturn004a
 * @run main/othervm/native
 *      nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn004.forceEarlyReturn004
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn004;

import java.io.PrintStream;
import com.sun.jdi.*;
import nsk.share.Consts;
import nsk.share.jdi.ForceEarlyReturnDebugger;

public class forceEarlyReturn004 extends ForceEarlyReturnDebugger {
    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public String debuggeeClassName() {
        return nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn004.forceEarlyReturn004a.class.getName();
    }

    public static int run(String argv[], PrintStream out) {
        return new forceEarlyReturn004().runIt(argv, out);
    }

    public void doTest() {
        pipe.println(forceEarlyReturn004a.COMMAND_STOP_THREAD_IN_NATIVE);

        if (!isDebuggeeReady())
            return;

        // this thread execute native method
        ThreadReference threadReference = debuggee.threadByName(forceEarlyReturn004a.testThreadInNativeName);

        vm.suspend();

        try {
            // expect NativeMethodException
            threadReference.forceEarlyReturn(vm.mirrorOf(0));
            setSuccess(false);
            log.complain("Expected 'NativeMethodException' exception was not thrown");
        } catch (NativeMethodException e) {
            // expected exception
        } catch (Exception e) {
            setSuccess(false);
            log.complain("Unexpected exception: " + e);
            e.printStackTrace(log.getOutStream());
        }

        vm.resume();
    }
}
