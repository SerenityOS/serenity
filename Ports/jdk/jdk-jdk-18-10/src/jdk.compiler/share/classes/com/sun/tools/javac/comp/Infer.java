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

import com.sun.tools.javac.code.Source.Feature;
import com.sun.tools.javac.code.Type.UndetVar.UndetVarListener;
import com.sun.tools.javac.code.Types.TypeMapping;
import com.sun.tools.javac.comp.Attr.CheckMode;
import com.sun.tools.javac.resources.CompilerProperties.Fragments;
import com.sun.tools.javac.resources.CompilerProperties.Notes;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCTypeCast;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.GraphUtils.DottableNode;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.JCDiagnostic.Fragment;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Type.*;
import com.sun.tools.javac.code.Type.UndetVar.InferenceBound;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.comp.DeferredAttr.AttrMode;
import com.sun.tools.javac.comp.DeferredAttr.DeferredAttrContext;
import com.sun.tools.javac.comp.Infer.GraphSolver.InferenceGraph;
import com.sun.tools.javac.comp.Infer.GraphSolver.InferenceGraph.Node;
import com.sun.tools.javac.comp.Resolve.InapplicableMethodException;
import com.sun.tools.javac.comp.Resolve.VerboseResolutionMode;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static com.sun.tools.javac.code.TypeTag.*;

/** Helper class for type parameter inference, used by the attribution phase.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Infer {
    protected static final Context.Key<Infer> inferKey = new Context.Key<>();

    Resolve rs;
    Check chk;
    Symtab syms;
    Types types;
    JCDiagnostic.Factory diags;
    Log log;

    /** should the graph solver be used? */
    boolean allowGraphInference;

    /**
     * folder in which the inference dependency graphs should be written.
     */
    private final String dependenciesFolder;

    /**
     * List of graphs awaiting to be dumped to a file.
     */
    private List<String> pendingGraphs;

    public static Infer instance(Context context) {
        Infer instance = context.get(inferKey);
        if (instance == null)
            instance = new Infer(context);
        return instance;
    }

    protected Infer(Context context) {
        context.put(inferKey, this);

        rs = Resolve.instance(context);
        chk = Check.instance(context);
        syms = Symtab.instance(context);
        types = Types.instance(context);
        diags = JCDiagnostic.Factory.instance(context);
        log = Log.instance(context);
        Options options = Options.instance(context);
        Source source = Source.instance(context);
        allowGraphInference = Feature.GRAPH_INFERENCE.allowedInSource(source)
                && options.isUnset("useLegacyInference");
        dependenciesFolder = options.get("debug.dumpInferenceGraphsTo");
        pendingGraphs = List.nil();

        emptyContext = new InferenceContext(this, List.nil());
    }

    /** A value for prototypes that admit any type, including polymorphic ones. */
    public static final Type anyPoly = new JCNoType();

   /**
    * This exception class is design to store a list of diagnostics corresponding
    * to inference errors that can arise during a method applicability check.
    */
    public static class InferenceException extends InapplicableMethodException {
        private static final long serialVersionUID = 0;

        transient List<JCDiagnostic> messages = List.nil();

        InferenceException() {
            super(null);
        }

        @Override
        public JCDiagnostic getDiagnostic() {
            return messages.head;
        }
    }

    InferenceException error(JCDiagnostic diag) {
        InferenceException result = new InferenceException();
        if (diag != null) {
            result.messages = result.messages.append(diag);
        }
        return result;
    }

    // <editor-fold defaultstate="collapsed" desc="Inference routines">
    /**
     * Main inference entry point - instantiate a generic method type
     * using given argument types and (possibly) an expected target-type.
     */
    Type instantiateMethod( Env<AttrContext> env,
                            List<Type> tvars,
                            MethodType mt,
                            Attr.ResultInfo resultInfo,
                            MethodSymbol msym,
                            List<Type> argtypes,
                            boolean allowBoxing,
                            boolean useVarargs,
                            Resolve.MethodResolutionContext resolveContext,
                            Warner warn) throws InferenceException {
        //-System.err.println("instantiateMethod(" + tvars + ", " + mt + ", " + argtypes + ")"); //DEBUG
        final InferenceContext inferenceContext = new InferenceContext(this, tvars);  //B0
        try {
            DeferredAttr.DeferredAttrContext deferredAttrContext =
                        resolveContext.deferredAttrContext(msym, inferenceContext, resultInfo, warn);

            resolveContext.methodCheck.argumentsAcceptable(env, deferredAttrContext,   //B2
                    argtypes, mt.getParameterTypes(), warn);

            if (allowGraphInference && resultInfo != null && resultInfo.pt == anyPoly) {
                doIncorporation(inferenceContext, warn);
                //we are inside method attribution - just return a partially inferred type
                return new PartiallyInferredMethodType(mt, inferenceContext, env, warn);
            } else if (allowGraphInference && resultInfo != null) {

                //inject return constraints earlier
                doIncorporation(inferenceContext, warn); //propagation

                if (!warn.hasNonSilentLint(Lint.LintCategory.UNCHECKED)) {
                    boolean shouldPropagate = shouldPropagate(mt.getReturnType(), resultInfo, inferenceContext);

                    InferenceContext minContext = shouldPropagate ?
                            inferenceContext.min(roots(mt, deferredAttrContext), true, warn) :
                            inferenceContext;

                    Type newRestype = generateReturnConstraints(env.tree, resultInfo,  //B3
                            mt, minContext);
                    mt = (MethodType)types.createMethodTypeWithReturn(mt, newRestype);

                    //propagate outwards if needed
                    if (shouldPropagate) {
                        //propagate inference context outwards and exit
                        minContext.dupTo(resultInfo.checkContext.inferenceContext());
                        deferredAttrContext.complete();
                        return mt;
                    }
                }
            }

            deferredAttrContext.complete();

            // minimize as yet undetermined type variables
            if (allowGraphInference) {
                inferenceContext.solve(warn);
            } else {
                inferenceContext.solveLegacy(true, warn, LegacyInferenceSteps.EQ_LOWER.steps); //minimizeInst
            }

            mt = (MethodType)inferenceContext.asInstType(mt);

            if (!allowGraphInference &&
                    inferenceContext.restvars().nonEmpty() &&
                    resultInfo != null &&
                    !warn.hasNonSilentLint(Lint.LintCategory.UNCHECKED)) {
                generateReturnConstraints(env.tree, resultInfo, mt, inferenceContext);
                inferenceContext.solveLegacy(false, warn, LegacyInferenceSteps.EQ_UPPER.steps); //maximizeInst
                mt = (MethodType)inferenceContext.asInstType(mt);
            }

            if (resultInfo != null && rs.verboseResolutionMode.contains(VerboseResolutionMode.DEFERRED_INST)) {
                log.note(env.tree.pos, Notes.DeferredMethodInst(msym, mt, resultInfo.pt));
            }

            // return instantiated version of method type
            return mt;
        } finally {
            if (resultInfo != null || !allowGraphInference) {
                inferenceContext.notifyChange();
            } else {
                inferenceContext.notifyChange(inferenceContext.boundedVars());
            }
            if (resultInfo == null) {
                /* if the is no result info then we can clear the capture types
                 * cache without affecting any result info check
                 */
                inferenceContext.captureTypeCache.clear();
            }
            dumpGraphsIfNeeded(env.tree, msym, resolveContext);
        }
    }
    //where
        private boolean shouldPropagate(Type restype, Attr.ResultInfo target, InferenceContext inferenceContext) {
            return target.checkContext.inferenceContext() != emptyContext && //enclosing context is a generic method
                        inferenceContext.free(restype) && //return type contains inference vars
                        (!inferenceContext.inferencevars.contains(restype) || //no eager instantiation is required (as per 18.5.2)
                                !needsEagerInstantiation((UndetVar)inferenceContext.asUndetVar(restype), target.pt, inferenceContext));
        }

        private List<Type> roots(MethodType mt, DeferredAttrContext deferredAttrContext) {
            if (deferredAttrContext != null && deferredAttrContext.mode == AttrMode.CHECK) {
                ListBuffer<Type> roots = new ListBuffer<>();
                roots.add(mt.getReturnType());
                for (DeferredAttr.DeferredAttrNode n : deferredAttrContext.deferredAttrNodes) {
                    roots.addAll(n.deferredStuckPolicy.stuckVars());
                    roots.addAll(n.deferredStuckPolicy.depVars());
                }
                List<Type> thrownVars = deferredAttrContext.inferenceContext.inferencevars.stream()
                                .filter(tv -> (tv.tsym.flags() & Flags.THROWS) != 0).collect(List.collector());
                List<Type> result = roots.toList();
                result = result.appendList(thrownVars.diff(result));
                return result;
            } else {
                return List.of(mt.getReturnType());
            }
        }

    /**
     * A partially inferred method/constructor type; such a type can be checked multiple times
     * against different targets.
     */
    public class PartiallyInferredMethodType extends MethodType {
        public PartiallyInferredMethodType(MethodType mtype, InferenceContext inferenceContext, Env<AttrContext> env, Warner warn) {
            super(mtype.getParameterTypes(), mtype.getReturnType(), mtype.getThrownTypes(), mtype.tsym);
            this.inferenceContext = inferenceContext;
            this.env = env;
            this.warn = warn;
        }

        /** The inference context. */
        final InferenceContext inferenceContext;

        /** The attribution environment. */
        Env<AttrContext> env;

        /** The warner. */
        final Warner warn;

        @Override
        public boolean isPartial() {
            return true;
        }

        /**
         * Checks this type against a target; this means generating return type constraints, solve
         * and then roll back the results (to avoid polluting the context).
         */
        Type check(Attr.ResultInfo resultInfo) {
            Warner noWarnings = new Warner(null);
            List<Type> saved_undet = null;
            try {
                /** we need to save the inference context before generating target type constraints.
                 *  This constraints may pollute the inference context and make it useless in case we
                 *  need to use it several times: with several targets.
                 */
                saved_undet = inferenceContext.save();
                boolean unchecked = warn.hasNonSilentLint(Lint.LintCategory.UNCHECKED);
                if (!unchecked) {
                    boolean shouldPropagate = shouldPropagate(getReturnType(), resultInfo, inferenceContext);

                    InferenceContext minContext = shouldPropagate ?
                            inferenceContext.min(roots(asMethodType(), null), false, warn) :
                            inferenceContext;

                    MethodType other = (MethodType)minContext.update(asMethodType());
                    Type newRestype = generateReturnConstraints(env.tree, resultInfo,  //B3
                            other, minContext);

                    if (shouldPropagate) {
                        //propagate inference context outwards and exit
                        minContext.dupTo(resultInfo.checkContext.inferenceContext(),
                                resultInfo.checkContext.deferredAttrContext().insideOverloadPhase());
                        return newRestype;
                    }
                }
                inferenceContext.solve(noWarnings);
                Type ret = inferenceContext.asInstType(this).getReturnType();
                if (unchecked) {
                    //inline logic from Attr.checkMethod - if unchecked conversion was required, erase
                    //return type _after_ resolution, and check against target
                    ret = types.erasure(ret);
                }
                return resultInfo.check(env.tree, ret);
            } catch (InferenceException ex) {
                resultInfo.checkContext.report(null, ex.getDiagnostic());
                Assert.error(); //cannot get here (the above should throw)
                return null;
            } finally {
                if (saved_undet != null) {
                    inferenceContext.rollback(saved_undet);
                }
            }
        }
    }

    private void dumpGraphsIfNeeded(DiagnosticPosition pos, Symbol msym, Resolve.MethodResolutionContext rsContext) {
        int round = 0;
        try {
            for (String graph : pendingGraphs.reverse()) {
                Assert.checkNonNull(dependenciesFolder);
                Name name = msym.name == msym.name.table.names.init ?
                        msym.owner.name : msym.name;
                String filename = String.format("%s@%s[mode=%s,step=%s]_%d.dot",
                        name,
                        pos.getStartPosition(),
                        rsContext.attrMode(),
                        rsContext.step,
                        round);
                Path dotFile = Paths.get(dependenciesFolder, filename);
                try (Writer w = Files.newBufferedWriter(dotFile)) {
                    w.append(graph);
                }
                round++;
            }
        } catch (IOException ex) {
            Assert.error("Error occurred when dumping inference graph: " + ex.getMessage());
        } finally {
            pendingGraphs = List.nil();
        }
    }

    /**
     * Generate constraints from the generic method's return type. If the method
     * call occurs in a context where a type T is expected, use the expected
     * type to derive more constraints on the generic method inference variables.
     */
    Type generateReturnConstraints(JCTree tree, Attr.ResultInfo resultInfo,
            MethodType mt, InferenceContext inferenceContext) {
        InferenceContext rsInfoInfContext = resultInfo.checkContext.inferenceContext();
        Type from = mt.getReturnType();
        if (mt.getReturnType().containsAny(inferenceContext.inferencevars) &&
                rsInfoInfContext != emptyContext) {
            from = types.capture(from);
            //add synthetic captured ivars
            for (Type t : from.getTypeArguments()) {
                if (t.hasTag(TYPEVAR) && ((TypeVar)t).isCaptured()) {
                    inferenceContext.addVar((TypeVar)t);
                }
            }
        }
        Type qtype = inferenceContext.asUndetVar(from);
        Type to = resultInfo.pt;

        if (qtype.hasTag(VOID)) {
            to = syms.voidType;
        } else if (to.hasTag(NONE)) {
            to = from.isPrimitive() ? from : syms.objectType;
        } else if (qtype.hasTag(UNDETVAR)) {
            if (needsEagerInstantiation((UndetVar)qtype, to, inferenceContext) &&
                    (allowGraphInference || !to.isPrimitive())) {
                to = generateReferenceToTargetConstraint(tree, (UndetVar)qtype, to, resultInfo, inferenceContext);
            }
        } else if (rsInfoInfContext.free(resultInfo.pt)) {
            //propagation - cache captured vars
            qtype = inferenceContext.asUndetVar(rsInfoInfContext.cachedCapture(tree, from, !resultInfo.checkMode.updateTreeType()));
        }
        Assert.check(allowGraphInference || !rsInfoInfContext.free(to),
                "legacy inference engine cannot handle constraints on both sides of a subtyping assertion");
        //we need to skip capture?
        Warner retWarn = new Warner();
        if (!resultInfo.checkContext.compatible(qtype, rsInfoInfContext.asUndetVar(to), retWarn) ||
                //unchecked conversion is not allowed in source 7 mode
                (!allowGraphInference && retWarn.hasLint(Lint.LintCategory.UNCHECKED))) {
            throw error(diags.fragment(Fragments.InferNoConformingInstanceExists(inferenceContext.restvars(), mt.getReturnType(), to)));
        }
        return from;
    }

    private boolean needsEagerInstantiation(UndetVar from, Type to, InferenceContext inferenceContext) {
        if (to.isPrimitive()) {
            /* T is a primitive type, and one of the primitive wrapper classes is an instantiation,
             * upper bound, or lower bound for alpha in B2.
             */
            for (Type t : from.getBounds(InferenceBound.values())) {
                Type boundAsPrimitive = types.unboxedType(t);
                if (boundAsPrimitive == null || boundAsPrimitive.hasTag(NONE)) {
                    continue;
                }
                return true;
            }
            return false;
        }

        Type captureOfTo = types.capture(to);
        /* T is a reference type, but is not a wildcard-parameterized type, and either
         */
        if (captureOfTo == to) { //not a wildcard parameterized type
            /* i) B2 contains a bound of one of the forms alpha = S or S <: alpha,
             *      where S is a wildcard-parameterized type, or
             */
            for (Type t : from.getBounds(InferenceBound.EQ, InferenceBound.LOWER)) {
                Type captureOfBound = types.capture(t);
                if (captureOfBound != t) {
                    return true;
                }
            }

            /* ii) B2 contains two bounds of the forms S1 <: alpha and S2 <: alpha,
             * where S1 and S2 have supertypes that are two different
             * parameterizations of the same generic class or interface.
             */
            for (Type aLowerBound : from.getBounds(InferenceBound.LOWER)) {
                for (Type anotherLowerBound : from.getBounds(InferenceBound.LOWER)) {
                    if (aLowerBound != anotherLowerBound &&
                            !inferenceContext.free(aLowerBound) &&
                            !inferenceContext.free(anotherLowerBound) &&
                            commonSuperWithDiffParameterization(aLowerBound, anotherLowerBound)) {
                        return true;
                    }
                }
            }
        }

        /* T is a parameterization of a generic class or interface, G,
         * and B2 contains a bound of one of the forms alpha = S or S <: alpha,
         * where there exists no type of the form G<...> that is a
         * supertype of S, but the raw type G is a supertype of S
         */
        if (to.isParameterized()) {
            for (Type t : from.getBounds(InferenceBound.EQ, InferenceBound.LOWER)) {
                Type sup = types.asSuper(t, to.tsym);
                if (sup != null && sup.isRaw()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean commonSuperWithDiffParameterization(Type t, Type s) {
        for (Pair<Type, Type> supers : getParameterizedSupers(t, s)) {
            if (!types.isSameType(supers.fst, supers.snd)) return true;
        }
        return false;
    }

    private Type generateReferenceToTargetConstraint(JCTree tree, UndetVar from,
            Type to, Attr.ResultInfo resultInfo,
            InferenceContext inferenceContext) {
        inferenceContext.solve(List.of(from.qtype), new Warner());
        inferenceContext.notifyChange();
        Type capturedType = resultInfo.checkContext.inferenceContext()
                .cachedCapture(tree, from.getInst(), !resultInfo.checkMode.updateTreeType());
        if (types.isConvertible(capturedType,
                resultInfo.checkContext.inferenceContext().asUndetVar(to))) {
            //effectively skip additional return-type constraint generation (compatibility)
            return syms.objectType;
        }
        return to;
    }

    /**
      * Infer cyclic inference variables as described in 15.12.2.8.
      */
    void instantiateAsUninferredVars(List<Type> vars, InferenceContext inferenceContext) {
        ListBuffer<Type> todo = new ListBuffer<>();
        //step 1 - create fresh tvars
        for (Type t : vars) {
            UndetVar uv = (UndetVar)inferenceContext.asUndetVar(t);
            List<Type> upperBounds = uv.getBounds(InferenceBound.UPPER);
            if (Type.containsAny(upperBounds, vars)) {
                TypeSymbol fresh_tvar = new TypeVariableSymbol(Flags.SYNTHETIC, uv.qtype.tsym.name, null, uv.qtype.tsym.owner);
                fresh_tvar.type = new TypeVar(fresh_tvar, types.makeIntersectionType(uv.getBounds(InferenceBound.UPPER)), syms.botType);
                todo.append(uv);
                uv.setInst(fresh_tvar.type);
            } else if (upperBounds.nonEmpty()) {
                uv.setInst(types.glb(upperBounds));
            } else {
                uv.setInst(syms.objectType);
            }
        }
        //step 2 - replace fresh tvars in their bounds
        List<Type> formals = vars;
        for (Type t : todo) {
            UndetVar uv = (UndetVar)t;
            TypeVar ct = (TypeVar)uv.getInst();
            ct.setUpperBound( types.glb(inferenceContext.asInstTypes(types.getBounds(ct))) );
            if (ct.getUpperBound().isErroneous()) {
                //report inference error if glb fails
                reportBoundError(uv, InferenceBound.UPPER);
            }
            formals = formals.tail;
        }
    }

    /**
     * Compute a synthetic method type corresponding to the requested polymorphic
     * method signature. The target return type is computed from the immediately
     * enclosing scope surrounding the polymorphic-signature call.
     */
    Type instantiatePolymorphicSignatureInstance(Env<AttrContext> env,
                                            MethodSymbol spMethod,  // sig. poly. method or null if none
                                            Resolve.MethodResolutionContext resolveContext,
                                            List<Type> argtypes) {
        final Type restype;

        Type spType = spMethod == null ? syms.objectType : spMethod.getReturnType();

        switch (env.next.tree.getTag()) {
            case TYPECAST:
                JCTypeCast castTree = (JCTypeCast)env.next.tree;
                restype = (TreeInfo.skipParens(castTree.expr) == env.tree) ?
                          castTree.clazz.type :
                          spType;
                break;
            case EXEC:
                JCTree.JCExpressionStatement execTree =
                        (JCTree.JCExpressionStatement)env.next.tree;
                restype = (TreeInfo.skipParens(execTree.expr) == env.tree) ?
                          syms.voidType :
                          spType;
                break;
            default:
                restype = spType;
        }

        List<Type> paramtypes = argtypes.map(new ImplicitArgType(spMethod, resolveContext.step));
        List<Type> exType = spMethod != null ?
            spMethod.getThrownTypes() :
            List.of(syms.throwableType); // make it throw all exceptions

        MethodType mtype = new MethodType(paramtypes,
                                          restype,
                                          exType,
                                          syms.methodClass);
        return mtype;
    }
    //where
        class ImplicitArgType extends DeferredAttr.DeferredTypeMap<Void> {

            public ImplicitArgType(Symbol msym, Resolve.MethodResolutionPhase phase) {
                (rs.deferredAttr).super(AttrMode.SPECULATIVE, msym, phase);
            }

            @Override
            public Type visitClassType(ClassType t, Void aVoid) {
                return types.erasure(t);
            }

            @Override
            public Type visitType(Type t, Void _unused) {
                if (t.hasTag(DEFERRED)) {
                    return visit(super.visitType(t, null));
                } else if (t.hasTag(BOT))
                    // nulls type as the marker type Null (which has no instances)
                    // infer as java.lang.Void for now
                    t = types.boxedClass(syms.voidType).type;
                return t;
            }
        }

    TypeMapping<Void> fromTypeVarFun = new StructuralTypeMapping<Void>() {
        @Override
        public Type visitTypeVar(TypeVar tv, Void aVoid) {
            UndetVar uv = new UndetVar(tv, incorporationEngine(), types);
            if ((tv.tsym.flags() & Flags.THROWS) != 0) {
                uv.setThrow();
            }
            return uv;
        }
    };

    /**
      * This method is used to infer a suitable target SAM in case the original
      * SAM type contains one or more wildcards. An inference process is applied
      * so that wildcard bounds, as well as explicit lambda/method ref parameters
      * (where applicable) are used to constraint the solution.
      */
    public Type instantiateFunctionalInterface(DiagnosticPosition pos, Type funcInterface,
            List<Type> paramTypes, Check.CheckContext checkContext) {
        if (types.capture(funcInterface) == funcInterface) {
            //if capture doesn't change the type then return the target unchanged
            //(this means the target contains no wildcards!)
            return funcInterface;
        } else {
            Type formalInterface = funcInterface.tsym.type;
            InferenceContext funcInterfaceContext =
                    new InferenceContext(this, funcInterface.tsym.type.getTypeArguments());

            Assert.check(paramTypes != null);
            //get constraints from explicit params (this is done by
            //checking that explicit param types are equal to the ones
            //in the functional interface descriptors)
            List<Type> descParameterTypes = types.findDescriptorType(formalInterface).getParameterTypes();
            if (descParameterTypes.size() != paramTypes.size()) {
                checkContext.report(pos, diags.fragment(Fragments.IncompatibleArgTypesInLambda));
                return types.createErrorType(funcInterface);
            }
            for (Type p : descParameterTypes) {
                if (!types.isSameType(funcInterfaceContext.asUndetVar(p), paramTypes.head)) {
                    checkContext.report(pos, diags.fragment(Fragments.NoSuitableFunctionalIntfInst(funcInterface)));
                    return types.createErrorType(funcInterface);
                }
                paramTypes = paramTypes.tail;
            }

            List<Type> actualTypeargs = funcInterface.getTypeArguments();
            for (Type t : funcInterfaceContext.undetvars) {
                UndetVar uv = (UndetVar)t;
                Optional<Type> inst = uv.getBounds(InferenceBound.EQ).stream()
                        .filter(b -> !b.containsAny(formalInterface.getTypeArguments())).findFirst();
                uv.setInst(inst.orElse(actualTypeargs.head));
                actualTypeargs = actualTypeargs.tail;
            }

            Type owntype = funcInterfaceContext.asInstType(formalInterface);
            if (!chk.checkValidGenericType(owntype)) {
                //if the inferred functional interface type is not well-formed,
                //or if it's not a subtype of the original target, issue an error
                checkContext.report(pos, diags.fragment(Fragments.NoSuitableFunctionalIntfInst(funcInterface)));
            }
            //propagate constraints as per JLS 18.2.1
            checkContext.compatible(owntype, funcInterface, types.noWarnings);
            return owntype;
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Incorporation">

    /**
     * This class is the root of all incorporation actions.
     */
    public abstract class IncorporationAction {
        UndetVar uv;
        Type t;

        IncorporationAction(UndetVar uv, Type t) {
            this.uv = uv;
            this.t = t;
        }

        public abstract IncorporationAction dup(UndetVar that);

        /**
         * Incorporation action entry-point. Subclasses should define the logic associated with
         * this incorporation action.
         */
        abstract void apply(InferenceContext ic, Warner warn);

        /**
         * Helper function: perform subtyping through incorporation cache.
         */
        boolean isSubtype(Type s, Type t, Warner warn) {
            return doIncorporationOp(IncorporationBinaryOpKind.IS_SUBTYPE, s, t, warn);
        }

        /**
         * Helper function: perform type-equivalence through incorporation cache.
         */
        boolean isSameType(Type s, Type t) {
            return doIncorporationOp(IncorporationBinaryOpKind.IS_SAME_TYPE, s, t, null);
        }

        @Override
        public String toString() {
            return String.format("%s[undet=%s,t=%s]", getClass().getSimpleName(), uv.qtype, t);
        }
    }

    /**
     * Bound-check incorporation action. A newly added bound is checked against existing bounds,
     * to verify its compatibility; each bound is checked using either subtyping or type equivalence.
     */
    class CheckBounds extends IncorporationAction {

        InferenceBound from;
        BiFunction<InferenceContext, Type, Type> typeFunc;
        BiPredicate<InferenceContext, Type> optFilter;

        CheckBounds(UndetVar uv, Type t, InferenceBound from) {
            this(uv, t, InferenceContext::asUndetVar, null, from);
        }

        CheckBounds(UndetVar uv, Type t, BiFunction<InferenceContext, Type, Type> typeFunc,
                    BiPredicate<InferenceContext, Type> typeFilter, InferenceBound from) {
            super(uv, t);
            this.from = from;
            this.typeFunc = typeFunc;
            this.optFilter = typeFilter;
        }

        @Override
        public IncorporationAction dup(UndetVar that) {
            return new CheckBounds(that, t, typeFunc, optFilter, from);
        }

        @Override
        void apply(InferenceContext inferenceContext, Warner warn) {
            t = typeFunc.apply(inferenceContext, t);
            if (optFilter != null && optFilter.test(inferenceContext, t)) return;
            for (InferenceBound to : boundsToCheck()) {
                for (Type b : uv.getBounds(to)) {
                    b = typeFunc.apply(inferenceContext, b);
                    if (optFilter != null && optFilter.test(inferenceContext, b)) continue;
                    boolean success = checkBound(t, b, from, to, warn);
                    if (!success) {
                        report(from, to);
                    }
                }
            }
        }

        /**
         * The list of bound kinds to be checked.
         */
        EnumSet<InferenceBound> boundsToCheck() {
            return (from == InferenceBound.EQ) ?
                            EnumSet.allOf(InferenceBound.class) :
                            EnumSet.complementOf(EnumSet.of(from));
        }

        /**
         * Is source type 's' compatible with target type 't' given source and target bound kinds?
         */
        boolean checkBound(Type s, Type t, InferenceBound ib_s, InferenceBound ib_t, Warner warn) {
            if (ib_s.lessThan(ib_t)) {
                return isSubtype(s, t, warn);
            } else if (ib_t.lessThan(ib_s)) {
                return isSubtype(t, s, warn);
            } else {
                return isSameType(s, t);
            }
        }

        /**
         * Report a bound check error.
         */
        void report(InferenceBound from, InferenceBound to) {
            //this is a workaround to preserve compatibility with existing messages
            if (from == to) {
                reportBoundError(uv, from);
            } else if (from == InferenceBound.LOWER || to == InferenceBound.EQ) {
                reportBoundError(uv, to, from);
            } else {
                reportBoundError(uv, from, to);
            }
        }

        @Override
        public String toString() {
            return String.format("%s[undet=%s,t=%s,bound=%s]", getClass().getSimpleName(), uv.qtype, t, from);
        }
    }

    /**
     * Custom check executed by the legacy incorporation engine. Newly added bounds are checked
     * against existing eq bounds.
     */
    class EqCheckLegacy extends CheckBounds {
        EqCheckLegacy(UndetVar uv, Type t, InferenceBound from) {
            super(uv, t, InferenceContext::asInstType, InferenceContext::free, from);
        }

        @Override
        public IncorporationAction dup(UndetVar that) {
            return new EqCheckLegacy(that, t, from);
        }

        @Override
        EnumSet<InferenceBound> boundsToCheck() {
            return (from == InferenceBound.EQ) ?
                            EnumSet.allOf(InferenceBound.class) :
                            EnumSet.of(InferenceBound.EQ);
        }
    }

    /**
     * Check that the inferred type conforms to all bounds.
     */
    class CheckInst extends CheckBounds {

        EnumSet<InferenceBound> to;

        CheckInst(UndetVar uv, InferenceBound ib, InferenceBound... rest) {
            this(uv, EnumSet.of(ib, rest));
        }

        CheckInst(UndetVar uv, EnumSet<InferenceBound> to) {
            super(uv, uv.getInst(), InferenceBound.EQ);
            this.to = to;
        }

        @Override
        public IncorporationAction dup(UndetVar that) {
            return new CheckInst(that, to);
        }

        @Override
        EnumSet<InferenceBound> boundsToCheck() {
            return to;
        }

        @Override
        void report(InferenceBound from, InferenceBound to) {
            reportInstError(uv, to);
        }
    }

    /**
     * Replace undetvars in bounds and check that the inferred type conforms to all bounds.
     */
    class SubstBounds extends CheckInst {
        SubstBounds(UndetVar uv) {
            super(uv, InferenceBound.LOWER, InferenceBound.EQ, InferenceBound.UPPER);
        }

        @Override
        public IncorporationAction dup(UndetVar that) {
            return new SubstBounds(that);
        }

        @Override
        void apply(InferenceContext inferenceContext, Warner warn) {
            for (Type undet : inferenceContext.undetvars) {
                //we could filter out variables not mentioning uv2...
                UndetVar uv2 = (UndetVar)undet;
                uv2.substBounds(List.of(uv.qtype), List.of(uv.getInst()), types);
                checkCompatibleUpperBounds(uv2, inferenceContext);
            }
            super.apply(inferenceContext, warn);
        }

        /**
         * Make sure that the upper bounds we got so far lead to a solvable inference
         * variable by making sure that a glb exists.
         */
        void checkCompatibleUpperBounds(UndetVar uv, InferenceContext inferenceContext) {
            List<Type> hibounds =
                    Type.filter(uv.getBounds(InferenceBound.UPPER), new BoundFilter(inferenceContext));
            final Type hb;
            if (hibounds.isEmpty())
                hb = syms.objectType;
            else if (hibounds.tail.isEmpty())
                hb = hibounds.head;
            else
                hb = types.glb(hibounds);
            if (hb == null || hb.isErroneous())
                reportBoundError(uv, InferenceBound.UPPER);
        }
    }

    /**
     * Perform pairwise comparison between common generic supertypes of two upper bounds.
     */
    class CheckUpperBounds extends IncorporationAction {

        public CheckUpperBounds(UndetVar uv, Type t) {
            super(uv, t);
        }

        @Override
        public IncorporationAction dup(UndetVar that) {
            return new CheckUpperBounds(that, t);
        }

        @Override
        void apply(InferenceContext inferenceContext, Warner warn) {
            List<Type> boundList = uv.getBounds(InferenceBound.UPPER).stream()
                    .collect(types.closureCollector(true, types::isSameType));
            for (Type b2 : boundList) {
                if (t == b2) continue;
                    /* This wildcard check is temporary workaround. This code may need to be
                     * revisited once spec bug JDK-7034922 is fixed.
                     */
                if (t != b2 && !t.hasTag(WILDCARD) && !b2.hasTag(WILDCARD)) {
                    for (Pair<Type, Type> commonSupers : getParameterizedSupers(t, b2)) {
                        List<Type> allParamsSuperBound1 = commonSupers.fst.allparams();
                        List<Type> allParamsSuperBound2 = commonSupers.snd.allparams();
                        while (allParamsSuperBound1.nonEmpty() && allParamsSuperBound2.nonEmpty()) {
                            //traverse the list of all params comparing them
                            if (!allParamsSuperBound1.head.hasTag(WILDCARD) &&
                                    !allParamsSuperBound2.head.hasTag(WILDCARD)) {
                                if (!isSameType(inferenceContext.asUndetVar(allParamsSuperBound1.head),
                                        inferenceContext.asUndetVar(allParamsSuperBound2.head))) {
                                    reportBoundError(uv, InferenceBound.UPPER);
                                }
                            }
                            allParamsSuperBound1 = allParamsSuperBound1.tail;
                            allParamsSuperBound2 = allParamsSuperBound2.tail;
                        }
                        Assert.check(allParamsSuperBound1.isEmpty() && allParamsSuperBound2.isEmpty());
                    }
                }
            }
        }
    }

    /**
     * Perform propagation of bounds. Given a constraint of the kind {@code alpha <: T}, three
     * kind of propagation occur:
     *
     * <li>T is copied into all matching bounds (i.e. lower/eq bounds) B of alpha such that B=beta (forward propagation)</li>
     * <li>if T=beta, matching bounds (i.e. upper bounds) of beta are copied into alpha (backwards propagation)</li>
     * <li>if T=beta, sets a symmetric bound on beta (i.e. beta :> alpha) (symmetric propagation) </li>
     */
    class PropagateBounds extends IncorporationAction {

        InferenceBound ib;

        public PropagateBounds(UndetVar uv, Type t, InferenceBound ib) {
            super(uv, t);
            this.ib = ib;
        }

        @Override
        public IncorporationAction dup(UndetVar that) {
            return new PropagateBounds(that, t, ib);
        }

        void apply(InferenceContext inferenceContext, Warner warner) {
            Type undetT = inferenceContext.asUndetVar(t);
            if (undetT.hasTag(UNDETVAR) && !((UndetVar)undetT).isCaptured()) {
                UndetVar uv2 = (UndetVar)undetT;
                //symmetric propagation
                uv2.addBound(ib.complement(), uv, types);
                //backwards propagation
                for (InferenceBound ib2 : backwards()) {
                    for (Type b : uv2.getBounds(ib2)) {
                        uv.addBound(ib2, b, types);
                    }
                }
            }
            //forward propagation
            for (InferenceBound ib2 : forward()) {
                for (Type l : uv.getBounds(ib2)) {
                    Type undet = inferenceContext.asUndetVar(l);
                    if (undet.hasTag(TypeTag.UNDETVAR) && !((UndetVar)undet).isCaptured()) {
                        UndetVar uv2 = (UndetVar)undet;
                        uv2.addBound(ib, inferenceContext.asInstType(t), types);
                    }
                }
            }
        }

        EnumSet<InferenceBound> forward() {
            return (ib == InferenceBound.EQ) ?
                    EnumSet.of(InferenceBound.EQ) : EnumSet.complementOf(EnumSet.of(ib));
        }

        EnumSet<InferenceBound> backwards() {
            return (ib == InferenceBound.EQ) ?
                    EnumSet.allOf(InferenceBound.class) : EnumSet.of(ib);
        }

        @Override
        public String toString() {
            return String.format("%s[undet=%s,t=%s,bound=%s]", getClass().getSimpleName(), uv.qtype, t, ib);
        }
    }

    /**
     * This class models an incorporation engine. The engine is responsible for listening to
     * changes in inference variables and register incorporation actions accordingly.
     */
    abstract class AbstractIncorporationEngine implements UndetVarListener {

        @Override
        public void varInstantiated(UndetVar uv) {
            uv.incorporationActions.addFirst(new SubstBounds(uv));
        }

        @Override
        public void varBoundChanged(UndetVar uv, InferenceBound ib, Type bound, boolean update) {
            if (uv.isCaptured()) return;
            uv.incorporationActions.addAll(getIncorporationActions(uv, ib, bound, update));
        }

        abstract List<IncorporationAction> getIncorporationActions(UndetVar uv, InferenceBound ib, Type t, boolean update);
    }

    /**
     * A legacy incorporation engine. Used for source <= 7.
     */
    AbstractIncorporationEngine legacyEngine = new AbstractIncorporationEngine() {

        List<IncorporationAction> getIncorporationActions(UndetVar uv, InferenceBound ib, Type t, boolean update) {
            ListBuffer<IncorporationAction> actions = new ListBuffer<>();
            Type inst = uv.getInst();
            if (inst != null) {
                actions.add(new CheckInst(uv, ib));
            }
            actions.add(new EqCheckLegacy(uv, t, ib));
            return actions.toList();
        }
    };

    /**
     * The standard incorporation engine. Used for source >= 8.
     */
    AbstractIncorporationEngine graphEngine = new AbstractIncorporationEngine() {

        @Override
        List<IncorporationAction> getIncorporationActions(UndetVar uv, InferenceBound ib, Type t, boolean update) {
            ListBuffer<IncorporationAction> actions = new ListBuffer<>();
            Type inst = uv.getInst();
            if (inst != null) {
                actions.add(new CheckInst(uv, ib));
            }
            actions.add(new CheckBounds(uv, t, ib));

            if (update) {
                return actions.toList();
            }

            if (ib == InferenceBound.UPPER) {
                actions.add(new CheckUpperBounds(uv, t));
            }

            actions.add(new PropagateBounds(uv, t, ib));

            return actions.toList();
        }
    };

    /**
     * Get the incorporation engine to be used in this compilation.
     */
    AbstractIncorporationEngine incorporationEngine() {
        return allowGraphInference ? graphEngine : legacyEngine;
    }

    /** max number of incorporation rounds. */
    static final int MAX_INCORPORATION_STEPS = 10000;

    /**
     * Check bounds and perform incorporation.
     */
    void doIncorporation(InferenceContext inferenceContext, Warner warn) throws InferenceException {
        try {
            boolean progress = true;
            int round = 0;
            while (progress && round < MAX_INCORPORATION_STEPS) {
                progress = false;
                for (Type t : inferenceContext.undetvars) {
                    UndetVar uv = (UndetVar)t;
                    if (!uv.incorporationActions.isEmpty()) {
                        progress = true;
                        uv.incorporationActions.removeFirst().apply(inferenceContext, warn);
                    }
                }
                round++;
            }
        } finally {
            incorporationCache.clear();
        }
    }

    /* If for two types t and s there is a least upper bound that contains
     * parameterized types G1, G2 ... Gn, then there exists supertypes of 't' of the form
     * G1<T1, ..., Tn>, G2<T1, ..., Tn>, ... Gn<T1, ..., Tn> and supertypes of 's' of the form
     * G1<S1, ..., Sn>, G2<S1, ..., Sn>, ... Gn<S1, ..., Sn> which will be returned by this method.
     * If no such common supertypes exists then an empty list is returned.
     *
     * As an example for the following input:
     *
     * t = java.util.ArrayList<java.lang.String>
     * s = java.util.List<T>
     *
     * we get this ouput (singleton list):
     *
     * [Pair[java.util.List<java.lang.String>,java.util.List<T>]]
     */
    private List<Pair<Type, Type>> getParameterizedSupers(Type t, Type s) {
        Type lubResult = types.lub(t, s);
        if (lubResult == syms.errType || lubResult == syms.botType) {
            return List.nil();
        }
        List<Type> supertypesToCheck = lubResult.isIntersection() ?
                ((IntersectionClassType)lubResult).getComponents() :
                List.of(lubResult);
        ListBuffer<Pair<Type, Type>> commonSupertypes = new ListBuffer<>();
        for (Type sup : supertypesToCheck) {
            if (sup.isParameterized()) {
                Type asSuperOfT = asSuper(t, sup);
                Type asSuperOfS = asSuper(s, sup);
                commonSupertypes.add(new Pair<>(asSuperOfT, asSuperOfS));
            }
        }
        return commonSupertypes.toList();
    }
    //where
        private Type asSuper(Type t, Type sup) {
            return (sup.hasTag(ARRAY)) ?
                    new ArrayType(asSuper(types.elemtype(t), types.elemtype(sup)), syms.arrayClass) :
                    types.asSuper(t, sup.tsym);
        }

    boolean doIncorporationOp(IncorporationBinaryOpKind opKind, Type op1, Type op2, Warner warn) {
            IncorporationBinaryOp newOp = new IncorporationBinaryOp(opKind, op1, op2);
            Boolean res = incorporationCache.get(newOp);
            if (res == null) {
                incorporationCache.put(newOp, res = newOp.apply(warn));
            }
            return res;
        }

    /**
     * Three kinds of basic operation are supported as part of an incorporation step:
     * (i) subtype check, (ii) same type check and (iii) bound addition (either
     * upper/lower/eq bound).
     */
    enum IncorporationBinaryOpKind {
        IS_SUBTYPE() {
            @Override
            boolean apply(Type op1, Type op2, Warner warn, Types types) {
                return types.isSubtypeUnchecked(op1, op2, warn);
            }
        },
        IS_SAME_TYPE() {
            @Override
            boolean apply(Type op1, Type op2, Warner warn, Types types) {
                return types.isSameType(op1, op2);
            }
        };

        abstract boolean apply(Type op1, Type op2, Warner warn, Types types);
    }

    /**
     * This class encapsulates a basic incorporation operation; incorporation
     * operations takes two type operands and a kind. Each operation performed
     * during an incorporation round is stored in a cache, so that operations
     * are not executed unnecessarily (which would potentially lead to adding
     * same bounds over and over).
     */
    class IncorporationBinaryOp {

        IncorporationBinaryOpKind opKind;
        Type op1;
        Type op2;

        IncorporationBinaryOp(IncorporationBinaryOpKind opKind, Type op1, Type op2) {
            this.opKind = opKind;
            this.op1 = op1;
            this.op2 = op2;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof IncorporationBinaryOp incorporationBinaryOp)
                    && opKind == incorporationBinaryOp.opKind
                    && types.isSameType(op1, incorporationBinaryOp.op1)
                    && types.isSameType(op2, incorporationBinaryOp.op2);
        }

        @Override
        public int hashCode() {
            int result = opKind.hashCode();
            result *= 127;
            result += types.hashCode(op1);
            result *= 127;
            result += types.hashCode(op2);
            return result;
        }

        boolean apply(Warner warn) {
            return opKind.apply(op1, op2, warn, types);
        }
    }

    /** an incorporation cache keeps track of all executed incorporation-related operations */
    Map<IncorporationBinaryOp, Boolean> incorporationCache = new HashMap<>();

    protected static class BoundFilter implements Predicate<Type> {

        InferenceContext inferenceContext;

        public BoundFilter(InferenceContext inferenceContext) {
            this.inferenceContext = inferenceContext;
        }

        @Override
        public boolean test(Type t) {
            return !t.isErroneous() && !inferenceContext.free(t) &&
                    !t.hasTag(BOT);
        }
    }

    /**
     * Incorporation error: mismatch between inferred type and given bound.
     */
    void reportInstError(UndetVar uv, InferenceBound ib) {
        switch (ib) {
            case EQ:
                throw error(diags.fragment(Fragments.InferredDoNotConformToEqBounds(uv.getInst(), uv.getBounds(ib))));
            case LOWER:
                throw error(diags.fragment(Fragments.InferredDoNotConformToLowerBounds(uv.getInst(), uv.getBounds(ib))));
            case UPPER:
                throw error(diags.fragment(Fragments.InferredDoNotConformToUpperBounds(uv.getInst(), uv.getBounds(ib))));
        }
    }

    /**
     * Incorporation error: mismatch between two (or more) bounds of same kind.
     */
    void reportBoundError(UndetVar uv, InferenceBound ib) {
        switch (ib) {
            case EQ:
                throw error(diags.fragment(Fragments.IncompatibleEqBounds(uv.qtype, uv.getBounds(ib))));
            case UPPER:
                throw error(diags.fragment(Fragments.IncompatibleUpperBounds(uv.qtype, uv.getBounds(ib))));
            case LOWER:
                throw new AssertionError("this case shouldn't happen");
        }
    }

    /**
     * Incorporation error: mismatch between two (or more) bounds of different kinds.
     */
    void reportBoundError(UndetVar uv, InferenceBound ib1, InferenceBound ib2) {
        throw error(diags.fragment(Fragments.IncompatibleBounds(
                uv.qtype,
                getBoundFragment(ib1, uv.getBounds(ib1)),
                getBoundFragment(ib2, uv.getBounds(ib2)))));
    }

    Fragment getBoundFragment(InferenceBound ib, List<Type> types) {
        switch (ib) {
            case EQ: return Fragments.EqBounds(types);
            case LOWER: return Fragments.LowerBounds(types);
            case UPPER: return Fragments.UpperBounds(types);
        }
        throw new AssertionError("can't get to this place");
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Inference engine">
    /**
     * Graph inference strategy - act as an input to the inference solver; a strategy is
     * composed of two ingredients: (i) find a node to solve in the inference graph,
     * and (ii) tell th engine when we are done fixing inference variables
     */
    interface GraphStrategy {

        /**
         * A NodeNotFoundException is thrown whenever an inference strategy fails
         * to pick the next node to solve in the inference graph.
         */
        public static class NodeNotFoundException extends RuntimeException {
            private static final long serialVersionUID = 0;

            transient InferenceGraph graph;

            public NodeNotFoundException(InferenceGraph graph) {
                this.graph = graph;
            }
        }
        /**
         * Pick the next node (leaf) to solve in the graph
         */
        Node pickNode(InferenceGraph g) throws NodeNotFoundException;
        /**
         * Is this the last step?
         */
        boolean done();
    }

    /**
     * Simple solver strategy class that locates all leaves inside a graph
     * and picks the first leaf as the next node to solve
     */
    abstract class LeafSolver implements GraphStrategy {
        public Node pickNode(InferenceGraph g) {
            if (g.nodes.isEmpty()) {
                //should not happen
                throw new NodeNotFoundException(g);
            }
            return g.nodes.get(0);
        }
    }

    /**
     * This solver uses an heuristic to pick the best leaf - the heuristic
     * tries to select the node that has maximal probability to contain one
     * or more inference variables in a given list
     */
    abstract class BestLeafSolver extends LeafSolver {

        /** list of ivars of which at least one must be solved */
        List<Type> varsToSolve;

        BestLeafSolver(List<Type> varsToSolve) {
            this.varsToSolve = varsToSolve;
        }

        /**
         * Computes a path that goes from a given node to the leafs in the graph.
         * Typically this will start from a node containing a variable in
         * {@code varsToSolve}. For any given path, the cost is computed as the total
         * number of type-variables that should be eagerly instantiated across that path.
         */
        Pair<List<Node>, Integer> computeTreeToLeafs(Node n) {
            Pair<List<Node>, Integer> cachedPath = treeCache.get(n);
            if (cachedPath == null) {
                //cache miss
                if (n.isLeaf()) {
                    //if leaf, stop
                    cachedPath = new Pair<>(List.of(n), n.data.length());
                } else {
                    //if non-leaf, proceed recursively
                    Pair<List<Node>, Integer> path = new Pair<>(List.of(n), n.data.length());
                    for (Node n2 : n.getAllDependencies()) {
                        if (n2 == n) continue;
                        Pair<List<Node>, Integer> subpath = computeTreeToLeafs(n2);
                        path = new Pair<>(path.fst.prependList(subpath.fst),
                                          path.snd + subpath.snd);
                    }
                    cachedPath = path;
                }
                //save results in cache
                treeCache.put(n, cachedPath);
            }
            return cachedPath;
        }

        /** cache used to avoid redundant computation of tree costs */
        final Map<Node, Pair<List<Node>, Integer>> treeCache = new HashMap<>();

        /** constant value used to mark non-existent paths */
        final Pair<List<Node>, Integer> noPath = new Pair<>(null, Integer.MAX_VALUE);

        /**
         * Pick the leaf that minimize cost
         */
        @Override
        public Node pickNode(final InferenceGraph g) {
            treeCache.clear(); //graph changes at every step - cache must be cleared
            Pair<List<Node>, Integer> bestPath = noPath;
            for (Node n : g.nodes) {
                if (!Collections.disjoint(n.data, varsToSolve)) {
                    Pair<List<Node>, Integer> path = computeTreeToLeafs(n);
                    //discard all paths containing at least a node in the
                    //closure computed above
                    if (path.snd < bestPath.snd) {
                        bestPath = path;
                    }
                }
            }
            if (bestPath == noPath) {
                //no path leads there
                throw new NodeNotFoundException(g);
            }
            return bestPath.fst.head;
        }
    }

    /**
     * The inference process can be thought of as a sequence of steps. Each step
     * instantiates an inference variable using a subset of the inference variable
     * bounds, if certain condition are met. Decisions such as the sequence in which
     * steps are applied, or which steps are to be applied are left to the inference engine.
     */
    enum InferenceStep {

        /**
         * Instantiate an inference variables using one of its (ground) equality
         * constraints
         */
        EQ(InferenceBound.EQ) {
            @Override
            Type solve(UndetVar uv, InferenceContext inferenceContext) {
                return filterBounds(uv, inferenceContext).head;
            }
        },
        /**
         * Instantiate an inference variables using its (ground) lower bounds. Such
         * bounds are merged together using lub().
         */
        LOWER(InferenceBound.LOWER) {
            @Override
            Type solve(UndetVar uv, InferenceContext inferenceContext) {
                Infer infer = inferenceContext.infer;
                List<Type> lobounds = filterBounds(uv, inferenceContext);
                //note: lobounds should have at least one element
                Type owntype = lobounds.tail.tail == null  ? lobounds.head : infer.types.lub(lobounds);
                if (owntype.isPrimitive() || owntype.hasTag(ERROR)) {
                    throw infer.error(infer.diags.fragment(Fragments.NoUniqueMinimalInstanceExists(uv.qtype, lobounds)));
                } else {
                    return owntype;
                }
            }
        },
        /**
         * Infer uninstantiated/unbound inference variables occurring in 'throws'
         * clause as RuntimeException
         */
        THROWS(InferenceBound.UPPER) {
            @Override
            public boolean accepts(UndetVar t, InferenceContext inferenceContext) {
                if (!t.isThrows()) {
                    //not a throws undet var
                    return false;
                }
                Types types = inferenceContext.types;
                Symtab syms = inferenceContext.infer.syms;
                return t.getBounds(InferenceBound.UPPER).stream()
                        .filter(b -> !inferenceContext.free(b))
                        .allMatch(u -> types.isSubtype(syms.runtimeExceptionType, u));
            }

            @Override
            Type solve(UndetVar uv, InferenceContext inferenceContext) {
                return inferenceContext.infer.syms.runtimeExceptionType;
            }
        },
        /**
         * Instantiate an inference variables using its (ground) upper bounds. Such
         * bounds are merged together using glb().
         */
        UPPER(InferenceBound.UPPER) {
            @Override
            Type solve(UndetVar uv, InferenceContext inferenceContext) {
                Infer infer = inferenceContext.infer;
                List<Type> hibounds = filterBounds(uv, inferenceContext);
                //note: hibounds should have at least one element
                Type owntype = hibounds.tail.tail == null  ? hibounds.head : infer.types.glb(hibounds);
                if (owntype.isPrimitive() || owntype.hasTag(ERROR)) {
                    throw infer.error(infer.diags.fragment(Fragments.NoUniqueMaximalInstanceExists(uv.qtype, hibounds)));
                } else {
                    return owntype;
                }
            }
        },
        /**
         * Like the former; the only difference is that this step can only be applied
         * if all upper bounds are ground.
         */
        UPPER_LEGACY(InferenceBound.UPPER) {
            @Override
            public boolean accepts(UndetVar t, InferenceContext inferenceContext) {
                return !inferenceContext.free(t.getBounds(ib)) && !t.isCaptured();
            }

            @Override
            Type solve(UndetVar uv, InferenceContext inferenceContext) {
                return UPPER.solve(uv, inferenceContext);
            }
        },
        /**
         * Like the former; the only difference is that this step can only be applied
         * if all upper/lower bounds are ground.
         */
        CAPTURED(InferenceBound.UPPER) {
            @Override
            public boolean accepts(UndetVar t, InferenceContext inferenceContext) {
                return t.isCaptured() &&
                        !inferenceContext.free(t.getBounds(InferenceBound.UPPER, InferenceBound.LOWER));
            }

            @Override
            Type solve(UndetVar uv, InferenceContext inferenceContext) {
                Infer infer = inferenceContext.infer;
                Type upper = UPPER.filterBounds(uv, inferenceContext).nonEmpty() ?
                        UPPER.solve(uv, inferenceContext) :
                        infer.syms.objectType;
                Type lower = LOWER.filterBounds(uv, inferenceContext).nonEmpty() ?
                        LOWER.solve(uv, inferenceContext) :
                        infer.syms.botType;
                CapturedType prevCaptured = (CapturedType)uv.qtype;
                return new CapturedType(prevCaptured.tsym.name, prevCaptured.tsym.owner,
                                        upper, lower, prevCaptured.wildcard);
            }
        };

        final InferenceBound ib;

        InferenceStep(InferenceBound ib) {
            this.ib = ib;
        }

        /**
         * Find an instantiated type for a given inference variable within
         * a given inference context
         */
        abstract Type solve(UndetVar uv, InferenceContext inferenceContext);

        /**
         * Can the inference variable be instantiated using this step?
         */
        public boolean accepts(UndetVar t, InferenceContext inferenceContext) {
            return filterBounds(t, inferenceContext).nonEmpty() && !t.isCaptured();
        }

        /**
         * Return the subset of ground bounds in a given bound set (i.e. eq/lower/upper)
         */
        List<Type> filterBounds(UndetVar uv, InferenceContext inferenceContext) {
            return Type.filter(uv.getBounds(ib), new BoundFilter(inferenceContext));
        }
    }

    /**
     * This enumeration defines the sequence of steps to be applied when the
     * solver works in legacy mode. The steps in this enumeration reflect
     * the behavior of old inference routine (see JLS SE 7 15.12.2.7/15.12.2.8).
     */
    enum LegacyInferenceSteps {

        EQ_LOWER(EnumSet.of(InferenceStep.EQ, InferenceStep.LOWER)),
        EQ_UPPER(EnumSet.of(InferenceStep.EQ, InferenceStep.UPPER_LEGACY));

        final EnumSet<InferenceStep> steps;

        LegacyInferenceSteps(EnumSet<InferenceStep> steps) {
            this.steps = steps;
        }
    }

    /**
     * This enumeration defines the sequence of steps to be applied when the
     * graph solver is used. This order is defined so as to maximize compatibility
     * w.r.t. old inference routine (see JLS SE 7 15.12.2.7/15.12.2.8).
     */
    enum GraphInferenceSteps {

        EQ(EnumSet.of(InferenceStep.EQ)),
        EQ_LOWER(EnumSet.of(InferenceStep.EQ, InferenceStep.LOWER)),
        EQ_LOWER_THROWS_UPPER_CAPTURED(EnumSet.of(InferenceStep.EQ, InferenceStep.LOWER, InferenceStep.UPPER, InferenceStep.THROWS, InferenceStep.CAPTURED));

        final EnumSet<InferenceStep> steps;

        GraphInferenceSteps(EnumSet<InferenceStep> steps) {
            this.steps = steps;
        }
    }

    /**
     * There are two kinds of dependencies between inference variables. The basic
     * kind of dependency (or bound dependency) arises when a variable mention
     * another variable in one of its bounds. There's also a more subtle kind
     * of dependency that arises when a variable 'might' lead to better constraints
     * on another variable (this is typically the case with variables holding up
     * stuck expressions).
     */
    enum DependencyKind implements GraphUtils.DependencyKind {

        /** bound dependency */
        BOUND("dotted"),
        /** stuck dependency */
        STUCK("dashed");

        final String dotStyle;

        private DependencyKind(String dotStyle) {
            this.dotStyle = dotStyle;
        }
    }

    /**
     * This is the graph inference solver - the solver organizes all inference variables in
     * a given inference context by bound dependencies - in the general case, such dependencies
     * would lead to a cyclic directed graph (hence the name); the dependency info is used to build
     * an acyclic graph, where all cyclic variables are bundled together. An inference
     * step corresponds to solving a node in the acyclic graph - this is done by
     * relying on a given strategy (see GraphStrategy).
     */
    class GraphSolver {

        InferenceContext inferenceContext;
        Warner warn;

        GraphSolver(InferenceContext inferenceContext, Warner warn) {
            this.inferenceContext = inferenceContext;
            this.warn = warn;
        }

        /**
         * Solve variables in a given inference context. The amount of variables
         * to be solved, and the way in which the underlying acyclic graph is explored
         * depends on the selected solver strategy.
         */
        void solve(GraphStrategy sstrategy) {
            doIncorporation(inferenceContext, warn); //initial propagation of bounds
            InferenceGraph inferenceGraph = new InferenceGraph();
            while (!sstrategy.done()) {
                if (dependenciesFolder != null) {
                    //add this graph to the pending queue
                    pendingGraphs = pendingGraphs.prepend(inferenceGraph.toDot());
                }
                InferenceGraph.Node nodeToSolve = sstrategy.pickNode(inferenceGraph);
                List<Type> varsToSolve = List.from(nodeToSolve.data);
                List<Type> saved_undet = inferenceContext.save();
                try {
                    //repeat until all variables are solved
                    outer: while (Type.containsAny(inferenceContext.restvars(), varsToSolve)) {
                        //for each inference phase
                        for (GraphInferenceSteps step : GraphInferenceSteps.values()) {
                            if (inferenceContext.solveBasic(varsToSolve, step.steps).nonEmpty()) {
                                doIncorporation(inferenceContext, warn);
                                continue outer;
                            }
                        }
                        //no progress
                        throw error(null);
                    }
                }
                catch (InferenceException ex) {
                    //did we fail because of interdependent ivars?
                    inferenceContext.rollback(saved_undet);
                    instantiateAsUninferredVars(varsToSolve, inferenceContext);
                    doIncorporation(inferenceContext, warn);
                }
                inferenceGraph.deleteNode(nodeToSolve);
            }
        }

        /**
         * The dependencies between the inference variables that need to be solved
         * form a (possibly cyclic) graph. This class reduces the original dependency graph
         * to an acyclic version, where cyclic nodes are folded into a single 'super node'.
         */
        class InferenceGraph {

            /**
             * This class represents a node in the graph. Each node corresponds
             * to an inference variable and has edges (dependencies) on other
             * nodes. The node defines an entry point that can be used to receive
             * updates on the structure of the graph this node belongs to (used to
             * keep dependencies in sync).
             */
            class Node extends GraphUtils.TarjanNode<ListBuffer<Type>, Node> implements DottableNode<ListBuffer<Type>, Node> {

                /** node dependencies */
                Set<Node> deps;

                Node(Type ivar) {
                    super(ListBuffer.of(ivar));
                    this.deps = new LinkedHashSet<>();
                }

                @Override
                public GraphUtils.DependencyKind[] getSupportedDependencyKinds() {
                    return new GraphUtils.DependencyKind[] { DependencyKind.BOUND };
                }

                public Iterable<? extends Node> getAllDependencies() {
                    return deps;
                }

                @Override
                public Collection<? extends Node> getDependenciesByKind(GraphUtils.DependencyKind dk) {
                    if (dk == DependencyKind.BOUND) {
                        return deps;
                    } else {
                        throw new IllegalStateException();
                    }
                }

                /**
                 * Adds dependency with given kind.
                 */
                protected void addDependency(Node depToAdd) {
                    deps.add(depToAdd);
                }

                /**
                 * Add multiple dependencies of same given kind.
                 */
                protected void addDependencies(Set<Node> depsToAdd) {
                    for (Node n : depsToAdd) {
                        addDependency(n);
                    }
                }

                /**
                 * Remove a dependency, regardless of its kind.
                 */
                protected boolean removeDependency(Node n) {
                    return deps.remove(n);
                }

                /**
                 * Compute closure of a give node, by recursively walking
                 * through all its dependencies.
                 */
                protected Set<Node> closure() {
                    Set<Node> closure = new HashSet<>();
                    closureInternal(closure);
                    return closure;
                }

                private void closureInternal(Set<Node> closure) {
                    if (closure.add(this)) {
                        for (Node n : deps) {
                            n.closureInternal(closure);
                        }
                    }
                }

                /**
                 * Is this node a leaf? This means either the node has no dependencies,
                 * or it just has self-dependencies.
                 */
                protected boolean isLeaf() {
                    //no deps, or only one self dep
                    if (deps.isEmpty()) return true;
                    for (Node n : deps) {
                        if (n != this) {
                            return false;
                        }
                    }
                    return true;
                }

                /**
                 * Merge this node with another node, acquiring its dependencies.
                 * This routine is used to merge all cyclic node together and
                 * form an acyclic graph.
                 */
                protected void mergeWith(List<? extends Node> nodes) {
                    for (Node n : nodes) {
                        Assert.check(n.data.length() == 1, "Attempt to merge a compound node!");
                        data.appendList(n.data);
                        addDependencies(n.deps);
                    }
                    //update deps
                    Set<Node> deps2 = new LinkedHashSet<>();
                    for (Node d : deps) {
                        if (data.contains(d.data.first())) {
                            deps2.add(this);
                        } else {
                            deps2.add(d);
                        }
                    }
                    deps = deps2;
                }

                /**
                 * Notify all nodes that something has changed in the graph
                 * topology.
                 */
                private void graphChanged(Node from, Node to) {
                    if (removeDependency(from)) {
                        if (to != null) {
                            addDependency(to);
                        }
                    }
                }

                @Override
                public Properties nodeAttributes() {
                    Properties p = new Properties();
                    p.put("label", "\"" + toString() + "\"");
                    return p;
                }

                @Override
                public Properties dependencyAttributes(Node sink, GraphUtils.DependencyKind dk) {
                    Properties p = new Properties();
                    p.put("style", ((DependencyKind)dk).dotStyle);
                    StringBuilder buf = new StringBuilder();
                    String sep = "";
                    for (Type from : data) {
                        UndetVar uv = (UndetVar)inferenceContext.asUndetVar(from);
                        for (Type bound : uv.getBounds(InferenceBound.values())) {
                            if (bound.containsAny(List.from(sink.data))) {
                                buf.append(sep);
                                buf.append(bound);
                                sep = ",";
                            }
                        }
                    }
                    p.put("label", "\"" + buf.toString() + "\"");
                    return p;
                }
            }

            /** the nodes in the inference graph */
            ArrayList<Node> nodes;

            InferenceGraph() {
                initNodes();
            }

            /**
             * Basic lookup helper for retrieving a graph node given an inference
             * variable type.
             */
            public Node findNode(Type t) {
                for (Node n : nodes) {
                    if (n.data.contains(t)) {
                        return n;
                    }
                }
                return null;
            }

            /**
             * Delete a node from the graph. This update the underlying structure
             * of the graph (including dependencies) via listeners updates.
             */
            public void deleteNode(Node n) {
                Assert.check(nodes.contains(n));
                nodes.remove(n);
                notifyUpdate(n, null);
            }

            /**
             * Notify all nodes of a change in the graph. If the target node is
             * {@code null} the source node is assumed to be removed.
             */
            void notifyUpdate(Node from, Node to) {
                for (Node n : nodes) {
                    n.graphChanged(from, to);
                }
            }

            /**
             * Create the graph nodes. First a simple node is created for every inference
             * variables to be solved. Then Tarjan is used to found all connected components
             * in the graph. For each component containing more than one node, a super node is
             * created, effectively replacing the original cyclic nodes.
             */
            void initNodes() {
                //add nodes
                nodes = new ArrayList<>();
                for (Type t : inferenceContext.restvars()) {
                    nodes.add(new Node(t));
                }
                //add dependencies
                for (Node n_i : nodes) {
                    Type i = n_i.data.first();
                    for (Node n_j : nodes) {
                        Type j = n_j.data.first();
                        // don't compare a variable to itself
                        if (i != j) {
                            UndetVar uv_i = (UndetVar)inferenceContext.asUndetVar(i);
                            if (Type.containsAny(uv_i.getBounds(InferenceBound.values()), List.of(j))) {
                                //update i's bound dependencies
                                n_i.addDependency(n_j);
                            }
                        }
                    }
                }
                //merge cyclic nodes
                ArrayList<Node> acyclicNodes = new ArrayList<>();
                for (List<? extends Node> conSubGraph : GraphUtils.tarjan(nodes)) {
                    if (conSubGraph.length() > 1) {
                        Node root = conSubGraph.head;
                        root.mergeWith(conSubGraph.tail);
                        for (Node n : conSubGraph) {
                            notifyUpdate(n, root);
                        }
                    }
                    acyclicNodes.add(conSubGraph.head);
                }
                nodes = acyclicNodes;
            }

            /**
             * Debugging: dot representation of this graph
             */
            String toDot() {
                StringBuilder buf = new StringBuilder();
                for (Type t : inferenceContext.undetvars) {
                    UndetVar uv = (UndetVar)t;
                    buf.append(String.format("var %s - upper bounds = %s, lower bounds = %s, eq bounds = %s\\n",
                            uv.qtype, uv.getBounds(InferenceBound.UPPER), uv.getBounds(InferenceBound.LOWER),
                            uv.getBounds(InferenceBound.EQ)));
                }
                return GraphUtils.toDot(nodes, "inferenceGraph" + hashCode(), buf.toString());
            }
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Inference context">
    /**
     * Functional interface for defining inference callbacks. Certain actions
     * (i.e. subtyping checks) might need to be redone after all inference variables
     * have been fixed.
     */
    interface FreeTypeListener {
        void typesInferred(InferenceContext inferenceContext);
    }

    final InferenceContext emptyContext;
    // </editor-fold>
}
