/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8037085
 * @summary Ensures that sjavac can handle various exclusion patterns.
 *
 * @modules jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.sjavac
 *          jdk.compiler/com.sun.tools.sjavac.server
 * @library /tools/lib
 * @build Wrapper toolbox.ToolBox toolbox.Assert
 * @run main Wrapper IncludeExcludePatterns
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sun.tools.javac.main.Main.Result;

import toolbox.Assert;

public class IncludeExcludePatterns extends SjavacBase {

    final Path SRC = Paths.get("src");
    final Path BIN = Paths.get("bin");
    final Path STATE_DIR = Paths.get("state-dir");

    // An arbitrarily but sufficiently complicated source tree.
    final Path A = Paths.get("pkga/A.java");
    final Path X1 = Paths.get("pkga/subpkg/Xx.java");
    final Path Y = Paths.get("pkga/subpkg/subsubpkg/Y.java");
    final Path B = Paths.get("pkgb/B.java");
    final Path C = Paths.get("pkgc/C.java");
    final Path X2 = Paths.get("pkgc/Xx.java");

    final Path[] ALL_PATHS = {A, X1, Y, B, C, X2};

    public static void main(String[] ignore) throws Exception {
        new IncludeExcludePatterns().runTest();
    }

    public void runTest() throws IOException, ReflectiveOperationException {
        Files.createDirectories(BIN);
        Files.createDirectories(STATE_DIR);
        for (Path p : ALL_PATHS) {
            writeDummyClass(p);
        }

        // Single file
        testPattern("pkga/A.java", A);

        // Leading wild cards
        testPattern("*/A.java", A);
        testPattern("**/Xx.java", X1, X2);
        testPattern("**x.java", X1, X2);

        // Wild card in middle of path
        testPattern("pkga/*/Xx.java", X1);
        testPattern("pkga/**/Y.java", Y);

        // Trailing wild cards
        testPattern("pkga/*", A);
        testPattern("pkga/**", A, X1, Y);

        // Multiple wildcards
        testPattern("pkga/*/*/Y.java", Y);
        testPattern("**/*/**", X1, Y);

    }

    // Given "src/pkg/subpkg/A.java" this method returns "A"
    String classNameOf(Path javaFile) {
        return javaFile.getFileName()
                       .toString()
                       .replace(".java", "");
    }

    // Puts an empty (dummy) class definition in the given path.
    void writeDummyClass(Path javaFile) throws IOException {
        String pkg = javaFile.getParent().toString().replace(File.separatorChar, '.');
        String cls = javaFile.getFileName().toString().replace(".java", "");
        toolbox.writeFile(SRC.resolve(javaFile), "package " + pkg + "; class " + cls + " {}");
    }

    void testPattern(String filterArgs, Path... sourcesExpectedToBeVisible)
            throws ReflectiveOperationException, IOException {
        testFilter("-i " + filterArgs, Arrays.asList(sourcesExpectedToBeVisible));

        Set<Path> complement = new HashSet<>(Arrays.asList(ALL_PATHS));
        complement.removeAll(Arrays.asList(sourcesExpectedToBeVisible));
        testFilter("-x " + filterArgs, complement);
    }

    void testFilter(String filterArgs, Collection<Path> sourcesExpectedToBeVisible)
            throws IOException, ReflectiveOperationException {
        System.out.println("Testing filter: " + filterArgs);
        toolbox.cleanDirectory(BIN);
        toolbox.cleanDirectory(STATE_DIR);
        String args = filterArgs + " " + SRC
                + " -d " + BIN
                + " --state-dir=" + STATE_DIR;
        int rc = compile((Object[]) args.split(" "));

        // Compilation should always pass in these tests
        Assert.check(rc == Result.OK.exitCode, "Compilation failed unexpectedly.");

        // The resulting .class files should correspond to the visible source files
        Set<Path> result = allFilesInDir(BIN);
        Set<Path> expected = correspondingClassFiles(sourcesExpectedToBeVisible);
        if (!result.equals(expected)) {
            System.out.println("Result:");
            printPaths(result);
            System.out.println("Expected:");
            printPaths(expected);
            Assert.error("Test case failed: " + filterArgs);
        }
    }

    void printPaths(Collection<Path> paths) {
        paths.stream()
             .sorted()
             .forEachOrdered(p -> System.out.println("    " + p));
    }

    // Given "pkg/A.java, pkg/B.java" this method returns "bin/pkg/A.class, bin/pkg/B.class"
    Set<Path> correspondingClassFiles(Collection<Path> javaFiles) {
        return javaFiles.stream()
                        .map(javaFile -> javaFile.resolveSibling(classNameOf(javaFile) + ".class"))
                        .map(BIN::resolve)
                        .collect(Collectors.toSet());
    }

    Set<Path> allFilesInDir(Path p) throws IOException {
        try (Stream<Path> files = Files.walk(p).filter(Files::isRegularFile)) {
            return files.collect(Collectors.toSet());
        }
    }
}
