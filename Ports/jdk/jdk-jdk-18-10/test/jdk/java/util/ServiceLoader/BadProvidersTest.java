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
 * @library /test/lib
 * @modules java.base/jdk.internal.org.objectweb.asm
 *          jdk.compiler
 * @build jdk.test.lib.compiler.CompilerUtils
 * @run testng/othervm BadProvidersTest
 * @summary Basic test of ServiceLoader with bad provider and bad provider
 *          factories deployed on the module path
 */

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.Set;
import java.util.stream.Collectors;

import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

import jdk.test.lib.compiler.CompilerUtils;

import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;
import static org.testng.Assert.*;

/**
 * Basic test of `provides S with PF` and `provides S with P` where the provider
 * factory or provider
 */

public class BadProvidersTest {

    private static final String TEST_SRC = System.getProperty("test.src");

    private static final Path USER_DIR   = Paths.get(System.getProperty("user.dir"));
    private static final Path SRC_DIR    = Paths.get(TEST_SRC, "modules");

    private static final Path BADFACTORIES_DIR = Paths.get(TEST_SRC, "badfactories");
    private static final Path BADPROVIDERS_DIR = Paths.get(TEST_SRC, "badproviders");

    private static final String TEST1_MODULE = "test1";
    private static final String TEST2_MODULE = "test2";

    private static final String TEST_SERVICE = "p.Service";

    /**
     * Compiles a module, returning a module path with the compiled module.
     */
    private Path compileTest(String moduleName) throws Exception {
        Path dir = Files.createTempDirectory(USER_DIR, "mods");
        Path output = Files.createDirectory(dir.resolve(moduleName));
        boolean compiled = CompilerUtils.compile(SRC_DIR.resolve(moduleName), output);
        assertTrue(compiled);
        return dir;
    }

    /**
     * Resolves a test module and loads it into its own layer. ServiceLoader
     * is then used to load all providers.
     */
    private List<Provider> loadProviders(Path mp, String moduleName) throws Exception {
        ModuleFinder finder = ModuleFinder.of(mp);

        ModuleLayer bootLayer = ModuleLayer.boot();

        Configuration cf = bootLayer.configuration()
                .resolveAndBind(finder, ModuleFinder.of(), Set.of(moduleName));

        ClassLoader scl = ClassLoader.getSystemClassLoader();

        ModuleLayer layer = ModuleLayer.boot().defineModulesWithOneLoader(cf, scl);

        Class<?> service = layer.findLoader(moduleName).loadClass(TEST_SERVICE);

        return ServiceLoader.load(layer, service)
                .stream()
                .collect(Collectors.toList());
    }

    @Test
    public void sanityTest1() throws Exception {
        Path mods = compileTest(TEST1_MODULE);
        List<Provider> list = loadProviders(mods, TEST1_MODULE);
        assertTrue(list.size() == 1);

        // the provider is a singleton, enforced by the provider factory
        Object p1 = list.get(0).get();
        Object p2 = list.get(0).get();
        assertTrue(p1 != null);
        assertTrue(p1 == p2);
    }

    @Test
    public void sanityTest2() throws Exception {
        Path mods = compileTest(TEST2_MODULE);
        List<Provider> list = loadProviders(mods, TEST2_MODULE);
        assertTrue(list.size() == 1);
        Object p = list.get(0).get();
        assertTrue(p != null);
    }


    @DataProvider(name = "badfactories")
    public Object[][] createBadFactories() {
        return new Object[][] {
                { "classnotpublic",     null },
                { "methodnotpublic",    null },
                { "badreturntype",      null },
                { "returnsnull",        null },
                { "throwsexception",    null },
        };
    }


    @Test(dataProvider = "badfactories",
          expectedExceptions = ServiceConfigurationError.class)
    public void testBadFactory(String testName, String ignore) throws Exception {
        Path mods = compileTest(TEST1_MODULE);

        // compile the bad factory
        Path source = BADFACTORIES_DIR.resolve(testName);
        Path output = Files.createTempDirectory(USER_DIR, "tmp");
        boolean compiled = CompilerUtils.compile(source, output);
        assertTrue(compiled);

        // copy the compiled class into the module
        Path classFile = Paths.get("p", "ProviderFactory.class");
        Files.copy(output.resolve(classFile),
                   mods.resolve(TEST1_MODULE).resolve(classFile),
                   StandardCopyOption.REPLACE_EXISTING);

        // load providers and instantiate each one
        loadProviders(mods, TEST1_MODULE).forEach(Provider::get);
    }


    @DataProvider(name = "badproviders")
    public Object[][] createBadProviders() {
        return new Object[][] {
                { "notpublic",          null },
                { "ctornotpublic",      null },
                { "notasubtype",        null },
                { "throwsexception",    null }
        };
    }


    @Test(dataProvider = "badproviders",
          expectedExceptions = ServiceConfigurationError.class)
    public void testBadProvider(String testName, String ignore) throws Exception {
        Path mods = compileTest(TEST2_MODULE);

        // compile the bad provider
        Path source = BADPROVIDERS_DIR.resolve(testName);
        Path output = Files.createTempDirectory(USER_DIR, "tmp");
        boolean compiled = CompilerUtils.compile(source, output);
        assertTrue(compiled);

        // copy the compiled class into the module
        Path classFile = Paths.get("p", "Provider.class");
        Files.copy(output.resolve(classFile),
                   mods.resolve(TEST2_MODULE).resolve(classFile),
                   StandardCopyOption.REPLACE_EXISTING);

        // load providers and instantiate each one
        loadProviders(mods, TEST2_MODULE).forEach(Provider::get);
    }


    /**
     * Test a service provider that defines more than one no-args
     * public static "provider" method.
     */
    @Test(expectedExceptions = ServiceConfigurationError.class)
    public void testWithTwoFactoryMethods() throws Exception {
        Path mods = compileTest(TEST1_MODULE);

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS
                                         + ClassWriter.COMPUTE_FRAMES);
        cw.visit(V9,
                ACC_PUBLIC + ACC_SUPER,
                "p/ProviderFactory",
                null,
                "java/lang/Object",
                null);

        // public static p.Service provider()
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC,
                "provider",
                "()Lp/Service;",
                null,
                null);
        mv.visitTypeInsn(NEW, "p/ProviderFactory$1");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL,
                "p/ProviderFactory$1",
                "<init>", "()V",
                false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // public static p.ProviderFactory$1 provider()
        mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC,
                "provider",
                "()Lp/ProviderFactory$1;",
                null,
                null);
        mv.visitTypeInsn(NEW, "p/ProviderFactory$1");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL,
                "p/ProviderFactory$1",
                "<init>",
                "()V",
                false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();

        // write the class bytes into the compiled module directory
        Path classFile = mods.resolve(TEST1_MODULE)
                .resolve("p")
                .resolve("ProviderFactory.class");
        Files.write(classFile, cw.toByteArray());

        // load providers and instantiate each one
        loadProviders(mods, TEST1_MODULE).forEach(Provider::get);
    }

}

