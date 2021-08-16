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
 * @summary converted from VM Testbase nsk/jdwp/ThreadReference/OwnedMonitorsStackDepthInfo/ownedMonitorsStackDepthInfo001.
 * VM Testbase keywords: [quick, jpda, jdwp, feature_jdk6_jpda, vm6, monitoring]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test performs checking for
 *         command set: ThreadReference
 *         command: OwnedMonitorsStackDepthInfo
 *     Test checks that debuggee accept the command packet and
 *     replies with correct reply packet.
 *     Test consists of two compoments:
 *         debugger: ownedMonitorsStackDepthInfo001
 *         debuggee: ownedMonitorsStackDepthInfo001a
 *     Debugger uses nsk.share support classes to launch debuggee
 *     and obtain Transport object, that represents JDWP transport channel.
 *     Also communication channel (IOPipe) is established between
 *     debugger and debuggee to exchange with execution commands.
 *     Debuggee during startup start test thread which acquires 6 different monitors in following ways:
 *         - entering synchronized method
 *         - entering synchronized method for thread object itself
 *         - entering synchronized static method
 *         - entering synchronized method for thread class itself
 *         - entering synchronized block on non-static object
 *         - acquire JNI monitor through JNI MonitorEnter()
 *     Debuggee save information about acquired monitors(monitor instance and stack depth location
 *     where monitor was acquired) in static fields with names monitor1, ..., monitor6 and depth1, ..., depth6 to
 *     simplify access to this information for debugger.
 *     Debugger obtains threadID for test thread.
 *     Debugger creates command packet for OwnedMonitorsStackDepthInfo command with the
 *     found threadID as an argument, writes packet to the transport channel, and waits for a reply packet.
 *     When reply packet is received, debugger parses the packet structure and extract
 *     monitors ids and stack depth locations.
 *     Debugger obtains information about acquired monitors from debuggee's static fields monitor1, ..., monitor6 and
 *     depth1, ..., depth6 and checks that this data and data received via JDWP command are identical.
 *     Finally, debugger sends debuggee signal to quit, waits for it exits
 *     and exits too with the proper exit code.
 *
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @run main/othervm/native/timeout=420
 *      nsk.jdwp.ThreadReference.OwnedMonitorsStackDepthInfo.ownedMonitorsStackDepthInfo001.ownedMonitorsStackDepthInfo001
 *      -arch=${os.family}-${os.simpleArch}
 *      -verbose
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdwp.ThreadReference.OwnedMonitorsStackDepthInfo.ownedMonitorsStackDepthInfo001;

import nsk.share.Consts;
import nsk.share.jdwp.CommandPacket;
import nsk.share.jdwp.JDWP;
import nsk.share.jdwp.ReplyPacket;
import nsk.share.jdwp.TestDebuggerType1;

import java.io.PrintStream;

public class ownedMonitorsStackDepthInfo001 extends TestDebuggerType1 {
    protected String getDebugeeClassName() {
        return nsk.jdwp.ThreadReference.OwnedMonitorsStackDepthInfo.ownedMonitorsStackDepthInfo001.ownedMonitorsStackDepthInfo001a.class.getName();
    }

    public static void main(String[] argv) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String[] argv, PrintStream out) {
        return new ownedMonitorsStackDepthInfo001().runIt(argv, out);
    }

    // information for acquired monitor
    static class MonitorInfo {
        long monitorObjectID;

        int depth;

        public MonitorInfo(long monitorObjectID, int depth) {
            this.monitorObjectID = monitorObjectID;
            this.depth = depth;
        }
    }

    private void testCommand() {
        try {
            int JDWP_COMMAND_ID = JDWP.Command.ThreadReference.OwnedMonitorsStackDepthInfo;

            log.display("Create command: " + JDWP.commandNames.get(JDWP_COMMAND_ID));

            long threadID = debuggee.getThreadID(ownedMonitorsStackDepthInfo001a.lockingThreadName);
            log.display("threadID = " + threadID);

            debuggee.suspendThread(threadID);

            CommandPacket command = new CommandPacket(JDWP_COMMAND_ID);
            command.addObjectID(threadID);
            command.setLength();

            log.display("Sending command packet:\n" + command);
            transport.write(command);

            ReplyPacket reply;

            reply = getReply(command);

            MonitorInfo[] expectedMonitors = new MonitorInfo[ownedMonitorsStackDepthInfo001a.expectedMonitorCounts];

            long classID = debuggee.getReferenceTypeID(createTypeSignature(getDebugeeClassName()));

            // obtain information about aquired monitors
            for (int i = 0; i < ownedMonitorsStackDepthInfo001a.expectedMonitorCounts; i++) {
                long monitorID = queryObjectID(classID, "monitor" + (i + 1));
                int depth = (Integer) debuggee.getStaticFieldValue(classID, "depth" + (i + 1), JDWP.Tag.INT).getValue();

                expectedMonitors[i] = new MonitorInfo(monitorID, depth);
            }

            int owned = reply.getInt();

            log.display("owned = " + owned);

            // check that correct value of 'owned' was received
            if (owned != expectedMonitors.length) {
                setSuccess(false);
                log.complain("Unexpected value of 'owned': " + owned + ", expected value is " + expectedMonitors.length);
            }

            MonitorInfo[] receivedMonitors = new MonitorInfo[owned];

            for (int i = 0; i < owned; i++) {
                JDWP.Value value = reply.getValue();
                log.display("tagged-ObjectID = " + value);

                int stack_depth = reply.getInt();
                log.display("stack_depth = " + stack_depth);

                receivedMonitors[i] = new MonitorInfo((Long) value.getValue(), stack_depth);
            }

            // check that correct information about acquired monitors was received
            for (int i = 0; i < owned; i++) {
                boolean monitorFound = false;

                for (MonitorInfo expectedMonitor : expectedMonitors) {
                    if (receivedMonitors[i].monitorObjectID == expectedMonitor.monitorObjectID) {
                        monitorFound = true;

                        if (receivedMonitors[i].depth != expectedMonitor.depth) {
                            setSuccess(false);
                            log.complain("Unexpected monitor depth for monitor " + receivedMonitors[i].monitorObjectID + ": "
                                    + receivedMonitors[i].depth + ", expected value is " + expectedMonitor.depth);
                        }

                        break;
                    }
                }

                if (!monitorFound) {
                    setSuccess(false);
                    log.complain("Unexpected monitor: monitor" + receivedMonitors[i].monitorObjectID + " stack_depth" + receivedMonitors[i].depth);
                }
            }

            if (!getSuccess()) {
                log.complain("Expected monitors: ");
                for (MonitorInfo expectedMonitor : expectedMonitors) {
                    log.complain("monitor: " + expectedMonitor.monitorObjectID + " " + expectedMonitor.depth);
                }
            }

            if (!reply.isParsed()) {
                setSuccess(false);
                log.complain("Extra trailing bytes found in reply packet at: " + reply.currentPosition());
            }
        } catch (Exception e) {
            setSuccess(false);
            log.complain("Caught exception while testing JDWP command: " + e);
            e.printStackTrace(log.getOutStream());
        }
    }

    public void doTest() {
        testCommand();
    }
}
