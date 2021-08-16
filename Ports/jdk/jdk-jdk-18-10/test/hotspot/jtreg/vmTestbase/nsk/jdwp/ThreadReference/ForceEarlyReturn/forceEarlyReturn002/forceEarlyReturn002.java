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
 * @summary converted from VM Testbase nsk/jdwp/ThreadReference/ForceEarlyReturn/forceEarlyReturn002.
 * VM Testbase keywords: [quick, jpda, jdwp, feature_jdk6_jpda, vm6, monitoring]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test performs checking for
 *         command set: ThreadReference
 *         command: ForceEarlyReturn
 *     Test checks that debuggee accept the command packet and
 *     replies with correct reply packet.
 *     Test consists of two compoments:
 *         debugger: forceEarlyReturn002
 *         debuggee: forceEarlyReturn002a
 *     Debugger uses nsk.share support classes to launch debuggee
 *     and obtain Transport object, that represents JDWP transport channel.
 *     Also communication channel (IOPipe) is established between
 *     debugger and debuggee to exchange with execution commands.
 *     Test performs checks for cases when incorrect data are send in command.
 *     Following cases are tested:
 *         - create command with threadID = 1, expect INVALID_OBJECT error
 *         - debuggee creates test thread which sequentially changes its state in following order:
 *             - thread not started
 *             - thread is running
 *             - thread is sleeping
 *             - thread in Object.wait()
 *             - thread wait on java monitor
 *             - thread is finished
 *         Debugger try execute command for this thread without thread suspending for states: thread running, thread sleeping,
 *         thread waiting, thread blocked, THREAD_NOT_SUSPENDED error is expected.
 *         When test thread has finish execution debugger suspends thread and call command for this thread, INVALID_THREAD
 *         error is expected, then, debugger resumes test thread and call command again, INVALID_THREAD error should
 *         be returned in reply.
 *         - debuggee starts test thread which executes infinite loop in native method, debugger calls command for
 *         this thread(without suspending) and expects THREAD_NOT_SUSPENDED error. Then, debugger suspends this thread
 *         and calls command again, OPAQUE_FRAME error is expected.
 *         - debugger creates ThreadStartEventRequest with suspend policy 'JDWP.SuspendPolicy.ALL' and forces debuggee start new thread.
 *         When ThreadStartEvent is received debugger obtains threadID from this event and calls command for this thread. Since
 *         just started thread has no frames yet NO_MORE_FRAMES error is expected.
 *     Finally, debugger sends debuggee signal to quit, waits for it exits
 *     and exits too with the proper exit code.
 *
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @run main/othervm/native
 *      nsk.jdwp.ThreadReference.ForceEarlyReturn.forceEarlyReturn002.forceEarlyReturn002
 *      -arch=${os.family}-${os.simpleArch}
 *      -verbose
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

package nsk.jdwp.ThreadReference.ForceEarlyReturn.forceEarlyReturn002;

import nsk.share.Consts;
import nsk.share.jdwp.CommandPacket;
import nsk.share.jdwp.EventPacket;
import nsk.share.jdwp.JDWP;
import nsk.share.jdwp.JDWP.Value;
import nsk.share.jdwp.ReplyPacket;
import nsk.share.jdwp.TestDebuggerType1;
import nsk.share.jpda.AbstractDebuggeeTest;
import nsk.share.jpda.StateTestThread;

import java.io.PrintStream;

public class forceEarlyReturn002 extends TestDebuggerType1 {
    protected String getDebugeeClassName() {
        return "nsk.jdwp.ThreadReference.ForceEarlyReturn.forceEarlyReturn002.forceEarlyReturn002a";
    }

    public static void main(String[] argv) {
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String[] argv, PrintStream out) {
        return new forceEarlyReturn002().runIt(argv, out);
    }

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

    private int createThreadStartEventRequest() {
        try {
            // create command packet and fill requred out data
            CommandPacket command = new CommandPacket(JDWP.Command.EventRequest.Set);
            command.addByte(JDWP.EventKind.THREAD_START);
            command.addByte(JDWP.SuspendPolicy.ALL);
            command.addInt(0);
            command.setLength();

            transport.write(command);

            ReplyPacket reply;
            reply = getReply(command);

            int requestID = reply.getInt();

            if (!reply.isParsed()) {
                setSuccess(false);
                log.complain("Extra trailing bytes found in request reply packet at: " + reply.offsetString());
                return -1;
            }

            return requestID;
        } catch (Exception e) {
            setSuccess(false);
            log.complain("Caught exception while testing JDWP command: " + e);
            e.printStackTrace(log.getOutStream());

            return -1;
        }
    }

    public void doTest() {
        Value value;

        value = new Value(JDWP.Tag.INT, 0);
        // create command with invalid trheadID, expect INVALID_OBJECT error
        sendCommand(-1, value, true, JDWP.Error.INVALID_OBJECT);

        // create StateTestThread
        pipe.println(AbstractDebuggeeTest.COMMAND_CREATE_STATETESTTHREAD);

        if (!isDebuggeeReady())
            return;

        // switch thread state to RUNNING to get threadID (can't get threadID
        // when thread not running)
        pipe.println(AbstractDebuggeeTest.COMMAND_NEXTSTATE_STATETESTTHREAD);

        if (!isDebuggeeReady())
            return;

        long threadID = debuggee.getThreadID(AbstractDebuggeeTest.stateTestThreadName);

        int state = 2;

        // check following states: "RUNNING", "SLEEPING", "WAIT", "MONITOR"
        while (state++ < StateTestThread.stateTestThreadStates.length) {
            sendCommand(threadID, value, true, JDWP.Error.THREAD_NOT_SUSPENDED);

            pipe.println(AbstractDebuggeeTest.COMMAND_NEXTSTATE_STATETESTTHREAD);

            if (!isDebuggeeReady())
                return;
        }

        // here thread has exited (state "ZOMBIE")
        sendCommand(threadID, value, true, JDWP.Error.INVALID_THREAD);

        // suspend thread, but also expect INVALID_THREAD error
        debuggee.suspendThread(threadID);
        sendCommand(threadID, value, true, JDWP.Error.INVALID_THREAD);

        // create thread which executes native method
        pipe.println(forceEarlyReturn002a.COMMAND_STOP_THREAD_IN_NATIVE);

        if (!isDebuggeeReady())
            return;

        threadID = debuggee.getThreadID(forceEarlyReturn002a.testThreadInNativeName);

        // thread in native not suspended, expect THREAD_NOT_SUSPENDED error
        sendCommand(threadID, value, true, JDWP.Error.THREAD_NOT_SUSPENDED);

        debuggee.suspendThread(threadID);
        // suspended thread in native, expect OPAQUE_FRAME error
        sendCommand(threadID, value, true, JDWP.Error.OPAQUE_FRAME);

        // create request for ThreadStart event
        int requestID = createThreadStartEventRequest();

        // force debuggee start new thread
        pipe.println(forceEarlyReturn002a.COMMAND_START_NEW_THREAD);

        // receive ThreadStart event
        EventPacket eventPacket = receiveSingleEvent(JDWP.EventKind.THREAD_START, requestID);

        try {
            threadID = eventPacket.getObjectID();

            value = new Value(JDWP.Tag.VOID, 0);
            // just started thread has no frames, expect NO_MORE_FRAMES error
            sendCommand(threadID, value, true, JDWP.Error.NO_MORE_FRAMES);
        } catch (Exception e) {
            setSuccess(false);
            log.complain("Caught exception while testing JDWP command: " + e);
            e.printStackTrace(log.getOutStream());
        }

        clearRequest(JDWP.EventKind.THREAD_START, requestID);

        debuggee.resume();

        if (!isDebuggeeReady())
            return;
    }
}
