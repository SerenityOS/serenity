/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @modules java.scripting
 * @library modules /test/lib
 * @build bananascript/*
 * @build jdk.test.lib.util.JarUtils
 * @compile classpath/pearscript/org/pear/PearScriptEngineFactory.java
 *          classpath/pearscript/org/pear/PearScript.java
 * @run testng/othervm ModulesTest
 * @summary Basic test for ServiceLoader with a provider deployed as a module.
 */

import java.io.File;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.script.ScriptEngineFactory;

import jdk.test.lib.util.JarUtils;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeTest;
import static org.testng.Assert.*;

/**
 * Basic test for ServiceLoader. The test make use of two service providers:
 * 1. BananaScriptEngine - a ScriptEngineFactory deployed as a module on the
 *    module path. It implementations a singleton via the public static
 *    provider method.
 * 2. PearScriptEngine - a ScriptEngineFactory deployed on the class path
 *    with a service configuration file.
 */

public class ModulesTest {

    // Copy the services configuration file for "pearscript" into place.
    @BeforeTest
    public void setup() throws Exception {
        Path src = Paths.get(System.getProperty("test.src"));
        Path classes = Paths.get(System.getProperty("test.classes"));
        String st = ScriptEngineFactory.class.getName();
        Path config = Paths.get("META-INF", "services", st);
        Path source = src.resolve("classpath").resolve("pearscript").resolve(config);
        Path target = classes.resolve(config);
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Basic test of iterator() to ensure that providers located as modules
     * and on the class path are found.
     */
    @Test
    public void testIterator() {
        ServiceLoader<ScriptEngineFactory> loader
            = ServiceLoader.load(ScriptEngineFactory.class);
        Set<String> names = collectAll(loader)
                .stream()
                .map(ScriptEngineFactory::getEngineName)
                .collect(Collectors.toSet());
        assertTrue(names.contains("BananaScriptEngine"));
        assertTrue(names.contains("PearScriptEngine"));
    }

    /**
     * Basic test of iterator() to test iteration order. Providers deployed
     * as named modules should be found before providers deployed on the class
     * path.
     */
    @Test
    public void testIteratorOrder() {
        ServiceLoader<ScriptEngineFactory> loader
            = ServiceLoader.load(ScriptEngineFactory.class);
        boolean foundUnnamed = false;
        for (ScriptEngineFactory factory : collectAll(loader)) {
            if (factory.getClass().getModule().isNamed()) {
                if (foundUnnamed) {
                    assertTrue(false, "Named module element after unnamed");
                }
            } else {
                foundUnnamed = true;
            }
        }
    }

    /**
     * Basic test of Provider::type
     */
    @Test
    public void testProviderType() {
        Set<String> types = ServiceLoader.load(ScriptEngineFactory.class)
                .stream()
                .map(Provider::type)
                .map(Class::getName)
                .collect(Collectors.toSet());
        assertTrue(types.contains("org.banana.BananaScriptEngineFactory"));
        assertTrue(types.contains("org.pear.PearScriptEngineFactory"));
    }

    /**
     * Basic test of Provider::get
     */
    @Test
    public void testProviderGet() {
        Set<String> names = ServiceLoader.load(ScriptEngineFactory.class)
                .stream()
                .map(Provider::get)
                .map(ScriptEngineFactory::getEngineName)
                .collect(Collectors.toSet());
        assertTrue(names.contains("BananaScriptEngine"));
        assertTrue(names.contains("PearScriptEngine"));
    }

    /**
     * Basic test of the public static provider method. BananaScriptEngine
     * defines a provider method that returns the same instance.
     */
    @Test
    public void testSingleton() {
        Optional<Provider<ScriptEngineFactory>> oprovider
            = ServiceLoader.load(ScriptEngineFactory.class)
                .stream()
                .filter(p -> p.type().getName().equals("org.banana.BananaScriptEngineFactory"))
                .findFirst();
        assertTrue(oprovider.isPresent());
        Provider<ScriptEngineFactory> provider = oprovider.get();

        // invoke Provider::get twice
        ScriptEngineFactory factory1 = provider.get();
        ScriptEngineFactory factory2 = provider.get();
        assertTrue(factory1 == factory2);
    }

    /**
     * Basic test of stream() to ensure that elements for providers in named
     * modules come before elements for providers in unnamed modules.
     */
    @Test
    public void testStreamOrder() {
        List<Class<?>> types = ServiceLoader.load(ScriptEngineFactory.class)
                .stream()
                .map(Provider::type)
                .collect(Collectors.toList());

        boolean foundUnnamed = false;
        for (Class<?> factoryClass : types) {
            if (factoryClass.getModule().isNamed()) {
                if (foundUnnamed) {
                    assertTrue(false, "Named module element after unnamed");
                }
            } else {
                foundUnnamed = true;
            }
        }
    }

    /**
     * Basic test of ServiceLoader.findFirst()
     */
    @Test
    public void testFindFirst() {
        Optional<ScriptEngineFactory> ofactory
            = ServiceLoader.load(ScriptEngineFactory.class).findFirst();
        assertTrue(ofactory.isPresent());
        ScriptEngineFactory factory = ofactory.get();
        assertTrue(factory.getClass().getModule().isNamed());

        class S { }
        assertFalse(ServiceLoader.load(S.class).findFirst().isPresent());
    }

    /**
     * Basic test ServiceLoader.load specifying the platform class loader.
     * The providers on the module path and class path should not be located.
     */
    @Test
    public void testWithPlatformClassLoader() {
        ClassLoader pcl = ClassLoader.getPlatformClassLoader();

        // iterator
        ServiceLoader<ScriptEngineFactory> loader
            = ServiceLoader.load(ScriptEngineFactory.class, pcl);
        Set<String> names = collectAll(loader)
                .stream()
                .map(ScriptEngineFactory::getEngineName)
                .collect(Collectors.toSet());
        assertFalse(names.contains("BananaScriptEngine"));
        assertFalse(names.contains("PearScriptEngine"));

        // stream
        names = ServiceLoader.load(ScriptEngineFactory.class, pcl)
                .stream()
                .map(Provider::get)
                .map(ScriptEngineFactory::getEngineName)
                .collect(Collectors.toSet());
        assertFalse(names.contains("BananaScriptEngine"));
        assertFalse(names.contains("PearScriptEngine"));
    }

    /**
     * Basic test of ServiceLoader.load where the service provider module is an
     * automatic module.
     */
    @Test
    public void testWithAutomaticModule() throws Exception {
        Path here = Paths.get("");
        Path jar = Files.createTempDirectory(here, "lib").resolve("pearscript.jar");
        Path classes = Paths.get(System.getProperty("test.classes"));

        JarUtils.createJarFile(jar, classes, "META-INF", "org");

        ModuleFinder finder = ModuleFinder.of(jar);
        ModuleLayer bootLayer = ModuleLayer.boot();
        Configuration parent = bootLayer.configuration();
        Configuration cf = parent.resolveAndBind(finder, ModuleFinder.of(), Set.of());
        assertTrue(cf.modules().size() == 1);

        ClassLoader scl = ClassLoader.getSystemClassLoader();
        ModuleLayer layer = bootLayer.defineModulesWithOneLoader(cf, scl);
        assertTrue(layer.modules().size() == 1);

        ClassLoader loader = layer.findLoader("pearscript");
        ScriptEngineFactory factory;

        // load using the class loader as context
        factory = ServiceLoader.load(ScriptEngineFactory.class, loader)
                .findFirst()
                .orElse(null);
        assertNotNull(factory);
        assertTrue(factory.getClass().getClassLoader() == loader);

        // load using the layer as context
        factory = ServiceLoader.load(layer, ScriptEngineFactory.class)
                .findFirst()
                .orElse(null);
        assertNotNull(factory);
        assertTrue(factory.getClass().getClassLoader() == loader);
    }

    /**
     * Basic test of ServiceLoader.load, using the class loader for
     * a module in a custom layer as the context.
     */
    @Test
    public void testWithCustomLayer1() {
        ModuleLayer layer = createCustomLayer("bananascript");

        ClassLoader loader = layer.findLoader("bananascript");
        List<ScriptEngineFactory> providers
            = collectAll(ServiceLoader.load(ScriptEngineFactory.class, loader));

        // should have at least 2 x bananascript + pearscript
        assertTrue(providers.size() >= 3);

        // first element should be the provider in the custom layer
        ScriptEngineFactory factory = providers.get(0);
        assertTrue(factory.getClass().getClassLoader() == loader);
        assertTrue(factory.getClass().getModule().getLayer() == layer);
        assertTrue(factory.getEngineName().equals("BananaScriptEngine"));

        // remainder should be the boot layer
        providers.remove(0);
        Set<String> names = providers.stream()
                .map(ScriptEngineFactory::getEngineName)
                .collect(Collectors.toSet());
        assertTrue(names.contains("BananaScriptEngine"));
        assertTrue(names.contains("PearScriptEngine"));
    }

    /**
     * Basic test of ServiceLoader.load using a custom Layer as the context.
     */
    @Test
    public void testWithCustomLayer2() {
        ModuleLayer layer = createCustomLayer("bananascript");

        List<ScriptEngineFactory> factories
            = collectAll(ServiceLoader.load(layer, ScriptEngineFactory.class));

        // should have at least 2 x bananascript
        assertTrue(factories.size() >= 2);

        // first element should be the provider in the custom layer
        ScriptEngineFactory factory = factories.get(0);
        assertTrue(factory.getClass().getModule().getLayer() == layer);
        assertTrue(factory.getEngineName().equals("BananaScriptEngine"));

        // remainder should be the boot layer
        factories.remove(0);
        Set<String> names = factories.stream()
                .map(ScriptEngineFactory::getEngineName)
                .collect(Collectors.toSet());
        assertTrue(names.contains("BananaScriptEngine"));
        assertFalse(names.contains("PearScriptEngine"));
    }

    /**
     * Basic test of ServiceLoader.load with a tree of layers.
     *
     * Test scenario:
     * - boot layer contains "bananascript", maybe other script engines
     * - layer1, with boot layer as parent, contains "bananascript"
     * - layer2, with boot layer as parent, contains "bananascript"
     * - layer3, with layer1 ad layer as parents, contains "bananascript"
     *
     * ServiceLoader should locate all 4 script engine factories in DFS order.
     */
    @Test
    public void testWithCustomLayer3() {
        ModuleLayer bootLayer = ModuleLayer.boot();
        Configuration cf0 = bootLayer.configuration();

        // boot layer should contain "bananascript"
        List<ScriptEngineFactory> factories
            = collectAll(ServiceLoader.load(bootLayer, ScriptEngineFactory.class));
        int countInBootLayer = factories.size();
        assertTrue(countInBootLayer >= 1);
        assertTrue(factories.stream()
                .map(p -> p.getEngineName())
                .filter("BananaScriptEngine"::equals)
                .findAny()
                .isPresent());

        ClassLoader scl = ClassLoader.getSystemClassLoader();
        ModuleFinder finder = ModuleFinder.of(testModulePath());

        // layer1
        Configuration cf1 = cf0.resolveAndBind(finder, ModuleFinder.of(), Set.of());
        ModuleLayer layer1 = bootLayer.defineModulesWithOneLoader(cf1, scl);
        assertTrue(layer1.modules().size() == 1);

        // layer2
        Configuration cf2 = cf0.resolveAndBind(finder, ModuleFinder.of(), Set.of());
        ModuleLayer layer2 = bootLayer.defineModulesWithOneLoader(cf2, scl);
        assertTrue(layer2.modules().size() == 1);

        // layer3 with layer1 and layer2 as parents
        Configuration cf3 = Configuration.resolveAndBind(finder,
                List.of(cf1, cf2),
                ModuleFinder.of(),
                Set.of());
        ModuleLayer layer3
            = ModuleLayer.defineModulesWithOneLoader(cf3, List.of(layer1, layer2), scl).layer();
        assertTrue(layer3.modules().size() == 1);


        // class loaders
        ClassLoader loader1 = layer1.findLoader("bananascript");
        ClassLoader loader2 = layer2.findLoader("bananascript");
        ClassLoader loader3 = layer3.findLoader("bananascript");
        assertTrue(loader1 != loader2);
        assertTrue(loader1 != loader3);
        assertTrue(loader2 != loader3);

        // load all factories with layer3 as the context
        factories = collectAll(ServiceLoader.load(layer3, ScriptEngineFactory.class));
        int count = factories.size();
        assertTrue(count == countInBootLayer + 3);

        // the ordering should be layer3, layer1, boot layer, layer2

        ScriptEngineFactory factory = factories.get(0);
        assertTrue(factory.getClass().getModule().getLayer() == layer3);
        assertTrue(factory.getClass().getClassLoader() == loader3);
        assertTrue(factory.getEngineName().equals("BananaScriptEngine"));

        factory = factories.get(1);
        assertTrue(factory.getClass().getModule().getLayer() == layer1);
        assertTrue(factory.getClass().getClassLoader() == loader1);
        assertTrue(factory.getEngineName().equals("BananaScriptEngine"));

        // boot layer "bananascript" and maybe other factories
        int last = count -1;
        boolean found = false;
        for (int i=2; i<last; i++) {
            factory = factories.get(i);
            assertTrue(factory.getClass().getModule().getLayer() == bootLayer);
            if (factory.getEngineName().equals("BananaScriptEngine")) {
                assertFalse(found);
                found = true;
            }
        }
        assertTrue(found);

        factory = factories.get(last);
        assertTrue(factory.getClass().getModule().getLayer() == layer2);
        assertTrue(factory.getClass().getClassLoader() == loader2);
        assertTrue(factory.getEngineName().equals("BananaScriptEngine"));
    }


    // -- nulls --

    @Test(expectedExceptions = { NullPointerException.class })
    public void testLoadNull1() {
        ServiceLoader.load(null);
    }

    @Test(expectedExceptions = { NullPointerException.class })
    public void testLoadNull2() {
        ServiceLoader.load((Class<?>) null, ClassLoader.getSystemClassLoader());
    }

    @Test(expectedExceptions = { NullPointerException.class })
    public void testLoadNull3() {
        class S { }
        ServiceLoader.load((ModuleLayer) null, S.class);
    }

    @Test(expectedExceptions = { NullPointerException.class })
    public void testLoadNull4() {
        ServiceLoader.load(ModuleLayer.empty(), null);
    }

    @Test(expectedExceptions = { NullPointerException.class })
    public void testLoadNull5() {
        ServiceLoader.loadInstalled(null);
    }

    /**
     * Create a custom layer by resolving the given module names. The modules
     * are located on the test module path ({@code ${test.module.path}}).
     */
    private ModuleLayer createCustomLayer(String... modules) {
        ModuleFinder finder = ModuleFinder.of(testModulePath());
        Set<String> roots = new HashSet<>();
        Collections.addAll(roots, modules);
        ModuleLayer bootLayer = ModuleLayer.boot();
        Configuration parent = bootLayer.configuration();
        Configuration cf = parent.resolve(finder, ModuleFinder.of(), roots);
        ClassLoader scl = ClassLoader.getSystemClassLoader();
        ModuleLayer layer = bootLayer.defineModulesWithOneLoader(cf, scl);
        assertTrue(layer.modules().size() == 1);
        return layer;
    }

    private Path[] testModulePath() {
        String mp = System.getProperty("test.module.path");
        return Stream.of(mp.split(File.pathSeparator))
                .map(Paths::get)
                .toArray(Path[]::new);
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

