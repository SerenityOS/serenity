/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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

package java.awt;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import java.security.PrivilegedAction;
import java.security.AccessController;

import sun.awt.PeerEvent;

import sun.util.logging.PlatformLogger;

/**
 * This utility class is used to suspend execution on a thread
 * while still allowing {@code EventDispatchThread} to dispatch events.
 * The API methods of the class are thread-safe.
 *
 * @author Anton Tarasov, Artem Ananiev
 *
 * @since 1.7
 */
class WaitDispatchSupport implements SecondaryLoop {

    private static final PlatformLogger log =
        PlatformLogger.getLogger("java.awt.event.WaitDispatchSupport");

    private EventDispatchThread dispatchThread;
    private EventFilter filter;

    private volatile Conditional extCondition;
    private volatile Conditional condition;

    private long interval;
    // Use a shared daemon timer to serve all the WaitDispatchSupports
    private static Timer timer;
    // When this WDS expires, we cancel the timer task leaving the
    // shared timer up and running
    private TimerTask timerTask;

    private AtomicBoolean keepBlockingEDT = new AtomicBoolean(false);
    private AtomicBoolean keepBlockingCT = new AtomicBoolean(false);
    private AtomicBoolean afterExit = new AtomicBoolean(false);

    private static synchronized void initializeTimer() {
        if (timer == null) {
            timer = new Timer("AWT-WaitDispatchSupport-Timer", true);
        }
    }

    /**
     * Creates a {@code WaitDispatchSupport} instance to
     * serve the given event dispatch thread.
     *
     * @param dispatchThread An event dispatch thread that
     *        should not stop dispatching events while waiting
     *
     * @since 1.7
     */
    public WaitDispatchSupport(EventDispatchThread dispatchThread) {
        this(dispatchThread, null);
    }

    /**
     * Creates a {@code WaitDispatchSupport} instance to
     * serve the given event dispatch thread.
     *
     * @param dispatchThread An event dispatch thread that
     *        should not stop dispatching events while waiting
     * @param extCond A conditional object used to determine
     *        if the loop should be terminated
     *
     * @since 1.7
     */
    public WaitDispatchSupport(EventDispatchThread dispatchThread,
                               Conditional extCond)
    {
        if (dispatchThread == null) {
            throw new IllegalArgumentException("The dispatchThread can not be null");
        }

        this.dispatchThread = dispatchThread;
        this.extCondition = extCond;
        this.condition = new Conditional() {
            @Override
            public boolean evaluate() {
                if (log.isLoggable(PlatformLogger.Level.FINEST)) {
                    log.finest("evaluate(): blockingEDT=" + keepBlockingEDT.get() +
                               ", blockingCT=" + keepBlockingCT.get());
                }
                boolean extEvaluate =
                    (extCondition != null) ? extCondition.evaluate() : true;
                if (!keepBlockingEDT.get() || !extEvaluate || afterExit.get()) {
                    if (timerTask != null) {
                        timerTask.cancel();
                        timerTask = null;
                    }
                    return false;
                }
                return true;
            }
        };
    }

    /**
     * Creates a {@code WaitDispatchSupport} instance to
     * serve the given event dispatch thread.
     * <p>
     * The {@link EventFilter} is set on the {@code dispatchThread}
     * while waiting. The filter is removed on completion of the
     * waiting process.
     * <p>
     *
     *
     * @param dispatchThread An event dispatch thread that
     *        should not stop dispatching events while waiting
     * @param filter {@code EventFilter} to be set
     * @param interval A time interval to wait for. Note that
     *        when the waiting process takes place on EDT
     *        there is no guarantee to stop it in the given time
     *
     * @since 1.7
     */
    public WaitDispatchSupport(EventDispatchThread dispatchThread,
                               Conditional extCondition,
                               EventFilter filter, long interval)
    {
        this(dispatchThread, extCondition);
        this.filter = filter;
        if (interval < 0) {
            throw new IllegalArgumentException("The interval value must be >= 0");
        }
        this.interval = interval;
        if (interval != 0) {
            initializeTimer();
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("removal")
    @Override
    public boolean enter() {
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("enter(): blockingEDT=" + keepBlockingEDT.get() +
                     ", blockingCT=" + keepBlockingCT.get());
        }

        if (!keepBlockingEDT.compareAndSet(false, true)) {
            log.fine("The secondary loop is already running, aborting");
            return false;
        }
        try {
            if (afterExit.get()) {
                log.fine("Exit was called already, aborting");
                return false;
            }

            final Runnable run = new Runnable() {
                public void run() {
                    log.fine("Starting a new event pump");
                    if (filter == null) {
                        dispatchThread.pumpEvents(condition);
                    } else {
                        dispatchThread.pumpEventsForFilter(condition, filter);
                    }
                }
            };

            // We have two mechanisms for blocking: if we're on the
            // dispatch thread, start a new event pump; if we're
            // on any other thread, call wait() on the treelock

            Thread currentThread = Thread.currentThread();
            if (currentThread == dispatchThread) {
                if (log.isLoggable(PlatformLogger.Level.FINEST)) {
                    log.finest("On dispatch thread: " + dispatchThread);
                }
                if (interval != 0) {
                    if (log.isLoggable(PlatformLogger.Level.FINEST)) {
                        log.finest("scheduling the timer for " + interval + " ms");
                    }
                    timer.schedule(timerTask = new TimerTask() {
                        @Override
                        public void run() {
                            if (keepBlockingEDT.compareAndSet(true, false)) {
                                wakeupEDT();
                            }
                        }
                    }, interval);
                }
                // Dispose SequencedEvent we are dispatching on the current
                // AppContext, to prevent us from hang - see 4531693 for details
                SequencedEvent currentSE = KeyboardFocusManager.
                        getCurrentKeyboardFocusManager().getCurrentSequencedEvent();
                if (currentSE != null) {
                    if (log.isLoggable(PlatformLogger.Level.FINE)) {
                        log.fine("Dispose current SequencedEvent: " + currentSE);
                    }
                    currentSE.dispose();
                }
                // In case the exit() method is called before starting
                // new event pump it will post the waking event to EDT.
                // The event will be handled after the new event pump
                // starts. Thus, the enter() method will not hang.
                //
                // Event pump should be privileged. See 6300270.
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    public Void run() {
                        run.run();
                        return null;
                    }
                });
            } else {
                if (log.isLoggable(PlatformLogger.Level.FINEST)) {
                    log.finest("On non-dispatch thread: " + currentThread);
                }
                keepBlockingCT.set(true);
                synchronized (getTreeLock()) {
                    if (afterExit.get()) return false;
                    if (filter != null) {
                        dispatchThread.addEventFilter(filter);
                    }
                    try {
                        EventQueue eq = dispatchThread.getEventQueue();
                        eq.postEvent(new PeerEvent(this, run, PeerEvent.PRIORITY_EVENT));
                        if (interval > 0) {
                            long currTime = System.currentTimeMillis();
                            while (keepBlockingCT.get() &&
                                    ((extCondition != null) ? extCondition.evaluate() : true) &&
                                    (currTime + interval > System.currentTimeMillis()))
                            {
                                getTreeLock().wait(interval);
                            }
                        } else {
                            while (keepBlockingCT.get() &&
                                    ((extCondition != null) ? extCondition.evaluate() : true))
                            {
                                getTreeLock().wait();
                            }
                        }
                        if (log.isLoggable(PlatformLogger.Level.FINE)) {
                            log.fine("waitDone " + keepBlockingEDT.get() + " " + keepBlockingCT.get());
                        }
                    } catch (InterruptedException e) {
                        if (log.isLoggable(PlatformLogger.Level.FINE)) {
                            log.fine("Exception caught while waiting: " + e);
                        }
                    } finally {
                        if (filter != null) {
                            dispatchThread.removeEventFilter(filter);
                        }
                    }
                }
            }
            return true;
        }
        finally {
            keepBlockingEDT.set(false);
            keepBlockingCT.set(false);
            afterExit.set(false);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean exit() {
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("exit(): blockingEDT=" + keepBlockingEDT.get() +
                     ", blockingCT=" + keepBlockingCT.get());
        }
        afterExit.set(true);
        if (keepBlockingEDT.getAndSet(false)) {
            wakeupEDT();
            return true;
        }
        return false;
    }

    private static final Object getTreeLock() {
        return Component.LOCK;
    }

    private final Runnable wakingRunnable = new Runnable() {
        public void run() {
            log.fine("Wake up EDT");
            synchronized (getTreeLock()) {
                keepBlockingCT.set(false);
                getTreeLock().notifyAll();
            }
            log.fine("Wake up EDT done");
        }
    };

    private void wakeupEDT() {
        if (log.isLoggable(PlatformLogger.Level.FINEST)) {
            log.finest("wakeupEDT(): EDT == " + dispatchThread);
        }
        EventQueue eq = dispatchThread.getEventQueue();
        eq.postEvent(new PeerEvent(this, wakingRunnable, PeerEvent.PRIORITY_EVENT));
    }
}
