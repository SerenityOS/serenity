/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8143268
 * @summary javac should create output directories as needed
 * @modules jdk.compiler
 */

import java.io.*;

public class T8143268 {
    public static void main(String... args) throws Exception{
        new T8143268().run();
    }

    void run() throws IOException {
        File src = new File("src");
        src.mkdirs();
        try (FileWriter out = new FileWriter(new File(src, "Test.java"))) {
            out.write("public class Test { native void m(); }");
        }

        javac("-d", "classes", "-h", "hdr", "src/Test.java");

        check("classes/Test.class");
        check("hdr/Test.h");
    }

    void javac(String... args) {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(System.out))) {
            int rc = com.sun.tools.javac.Main.compile(args, pw);
            if (rc != 0) {
                throw new Error("compilation failed: " + rc);
            }
        }
    }

    void check(String path) {
        if (!new File(path).exists()) {
            throw new Error("file not found: " + path);
        }
    }
}

