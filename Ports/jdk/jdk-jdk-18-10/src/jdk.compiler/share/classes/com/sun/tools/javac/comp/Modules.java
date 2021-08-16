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


package com.sun.tools.javac.comp;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.SourceVersion;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardLocation;

import com.sun.source.tree.ModuleTree.ModuleKind;
import com.sun.tools.javac.code.ClassFinder;
import com.sun.tools.javac.code.DeferredLintHandler;
import com.sun.tools.javac.code.Directive;
import com.sun.tools.javac.code.Directive.ExportsDirective;
import com.sun.tools.javac.code.Directive.ExportsFlag;
import com.sun.tools.javac.code.Directive.OpensDirective;
import com.sun.tools.javac.code.Directive.OpensFlag;
import com.sun.tools.javac.code.Directive.RequiresDirective;
import com.sun.tools.javac.code.Directive.RequiresFlag;
import com.sun.tools.javac.code.Directive.UsesDirective;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Flags.Flag;
import com.sun.tools.javac.code.Lint.LintCategory;
import com.sun.tools.javac.code.ModuleFinder;
import com.sun.tools.javac.code.Source;
import com.sun.tools.javac.code.Source.Feature;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.Completer;
import com.sun.tools.javac.code.Symbol.CompletionFailure;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.ModuleFlags;
import com.sun.tools.javac.code.Symbol.ModuleSymbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.jvm.ClassWriter;
import com.sun.tools.javac.jvm.JNIWriter;
import com.sun.tools.javac.jvm.Target;
import com.sun.tools.javac.main.Option;
import com.sun.tools.javac.resources.CompilerProperties.Errors;
import com.sun.tools.javac.resources.CompilerProperties.Warnings;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCDirective;
import com.sun.tools.javac.tree.JCTree.JCExports;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCModuleDecl;
import com.sun.tools.javac.tree.JCTree.JCOpens;
import com.sun.tools.javac.tree.JCTree.JCProvides;
import com.sun.tools.javac.tree.JCTree.JCRequires;
import com.sun.tools.javac.tree.JCTree.JCUses;
import com.sun.tools.javac.tree.JCTree.Tag;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Options;

import static com.sun.tools.javac.code.Flags.ABSTRACT;
import static com.sun.tools.javac.code.Flags.ENUM;
import static com.sun.tools.javac.code.Flags.PUBLIC;
import static com.sun.tools.javac.code.Flags.UNATTRIBUTED;

import com.sun.tools.javac.code.Kinds;

import static com.sun.tools.javac.code.Kinds.Kind.ERR;
import static com.sun.tools.javac.code.Kinds.Kind.MDL;
import static com.sun.tools.javac.code.Kinds.Kind.MTH;

import com.sun.tools.javac.code.Symbol.ModuleResolutionFlags;

import static com.sun.tools.javac.code.TypeTag.CLASS;

/**
 *  TODO: fill in
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Modules extends JCTree.Visitor {
    private static final String ALL_SYSTEM = "ALL-SYSTEM";
    private static final String ALL_MODULE_PATH = "ALL-MODULE-PATH";

    private final Log log;
    private final Names names;
    private final Symtab syms;
    private final Attr attr;
    private final Check chk;
    private final DeferredLintHandler deferredLintHandler;
    private final TypeEnvs typeEnvs;
    private final Types types;
    private final JavaFileManager fileManager;
    private final ModuleFinder moduleFinder;
    private final Source source;
    private final Target target;
    private final boolean allowModules;
    private final boolean allowAccessIntoSystem;

    public final boolean multiModuleMode;

    private final Name java_se;
    private final Name java_;

    ModuleSymbol defaultModule;

    private final String addExportsOpt;
    private Map<ModuleSymbol, Set<ExportsDirective>> addExports;
    private final String addReadsOpt;
    private Map<ModuleSymbol, Set<RequiresDirective>> addReads;
    private final String addModsOpt;
    private final Set<String> extraAddMods = new HashSet<>();
    private final String limitModsOpt;
    private final Set<String> extraLimitMods = new HashSet<>();
    private final String moduleVersionOpt;

    private final boolean lintOptions;

    private Set<ModuleSymbol> rootModules = null;
    private final Set<ModuleSymbol> warnedMissing = new HashSet<>();

    public PackageNameFinder findPackageInFile;

    public static Modules instance(Context context) {
        Modules instance = context.get(Modules.class);
        if (instance == null)
            instance = new Modules(context);
        return instance;
    }

    protected Modules(Context context) {
        context.put(Modules.class, this);
        log = Log.instance(context);
        names = Names.instance(context);
        syms = Symtab.instance(context);
        attr = Attr.instance(context);
        chk = Check.instance(context);
        deferredLintHandler = DeferredLintHandler.instance(context);
        typeEnvs = TypeEnvs.instance(context);
        moduleFinder = ModuleFinder.instance(context);
        types = Types.instance(context);
        fileManager = context.get(JavaFileManager.class);
        source = Source.instance(context);
        target = Target.instance(context);
        allowModules = Feature.MODULES.allowedInSource(source);
        Options options = Options.instance(context);

        allowAccessIntoSystem = options.isUnset(Option.RELEASE);
        lintOptions = options.isUnset(Option.XLINT_CUSTOM, "-" + LintCategory.OPTIONS.option);

        multiModuleMode = fileManager.hasLocation(StandardLocation.MODULE_SOURCE_PATH);
        ClassWriter classWriter = ClassWriter.instance(context);
        classWriter.multiModuleMode = multiModuleMode;
        JNIWriter jniWriter = JNIWriter.instance(context);
        jniWriter.multiModuleMode = multiModuleMode;

        java_se = names.fromString("java.se");
        java_ = names.fromString("java.");

        addExportsOpt = options.get(Option.ADD_EXPORTS);
        addReadsOpt = options.get(Option.ADD_READS);
        addModsOpt = options.get(Option.ADD_MODULES);
        limitModsOpt = options.get(Option.LIMIT_MODULES);
        moduleVersionOpt = options.get(Option.MODULE_VERSION);
    }

    int depth = -1;

    public void addExtraAddModules(String... extras) {
        extraAddMods.addAll(Arrays.asList(extras));
    }

    boolean inInitModules;
    public void initModules(List<JCCompilationUnit> trees) {
        Assert.check(!inInitModules);
        try {
            inInitModules = true;
            Assert.checkNull(rootModules);
            enter(trees, modules -> {
                Assert.checkNull(rootModules);
                Assert.checkNull(allModules);
                this.rootModules = modules;
                setupAllModules(); //initialize the module graph
                Assert.checkNonNull(allModules);
                inInitModules = false;
            }, null);
        } finally {
            inInitModules = false;
        }
    }

    public boolean enter(List<JCCompilationUnit> trees, ClassSymbol c) {
        Assert.check(rootModules != null || inInitModules || !allowModules);
        return enter(trees, modules -> {}, c);
    }

    private boolean enter(List<JCCompilationUnit> trees, Consumer<Set<ModuleSymbol>> init, ClassSymbol c) {
        if (!allowModules) {
            for (JCCompilationUnit tree: trees) {
                tree.modle = syms.noModule;
            }
            defaultModule = syms.noModule;
            return true;
        }

        int startErrors = log.nerrors;

        depth++;
        try {
            // scan trees for module defs
            Set<ModuleSymbol> roots = enterModules(trees, c);

            setCompilationUnitModules(trees, roots, c);

            init.accept(roots);

            for (ModuleSymbol msym: roots) {
                msym.complete();
            }
        } catch (CompletionFailure ex) {
            chk.completionError(null, ex);
        } finally {
            depth--;
        }

        return (log.nerrors == startErrors);
    }

    public Completer getCompleter() {
        return mainCompleter;
    }

    public ModuleSymbol getDefaultModule() {
        return defaultModule;
    }

    public boolean modulesInitialized() {
        return allModules != null;
    }

    private Set<ModuleSymbol> enterModules(List<JCCompilationUnit> trees, ClassSymbol c) {
        Set<ModuleSymbol> modules = new LinkedHashSet<>();
        for (JCCompilationUnit tree : trees) {
            JavaFileObject prev = log.useSource(tree.sourcefile);
            try {
                enterModule(tree, c, modules);
            } finally {
                log.useSource(prev);
            }
        }
        return modules;
    }


    private void enterModule(JCCompilationUnit toplevel, ClassSymbol c, Set<ModuleSymbol> modules) {
        boolean isModuleInfo = toplevel.sourcefile.isNameCompatible("module-info", Kind.SOURCE);
        boolean isModuleDecl = toplevel.getModuleDecl() != null;
        if (isModuleDecl) {
            JCModuleDecl decl = toplevel.getModuleDecl();
            if (!isModuleInfo) {
                log.error(decl.pos(), Errors.ModuleDeclSbInModuleInfoJava);
            }
            Name name = TreeInfo.fullName(decl.qualId);
            ModuleSymbol sym;
            if (c != null) {
                sym = (ModuleSymbol) c.owner;
                Assert.checkNonNull(sym.name);
                Name treeName = TreeInfo.fullName(decl.qualId);
                if (sym.name != treeName) {
                    log.error(decl.pos(), Errors.ModuleNameMismatch(name, sym.name));
                }
            } else {
                sym = syms.enterModule(name);
                if (sym.module_info.sourcefile != null && sym.module_info.sourcefile != toplevel.sourcefile) {
                    log.error(decl.pos(), Errors.DuplicateModule(sym));
                    return;
                }
            }
            sym.completer = getSourceCompleter(toplevel);
            sym.module_info.sourcefile = toplevel.sourcefile;
            decl.sym = sym;

            if (multiModuleMode || modules.isEmpty()) {
                modules.add(sym);
            } else {
                log.error(toplevel.pos(), Errors.TooManyModules);
            }

            Env<AttrContext> provisionalEnv = new Env<>(decl, null);

            provisionalEnv.toplevel = toplevel;
            typeEnvs.put(sym, provisionalEnv);
        } else if (isModuleInfo) {
            if (multiModuleMode) {
                JCTree tree = toplevel.defs.isEmpty() ? toplevel : toplevel.defs.head;
                log.error(tree.pos(), Errors.ExpectedModule);
            }
        }
    }

    private void setCompilationUnitModules(List<JCCompilationUnit> trees, Set<ModuleSymbol> rootModules, ClassSymbol c) {
        // update the module for each compilation unit
        if (multiModuleMode) {
            boolean patchesAutomaticModules = false;
            for (JCCompilationUnit tree: trees) {
                if (tree.defs.isEmpty()) {
                    tree.modle = syms.unnamedModule;
                    continue;
                }

                JavaFileObject prev = log.useSource(tree.sourcefile);
                try {
                    Location msplocn = getModuleLocation(tree);
                    Location plocn = fileManager.hasLocation(StandardLocation.PATCH_MODULE_PATH) ?
                            fileManager.getLocationForModule(StandardLocation.PATCH_MODULE_PATH,
                                                             tree.sourcefile) :
                            null;

                    if (plocn != null) {
                        Name name = names.fromString(fileManager.inferModuleName(plocn));
                        ModuleSymbol msym = moduleFinder.findModule(name);
                        tree.modle = msym;
                        rootModules.add(msym);
                        patchesAutomaticModules |= (msym.flags_field & Flags.AUTOMATIC_MODULE) != 0;

                        if (msplocn != null) {
                            Name mspname = names.fromString(fileManager.inferModuleName(msplocn));
                            if (name != mspname) {
                                log.error(tree.pos(), Errors.FilePatchedAndMsp(name, mspname));
                            }
                        }
                    } else if (msplocn != null) {
                        if (tree.getModuleDecl() != null) {
                            JavaFileObject canonical =
                                    fileManager.getJavaFileForInput(msplocn, "module-info", Kind.SOURCE);
                            if (canonical == null || !fileManager.isSameFile(canonical, tree.sourcefile)) {
                                log.error(tree.pos(), Errors.ModuleNotFoundOnModuleSourcePath);
                            }
                        }
                        Name name = names.fromString(fileManager.inferModuleName(msplocn));
                        ModuleSymbol msym;
                        JCModuleDecl decl = tree.getModuleDecl();
                        if (decl != null) {
                            msym = decl.sym;
                            if (msym.name != name) {
                                log.error(decl.qualId, Errors.ModuleNameMismatch(msym.name, name));
                            }
                        } else {
                            if (tree.getPackage() == null) {
                                log.error(tree.pos(), Errors.UnnamedPkgNotAllowedNamedModules);
                            }
                            msym = syms.enterModule(name);
                        }
                        if (msym.sourceLocation == null) {
                            msym.sourceLocation = msplocn;
                            if (fileManager.hasLocation(StandardLocation.PATCH_MODULE_PATH)) {
                                msym.patchLocation = fileManager.getLocationForModule(
                                        StandardLocation.PATCH_MODULE_PATH, msym.name.toString());
                            }
                            if (fileManager.hasLocation(StandardLocation.CLASS_OUTPUT)) {
                                Location outputLocn = fileManager.getLocationForModule(
                                        StandardLocation.CLASS_OUTPUT, msym.name.toString());
                                if (msym.patchLocation == null) {
                                    msym.classLocation = outputLocn;
                                } else {
                                    msym.patchOutputLocation = outputLocn;
                                }
                            }
                        }
                        tree.modle = msym;
                        rootModules.add(msym);
                    } else if (c != null && c.packge().modle == syms.unnamedModule) {
                        tree.modle = syms.unnamedModule;
                    } else {
                        if (tree.getModuleDecl() != null) {
                            log.error(tree.pos(), Errors.ModuleNotFoundOnModuleSourcePath);
                        } else {
                            log.error(tree.pos(), Errors.NotInModuleOnModuleSourcePath);
                        }
                        tree.modle = syms.errModule;
                    }
                } catch (IOException e) {
                    throw new Error(e); // FIXME
                } finally {
                    log.useSource(prev);
                }
            }
            if (!patchesAutomaticModules) {
                checkNoAllModulePath();
            }
            if (syms.unnamedModule.sourceLocation == null) {
                syms.unnamedModule.completer = getUnnamedModuleCompleter();
                syms.unnamedModule.sourceLocation = StandardLocation.SOURCE_PATH;
                syms.unnamedModule.classLocation = StandardLocation.CLASS_PATH;
            }
            defaultModule = syms.unnamedModule;
        } else {
            ModuleSymbol module = null;
            if (defaultModule == null) {
                String moduleOverride = singleModuleOverride(trees);
                switch (rootModules.size()) {
                    case 0:
                        try {
                            defaultModule = moduleFinder.findSingleModule();
                        } catch (CompletionFailure cf) {
                            chk.completionError(null, cf);
                            defaultModule = syms.unnamedModule;
                        }
                        if (defaultModule == syms.unnamedModule) {
                            if (moduleOverride != null) {
                                defaultModule = moduleFinder.findModule(names.fromString(moduleOverride));
                                defaultModule.patchOutputLocation = StandardLocation.CLASS_OUTPUT;
                                if ((defaultModule.flags_field & Flags.AUTOMATIC_MODULE) == 0) {
                                    checkNoAllModulePath();
                                }
                            } else {
                                // Question: why not do findAllModules and initVisiblePackages here?
                                // i.e. body of unnamedModuleCompleter
                                defaultModule.completer = getUnnamedModuleCompleter();
                                defaultModule.sourceLocation = StandardLocation.SOURCE_PATH;
                                defaultModule.classLocation = StandardLocation.CLASS_PATH;
                            }
                        } else {
                            checkNoAllModulePath();
                            defaultModule.complete();
                            // Question: why not do completeModule here?
                            defaultModule.completer = sym -> completeModule((ModuleSymbol) sym);
                            defaultModule.sourceLocation = StandardLocation.SOURCE_PATH;
                        }
                        rootModules.add(defaultModule);
                        break;
                    case 1:
                        checkNoAllModulePath();
                        defaultModule = rootModules.iterator().next();
                        defaultModule.sourceLocation = StandardLocation.SOURCE_PATH;
                        if (fileManager.hasLocation(StandardLocation.PATCH_MODULE_PATH)) {
                            try {
                                defaultModule.patchLocation = fileManager.getLocationForModule(
                                        StandardLocation.PATCH_MODULE_PATH, defaultModule.name.toString());
                            } catch (IOException ex) {
                                throw new Error(ex);
                            }
                        }
                        if (defaultModule.patchLocation == null) {
                            defaultModule.classLocation = StandardLocation.CLASS_OUTPUT;
                        } else {
                            defaultModule.patchOutputLocation = StandardLocation.CLASS_OUTPUT;
                        }
                        break;
                    default:
                        Assert.error("too many modules");
                }
            } else if (rootModules.size() == 1) {
                module = rootModules.iterator().next();
                module.complete();
                module.completer = sym -> completeModule((ModuleSymbol) sym);
            } else {
                Assert.check(rootModules.isEmpty());
                Assert.checkNonNull(c);
                module = c.packge().modle;
                rootModules.add(module);
            }

            if (defaultModule != syms.unnamedModule) {
                syms.unnamedModule.completer = getUnnamedModuleCompleter();
                syms.unnamedModule.classLocation = StandardLocation.CLASS_PATH;
            }

            if (module == null) {
                module = defaultModule;
            }

            for (JCCompilationUnit tree : trees) {
                if (defaultModule != syms.unnamedModule
                        && defaultModule.sourceLocation == StandardLocation.SOURCE_PATH
                        && fileManager.hasLocation(StandardLocation.SOURCE_PATH)) {
                    checkSourceLocation(tree, module);
                }
                tree.modle = module;
            }
        }
    }

    private void checkSourceLocation(JCCompilationUnit tree, ModuleSymbol msym) {
        try {
            JavaFileObject fo = tree.sourcefile;
            if (fileManager.contains(msym.sourceLocation, fo)) {
                return;
            }
            if (msym.patchLocation != null && fileManager.contains(msym.patchLocation, fo)) {
                return;
            }
            if (fileManager.hasLocation(StandardLocation.SOURCE_OUTPUT)) {
                if (fileManager.contains(StandardLocation.SOURCE_OUTPUT, fo)) {
                    return;
                }
            } else {
                if (fileManager.contains(StandardLocation.CLASS_OUTPUT, fo)) {
                    return;
                }
            }
        } catch (IOException e) {
            throw new Error(e);
        }

        JavaFileObject prev = log.useSource(tree.sourcefile);
        try {
            log.error(tree.pos(), Errors.FileSbOnSourceOrPatchPathForModule);
        } finally {
            log.useSource(prev);
        }
    }

    private String singleModuleOverride(List<JCCompilationUnit> trees) {
        if (!fileManager.hasLocation(StandardLocation.PATCH_MODULE_PATH)) {
            return null;
        }

        Set<String> override = new LinkedHashSet<>();
        for (JCCompilationUnit tree : trees) {
            JavaFileObject fo = tree.sourcefile;

            try {
                Location loc =
                        fileManager.getLocationForModule(StandardLocation.PATCH_MODULE_PATH, fo);

                if (loc != null) {
                    override.add(fileManager.inferModuleName(loc));
                }
            } catch (IOException ex) {
                throw new Error(ex);
            }
        }

        switch (override.size()) {
            case 0: return null;
            case 1: return override.iterator().next();
            default:
                log.error(Errors.TooManyPatchedModules(override));
                return null;
        }
    }

    /**
     * Determine the location for the module on the module source path
     * or source output directory which contains a given CompilationUnit.
     * If the source output directory is unset, the class output directory
     * will be checked instead.
     * {@code null} is returned if no such module can be found.
     * @param tree the compilation unit tree
     * @return the location for the enclosing module
     * @throws IOException if there is a problem while searching for the module.
     */
    private Location getModuleLocation(JCCompilationUnit tree) throws IOException {
        JavaFileObject fo = tree.sourcefile;

        Location loc =
                fileManager.getLocationForModule(StandardLocation.MODULE_SOURCE_PATH, fo);
        if (loc == null) {
            Location sourceOutput = fileManager.hasLocation(StandardLocation.SOURCE_OUTPUT) ?
                    StandardLocation.SOURCE_OUTPUT : StandardLocation.CLASS_OUTPUT;
            loc =
                fileManager.getLocationForModule(sourceOutput, fo);
        }
        return loc;
    }

    private void checkNoAllModulePath() {
        if (addModsOpt != null && Arrays.asList(addModsOpt.split(",")).contains(ALL_MODULE_PATH)) {
            log.error(Errors.AddmodsAllModulePathInvalid);
        }
    }

    private final Completer mainCompleter = new Completer() {
        @Override
        public void complete(Symbol sym) throws CompletionFailure {
            ModuleSymbol msym = moduleFinder.findModule((ModuleSymbol) sym);

            if (msym.kind == ERR) {
                //make sure the module is initialized:
                initErrModule(msym);
            } else if ((msym.flags_field & Flags.AUTOMATIC_MODULE) != 0) {
                setupAutomaticModule(msym);
            } else {
                try {
                    msym.module_info.complete();
                } catch (CompletionFailure cf) {
                    msym.kind = ERR;
                    //make sure the module is initialized:
                    initErrModule(msym);
                    completeModule(msym);
                    throw cf;
                }
            }

            // If module-info comes from a .java file, the underlying
            // call of classFinder.fillIn will have called through the
            // source completer, to Enter, and then to Modules.enter,
            // which will call completeModule.
            // But, if module-info comes from a .class file, the underlying
            // call of classFinder.fillIn will just call ClassReader to read
            // the .class file, and so we call completeModule here.
            if (msym.module_info.classfile == null || msym.module_info.classfile.getKind() == Kind.CLASS) {
                completeModule(msym);
            }
        }

        private void initErrModule(ModuleSymbol msym) {
            msym.directives = List.nil();
            msym.exports = List.nil();
            msym.provides = List.nil();
            msym.requires = List.nil();
            msym.uses = List.nil();
        }

        @Override
        public String toString() {
            return "mainCompleter";
        }
    };

    private void setupAutomaticModule(ModuleSymbol msym) throws CompletionFailure {
        try {
            ListBuffer<Directive> directives = new ListBuffer<>();
            ListBuffer<ExportsDirective> exports = new ListBuffer<>();
            Set<String> seenPackages = new HashSet<>();

            for (JavaFileObject clazz : fileManager.list(msym.classLocation, "", EnumSet.of(Kind.CLASS), true)) {
                String binName = fileManager.inferBinaryName(msym.classLocation, clazz);
                String pack = binName.lastIndexOf('.') != (-1) ? binName.substring(0, binName.lastIndexOf('.')) : ""; //unnamed package????
                if (seenPackages.add(pack)) {
                    ExportsDirective d = new ExportsDirective(syms.enterPackage(msym, names.fromString(pack)), null);
                    //TODO: opens?
                    directives.add(d);
                    exports.add(d);
                }
            }

            msym.exports = exports.toList();
            msym.provides = List.nil();
            msym.requires = List.nil();
            msym.uses = List.nil();
            msym.directives = directives.toList();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void completeAutomaticModule(ModuleSymbol msym) throws CompletionFailure {
        ListBuffer<Directive> directives = new ListBuffer<>();

        directives.addAll(msym.directives);

        ListBuffer<RequiresDirective> requires = new ListBuffer<>();

        for (ModuleSymbol ms : allModules()) {
            if (ms == syms.unnamedModule || ms == msym)
                continue;
            Set<RequiresFlag> flags = (ms.flags_field & Flags.AUTOMATIC_MODULE) != 0 ?
                    EnumSet.of(RequiresFlag.TRANSITIVE) : EnumSet.noneOf(RequiresFlag.class);
            RequiresDirective d = new RequiresDirective(ms, flags);
            directives.add(d);
            requires.add(d);
        }

        RequiresDirective requiresUnnamed = new RequiresDirective(syms.unnamedModule);
        directives.add(requiresUnnamed);
        requires.add(requiresUnnamed);

        msym.requires = requires.toList();
        msym.directives = directives.toList();
    }

    private Completer getSourceCompleter(JCCompilationUnit tree) {
        return new Completer() {
            @Override
            public void complete(Symbol sym) throws CompletionFailure {
                ModuleSymbol msym = (ModuleSymbol) sym;
                msym.flags_field |= UNATTRIBUTED;
                ModuleVisitor v = new ModuleVisitor();
                JavaFileObject prev = log.useSource(tree.sourcefile);
                JCModuleDecl moduleDecl = tree.getModuleDecl();
                DiagnosticPosition prevLintPos = deferredLintHandler.setPos(moduleDecl.pos());

                try {
                    moduleDecl.accept(v);
                    completeModule(msym);
                    checkCyclicDependencies(moduleDecl);
                } finally {
                    log.useSource(prev);
                    deferredLintHandler.setPos(prevLintPos);
                    msym.flags_field &= ~UNATTRIBUTED;
                }
            }

            @Override
            public String toString() {
                return "SourceCompleter: " + tree.sourcefile.getName();
            }

        };
    }

    public boolean isRootModule(ModuleSymbol module) {
        Assert.checkNonNull(rootModules);
        return rootModules.contains(module);
    }

    public Set<ModuleSymbol> getRootModules() {
        Assert.checkNonNull(rootModules);
        return rootModules;
    }

    class ModuleVisitor extends JCTree.Visitor {
        private ModuleSymbol sym;
        private final Set<ModuleSymbol> allRequires = new HashSet<>();
        private final Map<PackageSymbol,List<ExportsDirective>> allExports = new HashMap<>();
        private final Map<PackageSymbol,List<OpensDirective>> allOpens = new HashMap<>();

        @Override
        public void visitModuleDef(JCModuleDecl tree) {
            sym = Assert.checkNonNull(tree.sym);

            if (tree.getModuleType() == ModuleKind.OPEN) {
                sym.flags.add(ModuleFlags.OPEN);
            }
            sym.flags_field |= (tree.mods.flags & Flags.DEPRECATED);

            sym.requires = List.nil();
            sym.exports = List.nil();
            sym.opens = List.nil();
            tree.directives.forEach(t -> t.accept(this));
            sym.requires = sym.requires.reverse();
            sym.exports = sym.exports.reverse();
            sym.opens = sym.opens.reverse();
            ensureJavaBase();
        }

        @Override
        public void visitRequires(JCRequires tree) {
            ModuleSymbol msym = lookupModule(tree.moduleName);
            if (msym.kind != MDL) {
                log.error(tree.moduleName.pos(), Errors.ModuleNotFound(msym));
                warnedMissing.add(msym);
            } else if (allRequires.contains(msym)) {
                log.error(tree.moduleName.pos(), Errors.DuplicateRequires(msym));
            } else {
                allRequires.add(msym);
                Set<RequiresFlag> flags = EnumSet.noneOf(RequiresFlag.class);
                if (tree.isTransitive) {
                    if (msym == syms.java_base && source.compareTo(Source.JDK10) >= 0) {
                        log.error(tree.pos(), Errors.ModifierNotAllowedHere(names.transitive));
                    } else {
                        flags.add(RequiresFlag.TRANSITIVE);
                    }
                }
                if (tree.isStaticPhase) {
                    if (msym == syms.java_base && source.compareTo(Source.JDK10) >= 0) {
                        log.error(tree.pos(), Errors.ModNotAllowedHere(EnumSet.of(Flag.STATIC)));
                    } else {
                        flags.add(RequiresFlag.STATIC_PHASE);
                    }
                }
                RequiresDirective d = new RequiresDirective(msym, flags);
                tree.directive = d;
                sym.requires = sym.requires.prepend(d);
            }
        }

        @Override
        public void visitExports(JCExports tree) {
            Name name = TreeInfo.fullName(tree.qualid);
            PackageSymbol packge = syms.enterPackage(sym, name);
            attr.setPackageSymbols(tree.qualid, packge);

            List<ExportsDirective> exportsForPackage = allExports.computeIfAbsent(packge, p -> List.nil());
            for (ExportsDirective d : exportsForPackage) {
                reportExportsConflict(tree, packge);
            }

            List<ModuleSymbol> toModules = null;
            if (tree.moduleNames != null) {
                Set<ModuleSymbol> to = new LinkedHashSet<>();
                for (JCExpression n: tree.moduleNames) {
                    ModuleSymbol msym = lookupModule(n);
                    chk.checkModuleExists(n.pos(), msym);
                    for (ExportsDirective d : exportsForPackage) {
                        checkDuplicateExportsToModule(n, msym, d);
                    }
                    if (!to.add(msym)) {
                        reportExportsConflictToModule(n, msym);
                    }
                }
                toModules = List.from(to);
            }

            if (toModules == null || !toModules.isEmpty()) {
                Set<ExportsFlag> flags = EnumSet.noneOf(ExportsFlag.class);
                ExportsDirective d = new ExportsDirective(packge, toModules, flags);
                sym.exports = sym.exports.prepend(d);
                tree.directive = d;

                allExports.put(packge, exportsForPackage.prepend(d));
            }
        }

        private void reportExportsConflict(JCExports tree, PackageSymbol packge) {
            log.error(tree.qualid.pos(), Errors.ConflictingExports(packge));
        }

        private void checkDuplicateExportsToModule(JCExpression name, ModuleSymbol msym,
                ExportsDirective d) {
            if (d.modules != null) {
                for (ModuleSymbol other : d.modules) {
                    if (msym == other) {
                        reportExportsConflictToModule(name, msym);
                    }
                }
            }
        }

        private void reportExportsConflictToModule(JCExpression name, ModuleSymbol msym) {
            log.error(name.pos(), Errors.ConflictingExportsToModule(msym));
        }

        @Override
        public void visitOpens(JCOpens tree) {
            Name name = TreeInfo.fullName(tree.qualid);
            PackageSymbol packge = syms.enterPackage(sym, name);
            attr.setPackageSymbols(tree.qualid, packge);

            if (sym.flags.contains(ModuleFlags.OPEN)) {
                log.error(tree.pos(), Errors.NoOpensUnlessStrong);
            }
            List<OpensDirective> opensForPackage = allOpens.computeIfAbsent(packge, p -> List.nil());
            for (OpensDirective d : opensForPackage) {
                reportOpensConflict(tree, packge);
            }

            List<ModuleSymbol> toModules = null;
            if (tree.moduleNames != null) {
                Set<ModuleSymbol> to = new LinkedHashSet<>();
                for (JCExpression n: tree.moduleNames) {
                    ModuleSymbol msym = lookupModule(n);
                    chk.checkModuleExists(n.pos(), msym);
                    for (OpensDirective d : opensForPackage) {
                        checkDuplicateOpensToModule(n, msym, d);
                    }
                    if (!to.add(msym)) {
                        reportOpensConflictToModule(n, msym);
                    }
                }
                toModules = List.from(to);
            }

            if (toModules == null || !toModules.isEmpty()) {
                Set<OpensFlag> flags = EnumSet.noneOf(OpensFlag.class);
                OpensDirective d = new OpensDirective(packge, toModules, flags);
                sym.opens = sym.opens.prepend(d);
                tree.directive = d;

                allOpens.put(packge, opensForPackage.prepend(d));
            }
        }

        private void reportOpensConflict(JCOpens tree, PackageSymbol packge) {
            log.error(tree.qualid.pos(), Errors.ConflictingOpens(packge));
        }

        private void checkDuplicateOpensToModule(JCExpression name, ModuleSymbol msym,
                OpensDirective d) {
            if (d.modules != null) {
                for (ModuleSymbol other : d.modules) {
                    if (msym == other) {
                        reportOpensConflictToModule(name, msym);
                    }
                }
            }
        }

        private void reportOpensConflictToModule(JCExpression name, ModuleSymbol msym) {
            log.error(name.pos(), Errors.ConflictingOpensToModule(msym));
        }

        @Override
        public void visitProvides(JCProvides tree) { }

        @Override
        public void visitUses(JCUses tree) { }

        private void ensureJavaBase() {
            if (sym.name == names.java_base)
                return;

            for (RequiresDirective d: sym.requires) {
                if (d.module.name == names.java_base)
                    return;
            }

            ModuleSymbol java_base = syms.enterModule(names.java_base);
            Directive.RequiresDirective d =
                    new Directive.RequiresDirective(java_base,
                            EnumSet.of(Directive.RequiresFlag.MANDATED));
            sym.requires = sym.requires.prepend(d);
        }

        private ModuleSymbol lookupModule(JCExpression moduleName) {
            Name name = TreeInfo.fullName(moduleName);
            ModuleSymbol msym = moduleFinder.findModule(name);
            TreeInfo.setSymbol(moduleName, msym);
            return msym;
        }
    }

    public Completer getUsesProvidesCompleter() {
        return sym -> {
            ModuleSymbol msym = (ModuleSymbol) sym;

            msym.complete();

            Env<AttrContext> env = typeEnvs.get(msym);
            UsesProvidesVisitor v = new UsesProvidesVisitor(msym, env);
            JavaFileObject prev = log.useSource(env.toplevel.sourcefile);
            JCModuleDecl decl = env.toplevel.getModuleDecl();
            DiagnosticPosition prevLintPos = deferredLintHandler.setPos(decl.pos());

            try {
                decl.accept(v);
            } finally {
                log.useSource(prev);
                deferredLintHandler.setPos(prevLintPos);
            }
        };
    }

    class UsesProvidesVisitor extends JCTree.Visitor {
        private final ModuleSymbol msym;
        private final Env<AttrContext> env;

        private final Set<ClassSymbol> allUses = new HashSet<>();
        private final Map<ClassSymbol, Set<ClassSymbol>> allProvides = new HashMap<>();

        public UsesProvidesVisitor(ModuleSymbol msym, Env<AttrContext> env) {
            this.msym = msym;
            this.env = env;
        }

        @Override @SuppressWarnings("unchecked")
        public void visitModuleDef(JCModuleDecl tree) {
            msym.directives = List.nil();
            msym.provides = List.nil();
            msym.uses = List.nil();
            tree.directives.forEach(t -> t.accept(this));
            msym.directives = msym.directives.reverse();
            msym.provides = msym.provides.reverse();
            msym.uses = msym.uses.reverse();

            if (msym.requires.nonEmpty() && msym.requires.head.flags.contains(RequiresFlag.MANDATED))
                msym.directives = msym.directives.prepend(msym.requires.head);

            msym.directives = msym.directives.appendList(List.from(addReads.getOrDefault(msym, Collections.emptySet())));

            checkForCorrectness();
        }

        @Override
        public void visitExports(JCExports tree) {
            Iterable<Symbol> packageContent = tree.directive.packge.members().getSymbols();
            List<JavaFileObject> filesToCheck = List.nil();
            boolean packageNotEmpty = false;
            for (Symbol sym : packageContent) {
                if (sym.kind != Kinds.Kind.TYP)
                    continue;
                ClassSymbol csym = (ClassSymbol) sym;
                if (sym.completer.isTerminal() ||
                    csym.classfile.getKind() == Kind.CLASS) {
                    packageNotEmpty = true;
                    filesToCheck = List.nil();
                    break;
                }
                if (csym.classfile.getKind() == Kind.SOURCE) {
                    filesToCheck = filesToCheck.prepend(csym.classfile);
                }
            }
            for (JavaFileObject jfo : filesToCheck) {
                if (findPackageInFile.findPackageNameOf(jfo) == tree.directive.packge.fullname) {
                    packageNotEmpty = true;
                    break;
                }
            }
            if (!packageNotEmpty) {
                log.error(tree.qualid.pos(), Errors.PackageEmptyOrNotFound(tree.directive.packge));
            }
            msym.directives = msym.directives.prepend(tree.directive);
        }

        @Override
        public void visitOpens(JCOpens tree) {
            chk.checkPackageExistsForOpens(tree.qualid, tree.directive.packge);
            msym.directives = msym.directives.prepend(tree.directive);
        }

        MethodSymbol noArgsConstructor(ClassSymbol tsym) {
            for (Symbol sym : tsym.members().getSymbolsByName(names.init)) {
                MethodSymbol mSym = (MethodSymbol)sym;
                if (mSym.params().isEmpty()) {
                    return mSym;
                }
            }
            return null;
        }

        MethodSymbol factoryMethod(ClassSymbol tsym) {
            for (Symbol sym : tsym.members().getSymbolsByName(names.provider, sym -> sym.kind == MTH)) {
                MethodSymbol mSym = (MethodSymbol)sym;
                if (mSym.isStatic() && (mSym.flags() & Flags.PUBLIC) != 0 && mSym.params().isEmpty()) {
                    return mSym;
                }
            }
            return null;
        }

        Map<Directive.ProvidesDirective, JCProvides> directiveToTreeMap = new HashMap<>();

        @Override
        public void visitProvides(JCProvides tree) {
            Type st = attr.attribType(tree.serviceName, env, syms.objectType);
            ClassSymbol service = (ClassSymbol) st.tsym;
            if (allProvides.containsKey(service)) {
                log.error(tree.serviceName.pos(), Errors.RepeatedProvidesForService(service));
            }
            ListBuffer<ClassSymbol> impls = new ListBuffer<>();
            for (JCExpression implName : tree.implNames) {
                Type it;
                boolean prevVisitingServiceImplementation = env.info.visitingServiceImplementation;
                try {
                    env.info.visitingServiceImplementation = true;
                    it = attr.attribType(implName, env, syms.objectType);
                } finally {
                    env.info.visitingServiceImplementation = prevVisitingServiceImplementation;
                }
                ClassSymbol impl = (ClassSymbol) it.tsym;
                if ((impl.flags_field & PUBLIC) == 0) {
                    log.error(implName.pos(), Errors.NotDefPublic(impl, impl.location()));
                }
                //find provider factory:
                MethodSymbol factory = factoryMethod(impl);
                if (factory != null) {
                    Type returnType = factory.type.getReturnType();
                    if (!types.isSubtype(returnType, st)) {
                        log.error(implName.pos(), Errors.ServiceImplementationProviderReturnMustBeSubtypeOfServiceInterface);
                    }
                } else {
                    if (!types.isSubtype(it, st)) {
                        log.error(implName.pos(), Errors.ServiceImplementationMustBeSubtypeOfServiceInterface);
                    } else if ((impl.flags() & ABSTRACT) != 0) {
                        log.error(implName.pos(), Errors.ServiceImplementationIsAbstract(impl));
                    } else if (impl.isInner()) {
                        log.error(implName.pos(), Errors.ServiceImplementationIsInner(impl));
                    } else {
                        MethodSymbol constr = noArgsConstructor(impl);
                        if (constr == null) {
                            log.error(implName.pos(), Errors.ServiceImplementationDoesntHaveANoArgsConstructor(impl));
                        } else if ((constr.flags() & PUBLIC) == 0) {
                            log.error(implName.pos(), Errors.ServiceImplementationNoArgsConstructorNotPublic(impl));
                        }
                    }
                }
                if (it.hasTag(CLASS)) {
                    if (allProvides.computeIfAbsent(service, s -> new HashSet<>()).add(impl)) {
                        impls.append(impl);
                    } else {
                        log.error(implName.pos(), Errors.DuplicateProvides(service, impl));
                    }
                }
            }
            if (st.hasTag(CLASS) && !impls.isEmpty()) {
                Directive.ProvidesDirective d = new Directive.ProvidesDirective(service, impls.toList());
                msym.provides = msym.provides.prepend(d);
                msym.directives = msym.directives.prepend(d);
                directiveToTreeMap.put(d, tree);
            }
        }

        @Override
        public void visitRequires(JCRequires tree) {
            if (tree.directive != null && allModules().contains(tree.directive.module)) {
                chk.checkDeprecated(tree.moduleName.pos(), msym, tree.directive.module);
                chk.checkPreview(tree.moduleName.pos(), msym, tree.directive.module);
                chk.checkModuleRequires(tree.moduleName.pos(), tree.directive);
                msym.directives = msym.directives.prepend(tree.directive);
            }
        }

        @Override
        public void visitUses(JCUses tree) {
            Type st = attr.attribType(tree.qualid, env, syms.objectType);
            Symbol sym = TreeInfo.symbol(tree.qualid);
            if ((sym.flags() & ENUM) != 0) {
                log.error(tree.qualid.pos(), Errors.ServiceDefinitionIsEnum(st.tsym));
            } else if (st.hasTag(CLASS)) {
                ClassSymbol service = (ClassSymbol) st.tsym;
                if (allUses.add(service)) {
                    Directive.UsesDirective d = new Directive.UsesDirective(service);
                    msym.uses = msym.uses.prepend(d);
                    msym.directives = msym.directives.prepend(d);
                } else {
                    log.error(tree.pos(), Errors.DuplicateUses(service));
                }
            }
        }

        private void checkForCorrectness() {
            for (Directive.ProvidesDirective provides : msym.provides) {
                JCProvides tree = directiveToTreeMap.get(provides);
                for (ClassSymbol impl : provides.impls) {
                    /* The implementation must be defined in the same module as the provides directive
                     * (else, error)
                     */
                    PackageSymbol implementationDefiningPackage = impl.packge();
                    if (implementationDefiningPackage.modle != msym) {
                        // TODO: should use tree for the implementation name, not the entire provides tree
                        // TODO: should improve error message to identify the implementation type
                        log.error(tree.pos(), Errors.ServiceImplementationNotInRightModule(implementationDefiningPackage.modle));
                    }

                    /* There is no inherent requirement that module that provides a service should actually
                     * use it itself. However, it is a pointless declaration if the service package is not
                     * exported and there is no uses for the service.
                     */
                    PackageSymbol interfaceDeclaringPackage = provides.service.packge();
                    boolean isInterfaceDeclaredInCurrentModule = interfaceDeclaringPackage.modle == msym;
                    boolean isInterfaceExportedFromAReadableModule =
                            msym.visiblePackages.get(interfaceDeclaringPackage.fullname) == interfaceDeclaringPackage;
                    if (isInterfaceDeclaredInCurrentModule && !isInterfaceExportedFromAReadableModule) {
                        // ok the interface is declared in this module. Let's check if it's exported
                        boolean warn = true;
                        for (ExportsDirective export : msym.exports) {
                            if (interfaceDeclaringPackage == export.packge) {
                                warn = false;
                                break;
                            }
                        }
                        if (warn) {
                            for (UsesDirective uses : msym.uses) {
                                if (provides.service == uses.service) {
                                    warn = false;
                                    break;
                                }
                            }
                        }
                        if (warn) {
                            log.warning(tree.pos(), Warnings.ServiceProvidedButNotExportedOrUsed(provides.service));
                        }
                    }
                }
            }
        }
    }

    private Set<ModuleSymbol> allModules;

    public Set<ModuleSymbol> allModules() {
        Assert.checkNonNull(allModules);
        return allModules;
    }

    private void setupAllModules() {
        Assert.checkNonNull(rootModules);
        Assert.checkNull(allModules);

        Set<ModuleSymbol> observable;

        if (limitModsOpt == null && extraLimitMods.isEmpty()) {
            observable = null;
        } else {
            Set<ModuleSymbol> limitMods = new HashSet<>();
            if (limitModsOpt != null) {
                for (String limit : limitModsOpt.split(",")) {
                    if (!isValidName(limit))
                        continue;
                    limitMods.add(syms.enterModule(names.fromString(limit)));
                }
            }
            for (String limit : extraLimitMods) {
                limitMods.add(syms.enterModule(names.fromString(limit)));
            }
            observable = computeTransitiveClosure(limitMods, rootModules, null);
            observable.addAll(rootModules);
            if (lintOptions) {
                for (ModuleSymbol msym : limitMods) {
                    if (!observable.contains(msym)) {
                        log.warning(LintCategory.OPTIONS,
                                Warnings.ModuleForOptionNotFound(Option.LIMIT_MODULES, msym));
                    }
                }
            }
        }

        Predicate<ModuleSymbol> observablePred = sym ->
             (observable == null) ? (moduleFinder.findModule(sym).kind != ERR) : observable.contains(sym);
        Predicate<ModuleSymbol> systemModulePred = sym -> (sym.flags() & Flags.SYSTEM_MODULE) != 0;
        Set<ModuleSymbol> enabledRoot = new LinkedHashSet<>();

        if (rootModules.contains(syms.unnamedModule)) {
            Predicate<ModuleSymbol> jdkModulePred;
            if (target.allApiModulesAreRoots()) {
                jdkModulePred = sym -> {
                    sym.complete();
                    return sym.exports.stream().anyMatch(e -> e.modules == null);
                };
            } else {
                ModuleSymbol javaSE = syms.getModule(java_se);
                if (javaSE != null && (observable == null || observable.contains(javaSE))) {
                    jdkModulePred = sym -> {
                        sym.complete();
                        return !sym.name.startsWith(java_)
                            && sym.exports.stream().anyMatch(e -> e.modules == null);
                    };
                    enabledRoot.add(javaSE);
                } else {
                    jdkModulePred = sym -> true;
                }
            }

            Predicate<ModuleSymbol> noIncubatorPred = sym -> {
                sym.complete();
                return !sym.resolutionFlags.contains(ModuleResolutionFlags.DO_NOT_RESOLVE_BY_DEFAULT);
            };

            for (ModuleSymbol sym : new HashSet<>(syms.getAllModules())) {
                try {
                    if (systemModulePred.test(sym) && observablePred.test(sym) && jdkModulePred.test(sym) && noIncubatorPred.test(sym)) {
                        enabledRoot.add(sym);
                    }
                } catch (CompletionFailure ex) {
                    chk.completionError(null, ex);
                }
            }
        }

        enabledRoot.addAll(rootModules);

        if (addModsOpt != null || !extraAddMods.isEmpty()) {
            Set<String> fullAddMods = new HashSet<>();
            fullAddMods.addAll(extraAddMods);

            if (addModsOpt != null) {
                fullAddMods.addAll(Arrays.asList(addModsOpt.split(",")));
            }

            for (String added : fullAddMods) {
                Stream<ModuleSymbol> modules;
                switch (added) {
                    case ALL_SYSTEM:
                        modules = new HashSet<>(syms.getAllModules())
                                .stream()
                                .filter(systemModulePred.and(observablePred));
                        break;
                    case ALL_MODULE_PATH:
                        modules = new HashSet<>(syms.getAllModules())
                                .stream()
                                .filter(systemModulePred.negate().and(observablePred));
                        break;
                    default:
                        if (!isValidName(added))
                            continue;
                        modules = Stream.of(syms.enterModule(names.fromString(added)));
                        break;
                }
                modules.forEach(sym -> {
                    enabledRoot.add(sym);
                    if (observable != null)
                        observable.add(sym);
                });
            }
        }

        Set<ModuleSymbol> result = computeTransitiveClosure(enabledRoot, rootModules, observable);

        result.add(syms.unnamedModule);

        boolean hasAutomatic = result.stream().anyMatch(IS_AUTOMATIC);

        if (hasAutomatic) {
            syms.getAllModules()
                .stream()
                .filter(IS_AUTOMATIC)
                .forEach(result::add);
        }

        String incubatingModules = result.stream()
                .filter(msym -> msym.resolutionFlags.contains(ModuleResolutionFlags.WARN_INCUBATING))
                .map(msym -> msym.name.toString())
                .collect(Collectors.joining(","));

        if (!incubatingModules.isEmpty()) {
            log.warning(Warnings.IncubatingModules(incubatingModules));
        }

        allModules = result;

        //add module versions from options, if any:
        if (moduleVersionOpt != null) {
            Name version = names.fromString(moduleVersionOpt);
            rootModules.forEach(m -> m.version = version);
        }
    }
    //where:
        private static final Predicate<ModuleSymbol> IS_AUTOMATIC =
                m -> (m.flags_field & Flags.AUTOMATIC_MODULE) != 0;

    public boolean isInModuleGraph(ModuleSymbol msym) {
        return allModules == null || allModules.contains(msym);
    }

    private Set<ModuleSymbol> computeTransitiveClosure(Set<? extends ModuleSymbol> base,
                                                       Set<? extends ModuleSymbol> rootModules,
                                                       Set<ModuleSymbol> observable) {
        List<ModuleSymbol> primaryTodo = List.nil();
        List<ModuleSymbol> secondaryTodo = List.nil();

        for (ModuleSymbol ms : base) {
            if (rootModules.contains(ms)) {
                primaryTodo = primaryTodo.prepend(ms);
            } else {
                secondaryTodo = secondaryTodo.prepend(ms);
            }
        }

        Set<ModuleSymbol> result = new LinkedHashSet<>();
        result.add(syms.java_base);

        while (primaryTodo.nonEmpty() || secondaryTodo.nonEmpty()) {
            try {
                ModuleSymbol current;
                boolean isPrimaryTodo;
                if (primaryTodo.nonEmpty()) {
                    current = primaryTodo.head;
                    primaryTodo = primaryTodo.tail;
                    isPrimaryTodo = true;
                } else {
                    current = secondaryTodo.head;
                    secondaryTodo = secondaryTodo.tail;
                    isPrimaryTodo = false;
                }
                if (observable != null && !observable.contains(current))
                    continue;
                if (!result.add(current) || current == syms.unnamedModule || ((current.flags_field & Flags.AUTOMATIC_MODULE) != 0))
                    continue;
                current.complete();
                if (current.kind == ERR && (isPrimaryTodo || base.contains(current)) && warnedMissing.add(current)) {
                    log.error(Errors.ModuleNotFound(current));
                }
                for (RequiresDirective rd : current.requires) {
                    if (rd.module == syms.java_base) continue;
                    if ((rd.isTransitive() && isPrimaryTodo) || rootModules.contains(current)) {
                        primaryTodo = primaryTodo.prepend(rd.module);
                    } else {
                        secondaryTodo = secondaryTodo.prepend(rd.module);
                    }
                }
            } catch (CompletionFailure ex) {
                chk.completionError(null, ex);
            }
        }

        return result;
    }

    public ModuleSymbol getObservableModule(Name name) {
        ModuleSymbol mod = syms.getModule(name);

        if (allModules().contains(mod)) {
            return mod;
        }

        return null;
    }

    private Completer getUnnamedModuleCompleter() {
        moduleFinder.findAllModules();
        return new Symbol.Completer() {
            @Override
            public void complete(Symbol sym) throws CompletionFailure {
                if (inInitModules) {
                    sym.completer = this;
                    return ;
                }
                ModuleSymbol msym = (ModuleSymbol) sym;
                Set<ModuleSymbol> allModules = new HashSet<>(allModules());
                allModules.remove(syms.unnamedModule);
                for (ModuleSymbol m : allModules) {
                    m.complete();
                }
                initVisiblePackages(msym, allModules);
            }

            @Override
            public String toString() {
                return "unnamedModule Completer";
            }
        };
    }

    private final Map<ModuleSymbol, Set<ModuleSymbol>> requiresTransitiveCache = new HashMap<>();

    private void completeModule(ModuleSymbol msym) {
        if (inInitModules) {
            msym.completer = sym -> completeModule(msym);
            return ;
        }

        if ((msym.flags_field & Flags.AUTOMATIC_MODULE) != 0) {
            completeAutomaticModule(msym);
        }

        Assert.checkNonNull(msym.requires);

        initAddReads();

        msym.requires = msym.requires.appendList(List.from(addReads.getOrDefault(msym, Collections.emptySet())));

        List<RequiresDirective> requires = msym.requires;

        while (requires.nonEmpty()) {
            if (!allModules().contains(requires.head.module)) {
                Env<AttrContext> env = typeEnvs.get(msym);
                if (env != null) {
                    JavaFileObject origSource = log.useSource(env.toplevel.sourcefile);
                    try {
                        log.error(/*XXX*/env.tree, Errors.ModuleNotFound(requires.head.module));
                    } finally {
                        log.useSource(origSource);
                    }
                } else {
                    Assert.check((msym.flags() & Flags.AUTOMATIC_MODULE) == 0);
                }
                msym.requires = List.filter(msym.requires, requires.head);
            }
            requires = requires.tail;
        }

        Set<ModuleSymbol> readable = new LinkedHashSet<>();
        Set<ModuleSymbol> requiresTransitive = new HashSet<>();

        for (RequiresDirective d : msym.requires) {
            d.module.complete();
            readable.add(d.module);
            Set<ModuleSymbol> s = retrieveRequiresTransitive(d.module);
            Assert.checkNonNull(s, () -> "no entry in cache for " + d.module);
            readable.addAll(s);
            if (d.flags.contains(RequiresFlag.TRANSITIVE)) {
                requiresTransitive.add(d.module);
                requiresTransitive.addAll(s);
            }
        }

        requiresTransitiveCache.put(msym, requiresTransitive);
        initVisiblePackages(msym, readable);
        for (ExportsDirective d: msym.exports) {
            if (d.packge != null) {
                d.packge.modle = msym;
            }
        }
    }

    private Set<ModuleSymbol> retrieveRequiresTransitive(ModuleSymbol msym) {
        Set<ModuleSymbol> requiresTransitive = requiresTransitiveCache.get(msym);

        if (requiresTransitive == null) {
            //the module graph may contain cycles involving automatic modules or --add-reads edges
            requiresTransitive = new HashSet<>();

            Set<ModuleSymbol> seen = new HashSet<>();
            List<ModuleSymbol> todo = List.of(msym);

            while (todo.nonEmpty()) {
                ModuleSymbol current = todo.head;
                todo = todo.tail;
                if (!seen.add(current))
                    continue;
                requiresTransitive.add(current);
                current.complete();
                Iterable<? extends RequiresDirective> requires;
                if (current != syms.unnamedModule) {
                    Assert.checkNonNull(current.requires, () -> current + ".requires == null; " + msym);
                    requires = current.requires;
                    for (RequiresDirective rd : requires) {
                        if (rd.isTransitive())
                            todo = todo.prepend(rd.module);
                    }
                } else {
                    for (ModuleSymbol mod : allModules()) {
                        todo = todo.prepend(mod);
                    }
                }
            }

            requiresTransitive.remove(msym);
        }

        return requiresTransitive;
    }

    private void initVisiblePackages(ModuleSymbol msym, Collection<ModuleSymbol> readable) {
        initAddExports();

        msym.visiblePackages = new LinkedHashMap<>();
        msym.readModules = new HashSet<>(readable);

        Map<Name, ModuleSymbol> seen = new HashMap<>();

        for (ModuleSymbol rm : readable) {
            if (rm == syms.unnamedModule)
                continue;
            addVisiblePackages(msym, seen, rm, rm.exports);
        }

        addExports.forEach((exportsFrom, exports) -> {
            if (msym.readModules.contains(exportsFrom)) {
                addVisiblePackages(msym, seen, exportsFrom, exports);
            }
        });
    }

    private void addVisiblePackages(ModuleSymbol msym,
                                    Map<Name, ModuleSymbol> seenPackages,
                                    ModuleSymbol exportsFrom,
                                    Collection<ExportsDirective> exports) {
        for (ExportsDirective d : exports) {
            if (d.modules == null || d.modules.contains(msym)) {
                Name packageName = d.packge.fullname;
                ModuleSymbol previousModule = seenPackages.get(packageName);

                if (previousModule != null && previousModule != exportsFrom) {
                    Env<AttrContext> env = typeEnvs.get(msym);
                    JavaFileObject origSource = env != null ? log.useSource(env.toplevel.sourcefile)
                                                            : null;
                    DiagnosticPosition pos = env != null ? env.tree.pos() : null;
                    try {
                        if (msym.isUnnamed()) {
                            log.error(pos, Errors.PackageClashFromRequiresInUnnamed(packageName,
                                                                                    previousModule, exportsFrom));
                        } else {
                            log.error(pos, Errors.PackageClashFromRequires(msym, packageName,
                                                                           previousModule, exportsFrom));
                        }
                    } finally {
                        if (env != null)
                            log.useSource(origSource);
                    }
                    continue;
                }

                seenPackages.put(packageName, exportsFrom);
                msym.visiblePackages.put(d.packge.fullname, d.packge);
            }
        }
    }

    private void initAddExports() {
        if (addExports != null)
            return;

        addExports = new LinkedHashMap<>();
        Set<ModuleSymbol> unknownModules = new HashSet<>();

        if (addExportsOpt == null)
            return;

        Pattern ep = Pattern.compile("([^/]+)/([^=]+)=(.*)");
        for (String s: addExportsOpt.split("\0+")) {
            if (s.isEmpty())
                continue;
            Matcher em = ep.matcher(s);
            if (!em.matches()) {
                continue;
            }

            // Terminology comes from
            //  --add-exports module/package=target,...
            // Compare to
            //  module module { exports package to target, ... }
            String moduleName = em.group(1);
            String packageName = em.group(2);
            String targetNames = em.group(3);

            if (!isValidName(moduleName))
                continue;

            ModuleSymbol msym = syms.enterModule(names.fromString(moduleName));
            if (!isKnownModule(msym, unknownModules))
                continue;

            if (!isValidName(packageName))
                continue;

            if (!allowAccessIntoSystem && (msym.flags() & Flags.SYSTEM_MODULE) != 0) {
                log.error(Errors.AddExportsWithRelease(msym));
                continue;
            }

            PackageSymbol p = syms.enterPackage(msym, names.fromString(packageName));
            p.modle = msym;  // TODO: do we need this?

            List<ModuleSymbol> targetModules = List.nil();
            for (String toModule : targetNames.split("[ ,]+")) {
                ModuleSymbol m;
                if (toModule.equals("ALL-UNNAMED")) {
                    m = syms.unnamedModule;
                } else {
                    if (!isValidName(toModule))
                        continue;
                    m = syms.enterModule(names.fromString(toModule));
                    if (!isKnownModule(m, unknownModules))
                        continue;
                }
                targetModules = targetModules.prepend(m);
            }

            Set<ExportsDirective> extra = addExports.computeIfAbsent(msym, _x -> new LinkedHashSet<>());
            ExportsDirective d = new ExportsDirective(p, targetModules);
            extra.add(d);
        }
    }

    private boolean isKnownModule(ModuleSymbol msym, Set<ModuleSymbol> unknownModules) {
        if (allModules.contains(msym)) {
            return true;
        }

        if (!unknownModules.contains(msym)) {
            if (lintOptions) {
                log.warning(LintCategory.OPTIONS,
                        Warnings.ModuleForOptionNotFound(Option.ADD_EXPORTS, msym));
            }
            unknownModules.add(msym);
        }
        return false;
    }

    private void initAddReads() {
        if (addReads != null)
            return;

        addReads = new LinkedHashMap<>();

        if (addReadsOpt == null)
            return;

        Pattern rp = Pattern.compile("([^=]+)=(.*)");
        for (String s : addReadsOpt.split("\0+")) {
            if (s.isEmpty())
                continue;
            Matcher rm = rp.matcher(s);
            if (!rm.matches()) {
                continue;
            }

            // Terminology comes from
            //  --add-reads source-module=target-module,...
            // Compare to
            //  module source-module { requires target-module; ... }
            String sourceName = rm.group(1);
            String targetNames = rm.group(2);

            if (!isValidName(sourceName))
                continue;

            ModuleSymbol msym = syms.enterModule(names.fromString(sourceName));
            if (!allModules.contains(msym)) {
                if (lintOptions) {
                    log.warning(Warnings.ModuleForOptionNotFound(Option.ADD_READS, msym));
                }
                continue;
            }

            if (!allowAccessIntoSystem && (msym.flags() & Flags.SYSTEM_MODULE) != 0) {
                log.error(Errors.AddReadsWithRelease(msym));
                continue;
            }

            for (String targetName : targetNames.split("[ ,]+", -1)) {
                ModuleSymbol targetModule;
                if (targetName.equals("ALL-UNNAMED")) {
                    targetModule = syms.unnamedModule;
                } else {
                    if (!isValidName(targetName))
                        continue;
                    targetModule = syms.enterModule(names.fromString(targetName));
                    if (!allModules.contains(targetModule)) {
                        if (lintOptions) {
                            log.warning(LintCategory.OPTIONS, Warnings.ModuleForOptionNotFound(Option.ADD_READS, targetModule));
                        }
                        continue;
                    }
                }
                addReads.computeIfAbsent(msym, m -> new HashSet<>())
                        .add(new RequiresDirective(targetModule, EnumSet.of(RequiresFlag.EXTRA)));
            }
        }
    }

    private void checkCyclicDependencies(JCModuleDecl mod) {
        for (JCDirective d : mod.directives) {
            JCRequires rd;
            if (!d.hasTag(Tag.REQUIRES) || (rd = (JCRequires) d).directive == null)
                continue;
            Set<ModuleSymbol> nonSyntheticDeps = new HashSet<>();
            List<ModuleSymbol> queue = List.of(rd.directive.module);
            while (queue.nonEmpty()) {
                ModuleSymbol current = queue.head;
                queue = queue.tail;
                if (!nonSyntheticDeps.add(current))
                    continue;
                current.complete();
                if ((current.flags() & Flags.AUTOMATIC_MODULE) != 0)
                    continue;
                Assert.checkNonNull(current.requires, current::toString);
                for (RequiresDirective dep : current.requires) {
                    if (!dep.flags.contains(RequiresFlag.EXTRA))
                        queue = queue.prepend(dep.module);
                }
            }
            if (nonSyntheticDeps.contains(mod.sym)) {
                log.error(rd.moduleName.pos(), Errors.CyclicRequires(rd.directive.module));
            }
        }
    }

    private boolean isValidName(CharSequence name) {
        return SourceVersion.isName(name, Source.toSourceVersion(source));
    }

    // DEBUG
    private String toString(ModuleSymbol msym) {
        return msym.name + "["
                + "kind:" + msym.kind + ";"
                + "locn:" + toString(msym.sourceLocation) + "," + toString(msym.classLocation) + ";"
                + "info:" + toString(msym.module_info.sourcefile) + ","
                            + toString(msym.module_info.classfile) + ","
                            + msym.module_info.completer
                + "]";
    }

    // DEBUG
    String toString(Location locn) {
        return (locn == null) ? "--" : locn.getName();
    }

    // DEBUG
    String toString(JavaFileObject fo) {
        return (fo == null) ? "--" : fo.getName();
    }

    public void newRound() {
        allModules = null;
        rootModules = null;
        defaultModule = null;
        warnedMissing.clear();
    }

    public interface PackageNameFinder {
        public Name findPackageNameOf(JavaFileObject jfo);
    }
}
