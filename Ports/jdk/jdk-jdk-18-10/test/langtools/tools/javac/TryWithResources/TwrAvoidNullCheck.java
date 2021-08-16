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

/*
 * @test
 * @bug 7020499
 * @summary Verify that try-with-resources desugaring is not generating unnecessary null checks
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.comp
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @build toolbox.ToolBox toolbox.JavacTask TwrAvoidNullCheck
 * @run main TwrAvoidNullCheck
 */

import java.io.IOException;
import java.util.Arrays;

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.Lower;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Context.Factory;
import com.sun.tools.javac.util.List;

import toolbox.ToolBox;

public class TwrAvoidNullCheck {
    public static void main(String... args) throws IOException {
        new TwrAvoidNullCheck().run();
    }
    void run() throws IOException {
        run("new Test()", false);
        run("null", true);
        run("System.getProperty(\"test\") != null ? new Test() : null", true);
    }
    void run(String resourceSpecification, boolean expected) throws IOException {
        String template = "public class Test implements AutoCloseable {\n" +
                          "    void t() {\n" +
                          "        try (Test resource = RESOURCE) { }\n" +
                          "    }\n" +
                          "    public void close() { }\n" +
                          "}\n";
        String code = template.replace("RESOURCE", resourceSpecification);
        Context ctx = new Context();
        DumpLower.preRegister(ctx);
        Iterable<ToolBox.JavaSource> files = Arrays.asList(new ToolBox.JavaSource(code));
        JavacTask task = JavacTool.create().getTask(null, null, null, null, null, files, ctx);
        task.call();

        boolean hasNullCheck = ((DumpLower) DumpLower.instance(ctx)).hasNullCheck;

        if (hasNullCheck != expected) {
            throw new IllegalStateException("expected: " + expected +
                                            "; actual: " + hasNullCheck +
                                            "; code: " + code);
        }
    }

    static class DumpLower extends Lower {

        public static void preRegister(Context ctx) {
            ctx.put(lowerKey, new Factory<Lower>() {
                @Override
                public Lower make(Context c) {
                    return new DumpLower(c);
                }
            });
        }

        public DumpLower(Context context) {
            super(context);
        }

        boolean hasNullCheck;

        @Override
        public List<JCTree> translateTopLevelClass(Env<AttrContext> env, JCTree cdef, TreeMaker make) {
            List<JCTree> result = super.translateTopLevelClass(env, cdef, make);

            new TreeScanner() {
                @Override
                public void visitBinary(JCBinary tree) {
                    hasNullCheck |= tree.operator.getSimpleName().contentEquals("!=") &&
                                    "resource".equals(String.valueOf(TreeInfo.name(tree.lhs))) &&
                                    TreeInfo.isNull(tree.rhs);
                    super.visitBinary(tree);
                }
            }.scan(result);

            return result;
        }

    }
}
