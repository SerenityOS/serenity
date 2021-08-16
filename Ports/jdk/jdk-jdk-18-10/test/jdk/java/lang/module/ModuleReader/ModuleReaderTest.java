/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @modules java.base/jdk.internal.module
 *          jdk.compiler
 *          jdk.jlink
 * @build ModuleReaderTest
 *        jdk.test.lib.compiler.CompilerUtils
 *        jdk.test.lib.util.JarUtils
 * @run testng ModuleReaderTest
 * @summary Basic tests for java.lang.module.ModuleReader
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.spi.ToolProvider;

import jdk.internal.module.ModulePath;
import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.util.JarUtils;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Test
public class ModuleReaderTest {

    private static final String TEST_SRC = System.getProperty("test.src");

    private static final Path USER_DIR   = Paths.get(System.getProperty("user.dir"));
    private static final Path SRC_DIR    = Paths.get(TEST_SRC, "src");
    private static final Path MODS_DIR   = Paths.get("mods");

    // the module name of the base module
    private static final String BASE_MODULE = "java.base";

    // the module name of the test module
    private static final String TEST_MODULE = "m";

    // resources in the base module
    private static final String[] BASE_RESOURCES = {
        "java/lang/Object.class"
    };

    // (directory) resources that may be in the base module
    private static final String[] MAYBE_BASE_RESOURCES = {
        "java",
        "java/",
        "java/lang",
        "java/lang/",
    };

    // resource names that should not be found in the base module
    private static final String[] NOT_BASE_RESOURCES = {
        "NotFound",
        "/java",
        "//java",
        "/java/lang",
        "//java/lang",
        "java//lang",
        "/java/lang/Object.class",
        "//java/lang/Object.class",
        "java/lang/Object.class/",
        "java//lang//Object.class",
        "./java/lang/Object.class",
        "java/./lang/Object.class",
        "java/lang/./Object.class",
        "../java/lang/Object.class",
        "java/../lang/Object.class",
        "java/lang/../Object.class",
    };

    // resources in test module (can't use module-info.class as a test
    // resource as it will be modified by the jmod tool)
    private static final String[] TEST_RESOURCES = {
        "p/Main.class"
    };

    // (directory) resources that may be in the test module
    private static final String[] MAYBE_TEST_RESOURCES = {
        "p",
        "p/"
    };

    // resource names that should not be found in the test module
    private static final String[] NOT_TEST_RESOURCES = {
        "NotFound",
        "/p",
        "//p",
        "/p/Main.class",
        "//p/Main.class",
        "p/Main.class/",
        "p//Main.class",
        "./p/Main.class",
        "p/./Main.class",
        "../p/Main.class",
        "p/../p/Main.class"
    };


    @BeforeTest
    public void compileTestModule() throws Exception {

        // javac -d mods/$TESTMODULE src/$TESTMODULE/**
        boolean compiled
            = CompilerUtils.compile(SRC_DIR.resolve(TEST_MODULE),
                                    MODS_DIR.resolve(TEST_MODULE));
        assertTrue(compiled, "test module did not compile");
    }


    /**
     * Test ModuleReader to module in runtime image
     */
    public void testImage() throws IOException {

        ModuleFinder finder = ModuleFinder.ofSystem();
        ModuleReference mref = finder.find(BASE_MODULE).get();
        ModuleReader reader = mref.open();

        try (reader) {

            for (String name : BASE_RESOURCES) {
                byte[] expectedBytes;
                Module baseModule = Object.class.getModule();
                try (InputStream in = baseModule.getResourceAsStream(name)) {
                    expectedBytes = in.readAllBytes();
                }

                testFind(reader, name, expectedBytes);
                testOpen(reader, name, expectedBytes);
                testRead(reader, name, expectedBytes);
                testList(reader, name);
            }

            // test resources that may be in the base module
            for (String name : MAYBE_BASE_RESOURCES) {
                Optional<URI> ouri = reader.find(name);
                ouri.ifPresent(uri -> {
                    if (name.endsWith("/"))
                        assertTrue(uri.toString().endsWith("/"));
                });
            }

            // test "not found" in java.base module
            for (String name : NOT_BASE_RESOURCES) {
                assertFalse(reader.find(name).isPresent());
                assertFalse(reader.open(name).isPresent());
                assertFalse(reader.read(name).isPresent());
            }

            // test nulls
            try {
                reader.find(null);
                assertTrue(false);
            } catch (NullPointerException expected) { }

            try {
                reader.open(null);
                assertTrue(false);
            } catch (NullPointerException expected) { }

            try {
                reader.read(null);
                assertTrue(false);
            } catch (NullPointerException expected) { }

            try {
                reader.release(null);
                assertTrue(false);
            } catch (NullPointerException expected) { }

        }

        // test closed ModuleReader
        try {
            reader.open(BASE_RESOURCES[0]);
            assertTrue(false);
        } catch (IOException expected) { }


        try {
            reader.read(BASE_RESOURCES[0]);
            assertTrue(false);
        } catch (IOException expected) { }
    }


    /**
     * Test ModuleReader to exploded module
     */
    public void testExplodedModule() throws IOException {
        test(MODS_DIR);
    }


    /**
     * Test ModuleReader to modular JAR
     */
    public void testModularJar() throws IOException {
        Path dir = Files.createTempDirectory(USER_DIR, "mlib");

        // jar cf mlib/${TESTMODULE}.jar -C mods .
        JarUtils.createJarFile(dir.resolve("m.jar"),
                               MODS_DIR.resolve(TEST_MODULE));

        test(dir);
    }


    /**
     * Test ModuleReader to JMOD
     */
    public void testJMod() throws IOException {
        Path dir = Files.createTempDirectory(USER_DIR, "mlib");

        // jmod create --class-path mods/${TESTMODULE}  mlib/${TESTMODULE}.jmod
        String cp = MODS_DIR.resolve(TEST_MODULE).toString();
        String jmod = dir.resolve("m.jmod").toString();
        String[] args = { "create", "--class-path", cp, jmod };
        ToolProvider jmodTool = ToolProvider.findFirst("jmod")
            .orElseThrow(() ->
                new RuntimeException("jmod tool not found")
            );
        assertEquals(jmodTool.run(System.out, System.out, args), 0);

        test(dir);
    }


    /**
     * The test module is found on the given module path. Open a ModuleReader
     * to the test module and test the reader.
     */
    void test(Path mp) throws IOException {

        ModuleFinder finder = ModulePath.of(Runtime.version(), true, mp);
        ModuleReference mref = finder.find(TEST_MODULE).get();
        ModuleReader reader = mref.open();

        try (reader) {

            // test resources in test module
            for (String name : TEST_RESOURCES) {
                byte[] expectedBytes
                    = Files.readAllBytes(MODS_DIR
                        .resolve(TEST_MODULE)
                        .resolve(name.replace('/', File.separatorChar)));

                testFind(reader, name, expectedBytes);
                testOpen(reader, name, expectedBytes);
                testRead(reader, name, expectedBytes);
                testList(reader, name);
            }

            // test resources that may be in the test module
            for (String name : MAYBE_TEST_RESOURCES) {
                System.out.println(name);
                Optional<URI> ouri = reader.find(name);
                ouri.ifPresent(uri -> {
                    if (name.endsWith("/"))
                        assertTrue(uri.toString().endsWith("/"));
                });
            }

            // test "not found" in test module
            for (String name : NOT_TEST_RESOURCES) {
                assertFalse(reader.find(name).isPresent());
                assertFalse(reader.open(name).isPresent());
                assertFalse(reader.read(name).isPresent());
            }

            // test nulls
            try {
                reader.find(null);
                assertTrue(false);
            } catch (NullPointerException expected) { }

            try {
                reader.open(null);
                assertTrue(false);
            } catch (NullPointerException expected) { }

            try {
                reader.read(null);
                assertTrue(false);
            } catch (NullPointerException expected) { }

            try {
                reader.release(null);
                throw new RuntimeException();
            } catch (NullPointerException expected) { }

        }

        // test closed ModuleReader
        try {
            reader.open(TEST_RESOURCES[0]);
            assertTrue(false);
        } catch (IOException expected) { }


        try {
            reader.read(TEST_RESOURCES[0]);
            assertTrue(false);
        } catch (IOException expected) { }

        try {
            reader.list();
            assertTrue(false);
        } catch (IOException expected) { }
    }

    /**
     * Test ModuleReader#find
     */
    void testFind(ModuleReader reader, String name, byte[] expectedBytes)
        throws IOException
    {
        Optional<URI> ouri = reader.find(name);
        assertTrue(ouri.isPresent());

        URL url = ouri.get().toURL();
        if (!url.getProtocol().equalsIgnoreCase("jmod")) {
            URLConnection uc = url.openConnection();
            uc.setUseCaches(false);
            try (InputStream in = uc.getInputStream()) {
                byte[] bytes = in.readAllBytes();
                assertTrue(Arrays.equals(bytes, expectedBytes));
            }
        }
    }

    /**
     * Test ModuleReader#open
     */
    void testOpen(ModuleReader reader, String name, byte[] expectedBytes)
        throws IOException
    {
        Optional<InputStream> oin = reader.open(name);
        assertTrue(oin.isPresent());

        InputStream in = oin.get();
        try (in) {
            byte[] bytes = in.readAllBytes();
            assertTrue(Arrays.equals(bytes, expectedBytes));
        }
    }

    /**
     * Test ModuleReader#read
     */
    void testRead(ModuleReader reader, String name, byte[] expectedBytes)
        throws IOException
    {
        Optional<ByteBuffer> obb = reader.read(name);
        assertTrue(obb.isPresent());

        ByteBuffer bb = obb.get();
        try {
            int rem = bb.remaining();
            assertTrue(rem == expectedBytes.length);
            byte[] bytes = new byte[rem];
            bb.get(bytes);
            assertTrue(Arrays.equals(bytes, expectedBytes));
        } finally {
            reader.release(bb);
        }
    }

    /**
     * Test ModuleReader#list
     */
    void testList(ModuleReader reader, String name) throws IOException {
        List<String> list = reader.list().collect(Collectors.toList());
        Set<String> names = new HashSet<>(list);
        assertTrue(names.size() == list.size()); // no duplicates

        assertTrue(names.contains("module-info.class"));
        assertTrue(names.contains(name));

        // all resources should be locatable via find
        for (String e : names) {
            assertTrue(reader.find(e).isPresent());
        }
    }

}
