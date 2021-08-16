/*
 * Copyright (c) 2009, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6907575
 * @modules jdk.jdeps/com.sun.tools.classfile
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 * @build GetDeps p.C1
 * @run main T6907575
 */

import java.io.*;

public class T6907575 {
    public static void main(String... args) throws Exception {
        new T6907575().run();
    }

    void run() throws Exception {
        String testSrc = System.getProperty("test.src");
        String testClasses = System.getProperty("test.classes");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        GetDeps gd = new GetDeps();
        gd.run(pw, "-classpath", testClasses, "-t", "-p", "p", "p/C1");
        pw.close();
        System.out.println(sw);

        String ref = readFile(new File(testSrc, "T6907575.out"));
        diff(sw.toString().replaceAll("[\r\n]+", "\n"), ref);
    }

    void diff(String actual, String ref) throws Exception {
        System.out.println("EXPECT:>>>" + ref + "<<<");
        System.out.println("ACTUAL:>>>" + actual + "<<<");
        if (!actual.equals(ref))
            throw new Exception("output not as expected");
    }

    String readFile(File f) throws IOException {
        Reader r = new FileReader(f);
        char[] buf = new char[(int) f.length()];
        int offset = 0;
        int n;
        while (offset < buf.length && (n = r.read(buf, offset, buf.length - offset)) != -1)
            offset += n;
        return new String(buf, 0, offset);
    }
}
