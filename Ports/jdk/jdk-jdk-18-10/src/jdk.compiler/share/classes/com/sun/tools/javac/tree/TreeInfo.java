/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package com.sun.tools.javac.tree;



import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.JCTree.JCPolyExpression.*;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;

import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.Kinds.Kind.*;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import static com.sun.tools.javac.code.TypeTag.BOOLEAN;
import static com.sun.tools.javac.code.TypeTag.BOT;
import static com.sun.tools.javac.tree.JCTree.Tag.*;
import static com.sun.tools.javac.tree.JCTree.Tag.BLOCK;
import static com.sun.tools.javac.tree.JCTree.Tag.SYNCHRONIZED;

import javax.tools.JavaFileObject;

import java.util.function.ToIntFunction;

import static com.sun.tools.javac.tree.JCTree.JCOperatorExpression.OperandPos.LEFT;
import static com.sun.tools.javac.tree.JCTree.JCOperatorExpression.OperandPos.RIGHT;

/** Utility class containing inspector methods for trees.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class TreeInfo {

    public static List<JCExpression> args(JCTree t) {
        switch (t.getTag()) {
            case APPLY:
                return ((JCMethodInvocation)t).args;
            case NEWCLASS:
                return ((JCNewClass)t).args;
            default:
                return null;
        }
    }

    /** Is tree a constructor declaration?
     */
    public static boolean isConstructor(JCTree tree) {
        if (tree.hasTag(METHODDEF)) {
            Name name = ((JCMethodDecl) tree).name;
            return name == name.table.names.init;
        } else {
            return false;
        }
    }

    public static boolean isCanonicalConstructor(JCTree tree) {
        // the record flag is only set to the canonical constructor
        return isConstructor(tree) && (((JCMethodDecl)tree).sym.flags_field & RECORD) != 0;
    }

    public static boolean isCompactConstructor(JCTree tree) {
        // the record flag is only set to the canonical constructor
        return isCanonicalConstructor(tree) && (((JCMethodDecl)tree).sym.flags_field & COMPACT_RECORD_CONSTRUCTOR) != 0;
    }

    public static boolean isReceiverParam(JCTree tree) {
        if (tree.hasTag(VARDEF)) {
            return ((JCVariableDecl)tree).nameexpr != null;
        } else {
            return false;
        }
    }

    /** Is there a constructor declaration in the given list of trees?
     */
    public static boolean hasConstructors(List<JCTree> trees) {
        for (List<JCTree> l = trees; l.nonEmpty(); l = l.tail)
            if (isConstructor(l.head)) return true;
        return false;
    }

    /** Is there a constructor invocation in the given list of trees?
     */
    public static Name getConstructorInvocationName(List<? extends JCTree> trees, Names names) {
        for (JCTree tree : trees) {
            if (tree.hasTag(EXEC)) {
                JCExpressionStatement stat = (JCExpressionStatement)tree;
                if (stat.expr.hasTag(APPLY)) {
                    JCMethodInvocation apply = (JCMethodInvocation)stat.expr;
                    Name methName = TreeInfo.name(apply.meth);
                    if (methName == names._this ||
                        methName == names._super) {
                        return methName;
                    }
                }
            }
        }
        return names.empty;
    }

    public static boolean isMultiCatch(JCCatch catchClause) {
        return catchClause.param.vartype.hasTag(TYPEUNION);
    }

    /** Is statement an initializer for a synthetic field?
     */
    public static boolean isSyntheticInit(JCTree stat) {
        if (stat.hasTag(EXEC)) {
            JCExpressionStatement exec = (JCExpressionStatement)stat;
            if (exec.expr.hasTag(ASSIGN)) {
                JCAssign assign = (JCAssign)exec.expr;
                if (assign.lhs.hasTag(SELECT)) {
                    JCFieldAccess select = (JCFieldAccess)assign.lhs;
                    if (select.sym != null &&
                        (select.sym.flags() & SYNTHETIC) != 0) {
                        Name selected = name(select.selected);
                        if (selected != null && selected == selected.table.names._this)
                            return true;
                    }
                }
            }
        }
        return false;
    }

    /** If the expression is a method call, return the method name, null
     *  otherwise. */
    public static Name calledMethodName(JCTree tree) {
        if (tree.hasTag(EXEC)) {
            JCExpressionStatement exec = (JCExpressionStatement)tree;
            if (exec.expr.hasTag(APPLY)) {
                Name mname = TreeInfo.name(((JCMethodInvocation) exec.expr).meth);
                return mname;
            }
        }
        return null;
    }

    /** Is this a call to this or super?
     */
    public static boolean isSelfCall(JCTree tree) {
        Name name = calledMethodName(tree);
        if (name != null) {
            Names names = name.table.names;
            return name==names._this || name==names._super;
        } else {
            return false;
        }
    }

    /** Is this tree a 'this' identifier?
     */
    public static boolean isThisQualifier(JCTree tree) {
        switch (tree.getTag()) {
            case PARENS:
                return isThisQualifier(skipParens(tree));
            case IDENT: {
                JCIdent id = (JCIdent)tree;
                return id.name == id.name.table.names._this;
            }
            default:
                return false;
        }
    }

    /** Is this tree an identifier, possibly qualified by 'this'?
     */
    public static boolean isIdentOrThisDotIdent(JCTree tree) {
        switch (tree.getTag()) {
            case PARENS:
                return isIdentOrThisDotIdent(skipParens(tree));
            case IDENT:
                return true;
            case SELECT:
                return isThisQualifier(((JCFieldAccess)tree).selected);
            default:
                return false;
        }
    }

    /** Is this a call to super?
     */
    public static boolean isSuperCall(JCTree tree) {
        Name name = calledMethodName(tree);
        if (name != null) {
            Names names = name.table.names;
            return name==names._super;
        } else {
            return false;
        }
    }

    public static List<JCVariableDecl> recordFields(JCClassDecl tree) {
        return tree.defs.stream()
                .filter(t -> t.hasTag(VARDEF))
                .map(t -> (JCVariableDecl)t)
                .filter(vd -> (vd.getModifiers().flags & (Flags.RECORD)) == RECORD)
                .collect(List.collector());
    }

    public static List<Type> recordFieldTypes(JCClassDecl tree) {
        return recordFields(tree).stream()
                .map(vd -> vd.type)
                .collect(List.collector());
    }

    /** Is this a constructor whose first (non-synthetic) statement is not
     *  of the form this(...)?
     */
    public static boolean isInitialConstructor(JCTree tree) {
        JCMethodInvocation app = firstConstructorCall(tree);
        if (app == null) return false;
        Name meth = name(app.meth);
        return meth == null || meth != meth.table.names._this;
    }

    /** Return the first call in a constructor definition. */
    public static JCMethodInvocation firstConstructorCall(JCTree tree) {
        if (!tree.hasTag(METHODDEF)) return null;
        JCMethodDecl md = (JCMethodDecl) tree;
        Names names = md.name.table.names;
        if (md.name != names.init) return null;
        if (md.body == null) return null;
        List<JCStatement> stats = md.body.stats;
        // Synthetic initializations can appear before the super call.
        while (stats.nonEmpty() && isSyntheticInit(stats.head))
            stats = stats.tail;
        if (stats.isEmpty()) return null;
        if (!stats.head.hasTag(EXEC)) return null;
        JCExpressionStatement exec = (JCExpressionStatement) stats.head;
        if (!exec.expr.hasTag(APPLY)) return null;
        return (JCMethodInvocation)exec.expr;
    }

    /** Return true if a tree represents a diamond new expr. */
    public static boolean isDiamond(JCTree tree) {
        switch(tree.getTag()) {
            case TYPEAPPLY: return ((JCTypeApply)tree).getTypeArguments().isEmpty();
            case NEWCLASS: return isDiamond(((JCNewClass)tree).clazz);
            case ANNOTATED_TYPE: return isDiamond(((JCAnnotatedType)tree).underlyingType);
            default: return false;
        }
    }

    public static boolean isEnumInit(JCTree tree) {
        switch (tree.getTag()) {
            case VARDEF:
                return (((JCVariableDecl)tree).mods.flags & ENUM) != 0;
            default:
                return false;
        }
    }

    /** set 'polyKind' on given tree */
    public static void setPolyKind(JCTree tree, PolyKind pkind) {
        switch (tree.getTag()) {
            case APPLY:
                ((JCMethodInvocation)tree).polyKind = pkind;
                break;
            case NEWCLASS:
                ((JCNewClass)tree).polyKind = pkind;
                break;
            case REFERENCE:
                ((JCMemberReference)tree).refPolyKind = pkind;
                break;
            default:
                throw new AssertionError("Unexpected tree: " + tree);
        }
    }

    /** set 'varargsElement' on given tree */
    public static void setVarargsElement(JCTree tree, Type varargsElement) {
        switch (tree.getTag()) {
            case APPLY:
                ((JCMethodInvocation)tree).varargsElement = varargsElement;
                break;
            case NEWCLASS:
                ((JCNewClass)tree).varargsElement = varargsElement;
                break;
            case REFERENCE:
                ((JCMemberReference)tree).varargsElement = varargsElement;
                break;
            default:
                throw new AssertionError("Unexpected tree: " + tree);
        }
    }

    /** Return true if the tree corresponds to an expression statement */
    public static boolean isExpressionStatement(JCExpression tree) {
        switch(tree.getTag()) {
            case PREINC: case PREDEC:
            case POSTINC: case POSTDEC:
            case ASSIGN:
            case BITOR_ASG: case BITXOR_ASG: case BITAND_ASG:
            case SL_ASG: case SR_ASG: case USR_ASG:
            case PLUS_ASG: case MINUS_ASG:
            case MUL_ASG: case DIV_ASG: case MOD_ASG:
            case APPLY: case NEWCLASS:
            case ERRONEOUS:
                return true;
            default:
                return false;
        }
    }

    /** Return true if the tree corresponds to a statement */
    public static boolean isStatement(JCTree tree) {
        return (tree instanceof JCStatement) &&
                !tree.hasTag(CLASSDEF) &&
                !tree.hasTag(Tag.BLOCK) &&
                !tree.hasTag(METHODDEF);
    }

    /**
     * Return true if the AST corresponds to a static select of the kind A.B
     */
    public static boolean isStaticSelector(JCTree base, Names names) {
        if (base == null)
            return false;
        switch (base.getTag()) {
            case IDENT:
                JCIdent id = (JCIdent)base;
                return id.name != names._this &&
                        id.name != names._super &&
                        isStaticSym(base);
            case SELECT:
                return isStaticSym(base) &&
                    isStaticSelector(((JCFieldAccess)base).selected, names);
            case TYPEAPPLY:
            case TYPEARRAY:
                return true;
            case ANNOTATED_TYPE:
                return isStaticSelector(((JCAnnotatedType)base).underlyingType, names);
            default:
                return false;
        }
    }
    //where
        private static boolean isStaticSym(JCTree tree) {
            Symbol sym = symbol(tree);
            return (sym.kind == TYP || sym.kind == PCK);
        }

    /** Return true if a tree represents the null literal. */
    public static boolean isNull(JCTree tree) {
        if (!tree.hasTag(LITERAL))
            return false;
        JCLiteral lit = (JCLiteral) tree;
        return (lit.typetag == BOT);
    }

    /** Return true iff this tree is a child of some annotation. */
    public static boolean isInAnnotation(Env<?> env, JCTree tree) {
        TreePath tp = TreePath.getPath(env.toplevel, tree);
        if (tp != null) {
            for (Tree t : tp) {
                if (t.getKind() == Tree.Kind.ANNOTATION)
                    return true;
            }
        }
        return false;
    }

    public static String getCommentText(Env<?> env, JCTree tree) {
        DocCommentTable docComments = (tree.hasTag(JCTree.Tag.TOPLEVEL))
                ? ((JCCompilationUnit) tree).docComments
                : env.toplevel.docComments;
        return (docComments == null) ? null : docComments.getCommentText(tree);
    }

    public static DCTree.DCDocComment getCommentTree(Env<?> env, JCTree tree) {
        DocCommentTable docComments = (tree.hasTag(JCTree.Tag.TOPLEVEL))
                ? ((JCCompilationUnit) tree).docComments
                : env.toplevel.docComments;
        return (docComments == null) ? null : docComments.getCommentTree(tree);
    }

    /** The position of the first statement in a block, or the position of
     *  the block itself if it is empty.
     */
    public static int firstStatPos(JCTree tree) {
        if (tree.hasTag(BLOCK) && ((JCBlock) tree).stats.nonEmpty())
            return ((JCBlock) tree).stats.head.pos;
        else
            return tree.pos;
    }

    /** The end position of given tree, if it is a block with
     *  defined endpos.
     */
    public static int endPos(JCTree tree) {
        if (tree.hasTag(BLOCK) && ((JCBlock) tree).endpos != Position.NOPOS)
            return ((JCBlock) tree).endpos;
        else if (tree.hasTag(SYNCHRONIZED))
            return endPos(((JCSynchronized) tree).body);
        else if (tree.hasTag(TRY)) {
            JCTry t = (JCTry) tree;
            return endPos((t.finalizer != null) ? t.finalizer
                          : (t.catchers.nonEmpty() ? t.catchers.last().body : t.body));
        } else if (tree.hasTag(SWITCH) &&
                   ((JCSwitch) tree).endpos != Position.NOPOS) {
            return ((JCSwitch) tree).endpos;
        } else if (tree.hasTag(SWITCH_EXPRESSION) &&
                   ((JCSwitchExpression) tree).endpos != Position.NOPOS) {
            return ((JCSwitchExpression) tree).endpos;
        } else
            return tree.pos;
    }


    /** Get the start position for a tree node.  The start position is
     * defined to be the position of the first character of the first
     * token of the node's source text.
     * @param tree  The tree node
     */
    public static int getStartPos(JCTree tree) {
        if (tree == null)
            return Position.NOPOS;

        switch(tree.getTag()) {
            case MODULEDEF: {
                JCModuleDecl md = (JCModuleDecl)tree;
                return md.mods.annotations.isEmpty() ? md.pos :
                       md.mods.annotations.head.pos;
            }
            case PACKAGEDEF: {
                JCPackageDecl pd = (JCPackageDecl)tree;
                return pd.annotations.isEmpty() ? pd.pos :
                       pd.annotations.head.pos;
            }
            case APPLY:
                return getStartPos(((JCMethodInvocation) tree).meth);
            case ASSIGN:
                return getStartPos(((JCAssign) tree).lhs);
            case BITOR_ASG: case BITXOR_ASG: case BITAND_ASG:
            case SL_ASG: case SR_ASG: case USR_ASG:
            case PLUS_ASG: case MINUS_ASG: case MUL_ASG:
            case DIV_ASG: case MOD_ASG:
            case OR: case AND: case BITOR:
            case BITXOR: case BITAND: case EQ:
            case NE: case LT: case GT:
            case LE: case GE: case SL:
            case SR: case USR: case PLUS:
            case MINUS: case MUL: case DIV:
            case MOD:
            case POSTINC:
            case POSTDEC:
                return getStartPos(((JCOperatorExpression) tree).getOperand(LEFT));
            case CLASSDEF: {
                JCClassDecl node = (JCClassDecl)tree;
                if (node.mods.pos != Position.NOPOS)
                    return node.mods.pos;
                break;
            }
            case CONDEXPR:
                return getStartPos(((JCConditional) tree).cond);
            case EXEC:
                return getStartPos(((JCExpressionStatement) tree).expr);
            case INDEXED:
                return getStartPos(((JCArrayAccess) tree).indexed);
            case METHODDEF: {
                JCMethodDecl node = (JCMethodDecl)tree;
                if (node.mods.pos != Position.NOPOS)
                    return node.mods.pos;
                if (node.typarams.nonEmpty()) // List.nil() used for no typarams
                    return getStartPos(node.typarams.head);
                return node.restype == null ? node.pos : getStartPos(node.restype);
            }
            case SELECT:
                return getStartPos(((JCFieldAccess) tree).selected);
            case TYPEAPPLY:
                return getStartPos(((JCTypeApply) tree).clazz);
            case TYPEARRAY:
                return getStartPos(((JCArrayTypeTree) tree).elemtype);
            case TYPETEST:
                return getStartPos(((JCInstanceOf) tree).expr);
            case ANNOTATED_TYPE: {
                JCAnnotatedType node = (JCAnnotatedType) tree;
                if (node.annotations.nonEmpty()) {
                    if (node.underlyingType.hasTag(TYPEARRAY) ||
                            node.underlyingType.hasTag(SELECT)) {
                        return getStartPos(node.underlyingType);
                    } else {
                        return getStartPos(node.annotations.head);
                    }
                } else {
                    return getStartPos(node.underlyingType);
                }
            }
            case NEWCLASS: {
                JCNewClass node = (JCNewClass)tree;
                if (node.encl != null)
                    return getStartPos(node.encl);
                break;
            }
            case VARDEF: {
                JCVariableDecl node = (JCVariableDecl)tree;
                if (node.startPos != Position.NOPOS) {
                    return node.startPos;
                } else if (node.mods.pos != Position.NOPOS) {
                    return node.mods.pos;
                } else if (node.vartype == null || node.vartype.pos == Position.NOPOS) {
                    //if there's no type (partially typed lambda parameter)
                    //simply return node position
                    return node.pos;
                } else {
                    return getStartPos(node.vartype);
                }
            }
            case BINDINGPATTERN: {
                JCBindingPattern node = (JCBindingPattern)tree;
                return getStartPos(node.var);
            }
            case GUARDPATTERN: {
                JCGuardPattern node = (JCGuardPattern) tree;
                return getStartPos(node.patt);
            }
            case ERRONEOUS: {
                JCErroneous node = (JCErroneous)tree;
                if (node.errs != null && node.errs.nonEmpty())
                    return getStartPos(node.errs.head);
            }
        }
        return tree.pos;
    }

    /** The end position of given tree, given  a table of end positions generated by the parser
     */
    public static int getEndPos(JCTree tree, EndPosTable endPosTable) {
        if (tree == null)
            return Position.NOPOS;

        if (endPosTable == null) {
            // fall back on limited info in the tree
            return endPos(tree);
        }

        int mapPos = endPosTable.getEndPos(tree);
        if (mapPos != Position.NOPOS)
            return mapPos;

        switch(tree.getTag()) {
            case BITOR_ASG: case BITXOR_ASG: case BITAND_ASG:
            case SL_ASG: case SR_ASG: case USR_ASG:
            case PLUS_ASG: case MINUS_ASG: case MUL_ASG:
            case DIV_ASG: case MOD_ASG:
            case OR: case AND: case BITOR:
            case BITXOR: case BITAND: case EQ:
            case NE: case LT: case GT:
            case LE: case GE: case SL:
            case SR: case USR: case PLUS:
            case MINUS: case MUL: case DIV:
            case MOD:
            case POS:
            case NEG:
            case NOT:
            case COMPL:
            case PREINC:
            case PREDEC:
                return getEndPos(((JCOperatorExpression) tree).getOperand(RIGHT), endPosTable);
            case CASE:
                return getEndPos(((JCCase) tree).stats.last(), endPosTable);
            case CATCH:
                return getEndPos(((JCCatch) tree).body, endPosTable);
            case CONDEXPR:
                return getEndPos(((JCConditional) tree).falsepart, endPosTable);
            case FORLOOP:
                return getEndPos(((JCForLoop) tree).body, endPosTable);
            case FOREACHLOOP:
                return getEndPos(((JCEnhancedForLoop) tree).body, endPosTable);
            case IF: {
                JCIf node = (JCIf)tree;
                if (node.elsepart == null) {
                    return getEndPos(node.thenpart, endPosTable);
                } else {
                    return getEndPos(node.elsepart, endPosTable);
                }
            }
            case LABELLED:
                return getEndPos(((JCLabeledStatement) tree).body, endPosTable);
            case MODIFIERS:
                return getEndPos(((JCModifiers) tree).annotations.last(), endPosTable);
            case SYNCHRONIZED:
                return getEndPos(((JCSynchronized) tree).body, endPosTable);
            case TOPLEVEL:
                return getEndPos(((JCCompilationUnit) tree).defs.last(), endPosTable);
            case TRY: {
                JCTry node = (JCTry)tree;
                if (node.finalizer != null) {
                    return getEndPos(node.finalizer, endPosTable);
                } else if (!node.catchers.isEmpty()) {
                    return getEndPos(node.catchers.last(), endPosTable);
                } else {
                    return getEndPos(node.body, endPosTable);
                }
            }
            case WILDCARD:
                return getEndPos(((JCWildcard) tree).inner, endPosTable);
            case TYPECAST:
                return getEndPos(((JCTypeCast) tree).expr, endPosTable);
            case TYPETEST:
                return getEndPos(((JCInstanceOf) tree).pattern, endPosTable);
            case WHILELOOP:
                return getEndPos(((JCWhileLoop) tree).body, endPosTable);
            case ANNOTATED_TYPE:
                return getEndPos(((JCAnnotatedType) tree).underlyingType, endPosTable);
            case PARENTHESIZEDPATTERN: {
                JCParenthesizedPattern node = (JCParenthesizedPattern) tree;
                return getEndPos(node.pattern, endPosTable);
            }
            case GUARDPATTERN: {
                JCGuardPattern node = (JCGuardPattern) tree;
                return getEndPos(node.expr, endPosTable);
            }
            case ERRONEOUS: {
                JCErroneous node = (JCErroneous)tree;
                if (node.errs != null && node.errs.nonEmpty())
                    return getEndPos(node.errs.last(), endPosTable);
            }
        }
        return Position.NOPOS;
    }


    /** A DiagnosticPosition with the preferred position set to the
     *  end position of given tree, if it is a block with
     *  defined endpos.
     */
    public static DiagnosticPosition diagEndPos(final JCTree tree) {
        final int endPos = TreeInfo.endPos(tree);
        return new DiagnosticPosition() {
            public JCTree getTree() { return tree; }
            public int getStartPosition() { return TreeInfo.getStartPos(tree); }
            public int getPreferredPosition() { return endPos; }
            public int getEndPosition(EndPosTable endPosTable) {
                return TreeInfo.getEndPos(tree, endPosTable);
            }
        };
    }

    public enum PosKind {
        START_POS(TreeInfo::getStartPos),
        FIRST_STAT_POS(TreeInfo::firstStatPos),
        END_POS(TreeInfo::endPos);

        final ToIntFunction<JCTree> posFunc;

        PosKind(ToIntFunction<JCTree> posFunc) {
            this.posFunc = posFunc;
        }

        int toPos(JCTree tree) {
            return posFunc.applyAsInt(tree);
        }
    }

    /** The position of the finalizer of given try/synchronized statement.
     */
    public static int finalizerPos(JCTree tree, PosKind posKind) {
        if (tree.hasTag(TRY)) {
            JCTry t = (JCTry) tree;
            Assert.checkNonNull(t.finalizer);
            return posKind.toPos(t.finalizer);
        } else if (tree.hasTag(SYNCHRONIZED)) {
            return endPos(((JCSynchronized) tree).body);
        } else {
            throw new AssertionError();
        }
    }

    /** Find the position for reporting an error about a symbol, where
     *  that symbol is defined somewhere in the given tree. */
    public static int positionFor(final Symbol sym, final JCTree tree) {
        JCTree decl = declarationFor(sym, tree);
        return ((decl != null) ? decl : tree).pos;
    }

    /** Find the position for reporting an error about a symbol, where
     *  that symbol is defined somewhere in the given tree. */
    public static DiagnosticPosition diagnosticPositionFor(final Symbol sym, final JCTree tree) {
        return diagnosticPositionFor(sym, tree, false);
    }

    public static DiagnosticPosition diagnosticPositionFor(final Symbol sym, final JCTree tree, boolean returnNullIfNotFound) {
        class DiagScanner extends DeclScanner {
            DiagScanner(Symbol sym) {
                super(sym);
            }

            public void visitIdent(JCIdent that) {
                if (that.sym == sym) result = that;
                else super.visitIdent(that);
            }
            public void visitSelect(JCFieldAccess that) {
                if (that.sym == sym) result = that;
                else super.visitSelect(that);
            }
        }
        DiagScanner s = new DiagScanner(sym);
        tree.accept(s);
        JCTree decl = s.result;
        if (decl == null && returnNullIfNotFound) { return null; }
        return ((decl != null) ? decl : tree).pos();
    }

    public static DiagnosticPosition diagnosticPositionFor(final Symbol sym, final List<? extends JCTree> trees) {
        return trees.stream().map(t -> TreeInfo.diagnosticPositionFor(sym, t)).filter(t -> t != null).findFirst().get();
    }

    private static class DeclScanner extends TreeScanner {
        final Symbol sym;

        DeclScanner(final Symbol sym) {
            this.sym = sym;
        }

        JCTree result = null;
        public void scan(JCTree tree) {
            if (tree!=null && result==null)
                tree.accept(this);
        }
        public void visitTopLevel(JCCompilationUnit that) {
            if (that.packge == sym) result = that;
            else super.visitTopLevel(that);
        }
        public void visitModuleDef(JCModuleDecl that) {
            if (that.sym == sym) result = that;
            // no need to scan within module declaration
        }
        public void visitPackageDef(JCPackageDecl that) {
            if (that.packge == sym) result = that;
            else super.visitPackageDef(that);
        }
        public void visitClassDef(JCClassDecl that) {
            if (that.sym == sym) result = that;
            else super.visitClassDef(that);
        }
        public void visitMethodDef(JCMethodDecl that) {
            if (that.sym == sym) result = that;
            else super.visitMethodDef(that);
        }
        public void visitVarDef(JCVariableDecl that) {
            if (that.sym == sym) result = that;
            else super.visitVarDef(that);
        }
        public void visitTypeParameter(JCTypeParameter that) {
            if (that.type != null && that.type.tsym == sym) result = that;
            else super.visitTypeParameter(that);
        }
    }

    /** Find the declaration for a symbol, where
     *  that symbol is defined somewhere in the given tree. */
    public static JCTree declarationFor(final Symbol sym, final JCTree tree) {
        DeclScanner s = new DeclScanner(sym);
        tree.accept(s);
        return s.result;
    }

    public static Env<AttrContext> scopeFor(JCTree node, JCCompilationUnit unit) {
        return scopeFor(pathFor(node, unit));
    }

    public static Env<AttrContext> scopeFor(List<JCTree> path) {
        // TODO: not implemented yet
        throw new UnsupportedOperationException("not implemented yet");
    }

    public static List<JCTree> pathFor(final JCTree node, final JCCompilationUnit unit) {
        class Result extends Error {
            static final long serialVersionUID = -5942088234594905625L;
            List<JCTree> path;
            Result(List<JCTree> path) {
                this.path = path;
            }
        }
        class PathFinder extends TreeScanner {
            List<JCTree> path = List.nil();
            public void scan(JCTree tree) {
                if (tree != null) {
                    path = path.prepend(tree);
                    if (tree == node)
                        throw new Result(path);
                    super.scan(tree);
                    path = path.tail;
                }
            }
        }
        try {
            new PathFinder().scan(unit);
        } catch (Result result) {
            return result.path;
        }
        return List.nil();
    }

    /** Return the statement referenced by a label.
     *  If the label refers to a loop or switch, return that switch
     *  otherwise return the labelled statement itself
     */
    public static JCTree referencedStatement(JCLabeledStatement tree) {
        JCTree t = tree;
        do t = ((JCLabeledStatement) t).body;
        while (t.hasTag(LABELLED));
        switch (t.getTag()) {
        case DOLOOP: case WHILELOOP: case FORLOOP: case FOREACHLOOP: case SWITCH:
            return t;
        default:
            return tree;
        }
    }

    /** Skip parens and return the enclosed expression
     */
    public static JCExpression skipParens(JCExpression tree) {
        while (tree.hasTag(PARENS)) {
            tree = ((JCParens) tree).expr;
        }
        return tree;
    }

    /** Skip parens and return the enclosed expression
     */
    public static JCTree skipParens(JCTree tree) {
        if (tree.hasTag(PARENS))
            return skipParens((JCParens)tree);
        else
            return tree;
    }

    /** Return the types of a list of trees.
     */
    public static List<Type> types(List<? extends JCTree> trees) {
        ListBuffer<Type> ts = new ListBuffer<>();
        for (List<? extends JCTree> l = trees; l.nonEmpty(); l = l.tail)
            ts.append(l.head.type);
        return ts.toList();
    }

    /** If this tree is an identifier or a field or a parameterized type,
     *  return its name, otherwise return null.
     */
    public static Name name(JCTree tree) {
        switch (tree.getTag()) {
        case IDENT:
            return ((JCIdent) tree).name;
        case SELECT:
            return ((JCFieldAccess) tree).name;
        case TYPEAPPLY:
            return name(((JCTypeApply) tree).clazz);
        default:
            return null;
        }
    }

    /** If this tree is a qualified identifier, its return fully qualified name,
     *  otherwise return null.
     */
    public static Name fullName(JCTree tree) {
        tree = skipParens(tree);
        switch (tree.getTag()) {
        case IDENT:
            return ((JCIdent) tree).name;
        case SELECT:
            Name sname = fullName(((JCFieldAccess) tree).selected);
            return sname == null ? null : sname.append('.', name(tree));
        default:
            return null;
        }
    }

    public static Symbol symbolFor(JCTree node) {
        Symbol sym = symbolForImpl(node);

        return sym != null ? sym.baseSymbol() : null;
    }

    private static Symbol symbolForImpl(JCTree node) {
        node = skipParens(node);
        switch (node.getTag()) {
        case TOPLEVEL:
            JCCompilationUnit cut = (JCCompilationUnit) node;
            JCModuleDecl moduleDecl = cut.getModuleDecl();
            if (isModuleInfo(cut) && moduleDecl != null)
                return symbolFor(moduleDecl);
            return cut.packge;
        case MODULEDEF:
            return ((JCModuleDecl) node).sym;
        case PACKAGEDEF:
            return ((JCPackageDecl) node).packge;
        case CLASSDEF:
            return ((JCClassDecl) node).sym;
        case METHODDEF:
            return ((JCMethodDecl) node).sym;
        case VARDEF:
            return ((JCVariableDecl) node).sym;
        case IDENT:
            return ((JCIdent) node).sym;
        case SELECT:
            return ((JCFieldAccess) node).sym;
        case REFERENCE:
            return ((JCMemberReference) node).sym;
        case NEWCLASS:
            return ((JCNewClass) node).constructor;
        case APPLY:
            return symbolFor(((JCMethodInvocation) node).meth);
        case TYPEAPPLY:
            return symbolFor(((JCTypeApply) node).clazz);
        case ANNOTATION:
        case TYPE_ANNOTATION:
        case TYPEPARAMETER:
            if (node.type != null)
                return node.type.tsym;
            return null;
        default:
            return null;
        }
    }

    public static boolean isDeclaration(JCTree node) {
        node = skipParens(node);
        switch (node.getTag()) {
        case PACKAGEDEF:
        case CLASSDEF:
        case METHODDEF:
        case VARDEF:
            return true;
        default:
            return false;
        }
    }

    /** If this tree is an identifier or a field, return its symbol,
     *  otherwise return null.
     */
    public static Symbol symbol(JCTree tree) {
        tree = skipParens(tree);
        switch (tree.getTag()) {
        case IDENT:
            return ((JCIdent) tree).sym;
        case SELECT:
            return ((JCFieldAccess) tree).sym;
        case TYPEAPPLY:
            return symbol(((JCTypeApply) tree).clazz);
        case ANNOTATED_TYPE:
            return symbol(((JCAnnotatedType) tree).underlyingType);
        case REFERENCE:
            return ((JCMemberReference) tree).sym;
        default:
            return null;
        }
    }

    /** If this tree has a modifiers field, return it otherwise return null
     */
    public static JCModifiers getModifiers(JCTree tree) {
        tree = skipParens(tree);
        switch (tree.getTag()) {
            case VARDEF:
                return ((JCVariableDecl) tree).mods;
            case METHODDEF:
                return ((JCMethodDecl) tree).mods;
            case CLASSDEF:
                return ((JCClassDecl) tree).mods;
            case MODULEDEF:
                return ((JCModuleDecl) tree).mods;
        default:
            return null;
        }
    }

    /** Return true if this is a nonstatic selection. */
    public static boolean nonstaticSelect(JCTree tree) {
        tree = skipParens(tree);
        if (!tree.hasTag(SELECT)) return false;
        JCFieldAccess s = (JCFieldAccess) tree;
        Symbol e = symbol(s.selected);
        return e == null || (e.kind != PCK && e.kind != TYP);
    }

    /** If this tree is an identifier or a field, set its symbol, otherwise skip.
     */
    public static void setSymbol(JCTree tree, Symbol sym) {
        tree = skipParens(tree);
        switch (tree.getTag()) {
        case IDENT:
            ((JCIdent) tree).sym = sym; break;
        case SELECT:
            ((JCFieldAccess) tree).sym = sym; break;
        default:
        }
    }

    /** If this tree is a declaration or a block, return its flags field,
     *  otherwise return 0.
     */
    public static long flags(JCTree tree) {
        switch (tree.getTag()) {
        case VARDEF:
            return ((JCVariableDecl) tree).mods.flags;
        case METHODDEF:
            return ((JCMethodDecl) tree).mods.flags;
        case CLASSDEF:
            return ((JCClassDecl) tree).mods.flags;
        case BLOCK:
            return ((JCBlock) tree).flags;
        default:
            return 0;
        }
    }

    /** Return first (smallest) flag in `flags':
     *  pre: flags != 0
     */
    public static long firstFlag(long flags) {
        long flag = 1;
        while ((flag & flags) == 0)
            flag = flag << 1;
        return flag;
    }

    /** Return flags as a string, separated by " ".
     */
    public static String flagNames(long flags) {
        return Flags.toString(flags & ExtendedStandardFlags).trim();
    }

    /** Operator precedences values.
     */
    public static final int
        notExpression = -1,   // not an expression
        noPrec = 0,           // no enclosing expression
        assignPrec = 1,
        assignopPrec = 2,
        condPrec = 3,
        orPrec = 4,
        andPrec = 5,
        bitorPrec = 6,
        bitxorPrec = 7,
        bitandPrec = 8,
        eqPrec = 9,
        ordPrec = 10,
        shiftPrec = 11,
        addPrec = 12,
        mulPrec = 13,
        prefixPrec = 14,
        postfixPrec = 15,
        precCount = 16;


    /** Map operators to their precedence levels.
     */
    public static int opPrec(JCTree.Tag op) {
        switch(op) {
        case POS:
        case NEG:
        case NOT:
        case COMPL:
        case PREINC:
        case PREDEC: return prefixPrec;
        case POSTINC:
        case POSTDEC:
        case NULLCHK: return postfixPrec;
        case ASSIGN: return assignPrec;
        case BITOR_ASG:
        case BITXOR_ASG:
        case BITAND_ASG:
        case SL_ASG:
        case SR_ASG:
        case USR_ASG:
        case PLUS_ASG:
        case MINUS_ASG:
        case MUL_ASG:
        case DIV_ASG:
        case MOD_ASG: return assignopPrec;
        case OR: return orPrec;
        case AND: return andPrec;
        case EQ:
        case NE: return eqPrec;
        case LT:
        case GT:
        case LE:
        case GE: return ordPrec;
        case BITOR: return bitorPrec;
        case BITXOR: return bitxorPrec;
        case BITAND: return bitandPrec;
        case SL:
        case SR:
        case USR: return shiftPrec;
        case PLUS:
        case MINUS: return addPrec;
        case MUL:
        case DIV:
        case MOD: return mulPrec;
        case TYPETEST: return ordPrec;
        default: throw new AssertionError();
        }
    }

    static Tree.Kind tagToKind(JCTree.Tag tag) {
        switch (tag) {
        // Postfix expressions
        case POSTINC:           // _ ++
            return Tree.Kind.POSTFIX_INCREMENT;
        case POSTDEC:           // _ --
            return Tree.Kind.POSTFIX_DECREMENT;

        // Unary operators
        case PREINC:            // ++ _
            return Tree.Kind.PREFIX_INCREMENT;
        case PREDEC:            // -- _
            return Tree.Kind.PREFIX_DECREMENT;
        case POS:               // +
            return Tree.Kind.UNARY_PLUS;
        case NEG:               // -
            return Tree.Kind.UNARY_MINUS;
        case COMPL:             // ~
            return Tree.Kind.BITWISE_COMPLEMENT;
        case NOT:               // !
            return Tree.Kind.LOGICAL_COMPLEMENT;

        // Binary operators

        // Multiplicative operators
        case MUL:               // *
            return Tree.Kind.MULTIPLY;
        case DIV:               // /
            return Tree.Kind.DIVIDE;
        case MOD:               // %
            return Tree.Kind.REMAINDER;

        // Additive operators
        case PLUS:              // +
            return Tree.Kind.PLUS;
        case MINUS:             // -
            return Tree.Kind.MINUS;

        // Shift operators
        case SL:                // <<
            return Tree.Kind.LEFT_SHIFT;
        case SR:                // >>
            return Tree.Kind.RIGHT_SHIFT;
        case USR:               // >>>
            return Tree.Kind.UNSIGNED_RIGHT_SHIFT;

        // Relational operators
        case LT:                // <
            return Tree.Kind.LESS_THAN;
        case GT:                // >
            return Tree.Kind.GREATER_THAN;
        case LE:                // <=
            return Tree.Kind.LESS_THAN_EQUAL;
        case GE:                // >=
            return Tree.Kind.GREATER_THAN_EQUAL;

        // Equality operators
        case EQ:                // ==
            return Tree.Kind.EQUAL_TO;
        case NE:                // !=
            return Tree.Kind.NOT_EQUAL_TO;

        // Bitwise and logical operators
        case BITAND:            // &
            return Tree.Kind.AND;
        case BITXOR:            // ^
            return Tree.Kind.XOR;
        case BITOR:             // |
            return Tree.Kind.OR;

        // Conditional operators
        case AND:               // &&
            return Tree.Kind.CONDITIONAL_AND;
        case OR:                // ||
            return Tree.Kind.CONDITIONAL_OR;

        // Assignment operators
        case MUL_ASG:           // *=
            return Tree.Kind.MULTIPLY_ASSIGNMENT;
        case DIV_ASG:           // /=
            return Tree.Kind.DIVIDE_ASSIGNMENT;
        case MOD_ASG:           // %=
            return Tree.Kind.REMAINDER_ASSIGNMENT;
        case PLUS_ASG:          // +=
            return Tree.Kind.PLUS_ASSIGNMENT;
        case MINUS_ASG:         // -=
            return Tree.Kind.MINUS_ASSIGNMENT;
        case SL_ASG:            // <<=
            return Tree.Kind.LEFT_SHIFT_ASSIGNMENT;
        case SR_ASG:            // >>=
            return Tree.Kind.RIGHT_SHIFT_ASSIGNMENT;
        case USR_ASG:           // >>>=
            return Tree.Kind.UNSIGNED_RIGHT_SHIFT_ASSIGNMENT;
        case BITAND_ASG:        // &=
            return Tree.Kind.AND_ASSIGNMENT;
        case BITXOR_ASG:        // ^=
            return Tree.Kind.XOR_ASSIGNMENT;
        case BITOR_ASG:         // |=
            return Tree.Kind.OR_ASSIGNMENT;

        // Null check (implementation detail), for example, __.getClass()
        case NULLCHK:
            return Tree.Kind.OTHER;

        case ANNOTATION:
            return Tree.Kind.ANNOTATION;
        case TYPE_ANNOTATION:
            return Tree.Kind.TYPE_ANNOTATION;

        case EXPORTS:
            return Tree.Kind.EXPORTS;
        case OPENS:
            return Tree.Kind.OPENS;

        default:
            return null;
        }
    }

    /**
     * Returns the underlying type of the tree if it is an annotated type,
     * or the tree itself otherwise.
     */
    public static JCExpression typeIn(JCExpression tree) {
        switch (tree.getTag()) {
        case ANNOTATED_TYPE:
            return ((JCAnnotatedType)tree).underlyingType;
        case IDENT: /* simple names */
        case TYPEIDENT: /* primitive name */
        case SELECT: /* qualified name */
        case TYPEARRAY: /* array types */
        case WILDCARD: /* wild cards */
        case TYPEPARAMETER: /* type parameters */
        case TYPEAPPLY: /* parameterized types */
        case ERRONEOUS: /* error tree TODO: needed for BadCast JSR308 test case. Better way? */
            return tree;
        default:
            throw new AssertionError("Unexpected type tree: " + tree);
        }
    }

    /* Return the inner-most type of a type tree.
     * For an array that contains an annotated type, return that annotated type.
     * TODO: currently only used by Pretty. Describe behavior better.
     */
    public static JCTree innermostType(JCTree type, boolean skipAnnos) {
        JCTree lastAnnotatedType = null;
        JCTree cur = type;
        loop: while (true) {
            switch (cur.getTag()) {
            case TYPEARRAY:
                lastAnnotatedType = null;
                cur = ((JCArrayTypeTree)cur).elemtype;
                break;
            case WILDCARD:
                lastAnnotatedType = null;
                cur = ((JCWildcard)cur).inner;
                break;
            case ANNOTATED_TYPE:
                lastAnnotatedType = cur;
                cur = ((JCAnnotatedType)cur).underlyingType;
                break;
            default:
                break loop;
            }
        }
        if (!skipAnnos && lastAnnotatedType!=null) {
            return lastAnnotatedType;
        } else {
            return cur;
        }
    }

    private static class TypeAnnotationFinder extends TreeScanner {
        public boolean foundTypeAnno = false;

        @Override
        public void scan(JCTree tree) {
            if (foundTypeAnno || tree == null)
                return;
            super.scan(tree);
        }

        public void visitAnnotation(JCAnnotation tree) {
            foundTypeAnno = foundTypeAnno || tree.hasTag(TYPE_ANNOTATION);
        }
    }

    public static boolean containsTypeAnnotation(JCTree e) {
        TypeAnnotationFinder finder = new TypeAnnotationFinder();
        finder.scan(e);
        return finder.foundTypeAnno;
    }

    public static boolean isModuleInfo(JCCompilationUnit tree) {
        return tree.sourcefile.isNameCompatible("module-info", JavaFileObject.Kind.SOURCE)
                && tree.getModuleDecl() != null;
    }

    public static JCModuleDecl getModule(JCCompilationUnit t) {
        if (t.defs.nonEmpty()) {
            JCTree def = t.defs.head;
            if (def.hasTag(MODULEDEF))
                return (JCModuleDecl) def;
        }
        return null;
    }

    public static boolean isPackageInfo(JCCompilationUnit tree) {
        return tree.sourcefile.isNameCompatible("package-info", JavaFileObject.Kind.SOURCE);
    }

    public static boolean isErrorEnumSwitch(JCExpression selector, List<JCCase> cases) {
        return selector.type.tsym.kind == Kinds.Kind.ERR &&
               cases.stream().flatMap(c -> c.labels.stream())
                             .allMatch(p -> p.hasTag(IDENT));
    }

    public static PatternPrimaryType primaryPatternType(JCPattern pat) {
        return switch (pat.getTag()) {
            case BINDINGPATTERN -> new PatternPrimaryType(((JCBindingPattern) pat).type, true);
            case GUARDPATTERN -> {
                JCGuardPattern guarded = (JCGuardPattern) pat;
                PatternPrimaryType nested = primaryPatternType(guarded.patt);
                boolean unconditional = nested.unconditional();
                if (guarded.expr.type.hasTag(BOOLEAN) && unconditional) {
                    unconditional = false;
                    var constValue = guarded.expr.type.constValue();
                    if (constValue != null && ((int) constValue) == 1) {
                        unconditional = true;
                    }
                }
                yield new PatternPrimaryType(nested.type(), unconditional);
            }
            case PARENTHESIZEDPATTERN -> primaryPatternType(((JCParenthesizedPattern) pat).pattern);
            default -> throw new AssertionError();
        };
    }

    public static JCBindingPattern primaryPatternTree(JCPattern pat) {
        return switch (pat.getTag()) {
            case BINDINGPATTERN -> (JCBindingPattern) pat;
            case GUARDPATTERN -> primaryPatternTree(((JCGuardPattern) pat).patt);
            case PARENTHESIZEDPATTERN -> primaryPatternTree(((JCParenthesizedPattern) pat).pattern);
            default -> throw new AssertionError();
        };
    }

    public record PatternPrimaryType(Type type, boolean unconditional) {}

}
