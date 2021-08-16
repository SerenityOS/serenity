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
 * @bug     7029150 7025809
 * @summary Test support for union types
 * @library /tools/javac/lib
 * @modules jdk.compiler
 */

import java.net.URI;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;
import javax.tools.*;

import com.sun.source.tree.*;
import com.sun.source.util.*;

public class TestUnionType extends JavacTestingAbstractProcessor {
    enum TestKind {
        SingleType("E1", "E1",
                "VariableTree: E1 e",
                "VariableTree: elem EXCEPTION_PARAMETER e",
                "VariableTree: elem.type DECLARED",
                "VariableTree: elem.type.elem CLASS E1",
                "VariableTree: type DECLARED",
                "VariableTree: type.elem CLASS E1",
                "VariableTree: type.elem.type DECLARED"),

        ValidTypes("E1, E2", "E1 | E2",
                "VariableTree: E1 | E2 e",
                "VariableTree: elem EXCEPTION_PARAMETER e",
                "VariableTree: elem.type UNION Test.E1,Test.E2",
                "VariableTree: elem.type.elem null",
                "VariableTree: type UNION Test.E1,Test.E2",
                "VariableTree: type.elem null"),

        InvalidTypes("E1, E2", "E1 | EMissing",
                "VariableTree: E1 | EMissing e",
                "VariableTree: elem EXCEPTION_PARAMETER e",
                "VariableTree: elem.type UNION Test.E1,EMissing",
                "VariableTree: elem.type.elem null",
                "VariableTree: type UNION Test.E1,EMissing",
                "VariableTree: type.elem null"),

        Uncaught("E1", "E1 | E2",
                "VariableTree: E1 | E2 e",
                "VariableTree: elem EXCEPTION_PARAMETER e",
                "VariableTree: elem.type UNION Test.E1,Test.E2",
                "VariableTree: elem.type.elem null",
                "VariableTree: type UNION Test.E1,Test.E2",
                "VariableTree: type.elem null");

        TestKind(String throwsTypes, String catchTypes, String... gold) {
            this.throwsTypes = throwsTypes;
            this.catchTypes = catchTypes;
            this.gold = Arrays.asList(gold);
        }

        final String throwsTypes;
        final String catchTypes;
        final List<String> gold;
    }

    static class TestFileObject extends SimpleJavaFileObject {
        public static final String template =
                  "class Test {\n"
                + "    class E1 extends Exception { }\n"
                + "    class E2 extends Exception { }\n"
                + "    void doSomething() throws #T { }\n"
                + "    void test() {\n"
                + "        try {\n"
                + "            doSomething();\n"
                + "        } catch (#C e) {\n"
                + "        }\n"
                + "    }\n"
                + "}\n";

        public TestFileObject(TestKind tk) {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            this.tk = tk;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return template
                    .replace("#T", tk.throwsTypes)
                    .replace("#C", tk.catchTypes);
        }
        final TestKind tk;
    }

    public static void main(String... args) throws Exception {
        JavaCompiler comp = ToolProvider.getSystemJavaCompiler();
        List<String> options = Arrays.asList("-proc:only");
        for (TestKind tk: TestKind.values()) {
            System.err.println("Test: " + tk);
            TestUnionType p = new TestUnionType();
            JavaFileObject fo = new TestFileObject(tk);
            JavaCompiler.CompilationTask task = comp.getTask(null, null, null, options, null, Arrays.asList(fo));
            task.setProcessors(Arrays.asList(p));
            boolean ok = task.call();
            System.err.println("compilation " + (ok ? "passed" : "failed"));
            if (!ok)
                throw new Exception("compilation failed unexpectedly");
            if (!p.log.equals(tk.gold)) {
                System.err.println("Expected output:");
                for (String g: tk.gold)
                    System.err.println(g);
                throw new Exception("unexpected output from test");
            }
            System.err.println();
        }
    }

    Trees trees;
    List<String> log;

    @Override
    public void init(ProcessingEnvironment env) {
        super.init(env);
        trees = Trees.instance(env);
        log = new ArrayList<String>();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            for (Element e: roundEnv.getRootElements()) {
                scan(trees.getPath(e));
            }
        }
        return true;
    }

    void scan(TreePath path) {
        new Scanner().scan(path, null);
    }

    class Scanner extends TreePathScanner<Void,Void> {
        @Override
        public Void visitVariable(VariableTree tree, Void ignore) {
            TreePath p = getCurrentPath();
            Element e = trees.getElement(p);
            if (e.getKind() == ElementKind.EXCEPTION_PARAMETER) {
                log("VariableTree: " + tree);
                log("VariableTree: elem " + print(e));
                log("VariableTree: elem.type " + print(e.asType()));
                log("VariableTree: elem.type.elem " + print(types.asElement(e.asType())));
                TypeMirror tm = trees.getTypeMirror(p);
                log("VariableTree: type " + print(tm));
                log("VariableTree: type.elem " + print(types.asElement(tm)));
                if (types.asElement(tm) != null)
                    log("VariableTree: type.elem.type " + print(types.asElement(tm).asType()));
            }
            return super.visitVariable(tree, null);
        }

        String print(TypeMirror tm) {
            return (tm == null) ? null : new TypePrinter().visit(tm);
        }

        String print(Element e) {
            return (e == null) ? null : (e.getKind() + " " + e.getSimpleName());
        }

        void log(String msg) {
            System.err.println(msg);
            log.add(msg);
        }
    }

    class TypePrinter extends SimpleTypeVisitor<String, Void> {
        @Override
        protected String defaultAction(TypeMirror tm, Void ignore) {
            return String.valueOf(tm.getKind());
        }

        @Override
        public String visitUnion(UnionType t, Void ignore) {
            return (t.getKind() + " " + t.getAlternatives());
        }
    }
}
