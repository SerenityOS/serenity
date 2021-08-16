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
 * @summary Test serialized forms, with at-see to other members
 * @library /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.* toolbox.ToolBox
 * @run main TestSerializedFormWithSee
 */

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import toolbox.ToolBox;

/**
 * Test the links generated in source files with combinations
 * of modules, Serializable, and @see for public and private methods.
 *
 * In the various test cases, in addition to the explicit call
 * to {@code checkExit}, the primary check is the implicit call
 * to {@code checkLinks}, to verify that there are no broken
 * links in the generated files.
 */
import javadoc.tester.JavadocTester;

public class TestSerializedFormWithSee extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestSerializedFormWithSee tester = new TestSerializedFormWithSee();
        tester.runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    private final ToolBox tb;

    TestSerializedFormWithSee() {
        tb = new ToolBox();
    }

    @Test
    public void test_noModule_notSerializable(Path base) throws IOException {
        Path srcDir = generateSource(base, false, false);

        Path outDir = base.resolve("out");
        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "p");
        checkExit(Exit.OK);
    }

    @Test
    public void test_noModule_serializable(Path base) throws IOException {
        Path srcDir = generateSource(base, false, true);

        Path outDir = base.resolve("out");
        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "p");
        checkExit(Exit.OK);
    }

    @Test
    public void test_module_notSerializable(Path base) throws IOException {
        Path srcDir = generateSource(base, true, false);

        Path outDir = base.resolve("out");
        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "m/p");
        checkExit(Exit.OK);
    }

    @Test
    public void test_module_serializable(Path base) throws IOException {
        Path srcDir = generateSource(base, true, true);

        Path outDir = base.resolve("out");
        javadoc("-d", outDir.toString(),
                "-sourcepath", srcDir.toString(),
                "m/p");
        checkExit(Exit.OK);
    }

    Path generateSource(Path base, boolean module, boolean serializable) throws IOException {
        Path dir = base.resolve("src");
        if (module) {
            tb.writeJavaFiles(dir, "module m { }");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("package p;\n");
        sb.append("public class C " + (serializable ? "implements java.io.Serializable " : "") + "{\n");
        for (String access : new String[] { "public", "private" }) {
            sb.append("    /**\n");
            sb.append("     * This is a " + access + " " + (serializable ? "serializable " : "") + "field.\n");
            sb.append("     * More description.\n");
            sb.append("     * " + (serializable ? "@serial This is the serial description." : "") + "\n");
            sb.append("     * @see #publicMethod()\n");
            sb.append("     * @see #privateMethod()\n");
            sb.append("     */\n");
            sb.append("    " + access + " int " + access + "Field;\n");
        }
        for (String access : new String[] { "public", "private" }) {
            sb.append("    /**\n");
            sb.append("     * This is a " + access + " method.\n");
            sb.append("     * More description.\n");
            sb.append("     * @return zero.\n");
            sb.append("     */\n");
            sb.append("    " + access + " int " + access + "Method() { return 0; }\n");
        }
        sb.append("    }\n");
        tb.writeJavaFiles(dir, sb.toString());
        return dir;
    }
}
