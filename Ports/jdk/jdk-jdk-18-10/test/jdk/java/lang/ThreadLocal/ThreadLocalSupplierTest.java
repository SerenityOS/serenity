/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * @test
 * @run testng ThreadLocalSupplierTest
 * @summary tests ThreadLocal.withInitial(<Supplier>).
 * Adapted from java.lang.Basic functional test of ThreadLocal
 *
 * @author Jim Gish <jim.gish@oracle.com>
 */
@Test
public class ThreadLocalSupplierTest {

    static final class IntegerSupplier implements Supplier<Integer> {

        private final AtomicInteger supply = new AtomicInteger(0);

        @Override
        public Integer get() {
            return supply.getAndIncrement();
        }

        public int numCalls() {
            return supply.intValue();
        }
    }

    static IntegerSupplier theSupply = new IntegerSupplier();

    static final class MyThreadLocal extends ThreadLocal<Integer> {

        private final ThreadLocal<Integer> delegate;

        public volatile boolean everCalled;

        public MyThreadLocal(Supplier<Integer> supplier) {
            delegate = ThreadLocal.<Integer>withInitial(supplier);
        }

        @Override
        public Integer get() {
            return delegate.get();
        }

        @Override
        protected synchronized Integer initialValue() {
            // this should never be called since we are using the factory instead
            everCalled = true;
            return null;
        }
    }

    /**
     * Our one and only ThreadLocal from which we get thread ids using a
     * supplier which simply increments a counter on each call of get().
     */
    static MyThreadLocal threadLocal = new MyThreadLocal(theSupply);

    public void testMultiThread() throws Exception {
        final int threadCount = 500;
        final Thread th[] = new Thread[threadCount];
        final boolean visited[] = new boolean[threadCount];

        // Create and start the threads
        for (int i = 0; i < threadCount; i++) {
            th[i] = new Thread() {
                @Override
                public void run() {
                    final int threadId = threadLocal.get();
                    assertFalse(visited[threadId], "visited[" + threadId + "]=" + visited[threadId]);
                    visited[threadId] = true;
                    // check the get() again
                    final int secondCheckThreadId = threadLocal.get();
                    assertEquals(secondCheckThreadId, threadId);
                }
            };
            th[i].start();
        }

        // Wait for the threads to finish
        for (int i = 0; i < threadCount; i++) {
            th[i].join();
        }

        assertEquals(theSupply.numCalls(), threadCount);
        // make sure the provided initialValue() has not been called
        assertFalse(threadLocal.everCalled);
        // Check results
        for (int i = 0; i < threadCount; i++) {
            assertTrue(visited[i], "visited[" + i + "]=" + visited[i]);
        }
    }

    public void testSimple() {
        final String expected = "OneWithEverything";
        final ThreadLocal<String> threadLocal = ThreadLocal.<String>withInitial(() -> expected);
        assertEquals(expected, threadLocal.get());
    }
}
