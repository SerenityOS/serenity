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
package nsk.share.locks;

import java.util.*;
import nsk.share.TestBug;
import nsk.share.Wicket;

/*
 * Class used to create deadlocked threads. It is possible create 2 or more deadlocked thread, also
 * is is possible to specify resource of which type should lock each deadlocked thread
 */
public class DeadlockMaker {
    // create deadlock with 2 threads
    // lockType1 and lockType2 - type of locking resources used for deadlock creation
    public static DeadlockedThread[] createDeadlockedThreads(LockType lockType1, LockType lockType2) {
        DeadlockedThread[] resultThreads = new DeadlockedThread[2];

        Wicket step1 = new Wicket();
        Wicket step2 = new Wicket();

        Wicket readyWicket = new Wicket(2);

        DeadlockLocker locker1 = createLocker(lockType1, step1, step2, readyWicket);
        DeadlockLocker locker2 = createLocker(lockType2, step2, step1, readyWicket);
        locker1.setInner(locker2);
        locker2.setInner(locker1);

        resultThreads[0] = new DeadlockedThread(locker1);
        resultThreads[1] = new DeadlockedThread(locker2);

        resultThreads[0].start();
        resultThreads[1].start();

        readyWicket.waitFor();

        // additional check to be sure that all threads really blocked
        waitForDeadlock(resultThreads);

        return resultThreads;
    }

    // create deadlock with several threads
    // locksTypes - type of locking resources used for deadlock creation
    public static DeadlockedThread[] createDeadlockedThreads(List<LockType> locksTypes) {
        if (locksTypes.size() < 2) {
            throw new IllegalArgumentException("Need at least 2 threads for deadlock");
        }

        int threadsNumber = locksTypes.size();

        DeadlockedThread[] resultThreads = new DeadlockedThread[threadsNumber];

        Wicket readyWicket = new Wicket(threadsNumber);

        DeadlockLocker deadlockLockers[] = new DeadlockLocker[threadsNumber];
        Wicket stepWickets[] = new Wicket[threadsNumber];

        for (int i = 0; i < threadsNumber; i++)
            stepWickets[i] = new Wicket();

        int index1 = 0;
        int index2 = 1;
        for (int i = 0; i < threadsNumber; i++) {
            Wicket step1 = stepWickets[index1];
            Wicket step2 = stepWickets[index2];

            deadlockLockers[i] = createLocker(locksTypes.get(i), step1, step2, readyWicket);

            if (i > 0)
                deadlockLockers[i - 1].setInner(deadlockLockers[i]);

            index1 = (index1 + 1) % threadsNumber;
            index2 = (index2 + 1) % threadsNumber;
        }
        deadlockLockers[threadsNumber - 1].setInner(deadlockLockers[0]);

        for (int i = 0; i < threadsNumber; i++) {
            resultThreads[i] = new DeadlockedThread(deadlockLockers[i]);
            resultThreads[i].start();
        }

        readyWicket.waitFor();

        // additional check to be sure that all threads really blocked
        waitForDeadlock(resultThreads);

        return resultThreads;
    }

    /*
     * Wait when thread state will change to be sure that deadlock is really created
     */
    static private void waitForDeadlock(DeadlockedThread[] threads) {
        Set<Thread.State> targetStates = new HashSet<Thread.State>();

        // thread is waiting for a monitor lock to enter a synchronized block/method
        targetStates.add(Thread.State.BLOCKED);

        // thread calls LockSupport.park
        targetStates.add(Thread.State.WAITING);

        // thread calls LockSupport.parkNanos or LockSupport.parkUntil
        targetStates.add(Thread.State.TIMED_WAITING);

        for (Thread thread : threads) {
            while (!targetStates.contains(thread.getState())) {
                sleep(100);
            }
        }
    }

    static private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            System.out.println("Unexpected exception: " + e);
            e.printStackTrace(System.out);

            TestBug testBugException = new TestBug("Unexpected exception was throw: " + e);
            testBugException.initCause(e);
            throw testBugException;
        }
    }

    // create locker with given type
    public static DeadlockLocker createLocker(LockType type, Wicket step1, Wicket step2, Wicket readyWicket) {
        switch (type) {
        case SYNCHRONIZED_METHOD:
            return new SynchronizedMethodLocker(step1, step2, readyWicket);
        case SYNCHRONIZED_BLOCK:
            return new SynchronizedBlockLocker(step1, step2, readyWicket);
        case REENTRANT_LOCK:
            return new ReentrantLockLocker(step1, step2, readyWicket);
        case JNI_LOCK:
            return new JNIMonitorLocker(step1, step2, readyWicket);
        }

        throw new IllegalArgumentException("Unsupported lock type: " + type);
    }
}
