/*
 * Copyright (c) 2002, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6968833
 * @summary javadoc reports error but still returns 0
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 */

import java.io.*;

public class T6968833 {
    public static void main(String... args) throws IOException {
        new T6968833().run();
    }

    void run() throws IOException {
        File srcDir = new File("src");
        // following file causes error: No public or protected classes found to document.
        File f = writeFile(srcDir, "Foo.java", "class Foo { }");
        String[] args = { f.getPath() };
        int rc = jdk.javadoc.internal.tool.Main.execute(args);
        if (rc == 0)
            throw new Error("Unexpected exit from javadoc: " + rc);
    }

    File writeFile(File dir, String path, String s) throws IOException {
        File f = new File(dir, path);
        f.getParentFile().mkdirs();
        try (Writer out = new FileWriter(f)) {
            out.write(s);
        }
        return f;
    }
}

