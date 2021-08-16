/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt;

import java.awt.AWTEvent;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import sun.awt.util.ThreadGroupUtils;
import sun.util.logging.PlatformLogger;

/**
 * This class is to let AWT shutdown automatically when a user is done
 * with AWT. It tracks AWT state using the following parameters:
 * <ul>
 * <li>{@code peerMap} - the map between the existing peer objects
 *     and their associated targets
 * <li>{@code toolkitThreadBusy} - whether the toolkit thread
 *     is waiting for a new native event to appear in its queue
 *     or is dispatching an event
 * <li>{@code busyThreadSet} - a set of all the event dispatch
 *     threads that are busy at this moment, i.e. those that are not
 *     waiting for a new event to appear in their event queue.
 * </ul><p>
 * AWT is considered to be in ready-to-shutdown state when
 * {@code peerMap} is empty and {@code toolkitThreadBusy}
 * is false and {@code busyThreadSet} is empty.
 * The internal AWTAutoShutdown logic secures that the single non-daemon
 * thread ({@code blockerThread}) is running when AWT is not in
 * ready-to-shutdown state. This blocker thread is to prevent AWT from
 * exiting since the toolkit thread is now daemon and all the event
 * dispatch threads are started only when needed. Once it is detected
 * that AWT is in ready-to-shutdown state this blocker thread waits
 * for a certain timeout and if AWT state doesn't change during timeout
 * this blocker thread terminates all the event dispatch threads and
 * exits.
 */
public final class AWTAutoShutdown implements Runnable {

    private static final AWTAutoShutdown theInstance = new AWTAutoShutdown();

    /**
     * This lock object is used to synchronize shutdown operations.
     */
    private final Object mainLock = new Object();

    /**
     * This lock object is to secure that when a new blocker thread is
     * started it will be the first who acquire the main lock after
     * the thread that created the new blocker released the main lock
     * by calling lock.wait() to wait for the blocker to start.
     */
    private final Object activationLock = new Object();

    /**
     * This set keeps references to all the event dispatch threads that
     * are busy at this moment, i.e. those that are not waiting for a
     * new event to appear in their event queue.
     * Access is synchronized on the main lock object.
     */
    private final Set<Thread> busyThreadSet = new HashSet<>(7);

    /**
     * Indicates whether the toolkit thread is waiting for a new native
     * event to appear or is dispatching an event.
     */
    private boolean toolkitThreadBusy = false;

    /**
     * This is a map between components and their peers.
     * we should work with in under activationLock&mainLock lock.
     */
    private final Map<Object, Object> peerMap = new IdentityHashMap<>();

    /**
     * References the alive non-daemon thread that is currently used
     * for keeping AWT from exiting.
     */
    private Thread blockerThread = null;

    /**
     * We need this flag to secure that AWT state hasn't changed while
     * we were waiting for the safety timeout to pass.
     */
    private boolean timeoutPassed = false;

    /**
     * Once we detect that AWT is ready to shutdown we wait for a certain
     * timeout to pass before stopping event dispatch threads.
     */
    private static final int SAFETY_TIMEOUT = 1000;

    /**
     * Constructor method is intentionally made private to secure
     * a single instance. Use getInstance() to reference it.
     *
     * @see     AWTAutoShutdown#getInstance
     */
    private AWTAutoShutdown() {}

    /**
     * Returns reference to a single AWTAutoShutdown instance.
     */
    public static AWTAutoShutdown getInstance() {
        return theInstance;
    }

    /**
     * Notify that the toolkit thread is not waiting for a native event
     * to appear in its queue.
     *
     * @see     AWTAutoShutdown#notifyToolkitThreadFree
     * @see     AWTAutoShutdown#setToolkitBusy
     * @see     AWTAutoShutdown#isReadyToShutdown
     */
    public static void notifyToolkitThreadBusy() {
        getInstance().setToolkitBusy(true);
    }

    /**
     * Notify that the toolkit thread is waiting for a native event
     * to appear in its queue.
     *
     * @see     AWTAutoShutdown#notifyToolkitThreadFree
     * @see     AWTAutoShutdown#setToolkitBusy
     * @see     AWTAutoShutdown#isReadyToShutdown
     */
    public static void notifyToolkitThreadFree() {
        getInstance().setToolkitBusy(false);
    }

    /**
     * Add a specified thread to the set of busy event dispatch threads.
     * If this set already contains the specified thread or the thread is null,
     * the call leaves this set unchanged and returns silently.
     *
     * @param thread thread to be added to this set, if not present.
     * @see     AWTAutoShutdown#notifyThreadFree
     * @see     AWTAutoShutdown#isReadyToShutdown
     */
    public void notifyThreadBusy(final Thread thread) {
        if (thread == null) {
            return;
        }
        synchronized (activationLock) {
            synchronized (mainLock) {
                if (blockerThread == null) {
                    activateBlockerThread();
                } else if (isReadyToShutdown()) {
                    mainLock.notifyAll();
                    timeoutPassed = false;
                }
                busyThreadSet.add(thread);
            }
        }
    }

    /**
     * Remove a specified thread from the set of busy event dispatch threads.
     * If this set doesn't contain the specified thread or the thread is null,
     * the call leaves this set unchanged and returns silently.
     *
     * @param thread thread to be removed from this set, if present.
     * @see     AWTAutoShutdown#notifyThreadBusy
     * @see     AWTAutoShutdown#isReadyToShutdown
     */
    public void notifyThreadFree(final Thread thread) {
        if (thread == null) {
            return;
        }
        synchronized (activationLock) {
            synchronized (mainLock) {
                busyThreadSet.remove(thread);
                if (isReadyToShutdown()) {
                    mainLock.notifyAll();
                    timeoutPassed = false;
                }
            }
        }
    }

    /**
     * Notify that the peermap has been updated, that means a new peer
     * has been created or some existing peer has been disposed.
     *
     * @see     AWTAutoShutdown#isReadyToShutdown
     */
    void notifyPeerMapUpdated() {
        synchronized (activationLock) {
            synchronized (mainLock) {
                if (!isReadyToShutdown() && blockerThread == null) {
                    activateBlockerThread();
                } else {
                    mainLock.notifyAll();
                    timeoutPassed = false;
                }
            }
        }
    }

    /**
     * Determine whether AWT is currently in ready-to-shutdown state.
     * AWT is considered to be in ready-to-shutdown state if
     * {@code peerMap} is empty and {@code toolkitThreadBusy}
     * is false and {@code busyThreadSet} is empty.
     *
     * @return true if AWT is in ready-to-shutdown state.
     */
    private boolean isReadyToShutdown() {
        return (!toolkitThreadBusy &&
                 peerMap.isEmpty() &&
                 busyThreadSet.isEmpty());
    }

    /**
     * Notify about the toolkit thread state change.
     *
     * @param busy true if the toolkit thread state changes from idle
     *             to busy.
     * @see     AWTAutoShutdown#notifyToolkitThreadBusy
     * @see     AWTAutoShutdown#notifyToolkitThreadFree
     * @see     AWTAutoShutdown#isReadyToShutdown
     */
    private void setToolkitBusy(final boolean busy) {
        if (busy != toolkitThreadBusy) {
            synchronized (activationLock) {
                synchronized (mainLock) {
                    if (busy != toolkitThreadBusy) {
                        if (busy) {
                            if (blockerThread == null) {
                                activateBlockerThread();
                            } else if (isReadyToShutdown()) {
                                mainLock.notifyAll();
                                timeoutPassed = false;
                            }
                            toolkitThreadBusy = busy;
                        } else {
                            toolkitThreadBusy = busy;
                            if (isReadyToShutdown()) {
                                mainLock.notifyAll();
                                timeoutPassed = false;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Implementation of the Runnable interface.
     * Incapsulates the blocker thread functionality.
     *
     * @see     AWTAutoShutdown#isReadyToShutdown
     */
    public void run() {
        Thread currentThread = Thread.currentThread();
        boolean interrupted = false;
        synchronized (mainLock) {
            try {
                /* Notify that the thread is started. */
                mainLock.notifyAll();
                while (blockerThread == currentThread) {
                    mainLock.wait();
                    timeoutPassed = false;
                    /*
                     * This loop is introduced to handle the following case:
                     * it is possible that while we are waiting for the
                     * safety timeout to pass AWT state can change to
                     * not-ready-to-shutdown and back to ready-to-shutdown.
                     * In this case we have to wait once again.
                     * NOTE: we shouldn't break into the outer loop
                     * in this case, since we may never be notified
                     * in an outer infinite wait at this point.
                     */
                    while (isReadyToShutdown()) {
                        if (timeoutPassed) {
                            timeoutPassed = false;
                            blockerThread = null;
                            break;
                        }
                        timeoutPassed = true;
                        mainLock.wait(SAFETY_TIMEOUT);
                    }
                }
            } catch (InterruptedException e) {
                interrupted = true;
            } finally {
                if (blockerThread == currentThread) {
                    blockerThread = null;
                }
            }
        }
        if (!interrupted) {
            AppContext.stopEventDispatchThreads();
        }
    }

    @SuppressWarnings("serial")
    static AWTEvent getShutdownEvent() {
        return new AWTEvent(getInstance(), 0) {
        };
    }

    /**
     * Creates and starts a new blocker thread. Doesn't return until
     * the new blocker thread starts.
     */
    @SuppressWarnings("removal")
    private void activateBlockerThread() {
        AccessController.doPrivileged((PrivilegedAction<Thread>) () -> {
            String name = "AWT-Shutdown";
            Thread thread = new Thread(
                   ThreadGroupUtils.getRootThreadGroup(), this, name, 0, false);
            thread.setContextClassLoader(null);
            thread.setDaemon(false);
            blockerThread = thread;
            return thread;
        }).start();
        try {
            /* Wait for the blocker thread to start. */
            mainLock.wait();
        } catch (InterruptedException e) {
            System.err.println("AWT blocker activation interrupted:");
            e.printStackTrace();
        }
    }

    void registerPeer(final Object target, final Object peer) {
        synchronized (activationLock) {
            synchronized (mainLock) {
                peerMap.put(target, peer);
                notifyPeerMapUpdated();
            }
        }
    }

    void unregisterPeer(final Object target, final Object peer) {
        synchronized (activationLock) {
            synchronized (mainLock) {
                if (peerMap.get(target) == peer) {
                    peerMap.remove(target);
                    notifyPeerMapUpdated();
                }
            }
        }
    }

    Object getPeer(final Object target) {
        synchronized (activationLock) {
            synchronized (mainLock) {
                return peerMap.get(target);
            }
        }
    }

    void dumpPeers(final PlatformLogger aLog) {
        if (aLog.isLoggable(PlatformLogger.Level.FINE)) {
            synchronized (activationLock) {
                synchronized (mainLock) {
                    aLog.fine("Mapped peers:");
                    for (Object key : peerMap.keySet()) {
                        aLog.fine(key + "->" + peerMap.get(key));
                    }
                }
            }
        }
    }

} // class AWTAutoShutdown
