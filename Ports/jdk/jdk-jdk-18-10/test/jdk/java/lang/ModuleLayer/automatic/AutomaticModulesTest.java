/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8211825
 * @modules jdk.compiler
 * @library /test/lib
 * @build jdk.test.lib.compiler.CompilerUtils jdk.test.lib.util.JarUtils
 * @run testng/othervm AutomaticModulesTest
 * @summary Tests automatic modules in module layers
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.ModuleLayer.Controller;
import java.lang.module.*;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.util.JarUtils;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * This test uses two modules:
 *     m requires alib and has an entry point p.Main
 *     alib is an automatic module
 */

@Test
public class AutomaticModulesTest {

    private static final String TEST_SRC = System.getProperty("test.src");

    private static final Path CLASSES = Path.of("classes");
    private static final Path LIB = Path.of("lib");
    private static final Path MODS = Path.of("mods");

    @BeforeTest
    public void setup() throws Exception {
        // javac -d classes src/alib/**
        // jar cf lib/alib.jar -C classes .
        Files.createDirectory(CLASSES);
        assertTrue(CompilerUtils.compile(Path.of(TEST_SRC, "src", "alib"), CLASSES));
        JarUtils.createJarFile(LIB.resolve("alib.jar"), CLASSES);

        // javac -p lib -d mods/m - src/m/**
        Path src = Path.of(TEST_SRC, "src", "m");
        Path output = Files.createDirectories(MODS.resolve("m"));
        assertTrue(CompilerUtils.compile(src, output, "-p", LIB.toString()));
    }

    /**
     * Create a module layer with modules m and alib mapped to the same class
     * loader.
     */
    public void testOneLoader() throws Exception {
        Configuration cf = ModuleLayer.boot()
                .configuration()
                .resolve(ModuleFinder.of(), ModuleFinder.of(MODS, LIB), Set.of("m"));
        ResolvedModule m = cf.findModule("m").orElseThrow();
        ResolvedModule alib = cf.findModule("alib").orElseThrow();
        assertTrue(m.reads().contains(alib));
        assertTrue(alib.reference().descriptor().isAutomatic());
        ModuleLayer bootLayer = ModuleLayer.boot();
        ClassLoader scl = ClassLoader.getSystemClassLoader();
        Controller controller = ModuleLayer.defineModulesWithOneLoader(cf, List.of(bootLayer), scl);
        invokeMain(controller, "m/p.Main");
    }

    /**
     * Create a module layer with modules m and alib mapped to different class
     * loaders. This will test that L(m) delegates to L(alib) in the same layer.
     */
    public void testManyLoaders() throws Exception {
        Configuration cf = ModuleLayer.boot()
                .configuration()
                .resolve(ModuleFinder.of(), ModuleFinder.of(MODS, LIB), Set.of("m"));
        ResolvedModule m = cf.findModule("m").orElseThrow();
        ResolvedModule alib = cf.findModule("alib").orElseThrow();
        assertTrue(m.reads().contains(alib));
        assertTrue(alib.reference().descriptor().isAutomatic());
        ModuleLayer bootLayer = ModuleLayer.boot();
        ClassLoader scl = ClassLoader.getSystemClassLoader();
        Controller controller = ModuleLayer.defineModulesWithManyLoaders(cf, List.of(bootLayer), scl);
        invokeMain(controller, "m/p.Main");
    }

    /**
     * Create a module layer with alib and another module layer with m.
     * This will test that L(m) delegates to L(alib) in a parent layer.
     */
    public void testAutomaticModuleInParent() throws Exception {
        ModuleLayer bootLayer = ModuleLayer.boot();
        ClassLoader scl = ClassLoader.getSystemClassLoader();

        // configuration/layer containing alib
        Configuration cf1 = bootLayer
                .configuration()
                .resolve(ModuleFinder.of(), ModuleFinder.of(LIB), Set.of("alib"));
        ModuleLayer layer1 = bootLayer.defineModulesWithOneLoader(cf1, scl);

        // configuration/layer containing m
        Configuration cf2 = cf1.resolve(ModuleFinder.of(), ModuleFinder.of(MODS), Set.of("m"));
        Controller controller = ModuleLayer.defineModulesWithOneLoader(cf2, List.of(layer1), scl);

        invokeMain(controller, "m/p.Main");
    }

    /**
     * Invokes the main method of the given entry point (module-name/class-name)
     */
    private void invokeMain(Controller controller, String entry) throws Exception {
        String[] s = entry.split("/");
        String moduleName = s[0];
        String className = s[1];
        int pos = className.lastIndexOf('.');
        String packageName = className.substring(0, pos);
        ModuleLayer layer = controller.layer();
        Module module = layer.findModule(moduleName).orElseThrow();
        controller.addExports(module, packageName, this.getClass().getModule());
        ClassLoader loader = layer.findLoader(moduleName);
        Class<?> c = loader.loadClass(className);
        Method m = c.getMethod("main", String[].class);
        m.invoke(null, (Object)new String[0]);
    }
}
