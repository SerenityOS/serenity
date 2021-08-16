/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.jdi.VirtualMachine.resume;

import nsk.share.jdi.*;

//    THIS TEST IS LINE NUMBER SENSITIVE

/*
 * Debuggee part of test (debuggee starts several test threads which should
 * be suspended at breakpoint)
 *
 * WARNING: edit this file carefully, breakpoint line number is hardcoded
 */
public class resume001a extends AbstractJDIDebuggee {

    static final String COMMAND_STOP_ALL_THREADS_AT_BREAKPOINT = "COMMAND_START_TEST_THREADS";

    static final String COMMAND_JOIN_TEST_THREADS = "COMMAND_JOIN_TEST_THREADS";

    static int counter;

    static final String COUNTER_FIELD_NAME = "counter";

    synchronized static void incCounter() {
        counter++;
    }

    class TestThread extends Thread {

        public void run() {
            log.display(getName() + " started");

            stopAtBreakpoint();

            log.display(getName() + " finished");
        }
    }

    static final int BREAKPOINT_LINE_NUMBER = 63;

    static void stopAtBreakpoint() {
        int i = 0; // BREAKPOINT_LINE_NUMBER

        incCounter();
    }

    static final int TEST_THREAD_NUMBER = 10;

    private TestThread[] testThreads = new TestThread[TEST_THREAD_NUMBER];

    public resume001a() {
        for (int i = 0; i < testThreads.length; i++) {
            testThreads[i] = new TestThread();
        }
    }

    public boolean parseCommand(String command) {
        if (super.parseCommand(command))
            return true;

        if (command.equals(COMMAND_STOP_ALL_THREADS_AT_BREAKPOINT)) {
            /*
             * All TestThreads execute method stopAtBreakpoint()
             */
            for (TestThread testThread : testThreads) {
                testThread.start();
            }

            // debuggee main thread also stops at breakpoint
            stopAtBreakpoint();

            return true;
        } else if (command.equals(COMMAND_JOIN_TEST_THREADS)) {
            for (TestThread testThread : testThreads) {
                try {
                    testThread.join();
                } catch (InterruptedException e) {
                    unexpectedException(e);
                }
            }
            return true;
        }

        return false;
    }

    public static void main(String args[]) {
        new resume001a().doTest(args);
    }

}
