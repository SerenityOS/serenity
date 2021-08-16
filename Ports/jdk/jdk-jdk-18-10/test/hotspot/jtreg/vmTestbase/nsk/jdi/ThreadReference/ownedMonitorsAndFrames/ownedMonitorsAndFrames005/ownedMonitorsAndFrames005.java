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
 * @summary converted from VM Testbase nsk/jdi/ThreadReference/ownedMonitorsAndFrames/ownedMonitorsAndFrames005.
 * VM Testbase keywords: [quick, jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *         Test checks that acquired JNI monitors are handled correctly. Possibility to return JNI monitors related to 6415394,
 *         it should be fixed in Mustang b85. If JNI monitors should be returned by ThreadReference.ownedMonitorsAndFrames()
 *         run test with parameter '-expectJNIMonitors', otherwise run test without parameters.
 *         Test scenario:
 *         Debuggee VM creates thread which acquires several JNI monitors(through JNI MonitorEnter()) and
 *         one monitor through entering synchronized block.
 *         Information about all monitors acquired by test thread is stored in debuggee VM and can be obtained through
 *         special field in debuggee class: OwnedMonitorsDebuggee.monitorsInfo.
 *         Debugger VM reads information about acquired monitors from 'OwnedMonitorsDebuggee.monitorsInfo'
 *         and checks that com.sun.jdi.ThreadReference.ownedMonitorsAndFrames returns correct list of MonitorInfo objects.
 *         Debugger VM forces test thread in debuggee VM sequentially free all monitors(exit from synchronized block or
 *         call JNI MonitorExit()), updates debug information about acquired monitors and checks that com.sun.jdi.ThreadReference.ownedMonitorsAndFrames
 *         returns correct list of MonitorInfo objects.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ThreadReference.ownedMonitorsAndFrames.ownedMonitorsAndFrames005.ownedMonitorsAndFrames005
 * @run main/othervm/native
 *      nsk.jdi.ThreadReference.ownedMonitorsAndFrames.ownedMonitorsAndFrames005.ownedMonitorsAndFrames005
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 *      -expectJNIMonitors
 */

package nsk.jdi.ThreadReference.ownedMonitorsAndFrames.ownedMonitorsAndFrames005;

import java.io.*;
import java.util.*;
import com.sun.jdi.*;
import nsk.share.Consts;
import nsk.share.jdi.OwnedMonitorsDebuggee;
import nsk.share.jdi.OwnedMonitorsDebugger;
import nsk.share.locks.LockingThread;

public class ownedMonitorsAndFrames005 extends OwnedMonitorsDebugger {
    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new ownedMonitorsAndFrames005().runIt(argv, out);
    }

    // expect or not JNI monitors in results of ownedMonitorsAndFrames()
    private boolean expectJNIMonitors;

    protected String[] doInit(String args[], PrintStream out) {
        args = super.doInit(args, out);

        ArrayList<String> standardArgs = new ArrayList<String>();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-expectJNIMonitors")) {
                expectJNIMonitors = true;
            } else
                standardArgs.add(args[i]);
        }

        return standardArgs.toArray(new String[] {});
    }

    public String debuggeeClassName() {
        if (expectJNIMonitors)
            return super.debuggeeClassName() + " -returnJNIMonitors";
        else
            return super.debuggeeClassName();
    }

    public void doTest() {
        initDefaultBreakpoint();

        try {
            List<String> locksTypes = new ArrayList<String>();

            // LockingThead should acquire 3 JNI monitors and 1 non-JNI monitor
            locksTypes.add(LockingThread.SYNCHRONIZED_OBJECT_BLOCK);
            locksTypes.add(LockingThread.JNI_MONITOR_ENTER);
            locksTypes.add(LockingThread.JNI_MONITOR_ENTER);
            locksTypes.add(LockingThread.JNI_MONITOR_ENTER);

            String threadName = "ownedMonitorsAndFrames005_LockingThread1";
            String command = OwnedMonitorsDebuggee.COMMAND_CREATE_LOCKING_THREAD + ":" + threadName;

            for (String lockType : locksTypes) {
                command += ":" + lockType;
            }

            pipe.println(command);

            if (!isDebuggeeReady())
                return;

            forceBreakpoint();

            ThreadReference threadReference = debuggee.threadByName(threadName);

            // check ownedMonitorsAndFrames() first time
            checkMonitorInfo(threadReference);

            debuggee.resume();

            if (!isDebuggeeReady())
                return;

            // force debuggee thread sequentially exit from synchronized blocks, free JNI monitors and check results of ownedMonitorsAndFrames()
            for (int i = 0; i < locksTypes.size(); i++) {
                pipe.println(OwnedMonitorsDebuggee.COMMAND_EXIT_SINGLE_FRAME + ":" + threadName);

                if (!isDebuggeeReady())
                    return;

                forceBreakpoint();

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
