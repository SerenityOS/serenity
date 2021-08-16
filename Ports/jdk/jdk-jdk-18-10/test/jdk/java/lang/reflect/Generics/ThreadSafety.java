/*
 * Copyright 2014 Google Inc.  All Rights Reserved.
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

/**
 * @test
 * @bug 8062771 8016236
 * @summary Test publication of Class objects via a data race
 * @run testng ThreadSafety
 */

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 * A test resulting from an attempt to repro this failure (in guice):
 *
 * java.lang.NullPointerException
 *   at sun.reflect.generics.visitor.Reifier.visitClassTypeSignature(Reifier.java:125)
 *   at sun.reflect.generics.tree.ClassTypeSignature.accept(ClassTypeSignature.java:49)
 *   at sun.reflect.generics.repository.ClassRepository.getSuperclass(ClassRepository.java:84)
 *   at java.lang.Class.getGenericSuperclass(Class.java:692)
 *   at com.google.inject.TypeLiteral.getSuperclassTypeParameter(TypeLiteral.java:99)
 *   at com.google.inject.TypeLiteral.<init>(TypeLiteral.java:79)
 *
 * However, as one would expect with thread safety problems in reflection, these
 * are very hard to reproduce.  This very test has never been observed to fail,
 * but a similar test has been observed to fail about once in 2000 executions
 * (about once every 6 CPU-hours), in jdk7 only.  It appears to be fixed in jdk8+ by:
 *
 * 8016236: Class.getGenericInterfaces performance improvement.
 * (by making Class.genericInfo volatile)
 */
public class ThreadSafety {
    public static class EmptyClass {
        public static class EmptyGenericSuperclass<T> {}
        public static class EmptyGenericSubclass<T> extends EmptyGenericSuperclass<T> {}
    }

    /** published via data race */
    private Class<?> racyClass = Object.class;

    private Class<?> createNewEmptyGenericSubclassClass() throws Exception {
        String[] cpaths = System.getProperty("test.classes", ".")
                                .split(File.pathSeparator);
        URL[] urls = new URL[cpaths.length];
        for (int i=0; i < cpaths.length; i++) {
            urls[i] = Paths.get(cpaths[i]).toUri().toURL();
        }
        URLClassLoader ucl = new URLClassLoader(urls, null);
        return Class.forName("ThreadSafety$EmptyClass$EmptyGenericSubclass", true, ucl);
    }

    @Test
    public void testRacy_getGenericSuperclass() throws Exception {
        final int nThreads = 10;
        final int iterations = 30;
        final int timeout = 10;
        final CyclicBarrier newCycle = new CyclicBarrier(nThreads);
        final Callable<Void> task = new Callable<Void>() {
            public Void call() throws Exception {
                for (int i = 0; i < iterations; i++) {
                    final int threadId;
                    try {
                        threadId = newCycle.await(timeout, SECONDS);
                    } catch (BrokenBarrierException e) {
                        return null;
                    }
                    for (int j = 0; j < iterations; j++) {
                        // one thread publishes the class object via a data
                        // race, for the other threads to consume.
                        if (threadId == 0) {
                            racyClass = createNewEmptyGenericSubclassClass();
                        } else {
                            racyClass.getGenericSuperclass();
                        }
                    }
                }
                return null;
            }};

        final ExecutorService pool = Executors.newFixedThreadPool(nThreads);
        try {
            for (Future<Void> future :
                     pool.invokeAll(Collections.nCopies(nThreads, task))) {
                try {
                    future.get(iterations * timeout, SECONDS);
                } catch (ExecutionException e) {
                    // ignore "collateral damage"
                    if (!(e.getCause() instanceof BrokenBarrierException)
                        &&
                        !(e.getCause() instanceof TimeoutException)) {
                        throw e;
                    }
                }
            }
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(2 * timeout, SECONDS));
        }
    }
}
