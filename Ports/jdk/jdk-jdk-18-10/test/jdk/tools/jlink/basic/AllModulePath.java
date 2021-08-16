/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary jlink test of --add-module ALL-MODULE-PATH
 * @library /test/lib
 * @modules jdk.compiler
 * @build jdk.test.lib.process.ProcessTools
 *        jdk.test.lib.process.OutputAnalyzer
 *        jdk.test.lib.compiler.CompilerUtils
 * @run testng AllModulePath
 */

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.spi.ToolProvider;

import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.process.ProcessTools;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class AllModulePath {

    private final Path JMODS = Paths.get(System.getProperty("test.jdk")).resolve("jmods");
    private final Path SRC = Paths.get(System.getProperty("test.src")).resolve("src");
    private final Path MODS = Paths.get("mods");

    private final static Set<String> MODULES = Set.of("test", "m1");

    static final ToolProvider JLINK_TOOL = ToolProvider.findFirst("jlink")
        .orElseThrow(() ->
            new RuntimeException("jlink tool not found")
        );

    @BeforeClass
    public void setup() throws Throwable {
        if (Files.notExists(JMODS)) {
            return;
        }

        Files.createDirectories(MODS);

        for (String mn : MODULES) {
            Path mod = MODS.resolve(mn);
            if (!CompilerUtils.compile(SRC.resolve(mn), mod)) {
                throw new AssertionError("Compilation failure. See log.");
            }
        }
    }

    @Test
    public void testAllModulePath() throws Throwable {
        if (Files.notExists(JMODS)) {
            return;
        }

        // create custom image
        Path image = Paths.get("image");
        createImage(image, "--add-modules", "ALL-MODULE-PATH");

        Set<String> modules = new HashSet<>();
        Files.find(JMODS, 1, (Path p, BasicFileAttributes attr) ->
                                p.toString().endsWith(".jmod"))
             .map(p -> JMODS.relativize(p).toString())
             .map(n -> n.substring(0, n.length()-5))
             .forEach(modules::add);
        modules.add("m1");
        modules.add("test");
        checkModules(image, modules);
    }

    @Test
    public void testLimitModules() throws Throwable {
        if (Files.notExists(JMODS)) {
            return;
        }

        // create custom image
        Path image = Paths.get("image1");
        createImage(image,
                    "--add-modules", "ALL-MODULE-PATH",
                    "--limit-modules", "m1");

        checkModules(image, Set.of("m1", "java.base"));
    }

    @Test
    public void testAddModules() throws Throwable {
        if (Files.notExists(JMODS)) {
            return;
        }

        // create custom image
        Path image = Paths.get("image2");
        createImage(image,
                    "--add-modules", "m1,test",
                    "--add-modules", "ALL-MODULE-PATH",
                    "--limit-modules", "java.base");

        checkModules(image, Set.of("m1", "test", "java.base"));
    }

    /*
     * check the modules linked in the image
     */
    private void checkModules(Path image, Set<String> modules) throws Throwable {
        Path cmd = findTool(image, "java");

        List<String> options = new ArrayList<>();
        options.add(cmd.toString());
        options.add("-m");
        options.add("m1/p.ListModules");
        options.addAll(modules);

        ProcessBuilder pb = new ProcessBuilder(options);
        ProcessTools.executeCommand(pb)
                    .shouldHaveExitValue(0);
    }

    private Path findTool(Path image, String tool)  {
        String suffix = System.getProperty("os.name").startsWith("Windows")
                            ? ".exe" : "";

        Path cmd = image.resolve("bin").resolve(tool + suffix);
        if (Files.notExists(cmd)) {
            throw new RuntimeException(cmd + " not found");
        }
        return cmd;
    }

    private void createImage(Path image, String... options) throws IOException {
        String modulepath = JMODS.toString() + File.pathSeparator + MODS.toString();
        List<String> opts = List.of("--module-path", modulepath,
                                    "--output", image.toString());
        String[] args = Stream.concat(opts.stream(), Arrays.stream(options))
                              .toArray(String[]::new);

        System.out.println("jlink " + Arrays.stream(args).collect(Collectors.joining(" ")));
        PrintWriter pw = new PrintWriter(System.out);
        int rc = JLINK_TOOL.run(pw, pw, args);
        assertTrue(rc == 0);
    }
}
