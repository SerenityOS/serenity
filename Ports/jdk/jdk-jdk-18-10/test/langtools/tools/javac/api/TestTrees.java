/*
 * Copyright (c) 2006, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6346249 6392177 6411385
 * @summary new Trees API
 * @modules jdk.compiler/com.sun.tools.javac.tree
 */

import java.io.*;
import java.lang.annotation.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.tools.*;

import com.sun.source.tree.*;
import com.sun.source.util.*;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;

@Anno
@SupportedAnnotationTypes("*")
public class TestTrees extends AbstractProcessor {
    @Anno
    void annoMethod() { }

    @Anno
    int annoField;

    @Anno
    public TestTrees() {
    }

    static final String testSrcDir = System.getProperty("test.src");
    static final String testClassDir = System.getProperty("test.classes");
    static final String self = TestTrees.class.getName();
    static PrintWriter out = new PrintWriter(System.err, true);

    public static void main(String[] args) throws IOException {
        new TestTrees().run();
    }

    void run() throws IOException {

        JavaCompiler tool = ToolProvider.getSystemJavaCompiler();

        DiagnosticListener<JavaFileObject> dl = new DiagnosticListener<JavaFileObject>() {
                public void report(Diagnostic d) {
                    error(d.toString());
                }
            };

        try (StandardJavaFileManager fm = tool.getStandardFileManager(dl, null, null)) {
            Iterable<? extends JavaFileObject> files =
                fm.getJavaFileObjectsFromFiles(Arrays.asList(new File(testSrcDir, self + ".java")));

            Iterable<String> opts = Arrays.asList(
                "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
                "-d", ".",
                "-XDcompilePolicy=simple");

            System.err.println("simple compilation, no processing");
            JavacTask task = (JavacTask) tool.getTask(out, fm, dl, opts, null, files);
            task.setTaskListener(new MyTaskListener(task));
            if (!task.call())
                throw new AssertionError("compilation failed");

            opts =  Arrays.asList(
                "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
                "-XDaccessInternalAPI",
                "-d", ".",
                "-processorpath", testClassDir,
                "-processor", self,
                "-XDcompilePolicy=simple");

            System.err.println();
            System.err.println("compilation with processing");
            task = (JavacTask) tool.getTask(out, fm, dl,opts, null, files);
            if (!task.call())
                throw new AssertionError("compilation failed");

            if (errors > 0)
                throw new AssertionError(errors + " errors occurred");
        }
    }

    void testElement(Trees trees, Element e) {
        trees.getClass();
        e.getClass();

        System.err.println("testElement: " + e);
        Tree tree = trees.getTree(e);
        //System.err.println(tree);

        if (TreeInfo.symbolFor((JCTree)tree) != e)
            error("bad result from getTree");

        TreePath path = trees.getPath(e);
        if (path == null) {
            error("getPath returned null");
            return;
        }
        if (path.getLeaf() != tree)
            error("bad result from getPath");

        Element e2 = trees.getElement(path);
        if (e2 == null) {
            error("getElement returned null");
            return;
        }
        if (e2 != e)
            error("bad result from getElement");

        // The TypeMirror is not available yet when annotation processing;
        // it is set up later during ANALYSE.
        TypeMirror t = trees.getTypeMirror(path);
        if (t != null && t.getKind() == TypeKind.DECLARED &&
                ((DeclaredType)t).asElement() != e2)
            error("bad result from getTypeMirror");

        for (AnnotationMirror m: e.getAnnotationMirrors()) {
            testAnnotation(trees, e, m);
        }
    }

    void testAnnotation(Trees trees, Element e, AnnotationMirror a) {
        System.err.println("testAnnotation: " + e + " " + a);
        Tree tree = trees.getTree(e, a);

        if (tree.getKind() != Tree.Kind.ANNOTATION && tree.getKind() != Tree.Kind.TYPE_ANNOTATION)
            error("bad result from getTree");

        TreePath path = trees.getPath(e, a);
        if (path.getLeaf() != tree)
            error("bad result from getPath");
    }

    void testAllDeclarations(Trees trees, CompilationUnitTree cut) {
        new TreePathScanner<Void, Void>() {
            @Override public Void scan(Tree tree, Void p) {
                if (tree == null) return null;
                switch (tree.getKind()) {
                    case METHOD: case CLASS: case VARIABLE: case TYPE_PARAMETER:
                        TreePath path = new TreePath(getCurrentPath(), tree);
                        Element el = trees.getElement(path);
                        if (el == null) {
                            error("null element");
                        } else {
                            TreePath inferred = trees.getPath(el);
                            if (inferred == null) {
                                error("null path");
                            } else {
                                if (inferred.getLeaf() != path.getLeaf())
                                    error("bad result from getPath");
                            }
                            if (trees.getTree(el) != path.getLeaf())
                                error("bad result from getTree");
                            for (AnnotationMirror m: el.getAnnotationMirrors()) {
                                testAnnotation(trees, el, m);
                            }
                        }
                }
                return super.scan(tree, p);
            }
        }.scan(cut, null);
    }

    void error(String msg) {
        if (messager != null)
            // annotation processing will happen in a separate instance/classloader
            // so pass the message back to the calling instance.
            messager.printMessage(Diagnostic.Kind.ERROR, msg);
        else {
            System.err.println(msg);
            errors++;
        }

    }

    Messager messager;
    int errors;


    public boolean process(Set<? extends TypeElement> annos, RoundEnvironment rEnv) {
        Trees trees = Trees.instance(processingEnv);
        messager = processingEnv.getMessager();

        for (Element e: rEnv.getRootElements()) {
            testElement(trees, e);
        }

        for (TypeElement anno: annos) {
            Set<? extends Element> elts = rEnv.getElementsAnnotatedWith(anno);
            System.err.println("anno: " + anno);
            System.err.println("elts: " + elts);
            if (elts != null) { // 6397298, should return empty set
                for (Element e: rEnv.getElementsAnnotatedWith(anno))
                    testElement(trees, e);
            }
        }

        return true;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    class MyTaskListener implements TaskListener {
        MyTaskListener(JavacTask task) {
            this.task = task;
        }

        public void started(TaskEvent e) {
            System.err.println("started " + e);
        }

        public void finished(TaskEvent e) {
            //System.err.println("finished " + e);
            switch (e.getKind()) {
            case ANALYZE:
                testElement(Trees.instance(task), e.getTypeElement());
                testAllDeclarations(Trees.instance(task), e.getCompilationUnit());
                break;
            }
        }

        private final JavacTask task;
    }

    public static class TestTypeParams<@Anno T extends CharSequence> {
        public <@Anno T extends Object> TestTypeParams(T param) { }
        public <@Anno T extends Number> void m(T param) {
            int local;
            try {
                new String();
            } catch (Exception exc) { }
        }
    }
}

@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.TYPE_PARAMETER,
         ElementType.FIELD, ElementType.LOCAL_VARIABLE})
@interface Anno {
}
