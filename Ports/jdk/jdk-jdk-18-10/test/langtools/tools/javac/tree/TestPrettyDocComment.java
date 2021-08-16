/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8176470
 * @summary javac Pretty printer should include doc comment for modules
 * @modules jdk.compiler
 * @library /tools/lib
 * @build toolbox.TestRunner
 * @run main TestPrettyDocComment
 */

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.List;

import javax.tools.JavaFileObject;
import javax.tools.JavaCompiler;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;

import toolbox.TestRunner;

public class TestPrettyDocComment extends TestRunner {

    public static void main(String... args) throws Exception {
        TestPrettyDocComment t = new TestPrettyDocComment();
        t.runTests();
    }

    final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();

    TestPrettyDocComment() {
        super(System.err);
    }

    @Test
    public void testModule() throws IOException {
        test("module-info.java", "/** This is a module. */ module m { }");
    }

    @Test
    public void testPackage() throws IOException {
        test("package-info.java", "/** This is a package. */ package p;");
    }

    @Test
    public void testClass() throws IOException {
        test("C.java", "/** This is a class. */ class C { }");
    }

    @Test
    public void testField() throws IOException {
        test("C.java", "class C { /** This is a field. */ int f; }");
    }

    @Test
    public void testMethod() throws IOException {
        test("C.java", "class C { /** This is a method. */ void m() { } }");
    }

    void test(String name, String source) throws IOException {
        JavaFileObject fo = new JavaSource(name, source);
        StringWriter log = new StringWriter();
        JavacTask t = (JavacTask) tool.getTask(log, null, null, null, null, List.of(fo));
        Iterable<? extends CompilationUnitTree> trees = t.parse();
        String out = log.toString();
        if (!out.isEmpty()) {
            System.err.println(log);
        }
        String pretty = trees.iterator().next().toString();
        System.err.println("Pretty: <<<");
        System.err.println(pretty);
        System.err.println(">>>");

        String commentText = source.replaceAll(".*\\Q/**\\E (.*) \\Q*/\\E.*", "$1");
        if (!pretty.contains(commentText)) {
            error("expected text not found: " + commentText);
        }
    }

    static class JavaSource extends SimpleJavaFileObject {
        final String source;

        public JavaSource(String name, String source) {
            super(URI.create("myfo:/" + name), JavaFileObject.Kind.SOURCE);
            this.source = source;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }
}
