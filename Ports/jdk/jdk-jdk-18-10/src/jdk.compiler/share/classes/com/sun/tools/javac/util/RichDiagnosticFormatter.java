/*
 * Copyright (c) 2009, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.util;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import com.sun.tools.javac.code.Printer;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.*;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.resources.CompilerProperties.Fragments;

import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.TypeTag.*;
import static com.sun.tools.javac.code.Kinds.*;
import static com.sun.tools.javac.code.Kinds.Kind.*;
import static com.sun.tools.javac.util.LayoutCharacters.*;
import static com.sun.tools.javac.util.RichDiagnosticFormatter.RichConfiguration.*;

/**
 * A rich diagnostic formatter is a formatter that provides better integration
 * with javac's type system. A diagnostic is first preprocessed in order to keep
 * track of each types/symbols in it; after this information is collected,
 * the diagnostic is rendered using a standard formatter, whose type/symbol printer
 * has been replaced by a more refined version provided by this rich formatter.
 * The rich formatter currently enables three different features: (i) simple class
 * names - that is class names are displayed used a non qualified name (thus
 * omitting package info) whenever possible - (ii) where clause list - a list of
 * additional subdiagnostics that provide specific info about type-variables,
 * captured types, intersection types that occur in the diagnostic that is to be
 * formatted and (iii) type-variable disambiguation - when the diagnostic refers
 * to two different type-variables with the same name, their representation is
 * disambiguated by appending an index to the type variable name.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b>
 */
public class RichDiagnosticFormatter extends
        ForwardingDiagnosticFormatter<JCDiagnostic, AbstractDiagnosticFormatter> {

    final Symtab syms;
    final Types types;
    final JCDiagnostic.Factory diags;
    final JavacMessages messages;

    /* name simplifier used by this formatter */
    protected ClassNameSimplifier nameSimplifier;

    /* type/symbol printer used by this formatter */
    private RichPrinter printer;

    /* map for keeping track of a where clause associated to a given type */
    Map<WhereClauseKind, Map<Type, JCDiagnostic>> whereClauses;

    /** Get the DiagnosticFormatter instance for this context. */
    public static RichDiagnosticFormatter instance(Context context) {
        RichDiagnosticFormatter instance = context.get(RichDiagnosticFormatter.class);
        if (instance == null)
            instance = new RichDiagnosticFormatter(context);
        return instance;
    }

    protected RichDiagnosticFormatter(Context context) {
        super((AbstractDiagnosticFormatter)Log.instance(context).getDiagnosticFormatter());
        setRichPrinter(new RichPrinter());
        this.syms = Symtab.instance(context);
        this.diags = JCDiagnostic.Factory.instance(context);
        this.types = Types.instance(context);
        this.messages = JavacMessages.instance(context);
        whereClauses = new EnumMap<>(WhereClauseKind.class);
        configuration = new RichConfiguration(Options.instance(context), formatter);
        for (WhereClauseKind kind : WhereClauseKind.values())
            whereClauses.put(kind, new LinkedHashMap<Type, JCDiagnostic>());
    }

    @Override
    public String format(JCDiagnostic diag, Locale l) {
        StringBuilder sb = new StringBuilder();
        nameSimplifier = new ClassNameSimplifier();
        for (WhereClauseKind kind : WhereClauseKind.values())
            whereClauses.get(kind).clear();
        preprocessDiagnostic(diag);
        sb.append(formatter.format(diag, l));
        if (getConfiguration().isEnabled(RichFormatterFeature.WHERE_CLAUSES)) {
            List<JCDiagnostic> clauses = getWhereClauses();
            String indent = formatter.isRaw() ? "" :
                formatter.indentString(DetailsInc);
            for (JCDiagnostic d : clauses) {
                String whereClause = formatter.format(d, l);
                if (whereClause.length() > 0) {
                    sb.append('\n' + indent + whereClause);
                }
            }
        }
        return sb.toString();
    }

    @Override
    public String formatMessage(JCDiagnostic diag, Locale l) {
        nameSimplifier = new ClassNameSimplifier();
        preprocessDiagnostic(diag);
        return super.formatMessage(diag, l);
    }

    /**
     * Sets the type/symbol printer used by this formatter.
     * @param printer the rich printer to be set
     */
    protected void setRichPrinter(RichPrinter printer) {
        this.printer = printer;
        formatter.setPrinter(printer);
    }

    /**
     * Returns the type/symbol printer used by this formatter.
     * @return type/symbol rich printer
     */
    protected RichPrinter getRichPrinter() {
        return printer;
    }

    /**
     * Preprocess a given diagnostic by looking both into its arguments and into
     * its subdiagnostics (if any). This preprocessing is responsible for
     * generating info corresponding to features like where clauses, name
     * simplification, etc.
     *
     * @param diag the diagnostic to be preprocessed
     */
    protected void preprocessDiagnostic(JCDiagnostic diag) {
        for (Object o : diag.getArgs()) {
            if (o != null) {
                preprocessArgument(o);
            }
        }
        if (diag.isMultiline()) {
            for (JCDiagnostic d : diag.getSubdiagnostics())
                preprocessDiagnostic(d);
        }
    }

    /**
     * Preprocess a diagnostic argument. A type/symbol argument is
     * preprocessed by specialized type/symbol preprocessors.
     *
     * @param arg the argument to be translated
     */
    protected void preprocessArgument(Object arg) {
        if (arg instanceof Type type) {
            preprocessType(type);
        }
        else if (arg instanceof Symbol symbol) {
            preprocessSymbol(symbol);
        }
        else if (arg instanceof JCDiagnostic diagnostic) {
            preprocessDiagnostic(diagnostic);
        }
        else if (arg instanceof Iterable<?> iterable && !(arg instanceof Path)) {
            for (Object o : iterable) {
                preprocessArgument(o);
            }
        }
    }

    /**
     * Build a list of multiline diagnostics containing detailed info about
     * type-variables, captured types, and intersection types
     *
     * @return where clause list
     */
    protected List<JCDiagnostic> getWhereClauses() {
        List<JCDiagnostic> clauses = List.nil();
        for (WhereClauseKind kind : WhereClauseKind.values()) {
            List<JCDiagnostic> lines = List.nil();
            for (Map.Entry<Type, JCDiagnostic> entry : whereClauses.get(kind).entrySet()) {
                lines = lines.prepend(entry.getValue());
            }
            if (!lines.isEmpty()) {
                String key = kind.key();
                if (lines.size() > 1)
                    key += ".1";
                JCDiagnostic d = diags.fragment(key, whereClauses.get(kind).keySet());
                d = new JCDiagnostic.MultilineDiagnostic(d, lines.reverse());
                clauses = clauses.prepend(d);
            }
        }
        return clauses.reverse();
    }

    private int indexOf(Type type, WhereClauseKind kind) {
        int index = 1;
        for (Type t : whereClauses.get(kind).keySet()) {
            if (t.tsym == type.tsym) {
                return index;
            }
            if (kind != WhereClauseKind.TYPEVAR ||
                    t.toString().equals(type.toString())) {
                index++;
            }
        }
        return -1;
    }

    private boolean unique(TypeVar typevar) {
        typevar = (TypeVar) typevar.stripMetadata();

        int found = 0;
        for (Type t : whereClauses.get(WhereClauseKind.TYPEVAR).keySet()) {
            if (t.stripMetadata().toString().equals(typevar.toString())) {
                found++;
            }
        }
        if (found < 1)
            throw new AssertionError("Missing type variable in where clause: " + typevar);
        return found == 1;
    }
    //where
    /**
     * This enum defines all possible kinds of where clauses that can be
     * attached by a rich diagnostic formatter to a given diagnostic
     */
    enum WhereClauseKind {

        /** where clause regarding a type variable */
        TYPEVAR("where.description.typevar"),
        /** where clause regarding a captured type */
        CAPTURED("where.description.captured"),
        /** where clause regarding an intersection type */
        INTERSECTION("where.description.intersection");

        /** resource key for this where clause kind */
        private final String key;

        WhereClauseKind(String key) {
            this.key = key;
        }

        String key() {
            return key;
        }
    }

    // <editor-fold defaultstate="collapsed" desc="name simplifier">
    /**
     * A name simplifier keeps track of class names usages in order to determine
     * whether a class name can be compacted or not. Short names are not used
     * if a conflict is detected, e.g. when two classes with the same simple
     * name belong to different packages - in this case the formatter reverts
     * to fullnames as compact names might lead to a confusing diagnostic.
     */
    protected class ClassNameSimplifier {

        /* table for keeping track of all short name usages */
        Map<Name, List<Symbol>> nameClashes = new HashMap<>();

        /**
         * Add a name usage to the simplifier's internal cache
         */
        protected void addUsage(Symbol sym) {
            Name n = sym.getSimpleName();
            List<Symbol> conflicts = nameClashes.get(n);
            if (conflicts == null) {
                conflicts = List.nil();
            }
            if (!conflicts.contains(sym))
                nameClashes.put(n, conflicts.append(sym));
        }

        public String simplify(Symbol s) {
            String name = s.getQualifiedName().toString();
            if (!s.type.isCompound() && !s.type.isPrimitive()) {
                List<Symbol> conflicts = nameClashes.get(s.getSimpleName());
                if (conflicts == null ||
                    (conflicts.size() == 1 &&
                    conflicts.contains(s))) {
                    List<Name> l = List.nil();
                    Symbol s2 = s;
                    while (s2.type.hasTag(CLASS) &&
                            s2.type.getEnclosingType().hasTag(CLASS) &&
                            s2.owner.kind == TYP) {
                        l = l.prepend(s2.getSimpleName());
                        s2 = s2.owner;
                    }
                    l = l.prepend(s2.getSimpleName());
                    StringBuilder buf = new StringBuilder();
                    String sep = "";
                    for (Name n2 : l) {
                        buf.append(sep);
                        buf.append(n2);
                        sep = ".";
                    }
                    name = buf.toString();
                }
            }
            return name;
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="rich printer">
    /**
     * Enhanced type/symbol printer that provides support for features like simple names
     * and type variable disambiguation. This enriched printer exploits the info
     * discovered during type/symbol preprocessing. This printer is set on the delegate
     * formatter so that rich type/symbol info can be properly rendered.
     */
    protected class RichPrinter extends Printer {

        @Override
        public String localize(Locale locale, String key, Object... args) {
            return formatter.localize(locale, key, args);
        }

        @Override
        public String capturedVarId(CapturedType t, Locale locale) {
            return indexOf(t, WhereClauseKind.CAPTURED) + "";
        }

        @Override
        public String visitType(Type t, Locale locale) {
            String s = super.visitType(t, locale);
            if (t == syms.botType)
                s = localize(locale, "compiler.misc.type.null");
            return s;
        }

        @Override
        public String visitCapturedType(CapturedType t, Locale locale) {
            if (getConfiguration().isEnabled(RichFormatterFeature.WHERE_CLAUSES)) {
                return localize(locale,
                    "compiler.misc.captured.type",
                    indexOf(t, WhereClauseKind.CAPTURED));
            }
            else
                return super.visitCapturedType(t, locale);
        }

        @Override
        public String visitClassType(ClassType t, Locale locale) {
            if (t.isCompound() &&
                    getConfiguration().isEnabled(RichFormatterFeature.WHERE_CLAUSES)) {
                return localize(locale,
                        "compiler.misc.intersection.type",
                        indexOf(t, WhereClauseKind.INTERSECTION));
            }
            else
                return super.visitClassType(t, locale);
        }

        @Override
        protected String className(ClassType t, boolean longform, Locale locale) {
            Symbol sym = t.tsym;
            if (sym.name.length() == 0 ||
                    !getConfiguration().isEnabled(RichFormatterFeature.SIMPLE_NAMES)) {
                return super.className(t, longform, locale);
            }
            else if (longform)
                return nameSimplifier.simplify(sym).toString();
            else
                return sym.name.toString();
        }

        @Override
        public String visitTypeVar(TypeVar t, Locale locale) {
            if (unique(t) ||
                    !getConfiguration().isEnabled(RichFormatterFeature.UNIQUE_TYPEVAR_NAMES)) {
                return t.toString();
            }
            else {
                return localize(locale,
                        "compiler.misc.type.var",
                        t.toString(), indexOf(t, WhereClauseKind.TYPEVAR));
            }
        }

        @Override
        public String visitClassSymbol(ClassSymbol s, Locale locale) {
            if (s.type.isCompound()) {
                return visit(s.type, locale);
            }
            String name = nameSimplifier.simplify(s);
            if (name.length() == 0 ||
                    !getConfiguration().isEnabled(RichFormatterFeature.SIMPLE_NAMES)) {
                return super.visitClassSymbol(s, locale);
            }
            else {
                return name;
            }
        }

        @Override
        public String visitMethodSymbol(MethodSymbol s, Locale locale) {
            String ownerName = visit(s.owner, locale);
            if (s.isStaticOrInstanceInit()) {
               return ownerName;
            } else {
                String ms = (s.name == s.name.table.names.init)
                    ? ownerName
                    : s.name.toString();
                if (s.type != null) {
                    if (s.type.hasTag(FORALL)) {
                        ms = "<" + visitTypes(s.type.getTypeArguments(), locale) + ">" + ms;
                    }
                    ms += "(" + printMethodArgs(
                            s.type.getParameterTypes(),
                            (s.flags() & VARARGS) != 0,
                            locale) + ")";
                }
                return ms;
            }
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="type scanner">
    /**
     * Preprocess a given type looking for (i) additional info (where clauses) to be
     * added to the main diagnostic (ii) names to be compacted.
     */
    protected void preprocessType(Type t) {
        typePreprocessor.visit(t);
    }
    //where
    protected Types.UnaryVisitor<Void> typePreprocessor =
            new Types.UnaryVisitor<Void>() {

        public Void visit(List<Type> ts) {
            for (Type t : ts)
                visit(t);
            return null;
        }

        @Override
        public Void visitForAll(ForAll t, Void ignored) {
            visit(t.tvars);
            visit(t.qtype);
            return null;
        }

        @Override
        public Void visitMethodType(MethodType t, Void ignored) {
            visit(t.argtypes);
            visit(t.restype);
            return null;
        }

        @Override
        public Void visitErrorType(ErrorType t, Void ignored) {
            Type ot = t.getOriginalType();
            if (ot != null)
                visit(ot);
            return null;
        }

        @Override
        public Void visitArrayType(ArrayType t, Void ignored) {
            visit(t.elemtype);
            return null;
        }

        @Override
        public Void visitWildcardType(WildcardType t, Void ignored) {
            visit(t.type);
            return null;
        }

        public Void visitType(Type t, Void ignored) {
            return null;
        }

        @Override
        public Void visitCapturedType(CapturedType t, Void ignored) {
            if (indexOf(t, WhereClauseKind.CAPTURED) == -1) {
                String suffix = t.lower == syms.botType ? ".1" : "";
                JCDiagnostic d = diags.fragment("where.captured"+ suffix, t, t.getUpperBound(), t.lower, t.wildcard);
                whereClauses.get(WhereClauseKind.CAPTURED).put(t, d);
                visit(t.wildcard);
                visit(t.lower);
                visit(t.getUpperBound());
            }
            return null;
        }

        @Override
        public Void visitClassType(ClassType t, Void ignored) {
            if (t.isCompound()) {
                if (indexOf(t, WhereClauseKind.INTERSECTION) == -1) {
                    Type supertype = types.supertype(t);
                    List<Type> interfaces = types.interfaces(t);
                    JCDiagnostic d = diags.fragment(Fragments.WhereIntersection(t, interfaces.prepend(supertype)));
                    whereClauses.get(WhereClauseKind.INTERSECTION).put(t, d);
                    visit(supertype);
                    visit(interfaces);
                }
            } else if (t.tsym.name.isEmpty()) {
                //anon class
                ClassType norm = (ClassType) t.tsym.type;
                if (norm != null) {
                    if (norm.interfaces_field != null && norm.interfaces_field.nonEmpty()) {
                        visit(norm.interfaces_field.head);
                    } else {
                        visit(norm.supertype_field);
                    }
                }
            }
            nameSimplifier.addUsage(t.tsym);
            visit(t.getTypeArguments());
            Type enclosingType;
            try {
                enclosingType = t.getEnclosingType();
            } catch (CompletionFailure cf) {
                return null;
            }
            if (enclosingType != Type.noType)
                visit(t.getEnclosingType());
            return null;
        }

        @Override
        public Void visitTypeVar(TypeVar t, Void ignored) {
            t = (TypeVar)t.stripMetadataIfNeeded();
            if (indexOf(t, WhereClauseKind.TYPEVAR) == -1) {
                //access the bound type and skip error types
                Type bound = t.getUpperBound();
                while ((bound instanceof ErrorType errorType))
                    bound = errorType.getOriginalType();
                //retrieve the bound list - if the type variable
                //has not been attributed the bound is not set
                List<Type> bounds = (bound != null) &&
                        (bound.hasTag(CLASS) || bound.hasTag(TYPEVAR)) ?
                    getBounds(bound) :
                    List.nil();

                nameSimplifier.addUsage(t.tsym);

                boolean boundErroneous = bounds.head == null ||
                                         bounds.head.hasTag(NONE) ||
                                         bounds.head.hasTag(ERROR);

                if ((t.tsym.flags() & SYNTHETIC) == 0) {
                    //this is a true typevar
                    JCDiagnostic d = diags.fragment("where.typevar" +
                        (boundErroneous ? ".1" : ""), t, bounds,
                        kindName(t.tsym.location()), t.tsym.location());
                    whereClauses.get(WhereClauseKind.TYPEVAR).put(t, d);
                    symbolPreprocessor.visit(t.tsym.location(), null);
                    visit(bounds);
                } else {
                    Assert.check(!boundErroneous);
                    //this is a fresh (synthetic) tvar
                    JCDiagnostic d = diags.fragment(Fragments.WhereFreshTypevar(t, bounds));
                    whereClauses.get(WhereClauseKind.TYPEVAR).put(t, d);
                    visit(bounds);
                }

            }
            return null;
        }
        //where:
            private List<Type> getBounds(Type bound) {
                return bound.isCompound() ? types.directSupertypes(bound) : List.of(bound);
            }
    };
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="symbol scanner">
    /**
     * Preprocess a given symbol looking for (i) additional info (where clauses) to be
     * added to the main diagnostic (ii) names to be compacted
     */
    protected void preprocessSymbol(Symbol s) {
        symbolPreprocessor.visit(s, null);
    }
    //where
    protected Types.DefaultSymbolVisitor<Void, Void> symbolPreprocessor =
            new Types.DefaultSymbolVisitor<Void, Void>() {

        @Override
        public Void visitClassSymbol(ClassSymbol s, Void ignored) {
            if (s.type.isCompound()) {
                typePreprocessor.visit(s.type);
            } else {
                nameSimplifier.addUsage(s);
            }
            return null;
        }

        @Override
        public Void visitSymbol(Symbol s, Void ignored) {
            return null;
        }

        @Override
        public Void visitMethodSymbol(MethodSymbol s, Void ignored) {
            visit(s.owner, null);
            if (s.type != null)
                typePreprocessor.visit(s.type);
            return null;
        }
    };
    // </editor-fold>

    @Override
    public RichConfiguration getConfiguration() {
        //the following cast is always safe - see init
        return (RichConfiguration)configuration;
    }

    /**
     * Configuration object provided by the rich formatter.
     */
    public static class RichConfiguration extends ForwardingDiagnosticFormatter.ForwardingConfiguration {

        /** set of enabled rich formatter's features */
        protected java.util.EnumSet<RichFormatterFeature> features;

        @SuppressWarnings("fallthrough")
        public RichConfiguration(Options options, AbstractDiagnosticFormatter formatter) {
            super(formatter.getConfiguration());
            features = formatter.isRaw() ? EnumSet.noneOf(RichFormatterFeature.class) :
                EnumSet.of(RichFormatterFeature.SIMPLE_NAMES,
                    RichFormatterFeature.WHERE_CLAUSES,
                    RichFormatterFeature.UNIQUE_TYPEVAR_NAMES);
            String diagOpts = options.get("diags.formatterOptions");
            if (diagOpts != null) {
                for (String args: diagOpts.split(",")) {
                    if (args.equals("-where")) {
                        features.remove(RichFormatterFeature.WHERE_CLAUSES);
                    }
                    else if (args.equals("where")) {
                        features.add(RichFormatterFeature.WHERE_CLAUSES);
                    }
                    if (args.equals("-simpleNames")) {
                        features.remove(RichFormatterFeature.SIMPLE_NAMES);
                    }
                    else if (args.equals("simpleNames")) {
                        features.add(RichFormatterFeature.SIMPLE_NAMES);
                    }
                    if (args.equals("-disambiguateTvars")) {
                        features.remove(RichFormatterFeature.UNIQUE_TYPEVAR_NAMES);
                    }
                    else if (args.equals("disambiguateTvars")) {
                        features.add(RichFormatterFeature.UNIQUE_TYPEVAR_NAMES);
                    }
                }
            }
        }

        /**
         * Returns a list of all the features supported by the rich formatter.
         * @return list of supported features
         */
        public RichFormatterFeature[] getAvailableFeatures() {
            return RichFormatterFeature.values();
        }

        /**
         * Enable a specific feature on this rich formatter.
         * @param feature feature to be enabled
         */
        public void enable(RichFormatterFeature feature) {
            features.add(feature);
        }

        /**
         * Disable a specific feature on this rich formatter.
         * @param feature feature to be disabled
         */
        public void disable(RichFormatterFeature feature) {
            features.remove(feature);
        }

        /**
         * Is a given feature enabled on this formatter?
         * @param feature feature to be tested
         */
        public boolean isEnabled(RichFormatterFeature feature) {
            return features.contains(feature);
        }

        /**
         * The advanced formatting features provided by the rich formatter
         */
        public enum RichFormatterFeature {
            /** a list of additional info regarding a given type/symbol */
            WHERE_CLAUSES,
            /** full class names simplification (where possible) */
            SIMPLE_NAMES,
            /** type-variable names disambiguation */
            UNIQUE_TYPEVAR_NAMES
        }
    }
}
