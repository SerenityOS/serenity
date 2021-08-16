/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

/**
 * Tests loading an archived class that has the same class name as one in the
 * jimage. The class should normally fail to load since a classpath class is not
 * allowed to have the same package name as a module in the jimage. However,
 * if --limit-modules was used then archived class should be loaded.
 */

package jdk.test;

public class Main {
    static final ClassLoader BOOT_LOADER     = null;
    static final ClassLoader PLATFORM_LOADER = ClassLoader.getPlatformClassLoader();
    static final ClassLoader SYS_LOADER      = ClassLoader.getSystemClassLoader();

    public static void main(String[] args) throws Exception {
        boolean shouldLoad = false;
        ClassLoader expectedLoader = SYS_LOADER;

        /*
         * 3 Arguments are passed to this test:
         *   1. testName: Name of the test being run.
         *   2. className: Name of the class to load and instantiate.
         *   3. shouldLoad: Either "true" or "false" to indicate whether the class should
         *      successfully load ("true" indicates --limit-modules was used.)
         * The 4th argument is optional. It specifies the classloader.
         */

        assertTrue(args.length <= 4);
        String testName = args[0];
        String className = args[1].replace('/', '.');
        String shouldLoadName = args[2];  // "true" or "false"
        String loaderName = "SYS";
        if (args.length == 4) {
            loaderName = args[3];
        }

        if (shouldLoadName.equals("true")) {
            shouldLoad = true;
        } else if (shouldLoadName.equals("false")) {
            shouldLoad = false;
        } else {
            assertTrue(false);
        }

        if (loaderName.equals("SYS")) {
            expectedLoader = SYS_LOADER;
        } else if (loaderName.equals("EXT")) {
            expectedLoader = PLATFORM_LOADER;
        } else if (loaderName.equals("BOOT")) {
            expectedLoader = BOOT_LOADER;
        }

        System.out.println(testName + ": class=" + className + " shouldLoad=" +
                           shouldLoadName + " by loader:" + expectedLoader);

        // Try to load the specified class with the default ClassLoader.
        Class<?> clazz = null;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            System.out.println(e);
        }

        if (clazz != null) {
            // class loaded
            if (shouldLoad) {
                // Make sure we got the expected defining ClassLoader
                ClassLoader actualLoader = clazz.getClassLoader();
                if (actualLoader != expectedLoader) {
                    throw new RuntimeException(testName + " FAILED: " + clazz + " loaded by " + actualLoader +
                                               ", expected " + expectedLoader);
                }
                // Make sure we got the right version of the class. toString() of an instance
                // of the overridden version of the class should return "hi".
                if (actualLoader == SYS_LOADER) {
                    String s = clazz.newInstance().toString();
                    if (!s.equals("hi")) {
                        throw new RuntimeException(testName + " FAILED: toString() returned \"" + s
                                                   + "\" instead of \"hi\"" );
                    }
                }
                System.out.println(testName + " PASSED: class loaded as expected.");
            } else {
                throw new RuntimeException(testName + " FAILED: class loaded, but should have failed to load.");
            }
        } else {
            // class did not load
            if (shouldLoad) {
                throw new RuntimeException(testName + " FAILED: class failed to load.");
            } else {
                System.out.println(testName + " PASSED: ClassNotFoundException thrown as expected");
            }
        }
    }

    static void assertTrue(boolean expr) {
        if (!expr)
            throw new RuntimeException("assertion failed");
    }
}
