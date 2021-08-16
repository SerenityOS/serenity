/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.jdi.MethodExitEvent.returnValue.returnValue003;

import nsk.share.*;
import nsk.share.jpda.ForceEarlyReturnTestThread;
import nsk.share.jdi.*;

/*
 * Debuggee class, handles commands for starting and stoping ForceEarlyReturnTestThread.
 */
public class returnValue003a extends AbstractJDIDebuggee {
    static {
        try {
            // load thread class to let debugger get ReferenceType for TestThread class
            Class.forName(ForceEarlyReturnTestThread.class.getName());
        } catch (ClassNotFoundException e) {
            System.out.println("ClassNotFoundException while loading test thread class: " + e);
            e.printStackTrace(System.out);
            System.exit(Consts.JCK_STATUS_BASE + Consts.TEST_FAILED);
        }
    }

    public static void main(String args[]) {
        new returnValue003a().doTest(args);
    }

    // start and suspend test threads to let debugger initialize breakpoints (debugger should obtain ThreadReference)
    // command:threadsNumber:iterationsNumber
    public static final String COMMAND_START_AND_SUSPEND_TEST_THREAD = "startAndSuspendTestThread";

    // let test threads continue execution
    public static final String COMMAND_START_TEST_THREAD_EXECUTION = "startTestThreadExecution";

    // stop test threads
    public static final String COMMAND_STOP_TEST_THREAD = "stopTestThread";

    public static final String testThreadName = "returnValue03_TestThread";

    public boolean parseCommand(String command) {
        if (super.parseCommand(command))
            return true;

        if (command.equals(COMMAND_START_AND_SUSPEND_TEST_THREAD)) {
            startTestThread();
            return true;
        } else if (command.equals(COMMAND_START_TEST_THREAD_EXECUTION)) {
            startTestThreadsExecution();
            return true;
        } else if (command.equals(COMMAND_STOP_TEST_THREAD)) {
            stopTestThreads();
            return true;
        }

        return false;
    }

    private ForceEarlyReturnTestThread testThread;

    private void startTestThread() {
        testThread = new ForceEarlyReturnTestThread(log, true, 1);
        testThread.setName(testThreadName);
        testThread.start();
    }

    private void startTestThreadsExecution() {
        if (testThread == null) {
            throw new TestBug("Test threads wasn't started");
        }
        testThread.startExecuion();
    }

    private void stopTestThreads() {
        if (testThread == null) {
            throw new TestBug("Test threads wasn't started");
        }

        testThread.stopExecution();

        try {
            testThread.join();
        } catch (InterruptedException e) {
            setSuccess(false);
            log.complain("Unexpected exception: " + e);
            e.printStackTrace(log.getOutStream());
        }

        if (!testThread.getSuccess())
            setSuccess(false);
    }

    // access to success status for TestThread
    public void setSuccess(boolean value) {
        super.setSuccess(value);
    }
}
