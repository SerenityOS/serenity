/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8206986
 * @summary Ensure than parser can parse incomplete sources
 * @modules jdk.compiler
 */

import java.io.StringWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import javax.tools.*;

import com.sun.source.util.JavacTask;

public class ParseIncomplete {

    private static final String CODE =
            "public class C {" +
            "    void t1(Integer i) {" +
            "        switch (i) {" +
            "            case null: i++; break;" +
            "            case 0, 1: i++; break;" +
            "            default: i++; break;" +
            "        }" +
            "    }" +
            "    int t2(Integer i) {" +
            "        return switch (i) {" +
            "            case null: yield 0;" +
            "            case 0, 1: yield 1;" +
            "            default: yield 2;" +
            "        }" +
            "    }" +
            "}";

    public static void main(String[] args) throws Exception {
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;
        DiagnosticListener<JavaFileObject> noErrors = d -> {};
        String sourceVersion = Integer.toString(Runtime.version().feature());

        for (int i = 0; i < CODE.length(); i++) {
            String code = CODE.substring(0, i + 1);
            StringWriter out = new StringWriter();
            try {
                JavacTask ct = (JavacTask) tool.getTask(out, null, noErrors,
                    List.of("-XDdev"), null,
                    Arrays.asList(new MyFileObject(code)));
                ct.parse().iterator().next();
            } catch (Throwable t) {
                System.err.println("Unexpected exception for code: " + code);
                System.err.println("output: " + out);
                throw t;
            }
            if (!out.toString().isEmpty()) {
                System.err.println("Unexpected compiler for code: " + code);
                System.err.println(out);
                throw new AssertionError();
            }
        }
    }

    static class MyFileObject extends SimpleJavaFileObject {
        private String text;

        public MyFileObject(String text) {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            this.text = text;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return text;
        }
    }
}
