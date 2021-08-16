/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6405099
 * @summary Compiler crashes when javac encounters /usr/jdk/packges/lib/ext with no 777 permissions
 * @modules jdk.compiler
 */

import java.io.*;

public class T6405099
{
    public static void main(String[] args) {
        File bad = new File("bad");
        try {
            bad.mkdir();
            bad.setReadable(false);
            bad.setExecutable(false);

            test(bad);

        } finally {
            bad.setExecutable(true);
            bad.setReadable(true);
        }
    }

    static void test(File dir) {
        String[] args = {
            "-source", "8", "-target", "8", // -extdirs not allowed after -target 8
            "-extdirs", dir.getPath(),
            "-d", ".",
            new File(System.getProperty("test.src", "."), "T6405099.java").getPath()
        };

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        int rc = com.sun.tools.javac.Main.compile(args, pw);
        pw.close();
        System.out.println(sw);

        if (rc != 0)
            throw new Error("compilation failed");
    }
}
