/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Type.TypeVar;
import com.sun.tools.javac.code.Type.UndetVar;
import com.sun.tools.javac.code.Type.UndetVar.InferenceBound;
import com.sun.tools.javac.code.Type.WildcardType;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.comp.Infer.FreeTypeListener;
import com.sun.tools.javac.comp.Infer.GraphSolver;
import com.sun.tools.javac.comp.Infer.GraphStrategy;
import com.sun.tools.javac.comp.Infer.InferenceException;
import com.sun.tools.javac.comp.Infer.InferenceStep;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Warner;

/**
 * An inference context keeps track of the set of variables that are free
 * in the current context. It provides utility methods for opening/closing
 * types to their corresponding free/closed forms. It also provide hooks for
 * attaching deferred post-inference action (see PendingCheck). Finally,
 * it can be used as an entry point for performing upper/lower bound inference
 * (see InferenceKind).
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b>
 */
public class InferenceContext {

    /** list of inference vars as undet vars */
    List<Type> undetvars;

    Type update(Type t) {
        return t;
    }

    /** list of inference vars in this context */
    List<Type> inferencevars;

    Map<FreeTypeListener, List<Type>> freeTypeListeners = new LinkedHashMap<>();

    Types types;
    Infer infer;

    public InferenceContext(Infer infer, List<Type> inferencevars) {
        this(infer, inferencevars, inferencevars.map(infer.fromTypeVarFun));
    }

    public InferenceContext(Infer infer, List<Type> inferencevars, List<Type> undetvars) {
        this.inferencevars = inferencevars;
        this.undetvars = undetvars;
        this.infer = infer;
        this.types = infer.types;
    }

    /**
     * add a new inference var to this inference context
     */
    void addVar(TypeVar t) {
        this.undetvars = this.undetvars.prepend(infer.fromTypeVarFun.apply(t));
        this.inferencevars = this.inferencevars.prepend(t);
    }

    /**
     * returns the list of free variables (as type-variables) in this
     * inference context
     */
    List<Type> inferenceVars() {
        return inferencevars;
    }

    /**
     * returns the list of undetermined variables in this inference context
     */
    public List<Type> undetVars() {
        return undetvars;
    }

    /**
     * returns the list of uninstantiated variables (as type-variables) in this
     * inference context
     */
    List<Type> restvars() {
        return filterVars(uv -> uv.getInst() == null);
    }

    /**
     * returns the list of instantiated variables (as type-variables) in this
     * inference context
     */
    List<Type> instvars() {
        return filterVars(uv -> uv.getInst() != null);
    }

    /**
     * Get list of bounded inference variables (where bound is other than
     * declared bounds).
     */
    final List<Type> boundedVars() {
        return filterVars(uv -> uv.getBounds(InferenceBound.UPPER)
                 .diff(uv.getDeclaredBounds())
                 .appendList(uv.getBounds(InferenceBound.EQ, InferenceBound.LOWER)).nonEmpty());
    }

    /* Returns the corresponding inference variables.
     */
    private List<Type> filterVars(Predicate<UndetVar> fu) {
        ListBuffer<Type> res = new ListBuffer<>();
        for (Type t : undetvars) {
            UndetVar uv = (UndetVar)t;
            if (fu.test(uv)) {
                res.append(uv.qtype);
            }
        }
        return res.toList();
    }

    /**
     * is this type free?
     */
    final boolean free(Type t) {
        return t.containsAny(inferencevars);
    }

    final boolean free(List<Type> ts) {
        for (Type t : ts) {
            if (free(t)) return true;
        }
        return false;
    }

    /**
     * Returns a list of free variables in a given type
     */
    final List<Type> freeVarsIn(Type t) {
        ListBuffer<Type> buf = new ListBuffer<>();
        for (Type iv : inferenceVars()) {
            if (t.contains(iv)) {
                buf.add(iv);
            }
        }
        return buf.toList();
    }

    final List<Type> freeVarsIn(List<Type> ts) {
        ListBuffer<Type> buf = new ListBuffer<>();
        for (Type t : ts) {
            buf.appendList(freeVarsIn(t));
        }
        ListBuffer<Type> buf2 = new ListBuffer<>();
        for (Type t : buf) {
            if (!buf2.contains(t)) {
                buf2.add(t);
            }
        }
        return buf2.toList();
    }

    /**
     * Replace all free variables in a given type with corresponding
     * undet vars (used ahead of subtyping/compatibility checks to allow propagation
     * of inference constraints).
     */
    public final Type asUndetVar(Type t) {
        return types.subst(t, inferencevars, undetvars);
    }

    final List<Type> asUndetVars(List<Type> ts) {
        ListBuffer<Type> buf = new ListBuffer<>();
        for (Type t : ts) {
            buf.append(asUndetVar(t));
        }
        return buf.toList();
    }

    List<Type> instTypes() {
        ListBuffer<Type> buf = new ListBuffer<>();
        for (Type t : undetvars) {
            UndetVar uv = (UndetVar)t;
            buf.append(uv.getInst() != null ? uv.getInst() : uv.qtype);
        }
        return buf.toList();
    }

    /**
     * Replace all free variables in a given type with corresponding
     * instantiated types - if one or more free variable has not been
     * fully instantiated, it will still be available in the resulting type.
     */
    Type asInstType(Type t) {
        return types.subst(t, inferencevars, instTypes());
    }

    List<Type> asInstTypes(List<Type> ts) {
        ListBuffer<Type> buf = new ListBuffer<>();
        for (Type t : ts) {
            buf.append(asInstType(t));
        }
        return buf.toList();
    }

    /**
     * Add custom hook for performing post-inference action
     */
    void addFreeTypeListener(List<Type> types, FreeTypeListener ftl) {
        freeTypeListeners.put(ftl, freeVarsIn(types));
    }

    /**
     * Mark the inference context as complete and trigger evaluation
     * of all deferred checks.
     */
    void notifyChange() {
        notifyChange(inferencevars.diff(restvars()));
    }

    void notifyChange(List<Type> inferredVars) {
        InferenceException thrownEx = null;
        for (Map.Entry<FreeTypeListener, List<Type>> entry :
                new LinkedHashMap<>(freeTypeListeners).entrySet()) {
            if (!Type.containsAny(entry.getValue(), inferencevars.diff(inferredVars))) {
                try {
                    entry.getKey().typesInferred(this);
                    freeTypeListeners.remove(entry.getKey());
                } catch (InferenceException ex) {
                    if (thrownEx == null) {
                        thrownEx = ex;
                    }
                }
            }
        }
        //inference exception multiplexing - present any inference exception
        //thrown when processing listeners as a single one
        if (thrownEx != null) {
            throw thrownEx;
        }
    }

    /**
     * Save the state of this inference context
     */
    public List<Type> save() {
        ListBuffer<Type> buf = new ListBuffer<>();
        for (Type t : undetvars) {
            buf.add(((UndetVar)t).dup(infer.types));
        }
        return buf.toList();
    }

    /** Restore the state of this inference context to the previous known checkpoint.
    *  Consider that the number of saved undetermined variables can be different to the current
    *  amount. This is because new captured variables could have been added.
    */
    public void rollback(List<Type> saved_undet) {
        Assert.check(saved_undet != null);
        //restore bounds (note: we need to preserve the old instances)
        ListBuffer<Type> newUndetVars = new ListBuffer<>();
        ListBuffer<Type> newInferenceVars = new ListBuffer<>();
        while (saved_undet.nonEmpty() && undetvars.nonEmpty()) {
            UndetVar uv = (UndetVar)undetvars.head;
            UndetVar uv_saved = (UndetVar)saved_undet.head;
            if (uv.qtype == uv_saved.qtype) {
                uv_saved.dupTo(uv, types);
                undetvars = undetvars.tail;
                saved_undet = saved_undet.tail;
                newUndetVars.add(uv);
                newInferenceVars.add(uv.qtype);
            } else {
                undetvars = undetvars.tail;
            }
        }
        undetvars = newUndetVars.toList();
        inferencevars = newInferenceVars.toList();
    }

    /**
     * Copy variable in this inference context to the given context
     */
    void dupTo(final InferenceContext that) {
        dupTo(that, false);
    }

    void dupTo(final InferenceContext that, boolean clone) {
        that.inferencevars = that.inferencevars.appendList(inferencevars.diff(that.inferencevars));
        List<Type> undetsToPropagate = clone ? save() : undetvars;
        that.undetvars = that.undetvars.appendList(undetsToPropagate.diff(that.undetvars)); //propagate cloned undet!!
        //set up listeners to notify original inference contexts as
        //propagated vars are inferred in new context
        for (Type t : inferencevars) {
            that.freeTypeListeners.put(inferenceContext -> InferenceContext.this.notifyChange(), List.of(t));
        }
    }

    InferenceContext min(List<Type> roots, boolean shouldSolve, Warner warn) {
        if (roots.length() == inferencevars.length()) {
            return this;
        }
        ReachabilityVisitor rv = new ReachabilityVisitor();
        rv.scan(roots);
        if (rv.min.size() == inferencevars.length()) {
            return this;
        }

        List<Type> minVars = List.from(rv.min);
        List<Type> redundantVars = inferencevars.diff(minVars);

        //compute new undet variables (bounds associated to redundant variables are dropped)
        ListBuffer<Type> minUndetVars = new ListBuffer<>();
        for (Type minVar : minVars) {
            UndetVar uv = (UndetVar)asUndetVar(minVar);
            Assert.check(uv.incorporationActions.isEmpty());
            UndetVar uv2 = uv.dup(types);
            for (InferenceBound ib : InferenceBound.values()) {
                List<Type> newBounds = uv.getBounds(ib).stream()
                        .filter(b -> !redundantVars.contains(b))
                        .collect(List.collector());
                uv2.setBounds(ib, newBounds);
            }
            minUndetVars.add(uv2);
        }

        //compute new minimal inference context
        InferenceContext minContext = new InferenceContext(infer, minVars, minUndetVars.toList());
        for (Type t : minContext.inferencevars) {
            //add listener that forwards notifications to original context
            minContext.addFreeTypeListener(List.of(t), (inferenceContext) -> {
                Type instType = inferenceContext.asInstType(t);
                for (Type eq : rv.minMap.get(t)) {
                    ((UndetVar)asUndetVar(eq)).setInst(instType);
                }
                infer.doIncorporation(this, warn);
                notifyChange();
            });
        }
        if (shouldSolve) {
            //solve definitively unreachable variables
            List<Type> unreachableVars = redundantVars.diff(List.from(rv.equiv));
            minContext.addFreeTypeListener(minVars, (inferenceContext) -> {
                solve(unreachableVars, warn);
                notifyChange();
            });
        }
        return minContext;
    }

    class ReachabilityVisitor extends Types.UnaryVisitor<Void> {

        Set<Type> equiv = new LinkedHashSet<>();
        Set<Type> min = new LinkedHashSet<>();
        Map<Type, Set<Type>> minMap = new LinkedHashMap<>();

        void scan(List<Type> roots) {
            roots.stream().forEach(this::visit);
        }

        @Override
        public Void visitType(Type t, Void _unused) {
            return null;
        }

        @Override
        public Void visitUndetVar(UndetVar t, Void _unused) {
            if (min.add(t.qtype)) {
                Set<Type> deps = minMap.getOrDefault(t.qtype, new LinkedHashSet<>(Collections.singleton(t.qtype)));
                for (InferenceBound boundKind : InferenceBound.values()) {
                    for (Type b : t.getBounds(boundKind)) {
                        Type undet = asUndetVar(b);
                        if (!undet.hasTag(TypeTag.UNDETVAR)) {
                            visit(undet);
                        } else if (isEquiv(t, b, boundKind)) {
                            deps.add(b);
                            equiv.add(b);
                        } else {
                            visit(undet);
                        }
                    }
                }
                minMap.put(t.qtype, deps);
            }
            return null;
        }

        @Override
        public Void visitWildcardType(WildcardType t, Void _unused) {
            return visit(t.type);
        }

        @Override
        public Void visitTypeVar(TypeVar t, Void aVoid) {
            Type undet = asUndetVar(t);
            if (undet.hasTag(TypeTag.UNDETVAR)) {
                visitUndetVar((UndetVar)undet, null);
            }
            return null;
        }

        @Override
        public Void visitArrayType(ArrayType t, Void _unused) {
            return visit(t.elemtype);
        }

        @Override
        public Void visitClassType(ClassType t, Void _unused) {
            visit(t.getEnclosingType());
            for (Type targ : t.getTypeArguments()) {
                visit(targ);
            }
            return null;
        }

        boolean isEquiv(UndetVar from, Type t, InferenceBound boundKind) {
            UndetVar uv = (UndetVar)asUndetVar(t);
            for (InferenceBound ib : InferenceBound.values()) {
                List<Type> b1 = from.getBounds(ib);
                if (ib == boundKind) {
                    b1 = b1.diff(List.of(t));
                }
                List<Type> b2 = uv.getBounds(ib);
                if (ib == boundKind.complement()) {
                    b2 = b2.diff(List.of(from.qtype));
                }
                if (!b1.containsAll(b2) || !b2.containsAll(b1)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Solve with given graph strategy.
     */
    private void solve(GraphStrategy ss, Warner warn) {
        GraphSolver s = infer.new GraphSolver(this, warn);
        s.solve(ss);
    }

    /**
     * Solve all variables in this context.
     */
    public void solve(Warner warn) {
        solve(infer.new LeafSolver() {
            public boolean done() {
                return restvars().isEmpty();
            }
        }, warn);
    }

    /**
     * Solve all variables in the given list.
     */
    public void solve(final List<Type> vars, Warner warn) {
        solve(infer.new BestLeafSolver(vars) {
            public boolean done() {
                return !free(asInstTypes(vars));
            }
        }, warn);
    }

    /**
     * Solve at least one variable in given list.
     */
    public void solveAny(List<Type> varsToSolve, Warner warn) {
        solve(infer.new BestLeafSolver(varsToSolve.intersect(restvars())) {
            public boolean done() {
                return instvars().intersect(varsToSolve).nonEmpty();
            }
        }, warn);
    }

    /**
     * Apply a set of inference steps
     */
    private List<Type> solveBasic(EnumSet<InferenceStep> steps) {
        return solveBasic(inferencevars, steps);
    }

    List<Type> solveBasic(List<Type> varsToSolve, EnumSet<InferenceStep> steps) {
        ListBuffer<Type> solvedVars = new ListBuffer<>();
        for (Type t : varsToSolve.intersect(restvars())) {
            UndetVar uv = (UndetVar)asUndetVar(t);
            for (InferenceStep step : steps) {
                if (step.accepts(uv, this)) {
                    uv.setInst(step.solve(uv, this));
                    solvedVars.add(uv.qtype);
                    break;
                }
            }
        }
        return solvedVars.toList();
    }

    /**
     * Instantiate inference variables in legacy mode (JLS 15.12.2.7, 15.12.2.8).
     * During overload resolution, instantiation is done by doing a partial
     * inference process using eq/lower bound instantiation. During check,
     * we also instantiate any remaining vars by repeatedly using eq/upper
     * instantiation, until all variables are solved.
     */
    public void solveLegacy(boolean partial, Warner warn, EnumSet<InferenceStep> steps) {
        while (true) {
            List<Type> solvedVars = solveBasic(steps);
            if (restvars().isEmpty() || partial) {
                //all variables have been instantiated - exit
                break;
            } else if (solvedVars.isEmpty()) {
                //some variables could not be instantiated because of cycles in
                //upper bounds - provide a (possibly recursive) default instantiation
                infer.instantiateAsUninferredVars(restvars(), this);
                break;
            } else {
                //some variables have been instantiated - replace newly instantiated
                //variables in remaining upper bounds and continue
                for (Type t : undetvars) {
                    UndetVar uv = (UndetVar)t;
                    uv.substBounds(solvedVars, asInstTypes(solvedVars), types);
                }
            }
        }
        infer.doIncorporation(this, warn);
    }

    @Override
    public String toString() {
        return "Inference vars: " + inferencevars + '\n' +
               "Undet vars: " + undetvars;
    }

    /* Method Types.capture() generates a new type every time it's applied
     * to a wildcard parameterized type. This is intended functionality but
     * there are some cases when what you need is not to generate a new
     * captured type but to check that a previously generated captured type
     * is correct. There are cases when caching a captured type for later
     * reuse is sound. In general two captures from the same AST are equal.
     * This is why the tree is used as the key of the map below. This map
     * stores a Type per AST.
     */
    Map<JCTree, Type> captureTypeCache = new HashMap<>();

    Type cachedCapture(JCTree tree, Type t, boolean readOnly) {
        Type captured = captureTypeCache.get(tree);
        if (captured != null) {
            return captured;
        }

        Type result = types.capture(t);
        if (result != t && !readOnly) { // then t is a wildcard parameterized type
            captureTypeCache.put(tree, result);
        }
        return result;
    }
}
