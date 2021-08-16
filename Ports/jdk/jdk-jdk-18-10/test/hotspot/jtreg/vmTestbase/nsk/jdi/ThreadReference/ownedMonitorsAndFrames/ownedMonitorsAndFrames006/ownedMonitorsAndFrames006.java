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
 * @summary converted from VM Testbase nsk/jdi/ThreadReference/ownedMonitorsAndFrames/ownedMonitorsAndFrames006.
 * VM Testbase keywords: [jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *         This test do the same things as 'ownedMonitorsAndFrames002', but run in debuggee VM several test threads.
 *         Default test threads count is 10, but number of test threads can be changed through test parameter '-testThreadsCount'
 *         (for example "-testThreadsCount 100").
 *         Debuggee VM creates a number of threads which acquire several monitors in following ways:
 *                 - entering synchronized method
 *                 - entering synchronized method for thread object itself
 *                 - entering synchronized block on non-static object
 *                 - entering synchronized block on thread object itself
 *         Information about all monitors acquired by test threads is stored in debuggee VM and can be obtained through
 *         special field in debuggee class: OwnedMonitorsDebuggee.monitorsInfo.
 *         Debugger VM reads information about acquired monitors from 'OwnedMonitorsDebuggee.monitorsInfo'
 *         and checks that com.sun.jdi.ThreadReference.ownedMonitorsAndFrames returns correct list of MonitorInfo objects for
 *         all test threads.
 *         Debugger VM forces test all threads in debuggee VM sequentially free all monitors(exit from synchronized blocks/methods),
 *         updates debug information about acquired monitors and checks that com.sun.jdi.ThreadReference.ownedMonitorsAndFrames
 *         returns correct list of MonitorInfo objects for all test threads.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ThreadReference.ownedMonitorsAndFrames.ownedMonitorsAndFrames006.ownedMonitorsAndFrames006
 * @run main/othervm/native
 *      nsk.jdi.ThreadReference.ownedMonitorsAndFrames.ownedMonitorsAndFrames006.ownedMonitorsAndFrames006
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdi.ThreadReference.ownedMonitorsAndFrames.ownedMonitorsAndFrames006;

import java.io.PrintStream;
import nsk.share.Consts;
import nsk.share.jdi.OwnedMonitorsDebugger;
import nsk.share.jdi.OwnedMonitorsDebuggee;
import nsk.share.locks.*;
import java.util.*;

import com.sun.jdi.ThreadReference;

public class ownedMonitorsAndFrames006 extends OwnedMonitorsDebugger {
    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new ownedMonitorsAndFrames006().runIt(argv, out);
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

    public void doTest() {
        initDefaultBreakpoint();

        try {
            String testThreadsNames[] = new String[testThreadsCount];

            for (int i = 0; i < testThreadsNames.length; i++)
                testThreadsNames[i] = "ownedMonitorsAndFrames006_LockingThread" + (i + 1);

            List<String> locksTypes = new ArrayList<String>();

            locksTypes.add(LockingThread.SYNCHRONIZED_METHOD);
            locksTypes.add(LockingThread.SYNCHRONIZED_THREAD_METHOD);
            locksTypes.add(LockingThread.SYNCHRONIZED_THIS_BLOCK);
            locksTypes.add(LockingThread.SYNCHRONIZED_OBJECT_BLOCK);
            locksTypes.add(LockingThread.FRAME_WITHOUT_LOCK);

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

            // check ownedMonitorsAndFrames() for all threads
            for (int i = 0; i < testThreadsNames.length; i++) {
                ThreadReference threadReference = debuggee.threadByName(testThreadsNames[i]);

                pipe.println(OwnedMonitorsDebuggee.COMMAND_UPDATE_MONITOR_INFO + ":" + testThreadsNames[i]);

                if (!isDebuggeeReady())
                    return;

                forceBreakpoint();

                checkMonitorInfo(threadReference);

                debuggee.resume();

                if (!isDebuggeeReady())
                    return;
            }

            // force debuggee threads sequentially exit from synchronized blocks/methods and check results of ownedMonitorsAndFrames()
            for (int i = 0; i < locksTypes.size(); i++) {
                // all threads free single lock
                for (int j = 0; j < testThreadsNames.length; j++) {
                    pipe.println(OwnedMonitorsDebuggee.COMMAND_EXIT_SINGLE_FRAME + ":" + testThreadsNames[j]);

                    if (!isDebuggeeReady())
                        return;
                }

                // check results of ownedMonitorsAndFrames()
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
        } finally {
            removeDefaultBreakpoint();
        }

    }
}
