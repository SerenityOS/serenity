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
 * @summary converted from VM Testbase nsk/jdwp/ThreadReference/OwnedMonitorsStackDepthInfo/ownedMonitorsStackDepthInfo002.
 * VM Testbase keywords: [quick, jpda, jdwp, feature_jdk6_jpda, vm6, monitoring]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test performs checking for
 *         command set: ThreadReference
 *         command: OwnedMonitorsStackDepthInfo
 *     Test checks that debuggee accept the command packet and
 *     replies with correct reply packet.
 *     Test consists of two compoments:
 *         debugger: ownedMonitorsStackDepthInfo002
 *         debuggee: nsk.share.jdwp.TestDebuggeeType2
 *     Debugger uses nsk.share support classes to launch debuggee
 *     and obtain Transport object, that represents JDWP transport channel.
 *     Also communication channel (IOPipe) is established between
 *     debugger and debuggee to exchange with execution commands.
 *     Test performs checks for cases when incorrect data is sent in command.
 *     Following cases are tested:
 *         - create command with -1 as threadID, expect INVALID_OBJECT error
 *         - create command with referenceID instead of threadID, expect INVALID_THREAD error
 *         - try execute command for thread wich has finished execution, expect INVALID_THREAD error
 *     Finally, debugger sends debuggee signal to quit, waits for it exits
 *     and exits too with the proper exit code.
 *
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @run main/othervm
 *      nsk.jdwp.ThreadReference.OwnedMonitorsStackDepthInfo.ownedMonitorsStackDepthInfo002.ownedMonitorsStackDepthInfo002
 *      -arch=${os.family}-${os.simpleArch}
 *      -verbose
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdwp.ThreadReference.OwnedMonitorsStackDepthInfo.ownedMonitorsStackDepthInfo002;

import nsk.share.Consts;
import nsk.share.jdwp.AbstractJDWPDebuggee;
import nsk.share.jdwp.CommandPacket;
import nsk.share.jdwp.JDWP;
import nsk.share.jdwp.TestDebuggerType1;
import nsk.share.jpda.AbstractDebuggeeTest;
import nsk.share.jpda.StateTestThread;

import java.io.PrintStream;

public class ownedMonitorsStackDepthInfo002 extends TestDebuggerType1 {
    protected String getDebugeeClassName() {
        return AbstractJDWPDebuggee.class.getName();
    }

    public static void main(String[] argv) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String[] argv, PrintStream out) {
        return new ownedMonitorsStackDepthInfo002().runIt(argv, out);
    }

    private void sendCommand(long threadID, boolean trySuspend, int errorCode) {
        try {
            int JDWP_COMMAND_ID = JDWP.Command.ThreadReference.OwnedMonitorsStackDepthInfo;

            log.display("Create command: " + JDWP.commandNames.get(JDWP_COMMAND_ID));
            log.display("threadID = " + threadID);

            // suspend thread or not (can't use suspending when create command with invalid threadID)
            if (trySuspend)
                debuggee.suspendThread(threadID);

            CommandPacket command = new CommandPacket(JDWP_COMMAND_ID);
            command.addObjectID(threadID);
            command.setLength();

            log.display("Sending command packet:\n" + command);
            transport.write(command);

            // in this test always expect reply with error
            getReply(command, true, errorCode);
        } catch (Exception e) {
            setSuccess(false);
            log.complain("Caught exception while testing JDWP command: " + e);
            e.printStackTrace(log.getOutStream());
        }
    }

    public void doTest() {
        // create command with threadID = -1, expect INVALID_OBJECT error
        sendCommand(-1, false, JDWP.Error.INVALID_OBJECT);

        // create command with referenceTypeID instead of trheadID, expect INVALID_THREAD error
        sendCommand(debuggee.getReferenceTypeID(createTypeSignature(getDebugeeClassName())), false, JDWP.Error.INVALID_THREAD);

        // create StateTestThread and force this finish execution
        pipe.println(AbstractDebuggeeTest.COMMAND_CREATE_STATETESTTHREAD);

        if (!isDebuggeeReady())
            return;

        // skip one state(thread not running), because of can obtain threadID only for running thread
        pipe.println(AbstractDebuggeeTest.COMMAND_NEXTSTATE_STATETESTTHREAD);

        if (!isDebuggeeReady())
            return;

        long threadID = debuggee.getThreadID(AbstractDebuggeeTest.stateTestThreadName);

        int state = 2;

        // skip all states
        while (state++ < StateTestThread.stateTestThreadStates.length) {
            pipe.println(AbstractDebuggeeTest.COMMAND_NEXTSTATE_STATETESTTHREAD);

            if (!isDebuggeeReady())
                return;
        }

        // send command for thread which has exited expect INVALID_THREAD error
        sendCommand(threadID, true, JDWP.Error.INVALID_THREAD);
    }
}
