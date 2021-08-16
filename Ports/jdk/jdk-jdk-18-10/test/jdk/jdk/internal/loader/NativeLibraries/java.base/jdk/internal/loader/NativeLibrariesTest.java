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

package jdk.internal.loader;

import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;

public class NativeLibrariesTest implements Runnable {
    public static final String LIB_NAME = "nativeLibrariesTest";
    // increments when JNI_OnLoad and JNI_OnUnload is invoked.
    // This is only for JNI native library
    private static int loadedCount = 0;
    private static int unloadedCount = 0;
    /*
     * Called by JNI_OnLoad when the native library is unloaded
     */
    static void nativeLibraryLoaded() {
        loadedCount++;
    }

    /*
     * Called by JNI_OnUnload when the native library is unloaded
     */
    static void nativeLibraryUnloaded() {
        unloadedCount++;
    }

    private final NativeLibraries nativeLibraries;
    public NativeLibrariesTest() {
        this.nativeLibraries = NativeLibraries.rawNativeLibraries(NativeLibraries.class, true);
    }

    /*
     * Invoke by p.Test to load the same native library from different class loader
     */
    public void run() {
        load(true); // expect loading of native library succeed
    }

    public void runTest() throws Exception {
        NativeLibrary nl1 = nativeLibraries.loadLibrary(LIB_NAME);
        NativeLibrary nl2 = nativeLibraries.loadLibrary(LIB_NAME);
        assertTrue(nl1 != null && nl2 != null, "fail to load library");
        assertTrue(nl1 == nl2, nl1 + " != " + nl2);
        assertTrue(loadedCount == 0, "Native library loaded.  Expected: JNI_OnUnload not invoked");
        assertTrue(unloadedCount == 0, "native library never unloaded");

        // load successfully even from another loader
        loadWithCustomLoader();

        // unload the native library
        nativeLibraries.unload(nl1);
        assertTrue(unloadedCount == 0, "Native library unloaded.  Expected: JNI_OnUnload not invoked");

        // reload the native library and expect new NativeLibrary instance
        NativeLibrary nl3 = nativeLibraries.loadLibrary(LIB_NAME);
        assertTrue(nl1 != nl3, nl1 + " == " + nl3);
        assertTrue(loadedCount == 0, "Native library loaded.  Expected: JNI_OnUnload not invoked");

        // load successfully even from another loader
        loadWithCustomLoader();
    }

    public void unload() {
        NativeLibrary nl = nativeLibraries.loadLibrary(LIB_NAME);
        // unload the native library
        nativeLibraries.unload(nl);
        assertTrue(unloadedCount == 0, "Native library unloaded.  Expected: JNI_OnUnload not invoked");
    }

    public void load(boolean succeed) {
        NativeLibrary nl = nativeLibraries.loadLibrary(LIB_NAME);
        if (succeed) {
            assertTrue(nl != null, "fail to load library");
        } else {
            assertTrue(nl == null, "load library should fail");
        }
    }

    /*
     * Loads p.Test class with a new class loader and invokes the run() method.
     * p.Test::run invokes NativeLibrariesTest::run
     */
    private void loadWithCustomLoader() throws Exception {
        TestLoader loader = new TestLoader();
        Class<?> c = Class.forName("p.Test", true, loader);
        Constructor<?> ctr = c.getConstructor(Runnable.class);
        Runnable r = (Runnable) ctr.newInstance(this);
        r.run();
    }

    static class TestLoader extends URLClassLoader {
        static URL[] toURLs() {
            try {
                return new URL[] { Paths.get("classes").toUri().toURL() };
            } catch (MalformedURLException e) {
                throw new Error(e);
            }
        }

        TestLoader() {
            super("testloader", toURLs(), ClassLoader.getSystemClassLoader());
        }
    }

    static void assertTrue(boolean value, String msg) {
        if (!value) {
            throw new AssertionError(msg);
        }
    }
}
