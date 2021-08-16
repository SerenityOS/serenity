/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8191078
 * @summary ensure that javadoc considers packages correctly
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.javadoc/jdk.javadoc.internal.api
 *          jdk.javadoc/jdk.javadoc.internal.tool
 * @library /tools/lib
 * @build toolbox.JavadocTask toolbox.TestRunner toolbox.ToolBox
 * @run main TestPackages
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import toolbox.*;
import toolbox.Task.Expect;
import toolbox.Task.OutputKind;
import toolbox.Task.Result;

public class TestPackages extends TestRunner {

    final ToolBox tb = new ToolBox();

    public TestPackages() {
        super(System.err);
    }

    public static void main(String[] args) throws Exception {
        TestPackages t = new TestPackages();
        t.runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    @Test
    public void testEmptyPackage(Path base) throws Exception {
        Files.createDirectories(base);
        tb.writeFile(base.resolve("p1/package-info.java"), "package p1;\n");
        tb.writeFile(base.resolve("p2/A.java"), "package p2;\npublic class A {}\n");

        Path outDir = base.resolve("out");
        Files.createDirectory(outDir);
        JavadocTask task = new JavadocTask(tb);
        task = task.outdir(outDir).sourcepath(base).options("p1", "p2");
        Result r = task.run(Expect.SUCCESS);
        List<String> list = tb.grep(".*warning.*not found.*", r.getOutputLines(OutputKind.DIRECT));
        if (!list.isEmpty()) {
            throw new Exception("Found warning: " + list.get(0));
        }
    }
}
