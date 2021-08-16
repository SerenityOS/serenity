/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @modules jdk.zipfs
 * @library /test/lib
 * @build ModulesInCustomFileSystem m1/* m2/*
 *        jdk.test.lib.util.JarUtils
 * @run testng/othervm ModulesInCustomFileSystem
 * @summary Test ModuleFinder to find modules in a custom file system
 */

import java.io.File;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.lang.reflect.Method;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import jdk.test.lib.util.JarUtils;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Test
public class ModulesInCustomFileSystem {
    private static final Path HERE = Paths.get("");

    /**
     * Test exploded modules in a Zip file system.
     */
    public void testExplodedModulesInZipFileSystem() throws Exception {
        Path m1 = findModuleDirectory("m1");
        Path m2 = findModuleDirectory("m2");
        Path mlib = m1.getParent();
        assertEquals(mlib, m2.getParent());

        // create JAR file containing m1/** and m2/**
        Path jar = Files.createTempDirectory(HERE, "mlib").resolve("modules.jar");
        JarUtils.createJarFile(jar, mlib);
        testZipFileSystem(jar);
    }

    /**
     * Test modular JARs in a Zip file system.
     */
    public void testModularJARsInZipFileSystem() throws Exception {
        Path m1 = findModuleDirectory("m1");
        Path m2 = findModuleDirectory("m2");
        Path contents = Files.createTempDirectory(HERE, "contents");
        JarUtils.createJarFile(contents.resolve("m1.jar"), m1);
        JarUtils.createJarFile(contents.resolve("m2.jar"), m2);

        // create JAR file containing m1.jar and m2.jar
        Path jar = Files.createTempDirectory(HERE, "mlib").resolve("modules.jar");
        JarUtils.createJarFile(jar, contents);
        testZipFileSystem(jar);
    }

    /**
     * Opens a JAR file as a file system
     */
    private void testZipFileSystem(Path zip) throws Exception {
        try (FileSystem fs = FileSystems.newFileSystem(zip)) {
            // ModuleFinder to find modules in top-level directory
            Path top = fs.getPath("/");
            ModuleFinder finder = ModuleFinder.of(top);

            // list the modules
            listAllModules(finder);

            // load modules into child layer, invoking m1/p.Main
            loadAndRunModule(finder);
        }
    }

    /**
     * List all modules that the finder finds and the resources in the module.
     */
    private void listAllModules(ModuleFinder finder) throws Exception {
        for (ModuleReference mref : finder.findAll()) {
            System.out.println(mref.descriptor());
            try (ModuleReader reader = mref.open()) {
                reader.list().forEach(name -> System.out.format("  %s%n", name));
            }
        }
    }

    /**
     * Creates a child layer with m1 and m2, invokes m1/p.Main to ensure that
     * classes can be loaded.
     */
    private void loadAndRunModule(ModuleFinder finder) throws Exception {
        ModuleLayer bootLayer = ModuleLayer.boot();
        Configuration cf = bootLayer.configuration()
                .resolve(finder, ModuleFinder.of(), Set.of("m1"));
        ClassLoader scl = ClassLoader.getSystemClassLoader();
        ModuleLayer layer = bootLayer.defineModulesWithOneLoader(cf, scl);
        Class<?> c = layer.findLoader("m1").loadClass("p.Main");
        Method m = c.getMethod("main", String[].class);
        m.invoke(null, (Object)new String[0]);
    }

    /**
     * Find the directory for a module on the module path
     */
    private Path findModuleDirectory(String name) {
        String mp = System.getProperty("jdk.module.path");
        for (String element : mp.split(File.pathSeparator)) {
            Path dir = Paths.get(element).resolve(name);
            if (Files.exists(dir)) {
                return dir;
            }
        }
        assertFalse(true);
        return null;
    }
}
