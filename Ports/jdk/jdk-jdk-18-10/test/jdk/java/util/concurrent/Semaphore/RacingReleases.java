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
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

/*
 * @test
 * @bug 6801020 6803402
 * @summary Try to tickle race conditions in
 * AbstractQueuedSynchronizer "shared" code
 */

import java.util.concurrent.Semaphore;

public class RacingReleases {

    /** Increase this for better chance of tickling races */
    static final int iterations = 1000;

    public static void test(final boolean fair,
                            final boolean interruptibly)
        throws Throwable {
        for (int i = 0; i < iterations; i++) {
            final Semaphore sem = new Semaphore(0, fair);
            final Throwable[] badness = new Throwable[1];
            Runnable blocker = interruptibly ?
                new Runnable() {
                    public void run() {
                        try {
                            sem.acquire();
                        } catch (Throwable t) {
                            badness[0] = t;
                            throw new Error(t);
                        }}}
                :
                new Runnable() {
                    public void run() {
                        try {
                            sem.acquireUninterruptibly();
                        } catch (Throwable t) {
                            badness[0] = t;
                            throw new Error(t);
                        }}};

            Thread b1 = new Thread(blocker);
            Thread b2 = new Thread(blocker);
            Runnable signaller = new Runnable() {
                public void run() {
                    try {
                        sem.release();
                    } catch (Throwable t) {
                        badness[0] = t;
                        throw new Error(t);
                    }}};
            Thread s1 = new Thread(signaller);
            Thread s2 = new Thread(signaller);
            Thread[] threads = { b1, b2, s1, s2 };
            java.util.Collections.shuffle(java.util.Arrays.asList(threads));
            for (Thread thread : threads)
                thread.start();
            for (Thread thread : threads) {
                thread.join(60 * 1000);
                if (thread.isAlive())
                    throw new Error
                        (String.format
                         ("Semaphore stuck: permits %d, thread waiting %s%n",
                          sem.availablePermits(),
                          sem.hasQueuedThreads() ? "true" : "false"));
            }
            if (badness[0] != null)
                throw new Error(badness[0]);
            if (sem.availablePermits() != 0)
              throw new Error(String.valueOf(sem.availablePermits()));
            if (sem.hasQueuedThreads())
              throw new Error(String.valueOf(sem.hasQueuedThreads()));
            if (sem.getQueueLength() != 0)
              throw new Error(String.valueOf(sem.getQueueLength()));
            if (sem.isFair() != fair)
              throw new Error(String.valueOf(sem.isFair()));
        }
    }

    public static void main(String[] args) throws Throwable {
        for (boolean fair : new boolean[] { true, false })
            for (boolean interruptibly : new boolean[] { true, false })
                test(fair, interruptibly);
    }
}
