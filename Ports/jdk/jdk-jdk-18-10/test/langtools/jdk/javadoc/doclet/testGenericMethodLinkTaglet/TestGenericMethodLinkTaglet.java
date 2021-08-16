/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      8188248
 * @summary  NullPointerException on generic methods
 * @library  /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.* toolbox.ToolBox builder.ClassBuilder
 * @run main TestGenericMethodLinkTaglet
 */

import java.nio.file.Path;
import java.nio.file.Paths;

import builder.ClassBuilder;
import builder.ClassBuilder.MethodBuilder;
import toolbox.ToolBox;

import javadoc.tester.JavadocTester;

public class TestGenericMethodLinkTaglet extends JavadocTester {

    final ToolBox tb;

    public static void main(String... args) throws Exception {
        TestGenericMethodLinkTaglet tester = new TestGenericMethodLinkTaglet();
        tester.runTests(m -> new Object[]{Paths.get(m.getName())});
    }

    TestGenericMethodLinkTaglet() {
        tb = new ToolBox();
    }

    @Test
    public void test(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        createTestClass(srcDir);

        Path outDir = base.resolve("out");
        javadoc("-Xdoclint:none",
                "-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "pkg");
        checkExit(Exit.OK);

        checkOutput("pkg/A.html", true,
                """
                    <a href="A.html" title="class in pkg"><code>A</code></a>""");
    }

    void createTestClass(Path srcDir) throws Exception {
        MethodBuilder method = MethodBuilder
                .parse("public <T> void m1(T t) {}")
                .setComments(
                    "@param <T> type param",
                    "@param t param {@link T}");

        new ClassBuilder(tb, "pkg.A")
                .setModifiers("public", "class")
                .addMembers(method)
                .write(srcDir);
    }

}
