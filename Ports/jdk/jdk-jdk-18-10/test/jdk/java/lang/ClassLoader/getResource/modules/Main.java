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

import java.io.InputStream;
import java.lang.module.Configuration;
import java.lang.module.ResolvedModule;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Basic test of ClassLoader getResource and getResourceAsStream when
 * invoked from code in named modules.
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


        // invoke ClassLoader getResource from the unnamed module
        ClassLoader thisLoader = Main.class.getClassLoader();

        URL url = thisLoader.getResource(NAME);
        assertNotNull(url);

        url = thisLoader.getResource("p1/" + NAME);
        assertNull(url);

        url = thisLoader.getResource("p1/impl/" + NAME);
        assertNull(url);

        url = thisLoader.getResource("p1/resources/" + NAME);
        assertEquals(readAllAsString(url), "m1/p1.resources");

        url = thisLoader.getResource("p2/" + NAME);
        assertNull(url);

        url = thisLoader.getResource("p2/impl/" + NAME);
        assertNull(url);

        url = thisLoader.getResource("p2/resources/" + NAME);
        assertNull(url);


        // invoke ClassLoader getResource from module m1
        url = p1.Main.getResourceInClassLoader(NAME);
        assertNotNull(url);

        url = p1.Main.getResourceInClassLoader("p1/" + NAME);
        assertNull(url);

        url = p1.Main.getResourceInClassLoader("p1/impl/" + NAME);
        assertNull(url);

        url = p1.Main.getResourceInClassLoader("p1/resources/" + NAME);
        assertEquals(readAllAsString(url), "m1/p1.resources");

        url = p1.Main.getResourceInClassLoader("p2/" + NAME);
        assertNull(url);

        url = p1.Main.getResourceInClassLoader("p2/impl/" + NAME);
        assertNull(url);

        url = p1.Main.getResourceInClassLoader("p2/resources/" + NAME);
        assertNull(url);


        // invoke ClassLoader getResource from module m2
        url = p2.Main.getResourceInClassLoader(NAME);
        assertNotNull(url);

        url = p2.Main.getResourceInClassLoader("p1/" + NAME);
        assertNull(url);

        url = p2.Main.getResourceInClassLoader("p1/impl/" + NAME);
        assertNull(url);

        url = p2.Main.getResourceInClassLoader("p1/resources/" + NAME);
        assertEquals(readAllAsString(url), "m1/p1.resources");

        url = p2.Main.getResourceInClassLoader("p2/" + NAME);
        assertNull(url);

        url = p2.Main.getResourceInClassLoader("p2/impl/" + NAME);
        assertNull(url);

        url = p2.Main.getResourceInClassLoader("p2/resources/" + NAME);
        assertNull(url);


        // invoke ClassLoader getResources from the unnamed module
        Enumeration<URL> urls = thisLoader.getResources(NAME);
        List<String> resources = readAllAsStrings(urls);
        assertTrue(resources.size() == 2);
        assertTrue(resources.contains("m1"));
        assertTrue(resources.contains("m2"));

        urls = thisLoader.getResources("p1/" + NAME);
        assertTrue(readAllAsStrings(urls).isEmpty());

        urls = thisLoader.getResources("p1/impl/" + NAME);
        assertTrue(readAllAsStrings(urls).isEmpty());

        urls = thisLoader.getResources("p1/resources/" + NAME);
        resources = readAllAsStrings(urls);
        assertTrue(resources.size() == 1);
        assertTrue(resources.contains("m1/p1.resources"));

        urls = thisLoader.getResources("p2/" + NAME);
        assertTrue(readAllAsStrings(urls).isEmpty());

        urls = thisLoader.getResources("p2/impl/" + NAME);
        assertTrue(readAllAsStrings(urls).isEmpty());

        urls = thisLoader.getResources("p2/resources/" + NAME);
        assertTrue(readAllAsStrings(urls).isEmpty());


        // invoke ClassLoader getResources from m1
        urls = p1.Main.getResourcesInClassLoader(NAME);
        resources = readAllAsStrings(urls);
        assertTrue(resources.size() == 2);
        assertTrue(resources.contains("m1"));
        assertTrue(resources.contains("m2"));

        urls = p1.Main.getResourcesInClassLoader("p1/" + NAME);
        assertTrue(readAllAsStrings(urls).isEmpty());

        urls = p1.Main.getResourcesInClassLoader("p1/impl/" + NAME);
        assertTrue(readAllAsStrings(urls).isEmpty());

        urls = p1.Main.getResourcesInClassLoader("p1/resources/" + NAME);
        resources = readAllAsStrings(urls);
        assertTrue(resources.size() == 1);
        assertTrue(resources.contains("m1/p1.resources"));

        urls = p1.Main.getResourcesInClassLoader("p2/" + NAME);
        assertTrue(readAllAsStrings(urls).isEmpty());

        urls = p1.Main.getResourcesInClassLoader("p2/impl/" + NAME);
        assertTrue(readAllAsStrings(urls).isEmpty());

        urls = p1.Main.getResourcesInClassLoader("p2/resources/" + NAME);
        assertTrue(readAllAsStrings(urls).isEmpty());


        // invoke ClassLoader getResources from m2
        urls = p2.Main.getResourcesInClassLoader(NAME);
        resources = readAllAsStrings(urls);
        assertTrue(resources.size() == 2);
        assertTrue(resources.contains("m1"));
        assertTrue(resources.contains("m2"));

        urls = p2.Main.getResourcesInClassLoader("p1/" + NAME);
        assertTrue(readAllAsStrings(urls).isEmpty());

        urls = p2.Main.getResourcesInClassLoader("p1/impl/" + NAME);
        assertTrue(readAllAsStrings(urls).isEmpty());

        urls = p2.Main.getResourcesInClassLoader("p1/resources/" + NAME);
        resources = readAllAsStrings(urls);
        assertTrue(resources.size() == 1);
        assertTrue(resources.contains("m1/p1.resources"));

        urls = p2.Main.getResourcesInClassLoader("p2/" + NAME);
        assertTrue(readAllAsStrings(urls).isEmpty());

        urls = p2.Main.getResourcesInClassLoader("p2/impl/" + NAME);
        assertTrue(readAllAsStrings(urls).isEmpty());

        urls = p2.Main.getResourcesInClassLoader("p2/resources/" + NAME);
        assertTrue(readAllAsStrings(urls).isEmpty());


        // invoke ClassLoader getResourceAsStream from the unnamed module
        InputStream in = thisLoader.getResourceAsStream(NAME);
        assertNotNull(in);

        in = thisLoader.getResourceAsStream("p1/" + NAME);
        assertNull(in);

        in = thisLoader.getResourceAsStream("p1/impl/" + NAME);
        assertNull(in);

        in = thisLoader.getResourceAsStream("p1/resources/" + NAME);
        assertEquals(readAllAsString(in), "m1/p1.resources");

        in = thisLoader.getResourceAsStream("p2/" + NAME);
        assertNull(in);

        in = thisLoader.getResourceAsStream("p2/impl/" + NAME);
        assertNull(in);

        in = thisLoader.getResourceAsStream("p2/resources/" + NAME);
        assertNull(in);


        // invoke ClassLoader getResource from modules m1
        in = p1.Main.getResourceAsStreamInClassLoader(NAME);
        assertNotNull(in);

        in = p1.Main.getResourceAsStreamInClassLoader("p1/" + NAME);
        assertNull(in);

        in = p1.Main.getResourceAsStreamInClassLoader("p1/impl/" + NAME);
        assertNull(in);

        in = p1.Main.getResourceAsStreamInClassLoader("p1/resources/" + NAME);
        assertEquals(readAllAsString(in), "m1/p1.resources");

        in = p1.Main.getResourceAsStreamInClassLoader("p2/" + NAME);
        assertNull(in);

        in = p1.Main.getResourceAsStreamInClassLoader("p2/impl/" + NAME);
        assertNull(in);

        in = p1.Main.getResourceAsStreamInClassLoader("p2/resources/" + NAME);
        assertNull(in);


        // invoke ClassLoader getResource from modules m2
        in = p2.Main.getResourceAsStreamInClassLoader(NAME);
        assertNotNull(in);

        in = p2.Main.getResourceAsStreamInClassLoader("p1/" + NAME);
        assertNull(in);

        in = p2.Main.getResourceAsStreamInClassLoader("p1/impl/" + NAME);
        assertNull(in);

        in = p2.Main.getResourceAsStreamInClassLoader("p1/resources/" + NAME);
        assertEquals(readAllAsString(in), "m1/p1.resources");

        in = p2.Main.getResourceAsStreamInClassLoader("p2/" + NAME);
        assertNull(in);

        in = p2.Main.getResourceAsStreamInClassLoader("p2/impl/" + NAME);
        assertNull(in);

        in = p2.Main.getResourceAsStreamInClassLoader("p2/resources/" + NAME);
        assertNull(in);


        // SecurityManager case
        System.setSecurityManager(new SecurityManager());

        assertNull(Main.class.getClassLoader().getResource("/" + NAME));
        assertNull(p1.Main.getResourceInClassLoader("/" + NAME));
        assertNull(p2.Main.getResourceInClassLoader("/" + NAME));

        assertNull(Main.class.getClassLoader().getResourceAsStream("/" + NAME));
        assertNull(p1.Main.getResourceAsStreamInClassLoader("/" + NAME));
        assertNull(p2.Main.getResourceAsStreamInClassLoader("/" + NAME));

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
    static Path directoryFor(String mn) {
        Configuration cf = ModuleLayer.boot().configuration();
        ResolvedModule resolvedModule = cf.findModule(mn).orElse(null);
        if (resolvedModule == null)
            throw new RuntimeException("not found: " + mn);
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

    static List<String> readAllAsStrings(Enumeration<URL> urls) throws IOException {
        List<String> result = new ArrayList<>();
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            result.add(readAllAsString(url));
        }
        return result;
    }

    static void assertTrue(boolean condition) {
        if (!condition) throw new RuntimeException();
    }

    static void assertFalse(boolean condition) {
        if (condition) throw new RuntimeException();
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

