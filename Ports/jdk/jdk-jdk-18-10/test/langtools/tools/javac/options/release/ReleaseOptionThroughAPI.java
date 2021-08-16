/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8072480
 * @summary Verify that javac can handle --release when invoked using the Compiler API
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class ReleaseOptionThroughAPI {
    public static void main(String... args) throws IOException {
        new ReleaseOptionThroughAPI().run();
    }

    void run() throws IOException {
        String lineSep = System.getProperty("line.separator");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null);
             StringWriter out = new StringWriter();
             PrintWriter outWriter = new PrintWriter(out)) {
            Iterable<? extends JavaFileObject> input =
                    fm.getJavaFileObjects(System.getProperty("test.src") + "/ReleaseOption.java");
            List<String> options = Arrays.asList("--release", "7", "-XDrawDiagnostics", "-Xlint:-options");

            compiler.getTask(outWriter, fm, null, options, null, input).call();
            String expected =
                    "ReleaseOption.java:9:49: compiler.err.doesnt.exist: java.util.stream" + lineSep +
                    "1 error" + lineSep;
            if (!expected.equals(out.toString())) {
                throw new AssertionError("Unexpected output: " + out.toString());
            }
        }
    }
}
