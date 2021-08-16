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
 * Written by Doug Lea and Martin Buchholz with assistance from
 * members of JCP JSR-166 Expert Group and released to the public
 * domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import junit.framework.Test;
import junit.framework.TestSuite;

public class ForkJoinPool9Test extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }

    public static Test suite() {
        return new TestSuite(ForkJoinPool9Test.class);
    }

    /**
     * Check handling of common pool thread context class loader
     */
    public void testCommonPoolThreadContextClassLoader() throws Throwable {
        if (!testImplementationDetails) return;

        // Ensure common pool has at least one real thread
        String prop = System.getProperty(
            "java.util.concurrent.ForkJoinPool.common.parallelism");
        if ("0".equals(prop)) return;

        VarHandle CCL =
            MethodHandles.privateLookupIn(Thread.class, MethodHandles.lookup())
            .findVarHandle(Thread.class, "contextClassLoader", ClassLoader.class);
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        boolean haveSecurityManager = (System.getSecurityManager() != null);
        CountDownLatch runInCommonPoolStarted = new CountDownLatch(1);
        ClassLoader classLoaderDistinctFromSystemClassLoader
            = ClassLoader.getPlatformClassLoader();
        assertNotSame(classLoaderDistinctFromSystemClassLoader,
                      systemClassLoader);
        Runnable runInCommonPool = () -> {
            runInCommonPoolStarted.countDown();
            assertTrue(ForkJoinTask.inForkJoinPool());
            assertSame(ForkJoinPool.commonPool(), ForkJoinTask.getPool());
            Thread currentThread = Thread.currentThread();

            Stream.of(systemClassLoader, null).forEach(cl -> {
                if (randomBoolean())
                    // should always be permitted, without effect
                    currentThread.setContextClassLoader(cl);
                });

            Stream.of(currentThread.getContextClassLoader(),
                      (ClassLoader) CCL.get(currentThread))
            .forEach(cl -> assertTrue(cl == systemClassLoader || cl == null));

            if (haveSecurityManager)
                assertThrows(
                    SecurityException.class,
                    () -> System.getProperty("foo"),
                    () -> currentThread.setContextClassLoader(
                        classLoaderDistinctFromSystemClassLoader));
            // TODO ?
//          if (haveSecurityManager
//              && Thread.currentThread().getClass().getSimpleName()
//                 .equals("InnocuousForkJoinWorkerThread"))
//              assertThrows(SecurityException.class, /* ?? */);
        };
        Future<?> f = ForkJoinPool.commonPool().submit(runInCommonPool);
        // Ensure runInCommonPool is truly running in the common pool,
        // by giving this thread no opportunity to "help" on get().
        await(runInCommonPoolStarted);
        assertNull(f.get());
    }

}
