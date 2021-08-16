/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8009411
 * @summary Test that a static method on an interface doesn't hide a default
 *          method with the same name and signature in a separate compilation
 *          scenario.
 * @run main StaticInterfaceMethodInWayOfDefault
 */

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public class StaticInterfaceMethodInWayOfDefault {
    public interface A_v1 {
    }

    public interface A_v2 {
        default void m() {
            System.err.println("A.m() called");
        }
    }

    public interface B  extends A_v1 {
        static void m() {
            System.err.println("B.m() called");
        }
    }

    public interface C_v1 extends B {
        default void m() {
            System.err.println("C.m() called");
        }
    }

    public interface C_v2 extends B {
    }

    public static class TestTask implements Callable<String> {
        @Override
        public String call() {
            try {
                Method m = C_v1.class.getMethod("m", (Class<?>[])null);
                return  m.getDeclaringClass().getSimpleName();
            } catch (NoSuchMethodException e) {
                System.err.println("Couldn't find method");
                return "ERROR";
            }
        }
    }

    public static void main(String[] args) throws Exception {
        int errors = 0;
        Callable<String> v1Task = new TestTask();

        ClassLoader v2Loader = new V2ClassLoader(
            StaticInterfaceMethodInWayOfDefault.class.getClassLoader());
        Callable<String> v2Task = (Callable<String>) Class.forName(
            TestTask.class.getName(),
            true,
            v2Loader).newInstance();

        System.err.println("Running using _v1 classes:");
        String res = v1Task.call();
        if(!res.equals("C_v1")) {
            System.err.println("Got wrong method, expecting C_v1, got: " + res);
            errors++;
        }

        System.err.println("Running using _v2 classes:");
        res = v2Task.call();
        if(!res.equals("A_v1")) {
            System.err.println("Got wrong method, expecting A_v1, got: " + res);
            errors++;
        }

        if (errors != 0)
            throw new RuntimeException("Errors found, check log for details");
    }

    /**
     * A ClassLoader implementation that loads alternative implementations of
     * classes. If class name ends with "_v1" it locates instead a class with
     * name ending with "_v2" and loads that class instead.
     */
    static class V2ClassLoader extends ClassLoader {
        V2ClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
            if (name.indexOf('.') < 0) { // root package is our class
                synchronized (getClassLoadingLock(name)) {
                    // First, check if the class has already been loaded
                    Class<?> c = findLoadedClass(name);
                    if (c == null) {
                        c = findClass(name);
                    }
                    if (resolve) {
                        resolveClass(c);
                    }
                    return c;
                }
            }
            else { // not our class
                return super.loadClass(name, resolve);
            }
        }

        @Override
        protected Class<?> findClass(String name)
            throws ClassNotFoundException {
            // special class name -> replace it with alternative name
            if (name.endsWith("_v1")) {
                String altName = name.substring(0, name.length() - 3) + "_v2";
                String altPath = altName.replace('.', '/').concat(".class");
                try (InputStream is = getResourceAsStream(altPath)) {
                    if (is != null) {
                        byte[] bytes = is.readAllBytes();
                        // patch class bytes to contain original name
                        for (int i = 0; i < bytes.length - 2; i++) {
                            if (bytes[i] == '_' &&
                                bytes[i + 1] == 'v' &&
                                bytes[i + 2] == '2') {
                                bytes[i + 2] = '1';
                            }
                        }
                        return defineClass(name, bytes, 0, bytes.length);
                    }
                    else {
                        throw new ClassNotFoundException(name);
                    }
                }
                catch (IOException e) {
                    throw new ClassNotFoundException(name, e);
                }
            }
            else { // not special class name -> just load the class
                String path = name.replace('.', '/').concat(".class");
                try (InputStream is = getResourceAsStream(path)) {
                    if (is != null) {
                        byte[] bytes = is.readAllBytes();
                        return defineClass(name, bytes, 0, bytes.length);
                    }
                    else {
                        throw new ClassNotFoundException(name);
                    }
                }
                catch (IOException e) {
                    throw new ClassNotFoundException(name, e);
                }
            }
        }
    }
}
