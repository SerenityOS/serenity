/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.Asserts;
import sun.hotspot.WhiteBox;

import java.util.Objects;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * Base class for different transactional execution abortion
 * provokers aimed to force abort due to specified reason.
 */
public abstract class AbortProvoker implements CompilableTest {
    public static final long DEFAULT_ITERATIONS = 10000L;
    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
    @SuppressWarnings("unused")
    private static int sharedState = 0;
    /**
     * Inflates monitor associated with object {@code monitor}.
     * Inflation is forced by entering the same monitor from
     * two different threads.
     *
     * @param monitor monitor to be inflated.
     * @return inflated monitor.
     * @throws Exception if something went wrong.
     */
    public static Object inflateMonitor(Object monitor) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);

        Runnable inflatingRunnable = () -> {
            synchronized (monitor) {
                try {
                    barrier.await();
                } catch (BrokenBarrierException  | InterruptedException e) {
                    throw new RuntimeException(
                            "Synchronization issue occurred.", e);
                }
                try {
                    monitor.wait();
                } catch (InterruptedException e) {
                    throw new AssertionError("The thread waiting on an"
                            + " inflated monitor was interrupted, thus test"
                            + " results may be incorrect.", e);
                }
            }
        };

        Thread t = new Thread(inflatingRunnable);
        t.setDaemon(true);
        t.start();
        // Wait until thread t enters the monitor.
        barrier.await();
        synchronized (monitor) {
            // At this point thread t is already waiting on the monitor.
            // Modifying static field just to avoid lock's elimination.
            sharedState++;
        }
        verifyMonitorState(monitor, true /* inflated */);
        return monitor;
    }

    /**
     * Verifies that {@code monitor} is a stack-lock or inflated lock depending
     * on {@code shouldBeInflated} value. If {@code monitor} is inflated while
     * it is expected that it should be a stack-lock, then this method attempts
     * to deflate it by forcing a safepoint and then verifies the state once
     * again.
     *
     * @param monitor monitor to be verified.
     * @param shouldBeInflated flag indicating whether or not monitor is
     *                         expected to be inflated.
     * @throws RuntimeException if the {@code monitor} in a wrong state.
     */
    public static void verifyMonitorState(Object monitor,
            boolean shouldBeInflated) {
        if (!shouldBeInflated && WHITE_BOX.isMonitorInflated(monitor)) {
            boolean did_deflation = WHITE_BOX.deflateIdleMonitors();
            Asserts.assertEQ(did_deflation, true,
                             "deflateIdleMonitors() should have worked.");
        }
        Asserts.assertEQ(WHITE_BOX.isMonitorInflated(monitor), shouldBeInflated,
                "Monitor in a wrong state.");
    }
    /**
     * Verifies that monitor used by the {@code provoker} is a stack-lock or
     * inflated lock depending on {@code shouldBeInflated} value. If such
     * monitor is inflated while it is expected that it should be a stack-lock,
     * then this method attempts to deflate it by forcing a safepoint and then
     * verifies the state once again.
     *
     * @param provoker AbortProvoker whose monitor's state should be verified.
     * @param shouldBeInflated flag indicating whether or not monitor is
     *                         expected to be inflated.
     * @throws RuntimeException if the {@code monitor} in a wrong state.
     */
    public static void verifyMonitorState(AbortProvoker provoker,
            boolean shouldBeInflated) {
        verifyMonitorState(provoker.monitor, shouldBeInflated);
    }

    /**
     * Get instance of specified AbortProvoker, inflate associated monitor
     * if needed and then invoke forceAbort method in a loop.
     *
     * Usage:
     * AbortProvoker &lt;AbortType name&gt; [&lt;inflate monitor&gt
     * [&lt;iterations&gt; [ &lt;delay&gt;]]]
     *
     *  Default parameters are:
     *  <ul>
     *  <li>inflate monitor = <b>true</b></li>
     *  <li>iterations = {@code AbortProvoker.DEFAULT_ITERATIONS}</li>
     *  <li>delay = <b>0</b></li>
     *  </ul>
     */
    public static void main(String args[]) throws Throwable {
        Asserts.assertGT(args.length, 0, "At least one argument is required.");

        AbortType abortType = AbortType.lookup(Integer.valueOf(args[0]));
        boolean monitorShouldBeInflated = true;
        long iterations = AbortProvoker.DEFAULT_ITERATIONS;

        if (args.length > 1) {
            monitorShouldBeInflated = Boolean.valueOf(args[1]);

            if (args.length > 2) {
                iterations = Long.valueOf(args[2]);

                if (args.length > 3) {
                    Thread.sleep(Integer.valueOf(args[3]));
                }
            }
        }

        AbortProvoker provoker = abortType.provoker();

        if (monitorShouldBeInflated) {
            provoker.inflateMonitor();
        }

        for (long i = 0; i < iterations; i++) {
            AbortProvoker.verifyMonitorState(provoker, monitorShouldBeInflated);
            provoker.forceAbort();
        }
    }

    protected final Object monitor;

    protected AbortProvoker() {
        this(new Object());
    }

    protected AbortProvoker(Object monitor) {
        this.monitor = Objects.requireNonNull(monitor);
    }

    /**
     * Inflates monitor used by this AbortProvoker instance.
     * @throws Exception
     */
    public void inflateMonitor() throws Exception {
        AbortProvoker.inflateMonitor(monitor);
    }

    /**
     * Forces transactional execution abortion.
     */
    public abstract void forceAbort();

    /**
     * Returns names of all methods that have to be compiled
     * in order to successfully force transactional execution
     * abortion.
     *
     * @return array with methods' names that have to be compiled.
     */
    @Override
    public String[] getMethodsToCompileNames() {
        return new String[] { getMethodWithLockName() };
    }

    /**
     * Returns name of the method that will contain monitor whose locking
     * will be elided using transactional execution.
     *
     * @return name of the method that will contain elided lock.
     */
    @Override
    public String getMethodWithLockName() {
        return this.getClass().getName() + "::forceAbort";
    }
}
