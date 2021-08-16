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
 * Used with -p or --upgrade-module-path to exercise the replacement
 * of classes in modules that are linked into the runtime image.
 */

package jdk.test;

public class Main {
    static final ClassLoader PLATFORM_LOADER = ClassLoader.getPlatformClassLoader();
    static final ClassLoader SYS_LOADER      = ClassLoader.getSystemClassLoader();

    public static void main(String[] args) throws Exception {
        ClassLoader loader = null;
        boolean shouldOverride = false;

        /*
         * 3 Arguments are passed to this test:
         *   1. className: Name of the class to load.
         *   2. loaderName: Either "platform" or "app", which specifies which ClassLoader is expected
         *      to be the defining ClassLoader once the class is loaded. The initiating
         *      ClassLoader is always the default ClassLoader (which should be the
         *      app (system) ClassLoader.
         *   3. shouldOverride: Either "true" or "false" to indicate whether the loaded class
         *      should be the one we are attempting to override with (not the archived version).
         */

        assertTrue(args.length == 3, "Unexpected number of arguments: expected 3, actual " + args.length);
        String className = args[0].replace('/', '.');
        String loaderName = args[1]; // "platform" or "app"
        String shouldOverrideName = args[2];  // "true" or "false"

        if (loaderName.equals("app")) {
            loader = SYS_LOADER;
        } else if (loaderName.equals("platform")) {
            loader = PLATFORM_LOADER;
        } else {
            assertTrue(false);
        }

        if (shouldOverrideName.equals("true")) {
            shouldOverride = true;
        } else if (shouldOverrideName.equals("false")) {
            shouldOverride = false;
        } else {
            assertTrue(false);
        }

        // Load the class with the default ClassLoader.
        Class<?> clazz = Class.forName(className, true, loader);
        // Make sure we got the expected defining ClassLoader
        testLoader(clazz, loader);

        String s = null;
        if (shouldOverride) {
          // Create an instance and see what toString() returns
          clazz.newInstance().toString();
        }
        // The overridden version of the class should return "hi". Make sure
        // it does only if we are expecting to have loaded the overridden version.
        assertTrue("hi".equals(s) == shouldOverride);
    }

    /**
     * Asserts that given class has the expected defining loader.
     */
    static void testLoader(Class<?> clazz, ClassLoader expected) {
        ClassLoader loader = clazz.getClassLoader();
        if (loader != expected) {
            throw new RuntimeException(clazz + " loaded by " + loader + ", expected " + expected);
        }
    }

    static void assertTrue(boolean expr) {
        assertTrue(expr, "");
    }

    static void assertTrue(boolean expr, String msg) {
        if (!expr)
            throw new RuntimeException("assertion failed: " + msg);
    }
}
