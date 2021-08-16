/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @test
 * @bug 8028564
 * @run testng/timeout=1200 ConcurrentAssociateTest
 * @summary Test that association operations, such as put and compute,
 * place entries in the map
 * @modules java.management
 */
@Test
public class ConcurrentAssociateTest {

    /** Maximum time (in seconds) to wait for a test method to complete. */
    private static final int TIMEOUT = Integer.getInteger("timeout", 200);

    /** The number of entries for each thread to place in a map. */
    private static final int N = Integer.getInteger("n", 128);

    /** The number of iterations of the test. */
    private static final int I = Integer.getInteger("i", 64);

    /** Objects to be placed in the concurrent map. */
    static class X {
        // Limit the hash code to trigger collisions
        final int hc = ThreadLocalRandom.current().nextInt(1, 9);

        public int hashCode() { return hc; }
    }

    @Test
    public void testPut() throws Throwable {
        test("CHM.put", (m, o) -> m.put(o, o));
    }

    @Test
    public void testCompute() throws Throwable {
        test("CHM.compute", (m, o) -> m.compute(o, (k, v) -> o));
    }

    @Test
    public void testComputeIfAbsent() throws Throwable {
        test("CHM.computeIfAbsent", (m, o) -> m.computeIfAbsent(o, (k) -> o));
    }

    @Test
    public void testMerge() throws Throwable {
        test("CHM.merge", (m, o) -> m.merge(o, o, (v1, v2) -> v1));
    }

    @Test
    public void testPutAll() throws Throwable {
        test("CHM.putAll", (m, o) -> {
            Map<Object, Object> hm = new HashMap<>();
            hm.put(o, o);
            m.putAll(hm);
        });
    }

    private static void test(String desc, BiConsumer<ConcurrentMap<Object, Object>, Object> associator) throws Throwable {
        for (int i = 0; i < I; i++) {
            testOnce(desc, associator);
        }
    }

    static class AssociationFailure extends RuntimeException {
        AssociationFailure(String message) {
            super(message);
        }
    }

    private static void testOnce(String desc, BiConsumer<ConcurrentMap<Object, Object>, Object> associator) throws Throwable {
        ConcurrentHashMap<Object, Object> m = new ConcurrentHashMap<>();
        CountDownLatch s = new CountDownLatch(1);

        Supplier<Runnable> sr = () -> () -> {
            try {
                if (!s.await(TIMEOUT, TimeUnit.SECONDS)) {
                    dumpTestThreads();
                    throw new AssertionError("timed out");
                }
            }
            catch (InterruptedException e) {
            }

            for (int i = 0; i < N; i++) {
                Object o = new X();
                associator.accept(m, o);
                if (!m.containsKey(o)) {
                    throw new AssociationFailure(desc + " failed: entry does not exist");
                }
            }
        };

        // Bound concurrency to avoid degenerate performance
        int ps = Math.min(Runtime.getRuntime().availableProcessors(), 8);
        Stream<CompletableFuture> runners = IntStream.range(0, ps)
                .mapToObj(i -> sr.get())
                .map(CompletableFuture::runAsync);

        CompletableFuture all = CompletableFuture.allOf(
                runners.toArray(CompletableFuture[]::new));

        // Trigger the runners to start associating
        s.countDown();

        try {
            all.get(TIMEOUT, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            dumpTestThreads();
            throw e;
        } catch (Throwable e) {
            dumpTestThreads();
            Throwable cause = e.getCause();
            if (cause instanceof AssociationFailure) {
                throw cause;
            }
            throw e;
        }
    }

    /**
     * A debugging tool to print stack traces of most threads, as jstack does.
     * Uninteresting threads are filtered out.
     */
    static void dumpTestThreads() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        System.err.println("------ stacktrace dump start ------");
        for (ThreadInfo info : threadMXBean.dumpAllThreads(true, true)) {
            final String name = info.getThreadName();
            String lockName;
            if ("Signal Dispatcher".equals(name))
                continue;
            if ("Reference Handler".equals(name)
                && (lockName = info.getLockName()) != null
                && lockName.startsWith("java.lang.ref.Reference$Lock"))
                continue;
            if ("Finalizer".equals(name)
                && (lockName = info.getLockName()) != null
                && lockName.startsWith("java.lang.ref.ReferenceQueue$Lock"))
                continue;
            System.err.print(info);
        }
        System.err.println("------ stacktrace dump end ------");
    }
}
