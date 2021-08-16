/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jshell;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.util.Context;
import java.util.ArrayList;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import static jdk.jshell.Util.*;
import com.sun.source.tree.ImportTree;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.util.JavacMessages;
import jdk.jshell.MemoryFileManager.OutputMemoryJavaFileObject;
import java.util.Collections;
import java.util.Locale;
import static javax.tools.StandardLocation.CLASS_OUTPUT;
import static jdk.internal.jshell.debug.InternalDebugControl.DBG_GEN;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import javax.lang.model.util.Elements;
import javax.tools.FileObject;
import jdk.jshell.MemoryFileManager.SourceMemoryJavaFileObject;
import java.lang.Runtime.Version;
import java.nio.CharBuffer;
import java.util.function.BiFunction;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.JavacTaskPool;
import com.sun.tools.javac.code.ClassFinder;
import com.sun.tools.javac.code.Kinds;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.ModuleSymbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.parser.Parser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCTypeCast;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.Tag;
import com.sun.tools.javac.util.Context.Factory;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Log.DiscardDiagnosticHandler;
import com.sun.tools.javac.util.Names;
import static jdk.internal.jshell.debug.InternalDebugControl.DBG_FMGR;
import jdk.jshell.Snippet.Status;

/**
 * The primary interface to the compiler API.  Parsing, analysis, and
 * compilation to class files (in memory).
 * @author Robert Field
 */
class TaskFactory {

    private final JavaCompiler compiler;
    private final MemoryFileManager fileManager;
    private final JShell state;
    private String classpath = System.getProperty("java.class.path");
    private static final Version INITIAL_SUPPORTED_VER = Version.parse("9");

    TaskFactory(JShell state) {
        this.state = state;
        this.compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new UnsupportedOperationException("Compiler not available, must be run with full JDK 9.");
        }
        Version current = Version.parse(System.getProperty("java.specification.version"));
        if (INITIAL_SUPPORTED_VER.compareToIgnoreOptional(current) > 0)  {
            throw new UnsupportedOperationException("Wrong compiler, must be run with full JDK 9.");
        }
        this.fileManager = new MemoryFileManager(
                compiler.getStandardFileManager(null, null, null), state);
        initTaskPool();
    }

    void addToClasspath(String path) {
        classpath = classpath + File.pathSeparator + path;
        List<String> args = new ArrayList<>();
        args.add(classpath);
        fileManager().handleOption("-classpath", args.iterator());
        initTaskPool();
    }

    MemoryFileManager fileManager() {
        return fileManager;
    }

    public <Z> Z parse(String source,
                       boolean forceExpression,
                       Worker<ParseTask, Z> worker) {
        StringSourceHandler sh = new StringSourceHandler();
        return runTask(Stream.of(source),
                       sh,
                       List.of("-XDallowStringFolding=false", "-proc:none",
                               "-XDneedsReplParserFactory=" + forceExpression),
                       (jti, diagnostics) -> new ParseTask(sh, jti, diagnostics, forceExpression),
                       worker);
    }

    public <Z> Z analyze(OuterWrap wrap,
                         Worker<AnalyzeTask, Z> worker) {
        return analyze(Collections.singletonList(wrap), worker);
    }

    public <Z> Z analyze(OuterWrap wrap,
                         List<String> extraArgs,
                         Worker<AnalyzeTask, Z> worker) {
        return analyze(Collections.singletonList(wrap), extraArgs, worker);
    }

    public <Z> Z analyze(Collection<OuterWrap> wraps,
                         Worker<AnalyzeTask, Z> worker) {
        return analyze(wraps, Collections.emptyList(), worker);
    }

    public <Z> Z analyze(Collection<OuterWrap> wraps,
                         List<String> extraArgs,
                         Worker<AnalyzeTask, Z> worker) {
        WrapSourceHandler sh = new WrapSourceHandler();
        List<String> allOptions = new ArrayList<>();

        allOptions.add("--should-stop=at=FLOW");
        allOptions.add("-Xlint:unchecked,-strictfp");
        allOptions.add("-proc:none");
        allOptions.addAll(extraArgs);

        return runTask(wraps.stream(),
                       sh,
                       allOptions,
                       (jti, diagnostics) -> new AnalyzeTask(sh, jti, diagnostics),
                       worker);
    }

    public <Z> Z compile(Collection<OuterWrap> wraps,
                         Worker<CompileTask, Z> worker) {
        WrapSourceHandler sh = new WrapSourceHandler();

        return runTask(wraps.stream(),
                       sh,
                       List.of("-Xlint:unchecked,-strictfp", "-proc:none", "-parameters"),
                       (jti, diagnostics) -> new CompileTask(sh, jti, diagnostics),
                       worker);
    }

    private <S, T extends BaseTask, Z> Z runTask(Stream<S> inputs,
                                                 SourceHandler<S> sh,
                                                 List<String> options,
                                                 BiFunction<JavacTaskImpl, DiagnosticCollector<JavaFileObject>, T> creator,
                                                 Worker<T, Z> worker) {
            List<String> allOptions = new ArrayList<>(options.size() + state.extraCompilerOptions.size());
            allOptions.addAll(options);
            allOptions.addAll(state.extraCompilerOptions);
            Iterable<? extends JavaFileObject> compilationUnits = inputs
                            .map(in -> sh.sourceToFileObject(fileManager, in))
                            .toList();
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            state.debug(DBG_FMGR, "Task (%s %s) Options: %s\n", this, compilationUnits, allOptions);
            return javacTaskPool.getTask(null, fileManager, diagnostics, allOptions, null,
                                         compilationUnits, task -> {
                 JavacTaskImpl jti = (JavacTaskImpl) task;
                 Context context = jti.getContext();
                 DisableAccessibilityResolve.preRegister(context);
                 jti.addTaskListener(new TaskListenerImpl(context, state));
                 try {
                     return worker.withTask(creator.apply(jti, diagnostics));
                 } finally {
                     //additional cleanup: purge the REPL package:
                     Symtab syms = Symtab.instance(context);
                     Names names = Names.instance(context);
                     ModuleSymbol replModule = syms.java_base == syms.noModule ? syms.noModule
                                                                               : syms.unnamedModule;
                     PackageSymbol repl = syms.getPackage(replModule, names.fromString(Util.REPL_PACKAGE));
                     if (repl != null) {
                         for (ClassSymbol clazz : syms.getAllClasses()) {
                             if (clazz.packge() == repl) {
                                 syms.removeClass(replModule, clazz.flatName());
                             }
                         }
                         repl.members_field = null;
                         repl.completer = ClassFinder.instance(context).getCompleter();
                     }
                 }
            });
    }

    interface Worker<T extends BaseTask, Z> {
        public Z withTask(T task);
    }

    // Parse a snippet and return our parse task handler
    <Z> Z parse(final String source, Worker<ParseTask, Z> worker) {
        return parse(source, false, pt -> {
            if (!pt.units().isEmpty()
                    && pt.units().get(0).getKind() == Kind.EXPRESSION_STATEMENT
                    && pt.getDiagnostics().hasOtherThanNotStatementErrors()) {
                // It failed, it may be an expression being incorrectly
                // parsed as having a leading type variable, example:   a < b
                // Try forcing interpretation as an expression
                return parse(source, true, ept -> {
                    if (!ept.getDiagnostics().hasOtherThanNotStatementErrors()) {
                        return worker.withTask(ept);
                    } else {
                        return worker.withTask(pt);
                    }
                });
            }
            return worker.withTask(pt);
        });
    }

    private interface SourceHandler<T> {

        JavaFileObject sourceToFileObject(MemoryFileManager fm, T t);

        Diag diag(Diagnostic<? extends JavaFileObject> d);
    }

    private class StringSourceHandler implements SourceHandler<String> {

        @Override
        public JavaFileObject sourceToFileObject(MemoryFileManager fm, String src) {
            return fm.createSourceFileObject(src, "$NeverUsedName$", src);
        }

        @Override
        public Diag diag(final Diagnostic<? extends JavaFileObject> d) {
            return new Diag() {

                @Override
                public boolean isError() {
                    return d.getKind() == Diagnostic.Kind.ERROR;
                }

                @Override
                public long getPosition() {
                    return d.getPosition();
                }

                @Override
                public long getStartPosition() {
                    return d.getStartPosition();
                }

                @Override
                public long getEndPosition() {
                    return d.getEndPosition();
                }

                @Override
                public String getCode() {
                    return d.getCode();
                }

                @Override
                public String getMessage(Locale locale) {
                    return expunge(d.getMessage(locale));
                }
            };
        }
    }

    private class WrapSourceHandler implements SourceHandler<OuterWrap> {

        @Override
        public JavaFileObject sourceToFileObject(MemoryFileManager fm, OuterWrap w) {
            return fm.createSourceFileObject(w, w.classFullName(), w.wrapped());
        }

        /**
         * Get the source information from the wrap.  If this is external, or
         * otherwise does not have wrap info, just use source code.
         * @param d the Diagnostic from the compiler
         * @return the corresponding Diag
         */
        @Override
        public Diag diag(Diagnostic<? extends JavaFileObject> d) {
            JavaFileObject jfo = d.getSource();
            return jfo instanceof SourceMemoryJavaFileObject
                    ? ((OuterWrap) ((SourceMemoryJavaFileObject) jfo).getOrigin()).wrapDiag(d)
                    : new StringSourceHandler().diag(d);
        }
    }

    /**
     * Parse a snippet of code (as a String) using the parser subclass.  Return
     * the parse tree (and errors).
     */
    class ParseTask extends BaseTask {

        private final Iterable<? extends CompilationUnitTree> cuts;
        private final List<? extends Tree> units;

        private ParseTask(SourceHandler<String> sh,
                          JavacTaskImpl task,
                          DiagnosticCollector<JavaFileObject> diagnostics,
                          boolean forceExpression) {
            super(sh, task, diagnostics);
            ReplParserFactory.preRegister(context, forceExpression);
            cuts = parse();
            units = Util.stream(cuts)
                    .flatMap(cut -> {
                        List<? extends ImportTree> imps = cut.getImports();
                        return (!imps.isEmpty() ? imps : cut.getTypeDecls()).stream();
                    })
                    .toList();
        }

        private Iterable<? extends CompilationUnitTree> parse() {
            try {
                return task.parse();
            } catch (Exception ex) {
                throw new InternalError("Exception during parse - " + ex.getMessage(), ex);
            }
        }

        List<? extends Tree> units() {
            return units;
        }

        @Override
        Iterable<? extends CompilationUnitTree> cuTrees() {
            return cuts;
        }
    }

    /**
     * Run the normal "analyze()" pass of the compiler over the wrapped snippet.
     */
    class AnalyzeTask extends BaseTask {

        private final Iterable<? extends CompilationUnitTree> cuts;

        private AnalyzeTask(SourceHandler<OuterWrap> sh,
                            JavacTaskImpl task,
                            DiagnosticCollector<JavaFileObject> diagnostics) {
            super(sh, task, diagnostics);
            cuts = analyze();
        }

        private Iterable<? extends CompilationUnitTree> analyze() {
            try {
                Iterable<? extends CompilationUnitTree> cuts = task.parse();
                task.analyze();
                return cuts;
            } catch (Exception ex) {
                throw new InternalError("Exception during analyze - " + ex.getMessage(), ex);
            }
        }

        @Override
        Iterable<? extends CompilationUnitTree> cuTrees() {
            return cuts;
        }

        Elements getElements() {
            return task.getElements();
        }

        javax.lang.model.util.Types getTypes() {
            return task.getTypes();
        }
    }

    /**
     * Unit the wrapped snippet to class files.
     */
    class CompileTask extends BaseTask {

        private final Map<OuterWrap, List<OutputMemoryJavaFileObject>> classObjs = new HashMap<>();

        CompileTask(SourceHandler<OuterWrap>sh,
                    JavacTaskImpl jti,
                    DiagnosticCollector<JavaFileObject> diagnostics) {
            super(sh, jti, diagnostics);
        }

        boolean compile() {
            fileManager.registerClassFileCreationListener(this::listenForNewClassFile);
            boolean result = task.call();
            fileManager.registerClassFileCreationListener(null);
            return result;
        }

        // Returns the list of classes generated during this compile.
        // Stores the mapping between class name and current compiled bytes.
        List<String> classList(OuterWrap w) {
            List<OutputMemoryJavaFileObject> l = classObjs.get(w);
            if (l == null) {
                return Collections.emptyList();
            }
            List<String> list = new ArrayList<>();
            for (OutputMemoryJavaFileObject fo : l) {
                state.classTracker.setCurrentBytes(fo.getName(), fo.getBytes());
                list.add(fo.getName());
            }
            return list;
        }

        private void listenForNewClassFile(OutputMemoryJavaFileObject jfo, JavaFileManager.Location location,
                String className, JavaFileObject.Kind kind, FileObject sibling) {
            //debug("listenForNewClassFile %s loc=%s kind=%s\n", className, location, kind);
            if (location == CLASS_OUTPUT) {
                state.debug(DBG_GEN, "Compiler generating class %s\n", className);
                OuterWrap w = ((sibling instanceof SourceMemoryJavaFileObject)
                        && (((SourceMemoryJavaFileObject) sibling).getOrigin() instanceof OuterWrap))
                        ? (OuterWrap) ((SourceMemoryJavaFileObject) sibling).getOrigin()
                        : null;
                classObjs.compute(w, (k, v) -> (v == null)? new ArrayList<>() : v)
                        .add(jfo);
            }
        }

        @Override
        Iterable<? extends CompilationUnitTree> cuTrees() {
            throw new UnsupportedOperationException("Not supported.");
        }
    }

    private JavacTaskPool javacTaskPool;

    private void initTaskPool() {
        javacTaskPool = new JavacTaskPool(5);
    }

    abstract class BaseTask {

        final DiagnosticCollector<JavaFileObject> diagnostics;
        final JavacTaskImpl task;
        private DiagList diags = null;
        private final SourceHandler<?> sourceHandler;
        final Context context;
        private Types types;
        private JavacMessages messages;
        private Trees trees;

        private <T>BaseTask(SourceHandler<T> sh,
                            JavacTaskImpl task,
                            DiagnosticCollector<JavaFileObject> diagnostics) {
            this.sourceHandler = sh;
            this.task = task;
            context = task.getContext();
            this.diagnostics = diagnostics;
        }

        abstract Iterable<? extends CompilationUnitTree> cuTrees();

        CompilationUnitTree firstCuTree() {
            return cuTrees().iterator().next();
        }

        Diag diag(Diagnostic<? extends JavaFileObject> diag) {
            return sourceHandler.diag(diag);
        }

        Context getContext() {
            return context;
        }

        Types types() {
            if (types == null) {
                types = Types.instance(context);
            }
            return types;
        }

        JavacMessages messages() {
            if (messages == null) {
                messages = JavacMessages.instance(context);
            }
            return messages;
        }

        Trees trees() {
            if (trees == null) {
                trees = Trees.instance(task);
            }
            return trees;
        }

        // ------------------ diags functionality

        DiagList getDiagnostics() {
            if (diags == null) {
                LinkedHashMap<String, Diag> diagMap = new LinkedHashMap<>();
                for (Diagnostic<? extends JavaFileObject> in : diagnostics.getDiagnostics()) {
                    Diag d = diag(in);
                    String uniqueKey = d.getCode() + ":" + d.getPosition() + ":" + d.getMessage(PARSED_LOCALE);
                    diagMap.put(uniqueKey, d);
                }
                diags = new DiagList(diagMap.values());
            }
            return diags;
        }

        boolean hasErrors() {
            return getDiagnostics().hasErrors();
        }

        String shortErrorMessage() {
            StringBuilder sb = new StringBuilder();
            for (Diag diag : getDiagnostics()) {
                for (String line : diag.getMessage(PARSED_LOCALE).split("\\r?\\n")) {
                    if (!line.trim().startsWith("location:")) {
                        sb.append(line);
                    }
                }
            }
            return sb.toString();
        }

        void debugPrintDiagnostics(String src) {
            for (Diag diag : getDiagnostics()) {
                state.debug(DBG_GEN, "ERROR --\n");
                for (String line : diag.getMessage(PARSED_LOCALE).split("\\r?\\n")) {
                    if (!line.trim().startsWith("location:")) {
                        state.debug(DBG_GEN, "%s\n", line);
                    }
                }
                int start = (int) diag.getStartPosition();
                int end = (int) diag.getEndPosition();
                if (src != null) {
                    String[] srcLines = src.split("\\r?\\n");
                    for (String line : srcLines) {
                        state.debug(DBG_GEN, "%s\n", line);
                    }

                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < start; ++i) {
                        sb.append(' ');
                    }
                    sb.append('^');
                    if (end > start) {
                        for (int i = start + 1; i < end; ++i) {
                            sb.append('-');
                        }
                        sb.append('^');
                    }
                    state.debug(DBG_GEN, "%s\n", sb.toString());
                }
                state.debug(DBG_GEN, "printDiagnostics start-pos = %d ==> %d -- wrap = %s\n",
                        diag.getStartPosition(), start, this);
                state.debug(DBG_GEN, "Code: %s\n", diag.getCode());
                state.debug(DBG_GEN, "Pos: %d (%d - %d) -- %s\n", diag.getPosition(),
                        diag.getStartPosition(), diag.getEndPosition(), diag.getMessage(null));
            }
        }
    }

    /**The variable types inferred for "var"s may be non-denotable.
     * jshell desugars these variables into fields, and fields must have
     * a denotable type. So these fields are declared with some simpler denotable
     * type, and the listener here enhances the types of the fields to be the full
     * inferred types. This is mainly when the inferred type contains:
     * -intersection types (e.g. <Z extends Runnable&CharSequence> Z get() {...} var z = get();)
     * -types that are inaccessible at the given place
     *
     * This type enhancement does not need to do anything about anonymous classes, as these
     * are desugared into member classes.
     */
    private static final class TaskListenerImpl implements TaskListener {

        private final Context context;
        private final JShell state;
        /* Keep the original (declaration) types of the fields that were enhanced.
         * The declaration types need to be put back before writing the fields
         * into classfiles.*/
        private final Map<VarSymbol, Type> var2OriginalType = new HashMap<>();

        public TaskListenerImpl(Context context, JShell state) {
            this.context = context;
            this.state = state;
        }

        @Override
        public void started(TaskEvent e) {
            if (e.getKind() != TaskEvent.Kind.GENERATE)
                return ;
            //clear enhanced types in fields we are about to write to the classfiles:
            for (Tree clazz : e.getCompilationUnit().getTypeDecls()) {
                ClassTree ct = (ClassTree) clazz;

                for (Tree member : ct.getMembers()) {
                    if (member.getKind() != Tree.Kind.VARIABLE)
                        continue;
                    VarSymbol vsym = ((JCVariableDecl) member).sym;
                    Type original = var2OriginalType.remove(vsym);
                    if (original != null) {
                        vsym.type = original;
                    }
                }
            }
        }

        private boolean variablesSet = false;

        @Override
        public void finished(TaskEvent e) {
            if (e.getKind() != TaskEvent.Kind.ENTER || variablesSet)
                return ;
            state.maps
                 .snippetList()
                 .stream()
                 .filter(s -> s.status() == Status.VALID)
                 .filter(s -> s.kind() == Snippet.Kind.VAR)
                 .filter(s -> s.subKind() == Snippet.SubKind.VAR_DECLARATION_WITH_INITIALIZER_SUBKIND ||
                              s.subKind() == Snippet.SubKind.TEMP_VAR_EXPRESSION_SUBKIND)
                 .forEach(s -> setVariableType((VarSnippet) s));
            variablesSet = true;
        }

        /* If the snippet contain enhanced types, enhance the type of
         * the variable from snippet s to be the enhanced type.
         */
        private void setVariableType(VarSnippet s) {
            String typeName = s.fullTypeName;

            if (typeName == null)
                return ;

            Symtab syms = Symtab.instance(context);
            Names names = Names.instance(context);
            Log log  = Log.instance(context);
            ParserFactory parserFactory = ParserFactory.instance(context);
            Attr attr = Attr.instance(context);
            Enter enter = Enter.instance(context);
            DisableAccessibilityResolve rs = (DisableAccessibilityResolve) Resolve.instance(context);

            //find the variable:
            ClassSymbol clazz = syms.getClass(syms.unnamedModule, names.fromString(s.classFullName()));
            if (clazz == null || !clazz.isCompleted())
                return;
            VarSymbol field = (VarSymbol) clazz.members().findFirst(names.fromString(s.name()), sym -> sym.kind == Kinds.Kind.VAR);

            if (field != null && !var2OriginalType.containsKey(field)) {
                //if it was not enhanced yet:
                //ignore any errors:
                JavaFileObject prev = log.useSource(null);
                DiscardDiagnosticHandler h = new DiscardDiagnosticHandler(log);
                try {
                    //parse the type as a cast, i.e. "(<typeName>) x". This is to support
                    //intersection types:
                    CharBuffer buf = CharBuffer.wrap(("(" + typeName +")x\u0000").toCharArray(), 0, typeName.length() + 3);
                    Parser parser = parserFactory.newParser(buf, false, false, false);
                    JCExpression expr = parser.parseExpression();
                    if (expr.hasTag(Tag.TYPECAST)) {
                        //if parsed OK, attribute and set the type:
                        var2OriginalType.put(field, field.type);

                        JCTypeCast tree = (JCTypeCast) expr;
                        rs.runWithoutAccessChecks(() -> {
                            field.type = attr.attribType(tree.clazz,
                                                         enter.getEnvs().iterator().next().enclClass.sym);
                        });
                    }
                } finally {
                    log.popDiagnosticHandler(h);
                    log.useSource(prev);
                }
            }
        }
    }

    private static final class DisableAccessibilityResolve extends Resolve {

        public static void preRegister(Context context) {
            if (context.get(Marker.class) == null) {
                context.put(resolveKey, ((Factory<Resolve>) c -> new DisableAccessibilityResolve(c)));
                context.put(Marker.class, new Marker());
            }
        }

        private boolean noAccessChecks;

        public DisableAccessibilityResolve(Context context) {
            super(context);
        }

        /**Run the given Runnable with all access checks disabled.
         *
         * @param r Runnnable to run
         */
        public void runWithoutAccessChecks(Runnable r) {
            boolean prevNoAccessCheckes = noAccessChecks;
            try {
                noAccessChecks = true;
                r.run();
            } finally {
                noAccessChecks = prevNoAccessCheckes;
            }
        }

        @Override
        public boolean isAccessible(Env<AttrContext> env, TypeSymbol c, boolean checkInner) {
            if (noAccessChecks) return true;
            return super.isAccessible(env, c, checkInner);
        }

        @Override
        public boolean isAccessible(Env<AttrContext> env, Type site, Symbol sym, boolean checkInner) {
            if (noAccessChecks) return true;
            return super.isAccessible(env, site, sym, checkInner);
        }

        private static final class Marker {}
    }

}
