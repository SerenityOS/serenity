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
 * @bug 6403424
 * @summary JavacFileManager.inferBinaryName is not case-insensitive on Windows
 * @modules jdk.compiler/com.sun.tools.javac.api
 */

import java.io.*;
import java.util.*;
import com.sun.tools.javac.api.*;

public class T6403424 {
    public static void main(String[] args) {
        File testSrc = new File(System.getProperty("test.src", "."));

        File TMP = new File("TMP");
        TMP.mkdirs();

        // first, compile A to the TMP directory
        File A_java = new File(testSrc, "A.java");
        compile("-d", TMP.getPath(), A_java.getPath());

        // now compile B, which references A,
        // with TMP on classpath and tmp on bootclasspath
        File B_java = new File(testSrc, "B.java");
        compile("-source", "8", "-target", "8", // can't use -Xbootclasspath/p after 8
                "-classpath",  TMP.getPath(),
                "-Xbootclasspath/p:" + TMP.getPath().toLowerCase(),
                "-d", ".",
                B_java.getPath());

        // should not get NPE from compiler
    }

    private static void compile(String... args) {
        System.err.println("compile: " + Arrays.asList(args));
        JavacTool javac = JavacTool.create();

        int rc = javac.run(null, null, null, args);
        if (rc != 0)
            throw new AssertionError("test compilation failed");
    }
}
