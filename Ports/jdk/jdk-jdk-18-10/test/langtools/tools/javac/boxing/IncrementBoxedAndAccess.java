/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8143388
 * @summary Verify that boxed postfix operator works properly when referring to super class' field.
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.javap
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main IncrementBoxedAndAccess
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import toolbox.JavacTask;
import toolbox.ToolBox;

public class IncrementBoxedAndAccess {
    public static void main(String... args) throws IOException {
        new IncrementBoxedAndAccess().run();
    }

    void run() throws IOException {
        ToolBox tb = new ToolBox();

        Path expected = Paths.get("expected");
        Files.createDirectories(expected);
        tb.cleanDirectory(expected);
        new JavacTask(tb)
          .sources("package p1;" +
                   "public class B {" +
                   "    protected Integer i;" +
                   "}",
                   "package p2;" +
                   "public class S extends p1.B {" +
                   "    public void i() { i++; }" +
                   "    private class I {" +
                   "        void i() { i++; }" +
                   "        private class II {" +
                   "            void i() { i++; }" +
                   "        }" +
                   "    }" +
                   "}")
          .outdir(expected)
          .run();

        Path actual = Paths.get("actual");
        Files.createDirectories(actual);
        tb.cleanDirectory(actual);
        new JavacTask(tb)
          .sources("package p1;" +
                   "public class B {" +
                   "    protected Integer i;" +
                   "}",
                   "package p2;" +
                   "public class S extends p1.B {" +
                   "    public void i() { super.i++; }" +
                   "    private class I {" +
                   "        void i() { S.super.i++; }" +
                   "        private class II {" +
                   "            void i() { S.super.i++; }" +
                   "        }" +
                   "    }" +
                   "}")
          .outdir(actual)
          .run();
    }
}
