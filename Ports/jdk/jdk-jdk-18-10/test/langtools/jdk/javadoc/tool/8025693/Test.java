/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8025693
 * @summary javadoc should ignore <clinit> methods found in classes on classpath
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 */

import java.io.*;

public class Test {
    public static void main(String[] args) throws Exception {
        new Test().run();
    }

    final File baseFile = new File("src/Base.java");
    final String baseText =
        """
            package p;
            public class Base { static { } }
            """;

    final File srcFile = new File("src/C.java");
    final String srcText =
        """
            package p;
            /** comment */
            public abstract class C extends Base { }
            """;

    void run() throws Exception {
        File classesDir = new File("classes");
        classesDir.mkdirs();
        writeFile(baseFile, baseText);
        String[] javacArgs = {
            "-d", classesDir.getPath(),
            baseFile.getPath()
        };
        com.sun.tools.javac.Main.compile(javacArgs);

        writeFile(srcFile, srcText);
        String[] args = {
            "-d", "api",
            "-classpath", classesDir.getPath(),
            "-package", "p",
            srcFile.getPath()
        };

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        PrintStream prev = System.err;
        System.setErr(ps);
        try {
            int rc = jdk.javadoc.internal.tool.Main.execute(args);
        } finally {
            System.err.flush();
            System.setErr(prev);
        }
        String out = baos.toString();
        System.out.println(out);

        String errorMessage = "java.lang.IllegalArgumentException: <clinit>";
        if (out.contains(errorMessage))
            throw new Exception("error message found: " + errorMessage);
    }

    void writeFile(File file, String body) throws IOException {
        file.getParentFile().mkdirs();
        try (FileWriter out = new FileWriter(file)) {
            out.write(body);
        }
    }
}

