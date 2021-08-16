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

import jdk.internal.access.JavaUtilJarAccess;
import jdk.internal.access.SharedSecrets;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.jar.JarFile;

public class MultiThreadLoad {

    private static PrintStream out = System.err;
    static String TEST_CLASS_PATH;
    private static final JavaUtilJarAccess JUJA = SharedSecrets.javaUtilJarAccess();
    private static CountDownLatch cdl = new CountDownLatch(1);

    private static <T> Set<T> setOf(Iterable<T> it) {
        Set<T> s = new HashSet<T>();
        for (T t : it)
            s.add(t);
        return s;
    }

    private static <T> void checkEquals(Set<T> s1, Set<T> s2, boolean eq) {
        if (s1.equals(s2) != eq)
            throw new RuntimeException(String.format("%b %s : %s",
                                                     eq, s1, s2));
    }

    abstract static class TestLoader {
        String name;

        TestLoader(String name) { this.name = name; }

        abstract ServiceLoader<FooService> load();
    }

    static TestLoader tcclLoader = new TestLoader("Thread context class loader") {
        ServiceLoader<FooService> load() {
            return ServiceLoader.load(FooService.class);
        }
    };

    static TestLoader systemClLoader = new TestLoader("System class loader") {
        ServiceLoader<FooService> load() {
            return ServiceLoader.load(FooService.class, ClassLoader.getSystemClassLoader());
        }
    };

    static TestLoader nullClLoader = new TestLoader("null (defer to system class loader)") {
        ServiceLoader<FooService> load() {
            return ServiceLoader.load(FooService.class, null);
        }
    };

    public static void main(String[] args) {
        // keep reference to variables for the newly launced process
        TEST_CLASS_PATH = args[0];
        for (TestLoader tl : Arrays.asList(tcclLoader, systemClLoader, nullClLoader)) {
            test(tl);
        }
    }

    static void test(TestLoader tl) {
        Runnable r1 = () -> {
            ServiceLoader<FooService> sl = tl.load();
            out.format("%s: %s%n", tl.name, sl);

            // Providers are cached
            Set<FooService> ps = setOf(sl);
            cdl.countDown();
            checkEquals(ps, setOf(sl), true);

            // The cache can be flushed and reloaded
            sl.reload();
            checkEquals(ps, setOf(sl), false);
        };

        Runnable r2 = () -> {
            jarCrawler(Path.of(TEST_CLASS_PATH));
        };
        new Thread(r2).start();
        new Thread(r1).start();

    }

    private static void jarCrawler(Path p) {
        try {
            // let the other thread spin up
            cdl.await();
        } catch (InterruptedException e) {
            // ignore
        }
        try {
            for (int i = MultiProviderTest.NUM_JARS -1; i >= 0; i--) {
                JUJA.ensureInitialization(new JarFile(TEST_CLASS_PATH + File.separator
                        + "FooProvider" + i + ".jar"));
            }
        } catch (Exception e) {
            System.out.println("Exception during jar crawl: ");
            e.printStackTrace(System.out);
        }

    }
}
