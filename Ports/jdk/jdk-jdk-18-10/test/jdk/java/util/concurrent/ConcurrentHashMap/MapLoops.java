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
 * @bug 4486658
 * @summary Exercise multithreaded maps, by default ConcurrentHashMap.
 * Multithreaded hash table test.  Each thread does a random walk
 * though elements of "key" array. On each iteration, it checks if
 * table includes key.  If absent, with probability pinsert it
 * inserts it, and if present, with probability premove it removes
 * it.  (pinsert and premove are expressed as percentages to simplify
 * parsing from command line.)
 * @library /test/lib
 * @run main/timeout=1600 MapLoops
 */

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import jdk.test.lib.Utils;

public class MapLoops {
    static final long LONG_DELAY_MS = Utils.adjustTimeout(10_000);
    static int nkeys       = 1000; // 10_000
    static int pinsert     = 60;
    static int premove     = 2;
    static int maxThreads  = 100;
    static int nops        = 10000; // 100_000
    static int removesPerMaxRandom;
    static int insertsPerMaxRandom;

    static final ExecutorService pool = Executors.newCachedThreadPool();

    static final List<Throwable> throwables = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws Exception {

        Class mapClass = null;
        if (args.length > 0) {
            try {
                mapClass = Class.forName(args[0]);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Class " + args[0] + " not found.");
            }
        }
        else
            mapClass = java.util.concurrent.ConcurrentHashMap.class;

        if (args.length > 1)
            maxThreads = Integer.parseInt(args[1]);

        if (args.length > 2)
            nkeys = Integer.parseInt(args[2]);

        if (args.length > 3)
            pinsert = Integer.parseInt(args[3]);

        if (args.length > 4)
            premove = Integer.parseInt(args[4]);

        if (args.length > 5)
            nops = Integer.parseInt(args[5]);

        // normalize probabilities wrt random number generator
        removesPerMaxRandom = (int)((double)premove/100.0 * 0x7FFFFFFFL);
        insertsPerMaxRandom = (int)((double)pinsert/100.0 * 0x7FFFFFFFL);

        System.out.print("Class: " + mapClass.getName());
        System.out.print(" threads: " + maxThreads);
        System.out.print(" size: " + nkeys);
        System.out.print(" ins: " + pinsert);
        System.out.print(" rem: " + premove);
        System.out.print(" ops: " + nops);
        System.out.println();

        int k = 1;
        int warmups = 2;
        for (int i = 1; i <= maxThreads;) {
            test(i, nkeys, mapClass);
            if (warmups > 0)
                --warmups;
            else if (i == k) {
                k = i << 1;
                i = i + (i >>> 1);
            }
            else if (i == 1 && k == 2) {
                i = k;
                warmups = 1;
            }
            else
                i = k;
        }
        pool.shutdown();
        if (! pool.awaitTermination(LONG_DELAY_MS, MILLISECONDS))
            throw new Error();

        if (! throwables.isEmpty())
            throw new Error
                (throwables.size() + " thread(s) terminated abruptly.");
    }

    static Integer[] makeKeys(int n) {
        SplittableRandom rnd = new SplittableRandom();
        Integer[] key = new Integer[n];
        for (int i = 0; i < key.length; ++i)
            key[i] = new Integer(rnd.nextInt());
        return key;
    }

    static void shuffleKeys(Integer[] key) {
        SplittableRandom rnd = new SplittableRandom();
        for (int i = key.length; i > 1; --i) {
            int j = rnd.nextInt(i);
            Integer tmp = key[j];
            key[j] = key[i-1];
            key[i-1] = tmp;
        }
    }

    static void test(int i, int nkeys, Class mapClass) throws Exception {
        System.out.print("Threads: " + i + "\t:");
        Map<Integer, Integer> map = (Map<Integer, Integer>)
            mapClass.getDeclaredConstructor().newInstance();
        Integer[] key = makeKeys(nkeys);
        // Uncomment to start with a non-empty table
        //        for (int j = 0; j < nkeys; j += 4) // start 1/4 occupied
        //            map.put(key[j], key[j]);
        LoopHelpers.BarrierTimer timer = new LoopHelpers.BarrierTimer();
        CyclicBarrier barrier = new CyclicBarrier(i+1, timer);
        SplittableRandom rnd = new SplittableRandom();
        for (int t = 0; t < i; ++t)
            pool.execute(new Runner(map, key, barrier, rnd.split()));
        barrier.await();
        barrier.await();
        long time = timer.getTime();
        long tpo = time / (i * (long)nops);
        System.out.print(LoopHelpers.rightJustify(tpo) + " ns per op");
        double secs = (double)time / 1000000000.0;
        System.out.println("\t " + secs + "s run time");
        map.clear();
    }

    static class Runner implements Runnable {
        final Map<Integer,Integer> map;
        final Integer[] key;
        final CyclicBarrier barrier;
        final SplittableRandom rnd;
        int position;
        int total;

        Runner(Map<Integer,Integer> map,
               Integer[] key,
               CyclicBarrier barrier,
               SplittableRandom rnd) {
            this.map = map;
            this.key = key;
            this.barrier = barrier;
            this.rnd = rnd;
            position = key.length / 2;
        }

        int step() {
            // random-walk around key positions, bunching accesses
            int r = rnd.nextInt(Integer.MAX_VALUE);
            position += (r & 7) - 3;
            while (position >= key.length) position -= key.length;
            while (position < 0) position += key.length;

            Integer k = key[position];
            Integer x = map.get(k);

            if (x != null) {
                if (x.intValue() != k.intValue())
                    throw new Error("bad mapping: " + x + " to " + k);

                if (r < removesPerMaxRandom) {
                    if (map.remove(k) != null) {
                        position = total % key.length; // move from position
                        return 2;
                    }
                }
            }
            else if (r < insertsPerMaxRandom) {
                ++position;
                map.put(k, k);
                return 2;
            }

            // Uncomment to add a little computation between accesses
            //            total += LoopHelpers.compute1(k.intValue());
            total += r;
            return 1;
        }

        public void run() {
            try {
                barrier.await();
                int ops = nops;
                while (ops > 0)
                    ops -= step();
                barrier.await();
            }
            catch (Throwable throwable) {
                synchronized (System.err) {
                    System.err.println("--------------------------------");
                    throwable.printStackTrace();
                }
                throwables.add(throwable);
            }
        }
    }
}
