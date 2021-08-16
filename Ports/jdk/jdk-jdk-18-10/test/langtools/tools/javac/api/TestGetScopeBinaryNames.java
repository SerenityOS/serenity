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
 * @bug 8223443
 * @summary Verify binary names are not changed and are correct
 *          when using Trees.getScope
 * @modules jdk.compiler
 */

import com.sun.source.tree.ClassTree;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.JavaCompiler;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

import static javax.tools.JavaFileObject.Kind.SOURCE;

public class TestGetScopeBinaryNames {
    public static void main(String... args) throws IOException {
        new TestGetScopeBinaryNames().run();
    }

    public void run() throws IOException {
        class EnclosingDesc {
            final String code;
            final boolean supportsLocal;
            public EnclosingDesc(String code, boolean supportsLocal) {
                this.code = code;
                this.supportsLocal = supportsLocal;
            }
        }
        List<EnclosingDesc> enclosingEnvs = List.of(
                new EnclosingDesc("class Test {" +
                                  "    void test() {" +
                                  "        $" +
                                  "    }" +
                                  "}",
                                  true),
                new EnclosingDesc("class Test {" +
                                  "    {" +
                                  "        $" +
                                  "    }" +
                                  "}",
                                  true),
                new EnclosingDesc("class Test {" +
                                  "    static {" +
                                  "        $" +
                                  "    }" +
                                  "}",
                                  true),
                new EnclosingDesc("class Test {" +
                                  "    Object I = $" +
                                  "}",
                                  true)
        );
        class LocalDesc {
            final String localCode;
            final boolean isLocalClass;
            public LocalDesc(String localCode, boolean isLocalClass) {
                this.localCode = localCode;
                this.isLocalClass = isLocalClass;
            }
        }
        List<LocalDesc> locals = List.of(
            new LocalDesc("new A() {" +
                          "    class AI extends B {" +
                          "        class AII extends C {" +
                          "            private void t() {" +
                          "                new D() { class DI extends E {} };" +
                          "            }" +
                          "        }" +
                          "        private void t() { new F() {}; }" +
                          "    }" +
                          "    private void t() { new G() {}; }" +
                          "};",
                          false),
            new LocalDesc("class AA extends A {" +
                          "    class AI extends B {" +
                          "        class AII extends C {" +
                          "            private void t() {" +
                          "                new D() { class DI extends E {} };" +
                          "            }" +
                          "        }" +
                          "        private void t() { new F() {}; }" +
                          "    }" +
                          "    private void t() { new G() {}; }" +
                          "}",
                          false)
        );
        String markerClasses = "class A {} class B {} class C {}" +
                               "class D {} class E {} class F {}" +
                               "class G {}";
        for (EnclosingDesc enclosing : enclosingEnvs) {
            for (LocalDesc local : locals) {
                if (!local.isLocalClass || enclosing.supportsLocal) {
                    doTest(enclosing.code.replace("$", local.localCode) +
                           markerClasses);
                }
            }
        }
    }

    void doTest(String code, String... expected) throws IOException {
        Map<String, String> name2BinaryName = new HashMap<>();
        Map<String, String> name2QualifiedName = new HashMap<>();

        computeNames(code, name2BinaryName, name2QualifiedName);

        JavaCompiler c = ToolProvider.getSystemJavaCompiler();
        JavacTask t = (JavacTask) c.getTask(null, null, null, null, null,
                                            List.of(new MyFileObject(code)));
        CompilationUnitTree cut = t.parse().iterator().next();
        Trees trees = Trees.instance(t);

        t.addTaskListener(new TaskListener() {
            @Override
            public void finished(TaskEvent e) {
                if (e.getKind() == TaskEvent.Kind.ENTER) {
                    new TreePathScanner<Void, Void>() {
                        @Override
                        public Void scan(Tree tree, Void p) {
                            if (tree != null &&
                                !isInExtendsClause(getCurrentPath(), tree)) {
                                TreePath path =
                                        new TreePath(getCurrentPath(), tree);
                                Scope scope = trees.getScope(path);
                                checkScope(t.getElements(), scope,
                                           name2BinaryName, name2QualifiedName);
                            }
                            return super.scan(tree, p);
                        }
                    }.scan(cut, null);
                }
            }
        });

        t.analyze();

        new TreePathScanner<Void, Void>() {
            @Override
            public Void visitClass(ClassTree node, Void p) {
                TypeElement type =
                        (TypeElement) trees.getElement(getCurrentPath());
                checkClass(t.getElements(), type,
                           name2BinaryName, name2QualifiedName);
                return super.visitClass(node, p);
            }
        }.scan(cut, null);

        new TreePathScanner<Void, Void>() {
            @Override
            public Void scan(Tree tree, Void p) {
                if (tree != null &&
                    !isInExtendsClause(getCurrentPath(), tree)) {
                    TreePath path =
                            new TreePath(getCurrentPath(), tree);
                    Scope scope = trees.getScope(path);
                    checkScope(t.getElements(), scope,
                               name2BinaryName, name2QualifiedName);
                }
                return super.scan(tree, p);
            }
        }.scan(cut, null);
    }

    void computeNames(String code,
                      Map<String, String> name2BinaryName,
                      Map<String, String> name2QualifiedName) throws IOException {
        JavaCompiler c = ToolProvider.getSystemJavaCompiler();
        JavacTask t = (JavacTask) c.getTask(null, null, null, null, null,
                                            List.of(new MyFileObject(code)));
        CompilationUnitTree cut = t.parse().iterator().next();

        t.analyze();

        new TreePathScanner<Void, Void>() {
            Trees trees = Trees.instance(t);
            Elements els = t.getElements();
            @Override
            public Void visitClass(ClassTree node, Void p) {
                TypeElement type =
                        (TypeElement) trees.getElement(getCurrentPath());
                String key = type.getSuperclass().toString();

                name2BinaryName.put(key, els.getBinaryName(type).toString());
                name2QualifiedName.put(key, type.getQualifiedName().toString());
                return super.visitClass(node, p);
            }
        }.scan(cut, null);
    }

    boolean isInExtendsClause(TreePath clazz, Tree toCheck) {
        return clazz != null &&
               clazz.getLeaf().getKind() == Kind.CLASS &&
               ((ClassTree) clazz.getLeaf()).getExtendsClause() == toCheck;
    }

    void checkClass(Elements els, TypeElement type,
                    Map<String, String> name2BinaryName,
                    Map<String, String> name2QualifiedName) {
        if (type.getNestingKind() == NestingKind.TOP_LEVEL ||
            type.getNestingKind() == NestingKind.MEMBER) {
            return ;
        }

        String binaryName = name2BinaryName.get(type.getSuperclass().toString());

        if (!els.getBinaryName(type).contentEquals(binaryName)) {
            throw new AssertionError("Unexpected: " + els.getBinaryName(type));
        }

        String qualifiedName = name2QualifiedName.get(type.getSuperclass().toString());

        if (qualifiedName != null) {
            if (!type.getQualifiedName().contentEquals(qualifiedName)) {
                throw new AssertionError("Unexpected: " + type.getQualifiedName() +
                                         ", expected: " + qualifiedName);
            }
        }
    }

    void checkScope(Elements els, Scope scope,
                    Map<String, String> name2BinaryName,
                    Map<String, String> name2QualifiedName) {
        while (scope != null) {
            for (Element el : scope.getLocalElements()) {
                if (el.getKind().isClass()) {
                    checkClass(els, (TypeElement) el,
                               name2BinaryName, name2QualifiedName);
                }
            }
            scope = scope.getEnclosingScope();
        }
    }

    class MyFileObject extends SimpleJavaFileObject {
        private final String code;

        MyFileObject(String code) {
            super(URI.create("myfo:///Test.java"), SOURCE);
            this.code = code;
        }
        @Override
        public String getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
}

