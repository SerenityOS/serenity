/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package org.netbeans.jemmy;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.InvocationEvent;
import java.lang.reflect.InvocationTargetException;

/**
 *
 * Provides functionality to work with java.awt.EventQueue empty.
 *
 * <BR><BR>Timeouts used: <BR>
 * QueueTool.WaitQueueEmptyTimeout - timeout to wait queue emptied<BR>
 * QueueTool.QueueCheckingDelta - time delta to check result<BR>
 * QueueTool.LockTimeout - time to wait queue locked after lock action has been
 * put there<BR>
 * QueueTool.InvocationTimeout - time for action was put into queue to be
 * started<BR>
 * QueueTool.MaximumLockingTime - maximum time to lock queue.<br>
 *
 * @see Timeouts
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 *
 */
public class QueueTool implements Outputable, Timeoutable {

    private final static long WAIT_QUEUE_EMPTY_TIMEOUT = 180000;
    private final static long QUEUE_CHECKING_DELTA = 10;
    private final static long LOCK_TIMEOUT = 180000;
    private final static long MAXIMUM_LOCKING_TIME = 180000;
    private final static long INVOCATION_TIMEOUT = 180000;

    private static JemmyQueue jemmyQueue = null;

    private TestOut output;
    private Timeouts timeouts;
    private Locker locker;
    private Waiter<String, Void> lockWaiter;

    /**
     * Constructor.
     */
    public QueueTool() {
        locker = new Locker();
        lockWaiter = new Waiter<String, Void>(new Waitable<String, Void>() {
            @Override
            public String actionProduced(Void obj) {
                return locker.isLocked() ? "" : null;
            }

            @Override
            public String getDescription() {
                return "Event queue to be locked";
            }

            @Override
            public String toString() {
                return "QueueTool.Waiter{" + getDescription() + '}';
            }
        });
        setOutput(JemmyProperties.getProperties().getOutput());
        setTimeouts(JemmyProperties.getProperties().getTimeouts());
    }

    /**
     * Returns system EventQueue.
     *
     * @return system EventQueue.
     */
    public static EventQueue getQueue() {
        return Toolkit.getDefaultToolkit().getSystemEventQueue();
    }

    /**
     * Map to {@code EventQueue.isDispatchThread()}.
     *
     * @return true if this thread is the AWT dispatching thread.
     */
    public static boolean isDispatchThread() {
        return EventQueue.isDispatchThread();
    }

    /**
     * Checks if system event queue is empty.
     *
     * @return true if EventQueue is empty.
     */
    public static boolean checkEmpty() {
        return getQueue().peekEvent() == null;
    }

    /**
     * Shortcuts event if
     * {@code ((JemmyProperties.getCurrentDispatchingModel() & JemmyProperties.SHORTCUT_MODEL_MASK) != 0)}
     * and if executed in the dispatch thread. Otherwise posts event.
     *
     * @param event Event to dispatch.
     */
    public static void processEvent(AWTEvent event) {
        if ((JemmyProperties.getCurrentDispatchingModel()
                & JemmyProperties.SHORTCUT_MODEL_MASK) != 0) {
            installQueue();
        }
        if ((JemmyProperties.getCurrentDispatchingModel()
                & JemmyProperties.SHORTCUT_MODEL_MASK) != 0
                && isDispatchThread()) {
            shortcutEvent(event);
        } else {
            postEvent(event);
        }
    }

    /**
     * Simply posts events into the system event queue.
     *
     * @param event Event to dispatch.
     */
    public static void postEvent(AWTEvent event) {
        getQueue().postEvent(event);
    }

    /**
     * Dispatches event ahead of all events staying in the event queue.
     *
     * @param event an event to be shortcut.
     */
    public static void shortcutEvent(AWTEvent event) {
        installQueue();
        jemmyQueue.shortcutEvent(event);
    }

    /**
     * Installs own Jemmy EventQueue implementation. The method is executed in
     * dispatchmode only.
     *
     * @see #uninstallQueue
     */
    public static void installQueue() {
        if (jemmyQueue == null) {
            jemmyQueue = new JemmyQueue();
        }
        jemmyQueue.install();
    }

    /**
     * Uninstalls own Jemmy EventQueue implementation.
     *
     * @see #installQueue
     */
    public static void uninstallQueue() {
        if (jemmyQueue != null) {
            jemmyQueue.uninstall();
        }
    }

    static {
        Timeouts.initDefault("QueueTool.WaitQueueEmptyTimeout", WAIT_QUEUE_EMPTY_TIMEOUT);
        Timeouts.initDefault("QueueTool.QueueCheckingDelta", QUEUE_CHECKING_DELTA);
        Timeouts.initDefault("QueueTool.LockTimeout", LOCK_TIMEOUT);
        Timeouts.initDefault("QueueTool.InvocationTimeout", INVOCATION_TIMEOUT);
        Timeouts.initDefault("QueueTool.MaximumLockingTime", MAXIMUM_LOCKING_TIME);
    }

    /**
     * Defines current timeouts.
     *
     * @param ts ?t? A collection of timeout assignments.
     * @see org.netbeans.jemmy.Timeouts
     * @see org.netbeans.jemmy.Timeoutable
     * @see #getTimeouts
     */
    @Override
    public void setTimeouts(Timeouts ts) {
        timeouts = ts;
        lockWaiter.setTimeouts(getTimeouts().cloneThis());
    }

    /**
     * Return current timeouts.
     *
     * @return the collection of current timeout assignments.
     * @see org.netbeans.jemmy.Timeouts
     * @see org.netbeans.jemmy.Timeoutable
     * @see #setTimeouts
     */
    @Override
    public Timeouts getTimeouts() {
        return timeouts;
    }

    /**
     * Defines print output streams or writers.
     *
     * @param out Identify the streams or writers used for print output.
     * @see org.netbeans.jemmy.Outputable
     * @see org.netbeans.jemmy.TestOut
     * @see #getOutput
     */
    @Override
    public void setOutput(TestOut out) {
        output = out;
        lockWaiter.setOutput(output.createErrorOutput());
    }

    /**
     * Returns print output streams or writers.
     *
     * @return an object that contains references to objects for printing to
     * output and err streams.
     * @see org.netbeans.jemmy.Outputable
     * @see org.netbeans.jemmy.TestOut
     * @see #setOutput
     */
    @Override
    public TestOut getOutput() {
        return output;
    }

    /**
     * Waits for system event queue empty. Uses
     * "QueueTool.WaitQueueEmptyTimeout" milliseconds to wait.
     *
     * @throws TimeoutExpiredException
     */
    public void waitEmpty() {
        Waiter<String, Void> waiter = new Waiter<>(new Waitable<String, Void>() {
            @Override
            public String actionProduced(Void obj) {
                if (checkEmpty()) {
                    return "Empty";
                }
                return null;
            }

            @Override
            public String getDescription() {
                return "Wait event queue empty";
            }

            @Override
            public String toString() {
                return "waitEmpty.Waiter{" + getDescription() + '}';
            }
        });
        waiter.setTimeoutsToCloneOf(timeouts, "QueueTool.WaitQueueEmptyTimeout");
        waiter.setOutput(output);
        try {
            waiter.waitAction(null);
        } catch (TimeoutExpiredException e) {
            final AWTEvent event = getQueue().peekEvent();
            // if event != null run toString in dispatch thread
            String eventToString = (event == null) ? "null" : invokeSmoothly(
                    new QueueTool.QueueAction<String>("event.toString()") {
                @Override
                public String launch() {
                    return event.toString();
                }
            }
            );
            getOutput().printErrLine("Event at the top of stack: " + eventToString);
            throw (e);
        } catch (InterruptedException e) {
            output.printStackTrace(e);
        }
    }

    /**
     * Waits for system event queue be empty for {@code emptyTime}
     * milliseconds. Uses "QueueTool.WaitQueueEmptyTimeout" milliseconds to
     * wait.
     *
     * @param emptyTime time for the queue to stay empty.
     * @throws TimeoutExpiredException
     */
    public void waitEmpty(long emptyTime) {

        StayingEmptyWaiter waiter = new StayingEmptyWaiter(emptyTime);
        waiter.setTimeoutsToCloneOf(timeouts, "QueueTool.WaitQueueEmptyTimeout");
        waiter.setOutput(output);
        try {
            waiter.waitAction(null);
        } catch (TimeoutExpiredException e) {
            final AWTEvent event = getQueue().peekEvent();
            String eventToString = (event == null) ? "null" : invokeSmoothly(
                    new QueueTool.QueueAction<String>("event.toString()") {
                @Override
                public String launch() {
                    return event.toString();
                }
            }
            );
            getOutput().printErrLine("Event at the top of stack: " + eventToString);
            throw (e);
        } catch (InterruptedException e) {
            output.printStackTrace(e);
        }
    }

    /**
     * Invokes action through EventQueue. Does not wait for it execution.
     *
     * @param action an action to be invoked.
     */
    public void invoke(QueueAction<?> action) {
        output.printTrace("Invoking \"" + action.getDescription() + "\" action through event queue");
        EventQueue.invokeLater(action);
    }

    /**
     * Invokes runnable through EventQueue. Does not wait for it execution.
     *
     * @param runnable a runnable to be invoked.
     * @return QueueAction instance which can be use for execution monitoring.
     * @see QueueTool.QueueAction
     */
    public QueueAction<Void> invoke(Runnable runnable) {
        QueueAction<Void> result = new RunnableRunnable(runnable);
        invoke(result);
        return result;
    }

    /**
     * Invokes action through EventQueue. Does not wait for it execution.
     *
     * @param action an action to be invoked.
     * @param param {@code action.launch(Object)} method parameter.
     * @return QueueAction instance which can be use for execution monitoring.
     * @see QueueTool.QueueAction
     */
    public <R, P> QueueAction<R> invoke(Action<R, P> action, P param) {
        QueueAction<R> result = new ActionRunnable<>(action, param);
        invoke(result);
        return result;
    }

    /**
     * Being executed outside of AWT dispatching thread, invokes an action
     * through the event queue. Otherwise executes {@code action.launch()}
     * method directly.
     *
     * @param action anaction to be executed.
     * @return Action result.
     */
    public <R> R invokeSmoothly(QueueAction<R> action) {
        if (!EventQueue.isDispatchThread()) {
            return invokeAndWait(action);
        } else {
            try {
                return action.launch();
            } catch (Exception e) {
                throw (new JemmyException("Exception in " + action.getDescription(), e));
            }
        }
    }

    /**
     * Being executed outside of AWT dispatching thread, invokes a runnable
     * through the event queue. Otherwise executes {@code runnable.run()}
     * method directly.
     *
     * @param runnable a runnable to be executed.
     */
    public void invokeSmoothly(Runnable runnable) {
        if (!EventQueue.isDispatchThread()) {
            invokeAndWait(runnable);
        } else {
            runnable.run();
        }
    }

    /**
     * Being executed outside of AWT dispatching thread, invokes an action
     * through the event queue. Otherwise executes
     * {@code action.launch(Object)} method directly.
     *
     * @param action anaction to be executed.
     * @param param an action parameter
     * @return Action result.
     */
    public <R, P> R invokeSmoothly(Action<R, P> action, P param) {
        if (!EventQueue.isDispatchThread()) {
            return invokeAndWait(action, param);
        } else {
            return action.launch(param);
        }
    }

    /**
     * Invokes action through EventQueue. Waits for it execution.
     *
     * @param action an action to be invoked.
     * @return a result of action
     * @throws TimeoutExpiredException if action was not executed in
     * "QueueTool.InvocationTimeout" milliseconds.
     */
    public <R> R invokeAndWait(QueueAction<R> action) {

        class JemmyInvocationLock {
        }
        Object lock = new JemmyInvocationLock();
        InvocationEvent event
                = new JemmyInvocationEvent(Toolkit.getDefaultToolkit(),
                        action,
                        lock,
                        true);
        try {
            synchronized (lock) {
                getQueue().postEvent(event);
                while (!action.getFinished()) {
                    lock.wait();
                }
            }
        } catch (InterruptedException e) {
            throw (new JemmyException("InterruptedException during "
                    + action.getDescription()
                    + " execution", e));
        }
        if (action.getException() != null) {
            throw (new JemmyException("Exception in " + action.getDescription(),
                    action.getException()));
        }
        if (event.getException() != null) {
            throw (new JemmyException("Exception in " + action.getDescription(),
                    event.getException()));
        }
        return action.getResult();
    }

    public static final class JemmyInvocationEvent extends InvocationEvent {

        private static final long serialVersionUID = 42L;

        public JemmyInvocationEvent(Object source, Runnable runnable,
                Object notifier, boolean catchThrowables) {
            super(source, runnable, notifier, catchThrowables);
        }
    }

    /**
     * Invokes runnable through EventQueue. Waits for it execution.
     *
     * @param runnable a runnable to be invoked.
     * @throws TimeoutExpiredException if runnable was not executed in
     * "QueueTool.InvocationTimeout" milliseconds.
     */
    public void invokeAndWait(Runnable runnable) {
        invokeAndWait(new RunnableRunnable(runnable));
    }

    /**
     * Invokes action through EventQueue. Waits for it execution. May throw
     * TimeoutExpiredException if action was not executed in
     * "QueueTool.InvocationTimeout" milliseconds.
     *
     * @param action an action to be invoked.
     * @param param action.launch(Object method parameter.
     * @return a result of action
     * @throws TimeoutExpiredException if action was not executed in
     * "QueueTool.InvocationTimeout" milliseconds.
     */
    public <R, P> R invokeAndWait(Action<R, P> action, P param) {
        return invokeAndWait(new ActionRunnable<>(action, param));
    }

    /**
     * Locks EventQueue. Locking will be automatically aborted after
     * "QueueTool.MaximumLockingTime" milliseconds.
     *
     * @see #unlock()
     * @throws TimeoutExpiredException
     */
    public void lock() {
        output.printTrace("Locking queue.");
        invoke(locker);
        try {
            lockWaiter.
                    getTimeouts().
                    setTimeout("Waiter.WaitingTime",
                            timeouts.
                            getTimeout("QueueTool.LockTimeout"));
            lockWaiter.
                    getTimeouts().
                    setTimeout("Waiter.TimeDelta",
                            timeouts.
                            getTimeout("QueueTool.QueueCheckingDelta"));
            lockWaiter.waitAction(null);
        } catch (InterruptedException e) {
            output.printStackTrace(e);
        }
    }

    /**
     * Unlocks EventQueue.
     *
     * @see #lock()
     */
    public void unlock() {
        output.printTrace("Unlocking queue.");
        locker.setLocked(false);
    }

    /**
     * Locks event queue for "time" milliseconds. Returns immediately after
     * locking.
     *
     * @param time a time to lock the queue for.
     */
    public void lock(long time) {
        output.printTrace("Locking queue for " + Long.toString(time) + " milliseconds");
        lock();
        invoke(new UnlockPostponer(time));
    }

    /**
     * Sais if last locking was expired.
     *
     * @return true if last locking had beed expired.
     */
    public boolean wasLockingExpired() {
        return locker.expired;
    }

    /**
     * Action to be executed through event queue. Even if it was executed without
     * waiting by {@code invoke(QueueAction)} execution process can be
     * monitored by {@code getResult()}, {@code getException()},
     * {@code getFinished()} methods.
     */
    public static abstract class QueueAction<R> implements Runnable {

        private volatile boolean finished;
        private Exception exception;
        private R result;
        private String description;

        /**
         * Constructor.
         *
         * @param description a description.
         */
        public QueueAction(String description) {
            this.description = description;
            finished = false;
            exception = null;
            result = null;
        }

        /**
         * Method to implement action functionality.
         *
         * @return an Object - action result
         * @throws Exception
         */
        public abstract R launch()
                throws Exception;

        /**
         */
        @Override
        public final void run() {
            finished = false;
            exception = null;
            result = null;
            try {
                result = launch();
            } catch (Exception e) {
                exception = e;
            } finally {
                finished = true;
            }
        }

        /**
         * Action description.
         *
         * @return the description.
         */
        public String getDescription() {
            return description;
        }

        /**
         * Returns action result if action has already been finished, null
         * otherwise.
         *
         * @return an action result.
         */
        public R getResult() {
            return result;
        }

        /**
         * Returns exception occured during action execution (if any).
         *
         * @return the Exception happened inside {@code launch()} method.
         */
        public Exception getException() {
            return exception;
        }

        /**
         * Informs whether action has been finished or not.
         *
         * @return true if this action have been finished
         */
        public boolean getFinished() {
            return finished;
        }

        @Override
        public String toString() {
            return "QueueAction{description=" + description + ", result=" + result + ", finished=" + finished + ", exception=" + exception + '}';
        }
    }

    private static class JemmyQueue extends EventQueue {

        private boolean installed = false;

        public void shortcutEvent(AWTEvent event) {
            super.dispatchEvent(event);
        }

        @Override
        protected void dispatchEvent(AWTEvent event) {
            //it's necessary to catch exception here.
            //because test might already fail by timeout
            //but generated events are still in stack
            try {
                super.dispatchEvent(event);
            } catch (Exception e) {
                //the exceptions should be printed into
                //Jemmy output - not System.out
                JemmyProperties.getCurrentOutput().printStackTrace(e);
            }
        }

        public synchronized void install() {
            if (!installed) {
                getQueue().push(this);
                installed = true;
            }
        }

        public synchronized void uninstall() {
            if (installed) {
                pop();
                installed = false;
            }
        }
    }

    private class EventWaiter implements Runnable {

        boolean empty = true;
        long emptyTime;

        public EventWaiter(long emptyTime) {
            this.emptyTime = emptyTime;
        }

        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            while ((empty = checkEmpty())
                    && (System.currentTimeMillis() - startTime) < emptyTime) {
                timeouts.sleep("QueueTool.QueueCheckingDelta");
            }
        }
    }

    private class StayingEmptyWaiter extends Waiter<String, Void> {

        long emptyTime;

        public StayingEmptyWaiter(long emptyTime) {
            this.emptyTime = emptyTime;
        }

        @Override
        public String actionProduced(Void obj) {
            try {
                EventWaiter eventWaiter = new EventWaiter(emptyTime);
                EventQueue.invokeAndWait(eventWaiter);
                if (eventWaiter.empty
                        && timeFromStart() <= super.getTimeouts().getTimeout("Waiter.WaitingTime")) {
                    return "Reached";
                }
            } catch (InterruptedException | InvocationTargetException e) {
                output.printStackTrace(e);
            }
            return null;
        }

        @Override
        public String getDescription() {
            return "Wait event queue staying empty for " + emptyTime;
        }

        @Override
        public String toString() {
            return "StayingEmptyWaiter{" + "emptyTime=" + emptyTime + '}';
        }
    }

    private class ActionRunnable<R, P> extends QueueAction<R> {

        Action<R, P> action;
        P param;

        public ActionRunnable(Action<R, P> action, P param) {
            super(action.getDescription());
            this.action = action;
            this.param = param;
        }

        @Override
        public R launch() throws Exception {
            return action.launch(param);
        }
    }

    private class RunnableRunnable extends QueueAction<Void> {

        Runnable action;

        public RunnableRunnable(Runnable action) {
            super("Runnable");
            this.action = action;
        }

        @Override
        public Void launch() throws Exception {
            action.run();
            return null;
        }
    }

    private class Locker extends QueueAction<Void> {

        volatile boolean locked = false;
        long wholeTime, deltaTime;
        boolean expired;

        public Locker() {
            super("Event queue locking");
        }

        @Override
        public Void launch() {
            wholeTime = timeouts.getTimeout("QueueTool.MaximumLockingTime");
            deltaTime = timeouts.getTimeout("QueueTool.QueueCheckingDelta");
            setLocked(true);
            expired = false;
            long startTime = System.currentTimeMillis();
            while (isLocked()) {
                try {
                    Thread.sleep(deltaTime);
                } catch (InterruptedException e) {
                    getOutput().printStackTrace(e);
                }
                if (System.currentTimeMillis() - startTime > wholeTime) {
                    getOutput().printLine("Locking has been expired!");
                    expired = true;
                    break;
                }
            }
            return null;
        }

        public void setLocked(boolean locked) {
            this.locked = locked;
        }

        public boolean isLocked() {
            return locked;
        }
    }

    private class UnlockPostponer implements Runnable {

        long time;

        public UnlockPostponer(long time) {
            this.time = time;
        }

        @Override
        public void run() {
            new Timeout("", time).sleep();
            unlock();
        }
    }
}
