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

/*
 * @test
 * @summary verify if the hidden class is unloaded when the class loader is GC'ed
 * @modules jdk.compiler
 * @library /test/lib/
 * @build jdk.test.lib.util.ForceGC
 * @run testng/othervm UnloadingTest
 */

import java.io.IOException;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

import jdk.test.lib.util.ForceGC;

import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.Utils;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static java.lang.invoke.MethodHandles.lookup;
import static java.lang.invoke.MethodHandles.Lookup.ClassOption.*;
import static org.testng.Assert.*;

public class UnloadingTest {
    private static final Path CLASSES_DIR = Paths.get("classes");
    private static byte[] hiddenClassBytes;

    @BeforeTest
    static void setup() throws IOException {
        Path src = Paths.get(Utils.TEST_SRC, "src", "LookupHelper.java");
        if (!CompilerUtils.compile(src, CLASSES_DIR)) {
            throw new RuntimeException("Compilation of the test failed: " + src);
        }

        hiddenClassBytes = Files.readAllBytes(CLASSES_DIR.resolve("LookupHelper.class"));
    }

    /*
     * Test that a hidden class is unloaded while the loader remains strongly reachable
     */
    @Test
    public void unloadable() throws Exception {
        TestLoader loader = new TestLoader();
        Class<?> helper = Class.forName("LookupHelper", true, loader);
        Method m = helper.getMethod("getLookup");
        Lookup lookup = (Lookup)m.invoke(null);
        HiddenClassUnloader unloader = createHiddenClass(lookup, false);
        // the hidden class should be unloaded
        unloader.unload();

        // loader is strongly reachable
        Reference.reachabilityFence(loader);
    }

    /*
     * Test that a hidden class is not unloaded when the loader is strongly reachable
     */
    @Test
    public void notUnloadable() throws Exception {
        TestLoader loader = new TestLoader();
        Class<?> helper = Class.forName("LookupHelper", true, loader);
        Method m = helper.getMethod("getLookup");
        Lookup lookup = (Lookup)m.invoke(null);
        HiddenClassUnloader unloader = createHiddenClass(lookup, true);
        assertFalse(unloader.tryUnload());      // hidden class is not unloaded

        // loader is strongly reachable
        Reference.reachabilityFence(loader);
    }

    /*
     * Create a nest of two hidden classes.
     * They can be unloaded even the loader is strongly reachable
     */
    @Test
    public void hiddenClassNest() throws Exception {
        TestLoader loader = new TestLoader();
        Class<?> helper = Class.forName("LookupHelper", true, loader);
        Method m = helper.getMethod("getLookup");
        Lookup lookup = (Lookup)m.invoke(null);
        HiddenClassUnloader[] unloaders = createNestOfTwoHiddenClasses(lookup, false, false);

        // keep a strong reference to the nest member class
        Class<?> member = unloaders[1].weakRef.get();
        assertTrue(member != null);
        // nest host and member will not be unloaded
        assertFalse(unloaders[0].tryUnload());
        assertFalse(unloaders[1].tryUnload());

        // clear the reference to the nest member
        Reference.reachabilityFence(member);
        member = null;

        // nest host and member will be unloaded
        unloaders[0].unload();
        unloaders[1].unload();

        // loader is strongly reachable
        Reference.reachabilityFence(loader);
    }

    /*
     * Create a nest with a hidden class nest host and strong nest member.
     * Test that both are not unloaded
     */
    @Test
    public void hiddenClassNestStrongMember() throws Exception {
        TestLoader loader = new TestLoader();
        Class<?> helper = Class.forName("LookupHelper", true, loader);
        Method m = helper.getMethod("getLookup");
        Lookup lookup = (Lookup)m.invoke(null);
        HiddenClassUnloader[] unloaders = createNestOfTwoHiddenClasses(lookup, false, true);
        assertFalse(unloaders[0].tryUnload());      // nest host cannot be unloaded
        assertFalse(unloaders[1].tryUnload());      // nest member cannot be unloaded

        // loader is strongly reachable
        Reference.reachabilityFence(loader);
    }

    /*
     * Create a nest with a strong hidden nest host and a hidden class member.
     * The nest member can be unloaded whereas the nest host will not be unloaded.
     */
    @Test
    public void hiddenClassNestStrongHost() throws Exception {
        TestLoader loader = new TestLoader();
        Class<?> helper = Class.forName("LookupHelper", true, loader);
        Method m = helper.getMethod("getLookup");
        Lookup lookup = (Lookup)m.invoke(null);
        HiddenClassUnloader[] unloaders = createNestOfTwoHiddenClasses(lookup, true, false);
        assertFalse(unloaders[0].tryUnload());      // nest host cannot be unloaded
        unloaders[1].unload();

        // loader is strongly reachable
        Reference.reachabilityFence(loader);
    }

    /*
     * Create a HiddenClassUnloader that holds a weak reference to the newly created
     * hidden class.
     */
    static HiddenClassUnloader createHiddenClass(Lookup lookup, boolean strong) throws Exception {
        Class<?> hc;
        if (strong) {
            hc = lookup.defineHiddenClass(hiddenClassBytes, false, STRONG).lookupClass();
        } else {
            hc = lookup.defineHiddenClass(hiddenClassBytes, false).lookupClass();
        }
        assertTrue(hc.getClassLoader() == lookup.lookupClass().getClassLoader());
        return new HiddenClassUnloader(hc);
    }

    /*
     * Create an array of HiddenClassUnloader with two elements: the first element
     * is for the nest host and the second element is for the nest member.
     */
    static HiddenClassUnloader[] createNestOfTwoHiddenClasses(Lookup lookup, boolean strongHost, boolean strongMember) throws Exception {
        Lookup hostLookup;
        if (strongHost) {
            hostLookup = lookup.defineHiddenClass(hiddenClassBytes, false, STRONG);
        } else {
            hostLookup = lookup.defineHiddenClass(hiddenClassBytes, false);
        }
        Class<?> host = hostLookup.lookupClass();
        Class<?> member;
        if (strongMember) {
            member = hostLookup.defineHiddenClass(hiddenClassBytes, false, NESTMATE, STRONG).lookupClass();
        } else {
            member = hostLookup.defineHiddenClass(hiddenClassBytes, false, NESTMATE).lookupClass();
        }
        assertTrue(member.getNestHost() == host);
        return new HiddenClassUnloader[] { new HiddenClassUnloader(host), new HiddenClassUnloader(member) };
    }

    static class HiddenClassUnloader {
        private final WeakReference<Class<?>> weakRef;
        private HiddenClassUnloader(Class<?> hc) {
            assertTrue(hc.isHidden());
            this.weakRef = new WeakReference<>(hc);
        }

        void unload() {
            // Force garbage collection to trigger unloading of class loader and native library
            ForceGC gc = new ForceGC();
            assertTrue(gc.await(() -> weakRef.get() == null));

            if (weakRef.get() != null) {
                throw new RuntimeException("loader " + " not unloaded!");
            }
        }

        boolean tryUnload() {
            ForceGC gc = new ForceGC();
            return gc.await(() -> weakRef.get() == null);
        }
    }

    static class TestLoader extends URLClassLoader {
        static URL[] toURLs() {
            try {
                return new URL[] { CLASSES_DIR.toUri().toURL() };
            } catch (MalformedURLException e) {
                throw new Error(e);
            }
        }

        static AtomicInteger counter = new AtomicInteger();
        TestLoader() {
            super("testloader-" + counter.addAndGet(1), toURLs(), ClassLoader.getSystemClassLoader());
        }
    }
}
