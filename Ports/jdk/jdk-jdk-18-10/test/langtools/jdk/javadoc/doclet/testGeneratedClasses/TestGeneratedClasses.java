/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8223355
 * @summary Redundant output by javadoc
 * @library /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build toolbox.ToolBox javadoc.tester.*
 * @run main TestGeneratedClasses
 */

import java.nio.file.Path;

import javadoc.tester.JavadocTester;
import toolbox.ToolBox;

public class TestGeneratedClasses extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestGeneratedClasses tester = new TestGeneratedClasses();
        tester.runTests(m -> new Object[]{Path.of(m.getName())});
    }

    ToolBox tb = new ToolBox();

    @Test
    public void testClasses(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m = src.resolve("m");
        tb.writeJavaFiles(src_m,
                "module m { exports p; }",
                "package p; public class C { }");

        javadoc("-d", base.resolve("out").toString(),
                "--source-path", src_m.toString(),
                "-Xdoclint:none",
                "--module", "m");

        // verify that C.html is only generated once
        checkOutput(Output.OUT, true,
                """
                    Building tree for all the packages and classes...
                    Generating testClasses/out/m/p/C.html...
                    Generating testClasses/out/m/p/package-summary.html..."""
                    .replace("/", FS));
    }
}