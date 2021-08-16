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
 * @summary Test consistent parsing of ex-RUNTIME annotations that
 *          were changed and separately compiled to have CLASS retention
 * @run main AnnotationTypeRuntimeAssumptionTest
 */

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static java.lang.annotation.RetentionPolicy.CLASS;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This test simulates a situation where there are two mutually recursive
 * {@link RetentionPolicy#RUNTIME RUNTIME} annotations {@link AnnA_v1 AnnA_v1}
 * and {@link AnnB AnnB} and then the first is changed to have
 * {@link RetentionPolicy#CLASS CLASS} retention and separately compiled.
 * When {@link AnnA_v1 AnnA_v1} annotation is looked-up on {@link AnnB AnnB}
 * it still appears to have {@link RetentionPolicy#RUNTIME RUNTIME} retention.
 */
public class AnnotationTypeRuntimeAssumptionTest {

    @Retention(RUNTIME)
    @AnnB
    public @interface AnnA_v1 {
    }

    // An alternative version of AnnA_v1 with CLASS retention instead.
    // Used to simulate separate compilation (see AltClassLoader below).
    @Retention(CLASS)
    @AnnB
    public @interface AnnA_v2 {
    }

    @Retention(RUNTIME)
    @AnnA_v1
    public @interface AnnB {
    }

    @AnnA_v1
    public static class TestTask implements Runnable {
        @Override
        public void run() {
            AnnA_v1 ann1 = TestTask.class.getDeclaredAnnotation(AnnA_v1.class);
            if (ann1 != null) {
                throw new IllegalStateException(
                    "@" + ann1.annotationType().getSimpleName() +
                    " found on: " + TestTask.class.getName() +
                    " should not be visible at runtime");
            }
            AnnA_v1 ann2 = AnnB.class.getDeclaredAnnotation(AnnA_v1.class);
            if (ann2 != null) {
                throw new IllegalStateException(
                    "@" + ann2.annotationType().getSimpleName() +
                    " found on: " + AnnB.class.getName() +
                    " should not be visible at runtime");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        ClassLoader altLoader = new AltClassLoader(
            AnnotationTypeRuntimeAssumptionTest.class.getClassLoader());

        Runnable altTask = (Runnable) Class.forName(
            TestTask.class.getName(),
            true,
            altLoader).newInstance();

        altTask.run();
    }

    /**
     * A ClassLoader implementation that loads alternative implementations of
     * classes. If class name ends with "_v1" it locates instead a class with
     * name ending with "_v2" and loads that class instead.
     */
    static class AltClassLoader extends ClassLoader {
        AltClassLoader(ClassLoader parent) {
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
