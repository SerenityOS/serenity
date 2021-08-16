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

package com.sun.tools.javac.comp;

import java.util.*;
import java.util.stream.Collectors;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Kinds.KindSelector;
import com.sun.tools.javac.code.Scope.WriteableScope;
import com.sun.tools.javac.jvm.*;
import com.sun.tools.javac.jvm.PoolConstant.LoadableConstant;
import com.sun.tools.javac.main.Option.PkgInfo;
import com.sun.tools.javac.resources.CompilerProperties.Fragments;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.List;

import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Symbol.OperatorSymbol.AccessCode;
import com.sun.tools.javac.resources.CompilerProperties.Errors;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.code.Type.*;

import com.sun.tools.javac.jvm.Target;
import com.sun.tools.javac.tree.EndPosTable;

import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.Flags.BLOCK;
import static com.sun.tools.javac.code.Scope.LookupKind.NON_RECURSIVE;
import static com.sun.tools.javac.code.TypeTag.*;
import static com.sun.tools.javac.code.Kinds.Kind.*;
import static com.sun.tools.javac.jvm.ByteCodes.*;
import com.sun.tools.javac.tree.JCTree.JCBreak;
import com.sun.tools.javac.tree.JCTree.JCCase;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import static com.sun.tools.javac.tree.JCTree.JCOperatorExpression.OperandPos.LEFT;
import com.sun.tools.javac.tree.JCTree.JCSwitchExpression;
import static com.sun.tools.javac.tree.JCTree.Tag.*;

/** This pass translates away some syntactic sugar: inner classes,
 *  class literals, assertions, foreach loops, etc.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Lower extends TreeTranslator {
    protected static final Context.Key<Lower> lowerKey = new Context.Key<>();

    public static Lower instance(Context context) {
        Lower instance = context.get(lowerKey);
        if (instance == null)
            instance = new Lower(context);
        return instance;
    }

    private final Names names;
    private final Log log;
    private final Symtab syms;
    private final Resolve rs;
    private final Operators operators;
    private final Check chk;
    private final Attr attr;
    private TreeMaker make;
    private DiagnosticPosition make_pos;
    private final ConstFold cfolder;
    private final Target target;
    private final TypeEnvs typeEnvs;
    private final Name dollarAssertionsDisabled;
    private final Types types;
    private final boolean debugLower;
    private final boolean disableProtectedAccessors; // experimental
    private final PkgInfo pkginfoOpt;

    protected Lower(Context context) {
        context.put(lowerKey, this);
        names = Names.instance(context);
        log = Log.instance(context);
        syms = Symtab.instance(context);
        rs = Resolve.instance(context);
        operators = Operators.instance(context);
        chk = Check.instance(context);
        attr = Attr.instance(context);
        make = TreeMaker.instance(context);
        cfolder = ConstFold.instance(context);
        target = Target.instance(context);
        typeEnvs = TypeEnvs.instance(context);
        dollarAssertionsDisabled = names.
            fromString(target.syntheticNameChar() + "assertionsDisabled");

        types = Types.instance(context);
        Options options = Options.instance(context);
        debugLower = options.isSet("debuglower");
        pkginfoOpt = PkgInfo.get(options);
        disableProtectedAccessors = options.isSet("disableProtectedAccessors");
    }

    /** The currently enclosing class.
     */
    ClassSymbol currentClass;

    /** A queue of all translated classes.
     */
    ListBuffer<JCTree> translated;

    /** Environment for symbol lookup, set by translateTopLevelClass.
     */
    Env<AttrContext> attrEnv;

    /** A hash table mapping syntax trees to their ending source positions.
     */
    EndPosTable endPosTable;

/**************************************************************************
 * Global mappings
 *************************************************************************/

    /** A hash table mapping local classes to their definitions.
     */
    Map<ClassSymbol, JCClassDecl> classdefs;

    /** A hash table mapping local classes to a list of pruned trees.
     */
    public Map<ClassSymbol, List<JCTree>> prunedTree = new WeakHashMap<>();

    /** A hash table mapping virtual accessed symbols in outer subclasses
     *  to the actually referred symbol in superclasses.
     */
    Map<Symbol,Symbol> actualSymbols;

    /** The current method definition.
     */
    JCMethodDecl currentMethodDef;

    /** The current method symbol.
     */
    MethodSymbol currentMethodSym;

    /** The currently enclosing outermost class definition.
     */
    JCClassDecl outermostClassDef;

    /** The currently enclosing outermost member definition.
     */
    JCTree outermostMemberDef;

    /** A map from local variable symbols to their translation (as per LambdaToMethod).
     * This is required when a capturing local class is created from a lambda (in which
     * case the captured symbols should be replaced with the translated lambda symbols).
     */
    Map<Symbol, Symbol> lambdaTranslationMap = null;

    /** A navigator class for assembling a mapping from local class symbols
     *  to class definition trees.
     *  There is only one case; all other cases simply traverse down the tree.
     */
    class ClassMap extends TreeScanner {

        /** All encountered class defs are entered into classdefs table.
         */
        public void visitClassDef(JCClassDecl tree) {
            classdefs.put(tree.sym, tree);
            super.visitClassDef(tree);
        }
    }
    ClassMap classMap = new ClassMap();

    /** Map a class symbol to its definition.
     *  @param c    The class symbol of which we want to determine the definition.
     */
    JCClassDecl classDef(ClassSymbol c) {
        // First lookup the class in the classdefs table.
        JCClassDecl def = classdefs.get(c);
        if (def == null && outermostMemberDef != null) {
            // If this fails, traverse outermost member definition, entering all
            // local classes into classdefs, and try again.
            classMap.scan(outermostMemberDef);
            def = classdefs.get(c);
        }
        if (def == null) {
            // If this fails, traverse outermost class definition, entering all
            // local classes into classdefs, and try again.
            classMap.scan(outermostClassDef);
            def = classdefs.get(c);
        }
        return def;
    }

    /** A hash table mapping class symbols to lists of free variables.
     *  accessed by them. Only free variables of the method immediately containing
     *  a class are associated with that class.
     */
    Map<ClassSymbol,List<VarSymbol>> freevarCache;

    /** A navigator class for collecting the free variables accessed
     *  from a local class. There is only one case; all other cases simply
     *  traverse down the tree. This class doesn't deal with the specific
     *  of Lower - it's an abstract visitor that is meant to be reused in
     *  order to share the local variable capture logic.
     */
    abstract class BasicFreeVarCollector extends TreeScanner {

        /** Add all free variables of class c to fvs list
         *  unless they are already there.
         */
        abstract void addFreeVars(ClassSymbol c);

        /** If tree refers to a variable in owner of local class, add it to
         *  free variables list.
         */
        public void visitIdent(JCIdent tree) {
            visitSymbol(tree.sym);
        }
        // where
        abstract void visitSymbol(Symbol _sym);

        /** If tree refers to a class instance creation expression
         *  add all free variables of the freshly created class.
         */
        public void visitNewClass(JCNewClass tree) {
            ClassSymbol c = (ClassSymbol)tree.constructor.owner;
            addFreeVars(c);
            super.visitNewClass(tree);
        }

        /** If tree refers to a superclass constructor call,
         *  add all free variables of the superclass.
         */
        public void visitApply(JCMethodInvocation tree) {
            if (TreeInfo.name(tree.meth) == names._super) {
                addFreeVars((ClassSymbol) TreeInfo.symbol(tree.meth).owner);
            }
            super.visitApply(tree);
        }

        @Override
        public void visitYield(JCYield tree) {
            scan(tree.value);
        }

    }

    /**
     * Lower-specific subclass of {@code BasicFreeVarCollector}.
     */
    class FreeVarCollector extends BasicFreeVarCollector {

        /** The owner of the local class.
         */
        Symbol owner;

        /** The local class.
         */
        ClassSymbol clazz;

        /** The list of owner's variables accessed from within the local class,
         *  without any duplicates.
         */
        List<VarSymbol> fvs;

        FreeVarCollector(ClassSymbol clazz) {
            this.clazz = clazz;
            this.owner = clazz.owner;
            this.fvs = List.nil();
        }

        /** Add free variable to fvs list unless it is already there.
         */
        private void addFreeVar(VarSymbol v) {
            for (List<VarSymbol> l = fvs; l.nonEmpty(); l = l.tail)
                if (l.head == v) return;
            fvs = fvs.prepend(v);
        }

        @Override
        void addFreeVars(ClassSymbol c) {
            List<VarSymbol> fvs = freevarCache.get(c);
            if (fvs != null) {
                for (List<VarSymbol> l = fvs; l.nonEmpty(); l = l.tail) {
                    addFreeVar(l.head);
                }
            }
        }

        @Override
        void visitSymbol(Symbol _sym) {
            Symbol sym = _sym;
            if (sym.kind == VAR || sym.kind == MTH) {
                if (sym != null && sym.owner != owner)
                    sym = proxies.get(sym);
                if (sym != null && sym.owner == owner) {
                    VarSymbol v = (VarSymbol)sym;
                    if (v.getConstValue() == null) {
                        addFreeVar(v);
                    }
                } else {
                    if (outerThisStack.head != null &&
                        outerThisStack.head != _sym)
                        visitSymbol(outerThisStack.head);
                }
            }
        }

        /** If tree refers to a class instance creation expression
         *  add all free variables of the freshly created class.
         */
        public void visitNewClass(JCNewClass tree) {
            ClassSymbol c = (ClassSymbol)tree.constructor.owner;
            if (tree.encl == null &&
                c.hasOuterInstance() &&
                outerThisStack.head != null)
                visitSymbol(outerThisStack.head);
            super.visitNewClass(tree);
        }

        /** If tree refers to a qualified this or super expression
         *  for anything but the current class, add the outer this
         *  stack as a free variable.
         */
        public void visitSelect(JCFieldAccess tree) {
            if ((tree.name == names._this || tree.name == names._super) &&
                tree.selected.type.tsym != clazz &&
                outerThisStack.head != null)
                visitSymbol(outerThisStack.head);
            super.visitSelect(tree);
        }

        /** If tree refers to a superclass constructor call,
         *  add all free variables of the superclass.
         */
        public void visitApply(JCMethodInvocation tree) {
            if (TreeInfo.name(tree.meth) == names._super) {
                Symbol constructor = TreeInfo.symbol(tree.meth);
                ClassSymbol c = (ClassSymbol)constructor.owner;
                if (c.hasOuterInstance() &&
                    !tree.meth.hasTag(SELECT) &&
                    outerThisStack.head != null)
                    visitSymbol(outerThisStack.head);
            }
            super.visitApply(tree);
        }

    }

    ClassSymbol ownerToCopyFreeVarsFrom(ClassSymbol c) {
        if (!c.isDirectlyOrIndirectlyLocal()) {
            return null;
        }
        Symbol currentOwner = c.owner;
        while (currentOwner.owner.kind.matches(KindSelector.TYP) && currentOwner.isDirectlyOrIndirectlyLocal()) {
            currentOwner = currentOwner.owner;
        }
        if (currentOwner.owner.kind.matches(KindSelector.VAL_MTH) && c.isSubClass(currentOwner, types)) {
            return (ClassSymbol)currentOwner;
        }
        return null;
    }

    /** Return the variables accessed from within a local class, which
     *  are declared in the local class' owner.
     *  (in reverse order of first access).
     */
    List<VarSymbol> freevars(ClassSymbol c)  {
        List<VarSymbol> fvs = freevarCache.get(c);
        if (fvs != null) {
            return fvs;
        }
        if (c.owner.kind.matches(KindSelector.VAL_MTH)) {
            FreeVarCollector collector = new FreeVarCollector(c);
            collector.scan(classDef(c));
            fvs = collector.fvs;
            freevarCache.put(c, fvs);
            return fvs;
        } else {
            ClassSymbol owner = ownerToCopyFreeVarsFrom(c);
            if (owner != null) {
                fvs = freevarCache.get(owner);
                freevarCache.put(c, fvs);
                return fvs;
            } else {
                return List.nil();
            }
        }
    }

    Map<TypeSymbol,EnumMapping> enumSwitchMap = new LinkedHashMap<>();

    EnumMapping mapForEnum(DiagnosticPosition pos, TypeSymbol enumClass) {
        EnumMapping map = enumSwitchMap.get(enumClass);
        if (map == null)
            enumSwitchMap.put(enumClass, map = new EnumMapping(pos, enumClass));
        return map;
    }

    /** This map gives a translation table to be used for enum
     *  switches.
     *
     *  <p>For each enum that appears as the type of a switch
     *  expression, we maintain an EnumMapping to assist in the
     *  translation, as exemplified by the following example:
     *
     *  <p>we translate
     *  <pre>
     *          switch(colorExpression) {
     *          case red: stmt1;
     *          case green: stmt2;
     *          }
     *  </pre>
     *  into
     *  <pre>
     *          switch(Outer$0.$EnumMap$Color[colorExpression.ordinal()]) {
     *          case 1: stmt1;
     *          case 2: stmt2
     *          }
     *  </pre>
     *  with the auxiliary table initialized as follows:
     *  <pre>
     *          class Outer$0 {
     *              synthetic final int[] $EnumMap$Color = new int[Color.values().length];
     *              static {
     *                  try { $EnumMap$Color[red.ordinal()] = 1; } catch (NoSuchFieldError ex) {}
     *                  try { $EnumMap$Color[green.ordinal()] = 2; } catch (NoSuchFieldError ex) {}
     *              }
     *          }
     *  </pre>
     *  class EnumMapping provides mapping data and support methods for this translation.
     */
    class EnumMapping {
        EnumMapping(DiagnosticPosition pos, TypeSymbol forEnum) {
            this.forEnum = forEnum;
            this.values = new LinkedHashMap<>();
            this.pos = pos;
            Name varName = names
                .fromString(target.syntheticNameChar() +
                            "SwitchMap" +
                            target.syntheticNameChar() +
                            names.fromUtf(ClassWriter.externalize(forEnum.type.tsym.flatName())).toString()
                            .replace('/', '.')
                            .replace('.', target.syntheticNameChar()));
            ClassSymbol outerCacheClass = outerCacheClass();
            this.mapVar = new VarSymbol(STATIC | SYNTHETIC | FINAL,
                                        varName,
                                        new ArrayType(syms.intType, syms.arrayClass),
                                        outerCacheClass);
            enterSynthetic(pos, mapVar, outerCacheClass.members());
        }

        DiagnosticPosition pos = null;

        // the next value to use
        int next = 1; // 0 (unused map elements) go to the default label

        // the enum for which this is a map
        final TypeSymbol forEnum;

        // the field containing the map
        final VarSymbol mapVar;

        // the mapped values
        final Map<VarSymbol,Integer> values;

        JCLiteral forConstant(VarSymbol v) {
            Integer result = values.get(v);
            if (result == null)
                values.put(v, result = next++);
            return make.Literal(result);
        }

        // generate the field initializer for the map
        void translate() {
            make.at(pos.getStartPosition());
            JCClassDecl owner = classDef((ClassSymbol)mapVar.owner);

            // synthetic static final int[] $SwitchMap$Color = new int[Color.values().length];
            MethodSymbol valuesMethod = lookupMethod(pos,
                                                     names.values,
                                                     forEnum.type,
                                                     List.nil());
            JCExpression size = make // Color.values().length
                .Select(make.App(make.QualIdent(valuesMethod)),
                        syms.lengthVar);
            JCExpression mapVarInit = make
                .NewArray(make.Type(syms.intType), List.of(size), null)
                .setType(new ArrayType(syms.intType, syms.arrayClass));

            // try { $SwitchMap$Color[red.ordinal()] = 1; } catch (java.lang.NoSuchFieldError ex) {}
            ListBuffer<JCStatement> stmts = new ListBuffer<>();
            Symbol ordinalMethod = lookupMethod(pos,
                                                names.ordinal,
                                                forEnum.type,
                                                List.nil());
            List<JCCatch> catcher = List.<JCCatch>nil()
                .prepend(make.Catch(make.VarDef(new VarSymbol(PARAMETER, names.ex,
                                                              syms.noSuchFieldErrorType,
                                                              syms.noSymbol),
                                                null),
                                    make.Block(0, List.nil())));
            for (Map.Entry<VarSymbol,Integer> e : values.entrySet()) {
                VarSymbol enumerator = e.getKey();
                Integer mappedValue = e.getValue();
                JCExpression assign = make
                    .Assign(make.Indexed(mapVar,
                                         make.App(make.Select(make.QualIdent(enumerator),
                                                              ordinalMethod))),
                            make.Literal(mappedValue))
                    .setType(syms.intType);
                JCStatement exec = make.Exec(assign);
                JCStatement _try = make.Try(make.Block(0, List.of(exec)), catcher, null);
                stmts.append(_try);
            }

            owner.defs = owner.defs
                .prepend(make.Block(STATIC, stmts.toList()))
                .prepend(make.VarDef(mapVar, mapVarInit));
        }
    }


/**************************************************************************
 * Tree building blocks
 *************************************************************************/

    /** Equivalent to make.at(pos.getStartPosition()) with side effect of caching
     *  pos as make_pos, for use in diagnostics.
     **/
    TreeMaker make_at(DiagnosticPosition pos) {
        make_pos = pos;
        return make.at(pos);
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

    /** Make an attributed tree representing null.
     */
    JCExpression makeNull() {
        return makeLit(syms.botType, null);
    }

    /** Make an attributed class instance creation expression.
     *  @param ctype    The class type.
     *  @param args     The constructor arguments.
     */
    JCNewClass makeNewClass(Type ctype, List<JCExpression> args) {
        JCNewClass tree = make.NewClass(null,
            null, make.QualIdent(ctype.tsym), args, null);
        tree.constructor = rs.resolveConstructor(
            make_pos, attrEnv, ctype, TreeInfo.types(args), List.nil());
        tree.type = ctype;
        return tree;
    }

    /** Make an attributed unary expression.
     *  @param optag    The operators tree tag.
     *  @param arg      The operator's argument.
     */
    JCUnary makeUnary(JCTree.Tag optag, JCExpression arg) {
        JCUnary tree = make.Unary(optag, arg);
        tree.operator = operators.resolveUnary(tree, optag, arg.type);
        tree.type = tree.operator.type.getReturnType();
        return tree;
    }

    /** Make an attributed binary expression.
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

    /** Make an attributed assignop expression.
     *  @param optag    The operators tree tag.
     *  @param lhs      The operator's left argument.
     *  @param rhs      The operator's right argument.
     */
    JCAssignOp makeAssignop(JCTree.Tag optag, JCTree lhs, JCTree rhs) {
        JCAssignOp tree = make.Assignop(optag, lhs, rhs);
        tree.operator = operators.resolveBinary(tree, tree.getTag().noAssignOp(), lhs.type, rhs.type);
        tree.type = lhs.type;
        return tree;
    }

    /** Convert tree into string object, unless it has already a
     *  reference type..
     */
    JCExpression makeString(JCExpression tree) {
        if (!tree.type.isPrimitiveOrVoid()) {
            return tree;
        } else {
            Symbol valueOfSym = lookupMethod(tree.pos(),
                                             names.valueOf,
                                             syms.stringType,
                                             List.of(tree.type));
            return make.App(make.QualIdent(valueOfSym), List.of(tree));
        }
    }

    /** Create an empty anonymous class definition and enter and complete
     *  its symbol. Return the class definition's symbol.
     *  and create
     *  @param flags    The class symbol's flags
     *  @param owner    The class symbol's owner
     */
    JCClassDecl makeEmptyClass(long flags, ClassSymbol owner) {
        return makeEmptyClass(flags, owner, null, true);
    }

    JCClassDecl makeEmptyClass(long flags, ClassSymbol owner, Name flatname,
            boolean addToDefs) {
        // Create class symbol.
        ClassSymbol c = syms.defineClass(names.empty, owner);
        if (flatname != null) {
            c.flatname = flatname;
        } else {
            c.flatname = chk.localClassName(c);
        }
        c.sourcefile = owner.sourcefile;
        c.completer = Completer.NULL_COMPLETER;
        c.members_field = WriteableScope.create(c);
        c.flags_field = flags;
        ClassType ctype = (ClassType) c.type;
        ctype.supertype_field = syms.objectType;
        ctype.interfaces_field = List.nil();

        JCClassDecl odef = classDef(owner);

        // Enter class symbol in owner scope and compiled table.
        enterSynthetic(odef.pos(), c, owner.members());
        chk.putCompiled(c);

        // Create class definition tree.
        JCClassDecl cdef = make.ClassDef(
            make.Modifiers(flags), names.empty,
            List.nil(),
            null, List.nil(), List.nil());
        cdef.sym = c;
        cdef.type = c.type;

        // Append class definition tree to owner's definitions.
        if (addToDefs) odef.defs = odef.defs.prepend(cdef);
        return cdef;
    }

/**************************************************************************
 * Symbol manipulation utilities
 *************************************************************************/

    /** Enter a synthetic symbol in a given scope, but complain if there was already one there.
     *  @param pos           Position for error reporting.
     *  @param sym           The symbol.
     *  @param s             The scope.
     */
    private void enterSynthetic(DiagnosticPosition pos, Symbol sym, WriteableScope s) {
        s.enter(sym);
    }

    /** Create a fresh synthetic name within a given scope - the unique name is
     *  obtained by appending '$' chars at the end of the name until no match
     *  is found.
     *
     * @param name base name
     * @param s scope in which the name has to be unique
     * @return fresh synthetic name
     */
    private Name makeSyntheticName(Name name, Scope s) {
        do {
            name = name.append(
                    target.syntheticNameChar(),
                    names.empty);
        } while (lookupSynthetic(name, s) != null);
        return name;
    }

    /** Check whether synthetic symbols generated during lowering conflict
     *  with user-defined symbols.
     *
     *  @param translatedTrees lowered class trees
     */
    void checkConflicts(List<JCTree> translatedTrees) {
        for (JCTree t : translatedTrees) {
            t.accept(conflictsChecker);
        }
    }

    JCTree.Visitor conflictsChecker = new TreeScanner() {

        TypeSymbol currentClass;

        @Override
        public void visitMethodDef(JCMethodDecl that) {
            checkConflicts(that.pos(), that.sym, currentClass);
            super.visitMethodDef(that);
        }

        @Override
        public void visitVarDef(JCVariableDecl that) {
            if (that.sym.owner.kind == TYP) {
                checkConflicts(that.pos(), that.sym, currentClass);
            }
            super.visitVarDef(that);
        }

        @Override
        public void visitClassDef(JCClassDecl that) {
            TypeSymbol prevCurrentClass = currentClass;
            currentClass = that.sym;
            try {
                super.visitClassDef(that);
            }
            finally {
                currentClass = prevCurrentClass;
            }
        }

        void checkConflicts(DiagnosticPosition pos, Symbol sym, TypeSymbol c) {
            for (Type ct = c.type; ct != Type.noType ; ct = types.supertype(ct)) {
                for (Symbol sym2 : ct.tsym.members().getSymbolsByName(sym.name, NON_RECURSIVE)) {
                    // VM allows methods and variables with differing types
                    if (sym.kind == sym2.kind &&
                        types.isSameType(types.erasure(sym.type), types.erasure(sym2.type)) &&
                        sym != sym2 &&
                        (sym.flags() & Flags.SYNTHETIC) != (sym2.flags() & Flags.SYNTHETIC) &&
                        (sym.flags() & BRIDGE) == 0 && (sym2.flags() & BRIDGE) == 0) {
                        syntheticError(pos, (sym2.flags() & SYNTHETIC) == 0 ? sym2 : sym);
                        return;
                    }
                }
            }
        }

        /** Report a conflict between a user symbol and a synthetic symbol.
         */
        private void syntheticError(DiagnosticPosition pos, Symbol sym) {
            if (!sym.type.isErroneous()) {
                log.error(pos, Errors.CannotGenerateClass(sym.location(), Fragments.SyntheticNameConflict(sym, sym.location())));
            }
        }
    };

    /** Look up a synthetic name in a given scope.
     *  @param s            The scope.
     *  @param name         The name.
     */
    private Symbol lookupSynthetic(Name name, Scope s) {
        Symbol sym = s.findFirst(name);
        return (sym==null || (sym.flags()&SYNTHETIC)==0) ? null : sym;
    }

    /** Look up a method in a given scope.
     */
    private MethodSymbol lookupMethod(DiagnosticPosition pos, Name name, Type qual, List<Type> args) {
        return rs.resolveInternalMethod(pos, attrEnv, qual, name, args, List.nil());
    }

    /** Anon inner classes are used as access constructor tags.
     * accessConstructorTag will use an existing anon class if one is available,
     * and synthesize a class (with makeEmptyClass) if one is not available.
     * However, there is a small possibility that an existing class will not
     * be generated as expected if it is inside a conditional with a constant
     * expression. If that is found to be the case, create an empty class tree here.
     */
    private void checkAccessConstructorTags() {
        for (List<ClassSymbol> l = accessConstrTags; l.nonEmpty(); l = l.tail) {
            ClassSymbol c = l.head;
            if (isTranslatedClassAvailable(c))
                continue;
            // Create class definition tree.
            JCClassDecl cdec = makeEmptyClass(STATIC | SYNTHETIC,
                    c.outermostClass(), c.flatname, false);
            swapAccessConstructorTag(c, cdec.sym);
            translated.append(cdec);
        }
    }
    // where
    private boolean isTranslatedClassAvailable(ClassSymbol c) {
        for (JCTree tree: translated) {
            if (tree.hasTag(CLASSDEF)
                    && ((JCClassDecl) tree).sym == c) {
                return true;
            }
        }
        return false;
    }

    void swapAccessConstructorTag(ClassSymbol oldCTag, ClassSymbol newCTag) {
        for (MethodSymbol methodSymbol : accessConstrs.values()) {
            Assert.check(methodSymbol.type.hasTag(METHOD));
            MethodType oldMethodType =
                    (MethodType)methodSymbol.type;
            if (oldMethodType.argtypes.head.tsym == oldCTag)
                methodSymbol.type =
                    types.createMethodTypeWithParameters(oldMethodType,
                        oldMethodType.getParameterTypes().tail
                            .prepend(newCTag.erasure(types)));
        }
    }

/**************************************************************************
 * Access methods
 *************************************************************************/

    /** A mapping from symbols to their access numbers.
     */
    private Map<Symbol,Integer> accessNums;

    /** A mapping from symbols to an array of access symbols, indexed by
     *  access code.
     */
    private Map<Symbol,MethodSymbol[]> accessSyms;

    /** A mapping from (constructor) symbols to access constructor symbols.
     */
    private Map<Symbol,MethodSymbol> accessConstrs;

    /** A list of all class symbols used for access constructor tags.
     */
    private List<ClassSymbol> accessConstrTags;

    /** A queue for all accessed symbols.
     */
    private ListBuffer<Symbol> accessed;

    /** return access code for identifier,
     *  @param tree     The tree representing the identifier use.
     *  @param enclOp   The closest enclosing operation node of tree,
     *                  null if tree is not a subtree of an operation.
     */
    private static int accessCode(JCTree tree, JCTree enclOp) {
        if (enclOp == null)
            return AccessCode.DEREF.code;
        else if (enclOp.hasTag(ASSIGN) &&
                 tree == TreeInfo.skipParens(((JCAssign) enclOp).lhs))
            return AccessCode.ASSIGN.code;
        else if ((enclOp.getTag().isIncOrDecUnaryOp() || enclOp.getTag().isAssignop()) &&
                tree == TreeInfo.skipParens(((JCOperatorExpression) enclOp).getOperand(LEFT)))
            return (((JCOperatorExpression) enclOp).operator).getAccessCode(enclOp.getTag());
        else
            return AccessCode.DEREF.code;
    }

    /** Return binary operator that corresponds to given access code.
     */
    private OperatorSymbol binaryAccessOperator(int acode, Tag tag) {
        return operators.lookupBinaryOp(op -> op.getAccessCode(tag) == acode);
    }

    /** Return tree tag for assignment operation corresponding
     *  to given binary operator.
     */
    private static JCTree.Tag treeTag(OperatorSymbol operator) {
        switch (operator.opcode) {
        case ByteCodes.ior: case ByteCodes.lor:
            return BITOR_ASG;
        case ByteCodes.ixor: case ByteCodes.lxor:
            return BITXOR_ASG;
        case ByteCodes.iand: case ByteCodes.land:
            return BITAND_ASG;
        case ByteCodes.ishl: case ByteCodes.lshl:
        case ByteCodes.ishll: case ByteCodes.lshll:
            return SL_ASG;
        case ByteCodes.ishr: case ByteCodes.lshr:
        case ByteCodes.ishrl: case ByteCodes.lshrl:
            return SR_ASG;
        case ByteCodes.iushr: case ByteCodes.lushr:
        case ByteCodes.iushrl: case ByteCodes.lushrl:
            return USR_ASG;
        case ByteCodes.iadd: case ByteCodes.ladd:
        case ByteCodes.fadd: case ByteCodes.dadd:
        case ByteCodes.string_add:
            return PLUS_ASG;
        case ByteCodes.isub: case ByteCodes.lsub:
        case ByteCodes.fsub: case ByteCodes.dsub:
            return MINUS_ASG;
        case ByteCodes.imul: case ByteCodes.lmul:
        case ByteCodes.fmul: case ByteCodes.dmul:
            return MUL_ASG;
        case ByteCodes.idiv: case ByteCodes.ldiv:
        case ByteCodes.fdiv: case ByteCodes.ddiv:
            return DIV_ASG;
        case ByteCodes.imod: case ByteCodes.lmod:
        case ByteCodes.fmod: case ByteCodes.dmod:
            return MOD_ASG;
        default:
            throw new AssertionError();
        }
    }

    /** The name of the access method with number `anum' and access code `acode'.
     */
    Name accessName(int anum, int acode) {
        return names.fromString(
            "access" + target.syntheticNameChar() + anum + acode / 10 + acode % 10);
    }

    /** Return access symbol for a private or protected symbol from an inner class.
     *  @param sym        The accessed private symbol.
     *  @param tree       The accessing tree.
     *  @param enclOp     The closest enclosing operation node of tree,
     *                    null if tree is not a subtree of an operation.
     *  @param protAccess Is access to a protected symbol in another
     *                    package?
     *  @param refSuper   Is access via a (qualified) C.super?
     */
    MethodSymbol accessSymbol(Symbol sym, JCTree tree, JCTree enclOp,
                              boolean protAccess, boolean refSuper) {
        ClassSymbol accOwner = refSuper && protAccess
            // For access via qualified super (T.super.x), place the
            // access symbol on T.
            ? (ClassSymbol)((JCFieldAccess) tree).selected.type.tsym
            // Otherwise pretend that the owner of an accessed
            // protected symbol is the enclosing class of the current
            // class which is a subclass of the symbol's owner.
            : accessClass(sym, protAccess, tree);

        Symbol vsym = sym;
        if (sym.owner != accOwner) {
            vsym = sym.clone(accOwner);
            actualSymbols.put(vsym, sym);
        }

        Integer anum              // The access number of the access method.
            = accessNums.get(vsym);
        if (anum == null) {
            anum = accessed.length();
            accessNums.put(vsym, anum);
            accessSyms.put(vsym, new MethodSymbol[AccessCode.numberOfAccessCodes]);
            accessed.append(vsym);
            // System.out.println("accessing " + vsym + " in " + vsym.location());
        }

        int acode;                // The access code of the access method.
        List<Type> argtypes;      // The argument types of the access method.
        Type restype;             // The result type of the access method.
        List<Type> thrown;        // The thrown exceptions of the access method.
        switch (vsym.kind) {
        case VAR:
            acode = accessCode(tree, enclOp);
            if (acode >= AccessCode.FIRSTASGOP.code) {
                OperatorSymbol operator = binaryAccessOperator(acode, enclOp.getTag());
                if (operator.opcode == string_add)
                    argtypes = List.of(syms.objectType);
                else
                    argtypes = operator.type.getParameterTypes().tail;
            } else if (acode == AccessCode.ASSIGN.code)
                argtypes = List.of(vsym.erasure(types));
            else
                argtypes = List.nil();
            restype = vsym.erasure(types);
            thrown = List.nil();
            break;
        case MTH:
            acode = AccessCode.DEREF.code;
            argtypes = vsym.erasure(types).getParameterTypes();
            restype = vsym.erasure(types).getReturnType();
            thrown = vsym.type.getThrownTypes();
            break;
        default:
            throw new AssertionError();
        }

        // For references via qualified super, increment acode by one,
        // making it odd.
        if (protAccess && refSuper) acode++;

        // Instance access methods get instance as first parameter.
        // For protected symbols this needs to be the instance as a member
        // of the type containing the accessed symbol, not the class
        // containing the access method.
        if ((vsym.flags() & STATIC) == 0) {
            argtypes = argtypes.prepend(vsym.owner.erasure(types));
        }
        MethodSymbol[] accessors = accessSyms.get(vsym);
        MethodSymbol accessor = accessors[acode];
        if (accessor == null) {
            accessor = new MethodSymbol(
                STATIC | SYNTHETIC | (accOwner.isInterface() ? PUBLIC : 0),
                accessName(anum.intValue(), acode),
                new MethodType(argtypes, restype, thrown, syms.methodClass),
                accOwner);
            enterSynthetic(tree.pos(), accessor, accOwner.members());
            accessors[acode] = accessor;
        }
        return accessor;
    }

    /** The qualifier to be used for accessing a symbol in an outer class.
     *  This is either C.sym or C.this.sym, depending on whether or not
     *  sym is static.
     *  @param sym   The accessed symbol.
     */
    JCExpression accessBase(DiagnosticPosition pos, Symbol sym) {
        return (sym.flags() & STATIC) != 0
            ? access(make.at(pos.getStartPosition()).QualIdent(sym.owner))
            : makeOwnerThis(pos, sym, true);
    }

    /** Do we need an access method to reference private symbol?
     */
    boolean needsPrivateAccess(Symbol sym) {
        if (target.hasNestmateAccess()) {
            return false;
        }
        if ((sym.flags() & PRIVATE) == 0 || sym.owner == currentClass) {
            return false;
        } else if (sym.name == names.init && sym.owner.isDirectlyOrIndirectlyLocal()) {
            // private constructor in local class: relax protection
            sym.flags_field &= ~PRIVATE;
            return false;
        } else {
            return true;
        }
    }

    /** Do we need an access method to reference symbol in other package?
     */
    boolean needsProtectedAccess(Symbol sym, JCTree tree) {
        if (disableProtectedAccessors) return false;
        if ((sym.flags() & PROTECTED) == 0 ||
            sym.owner.owner == currentClass.owner || // fast special case
            sym.packge() == currentClass.packge())
            return false;
        if (!currentClass.isSubClass(sym.owner, types))
            return true;
        if ((sym.flags() & STATIC) != 0 ||
            !tree.hasTag(SELECT) ||
            TreeInfo.name(((JCFieldAccess) tree).selected) == names._super)
            return false;
        return !((JCFieldAccess) tree).selected.type.tsym.isSubClass(currentClass, types);
    }

    /** The class in which an access method for given symbol goes.
     *  @param sym        The access symbol
     *  @param protAccess Is access to a protected symbol in another
     *                    package?
     */
    ClassSymbol accessClass(Symbol sym, boolean protAccess, JCTree tree) {
        if (protAccess) {
            Symbol qualifier = null;
            ClassSymbol c = currentClass;
            if (tree.hasTag(SELECT) && (sym.flags() & STATIC) == 0) {
                qualifier = ((JCFieldAccess) tree).selected.type.tsym;
                while (!qualifier.isSubClass(c, types)) {
                    c = c.owner.enclClass();
                }
                return c;
            } else {
                while (!c.isSubClass(sym.owner, types)) {
                    c = c.owner.enclClass();
                }
            }
            return c;
        } else {
            // the symbol is private
            return sym.owner.enclClass();
        }
    }

    private void addPrunedInfo(JCTree tree) {
        List<JCTree> infoList = prunedTree.get(currentClass);
        infoList = (infoList == null) ? List.of(tree) : infoList.prepend(tree);
        prunedTree.put(currentClass, infoList);
    }

    /** Ensure that identifier is accessible, return tree accessing the identifier.
     *  @param sym      The accessed symbol.
     *  @param tree     The tree referring to the symbol.
     *  @param enclOp   The closest enclosing operation node of tree,
     *                  null if tree is not a subtree of an operation.
     *  @param refSuper Is access via a (qualified) C.super?
     */
    JCExpression access(Symbol sym, JCExpression tree, JCExpression enclOp, boolean refSuper) {
        // Access a free variable via its proxy, or its proxy's proxy
        while (sym.kind == VAR && sym.owner.kind == MTH &&
            sym.owner.enclClass() != currentClass) {
            // A constant is replaced by its constant value.
            Object cv = ((VarSymbol)sym).getConstValue();
            if (cv != null) {
                make.at(tree.pos);
                return makeLit(sym.type, cv);
            }
            if (lambdaTranslationMap != null && lambdaTranslationMap.get(sym) != null) {
                return make.at(tree.pos).Ident(lambdaTranslationMap.get(sym));
            } else {
                // Otherwise replace the variable by its proxy.
                sym = proxies.get(sym);
                Assert.check(sym != null && (sym.flags_field & FINAL) != 0);
                tree = make.at(tree.pos).Ident(sym);
            }
        }
        JCExpression base = (tree.hasTag(SELECT)) ? ((JCFieldAccess) tree).selected : null;
        switch (sym.kind) {
        case TYP:
            if (sym.owner.kind != PCK) {
                // Convert type idents to
                // <flat name> or <package name> . <flat name>
                Name flatname = Convert.shortName(sym.flatName());
                while (base != null &&
                       TreeInfo.symbol(base) != null &&
                       TreeInfo.symbol(base).kind != PCK) {
                    base = (base.hasTag(SELECT))
                        ? ((JCFieldAccess) base).selected
                        : null;
                }
                if (tree.hasTag(IDENT)) {
                    ((JCIdent) tree).name = flatname;
                } else if (base == null) {
                    tree = make.at(tree.pos).Ident(sym);
                    ((JCIdent) tree).name = flatname;
                } else {
                    ((JCFieldAccess) tree).selected = base;
                    ((JCFieldAccess) tree).name = flatname;
                }
            }
            break;
        case MTH: case VAR:
            if (sym.owner.kind == TYP) {

                // Access methods are required for
                //  - private members,
                //  - protected members in a superclass of an
                //    enclosing class contained in another package.
                //  - all non-private members accessed via a qualified super.
                boolean protAccess = refSuper && !needsPrivateAccess(sym)
                    || needsProtectedAccess(sym, tree);
                boolean accReq = protAccess || needsPrivateAccess(sym);

                // A base has to be supplied for
                //  - simple identifiers accessing variables in outer classes.
                boolean baseReq =
                    base == null &&
                    sym.owner != syms.predefClass &&
                    !sym.isMemberOf(currentClass, types);

                if (accReq || baseReq) {
                    make.at(tree.pos);

                    // Constants are replaced by their constant value.
                    if (sym.kind == VAR) {
                        Object cv = ((VarSymbol)sym).getConstValue();
                        if (cv != null) {
                            addPrunedInfo(tree);
                            return makeLit(sym.type, cv);
                        }
                    }

                    // Private variables and methods are replaced by calls
                    // to their access methods.
                    if (accReq) {
                        List<JCExpression> args = List.nil();
                        if ((sym.flags() & STATIC) == 0) {
                            // Instance access methods get instance
                            // as first parameter.
                            if (base == null)
                                base = makeOwnerThis(tree.pos(), sym, true);
                            args = args.prepend(base);
                            base = null;   // so we don't duplicate code
                        }
                        Symbol access = accessSymbol(sym, tree,
                                                     enclOp, protAccess,
                                                     refSuper);
                        JCExpression receiver = make.Select(
                            base != null ? base : make.QualIdent(access.owner),
                            access);
                        return make.App(receiver, args);

                    // Other accesses to members of outer classes get a
                    // qualifier.
                    } else if (baseReq) {
                        return make.at(tree.pos).Select(
                            accessBase(tree.pos(), sym), sym).setType(tree.type);
                    }
                }
            } else if (sym.owner.kind == MTH && lambdaTranslationMap != null) {
                //sym is a local variable - check the lambda translation map to
                //see if sym has been translated to something else in the current
                //scope (by LambdaToMethod)
                Symbol translatedSym = lambdaTranslationMap.get(sym.baseSymbol());
                if (translatedSym != null) {
                    tree = make.at(tree.pos).Ident(translatedSym);
                }
            }
        }
        return tree;
    }

    /** Ensure that identifier is accessible, return tree accessing the identifier.
     *  @param tree     The identifier tree.
     */
    JCExpression access(JCExpression tree) {
        Symbol sym = TreeInfo.symbol(tree);
        return sym == null ? tree : access(sym, tree, null, false);
    }

    /** Return access constructor for a private constructor,
     *  or the constructor itself, if no access constructor is needed.
     *  @param pos       The position to report diagnostics, if any.
     *  @param constr    The private constructor.
     */
    Symbol accessConstructor(DiagnosticPosition pos, Symbol constr) {
        if (needsPrivateAccess(constr)) {
            ClassSymbol accOwner = constr.owner.enclClass();
            MethodSymbol aconstr = accessConstrs.get(constr);
            if (aconstr == null) {
                List<Type> argtypes = constr.type.getParameterTypes();
                if ((accOwner.flags_field & ENUM) != 0)
                    argtypes = argtypes
                        .prepend(syms.intType)
                        .prepend(syms.stringType);
                aconstr = new MethodSymbol(
                    SYNTHETIC,
                    names.init,
                    new MethodType(
                        argtypes.append(
                            accessConstructorTag().erasure(types)),
                        constr.type.getReturnType(),
                        constr.type.getThrownTypes(),
                        syms.methodClass),
                    accOwner);
                enterSynthetic(pos, aconstr, accOwner.members());
                accessConstrs.put(constr, aconstr);
                accessed.append(constr);
            }
            return aconstr;
        } else {
            return constr;
        }
    }

    /** Return an anonymous class nested in this toplevel class.
     */
    ClassSymbol accessConstructorTag() {
        ClassSymbol topClass = currentClass.outermostClass();
        ModuleSymbol topModle = topClass.packge().modle;
        for (int i = 1; ; i++) {
            Name flatname = names.fromString("" + topClass.getQualifiedName() +
                                            target.syntheticNameChar() +
                                            i);
            ClassSymbol ctag = chk.getCompiled(topModle, flatname);
            if (ctag == null)
                ctag = makeEmptyClass(STATIC | SYNTHETIC, topClass).sym;
            else if (!ctag.isAnonymous())
                continue;
            // keep a record of all tags, to verify that all are generated as required
            accessConstrTags = accessConstrTags.prepend(ctag);
            return ctag;
        }
    }

    /** Add all required access methods for a private symbol to enclosing class.
     *  @param sym       The symbol.
     */
    void makeAccessible(Symbol sym) {
        JCClassDecl cdef = classDef(sym.owner.enclClass());
        if (cdef == null) Assert.error("class def not found: " + sym + " in " + sym.owner);
        if (sym.name == names.init) {
            cdef.defs = cdef.defs.prepend(
                accessConstructorDef(cdef.pos, sym, accessConstrs.get(sym)));
        } else {
            MethodSymbol[] accessors = accessSyms.get(sym);
            for (int i = 0; i < AccessCode.numberOfAccessCodes; i++) {
                if (accessors[i] != null)
                    cdef.defs = cdef.defs.prepend(
                        accessDef(cdef.pos, sym, accessors[i], i));
            }
        }
    }

    /** Construct definition of an access method.
     *  @param pos        The source code position of the definition.
     *  @param vsym       The private or protected symbol.
     *  @param accessor   The access method for the symbol.
     *  @param acode      The access code.
     */
    JCTree accessDef(int pos, Symbol vsym, MethodSymbol accessor, int acode) {
//      System.err.println("access " + vsym + " with " + accessor);//DEBUG
        currentClass = vsym.owner.enclClass();
        make.at(pos);
        JCMethodDecl md = make.MethodDef(accessor, null);

        // Find actual symbol
        Symbol sym = actualSymbols.get(vsym);
        if (sym == null) sym = vsym;

        JCExpression ref;           // The tree referencing the private symbol.
        List<JCExpression> args;    // Any additional arguments to be passed along.
        if ((sym.flags() & STATIC) != 0) {
            ref = make.Ident(sym);
            args = make.Idents(md.params);
        } else {
            JCExpression site = make.Ident(md.params.head);
            if (acode % 2 != 0) {
                //odd access codes represent qualified super accesses - need to
                //emit reference to the direct superclass, even if the referred
                //member is from an indirect superclass (JLS 13.1)
                site.setType(types.erasure(types.supertype(vsym.owner.enclClass().type)));
            }
            ref = make.Select(site, sym);
            args = make.Idents(md.params.tail);
        }
        JCStatement stat;          // The statement accessing the private symbol.
        if (sym.kind == VAR) {
            // Normalize out all odd access codes by taking floor modulo 2:
            int acode1 = acode - (acode & 1);

            JCExpression expr;      // The access method's return value.
            AccessCode aCode = AccessCode.getFromCode(acode1);
            switch (aCode) {
            case DEREF:
                expr = ref;
                break;
            case ASSIGN:
                expr = make.Assign(ref, args.head);
                break;
            case PREINC: case POSTINC: case PREDEC: case POSTDEC:
                expr = makeUnary(aCode.tag, ref);
                break;
            default:
                expr = make.Assignop(
                    treeTag(binaryAccessOperator(acode1, JCTree.Tag.NO_TAG)), ref, args.head);
                ((JCAssignOp) expr).operator = binaryAccessOperator(acode1, JCTree.Tag.NO_TAG);
            }
            stat = make.Return(expr.setType(sym.type));
        } else {
            stat = make.Call(make.App(ref, args));
        }
        md.body = make.Block(0, List.of(stat));

        // Make sure all parameters, result types and thrown exceptions
        // are accessible.
        for (List<JCVariableDecl> l = md.params; l.nonEmpty(); l = l.tail)
            l.head.vartype = access(l.head.vartype);
        md.restype = access(md.restype);
        for (List<JCExpression> l = md.thrown; l.nonEmpty(); l = l.tail)
            l.head = access(l.head);

        return md;
    }

    /** Construct definition of an access constructor.
     *  @param pos        The source code position of the definition.
     *  @param constr     The private constructor.
     *  @param accessor   The access method for the constructor.
     */
    JCTree accessConstructorDef(int pos, Symbol constr, MethodSymbol accessor) {
        make.at(pos);
        JCMethodDecl md = make.MethodDef(accessor,
                                      accessor.externalType(types),
                                      null);
        JCIdent callee = make.Ident(names._this);
        callee.sym = constr;
        callee.type = constr.type;
        md.body =
            make.Block(0, List.of(
                make.Call(
                    make.App(
                        callee,
                        make.Idents(md.params.reverse().tail.reverse())))));
        return md;
    }

/**************************************************************************
 * Free variables proxies and this$n
 *************************************************************************/

    /** A map which allows to retrieve the translated proxy variable for any given symbol of an
     *  enclosing scope that is accessed (the accessed symbol could be the synthetic 'this$n' symbol).
     *  Inside a constructor, the map temporarily overrides entries corresponding to proxies and any
     *  'this$n' symbols, where they represent the constructor parameters.
     */
    Map<Symbol, Symbol> proxies;

    /** A scope containing all unnamed resource variables/saved
     *  exception variables for translated TWR blocks
     */
    WriteableScope twrVars;

    /** A stack containing the this$n field of the currently translated
     *  classes (if needed) in innermost first order.
     *  Inside a constructor, proxies and any this$n symbol are duplicated
     *  in an additional innermost scope, where they represent the constructor
     *  parameters.
     */
    List<VarSymbol> outerThisStack;

    /** The name of a free variable proxy.
     */
    Name proxyName(Name name, int index) {
        Name proxyName = names.fromString("val" + target.syntheticNameChar() + name);
        if (index > 0) {
            proxyName = proxyName.append(names.fromString("" + target.syntheticNameChar() + index));
        }
        return proxyName;
    }

    /** Proxy definitions for all free variables in given list, in reverse order.
     *  @param pos        The source code position of the definition.
     *  @param freevars   The free variables.
     *  @param owner      The class in which the definitions go.
     */
    List<JCVariableDecl> freevarDefs(int pos, List<VarSymbol> freevars, Symbol owner) {
        return freevarDefs(pos, freevars, owner, 0);
    }

    List<JCVariableDecl> freevarDefs(int pos, List<VarSymbol> freevars, Symbol owner,
            long additionalFlags) {
        long flags = FINAL | SYNTHETIC | additionalFlags;
        List<JCVariableDecl> defs = List.nil();
        Set<Name> proxyNames = new HashSet<>();
        for (List<VarSymbol> l = freevars; l.nonEmpty(); l = l.tail) {
            VarSymbol v = l.head;
            int index = 0;
            Name proxyName;
            do {
                proxyName = proxyName(v.name, index++);
            } while (!proxyNames.add(proxyName));
            VarSymbol proxy = new VarSymbol(
                flags, proxyName, v.erasure(types), owner);
            proxies.put(v, proxy);
            JCVariableDecl vd = make.at(pos).VarDef(proxy, null);
            vd.vartype = access(vd.vartype);
            defs = defs.prepend(vd);
        }
        return defs;
    }

    /** The name of a this$n field
     *  @param type   The class referenced by the this$n field
     */
    Name outerThisName(Type type, Symbol owner) {
        Type t = type.getEnclosingType();
        int nestingLevel = 0;
        while (t.hasTag(CLASS)) {
            t = t.getEnclosingType();
            nestingLevel++;
        }
        Name result = names.fromString("this" + target.syntheticNameChar() + nestingLevel);
        while (owner.kind == TYP && ((ClassSymbol)owner).members().findFirst(result) != null)
            result = names.fromString(result.toString() + target.syntheticNameChar());
        return result;
    }

    private VarSymbol makeOuterThisVarSymbol(Symbol owner, long flags) {
        Type target = types.erasure(owner.enclClass().type.getEnclosingType());
        VarSymbol outerThis =
            new VarSymbol(flags, outerThisName(target, owner), target, owner);
        outerThisStack = outerThisStack.prepend(outerThis);
        return outerThis;
    }

    private JCVariableDecl makeOuterThisVarDecl(int pos, VarSymbol sym) {
        JCVariableDecl vd = make.at(pos).VarDef(sym, null);
        vd.vartype = access(vd.vartype);
        return vd;
    }

    /** Definition for this$n field.
     *  @param pos        The source code position of the definition.
     *  @param owner      The method in which the definition goes.
     */
    JCVariableDecl outerThisDef(int pos, MethodSymbol owner) {
        ClassSymbol c = owner.enclClass();
        boolean isMandated =
            // Anonymous constructors
            (owner.isConstructor() && owner.isAnonymous()) ||
            // Constructors of non-private inner member classes
            (owner.isConstructor() && c.isInner() &&
             !c.isPrivate() && !c.isStatic());
        long flags =
            FINAL | (isMandated ? MANDATED : SYNTHETIC) | PARAMETER;
        VarSymbol outerThis = makeOuterThisVarSymbol(owner, flags);
        owner.extraParams = owner.extraParams.prepend(outerThis);
        return makeOuterThisVarDecl(pos, outerThis);
    }

    /** Definition for this$n field.
     *  @param pos        The source code position of the definition.
     *  @param owner      The class in which the definition goes.
     */
    JCVariableDecl outerThisDef(int pos, ClassSymbol owner) {
        VarSymbol outerThis = makeOuterThisVarSymbol(owner, FINAL | SYNTHETIC);
        return makeOuterThisVarDecl(pos, outerThis);
    }

    /** Return a list of trees that load the free variables in given list,
     *  in reverse order.
     *  @param pos          The source code position to be used for the trees.
     *  @param freevars     The list of free variables.
     */
    List<JCExpression> loadFreevars(DiagnosticPosition pos, List<VarSymbol> freevars) {
        List<JCExpression> args = List.nil();
        for (List<VarSymbol> l = freevars; l.nonEmpty(); l = l.tail)
            args = args.prepend(loadFreevar(pos, l.head));
        return args;
    }
//where
        JCExpression loadFreevar(DiagnosticPosition pos, VarSymbol v) {
            return access(v, make.at(pos).Ident(v), null, false);
        }

    /** Construct a tree simulating the expression {@code C.this}.
     *  @param pos           The source code position to be used for the tree.
     *  @param c             The qualifier class.
     */
    JCExpression makeThis(DiagnosticPosition pos, TypeSymbol c) {
        if (currentClass == c) {
            // in this case, `this' works fine
            return make.at(pos).This(c.erasure(types));
        } else {
            // need to go via this$n
            return makeOuterThis(pos, c);
        }
    }

    /**
     * Optionally replace a try statement with the desugaring of a
     * try-with-resources statement.  The canonical desugaring of
     *
     * try ResourceSpecification
     *   Block
     *
     * is
     *
     * {
     *   final VariableModifiers_minus_final R #resource = Expression;
     *
     *   try ResourceSpecificationtail
     *     Block
     *   } body-only-finally {
     *     if (#resource != null) //nullcheck skipped if Expression is provably non-null
     *         #resource.close();
     *   } catch (Throwable #primaryException) {
     *       if (#resource != null) //nullcheck skipped if Expression is provably non-null
     *           try {
     *               #resource.close();
     *           } catch (Throwable #suppressedException) {
     *              #primaryException.addSuppressed(#suppressedException);
     *           }
     *       throw #primaryException;
     *   }
     * }
     *
     * @param tree  The try statement to inspect.
     * @return A a desugared try-with-resources tree, or the original
     * try block if there are no resources to manage.
     */
    JCTree makeTwrTry(JCTry tree) {
        make_at(tree.pos());
        twrVars = twrVars.dup();
        JCBlock twrBlock = makeTwrBlock(tree.resources, tree.body, 0);
        if (tree.catchers.isEmpty() && tree.finalizer == null)
            result = translate(twrBlock);
        else
            result = translate(make.Try(twrBlock, tree.catchers, tree.finalizer));
        twrVars = twrVars.leave();
        return result;
    }

    private JCBlock makeTwrBlock(List<JCTree> resources, JCBlock block, int depth) {
        if (resources.isEmpty())
            return block;

        // Add resource declaration or expression to block statements
        ListBuffer<JCStatement> stats = new ListBuffer<>();
        JCTree resource = resources.head;
        JCExpression resourceUse;
        boolean resourceNonNull;
        if (resource instanceof JCVariableDecl variableDecl) {
            resourceUse = make.Ident(variableDecl.sym).setType(resource.type);
            resourceNonNull = variableDecl.init != null && TreeInfo.skipParens(variableDecl.init).hasTag(NEWCLASS);
            stats.add(variableDecl);
        } else {
            Assert.check(resource instanceof JCExpression);
            VarSymbol syntheticTwrVar =
            new VarSymbol(SYNTHETIC | FINAL,
                          makeSyntheticName(names.fromString("twrVar" +
                                           depth), twrVars),
                          (resource.type.hasTag(BOT)) ?
                          syms.autoCloseableType : resource.type,
                          currentMethodSym);
            twrVars.enter(syntheticTwrVar);
            JCVariableDecl syntheticTwrVarDecl =
                make.VarDef(syntheticTwrVar, (JCExpression)resource);
            resourceUse = (JCExpression)make.Ident(syntheticTwrVar);
            resourceNonNull = false;
            stats.add(syntheticTwrVarDecl);
        }

        //create (semi-) finally block that will be copied into the main try body:
        int oldPos = make.pos;
        make.at(TreeInfo.endPos(block));

        // if (#resource != null) { #resource.close(); }
        JCStatement bodyCloseStatement = makeResourceCloseInvocation(resourceUse);

        if (!resourceNonNull) {
            bodyCloseStatement = make.If(makeNonNullCheck(resourceUse),
                                         bodyCloseStatement,
                                         null);
        }

        JCBlock finallyClause = make.Block(BODY_ONLY_FINALIZE, List.of(bodyCloseStatement));
        make.at(oldPos);

        // Create catch clause that saves exception, closes the resource and then rethrows the exception:
        VarSymbol primaryException =
            new VarSymbol(FINAL|SYNTHETIC,
                          names.fromString("t" +
                                           target.syntheticNameChar()),
                          syms.throwableType,
                          currentMethodSym);
        JCVariableDecl primaryExceptionDecl = make.VarDef(primaryException, null);

        // close resource:
        // try {
        //     #resource.close();
        // } catch (Throwable #suppressedException) {
        //     #primaryException.addSuppressed(#suppressedException);
        // }
        VarSymbol suppressedException =
            new VarSymbol(SYNTHETIC, make.paramName(2),
                          syms.throwableType,
                          currentMethodSym);
        JCStatement addSuppressedStatement =
            make.Exec(makeCall(make.Ident(primaryException),
                               names.addSuppressed,
                               List.of(make.Ident(suppressedException))));
        JCBlock closeResourceTryBlock =
            make.Block(0L, List.of(makeResourceCloseInvocation(resourceUse)));
        JCVariableDecl catchSuppressedDecl = make.VarDef(suppressedException, null);
        JCBlock catchSuppressedBlock = make.Block(0L, List.of(addSuppressedStatement));
        List<JCCatch> catchSuppressedClauses =
                List.of(make.Catch(catchSuppressedDecl, catchSuppressedBlock));
        JCTry closeResourceTry = make.Try(closeResourceTryBlock, catchSuppressedClauses, null);
        closeResourceTry.finallyCanCompleteNormally = true;

        JCStatement exceptionalCloseStatement = closeResourceTry;

        if (!resourceNonNull) {
            // if (#resource != null) {  }
            exceptionalCloseStatement = make.If(makeNonNullCheck(resourceUse),
                                                exceptionalCloseStatement,
                                                null);
        }

        JCStatement exceptionalRethrow = make.Throw(make.Ident(primaryException));
        JCBlock exceptionalCloseBlock = make.Block(0L, List.of(exceptionalCloseStatement, exceptionalRethrow));
        JCCatch exceptionalCatchClause = make.Catch(primaryExceptionDecl, exceptionalCloseBlock);

        //create the main try statement with the close:
        JCTry outerTry = make.Try(makeTwrBlock(resources.tail, block, depth + 1),
                                  List.of(exceptionalCatchClause),
                                  finallyClause);

        outerTry.finallyCanCompleteNormally = true;
        stats.add(outerTry);

        JCBlock newBlock = make.Block(0L, stats.toList());
        return newBlock;
    }

    private JCStatement makeResourceCloseInvocation(JCExpression resource) {
        // convert to AutoCloseable if needed
        if (types.asSuper(resource.type, syms.autoCloseableType.tsym) == null) {
            resource = convert(resource, syms.autoCloseableType);
        }

        // create resource.close() method invocation
        JCExpression resourceClose = makeCall(resource,
                                              names.close,
                                              List.nil());
        return make.Exec(resourceClose);
    }

    private JCExpression makeNonNullCheck(JCExpression expression) {
        return makeBinary(NE, expression, makeNull());
    }

    /** Construct a tree that represents the outer instance
     *  {@code C.this}. Never pick the current `this'.
     *  @param pos           The source code position to be used for the tree.
     *  @param c             The qualifier class.
     */
    JCExpression makeOuterThis(DiagnosticPosition pos, TypeSymbol c) {
        List<VarSymbol> ots = outerThisStack;
        if (ots.isEmpty()) {
            log.error(pos, Errors.NoEnclInstanceOfTypeInScope(c));
            Assert.error();
            return makeNull();
        }
        VarSymbol ot = ots.head;
        JCExpression tree = access(make.at(pos).Ident(ot));
        TypeSymbol otc = ot.type.tsym;
        while (otc != c) {
            do {
                ots = ots.tail;
                if (ots.isEmpty()) {
                    log.error(pos, Errors.NoEnclInstanceOfTypeInScope(c));
                    Assert.error(); // should have been caught in Attr
                    return tree;
                }
                ot = ots.head;
            } while (ot.owner != otc);
            if (otc.owner.kind != PCK && !otc.hasOuterInstance()) {
                chk.earlyRefError(pos, c);
                Assert.error(); // should have been caught in Attr
                return makeNull();
            }
            tree = access(make.at(pos).Select(tree, ot));
            otc = ot.type.tsym;
        }
        return tree;
    }

    /** Construct a tree that represents the closest outer instance
     *  {@code C.this} such that the given symbol is a member of C.
     *  @param pos           The source code position to be used for the tree.
     *  @param sym           The accessed symbol.
     *  @param preciseMatch  should we accept a type that is a subtype of
     *                       sym's owner, even if it doesn't contain sym
     *                       due to hiding, overriding, or non-inheritance
     *                       due to protection?
     */
    JCExpression makeOwnerThis(DiagnosticPosition pos, Symbol sym, boolean preciseMatch) {
        Symbol c = sym.owner;
        if (preciseMatch ? sym.isMemberOf(currentClass, types)
                         : currentClass.isSubClass(sym.owner, types)) {
            // in this case, `this' works fine
            return make.at(pos).This(c.erasure(types));
        } else {
            // need to go via this$n
            return makeOwnerThisN(pos, sym, preciseMatch);
        }
    }

    /**
     * Similar to makeOwnerThis but will never pick "this".
     */
    JCExpression makeOwnerThisN(DiagnosticPosition pos, Symbol sym, boolean preciseMatch) {
        Symbol c = sym.owner;
        List<VarSymbol> ots = outerThisStack;
        if (ots.isEmpty()) {
            log.error(pos, Errors.NoEnclInstanceOfTypeInScope(c));
            Assert.error();
            return makeNull();
        }
        VarSymbol ot = ots.head;
        JCExpression tree = access(make.at(pos).Ident(ot));
        TypeSymbol otc = ot.type.tsym;
        while (!(preciseMatch ? sym.isMemberOf(otc, types) : otc.isSubClass(sym.owner, types))) {
            do {
                ots = ots.tail;
                if (ots.isEmpty()) {
                    log.error(pos, Errors.NoEnclInstanceOfTypeInScope(c));
                    Assert.error();
                    return tree;
                }
                ot = ots.head;
            } while (ot.owner != otc);
            tree = access(make.at(pos).Select(tree, ot));
            otc = ot.type.tsym;
        }
        return tree;
    }

    /** Return tree simulating the assignment {@code this.name = name}, where
     *  name is the name of a free variable.
     */
    JCStatement initField(int pos, Symbol rhs, Symbol lhs) {
        Assert.check(rhs.owner.kind == MTH);
        Assert.check(rhs.owner.owner == lhs.owner);
        make.at(pos);
        return
            make.Exec(
                make.Assign(
                    make.Select(make.This(lhs.owner.erasure(types)), lhs),
                    make.Ident(rhs)).setType(lhs.erasure(types)));
    }

    /** Return tree simulating the assignment {@code this.this$n = this$n}.
     */
    JCStatement initOuterThis(int pos) {
        VarSymbol rhs = outerThisStack.head;
        Assert.check(rhs.owner.kind == MTH);
        VarSymbol lhs = outerThisStack.tail.head;
        Assert.check(rhs.owner.owner == lhs.owner);
        make.at(pos);
        return
            make.Exec(
                make.Assign(
                    make.Select(make.This(lhs.owner.erasure(types)), lhs),
                    make.Ident(rhs)).setType(lhs.erasure(types)));
    }

/**************************************************************************
 * Code for .class
 *************************************************************************/

    /** Return the symbol of a class to contain a cache of
     *  compiler-generated statics such as class$ and the
     *  $assertionsDisabled flag.  We create an anonymous nested class
     *  (unless one already exists) and return its symbol.  However,
     *  for backward compatibility in 1.4 and earlier we use the
     *  top-level class itself.
     */
    private ClassSymbol outerCacheClass() {
        ClassSymbol clazz = outermostClassDef.sym;
        Scope s = clazz.members();
        for (Symbol sym : s.getSymbols(NON_RECURSIVE))
            if (sym.kind == TYP &&
                sym.name == names.empty &&
                (sym.flags() & INTERFACE) == 0) return (ClassSymbol) sym;
        return makeEmptyClass(STATIC | SYNTHETIC, clazz).sym;
    }

    /** Create an attributed tree of the form left.name(). */
    private JCMethodInvocation makeCall(JCExpression left, Name name, List<JCExpression> args) {
        Assert.checkNonNull(left.type);
        Symbol funcsym = lookupMethod(make_pos, name, left.type,
                                      TreeInfo.types(args));
        return make.App(make.Select(left, funcsym), args);
    }

    /** The tree simulating a T.class expression.
     *  @param clazz      The tree identifying type T.
     */
    private JCExpression classOf(JCTree clazz) {
        return classOfType(clazz.type, clazz.pos());
    }

    private JCExpression classOfType(Type type, DiagnosticPosition pos) {
        switch (type.getTag()) {
        case BYTE: case SHORT: case CHAR: case INT: case LONG: case FLOAT:
        case DOUBLE: case BOOLEAN: case VOID:
            // replace with <BoxedClass>.TYPE
            ClassSymbol c = types.boxedClass(type);
            Symbol typeSym =
                rs.accessBase(
                    rs.findIdentInType(pos, attrEnv, c.type, names.TYPE, KindSelector.VAR),
                    pos, c.type, names.TYPE, true);
            if (typeSym.kind == VAR)
                ((VarSymbol)typeSym).getConstValue(); // ensure initializer is evaluated
            return make.QualIdent(typeSym);
        case CLASS: case ARRAY:
                VarSymbol sym = new VarSymbol(
                        STATIC | PUBLIC | FINAL, names._class,
                        syms.classType, type.tsym);
                return make_at(pos).Select(make.Type(type), sym);
        default:
            throw new AssertionError();
        }
    }

/**************************************************************************
 * Code for enabling/disabling assertions.
 *************************************************************************/

    private ClassSymbol assertionsDisabledClassCache;

    /**Used to create an auxiliary class to hold $assertionsDisabled for interfaces.
     */
    private ClassSymbol assertionsDisabledClass() {
        if (assertionsDisabledClassCache != null) return assertionsDisabledClassCache;

        assertionsDisabledClassCache = makeEmptyClass(STATIC | SYNTHETIC, outermostClassDef.sym).sym;

        return assertionsDisabledClassCache;
    }

    // This code is not particularly robust if the user has
    // previously declared a member named '$assertionsDisabled'.
    // The same faulty idiom also appears in the translation of
    // class literals above.  We should report an error if a
    // previous declaration is not synthetic.

    private JCExpression assertFlagTest(DiagnosticPosition pos) {
        // Outermost class may be either true class or an interface.
        ClassSymbol outermostClass = outermostClassDef.sym;

        //only classes can hold a non-public field, look for a usable one:
        ClassSymbol container = !currentClass.isInterface() ? currentClass :
                assertionsDisabledClass();

        VarSymbol assertDisabledSym =
            (VarSymbol)lookupSynthetic(dollarAssertionsDisabled,
                                       container.members());
        if (assertDisabledSym == null) {
            assertDisabledSym =
                new VarSymbol(STATIC | FINAL | SYNTHETIC,
                              dollarAssertionsDisabled,
                              syms.booleanType,
                              container);
            enterSynthetic(pos, assertDisabledSym, container.members());
            Symbol desiredAssertionStatusSym = lookupMethod(pos,
                                                            names.desiredAssertionStatus,
                                                            types.erasure(syms.classType),
                                                            List.nil());
            JCClassDecl containerDef = classDef(container);
            make_at(containerDef.pos());
            JCExpression notStatus = makeUnary(NOT, make.App(make.Select(
                    classOfType(types.erasure(outermostClass.type),
                                containerDef.pos()),
                    desiredAssertionStatusSym)));
            JCVariableDecl assertDisabledDef = make.VarDef(assertDisabledSym,
                                                   notStatus);
            containerDef.defs = containerDef.defs.prepend(assertDisabledDef);

            if (currentClass.isInterface()) {
                //need to load the assertions enabled/disabled state while
                //initializing the interface:
                JCClassDecl currentClassDef = classDef(currentClass);
                make_at(currentClassDef.pos());
                JCStatement dummy = make.If(make.QualIdent(assertDisabledSym), make.Skip(), null);
                JCBlock clinit = make.Block(STATIC, List.of(dummy));
                currentClassDef.defs = currentClassDef.defs.prepend(clinit);
            }
        }
        make_at(pos);
        return makeUnary(NOT, make.Ident(assertDisabledSym));
    }


/**************************************************************************
 * Building blocks for let expressions
 *************************************************************************/

    interface TreeBuilder {
        JCExpression build(JCExpression arg);
    }

    /** Construct an expression using the builder, with the given rval
     *  expression as an argument to the builder.  However, the rval
     *  expression must be computed only once, even if used multiple
     *  times in the result of the builder.  We do that by
     *  constructing a "let" expression that saves the rvalue into a
     *  temporary variable and then uses the temporary variable in
     *  place of the expression built by the builder.  The complete
     *  resulting expression is of the form
     *  <pre>
     *    (let <b>TYPE</b> <b>TEMP</b> = <b>RVAL</b>;
     *     in (<b>BUILDER</b>(<b>TEMP</b>)))
     *  </pre>
     *  where <code><b>TEMP</b></code> is a newly declared variable
     *  in the let expression.
     */
    JCExpression abstractRval(JCExpression rval, Type type, TreeBuilder builder) {
        rval = TreeInfo.skipParens(rval);
        switch (rval.getTag()) {
        case LITERAL:
            return builder.build(rval);
        case IDENT:
            JCIdent id = (JCIdent) rval;
            if ((id.sym.flags() & FINAL) != 0 && id.sym.owner.kind == MTH)
                return builder.build(rval);
        }
        Name name = TreeInfo.name(rval);
        if (name == names._super || name == names._this)
            return builder.build(rval);
        VarSymbol var =
            new VarSymbol(FINAL|SYNTHETIC,
                          names.fromString(
                                          target.syntheticNameChar()
                                          + "" + rval.hashCode()),
                                      type,
                                      currentMethodSym);
        rval = convert(rval,type);
        JCVariableDecl def = make.VarDef(var, rval); // XXX cast
        JCExpression built = builder.build(make.Ident(var));
        JCExpression res = make.LetExpr(def, built);
        res.type = built.type;
        return res;
    }

    // same as above, with the type of the temporary variable computed
    JCExpression abstractRval(JCExpression rval, TreeBuilder builder) {
        return abstractRval(rval, rval.type, builder);
    }

    // same as above, but for an expression that may be used as either
    // an rvalue or an lvalue.  This requires special handling for
    // Select expressions, where we place the left-hand-side of the
    // select in a temporary, and for Indexed expressions, where we
    // place both the indexed expression and the index value in temps.
    JCExpression abstractLval(JCExpression lval, final TreeBuilder builder) {
        lval = TreeInfo.skipParens(lval);
        switch (lval.getTag()) {
        case IDENT:
            return builder.build(lval);
        case SELECT: {
            final JCFieldAccess s = (JCFieldAccess)lval;
            Symbol lid = TreeInfo.symbol(s.selected);
            if (lid != null && lid.kind == TYP) return builder.build(lval);
            return abstractRval(s.selected, selected -> builder.build(make.Select(selected, s.sym)));
        }
        case INDEXED: {
            final JCArrayAccess i = (JCArrayAccess)lval;
            return abstractRval(i.indexed, indexed -> abstractRval(i.index, syms.intType, index -> {
                JCExpression newLval = make.Indexed(indexed, index);
                newLval.setType(i.type);
                return builder.build(newLval);
            }));
        }
        case TYPECAST: {
            return abstractLval(((JCTypeCast)lval).expr, builder);
        }
        }
        throw new AssertionError(lval);
    }

    // evaluate and discard the first expression, then evaluate the second.
    JCExpression makeComma(final JCExpression expr1, final JCExpression expr2) {
        JCExpression res = make.LetExpr(List.of(make.Exec(expr1)), expr2);
        res.type = expr2.type;
        return res;
    }

/**************************************************************************
 * Translation methods
 *************************************************************************/

    /** Visitor argument: enclosing operator node.
     */
    private JCExpression enclOp;

    /** Visitor method: Translate a single node.
     *  Attach the source position from the old tree to its replacement tree.
     */
    @Override
    public <T extends JCTree> T translate(T tree) {
        if (tree == null) {
            return null;
        } else {
            make_at(tree.pos());
            T result = super.translate(tree);
            if (endPosTable != null && result != tree) {
                endPosTable.replaceTree(tree, result);
            }
            return result;
        }
    }

    /** Visitor method: Translate a single node, boxing or unboxing if needed.
     */
    public <T extends JCExpression> T translate(T tree, Type type) {
        return (tree == null) ? null : boxIfNeeded(translate(tree), type);
    }

    /** Visitor method: Translate tree.
     */
    public <T extends JCTree> T translate(T tree, JCExpression enclOp) {
        JCExpression prevEnclOp = this.enclOp;
        this.enclOp = enclOp;
        T res = translate(tree);
        this.enclOp = prevEnclOp;
        return res;
    }

    /** Visitor method: Translate list of trees.
     */
    public <T extends JCExpression> List<T> translate(List<T> trees, Type type) {
        if (trees == null) return null;
        for (List<T> l = trees; l.nonEmpty(); l = l.tail)
            l.head = translate(l.head, type);
        return trees;
    }

    public void visitPackageDef(JCPackageDecl tree) {
        if (!needPackageInfoClass(tree))
                        return;

        long flags = Flags.ABSTRACT | Flags.INTERFACE;
        // package-info is marked SYNTHETIC in JDK 1.6 and later releases
        flags = flags | Flags.SYNTHETIC;
        ClassSymbol c = tree.packge.package_info;
        c.setAttributes(tree.packge);
        c.flags_field |= flags;
        ClassType ctype = (ClassType) c.type;
        ctype.supertype_field = syms.objectType;
        ctype.interfaces_field = List.nil();
        createInfoClass(tree.annotations, c);
    }
    // where
    private boolean needPackageInfoClass(JCPackageDecl pd) {
        switch (pkginfoOpt) {
            case ALWAYS:
                return true;
            case LEGACY:
                return pd.getAnnotations().nonEmpty();
            case NONEMPTY:
                for (Attribute.Compound a :
                         pd.packge.getDeclarationAttributes()) {
                    Attribute.RetentionPolicy p = types.getRetention(a);
                    if (p != Attribute.RetentionPolicy.SOURCE)
                        return true;
                }
                return false;
        }
        throw new AssertionError();
    }

    public void visitModuleDef(JCModuleDecl tree) {
        ModuleSymbol msym = tree.sym;
        ClassSymbol c = msym.module_info;
        c.setAttributes(msym);
        c.flags_field |= Flags.MODULE;
        createInfoClass(List.nil(), tree.sym.module_info);
    }

    private void createInfoClass(List<JCAnnotation> annots, ClassSymbol c) {
        long flags = Flags.ABSTRACT | Flags.INTERFACE;
        JCClassDecl infoClass =
                make.ClassDef(make.Modifiers(flags, annots),
                    c.name, List.nil(),
                    null, List.nil(), List.nil());
        infoClass.sym = c;
        translated.append(infoClass);
    }

    public void visitClassDef(JCClassDecl tree) {
        Env<AttrContext> prevEnv = attrEnv;
        ClassSymbol currentClassPrev = currentClass;
        MethodSymbol currentMethodSymPrev = currentMethodSym;

        currentClass = tree.sym;
        currentMethodSym = null;
        attrEnv = typeEnvs.remove(currentClass);
        if (attrEnv == null)
            attrEnv = prevEnv;

        classdefs.put(currentClass, tree);

        Map<Symbol, Symbol> prevProxies = proxies;
        proxies = new HashMap<>(proxies);
        List<VarSymbol> prevOuterThisStack = outerThisStack;

        // If this is an enum definition
        if ((tree.mods.flags & ENUM) != 0 &&
            (types.supertype(currentClass.type).tsym.flags() & ENUM) == 0)
            visitEnumDef(tree);

        if ((tree.mods.flags & RECORD) != 0) {
            visitRecordDef(tree);
        }

        // If this is a nested class, define a this$n field for
        // it and add to proxies.
        JCVariableDecl otdef = null;
        if (currentClass.hasOuterInstance())
            otdef = outerThisDef(tree.pos, currentClass);

        // If this is a local class, define proxies for all its free variables.
        List<JCVariableDecl> fvdefs = freevarDefs(
            tree.pos, freevars(currentClass), currentClass);

        // Recursively translate superclass, interfaces.
        tree.extending = translate(tree.extending);
        tree.implementing = translate(tree.implementing);

        if (currentClass.isDirectlyOrIndirectlyLocal()) {
            ClassSymbol encl = currentClass.owner.enclClass();
            if (encl.trans_local == null) {
                encl.trans_local = List.nil();
            }
            encl.trans_local = encl.trans_local.prepend(currentClass);
        }

        // Recursively translate members, taking into account that new members
        // might be created during the translation and prepended to the member
        // list `tree.defs'.
        List<JCTree> seen = List.nil();
        while (tree.defs != seen) {
            List<JCTree> unseen = tree.defs;
            for (List<JCTree> l = unseen; l.nonEmpty() && l != seen; l = l.tail) {
                JCTree outermostMemberDefPrev = outermostMemberDef;
                if (outermostMemberDefPrev == null) outermostMemberDef = l.head;
                l.head = translate(l.head);
                outermostMemberDef = outermostMemberDefPrev;
            }
            seen = unseen;
        }

        // Convert a protected modifier to public, mask static modifier.
        if ((tree.mods.flags & PROTECTED) != 0) tree.mods.flags |= PUBLIC;
        tree.mods.flags &= ClassFlags;

        // Convert name to flat representation, replacing '.' by '$'.
        tree.name = Convert.shortName(currentClass.flatName());

        // Add this$n and free variables proxy definitions to class.

        for (List<JCVariableDecl> l = fvdefs; l.nonEmpty(); l = l.tail) {
            tree.defs = tree.defs.prepend(l.head);
            enterSynthetic(tree.pos(), l.head.sym, currentClass.members());
        }
        if (currentClass.hasOuterInstance()) {
            tree.defs = tree.defs.prepend(otdef);
            enterSynthetic(tree.pos(), otdef.sym, currentClass.members());
        }

        proxies = prevProxies;
        outerThisStack = prevOuterThisStack;

        // Append translated tree to `translated' queue.
        translated.append(tree);

        attrEnv = prevEnv;
        currentClass = currentClassPrev;
        currentMethodSym = currentMethodSymPrev;

        // Return empty block {} as a placeholder for an inner class.
        result = make_at(tree.pos()).Block(SYNTHETIC, List.nil());
    }

    List<JCTree> generateMandatedAccessors(JCClassDecl tree) {
        List<JCVariableDecl> fields = TreeInfo.recordFields(tree);
        return tree.sym.getRecordComponents().stream()
                .filter(rc -> (rc.accessor.flags() & Flags.GENERATED_MEMBER) != 0)
                .map(rc -> {
                    // we need to return the field not the record component
                    JCVariableDecl field = fields.stream().filter(f -> f.name == rc.name).findAny().get();
                    make_at(tree.pos());
                    return make.MethodDef(rc.accessor, make.Block(0,
                            List.of(make.Return(make.Ident(field)))));
                }).collect(List.collector());
    }

    /** Translate an enum class. */
    private void visitEnumDef(JCClassDecl tree) {
        make_at(tree.pos());

        // add the supertype, if needed
        if (tree.extending == null)
            tree.extending = make.Type(types.supertype(tree.type));

        // classOfType adds a cache field to tree.defs
        JCExpression e_class = classOfType(tree.sym.type, tree.pos()).
            setType(types.erasure(syms.classType));

        // process each enumeration constant, adding implicit constructor parameters
        int nextOrdinal = 0;
        ListBuffer<JCExpression> values = new ListBuffer<>();
        ListBuffer<JCTree> enumDefs = new ListBuffer<>();
        ListBuffer<JCTree> otherDefs = new ListBuffer<>();
        for (List<JCTree> defs = tree.defs;
             defs.nonEmpty();
             defs=defs.tail) {
            if (defs.head.hasTag(VARDEF) && (((JCVariableDecl) defs.head).mods.flags & ENUM) != 0) {
                JCVariableDecl var = (JCVariableDecl)defs.head;
                visitEnumConstantDef(var, nextOrdinal++);
                values.append(make.QualIdent(var.sym));
                enumDefs.append(var);
            } else {
                otherDefs.append(defs.head);
            }
        }

        // synthetic private static T[] $values() { return new T[] { a, b, c }; }
        // synthetic private static final T[] $VALUES = $values();
        Name valuesName = syntheticName(tree, "VALUES");
        Type arrayType = new ArrayType(types.erasure(tree.type), syms.arrayClass);
        VarSymbol valuesVar = new VarSymbol(PRIVATE|FINAL|STATIC|SYNTHETIC,
                                            valuesName,
                                            arrayType,
                                            tree.type.tsym);
        JCNewArray newArray = make.NewArray(make.Type(types.erasure(tree.type)),
                                          List.nil(),
                                          values.toList());
        newArray.type = arrayType;

        MethodSymbol valuesMethod = new MethodSymbol(PRIVATE|STATIC|SYNTHETIC,
                syntheticName(tree, "values"),
                new MethodType(List.nil(), arrayType, List.nil(), tree.type.tsym),
                tree.type.tsym);
        enumDefs.append(make.MethodDef(valuesMethod, make.Block(0, List.of(make.Return(newArray)))));
        tree.sym.members().enter(valuesMethod);

        enumDefs.append(make.VarDef(valuesVar, make.App(make.QualIdent(valuesMethod))));
        tree.sym.members().enter(valuesVar);

        Symbol valuesSym = lookupMethod(tree.pos(), names.values,
                                        tree.type, List.nil());
        List<JCStatement> valuesBody;
        if (useClone()) {
            // return (T[]) $VALUES.clone();
            JCTypeCast valuesResult =
                make.TypeCast(valuesSym.type.getReturnType(),
                              make.App(make.Select(make.Ident(valuesVar),
                                                   syms.arrayCloneMethod)));
            valuesBody = List.of(make.Return(valuesResult));
        } else {
            // template: T[] $result = new T[$values.length];
            Name resultName = syntheticName(tree, "result");
            VarSymbol resultVar = new VarSymbol(FINAL|SYNTHETIC,
                                                resultName,
                                                arrayType,
                                                valuesSym);
            JCNewArray resultArray = make.NewArray(make.Type(types.erasure(tree.type)),
                                  List.of(make.Select(make.Ident(valuesVar), syms.lengthVar)),
                                  null);
            resultArray.type = arrayType;
            JCVariableDecl decl = make.VarDef(resultVar, resultArray);

            // template: System.arraycopy($VALUES, 0, $result, 0, $VALUES.length);
            if (systemArraycopyMethod == null) {
                systemArraycopyMethod =
                    new MethodSymbol(PUBLIC | STATIC,
                                     names.fromString("arraycopy"),
                                     new MethodType(List.of(syms.objectType,
                                                            syms.intType,
                                                            syms.objectType,
                                                            syms.intType,
                                                            syms.intType),
                                                    syms.voidType,
                                                    List.nil(),
                                                    syms.methodClass),
                                     syms.systemType.tsym);
            }
            JCStatement copy =
                make.Exec(make.App(make.Select(make.Ident(syms.systemType.tsym),
                                               systemArraycopyMethod),
                          List.of(make.Ident(valuesVar), make.Literal(0),
                                  make.Ident(resultVar), make.Literal(0),
                                  make.Select(make.Ident(valuesVar), syms.lengthVar))));

            // template: return $result;
            JCStatement ret = make.Return(make.Ident(resultVar));
            valuesBody = List.of(decl, copy, ret);
        }

        JCMethodDecl valuesDef =
             make.MethodDef((MethodSymbol)valuesSym, make.Block(0, valuesBody));

        enumDefs.append(valuesDef);

        if (debugLower)
            System.err.println(tree.sym + ".valuesDef = " + valuesDef);

        /** The template for the following code is:
         *
         *     public static E valueOf(String name) {
         *         return (E)Enum.valueOf(E.class, name);
         *     }
         *
         *  where E is tree.sym
         */
        MethodSymbol valueOfSym = lookupMethod(tree.pos(),
                         names.valueOf,
                         tree.sym.type,
                         List.of(syms.stringType));
        Assert.check((valueOfSym.flags() & STATIC) != 0);
        VarSymbol nameArgSym = valueOfSym.params.head;
        JCIdent nameVal = make.Ident(nameArgSym);
        JCStatement enum_ValueOf =
            make.Return(make.TypeCast(tree.sym.type,
                                      makeCall(make.Ident(syms.enumSym),
                                               names.valueOf,
                                               List.of(e_class, nameVal))));
        JCMethodDecl valueOf = make.MethodDef(valueOfSym,
                                           make.Block(0, List.of(enum_ValueOf)));
        nameVal.sym = valueOf.params.head.sym;
        if (debugLower)
            System.err.println(tree.sym + ".valueOf = " + valueOf);
        enumDefs.append(valueOf);

        enumDefs.appendList(otherDefs.toList());
        tree.defs = enumDefs.toList();
    }
        // where
        private MethodSymbol systemArraycopyMethod;
        private boolean useClone() {
            try {
                return syms.objectType.tsym.members().findFirst(names.clone) != null;
            }
            catch (CompletionFailure e) {
                return false;
            }
        }

        private Name syntheticName(JCClassDecl tree, String baseName) {
            Name valuesName = names.fromString(target.syntheticNameChar() + baseName);
            while (tree.sym.members().findFirst(valuesName) != null) // avoid name clash
                valuesName = names.fromString(valuesName + "" + target.syntheticNameChar());
            return valuesName;
        }

    /** Translate an enumeration constant and its initializer. */
    private void visitEnumConstantDef(JCVariableDecl var, int ordinal) {
        JCNewClass varDef = (JCNewClass)var.init;
        varDef.args = varDef.args.
            prepend(makeLit(syms.intType, ordinal)).
            prepend(makeLit(syms.stringType, var.name.toString()));
    }

    private List<VarSymbol> recordVars(Type t) {
        List<VarSymbol> vars = List.nil();
        while (!t.hasTag(NONE)) {
            if (t.hasTag(CLASS)) {
                for (Symbol s : t.tsym.members().getSymbols(s -> s.kind == VAR && (s.flags() & RECORD) != 0)) {
                    vars = vars.prepend((VarSymbol)s);
                }
            }
            t = types.supertype(t);
        }
        return vars;
    }

    /** Translate a record. */
    private void visitRecordDef(JCClassDecl tree) {
        make_at(tree.pos());
        List<VarSymbol> vars = recordVars(tree.type);
        MethodHandleSymbol[] getterMethHandles = new MethodHandleSymbol[vars.size()];
        int index = 0;
        for (VarSymbol var : vars) {
            if (var.owner != tree.sym) {
                var = new VarSymbol(var.flags_field, var.name, var.type, tree.sym);
            }
            getterMethHandles[index] = var.asMethodHandle(true);
            index++;
        }

        tree.defs = tree.defs.appendList(generateMandatedAccessors(tree));
        tree.defs = tree.defs.appendList(List.of(
                generateRecordMethod(tree, names.toString, vars, getterMethHandles),
                generateRecordMethod(tree, names.hashCode, vars, getterMethHandles),
                generateRecordMethod(tree, names.equals, vars, getterMethHandles)
        ));
    }

    JCTree generateRecordMethod(JCClassDecl tree, Name name, List<VarSymbol> vars, MethodHandleSymbol[] getterMethHandles) {
        make_at(tree.pos());
        boolean isEquals = name == names.equals;
        MethodSymbol msym = lookupMethod(tree.pos(),
                name,
                tree.sym.type,
                isEquals ? List.of(syms.objectType) : List.nil());
        // compiler generated methods have the record flag set, user defined ones dont
        if ((msym.flags() & RECORD) != 0) {
            /* class java.lang.runtime.ObjectMethods provides a common bootstrap that provides a customized implementation
             * for methods: toString, hashCode and equals. Here we just need to generate and indy call to:
             * java.lang.runtime.ObjectMethods::bootstrap and provide: the record class, the record component names and
             * the accessors.
             */
            Name bootstrapName = names.bootstrap;
            LoadableConstant[] staticArgsValues = new LoadableConstant[2 + getterMethHandles.length];
            staticArgsValues[0] = (ClassType)tree.sym.type;
            String concatNames = vars.stream()
                    .map(v -> v.name)
                    .collect(Collectors.joining(";", "", ""));
            staticArgsValues[1] = LoadableConstant.String(concatNames);
            int index = 2;
            for (MethodHandleSymbol mho : getterMethHandles) {
                staticArgsValues[index] = mho;
                index++;
            }

            List<Type> staticArgTypes = List.of(syms.classType,
                    syms.stringType,
                    new ArrayType(syms.methodHandleType, syms.arrayClass));

            JCFieldAccess qualifier = makeIndyQualifier(syms.objectMethodsType, tree, msym,
                    List.of(syms.methodHandleLookupType,
                            syms.stringType,
                            syms.typeDescriptorType).appendList(staticArgTypes),
                    staticArgsValues, bootstrapName, name, false);

            VarSymbol _this = new VarSymbol(SYNTHETIC, names._this, tree.sym.type, tree.sym);

            JCMethodInvocation proxyCall;
            if (!isEquals) {
                proxyCall = make.Apply(List.nil(), qualifier, List.of(make.Ident(_this)));
            } else {
                VarSymbol o = msym.params.head;
                o.adr = 0;
                proxyCall = make.Apply(List.nil(), qualifier, List.of(make.Ident(_this), make.Ident(o)));
            }
            proxyCall.type = qualifier.type;
            return make.MethodDef(msym, make.Block(0, List.of(make.Return(proxyCall))));
        } else {
            return make.Block(SYNTHETIC, List.nil());
        }
    }

    private String argsTypeSig(List<Type> typeList) {
        LowerSignatureGenerator sg = new LowerSignatureGenerator();
        sg.assembleSig(typeList);
        return sg.toString();
    }

    /**
     * Signature Generation
     */
    private class LowerSignatureGenerator extends Types.SignatureGenerator {

        /**
         * An output buffer for type signatures.
         */
        StringBuilder sb = new StringBuilder();

        LowerSignatureGenerator() {
            super(types);
        }

        @Override
        protected void append(char ch) {
            sb.append(ch);
        }

        @Override
        protected void append(byte[] ba) {
            sb.append(new String(ba));
        }

        @Override
        protected void append(Name name) {
            sb.append(name.toString());
        }

        @Override
        public String toString() {
            return sb.toString();
        }
    }

    /**
     * Creates an indy qualifier, helpful to be part of an indy invocation
     * @param site                the site
     * @param tree                a class declaration tree
     * @param msym                the method symbol
     * @param staticArgTypes      the static argument types
     * @param staticArgValues     the static argument values
     * @param bootstrapName       the bootstrap name to look for
     * @param argName             normally bootstraps receives a method name as second argument, if you want that name
     *                            to be different to that of the bootstrap name pass a different name here
     * @param isStatic            is it static or not
     * @return                    a field access tree
     */
    JCFieldAccess makeIndyQualifier(
            Type site,
            JCClassDecl tree,
            MethodSymbol msym,
            List<Type> staticArgTypes,
            LoadableConstant[] staticArgValues,
            Name bootstrapName,
            Name argName,
            boolean isStatic) {
        Symbol bsm = rs.resolveInternalMethod(tree.pos(), attrEnv, site,
                bootstrapName, staticArgTypes, List.nil());

        MethodType indyType = msym.type.asMethodType();
        indyType = new MethodType(
                isStatic ? List.nil() : indyType.argtypes.prepend(tree.sym.type),
                indyType.restype,
                indyType.thrown,
                syms.methodClass
        );
        DynamicMethodSymbol dynSym = new DynamicMethodSymbol(argName,
                syms.noSymbol,
                ((MethodSymbol)bsm).asHandle(),
                indyType,
                staticArgValues);
        JCFieldAccess qualifier = make.Select(make.QualIdent(site.tsym), argName);
        qualifier.sym = dynSym;
        qualifier.type = msym.type.asMethodType().restype;
        return qualifier;
    }

    public void visitMethodDef(JCMethodDecl tree) {
        if (tree.name == names.init && (currentClass.flags_field&ENUM) != 0) {
            // Add "String $enum$name, int $enum$ordinal" to the beginning of the
            // argument list for each constructor of an enum.
            JCVariableDecl nameParam = make_at(tree.pos()).
                Param(names.fromString(target.syntheticNameChar() +
                                       "enum" + target.syntheticNameChar() + "name"),
                      syms.stringType, tree.sym);
            nameParam.mods.flags |= SYNTHETIC; nameParam.sym.flags_field |= SYNTHETIC;
            JCVariableDecl ordParam = make.
                Param(names.fromString(target.syntheticNameChar() +
                                       "enum" + target.syntheticNameChar() +
                                       "ordinal"),
                      syms.intType, tree.sym);
            ordParam.mods.flags |= SYNTHETIC; ordParam.sym.flags_field |= SYNTHETIC;

            MethodSymbol m = tree.sym;
            tree.params = tree.params.prepend(ordParam).prepend(nameParam);

            m.extraParams = m.extraParams.prepend(ordParam.sym);
            m.extraParams = m.extraParams.prepend(nameParam.sym);
            Type olderasure = m.erasure(types);
            m.erasure_field = new MethodType(
                olderasure.getParameterTypes().prepend(syms.intType).prepend(syms.stringType),
                olderasure.getReturnType(),
                olderasure.getThrownTypes(),
                syms.methodClass);
        }

        JCMethodDecl prevMethodDef = currentMethodDef;
        MethodSymbol prevMethodSym = currentMethodSym;
        try {
            currentMethodDef = tree;
            currentMethodSym = tree.sym;
            visitMethodDefInternal(tree);
        } finally {
            currentMethodDef = prevMethodDef;
            currentMethodSym = prevMethodSym;
        }
    }

    private void visitMethodDefInternal(JCMethodDecl tree) {
        if (tree.name == names.init &&
            (currentClass.isInner() || currentClass.isDirectlyOrIndirectlyLocal())) {
            // We are seeing a constructor of an inner class.
            MethodSymbol m = tree.sym;

            // Push a new proxy scope for constructor parameters.
            // and create definitions for any this$n and proxy parameters.
            Map<Symbol, Symbol> prevProxies = proxies;
            proxies = new HashMap<>(proxies);
            List<VarSymbol> prevOuterThisStack = outerThisStack;
            List<VarSymbol> fvs = freevars(currentClass);
            JCVariableDecl otdef = null;
            if (currentClass.hasOuterInstance())
                otdef = outerThisDef(tree.pos, m);
            List<JCVariableDecl> fvdefs = freevarDefs(tree.pos, fvs, m, PARAMETER);

            // Recursively translate result type, parameters and thrown list.
            tree.restype = translate(tree.restype);
            tree.params = translateVarDefs(tree.params);
            tree.thrown = translate(tree.thrown);

            // when compiling stubs, don't process body
            if (tree.body == null) {
                result = tree;
                return;
            }

            // Add this$n (if needed) in front of and free variables behind
            // constructor parameter list.
            tree.params = tree.params.appendList(fvdefs);
            if (currentClass.hasOuterInstance()) {
                tree.params = tree.params.prepend(otdef);
            }

            // If this is an initial constructor, i.e., it does not start with
            // this(...), insert initializers for this$n and proxies
            // before (pre-1.4, after) the call to superclass constructor.
            JCStatement selfCall = translate(tree.body.stats.head);

            List<JCStatement> added = List.nil();
            if (fvs.nonEmpty()) {
                List<Type> addedargtypes = List.nil();
                for (List<VarSymbol> l = fvs; l.nonEmpty(); l = l.tail) {
                    m.capturedLocals =
                        m.capturedLocals.prepend((VarSymbol)
                                                (proxies.get(l.head)));
                    if (TreeInfo.isInitialConstructor(tree)) {
                        added = added.prepend(
                          initField(tree.body.pos, proxies.get(l.head), prevProxies.get(l.head)));
                    }
                    addedargtypes = addedargtypes.prepend(l.head.erasure(types));
                }
                Type olderasure = m.erasure(types);
                m.erasure_field = new MethodType(
                    olderasure.getParameterTypes().appendList(addedargtypes),
                    olderasure.getReturnType(),
                    olderasure.getThrownTypes(),
                    syms.methodClass);
            }
            if (currentClass.hasOuterInstance() &&
                TreeInfo.isInitialConstructor(tree))
            {
                added = added.prepend(initOuterThis(tree.body.pos));
            }

            // pop local variables from proxy stack
            proxies = prevProxies;

            // recursively translate following local statements and
            // combine with this- or super-call
            List<JCStatement> stats = translate(tree.body.stats.tail);
            tree.body.stats = stats.prepend(selfCall).prependList(added);
            outerThisStack = prevOuterThisStack;
        } else {
            Map<Symbol, Symbol> prevLambdaTranslationMap =
                    lambdaTranslationMap;
            try {
                lambdaTranslationMap = (tree.sym.flags() & SYNTHETIC) != 0 &&
                        tree.sym.name.startsWith(names.lambda) ?
                        makeTranslationMap(tree) : null;
                super.visitMethodDef(tree);
            } finally {
                lambdaTranslationMap = prevLambdaTranslationMap;
            }
        }
        if (tree.name == names.init && (tree.sym.flags_field & Flags.COMPACT_RECORD_CONSTRUCTOR) != 0) {
            // lets find out if there is any field waiting to be initialized
            ListBuffer<VarSymbol> fields = new ListBuffer<>();
            for (Symbol sym : currentClass.getEnclosedElements()) {
                if (sym.kind == Kinds.Kind.VAR && ((sym.flags() & RECORD) != 0))
                    fields.append((VarSymbol) sym);
            }
            for (VarSymbol field: fields) {
                if ((field.flags_field & Flags.UNINITIALIZED_FIELD) != 0) {
                    VarSymbol param = tree.params.stream().filter(p -> p.name == field.name).findFirst().get().sym;
                    make.at(tree.pos);
                    tree.body.stats = tree.body.stats.append(
                            make.Exec(
                                    make.Assign(
                                            make.Select(make.This(field.owner.erasure(types)), field),
                                            make.Ident(param)).setType(field.erasure(types))));
                    // we don't need the flag at the field anymore
                    field.flags_field &= ~Flags.UNINITIALIZED_FIELD;
                }
            }
        }
        result = tree;
    }
    //where
        private Map<Symbol, Symbol> makeTranslationMap(JCMethodDecl tree) {
            Map<Symbol, Symbol> translationMap = new HashMap<>();
            for (JCVariableDecl vd : tree.params) {
                Symbol p = vd.sym;
                if (p != p.baseSymbol()) {
                    translationMap.put(p.baseSymbol(), p);
                }
            }
            return translationMap;
        }

    public void visitTypeCast(JCTypeCast tree) {
        tree.clazz = translate(tree.clazz);
        if (tree.type.isPrimitive() != tree.expr.type.isPrimitive())
            tree.expr = translate(tree.expr, tree.type);
        else
            tree.expr = translate(tree.expr);
        result = tree;
    }

    public void visitNewClass(JCNewClass tree) {
        ClassSymbol c = (ClassSymbol)tree.constructor.owner;

        // Box arguments, if necessary
        boolean isEnum = (tree.constructor.owner.flags() & ENUM) != 0;
        List<Type> argTypes = tree.constructor.type.getParameterTypes();
        if (isEnum) argTypes = argTypes.prepend(syms.intType).prepend(syms.stringType);
        tree.args = boxArgs(argTypes, tree.args, tree.varargsElement);
        tree.varargsElement = null;

        // If created class is local, add free variables after
        // explicit constructor arguments.
        if (c.isDirectlyOrIndirectlyLocal()) {
            tree.args = tree.args.appendList(loadFreevars(tree.pos(), freevars(c)));
        }

        // If an access constructor is used, append null as a last argument.
        Symbol constructor = accessConstructor(tree.pos(), tree.constructor);
        if (constructor != tree.constructor) {
            tree.args = tree.args.append(makeNull());
            tree.constructor = constructor;
        }

        // If created class has an outer instance, and new is qualified, pass
        // qualifier as first argument. If new is not qualified, pass the
        // correct outer instance as first argument.
        if (c.hasOuterInstance()) {
            JCExpression thisArg;
            if (tree.encl != null) {
                thisArg = attr.makeNullCheck(translate(tree.encl));
                thisArg.type = tree.encl.type;
            } else if (c.isDirectlyOrIndirectlyLocal()) {
                // local class
                thisArg = makeThis(tree.pos(), c.type.getEnclosingType().tsym);
            } else {
                // nested class
                thisArg = makeOwnerThis(tree.pos(), c, false);
            }
            tree.args = tree.args.prepend(thisArg);
        }
        tree.encl = null;

        // If we have an anonymous class, create its flat version, rather
        // than the class or interface following new.
        if (tree.def != null) {
            Map<Symbol, Symbol> prevLambdaTranslationMap = lambdaTranslationMap;
            try {
                lambdaTranslationMap = null;
                translate(tree.def);
            } finally {
                lambdaTranslationMap = prevLambdaTranslationMap;
            }

            tree.clazz = access(make_at(tree.clazz.pos()).Ident(tree.def.sym));
            tree.def = null;
        } else {
            tree.clazz = access(c, tree.clazz, enclOp, false);
        }
        result = tree;
    }

    // Simplify conditionals with known constant controlling expressions.
    // This allows us to avoid generating supporting declarations for
    // the dead code, which will not be eliminated during code generation.
    // Note that Flow.isFalse and Flow.isTrue only return true
    // for constant expressions in the sense of JLS 15.27, which
    // are guaranteed to have no side-effects.  More aggressive
    // constant propagation would require that we take care to
    // preserve possible side-effects in the condition expression.

    // One common case is equality expressions involving a constant and null.
    // Since null is not a constant expression (because null cannot be
    // represented in the constant pool), equality checks involving null are
    // not captured by Flow.isTrue/isFalse.
    // Equality checks involving a constant and null, e.g.
    //     "" == null
    // are safe to simplify as no side-effects can occur.

    private boolean isTrue(JCTree exp) {
        if (exp.type.isTrue())
            return true;
        Boolean b = expValue(exp);
        return b == null ? false : b;
    }
    private boolean isFalse(JCTree exp) {
        if (exp.type.isFalse())
            return true;
        Boolean b = expValue(exp);
        return b == null ? false : !b;
    }
    /* look for (in)equality relations involving null.
     * return true - if expression is always true
     *       false - if expression is always false
     *        null - if expression cannot be eliminated
     */
    private Boolean expValue(JCTree exp) {
        while (exp.hasTag(PARENS))
            exp = ((JCParens)exp).expr;

        boolean eq;
        switch (exp.getTag()) {
        case EQ: eq = true;  break;
        case NE: eq = false; break;
        default:
            return null;
        }

        // we have a JCBinary(EQ|NE)
        // check if we have two literals (constants or null)
        JCBinary b = (JCBinary)exp;
        if (b.lhs.type.hasTag(BOT)) return expValueIsNull(eq, b.rhs);
        if (b.rhs.type.hasTag(BOT)) return expValueIsNull(eq, b.lhs);
        return null;
    }
    private Boolean expValueIsNull(boolean eq, JCTree t) {
        if (t.type.hasTag(BOT)) return Boolean.valueOf(eq);
        if (t.hasTag(LITERAL))  return Boolean.valueOf(!eq);
        return null;
    }

    /** Visitor method for conditional expressions.
     */
    @Override
    public void visitConditional(JCConditional tree) {
        JCTree cond = tree.cond = translate(tree.cond, syms.booleanType);
        if (isTrue(cond)) {
            result = convert(translate(tree.truepart, tree.type), tree.type);
            addPrunedInfo(cond);
        } else if (isFalse(cond)) {
            result = convert(translate(tree.falsepart, tree.type), tree.type);
            addPrunedInfo(cond);
        } else {
            // Condition is not a compile-time constant.
            tree.truepart = translate(tree.truepart, tree.type);
            tree.falsepart = translate(tree.falsepart, tree.type);
            result = tree;
        }
    }
//where
    private JCExpression convert(JCExpression tree, Type pt) {
        if (tree.type == pt || tree.type.hasTag(BOT))
            return tree;
        JCExpression result = make_at(tree.pos()).TypeCast(make.Type(pt), tree);
        result.type = (tree.type.constValue() != null) ? cfolder.coerce(tree.type, pt)
                                                       : pt;
        return result;
    }

    /** Visitor method for if statements.
     */
    public void visitIf(JCIf tree) {
        JCTree cond = tree.cond = translate(tree.cond, syms.booleanType);
        if (isTrue(cond)) {
            result = translate(tree.thenpart);
            addPrunedInfo(cond);
        } else if (isFalse(cond)) {
            if (tree.elsepart != null) {
                result = translate(tree.elsepart);
            } else {
                result = make.Skip();
            }
            addPrunedInfo(cond);
        } else {
            // Condition is not a compile-time constant.
            tree.thenpart = translate(tree.thenpart);
            tree.elsepart = translate(tree.elsepart);
            result = tree;
        }
    }

    /** Visitor method for assert statements. Translate them away.
     */
    public void visitAssert(JCAssert tree) {
        tree.cond = translate(tree.cond, syms.booleanType);
        if (!tree.cond.type.isTrue()) {
            JCExpression cond = assertFlagTest(tree.pos());
            List<JCExpression> exnArgs = (tree.detail == null) ?
                List.nil() : List.of(translate(tree.detail));
            if (!tree.cond.type.isFalse()) {
                cond = makeBinary
                    (AND,
                     cond,
                     makeUnary(NOT, tree.cond));
            }
            result =
                make.If(cond,
                        make_at(tree).
                           Throw(makeNewClass(syms.assertionErrorType, exnArgs)),
                        null);
        } else {
            result = make.Skip();
        }
    }

    public void visitApply(JCMethodInvocation tree) {
        Symbol meth = TreeInfo.symbol(tree.meth);
        List<Type> argtypes = meth.type.getParameterTypes();
        if (meth.name == names.init && meth.owner == syms.enumSym)
            argtypes = argtypes.tail.tail;
        tree.args = boxArgs(argtypes, tree.args, tree.varargsElement);
        tree.varargsElement = null;
        Name methName = TreeInfo.name(tree.meth);
        if (meth.name==names.init) {
            // We are seeing a this(...) or super(...) constructor call.
            // If an access constructor is used, append null as a last argument.
            Symbol constructor = accessConstructor(tree.pos(), meth);
            if (constructor != meth) {
                tree.args = tree.args.append(makeNull());
                TreeInfo.setSymbol(tree.meth, constructor);
            }

            // If we are calling a constructor of a local class, add
            // free variables after explicit constructor arguments.
            ClassSymbol c = (ClassSymbol)constructor.owner;
            if (c.isDirectlyOrIndirectlyLocal()) {
                tree.args = tree.args.appendList(loadFreevars(tree.pos(), freevars(c)));
            }

            // If we are calling a constructor of an enum class, pass
            // along the name and ordinal arguments
            if ((c.flags_field&ENUM) != 0 || c.getQualifiedName() == names.java_lang_Enum) {
                List<JCVariableDecl> params = currentMethodDef.params;
                if (currentMethodSym.owner.hasOuterInstance())
                    params = params.tail; // drop this$n
                tree.args = tree.args
                    .prepend(make_at(tree.pos()).Ident(params.tail.head.sym)) // ordinal
                    .prepend(make.Ident(params.head.sym)); // name
            }

            // If we are calling a constructor of a class with an outer
            // instance, and the call
            // is qualified, pass qualifier as first argument in front of
            // the explicit constructor arguments. If the call
            // is not qualified, pass the correct outer instance as
            // first argument.
            if (c.hasOuterInstance()) {
                JCExpression thisArg;
                if (tree.meth.hasTag(SELECT)) {
                    thisArg = attr.
                        makeNullCheck(translate(((JCFieldAccess) tree.meth).selected));
                    tree.meth = make.Ident(constructor);
                    ((JCIdent) tree.meth).name = methName;
                } else if (c.isDirectlyOrIndirectlyLocal() || methName == names._this){
                    // local class or this() call
                    thisArg = makeThis(tree.meth.pos(), c.type.getEnclosingType().tsym);
                } else {
                    // super() call of nested class - never pick 'this'
                    thisArg = makeOwnerThisN(tree.meth.pos(), c, false);
                }
                tree.args = tree.args.prepend(thisArg);
            }
        } else {
            // We are seeing a normal method invocation; translate this as usual.
            tree.meth = translate(tree.meth);

            // If the translated method itself is an Apply tree, we are
            // seeing an access method invocation. In this case, append
            // the method arguments to the arguments of the access method.
            if (tree.meth.hasTag(APPLY)) {
                JCMethodInvocation app = (JCMethodInvocation)tree.meth;
                app.args = tree.args.prependList(app.args);
                result = app;
                return;
            }
        }
        result = tree;
    }

    List<JCExpression> boxArgs(List<Type> parameters, List<JCExpression> _args, Type varargsElement) {
        List<JCExpression> args = _args;
        if (parameters.isEmpty()) return args;
        boolean anyChanges = false;
        ListBuffer<JCExpression> result = new ListBuffer<>();
        while (parameters.tail.nonEmpty()) {
            JCExpression arg = translate(args.head, parameters.head);
            anyChanges |= (arg != args.head);
            result.append(arg);
            args = args.tail;
            parameters = parameters.tail;
        }
        Type parameter = parameters.head;
        if (varargsElement != null) {
            anyChanges = true;
            ListBuffer<JCExpression> elems = new ListBuffer<>();
            while (args.nonEmpty()) {
                JCExpression arg = translate(args.head, varargsElement);
                elems.append(arg);
                args = args.tail;
            }
            JCNewArray boxedArgs = make.NewArray(make.Type(varargsElement),
                                               List.nil(),
                                               elems.toList());
            boxedArgs.type = new ArrayType(varargsElement, syms.arrayClass);
            result.append(boxedArgs);
        } else {
            if (args.length() != 1) throw new AssertionError(args);
            JCExpression arg = translate(args.head, parameter);
            anyChanges |= (arg != args.head);
            result.append(arg);
            if (!anyChanges) return _args;
        }
        return result.toList();
    }

    /** Expand a boxing or unboxing conversion if needed. */
    @SuppressWarnings("unchecked") // XXX unchecked
    <T extends JCExpression> T boxIfNeeded(T tree, Type type) {
        boolean havePrimitive = tree.type.isPrimitive();
        if (havePrimitive == type.isPrimitive())
            return tree;
        if (havePrimitive) {
            Type unboxedTarget = types.unboxedType(type);
            if (!unboxedTarget.hasTag(NONE)) {
                if (!types.isSubtype(tree.type, unboxedTarget)) //e.g. Character c = 89;
                    tree.type = unboxedTarget.constType(tree.type.constValue());
                return (T)boxPrimitive(tree, types.erasure(type));
            } else {
                tree = (T)boxPrimitive(tree);
            }
        } else {
            tree = (T)unbox(tree, type);
        }
        return tree;
    }

    /** Box up a single primitive expression. */
    JCExpression boxPrimitive(JCExpression tree) {
        return boxPrimitive(tree, types.boxedClass(tree.type).type);
    }

    /** Box up a single primitive expression. */
    JCExpression boxPrimitive(JCExpression tree, Type box) {
        make_at(tree.pos());
        Symbol valueOfSym = lookupMethod(tree.pos(),
                                         names.valueOf,
                                         box,
                                         List.<Type>nil()
                                         .prepend(tree.type));
        return make.App(make.QualIdent(valueOfSym), List.of(tree));
    }

    /** Unbox an object to a primitive value. */
    JCExpression unbox(JCExpression tree, Type primitive) {
        Type unboxedType = types.unboxedType(tree.type);
        if (unboxedType.hasTag(NONE)) {
            unboxedType = primitive;
            if (!unboxedType.isPrimitive())
                throw new AssertionError(unboxedType);
            make_at(tree.pos());
            tree = make.TypeCast(types.boxedClass(unboxedType).type, tree);
        } else {
            // There must be a conversion from unboxedType to primitive.
            if (!types.isSubtype(unboxedType, primitive))
                throw new AssertionError(tree);
        }
        make_at(tree.pos());
        Symbol valueSym = lookupMethod(tree.pos(),
                                       unboxedType.tsym.name.append(names.Value), // x.intValue()
                                       tree.type,
                                       List.nil());
        return make.App(make.Select(tree, valueSym));
    }

    /** Visitor method for parenthesized expressions.
     *  If the subexpression has changed, omit the parens.
     */
    public void visitParens(JCParens tree) {
        JCTree expr = translate(tree.expr);
        result = ((expr == tree.expr) ? tree : expr);
    }

    public void visitIndexed(JCArrayAccess tree) {
        tree.indexed = translate(tree.indexed);
        tree.index = translate(tree.index, syms.intType);
        result = tree;
    }

    public void visitAssign(JCAssign tree) {
        tree.lhs = translate(tree.lhs, tree);
        tree.rhs = translate(tree.rhs, tree.lhs.type);

        // If translated left hand side is an Apply, we are
        // seeing an access method invocation. In this case, append
        // right hand side as last argument of the access method.
        if (tree.lhs.hasTag(APPLY)) {
            JCMethodInvocation app = (JCMethodInvocation)tree.lhs;
            app.args = List.of(tree.rhs).prependList(app.args);
            result = app;
        } else {
            result = tree;
        }
    }

    public void visitAssignop(final JCAssignOp tree) {
        final boolean boxingReq = !tree.lhs.type.isPrimitive() &&
            tree.operator.type.getReturnType().isPrimitive();

        AssignopDependencyScanner depScanner = new AssignopDependencyScanner(tree);
        depScanner.scan(tree.rhs);

        if (boxingReq || depScanner.dependencyFound) {
            // boxing required; need to rewrite as x = (unbox typeof x)(x op y);
            // or if x == (typeof x)z then z = (unbox typeof x)((typeof x)z op y)
            // (but without recomputing x)
            JCTree newTree = abstractLval(tree.lhs, lhs -> {
                Tag newTag = tree.getTag().noAssignOp();
                // Erasure (TransTypes) can change the type of
                // tree.lhs.  However, we can still get the
                // unerased type of tree.lhs as it is stored
                // in tree.type in Attr.
                OperatorSymbol newOperator = operators.resolveBinary(tree,
                                                              newTag,
                                                              tree.type,
                                                              tree.rhs.type);
                //Need to use the "lhs" at two places, once on the future left hand side
                //and once in the future binary operator. But further processing may change
                //the components of the tree in place (see visitSelect for e.g. <Class>.super.<ident>),
                //so cloning the tree to avoid interference between the uses:
                JCExpression expr = (JCExpression) lhs.clone();
                if (expr.type != tree.type)
                    expr = make.TypeCast(tree.type, expr);
                JCBinary opResult = make.Binary(newTag, expr, tree.rhs);
                opResult.operator = newOperator;
                opResult.type = newOperator.type.getReturnType();
                JCExpression newRhs = boxingReq ?
                    make.TypeCast(types.unboxedType(tree.type), opResult) :
                    opResult;
                return make.Assign(lhs, newRhs).setType(tree.type);
            });
            result = translate(newTree);
            return;
        }
        tree.lhs = translate(tree.lhs, tree);
        tree.rhs = translate(tree.rhs, tree.operator.type.getParameterTypes().tail.head);

        // If translated left hand side is an Apply, we are
        // seeing an access method invocation. In this case, append
        // right hand side as last argument of the access method.
        if (tree.lhs.hasTag(APPLY)) {
            JCMethodInvocation app = (JCMethodInvocation)tree.lhs;
            // if operation is a += on strings,
            // make sure to convert argument to string
            JCExpression rhs = tree.operator.opcode == string_add
              ? makeString(tree.rhs)
              : tree.rhs;
            app.args = List.of(rhs).prependList(app.args);
            result = app;
        } else {
            result = tree;
        }
    }

    class AssignopDependencyScanner extends TreeScanner {

        Symbol sym;
        boolean dependencyFound = false;

        AssignopDependencyScanner(JCAssignOp tree) {
            this.sym = TreeInfo.symbol(tree.lhs);
        }

        @Override
        public void scan(JCTree tree) {
            if (tree != null && sym != null) {
                tree.accept(this);
            }
        }

        @Override
        public void visitAssignop(JCAssignOp tree) {
            if (TreeInfo.symbol(tree.lhs) == sym) {
                dependencyFound = true;
                return;
            }
            super.visitAssignop(tree);
        }

        @Override
        public void visitUnary(JCUnary tree) {
            if (TreeInfo.symbol(tree.arg) == sym) {
                dependencyFound = true;
                return;
            }
            super.visitUnary(tree);
        }
    }

    /** Lower a tree of the form e++ or e-- where e is an object type */
    JCExpression lowerBoxedPostop(final JCUnary tree) {
        // translate to tmp1=lval(e); tmp2=tmp1; tmp1 OP 1; tmp2
        // or
        // translate to tmp1=lval(e); tmp2=tmp1; (typeof tree)tmp1 OP 1; tmp2
        // where OP is += or -=
        final boolean cast = TreeInfo.skipParens(tree.arg).hasTag(TYPECAST);
        return abstractLval(tree.arg, tmp1 -> abstractRval(tmp1, tree.arg.type, tmp2 -> {
            Tag opcode = (tree.hasTag(POSTINC))
                ? PLUS_ASG : MINUS_ASG;
            //"tmp1" and "tmp2" may refer to the same instance
            //(for e.g. <Class>.super.<ident>). But further processing may
            //change the components of the tree in place (see visitSelect),
            //so cloning the tree to avoid interference between the two uses:
            JCExpression lhs = (JCExpression)tmp1.clone();
            lhs = cast
                ? make.TypeCast(tree.arg.type, lhs)
                : lhs;
            JCExpression update = makeAssignop(opcode,
                                         lhs,
                                         make.Literal(1));
            return makeComma(update, tmp2);
        }));
    }

    public void visitUnary(JCUnary tree) {
        boolean isUpdateOperator = tree.getTag().isIncOrDecUnaryOp();
        if (isUpdateOperator && !tree.arg.type.isPrimitive()) {
            switch(tree.getTag()) {
            case PREINC:            // ++ e
                    // translate to e += 1
            case PREDEC:            // -- e
                    // translate to e -= 1
                {
                    JCTree.Tag opcode = (tree.hasTag(PREINC))
                        ? PLUS_ASG : MINUS_ASG;
                    JCAssignOp newTree = makeAssignop(opcode,
                                                    tree.arg,
                                                    make.Literal(1));
                    result = translate(newTree, tree.type);
                    return;
                }
            case POSTINC:           // e ++
            case POSTDEC:           // e --
                {
                    result = translate(lowerBoxedPostop(tree), tree.type);
                    return;
                }
            }
            throw new AssertionError(tree);
        }

        tree.arg = boxIfNeeded(translate(tree.arg, tree), tree.type);

        if (tree.hasTag(NOT) && tree.arg.type.constValue() != null) {
            tree.type = cfolder.fold1(bool_not, tree.arg.type);
        }

        // If translated left hand side is an Apply, we are
        // seeing an access method invocation. In this case, return
        // that access method invocation as result.
        if (isUpdateOperator && tree.arg.hasTag(APPLY)) {
            result = tree.arg;
        } else {
            result = tree;
        }
    }

    public void visitBinary(JCBinary tree) {
        List<Type> formals = tree.operator.type.getParameterTypes();
        JCTree lhs = tree.lhs = translate(tree.lhs, formals.head);
        switch (tree.getTag()) {
        case OR:
            if (isTrue(lhs)) {
                result = lhs;
                return;
            }
            if (isFalse(lhs)) {
                result = translate(tree.rhs, formals.tail.head);
                return;
            }
            break;
        case AND:
            if (isFalse(lhs)) {
                result = lhs;
                return;
            }
            if (isTrue(lhs)) {
                result = translate(tree.rhs, formals.tail.head);
                return;
            }
            break;
        }
        tree.rhs = translate(tree.rhs, formals.tail.head);
        result = tree;
    }

    public void visitIdent(JCIdent tree) {
        result = access(tree.sym, tree, enclOp, false);
    }

    /** Translate away the foreach loop.  */
    public void visitForeachLoop(JCEnhancedForLoop tree) {
        if (types.elemtype(tree.expr.type) == null)
            visitIterableForeachLoop(tree);
        else
            visitArrayForeachLoop(tree);
    }
        // where
        /**
         * A statement of the form
         *
         * <pre>
         *     for ( T v : arrayexpr ) stmt;
         * </pre>
         *
         * (where arrayexpr is of an array type) gets translated to
         *
         * <pre>{@code
         *     for ( { arraytype #arr = arrayexpr;
         *             int #len = array.length;
         *             int #i = 0; };
         *           #i < #len; i$++ ) {
         *         T v = arr$[#i];
         *         stmt;
         *     }
         * }</pre>
         *
         * where #arr, #len, and #i are freshly named synthetic local variables.
         */
        private void visitArrayForeachLoop(JCEnhancedForLoop tree) {
            make_at(tree.expr.pos());
            VarSymbol arraycache = new VarSymbol(SYNTHETIC,
                                                 names.fromString("arr" + target.syntheticNameChar()),
                                                 tree.expr.type,
                                                 currentMethodSym);
            JCStatement arraycachedef = make.VarDef(arraycache, tree.expr);
            VarSymbol lencache = new VarSymbol(SYNTHETIC,
                                               names.fromString("len" + target.syntheticNameChar()),
                                               syms.intType,
                                               currentMethodSym);
            JCStatement lencachedef = make.
                VarDef(lencache, make.Select(make.Ident(arraycache), syms.lengthVar));
            VarSymbol index = new VarSymbol(SYNTHETIC,
                                            names.fromString("i" + target.syntheticNameChar()),
                                            syms.intType,
                                            currentMethodSym);

            JCVariableDecl indexdef = make.VarDef(index, make.Literal(INT, 0));
            indexdef.init.type = indexdef.type = syms.intType.constType(0);

            List<JCStatement> loopinit = List.of(arraycachedef, lencachedef, indexdef);
            JCBinary cond = makeBinary(LT, make.Ident(index), make.Ident(lencache));

            JCExpressionStatement step = make.Exec(makeUnary(PREINC, make.Ident(index)));

            Type elemtype = types.elemtype(tree.expr.type);
            JCExpression loopvarinit = make.Indexed(make.Ident(arraycache),
                                                    make.Ident(index)).setType(elemtype);
            JCVariableDecl loopvardef = (JCVariableDecl)make.VarDef(tree.var.mods,
                                                  tree.var.name,
                                                  tree.var.vartype,
                                                  loopvarinit).setType(tree.var.type);
            loopvardef.sym = tree.var.sym;
            JCBlock body = make.
                Block(0, List.of(loopvardef, tree.body));

            result = translate(make.
                               ForLoop(loopinit,
                                       cond,
                                       List.of(step),
                                       body));
            patchTargets(body, tree, result);
        }
        /** Patch up break and continue targets. */
        private void patchTargets(JCTree body, final JCTree src, final JCTree dest) {
            class Patcher extends TreeScanner {
                public void visitBreak(JCBreak tree) {
                    if (tree.target == src)
                        tree.target = dest;
                }
                public void visitYield(JCYield tree) {
                    if (tree.target == src)
                        tree.target = dest;
                    scan(tree.value);
                }
                public void visitContinue(JCContinue tree) {
                    if (tree.target == src)
                        tree.target = dest;
                }
                public void visitClassDef(JCClassDecl tree) {}
            }
            new Patcher().scan(body);
        }
        /**
         * A statement of the form
         *
         * <pre>
         *     for ( T v : coll ) stmt ;
         * </pre>
         *
         * (where coll implements {@code Iterable<? extends T>}) gets translated to
         *
         * <pre>{@code
         *     for ( Iterator<? extends T> #i = coll.iterator(); #i.hasNext(); ) {
         *         T v = (T) #i.next();
         *         stmt;
         *     }
         * }</pre>
         *
         * where #i is a freshly named synthetic local variable.
         */
        private void visitIterableForeachLoop(JCEnhancedForLoop tree) {
            make_at(tree.expr.pos());
            Type iteratorTarget = syms.objectType;
            Type iterableType = types.asSuper(types.cvarUpperBound(tree.expr.type),
                                              syms.iterableType.tsym);
            if (iterableType.getTypeArguments().nonEmpty())
                iteratorTarget = types.erasure(iterableType.getTypeArguments().head);
            Type eType = types.skipTypeVars(tree.expr.type, false);
            tree.expr.type = types.erasure(eType);
            if (eType.isCompound())
                tree.expr = make.TypeCast(types.erasure(iterableType), tree.expr);
            Symbol iterator = lookupMethod(tree.expr.pos(),
                                           names.iterator,
                                           eType,
                                           List.nil());
            VarSymbol itvar = new VarSymbol(SYNTHETIC, names.fromString("i" + target.syntheticNameChar()),
                                            types.erasure(types.asSuper(iterator.type.getReturnType(), syms.iteratorType.tsym)),
                                            currentMethodSym);

             JCStatement init = make.
                VarDef(itvar, make.App(make.Select(tree.expr, iterator)
                     .setType(types.erasure(iterator.type))));

            Symbol hasNext = lookupMethod(tree.expr.pos(),
                                          names.hasNext,
                                          itvar.type,
                                          List.nil());
            JCMethodInvocation cond = make.App(make.Select(make.Ident(itvar), hasNext));
            Symbol next = lookupMethod(tree.expr.pos(),
                                       names.next,
                                       itvar.type,
                                       List.nil());
            JCExpression vardefinit = make.App(make.Select(make.Ident(itvar), next));
            if (tree.var.type.isPrimitive())
                vardefinit = make.TypeCast(types.cvarUpperBound(iteratorTarget), vardefinit);
            else
                vardefinit = make.TypeCast(tree.var.type, vardefinit);
            JCVariableDecl indexDef = (JCVariableDecl)make.VarDef(tree.var.mods,
                                                  tree.var.name,
                                                  tree.var.vartype,
                                                  vardefinit).setType(tree.var.type);
            indexDef.sym = tree.var.sym;
            JCBlock body = make.Block(0, List.of(indexDef, tree.body));
            body.endpos = TreeInfo.endPos(tree.body);
            result = translate(make.
                ForLoop(List.of(init),
                        cond,
                        List.nil(),
                        body));
            patchTargets(body, tree, result);
        }

    public void visitVarDef(JCVariableDecl tree) {
        MethodSymbol oldMethodSym = currentMethodSym;
        tree.mods = translate(tree.mods);
        tree.vartype = translate(tree.vartype);
        if (currentMethodSym == null) {
            // A class or instance field initializer.
            currentMethodSym =
                new MethodSymbol((tree.mods.flags&STATIC) | BLOCK,
                                 names.empty, null,
                                 currentClass);
        }
        if (tree.init != null) tree.init = translate(tree.init, tree.type);
        result = tree;
        currentMethodSym = oldMethodSym;
    }

    public void visitBlock(JCBlock tree) {
        MethodSymbol oldMethodSym = currentMethodSym;
        if (currentMethodSym == null) {
            // Block is a static or instance initializer.
            currentMethodSym =
                new MethodSymbol(tree.flags | BLOCK,
                                 names.empty, null,
                                 currentClass);
        }
        super.visitBlock(tree);
        currentMethodSym = oldMethodSym;
    }

    public void visitDoLoop(JCDoWhileLoop tree) {
        tree.body = translate(tree.body);
        tree.cond = translate(tree.cond, syms.booleanType);
        result = tree;
    }

    public void visitWhileLoop(JCWhileLoop tree) {
        tree.cond = translate(tree.cond, syms.booleanType);
        tree.body = translate(tree.body);
        result = tree;
    }

    public void visitForLoop(JCForLoop tree) {
        tree.init = translate(tree.init);
        if (tree.cond != null)
            tree.cond = translate(tree.cond, syms.booleanType);
        tree.step = translate(tree.step);
        tree.body = translate(tree.body);
        result = tree;
    }

    public void visitReturn(JCReturn tree) {
        if (tree.expr != null)
            tree.expr = translate(tree.expr,
                                  types.erasure(currentMethodDef
                                                .restype.type));
        result = tree;
    }

    public void visitSwitch(JCSwitch tree) {
        List<JCCase> cases = tree.patternSwitch ? addDefaultIfNeeded(tree.cases) : tree.cases;
        handleSwitch(tree, tree.selector, cases);
    }

    @Override
    public void visitSwitchExpression(JCSwitchExpression tree) {
        List<JCCase> cases = addDefaultIfNeeded(tree.cases);
        handleSwitch(tree, tree.selector, cases);
    }

    private List<JCCase> addDefaultIfNeeded(List<JCCase> cases) {
        if (cases.stream().flatMap(c -> c.labels.stream()).noneMatch(p -> p.hasTag(Tag.DEFAULTCASELABEL))) {
            JCThrow thr = make.Throw(makeNewClass(syms.incompatibleClassChangeErrorType,
                                                  List.nil()));
            JCCase c = make.Case(JCCase.STATEMENT, List.of(make.DefaultCaseLabel()), List.of(thr), null);
            cases = cases.prepend(c);
        }

        return cases;
    }

    private void handleSwitch(JCTree tree, JCExpression selector, List<JCCase> cases) {
        //expand multiple label cases:
        ListBuffer<JCCase> convertedCases = new ListBuffer<>();

        for (JCCase c : cases) {
            switch (c.labels.size()) {
                case 0: //default
                case 1: //single label
                    convertedCases.append(c);
                    break;
                default: //multiple labels, expand:
                    //case C1, C2, C3: ...
                    //=>
                    //case C1:
                    //case C2:
                    //case C3: ...
                    List<JCCaseLabel> patterns = c.labels;
                    while (patterns.tail.nonEmpty()) {
                        convertedCases.append(make_at(c.pos()).Case(JCCase.STATEMENT,
                                                           List.of(patterns.head),
                                                           List.nil(),
                                                           null));
                        patterns = patterns.tail;
                    }
                    c.labels = patterns;
                    convertedCases.append(c);
                    break;
            }
        }

        for (JCCase c : convertedCases) {
            if (c.caseKind == JCCase.RULE && c.completesNormally) {
                JCBreak b = make.at(TreeInfo.endPos(c.stats.last())).Break(null);
                b.target = tree;
                c.stats = c.stats.append(b);
            }
        }

        cases = convertedCases.toList();

        Type selsuper = types.supertype(selector.type);
        boolean enumSwitch = selsuper != null &&
            (selector.type.tsym.flags() & ENUM) != 0;
        boolean stringSwitch = selsuper != null &&
            types.isSameType(selector.type, syms.stringType);
        boolean boxedSwitch = !enumSwitch && !stringSwitch && !selector.type.isPrimitive();
        selector = translate(selector, selector.type);
        cases = translateCases(cases);
        if (tree.hasTag(SWITCH)) {
            ((JCSwitch) tree).selector = selector;
            ((JCSwitch) tree).cases = cases;
        } else if (tree.hasTag(SWITCH_EXPRESSION)) {
            ((JCSwitchExpression) tree).selector = selector;
            ((JCSwitchExpression) tree).cases = cases;
        } else {
            Assert.error();
        }
        if (enumSwitch) {
            result = visitEnumSwitch(tree, selector, cases);
        } else if (stringSwitch) {
            result = visitStringSwitch(tree, selector, cases);
        } else if (boxedSwitch) {
            //An switch over boxed primitive. Pattern matching switches are already translated
            //by TransPatterns, so all non-primitive types are only boxed primitives:
            result = visitBoxedPrimitiveSwitch(tree, selector, cases);
        } else {
            result = tree;
        }
    }

    public JCTree visitEnumSwitch(JCTree tree, JCExpression selector, List<JCCase> cases) {
        TypeSymbol enumSym = selector.type.tsym;
        EnumMapping map = mapForEnum(tree.pos(), enumSym);
        make_at(tree.pos());
        Symbol ordinalMethod = lookupMethod(tree.pos(),
                                            names.ordinal,
                                            selector.type,
                                            List.nil());
        JCExpression newSelector;

        if (cases.stream().anyMatch(c -> TreeInfo.isNull(c.labels.head))) {
            //for enum switches with case null, do:
            //switch ($selector != null ? $mapVar[$selector.ordinal()] : -1) {...}
            //replacing case null with case -1:
            VarSymbol dollar_s = new VarSymbol(FINAL|SYNTHETIC,
                                               names.fromString("s" + tree.pos + this.target.syntheticNameChar()),
                                               selector.type,
                                               currentMethodSym);
            JCStatement var = make.at(tree.pos()).VarDef(dollar_s, selector).setType(dollar_s.type);
            newSelector = make.Indexed(map.mapVar,
                    make.App(make.Select(make.Ident(dollar_s),
                            ordinalMethod)));
            newSelector =
                    make.LetExpr(List.of(var),
                                 make.Conditional(makeBinary(NE, make.Ident(dollar_s), makeNull()),
                                                  newSelector,
                                                  makeLit(syms.intType, -1))
                                     .setType(newSelector.type))
                        .setType(newSelector.type);
        } else {
            newSelector = make.Indexed(map.mapVar,
                    make.App(make.Select(selector,
                            ordinalMethod)));
        }
        ListBuffer<JCCase> newCases = new ListBuffer<>();
        for (JCCase c : cases) {
            if (c.labels.head.isExpression()) {
                JCExpression pat;
                if (TreeInfo.isNull(c.labels.head)) {
                    pat = makeLit(syms.intType, -1);
                } else {
                    VarSymbol label = (VarSymbol)TreeInfo.symbol((JCExpression) c.labels.head);
                    pat = map.forConstant(label);
                }
                newCases.append(make.Case(JCCase.STATEMENT, List.of(pat), c.stats, null));
            } else {
                newCases.append(c);
            }
        }
        JCTree enumSwitch;
        if (tree.hasTag(SWITCH)) {
            enumSwitch = make.Switch(newSelector, newCases.toList());
        } else if (tree.hasTag(SWITCH_EXPRESSION)) {
            enumSwitch = make.SwitchExpression(newSelector, newCases.toList());
            enumSwitch.setType(tree.type);
        } else {
            Assert.error();
            throw new AssertionError();
        }
        patchTargets(enumSwitch, tree, enumSwitch);
        return enumSwitch;
    }

    public JCTree visitStringSwitch(JCTree tree, JCExpression selector, List<JCCase> caseList) {
        int alternatives = caseList.size();

        if (alternatives == 0) { // Strange but legal possibility (only legal for switch statement)
            return make.at(tree.pos()).Exec(attr.makeNullCheck(selector));
        } else {
            /*
             * The general approach used is to translate a single
             * string switch statement into a series of two chained
             * switch statements: the first a synthesized statement
             * switching on the argument string's hash value and
             * computing a string's position in the list of original
             * case labels, if any, followed by a second switch on the
             * computed integer value.  The second switch has the same
             * code structure as the original string switch statement
             * except that the string case labels are replaced with
             * positional integer constants starting at 0.
             *
             * The first switch statement can be thought of as an
             * inlined map from strings to their position in the case
             * label list.  An alternate implementation would use an
             * actual Map for this purpose, as done for enum switches.
             *
             * With some additional effort, it would be possible to
             * use a single switch statement on the hash code of the
             * argument, but care would need to be taken to preserve
             * the proper control flow in the presence of hash
             * collisions and other complications, such as
             * fallthroughs.  Switch statements with one or two
             * alternatives could also be specially translated into
             * if-then statements to omit the computation of the hash
             * code.
             *
             * The generated code assumes that the hashing algorithm
             * of String is the same in the compilation environment as
             * in the environment the code will run in.  The string
             * hashing algorithm in the SE JDK has been unchanged
             * since at least JDK 1.2.  Since the algorithm has been
             * specified since that release as well, it is very
             * unlikely to be changed in the future.
             *
             * Different hashing algorithms, such as the length of the
             * strings or a perfect hashing algorithm over the
             * particular set of case labels, could potentially be
             * used instead of String.hashCode.
             */

            ListBuffer<JCStatement> stmtList = new ListBuffer<>();

            // Map from String case labels to their original position in
            // the list of case labels.
            Map<String, Integer> caseLabelToPosition = new LinkedHashMap<>(alternatives + 1, 1.0f);

            // Map of hash codes to the string case labels having that hashCode.
            Map<Integer, Set<String>> hashToString = new LinkedHashMap<>(alternatives + 1, 1.0f);

            int casePosition = 0;
            JCCase nullCase = null;
            int nullCaseLabel = -1;

            for(JCCase oneCase : caseList) {
                if (oneCase.labels.head.isExpression()) {
                    if (TreeInfo.isNull(oneCase.labels.head)) {
                        nullCase = oneCase;
                        nullCaseLabel = casePosition;
                    } else {
                        JCExpression expression = (JCExpression) oneCase.labels.head;
                        String labelExpr = (String) expression.type.constValue();
                        Integer mapping = caseLabelToPosition.put(labelExpr, casePosition);
                        Assert.checkNull(mapping);
                        int hashCode = labelExpr.hashCode();

                        Set<String> stringSet = hashToString.get(hashCode);
                        if (stringSet == null) {
                            stringSet = new LinkedHashSet<>(1, 1.0f);
                            stringSet.add(labelExpr);
                            hashToString.put(hashCode, stringSet);
                        } else {
                            boolean added = stringSet.add(labelExpr);
                            Assert.check(added);
                        }
                    }
                }
                casePosition++;
            }

            // Synthesize a switch statement that has the effect of
            // mapping from a string to the integer position of that
            // string in the list of case labels.  This is done by
            // switching on the hashCode of the string followed by an
            // if-then-else chain comparing the input for equality
            // with all the case labels having that hash value.

            /*
             * s$ = top of stack;
             * tmp$ = -1;
             * switch($s.hashCode()) {
             *     case caseLabel.hashCode:
             *         if (s$.equals("caseLabel_1")
             *           tmp$ = caseLabelToPosition("caseLabel_1");
             *         else if (s$.equals("caseLabel_2"))
             *           tmp$ = caseLabelToPosition("caseLabel_2");
             *         ...
             *         break;
             * ...
             * }
             */

            VarSymbol dollar_s = new VarSymbol(FINAL|SYNTHETIC,
                                               names.fromString("s" + tree.pos + target.syntheticNameChar()),
                                               syms.stringType,
                                               currentMethodSym);
            stmtList.append(make.at(tree.pos()).VarDef(dollar_s, selector).setType(dollar_s.type));

            VarSymbol dollar_tmp = new VarSymbol(SYNTHETIC,
                                                 names.fromString("tmp" + tree.pos + target.syntheticNameChar()),
                                                 syms.intType,
                                                 currentMethodSym);
            JCVariableDecl dollar_tmp_def =
                (JCVariableDecl)make.VarDef(dollar_tmp, make.Literal(INT, -1)).setType(dollar_tmp.type);
            dollar_tmp_def.init.type = dollar_tmp.type = syms.intType;
            stmtList.append(dollar_tmp_def);
            ListBuffer<JCCase> caseBuffer = new ListBuffer<>();
            // hashCode will trigger nullcheck on original switch expression
            JCMethodInvocation hashCodeCall = makeCall(make.Ident(dollar_s),
                                                       names.hashCode,
                                                       List.nil()).setType(syms.intType);
            JCSwitch switch1 = make.Switch(hashCodeCall,
                                        caseBuffer.toList());
            for(Map.Entry<Integer, Set<String>> entry : hashToString.entrySet()) {
                int hashCode = entry.getKey();
                Set<String> stringsWithHashCode = entry.getValue();
                Assert.check(stringsWithHashCode.size() >= 1);

                JCStatement elsepart = null;
                for(String caseLabel : stringsWithHashCode ) {
                    JCMethodInvocation stringEqualsCall = makeCall(make.Ident(dollar_s),
                                                                   names.equals,
                                                                   List.of(make.Literal(caseLabel)));
                    elsepart = make.If(stringEqualsCall,
                                       make.Exec(make.Assign(make.Ident(dollar_tmp),
                                                             make.Literal(caseLabelToPosition.get(caseLabel))).
                                                 setType(dollar_tmp.type)),
                                       elsepart);
                }

                ListBuffer<JCStatement> lb = new ListBuffer<>();
                JCBreak breakStmt = make.Break(null);
                breakStmt.target = switch1;
                lb.append(elsepart).append(breakStmt);

                caseBuffer.append(make.Case(JCCase.STATEMENT, List.of(make.Literal(hashCode)), lb.toList(), null));
            }

            switch1.cases = caseBuffer.toList();

            if (nullCase != null) {
                stmtList.append(make.If(makeBinary(NE, make.Ident(dollar_s), makeNull()), switch1, make.Exec(make.Assign(make.Ident(dollar_tmp),
                                                             make.Literal(nullCaseLabel)).
                                                 setType(dollar_tmp.type))).setType(syms.intType));
            } else {
                stmtList.append(switch1);
            }

            // Make isomorphic switch tree replacing string labels
            // with corresponding integer ones from the label to
            // position map.

            ListBuffer<JCCase> lb = new ListBuffer<>();
            for(JCCase oneCase : caseList ) {
                boolean isDefault = !oneCase.labels.head.isExpression();
                JCCaseLabel caseExpr;
                if (isDefault)
                    caseExpr = null;
                else if (oneCase == nullCase) {
                    caseExpr = make.Literal(nullCaseLabel);
                } else {
                    caseExpr = make.Literal(caseLabelToPosition.get((String)TreeInfo.skipParens((JCExpression) oneCase.labels.head).
                                                                    type.constValue()));
                }

                lb.append(make.Case(JCCase.STATEMENT, caseExpr == null ? List.of(make.DefaultCaseLabel()) : List.of(caseExpr),
                                    oneCase.stats, null));
            }

            if (tree.hasTag(SWITCH)) {
                JCSwitch switch2 = make.Switch(make.Ident(dollar_tmp), lb.toList());
                // Rewire up old unlabeled break statements to the
                // replacement switch being created.
                patchTargets(switch2, tree, switch2);

                stmtList.append(switch2);

                JCBlock res = make.Block(0L, stmtList.toList());
                res.endpos = TreeInfo.endPos(tree);
                return res;
            } else {
                JCSwitchExpression switch2 = make.SwitchExpression(make.Ident(dollar_tmp), lb.toList());

                // Rewire up old unlabeled break statements to the
                // replacement switch being created.
                patchTargets(switch2, tree, switch2);

                switch2.setType(tree.type);

                LetExpr res = make.LetExpr(stmtList.toList(), switch2);

                res.needsCond = true;
                res.setType(tree.type);

                return res;
            }
        }
    }

    private JCTree visitBoxedPrimitiveSwitch(JCTree tree, JCExpression selector, List<JCCase> cases) {
        JCExpression newSelector;

        if (cases.stream().anyMatch(c -> TreeInfo.isNull(c.labels.head))) {
            //a switch over a boxed primitive, with a null case. Pick two constants that are
            //not used by any branch in the case (c1 and c2), close to other constants that are
            //used in the switch. Then do:
            //switch ($selector != null ? $selector != c1 ? $selector : c2 : c1) {...}
            //replacing case null with case c1
            Set<Integer> constants = new LinkedHashSet<>();
            JCCase nullCase = null;

            for (JCCase c : cases) {
                if (TreeInfo.isNull(c.labels.head)) {
                    nullCase = c;
                } else if (!c.labels.head.hasTag(DEFAULTCASELABEL)) {
                    constants.add((int) c.labels.head.type.constValue());
                }
            }

            Assert.checkNonNull(nullCase);

            int nullValue = constants.isEmpty() ? 0 : constants.iterator().next();

            while (constants.contains(nullValue)) nullValue++;

            constants.add(nullValue);
            nullCase.labels.head = makeLit(syms.intType, nullValue);

            int replacementValue = nullValue;

            while (constants.contains(replacementValue)) replacementValue++;

            VarSymbol dollar_s = new VarSymbol(FINAL|SYNTHETIC,
                                               names.fromString("s" + tree.pos + this.target.syntheticNameChar()),
                                               selector.type,
                                               currentMethodSym);
            JCStatement var = make.at(tree.pos()).VarDef(dollar_s, selector).setType(dollar_s.type);
            JCExpression nullValueReplacement =
                    make.Conditional(makeBinary(NE,
                                                 unbox(make.Ident(dollar_s), syms.intType),
                                                 makeLit(syms.intType, nullValue)),
                                     unbox(make.Ident(dollar_s), syms.intType),
                                     makeLit(syms.intType, replacementValue))
                        .setType(syms.intType);
            JCExpression nullCheck =
                    make.Conditional(makeBinary(NE, make.Ident(dollar_s), makeNull()),
                                     nullValueReplacement,
                                     makeLit(syms.intType, nullValue))
                        .setType(syms.intType);
            newSelector = make.LetExpr(List.of(var), nullCheck).setType(syms.intType);
        } else {
            newSelector = unbox(selector, syms.intType);
        }

        if (tree.hasTag(SWITCH)) {
            ((JCSwitch) tree).selector = newSelector;
        } else {
            ((JCSwitchExpression) tree).selector = newSelector;
        }

        return tree;
    }

    @Override
    public void visitBreak(JCBreak tree) {
        result = tree;
    }

    @Override
    public void visitYield(JCYield tree) {
        tree.value = translate(tree.value, tree.target.type);
        result = tree;
    }

    public void visitNewArray(JCNewArray tree) {
        tree.elemtype = translate(tree.elemtype);
        for (List<JCExpression> t = tree.dims; t.tail != null; t = t.tail)
            if (t.head != null) t.head = translate(t.head, syms.intType);
        tree.elems = translate(tree.elems, types.elemtype(tree.type));
        result = tree;
    }

    public void visitSelect(JCFieldAccess tree) {
        // need to special case-access of the form C.super.x
        // these will always need an access method, unless C
        // is a default interface subclassed by the current class.
        boolean qualifiedSuperAccess =
            tree.selected.hasTag(SELECT) &&
            TreeInfo.name(tree.selected) == names._super &&
            !types.isDirectSuperInterface(((JCFieldAccess)tree.selected).selected.type.tsym, currentClass);
        tree.selected = translate(tree.selected);
        if (tree.name == names._class) {
            result = classOf(tree.selected);
        }
        else if (tree.name == names._super &&
                types.isDirectSuperInterface(tree.selected.type.tsym, currentClass)) {
            //default super call!! Not a classic qualified super call
            TypeSymbol supSym = tree.selected.type.tsym;
            Assert.checkNonNull(types.asSuper(currentClass.type, supSym));
            result = tree;
        }
        else if (tree.name == names._this || tree.name == names._super) {
            result = makeThis(tree.pos(), tree.selected.type.tsym);
        }
        else
            result = access(tree.sym, tree, enclOp, qualifiedSuperAccess);
    }

    public void visitLetExpr(LetExpr tree) {
        tree.defs = translate(tree.defs);
        tree.expr = translate(tree.expr, tree.type);
        result = tree;
    }

    // There ought to be nothing to rewrite here;
    // we don't generate code.
    public void visitAnnotation(JCAnnotation tree) {
        result = tree;
    }

    @Override
    public void visitTry(JCTry tree) {
        if (tree.resources.nonEmpty()) {
            result = makeTwrTry(tree);
            return;
        }

        boolean hasBody = tree.body.getStatements().nonEmpty();
        boolean hasCatchers = tree.catchers.nonEmpty();
        boolean hasFinally = tree.finalizer != null &&
                tree.finalizer.getStatements().nonEmpty();

        if (!hasCatchers && !hasFinally) {
            result = translate(tree.body);
            return;
        }

        if (!hasBody) {
            if (hasFinally) {
                result = translate(tree.finalizer);
            } else {
                result = translate(tree.body);
            }
            return;
        }

        // no optimizations possible
        super.visitTry(tree);
    }

/**************************************************************************
 * main method
 *************************************************************************/

    /** Translate a toplevel class and return a list consisting of
     *  the translated class and translated versions of all inner classes.
     *  @param env   The attribution environment current at the class definition.
     *               We need this for resolving some additional symbols.
     *  @param cdef  The tree representing the class definition.
     */
    public List<JCTree> translateTopLevelClass(Env<AttrContext> env, JCTree cdef, TreeMaker make) {
        ListBuffer<JCTree> translated = null;
        try {
            attrEnv = env;
            this.make = make;
            endPosTable = env.toplevel.endPositions;
            currentClass = null;
            currentMethodDef = null;
            outermostClassDef = (cdef.hasTag(CLASSDEF)) ? (JCClassDecl)cdef : null;
            outermostMemberDef = null;
            this.translated = new ListBuffer<>();
            classdefs = new HashMap<>();
            actualSymbols = new HashMap<>();
            freevarCache = new HashMap<>();
            proxies = new HashMap<>();
            twrVars = WriteableScope.create(syms.noSymbol);
            outerThisStack = List.nil();
            accessNums = new HashMap<>();
            accessSyms = new HashMap<>();
            accessConstrs = new HashMap<>();
            accessConstrTags = List.nil();
            accessed = new ListBuffer<>();
            translate(cdef, (JCExpression)null);
            for (List<Symbol> l = accessed.toList(); l.nonEmpty(); l = l.tail)
                makeAccessible(l.head);
            for (EnumMapping map : enumSwitchMap.values())
                map.translate();
            checkConflicts(this.translated.toList());
            checkAccessConstructorTags();
            translated = this.translated;
        } finally {
            // note that recursive invocations of this method fail hard
            attrEnv = null;
            this.make = null;
            endPosTable = null;
            currentClass = null;
            currentMethodDef = null;
            outermostClassDef = null;
            outermostMemberDef = null;
            this.translated = null;
            classdefs = null;
            actualSymbols = null;
            freevarCache = null;
            proxies = null;
            outerThisStack = null;
            accessNums = null;
            accessSyms = null;
            accessConstrs = null;
            accessConstrTags = null;
            accessed = null;
            enumSwitchMap.clear();
            assertionsDisabledClassCache = null;
        }
        return translated.toList();
    }
}
