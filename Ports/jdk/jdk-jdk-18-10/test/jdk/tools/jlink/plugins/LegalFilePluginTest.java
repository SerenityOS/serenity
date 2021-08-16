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
 * @bug 8169925
 * @summary Validate the license files deduplicated in the image
 * @library /test/lib
 * @modules jdk.compiler
 *          jdk.jlink
 * @build jdk.test.lib.compiler.CompilerUtils
 * @run testng LegalFilePluginTest
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jdk.test.lib.compiler.CompilerUtils;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class LegalFilePluginTest {
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
    static final Path LEGAL_DIR = Paths.get("legal");
    static final Path IMAGES_DIR = Paths.get("images");

    static final Map<List<String>, Map<String,String>> LICENSES = Map.of(
        // Key is module name and requires
        // Value is a map of filename to the file content
        List.of("m1"),       Map.of("LICENSE",         "m1 LICENSE",
                                    "m1-license.txt",  "m1 license",
                                    "test-license",    "test license v1"),
        List.of("m2", "m1"), Map.of("m2-license",      "m2 license",
                                    "test-license",    "test license v1"),
        List.of("m3"),       Map.of("m3-license.md",   "m3 license",
                                    "test-license",    "test license v3"),
        List.of("m4"),       Map.of("test-license",    "test license v4")
    );

    @BeforeTest
    private void setup() throws Exception {
        List<JmodFileBuilder> builders = new ArrayList<>();
        for (Map.Entry<List<String>, Map<String,String>> e : LICENSES.entrySet()) {
            List<String> names = e.getKey();
            String mn = names.get(0);
            JmodFileBuilder builder = new JmodFileBuilder(mn);
            builders.add(builder);

            if (names.size() > 1) {
                names.subList(1, names.size())
                     .stream()
                     .forEach(builder::requires);
            }
            e.getValue().entrySet()
               .stream()
               .forEach(f -> builder.licenseFile(f.getKey(), f.getValue()));
            // generate source
            builder.writeModuleInfo();
        }

        // create jmod file
        for (JmodFileBuilder builder: builders) {
            builder.build();
        }

    }

    private String imageDir(String dir) {
        return IMAGES_DIR.resolve(dir).toString();
    }


    @DataProvider(name = "modules")
    public Object[][] jlinkoptions() {
        String m2TestLicenseContent =
            symlinkContent(Paths.get("legal", "m2", "test-license"),
                           Paths.get("legal", "m1", "test-license"),
                            "test license v1");
        // options and expected header files & man pages
        return new Object[][] {
            {   new String [] {
                    "test1",
                    "--add-modules=m1",
                },
                Map.of( "m1/LICENSE",        "m1 LICENSE",
                        "m1/m1-license.txt", "m1 license",
                        "m1/test-license",   "test license v1")
            },
            {   new String [] {
                    "test2",
                    "--add-modules=m1,m2",
                },
                Map.of( "m1/LICENSE",        "m1 LICENSE",
                        "m1/m1-license.txt", "m1 license",
                        "m1/test-license",   "test license v1",
                        "m2/m2-license",     "m2 license",
                        "m2/test-license",   m2TestLicenseContent),
            },
            {   new String [] {
                "test3",
                "--add-modules=m2,m3",
            },
                Map.of( "m1/LICENSE",        "m1 LICENSE",
                        "m1/m1-license.txt", "m1 license",
                        "m1/test-license",   "test license v1",
                        "m2/m2-license",     "m2 license",
                        "m2/test-license",   m2TestLicenseContent,
                        "m3/m3-license.md",  "m3 license",
                        "m3/test-license",   "test license v3"),
            },
        };
    }

    private static String symlinkContent(Path source, Path target, String content) {
        String osName = System.getProperty("os.name");
        if (!osName.startsWith("Windows") && MODULE_PATH.getFileSystem()
                                                        .supportedFileAttributeViews()
                                                        .contains("posix")) {
            // symlink created
            return content;
        } else {
            // tiny file is created
            Path symlink = source.getParent().relativize(target);
            return String.format("Please see %s", symlink.toString());
        }
    }

    @Test(dataProvider = "modules")
    public void test(String[] opts, Map<String,String> expectedFiles) throws Exception {
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
                  "--output", imageDir(dir))
              .forEach(options::add);

        Path image = createImage(dir, options);

        Files.walk(image.resolve("legal"), Integer.MAX_VALUE)
            .filter(p -> Files.isRegularFile(p))
            .filter(p -> p.getParent().endsWith("m1") ||
                         p.getParent().endsWith("m2") ||
                         p.getParent().endsWith("m3") ||
                         p.getParent().endsWith("m4"))
            .forEach(p -> {
                String fn = image.resolve("legal").relativize(p)
                                 .toString()
                                 .replace(File.separatorChar, '/');
                System.out.println(fn);
                if (!expectedFiles.containsKey(fn)) {
                    throw new RuntimeException(fn + " should not be in the image");
                }
                compareFileContent(p, expectedFiles.get(fn));
            });
    }

    @Test
    public void errorIfNotSameContent() {
        if (Files.notExists(MODULE_PATH)) {
            // exploded image
            return;
        }

        String dir = "test";

        String mpath = MODULE_PATH.toString() + File.pathSeparator +
                       JMODS_DIR.toString();
        List<String> options = Stream.of("--dedup-legal-notices",
                                         "error-if-not-same-content",
                                         "--module-path", mpath,
                                         "--add-modules=m3,m4",
                                         "--output", imageDir(dir))
                                     .collect(Collectors.toList());

        StringWriter writer = new StringWriter();
        PrintWriter pw = new PrintWriter(writer);
        System.out.println("jlink " + options.stream().collect(Collectors.joining(" ")));
        int rc = JLINK_TOOL.run(pw, pw,
                                options.toArray(new String[0]));
        assertTrue(rc != 0);
        assertTrue(writer.toString().trim()
                         .matches("Error:.*/m4/legal/m4/test-license .*contain different content"));
    }

    private void compareFileContent(Path file, String content) {
        try {
            byte[] bytes = Files.readAllBytes(file);
            byte[] expected = String.format("%s%n", content).getBytes();
            assertEquals(bytes, expected, String.format("%s not matched:%nfile: %s%nexpected:%s%n",
                file.toString(), new String(bytes), new String(expected)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path createImage(String outputDir, List<String> options) {
        System.out.println("jlink " + options.stream().collect(Collectors.joining(" ")));
        int rc = JLINK_TOOL.run(System.out, System.out,
                                options.toArray(new String[0]));
        assertTrue(rc == 0);

        return IMAGES_DIR.resolve(outputDir);
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
        final Set<String> requires = new HashSet<>();
        final Map<String, String> licenses = new HashMap<>();

        JmodFileBuilder(String name) throws IOException {
            this.name = name;

            Path msrc = SRC_DIR.resolve(name);
            if (Files.exists(msrc)) {
                deleteDirectory(msrc);
            }
        }

        JmodFileBuilder writeModuleInfo()throws IOException {
            Path msrc = SRC_DIR.resolve(name);
            Files.createDirectories(msrc);
            Path minfo = msrc.resolve("module-info.java");
            try (BufferedWriter bw = Files.newBufferedWriter(minfo);
                 PrintWriter writer = new PrintWriter(bw)) {
                writer.format("module %s {%n", name);
                for (String req : requires) {
                    writer.format("    requires %s;%n", req);
                }
                writer.format("}%n");
            }
            return this;
        }

        JmodFileBuilder licenseFile(String filename, String content) {
            licenses.put(filename, content);
            return this;
        }

        JmodFileBuilder requires(String name) {
            requires.add(name);
            return this;
        }

        Path build() throws IOException {
            compileModule();

            Path mdir = LEGAL_DIR.resolve(name);
            for (Map.Entry<String,String> e : licenses.entrySet()) {
                Files.createDirectories(mdir);
                String filename = e.getKey();
                String content = e.getValue();
                Path file = mdir.resolve(filename);
                try (BufferedWriter writer = Files.newBufferedWriter(file);
                     PrintWriter pw = new PrintWriter(writer)) {
                    pw.println(content);
                }
            }

            return createJmodFile();
        }


        void compileModule() throws IOException {
            Path msrc = SRC_DIR.resolve(name);
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
            if (licenses.size() > 0) {
                args.add("--legal-notices");
                args.add(LEGAL_DIR.resolve(name).toString());
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
