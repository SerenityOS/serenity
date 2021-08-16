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
 * @bug 8035104
 * @summary reorder class file attributes in javap listing
 * @modules jdk.jdeps/com.sun.tools.javap
 */

import java.io.*;

public class T8035104 {
    public static void main(String[] args) throws Exception {
        new T8035104().run();
    }

    public void run() throws Exception {
        String[] lines = javap("-v", T8035104.class.getName()).split("[\r\n]+");
        int minor = -1;
        int SourceFile = -1;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.matches(" *minor version: [0-9.]+"))
                minor = i;
            if (line.matches(" *SourceFile: .+"))
                SourceFile = i;
        }
        if (minor == -1)
            throw new Exception("minor version not found");
        if (SourceFile == -1)
            throw new Exception("SourceFile not found");
        if (SourceFile < minor)
            throw new Exception("unexpected order of output");

        System.out.println("output OK");
    }

    String javap(String... args) {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        int rc = com.sun.tools.javap.Main.run(args, out);
        out.close();
        System.out.println(sw.toString());
        System.out.println("javap exited, rc=" + rc);
        return sw.toString();
    }
}
