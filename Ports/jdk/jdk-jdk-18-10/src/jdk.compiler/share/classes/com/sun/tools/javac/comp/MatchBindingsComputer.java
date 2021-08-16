/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.BindingSymbol;
import com.sun.tools.javac.resources.CompilerProperties.Errors;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCGuardPattern;
import com.sun.tools.javac.tree.JCTree.Tag;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Log;

import static com.sun.tools.javac.code.Flags.CLASH;


public class MatchBindingsComputer extends TreeScanner {
    public static final MatchBindings EMPTY = new MatchBindings(List.nil(), List.nil());

    protected static final Context.Key<MatchBindingsComputer> matchBindingsComputerKey = new Context.Key<>();

    private final Log log;

    public static MatchBindingsComputer instance(Context context) {
        MatchBindingsComputer instance = context.get(matchBindingsComputerKey);
        if (instance == null)
            instance = new MatchBindingsComputer(context);
        return instance;
    }

    protected MatchBindingsComputer(Context context) {
        this.log = Log.instance(context);
    }

    public MatchBindings conditional(JCTree tree, MatchBindings condBindings, MatchBindings trueBindings, MatchBindings falseBindings) {
        if (condBindings == EMPTY &&
            trueBindings == EMPTY &&
            falseBindings == EMPTY) {
            return EMPTY;
        }

        DiagnosticPosition pos = tree.pos();
         //A pattern variable is introduced both by a when true, and by c when true:
        List<BindingSymbol> xTzT = intersection(pos, condBindings.bindingsWhenTrue, falseBindings.bindingsWhenTrue);
         //A pattern variable is introduced both by a when false, and by b when true:
        List<BindingSymbol> xFyT = intersection(pos, condBindings.bindingsWhenFalse, trueBindings.bindingsWhenTrue);
         //A pattern variable is introduced both by b when true, and by c when true:
        List<BindingSymbol> yTzT = intersection(pos, trueBindings.bindingsWhenTrue, falseBindings.bindingsWhenTrue);
         //A pattern variable is introduced both by a when true, and by c when false:
        List<BindingSymbol> xTzF = intersection(pos, condBindings.bindingsWhenTrue, falseBindings.bindingsWhenFalse);
         //A pattern variable is introduced both by a when false, and by b when false:
        List<BindingSymbol> xFyF = intersection(pos, condBindings.bindingsWhenFalse, trueBindings.bindingsWhenFalse);
         //A pattern variable is introduced both by b when false, and by c when false:
        List<BindingSymbol> yFzF = intersection(pos, trueBindings.bindingsWhenFalse, falseBindings.bindingsWhenFalse);

        //error recovery:
        /* if e = "x ? y : z", then:
               e.T = union(intersect(y.T, z.T), intersect(x.T, z.T), intersect(x.F, y.T))
               e.F = union(intersect(y.F, z.F), intersect(x.T, z.F), intersect(x.F, y.F))
        */
        List<BindingSymbol> bindingsWhenTrue = union(pos, yTzT, xTzT, xFyT);
        List<BindingSymbol> bindingsWhenFalse = union(pos, yFzF, xTzF, xFyF);
        return new MatchBindings(bindingsWhenTrue, bindingsWhenFalse);
    }

    public MatchBindings unary(JCTree tree, MatchBindings bindings) {
        if (bindings == EMPTY || !tree.hasTag(Tag.NOT)) return bindings;
        return new MatchBindings(bindings.bindingsWhenFalse, bindings.bindingsWhenTrue);
    }

    public MatchBindings binary(JCTree tree, MatchBindings lhsBindings, MatchBindings rhsBindings) {
        switch (tree.getTag()) {
            case AND: {
                return andOperation(tree.pos(), lhsBindings, rhsBindings);
            }
            case OR: {
                // e.T = intersection(x.T, y.T) (error recovery)
                // e.F = union(x.F, y.F)
                List<BindingSymbol> bindingsWhenTrue = //error recovery
                        intersection(tree.pos(), lhsBindings.bindingsWhenTrue, rhsBindings.bindingsWhenTrue);
                List<Symbol.BindingSymbol> bindingsWhenFalse =
                        union(tree.pos(), lhsBindings.bindingsWhenFalse, rhsBindings.bindingsWhenFalse);
                return new MatchBindings(bindingsWhenTrue, bindingsWhenFalse);
            }
        }
        return EMPTY;
    }

    public MatchBindings guardedPattern(JCGuardPattern tree, MatchBindings patternBindings, MatchBindings guardBindings) {
        return andOperation(tree.pos(), patternBindings, guardBindings);
    }

    public MatchBindings andOperation(DiagnosticPosition pos, MatchBindings lhsBindings, MatchBindings rhsBindings) {
        // e.T = union(x.T, y.T)
        // e.F = intersection(x.F, y.F) (error recovery)
        List<BindingSymbol> bindingsWhenTrue =
                union(pos, lhsBindings.bindingsWhenTrue, rhsBindings.bindingsWhenTrue);
        List<BindingSymbol> bindingsWhenFalse = //error recovery
                intersection(pos, lhsBindings.bindingsWhenFalse, rhsBindings.bindingsWhenFalse);
        return new MatchBindings(bindingsWhenTrue, bindingsWhenFalse);
    }

    public MatchBindings switchCase(JCTree tree, MatchBindings prevBindings, MatchBindings currentBindings) {
        if (prevBindings == null)
            return currentBindings;
        if (prevBindings.nullPattern) {
            return currentBindings;
        }
        if (currentBindings.nullPattern) {
            return prevBindings;
        }
        return new MatchBindings(intersection(tree.pos(), prevBindings.bindingsWhenTrue, currentBindings.bindingsWhenTrue),
                                 intersection(tree.pos(), prevBindings.bindingsWhenFalse, currentBindings.bindingsWhenFalse));
    }

    public MatchBindings finishBindings(JCTree tree, MatchBindings matchBindings) {
        switch (tree.getTag()) {
            case NOT: case AND: case OR: case BINDINGPATTERN:
            case PARENTHESIZEDPATTERN: case GUARDPATTERN:
            case PARENS: case TYPETEST:
            case CONDEXPR: //error recovery:
                return matchBindings;
            default:
                return MatchBindingsComputer.EMPTY;
        }
    }

    public static class MatchBindings {

        public final List<BindingSymbol> bindingsWhenTrue;
        public final List<BindingSymbol> bindingsWhenFalse;
        public final boolean nullPattern;

        public MatchBindings(List<BindingSymbol> bindingsWhenTrue, List<BindingSymbol> bindingsWhenFalse) {
            this(bindingsWhenTrue, bindingsWhenFalse, false);
        }

        public MatchBindings(List<BindingSymbol> bindingsWhenTrue, List<BindingSymbol> bindingsWhenFalse, boolean nullPattern) {
            this.bindingsWhenTrue = bindingsWhenTrue;
            this.bindingsWhenFalse = bindingsWhenFalse;
            this.nullPattern = nullPattern;
        }

    }
    private List<BindingSymbol> intersection(DiagnosticPosition pos, List<BindingSymbol> lhsBindings, List<BindingSymbol> rhsBindings) {
        // It is an error if, for intersection(a,b), if a and b contain the same variable name (may be eventually relaxed to merge variables of same type)
        List<BindingSymbol> list = List.nil();
        for (BindingSymbol v1 : lhsBindings) {
            for (BindingSymbol v2 : rhsBindings) {
                if (v1.name == v2.name &&
                    (v1.flags() & CLASH) == 0 &&
                    (v2.flags() & CLASH) == 0) {
                    log.error(pos, Errors.MatchBindingExists);
                    v2.flags_field |= CLASH;
                    list = list.append(v2);
                }
            }
        }
        return list;
    }

    @SafeVarargs
    private final List<BindingSymbol> union(DiagnosticPosition pos, List<BindingSymbol> lhsBindings, List<BindingSymbol> ... rhsBindings_s) {
        // It is an error if for union(a,b), a and b contain the same name (disjoint union).
        List<BindingSymbol> list = lhsBindings;
        for (List<BindingSymbol> rhsBindings : rhsBindings_s) {
            for (BindingSymbol v : rhsBindings) {
                for (BindingSymbol ov : list) {
                    if (ov.name == v.name &&
                        (ov.flags() & CLASH) == 0 &&
                        (v.flags() & CLASH) == 0) {
                        log.error(pos, Errors.MatchBindingExists);
                        v.flags_field |= CLASH;
                    }
                }
                list = list.append(v);
            }
        }
        return list;
    }
}
