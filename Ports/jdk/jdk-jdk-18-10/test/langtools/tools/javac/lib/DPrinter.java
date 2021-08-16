/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8043484 8007307
 * @summary Make sure DPrinter.java compiles
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @compile DPrinter.java
 */

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import com.sun.source.doctree.*;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.SymbolMetadata;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Kinds;
import com.sun.tools.javac.code.Printer;
import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Scope.CompoundScope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.*;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.Pretty;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Convert;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;


/**
 * Debug printer for javac internals, for when toString() just isn't enough.
 *
 * <p>
 * The printer provides an API to generate structured views of javac objects,
 * such as AST nodes, symbol, types and annotations. Various aspects of the
 * output can be configured, such as whether to show nulls, empty lists, or
 * a compressed representation of the source code. Visitors are used to walk
 * object hierarchies, and can be replaced with custom visitors if the default
 * visitors are not flexible enough.
 *
 * <p>
 * In general, nodes are printed with an initial line identifying the node
 * followed by indented lines for the child nodes. Currently, graphs are
 * represented by printing a spanning subtree.
 *
 * <p>
 * The printer can be accessed via a simple command-line utility,
 * which makes it easy to see the internal representation of source code,
 * such as simple test programs, during the compilation pipeline.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */

public class DPrinter {
    protected final PrintWriter out;
    protected final Trees trees;
    protected Printer printer;
    protected boolean showEmptyItems = true;
    protected boolean showNulls = true;
    protected boolean showPositions = false;
    protected boolean showSrc;
    protected boolean showTreeSymbols;
    protected boolean showTreeTypes;
    protected int maxSrcLength = 32;
    protected Locale locale = Locale.getDefault();
    protected static final String NULL = "#null";

    // <editor-fold defaultstate="collapsed" desc="Configuration">

    public static DPrinter instance(Context context) {
        DPrinter dp = context.get(DPrinter.class);
        if (dp == null) {
            dp = new DPrinter(context);
        }
        return dp;

    }

    protected DPrinter(Context context) {
        context.put(DPrinter.class, this);
        out = context.get(Log.logKey).getWriter(Log.WriterKind.STDERR);
        trees = JavacTrees.instance(context);
    }

    public DPrinter(PrintWriter out, Trees trees) {
        this.out = out;
        this.trees = trees;
    }

    public DPrinter emptyItems(boolean showEmptyItems) {
        this.showEmptyItems = showEmptyItems;
        return this;
    }

    public DPrinter nulls(boolean showNulls) {
        this.showNulls = showNulls;
        return this;
    }

    public DPrinter positions(boolean showPositions) {
        this.showPositions = showPositions;
        return this;
    }

    public DPrinter source(boolean showSrc) {
        this.showSrc = showSrc;
        return this;
    }

    public DPrinter source(int maxSrcLength) {
        this.showSrc = true;
        this.maxSrcLength = maxSrcLength;
        return this;
    }

    public DPrinter treeSymbols(boolean showTreeSymbols) {
        this.showTreeSymbols = showTreeSymbols;
        return this;
    }

    public DPrinter treeTypes(boolean showTreeTypes) {
        this.showTreeTypes = showTreeTypes;
        return this;
    }

    public DPrinter typeSymbolPrinter(Printer p) {
        printer = p;
        return this;
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Printing">

    protected enum Details {
        /** A one-line non-recursive summary */
        SUMMARY,
        /** Multi-line, possibly recursive. */
        FULL
    };

    public void printAnnotations(String label, SymbolMetadata annotations) {
        printAnnotations(label, annotations, Details.FULL);
    }

    protected void printAnnotations(String label, SymbolMetadata annotations, Details details) {
        if (annotations == null) {
            printNull(label);
        } else {
            // no SUMMARY format currently available to use

            // use reflection to get at private fields
            Object DECL_NOT_STARTED = getField(null, SymbolMetadata.class, "DECL_NOT_STARTED");
            Object DECL_IN_PROGRESS = getField(null, SymbolMetadata.class, "DECL_IN_PROGRESS");
            Object attributes = getField(annotations, SymbolMetadata.class, "attributes");
            Object type_attributes = getField(annotations, SymbolMetadata.class, "type_attributes");

            if (!showEmptyItems) {
                if (attributes instanceof List && ((List) attributes).isEmpty()
                        && attributes != DECL_NOT_STARTED
                        && attributes != DECL_IN_PROGRESS
                        && type_attributes instanceof List && ((List) type_attributes).isEmpty())
                    return;
            }

            printString(label, hashString(annotations));

            indent(+1);
            if (attributes == DECL_NOT_STARTED)
                printString("attributes", "DECL_NOT_STARTED");
            else if (attributes == DECL_IN_PROGRESS)
                printString("attributes", "DECL_IN_PROGRESS");
            else if (attributes instanceof List)
                printList("attributes", (List) attributes);
            else
                printObject("attributes", attributes, Details.SUMMARY);

            if (attributes instanceof List)
                printList("type_attributes", (List) type_attributes);
            else
                printObject("type_attributes", type_attributes, Details.SUMMARY);
            indent(-1);
        }
    }

    public void printAttribute(String label, Attribute attr) {
        if (attr == null) {
            printNull(label);
        } else {
            printString(label, attr.getClass().getSimpleName());

            indent(+1);
            attr.accept(attrVisitor);
            indent(-1);
        }
    }

    public void printDocTree(String label, DocTree tree) {
        if (tree == null) {
             printNull(label);
        } else {
            indent();
            out.print(label);
            out.println(": " + tree.getClass().getSimpleName() + "," + tree.getKind());

            indent(+1);
            tree.accept(docTreeVisitor, null);
            indent(-1);
        }
    }

    public void printFileObject(String label, FileObject fo) {
        if (fo == null) {
            printNull(label);
        } else {
            printString(label, fo.getName());
        }
    }

    protected <T> void printImplClass(T item, Class<? extends T> stdImplClass) {
        if (item.getClass() != stdImplClass)
            printString("impl", item.getClass().getName());
    }

    public void printInt(String label, int i) {
        printString(label, String.valueOf(i));
    }

    public void printLimitedEscapedString(String label, String text) {
        String s = Convert.quote(text);
        if (s.length() > maxSrcLength) {
            String trim = "[...]";
            int head = (maxSrcLength - trim.length()) * 2 / 3;
            int tail = maxSrcLength - trim.length() - head;
            s = s.substring(0, head) + trim + s.substring(s.length() - tail);
        }
        printString(label, s);
    }

    public void printList(String label, List<?> list) {
        if (list == null) {
             printNull(label);
        } else if (!list.isEmpty() || showEmptyItems) {
            printString(label, "[" + list.size() + "]");

            indent(+1);
            int i = 0;
            for (Object item: list) {
                printObject(String.valueOf(i++), item, Details.FULL);
            }
            indent(-1);
        }
    }

    public void printName(String label, Name name) {
        if (name == null) {
            printNull(label);
        } else {
            printString(label, name.toString());
        }
    }

    public void printNull(String label) {
        if (showNulls)
            printString(label, NULL);
    }

    protected void printObject(String label, Object item, Details details) {
        if (item == null) {
            printNull(label);
        } else if (item instanceof Attribute) {
            printAttribute(label, (Attribute) item);
        } else if (item instanceof Symbol) {
            printSymbol(label, (Symbol) item, details);
        } else if (item instanceof Type) {
            printType(label, (Type) item, details);
        } else if (item instanceof JCTree) {
            printTree(label, (JCTree) item);
        } else if (item instanceof DocTree) {
            printDocTree(label, (DocTree) item);
        } else if (item instanceof List) {
            printList(label, (List) item);
        } else if (item instanceof Name) {
            printName(label, (Name) item);
        } else if (item instanceof Scope) {
            printScope(label, (Scope) item);
        } else {
            printString(label, String.valueOf(item));
        }
    }

    public void printScope(String label, Scope scope) {
        printScope(label, scope, Details.FULL);
    }

    public void printScope(String label, Scope scope, Details details) {
        if (scope == null) {
            printNull(label);
        } else {
            switch (details) {
                case SUMMARY: {
                    indent();
                    out.print(label);
                    out.print(": [");
                    String sep = "";
                    for (Symbol sym: scope.getSymbols()) {
                        out.print(sep);
                        out.print(sym.name);
                        sep = ",";
                    }
                    out.println("]");
                    break;
                }

                case FULL: {
                    indent();
                    out.println(label);

                    indent(+1);
                    printFullScopeImpl(scope);
                    indent(-1);
                    break;
                }
            }
        }
    }

    void printFullScopeImpl(Scope scope) {
        indent();
        out.println(scope.getClass().getName());
        printSymbol("owner", scope.owner, Details.SUMMARY);
        if (SCOPE_IMPL_CLASS.equals(scope.getClass().getName())) {
            printScope("next", (Scope) getField(scope, scope.getClass(), "next"), Details.SUMMARY);
            printObject("shared", getField(scope, scope.getClass(), "shared"), Details.SUMMARY);
            Object[] table = (Object[]) getField(scope, scope.getClass(), "table");
            for (int i = 0; i < table.length; i++) {
                if (i > 0)
                    out.print(", ");
                else
                    indent();
                out.print(i + ":" + entryToString(table[i], table, false));
            }
            out.println();
        } else if (FILTER_SCOPE_CLASS.equals(scope.getClass().getName())) {
            printScope("origin",
                    (Scope) getField(scope, scope.getClass(), "origin"), Details.FULL);
        } else if (scope instanceof CompoundScope) {
            printList("delegates", ((ListBuffer<?>) getField(scope, CompoundScope.class, "subScopes")).toList());
        } else {
            for (Symbol sym : scope.getSymbols()) {
                printSymbol(sym.name.toString(), sym, Details.SUMMARY);
            }
        }
    }
        //where:
        static final String SCOPE_IMPL_CLASS = "com.sun.tools.javac.code.Scope$ScopeImpl";
        static final String FILTER_SCOPE_CLASS = "com.sun.tools.javac.code.Scope$FilterImportScope";

    /**
     * Create a string showing the contents of an entry, using the table
     * to help identify cross-references to other entries in the table.
     * @param e the entry to be shown
     * @param table the table containing the other entries
     */
    String entryToString(Object e, Object[] table, boolean ref) {
        if (e == null)
            return "null";
        Symbol sym = (Symbol) getField(e, e.getClass(), "sym");
        if (sym == null)
            return "sent"; // sentinel
        if (ref) {
            int index = indexOf(table, e);
            if (index != -1)
                return String.valueOf(index);
        }
        Scope scope = (Scope) getField(e, e.getClass(), "scope");
        return "(" + sym.name + ":" + sym
                + ",shdw:" + entryToString(callMethod(e, e.getClass(), "next"), table, true)
                + ",nextSibling:" + entryToString(getField(e, e.getClass(), "nextSibling"), table, true)
                + ",prevSibling:" + entryToString(getField(e, e.getClass(), "prevSibling"), table, true)
                + ((sym.owner != scope.owner)
                    ? (",BOGUS[" + sym.owner + "," + scope.owner + "]")
                    : "")
                + ")";
    }

    <T> int indexOf(T[] array, T item) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == item)
                return i;
        }
        return -1;
    }

    public void printSource(String label, JCTree tree) {
        printString(label, Pretty.toSimpleString(tree, maxSrcLength));
    }

    public void printString(String label, String text) {
        indent();
        out.print(label);
        out.print(": ");
        out.print(text);
        out.println();
    }

    public void printSymbol(String label, Symbol symbol) {
        printSymbol(label, symbol, Details.FULL);
    }

    protected void printSymbol(String label, Symbol sym, Details details) {
        if (sym == null) {
            printNull(label);
        } else {
            switch (details) {
            case SUMMARY:
                printString(label, toString(sym));
                break;

            case FULL:
                indent();
                out.print(label);
                out.println(": " +
                        info(sym.getClass(),
                            String.format("0x%x--%s", sym.kind.ordinal(), Kinds.kindName(sym)),
                            sym.getKind())
                        + " " + sym.name
                        + " " + hashString(sym));

                indent(+1);
                if (showSrc) {
                    JCTree tree = (JCTree) trees.getTree(sym);
                    if (tree != null)
                        printSource("src", tree);
                }
                printString("flags", String.format("0x%x--%s",
                        sym.flags_field, Flags.toString(sym.flags_field)));
                printObject("completer", sym.completer, Details.SUMMARY); // what if too long?
                printSymbol("owner", sym.owner, Details.SUMMARY);
                printType("type", sym.type, Details.SUMMARY);
                printType("erasure", sym.erasure_field, Details.SUMMARY);
                sym.accept(symVisitor, null);
                printAnnotations("annotations", sym.getMetadata(), Details.SUMMARY);
                indent(-1);
            }
        }
    }

    protected String toString(Symbol sym) {
        return (printer != null) ? printer.visit(sym, locale) : String.valueOf(sym);
    }

    protected void printTree(String label, JCTree tree) {
        if (tree == null) {
            printNull(label);
        } else {
            indent();
            String ext;
            try {
                ext = tree.getKind().name();
            } catch (Throwable t) {
                ext = "n/a";
            }
            out.print(label + ": " + info(tree.getClass(), tree.getTag(), ext));
            if (showPositions) {
                // We can always get start position, but to get end position
                // and/or line+offset, we would need a JCCompilationUnit
                out.print(" pos:" + tree.pos);
            }
            if (showTreeTypes && tree.type != null)
                out.print(" type:" + toString(tree.type));
            Symbol sym;
            if (showTreeSymbols && (sym = TreeInfo.symbolFor(tree)) != null)
                out.print(" sym:" + toString(sym));
            out.println();

            indent(+1);
            if (showSrc) {
                indent();
                out.println("src: " + Pretty.toSimpleString(tree, maxSrcLength));
            }
            tree.accept(treeVisitor);
            indent(-1);
        }
    }

    public void printType(String label, Type type) {
        printType(label, type, Details.FULL);
    }

    protected void printType(String label, Type type, Details details) {
        if (type == null)
            printNull(label);
        else {
            switch (details) {
                case SUMMARY:
                    printString(label, toString(type));
                    break;

                case FULL:
                    indent();
                    out.print(label);
                    out.println(": " + info(type.getClass(), type.getTag(), type.getKind())
                            + " " + hashString(type));

                    indent(+1);
                    printSymbol("tsym", type.tsym, Details.SUMMARY);
                    printObject("constValue", type.constValue(), Details.SUMMARY);
                    printObject("annotations", type.getAnnotationMirrors(), Details.SUMMARY);
                    type.accept(typeVisitor, null);
                    indent(-1);
            }
        }
    }

    protected String toString(Type type) {
        return (printer != null) ? printer.visit(type, locale) : String.valueOf(type);
    }

    protected String hashString(Object obj) {
        return String.format("#%x", obj.hashCode());
    }

    protected String info(Class<?> clazz, Object internal, Object external) {
        return String.format("%s,%s,%s", clazz.getSimpleName(), internal, external);
    }

    private int indent = 0;

    protected void indent() {
        for (int i = 0; i < indent; i++) {
            out.print("  ");
        }
    }

    protected void indent(int n) {
        indent += n;
    }

    protected Object getField(Object o, Class<?> clazz, String name) {
        try {
            Field f = clazz.getDeclaredField(name);
            boolean prev = f.isAccessible();
            f.setAccessible(true);
            try {
                return f.get(o);
            } finally {
                f.setAccessible(prev);
            }
        } catch (ReflectiveOperationException e) {
            return e;
        } catch (SecurityException e) {
            return e;
        }
    }

    protected Object callMethod(Object o, Class<?> clazz, String name) {
        try {
            Method m = clazz.getDeclaredMethod(name);
            boolean prev = m.isAccessible();
            m.setAccessible(true);
            try {
                return m.invoke(o);
            } finally {
                m.setAccessible(prev);
            }
        } catch (ReflectiveOperationException e) {
            return e;
        } catch (SecurityException e) {
            return e;
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="JCTree visitor methods">

    protected JCTree.Visitor treeVisitor = new TreeVisitor();

    /**
     * Default visitor class for JCTree (AST) objects.
     */
    public class TreeVisitor extends JCTree.Visitor {
        @Override
        public void visitTopLevel(JCCompilationUnit tree) {
            printList("packageAnnotations", tree.getPackageAnnotations());
            printList("defs", tree.defs);
        }

        @Override
        public void visitPackageDef(JCPackageDecl tree) {
            printTree("pid", tree.pid);
        }

        @Override
        public void visitImport(JCImport tree) {
            printTree("qualid", tree.qualid);
        }

        @Override
        public void visitClassDef(JCClassDecl tree) {
            printName("name", tree.name);
            printTree("mods", tree.mods);
            printList("typarams", tree.typarams);
            printTree("extending", tree.extending);
            printList("implementing", tree.implementing);
            printList("defs", tree.defs);
        }

        @Override
        public void visitMethodDef(JCMethodDecl tree) {
            printName("name", tree.name);
            printTree("mods", tree.mods);
            printTree("restype", tree.restype);
            printList("typarams", tree.typarams);
            printTree("recvparam", tree.recvparam);
            printList("params", tree.params);
            printList("thrown", tree.thrown);
            printTree("defaultValue", tree.defaultValue);
            printTree("body", tree.body);
        }

        @Override
        public void visitVarDef(JCVariableDecl tree) {
            printName("name", tree.name);
            printTree("mods", tree.mods);
            printTree("vartype", tree.vartype);
            printTree("init", tree.init);
        }

        @Override
        public void visitSkip(JCSkip tree) {
        }

        @Override
        public void visitBlock(JCBlock tree) {
            printList("stats", tree.stats);
        }

        @Override
        public void visitDoLoop(JCDoWhileLoop tree) {
            printTree("body", tree.body);
            printTree("cond", tree.cond);
        }

        @Override
        public void visitWhileLoop(JCWhileLoop tree) {
            printTree("cond", tree.cond);
            printTree("body", tree.body);
        }

        @Override
        public void visitForLoop(JCForLoop tree) {
            printList("init", tree.init);
            printTree("cond", tree.cond);
            printList("step", tree.step);
            printTree("body", tree.body);
        }

        @Override
        public void visitForeachLoop(JCEnhancedForLoop tree) {
            printTree("var", tree.var);
            printTree("expr", tree.expr);
            printTree("body", tree.body);
        }

        @Override
        public void visitLabelled(JCLabeledStatement tree) {
            printTree("body", tree.body);
        }

        @Override
        public void visitSwitch(JCSwitch tree) {
            printTree("selector", tree.selector);
            printList("cases", tree.cases);
        }

        @Override
        public void visitCase(JCCase tree) {
            printList("labels", tree.labels);
            printList("stats", tree.stats);
        }

        @Override
        public void visitSynchronized(JCSynchronized tree) {
            printTree("lock", tree.lock);
            printTree("body", tree.body);
        }

        @Override
        public void visitTry(JCTry tree) {
            printList("resources", tree.resources);
            printTree("body", tree.body);
            printList("catchers", tree.catchers);
            printTree("finalizer", tree.finalizer);
        }

        @Override
        public void visitCatch(JCCatch tree) {
            printTree("param", tree.param);
            printTree("body", tree.body);
        }

        @Override
        public void visitConditional(JCConditional tree) {
            printTree("cond", tree.cond);
            printTree("truepart", tree.truepart);
            printTree("falsepart", tree.falsepart);
        }

        @Override
        public void visitIf(JCIf tree) {
            printTree("cond", tree.cond);
            printTree("thenpart", tree.thenpart);
            printTree("elsepart", tree.elsepart);
        }

        @Override
        public void visitExec(JCExpressionStatement tree) {
            printTree("expr", tree.expr);
        }

        @Override
        public void visitBreak(JCBreak tree) {
            printName("label", tree.label);
        }

        @Override
        public void visitYield(JCYield tree) {
            printTree("value", tree.value);
        }

        @Override
        public void visitContinue(JCContinue tree) {
            printName("label", tree.label);
        }

        @Override
        public void visitReturn(JCReturn tree) {
            printTree("expr", tree.expr);
        }

        @Override
        public void visitThrow(JCThrow tree) {
            printTree("expr", tree.expr);
        }

        @Override
        public void visitAssert(JCAssert tree) {
            printTree("cond", tree.cond);
            printTree("detail", tree.detail);
        }

        @Override
        public void visitApply(JCMethodInvocation tree) {
            printList("typeargs", tree.typeargs);
            printTree("meth", tree.meth);
            printList("args", tree.args);
        }

        @Override
        public void visitNewClass(JCNewClass tree) {
            printTree("encl", tree.encl);
            printList("typeargs", tree.typeargs);
            printTree("clazz", tree.clazz);
            printList("args", tree.args);
            printTree("def", tree.def);
        }

        @Override
        public void visitNewArray(JCNewArray tree) {
            printList("annotations", tree.annotations);
            printTree("elemtype", tree.elemtype);
            printList("dims", tree.dims);
            printList("dimAnnotations", tree.dimAnnotations);
            printList("elems", tree.elems);
        }

        @Override
        public void visitLambda(JCLambda tree) {
            printTree("body", tree.body);
            printList("params", tree.params);
        }

        @Override
        public void visitParens(JCParens tree) {
            printTree("expr", tree.expr);
        }

        @Override
        public void visitAssign(JCAssign tree) {
            printTree("lhs", tree.lhs);
            printTree("rhs", tree.rhs);
        }

        @Override
        public void visitAssignop(JCAssignOp tree) {
            printTree("lhs", tree.lhs);
            printTree("rhs", tree.rhs);
        }

        @Override
        public void visitUnary(JCUnary tree) {
            printTree("arg", tree.arg);
        }

        @Override
        public void visitBinary(JCBinary tree) {
            printTree("lhs", tree.lhs);
            printTree("rhs", tree.rhs);
        }

        @Override
        public void visitTypeCast(JCTypeCast tree) {
            printTree("clazz", tree.clazz);
            printTree("expr", tree.expr);
        }

        @Override
        public void visitTypeTest(JCInstanceOf tree) {
            printTree("expr", tree.expr);
            printTree("pattern", tree.pattern);
        }

        @Override
        public void visitIndexed(JCArrayAccess tree) {
            printTree("indexed", tree.indexed);
            printTree("index", tree.index);
        }

        @Override
        public void visitSelect(JCFieldAccess tree) {
            printTree("selected", tree.selected);
        }

        @Override
        public void visitReference(JCMemberReference tree) {
            printTree("expr", tree.expr);
            printList("typeargs", tree.typeargs);
        }

        @Override
        public void visitIdent(JCIdent tree) {
            printName("name", tree.name);
        }

        @Override
        public void visitLiteral(JCLiteral tree) {
            printString("value", Pretty.toSimpleString(tree, 32));
        }

        @Override
        public void visitTypeIdent(JCPrimitiveTypeTree tree) {
            printString("typetag", tree.typetag.name());
        }

        @Override
        public void visitTypeArray(JCArrayTypeTree tree) {
            printTree("elemtype", tree.elemtype);
        }

        @Override
        public void visitTypeApply(JCTypeApply tree) {
            printTree("clazz", tree.clazz);
            printList("arguments", tree.arguments);
        }

        @Override
        public void visitTypeUnion(JCTypeUnion tree) {
            printList("alternatives", tree.alternatives);
        }

        @Override
        public void visitTypeIntersection(JCTypeIntersection tree) {
            printList("bounds", tree.bounds);
        }

        @Override
        public void visitTypeParameter(JCTypeParameter tree) {
            printName("name", tree.name);
            printList("annotations", tree.annotations);
            printList("bounds", tree.bounds);
        }

        @Override
        public void visitWildcard(JCWildcard tree) {
            printTree("kind", tree.kind);
            printTree("inner", tree.inner);
        }

        @Override
        public void visitTypeBoundKind(TypeBoundKind tree) {
            printString("kind", tree.kind.name());
        }

        @Override
        public void visitModifiers(JCModifiers tree) {
            printList("annotations", tree.annotations);
            printString("flags", String.valueOf(Flags.asFlagSet(tree.flags)));
        }

        @Override
        public void visitAnnotation(JCAnnotation tree) {
            printTree("annotationType", tree.annotationType);
            printList("args", tree.args);
        }

        @Override
        public void visitAnnotatedType(JCAnnotatedType tree) {
            printList("annotations", tree.annotations);
            printTree("underlyingType", tree.underlyingType);
        }

        @Override
        public void visitErroneous(JCErroneous tree) {
            printList("errs", tree.errs);
        }

        @Override
        public void visitLetExpr(LetExpr tree) {
            printList("defs", tree.defs);
            printTree("expr", tree.expr);
        }

        @Override
        public void visitTree(JCTree tree) {
            Assert.error();
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="DocTree visitor">

    protected DocTreeVisitor<Void,Void> docTreeVisitor = new DefaultDocTreeVisitor();

    /**
     * Default visitor class for DocTree objects.
     * Note: each visitXYZ method ends by calling the corresponding
     * visit method for its superclass.
     */
    class DefaultDocTreeVisitor implements DocTreeVisitor<Void,Void> {

        public Void visitAttribute(AttributeTree node, Void p) {
            printName("name", node.getName());
            printString("vkind", node.getValueKind().name());
            printList("value", node.getValue());
            return visitTree(node, null);
        }

        public Void visitAuthor(AuthorTree node, Void p) {
            printList("name", node.getName());
            return visitBlockTag(node, null);
        }

        public Void visitComment(CommentTree node, Void p) {
            printLimitedEscapedString("body", node.getBody());
            return visitTree(node, null);
        }

        public Void visitDeprecated(DeprecatedTree node, Void p) {
            printList("body", node.getBody());
            return visitBlockTag(node, null);
        }

        public Void visitDocComment(DocCommentTree node, Void p) {
            printList("firstSentence", node.getFirstSentence());
            printList("body", node.getBody());
            printList("tags", node.getBlockTags());
            return visitTree(node, null);
        }

        public Void visitDocRoot(DocRootTree node, Void p) {
            return visitInlineTag(node, null);
        }

        @Override
        public Void visitDocType(DocTypeTree node, Void aVoid) {
            printLimitedEscapedString("body", node.getText());
            return visitTree(node, null);
        }

        public Void visitEndElement(EndElementTree node, Void p) {
            printName("name", node.getName());
            return visitTree(node, null);
        }

        public Void visitEntity(EntityTree node, Void p) {
            printName("name", node.getName());
            return visitTree(node, null);
        }

        public Void visitErroneous(ErroneousTree node, Void p) {
            printLimitedEscapedString("body", node.getBody());
            printString("diag", node.getDiagnostic().getMessage(Locale.getDefault()));
            return visitTree(node, null);
        }

        public Void visitHidden(HiddenTree node, Void p) {
            printList("body", node.getBody());
            return visitBlockTag(node, null);
        }

        public Void visitIdentifier(IdentifierTree node, Void p) {
            printName("name", node.getName());
            return visitTree(node, null);
        }

        public Void visitIndex(IndexTree node, Void p) {
            printString("kind", node.getKind().name());
            printDocTree("term", node.getSearchTerm());
            printList("desc", node.getDescription());
            return visitInlineTag(node, p);
        }

        public Void visitInheritDoc(InheritDocTree node, Void p) {
            return visitInlineTag(node, null);
        }

        public Void visitLink(LinkTree node, Void p) {
            printString("kind", node.getKind().name());
            printDocTree("ref", node.getReference());
            printList("list", node.getLabel());
            return visitInlineTag(node, null);
        }

        public Void visitLiteral(LiteralTree node, Void p) {
            printString("kind", node.getKind().name());
            printDocTree("body", node.getBody());
            return visitInlineTag(node, null);
        }

        public Void visitParam(ParamTree node, Void p) {
            printString("isTypeParameter", String.valueOf(node.isTypeParameter()));
            printString("kind", node.getKind().name());
            printList("desc", node.getDescription());
            return visitBlockTag(node, null);
        }

        public Void visitProvides(ProvidesTree node, Void p) {
            printString("kind", node.getKind().name());
            printDocTree("serviceType", node.getServiceType());
            printList("description", node.getDescription());
            return visitBlockTag(node, null);
        }

        public Void visitReference(ReferenceTree node, Void p) {
            printString("signature", node.getSignature());
            return visitTree(node, null);
        }

        public Void visitReturn(ReturnTree node, Void p) {
            printList("desc", node.getDescription());
            return visitBlockTag(node, null);
        }

        public Void visitSee(SeeTree node, Void p) {
            printList("ref", node.getReference());
            return visitBlockTag(node, null);
        }

        public Void visitSerial(SerialTree node, Void p) {
            printList("desc", node.getDescription());
            return visitBlockTag(node, null);
        }

        public Void visitSerialData(SerialDataTree node, Void p) {
            printList("desc", node.getDescription());
            return visitBlockTag(node, null);
        }

        public Void visitSerialField(SerialFieldTree node, Void p) {
            printDocTree("name", node.getName());
            printDocTree("type", node.getType());
            printList("desc", node.getDescription());
            return visitBlockTag(node, null);
        }

        public Void visitSince(SinceTree node, Void p) {
            printList("body", node.getBody());
            return visitBlockTag(node, null);
        }

        public Void visitStartElement(StartElementTree node, Void p) {
            printName("name", node.getName());
            printList("attrs", node.getAttributes());
            printString("selfClosing", String.valueOf(node.isSelfClosing()));
            return visitBlockTag(node, null);
        }

        public Void visitSummary(SummaryTree node, Void p) {
            printString("name", node.getTagName());
            printList("summary", node.getSummary());
            return visitInlineTag(node, null);
        }

        public Void visitText(TextTree node, Void p) {
            printLimitedEscapedString("body", node.getBody());
            return visitTree(node, null);
        }

        public Void visitThrows(ThrowsTree node, Void p) {
            printDocTree("name", node.getExceptionName());
            printList("desc", node.getDescription());
            return visitBlockTag(node, null);
        }

        public Void visitUnknownBlockTag(UnknownBlockTagTree node, Void p) {
            printString("name", node.getTagName());
            printList("content", node.getContent());
            return visitBlockTag(node, null);
        }

        public Void visitUnknownInlineTag(UnknownInlineTagTree node, Void p) {
            printString("name", node.getTagName());
            printList("content", node.getContent());
            return visitInlineTag(node, null);
        }

        public Void visitUses(UsesTree node, Void p) {
            printString("kind", node.getKind().name());
            printDocTree("serviceType", node.getServiceType());
            printList("description", node.getDescription());
            return visitBlockTag(node, null);
        }

        public Void visitValue(ValueTree node, Void p) {
            printDocTree("value", node.getReference());
            return visitInlineTag(node, null);
        }

        public Void visitVersion(VersionTree node, Void p) {
            printList("body", node.getBody());
            return visitBlockTag(node, null);
        }

        public Void visitOther(DocTree node, Void p) {
            return visitTree(node, null);
        }

        public Void visitBlockTag(DocTree node, Void p) {
            return visitTree(node, null);
        }

        public Void visitInlineTag(DocTree node, Void p) {
            return visitTree(node, null);
        }

        public Void visitTree(DocTree node, Void p) {
            return null;
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Symbol visitor">

    protected Symbol.Visitor<Void,Void> symVisitor = new SymbolVisitor();

    /**
     * Default visitor class for Symbol objects.
     * Note: each visitXYZ method ends by calling the corresponding
     * visit method for its superclass.
     */
    class SymbolVisitor implements Symbol.Visitor<Void,Void> {
        @Override
        public Void visitClassSymbol(ClassSymbol sym, Void ignore) {
            printName("fullname", sym.fullname);
            printName("flatname", sym.flatname);
            printScope("members", sym.members_field);
            printFileObject("sourcefile", sym.sourcefile);
            printFileObject("classfile", sym.classfile);
            // trans-local?
            // pool?
            return visitTypeSymbol(sym, null);
        }

        @Override
        public Void visitMethodSymbol(MethodSymbol sym, Void ignore) {
            // code
            printList("params", sym.params);
            return visitSymbol(sym, null);
        }

        @Override
        public Void visitPackageSymbol(PackageSymbol sym, Void ignore) {
            printName("fullname", sym.fullname);
            printScope("members", sym.members_field);
            printSymbol("package-info", sym.package_info, Details.SUMMARY);
            return visitTypeSymbol(sym, null);
        }

        @Override
        public Void visitOperatorSymbol(OperatorSymbol sym, Void ignore) {
            printInt("opcode", sym.opcode);
            return visitMethodSymbol(sym, null);
        }

        @Override
        public Void visitVarSymbol(VarSymbol sym, Void ignore) {
            printInt("pos", sym.pos);
            printInt("adm", sym.adr);
            // data is a private field, and the standard accessors may
            // mutate it as part of lazy evaluation. Therefore, use
            // reflection to get the raw data.
            printObject("data", getField(sym, VarSymbol.class, "data"), Details.SUMMARY);
            return visitSymbol(sym, null);
        }

        @Override
        public Void visitTypeSymbol(TypeSymbol sym, Void ignore) {
            return visitSymbol(sym, null);
        }

        @Override
        public Void visitSymbol(Symbol sym, Void ignore) {
            return null;
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Type visitor">

    protected Type.Visitor<Void,Void> typeVisitor = new TypeVisitor();

    /**
     * Default visitor class for Type objects.
     * Note: each visitXYZ method ends by calling the corresponding
     * visit method for its superclass.
     */
    public class TypeVisitor implements Type.Visitor<Void,Void> {
        public Void visitArrayType(ArrayType type, Void ignore) {
            printType("elemType", type.elemtype, Details.FULL);
            return visitType(type, null);
        }

        public Void visitCapturedType(CapturedType type, Void ignore) {
            printType("wildcard", type.wildcard, Details.FULL);
            return visitTypeVar(type, null);
        }

        public Void visitClassType(ClassType type, Void ignore) {
            printType("outer", type.getEnclosingType(), Details.SUMMARY);
            printList("typarams", type.typarams_field);
            printList("allparams", type.allparams_field);
            printType("supertype", type.supertype_field, Details.SUMMARY);
            printList("interfaces", type.interfaces_field);
            printList("allinterfaces", type.all_interfaces_field);
            return visitType(type, null);
        }

        public Void visitErrorType(ErrorType type, Void ignore) {
            printType("originalType", type.getOriginalType(), Details.FULL);
            return visitClassType(type, null);
        }

        public Void visitForAll(ForAll type, Void ignore) {
            printList("tvars", type.tvars);
            return visitDelegatedType(type);
        }

        public Void visitMethodType(MethodType type, Void ignore) {
            printList("argtypes", type.argtypes);
            printType("restype", type.restype, Details.FULL);
            printList("thrown", type.thrown);
            printType("recvtype", type.recvtype, Details.FULL);
            return visitType(type, null);
        }

        public Void visitModuleType(ModuleType type, Void ignore) {
            return visitType(type, null);
        }

        public Void visitPackageType(PackageType type, Void ignore) {
            return visitType(type, null);
        }

        public Void visitTypeVar(TypeVar type, Void ignore) {
            // For TypeVars (and not subtypes), the bound should always be
            // null or bot. So, only print the bound for subtypes of TypeVar,
            // or if the bound is (erroneously) not null or bot.
            if (!type.hasTag(TypeTag.TYPEVAR)
                    || !(type.getUpperBound() == null || type.getUpperBound().hasTag(TypeTag.BOT))) {
                printType("bound", type.getUpperBound(), Details.FULL);
            }
            printType("lower", type.lower, Details.FULL);
            return visitType(type, null);
        }

        public Void visitUndetVar(UndetVar type, Void ignore) {
            for (UndetVar.InferenceBound ib: UndetVar.InferenceBound.values())
                printList("bounds." + ib, type.getBounds(ib));
            printInt("declaredCount", type.declaredCount);
            printType("inst", type.getInst(), Details.SUMMARY);
            return visitDelegatedType(type);
        }

        public Void visitWildcardType(WildcardType type, Void ignore) {
            printType("type", type.type, Details.SUMMARY);
            printString("kind", type.kind.name());
            printType("bound", type.bound, Details.SUMMARY);
            return visitType(type, null);
        }

        protected Void visitDelegatedType(DelegatedType type) {
            printType("qtype", type.qtype, Details.FULL);
            return visitType(type, null);
        }

        public Void visitType(Type type, Void ignore) {
            return null;
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Attribute (annotations) visitor">

    protected Attribute.Visitor attrVisitor = new AttributeVisitor();

    /**
     * Default visitor class for Attribute (annotation) objects.
     */
    public class AttributeVisitor implements Attribute.Visitor {

        public void visitConstant(Attribute.Constant a) {
            printObject("value", a.value, Details.SUMMARY);
            visitAttribute(a);
        }

        public void visitClass(Attribute.Class a) {
            printObject("classType", a.classType, Details.SUMMARY);
            visitAttribute(a);
        }

        public void visitCompound(Attribute.Compound a) {
            if (a instanceof Attribute.TypeCompound) {
                Attribute.TypeCompound ta = (Attribute.TypeCompound) a;
                // consider a custom printer?
                printObject("position", ta.position, Details.SUMMARY);
            }
            printObject("synthesized", a.isSynthesized(), Details.SUMMARY);
            printList("values", a.values);
            visitAttribute(a);
        }

        public void visitArray(Attribute.Array a) {
            printList("values", Arrays.asList(a.values));
            visitAttribute(a);
        }

        public void visitEnum(Attribute.Enum a) {
            printSymbol("value", a.value, Details.SUMMARY);
            visitAttribute(a);
        }

        public void visitError(Attribute.Error a) {
            visitAttribute(a);
        }

        public void visitAttribute(Attribute a) {
            printType("type", a.type, Details.SUMMARY);
        }

    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Utility front end">

    /**
     * Utility class to invoke DPrinter from the command line.
     */
    static class Main {
        public static void main(String... args) throws IOException {
            Main m = new Main();
            PrintWriter out = new PrintWriter(System.out);
            try {
                if (args.length == 0)
                    m.usage(out);
                else
                    m.run(out, args);
            } finally {
                out.flush();
            }
        }

        void usage(PrintWriter out) {
            out.println("Usage:");
            out.println("  java " + Main.class.getName() + " mode [options] [javac-options]");
            out.print("where mode is one of: ");
            String sep = "";
            for (Handler h: getHandlers().values()) {
                out.print(sep);
                out.print(h.name);
                sep = ", ";
            }
            out.println();
            out.println("and where options include:");
            out.println("  -before PARSE|ENTER|ANALYZE|GENERATE|ANNOTATION_PROCESSING|ANNOTATION_PROCESSING_ROUND");
            out.println("  -after PARSE|ENTER|ANALYZE|GENERATE|ANNOTATION_PROCESSING|ANNOTATION_PROCESSING_ROUND");
            out.println("  -showPositions");
            out.println("  -showSource");
            out.println("  -showTreeSymbols");
            out.println("  -showTreeTypes");
            out.println("  -hideEmptyItems");
            out.println("  -hideNulls");
        }

        void run(PrintWriter out, String... args) throws IOException {
            JavaCompiler c = ToolProvider.getSystemJavaCompiler();
            StandardJavaFileManager fm = c.getStandardFileManager(null, null, null);

            // DPrinter options
            final Set<TaskEvent.Kind> before = EnumSet.noneOf(TaskEvent.Kind.class);
            final Set<TaskEvent.Kind> after = EnumSet.noneOf(TaskEvent.Kind.class);
            boolean showPositions = false;
            boolean showSource = false;
            boolean showTreeSymbols = false;
            boolean showTreeTypes = false;
            boolean showEmptyItems = true;
            boolean showNulls = true;

            // javac options
            Collection<String> options = new ArrayList<String>();
            Collection<File> files = new ArrayList<File>();
            String classpath = null;
            String classoutdir = null;

            final Handler h = getHandlers().get(args[0]);
            if (h == null)
                throw new IllegalArgumentException(args[0]);

            for (int i = 1; i < args.length; i++) {
                String arg = args[i];
                if (arg.equals("-before") && i + 1 < args.length) {
                    before.add(getKind(args[++i]));
                } else if (arg.equals("-after") && i + 1 < args.length) {
                    after.add(getKind(args[++i]));
                } else if (arg.equals("-showPositions")) {
                    showPositions = true;
                } else if (arg.equals("-showSource")) {
                    showSource = true;
                } else if (arg.equals("-showTreeSymbols")) {
                    showTreeSymbols = true;
                } else if (arg.equals("-showTreeTypes")) {
                    showTreeTypes = true;
                } else if (arg.equals("-hideEmptyLists")) {
                    showEmptyItems = false;
                } else if (arg.equals("-hideNulls")) {
                    showNulls = false;
                } else if (arg.equals("-classpath") && i + 1 < args.length) {
                    classpath = args[++i];
                } else if (arg.equals("-d") && i + 1 < args.length) {
                    classoutdir = args[++i];
                } else if (arg.startsWith("-")) {
                    int n = c.isSupportedOption(arg);
                    if (n < 0) throw new IllegalArgumentException(arg);
                    options.add(arg);
                    while (n > 0) options.add(args[++i]);
                } else if (arg.endsWith(".java")) {
                    files.add(new File(arg));
                }
            }

            if (classoutdir != null) {
                fm.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(new File(classoutdir)));
            }

            if (classpath != null) {
                Collection<File> path = new ArrayList<File>();
                for (String p: classpath.split(File.pathSeparator)) {
                    if (p.isEmpty()) continue;
                    File f = new File(p);
                    if (f.exists()) path.add(f);
                }
                fm.setLocation(StandardLocation.CLASS_PATH, path);
            }
            Iterable<? extends JavaFileObject> fos = fm.getJavaFileObjectsFromFiles(files);

            JavacTask task = (JavacTask) c.getTask(out, fm, null, options, null, fos);
            final Trees trees = Trees.instance(task);

            final DPrinter dprinter = new DPrinter(out, trees);
            dprinter.source(showSource)
                    .emptyItems(showEmptyItems)
                    .nulls(showNulls)
                    .positions(showPositions)
                    .treeSymbols(showTreeSymbols)
                    .treeTypes(showTreeTypes);

            if (before.isEmpty() && after.isEmpty()) {
                if (h.name.equals("trees") && !showTreeSymbols && !showTreeTypes)
                    after.add(TaskEvent.Kind.PARSE);
                else
                    after.add(TaskEvent.Kind.ANALYZE);
            }

            task.addTaskListener(new TaskListener() {
                public void started(TaskEvent e) {
                    if (before.contains(e.getKind()))
                        handle(e);
                }

                public void finished(TaskEvent e) {
                    if (after.contains(e.getKind()))
                        handle(e);
                }

                private void handle(TaskEvent e) {
                    JCCompilationUnit unit = (JCCompilationUnit) e.getCompilationUnit();
                     switch (e.getKind()) {
                         case PARSE:
                         case ENTER:
                             h.handle(e.getSourceFile().getName(),
                                     unit, unit,
                                     dprinter);
                             break;

                         default:
                             TypeElement elem = e.getTypeElement();
                             h.handle(elem.toString(),
                                     unit, (JCTree) trees.getTree(elem),
                                     dprinter);
                             break;
                     }
                }
            });

            task.call();
        }

        TaskEvent.Kind getKind(String s) {
            return TaskEvent.Kind.valueOf(s.toUpperCase());
        }

        static protected abstract class Handler {
            final String name;
            Handler(String name) {
                this.name = name;
            }
            abstract void handle(String label,
                    JCCompilationUnit unit, JCTree tree,
                    DPrinter dprinter);
        }

        Map<String,Handler> getHandlers() {
            Map<String,Handler> map = new HashMap<String, Handler>();
            for (Handler h: defaultHandlers) {
                map.put(h.name, h);
            }
            return map;
        }

        protected final Handler[] defaultHandlers = {
            new Handler("trees") {
                @Override
                void handle(String name, JCCompilationUnit unit, JCTree tree, DPrinter dprinter) {
                    dprinter.printTree(name, tree);
                    dprinter.out.println();
                }
            },

            new Handler("doctrees") {
                @Override
                void handle(final String name, final JCCompilationUnit unit, JCTree tree, final DPrinter dprinter) {
                    TreeScanner ds = new DeclScanner() {
                        public void visitDecl(JCTree tree, Symbol sym) {
                            DocTree dt = unit.docComments.getCommentTree(tree);
                            if (dt != null) {
                                String label = (sym == null) ? Pretty.toSimpleString(tree) : sym.name.toString();
                                dprinter.printDocTree(label, dt);
                                dprinter.out.println();
                            }
                        }
                    };
                    ds.scan(tree);
                }
            },

            new Handler("symbols") {
                @Override
                void handle(String name, JCCompilationUnit unit, JCTree tree, final DPrinter dprinter) {
                    TreeScanner ds = new DeclScanner() {
                        public void visitDecl(JCTree tree, Symbol sym) {
                            String label = (sym == null) ? Pretty.toSimpleString(tree) : sym.name.toString();
                            dprinter.printSymbol(label, sym);
                            dprinter.out.println();
                        }
                    };
                    ds.scan(tree);
                }
            },

            new Handler("types") {
                @Override
                void handle(String name, JCCompilationUnit unit, JCTree tree, final DPrinter dprinter) {
                    TreeScanner ts = new TreeScanner() {
                        @Override
                        public void scan(JCTree tree) {
                            if (tree == null) {
                                return;
                            }
                            if (tree.type != null) {
                                String label = Pretty.toSimpleString(tree);
                                dprinter.printType(label, tree.type);
                                dprinter.out.println();
                            }
                            super.scan(tree);
                        }
                    };
                    ts.scan(tree);
                }
            }
        };
    }

    protected static abstract class DeclScanner extends TreeScanner {
        @Override
        public void visitClassDef(JCClassDecl tree) {
            visitDecl(tree, tree.sym);
            super.visitClassDef(tree);
        }

        @Override
        public void visitMethodDef(JCMethodDecl tree) {
            visitDecl(tree, tree.sym);
            super.visitMethodDef(tree);
        }

        @Override
        public void visitVarDef(JCVariableDecl tree) {
            visitDecl(tree, tree.sym);
            super.visitVarDef(tree);
        }

        protected abstract void visitDecl(JCTree tree, Symbol sym);
    }

    // </editor-fold>

}
