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
 * @summary Verify errors are not reported when computing Scopes.
 * @modules jdk.compiler
 */

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import static javax.tools.JavaFileObject.Kind.SOURCE;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

public class TestGetScopeErrors {
    public static void main(String... args) throws IOException {
        new TestGetScopeErrors().run();
    }

    void run() throws IOException {
        JavaCompiler c = ToolProvider.getSystemJavaCompiler();
        String code =
                "public class Test {" +
                "    private Object obj = new Object() {" +
                "        private Unresolvable u;" +
                "    };" +
                "    void test() {" +
                "        new Object() {" +
                "            private Unresolvable u;" +
                "        };" +
                "    }" +
                "}";
        class MyFileObject extends SimpleJavaFileObject {
            MyFileObject() {
                super(URI.create("myfo:///Test.java"), SOURCE);
            }
            @Override
            public String getCharContent(boolean ignoreEncodingErrors) {
                return code;
            }
        }
        AtomicBoolean enterDone = new AtomicBoolean();
        List<String> errors = new ArrayList<>();
        DiagnosticListener<JavaFileObject> noErrors = d -> {
            if (!enterDone.get() && d.getKind() == Diagnostic.Kind.ERROR) {
                throw new AssertionError(d.toString());
            }
            errors.add(d.getSource().getName() + ":" +
                       d.getPosition() + ":" +
                       d.getCode());
        };
        JavacTask t =
                (JavacTask) c.getTask(null, null, noErrors,
                                      Arrays.asList("-XDrawDiagnostics"),
                                      null, List.of(new MyFileObject()));
        CompilationUnitTree cut = t.parse().iterator().next();
        Trees trees = Trees.instance(t);
        t.addTaskListener(new TaskListener() {
            @Override
            public void finished(TaskEvent e) {
                if (e.getKind() == TaskEvent.Kind.ENTER) {
                    new TreePathScanner<Void, Void>() {
                        @Override
                        public Void scan(Tree tree, Void p) {
                            if (tree != null) {
                                TreePath path =
                                        new TreePath(getCurrentPath(), tree);
                                trees.getScope(path);
                            }
                            return super.scan(tree, p);
                        }
                    }.scan(cut, null);
                    enterDone.set(true);
                }
            }
        });

        t.analyze();

        List<String> expectedErrors = List.of(
            "/Test.java:74:compiler.err.cant.resolve",
            "/Test.java:154:compiler.err.cant.resolve"
        );

        if (!expectedErrors.equals(errors)) {
            throw new IllegalStateException("Unexpected errors: " + errors);
        }
    }

}
