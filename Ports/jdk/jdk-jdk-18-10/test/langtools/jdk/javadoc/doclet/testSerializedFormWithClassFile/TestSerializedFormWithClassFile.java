/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8199307
 * @summary NPE in jdk.javadoc.internal.doclets.toolkit.util.Utils.getLineNumber
 * @library /tools/lib ../../lib
 * @modules
 *      jdk.javadoc/jdk.javadoc.internal.tool
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build javadoc.tester.*
 * @run main TestSerializedFormWithClassFile
 */

import builder.ClassBuilder;

import java.nio.file.Path;
import java.nio.file.Paths;

import builder.ClassBuilder.MethodBuilder;
import toolbox.ToolBox;
import toolbox.JavacTask;

import javadoc.tester.JavadocTester;

public class TestSerializedFormWithClassFile extends JavadocTester {

    final ToolBox tb;

    public static void main(String... args) throws Exception {
        TestSerializedFormWithClassFile tester = new TestSerializedFormWithClassFile();
        tester.runTests(m -> new Object[]{Paths.get(m.getName())});
    }

    TestSerializedFormWithClassFile() {
        tb = new ToolBox();
    }

    @Test
    public void test(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        createTestClass(base, srcDir);

        Path outDir = base.resolve("out");
        javadoc("-d", outDir.toString(),
                "-linksource",
                "--no-platform-links",
                "-classpath", base.resolve("classes").toString(),
                "-sourcepath", "",
                srcDir.resolve("B.java").toString());

        checkExit(Exit.OK);

        checkOutput("serialized-form.html", true,
                """
                    <div class="member-signature"><span class="modifiers">public</span>&nbsp;<span c\
                    lass="return-type">void</span>&nbsp;<span class="element-name">readObject</span>\
                    <wbr><span class="parameters">(java.io.ObjectInputStream&nbsp;arg0)</span>
                                    throws <span class="exceptions">java.lang.ClassNotFoundException,
                    java.io.IOException</span></div>
                    """);
    }

    void createTestClass(Path base, Path srcDir) throws Exception {
        //create A.java , compile the class in classes dir
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        MethodBuilder method = MethodBuilder
                .parse("public void readObject(ObjectInputStream s)"
                        + "throws ClassNotFoundException, IOException {}")
                .setComments(
                    "@param s a serialization stream",
                    "@throws ClassNotFoundException if class not found",
                    "@throws java.io.IOException if an IO error",
                    "@serial");

        new ClassBuilder(tb, "A")
                .setModifiers("public", "abstract", "class")
                .addImplements("Serializable")
                .addImports("java.io.*")
                .addMembers(method)
                .write(srcDir);
        new JavacTask(tb).files(srcDir.resolve("A.java")).outdir(classes).run();

        new ClassBuilder(tb, "B")
                .setExtends("A")
                .setModifiers("public", "class")
                .write(srcDir);
    }
}
