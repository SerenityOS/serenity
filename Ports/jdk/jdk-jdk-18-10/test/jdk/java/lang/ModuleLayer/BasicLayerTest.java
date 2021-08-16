/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @modules java.base/jdk.internal.access
 * @build BasicLayerTest
 *        jdk.test.lib.util.ModuleUtils
 * @compile layertest/Test.java
 * @run testng BasicLayerTest
 * @summary Basic tests for java.lang.ModuleLayer
 */

import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleFinder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jdk.test.lib.util.ModuleUtils;

import jdk.internal.access.SharedSecrets;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Test
public class BasicLayerTest {

    /**
     * Creates a "non-strict" builder for building a module. This allows the
     * test the create ModuleDescriptor objects that do not require java.base.
     */
    private static ModuleDescriptor.Builder newBuilder(String mn) {
        return SharedSecrets.getJavaLangModuleAccess()
                .newModuleBuilder(mn, false, Set.of());
    }

    /**
     * Exercise ModuleLayer.empty()
     */
    public void testEmpty() {
        ModuleLayer emptyLayer = ModuleLayer.empty();

        assertTrue(emptyLayer.parents().isEmpty());

        assertTrue(emptyLayer.configuration() == Configuration.empty());

        assertTrue(emptyLayer.modules().isEmpty());

        assertFalse(emptyLayer.findModule("java.base").isPresent());

        try {
            emptyLayer.findLoader("java.base");
            assertTrue(false);
        } catch (IllegalArgumentException expected) { }
    }


    /**
     * Exercise ModuleLayer.boot()
     */
    public void testBoot() {
        ModuleLayer bootLayer = ModuleLayer.boot();

        // configuration
        Configuration cf = bootLayer.configuration();
        assertTrue(cf.findModule("java.base").get()
                .reference()
                .descriptor()
                .exports()
                .stream().anyMatch(e -> (e.source().equals("java.lang")
                                         && !e.isQualified())));

        // modules
        Set<Module> modules = bootLayer.modules();
        assertTrue(modules.contains(Object.class.getModule()));
        int count = (int) modules.stream().map(Module::getName).count();
        assertEquals(count, modules.size()); // module names are unique

        // findModule
        Module base = Object.class.getModule();
        assertTrue(bootLayer.findModule("java.base").get() == base);
        assertTrue(base.getLayer() == bootLayer);

        // findLoader
        assertTrue(bootLayer.findLoader("java.base") == null);

        // parents
        assertTrue(bootLayer.parents().size() == 1);
        assertTrue(bootLayer.parents().get(0) == ModuleLayer.empty());
    }


    /**
     * Exercise defineModules, created with empty layer as parent
     */
    public void testLayerOnEmpty() {
        ModuleDescriptor descriptor1 = newBuilder("m1")
                .requires("m2")
                .exports("p1")
                .build();

        ModuleDescriptor descriptor2 = newBuilder("m2")
                .requires("m3")
                .build();

        ModuleDescriptor descriptor3 = newBuilder("m3")
                .build();

        ModuleFinder finder
            = ModuleUtils.finderOf(descriptor1, descriptor2, descriptor3);

        Configuration cf = resolve(finder, "m1");

        // map each module to its own class loader for this test
        ClassLoader loader1 = new ClassLoader() { };
        ClassLoader loader2 = new ClassLoader() { };
        ClassLoader loader3 = new ClassLoader() { };
        Map<String, ClassLoader> map = new HashMap<>();
        map.put("m1", loader1);
        map.put("m2", loader2);
        map.put("m3", loader3);

        ModuleLayer layer = ModuleLayer.empty().defineModules(cf, map::get);

        // configuration
        assertTrue(layer.configuration() == cf);
        assertTrue(layer.configuration().modules().size() == 3);

        // modules
        Set<Module> modules = layer.modules();
        assertTrue(modules.size() == 3);
        Set<String> names = modules.stream()
            .map(Module::getName)
            .collect(Collectors.toSet());
        assertTrue(names.contains("m1"));
        assertTrue(names.contains("m2"));
        assertTrue(names.contains("m3"));

        // findModule
        Module m1 = layer.findModule("m1").get();
        Module m2 = layer.findModule("m2").get();
        Module m3 = layer.findModule("m3").get();
        assertEquals(m1.getName(), "m1");
        assertEquals(m2.getName(), "m2");
        assertEquals(m3.getName(), "m3");
        assertTrue(m1.getDescriptor() == descriptor1);
        assertTrue(m2.getDescriptor() == descriptor2);
        assertTrue(m3.getDescriptor() == descriptor3);
        assertTrue(m1.getLayer() == layer);
        assertTrue(m2.getLayer() == layer);
        assertTrue(m3.getLayer() == layer);
        assertTrue(modules.contains(m1));
        assertTrue(modules.contains(m2));
        assertTrue(modules.contains(m3));
        assertFalse(layer.findModule("godot").isPresent());

        // findLoader
        assertTrue(layer.findLoader("m1") == loader1);
        assertTrue(layer.findLoader("m2") == loader2);
        assertTrue(layer.findLoader("m3") == loader3);
        try {
            ClassLoader loader = layer.findLoader("godot");
            assertTrue(false);
        } catch (IllegalArgumentException ignore) { }

        // parents
        assertTrue(layer.parents().size() == 1);
        assertTrue(layer.parents().get(0) == ModuleLayer.empty());
    }


    /**
     * Exercise defineModules, created with boot layer as parent
     */
    public void testLayerOnBoot() {
        ModuleDescriptor descriptor1 = newBuilder("m1")
                .requires("m2")
                .requires("java.base")
                .exports("p1")
                .build();

        ModuleDescriptor descriptor2 = newBuilder("m2")
                .requires("java.base")
                .build();

        ModuleFinder finder
            = ModuleUtils.finderOf(descriptor1, descriptor2);

        Configuration parent = ModuleLayer.boot().configuration();
        Configuration cf = resolve(parent, finder, "m1");

        ClassLoader loader = new ClassLoader() { };

        ModuleLayer layer = ModuleLayer.boot().defineModules(cf, mn -> loader);

        // configuration
        assertTrue(layer.configuration() == cf);
        assertTrue(layer.configuration().modules().size() == 2);

        // modules
        Set<Module> modules = layer.modules();
        assertTrue(modules.size() == 2);
        Set<String> names = modules.stream()
            .map(Module::getName)
            .collect(Collectors.toSet());
        assertTrue(names.contains("m1"));
        assertTrue(names.contains("m2"));

        // findModule
        Module m1 = layer.findModule("m1").get();
        Module m2 = layer.findModule("m2").get();
        assertEquals(m1.getName(), "m1");
        assertEquals(m2.getName(), "m2");
        assertTrue(m1.getDescriptor() == descriptor1);
        assertTrue(m2.getDescriptor() == descriptor2);
        assertTrue(m1.getLayer() == layer);
        assertTrue(m2.getLayer() == layer);
        assertTrue(modules.contains(m1));
        assertTrue(modules.contains(m2));
        assertTrue(layer.findModule("java.base").get() == Object.class.getModule());
        assertFalse(layer.findModule("godot").isPresent());

        // findLoader
        assertTrue(layer.findLoader("m1") == loader);
        assertTrue(layer.findLoader("m2") == loader);
        assertTrue(layer.findLoader("java.base") == null);

        // parents
        assertTrue(layer.parents().size() == 1);
        assertTrue(layer.parents().get(0) == ModuleLayer.boot());
    }


    /**
     * Exercise defineModules with a configuration of two modules that
     * have the same module-private package.
     */
    public void testPackageContainedInSelfAndOther() {
        ModuleDescriptor descriptor1 =  newBuilder("m1")
                .requires("m2")
                .packages(Set.of("p"))
                .build();

        ModuleDescriptor descriptor2 = newBuilder("m2")
                .packages(Set.of("p"))
                .build();

        ModuleFinder finder
            = ModuleUtils.finderOf(descriptor1, descriptor2);

        Configuration cf = resolve(finder, "m1");
        assertTrue(cf.modules().size() == 2);

        // one loader per module, should be okay
        ModuleLayer.empty().defineModules(cf, mn -> new ClassLoader() { });

        // same class loader
        try {
            ClassLoader loader = new ClassLoader() { };
            ModuleLayer.empty().defineModules(cf, mn -> loader);
            assertTrue(false);
        } catch (LayerInstantiationException expected) { }
    }


    /**
     * Exercise defineModules with a configuration that is a partitioned
     * graph. The same package is exported in both partitions.
     */
    public void testSameExportInPartitionedGraph() {

        // m1 reads m2, m2 exports p to m1
        ModuleDescriptor descriptor1 =  newBuilder("m1")
                .requires("m2")
                .build();
        ModuleDescriptor descriptor2 =  newBuilder("m2")
                .exports("p", Set.of("m1"))
                .build();

        // m3 reads m4, m4 exports p to m3
        ModuleDescriptor descriptor3 = newBuilder("m3")
                .requires("m4")
                .build();
        ModuleDescriptor descriptor4 = newBuilder("m4")
                .exports("p", Set.of("m3"))
                .build();

        ModuleFinder finder
            = ModuleUtils.finderOf(descriptor1,
                                   descriptor2,
                                   descriptor3,
                                   descriptor4);

        Configuration cf = resolve(finder, "m1", "m3");
        assertTrue(cf.modules().size() == 4);

        // one loader per module
        ModuleLayer.empty().defineModules(cf, mn -> new ClassLoader() { });

        // m1 & m2 in one loader, m3 & m4 in another loader
        ClassLoader loader1 = new ClassLoader() { };
        ClassLoader loader2 = new ClassLoader() { };
        Map<String, ClassLoader> map = new HashMap<>();
        map.put("m1", loader1);
        map.put("m2", loader1);
        map.put("m3", loader2);
        map.put("m4", loader2);
        ModuleLayer.empty().defineModules(cf, map::get);

        // same loader
        try {
            ClassLoader loader = new ClassLoader() { };
            ModuleLayer.empty().defineModules(cf, mn -> loader);
            assertTrue(false);
        } catch (LayerInstantiationException expected) { }
    }


    /**
     * Exercise defineModules with a configuration with a module that
     * contains a package that is the same name as a non-exported package in
     * a parent layer.
     */
    public void testContainsSamePackageAsBootLayer() {

        // check assumption that java.base contains sun.launcher
        ModuleDescriptor base = Object.class.getModule().getDescriptor();
        assertTrue(base.packages().contains("sun.launcher"));

        ModuleDescriptor descriptor = newBuilder("m1")
               .requires("java.base")
               .packages(Set.of("sun.launcher"))
               .build();

        ModuleFinder finder = ModuleUtils.finderOf(descriptor);

        Configuration parent = ModuleLayer.boot().configuration();
        Configuration cf = parent.resolve(finder, ModuleFinder.of(), Set.of("m1"));
        assertTrue(cf.modules().size() == 1);

        ClassLoader loader = new ClassLoader() { };
        ModuleLayer layer = ModuleLayer.boot().defineModules(cf, mn -> loader);
        assertTrue(layer.modules().size() == 1);
   }


    /**
     * Test layers with implied readability.
     *
     * The test consists of three configurations:
     * - Configuration/layer1: m1, m2 requires transitive m1
     * - Configuration/layer2: m3 requires m1
     */
    public void testImpliedReadabilityWithLayers1() {

        // cf1: m1 and m2, m2 requires transitive m1

        ModuleDescriptor descriptor1 = newBuilder("m1")
                .build();

        ModuleDescriptor descriptor2 = newBuilder("m2")
                .requires(Set.of(Requires.Modifier.TRANSITIVE), "m1")
                .build();

        ModuleFinder finder1 = ModuleUtils.finderOf(descriptor1, descriptor2);

        Configuration cf1 = resolve(finder1, "m2");

        ClassLoader cl1 = new ClassLoader() { };
        ModuleLayer layer1 = ModuleLayer.empty().defineModules(cf1, mn -> cl1);


        // cf2: m3, m3 requires m2

        ModuleDescriptor descriptor3 = newBuilder("m3")
                .requires("m2")
                .build();

        ModuleFinder finder2 = ModuleUtils.finderOf(descriptor3);

        Configuration cf2 = resolve(cf1, finder2, "m3");

        ClassLoader cl2 = new ClassLoader() { };
        ModuleLayer layer2 = layer1.defineModules(cf2, mn -> cl2);

        assertTrue(layer1.parents().size() == 1);
        assertTrue(layer1.parents().get(0) == ModuleLayer.empty());

        assertTrue(layer2.parents().size() == 1);
        assertTrue(layer2.parents().get(0) == layer1);

        Module m1 = layer2.findModule("m1").get();
        Module m2 = layer2.findModule("m2").get();
        Module m3 = layer2.findModule("m3").get();

        assertTrue(m1.getLayer() == layer1);
        assertTrue(m2.getLayer() == layer1);
        assertTrue(m3.getLayer() == layer2);

        assertTrue(m1.getClassLoader() == cl1);
        assertTrue(m2.getClassLoader() == cl1);
        assertTrue(m3.getClassLoader() == cl2);

        assertTrue(m1.canRead(m1));
        assertFalse(m1.canRead(m2));
        assertFalse(m1.canRead(m3));

        assertTrue(m2.canRead(m1));
        assertTrue(m2.canRead(m2));
        assertFalse(m2.canRead(m3));

        assertTrue(m3.canRead(m1));
        assertTrue(m3.canRead(m2));
        assertTrue(m3.canRead(m3));
    }


    /**
     * Test layers with implied readability.
     *
     * The test consists of three configurations:
     * - Configuration/layer1: m1
     * - Configuration/layer2: m2 requires transitive m3, m3 requires m2
     */
    public void testImpliedReadabilityWithLayers2() {

        // cf1: m1

        ModuleDescriptor descriptor1 = newBuilder("m1").build();

        ModuleFinder finder1 = ModuleUtils.finderOf(descriptor1);

        Configuration cf1 = resolve(finder1, "m1");

        ClassLoader cl1 = new ClassLoader() { };
        ModuleLayer layer1 = ModuleLayer.empty().defineModules(cf1, mn -> cl1);


        // cf2: m2, m3: m2 requires transitive m1, m3 requires m2

        ModuleDescriptor descriptor2 = newBuilder("m2")
                .requires(Set.of(Requires.Modifier.TRANSITIVE), "m1")
                .build();

        ModuleDescriptor descriptor3 = newBuilder("m3")
                .requires("m2")
                .build();

        ModuleFinder finder2 = ModuleUtils.finderOf(descriptor2, descriptor3);

        Configuration cf2 = resolve(cf1, finder2, "m3");

        ClassLoader cl2 = new ClassLoader() { };
        ModuleLayer layer2 = layer1.defineModules(cf2, mn -> cl2);

        assertTrue(layer1.parents().size() == 1);
        assertTrue(layer1.parents().get(0) == ModuleLayer.empty());

        assertTrue(layer2.parents().size() == 1);
        assertTrue(layer2.parents().get(0) == layer1);

        Module m1 = layer2.findModule("m1").get();
        Module m2 = layer2.findModule("m2").get();
        Module m3 = layer2.findModule("m3").get();

        assertTrue(m1.getLayer() == layer1);
        assertTrue(m2.getLayer() == layer2);
        assertTrue(m3.getLayer() == layer2);

        assertTrue(m1.canRead(m1));
        assertFalse(m1.canRead(m2));
        assertFalse(m1.canRead(m3));

        assertTrue(m2.canRead(m1));
        assertTrue(m2.canRead(m2));
        assertFalse(m2.canRead(m3));

        assertTrue(m3.canRead(m1));
        assertTrue(m3.canRead(m2));
        assertTrue(m3.canRead(m3));
    }


    /**
     * Test layers with implied readability.
     *
     * The test consists of three configurations:
     * - Configuration/layer1: m1
     * - Configuration/layer2: m2 requires transitive m1
     * - Configuration/layer3: m3 requires m1
     */
    public void testImpliedReadabilityWithLayers3() {

        // cf1: m1

        ModuleDescriptor descriptor1 = newBuilder("m1").build();

        ModuleFinder finder1 = ModuleUtils.finderOf(descriptor1);

        Configuration cf1 = resolve(finder1, "m1");

        ClassLoader cl1 = new ClassLoader() { };
        ModuleLayer layer1 = ModuleLayer.empty().defineModules(cf1, mn -> cl1);


        // cf2: m2 requires transitive m1

        ModuleDescriptor descriptor2 = newBuilder("m2")
                .requires(Set.of(Requires.Modifier.TRANSITIVE), "m1")
                .build();

        ModuleFinder finder2 = ModuleUtils.finderOf(descriptor2);

        Configuration cf2 = resolve(cf1, finder2, "m2");

        ClassLoader cl2 = new ClassLoader() { };
        ModuleLayer layer2 = layer1.defineModules(cf2, mn -> cl2);


        // cf3: m3 requires m2

        ModuleDescriptor descriptor3 = newBuilder("m3")
                .requires("m2")
                .build();

        ModuleFinder finder3 = ModuleUtils.finderOf(descriptor3);

        Configuration cf3 = resolve(cf2, finder3, "m3");

        ClassLoader cl3 = new ClassLoader() { };
        ModuleLayer layer3 = layer2.defineModules(cf3, mn -> cl3);

        assertTrue(layer1.parents().size() == 1);
        assertTrue(layer1.parents().get(0) == ModuleLayer.empty());

        assertTrue(layer2.parents().size() == 1);
        assertTrue(layer2.parents().get(0) == layer1);

        assertTrue(layer3.parents().size() == 1);
        assertTrue(layer3.parents().get(0) == layer2);

        Module m1 = layer3.findModule("m1").get();
        Module m2 = layer3.findModule("m2").get();
        Module m3 = layer3.findModule("m3").get();

        assertTrue(m1.getLayer() == layer1);
        assertTrue(m2.getLayer() == layer2);
        assertTrue(m3.getLayer() == layer3);

        assertTrue(m1.canRead(m1));
        assertFalse(m1.canRead(m2));
        assertFalse(m1.canRead(m3));

        assertTrue(m2.canRead(m1));
        assertTrue(m2.canRead(m2));
        assertFalse(m2.canRead(m3));

        assertTrue(m3.canRead(m1));
        assertTrue(m3.canRead(m2));
        assertTrue(m3.canRead(m3));
    }


    /**
     * Test layers with implied readability.
     *
     * The test consists of two configurations:
     * - Configuration/layer1: m1, m2 requires transitive m1
     * - Configuration/layer2: m3 requires transitive m2, m4 requires m3
     */
    public void testImpliedReadabilityWithLayers4() {

        // cf1: m1, m2 requires transitive m1

        ModuleDescriptor descriptor1 = newBuilder("m1")
                .build();

        ModuleDescriptor descriptor2 = newBuilder("m2")
                .requires(Set.of(Requires.Modifier.TRANSITIVE), "m1")
                .build();

        ModuleFinder finder1 = ModuleUtils.finderOf(descriptor1, descriptor2);

        Configuration cf1 = resolve(finder1, "m2");

        ClassLoader cl1 = new ClassLoader() { };
        ModuleLayer layer1 = ModuleLayer.empty().defineModules(cf1, mn -> cl1);


        // cf2: m3 requires transitive m2, m4 requires m3

        ModuleDescriptor descriptor3 = newBuilder("m3")
                .requires(Set.of(Requires.Modifier.TRANSITIVE), "m2")
                .build();

        ModuleDescriptor descriptor4 = newBuilder("m4")
                .requires("m3")
                .build();


        ModuleFinder finder2 = ModuleUtils.finderOf(descriptor3, descriptor4);

        Configuration cf2 = resolve(cf1, finder2, "m3", "m4");

        ClassLoader cl2 = new ClassLoader() { };
        ModuleLayer layer2 = layer1.defineModules(cf2, mn -> cl2);

        assertTrue(layer1.parents().size() == 1);
        assertTrue(layer1.parents().get(0) == ModuleLayer.empty());

        assertTrue(layer2.parents().size() == 1);
        assertTrue(layer2.parents().get(0) == layer1);

        Module m1 = layer2.findModule("m1").get();
        Module m2 = layer2.findModule("m2").get();
        Module m3 = layer2.findModule("m3").get();
        Module m4 = layer2.findModule("m4").get();

        assertTrue(m1.getLayer() == layer1);
        assertTrue(m2.getLayer() == layer1);
        assertTrue(m3.getLayer() == layer2);
        assertTrue(m4.getLayer() == layer2);

        assertTrue(m1.canRead(m1));
        assertFalse(m1.canRead(m2));
        assertFalse(m1.canRead(m3));
        assertFalse(m1.canRead(m4));

        assertTrue(m2.canRead(m1));
        assertTrue(m2.canRead(m2));
        assertFalse(m1.canRead(m3));
        assertFalse(m1.canRead(m4));

        assertTrue(m3.canRead(m1));
        assertTrue(m3.canRead(m2));
        assertTrue(m3.canRead(m3));
        assertFalse(m3.canRead(m4));

        assertTrue(m4.canRead(m1));
        assertTrue(m4.canRead(m2));
        assertTrue(m4.canRead(m3));
        assertTrue(m4.canRead(m4));
    }


    /**
     * Test layers with a qualified export. The module exporting the package
     * does not read the target module.
     *
     * m1 { exports p to m2 }
     * m2 { }
     */
    public void testQualifiedExports1() {
        ModuleDescriptor descriptor1 = newBuilder("m1").
                exports("p", Set.of("m2"))
                .build();

        ModuleDescriptor descriptor2 = newBuilder("m2")
                .build();

        ModuleFinder finder1 = ModuleUtils.finderOf(descriptor1, descriptor2);

        Configuration cf = resolve(finder1, "m1", "m2");

        ClassLoader cl = new ClassLoader() { };
        ModuleLayer layer = ModuleLayer.empty().defineModules(cf, mn -> cl);
        assertTrue(layer.modules().size() == 2);

        Module m1 = layer.findModule("m1").get();
        Module m2 = layer.findModule("m2").get();

        // check m1 exports p to m2
        assertFalse(m1.isExported("p"));
        assertTrue(m1.isExported("p", m2));
        assertFalse(m1.isOpen("p", m2));
    }


    /**
     * Test layers with a qualified export. The module exporting the package
     * reads the target module.
     *
     * m1 { exports p to m2; }
     * m2 { requires m1; }
     */
    public void testQualifiedExports2() {
        ModuleDescriptor descriptor1 = newBuilder("m1")
                .exports("p", Set.of("m2"))
                .build();

        ModuleDescriptor descriptor2 = newBuilder("m2")
                .requires("m1")
                .build();

        ModuleFinder finder1 = ModuleUtils.finderOf(descriptor1, descriptor2);

        Configuration cf = resolve(finder1, "m2");
        ClassLoader cl = new ClassLoader() { };
        ModuleLayer layer = ModuleLayer.empty().defineModules(cf, mn -> cl);
        assertTrue(layer.modules().size() == 2);

        Module m1 = layer.findModule("m1").get();
        Module m2 = layer.findModule("m2").get();

        // check m1 exports p to m2
        assertFalse(m1.isExported("p"));
        assertTrue(m1.isExported("p", m2));
        assertFalse(m1.isOpen("p", m2));
    }


    /**
     * Test layers with a qualified export. The module exporting the package
     * does not read the target module in the parent layer.
     *
     * - Configuration/layer1: m1 { }
     * - Configuration/layer2: m2 { exports p to m1; }
     */
    public void testQualifiedExports3() {
        // create layer1 with m1
        ModuleDescriptor descriptor1 = newBuilder("m1").build();
        ModuleFinder finder1 = ModuleUtils.finderOf(descriptor1);
        Configuration cf1 = resolve(finder1, "m1");
        ClassLoader cl1 = new ClassLoader() { };
        ModuleLayer layer1 = ModuleLayer.empty().defineModules(cf1, mn -> cl1);
        assertTrue(layer1.modules().size() == 1);

        // create layer2 with m2
        ModuleDescriptor descriptor2 = newBuilder("m2")
                .exports("p", Set.of("m1"))
                .build();
        ModuleFinder finder2 = ModuleUtils.finderOf(descriptor2);
        Configuration cf2 = resolve(cf1, finder2, "m2");
        ClassLoader cl2 = new ClassLoader() { };
        ModuleLayer layer2 = layer1.defineModules(cf2, mn -> cl2);
        assertTrue(layer2.modules().size() == 1);

        Module m1 = layer1.findModule("m1").get();
        Module m2 = layer2.findModule("m2").get();

        // check m2 exports p to layer1/m1
        assertFalse(m2.isExported("p"));
        assertTrue(m2.isExported("p", m1));
        assertFalse(m2.isOpen("p", m1));
    }


    /**
     * Test layers with a qualified export. The module exporting the package
     * reads the target module in the parent layer.
     *
     * - Configuration/layer1: m1 { }
     * - Configuration/layer2: m2 { requires m1; exports p to m1; }
     */
    public void testQualifiedExports4() {
        // create layer1 with m1
        ModuleDescriptor descriptor1 = newBuilder("m1").build();
        ModuleFinder finder1 = ModuleUtils.finderOf(descriptor1);
        Configuration cf1 = resolve(finder1, "m1");
        ClassLoader cl1 = new ClassLoader() { };
        ModuleLayer layer1 = ModuleLayer.empty().defineModules(cf1, mn -> cl1);
        assertTrue(layer1.modules().size() == 1);

        // create layer2 with m2
        ModuleDescriptor descriptor2 = newBuilder("m2")
                .requires("m1")
                .exports("p", Set.of("m1"))
                .build();
        ModuleFinder finder2 = ModuleUtils.finderOf(descriptor2);
        Configuration cf2 = resolve(cf1, finder2, "m2");
        ClassLoader cl2 = new ClassLoader() { };
        ModuleLayer layer2 = layer1.defineModules(cf2, mn -> cl2);
        assertTrue(layer2.modules().size() == 1);

        Module m1 = layer1.findModule("m1").get();
        Module m2 = layer2.findModule("m2").get();

        // check m2 exports p to layer1/m1
        assertFalse(m2.isExported("p"));
        assertTrue(m2.isExported("p", m1));
        assertFalse(m2.isOpen("p", m1));
    }

    /**
     * Test layers with a qualified export. The module exporting the package
     * does not read the target module.
     *
     * - Configuration/layer1: m1
     * - Configuration/layer2: m1, m2 { exports p to m1; }
     */
    public void testQualifiedExports5() {
        // create layer1 with m1
        ModuleDescriptor descriptor1 = newBuilder("m1").build();
        ModuleFinder finder1 = ModuleUtils.finderOf(descriptor1);
        Configuration cf1 = resolve(finder1, "m1");
        ClassLoader cl1 = new ClassLoader() { };
        ModuleLayer layer1 = ModuleLayer.empty().defineModules(cf1, mn -> cl1);
        assertTrue(layer1.modules().size() == 1);

        // create layer2 with m1 and m2
        ModuleDescriptor descriptor2 = newBuilder("m2").exports("p", Set.of("m1")).build();
        ModuleFinder finder2 = ModuleUtils.finderOf(descriptor1, descriptor2);
        Configuration cf2 = resolve(cf1, finder2, "m1", "m2");
        ClassLoader cl2 = new ClassLoader() { };
        ModuleLayer layer2 = layer1.defineModules(cf2, mn -> cl2);
        assertTrue(layer2.modules().size() == 2);

        Module m1_v1 = layer1.findModule("m1").get();
        Module m1_v2 = layer2.findModule("m1").get();
        Module m2 = layer2.findModule("m2").get();

        // check m2 exports p to layer2/m2
        assertFalse(m2.isExported("p"));
        assertTrue(m2.isExported("p", m1_v2));
        assertFalse(m2.isExported("p", m1_v1));
    }


    /**
     * Test layers with a qualified export. The module exporting the package
     * reads the target module in the parent layer (due to requires transitive).
     *
     * - Configuration/layer1: m1, m2 { requires transitive m1; }
     * - Configuration/layer2: m1, m3 { requires m2; exports p to m1; }
     */
    public void testQualifiedExports6() {
        // create layer1 with m1 and m2
        ModuleDescriptor descriptor1 = newBuilder("m1").build();
        ModuleDescriptor descriptor2 = newBuilder("m2")
                .requires(Set.of(Requires.Modifier.TRANSITIVE), "m1")
                .build();
        ModuleFinder finder1 = ModuleUtils.finderOf(descriptor1, descriptor2);
        Configuration cf1 = resolve(finder1, "m2");
        ClassLoader loader1 = new ClassLoader() { };
        ModuleLayer layer1 = ModuleLayer.empty().defineModules(cf1, mn -> loader1);
        assertTrue(layer1.modules().size() == 2);

        // create layer2 with m1 and m3
        ModuleDescriptor descriptor3 = newBuilder("m3")
                .requires("m2")
                .exports("p", Set.of("m1"))
                .build();
        ModuleFinder finder2 = ModuleUtils.finderOf(descriptor1, descriptor3);
        Configuration cf2 = resolve(cf1, finder2, "m1", "m3");
        ClassLoader loader2 = new ClassLoader() { };
        ModuleLayer layer2 = layer1.defineModules(cf2, mn -> loader2);
        assertTrue(layer2.modules().size() == 2);

        Module m1_v1 = layer1.findModule("m1").get();
        Module m2 = layer1.findModule("m2").get();

        Module m1_v2 = layer2.findModule("m1").get();
        Module m3 = layer2.findModule("m3").get();

        assertTrue(m3.canRead(m1_v1));
        assertFalse(m3.canRead(m1_v2));

        assertFalse(m3.isExported("p"));
        assertTrue(m3.isExported("p", m1_v1));
        assertFalse(m3.isExported("p", m1_v2));
        assertFalse(m3.isExported("p", m2));
    }


    /**
     * Test layers with a qualified export. The target module is not in any layer.
     *
     * - Configuration/layer1: m1 { }
     * - Configuration/layer2: m2 { exports p to m3; }
     */
    public void testQualifiedExports7() {
        // create layer1 with m1
        ModuleDescriptor descriptor1 = newBuilder("m1").build();
        ModuleFinder finder1 = ModuleUtils.finderOf(descriptor1);
        Configuration cf1 = resolve(finder1, "m1");
        ClassLoader cl1 = new ClassLoader() { };
        ModuleLayer layer1 = ModuleLayer.empty().defineModules(cf1, mn -> cl1);
        assertTrue(layer1.modules().size() == 1);

        // create layer2 with m2
        ModuleDescriptor descriptor2 = newBuilder("m2")
                .exports("p", Set.of("m3"))
                .build();
        ModuleFinder finder2 = ModuleUtils.finderOf(descriptor2);
        Configuration cf2 = resolve(cf1, finder2, "m2");
        ClassLoader cl2 = new ClassLoader() { };
        ModuleLayer layer2 = layer1.defineModules(cf2, mn -> cl2);
        assertTrue(layer2.modules().size() == 1);

        Module m1 = layer1.findModule("m1").get();
        Module m2 = layer2.findModule("m2").get();

        // check m2 does not export p to anyone
        assertFalse(m2.isExported("p"));
        assertFalse(m2.isExported("p", m1));
    }

    /**
     * Attempt to use defineModules to create a layer with a module defined
     * to a class loader that already has a module of the same name defined
     * to the class loader.
     */
    @Test(expectedExceptions = { LayerInstantiationException.class })
    public void testModuleAlreadyDefinedToLoader() {

        ModuleDescriptor md = newBuilder("m")
                .requires("java.base")
                .build();

        ModuleFinder finder = ModuleUtils.finderOf(md);

        Configuration parent = ModuleLayer.boot().configuration();

        Configuration cf = parent.resolve(finder, ModuleFinder.of(), Set.of("m"));

        ClassLoader loader = new ClassLoader() { };

        ModuleLayer.boot().defineModules(cf, mn -> loader);

        // should throw LayerInstantiationException as m1 already defined to loader
        ModuleLayer.boot().defineModules(cf, mn -> loader);

    }


    /**
     * Attempt to use defineModules to create a layer with a module containing
     * package {@code p} where the class loader already has a module defined
     * to it containing package {@code p}.
     */
    @Test(expectedExceptions = { LayerInstantiationException.class })
    public void testPackageAlreadyInNamedModule() {

        ModuleDescriptor md1 = newBuilder("m1")
                .packages(Set.of("p"))
                .requires("java.base")
                .build();

        ModuleDescriptor md2 = newBuilder("m2")
                .packages(Set.of("p"))
                .requires("java.base")
                .build();

        ModuleFinder finder = ModuleUtils.finderOf(md1, md2);

        ClassLoader loader = new ClassLoader() { };

        // define m1 containing package p to class loader

        Configuration parent = ModuleLayer.boot().configuration();

        Configuration cf1 = parent.resolve(finder, ModuleFinder.of(), Set.of("m1"));

        ModuleLayer layer1 = ModuleLayer.boot().defineModules(cf1, mn -> loader);

        // attempt to define m2 containing package p to class loader

        Configuration cf2 = parent.resolve(finder, ModuleFinder.of(), Set.of("m2"));

        // should throw exception because p already in m1
        ModuleLayer layer2 = ModuleLayer.boot().defineModules(cf2, mn -> loader);

    }


    /**
     * Attempt to use defineModules to create a layer with a module
     * containing a package in which a type is already loaded by the class
     * loader.
     */
    @Test(expectedExceptions = { LayerInstantiationException.class })
    public void testPackageAlreadyInUnnamedModule() throws Exception {

        Class<?> c = layertest.Test.class;
        assertFalse(c.getModule().isNamed());  // in unnamed module

        ModuleDescriptor md = newBuilder("m")
                .packages(Set.of(c.getPackageName()))
                .requires("java.base")
                .build();

        ModuleFinder finder = ModuleUtils.finderOf(md);

        Configuration parent = ModuleLayer.boot().configuration();
        Configuration cf = parent.resolve(finder, ModuleFinder.of(), Set.of("m"));

        ModuleLayer.boot().defineModules(cf, mn -> c.getClassLoader());
    }


    /**
     * Attempt to create a layer with a module named "java.base".
     */
    public void testLayerWithJavaBase() {
        ModuleDescriptor descriptor = newBuilder("java.base")
                .exports("java.lang")
                .build();

        ModuleFinder finder = ModuleUtils.finderOf(descriptor);

        Configuration cf = ModuleLayer.boot()
            .configuration()
            .resolve(finder, ModuleFinder.of(), Set.of("java.base"));
        assertTrue(cf.modules().size() == 1);

        ClassLoader scl = ClassLoader.getSystemClassLoader();

        try {
            ModuleLayer.boot().defineModules(cf, mn -> new ClassLoader() { });
            assertTrue(false);
        } catch (LayerInstantiationException e) { }

        try {
            ModuleLayer.boot().defineModulesWithOneLoader(cf, scl);
            assertTrue(false);
        } catch (LayerInstantiationException e) { }

        try {
            ModuleLayer.boot().defineModulesWithManyLoaders(cf, scl);
            assertTrue(false);
        } catch (LayerInstantiationException e) { }
    }


    @DataProvider(name = "javaPackages")
    public Object[][] javaPackages() {
        return new Object[][] { { "m1", "java" }, { "m2", "java.x" } };
    }

    /**
     * Attempt to create a layer with a module containing a "java" package.
     */
    @Test(dataProvider = "javaPackages")
    public void testLayerWithJavaPackage(String mn, String pn) {
        ModuleDescriptor descriptor = newBuilder(mn).packages(Set.of(pn)).build();
        ModuleFinder finder = ModuleUtils.finderOf(descriptor);

        Configuration cf = ModuleLayer.boot()
                .configuration()
                .resolve(finder, ModuleFinder.of(), Set.of(mn));
        assertTrue(cf.modules().size() == 1);

        ClassLoader scl = ClassLoader.getSystemClassLoader();

        try {
            ModuleLayer.boot().defineModules(cf, _mn -> new ClassLoader() { });
            assertTrue(false);
        } catch (LayerInstantiationException e) { }

        try {
            ModuleLayer.boot().defineModulesWithOneLoader(cf, scl);
            assertTrue(false);
        } catch (LayerInstantiationException e) { }

        try {
            ModuleLayer.boot().defineModulesWithManyLoaders(cf, scl);
            assertTrue(false);
        } catch (LayerInstantiationException e) { }
    }


    /**
     * Attempt to create a layer with a module defined to the boot loader
     */
    @Test(expectedExceptions = { LayerInstantiationException.class })
    public void testLayerWithBootLoader() {
        ModuleDescriptor descriptor = newBuilder("m1").build();

        ModuleFinder finder = ModuleUtils.finderOf(descriptor);

        Configuration cf = ModuleLayer.boot()
            .configuration()
            .resolve(finder, ModuleFinder.of(), Set.of("m1"));
        assertTrue(cf.modules().size() == 1);

        ModuleLayer.boot().defineModules(cf, mn -> null );
    }


    /**
     * Attempt to create a layer with a module defined to the platform loader
     */
    @Test(expectedExceptions = { LayerInstantiationException.class })
    public void testLayerWithPlatformLoader() {
        ModuleDescriptor descriptor = newBuilder("m1").build();

        ModuleFinder finder = ModuleUtils.finderOf(descriptor);

        Configuration cf = ModuleLayer.boot()
                .configuration()
                .resolve(finder, ModuleFinder.of(), Set.of("m1"));
        assertTrue(cf.modules().size() == 1);

        ClassLoader cl = ClassLoader.getPlatformClassLoader();
        ModuleLayer.boot().defineModules(cf, mn -> cl );
    }


    /**
     * Parent of configuration != configuration of parent layer
     */
    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void testIncorrectParent1() {
        ModuleDescriptor descriptor1 = newBuilder("m1")
                .requires("java.base")
                .build();

        ModuleFinder finder = ModuleUtils.finderOf(descriptor1);

        Configuration parent = ModuleLayer.boot().configuration();
        Configuration cf = parent.resolve(finder, ModuleFinder.of(), Set.of("m1"));

        ClassLoader loader = new ClassLoader() { };
        ModuleLayer.empty().defineModules(cf, mn -> loader);
    }


    /**
     * Parent of configuration != configuration of parent layer
     */
    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void testIncorrectParent2() {
        ModuleDescriptor descriptor1 = newBuilder("m1").build();

        ModuleFinder finder = ModuleUtils.finderOf(descriptor1);

        Configuration cf = resolve(finder, "m1");

        ClassLoader loader = new ClassLoader() { };
        ModuleLayer.boot().defineModules(cf, mn -> loader);
    }


    // null handling

    @Test(expectedExceptions = { NullPointerException.class })
    public void testCreateWithNull1() {
        ClassLoader loader = new ClassLoader() { };
        ModuleLayer.empty().defineModules(null, mn -> loader);
    }

    @Test(expectedExceptions = { NullPointerException.class })
    public void testCreateWithNull2() {
        Configuration cf = resolve(ModuleLayer.boot().configuration(), ModuleFinder.of());
        ModuleLayer.boot().defineModules(cf, null);
    }

    @Test(expectedExceptions = { NullPointerException.class })
    public void testCreateWithNull3() {
        ClassLoader scl = ClassLoader.getSystemClassLoader();
        ModuleLayer.empty().defineModulesWithOneLoader(null, scl);
    }

    @Test(expectedExceptions = { NullPointerException.class })
    public void testCreateWithNull4() {
        ClassLoader scl = ClassLoader.getSystemClassLoader();
        ModuleLayer.empty().defineModulesWithManyLoaders(null, scl);
    }

    @Test(expectedExceptions = { NullPointerException.class })
    public void testFindModuleWithNull() {
        ModuleLayer.boot().findModule(null);
    }

    @Test(expectedExceptions = { NullPointerException.class })
    public void testFindLoaderWithNull() {
        ModuleLayer.boot().findLoader(null);
    }


    // unmodifiable collections

    @DataProvider(name = "layers")
    public Object[][] layers() {
        Configuration cf = resolve(ModuleFinder.of());
        ModuleLayer layer1 = ModuleLayer.empty().defineModulesWithOneLoader(cf, null);
        ModuleLayer layer2 = ModuleLayer.empty().defineModulesWithManyLoaders(cf, null);
        ModuleLayer layer3 = ModuleLayer.empty().defineModules(cf, mn -> null);

        // empty, boot, and custom layers
        return new Object[][] {
            { ModuleLayer.empty(), null },
            { ModuleLayer.boot(),  null },
            { layer1,              null },
            { layer2,              null },
            { layer3,              null },
        };
    }

    @Test(dataProvider = "layers",
            expectedExceptions = { UnsupportedOperationException.class })
    public void testUnmodifiableParents1(ModuleLayer layer, Object ignore) {
        layer.parents().add(ModuleLayer.empty());
    }

    @Test(dataProvider = "layers",
            expectedExceptions = { UnsupportedOperationException.class })
    public void testUnmodifiableParents2(ModuleLayer layer, Object ignore) {
        layer.parents().remove(ModuleLayer.empty());
    }

    @Test(dataProvider = "layers",
            expectedExceptions = { UnsupportedOperationException.class })
    public void testUnmodifiableModules1(ModuleLayer layer, Object ignore) {
        layer.modules().add(Object.class.getModule());
    }

    @Test(dataProvider = "layers",
            expectedExceptions = { UnsupportedOperationException.class })
    public void testUnmodifiableModules2(ModuleLayer layer, Object ignore) {
        layer.modules().remove(Object.class.getModule());
    }

    /**
     * Resolve the given modules, by name, and returns the resulting
     * Configuration.
     */
    private static Configuration resolve(Configuration cf,
                                         ModuleFinder finder,
                                         String... roots) {
        return cf.resolve(finder, ModuleFinder.of(), Set.of(roots));
    }

    private static Configuration resolve(ModuleFinder finder,
                                         String... roots) {
        return resolve(Configuration.empty(), finder, roots);
    }
}
