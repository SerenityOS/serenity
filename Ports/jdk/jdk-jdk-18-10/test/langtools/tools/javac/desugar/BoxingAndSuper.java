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

/**
 * @test
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.comp
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 */

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.Lower;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.LetExpr;
import com.sun.tools.javac.tree.JCTree.Tag;
import com.sun.tools.javac.tree.TreeCopier;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Log.WriterKind;
import com.sun.tools.javac.util.Names;

import toolbox.ToolBox;

public class BoxingAndSuper {
    public static void main(String... args) throws Exception {
        new BoxingAndSuper().testSuper();
        new BoxingAndSuper().testThis();
    }

    public void testSuper() throws Exception {
        //super, same package:
        runTest("package p;\n" +
                "class Test extends Parent {\n" +
                "     protected Integer i=20;\n" +
                "     private Integer dump() {\n" +
                "         return super.i++;\n" +
                "     }\n" +
                "}\n" +
                "---" +
                "package p;\n" +
                "class Parent {\n" +
                "     protected Integer i=10;\n" +
                "} ",
                "p.Test.dump()java.lang.Integer\n" +
                "{\n" +
                "    return (let /*synthetic*/ final Integer $le0 = (Integer)super.i " +
                            "in (let super.i = Integer.valueOf((int)(super.i.intValue() + 1)); " +
                                "in $le0));\n" +
                "}\n");
        //qualified super, same package:
        runTest("package p;\n" +
                "class Test extends Parent {\n" +
                "     protected Integer i=20;\n" +
                "     class Inner {\n" +
                "         private Integer dump() {\n" +
                "             return Test.super.i++;\n" +
                "         }\n" +
                "     }\n" +
                "}\n" +
                "---" +
                "package p;\n" +
                "class Parent {\n" +
                "     protected Integer i=10;\n" +
                "} ",
                "p.Test.Inner.dump()java.lang.Integer\n" +
                "{\n" +
                "    return (let /*synthetic*/ final Integer $le0 = (Integer)Test.access$001(this$0) " +
                            "in (let Test.access$103(this$0, Integer.valueOf((int)(Test.access$201(this$0).intValue() + 1))); " +
                                "in $le0));\n" +
                "}\n" +
                "p.Test.access$001(p.Test)java.lang.Integer\n" +
                "{\n" +
                "    return x0.i;\n" +
                "}\n" +
                "p.Test.access$103(p.Test,java.lang.Integer)java.lang.Integer\n" +
                "{\n" +
                "    return x0.i = x1;\n" +
                "}\n" +
                "p.Test.access$201(p.Test)java.lang.Integer\n" +
                "{\n" +
                "    return x0.i;\n" +
                "}\n");
        //super, different packages:
        runTest("package p1;\n" +
                "class Test extends p2.Parent {\n" +
                "     protected Integer i=20;\n" +
                "     private Integer dump() {\n" +
                "         return super.i++;\n" +
                "     }\n" +
                "}\n" +
                "---" +
                "package p2;\n" +
                "public class Parent {\n" +
                "     protected Integer i=10;\n" +
                "} ",
                "p1.Test.dump()java.lang.Integer\n" +
                "{\n" +
                "    return (let /*synthetic*/ final Integer $le0 = (Integer)super.i " +
                            "in (let super.i = Integer.valueOf((int)(super.i.intValue() + 1)); " +
                                "in $le0));\n" +
                "}\n");
        //qualified super, different packages:
        runTest("package p1;\n" +
                "class Test extends p2.Parent {\n" +
                "     protected Integer i=20;\n" +
                "     class Inner {\n" +
                "         private Integer dump() {\n" +
                "             return Test.super.i++;\n" +
                "         }\n" +
                "     }\n" +
                "}\n" +
                "---" +
                "package p2;\n" +
                "public class Parent {\n" +
                "     protected Integer i=10;\n" +
                "} ",
                "p1.Test.Inner.dump()java.lang.Integer\n" +
                "{\n" +
                "    return (let /*synthetic*/ final Integer $le0 = (Integer)Test.access$001(this$0) " +
                            "in (let Test.access$103(this$0, Integer.valueOf((int)(Test.access$201(this$0).intValue() + 1))); " +
                                "in $le0));\n" +
                "}\n" +
                "p1.Test.access$001(p1.Test)java.lang.Integer\n" +
                "{\n" +
                "    return x0.i;\n" +
                "}\n" +
                "p1.Test.access$103(p1.Test,java.lang.Integer)java.lang.Integer\n" +
                "{\n" +
                "    return x0.i = x1;\n" +
                "}\n" +
                "p1.Test.access$201(p1.Test)java.lang.Integer\n" +
                "{\n" +
                "    return x0.i;\n" +
                "}\n");
    }

    public void testThis() throws Exception {
        String code = "public class Test {\n" +
                      "    Integer i;\n" +
                      "    private void dump() {\n" +
                      "        i++;\n" +
                      "        this.i++;\n" +
                      "    }\n" +
                      "}";
        String expected =
                "Test.dump()void\n" +
                "{\n" +
                "    (let /*synthetic*/ final Integer $le0 = i in (let i = Integer.valueOf((int)(i.intValue() + 1)); in $le0));\n" +
                "    (let /*synthetic*/ final Integer $le1 = (Integer)this.i in (let this.i = Integer.valueOf((int)(this.i.intValue() + 1)); in $le1));\n" +
                "}\n";
        runTest(code, expected);
        //qualified this:
        runTest("public class Test {\n" +
                "   Integer i;\n" +
                "   class Inner1 {\n" +
                "       class Inner2 {\n" +
                "           private Integer dump() {\n" +
                "               return Test.this.i++;\n" +
                "           }\n" +
                "       }\n" +
                "   }\n" +
                "}",
                "Test.Inner1.Inner2.dump()java.lang.Integer\n" +
                "{\n" +
                "    return (let /*synthetic*/ final Integer $le0 = (Integer)this$1.this$0.i" +
                           " in (let this$1.this$0.i = " +
                                "Integer.valueOf((int)(this$1.this$0.i.intValue() + 1)); " +
                                "in $le0));\n" +
                "}\n"
        );
    }

    private final ToolBox tb = new ToolBox();

    private void runTest(String code, String expectedDesugar) throws Exception {
        List<JavaFileObject> files = List.nil();

        for (String file : code.split("---")) {
            files = files.prepend(new ToolBox.JavaSource(file));
        }

        Path classes = Paths.get("classes");

        if (Files.exists(classes)) {
            tb.cleanDirectory(classes);
        } else {
            Files.createDirectories(classes);
        }

        JavacTool compiler = (JavacTool) ToolProvider.getSystemJavaCompiler();
        StringWriter out = new StringWriter();
        Context context = new Context();
        TestLower.preRegister(context);
        Iterable<String> options = Arrays.asList("-d", classes.toString());
        JavacTask task = (JavacTask) compiler.getTask(out, null, null, options, null, files, context);

        task.generate();

        out.flush();

        String actual = out.toString().replace(System.getProperty("line.separator"), "\n");

        if (!expectedDesugar.equals(actual)) {
            throw new IllegalStateException("Actual does not match expected: " + actual);
        }
    }

    private static final class TestLower extends Lower {

        public static void preRegister(Context context) {
            context.put(lowerKey, new Context.Factory<Lower>() {
                   public Lower make(Context c) {
                       return new TestLower(c);
                   }
            });
        }

        private final TreeMaker make;
        private final Names names;
        private final Log log;

        public TestLower(Context context) {
            super(context);
            make = TreeMaker.instance(context);
            names = Names.instance(context);
            log = Log.instance(context);
        }

        @Override
        public List<JCTree> translateTopLevelClass(Env<AttrContext> env, JCTree cdef, TreeMaker make) {
            List<JCTree> result = super.translateTopLevelClass(env, cdef, make);
            Map<Symbol, JCMethodDecl> declarations = new HashMap<>();
            Set<Symbol> toDump = new TreeSet<>(symbolComparator);

            new TreeScanner() {
                @Override
                public void visitMethodDef(JCMethodDecl tree) {
                    if (tree.name.toString().startsWith("dump")) {
                        toDump.add(tree.sym);
                    }
                    declarations.put(tree.sym, tree);
                    super.visitMethodDef(tree);
                }
            }.scan(result);

            for (Symbol d : toDump) {
                dump(d, declarations, new HashSet<>());
            }

            return result;
        }

        private void dump(Symbol methodSym, Map<Symbol, JCMethodDecl> declarations, Set<Symbol> alreadyPrinted) {
            if (!alreadyPrinted.add(methodSym))
                return ;

            JCMethodDecl method = declarations.get(methodSym);

            if (method == null) {
                return ;
            }

            log.getWriter(WriterKind.NOTICE).println(symbol2String(methodSym));

            JCBlock body = new TreeCopier<Void>(make) {
                private final Map<String, String> letExprRemap = new HashMap<>();
                private int i;

                @Override
                public JCTree visitOther(Tree node, Void p) {
                    JCTree tree = (JCTree) node;
                    if (tree.hasTag(Tag.LETEXPR)) {
                        LetExpr le = (LetExpr) tree;

                        for (JCStatement var : le.defs) {
                            if (var.hasTag(Tag.VARDEF))
                                letExprRemap.put(((JCVariableDecl) var).name.toString(), "$le" + i++);
                        }
                    }
                    return super.visitOther(node, p);
                }

                @Override
                public JCTree visitVariable(VariableTree node, Void p) {
                    String newName = letExprRemap.get(node.getName().toString());
                    if (newName != null) {
                        node = make.VarDef((JCModifiers) node.getModifiers(), names.fromString(newName), (JCExpression) node.getType(), (JCExpression) node.getInitializer());
                    }
                    return super.visitVariable(node, p);
                }

                @Override
                public JCTree visitIdentifier(IdentifierTree node, Void p) {
                    String newName = letExprRemap.get(node.getName().toString());
                    if (newName != null) {
                        node = make.Ident(names.fromString(newName));
                    }
                    return super.visitIdentifier(node, p);
                }

                @Override
                public <T extends JCTree> T copy(T tree, Void p) {
                    if (tree.hasTag(Tag.LETEXPR)) {
                        return (T) visitOther(tree, p);
                    }
                    return super.copy(tree, p);
                }

            }.copy(method.body);
            log.getWriter(WriterKind.NOTICE).println(body.toString());

            Set<Symbol> invoked = new TreeSet<>(symbolComparator);

            new TreeScanner() {
                @Override
                public void visitApply(JCMethodInvocation tree) {
                    invoked.add(TreeInfo.symbol(tree.meth));
                    super.visitApply(tree);
                }
            }.scan(method);

            for (Symbol search : invoked) {
                dump(search, declarations, alreadyPrinted);
            }
        }

        private String symbol2String(Symbol sym) {
            switch (sym.kind) {
                case TYP:
                    return sym.getQualifiedName().toString();
                case MTH:
                    return symbol2String(sym.owner) + "." + sym.name + sym.type.toString();
                default:
                    throw new UnsupportedOperationException();
            }
        }

        private final Comparator<Symbol> symbolComparator = (s1, s2) -> symbol2String(s1).compareTo(symbol2String(s2));
    }

}
