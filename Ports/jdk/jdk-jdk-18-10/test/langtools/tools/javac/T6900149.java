/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6900149
 * @summary IllegalStateException when compiling same files and DiagnosticListener is set
 * @modules java.compiler
 *          jdk.compiler
 */

import java.io.*;
import java.util.*;
import javax.tools.*;
import javax.tools.JavaCompiler.CompilationTask;

public class T6900149 {
    public static void main(String[] args) throws IOException {
        DiagnosticCollector<JavaFileObject> diag =
                new DiagnosticCollector<JavaFileObject>();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm =
                compiler.getStandardFileManager(null, null, null)) {
            File emptyFile = createTempFile("Empty.java");
            File[] files = new File[] { emptyFile, emptyFile };
            CompilationTask task = compiler.getTask(null, fm, diag,
                    null, null, fm.getJavaFileObjects(files));
            if (! task.call()) {
                throw new AssertionError("compilation failed");
            }
        }
    }

    private static File createTempFile(String path) throws IOException {
        File f = new File(path);
        try (FileWriter out = new FileWriter(f)) { }
        return f;
    }
}
