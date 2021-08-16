/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.net.http.common;

import org.testng.annotations.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class DemandTest {

    @Test
    public void test01() {
        assertTrue(new Demand().isFulfilled());
    }

    @Test
    public void test011() {
        Demand d = new Demand();
        d.increase(3);
        d.decreaseAndGet(3);
        assertTrue(d.isFulfilled());
    }

    @Test
    public void test02() {
        Demand d = new Demand();
        d.increase(1);
        assertFalse(d.isFulfilled());
    }

    @Test
    public void test03() {
        Demand d = new Demand();
        d.increase(3);
        assertEquals(d.decreaseAndGet(3), 3);
    }

    @Test
    public void test04() {
        Demand d = new Demand();
        d.increase(3);
        assertEquals(d.decreaseAndGet(5), 3);
    }

    @Test
    public void test05() {
        Demand d = new Demand();
        d.increase(7);
        assertEquals(d.decreaseAndGet(4), 4);
    }

    @Test
    public void test06() {
        Demand d = new Demand();
        assertEquals(d.decreaseAndGet(3), 0);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test07() {
        Demand d = new Demand();
        d.increase(0);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test08() {
        Demand d = new Demand();
        d.increase(-1);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test09() {
        Demand d = new Demand();
        d.increase(10);
        d.decreaseAndGet(0);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test10() {
        Demand d = new Demand();
        d.increase(13);
        d.decreaseAndGet(-3);
    }

    @Test
    public void test11() {
        Demand d = new Demand();
        d.increase(1);
        assertTrue(d.tryDecrement());
    }

    @Test
    public void test12() {
        Demand d = new Demand();
        d.increase(2);
        assertTrue(d.tryDecrement());
    }

    @Test
    public void test14() {
        Demand d = new Demand();
        assertFalse(d.tryDecrement());
    }

    @Test
    public void test141() {
        Demand d = new Demand();
        d.increase(Long.MAX_VALUE);
        assertFalse(d.isFulfilled());
    }

    @Test
    public void test142() {
        Demand d = new Demand();
        d.increase(Long.MAX_VALUE);
        d.increase(1);
        assertFalse(d.isFulfilled());
    }

    @Test
    public void test143() {
        Demand d = new Demand();
        d.increase(Long.MAX_VALUE);
        d.increase(1);
        assertFalse(d.isFulfilled());
    }

    @Test
    public void test144() {
        Demand d = new Demand();
        d.increase(Long.MAX_VALUE);
        d.increase(Long.MAX_VALUE);
        d.decreaseAndGet(3);
        d.decreaseAndGet(5);
        assertFalse(d.isFulfilled());
    }

    @Test
    public void test145() {
        Demand d = new Demand();
        d.increase(Long.MAX_VALUE);
        d.decreaseAndGet(Long.MAX_VALUE);
        assertTrue(d.isFulfilled());
    }

    @Test(invocationCount = 32)
    public void test15() throws InterruptedException {
        int N = Math.max(2, Runtime.getRuntime().availableProcessors() + 1);
        int M = ((N + 1) * N) / 2; // 1 + 2 + 3 + ... N
        Demand d = new Demand();
        d.increase(M);
        CyclicBarrier start = new CyclicBarrier(N);
        CountDownLatch stop = new CountDownLatch(N);
        AtomicReference<Throwable> error = new AtomicReference<>();
        for (int i = 0; i < N; i++) {
            int j = i + 1;
            new Thread(() -> {
                try {
                    start.await();
                } catch (Exception e) {
                    error.compareAndSet(null, e);
                }
                try {
                    assertEquals(d.decreaseAndGet(j), j);
                } catch (Throwable t) {
                    error.compareAndSet(null, t);
                } finally {
                    stop.countDown();
                }
            }).start();
        }
        stop.await();
        assertTrue(d.isFulfilled());
        assertEquals(error.get(), null);
    }
}
