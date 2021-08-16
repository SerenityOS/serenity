/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8227415 8254975 8270056
 * @run testng/othervm p.ProtectedMethodInOtherPackage
 * @summary method reference to a protected method inherited from its
 *          superclass in a different runtime package where
 *          lambda proxy class has no access to it.
 */

package p;

import q.I;
import q.J;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ProtectedMethodInOtherPackage  {
    @Test
    public static void remotePackageSameLoader() {
        Sub_I sub = new Sub_I();
        sub.test(Paths.get("test"));
    }

    public static class Sub_J extends J {
        Sub_J(Function<Path,String> function) {
            super(function);
        }
    }

    public static class Sub_I extends I {
        public void test(Path path) {
            /*
             * The method reference to an inherited protected method
             * in another package is desugared with REF_invokeVirtual on
             * a bridge method to invoke protected q.I::filename method
             */
            Sub_J c = new Sub_J(this::filename);
            c.check(path);
        }
    }

    @Test
    public static void splitPackage() throws Throwable {
        ClassLoader parent = new Loader("loader-A", null, A.class);
        ClassLoader loader = new Loader("loader-B", parent, B.class);
        Class<?> aClass = Class.forName(A.class.getName(), false, loader);
        Class<?> bClass = Class.forName(B.class.getName(), false, loader);
        assertTrue(aClass.getClassLoader() == parent);
        assertTrue(bClass.getClassLoader() == loader);
        assertEquals(aClass.getPackageName(), bClass.getPackageName());

        Object b = bClass.getDeclaredConstructor().newInstance();

        // verify subclass can access a protected member inherited from
        // its superclass in a split package
        MethodHandle test = MethodHandles.lookup()
                .findVirtual(bClass, "test", MethodType.methodType(void.class));
        test.invoke(b);

        // verify lambda can access a protected member inherited from
        // a superclass of the host class where the superclass is in
        // a split package (not the same runtime package as the host class)
        MethodHandle get = MethodHandles.lookup()
                .findVirtual(bClass, "get", MethodType.methodType(Runnable.class));
        ((Runnable) get.invoke(b)).run();
    }

    @Test
    public static void protectedStaticMethodInSplitPackage() throws Throwable {
        ClassLoader parent = new Loader("loader-A1", null, A1.class);
        ClassLoader loader = new Loader("loader-B1", parent, B1.class);
        Class<?> aClass1 = Class.forName(A1.class.getName(), false, loader);
        Class<?> bClass1 = Class.forName(B1.class.getName(), false, loader);
        assertTrue(aClass1.getClassLoader() == parent);
        assertTrue(bClass1.getClassLoader() == loader);
        assertEquals(aClass1.getPackageName(), bClass1.getPackageName());

        // verify subclass can access a static protected method inherited from
        // its superclass in a split package
        MethodHandle test = MethodHandles.lookup()
                .findStatic(bClass1, "test", MethodType.methodType(void.class));
        test.invoke();

        // verify lambda can access a static protected method inherited from
        // a superclass of the host class where the superclass is in
        // a split package (not the same runtime package as the host class)
        MethodHandle get = MethodHandles.lookup()
                .findStatic(bClass1, "get", MethodType.methodType(Runnable.class));
        ((Runnable) get.invoke()).run();
    }

    static class Loader extends URLClassLoader {
        static final Path CLASSES_DIR = Paths.get(System.getProperty("test.class.path"));
        private final Class<?> c;
        Loader(String name, ClassLoader parent, Class<?> c) {
            super(name, new URL[]{}, parent);
            this.c = c;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (name.equals(c.getName())) {
                try {
                    String path = name.replace('.', '/') + ".class";
                    byte[] bytes = Files.readAllBytes(CLASSES_DIR.resolve(path));
                    return defineClass(name, bytes, 0, bytes.length);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            return super.findClass(name);
        }

    }

    public static class A {
        protected void func() { }
    }

    public static class B extends A {
        public Runnable get() {
            return this::func;
        }
        public void test() {
            func();
        }
    }

    public static class A1 {
        protected static void func() { }
    }

    public static class B1 extends A1 {
        public static Runnable get() {
            return A1::func;
        }
        public static void test() {
            func();
        }
    }
}
