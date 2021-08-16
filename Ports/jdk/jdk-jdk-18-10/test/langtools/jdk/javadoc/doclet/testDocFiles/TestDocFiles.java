/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8008949 8234051 8245696
 * @summary doclet crashes if HTML files in module doc-files directories
 * @library /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build toolbox.ToolBox javadoc.tester.*
 * @run main TestDocFiles
 */

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import toolbox.ToolBox;
import javadoc.tester.JavadocTester;

public class TestDocFiles extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestDocFiles tester = new TestDocFiles();
        tester.runTests(m -> new Object[] { Path.of(m.getName()) });
    }

    ToolBox tb = new ToolBox();

    /**
     * Check doc-files support for a package that is not in a module.
     * @param base the base directory for scratch files
     * @throws IOException if an exception occurs
     */
    @Test
    public void testPackage(Path base) throws IOException {
        Path src = base.resolve("src");

        // write the skeletal Java files
        tb.writeJavaFiles(src,
                "package p; public class C { }\n");

        // write the doc files for the package
        Path pkgDocFiles = src.resolve("p").resolve("doc-files");
        tb.writeFile(pkgDocFiles.resolve("pkg-file.txt"),
                "package text file\n");
        tb.writeFile(pkgDocFiles.resolve("pkg-file.html"),
                """
                    <html>
                    <head><title>Package HTML file</title></head>
                    <body><h1>Package HTML file</h1>File content</body>
                    </html>
                    """);

        javadoc("-d", base.resolve("out").toString(),
                "--source-path", src.toString(),
                "p");
        checkExit(Exit.OK);

        checkOutput("p/doc-files/pkg-file.txt", true,
                "package text file");
        checkOutput("p/doc-files/pkg-file.html", true,
                "Package HTML file");
    }

    /**
     * Check doc-files support for a module and a package that is in a module.
     * @param base the base directory for scratch files
     * @throws IOException if an exception occurs
     */
    @Test
    public void testModules(Path base) throws IOException {
        Path src = base.resolve("src");

        // write the skeletal Java files
        tb.writeJavaFiles(src,
                "module m { exports p; }\n",
                "package p; public class C { }\n");

        // write the doc files for the module
        Path mdlDocFiles = src.resolve("doc-files");
        tb.writeFile(mdlDocFiles.resolve("mdl-file.txt"),
                "module text file\n");
        tb.writeFile(mdlDocFiles.resolve("mdl-file.html"),
                """
                    <html>
                    <head><title>Module HTML file</title></head>
                    <body><h1>Module HTML file</h1>File content</body>
                    </html>
                    """);

        // write the doc files for a package in the module
        Path pkgDocFiles = src.resolve("p").resolve("doc-files");
        tb.writeFile(pkgDocFiles.resolve("pkg-file.txt"),
                "package text file\n");
        tb.writeFile(pkgDocFiles.resolve("pkg-file.html"),
                """
                    <html>
                    <head><title>Package HTML file</title></head>
                    <body><h1>Package HTML file</h1>File content</body>
                    </html>
                    """);

        javadoc("-d", base.resolve("out").toString(),
                "--source-path", src.toString(),
                "--module", "m");
        checkExit(Exit.OK);

        checkOutput("m/doc-files/mdl-file.txt", true,
                "module text file");
        checkOutput("m/doc-files/mdl-file.html", true,
                "Module HTML file");
        checkOutput("m/p/doc-files/pkg-file.txt", true,
                "package text file");
        checkOutput("m/p/doc-files/pkg-file.html", true,
                "Package HTML file");
    }

    @Test
    public void testBadFiles(Path  base) throws IOException {
        Path src = base.resolve("src");

        tb.writeJavaFiles(src,
                "package p; public class C { }");
        Path df = src.resolve("p").resolve("doc-files");

        // note that '?' may be an illegal filename character on some systems (e.g. Windows)
        List<String> cases = List.of("valid", "#bad#", "#bad", "bad#bad", "bad?bad");
        List<Path> files = new ArrayList<>();
        for (String s : cases) {
            try {
                Path f = df.resolve(s);
                tb.writeFile(f, "dummy contents");
                files.add(f);
            } catch (Throwable t) {
                out.println("Cannot write doc-file " + s);
            }
        }

        javadoc("-d", base.resolve("out").toString(),
                "--source-path", src.toString(),
                "p");
        checkExit(Exit.OK);

        // only check for those files
        for (Path f : files) {
            if (f.getFileName().toString().contains("valid")) {
                checkOutput("p/doc-files/" + f.getFileName(), true,
                        "dummy contents");
            } else {
                // be careful handing file separator characters in the message
                checkOutput(Output.OUT, true,
                        "warning: File " + f + " not copied: invalid name");
            }
        }
    }
}
