/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.module.ModuleInfoWriter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

/*
 * Launches a new Java process with a main class packed inside a module.
 */

public class TestProcessModuleLauncher extends TestProcessLauncher {

    private static final Path TEST_MODULES = USER_DIR.resolve("testmodules");
    private static final String MODULE_NAME = "module1";

    public TestProcessModuleLauncher(String className) {
        super(className);
    }

    protected String prepareLaunch(String javaExec, String pipePort) {
        try {
            prepareModule();
            return javaExec + " --module-path " + TEST_MODULES.toFile().getAbsolutePath() +
                    " -m " + MODULE_NAME + "/" + className + " -pipe.port=" + pipePort;
        } catch (IOException e) {
            throw new RuntimeException("Failed to prepare a jar file", e);
        }
    }

    private void prepareModule() throws IOException {
        TEST_MODULES.toFile().mkdirs();
        Path moduleJar = TEST_MODULES.resolve("mod1.jar");
        ModuleDescriptor md = createModuleDescriptor();
        createModuleJarFile(moduleJar, md, TEST_CLASSES_DIR, Paths.get("."));
    }

    private ModuleDescriptor createModuleDescriptor() {
        ModuleDescriptor.Builder builder
                = ModuleDescriptor.newModule(MODULE_NAME).requires("java.base");
        return builder.build();
    }

    private static void createModuleJarFile(Path jarfile, ModuleDescriptor md, Path dir, Path... files)
            throws IOException {

        Path parent = jarfile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        List<Path> entries = findAllRegularFiles(dir, files);

        try (OutputStream out = Files.newOutputStream(jarfile);
             JarOutputStream jos = new JarOutputStream(out)) {
            if (md != null) {
                JarEntry je = new JarEntry("module-info.class");
                jos.putNextEntry(je);
                ModuleInfoWriter.write(md, jos);
                jos.closeEntry();
            }

            for (Path entry : entries) {
                String name = toJarEntryName(entry);
                jos.putNextEntry(new JarEntry(name));
                Files.copy(dir.resolve(entry), jos);
                jos.closeEntry();
            }
        }
    }

    private static String toJarEntryName(Path file) {
        Path normalized = file.normalize();
        return normalized.subpath(0, normalized.getNameCount())
                .toString()
                .replace(File.separatorChar, '/');
    }

    private static List<Path> findAllRegularFiles(Path dir, Path[] files) throws IOException {
        List<Path> entries = new ArrayList<>();
        for (Path file : files) {
            try (Stream<Path> stream = Files.find(dir.resolve(file), Integer.MAX_VALUE,
                    (p, attrs) -> attrs.isRegularFile() && !p.getParent().equals(dir.resolve(".")))) {
                stream.map(dir::relativize)
                        .forEach(entries::add);
            }
        }
        return entries;
    }

    public String getModuleName() {
        return MODULE_NAME;
    }
}
