/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8259530
 * @summary Generated docs contain MIT/GPL-licenced works without reproducing the licence
 * @library  /tools/lib ../../lib
 * @modules  jdk.javadoc/jdk.javadoc.internal.tool
 * @build toolbox.ToolBox javadoc.tester.*
 * @run main TestLegalNotices
 */

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

import javadoc.tester.JavadocTester;
import toolbox.ToolBox;

public class TestLegalNotices extends JavadocTester {
    public static void main(String... args) throws Exception {
        TestLegalNotices tester = new TestLegalNotices();
        tester.runTests(m -> new Object[]{Path.of(m.getName())});
    }

    private final ToolBox tb = new ToolBox();

    enum OptionKind {
        UNSET, DEFAULT, NONE, DIR
    }

    enum IndexKind {
        INDEX, NO_INDEX
    }


    @Test
    public void testCombo(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "package p; public class C { }");
        Path legal = base.resolve("toy-legal");
        tb.writeFile(legal.resolve("TOY-LICENSE"), "This is a demo license.");

        for (var optionKind : OptionKind.values()) {
            for (var indexKind : IndexKind.values()) {
                test(base, src, legal, optionKind, indexKind);
            }
        }
    }

    void test(Path base, Path src, Path legal, OptionKind optionKind, IndexKind indexKind) throws IOException {
        System.out.println("testing " + optionKind + " " + indexKind);
        Path out = base.resolve(optionKind + "-" + indexKind).resolve("out");
        List<String> args = new ArrayList<>();
        args.addAll(List.of(
                "-d", out.toString()));

        if (indexKind == IndexKind.NO_INDEX) {
            args.add("-noindex");
        }

        args.addAll(List.of(
                "-Xdoclint:-missing",
                "--source-path", src.toString(),
                "p"));

        String value = switch (optionKind) {
            case UNSET -> null;
            case DEFAULT -> "default";
            case NONE -> "none";
            case DIR   -> legal.toString();
        };
        if (value != null) {
            args.addAll(List.of("--legal-notices", value));
        }
        javadoc(args.toArray(new String[0]));

        Set<Path> expectFiles = getExpectFiles(optionKind, indexKind, legal);
        Set<Path> foundFiles = listFiles(out.resolve("legal"));

        checking("Checking legal notice files");
        super.out.println("Expected: " + expectFiles);
        super.out.println("   Found: " + foundFiles);
        if (foundFiles.equals(expectFiles)) {
            passed("Found all expected files");
        }
    }

    Set<Path> getExpectFiles(OptionKind optionKind, IndexKind indexKind, Path legal) throws IOException {
        switch (optionKind) {
            case UNSET, DEFAULT -> {
                Path javaHome = Path.of(System.getProperty("java.home"));
                Path legal_javadoc = javaHome.resolve("legal").resolve("jdk.javadoc");
                return listFiles(legal_javadoc, p ->
                        switch (indexKind) {
                            case INDEX -> true;
                            case NO_INDEX -> !p.getFileName().toString().contains("jquery");
                        });
            }

            case NONE -> {
                return Collections.emptySet();
            }

            case DIR -> {
                return listFiles(legal);
            }
        }
        throw new IllegalStateException();
    }

    Set<Path> listFiles(Path dir) throws IOException {
        return listFiles(dir, p -> true);
    }

    Set<Path> listFiles(Path dir, Predicate<Path> filter) throws IOException {
        if (!Files.exists(dir)) {
            return Collections.emptySet();
        }

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            Set<Path> files = new TreeSet<>();
            for (Path p : ds) {
                if (!Files.isDirectory(p) && filter.test(p)) {
                    files.add(p.getFileName());
                }
            }
            return files;
        }
    }
}