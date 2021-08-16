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
 * @summary converted from VM Testbase nsk/jdwp/ReferenceType/Instances/instances001.
 * VM Testbase keywords: [quick, jpda, jdwp, feature_jdk6_jpda, vm6, monitoring]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test performs checking for
 *         command set: ReferenceType
 *         command: Instances
 *     Test checks that debuggee accept the command packet and
 *     replies with correct reply packet.
 *     Test consists of two compoments:
 *         debugger: instances001
 *         debuggee: instances001a
 *     Debugger uses nsk.share support classes to launch debuggee
 *     and obtain Transport object, that represents JDWP transport channel.
 *     Also communication channel (IOPipe) is established between
 *     debugger and debuggee to exchange with execution commands.
 *     For maxInstances in [1, 0, Interger.MAX_VALUE]
 *     do
 *         Debugger obtains referenceTypeID for 'nsk.share.jdwp.ReferenceType.instances.instances001.TestClass'.
 *         Then, debugger creates command packet for Instances command with the
 *         found referenceTypeID and maxInstances as an arguments, writes packet to the transport
 *         channel, and waits for a reply packet.
 *         When reply packet is received, debugger parses the packet structure
 *         and extracts number of instances and instance's ids.
 *         Debugger checks that received number of instances is correct:
 *             - if maxInstances=1 only 1 instance should be returned
 *             - if maxInstances=0 or maxInstances=Integer.MAX_VALUE all instances should be returned
 *     done
 *     Also, test performs checks for cases when incorrect data is sent in command.
 *     Following cases are tested:
 *         - create command with maxInstances < 0, expect ILLEGAL_ARGUMENT error
 *         - create command with typeID = -1, expect INVALID_OBJECT error
 *         - create command with threadID instead of referenceTypeID, expect INVALID_CLASS error
 *     Finally, debugger sends debuggee signal to quit, waits for it exits
 *     and exits too with the proper exit code.
 *
 * @requires vm.gc != "Z"
 * @requires !vm.graal.enabled
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @run main/othervm/native
 *      nsk.jdwp.ReferenceType.Instances.instances001.instances001
 *      -arch=${os.family}-${os.simpleArch}
 *      -verbose
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="-Xmx128M ${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdwp.ReferenceType.Instances.instances001;

import nsk.share.Consts;
import nsk.share.jdwp.CommandPacket;
import nsk.share.jdwp.JDWP;
import nsk.share.jdwp.ReplyPacket;
import nsk.share.jdwp.TestDebuggerType1;

import java.io.PrintStream;

public class instances001 extends TestDebuggerType1 {
    protected String getDebugeeClassName() {
        return nsk.jdwp.ReferenceType.Instances.instances001.instances001a.class.getName();
    }

    public static void main(String[] argv) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String[] argv, PrintStream out) {
        return new instances001().runIt(argv, out);
    }

    private void testClass(long typeID, int maxInstances, int expectedInstances, boolean expectError, int errorCode) {
        try {
            int JDWP_COMMAND_ID = JDWP.Command.ReferenceType.Instances;

            log.display("Create command: " + JDWP.commandNames.get(JDWP_COMMAND_ID));
            log.display("referenceType = " + typeID);
            log.display("maxInstances = " + maxInstances);

            CommandPacket command = new CommandPacket(JDWP_COMMAND_ID);
            command.addReferenceTypeID(typeID);
            command.addInt(maxInstances);
            command.setLength();

            log.display("Sending command packet:\n" + command);
            transport.write(command);

            ReplyPacket reply;

            reply = getReply(command, expectError, errorCode);

            if (expectError)
                return;

            int instances = reply.getInt();
            log.display("instances = " + instances);

            // check that correct value of 'instances' was received
            if (instances != expectedInstances) {
                setSuccess(false);
                log.complain("Unexpected 'instances' value: " + instances + ", expected is " + expectedInstances);
            }

            for (int i = 0; i < instances; i++) {
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
        // force GC in debuggee VM to avoid collection of weak references during test execution
        forceGC();
        pipe.println(instances001a.COMMAND_CREATE_TEST_INSTANCES);

        if (!isDebuggeeReady())
            return;

        int expectedInstances = instances001a.expectedCount;

        String testClassName = nsk.jdwp.ReferenceType.Instances.instances001.TestClass.class.getName();

        long typeID = debuggee.getReferenceTypeID(createTypeSignature(testClassName));

        // Note! This test is broken, in the sense that it incorrectly assumes
        // that no GC can happen before it walks the heap. In practice, it seems
        // to only affect this test when using ZGC. However, this test will also
        // fail when using other GCs if an explicit GC is done here.

        // create command with maxInstances=1, only 1 instance should be returned
        testClass(typeID, 1, 1, false, 0);
        // create command with maxInstances=0, all instances should be returned
        testClass(typeID, 0, expectedInstances, false, 0);
        // create command with maxInstances=Integer.MAX_VALUE, all instances should be returned
        testClass(typeID, Integer.MAX_VALUE, expectedInstances, false, 0);

        // create command with maxInstances < 0, expect ILLEGAL_ARGUMENT error
        testClass(typeID, -1, expectedInstances, true, JDWP.Error.ILLEGAL_ARGUMENT);

        // create command with typeID = 1, expect INVALID_OBJECT error
        testClass(-1, Integer.MAX_VALUE, expectedInstances, true, JDWP.Error.INVALID_OBJECT);

        // create command with threadID instead of referenceTypeID, expect INVALID_CLASS error
        testClass(debuggee.getThreadID("main"), Integer.MAX_VALUE, expectedInstances, true, JDWP.Error.INVALID_CLASS);

        // if GC occurs during test the results should be ignored
        resetStatusIfGC();
    }
}
