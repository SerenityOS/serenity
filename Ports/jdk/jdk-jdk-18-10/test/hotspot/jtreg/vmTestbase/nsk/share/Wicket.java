/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Wicket provides a means for one or more threads to suspend execution
 * (to wait) until notified by one or more other threads that some set
 * of locks is now open.
 *
 * <p>Wicket instances are intended to be used generally in the following
 * scenarios:
 *
 *   <ul><li>One thread starts one or more child threads and waits until the
 *   child threads to be started.
 *
 *   <li>One thread starts one or more child threads and waits until at least
 *   one of the child threads to be started.
 *
 *   <li>One or more child threads wait until a main thread lets them
 *   to finish.
 *
 *   <li>Disable the current thread for thread scheduling purposes, for up to
 *   the specified waiting time.</ul>
 */

public class Wicket {

    /** Number of closed locks, can be greater or equal to zero */
    private int count;

    /** Number of waiters **/
    private int waiters = 0;

    /** Enable debug output */
    private PrintStream debugOutput = null;

    /** Wicket's string identifier */
    private String name = "";

    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    /**
     * Construct a Wicket with only one closed lock.
     */
    public Wicket() {
        this(1);
    }

    /**
     * Construct a Wicket with the given number of closed locks.
     *
     * @param _name Wicket's identifier
     * @param _count the initial number of closed locks
     * @param _debugOutput whether to print debug info or not
     * @throws IllegalArgumentException if count is less than 1
     */
    public Wicket(String _name, int _count, PrintStream _debugOutput) {
        this(_count);
        name = _name;
        debugOutput = _debugOutput;
    }

    /**
     * Construct a Wicket with the given number of closed locks.
     *
     * @param count the initial number of closed locks
     * @throws IllegalArgumentException if count is less than 1
     */
    public Wicket(int count) {
        if (count < 1)
            throw new IllegalArgumentException(
                "count is less than one: " + count);
        this.count = count;
    }

    /**
     * Wait for all locks of this Wicket to be open.
     *
     * <p>If all locks are already open then returns immediately.
     *
     * <p>If at least one lock is still closed then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until all
     * the locks will be open by some other threads. One lock can be open
     * by invoking the unlock method for this Wicket.
     *
     * <p>Please note, that the method would ignore Thread.interrupt() requests.
     */
    public void waitFor() {
        long id = System.currentTimeMillis();

        lock.lock();
        try {
            ++waiters;
            if (debugOutput != null) {
                debugOutput.printf("Wicket %d %s: waitFor(). There are %d waiters totally now.\n", id, name, waiters);
            }

            while (count > 0) {
                try {
                    condition.await();
                } catch (InterruptedException e) {
                }
            }
            --waiters;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Wait for all locks of this Wicket to be open within the given
     * period of time.
     *
     * <p>If all locks are already open then returns immediately with zero.
     *
     * <p>If the time is equal to zero, the method will not
     * wait and returns a number of closed locks,
     * if all locks are open, the return value is zero.
     *
     * <p>If at least one lock is still closed then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * of the two things happens:
     *
     *   <ul><li>Some other threads invoke the unlock method for this Wicket
     *   to open all the closed locks; or
     *
     *   <li>The specified waiting time elapses.</ul>
     *
     * <p>If all locks are open then the return value is 0.
     *
     * <p>If the specified waiting time elapses and some locks are still closed
     * then the return value is equal to number of closed locks.
     *
     * <p>Please note, that the method would ignore Thread.interrupt() requests.
     *
     * @param timeout the maximum time to wait in milliseconds
     * @return the number of closed locks
     * @throws IllegalArgumentException if timeout is less than 0
     */
    public int waitFor(long timeout) {
        if (timeout < 0)
            throw new IllegalArgumentException(
                    "timeout value is negative: " + timeout);

        long id = System.currentTimeMillis();

        lock.lock();
        try {
            ++waiters;
            if (debugOutput != null) {
                debugOutput.printf("Wicket %d %s: waitFor(). There are %d waiters totally now.\n", id, name, waiters);
            }

            long waitTime = timeout;
            long startTime = System.currentTimeMillis();

            while (count > 0  && waitTime > 0) {
                try {
                    condition.await(waitTime, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                }
                waitTime = timeout - (System.currentTimeMillis() - startTime);
            }
            --waiters;
            return count;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Unlock one closed lock.
     *
     * <p>Open a lock, reducing the number of closed locks by one.
     *
     * <p>If last closed lock is opened then all of the threads waiting
     * by invoking the waitFor method for this Wicket will be released
     * and re-enabled for thread scheduling purposes.
     *
     * @throws IllegalStateException if there is no one closed lock
     */
    public void unlock() {

        lock.lock();
        try {
            if (count == 0)
                throw new IllegalStateException("locks are already open");

            --count;
            if (debugOutput != null) {
                debugOutput.printf("Wicket %s: unlock() the count is now %d\n", name, count);
            }

            if (count == 0) {
                condition.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Unlock all closed locks.
     *
     * <p>Open all closed locks, setting the number of closed locks to zero.
     *
     * <p>If any threads are waiting by invoking the waitFor method for
     * this Wicket then they will be released and re-enabled for thread
     * scheduling purposes.
     */
    public void unlockAll() {
        if (debugOutput != null) {
            debugOutput.printf("Wicket %s: unlockAll()\n", name);
        }

        lock.lock();
        try {
            count = 0;
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Return current number of waiters - threads that are currently
     * waiting using one of waitFor methods.
     *
     * @return number of waiters
     */
    public int getWaiters() {

        lock.lock();
        try {
            if (debugOutput != null) {
                debugOutput.printf("Wicket %s: getWaiters()\n", name);
            }
            return waiters;
        } finally {
            lock.unlock();
        }
    }
}
