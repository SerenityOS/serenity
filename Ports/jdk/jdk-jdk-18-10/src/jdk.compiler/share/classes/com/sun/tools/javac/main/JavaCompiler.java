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

package com.sun.tools.javac.main;

import java.io.*;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Queue;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.processing.Processor;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementVisitor;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardLocation;

import com.sun.source.util.TaskEvent;
import com.sun.tools.javac.api.MultiTaskListener;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Lint.LintCategory;
import com.sun.tools.javac.code.Source.Feature;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.CompletionFailure;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.comp.*;
import com.sun.tools.javac.comp.CompileStates.CompileState;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.jvm.*;
import com.sun.tools.javac.parser.*;
import com.sun.tools.javac.platform.PlatformDescription;
import com.sun.tools.javac.processing.*;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCLambda;
import com.sun.tools.javac.tree.JCTree.JCMemberReference;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.DefinedBy.Api;
import com.sun.tools.javac.util.JCDiagnostic.Factory;
import com.sun.tools.javac.util.Log.DiagnosticHandler;
import com.sun.tools.javac.util.Log.DiscardDiagnosticHandler;
import com.sun.tools.javac.util.Log.WriterKind;

import static com.sun.tools.javac.code.Kinds.Kind.*;

import com.sun.tools.javac.code.Symbol.ModuleSymbol;
import com.sun.tools.javac.resources.CompilerProperties.Errors;
import com.sun.tools.javac.resources.CompilerProperties.Fragments;
import com.sun.tools.javac.resources.CompilerProperties.Notes;
import com.sun.tools.javac.resources.CompilerProperties.Warnings;

import static com.sun.tools.javac.code.TypeTag.CLASS;
import static com.sun.tools.javac.main.Option.*;
import static com.sun.tools.javac.util.JCDiagnostic.DiagnosticFlag.*;

import static javax.tools.StandardLocation.CLASS_OUTPUT;

import com.sun.tools.javac.tree.JCTree.JCModuleDecl;

/** This class could be the main entry point for GJC when GJC is used as a
 *  component in a larger software system. It provides operations to
 *  construct a new compiler, and to run a new compiler on a set of source
 *  files.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class JavaCompiler {
    /** The context key for the compiler. */
    public static final Context.Key<JavaCompiler> compilerKey = new Context.Key<>();

    /** Get the JavaCompiler instance for this context. */
    public static JavaCompiler instance(Context context) {
        JavaCompiler instance = context.get(compilerKey);
        if (instance == null)
            instance = new JavaCompiler(context);
        return instance;
    }

    /** The current version number as a string.
     */
    public static String version() {
        return version("release");  // mm.nn.oo[-milestone]
    }

    /** The current full version number as a string.
     */
    public static String fullVersion() {
        return version("full"); // mm.mm.oo[-milestone]-build
    }

    private static final String versionRBName = "com.sun.tools.javac.resources.version";
    private static ResourceBundle versionRB;

    private static String version(String key) {
        if (versionRB == null) {
            try {
                versionRB = ResourceBundle.getBundle(versionRBName);
            } catch (MissingResourceException e) {
                return Log.getLocalizedString("version.not.available");
            }
        }
        try {
            return versionRB.getString(key);
        }
        catch (MissingResourceException e) {
            return Log.getLocalizedString("version.not.available");
        }
    }

    /**
     * Control how the compiler's latter phases (attr, flow, desugar, generate)
     * are connected. Each individual file is processed by each phase in turn,
     * but with different compile policies, you can control the order in which
     * each class is processed through its next phase.
     *
     * <p>Generally speaking, the compiler will "fail fast" in the face of
     * errors, although not aggressively so. flow, desugar, etc become no-ops
     * once any errors have occurred. No attempt is currently made to determine
     * if it might be safe to process a class through its next phase because
     * it does not depend on any unrelated errors that might have occurred.
     */
    protected static enum CompilePolicy {
        /**
         * Attribute everything, then do flow analysis for everything,
         * then desugar everything, and only then generate output.
         * This means no output will be generated if there are any
         * errors in any classes.
         */
        SIMPLE,

        /**
         * Groups the classes for each source file together, then process
         * each group in a manner equivalent to the {@code SIMPLE} policy.
         * This means no output will be generated if there are any
         * errors in any of the classes in a source file.
         */
        BY_FILE,

        /**
         * Completely process each entry on the todo list in turn.
         * -- this is the same for 1.5.
         * Means output might be generated for some classes in a compilation unit
         * and not others.
         */
        BY_TODO;

        static CompilePolicy decode(String option) {
            if (option == null)
                return DEFAULT_COMPILE_POLICY;
            else if (option.equals("simple"))
                return SIMPLE;
            else if (option.equals("byfile"))
                return BY_FILE;
            else if (option.equals("bytodo"))
                return BY_TODO;
            else
                return DEFAULT_COMPILE_POLICY;
        }
    }

    private static final CompilePolicy DEFAULT_COMPILE_POLICY = CompilePolicy.BY_TODO;

    protected static enum ImplicitSourcePolicy {
        /** Don't generate or process implicitly read source files. */
        NONE,
        /** Generate classes for implicitly read source files. */
        CLASS,
        /** Like CLASS, but generate warnings if annotation processing occurs */
        UNSET;

        static ImplicitSourcePolicy decode(String option) {
            if (option == null)
                return UNSET;
            else if (option.equals("none"))
                return NONE;
            else if (option.equals("class"))
                return CLASS;
            else
                return UNSET;
        }
    }

    /** The log to be used for error reporting.
     */
    public Log log;

    /** Factory for creating diagnostic objects
     */
    JCDiagnostic.Factory diagFactory;

    /** The tree factory module.
     */
    protected TreeMaker make;

    /** The class finder.
     */
    protected ClassFinder finder;

    /** The class reader.
     */
    protected ClassReader reader;

    /** The class writer.
     */
    protected ClassWriter writer;

    /** The native header writer.
     */
    protected JNIWriter jniWriter;

    /** The module for the symbol table entry phases.
     */
    protected Enter enter;

    /** The symbol table.
     */
    protected Symtab syms;

    /** The language version.
     */
    protected Source source;

    /** The preview language version.
     */
    protected Preview preview;

    /** The module for code generation.
     */
    protected Gen gen;

    /** The name table.
     */
    protected Names names;

    /** The attributor.
     */
    protected Attr attr;

    /** The analyzer
     */
    protected Analyzer analyzer;

    /** The attributor.
     */
    protected Check chk;

    /** The flow analyzer.
     */
    protected Flow flow;

    /** The modules visitor
     */
    protected Modules modules;

    /** The module finder
     */
    protected ModuleFinder moduleFinder;

    /** The diagnostics factory
     */
    protected JCDiagnostic.Factory diags;

    protected DeferredCompletionFailureHandler dcfh;

    /** The type eraser.
     */
    protected TransTypes transTypes;

    /** The syntactic sugar desweetener.
     */
    protected Lower lower;

    /** The annotation annotator.
     */
    protected Annotate annotate;

    /** Force a completion failure on this name
     */
    protected final Name completionFailureName;

    /** Type utilities.
     */
    protected Types types;

    /** Access to file objects.
     */
    protected JavaFileManager fileManager;

    /** Factory for parsers.
     */
    protected ParserFactory parserFactory;

    /** Broadcasting listener for progress events
     */
    protected MultiTaskListener taskListener;

    /**
     * SourceCompleter that delegates to the readSourceFile method of this class.
     */
    protected final Symbol.Completer sourceCompleter =
            sym -> readSourceFile((ClassSymbol) sym);

    /**
     * Command line options.
     */
    protected Options options;

    protected Context context;

    /**
     * Flag set if any annotation processing occurred.
     **/
    protected boolean annotationProcessingOccurred;

    /**
     * Flag set if any implicit source files read.
     **/
    protected boolean implicitSourceFilesRead;

    private boolean enterDone;

    protected CompileStates compileStates;

    /** Construct a new compiler using a shared context.
     */
    public JavaCompiler(Context context) {
        this.context = context;
        context.put(compilerKey, this);

        // if fileManager not already set, register the JavacFileManager to be used
        if (context.get(JavaFileManager.class) == null)
            JavacFileManager.preRegister(context);

        names = Names.instance(context);
        log = Log.instance(context);
        diagFactory = JCDiagnostic.Factory.instance(context);
        finder = ClassFinder.instance(context);
        reader = ClassReader.instance(context);
        make = TreeMaker.instance(context);
        writer = ClassWriter.instance(context);
        jniWriter = JNIWriter.instance(context);
        enter = Enter.instance(context);
        todo = Todo.instance(context);

        fileManager = context.get(JavaFileManager.class);
        parserFactory = ParserFactory.instance(context);
        compileStates = CompileStates.instance(context);

        try {
            // catch completion problems with predefineds
            syms = Symtab.instance(context);
        } catch (CompletionFailure ex) {
            // inlined Check.completionError as it is not initialized yet
            log.error(Errors.CantAccess(ex.sym, ex.getDetailValue()));
        }
        source = Source.instance(context);
        preview = Preview.instance(context);
        attr = Attr.instance(context);
        analyzer = Analyzer.instance(context);
        chk = Check.instance(context);
        gen = Gen.instance(context);
        flow = Flow.instance(context);
        transTypes = TransTypes.instance(context);
        lower = Lower.instance(context);
        annotate = Annotate.instance(context);
        types = Types.instance(context);
        taskListener = MultiTaskListener.instance(context);
        modules = Modules.instance(context);
        moduleFinder = ModuleFinder.instance(context);
        diags = Factory.instance(context);
        dcfh = DeferredCompletionFailureHandler.instance(context);

        finder.sourceCompleter = sourceCompleter;
        modules.findPackageInFile = this::findPackageInFile;
        moduleFinder.moduleNameFromSourceReader = this::readModuleName;

        options = Options.instance(context);

        verbose       = options.isSet(VERBOSE);
        sourceOutput  = options.isSet(PRINTSOURCE); // used to be -s
        lineDebugInfo = options.isUnset(G_CUSTOM) ||
                        options.isSet(G_CUSTOM, "lines");
        genEndPos     = options.isSet(XJCOV) ||
                        context.get(DiagnosticListener.class) != null;
        devVerbose    = options.isSet("dev");
        processPcks   = options.isSet("process.packages");
        werror        = options.isSet(WERROR);

        verboseCompilePolicy = options.isSet("verboseCompilePolicy");

        compilePolicy = CompilePolicy.decode(options.get("compilePolicy"));

        implicitSourcePolicy = ImplicitSourcePolicy.decode(options.get("-implicit"));

        completionFailureName =
            options.isSet("failcomplete")
            ? names.fromString(options.get("failcomplete"))
            : null;

        shouldStopPolicyIfError =
            options.isSet("should-stop.at") // backwards compatible
            ? CompileState.valueOf(options.get("should-stop.at"))
            : options.isSet("should-stop.ifError")
            ? CompileState.valueOf(options.get("should-stop.ifError"))
            : CompileState.INIT;
        shouldStopPolicyIfNoError =
            options.isSet("should-stop.ifNoError")
            ? CompileState.valueOf(options.get("should-stop.ifNoError"))
            : CompileState.GENERATE;

        if (options.isUnset("diags.legacy"))
            log.setDiagnosticFormatter(RichDiagnosticFormatter.instance(context));

        PlatformDescription platformProvider = context.get(PlatformDescription.class);

        if (platformProvider != null)
            closeables = closeables.prepend(platformProvider);

        silentFail = new Symbol(ABSENT_TYP, 0, names.empty, Type.noType, syms.rootPackage) {
            @DefinedBy(Api.LANGUAGE_MODEL)
            public <R, P> R accept(ElementVisitor<R, P> v, P p) {
                return v.visitUnknown(this, p);
            }
            @Override
            public boolean exists() {
                return false;
            }
        };

    }

    /* Switches:
     */

    /** Verbose output.
     */
    public boolean verbose;

    /** Emit plain Java source files rather than class files.
     */
    public boolean sourceOutput;


    /** Generate code with the LineNumberTable attribute for debugging
     */
    public boolean lineDebugInfo;

    /** Switch: should we store the ending positions?
     */
    public boolean genEndPos;

    /** Switch: should we debug ignored exceptions
     */
    protected boolean devVerbose;

    /** Switch: should we (annotation) process packages as well
     */
    protected boolean processPcks;

    /** Switch: treat warnings as errors
     */
    protected boolean werror;

    /** Switch: is annotation processing requested explicitly via
     * CompilationTask.setProcessors?
     */
    protected boolean explicitAnnotationProcessingRequested = false;

    /**
     * The policy for the order in which to perform the compilation
     */
    protected CompilePolicy compilePolicy;

    /**
     * The policy for what to do with implicitly read source files
     */
    protected ImplicitSourcePolicy implicitSourcePolicy;

    /**
     * Report activity related to compilePolicy
     */
    public boolean verboseCompilePolicy;

    /**
     * Policy of how far to continue compilation after errors have occurred.
     * Set this to minimum CompileState (INIT) to stop as soon as possible
     * after errors.
     */
    public CompileState shouldStopPolicyIfError;

    /**
     * Policy of how far to continue compilation when no errors have occurred.
     * Set this to maximum CompileState (GENERATE) to perform full compilation.
     * Set this lower to perform partial compilation, such as -proc:only.
     */
    public CompileState shouldStopPolicyIfNoError;

    /** A queue of all as yet unattributed classes.
     */
    public Todo todo;

    /** A list of items to be closed when the compilation is complete.
     */
    public List<Closeable> closeables = List.nil();

    /** The set of currently compiled inputfiles, needed to ensure
     *  we don't accidentally overwrite an input file when -s is set.
     *  initialized by `compile'.
     */
    protected Set<JavaFileObject> inputFiles = new HashSet<>();

    /** Used by the resolveBinaryNameOrIdent to say that the given type cannot be found, and that
     *  an error has already been produced about that.
     */
    private final Symbol silentFail;

    protected boolean shouldStop(CompileState cs) {
        CompileState shouldStopPolicy = (errorCount() > 0 || unrecoverableError())
            ? shouldStopPolicyIfError
            : shouldStopPolicyIfNoError;
        return cs.isAfter(shouldStopPolicy);
    }

    /** The number of errors reported so far.
     */
    public int errorCount() {
        if (werror && log.nerrors == 0 && log.nwarnings > 0) {
            log.error(Errors.WarningsAndWerror);
        }
        return log.nerrors;
    }

    protected final <T> Queue<T> stopIfError(CompileState cs, Queue<T> queue) {
        return shouldStop(cs) ? new ListBuffer<T>() : queue;
    }

    protected final <T> List<T> stopIfError(CompileState cs, List<T> list) {
        return shouldStop(cs) ? List.nil() : list;
    }

    /** The number of warnings reported so far.
     */
    public int warningCount() {
        return log.nwarnings;
    }

    /** Try to open input stream with given name.
     *  Report an error if this fails.
     *  @param filename   The file name of the input stream to be opened.
     */
    public CharSequence readSource(JavaFileObject filename) {
        try {
            inputFiles.add(filename);
            return filename.getCharContent(false);
        } catch (IOException e) {
            log.error(Errors.ErrorReadingFile(filename, JavacFileManager.getMessage(e)));
            return null;
        }
    }

    /** Parse contents of input stream.
     *  @param filename     The name of the file from which input stream comes.
     *  @param content      The characters to be parsed.
     */
    protected JCCompilationUnit parse(JavaFileObject filename, CharSequence content) {
        long msec = now();
        JCCompilationUnit tree = make.TopLevel(List.nil());
        if (content != null) {
            if (verbose) {
                log.printVerbose("parsing.started", filename);
            }
            if (!taskListener.isEmpty()) {
                TaskEvent e = new TaskEvent(TaskEvent.Kind.PARSE, filename);
                taskListener.started(e);
                keepComments = true;
                genEndPos = true;
            }
            Parser parser = parserFactory.newParser(content, keepComments(), genEndPos,
                                lineDebugInfo, filename.isNameCompatible("module-info", Kind.SOURCE));
            tree = parser.parseCompilationUnit();
            if (verbose) {
                log.printVerbose("parsing.done", Long.toString(elapsed(msec)));
            }
        }

        tree.sourcefile = filename;

        if (content != null && !taskListener.isEmpty()) {
            TaskEvent e = new TaskEvent(TaskEvent.Kind.PARSE, tree);
            taskListener.finished(e);
        }

        return tree;
    }
    // where
        public boolean keepComments = false;
        protected boolean keepComments() {
            return keepComments || sourceOutput;
        }


    /** Parse contents of file.
     *  @param filename     The name of the file to be parsed.
     */
    @Deprecated
    public JCTree.JCCompilationUnit parse(String filename) {
        JavacFileManager fm = (JavacFileManager)fileManager;
        return parse(fm.getJavaFileObjectsFromStrings(List.of(filename)).iterator().next());
    }

    /** Parse contents of file.
     *  @param filename     The name of the file to be parsed.
     */
    public JCTree.JCCompilationUnit parse(JavaFileObject filename) {
        JavaFileObject prev = log.useSource(filename);
        try {
            JCTree.JCCompilationUnit t = parse(filename, readSource(filename));
            if (t.endPositions != null)
                log.setEndPosTable(filename, t.endPositions);
            return t;
        } finally {
            log.useSource(prev);
        }
    }

    /** Resolve an identifier which may be the binary name of a class or
     * the Java name of a class or package.
     * @param name      The name to resolve
     */
    public Symbol resolveBinaryNameOrIdent(String name) {
        ModuleSymbol msym;
        String typeName;
        int sep = name.indexOf('/');
        if (sep == -1) {
            msym = modules.getDefaultModule();
            typeName = name;
        } else if (Feature.MODULES.allowedInSource(source)) {
            Name modName = names.fromString(name.substring(0, sep));

            msym = moduleFinder.findModule(modName);
            typeName = name.substring(sep + 1);
        } else {
            log.error(Errors.InvalidModuleSpecifier(name));
            return silentFail;
        }

        return resolveBinaryNameOrIdent(msym, typeName);
    }

    /** Resolve an identifier which may be the binary name of a class or
     * the Java name of a class or package.
     * @param msym      The module in which the search should be performed
     * @param name      The name to resolve
     */
    public Symbol resolveBinaryNameOrIdent(ModuleSymbol msym, String name) {
        try {
            Name flatname = names.fromString(name.replace("/", "."));
            return finder.loadClass(msym, flatname);
        } catch (CompletionFailure ignore) {
            return resolveIdent(msym, name);
        }
    }

    /** Resolve an identifier.
     * @param msym      The module in which the search should be performed
     * @param name      The identifier to resolve
     */
    public Symbol resolveIdent(ModuleSymbol msym, String name) {
        if (name.equals(""))
            return syms.errSymbol;
        JavaFileObject prev = log.useSource(null);
        try {
            JCExpression tree = null;
            for (String s : name.split("\\.", -1)) {
                if (!SourceVersion.isIdentifier(s)) // TODO: check for keywords
                    return syms.errSymbol;
                tree = (tree == null) ? make.Ident(names.fromString(s))
                                      : make.Select(tree, names.fromString(s));
            }
            JCCompilationUnit toplevel =
                make.TopLevel(List.nil());
            toplevel.modle = msym;
            toplevel.packge = msym.unnamedPackage;
            return attr.attribIdent(tree, toplevel);
        } finally {
            log.useSource(prev);
        }
    }

    /** Generate code and emit a class file for a given class
     *  @param env    The attribution environment of the outermost class
     *                containing this class.
     *  @param cdef   The class definition from which code is generated.
     */
    JavaFileObject genCode(Env<AttrContext> env, JCClassDecl cdef) throws IOException {
        try {
            if (gen.genClass(env, cdef) && (errorCount() == 0))
                return writer.writeClass(cdef.sym);
        } catch (ClassWriter.PoolOverflow ex) {
            log.error(cdef.pos(), Errors.LimitPool);
        } catch (ClassWriter.StringOverflow ex) {
            log.error(cdef.pos(),
                      Errors.LimitStringOverflow(ex.value.substring(0, 20)));
        } catch (CompletionFailure ex) {
            chk.completionError(cdef.pos(), ex);
        }
        return null;
    }

    /** Emit plain Java source for a class.
     *  @param env    The attribution environment of the outermost class
     *                containing this class.
     *  @param cdef   The class definition to be printed.
     */
    JavaFileObject printSource(Env<AttrContext> env, JCClassDecl cdef) throws IOException {
        JavaFileObject outFile
           = fileManager.getJavaFileForOutput(CLASS_OUTPUT,
                                               cdef.sym.flatname.toString(),
                                               JavaFileObject.Kind.SOURCE,
                                               null);
        if (inputFiles.contains(outFile)) {
            log.error(cdef.pos(), Errors.SourceCantOverwriteInputFile(outFile));
            return null;
        } else {
            try (BufferedWriter out = new BufferedWriter(outFile.openWriter())) {
                new Pretty(out, true).printUnit(env.toplevel, cdef);
                if (verbose)
                    log.printVerbose("wrote.file", outFile.getName());
            }
            return outFile;
        }
    }

    /** Compile a source file that has been accessed by the class finder.
     *  @param c          The class the source file of which needs to be compiled.
     */
    private void readSourceFile(ClassSymbol c) throws CompletionFailure {
        readSourceFile(null, c);
    }

    /** Compile a ClassSymbol from source, optionally using the given compilation unit as
     *  the source tree.
     *  @param tree the compilation unit in which the given ClassSymbol resides,
     *              or null if should be parsed from source
     *  @param c    the ClassSymbol to complete
     */
    public void readSourceFile(JCCompilationUnit tree, ClassSymbol c) throws CompletionFailure {
        if (completionFailureName == c.fullname) {
            throw new CompletionFailure(
                c, () -> diagFactory.fragment(Fragments.UserSelectedCompletionFailure), dcfh);
        }
        JavaFileObject filename = c.classfile;
        JavaFileObject prev = log.useSource(filename);

        if (tree == null) {
            try {
                tree = parse(filename, filename.getCharContent(false));
            } catch (IOException e) {
                log.error(Errors.ErrorReadingFile(filename, JavacFileManager.getMessage(e)));
                tree = make.TopLevel(List.<JCTree>nil());
            } finally {
                log.useSource(prev);
            }
        }

        if (!taskListener.isEmpty()) {
            TaskEvent e = new TaskEvent(TaskEvent.Kind.ENTER, tree);
            taskListener.started(e);
        }

        // Process module declarations.
        // If module resolution fails, ignore trees, and if trying to
        // complete a specific symbol, throw CompletionFailure.
        // Note that if module resolution failed, we may not even
        // have enough modules available to access java.lang, and
        // so risk getting FatalError("no.java.lang") from MemberEnter.
        if (!modules.enter(List.of(tree), c)) {
            throw new CompletionFailure(c, () -> diags.fragment(Fragments.CantResolveModules), dcfh);
        }

        enter.complete(List.of(tree), c);

        if (!taskListener.isEmpty()) {
            TaskEvent e = new TaskEvent(TaskEvent.Kind.ENTER, tree);
            taskListener.finished(e);
        }

        if (enter.getEnv(c) == null) {
            boolean isPkgInfo =
                tree.sourcefile.isNameCompatible("package-info",
                                                 JavaFileObject.Kind.SOURCE);
            boolean isModuleInfo =
                tree.sourcefile.isNameCompatible("module-info",
                                                 JavaFileObject.Kind.SOURCE);
            if (isModuleInfo) {
                if (enter.getEnv(tree.modle) == null) {
                    JCDiagnostic diag =
                        diagFactory.fragment(Fragments.FileDoesNotContainModule);
                    throw new ClassFinder.BadClassFile(c, filename, diag, diagFactory, dcfh);
                }
            } else if (isPkgInfo) {
                if (enter.getEnv(tree.packge) == null) {
                    JCDiagnostic diag =
                        diagFactory.fragment(Fragments.FileDoesNotContainPackage(c.location()));
                    throw new ClassFinder.BadClassFile(c, filename, diag, diagFactory, dcfh);
                }
            } else {
                JCDiagnostic diag =
                        diagFactory.fragment(Fragments.FileDoesntContainClass(c.getQualifiedName()));
                throw new ClassFinder.BadClassFile(c, filename, diag, diagFactory, dcfh);
            }
        }

        implicitSourceFilesRead = true;
    }

    /** Track when the JavaCompiler has been used to compile something. */
    private boolean hasBeenUsed = false;
    private long start_msec = 0;
    public long elapsed_msec = 0;

    public void compile(List<JavaFileObject> sourceFileObject)
        throws Throwable {
        compile(sourceFileObject, List.nil(), null, List.nil());
    }

    /**
     * Main method: compile a list of files, return all compiled classes
     *
     * @param sourceFileObjects file objects to be compiled
     * @param classnames class names to process for annotations
     * @param processors user provided annotation processors to bypass
     * discovery, {@code null} means that no processors were provided
     * @param addModules additional root modules to be used during
     * module resolution.
     */
    public void compile(Collection<JavaFileObject> sourceFileObjects,
                        Collection<String> classnames,
                        Iterable<? extends Processor> processors,
                        Collection<String> addModules)
    {
        if (!taskListener.isEmpty()) {
            taskListener.started(new TaskEvent(TaskEvent.Kind.COMPILATION));
        }

        if (processors != null && processors.iterator().hasNext())
            explicitAnnotationProcessingRequested = true;
        // as a JavaCompiler can only be used once, throw an exception if
        // it has been used before.
        if (hasBeenUsed)
            checkReusable();
        hasBeenUsed = true;

        // forcibly set the equivalent of -Xlint:-options, so that no further
        // warnings about command line options are generated from this point on
        options.put(XLINT_CUSTOM.primaryName + "-" + LintCategory.OPTIONS.option, "true");
        options.remove(XLINT_CUSTOM.primaryName + LintCategory.OPTIONS.option);

        start_msec = now();

        try {
            initProcessAnnotations(processors, sourceFileObjects, classnames);

            for (String className : classnames) {
                int sep = className.indexOf('/');
                if (sep != -1) {
                    modules.addExtraAddModules(className.substring(0, sep));
                }
            }

            for (String moduleName : addModules) {
                modules.addExtraAddModules(moduleName);
            }

            // These method calls must be chained to avoid memory leaks
            processAnnotations(
                enterTrees(
                        stopIfError(CompileState.ENTER,
                                initModules(stopIfError(CompileState.ENTER, parseFiles(sourceFileObjects))))
                ),
                classnames
            );

            // If it's safe to do so, skip attr / flow / gen for implicit classes
            if (taskListener.isEmpty() &&
                    implicitSourcePolicy == ImplicitSourcePolicy.NONE) {
                todo.retainFiles(inputFiles);
            }

            if (!CompileState.ATTR.isAfter(shouldStopPolicyIfNoError)) {
                switch (compilePolicy) {
                case SIMPLE:
                    generate(desugar(flow(attribute(todo))));
                    break;

                case BY_FILE: {
                        Queue<Queue<Env<AttrContext>>> q = todo.groupByFile();
                        while (!q.isEmpty() && !shouldStop(CompileState.ATTR)) {
                            generate(desugar(flow(attribute(q.remove()))));
                        }
                    }
                    break;

                case BY_TODO:
                    while (!todo.isEmpty())
                        generate(desugar(flow(attribute(todo.remove()))));
                    break;

                default:
                    Assert.error("unknown compile policy");
                }
            }
        } catch (Abort ex) {
            if (devVerbose)
                ex.printStackTrace(System.err);
        } finally {
            if (verbose) {
                elapsed_msec = elapsed(start_msec);
                log.printVerbose("total", Long.toString(elapsed_msec));
            }

            reportDeferredDiagnostics();

            if (!log.hasDiagnosticListener()) {
                printCount("error", errorCount());
                printCount("warn", warningCount());
                printSuppressedCount(errorCount(), log.nsuppressederrors, "count.error.recompile");
                printSuppressedCount(warningCount(), log.nsuppressedwarns, "count.warn.recompile");
            }
            if (!taskListener.isEmpty()) {
                taskListener.finished(new TaskEvent(TaskEvent.Kind.COMPILATION));
            }
            close();
            if (procEnvImpl != null)
                procEnvImpl.close();
        }
    }

    protected void checkReusable() {
        throw new AssertionError("attempt to reuse JavaCompiler");
    }

    /**
     * The list of classes explicitly supplied on the command line for compilation.
     * Not always populated.
     */
    private List<JCClassDecl> rootClasses;

    /**
     * Parses a list of files.
     */
   public List<JCCompilationUnit> parseFiles(Iterable<JavaFileObject> fileObjects) {
       return parseFiles(fileObjects, false);
   }

   public List<JCCompilationUnit> parseFiles(Iterable<JavaFileObject> fileObjects, boolean force) {
       if (!force && shouldStop(CompileState.PARSE))
           return List.nil();

        //parse all files
        ListBuffer<JCCompilationUnit> trees = new ListBuffer<>();
        Set<JavaFileObject> filesSoFar = new HashSet<>();
        for (JavaFileObject fileObject : fileObjects) {
            if (!filesSoFar.contains(fileObject)) {
                filesSoFar.add(fileObject);
                trees.append(parse(fileObject));
            }
        }
        return trees.toList();
    }

   /**
    * Returns true iff the compilation will continue after annotation processing
    * is done.
    */
    public boolean continueAfterProcessAnnotations() {
        return !shouldStop(CompileState.ATTR);
    }

    public List<JCCompilationUnit> initModules(List<JCCompilationUnit> roots) {
        modules.initModules(roots);
        if (roots.isEmpty()) {
            enterDone();
        }
        return roots;
    }

    /**
     * Enter the symbols found in a list of parse trees.
     * As a side-effect, this puts elements on the "todo" list.
     * Also stores a list of all top level classes in rootClasses.
     */
    public List<JCCompilationUnit> enterTrees(List<JCCompilationUnit> roots) {
        //enter symbols for all files
        if (!taskListener.isEmpty()) {
            for (JCCompilationUnit unit: roots) {
                TaskEvent e = new TaskEvent(TaskEvent.Kind.ENTER, unit);
                taskListener.started(e);
            }
        }

        enter.main(roots);

        enterDone();

        if (!taskListener.isEmpty()) {
            for (JCCompilationUnit unit: roots) {
                TaskEvent e = new TaskEvent(TaskEvent.Kind.ENTER, unit);
                taskListener.finished(e);
            }
        }

        // If generating source, or if tracking public apis,
        // then remember the classes declared in
        // the original compilation units listed on the command line.
        if (sourceOutput) {
            ListBuffer<JCClassDecl> cdefs = new ListBuffer<>();
            for (JCCompilationUnit unit : roots) {
                for (List<JCTree> defs = unit.defs;
                     defs.nonEmpty();
                     defs = defs.tail) {
                    if (defs.head instanceof JCClassDecl classDecl)
                        cdefs.append(classDecl);
                }
            }
            rootClasses = cdefs.toList();
        }

        // Ensure the input files have been recorded. Although this is normally
        // done by readSource, it may not have been done if the trees were read
        // in a prior round of annotation processing, and the trees have been
        // cleaned and are being reused.
        for (JCCompilationUnit unit : roots) {
            inputFiles.add(unit.sourcefile);
        }

        return roots;
    }

    /**
     * Set to true to enable skeleton annotation processing code.
     * Currently, we assume this variable will be replaced more
     * advanced logic to figure out if annotation processing is
     * needed.
     */
    boolean processAnnotations = false;

    Log.DeferredDiagnosticHandler deferredDiagnosticHandler;

    /**
     * Object to handle annotation processing.
     */
    private JavacProcessingEnvironment procEnvImpl = null;

    /**
     * Check if we should process annotations.
     * If so, and if no scanner is yet registered, then set up the DocCommentScanner
     * to catch doc comments, and set keepComments so the parser records them in
     * the compilation unit.
     *
     * @param processors user provided annotation processors to bypass
     * discovery, {@code null} means that no processors were provided
     */
    public void initProcessAnnotations(Iterable<? extends Processor> processors,
                                       Collection<? extends JavaFileObject> initialFiles,
                                       Collection<String> initialClassNames) {
        // Process annotations if processing is not disabled and there
        // is at least one Processor available.
        if (options.isSet(PROC, "none")) {
            processAnnotations = false;
        } else if (procEnvImpl == null) {
            procEnvImpl = JavacProcessingEnvironment.instance(context);
            procEnvImpl.setProcessors(processors);
            processAnnotations = procEnvImpl.atLeastOneProcessor();

            if (processAnnotations) {
                options.put("parameters", "parameters");
                reader.saveParameterNames = true;
                keepComments = true;
                genEndPos = true;
                if (!taskListener.isEmpty())
                    taskListener.started(new TaskEvent(TaskEvent.Kind.ANNOTATION_PROCESSING));
                deferredDiagnosticHandler = new Log.DeferredDiagnosticHandler(log);
                procEnvImpl.getFiler().setInitialState(initialFiles, initialClassNames);
            } else { // free resources
                procEnvImpl.close();
            }
        }
    }

    // TODO: called by JavacTaskImpl
    public void processAnnotations(List<JCCompilationUnit> roots) {
        processAnnotations(roots, List.nil());
    }

    /**
     * Process any annotations found in the specified compilation units.
     * @param roots a list of compilation units
     */
    // Implementation note: when this method is called, log.deferredDiagnostics
    // will have been set true by initProcessAnnotations, meaning that any diagnostics
    // that are reported will go into the log.deferredDiagnostics queue.
    // By the time this method exits, log.deferDiagnostics must be set back to false,
    // and all deferredDiagnostics must have been handled: i.e. either reported
    // or determined to be transient, and therefore suppressed.
    public void processAnnotations(List<JCCompilationUnit> roots,
                                   Collection<String> classnames) {
        if (shouldStop(CompileState.PROCESS)) {
            // Errors were encountered.
            // Unless all the errors are resolve errors, the errors were parse errors
            // or other errors during enter which cannot be fixed by running
            // any annotation processors.
            if (processAnnotations) {
                deferredDiagnosticHandler.reportDeferredDiagnostics();
                log.popDiagnosticHandler(deferredDiagnosticHandler);
                return ;
            }
        }

        // ASSERT: processAnnotations and procEnvImpl should have been set up by
        // by initProcessAnnotations

        // NOTE: The !classnames.isEmpty() checks should be refactored to Main.

        if (!processAnnotations) {
            // If there are no annotation processors present, and
            // annotation processing is to occur with compilation,
            // emit a warning.
            if (options.isSet(PROC, "only")) {
                log.warning(Warnings.ProcProcOnlyRequestedNoProcs);
                todo.clear();
            }
            // If not processing annotations, classnames must be empty
            if (!classnames.isEmpty()) {
                log.error(Errors.ProcNoExplicitAnnotationProcessingRequested(classnames));
            }
            Assert.checkNull(deferredDiagnosticHandler);
            return ; // continue regular compilation
        }

        Assert.checkNonNull(deferredDiagnosticHandler);

        try {
            List<ClassSymbol> classSymbols = List.nil();
            List<PackageSymbol> pckSymbols = List.nil();
            if (!classnames.isEmpty()) {
                 // Check for explicit request for annotation
                 // processing
                if (!explicitAnnotationProcessingRequested()) {
                    log.error(Errors.ProcNoExplicitAnnotationProcessingRequested(classnames));
                    deferredDiagnosticHandler.reportDeferredDiagnostics();
                    log.popDiagnosticHandler(deferredDiagnosticHandler);
                    return ; // TODO: Will this halt compilation?
                } else {
                    boolean errors = false;
                    for (String nameStr : classnames) {
                        Symbol sym = resolveBinaryNameOrIdent(nameStr);
                        if (sym == null ||
                            (sym.kind == PCK && !processPcks) ||
                            sym.kind == ABSENT_TYP) {
                            if (sym != silentFail)
                                log.error(Errors.ProcCantFindClass(nameStr));
                            errors = true;
                            continue;
                        }
                        try {
                            if (sym.kind == PCK)
                                sym.complete();
                            if (sym.exists()) {
                                if (sym.kind == PCK)
                                    pckSymbols = pckSymbols.prepend((PackageSymbol)sym);
                                else
                                    classSymbols = classSymbols.prepend((ClassSymbol)sym);
                                continue;
                            }
                            Assert.check(sym.kind == PCK);
                            log.warning(Warnings.ProcPackageDoesNotExist(nameStr));
                            pckSymbols = pckSymbols.prepend((PackageSymbol)sym);
                        } catch (CompletionFailure e) {
                            log.error(Errors.ProcCantFindClass(nameStr));
                            errors = true;
                            continue;
                        }
                    }
                    if (errors) {
                        deferredDiagnosticHandler.reportDeferredDiagnostics();
                        log.popDiagnosticHandler(deferredDiagnosticHandler);
                        return ;
                    }
                }
            }
            try {
                annotationProcessingOccurred =
                        procEnvImpl.doProcessing(roots,
                                                 classSymbols,
                                                 pckSymbols,
                                                 deferredDiagnosticHandler);
                // doProcessing will have handled deferred diagnostics
            } finally {
                procEnvImpl.close();
            }
        } catch (CompletionFailure ex) {
            log.error(Errors.CantAccess(ex.sym, ex.getDetailValue()));
            if (deferredDiagnosticHandler != null) {
                deferredDiagnosticHandler.reportDeferredDiagnostics();
                log.popDiagnosticHandler(deferredDiagnosticHandler);
            }
        }
    }

    private boolean unrecoverableError() {
        if (deferredDiagnosticHandler != null) {
            for (JCDiagnostic d: deferredDiagnosticHandler.getDiagnostics()) {
                if (d.getKind() == JCDiagnostic.Kind.ERROR && !d.isFlagSet(RECOVERABLE))
                    return true;
            }
        }
        return false;
    }

    boolean explicitAnnotationProcessingRequested() {
        return
            explicitAnnotationProcessingRequested ||
            explicitAnnotationProcessingRequested(options);
    }

    static boolean explicitAnnotationProcessingRequested(Options options) {
        return
            options.isSet(PROCESSOR) ||
            options.isSet(PROCESSOR_PATH) ||
            options.isSet(PROCESSOR_MODULE_PATH) ||
            options.isSet(PROC, "only") ||
            options.isSet(XPRINT);
    }

    public void setDeferredDiagnosticHandler(Log.DeferredDiagnosticHandler deferredDiagnosticHandler) {
        this.deferredDiagnosticHandler = deferredDiagnosticHandler;
    }

    /**
     * Attribute a list of parse trees, such as found on the "todo" list.
     * Note that attributing classes may cause additional files to be
     * parsed and entered via the SourceCompleter.
     * Attribution of the entries in the list does not stop if any errors occur.
     * @return a list of environments for attribute classes.
     */
    public Queue<Env<AttrContext>> attribute(Queue<Env<AttrContext>> envs) {
        ListBuffer<Env<AttrContext>> results = new ListBuffer<>();
        while (!envs.isEmpty())
            results.append(attribute(envs.remove()));
        return stopIfError(CompileState.ATTR, results);
    }

    /**
     * Attribute a parse tree.
     * @return the attributed parse tree
     */
    public Env<AttrContext> attribute(Env<AttrContext> env) {
        if (compileStates.isDone(env, CompileState.ATTR))
            return env;

        if (verboseCompilePolicy)
            printNote("[attribute " + env.enclClass.sym + "]");
        if (verbose)
            log.printVerbose("checking.attribution", env.enclClass.sym);

        if (!taskListener.isEmpty()) {
            TaskEvent e = new TaskEvent(TaskEvent.Kind.ANALYZE, env.toplevel, env.enclClass.sym);
            taskListener.started(e);
        }

        JavaFileObject prev = log.useSource(
                                  env.enclClass.sym.sourcefile != null ?
                                  env.enclClass.sym.sourcefile :
                                  env.toplevel.sourcefile);
        try {
            attr.attrib(env);
            if (errorCount() > 0 && !shouldStop(CompileState.ATTR)) {
                //if in fail-over mode, ensure that AST expression nodes
                //are correctly initialized (e.g. they have a type/symbol)
                attr.postAttr(env.tree);
            }
            compileStates.put(env, CompileState.ATTR);
        }
        finally {
            log.useSource(prev);
        }

        return env;
    }

    /**
     * Perform dataflow checks on attributed parse trees.
     * These include checks for definite assignment and unreachable statements.
     * If any errors occur, an empty list will be returned.
     * @return the list of attributed parse trees
     */
    public Queue<Env<AttrContext>> flow(Queue<Env<AttrContext>> envs) {
        ListBuffer<Env<AttrContext>> results = new ListBuffer<>();
        for (Env<AttrContext> env: envs) {
            flow(env, results);
        }
        return stopIfError(CompileState.FLOW, results);
    }

    /**
     * Perform dataflow checks on an attributed parse tree.
     */
    public Queue<Env<AttrContext>> flow(Env<AttrContext> env) {
        ListBuffer<Env<AttrContext>> results = new ListBuffer<>();
        flow(env, results);
        return stopIfError(CompileState.FLOW, results);
    }

    /**
     * Perform dataflow checks on an attributed parse tree.
     */
    protected void flow(Env<AttrContext> env, Queue<Env<AttrContext>> results) {
        if (compileStates.isDone(env, CompileState.FLOW)) {
            results.add(env);
            return;
        }

        try {
            if (shouldStop(CompileState.FLOW))
                return;

            if (verboseCompilePolicy)
                printNote("[flow " + env.enclClass.sym + "]");
            JavaFileObject prev = log.useSource(
                                                env.enclClass.sym.sourcefile != null ?
                                                env.enclClass.sym.sourcefile :
                                                env.toplevel.sourcefile);
            try {
                make.at(Position.FIRSTPOS);
                TreeMaker localMake = make.forToplevel(env.toplevel);
                flow.analyzeTree(env, localMake);
                compileStates.put(env, CompileState.FLOW);

                if (shouldStop(CompileState.FLOW))
                    return;

                analyzer.flush(env);

                results.add(env);
            }
            finally {
                log.useSource(prev);
            }
        }
        finally {
            if (!taskListener.isEmpty()) {
                TaskEvent e = new TaskEvent(TaskEvent.Kind.ANALYZE, env.toplevel, env.enclClass.sym);
                taskListener.finished(e);
            }
        }
    }

    /**
     * Prepare attributed parse trees, in conjunction with their attribution contexts,
     * for source or code generation.
     * If any errors occur, an empty list will be returned.
     * @return a list containing the classes to be generated
     */
    public Queue<Pair<Env<AttrContext>, JCClassDecl>> desugar(Queue<Env<AttrContext>> envs) {
        ListBuffer<Pair<Env<AttrContext>, JCClassDecl>> results = new ListBuffer<>();
        for (Env<AttrContext> env: envs)
            desugar(env, results);
        return stopIfError(CompileState.FLOW, results);
    }

    HashMap<Env<AttrContext>, Queue<Pair<Env<AttrContext>, JCClassDecl>>> desugaredEnvs = new HashMap<>();

    /**
     * Prepare attributed parse trees, in conjunction with their attribution contexts,
     * for source or code generation. If the file was not listed on the command line,
     * the current implicitSourcePolicy is taken into account.
     * The preparation stops as soon as an error is found.
     */
    protected void desugar(final Env<AttrContext> env, Queue<Pair<Env<AttrContext>, JCClassDecl>> results) {
        if (shouldStop(CompileState.TRANSTYPES))
            return;

        if (implicitSourcePolicy == ImplicitSourcePolicy.NONE
                && !inputFiles.contains(env.toplevel.sourcefile)) {
            return;
        }

        if (!modules.multiModuleMode && env.toplevel.modle != modules.getDefaultModule()) {
            //can only generate classfiles for a single module:
            return;
        }

        if (compileStates.isDone(env, CompileState.LOWER)) {
            results.addAll(desugaredEnvs.get(env));
            return;
        }

        /**
         * Ensure that superclasses of C are desugared before C itself. This is
         * required for two reasons: (i) as erasure (TransTypes) destroys
         * information needed in flow analysis and (ii) as some checks carried
         * out during lowering require that all synthetic fields/methods have
         * already been added to C and its superclasses.
         */
        class ScanNested extends TreeScanner {
            Set<Env<AttrContext>> dependencies = new LinkedHashSet<>();
            protected boolean hasLambdas;
            @Override
            public void visitClassDef(JCClassDecl node) {
                Type st = types.supertype(node.sym.type);
                boolean envForSuperTypeFound = false;
                while (!envForSuperTypeFound && st.hasTag(CLASS)) {
                    ClassSymbol c = st.tsym.outermostClass();
                    Env<AttrContext> stEnv = enter.getEnv(c);
                    if (stEnv != null && env != stEnv) {
                        if (dependencies.add(stEnv)) {
                            boolean prevHasLambdas = hasLambdas;
                            try {
                                scan(stEnv.tree);
                            } finally {
                                /*
                                 * ignore any updates to hasLambdas made during
                                 * the nested scan, this ensures an initialized
                                 * LambdaToMethod is available only to those
                                 * classes that contain lambdas
                                 */
                                hasLambdas = prevHasLambdas;
                            }
                        }
                        envForSuperTypeFound = true;
                    }
                    st = types.supertype(st);
                }
                super.visitClassDef(node);
            }
            @Override
            public void visitLambda(JCLambda tree) {
                hasLambdas = true;
                super.visitLambda(tree);
            }
            @Override
            public void visitReference(JCMemberReference tree) {
                hasLambdas = true;
                super.visitReference(tree);
            }
        }
        ScanNested scanner = new ScanNested();
        scanner.scan(env.tree);
        for (Env<AttrContext> dep: scanner.dependencies) {
        if (!compileStates.isDone(dep, CompileState.FLOW))
            desugaredEnvs.put(dep, desugar(flow(attribute(dep))));
        }

        //We need to check for error another time as more classes might
        //have been attributed and analyzed at this stage
        if (shouldStop(CompileState.TRANSTYPES))
            return;

        if (verboseCompilePolicy)
            printNote("[desugar " + env.enclClass.sym + "]");

        JavaFileObject prev = log.useSource(env.enclClass.sym.sourcefile != null ?
                                  env.enclClass.sym.sourcefile :
                                  env.toplevel.sourcefile);
        try {
            //save tree prior to rewriting
            JCTree untranslated = env.tree;

            make.at(Position.FIRSTPOS);
            TreeMaker localMake = make.forToplevel(env.toplevel);

            if (env.tree.hasTag(JCTree.Tag.PACKAGEDEF) || env.tree.hasTag(JCTree.Tag.MODULEDEF)) {
                if (!(sourceOutput)) {
                    if (shouldStop(CompileState.LOWER))
                        return;
                    List<JCTree> def = lower.translateTopLevelClass(env, env.tree, localMake);
                    if (def.head != null) {
                        Assert.check(def.tail.isEmpty());
                        results.add(new Pair<>(env, (JCClassDecl)def.head));
                    }
                }
                return;
            }

            if (shouldStop(CompileState.TRANSTYPES))
                return;

            env.tree = transTypes.translateTopLevelClass(env.tree, localMake);
            compileStates.put(env, CompileState.TRANSTYPES);

            if (shouldStop(CompileState.TRANSPATTERNS))
                return;

            env.tree = TransPatterns.instance(context).translateTopLevelClass(env, env.tree, localMake);
            compileStates.put(env, CompileState.TRANSPATTERNS);

            if (Feature.LAMBDA.allowedInSource(source) && scanner.hasLambdas) {
                if (shouldStop(CompileState.UNLAMBDA))
                    return;

                env.tree = LambdaToMethod.instance(context).translateTopLevelClass(env, env.tree, localMake);
                compileStates.put(env, CompileState.UNLAMBDA);
            }

            if (shouldStop(CompileState.LOWER))
                return;

            if (sourceOutput) {
                //emit standard Java source file, only for compilation
                //units enumerated explicitly on the command line
                JCClassDecl cdef = (JCClassDecl)env.tree;
                if (untranslated instanceof JCClassDecl classDecl &&
                    rootClasses.contains(classDecl)) {
                    results.add(new Pair<>(env, cdef));
                }
                return;
            }

            //translate out inner classes
            List<JCTree> cdefs = lower.translateTopLevelClass(env, env.tree, localMake);
            compileStates.put(env, CompileState.LOWER);

            if (shouldStop(CompileState.LOWER))
                return;

            //generate code for each class
            for (List<JCTree> l = cdefs; l.nonEmpty(); l = l.tail) {
                JCClassDecl cdef = (JCClassDecl)l.head;
                results.add(new Pair<>(env, cdef));
            }
        }
        finally {
            log.useSource(prev);
        }

    }

    /** Generates the source or class file for a list of classes.
     * The decision to generate a source file or a class file is
     * based upon the compiler's options.
     * Generation stops if an error occurs while writing files.
     */
    public void generate(Queue<Pair<Env<AttrContext>, JCClassDecl>> queue) {
        generate(queue, null);
    }

    public void generate(Queue<Pair<Env<AttrContext>, JCClassDecl>> queue, Queue<JavaFileObject> results) {
        if (shouldStop(CompileState.GENERATE))
            return;

        for (Pair<Env<AttrContext>, JCClassDecl> x: queue) {
            Env<AttrContext> env = x.fst;
            JCClassDecl cdef = x.snd;

            if (verboseCompilePolicy) {
                printNote("[generate " + (sourceOutput ? " source" : "code") + " " + cdef.sym + "]");
            }

            if (!taskListener.isEmpty()) {
                TaskEvent e = new TaskEvent(TaskEvent.Kind.GENERATE, env.toplevel, cdef.sym);
                taskListener.started(e);
            }

            JavaFileObject prev = log.useSource(env.enclClass.sym.sourcefile != null ?
                                      env.enclClass.sym.sourcefile :
                                      env.toplevel.sourcefile);
            try {
                JavaFileObject file;
                if (sourceOutput) {
                    file = printSource(env, cdef);
                } else {
                    if (fileManager.hasLocation(StandardLocation.NATIVE_HEADER_OUTPUT)
                            && jniWriter.needsHeader(cdef.sym)) {
                        jniWriter.write(cdef.sym);
                    }
                    file = genCode(env, cdef);
                }
                if (results != null && file != null)
                    results.add(file);
            } catch (IOException ex) {
                log.error(cdef.pos(),
                          Errors.ClassCantWrite(cdef.sym, ex.getMessage()));
                return;
            } finally {
                log.useSource(prev);
            }

            if (!taskListener.isEmpty()) {
                TaskEvent e = new TaskEvent(TaskEvent.Kind.GENERATE, env.toplevel, cdef.sym);
                taskListener.finished(e);
            }
        }
    }

        // where
        Map<JCCompilationUnit, Queue<Env<AttrContext>>> groupByFile(Queue<Env<AttrContext>> envs) {
            // use a LinkedHashMap to preserve the order of the original list as much as possible
            Map<JCCompilationUnit, Queue<Env<AttrContext>>> map = new LinkedHashMap<>();
            for (Env<AttrContext> env: envs) {
                Queue<Env<AttrContext>> sublist = map.get(env.toplevel);
                if (sublist == null) {
                    sublist = new ListBuffer<>();
                    map.put(env.toplevel, sublist);
                }
                sublist.add(env);
            }
            return map;
        }

        JCClassDecl removeMethodBodies(JCClassDecl cdef) {
            final boolean isInterface = (cdef.mods.flags & Flags.INTERFACE) != 0;
            class MethodBodyRemover extends TreeTranslator {
                @Override
                public void visitMethodDef(JCMethodDecl tree) {
                    tree.mods.flags &= ~Flags.SYNCHRONIZED;
                    for (JCVariableDecl vd : tree.params)
                        vd.mods.flags &= ~Flags.FINAL;
                    tree.body = null;
                    super.visitMethodDef(tree);
                }
                @Override
                public void visitVarDef(JCVariableDecl tree) {
                    if (tree.init != null && tree.init.type.constValue() == null)
                        tree.init = null;
                    super.visitVarDef(tree);
                }
                @Override
                public void visitClassDef(JCClassDecl tree) {
                    ListBuffer<JCTree> newdefs = new ListBuffer<>();
                    for (List<JCTree> it = tree.defs; it.tail != null; it = it.tail) {
                        JCTree t = it.head;
                        switch (t.getTag()) {
                        case CLASSDEF:
                            if (isInterface ||
                                (((JCClassDecl) t).mods.flags & (Flags.PROTECTED|Flags.PUBLIC)) != 0 ||
                                (((JCClassDecl) t).mods.flags & (Flags.PRIVATE)) == 0 && ((JCClassDecl) t).sym.packge().getQualifiedName() == names.java_lang)
                                newdefs.append(t);
                            break;
                        case METHODDEF:
                            if (isInterface ||
                                (((JCMethodDecl) t).mods.flags & (Flags.PROTECTED|Flags.PUBLIC)) != 0 ||
                                ((JCMethodDecl) t).sym.name == names.init ||
                                (((JCMethodDecl) t).mods.flags & (Flags.PRIVATE)) == 0 && ((JCMethodDecl) t).sym.packge().getQualifiedName() == names.java_lang)
                                newdefs.append(t);
                            break;
                        case VARDEF:
                            if (isInterface || (((JCVariableDecl) t).mods.flags & (Flags.PROTECTED|Flags.PUBLIC)) != 0 ||
                                (((JCVariableDecl) t).mods.flags & (Flags.PRIVATE)) == 0 && ((JCVariableDecl) t).sym.packge().getQualifiedName() == names.java_lang)
                                newdefs.append(t);
                            break;
                        default:
                            break;
                        }
                    }
                    tree.defs = newdefs.toList();
                    super.visitClassDef(tree);
                }
            }
            MethodBodyRemover r = new MethodBodyRemover();
            return r.translate(cdef);
        }

    public void reportDeferredDiagnostics() {
        if (errorCount() == 0
                && annotationProcessingOccurred
                && implicitSourceFilesRead
                && implicitSourcePolicy == ImplicitSourcePolicy.UNSET) {
            if (explicitAnnotationProcessingRequested())
                log.warning(Warnings.ProcUseImplicit);
            else
                log.warning(Warnings.ProcUseProcOrImplicit);
        }
        chk.reportDeferredDiagnostics();
        preview.reportDeferredDiagnostics();
        if (log.compressedOutput) {
            log.mandatoryNote(null, Notes.CompressedDiags);
        }
    }

    public void enterDone() {
        enterDone = true;
        annotate.enterDone();
    }

    public boolean isEnterDone() {
        return enterDone;
    }

    private Name readModuleName(JavaFileObject fo) {
        return parseAndGetName(fo, t -> {
            JCModuleDecl md = t.getModuleDecl();

            return md != null ? TreeInfo.fullName(md.getName()) : null;
        });
    }

    private Name findPackageInFile(JavaFileObject fo) {
        return parseAndGetName(fo, t -> t.getPackage() != null ?
                                        TreeInfo.fullName(t.getPackage().getPackageName()) : null);
    }

    private Name parseAndGetName(JavaFileObject fo,
                                 Function<JCTree.JCCompilationUnit, Name> tree2Name) {
        DiagnosticHandler dh = new DiscardDiagnosticHandler(log);
        JavaFileObject prevSource = log.useSource(fo);
        try {
            JCTree.JCCompilationUnit t = parse(fo, fo.getCharContent(false));
            return tree2Name.apply(t);
        } catch (IOException e) {
            return null;
        } finally {
            log.popDiagnosticHandler(dh);
            log.useSource(prevSource);
        }
    }

    /** Close the compiler, flushing the logs
     */
    public void close() {
        rootClasses = null;
        finder = null;
        reader = null;
        make = null;
        writer = null;
        enter = null;
        if (todo != null)
            todo.clear();
        todo = null;
        parserFactory = null;
        syms = null;
        source = null;
        attr = null;
        chk = null;
        gen = null;
        flow = null;
        transTypes = null;
        lower = null;
        annotate = null;
        types = null;

        log.flush();
        try {
            fileManager.flush();
        } catch (IOException e) {
            throw new Abort(e);
        } finally {
            if (names != null)
                names.dispose();
            names = null;

            FatalError fatalError = null;
            for (Closeable c: closeables) {
                try {
                    c.close();
                } catch (IOException e) {
                    if (fatalError == null) {
                        JCDiagnostic msg = diagFactory.fragment(Fragments.FatalErrCantClose);
                        fatalError = new FatalError(msg, e);
                    } else {
                        fatalError.addSuppressed(e);
                    }
                }
            }
            if (fatalError != null) {
                throw fatalError;
            }
            closeables = List.nil();
        }
    }

    protected void printNote(String lines) {
        log.printRawLines(Log.WriterKind.NOTICE, lines);
    }

    /** Print numbers of errors and warnings.
     */
    public void printCount(String kind, int count) {
        if (count != 0) {
            String key;
            if (count == 1)
                key = "count." + kind;
            else
                key = "count." + kind + ".plural";
            log.printLines(WriterKind.ERROR, key, String.valueOf(count));
            log.flush(Log.WriterKind.ERROR);
        }
    }

    private void printSuppressedCount(int shown, int suppressed, String diagKey) {
        if (suppressed > 0) {
            int total = shown + suppressed;
            log.printLines(WriterKind.ERROR, diagKey,
                    String.valueOf(shown), String.valueOf(total));
            log.flush(Log.WriterKind.ERROR);
        }
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    private static long elapsed(long then) {
        return now() - then;
    }

    public void newRound() {
        inputFiles.clear();
        todo.clear();
    }
}
