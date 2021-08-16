/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8246712
 * @summary doclint incorrectly reports some HTML elements as empty
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @library /tools/lib
 * @build toolbox.TestRunner toolbox.ToolBox
 * @run main EmptyHtmlTest
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.sun.source.doctree.DocTreeVisitor;
import com.sun.source.doctree.InlineTagTree;
import jdk.javadoc.internal.doclint.DocLint;
import toolbox.TestRunner;
import toolbox.ToolBox;

public class EmptyHtmlTest extends TestRunner {

    public static void main(String... args) throws Exception {
        EmptyHtmlTest t = new EmptyHtmlTest();
        t.runTests(m -> new Object[] { Path.of(m.getName()) });
    }

    public EmptyHtmlTest() {
        super(System.err);
    }

    ToolBox tb = new ToolBox();

    /**
     * This test is intended to be future-proof, and hence detect any
     * problems in any inline tags added in the future.
     * Since there is not yet any mapping between DocTree.Kind and
     * the corresponding subtype DocTree (see javac Tree.Kind, Tree)
     * the list of all current inline tag classes is determined by
     * scanning DocTreeVisitor.
     *
     * @param base working directory for the test case
     * @throws Exception if an error occurs
     */
    @Test
    public void testInlines(Path base) throws Exception {
        Class<DocTreeVisitor> c = DocTreeVisitor.class;
        for (Method m : c.getDeclaredMethods()) {
            if (m.getName().startsWith("visit") && m.getParameterCount() == 2) {
                Class<?>[] paramTypes = m.getParameterTypes();
                Class<?> firstParamType = paramTypes[0];
                if (InlineTagTree.class.isAssignableFrom(firstParamType)) {
                    testInline(base, firstParamType);
                }
            }
        }
    }

    void testInline(Path base, Class<?> type) throws Exception {
        // the following can eventually be converted to instanceof pattern switch
        Path d = Files.createDirectories(base.resolve(type.getSimpleName()));
        switch (type.getSimpleName()) {
            case "DocRootTree" ->
                    test(d, type, "{@docRoot}");

            case "IndexTree" ->
                    test(d, type, "{@index Object}");

            case "InheritDocTree" ->
                    test(d, type, "{@inheritDoc}");

            case "LinkTree" ->
                    test(d, type, "{@link Object}");

            case "LiteralTree" ->
                    test(d, type, "{@literal abc}");

            case "ReturnTree" ->
                    test(d, type, "{@return abc}");

            case "SummaryTree" ->
                    test(d, type, "{@summary First sentence.}");

            case "SystemPropertyTree" ->
                    test(d, type, "{@systemProperty file.separator}");

            case "UnknownInlineTagTree" ->
                    test(d, type, "{@unknown}");

            case "ValueTree" ->
                    test(d, type, "{@value Math.PI}");

            default ->
                error("no test case provided for " + type);
        }
    }

    void test(Path base, Class<?> type, String tag) throws Exception {
        System.err.println("test " + type.getSimpleName() + " " + tag);
        Path src = base.resolve("src");
        String text = """
                /**
                 * This is a comment.
                 * <b>INSERT</b>
                 */
                 public class C {  }
                """.replace("INSERT", tag);
        tb.writeJavaFiles(src, text);

        List<String> cmdArgs = List.of(
                "-Xmsgs:html",
                "-XcustomTags:unknown",
                src.resolve("C.java").toString()
        );

        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            new DocLint().run(pw, cmdArgs.toArray(new String[0]));
        }
        String log = sw.toString();
        if (!log.isEmpty()) {
            System.err.println("output:");
            log.lines().forEach(System.err::println);
            error("Unexpected output from doclint");
        }
    }
}
