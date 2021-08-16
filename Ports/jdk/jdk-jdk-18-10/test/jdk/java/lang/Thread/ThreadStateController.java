/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import jdk.test.lib.LockFreeLogger;
import jdk.test.lib.Utils;

/**
 * ThreadStateController allows a thread to request this thread to transition
 * to a specific thread state.  The {@linkplain #transitionTo request} is
 * a blocking call that the calling thread will wait until this thread is about
 * going to the new state.  Only one request of state transition at a time
 * is supported (the Phaser expects only parties of 2 to arrive and advance
 * to next phase).
 */
public class ThreadStateController extends Thread {
    // used to achieve waiting states
    private final Object lock;
    public ThreadStateController(String name, Object lock) {
        super(name);
        this.lock = lock;
    }

    public void checkThreadState(Thread.State expected) {
        // maximum number of retries when checking for thread state.
        final int MAX_RETRY = 500;

        // wait for the thread to transition to the expected state.
        // There is a small window between the thread checking the state
        // and the thread actual entering that state.
        Thread.State state;
        int retryCount=0;
        while ((state = getState()) != expected && retryCount < MAX_RETRY) {
            pause(10);
            retryCount++;
        }

        if (state == null) {
            throw new RuntimeException(getName() + " expected to have " +
                expected + " but got null.");
        }

        if (state != expected) {
            throw new RuntimeException(String.format("%s expected in %s state but got %s " +
                "(iterations %d interrupted %d)%n",
                getName(), expected, state, iterations.get(), interrupted.get()));
        }
    }

    public static void pause(long ms) {
        try {
            Thread.sleep(Utils.adjustTimeout(ms));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // Phaser to sync between the main thread putting
    // this thread into various states
    private final Phaser phaser =  new Phaser(2);
    private volatile int newState = S_RUNNABLE;
    private volatile int state = 0;
    private boolean done = false;

    private static final int S_RUNNABLE = 1;
    private static final int S_BLOCKED = 2;
    private static final int S_WAITING = 3;
    private static final int S_TIMED_WAITING = 4;
    private static final int S_PARKED = 5;
    private static final int S_TIMED_PARKED = 6;
    private static final int S_SLEEPING = 7;
    private static final int S_TERMINATE = 8;

    // for debugging
    private final AtomicInteger iterations = new AtomicInteger();
    private final AtomicInteger interrupted = new AtomicInteger();

    private final LockFreeLogger logger = new LockFreeLogger();

    @Override
    public void run() {
        // this thread has started
        while (!done) {
            // state transition
            int nextState = state;
            if (newState != state) {
                nextState = newState;
                iterations.set(0);
                interrupted.set(0);
            }
            iterations.incrementAndGet();
            switch (nextState) {
                case S_RUNNABLE: {
                    stateChange(nextState);
                    double sum = 0;
                    for (int i = 0; i < 1000; i++) {
                       double r = Math.random();
                       double x = Math.pow(3, r);
                       sum += x - r;
                    }
                    break;
                }
                case S_BLOCKED: {
                    log("%d: %s is going to block (iterations %d)%n",
                        getId(), getName(), iterations.get());
                    stateChange(nextState);
                    // going to block on lock
                    synchronized (lock) {
                        log("%d:   %s acquired the lock (iterations %d)%n",
                            getId(), getName(), iterations.get());
                        try {
                            // this thread has escaped the BLOCKED state
                            // release the lock and a short wait before continue
                            lock.wait(Utils.adjustTimeout(10));
                        } catch (InterruptedException e) {
                            // ignore
                            interrupted.incrementAndGet();
                        }
                    }
                    break;
                }
                case S_WAITING: {
                    synchronized (lock) {
                        log("%d: %s is going to waiting (iterations %d interrupted %d)%n",
                            getId(), getName(), iterations.get(), interrupted.get());
                        try {
                            stateChange(nextState);
                            lock.wait();
                            log("%d:   %s wakes up from waiting (iterations %d interrupted %d)%n",
                                getId(), getName(), iterations.get(), interrupted.get());
                        } catch (InterruptedException e) {
                            // ignore
                            interrupted.incrementAndGet();
                        }
                    }
                    break;
                }
                case S_TIMED_WAITING: {
                    synchronized (lock) {
                        log("%d: %s is going to timed waiting (iterations %d interrupted %d)%n",
                            getId(), getName(), iterations.get(), interrupted.get());
                        try {
                            stateChange(nextState);
                            lock.wait(Integer.MAX_VALUE);
                            log("%d:   %s wakes up from timed waiting (iterations %d interrupted %d)%n",
                                getId(), getName(), iterations.get(), interrupted.get());
                        } catch (InterruptedException e) {
                            // ignore
                            interrupted.incrementAndGet();
                        }
                    }
                    break;
                }
                case S_PARKED: {
                    log("%d: %s is going to park (iterations %d)%n",
                        getId(), getName(), iterations.get());
                    stateChange(nextState);
                    LockSupport.park();
                    break;
                }
                case S_TIMED_PARKED: {
                    log("%d: %s is going to timed park (iterations %d)%n",
                        getId(), getName(), iterations.get());
                    long deadline = System.currentTimeMillis() +
                                        Utils.adjustTimeout(10000*1000);
                    stateChange(nextState);
                    LockSupport.parkUntil(deadline);
                    break;
                }
                case S_SLEEPING: {
                    log("%d: %s is going to sleep (iterations %d interrupted %d)%n",
                        getId(), getName(), iterations.get(), interrupted.get());
                    try {
                        stateChange(nextState);
                        Thread.sleep(Utils.adjustTimeout(1000000));
                    } catch (InterruptedException e) {
                        // finish sleeping
                        interrupted.incrementAndGet();
                    }
                    break;
                }
                case S_TERMINATE: {
                    done = true;
                    stateChange(nextState);
                    break;
                }
                default:
                    break;
            }
        }
    }

    /**
     * Change the state if it matches newState.
     */
    private void stateChange(int nextState) {
        // no state change
        if (state == nextState)
            return;

        // transition to the new state
        if (newState == nextState) {
            state = nextState;
            phaser.arrive();
            log("%d:   state change: %s %s%n",
                getId(), toStateName(nextState), phaserToString(phaser));
            return;
        }

        // should never reach here
        throw new RuntimeException("current " + state + " next " + nextState +
                " new state " + newState);
    }

    /**
     * Blocks until this thread transitions to the given state
     */
    public void transitionTo(Thread.State tstate) throws InterruptedException {
        switch (tstate) {
            case RUNNABLE:
                nextState(S_RUNNABLE);
                break;
            case BLOCKED:
                nextState(S_BLOCKED);
                break;
            case WAITING:
                nextState(S_WAITING);
                break;
            case TIMED_WAITING:
                nextState(S_TIMED_WAITING);
                break;
            case TERMINATED:
                nextState(S_TERMINATE);
                break;
            default:
                break;
        }
    }

    /**
     * Blocks until this thread transitions to sleeping
     */
    public void transitionToSleep() throws InterruptedException {
        nextState(S_SLEEPING);
    }

    /**
     * Blocks until this thread transitions to park or timed park
     */
    public void transitionToPark(boolean timed) throws InterruptedException {
        nextState(timed ? S_TIMED_PARKED : S_PARKED);
    }

    private void nextState(int s) throws InterruptedException {
        final long id = Thread.currentThread().getId();
        log("%d: wait until the thread transitions to %s %s%n",
            id, toStateName(s), phaserToString(phaser));
        this.newState = s;
        int phase = phaser.arrive();
        log("%d:   awaiting party arrive %s %s%n",
            id, toStateName(s), phaserToString(phaser));
        for (;;) {
            // when this thread has changed its state before it waits or parks
            // on a lock, a potential race might happen if it misses the notify
            // or unpark.  Hence await for the phaser to advance with timeout
            // to cope with this race condition.
            switch (state) {
                case S_WAITING:
                case S_TIMED_WAITING:
                    synchronized (lock) {
                        lock.notify();
                    }
                    break;
                case S_PARKED:
                case S_TIMED_PARKED:
                    LockSupport.unpark(this);
                    break;
                case S_SLEEPING:
                    this.interrupt();
                    break;
                case S_BLOCKED:
                default:
                    break;
            }
            try {
                phaser.awaitAdvanceInterruptibly(phase, 100, TimeUnit.MILLISECONDS);
                log("%d:   arrived at %s %s%n",
                    id, toStateName(s), phaserToString(phaser));
                return;
            } catch (TimeoutException ex) {
                // this thread hasn't arrived at this phase
                log("%d: Timeout: %s%n", id, phaser);
            }
        }
    }

    private String phaserToString(Phaser p) {
        return "[phase = " + p.getPhase() +
               " parties = " + p.getRegisteredParties() +
               " arrived = " + p.getArrivedParties() + "]";
    }

    private String toStateName(int state) {
        switch (state) {
            case S_RUNNABLE:
                return "runnable";
            case S_WAITING:
                return "waiting";
            case S_TIMED_WAITING:
                return "timed waiting";
            case S_PARKED:
                return "parked";
            case S_TIMED_PARKED:
                return "timed parked";
            case S_SLEEPING:
                return "sleeping";
            case S_BLOCKED:
                return "blocked";
            case S_TERMINATE:
                return "terminated";
            default:
                return "unknown " + state;
        }
    }

    private void log(String msg, Object ... params) {
        logger.log(msg, params);
    }

    /**
     * Waits for the controller to complete the test run and returns the
     * generated log
     * @return The controller log
     * @throws InterruptedException
     */
    public String getLog() throws InterruptedException {
        return getLog(0);
    }

    /**
     * Waits at most {@code millis} milliseconds for the controller
     * to complete the test run and returns the generated log.
     * A timeout of {@code 0} means to wait forever.
     */
    public String getLog(long millis) throws InterruptedException {
        this.join(millis);

        return logger.toString();
    }
}
