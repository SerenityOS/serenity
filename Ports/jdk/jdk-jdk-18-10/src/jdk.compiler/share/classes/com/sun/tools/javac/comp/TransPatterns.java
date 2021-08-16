/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.source.tree.CaseTree;
import com.sun.tools.javac.code.BoundKind;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Kinds;
import com.sun.tools.javac.code.Kinds.Kind;
import com.sun.tools.javac.code.Preview;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.BindingSymbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.DynamicMethodSymbol;
import com.sun.tools.javac.code.Symbol.DynamicVarSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCConditional;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCForLoop;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCIf;
import com.sun.tools.javac.tree.JCTree.JCInstanceOf;
import com.sun.tools.javac.tree.JCTree.JCLabeledStatement;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCSwitch;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.JCBindingPattern;
import com.sun.tools.javac.tree.JCTree.JCWhileLoop;
import com.sun.tools.javac.tree.JCTree.Tag;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Options;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.code.Type.WildcardType;
import com.sun.tools.javac.code.TypeTag;
import static com.sun.tools.javac.code.TypeTag.BOT;
import com.sun.tools.javac.jvm.PoolConstant.LoadableConstant;
import com.sun.tools.javac.jvm.Target;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCBreak;
import com.sun.tools.javac.tree.JCTree.JCCase;
import com.sun.tools.javac.tree.JCTree.JCCaseLabel;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCContinue;
import com.sun.tools.javac.tree.JCTree.JCDoWhileLoop;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCGuardPattern;
import com.sun.tools.javac.tree.JCTree.JCLambda;
import com.sun.tools.javac.tree.JCTree.JCParenthesizedPattern;
import com.sun.tools.javac.tree.JCTree.JCPattern;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCSwitchExpression;
import com.sun.tools.javac.tree.JCTree.LetExpr;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.List;
import java.util.Iterator;

/**
 * This pass translates pattern-matching constructs, such as instanceof <pattern>.
 */
public class TransPatterns extends TreeTranslator {

    protected static final Context.Key<TransPatterns> transPatternsKey = new Context.Key<>();

    public static TransPatterns instance(Context context) {
        TransPatterns instance = context.get(transPatternsKey);
        if (instance == null)
            instance = new TransPatterns(context);
        return instance;
    }

    private final Symtab syms;
    private final Attr attr;
    private final Resolve rs;
    private final Types types;
    private final Operators operators;
    private final Names names;
    private final Target target;
    private final Preview preview;
    private TreeMaker make;
    private Env<AttrContext> env;

    BindingContext bindingContext = new BindingContext() {
        @Override
        VarSymbol bindingDeclared(BindingSymbol varSymbol) {
            return null;
        }

        @Override
        VarSymbol getBindingFor(BindingSymbol varSymbol) {
            return null;
        }

        @Override
        List<JCStatement> bindingVars(int diagPos) {
            return List.nil();
        }

        @Override
        JCStatement decorateStatement(JCStatement stat) {
            return stat;
        }

        @Override
        JCExpression decorateExpression(JCExpression expr) {
            return expr;
        }

        @Override
        BindingContext pop() {
            //do nothing
            return this;
        }

        @Override
        boolean tryPrepend(BindingSymbol binding, JCVariableDecl var) {
            return false;
        }
    };

    JCLabeledStatement pendingMatchLabel = null;

    boolean debugTransPatterns;

    private ClassSymbol currentClass = null;
    private MethodSymbol currentMethodSym = null;
    private VarSymbol currentValue = null;

    protected TransPatterns(Context context) {
        context.put(transPatternsKey, this);
        syms = Symtab.instance(context);
        attr = Attr.instance(context);
        rs = Resolve.instance(context);
        make = TreeMaker.instance(context);
        types = Types.instance(context);
        operators = Operators.instance(context);
        names = Names.instance(context);
        target = Target.instance(context);
        preview = Preview.instance(context);
        debugTransPatterns = Options.instance(context).isSet("debug.patterns");
    }

    @Override
    public void visitTypeTest(JCInstanceOf tree) {
        if (tree.pattern instanceof JCPattern) {
            //E instanceof $pattern
            //=>
            //(let T' N$temp = E; N$temp instanceof typeof($pattern) && <desugared $pattern>)
            //note the pattern desugaring performs binding variable assignments
            Type tempType = tree.expr.type.hasTag(BOT) ?
                    syms.objectType
                    : tree.expr.type;
            VarSymbol prevCurrentValue = currentValue;
            bindingContext = new BasicBindingContext();
            try {
                JCExpression translatedExpr = translate(tree.expr);
                Symbol exprSym = TreeInfo.symbol(translatedExpr);

                if (exprSym != null &&
                    exprSym.kind == Kind.VAR &&
                    exprSym.owner.kind.matches(Kinds.KindSelector.VAL_MTH)) {
                    currentValue = (VarSymbol) exprSym;
                } else {
                    currentValue = new VarSymbol(Flags.FINAL | Flags.SYNTHETIC,
                            names.fromString("patt" + tree.pos + target.syntheticNameChar() + "temp"),
                            tempType,
                            currentMethodSym);
                }

                Type principalType = principalType((JCPattern) tree.pattern);
                JCExpression resultExpression=
                        makeBinary(Tag.AND,
                                   makeTypeTest(make.Ident(currentValue), make.Type(principalType)),
                                   (JCExpression) this.<JCTree>translate(tree.pattern));
                if (currentValue != exprSym) {
                    resultExpression =
                            make.at(tree.pos).LetExpr(make.VarDef(currentValue, translatedExpr),
                                                      resultExpression).setType(syms.booleanType);
                    ((LetExpr) resultExpression).needsCond = true;
                }
                result = bindingContext.decorateExpression(resultExpression);
            } finally {
                currentValue = prevCurrentValue;
                bindingContext.pop();
            }
        } else {
            super.visitTypeTest(tree);
        }
    }

    @Override
    public void visitBindingPattern(JCBindingPattern tree) {
        //it is assumed the primary type has already been checked:
        BindingSymbol binding = (BindingSymbol) tree.var.sym;
        Type castTargetType = principalType(tree);
        VarSymbol bindingVar = bindingContext.bindingDeclared(binding);

        if (bindingVar != null) {
            JCAssign fakeInit = (JCAssign)make.at(TreeInfo.getStartPos(tree)).Assign(
                    make.Ident(bindingVar), convert(make.Ident(currentValue), castTargetType)).setType(bindingVar.erasure(types));
            LetExpr nestedLE = make.LetExpr(List.of(make.Exec(fakeInit)),
                                            make.Literal(true));
            nestedLE.needsCond = true;
            nestedLE.setType(syms.booleanType);
            result = nestedLE;
        } else {
            result = make.Literal(true);
        }
    }

    @Override
    public void visitParenthesizedPattern(JCParenthesizedPattern tree) {
        result = translate(tree.pattern);
    }

    @Override
    public void visitGuardPattern(JCGuardPattern tree) {
        JCExpression pattern = (JCExpression) this.<JCTree>translate(tree.patt);
        JCExpression guard = translate(tree.expr);
        result = makeBinary(Tag.AND, pattern, guard);
    }

    @Override
    public void visitSwitch(JCSwitch tree) {
        handleSwitch(tree, tree.selector, tree.cases, tree.hasTotalPattern, tree.patternSwitch);
    }

    @Override
    public void visitSwitchExpression(JCSwitchExpression tree) {
        handleSwitch(tree, tree.selector, tree.cases, tree.hasTotalPattern, tree.patternSwitch);
    }

    private void handleSwitch(JCTree tree,
                              JCExpression selector,
                              List<JCCase> cases,
                              boolean hasTotalPattern,
                              boolean patternSwitch) {
        Type seltype = selector.type;

        if (patternSwitch) {
            Assert.check(preview.isEnabled());
            Assert.check(preview.usesPreview(env.toplevel.sourcefile));

            //rewrite pattern matching switches:
            //switch ($obj) {
            //     case $constant: $stats$
            //     case $pattern1: $stats$
            //     case $pattern2, null: $stats$
            //     case $pattern3: $stats$
            //}
            //=>
            //int $idx = 0;
            //$RESTART: switch (invokeDynamic typeSwitch($constant, typeof($pattern1), typeof($pattern2), typeof($pattern3))($obj, $idx)) {
            //     case 0:
            //         if (!(<desugared $pattern1>)) { $idx = 1; continue $RESTART; }
            //         $stats$
            //     case 1:
            //         if (!(<desugared $pattern1>)) { $idx = 2; continue $RESTART; }
            //         $stats$
            //     case 2, -1:
            //         if (!(<desugared $pattern1>)) { $idx = 3; continue $RESTART; }
            //         $stats$
            //     case 3:
            //         if (!(<desugared $pattern1>)) { $idx = 4; continue $RESTART; }
            //         $stats$
            //}
            //notes:
            //-pattern desugaring performs assignment to the binding variables
            //-the selector is evaluated only once and stored in a temporary variable
            //-typeSwitch bootstrap method can restart matching at specified index. The bootstrap will
            // categorize the input, and return the case index whose type or constant matches the input.
            // The bootstrap does not evaluate guards, which are injected at the beginning of the case's
            // statement list, and if the guard fails, the switch is "continued" and matching is
            // restarted from the next index.
            //-case null is always desugared to case -1, as the typeSwitch bootstrap method will
            // return -1 when the input is null
            //
            //note the selector is evaluated only once and stored in a temporary variable
            ListBuffer<JCCase> newCases = new ListBuffer<>();
            for (List<JCCase> c = cases; c.nonEmpty(); c = c.tail) {
                if (c.head.stats.isEmpty() && c.tail.nonEmpty()) {
                    c.tail.head.labels = c.tail.head.labels.prependList(c.head.labels);
                } else {
                    newCases.add(c.head);
                }
            }
            cases = newCases.toList();
            ListBuffer<JCStatement> statements = new ListBuffer<>();
            VarSymbol temp = new VarSymbol(Flags.SYNTHETIC,
                    names.fromString("selector" + tree.pos + target.syntheticNameChar() + "temp"),
                    seltype,
                    currentMethodSym);
            boolean hasNullCase = cases.stream()
                                       .flatMap(c -> c.labels.stream())
                                       .anyMatch(p -> p.isExpression() &&
                                                      TreeInfo.isNull((JCExpression) p));

            JCCase lastCase = cases.last();

            if (hasTotalPattern && !hasNullCase) {
                JCCase last = lastCase;
                if (last.labels.stream().noneMatch(l -> l.hasTag(Tag.DEFAULTCASELABEL))) {
                    last.labels = last.labels.prepend(makeLit(syms.botType, null));
                    hasNullCase = true;
                }
            }
            selector = translate(selector);
            statements.append(make.at(tree.pos).VarDef(temp, !hasNullCase ? attr.makeNullCheck(selector)
                                                                          : selector));
            VarSymbol index = new VarSymbol(Flags.SYNTHETIC,
                    names.fromString(tree.pos + target.syntheticNameChar() + "index"),
                    syms.intType,
                    currentMethodSym);
            statements.append(make.at(tree.pos).VarDef(index, makeLit(syms.intType, 0)));

            List<Type> staticArgTypes = List.of(syms.methodHandleLookupType,
                                                syms.stringType,
                                                syms.methodTypeType,
                                                types.makeArrayType(new ClassType(syms.classType.getEnclosingType(),
                                                                    List.of(new WildcardType(syms.objectType, BoundKind.UNBOUND,
                                                                                             syms.boundClass)),
                                                                    syms.classType.tsym)));
            LoadableConstant[] staticArgValues =
                    cases.stream()
                         .flatMap(c -> c.labels.stream())
                         .map(l -> toLoadableConstant(l, seltype))
                         .filter(c -> c != null)
                         .toArray(s -> new LoadableConstant[s]);

            boolean enumSelector = seltype.tsym.isEnum();
            Name bootstrapName = enumSelector ? names.enumSwitch : names.typeSwitch;
            Symbol bsm = rs.resolveInternalMethod(tree.pos(), env, syms.switchBootstrapsType,
                    bootstrapName, staticArgTypes, List.nil());

            MethodType indyType = new MethodType(
                    List.of(enumSelector ? seltype : syms.objectType, syms.intType),
                    syms.intType,
                    List.nil(),
                    syms.methodClass
            );
            DynamicMethodSymbol dynSym = new DynamicMethodSymbol(bootstrapName,
                    syms.noSymbol,
                    ((MethodSymbol)bsm).asHandle(),
                    indyType,
                    staticArgValues);

            JCFieldAccess qualifier = make.Select(make.QualIdent(bsm.owner), dynSym.name);
            qualifier.sym = dynSym;
            qualifier.type = syms.intType;
            selector = make.Apply(List.nil(),
                                  qualifier,
                                  List.of(make.Ident(temp), make.Ident(index)))
                           .setType(syms.intType);

            int i = 0;
            boolean previousCompletesNormally = false;
            boolean hasDefault = false;

            for (var c : cases) {
                List<JCCaseLabel> clearedPatterns = c.labels;
                boolean hasJoinedNull =
                        c.labels.size() > 1 && c.labels.stream().anyMatch(l -> l.isNullPattern());
                if (hasJoinedNull) {
                    clearedPatterns = c.labels.stream()
                                              .filter(l -> !l.isNullPattern())
                                              .collect(List.collector());
                }
                if (clearedPatterns.size() == 1 && clearedPatterns.head.isPattern() && !previousCompletesNormally) {
                    JCCaseLabel p = clearedPatterns.head;
                    bindingContext = new BasicBindingContext();
                    VarSymbol prevCurrentValue = currentValue;
                    try {
                        currentValue = temp;
                        JCExpression test = (JCExpression) this.<JCTree>translate(p);
                        c.stats = translate(c.stats);
                        JCContinue continueSwitch = make.at(clearedPatterns.head.pos()).Continue(null);
                        continueSwitch.target = tree;
                        c.stats = c.stats.prepend(make.If(makeUnary(Tag.NOT, test).setType(syms.booleanType),
                                                           make.Block(0, List.of(make.Exec(make.Assign(make.Ident(index),
                                                                                                       makeLit(syms.intType, i + 1))
                                                                                     .setType(syms.intType)),
                                                                                 continueSwitch)),
                                                           null));
                        c.stats = c.stats.prependList(bindingContext.bindingVars(c.pos));
                    } finally {
                        currentValue = prevCurrentValue;
                        bindingContext.pop();
                    }
                } else {
                    c.stats = translate(c.stats);
                }
                ListBuffer<JCCaseLabel> translatedLabels = new ListBuffer<>();
                for (var p : c.labels) {
                    if (p.hasTag(Tag.DEFAULTCASELABEL)) {
                        translatedLabels.add(p);
                        hasDefault = true;
                    } else if (hasTotalPattern && !hasDefault &&
                               c == lastCase && p.isPattern()) {
                        //If the switch has total pattern, the last case will contain it.
                        //Convert the total pattern to default:
                        translatedLabels.add(make.DefaultCaseLabel());
                    } else {
                        int value;
                        if (p.isNullPattern()) {
                            value = -1;
                        } else {
                            value = i++;
                        }
                        translatedLabels.add(make.Literal(value));
                    }
                }
                c.labels = translatedLabels.toList();
                if (c.caseKind == CaseTree.CaseKind.STATEMENT) {
                    previousCompletesNormally = c.completesNormally;
                } else {
                    previousCompletesNormally = false;
                    JCBreak brk = make.at(TreeInfo.endPos(c.stats.last())).Break(null);
                    brk.target = tree;
                    c.stats = c.stats.append(brk);
                }
            }

            if (tree.hasTag(Tag.SWITCH)) {
                ((JCSwitch) tree).selector = selector;
                ((JCSwitch) tree).cases = cases;
                statements.append((JCSwitch) tree);
                result = make.Block(0, statements.toList());
            } else {
                ((JCSwitchExpression) tree).selector = selector;
                ((JCSwitchExpression) tree).cases = cases;
                LetExpr r = (LetExpr) make.LetExpr(statements.toList(), (JCSwitchExpression) tree)
                                          .setType(tree.type);

                r.needsCond = true;
                result = r;
            }
            return ;
        }
        if (tree.hasTag(Tag.SWITCH)) {
            super.visitSwitch((JCSwitch) tree);
        } else {
            super.visitSwitchExpression((JCSwitchExpression) tree);
        }
    }

    private Type principalType(JCPattern p) {
        return types.boxedTypeOrType(types.erasure(TreeInfo.primaryPatternType(p).type()));
    }

    private LoadableConstant toLoadableConstant(JCCaseLabel l, Type selector) {
        if (l.isPattern()) {
            Type principalType = principalType((JCPattern) l);
            if (types.isSubtype(selector, principalType)) {
                return (LoadableConstant) selector;
            } else {
                return (LoadableConstant) principalType;
            }
        } else if (l.isExpression() && !TreeInfo.isNull((JCExpression) l)) {
            if ((l.type.tsym.flags_field & Flags.ENUM) != 0) {
                return LoadableConstant.String(((JCIdent) l).name.toString());
            } else {
                Assert.checkNonNull(l.type.constValue());

                return switch (l.type.getTag()) {
                    case BYTE, CHAR,
                         SHORT, INT -> LoadableConstant.Int((Integer) l.type.constValue());
                    case CLASS -> LoadableConstant.String((String) l.type.constValue());
                    default -> throw new AssertionError();
                };
            }
        } else {
            return null;
        }
    }

    @Override
    public void visitBinary(JCBinary tree) {
        bindingContext = new BasicBindingContext();
        try {
            super.visitBinary(tree);
            result = bindingContext.decorateExpression(tree);
        } finally {
            bindingContext.pop();
        }
    }

    @Override
    public void visitConditional(JCConditional tree) {
        bindingContext = new BasicBindingContext();
        try {
            super.visitConditional(tree);
            result = bindingContext.decorateExpression(tree);
        } finally {
            bindingContext.pop();
        }
    }

    @Override
    public void visitIf(JCIf tree) {
        bindingContext = new BasicBindingContext();
        try {
            super.visitIf(tree);
            result = bindingContext.decorateStatement(tree);
        } finally {
            bindingContext.pop();
        }
    }

    @Override
    public void visitForLoop(JCForLoop tree) {
        bindingContext = new BasicBindingContext();
        try {
            super.visitForLoop(tree);
            result = bindingContext.decorateStatement(tree);
        } finally {
            bindingContext.pop();
        }
    }

    @Override
    public void visitWhileLoop(JCWhileLoop tree) {
        bindingContext = new BasicBindingContext();
        try {
            super.visitWhileLoop(tree);
            result = bindingContext.decorateStatement(tree);
        } finally {
            bindingContext.pop();
        }
    }

    @Override
    public void visitDoLoop(JCDoWhileLoop tree) {
        bindingContext = new BasicBindingContext();
        try {
            super.visitDoLoop(tree);
            result = bindingContext.decorateStatement(tree);
        } finally {
            bindingContext.pop();
        }
    }

    @Override
    public void visitMethodDef(JCMethodDecl tree) {
        MethodSymbol prevMethodSym = currentMethodSym;
        try {
            currentMethodSym = tree.sym;
            super.visitMethodDef(tree);
        } finally {
            currentMethodSym = prevMethodSym;
        }
    }

    @Override
    public void visitIdent(JCIdent tree) {
        VarSymbol bindingVar = null;
        if ((tree.sym.flags() & Flags.MATCH_BINDING) != 0) {
            bindingVar = bindingContext.getBindingFor((BindingSymbol)tree.sym);
        }
        if (bindingVar == null) {
            super.visitIdent(tree);
        } else {
            result = make.at(tree.pos).Ident(bindingVar);
        }
    }

    @Override
    public void visitBlock(JCBlock tree) {
        ListBuffer<JCStatement> statements = new ListBuffer<>();
        bindingContext = new BindingDeclarationFenceBindingContext() {
            boolean tryPrepend(BindingSymbol binding, JCVariableDecl var) {
                //{
                //    if (E instanceof T N) {
                //        return ;
                //    }
                //    //use of N:
                //}
                //=>
                //{
                //    T N;
                //    if ((let T' N$temp = E; N$temp instanceof T && (N = (T) N$temp == (T) N$temp))) {
                //        return ;
                //    }
                //    //use of N:
                //}
                hoistedVarMap.put(binding, var.sym);
                statements.append(var);
                return true;
            }
        };
        MethodSymbol oldMethodSym = currentMethodSym;
        try {
            if (currentMethodSym == null) {
                // Block is a static or instance initializer.
                currentMethodSym =
                    new MethodSymbol(tree.flags | Flags.BLOCK,
                                     names.empty, null,
                                     currentClass);
            }
            for (List<JCStatement> l = tree.stats; l.nonEmpty(); l = l.tail) {
                statements.append(translate(l.head));
            }

            tree.stats = statements.toList();
            result = tree;
        } finally {
            currentMethodSym = oldMethodSym;
            bindingContext.pop();
        }
    }

    @Override
    public void visitLambda(JCLambda tree) {
        BindingContext prevContent = bindingContext;
        try {
            bindingContext = new BindingDeclarationFenceBindingContext();
            super.visitLambda(tree);
        } finally {
            bindingContext = prevContent;
        }
    }

    @Override
    public void visitClassDef(JCClassDecl tree) {
        ClassSymbol prevCurrentClass = currentClass;
        try {
            currentClass = tree.sym;
            super.visitClassDef(tree);
        } finally {
            currentClass = prevCurrentClass;
        }
    }

    public void visitVarDef(JCVariableDecl tree) {
        MethodSymbol prevMethodSym = currentMethodSym;
        try {
            tree.mods = translate(tree.mods);
            tree.vartype = translate(tree.vartype);
            if (currentMethodSym == null) {
                // A class or instance field initializer.
                currentMethodSym =
                    new MethodSymbol((tree.mods.flags&Flags.STATIC) | Flags.BLOCK,
                                     names.empty, null,
                                     currentClass);
            }
            if (tree.init != null) tree.init = translate(tree.init);
            result = tree;
        } finally {
            currentMethodSym = prevMethodSym;
        }
    }

    public JCTree translateTopLevelClass(Env<AttrContext> env, JCTree cdef, TreeMaker make) {
        try {
            this.make = make;
            this.env = env;
            translate(cdef);
        } finally {
            // note that recursive invocations of this method fail hard
            this.make = null;
            this.env = null;
        }

        return cdef;
    }

    /** Make an instanceof expression.
     *  @param lhs      The expression.
     *  @param type     The type to be tested.
     */

    JCInstanceOf makeTypeTest(JCExpression lhs, JCExpression type) {
        JCInstanceOf tree = make.TypeTest(lhs, type);
        tree.type = syms.booleanType;
        return tree;
    }

    /** Make an attributed binary expression (copied from Lower).
     *  @param optag    The operators tree tag.
     *  @param lhs      The operator's left argument.
     *  @param rhs      The operator's right argument.
     */
    JCBinary makeBinary(JCTree.Tag optag, JCExpression lhs, JCExpression rhs) {
        JCBinary tree = make.Binary(optag, lhs, rhs);
        tree.operator = operators.resolveBinary(tree, optag, lhs.type, rhs.type);
        tree.type = tree.operator.type.getReturnType();
        return tree;
    }

    /** Make an attributed unary expression.
     *  @param optag    The operators tree tag.
     *  @param arg      The operator's argument.
     */
    JCTree.JCUnary makeUnary(JCTree.Tag optag, JCExpression arg) {
        JCTree.JCUnary tree = make.Unary(optag, arg);
        tree.operator = operators.resolveUnary(tree, optag, arg.type);
        tree.type = tree.operator.type.getReturnType();
        return tree;
    }

    JCExpression convert(JCExpression expr, Type target) {
        JCExpression result = make.at(expr.pos()).TypeCast(make.Type(target), expr);
        result.type = target;
        return result;
    }

    abstract class BindingContext {
        abstract VarSymbol bindingDeclared(BindingSymbol varSymbol);
        abstract VarSymbol getBindingFor(BindingSymbol varSymbol);
        abstract List<JCStatement> bindingVars(int diagPos);
        abstract JCStatement decorateStatement(JCStatement stat);
        abstract JCExpression decorateExpression(JCExpression expr);
        abstract BindingContext pop();
        abstract boolean tryPrepend(BindingSymbol binding, JCVariableDecl var);
    }

    class BasicBindingContext extends BindingContext {
        Map<BindingSymbol, VarSymbol> hoistedVarMap;
        BindingContext parent;

        public BasicBindingContext() {
            this.parent = bindingContext;
            this.hoistedVarMap = new LinkedHashMap<>();
        }

        @Override
        VarSymbol bindingDeclared(BindingSymbol varSymbol) {
            VarSymbol res = parent.bindingDeclared(varSymbol);
            if (res == null) {
                res = new VarSymbol(varSymbol.flags(), varSymbol.name, varSymbol.type, currentMethodSym);
                res.setTypeAttributes(varSymbol.getRawTypeAttributes());
                hoistedVarMap.put(varSymbol, res);
            }
            return res;
        }

        @Override
        VarSymbol getBindingFor(BindingSymbol varSymbol) {
            VarSymbol res = parent.getBindingFor(varSymbol);
            if (res != null) {
                return res;
            }
            return hoistedVarMap.entrySet().stream()
                    .filter(e -> e.getKey().isAliasFor(varSymbol))
                    .findFirst()
                    .map(e -> e.getValue()).orElse(null);
        }

        @Override
        List<JCStatement> bindingVars(int diagPos) {
            if (hoistedVarMap.isEmpty()) return List.nil();
            ListBuffer<JCStatement> stats = new ListBuffer<>();
            for (Entry<BindingSymbol, VarSymbol> e : hoistedVarMap.entrySet()) {
                JCVariableDecl decl = makeHoistedVarDecl(diagPos, e.getValue());
                if (!e.getKey().isPreserved() ||
                    !parent.tryPrepend(e.getKey(), decl)) {
                    stats.add(decl);
                }
            }
            return stats.toList();
        }

        @Override
        JCStatement decorateStatement(JCStatement stat) {
            //if (E instanceof T N) {
            //     //use N
            //}
            //=>
            //{
            //    T N;
            //    if ((let T' N$temp = E; N$temp instanceof T && (N = (T) N$temp == (T) N$temp))) {
            //        //use N
            //    }
            //}
            List<JCStatement> stats = bindingVars(stat.pos);
            if (stats.nonEmpty()) {
                stat = make.at(stat.pos).Block(0, stats.append(stat));
            }
            return stat;
        }

        @Override
        JCExpression decorateExpression(JCExpression expr) {
            //E instanceof T N && /*use of N*/
            //=>
            //(let T N; (let T' N$temp = E; N$temp instanceof T && (N = (T) N$temp == (T) N$temp)) && /*use of N*/)
            for (VarSymbol vsym : hoistedVarMap.values()) {
                expr = make.at(expr.pos).LetExpr(makeHoistedVarDecl(expr.pos, vsym), expr).setType(expr.type);
            }
            return expr;
        }

        @Override
        BindingContext pop() {
            return bindingContext = parent;
        }

        @Override
        boolean tryPrepend(BindingSymbol binding, JCVariableDecl var) {
            return false;
        }

        private JCVariableDecl makeHoistedVarDecl(int pos, VarSymbol varSymbol) {
            return make.at(pos).VarDef(varSymbol, null);
        }
    }

    private class BindingDeclarationFenceBindingContext extends BasicBindingContext {

        @Override
        VarSymbol bindingDeclared(BindingSymbol varSymbol) {
            return null;
        }

    }

    /** Make an attributed tree representing a literal. This will be an
     *  Ident node in the case of boolean literals, a Literal node in all
     *  other cases.
     *  @param type       The literal's type.
     *  @param value      The literal's value.
     */
    JCExpression makeLit(Type type, Object value) {
        return make.Literal(type.getTag(), value).setType(type.constType(value));
    }
}
