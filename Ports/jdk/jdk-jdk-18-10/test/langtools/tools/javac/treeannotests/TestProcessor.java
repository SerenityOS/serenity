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

import java.io.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.lang.model.util.*;
import javax.tools.*;

import com.sun.source.util.*;
import com.sun.tools.javac.code.BoundKind;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.util.List;

/**
 * Test processor used to check test programs using the @Test, @DA, and @TA
 * annotations.
 *
 * The processor looks for elements annotated with @Test, and analyzes the
 * syntax trees for those elements. Within such trees, the processor looks
 * for the DA annotations on decls and TA annotations on types.
 * The value of these annotations should be a simple string rendition of
 * the tree node to which it is attached.
 * The expected number of annotations is given by the parameter to the
 * @Test annotation itself.
 */
@SupportedAnnotationTypes({"Test"})
public class TestProcessor extends AbstractProcessor {
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    /** Process trees for elements annotated with the @Test(n) annotation. */
    public boolean process(Set<? extends TypeElement> annos, RoundEnvironment renv) {
        if (renv.processingOver())
            return true;

        Elements elements = processingEnv.getElementUtils();
        Trees trees = Trees.instance(processingEnv);

        TypeElement testAnno = elements.getTypeElement("Test");
        for (Element elem: renv.getElementsAnnotatedWith(testAnno)) {
            System.err.println("ELEM: " + elem);
            int count = getValue(getAnnoMirror(elem, testAnno), Integer.class);
            System.err.println("count: " + count);
            TreePath p = trees.getPath(elem);
            JavaFileObject file = p.getCompilationUnit().getSourceFile();
            JCTree tree = (JCTree) p.getLeaf();
            System.err.println("tree: " + tree);
            new TestScanner(file).check(tree, count);
        }
        return true;
    }

    /** Get the AnnotationMirror on an element for a given annotation. */
    AnnotationMirror getAnnoMirror(Element e, TypeElement anno) {
        Types types = processingEnv.getTypeUtils();
        for (AnnotationMirror m: e.getAnnotationMirrors()) {
            if (types.isSameType(m.getAnnotationType(), anno.asType()))
                return m;
        }
        return null;
    }

    /** Get the value of the value element of an annotation mirror. */
    <T> T getValue(AnnotationMirror m, Class<T> type) {
        for (Map.Entry<? extends ExecutableElement,? extends AnnotationValue> e: m.getElementValues().entrySet()) {
            ExecutableElement ee = e.getKey();
            if (ee.getSimpleName().contentEquals("value")) {
                AnnotationValue av = e.getValue();
                return type.cast(av.getValue());
            }
        }
        return null;
    }

    /** Report an error to the annotation processing system. */
    void error(String msg) {
        Messager messager = processingEnv.getMessager();
        messager.printMessage(Diagnostic.Kind.ERROR, msg);
    }

    /** Report an error to the annotation processing system. */
    void error(JavaFileObject file, JCTree tree, String msg) {
        // need better API for reporting tree position errors to the messager
        Messager messager = processingEnv.getMessager();
        String text = file.getName() + ":" + getLine(file, tree) + ": " + msg;
        messager.printMessage(Diagnostic.Kind.ERROR, text);
    }

    /** Get the line number for the primary position for a tree.
     * The code is intended to be simple, although not necessarily efficient.
     * However, note that a file manager such as JavacFileManager is likely
     * to cache the results of file.getCharContent, avoiding the need to read
     * the bits from disk each time this method is called.
     */
    int getLine(JavaFileObject file, JCTree tree) {
        try {
            CharSequence cs = file.getCharContent(true);
            int line = 1;
            for (int i = 0; i < tree.pos; i++) {
                if (cs.charAt(i) == '\n') // jtreg tests always use Unix line endings
                    line++;
            }
            return line;
        } catch (IOException e) {
            return -1;
        }
    }

    /** Scan a tree, looking for @DA and @TA annotations, and verifying that such
     * annotations are attached to the expected tree node matching the string
     * parameter of the annotation.
     */
    class TestScanner extends TreeScanner {
        /** Create a scanner for a given file. */
        TestScanner(JavaFileObject file) {
            this.file = file;
        }

        /** Check the annotations in a given tree. */
        void check(JCTree tree, int expectCount) {
            foundCount = 0;
            scan(tree);
            if (foundCount != expectCount)
                error(file, tree, "Wrong number of annotations found: " + foundCount + ", expected: " + expectCount);
        }

        /** Check @DA annotations on a class declaration. */
        @Override
        public void visitClassDef(JCClassDecl tree) {
            super.visitClassDef(tree);
            check(tree.mods.annotations, "DA", tree);
        }

        /** Check @DA annotations on a method declaration. */
        @Override
        public void visitMethodDef(JCMethodDecl tree) {
            super.visitMethodDef(tree);
            check(tree.mods.annotations, "DA", tree);
        }

        /** Check @DA annotations on a field, parameter or local variable declaration. */
        @Override
        public void visitVarDef(JCVariableDecl tree) {
            super.visitVarDef(tree);
            check(tree.mods.annotations, "DA", tree);
        }

        /** Check @TA annotations on a type. */
        public void visitAnnotatedType(JCAnnotatedType tree) {
            super.visitAnnotatedType(tree);
            check(tree.annotations, "TA", tree);
        }

        /** Check to see if a list of annotations contains a named annotation, and
         * if so, verify the annotation is expected by comparing the value of the
         * annotation's argument against the string rendition of the reference tree
         * node.
         * @param annos the list of annotations to be checked
         * @param name  the name of the annotation to be checked
         * @param tree  the tree against which to compare the annotations's argument
         */
        void check(List<? extends JCAnnotation> annos, String name, JCTree tree) {
            for (List<? extends JCAnnotation> l = annos; l.nonEmpty(); l = l.tail) {
                JCAnnotation anno = l.head;
                if (anno.annotationType.toString().equals(name) && (anno.args.size() == 1)) {
                    String expect = getStringValue(anno.args.head);
                    foundCount++;
                    System.err.println("found: " + name + " " + expect);
                    String found = new TypePrinter().print(tree);
                    if (!found.equals(expect))
                        error(file, anno, "Unexpected result: expected: \"" + expect + "\", found: \"" + found + "\"");
                }
            }
        }

        /** Get the string value of an annotation argument, which is given by the
         * expression <i>name</i>=<i>value</i>.
         */
        String getStringValue(JCExpression e) {
            if (e.hasTag(JCTree.Tag.ASSIGN)) {
                JCAssign a = (JCAssign) e;
                JCExpression rhs = a.rhs;
                if (rhs.hasTag(JCTree.Tag.LITERAL)) {
                    JCLiteral l = (JCLiteral) rhs;
                    return (String) l.value;
                }
            } else if (e.hasTag(JCTree.Tag.LITERAL)) {
                JCLiteral l = (JCLiteral) e;
                return (String) l.value;
            }
            throw new IllegalArgumentException(e.toString());
        }

        /** The file for the tree. Used to locate errors. */
        JavaFileObject file;
        /** The number of annotations that have been found. @see #check */
        int foundCount;
    }

    /** Convert a type or decl tree to a reference string used by the @DA and @TA annotations. */
    class TypePrinter extends Visitor {
        /** Convert a type or decl tree to a string. */
        String print(JCTree tree) {
            if (tree == null)
                return null;
            tree.accept(this);
            return result;
        }

        String print(List<? extends JCTree> list) {
            return print(list, ", ");
        }

        String print(List<? extends JCTree> list, String sep) {
            StringBuilder sb = new StringBuilder();
            if (list.nonEmpty()) {
                sb.append(print(list.head));
                for (List<? extends JCTree> l = list.tail; l.nonEmpty(); l = l.tail) {
                    sb.append(sep);
                    sb.append(print(l.head));
                }
            }
            return sb.toString();
        }

        @Override
        public void visitClassDef(JCClassDecl tree) {
            result = tree.name.toString();
        }

        @Override
        public void visitMethodDef(JCMethodDecl tree) {
            result = tree.name.toString();
        }

        @Override
        public void visitVarDef(JCVariableDecl tree) {
            tree.vartype.accept(this);
        }

        @Override
        public void visitAnnotatedType(JCAnnotatedType tree) {
            tree.underlyingType.accept(this);
        }

        @Override
        public void visitTypeIdent(JCPrimitiveTypeTree tree) {
            result = tree.toString();
        }

        @Override
        public void visitTypeArray(JCArrayTypeTree tree) {
            result = print(tree.elemtype) + "[]";
        }

        @Override
        public void visitTypeApply(JCTypeApply tree) {
            result = print(tree.clazz) + "<" + print(tree.arguments) + ">";
        }

        @Override
        public void visitTypeParameter(JCTypeParameter tree) {
            if (tree.bounds.isEmpty())
                result = tree.name.toString();
            else
                result = tree.name + " extends " + print(tree.bounds, "&");
        }

        @Override
        public void visitWildcard(JCWildcard tree) {
            if (tree.kind.kind == BoundKind.UNBOUND)
                result = tree.kind.toString();
            else
                result = tree.kind + " " + print(tree.inner);
        }

        private String result;
    }
}
