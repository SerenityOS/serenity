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
 * @summary converted from VM Testbase nsk/jdi/ThreadReference/ownedMonitorsAndFrames/ownedMonitorsAndFrames007.
 * VM Testbase keywords: [jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *         This test do the same things as 'ownedMonitorsAndFrames003', but run in debuggee VM several test threads.
 *         Default test threads count is 10, but number of test threads can be changed through test parameter '-testThreadsCount'
 *         (for example "-testThreadsCount 100").
 *         Debuggee VM creates a number of threads and each test thread acquires 4 different monitors in following ways:
 *                 - entering synchronized method
 *                 - entering synchronized block on non-static object
 *                 - entering synchronized method for thread object itself
 *                 - entering synchronized block on static object
 *         Information about all monitors acquired by test thread is stored in debuggee VM and can be obtained through
 *         special field in debuggee class: OwnedMonitorsDebuggee.monitorsInfo.
 *         Debugger VM reads information about acquired monitors from 'OwnedMonitorsDebuggee.monitorsInfo'
 *         and checks that com.sun.jdi.ThreadReference.ownedMonitorsAndFrames returns correct list of MonitorInfo objects, this
 *         check is performed for all test threads.
 *         Debugger VM forces all test threads in debuggee VM sequentially free monitors through Object.wait(),
 *         updates debug information about acquired monitors and checks that com.sun.jdi.ThreadReference.ownedMonitorsAndFrames
 *         returns correct list of MonitorInfo objects for all test threads.
 *         Debugger VM forces all test threads in debuggee VM free all monitors(exit from all synchronized objects/blocks),
 *         updates debug information about acquired monitors and checks that com.sun.jdi.ThreadReference.ownedMonitorsAndFrames
 *         returns correct list of MonitorInfo objects for all test threads.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ThreadReference.ownedMonitorsAndFrames.ownedMonitorsAndFrames007.ownedMonitorsAndFrames007
 * @run main/othervm/native
 *      nsk.jdi.ThreadReference.ownedMonitorsAndFrames.ownedMonitorsAndFrames007.ownedMonitorsAndFrames007
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdi.ThreadReference.ownedMonitorsAndFrames.ownedMonitorsAndFrames007;

import java.io.PrintStream;
import java.util.*;
import com.sun.jdi.*;
import nsk.share.Consts;
import nsk.share.jdi.OwnedMonitorsDebuggee;
import nsk.share.jdi.OwnedMonitorsDebugger;
import nsk.share.locks.LockingThread;

public class ownedMonitorsAndFrames007 extends OwnedMonitorsDebugger {
    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new ownedMonitorsAndFrames007().runIt(argv, out);
    }

    private int testThreadsCount = 10;

    // initialize test and remove unsupported by nsk.share.jdi.ArgumentHandler arguments
    protected String[] doInit(String args[], PrintStream out) {
        args = super.doInit(args, out);

        ArrayList<String> standardArgs = new ArrayList<String>();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-testThreadsCount") && (i < args.length - 1)) {
                testThreadsCount = Integer.parseInt(args[i + 1]);
                i++;
            } else
                standardArgs.add(args[i]);
        }

        return standardArgs.toArray(new String[] {});
    }

    private void checkAllThreads(String testThreadsNames[]) {
        for (int j = 0; j < testThreadsNames.length; j++) {
            ThreadReference threadReference = debuggee.threadByName(testThreadsNames[j]);

            pipe.println(OwnedMonitorsDebuggee.COMMAND_UPDATE_MONITOR_INFO + ":" + testThreadsNames[j]);

            if (!isDebuggeeReady())
                return;

            forceBreakpoint();

            checkMonitorInfo(threadReference);

            debuggee.resume();

            if (!isDebuggeeReady())
                return;
        }
    }

    public void doTest() {
        initDefaultBreakpoint();

        try {
            List<String> locksTypes = new ArrayList<String>();

            String testThreadsNames[] = new String[testThreadsCount];

            for (int i = 0; i < testThreadsNames.length; i++)
                testThreadsNames[i] = "ownedMonitorsAndFrames007_LockingThread" + (i + 1);

            // LockingThreads should acquire 4 different monitors
            locksTypes.add(LockingThread.SYNCHRONIZED_METHOD);
            locksTypes.add(LockingThread.SYNCHRONIZED_OBJECT_BLOCK);
            locksTypes.add(LockingThread.SYNCHRONIZED_THREAD_METHOD);
            locksTypes.add(LockingThread.SYNCHRONIZED_BLOCK_STATIC_OBJECT);

            for (int i = 0; i < testThreadsNames.length; i++) {
                String threadName = testThreadsNames[i];
                String command = OwnedMonitorsDebuggee.COMMAND_CREATE_LOCKING_THREAD + ":" + threadName;

                for (String lockType : locksTypes) {
                    command += ":" + lockType;
                }

                pipe.println(command);

                if (!isDebuggeeReady())
                    return;
            }

            checkAllThreads(testThreadsNames);

            // For all LockingThread's monitors:
            // - force thread relinquish monitors through Object.wait(),
            // - check results of ownedMonitorsAndFrames()
            // - force debuggee thread acquire relinquished monitor
            // - check results of ownedMonitorsAndFrames()
            for (int i = 0; i < locksTypes.size(); i++) {
                // all threads relinquish monitor
                for (int j = 0; j < testThreadsNames.length; j++) {
                    pipe.println(OwnedMonitorsDebuggee.COMMAND_RELINQUISH_MONITOR + ":" + testThreadsNames[j] + ":" + i);

                    if (!isDebuggeeReady())
                        return;
                }

                checkAllThreads(testThreadsNames);

                // all threads acquire monitor
                for (int j = 0; j < testThreadsNames.length; j++) {
                    pipe.println(OwnedMonitorsDebuggee.COMMAND_ACQUIRE_RELINQUISHED_MONITOR + ":" + testThreadsNames[j]);

                    if (!isDebuggeeReady())
                        return;
                }

                checkAllThreads(testThreadsNames);
            }

            // LockingThreads should free all monitors
            for (int i = 0; i < testThreadsNames.length; i++) {
                pipe.println(OwnedMonitorsDebuggee.COMMAND_STOP_LOCKING_THREAD + ":" + testThreadsNames[i]);

                if (!isDebuggeeReady())
                    return;
            }

            checkAllThreads(testThreadsNames);
        } finally {
            removeDefaultBreakpoint();
        }
    }

}
