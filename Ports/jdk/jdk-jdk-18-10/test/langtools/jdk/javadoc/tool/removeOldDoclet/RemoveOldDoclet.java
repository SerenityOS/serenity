/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8215584
 * @summary Remove support for the "old" doclet API in com/sun/javadoc
 * @library /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.* toolbox.ToolBox builder.ClassBuilder
 * @run main RemoveOldDoclet
 */


import java.nio.file.Path;
import java.nio.file.Paths;

import builder.ClassBuilder;
import toolbox.ToolBox;

import javadoc.tester.JavadocTester;

public class RemoveOldDoclet extends JavadocTester {

    final ToolBox tb;
    static final String Doclet_CLASS_NAME = TestDoclet.class.getName();

    public static void main(String... args) throws Exception {
        RemoveOldDoclet tester = new RemoveOldDoclet();
        tester.runTests(m -> new Object[]{Paths.get(m.getName())});
    }

    RemoveOldDoclet() {
        tb = new ToolBox();
    }

    @Test
    public void testInvalidDoclet(Path base) throws Exception {
        Path srcDir = base.resolve("src");
        Path outDir = base.resolve("out");

        new ClassBuilder(tb, "pkg.A")
                .setModifiers("public", "class")
                .write(srcDir);

        javadoc("-d", outDir.toString(),
                "-doclet", Doclet_CLASS_NAME,
                "-docletpath", System.getProperty("test.classes", "."),
                "-sourcepath", srcDir.toString(),
                "pkg");

        checkExit(Exit.ERROR);
        checkOutput(Output.OUT, true, String.format("""
                error: Class %s is not a valid doclet.
                  Note: As of JDK 13, the com.sun.javadoc API is no longer supported.""",
                Doclet_CLASS_NAME));
    }

    static class TestDoclet {
        public static boolean start() {
            System.out.println("OLD_DOCLET_MARKER");
            return true;
        }
    }
}
