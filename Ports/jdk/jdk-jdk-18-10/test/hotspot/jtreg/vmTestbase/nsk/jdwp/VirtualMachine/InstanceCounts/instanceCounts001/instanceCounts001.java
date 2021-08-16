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
 * @summary converted from VM Testbase nsk/jdwp/VirtualMachine/InstanceCounts/instanceCounts001.
 * VM Testbase keywords: [quick, jpda, jdwp, feature_jdk6_jpda, vm6, monitoring]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test performs checking for
 *         command set: VirtualMachine
 *         command: InstanceCounts
 *     Test checks that debuggee accept the command packet and
 *     replies with correct reply packet.
 *     Test consists of two compoments:
 *         debugger: instanceCounts001
 *         debuggee: instanceCounts001a
 *     Debugger uses nsk.share support classes to launch debuggee
 *     and obtain Transport object, that represents JDWP transport channel.
 *     Also communication channel (IOPipe) is established between
 *     debugger and debuggee to exchange with execution commands.
 *     Debuggee during startup creates instances of 'nsk.jdwp.VirtualMachine.InstanceCounts.instanceCounts001.TestClass1'
 *     and 'nsk.jdwp.VirtualMachine.InstanceCounts.instanceCounts001.TestClass2' reachable via references with types
 *     which should be supported by command VirtualMachine.InstanceCounts:
 *         - strong reference
 *         - soft reference
 *         - weak reference
 *         - phantom reference
 *         - JNI global reference
 *         - JNI local reference
 *     First, debugger obtains referenceTypeID for 'nsk.jdwp.VirtualMachine.InstanceCounts.instanceCounts001.TestClass1'.
 *     Then, debugger creates command packet for InstanceCounts command with the found referenceTypeID
 *     as an argument, writes packet to the transport      channel, and waits for a reply packet.
 *     When reply packet is received, debugger parses the packet structure, extracts number of instances
 *     and checks that received value is correct.
 *     Then debugger repeat described above actions, but created command packet contains 2 referenceTypeIDs for
 *     'nsk.jdwp.VirtualMachine.InstanceCounts.instanceCounts001.TestClass1' and 'nsk.jdwp.VirtualMachine.InstanceCounts.instanceCounts001.TestClass2'.
 *         Also, test performs check for case when incorrect data is sent in command:
 *                 - create command with refTypesCount < 0, expect ILLEGAL_ARGUMENT error
 *     Finally, debugger sends debuggee signal to quit, waits for it exits
 *     and exits too with the proper exit code.
 *
 * @requires vm.gc != "Z"
 * @requires !vm.graal.enabled
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @build nsk.jdwp.VirtualMachine.InstanceCounts.instanceCounts001.instanceCounts001a
 * @run main/othervm/native
 *      nsk.jdwp.VirtualMachine.InstanceCounts.instanceCounts001.instanceCounts001
 *      -arch=${os.family}-${os.simpleArch}
 *      -verbose
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="-Xmx128M ${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdwp.VirtualMachine.InstanceCounts.instanceCounts001;

import nsk.share.Consts;
import nsk.share.jdwp.CommandPacket;
import nsk.share.jdwp.JDWP;
import nsk.share.jdwp.ReplyPacket;
import nsk.share.jdwp.TestDebuggerType1;

import java.io.PrintStream;

public class instanceCounts001 extends TestDebuggerType1 {
    protected String getDebugeeClassName() {
        return "nsk.jdwp.VirtualMachine.InstanceCounts.instanceCounts001.instanceCounts001a";
    }

    public static void main(String[] argv) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String[] argv, PrintStream out) {
        return new instanceCounts001().runIt(argv, out);
    }

    private void testCommand(long[] typeIDs, int refTypesCount, int[] expectedInstanceCounts, boolean expectError, int errorCode) {
        try {
            int JDWP_COMMAND_ID = JDWP.Command.VirtualMachine.InstanceCounts;

            log.display("Create command: " + JDWP.commandNames.get(JDWP_COMMAND_ID));
            log.display("refTypesCount = " + refTypesCount);
            for (long typeID : typeIDs)
                log.display("refType = " + typeID);

            CommandPacket command = new CommandPacket(JDWP_COMMAND_ID);
            command.addInt(refTypesCount);

            for (int i = 0; i < refTypesCount; i++) {
                command.addReferenceTypeID(typeIDs[i]);
            }

            command.setLength();

            log.display("Sending command packet:\n" + command);
            transport.write(command);

            ReplyPacket reply;

            reply = getReply(command, expectError, errorCode);

            if (expectError)
                return;

            int counts = reply.getInt();
            log.display("counts = " + counts);

            // check received 'counts' value
            if (counts != refTypesCount) {
                setSuccess(false);
                log.complain("Invalid 'counts' value, expected is: " + refTypesCount);
            }

            // check received 'instanceCount' value for all classes
            for (int i = 0; i < counts; i++) {
                long instanceCount = reply.getLong();
                log.display("instanceCount = " + instanceCount);

                if (instanceCount != expectedInstanceCounts[i]) {
                    setSuccess(false);
                    log.complain("Unexpected value 'instanceCount': " + instanceCount + ", expected is " + expectedInstanceCounts[i]);
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
        String testClass1 = nsk.jdwp.VirtualMachine.InstanceCounts.instanceCounts001.TestClass1.class.getName();
        String testClass2 = nsk.jdwp.VirtualMachine.InstanceCounts.instanceCounts001.TestClass2.class.getName();

        forceGC();
        pipe.println(instanceCounts001a.COMMAND_CREATE_TEST_INSTANCES);

        if (!isDebuggeeReady())
            return;

        // Note! This test is broken, in the sense that it incorrectly assumes
        // that no GC can happen before it walks the heap. In practice, it seems
        // to only affect this test when using ZGC. However, this test will also
        // fail when using other GCs if an explicit GC is done here.

        int expectedCount = instanceCounts001a.expectedCount;

        String[] classNames;

        classNames = new String[]{createTypeSignature(testClass1)};

        long[] typeIDs;

        typeIDs = new long[classNames.length];

        // create valid command for 1 class
        for (int i = 0; i < classNames.length; i++)
            typeIDs[i] = debuggee.getReferenceTypeID(classNames[i]);

        testCommand(typeIDs, typeIDs.length, new int[]{expectedCount}, false, 0);

        classNames = new String[]{createTypeSignature(testClass1), createTypeSignature(testClass2)};

        typeIDs = new long[classNames.length];

        // create valid command for 2 classes
        for (int i = 0; i < classNames.length; i++)
            typeIDs[i] = debuggee.getReferenceTypeID(classNames[i]);

        testCommand(typeIDs, typeIDs.length, new int[]{expectedCount, expectedCount}, false, 0);

        // create command with refTypesCount < 0, expect ILLEGAL_ARGUMENT error
        testCommand(typeIDs, -1, new int[]{expectedCount}, true, JDWP.Error.ILLEGAL_ARGUMENT);
        resetStatusIfGC();
    }
}
