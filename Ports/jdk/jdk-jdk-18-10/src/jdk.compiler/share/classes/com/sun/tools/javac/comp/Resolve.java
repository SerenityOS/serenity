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

import com.sun.tools.javac.api.Formattable.LocalizedString;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Scope.WriteableScope;
import com.sun.tools.javac.code.Source.Feature;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Type.*;
import com.sun.tools.javac.comp.Attr.ResultInfo;
import com.sun.tools.javac.comp.Check.CheckContext;
import com.sun.tools.javac.comp.DeferredAttr.AttrMode;
import com.sun.tools.javac.comp.DeferredAttr.DeferredAttrContext;
import com.sun.tools.javac.comp.DeferredAttr.DeferredType;
import com.sun.tools.javac.comp.Resolve.MethodResolutionContext.Candidate;
import com.sun.tools.javac.comp.Resolve.MethodResolutionDiagHelper.Template;
import com.sun.tools.javac.comp.Resolve.ReferenceLookupResult.StaticKind;
import com.sun.tools.javac.jvm.*;
import com.sun.tools.javac.main.Option;
import com.sun.tools.javac.resources.CompilerProperties.Errors;
import com.sun.tools.javac.resources.CompilerProperties.Fragments;
import com.sun.tools.javac.resources.CompilerProperties.Warnings;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.JCTree.JCMemberReference.ReferenceKind;
import com.sun.tools.javac.tree.JCTree.JCPolyExpression.*;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.DefinedBy.Api;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticFlag;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticType;
import com.sun.tools.javac.util.JCDiagnostic.Warning;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.lang.model.element.ElementVisitor;

import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.Flags.BLOCK;
import static com.sun.tools.javac.code.Flags.STATIC;
import static com.sun.tools.javac.code.Kinds.*;
import static com.sun.tools.javac.code.Kinds.Kind.*;
import static com.sun.tools.javac.code.TypeTag.*;
import static com.sun.tools.javac.comp.Resolve.MethodResolutionPhase.*;
import static com.sun.tools.javac.tree.JCTree.Tag.*;
import static com.sun.tools.javac.util.Iterators.createCompoundIterator;

/** Helper class for name resolution, used mostly by the attribution phase.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Resolve {
    protected static final Context.Key<Resolve> resolveKey = new Context.Key<>();

    Names names;
    Log log;
    Symtab syms;
    Attr attr;
    AttrRecover attrRecover;
    DeferredAttr deferredAttr;
    Check chk;
    Infer infer;
    ClassFinder finder;
    ModuleFinder moduleFinder;
    Types types;
    JCDiagnostic.Factory diags;
    public final boolean allowFunctionalInterfaceMostSpecific;
    public final boolean allowModules;
    public final boolean allowRecords;
    public final boolean checkVarargsAccessAfterResolution;
    private final boolean compactMethodDiags;
    private final boolean allowLocalVariableTypeInference;
    private final boolean allowYieldStatement;
    final EnumSet<VerboseResolutionMode> verboseResolutionMode;
    final boolean dumpMethodReferenceSearchResults;

    WriteableScope polymorphicSignatureScope;

    protected Resolve(Context context) {
        context.put(resolveKey, this);
        syms = Symtab.instance(context);

        varNotFound = new SymbolNotFoundError(ABSENT_VAR);
        methodNotFound = new SymbolNotFoundError(ABSENT_MTH);
        typeNotFound = new SymbolNotFoundError(ABSENT_TYP);
        referenceNotFound = ReferenceLookupResult.error(methodNotFound);

        names = Names.instance(context);
        log = Log.instance(context);
        attr = Attr.instance(context);
        attrRecover = AttrRecover.instance(context);
        deferredAttr = DeferredAttr.instance(context);
        chk = Check.instance(context);
        infer = Infer.instance(context);
        finder = ClassFinder.instance(context);
        moduleFinder = ModuleFinder.instance(context);
        types = Types.instance(context);
        diags = JCDiagnostic.Factory.instance(context);
        Preview preview = Preview.instance(context);
        Source source = Source.instance(context);
        Options options = Options.instance(context);
        compactMethodDiags = options.isSet(Option.XDIAGS, "compact") ||
                options.isUnset(Option.XDIAGS) && options.isUnset("rawDiagnostics");
        verboseResolutionMode = VerboseResolutionMode.getVerboseResolutionMode(options);
        Target target = Target.instance(context);
        allowFunctionalInterfaceMostSpecific = Feature.FUNCTIONAL_INTERFACE_MOST_SPECIFIC.allowedInSource(source);
        allowLocalVariableTypeInference = Feature.LOCAL_VARIABLE_TYPE_INFERENCE.allowedInSource(source);
        allowYieldStatement = Feature.SWITCH_EXPRESSION.allowedInSource(source);
        checkVarargsAccessAfterResolution =
                Feature.POST_APPLICABILITY_VARARGS_ACCESS_CHECK.allowedInSource(source);
        polymorphicSignatureScope = WriteableScope.create(syms.noSymbol);
        allowModules = Feature.MODULES.allowedInSource(source);
        allowRecords = Feature.RECORDS.allowedInSource(source);
        dumpMethodReferenceSearchResults = options.isSet("debug.dumpMethodReferenceSearchResults");
    }

    /** error symbols, which are returned when resolution fails
     */
    private final SymbolNotFoundError varNotFound;
    private final SymbolNotFoundError methodNotFound;
    private final SymbolNotFoundError typeNotFound;

    /** empty reference lookup result */
    private final ReferenceLookupResult referenceNotFound;

    public static Resolve instance(Context context) {
        Resolve instance = context.get(resolveKey);
        if (instance == null)
            instance = new Resolve(context);
        return instance;
    }

    private static Symbol bestOf(Symbol s1,
                                 Symbol s2) {
        return s1.kind.betterThan(s2.kind) ? s1 : s2;
    }

    // <editor-fold defaultstate="collapsed" desc="Verbose resolution diagnostics support">
    enum VerboseResolutionMode {
        SUCCESS("success"),
        FAILURE("failure"),
        APPLICABLE("applicable"),
        INAPPLICABLE("inapplicable"),
        DEFERRED_INST("deferred-inference"),
        PREDEF("predef"),
        OBJECT_INIT("object-init"),
        INTERNAL("internal");

        final String opt;

        private VerboseResolutionMode(String opt) {
            this.opt = opt;
        }

        static EnumSet<VerboseResolutionMode> getVerboseResolutionMode(Options opts) {
            String s = opts.get("debug.verboseResolution");
            EnumSet<VerboseResolutionMode> res = EnumSet.noneOf(VerboseResolutionMode.class);
            if (s == null) return res;
            if (s.contains("all")) {
                res = EnumSet.allOf(VerboseResolutionMode.class);
            }
            Collection<String> args = Arrays.asList(s.split(","));
            for (VerboseResolutionMode mode : values()) {
                if (args.contains(mode.opt)) {
                    res.add(mode);
                } else if (args.contains("-" + mode.opt)) {
                    res.remove(mode);
                }
            }
            return res;
        }
    }

    void reportVerboseResolutionDiagnostic(DiagnosticPosition dpos, Name name, Type site,
            List<Type> argtypes, List<Type> typeargtypes, Symbol bestSoFar) {
        boolean success = !bestSoFar.kind.isResolutionError();

        if (success && !verboseResolutionMode.contains(VerboseResolutionMode.SUCCESS)) {
            return;
        } else if (!success && !verboseResolutionMode.contains(VerboseResolutionMode.FAILURE)) {
            return;
        }

        if (bestSoFar.name == names.init &&
                bestSoFar.owner == syms.objectType.tsym &&
                !verboseResolutionMode.contains(VerboseResolutionMode.OBJECT_INIT)) {
            return; //skip diags for Object constructor resolution
        } else if (site == syms.predefClass.type &&
                !verboseResolutionMode.contains(VerboseResolutionMode.PREDEF)) {
            return; //skip spurious diags for predef symbols (i.e. operators)
        } else if (currentResolutionContext.internalResolution &&
                !verboseResolutionMode.contains(VerboseResolutionMode.INTERNAL)) {
            return;
        }

        int pos = 0;
        int mostSpecificPos = -1;
        ListBuffer<JCDiagnostic> subDiags = new ListBuffer<>();
        for (Candidate c : currentResolutionContext.candidates) {
            if (currentResolutionContext.step != c.step ||
                    (c.isApplicable() && !verboseResolutionMode.contains(VerboseResolutionMode.APPLICABLE)) ||
                    (!c.isApplicable() && !verboseResolutionMode.contains(VerboseResolutionMode.INAPPLICABLE))) {
                continue;
            } else {
                subDiags.append(c.isApplicable() ?
                        getVerboseApplicableCandidateDiag(pos, c.sym, c.mtype) :
                        getVerboseInapplicableCandidateDiag(pos, c.sym, c.details));
                if (c.sym == bestSoFar)
                    mostSpecificPos = pos;
                pos++;
            }
        }
        String key = success ? "verbose.resolve.multi" : "verbose.resolve.multi.1";
        List<Type> argtypes2 = argtypes.map(deferredAttr.new RecoveryDeferredTypeMap(AttrMode.SPECULATIVE, bestSoFar, currentResolutionContext.step));
        JCDiagnostic main = diags.note(log.currentSource(), dpos, key, name,
                site.tsym, mostSpecificPos, currentResolutionContext.step,
                methodArguments(argtypes2),
                methodArguments(typeargtypes));
        JCDiagnostic d = new JCDiagnostic.MultilineDiagnostic(main, subDiags.toList());
        log.report(d);
    }

    JCDiagnostic getVerboseApplicableCandidateDiag(int pos, Symbol sym, Type inst) {
        JCDiagnostic subDiag = null;
        if (sym.type.hasTag(FORALL)) {
            subDiag = diags.fragment(Fragments.PartialInstSig(inst));
        }

        String key = subDiag == null ?
                "applicable.method.found" :
                "applicable.method.found.1";

        return diags.fragment(key, pos, sym, subDiag);
    }

    JCDiagnostic getVerboseInapplicableCandidateDiag(int pos, Symbol sym, JCDiagnostic subDiag) {
        return diags.fragment(Fragments.NotApplicableMethodFound(pos, sym, subDiag));
    }
    // </editor-fold>

/* ************************************************************************
 * Identifier resolution
 *************************************************************************/

    /** An environment is "static" if its static level is greater than
     *  the one of its outer environment
     */
    protected static boolean isStatic(Env<AttrContext> env) {
        return env.outer != null && env.info.staticLevel > env.outer.info.staticLevel;
    }

    /** An environment is an "initializer" if it is a constructor or
     *  an instance initializer.
     */
    static boolean isInitializer(Env<AttrContext> env) {
        Symbol owner = env.info.scope.owner;
        return owner.isConstructor() ||
            owner.owner.kind == TYP &&
            (owner.kind == VAR ||
             owner.kind == MTH && (owner.flags() & BLOCK) != 0) &&
            (owner.flags() & STATIC) == 0;
    }

    /** Is class accessible in given environment?
     *  @param env    The current environment.
     *  @param c      The class whose accessibility is checked.
     */
    public boolean isAccessible(Env<AttrContext> env, TypeSymbol c) {
        return isAccessible(env, c, false);
    }

    public boolean isAccessible(Env<AttrContext> env, TypeSymbol c, boolean checkInner) {

        /* 15.9.5.1: Note that it is possible for the signature of the anonymous constructor
           to refer to an inaccessible type
        */
        if (env.enclMethod != null && (env.enclMethod.mods.flags & ANONCONSTR) != 0)
            return true;

        if (env.info.visitingServiceImplementation &&
            env.toplevel.modle == c.packge().modle) {
            return true;
        }

        boolean isAccessible = false;
        switch ((short)(c.flags() & AccessFlags)) {
            case PRIVATE:
                isAccessible =
                    env.enclClass.sym.outermostClass() ==
                    c.owner.outermostClass();
                break;
            case 0:
                isAccessible =
                    env.toplevel.packge == c.owner // fast special case
                    ||
                    env.toplevel.packge == c.packge();
                break;
            default: // error recovery
                isAccessible = true;
                break;
            case PUBLIC:
                if (allowModules) {
                    ModuleSymbol currModule = env.toplevel.modle;
                    currModule.complete();
                    PackageSymbol p = c.packge();
                    isAccessible =
                        currModule == p.modle ||
                        currModule.visiblePackages.get(p.fullname) == p ||
                        p == syms.rootPackage ||
                        (p.modle == syms.unnamedModule && currModule.readModules.contains(p.modle));
                } else {
                    isAccessible = true;
                }
                break;
            case PROTECTED:
                isAccessible =
                    env.toplevel.packge == c.owner // fast special case
                    ||
                    env.toplevel.packge == c.packge()
                    ||
                    isInnerSubClass(env.enclClass.sym, c.owner);
                break;
        }
        return (checkInner == false || c.type.getEnclosingType() == Type.noType) ?
            isAccessible :
            isAccessible && isAccessible(env, c.type.getEnclosingType(), checkInner);
    }
    //where
        /** Is given class a subclass of given base class, or an inner class
         *  of a subclass?
         *  Return null if no such class exists.
         *  @param c     The class which is the subclass or is contained in it.
         *  @param base  The base class
         */
        private boolean isInnerSubClass(ClassSymbol c, Symbol base) {
            while (c != null && !c.isSubClass(base, types)) {
                c = c.owner.enclClass();
            }
            return c != null;
        }

    boolean isAccessible(Env<AttrContext> env, Type t) {
        return isAccessible(env, t, false);
    }

    boolean isAccessible(Env<AttrContext> env, Type t, boolean checkInner) {
        if (t.hasTag(ARRAY)) {
            return isAccessible(env, types.cvarUpperBound(types.elemtype(t)));
        } else if (t.isUnion()) {
            return StreamSupport.stream(((UnionClassType) t).getAlternativeTypes().spliterator(), false)
                    .allMatch(alternative -> isAccessible(env, alternative.tsym, checkInner));
        } else {
            return isAccessible(env, t.tsym, checkInner);
        }
    }

    /** Is symbol accessible as a member of given type in given environment?
     *  @param env    The current environment.
     *  @param site   The type of which the tested symbol is regarded
     *                as a member.
     *  @param sym    The symbol.
     */
    public boolean isAccessible(Env<AttrContext> env, Type site, Symbol sym) {
        return isAccessible(env, site, sym, false);
    }
    public boolean isAccessible(Env<AttrContext> env, Type site, Symbol sym, boolean checkInner) {
        if (sym.name == names.init && sym.owner != site.tsym) return false;

        /* 15.9.5.1: Note that it is possible for the signature of the anonymous constructor
           to refer to an inaccessible type
        */
        if (env.enclMethod != null && (env.enclMethod.mods.flags & ANONCONSTR) != 0)
            return true;

        if (env.info.visitingServiceImplementation &&
            env.toplevel.modle == sym.packge().modle) {
            return true;
        }

        switch ((short)(sym.flags() & AccessFlags)) {
        case PRIVATE:
            return
                (env.enclClass.sym == sym.owner // fast special case
                 ||
                 env.enclClass.sym.outermostClass() ==
                 sym.owner.outermostClass())
                &&
                sym.isInheritedIn(site.tsym, types);
        case 0:
            return
                (env.toplevel.packge == sym.owner.owner // fast special case
                 ||
                 env.toplevel.packge == sym.packge())
                &&
                isAccessible(env, site, checkInner)
                &&
                sym.isInheritedIn(site.tsym, types)
                &&
                notOverriddenIn(site, sym);
        case PROTECTED:
            return
                (env.toplevel.packge == sym.owner.owner // fast special case
                 ||
                 env.toplevel.packge == sym.packge()
                 ||
                 isProtectedAccessible(sym, env.enclClass.sym, site)
                 ||
                 // OK to select instance method or field from 'super' or type name
                 // (but type names should be disallowed elsewhere!)
                 env.info.selectSuper && (sym.flags() & STATIC) == 0 && sym.kind != TYP)
                &&
                isAccessible(env, site, checkInner)
                &&
                notOverriddenIn(site, sym);
        default: // this case includes erroneous combinations as well
            return isAccessible(env, site, checkInner) && notOverriddenIn(site, sym);
        }
    }
    //where
    /* `sym' is accessible only if not overridden by
     * another symbol which is a member of `site'
     * (because, if it is overridden, `sym' is not strictly
     * speaking a member of `site'). A polymorphic signature method
     * cannot be overridden (e.g. MH.invokeExact(Object[])).
     */
    private boolean notOverriddenIn(Type site, Symbol sym) {
        if (sym.kind != MTH || sym.isConstructor() || sym.isStatic())
            return true;
        else {
            Symbol s2 = ((MethodSymbol)sym).implementation(site.tsym, types, true);
            return (s2 == null || s2 == sym || sym.owner == s2.owner ||
                    !types.isSubSignature(types.memberType(site, s2), types.memberType(site, sym)));
        }
    }
    //where
        /** Is given protected symbol accessible if it is selected from given site
         *  and the selection takes place in given class?
         *  @param sym     The symbol with protected access
         *  @param c       The class where the access takes place
         *  @site          The type of the qualifier
         */
        private
        boolean isProtectedAccessible(Symbol sym, ClassSymbol c, Type site) {
            Type newSite = site.hasTag(TYPEVAR) ? site.getUpperBound() : site;
            while (c != null &&
                   !(c.isSubClass(sym.owner, types) &&
                     (c.flags() & INTERFACE) == 0 &&
                     // In JLS 2e 6.6.2.1, the subclass restriction applies
                     // only to instance fields and methods -- types are excluded
                     // regardless of whether they are declared 'static' or not.
                     ((sym.flags() & STATIC) != 0 || sym.kind == TYP || newSite.tsym.isSubClass(c, types))))
                c = c.owner.enclClass();
            return c != null;
        }

    /**
     * Performs a recursive scan of a type looking for accessibility problems
     * from current attribution environment
     */
    void checkAccessibleType(Env<AttrContext> env, Type t) {
        accessibilityChecker.visit(t, env);
    }

    /**
     * Accessibility type-visitor
     */
    Types.SimpleVisitor<Void, Env<AttrContext>> accessibilityChecker =
            new Types.SimpleVisitor<Void, Env<AttrContext>>() {

        void visit(List<Type> ts, Env<AttrContext> env) {
            for (Type t : ts) {
                visit(t, env);
            }
        }

        public Void visitType(Type t, Env<AttrContext> env) {
            return null;
        }

        @Override
        public Void visitArrayType(ArrayType t, Env<AttrContext> env) {
            visit(t.elemtype, env);
            return null;
        }

        @Override
        public Void visitClassType(ClassType t, Env<AttrContext> env) {
            visit(t.getTypeArguments(), env);
            if (!isAccessible(env, t, true)) {
                accessBase(new AccessError(env, null, t.tsym), env.tree.pos(), env.enclClass.sym, t, t.tsym.name, true);
            }
            return null;
        }

        @Override
        public Void visitWildcardType(WildcardType t, Env<AttrContext> env) {
            visit(t.type, env);
            return null;
        }

        @Override
        public Void visitMethodType(MethodType t, Env<AttrContext> env) {
            visit(t.getParameterTypes(), env);
            visit(t.getReturnType(), env);
            visit(t.getThrownTypes(), env);
            return null;
        }
    };

    /** Try to instantiate the type of a method so that it fits
     *  given type arguments and argument types. If successful, return
     *  the method's instantiated type, else return null.
     *  The instantiation will take into account an additional leading
     *  formal parameter if the method is an instance method seen as a member
     *  of an under determined site. In this case, we treat site as an additional
     *  parameter and the parameters of the class containing the method as
     *  additional type variables that get instantiated.
     *
     *  @param env         The current environment
     *  @param site        The type of which the method is a member.
     *  @param m           The method symbol.
     *  @param argtypes    The invocation's given value arguments.
     *  @param typeargtypes    The invocation's given type arguments.
     *  @param allowBoxing Allow boxing conversions of arguments.
     *  @param useVarargs Box trailing arguments into an array for varargs.
     */
    Type rawInstantiate(Env<AttrContext> env,
                        Type site,
                        Symbol m,
                        ResultInfo resultInfo,
                        List<Type> argtypes,
                        List<Type> typeargtypes,
                        boolean allowBoxing,
                        boolean useVarargs,
                        Warner warn) throws Infer.InferenceException {
        Type mt = types.memberType(site, m);
        // tvars is the list of formal type variables for which type arguments
        // need to inferred.
        List<Type> tvars = List.nil();
        if (typeargtypes == null) typeargtypes = List.nil();
        if (!mt.hasTag(FORALL) && typeargtypes.nonEmpty()) {
            // This is not a polymorphic method, but typeargs are supplied
            // which is fine, see JLS 15.12.2.1
        } else if (mt.hasTag(FORALL) && typeargtypes.nonEmpty()) {
            ForAll pmt = (ForAll) mt;
            if (typeargtypes.length() != pmt.tvars.length())
                 // not enough args
                throw new InapplicableMethodException(diags.fragment(Fragments.WrongNumberTypeArgs(Integer.toString(pmt.tvars.length()))));
            // Check type arguments are within bounds
            List<Type> formals = pmt.tvars;
            List<Type> actuals = typeargtypes;
            while (formals.nonEmpty() && actuals.nonEmpty()) {
                List<Type> bounds = types.subst(types.getBounds((TypeVar)formals.head),
                                                pmt.tvars, typeargtypes);
                for (; bounds.nonEmpty(); bounds = bounds.tail) {
                    if (!types.isSubtypeUnchecked(actuals.head, bounds.head, warn)) {
                        throw new InapplicableMethodException(diags.fragment(Fragments.ExplicitParamDoNotConformToBounds(actuals.head, bounds)));
                    }
                }
                formals = formals.tail;
                actuals = actuals.tail;
            }
            mt = types.subst(pmt.qtype, pmt.tvars, typeargtypes);
        } else if (mt.hasTag(FORALL)) {
            ForAll pmt = (ForAll) mt;
            List<Type> tvars1 = types.newInstances(pmt.tvars);
            tvars = tvars.appendList(tvars1);
            mt = types.subst(pmt.qtype, pmt.tvars, tvars1);
        }

        // find out whether we need to go the slow route via infer
        boolean instNeeded = tvars.tail != null; /*inlined: tvars.nonEmpty()*/
        for (List<Type> l = argtypes;
             l.tail != null/*inlined: l.nonEmpty()*/ && !instNeeded;
             l = l.tail) {
            if (l.head.hasTag(FORALL)) instNeeded = true;
        }

        if (instNeeded) {
            return infer.instantiateMethod(env,
                                    tvars,
                                    (MethodType)mt,
                                    resultInfo,
                                    (MethodSymbol)m,
                                    argtypes,
                                    allowBoxing,
                                    useVarargs,
                                    currentResolutionContext,
                                    warn);
        }

        DeferredAttr.DeferredAttrContext dc = currentResolutionContext.deferredAttrContext(m, infer.emptyContext, resultInfo, warn);
        currentResolutionContext.methodCheck.argumentsAcceptable(env, dc,
                                argtypes, mt.getParameterTypes(), warn);
        dc.complete();
        return mt;
    }

    Type checkMethod(Env<AttrContext> env,
                     Type site,
                     Symbol m,
                     ResultInfo resultInfo,
                     List<Type> argtypes,
                     List<Type> typeargtypes,
                     Warner warn) {
        MethodResolutionContext prevContext = currentResolutionContext;
        try {
            currentResolutionContext = new MethodResolutionContext();
            currentResolutionContext.attrMode = (resultInfo.pt == Infer.anyPoly) ?
                    AttrMode.SPECULATIVE : DeferredAttr.AttrMode.CHECK;
            if (env.tree.hasTag(JCTree.Tag.REFERENCE)) {
                //method/constructor references need special check class
                //to handle inference variables in 'argtypes' (might happen
                //during an unsticking round)
                currentResolutionContext.methodCheck =
                        new MethodReferenceCheck(resultInfo.checkContext.inferenceContext());
            }
            MethodResolutionPhase step = currentResolutionContext.step = env.info.pendingResolutionPhase;
            return rawInstantiate(env, site, m, resultInfo, argtypes, typeargtypes,
                    step.isBoxingRequired(), step.isVarargsRequired(), warn);
        }
        finally {
            currentResolutionContext = prevContext;
        }
    }

    /** Same but returns null instead throwing a NoInstanceException
     */
    Type instantiate(Env<AttrContext> env,
                     Type site,
                     Symbol m,
                     ResultInfo resultInfo,
                     List<Type> argtypes,
                     List<Type> typeargtypes,
                     boolean allowBoxing,
                     boolean useVarargs,
                     Warner warn) {
        try {
            return rawInstantiate(env, site, m, resultInfo, argtypes, typeargtypes,
                                  allowBoxing, useVarargs, warn);
        } catch (InapplicableMethodException ex) {
            return null;
        }
    }

    /**
     * This interface defines an entry point that should be used to perform a
     * method check. A method check usually consist in determining as to whether
     * a set of types (actuals) is compatible with another set of types (formals).
     * Since the notion of compatibility can vary depending on the circumstances,
     * this interfaces allows to easily add new pluggable method check routines.
     */
    interface MethodCheck {
        /**
         * Main method check routine. A method check usually consist in determining
         * as to whether a set of types (actuals) is compatible with another set of
         * types (formals). If an incompatibility is found, an unchecked exception
         * is assumed to be thrown.
         */
        void argumentsAcceptable(Env<AttrContext> env,
                                DeferredAttrContext deferredAttrContext,
                                List<Type> argtypes,
                                List<Type> formals,
                                Warner warn);

        /**
         * Retrieve the method check object that will be used during a
         * most specific check.
         */
        MethodCheck mostSpecificCheck(List<Type> actuals);
    }

    /**
     * Helper enum defining all method check diagnostics (used by resolveMethodCheck).
     */
    enum MethodCheckDiag {
        /**
         * Actuals and formals differs in length.
         */
        ARITY_MISMATCH("arg.length.mismatch", "infer.arg.length.mismatch"),
        /**
         * An actual is incompatible with a formal.
         */
        ARG_MISMATCH("no.conforming.assignment.exists", "infer.no.conforming.assignment.exists"),
        /**
         * An actual is incompatible with the varargs element type.
         */
        VARARG_MISMATCH("varargs.argument.mismatch", "infer.varargs.argument.mismatch"),
        /**
         * The varargs element type is inaccessible.
         */
        INACCESSIBLE_VARARGS("inaccessible.varargs.type", "inaccessible.varargs.type");

        final String basicKey;
        final String inferKey;

        MethodCheckDiag(String basicKey, String inferKey) {
            this.basicKey = basicKey;
            this.inferKey = inferKey;
        }

        String regex() {
            return String.format("([a-z]*\\.)*(%s|%s)", basicKey, inferKey);
        }
    }

    /**
     * Dummy method check object. All methods are deemed applicable, regardless
     * of their formal parameter types.
     */
    MethodCheck nilMethodCheck = new MethodCheck() {
        public void argumentsAcceptable(Env<AttrContext> env, DeferredAttrContext deferredAttrContext, List<Type> argtypes, List<Type> formals, Warner warn) {
            //do nothing - method always applicable regardless of actuals
        }

        public MethodCheck mostSpecificCheck(List<Type> actuals) {
            return this;
        }
    };

    /**
     * Base class for 'real' method checks. The class defines the logic for
     * iterating through formals and actuals and provides and entry point
     * that can be used by subclasses in order to define the actual check logic.
     */
    abstract class AbstractMethodCheck implements MethodCheck {
        @Override
        public void argumentsAcceptable(final Env<AttrContext> env,
                                    DeferredAttrContext deferredAttrContext,
                                    List<Type> argtypes,
                                    List<Type> formals,
                                    Warner warn) {
            //should we expand formals?
            boolean useVarargs = deferredAttrContext.phase.isVarargsRequired();
            JCTree callTree = treeForDiagnostics(env);
            List<JCExpression> trees = TreeInfo.args(callTree);

            //inference context used during this method check
            InferenceContext inferenceContext = deferredAttrContext.inferenceContext;

            Type varargsFormal = useVarargs ? formals.last() : null;

            if (varargsFormal == null &&
                    argtypes.size() != formals.size()) {
                reportMC(callTree, MethodCheckDiag.ARITY_MISMATCH, inferenceContext); // not enough args
            }

            while (argtypes.nonEmpty() && formals.head != varargsFormal) {
                DiagnosticPosition pos = trees != null ? trees.head : null;
                checkArg(pos, false, argtypes.head, formals.head, deferredAttrContext, warn);
                argtypes = argtypes.tail;
                formals = formals.tail;
                trees = trees != null ? trees.tail : trees;
            }

            if (formals.head != varargsFormal) {
                reportMC(callTree, MethodCheckDiag.ARITY_MISMATCH, inferenceContext); // not enough args
            }

            if (useVarargs) {
                //note: if applicability check is triggered by most specific test,
                //the last argument of a varargs is _not_ an array type (see JLS 15.12.2.5)
                final Type elt = types.elemtype(varargsFormal);
                while (argtypes.nonEmpty()) {
                    DiagnosticPosition pos = trees != null ? trees.head : null;
                    checkArg(pos, true, argtypes.head, elt, deferredAttrContext, warn);
                    argtypes = argtypes.tail;
                    trees = trees != null ? trees.tail : trees;
                }
            }
        }

            // where
            private JCTree treeForDiagnostics(Env<AttrContext> env) {
                return env.info.preferredTreeForDiagnostics != null ? env.info.preferredTreeForDiagnostics : env.tree;
            }

        /**
         * Does the actual argument conforms to the corresponding formal?
         */
        abstract void checkArg(DiagnosticPosition pos, boolean varargs, Type actual, Type formal, DeferredAttrContext deferredAttrContext, Warner warn);

        protected void reportMC(DiagnosticPosition pos, MethodCheckDiag diag, InferenceContext inferenceContext, Object... args) {
            boolean inferDiag = inferenceContext != infer.emptyContext;
            if (inferDiag && (!diag.inferKey.equals(diag.basicKey))) {
                Object[] args2 = new Object[args.length + 1];
                System.arraycopy(args, 0, args2, 1, args.length);
                args2[0] = inferenceContext.inferenceVars();
                args = args2;
            }
            String key = inferDiag ? diag.inferKey : diag.basicKey;
            throw inferDiag ?
                infer.error(diags.create(DiagnosticType.FRAGMENT, log.currentSource(), pos, key, args)) :
                methodCheckFailure.setMessage(diags.create(DiagnosticType.FRAGMENT, log.currentSource(), pos, key, args));
        }

        /**
         * To eliminate the overhead associated with allocating an exception object in such an
         * hot execution path, we use flyweight pattern - and share the same exception instance
         * across multiple method check failures.
         */
        class SharedInapplicableMethodException extends InapplicableMethodException {
            private static final long serialVersionUID = 0;

            SharedInapplicableMethodException() {
                super(null);
            }

            SharedInapplicableMethodException setMessage(JCDiagnostic details) {
                this.diagnostic = details;
                return this;
            }
        }

        SharedInapplicableMethodException methodCheckFailure = new SharedInapplicableMethodException();

        public MethodCheck mostSpecificCheck(List<Type> actuals) {
            return nilMethodCheck;
        }

    }

    /**
     * Arity-based method check. A method is applicable if the number of actuals
     * supplied conforms to the method signature.
     */
    MethodCheck arityMethodCheck = new AbstractMethodCheck() {
        @Override
        void checkArg(DiagnosticPosition pos, boolean varargs, Type actual, Type formal, DeferredAttrContext deferredAttrContext, Warner warn) {
            //do nothing - actual always compatible to formals
        }

        @Override
        public String toString() {
            return "arityMethodCheck";
        }
    };

    /**
     * Main method applicability routine. Given a list of actual types A,
     * a list of formal types F, determines whether the types in A are
     * compatible (by method invocation conversion) with the types in F.
     *
     * Since this routine is shared between overload resolution and method
     * type-inference, a (possibly empty) inference context is used to convert
     * formal types to the corresponding 'undet' form ahead of a compatibility
     * check so that constraints can be propagated and collected.
     *
     * Moreover, if one or more types in A is a deferred type, this routine uses
     * DeferredAttr in order to perform deferred attribution. If one or more actual
     * deferred types are stuck, they are placed in a queue and revisited later
     * after the remainder of the arguments have been seen. If this is not sufficient
     * to 'unstuck' the argument, a cyclic inference error is called out.
     *
     * A method check handler (see above) is used in order to report errors.
     */
    MethodCheck resolveMethodCheck = new AbstractMethodCheck() {

        @Override
        void checkArg(DiagnosticPosition pos, boolean varargs, Type actual, Type formal, DeferredAttrContext deferredAttrContext, Warner warn) {
            ResultInfo mresult = methodCheckResult(varargs, formal, deferredAttrContext, warn);
            mresult.check(pos, actual);
        }

        @Override
        public void argumentsAcceptable(final Env<AttrContext> env,
                                    DeferredAttrContext deferredAttrContext,
                                    List<Type> argtypes,
                                    List<Type> formals,
                                    Warner warn) {
            super.argumentsAcceptable(env, deferredAttrContext, argtypes, formals, warn);
            // should we check varargs element type accessibility?
            if (deferredAttrContext.phase.isVarargsRequired()) {
                if (deferredAttrContext.mode == AttrMode.CHECK || !checkVarargsAccessAfterResolution) {
                    varargsAccessible(env, types.elemtype(formals.last()), deferredAttrContext.inferenceContext);
                }
            }
        }

        /**
         * Test that the runtime array element type corresponding to 't' is accessible.  't' should be the
         * varargs element type of either the method invocation type signature (after inference completes)
         * or the method declaration signature (before inference completes).
         */
        private void varargsAccessible(final Env<AttrContext> env, final Type t, final InferenceContext inferenceContext) {
            if (inferenceContext.free(t)) {
                inferenceContext.addFreeTypeListener(List.of(t),
                        solvedContext -> varargsAccessible(env, solvedContext.asInstType(t), solvedContext));
            } else {
                if (!isAccessible(env, types.erasure(t))) {
                    Symbol location = env.enclClass.sym;
                    reportMC(env.tree, MethodCheckDiag.INACCESSIBLE_VARARGS, inferenceContext, t, Kinds.kindName(location), location);
                }
            }
        }

        private ResultInfo methodCheckResult(final boolean varargsCheck, Type to,
                final DeferredAttr.DeferredAttrContext deferredAttrContext, Warner rsWarner) {
            CheckContext checkContext = new MethodCheckContext(!deferredAttrContext.phase.isBoxingRequired(), deferredAttrContext, rsWarner) {
                MethodCheckDiag methodDiag = varargsCheck ?
                                 MethodCheckDiag.VARARG_MISMATCH : MethodCheckDiag.ARG_MISMATCH;

                @Override
                public void report(DiagnosticPosition pos, JCDiagnostic details) {
                    reportMC(pos, methodDiag, deferredAttrContext.inferenceContext, details);
                }
            };
            return new MethodResultInfo(to, checkContext);
        }

        @Override
        public MethodCheck mostSpecificCheck(List<Type> actuals) {
            return new MostSpecificCheck(actuals);
        }

        @Override
        public String toString() {
            return "resolveMethodCheck";
        }
    };

    /**
     * This class handles method reference applicability checks; since during
     * these checks it's sometime possible to have inference variables on
     * the actual argument types list, the method applicability check must be
     * extended so that inference variables are 'opened' as needed.
     */
    class MethodReferenceCheck extends AbstractMethodCheck {

        InferenceContext pendingInferenceContext;

        MethodReferenceCheck(InferenceContext pendingInferenceContext) {
            this.pendingInferenceContext = pendingInferenceContext;
        }

        @Override
        void checkArg(DiagnosticPosition pos, boolean varargs, Type actual, Type formal, DeferredAttrContext deferredAttrContext, Warner warn) {
            ResultInfo mresult = methodCheckResult(varargs, formal, deferredAttrContext, warn);
            mresult.check(pos, actual);
        }

        private ResultInfo methodCheckResult(final boolean varargsCheck, Type to,
                final DeferredAttr.DeferredAttrContext deferredAttrContext, Warner rsWarner) {
            CheckContext checkContext = new MethodCheckContext(!deferredAttrContext.phase.isBoxingRequired(), deferredAttrContext, rsWarner) {
                MethodCheckDiag methodDiag = varargsCheck ?
                                 MethodCheckDiag.VARARG_MISMATCH : MethodCheckDiag.ARG_MISMATCH;

                @Override
                public boolean compatible(Type found, Type req, Warner warn) {
                    found = pendingInferenceContext.asUndetVar(found);
                    if (found.hasTag(UNDETVAR) && req.isPrimitive()) {
                        req = types.boxedClass(req).type;
                    }
                    return super.compatible(found, req, warn);
                }

                @Override
                public void report(DiagnosticPosition pos, JCDiagnostic details) {
                    reportMC(pos, methodDiag, deferredAttrContext.inferenceContext, details);
                }
            };
            return new MethodResultInfo(to, checkContext);
        }

        @Override
        public MethodCheck mostSpecificCheck(List<Type> actuals) {
            return new MostSpecificCheck(actuals);
        }

        @Override
        public String toString() {
            return "MethodReferenceCheck";
        }
    }

    /**
     * Check context to be used during method applicability checks. A method check
     * context might contain inference variables.
     */
    abstract class MethodCheckContext implements CheckContext {

        boolean strict;
        DeferredAttrContext deferredAttrContext;
        Warner rsWarner;

        public MethodCheckContext(boolean strict, DeferredAttrContext deferredAttrContext, Warner rsWarner) {
           this.strict = strict;
           this.deferredAttrContext = deferredAttrContext;
           this.rsWarner = rsWarner;
        }

        public boolean compatible(Type found, Type req, Warner warn) {
            InferenceContext inferenceContext = deferredAttrContext.inferenceContext;
            return strict ?
                    types.isSubtypeUnchecked(inferenceContext.asUndetVar(found), inferenceContext.asUndetVar(req), warn) :
                    types.isConvertible(inferenceContext.asUndetVar(found), inferenceContext.asUndetVar(req), warn);
        }

        public void report(DiagnosticPosition pos, JCDiagnostic details) {
            throw new InapplicableMethodException(details);
        }

        public Warner checkWarner(DiagnosticPosition pos, Type found, Type req) {
            return rsWarner;
        }

        public InferenceContext inferenceContext() {
            return deferredAttrContext.inferenceContext;
        }

        public DeferredAttrContext deferredAttrContext() {
            return deferredAttrContext;
        }

        @Override
        public String toString() {
            return "MethodCheckContext";
        }
    }

    /**
     * ResultInfo class to be used during method applicability checks. Check
     * for deferred types goes through special path.
     */
    class MethodResultInfo extends ResultInfo {

        public MethodResultInfo(Type pt, CheckContext checkContext) {
            attr.super(KindSelector.VAL, pt, checkContext);
        }

        @Override
        protected Type check(DiagnosticPosition pos, Type found) {
            if (found.hasTag(DEFERRED)) {
                DeferredType dt = (DeferredType)found;
                return dt.check(this);
            } else {
                Type uResult = U(found);
                Type capturedType = pos == null || pos.getTree() == null ?
                        types.capture(uResult) :
                        checkContext.inferenceContext()
                            .cachedCapture(pos.getTree(), uResult, true);
                return super.check(pos, chk.checkNonVoid(pos, capturedType));
            }
        }

        /**
         * javac has a long-standing 'simplification' (see 6391995):
         * given an actual argument type, the method check is performed
         * on its upper bound. This leads to inconsistencies when an
         * argument type is checked against itself. For example, given
         * a type-variable T, it is not true that {@code U(T) <: T},
         * so we need to guard against that.
         */
        private Type U(Type found) {
            return found == pt ?
                    found : types.cvarUpperBound(found);
        }

        @Override
        protected MethodResultInfo dup(Type newPt) {
            return new MethodResultInfo(newPt, checkContext);
        }

        @Override
        protected ResultInfo dup(CheckContext newContext) {
            return new MethodResultInfo(pt, newContext);
        }

        @Override
        protected ResultInfo dup(Type newPt, CheckContext newContext) {
            return new MethodResultInfo(newPt, newContext);
        }
    }

    /**
     * Most specific method applicability routine. Given a list of actual types A,
     * a list of formal types F1, and a list of formal types F2, the routine determines
     * as to whether the types in F1 can be considered more specific than those in F2 w.r.t.
     * argument types A.
     */
    class MostSpecificCheck implements MethodCheck {

        List<Type> actuals;

        MostSpecificCheck(List<Type> actuals) {
            this.actuals = actuals;
        }

        @Override
        public void argumentsAcceptable(final Env<AttrContext> env,
                                    DeferredAttrContext deferredAttrContext,
                                    List<Type> formals1,
                                    List<Type> formals2,
                                    Warner warn) {
            formals2 = adjustArgs(formals2, deferredAttrContext.msym, formals1.length(), deferredAttrContext.phase.isVarargsRequired());
            while (formals2.nonEmpty()) {
                ResultInfo mresult = methodCheckResult(formals2.head, deferredAttrContext, warn, actuals.head);
                mresult.check(null, formals1.head);
                formals1 = formals1.tail;
                formals2 = formals2.tail;
                actuals = actuals.isEmpty() ? actuals : actuals.tail;
            }
        }

       /**
        * Create a method check context to be used during the most specific applicability check
        */
        ResultInfo methodCheckResult(Type to, DeferredAttr.DeferredAttrContext deferredAttrContext,
               Warner rsWarner, Type actual) {
            return attr.new ResultInfo(KindSelector.VAL, to,
                   new MostSpecificCheckContext(deferredAttrContext, rsWarner, actual));
        }

        /**
         * Subclass of method check context class that implements most specific
         * method conversion. If the actual type under analysis is a deferred type
         * a full blown structural analysis is carried out.
         */
        class MostSpecificCheckContext extends MethodCheckContext {

            Type actual;

            public MostSpecificCheckContext(DeferredAttrContext deferredAttrContext, Warner rsWarner, Type actual) {
                super(true, deferredAttrContext, rsWarner);
                this.actual = actual;
            }

            public boolean compatible(Type found, Type req, Warner warn) {
                if (allowFunctionalInterfaceMostSpecific &&
                        unrelatedFunctionalInterfaces(found, req) &&
                        (actual != null && actual.getTag() == DEFERRED)) {
                    DeferredType dt = (DeferredType) actual;
                    JCTree speculativeTree = dt.speculativeTree(deferredAttrContext);
                    if (speculativeTree != deferredAttr.stuckTree) {
                        return functionalInterfaceMostSpecific(found, req, speculativeTree);
                    }
                }
                return compatibleBySubtyping(found, req);
            }

            private boolean compatibleBySubtyping(Type found, Type req) {
                if (!strict && found.isPrimitive() != req.isPrimitive()) {
                    found = found.isPrimitive() ? types.boxedClass(found).type : types.unboxedType(found);
                }
                return types.isSubtypeNoCapture(found, deferredAttrContext.inferenceContext.asUndetVar(req));
            }

            /** Whether {@code t} and {@code s} are unrelated functional interface types. */
            private boolean unrelatedFunctionalInterfaces(Type t, Type s) {
                return types.isFunctionalInterface(t.tsym) &&
                       types.isFunctionalInterface(s.tsym) &&
                       unrelatedInterfaces(t, s);
            }

            /** Whether {@code t} and {@code s} are unrelated interface types; recurs on intersections. **/
            private boolean unrelatedInterfaces(Type t, Type s) {
                if (t.isCompound()) {
                    for (Type ti : types.interfaces(t)) {
                        if (!unrelatedInterfaces(ti, s)) {
                            return false;
                        }
                    }
                    return true;
                } else if (s.isCompound()) {
                    for (Type si : types.interfaces(s)) {
                        if (!unrelatedInterfaces(t, si)) {
                            return false;
                        }
                    }
                    return true;
                } else {
                    return types.asSuper(t, s.tsym) == null && types.asSuper(s, t.tsym) == null;
                }
            }

            /** Parameters {@code t} and {@code s} are unrelated functional interface types. */
            private boolean functionalInterfaceMostSpecific(Type t, Type s, JCTree tree) {
                Type tDesc = types.findDescriptorType(types.capture(t));
                Type tDescNoCapture = types.findDescriptorType(t);
                Type sDesc = types.findDescriptorType(s);
                final List<Type> tTypeParams = tDesc.getTypeArguments();
                final List<Type> tTypeParamsNoCapture = tDescNoCapture.getTypeArguments();
                final List<Type> sTypeParams = sDesc.getTypeArguments();

                // compare type parameters
                if (tDesc.hasTag(FORALL) && !types.hasSameBounds((ForAll) tDesc, (ForAll) tDescNoCapture)) {
                    return false;
                }
                // can't use Types.hasSameBounds on sDesc because bounds may have ivars
                List<Type> tIter = tTypeParams;
                List<Type> sIter = sTypeParams;
                while (tIter.nonEmpty() && sIter.nonEmpty()) {
                    Type tBound = tIter.head.getUpperBound();
                    Type sBound = types.subst(sIter.head.getUpperBound(), sTypeParams, tTypeParams);
                    if (tBound.containsAny(tTypeParams) && inferenceContext().free(sBound)) {
                        return false;
                    }
                    if (!types.isSameType(tBound, inferenceContext().asUndetVar(sBound))) {
                        return false;
                    }
                    tIter = tIter.tail;
                    sIter = sIter.tail;
                }
                if (!tIter.isEmpty() || !sIter.isEmpty()) {
                    return false;
                }

                // compare parameters
                List<Type> tParams = tDesc.getParameterTypes();
                List<Type> tParamsNoCapture = tDescNoCapture.getParameterTypes();
                List<Type> sParams = sDesc.getParameterTypes();
                while (tParams.nonEmpty() && tParamsNoCapture.nonEmpty() && sParams.nonEmpty()) {
                    Type tParam = tParams.head;
                    Type tParamNoCapture = types.subst(tParamsNoCapture.head, tTypeParamsNoCapture, tTypeParams);
                    Type sParam = types.subst(sParams.head, sTypeParams, tTypeParams);
                    if (tParam.containsAny(tTypeParams) && inferenceContext().free(sParam)) {
                        return false;
                    }
                    if (!types.isSubtype(inferenceContext().asUndetVar(sParam), tParam)) {
                        return false;
                    }
                    if (!types.isSameType(tParamNoCapture, inferenceContext().asUndetVar(sParam))) {
                        return false;
                    }
                    tParams = tParams.tail;
                    tParamsNoCapture = tParamsNoCapture.tail;
                    sParams = sParams.tail;
                }
                if (!tParams.isEmpty() || !tParamsNoCapture.isEmpty() || !sParams.isEmpty()) {
                    return false;
                }

                // compare returns
                Type tRet = tDesc.getReturnType();
                Type sRet = types.subst(sDesc.getReturnType(), sTypeParams, tTypeParams);
                if (tRet.containsAny(tTypeParams) && inferenceContext().free(sRet)) {
                    return false;
                }
                MostSpecificFunctionReturnChecker msc = new MostSpecificFunctionReturnChecker(tRet, sRet);
                msc.scan(tree);
                return msc.result;
            }

            /**
             * Tests whether one functional interface type can be considered more specific
             * than another unrelated functional interface type for the scanned expression.
             */
            class MostSpecificFunctionReturnChecker extends DeferredAttr.PolyScanner {

                final Type tRet;
                final Type sRet;
                boolean result;

                /** Parameters {@code t} and {@code s} are unrelated functional interface types. */
                MostSpecificFunctionReturnChecker(Type tRet, Type sRet) {
                    this.tRet = tRet;
                    this.sRet = sRet;
                    result = true;
                }

                @Override
                void skip(JCTree tree) {
                    result &= false;
                }

                @Override
                public void visitConditional(JCConditional tree) {
                    scan(asExpr(tree.truepart));
                    scan(asExpr(tree.falsepart));
                }

                @Override
                public void visitReference(JCMemberReference tree) {
                    if (sRet.hasTag(VOID)) {
                        result &= true;
                    } else if (tRet.hasTag(VOID)) {
                        result &= false;
                    } else if (tRet.isPrimitive() != sRet.isPrimitive()) {
                        boolean retValIsPrimitive =
                                tree.refPolyKind == PolyKind.STANDALONE &&
                                tree.sym.type.getReturnType().isPrimitive();
                        result &= (retValIsPrimitive == tRet.isPrimitive()) &&
                                  (retValIsPrimitive != sRet.isPrimitive());
                    } else {
                        result &= compatibleBySubtyping(tRet, sRet);
                    }
                }

                @Override
                public void visitParens(JCParens tree) {
                    scan(asExpr(tree.expr));
                }

                @Override
                public void visitLambda(JCLambda tree) {
                    if (sRet.hasTag(VOID)) {
                        result &= true;
                    } else if (tRet.hasTag(VOID)) {
                        result &= false;
                    } else {
                        List<JCExpression> lambdaResults = lambdaResults(tree);
                        if (!lambdaResults.isEmpty() && unrelatedFunctionalInterfaces(tRet, sRet)) {
                            for (JCExpression expr : lambdaResults) {
                                result &= functionalInterfaceMostSpecific(tRet, sRet, expr);
                            }
                        } else if (!lambdaResults.isEmpty() && tRet.isPrimitive() != sRet.isPrimitive()) {
                            for (JCExpression expr : lambdaResults) {
                                boolean retValIsPrimitive = expr.isStandalone() && expr.type.isPrimitive();
                                result &= (retValIsPrimitive == tRet.isPrimitive()) &&
                                        (retValIsPrimitive != sRet.isPrimitive());
                            }
                        } else {
                            result &= compatibleBySubtyping(tRet, sRet);
                        }
                    }
                }
                //where

                private List<JCExpression> lambdaResults(JCLambda lambda) {
                    if (lambda.getBodyKind() == JCTree.JCLambda.BodyKind.EXPRESSION) {
                        return List.of(asExpr((JCExpression) lambda.body));
                    } else {
                        final ListBuffer<JCExpression> buffer = new ListBuffer<>();
                        DeferredAttr.LambdaReturnScanner lambdaScanner =
                                new DeferredAttr.LambdaReturnScanner() {
                                    @Override
                                    public void visitReturn(JCReturn tree) {
                                        if (tree.expr != null) {
                                            buffer.append(asExpr(tree.expr));
                                        }
                                    }
                                };
                        lambdaScanner.scan(lambda.body);
                        return buffer.toList();
                    }
                }

                private JCExpression asExpr(JCExpression expr) {
                    if (expr.type.hasTag(DEFERRED)) {
                        JCTree speculativeTree = ((DeferredType)expr.type).speculativeTree(deferredAttrContext);
                        if (speculativeTree != deferredAttr.stuckTree) {
                            expr = (JCExpression)speculativeTree;
                        }
                    }
                    return expr;
                }
            }

        }

        public MethodCheck mostSpecificCheck(List<Type> actuals) {
            Assert.error("Cannot get here!");
            return null;
        }
    }

    public static class InapplicableMethodException extends RuntimeException {
        private static final long serialVersionUID = 0;

        transient JCDiagnostic diagnostic;

        InapplicableMethodException(JCDiagnostic diag) {
            this.diagnostic = diag;
        }

        public JCDiagnostic getDiagnostic() {
            return diagnostic;
        }
    }

/* ***************************************************************************
 *  Symbol lookup
 *  the following naming conventions for arguments are used
 *
 *       env      is the environment where the symbol was mentioned
 *       site     is the type of which the symbol is a member
 *       name     is the symbol's name
 *                if no arguments are given
 *       argtypes are the value arguments, if we search for a method
 *
 *  If no symbol was found, a ResolveError detailing the problem is returned.
 ****************************************************************************/

    /** Find field. Synthetic fields are always skipped.
     *  @param env     The current environment.
     *  @param site    The original type from where the selection takes place.
     *  @param name    The name of the field.
     *  @param c       The class to search for the field. This is always
     *                 a superclass or implemented interface of site's class.
     */
    Symbol findField(Env<AttrContext> env,
                     Type site,
                     Name name,
                     TypeSymbol c) {
        while (c.type.hasTag(TYPEVAR))
            c = c.type.getUpperBound().tsym;
        Symbol bestSoFar = varNotFound;
        Symbol sym;
        for (Symbol s : c.members().getSymbolsByName(name)) {
            if (s.kind == VAR && (s.flags_field & SYNTHETIC) == 0) {
                return isAccessible(env, site, s)
                    ? s : new AccessError(env, site, s);
            }
        }
        Type st = types.supertype(c.type);
        if (st != null && (st.hasTag(CLASS) || st.hasTag(TYPEVAR))) {
            sym = findField(env, site, name, st.tsym);
            bestSoFar = bestOf(bestSoFar, sym);
        }
        for (List<Type> l = types.interfaces(c.type);
             bestSoFar.kind != AMBIGUOUS && l.nonEmpty();
             l = l.tail) {
            sym = findField(env, site, name, l.head.tsym);
            if (bestSoFar.exists() && sym.exists() &&
                sym.owner != bestSoFar.owner)
                bestSoFar = new AmbiguityError(bestSoFar, sym);
            else
                bestSoFar = bestOf(bestSoFar, sym);
        }
        return bestSoFar;
    }

    /** Resolve a field identifier, throw a fatal error if not found.
     *  @param pos       The position to use for error reporting.
     *  @param env       The environment current at the method invocation.
     *  @param site      The type of the qualifying expression, in which
     *                   identifier is searched.
     *  @param name      The identifier's name.
     */
    public VarSymbol resolveInternalField(DiagnosticPosition pos, Env<AttrContext> env,
                                          Type site, Name name) {
        Symbol sym = findField(env, site, name, site.tsym);
        if (sym.kind == VAR) return (VarSymbol)sym;
        else throw new FatalError(
                 diags.fragment(Fragments.FatalErrCantLocateField(name)));
    }

    /** Find unqualified variable or field with given name.
     *  Synthetic fields always skipped.
     *  @param env     The current environment.
     *  @param name    The name of the variable or field.
     */
    Symbol findVar(Env<AttrContext> env, Name name) {
        Symbol bestSoFar = varNotFound;
        Env<AttrContext> env1 = env;
        boolean staticOnly = false;
        while (env1.outer != null) {
            Symbol sym = null;
            for (Symbol s : env1.info.scope.getSymbolsByName(name)) {
                if (s.kind == VAR && (s.flags_field & SYNTHETIC) == 0) {
                    sym = s;
                    if (staticOnly) {
                        return new StaticError(sym);
                    }
                    break;
                }
            }
            if (isStatic(env1)) staticOnly = true;
            if (sym == null) {
                sym = findField(env1, env1.enclClass.sym.type, name, env1.enclClass.sym);
            }
            if (sym.exists()) {
                if (staticOnly &&
                        sym.kind == VAR &&
                        sym.owner.kind == TYP &&
                        (sym.flags() & STATIC) == 0)
                    return new StaticError(sym);
                else
                    return sym;
            } else {
                bestSoFar = bestOf(bestSoFar, sym);
            }

            if ((env1.enclClass.sym.flags() & STATIC) != 0) staticOnly = true;
            env1 = env1.outer;
        }

        Symbol sym = findField(env, syms.predefClass.type, name, syms.predefClass);
        if (sym.exists())
            return sym;
        if (bestSoFar.exists())
            return bestSoFar;

        Symbol origin = null;
        for (Scope sc : new Scope[] { env.toplevel.namedImportScope, env.toplevel.starImportScope }) {
            for (Symbol currentSymbol : sc.getSymbolsByName(name)) {
                if (currentSymbol.kind != VAR)
                    continue;
                // invariant: sym.kind == Symbol.Kind.VAR
                if (!bestSoFar.kind.isResolutionError() &&
                    currentSymbol.owner != bestSoFar.owner)
                    return new AmbiguityError(bestSoFar, currentSymbol);
                else if (!bestSoFar.kind.betterThan(VAR)) {
                    origin = sc.getOrigin(currentSymbol).owner;
                    bestSoFar = isAccessible(env, origin.type, currentSymbol)
                        ? currentSymbol : new AccessError(env, origin.type, currentSymbol);
                }
            }
            if (bestSoFar.exists()) break;
        }
        if (bestSoFar.kind == VAR && bestSoFar.owner.type != origin.type)
            return bestSoFar.clone(origin);
        else
            return bestSoFar;
    }

    Warner noteWarner = new Warner();

    /** Select the best method for a call site among two choices.
     *  @param env              The current environment.
     *  @param site             The original type from where the
     *                          selection takes place.
     *  @param argtypes         The invocation's value arguments,
     *  @param typeargtypes     The invocation's type arguments,
     *  @param sym              Proposed new best match.
     *  @param bestSoFar        Previously found best match.
     *  @param allowBoxing Allow boxing conversions of arguments.
     *  @param useVarargs Box trailing arguments into an array for varargs.
     */
    @SuppressWarnings("fallthrough")
    Symbol selectBest(Env<AttrContext> env,
                      Type site,
                      List<Type> argtypes,
                      List<Type> typeargtypes,
                      Symbol sym,
                      Symbol bestSoFar,
                      boolean allowBoxing,
                      boolean useVarargs) {
        if (sym.kind == ERR ||
                (site.tsym != sym.owner && !sym.isInheritedIn(site.tsym, types)) ||
                !notOverriddenIn(site, sym)) {
            return bestSoFar;
        } else if (useVarargs && (sym.flags() & VARARGS) == 0) {
            return bestSoFar.kind.isResolutionError() ?
                    new BadVarargsMethod((ResolveError)bestSoFar.baseSymbol()) :
                    bestSoFar;
        }
        Assert.check(!sym.kind.isResolutionError());
        try {
            types.noWarnings.clear();
            Type mt = rawInstantiate(env, site, sym, null, argtypes, typeargtypes,
                               allowBoxing, useVarargs, types.noWarnings);
            currentResolutionContext.addApplicableCandidate(sym, mt);
        } catch (InapplicableMethodException ex) {
            currentResolutionContext.addInapplicableCandidate(sym, ex.getDiagnostic());
            // Currently, an InapplicableMethodException occurs.
            // If bestSoFar.kind was ABSENT_MTH, return an InapplicableSymbolError(kind is WRONG_MTH).
            // If bestSoFar.kind was HIDDEN(AccessError)/WRONG_MTH/WRONG_MTHS, return an InapplicableSymbolsError(kind is WRONG_MTHS).
            // See JDK-8255968 for more information.
            switch (bestSoFar.kind) {
                case ABSENT_MTH:
                    return new InapplicableSymbolError(currentResolutionContext);
                case HIDDEN:
                    if (bestSoFar instanceof AccessError accessError) {
                        // Add the JCDiagnostic of previous AccessError to the currentResolutionContext
                        // and construct InapplicableSymbolsError.
                        // Intentionally fallthrough.
                        currentResolutionContext.addInapplicableCandidate(accessError.sym,
                                accessError.getDiagnostic(JCDiagnostic.DiagnosticType.FRAGMENT, null, null, site, null, argtypes, typeargtypes));
                    } else {
                        return bestSoFar;
                    }
                case WRONG_MTH:
                    bestSoFar = new InapplicableSymbolsError(currentResolutionContext);
                default:
                    return bestSoFar;
            }
        }
        if (!isAccessible(env, site, sym)) {
            AccessError curAccessError = new AccessError(env, site, sym);
            JCDiagnostic curDiagnostic = curAccessError.getDiagnostic(JCDiagnostic.DiagnosticType.FRAGMENT, null, null, site, null, argtypes, typeargtypes);
            // Currently, an AccessError occurs.
            // If bestSoFar.kind was ABSENT_MTH, return an AccessError(kind is HIDDEN).
            // If bestSoFar.kind was HIDDEN(AccessError), WRONG_MTH, WRONG_MTHS, return an InapplicableSymbolsError(kind is WRONG_MTHS).
            // See JDK-8255968 for more information.
            if (bestSoFar.kind == ABSENT_MTH) {
                bestSoFar = curAccessError;
            } else if (bestSoFar.kind == WRONG_MTH) {
                // Add the JCDiagnostic of current AccessError to the currentResolutionContext
                // and construct InapplicableSymbolsError.
                currentResolutionContext.addInapplicableCandidate(sym, curDiagnostic);
                bestSoFar = new InapplicableSymbolsError(currentResolutionContext);
            } else if (bestSoFar.kind == WRONG_MTHS) {
                // Add the JCDiagnostic of current AccessError to the currentResolutionContext
                currentResolutionContext.addInapplicableCandidate(sym, curDiagnostic);
            } else if (bestSoFar.kind == HIDDEN && bestSoFar instanceof AccessError accessError) {
                // Add the JCDiagnostics of previous and current AccessError to the currentResolutionContext
                // and construct InapplicableSymbolsError.
                currentResolutionContext.addInapplicableCandidate(accessError.sym,
                        accessError.getDiagnostic(JCDiagnostic.DiagnosticType.FRAGMENT, null, null, site, null, argtypes, typeargtypes));
                currentResolutionContext.addInapplicableCandidate(sym, curDiagnostic);
                bestSoFar = new InapplicableSymbolsError(currentResolutionContext);
            }
            return bestSoFar;
        }
        return (bestSoFar.kind.isResolutionError() && bestSoFar.kind != AMBIGUOUS)
            ? sym
            : mostSpecific(argtypes, sym, bestSoFar, env, site, useVarargs);
    }

    /* Return the most specific of the two methods for a call,
     *  given that both are accessible and applicable.
     *  @param m1               A new candidate for most specific.
     *  @param m2               The previous most specific candidate.
     *  @param env              The current environment.
     *  @param site             The original type from where the selection
     *                          takes place.
     *  @param allowBoxing Allow boxing conversions of arguments.
     *  @param useVarargs Box trailing arguments into an array for varargs.
     */
    Symbol mostSpecific(List<Type> argtypes, Symbol m1,
                        Symbol m2,
                        Env<AttrContext> env,
                        final Type site,
                        boolean useVarargs) {
        switch (m2.kind) {
        case MTH:
            if (m1 == m2) return m1;
            boolean m1SignatureMoreSpecific =
                    signatureMoreSpecific(argtypes, env, site, m1, m2, useVarargs);
            boolean m2SignatureMoreSpecific =
                    signatureMoreSpecific(argtypes, env, site, m2, m1, useVarargs);
            if (m1SignatureMoreSpecific && m2SignatureMoreSpecific) {
                Type mt1 = types.memberType(site, m1);
                Type mt2 = types.memberType(site, m2);
                if (!types.overrideEquivalent(mt1, mt2))
                    return ambiguityError(m1, m2);

                // same signature; select (a) the non-bridge method, or
                // (b) the one that overrides the other, or (c) the concrete
                // one, or (d) merge both abstract signatures
                if ((m1.flags() & BRIDGE) != (m2.flags() & BRIDGE))
                    return ((m1.flags() & BRIDGE) != 0) ? m2 : m1;

                if (m1.baseSymbol() == m2.baseSymbol()) {
                    // this is the same imported symbol which has been cloned twice.
                    // Return the first one (either will do).
                    return m1;
                }

                // if one overrides or hides the other, use it
                TypeSymbol m1Owner = (TypeSymbol)m1.owner;
                TypeSymbol m2Owner = (TypeSymbol)m2.owner;
                // the two owners can never be the same if the target methods are compiled from source,
                // but we need to protect against cases where the methods are defined in some classfile
                // and make sure we issue an ambiguity error accordingly (by skipping the logic below).
                if (m1Owner != m2Owner) {
                    if (types.asSuper(m1Owner.type, m2Owner) != null &&
                        ((m1.owner.flags_field & INTERFACE) == 0 ||
                         (m2.owner.flags_field & INTERFACE) != 0) &&
                        m1.overrides(m2, m1Owner, types, false))
                        return m1;
                    if (types.asSuper(m2Owner.type, m1Owner) != null &&
                        ((m2.owner.flags_field & INTERFACE) == 0 ||
                         (m1.owner.flags_field & INTERFACE) != 0) &&
                        m2.overrides(m1, m2Owner, types, false))
                        return m2;
                }
                boolean m1Abstract = (m1.flags() & ABSTRACT) != 0;
                boolean m2Abstract = (m2.flags() & ABSTRACT) != 0;
                if (m1Abstract && !m2Abstract) return m2;
                if (m2Abstract && !m1Abstract) return m1;
                // both abstract or both concrete
                return ambiguityError(m1, m2);
            }
            if (m1SignatureMoreSpecific) return m1;
            if (m2SignatureMoreSpecific) return m2;
            return ambiguityError(m1, m2);
        case AMBIGUOUS:
            //compare m1 to ambiguous methods in m2
            AmbiguityError e = (AmbiguityError)m2.baseSymbol();
            boolean m1MoreSpecificThanAnyAmbiguous = true;
            boolean allAmbiguousMoreSpecificThanM1 = true;
            for (Symbol s : e.ambiguousSyms) {
                Symbol moreSpecific = mostSpecific(argtypes, m1, s, env, site, useVarargs);
                m1MoreSpecificThanAnyAmbiguous &= moreSpecific == m1;
                allAmbiguousMoreSpecificThanM1 &= moreSpecific == s;
            }
            if (m1MoreSpecificThanAnyAmbiguous)
                return m1;
            //if m1 is more specific than some ambiguous methods, but other ambiguous methods are
            //more specific than m1, add it as a new ambiguous method:
            if (!allAmbiguousMoreSpecificThanM1)
                e.addAmbiguousSymbol(m1);
            return e;
        default:
            throw new AssertionError();
        }
    }
    //where
    private boolean signatureMoreSpecific(List<Type> actuals, Env<AttrContext> env, Type site, Symbol m1, Symbol m2, boolean useVarargs) {
        noteWarner.clear();
        int maxLength = Math.max(
                            Math.max(m1.type.getParameterTypes().length(), actuals.length()),
                            m2.type.getParameterTypes().length());
        MethodResolutionContext prevResolutionContext = currentResolutionContext;
        try {
            currentResolutionContext = new MethodResolutionContext();
            currentResolutionContext.step = prevResolutionContext.step;
            currentResolutionContext.methodCheck =
                    prevResolutionContext.methodCheck.mostSpecificCheck(actuals);
            Type mst = instantiate(env, site, m2, null,
                    adjustArgs(types.cvarLowerBounds(types.memberType(site, m1).getParameterTypes()), m1, maxLength, useVarargs), null,
                    false, useVarargs, noteWarner);
            return mst != null &&
                    !noteWarner.hasLint(Lint.LintCategory.UNCHECKED);
        } finally {
            currentResolutionContext = prevResolutionContext;
        }
    }

    List<Type> adjustArgs(List<Type> args, Symbol msym, int length, boolean allowVarargs) {
        if ((msym.flags() & VARARGS) != 0 && allowVarargs) {
            Type varargsElem = types.elemtype(args.last());
            if (varargsElem == null) {
                Assert.error("Bad varargs = " + args.last() + " " + msym);
            }
            List<Type> newArgs = args.reverse().tail.prepend(varargsElem).reverse();
            while (newArgs.length() < length) {
                newArgs = newArgs.append(newArgs.last());
            }
            return newArgs;
        } else {
            return args;
        }
    }
    //where
    Symbol ambiguityError(Symbol m1, Symbol m2) {
        if (((m1.flags() | m2.flags()) & CLASH) != 0) {
            return (m1.flags() & CLASH) == 0 ? m1 : m2;
        } else {
            return new AmbiguityError(m1, m2);
        }
    }

    Symbol findMethodInScope(Env<AttrContext> env,
            Type site,
            Name name,
            List<Type> argtypes,
            List<Type> typeargtypes,
            Scope sc,
            Symbol bestSoFar,
            boolean allowBoxing,
            boolean useVarargs,
            boolean abstractok) {
        for (Symbol s : sc.getSymbolsByName(name, new LookupFilter(abstractok))) {
            bestSoFar = selectBest(env, site, argtypes, typeargtypes, s,
                    bestSoFar, allowBoxing, useVarargs);
        }
        return bestSoFar;
    }
    //where
        class LookupFilter implements Predicate<Symbol> {

            boolean abstractOk;

            LookupFilter(boolean abstractOk) {
                this.abstractOk = abstractOk;
            }

            @Override
            public boolean test(Symbol s) {
                long flags = s.flags();
                return s.kind == MTH &&
                        (flags & SYNTHETIC) == 0 &&
                        (abstractOk ||
                        (flags & DEFAULT) != 0 ||
                        (flags & ABSTRACT) == 0);
            }
        }

    /** Find best qualified method matching given name, type and value
     *  arguments.
     *  @param env       The current environment.
     *  @param site      The original type from where the selection
     *                   takes place.
     *  @param name      The method's name.
     *  @param argtypes  The method's value arguments.
     *  @param typeargtypes The method's type arguments
     *  @param allowBoxing Allow boxing conversions of arguments.
     *  @param useVarargs Box trailing arguments into an array for varargs.
     */
    Symbol findMethod(Env<AttrContext> env,
                      Type site,
                      Name name,
                      List<Type> argtypes,
                      List<Type> typeargtypes,
                      boolean allowBoxing,
                      boolean useVarargs) {
        Symbol bestSoFar = methodNotFound;
        bestSoFar = findMethod(env,
                          site,
                          name,
                          argtypes,
                          typeargtypes,
                          site.tsym.type,
                          bestSoFar,
                          allowBoxing,
                          useVarargs);
        return bestSoFar;
    }
    // where
    private Symbol findMethod(Env<AttrContext> env,
                              Type site,
                              Name name,
                              List<Type> argtypes,
                              List<Type> typeargtypes,
                              Type intype,
                              Symbol bestSoFar,
                              boolean allowBoxing,
                              boolean useVarargs) {
        @SuppressWarnings({"unchecked","rawtypes"})
        List<Type>[] itypes = (List<Type>[])new List[] { List.<Type>nil(), List.<Type>nil() };

        InterfaceLookupPhase iphase = InterfaceLookupPhase.ABSTRACT_OK;
        for (TypeSymbol s : superclasses(intype)) {
            bestSoFar = findMethodInScope(env, site, name, argtypes, typeargtypes,
                    s.members(), bestSoFar, allowBoxing, useVarargs, true);
            if (name == names.init) return bestSoFar;
            iphase = (iphase == null) ? null : iphase.update(s, this);
            if (iphase != null) {
                for (Type itype : types.interfaces(s.type)) {
                    itypes[iphase.ordinal()] = types.union(types.closure(itype), itypes[iphase.ordinal()]);
                }
            }
        }

        Symbol concrete = bestSoFar.kind.isValid() &&
                (bestSoFar.flags() & ABSTRACT) == 0 ?
                bestSoFar : methodNotFound;

        for (InterfaceLookupPhase iphase2 : InterfaceLookupPhase.values()) {
            //keep searching for abstract methods
            for (Type itype : itypes[iphase2.ordinal()]) {
                if (!itype.isInterface()) continue; //skip j.l.Object (included by Types.closure())
                if (iphase2 == InterfaceLookupPhase.DEFAULT_OK &&
                        (itype.tsym.flags() & DEFAULT) == 0) continue;
                bestSoFar = findMethodInScope(env, site, name, argtypes, typeargtypes,
                        itype.tsym.members(), bestSoFar, allowBoxing, useVarargs, true);
                if (concrete != bestSoFar &&
                    concrete.kind.isValid() &&
                    bestSoFar.kind.isValid() &&
                        types.isSubSignature(concrete.type, bestSoFar.type)) {
                    //this is an hack - as javac does not do full membership checks
                    //most specific ends up comparing abstract methods that might have
                    //been implemented by some concrete method in a subclass and,
                    //because of raw override, it is possible for an abstract method
                    //to be more specific than the concrete method - so we need
                    //to explicitly call that out (see CR 6178365)
                    bestSoFar = concrete;
                }
            }
        }
        return bestSoFar;
    }

    enum InterfaceLookupPhase {
        ABSTRACT_OK() {
            @Override
            InterfaceLookupPhase update(Symbol s, Resolve rs) {
                //We should not look for abstract methods if receiver is a concrete class
                //(as concrete classes are expected to implement all abstracts coming
                //from superinterfaces)
                if ((s.flags() & (ABSTRACT | INTERFACE | ENUM)) != 0) {
                    return this;
                } else {
                    return DEFAULT_OK;
                }
            }
        },
        DEFAULT_OK() {
            @Override
            InterfaceLookupPhase update(Symbol s, Resolve rs) {
                return this;
            }
        };

        abstract InterfaceLookupPhase update(Symbol s, Resolve rs);
    }

    /**
     * Return an Iterable object to scan the superclasses of a given type.
     * It's crucial that the scan is done lazily, as we don't want to accidentally
     * access more supertypes than strictly needed (as this could trigger completion
     * errors if some of the not-needed supertypes are missing/ill-formed).
     */
    Iterable<TypeSymbol> superclasses(final Type intype) {
        return () -> new Iterator<TypeSymbol>() {

            List<TypeSymbol> seen = List.nil();
            TypeSymbol currentSym = symbolFor(intype);
            TypeSymbol prevSym = null;

            public boolean hasNext() {
                if (currentSym == syms.noSymbol) {
                    currentSym = symbolFor(types.supertype(prevSym.type));
                }
                return currentSym != null;
            }

            public TypeSymbol next() {
                prevSym = currentSym;
                currentSym = syms.noSymbol;
                Assert.check(prevSym != null || prevSym != syms.noSymbol);
                return prevSym;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

            TypeSymbol symbolFor(Type t) {
                if (!t.hasTag(CLASS) &&
                        !t.hasTag(TYPEVAR)) {
                    return null;
                }
                t = types.skipTypeVars(t, false);
                if (seen.contains(t.tsym)) {
                    //degenerate case in which we have a circular
                    //class hierarchy - because of ill-formed classfiles
                    return null;
                }
                seen = seen.prepend(t.tsym);
                return t.tsym;
            }
        };
    }

    /** Find unqualified method matching given name, type and value arguments.
     *  @param env       The current environment.
     *  @param name      The method's name.
     *  @param argtypes  The method's value arguments.
     *  @param typeargtypes  The method's type arguments.
     *  @param allowBoxing Allow boxing conversions of arguments.
     *  @param useVarargs Box trailing arguments into an array for varargs.
     */
    Symbol findFun(Env<AttrContext> env, Name name,
                   List<Type> argtypes, List<Type> typeargtypes,
                   boolean allowBoxing, boolean useVarargs) {
        Symbol bestSoFar = methodNotFound;
        Env<AttrContext> env1 = env;
        boolean staticOnly = false;
        while (env1.outer != null) {
            if (isStatic(env1)) staticOnly = true;
            Assert.check(env1.info.preferredTreeForDiagnostics == null);
            env1.info.preferredTreeForDiagnostics = env.tree;
            try {
                Symbol sym = findMethod(
                    env1, env1.enclClass.sym.type, name, argtypes, typeargtypes,
                    allowBoxing, useVarargs);
                if (sym.exists()) {
                    if (staticOnly &&
                        sym.kind == MTH &&
                        sym.owner.kind == TYP &&
                        (sym.flags() & STATIC) == 0) return new StaticError(sym);
                    else return sym;
                } else {
                    bestSoFar = bestOf(bestSoFar, sym);
                }
            } finally {
                env1.info.preferredTreeForDiagnostics = null;
            }
            if ((env1.enclClass.sym.flags() & STATIC) != 0) staticOnly = true;
            env1 = env1.outer;
        }

        Symbol sym = findMethod(env, syms.predefClass.type, name, argtypes,
                                typeargtypes, allowBoxing, useVarargs);
        if (sym.exists())
            return sym;

        for (Symbol currentSym : env.toplevel.namedImportScope.getSymbolsByName(name)) {
            Symbol origin = env.toplevel.namedImportScope.getOrigin(currentSym).owner;
            if (currentSym.kind == MTH) {
                if (currentSym.owner.type != origin.type)
                    currentSym = currentSym.clone(origin);
                if (!isAccessible(env, origin.type, currentSym))
                    currentSym = new AccessError(env, origin.type, currentSym);
                bestSoFar = selectBest(env, origin.type,
                                       argtypes, typeargtypes,
                                       currentSym, bestSoFar,
                                       allowBoxing, useVarargs);
            }
        }
        if (bestSoFar.exists())
            return bestSoFar;

        for (Symbol currentSym : env.toplevel.starImportScope.getSymbolsByName(name)) {
            Symbol origin = env.toplevel.starImportScope.getOrigin(currentSym).owner;
            if (currentSym.kind == MTH) {
                if (currentSym.owner.type != origin.type)
                    currentSym = currentSym.clone(origin);
                if (!isAccessible(env, origin.type, currentSym))
                    currentSym = new AccessError(env, origin.type, currentSym);
                bestSoFar = selectBest(env, origin.type,
                                       argtypes, typeargtypes,
                                       currentSym, bestSoFar,
                                       allowBoxing, useVarargs);
            }
        }
        return bestSoFar;
    }

    /** Load toplevel or member class with given fully qualified name and
     *  verify that it is accessible.
     *  @param env       The current environment.
     *  @param name      The fully qualified name of the class to be loaded.
     */
    Symbol loadClass(Env<AttrContext> env, Name name, RecoveryLoadClass recoveryLoadClass) {
        try {
            ClassSymbol c = finder.loadClass(env.toplevel.modle, name);
            return isAccessible(env, c) ? c : new AccessError(env, null, c);
        } catch (ClassFinder.BadClassFile err) {
            return new BadClassFileError(err);
        } catch (CompletionFailure ex) {
            Symbol candidate = recoveryLoadClass.loadClass(env, name);

            if (candidate != null) {
                return candidate;
            }

            return typeNotFound;
        }
    }

    public interface RecoveryLoadClass {
        Symbol loadClass(Env<AttrContext> env, Name name);
    }

    private final RecoveryLoadClass noRecovery = (env, name) -> null;

    private final RecoveryLoadClass doRecoveryLoadClass = new RecoveryLoadClass() {
        @Override public Symbol loadClass(Env<AttrContext> env, Name name) {
            List<Name> candidates = Convert.classCandidates(name);
            return lookupInvisibleSymbol(env, name,
                                         n -> () -> createCompoundIterator(candidates,
                                                                           c -> syms.getClassesForName(c)
                                                                                    .iterator()),
                                         (ms, n) -> {
                for (Name candidate : candidates) {
                    try {
                        return finder.loadClass(ms, candidate);
                    } catch (CompletionFailure cf) {
                        //ignore
                    }
                }
                return null;
            }, sym -> sym.kind == Kind.TYP, typeNotFound);
        }
    };

    private final RecoveryLoadClass namedImportScopeRecovery = (env, name) -> {
        Scope importScope = env.toplevel.namedImportScope;
        Symbol existing = importScope.findFirst(Convert.shortName(name),
                                                sym -> sym.kind == TYP && sym.flatName() == name);

        if (existing != null) {
            return new InvisibleSymbolError(env, true, existing);
        }
        return null;
    };

    private final RecoveryLoadClass starImportScopeRecovery = (env, name) -> {
        Scope importScope = env.toplevel.starImportScope;
        Symbol existing = importScope.findFirst(Convert.shortName(name),
                                                sym -> sym.kind == TYP && sym.flatName() == name);

        if (existing != null) {
            try {
                existing = finder.loadClass(existing.packge().modle, name);

                return new InvisibleSymbolError(env, true, existing);
            } catch (CompletionFailure cf) {
                //ignore
            }
        }

        return null;
    };

    Symbol lookupPackage(Env<AttrContext> env, Name name) {
        PackageSymbol pack = syms.lookupPackage(env.toplevel.modle, name);

        if (allowModules && isImportOnDemand(env, name)) {
            if (pack.members().isEmpty()) {
                return lookupInvisibleSymbol(env, name, syms::getPackagesForName, syms::enterPackage, sym -> {
                    sym.complete();
                    return !sym.members().isEmpty();
                }, pack);
            }
        }

        return pack;
    }

    private boolean isImportOnDemand(Env<AttrContext> env, Name name) {
        if (!env.tree.hasTag(IMPORT))
            return false;

        JCTree qualid = ((JCImport) env.tree).qualid;

        if (!qualid.hasTag(SELECT))
            return false;

        if (TreeInfo.name(qualid) != names.asterisk)
            return false;

        return TreeInfo.fullName(((JCFieldAccess) qualid).selected) == name;
    }

    private <S extends Symbol> Symbol lookupInvisibleSymbol(Env<AttrContext> env,
                                                            Name name,
                                                            Function<Name, Iterable<S>> get,
                                                            BiFunction<ModuleSymbol, Name, S> load,
                                                            Predicate<S> validate,
                                                            Symbol defaultResult) {
        //even if a class/package cannot be found in the current module and among packages in modules
        //it depends on that are exported for any or this module, the class/package may exist internally
        //in some of these modules, or may exist in a module on which this module does not depend.
        //Provide better diagnostic in such cases by looking for the class in any module:
        Iterable<? extends S> candidates = get.apply(name);

        for (S sym : candidates) {
            if (validate.test(sym))
                return createInvisibleSymbolError(env, sym);
        }

        Set<ModuleSymbol> recoverableModules = new HashSet<>(syms.getAllModules());

        recoverableModules.add(syms.unnamedModule);
        recoverableModules.remove(env.toplevel.modle);

        for (ModuleSymbol ms : recoverableModules) {
            //avoid overly eager completing classes from source-based modules, as those
            //may not be completable with the current compiler settings:
            if (ms.sourceLocation == null) {
                if (ms.classLocation == null) {
                    ms = moduleFinder.findModule(ms);
                }

                if (ms.kind != ERR) {
                    S sym = load.apply(ms, name);

                    if (sym != null && validate.test(sym)) {
                        return createInvisibleSymbolError(env, sym);
                    }
                }
            }
        }

        return defaultResult;
    }

    private Symbol createInvisibleSymbolError(Env<AttrContext> env, Symbol sym) {
        if (symbolPackageVisible(env, sym)) {
            return new AccessError(env, null, sym);
        } else {
            return new InvisibleSymbolError(env, false, sym);
        }
    }

    private boolean symbolPackageVisible(Env<AttrContext> env, Symbol sym) {
        ModuleSymbol envMod = env.toplevel.modle;
        PackageSymbol symPack = sym.packge();
        return envMod == symPack.modle ||
               envMod.visiblePackages.containsKey(symPack.fullname);
    }

    /**
     * Find a type declared in a scope (not inherited).  Return null
     * if none is found.
     *  @param env       The current environment.
     *  @param site      The original type from where the selection takes
     *                   place.
     *  @param name      The type's name.
     *  @param c         The class to search for the member type. This is
     *                   always a superclass or implemented interface of
     *                   site's class.
     */
    Symbol findImmediateMemberType(Env<AttrContext> env,
                                   Type site,
                                   Name name,
                                   TypeSymbol c) {
        for (Symbol sym : c.members().getSymbolsByName(name)) {
            if (sym.kind == TYP) {
                return isAccessible(env, site, sym)
                    ? sym
                    : new AccessError(env, site, sym);
            }
        }
        return typeNotFound;
    }

    /** Find a member type inherited from a superclass or interface.
     *  @param env       The current environment.
     *  @param site      The original type from where the selection takes
     *                   place.
     *  @param name      The type's name.
     *  @param c         The class to search for the member type. This is
     *                   always a superclass or implemented interface of
     *                   site's class.
     */
    Symbol findInheritedMemberType(Env<AttrContext> env,
                                   Type site,
                                   Name name,
                                   TypeSymbol c) {
        Symbol bestSoFar = typeNotFound;
        Symbol sym;
        Type st = types.supertype(c.type);
        if (st != null && st.hasTag(CLASS)) {
            sym = findMemberType(env, site, name, st.tsym);
            bestSoFar = bestOf(bestSoFar, sym);
        }
        for (List<Type> l = types.interfaces(c.type);
             bestSoFar.kind != AMBIGUOUS && l.nonEmpty();
             l = l.tail) {
            sym = findMemberType(env, site, name, l.head.tsym);
            if (!bestSoFar.kind.isResolutionError() &&
                !sym.kind.isResolutionError() &&
                sym.owner != bestSoFar.owner)
                bestSoFar = new AmbiguityError(bestSoFar, sym);
            else
                bestSoFar = bestOf(bestSoFar, sym);
        }
        return bestSoFar;
    }

    /** Find qualified member type.
     *  @param env       The current environment.
     *  @param site      The original type from where the selection takes
     *                   place.
     *  @param name      The type's name.
     *  @param c         The class to search for the member type. This is
     *                   always a superclass or implemented interface of
     *                   site's class.
     */
    Symbol findMemberType(Env<AttrContext> env,
                          Type site,
                          Name name,
                          TypeSymbol c) {
        Symbol sym = findImmediateMemberType(env, site, name, c);

        if (sym != typeNotFound)
            return sym;

        return findInheritedMemberType(env, site, name, c);

    }

    /** Find a global type in given scope and load corresponding class.
     *  @param env       The current environment.
     *  @param scope     The scope in which to look for the type.
     *  @param name      The type's name.
     */
    Symbol findGlobalType(Env<AttrContext> env, Scope scope, Name name, RecoveryLoadClass recoveryLoadClass) {
        Symbol bestSoFar = typeNotFound;
        for (Symbol s : scope.getSymbolsByName(name)) {
            Symbol sym = loadClass(env, s.flatName(), recoveryLoadClass);
            if (bestSoFar.kind == TYP && sym.kind == TYP &&
                bestSoFar != sym)
                return new AmbiguityError(bestSoFar, sym);
            else
                bestSoFar = bestOf(bestSoFar, sym);
        }
        return bestSoFar;
    }

    Symbol findTypeVar(Env<AttrContext> env, Name name, boolean staticOnly) {
        for (Symbol sym : env.info.scope.getSymbolsByName(name)) {
            if (sym.kind == TYP) {
                if (sym.type.hasTag(TYPEVAR) &&
                        (staticOnly || (isStatic(env) && sym.owner.kind == TYP)))
                    // if staticOnly is set, it means that we have recursed through a static declaration,
                    // so type variable symbols should not be accessible. If staticOnly is unset, but
                    // we are in a static declaration (field or method), we should not allow type-variables
                    // defined in the enclosing class to "leak" into this context.
                    return new StaticError(sym);
                return sym;
            }
        }
        return typeNotFound;
    }

    /** Find an unqualified type symbol.
     *  @param env       The current environment.
     *  @param name      The type's name.
     */
    Symbol findType(Env<AttrContext> env, Name name) {
        if (name == names.empty)
            return typeNotFound; // do not allow inadvertent "lookup" of anonymous types
        Symbol bestSoFar = typeNotFound;
        Symbol sym;
        boolean staticOnly = false;
        for (Env<AttrContext> env1 = env; env1.outer != null; env1 = env1.outer) {
            // First, look for a type variable and the first member type
            final Symbol tyvar = findTypeVar(env1, name, staticOnly);
            if (isStatic(env1)) staticOnly = true;
            sym = findImmediateMemberType(env1, env1.enclClass.sym.type,
                                          name, env1.enclClass.sym);

            // Return the type variable if we have it, and have no
            // immediate member, OR the type variable is for a method.
            if (tyvar != typeNotFound) {
                if (env.baseClause || sym == typeNotFound ||
                    (tyvar.kind == TYP && tyvar.exists() &&
                     tyvar.owner.kind == MTH)) {
                    return tyvar;
                }
            }

            // If the environment is a class def, finish up,
            // otherwise, do the entire findMemberType
            if (sym == typeNotFound)
                sym = findInheritedMemberType(env1, env1.enclClass.sym.type,
                                              name, env1.enclClass.sym);

            if (staticOnly && sym.kind == TYP &&
                sym.type.hasTag(CLASS) &&
                sym.type.getEnclosingType().hasTag(CLASS) &&
                env1.enclClass.sym.type.isParameterized() &&
                sym.type.getEnclosingType().isParameterized())
                return new StaticError(sym);
            else if (sym.exists()) return sym;
            else bestSoFar = bestOf(bestSoFar, sym);

            JCClassDecl encl = env1.baseClause ? (JCClassDecl)env1.tree : env1.enclClass;
            if ((encl.sym.flags() & STATIC) != 0)
                staticOnly = true;
        }

        if (!env.tree.hasTag(IMPORT)) {
            sym = findGlobalType(env, env.toplevel.namedImportScope, name, namedImportScopeRecovery);
            if (sym.exists()) return sym;
            else bestSoFar = bestOf(bestSoFar, sym);

            sym = findGlobalType(env, env.toplevel.toplevelScope, name, noRecovery);
            if (sym.exists()) return sym;
            else bestSoFar = bestOf(bestSoFar, sym);

            sym = findGlobalType(env, env.toplevel.packge.members(), name, noRecovery);
            if (sym.exists()) return sym;
            else bestSoFar = bestOf(bestSoFar, sym);

            sym = findGlobalType(env, env.toplevel.starImportScope, name, starImportScopeRecovery);
            if (sym.exists()) return sym;
            else bestSoFar = bestOf(bestSoFar, sym);
        }

        return bestSoFar;
    }

    /** Find an unqualified identifier which matches a specified kind set.
     *  @param pos       position on which report warnings, if any;
     *                   null warnings should not be reported
     *  @param env       The current environment.
     *  @param name      The identifier's name.
     *  @param kind      Indicates the possible symbol kinds
     *                   (a subset of VAL, TYP, PCK).
     */
    Symbol findIdent(DiagnosticPosition pos, Env<AttrContext> env, Name name, KindSelector kind) {
        return checkRestrictedType(pos, findIdentInternal(env, name, kind), name);
    }

    Symbol findIdentInternal(Env<AttrContext> env, Name name, KindSelector kind) {
        Symbol bestSoFar = typeNotFound;
        Symbol sym;

        if (kind.contains(KindSelector.VAL)) {
            sym = findVar(env, name);
            if (sym.exists()) return sym;
            else bestSoFar = bestOf(bestSoFar, sym);
        }

        if (kind.contains(KindSelector.TYP)) {
            sym = findType(env, name);

            if (sym.exists()) return sym;
            else bestSoFar = bestOf(bestSoFar, sym);
        }

        if (kind.contains(KindSelector.PCK))
            return lookupPackage(env, name);
        else return bestSoFar;
    }

    /** Find an identifier in a package which matches a specified kind set.
     *  @param pos       position on which report warnings, if any;
     *                   null warnings should not be reported
     *  @param env       The current environment.
     *  @param name      The identifier's name.
     *  @param kind      Indicates the possible symbol kinds
     *                   (a nonempty subset of TYP, PCK).
     */
    Symbol findIdentInPackage(DiagnosticPosition pos,
                              Env<AttrContext> env, TypeSymbol pck,
                              Name name, KindSelector kind) {
        return checkRestrictedType(pos, findIdentInPackageInternal(env, pck, name, kind), name);
    }

    Symbol findIdentInPackageInternal(Env<AttrContext> env, TypeSymbol pck,
                              Name name, KindSelector kind) {
        Name fullname = TypeSymbol.formFullName(name, pck);
        Symbol bestSoFar = typeNotFound;
        if (kind.contains(KindSelector.TYP)) {
            RecoveryLoadClass recoveryLoadClass =
                    allowModules && !kind.contains(KindSelector.PCK) &&
                    !pck.exists() && !env.info.attributionMode.isSpeculative ?
                        doRecoveryLoadClass : noRecovery;
            Symbol sym = loadClass(env, fullname, recoveryLoadClass);
            if (sym.exists()) {
                // don't allow programs to use flatnames
                if (name == sym.name) return sym;
            }
            else bestSoFar = bestOf(bestSoFar, sym);
        }
        if (kind.contains(KindSelector.PCK)) {
            return lookupPackage(env, fullname);
        }
        return bestSoFar;
    }

    /** Find an identifier among the members of a given type `site'.
     *  @param pos       position on which report warnings, if any;
     *                   null warnings should not be reported
     *  @param env       The current environment.
     *  @param site      The type containing the symbol to be found.
     *  @param name      The identifier's name.
     *  @param kind      Indicates the possible symbol kinds
     *                   (a subset of VAL, TYP).
     */
    Symbol findIdentInType(DiagnosticPosition pos,
                           Env<AttrContext> env, Type site,
                           Name name, KindSelector kind) {
        return checkRestrictedType(pos, findIdentInTypeInternal(env, site, name, kind), name);
    }

    Symbol findIdentInTypeInternal(Env<AttrContext> env, Type site,
                           Name name, KindSelector kind) {
        Symbol bestSoFar = typeNotFound;
        Symbol sym;
        if (kind.contains(KindSelector.VAL)) {
            sym = findField(env, site, name, site.tsym);
            if (sym.exists()) return sym;
            else bestSoFar = bestOf(bestSoFar, sym);
        }

        if (kind.contains(KindSelector.TYP)) {
            sym = findMemberType(env, site, name, site.tsym);
            if (sym.exists()) return sym;
            else bestSoFar = bestOf(bestSoFar, sym);
        }
        return bestSoFar;
    }

    private Symbol checkRestrictedType(DiagnosticPosition pos, Symbol bestSoFar, Name name) {
        if (bestSoFar.kind == TYP || bestSoFar.kind == ABSENT_TYP) {
            if (allowLocalVariableTypeInference && name.equals(names.var)) {
                bestSoFar = new BadRestrictedTypeError(names.var);
            } else if (name.equals(names.yield)) {
                if (allowYieldStatement) {
                    bestSoFar = new BadRestrictedTypeError(names.yield);
                } else if (pos != null) {
                    log.warning(pos, Warnings.IllegalRefToRestrictedType(names.yield));
                }
            }
        }
        return bestSoFar;
    }

/* ***************************************************************************
 *  Access checking
 *  The following methods convert ResolveErrors to ErrorSymbols, issuing
 *  an error message in the process
 ****************************************************************************/

    /** If `sym' is a bad symbol: report error and return errSymbol
     *  else pass through unchanged,
     *  additional arguments duplicate what has been used in trying to find the
     *  symbol {@literal (--> flyweight pattern)}. This improves performance since we
     *  expect misses to happen frequently.
     *
     *  @param sym       The symbol that was found, or a ResolveError.
     *  @param pos       The position to use for error reporting.
     *  @param location  The symbol the served as a context for this lookup
     *  @param site      The original type from where the selection took place.
     *  @param name      The symbol's name.
     *  @param qualified Did we get here through a qualified expression resolution?
     *  @param argtypes  The invocation's value arguments,
     *                   if we looked for a method.
     *  @param typeargtypes  The invocation's type arguments,
     *                   if we looked for a method.
     *  @param logResolveHelper helper class used to log resolve errors
     */
    Symbol accessInternal(Symbol sym,
                  DiagnosticPosition pos,
                  Symbol location,
                  Type site,
                  Name name,
                  boolean qualified,
                  List<Type> argtypes,
                  List<Type> typeargtypes,
                  LogResolveHelper logResolveHelper) {
        if (sym.kind.isResolutionError()) {
            ResolveError errSym = (ResolveError)sym.baseSymbol();
            sym = errSym.access(name, qualified ? site.tsym : syms.noSymbol);
            argtypes = logResolveHelper.getArgumentTypes(errSym, sym, name, argtypes);
            if (logResolveHelper.resolveDiagnosticNeeded(site, argtypes, typeargtypes)) {
                logResolveError(errSym, pos, location, site, name, argtypes, typeargtypes);
            }
        }
        return sym;
    }

    /**
     * Variant of the generalized access routine, to be used for generating method
     * resolution diagnostics
     */
    Symbol accessMethod(Symbol sym,
                  DiagnosticPosition pos,
                  Symbol location,
                  Type site,
                  Name name,
                  boolean qualified,
                  List<Type> argtypes,
                  List<Type> typeargtypes) {
        return accessInternal(sym, pos, location, site, name, qualified, argtypes, typeargtypes, methodLogResolveHelper);
    }

    /** Same as original accessMethod(), but without location.
     */
    Symbol accessMethod(Symbol sym,
                  DiagnosticPosition pos,
                  Type site,
                  Name name,
                  boolean qualified,
                  List<Type> argtypes,
                  List<Type> typeargtypes) {
        return accessMethod(sym, pos, site.tsym, site, name, qualified, argtypes, typeargtypes);
    }

    /**
     * Variant of the generalized access routine, to be used for generating variable,
     * type resolution diagnostics
     */
    Symbol accessBase(Symbol sym,
                  DiagnosticPosition pos,
                  Symbol location,
                  Type site,
                  Name name,
                  boolean qualified) {
        return accessInternal(sym, pos, location, site, name, qualified, List.nil(), null, basicLogResolveHelper);
    }

    /** Same as original accessBase(), but without location.
     */
    Symbol accessBase(Symbol sym,
                  DiagnosticPosition pos,
                  Type site,
                  Name name,
                  boolean qualified) {
        return accessBase(sym, pos, site.tsym, site, name, qualified);
    }

    interface LogResolveHelper {
        boolean resolveDiagnosticNeeded(Type site, List<Type> argtypes, List<Type> typeargtypes);
        List<Type> getArgumentTypes(ResolveError errSym, Symbol accessedSym, Name name, List<Type> argtypes);
    }

    LogResolveHelper basicLogResolveHelper = new LogResolveHelper() {
        public boolean resolveDiagnosticNeeded(Type site, List<Type> argtypes, List<Type> typeargtypes) {
            return !site.isErroneous();
        }
        public List<Type> getArgumentTypes(ResolveError errSym, Symbol accessedSym, Name name, List<Type> argtypes) {
            return argtypes;
        }
    };

    LogResolveHelper silentLogResolveHelper = new LogResolveHelper() {
        public boolean resolveDiagnosticNeeded(Type site, List<Type> argtypes, List<Type> typeargtypes) {
            return false;
        }
        public List<Type> getArgumentTypes(ResolveError errSym, Symbol accessedSym, Name name, List<Type> argtypes) {
            return argtypes;
        }
    };

    LogResolveHelper methodLogResolveHelper = new LogResolveHelper() {
        public boolean resolveDiagnosticNeeded(Type site, List<Type> argtypes, List<Type> typeargtypes) {
            return !site.isErroneous() &&
                        !Type.isErroneous(argtypes) &&
                        (typeargtypes == null || !Type.isErroneous(typeargtypes));
        }
        public List<Type> getArgumentTypes(ResolveError errSym, Symbol accessedSym, Name name, List<Type> argtypes) {
            return argtypes.map(new ResolveDeferredRecoveryMap(AttrMode.SPECULATIVE, accessedSym, currentResolutionContext.step));
        }
    };

    class ResolveDeferredRecoveryMap extends DeferredAttr.RecoveryDeferredTypeMap {

        public ResolveDeferredRecoveryMap(AttrMode mode, Symbol msym, MethodResolutionPhase step) {
            deferredAttr.super(mode, msym, step);
        }

        @Override
        protected Type typeOf(DeferredType dt, Type pt) {
            Type res = super.typeOf(dt, pt);
            if (!res.isErroneous()) {
                switch (TreeInfo.skipParens(dt.tree).getTag()) {
                    case LAMBDA:
                    case REFERENCE:
                        return dt;
                    case CONDEXPR:
                        return res == Type.recoveryType ?
                                dt : res;
                }
            }
            return res;
        }
    }

    /** Check that sym is not an abstract method.
     */
    void checkNonAbstract(DiagnosticPosition pos, Symbol sym) {
        if ((sym.flags() & ABSTRACT) != 0 && (sym.flags() & DEFAULT) == 0)
            log.error(pos,
                      Errors.AbstractCantBeAccessedDirectly(kindName(sym),sym, sym.location()));
    }

/* ***************************************************************************
 *  Name resolution
 *  Naming conventions are as for symbol lookup
 *  Unlike the find... methods these methods will report access errors
 ****************************************************************************/

    /** Resolve an unqualified (non-method) identifier.
     *  @param pos       The position to use for error reporting.
     *  @param env       The environment current at the identifier use.
     *  @param name      The identifier's name.
     *  @param kind      The set of admissible symbol kinds for the identifier.
     */
    Symbol resolveIdent(DiagnosticPosition pos, Env<AttrContext> env,
                        Name name, KindSelector kind) {
        return accessBase(
            findIdent(pos, env, name, kind),
            pos, env.enclClass.sym.type, name, false);
    }

    /** Resolve an unqualified method identifier.
     *  @param pos       The position to use for error reporting.
     *  @param env       The environment current at the method invocation.
     *  @param name      The identifier's name.
     *  @param argtypes  The types of the invocation's value arguments.
     *  @param typeargtypes  The types of the invocation's type arguments.
     */
    Symbol resolveMethod(DiagnosticPosition pos,
                         Env<AttrContext> env,
                         Name name,
                         List<Type> argtypes,
                         List<Type> typeargtypes) {
        return lookupMethod(env, pos, env.enclClass.sym, resolveMethodCheck,
                new BasicLookupHelper(name, env.enclClass.sym.type, argtypes, typeargtypes) {
                    @Override
                    Symbol doLookup(Env<AttrContext> env, MethodResolutionPhase phase) {
                        return findFun(env, name, argtypes, typeargtypes,
                                phase.isBoxingRequired(),
                                phase.isVarargsRequired());
                    }});
    }

    /** Resolve a qualified method identifier
     *  @param pos       The position to use for error reporting.
     *  @param env       The environment current at the method invocation.
     *  @param site      The type of the qualifying expression, in which
     *                   identifier is searched.
     *  @param name      The identifier's name.
     *  @param argtypes  The types of the invocation's value arguments.
     *  @param typeargtypes  The types of the invocation's type arguments.
     */
    Symbol resolveQualifiedMethod(DiagnosticPosition pos, Env<AttrContext> env,
                                  Type site, Name name, List<Type> argtypes,
                                  List<Type> typeargtypes) {
        return resolveQualifiedMethod(pos, env, site.tsym, site, name, argtypes, typeargtypes);
    }
    Symbol resolveQualifiedMethod(DiagnosticPosition pos, Env<AttrContext> env,
                                  Symbol location, Type site, Name name, List<Type> argtypes,
                                  List<Type> typeargtypes) {
        return resolveQualifiedMethod(new MethodResolutionContext(), pos, env, location, site, name, argtypes, typeargtypes);
    }
    private Symbol resolveQualifiedMethod(MethodResolutionContext resolveContext,
                                  DiagnosticPosition pos, Env<AttrContext> env,
                                  Symbol location, Type site, Name name, List<Type> argtypes,
                                  List<Type> typeargtypes) {
        return lookupMethod(env, pos, location, resolveContext, new BasicLookupHelper(name, site, argtypes, typeargtypes) {
            @Override
            Symbol doLookup(Env<AttrContext> env, MethodResolutionPhase phase) {
                return findMethod(env, site, name, argtypes, typeargtypes,
                        phase.isBoxingRequired(),
                        phase.isVarargsRequired());
            }
            @Override
            Symbol access(Env<AttrContext> env, DiagnosticPosition pos, Symbol location, Symbol sym) {
                if (sym.kind.isResolutionError()) {
                    sym = super.access(env, pos, location, sym);
                } else {
                    MethodSymbol msym = (MethodSymbol)sym;
                    if ((msym.flags() & SIGNATURE_POLYMORPHIC) != 0) {
                        env.info.pendingResolutionPhase = BASIC;
                        return findPolymorphicSignatureInstance(env, sym, argtypes);
                    }
                }
                return sym;
            }
        });
    }

    /** Find or create an implicit method of exactly the given type (after erasure).
     *  Searches in a side table, not the main scope of the site.
     *  This emulates the lookup process required by JSR 292 in JVM.
     *  @param env       Attribution environment
     *  @param spMethod  signature polymorphic method - i.e. MH.invokeExact
     *  @param argtypes  The required argument types
     */
    Symbol findPolymorphicSignatureInstance(Env<AttrContext> env,
                                            final Symbol spMethod,
                                            List<Type> argtypes) {
        Type mtype = infer.instantiatePolymorphicSignatureInstance(env,
                (MethodSymbol)spMethod, currentResolutionContext, argtypes);
        return findPolymorphicSignatureInstance(spMethod, mtype);
    }

    Symbol findPolymorphicSignatureInstance(final Symbol spMethod,
                                            Type mtype) {
        for (Symbol sym : polymorphicSignatureScope.getSymbolsByName(spMethod.name)) {
            // Check that there is already a method symbol for the method
            // type and owner
            if (types.isSameType(mtype, sym.type) &&
                spMethod.owner == sym.owner) {
                return sym;
            }
        }

        Type spReturnType = spMethod.asType().getReturnType();
        if (types.isSameType(spReturnType, syms.objectType)) {
            // Polymorphic return, pass through mtype
        } else if (!types.isSameType(spReturnType, mtype.getReturnType())) {
            // Retain the sig poly method's return type, which differs from that of mtype
            // Will result in an incompatible return type error
            mtype = new MethodType(mtype.getParameterTypes(),
                    spReturnType,
                    mtype.getThrownTypes(),
                    syms.methodClass);
        }

        // Create the desired method
        // Retain static modifier is to support invocations to
        // MethodHandle.linkTo* methods
        long flags = ABSTRACT | HYPOTHETICAL |
                     spMethod.flags() & (Flags.AccessFlags | Flags.STATIC);
        Symbol msym = new MethodSymbol(flags, spMethod.name, mtype, spMethod.owner) {
            @Override
            public Symbol baseSymbol() {
                return spMethod;
            }
        };
        if (!mtype.isErroneous()) { // Cache only if kosher.
            polymorphicSignatureScope.enter(msym);
        }
        return msym;
    }

    /** Resolve a qualified method identifier, throw a fatal error if not
     *  found.
     *  @param pos       The position to use for error reporting.
     *  @param env       The environment current at the method invocation.
     *  @param site      The type of the qualifying expression, in which
     *                   identifier is searched.
     *  @param name      The identifier's name.
     *  @param argtypes  The types of the invocation's value arguments.
     *  @param typeargtypes  The types of the invocation's type arguments.
     */
    public MethodSymbol resolveInternalMethod(DiagnosticPosition pos, Env<AttrContext> env,
                                        Type site, Name name,
                                        List<Type> argtypes,
                                        List<Type> typeargtypes) {
        MethodResolutionContext resolveContext = new MethodResolutionContext();
        resolveContext.internalResolution = true;
        Symbol sym = resolveQualifiedMethod(resolveContext, pos, env, site.tsym,
                site, name, argtypes, typeargtypes);
        if (sym.kind == MTH) return (MethodSymbol)sym;
        else throw new FatalError(
                 diags.fragment(Fragments.FatalErrCantLocateMeth(name)));
    }

    /** Resolve constructor.
     *  @param pos       The position to use for error reporting.
     *  @param env       The environment current at the constructor invocation.
     *  @param site      The type of class for which a constructor is searched.
     *  @param argtypes  The types of the constructor invocation's value
     *                   arguments.
     *  @param typeargtypes  The types of the constructor invocation's type
     *                   arguments.
     */
    Symbol resolveConstructor(DiagnosticPosition pos,
                              Env<AttrContext> env,
                              Type site,
                              List<Type> argtypes,
                              List<Type> typeargtypes) {
        return resolveConstructor(new MethodResolutionContext(), pos, env, site, argtypes, typeargtypes);
    }

    private Symbol resolveConstructor(MethodResolutionContext resolveContext,
                              final DiagnosticPosition pos,
                              Env<AttrContext> env,
                              Type site,
                              List<Type> argtypes,
                              List<Type> typeargtypes) {
        return lookupMethod(env, pos, site.tsym, resolveContext, new BasicLookupHelper(names.init, site, argtypes, typeargtypes) {
            @Override
            Symbol doLookup(Env<AttrContext> env, MethodResolutionPhase phase) {
                return findConstructor(pos, env, site, argtypes, typeargtypes,
                        phase.isBoxingRequired(),
                        phase.isVarargsRequired());
            }
        });
    }

    /** Resolve a constructor, throw a fatal error if not found.
     *  @param pos       The position to use for error reporting.
     *  @param env       The environment current at the method invocation.
     *  @param site      The type to be constructed.
     *  @param argtypes  The types of the invocation's value arguments.
     *  @param typeargtypes  The types of the invocation's type arguments.
     */
    public MethodSymbol resolveInternalConstructor(DiagnosticPosition pos, Env<AttrContext> env,
                                        Type site,
                                        List<Type> argtypes,
                                        List<Type> typeargtypes) {
        MethodResolutionContext resolveContext = new MethodResolutionContext();
        resolveContext.internalResolution = true;
        Symbol sym = resolveConstructor(resolveContext, pos, env, site, argtypes, typeargtypes);
        if (sym.kind == MTH) return (MethodSymbol)sym;
        else throw new FatalError(
                 diags.fragment(Fragments.FatalErrCantLocateCtor(site)));
    }

    Symbol findConstructor(DiagnosticPosition pos, Env<AttrContext> env,
                              Type site, List<Type> argtypes,
                              List<Type> typeargtypes,
                              boolean allowBoxing,
                              boolean useVarargs) {
        Symbol sym = findMethod(env, site,
                                    names.init, argtypes,
                                    typeargtypes, allowBoxing,
                                    useVarargs);
        chk.checkDeprecated(pos, env.info.scope.owner, sym);
        chk.checkPreview(pos, env.info.scope.owner, sym);
        return sym;
    }

    /** Resolve constructor using diamond inference.
     *  @param pos       The position to use for error reporting.
     *  @param env       The environment current at the constructor invocation.
     *  @param site      The type of class for which a constructor is searched.
     *                   The scope of this class has been touched in attribution.
     *  @param argtypes  The types of the constructor invocation's value
     *                   arguments.
     *  @param typeargtypes  The types of the constructor invocation's type
     *                   arguments.
     */
    Symbol resolveDiamond(DiagnosticPosition pos,
                              Env<AttrContext> env,
                              Type site,
                              List<Type> argtypes,
                              List<Type> typeargtypes) {
        return lookupMethod(env, pos, site.tsym, resolveMethodCheck,
                new BasicLookupHelper(names.init, site, argtypes, typeargtypes) {
                    @Override
                    Symbol doLookup(Env<AttrContext> env, MethodResolutionPhase phase) {
                        return findDiamond(pos, env, site, argtypes, typeargtypes,
                                phase.isBoxingRequired(),
                                phase.isVarargsRequired());
                    }
                    @Override
                    Symbol access(Env<AttrContext> env, DiagnosticPosition pos, Symbol location, Symbol sym) {
                        if (sym.kind.isResolutionError()) {
                            if (sym.kind != WRONG_MTH &&
                                sym.kind != WRONG_MTHS) {
                                sym = super.access(env, pos, location, sym);
                            } else {
                                final JCDiagnostic details = sym.kind == WRONG_MTH ?
                                                ((InapplicableSymbolError)sym.baseSymbol()).errCandidate().snd :
                                                null;
                                sym = new DiamondError(sym, currentResolutionContext);
                                sym = accessMethod(sym, pos, site, names.init, true, argtypes, typeargtypes);
                                env.info.pendingResolutionPhase = currentResolutionContext.step;
                            }
                        }
                        return sym;
                    }});
    }

    /** Find the constructor using diamond inference and do some checks(deprecated and preview).
     *  @param pos          The position to use for error reporting.
     *  @param env          The environment current at the constructor invocation.
     *  @param site         The type of class for which a constructor is searched.
     *                      The scope of this class has been touched in attribution.
     *  @param argtypes     The types of the constructor invocation's value arguments.
     *  @param typeargtypes The types of the constructor invocation's type arguments.
     *  @param allowBoxing  Allow boxing conversions of arguments.
     *  @param useVarargs   Box trailing arguments into an array for varargs.
     */
    private Symbol findDiamond(DiagnosticPosition pos,
                               Env<AttrContext> env,
                               Type site,
                               List<Type> argtypes,
                               List<Type> typeargtypes,
                               boolean allowBoxing,
                               boolean useVarargs) {
        Symbol sym = findDiamond(env, site, argtypes, typeargtypes, allowBoxing, useVarargs);
        chk.checkDeprecated(pos, env.info.scope.owner, sym);
        chk.checkPreview(pos, env.info.scope.owner, sym);
        return sym;
    }

    /** This method scans all the constructor symbol in a given class scope -
     *  assuming that the original scope contains a constructor of the kind:
     *  {@code Foo(X x, Y y)}, where X,Y are class type-variables declared in Foo,
     *  a method check is executed against the modified constructor type:
     *  {@code <X,Y>Foo<X,Y>(X x, Y y)}. This is crucial in order to enable diamond
     *  inference. The inferred return type of the synthetic constructor IS
     *  the inferred type for the diamond operator.
     */
    private Symbol findDiamond(Env<AttrContext> env,
                              Type site,
                              List<Type> argtypes,
                              List<Type> typeargtypes,
                              boolean allowBoxing,
                              boolean useVarargs) {
        Symbol bestSoFar = methodNotFound;
        TypeSymbol tsym = site.tsym.isInterface() ? syms.objectType.tsym : site.tsym;
        for (final Symbol sym : tsym.members().getSymbolsByName(names.init)) {
            //- System.out.println(" e " + e.sym);
            if (sym.kind == MTH &&
                (sym.flags_field & SYNTHETIC) == 0) {
                    List<Type> oldParams = sym.type.hasTag(FORALL) ?
                            ((ForAll)sym.type).tvars :
                            List.nil();
                    Type constrType = new ForAll(site.tsym.type.getTypeArguments().appendList(oldParams),
                                                 types.createMethodTypeWithReturn(sym.type.asMethodType(), site));
                    MethodSymbol newConstr = new MethodSymbol(sym.flags(), names.init, constrType, site.tsym) {
                        @Override
                        public Symbol baseSymbol() {
                            return sym;
                        }
                    };
                    bestSoFar = selectBest(env, site, argtypes, typeargtypes,
                            newConstr,
                            bestSoFar,
                            allowBoxing,
                            useVarargs);
            }
        }
        return bestSoFar;
    }

    Symbol getMemberReference(DiagnosticPosition pos,
            Env<AttrContext> env,
            JCMemberReference referenceTree,
            Type site,
            Name name) {

        site = types.capture(site);

        ReferenceLookupHelper lookupHelper = makeReferenceLookupHelper(
                referenceTree, site, name, List.nil(), null, VARARITY);

        Env<AttrContext> newEnv = env.dup(env.tree, env.info.dup());
        Symbol sym = lookupMethod(newEnv, env.tree.pos(), site.tsym,
                nilMethodCheck, lookupHelper);

        env.info.pendingResolutionPhase = newEnv.info.pendingResolutionPhase;

        return sym;
    }

    ReferenceLookupHelper makeReferenceLookupHelper(JCMemberReference referenceTree,
                                  Type site,
                                  Name name,
                                  List<Type> argtypes,
                                  List<Type> typeargtypes,
                                  MethodResolutionPhase maxPhase) {
        if (!name.equals(names.init)) {
            //method reference
            return new MethodReferenceLookupHelper(referenceTree, name, site, argtypes, typeargtypes, maxPhase);
        } else if (site.hasTag(ARRAY)) {
            //array constructor reference
            return new ArrayConstructorReferenceLookupHelper(referenceTree, site, argtypes, typeargtypes, maxPhase);
        } else {
            //class constructor reference
            return new ConstructorReferenceLookupHelper(referenceTree, site, argtypes, typeargtypes, maxPhase);
        }
    }

    /**
     * Resolution of member references is typically done as a single
     * overload resolution step, where the argument types A are inferred from
     * the target functional descriptor.
     *
     * If the member reference is a method reference with a type qualifier,
     * a two-step lookup process is performed. The first step uses the
     * expected argument list A, while the second step discards the first
     * type from A (which is treated as a receiver type).
     *
     * There are two cases in which inference is performed: (i) if the member
     * reference is a constructor reference and the qualifier type is raw - in
     * which case diamond inference is used to infer a parameterization for the
     * type qualifier; (ii) if the member reference is an unbound reference
     * where the type qualifier is raw - in that case, during the unbound lookup
     * the receiver argument type is used to infer an instantiation for the raw
     * qualifier type.
     *
     * When a multi-step resolution process is exploited, the process of picking
     * the resulting symbol is delegated to an helper class {@link com.sun.tools.javac.comp.Resolve.ReferenceChooser}.
     *
     * This routine returns a pair (T,S), where S is the member reference symbol,
     * and T is the type of the class in which S is defined. This is necessary as
     * the type T might be dynamically inferred (i.e. if constructor reference
     * has a raw qualifier).
     */
    Pair<Symbol, ReferenceLookupHelper> resolveMemberReference(Env<AttrContext> env,
                                  JCMemberReference referenceTree,
                                  Type site,
                                  Name name,
                                  List<Type> argtypes,
                                  List<Type> typeargtypes,
                                  Type descriptor,
                                  MethodCheck methodCheck,
                                  InferenceContext inferenceContext,
                                  ReferenceChooser referenceChooser) {

        //step 1 - bound lookup
        ReferenceLookupHelper boundLookupHelper = makeReferenceLookupHelper(
                referenceTree, site, name, argtypes, typeargtypes, VARARITY);
        Env<AttrContext> boundEnv = env.dup(env.tree, env.info.dup());
        MethodResolutionContext boundSearchResolveContext = new MethodResolutionContext();
        boundSearchResolveContext.methodCheck = methodCheck;
        Symbol boundSym = lookupMethod(boundEnv, env.tree.pos(),
                site.tsym, boundSearchResolveContext, boundLookupHelper);
        ReferenceLookupResult boundRes = new ReferenceLookupResult(boundSym, boundSearchResolveContext);
        if (dumpMethodReferenceSearchResults) {
            dumpMethodReferenceSearchResults(referenceTree, boundSearchResolveContext, boundSym, true);
        }

        //step 2 - unbound lookup
        Symbol unboundSym = methodNotFound;
        Env<AttrContext> unboundEnv = env.dup(env.tree, env.info.dup());
        ReferenceLookupHelper unboundLookupHelper = boundLookupHelper.unboundLookup(inferenceContext);
        ReferenceLookupResult unboundRes = referenceNotFound;
        if (unboundLookupHelper != null) {
            MethodResolutionContext unboundSearchResolveContext =
                    new MethodResolutionContext();
            unboundSearchResolveContext.methodCheck = methodCheck;
            unboundSym = lookupMethod(unboundEnv, env.tree.pos(),
                    site.tsym, unboundSearchResolveContext, unboundLookupHelper);
            unboundRes = new ReferenceLookupResult(unboundSym, unboundSearchResolveContext);
            if (dumpMethodReferenceSearchResults) {
                dumpMethodReferenceSearchResults(referenceTree, unboundSearchResolveContext, unboundSym, false);
            }
        }

        //merge results
        Pair<Symbol, ReferenceLookupHelper> res;
        ReferenceLookupResult bestRes = referenceChooser.result(boundRes, unboundRes);
        res = new Pair<>(bestRes.sym,
                bestRes == unboundRes ? unboundLookupHelper : boundLookupHelper);
        env.info.pendingResolutionPhase = bestRes == unboundRes ?
                unboundEnv.info.pendingResolutionPhase :
                boundEnv.info.pendingResolutionPhase;

        if (!res.fst.kind.isResolutionError()) {
            //handle sigpoly method references
            MethodSymbol msym = (MethodSymbol)res.fst;
            if ((msym.flags() & SIGNATURE_POLYMORPHIC) != 0) {
                env.info.pendingResolutionPhase = BASIC;
                res = new Pair<>(findPolymorphicSignatureInstance(msym, descriptor), res.snd);
            }
        }

        return res;
    }

    private void dumpMethodReferenceSearchResults(JCMemberReference referenceTree,
                                                  MethodResolutionContext resolutionContext,
                                                  Symbol bestSoFar,
                                                  boolean bound) {
        ListBuffer<JCDiagnostic> subDiags = new ListBuffer<>();
        int pos = 0;
        int mostSpecificPos = -1;
        for (Candidate c : resolutionContext.candidates) {
            if (resolutionContext.step != c.step || !c.isApplicable()) {
                continue;
            } else {
                JCDiagnostic subDiag = null;
                if (c.sym.type.hasTag(FORALL)) {
                    subDiag = diags.fragment(Fragments.PartialInstSig(c.mtype));
                }

                String key = subDiag == null ?
                        "applicable.method.found.2" :
                        "applicable.method.found.3";
                subDiags.append(diags.fragment(key, pos,
                        c.sym.isStatic() ? Fragments.Static : Fragments.NonStatic, c.sym, subDiag));
                if (c.sym == bestSoFar)
                    mostSpecificPos = pos;
                pos++;
            }
        }
        JCDiagnostic main = diags.note(
                log.currentSource(),
                referenceTree,
                "method.ref.search.results.multi",
                bound ? Fragments.Bound : Fragments.Unbound,
                referenceTree.toString(), mostSpecificPos);
        JCDiagnostic d = new JCDiagnostic.MultilineDiagnostic(main, subDiags.toList());
        log.report(d);
    }

    /**
     * This class is used to represent a method reference lookup result. It keeps track of two
     * things: (i) the symbol found during a method reference lookup and (ii) the static kind
     * of the lookup (see {@link com.sun.tools.javac.comp.Resolve.ReferenceLookupResult.StaticKind}).
     */
    static class ReferenceLookupResult {

        /**
         * Static kind associated with a method reference lookup. Erroneous lookups end up with
         * the UNDEFINED kind; successful lookups will end up with either STATIC, NON_STATIC,
         * depending on whether all applicable candidates are static or non-static methods,
         * respectively. If a successful lookup has both static and non-static applicable methods,
         * its kind is set to BOTH.
         */
        enum StaticKind {
            STATIC,
            NON_STATIC,
            BOTH,
            UNDEFINED;

            /**
             * Retrieve the static kind associated with a given (method) symbol.
             */
            static StaticKind from(Symbol s) {
                return s.isStatic() ?
                        STATIC : NON_STATIC;
            }

            /**
             * Merge two static kinds together.
             */
            static StaticKind reduce(StaticKind sk1, StaticKind sk2) {
                if (sk1 == UNDEFINED) {
                    return sk2;
                } else if (sk2 == UNDEFINED) {
                    return sk1;
                } else {
                    return sk1 == sk2 ? sk1 : BOTH;
                }
            }
        }

        /** The static kind. */
        StaticKind staticKind;

        /** The lookup result. */
        Symbol sym;

        ReferenceLookupResult(Symbol sym, MethodResolutionContext resolutionContext) {
            this(sym, staticKind(sym, resolutionContext));
        }

        private ReferenceLookupResult(Symbol sym, StaticKind staticKind) {
            this.staticKind = staticKind;
            this.sym = sym;
        }

        private static StaticKind staticKind(Symbol sym, MethodResolutionContext resolutionContext) {
            switch (sym.kind) {
                case MTH:
                case AMBIGUOUS:
                    return resolutionContext.candidates.stream()
                            .filter(c -> c.isApplicable() && c.step == resolutionContext.step)
                            .map(c -> StaticKind.from(c.sym))
                            .reduce(StaticKind::reduce)
                            .orElse(StaticKind.UNDEFINED);
                default:
                    return StaticKind.UNDEFINED;
            }
        }

        /**
         * Does this result corresponds to a successful lookup (i.e. one where a method has been found?)
         */
        boolean isSuccess() {
            return staticKind != StaticKind.UNDEFINED;
        }

        /**
         * Does this result have given static kind?
         */
        boolean hasKind(StaticKind sk) {
            return this.staticKind == sk;
        }

        /**
         * Error recovery helper: can this lookup result be ignored (for the purpose of returning
         * some 'better' result) ?
         */
        boolean canIgnore() {
            switch (sym.kind) {
                case ABSENT_MTH:
                    return true;
                case WRONG_MTH:
                    InapplicableSymbolError errSym =
                            (InapplicableSymbolError)sym.baseSymbol();
                    return new Template(MethodCheckDiag.ARITY_MISMATCH.regex())
                            .matches(errSym.errCandidate().snd);
                case WRONG_MTHS:
                    InapplicableSymbolsError errSyms =
                            (InapplicableSymbolsError)sym.baseSymbol();
                    return errSyms.filterCandidates(errSyms.mapCandidates()).isEmpty();
                default:
                    return false;
            }
        }

        static ReferenceLookupResult error(Symbol sym) {
            return new ReferenceLookupResult(sym, StaticKind.UNDEFINED);
        }
    }

    /**
     * This abstract class embodies the logic that converts one (bound lookup) or two (unbound lookup)
     * {@code ReferenceLookupResult} objects into a (@code Symbol), which is then regarded as the
     * result of method reference resolution.
     */
    abstract class ReferenceChooser {
        /**
         * Generate a result from a pair of lookup result objects. This method delegates to the
         * appropriate result generation routine.
         */
        ReferenceLookupResult result(ReferenceLookupResult boundRes, ReferenceLookupResult unboundRes) {
            return unboundRes != referenceNotFound ?
                    unboundResult(boundRes, unboundRes) :
                    boundResult(boundRes);
        }

        /**
         * Generate a symbol from a given bound lookup result.
         */
        abstract ReferenceLookupResult boundResult(ReferenceLookupResult boundRes);

        /**
         * Generate a symbol from a pair of bound/unbound lookup results.
         */
        abstract ReferenceLookupResult unboundResult(ReferenceLookupResult boundRes, ReferenceLookupResult unboundRes);
    }

    /**
     * This chooser implements the selection strategy used during a full lookup; this logic
     * is described in JLS SE 8 (15.3.2).
     */
    ReferenceChooser basicReferenceChooser = new ReferenceChooser() {

        @Override
        ReferenceLookupResult boundResult(ReferenceLookupResult boundRes) {
            return !boundRes.isSuccess() || boundRes.hasKind(StaticKind.NON_STATIC) ?
                    boundRes : //the search produces a non-static method
                    ReferenceLookupResult.error(new BadMethodReferenceError(boundRes.sym, false));
        }

        @Override
        ReferenceLookupResult unboundResult(ReferenceLookupResult boundRes, ReferenceLookupResult unboundRes) {
            if (boundRes.isSuccess() && boundRes.sym.isStatic() &&
                    (!unboundRes.isSuccess() || unboundRes.hasKind(StaticKind.STATIC))) {
                //the first search produces a static method and no non-static method is applicable
                //during the second search
                return boundRes;
            } else if (unboundRes.isSuccess() && !unboundRes.sym.isStatic() &&
                    (!boundRes.isSuccess() || boundRes.hasKind(StaticKind.NON_STATIC))) {
                //the second search produces a non-static method and no static method is applicable
                //during the first search
                return unboundRes;
            } else if (boundRes.isSuccess() && unboundRes.isSuccess()) {
                //both searches produce some result; ambiguity (error recovery)
                return ReferenceLookupResult.error(ambiguityError(boundRes.sym, unboundRes.sym));
            } else if (boundRes.isSuccess() || unboundRes.isSuccess()) {
                //Both searches failed to produce a result with correct staticness (i.e. first search
                //produces an non-static method). Alternatively, a given search produced a result
                //with the right staticness, but the other search has applicable methods with wrong
                //staticness (error recovery)
                return ReferenceLookupResult.error(new BadMethodReferenceError(boundRes.isSuccess() ?
                        boundRes.sym : unboundRes.sym, true));
            } else {
                //both searches fail to produce a result - pick 'better' error using heuristics (error recovery)
                return (boundRes.canIgnore() && !unboundRes.canIgnore()) ?
                        unboundRes : boundRes;
            }
        }
    };

    /**
     * This chooser implements the selection strategy used during an arity-based lookup; this logic
     * is described in JLS SE 8 (15.12.2.1).
     */
    ReferenceChooser structuralReferenceChooser = new ReferenceChooser() {

        @Override
        ReferenceLookupResult boundResult(ReferenceLookupResult boundRes) {
            return (!boundRes.isSuccess() || !boundRes.hasKind(StaticKind.STATIC)) ?
                    boundRes : //the search has at least one applicable non-static method
                    ReferenceLookupResult.error(new BadMethodReferenceError(boundRes.sym, false));
        }

        @Override
        ReferenceLookupResult unboundResult(ReferenceLookupResult boundRes, ReferenceLookupResult unboundRes) {
            if (boundRes.isSuccess() && !boundRes.hasKind(StaticKind.NON_STATIC)) {
                //the first search has at least one applicable static method
                return boundRes;
            } else if (unboundRes.isSuccess() && !unboundRes.hasKind(StaticKind.STATIC)) {
                //the second search has at least one applicable non-static method
                return unboundRes;
            } else if (boundRes.isSuccess() || unboundRes.isSuccess()) {
                //either the first search produces a non-static method, or second search produces
                //a non-static method (error recovery)
                return ReferenceLookupResult.error(new BadMethodReferenceError(boundRes.isSuccess() ?
                        boundRes.sym : unboundRes.sym, true));
            } else {
                //both searches fail to produce a result - pick 'better' error using heuristics (error recovery)
                return (boundRes.canIgnore() && !unboundRes.canIgnore()) ?
                        unboundRes : boundRes;
            }
        }
    };

    /**
     * Helper for defining custom method-like lookup logic; a lookup helper
     * provides hooks for (i) the actual lookup logic and (ii) accessing the
     * lookup result (this step might result in compiler diagnostics to be generated)
     */
    abstract class LookupHelper {

        /** name of the symbol to lookup */
        Name name;

        /** location in which the lookup takes place */
        Type site;

        /** actual types used during the lookup */
        List<Type> argtypes;

        /** type arguments used during the lookup */
        List<Type> typeargtypes;

        /** Max overload resolution phase handled by this helper */
        MethodResolutionPhase maxPhase;

        LookupHelper(Name name, Type site, List<Type> argtypes, List<Type> typeargtypes, MethodResolutionPhase maxPhase) {
            this.name = name;
            this.site = site;
            this.argtypes = argtypes;
            this.typeargtypes = typeargtypes;
            this.maxPhase = maxPhase;
        }

        /**
         * Should lookup stop at given phase with given result
         */
        final boolean shouldStop(Symbol sym, MethodResolutionPhase phase) {
            return phase.ordinal() > maxPhase.ordinal() ||
                 !sym.kind.isResolutionError() || sym.kind == AMBIGUOUS || sym.kind == STATICERR;
        }

        /**
         * Search for a symbol under a given overload resolution phase - this method
         * is usually called several times, once per each overload resolution phase
         */
        abstract Symbol lookup(Env<AttrContext> env, MethodResolutionPhase phase);

        /**
         * Dump overload resolution info
         */
        void debug(DiagnosticPosition pos, Symbol sym) {
            //do nothing
        }

        /**
         * Validate the result of the lookup
         */
        abstract Symbol access(Env<AttrContext> env, DiagnosticPosition pos, Symbol location, Symbol sym);
    }

    abstract class BasicLookupHelper extends LookupHelper {

        BasicLookupHelper(Name name, Type site, List<Type> argtypes, List<Type> typeargtypes) {
            this(name, site, argtypes, typeargtypes, MethodResolutionPhase.VARARITY);
        }

        BasicLookupHelper(Name name, Type site, List<Type> argtypes, List<Type> typeargtypes, MethodResolutionPhase maxPhase) {
            super(name, site, argtypes, typeargtypes, maxPhase);
        }

        @Override
        final Symbol lookup(Env<AttrContext> env, MethodResolutionPhase phase) {
            Symbol sym = doLookup(env, phase);
            if (sym.kind == AMBIGUOUS) {
                AmbiguityError a_err = (AmbiguityError)sym.baseSymbol();
                sym = a_err.mergeAbstracts(site);
            }
            return sym;
        }

        abstract Symbol doLookup(Env<AttrContext> env, MethodResolutionPhase phase);

        @Override
        Symbol access(Env<AttrContext> env, DiagnosticPosition pos, Symbol location, Symbol sym) {
            if (sym.kind.isResolutionError()) {
                //if nothing is found return the 'first' error
                sym = accessMethod(sym, pos, location, site, name, true, argtypes, typeargtypes);
            }
            return sym;
        }

        @Override
        void debug(DiagnosticPosition pos, Symbol sym) {
            reportVerboseResolutionDiagnostic(pos, name, site, argtypes, typeargtypes, sym);
        }
    }

    /**
     * Helper class for member reference lookup. A reference lookup helper
     * defines the basic logic for member reference lookup; a method gives
     * access to an 'unbound' helper used to perform an unbound member
     * reference lookup.
     */
    abstract class ReferenceLookupHelper extends LookupHelper {

        /** The member reference tree */
        JCMemberReference referenceTree;

        ReferenceLookupHelper(JCMemberReference referenceTree, Name name, Type site,
                List<Type> argtypes, List<Type> typeargtypes, MethodResolutionPhase maxPhase) {
            super(name, site, argtypes, typeargtypes, maxPhase);
            this.referenceTree = referenceTree;
        }

        /**
         * Returns an unbound version of this lookup helper. By default, this
         * method returns an dummy lookup helper.
         */
        ReferenceLookupHelper unboundLookup(InferenceContext inferenceContext) {
            return null;
        }

        /**
         * Get the kind of the member reference
         */
        abstract JCMemberReference.ReferenceKind referenceKind(Symbol sym);

        Symbol access(Env<AttrContext> env, DiagnosticPosition pos, Symbol location, Symbol sym) {
            if (sym.kind == AMBIGUOUS) {
                AmbiguityError a_err = (AmbiguityError)sym.baseSymbol();
                sym = a_err.mergeAbstracts(site);
            }
            //skip error reporting
            return sym;
        }
    }

    /**
     * Helper class for method reference lookup. The lookup logic is based
     * upon Resolve.findMethod; in certain cases, this helper class has a
     * corresponding unbound helper class (see UnboundMethodReferenceLookupHelper).
     * In such cases, non-static lookup results are thrown away.
     */
    class MethodReferenceLookupHelper extends ReferenceLookupHelper {

        /** The original method reference lookup site. */
        Type originalSite;

        MethodReferenceLookupHelper(JCMemberReference referenceTree, Name name, Type site,
                List<Type> argtypes, List<Type> typeargtypes, MethodResolutionPhase maxPhase) {
            super(referenceTree, name, types.skipTypeVars(site, true), argtypes, typeargtypes, maxPhase);
            this.originalSite = site;
        }

        @Override
        final Symbol lookup(Env<AttrContext> env, MethodResolutionPhase phase) {
            return findMethod(env, site, name, argtypes, typeargtypes,
                    phase.isBoxingRequired(), phase.isVarargsRequired());
        }

        @Override
        ReferenceLookupHelper unboundLookup(InferenceContext inferenceContext) {
            if (TreeInfo.isStaticSelector(referenceTree.expr, names)) {
                if (argtypes.nonEmpty() &&
                        (argtypes.head.hasTag(NONE) ||
                        types.isSubtypeUnchecked(inferenceContext.asUndetVar(argtypes.head), originalSite))) {
                    return new UnboundMethodReferenceLookupHelper(referenceTree, name,
                            originalSite, argtypes, typeargtypes, maxPhase);
                } else {
                    return new ReferenceLookupHelper(referenceTree, name, site, argtypes, typeargtypes, maxPhase) {
                        @Override
                        ReferenceLookupHelper unboundLookup(InferenceContext inferenceContext) {
                            return this;
                        }

                        @Override
                        Symbol lookup(Env<AttrContext> env, MethodResolutionPhase phase) {
                            return methodNotFound;
                        }

                        @Override
                        ReferenceKind referenceKind(Symbol sym) {
                            Assert.error();
                            return null;
                        }
                    };
                }
            } else {
                return super.unboundLookup(inferenceContext);
            }
        }

        @Override
        ReferenceKind referenceKind(Symbol sym) {
            if (sym.isStatic()) {
                return ReferenceKind.STATIC;
            } else {
                Name selName = TreeInfo.name(referenceTree.getQualifierExpression());
                return selName != null && selName == names._super ?
                        ReferenceKind.SUPER :
                        ReferenceKind.BOUND;
            }
        }
    }

    /**
     * Helper class for unbound method reference lookup. Essentially the same
     * as the basic method reference lookup helper; main difference is that static
     * lookup results are thrown away. If qualifier type is raw, an attempt to
     * infer a parameterized type is made using the first actual argument (that
     * would otherwise be ignored during the lookup).
     */
    class UnboundMethodReferenceLookupHelper extends MethodReferenceLookupHelper {

        UnboundMethodReferenceLookupHelper(JCMemberReference referenceTree, Name name, Type site,
                List<Type> argtypes, List<Type> typeargtypes, MethodResolutionPhase maxPhase) {
            super(referenceTree, name, site, argtypes.tail, typeargtypes, maxPhase);
            if (site.isRaw() && !argtypes.head.hasTag(NONE)) {
                Type asSuperSite = types.asSuper(argtypes.head, site.tsym);
                this.site = types.skipTypeVars(asSuperSite, true);
            }
        }

        @Override
        ReferenceLookupHelper unboundLookup(InferenceContext inferenceContext) {
            return this;
        }

        @Override
        ReferenceKind referenceKind(Symbol sym) {
            return ReferenceKind.UNBOUND;
        }
    }

    /**
     * Helper class for array constructor lookup; an array constructor lookup
     * is simulated by looking up a method that returns the array type specified
     * as qualifier, and that accepts a single int parameter (size of the array).
     */
    class ArrayConstructorReferenceLookupHelper extends ReferenceLookupHelper {

        ArrayConstructorReferenceLookupHelper(JCMemberReference referenceTree, Type site, List<Type> argtypes,
                List<Type> typeargtypes, MethodResolutionPhase maxPhase) {
            super(referenceTree, names.init, site, argtypes, typeargtypes, maxPhase);
        }

        @Override
        protected Symbol lookup(Env<AttrContext> env, MethodResolutionPhase phase) {
            WriteableScope sc = WriteableScope.create(syms.arrayClass);
            MethodSymbol arrayConstr = new MethodSymbol(PUBLIC, name, null, site.tsym);
            arrayConstr.type = new MethodType(List.of(syms.intType), site, List.nil(), syms.methodClass);
            sc.enter(arrayConstr);
            return findMethodInScope(env, site, name, argtypes, typeargtypes, sc, methodNotFound, phase.isBoxingRequired(), phase.isVarargsRequired(), false);
        }

        @Override
        ReferenceKind referenceKind(Symbol sym) {
            return ReferenceKind.ARRAY_CTOR;
        }
    }

    /**
     * Helper class for constructor reference lookup. The lookup logic is based
     * upon either Resolve.findMethod or Resolve.findDiamond - depending on
     * whether the constructor reference needs diamond inference (this is the case
     * if the qualifier type is raw). A special erroneous symbol is returned
     * if the lookup returns the constructor of an inner class and there's no
     * enclosing instance in scope.
     */
    class ConstructorReferenceLookupHelper extends ReferenceLookupHelper {

        boolean needsInference;

        ConstructorReferenceLookupHelper(JCMemberReference referenceTree, Type site, List<Type> argtypes,
                List<Type> typeargtypes, MethodResolutionPhase maxPhase) {
            super(referenceTree, names.init, site, argtypes, typeargtypes, maxPhase);
            if (site.isRaw()) {
                this.site = new ClassType(site.getEnclosingType(), site.tsym.type.getTypeArguments(), site.tsym, site.getMetadata());
                needsInference = true;
            }
        }

        @Override
        protected Symbol lookup(Env<AttrContext> env, MethodResolutionPhase phase) {
            Symbol sym = needsInference ?
                findDiamond(env, site, argtypes, typeargtypes, phase.isBoxingRequired(), phase.isVarargsRequired()) :
                findMethod(env, site, name, argtypes, typeargtypes,
                        phase.isBoxingRequired(), phase.isVarargsRequired());
            return enclosingInstanceMissing(env, site) ? new BadConstructorReferenceError(sym) : sym;
        }

        @Override
        ReferenceKind referenceKind(Symbol sym) {
            return site.getEnclosingType().hasTag(NONE) ?
                    ReferenceKind.TOPLEVEL : ReferenceKind.IMPLICIT_INNER;
        }
    }

    /**
     * Main overload resolution routine. On each overload resolution step, a
     * lookup helper class is used to perform the method/constructor lookup;
     * at the end of the lookup, the helper is used to validate the results
     * (this last step might trigger overload resolution diagnostics).
     */
    Symbol lookupMethod(Env<AttrContext> env, DiagnosticPosition pos, Symbol location, MethodCheck methodCheck, LookupHelper lookupHelper) {
        MethodResolutionContext resolveContext = new MethodResolutionContext();
        resolveContext.methodCheck = methodCheck;
        return lookupMethod(env, pos, location, resolveContext, lookupHelper);
    }

    Symbol lookupMethod(Env<AttrContext> env, DiagnosticPosition pos, Symbol location,
            MethodResolutionContext resolveContext, LookupHelper lookupHelper) {
        MethodResolutionContext prevResolutionContext = currentResolutionContext;
        try {
            Symbol bestSoFar = methodNotFound;
            currentResolutionContext = resolveContext;
            for (MethodResolutionPhase phase : methodResolutionSteps) {
                if (lookupHelper.shouldStop(bestSoFar, phase))
                    break;
                MethodResolutionPhase prevPhase = currentResolutionContext.step;
                Symbol prevBest = bestSoFar;
                currentResolutionContext.step = phase;
                Symbol sym = lookupHelper.lookup(env, phase);
                lookupHelper.debug(pos, sym);
                bestSoFar = phase.mergeResults(bestSoFar, sym);
                env.info.pendingResolutionPhase = (prevBest == bestSoFar) ? prevPhase : phase;
            }
            return lookupHelper.access(env, pos, location, bestSoFar);
        } finally {
            currentResolutionContext = prevResolutionContext;
        }
    }

    /**
     * Resolve `c.name' where name == this or name == super.
     * @param pos           The position to use for error reporting.
     * @param env           The environment current at the expression.
     * @param c             The qualifier.
     * @param name          The identifier's name.
     */
    Symbol resolveSelf(DiagnosticPosition pos,
                       Env<AttrContext> env,
                       TypeSymbol c,
                       Name name) {
        Env<AttrContext> env1 = env;
        boolean staticOnly = false;
        while (env1.outer != null) {
            if (isStatic(env1)) staticOnly = true;
            if (env1.enclClass.sym == c) {
                Symbol sym = env1.info.scope.findFirst(name);
                if (sym != null) {
                    if (staticOnly) sym = new StaticError(sym);
                    return accessBase(sym, pos, env.enclClass.sym.type,
                                  name, true);
                }
            }
            if ((env1.enclClass.sym.flags() & STATIC) != 0) staticOnly = true;
            env1 = env1.outer;
        }
        if (c.isInterface() &&
            name == names._super && !isStatic(env) &&
            types.isDirectSuperInterface(c, env.enclClass.sym)) {
            //this might be a default super call if one of the superinterfaces is 'c'
            for (Type t : pruneInterfaces(env.enclClass.type)) {
                if (t.tsym == c) {
                    env.info.defaultSuperCallSite = t;
                    return new VarSymbol(0, names._super,
                            types.asSuper(env.enclClass.type, c), env.enclClass.sym);
                }
            }
            //find a direct super type that is a subtype of 'c'
            for (Type i : types.directSupertypes(env.enclClass.type)) {
                if (i.tsym.isSubClass(c, types) && i.tsym != c) {
                    log.error(pos,
                              Errors.IllegalDefaultSuperCall(c,
                                                             Fragments.RedundantSupertype(c, i)));
                    return syms.errSymbol;
                }
            }
            Assert.error();
        }
        log.error(pos, Errors.NotEnclClass(c));
        return syms.errSymbol;
    }
    //where
    private List<Type> pruneInterfaces(Type t) {
        ListBuffer<Type> result = new ListBuffer<>();
        for (Type t1 : types.interfaces(t)) {
            boolean shouldAdd = true;
            for (Type t2 : types.directSupertypes(t)) {
                if (t1 != t2 && !t2.hasTag(ERROR) && types.isSubtypeNoCapture(t2, t1)) {
                    shouldAdd = false;
                }
            }
            if (shouldAdd) {
                result.append(t1);
            }
        }
        return result.toList();
    }


    /**
     * Resolve `c.this' for an enclosing class c that contains the
     * named member.
     * @param pos           The position to use for error reporting.
     * @param env           The environment current at the expression.
     * @param member        The member that must be contained in the result.
     */
    Symbol resolveSelfContaining(DiagnosticPosition pos,
                                 Env<AttrContext> env,
                                 Symbol member,
                                 boolean isSuperCall) {
        Symbol sym = resolveSelfContainingInternal(env, member, isSuperCall);
        if (sym == null) {
            log.error(pos, Errors.EnclClassRequired(member));
            return syms.errSymbol;
        } else {
            return accessBase(sym, pos, env.enclClass.sym.type, sym.name, true);
        }
    }

    boolean enclosingInstanceMissing(Env<AttrContext> env, Type type) {
        if (type.hasTag(CLASS) && type.getEnclosingType().hasTag(CLASS)) {
            Symbol encl = resolveSelfContainingInternal(env, type.tsym, false);
            return encl == null || encl.kind.isResolutionError();
        }
        return false;
    }

    private Symbol resolveSelfContainingInternal(Env<AttrContext> env,
                                 Symbol member,
                                 boolean isSuperCall) {
        Name name = names._this;
        Env<AttrContext> env1 = isSuperCall ? env.outer : env;
        boolean staticOnly = false;
        if (env1 != null) {
            while (env1 != null && env1.outer != null) {
                if (isStatic(env1)) staticOnly = true;
                if (env1.enclClass.sym.isSubClass(member.owner.enclClass(), types)) {
                    Symbol sym = env1.info.scope.findFirst(name);
                    if (sym != null) {
                        if (staticOnly) sym = new StaticError(sym);
                        return sym;
                    }
                }
                if ((env1.enclClass.sym.flags() & STATIC) != 0)
                    staticOnly = true;
                env1 = env1.outer;
            }
        }
        return null;
    }

    /**
     * Resolve an appropriate implicit this instance for t's container.
     * JLS 8.8.5.1 and 15.9.2
     */
    Type resolveImplicitThis(DiagnosticPosition pos, Env<AttrContext> env, Type t) {
        return resolveImplicitThis(pos, env, t, false);
    }

    Type resolveImplicitThis(DiagnosticPosition pos, Env<AttrContext> env, Type t, boolean isSuperCall) {
        Type thisType = (t.tsym.owner.kind.matches(KindSelector.VAL_MTH)
                         ? resolveSelf(pos, env, t.getEnclosingType().tsym, names._this)
                         : resolveSelfContaining(pos, env, t.tsym, isSuperCall)).type;
        if (env.info.isSelfCall && thisType.tsym == env.enclClass.sym) {
            log.error(pos, Errors.CantRefBeforeCtorCalled("this"));
        }
        return thisType;
    }

/* ***************************************************************************
 *  ResolveError classes, indicating error situations when accessing symbols
 ****************************************************************************/

    //used by TransTypes when checking target type of synthetic cast
    public void logAccessErrorInternal(Env<AttrContext> env, JCTree tree, Type type) {
        AccessError error = new AccessError(env, env.enclClass.type, type.tsym);
        logResolveError(error, tree.pos(), env.enclClass.sym, env.enclClass.type, null, null, null);
    }
    //where
    private void logResolveError(ResolveError error,
            DiagnosticPosition pos,
            Symbol location,
            Type site,
            Name name,
            List<Type> argtypes,
            List<Type> typeargtypes) {
        JCDiagnostic d = error.getDiagnostic(JCDiagnostic.DiagnosticType.ERROR,
                pos, location, site, name, argtypes, typeargtypes);
        if (d != null) {
            d.setFlag(DiagnosticFlag.RESOLVE_ERROR);
            log.report(d);
        }
    }

    private final LocalizedString noArgs = new LocalizedString("compiler.misc.no.args");

    public Object methodArguments(List<Type> argtypes) {
        if (argtypes == null || argtypes.isEmpty()) {
            return noArgs;
        } else {
            ListBuffer<Object> diagArgs = new ListBuffer<>();
            for (Type t : argtypes) {
                if (t.hasTag(DEFERRED)) {
                    diagArgs.append(((DeferredAttr.DeferredType)t).tree);
                } else {
                    diagArgs.append(t);
                }
            }
            return diagArgs;
        }
    }

    /**
     * Root class for resolution errors. Subclass of ResolveError
     * represent a different kinds of resolution error - as such they must
     * specify how they map into concrete compiler diagnostics.
     */
    abstract class ResolveError extends Symbol {

        /** The name of the kind of error, for debugging only. */
        final String debugName;

        ResolveError(Kind kind, String debugName) {
            super(kind, 0, null, null, null);
            this.debugName = debugName;
        }

        @Override @DefinedBy(Api.LANGUAGE_MODEL)
        public <R, P> R accept(ElementVisitor<R, P> v, P p) {
            throw new AssertionError();
        }

        @Override
        public String toString() {
            return debugName;
        }

        @Override
        public boolean exists() {
            return false;
        }

        @Override
        public boolean isStatic() {
            return false;
        }

        /**
         * Create an external representation for this erroneous symbol to be
         * used during attribution - by default this returns the symbol of a
         * brand new error type which stores the original type found
         * during resolution.
         *
         * @param name     the name used during resolution
         * @param location the location from which the symbol is accessed
         */
        protected Symbol access(Name name, TypeSymbol location) {
            return types.createErrorType(name, location, syms.errSymbol.type).tsym;
        }

        /**
         * Create a diagnostic representing this resolution error.
         *
         * @param dkind     The kind of the diagnostic to be created (e.g error).
         * @param pos       The position to be used for error reporting.
         * @param site      The original type from where the selection took place.
         * @param name      The name of the symbol to be resolved.
         * @param argtypes  The invocation's value arguments,
         *                  if we looked for a method.
         * @param typeargtypes  The invocation's type arguments,
         *                      if we looked for a method.
         */
        abstract JCDiagnostic getDiagnostic(JCDiagnostic.DiagnosticType dkind,
                DiagnosticPosition pos,
                Symbol location,
                Type site,
                Name name,
                List<Type> argtypes,
                List<Type> typeargtypes);
    }

    /**
     * This class is the root class of all resolution errors caused by
     * an invalid symbol being found during resolution.
     */
    abstract class InvalidSymbolError extends ResolveError {

        /** The invalid symbol found during resolution */
        Symbol sym;

        InvalidSymbolError(Kind kind, Symbol sym, String debugName) {
            super(kind, debugName);
            this.sym = sym;
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public String toString() {
             return super.toString() + " wrongSym=" + sym;
        }

        @Override
        public Symbol access(Name name, TypeSymbol location) {
            if (!sym.kind.isResolutionError() && sym.kind.matches(KindSelector.TYP))
                return types.createErrorType(name, location, sym.type).tsym;
            else
                return sym;
        }
    }

    class BadRestrictedTypeError extends ResolveError {
        private final Name typeName;
        BadRestrictedTypeError(Name typeName) {
            super(Kind.BAD_RESTRICTED_TYPE, "bad var use");
            this.typeName = typeName;
        }

        @Override
        JCDiagnostic getDiagnostic(DiagnosticType dkind, DiagnosticPosition pos, Symbol location, Type site, Name name, List<Type> argtypes, List<Type> typeargtypes) {
            return diags.create(dkind, log.currentSource(), pos, "illegal.ref.to.restricted.type", typeName);
        }
    }

    /**
     * InvalidSymbolError error class indicating that a symbol matching a
     * given name does not exists in a given site.
     */
    class SymbolNotFoundError extends ResolveError {

        SymbolNotFoundError(Kind kind) {
            this(kind, "symbol not found error");
        }

        SymbolNotFoundError(Kind kind, String debugName) {
            super(kind, debugName);
        }

        @Override
        JCDiagnostic getDiagnostic(JCDiagnostic.DiagnosticType dkind,
                DiagnosticPosition pos,
                Symbol location,
                Type site,
                Name name,
                List<Type> argtypes,
                List<Type> typeargtypes) {
            argtypes = argtypes == null ? List.nil() : argtypes;
            typeargtypes = typeargtypes == null ? List.nil() : typeargtypes;
            if (name == names.error)
                return null;

            boolean hasLocation = false;
            if (location == null) {
                location = site.tsym;
            }
            if (!location.name.isEmpty()) {
                if (location.kind == PCK && !site.tsym.exists() && location.name != names.java) {
                    return diags.create(dkind, log.currentSource(), pos,
                        "doesnt.exist", location);
                }
                hasLocation = !location.name.equals(names._this) &&
                        !location.name.equals(names._super);
            }
            boolean isConstructor = name == names.init;
            KindName kindname = isConstructor ? KindName.CONSTRUCTOR : kind.absentKind();
            Name idname = isConstructor ? site.tsym.name : name;
            String errKey = getErrorKey(kindname, typeargtypes.nonEmpty(), hasLocation);
            if (hasLocation) {
                return diags.create(dkind, log.currentSource(), pos,
                        errKey, kindname, idname, //symbol kindname, name
                        typeargtypes, args(argtypes), //type parameters and arguments (if any)
                        getLocationDiag(location, site)); //location kindname, type
            }
            else {
                return diags.create(dkind, log.currentSource(), pos,
                        errKey, kindname, idname, //symbol kindname, name
                        typeargtypes, args(argtypes)); //type parameters and arguments (if any)
            }
        }
        //where
        private Object args(List<Type> args) {
            return args.isEmpty() ? args : methodArguments(args);
        }

        private String getErrorKey(KindName kindname, boolean hasTypeArgs, boolean hasLocation) {
            String key = "cant.resolve";
            String suffix = hasLocation ? ".location" : "";
            switch (kindname) {
                case METHOD:
                case CONSTRUCTOR: {
                    suffix += ".args";
                    suffix += hasTypeArgs ? ".params" : "";
                }
            }
            return key + suffix;
        }
        private JCDiagnostic getLocationDiag(Symbol location, Type site) {
            if (location.kind == VAR) {
                return diags.fragment(Fragments.Location1(kindName(location),
                                                          location,
                                                          location.type));
            } else {
                return diags.fragment(Fragments.Location(typeKindName(site),
                                      site,
                                      null));
            }
        }
    }

    /**
     * InvalidSymbolError error class indicating that a given symbol
     * (either a method, a constructor or an operand) is not applicable
     * given an actual arguments/type argument list.
     */
    class InapplicableSymbolError extends ResolveError {

        protected MethodResolutionContext resolveContext;

        InapplicableSymbolError(MethodResolutionContext context) {
            this(WRONG_MTH, "inapplicable symbol error", context);
        }

        protected InapplicableSymbolError(Kind kind, String debugName, MethodResolutionContext context) {
            super(kind, debugName);
            this.resolveContext = context;
        }

        @Override
        public String toString() {
            return super.toString();
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        JCDiagnostic getDiagnostic(JCDiagnostic.DiagnosticType dkind,
                DiagnosticPosition pos,
                Symbol location,
                Type site,
                Name name,
                List<Type> argtypes,
                List<Type> typeargtypes) {
            if (name == names.error)
                return null;

            Pair<Symbol, JCDiagnostic> c = errCandidate();
            if (compactMethodDiags) {
                JCDiagnostic simpleDiag =
                    MethodResolutionDiagHelper.rewrite(diags, pos, log.currentSource(), dkind, c.snd);
                if (simpleDiag != null) {
                    return simpleDiag;
                }
            }
            Symbol ws = c.fst.asMemberOf(site, types);
            return diags.create(dkind, log.currentSource(), pos,
                      "cant.apply.symbol",
                      kindName(ws),
                      ws.name == names.init ? ws.owner.name : ws.name,
                      methodArguments(ws.type.getParameterTypes()),
                      methodArguments(argtypes),
                      kindName(ws.owner),
                      ws.owner.type,
                      c.snd);
        }

        @Override
        public Symbol access(Name name, TypeSymbol location) {
            Pair<Symbol, JCDiagnostic> cand = errCandidate();
            TypeSymbol errSymbol = types.createErrorType(name, location, cand != null ? cand.fst.type : syms.errSymbol.type).tsym;
            if (cand != null) {
                attrRecover.wrongMethodSymbolCandidate(errSymbol, cand.fst, cand.snd);
            }
            return errSymbol;
        }

        protected Pair<Symbol, JCDiagnostic> errCandidate() {
            Candidate bestSoFar = null;
            for (Candidate c : resolveContext.candidates) {
                if (c.isApplicable()) continue;
                bestSoFar = c;
            }
            Assert.checkNonNull(bestSoFar);
            return new Pair<>(bestSoFar.sym, bestSoFar.details);
        }
    }

    /**
     * ResolveError error class indicating that a symbol (either methods, constructors or operand)
     * is not applicable given an actual arguments/type argument list.
     */
    class InapplicableSymbolsError extends InapplicableSymbolError {

        InapplicableSymbolsError(MethodResolutionContext context) {
            super(WRONG_MTHS, "inapplicable symbols", context);
        }

        @Override
        JCDiagnostic getDiagnostic(JCDiagnostic.DiagnosticType dkind,
                DiagnosticPosition pos,
                Symbol location,
                Type site,
                Name name,
                List<Type> argtypes,
                List<Type> typeargtypes) {
            Map<Symbol, JCDiagnostic> candidatesMap = mapCandidates();
            Map<Symbol, JCDiagnostic> filteredCandidates = compactMethodDiags ?
                    filterCandidates(candidatesMap) :
                    mapCandidates();
            if (filteredCandidates.isEmpty()) {
                filteredCandidates = candidatesMap;
            }
            boolean truncatedDiag = candidatesMap.size() != filteredCandidates.size();
            if (filteredCandidates.size() > 1) {
                JCDiagnostic err = diags.create(dkind,
                        null,
                        truncatedDiag ?
                            EnumSet.of(DiagnosticFlag.COMPRESSED) :
                            EnumSet.noneOf(DiagnosticFlag.class),
                        log.currentSource(),
                        pos,
                        "cant.apply.symbols",
                        name == names.init ? KindName.CONSTRUCTOR : kind.absentKind(),
                        name == names.init ? site.tsym.name : name,
                        methodArguments(argtypes));
                return new JCDiagnostic.MultilineDiagnostic(err, candidateDetails(filteredCandidates, site));
            } else if (filteredCandidates.size() == 1) {
                Map.Entry<Symbol, JCDiagnostic> _e =
                                filteredCandidates.entrySet().iterator().next();
                final Pair<Symbol, JCDiagnostic> p = new Pair<>(_e.getKey(), _e.getValue());
                JCDiagnostic d = new InapplicableSymbolError(resolveContext) {
                    @Override
                    protected Pair<Symbol, JCDiagnostic> errCandidate() {
                        return p;
                    }
                }.getDiagnostic(dkind, pos,
                    location, site, name, argtypes, typeargtypes);
                if (truncatedDiag) {
                    d.setFlag(DiagnosticFlag.COMPRESSED);
                }
                return d;
            } else {
                return new SymbolNotFoundError(ABSENT_MTH).getDiagnostic(dkind, pos,
                    location, site, name, argtypes, typeargtypes);
            }
        }
        //where
            private Map<Symbol, JCDiagnostic> mapCandidates() {
                MostSpecificMap candidates = new MostSpecificMap();
                for (Candidate c : resolveContext.candidates) {
                    if (c.isApplicable()) continue;
                    candidates.put(c);
                }
                return candidates;
            }

            @SuppressWarnings("serial")
            private class MostSpecificMap extends LinkedHashMap<Symbol, JCDiagnostic> {
                private void put(Candidate c) {
                    ListBuffer<Symbol> overridden = new ListBuffer<>();
                    for (Symbol s : keySet()) {
                        if (s == c.sym) {
                            continue;
                        }
                        if (c.sym.overrides(s, (TypeSymbol)s.owner, types, false)) {
                            overridden.add(s);
                        } else if (s.overrides(c.sym, (TypeSymbol)c.sym.owner, types, false)) {
                            return;
                        }
                    }
                    for (Symbol s : overridden) {
                        remove(s);
                    }
                    put(c.sym, c.details);
                }
            }

            Map<Symbol, JCDiagnostic> filterCandidates(Map<Symbol, JCDiagnostic> candidatesMap) {
                Map<Symbol, JCDiagnostic> candidates = new LinkedHashMap<>();
                for (Map.Entry<Symbol, JCDiagnostic> _entry : candidatesMap.entrySet()) {
                    JCDiagnostic d = _entry.getValue();
                    if (!new Template(MethodCheckDiag.ARITY_MISMATCH.regex()).matches(d)) {
                        candidates.put(_entry.getKey(), d);
                    }
                }
                return candidates;
            }

            private List<JCDiagnostic> candidateDetails(Map<Symbol, JCDiagnostic> candidatesMap, Type site) {
                List<JCDiagnostic> details = List.nil();
                for (Map.Entry<Symbol, JCDiagnostic> _entry : candidatesMap.entrySet()) {
                    Symbol sym = _entry.getKey();
                    JCDiagnostic detailDiag =
                            diags.fragment(Fragments.InapplicableMethod(Kinds.kindName(sym),
                                                                        sym.location(site, types),
                                                                        sym.asMemberOf(site, types),
                                                                        _entry.getValue()));
                    details = details.prepend(detailDiag);
                }
                //typically members are visited in reverse order (see Scope)
                //so we need to reverse the candidate list so that candidates
                //conform to source order
                return details;
            }

        @Override
        protected Pair<Symbol, JCDiagnostic> errCandidate() {
            Map<Symbol, JCDiagnostic> candidatesMap = mapCandidates();
            Map<Symbol, JCDiagnostic> filteredCandidates = filterCandidates(candidatesMap);
            if (filteredCandidates.size() == 1) {
                return Pair.of(filteredCandidates.keySet().iterator().next(),
                               filteredCandidates.values().iterator().next());
            }
            return null;
        }
    }

    /**
     * DiamondError error class indicating that a constructor symbol is not applicable
     * given an actual arguments/type argument list using diamond inference.
     */
    class DiamondError extends InapplicableSymbolError {

        Symbol sym;

        public DiamondError(Symbol sym, MethodResolutionContext context) {
            super(sym.kind, "diamondError", context);
            this.sym = sym;
        }

        JCDiagnostic getDetails() {
            return (sym.kind == WRONG_MTH) ?
                    ((InapplicableSymbolError)sym.baseSymbol()).errCandidate().snd :
                    null;
        }

        @Override
        JCDiagnostic getDiagnostic(DiagnosticType dkind, DiagnosticPosition pos,
                Symbol location, Type site, Name name, List<Type> argtypes, List<Type> typeargtypes) {
            JCDiagnostic details = getDetails();
            if (details != null && compactMethodDiags) {
                JCDiagnostic simpleDiag =
                        MethodResolutionDiagHelper.rewrite(diags, pos, log.currentSource(), dkind, details);
                if (simpleDiag != null) {
                    return simpleDiag;
                }
            }
            String key = details == null ?
                "cant.apply.diamond" :
                "cant.apply.diamond.1";
            return diags.create(dkind, log.currentSource(), pos, key,
                    Fragments.Diamond(site.tsym), details);
        }
    }

    /**
     * An InvalidSymbolError error class indicating that a symbol is not
     * accessible from a given site
     */
    class AccessError extends InvalidSymbolError {

        private Env<AttrContext> env;
        private Type site;

        AccessError(Env<AttrContext> env, Type site, Symbol sym) {
            super(HIDDEN, sym, "access error");
            this.env = env;
            this.site = site;
        }

        @Override
        public boolean exists() {
            return false;
        }

        @Override
        JCDiagnostic getDiagnostic(JCDiagnostic.DiagnosticType dkind,
                DiagnosticPosition pos,
                Symbol location,
                Type site,
                Name name,
                List<Type> argtypes,
                List<Type> typeargtypes) {
            if (sym.name == names.init && sym.owner != site.tsym) {
                return new SymbolNotFoundError(ABSENT_MTH).getDiagnostic(dkind,
                        pos, location, site, name, argtypes, typeargtypes);
            }
            else if ((sym.flags() & PUBLIC) != 0
                || (env != null && this.site != null
                    && !isAccessible(env, this.site))) {
                if (sym.owner.kind == PCK) {
                    return diags.create(dkind, log.currentSource(),
                            pos, "not.def.access.package.cant.access",
                        sym, sym.location(), inaccessiblePackageReason(env, sym.packge()));
                } else if (   sym.packge() != syms.rootPackage
                           && !symbolPackageVisible(env, sym)) {
                    return diags.create(dkind, log.currentSource(),
                            pos, "not.def.access.class.intf.cant.access.reason",
                            sym, sym.location(), sym.location().packge(),
                            inaccessiblePackageReason(env, sym.packge()));
                } else {
                    return diags.create(dkind, log.currentSource(),
                            pos, "not.def.access.class.intf.cant.access",
                        sym, sym.location());
                }
            }
            else if ((sym.flags() & (PRIVATE | PROTECTED)) != 0) {
                return diags.create(dkind, log.currentSource(),
                        pos, "report.access", sym,
                        asFlagSet(sym.flags() & (PRIVATE | PROTECTED)),
                        sym.location());
            }
            else {
                return diags.create(dkind, log.currentSource(),
                        pos, "not.def.public.cant.access", sym, sym.location());
            }
        }

        private String toString(Type type) {
            StringBuilder sb = new StringBuilder();
            sb.append(type);
            if (type != null) {
                sb.append("[tsym:").append(type.tsym);
                if (type.tsym != null)
                    sb.append("packge:").append(type.tsym.packge());
                sb.append("]");
            }
            return sb.toString();
        }
    }

    class InvisibleSymbolError extends InvalidSymbolError {

        private final Env<AttrContext> env;
        private final boolean suppressError;

        InvisibleSymbolError(Env<AttrContext> env, boolean suppressError, Symbol sym) {
            super(HIDDEN, sym, "invisible class error");
            this.env = env;
            this.suppressError = suppressError;
            this.name = sym.name;
        }

        @Override
        JCDiagnostic getDiagnostic(JCDiagnostic.DiagnosticType dkind,
                DiagnosticPosition pos,
                Symbol location,
                Type site,
                Name name,
                List<Type> argtypes,
                List<Type> typeargtypes) {
            if (suppressError)
                return null;

            if (sym.kind == PCK) {
                JCDiagnostic details = inaccessiblePackageReason(env, sym.packge());
                return diags.create(dkind, log.currentSource(),
                        pos, "package.not.visible", sym, details);
            }

            JCDiagnostic details = inaccessiblePackageReason(env, sym.packge());

            if (pos.getTree() != null) {
                Symbol o = sym;
                JCTree tree = pos.getTree();

                while (o.kind != PCK && tree.hasTag(SELECT)) {
                    o = o.owner;
                    tree = ((JCFieldAccess) tree).selected;
                }

                if (o.kind == PCK) {
                    pos = tree.pos();

                    return diags.create(dkind, log.currentSource(),
                            pos, "package.not.visible", o, details);
                }
            }

            return diags.create(dkind, log.currentSource(),
                    pos, "not.def.access.package.cant.access", sym, sym.packge(), details);
        }
    }

    JCDiagnostic inaccessiblePackageReason(Env<AttrContext> env, PackageSymbol sym) {
        //no dependency:
        if (!env.toplevel.modle.readModules.contains(sym.modle)) {
            //does not read:
            if (sym.modle != syms.unnamedModule) {
                if (env.toplevel.modle != syms.unnamedModule) {
                    return diags.fragment(Fragments.NotDefAccessDoesNotRead(env.toplevel.modle,
                                                                            sym,
                                                                            sym.modle));
                } else {
                    return diags.fragment(Fragments.NotDefAccessDoesNotReadFromUnnamed(sym,
                                                                                       sym.modle));
                }
            } else {
                return diags.fragment(Fragments.NotDefAccessDoesNotReadUnnamed(sym,
                                                                               env.toplevel.modle));
            }
        } else {
            if (sym.packge().modle.exports.stream().anyMatch(e -> e.packge == sym)) {
                //not exported to this module:
                if (env.toplevel.modle != syms.unnamedModule) {
                    return diags.fragment(Fragments.NotDefAccessNotExportedToModule(sym,
                                                                                    sym.modle,
                                                                                    env.toplevel.modle));
                } else {
                    return diags.fragment(Fragments.NotDefAccessNotExportedToModuleFromUnnamed(sym,
                                                                                               sym.modle));
                }
            } else {
                //not exported:
                if (env.toplevel.modle != syms.unnamedModule) {
                    return diags.fragment(Fragments.NotDefAccessNotExported(sym,
                                                                            sym.modle));
                } else {
                    return diags.fragment(Fragments.NotDefAccessNotExportedFromUnnamed(sym,
                                                                                       sym.modle));
                }
            }
        }
    }

    /**
     * InvalidSymbolError error class indicating that an instance member
     * has erroneously been accessed from a static context.
     */
    class StaticError extends InvalidSymbolError {

        StaticError(Symbol sym) {
            super(STATICERR, sym, "static error");
        }

        @Override
        JCDiagnostic getDiagnostic(JCDiagnostic.DiagnosticType dkind,
                DiagnosticPosition pos,
                Symbol location,
                Type site,
                Name name,
                List<Type> argtypes,
                List<Type> typeargtypes) {
            Symbol errSym = ((sym.kind == TYP && sym.type.hasTag(CLASS))
                ? types.erasure(sym.type).tsym
                : sym);
            return diags.create(dkind, log.currentSource(), pos,
                    "non-static.cant.be.ref", kindName(sym), errSym);
        }
    }

    /**
     * InvalidSymbolError error class indicating that a pair of symbols
     * (either methods, constructors or operands) are ambiguous
     * given an actual arguments/type argument list.
     */
    class AmbiguityError extends ResolveError {

        /** The other maximally specific symbol */
        List<Symbol> ambiguousSyms = List.nil();

        @Override
        public boolean exists() {
            return true;
        }

        AmbiguityError(Symbol sym1, Symbol sym2) {
            super(AMBIGUOUS, "ambiguity error");
            ambiguousSyms = flatten(sym2).appendList(flatten(sym1));
        }

        private List<Symbol> flatten(Symbol sym) {
            if (sym.kind == AMBIGUOUS) {
                return ((AmbiguityError)sym.baseSymbol()).ambiguousSyms;
            } else {
                return List.of(sym);
            }
        }

        AmbiguityError addAmbiguousSymbol(Symbol s) {
            ambiguousSyms = ambiguousSyms.prepend(s);
            return this;
        }

        @Override
        JCDiagnostic getDiagnostic(JCDiagnostic.DiagnosticType dkind,
                DiagnosticPosition pos,
                Symbol location,
                Type site,
                Name name,
                List<Type> argtypes,
                List<Type> typeargtypes) {
            List<Symbol> diagSyms = ambiguousSyms.reverse();
            Symbol s1 = diagSyms.head;
            Symbol s2 = diagSyms.tail.head;
            Name sname = s1.name;
            if (sname == names.init) sname = s1.owner.name;
            return diags.create(dkind, log.currentSource(),
                    pos, "ref.ambiguous", sname,
                    kindName(s1),
                    s1,
                    s1.location(site, types),
                    kindName(s2),
                    s2,
                    s2.location(site, types));
        }

        /**
         * If multiple applicable methods are found during overload and none of them
         * is more specific than the others, attempt to merge their signatures.
         */
        Symbol mergeAbstracts(Type site) {
            List<Symbol> ambiguousInOrder = ambiguousSyms.reverse();
            return types.mergeAbstracts(ambiguousInOrder, site, true).orElse(this);
        }

        @Override
        protected Symbol access(Name name, TypeSymbol location) {
            Symbol firstAmbiguity = ambiguousSyms.last();
            return firstAmbiguity.kind == TYP ?
                    types.createErrorType(name, location, firstAmbiguity.type).tsym :
                    firstAmbiguity;
        }
    }

    class BadVarargsMethod extends ResolveError {

        ResolveError delegatedError;

        BadVarargsMethod(ResolveError delegatedError) {
            super(delegatedError.kind, "badVarargs");
            this.delegatedError = delegatedError;
        }

        @Override
        public Symbol baseSymbol() {
            return delegatedError.baseSymbol();
        }

        @Override
        protected Symbol access(Name name, TypeSymbol location) {
            return delegatedError.access(name, location);
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        JCDiagnostic getDiagnostic(DiagnosticType dkind, DiagnosticPosition pos, Symbol location, Type site, Name name, List<Type> argtypes, List<Type> typeargtypes) {
            return delegatedError.getDiagnostic(dkind, pos, location, site, name, argtypes, typeargtypes);
        }
    }

    /**
     * BadMethodReferenceError error class indicating that a method reference symbol has been found,
     * but with the wrong staticness.
     */
    class BadMethodReferenceError extends StaticError {

        boolean unboundLookup;

        public BadMethodReferenceError(Symbol sym, boolean unboundLookup) {
            super(sym);
            this.unboundLookup = unboundLookup;
        }

        @Override
        JCDiagnostic getDiagnostic(DiagnosticType dkind, DiagnosticPosition pos, Symbol location, Type site, Name name, List<Type> argtypes, List<Type> typeargtypes) {
            final String key;
            if (!unboundLookup) {
                key = "bad.static.method.in.bound.lookup";
            } else if (sym.isStatic()) {
                key = "bad.static.method.in.unbound.lookup";
            } else {
                key = "bad.instance.method.in.unbound.lookup";
            }
            return sym.kind.isResolutionError() ?
                    ((ResolveError)sym).getDiagnostic(dkind, pos, location, site, name, argtypes, typeargtypes) :
                    diags.create(dkind, log.currentSource(), pos, key, Kinds.kindName(sym), sym);
        }
    }

    /**
     * BadConstructorReferenceError error class indicating that a constructor reference symbol has been found,
     * but pointing to a class for which an enclosing instance is not available.
     */
    class BadConstructorReferenceError extends InvalidSymbolError {

        public BadConstructorReferenceError(Symbol sym) {
            super(MISSING_ENCL, sym, "BadConstructorReferenceError");
        }

        @Override
        JCDiagnostic getDiagnostic(DiagnosticType dkind, DiagnosticPosition pos, Symbol location, Type site, Name name, List<Type> argtypes, List<Type> typeargtypes) {
           return diags.create(dkind, log.currentSource(), pos,
                "cant.access.inner.cls.constr", site.tsym.name, argtypes, site.getEnclosingType());
        }
    }

    class BadClassFileError extends InvalidSymbolError {

        private final CompletionFailure ex;

        public BadClassFileError(CompletionFailure ex) {
            super(HIDDEN, ex.sym, "BadClassFileError");
            this.name = sym.name;
            this.ex = ex;
        }

        @Override
        JCDiagnostic getDiagnostic(DiagnosticType dkind, DiagnosticPosition pos, Symbol location, Type site, Name name, List<Type> argtypes, List<Type> typeargtypes) {
            JCDiagnostic d = diags.create(dkind, log.currentSource(), pos,
                "cant.access", ex.sym, ex.getDetailValue());

            d.setFlag(DiagnosticFlag.NON_DEFERRABLE);
            return d;
        }

    }

    /**
     * Helper class for method resolution diagnostic simplification.
     * Certain resolution diagnostic are rewritten as simpler diagnostic
     * where the enclosing resolution diagnostic (i.e. 'inapplicable method')
     * is stripped away, as it doesn't carry additional info. The logic
     * for matching a given diagnostic is given in terms of a template
     * hierarchy: a diagnostic template can be specified programmatically,
     * so that only certain diagnostics are matched. Each templete is then
     * associated with a rewriter object that carries out the task of rewtiting
     * the diagnostic to a simpler one.
     */
    static class MethodResolutionDiagHelper {

        /**
         * A diagnostic rewriter transforms a method resolution diagnostic
         * into a simpler one
         */
        interface DiagnosticRewriter {
            JCDiagnostic rewriteDiagnostic(JCDiagnostic.Factory diags,
                    DiagnosticPosition preferredPos, DiagnosticSource preferredSource,
                    DiagnosticType preferredKind, JCDiagnostic d);
        }

        /**
         * A diagnostic template is made up of two ingredients: (i) a regular
         * expression for matching a diagnostic key and (ii) a list of sub-templates
         * for matching diagnostic arguments.
         */
        static class Template {

            /** regex used to match diag key */
            String regex;

            /** templates used to match diagnostic args */
            Template[] subTemplates;

            Template(String key, Template... subTemplates) {
                this.regex = key;
                this.subTemplates = subTemplates;
            }

            /**
             * Returns true if the regex matches the diagnostic key and if
             * all diagnostic arguments are matches by corresponding sub-templates.
             */
            boolean matches(Object o) {
                JCDiagnostic d = (JCDiagnostic)o;
                Object[] args = d.getArgs();
                if (!d.getCode().matches(regex) ||
                        subTemplates.length != d.getArgs().length) {
                    return false;
                }
                for (int i = 0; i < args.length ; i++) {
                    if (!subTemplates[i].matches(args[i])) {
                        return false;
                    }
                }
                return true;
            }
        }

        /**
         * Common rewriter for all argument mismatch simplifications.
         */
        static class ArgMismatchRewriter implements DiagnosticRewriter {

            /** the index of the subdiagnostic to be used as primary. */
            int causeIndex;

            public ArgMismatchRewriter(int causeIndex) {
                this.causeIndex = causeIndex;
            }

            @Override
            public JCDiagnostic rewriteDiagnostic(JCDiagnostic.Factory diags,
                    DiagnosticPosition preferredPos, DiagnosticSource preferredSource,
                    DiagnosticType preferredKind, JCDiagnostic d) {
                JCDiagnostic cause = (JCDiagnostic)d.getArgs()[causeIndex];
                DiagnosticPosition pos = d.getDiagnosticPosition();
                if (pos == null) {
                    pos = preferredPos;
                }
                return diags.create(preferredKind, preferredSource, pos,
                        "prob.found.req", cause);
            }
        }

        /** a dummy template that match any diagnostic argument */
        static final Template skip = new Template("") {
            @Override
            boolean matches(Object d) {
                return true;
            }
        };

        /** template for matching inference-free arguments mismatch failures */
        static final Template argMismatchTemplate = new Template(MethodCheckDiag.ARG_MISMATCH.regex(), skip);

        /** template for matching inference related arguments mismatch failures */
        static final Template inferArgMismatchTemplate = new Template(MethodCheckDiag.ARG_MISMATCH.regex(), skip, skip) {
            @Override
            boolean matches(Object o) {
                if (!super.matches(o)) {
                    return false;
                }
                JCDiagnostic d = (JCDiagnostic)o;
                @SuppressWarnings("unchecked")
                List<Type> tvars = (List<Type>)d.getArgs()[0];
                return !containsAny(d, tvars);
            }

            BiPredicate<Object, List<Type>> containsPredicate = (o, ts) -> {
                if (o instanceof Type type) {
                    return type.containsAny(ts);
                } else if (o instanceof JCDiagnostic diagnostic) {
                    return containsAny(diagnostic, ts);
                } else {
                    return false;
                }
            };

            boolean containsAny(JCDiagnostic d, List<Type> ts) {
                return Stream.of(d.getArgs())
                        .anyMatch(o -> containsPredicate.test(o, ts));
            }
        };

        /** rewriter map used for method resolution simplification */
        static final Map<Template, DiagnosticRewriter> rewriters = new LinkedHashMap<>();

        static {
            rewriters.put(argMismatchTemplate, new ArgMismatchRewriter(0));
            rewriters.put(inferArgMismatchTemplate, new ArgMismatchRewriter(1));
        }

        /**
         * Main entry point for diagnostic rewriting - given a diagnostic, see if any templates matches it,
         * and rewrite it accordingly.
         */
        static JCDiagnostic rewrite(JCDiagnostic.Factory diags, DiagnosticPosition pos, DiagnosticSource source,
                                    DiagnosticType dkind, JCDiagnostic d) {
            for (Map.Entry<Template, DiagnosticRewriter> _entry : rewriters.entrySet()) {
                if (_entry.getKey().matches(d)) {
                    JCDiagnostic simpleDiag =
                            _entry.getValue().rewriteDiagnostic(diags, pos, source, dkind, d);
                    simpleDiag.setFlag(DiagnosticFlag.COMPRESSED);
                    return simpleDiag;
                }
            }
            return null;
        }
    }

    enum MethodResolutionPhase {
        BASIC(false, false),
        BOX(true, false),
        VARARITY(true, true) {
            @Override
            public Symbol mergeResults(Symbol bestSoFar, Symbol sym) {
                //Check invariants (see {@code LookupHelper.shouldStop})
                Assert.check(bestSoFar.kind.isResolutionError() && bestSoFar.kind != AMBIGUOUS);
                if (!sym.kind.isResolutionError()) {
                    //varargs resolution successful
                    return sym;
                } else {
                    //pick best error
                    switch (bestSoFar.kind) {
                        case WRONG_MTH:
                        case WRONG_MTHS:
                            //Override previous errors if they were caused by argument mismatch.
                            //This generally means preferring current symbols - but we need to pay
                            //attention to the fact that the varargs lookup returns 'less' candidates
                            //than the previous rounds, and adjust that accordingly.
                            switch (sym.kind) {
                                case WRONG_MTH:
                                    //if the previous round matched more than one method, return that
                                    //result instead
                                    return bestSoFar.kind == WRONG_MTHS ?
                                            bestSoFar : sym;
                                case ABSENT_MTH:
                                    //do not override erroneous symbol if the arity lookup did not
                                    //match any method
                                    return bestSoFar;
                                case WRONG_MTHS:
                                default:
                                    //safe to override
                                    return sym;
                            }
                        default:
                            //otherwise, return first error
                            return bestSoFar;
                    }
                }
            }
        };

        final boolean isBoxingRequired;
        final boolean isVarargsRequired;

        MethodResolutionPhase(boolean isBoxingRequired, boolean isVarargsRequired) {
           this.isBoxingRequired = isBoxingRequired;
           this.isVarargsRequired = isVarargsRequired;
        }

        public boolean isBoxingRequired() {
            return isBoxingRequired;
        }

        public boolean isVarargsRequired() {
            return isVarargsRequired;
        }

        public Symbol mergeResults(Symbol prev, Symbol sym) {
            return sym;
        }
    }

    final List<MethodResolutionPhase> methodResolutionSteps = List.of(BASIC, BOX, VARARITY);

    /**
     * A resolution context is used to keep track of intermediate results of
     * overload resolution, such as list of method that are not applicable
     * (used to generate more precise diagnostics) and so on. Resolution contexts
     * can be nested - this means that when each overload resolution routine should
     * work within the resolution context it created.
     */
    class MethodResolutionContext {

        private List<Candidate> candidates = List.nil();

        MethodResolutionPhase step = null;

        MethodCheck methodCheck = resolveMethodCheck;

        private boolean internalResolution = false;
        private DeferredAttr.AttrMode attrMode = DeferredAttr.AttrMode.SPECULATIVE;

        void addInapplicableCandidate(Symbol sym, JCDiagnostic details) {
            Candidate c = new Candidate(currentResolutionContext.step, sym, details, null);
            candidates = candidates.append(c);
        }

        void addApplicableCandidate(Symbol sym, Type mtype) {
            Candidate c = new Candidate(currentResolutionContext.step, sym, null, mtype);
            candidates = candidates.append(c);
        }

        DeferredAttrContext deferredAttrContext(Symbol sym, InferenceContext inferenceContext, ResultInfo pendingResult, Warner warn) {
            DeferredAttrContext parent = (pendingResult == null)
                ? deferredAttr.emptyDeferredAttrContext
                : pendingResult.checkContext.deferredAttrContext();
            return deferredAttr.new DeferredAttrContext(attrMode, sym, step,
                    inferenceContext, parent, warn);
        }

        /**
         * This class represents an overload resolution candidate. There are two
         * kinds of candidates: applicable methods and inapplicable methods;
         * applicable methods have a pointer to the instantiated method type,
         * while inapplicable candidates contain further details about the
         * reason why the method has been considered inapplicable.
         */
        @SuppressWarnings("overrides")
        class Candidate {

            final MethodResolutionPhase step;
            final Symbol sym;
            final JCDiagnostic details;
            final Type mtype;

            private Candidate(MethodResolutionPhase step, Symbol sym, JCDiagnostic details, Type mtype) {
                this.step = step;
                this.sym = sym;
                this.details = details;
                this.mtype = mtype;
            }

            boolean isApplicable() {
                return mtype != null;
            }
        }

        DeferredAttr.AttrMode attrMode() {
            return attrMode;
        }

        boolean internal() {
            return internalResolution;
        }
    }

    MethodResolutionContext currentResolutionContext = null;
}
