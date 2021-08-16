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
package nsk.jdi.StackFrame.getArgumentValues.getArgumentValues002;

import nsk.share.TestBug;
import nsk.share.jdi.*;

// Debuggee class, starts test thread which has stack with many frames
public class getArgumentValues002a extends AbstractJDIDebuggee {

    public static final String COMMAND_START_TEST_THREAD = "COMMAND_START_TEST_THREAD";

    public static final String COMMAND_STOP_TEST_THREAD = "COMMAND_STOP_TEST_THREAD";

    public static final String testThreadName = "getArgumentValues002a_TestThread";

    class TestThread extends Thread {

        volatile boolean stackWasCreated;

        volatile boolean stoped;

        public TestThread() {
            super(testThreadName);
        }

        public void run() {
            recursiveMethod1(300);
        }

        void recursiveMethod1(int arg1) {
            log.display("recursiveMethod1: " + arg1);
            if (arg1 > 0) {
                if (arg1 % 3 == 0)
                    recursiveMethod3(arg1 - 1);
                else if (arg1 % 3 == 1)
                    recursiveMethod2(arg1 - 1);
                else
                    recursiveMethod1(arg1 - 1);
            }
            else {
                stackWasCreated = true;
                while (!stoped);
            }
        }

        void recursiveMethod2(int arg1) {
            log.display("recursiveMethod2: " + arg1);
            if (arg1 > 0) {
                if (arg1 % 3 == 0)
                    recursiveMethod3(arg1 - 1);
                else if (arg1 % 3 == 1)
                    recursiveMethod2(arg1 - 1);
                else
                    recursiveMethod1(arg1 - 1);
            }
            else {
                stackWasCreated = true;
                while (!stoped);
            }
        }

        void recursiveMethod3(int arg1) {
            log.display("recursiveMethod3: " + arg1);
            if (arg1 > 0) {
                if (arg1 % 3 == 0)
                    recursiveMethod3(arg1 - 1);
                else if (arg1 % 3 == 1)
                    recursiveMethod2(arg1 - 1);
                else
                    recursiveMethod1(arg1 - 1);
            }
            else {
                stackWasCreated = true;
                while (!stoped);
            }
        }
    }

    private TestThread testThread;

    public boolean parseCommand(String command) {
        if (super.parseCommand(command))
            return true;

        if (command.equals(COMMAND_START_TEST_THREAD)) {

            if (testThread != null)
                throw new TestBug("Thread is already created");

            testThread = new TestThread();
            testThread.start();
            while (!testThread.stackWasCreated)
                Thread.yield();

            return true;
        } else if (command.equals(COMMAND_STOP_TEST_THREAD)) {

            if (testThread == null)
                throw new TestBug("Thread isn't created");

            testThread.stoped = true;

            try {
                testThread.join();
            } catch (InterruptedException e) {
                setSuccess(false);
                log.complain("Unexpected exception: " + e);
                e.printStackTrace(log.getOutStream());
            }

            return true;
        }

        return false;
    }

    public static void main(String args[]) {
        new getArgumentValues002a().doTest(args);
    }

}
