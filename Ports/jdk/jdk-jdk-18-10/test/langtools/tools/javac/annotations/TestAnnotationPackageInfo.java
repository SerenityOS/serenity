/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6993311
 * @summary annotations on packages are not validated
 * @modules jdk.compiler
 */

import java.io.*;
import java.net.*;
import java.util.*;
import javax.tools.*;
import com.sun.source.util.*;

public class TestAnnotationPackageInfo {
    public static void main(String... args) throws Exception {
        new TestAnnotationPackageInfo().run();
    }

    static class MyFileObject extends SimpleJavaFileObject {
        private String text;
        public MyFileObject(String fileName, String text) {
            super(URI.create("myfo:/" + fileName), JavaFileObject.Kind.SOURCE);
            this.text = text;
        }
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return text;
        }
    }

    public void run() throws Exception {
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        JavaFileObject test_java = new MyFileObject("test/Test.java",
            "package test; public @interface Test {\n" +
            "    public int mandatory();\n" +
            "}\n");

        JavaFileObject package_info_java = new MyFileObject("test/package-info.java",
            "@Test package test;");

        DiagnosticCollector<JavaFileObject> coll = new DiagnosticCollector<JavaFileObject>();

        List<? extends JavaFileObject> files = Arrays.asList(test_java, package_info_java);
        JavacTask ct = (JavacTask)tool.getTask(null, null, coll, null, null, files);
        ct.analyze();

        String expectedCode = "compiler.err.annotation.missing.default.value";
        List<Diagnostic<? extends JavaFileObject>> diags = coll.getDiagnostics();
        switch (diags.size()) {
            case 0:
                throw new Exception("no diagnostics reported");
            case 1:
                String code = diags.get(0).getCode();
                if (code.equals(expectedCode))
                    return;
                throw new Exception("unexpected diag: " + diags.get(0));
            default:
                throw new Exception("unexpected diags reported: " + diags);
        }
    }
}
