/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6413682
 * @summary Compiler confused about implicit type args and arrays
 * @author  Peter von der Ah\u00e9
 * @modules jdk.compiler
 */

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ErroneousTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import static javax.tools.JavaFileObject.Kind.SOURCE;
import javax.tools.ToolProvider;

public class TestPos {

    static final String errCode = "compiler.err.cannot.create.array.with.type.arguments";
    static final String expected =
        String.format("%s%n%s%n",
                      "compiler.err.cannot.create.array.with.type.arguments @ 33",
                      "begin=28, end=50 : new Object[0],T,e,s,t");

    public static void main(String... args) throws IOException {
        final boolean[] sawError = { false };
        final StringBuilder log = new StringBuilder();
        class MyFileObject extends SimpleJavaFileObject {
            MyFileObject() {
                super(URI.create("myfo:///Test.java"), SOURCE);
            }
            @Override
            public String getCharContent(boolean ignoreEncodingErrors) {
                //      0         1         2         3         4         5
                //      0123456789012345678901234567890123456789012345678901234
                return "class Test { { Object[] o = new <T,e,s,t>Object[0]; } }";
            }
        }
        class Scanner extends TreeScanner<Void,Trees> {
            CompilationUnitTree toplevel = null;
            @Override
            public Void visitCompilationUnit(CompilationUnitTree node, Trees trees) {
                toplevel = node;
                return super.visitCompilationUnit(node, trees);
            }
            @Override
            public Void visitErroneous(ErroneousTree node, Trees trees) {
                sawError[0] = true;
                long startPos = trees.getSourcePositions().getStartPosition(toplevel, node);
                long endPos = trees.getSourcePositions().getEndPosition(toplevel, node);
                log.append(String.format("begin=%s, end=%s : %s%n",
                                         startPos,
                                         endPos,
                                         node.getErrorTrees()));
                if (startPos != 28)
                    error("Start pos for %s is incorrect (%s)!", node, startPos);
                if (endPos != 50)
                    error("End pos for %s is incorrect (%s)!", node, endPos);
                return null;
            }
        }
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        List<JavaFileObject> compilationUnits =
                Collections.<JavaFileObject>singletonList(new MyFileObject());
        DiagnosticListener<JavaFileObject> dl = new DiagnosticListener<JavaFileObject>() {
            public void report(Diagnostic<? extends JavaFileObject> diag) {
                log.append(String.format("%s @ %s%n", diag.getCode(), diag.getPosition()));
                if (!diag.getCode().equals(errCode))
                    error("unexpected error");
                if (diag.getPosition() != 33)
                    error("Error pos for %s is incorrect (%s)!",
                          diag.getCode(), diag.getPosition());
                sawError[0] = true;
            }
        };
        JavacTask task = (JavacTask)javac.getTask(null, null, dl, null, null,
                                                  compilationUnits);
        Trees trees = Trees.instance(task);
        Iterable<? extends Tree> toplevels = task.parse();
        if (!sawError[0])
            error("No parse error detected");
        sawError[0] = false;
        new Scanner().scan(toplevels, trees);
        if (!sawError[0])
            error("No error tree detected");
        if (!log.toString().equals(expected))
            error("Unexpected log message: %n%s%n", log);
        System.out.print(log);
        System.out.flush();
    }

    static void error(String format, Object... args) {
        throw new AssertionError(String.format(format, args));
    }

}
