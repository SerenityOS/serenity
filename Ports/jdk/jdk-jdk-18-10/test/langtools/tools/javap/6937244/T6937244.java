/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6937244
 * @summary fields display with JVMS names, not Java names
 * @modules jdk.jdeps/com.sun.tools.javap
 */

import java.io.*;

public class T6937244 {
    public static void main(String[] args) throws Exception {
        new T6937244().run();
    }

    void run() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        String[] args = { "java.lang.String" };
        int rc = com.sun.tools.javap.Main.run(args, pw);
        pw.close();
        String out = sw.toString();
        System.err.println(out);
        if (rc != 0)
            throw new Exception("unexpected exit from javap: " + rc);
        for (String line: out.split("[\r\n]+")) {
            if (line.contains("CASE_INSENSITIVE_ORDER")) {
                if (line.matches("\\s*\\Qpublic static final java.util.Comparator<java.lang.String> CASE_INSENSITIVE_ORDER;\\E\\s*"))
                    return;
                throw new Exception("declaration not shown as expected");
            }
        }
        throw new Exception("declaration of CASE_INSENSITIVE_ORDER not found");
    }
}

