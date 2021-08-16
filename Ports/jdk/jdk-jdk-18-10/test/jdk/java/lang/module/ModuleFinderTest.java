/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @modules java.base/jdk.internal.module
 * @build ModuleFinderTest
 * @run testng ModuleFinderTest
 * @summary Basic tests for java.lang.module.ModuleFinder
 */

import java.io.File;
import java.io.OutputStream;
import java.lang.module.FindException;
import java.lang.module.InvalidModuleDescriptorException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

import jdk.internal.module.ModuleInfoWriter;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Test
public class ModuleFinderTest {

    private static final Path USER_DIR
        = Paths.get(System.getProperty("user.dir"));


    /**
     * Test ModuleFinder.ofSystem
     */
    public void testOfSystem() {
        ModuleFinder finder = ModuleFinder.ofSystem();

        assertTrue(finder.find("java.se").isPresent());
        assertTrue(finder.find("java.base").isPresent());
        assertFalse(finder.find("java.rhubarb").isPresent());

        Set<String> names = finder.findAll().stream()
            .map(ModuleReference::descriptor)
            .map(ModuleDescriptor::name)
            .collect(Collectors.toSet());
        assertTrue(names.contains("java.se"));
        assertTrue(names.contains("java.base"));
        assertFalse(names.contains("java.rhubarb"));
    }


    /**
     * Test ModuleFinder.of with no entries
     */
    public void testOfNoEntries() {
        ModuleFinder finder = ModuleFinder.of();
        assertTrue(finder.findAll().isEmpty());
        assertFalse(finder.find("java.rhubarb").isPresent());
    }


    /**
     * Test ModuleFinder.of with one directory of modules
     */
    public void testOfOneDirectory() throws Exception {
        Path dir = Files.createTempDirectory(USER_DIR, "mods");
        createExplodedModule(dir.resolve("m1"), "m1");
        createModularJar(dir.resolve("m2.jar"), "m2");

        ModuleFinder finder = ModuleFinder.of(dir);
        assertTrue(finder.findAll().size() == 2);
        assertTrue(finder.find("m1").isPresent());
        assertTrue(finder.find("m2").isPresent());
        assertFalse(finder.find("java.rhubarb").isPresent());
    }


    /**
     * Test ModuleFinder.of with two directories
     */
    public void testOfTwoDirectories() throws Exception {
        Path dir1 = Files.createTempDirectory(USER_DIR, "mods1");
        createExplodedModule(dir1.resolve("m1"), "m1@1.0");
        createModularJar(dir1.resolve("m2.jar"), "m2@1.0");

        Path dir2 = Files.createTempDirectory(USER_DIR, "mods2");
        createExplodedModule(dir2.resolve("m1"), "m1@2.0");
        createModularJar(dir2.resolve("m2.jar"), "m2@2.0");
        createExplodedModule(dir2.resolve("m3"), "m3");
        createModularJar(dir2.resolve("m4.jar"), "m4");

        ModuleFinder finder = ModuleFinder.of(dir1, dir2);
        assertTrue(finder.findAll().size() == 4);
        assertTrue(finder.find("m1").isPresent());
        assertTrue(finder.find("m2").isPresent());
        assertTrue(finder.find("m3").isPresent());
        assertTrue(finder.find("m4").isPresent());
        assertFalse(finder.find("java.rhubarb").isPresent());

        // check that m1@1.0 (and not m1@2.0) is found
        ModuleDescriptor m1 = finder.find("m1").get().descriptor();
        assertEquals(m1.version().get().toString(), "1.0");

        // check that m2@1.0 (and not m2@2.0) is found
        ModuleDescriptor m2 = finder.find("m2").get().descriptor();
        assertEquals(m2.version().get().toString(), "1.0");
    }


    /**
     * Test ModuleFinder.of with one JAR file
     */
    public void testOfOneJarFile() throws Exception {
        Path dir = Files.createTempDirectory(USER_DIR, "mods");
        Path jar1 = createModularJar(dir.resolve("m1.jar"), "m1");

        ModuleFinder finder = ModuleFinder.of(jar1);
        assertTrue(finder.findAll().size() == 1);
        assertTrue(finder.find("m1").isPresent());
        assertFalse(finder.find("java.rhubarb").isPresent());
    }


    /**
     * Test ModuleFinder.of with two JAR files
     */
    public void testOfTwoJarFiles() throws Exception {
        Path dir = Files.createTempDirectory(USER_DIR, "mods");

        Path jar1 = createModularJar(dir.resolve("m1.jar"), "m1");
        Path jar2 = createModularJar(dir.resolve("m2.jar"), "m2");

        ModuleFinder finder = ModuleFinder.of(jar1, jar2);
        assertTrue(finder.findAll().size() == 2);
        assertTrue(finder.find("m1").isPresent());
        assertTrue(finder.find("m2").isPresent());
        assertFalse(finder.find("java.rhubarb").isPresent());
    }


    /**
     * Test ModuleFinder.of with many JAR files
     */
    public void testOfManyJarFiles() throws Exception {
        Path dir = Files.createTempDirectory(USER_DIR, "mods");

        Path jar1 = createModularJar(dir.resolve("m1@1.0.jar"), "m1@1.0");
        Path jar2 = createModularJar(dir.resolve("m2@1.0.jar"), "m2");
        Path jar3 = createModularJar(dir.resolve("m1@2.0.jar"), "m1@2.0"); // shadowed
        Path jar4 = createModularJar(dir.resolve("m3@1.0.jar"), "m3");

        ModuleFinder finder = ModuleFinder.of(jar1, jar2, jar3, jar4);
        assertTrue(finder.findAll().size() == 3);
        assertTrue(finder.find("m1").isPresent());
        assertTrue(finder.find("m2").isPresent());
        assertTrue(finder.find("m3").isPresent());
        assertFalse(finder.find("java.rhubarb").isPresent());

        // check that m1@1.0 (and not m1@2.0) is found
        ModuleDescriptor m1 = finder.find("m1").get().descriptor();
        assertEquals(m1.version().get().toString(), "1.0");
    }


    /**
     * Test ModuleFinder.of with one exploded module.
     */
    public void testOfOneExplodedModule() throws Exception {
        Path dir = Files.createTempDirectory(USER_DIR, "mods");
        Path m1_dir = createExplodedModule(dir.resolve("m1"), "m1");

        ModuleFinder finder = ModuleFinder.of(m1_dir);
        assertTrue(finder.findAll().size() == 1);
        assertTrue(finder.find("m1").isPresent());
        assertFalse(finder.find("java.rhubarb").isPresent());
    }


    /**
     * Test ModuleFinder.of with two exploded modules.
     */
    public void testOfTwoExplodedModules() throws Exception {
        Path dir = Files.createTempDirectory(USER_DIR, "mods");
        Path m1_dir = createExplodedModule(dir.resolve("m1"), "m1");
        Path m2_dir = createExplodedModule(dir.resolve("m2"), "m2");

        ModuleFinder finder = ModuleFinder.of(m1_dir, m2_dir);
        assertTrue(finder.findAll().size() == 2);
        assertTrue(finder.find("m1").isPresent());
        assertTrue(finder.find("m2").isPresent());
        assertFalse(finder.find("java.rhubarb").isPresent());
    }


    /**
     * Test ModuleFinder.of with a mix of module directories and JAR files.
     */
    public void testOfMixDirectoriesAndJars() throws Exception {

        // directory with m1@1.0 and m2@1.0
        Path dir1 = Files.createTempDirectory(USER_DIR, "mods1");
        createExplodedModule(dir1.resolve("m1"), "m1@1.0");
        createModularJar(dir1.resolve("m2.jar"), "m2@1.0");

        // JAR files: m1@2.0, m2@2.0, m3@2.0, m4@2.0
        Path dir2 = Files.createTempDirectory(USER_DIR, "mods2");
        Path jar1 = createModularJar(dir2.resolve("m1.jar"), "m1@2.0");
        Path jar2 = createModularJar(dir2.resolve("m2.jar"), "m2@2.0");
        Path jar3 = createModularJar(dir2.resolve("m3.jar"), "m3@2.0");
        Path jar4 = createModularJar(dir2.resolve("m4.jar"), "m4@2.0");

        // directory with m3@3.0 and m4@3.0
        Path dir3 = Files.createTempDirectory(USER_DIR, "mods3");
        createExplodedModule(dir3.resolve("m3"), "m3@3.0");
        createModularJar(dir3.resolve("m4.jar"), "m4@3.0");

        // JAR files: m5 and m6
        Path dir4 = Files.createTempDirectory(USER_DIR, "mods4");
        Path jar5 = createModularJar(dir4.resolve("m5.jar"), "m5@4.0");
        Path jar6 = createModularJar(dir4.resolve("m6.jar"), "m6@4.0");


        ModuleFinder finder
            = ModuleFinder.of(dir1, jar1, jar2, jar3, jar4, dir3, jar5, jar6);
        assertTrue(finder.findAll().size() == 6);
        assertTrue(finder.find("m1").isPresent());
        assertTrue(finder.find("m2").isPresent());
        assertTrue(finder.find("m3").isPresent());
        assertTrue(finder.find("m4").isPresent());
        assertTrue(finder.find("m5").isPresent());
        assertTrue(finder.find("m6").isPresent());
        assertFalse(finder.find("java.rhubarb").isPresent());

        // m1 and m2 should be located in dir1
        ModuleDescriptor m1 = finder.find("m1").get().descriptor();
        assertEquals(m1.version().get().toString(), "1.0");
        ModuleDescriptor m2 = finder.find("m2").get().descriptor();
        assertEquals(m2.version().get().toString(), "1.0");

        // m3 and m4 should be located in JAR files
        ModuleDescriptor m3 = finder.find("m3").get().descriptor();
        assertEquals(m3.version().get().toString(), "2.0");
        ModuleDescriptor m4 = finder.find("m4").get().descriptor();
        assertEquals(m4.version().get().toString(), "2.0");

        // m5 and m6 should be located in JAR files
        ModuleDescriptor m5 = finder.find("m5").get().descriptor();
        assertEquals(m5.version().get().toString(), "4.0");
        ModuleDescriptor m6 = finder.find("m6").get().descriptor();
        assertEquals(m6.version().get().toString(), "4.0");
    }


    /**
     * Test ModuleFinder.of with a mix of module directories and exploded
     * modules.
     */
    public void testOfMixDirectoriesAndExplodedModules() throws Exception {
        // directory with m1@1.0 and m2@1.0
        Path dir1 = Files.createTempDirectory(USER_DIR, "mods1");
        createExplodedModule(dir1.resolve("m1"), "m1@1.0");
        createModularJar(dir1.resolve("m2.jar"), "m2@1.0");

        // exploded modules: m1@2.0, m2@2.0, m3@2.0, m4@2.0
        Path dir2 = Files.createTempDirectory(USER_DIR, "mods2");
        Path m1_dir = createExplodedModule(dir2.resolve("m1"), "m1@2.0");
        Path m2_dir = createExplodedModule(dir2.resolve("m2"), "m2@2.0");
        Path m3_dir = createExplodedModule(dir2.resolve("m3"), "m3@2.0");
        Path m4_dir = createExplodedModule(dir2.resolve("m4"), "m4@2.0");

        ModuleFinder finder = ModuleFinder.of(dir1, m1_dir, m2_dir, m3_dir, m4_dir);
        assertTrue(finder.findAll().size() == 4);
        assertTrue(finder.find("m1").isPresent());
        assertTrue(finder.find("m2").isPresent());
        assertTrue(finder.find("m3").isPresent());
        assertTrue(finder.find("m4").isPresent());
        assertFalse(finder.find("java.rhubarb").isPresent());

        // m1 and m2 should be located in dir1
        ModuleDescriptor m1 = finder.find("m1").get().descriptor();
        assertEquals(m1.version().get().toString(), "1.0");
        ModuleDescriptor m2 = finder.find("m2").get().descriptor();
        assertEquals(m2.version().get().toString(), "1.0");

        // m3 and m4 should be located in dir2
        ModuleDescriptor m3 = finder.find("m3").get().descriptor();
        assertEquals(m3.version().get().toString(), "2.0");
        ModuleDescriptor m4 = finder.find("m4").get().descriptor();
        assertEquals(m4.version().get().toString(), "2.0");
    }


    /**
     * Test ModuleFinder with a JAR file containing a mix of class and
     * non-class resources.
     */
    public void testOfOneJarFileWithResources() throws Exception {
        Path dir = Files.createTempDirectory(USER_DIR, "mods");
        Path jar = createModularJar(dir.resolve("m.jar"), "m",
                "LICENSE",
                "README",
                "WEB-INF/tags",
                "p/Type.class",
                "p/resources/m.properties",
                "q-/Type.class",                // not a legal package name
                "q-/resources/m/properties");

        ModuleFinder finder = ModuleFinder.of(jar);
        Optional<ModuleReference> mref = finder.find("m");
        assertTrue(mref.isPresent(), "m1 not found");

        ModuleDescriptor descriptor = mref.get().descriptor();

        assertTrue(descriptor.packages().size() == 2);
        assertTrue(descriptor.packages().contains("p"));
        assertTrue(descriptor.packages().contains("p.resources"));
    }


    /**
     * Test ModuleFinder with an exploded module containing a mix of class
     * and non-class resources
     */
    public void testOfOneExplodedModuleWithResources() throws Exception {
        Path dir = Files.createTempDirectory(USER_DIR, "mods");
        Path m_dir = createExplodedModule(dir.resolve("m"), "m",
                "LICENSE",
                "README",
                "WEB-INF/tags",
                "p/Type.class",
                "p/resources/m.properties",
                "q-/Type.class",                 // not a legal package name
                "q-/resources/m/properties");

        ModuleFinder finder = ModuleFinder.of(m_dir);
        Optional<ModuleReference> mref = finder.find("m");
        assertTrue(mref.isPresent(), "m not found");

        ModuleDescriptor descriptor = mref.get().descriptor();

        assertTrue(descriptor.packages().size() == 2);
        assertTrue(descriptor.packages().contains("p"));
        assertTrue(descriptor.packages().contains("p.resources"));
    }


    /**
     * Test ModuleFinder with a JAR file containing a .class file in the top
     * level directory.
     */
    public void testOfOneJarFileWithTopLevelClass() throws Exception {
        Path dir = Files.createTempDirectory(USER_DIR, "mods");
        Path jar = createModularJar(dir.resolve("m.jar"), "m", "Mojo.class");

        ModuleFinder finder = ModuleFinder.of(jar);
        try {
            finder.find("m");
            assertTrue(false);
        } catch (FindException e) {
            assertTrue(e.getCause() instanceof InvalidModuleDescriptorException);
            assertTrue(e.getCause().getMessage().contains("Mojo.class"));
        }

        finder = ModuleFinder.of(jar);
        try {
            finder.findAll();
            assertTrue(false);
        } catch (FindException e) {
            assertTrue(e.getCause() instanceof InvalidModuleDescriptorException);
            assertTrue(e.getCause().getMessage().contains("Mojo.class"));
        }
    }

    /**
     * Test ModuleFinder with a JAR file containing a .class file in the top
     * level directory.
     */
    public void testOfOneExplodedModuleWithTopLevelClass() throws Exception {
        Path dir = Files.createTempDirectory(USER_DIR, "mods");
        Path m_dir = createExplodedModule(dir.resolve("m"), "m", "Mojo.class");

        ModuleFinder finder = ModuleFinder.of(m_dir);
        try {
            finder.find("m");
            assertTrue(false);
        } catch (FindException e) {
            assertTrue(e.getCause() instanceof InvalidModuleDescriptorException);
            assertTrue(e.getCause().getMessage().contains("Mojo.class"));
        }

        finder = ModuleFinder.of(m_dir);
        try {
            finder.findAll();
            assertTrue(false);
        } catch (FindException e) {
            assertTrue(e.getCause() instanceof InvalidModuleDescriptorException);
            assertTrue(e.getCause().getMessage().contains("Mojo.class"));
        }
    }


    /**
     * Test ModuleFinder.of with a path to a file that does not exist.
     */
    public void testOfWithDoesNotExistEntry() throws Exception {
        Path dir1 = Files.createTempDirectory(USER_DIR, "mods1");

        Path dir2 = Files.createTempDirectory(USER_DIR, "mods2");
        createModularJar(dir2.resolve("m2.jar"), "m2@1.0");

        Files.delete(dir1);

        ModuleFinder finder = ModuleFinder.of(dir1, dir2);

        assertTrue(finder.find("m2").isPresent());
        assertTrue(finder.findAll().size() == 1);
        assertFalse(finder.find("java.rhubarb").isPresent());
    }


    /**
     * Test ModuleFinder.of with a file path to an unrecognized file type.
     */
    public void testOfWithUnrecognizedEntry() throws Exception {
        Path dir = Files.createTempDirectory(USER_DIR, "mods");
        Path mod = Files.createTempFile(dir, "m", ".junk");

        ModuleFinder finder = ModuleFinder.of(mod);
        try {
            finder.find("java.rhubarb");
            assertTrue(false);
        } catch (FindException e) {
            // expected
        }

        finder = ModuleFinder.of(mod);
        try {
            finder.findAll();
            assertTrue(false);
        } catch (FindException e) {
            // expected
        }
    }


    /**
     * Test ModuleFinder.of with a file path to a directory containing a file
     * that will not be recognized as a module.
     */
    public void testOfWithUnrecognizedEntryInDirectory1() throws Exception {
        Path dir = Files.createTempDirectory(USER_DIR, "mods");
        Files.createTempFile(dir, "m", ".junk");

        ModuleFinder finder = ModuleFinder.of(dir);
        assertFalse(finder.find("java.rhubarb").isPresent());

        finder = ModuleFinder.of(dir);
        assertTrue(finder.findAll().isEmpty());
    }


    /**
     * Test ModuleFinder.of with a file path to a directory containing a file
     * that will not be recognized as a module.
     */
    public void testOfWithUnrecognizedEntryInDirectory2() throws Exception {
        Path dir = Files.createTempDirectory(USER_DIR, "mods");
        createModularJar(dir.resolve("m1.jar"), "m1");
        Files.createTempFile(dir, "m2", ".junk");

        ModuleFinder finder = ModuleFinder.of(dir);
        assertTrue(finder.find("m1").isPresent());
        assertFalse(finder.find("m2").isPresent());

        finder = ModuleFinder.of(dir);
        assertTrue(finder.findAll().size() == 1);
    }


    /**
     * Test ModuleFinder.of with a directory that contains two
     * versions of the same module
     */
    public void testOfDuplicateModulesInDirectory() throws Exception {
        Path dir = Files.createTempDirectory(USER_DIR, "mods");
        createModularJar(dir.resolve("m1@1.0.jar"), "m1");
        createModularJar(dir.resolve("m1@2.0.jar"), "m1");

        ModuleFinder finder = ModuleFinder.of(dir);
        try {
            finder.find("m1");
            assertTrue(false);
        } catch (FindException expected) { }

        finder = ModuleFinder.of(dir);
        try {
            finder.findAll();
            assertTrue(false);
        } catch (FindException expected) { }
    }


    /**
     * Test ModuleFinder.of with a directory containing hidden files
     */
    public void testOfWithHiddenFiles() throws Exception {
        Path dir = Files.createTempDirectory(USER_DIR, "mods");
        createExplodedModule(dir.resolve("m"), "m",
                "com/.ignore",
                "com/foo/.ignore",
                "com/foo/foo.properties");

        ModuleFinder finder = ModuleFinder.of(dir);
        ModuleReference mref = finder.find("m").orElse(null);
        assertNotNull(mref);

        Set<String> expectedPackages;
        if (System.getProperty("os.name").startsWith("Windows")) {
            expectedPackages = Set.of("com", "com.foo");
        } else {
            expectedPackages = Set.of("com.foo");
        }
        assertEquals(mref.descriptor().packages(), expectedPackages);
    }


    /**
     * Test ModuleFinder.of with a truncated module-info.class
     */
    public void testOfWithTruncatedModuleInfo() throws Exception {
        Path dir = Files.createTempDirectory(USER_DIR, "mods");

        // create an empty <dir>/rhubarb/module-info.class
        Path subdir = Files.createDirectory(dir.resolve("rhubarb"));
        Files.createFile(subdir.resolve("module-info.class"));

        ModuleFinder finder = ModuleFinder.of(dir);
        try {
            finder.find("rhubarb");
            assertTrue(false);
        } catch (FindException e) {
            assertTrue(e.getCause() instanceof InvalidModuleDescriptorException);
        }

        finder = ModuleFinder.of(dir);
        try {
            finder.findAll();
            assertTrue(false);
        } catch (FindException e) {
            assertTrue(e.getCause() instanceof InvalidModuleDescriptorException);
        }
    }


    /**
     * Test ModuleFinder.compose with no module finders
     */
    public void testComposeOfNone() throws Exception {
        ModuleFinder finder = ModuleFinder.of();
        assertTrue(finder.findAll().isEmpty());
        assertFalse(finder.find("java.rhubarb").isPresent());
    }


    /**
     * Test ModuleFinder.compose with one module finder
     */
    public void testComposeOfOne() throws Exception {
        Path dir = Files.createTempDirectory(USER_DIR, "mods");
        createModularJar(dir.resolve("m1.jar"), "m1");
        createModularJar(dir.resolve("m2.jar"), "m2");

        ModuleFinder finder1 = ModuleFinder.of(dir);

        ModuleFinder finder = ModuleFinder.compose(finder1);
        assertTrue(finder.findAll().size() == 2);
        assertTrue(finder.find("m1").isPresent());
        assertTrue(finder.find("m2").isPresent());
        assertFalse(finder.find("java.rhubarb").isPresent());
    }


    /**
     * Test ModuleFinder.compose with two module finders
     */
    public void testComposeOfTwo() throws Exception {
        Path dir1 = Files.createTempDirectory(USER_DIR, "mods1");
        createModularJar(dir1.resolve("m1.jar"), "m1@1.0");
        createModularJar(dir1.resolve("m2.jar"), "m2@1.0");

        Path dir2 = Files.createTempDirectory(USER_DIR, "mods2");
        createModularJar(dir2.resolve("m1.jar"), "m1@2.0");
        createModularJar(dir2.resolve("m2.jar"), "m2@2.0");
        createModularJar(dir2.resolve("m3.jar"), "m3");
        createModularJar(dir2.resolve("m4.jar"), "m4");

        ModuleFinder finder1 = ModuleFinder.of(dir1);
        ModuleFinder finder2 = ModuleFinder.of(dir2);

        ModuleFinder finder = ModuleFinder.compose(finder1, finder2);
        assertTrue(finder.findAll().size() == 4);
        assertTrue(finder.find("m1").isPresent());
        assertTrue(finder.find("m2").isPresent());
        assertTrue(finder.find("m3").isPresent());
        assertTrue(finder.find("m4").isPresent());
        assertFalse(finder.find("java.rhubarb").isPresent());

        // check that m1@1.0 is found
        ModuleDescriptor m1 = finder.find("m1").get().descriptor();
        assertEquals(m1.version().get().toString(), "1.0");

        // check that m2@1.0 is found
        ModuleDescriptor m2 = finder.find("m2").get().descriptor();
        assertEquals(m2.version().get().toString(), "1.0");
    }


    /**
     * Test ModuleFinder.compose with three module finders
     */
    public void testComposeOfThree() throws Exception {
        Path dir1 = Files.createTempDirectory(USER_DIR, "mods1");
        createModularJar(dir1.resolve("m1.jar"), "m1@1.0");
        createModularJar(dir1.resolve("m2.jar"), "m2@1.0");

        Path dir2 = Files.createTempDirectory(USER_DIR, "mods2");
        createModularJar(dir2.resolve("m1.jar"), "m1@2.0");
        createModularJar(dir2.resolve("m2.jar"), "m2@2.0");
        createModularJar(dir2.resolve("m3.jar"), "m3@2.0");
        createModularJar(dir2.resolve("m4.jar"), "m4@2.0");

        Path dir3 = Files.createTempDirectory(USER_DIR, "mods3");
        createModularJar(dir3.resolve("m3.jar"), "m3@3.0");
        createModularJar(dir3.resolve("m4.jar"), "m4@3.0");
        createModularJar(dir3.resolve("m5.jar"), "m5");
        createModularJar(dir3.resolve("m6.jar"), "m6");

        ModuleFinder finder1 = ModuleFinder.of(dir1);
        ModuleFinder finder2 = ModuleFinder.of(dir2);
        ModuleFinder finder3 = ModuleFinder.of(dir3);

        ModuleFinder finder = ModuleFinder.compose(finder1, finder2, finder3);
        assertTrue(finder.findAll().size() == 6);
        assertTrue(finder.find("m1").isPresent());
        assertTrue(finder.find("m2").isPresent());
        assertTrue(finder.find("m3").isPresent());
        assertTrue(finder.find("m4").isPresent());
        assertTrue(finder.find("m5").isPresent());
        assertTrue(finder.find("m6").isPresent());
        assertFalse(finder.find("java.rhubarb").isPresent());

        // check that m1@1.0 is found
        ModuleDescriptor m1 = finder.find("m1").get().descriptor();
        assertEquals(m1.version().get().toString(), "1.0");

        // check that m2@1.0 is found
        ModuleDescriptor m2 = finder.find("m2").get().descriptor();
        assertEquals(m2.version().get().toString(), "1.0");

        // check that m3@2.0 is found
        ModuleDescriptor m3 = finder.find("m3").get().descriptor();
        assertEquals(m3.version().get().toString(), "2.0");

        // check that m4@2.0 is found
        ModuleDescriptor m4 = finder.find("m4").get().descriptor();
        assertEquals(m4.version().get().toString(), "2.0");
    }


    /**
     * Test null handling
     */
    public void testNulls() {

        // ofSystem
        try {
            ModuleFinder.ofSystem().find(null);
            assertTrue(false);
        } catch (NullPointerException expected) { }

        // of
        Path dir = Paths.get("d");
        try {
            ModuleFinder.of().find(null);
            assertTrue(false);
        } catch (NullPointerException expected) { }
        try {
            ModuleFinder.of((Path)null);
            assertTrue(false);
        } catch (NullPointerException expected) { }
        try {
            ModuleFinder.of((Path[])null);
            assertTrue(false);
        } catch (NullPointerException expected) { }
        try {
            ModuleFinder.of(dir, null);
            assertTrue(false);
        } catch (NullPointerException expected) { }
        try {
            ModuleFinder.of(null, dir);
            assertTrue(false);
        } catch (NullPointerException expected) { }

        // compose
        ModuleFinder finder = ModuleFinder.of();
        try {
            ModuleFinder.compose((ModuleFinder)null);
            assertTrue(false);
        } catch (NullPointerException expected) { }
        try {
            ModuleFinder.compose((ModuleFinder[])null);
            assertTrue(false);
        } catch (NullPointerException expected) { }
        try {
            ModuleFinder.compose(finder, null);
            assertTrue(false);
        } catch (NullPointerException expected) { }
        try {
            ModuleFinder.compose(null, finder);
            assertTrue(false);
        } catch (NullPointerException expected) { }

    }


    /**
     * Parses a string of the form {@code name[@version]} and returns a
     * ModuleDescriptor with that name and version. The ModuleDescriptor
     * will have a requires on java.base.
     */
    static ModuleDescriptor newModuleDescriptor(String mid) {
        String mn;
        String vs;
        int i = mid.indexOf("@");
        if (i == -1) {
            mn = mid;
            vs = null;
        } else {
            mn = mid.substring(0, i);
            vs = mid.substring(i+1);
        }
        ModuleDescriptor.Builder builder
            = ModuleDescriptor.newModule(mn).requires("java.base");
        if (vs != null)
            builder.version(vs);
        return builder.build();
    }

    /**
     * Creates an exploded module in the given directory and containing a
     * module descriptor with the given module name/version.
     */
    static Path createExplodedModule(Path dir, String mid, String... entries)
        throws Exception
    {
        ModuleDescriptor descriptor = newModuleDescriptor(mid);
        Files.createDirectories(dir);
        Path mi = dir.resolve("module-info.class");
        try (OutputStream out = Files.newOutputStream(mi)) {
            ModuleInfoWriter.write(descriptor, out);
        }

        for (String entry : entries) {
            Path file = dir.resolve(entry.replace('/', File.separatorChar));
            Files.createDirectories(file.getParent());
            Files.createFile(file);
        }

        return dir;
    }

    /**
     * Creates a JAR file with the given file path and containing a module
     * descriptor with the given module name/version.
     */
    static Path createModularJar(Path file, String mid, String ... entries)
        throws Exception
    {
        ModuleDescriptor descriptor = newModuleDescriptor(mid);
        try (OutputStream out = Files.newOutputStream(file)) {
            try (JarOutputStream jos = new JarOutputStream(out)) {

                JarEntry je = new JarEntry("module-info.class");
                jos.putNextEntry(je);
                ModuleInfoWriter.write(descriptor, jos);
                jos.closeEntry();

                for (String entry : entries) {
                    je = new JarEntry(entry);
                    jos.putNextEntry(je);
                    jos.closeEntry();
                }
            }

        }
        return file;
    }

}

