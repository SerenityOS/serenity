/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.tools.JavaFileObject;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Lint.LintCategory;
import com.sun.tools.javac.code.Scope.ImportFilter;
import com.sun.tools.javac.code.Scope.NamedImportScope;
import com.sun.tools.javac.code.Scope.StarImportScope;
import com.sun.tools.javac.code.Scope.WriteableScope;
import com.sun.tools.javac.code.Source.Feature;
import com.sun.tools.javac.comp.Annotate.AnnotationTypeMetadata;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.DefinedBy.Api;

import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Type.*;
import com.sun.tools.javac.resources.CompilerProperties.Errors;
import com.sun.tools.javac.tree.JCTree.*;

import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.Flags.ANNOTATION;
import static com.sun.tools.javac.code.Scope.LookupKind.NON_RECURSIVE;
import static com.sun.tools.javac.code.Kinds.Kind.*;
import static com.sun.tools.javac.code.TypeTag.CLASS;
import static com.sun.tools.javac.code.TypeTag.ERROR;
import com.sun.tools.javac.resources.CompilerProperties.Fragments;

import static com.sun.tools.javac.code.TypeTag.*;
import static com.sun.tools.javac.tree.JCTree.Tag.*;

import com.sun.tools.javac.util.Dependencies.CompletionCause;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticFlag;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;

/** This is the second phase of Enter, in which classes are completed
 *  by resolving their headers and entering their members in the into
 *  the class scope. See Enter for an overall overview.
 *
 *  This class uses internal phases to process the classes. When a phase
 *  processes classes, the lower phases are not invoked until all classes
 *  pass through the current phase. Note that it is possible that upper phases
 *  are run due to recursive completion. The internal phases are:
 *  - ImportPhase: shallow pass through imports, adds information about imports
 *                 the NamedImportScope and StarImportScope, but avoids queries
 *                 about class hierarchy.
 *  - HierarchyPhase: resolves the supertypes of the given class. Does not handle
 *                    type parameters of the class or type argument of the supertypes.
 *  - HeaderPhase: finishes analysis of the header of the given class by resolving
 *                 type parameters, attributing supertypes including type arguments
 *                 and scheduling full annotation attribution. This phase also adds
 *                 a synthetic default constructor if needed and synthetic "this" field.
 *  - MembersPhase: resolves headers for fields, methods and constructors in the given class.
 *                  Also generates synthetic enum members.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class TypeEnter implements Completer {
    protected static final Context.Key<TypeEnter> typeEnterKey = new Context.Key<>();

    /** A switch to determine whether we check for package/class conflicts
     */
    static final boolean checkClash = true;

    private final Names names;
    private final Enter enter;
    private final MemberEnter memberEnter;
    private final Log log;
    private final Check chk;
    private final Attr attr;
    private final Symtab syms;
    private final TreeMaker make;
    private final Todo todo;
    private final Annotate annotate;
    private final TypeAnnotations typeAnnotations;
    private final Types types;
    private final JCDiagnostic.Factory diags;
    private final DeferredLintHandler deferredLintHandler;
    private final Lint lint;
    private final TypeEnvs typeEnvs;
    private final Dependencies dependencies;
    private final Preview preview;

    public static TypeEnter instance(Context context) {
        TypeEnter instance = context.get(typeEnterKey);
        if (instance == null)
            instance = new TypeEnter(context);
        return instance;
    }

    protected TypeEnter(Context context) {
        context.put(typeEnterKey, this);
        names = Names.instance(context);
        enter = Enter.instance(context);
        memberEnter = MemberEnter.instance(context);
        log = Log.instance(context);
        chk = Check.instance(context);
        attr = Attr.instance(context);
        syms = Symtab.instance(context);
        make = TreeMaker.instance(context);
        todo = Todo.instance(context);
        annotate = Annotate.instance(context);
        typeAnnotations = TypeAnnotations.instance(context);
        types = Types.instance(context);
        diags = JCDiagnostic.Factory.instance(context);
        deferredLintHandler = DeferredLintHandler.instance(context);
        lint = Lint.instance(context);
        typeEnvs = TypeEnvs.instance(context);
        dependencies = Dependencies.instance(context);
        preview = Preview.instance(context);
        Source source = Source.instance(context);
        allowTypeAnnos = Feature.TYPE_ANNOTATIONS.allowedInSource(source);
        allowDeprecationOnImport = Feature.DEPRECATION_ON_IMPORT.allowedInSource(source);
    }

    /** Switch: support type annotations.
     */
    boolean allowTypeAnnos;

    /**
     * Switch: should deprecation warnings be issued on import
     */
    boolean allowDeprecationOnImport;

    /** A flag to disable completion from time to time during member
     *  enter, as we only need to look up types.  This avoids
     *  unnecessarily deep recursion.
     */
    boolean completionEnabled = true;

    /* Verify Imports:
     */
    protected void ensureImportsChecked(List<JCCompilationUnit> trees) {
        // if there remain any unimported toplevels (these must have
        // no classes at all), process their import statements as well.
        for (JCCompilationUnit tree : trees) {
            if (!tree.starImportScope.isFilled()) {
                Env<AttrContext> topEnv = enter.topLevelEnv(tree);
                finishImports(tree, () -> { completeClass.resolveImports(tree, topEnv); });
            }
        }
    }

/* ********************************************************************
 * Source completer
 *********************************************************************/

    /** Complete entering a class.
     *  @param sym         The symbol of the class to be completed.
     */
    @Override
    public void complete(Symbol sym) throws CompletionFailure {
        // Suppress some (recursive) MemberEnter invocations
        if (!completionEnabled) {
            // Re-install same completer for next time around and return.
            Assert.check((sym.flags() & Flags.COMPOUND) == 0);
            sym.completer = this;
            return;
        }

        try {
            annotate.blockAnnotations();
            sym.flags_field |= UNATTRIBUTED;

            List<Env<AttrContext>> queue;

            dependencies.push((ClassSymbol) sym, CompletionCause.MEMBER_ENTER);
            try {
                queue = completeClass.completeEnvs(List.of(typeEnvs.get((ClassSymbol) sym)));
            } finally {
                dependencies.pop();
            }

            if (!queue.isEmpty()) {
                Set<JCCompilationUnit> seen = new HashSet<>();

                for (Env<AttrContext> env : queue) {
                    if (env.toplevel.defs.contains(env.enclClass) && seen.add(env.toplevel)) {
                        finishImports(env.toplevel, () -> {});
                    }
                }
            }
        } finally {
            annotate.unblockAnnotations();
        }
    }

    void finishImports(JCCompilationUnit toplevel, Runnable resolve) {
        JavaFileObject prev = log.useSource(toplevel.sourcefile);
        try {
            resolve.run();
            chk.checkImportsUnique(toplevel);
            chk.checkImportsResolvable(toplevel);
            chk.checkImportedPackagesObservable(toplevel);
            toplevel.namedImportScope.finalizeScope();
            toplevel.starImportScope.finalizeScope();
        } catch (CompletionFailure cf) {
            chk.completionError(toplevel.pos(), cf);
        } finally {
            log.useSource(prev);
        }
    }

    abstract class Phase {
        private final ListBuffer<Env<AttrContext>> queue = new ListBuffer<>();
        private final Phase next;
        private final CompletionCause phaseName;

        Phase(CompletionCause phaseName, Phase next) {
            this.phaseName = phaseName;
            this.next = next;
        }

        public final List<Env<AttrContext>> completeEnvs(List<Env<AttrContext>> envs) {
            boolean firstToComplete = queue.isEmpty();

            Phase prevTopLevelPhase = topLevelPhase;
            boolean success = false;

            try {
                topLevelPhase = this;
                doCompleteEnvs(envs);
                success = true;
            } finally {
                topLevelPhase = prevTopLevelPhase;
                if (!success && firstToComplete) {
                    //an exception was thrown, e.g. BreakAttr:
                    //the queue would become stale, clear it:
                    queue.clear();
                }
            }

            if (firstToComplete) {
                List<Env<AttrContext>> out = queue.toList();

                queue.clear();
                return next != null ? next.completeEnvs(out) : out;
            } else {
                return List.nil();
            }
        }

        protected void doCompleteEnvs(List<Env<AttrContext>> envs) {
            for (Env<AttrContext> env : envs) {
                JCClassDecl tree = (JCClassDecl)env.tree;

                queue.add(env);

                JavaFileObject prev = log.useSource(env.toplevel.sourcefile);
                DiagnosticPosition prevLintPos = deferredLintHandler.setPos(tree.pos());
                try {
                    dependencies.push(env.enclClass.sym, phaseName);
                    runPhase(env);
                } catch (CompletionFailure ex) {
                    chk.completionError(tree.pos(), ex);
                } finally {
                    dependencies.pop();
                    deferredLintHandler.setPos(prevLintPos);
                    log.useSource(prev);
                }
            }
        }

        protected abstract void runPhase(Env<AttrContext> env);
    }

    private final ImportsPhase completeClass = new ImportsPhase();
    private Phase topLevelPhase;

    /**Analyze import clauses.
     */
    private final class ImportsPhase extends Phase {

        public ImportsPhase() {
            super(CompletionCause.IMPORTS_PHASE, new HierarchyPhase());
        }

        Env<AttrContext> env;
        ImportFilter staticImportFilter;
        ImportFilter typeImportFilter;
        BiConsumer<JCImport, CompletionFailure> cfHandler =
                (imp, cf) -> chk.completionError(imp.pos(), cf);

        @Override
        protected void runPhase(Env<AttrContext> env) {
            JCClassDecl tree = env.enclClass;
            ClassSymbol sym = tree.sym;

            // If sym is a toplevel-class, make sure any import
            // clauses in its source file have been seen.
            if (sym.owner.kind == PCK) {
                resolveImports(env.toplevel, env.enclosing(TOPLEVEL));
                todo.append(env);
            }

            if (sym.owner.kind == TYP)
                sym.owner.complete();
        }

        private void resolveImports(JCCompilationUnit tree, Env<AttrContext> env) {
            if (tree.starImportScope.isFilled()) {
                // we must have already processed this toplevel
                return;
            }

            ImportFilter prevStaticImportFilter = staticImportFilter;
            ImportFilter prevTypeImportFilter = typeImportFilter;
            DiagnosticPosition prevLintPos = deferredLintHandler.immediate();
            Lint prevLint = chk.setLint(lint);
            Env<AttrContext> prevEnv = this.env;
            try {
                this.env = env;
                final PackageSymbol packge = env.toplevel.packge;
                this.staticImportFilter =
                        (origin, sym) -> sym.isStatic() &&
                                         chk.importAccessible(sym, packge) &&
                                         sym.isMemberOf((TypeSymbol) origin.owner, types);
                this.typeImportFilter =
                        (origin, sym) -> sym.kind == TYP &&
                                         chk.importAccessible(sym, packge);

                // Import-on-demand java.lang.
                PackageSymbol javaLang = syms.enterPackage(syms.java_base, names.java_lang);
                if (javaLang.members().isEmpty() && !javaLang.exists())
                    throw new FatalError(diags.fragment(Fragments.FatalErrNoJavaLang));
                importAll(make.at(tree.pos()).Import(make.QualIdent(javaLang), false), javaLang, env);

                JCModuleDecl decl = tree.getModuleDecl();

                // Process the package def and all import clauses.
                if (tree.getPackage() != null && decl == null)
                    checkClassPackageClash(tree.getPackage());

                for (JCImport imp : tree.getImports()) {
                    doImport(imp);
                }

                if (decl != null) {
                    //check @Deprecated:
                    markDeprecated(decl.sym, decl.mods.annotations, env);
                    // process module annotations
                    annotate.annotateLater(decl.mods.annotations, env, env.toplevel.modle, null);
                }
            } finally {
                this.env = prevEnv;
                chk.setLint(prevLint);
                deferredLintHandler.setPos(prevLintPos);
                this.staticImportFilter = prevStaticImportFilter;
                this.typeImportFilter = prevTypeImportFilter;
            }
        }

        private void checkClassPackageClash(JCPackageDecl tree) {
            // check that no class exists with same fully qualified name as
            // toplevel package
            if (checkClash && tree.pid != null) {
                Symbol p = env.toplevel.packge;
                while (p.owner != syms.rootPackage) {
                    p.owner.complete(); // enter all class members of p
                    //need to lookup the owning module/package:
                    PackageSymbol pack = syms.lookupPackage(env.toplevel.modle, p.owner.getQualifiedName());
                    if (syms.getClass(pack.modle, p.getQualifiedName()) != null) {
                        log.error(tree.pos,
                                  Errors.PkgClashesWithClassOfSameName(p));
                    }
                    p = p.owner;
                }
            }
            // process package annotations
            annotate.annotateLater(tree.annotations, env, env.toplevel.packge, null);
        }

        private void doImport(JCImport tree) {
            JCFieldAccess imp = (JCFieldAccess)tree.qualid;
            Name name = TreeInfo.name(imp);

            // Create a local environment pointing to this tree to disable
            // effects of other imports in Resolve.findGlobalType
            Env<AttrContext> localEnv = env.dup(tree);

            TypeSymbol p = attr.attribImportQualifier(tree, localEnv).tsym;
            if (name == names.asterisk) {
                // Import on demand.
                chk.checkCanonical(imp.selected);
                if (tree.staticImport)
                    importStaticAll(tree, p, env);
                else
                    importAll(tree, p, env);
            } else {
                // Named type import.
                if (tree.staticImport) {
                    importNamedStatic(tree, p, name, localEnv);
                    chk.checkCanonical(imp.selected);
                } else {
                    Type importedType = attribImportType(imp, localEnv);
                    Type originalType = importedType.getOriginalType();
                    TypeSymbol c = originalType.hasTag(CLASS) ? originalType.tsym : importedType.tsym;
                    chk.checkCanonical(imp);
                    importNamed(tree.pos(), c, env, tree);
                }
            }
        }

        Type attribImportType(JCTree tree, Env<AttrContext> env) {
            Assert.check(completionEnabled);
            Lint prevLint = chk.setLint(allowDeprecationOnImport ?
                    lint : lint.suppress(LintCategory.DEPRECATION, LintCategory.REMOVAL, LintCategory.PREVIEW));
            try {
                // To prevent deep recursion, suppress completion of some
                // types.
                completionEnabled = false;
                return attr.attribType(tree, env);
            } finally {
                completionEnabled = true;
                chk.setLint(prevLint);
            }
        }

        /** Import all classes of a class or package on demand.
         *  @param imp           The import that is being handled.
         *  @param tsym          The class or package the members of which are imported.
         *  @param env           The env in which the imported classes will be entered.
         */
        private void importAll(JCImport imp,
                               final TypeSymbol tsym,
                               Env<AttrContext> env) {
            env.toplevel.starImportScope.importAll(types, tsym.members(), typeImportFilter, imp, cfHandler);
        }

        /** Import all static members of a class or package on demand.
         *  @param imp           The import that is being handled.
         *  @param tsym          The class or package the members of which are imported.
         *  @param env           The env in which the imported classes will be entered.
         */
        private void importStaticAll(JCImport imp,
                                     final TypeSymbol tsym,
                                     Env<AttrContext> env) {
            final StarImportScope toScope = env.toplevel.starImportScope;
            final TypeSymbol origin = tsym;

            toScope.importAll(types, origin.members(), staticImportFilter, imp, cfHandler);
        }

        /** Import statics types of a given name.  Non-types are handled in Attr.
         *  @param imp           The import that is being handled.
         *  @param tsym          The class from which the name is imported.
         *  @param name          The (simple) name being imported.
         *  @param env           The environment containing the named import
         *                  scope to add to.
         */
        private void importNamedStatic(final JCImport imp,
                                       final TypeSymbol tsym,
                                       final Name name,
                                       final Env<AttrContext> env) {
            if (tsym.kind != TYP) {
                log.error(DiagnosticFlag.RECOVERABLE, imp.pos(), Errors.StaticImpOnlyClassesAndInterfaces);
                return;
            }

            final NamedImportScope toScope = env.toplevel.namedImportScope;
            final Scope originMembers = tsym.members();

            imp.importScope = toScope.importByName(types, originMembers, name, staticImportFilter, imp, cfHandler);
        }

        /** Import given class.
         *  @param pos           Position to be used for error reporting.
         *  @param tsym          The class to be imported.
         *  @param env           The environment containing the named import
         *                  scope to add to.
         */
        private void importNamed(DiagnosticPosition pos, final Symbol tsym, Env<AttrContext> env, JCImport imp) {
            if (tsym.kind == TYP)
                imp.importScope = env.toplevel.namedImportScope.importType(tsym.owner.members(), tsym.owner.members(), tsym);
        }

    }

    /**Defines common utility methods used by the HierarchyPhase and HeaderPhase.
     */
    private abstract class AbstractHeaderPhase extends Phase {

        public AbstractHeaderPhase(CompletionCause phaseName, Phase next) {
            super(phaseName, next);
        }

        protected Env<AttrContext> baseEnv(JCClassDecl tree, Env<AttrContext> env) {
            WriteableScope baseScope = WriteableScope.create(tree.sym);
            //import already entered local classes into base scope
            for (Symbol sym : env.outer.info.scope.getSymbols(NON_RECURSIVE)) {
                if (sym.isDirectlyOrIndirectlyLocal()) {
                    baseScope.enter(sym);
                }
            }
            //import current type-parameters into base scope
            if (tree.typarams != null)
                for (List<JCTypeParameter> typarams = tree.typarams;
                     typarams.nonEmpty();
                     typarams = typarams.tail)
                    baseScope.enter(typarams.head.type.tsym);
            Env<AttrContext> outer = env.outer; // the base clause can't see members of this class
            Env<AttrContext> localEnv = outer.dup(tree, outer.info.dup(baseScope));
            localEnv.baseClause = true;
            localEnv.outer = outer;
            localEnv.info.isSelfCall = false;
            return localEnv;
        }

        /** Generate a base clause for an enum type.
         *  @param pos              The position for trees and diagnostics, if any
         *  @param c                The class symbol of the enum
         */
        protected  JCExpression enumBase(int pos, ClassSymbol c) {
            JCExpression result = make.at(pos).
                TypeApply(make.QualIdent(syms.enumSym),
                          List.of(make.Type(c.type)));
            return result;
        }

        protected Type modelMissingTypes(Env<AttrContext> env, Type t, final JCExpression tree, final boolean interfaceExpected) {
            if (!t.hasTag(ERROR))
                return t;

            return new ErrorType(t.getOriginalType(), t.tsym) {
                private Type modelType;

                @Override
                public Type getModelType() {
                    if (modelType == null)
                        modelType = new Synthesizer(env.toplevel.modle, getOriginalType(), interfaceExpected).visit(tree);
                    return modelType;
                }
            };
        }
            // where:
            private class Synthesizer extends JCTree.Visitor {
                ModuleSymbol msym;
                Type originalType;
                boolean interfaceExpected;
                List<ClassSymbol> synthesizedSymbols = List.nil();
                Type result;

                Synthesizer(ModuleSymbol msym, Type originalType, boolean interfaceExpected) {
                    this.msym = msym;
                    this.originalType = originalType;
                    this.interfaceExpected = interfaceExpected;
                }

                Type visit(JCTree tree) {
                    tree.accept(this);
                    return result;
                }

                List<Type> visit(List<? extends JCTree> trees) {
                    ListBuffer<Type> lb = new ListBuffer<>();
                    for (JCTree t: trees)
                        lb.append(visit(t));
                    return lb.toList();
                }

                @Override
                public void visitTree(JCTree tree) {
                    result = syms.errType;
                }

                @Override
                public void visitIdent(JCIdent tree) {
                    if (!tree.type.hasTag(ERROR)) {
                        result = tree.type;
                    } else {
                        result = synthesizeClass(tree.name, msym.unnamedPackage).type;
                    }
                }

                @Override
                public void visitSelect(JCFieldAccess tree) {
                    if (!tree.type.hasTag(ERROR)) {
                        result = tree.type;
                    } else {
                        Type selectedType;
                        boolean prev = interfaceExpected;
                        try {
                            interfaceExpected = false;
                            selectedType = visit(tree.selected);
                        } finally {
                            interfaceExpected = prev;
                        }
                        ClassSymbol c = synthesizeClass(tree.name, selectedType.tsym);
                        result = c.type;
                    }
                }

                @Override
                public void visitTypeApply(JCTypeApply tree) {
                    if (!tree.type.hasTag(ERROR)) {
                        result = tree.type;
                    } else {
                        ClassType clazzType = (ClassType) visit(tree.clazz);
                        if (synthesizedSymbols.contains(clazzType.tsym))
                            synthesizeTyparams((ClassSymbol) clazzType.tsym, tree.arguments.size());
                        final List<Type> actuals = visit(tree.arguments);
                        result = new ErrorType(tree.type, clazzType.tsym) {
                            @Override @DefinedBy(Api.LANGUAGE_MODEL)
                            public List<Type> getTypeArguments() {
                                return actuals;
                            }
                        };
                    }
                }

                ClassSymbol synthesizeClass(Name name, Symbol owner) {
                    int flags = interfaceExpected ? INTERFACE : 0;
                    ClassSymbol c = new ClassSymbol(flags, name, owner);
                    c.members_field = new Scope.ErrorScope(c);
                    c.type = new ErrorType(originalType, c) {
                        @Override @DefinedBy(Api.LANGUAGE_MODEL)
                        public List<Type> getTypeArguments() {
                            return typarams_field;
                        }
                    };
                    synthesizedSymbols = synthesizedSymbols.prepend(c);
                    return c;
                }

                void synthesizeTyparams(ClassSymbol sym, int n) {
                    ClassType ct = (ClassType) sym.type;
                    Assert.check(ct.typarams_field.isEmpty());
                    if (n == 1) {
                        TypeVar v = new TypeVar(names.fromString("T"), sym, syms.botType);
                        ct.typarams_field = ct.typarams_field.prepend(v);
                    } else {
                        for (int i = n; i > 0; i--) {
                            TypeVar v = new TypeVar(names.fromString("T" + i), sym,
                                                    syms.botType);
                            ct.typarams_field = ct.typarams_field.prepend(v);
                        }
                    }
                }
            }

        protected void attribSuperTypes(Env<AttrContext> env, Env<AttrContext> baseEnv) {
            JCClassDecl tree = env.enclClass;
            ClassSymbol sym = tree.sym;
            ClassType ct = (ClassType)sym.type;
            // Determine supertype.
            Type supertype;
            JCExpression extending;

            if (tree.extending != null) {
                extending = clearTypeParams(tree.extending);
                supertype = attr.attribBase(extending, baseEnv, true, false, true);
                if (supertype == syms.recordType) {
                    log.error(tree, Errors.InvalidSupertypeRecord(supertype.tsym));
                }
            } else {
                extending = null;
                supertype = ((tree.mods.flags & Flags.ENUM) != 0)
                ? attr.attribBase(enumBase(tree.pos, sym), baseEnv,
                                  true, false, false)
                : (sym.fullname == names.java_lang_Object)
                ? Type.noType
                : sym.isRecord() ? syms.recordType : syms.objectType;
            }
            ct.supertype_field = modelMissingTypes(baseEnv, supertype, extending, false);

            // Determine interfaces.
            ListBuffer<Type> interfaces = new ListBuffer<>();
            ListBuffer<Type> all_interfaces = null; // lazy init
            List<JCExpression> interfaceTrees = tree.implementing;
            for (JCExpression iface : interfaceTrees) {
                iface = clearTypeParams(iface);
                Type it = attr.attribBase(iface, baseEnv, false, true, true);
                if (it.hasTag(CLASS)) {
                    interfaces.append(it);
                    if (all_interfaces != null) all_interfaces.append(it);
                } else {
                    if (all_interfaces == null)
                        all_interfaces = new ListBuffer<Type>().appendList(interfaces);
                    all_interfaces.append(modelMissingTypes(baseEnv, it, iface, true));
                }
            }

            // Determine permits.
            ListBuffer<Symbol> permittedSubtypeSymbols = new ListBuffer<>();
            List<JCExpression> permittedTrees = tree.permitting;
            for (JCExpression permitted : permittedTrees) {
                Type pt = attr.attribBase(permitted, baseEnv, false, false, false);
                permittedSubtypeSymbols.append(pt.tsym);
            }

            if ((sym.flags_field & ANNOTATION) != 0) {
                ct.interfaces_field = List.of(syms.annotationType);
                ct.all_interfaces_field = ct.interfaces_field;
            }  else {
                ct.interfaces_field = interfaces.toList();
                ct.all_interfaces_field = (all_interfaces == null)
                        ? ct.interfaces_field : all_interfaces.toList();
            }

            /* it could be that there are already some symbols in the permitted list, for the case
             * where there are subtypes in the same compilation unit but the permits list is empty
             * so don't overwrite the permitted list if it is not empty
             */
            if (!permittedSubtypeSymbols.isEmpty()) {
                sym.permitted = permittedSubtypeSymbols.toList();
            }
            sym.isPermittedExplicit = !permittedSubtypeSymbols.isEmpty();
        }
            //where:
            protected JCExpression clearTypeParams(JCExpression superType) {
                return superType;
            }
    }

    private final class HierarchyPhase extends AbstractHeaderPhase implements Completer {

        public HierarchyPhase() {
            super(CompletionCause.HIERARCHY_PHASE, new PermitsPhase());
        }

        @Override
        protected void doCompleteEnvs(List<Env<AttrContext>> envs) {
            //The ClassSymbols in the envs list may not be in the dependency order.
            //To get proper results, for every class or interface C, the supertypes of
            //C must be processed by the HierarchyPhase phase before C.
            //To achieve that, the HierarchyPhase is registered as the Completer for
            //all the classes first, and then all the classes are completed.
            for (Env<AttrContext> env : envs) {
                env.enclClass.sym.completer = this;
            }
            for (Env<AttrContext> env : envs) {
                env.enclClass.sym.complete();
            }
        }

        @Override
        protected void runPhase(Env<AttrContext> env) {
            JCClassDecl tree = env.enclClass;
            ClassSymbol sym = tree.sym;
            ClassType ct = (ClassType)sym.type;

            Env<AttrContext> baseEnv = baseEnv(tree, env);

            attribSuperTypes(env, baseEnv);

            if (sym.fullname == names.java_lang_Object) {
                if (tree.extending != null) {
                    chk.checkNonCyclic(tree.extending.pos(),
                                       ct.supertype_field);
                    ct.supertype_field = Type.noType;
                }
                else if (tree.implementing.nonEmpty()) {
                    chk.checkNonCyclic(tree.implementing.head.pos(),
                                       ct.interfaces_field.head);
                    ct.interfaces_field = List.nil();
                }
            }

            markDeprecated(sym, tree.mods.annotations, baseEnv);

            chk.checkNonCyclicDecl(tree);
        }
            //where:
            @Override
            protected JCExpression clearTypeParams(JCExpression superType) {
                switch (superType.getTag()) {
                    case TYPEAPPLY:
                        return ((JCTypeApply) superType).clazz;
                }

                return superType;
            }

        @Override
        public void complete(Symbol sym) throws CompletionFailure {
            Assert.check((topLevelPhase instanceof ImportsPhase) ||
                         (topLevelPhase == this));

            if (topLevelPhase != this) {
                //only do the processing based on dependencies in the HierarchyPhase:
                sym.completer = this;
                return ;
            }

            Env<AttrContext> env = typeEnvs.get((ClassSymbol) sym);

            super.doCompleteEnvs(List.of(env));
        }

    }

    private final class PermitsPhase extends AbstractHeaderPhase {

        public PermitsPhase() {
            super(CompletionCause.HIERARCHY_PHASE, new HeaderPhase());
        }

        @Override
        protected void runPhase(Env<AttrContext> env) {
            JCClassDecl tree = env.enclClass;
            if (!tree.sym.isAnonymous() || tree.sym.isEnum()) {
                for (Type supertype : types.directSupertypes(tree.sym.type)) {
                    if (supertype.tsym.kind == TYP) {
                        ClassSymbol supClass = (ClassSymbol) supertype.tsym;
                        Env<AttrContext> supClassEnv = enter.getEnv(supClass);
                        if (supClass.isSealed() &&
                            !supClass.isPermittedExplicit &&
                            supClassEnv != null &&
                            supClassEnv.toplevel == env.toplevel) {
                            supClass.permitted = supClass.permitted.append(tree.sym);
                        }
                    }
                }
            }
        }

    }

    private final class HeaderPhase extends AbstractHeaderPhase {

        public HeaderPhase() {
            super(CompletionCause.HEADER_PHASE, new RecordPhase());
        }

        @Override
        protected void runPhase(Env<AttrContext> env) {
            JCClassDecl tree = env.enclClass;
            ClassSymbol sym = tree.sym;
            ClassType ct = (ClassType)sym.type;

            // create an environment for evaluating the base clauses
            Env<AttrContext> baseEnv = baseEnv(tree, env);

            if (tree.extending != null)
                annotate.queueScanTreeAndTypeAnnotate(tree.extending, baseEnv, sym, tree.pos());
            for (JCExpression impl : tree.implementing)
                annotate.queueScanTreeAndTypeAnnotate(impl, baseEnv, sym, tree.pos());
            annotate.flush();

            attribSuperTypes(env, baseEnv);

            Set<Type> interfaceSet = new HashSet<>();

            for (JCExpression iface : tree.implementing) {
                Type it = iface.type;
                if (it.hasTag(CLASS))
                    chk.checkNotRepeated(iface.pos(), types.erasure(it), interfaceSet);
            }

            annotate.annotateLater(tree.mods.annotations, baseEnv,
                        sym, tree.pos());
            attr.attribTypeVariables(tree.typarams, baseEnv, false);

            for (JCTypeParameter tp : tree.typarams)
                annotate.queueScanTreeAndTypeAnnotate(tp, baseEnv, sym, tree.pos());

            // check that no package exists with same fully qualified name,
            // but admit classes in the unnamed package which have the same
            // name as a top-level package.
            if (checkClash &&
                sym.owner.kind == PCK && sym.owner != env.toplevel.modle.unnamedPackage &&
                syms.packageExists(env.toplevel.modle, sym.fullname)) {
                log.error(tree.pos, Errors.ClashWithPkgOfSameName(Kinds.kindName(sym),sym));
            }
            if (sym.owner.kind == PCK && (sym.flags_field & PUBLIC) == 0 &&
                !env.toplevel.sourcefile.isNameCompatible(sym.name.toString(),JavaFileObject.Kind.SOURCE)) {
                sym.flags_field |= AUXILIARY;
            }
        }
    }

    private abstract class AbstractMembersPhase extends Phase {

        public AbstractMembersPhase(CompletionCause completionCause, Phase next) {
            super(completionCause, next);
        }

        private boolean completing;
        private List<Env<AttrContext>> todo = List.nil();

        @Override
        protected void doCompleteEnvs(List<Env<AttrContext>> envs) {
            todo = todo.prependList(envs);
            if (completing) {
                return ; //the top-level invocation will handle all envs
            }
            boolean prevCompleting = completing;
            completing = true;
            try {
                while (todo.nonEmpty()) {
                    Env<AttrContext> head = todo.head;
                    todo = todo.tail;
                    super.doCompleteEnvs(List.of(head));
                }
            } finally {
                completing = prevCompleting;
            }
        }

        void enterThisAndSuper(ClassSymbol sym, Env<AttrContext> env) {
            ClassType ct = (ClassType)sym.type;
            // enter symbols for 'this' into current scope.
            VarSymbol thisSym =
                    new VarSymbol(FINAL | HASINIT, names._this, sym.type, sym);
            thisSym.pos = Position.FIRSTPOS;
            env.info.scope.enter(thisSym);
            // if this is a class, enter symbol for 'super' into current scope.
            if ((sym.flags_field & INTERFACE) == 0 &&
                    ct.supertype_field.hasTag(CLASS)) {
                VarSymbol superSym =
                        new VarSymbol(FINAL | HASINIT, names._super,
                                ct.supertype_field, sym);
                superSym.pos = Position.FIRSTPOS;
                env.info.scope.enter(superSym);
            }
        }
    }

    private final class RecordPhase extends AbstractMembersPhase {

        public RecordPhase() {
            super(CompletionCause.RECORD_PHASE, new MembersPhase());
        }

        @Override
        protected void runPhase(Env<AttrContext> env) {
            JCClassDecl tree = env.enclClass;
            ClassSymbol sym = tree.sym;
            if ((sym.flags_field & RECORD) != 0) {
                List<JCVariableDecl> fields = TreeInfo.recordFields(tree);
                memberEnter.memberEnter(fields, env);
                for (JCVariableDecl field : fields) {
                    sym.getRecordComponent(field, true,
                            field.mods.annotations.isEmpty() ?
                                    List.nil() :
                                    new TreeCopier<JCTree>(make.at(field.pos)).copy(field.mods.annotations));
                }

                enterThisAndSuper(sym, env);

                // lets enter all constructors
                for (JCTree def : tree.defs) {
                    if (TreeInfo.isConstructor(def)) {
                        memberEnter.memberEnter(def, env);
                    }
                }
            }
        }
    }

    /** Enter member fields and methods of a class
     */
    private final class MembersPhase extends AbstractMembersPhase {

        public MembersPhase() {
            super(CompletionCause.MEMBERS_PHASE, null);
        }

        @Override
        protected void runPhase(Env<AttrContext> env) {
            JCClassDecl tree = env.enclClass;
            ClassSymbol sym = tree.sym;
            ClassType ct = (ClassType)sym.type;

            JCTree defaultConstructor = null;

            // Add default constructor if needed.
            DefaultConstructorHelper helper = getDefaultConstructorHelper(env);
            if (helper != null) {
                chk.checkDefaultConstructor(sym, tree.pos());
                defaultConstructor = defaultConstructor(make.at(tree.pos), helper);
                tree.defs = tree.defs.prepend(defaultConstructor);
            }
            if (!sym.isRecord()) {
                enterThisAndSuper(sym, env);
            }

            if (!tree.typarams.isEmpty()) {
                for (JCTypeParameter tvar : tree.typarams) {
                    chk.checkNonCyclic(tvar, (TypeVar)tvar.type);
                }
            }

            finishClass(tree, defaultConstructor, env);

            if (allowTypeAnnos) {
                typeAnnotations.organizeTypeAnnotationsSignatures(env, (JCClassDecl)env.tree);
                typeAnnotations.validateTypeAnnotationsSignatures(env, (JCClassDecl)env.tree);
            }
        }

        DefaultConstructorHelper getDefaultConstructorHelper(Env<AttrContext> env) {
            JCClassDecl tree = env.enclClass;
            ClassSymbol sym = tree.sym;
            DefaultConstructorHelper helper = null;
            boolean isClassWithoutInit = (sym.flags() & INTERFACE) == 0 && !TreeInfo.hasConstructors(tree.defs);
            boolean isRecord = sym.isRecord();
            if (isClassWithoutInit && !isRecord) {
                helper = new BasicConstructorHelper(sym);
                if (sym.name.isEmpty()) {
                    JCNewClass nc = (JCNewClass)env.next.tree;
                    if (nc.constructor != null) {
                        if (nc.constructor.kind != ERR) {
                            helper = new AnonClassConstructorHelper(sym, (MethodSymbol)nc.constructor, nc.encl);
                        } else {
                            helper = null;
                        }
                    }
                }
            }
            if (isRecord) {
                JCMethodDecl canonicalInit = null;
                if (isClassWithoutInit || (canonicalInit = getCanonicalConstructorDecl(env.enclClass)) == null) {
                    helper = new RecordConstructorHelper(sym, TreeInfo.recordFields(tree));
                }
                if (canonicalInit != null) {
                    canonicalInit.sym.flags_field |= Flags.RECORD;
                }
            }
            return helper;
        }

        /** Enter members for a class.
         */
        void finishClass(JCClassDecl tree, JCTree defaultConstructor, Env<AttrContext> env) {
            if ((tree.mods.flags & Flags.ENUM) != 0 &&
                !tree.sym.type.hasTag(ERROR) &&
                (types.supertype(tree.sym.type).tsym.flags() & Flags.ENUM) == 0) {
                addEnumMembers(tree, env);
            }
            boolean isRecord = (tree.sym.flags_field & RECORD) != 0;
            List<JCTree> alreadyEntered = null;
            if (isRecord) {
                alreadyEntered = List.convert(JCTree.class, TreeInfo.recordFields(tree));
                alreadyEntered = alreadyEntered.prependList(tree.defs.stream()
                        .filter(t -> TreeInfo.isConstructor(t) && t != defaultConstructor).collect(List.collector()));
            }
            List<JCTree> defsToEnter = isRecord ?
                    tree.defs.diff(alreadyEntered) : tree.defs;
            memberEnter.memberEnter(defsToEnter, env);
            if (isRecord) {
                addRecordMembersIfNeeded(tree, env);
            }
            if (tree.sym.isAnnotationType()) {
                Assert.check(tree.sym.isCompleted());
                tree.sym.setAnnotationTypeMetadata(new AnnotationTypeMetadata(tree.sym, annotate.annotationTypeSourceCompleter()));
            }
        }

        private void addAccessor(JCVariableDecl tree, Env<AttrContext> env) {
            MethodSymbol implSym = lookupMethod(env.enclClass.sym, tree.sym.name, List.nil());
            RecordComponent rec = ((ClassSymbol) tree.sym.owner).getRecordComponent(tree.sym);
            if (implSym == null || (implSym.flags_field & GENERATED_MEMBER) != 0) {
                /* here we are pushing the annotations present in the corresponding field down to the accessor
                 * it could be that some of those annotations are not applicable to the accessor, they will be striped
                 * away later at Check::validateAnnotation
                 */
                TreeCopier<JCTree> tc = new TreeCopier<JCTree>(make.at(tree.pos));
                List<JCAnnotation> originalAnnos = rec.getOriginalAnnos().isEmpty() ?
                        rec.getOriginalAnnos() :
                        tc.copy(rec.getOriginalAnnos());
                JCVariableDecl recordField = TreeInfo.recordFields((JCClassDecl) env.tree).stream().filter(rf -> rf.name == tree.name).findAny().get();
                JCMethodDecl getter = make.at(tree.pos).
                        MethodDef(
                                make.Modifiers(PUBLIC | Flags.GENERATED_MEMBER, originalAnnos),
                          tree.sym.name,
                          /* we need to special case for the case when the user declared the type as an ident
                           * if we don't do that then we can have issues if type annotations are applied to the
                           * return type: javac issues an error if a type annotation is applied to java.lang.String
                           * but applying a type annotation to String is kosher
                           */
                          tc.copy(recordField.vartype),
                          List.nil(),
                          List.nil(),
                          List.nil(), // thrown
                          null,
                          null);
                memberEnter.memberEnter(getter, env);
                rec.accessor = getter.sym;
                rec.accessorMeth = getter;
            } else if (implSym != null) {
                rec.accessor = implSym;
            }
        }

        /** Add the implicit members for an enum type
         *  to the symbol table.
         */
        private void addEnumMembers(JCClassDecl tree, Env<AttrContext> env) {
            JCExpression valuesType = make.Type(new ArrayType(tree.sym.type, syms.arrayClass));

            JCMethodDecl values = make.
                MethodDef(make.Modifiers(Flags.PUBLIC|Flags.STATIC),
                          names.values,
                          valuesType,
                          List.nil(),
                          List.nil(),
                          List.nil(),
                          null,
                          null);
            memberEnter.memberEnter(values, env);

            JCMethodDecl valueOf = make.
                MethodDef(make.Modifiers(Flags.PUBLIC|Flags.STATIC),
                          names.valueOf,
                          make.Type(tree.sym.type),
                          List.nil(),
                          List.of(make.VarDef(make.Modifiers(Flags.PARAMETER |
                                                             Flags.MANDATED),
                                                names.fromString("name"),
                                                make.Type(syms.stringType), null)),
                          List.nil(),
                          null,
                          null);
            memberEnter.memberEnter(valueOf, env);
        }

        JCMethodDecl getCanonicalConstructorDecl(JCClassDecl tree) {
            // let's check if there is a constructor with exactly the same arguments as the record components
            List<Type> recordComponentErasedTypes = types.erasure(TreeInfo.recordFields(tree).map(vd -> vd.sym.type));
            JCMethodDecl canonicalDecl = null;
            for (JCTree def : tree.defs) {
                if (TreeInfo.isConstructor(def)) {
                    JCMethodDecl mdecl = (JCMethodDecl)def;
                    if (types.isSameTypes(types.erasure(mdecl.params.stream().map(v -> v.sym.type).collect(List.collector())), recordComponentErasedTypes)) {
                        canonicalDecl = mdecl;
                        break;
                    }
                }
            }
            return canonicalDecl;
        }

        /** Add the implicit members for a record
         *  to the symbol table.
         */
        private void addRecordMembersIfNeeded(JCClassDecl tree, Env<AttrContext> env) {
            if (lookupMethod(tree.sym, names.toString, List.nil()) == null) {
                JCMethodDecl toString = make.
                    MethodDef(make.Modifiers(Flags.PUBLIC | Flags.RECORD | Flags.FINAL | Flags.GENERATED_MEMBER),
                              names.toString,
                              make.Type(syms.stringType),
                              List.nil(),
                              List.nil(),
                              List.nil(),
                              null,
                              null);
                memberEnter.memberEnter(toString, env);
            }

            if (lookupMethod(tree.sym, names.hashCode, List.nil()) == null) {
                JCMethodDecl hashCode = make.
                    MethodDef(make.Modifiers(Flags.PUBLIC | Flags.RECORD | Flags.FINAL | Flags.GENERATED_MEMBER),
                              names.hashCode,
                              make.Type(syms.intType),
                              List.nil(),
                              List.nil(),
                              List.nil(),
                              null,
                              null);
                memberEnter.memberEnter(hashCode, env);
            }

            if (lookupMethod(tree.sym, names.equals, List.of(syms.objectType)) == null) {
                JCMethodDecl equals = make.
                    MethodDef(make.Modifiers(Flags.PUBLIC | Flags.RECORD | Flags.FINAL | Flags.GENERATED_MEMBER),
                              names.equals,
                              make.Type(syms.booleanType),
                              List.nil(),
                              List.of(make.VarDef(make.Modifiers(Flags.PARAMETER),
                                                names.fromString("o"),
                                                make.Type(syms.objectType), null)),
                              List.nil(),
                              null,
                              null);
                memberEnter.memberEnter(equals, env);
            }

            // fields can't be varargs, lets remove the flag
            List<JCVariableDecl> recordFields = TreeInfo.recordFields(tree);
            for (JCVariableDecl field: recordFields) {
                field.mods.flags &= ~Flags.VARARGS;
                field.sym.flags_field &= ~Flags.VARARGS;
            }
            // now lets add the accessors
            recordFields.stream()
                    .filter(vd -> (lookupMethod(syms.objectType.tsym, vd.name, List.nil()) == null))
                    .forEach(vd -> addAccessor(vd, env));
        }
    }

    private MethodSymbol lookupMethod(TypeSymbol tsym, Name name, List<Type> argtypes) {
        for (Symbol s : tsym.members().getSymbolsByName(name, s -> s.kind == MTH)) {
            if (types.isSameTypes(s.type.getParameterTypes(), argtypes)) {
                return (MethodSymbol) s;
            }
        }
        return null;
    }

/* ***************************************************************************
 * tree building
 ****************************************************************************/

    interface DefaultConstructorHelper {
       Type constructorType();
       MethodSymbol constructorSymbol();
       Type enclosingType();
       TypeSymbol owner();
       List<Name> superArgs();
       default JCMethodDecl finalAdjustment(JCMethodDecl md) { return md; }
    }

    class BasicConstructorHelper implements DefaultConstructorHelper {

        TypeSymbol owner;
        Type constructorType;
        MethodSymbol constructorSymbol;

        BasicConstructorHelper(TypeSymbol owner) {
            this.owner = owner;
        }

        @Override
        public Type constructorType() {
            if (constructorType == null) {
                constructorType = new MethodType(List.nil(), syms.voidType, List.nil(), syms.methodClass);
            }
            return constructorType;
        }

        @Override
        public MethodSymbol constructorSymbol() {
            if (constructorSymbol == null) {
                long flags;
                if ((owner().flags() & ENUM) != 0 &&
                    (types.supertype(owner().type).tsym == syms.enumSym)) {
                    // constructors of true enums are private
                    flags = PRIVATE | GENERATEDCONSTR;
                } else {
                    flags = (owner().flags() & AccessFlags) | GENERATEDCONSTR;
                }
                constructorSymbol = new MethodSymbol(flags, names.init,
                    constructorType(), owner());
            }
            return constructorSymbol;
        }

        @Override
        public Type enclosingType() {
            return Type.noType;
    }

        @Override
        public TypeSymbol owner() {
            return owner;
        }

        @Override
        public List<Name> superArgs() {
            return List.nil();
            }
    }

    class AnonClassConstructorHelper extends BasicConstructorHelper {

        MethodSymbol constr;
        Type encl;
        boolean based = false;

        AnonClassConstructorHelper(TypeSymbol owner, MethodSymbol constr, JCExpression encl) {
            super(owner);
            this.constr = constr;
            this.encl = encl != null ? encl.type : Type.noType;
        }

        @Override
        public Type constructorType() {
            if (constructorType == null) {
                Type ctype = types.memberType(owner.type, constr);
                if (!enclosingType().hasTag(NONE)) {
                    ctype = types.createMethodTypeWithParameters(ctype, ctype.getParameterTypes().prepend(enclosingType()));
                    based = true;
                }
                constructorType = ctype;
            }
            return constructorType;
        }

        @Override
        public MethodSymbol constructorSymbol() {
            MethodSymbol csym = super.constructorSymbol();
            csym.flags_field |= ANONCONSTR | (constr.flags() & VARARGS);
            csym.flags_field |= based ? ANONCONSTR_BASED : 0;
            ListBuffer<VarSymbol> params = new ListBuffer<>();
            List<Type> argtypes = constructorType().getParameterTypes();
            if (!enclosingType().hasTag(NONE)) {
                argtypes = argtypes.tail;
                params = params.prepend(new VarSymbol(PARAMETER, make.paramName(0), enclosingType(), csym));
            }
            if (constr.params != null) {
                for (VarSymbol p : constr.params) {
                    params.add(new VarSymbol(PARAMETER | p.flags(), p.name, argtypes.head, csym));
                    argtypes = argtypes.tail;
                }
            }
            csym.params = params.toList();
            return csym;
        }

        @Override
        public Type enclosingType() {
            return encl;
        }

        @Override
        public List<Name> superArgs() {
            List<JCVariableDecl> params = make.Params(constructorType().getParameterTypes(), constructorSymbol());
            if (!enclosingType().hasTag(NONE)) {
                params = params.tail;
            }
            return params.map(vd -> vd.name);
        }
    }

    class RecordConstructorHelper extends BasicConstructorHelper {
        boolean lastIsVarargs;
        List<JCVariableDecl> recordFieldDecls;

        RecordConstructorHelper(ClassSymbol owner, List<JCVariableDecl> recordFieldDecls) {
            super(owner);
            this.recordFieldDecls = recordFieldDecls;
            this.lastIsVarargs = owner.getRecordComponents().stream().anyMatch(rc -> rc.isVarargs());
        }

        @Override
        public Type constructorType() {
            if (constructorType == null) {
                ListBuffer<Type> argtypes = new ListBuffer<>();
                JCVariableDecl lastField = recordFieldDecls.last();
                for (JCVariableDecl field : recordFieldDecls) {
                    argtypes.add(field == lastField && lastIsVarargs ? types.elemtype(field.sym.type) : field.sym.type);
                }

                constructorType = new MethodType(argtypes.toList(), syms.voidType, List.nil(), syms.methodClass);
            }
            return constructorType;
        }

        @Override
        public MethodSymbol constructorSymbol() {
            MethodSymbol csym = super.constructorSymbol();
            /* if we have to generate a default constructor for records we will treat it as the compact one
             * to trigger field initialization later on
             */
            csym.flags_field |= Flags.COMPACT_RECORD_CONSTRUCTOR | GENERATEDCONSTR;
            ListBuffer<VarSymbol> params = new ListBuffer<>();
            JCVariableDecl lastField = recordFieldDecls.last();
            for (JCVariableDecl field : recordFieldDecls) {
                params.add(new VarSymbol(
                        GENERATED_MEMBER | PARAMETER | RECORD | (field == lastField && lastIsVarargs ? Flags.VARARGS : 0),
                        field.name, field.sym.type, csym));
            }
            csym.params = params.toList();
            csym.flags_field |= RECORD;
            return csym;
        }

        @Override
        public JCMethodDecl finalAdjustment(JCMethodDecl md) {
            List<JCVariableDecl> tmpRecordFieldDecls = recordFieldDecls;
            for (JCVariableDecl arg : md.params) {
                /* at this point we are passing all the annotations in the field to the corresponding
                 * parameter in the constructor.
                 */
                RecordComponent rc = ((ClassSymbol) owner).getRecordComponent(arg.sym);
                TreeCopier<JCTree> tc = new TreeCopier<JCTree>(make.at(arg.pos));
                arg.mods.annotations = rc.getOriginalAnnos().isEmpty() ?
                        List.nil() :
                        tc.copy(rc.getOriginalAnnos());
                arg.vartype = tc.copy(tmpRecordFieldDecls.head.vartype);
                tmpRecordFieldDecls = tmpRecordFieldDecls.tail;
            }
            return md;
        }
    }

    JCTree defaultConstructor(TreeMaker make, DefaultConstructorHelper helper) {
        Type initType = helper.constructorType();
        MethodSymbol initSym = helper.constructorSymbol();
        ListBuffer<JCStatement> stats = new ListBuffer<>();
        if (helper.owner().type != syms.objectType) {
            JCExpression meth;
            if (!helper.enclosingType().hasTag(NONE)) {
                meth = make.Select(make.Ident(initSym.params.head), names._super);
            } else {
                meth = make.Ident(names._super);
            }
            List<JCExpression> typeargs = initType.getTypeArguments().nonEmpty() ?
                    make.Types(initType.getTypeArguments()) : null;
            JCStatement superCall = make.Exec(make.Apply(typeargs, meth, helper.superArgs().map(make::Ident)));
            stats.add(superCall);
        }
        JCMethodDecl result = make.MethodDef(initSym, make.Block(0, stats.toList()));
        return helper.finalAdjustment(result);
    }

    /**
     * Mark sym deprecated if annotations contain @Deprecated annotation.
     */
    public void markDeprecated(Symbol sym, List<JCAnnotation> annotations, Env<AttrContext> env) {
        // In general, we cannot fully process annotations yet,  but we
        // can attribute the annotation types and then check to see if the
        // @Deprecated annotation is present.
        attr.attribAnnotationTypes(annotations, env);
        handleDeprecatedAnnotations(annotations, sym);
    }

    /**
     * If a list of annotations contains a reference to java.lang.Deprecated,
     * set the DEPRECATED flag.
     * If the annotation is marked forRemoval=true, also set DEPRECATED_REMOVAL.
     **/
    private void handleDeprecatedAnnotations(List<JCAnnotation> annotations, Symbol sym) {
        for (List<JCAnnotation> al = annotations; !al.isEmpty(); al = al.tail) {
            JCAnnotation a = al.head;
            if (a.annotationType.type == syms.deprecatedType) {
                sym.flags_field |= (Flags.DEPRECATED | Flags.DEPRECATED_ANNOTATION);
                setFlagIfAttributeTrue(a, sym, names.forRemoval, DEPRECATED_REMOVAL);
            } else if (a.annotationType.type == syms.previewFeatureType) {
                sym.flags_field |= Flags.PREVIEW_API;
                setFlagIfAttributeTrue(a, sym, names.reflective, Flags.PREVIEW_REFLECTIVE);
            }
        }
    }
    //where:
        private void setFlagIfAttributeTrue(JCAnnotation a, Symbol sym, Name attribute, long flag) {
            a.args.stream()
                    .filter(e -> e.hasTag(ASSIGN))
                    .map(e -> (JCAssign) e)
                    .filter(assign -> TreeInfo.name(assign.lhs) == attribute)
                    .findFirst()
                    .ifPresent(assign -> {
                        JCExpression rhs = TreeInfo.skipParens(assign.rhs);
                        if (rhs.hasTag(LITERAL)
                                && Boolean.TRUE.equals(((JCLiteral) rhs).getValue())) {
                            sym.flags_field |= flag;
                        }
                    });
        }
}
