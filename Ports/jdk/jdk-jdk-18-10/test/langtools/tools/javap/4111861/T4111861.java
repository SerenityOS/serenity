/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;

/*
 * @test
 * @bug 4111861
 * @summary static final field contents are not displayed
 * @modules jdk.jdeps/com.sun.tools.javap
 */
public class T4111861 {
    public static void main(String... args) throws Exception {
        new T4111861().run();
    }

    void run() throws Exception {
        File testSrc = new File(System.getProperty("test.src", "."));
        File a_java = new File(testSrc, "A.java");
        javac("-d", ".", a_java.getPath());

        String out = javap("-classpath", ".", "-constants", "A");

        String a = read(a_java);

        if (!filter(out).equals(filter(read(a_java)))) {
            System.out.println(out);
            throw new Exception("unexpected output");
        }
    }

    String javac(String... args) throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        int rc = com.sun.tools.javac.Main.compile(args, pw);
        if (rc != 0)
            throw new Exception("javac failed, rc=" + rc);
        return sw.toString();
    }

    String javap(String... args) throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        int rc = com.sun.tools.javap.Main.run(args, pw);
        if (rc != 0)
            throw new Exception("javap failed, rc=" + rc);
        return sw.toString();
    }

    String read(File f) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader in = new BufferedReader(new FileReader(f));
        try {
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
        } finally {
            in.close();
        }
        return sb.toString();
    }

    // return those lines beginning "public static final"
    String filter(String s) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader in = new BufferedReader(new StringReader(s));
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.indexOf("public static final") > 0) {
                    sb.append(line.trim());
                    sb.append('\n');
                }
            }
        } finally {
            in.close();
        }
        return sb.toString();
    }
}
