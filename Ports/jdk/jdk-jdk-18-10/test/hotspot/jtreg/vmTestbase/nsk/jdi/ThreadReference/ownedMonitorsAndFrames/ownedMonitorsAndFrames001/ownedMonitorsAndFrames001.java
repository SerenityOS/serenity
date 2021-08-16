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
 * @summary converted from VM Testbase nsk/jdi/ThreadReference/ownedMonitorsAndFrames/ownedMonitorsAndFrames001.
 * VM Testbase keywords: [quick, jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *         Test check that attempt to call com.sun.jdi.ThreadReference.ownedMonitorsAndFrames on non-suspended VM
 *         throws IncompatibleThreadStateException.
 *         Special thread class is used in debugee VM for testing thread in different states - nsk.share.jpda.StateTestThread.
 *         StateTestThread sequentially changes its state in following order:
 *                 - thread not started
 *                 - thread is running
 *                 - thread is sleeping
 *                 - thread in Object.wait()
 *                 - thread wait on java monitor
 *                 - thread is finished
 *         Debugger VM calls ThreadReference.ownedMonitorsAndFrames() for all this states and expects IncompatibleThreadStateException.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ThreadReference.ownedMonitorsAndFrames.ownedMonitorsAndFrames001.ownedMonitorsAndFrames001
 * @run main/othervm
 *      nsk.jdi.ThreadReference.ownedMonitorsAndFrames.ownedMonitorsAndFrames001.ownedMonitorsAndFrames001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdi.ThreadReference.ownedMonitorsAndFrames.ownedMonitorsAndFrames001;

import java.io.*;
import com.sun.jdi.*;
import nsk.share.Consts;
import nsk.share.jdi.*;
import nsk.share.jpda.StateTestThread;
import nsk.share.jpda.AbstractDebuggeeTest;

public class ownedMonitorsAndFrames001 extends TestDebuggerType2 {
    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new ownedMonitorsAndFrames001().runIt(argv, out);
    }

    protected String debuggeeClassName() {
        return AbstractJDIDebuggee.class.getName();
    }

    private void test(ThreadReference threadReference) {
        log.display("Thread state: " + threadReference.status());
        try {
            // call ThreadReference.ownedMonitorsAndFrames() on non-suspended VM
            // IncompatibleThreadStateException should be thrown
            threadReference.ownedMonitorsAndFrames();
            setSuccess(false);
            log.complain("Expected IncompatibleThreadStateException was not thrown");
        } catch (IncompatibleThreadStateException e) {
            // expected exception
        } catch (Throwable e) {
            setSuccess(false);
            log.complain("Unexpected exception: " + e);
            e.printStackTrace(log.getOutStream());
        }
    }

    public void doTest() {
        // create StateTestThread
        pipe.println(AbstractDebuggeeTest.COMMAND_CREATE_STATETESTTHREAD);

        if (!isDebuggeeReady())
            return;

        // StateTestThread in state 0
        ThreadReference threadReference = (ThreadReference) findSingleObjectReference(AbstractDebuggeeTest.stateTestThreadClassName);

        test(threadReference);

        // change StateTestThread's state from 1 to stateTestThreadStates.length
        int state = 1;

        while (state++ < StateTestThread.stateTestThreadStates.length) {
            pipe.println(AbstractDebuggeeTest.COMMAND_NEXTSTATE_STATETESTTHREAD);

            if (!isDebuggeeReady())
                return;

            test(threadReference);
        }
    }
}
