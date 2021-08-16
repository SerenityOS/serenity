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

/*
 * @test
 * @bug 8213354
 * @summary Support package-specific stylesheets
 * @library /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.* toolbox.ToolBox builder.ClassBuilder
 * @run main TestPackageSpecificStylesheet
 */


import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import builder.ClassBuilder;
import toolbox.ToolBox;

import javadoc.tester.JavadocTester;

public class TestPackageSpecificStylesheet extends JavadocTester {

    final ToolBox tb;

    public static void main(String... args) throws Exception {
        TestPackageSpecificStylesheet tester = new TestPackageSpecificStylesheet();
        tester.runTests(m -> new Object[]{Paths.get(m.getName())});
    }

    TestPackageSpecificStylesheet() {
        tb = new ToolBox();
    }

    @Test
    public void test(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        Path outDir = base.resolve("out");

        new ClassBuilder(tb, "pkg.A")
                .setComments("<span>test span style</span>")
                .setModifiers("public", "class")
                .write(srcDir);

        Path docFilesDir = Files.createDirectories(srcDir.resolve("pkg").resolve("doc-files"));
        Path stylesheet = docFilesDir.resolve("spanstyle.css");
        Files.createFile(stylesheet);
        Files.write(stylesheet, List.of("span{ color:blue; }"));

        new ClassBuilder(tb, "pkg2.B")
                .setModifiers("public", "class")
                .write(srcDir);

        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "pkg", "pkg2");

        checkExit(Exit.OK);

        checkOutput("pkg/A.html", true,
                """
                    <link rel="stylesheet" type="text/css" href="../pkg/doc-files/spanstyle.css" title="Style">""");

        checkOutput("pkg/package-summary.html", true,
                """
                    <link rel="stylesheet" type="text/css" href="../pkg/doc-files/spanstyle.css" title="Style">""");

        checkOutput("pkg2/B.html", false,
                """
                    <link rel="stylesheet" type="text/css" href="../pkg2/doc-files/spanstyle.css" title="Style">""");

    }
}
