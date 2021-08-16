/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.module.Configuration;
import java.lang.module.ResolvedModule;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Basic test of Class getResource and getResourceAsStream to locate/read
 * resources in named modules.
 */

public class Main {

    static final String NAME = "myresource";

    public static void main(String[] args) throws IOException {

        // create resources in m1
        createResource("m1", Paths.get("."), "m1");
        createResource("m1", Paths.get("p1"), "m1/p1");
        createResource("m1", Paths.get("p1", "impl"), "m1/p1.impl");
        createResource("m1", Paths.get("p1", "resources"), "m1/p1.resources");

        // create resources in m2
        createResource("m2", Paths.get("."), "m2");
        createResource("m2", Paths.get("p2"), "m2/p2");
        createResource("m2", Paths.get("p2", "impl"), "m2/p2.impl");
        createResource("m2", Paths.get("p2", "resources"), "m2/p2.resources");


        // invoke Class getResource from an unnamed module.
        URL url = Main.class.getResource("/" + NAME);
        assertNotNull(url);

        url = Main.class.getResource("/p1/" + NAME);
        assertNull(url);

        url = Main.class.getResource("/p1/impl/" + NAME);
        assertNull(url);

        url = Main.class.getResource("/p1/resources/" + NAME);
        assertEquals(readAllAsString(url), "m1/p1.resources");

        url = Main.class.getResource("/p2/" + NAME);
        assertNull(url);

        url = Main.class.getResource("/p2/impl/" + NAME);
        assertNull(url);

        // m1: exports private p1.resources
        url = Main.class.getResource("/p2/resources/" + NAME);
        assertNull(url);


        // invoke Class getResource from module m1
        url = p1.Main.getResource("/" + NAME);
        assertEquals(readAllAsString(url), "m1");

        url = p1.Main.getResource("/p1/" + NAME);
        assertEquals(readAllAsString(url), "m1/p1");

        url = p1.Main.getResource("/p1/impl/" + NAME);
        assertEquals(readAllAsString(url), "m1/p1.impl");

        url = p1.Main.getResource("/p1/resources/" + NAME);
        assertEquals(readAllAsString(url), "m1/p1.resources");

        // m2: not a named package
        url = p1.Main.getResource(p2.Main.class, "/" + NAME);
        assertEquals(readAllAsString(url), "m2");

        url = p1.Main.getResource(p2.Main.class, "/p2/" + NAME);
        assertNull(url);

        // m2: exports p2.impl to m1
        url = p1.Main.getResource(p2.Main.class, "/p2/impl/" + NAME);
        assertNull(url);

        // m2: exports private p2.resources to m1;
        url = p1.Main.getResource(p2.Main.class, "/p2/resources/" + NAME);
        assertEquals(readAllAsString(url), "m2/p2.resources");


        // invoke Class getResource from module m2
        url = p2.Main.getResource("/" + NAME);
        assertEquals(readAllAsString(url), "m2");

        url = p2.Main.getResource("/p2/" + NAME);
        assertEquals(readAllAsString(url), "m2/p2");

        url = p2.Main.getResource("/p2/impl/" + NAME);
        assertEquals(readAllAsString(url), "m2/p2.impl");

        url = p2.Main.getResource("/p2/resources/" + NAME);
        assertEquals(readAllAsString(url), "m2/p2.resources");

        // m1: not a named package
        url = p2.Main.getResource(p1.Main.class, "/" + NAME);
        assertEquals(readAllAsString(url), "m1");

        url = p2.Main.getResource(p1.Main.class, "/p1/" + NAME);
        assertNull(url);

        url = p2.Main.getResource(p1.Main.class, "/p1/impl/" + NAME);
        assertNull(url);

        // m1: exports private p1.resources;
        url = p2.Main.getResource(p1.Main.class, "/p1/resources/" + NAME);
        assertEquals(readAllAsString(url), "m1/p1.resources");


        // invoke Class getResourceAsStream from an unnamed module.
        InputStream in = Main.class.getResourceAsStream("/" + NAME);
        assertNotNull(in);

        in = Main.class.getResourceAsStream("/p1/" + NAME);
        assertNull(in);

        in = Main.class.getResourceAsStream("/p1/impl/" + NAME);
        assertNull(in);

        in = Main.class.getResourceAsStream("/p1/resources/" + NAME);
        assertEquals(readAllAsString(in), "m1/p1.resources");

        in = Main.class.getResourceAsStream("/p2/" + NAME);
        assertNull(in);

        in = Main.class.getResourceAsStream("/p2/impl/" + NAME);
        assertNull(in);

        in = Main.class.getResourceAsStream("/p2/resources/" + NAME);
        assertNull(in);


        // invoke Class getResourceAsStream from module m1
        in = p1.Main.getResourceAsStream("/" + NAME);
        assertEquals(readAllAsString(in), "m1");

        in = p1.Main.getResourceAsStream("/p1/" + NAME);
        assertEquals(readAllAsString(in), "m1/p1");

        in = p1.Main.getResourceAsStream("/p1/impl/" + NAME);
        assertEquals(readAllAsString(in), "m1/p1.impl");

        in = p1.Main.getResourceAsStream("/p1/resources/" + NAME);
        assertEquals(readAllAsString(in), "m1/p1.resources");

        in = p1.Main.getResourceAsStream(p2.Main.class, "/" + NAME);
        assertEquals(readAllAsString(in), "m2");

        in = p1.Main.getResourceAsStream(p2.Main.class, "/p2/" + NAME);
        assertNull(in);

        in = p1.Main.getResourceAsStream(p2.Main.class, "/p2/impl/" + NAME);
        assertNull(in);

        in = p1.Main.getResourceAsStream(p2.Main.class, "/p2/resources/" + NAME);
        assertEquals(readAllAsString(in), "m2/p2.resources");


        // invoke Class getResourceAsStream from module m2
        in = p2.Main.getResourceAsStream("/" + NAME);
        assertEquals(readAllAsString(in), "m2");

        in = p2.Main.getResourceAsStream("/p2/" + NAME);
        assertEquals(readAllAsString(in), "m2/p2");

        in = p2.Main.getResourceAsStream("/p2/impl/" + NAME);
        assertEquals(readAllAsString(in), "m2/p2.impl");

        in = p2.Main.getResourceAsStream("/p2/resources/" + NAME);
        assertEquals(readAllAsString(in), "m2/p2.resources");

        in = p2.Main.getResourceAsStream(p1.Main.class, "/" + NAME);
        assertEquals(readAllAsString(in), "m1");

        in = p2.Main.getResourceAsStream(p1.Main.class, "/p1/" + NAME);
        assertNull(in);

        in = p2.Main.getResourceAsStream(p1.Main.class, "/p1/impl/" + NAME);
        assertNull(in);

        in = p2.Main.getResourceAsStream(p1.Main.class, "/p1/resources/" + NAME);
        assertEquals(readAllAsString(in), "m1/p1.resources");


        // Nulls
        try {
            Main.class.getResource(null);
            assertTrue(false);
        } catch (NullPointerException expected) { }
        try {
            Main.class.getResourceAsStream(null);
            assertTrue(false);
        } catch (NullPointerException expected) { }
        try {
            p1.Main.class.getResource(null);
            assertTrue(false);
        } catch (NullPointerException expected) { }
        try {
            p1.Main.class.getResourceAsStream(null);
            assertTrue(false);
        } catch (NullPointerException expected) { }

        // SecurityManager case
        System.setSecurityManager(new SecurityManager());

        assertNull(Main.class.getResource("/" + NAME));
        assertNull(p1.Main.getResource("/" + NAME));
        assertNull(p2.Main.getResource("/" + NAME));

        assertNull(Main.class.getResourceAsStream("/" + NAME));
        assertNull(p1.Main.getResourceAsStream("/" + NAME));
        assertNull(p2.Main.getResourceAsStream("/" + NAME));

        System.out.println("Success!");
    }

    /**
     * Create a resource in the sub-directory of the given exploded module
     */
    static void createResource(String mn, Path subdir, String msg) throws IOException {
        Path dir = directoryFor(mn).resolve(subdir);
        Path file = dir.resolve(NAME);
        Files.write(file, msg.getBytes("UTF-8"));
    }

    /**
     * Returns the directory for the given module (by name).
     */
    static Path directoryFor(String name) {
        Configuration cf = ModuleLayer.boot().configuration();
        ResolvedModule resolvedModule = cf.findModule(name).orElse(null);
        if (resolvedModule == null)
            throw new RuntimeException("not found: " + name);
        Path dir = Paths.get(resolvedModule.reference().location().get());
        if (!Files.isDirectory(dir))
            throw new RuntimeException("not a directory: " + dir);
        return dir;
    }

    static String readAllAsString(InputStream in) throws IOException {
        if (in == null)
            return null;
        try (in) {
            return new String(in.readAllBytes(), "UTF-8");
        }
    }

    static String readAllAsString(URL url) throws IOException {
        if (url == null)
            return null;
        InputStream in = url.openStream();
        return readAllAsString(url.openStream());
    }

    static void assertTrue(boolean condition) {
        if (!condition) throw new RuntimeException();
    }

    static void assertNull(Object o) {
        assertTrue(o == null);
    }

    static void assertNotNull(Object o) {
        assertTrue(o != null);
    }

    static void assertEquals(Object actual, Object expected) {
        if (expected == null) {
            assertNull(actual);
        } else {
            assertTrue(expected.equals(actual));
        }
    }

    static void assertNotEquals(Object actual, Object expected) {
        if (expected == null) {
            assertNotNull(actual);
        } else {
            assertTrue(!expected.equals(actual));
        }
    }
}

