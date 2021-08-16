/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6759996
 * @summary javac should ignore empty entries on paths
 * @modules jdk.compiler
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class T6759996 {
    public static void main(String[] args) throws Exception {
        new T6759996().run();
    }

    void run() throws IOException, InterruptedException {
        String PS = File.pathSeparator;
        write(new File("A.java"), "class A { }");
        write(new File("B.java"), "class B extends A { }");
        // In the following line, the presence of the empty element
        // should not mask the presence of the "." element on the path
        javac("-verbose", "-sourcepath", "" + PS + ".", "B.java");
    }

    void javac(String... args) throws IOException, InterruptedException {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        int rc = com.sun.tools.javac.Main.compile(args, out);
        System.out.println(sw.toString());
        if (rc != 0)
            throw new Error("javac failed: rc=" + rc);

    }

    void write(File to, String body) throws IOException {
        System.err.println("write " + to);
        File toDir = to.getParentFile();
        if (toDir != null) {
            boolean ok = toDir.mkdirs();
            if (!ok) {
                throw new Error("could not create directory " + toDir);
            }
        }
        Writer out = new FileWriter(to);
        try {
            out.write(body);
            if (!body.endsWith("\n"))
                out.write("\n");
        } finally {
            out.close();
        }
    }
}
