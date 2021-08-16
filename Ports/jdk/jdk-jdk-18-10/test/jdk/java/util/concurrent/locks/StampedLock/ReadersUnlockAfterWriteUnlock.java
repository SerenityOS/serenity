/*
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

/*
 * @test
 * @bug 8023234
 * @summary StampedLock serializes readers on writer unlock
 * @author Dmitry Chyuko
 * @author Aleksey Shipilev
 */

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.StampedLock;

public class ReadersUnlockAfterWriteUnlock {
    public static void main(String[] args) throws InterruptedException {
        final int RNUM = 2;
        final int REPS = 128;
        final StampedLock sl = new StampedLock();
        final AtomicReference<Throwable> bad = new AtomicReference<>();

        final CyclicBarrier iterationStart = new CyclicBarrier(RNUM + 1);
        final CyclicBarrier readersHaveLocks = new CyclicBarrier(RNUM);
        final CyclicBarrier writerHasLock = new CyclicBarrier(RNUM + 1);

        Runnable reader = () -> {
            try {
                for (int i = 0; i < REPS; i++) {
                    iterationStart.await();
                    writerHasLock.await();
                    long rs = sl.readLock();

                    // single reader blocks here indefinitely if readers
                    // are serialized
                    readersHaveLocks.await();

                    sl.unlockRead(rs);
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
                bad.set(ex);
            }
        };

        Thread[] threads = new Thread[RNUM];
        for (int i = 0 ; i < RNUM; i++) {
            Thread thread = new Thread(reader, "Reader");
            threads[i] = thread;
            thread.start();
        }
        for (int i = 0; i < REPS; i++) {
            try {
                iterationStart.await();
                long ws = sl.writeLock();
                writerHasLock.await();
                awaitWaitState(threads);
                sl.unlockWrite(ws);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        for (Thread thread : threads)
            thread.join();
        if (bad.get() != null)
            throw new AssertionError(bad.get());
    }

    static void awaitWaitState(Thread[] threads) {
        restart: for (;;) {
            for (Thread thread : threads) {
                if (thread.getState() != Thread.State.WAITING) {
                    Thread.yield();
                    continue restart;
                }
            }
            break;
        }
    }
}
