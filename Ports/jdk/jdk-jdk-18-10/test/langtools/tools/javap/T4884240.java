/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4884240 8225748
 * @summary additional option required for javap
 * @modules jdk.jdeps/com.sun.tools.javap
 */

import java.io.*;

public class T4884240 {
    public static void main(String... args) throws Exception {
        new T4884240().run();
    }

    public void run() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        String[] args = { "-sysinfo", "java.lang.Object" };
        int rc = com.sun.tools.javap.Main.run(args, pw);
        if (rc != 0)
            throw new Exception("unexpected return code: " + rc);
        pw.close();
        String[] lines = sw.toString().split("\n");
        if (lines.length < 3
            || !lines[0].trim().startsWith("Classfile")
            || !lines[1].trim().startsWith("Last modified")
            || !lines[2].trim().startsWith("SHA-256")) {
            System.out.println(sw);
            throw new Exception("unexpected output");
        }
    }
}
