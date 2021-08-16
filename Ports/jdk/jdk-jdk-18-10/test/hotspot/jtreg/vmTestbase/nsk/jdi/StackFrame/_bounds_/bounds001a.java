/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.jdi.StackFrame._bounds_;

import nsk.share.jdi.*;

/**
 *  <code>bounds001a</code> is deugee's part of the bounds001.
 */
public class bounds001a extends AbstractJDIDebuggee {

    static public final String COMMAND_STOP_TEST_THREAD = "COMMAND_STOP_TEST_THREAD";

    static public final String TEST_THREAD_NAME = "nsk.jdi.StackFrame._bounds_.bounds001a_TestThread";

    class TestThread extends Thread {
        boolean started;

        TestThread() {
            super(TEST_THREAD_NAME);
        }

        public void run() {
            /*
             * Local variables required by debugger
             */
            byte     byteVar = 0;
            char     charVar = ' ';
            double   doubleVar = 0;
            float    floatVar = 0;
            int      intVar = 0;
            long     longVar = 0;
            short    shortVar = 0;

            started = true;

            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                // expected exception
            }
        }
    }

    private TestThread testThread;

    protected String[] doInit(String[] args) {
        testThread = new TestThread();
        testThread.start();

        while (!testThread.started)
            Thread.yield();

        return super.doInit(args);
    }

    public boolean parseCommand(String command) {
        if (super.parseCommand(command))
            return true;

        if (command.equals(COMMAND_STOP_TEST_THREAD)) {
            testThread.interrupt();
            try {
                log.display("Wait for test thread: " + testThread);
                testThread.join();
            } catch (InterruptedException e) {
                unexpectedException(e);
            }

            return true;
        }

        return false;
    }

    public static void main (String args[]) {
        new bounds001a().doTest(args);
    }
}
