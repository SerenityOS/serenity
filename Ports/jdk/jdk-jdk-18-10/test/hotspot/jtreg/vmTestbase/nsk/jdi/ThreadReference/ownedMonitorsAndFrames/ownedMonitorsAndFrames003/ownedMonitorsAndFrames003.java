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
 * @summary converted from VM Testbase nsk/jdi/ThreadReference/ownedMonitorsAndFrames/ownedMonitorsAndFrames003.
 * VM Testbase keywords: [quick, jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *         Debuggee VM create thread which acquires 5 different monitors in following ways:
 *                 - entering synchronized method
 *                 - entering synchronized method for thread object itself
 *                 - entering synchronized static method
 *                 - entering synchronized method for thread class itself
 *                 - entering synchronized block on non-static object
 *         Information about all monitors acquired by test thread is stored in debuggee VM and can be obtained through
 *         special field in debuggee class: OwnedMonitorsDebuggee.monitorsInfo.
 *         Debugger VM reads information about acquired monitors from 'OwnedMonitorsDebuggee.monitorsInfo'
 *         and checks that com.sun.jdi.ThreadReference.ownedMonitorsAndFrames returns correct list of MonitorInfo objects.
 *         Debugger VM forces test thread in debuggee VM sequentially free monitors through Object.wait(),
 *         updates debug information about acquired monitors and checks that com.sun.jdi.ThreadReference.ownedMonitorsAndFrames
 *         returns correct list of MonitorInfo objects.
 *         Debugger VM forces test thread in debuggee VM free all monitors(exit from all synchronized objects/blocks),
 *         update debug information about acquired monitors and checks that com.sun.jdi.ThreadReference.ownedMonitorsAndFrames
 *         returns correct list of MonitorInfo objects.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ThreadReference.ownedMonitorsAndFrames.ownedMonitorsAndFrames003.ownedMonitorsAndFrames003
 * @run main/othervm/native
 *      nsk.jdi.ThreadReference.ownedMonitorsAndFrames.ownedMonitorsAndFrames003.ownedMonitorsAndFrames003
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdi.ThreadReference.ownedMonitorsAndFrames.ownedMonitorsAndFrames003;

import java.io.*;
import java.util.*;
import com.sun.jdi.*;
import nsk.share.jdi.OwnedMonitorsDebuggee;
import nsk.share.jdi.OwnedMonitorsDebugger;
import nsk.share.locks.LockingThread;
import nsk.share.Consts;

public class ownedMonitorsAndFrames003 extends OwnedMonitorsDebugger {
    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new ownedMonitorsAndFrames003().runIt(argv, out);
    }

    public void doTest() {
        initDefaultBreakpoint();

        try {
            List<String> locksTypes = new ArrayList<String>();

            // LockingThread acquire 5 different monitors
            locksTypes.add(LockingThread.SYNCHRONIZED_METHOD);
            locksTypes.add(LockingThread.SYNCHRONIZED_THREAD_METHOD);
            locksTypes.add(LockingThread.SYNCHRONIZED_STATIC_THREAD_METHOD);
            locksTypes.add(LockingThread.SYNCHRONIZED_OBJECT_BLOCK);
            locksTypes.add(LockingThread.SYNCHRONIZED_STATIC_METHOD);

            String threadName = "ownedMonitorsAndFrames003_LockingThread1";
            String command = OwnedMonitorsDebuggee.COMMAND_CREATE_LOCKING_THREAD + ":" + threadName;

            for (String lockType : locksTypes) {
                command += ":" + lockType;
            }

            pipe.println(command);

            if (!isDebuggeeReady())
                return;

            forceBreakpoint();

            ThreadReference threadReference = debuggee.threadByName(threadName);

            // check ownedMonitorsAndFrames() first time when all monitors are locked
            checkMonitorInfo(threadReference);

            debuggee.resume();

            if (!isDebuggeeReady())
                return;

            // For all LockingThread's monitors:
            // - force thread relinquish monitors through Object.wait(),
            // - check results of ownedMonitorsAndFrames()
            // - force debuggee thread acquire relinquished monitor
            // - check results of ownedMonitorsAndFrames()
            for (int i = 0; i < locksTypes.size(); i++) {
                pipe.println(OwnedMonitorsDebuggee.COMMAND_RELINQUISH_MONITOR + ":" + threadName + ":" + i);

                if (!isDebuggeeReady())
                    return;

                forceBreakpoint();

                checkMonitorInfo(threadReference);

                debuggee.resume();

                if (!isDebuggeeReady())
                    return;

                pipe.println(OwnedMonitorsDebuggee.COMMAND_ACQUIRE_RELINQUISHED_MONITOR + ":" + threadName);

                if (!isDebuggeeReady())
                    return;

                forceBreakpoint();

                checkMonitorInfo(threadReference);

                debuggee.resume();

                if (!isDebuggeeReady())
                    return;
            }

            // LockingThread should free all monitors

            pipe.println(OwnedMonitorsDebuggee.COMMAND_STOP_LOCKING_THREAD + ":" + threadName);

            if (!isDebuggeeReady())
                return;

            forceBreakpoint();

            checkMonitorInfo(threadReference);

            debuggee.resume();

            if (!isDebuggeeReady())
                return;
        } finally {
            removeDefaultBreakpoint();
        }

    }

}
