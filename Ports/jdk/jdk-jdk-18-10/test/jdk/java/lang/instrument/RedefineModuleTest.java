/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Basic test for redefineModule
 *
 * @build java.base/java.lang.TestProvider
 *        java.base/jdk.internal.test.TestProviderImpl1
 *        java.base/jdk.internal.test.TestProviderImpl2
 * @run shell MakeJAR3.sh RedefineModuleAgent
 * @run testng/othervm -javaagent:RedefineModuleAgent.jar RedefineModuleTest
 */

import java.lang.TestProvider;
import java.lang.instrument.Instrumentation;
import java.net.URLStreamHandler;
import java.net.spi.URLStreamHandlerProvider;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Test
public class RedefineModuleTest {

    static void redefineModule(Module module,
                               Set<Module> extraReads,
                               Map<String, Set<Module>> extraExports,
                               Map<String, Set<Module>> extraOpens,
                               Set<Class<?>> extraUses,
                               Map<Class<?>, List<Class<?>>> extraProvides) {
        RedefineModuleAgent.redefineModule(module,
                                           extraReads,
                                           extraExports,
                                           extraOpens,
                                           extraUses,
                                           extraProvides);
    }

    static boolean isModifiableModule(Module module) {
        return RedefineModuleAgent.isModifiableModule(module);
    }


    /**
     * Use redefineModule to update java.base to read java.instrument
     */
    public void testAddReads() {
        Module baseModule = Object.class.getModule();
        Module instrumentModule = Instrumentation.class.getModule();

        // pre-conditions
        assertFalse(baseModule.canRead(instrumentModule));

        // update java.base to read java.instrument
        Set<Module> extraReads = Set.of(instrumentModule);
        redefineModule(baseModule, extraReads, Map.of(), Map.of(), Set.of(), Map.of());
        assertTrue(baseModule.canRead(instrumentModule));
    }

    /**
     * Use redefineModule to update java.base to export jdk.internal.misc
     */
    public void testAddExports() {
        Module baseModule = Object.class.getModule();
        Module thisModule = this.getClass().getClassLoader().getUnnamedModule();
        String pkg = "jdk.internal.misc";

        // pre-conditions
        assertFalse(baseModule.isExported(pkg));
        assertFalse(baseModule.isExported(pkg, thisModule));

        // update java.base to export jdk.internal.misc to an unnamed module
        Map<String, Set<Module>> extraExports = Map.of(pkg, Set.of(thisModule));
        redefineModule(baseModule, Set.of(), extraExports, Map.of(), Set.of(), Map.of());
        assertFalse(baseModule.isExported(pkg));
        assertTrue(baseModule.isExported(pkg, thisModule));
        assertFalse(baseModule.isOpen(pkg));
        assertFalse(baseModule.isOpen(pkg, thisModule));
    }

    /**
     * Use redefineModule to update java.base to open jdk.internal.loader
     */
    public void testAddOpens() {
        Module baseModule = Object.class.getModule();
        Module thisModule = this.getClass().getClassLoader().getUnnamedModule();
        String pkg = "jdk.internal.loader";

        // pre-conditions
        assertFalse(baseModule.isOpen(pkg));
        assertFalse(baseModule.isOpen(pkg, thisModule));

        // update java.base to open dk.internal.loader to an unnamed module
        Map<String, Set<Module>> extraExports = Map.of(pkg, Set.of(thisModule));
        redefineModule(baseModule, Set.of(), Map.of(), extraExports, Set.of(), Map.of());
        assertFalse(baseModule.isExported(pkg));
        assertTrue(baseModule.isExported(pkg, thisModule));
        assertFalse(baseModule.isOpen(pkg));
        assertTrue(baseModule.isOpen(pkg, thisModule));
    }

    /**
     * Use redefineModule to update java.base to use TestProvider and
     * provide implementations of TestProvider.
     */
    public void testAddUsesAndProvides() throws Exception {
        Module baseModule = Object.class.getModule();
        Class<TestProvider> service = TestProvider.class;

        // pre-conditions
        assertFalse(baseModule.canUse(service));
        assertTrue(collect(ServiceLoader.load(service)).isEmpty());
        assertTrue(collect(ServiceLoader.load(ModuleLayer.boot(), service)).isEmpty());

        // update java.base to use TestProvider
        redefineModule(baseModule, Set.of(), Map.of(), Map.of(), Set.of(service), Map.of());
        assertTrue(baseModule.canUse(service));
        assertTrue(collect(ServiceLoader.load(service)).isEmpty());
        assertTrue(collect(ServiceLoader.load(ModuleLayer.boot(), service)).isEmpty());

        // update java.base to provide an implementation of TestProvider
        Class<?> type1 = Class.forName("jdk.internal.test.TestProviderImpl1");
        Map<Class<?>, List<Class<?>>> extraProvides = Map.of(service, List.of(type1));
        redefineModule(baseModule, Set.of(), Map.of(), Map.of(), Set.of(), extraProvides);

        // invoke ServiceLoader from java.base to find providers
        Set<TestProvider> providers = collect(TestProvider.providers());
        assertTrue(providers.size() == 1);
        assertTrue(containsInstanceOf(providers, type1));

        // use ServiceLoader to load implementations visible via TCCL
        providers = collect(ServiceLoader.load(service));
        assertTrue(collect(providers).size() == 1);
        assertTrue(containsInstanceOf(collect(providers), type1));

        // use ServiceLoader to load implementations in the boot layer
        providers = collect(ServiceLoader.load(ModuleLayer.boot(), service));
        assertTrue(collect(providers).size() == 1);
        assertTrue(containsInstanceOf(collect(providers), type1));

        // update java.base to provide a second implementation of TestProvider
        Class<?> type2 = Class.forName("jdk.internal.test.TestProviderImpl2");
        extraProvides = Map.of(service, List.of(type2));
        redefineModule(baseModule, Set.of(), Map.of(), Map.of(), Set.of(), extraProvides);

        // invoke ServiceLoader from java.base to find providers
        providers = collect(TestProvider.providers());
        assertTrue(providers.size() == 2);
        assertTrue(containsInstanceOf(providers, type1));
        assertTrue(containsInstanceOf(providers, type2));

        // use ServiceLoader to load implementations visible via TCCL
        providers = collect(ServiceLoader.load(service));
        assertTrue(collect(providers).size() == 2);
        assertTrue(containsInstanceOf(providers, type1));
        assertTrue(containsInstanceOf(providers, type2));

        // use ServiceLoader to load implementations in the boot layer
        providers = collect(ServiceLoader.load(ModuleLayer.boot(), service));
        assertTrue(collect(providers).size() == 2);
        assertTrue(containsInstanceOf(providers, type1));
        assertTrue(containsInstanceOf(providers, type2));
    }

    private <S> Set<S> collect(Iterable<S> sl) {
        Set<S> providers = new HashSet<>();
        sl.forEach(providers::add);
        return providers;
    }

    private boolean containsInstanceOf(Collection<?> c, Class<?> type) {
        for (Object o : c) {
            if (type.isInstance(o)) return true;
        }
        return false;
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testExportPackageToEmptySet() {
        // attempt to update java.base to export jdk.internal.misc to nobody
        Module baseModule = Object.class.getModule();
        Map<String, Set<Module>> extraExports = Map.of("jdk.internal.misc", Set.of());
        redefineModule(baseModule, Set.of(), extraExports, Map.of(), Set.of(), Map.of());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testOpenPackageToEmptySet() {
        // attempt to update java.base to open jdk.internal.misc to nobody
        Module baseModule = Object.class.getModule();
        Map<String, Set<Module>> extraOpens = Map.of("jdk.internal.misc", Set.of());
        redefineModule(baseModule, Set.of(), Map.of(), extraOpens, Set.of(), Map.of());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testProvideServiceWithEmptyList() throws Exception {
        // update java.base to provide an empty list of TestProvider
        Module baseModule = Object.class.getModule();
        Class<?> service = TestProvider.class;
        Map<Class<?>, List<Class<?>>> extraProvides = Map.of(service, List.of());
        redefineModule(baseModule, Set.of(), Map.of(), Map.of(), Set.of(), extraProvides);
    }

    /**
     * Test redefineClass by attempting to update java.base to export a package
     * that it does not contain.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testExportPackageNotInModule() {
        Module baseModule = Object.class.getModule();
        String pkg = "jdk.doesnotexist";

        // attempt to update java.base to export jdk.doesnotexist
        Map<String, Set<Module>> extraExports = Map.of(pkg, Set.of());
        redefineModule(baseModule, Set.of(), extraExports, Map.of(), Set.of(), Map.of());
    }

    /**
     * Test redefineClass by attempting to update java.base to provide a service
     * where the service provider class is not in the module.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testProvideServiceNotInModule() {
        Module baseModule = Object.class.getModule();
        class MyProvider extends URLStreamHandlerProvider {
            @Override
            public URLStreamHandler createURLStreamHandler(String protocol) {
                return null;
            }
        }

        // attempt to update java.base to provide MyProvider
        Map<Class<?>, List<Class<?>>> extraProvides
            = Map.of(URLStreamHandlerProvider.class, List.of(MyProvider.class));
        redefineModule(baseModule, Set.of(), Map.of(), Map.of(), Set.of(), extraProvides);
    }

    /**
     * Test redefineClass by attempting to update java.base to provide a
     * service where the service provider class is not a sub-type.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testProvideServiceNotSubtype() {
        Module baseModule = Object.class.getModule();

        Class<?> service = TestProvider.class;
        Class<?> impl = FileSystems.getDefault().provider().getClass();

        // attempt to update java.base to provide an implementation of TestProvider
        Map<Class<?>, List<Class<?>>> extraProvides = Map.of(service, List.of(impl));
        redefineModule(baseModule, Set.of(), Map.of(), Map.of(), Set.of(), extraProvides);
    }

    /**
     * Exercise IsModifiableModule
     */
    @Test
    public void testIsModifiableModule() {
        ClassLoader pcl = ClassLoader.getPlatformClassLoader();
        ClassLoader scl = ClassLoader.getSystemClassLoader();
        assertTrue(isModifiableModule(pcl.getUnnamedModule()));
        assertTrue(isModifiableModule(scl.getUnnamedModule()));
        assertTrue(isModifiableModule(RedefineModuleTest.class.getModule()));
        assertTrue(isModifiableModule(Object.class.getModule()));
    }

    /**
     * Test redefineClass with null
     */
    public void testNulls() {
        Module baseModule = Object.class.getModule();

        try {
            redefineModule(null, Set.of(), Map.of(), Map.of(), Set.of(), Map.of());
            assertTrue(false);
        } catch (NullPointerException e) { }

        try {
            redefineModule(baseModule, null, Map.of(), Map.of(), Set.of(), Map.of());
            assertTrue(false);
        } catch (NullPointerException e) { }

        try {
            redefineModule(baseModule, Set.of(), null, Map.of(), Set.of(), Map.of());
            assertTrue(false);
        } catch (NullPointerException e) { }

        try {
            redefineModule(baseModule, Set.of(), Map.of(), null, Set.of(), Map.of());
            assertTrue(false);
        } catch (NullPointerException e) { }

        try {
            redefineModule(baseModule, Set.of(), Map.of(), Map.of(), null, Map.of());
            assertTrue(false);
        } catch (NullPointerException e) { }

        try {
            redefineModule(baseModule, Set.of(), Map.of(), Map.of(), Set.of(), null);
            assertTrue(false);
        } catch (NullPointerException e) { }

        try {
            Set<Module> containsNull = new HashSet<>();
            containsNull.add(null);
            redefineModule(baseModule, containsNull, Map.of(), Map.of(), Set.of(), Map.of());
            assertTrue(false);
        } catch (NullPointerException e) { }

        try {
            Map<String, Set<Module>> extraExports = new HashMap<>();
            extraExports.put(null, Set.of());
            redefineModule(baseModule, Set.of(), extraExports, Map.of(), Set.of(), Map.of());
            assertTrue(false);
        } catch (NullPointerException e) { }

        try {
            Map<String, Set<Module>> extraExports = new HashMap<>();
            extraExports.put(null, Set.of());
            redefineModule(baseModule, Set.of(), Map.of(), extraExports, Set.of(), Map.of());
            assertTrue(false);
        } catch (NullPointerException e) { }

        try {
            Set<Module> containsNull = new HashSet<>();
            containsNull.add(null);
            Map<String, Set<Module>> extraExports = Map.of("java.lang", containsNull);
            redefineModule(baseModule, Set.of(), extraExports, Map.of(), Set.of(), Map.of());
            assertTrue(false);
        } catch (NullPointerException e) { }

        try {
            Set<Class<?>> containsNull = new HashSet<>();
            containsNull.add(null);
            redefineModule(baseModule, Set.of(), Map.of(), Map.of(), containsNull, Map.of());
            assertTrue(false);
        } catch (NullPointerException e) { }

        try {
            Map<Class<?>, List<Class<?>>> extraProvides = new HashMap<>();
            extraProvides.put(null, List.of());
            redefineModule(baseModule, Set.of(), Map.of(), Map.of(), Set.of(), extraProvides);
            assertTrue(false);
        } catch (NullPointerException e) { }

        try {
            List<Class<?>> containsNull = new ArrayList<>();
            containsNull.add(null);
            Map<Class<?>, List<Class<?>>> extraProvides = Map.of(TestProvider.class, containsNull);
            redefineModule(baseModule, Set.of(), Map.of(), Map.of(), Set.of(), extraProvides);
            assertTrue(false);
        } catch (NullPointerException e) { }
    }

}
