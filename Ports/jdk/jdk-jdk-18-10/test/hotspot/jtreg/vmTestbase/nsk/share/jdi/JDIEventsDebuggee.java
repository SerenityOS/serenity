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
import nsk.share.TestBug;

/*
 *  Subclasses of this class generate events and if needed
 *  save debug information about generated events
 */
abstract class EventActionsExecutor {
    // should this event generator save information about generated events
    public boolean saveEventData;

    // information about generated events(this data available for debugger)
    public List<DebuggeeEventData.DebugEventData> debugEventDataList = new ArrayList<DebuggeeEventData.DebugEventData>();

    public abstract void doEventAction();

    protected void addEventData(DebuggeeEventData.DebugEventData eventData) {
        if (saveEventData)
            debugEventDataList.add(eventData);
    }

    public void clearDebugData() {
        debugEventDataList.clear();
    }
}

/*
 * Class handles commands for running given number of threads which generate
 * given events. Objects which generate events save information about generated
 * events and this data available for debugger
 *
 * Class was written to test monitor evens(MonitorWaitEvent, MonitorWaitedEvent,
 * MonitorContendedEnterEvent, MonitorContendedEnteredEvent), possible it can be
 * used in tests for other events
 */
abstract public class JDIEventsDebuggee extends AbstractJDIDebuggee {
    protected String[] doInit(String[] args) {
        Thread.currentThread().setName(MAIN_THREAD_NAME);
        return super.doInit(args);
    }

    public static final String MAIN_THREAD_NAME = "JDIEventsDebuggee_MainThread";

    // command:events_count:event types
    public static final String COMMAND_CREATE_ACTIONS_EXECUTORS = "createActionsExecutors";

    // command
    public static final String COMMAND_START_EXECUTION = "startExecution";

    // command
    public static final String COMMAND_WAIT_EXECUTION_COMPLETION = "waitExecutionCompletion";

    // command
    public static final String COMMAND_STOP_EXECUTION = "stopExecution";

    protected List<EventActionsThread> eventActionsExecutorsPool = new ArrayList<EventActionsThread>();

    // initialize with empty array
    public static DebuggeeEventData.DebugEventData generatedEvents[] = new DebuggeeEventData.DebugEventData[0];

    // debuggee's main thread also can generate events and information about
    // this events should be saved (like for event generators)
    protected boolean saveEventData;

    public boolean parseCommand(String command) {
        if (super.parseCommand(command))
            return true;

        StreamTokenizer tokenizer = new StreamTokenizer(new StringReader(
                command));
        tokenizer.whitespaceChars(':', ':');
        tokenizer.wordChars('_', '_');
        tokenizer.wordChars(' ', ' ');

        try {
            if (command.startsWith(COMMAND_CREATE_ACTIONS_EXECUTORS)) {
                tokenizer.nextToken();

                if (tokenizer.nextToken() != StreamTokenizer.TT_NUMBER)
                    throw new TestBug("Invalid command format: " + command);

                int eventsCount = (int) tokenizer.nval;

                if (tokenizer.nextToken() != StreamTokenizer.TT_WORD)
                    throw new TestBug("Invalid command format: " + command);

                // pass to the createActionsExecutors() string describing types
                // of tested events
                createActionsExecutors(tokenizer.sval, eventsCount);

                return true;
            } else if (command.equals(COMMAND_START_EXECUTION)) {
                startExecution();

                return true;
            } else if (command.equals(COMMAND_WAIT_EXECUTION_COMPLETION)) {
                if (executionControllingThread == null)
                    throw new TestBug("executionControllingThread wasn't started");

                try {
                    executionControllingThread.join();
                    executionControllingThread = null;
                } catch (InterruptedException e) {
                    unexpectedException(e);
                }

                return true;
            } else if (command.equals(COMMAND_STOP_EXECUTION)) {
                if (executionControllingThread == null)
                    throw new TestBug("executionControllingThread wasn't started");

                for (EventActionsThread thread : eventActionsExecutorsPool) {
                    thread.stopExecution();
                }

                return true;
            }

        } catch (IOException e) {
            throw new TestBug("Invalid command format: " + command);
        }

        return false;
    }

    // debugee waits completion of test threads in separate thread to free thread listening commands
    protected Thread executionControllingThread;

    protected void startExecution() {
        if (eventActionsExecutorsPool.size() == 0) {
            throw new TestBug("ActionsExecutors were not created");
        }

        for (EventActionsThread thread : eventActionsExecutorsPool) {
            thread.startExecution();
        }

        // debugee waits completion of test threads in separate thread to free thread listening commands
        executionControllingThread = new Thread(
                new Runnable() {
                    public void run() {
                        for (EventActionsThread thread : eventActionsExecutorsPool) {
                            try {
                                thread.join();
                            } catch (InterruptedException e) {
                                unexpectedException(e);
                            }
                        }

                        completeExecution();
                    }
                });

        executionControllingThread.start();
    }

    protected void completeExecution() {
        // save information about all generated events in array 'generatedEvents'
        List<DebuggeeEventData.DebugEventData> generatedEventsList = new ArrayList<DebuggeeEventData.DebugEventData>();

        for (EventActionsThread thread : eventActionsExecutorsPool)
            generatedEventsList.addAll(thread.executor.debugEventDataList);

        generatedEvents = generatedEventsList.toArray(new DebuggeeEventData.DebugEventData[]{});

        // stop at breakpoint when all events was generated
        breakpointMethod();

        // clear data about generated events to allow execute command
        // several times
        clearResults();
    }

    // clear data about generated events to allow execute test several times
    protected void clearResults() {
        for (EventActionsThread thread : eventActionsExecutorsPool)
            thread.executor.clearDebugData();

        eventActionsExecutorsPool.clear();

        generatedEvents = new DebuggeeEventData.DebugEventData[0];
    }

    // create threads generating events
    abstract protected void createActionsExecutors(String description,
            int eventsCount);

    /*
     * Thread generating events, call in loop
     * EventActionsExecutor.doEventAction()
     */
    static class EventActionsThread extends Thread {
        // how many times call executor.doMonitorAction()
        private int actionsNumber;

        // object generating events
        public EventActionsExecutor executor;

        public EventActionsThread(EventActionsExecutor executor,
                int actionsNumber) {
            this.actionsNumber = actionsNumber;
            this.executor = executor;
        }

        private volatile boolean startExecution;
        private volatile boolean stopExecution;

        public void run() {
            while (!startExecution)
                Thread.yield();

            for (int i = 0; (i < actionsNumber) && !stopExecution; i++)
                executor.doEventAction();
        }

        public void startExecution() {
            startExecution = true;
        }

        public void stopExecution() {
            stopExecution = true;
        }
    }

}
