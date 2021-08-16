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
 * @bug 8207214
 * @summary Test package-level at-serial tags
 * @library /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.* toolbox.ToolBox
 * @run main TestSerialTag
 */

import java.io.IOException;
import java.nio.file.Path;

import toolbox.ToolBox;

import javadoc.tester.JavadocTester;

public class TestSerialTag extends JavadocTester {
    public static void main(String... args) throws Exception {
        TestSerialTag tester = new TestSerialTag();
        tester.runTests(m -> new Object[] { Path.of(m.getName()) });
    }

    private final ToolBox tb;

    TestSerialTag() {
        tb = new ToolBox();
    }

    @Test
    public void testCombo(Path base) throws IOException {
        boolean[] moduleValues = { false, true };
        String[] tagValues = { "", "@serial include", "@serial exclude" };
        for (boolean module : moduleValues ) {
            for (String tag : tagValues ) {
                String name = (module ? "module-" : "")
                              + (tag.isEmpty() ? "default" : tag.replace("@serial ", ""));
                Path dir = base.resolve(name);
                test(dir, module, tag);
            }
        }

    }

    void test(Path base, boolean module, String tag) throws IOException {
        out.println("Test: module:" + module + ", tag:" + tag);

        Path srcDir = generateSource(base, module, tag);

        Path outDir = base.resolve("out");
        if (module) {
            javadoc("-d", outDir.toString(),
                "--module-source-path", srcDir.toString(),
                "--module", "m");
        } else {
            javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "p", "q");
        }
        checkExit(Exit.OK);

        boolean expectLink = !tag.equals("@serial exclude");
        checkSeeSerializedForm(module, expectLink);
    }

    /**
     * Generates source for a test case.
     * Two classes are generated, in two different packages.
     * One package has a variable at-serial tag to test;
     * The other package is a control and always has no special tag.
     *
     * @param base the working directory for the test case
     * @param module whether or not to enclose the packages in a module
     * @param tag the at-serial tag to be tested
     * @return the directory in which the source was created
     */
    Path generateSource(Path base, boolean module, String tag) throws IOException {
        Path srcDir = base.resolve("src");

        Path dir;
        if (module) {
            dir = srcDir.resolve("m");
            tb.writeJavaFiles(dir,
                "module m { exports p; exports q; }");
        } else {
            dir = srcDir;
        }

        tb.writeJavaFiles(dir,
            "/** This is package p;\n * " + tag + "\n */\n"
            + "package p;",
            """
                /** This is class p.C1;
                 */
                package p; public class C1 implements java.io.Serializable { }""",
            """
                /** This is package q;
                 */
                package q;""",
            """
                /** This is class q.C2;
                 */
                package q; public class C2 implements java.io.Serializable { }"""
        );

        return srcDir;
    }

    /**
     * Checks the link to the serialized form page,
     * and whether classes are documented on that page.
     *
     * @param module whether or not the output is module-oriented
     * @param b whether or not class p.C1 should be documented as serializable
     */
    void checkSeeSerializedForm(boolean module, boolean b) {
        String prefix = module ? "m/" : "";

        checkOutput(prefix + "p/C1.html", b,
            "serialized-form.html");
        checkOutput("serialized-form.html", b,
            "C1");

        checkOutput(prefix + "q/C2.html", true,
            "serialized-form.html");
        checkOutput("serialized-form.html", true,
            "C2");
    }
}

