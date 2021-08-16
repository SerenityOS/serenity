/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Kinds.Kind;
import com.sun.tools.javac.code.Source;
import com.sun.tools.javac.code.Source.Feature;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.comp.ArgumentAttr.LocalCacheContext;
import com.sun.tools.javac.comp.DeferredAttr.AttributionMode;
import com.sun.tools.javac.resources.CompilerProperties.Warnings;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCDoWhileLoop;
import com.sun.tools.javac.tree.JCTree.JCEnhancedForLoop;
import com.sun.tools.javac.tree.JCTree.JCForLoop;
import com.sun.tools.javac.tree.JCTree.JCIf;
import com.sun.tools.javac.tree.JCTree.JCLambda;
import com.sun.tools.javac.tree.JCTree.JCLambda.ParameterKind;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCSwitch;
import com.sun.tools.javac.tree.JCTree.JCTry;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.JCTree.JCUnary;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.JCWhileLoop;
import com.sun.tools.javac.tree.JCTree.Tag;
import com.sun.tools.javac.tree.TreeCopier;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.DefinedBy;
import com.sun.tools.javac.util.DefinedBy.Api;
import com.sun.tools.javac.util.DiagnosticSource;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticType;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Options;
import com.sun.tools.javac.util.Position;

import static com.sun.tools.javac.code.Flags.GENERATEDCONSTR;
import static com.sun.tools.javac.code.TypeTag.CLASS;
import static com.sun.tools.javac.tree.JCTree.Tag.APPLY;
import static com.sun.tools.javac.tree.JCTree.Tag.FOREACHLOOP;
import static com.sun.tools.javac.tree.JCTree.Tag.LABELLED;
import static com.sun.tools.javac.tree.JCTree.Tag.METHODDEF;
import static com.sun.tools.javac.tree.JCTree.Tag.NEWCLASS;
import static com.sun.tools.javac.tree.JCTree.Tag.NULLCHK;
import static com.sun.tools.javac.tree.JCTree.Tag.TYPEAPPLY;
import static com.sun.tools.javac.tree.JCTree.Tag.VARDEF;

/**
 * Helper class for defining custom code analysis, such as finding instance creation expression
 * that can benefit from diamond syntax.
 */
public class Analyzer {
    protected static final Context.Key<Analyzer> analyzerKey = new Context.Key<>();

    final Types types;
    final Log log;
    final Attr attr;
    final DeferredAttr deferredAttr;
    final ArgumentAttr argumentAttr;
    final TreeMaker make;
    final AnalyzerCopier copier;
    private final boolean allowDiamondWithAnonymousClassCreation;

    final EnumSet<AnalyzerMode> analyzerModes;

    public static Analyzer instance(Context context) {
        Analyzer instance = context.get(analyzerKey);
        if (instance == null)
            instance = new Analyzer(context);
        return instance;
    }

    protected Analyzer(Context context) {
        context.put(analyzerKey, this);
        types = Types.instance(context);
        log = Log.instance(context);
        attr = Attr.instance(context);
        deferredAttr = DeferredAttr.instance(context);
        argumentAttr = ArgumentAttr.instance(context);
        make = TreeMaker.instance(context);
        copier = new AnalyzerCopier();
        Options options = Options.instance(context);
        String findOpt = options.get("find");
        //parse modes
        Source source = Source.instance(context);
        allowDiamondWithAnonymousClassCreation = Feature.DIAMOND_WITH_ANONYMOUS_CLASS_CREATION.allowedInSource(source);
        analyzerModes = AnalyzerMode.getAnalyzerModes(findOpt, source);
    }

    /**
     * This enum defines supported analyzer modes, as well as defining the logic for decoding
     * the {@code -XDfind} option.
     */
    enum AnalyzerMode {
        DIAMOND("diamond", Feature.DIAMOND),
        LAMBDA("lambda", Feature.LAMBDA),
        METHOD("method", Feature.GRAPH_INFERENCE),
        LOCAL("local", Feature.LOCAL_VARIABLE_TYPE_INFERENCE);

        final String opt;
        final Feature feature;

        AnalyzerMode(String opt, Feature feature) {
            this.opt = opt;
            this.feature = feature;
        }

        /**
         * This method is used to parse the {@code find} option.
         * Possible modes are separated by colon; a mode can be excluded by
         * prepending '-' to its name. Finally, the special mode 'all' can be used to
         * add all modes to the resulting enum.
         */
        static EnumSet<AnalyzerMode> getAnalyzerModes(String opt, Source source) {
            if (opt == null) {
                return EnumSet.noneOf(AnalyzerMode.class);
            }
            List<String> modes = List.from(opt.split(","));
            EnumSet<AnalyzerMode> res = EnumSet.noneOf(AnalyzerMode.class);
            if (modes.contains("all")) {
                res = EnumSet.allOf(AnalyzerMode.class);
            }
            for (AnalyzerMode mode : values()) {
                if (modes.contains("-" + mode.opt) || !mode.feature.allowedInSource(source)) {
                    res.remove(mode);
                } else if (modes.contains(mode.opt)) {
                    res.add(mode);
                }
            }
            return res;
        }
    }

    /**
     * A statement analyzer is a work-unit that matches certain AST nodes (of given type {@code S}),
     * rewrites them to different AST nodes (of type {@code T}) and then generates some meaningful
     * messages in case the analysis has been successful.
     */
    abstract class StatementAnalyzer<S extends JCTree, T extends JCTree> {

        AnalyzerMode mode;
        JCTree.Tag tag;

        StatementAnalyzer(AnalyzerMode mode, Tag tag) {
            this.mode = mode;
            this.tag = tag;
        }

        /**
         * Is this analyzer allowed to run?
         */
        boolean isEnabled() {
            return analyzerModes.contains(mode);
        }

        /**
         * Should this analyzer be rewriting the given tree?
         */
        abstract boolean match(S tree);

        /**
         * Rewrite a given AST node into a new one(s)
         */
        abstract List<T> rewrite(S oldTree);

        /**
         * Entry-point for comparing results and generating diagnostics.
         */
        abstract void process(S oldTree, T newTree, boolean hasErrors);
    }

    /**
     * This analyzer checks if generic instance creation expression can use diamond syntax.
     */
    class DiamondInitializer extends StatementAnalyzer<JCNewClass, JCNewClass> {

        DiamondInitializer() {
            super(AnalyzerMode.DIAMOND, NEWCLASS);
        }

        @Override
        boolean match(JCNewClass tree) {
            return tree.clazz.hasTag(TYPEAPPLY) &&
                    !TreeInfo.isDiamond(tree) &&
                    (tree.def == null || allowDiamondWithAnonymousClassCreation);
        }

        @Override
        List<JCNewClass> rewrite(JCNewClass oldTree) {
            if (oldTree.clazz.hasTag(TYPEAPPLY)) {
                JCNewClass nc = copier.copy(oldTree);
                ((JCTypeApply)nc.clazz).arguments = List.nil();
                return List.of(nc);
            } else {
                return List.of(oldTree);
            }
        }

        @Override
        void process(JCNewClass oldTree, JCNewClass newTree, boolean hasErrors) {
            if (!hasErrors) {
                List<Type> inferredArgs, explicitArgs;
                if (oldTree.def != null) {
                    inferredArgs = newTree.def.implementing.nonEmpty()
                                      ? newTree.def.implementing.get(0).type.getTypeArguments()
                                      : newTree.def.extending.type.getTypeArguments();
                    explicitArgs = oldTree.def.implementing.nonEmpty()
                                      ? oldTree.def.implementing.get(0).type.getTypeArguments()
                                      : oldTree.def.extending.type.getTypeArguments();
                } else {
                    inferredArgs = newTree.type.getTypeArguments();
                    explicitArgs = oldTree.type.getTypeArguments();
                }
                for (Type t : inferredArgs) {
                    if (!types.isSameType(t, explicitArgs.head)) {
                        return;
                    }
                    explicitArgs = explicitArgs.tail;
                }
                //exact match
                log.warning(oldTree.clazz, Warnings.DiamondRedundantArgs);
            }
        }
    }

    /**
     * This analyzer checks if anonymous instance creation expression can replaced by lambda.
     */
    class LambdaAnalyzer extends StatementAnalyzer<JCNewClass, JCLambda> {

        LambdaAnalyzer() {
            super(AnalyzerMode.LAMBDA, NEWCLASS);
        }

        @Override
        boolean match (JCNewClass tree){
            Type clazztype = tree.clazz.type;
            return tree.def != null &&
                    clazztype.hasTag(CLASS) &&
                    types.isFunctionalInterface(clazztype.tsym) &&
                    decls(tree.def).length() == 1;
        }
        //where
            private List<JCTree> decls(JCClassDecl decl) {
                ListBuffer<JCTree> decls = new ListBuffer<>();
                for (JCTree t : decl.defs) {
                    if (t.hasTag(METHODDEF)) {
                        JCMethodDecl md = (JCMethodDecl)t;
                        if ((md.getModifiers().flags & GENERATEDCONSTR) == 0) {
                            decls.add(md);
                        }
                    } else {
                        decls.add(t);
                    }
                }
                return decls.toList();
            }

        @Override
        List<JCLambda> rewrite(JCNewClass oldTree){
            JCMethodDecl md = (JCMethodDecl)copier.copy(decls(oldTree.def).head);
            List<JCVariableDecl> params = md.params;
            JCBlock body = md.body;
            JCLambda newTree = make.at(oldTree).Lambda(params, body);
            return List.of(newTree);
        }

        @Override
        void process (JCNewClass oldTree, JCLambda newTree, boolean hasErrors){
            if (!hasErrors) {
                log.warning(oldTree.def, Warnings.PotentialLambdaFound);
            }
        }
    }

    /**
     * This analyzer checks if generic method call has redundant type arguments.
     */
    class RedundantTypeArgAnalyzer extends StatementAnalyzer<JCMethodInvocation, JCMethodInvocation> {

        RedundantTypeArgAnalyzer() {
            super(AnalyzerMode.METHOD, APPLY);
        }

        @Override
        boolean match (JCMethodInvocation tree){
            return tree.typeargs != null &&
                    tree.typeargs.nonEmpty();
        }
        @Override
        List<JCMethodInvocation> rewrite(JCMethodInvocation oldTree){
            JCMethodInvocation app = copier.copy(oldTree);
            app.typeargs = List.nil();
            return List.of(app);
        }

        @Override
        void process (JCMethodInvocation oldTree, JCMethodInvocation newTree, boolean hasErrors){
            if (!hasErrors) {
                //exact match
                log.warning(oldTree, Warnings.MethodRedundantTypeargs);
            }
        }
    }

    /**
     * Base class for local variable inference analyzers.
     */
    abstract class RedundantLocalVarTypeAnalyzerBase<X extends JCStatement> extends StatementAnalyzer<X, X> {

        RedundantLocalVarTypeAnalyzerBase(JCTree.Tag tag) {
            super(AnalyzerMode.LOCAL, tag);
        }

        boolean isImplicitlyTyped(JCVariableDecl decl) {
            return decl.vartype.pos == Position.NOPOS;
        }

        /**
         * Map a variable tree into a new declaration using implicit type.
         */
        JCVariableDecl rewriteVarType(JCVariableDecl oldTree) {
            JCVariableDecl newTree = copier.copy(oldTree);
            newTree.vartype = null;
            return newTree;
        }

        /**
         * Analyze results of local variable inference.
         */
        void processVar(JCVariableDecl oldTree, JCVariableDecl newTree, boolean hasErrors) {
            if (!hasErrors) {
                if (types.isSameType(oldTree.type, newTree.type)) {
                    log.warning(oldTree, Warnings.LocalRedundantType);
                }
            }
        }
    }

    /**
     * This analyzer checks if a local variable declaration has redundant type.
     */
    class RedundantLocalVarTypeAnalyzer extends RedundantLocalVarTypeAnalyzerBase<JCVariableDecl> {

        RedundantLocalVarTypeAnalyzer() {
            super(VARDEF);
        }

        boolean match(JCVariableDecl tree){
            return tree.sym.owner.kind == Kind.MTH &&
                    tree.init != null && !isImplicitlyTyped(tree) &&
                    attr.canInferLocalVarType(tree) == null;
        }
        @Override
        List<JCVariableDecl> rewrite(JCVariableDecl oldTree) {
            return List.of(rewriteVarType(oldTree));
        }
        @Override
        void process(JCVariableDecl oldTree, JCVariableDecl newTree, boolean hasErrors){
            processVar(oldTree, newTree, hasErrors);
        }
    }

    /**
     * This analyzer checks if a for each variable declaration has redundant type.
     */
    class RedundantLocalVarTypeAnalyzerForEach extends RedundantLocalVarTypeAnalyzerBase<JCEnhancedForLoop> {

        RedundantLocalVarTypeAnalyzerForEach() {
            super(FOREACHLOOP);
        }

        @Override
        boolean match(JCEnhancedForLoop tree){
            return !isImplicitlyTyped(tree.var);
        }
        @Override
        List<JCEnhancedForLoop> rewrite(JCEnhancedForLoop oldTree) {
            JCEnhancedForLoop newTree = copier.copy(oldTree);
            newTree.var = rewriteVarType(oldTree.var);
            newTree.body = make.at(oldTree.body).Block(0, List.nil());
            return List.of(newTree);
        }
        @Override
        void process(JCEnhancedForLoop oldTree, JCEnhancedForLoop newTree, boolean hasErrors){
            processVar(oldTree.var, newTree.var, hasErrors);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    StatementAnalyzer<JCTree, JCTree>[] analyzers = new StatementAnalyzer[] {
            new DiamondInitializer(),
            new LambdaAnalyzer(),
            new RedundantTypeArgAnalyzer(),
            new RedundantLocalVarTypeAnalyzer(),
            new RedundantLocalVarTypeAnalyzerForEach()
    };

    /**
     * Create a copy of Env if needed.
     */
    Env<AttrContext> copyEnvIfNeeded(JCTree tree, Env<AttrContext> env) {
        if (!analyzerModes.isEmpty() &&
                !env.info.attributionMode.isSpeculative &&
                TreeInfo.isStatement(tree) &&
                !tree.hasTag(LABELLED)) {
            Env<AttrContext> analyzeEnv =
                    env.dup(env.tree, env.info.dup(env.info.scope.dupUnshared(env.info.scope.owner)));
            analyzeEnv.info.returnResult = analyzeEnv.info.returnResult != null ?
                    attr.new ResultInfo(analyzeEnv.info.returnResult.pkind,
                                        analyzeEnv.info.returnResult.pt) : null;
            return analyzeEnv;
        } else {
            return null;
        }
    }

    /**
     * Analyze an AST node if needed.
     */
    void analyzeIfNeeded(JCTree tree, Env<AttrContext> env) {
        if (env != null) {
            JCStatement stmt = (JCStatement)tree;
            analyze(stmt, env);
        }
    }

    /**
     * Analyze an AST node; this involves collecting a list of all the nodes that needs rewriting,
     * and speculatively type-check the rewritten code to compare results against previously attributed code.
     */
    protected void analyze(JCStatement statement, Env<AttrContext> env) {
        StatementScanner statementScanner = new StatementScanner(statement, env);
        statementScanner.scan();

        if (!statementScanner.rewritings.isEmpty()) {
            for (RewritingContext rewriting : statementScanner.rewritings) {
                deferredAnalysisHelper.queue(rewriting);
            }
        }
    }

    /**
     * Helper interface to handle deferral of analysis tasks.
     */
    interface DeferredAnalysisHelper {
        /**
         * Add a new analysis task to the queue.
         */
        void queue(RewritingContext rewriting);
        /**
         * Flush queue with given attribution env.
         */
        void flush(Env<AttrContext> flushEnv);
    }

    /**
     * Dummy deferral handler.
     */
    DeferredAnalysisHelper flushDeferredHelper = new DeferredAnalysisHelper() {
        @Override
        public void queue(RewritingContext rewriting) {
            //do nothing
        }

        @Override
        public void flush(Env<AttrContext> flushEnv) {
            //do nothing
        }
    };

    /**
     * Simple deferral handler. All tasks belonging to the same outermost class are added to
     * the same queue. The queue is flushed after flow analysis (only if no error occurred).
     */
    DeferredAnalysisHelper queueDeferredHelper = new DeferredAnalysisHelper() {

        Map<ClassSymbol, Queue<RewritingContext>> Q = new HashMap<>();

        @Override
        public void queue(RewritingContext rewriting) {
            Queue<RewritingContext> s = Q.computeIfAbsent(rewriting.env.enclClass.sym.outermostClass(), k -> new ArrayDeque<>());
            s.add(rewriting);
        }

        @Override
        public void flush(Env<AttrContext> flushEnv) {
            if (!Q.isEmpty()) {
                DeferredAnalysisHelper prevHelper = deferredAnalysisHelper;
                try {
                    deferredAnalysisHelper = flushDeferredHelper;
                    Queue<RewritingContext> rewritings = Q.get(flushEnv.enclClass.sym.outermostClass());
                    while (rewritings != null && !rewritings.isEmpty()) {
                        doAnalysis(rewritings.remove());
                    }
                } finally {
                    deferredAnalysisHelper = prevHelper;
                }
            }
        }
    };

    DeferredAnalysisHelper deferredAnalysisHelper = queueDeferredHelper;

    void doAnalysis(RewritingContext rewriting) {
        DiagnosticSource prevSource = log.currentSource();
        LocalCacheContext localCacheContext = argumentAttr.withLocalCacheContext();
        try {
            log.useSource(rewriting.env.toplevel.getSourceFile());

            JCStatement treeToAnalyze = (JCStatement)rewriting.originalTree;
            JCTree wrappedTree = null;

            if (rewriting.env.info.scope.owner.kind == Kind.TYP) {
                //add a block to hoist potential dangling variable declarations
                treeToAnalyze = make.at(Position.NOPOS)
                                    .Block(Flags.SYNTHETIC, List.of((JCStatement)rewriting.originalTree));
                wrappedTree = rewriting.originalTree;
            }

            //TODO: to further refine the analysis, try all rewriting combinations
            deferredAttr.attribSpeculative(treeToAnalyze, rewriting.env, attr.statInfo, new TreeRewriter(rewriting, wrappedTree),
                    () -> rewriting.diagHandler(), AttributionMode.ANALYZER, argumentAttr.withLocalCacheContext());
            rewriting.analyzer.process(rewriting.oldTree, rewriting.replacement, rewriting.erroneous);
        } catch (Throwable ex) {
            Assert.error("Analyzer error when processing: " +
                         rewriting.originalTree + ":" + ex.toString() + "\n" +
                         Arrays.stream(ex.getStackTrace())
                               .map(se -> se.toString())
                               .collect(Collectors.joining("\n")));
        } finally {
            log.useSource(prevSource.getFile());
            localCacheContext.leave();
        }
    }

    public void flush(Env<AttrContext> flushEnv) {
        deferredAnalysisHelper.flush(flushEnv);
    }

    /**
     * Subclass of {@link com.sun.tools.javac.tree.TreeScanner} which visit AST-nodes w/o crossing
     * statement boundaries.
     */
    class StatementScanner extends TreeScanner {
        /** Tree rewritings (generated by analyzers). */
        ListBuffer<RewritingContext> rewritings = new ListBuffer<>();
        JCTree originalTree;
        Env<AttrContext> env;

        StatementScanner(JCTree originalTree, Env<AttrContext> env) {
            this.originalTree = originalTree;
            this.env = attr.copyEnv(env);
        }

        public void scan() {
            scan(originalTree);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void scan(JCTree tree) {
            if (tree != null) {
                for (StatementAnalyzer<JCTree, JCTree> analyzer : analyzers) {
                    if (analyzer.isEnabled() &&
                            tree.hasTag(analyzer.tag) &&
                            analyzer.match(tree)) {
                        for (JCTree t : analyzer.rewrite(tree)) {
                            rewritings.add(new RewritingContext(originalTree, tree, t, analyzer, env));
                        }
                        break; //TODO: cover cases where multiple matching analyzers are found
                    }
                }
            }
            super.scan(tree);
        }

        @Override
        public void visitClassDef(JCClassDecl tree) {
            //do nothing (prevents seeing same stuff twice)
        }

        @Override
        public void visitMethodDef(JCMethodDecl tree) {
            //do nothing (prevents seeing same stuff twice)
        }

        @Override
        public void visitBlock(JCBlock tree) {
            //do nothing (prevents seeing same stuff twice)
        }

        @Override
        public void visitLambda(JCLambda tree) {
            //do nothing (prevents seeing same stuff in lambda expression twice)
        }

        @Override
        public void visitSwitch(JCSwitch tree) {
            scan(tree.getExpression());
        }

        @Override
        public void visitForLoop(JCForLoop tree) {
            //skip body and var decl (to prevents same statements to be analyzed twice)
            scan(tree.getCondition());
            scan(tree.getUpdate());
        }

        @Override
        public void visitTry(JCTry tree) {
            //skip resources (to prevents same statements to be analyzed twice)
            scan(tree.getBlock());
            scan(tree.getCatches());
            scan(tree.getFinallyBlock());
        }

        @Override
        public void visitForeachLoop(JCEnhancedForLoop tree) {
            //skip body (to prevents same statements to be analyzed twice)
            scan(tree.getExpression());
        }

        @Override
        public void visitWhileLoop(JCWhileLoop tree) {
            //skip body (to prevents same statements to be analyzed twice)
            scan(tree.getCondition());
        }

        @Override
        public void visitDoLoop(JCDoWhileLoop tree) {
            //skip body (to prevents same statements to be analyzed twice)
            scan(tree.getCondition());
        }

        @Override
        public void visitIf(JCIf tree) {
            //skip body (to prevents same statements to be analyzed twice)
            scan(tree.getCondition());
        }
    }

    class RewritingContext {
        // the whole tree being analyzed
        JCTree originalTree;
        // a subtree, old tree, that will be rewritten
        JCTree oldTree;
        // the replacement for the old tree
        JCTree replacement;
        // did the compiler find any error
        boolean erroneous;
        // the env
        Env<AttrContext> env;
        // the corresponding analyzer
        StatementAnalyzer<JCTree, JCTree> analyzer;

        RewritingContext(
                JCTree originalTree,
                JCTree oldTree,
                JCTree replacement,
                StatementAnalyzer<JCTree, JCTree> analyzer,
                Env<AttrContext> env) {
            this.originalTree = originalTree;
            this.oldTree = oldTree;
            this.replacement = replacement;
            this.analyzer = analyzer;
            this.env = attr.copyEnv(env);
            /*  this is a temporary workaround that should be removed once we have a truly independent
             *  clone operation
             */
            if (originalTree.hasTag(VARDEF)) {
                // avoid redefinition clashes
                this.env.info.scope.remove(((JCVariableDecl)originalTree).sym);
            }
        }

        /**
         * Simple deferred diagnostic handler which filters out all messages and keep track of errors.
         */
        Log.DeferredDiagnosticHandler diagHandler() {
            return new Log.DeferredDiagnosticHandler(log, d -> {
                if (d.getType() == DiagnosticType.ERROR) {
                    erroneous = true;
                }
                return true;
            });
        }
    }

    /**
     * Subclass of TreeCopier that maps nodes matched by analyzers onto new AST nodes.
     */
    class AnalyzerCopier extends TreeCopier<Void> {

        public AnalyzerCopier() {
            super(make);
        }

        @Override @DefinedBy(Api.COMPILER_TREE)
        public JCTree visitLambdaExpression(LambdaExpressionTree node, Void _unused) {
            JCLambda oldLambda = (JCLambda)node;
            JCLambda newLambda = (JCLambda)super.visitLambdaExpression(node, _unused);
            if (oldLambda.paramKind == ParameterKind.IMPLICIT) {
                //reset implicit lambda parameters (whose type might have been set during attr)
                newLambda.paramKind = ParameterKind.IMPLICIT;
                newLambda.params.forEach(p -> p.vartype = null);
            }
            return newLambda;
        }

        @Override @DefinedBy(Api.COMPILER_TREE)
        public JCTree visitNewClass(NewClassTree node, Void aVoid) {
            JCNewClass oldNewClazz = (JCNewClass)node;
            JCNewClass newNewClazz = (JCNewClass)super.visitNewClass(node, aVoid);
            if (!oldNewClazz.args.isEmpty() && oldNewClazz.args.head.hasTag(NULLCHK)) {
                //workaround to Attr generating trees
                newNewClazz.encl = ((JCUnary)newNewClazz.args.head).arg;
                newNewClazz.args = newNewClazz.args.tail;
            }
            return newNewClazz;
        }
    }

   class TreeRewriter extends AnalyzerCopier {

        RewritingContext rewriting;
        JCTree wrappedTree;

        TreeRewriter(RewritingContext rewriting, JCTree wrappedTree) {
            this.rewriting = rewriting;
            this.wrappedTree = wrappedTree;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <Z extends JCTree> Z copy(Z tree, Void _unused) {
            Z newTree = super.copy(tree, null);
            if (tree != null && tree == rewriting.oldTree) {
                Assert.checkNonNull(rewriting.replacement);
                newTree = (Z)rewriting.replacement;
            }
            return newTree;
        }

        @Override
        public JCTree visitVariable(VariableTree node, Void p) {
            JCTree result = super.visitVariable(node, p);
            if (node == wrappedTree) {
                //The current tree is a field and has been wrapped by a block, so it effectivelly
                //became local variable. If it has some modifiers (except for final), an error
                //would be reported, causing the whole rewrite to fail. Removing the non-final
                //modifiers from the variable here:
                ((JCVariableDecl) result).mods.flags &= Flags.FINAL;
            }
            return result;
        }

    }
}
