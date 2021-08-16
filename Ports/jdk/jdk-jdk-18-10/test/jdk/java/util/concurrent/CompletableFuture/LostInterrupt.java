/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import static java.util.concurrent.TimeUnit.DAYS;

/*
 * @test
 * @bug 8254350
 * @run main LostInterrupt
 * @summary CompletableFuture.get may swallow interrupt status
 * @key randomness
 */

// TODO: Rewrite as a CompletableFuture tck test ?

/**
 * Submits a task that completes immediately, then invokes CompletableFuture.get
 * with the interrupt status set. CompletableFuture.get should either complete
 * immediately with the interrupt status set, or else throw InterruptedException
 * with the interrupt status cleared.
 */
public class LostInterrupt {
    static final int ITERATIONS = 10_000;

    public static void main(String[] args) throws Exception {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        ForkJoinPool executor = new ForkJoinPool(1);
        try {
            for (int i = 0; i < ITERATIONS; i++) {
                CompletableFuture<String> future = new CompletableFuture<>();
                boolean timed = rnd.nextBoolean();
                executor.execute(() -> future.complete("foo"));

                Thread.currentThread().interrupt();
                try {
                    String result = timed ? future.get(1, DAYS) : future.get();

                    if (!Thread.interrupted())
                        throw new AssertionError("lost interrupt, run=" + i);
                } catch (InterruptedException expected) {
                    if (Thread.interrupted())
                        throw new AssertionError(
                            "interrupt status not cleared, run=" + i);
                }
            }
        } finally {
            executor.shutdown();
        }
    }
}
