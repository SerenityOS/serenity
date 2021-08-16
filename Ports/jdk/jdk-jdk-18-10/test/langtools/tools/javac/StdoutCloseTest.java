/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7118295
 * @summary javac does not explicitly close -Xstdout file
 * @modules jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 * @run main StdoutCloseTest
 */


import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.main.Main;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class StdoutCloseTest {

    public static void main(String[] args) throws Exception {
        new StdoutCloseTest().test();
    }

    static final String program = "public class Test {\n" +
                                  "  public boolean test() {\n" +
                                  "    int i;\n" +
                                  "    if (i > 0) return true;\n" +
                                  "    return false;\n" +
                                  "  }\n" +
                                  "}\n";

    public void test() throws Exception {
        final String sourceName = "Test.java";
        final String outName = "Test.out";
        File source = new File(sourceName);
        PrintWriter pw = new PrintWriter(source);
        pw.write(program);
        pw.flush();
        pw.close();

        PrintWriter log = compileClass(sourceName, outName);

        File outFile = new File(outName);
        if (!outFile.exists()) {
            throw new Exception("Output file was not created!");
        }
        if (!log.checkError()) { // will return true if the stream is still open
            log.close(); // Close output PrintWriter manually
            throw new Exception("Output file was still open!");
        }
    }

    public PrintWriter compileClass(String src, String out) {
        List<String> options = new ArrayList<>();
        options.add("-Xstdout");
        options.add(out);
        options.add(src);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        Main compiler = new Main("javac", pw);
        compiler.compile(options.toArray(new String[options.size()]));
        pw.flush();
        if (sw.getBuffer().length() > 0) {
            System.err.println(sw.toString());
        }
        return compiler.log.getWriter(Log.WriterKind.NOTICE);
    }
}
