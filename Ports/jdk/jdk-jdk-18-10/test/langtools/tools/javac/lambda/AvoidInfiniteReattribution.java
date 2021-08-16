/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8077605
 * @summary Check that when an exception occurs during Attr.visitLambda, an attempt to attribute
 *          the lambda again is avoided rather than falling into an infinite recursion.
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.comp
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 */

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Context.Factory;

public class AvoidInfiniteReattribution {

    public static void main(String... args) throws Exception {
        new AvoidInfiniteReattribution().run();
    }

    void run() throws IOException {
        JavacTool tool = JavacTool.create();
        JavaSource source = new JavaSource("class Test {" +
                                           "    I i = STOP -> {};" +
                                           "    interface I {" +
                                           "        public void test(int i) {}" +
                                           "    }" +
                                           "}");
        Context context = new Context();
        CrashingAttr.preRegister(context);
        List<JavaSource> inputs = Arrays.asList(source);
        JavacTaskImpl task =
                (JavacTaskImpl) tool.getTask(null, null, null, null, null, inputs, context);
        try {
            task.analyze(null);
            throw new AssertionError("Expected exception not seen.");
        } catch (StopException ex) {
            //ok
        }
    }

    static class CrashingAttr extends Attr {

        static void preRegister(Context context) {
            context.put(attrKey, (Factory<Attr>) c -> new CrashingAttr(c));
        }

        CrashingAttr(Context context) {
            super(context);
        }

        @Override public void visitVarDef(JCVariableDecl tree) {
            if (tree.name.contentEquals("STOP"))
                throw new StopException();
            super.visitVarDef(tree);
        }
    }

    static class StopException extends NullPointerException {}

    class JavaSource extends SimpleJavaFileObject {

        String source;

        JavaSource(String source) {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            this.source = source;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }

    }

}
