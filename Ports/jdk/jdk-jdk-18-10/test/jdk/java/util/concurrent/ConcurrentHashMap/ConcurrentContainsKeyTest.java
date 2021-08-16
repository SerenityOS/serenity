/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @test
 * @bug 8028564
 * @run testng ConcurrentContainsKeyTest
 * @summary Test that entries are always present in the map,
 * when entries are held within one bin that is a tree
 */
@Test
public class ConcurrentContainsKeyTest {

    // The number of entries for each thread to place in a map
    // Should be > ConcurrentHashMap.TREEIFY_THRESHOLD but small
    // enough to allow for enough iteration overlap by multiple threads
    private static final int N = Integer.getInteger("n", 16);
    // The number of rounds each thread performs per entry
    private static final int R = Integer.getInteger("r", 32);
    // The number of iterations of the test
    private static final int I = Integer.getInteger("i", 256);

    // Object to be placed in the concurrent map
    static class X implements Comparable<X> {

        private final int a;

        X(int a) {
            this.a = a;
        }

        public int compareTo(X o) {
            return this.a - o.a;
        }

        public int hashCode() {
            // Return the same hash code to guarantee collisions
            return 0;
        }
    }

    @Test
    public void testContainsKey() {
        X[] content = IntStream.range(0, N).mapToObj(i -> new X(i)).toArray(X[]::new);
        // Create map with an initial size >= ConcurrentHashMap.TREEIFY_THRESHOLD
        // ensuring tree'ification will occur for a small number of entries
        // with the same hash code
        ConcurrentHashMap<Object, Object> m = new ConcurrentHashMap<>(64);
        Stream.of(content).forEach(x -> m.put(x, x));
        test(content, m);
    }

    private static void test(X[] content, ConcurrentHashMap<Object, Object> m) {
        for (int i = 0; i < I; i++) {
            testOnce(content, m);
        }
    }

    static class AssociationFailure extends RuntimeException {
        AssociationFailure(String message) {
            super(message);
        }
    }

    private static void testOnce(Object[] content, ConcurrentHashMap<Object, Object> m) {
        CountDownLatch s = new CountDownLatch(1);

        Supplier<Runnable> sr = () -> () -> {
            try {
                s.await();
            }
            catch (InterruptedException e) {
            }

            for (int i = 0; i < R * N; i++) {
                Object o = content[i % content.length];
                if (!m.containsKey(o)) {
                    throw new AssociationFailure("CHM.containsKey failed: entry does not exist");
                }
            }
        };

        int ps = Runtime.getRuntime().availableProcessors();
        Stream<CompletableFuture> runners = IntStream.range(0, ps)
                .mapToObj(i -> sr.get())
                .map(CompletableFuture::runAsync);

        CompletableFuture all = CompletableFuture.allOf(
                runners.toArray(CompletableFuture[]::new));

        // Trigger the runners to start checking key membership
        s.countDown();
        try {
            all.join();
        }
        catch (CompletionException e) {
            Throwable t = e.getCause();
            if (t instanceof AssociationFailure) {
                throw (AssociationFailure) t;
            }
            else {
                throw e;
            }
        }
    }
}
