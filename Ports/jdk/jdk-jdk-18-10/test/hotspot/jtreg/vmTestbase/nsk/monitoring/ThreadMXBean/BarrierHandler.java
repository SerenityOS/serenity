/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.monitoring.ThreadMXBean;

import java.util.concurrent.*;

/**
 * Provide synchronization between management thread and its
 * managed threads.
 */
public class BarrierHandler {

    /**
     * CyclicBarries for internal synchronization
     */
    private CyclicBarrier monitoredThreadsBarrier, startActionBarrier,
            finishActionBarrier;

    /**
     * Creates new BarrierHandler that will match specified Threads with
     * management threads
     *
     * @param threads managed threads that are handled by BarrierHandler
     */
    public BarrierHandler(MXBeanTestThread... threads) {
        startActionBarrier = new CyclicBarrier(2);
        finishActionBarrier = new CyclicBarrier(2);
        monitoredThreadsBarrier = new CyclicBarrier(threads.length, new ActionThread());
    }

    /**
     * Waits when managed thread completes iteration and some action should
     * be performed by management thread. Should be called within managed thread
     * only.
     *
     */
    public void ready() {
        try {
            monitoredThreadsBarrier.await();
        } catch (Exception ignored) {}
    }

    /**
     * Allows managed threads to proceed to next iteration. Should be called
     * within management thread only.
     */
    public void proceed() {
        try {
            finishActionBarrier.await();
            startActionBarrier.await();
        } catch (Exception ignored) {}
    }

    /**
     * Waits until all managed threads complete first iteration. Should be
     * called within management thread only.
     */
    public void start() {
        try {
            startActionBarrier.await();
        } catch (Exception ignored) {}
    }

    /**
     * Allows managed threads to finish after last iteration. Should be called
     * within management thread only.
     */
    public void finish() {
        try {
            finishActionBarrier.await();
        } catch (Exception ignored) {}
    }

    /**
     * Action performed on main barrier. Used for synchronization between
     * management and managed threads
     */
    private class ActionThread extends Thread {
        @Override
        public void run() {
            try {
                startActionBarrier.await();
                finishActionBarrier.await();
            } catch (Exception ignored) {}
        }
    }
}
