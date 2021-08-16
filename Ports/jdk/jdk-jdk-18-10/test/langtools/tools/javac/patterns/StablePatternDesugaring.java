/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8264306
 * @summary Verify patterns are desugaring in a reproducible manner
 * @library /tools/lib /tools/javac/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.file
 *      jdk.compiler/com.sun.tools.javac.main
 *      jdk.compiler/com.sun.tools.javac.util
 * @build toolbox.ToolBox toolbox.JavacTask
 * @compile StablePatternDesugaring.java
 * @run main StablePatternDesugaring
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import toolbox.ToolBox;
import toolbox.JavacTask;

public class StablePatternDesugaring {
    protected ToolBox tb;

    StablePatternDesugaring() {
        tb = new ToolBox();
    }

    public static void main(String... args) throws Exception {
        new StablePatternDesugaring().run();
    }

    void run() throws Exception {
        String code = """
                      public class T {
                          private boolean t(Object o) {
                              if (o instanceof String s) {
                                  return s.isEmpty();
                              } else if (o instanceof CharSequence cs) {
                                  return cs.isEmpty();
                              } else if (o instanceof Integer i) {
                                  return i != 0;
                              } else if (o instanceof Float f) {
                                  return f != 0;
                              } else {
                                  return false;
                              }
                          }
                      }
                      """;
        Path base = Paths.get(".");
        Path src = base.resolve("src");

        tb.writeJavaFiles(src, code);

        Path classes1 = base.resolve("classes1");

        if (Files.isDirectory(classes1)) {
            tb.cleanDirectory(classes1);
        } else {
            Files.createDirectories(classes1);
        }

        new JavacTask(tb)
            .files(tb.findJavaFiles(src))
            .outdir(classes1)
            .run()
            .writeAll();

        byte[] expected = Files.readAllBytes(classes1.resolve("T.class"));

        for (int i = 0; i < 10; i++) {
            Path classes2 = base.resolve("classes2");

            if (Files.isDirectory(classes2)) {
                tb.cleanDirectory(classes2);
            } else {
                Files.createDirectories(classes2);
            }

            new JavacTask(tb)
                .files(tb.findJavaFiles(src))
                .outdir(classes2)
                .run()
                .writeAll();

            byte[] actual = Files.readAllBytes(classes2.resolve("T.class"));

            if (!Arrays.equals(expected, actual)) {
                throw new AssertionError("Classfiles differ!");
            }
        }
    }
}
