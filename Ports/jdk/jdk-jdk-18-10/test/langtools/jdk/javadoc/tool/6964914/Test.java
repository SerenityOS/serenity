/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6964914 8182765
 * @summary javadoc does not output number of warnings using user written doclet
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 */

import java.io.*;

public class Test {
    public static void main(String... args) throws Exception {
        new Test().run();
    }

    public void run() throws Exception {
        javadoc("Error.java", "1 error");
        javadoc("JavacWarning.java", "1 warning");
        javadoc("JavadocWarning.java", "1 warning");
        if (errors > 0)
            throw new Exception(errors + " errors found");
    }

    void javadoc(String path, String expect) {
        File testSrc = new File(System.getProperty("test.src"));
        String[] args = {
            "-Xdoclint:none",
            "-source", "8",
            "-classpath", ".",
            "-package",
            new File(testSrc, path).getPath()
        };

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        int rc = jdk.javadoc.internal.tool.Main.execute(args, pw);
        pw.close();
        String out = sw.toString();
        if (!out.isEmpty())
            System.err.println(out);
        System.err.println("javadoc exit: rc=" + rc);

        if (!out.contains(expect))
            error("expected text not found: " + expect);
    }

    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    int errors;
}
