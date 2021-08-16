/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * To force transactional execution abort due to memory conflict
 * one thread should access memory region from transactional region
 * while another thread should modify the same memory region.
 * Since this scenario is based on the race condition between threads
 * you should not expect some particular amount of aborts.
 */
class MemoryConflictProvoker extends AbortProvoker {
    // Following field have to be static in order to avoid escape analysis.
    @SuppressWarnings("UnsuedDeclaration")
    private static int field = 0;
    private static final int INNER_ITERATIONS = 10000;
    private final CyclicBarrier barrier;
    /**
     * This thread will access and modify memory region
     * from outside of the transaction.
     */
    private final Runnable conflictingThread;

    public MemoryConflictProvoker() {
        this(new Object());
    }

    public MemoryConflictProvoker(Object monitor) {
        super(monitor);
        barrier = new CyclicBarrier(2);
        conflictingThread = () -> {
            try {
                barrier.await();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            for (int i = 0; i < MemoryConflictProvoker.INNER_ITERATIONS; i++) {
                MemoryConflictProvoker.field++;
            }
        };
    }

    /**
     * Accesses and modifies memory region from within the transaction.
     */
    public void transactionalRegion() {
        for (int i = 0; i < MemoryConflictProvoker.INNER_ITERATIONS; i++) {
            synchronized(monitor) {
                MemoryConflictProvoker.field--;
            }
        }
    }

    @Override
    public void forceAbort() {
        try {
            Thread t = new Thread(conflictingThread);
            t.start();
            try {
                barrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }
            transactionalRegion();
            t.join();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getMethodWithLockName() {
        return this.getClass().getName() + "::transactionalRegion";
    }
}
