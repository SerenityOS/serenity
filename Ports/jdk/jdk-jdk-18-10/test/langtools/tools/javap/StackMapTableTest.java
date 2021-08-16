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
 * @bug 8033930 8033913
 * @summary bad formatting of StackMapTable entries
 * @modules jdk.jdeps/com.sun.tools.javap
 */

import java.io.*;
import java.util.*;

public class StackMapTableTest {
    public static void main(String... args) throws Exception {
        new StackMapTableTest().run();
    }

    void run() throws Exception {
        String testClasses = System.getProperty("test.classes");
        String out = javap("-v", "-classpath", testClasses, A.class.getName());

        String nl = System.getProperty("line.separator");
        out = out.replaceAll(nl, "\n");

        if (out.contains("\n\n\n"))
            error("double blank line found");

        String expect =
            "      StackMapTable: number_of_entries = 2\n" +
            "        frame_type = 252 /* append */\n" +
            "          offset_delta = 2\n" +
            "          locals = [ int ]\n" +
            "        frame_type = 250 /* chop */\n" +
            "          offset_delta = 18\n";
        if (!out.contains(expect))
            error("expected text not found");

        if (errors > 0)
            throw new Exception(errors + " errors found");
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

    /** Simple test class to run through javap. */
    public class A {
        public void a() {
            for (int i = 0; i < 10; i++) {
                System.out.println(i);
            }
        }
        public void b() {
        }
        public void c() {
        }
    }
}
