/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8176714
 * @summary javac is wrongly assuming that field JCMemberReference.overloadKind has been assigned to
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.source.util
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @build DPrinter
 * @run main FieldOverloadKindNotAssignedTest
 */

import java.net.URI;
import java.util.Arrays;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCMemberReference;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.Context;

public class FieldOverloadKindNotAssignedTest {
    public static void main(String... args) throws Exception {
        new FieldOverloadKindNotAssignedTest().run();
    }

    void run() throws Exception {
        Context context = new Context();
        JavacFileManager.preRegister(context);
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        JavacTask ct = (JavacTask)tool.getTask(null, null, null, null, null, Arrays.asList(new JavaSource()));
        Iterable<? extends CompilationUnitTree> elements = ct.parse();
        ct.analyze();
        Assert.check(elements.iterator().hasNext());
        JCTree topLevel = (JCTree)elements.iterator().next();
        new TreeScanner() {
            @Override
            public void visitReference(JCMemberReference tree) {
                Assert.check(tree.getOverloadKind() != null);
            }
        }.scan(topLevel);
    }

    static class JavaSource extends SimpleJavaFileObject {

        String source =
                "import java.util.function.*;\n" +

                "class Test {\n" +
                "    void m(Predicate<String> psi) {}\n" +
                "    void m(Function<String, String> fss) {}\n" +

                "    void foo(boolean b) {\n" +
                "        m(b ? s -> false : Test::g);\n" +
                "    }\n" +

                "    static boolean g(String s) { return false; }\n" +
                "    static boolean g(Integer i) { return false; }\n" +
                "}";

        public JavaSource() {
            super(URI.create("myfo:/Foo.java"), JavaFileObject.Kind.SOURCE);
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }
}
