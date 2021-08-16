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
 * @bug 4949279 6937857
 * @summary Independent instantiations of Random() have distinct seeds.
 * @key randomness
 */

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class DistinctSeeds {
    public static void main(String[] args) throws Exception {
        // Strictly speaking, it is possible for these to randomly fail,
        // but the probability should be small (approximately 2**-48).
        if (new Random().nextLong() == new Random().nextLong() ||
            new Random().nextLong() == new Random().nextLong())
            throw new RuntimeException("Random() seeds not unique.");

        // Now try generating seeds concurrently
        class RandomCollector implements Runnable {
            long[] randoms = new long[1<<17];
            public void run() {
                for (int i = 0; i < randoms.length; i++)
                    randoms[i] = new Random().nextLong();
            }
        }
        final int threadCount = 2;
        List<RandomCollector> collectors = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            RandomCollector r = new RandomCollector();
            collectors.add(r);
            threads.add(new Thread(r));
        }
        for (Thread thread : threads)
            thread.start();
        for (Thread thread : threads)
            thread.join();
        int collisions = 0;
        HashSet<Long> s = new HashSet<>();
        for (RandomCollector r : collectors) {
            for (long x : r.randoms) {
                if (s.contains(x))
                    collisions++;
                s.add(x);
            }
        }
        System.out.printf("collisions=%d%n", collisions);
        if (collisions > 10)
            throw new Error("too many collisions");
    }
}
