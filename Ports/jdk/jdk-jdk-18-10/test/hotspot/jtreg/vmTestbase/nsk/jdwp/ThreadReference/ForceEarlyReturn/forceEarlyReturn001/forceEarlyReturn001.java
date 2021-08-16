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
 * @summary converted from VM Testbase nsk/jdwp/ThreadReference/ForceEarlyReturn/forceEarlyReturn001.
 * VM Testbase keywords: [jpda, jdwp, feature_jdk6_jpda, vm6, monitoring, quarantine]
 * VM Testbase comments: 7199837
 * VM Testbase readme:
 * DESCRIPTION
 *     This test performs checking for
 *         command set: ThreadReference
 *         command: ForceEarlyReturn
 *     Test checks that debuggee accept the command packet and
 *     replies with correct reply packet.
 *     Test consists of two compoments:
 *         debugger: forceEarlyReturn001
 *         debuggee: forceEarlyReturn001a
 *     Debugger uses nsk.share support classes to launch debuggee
 *     and obtain Transport object, that represents JDWP transport channel.
 *     Also communication channel (IOPipe) is established between
 *     debugger and debuggee to exchange with execution commands.
 *     Debuggee VM create thread(class nsk.share.jpda.ForceEarlyReturnTestThread is used) which sequentially call
 *     test methods with different return value's type:
 *         - void
 *         - all primitive types
 *         - all wrappers of primitive types
 *         - String
 *         - Object
 *         - array of java.lang.Object
 *         - Thread
 *         - ThreadGroup
 *         - Class object
 *         - ClassLoader
 *     Also test thread class contains static fields with predefined values which should be returned through
 *     ForceEarlyReturn('expectedXXXValue') and fields with values which can be used to check is ForceEarlyReturn returns
 *     TYPE_MISMATCH error if value's type in command doesn't match method's return type('invalidXXXValue').
 *     Debugger set breakpoints in test thread's methods and create instances of 'nsk.share.jdwp.JDWP.Value'
 *     based on values predefined in ForceEarlyReturnTestThread (both valid and invalid values are created).
 *     Debugger obtains threadID for test thread.
 *     Debugger force debugee start test thread and wait while BreakpointEvents occurs. When debuggee's
 *     test thread stop at breakpoint debugger first creates command packet for ForceEarlyReturn command with the
 *     found threadID and corresponding instance of invalid 'nsk.share.jdwp.JDWP.Value', writes
 *     packet to the transport channel, and waits for a reply packet. When reply packet is received, debugger
 *     parses the packet structure and checks that reply contain TYPE_MISMATCH error. Then debugger send command
 *     with correct value, checks that reply is empty and resume debuggee VM.
 *     Test thread in debuggee VM checks that value returned from test methods equals predefined value and no
 *     instructions was executed in called method after force return (finally blocks are not executed too).
 *     When all breakpoint events occured debugger sends debuggee signal to finish test thread execution.
 *     Debuggee waits when test thread finish execution and checks is any errors occured during test.
 *     Finally, debugger sends debuggee signal to quit, waits for it exits
 *     and exits too with the proper exit code.
 *
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @run main/othervm
 *      nsk.jdwp.ThreadReference.ForceEarlyReturn.forceEarlyReturn001.forceEarlyReturn001
 *      -arch=${os.family}-${os.simpleArch}
 *      -verbose
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdwp.ThreadReference.ForceEarlyReturn.forceEarlyReturn001;

import nsk.share.Consts;
import nsk.share.jdwp.CommandPacket;
import nsk.share.jdwp.JDWP;
import nsk.share.jdwp.JDWP.Value;
import nsk.share.jdwp.ReplyPacket;
import nsk.share.jdwp.TestDebuggerType1;
import nsk.share.jpda.ForceEarlyReturnTestThread;

import java.io.PrintStream;

public class forceEarlyReturn001 extends TestDebuggerType1 {
    // data needed to create JDWP command,
    // also this class create breakpoint in method which should be forced to return
    class TestData {
        public TestData(long classID, String methodName, int lineNumber, long threadID, Value value, Value invalidValue) {
            breakpointID = debuggee.requestBreakpointEvent(
                    JDWP.TypeTag.CLASS,
                    classID,
                    debuggee.getMethodID(classID, methodName, true),
                    lineNumber,
                    JDWP.SuspendPolicy.EVENT_THREAD);

            this.value = value;
            this.invalidValue = invalidValue;
            this.threadID = threadID;
        }

        public int breakpointID;
        public Value value;
        public Value invalidValue;
        public long threadID;
    }

    protected String getDebugeeClassName() {
        return "nsk.jdwp.ThreadReference.ForceEarlyReturn.forceEarlyReturn001.forceEarlyReturn001a";
    }

    public static void main(String[] argv) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String[] argv, PrintStream out) {
        return new forceEarlyReturn001().runIt(argv, out);
    }

    // send command and receive empty reply
    // all asserts should be done in debuggee
    private void sendCommand(long threadID, Value value, boolean expectError, int errorCode) {
        try {
            int JDWP_COMMAND_ID = JDWP.Command.ThreadReference.ForceEarlyReturn;

            log.display("Create command: " + JDWP.commandNames.get(JDWP_COMMAND_ID));
            log.display("threadID = " + threadID);
            log.display("Value = " + value);

            CommandPacket command = new CommandPacket(JDWP_COMMAND_ID);
            command.addObjectID(threadID);
            command.addValue(value);
            command.setLength();

            log.display("Sending command packet:\n" + command);
            transport.write(command);

            ReplyPacket reply;

            reply = getReply(command, expectError, errorCode);

            if (expectError)
                return;

            log.display("Empty reply");

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

    private TestData[] testData;

    // create Value objects which should be send in command packet and
    // initialize breapoints in tested methods
    private void initTestData() {
        long classID = debuggee.getReferenceTypeID(createTypeSignature(ForceEarlyReturnTestThread.class.getName()));

        Value[] testValues = new Value[ForceEarlyReturnTestThread.testedTypesNames.length + 1];
        Value[] testInvalidValues = new Value[ForceEarlyReturnTestThread.testedTypesNames.length + 1];

        testValues[0] = new JDWP.Value(JDWP.Tag.VOID, 0L);

        for (int i = 1; i < ForceEarlyReturnTestThread.testedTypesNames.length; i++) {
            testValues[i] = debuggee.getStaticFieldValue(
                    classID,
                    debuggee.getClassFieldID(classID, "expected" + ForceEarlyReturnTestThread.testedTypesNames[i] + "Value", true));
        }

        for (int i = 0; i < ForceEarlyReturnTestThread.testedTypesNames.length; i++) {
            testInvalidValues[i] = debuggee.getStaticFieldValue(
                    classID,
                    debuggee.getClassFieldID(classID, "invalid" + ForceEarlyReturnTestThread.testedTypesNames[i] + "Value", true));
        }

        long threadID = debuggee.getThreadID(forceEarlyReturn001a.testThreadName);

        testData = new TestData[ForceEarlyReturnTestThread.testedTypesNames.length];

        for (int i = 0; i < ForceEarlyReturnTestThread.testedTypesNames.length; i++) {
            testData[i] = new TestData(classID,
                    ForceEarlyReturnTestThread.testedTypesNames[i] + "Method",
                    ForceEarlyReturnTestThread.breakpointLines[i],
                    threadID,
                    testValues[i],
                    testInvalidValues[i]);
        }
    }

    public void doTest() {
        initTestData();

        pipe.println(forceEarlyReturn001a.COMMAND_START_EXECUTION);

        if (!isDebuggeeReady())
            return;

        for (TestData testDatum : testData) {
            // wait when tested thread call method with breapoint
            debuggee.waitForBreakpointEvent(testDatum.breakpointID);

            log.display("Send invalid command: valid value: " + testDatum.value + " invalid value: " + testDatum.invalidValue);
            // send ForceEarlyReturn command with invalid value
            sendCommand(testDatum.threadID, testDatum.invalidValue, true, JDWP.Error.TYPE_MISMATCH);

            log.display("Send valid command: valid value: " + testDatum.value);
            // send ForceEarlyReturn command
            sendCommand(testDatum.threadID, testDatum.value, false, 0);

            // resume debuggee
            debuggee.resume();
        }

        pipe.println(forceEarlyReturn001a.COMMAND_END_EXECUTION);

        if (!isDebuggeeReady())
            return;
    }
}
