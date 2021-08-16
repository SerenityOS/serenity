/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2017 SAP SE and/or its affiliates. All rights reserved.
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

package gc.stress.TestJNIBlockFullGC;

/*
 * @test TestJNIBlockFullGC
 * @summary Check that in G1 a Full GC to reclaim space can not be blocked out by the GC locker.
 * @key randomness
 * @requires vm.gc.G1
 * @library /test/lib
 * @run main/othervm/native -Xmx64m -XX:+UseG1GC -Xlog:gc=info,gc+alloc=trace -XX:MaxGCPauseMillis=10 gc.stress.TestJNIBlockFullGC.TestJNIBlockFullGC 10 10000 10000 10000 30000 10000 0.7
 */

import java.lang.ref.SoftReference;
import java.util.Random;
import jdk.test.lib.Utils;

public class TestJNIBlockFullGC {
    private static final Random rng = Utils.getRandomInstance();

    static {
        System.loadLibrary("TestJNIBlockFullGC");
    }

    public static volatile Object tmp;

    public static volatile boolean hadError = false;

    private static native int TestCriticalArray0(int[] x);

    public static class Node {
        public SoftReference<Node> next;
        long payload1;
        long payload2;
        long payload3;
        long payload4;

        public Node(int load) {
            payload1 = payload2 = payload3 = payload4 = load;
        }
    }

    public static void warmUp(long warmupEndTime, int size, long seed) {
        Random r = new Random(seed);
        // First let the GC assume most of our objects will die.
        Node[] roots = new Node[size];

        while (System.currentTimeMillis() < warmupEndTime) {
            int index = (int) (r.nextDouble() * roots.length);
            roots[index] = new Node(1);
        }

        // Make sure the young generation is empty.
        for (int i = 0; i < roots.length; ++i) {
            roots[i] = null;
        }
    }

    public static void runTest(long endTime, int size, double alive, long seed) {
        Random r = new Random(seed);
        final int length = 10000;
        int[] array1 = new int[length];
        for (int x = 1; x < length; x++) {
            array1[x] = x;
        }

        Node[] roots = new Node[size];
        try {
            int index = 0;
            roots[0] = new Node(0);

            while (!hadError && (System.currentTimeMillis() < endTime)) {
                int test_val1 = TestCriticalArray0(array1);

                if (r.nextDouble() > alive) {
                    tmp = new Node(test_val1);
                } else {
                    index = (int) (r.nextDouble() * roots.length);

                    if (roots[index] != null) {
                        Node node = new Node(test_val1);
                        node.next = new SoftReference<Node>(roots[index]);
                        roots[index] = node;
                    } else {
                        roots[index] = new Node(test_val1);
                    }
                }
            }
        } catch (OutOfMemoryError e) {
            hadError = true;
            e.printStackTrace();
        }
    }

    private static void joinThreads(Thread[] threads) throws Exception {
        for (int i = 0; i < threads.length; i++) {
            try {
                if (threads[i] != null) {
                  threads[i].join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 7){
            System.out.println("Usage: java TestJNIBlockFullGC <warmupThreads> <warmup-time-in-millis> <warmup iterations> <threads> <time-in-millis> <iterations> <aliveFrac>");
            System.exit(0);
        }

        int warmupThreads = Integer.parseInt(args[0]);
        System.out.println("# Warmup Threads = " + warmupThreads);

        int warmupDuration = Integer.parseInt(args[1]);
        System.out.println("WarmUp Duration = " + warmupDuration);
        int warmupIterations = Integer.parseInt(args[2]);
        System.out.println("# Warmup Iterations = "+ warmupIterations);

        int mainThreads = Integer.parseInt(args[3]);
        System.out.println("# Main Threads = " + mainThreads);
        int mainDuration = Integer.parseInt(args[4]);
        System.out.println("Main Duration = " + mainDuration);
        int mainIterations = Integer.parseInt(args[5]);
        System.out.println("# Main Iterations = " + mainIterations);

        double liveFrac = Double.parseDouble(args[6]);
        System.out.println("Live Fraction = " + liveFrac);

        Thread threads[] = new Thread[Math.max(warmupThreads, mainThreads)];

        System.out.println("Start warm-up threads!");
        long warmupStartTime = System.currentTimeMillis();
        for (int i = 0; i < warmupThreads; i++) {
            long seed = rng.nextLong();
            threads[i] = new Thread() {
                public void run() {
                    warmUp(warmupStartTime + warmupDuration, warmupIterations, seed);
                };
            };
            threads[i].start();
        }

        joinThreads(threads);

        System.gc();
        System.out.println("Keep alive a lot");

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < mainThreads; i++) {
            long seed = rng.nextLong();
            threads[i] = new Thread() {
                public void run() {
                    runTest(startTime + mainDuration, mainIterations, liveFrac, seed);
                };
            };
            threads[i].start();
        }
        System.out.println("All threads started");

        joinThreads(threads);

        if (hadError) {
            throw new RuntimeException("Experienced an OoME during execution.");
        }
    }
}
