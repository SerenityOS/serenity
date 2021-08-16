/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8166628
 * @summary Verify that loading a classfile for a local class that is a member of an anonymous class
 *          won't break compilation.
 * @modules jdk.compiler
 */

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

public class LocalInAnonymous {

    public static void main(String[] args) throws Exception {
        Path base = Paths.get(".").toAbsolutePath();
        Path classes = base.resolve("classes");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticListener<JavaFileObject> noErrors = d -> {
            if (d.getKind() == Diagnostic.Kind.ERROR) {
                throw new AssertionError(d.getMessage(null));
            }
        };
        List<TJFO> files = Arrays.asList(new TJFO("Test", CODE));
        List<String> options = Arrays.asList("-d", classes.toString());
        StringWriter out = new StringWriter();
        JavacTask task = (JavacTask) compiler.getTask(out, null, noErrors, options, null, files);
        task.call();
        if (!out.toString().isEmpty()) {
            throw new AssertionError("Unexpected output: " + out);
        }
        options = Arrays.asList("-classpath", classes.toString(), "-d", classes.toString());
        JavacTask task2 = (JavacTask) compiler.getTask(out, null, noErrors, options, null, files);
        task2.addTaskListener(new TaskListener() {
            @Override
            public void started(TaskEvent te) {
            }
            @Override
            public void finished(TaskEvent te) {
                if (te.getKind() == Kind.ENTER) {
                    Element pack = task2.getElements().getTypeElement("Test").getEnclosingElement();
                    System.err.println(pack.getEnclosedElements());
                }
                if (te.getKind() == Kind.ANALYZE) {
                    PackageElement pack = task2.getElements().getPackageOf(te.getTypeElement());
                    new OwnerCheck(Trees.instance(task2), pack).scan(te.getCompilationUnit(), null);
                }
            }
        });
        task2.call();
        if (!out.toString().isEmpty()) {
            throw new AssertionError("Unexpected output: " + out);
        }
        options = Arrays.asList("-classpath", classes.toString(),
                                "-d", classes.toString(),
                                "-processorpath", System.getProperty("test.classes"),
                                "-processor", Processor.class.getName());
        JavacTask task3 = (JavacTask) compiler.getTask(out, null, noErrors, options, null, files);
        task3.call();
        if (!out.toString().isEmpty()) {
            throw new AssertionError("Unexpected output: " + out);
        }
    }

    private static final class TJFO extends SimpleJavaFileObject {

        private final String code;

        public TJFO(String name, String code) throws URISyntaxException {
            super(new URI("mem:///" + name + ".java"), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return code;
        }

    }

    @SupportedAnnotationTypes("*")
    public static final class Processor extends AbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            Trees trees = Trees.instance(processingEnv);
            Elements elements = processingEnv.getElementUtils();
            Element pack = elements.getTypeElement("Test").getEnclosingElement();
            for (Element root : pack.getEnclosedElements()) {
                TreePath tp = trees.getPath(root);
                new OwnerCheck(trees, pack).scan(tp.getCompilationUnit(), null);

            }
            return false;
        }

    }

    private static final class OwnerCheck extends TreePathScanner<Void, Void> {
        private final Trees trees;
        private Element currentOwner;

        public OwnerCheck(Trees trees, Element pack) {
            this.trees = trees;
            this.currentOwner = pack;
        }

        @Override
        public Void visitClass(ClassTree node, Void p) {
            Element prevOwner = currentOwner;
            try {
                Element currentElement = trees.getElement(getCurrentPath());
                if (currentOwner != null && currentElement.getEnclosingElement() != currentOwner) {
                    throw new AssertionError("Unexpected owner!");
                }
                currentOwner = currentElement;
                return super.visitClass(node, p);
            } finally {
                currentOwner = prevOwner;
            }
        }

        @Override
        public Void visitMethod(MethodTree node, Void p) {
            Element prevOwner = currentOwner;
            try {
                Element currentElement = trees.getElement(getCurrentPath());
                if (currentElement.getEnclosingElement() != currentOwner) {
                    throw new AssertionError("Unexpected owner!");
                }
                currentOwner = currentElement;
                return super.visitMethod(node, p);
            } finally {
                currentOwner = prevOwner;
            }
        }

        @Override
        public Void visitVariable(VariableTree node, Void p) {
            Element currentElement = trees.getElement(getCurrentPath());
            if (!currentElement.getKind().isField()) {
                return super.visitVariable(node, p);
            }
            Element prevOwner = currentOwner;
            try {
                if (currentElement.getEnclosingElement() != currentOwner) {
                    throw new AssertionError("Unexpected owner!");
                }
                currentOwner = currentElement;
                return super.visitVariable(node, p);
            } finally {
                currentOwner = prevOwner;
            }
        }

        @Override
        public Void visitBlock(BlockTree node, Void p) {
            if (getCurrentPath().getParentPath().getLeaf().getKind() != Tree.Kind.CLASS) {
                return super.visitBlock(node, p);
            }
            Element prevOwner = currentOwner;
            try {
                currentOwner = null;
                return super.visitBlock(node, p);
            } finally {
                currentOwner = prevOwner;
            }
        }

    }

    private static final String CODE =
            "public class Test {\n" +
            "   void test() {\n" +
            "       Object o = new Object() {\n" +
            "           class IC {}\n" +
            "           public Object get() {\n" +
            "               return new IC();\n" +
            "           }\n" +
            "       };\n" +
            "   }\n" +
            "   {\n" +
            "       Object o = new Object() {\n" +
            "           class IC {}\n" +
            "           public Object get() {\n" +
            "               return new IC();\n" +
            "           }\n" +
            "       };\n" +
            "   }\n" +
            "   static {\n" +
            "       Object o = new Object() {\n" +
            "           class IC {}\n" +
            "           public Object get() {\n" +
            "               return new IC();\n" +
            "           }\n" +
            "       };\n" +
            "   }\n" +
            "   Object o1 = new Object() {\n" +
            "       class IC {}\n" +
            "       public Object get() {\n" +
            "          return new IC();\n" +
            "       }\n" +
            "   };\n" +
            "   static Object o2 = new Object() {\n" +
            "       class IC {}\n" +
            "       public Object get() {\n" +
            "          return new IC();\n" +
            "       }\n" +
            "   };\n" +
            "}";
}
