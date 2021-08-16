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
 * @bug 8202141
 * @summary Verify that .class synthetic Symbols are not duplicated.
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @build combo.ComboTestHelper
 * @run main ClassFieldDeduplication
 */

import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.TreeScanner;
import combo.ComboInstance;
import combo.ComboParameter;
import combo.ComboTestHelper;

public class ClassFieldDeduplication extends ComboInstance<ClassFieldDeduplication> {

    enum Type implements ComboParameter {
        OBJECT("Object"),
        PRIMITIVE("int"),
        BOXED_PRIMITIVE("Integer"),
        VOID("void"),
        BOXED_VOID("Void"),
        OBJECT_ARRAY("Object[]"),
        PRIMITIVE_ARRAY("int[]"),
        BOXED_PRIMITIVE_ARRAY("Integer[]"),
        BOXED_VOID_ARRAY("Void[]");

        String type;

        Type(String type) {
            this.type = type;
        }

        @Override
        public String expand(String optParameter) {
            return type;
        }

    }

    public static void main(String... args) throws Exception {
        new ComboTestHelper<ClassFieldDeduplication>()
                .withDimension("TYPE", Type.values())
                .run(ClassFieldDeduplication::new);
    }

    private static final String TEMPLATE =
            "class Test { void t() { Object o1 = #{TYPE}.class; Object o2 = #{TYPE}.class; } }";

    @Override
    protected void doWork() throws Throwable {
        newCompilationTask()
                .withSourceFromTemplate(TEMPLATE)
                .withListener(new TaskListener() {
                    JCCompilationUnit cut;
                        @Override
                        public void finished(TaskEvent e) {
                            if (e.getKind() == TaskEvent.Kind.PARSE) {
                                if (cut != null)
                                    throw new AssertionError();
                                cut = (JCCompilationUnit) e.getCompilationUnit();
                            }
                            if (e.getKind() == TaskEvent.Kind.ANALYZE) {
                                cut.accept(new TreeScanner() {
                                    Symbol s;
                                    @Override
                                    public void visitSelect(JCFieldAccess tree) {
                                        if (tree.name.contentEquals("class")) {
                                            if (s == null) {
                                                s = tree.sym;
                                            } else if (s != tree.sym) {
                                                throw new AssertionError("Duplicated field symbol.");
                                            }
                                        }
                                        super.visitSelect(tree);
                                    }
                                });
                            }
                        }

                })
                .analyze(els -> {});
    }

}
