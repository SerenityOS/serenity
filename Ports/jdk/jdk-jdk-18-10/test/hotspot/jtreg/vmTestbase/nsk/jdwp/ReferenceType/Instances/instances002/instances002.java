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
 * @summary converted from VM Testbase nsk/jdwp/ReferenceType/Instances/instances002.
 * VM Testbase keywords: [quick, jpda, jdwp, feature_jdk6_jpda, vm6, monitoring]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test performs checking for
 *         command set: ReferenceType
 *         command: Instances
 *     Test checks that debuggee accept the command packet and
 *     replies with correct reply packet.
 *     Test consists of two compoments:
 *         debugger: instances002
 *         debuggee: instances002a
 *     Debuggee contains 5 static fields with names instance1, ..., instance5 initialized with
 *     different instances of 'nsk.jdwp.ReferenceType.Instances.instances002.TestClass' and
 *     there are no more instances of 'nsk.jdwp.ReferenceType.Instances.instances002.TestClass' in debugee VM.
 *     Debugger uses nsk.share support classes to launch debuggee
 *     and obtain Transport object, that represents JDWP transport channel.
 *     Also communication channel (IOPipe) is established between
 *     debugger and debuggee to exchange with execution commands.
 *     Debugger obtains referenceTypeID for 'nsk.jdwp.ReferenceType.Instances.instances002.TestClass'.
 *     Then, debugger creates command packet for Instances command with the
 *     found referenceTypeID and maxInstances as an arguments, writes packet to the transport
 *     channel, and waits for a reply packet.
 *     When reply packet is received, debugger parses the packet structure
 *     and extracts number of instances and instance's ids.
 *     Debugger checks that received number of instances is correct.
 *     Debugger obtains objectIDs for object instances stored in debuggee's fields instance1, ..., instance5
 *     and checks that this values and instances ids received via JDWP command are identical.
 *     Finally, debugger sends debuggee signal to quit, waits for it exits
 *     and exits too with the proper exit code.
 *
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @run main/othervm
 *      nsk.jdwp.ReferenceType.Instances.instances002.instances002
 *      -arch=${os.family}-${os.simpleArch}
 *      -verbose
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdwp.ReferenceType.Instances.instances002;

import nsk.share.Consts;
import nsk.share.jdwp.CommandPacket;
import nsk.share.jdwp.JDWP;
import nsk.share.jdwp.ReplyPacket;
import nsk.share.jdwp.TestDebuggerType1;

import java.io.PrintStream;

public class instances002 extends TestDebuggerType1 {
    protected String getDebugeeClassName() {
        return nsk.jdwp.ReferenceType.Instances.instances002.instances002a.class.getName();
    }

    public static void main(String[] argv) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String[] argv, PrintStream out) {
        return new instances002().runIt(argv, out);
    }

    private void testClass(String className, int maxInstances, int expectedInstances) {
        try {
            int JDWP_COMMAND_ID = JDWP.Command.ReferenceType.Instances;

            long typeID = debuggee.getReferenceTypeID(className);

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

            reply = getReply(command);

            int instances = reply.getInt();
            log.display("instances = " + instances);

            // check that correct value of 'instances' was received
            if (instances != expectedInstances) {
                setSuccess(false);
                log.complain("Unexpected 'instances' value: " + instances + ", expected is " + expectedInstances);
            }

            long[] expectedInstancesID = new long[expectedInstances];

            // initialize expected IDs of instances
            for (int i = 0; i < expectedInstances; i++) {
                expectedInstancesID[i] = queryObjectID(debuggee.getReferenceTypeID(createTypeSignature(getDebugeeClassName())), "instance" + (i + 1),
                        JDWP.Tag.OBJECT);
            }

            long[] receivedInstancesID = new long[instances];

            for (int i = 0; i < instances; i++) {
                JDWP.Value value = reply.getValue();
                log.display("tagged-ObjectID = " + value);

                receivedInstancesID[i] = (Long) value.getValue();
            }

            // check that correct IDs of instances was received
            for (int i = 0; i < instances; i++) {
                boolean isIDExpected = false;

                for (long l : expectedInstancesID) {
                    if (receivedInstancesID[i] == l) {
                        isIDExpected = true;
                        break;
                    }
                }

                if (!isIDExpected) {
                    setSuccess(false);
                    log.complain("Unexpected 'instance' value: " + receivedInstancesID[i]);
                }
            }

            if (!getSuccess()) {
                log.complain("Expected IDs:");
                for (long l : expectedInstancesID) {
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
        int expectedInstances = instances002a.expectedInstanceCount;

        String testClassName = nsk.jdwp.ReferenceType.Instances.instances002.TestClass.class.getName();
        testClass(createTypeSignature(testClassName), 0, expectedInstances);
    }
}
