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

/*
 * @test
 * @bug 8193192
 * @library ../lib
 * @build CompilerUtils JdepsUtil JdepsRunner
 * @modules jdk.jdeps/com.sun.tools.jdeps
 * @run testng GenModuleInfo
 * @summary Tests jdeps --generate-module-info option
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class GenModuleInfo {
    private static final String MODULE_INFO = "module-info.class";
    private static final String TEST_SRC = System.getProperty("test.src");

    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MODS_DIR = Paths.get("mods");
    private static final Path LIBS_DIR = Paths.get("libs");
    private static final Path MLIBS_DIR = Paths.get("mlibs");
    private static final Path DEST_DIR = Paths.get("moduleinfosrc");
    private static final Path NEW_MODS_DIR = Paths.get("new_mods");

    // the names of the modules in this test
    public static final String UNSUPPORTED = "unsupported";
    public static final Set<String> MODULES = Set.of(
        "mI", "mII", "mIII", "provider", "test", UNSUPPORTED
    );

    @BeforeTest
    public void setup() throws Exception {
        compileAndCreateJars();
    }

    /**
     * Compile modules
     */
    public static void compileModules(Path dest) {
        assertTrue(CompilerUtils.compileModule(SRC_DIR, dest, UNSUPPORTED,
            "--add-exports", "java.base/jdk.internal.perf=" + UNSUPPORTED));
        MODULES.stream()
               .filter(mn -> !mn.equals(UNSUPPORTED))
               .forEach(mn -> assertTrue(CompilerUtils.compileModule(SRC_DIR, dest, mn)));
    }

    /**
     * Create JAR files with no module-info.class
     */
    public static void createModularJARs(Path mods, Path dest, String... modules) throws IOException {
        Files.createDirectory(dest);
        // create modular JAR
        for (String mn : modules) {
            Path root = mods.resolve(mn);
            try (Stream<Path> stream = Files.find(root, Integer.MAX_VALUE,
                        (p, attr) -> { return attr.isRegularFile(); })) {
                JdepsUtil.createJar(dest.resolve(mn + ".jar"), root, stream);
            }
        }
    }

    /**
     * Create JAR files with no module-info.class
     */
    public static List<Path> createJARFiles(Path mods, Path libs) throws IOException {
        Files.createDirectory(libs);

        for (String mn : MODULES) {
            Path root = mods.resolve(mn);
            Path msrc = SRC_DIR.resolve(mn);
            Path metaInf = msrc.resolve("META-INF");
            if (Files.exists(metaInf)) {
                try (Stream<Path> resources = Files.find(metaInf, Integer.MAX_VALUE,
                        (p, attr) -> { return attr.isRegularFile();})) {
                    resources.forEach(file -> {
                        try {
                            Path path = msrc.relativize(file);
                            Files.createDirectories(root.resolve(path).getParent());
                            Files.copy(file, root.resolve(path));
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
                }
            }
            // copy all entries except module-info.class
            try (Stream<Path> stream = Files.find(root, Integer.MAX_VALUE,
                    (p, attr) -> { return attr.isRegularFile();})) {
                Stream<Path> entries = stream.filter(f -> {
                    String fn = f.getFileName().toString();
                    if (fn.endsWith(".class")) {
                        return !fn.equals("module-info.class");
                    } else {
                        return true;
                    }
                });

                JdepsUtil.createJar(libs.resolve(mn + ".jar"), root, entries);
            }
        }

       return MODULES.stream()
                .map(mn -> LIBS_DIR.resolve(mn + ".jar"))
                .collect(Collectors.toList());
    }

    /**
     * compile the generated module-info.java
     */
    public static void compileNewGenModuleInfo(Path source, Path dest) {

        assertTrue(CompilerUtils.compileModule(source, dest, UNSUPPORTED,
            "-p", dest.toString(),
            "--add-exports", "java.base/jdk.internal.perf=" + UNSUPPORTED));

        MODULES.stream()
            .filter(mn -> !mn.equals(UNSUPPORTED))
            .forEach(mn -> assertTrue(
                CompilerUtils.compileModule(source, dest,
                                            mn, "-p", dest.toString()))
            );

    }

    public static void compileAndCreateJars() throws Exception {
        CompilerUtils.cleanDir(MODS_DIR);
        CompilerUtils.cleanDir(LIBS_DIR);

        compileModules(MODS_DIR);

        // create modular JARs except test
        createModularJARs(MODS_DIR, MLIBS_DIR, MODULES.stream().filter(mn -> !mn.equals("test"))
                                                      .toArray(String[]::new));
        // create non-modular JARs
        createJARFiles(MODS_DIR, LIBS_DIR);
    }

    @Test
    public void automaticModules() throws IOException {
        Stream<String> files = MODULES.stream()
                .map(mn -> LIBS_DIR.resolve(mn + ".jar"))
                .map(Path::toString);
        JdepsRunner.run(Stream.concat(Stream.of("-cp"), files).toArray(String[]::new));
    }

    @Test
    public void test() throws IOException {
        Path dest = DEST_DIR.resolve("case1");
        Path classes = NEW_MODS_DIR.resolve("case1");
        Files.createDirectories(dest);
        Files.createDirectories(classes);

        Stream<String> files = MODULES.stream()
                .map(mn -> LIBS_DIR.resolve(mn + ".jar"))
                .map(Path::toString);

        Stream<String> options = Stream.concat(
            Stream.of("--generate-module-info", dest.toString()), files);
        JdepsRunner.run(options.toArray(String[]::new));

        // check file exists
        MODULES.stream()
             .map(mn -> dest.resolve(mn).resolve("module-info.java"))
             .forEach(f -> assertTrue(Files.exists(f)));

        // copy classes to a temporary directory
        // and then compile new module-info.java
        copyClasses(MODS_DIR, classes);
        compileNewGenModuleInfo(dest, classes);

        for (String mn : MODULES) {
            verify(mn, classes, MODS_DIR);
        }
    }

    @Test
    public void withModulePath() throws IOException {
        Path dest = DEST_DIR.resolve("case2");
        Path classes = NEW_MODS_DIR.resolve("case2");
        Files.createDirectories(dest);
        Files.createDirectories(classes);

        JdepsRunner.run("--module-path", MLIBS_DIR.toString(),
                        "--generate-module-info", dest.toString(),
                        LIBS_DIR.resolve("test.jar").toString());

        String name = "test";
        Path gensrc = dest.resolve(name).resolve("module-info.java");
        assertTrue(Files.exists(gensrc));

        // copy classes to a temporary directory
        // and then compile new module-info.java
        copyClasses(MODS_DIR.resolve(name), classes.resolve(name));
        assertTrue(CompilerUtils.compileModule(dest, classes, name, "-p", MLIBS_DIR.toString()));

        verify(name, classes, MODS_DIR);
    }

    /**
     * Verify the dependences from the given module-info.class files
     */
    public void verify(String mn, Path mdir1, Path mdir2) throws IOException {
        Path p1 = mdir1.resolve(mn).resolve(MODULE_INFO);
        Path p2 = mdir2.resolve(mn).resolve(MODULE_INFO);
        try (InputStream in1 = Files.newInputStream(p1);
             InputStream in2 = Files.newInputStream(p2)) {
            verify(ModuleDescriptor.read(in1),
                   ModuleDescriptor.read(in2, () -> packages(mdir2.resolve(mn))));
        }
    }

    /**
     * Copy classes except the module-info.class to the destination directory
     */
    public static void copyClasses(Path from, Path dest) throws IOException {
        try (Stream<Path> stream = Files.walk(from, Integer.MAX_VALUE)) {
            stream.filter(path -> !path.getFileName().toString().equals(MODULE_INFO) &&
                path.getFileName().toString().endsWith(".class"))
                .map(path -> from.relativize(path))
                .forEach(path -> {
                    try {
                        Path newFile = dest.resolve(path);
                        Files.createDirectories(newFile.getParent());
                        Files.copy(from.resolve(path), newFile);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        }
    }

    /**
     * Verify the generated module-info.java is equivalent to module-info.class
     * compiled from source.
     */
    private void verify(ModuleDescriptor md1, ModuleDescriptor md2) {
        System.out.println("verifying: " + md1.name());
        assertEquals(md1.name(), md2.name());
        assertEquals(md1.requires(), md2.requires());
        // all packages are exported
        assertEquals(md1.exports().stream()
                                  .map(ModuleDescriptor.Exports::source)
                                  .collect(Collectors.toSet()), md2.packages());
        if (!md1.opens().isEmpty()) {
            throw new RuntimeException("unexpected opens: " +
                md1.opens().stream()
                   .map(o -> o.toString())
                   .collect(Collectors.joining(",")));
        }

        assertEquals(md1.provides(), md2.provides());
    }

    private Set<String> packages(Path dir) {
        try (Stream<Path> stream = Files.find(dir, Integer.MAX_VALUE,
                             ((path, attrs) -> attrs.isRegularFile() &&
                                               path.toString().endsWith(".class")))) {
            return stream.map(path -> toPackageName(dir.relativize(path)))
                         .filter(pkg -> pkg.length() > 0)   // module-info
                         .distinct()
                         .collect(Collectors.toSet());
        } catch (IOException x) {
            throw new UncheckedIOException(x);
        }
    }

    private String toPackageName(Path path) {
        String name = path.toString();
        assert name.endsWith(".class");
        int index = name.lastIndexOf(File.separatorChar);
        if (index != -1) {
            return name.substring(0, index).replace(File.separatorChar, '.');
        } else {
            return "";
        }
    }

}
