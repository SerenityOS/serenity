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
 * @bug 8252031
 * @summary Verify the use of Flags.LOCKED in Check.checkNonCyclic is not broken
            by use in Types.asSuper.
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.JavacTask toolbox.TestRunner toolbox.ToolBox
 * @run main LockedFlagClash
 */

import java.io.IOException;

import java.nio.file.Path;
import java.nio.file.Paths;

import toolbox.JavacTask;
import toolbox.TestRunner;
import toolbox.ToolBox;

public class LockedFlagClash extends TestRunner {

    public static void main(String... args) throws Exception {
        LockedFlagClash t = new LockedFlagClash();
        t.runTests();
    }

    private final ToolBox tb = new ToolBox();

    LockedFlagClash() throws IOException {
        super(System.err);
    }

    protected void runTests() throws Exception {
        runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    @Test
    public void testLOCKEDFlagDoesNotClash(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                          """
                          package java.lang;
                          public abstract class Byte extends Number {
                          }
                          """,
                          """
                          package java.lang;
                          public class Object {
                          }
                          """,
                          """
                          package java.lang;
                          public class Test {
                              private static final String o1 = "";
                              private static final String o2 = "" + o1;
                          }
                          """);
        Path classes = base.resolve("classes");

        tb.createDirectories(classes);
        tb.cleanDirectory(classes);

        new JavacTask(tb)
                .outdir(classes)
                .options("--patch-module", "java.base=" + src)
                .files(tb.findJavaFiles(src.resolve("java").resolve("lang").resolve("Test.java")))
                .run()
                .writeAll();
    }
}
