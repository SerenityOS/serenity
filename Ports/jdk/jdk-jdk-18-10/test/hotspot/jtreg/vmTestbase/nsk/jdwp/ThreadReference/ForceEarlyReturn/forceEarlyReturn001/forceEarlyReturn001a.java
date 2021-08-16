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
package nsk.jdwp.ThreadReference.ForceEarlyReturn.forceEarlyReturn001;

import nsk.share.TestBug;
import nsk.share.jdwp.*;
import nsk.share.jpda.ForceEarlyReturnTestThread;

public class forceEarlyReturn001a extends AbstractJDWPDebuggee {
    public static String testThreadName = "ForceEarlyReturnTestThread";

    private ForceEarlyReturnTestThread testThread;

    protected void init(String args[]) {
        super.init(args);

        // create instance of ForceEarlyReturnTestThread during initialization
        // to let debugger obtain threadID for this thread
        testThread = new ForceEarlyReturnTestThread(log, true, 1);
        testThread.setName(testThreadName);
        testThread.start();
    }

    public static String COMMAND_START_EXECUTION = "startExecution";

    public static String COMMAND_END_EXECUTION = "endExecution";

    public boolean parseCommand(String command) {
        if (super.parseCommand(command))
            return true;

        // force ForceEarlyReturnTestThread start execution
        if (command.equals(COMMAND_START_EXECUTION)) {
            testThread.startExecuion();
            return true;
        } else
        // wait when ForceEarlyReturnTestThread finish execution and check
        // is any errors occured during test
        if (command.equals(COMMAND_END_EXECUTION)) {
            try {
                testThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace(log.getOutStream());
                throw new TestBug("Unexpected exception: " + e);
            }

            if (!testThread.getSuccess()) {
                setSuccess(false);
            }

            return true;
        }

        return false;
    }

    public static void main(String args[]) {
        new forceEarlyReturn001a().doTest(args);
    }
}
