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

/**
 * @test
 * @library /test/lib
 * @modules jdk.compiler
 * @build jdk.test.lib.compiler.CompilerUtils
 * @run testng NoInterferenceTest
 * @summary Basic test of ServiceLoader that ensures there is no interference
 *          when there are two service interfaces of the same name in a layer
 *          or overridden in a child layer.
 */

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import jdk.test.lib.compiler.CompilerUtils;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class NoInterferenceTest {

    private static final String TEST_SRC = System.getProperty("test.src");
    private static final Path SRC_DIR    = Paths.get(TEST_SRC, "modules");
    private static final Path MODS_DIR = Paths.get("mods");
    private static final List<String> MODULES = Arrays.asList("s1", "p1", "s2", "p2");

    @BeforeTest
    void compile() throws Exception {
        Files.createDirectory(MODS_DIR);
        for (String name : MODULES) {
            Path src = SRC_DIR.resolve(name);
            Path output = Files.createDirectory(MODS_DIR.resolve(name));
            assertTrue(CompilerUtils.compile(src, output, "-p", MODS_DIR.toString()));
        }
    }

    @Test
    public void test() throws Exception {
        ModuleFinder empty = ModuleFinder.of();
        ModuleFinder finder = ModuleFinder.of(MODS_DIR);

        ModuleLayer bootLayer = ModuleLayer.boot();

        Configuration cf0 = bootLayer.configuration();
        Configuration cf1 = cf0.resolveAndBind(finder, empty, Set.of("s1", "s2"));
        Configuration cf2 = cf1.resolveAndBind(finder, empty, Set.of("s1", "s2"));

        // cf1 contains s1, p1, s2, p2
        assertTrue(cf1.modules().size() == 4);

        // cf1 contains s1, p1, s2, p2
        assertTrue(cf2.modules().size() == 4);

        ClassLoader scl = ClassLoader.getSystemClassLoader();

        ModuleLayer layer1 = bootLayer.defineModulesWithManyLoaders(cf1, scl);
        testLayer(layer1);

        ModuleLayer layer2 = layer1.defineModulesWithManyLoaders(cf2, scl);
        testLayer(layer2);
    }

    /**
     * Tests that the layer contains s1, p1, s2, and p2.
     *
     * Tests loading instances of s1/p.S and s2/p.S.
     */
    private void testLayer(ModuleLayer layer) throws Exception {
        assertTrue(layer.modules().size() == 4);
        Module s1 = layer.findModule("s1").get();
        Module p1 = layer.findModule("p1").get();
        Module s2 = layer.findModule("s2").get();
        Module p2 = layer.findModule("p2").get();

        // p1 reads s1
        assertTrue(p1.canRead(s1));
        assertFalse(p1.canRead(s2));

        // p2 reads s2
        assertTrue(p2.canRead(s2));
        assertFalse(p2.canRead(s1));

        // iterate over implementations of s1/p.S
        {
            ClassLoader loader = layer.findLoader("s1");
            Class<?> service = loader.loadClass("p.S");

            List<?> list = collectAll(ServiceLoader.load(service, loader));
            assertTrue(list.size() == 1);
            assertTrue(list.get(0).getClass().getModule() == p1);

            list = collectAll(ServiceLoader.load(layer, service));
            assertTrue(list.size() == 1);
            assertTrue(list.get(0).getClass().getModule() == p1);
        }

        // iterate over implementations of s2/p.S
        {
            ClassLoader loader = layer.findLoader("s2");
            Class<?> service = loader.loadClass("p.S");

            List<?> list = collectAll(ServiceLoader.load(service, loader));
            assertTrue(list.size() == 1);
            assertTrue(list.get(0).getClass().getModule() == p2);

            list = collectAll(ServiceLoader.load(layer, service));
            assertTrue(list.size() == 1);
            assertTrue(list.get(0).getClass().getModule() == p2);
        }
    }

    private <E> List<E> collectAll(ServiceLoader<E> loader) {
        List<E> list = new ArrayList<>();
        Iterator<E> iterator = loader.iterator();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        return list;
    }
}
