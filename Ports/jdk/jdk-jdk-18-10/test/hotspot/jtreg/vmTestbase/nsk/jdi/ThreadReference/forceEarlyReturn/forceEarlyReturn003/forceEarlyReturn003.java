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
 * @summary converted from VM Testbase nsk/jdi/ThreadReference/forceEarlyReturn/forceEarlyReturn003.
 * VM Testbase keywords: [quick, jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *         The test checks that a result of the method com.sun.jdi.forceEarlyReturn(Value value)
 *         complies with its specification. The test checks:
 *                 - attempt to call forceEarlyReturn on not suspended thread throws IncompatibleThreadStateException
 *         Test scenario:
 *         Special thread class is used in debugee VM for testing thread in different states - nsk.share.jpda.StateTestThread.
 *         StateTestThread sequentially changes its state in following order:
 *                 - thread not started
 *                 - thread is running
 *                 - thread is sleeping
 *                 - thread in Object.wait()
 *                 - thread wait on java monitor
 *                 - thread is finished
 *         Debugger VM calls ThreadReference.forceEarlyReturn() for all this states and expects IncompatibleThreadStateException.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn003.forceEarlyReturn003
 * @run main/othervm
 *      nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn003.forceEarlyReturn003
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn003;

import java.io.PrintStream;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.ThreadReference;
import nsk.share.Consts;
import nsk.share.jdi.*;
import nsk.share.jpda.*;

public class forceEarlyReturn003 extends ForceEarlyReturnDebugger {
    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public String debuggeeClassName() {
        return AbstractJDIDebuggee.class.getName();
    }

    public static int run(String argv[], PrintStream out) {
        return new forceEarlyReturn003().runIt(argv, out);
    }

    public void test(ThreadReference threadReference) {
        log.display("Thread state: " + threadReference.status());
        try {
            // call ThreadReference.forceEarlyReturn() on non-suspended VM
            // IncompatibleThreadStateException should be thrown
            threadReference.forceEarlyReturn(vm.mirrorOf(0));

            setSuccess(false);
            log.complain("Expected IncompatibleThreadStateException was not thrown");
        } catch (IncompatibleThreadStateException e) {
            // expected exception
        } catch (Exception e) {
            setSuccess(false);
            log.complain("Unexpected exception: " + e);
            e.printStackTrace(System.out);
        }
    }

    public void doTest() {
        pipe.println(AbstractDebuggeeTest.COMMAND_CREATE_STATETESTTHREAD);

        if (!isDebuggeeReady())
            return;

        ThreadReference threadReference = (ThreadReference) debuggee.classByName(AbstractDebuggeeTest.stateTestThreadClassName).instances(0).get(0);

        test(threadReference);

        int state = 1;

        while (state++ < StateTestThread.stateTestThreadStates.length) {
            pipe.println(AbstractDebuggeeTest.COMMAND_NEXTSTATE_STATETESTTHREAD);

            if (!isDebuggeeReady())
                return;

            test(threadReference);
        }
    }
}
