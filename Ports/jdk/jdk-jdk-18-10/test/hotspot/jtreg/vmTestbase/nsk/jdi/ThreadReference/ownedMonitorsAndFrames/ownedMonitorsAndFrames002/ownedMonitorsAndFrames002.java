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
 * @summary converted from VM Testbase nsk/jdi/ThreadReference/ownedMonitorsAndFrames/ownedMonitorsAndFrames002.
 * VM Testbase keywords: [quick, jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *         Debuggee VM create thread which acquires several monitors in following ways:
 *                 - entering synchronized method
 *                 - entering synchronized method for thread object itself
 *                 - entering synchronized static method
 *                 - entering synchronized method for thread class itself
 *                 - entering synchronized block on non-static object
 *                 - entering synchronized block on non-static on thread object itself
 *                 - entering synchronized block on static object
 *                 - entering synchronized block on static thread object itself
 *         Information about all monitors acquired by test thread is stored in debuggee VM and can be obtained through
 *         special field in debuggee class: OwnedMonitorsDebuggee.monitorsInfo.
 *         When debuggee's test threads acquires all monitors debugger reads information about acquired monitors
 *         from 'OwnedMonitorsDebuggee.monitorsInfo' and checks that com.sun.jdi.ThreadReference.ownedMonitorsAndFrames
 *         returns correct list of MonitorInfo objects.
 *         Debugger VM forces test thread in debuggee VM sequentially free all monitors(exit from synchronized blocks/methods),
 *         update debug information about acquired monitors and checks that com.sun.jdi.ThreadReference.ownedMonitorsAndFrames
 *         returns correct list of MonitorInfo object.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ThreadReference.ownedMonitorsAndFrames.ownedMonitorsAndFrames002.ownedMonitorsAndFrames002
 * @run main/othervm/native
 *      nsk.jdi.ThreadReference.ownedMonitorsAndFrames.ownedMonitorsAndFrames002.ownedMonitorsAndFrames002
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdi.ThreadReference.ownedMonitorsAndFrames.ownedMonitorsAndFrames002;

import java.util.*;
import java.io.*;
import com.sun.jdi.*;
import nsk.share.Consts;
import nsk.share.jdi.OwnedMonitorsDebuggee;
import nsk.share.jdi.OwnedMonitorsDebugger;
import nsk.share.locks.LockingThread;

public class ownedMonitorsAndFrames002 extends OwnedMonitorsDebugger {
    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new ownedMonitorsAndFrames002().runIt(argv, out);
    }

    public void doTest() {
        initDefaultBreakpoint();

        try {
            // create command:

            List<String> locksTypes = new ArrayList<String>();

            // debuggee thread acquires monitors in different ways
            locksTypes.add(LockingThread.SYNCHRONIZED_METHOD);
            locksTypes.add(LockingThread.SYNCHRONIZED_BLOCK_STATIC_THREAD_OBJECT);
            locksTypes.add(LockingThread.SYNCHRONIZED_THREAD_METHOD);
            locksTypes.add(LockingThread.SYNCHRONIZED_STATIC_THREAD_METHOD);
            locksTypes.add(LockingThread.SYNCHRONIZED_THIS_BLOCK);
            locksTypes.add(LockingThread.SYNCHRONIZED_OBJECT_BLOCK);
            locksTypes.add(LockingThread.SYNCHRONIZED_BLOCK_STATIC_OBJECT);
            locksTypes.add(LockingThread.SYNCHRONIZED_STATIC_METHOD);
            locksTypes.add(LockingThread.FRAME_WITHOUT_LOCK);

            String threadName = "ownedMonitorsAndFrames002_LockingThread1";
            String command = OwnedMonitorsDebuggee.COMMAND_CREATE_LOCKING_THREAD + ":" + threadName;

            for (String lockType : locksTypes) {
                command += ":" + lockType;
            }

            pipe.println(command);

            if (!isDebuggeeReady())
                return;

            ThreadReference threadReference = debuggee.threadByName(threadName);

            // debuggee save information about acquired monitors in array 'OwnedMonitorsDebuggee.monitorsInfo'
            pipe.println(OwnedMonitorsDebuggee.COMMAND_UPDATE_MONITOR_INFO + ":" + threadName);

            if (!isDebuggeeReady())
                return;

            forceBreakpoint();

            // check that ThreadReference.ownedMonitorsAndFrames() returns correct data
            checkMonitorInfo(threadReference);

            debuggee.resume();

            if (!isDebuggeeReady())
                return;

            // force debuggee thread sequentially exit from synchronized blocks/methods and check results of ownedMonitorsAndFrames()
            for (int i = 0; i < locksTypes.size(); i++) {
                pipe.println(OwnedMonitorsDebuggee.COMMAND_EXIT_SINGLE_FRAME + ":" + threadName);

                if (!isDebuggeeReady())
                    return;

                forceBreakpoint();

                // check that ThreadReference.ownedMonitorsAndFrames() returns correct data
                checkMonitorInfo(threadReference);

                debuggee.resume();

                if (!isDebuggeeReady())
                    return;
            }
        } finally {
            removeDefaultBreakpoint();
        }

    }
}
