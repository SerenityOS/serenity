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

package com.sun.tools.javac.jvm;

import java.util.HashMap;
import java.util.Map;

import com.sun.tools.javac.jvm.PoolConstant.LoadableConstant;
import com.sun.tools.javac.tree.TreeInfo.PosKind;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Attribute.TypeCompound;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.comp.*;
import com.sun.tools.javac.tree.*;

import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Type.*;
import com.sun.tools.javac.jvm.Code.*;
import com.sun.tools.javac.jvm.Items.*;
import com.sun.tools.javac.resources.CompilerProperties.Errors;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree.*;

import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.Kinds.Kind.*;
import static com.sun.tools.javac.code.TypeTag.*;
import static com.sun.tools.javac.jvm.ByteCodes.*;
import static com.sun.tools.javac.jvm.CRTFlags.*;
import static com.sun.tools.javac.main.Option.*;
import static com.sun.tools.javac.tree.JCTree.Tag.*;

/** This pass maps flat Java (i.e. without inner classes) to bytecodes.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Gen extends JCTree.Visitor {
    protected static final Context.Key<Gen> genKey = new Context.Key<>();

    private final Log log;
    private final Symtab syms;
    private final Check chk;
    private final Resolve rs;
    private final TreeMaker make;
    private final Names names;
    private final Target target;
    private final Name accessDollar;
    private final Types types;
    private final Lower lower;
    private final Annotate annotate;
    private final StringConcat concat;

    /** Format of stackmap tables to be generated. */
    private final Code.StackMapFormat stackMap;

    /** A type that serves as the expected type for all method expressions.
     */
    private final Type methodType;

    public static Gen instance(Context context) {
        Gen instance = context.get(genKey);
        if (instance == null)
            instance = new Gen(context);
        return instance;
    }

    /** Constant pool writer, set by genClass.
     */
    final PoolWriter poolWriter;

    protected Gen(Context context) {
        context.put(genKey, this);

        names = Names.instance(context);
        log = Log.instance(context);
        syms = Symtab.instance(context);
        chk = Check.instance(context);
        rs = Resolve.instance(context);
        make = TreeMaker.instance(context);
        target = Target.instance(context);
        types = Types.instance(context);
        concat = StringConcat.instance(context);

        methodType = new MethodType(null, null, null, syms.methodClass);
        accessDollar = names.
            fromString("access" + target.syntheticNameChar());
        lower = Lower.instance(context);

        Options options = Options.instance(context);
        lineDebugInfo =
            options.isUnset(G_CUSTOM) ||
            options.isSet(G_CUSTOM, "lines");
        varDebugInfo =
            options.isUnset(G_CUSTOM)
            ? options.isSet(G)
            : options.isSet(G_CUSTOM, "vars");
        genCrt = options.isSet(XJCOV);
        debugCode = options.isSet("debug.code");
        disableVirtualizedPrivateInvoke = options.isSet("disableVirtualizedPrivateInvoke");
        poolWriter = new PoolWriter(types, names);

        // ignore cldc because we cannot have both stackmap formats
        this.stackMap = StackMapFormat.JSR202;
        annotate = Annotate.instance(context);
        qualifiedSymbolCache = new HashMap<>();
    }

    /** Switches
     */
    private final boolean lineDebugInfo;
    private final boolean varDebugInfo;
    private final boolean genCrt;
    private final boolean debugCode;
    private boolean disableVirtualizedPrivateInvoke;

    /** Code buffer, set by genMethod.
     */
    private Code code;

    /** Items structure, set by genMethod.
     */
    private Items items;

    /** Environment for symbol lookup, set by genClass
     */
    private Env<AttrContext> attrEnv;

    /** The top level tree.
     */
    private JCCompilationUnit toplevel;

    /** The number of code-gen errors in this class.
     */
    private int nerrs = 0;

    /** An object containing mappings of syntax trees to their
     *  ending source positions.
     */
    EndPosTable endPosTable;

    boolean inCondSwitchExpression;
    Chain switchExpressionTrueChain;
    Chain switchExpressionFalseChain;
    List<LocalItem> stackBeforeSwitchExpression;
    LocalItem switchResult;

    /** Cache the symbol to reflect the qualifying type.
     *  key: corresponding type
     *  value: qualified symbol
     */
    Map<Type, Symbol> qualifiedSymbolCache;

    /** Generate code to load an integer constant.
     *  @param n     The integer to be loaded.
     */
    void loadIntConst(int n) {
        items.makeImmediateItem(syms.intType, n).load();
    }

    /** The opcode that loads a zero constant of a given type code.
     *  @param tc   The given type code (@see ByteCode).
     */
    public static int zero(int tc) {
        switch(tc) {
        case INTcode: case BYTEcode: case SHORTcode: case CHARcode:
            return iconst_0;
        case LONGcode:
            return lconst_0;
        case FLOATcode:
            return fconst_0;
        case DOUBLEcode:
            return dconst_0;
        default:
            throw new AssertionError("zero");
        }
    }

    /** The opcode that loads a one constant of a given type code.
     *  @param tc   The given type code (@see ByteCode).
     */
    public static int one(int tc) {
        return zero(tc) + 1;
    }

    /** Generate code to load -1 of the given type code (either int or long).
     *  @param tc   The given type code (@see ByteCode).
     */
    void emitMinusOne(int tc) {
        if (tc == LONGcode) {
            items.makeImmediateItem(syms.longType, Long.valueOf(-1)).load();
        } else {
            code.emitop0(iconst_m1);
        }
    }

    /** Construct a symbol to reflect the qualifying type that should
     *  appear in the byte code as per JLS 13.1.
     *
     *  For {@literal target >= 1.2}: Clone a method with the qualifier as owner (except
     *  for those cases where we need to work around VM bugs).
     *
     *  For {@literal target <= 1.1}: If qualified variable or method is defined in a
     *  non-accessible class, clone it with the qualifier class as owner.
     *
     *  @param sym    The accessed symbol
     *  @param site   The qualifier's type.
     */
    Symbol binaryQualifier(Symbol sym, Type site) {

        if (site.hasTag(ARRAY)) {
            if (sym == syms.lengthVar ||
                sym.owner != syms.arrayClass)
                return sym;
            // array clone can be qualified by the array type in later targets
            Symbol qualifier;
            if ((qualifier = qualifiedSymbolCache.get(site)) == null) {
                qualifier = new ClassSymbol(Flags.PUBLIC, site.tsym.name, site, syms.noSymbol);
                qualifiedSymbolCache.put(site, qualifier);
            }
            return sym.clone(qualifier);
        }

        if (sym.owner == site.tsym ||
            (sym.flags() & (STATIC | SYNTHETIC)) == (STATIC | SYNTHETIC)) {
            return sym;
        }

        // leave alone methods inherited from Object
        // JLS 13.1.
        if (sym.owner == syms.objectType.tsym)
            return sym;

        return sym.clone(site.tsym);
    }

    /** Insert a reference to given type in the constant pool,
     *  checking for an array with too many dimensions;
     *  return the reference's index.
     *  @param type   The type for which a reference is inserted.
     */
    int makeRef(DiagnosticPosition pos, Type type) {
        return poolWriter.putClass(checkDimension(pos, type));
    }

    /** Check if the given type is an array with too many dimensions.
     */
    private Type checkDimension(DiagnosticPosition pos, Type t) {
        checkDimensionInternal(pos, t);
        return t;
    }

    private void checkDimensionInternal(DiagnosticPosition pos, Type t) {
        switch (t.getTag()) {
        case METHOD:
            checkDimension(pos, t.getReturnType());
            for (List<Type> args = t.getParameterTypes(); args.nonEmpty(); args = args.tail)
                checkDimension(pos, args.head);
            break;
        case ARRAY:
            if (types.dimensions(t) > ClassFile.MAX_DIMENSIONS) {
                log.error(pos, Errors.LimitDimensions);
                nerrs++;
            }
            break;
        default:
            break;
        }
    }

    /** Create a temporary variable.
     *  @param type   The variable's type.
     */
    LocalItem makeTemp(Type type) {
        VarSymbol v = new VarSymbol(Flags.SYNTHETIC,
                                    names.empty,
                                    type,
                                    env.enclMethod.sym);
        code.newLocal(v);
        return items.makeLocalItem(v);
    }

    /** Generate code to call a non-private method or constructor.
     *  @param pos         Position to be used for error reporting.
     *  @param site        The type of which the method is a member.
     *  @param name        The method's name.
     *  @param argtypes    The method's argument types.
     *  @param isStatic    A flag that indicates whether we call a
     *                     static or instance method.
     */
    void callMethod(DiagnosticPosition pos,
                    Type site, Name name, List<Type> argtypes,
                    boolean isStatic) {
        Symbol msym = rs.
            resolveInternalMethod(pos, attrEnv, site, name, argtypes, null);
        if (isStatic) items.makeStaticItem(msym).invoke();
        else items.makeMemberItem(msym, name == names.init).invoke();
    }

    /** Is the given method definition an access method
     *  resulting from a qualified super? This is signified by an odd
     *  access code.
     */
    private boolean isAccessSuper(JCMethodDecl enclMethod) {
        return
            (enclMethod.mods.flags & SYNTHETIC) != 0 &&
            isOddAccessName(enclMethod.name);
    }

    /** Does given name start with "access$" and end in an odd digit?
     */
    private boolean isOddAccessName(Name name) {
        return
            name.startsWith(accessDollar) &&
            (name.getByteAt(name.getByteLength() - 1) & 1) == 1;
    }

/* ************************************************************************
 * Non-local exits
 *************************************************************************/

    /** Generate code to invoke the finalizer associated with given
     *  environment.
     *  Any calls to finalizers are appended to the environments `cont' chain.
     *  Mark beginning of gap in catch all range for finalizer.
     */
    void genFinalizer(Env<GenContext> env) {
        if (code.isAlive() && env.info.finalize != null)
            env.info.finalize.gen();
    }

    /** Generate code to call all finalizers of structures aborted by
     *  a non-local
     *  exit.  Return target environment of the non-local exit.
     *  @param target      The tree representing the structure that's aborted
     *  @param env         The environment current at the non-local exit.
     */
    Env<GenContext> unwind(JCTree target, Env<GenContext> env) {
        Env<GenContext> env1 = env;
        while (true) {
            genFinalizer(env1);
            if (env1.tree == target) break;
            env1 = env1.next;
        }
        return env1;
    }

    /** Mark end of gap in catch-all range for finalizer.
     *  @param env   the environment which might contain the finalizer
     *               (if it does, env.info.gaps != null).
     */
    void endFinalizerGap(Env<GenContext> env) {
        if (env.info.gaps != null && env.info.gaps.length() % 2 == 1)
            env.info.gaps.append(code.curCP());
    }

    /** Mark end of all gaps in catch-all ranges for finalizers of environments
     *  lying between, and including to two environments.
     *  @param from    the most deeply nested environment to mark
     *  @param to      the least deeply nested environment to mark
     */
    void endFinalizerGaps(Env<GenContext> from, Env<GenContext> to) {
        Env<GenContext> last = null;
        while (last != to) {
            endFinalizerGap(from);
            last = from;
            from = from.next;
        }
    }

    /** Do any of the structures aborted by a non-local exit have
     *  finalizers that require an empty stack?
     *  @param target      The tree representing the structure that's aborted
     *  @param env         The environment current at the non-local exit.
     */
    boolean hasFinally(JCTree target, Env<GenContext> env) {
        while (env.tree != target) {
            if (env.tree.hasTag(TRY) && env.info.finalize.hasFinalizer())
                return true;
            env = env.next;
        }
        return false;
    }

/* ************************************************************************
 * Normalizing class-members.
 *************************************************************************/

    /** Distribute member initializer code into constructors and {@code <clinit>}
     *  method.
     *  @param defs         The list of class member declarations.
     *  @param c            The enclosing class.
     */
    List<JCTree> normalizeDefs(List<JCTree> defs, ClassSymbol c) {
        ListBuffer<JCStatement> initCode = new ListBuffer<>();
        ListBuffer<Attribute.TypeCompound> initTAs = new ListBuffer<>();
        ListBuffer<JCStatement> clinitCode = new ListBuffer<>();
        ListBuffer<Attribute.TypeCompound> clinitTAs = new ListBuffer<>();
        ListBuffer<JCTree> methodDefs = new ListBuffer<>();
        // Sort definitions into three listbuffers:
        //  - initCode for instance initializers
        //  - clinitCode for class initializers
        //  - methodDefs for method definitions
        for (List<JCTree> l = defs; l.nonEmpty(); l = l.tail) {
            JCTree def = l.head;
            switch (def.getTag()) {
            case BLOCK:
                JCBlock block = (JCBlock)def;
                if ((block.flags & STATIC) != 0)
                    clinitCode.append(block);
                else if ((block.flags & SYNTHETIC) == 0)
                    initCode.append(block);
                break;
            case METHODDEF:
                methodDefs.append(def);
                break;
            case VARDEF:
                JCVariableDecl vdef = (JCVariableDecl) def;
                VarSymbol sym = vdef.sym;
                checkDimension(vdef.pos(), sym.type);
                if (vdef.init != null) {
                    if ((sym.flags() & STATIC) == 0) {
                        // Always initialize instance variables.
                        JCStatement init = make.at(vdef.pos()).
                            Assignment(sym, vdef.init);
                        initCode.append(init);
                        endPosTable.replaceTree(vdef, init);
                        initTAs.addAll(getAndRemoveNonFieldTAs(sym));
                    } else if (sym.getConstValue() == null) {
                        // Initialize class (static) variables only if
                        // they are not compile-time constants.
                        JCStatement init = make.at(vdef.pos).
                            Assignment(sym, vdef.init);
                        clinitCode.append(init);
                        endPosTable.replaceTree(vdef, init);
                        clinitTAs.addAll(getAndRemoveNonFieldTAs(sym));
                    } else {
                        checkStringConstant(vdef.init.pos(), sym.getConstValue());
                        /* if the init contains a reference to an external class, add it to the
                         * constant's pool
                         */
                        vdef.init.accept(classReferenceVisitor);
                    }
                }
                break;
            default:
                Assert.error();
            }
        }
        // Insert any instance initializers into all constructors.
        if (initCode.length() != 0) {
            List<JCStatement> inits = initCode.toList();
            initTAs.addAll(c.getInitTypeAttributes());
            List<Attribute.TypeCompound> initTAlist = initTAs.toList();
            for (JCTree t : methodDefs) {
                normalizeMethod((JCMethodDecl)t, inits, initTAlist);
            }
        }
        // If there are class initializers, create a <clinit> method
        // that contains them as its body.
        if (clinitCode.length() != 0) {
            MethodSymbol clinit = new MethodSymbol(
                STATIC | (c.flags() & STRICTFP),
                names.clinit,
                new MethodType(
                    List.nil(), syms.voidType,
                    List.nil(), syms.methodClass),
                c);
            c.members().enter(clinit);
            List<JCStatement> clinitStats = clinitCode.toList();
            JCBlock block = make.at(clinitStats.head.pos()).Block(0, clinitStats);
            block.endpos = TreeInfo.endPos(clinitStats.last());
            methodDefs.append(make.MethodDef(clinit, block));

            if (!clinitTAs.isEmpty())
                clinit.appendUniqueTypeAttributes(clinitTAs.toList());
            if (!c.getClassInitTypeAttributes().isEmpty())
                clinit.appendUniqueTypeAttributes(c.getClassInitTypeAttributes());
        }
        // Return all method definitions.
        return methodDefs.toList();
    }

    private List<Attribute.TypeCompound> getAndRemoveNonFieldTAs(VarSymbol sym) {
        List<TypeCompound> tas = sym.getRawTypeAttributes();
        ListBuffer<Attribute.TypeCompound> fieldTAs = new ListBuffer<>();
        ListBuffer<Attribute.TypeCompound> nonfieldTAs = new ListBuffer<>();
        for (TypeCompound ta : tas) {
            Assert.check(ta.getPosition().type != TargetType.UNKNOWN);
            if (ta.getPosition().type == TargetType.FIELD) {
                fieldTAs.add(ta);
            } else {
                nonfieldTAs.add(ta);
            }
        }
        sym.setTypeAttributes(fieldTAs.toList());
        return nonfieldTAs.toList();
    }

    /** Check a constant value and report if it is a string that is
     *  too large.
     */
    private void checkStringConstant(DiagnosticPosition pos, Object constValue) {
        if (nerrs != 0 || // only complain about a long string once
            constValue == null ||
            !(constValue instanceof String str) ||
            str.length() < PoolWriter.MAX_STRING_LENGTH)
            return;
        log.error(pos, Errors.LimitString);
        nerrs++;
    }

    /** Insert instance initializer code into initial constructor.
     *  @param md        The tree potentially representing a
     *                   constructor's definition.
     *  @param initCode  The list of instance initializer statements.
     *  @param initTAs  Type annotations from the initializer expression.
     */
    void normalizeMethod(JCMethodDecl md, List<JCStatement> initCode, List<TypeCompound> initTAs) {
        if (md.name == names.init && TreeInfo.isInitialConstructor(md)) {
            // We are seeing a constructor that does not call another
            // constructor of the same class.
            List<JCStatement> stats = md.body.stats;
            ListBuffer<JCStatement> newstats = new ListBuffer<>();

            if (stats.nonEmpty()) {
                // Copy initializers of synthetic variables generated in
                // the translation of inner classes.
                while (TreeInfo.isSyntheticInit(stats.head)) {
                    newstats.append(stats.head);
                    stats = stats.tail;
                }
                // Copy superclass constructor call
                newstats.append(stats.head);
                stats = stats.tail;
                // Copy remaining synthetic initializers.
                while (stats.nonEmpty() &&
                       TreeInfo.isSyntheticInit(stats.head)) {
                    newstats.append(stats.head);
                    stats = stats.tail;
                }
                // Now insert the initializer code.
                newstats.appendList(initCode);
                // And copy all remaining statements.
                while (stats.nonEmpty()) {
                    newstats.append(stats.head);
                    stats = stats.tail;
                }
            }
            md.body.stats = newstats.toList();
            if (md.body.endpos == Position.NOPOS)
                md.body.endpos = TreeInfo.endPos(md.body.stats.last());

            md.sym.appendUniqueTypeAttributes(initTAs);
        }
    }

/* ************************************************************************
 * Traversal methods
 *************************************************************************/

    /** Visitor argument: The current environment.
     */
    Env<GenContext> env;

    /** Visitor argument: The expected type (prototype).
     */
    Type pt;

    /** Visitor result: The item representing the computed value.
     */
    Item result;

    /** Visitor method: generate code for a definition, catching and reporting
     *  any completion failures.
     *  @param tree    The definition to be visited.
     *  @param env     The environment current at the definition.
     */
    public void genDef(JCTree tree, Env<GenContext> env) {
        Env<GenContext> prevEnv = this.env;
        try {
            this.env = env;
            tree.accept(this);
        } catch (CompletionFailure ex) {
            chk.completionError(tree.pos(), ex);
        } finally {
            this.env = prevEnv;
        }
    }

    /** Derived visitor method: check whether CharacterRangeTable
     *  should be emitted, if so, put a new entry into CRTable
     *  and call method to generate bytecode.
     *  If not, just call method to generate bytecode.
     *  @see    #genStat(JCTree, Env)
     *
     *  @param  tree     The tree to be visited.
     *  @param  env      The environment to use.
     *  @param  crtFlags The CharacterRangeTable flags
     *                   indicating type of the entry.
     */
    public void genStat(JCTree tree, Env<GenContext> env, int crtFlags) {
        if (!genCrt) {
            genStat(tree, env);
            return;
        }
        int startpc = code.curCP();
        genStat(tree, env);
        if (tree.hasTag(Tag.BLOCK)) crtFlags |= CRT_BLOCK;
        code.crt.put(tree, crtFlags, startpc, code.curCP());
    }

    /** Derived visitor method: generate code for a statement.
     */
    public void genStat(JCTree tree, Env<GenContext> env) {
        if (code.isAlive()) {
            code.statBegin(tree.pos);
            genDef(tree, env);
        } else if (env.info.isSwitch && tree.hasTag(VARDEF)) {
            // variables whose declarations are in a switch
            // can be used even if the decl is unreachable.
            code.newLocal(((JCVariableDecl) tree).sym);
        }
    }

    /** Derived visitor method: check whether CharacterRangeTable
     *  should be emitted, if so, put a new entry into CRTable
     *  and call method to generate bytecode.
     *  If not, just call method to generate bytecode.
     *  @see    #genStats(List, Env)
     *
     *  @param  trees    The list of trees to be visited.
     *  @param  env      The environment to use.
     *  @param  crtFlags The CharacterRangeTable flags
     *                   indicating type of the entry.
     */
    public void genStats(List<JCStatement> trees, Env<GenContext> env, int crtFlags) {
        if (!genCrt) {
            genStats(trees, env);
            return;
        }
        if (trees.length() == 1) {        // mark one statement with the flags
            genStat(trees.head, env, crtFlags | CRT_STATEMENT);
        } else {
            int startpc = code.curCP();
            genStats(trees, env);
            code.crt.put(trees, crtFlags, startpc, code.curCP());
        }
    }

    /** Derived visitor method: generate code for a list of statements.
     */
    public void genStats(List<? extends JCTree> trees, Env<GenContext> env) {
        for (List<? extends JCTree> l = trees; l.nonEmpty(); l = l.tail)
            genStat(l.head, env, CRT_STATEMENT);
    }

    /** Derived visitor method: check whether CharacterRangeTable
     *  should be emitted, if so, put a new entry into CRTable
     *  and call method to generate bytecode.
     *  If not, just call method to generate bytecode.
     *  @see    #genCond(JCTree,boolean)
     *
     *  @param  tree     The tree to be visited.
     *  @param  crtFlags The CharacterRangeTable flags
     *                   indicating type of the entry.
     */
    public CondItem genCond(JCTree tree, int crtFlags) {
        if (!genCrt) return genCond(tree, false);
        int startpc = code.curCP();
        CondItem item = genCond(tree, (crtFlags & CRT_FLOW_CONTROLLER) != 0);
        code.crt.put(tree, crtFlags, startpc, code.curCP());
        return item;
    }

    /** Derived visitor method: generate code for a boolean
     *  expression in a control-flow context.
     *  @param _tree         The expression to be visited.
     *  @param markBranches The flag to indicate that the condition is
     *                      a flow controller so produced conditions
     *                      should contain a proper tree to generate
     *                      CharacterRangeTable branches for them.
     */
    public CondItem genCond(JCTree _tree, boolean markBranches) {
        JCTree inner_tree = TreeInfo.skipParens(_tree);
        if (inner_tree.hasTag(CONDEXPR)) {
            JCConditional tree = (JCConditional)inner_tree;
            CondItem cond = genCond(tree.cond, CRT_FLOW_CONTROLLER);
            if (cond.isTrue()) {
                code.resolve(cond.trueJumps);
                CondItem result = genCond(tree.truepart, CRT_FLOW_TARGET);
                if (markBranches) result.tree = tree.truepart;
                return result;
            }
            if (cond.isFalse()) {
                code.resolve(cond.falseJumps);
                CondItem result = genCond(tree.falsepart, CRT_FLOW_TARGET);
                if (markBranches) result.tree = tree.falsepart;
                return result;
            }
            Chain secondJumps = cond.jumpFalse();
            code.resolve(cond.trueJumps);
            CondItem first = genCond(tree.truepart, CRT_FLOW_TARGET);
            if (markBranches) first.tree = tree.truepart;
            Chain falseJumps = first.jumpFalse();
            code.resolve(first.trueJumps);
            Chain trueJumps = code.branch(goto_);
            code.resolve(secondJumps);
            CondItem second = genCond(tree.falsepart, CRT_FLOW_TARGET);
            CondItem result = items.makeCondItem(second.opcode,
                                      Code.mergeChains(trueJumps, second.trueJumps),
                                      Code.mergeChains(falseJumps, second.falseJumps));
            if (markBranches) result.tree = tree.falsepart;
            return result;
        } else if (inner_tree.hasTag(SWITCH_EXPRESSION)) {
            code.resolvePending();

            boolean prevInCondSwitchExpression = inCondSwitchExpression;
            Chain prevSwitchExpressionTrueChain = switchExpressionTrueChain;
            Chain prevSwitchExpressionFalseChain = switchExpressionFalseChain;
            try {
                inCondSwitchExpression = true;
                switchExpressionTrueChain = null;
                switchExpressionFalseChain = null;
                try {
                    doHandleSwitchExpression((JCSwitchExpression) inner_tree);
                } catch (CompletionFailure ex) {
                    chk.completionError(_tree.pos(), ex);
                    code.state.stacksize = 1;
                }
                CondItem result = items.makeCondItem(goto_,
                                                     switchExpressionTrueChain,
                                                     switchExpressionFalseChain);
                if (markBranches) result.tree = _tree;
                return result;
            } finally {
                inCondSwitchExpression = prevInCondSwitchExpression;
                switchExpressionTrueChain = prevSwitchExpressionTrueChain;
                switchExpressionFalseChain = prevSwitchExpressionFalseChain;
            }
        } else if (inner_tree.hasTag(LETEXPR) && ((LetExpr) inner_tree).needsCond) {
            code.resolvePending();

            LetExpr tree = (LetExpr) inner_tree;
            int limit = code.nextreg;
            int prevLetExprStart = code.setLetExprStackPos(code.state.stacksize);
            try {
                genStats(tree.defs, env);
            } finally {
                code.setLetExprStackPos(prevLetExprStart);
            }
            CondItem result = genCond(tree.expr, markBranches);
            code.endScopes(limit);
            return result;
        } else {
            CondItem result = genExpr(_tree, syms.booleanType).mkCond();
            if (markBranches) result.tree = _tree;
            return result;
        }
    }

    public Code getCode() {
        return code;
    }

    public Items getItems() {
        return items;
    }

    public Env<AttrContext> getAttrEnv() {
        return attrEnv;
    }

    /** Visitor class for expressions which might be constant expressions.
     *  This class is a subset of TreeScanner. Intended to visit trees pruned by
     *  Lower as long as constant expressions looking for references to any
     *  ClassSymbol. Any such reference will be added to the constant pool so
     *  automated tools can detect class dependencies better.
     */
    class ClassReferenceVisitor extends JCTree.Visitor {

        @Override
        public void visitTree(JCTree tree) {}

        @Override
        public void visitBinary(JCBinary tree) {
            tree.lhs.accept(this);
            tree.rhs.accept(this);
        }

        @Override
        public void visitSelect(JCFieldAccess tree) {
            if (tree.selected.type.hasTag(CLASS)) {
                makeRef(tree.selected.pos(), tree.selected.type);
            }
        }

        @Override
        public void visitIdent(JCIdent tree) {
            if (tree.sym.owner instanceof ClassSymbol classSymbol) {
                poolWriter.putClass(classSymbol);
            }
        }

        @Override
        public void visitConditional(JCConditional tree) {
            tree.cond.accept(this);
            tree.truepart.accept(this);
            tree.falsepart.accept(this);
        }

        @Override
        public void visitUnary(JCUnary tree) {
            tree.arg.accept(this);
        }

        @Override
        public void visitParens(JCParens tree) {
            tree.expr.accept(this);
        }

        @Override
        public void visitTypeCast(JCTypeCast tree) {
            tree.expr.accept(this);
        }
    }

    private ClassReferenceVisitor classReferenceVisitor = new ClassReferenceVisitor();

    /** Visitor method: generate code for an expression, catching and reporting
     *  any completion failures.
     *  @param tree    The expression to be visited.
     *  @param pt      The expression's expected type (proto-type).
     */
    public Item genExpr(JCTree tree, Type pt) {
        Type prevPt = this.pt;
        try {
            if (tree.type.constValue() != null) {
                // Short circuit any expressions which are constants
                tree.accept(classReferenceVisitor);
                checkStringConstant(tree.pos(), tree.type.constValue());
                Symbol sym = TreeInfo.symbol(tree);
                if (sym != null && isConstantDynamic(sym)) {
                    result = items.makeDynamicItem(sym);
                } else {
                    result = items.makeImmediateItem(tree.type, tree.type.constValue());
                }
            } else {
                this.pt = pt;
                tree.accept(this);
            }
            return result.coerce(pt);
        } catch (CompletionFailure ex) {
            chk.completionError(tree.pos(), ex);
            code.state.stacksize = 1;
            return items.makeStackItem(pt);
        } finally {
            this.pt = prevPt;
        }
    }

    public boolean isConstantDynamic(Symbol sym) {
        return sym.kind == VAR &&
                sym instanceof DynamicVarSymbol dynamicVarSymbol &&
                dynamicVarSymbol.isDynamic();
    }

    /** Derived visitor method: generate code for a list of method arguments.
     *  @param trees    The argument expressions to be visited.
     *  @param pts      The expression's expected types (i.e. the formal parameter
     *                  types of the invoked method).
     */
    public void genArgs(List<JCExpression> trees, List<Type> pts) {
        for (List<JCExpression> l = trees; l.nonEmpty(); l = l.tail) {
            genExpr(l.head, pts.head).load();
            pts = pts.tail;
        }
        // require lists be of same length
        Assert.check(pts.isEmpty());
    }

/* ************************************************************************
 * Visitor methods for statements and definitions
 *************************************************************************/

    /** Thrown when the byte code size exceeds limit.
     */
    public static class CodeSizeOverflow extends RuntimeException {
        private static final long serialVersionUID = 0;
        public CodeSizeOverflow() {}
    }

    public void visitMethodDef(JCMethodDecl tree) {
        // Create a new local environment that points pack at method
        // definition.
        Env<GenContext> localEnv = env.dup(tree);
        localEnv.enclMethod = tree;
        // The expected type of every return statement in this method
        // is the method's return type.
        this.pt = tree.sym.erasure(types).getReturnType();

        checkDimension(tree.pos(), tree.sym.erasure(types));
        genMethod(tree, localEnv, false);
    }
//where
        /** Generate code for a method.
         *  @param tree     The tree representing the method definition.
         *  @param env      The environment current for the method body.
         *  @param fatcode  A flag that indicates whether all jumps are
         *                  within 32K.  We first invoke this method under
         *                  the assumption that fatcode == false, i.e. all
         *                  jumps are within 32K.  If this fails, fatcode
         *                  is set to true and we try again.
         */
        void genMethod(JCMethodDecl tree, Env<GenContext> env, boolean fatcode) {
            MethodSymbol meth = tree.sym;
            int extras = 0;
            // Count up extra parameters
            if (meth.isConstructor()) {
                extras++;
                if (meth.enclClass().isInner() &&
                    !meth.enclClass().isStatic()) {
                    extras++;
                }
            } else if ((tree.mods.flags & STATIC) == 0) {
                extras++;
            }
            //      System.err.println("Generating " + meth + " in " + meth.owner); //DEBUG
            if (Code.width(types.erasure(env.enclMethod.sym.type).getParameterTypes()) + extras >
                ClassFile.MAX_PARAMETERS) {
                log.error(tree.pos(), Errors.LimitParameters);
                nerrs++;
            }

            else if (tree.body != null) {
                // Create a new code structure and initialize it.
                int startpcCrt = initCode(tree, env, fatcode);

                try {
                    genStat(tree.body, env);
                } catch (CodeSizeOverflow e) {
                    // Failed due to code limit, try again with jsr/ret
                    startpcCrt = initCode(tree, env, fatcode);
                    genStat(tree.body, env);
                }

                if (code.state.stacksize != 0) {
                    log.error(tree.body.pos(), Errors.StackSimError(tree.sym));
                    throw new AssertionError();
                }

                // If last statement could complete normally, insert a
                // return at the end.
                if (code.isAlive()) {
                    code.statBegin(TreeInfo.endPos(tree.body));
                    if (env.enclMethod == null ||
                        env.enclMethod.sym.type.getReturnType().hasTag(VOID)) {
                        code.emitop0(return_);
                    } else {
                        // sometime dead code seems alive (4415991);
                        // generate a small loop instead
                        int startpc = code.entryPoint();
                        CondItem c = items.makeCondItem(goto_);
                        code.resolve(c.jumpTrue(), startpc);
                    }
                }
                if (genCrt)
                    code.crt.put(tree.body,
                                 CRT_BLOCK,
                                 startpcCrt,
                                 code.curCP());

                code.endScopes(0);

                // If we exceeded limits, panic
                if (code.checkLimits(tree.pos(), log)) {
                    nerrs++;
                    return;
                }

                // If we generated short code but got a long jump, do it again
                // with fatCode = true.
                if (!fatcode && code.fatcode) genMethod(tree, env, true);

                // Clean up
                if(stackMap == StackMapFormat.JSR202) {
                    code.lastFrame = null;
                    code.frameBeforeLast = null;
                }

                // Compress exception table
                code.compressCatchTable();

                // Fill in type annotation positions for exception parameters
                code.fillExceptionParameterPositions();
            }
        }

        private int initCode(JCMethodDecl tree, Env<GenContext> env, boolean fatcode) {
            MethodSymbol meth = tree.sym;

            // Create a new code structure.
            meth.code = code = new Code(meth,
                                        fatcode,
                                        lineDebugInfo ? toplevel.lineMap : null,
                                        varDebugInfo,
                                        stackMap,
                                        debugCode,
                                        genCrt ? new CRTable(tree, env.toplevel.endPositions)
                                               : null,
                                        syms,
                                        types,
                                        poolWriter);
            items = new Items(poolWriter, code, syms, types);
            if (code.debugCode) {
                System.err.println(meth + " for body " + tree);
            }

            // If method is not static, create a new local variable address
            // for `this'.
            if ((tree.mods.flags & STATIC) == 0) {
                Type selfType = meth.owner.type;
                if (meth.isConstructor() && selfType != syms.objectType)
                    selfType = UninitializedType.uninitializedThis(selfType);
                code.setDefined(
                        code.newLocal(
                            new VarSymbol(FINAL, names._this, selfType, meth.owner)));
            }

            // Mark all parameters as defined from the beginning of
            // the method.
            for (List<JCVariableDecl> l = tree.params; l.nonEmpty(); l = l.tail) {
                checkDimension(l.head.pos(), l.head.sym.type);
                code.setDefined(code.newLocal(l.head.sym));
            }

            // Get ready to generate code for method body.
            int startpcCrt = genCrt ? code.curCP() : 0;
            code.entryPoint();

            // Suppress initial stackmap
            code.pendingStackMap = false;

            return startpcCrt;
        }

    public void visitVarDef(JCVariableDecl tree) {
        VarSymbol v = tree.sym;
        if (tree.init != null) {
            checkStringConstant(tree.init.pos(), v.getConstValue());
            if (v.getConstValue() == null || varDebugInfo) {
                Assert.check(code.isStatementStart());
                code.newLocal(v);
                genExpr(tree.init, v.erasure(types)).load();
                items.makeLocalItem(v).store();
                Assert.check(code.isStatementStart());
            }
        } else {
            code.newLocal(v);
        }
        checkDimension(tree.pos(), v.type);
    }

    public void visitSkip(JCSkip tree) {
    }

    public void visitBlock(JCBlock tree) {
        int limit = code.nextreg;
        Env<GenContext> localEnv = env.dup(tree, new GenContext());
        genStats(tree.stats, localEnv);
        // End the scope of all block-local variables in variable info.
        if (!env.tree.hasTag(METHODDEF)) {
            code.statBegin(tree.endpos);
            code.endScopes(limit);
            code.pendingStatPos = Position.NOPOS;
        }
    }

    public void visitDoLoop(JCDoWhileLoop tree) {
        genLoop(tree, tree.body, tree.cond, List.nil(), false);
    }

    public void visitWhileLoop(JCWhileLoop tree) {
        genLoop(tree, tree.body, tree.cond, List.nil(), true);
    }

    public void visitForLoop(JCForLoop tree) {
        int limit = code.nextreg;
        genStats(tree.init, env);
        genLoop(tree, tree.body, tree.cond, tree.step, true);
        code.endScopes(limit);
    }
    //where
        /** Generate code for a loop.
         *  @param loop       The tree representing the loop.
         *  @param body       The loop's body.
         *  @param cond       The loop's controlling condition.
         *  @param step       "Step" statements to be inserted at end of
         *                    each iteration.
         *  @param testFirst  True if the loop test belongs before the body.
         */
        private void genLoop(JCStatement loop,
                             JCStatement body,
                             JCExpression cond,
                             List<JCExpressionStatement> step,
                             boolean testFirst) {
            Env<GenContext> loopEnv = env.dup(loop, new GenContext());
            int startpc = code.entryPoint();
            if (testFirst) { //while or for loop
                CondItem c;
                if (cond != null) {
                    code.statBegin(cond.pos);
                    Assert.check(code.isStatementStart());
                    c = genCond(TreeInfo.skipParens(cond), CRT_FLOW_CONTROLLER);
                } else {
                    c = items.makeCondItem(goto_);
                }
                Chain loopDone = c.jumpFalse();
                code.resolve(c.trueJumps);
                Assert.check(code.isStatementStart());
                genStat(body, loopEnv, CRT_STATEMENT | CRT_FLOW_TARGET);
                code.resolve(loopEnv.info.cont);
                genStats(step, loopEnv);
                code.resolve(code.branch(goto_), startpc);
                code.resolve(loopDone);
            } else {
                genStat(body, loopEnv, CRT_STATEMENT | CRT_FLOW_TARGET);
                code.resolve(loopEnv.info.cont);
                genStats(step, loopEnv);
                if (code.isAlive()) {
                    CondItem c;
                    if (cond != null) {
                        code.statBegin(cond.pos);
                        Assert.check(code.isStatementStart());
                        c = genCond(TreeInfo.skipParens(cond), CRT_FLOW_CONTROLLER);
                    } else {
                        c = items.makeCondItem(goto_);
                    }
                    code.resolve(c.jumpTrue(), startpc);
                    Assert.check(code.isStatementStart());
                    code.resolve(c.falseJumps);
                }
            }
            Chain exit = loopEnv.info.exit;
            if (exit != null) {
                code.resolve(exit);
                exit.state.defined.excludeFrom(code.nextreg);
            }
        }

    public void visitForeachLoop(JCEnhancedForLoop tree) {
        throw new AssertionError(); // should have been removed by Lower.
    }

    public void visitLabelled(JCLabeledStatement tree) {
        Env<GenContext> localEnv = env.dup(tree, new GenContext());
        genStat(tree.body, localEnv, CRT_STATEMENT);
        Chain exit = localEnv.info.exit;
        if (exit != null) {
            code.resolve(exit);
            exit.state.defined.excludeFrom(code.nextreg);
        }
    }

    public void visitSwitch(JCSwitch tree) {
        handleSwitch(tree, tree.selector, tree.cases, tree.patternSwitch);
    }

    @Override
    public void visitSwitchExpression(JCSwitchExpression tree) {
        code.resolvePending();
        boolean prevInCondSwitchExpression = inCondSwitchExpression;
        try {
            inCondSwitchExpression = false;
            doHandleSwitchExpression(tree);
        } finally {
            inCondSwitchExpression = prevInCondSwitchExpression;
        }
        result = items.makeStackItem(pt);
    }

    private void doHandleSwitchExpression(JCSwitchExpression tree) {
        List<LocalItem> prevStackBeforeSwitchExpression = stackBeforeSwitchExpression;
        LocalItem prevSwitchResult = switchResult;
        int limit = code.nextreg;
        try {
            stackBeforeSwitchExpression = List.nil();
            switchResult = null;
            if (hasTry(tree)) {
                //if the switch expression contains try-catch, the catch handlers need to have
                //an empty stack. So stash whole stack to local variables, and restore it before
                //breaks:
                while (code.state.stacksize > 0) {
                    Type type = code.state.peek();
                    Name varName = names.fromString(target.syntheticNameChar() +
                                                    "stack" +
                                                    target.syntheticNameChar() +
                                                    tree.pos +
                                                    target.syntheticNameChar() +
                                                    code.state.stacksize);
                    VarSymbol var = new VarSymbol(Flags.SYNTHETIC, varName, type,
                                                  this.env.enclMethod.sym);
                    LocalItem item = items.new LocalItem(type, code.newLocal(var));
                    stackBeforeSwitchExpression = stackBeforeSwitchExpression.prepend(item);
                    item.store();
                }
                switchResult = makeTemp(tree.type);
            }
            int prevLetExprStart = code.setLetExprStackPos(code.state.stacksize);
            try {
                handleSwitch(tree, tree.selector, tree.cases, tree.patternSwitch);
            } finally {
                code.setLetExprStackPos(prevLetExprStart);
            }
        } finally {
            stackBeforeSwitchExpression = prevStackBeforeSwitchExpression;
            switchResult = prevSwitchResult;
            code.endScopes(limit);
        }
    }
    //where:
        private boolean hasTry(JCSwitchExpression tree) {
            boolean[] hasTry = new boolean[1];
            new TreeScanner() {
                @Override
                public void visitTry(JCTry tree) {
                    hasTry[0] = true;
                }

                @Override
                public void visitClassDef(JCClassDecl tree) {
                }

                @Override
                public void visitLambda(JCLambda tree) {
                }
            }.scan(tree);
            return hasTry[0];
        }

    private void handleSwitch(JCTree swtch, JCExpression selector, List<JCCase> cases,
                              boolean patternSwitch) {
        int limit = code.nextreg;
        Assert.check(!selector.type.hasTag(CLASS));
        int switchStart = patternSwitch ? code.entryPoint() : -1;
        int startpcCrt = genCrt ? code.curCP() : 0;
        Assert.check(code.isStatementStart());
        Item sel = genExpr(selector, syms.intType);
        if (cases.isEmpty()) {
            // We are seeing:  switch <sel> {}
            sel.load().drop();
            if (genCrt)
                code.crt.put(TreeInfo.skipParens(selector),
                             CRT_FLOW_CONTROLLER, startpcCrt, code.curCP());
        } else {
            // We are seeing a nonempty switch.
            sel.load();
            if (genCrt)
                code.crt.put(TreeInfo.skipParens(selector),
                             CRT_FLOW_CONTROLLER, startpcCrt, code.curCP());
            Env<GenContext> switchEnv = env.dup(swtch, new GenContext());
            switchEnv.info.isSwitch = true;

            // Compute number of labels and minimum and maximum label values.
            // For each case, store its label in an array.
            int lo = Integer.MAX_VALUE;  // minimum label.
            int hi = Integer.MIN_VALUE;  // maximum label.
            int nlabels = 0;               // number of labels.

            int[] labels = new int[cases.length()];  // the label array.
            int defaultIndex = -1;     // the index of the default clause.

            List<JCCase> l = cases;
            for (int i = 0; i < labels.length; i++) {
                if (l.head.labels.head.isExpression()) {
                    Assert.check(l.head.labels.size() == 1);
                    int val = ((Number)((JCExpression) l.head.labels.head).type.constValue()).intValue();
                    labels[i] = val;
                    if (val < lo) lo = val;
                    if (hi < val) hi = val;
                    nlabels++;
                } else {
                    Assert.check(defaultIndex == -1);
                    defaultIndex = i;
                }
                l = l.tail;
            }

            // Determine whether to issue a tableswitch or a lookupswitch
            // instruction.
            long table_space_cost = 4 + ((long) hi - lo + 1); // words
            long table_time_cost = 3; // comparisons
            long lookup_space_cost = 3 + 2 * (long) nlabels;
            long lookup_time_cost = nlabels;
            int opcode =
                nlabels > 0 &&
                table_space_cost + 3 * table_time_cost <=
                lookup_space_cost + 3 * lookup_time_cost
                ?
                tableswitch : lookupswitch;

            int startpc = code.curCP();    // the position of the selector operation
            code.emitop0(opcode);
            code.align(4);
            int tableBase = code.curCP();  // the start of the jump table
            int[] offsets = null;          // a table of offsets for a lookupswitch
            code.emit4(-1);                // leave space for default offset
            if (opcode == tableswitch) {
                code.emit4(lo);            // minimum label
                code.emit4(hi);            // maximum label
                for (long i = lo; i <= hi; i++) {  // leave space for jump table
                    code.emit4(-1);
                }
            } else {
                code.emit4(nlabels);    // number of labels
                for (int i = 0; i < nlabels; i++) {
                    code.emit4(-1); code.emit4(-1); // leave space for lookup table
                }
                offsets = new int[labels.length];
            }
            Code.State stateSwitch = code.state.dup();
            code.markDead();

            // For each case do:
            l = cases;
            for (int i = 0; i < labels.length; i++) {
                JCCase c = l.head;
                l = l.tail;

                int pc = code.entryPoint(stateSwitch);
                // Insert offset directly into code or else into the
                // offsets table.
                if (i != defaultIndex) {
                    if (opcode == tableswitch) {
                        code.put4(
                            tableBase + 4 * (labels[i] - lo + 3),
                            pc - startpc);
                    } else {
                        offsets[i] = pc - startpc;
                    }
                } else {
                    code.put4(tableBase, pc - startpc);
                }

                // Generate code for the statements in this case.
                genStats(c.stats, switchEnv, CRT_FLOW_TARGET);
            }

            if (switchEnv.info.cont != null) {
                Assert.check(patternSwitch);
                code.resolve(switchEnv.info.cont, switchStart);
            }

            // Resolve all breaks.
            Chain exit = switchEnv.info.exit;
            if  (exit != null) {
                code.resolve(exit);
                exit.state.defined.excludeFrom(limit);
            }

            // If we have not set the default offset, we do so now.
            if (code.get4(tableBase) == -1) {
                code.put4(tableBase, code.entryPoint(stateSwitch) - startpc);
            }

            if (opcode == tableswitch) {
                // Let any unfilled slots point to the default case.
                int defaultOffset = code.get4(tableBase);
                for (long i = lo; i <= hi; i++) {
                    int t = (int)(tableBase + 4 * (i - lo + 3));
                    if (code.get4(t) == -1)
                        code.put4(t, defaultOffset);
                }
            } else {
                // Sort non-default offsets and copy into lookup table.
                if (defaultIndex >= 0)
                    for (int i = defaultIndex; i < labels.length - 1; i++) {
                        labels[i] = labels[i+1];
                        offsets[i] = offsets[i+1];
                    }
                if (nlabels > 0)
                    qsort2(labels, offsets, 0, nlabels - 1);
                for (int i = 0; i < nlabels; i++) {
                    int caseidx = tableBase + 8 * (i + 1);
                    code.put4(caseidx, labels[i]);
                    code.put4(caseidx + 4, offsets[i]);
                }
            }
        }
        code.endScopes(limit);
    }
//where
        /** Sort (int) arrays of keys and values
         */
       static void qsort2(int[] keys, int[] values, int lo, int hi) {
            int i = lo;
            int j = hi;
            int pivot = keys[(i+j)/2];
            do {
                while (keys[i] < pivot) i++;
                while (pivot < keys[j]) j--;
                if (i <= j) {
                    int temp1 = keys[i];
                    keys[i] = keys[j];
                    keys[j] = temp1;
                    int temp2 = values[i];
                    values[i] = values[j];
                    values[j] = temp2;
                    i++;
                    j--;
                }
            } while (i <= j);
            if (lo < j) qsort2(keys, values, lo, j);
            if (i < hi) qsort2(keys, values, i, hi);
        }

    public void visitSynchronized(JCSynchronized tree) {
        int limit = code.nextreg;
        // Generate code to evaluate lock and save in temporary variable.
        final LocalItem lockVar = makeTemp(syms.objectType);
        Assert.check(code.isStatementStart());
        genExpr(tree.lock, tree.lock.type).load().duplicate();
        lockVar.store();

        // Generate code to enter monitor.
        code.emitop0(monitorenter);
        code.state.lock(lockVar.reg);

        // Generate code for a try statement with given body, no catch clauses
        // in a new environment with the "exit-monitor" operation as finalizer.
        final Env<GenContext> syncEnv = env.dup(tree, new GenContext());
        syncEnv.info.finalize = new GenFinalizer() {
            void gen() {
                genLast();
                Assert.check(syncEnv.info.gaps.length() % 2 == 0);
                syncEnv.info.gaps.append(code.curCP());
            }
            void genLast() {
                if (code.isAlive()) {
                    lockVar.load();
                    code.emitop0(monitorexit);
                    code.state.unlock(lockVar.reg);
                }
            }
        };
        syncEnv.info.gaps = new ListBuffer<>();
        genTry(tree.body, List.nil(), syncEnv);
        code.endScopes(limit);
    }

    public void visitTry(final JCTry tree) {
        // Generate code for a try statement with given body and catch clauses,
        // in a new environment which calls the finally block if there is one.
        final Env<GenContext> tryEnv = env.dup(tree, new GenContext());
        final Env<GenContext> oldEnv = env;
        tryEnv.info.finalize = new GenFinalizer() {
            void gen() {
                Assert.check(tryEnv.info.gaps.length() % 2 == 0);
                tryEnv.info.gaps.append(code.curCP());
                genLast();
            }
            void genLast() {
                if (tree.finalizer != null)
                    genStat(tree.finalizer, oldEnv, CRT_BLOCK);
            }
            boolean hasFinalizer() {
                return tree.finalizer != null;
            }

            @Override
            void afterBody() {
                if (tree.finalizer != null && (tree.finalizer.flags & BODY_ONLY_FINALIZE) != 0) {
                    //for body-only finally, remove the GenFinalizer after try body
                    //so that the finally is not generated to catch bodies:
                    tryEnv.info.finalize = null;
                }
            }

        };
        tryEnv.info.gaps = new ListBuffer<>();
        genTry(tree.body, tree.catchers, tryEnv);
    }
    //where
        /** Generate code for a try or synchronized statement
         *  @param body      The body of the try or synchronized statement.
         *  @param catchers  The lis of catch clauses.
         *  @param env       the environment current for the body.
         */
        void genTry(JCTree body, List<JCCatch> catchers, Env<GenContext> env) {
            int limit = code.nextreg;
            int startpc = code.curCP();
            Code.State stateTry = code.state.dup();
            genStat(body, env, CRT_BLOCK);
            int endpc = code.curCP();
            List<Integer> gaps = env.info.gaps.toList();
            code.statBegin(TreeInfo.endPos(body));
            genFinalizer(env);
            code.statBegin(TreeInfo.endPos(env.tree));
            Chain exitChain = code.branch(goto_);
            endFinalizerGap(env);
            env.info.finalize.afterBody();
            boolean hasFinalizer =
                env.info.finalize != null &&
                env.info.finalize.hasFinalizer();
            if (startpc != endpc) for (List<JCCatch> l = catchers; l.nonEmpty(); l = l.tail) {
                // start off with exception on stack
                code.entryPoint(stateTry, l.head.param.sym.type);
                genCatch(l.head, env, startpc, endpc, gaps);
                genFinalizer(env);
                if (hasFinalizer || l.tail.nonEmpty()) {
                    code.statBegin(TreeInfo.endPos(env.tree));
                    exitChain = Code.mergeChains(exitChain,
                                                 code.branch(goto_));
                }
                endFinalizerGap(env);
            }
            if (hasFinalizer) {
                // Create a new register segment to avoid allocating
                // the same variables in finalizers and other statements.
                code.newRegSegment();

                // Add a catch-all clause.

                // start off with exception on stack
                int catchallpc = code.entryPoint(stateTry, syms.throwableType);

                // Register all exception ranges for catch all clause.
                // The range of the catch all clause is from the beginning
                // of the try or synchronized block until the present
                // code pointer excluding all gaps in the current
                // environment's GenContext.
                int startseg = startpc;
                while (env.info.gaps.nonEmpty()) {
                    int endseg = env.info.gaps.next().intValue();
                    registerCatch(body.pos(), startseg, endseg,
                                  catchallpc, 0);
                    startseg = env.info.gaps.next().intValue();
                }
                code.statBegin(TreeInfo.finalizerPos(env.tree, PosKind.FIRST_STAT_POS));
                code.markStatBegin();

                Item excVar = makeTemp(syms.throwableType);
                excVar.store();
                genFinalizer(env);
                code.resolvePending();
                code.statBegin(TreeInfo.finalizerPos(env.tree, PosKind.END_POS));
                code.markStatBegin();

                excVar.load();
                registerCatch(body.pos(), startseg,
                              env.info.gaps.next().intValue(),
                              catchallpc, 0);
                code.emitop0(athrow);
                code.markDead();

                // If there are jsr's to this finalizer, ...
                if (env.info.cont != null) {
                    // Resolve all jsr's.
                    code.resolve(env.info.cont);

                    // Mark statement line number
                    code.statBegin(TreeInfo.finalizerPos(env.tree, PosKind.FIRST_STAT_POS));
                    code.markStatBegin();

                    // Save return address.
                    LocalItem retVar = makeTemp(syms.throwableType);
                    retVar.store();

                    // Generate finalizer code.
                    env.info.finalize.genLast();

                    // Return.
                    code.emitop1w(ret, retVar.reg);
                    code.markDead();
                }
            }
            // Resolve all breaks.
            code.resolve(exitChain);

            code.endScopes(limit);
        }

        /** Generate code for a catch clause.
         *  @param tree     The catch clause.
         *  @param env      The environment current in the enclosing try.
         *  @param startpc  Start pc of try-block.
         *  @param endpc    End pc of try-block.
         */
        void genCatch(JCCatch tree,
                      Env<GenContext> env,
                      int startpc, int endpc,
                      List<Integer> gaps) {
            if (startpc != endpc) {
                List<Pair<List<Attribute.TypeCompound>, JCExpression>> catchTypeExprs
                        = catchTypesWithAnnotations(tree);
                while (gaps.nonEmpty()) {
                    for (Pair<List<Attribute.TypeCompound>, JCExpression> subCatch1 : catchTypeExprs) {
                        JCExpression subCatch = subCatch1.snd;
                        int catchType = makeRef(tree.pos(), subCatch.type);
                        int end = gaps.head.intValue();
                        registerCatch(tree.pos(),
                                      startpc,  end, code.curCP(),
                                      catchType);
                        for (Attribute.TypeCompound tc :  subCatch1.fst) {
                                tc.position.setCatchInfo(catchType, startpc);
                        }
                    }
                    gaps = gaps.tail;
                    startpc = gaps.head.intValue();
                    gaps = gaps.tail;
                }
                if (startpc < endpc) {
                    for (Pair<List<Attribute.TypeCompound>, JCExpression> subCatch1 : catchTypeExprs) {
                        JCExpression subCatch = subCatch1.snd;
                        int catchType = makeRef(tree.pos(), subCatch.type);
                        registerCatch(tree.pos(),
                                      startpc, endpc, code.curCP(),
                                      catchType);
                        for (Attribute.TypeCompound tc :  subCatch1.fst) {
                            tc.position.setCatchInfo(catchType, startpc);
                        }
                    }
                }
                VarSymbol exparam = tree.param.sym;
                code.statBegin(tree.pos);
                code.markStatBegin();
                int limit = code.nextreg;
                code.newLocal(exparam);
                items.makeLocalItem(exparam).store();
                code.statBegin(TreeInfo.firstStatPos(tree.body));
                genStat(tree.body, env, CRT_BLOCK);
                code.endScopes(limit);
                code.statBegin(TreeInfo.endPos(tree.body));
            }
        }
        // where
        List<Pair<List<Attribute.TypeCompound>, JCExpression>> catchTypesWithAnnotations(JCCatch tree) {
            return TreeInfo.isMultiCatch(tree) ?
                    catchTypesWithAnnotationsFromMulticatch((JCTypeUnion)tree.param.vartype, tree.param.sym.getRawTypeAttributes()) :
                    List.of(new Pair<>(tree.param.sym.getRawTypeAttributes(), tree.param.vartype));
        }
        // where
        List<Pair<List<Attribute.TypeCompound>, JCExpression>> catchTypesWithAnnotationsFromMulticatch(JCTypeUnion tree, List<TypeCompound> first) {
            List<JCExpression> alts = tree.alternatives;
            List<Pair<List<TypeCompound>, JCExpression>> res = List.of(new Pair<>(first, alts.head));
            alts = alts.tail;

            while(alts != null && alts.head != null) {
                JCExpression alt = alts.head;
                if (alt instanceof JCAnnotatedType annotatedType) {
                    res = res.prepend(new Pair<>(annotate.fromAnnotations(annotatedType.annotations), alt));
                } else {
                    res = res.prepend(new Pair<>(List.nil(), alt));
                }
                alts = alts.tail;
            }
            return res.reverse();
        }

        /** Register a catch clause in the "Exceptions" code-attribute.
         */
        void registerCatch(DiagnosticPosition pos,
                           int startpc, int endpc,
                           int handler_pc, int catch_type) {
            char startpc1 = (char)startpc;
            char endpc1 = (char)endpc;
            char handler_pc1 = (char)handler_pc;
            if (startpc1 == startpc &&
                endpc1 == endpc &&
                handler_pc1 == handler_pc) {
                code.addCatch(startpc1, endpc1, handler_pc1,
                              (char)catch_type);
            } else {
                log.error(pos, Errors.LimitCodeTooLargeForTryStmt);
                nerrs++;
            }
        }

    public void visitIf(JCIf tree) {
        int limit = code.nextreg;
        Chain thenExit = null;
        Assert.check(code.isStatementStart());
        CondItem c = genCond(TreeInfo.skipParens(tree.cond),
                             CRT_FLOW_CONTROLLER);
        Chain elseChain = c.jumpFalse();
        Assert.check(code.isStatementStart());
        if (!c.isFalse()) {
            code.resolve(c.trueJumps);
            genStat(tree.thenpart, env, CRT_STATEMENT | CRT_FLOW_TARGET);
            thenExit = code.branch(goto_);
        }
        if (elseChain != null) {
            code.resolve(elseChain);
            if (tree.elsepart != null) {
                genStat(tree.elsepart, env,CRT_STATEMENT | CRT_FLOW_TARGET);
            }
        }
        code.resolve(thenExit);
        code.endScopes(limit);
        Assert.check(code.isStatementStart());
    }

    public void visitExec(JCExpressionStatement tree) {
        // Optimize x++ to ++x and x-- to --x.
        JCExpression e = tree.expr;
        switch (e.getTag()) {
            case POSTINC:
                ((JCUnary) e).setTag(PREINC);
                break;
            case POSTDEC:
                ((JCUnary) e).setTag(PREDEC);
                break;
        }
        Assert.check(code.isStatementStart());
        genExpr(tree.expr, tree.expr.type).drop();
        Assert.check(code.isStatementStart());
    }

    public void visitBreak(JCBreak tree) {
        Assert.check(code.isStatementStart());
        final Env<GenContext> targetEnv = unwindBreak(tree.target);
        targetEnv.info.addExit(code.branch(goto_));
        endFinalizerGaps(env, targetEnv);
    }

    public void visitYield(JCYield tree) {
        Assert.check(code.isStatementStart());
        final Env<GenContext> targetEnv;
        if (inCondSwitchExpression) {
            CondItem value = genCond(tree.value, CRT_FLOW_TARGET);
            Chain falseJumps = value.jumpFalse();

            code.resolve(value.trueJumps);
            Env<GenContext> localEnv = unwindBreak(tree.target);
            reloadStackBeforeSwitchExpr();
            Chain trueJumps = code.branch(goto_);

            endFinalizerGaps(env, localEnv);

            code.resolve(falseJumps);
            targetEnv = unwindBreak(tree.target);
            reloadStackBeforeSwitchExpr();
            falseJumps = code.branch(goto_);

            if (switchExpressionTrueChain == null) {
                switchExpressionTrueChain = trueJumps;
            } else {
                switchExpressionTrueChain =
                        Code.mergeChains(switchExpressionTrueChain, trueJumps);
            }
            if (switchExpressionFalseChain == null) {
                switchExpressionFalseChain = falseJumps;
            } else {
                switchExpressionFalseChain =
                        Code.mergeChains(switchExpressionFalseChain, falseJumps);
            }
        } else {
            genExpr(tree.value, pt).load();
            if (switchResult != null)
                switchResult.store();

            targetEnv = unwindBreak(tree.target);

            if (code.isAlive()) {
                reloadStackBeforeSwitchExpr();
                if (switchResult != null)
                    switchResult.load();

                code.state.forceStackTop(tree.target.type);
                targetEnv.info.addExit(code.branch(goto_));
                code.markDead();
            }
        }
        endFinalizerGaps(env, targetEnv);
    }
    //where:
        /** As side-effect, might mark code as dead disabling any further emission.
         */
        private Env<GenContext> unwindBreak(JCTree target) {
            int tmpPos = code.pendingStatPos;
            Env<GenContext> targetEnv = unwind(target, env);
            code.pendingStatPos = tmpPos;
            return targetEnv;
        }

        private void reloadStackBeforeSwitchExpr() {
            for (LocalItem li : stackBeforeSwitchExpression)
                li.load();
        }

    public void visitContinue(JCContinue tree) {
        int tmpPos = code.pendingStatPos;
        Env<GenContext> targetEnv = unwind(tree.target, env);
        code.pendingStatPos = tmpPos;
        Assert.check(code.isStatementStart());
        targetEnv.info.addCont(code.branch(goto_));
        endFinalizerGaps(env, targetEnv);
    }

    public void visitReturn(JCReturn tree) {
        int limit = code.nextreg;
        final Env<GenContext> targetEnv;

        /* Save and then restore the location of the return in case a finally
         * is expanded (with unwind()) in the middle of our bytecodes.
         */
        int tmpPos = code.pendingStatPos;
        if (tree.expr != null) {
            Assert.check(code.isStatementStart());
            Item r = genExpr(tree.expr, pt).load();
            if (hasFinally(env.enclMethod, env)) {
                r = makeTemp(pt);
                r.store();
            }
            targetEnv = unwind(env.enclMethod, env);
            code.pendingStatPos = tmpPos;
            r.load();
            code.emitop0(ireturn + Code.truncate(Code.typecode(pt)));
        } else {
            targetEnv = unwind(env.enclMethod, env);
            code.pendingStatPos = tmpPos;
            code.emitop0(return_);
        }
        endFinalizerGaps(env, targetEnv);
        code.endScopes(limit);
    }

    public void visitThrow(JCThrow tree) {
        Assert.check(code.isStatementStart());
        genExpr(tree.expr, tree.expr.type).load();
        code.emitop0(athrow);
        Assert.check(code.isStatementStart());
    }

/* ************************************************************************
 * Visitor methods for expressions
 *************************************************************************/

    public void visitApply(JCMethodInvocation tree) {
        setTypeAnnotationPositions(tree.pos);
        // Generate code for method.
        Item m = genExpr(tree.meth, methodType);
        // Generate code for all arguments, where the expected types are
        // the parameters of the method's external type (that is, any implicit
        // outer instance of a super(...) call appears as first parameter).
        MethodSymbol msym = (MethodSymbol)TreeInfo.symbol(tree.meth);
        genArgs(tree.args,
                msym.externalType(types).getParameterTypes());
        if (!msym.isDynamic()) {
            code.statBegin(tree.pos);
        }
        result = m.invoke();
    }

    public void visitConditional(JCConditional tree) {
        Chain thenExit = null;
        code.statBegin(tree.cond.pos);
        CondItem c = genCond(tree.cond, CRT_FLOW_CONTROLLER);
        Chain elseChain = c.jumpFalse();
        if (!c.isFalse()) {
            code.resolve(c.trueJumps);
            int startpc = genCrt ? code.curCP() : 0;
            code.statBegin(tree.truepart.pos);
            genExpr(tree.truepart, pt).load();
            code.state.forceStackTop(tree.type);
            if (genCrt) code.crt.put(tree.truepart, CRT_FLOW_TARGET,
                                     startpc, code.curCP());
            thenExit = code.branch(goto_);
        }
        if (elseChain != null) {
            code.resolve(elseChain);
            int startpc = genCrt ? code.curCP() : 0;
            code.statBegin(tree.falsepart.pos);
            genExpr(tree.falsepart, pt).load();
            code.state.forceStackTop(tree.type);
            if (genCrt) code.crt.put(tree.falsepart, CRT_FLOW_TARGET,
                                     startpc, code.curCP());
        }
        code.resolve(thenExit);
        result = items.makeStackItem(pt);
    }

    private void setTypeAnnotationPositions(int treePos) {
        MethodSymbol meth = code.meth;
        boolean initOrClinit = code.meth.getKind() == javax.lang.model.element.ElementKind.CONSTRUCTOR
                || code.meth.getKind() == javax.lang.model.element.ElementKind.STATIC_INIT;

        for (Attribute.TypeCompound ta : meth.getRawTypeAttributes()) {
            if (ta.hasUnknownPosition())
                ta.tryFixPosition();

            if (ta.position.matchesPos(treePos))
                ta.position.updatePosOffset(code.cp);
        }

        if (!initOrClinit)
            return;

        for (Attribute.TypeCompound ta : meth.owner.getRawTypeAttributes()) {
            if (ta.hasUnknownPosition())
                ta.tryFixPosition();

            if (ta.position.matchesPos(treePos))
                ta.position.updatePosOffset(code.cp);
        }

        ClassSymbol clazz = meth.enclClass();
        for (Symbol s : new com.sun.tools.javac.model.FilteredMemberList(clazz.members())) {
            if (!s.getKind().isField())
                continue;

            for (Attribute.TypeCompound ta : s.getRawTypeAttributes()) {
                if (ta.hasUnknownPosition())
                    ta.tryFixPosition();

                if (ta.position.matchesPos(treePos))
                    ta.position.updatePosOffset(code.cp);
            }
        }
    }

    public void visitNewClass(JCNewClass tree) {
        // Enclosing instances or anonymous classes should have been eliminated
        // by now.
        Assert.check(tree.encl == null && tree.def == null);
        setTypeAnnotationPositions(tree.pos);

        code.emitop2(new_, checkDimension(tree.pos(), tree.type), PoolWriter::putClass);
        code.emitop0(dup);

        // Generate code for all arguments, where the expected types are
        // the parameters of the constructor's external type (that is,
        // any implicit outer instance appears as first parameter).
        genArgs(tree.args, tree.constructor.externalType(types).getParameterTypes());

        items.makeMemberItem(tree.constructor, true).invoke();
        result = items.makeStackItem(tree.type);
    }

    public void visitNewArray(JCNewArray tree) {
        setTypeAnnotationPositions(tree.pos);

        if (tree.elems != null) {
            Type elemtype = types.elemtype(tree.type);
            loadIntConst(tree.elems.length());
            Item arr = makeNewArray(tree.pos(), tree.type, 1);
            int i = 0;
            for (List<JCExpression> l = tree.elems; l.nonEmpty(); l = l.tail) {
                arr.duplicate();
                loadIntConst(i);
                i++;
                genExpr(l.head, elemtype).load();
                items.makeIndexedItem(elemtype).store();
            }
            result = arr;
        } else {
            for (List<JCExpression> l = tree.dims; l.nonEmpty(); l = l.tail) {
                genExpr(l.head, syms.intType).load();
            }
            result = makeNewArray(tree.pos(), tree.type, tree.dims.length());
        }
    }
//where
        /** Generate code to create an array with given element type and number
         *  of dimensions.
         */
        Item makeNewArray(DiagnosticPosition pos, Type type, int ndims) {
            Type elemtype = types.elemtype(type);
            if (types.dimensions(type) > ClassFile.MAX_DIMENSIONS) {
                log.error(pos, Errors.LimitDimensions);
                nerrs++;
            }
            int elemcode = Code.arraycode(elemtype);
            if (elemcode == 0 || (elemcode == 1 && ndims == 1)) {
                code.emitAnewarray(makeRef(pos, elemtype), type);
            } else if (elemcode == 1) {
                code.emitMultianewarray(ndims, makeRef(pos, type), type);
            } else {
                code.emitNewarray(elemcode, type);
            }
            return items.makeStackItem(type);
        }

    public void visitParens(JCParens tree) {
        result = genExpr(tree.expr, tree.expr.type);
    }

    public void visitAssign(JCAssign tree) {
        Item l = genExpr(tree.lhs, tree.lhs.type);
        genExpr(tree.rhs, tree.lhs.type).load();
        if (tree.rhs.type.hasTag(BOT)) {
            /* This is just a case of widening reference conversion that per 5.1.5 simply calls
               for "regarding a reference as having some other type in a manner that can be proved
               correct at compile time."
            */
            code.state.forceStackTop(tree.lhs.type);
        }
        result = items.makeAssignItem(l);
    }

    public void visitAssignop(JCAssignOp tree) {
        OperatorSymbol operator = tree.operator;
        Item l;
        if (operator.opcode == string_add) {
            l = concat.makeConcat(tree);
        } else {
            // Generate code for first expression
            l = genExpr(tree.lhs, tree.lhs.type);

            // If we have an increment of -32768 to +32767 of a local
            // int variable we can use an incr instruction instead of
            // proceeding further.
            if ((tree.hasTag(PLUS_ASG) || tree.hasTag(MINUS_ASG)) &&
                l instanceof LocalItem localItem &&
                tree.lhs.type.getTag().isSubRangeOf(INT) &&
                tree.rhs.type.getTag().isSubRangeOf(INT) &&
                tree.rhs.type.constValue() != null) {
                int ival = ((Number) tree.rhs.type.constValue()).intValue();
                if (tree.hasTag(MINUS_ASG)) ival = -ival;
                localItem.incr(ival);
                result = l;
                return;
            }
            // Otherwise, duplicate expression, load one copy
            // and complete binary operation.
            l.duplicate();
            l.coerce(operator.type.getParameterTypes().head).load();
            completeBinop(tree.lhs, tree.rhs, operator).coerce(tree.lhs.type);
        }
        result = items.makeAssignItem(l);
    }

    public void visitUnary(JCUnary tree) {
        OperatorSymbol operator = tree.operator;
        if (tree.hasTag(NOT)) {
            CondItem od = genCond(tree.arg, false);
            result = od.negate();
        } else {
            Item od = genExpr(tree.arg, operator.type.getParameterTypes().head);
            switch (tree.getTag()) {
            case POS:
                result = od.load();
                break;
            case NEG:
                result = od.load();
                code.emitop0(operator.opcode);
                break;
            case COMPL:
                result = od.load();
                emitMinusOne(od.typecode);
                code.emitop0(operator.opcode);
                break;
            case PREINC: case PREDEC:
                od.duplicate();
                if (od instanceof LocalItem localItem &&
                    (operator.opcode == iadd || operator.opcode == isub)) {
                    localItem.incr(tree.hasTag(PREINC) ? 1 : -1);
                    result = od;
                } else {
                    od.load();
                    code.emitop0(one(od.typecode));
                    code.emitop0(operator.opcode);
                    // Perform narrowing primitive conversion if byte,
                    // char, or short.  Fix for 4304655.
                    if (od.typecode != INTcode &&
                        Code.truncate(od.typecode) == INTcode)
                      code.emitop0(int2byte + od.typecode - BYTEcode);
                    result = items.makeAssignItem(od);
                }
                break;
            case POSTINC: case POSTDEC:
                od.duplicate();
                if (od instanceof LocalItem localItem &&
                    (operator.opcode == iadd || operator.opcode == isub)) {
                    Item res = od.load();
                    localItem.incr(tree.hasTag(POSTINC) ? 1 : -1);
                    result = res;
                } else {
                    Item res = od.load();
                    od.stash(od.typecode);
                    code.emitop0(one(od.typecode));
                    code.emitop0(operator.opcode);
                    // Perform narrowing primitive conversion if byte,
                    // char, or short.  Fix for 4304655.
                    if (od.typecode != INTcode &&
                        Code.truncate(od.typecode) == INTcode)
                      code.emitop0(int2byte + od.typecode - BYTEcode);
                    od.store();
                    result = res;
                }
                break;
            case NULLCHK:
                result = od.load();
                code.emitop0(dup);
                genNullCheck(tree);
                break;
            default:
                Assert.error();
            }
        }
    }

    /** Generate a null check from the object value at stack top. */
    private void genNullCheck(JCTree tree) {
        code.statBegin(tree.pos);
        callMethod(tree.pos(), syms.objectsType, names.requireNonNull,
                   List.of(syms.objectType), true);
        code.emitop0(pop);
    }

    public void visitBinary(JCBinary tree) {
        OperatorSymbol operator = tree.operator;
        if (operator.opcode == string_add) {
            result = concat.makeConcat(tree);
        } else if (tree.hasTag(AND)) {
            CondItem lcond = genCond(tree.lhs, CRT_FLOW_CONTROLLER);
            if (!lcond.isFalse()) {
                Chain falseJumps = lcond.jumpFalse();
                code.resolve(lcond.trueJumps);
                CondItem rcond = genCond(tree.rhs, CRT_FLOW_TARGET);
                result = items.
                    makeCondItem(rcond.opcode,
                                 rcond.trueJumps,
                                 Code.mergeChains(falseJumps,
                                                  rcond.falseJumps));
            } else {
                result = lcond;
            }
        } else if (tree.hasTag(OR)) {
            CondItem lcond = genCond(tree.lhs, CRT_FLOW_CONTROLLER);
            if (!lcond.isTrue()) {
                Chain trueJumps = lcond.jumpTrue();
                code.resolve(lcond.falseJumps);
                CondItem rcond = genCond(tree.rhs, CRT_FLOW_TARGET);
                result = items.
                    makeCondItem(rcond.opcode,
                                 Code.mergeChains(trueJumps, rcond.trueJumps),
                                 rcond.falseJumps);
            } else {
                result = lcond;
            }
        } else {
            Item od = genExpr(tree.lhs, operator.type.getParameterTypes().head);
            od.load();
            result = completeBinop(tree.lhs, tree.rhs, operator);
        }
    }


        /** Complete generating code for operation, with left operand
         *  already on stack.
         *  @param lhs       The tree representing the left operand.
         *  @param rhs       The tree representing the right operand.
         *  @param operator  The operator symbol.
         */
        Item completeBinop(JCTree lhs, JCTree rhs, OperatorSymbol operator) {
            MethodType optype = (MethodType)operator.type;
            int opcode = operator.opcode;
            if (opcode >= if_icmpeq && opcode <= if_icmple &&
                    rhs.type.constValue() instanceof Number number &&
                    number.intValue() == 0) {
                opcode = opcode + (ifeq - if_icmpeq);
            } else if (opcode >= if_acmpeq && opcode <= if_acmpne &&
                       TreeInfo.isNull(rhs)) {
                opcode = opcode + (if_acmp_null - if_acmpeq);
            } else {
                // The expected type of the right operand is
                // the second parameter type of the operator, except for
                // shifts with long shiftcount, where we convert the opcode
                // to a short shift and the expected type to int.
                Type rtype = operator.erasure(types).getParameterTypes().tail.head;
                if (opcode >= ishll && opcode <= lushrl) {
                    opcode = opcode + (ishl - ishll);
                    rtype = syms.intType;
                }
                // Generate code for right operand and load.
                genExpr(rhs, rtype).load();
                // If there are two consecutive opcode instructions,
                // emit the first now.
                if (opcode >= (1 << preShift)) {
                    code.emitop0(opcode >> preShift);
                    opcode = opcode & 0xFF;
                }
            }
            if (opcode >= ifeq && opcode <= if_acmpne ||
                opcode == if_acmp_null || opcode == if_acmp_nonnull) {
                return items.makeCondItem(opcode);
            } else {
                code.emitop0(opcode);
                return items.makeStackItem(optype.restype);
            }
        }

    public void visitTypeCast(JCTypeCast tree) {
        result = genExpr(tree.expr, tree.clazz.type).load();
        setTypeAnnotationPositions(tree.pos);
        // Additional code is only needed if we cast to a reference type
        // which is not statically a supertype of the expression's type.
        // For basic types, the coerce(...) in genExpr(...) will do
        // the conversion.
        if (!tree.clazz.type.isPrimitive() &&
           !types.isSameType(tree.expr.type, tree.clazz.type) &&
           types.asSuper(tree.expr.type, tree.clazz.type.tsym) == null) {
            code.emitop2(checkcast, checkDimension(tree.pos(), tree.clazz.type), PoolWriter::putClass);
        }
    }

    public void visitWildcard(JCWildcard tree) {
        throw new AssertionError(this.getClass().getName());
    }

    public void visitTypeTest(JCInstanceOf tree) {
        genExpr(tree.expr, tree.expr.type).load();
        setTypeAnnotationPositions(tree.pos);
        code.emitop2(instanceof_, makeRef(tree.pos(), tree.pattern.type));
        result = items.makeStackItem(syms.booleanType);
    }

    public void visitIndexed(JCArrayAccess tree) {
        genExpr(tree.indexed, tree.indexed.type).load();
        genExpr(tree.index, syms.intType).load();
        result = items.makeIndexedItem(tree.type);
    }

    public void visitIdent(JCIdent tree) {
        Symbol sym = tree.sym;
        if (tree.name == names._this || tree.name == names._super) {
            Item res = tree.name == names._this
                ? items.makeThisItem()
                : items.makeSuperItem();
            if (sym.kind == MTH) {
                // Generate code to address the constructor.
                res.load();
                res = items.makeMemberItem(sym, true);
            }
            result = res;
       } else if (isInvokeDynamic(sym) || isConstantDynamic(sym)) {
            if (isConstantDynamic(sym)) {
                setTypeAnnotationPositions(tree.pos);
            }
            result = items.makeDynamicItem(sym);
        } else if (sym.kind == VAR && (sym.owner.kind == MTH || sym.owner.kind == VAR)) {
            result = items.makeLocalItem((VarSymbol)sym);
        } else if ((sym.flags() & STATIC) != 0) {
            if (!isAccessSuper(env.enclMethod))
                sym = binaryQualifier(sym, env.enclClass.type);
            result = items.makeStaticItem(sym);
        } else {
            items.makeThisItem().load();
            sym = binaryQualifier(sym, env.enclClass.type);
            result = items.makeMemberItem(sym, nonVirtualForPrivateAccess(sym));
        }
    }

    //where
    private boolean nonVirtualForPrivateAccess(Symbol sym) {
        boolean useVirtual = target.hasVirtualPrivateInvoke() &&
                             !disableVirtualizedPrivateInvoke;
        return !useVirtual && ((sym.flags() & PRIVATE) != 0);
    }

    public void visitSelect(JCFieldAccess tree) {
        Symbol sym = tree.sym;

        if (tree.name == names._class) {
            code.emitLdc((LoadableConstant)checkDimension(tree.pos(), tree.selected.type));
            result = items.makeStackItem(pt);
            return;
       }

        Symbol ssym = TreeInfo.symbol(tree.selected);

        // Are we selecting via super?
        boolean selectSuper =
            ssym != null && (ssym.kind == TYP || ssym.name == names._super);

        // Are we accessing a member of the superclass in an access method
        // resulting from a qualified super?
        boolean accessSuper = isAccessSuper(env.enclMethod);

        Item base = (selectSuper)
            ? items.makeSuperItem()
            : genExpr(tree.selected, tree.selected.type);

        if (sym.kind == VAR && ((VarSymbol) sym).getConstValue() != null) {
            // We are seeing a variable that is constant but its selecting
            // expression is not.
            if ((sym.flags() & STATIC) != 0) {
                if (!selectSuper && (ssym == null || ssym.kind != TYP))
                    base = base.load();
                base.drop();
            } else {
                base.load();
                genNullCheck(tree.selected);
            }
            result = items.
                makeImmediateItem(sym.type, ((VarSymbol) sym).getConstValue());
        } else {
            if (isInvokeDynamic(sym)) {
                result = items.makeDynamicItem(sym);
                return;
            } else {
                sym = binaryQualifier(sym, tree.selected.type);
            }
            if ((sym.flags() & STATIC) != 0) {
                if (!selectSuper && (ssym == null || ssym.kind != TYP))
                    base = base.load();
                base.drop();
                result = items.makeStaticItem(sym);
            } else {
                base.load();
                if (sym == syms.lengthVar) {
                    code.emitop0(arraylength);
                    result = items.makeStackItem(syms.intType);
                } else {
                    result = items.
                        makeMemberItem(sym,
                                       nonVirtualForPrivateAccess(sym) ||
                                       selectSuper || accessSuper);
                }
            }
        }
    }

    public boolean isInvokeDynamic(Symbol sym) {
        return sym.kind == MTH && ((MethodSymbol)sym).isDynamic();
    }

    public void visitLiteral(JCLiteral tree) {
        if (tree.type.hasTag(BOT)) {
            code.emitop0(aconst_null);
            result = items.makeStackItem(tree.type);
        }
        else
            result = items.makeImmediateItem(tree.type, tree.value);
    }

    public void visitLetExpr(LetExpr tree) {
        code.resolvePending();

        int limit = code.nextreg;
        int prevLetExprStart = code.setLetExprStackPos(code.state.stacksize);
        try {
            genStats(tree.defs, env);
        } finally {
            code.setLetExprStackPos(prevLetExprStart);
        }
        result = genExpr(tree.expr, tree.expr.type).load();
        code.endScopes(limit);
    }

    private void generateReferencesToPrunedTree(ClassSymbol classSymbol) {
        List<JCTree> prunedInfo = lower.prunedTree.get(classSymbol);
        if (prunedInfo != null) {
            for (JCTree prunedTree: prunedInfo) {
                prunedTree.accept(classReferenceVisitor);
            }
        }
    }

/* ************************************************************************
 * main method
 *************************************************************************/

    /** Generate code for a class definition.
     *  @param env   The attribution environment that belongs to the
     *               outermost class containing this class definition.
     *               We need this for resolving some additional symbols.
     *  @param cdef  The tree representing the class definition.
     *  @return      True if code is generated with no errors.
     */
    public boolean genClass(Env<AttrContext> env, JCClassDecl cdef) {
        try {
            attrEnv = env;
            ClassSymbol c = cdef.sym;
            this.toplevel = env.toplevel;
            this.endPosTable = toplevel.endPositions;
            /* method normalizeDefs() can add references to external classes into the constant pool
             */
            cdef.defs = normalizeDefs(cdef.defs, c);
            generateReferencesToPrunedTree(c);
            Env<GenContext> localEnv = new Env<>(cdef, new GenContext());
            localEnv.toplevel = env.toplevel;
            localEnv.enclClass = cdef;

            for (List<JCTree> l = cdef.defs; l.nonEmpty(); l = l.tail) {
                genDef(l.head, localEnv);
            }
            if (poolWriter.size() > PoolWriter.MAX_ENTRIES) {
                log.error(cdef.pos(), Errors.LimitPool);
                nerrs++;
            }
            if (nerrs != 0) {
                // if errors, discard code
                for (List<JCTree> l = cdef.defs; l.nonEmpty(); l = l.tail) {
                    if (l.head.hasTag(METHODDEF))
                        ((JCMethodDecl) l.head).sym.code = null;
                }
            }
            cdef.defs = List.nil(); // discard trees
            return nerrs == 0;
        } finally {
            // note: this method does NOT support recursion.
            attrEnv = null;
            this.env = null;
            toplevel = null;
            endPosTable = null;
            nerrs = 0;
            qualifiedSymbolCache.clear();
        }
    }

/* ************************************************************************
 * Auxiliary classes
 *************************************************************************/

    /** An abstract class for finalizer generation.
     */
    abstract class GenFinalizer {
        /** Generate code to clean up when unwinding. */
        abstract void gen();

        /** Generate code to clean up at last. */
        abstract void genLast();

        /** Does this finalizer have some nontrivial cleanup to perform? */
        boolean hasFinalizer() { return true; }

        /** Should be invoked after the try's body has been visited. */
        void afterBody() {}
    }

    /** code generation contexts,
     *  to be used as type parameter for environments.
     */
    static class GenContext {

        /** A chain for all unresolved jumps that exit the current environment.
         */
        Chain exit = null;

        /** A chain for all unresolved jumps that continue in the
         *  current environment.
         */
        Chain cont = null;

        /** A closure that generates the finalizer of the current environment.
         *  Only set for Synchronized and Try contexts.
         */
        GenFinalizer finalize = null;

        /** Is this a switch statement?  If so, allocate registers
         * even when the variable declaration is unreachable.
         */
        boolean isSwitch = false;

        /** A list buffer containing all gaps in the finalizer range,
         *  where a catch all exception should not apply.
         */
        ListBuffer<Integer> gaps = null;

        /** Add given chain to exit chain.
         */
        void addExit(Chain c)  {
            exit = Code.mergeChains(c, exit);
        }

        /** Add given chain to cont chain.
         */
        void addCont(Chain c) {
            cont = Code.mergeChains(c, cont);
        }
    }

}
