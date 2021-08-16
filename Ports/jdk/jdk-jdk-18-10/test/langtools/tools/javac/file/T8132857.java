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
 * @bug 8132857
 * @summary Verify an up-to-date name for UTF-8 encoding is provided in error messages.
 * @modules jdk.compiler
 */

import java.io.*;
import java.nio.charset.Charset;

public class T8132857 {
    public static void main(String... args) throws Exception{
        new T8132857().run();
    }

    void run() throws IOException {
        if (!Charset.defaultCharset().equals(Charset.forName("UTF-8"))) {
            System.err.println("skipping test, default charset is not UTF-8");
            return;
        }

        File src = new File("src");
        src.mkdirs();
        try (OutputStream out = new FileOutputStream(new File(src, "Test.java"))) {
            out.write('/');
            out.write('/');
            out.write(0b1100_0000);
            out.write('a');
        }

        try (StringWriter out = new StringWriter(); PrintWriter pw = new PrintWriter(out)) {
            int rc = com.sun.tools.javac.Main.compile(new String[] {"-XDrawDiagnostics", "src/Test.java"}, pw);

            pw.flush();

            String lineSeparator = System.getProperty("line.separator");
            String expected =
                    "Test.java:1:3: compiler.err.illegal.char.for.encoding: C0, UTF-8" + lineSeparator +
                    "1 error" + lineSeparator;
            String actual = out.toString();

            System.err.println(actual);

            if (rc == 0) {
                throw new Error("compilation unexpectedly passed: " + rc);
            }

            if (!expected.equals(actual)) {
                throw new Error("unexpected output: " + actual);
            }
        }
    }

}

