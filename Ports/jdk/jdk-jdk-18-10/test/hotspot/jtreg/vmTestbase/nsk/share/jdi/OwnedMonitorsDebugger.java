/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.share.jdi;

import java.io.*;
import java.util.*;
import com.sun.jdi.*;

import nsk.share.Consts;

/*
 *  Class is used as base debugger in tests for ThreadReference.ownedMonitorsAndFrames().
 *
 *  In all this test similar scenario is used:
 *      - debugger VM force debugge VM to create test thread which acquires different monitors
 *      - when test thread acquire all monitors debuggee save information about all acquired monitors in special array 'monitorsInfo'
 *      - debugger read data from 'monitorsInfo' and compare it with data returned by ThreadReference.ownedMonitorsAndFrames()
 */
public class OwnedMonitorsDebugger extends TestDebuggerType2 {

    /*
     * debug data about monitor acquired by debuggee test thread,
     * intended to compare with data returned by ThreadReference.ownedMonitorsAndFrames
     */
    public static class DebugMonitorInfo {
        // create DebugMonitorInfo using mirror of instance of nsk.share.locks.LockingThread.DebugMonitorInfo
        DebugMonitorInfo(ObjectReference debuggeeMirror) {
            monitor = (ObjectReference) debuggeeMirror.getValue(debuggeeMirror.referenceType().fieldByName("monitor"));
            stackDepth = ((IntegerValue) debuggeeMirror.getValue(debuggeeMirror.referenceType().fieldByName("stackDepth"))).intValue();
            thread = (ThreadReference) debuggeeMirror.getValue(debuggeeMirror.referenceType().fieldByName("thread"));
        }

        public DebugMonitorInfo(ObjectReference monitor, int stackDepth, ThreadReference thread) {
            this.monitor = monitor;
            this.stackDepth = stackDepth;
            this.thread = thread;
        }

        public ObjectReference monitor;

        public int stackDepth;

        public ThreadReference thread;
    }

    public static void main(String argv[]) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new OwnedMonitorsDebugger().runIt(argv, out);
    }

    protected boolean canRunTest() {
        if (!vm.canGetMonitorFrameInfo()) {
            log.display("TEST CANCELED due to: vm.canGetMonitorFrameInfo() = false");
            return false;
        } else
            return super.canRunTest();
    }

    protected String debuggeeClassName() {
        return OwnedMonitorsDebuggee.class.getName();
    }

    // read debuggee's array 'monitorsInfo' containing information about acquired monitors
    protected List<DebugMonitorInfo> getDebugMonitorsInfo() {
        List<DebugMonitorInfo> result = new ArrayList<DebugMonitorInfo>();

        ReferenceType referenceType = debuggee.classByName(debuggeeClassNameWithoutArgs());
        ArrayReference monitorsInfo = (ArrayReference) referenceType.getValue(referenceType.fieldByName("monitorsInfo"));

        for (int i = 0; i < monitorsInfo.length(); i++)
            result.add(new DebugMonitorInfo((ObjectReference) monitorsInfo.getValue(i)));

        return result;
    }

    private boolean compare(MonitorInfo actual, DebugMonitorInfo expected) {
        boolean success = true;

        if (actual.stackDepth() != expected.stackDepth) {
            setSuccess(false);
            success = false;
            log.complain("Expected and actual monitor(" + actual.monitor() + ") stack depth differs, expected: " + expected.stackDepth + " actual: "
                    + actual.stackDepth());
        }

        if (!actual.thread().equals(expected.thread)) {
            setSuccess(false);
            success = false;
            log.complain("Expected and actual monitor(" + actual.monitor() + " thread differs, expected: " + expected.thread + " actual: "
                    + actual.thread());
        }

        return success;
    }

    protected void compare(List<MonitorInfo> actualData, List<DebugMonitorInfo> expectedData) {
        boolean success = true;

        // compare total amount of monitors
        if (actualData.size() != expectedData.size()) {
            setSuccess(false);
            success = false;
            log.complain("Number of expected monitors and actual ones differs");
            log.complain("Expected: " + expectedData.size() + ", actual: " + actualData.size());
        }

        // check that all expected monitors are contained in 'actualData'
        for (DebugMonitorInfo expectedMonitorInfo : expectedData) {
            boolean isMonitorFound = false;

            for (MonitorInfo actualMonitorInfo : actualData) {
                if (expectedMonitorInfo.monitor.equals(actualMonitorInfo.monitor())) {
                    isMonitorFound = true;

                    if (!compare(actualMonitorInfo, expectedMonitorInfo))
                        success = false;

                    break;
                }
            }

            if (!isMonitorFound) {
                setSuccess(false);
                success = false;
                log.complain("Expected monitor not found in result of ownedMonitorsAndFrames(): " + expectedMonitorInfo.monitor);
            }
        }

        // check that all monitors from 'actualData' are contained in
        // 'expectedData'
        for (MonitorInfo actualMonitorInfo : actualData) {
            boolean isMonitorFound = false;

            for (DebugMonitorInfo expectedMonitorInfo : expectedData) {
                if (actualMonitorInfo.monitor().equals(expectedMonitorInfo.monitor)) {
                    isMonitorFound = true;
                    break;
                }
            }

            if (!isMonitorFound) {
                setSuccess(false);
                success = false;
                log.complain("Unexpected monitor in result of ownedMonitorsAndFrames(): " + actualMonitorInfo.monitor() + " Depth: "
                        + actualMonitorInfo.stackDepth() + " Thread: " + actualMonitorInfo.thread());
            }
        }

        if (!success)
            logDebugInfo(actualData, expectedData);
    }

    private void logDebugInfo(List<MonitorInfo> actualData, List<DebugMonitorInfo> expectedData) {
        log.display("ACTUAL MONITORS (total " + actualData.size() + "):");

        ThreadReference thread = null;

        for (MonitorInfo monitorInfo : actualData) {
            log.display("Monitor: " + monitorInfo.monitor());
            log.display("Depth: " + monitorInfo.stackDepth());
            log.display("Thread: " + monitorInfo.thread());
            if (thread == null)
                thread =  monitorInfo.thread();
        }

        log.display("EXPECTED MONITORS (total " + expectedData.size() + "):");

        for (DebugMonitorInfo monitorInfo : expectedData) {
            log.display("Monitor: " + monitorInfo.monitor);
            log.display("Depth: " + monitorInfo.stackDepth);
            log.display("Thread: " + monitorInfo.thread);
            if (thread == null)
                thread =  monitorInfo.thread;
        }

        if (thread != null) {
            try {
                log.display("Thread frames:");
                for (StackFrame frame : thread.frames()) {
                    Location location = frame.location();
                    log.display(location.declaringType().name() + "." + location.method().name() + ", line: " + location.lineNumber());
                }
            } catch (Exception e) {
                unexpectedException(e);
            }
        }
    }

    /*
     * Check that ThreadReference.ownedMonitorsAndFrames() returns correct
     * data before calling this method debuggee should save information about
     * acquired monitors in special array 'monitorsInfo',
     * debugger forces debuggee to do it using command 'COMMAND_UPDATE_MONITOR_INFO'
     */
    protected void checkMonitorInfo(ThreadReference threadReference) {
        List<MonitorInfo> actualData = null;

        try {
            actualData = threadReference.ownedMonitorsAndFrames();
        } catch (Exception e) {
            setSuccess(false);
            log.complain("Unexpected exception: " + e);
            e.printStackTrace(log.getOutStream());
        }

        List<DebugMonitorInfo> expectedData = getDebugMonitorsInfo();

        compare(actualData, expectedData);
    }

}
