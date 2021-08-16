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
 * @summary converted from VM Testbase nsk/jdwp/ObjectReference/ReferringObjects/referringObjects002.
 * VM Testbase keywords: [quick, jpda, jdwp, feature_jdk6_jpda, vm6, monitoring]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test performs checking for
 *         command set: ObjectReference
 *         command: ReferringObjects
 *     Test checks that debuggee accept the command packet and
 *     replies with correct reply packet.
 *     Test consists of two compoments:
 *         debugger: referringObjects002
 *         debuggee: referringObjects002a
 *     Debuggee contains static field 'testInstance' which initialized with java.lang.Object instance.
 *     Also debuggee contains 6 static fields with names referringObject1, ..., referringObject6, this fields
 *     are initialized with objects which refer to 'testInstance' and there are no more objects referring to
 *     the 'testInstance'.
 *     Debugger uses nsk.share support classes to launch debuggee
 *     and obtain Transport object, that represents JDWP transport channel.
 *     Also communication channel (IOPipe) is established between
 *     debugger and debuggee to exchange with execution commands.
 *     Debugger obtains objectID for instance assigned to the debuggee's static field 'testInstance'.
 *     Then, debugger creates command packet for ReferringObjects command with the
 *     found objectID and maxReferrers=0 as an arguments, writes packet to the transport
 *         channel, and waits for a reply packet.
 *     When reply packet is received, debugger parses the packet structure
 *     and extracts number of referring objects and referring objects ids.
 *     Debugger checks that number of referring objects is correct - 6.
 *     Debugger obtains objectIDs for object instances stored in debuggee's fields referringObject1, ..., referringObject6
 *     and checks that this values and referring objects ids received via JDWP command are identical.
 *     Finally, debugger sends debuggee signal to quit, waits for it exits
 *     and exits too with the proper exit code.
 *
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @run main/othervm/native
 *      nsk.jdwp.ObjectReference.ReferringObjects.referringObjects002.referringObjects002
 *      -arch=${os.family}-${os.simpleArch}
 *      -verbose
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdwp.ObjectReference.ReferringObjects.referringObjects002;

import nsk.share.Consts;
import nsk.share.jdwp.CommandPacket;
import nsk.share.jdwp.JDWP;
import nsk.share.jdwp.ReplyPacket;
import nsk.share.jdwp.TestDebuggerType1;

import java.io.PrintStream;

public class referringObjects002 extends TestDebuggerType1 {
    protected String getDebugeeClassName() {
        return nsk.jdwp.ObjectReference.ReferringObjects.referringObjects002.referringObjects002a.class.getName();
    }

    public static void main(String[] argv) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String[] argv, PrintStream out) {
        return new referringObjects002().runIt(argv, out);
    }

    private void testCommand() {
        try {
            int JDWP_COMMAND_ID = JDWP.Command.ObjectReference.ReferringObjects;

            long objectID = queryObjectID(
                    debuggee.getReferenceTypeID(createTypeSignature(getDebugeeClassName())),
                    "testInstance",
                    JDWP.Tag.OBJECT);

            // create command with maxReferrers=0 (receive all referrers)
            log.display("Create command: " + JDWP.commandNames.get(JDWP_COMMAND_ID));
            log.display("objectID = " + objectID);
            log.display("maxReferrers = " + 0);

            CommandPacket command = new CommandPacket(JDWP_COMMAND_ID);
            command.addObjectID(objectID);
            command.addInt(0);

            log.display("Sending command packet:\n" + command);
            transport.write(command);

            ReplyPacket reply;

            reply = getReply(command);

            int referringObjects = reply.getInt();
            log.display("referringObjects = " + referringObjects);

            // there are 6 referrers in debuggee
            int expectedReferrersCount = 6;

            if (referringObjects != expectedReferrersCount) {
                log.complain("Unexpected value 'referringObjects': " + referringObjects + " expected is " + expectedReferrersCount);
                setSuccess(false);
            }

            long[] expectedReferrersID = new long[expectedReferrersCount];

            // initialize expected IDs of referrers
            for (int i = 0; i < expectedReferrersCount; i++) {
                expectedReferrersID[i] = queryObjectID(debuggee.getReferenceTypeID(createTypeSignature(getDebugeeClassName())), "referringObject"
                        + (i + 1));
            }

            long[] receivedReferrersID = new long[referringObjects];

            for (int i = 0; i < referringObjects; i++) {
                JDWP.Value value = reply.getValue();
                log.display("tagged-ObjectID = " + value);

                receivedReferrersID[i] = (Long) value.getValue();
            }

            // check that correct IDs of referrers was received
            for (int i = 0; i < referringObjects; i++) {
                boolean isIDExpected = false;

                for (long l : expectedReferrersID) {
                    if (receivedReferrersID[i] == l) {
                        isIDExpected = true;
                        break;
                    }
                }

                if (!isIDExpected) {
                    setSuccess(false);
                    log.complain("Unexpected 'referrerID' value: " + receivedReferrersID[i]);
                }
            }

            if (!getSuccess()) {
                log.complain("Expected IDs:");
                for (long l : expectedReferrersID) {
                    log.complain("" + l);
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
