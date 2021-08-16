/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.comp;

import java.util.EnumSet;
import java.util.Set;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Scope.WriteableScope;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.JCDiagnostic.Error;

import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Type.*;
import com.sun.tools.javac.resources.CompilerProperties.Errors;
import com.sun.tools.javac.tree.JCTree.*;

import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.Kinds.*;
import static com.sun.tools.javac.code.Kinds.Kind.*;
import static com.sun.tools.javac.code.TypeTag.TYPEVAR;

/** Resolves field, method and constructor header, and constructs corresponding Symbols.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class MemberEnter extends JCTree.Visitor {
    protected static final Context.Key<MemberEnter> memberEnterKey = new Context.Key<>();

    private final Enter enter;
    private final Log log;
    private final Check chk;
    private final Attr attr;
    private final Symtab syms;
    private final Annotate annotate;
    private final Types types;
    private final DeferredLintHandler deferredLintHandler;

    public static MemberEnter instance(Context context) {
        MemberEnter instance = context.get(memberEnterKey);
        if (instance == null)
            instance = new MemberEnter(context);
        return instance;
    }

    protected MemberEnter(Context context) {
        context.put(memberEnterKey, this);
        enter = Enter.instance(context);
        log = Log.instance(context);
        chk = Check.instance(context);
        attr = Attr.instance(context);
        syms = Symtab.instance(context);
        annotate = Annotate.instance(context);
        types = Types.instance(context);
        deferredLintHandler = DeferredLintHandler.instance(context);
    }

    /** Construct method type from method signature.
     *  @param typarams    The method's type parameters.
     *  @param params      The method's value parameters.
     *  @param res             The method's result type,
     *                 null if it is a constructor.
     *  @param recvparam       The method's receiver parameter,
     *                 null if none given; TODO: or already set here?
     *  @param thrown      The method's thrown exceptions.
     *  @param env             The method's (local) environment.
     */
    Type signature(MethodSymbol msym,
                   List<JCTypeParameter> typarams,
                   List<JCVariableDecl> params,
                   JCTree res,
                   JCVariableDecl recvparam,
                   List<JCExpression> thrown,
                   Env<AttrContext> env) {

        // Enter and attribute type parameters.
        List<Type> tvars = enter.classEnter(typarams, env);
        attr.attribTypeVariables(typarams, env, true);

        // Enter and attribute value parameters.
        ListBuffer<Type> argbuf = new ListBuffer<>();
        for (List<JCVariableDecl> l = params; l.nonEmpty(); l = l.tail) {
            memberEnter(l.head, env);
            argbuf.append(l.head.vartype.type);
        }

        // Attribute result type, if one is given.
        Type restype = res == null ? syms.voidType : attr.attribType(res, env);

        // Attribute receiver type, if one is given.
        Type recvtype;
        if (recvparam!=null) {
            memberEnter(recvparam, env);
            recvtype = recvparam.vartype.type;
        } else {
            recvtype = null;
        }

        // Attribute thrown exceptions.
        ListBuffer<Type> thrownbuf = new ListBuffer<>();
        for (List<JCExpression> l = thrown; l.nonEmpty(); l = l.tail) {
            Type exc = attr.attribType(l.head, env);
            if (!exc.hasTag(TYPEVAR)) {
                exc = chk.checkClassType(l.head.pos(), exc);
            } else if (exc.tsym.owner == msym) {
                //mark inference variables in 'throws' clause
                exc.tsym.flags_field |= THROWS;
            }
            thrownbuf.append(exc);
        }
        MethodType mtype = new MethodType(argbuf.toList(),
                                    restype,
                                    thrownbuf.toList(),
                                    syms.methodClass);
        mtype.recvtype = recvtype;

        return tvars.isEmpty() ? mtype : new ForAll(tvars, mtype);
    }

/* ********************************************************************
 * Visitor methods for member enter
 *********************************************************************/

    /** Visitor argument: the current environment
     */
    protected Env<AttrContext> env;

    /** Enter field and method definitions and process import
     *  clauses, catching any completion failure exceptions.
     */
    protected void memberEnter(JCTree tree, Env<AttrContext> env) {
        Env<AttrContext> prevEnv = this.env;
        try {
            this.env = env;
            tree.accept(this);
        }  catch (CompletionFailure ex) {
            chk.completionError(tree.pos(), ex);
        } finally {
            this.env = prevEnv;
        }
    }

    /** Enter members from a list of trees.
     */
    void memberEnter(List<? extends JCTree> trees, Env<AttrContext> env) {
        for (List<? extends JCTree> l = trees; l.nonEmpty(); l = l.tail)
            memberEnter(l.head, env);
    }

    public void visitMethodDef(JCMethodDecl tree) {
        WriteableScope enclScope = enter.enterScope(env);
        MethodSymbol m = new MethodSymbol(0, tree.name, null, enclScope.owner);
        m.flags_field = chk.checkFlags(tree.pos(), tree.mods.flags, m, tree);
        tree.sym = m;

        //if this is a default method, add the DEFAULT flag to the enclosing interface
        if ((tree.mods.flags & DEFAULT) != 0) {
            m.owner.flags_field |= DEFAULT;
        }

        Env<AttrContext> localEnv = methodEnv(tree, env);
        DiagnosticPosition prevLintPos = deferredLintHandler.setPos(tree.pos());
        try {
            // Compute the method type
            m.type = signature(m, tree.typarams, tree.params,
                               tree.restype, tree.recvparam,
                               tree.thrown,
                               localEnv);
        } finally {
            deferredLintHandler.setPos(prevLintPos);
        }

        if (types.isSignaturePolymorphic(m)) {
            m.flags_field |= SIGNATURE_POLYMORPHIC;
        }

        // Set m.params
        ListBuffer<VarSymbol> params = new ListBuffer<>();
        JCVariableDecl lastParam = null;
        for (List<JCVariableDecl> l = tree.params; l.nonEmpty(); l = l.tail) {
            JCVariableDecl param = lastParam = l.head;
            params.append(Assert.checkNonNull(param.sym));
        }
        m.params = params.toList();

        // mark the method varargs, if necessary
        if (lastParam != null && (lastParam.mods.flags & Flags.VARARGS) != 0)
            m.flags_field |= Flags.VARARGS;

        localEnv.info.scope.leave();
        if (chk.checkUnique(tree.pos(), m, enclScope)) {
        enclScope.enter(m);
        }

        annotate.annotateLater(tree.mods.annotations, localEnv, m, tree.pos());
        // Visit the signature of the method. Note that
        // TypeAnnotate doesn't descend into the body.
        annotate.queueScanTreeAndTypeAnnotate(tree, localEnv, m, tree.pos());

        if (tree.defaultValue != null) {
            m.defaultValue = annotate.unfinishedDefaultValue(); // set it to temporary sentinel for now
            annotate.annotateDefaultValueLater(tree.defaultValue, localEnv, m, tree.pos());
        }
    }

    /** Create a fresh environment for method bodies.
     *  @param tree     The method definition.
     *  @param env      The environment current outside of the method definition.
     */
    Env<AttrContext> methodEnv(JCMethodDecl tree, Env<AttrContext> env) {
        Env<AttrContext> localEnv =
            env.dup(tree, env.info.dup(env.info.scope.dupUnshared(tree.sym)));
        localEnv.enclMethod = tree;
        if (tree.sym.type != null) {
            //when this is called in the enter stage, there's no type to be set
            localEnv.info.returnResult = attr.new ResultInfo(KindSelector.VAL,
                                                             tree.sym.type.getReturnType());
        }
        if ((tree.mods.flags & STATIC) != 0) localEnv.info.staticLevel++;
        localEnv.info.yieldResult = null;
        return localEnv;
    }

    public void visitVarDef(JCVariableDecl tree) {
        Env<AttrContext> localEnv = env;
        if ((tree.mods.flags & STATIC) != 0 ||
            (env.info.scope.owner.flags() & INTERFACE) != 0) {
            localEnv = env.dup(tree, env.info.dup());
            localEnv.info.staticLevel++;
        }
        DiagnosticPosition prevLintPos = deferredLintHandler.setPos(tree.pos());

        try {
            if (TreeInfo.isEnumInit(tree)) {
                attr.attribIdentAsEnumType(localEnv, (JCIdent)tree.vartype);
            } else if (!tree.isImplicitlyTyped()) {
                attr.attribType(tree.vartype, localEnv);
                if (TreeInfo.isReceiverParam(tree))
                    checkReceiver(tree, localEnv);
            }
        } finally {
            deferredLintHandler.setPos(prevLintPos);
        }

        if ((tree.mods.flags & VARARGS) != 0) {
            //if we are entering a varargs parameter, we need to
            //replace its type (a plain array type) with the more
            //precise VarargsType --- we need to do it this way
            //because varargs is represented in the tree as a
            //modifier on the parameter declaration, and not as a
            //distinct type of array node.
            ArrayType atype = (ArrayType)tree.vartype.type;
            tree.vartype.type = atype.makeVarargs();
        }
        WriteableScope enclScope = enter.enterScope(env);
        Type vartype = tree.isImplicitlyTyped()
                ? env.info.scope.owner.kind == MTH ? Type.noType : syms.errType
                : tree.vartype.type;
        VarSymbol v = new VarSymbol(0, tree.name, vartype, enclScope.owner);
        v.flags_field = chk.checkFlags(tree.pos(), tree.mods.flags, v, tree);
        tree.sym = v;
        if (tree.init != null) {
            v.flags_field |= HASINIT;
            if ((v.flags_field & FINAL) != 0 &&
                needsLazyConstValue(tree.init)) {
                Env<AttrContext> initEnv = getInitEnv(tree, env);
                initEnv.info.enclVar = v;
                v.setLazyConstValue(initEnv(tree, initEnv), attr, tree);
            }
        }
        if (chk.checkUnique(tree.pos(), v, enclScope)) {
            chk.checkTransparentVar(tree.pos(), v, enclScope);
            enclScope.enter(v);
        } else if (v.owner.kind == MTH || (v.flags_field & (Flags.PRIVATE | Flags.FINAL | Flags.GENERATED_MEMBER | Flags.RECORD)) != 0) {
            // if this is a parameter or a field obtained from a record component, enter it
            enclScope.enter(v);
        }

        annotate.annotateLater(tree.mods.annotations, localEnv, v, tree.pos());
        if (!tree.isImplicitlyTyped()) {
            annotate.queueScanTreeAndTypeAnnotate(tree.vartype, localEnv, v, tree.pos());
        }

        v.pos = tree.pos;
    }
    // where
    void checkType(JCTree tree, Type type, Error errorKey) {
        if (!tree.type.isErroneous() && !types.isSameType(tree.type, type)) {
            log.error(tree, errorKey);
        }
    }
    void checkReceiver(JCVariableDecl tree, Env<AttrContext> localEnv) {
        attr.attribExpr(tree.nameexpr, localEnv);
        MethodSymbol m = localEnv.enclMethod.sym;
        if (m.isConstructor()) {
            Type outertype = m.owner.owner.type;
            if (outertype.hasTag(TypeTag.METHOD)) {
                // we have a local inner class
                outertype = m.owner.owner.owner.type;
            }
            if (outertype.hasTag(TypeTag.CLASS)) {
                checkType(tree.vartype, outertype, Errors.IncorrectConstructorReceiverType(outertype, tree.vartype.type));
                checkType(tree.nameexpr, outertype, Errors.IncorrectConstructorReceiverName(outertype, tree.nameexpr.type));
            } else {
                log.error(tree, Errors.ReceiverParameterNotApplicableConstructorToplevelClass);
            }
        } else {
            checkType(tree.vartype, m.owner.type, Errors.IncorrectReceiverType(m.owner.type, tree.vartype.type));
            checkType(tree.nameexpr, m.owner.type, Errors.IncorrectReceiverName(m.owner.type, tree.nameexpr.type));
        }
    }

    public boolean needsLazyConstValue(JCTree tree) {
        InitTreeVisitor initTreeVisitor = new InitTreeVisitor();
        tree.accept(initTreeVisitor);
        return initTreeVisitor.result;
    }

    /** Visitor class for expressions which might be constant expressions,
     *  as per JLS 15.28 (Constant Expressions).
     */
    static class InitTreeVisitor extends JCTree.Visitor {

        private static final Set<Tag> ALLOWED_OPERATORS =
                EnumSet.of(Tag.POS, Tag.NEG, Tag.NOT, Tag.COMPL, Tag.PLUS, Tag.MINUS,
                           Tag.MUL, Tag.DIV, Tag.MOD, Tag.SL, Tag.SR, Tag.USR,
                           Tag.LT, Tag.LE, Tag.GT, Tag.GE, Tag.EQ, Tag.NE,
                           Tag.BITAND, Tag.BITXOR, Tag.BITOR, Tag.AND, Tag.OR);

        boolean result = true;

        @Override
        public void visitTree(JCTree tree) {
            result = false;
        }

        @Override
        public void visitLiteral(JCLiteral that) {}

        @Override
        public void visitTypeCast(JCTypeCast tree) {
            tree.expr.accept(this);
        }

        @Override
        public void visitUnary(JCUnary that) {
            if (!ALLOWED_OPERATORS.contains(that.getTag())) {
                result = false;
                return ;
            }
            that.arg.accept(this);
        }

        @Override
        public void visitBinary(JCBinary that) {
            if (!ALLOWED_OPERATORS.contains(that.getTag())) {
                result = false;
                return ;
            }
            that.lhs.accept(this);
            that.rhs.accept(this);
        }

        @Override
        public void visitConditional(JCConditional tree) {
            tree.cond.accept(this);
            tree.truepart.accept(this);
            tree.falsepart.accept(this);
        }

        @Override
        public void visitParens(JCParens tree) {
            tree.expr.accept(this);
        }

        @Override
        public void visitIdent(JCIdent that) {}

        @Override
        public void visitSelect(JCFieldAccess tree) {
            tree.selected.accept(this);
        }
    }

    /** Create a fresh environment for a variable's initializer.
     *  If the variable is a field, the owner of the environment's scope
     *  is be the variable itself, otherwise the owner is the method
     *  enclosing the variable definition.
     *
     *  @param tree     The variable definition.
     *  @param env      The environment current outside of the variable definition.
     */
    Env<AttrContext> initEnv(JCVariableDecl tree, Env<AttrContext> env) {
        Env<AttrContext> localEnv = env.dupto(new AttrContextEnv(tree, env.info.dup()));
        if (tree.sym.owner.kind == TYP) {
            localEnv.info.scope = env.info.scope.dupUnshared(tree.sym);
        }
        if ((tree.mods.flags & STATIC) != 0 ||
                ((env.enclClass.sym.flags() & INTERFACE) != 0 && env.enclMethod == null))
            localEnv.info.staticLevel++;
        return localEnv;
    }

    /** Default member enter visitor method: do nothing
     */
    public void visitTree(JCTree tree) {
    }

    public void visitErroneous(JCErroneous tree) {
        if (tree.errs != null)
            memberEnter(tree.errs, env);
    }

    public Env<AttrContext> getMethodEnv(JCMethodDecl tree, Env<AttrContext> env) {
        Env<AttrContext> mEnv = methodEnv(tree, env);
        mEnv.info.lint = mEnv.info.lint.augment(tree.sym);
        for (List<JCTypeParameter> l = tree.typarams; l.nonEmpty(); l = l.tail)
            mEnv.info.scope.enterIfAbsent(l.head.type.tsym);
        for (List<JCVariableDecl> l = tree.params; l.nonEmpty(); l = l.tail)
            mEnv.info.scope.enterIfAbsent(l.head.sym);
        return mEnv;
    }

    public Env<AttrContext> getInitEnv(JCVariableDecl tree, Env<AttrContext> env) {
        Env<AttrContext> iEnv = initEnv(tree, env);
        return iEnv;
    }
}
