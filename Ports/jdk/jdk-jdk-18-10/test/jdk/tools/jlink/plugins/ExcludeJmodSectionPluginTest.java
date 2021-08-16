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
 * @summary Test --no-man-pages and --no-header-files
 * @library /test/lib
 * @modules jdk.compiler
 *          jdk.jlink
 * @build jdk.test.lib.compiler.CompilerUtils
 * @run testng ExcludeJmodSectionPluginTest
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jdk.test.lib.compiler.CompilerUtils;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class ExcludeJmodSectionPluginTest {
    static final ToolProvider JMOD_TOOL = ToolProvider.findFirst("jmod")
        .orElseThrow(() ->
            new RuntimeException("jmod tool not found")
        );

    static final ToolProvider JLINK_TOOL = ToolProvider.findFirst("jlink")
        .orElseThrow(() ->
            new RuntimeException("jlink tool not found")
        );

    static final Path MODULE_PATH = Paths.get(System.getProperty("java.home"), "jmods");
    static final Path SRC_DIR = Paths.get("src");
    static final Path MODS_DIR = Paths.get("mods");
    static final Path JMODS_DIR = Paths.get("jmods");
    static final Path MAN_DIR = Paths.get("man");
    static final Path INCLUDE_DIR = Paths.get("include");
    static final Path IMAGES_DIR = Paths.get("images");

    @BeforeTest
    private void setup() throws Exception {
        // build jmod files
        JmodFileBuilder m1 = new JmodFileBuilder("m1");
        m1.headerFile("m1a.h");
        m1.headerFile("m1b.h");
        m1.build();

        JmodFileBuilder m2 = new JmodFileBuilder("m2");
        m2.headerFile("m2.h");
        m2.manPage("tool2.1");
        m2.build();

        JmodFileBuilder m3 = new JmodFileBuilder("m3");
        m3.manPage("tool3.1");
        m3.build();
    }

    private String imageDir(String dir) {
        return IMAGES_DIR.resolve(dir).toString();
    }


    @DataProvider(name = "jlinkoptions")
    public Object[][] jlinkoptions() {
        // options and expected header files & man pages
        return new Object[][] {
            {   new String [] {
                    "test1",
                    "--exclude-files=/java.base/include/**,/java.base/man/**",
                },
                List.of("include/m1a.h",
                        "include/m1b.h",
                        "include/m2.h",
                        "man/tool2.1",
                        "man/tool3.1")
            },

            {   new String [] {
                    "test2",
                    "--no-man-pages",
                    "--no-header-files",
                },
                List.of()
            },

            {   new String[] {
                    "test3",
                    "--no-header-files",
                    "--exclude-files=/java.base/man/**"
                },
                List.of("man/tool2.1",
                        "man/tool3.1") },

            {   new String [] {
                    "test4",
                    "--no-man-pages",
                    "--exclude-files=/java.base/include/**,/m2/include/**",
                },
                List.of("include/m1a.h",
                        "include/m1b.h")
            },

            {   new String [] {
                    "test5",
                    "--no-header-files",
                    "--exclude-files=/java.base/man/**,/m2/man/**"
                },
                List.of("man/tool3.1")
            },
        };
    }

    @Test(dataProvider = "jlinkoptions")
    public void test(String[] opts, List<String> expectedFiles) throws Exception {
        if (Files.notExists(MODULE_PATH)) {
            // exploded image
            return;
        }

        String dir = opts[0];
        List<String> options = new ArrayList<>();
        for (int i = 1; i < opts.length; i++) {
            options.add(opts[i]);
        }

        String mpath = MODULE_PATH.toString() + File.pathSeparator +
                       JMODS_DIR.toString();
        Stream.of("--module-path", mpath,
                  "--add-modules", "m1,m2,m3",
                  "--output", imageDir(dir))
              .forEach(options::add);

        Path image = createImage(dir, options, expectedFiles);

        // check if any unexpected header file or man page
        Set<Path> extraFiles = Files.walk(image, Integer.MAX_VALUE)
            .filter(p -> Files.isRegularFile(p))
            .filter(p -> p.getParent().endsWith("include") ||
                         p.getParent().endsWith("man"))
            .filter(p -> {
                String fn = String.format("%s/%s",
                    p.getParent().getFileName().toString(),
                    p.getFileName().toString());
                return !expectedFiles.contains(fn);
            })
            .collect(Collectors.toSet());

        if (extraFiles.size() > 0) {
            System.out.println("Unexpected files: " + extraFiles.toString());
            assertTrue(extraFiles.isEmpty());
        }
    }

    /**
     * Test java.base's include header files
     */
    @Test
    public void testJavaBase() {
        if (Files.notExists(MODULE_PATH)) {
            // exploded image
            return;
        }
        List<String> options = List.of("--module-path",
                                       MODULE_PATH.toString(),
                                        "--add-modules", "java.base",
                                        "--output", imageDir("base"));
        createImage("base", options,
                    List.of("include/jni.h", "include/jvmti.h"));

    }

    private Path createImage(String outputDir, List<String> options,
                             List<String> expectedFiles) {
        System.out.println("jlink " + options.toString());
        int rc = JLINK_TOOL.run(System.out, System.out,
                                options.toArray(new String[0]));
        assertTrue(rc == 0);

        Path d = IMAGES_DIR.resolve(outputDir);
        for (String fn : expectedFiles) {
            Path path = d.resolve(fn);
            if (Files.notExists(path)) {
                throw new RuntimeException(path + " not found");
            }
        }
        return d;
    }

    private void deleteDirectory(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException
            {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                throws IOException
            {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Builder to create JMOD file
     */
    class JmodFileBuilder {

        final String name;
        final Set<String> manPages = new HashSet<>();
        final Set<String> headerFiles = new HashSet<>();

        JmodFileBuilder(String name) throws IOException {
            this.name = name;

            Path msrc = SRC_DIR.resolve(name);
            if (Files.exists(msrc)) {
                deleteDirectory(msrc);
            }
        }

        JmodFileBuilder manPage(String filename) {
            manPages.add(filename);
            return this;
        }

        JmodFileBuilder headerFile(String filename) {
            headerFiles.add(filename);
            return this;
        }

        Path build() throws IOException {
            compileModule();
            // create man pages
            Path mdir = MAN_DIR.resolve(name);
            for (String filename : manPages) {
                Files.createDirectories(mdir);
                Files.createFile(mdir.resolve(filename));
            }
            // create header files
            mdir = INCLUDE_DIR.resolve(name);
            for (String filename : headerFiles) {
                Files.createDirectories(mdir);
                Files.createFile(mdir.resolve(filename));
            }
            return createJmodFile();
        }

        void compileModule() throws IOException  {
            Path msrc = SRC_DIR.resolve(name);
            Files.createDirectories(msrc);
            Path minfo = msrc.resolve("module-info.java");
            try (BufferedWriter bw = Files.newBufferedWriter(minfo);
                 PrintWriter writer = new PrintWriter(bw)) {
                writer.format("module %s { }%n", name);
            }

            assertTrue(CompilerUtils.compile(msrc, MODS_DIR,
                                             "--module-source-path",
                                             SRC_DIR.toString()));
        }

        Path createJmodFile() throws IOException {
            Path mclasses = MODS_DIR.resolve(name);
            Files.createDirectories(JMODS_DIR);
            Path outfile = JMODS_DIR.resolve(name + ".jmod");
            List<String> args = new ArrayList<>();
            args.add("create");
            // add classes
            args.add("--class-path");
            args.add(mclasses.toString());
            // man pages
            if (manPages.size() > 0) {
                args.add("--man-pages");
                args.add(MAN_DIR.resolve(name).toString());
            }
            // header files
            if (headerFiles.size() > 0) {
                args.add("--header-files");
                args.add(INCLUDE_DIR.resolve(name).toString());
            }
            args.add(outfile.toString());

            if (Files.exists(outfile))
                Files.delete(outfile);

            System.out.println("jmod " +
                args.stream().collect(Collectors.joining(" ")));

            int rc = JMOD_TOOL.run(System.out, System.out,
                                   args.toArray(new String[args.size()]));
            if (rc != 0) {
                throw new AssertionError("jmod failed: rc = " + rc);
            }
            return outfile;
        }
    }
}
