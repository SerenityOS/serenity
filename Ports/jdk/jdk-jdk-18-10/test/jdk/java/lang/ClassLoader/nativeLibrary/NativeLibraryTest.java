/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 8164512 8191360
 * @requires vm.compMode != "Xcomp"
 * @comment Under musl, dlclose is a no-op. The static variable 'count' in libnative.c
 * keeps its value across a GC and the check in Test.java fails.
 * @requires !vm.musl
 * @summary verify if the native library is unloaded when the class loader is GC'ed
 * @library /test/lib/
 * @build jdk.test.lib.util.ForceGC
 * @build p.Test
 * @run main/othervm/native -Xcheck:jni NativeLibraryTest
 */

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.test.lib.util.ForceGC;

public class NativeLibraryTest {
    static final Path CLASSES = Paths.get("classes");
    static int unloadedCount = 0;

    /*
     * Called by JNI_OnUnload when the native library is unloaded
     */
    static void nativeLibraryUnloaded() {
        unloadedCount++;
    }

    public static void main(String... args) throws Exception {
        setup();

        for (int count=1; count <= 5; count++) {
            System.out.println("count: " + count);
            // create a class loader and load a native library
            runTest();
            // Unload the class loader and native library, and give the Cleaner
            // thread a chance to unload the native library.
            // unloadedCount is incremented when the native library is unloaded.
            ForceGC gc = new ForceGC();
            final int finalCount = count;
            if (!gc.await(() -> finalCount == unloadedCount)) {
                throw new RuntimeException("Expected unloaded=" + count +
                    " but got=" + unloadedCount);
            }
        }
    }

    /*
     * Loads p.Test class with a new class loader and its static initializer
     * will load a native library.
     *
     * The class loader becomes unreachable when this method returns and
     * the native library should be unloaded at some point after the class
     * loader is garbage collected.
     */
    static void runTest() throws Exception {
        // invoke p.Test.run() that loads the native library
        Runnable r = newTestRunnable();
        r.run();

        // reload the native library by the same class loader
        r.run();

        // load the native library by another class loader
        Runnable r1 = newTestRunnable();
        try {
            r1.run();
            throw new RuntimeException("should fail to load the native library" +
                    " by another class loader");
        } catch (UnsatisfiedLinkError e) {}
    }

    /*
     * Loads p.Test class with a new class loader and returns
     * a Runnable instance.
     */
    static Runnable newTestRunnable() throws Exception {
        TestLoader loader = new TestLoader();
        Class<?> c = Class.forName("p.Test", true, loader);
        return (Runnable) c.newInstance();
    }

    static class TestLoader extends URLClassLoader {
        static URL[] toURLs() {
            try {
                return new URL[] { CLASSES.toUri().toURL() };
            } catch (MalformedURLException e) {
                throw new Error(e);
            }
        }

        TestLoader() {
            super("testloader", toURLs(), ClassLoader.getSystemClassLoader());
        }
    }

    /*
     * move p/Test.class out from classpath to the scratch directory
     */
    static void setup() throws IOException {
        String dir = System.getProperty("test.classes", ".");
        Path file = Paths.get("p", "Test.class");
        Files.createDirectories(CLASSES.resolve("p"));
        Files.move(Paths.get(dir).resolve(file),
                   CLASSES.resolve("p").resolve("Test.class"));
    }
}
