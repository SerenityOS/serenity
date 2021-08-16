/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.loader.ClassLoaderValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @test
 * @bug 8152115
 * @summary functional and concurrency test for ClassLoaderValue
 * @modules java.base/jdk.internal.loader
 * @author Peter Levart
 */
public class ClassLoaderValueTest {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {

        ClassLoaderValue[] clvs = {new ClassLoaderValue<>(),
                                   new ClassLoaderValue<>()};

        ClassLoader[] lds = {ClassLoader.getSystemClassLoader(),
                             ClassLoader.getPlatformClassLoader(),
                             null /* bootstrap class loader */};

        Integer[] keys = new Integer[32];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = i + 128;
        }

        try (AutoCloseable cleanup = () -> {
            for (ClassLoaderValue<Integer> clv : clvs) {
                for (ClassLoader ld : lds) {
                    clv.removeAll(ld);
                }
            }
        }) {
            // 1st just one sequential pass of single-threaded validation
            // which is easier to debug if it fails...
            for (ClassLoaderValue<Integer> clv : clvs) {
                for (ClassLoader ld : lds) {
                    writeValidateOps(clv, ld, keys);
                }
            }
            for (ClassLoaderValue<Integer> clv : clvs) {
                for (ClassLoader ld : lds) {
                    readValidateOps(clv, ld, keys);
                }
            }

            // 2nd the same in concurrent setting that also validates
            // failure-isolation between threads and data-isolation between
            // regions - (ClassLoader, ClassLoaderValue) pairs - of the storage
            testConcurrentIsolation(clvs, lds, keys, TimeUnit.SECONDS.toMillis(3));
        }
    }

    static void writeValidateOps(ClassLoaderValue<Integer> clv,
                                 ClassLoader ld,
                                 Object[] keys) {
        for (int i = 0; i < keys.length; i++) {
            Object k = keys[i];
            Integer v1 = i;
            Integer v2 = i + 333;
            Integer pv;
            boolean success;

            pv = clv.sub(k).putIfAbsent(ld, v1);
            assertEquals(pv, null);
            assertEquals(clv.sub(k).get(ld), v1);

            pv = clv.sub(k).putIfAbsent(ld, v2);
            assertEquals(pv, v1);
            assertEquals(clv.sub(k).get(ld), v1);

            success = clv.sub(k).remove(ld, v2);
            assertEquals(success, false);
            assertEquals(clv.sub(k).get(ld), v1);

            success = clv.sub(k).remove(ld, v1);
            assertEquals(success, true);
            assertEquals(clv.sub(k).get(ld), null);

            pv = clv.sub(k).putIfAbsent(ld, v2);
            assertEquals(pv, null);
            assertEquals(clv.sub(k).get(ld), v2);

            pv = clv.sub(k).computeIfAbsent(ld, (_ld, _clv) -> v1);
            assertEquals(pv, v2);
            assertEquals(clv.sub(k).get(ld), v2);

            success = clv.sub(k).remove(ld, v1);
            assertEquals(success, false);
            assertEquals(clv.sub(k).get(ld), v2);

            success = clv.sub(k).remove(ld, v2);
            assertEquals(success, true);
            assertEquals(clv.sub(k).get(ld), null);

            pv = clv.sub(k).computeIfAbsent(ld, (_ld, clv_k) -> {
                try {
                    // nested get for same key should throw
                    clv_k.get(_ld);
                    throw new AssertionError("Unexpected code path");
                } catch (IllegalStateException e) {
                    // expected
                }
                try {
                    // nested putIfAbsent for same key should throw
                    clv_k.putIfAbsent(_ld, v1);
                    throw new AssertionError("Unexpected code path");
                } catch (IllegalStateException e) {
                    // expected
                }
                // nested remove for for same key and any value (even null)
                // should return false
                assertEquals(clv_k.remove(_ld, null), false);
                assertEquals(clv_k.remove(_ld, v1), false);
                assertEquals(clv_k.remove(_ld, v2), false);
                try {
                    // nested computeIfAbsent for same key should throw
                    clv_k.computeIfAbsent(_ld, (__ld, _clv_k) -> v1);
                    throw new AssertionError("Unexpected code path");
                } catch (IllegalStateException e) {
                    // expected
                }
                // if everything above has been handled, we should succeed...
                return v2;
            });
            // ... and the result should be reflected in the CLV
            assertEquals(pv, v2);
            assertEquals(clv.sub(k).get(ld), v2);

            success = clv.sub(k).remove(ld, v2);
            assertEquals(success, true);
            assertEquals(clv.sub(k).get(ld), null);

            try {
                clv.sub(k).computeIfAbsent(ld, (_ld, clv_k) -> {
                    throw new UnsupportedOperationException();
                });
                throw new AssertionError("Unexpected code path");
            } catch (UnsupportedOperationException e) {
                // expected
            }
            assertEquals(clv.sub(k).get(ld), null);
        }
    }

    static void readValidateOps(ClassLoaderValue<Integer> clv,
                                ClassLoader ld,
                                Object[] keys) {
        for (int i = 0; i < keys.length; i++) {
            Object k = keys[i];
            Integer v1 = i;
            Integer v2 = i + 333;
            Integer rv = clv.sub(k).get(ld);
            if (!(rv == null || rv.equals(v1) || rv.equals(v2))) {
                throw new AssertionError("Unexpected value: " + rv +
                                         ", expected one of: null, " + v1 + ", " + v2);
            }
        }
    }

    static void testConcurrentIsolation(ClassLoaderValue<Integer>[] clvs,
                                        ClassLoader[] lds,
                                        Object[] keys,
                                        long millisRuntime) {
        ExecutorService exe = Executors.newCachedThreadPool();
        List<Future<?>> futures = new ArrayList<>();
        AtomicBoolean stop = new AtomicBoolean();
        for (ClassLoaderValue<Integer> clv : clvs) {
            for (ClassLoader ld : lds) {
                // submit a task that exercises a mix of modifying
                // and reading-validating operations in an isolated
                // part of the storage. If isolation is violated,
                // validation operations are expected to fail.
                futures.add(exe.submit(() -> {
                    do {
                        writeValidateOps(clv, ld, keys);
                    } while (!stop.get());
                }));
                // submit a task that just reads from the same part of
                // the storage as above task. It should not disturb
                // above task in any way and this task should never
                // exhibit any failure although above task produces
                // regular failures during lazy computation
                futures.add(exe.submit(() -> {
                    do {
                        readValidateOps(clv, ld, keys);
                    } while (!stop.get());
                }));
            }
        }
        // wait for some time
        try {
            Thread.sleep(millisRuntime);
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
        // stop tasks
        stop.set(true);
        // collect results
        AssertionError error = null;
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                if (error == null) error = new AssertionError("Failure");
                error.addSuppressed(e);
            }
        }
        exe.shutdown();
        if (error != null) throw error;
    }

    static void assertEquals(Object actual, Object expected) {
        if (!Objects.equals(actual, expected)) {
            throw new AssertionError("Expected: " + expected + ", actual: " + actual);
        }
    }
}
