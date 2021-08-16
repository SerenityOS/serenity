/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

package compiler.testlibrary.rtm;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * Test case for busy lock scenario.
 * One thread enters the monitor and sleep for a while.
 * Another thread is blocked on the same monitor.
 */
public class BusyLock implements CompilableTest, Runnable {
    private static final int DEFAULT_TIMEOUT = 1000;
    private final CyclicBarrier barrier;

    // Following field have to be static in order to avoid escape analysis.
    @SuppressWarnings("UnsuedDeclaration")
    private static int field = 0;
    protected final Object monitor;
    protected final int timeout;

    public BusyLock() {
        this(BusyLock.DEFAULT_TIMEOUT);
    }

    public BusyLock(int timeout) {
        this.timeout = timeout;
        this.monitor = new Object();
        this.barrier = new CyclicBarrier(2);
    }

    @Override
    public void run() {
        try {
            synchronized (monitor) {
                barrier.await();
                Thread.sleep(timeout);
            }
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException("Synchronization error happened.", e);
        }
    }

    public void syncAndTest() {
        try {
            // wait until monitor is locked by a ::run method
            barrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException("Synchronization error happened.", e);
        }
        test();
    }

    public void test() {
        synchronized(monitor) {
            BusyLock.field++;
        }
    }

    @Override
    public String getMethodWithLockName() {
        return this.getClass().getName() + "::test";
    }

    @Override
    public String[] getMethodsToCompileNames() {
        return new String[] { getMethodWithLockName() };
    }

    /**
     * Usage:
     * BusyLock [ &lt;inflate monitor&gt; [ &lt;timeout&gt; ] ]
     *
     * Default values are:
     * <ul>
     *     <li>inflate monitor = {@code true}</li>
     *     <li>timeout = {@code BusyLock.DEFAULT_TIMEOUT}</li>
     * </ul>
     */
    public static void main(String args[]) throws Exception {
        int timeoutValue = BusyLock.DEFAULT_TIMEOUT;
        boolean inflateMonitor = true;

        if (args.length > 0 ) {
            inflateMonitor = Boolean.valueOf(args[0]);

            if (args.length > 1) {
                timeoutValue = Integer.valueOf(args[1]);
            }
        }

        BusyLock busyLock = new BusyLock(timeoutValue);

        if (inflateMonitor) {
            AbortProvoker.inflateMonitor(busyLock.monitor);
        }

        Thread t = new Thread(busyLock);
        t.start();
        busyLock.syncAndTest();
        t.join();
    }
}
