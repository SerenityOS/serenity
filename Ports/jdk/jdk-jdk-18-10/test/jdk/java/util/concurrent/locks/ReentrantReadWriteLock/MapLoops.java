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
 */

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import jdk.test.lib.Utils;

public class MapLoops {
    static final long LONG_DELAY_MS = Utils.adjustTimeout(10_000);
    static final int NKEYS = 100000;
    static int pinsert     = 60;
    static int premove     = 2;
    static int maxThreads  = 5;
    static int nops        = 10000; // 1000000
    static int removesPerMaxRandom;
    static int insertsPerMaxRandom;

    static final ExecutorService pool = Executors.newCachedThreadPool();

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
            mapClass = RWMap.class;

        if (args.length > 1)
            maxThreads = Integer.parseInt(args[1]);

        if (args.length > 2)
            nops = Integer.parseInt(args[2]);

        if (args.length > 3)
            pinsert = Integer.parseInt(args[3]);

        if (args.length > 4)
            premove = Integer.parseInt(args[4]);

        // normalize probabilities wrt random number generator
        removesPerMaxRandom = (int)((double)premove/100.0 * 0x7FFFFFFFL);
        insertsPerMaxRandom = (int)((double)pinsert/100.0 * 0x7FFFFFFFL);

        System.out.println("Using " + mapClass.getName());

        SplittableRandom rnd = new SplittableRandom();
        Integer[] key = new Integer[NKEYS];
        for (int i = 0; i < key.length; ++i)
            key[i] = new Integer(rnd.nextInt());

        // warmup
        System.out.println("Warmup...");
        for (int k = 0; k < 2; ++k) {
            Map<Integer, Integer> map = (Map<Integer, Integer>)
                mapClass.getDeclaredConstructor().newInstance();
            LoopHelpers.BarrierTimer timer = new LoopHelpers.BarrierTimer();
            CyclicBarrier barrier = new CyclicBarrier(1, timer);
            new Runner(map, key, barrier, rnd.split()).run();
            map.clear();
        }

        for (int i = 1; i <= maxThreads; i += (i+1) >>> 1) {
            System.out.print("Threads: " + i + "\t:");
            Map<Integer, Integer> map = (Map<Integer, Integer>)
                mapClass.getDeclaredConstructor().newInstance();
            LoopHelpers.BarrierTimer timer = new LoopHelpers.BarrierTimer();
            CyclicBarrier barrier = new CyclicBarrier(i+1, timer);
            for (int k = 0; k < i; ++k)
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
        pool.shutdown();
        if (! pool.awaitTermination(LONG_DELAY_MS, MILLISECONDS))
            throw new Error();
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
                    // get away from this position
                    position = r % key.length;
                    map.remove(k);
                    return 2;
                }
                else
                    total += LoopHelpers.compute2(LoopHelpers.compute1(x.intValue()));
            }
            else {
                if (r < insertsPerMaxRandom) {
                    map.put(k, k);
                    return 2;
                }
            }
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
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
