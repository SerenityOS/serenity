/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8202627
 * @summary javadoc generates broken links to deprecated items when -nodeprecated is used
 * @library /tools/lib ../../lib
 * @modules
 *      jdk.javadoc/jdk.javadoc.internal.tool
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build javadoc.tester.*
 * @run main TestLinksWithNoDeprecatedOption
 */


import java.nio.file.Path;
import java.nio.file.Paths;

import builder.ClassBuilder;
import builder.ClassBuilder.FieldBuilder;
import builder.ClassBuilder.MethodBuilder;
import toolbox.ToolBox;

import javadoc.tester.JavadocTester;

public class TestLinksWithNoDeprecatedOption extends JavadocTester {

    final ToolBox tb;

    public static void main(String... args) throws Exception {
        TestLinksWithNoDeprecatedOption tester = new TestLinksWithNoDeprecatedOption();
        tester.runTests(m -> new Object[]{Paths.get(m.getName())});
    }

    TestLinksWithNoDeprecatedOption() {
        tb = new ToolBox();
    }

    @Test
    public void test(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        createTestClass(base, srcDir);

        Path outDir = base.resolve("out");
        javadoc("-d", outDir.toString(),
                "-nodeprecated",
                "-use",
                "-sourcepath", srcDir.toString(),
                "pkg");

        checkExit(Exit.OK);

        checkOutput("pkg/class-use/A.html", true,
                """
                    <a href="../B.html#a2" class="member-name-link">a2</a>""");

        //links for deprecated items will not be found
        checkOutput("pkg/class-use/A.html", false,
                """
                    <a href="../B.html#deprecatedField" class="member-name-link">deprecatedField</a>""");

        checkOutput("pkg/class-use/A.html", false,
                """
                    <a href="../B.html#deprecatedMethod(pkg.A)" class="member-name-link">deprecatedMethod</a>""");

        checkOutput("pkg/class-use/A.html",false,
                """
                    <a href="../B.html#%3Cinit%3E(pkg.A)" class="member-name-link">B</a>""");

    }

    void createTestClass(Path base, Path srcDir) throws Exception {
        new ClassBuilder(tb, "pkg.A")
                .setModifiers("public", "class")
                .write(srcDir);

        MethodBuilder method = MethodBuilder
                .parse("public void deprecatedMethod(A a) {}")
                .setComments(
                    "@deprecated",
                    "@param A a param");

        MethodBuilder constructor = MethodBuilder
                .parse("public B(A a) {}")
                .setComments("@deprecated");


        new ClassBuilder(tb, "pkg.B")
                .setModifiers("public", "class")
                .addMembers(
                        constructor,
                        method,
                        FieldBuilder.parse("public A deprecatedField").setComments("@deprecated"),
                        FieldBuilder.parse("public A a2"))
                .write(srcDir);
    }
}
