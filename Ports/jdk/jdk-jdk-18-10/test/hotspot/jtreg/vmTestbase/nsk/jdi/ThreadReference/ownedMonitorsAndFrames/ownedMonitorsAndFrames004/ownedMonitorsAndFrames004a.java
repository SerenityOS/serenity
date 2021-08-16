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
package nsk.jdi.ThreadReference.ownedMonitorsAndFrames.ownedMonitorsAndFrames004;

import java.io.*;
import java.util.ArrayList;
import nsk.share.TestBug;
import nsk.share.jdi.OwnedMonitorsDebuggee;
import nsk.share.locks.*;

public class ownedMonitorsAndFrames004a extends OwnedMonitorsDebuggee {
    // command:threadName:lock_type:threadName:lock_type
    public static final String COMMAND_CREATE_DEADLOCK = "createDeadlock";

    public static void main(String args[]) {
        ownedMonitorsAndFrames004a debuggee = new ownedMonitorsAndFrames004a();
        debuggee.init(args);
        debuggee.doTest();
    }

    public boolean parseCommand(String command) {
        if (super.parseCommand(command))
            return true;

        try {
            if (command.startsWith(COMMAND_CREATE_DEADLOCK)) {
                StreamTokenizer tokenizer = new StreamTokenizer(new StringReader(command));
                tokenizer.whitespaceChars(':', ':');
                tokenizer.wordChars('_', '_');
                tokenizer.wordChars(' ', ' ');
                tokenizer.wordChars('$', '$');

                tokenizer.nextToken();

                ArrayList<String> deadlockedThreadsNames = new ArrayList<String>();
                ArrayList<LockType> deadlockedThreadsLocksTypes = new ArrayList<LockType>();

                int nextToken = tokenizer.nextToken();

                while (nextToken != StreamTokenizer.TT_EOF) {
                    String threadName = tokenizer.sval;

                    if (tokenizer.nextToken() != StreamTokenizer.TT_WORD)
                        throw new TestBug("Invalid command format: " + command);

                    LockType lockType;

                    try {
                        lockType = LockType.valueOf(tokenizer.sval);
                    } catch (IllegalArgumentException e) {
                        throw new TestBug("Invalid command format: " + command + ", invalid lock type: " + tokenizer.sval);
                    }

                    deadlockedThreadsNames.add(threadName);
                    deadlockedThreadsLocksTypes.add(lockType);

                    nextToken = tokenizer.nextToken();
                }

                if (deadlockedThreadsNames.size() < 2)
                    throw new TestBug("Invalid command format: " + command + ", at least 2 thread are required for deadlock");

                createDeadlock(deadlockedThreadsNames, deadlockedThreadsLocksTypes);

                return true;
            }
        } catch (IOException e) {
            throw new TestBug("Invalid command format: " + command);
        }

        return false;
    }

    private DeadlockedThread deadlockedThreads[];

    public void createDeadlock(ArrayList<String> deadlockedThreadsNames, ArrayList<LockType> deadlockedThreadsLocksTypes) {
        deadlockedThreads = DeadlockMaker.createDeadlockedThreads(deadlockedThreadsLocksTypes);

        monitorsInfo = new LockingThread.DebugMonitorInfo[deadlockedThreadsNames.size()];

        // fill 'monitorsInfo' array with debug data using knowledge about nsk.share.locks.DeadlockedThread
        for (int i = 0; i < deadlockedThreadsNames.size(); i++) {
            deadlockedThreads[i].setName(deadlockedThreadsNames.get(i));

            Object monitor = deadlockedThreads[i].getLocker().getLock();

            int stackDepth = 2;
            boolean isJNILock = false;

            if (deadlockedThreads[i].getLocker() instanceof JNIMonitorLocker) {
                stackDepth = -1;
                isJNILock = true;
            }

            monitorsInfo[i] = new LockingThread.DebugMonitorInfo(monitor, stackDepth, deadlockedThreads[i], isJNILock);
        }
    }

}
