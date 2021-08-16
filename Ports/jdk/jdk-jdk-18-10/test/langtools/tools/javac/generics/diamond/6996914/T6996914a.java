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
 * @bug 6996914 7020044 8062373
 * @summary  Diamond inference: problem when accessing protected constructor
 * @modules jdk.compiler
 * @run main T6996914a
 */

import com.sun.source.util.JavacTask;
import java.net.URI;
import java.util.Arrays;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

public class T6996914a {

    enum PackageKind {
        DEFAULT("", ""),
        A("package a;", "import a.*;");

        String pkgDecl;
        String importDecl;

        PackageKind(String pkgDecl, String importDecl) {
            this.pkgDecl = pkgDecl;
            this.importDecl = importDecl;
        }
    }

    enum DiamondKind {
        STANDARD("new Foo<>();"),
        ANON("new Foo<>() {};");

        String expr;

        DiamondKind(String expr) {
            this.expr = expr;
        }
    }

    enum ConstructorKind {
        PACKAGE(""),
        PROTECTED("protected"),
        PRIVATE("private"),
        PUBLIC("public");

        String mod;

        ConstructorKind(String mod) {
            this.mod = mod;
        }
    }

    static class FooClass extends SimpleJavaFileObject {

        final static String sourceStub =
                        "#P\n" +
                        "public class Foo<X> {\n" +
                        "  #M Foo() {}\n" +
                        "}\n";

        String source;

        public FooClass(PackageKind pk, ConstructorKind ck) {
            super(URI.create("myfo:/" + (pk != PackageKind.DEFAULT ? "a/Foo.java" : "Foo.java")),
                    JavaFileObject.Kind.SOURCE);
            source = sourceStub.replace("#P", pk.pkgDecl).replace("#M", ck.mod);
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }

    static class ClientClass extends SimpleJavaFileObject {

        final static String sourceStub =
                        "#I\n" +
                        "class Test {\n" +
                        "  Foo<String> fs = #D\n" +
                        "}\n";

        String source;

        public ClientClass(PackageKind pk, DiamondKind dk) {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            source = sourceStub.replace("#I", pk.importDecl).replace("#D", dk.expr);
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }

    public static void main(String... args) throws Exception {
        for (PackageKind pk : PackageKind.values()) {
            for (ConstructorKind ck : ConstructorKind.values()) {
                for (DiamondKind dk : DiamondKind.values()) {
                    compileAndCheck(pk, ck, dk);
                }
            }
        }
    }

    static void compileAndCheck(PackageKind pk, ConstructorKind ck, DiamondKind dk) throws Exception {
        FooClass foo = new FooClass(pk, ck);
        ClientClass client = new ClientClass(pk, dk);
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        ErrorListener el = new ErrorListener();
        JavacTask ct = (JavacTask)tool.getTask(null, null, el,
                null, null, Arrays.asList(foo, client));
        ct.analyze();
        if (el.errors > 0 == check(pk, ck, dk)) {
            String msg = el.errors > 0 ?
                "Error compiling files" :
                "No error when compiling files";
            throw new AssertionError(msg + ": \n" + foo.source + "\n" + client.source);
        }
    }

    static boolean check(PackageKind pk, ConstructorKind ck, DiamondKind dk) {
        switch (pk) {
            case A: return ck == ConstructorKind.PUBLIC ||
                    (ck  == ConstructorKind.PROTECTED && dk == DiamondKind.ANON);
            case DEFAULT: return ck != ConstructorKind.PRIVATE;
            default: throw new AssertionError("Unknown package kind");
        }
    }

    /**
     * DiagnosticListener to count any errors that occur
     */
    private static class ErrorListener implements DiagnosticListener<JavaFileObject> {

        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            switch (diagnostic.getKind()) {
                case ERROR:
                    errors++;
            }
        }
        int errors;
    }
}
