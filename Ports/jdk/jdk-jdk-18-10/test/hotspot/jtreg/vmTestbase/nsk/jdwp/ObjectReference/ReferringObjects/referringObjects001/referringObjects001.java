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
 * @summary converted from VM Testbase nsk/jdwp/ObjectReference/ReferringObjects/referringObjects001.
 * VM Testbase keywords: [quick, jpda, jdwp, feature_jdk6_jpda, vm6, monitoring]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test performs checking for
 *         command set: ObjectReference
 *         command: ReferringObjects
 *     Test checks that debuggee accept the command packet and
 *     replies with correct reply packet.
 *     Test consists of two compoments:
 *         debugger: referringObjects001
 *         debuggee: referringObjects001a
 *     Debuggee during startup initializes static field 'testInstance' with java.lang.Object instance
 *     and creates objects refer to this instance via references with types which should be supported
 *     by command ObjectReference.ReferringObjects:
 *         - strong reference
 *         - soft reference
 *         - weak reference
 *         - phantom reference
 *     Debugger uses nsk.share support classes to launch debuggee
 *     and obtain Transport object, that represents JDWP transport channel.
 *     Also communication channel (IOPipe) is established between
 *     debugger and debuggee to exchange with execution commands.
 *     For maxReferrers in [1, 0, Interger.MAX_VALUE]
 *     do
 *         Debugger obtains objectID for instance assigned to the debuggee's static field 'testInstance'.
 *         Then, debugger creates command packet for ReferringObjects command with the
 *         found objectID and maxReferrers as an arguments, writes packet to the transport
 *         channel, and waits for a reply packet.
 *         When reply packet is received, debugger parses the packet structure
 *         and extracts number of referring objects and referring objects ids.
 *         Debugger checks that received number of referring objects is correct:
 *             - if maxReferrers=1 only 1 referring object should be returned
 *             - if maxReferrers=0 or maxReferrers=Integer.MAX_VALUE all referring objects should be returned
 *     done
 *     Also, test performs checks for cases when incorrect data is sent in command.
 *     Following cases are tested:
 *         - create command with maxReferrers=-1, expect ILLEGAL_ARGUMENT error
 *         - create command with objectID = -1, expect INVALID_OBJECT error
 *     Finally, debugger sends debuggee signal to quit, waits for it exits
 *     and exits too with the proper exit code.
 *
 * @requires !vm.graal.enabled
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @run main/othervm/native
 *      nsk.jdwp.ObjectReference.ReferringObjects.referringObjects001.referringObjects001
 *      -arch=${os.family}-${os.simpleArch}
 *      -verbose
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="-Xmx256M ${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdwp.ObjectReference.ReferringObjects.referringObjects001;

import nsk.share.Consts;
import nsk.share.jdwp.CommandPacket;
import nsk.share.jdwp.JDWP;
import nsk.share.jdwp.ReplyPacket;
import nsk.share.jdwp.TestDebuggerType1;

import java.io.PrintStream;

public class referringObjects001 extends TestDebuggerType1 {

    protected String getDebugeeClassName() {
        return nsk.jdwp.ObjectReference.ReferringObjects.referringObjects001.referringObjects001a.class.getName();
    }

    public static void main(String[] argv) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String[] argv, PrintStream out) {
        return new referringObjects001().runIt(argv, out);
    }

    private void testCommand(long objectID, int maxReferrers, int expectedReferrersCount, boolean expectError, int errorCode) {
        try {
            int JDWP_COMMAND_ID = JDWP.Command.ObjectReference.ReferringObjects;

            log.display("Create command: " + JDWP.commandNames.get(JDWP_COMMAND_ID));
            log.display("objectID = " + objectID);
            log.display("maxReferrers = " + maxReferrers);

            CommandPacket command = new CommandPacket(JDWP_COMMAND_ID);
            command.addObjectID(objectID);
            command.addInt(maxReferrers);

            log.display("Sending command packet:\n" + command);
            transport.write(command);

            ReplyPacket reply;

            reply = getReply(command, expectError, errorCode);

            if (expectError)
                return;

            int referringObjects = reply.getInt();
            log.display("referringObjects = " + referringObjects);

            // check that correct 'referringObjects' value was received
            if (referringObjects != expectedReferrersCount) {
                log.complain("Unexpected value 'referringObjects': " + referringObjects + " expected is " + expectedReferrersCount);
                setSuccess(false);
            }

            for (int i = 0; i < referringObjects; i++) {
                JDWP.Value value = reply.getValue();
                log.display("tagged-ObjectID = " + value);
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
        /*
         * Provoke GC here to be sure that GC won't start during test execution
         */
        forceGC();
        pipe.println(referringObjects001a.COMMAND_CREATE_TEST_INSTANCES);

        if (!isDebuggeeReady())
            return;


        int referrersCount = referringObjects001a.expectedCount;

        long objectID = queryObjectID(debuggee.getReferenceTypeID(createTypeSignature(getDebugeeClassName())), "testInstance", JDWP.Tag.OBJECT);

        // create command with maxReferrers=1, only 1 referrer should be returned
        testCommand(objectID, 1, 1, false, 0);
        // create command with maxReferrers=0, all referrers should be returned
        testCommand(objectID, 0, referrersCount, false, 0);
        // create command with maxReferrers=Integer.MAX_VALUE, all referrers should be returned
        testCommand(objectID, Integer.MAX_VALUE, referrersCount, false, 0);

        // create command with maxReferrers=-1, expect ILLEGAL_ARGUMENT error
        testCommand(objectID, -1, referrersCount, true, JDWP.Error.ILLEGAL_ARGUMENT);

        // create command with objectID = -1, expect INVALID_OBJECT error
        testCommand(-1, Integer.MAX_VALUE, referrersCount, true, JDWP.Error.INVALID_OBJECT);

        resetStatusIfGC();
    }
}
