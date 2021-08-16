/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 7009231
 * @summary C1: Incorrect CAS code for longs on SPARC 32bit
 *
 * @run main/othervm -Xbatch compiler.codegen.Test7009231
 */

package compiler.codegen;

import java.util.concurrent.atomic.AtomicLong;

public class Test7009231 {
    public static void main(String[] args) throws InterruptedException {
        doTest(8);
    }

    private static void doTest(int nThreads) throws InterruptedException {
        Thread[]         aThreads = new Thread[nThreads];
        final AtomicLong atl      = new AtomicLong();

        for (int i = 0; i < nThreads; i++) {
          aThreads[i] = new RunnerThread(atl, 1L << (8 * i));
        }

        for (int i = 0; i < nThreads; i++) {
          aThreads[i].start();
        }

        for (int i = 0; i < nThreads; i++) {
          aThreads[i].join();
        }
    }

    public static class RunnerThread extends Thread {
        public RunnerThread(AtomicLong atomic, long lMask) {
            m_lMask  = lMask;
            m_atomic = atomic;
        }

        public void run() {
            AtomicLong atomic = m_atomic;
            long       lMask  = m_lMask;
            for (int i = 0; i < 100000; i++) {
                setBit(atomic, lMask);
                clearBit(atomic, lMask);
            }
        }

        protected void setBit(AtomicLong atomic, long lMask) {
            long lWord;
            do {
                lWord = atomic.get();
            } while (!atomic.compareAndSet(lWord, lWord | lMask));

            if ((atomic.get() & lMask) == 0L) {
                throw new InternalError();
            }
        }

        protected void clearBit(AtomicLong atomic, long lMask) {
            long lWord;
            do {
                lWord = atomic.get();
            } while (!atomic.compareAndSet(lWord, lWord & ~lMask));

            if ((atomic.get() & lMask) != 0L) {
                throw new InternalError();
            }
        }

        private long m_lMask;
        private AtomicLong m_atomic;
    }
}
