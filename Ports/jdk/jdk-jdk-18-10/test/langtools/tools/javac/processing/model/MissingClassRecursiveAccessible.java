/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8196519
 * @summary Verify that enclosing type of an ErrorType is not the ErrorType itself.
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.TestRunner toolbox.ToolBox MissingClassRecursiveAccessible
 * @run main MissingClassRecursiveAccessible
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.*;
import toolbox.JavacTask;
import toolbox.TestRunner;
import toolbox.TestRunner.Test;
import toolbox.ToolBox;

public class MissingClassRecursiveAccessible extends TestRunner {

    public static void main(String... args) throws Exception {
        new MissingClassRecursiveAccessible().runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    private final ToolBox tb = new ToolBox();

    public MissingClassRecursiveAccessible() {
        super(System.err);
    }

    @Test
    public void testMissingClass(Path outerBase) throws Exception {
        Path src = outerBase.resolve("src");
        tb.writeJavaFiles(src,
                          "import java.util.Optional;\n" +
                          "\n" +
                          "abstract class B {\n" +
                          "  abstract <M> Optional<M> g(F<M> p);\n" +
                          "}\n",
                          "interface F<M> {\n" +
                          "  M u(A o);\n" +
                          "}\n",
                          "interface A {\n" +
                          "  <T> T v(Class<? extends T> o);\n" +
                          "}\n",
                          "abstract class Test {\n" +
                          "  abstract String f(Object proto);\n" +
                          "  String test(B o) {\n" +
                          "    return f(o.g((x) -> x.v(Object.class)).get());\n" +
                          "  }\n" +
                          "}\n");
        Path classes = outerBase.resolve("classes");
        Files.createDirectories(classes);
        new JavacTask(tb)
                .outdir(classes)
                .files(src.resolve("B.java"), src.resolve("F.java"), src.resolve("A.java"))
                .run()
                .writeAll();
        Files.delete(classes.resolve("A.class"));
        StringWriter out = new StringWriter();
        int ret = Main.compile(new String[] {"-d", classes.toString(),
                                             "-classpath", classes.toString(),
                                             "-XDrawDiagnostics",
                                             src.resolve("Test.java").toString()
                                            },
                               new PrintWriter(out));

        //check compilation output:
        if (ret != 1) {
            throw new AssertionError("Unexpected error code: " + ret);
        }
        String expectedOutput =
            "Test.java:4:18: compiler.err.cant.access: A, (compiler.misc.class.file.not.found: A)\n" +
            "1 error\n";
        if (!expectedOutput.equals(out.toString().replace(System.lineSeparator(), "\n"))) {
            throw new AssertionError("Unexpected output: " + out);
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            com.sun.source.util.JavacTask task = (com.sun.source.util.JavacTask)
                    compiler.getTask(null,
                                     null,
                                     d -> {},
                                     Arrays.asList("-d", classes.toString(),
                                                   "-classpath", classes.toString()),
                                     null,
                                     fm.getJavaFileObjects(src.resolve("Test.java")));
            CompilationUnitTree cut = task.parse().iterator().next();
            task.analyze();
            Trees trees = Trees.instance(task);
            TypeMirror[] foundType = new TypeMirror[1];
            new TreePathScanner<Void, Void>() {
                @Override
                public Void visitMemberSelect(MemberSelectTree node, Void p) {
                    if (node.getIdentifier().contentEquals("v")) {
                        foundType[0] = trees.getTypeMirror(getCurrentPath().getParentPath());
                    }
                    return super.visitMemberSelect(node, p);
                }
            }.scan(cut, null);

            //check error type properties:
            if (foundType[0] == null) {
                throw new AssertionError("Did not find the expected type.");
            }
            if (foundType[0].getKind() != TypeKind.ERROR) {
                throw new AssertionError("The type has an unexpected kind: " +
                                         foundType[0].getKind());
            }
            TypeMirror enclosing = ((ErrorType) foundType[0]).getEnclosingType();
            if (enclosing.getKind() != TypeKind.NONE) {
                throw new AssertionError("The enclosing type has an unexpected kind: " +
                                         enclosing.getKind());
            }

            //check that isAccessible does not crash on it:
            trees.isAccessible(trees.getScope(new TreePath(cut)),
                               (TypeElement) task.getTypes().asElement(foundType[0]));
        }
    }

}
