/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6302184 6350124 6357979
 * @summary javac hidden options that generate source should use the given
 * encoding, if available
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main HiddenOptionsShouldUseGivenEncodingTest
 */

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import toolbox.JavacTask;
import toolbox.ToolBox;

// Original test: test/tools/javac/6302184/T6302184.sh
public class HiddenOptionsShouldUseGivenEncodingTest {

    public static void main(String[] args) throws Exception {
        String encoding = "iso-8859-1";
        Path src = Paths.get("src");
        Files.createDirectories(src);
        Files.write(src.resolve("T6302184.java"), source, Charset.forName(encoding));
        Files.write(src.resolve("T6302184.out"), expect, Charset.forName(encoding));

        Path out = Paths.get("out");
        Files.createDirectories(out);

        ToolBox tb = new ToolBox();
        new JavacTask(tb)
                .outdir("out")
                .options("-encoding", encoding, "-XD-printsource")
                .files(src.resolve("T6302184.java"))
                .run();

        Path path1 = Paths.get("out").resolve("T6302184.java");
        List<String> file1 = tb.readAllLines(path1, encoding);
        Path path2 = src.resolve("T6302184.out");
        List<String> file2 = tb.readAllLines(path2, encoding);
        tb.checkEqual(file1, file2);
    }

    static List<String> source = Arrays.asList(
        "class T6302184 {",
        "    int \u00c0\u00c1\u00c2\u00c3\u00c4\u00c5 = 1;",
        "}"
    );

    static List<String> expect = Arrays.asList(
        "",
        "class T6302184 {",
        "    ",
        "    T6302184() {",
        "        super();",
        "    }",
        "    int \u00c0\u00c1\u00c2\u00c3\u00c4\u00c5 = 1;",
        "}"
    );

}
