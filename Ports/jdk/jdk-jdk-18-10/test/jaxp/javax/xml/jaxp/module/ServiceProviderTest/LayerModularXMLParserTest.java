/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.lang.ClassLoader;
import java.lang.String;
import java.lang.System;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.Set;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import jdk.test.lib.compiler.CompilerUtils;

/*
 * @test
 * @library /test/lib
 * @run testng LayerModularXMLParserTest
 * @bug 8078820 8156119
 * @summary Tests JAXP lib works with layer and TCCL
 */

@Test
public class LayerModularXMLParserTest {

    private static final String TEST_SRC = System.getProperty("test.src");

    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MOD_DIR1 = Paths.get("mod1");
    private static final Path MOD_DIR2 = Paths.get("mod2");

    /*
     * services provided by provider1
     */
    private static final String[] services1 = { "javax.xml.parsers.DocumentBuilderFactory",
            "javax.xml.parsers.SAXParserFactory", "javax.xml.stream.XMLInputFactory",
            "javax.xml.stream.XMLOutputFactory", "javax.xml.transform.TransformerFactory",
            "javax.xml.validation.SchemaFactory", "javax.xml.xpath.XPathFactory" };

    /*
     * services provided by provider2
     */
    private static final String[] services2 = { "javax.xml.datatype.DatatypeFactory",
            "javax.xml.stream.XMLEventFactory", "org.xml.sax.XMLReader" };

    /*
     * Compiles all modules used by the test
     */
    @BeforeTest
    public void compileAll() throws Exception {
        assertTrue(CompilerUtils.compile(SRC_DIR.resolve("xmlprovider1"), MOD_DIR1.resolve("xmlprovider1")));
        assertTrue(CompilerUtils.compile(SRC_DIR.resolve("xmlprovider2"), MOD_DIR2.resolve("xmlprovider2")));
        assertTrue(CompilerUtils.compile(SRC_DIR.resolve("test"), MOD_DIR1.resolve("test")));
        assertTrue(CompilerUtils.compile(SRC_DIR.resolve("test"), MOD_DIR2.resolve("test")));
    }

    /*
     * layer 1 is created on top of boot layer, layer1 includes module provider1.
     *
     * Instantiate each XML service, verify the services provided by provider1
     * are loaded from layer 1, the other services are loaded from boot layer
     */
    public void testOneLayer() throws Exception {
        ModuleFinder finder1 = ModuleFinder.of(MOD_DIR1);
        Configuration cf1 = ModuleLayer.boot().configuration()
                .resolveAndBind(finder1, ModuleFinder.of(), Set.of("test"));
        ClassLoader scl = ClassLoader.getSystemClassLoader();
        ModuleLayer layer1 = ModuleLayer.boot().defineModulesWithManyLoaders(cf1, scl);
        ClassLoader cl1 = layer1.findLoader("test");

        Method m = cl1.loadClass("test.XMLFactoryHelper").getMethod("instantiateXMLService", String.class);
        for (String service : services1) {
            Object o = m.invoke(null, service);
            ModuleLayer providerLayer = o.getClass().getModule().getLayer();
            assertSame(providerLayer, layer1);
        }

        for (String service : services2) {
            Object o = m.invoke(null, service);
            ModuleLayer providerLayer = o.getClass().getModule().getLayer();
            assertSame(providerLayer, ModuleLayer.boot());
        }

    }

    /*
     * layer 1 is created on top of boot layer, layer 1 includes module provider1.
     * layer 2 is created on top of layer 1, layer 2 includes module provider2.
     *
     * Instantiate each XML service, verify the services provided by provider1
     * are loaded from layer 1, the services provided by provider2 are loaded from layer 2
     */
    public void testTwoLayer() throws Exception {
        ModuleFinder finder1 = ModuleFinder.of(MOD_DIR1);
        Configuration cf1 = ModuleLayer.boot().configuration()
                .resolveAndBind(finder1, ModuleFinder.of(), Set.of("test"));
        ClassLoader scl = ClassLoader.getSystemClassLoader();
        ModuleLayer layer1 = ModuleLayer.boot().defineModulesWithManyLoaders(cf1, scl);

        ModuleFinder finder2 = ModuleFinder.of(MOD_DIR2);
        Configuration cf2 = cf1.resolveAndBind(finder2, ModuleFinder.of(), Set.of("test"));
        ModuleLayer layer2 = layer1.defineModulesWithOneLoader(cf2, layer1.findLoader("test"));
        ClassLoader cl2 = layer2.findLoader("test");

        Method m = cl2.loadClass("test.XMLFactoryHelper").getMethod("instantiateXMLService", String.class);
        for (String service : services1) {
            Object o = m.invoke(null, service);
            ModuleLayer providerLayer = o.getClass().getModule().getLayer();
            assertSame(providerLayer, layer1);
        }

        for (String service : services2) {
            Object o = m.invoke(null, service);
            ModuleLayer providerLayer = o.getClass().getModule().getLayer();
            assertSame(providerLayer, layer2);
        }

    }

    /*
     * layer 1 is created on top of boot layer, layer 1 includes module provider1 and provider2.
     * layer 2 is created on top of layer 1, layer 2 includes module provider2.
     *
     * Instantiate each XML service, verify the services provided by provider1
     * are loaded from layer 1, the services provided by provider2 are loaded from layer 2
     */
    public void testTwoLayerWithDuplicate() throws Exception {
        ModuleFinder finder1 = ModuleFinder.of(MOD_DIR1, MOD_DIR2);
        Configuration cf1 = ModuleLayer.boot().configuration()
                .resolveAndBind(finder1, ModuleFinder.of(), Set.of("test"));
        ClassLoader scl = ClassLoader.getSystemClassLoader();
        ModuleLayer layer1 = ModuleLayer.boot().defineModulesWithManyLoaders(cf1, scl);

        ModuleFinder finder2 = ModuleFinder.of(MOD_DIR2);
        Configuration cf2 = cf1.resolveAndBind(finder2, ModuleFinder.of(), Set.of("test"));
        ModuleLayer layer2 = layer1.defineModulesWithOneLoader(cf2, layer1.findLoader("test"));
        ClassLoader cl2 = layer2.findLoader("test");

        Method m = cl2.loadClass("test.XMLFactoryHelper").getMethod("instantiateXMLService", String.class);
        for (String service : services1) {
            Object o = m.invoke(null, service);
            ModuleLayer providerLayer = o.getClass().getModule().getLayer();
            assertSame(providerLayer, layer1);
        }

        for (String service : services2) {
            Object o = m.invoke(null, service);
            ModuleLayer providerLayer = o.getClass().getModule().getLayer();
            assertSame(providerLayer, layer2);
        }

    }
}
