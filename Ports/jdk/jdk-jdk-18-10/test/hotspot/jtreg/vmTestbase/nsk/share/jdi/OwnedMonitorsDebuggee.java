/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.share.jdi;

import java.io.*;
import java.util.*;
import nsk.share.locks.LockingThread;
import nsk.share.TestBug;

/*
 *  Class is used as base debuggee in tests for ThreadReference.ownedMonitorsAndFrames().
 *
 *  Class handle commands for creating threads which acquires and relinquish locks in different ways,
 *  nsk.share.locks.LockingThread is used for this purposes.
 *
 *  Information about acquired monitors can be stored in static array 'monitorsInfo' and in this way this
 *  data is available for debugger.
 */
public class OwnedMonitorsDebuggee extends AbstractJDIDebuggee {
    // command:threadName:stackFrameDescription
    public static final String COMMAND_CREATE_LOCKING_THREAD = "createLockingThread";

    // command:threadName
    public static final String COMMAND_STOP_LOCKING_THREAD = "stopLockingThread";

    // command:threadName
    public static final String COMMAND_UPDATE_MONITOR_INFO = "updateMonitorInfo";

    // command:threadName
    public static final String COMMAND_EXIT_SINGLE_FRAME = "exitSingleFrame";

    // command:threadName:monitorIndex
    public static final String COMMAND_RELINQUISH_MONITOR = "relinquishMonitor";

    // command:threadName
    public static final String COMMAND_ACQUIRE_RELINQUISHED_MONITOR = "acquireRelinquishedMonitor";

    // from this array information about acquired monitors is available for debugger
    // (this class is used in stress tests where several tests are executed consecutively, but in this case using of
    // static array shouldn't cause problems because of before using of array 'monitorsInfo' debugger
    // uses COMMAND_UPDATE_MONITOR_INFO which updates this array with latest data, so excution of one test shouldn't
    // affect other tests)
    public static LockingThread.DebugMonitorInfo monitorsInfo[];

    private boolean returnJNIMonitors;

    public final static String mainThreadName = "OwnedMonitorDebuggeeMainThread";

    protected String[] doInit(String[] args) {
        Thread.currentThread().setName(mainThreadName);

        args = super.doInit(args);

        ArrayList<String> standardArgs = new ArrayList<String>();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-returnJNIMonitors")) {
                returnJNIMonitors = true;
            } else
                standardArgs.add(args[i]);
        }

        return standardArgs.toArray(new String[] {});
    }

    public boolean parseCommand(String command) {
        if (super.parseCommand(command))
            return true;

        StreamTokenizer tokenizer = new StreamTokenizer(new StringReader(command));
        tokenizer.whitespaceChars(':', ':');
        tokenizer.wordChars('_', '_');

        try {
            if (command.startsWith(COMMAND_ACQUIRE_RELINQUISHED_MONITOR)) {
                tokenizer.nextToken();

                if (tokenizer.nextToken() != StreamTokenizer.TT_WORD)
                    throw new TestBug("Invalid command format: " + command);

                String threadName = tokenizer.sval;

                acquireRelinquishMonitor(threadName);

                return true;
            } else if (command.startsWith(COMMAND_RELINQUISH_MONITOR)) {
                tokenizer.nextToken();

                if (tokenizer.nextToken() != StreamTokenizer.TT_WORD)
                    throw new TestBug("Invalid command format: " + command);

                String threadName = tokenizer.sval;

                if (tokenizer.nextToken() != StreamTokenizer.TT_NUMBER)
                    throw new TestBug("Invalid command format: " + command);

                int monitorIndex = (int) tokenizer.nval;

                relinquishMonitor(threadName, monitorIndex);

                return true;
            } else if (command.startsWith(COMMAND_CREATE_LOCKING_THREAD)) {
                tokenizer.nextToken();

                if (tokenizer.nextToken() != StreamTokenizer.TT_WORD)
                    throw new TestBug("Invalid command format: " + command);

                String threadName = tokenizer.sval;

                List<String> stackFramesDescription = new ArrayList<String>();

                while (tokenizer.nextToken() == StreamTokenizer.TT_WORD) {
                    stackFramesDescription.add(tokenizer.sval);
                }

                createLockingThread(threadName, stackFramesDescription);
                return true;
            } else if (command.startsWith(COMMAND_STOP_LOCKING_THREAD)) {
                tokenizer.nextToken();

                if (tokenizer.nextToken() != StreamTokenizer.TT_WORD)
                    throw new TestBug("Invalid command format: " + command);

                String threadName = tokenizer.sval;

                stopLockingThread(threadName);

                return true;
            } else if (command.startsWith(COMMAND_UPDATE_MONITOR_INFO)) {
                tokenizer.nextToken();

                if (tokenizer.nextToken() != StreamTokenizer.TT_WORD)
                    throw new TestBug("Invalid command format: " + command);

                String threadName = tokenizer.sval;

                updateMonitorInfo(threadName);

                return true;
            } else if (command.startsWith(COMMAND_EXIT_SINGLE_FRAME)) {
                tokenizer.nextToken();

                if (tokenizer.nextToken() != StreamTokenizer.TT_WORD)
                    throw new TestBug("Invalid command format: " + command);

                String threadName = tokenizer.sval;

                exitSingleFrame(threadName);

                return true;
            }
        } catch (IOException e) {
            throw new TestBug("Invalid command format: " + command);
        }

        return false;
    }

    public void acquireRelinquishMonitor(String threadName) {
        LockingThread thread = getThread(threadName);
        thread.acquireRelinquishedMonitor();

        thread.waitState();
        updateMonitorInfo(threadName);
    }

    public void relinquishMonitor(String threadName, int monitorIndex) {
        LockingThread thread = getThread(threadName);
        thread.relinquishMonitor(monitorIndex);

        thread.waitState();
        updateMonitorInfo(threadName);
    }

    private void updateMonitorInfo(String threadName) {
        LockingThread thread = getThread(threadName);
        monitorsInfo = thread.getMonitorsInfo(returnJNIMonitors);
    }

    private Map<String, LockingThread> threads = new TreeMap<String, LockingThread>();

    private LockingThread getThread(String threadName) {
        LockingThread thread = threads.get(threadName);

        if (thread == null)
            throw new TestBug("Locking thread with name: " + threadName + " was not created");

        return thread;
    }

    private void stopLockingThread(String threadName) {
        LockingThread thread = getThread(threadName);
        thread.stopLockingThread();
        thread.waitState();

        updateMonitorInfo(threadName);
    }

    private void exitSingleFrame(String threadName) {
        LockingThread thread = getThread(threadName);
        thread.exitSingleFrame();
        thread.waitState();

        updateMonitorInfo(threadName);
    }

    private void createLockingThread(String threadName, List<String> stackFramesDescription) {
        if (threads.get(threadName) != null)
            throw new TestBug("Locking thread with name: " + threadName + " already exists");

        LockingThread thread = new LockingThread(log, stackFramesDescription);
        thread.setName(threadName);
        thread.start();
        thread.waitState();

        threads.put(threadName, thread);

        updateMonitorInfo(threadName);
    }

    public static void main(String args[]) {
        OwnedMonitorsDebuggee debuggee = new OwnedMonitorsDebuggee();
        debuggee.doTest(args);
    }

}
