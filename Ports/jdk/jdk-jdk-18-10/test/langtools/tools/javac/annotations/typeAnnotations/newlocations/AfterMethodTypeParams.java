/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8038788
 * @summary Verify proper handling of annotations after method's type parameters.
 * @modules jdk.compiler
 * @build AfterMethodTypeParams
 * @run main AfterMethodTypeParams
 */

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.*;

import javax.lang.model.element.Name;
import javax.tools.*;

import com.sun.source.tree.*;
import com.sun.source.util.*;

public class AfterMethodTypeParams {

    public static void main(String... args) throws IOException {
        new AfterMethodTypeParams().run();
    }

    void run() throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        for (TestCase tc : testCases) {
            String test = TEMPLATE.replace("CONTENT", tc.snippet);
            List<JavaFileObject> files = Arrays.asList(new MyFileObject(test));
            StringWriter out = new StringWriter();
            List<String> options = Arrays.asList("-XDrawDiagnostics", "--should-stop=at=FLOW");
            JavacTask task = (JavacTask) compiler.getTask(out, null, null, options, null, files);

            new TreePathScanner<Void, Void>() {
                boolean seenAnnotation;
                @Override
                public Void visitAnnotation(AnnotationTree node, Void p) {
                    Name name = ((IdentifierTree) node.getAnnotationType()).getName();
                    seenAnnotation |= name.contentEquals("TA") || name.contentEquals("DA");
                    return null;
                }
                @Override
                public Void visitCompilationUnit(CompilationUnitTree node, Void p) {
                    super.visitCompilationUnit(node, p);
                    if (!seenAnnotation)
                        error(test, "Annotation was missing");
                    return null;
                }
            }.scan(task.parse(), null);

            task.analyze();

            if (!tc.error.equals(out.toString().trim())) {
                error(test, "Incorrect errors: " + out.toString());
            }
        }

        if (errors > 0) {
            throw new IllegalStateException("Errors found");
        }
    }

    int errors;

    void error(String code, String error) {
        System.out.println("Error detected: " + error);
        System.out.println("Code:");
        System.out.println(code);
        errors++;
    }

    static String TEMPLATE =
        "import java.lang.annotation.*;\n" +
        "public class Test {\n" +
        "    CONTENT\n" +
        "}\n" +
        "@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})\n" +
        "@interface DA { }\n" +
        "@Target(ElementType.TYPE_USE)\n" +
        "@interface TA { }\n";

    static class MyFileObject extends SimpleJavaFileObject {
        final String text;
        public MyFileObject(String text) {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            this.text = text;
        }
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return text;
        }
    }

    static TestCase[] testCases = new TestCase[] {
        new TestCase("<T> @DA int foo1() { return 0;}", ""),
        new TestCase("<T> @DA void foo2() { }", ""),
        new TestCase("<T> @TA int foo3() { return 0;}", ""),
        new TestCase("<T> @TA void foo4() { }",
                "Test.java:3:9: compiler.err.annotation.type.not.applicable"),
        new TestCase("<T> @DA Test() { }", "Test.java:3:9: compiler.err.illegal.start.of.type"),
        new TestCase("<T> @TA Test() { }", "Test.java:3:9: compiler.err.illegal.start.of.type"),
    };

    static class TestCase {
        final String snippet;
        final String error;
        public TestCase(String snippet, String error) {
            this.snippet = snippet;
            this.error = error;
        }
    }
}

