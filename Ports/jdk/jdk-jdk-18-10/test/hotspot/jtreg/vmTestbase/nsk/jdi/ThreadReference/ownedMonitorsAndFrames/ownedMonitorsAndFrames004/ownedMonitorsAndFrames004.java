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
 * @summary converted from VM Testbase nsk/jdi/ThreadReference/ownedMonitorsAndFrames/ownedMonitorsAndFrames004.
 * VM Testbase keywords: [quick, jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *         Debuggee VM creates 3 deadlocks(package nsk.share.locks is used) with usage of following resources:
 *                 - first thread acquires java lock through synchronized block,
 *                 second thread acquires java lock through synchronized method
 *                 - first thread acquires java lock through synchronized blocks,
 *                 second thread acquires lock through JNI function 'MonitorEnter'
 *                 - first thread acquires java locks through synchronized blocks,
 *                 second thread acquires lock through JNI function 'MonitorEnter',
 *                 third thread acquires java locks through synchronized method
 *         Information about all monitors acquired by test threads is stored in debuggee VM and can be obtained through
 *         special field in debuggee class: OwnedMonitorsDebuggee.monitorsInfo.
 *         Debugger VM reads information about acquired monitors from 'OwnedMonitorsDebuggee.monitorsInfo'
 *         and checks that com.sun.jdi.ThreadReference.ownedMonitorsAndFrames returns correct list of MonitorInfo objects.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ThreadReference.ownedMonitorsAndFrames.ownedMonitorsAndFrames004.ownedMonitorsAndFrames004
 *        nsk.jdi.ThreadReference.ownedMonitorsAndFrames.ownedMonitorsAndFrames004.ownedMonitorsAndFrames004a
 * @run main/othervm/native
 *      nsk.jdi.ThreadReference.ownedMonitorsAndFrames.ownedMonitorsAndFrames004.ownedMonitorsAndFrames004
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdi.ThreadReference.ownedMonitorsAndFrames.ownedMonitorsAndFrames004;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import nsk.share.Consts;
import nsk.share.jdi.OwnedMonitorsDebugger;
import nsk.share.locks.LockType;

public class ownedMonitorsAndFrames004 extends OwnedMonitorsDebugger {
    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new ownedMonitorsAndFrames004().runIt(argv, out);
    }

    public String debuggeeClassName() {
        return "nsk.jdi.ThreadReference.ownedMonitorsAndFrames.ownedMonitorsAndFrames004.ownedMonitorsAndFrames004a";
    }

    private void checkDeadlockedThread(String... threadNames) {
        // expectedData.get(i) - data for thread with name 'threadNames[i]'
        List<DebugMonitorInfo> expectedData = getDebugMonitorsInfo();

        if (expectedData.size() != threadNames.length) {
            log.complain("TEST BUG: debugee didn't provide correct debug information");
            setSuccess(false);
            return;
        }

        List<DebugMonitorInfo> expectedDataForSingleThread = new ArrayList<DebugMonitorInfo>();

        for (int i = 0; i < threadNames.length; i++) {
            expectedDataForSingleThread.clear();
            expectedDataForSingleThread.add(expectedData.get(i));

            try {
                compare(debuggee.threadByName(threadNames[i]).ownedMonitorsAndFrames(), expectedDataForSingleThread);
            } catch (Exception e) {
                setSuccess(false);
                log.complain("Unexpected exception: " + e);
                e.printStackTrace(log.getOutStream());
            }
        }
    }

    private int threadNameIndex;

    private void createAndCheckDeadlock(LockType... lockTypes) {
        String threadNames[] = new String[lockTypes.length];

        for (int i = 0; i < threadNames.length; i++)
            threadNames[i] = "ownedMonitorsAndFrames004_DeadlockedThread" + (++threadNameIndex);

        String command = ownedMonitorsAndFrames004a.COMMAND_CREATE_DEADLOCK;

        for (int i = 0; i < threadNames.length; i++) {
            command += ":" + threadNames[i] + ":" + lockTypes[i];
        }

        pipe.println(command);

        if (!isDebuggeeReady())
            return;

        forceBreakpoint();

        // check result of ownedMonitorsAndFrames() for deadlocked threads
        checkDeadlockedThread(threadNames);

        debuggee.resume();

        if (!isDebuggeeReady())
            return;
    }

    public void doTest() {
        initDefaultBreakpoint();

        try {
            createAndCheckDeadlock(LockType.SYNCHRONIZED_BLOCK, LockType.SYNCHRONIZED_METHOD);

            createAndCheckDeadlock(LockType.SYNCHRONIZED_BLOCK, LockType.JNI_LOCK);

            createAndCheckDeadlock(LockType.SYNCHRONIZED_BLOCK, LockType.JNI_LOCK, LockType.SYNCHRONIZED_METHOD);
        } finally {
            removeDefaultBreakpoint();
        }

    }
}
