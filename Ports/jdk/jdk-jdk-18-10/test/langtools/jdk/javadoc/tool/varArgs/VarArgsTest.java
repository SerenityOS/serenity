/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4421066 5006659
 * @summary Verify the contents of a ClassDoc containing a varArgs method.
 *          Verify that see/link tags can use "..." notation.
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main VarArgsTest
 */

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.SeeTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTrees;
import com.sun.source.util.TreePath;
import javadoc.tester.TestDoclet;
import jdk.javadoc.doclet.DocletEnvironment;

import javadoc.tester.JavadocTester;

public class VarArgsTest extends JavadocTester {

    public static void main(String[] args) throws Exception {
        JavadocTester t = new VarArgsTest();
        t.runTests();
    }

    @Test
    public void testVarArgs() throws Exception {
        javadoc("-sourcepath", testSrc,
                "-docletpath", System.getProperty("test.class.path"),
                "-doclet", "VarArgsTest$ThisDoclet",
                "pkg1");
        checkExit(Exit.OK);

        String expect = Files.readAllLines(Path.of(testSrc).resolve("expected.out"))
                .stream()
                .collect(Collectors.joining("\n"));
        checkOutput(Output.STDOUT, true, expect);
    }


    public static class ThisDoclet extends TestDoclet {
        public boolean run(DocletEnvironment env) {
            DocTrees trees = env.getDocTrees();
            try {
                for (Element e : env.getIncludedElements()) {
                    if (e.getKind() == ElementKind.INTERFACE) {
                        printClass((TypeElement) e);

                        TreePath tp = trees.getPath(e);
                        DocCommentTree dct = trees.getDocCommentTree(e);
                        DocTreePath dtp = new DocTreePath(tp, dct);

                        ExecutableElement m0 = ElementFilter.methodsIn(e.getEnclosedElements()).get(0);
                        for (DocTree t : dct.getBlockTags()) {
                            if (t.getKind() == DocTree.Kind.SEE) {
                                SeeTree st = (SeeTree) t;
                                DocTreePath sp = new DocTreePath(dtp, st.getReference().get(0));
                                Element se = trees.getElement(sp);
                                System.err.println("Expect: " + m0);
                                System.err.println("Found:  " + se);
                                if (se != m0) {
                                    throw new Error("unexpected value for @see reference");
                                }
                            }
                        }
                    }
                }

                return true;
            } catch (Exception e) {
                return false;
            }
        }

        // this method mimics the printClass method from the old
        // tester framework
        void printClass(TypeElement te) {
            PrintStream out = System.out;
            out.format("%s %s%n",
                    te.getKind().toString().toLowerCase(),
                    te.getQualifiedName());
            out.format("  name: %s / %s / %s%n",
                    te.getSimpleName(), te.asType(), te.getQualifiedName());
            out.format("  methods:%n");
            te.getEnclosedElements().stream()
                    .filter(e -> e.getKind() == ElementKind.METHOD)
                    .map(e -> (ExecutableElement) e)
                    .forEach(e -> out.format("    %s %s(%s)%n",
                            e.getReturnType(),
                            e.getSimpleName(),
                            e.getParameters().stream()
                                    .map(this::paramToString)
                                    .collect(Collectors.joining(", "))
                    ));

        }

        private String paramToString(Element e) {
            System.err.println("paramToString " + e);
            ExecutableElement m = (ExecutableElement) e.getEnclosingElement();
            return typeToString(m, e.asType());
        }

        private String typeToString(ExecutableElement method, TypeMirror t) {
            System.err.println("typeToString " + method + " " + t + " " + t.getKind());
            switch (t.getKind()) {
                case INT:
                    return t.getKind().toString().toLowerCase();

                case DECLARED:
                    return ((DeclaredType) t).asElement().getSimpleName().toString();

                case ARRAY:
                    String cs = typeToString(method, ((ArrayType) t).getComponentType());
                    String suffix = method.isVarArgs() ? "..." : "[]";
                    return cs + suffix;

                default:
                    throw new IllegalArgumentException(t.getKind() + " " + t);
            }
        }
    }
}
