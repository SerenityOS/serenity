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
package nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn001;

import java.io.*;
import nsk.share.*;
import nsk.share.jpda.ForceEarlyReturnTestThread;
import nsk.share.jdi.*;

public class forceEarlyReturn001a extends AbstractJDIDebuggee {
    static {
        try {
            // load thread class to let debugger get ReferenceType for
            // TestThread class
            Class.forName(ForceEarlyReturnTestThread.class.getName());
        } catch (ClassNotFoundException e) {
            System.out.println("ClassNotFoundException while loading test thread class: " + e);
            e.printStackTrace(System.out);
            System.exit(Consts.JCK_STATUS_BASE + Consts.TEST_FAILED);
        }
    }

    public static void main(String args[]) {
        new forceEarlyReturn001a().doTest(args);
    }

    // start and suspend test threads to let debugger initialize breakpoints
    // (debugger should obtain ThreadReference)
    // command:threadsNumber:iterationsNumber
    public static final String COMMAND_START_AND_SUSPEND_TEST_THREADS = "startAndSuspendTestThreads";

    // let test threads continue execution
    public static final String COMMAND_START_TEST_THREADS_EXECUTION = "startTestThreadsExecution";

    // set flag 'stopExecution' for test threads
    public static final String COMMAND_STOP_TEST_THREADS = "stopTestThreads";

    // wait for test threads finishing
    public static final String COMMAND_JOIN_TEST_THREADS = "joinTestThreads";

    // base name for test threads, test thread should be named
    // 'baseTestThreadName + threadIndex'
    public static final String baseTestThreadName = "forceEarlyReturn001aTestThread_";

    public boolean parseCommand(String command) {
        if (super.parseCommand(command))
            return true;

        StreamTokenizer tokenizer = new StreamTokenizer(new StringReader(command));
        tokenizer.whitespaceChars(':', ':');

        try {
            if (command.startsWith(COMMAND_START_AND_SUSPEND_TEST_THREADS)) {
                tokenizer.nextToken();

                if (tokenizer.nextToken() != StreamTokenizer.TT_NUMBER)
                    throw new TestBug("Invalid command format: " + command);

                int threadsCount = (int) tokenizer.nval;

                if (tokenizer.nextToken() != StreamTokenizer.TT_NUMBER)
                    throw new TestBug("Invalid command format: " + command);

                int iterationsNumber = (int) tokenizer.nval;

                startTestThreads(threadsCount, iterationsNumber);

                return true;
            } else if (command.equals(COMMAND_START_TEST_THREADS_EXECUTION)) {
                startTestThreadsExecution();

                return true;
            } else if (command.equals(COMMAND_STOP_TEST_THREADS)) {
                stopTestThreads();

                return true;
            } else if (command.equals(COMMAND_JOIN_TEST_THREADS)) {
                joinTestThreads();

                return true;
            }
        } catch (IOException e) {
            throw new TestBug("Invalid command format: " + command);
        }

        return false;
    }

    private ForceEarlyReturnTestThread testThreads[];

    private void startTestThreads(int threadsCount, int iterationsNumber) {
        testThreads = new ForceEarlyReturnTestThread[threadsCount];

        for (int i = 0; i < threadsCount; i++) {
            testThreads[i] = new ForceEarlyReturnTestThread(log, (i == 0), iterationsNumber);
            testThreads[i].setName(baseTestThreadName + i);

            // testThreads[i] should wait in beginning of run()
            testThreads[i].start();
        }
    }

    private void startTestThreadsExecution() {
        if (testThreads == null) {
            throw new TestBug("Test threads weren't started");
        }

        // let test threads continue execution
        for (int i = 0; i < testThreads.length; i++)
            testThreads[i].startExecuion();
    }

    private void stopTestThreads() {
        if (testThreads == null) {
            throw new TestBug("Test threads weren't started");
        }

        for (int i = 0; i < testThreads.length; i++) {
            testThreads[i].stopExecution();
        }
    }

    private void joinTestThreads() {
        if (testThreads == null) {
            throw new TestBug("Test threads weren't started");
        }

        for (int i = 0; i < testThreads.length; i++) {
            try {
                testThreads[i].join();
            } catch (InterruptedException e) {
                unexpectedException(e);
            }

            if (!testThreads[i].getSuccess())
                setSuccess(false);
        }
    }
}
