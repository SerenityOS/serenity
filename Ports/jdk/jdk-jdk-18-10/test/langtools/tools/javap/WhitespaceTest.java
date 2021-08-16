/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8033581 8033798 8033726
 * @summary Check whitespace in generated output
 * @modules jdk.jdeps/com.sun.tools.javap
 */

import java.io.*;
import java.util.*;

public class WhitespaceTest {
    public static void main(String... args) throws Exception {
        new WhitespaceTest().run();
    }

    void run() throws Exception {
        test("-v", "java.lang.Object");
        test("-XDtab:1", "-v", "java.lang.Object");

        String testClasses = System.getProperty("test.classes");
        for (int i = 10; i < 40; i++)
            test("-XDtab:" + i, "-v", "-classpath", testClasses, "WhitespaceTest$HelloWorld");

        if (errors > 0)
            throw new Exception(errors + " errors found");
    }

    void test(String... args) throws Exception {
        // need to avoid "//" appearing as a constant in the constant pool
        String slash = "/";
        String doubleSlash = slash + slash;
        System.out.println("test: " + Arrays.asList(args));
        String out = javap(args);
        for (String line: out.split("[\r\n]+")) {
            if (line.endsWith(" "))
                error("line has trailing whitespace: " + line);
            int comment = line.indexOf(doubleSlash);
            if (comment > 0 && line.charAt(comment - 1) != ' ') {
                // make allowance for URLs
                if (!line.matches(".*\\bfile:/{3}.*"))
                    error("no space before comment: " + line);
            }
            if (line.matches(" +}"))
                error("bad indentation: " + line);
        }
    }

    String javap(String... args) throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        int rc = com.sun.tools.javap.Main.run(args, out);
        out.close();
        System.out.println(sw.toString());
        if (rc < 0)
            throw new Exception("javap exited, rc=" + rc);
        return sw.toString();
    }

    void error(String msg) {
        System.out.println("Error: " + msg);
        errors++;
    }

    int errors;

    // small class to test repeatedly with different tab values
    static class HelloWorld {
        public static void main(String... args) {
            System.out.println("Hello World!");
        }
    }
}


