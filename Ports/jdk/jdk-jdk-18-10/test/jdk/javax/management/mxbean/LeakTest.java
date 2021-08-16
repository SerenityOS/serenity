/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 6482247
 * @summary Test that creating MXBeans does not introduce memory leaks.
 * @requires vm.opt.final.ClassUnloading
 * @author Eamonn McManus
 *
 * @run build LeakTest RandomMXBeanTest MerlinMXBean TigerMXBean
 * @run main LeakTest
 */

/* In this test we create a ClassLoader, then use it to load and run another
 * jtreg test.  When the other test has completed, we wait for the ClassLoader
 * to be garbage-collected.  If it has not been gc'd after a reasonable
 * amount of time, then something is keeping a reference to the ClassLoader,
 * which implies a memory leak.
 *
 * This test can be applied to any jtreg test, not just the MXBean tests.
 */

import java.io.File;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;

public class LeakTest {
    /* Ideally we would include MXBeanTest in the list of tests, since it
     * has fairly complete coverage.  However, the ClassLoader fails to be
     * gc'd when we do that, and I am unable to figure out why.  Examining
     * a heap dump shows only weak references to the ClassLoader.  I suspect
     * something is wrong in the internals of the reflection classes, used
     * quite heavily by MXBeanTest.
     */
//    private static Class<?>[] otherTests = {MXBeanTest.class};

    private static Class<?>[] otherTests = {RandomMXBeanTest.class};

    // This class just makes it easier for us to spot our loader in heap dumps
    private static class ShadowClassLoader extends URLClassLoader {
        ShadowClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Testing that no references are held to ClassLoaders " +
                "by caches in the MXBean infrastructure");
        for (Class<?> testClass : otherTests)
            test(testClass);
        if (failure != null)
            throw new Exception("CLASSLOADER LEAK TEST FAILED: " + failure);
        System.out.println("CLASSLOADER LEAK TEST PASSED");
        if (args.length > 0) {
            System.out.println("Waiting for input");
            System.in.read();
        }
    }

    private static void test(Class<?> originalTestClass) throws Exception {
        System.out.println();
        System.out.println("TESTING " + originalTestClass.getName());
        WeakReference<ClassLoader> wr = testShadow(originalTestClass);
        System.out.println("Test passed, waiting for ClassLoader to disappear");
        long deadline = System.currentTimeMillis() + 20*1000;
        Reference<? extends ClassLoader> ref;
        while (wr.get() != null && System.currentTimeMillis() < deadline) {
            System.gc();
            Thread.sleep(100);
        }
        if (wr.get() != null)
            fail(originalTestClass.getName() + " kept ClassLoader reference");
    }

    private static WeakReference<ClassLoader>
            testShadow(Class<?> originalTestClass) throws Exception {
        String[] cpaths = System.getProperty("test.classes", ".")
                                .split(File.pathSeparator);
        URL[] urls = new URL[cpaths.length];
        for (int i=0; i < cpaths.length; i++) {
            urls[i] = Paths.get(cpaths[i]).toUri().toURL();
        }

        URLClassLoader shadowLoader =
                new ShadowClassLoader(urls, originalTestClass.getClassLoader().getParent());
        System.out.println("Shadow loader is " + shadowLoader);
        String className = originalTestClass.getName();
        Class<?> testClass = Class.forName(className, false, shadowLoader);
        if (testClass.getClassLoader() != shadowLoader) {
            throw new IllegalArgumentException("Loader didn't work: " +
                    testClass.getClassLoader() + " != " + shadowLoader);
        }
        Method main = testClass.getMethod("main", String[].class);
        main.invoke(null, (Object) new String[0]);
        return new WeakReference<ClassLoader>(shadowLoader);
    }

    private static void fail(String why) {
        System.out.println("FAILED: " + why);
        failure = why;
    }

    private static String failure;
}
