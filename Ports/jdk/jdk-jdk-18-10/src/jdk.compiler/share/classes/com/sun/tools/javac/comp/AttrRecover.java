/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Type.ErrorType;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.comp.Attr.ResultInfo;
import com.sun.tools.javac.comp.DeferredAttr.AttrMode;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCErroneous;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCLambda;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCReturn;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.Tag;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Names;

/** This is an error recovery addon for Attr. Currently, it recovers
 *  method invocations with lambdas, that require type inference.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class AttrRecover {
    protected static final Context.Key<AttrRecover> attrRepairKey = new Context.Key<>();

    final Attr attr;
    final DeferredAttr deferredAttr;
    final Names names;
    final TreeMaker make;
    final Symtab syms;
    final Types types;

    public static AttrRecover instance(Context context) {
        AttrRecover instance = context.get(attrRepairKey);
        if (instance == null)
            instance = new AttrRecover(context);
        return instance;
    }

    protected AttrRecover(Context context) {
        context.put(attrRepairKey, this);

        attr = Attr.instance(context);
        deferredAttr = DeferredAttr.instance(context);
        names = Names.instance(context);
        make = TreeMaker.instance(context);
        syms = Symtab.instance(context);
        types = Types.instance(context);
    }

    private final ListBuffer<RecoverTodo> recoveryTodo = new ListBuffer<>();

    public void doRecovery() {
        while (recoveryTodo.nonEmpty()) {
            RecoverTodo todo = recoveryTodo.remove();
            ListBuffer<Runnable> rollback = new ListBuffer<>();
            boolean repaired = false;
            RECOVER: if (todo.env.tree.hasTag(Tag.APPLY)) {
                JCMethodInvocation mit = (JCMethodInvocation) todo.env.tree;
                boolean vararg = (todo.candSym.flags() & Flags.VARARGS) !=  0;
                if (!vararg &&
                    mit.args.length() > todo.candSym.type.getParameterTypes().length()) {
                    break RECOVER; //too many actual parameters, skip
                }
                List<JCExpression> args = mit.args;
                List<Type> formals = todo.candSym.type.getParameterTypes();
                while (args.nonEmpty() && formals.nonEmpty()) {
                    JCExpression arg = args.head;
                    Type formal = formals.tail.nonEmpty() || !vararg
                            ? formals.head : ((ArrayType) formals.head).elemtype;
                    if (arg.hasTag(JCTree.Tag.LAMBDA)) {
                        final JCTree.JCLambda lambda = (JCLambda) arg;
                        if (lambda.paramKind == JCLambda.ParameterKind.IMPLICIT) {
                            for (JCVariableDecl var : lambda.params) {
                                var.vartype = null; //reset type
                            }
                        }
                        if (types.isFunctionalInterface(formal)) {
                            Type functionalType = types.findDescriptorType(formal);
                            boolean voidCompatible = functionalType.getReturnType().hasTag(TypeTag.VOID);
                            lambda.body = new TreeTranslator() {
                                @Override
                                public void visitReturn(JCReturn tree) {
                                    result = tree;
                                    if (voidCompatible) {
                                        if (tree.expr != null) {
                                            JCErroneous err = make.Erroneous(List.of(tree));
                                            result = err;
                                            rollback.append(() -> {
                                                lambda.body = new TreeTranslator() {
                                                    @SuppressWarnings("unchecked")
                                                    public <T extends JCTree> T translate(T t) {
                                                        if (t == err) return (T) tree;
                                                        else return super.translate(t);
                                                    }
                                                }.translate(lambda.body);
                                            });
                                        }
                                    } else {
                                        if (tree.expr == null) {
                                            tree.expr = make.Erroneous().setType(syms.errType);
                                            rollback.append(() -> {
                                                tree.expr = null;
                                            });
                                        }
                                    }
                                }
                                @Override
                                public void visitLambda(JCLambda tree) {
                                    //do not touch nested lambdas
                                }
                                @Override
                                public void visitClassDef(JCClassDecl tree) {
                                    //do not touch nested classes
                                }
                            }.translate(lambda.body);
                            if (!voidCompatible) {
                                JCReturn ret = make.Return(make.Erroneous().setType(syms.errType));
                                ((JCBlock) lambda.body).stats = ((JCBlock) lambda.body).stats.append(ret);
                                rollback.append(() -> {
                                    ((JCBlock) lambda.body).stats = List.filter(((JCBlock) lambda.body).stats, ret);
                                });
                            }
                        }
                        repaired = true;
                    }
                    args = args.tail;
                    if (formals.tail.nonEmpty() || !vararg) {
                        formals = formals.tail;
                    }
                }
                List<JCExpression> prevArgs = mit.args;
                while (formals.nonEmpty()) {
                    mit.args = mit.args.append(make.Erroneous().setType(syms.errType));
                    formals = formals.tail;
                    repaired = true;
                }
                rollback.append(() -> {
                    mit.args = prevArgs;
                });
            }

            Type owntype;
            if (repaired) {
                List<JCExpression> args = TreeInfo.args(todo.env.tree);
                List<Type> pats = todo.resultInfo.pt.getParameterTypes();
                while (pats.length() < args.length()) {
                    pats = pats.append(syms.errType);
                }
                owntype = attr.checkMethod(todo.site, todo.candSym,
                                 attr.new ResultInfo(todo.resultInfo.pkind, todo.resultInfo.pt.getReturnType(), todo.resultInfo.checkContext, todo.resultInfo.checkMode),
                                 todo.env, args, pats,
                                 todo.resultInfo.pt.getTypeArguments());
                rollback.stream().forEach(Runnable::run);
            } else {
                owntype = basicMethodInvocationRecovery(todo.tree, todo.site, todo.errSym, todo.env, todo.resultInfo);
            }
            todo.tree.type = owntype;
        }
    }

    Type recoverMethodInvocation(JCTree tree,
                                 Type site,
                                 Symbol sym,
                                 Env<AttrContext> env,
                                 ResultInfo resultInfo) {
        if ((sym.flags_field & Flags.RECOVERABLE) != 0 && env.info.attributionMode.recover()) {
            recoveryTodo.append(new RecoverTodo(tree, site, sym, ((RecoveryErrorType) sym.type).candidateSymbol, attr.copyEnv(env), resultInfo));
            return syms.errType;
        } else {
            return basicMethodInvocationRecovery(tree, site, sym, env, resultInfo);
        }
    }

    private Type basicMethodInvocationRecovery(JCTree tree,
                                               Type site,
                                               Symbol sym,
                                               Env<AttrContext> env,
                                               ResultInfo resultInfo) {
        Type pt = resultInfo.pt.map(deferredAttr.new RecoveryDeferredTypeMap(AttrMode.SPECULATIVE, sym, env.info.pendingResolutionPhase));
        Type owntype = attr.checkIdInternal(tree, site, sym, pt, env, resultInfo);
        resultInfo.pt.map(deferredAttr.new RecoveryDeferredTypeMap(AttrMode.CHECK, sym, env.info.pendingResolutionPhase));
        return owntype;
    }

    void wrongMethodSymbolCandidate(TypeSymbol errSymbol, Symbol candSym, JCDiagnostic diag) {
        List<JCDiagnostic> diags = List.of(diag);
        boolean recoverable = false;
        while (!recoverable && diags.nonEmpty()) {
            JCDiagnostic d = diags.head;
            diags = diags.tail;
            switch (d.getCode()) {
                case "compiler.misc.missing.ret.val":
                case "compiler.misc.unexpected.ret.val":
                case "compiler.misc.infer.arg.length.mismatch":
                case "compiler.misc.arg.length.mismatch":
                    errSymbol.type = new RecoveryErrorType((Type.ErrorType) errSymbol.type, candSym);
                    errSymbol.flags_field |= Flags.RECOVERABLE;
                    return ;
                default:
                    break;
            }
            for (Object a : d.getArgs()) {
                if (a instanceof JCDiagnostic diagnostic) {
                    diags = diags.prepend(diagnostic);
                }
            }
        }
    }

    private static class RecoveryErrorType extends ErrorType {
        public final Symbol candidateSymbol;

        public RecoveryErrorType(ErrorType original, Symbol candidateSymbol) {
            super(original.getOriginalType(), original.tsym);
            this.candidateSymbol = candidateSymbol;
        }

    }

    private static class RecoverTodo {
        public final JCTree tree;
        public final Type site;
        public final Symbol errSym;
        public final Symbol candSym;
        public final Env<AttrContext> env;
        public final ResultInfo resultInfo;

        public RecoverTodo(JCTree tree, Type site, Symbol errSym, Symbol candSym,
                           Env<AttrContext> env, Attr.ResultInfo resultInfo) {
            this.tree = tree;
            this.site = site;
            this.errSym = errSym;
            this.candSym = candSym;
            this.env = env;
            this.resultInfo = resultInfo;
        }

    }
}
