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
 * @bug 7096014
 * @summary Javac tokens should retain state
 * @modules jdk.compiler/com.sun.tools.javac.parser
 *          jdk.compiler/com.sun.tools.javac.tree
 */

import com.sun.source.tree.*;
import com.sun.source.util.*;
import com.sun.tools.javac.tree.DocCommentTable;
import com.sun.tools.javac.tree.JCTree;

import java.net.URI;
import java.util.*;
import javax.tools.*;


public class DocCommentToplevelTest {

    enum PackageKind {
        HAS_PKG("package pkg;"),
        NO_PKG("");

        String pkgStr;

        PackageKind(String pkgStr) {
            this.pkgStr = pkgStr;
        }
    }

    enum ImportKind {
        ZERO(""),
        ONE("import java.lang.*;"),
        TWO("import java.lang.*; import java.util.*;");

        String importStr;

        ImportKind(String importStr) {
            this.importStr = importStr;
        }
    }

    enum ModifierKind {
        DEFAULT(""),
        PUBLIC("public");

        String modStr;

        ModifierKind(String modStr) {
            this.modStr = modStr;
        }
    }

    enum ToplevelDocKind {
        HAS_DOC("/** Toplevel! */"),
        NO_DOC("");

        String docStr;

        ToplevelDocKind(String docStr) {
            this.docStr = docStr;
        }
    }

    static int errors;
    static int checks;

    public static void main(String... args) throws Exception {
        //create default shared JavaCompiler - reused across multiple compilations
        JavaCompiler comp = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = comp.getStandardFileManager(null, null, null)) {

            for (PackageKind pk : PackageKind.values()) {
                for (ImportKind ik : ImportKind.values()) {
                    for (ModifierKind mk1 : ModifierKind.values()) {
                        for (ModifierKind mk2 : ModifierKind.values()) {
                            for (ToplevelDocKind tdk : ToplevelDocKind.values()) {
                                new DocCommentToplevelTest(pk, ik, mk1, mk2, tdk).run(comp, fm);
                            }
                        }
                    }
                }
            }

            if (errors > 0)
                throw new AssertionError(errors + " errors found");

            System.out.println(checks + " checks were made");
        }
    }

    PackageKind pk;
    ImportKind ik;
    ModifierKind mk1;
    ModifierKind mk2;
    ToplevelDocKind tdk;
    JavaSource source;

    DocCommentToplevelTest(PackageKind pk, ImportKind ik, ModifierKind mk1, ModifierKind mk2, ToplevelDocKind tdk) {
        this.pk = pk;
        this.ik = ik;
        this.mk1 = mk1;
        this.mk2 = mk2;
        this.tdk = tdk;
        source = new JavaSource();
    }

    void run(JavaCompiler comp, JavaFileManager fm) throws Exception {
        JavacTask task = (JavacTask)comp.getTask(null, fm, null, Arrays.asList("-printsource"), null, Arrays.asList(source));
        for (CompilationUnitTree cu: task.parse()) {
            check(cu);
        }
    }

    void check(CompilationUnitTree cu) {
        checks++;

        new TreeScanner<ClassTree,Void>() {

            DocCommentTable docComments;

            @Override
            public ClassTree visitCompilationUnit(CompilationUnitTree node, Void unused) {
                docComments = ((JCTree.JCCompilationUnit)node).docComments;
                boolean expectedComment = tdk == ToplevelDocKind.HAS_DOC &&
                                          pk == PackageKind.NO_PKG &&
                                          ik != ImportKind.ZERO;
                boolean foundComment = docComments.hasComment((JCTree) node);
                if (expectedComment != foundComment) {
                    error("Unexpected comment " + docComments.getComment((JCTree) node) + " on toplevel");
                }
                return super.visitCompilationUnit(node, null);
            }

            @Override
            public ClassTree visitPackage(PackageTree node, Void unused) {
                boolean expectedComment = tdk == ToplevelDocKind.HAS_DOC &&
                                          pk != PackageKind.NO_PKG;
                boolean foundComment = docComments.hasComment((JCTree) node);
                if (expectedComment != foundComment) {
                    error("Unexpected comment " + docComments.getComment((JCTree) node) + " on toplevel");
                }
                return super.visitPackage(node, null);
            }

            @Override
            public ClassTree visitClass(ClassTree node, Void unused) {
                boolean expectedComment = tdk == ToplevelDocKind.HAS_DOC &&
                        pk == PackageKind.NO_PKG && ik == ImportKind.ZERO &&
                        node.getSimpleName().toString().equals("First");
                boolean foundComment = docComments.hasComment((JCTree) node);
                if (expectedComment != foundComment) {
                    error("Unexpected comment " + docComments.getComment((JCTree) node) + " on class " + node.getSimpleName());
                }
                return super.visitClass(node, unused);
            }
        }.scan(cu, null);
    }

    void error(String msg) {
        System.err.println("Error: " + msg);
        System.err.println("Source: " + source.source);
        errors++;
    }

    class JavaSource extends SimpleJavaFileObject {

        String template = "#D\n#P\n#I\n" +
                          "#M1 class First { }\n" +
                          "#M2 class Second { }\n";

        String source;

        public JavaSource() {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            source = template.replace("#P", pk.pkgStr)
                             .replace("#I", ik.importStr)
                             .replace("#M1", mk1.modStr)
                             .replace("#M2", mk2.modStr)
                             .replace("#D", tdk.docStr);
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }
}
