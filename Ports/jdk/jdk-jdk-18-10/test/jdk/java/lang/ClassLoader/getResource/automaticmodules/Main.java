/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.lang.module.ResolvedModule;
import java.net.URL;
import java.util.Enumeration;

/**
 * Usage: Main $MODULE
 *
 * Finds $MODULE in the boot layer and then tests that it can locate every
 * resource in the module content (JAR file).
 */

public class Main {

    static void testFind(String name) throws Exception {
        // getResource
        URL url = Main.class.getClassLoader().getResource(name);
        if (url == null)
            throw new RuntimeException("Unable to locate: " + name);
        System.out.println(name + " => " + url);

        // getResources
        Enumeration<URL> urls = Main.class.getClassLoader().getResources(name);
        if (!urls.hasMoreElements())
            throw new RuntimeException("Unable to locate: " + name);
        URL first = urls.nextElement();
        if (!first.toURI().equals(url.toURI()))
            throw new RuntimeException("found " + first + " ???");

        // getResourceAsStream
        if (!url.toString().endsWith("/")) {
            InputStream in = Main.class.getClassLoader().getResourceAsStream(name);
            if (in == null)
                throw new RuntimeException("Unable to locate: " + name);
            in.close();
        }
    }

    static void testFindUnchecked(String name) {
        try {
            testFind(name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        String mn = args[0];

        ModuleReference mref = ModuleLayer.boot()
                .configuration()
                .findModule(mn)
                .map(ResolvedModule::reference)
                .orElseThrow(() -> new RuntimeException(mn + " not resolved!!"));

        try (ModuleReader reader = mref.open()) {
            reader.list().forEach(name -> {
                testFindUnchecked(name);

                // if the resource is a directory then find without trailing slash
                if (name.endsWith("/")) {
                    testFindUnchecked(name.substring(0, name.length() - 1));
                }
            });
        }
    }
}
