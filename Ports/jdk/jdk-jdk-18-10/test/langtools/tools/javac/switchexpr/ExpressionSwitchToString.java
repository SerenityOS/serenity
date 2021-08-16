/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8223305
 * @summary Verify Tree.toString() related to switch expressions
 * @modules jdk.compiler
 */

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import javax.tools.*;

import com.sun.source.util.JavacTask;

public class ExpressionSwitchToString {

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

    private static final String EXPECTED =
            "\n" +
            "public class C {\n" +
            "    \n" +
            "    void t1(Integer i) {\n" +
            "        switch (i) {\n" +
            "        case null:\n" +
            "            i++;\n" +
            "            break;\n" +
            "        \n" +
            "        case 0, 1:\n" +
            "            i++;\n" +
            "            break;\n" +
            "        \n" +
            "        default:\n" +
            "            i++;\n" +
            "            break;\n" +
            "        \n" +
            "        }\n" +
            "    }\n" +
            "    \n" +
            "    int t2(Integer i) {\n" +
            "        return switch (i) {\n" +
            "        case null:\n" +
            "            yield 0;\n" +
            "        \n" +
            "        case 0, 1:\n" +
            "            yield 1;\n" +
            "        \n" +
            "        default:\n" +
            "            yield 2;\n" +
            "        \n" +
            "        };\n" +
            "    }\n" +
            "}";

    public static void main(String[] args) throws Exception {
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;
        DiagnosticListener<JavaFileObject> noErrors = d -> {};
        String sourceVersion = Integer.toString(Runtime.version().feature());

        JavacTask ct = (JavacTask) tool.getTask(null, null, noErrors,
            List.of("-XDdev"), null,
            Arrays.asList(new MyFileObject(CODE)));
        String actualCode = ct.parse().iterator().next().toString();
        actualCode = actualCode.replace(System.getProperty("line.separator"), "\n");
        if (!EXPECTED.equals(actualCode)) {
            throw new AssertionError("Unexpected toString outcome: " +
                                     actualCode);
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
