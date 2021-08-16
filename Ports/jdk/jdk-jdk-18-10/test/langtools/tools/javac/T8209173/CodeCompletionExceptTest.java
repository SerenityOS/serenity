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

/*
 * @test
 * @bug 8209173
 * @summary javac fails with completion exception while reporting an error
 * @library /tools/lib
 * @modules jdk.jdeps/com.sun.tools.classfile
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 *          jdk.jdeps/com.sun.tools.javap
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main CodeCompletionExceptTest
 */

import java.io.File;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.ToolBox;

public class CodeCompletionExceptTest {
    private static final String MyListSource =
            "import java.util.List;\n" +
            "public interface MyList<E> extends List<E> {}";

    private static final String C1Source =
            "public class C1 {\n" +
            "    int m(MyList<Integer> list) {\n" +
            "        return 0;\n" +
            "    }\n" +
            "}";

    private static final String C2Source =
            "class C2 {\n" +
            "    void m() {\n" +
            "        (new C1()).m(1, 2, 3);\n" +
            "    }\n" +
            "}";

    public static void main(String[] args) throws Exception {
        new CodeCompletionExceptTest().run();
    }

    ToolBox tb = new ToolBox();

    void run() throws Exception {
        // first we compile MyList
        new JavacTask(tb)
                .sources(MyListSource)
                .run();

        // then with MyList.class in the cp we compile C1
        new JavacTask(tb)
                .sources(C1Source)
                .classpath(System.getProperty("user.dir"))
                .run();
        // now we delete MyList.class
        tb.deleteFiles(System.getProperty("user.dir") + File.separatorChar + "MyList.class");
        /** and try to compile C2 which uses C1 which uses MyList but, MyList won't be in the
         *  classpath so its symbol can't be completed while javac is generating the diagnostic error
         *  the compiler should capture the completion exception and still be able to produce an error
         *  message without crashing
         */
        String javacOut = new JavacTask(tb)
                .sources(C2Source)
                .classpath(System.getProperty("user.dir"))
                .options("-XDrawDiagnostics")
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);
        String expectedOuput =
                "C2.java:3:19: compiler.err.cant.apply.symbol: kindname.method, m, MyList<java.lang.Integer>, int,int,int, kindname.class, C1, (compiler.misc.arg.length.mismatch)";
        if (!javacOut.contains(expectedOuput)) {
            throw new Exception("test failed, unexpected output");
        }
    }
}
