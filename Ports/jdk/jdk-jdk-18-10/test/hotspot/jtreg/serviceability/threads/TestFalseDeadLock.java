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

import jdk.test.lib.Utils;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Random;

/*
 * @test
 * @key randomness
 * @bug 8016304
 * @summary Make sure no deadlock is reported for this program which has no deadlocks.
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @run main/othervm TestFalseDeadLock
 */

/*
 * This test will not provoke the bug every time it is run since the bug is intermittent.
 * The test has a fixed running time of 5 seconds.
 */

public class TestFalseDeadLock {
    private static ThreadMXBean bean;
    private static volatile boolean running = true;
    private static volatile boolean found = false;

    public static void main(String[] args) throws Exception {
        bean = ManagementFactory.getThreadMXBean();
        Thread[] threads = new Thread[500];
        Random random = Utils.getRandomInstance();
        for (int i = 0; i < threads.length; i++) {
            Test t = new Test(random.nextLong());
            threads[i] = new Thread(t);
            threads[i].start();
        }
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
        }
        running = false;
        for (Thread t : threads) {
            t.join();
        }
        if (found) {
            throw new Exception("Deadlock reported, but there is no deadlock.");
        }
    }

    public static class Test implements Runnable {
        private final long seed;
        public Test(long seed) {
            this.seed = seed;
        }
        public void run() {
            Random r = new Random(seed);
            while (running) {
                try {
                    synchronized (this) {
                        wait(r.nextInt(1000) + 1);
                    }
                } catch (InterruptedException ex) {
                }
                recurse(2000);
            }
            if (bean.findDeadlockedThreads() != null) {
                System.out.println("FOUND!");
                found = true;
            }
        }

        private void recurse(int i) {
            if (!running) {
                // It is important for the test to call println here
                // since there are locks inside that path.
                System.out.println("Hullo");
            }
            else if (i > 0) {
                recurse(i - 1);
            }
        }
    }
}
