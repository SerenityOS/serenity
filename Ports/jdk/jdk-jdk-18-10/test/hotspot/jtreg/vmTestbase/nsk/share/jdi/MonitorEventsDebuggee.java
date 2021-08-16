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

import java.util.*;
import nsk.share.TestBug;
import nsk.share.locks.MonitorLockingThread;

/*
 * This class generates MonitorWaitEvent and MonitorWaitedEvent
 */
class MonitorWaitExecutor extends EventActionsExecutor {
    // MonitorWaited event may occurs when waiting thread was interrupted,
    // notified by notify or notifyAll, or when timeout expired
    enum ExitFromWaitType {
        EXIT_WITH_TIMEOUT,
        INTERRUPT,
        NOTIFY,
        NOTIFY_ALL
    }

    // this thread forces MonitorWaitExecutor to exit from wait()
    class AuxiliaryThread extends Thread {
        private Thread threadToInterrupt;

        private Object monitor;

        public AuxiliaryThread(Thread threadToInterrupt, Object monitor) {
            this.threadToInterrupt = threadToInterrupt;
            this.monitor = monitor;
        }

        public void run() {
            // wait when interrupted thread switches state to 'TIMED_WAITING'
            while ((threadToInterrupt.getState() != Thread.State.WAITING) && !exitedFromWait) {
                Thread.yield();
            }

            // threadToInterrupt 'spuriously' exited from wait()
            if(exitedFromWait)
                return;

            if (exitFromWaitType == ExitFromWaitType.INTERRUPT) {
                threadToInterrupt.interrupt();
            } else if ((exitFromWaitType == ExitFromWaitType.NOTIFY)
                    || (exitFromWaitType == ExitFromWaitType.NOTIFY_ALL)) {
                /*
                 * NOTE: thread's state WAITING doesn't guarantee that thread released
                 * monitor, and entering to the next synchronized block may cause MonitorContentedEnterEvent
                 * (if corresponding request was created).
                 *
                 * Debugger should take in account this issue.
                 */
                synchronized (monitor) {
                    if (exitFromWaitType == ExitFromWaitType.NOTIFY)
                        monitor.notify();
                    else if (exitFromWaitType == ExitFromWaitType.NOTIFY_ALL)
                        monitor.notifyAll();
                }
            }
        }
    }

    // thread may 'spuriously' exit from wait(), in this case AuxiliaryThread shouldn't wait 'WAITING' state
    private volatile boolean exitedFromWait;

    // save data about MonitorWait events
    private boolean monitorWait;

    // save data about MonitorWaited events
    private boolean monitorWaited;

    public MonitorWaitExecutor(boolean monitorWait, boolean monitorWaited) {
        this.monitorWait = monitorWait;
        this.monitorWaited = monitorWaited;
    }

    protected ExitFromWaitType exitFromWaitType;

    public void doEventAction() {
        for (ExitFromWaitType exitType : ExitFromWaitType.values()) {
            exitFromWaitType = exitType;

            monitorWait();
        }
    }

    protected void monitorWait() {

        exitedFromWait = false;

        long timeout;

        if (exitFromWaitType == ExitFromWaitType.EXIT_WITH_TIMEOUT)
            timeout = 100;
        else
            timeout = 0;

        if (monitorWait) {
            DebuggeeEventData.DebugMonitorWaitEventData eventData = new DebuggeeEventData.DebugMonitorWaitEventData(
                    this, Thread.currentThread(), timeout, this);
            addEventData(eventData);
        }

        if (monitorWaited) {
            DebuggeeEventData.DebugMonitorWaitedEventData eventData = new DebuggeeEventData.DebugMonitorWaitedEventData(
                    this, Thread.currentThread(),
                    (exitFromWaitType == ExitFromWaitType.EXIT_WITH_TIMEOUT),
                    this);
            addEventData(eventData);
        }

        AuxiliaryThread auxiliaryThread = null;

        if (exitFromWaitType != ExitFromWaitType.EXIT_WITH_TIMEOUT) {
            auxiliaryThread = new AuxiliaryThread(Thread.currentThread(), this);
            auxiliaryThread.start();
        }

        try {
            eventMethod(timeout);
        } catch (InterruptedException e) {
            // expected exception
        }

        exitedFromWait = true;

        if (auxiliaryThread != null) {
            // don't using join, because of join is realized with using of
            // wait(), and this method can generate unexpected events
            while (auxiliaryThread.getState() != Thread.State.TERMINATED)
                Thread.yield();
        }
    }

    // move event actions to the separate method to simplify method redifinition
    // in subclasses
    synchronized protected void eventMethod(long timeout)
            throws InterruptedException {
        wait(timeout);
    }

}

/*
 * Subclass of MonitorWaitExecutor, define its own event method(copy of parent
 * method), intended for event filters test
 */
class MonitorWaitExecutor_1Subclass extends MonitorWaitExecutor {
    public MonitorWaitExecutor_1Subclass(boolean monitorWait,
            boolean monitorWaited) {
        super(monitorWait, monitorWaited);
    }

    synchronized protected void eventMethod(long timeout)
            throws InterruptedException {
        wait(timeout);
    }
}

/*
 * Subclass of MonitorWaitExecutor, define its own event method(copy of parent
 * method), intended for event filters test
 */
class MonitorWaitExecutor_2Subclass extends MonitorWaitExecutor_1Subclass {
    public MonitorWaitExecutor_2Subclass(boolean monitorWait,
            boolean monitorWaited) {
        super(monitorWait, monitorWaited);
    }

    synchronized protected void eventMethod(long timeout)
            throws InterruptedException {
        wait(timeout);
    }
}

/*
 * This class generates MonitorContendedEnterEvent and
 * MonitorContendedEnteredEvent
 */
class MonitorEnterExecutor extends EventActionsExecutor {
    /*
     * Types of monitor acquiring:
     * - through synchronized block
     * - through synchronized method
     * - through JNI MonitorEnter
     */
    static enum MonitorAcquireType {
        SYNCHRONIZED_BLOCK,
        SYNCHRONIZED_METHOD,
        JNI_MONITOR_ENTER
    }

    // native method uses this class
    private static final Class<?> jniError = nsk.share.TestJNIError.class;


    static final String nativeLibararyName = "MonitorEnterExecutor";

    static {
        System.loadLibrary(nativeLibararyName);
    }

    // this thread forces MonitorLockingThread to release holding lock
    static class LockFreeThread extends Thread {
        private Thread blockedThread;

        private MonitorLockingThread lockingThread;

        public LockFreeThread(Thread blockedThread,
                MonitorLockingThread lockingThread) {
            this.blockedThread = blockedThread;
            this.lockingThread = lockingThread;
        }

        public void run() {
            // wait when blocked thread switches state to 'BLOCKED'
            while (blockedThread.getState() != Thread.State.BLOCKED)
                Thread.yield();

            lockingThread.releaseLock();
        }
    }

    private boolean monitorEnter;

    private boolean monitorEntered;

    public MonitorEnterExecutor(boolean monitorEnter, boolean monitorEntered) {
        this.monitorEnter = monitorEnter;
        this.monitorEntered = monitorEntered;
    }

    protected void monitorEnter() {
        // locking thread acquires 'this' lock
        MonitorLockingThread lockingThread = new MonitorLockingThread(this);
        lockingThread.acquireLock();

        if (monitorEnter) {
            DebuggeeEventData.DebugMonitorEnterEventData eventData = new DebuggeeEventData.DebugMonitorEnterEventData(
                    this, Thread.currentThread(), this);
            addEventData(eventData);
        }
        if (monitorEntered) {
            DebuggeeEventData.DebugMonitorEnteredEventData eventData = new DebuggeeEventData.DebugMonitorEnteredEventData(
                    this, Thread.currentThread(), this);
            addEventData(eventData);
        }

        /*
         * This thread forces lockingThread to free 'this' lock when current thread's state will change to 'BLOCKED'
         *
         * NOTE: this method to provoke MonitorContended events isn't 100% robust
         * Tests should take in account this issue.
         *
         */
        LockFreeThread lockFreeThread = new LockFreeThread(Thread.currentThread(), lockingThread);
        lockFreeThread.start();

        // try to acquire 'this' lock
        eventMethod();

        while(lockingThread.getState() != Thread.State.TERMINATED)
            Thread.yield();

        while(lockFreeThread.getState() != Thread.State.TERMINATED)
            Thread.yield();
    }

    private MonitorAcquireType monitorAcquireType;

    // generate events in different ways
    public void doEventAction() {
        for (MonitorAcquireType monitorAcquireType : MonitorAcquireType.values()) {
            this.monitorAcquireType = monitorAcquireType;
            monitorEnter();
        }
    }

    protected void eventMethod() {
        switch (monitorAcquireType) {
        case SYNCHRONIZED_BLOCK:
            synchronizedBlock();
            break;
        case SYNCHRONIZED_METHOD:
            synchronizedMethod();
            break;
        case JNI_MONITOR_ENTER:
            nativeJNIMonitorEnter();
            break;

        default:
            throw new TestBug("Invalid monitorAcquireType: "
                    + monitorAcquireType);
        }
    }

    // move event actions to the separate methods to simplify method
    // redifinition in subclasses:

    protected void synchronizedBlock() {
        // MonitorContendedEnterEvent and MonitorContendedEnteredEvent should occur here
        synchronized (this) {
            // don't expect events for following synchronized block
            synchronized (this) {
            }
        }
    }

    // MonitorContendedEnterEvent and MonitorContendedEnteredEvent should occur here
    synchronized protected void synchronizedMethod() {
        // don't expect events for following synchronized block
        synchronized (this) {
        }
    }

    // call JNI methods MonitorEnter and MonitorExit for 'this' object
    protected native void nativeJNIMonitorEnter();

}

/*
 * Subclass of MonitorEnterExecutor, defines its own event methods(copy of parent
 * method), intended for event filters test
 */
class MonitorEnterExecutor_1Subclass extends MonitorEnterExecutor {
    static {
        System.loadLibrary(nativeLibararyName);
    }

    public MonitorEnterExecutor_1Subclass(boolean monitorEnter,
            boolean monitorEntered) {
        super(monitorEnter, monitorEntered);
    }

    protected void synchronizedBlock() {
        // MonitorContendedEnterEvent and MonitorContendedEnteredEvent should occur here
        synchronized (this) {
        }
    }

    // MonitorContendedEnterEvent and MonitorContendedEnteredEvent should occur here
    synchronized protected void synchronizedMethod() {
    }

    // call JNI methods MonitorEnter and MonitorExit for 'this' object
    protected native void nativeJNIMonitorEnter();
}

/*
 * Subclass of MonitorEnterExecutor, defines its own event methods(copy of parent
 * method), intended for event filters test
 */
class MonitorEnterExecutor_2Subclass extends MonitorEnterExecutor_1Subclass {
    static {
        System.loadLibrary(nativeLibararyName);
    }

    public MonitorEnterExecutor_2Subclass(boolean monitorEnter,
            boolean monitorEntered) {
        super(monitorEnter, monitorEntered);
    }

    protected void synchronizedBlock() {
        // MonitorContendedEnterEvent and MonitorContendedEnteredEvent should occur here
        synchronized (this) {
        }
    }

    // MonitorContendedEnterEvent and MonitorContendedEnteredEvent should occur here
    synchronized protected void synchronizedMethod() {
    }

    // call JNI methods MonitorEnter and MonitorExit for 'this' object
    protected native void nativeJNIMonitorEnter();
}

/*
 * Class is used as base debuggee in tests for following events and event requests:
 * - MonitorContendedEnterRequest / MonitorContendedEnterEvent
 * - MonitorContendedEnteredRequest / MonitorContendedEnteredEvent
 * - MonitorWaitRequest / MonitorWaitEvent
 * - MonitorWaitedRequest / MonitorWaitedEvent
 */
public class MonitorEventsDebuggee extends JDIEventsDebuggee {
    public static void main(String[] args) {
        MonitorEventsDebuggee debuggee = new MonitorEventsDebuggee();
        debuggee.doTest(args);
    }

    protected void createActionsExecutors(String description, int eventsCount) {
        boolean monitorEnter = description
                .contains(JDIEventsDebugger.EventType.MONITOR_CONTENTED_ENTER
                        .name());
        boolean monitorEntered = description
                .contains(JDIEventsDebugger.EventType.MONITOR_CONTENTED_ENTERED
                        .name());
        boolean monitorWait = description
                .contains(JDIEventsDebugger.EventType.MONITOR_WAIT.name());
        boolean monitorWaited = description
                .contains(JDIEventsDebugger.EventType.MONITOR_WAITED.name());

        if (monitorEnter || monitorEntered || monitorWait || monitorWaited) {
            createActionsExecutors(monitorEnter, monitorEntered, monitorWait,
                    monitorWaited, eventsCount);
        } else
            throw new TestBug(
                    "Invalid command format (required event not specified)");
    }

    private List<DebuggeeEventData.DebugMonitorEventData> eventsData = new ArrayList<DebuggeeEventData.DebugMonitorEventData>();

    protected void clearResults() {
        super.clearResults();
        eventsData.clear();
    }

    private void createActionsExecutors(boolean monitorEnter,
            boolean monitorEntered, boolean monitorWait, boolean monitorWaited,
            int actionsCount) {
        EventActionsThread thread;

        // create 3 instances of generating objects of 3 different classes (for
        // event filters tests)
        if (monitorEnter || monitorEntered) {
            thread = new EventActionsThread(new MonitorEnterExecutor(
                    monitorEnter, monitorEntered), actionsCount);
            thread.start();
            eventActionsExecutorsPool.add(thread);

            thread = new EventActionsThread(new MonitorEnterExecutor_1Subclass(
                    monitorEnter, monitorEntered), actionsCount);
            thread.start();
            eventActionsExecutorsPool.add(thread);

            thread = new EventActionsThread(new MonitorEnterExecutor_2Subclass(
                    monitorEnter, monitorEntered), actionsCount);
            thread.start();
            eventActionsExecutorsPool.add(thread);
        }

        // create 3 instances of generating objects of 3 different classes (for
        // event filters tests)
        if (monitorWait || monitorWaited) {
            thread = new EventActionsThread(new MonitorWaitExecutor(
                    monitorWait, monitorWaited), actionsCount);
            thread.start();
            eventActionsExecutorsPool.add(thread);

            thread = new EventActionsThread(new MonitorWaitExecutor_1Subclass(
                    monitorWait, monitorWaited), actionsCount);
            thread.start();
            eventActionsExecutorsPool.add(thread);

            thread = new EventActionsThread(new MonitorWaitExecutor_2Subclass(
                    monitorWait, monitorWaited), actionsCount);
            thread.start();
            eventActionsExecutorsPool.add(thread);
        }
    }

    // start event generating threads and wait when all this threads finish
    // execution
    // override parent method because of Thread.join() used in parent method
    // generates unexpected MonitorWait/MonitorWaited events
    protected void startExecution() {
        if (eventActionsExecutorsPool.size() == 0) {
            throw new TestBug("ActionsExecutors were not created");
        }

        for (EventActionsThread thread : eventActionsExecutorsPool) {
            thread.startExecution();
        }

        // wait completion of test threads in separate thread to free thread listening commands
        executionControllingThread = new Thread(
                new Runnable() {
                    public void run() {
                        boolean executionCompleted;
                        do {
                            try {
                                // give time to event generators
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                unexpectedException(e);
                            }
                            executionCompleted = true;
                            for (EventActionsThread thread : eventActionsExecutorsPool) {
                                if (thread.isAlive()) {
                                    executionCompleted = false;
                                    break;
                                }
                            }
                        } while (!executionCompleted);

                        completeExecution();
                    }
                });

        executionControllingThread.start();
    }

}
