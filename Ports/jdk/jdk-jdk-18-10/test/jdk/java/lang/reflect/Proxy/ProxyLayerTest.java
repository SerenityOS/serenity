/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.module.ModuleFinder;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import jdk.test.lib.compiler.CompilerUtils;
import static jdk.test.lib.process.ProcessTools.executeTestJava;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * @test
 * @library /test/lib
 * @modules jdk.compiler
 * @build ProxyTest jdk.test.lib.process.ProcessTools
 *        jdk.test.lib.compiler.CompilerUtils
 * @run testng ProxyLayerTest
 * @summary Test proxies to implement interfaces in a layer
 */

public class ProxyLayerTest {

    private static final String TEST_SRC = System.getProperty("test.src");
    private static final String TEST_CLASSES = System.getProperty("test.classes");

    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MODS_DIR = Paths.get("mods");
    private static final Path CPATH_DIR = Paths.get(TEST_CLASSES);

    // the names of the modules in this test
    private static String[] modules = new String[] {"m1", "m2", "m3"};


    /**
     * Compiles all modules used by the test
     */
    @BeforeTest
    public void compileAll() throws Exception {
        for (String mn : modules) {
            Path msrc = SRC_DIR.resolve(mn);
            assertTrue(CompilerUtils.compile(msrc, MODS_DIR, "--module-source-path", SRC_DIR.toString()));
        }
    }

    /**
     * Test proxy implementing interfaces in a layer defined in
     * an unnamed module
     */
    @Test
    public void testProxyInUnnamed() throws Exception {
        ModuleFinder finder = ModuleFinder.of(MODS_DIR);
        ModuleLayer bootLayer = ModuleLayer.boot();
        Configuration cf = bootLayer
                .configuration()
                .resolveAndBind(ModuleFinder.of(), finder, Arrays.asList(modules));
        ClassLoader scl = ClassLoader.getSystemClassLoader();
        ModuleLayer layer = bootLayer.defineModulesWithOneLoader(cf, scl);

        ClassLoader loader = layer.findLoader("m1");

        assertTrue(layer.findModule("m1").isPresent());
        assertTrue(layer.findModule("m2").isPresent());
        assertTrue(layer.findModule("m3").isPresent());

        Class<?>[] interfaces = new Class<?>[] {
            Class.forName("p.one.I", false, loader),
            Class.forName("p.two.A", false, loader),
            Class.forName("p.three.P", false, loader),
        };
        Object o = Proxy.newProxyInstance(loader, interfaces, handler);

        Class<?> proxyClass = o.getClass();
        Package pkg = proxyClass.getPackage();
        assertTrue(proxyClass.getModule().isNamed());
        assertTrue(pkg.isSealed());
        assertTrue(proxyClass.getModule().isExported(pkg.getName()));
        assertEquals(proxyClass.getModule().getLayer(), null);
    }

    /**
     * Test proxy implementing interfaces in a Layer and defined in a
     * dynamic module
     */
    @Test
    public void testProxyInDynamicModule() throws Exception {
        ModuleFinder finder = ModuleFinder.of(MODS_DIR);
        ModuleLayer bootLayer = ModuleLayer.boot();
        Configuration cf = bootLayer
                .configuration()
                .resolveAndBind(ModuleFinder.of(), finder, Arrays.asList(modules));
        ClassLoader scl = ClassLoader.getSystemClassLoader();
        ModuleLayer layer = bootLayer.defineModulesWithOneLoader(cf, scl);

        ClassLoader loader = layer.findLoader("m1");

        assertTrue(layer.findModule("m1").isPresent());
        assertTrue(layer.findModule("m2").isPresent());
        assertTrue(layer.findModule("m3").isPresent());

        Class<?>[] interfaces = new Class<?>[] {
            Class.forName("p.one.internal.J", false, loader),
        };
        Object o = Proxy.newProxyInstance(loader, interfaces, handler);
        Class<?> proxyClass = o.getClass();
        Package pkg = proxyClass.getPackage();
        assertTrue(proxyClass.getModule().isNamed());
        assertTrue(pkg.isSealed());
        assertFalse(proxyClass.getModule().isExported(pkg.getName()));
        assertEquals(proxyClass.getModule().getLayer(), null);
    }

    /**
     * Test proxy implementing interfaces that the target module has no access
     */
    @Test
    public void testNoReadAccess() throws Exception {
        ModuleFinder finder = ModuleFinder.of(MODS_DIR);
        ModuleLayer bootLayer = ModuleLayer.boot();
        Configuration cf = bootLayer
                .configuration()
                .resolveAndBind(ModuleFinder.of(), finder, Arrays.asList(modules));
        ClassLoader scl = ClassLoader.getSystemClassLoader();
        ModuleLayer layer = bootLayer.defineModulesWithOneLoader(cf, scl);

        ClassLoader loader = layer.findLoader("m1");

        assertTrue(layer.findModule("m1").isPresent());
        assertTrue(layer.findModule("m2").isPresent());
        assertTrue(layer.findModule("m3").isPresent());

        Class<?>[] interfaces = new Class<?>[] {
                Class.forName("p.one.I", false, loader),
                Class.forName("p.two.B", false, loader)   // non-public interface but exported package
        };
        checkIAE(loader, interfaces);
    }

    private void checkIAE(ClassLoader loader, Class<?>[] interfaces) {
        try {
            Proxy.getProxyClass(loader, interfaces);
            throw new RuntimeException("Expected IllegalArgumentException thrown");
        } catch (IllegalArgumentException e) {}

        try {
            Proxy.newProxyInstance(loader, interfaces, handler);
            throw new RuntimeException("Expected IllegalArgumentException thrown");
        } catch (IllegalArgumentException e) {}
    }

    private final static InvocationHandler handler =
            (proxy, m, params) -> { throw new RuntimeException(m.toString()); };

}
