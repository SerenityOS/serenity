/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8202113
 * @summary Test the caller class loader is not kept strongly reachable
 *         by reflection API
 * @library /test/lib/
 * @modules jdk.compiler
 * @build ReflectionCallerCacheTest Members jdk.test.lib.compiler.CompilerUtils
 * @run testng/othervm ReflectionCallerCacheTest
 */

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.util.ForceGC;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ReflectionCallerCacheTest {
    private static final Path CLASSES = Paths.get("classes");
    private static final ReflectionCallerCacheTest TEST = new ReflectionCallerCacheTest();

    @BeforeTest
    public void setup() throws IOException {
        String src = System.getProperty("test.src", ".");
        String classpath = System.getProperty("test.classes", ".");
        boolean rc = CompilerUtils.compile(Paths.get(src, "AccessTest.java"), CLASSES, "-cp", classpath);
        if (!rc) {
            throw new RuntimeException("fail compilation");
        }
    }
    @DataProvider(name = "memberAccess")
    public Object[][] memberAccess() {
        return new Object[][] {
            { "AccessTest$PublicConstructor" },
            { "AccessTest$PublicMethod" },
            { "AccessTest$PublicField" },
            { "AccessTest$ProtectedMethod" },
            { "AccessTest$ProtectedField" },
            { "AccessTest$PrivateMethod" },
            { "AccessTest$PrivateField"},
            { "AccessTest$PublicFinalField"},
            { "AccessTest$PrivateFinalField"},
            { "AccessTest$PublicStaticFinalField"},
            { "AccessTest$PrivateStaticFinalField"},
            { "AccessTest$NewInstance"}
        };
    }

    // Keep the root of the reflective objects strongly reachable
    private final Constructor<?> publicConstructor;
    private final Method publicMethod;
    private final Method protectedMethod;
    private final Method privateMethod;
    private final Field publicField;
    private final Field protectedField;
    private final Field privateField;

    ReflectionCallerCacheTest() {
        try {
            this.publicConstructor = Members.class.getConstructor();
            this.publicMethod = Members.class.getDeclaredMethod("publicMethod");
            this.publicField = Members.class.getDeclaredField("publicField");
            this.protectedMethod = Members.class.getDeclaredMethod("protectedMethod");
            this.protectedField = Members.class.getDeclaredField("protectedField");
            this.privateMethod = Members.class.getDeclaredMethod("privateMethod");
            this.privateField = Members.class.getDeclaredField("privateField");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test(dataProvider = "memberAccess")
    private void load(String classname) throws Exception {
        WeakReference<?> weakLoader = loadAndRunClass(classname);

        // Force garbage collection to trigger unloading of class loader
        new ForceGC().await(() -> weakLoader.get() == null);

        if (weakLoader.get() != null) {
            throw new RuntimeException("Class " + classname + " not unloaded!");
        }
    }

    private WeakReference<?> loadAndRunClass(String classname) throws Exception {
        try (TestLoader loader = new TestLoader()) {
            // Load member access class with custom class loader
            Class<?> c = Class.forName(classname, true, loader);
            // access the reflective member
            Callable callable = (Callable) c.newInstance();
            callable.call();
            return new WeakReference<>(loader);
        }
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
}
