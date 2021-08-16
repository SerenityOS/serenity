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

import java.io.*;
import java.net.*;
import sun.hotspot.WhiteBox;

public class LoaderSegregation {
    // Use these definitions instead of literal strings, to avoid typos.
    static final String ONLY_BUILTIN      = "OnlyBuiltin";
    static final String ONLY_UNREGISTERED = "OnlyUnregistered";

    public static void main(String args[]) throws Exception {
        WhiteBox wb = WhiteBox.getWhiteBox();

        // [A] An archived BUILTIN class cannot be a subclass of a non-BUILTIN class.
        // [B] An archived BUILTIN class cannot implement a non-BUILTIN interface.
        if (wb.isSharedClass(LoaderSegregation.class)) {
            // [1] check that CustomLoadee is loadable from archive
            if (!wb.isSharedClass(CustomLoadee.class)) {
                throw new RuntimeException("wb.isSharedClass(CustomLoadee.class) should be true");
            }

            // [2] CustomInterface2_ia should be archived, even though it was not specified in the classlist.
            //     It was successfully dumped as a side effect of attempting to load CustomLoadee2
            //     during dump time. Note that CustomLoadee2 failed to dump because one of its interfaces,
            //     CustomInterface2_ib, was not loadable from the BOOT/EXT/APP classpath. during dump time.
            if (!wb.isSharedClass(CustomInterface2_ia.class)) {
                throw new RuntimeException("wb.isSharedClass(CustomInterface2_ia.class) should be true");
            }

            // [3] Check that the BUILTIN versions of CustomLoadee2 and CustomLoadee3Child are loadable
            //     at run time (since we have append LoaderSegregation_app2.jar the classpath),
            //     but these classes must be loaded from the JAR file.
            if (wb.isSharedClass(CustomLoadee2.class)) {
                throw new RuntimeException("wb.isSharedClass(CustomLoadee2.class) should be false");
            }
            if (wb.isSharedClass(CustomLoadee3.class)) {
                throw new RuntimeException("wb.isSharedClass(CustomLoadee3.class) should be false");
            }
            if (wb.isSharedClass(CustomLoadee3Child.class)) {
                throw new RuntimeException("wb.isSharedClass(CustomLoadee3Child.class) should be false");
            }
        }

        // [C] BUILTIN and UNREGISTERED classes can be loaded only by their corresponding
        //     type of loaders.

        String path = args[0];
        File jarFile = new File(path);
        URL url = new File(path).toURI().toURL();
        URL[] urls = new URL[] {url};
        ClassLoader appLoader = LoaderSegregation.class.getClassLoader();

        { // BUILTIN LOADER
            try {
                appLoader.loadClass(ONLY_UNREGISTERED);
                throw new RuntimeException("BUILTIN loader cannot load archived UNREGISTERED class");
            } catch (ClassNotFoundException expected) {}
        }

        { // UNREGISTERED LOADER
            URLClassLoader urlClassLoader = new URLClassLoader(urls) {
                protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                    synchronized (getClassLoadingLock(name)) {
                        Class<?> c = findLoadedClass(name);
                        if (c == null) {
                            try {
                                c = findClass(name);
                            } catch (ClassNotFoundException e) {
                                c = getParent().loadClass(name);
                            }
                        }
                        if (resolve) {
                            resolveClass(c);
                        }
                        return c;
                    }
                }
            };
            Class<?> c2 = urlClassLoader.loadClass(ONLY_BUILTIN);

            if (c2.getClassLoader() != urlClassLoader) {
                throw new RuntimeException("Error in test");
            }

            if (wb.isSharedClass(LoaderSegregation.class)) {
                if (wb.isSharedClass(c2)) {
                    throw new RuntimeException("wb.isSharedClass(c2) should be false - " +
                                               "unregistered loader cannot load an archived BUILTIN class");
                }
            }
        }
    }
}
