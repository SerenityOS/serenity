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
 * @modules java.base/jdk.internal.module
 * @build MultiReleaseJarTest jdk.test.lib.util.JarUtils
 * @run testng MultiReleaseJarTest
 * @run testng/othervm -Djdk.util.jar.enableMultiRelease=false MultiReleaseJarTest
 * @summary Basic test of modular JARs as multi-release JARs
 */

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import jdk.internal.module.ModuleInfoWriter;
import jdk.test.lib.util.JarUtils;

import org.testng.annotations.Test;
import static org.testng.Assert.*;


@Test
public class MultiReleaseJarTest {

    private static final String MODULE_INFO = "module-info.class";

    private static final int VERSION = Runtime.version().major();

    // are multi-release JARs enabled?
    private static final boolean MULTI_RELEASE;
    static {
        String s = System.getProperty("jdk.util.jar.enableMultiRelease");
        MULTI_RELEASE = (s == null || Boolean.parseBoolean(s));
    }

    /**
     * Basic test of a multi-release JAR.
     */
    public void testBasic() throws Exception {
        String name = "m1";

        ModuleDescriptor descriptor = ModuleDescriptor.newModule(name)
                .requires("java.base")
                .build();

        Path jar = new JarBuilder(name)
                .moduleInfo("module-info.class", descriptor)
                .resource("p/Main.class")
                .resource("p/Helper.class")
                .resource("META-INF/versions/" + VERSION + "/p/Helper.class")
                .resource("META-INF/versions/" + VERSION + "/p/internal/Helper.class")
                .build();

        // find the module
        ModuleFinder finder = ModuleFinder.of(jar);
        Optional<ModuleReference> omref = finder.find(name);
        assertTrue((omref.isPresent()));
        ModuleReference mref = omref.get();

        // check module packages
        descriptor = mref.descriptor();
        Set<String> packages = descriptor.packages();
        assertTrue(packages.contains("p"));
        if (MULTI_RELEASE) {
            assertTrue(packages.size() == 2);
            assertTrue(packages.contains("p.internal"));
        } else {
            assertTrue(packages.size() == 1);
        }
    }

    /**
     * Test a multi-release JAR with a module-info.class in the versioned
     * section of the JAR.
     */
    public void testModuleInfoInVersionedSection() throws Exception {
        String name = "m1";

        ModuleDescriptor descriptor1 = ModuleDescriptor.newModule(name)
                .requires("java.base")
                .build();

        // module descriptor for versioned section
        ModuleDescriptor descriptor2 = ModuleDescriptor.newModule(name)
                .requires("java.base")
                .requires("jdk.unsupported")
                .build();

        Path jar = new JarBuilder(name)
                .moduleInfo(MODULE_INFO, descriptor1)
                .resource("p/Main.class")
                .resource("p/Helper.class")
                .moduleInfo("META-INF/versions/" + VERSION + "/" + MODULE_INFO, descriptor2)
                .resource("META-INF/versions/" + VERSION + "/p/Helper.class")
                .resource("META-INF/versions/" + VERSION + "/p/internal/Helper.class")
                .build();

        // find the module
        ModuleFinder finder = ModuleFinder.of(jar);
        Optional<ModuleReference> omref = finder.find(name);
        assertTrue((omref.isPresent()));
        ModuleReference mref = omref.get();

        // ensure that the right module-info.class is loaded
        ModuleDescriptor descriptor = mref.descriptor();
        assertEquals(descriptor.name(), name);
        if (MULTI_RELEASE) {
            assertEquals(descriptor.requires(), descriptor2.requires());
        } else {
            assertEquals(descriptor.requires(), descriptor1.requires());
        }
    }

    /**
     * Test multi-release JAR as an automatic module.
     */
    public void testAutomaticModule() throws Exception {
        String name = "m";

        Path jar = new JarBuilder(name)
                .resource("p/Main.class")
                .resource("p/Helper.class")
                .resource("META-INF/versions/" + VERSION + "/p/Helper.class")
                .resource("META-INF/versions/" + VERSION + "/p/internal/Helper.class")
                .build();

        // find the module
        ModuleFinder finder = ModuleFinder.of(jar);
        Optional<ModuleReference> omref = finder.find(name);
        assertTrue((omref.isPresent()));
        ModuleReference mref = omref.get();

        // check module packages
        ModuleDescriptor descriptor = mref.descriptor();
        Set<String> packages = descriptor.packages();
        if (MULTI_RELEASE) {
            assertTrue(packages.size() == 2);
            assertTrue(packages.contains("p.internal"));
        } else {
            assertTrue(packages.size() == 1);
        }
    }

    /**
     * Exercise ModuleReader on a multi-release JAR
     */
    public void testModuleReader() throws Exception {
        String name = "m1";

        ModuleDescriptor descriptor1 = ModuleDescriptor.newModule(name)
                .requires("java.base")
                .build();

        // module descriptor for versioned section
        ModuleDescriptor descriptor2 = ModuleDescriptor.newModule(name)
                .requires("java.base")
                .requires("jdk.unsupported")
                .build();

        Path jar = new JarBuilder(name)
                .moduleInfo(MODULE_INFO, descriptor1)
                .moduleInfo("META-INF/versions/" + VERSION + "/" + MODULE_INFO, descriptor2)
                .build();

        // find the module
        ModuleFinder finder = ModuleFinder.of(jar);
        Optional<ModuleReference> omref = finder.find(name);
        assertTrue((omref.isPresent()));
        ModuleReference mref = omref.get();

        ModuleDescriptor expected;
        if (MULTI_RELEASE) {
            expected = descriptor2;
        } else {
            expected = descriptor1;
        }

        // test ModuleReader by reading module-info.class resource
        try (ModuleReader reader = mref.open()) {

            // open resource
            Optional<InputStream> oin = reader.open(MODULE_INFO);
            assertTrue(oin.isPresent());
            try (InputStream in = oin.get()) {
                checkRequires(ModuleDescriptor.read(in), expected);
            }

            // read resource
            Optional<ByteBuffer> obb = reader.read(MODULE_INFO);
            assertTrue(obb.isPresent());
            ByteBuffer bb = obb.get();
            try {
                checkRequires(ModuleDescriptor.read(bb), expected);
            } finally {
                reader.release(bb);
            }

            // find resource
            Optional<URI> ouri = reader.find(MODULE_INFO);
            assertTrue(ouri.isPresent());
            URI uri = ouri.get();

            String expectedTail = "!/";
            if (MULTI_RELEASE)
                expectedTail += "META-INF/versions/" + VERSION + "/";
            expectedTail += MODULE_INFO;
            assertTrue(uri.toString().endsWith(expectedTail));

            URLConnection uc = uri.toURL().openConnection();
            uc.setUseCaches(false);
            try (InputStream in = uc.getInputStream()) {
                checkRequires(ModuleDescriptor.read(in), expected);
            }

        }
    }

    /**
     * Check that two ModuleDescriptor have the same requires
     */
    static void checkRequires(ModuleDescriptor md1, ModuleDescriptor md2) {
        assertEquals(md1.requires(), md2.requires());
    }

    /**
     * A builder of multi-release JAR files.
     */
    static class JarBuilder {
        private String name;
        private Set<String> resources = new HashSet<>();
        private Map<String, ModuleDescriptor> descriptors = new HashMap<>();

        JarBuilder(String name) {
            this.name = name;
        }

        /**
         * Adds a module-info.class to the JAR file.
         */
        JarBuilder moduleInfo(String name, ModuleDescriptor descriptor) {
            descriptors.put(name, descriptor);
            return this;
        }

        /**
         * Adds a dummy resource to the JAR file.
         */
        JarBuilder resource(String name) {
            resources.add(name);
            return this;
        }

        /**
         * Create the multi-release JAR, returning its file path.
         */
        Path build() throws Exception {
            Path dir = Files.createTempDirectory(Paths.get(""), "jar");
            List<Path> files = new ArrayList<>();

            // write the module-info.class
            for (Map.Entry<String, ModuleDescriptor> e : descriptors.entrySet()) {
                String name = e.getKey();
                ModuleDescriptor descriptor = e.getValue();
                Path mi = Paths.get(name.replace('/', File.separatorChar));
                Path parent = dir.resolve(mi).getParent();
                if (parent != null)
                    Files.createDirectories(parent);
                try (OutputStream out = Files.newOutputStream(dir.resolve(mi))) {
                    ModuleInfoWriter.write(descriptor, out);
                }
                files.add(mi);
            }

            // write the dummy resources
            for (String name : resources) {
                Path file = Paths.get(name.replace('/', File.separatorChar));
                // create dummy resource
                Path parent = dir.resolve(file).getParent();
                if (parent != null)
                    Files.createDirectories(parent);
                Files.createFile(dir.resolve(file));
                files.add(file);
            }

            Manifest man = new Manifest();
            Attributes attrs = man.getMainAttributes();
            attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
            attrs.put(Attributes.Name.MULTI_RELEASE, "true");

            Path jarfile = Paths.get(name + ".jar");
            JarUtils.createJarFile(jarfile, man, dir, files.toArray(new Path[0]));
            return jarfile;
        }
    }
}
