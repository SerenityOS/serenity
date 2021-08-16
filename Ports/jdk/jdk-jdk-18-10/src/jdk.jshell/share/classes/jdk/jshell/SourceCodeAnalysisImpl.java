/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ErroneousTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.api.JavacScope;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol.CompletionFailure;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import jdk.internal.shellsupport.doc.JavadocHelper;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Pair;
import jdk.jshell.CompletenessAnalyzer.CaInfo;
import jdk.jshell.TaskFactory.AnalyzeTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import static jdk.internal.jshell.debug.InternalDebugControl.DBG_COMPA;

import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.lang.model.SourceVersion;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import javax.tools.JavaFileManager.Location;
import javax.tools.StandardLocation;

import jdk.jshell.ExpressionToTypeInfo.ExpressionInfo;
import static jdk.jshell.Util.REPL_DOESNOTMATTER_CLASS_NAME;
import static jdk.jshell.SourceCodeAnalysis.Completeness.DEFINITELY_INCOMPLETE;
import static jdk.jshell.TreeDissector.printType;

import static java.util.stream.Collectors.joining;

import javax.lang.model.type.IntersectionType;

/**
 * The concrete implementation of SourceCodeAnalysis.
 * @author Robert Field
 */
class SourceCodeAnalysisImpl extends SourceCodeAnalysis {

    private static final Map<Path, ClassIndex> PATH_TO_INDEX = new HashMap<>();
    private static final ExecutorService INDEXER = Executors.newFixedThreadPool(1, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setUncaughtExceptionHandler((thread, ex) -> ex.printStackTrace());
        return t;
    });

    private final JShell proc;
    private final CompletenessAnalyzer ca;
    private final List<AutoCloseable> closeables = new ArrayList<>();
    private final Map<Path, ClassIndex> currentIndexes = new HashMap<>();
    private int indexVersion;
    private int classpathVersion;
    private final Object suspendLock = new Object();
    private int suspend;

    SourceCodeAnalysisImpl(JShell proc) {
        this.proc = proc;
        this.ca = new CompletenessAnalyzer(proc);

        int cpVersion = classpathVersion = 1;

        INDEXER.submit(() -> refreshIndexes(cpVersion));
    }

    @Override
    public CompletionInfo analyzeCompletion(String srcInput) {
        MaskCommentsAndModifiers mcm = new MaskCommentsAndModifiers(srcInput, false);
        if (mcm.endsWithOpenToken()) {
            proc.debug(DBG_COMPA, "Incomplete (open comment): %s\n", srcInput);
            return new CompletionInfoImpl(DEFINITELY_INCOMPLETE, null, srcInput + '\n');
        }
        String cleared = mcm.cleared();
        String trimmedInput = Util.trimEnd(cleared);
        if (trimmedInput.isEmpty()) {
            // Just comment or empty
            return new CompletionInfoImpl(Completeness.EMPTY, srcInput, "");
        }
        CaInfo info = ca.scan(trimmedInput);
        Completeness status = info.status;
        int unitEndPos = info.unitEndPos;
        if (unitEndPos > srcInput.length()) {
            unitEndPos = srcInput.length();
        }
        int nonCommentNonWhiteLength = trimmedInput.length();
        String src = srcInput.substring(0, unitEndPos);
        switch (status) {
            case COMPLETE: {
                if (unitEndPos == nonCommentNonWhiteLength) {
                    // The unit is the whole non-coment/white input plus semicolon
                    String compileSource = src
                            + mcm.mask().substring(nonCommentNonWhiteLength);
                    proc.debug(DBG_COMPA, "Complete: %s\n", compileSource);
                    proc.debug(DBG_COMPA, "   nothing remains.\n");
                    return new CompletionInfoImpl(status, compileSource, "");
                } else {
                    String remain = srcInput.substring(unitEndPos);
                    proc.debug(DBG_COMPA, "Complete: %s\n", src);
                    proc.debug(DBG_COMPA, "          remaining: %s\n", remain);
                    return new CompletionInfoImpl(status, src, remain);
                }
            }
            case COMPLETE_WITH_SEMI: {
                // The unit is the whole non-coment/white input plus semicolon
                String compileSource = src
                        + ";"
                        + mcm.mask().substring(nonCommentNonWhiteLength);
                proc.debug(DBG_COMPA, "Complete with semi: %s\n", compileSource);
                proc.debug(DBG_COMPA, "   nothing remains.\n");
                return new CompletionInfoImpl(status, compileSource, "");
            }
            case DEFINITELY_INCOMPLETE:
                proc.debug(DBG_COMPA, "Incomplete: %s\n", srcInput);
                return new CompletionInfoImpl(status, null, srcInput + '\n');
            case CONSIDERED_INCOMPLETE: {
                // Since the source is potentually valid, construct the complete source
                String compileSource = src
                        + ";"
                        + mcm.mask().substring(nonCommentNonWhiteLength);
                proc.debug(DBG_COMPA, "Considered incomplete: %s\n", srcInput);
                return new CompletionInfoImpl(status, compileSource, srcInput + '\n');
            }
            case EMPTY:
                proc.debug(DBG_COMPA, "Detected empty: %s\n", srcInput);
                return new CompletionInfoImpl(status, srcInput, "");
            case UNKNOWN:
                proc.debug(DBG_COMPA, "Detected error: %s\n", srcInput);
                return new CompletionInfoImpl(status, srcInput, "");
        }
        throw new InternalError();
    }

    private Tree.Kind guessKind(String code) {
        return proc.taskFactory.parse(code, pt -> {
            List<? extends Tree> units = pt.units();
            if (units.isEmpty()) {
                return Tree.Kind.BLOCK;
            }
            Tree unitTree = units.get(0);
            proc.debug(DBG_COMPA, "Kind: %s -- %s\n", unitTree.getKind(), unitTree);
            return unitTree.getKind();
        });
    }

    //TODO: would be better handled through a lexer:
    private final Pattern JAVA_IDENTIFIER = Pattern.compile("\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*");

    @Override
    public List<Suggestion> completionSuggestions(String code, int cursor, int[] anchor) {
        suspendIndexing();
        try {
            return completionSuggestionsImpl(code, cursor, anchor);
        } catch (Throwable exc) {
            proc.debug(exc, "Exception thrown in SourceCodeAnalysisImpl.completionSuggestions");
            return Collections.emptyList();
        } finally {
            resumeIndexing();
        }
    }

    private List<Suggestion> completionSuggestionsImpl(String code, int cursor, int[] anchor) {
        code = code.substring(0, cursor);
        Matcher m = JAVA_IDENTIFIER.matcher(code);
        String identifier = "";
        while (m.find()) {
            if (m.end() == code.length()) {
                cursor = m.start();
                code = code.substring(0, cursor);
                identifier = m.group();
            }
        }
        code = code.substring(0, cursor);
        if (code.trim().isEmpty()) { //TODO: comment handling
            code += ";";
        }
        OuterWrap codeWrap = switch (guessKind(code)) {
            case IMPORT -> proc.outerMap.wrapImport(Wrap.simpleWrap(code + "any.any"), null);
            case CLASS, METHOD -> proc.outerMap.wrapInTrialClass(Wrap.classMemberWrap(code));
            default -> proc.outerMap.wrapInTrialClass(Wrap.methodWrap(code));
        };
        String requiredPrefix = identifier;
        return computeSuggestions(codeWrap, cursor, anchor).stream()
                .filter(s -> s.continuation().startsWith(requiredPrefix) && !s.continuation().equals(REPL_DOESNOTMATTER_CLASS_NAME))
                .sorted(Comparator.comparing(Suggestion::continuation))
                .toList();
    }

    private List<Suggestion> computeSuggestions(OuterWrap code, int cursor, int[] anchor) {
        return proc.taskFactory.analyze(code, at -> {
            SourcePositions sp = at.trees().getSourcePositions();
            CompilationUnitTree topLevel = at.firstCuTree();
            List<Suggestion> result = new ArrayList<>();
            TreePath tp = pathFor(topLevel, sp, code, cursor);
            if (tp != null) {
                Scope scope = at.trees().getScope(tp);
                Predicate<Element> accessibility = createAccessibilityFilter(at, tp);
                Predicate<Element> smartTypeFilter;
                Predicate<Element> smartFilter;
                Iterable<TypeMirror> targetTypes = findTargetType(at, tp);
                if (targetTypes != null) {
                    if (tp.getLeaf().getKind() == Kind.MEMBER_REFERENCE) {
                        Types types = at.getTypes();
                        smartTypeFilter = t -> {
                            if (t.getKind() != ElementKind.METHOD) {
                                return false;
                            }
                            ExecutableElement ee = (ExecutableElement) t;
                            for (TypeMirror type : targetTypes) {
                                if (type.getKind() != TypeKind.DECLARED)
                                    continue;
                                DeclaredType d = (DeclaredType) type;
                                List<? extends Element> enclosed =
                                        ((TypeElement) d.asElement()).getEnclosedElements();
                                for (ExecutableElement m : ElementFilter.methodsIn(enclosed)) {
                                    boolean matches = true;
                                    if (!m.getModifiers().contains(Modifier.ABSTRACT)) {
                                        continue;
                                    }
                                    if (m.getParameters().size() != ee.getParameters().size()) {
                                        continue;
                                    }
                                    ExecutableType mInst = (ExecutableType) types.asMemberOf(d, m);
                                    List<? extends TypeMirror> expectedParams = mInst.getParameterTypes();
                                    if (mInst.getReturnType().getKind() != TypeKind.VOID &&
                                        !types.isSubtype(ee.getReturnType(), mInst.getReturnType())) {
                                        continue;
                                    }
                                    for (int i = 0; i < m.getParameters().size(); i++) {
                                        if (!types.isSubtype(expectedParams.get(i),
                                                             ee.getParameters().get(i).asType())) {
                                            matches = false;
                                        }
                                    }
                                    if (matches) {
                                        return true;
                                    }
                                }
                            }
                            return false;
                        };
                    } else {
                        smartTypeFilter = el -> {
                            TypeMirror resultOf = resultTypeOf(el);
                            return Util.stream(targetTypes)
                                    .anyMatch(targetType -> at.getTypes().isAssignable(resultOf, targetType));
                        };
                    }

                    smartFilter = IS_CLASS.negate()
                                          .and(IS_INTERFACE.negate())
                                          .and(IS_PACKAGE.negate())
                                          .and(smartTypeFilter);
                } else {
                    smartFilter = TRUE;
                    smartTypeFilter = TRUE;
                }
                switch (tp.getLeaf().getKind()) {
                    case MEMBER_REFERENCE, MEMBER_SELECT: {
                        javax.lang.model.element.Name identifier;
                        ExpressionTree expression;
                        Function<Boolean, String> paren;
                        if (tp.getLeaf().getKind() == Kind.MEMBER_SELECT) {
                            MemberSelectTree mst = (MemberSelectTree)tp.getLeaf();
                            identifier = mst.getIdentifier();
                            expression = mst.getExpression();
                            paren = DEFAULT_PAREN;
                        } else {
                            MemberReferenceTree mst = (MemberReferenceTree)tp.getLeaf();
                            identifier = mst.getName();
                            expression = mst.getQualifierExpression();
                            paren = NO_PAREN;
                        }
                        if (identifier.contentEquals("*"))
                            break;
                        TreePath exprPath = new TreePath(tp, expression);
                        TypeMirror site = at.trees().getTypeMirror(exprPath);
                        boolean staticOnly = isStaticContext(at, exprPath);
                        ImportTree it = findImport(tp);
                        boolean isImport = it != null;

                        List<? extends Element> members = membersOf(at, site, staticOnly && !isImport && tp.getLeaf().getKind() == Kind.MEMBER_SELECT);
                        Predicate<Element> filter = accessibility;

                        if (isNewClass(tp)) { // new xxx.|
                            Predicate<Element> constructorFilter = accessibility.and(IS_CONSTRUCTOR)
                                .and(el -> {
                                    if (el.getEnclosingElement().getEnclosingElement().getKind() == ElementKind.CLASS) {
                                        return el.getEnclosingElement().getModifiers().contains(Modifier.STATIC);
                                    }
                                    return true;
                                });
                            addElements(membersOf(at, members), constructorFilter, smartFilter, result);

                            filter = filter.and(IS_PACKAGE);
                        } else if (isThrowsClause(tp)) {
                            staticOnly = true;
                            filter = filter.and(IS_PACKAGE.or(IS_CLASS).or(IS_INTERFACE));
                            smartFilter = IS_PACKAGE.negate().and(smartTypeFilter);
                        } else if (isImport) {
                            paren = NO_PAREN;
                            if (!it.isStatic()) {
                                filter = filter.and(IS_PACKAGE.or(IS_CLASS).or(IS_INTERFACE));
                            }
                        } else {
                            filter = filter.and(IS_CONSTRUCTOR.negate());
                        }

                        filter = filter.and(staticOnly ? STATIC_ONLY : INSTANCE_ONLY);

                        addElements(members, filter, smartFilter, paren, result);
                        break;
                    }
                    case IDENTIFIER:
                        if (isNewClass(tp)) {
                            Function<Element, Iterable<? extends Element>> listEnclosed =
                                    el -> el.getKind() == ElementKind.PACKAGE ? Collections.singletonList(el)
                                                                              : el.getEnclosedElements();
                            Predicate<Element> filter = accessibility.and(IS_CONSTRUCTOR.or(IS_PACKAGE));
                            NewClassTree newClassTree = (NewClassTree)tp.getParentPath().getLeaf();
                            ExpressionTree enclosingExpression = newClassTree.getEnclosingExpression();
                            if (enclosingExpression != null) { // expr.new IDENT|
                                TypeMirror site = at.trees().getTypeMirror(new TreePath(tp, enclosingExpression));
                                filter = filter.and(el -> el.getEnclosingElement().getKind() == ElementKind.CLASS && !el.getEnclosingElement().getModifiers().contains(Modifier.STATIC));
                                addElements(membersOf(at, membersOf(at, site, false)), filter, smartFilter, result);
                            } else {
                                addScopeElements(at, scope, listEnclosed, filter, smartFilter, result);
                            }
                            break;
                        }
                        if (isThrowsClause(tp)) {
                            Predicate<Element> accept = accessibility.and(STATIC_ONLY)
                                    .and(IS_PACKAGE.or(IS_CLASS).or(IS_INTERFACE));
                            addScopeElements(at, scope, IDENTITY, accept, IS_PACKAGE.negate().and(smartTypeFilter), result);
                            break;
                        }
                        ImportTree it = findImport(tp);
                        if (it != null) {
                            // the context of the identifier is an import, look for
                            // package names that start with the identifier.
                            // If and when Java allows imports from the default
                            // package to the the default package which would allow
                            // JShell to change to use the default package, and that
                            // change is done, then this should use some variation
                            // of membersOf(at, at.getElements().getPackageElement("").asType(), false)
                            addElements(listPackages(at, ""),
                                    it.isStatic()
                                            ? STATIC_ONLY.and(accessibility)
                                            : accessibility,
                                    smartFilter, result);
                        }
                        break;
                    case CLASS: {
                        Predicate<Element> accept = accessibility.and(IS_TYPE);
                        addScopeElements(at, scope, IDENTITY, accept, smartFilter, result);
                        addElements(primitivesOrVoid(at), TRUE, smartFilter, result);
                        break;
                    }
                    case BLOCK:
                    case EMPTY_STATEMENT:
                    case ERRONEOUS: {
                        boolean staticOnly = ReplResolve.isStatic(((JavacScope)scope).getEnv());
                        Predicate<Element> accept = accessibility.and(staticOnly ? STATIC_ONLY : TRUE);
                        if (isClass(tp)) {
                            ClassTree clazz = (ClassTree) tp.getParentPath().getLeaf();
                            if (clazz.getExtendsClause() == tp.getLeaf()) {
                                accept = accept.and(IS_TYPE);
                                smartFilter = smartFilter.and(el -> el.getKind() == ElementKind.CLASS);
                            } else {
                                Predicate<Element> f = smartFilterFromList(at, tp, clazz.getImplementsClause(), tp.getLeaf());
                                if (f != null) {
                                    accept = accept.and(IS_TYPE);
                                    smartFilter = f.and(el -> el.getKind() == ElementKind.INTERFACE);
                                }
                            }
                        } else if (isTypeParameter(tp)) {
                            TypeParameterTree tpt = (TypeParameterTree) tp.getParentPath().getLeaf();
                            Predicate<Element> f = smartFilterFromList(at, tp, tpt.getBounds(), tp.getLeaf());
                            if (f != null) {
                                accept = accept.and(IS_TYPE);
                                smartFilter = f;
                                if (!tpt.getBounds().isEmpty() && tpt.getBounds().get(0) != tp.getLeaf()) {
                                    smartFilter = smartFilter.and(el -> el.getKind() == ElementKind.INTERFACE);
                                }
                            }
                        } else if (isVariable(tp)) {
                            VariableTree var = (VariableTree) tp.getParentPath().getLeaf();
                            if (var.getType() == tp.getLeaf()) {
                                accept = accept.and(IS_TYPE);
                            }
                        }

                        addScopeElements(at, scope, IDENTITY, accept, smartFilter, result);

                        Tree parent = tp.getParentPath().getLeaf();
                        accept = switch (parent.getKind()) {
                            case VARIABLE -> ((VariableTree) parent).getType() == tp.getLeaf() ?
                                             IS_VOID.negate() :
                                             TRUE;
                            case PARAMETERIZED_TYPE -> FALSE; // TODO: JEP 218: Generics over Primitive Types
                            case TYPE_PARAMETER, CLASS, INTERFACE, ENUM -> FALSE;
                            default -> TRUE;
                        };
                        addElements(primitivesOrVoid(at), accept, smartFilter, result);
                        break;
                    }
                }
            }
            anchor[0] = cursor;
            return result;
        });
    }

    private static final Set<Kind> CLASS_KINDS = EnumSet.of(
            Kind.ANNOTATION_TYPE, Kind.CLASS, Kind.ENUM, Kind.INTERFACE
    );

    private Predicate<Element> smartFilterFromList(AnalyzeTask at, TreePath base, Collection<? extends Tree> types, Tree current) {
        Set<Element> existingEls = new HashSet<>();

        for (Tree type : types) {
            if (type == current) {
                return el -> !existingEls.contains(el);
            }
            existingEls.add(at.trees().getElement(new TreePath(base, type)));
        }

        return null;
    }

    @Override
    public SnippetWrapper wrapper(Snippet snippet) {
        return new SnippetWrapper() {
            @Override
            public String source() {
                return snippet.source();
            }

            @Override
            public String wrapped() {
                return snippet.outerWrap().wrapped();
            }

            @Override
            public String fullClassName() {
                return snippet.classFullName();
            }

            @Override
            public Snippet.Kind kind() {
                return snippet.kind() == Snippet.Kind.ERRONEOUS
                        ? ((ErroneousSnippet) snippet).probableKind()
                        : snippet.kind();
            }

            @Override
            public int sourceToWrappedPosition(int pos) {
                return snippet.outerWrap().snippetIndexToWrapIndex(pos);
            }

            @Override
            public int wrappedToSourcePosition(int pos) {
                return snippet.outerWrap().wrapIndexToSnippetIndex(pos);
            }
        };
    }

    @Override
    public List<SnippetWrapper> wrappers(String input) {
        return proc.eval.sourceToSnippetsWithWrappers(input).stream()
                .map(this::wrapper)
                .toList();
    }

    @Override
    public List<Snippet> sourceToSnippets(String input) {
        proc.checkIfAlive();
        List<Snippet> snl = proc.eval.toScratchSnippets(input);
        for (Snippet sn : snl) {
            sn.setId(Snippet.UNASSOCIATED_ID);
        }
        return snl;
    }

    @Override
    public Collection<Snippet> dependents(Snippet snippet) {
        return proc.maps.getDependents(snippet);
    }

    private boolean isStaticContext(AnalyzeTask at, TreePath path) {
        switch (path.getLeaf().getKind()) {
            case ARRAY_TYPE:
            case PRIMITIVE_TYPE:
                return true;
            default:
                Element selectEl = at.trees().getElement(path);
                return selectEl != null && (selectEl.getKind().isClass() || selectEl.getKind().isInterface() || selectEl.getKind() == ElementKind.TYPE_PARAMETER) && selectEl.asType().getKind() != TypeKind.ERROR;
        }
    }

    private TreePath pathFor(CompilationUnitTree topLevel, SourcePositions sp, GeneralWrap wrap, int snippetEndPos) {
        int wrapEndPos = snippetEndPos == 0
                ? wrap.snippetIndexToWrapIndex(snippetEndPos)
                : wrap.snippetIndexToWrapIndex(snippetEndPos - 1) + 1;
        TreePath[] deepest = new TreePath[1];

        new TreePathScanner<Void, Void>() {
            @Override
            public Void scan(Tree tree, Void p) {
                if (tree == null)
                    return null;

                long start = sp.getStartPosition(topLevel, tree);
                long end = sp.getEndPosition(topLevel, tree);
                long prevEnd = deepest[0] != null ? sp.getEndPosition(topLevel, deepest[0].getLeaf()) : -1;

                if (start <= wrapEndPos && wrapEndPos <= end &&
                    (start != end || prevEnd != end || deepest[0] == null ||
                     deepest[0].getParentPath().getLeaf() != getCurrentPath().getLeaf())) {
                    deepest[0] = new TreePath(getCurrentPath(), tree);
                    return super.scan(tree, p);
                }

                return null;
            }
            @Override
            public Void visitErroneous(ErroneousTree node, Void p) {
                return scan(node.getErrorTrees(), null);
            }
        }.scan(topLevel, null);

        return deepest[0];
    }

    private boolean isNewClass(TreePath tp) {
        return tp.getParentPath() != null &&
               tp.getParentPath().getLeaf().getKind() == Kind.NEW_CLASS &&
               ((NewClassTree) tp.getParentPath().getLeaf()).getIdentifier() == tp.getLeaf();
    }

    private boolean isThrowsClause(TreePath tp) {
        Tree parent = tp.getParentPath().getLeaf();
        return parent.getKind() == Kind.METHOD &&
                ((MethodTree)parent).getThrows().contains(tp.getLeaf());
    }

    private boolean isClass(TreePath tp) {
        return tp.getParentPath() != null &&
               CLASS_KINDS.contains(tp.getParentPath().getLeaf().getKind());
    }

    private boolean isTypeParameter(TreePath tp) {
        return tp.getParentPath() != null &&
               tp.getParentPath().getLeaf().getKind() == Kind.TYPE_PARAMETER;
    }

    private boolean isVariable(TreePath tp) {
        return tp.getParentPath() != null &&
               tp.getParentPath().getLeaf().getKind() == Kind.VARIABLE;
    }

    private ImportTree findImport(TreePath tp) {
        while (tp != null && tp.getLeaf().getKind() != Kind.IMPORT) {
            tp = tp.getParentPath();
        }
        return tp != null ? (ImportTree)tp.getLeaf() : null;
    }

    private Predicate<Element> createAccessibilityFilter(AnalyzeTask at, TreePath tp) {
        Scope scope = at.trees().getScope(tp);
        return el -> {
            switch (el.getKind()) {
                case ANNOTATION_TYPE: case CLASS: case ENUM: case INTERFACE:
                    return at.trees().isAccessible(scope, (TypeElement) el);
                case PACKAGE:
                case EXCEPTION_PARAMETER: case PARAMETER: case LOCAL_VARIABLE: case RESOURCE_VARIABLE:
                    return true;
                default:
                    TypeMirror type = el.getEnclosingElement().asType();
                    if (type.getKind() == TypeKind.DECLARED)
                        return at.trees().isAccessible(scope, el, (DeclaredType) type);
                    else
                        return true;
            }
        };
    }

    private final Predicate<Element> TRUE = el -> true;
    private final Predicate<Element> FALSE = TRUE.negate();
    private final Predicate<Element> IS_STATIC = el -> el.getModifiers().contains(Modifier.STATIC);
    private final Predicate<Element> IS_CONSTRUCTOR = el -> el.getKind() == ElementKind.CONSTRUCTOR;
    private final Predicate<Element> IS_METHOD = el -> el.getKind() == ElementKind.METHOD;
    private final Predicate<Element> IS_PACKAGE = el -> el.getKind() == ElementKind.PACKAGE;
    private final Predicate<Element> IS_CLASS = el -> el.getKind().isClass();
    private final Predicate<Element> IS_INTERFACE = el -> el.getKind().isInterface();
    private final Predicate<Element> IS_TYPE = IS_CLASS.or(IS_INTERFACE).or(el -> el.getKind() == ElementKind.TYPE_PARAMETER);
    private final Predicate<Element> IS_VOID = el -> el.asType().getKind() == TypeKind.VOID;
    private final Predicate<Element> STATIC_ONLY = el -> {
        ElementKind kind = el.getKind();
        Element encl = el.getEnclosingElement();
        ElementKind enclKind = encl != null ? encl.getKind() : ElementKind.OTHER;

        return IS_STATIC.or(IS_PACKAGE).or(IS_CLASS).or(IS_INTERFACE).test(el) || IS_PACKAGE.test(encl) ||
                (kind == ElementKind.TYPE_PARAMETER && !enclKind.isClass() && !enclKind.isInterface());
    };
    private final Predicate<Element> INSTANCE_ONLY = el -> {
        Element encl = el.getEnclosingElement();

        return IS_STATIC.or(IS_CLASS).or(IS_INTERFACE).negate().test(el) ||
                IS_PACKAGE.test(encl);
    };
    private final Function<Element, Iterable<? extends Element>> IDENTITY = Collections::singletonList;
    private final Function<Boolean, String> DEFAULT_PAREN = hasParams -> hasParams ? "(" : "()";
    private final Function<Boolean, String> NO_PAREN = hasParams -> "";

    private void addElements(Iterable<? extends Element> elements, Predicate<Element> accept, Predicate<Element> smart, List<Suggestion> result) {
        addElements(elements, accept, smart, DEFAULT_PAREN, result);
    }
    private void addElements(Iterable<? extends Element> elements, Predicate<Element> accept, Predicate<Element> smart, Function<Boolean, String> paren, List<Suggestion> result) {
        Set<String> hasParams = Util.stream(elements)
                .filter(accept)
                .filter(IS_CONSTRUCTOR.or(IS_METHOD))
                .filter(c -> !((ExecutableElement)c).getParameters().isEmpty())
                .map(this::simpleName)
                .collect(toSet());

        for (Element c : elements) {
            if (!accept.test(c))
                continue;
            if (c.getKind() == ElementKind.METHOD &&
                c.getSimpleName().contentEquals(Util.DOIT_METHOD_NAME) &&
                ((ExecutableElement) c).getParameters().isEmpty()) {
                continue;
            }
            String simpleName = simpleName(c);
            switch (c.getKind()) {
                case CONSTRUCTOR:
                case METHOD:
                    // add trailing open or matched parenthesis, as approriate
                    simpleName += paren.apply(hasParams.contains(simpleName));
                    break;
                case PACKAGE:
                    // add trailing dot to package names
                    simpleName += ".";
                    break;
            }
            result.add(new SuggestionImpl(simpleName, smart.test(c)));
        }
    }

    private String simpleName(Element el) {
        return el.getKind() == ElementKind.CONSTRUCTOR ? el.getEnclosingElement().getSimpleName().toString()
                                                       : el.getSimpleName().toString();
    }

    private List<? extends Element> membersOf(AnalyzeTask at, TypeMirror site, boolean shouldGenerateDotClassItem) {
        if (site  == null)
            return Collections.emptyList();

        switch (site.getKind()) {
            case INTERSECTION: {
                List<Element> result = new ArrayList<>();
                for (TypeMirror bound : ((IntersectionType) site).getBounds()) {
                    result.addAll(membersOf(at, bound, shouldGenerateDotClassItem));
                }
                return result;
            }
            case DECLARED: {
                TypeElement element = (TypeElement) at.getTypes().asElement(site);
                List<Element> result = new ArrayList<>();
                result.addAll(at.getElements().getAllMembers(element));
                if (shouldGenerateDotClassItem) {
                    result.add(createDotClassSymbol(at, site));
                }
                result.removeIf(el -> el.getKind() == ElementKind.STATIC_INIT);
                return result;
            }
            case ERROR: {
                //try current qualified name as a package:
                TypeElement typeElement = (TypeElement) at.getTypes().asElement(site);
                Element enclosingElement = typeElement.getEnclosingElement();
                String parentPackageName = enclosingElement instanceof QualifiedNameable ?
                    ((QualifiedNameable)enclosingElement).getQualifiedName().toString() :
                    "";
                Set<PackageElement> packages = listPackages(at, parentPackageName);
                return packages.stream()
                               .filter(p -> p.getQualifiedName().equals(typeElement.getQualifiedName()))
                               .findAny()
                               .map(p -> membersOf(at, p.asType(), false))
                               .orElse(Collections.emptyList());
            }
            case PACKAGE: {
                String packageName = site.toString()/*XXX*/;
                List<Element> result = new ArrayList<>();
                result.addAll(getEnclosedElements(at.getElements().getPackageElement(packageName)));
                result.addAll(listPackages(at, packageName));
                return result;
            }
            case BOOLEAN: case BYTE: case SHORT: case CHAR:
            case INT: case FLOAT: case LONG: case DOUBLE:
            case VOID: {
                return shouldGenerateDotClassItem ?
                    Collections.singletonList(createDotClassSymbol(at, site)) :
                    Collections.emptyList();
            }
            case ARRAY: {
                List<Element> result = new ArrayList<>();
                result.add(createArrayLengthSymbol(at, site));
                if (shouldGenerateDotClassItem)
                    result.add(createDotClassSymbol(at, site));
                return result;
            }
            default:
                return Collections.emptyList();
        }
    }

    private List<? extends Element> membersOf(AnalyzeTask at, List<? extends Element> elements) {
        return elements.stream()
                .flatMap(e -> membersOf(at, e.asType(), true).stream())
                .toList();
    }

    private List<? extends Element> getEnclosedElements(PackageElement packageEl) {
        if (packageEl == null) {
            return Collections.emptyList();
        }
        //workaround for: JDK-8024687
        while (true) {
            try {
                return packageEl.getEnclosedElements()
                                .stream()
                                .filter(el -> el.asType() != null)
                                .filter(el -> el.asType().getKind() != TypeKind.ERROR)
                                .toList();
            } catch (CompletionFailure cf) {
                //ignore...
            }
        }
    }

    private List<? extends Element> primitivesOrVoid(AnalyzeTask at) {
        Types types = at.getTypes();
        return Stream.of(
                TypeKind.BOOLEAN, TypeKind.BYTE, TypeKind.CHAR,
                TypeKind.DOUBLE, TypeKind.FLOAT, TypeKind.INT,
                TypeKind.LONG, TypeKind.SHORT, TypeKind.VOID)
                .map(tk -> (Type)(tk == TypeKind.VOID ? types.getNoType(tk) : types.getPrimitiveType(tk)))
                .map(Type::asElement)
                .toList();
    }

    void classpathChanged() {
        synchronized (currentIndexes) {
            int cpVersion = ++classpathVersion;

            INDEXER.submit(() -> refreshIndexes(cpVersion));
        }
    }

    private Set<PackageElement> listPackages(AnalyzeTask at, String enclosingPackage) {
        synchronized (currentIndexes) {
            return currentIndexes.values()
                                 .stream()
                                 .flatMap(idx -> idx.packages.stream())
                                 .filter(p -> enclosingPackage.isEmpty() || p.startsWith(enclosingPackage + "."))
                                 .map(p -> {
                                     int dot = p.indexOf('.', enclosingPackage.length() + 1);
                                     return dot == (-1) ? p : p.substring(0, dot);
                                 })
                                 .distinct()
                                 .map(p -> createPackageElement(at, p))
                                 .collect(Collectors.toSet());
        }
    }

    private PackageElement createPackageElement(AnalyzeTask at, String packageName) {
        Names names = Names.instance(at.getContext());
        Symtab syms = Symtab.instance(at.getContext());
        PackageElement existing = syms.enterPackage(syms.unnamedModule, names.fromString(packageName));

        return existing;
    }

    private Element createArrayLengthSymbol(AnalyzeTask at, TypeMirror site) {
        Name length = Names.instance(at.getContext()).length;
        Type intType = Symtab.instance(at.getContext()).intType;

        return new VarSymbol(Flags.PUBLIC | Flags.FINAL, length, intType, ((Type) site).tsym);
    }

    private Element createDotClassSymbol(AnalyzeTask at, TypeMirror site) {
        Name _class = Names.instance(at.getContext())._class;
        Type classType = Symtab.instance(at.getContext()).classType;
        Type erasedSite = (Type)at.getTypes().erasure(site);
        classType = new ClassType(classType.getEnclosingType(), com.sun.tools.javac.util.List.of(erasedSite), classType.asElement());

        return new VarSymbol(Flags.PUBLIC | Flags.STATIC | Flags.FINAL, _class, classType, erasedSite.tsym);
    }

    private Iterable<? extends Element> scopeContent(AnalyzeTask at, Scope scope, Function<Element, Iterable<? extends Element>> elementConvertor) {
        Iterable<Scope> scopeIterable = () -> new Iterator<Scope>() {
            private Scope currentScope = scope;
            @Override
            public boolean hasNext() {
                return currentScope != null;
            }
            @Override
            public Scope next() {
                if (!hasNext())
                    throw new NoSuchElementException();
                try {
                    return currentScope;
                } finally {
                    currentScope = currentScope.getEnclosingScope();
                }
            }
        };
        @SuppressWarnings("unchecked")
        List<Element> result = Util.stream(scopeIterable)
                             .flatMap(this::localElements)
                             .flatMap(el -> Util.stream((Iterable<Element>)elementConvertor.apply(el)))
                             .collect(toCollection(ArrayList :: new));
        result.addAll(listPackages(at, ""));
        return result;
    }

    private Stream<Element> localElements(Scope scope) {
        //workaround for: JDK-8024687
        Iterable<Element> elementsIt = () -> new Iterator<Element>() {
            Iterator<? extends Element> it = scope.getLocalElements().iterator();
            @Override
            public boolean hasNext() {
                while (true) {
                    try {
                        return it.hasNext();
                    } catch (CompletionFailure cf) {
                        //ignore...
                    }
                }
            }
            @Override
            public Element next() {
                while (true) {
                    try {
                        return it.next();
                    } catch (CompletionFailure cf) {
                        //ignore...
                    }
                }
            }
        };
        Stream<Element> elements = Util.stream(elementsIt);

        if (scope.getEnclosingScope() != null &&
            scope.getEnclosingClass() != scope.getEnclosingScope().getEnclosingClass()) {
            elements = Stream.concat(elements, scope.getEnclosingClass().getEnclosedElements().stream());
        }

        return elements;
    }

    @SuppressWarnings("fallthrough")
    private Iterable<TypeMirror> findTargetType(AnalyzeTask at, TreePath forPath) {
        if (forPath.getParentPath() == null)
            return null;

        Tree current = forPath.getLeaf();

        switch (forPath.getParentPath().getLeaf().getKind()) {
            case ASSIGNMENT: {
                AssignmentTree tree = (AssignmentTree) forPath.getParentPath().getLeaf();
                if (tree.getExpression() == current)
                    return Collections.singletonList(at.trees().getTypeMirror(new TreePath(forPath.getParentPath(), tree.getVariable())));
                break;
            }
            case VARIABLE: {
                VariableTree tree = (VariableTree) forPath.getParentPath().getLeaf();
                if (tree.getInitializer()== current)
                    return Collections.singletonList(at.trees().getTypeMirror(forPath.getParentPath()));
                break;
            }
            case ERRONEOUS:
                return findTargetType(at, forPath.getParentPath());
            case NEW_CLASS: {
                NewClassTree nct = (NewClassTree) forPath.getParentPath().getLeaf();
                List<TypeMirror> actuals = computeActualInvocationTypes(at, nct.getArguments(), forPath);

                if (actuals != null) {
                    Iterable<Pair<ExecutableElement, ExecutableType>> candidateConstructors = newClassCandidates(at, forPath.getParentPath());

                    return computeSmartTypesForExecutableType(at, candidateConstructors, actuals);
                } else {
                    return findTargetType(at, forPath.getParentPath());
                }
            }
            case METHOD:
                if (!isThrowsClause(forPath)) {
                    break;
                }
                // fall through
            case THROW:
                return Collections.singletonList(at.getElements().getTypeElement("java.lang.Throwable").asType());
            case METHOD_INVOCATION: {
                MethodInvocationTree mit = (MethodInvocationTree) forPath.getParentPath().getLeaf();
                List<TypeMirror> actuals = computeActualInvocationTypes(at, mit.getArguments(), forPath);

                if (actuals == null)
                    return null;

                Iterable<Pair<ExecutableElement, ExecutableType>> candidateMethods = methodCandidates(at, forPath.getParentPath());

                return computeSmartTypesForExecutableType(at, candidateMethods, actuals);
            }
        }

        return null;
    }

    private List<TypeMirror> computeActualInvocationTypes(AnalyzeTask at, List<? extends ExpressionTree> arguments, TreePath currentArgument) {
        if (currentArgument == null)
            return null;

        int paramIndex = arguments.indexOf(currentArgument.getLeaf());

        if (paramIndex == (-1))
            return null;

        List<TypeMirror> actuals = new ArrayList<>();

        for (ExpressionTree arg : arguments.subList(0, paramIndex)) {
            actuals.add(at.trees().getTypeMirror(new TreePath(currentArgument.getParentPath(), arg)));
        }

        return actuals;
    }

    private List<Pair<ExecutableElement, ExecutableType>> filterExecutableTypesByArguments(AnalyzeTask at, Iterable<Pair<ExecutableElement, ExecutableType>> candidateMethods, List<TypeMirror> precedingActualTypes) {
        List<Pair<ExecutableElement, ExecutableType>> candidate = new ArrayList<>();
        int paramIndex = precedingActualTypes.size();

        OUTER:
        for (Pair<ExecutableElement, ExecutableType> method : candidateMethods) {
            boolean varargInvocation = paramIndex >= method.snd.getParameterTypes().size();

            for (int i = 0; i < paramIndex; i++) {
                TypeMirror actual = precedingActualTypes.get(i);

                if (this.parameterType(method.fst, method.snd, i, !varargInvocation)
                        .noneMatch(formal -> at.getTypes().isAssignable(actual, formal))) {
                    continue OUTER;
                }
            }
            candidate.add(method);
        }

        return candidate;
    }

    private Stream<TypeMirror> parameterType(ExecutableElement method, ExecutableType methodType, int paramIndex, boolean allowVarArgsArray) {
        int paramCount = methodType.getParameterTypes().size();
        if (paramIndex >= paramCount && !method.isVarArgs())
            return Stream.empty();
        if (paramIndex < paramCount - 1 || !method.isVarArgs())
            return Stream.of(methodType.getParameterTypes().get(paramIndex));
        TypeMirror varargType = methodType.getParameterTypes().get(paramCount - 1);
        TypeMirror elemenType = ((ArrayType) varargType).getComponentType();
        if (paramIndex >= paramCount || !allowVarArgsArray)
            return Stream.of(elemenType);
        return Stream.of(varargType, elemenType);
    }

    private List<TypeMirror> computeSmartTypesForExecutableType(AnalyzeTask at, Iterable<Pair<ExecutableElement, ExecutableType>> candidateMethods, List<TypeMirror> precedingActualTypes) {
        List<TypeMirror> candidate = new ArrayList<>();
        int paramIndex = precedingActualTypes.size();

        this.filterExecutableTypesByArguments(at, candidateMethods, precedingActualTypes)
            .stream()
            .flatMap(method -> parameterType(method.fst, method.snd, paramIndex, true))
            .forEach(candidate::add);

        return candidate;
    }


    private TypeMirror resultTypeOf(Element el) {
        //TODO: should reflect the type of site!
        return switch (el.getKind()) {
            case METHOD -> ((ExecutableElement) el).getReturnType();
            // TODO: INSTANCE_INIT and STATIC_INIT should be filtered out
            case CONSTRUCTOR, INSTANCE_INIT, STATIC_INIT -> el.getEnclosingElement().asType();
            default -> el.asType();
        };
    }

    private void addScopeElements(AnalyzeTask at, Scope scope, Function<Element, Iterable<? extends Element>> elementConvertor, Predicate<Element> filter, Predicate<Element> smartFilter, List<Suggestion> result) {
        addElements(scopeContent(at, scope, elementConvertor), filter, smartFilter, result);
    }

    private Iterable<Pair<ExecutableElement, ExecutableType>> methodCandidates(AnalyzeTask at, TreePath invocation) {
        MethodInvocationTree mit = (MethodInvocationTree) invocation.getLeaf();
        ExpressionTree select = mit.getMethodSelect();
        List<Pair<ExecutableElement, ExecutableType>> result = new ArrayList<>();
        Predicate<Element> accessibility = createAccessibilityFilter(at, invocation);

        switch (select.getKind()) {
            case MEMBER_SELECT:
                MemberSelectTree mst = (MemberSelectTree) select;
                TreePath tp = new TreePath(new TreePath(invocation, select), mst.getExpression());
                TypeMirror site = at.trees().getTypeMirror(tp);

                if (site == null || site.getKind() != TypeKind.DECLARED)
                    break;

                Element siteEl = at.getTypes().asElement(site);

                if (siteEl == null)
                    break;

                if (isStaticContext(at, tp)) {
                    accessibility = accessibility.and(STATIC_ONLY);
                }

                for (ExecutableElement ee : ElementFilter.methodsIn(membersOf(at, siteEl.asType(), false))) {
                    if (ee.getSimpleName().contentEquals(mst.getIdentifier())) {
                        if (accessibility.test(ee)) {
                            result.add(Pair.of(ee, (ExecutableType) at.getTypes().asMemberOf((DeclaredType) site, ee)));
                        }
                    }
                }
                break;
            case IDENTIFIER:
                IdentifierTree it = (IdentifierTree) select;
                for (ExecutableElement ee : ElementFilter.methodsIn(scopeContent(at, at.trees().getScope(invocation), IDENTITY))) {
                    if (ee.getSimpleName().contentEquals(it.getName())) {
                        if (accessibility.test(ee)) {
                            result.add(Pair.of(ee, (ExecutableType) ee.asType())); //XXX: proper site
                        }
                    }
                }
                break;
            default:
                break;
        }

        return result;
    }

    private Iterable<Pair<ExecutableElement, ExecutableType>> newClassCandidates(AnalyzeTask at, TreePath newClassPath) {
        NewClassTree nct = (NewClassTree) newClassPath.getLeaf();
        Element type = at.trees().getElement(new TreePath(newClassPath.getParentPath(), nct.getIdentifier()));
        TypeMirror targetType = at.trees().getTypeMirror(newClassPath);
        if (targetType == null || targetType.getKind() != TypeKind.DECLARED) {
            Iterable<TypeMirror> targetTypes = findTargetType(at, newClassPath);
            if (targetTypes == null)
                targetTypes = Collections.emptyList();
            targetType =
                    StreamSupport.stream(targetTypes.spliterator(), false)
                                 .filter(t -> at.getTypes().asElement(t) == type)
                                 .findAny()
                                 .orElse(at.getTypes().erasure(type.asType()));
        }
        List<Pair<ExecutableElement, ExecutableType>> candidateConstructors = new ArrayList<>();
        Predicate<Element> accessibility = createAccessibilityFilter(at, newClassPath);

        if (targetType != null &&
            targetType.getKind() == TypeKind.DECLARED &&
            type != null &&
            (type.getKind().isClass() || type.getKind().isInterface())) {
            for (ExecutableElement constr : ElementFilter.constructorsIn(type.getEnclosedElements())) {
                if (accessibility.test(constr)) {
                    ExecutableType constrType =
                            (ExecutableType) at.getTypes().asMemberOf((DeclaredType) targetType, constr);
                    candidateConstructors.add(Pair.of(constr, constrType));
                }
            }
        }

        return candidateConstructors;
    }

    @Override
    public List<Documentation> documentation(String code, int cursor, boolean computeJavadoc) {
        suspendIndexing();
        try {
            return documentationImpl(code, cursor, computeJavadoc);
        } catch (Throwable exc) {
            proc.debug(exc, "Exception thrown in SourceCodeAnalysisImpl.documentation");
            return Collections.emptyList();
        } finally {
            resumeIndexing();
        }
    }

    //tweaked by tests to disable reading parameter names from classfiles so that tests using
    //JDK's classes are stable for both release and fastdebug builds:
    private final String[] keepParameterNames = new String[] {
        "-parameters"
    };

    private List<Documentation> documentationImpl(String code, int cursor, boolean computeJavadoc) {
        code = code.substring(0, cursor);
        if (code.trim().isEmpty()) { //TODO: comment handling
            code += ";";
        }

        if (guessKind(code) == Kind.IMPORT)
            return Collections.emptyList();

        OuterWrap codeWrap = proc.outerMap.wrapInTrialClass(Wrap.methodWrap(code));
        return proc.taskFactory.analyze(codeWrap, List.of(keepParameterNames), at -> {
            SourcePositions sp = at.trees().getSourcePositions();
            CompilationUnitTree topLevel = at.firstCuTree();
            TreePath tp = pathFor(topLevel, sp, codeWrap, cursor);

            if (tp == null)
                return Collections.emptyList();

            TreePath prevPath = null;
            while (tp != null && tp.getLeaf().getKind() != Kind.METHOD_INVOCATION &&
                   tp.getLeaf().getKind() != Kind.NEW_CLASS && tp.getLeaf().getKind() != Kind.IDENTIFIER &&
                   tp.getLeaf().getKind() != Kind.MEMBER_SELECT) {
                prevPath = tp;
                tp = tp.getParentPath();
            }

            if (tp == null)
                return Collections.emptyList();

            Stream<Element> elements;
            Iterable<Pair<ExecutableElement, ExecutableType>> candidates;
            List<? extends ExpressionTree> arguments;

            if (tp.getLeaf().getKind() == Kind.METHOD_INVOCATION || tp.getLeaf().getKind() == Kind.NEW_CLASS) {
                if (tp.getLeaf().getKind() == Kind.METHOD_INVOCATION) {
                    MethodInvocationTree mit = (MethodInvocationTree) tp.getLeaf();
                    candidates = methodCandidates(at, tp);
                    arguments = mit.getArguments();
                } else {
                    NewClassTree nct = (NewClassTree) tp.getLeaf();
                    candidates = newClassCandidates(at, tp);
                    arguments = nct.getArguments();
                }

                if (!isEmptyArgumentsContext(arguments)) {
                    List<TypeMirror> actuals = computeActualInvocationTypes(at, arguments, prevPath);
                    List<TypeMirror> fullActuals = actuals != null ? actuals : Collections.emptyList();

                    candidates =
                            this.filterExecutableTypesByArguments(at, candidates, fullActuals)
                                .stream()
                                .filter(method -> parameterType(method.fst, method.snd, fullActuals.size(), true).findAny().isPresent())
                                .toList();
                }

                elements = Util.stream(candidates).map(method -> method.fst);
            } else if (tp.getLeaf().getKind() == Kind.IDENTIFIER || tp.getLeaf().getKind() == Kind.MEMBER_SELECT) {
                Element el = at.trees().getElement(tp);

                if (el == null ||
                    el.asType().getKind() == TypeKind.ERROR ||
                    (el.getKind() == ElementKind.PACKAGE && el.getEnclosedElements().isEmpty())) {
                    //erroneous element:
                    return Collections.emptyList();
                }

                Predicate<Element> accessibility = createAccessibilityFilter(at, tp);

                if (!accessibility.test(el)) {
                    //not accessible
                    return Collections.emptyList();
                }

                elements = Stream.of(el);
            } else {
                return Collections.emptyList();
            }

            List<Documentation> result = Collections.emptyList();

            try (JavadocHelper helper = JavadocHelper.create(at.task, findSources())) {
                result = elements.map(el -> constructDocumentation(at, helper, el, computeJavadoc))
                                 .filter(Objects::nonNull)
                                 .toList();
            } catch (IOException ex) {
                proc.debug(ex, "JavadocHelper.close()");
            }

            return result;
        });
    }

    private Documentation constructDocumentation(AnalyzeTask at, JavadocHelper helper, Element el, boolean computeJavadoc) {
        String javadoc = null;
        try {
            if (hasSyntheticParameterNames(el)) {
                el = helper.getSourceElement(el);
            }
            if (computeJavadoc) {
                javadoc = helper.getResolvedDocComment(el);
            }
        } catch (IOException ex) {
            proc.debug(ex, "SourceCodeAnalysisImpl.element2String(..., " + el + ")");
        }
        String signature = Util.expunge(elementHeader(at, el, !hasSyntheticParameterNames(el), true));
        return new DocumentationImpl(signature,  javadoc);
    }

    public void close() {
        for (AutoCloseable closeable : closeables) {
            try {
                closeable.close();
            } catch (Exception ex) {
                proc.debug(ex, "SourceCodeAnalysisImpl.close()");
            }
        }
    }

    private static final class DocumentationImpl implements Documentation {

        private final String signature;
        private final String javadoc;

        public DocumentationImpl(String signature, String javadoc) {
            this.signature = signature;
            this.javadoc = javadoc;
        }

        @Override
        public String signature() {
            return signature;
        }

        @Override
        public String javadoc() {
            return javadoc;
        }

    }

    private boolean isEmptyArgumentsContext(List<? extends ExpressionTree> arguments) {
        if (arguments.size() == 1) {
            Tree firstArgument = arguments.get(0);
            return firstArgument.getKind() == Kind.ERRONEOUS;
        }
        return false;
    }

    private boolean hasSyntheticParameterNames(Element el) {
        if (el.getKind() != ElementKind.CONSTRUCTOR && el.getKind() != ElementKind.METHOD)
            return false;

        ExecutableElement ee = (ExecutableElement) el;

        if (ee.getParameters().isEmpty())
            return false;

        return ee.getParameters()
                 .stream()
                 .allMatch(param -> param.getSimpleName().toString().startsWith("arg"));
    }

    private static List<Path> availableSourcesOverride; //for tests
    private List<Path> availableSources;

    private List<Path> findSources() {
        if (availableSources != null) {
            return availableSources;
        }
        if (availableSourcesOverride != null) {
            return availableSources = availableSourcesOverride;
        }
        List<Path> result = new ArrayList<>();
        Path home = Paths.get(System.getProperty("java.home"));
        Path srcZip = home.resolve("lib").resolve("src.zip");
        if (!Files.isReadable(srcZip))
            srcZip = home.getParent().resolve("src.zip");
        if (Files.isReadable(srcZip)) {
            boolean keepOpen = false;
            FileSystem zipFO = null;

            try {
                URI uri = URI.create("jar:" + srcZip.toUri());
                zipFO = FileSystems.newFileSystem(uri, Collections.emptyMap());
                Path root = zipFO.getRootDirectories().iterator().next();

                if (Files.exists(root.resolve("java/lang/Object.java".replace("/", zipFO.getSeparator())))) {
                    //non-modular format:
                    result.add(srcZip);
                } else if (Files.exists(root.resolve("java.base/java/lang/Object.java".replace("/", zipFO.getSeparator())))) {
                    //modular format:
                    try (DirectoryStream<Path> ds = Files.newDirectoryStream(root)) {
                        for (Path p : ds) {
                            if (Files.isDirectory(p)) {
                                result.add(p);
                            }
                        }
                    }

                    keepOpen = true;
                }
            } catch (IOException ex) {
                proc.debug(ex, "SourceCodeAnalysisImpl.findSources()");
            } finally {
                if (zipFO != null) {
                    if (keepOpen) {
                        closeables.add(zipFO);
                    } else {
                        try {
                            zipFO.close();
                        } catch (IOException ex) {
                            proc.debug(ex, "SourceCodeAnalysisImpl.findSources()");
                        }
                    }
                }
            }
        }
        return availableSources = result;
    }

    private String elementHeader(AnalyzeTask at, Element el, boolean includeParameterNames, boolean useFQN) {
        switch (el.getKind()) {
            case ANNOTATION_TYPE: case CLASS: case ENUM: case INTERFACE: {
                TypeElement type = (TypeElement)el;
                String fullname = type.getQualifiedName().toString();
                Element pkg = at.getElements().getPackageOf(el);
                String name = pkg == null || useFQN ? fullname :
                        proc.maps.fullClassNameAndPackageToClass(fullname, ((PackageElement)pkg).getQualifiedName().toString());

                return name + typeParametersOpt(at, type.getTypeParameters(), includeParameterNames);
            }
            case TYPE_PARAMETER: {
                TypeParameterElement tp = (TypeParameterElement)el;
                String name = tp.getSimpleName().toString();

                List<? extends TypeMirror> bounds = tp.getBounds();
                boolean boundIsObject = bounds.isEmpty() ||
                        bounds.size() == 1 && at.getTypes().isSameType(bounds.get(0), Symtab.instance(at.getContext()).objectType);

                return boundIsObject
                        ? name
                        : name + " extends " + bounds.stream()
                                .map(bound -> printType(at, proc, bound))
                                .collect(joining(" & "));
            }
            case FIELD:
                return appendDot(elementHeader(at, el.getEnclosingElement(), includeParameterNames, false)) + el.getSimpleName() + ":" + el.asType();
            case ENUM_CONSTANT:
                return appendDot(elementHeader(at, el.getEnclosingElement(), includeParameterNames, false)) + el.getSimpleName();
            case EXCEPTION_PARAMETER: case LOCAL_VARIABLE: case PARAMETER: case RESOURCE_VARIABLE:
                return el.getSimpleName() + ":" + el.asType();
            case CONSTRUCTOR: case METHOD: {
                StringBuilder header = new StringBuilder();

                boolean isMethod = el.getKind() == ElementKind.METHOD;
                ExecutableElement method = (ExecutableElement) el;

                if (isMethod) {
                    // return type
                    header.append(printType(at, proc, method.getReturnType())).append(" ");
                } else {
                    // type parameters for the constructor
                    String typeParameters = typeParametersOpt(at, method.getTypeParameters(), includeParameterNames);
                    if (!typeParameters.isEmpty()) {
                        header.append(typeParameters).append(" ");
                    }
                }

                // receiver type
                String clazz = elementHeader(at, el.getEnclosingElement(), includeParameterNames, false);
                header.append(clazz);

                if (isMethod) {
                    //method name with type parameters
                    (clazz.isEmpty() ? header : header.append("."))
                            .append(typeParametersOpt(at, method.getTypeParameters(), includeParameterNames))
                            .append(el.getSimpleName());
                }

                // arguments
                header.append("(");
                String sep = "";
                for (Iterator<? extends VariableElement> i = method.getParameters().iterator(); i.hasNext();) {
                    VariableElement p = i.next();
                    header.append(sep);
                    if (!i.hasNext() && method.isVarArgs()) {
                        header.append(printType(at, proc, unwrapArrayType(p.asType()))).append("...");
                    } else {
                        header.append(printType(at, proc, p.asType()));
                    }
                    if (includeParameterNames) {
                        header.append(" ");
                        header.append(p.getSimpleName());
                    }
                    sep = ", ";
                }
                header.append(")");

                // throws
                List<? extends TypeMirror> thrownTypes = method.getThrownTypes();
                if (!thrownTypes.isEmpty()) {
                    header.append(" throws ")
                            .append(thrownTypes.stream()
                                    .map(type -> printType(at, proc, type))
                                    .collect(joining(", ")));
                }
                return header.toString();
            }
            default:
                return el.toString();
        }
    }
    private String appendDot(String fqn) {
        return fqn.isEmpty() ? fqn : fqn + ".";
    }
    private TypeMirror unwrapArrayType(TypeMirror arrayType) {
        if (arrayType.getKind() == TypeKind.ARRAY) {
            return ((ArrayType)arrayType).getComponentType();
        }
        return arrayType;
    }
    private String typeParametersOpt(AnalyzeTask at, List<? extends TypeParameterElement> typeParameters, boolean includeParameterNames) {
        return typeParameters.isEmpty() ? ""
                : typeParameters.stream()
                        .map(tp -> elementHeader(at, tp, includeParameterNames, false))
                        .collect(joining(", ", "<", ">"));
    }

    @Override
    public String analyzeType(String code, int cursor) {
        switch (guessKind(code)) {
            case IMPORT: case METHOD: case CLASS: case ENUM:
            case INTERFACE: case ANNOTATION_TYPE: case VARIABLE:
                return null;
            default:
                break;
        }
        ExpressionInfo ei = ExpressionToTypeInfo.expressionInfo(code, proc);
        return (ei == null || !ei.isNonVoid)
                ? null
                : ei.typeName;
    }

    @Override
    public QualifiedNames listQualifiedNames(String code, int cursor) {
        String codeFin = code.substring(0, cursor);
        if (codeFin.trim().isEmpty()) {
            return new QualifiedNames(Collections.emptyList(), -1, true, false);
        }
        OuterWrap codeWrap;
        switch (guessKind(codeFin)) {
            case IMPORT:
                return new QualifiedNames(Collections.emptyList(), -1, true, false);
            case METHOD:
                codeWrap = proc.outerMap.wrapInTrialClass(Wrap.classMemberWrap(codeFin));
                break;
            default:
                codeWrap = proc.outerMap.wrapInTrialClass(Wrap.methodWrap(codeFin));
                break;
        }
        return proc.taskFactory.analyze(codeWrap, at -> {
            SourcePositions sp = at.trees().getSourcePositions();
            CompilationUnitTree topLevel = at.firstCuTree();
            TreePath tp = pathFor(topLevel, sp, codeWrap, codeFin.length());
            if (tp.getLeaf().getKind() != Kind.IDENTIFIER) {
                return new QualifiedNames(Collections.emptyList(), -1, true, false);
            }
            Scope scope = at.trees().getScope(tp);
            TypeMirror type = at.trees().getTypeMirror(tp);
            Element el = at.trees().getElement(tp);

            boolean erroneous = (type.getKind() == TypeKind.ERROR && el.getKind() == ElementKind.CLASS) ||
                                (el.getKind() == ElementKind.PACKAGE && el.getEnclosedElements().isEmpty());
            String simpleName = ((IdentifierTree) tp.getLeaf()).getName().toString();
            boolean upToDate;
            List<String> result;

            synchronized (currentIndexes) {
                upToDate = classpathVersion == indexVersion;
                result = currentIndexes.values()
                                       .stream()
                                       .flatMap(idx -> idx.classSimpleName2FQN.getOrDefault(simpleName,
                                                                                            Collections.emptyList()).stream())
                                       .distinct()
                                       .filter(fqn -> isAccessible(at, scope, fqn))
                                       .sorted()
                                       .toList();
            }

            return new QualifiedNames(result, simpleName.length(), upToDate, !erroneous);
        });
    }

    private boolean isAccessible(AnalyzeTask at, Scope scope, String fqn) {
        TypeElement type = at.getElements().getTypeElement(fqn);
        if (type == null)
            return false;
        return at.trees().isAccessible(scope, type);
    }

    //--------------------
    // classpath indexing:
    //--------------------

    //the indexing can be suspended when a more important task is running:
    private void waitIndexingNotSuspended() {
        boolean suspendedNotified = false;
        synchronized (suspendLock) {
            while (suspend > 0) {
                if (!suspendedNotified) {
                    suspendedNotified = true;
                }
                try {
                    suspendLock.wait();
                } catch (InterruptedException ex) {
                }
            }
        }
    }

    public void suspendIndexing() {
        synchronized (suspendLock) {
            suspend++;
        }
    }

    public void resumeIndexing() {
        synchronized (suspendLock) {
            if (--suspend == 0) {
                suspendLock.notifyAll();
            }
        }
    }

    //update indexes, either initially or after a classpath change:
    private void refreshIndexes(int version) {
        try {
            Collection<Path> paths = new ArrayList<>();
            MemoryFileManager fm = proc.taskFactory.fileManager();

            appendPaths(fm, StandardLocation.PLATFORM_CLASS_PATH, paths);
            appendPaths(fm, StandardLocation.CLASS_PATH, paths);
            appendPaths(fm, StandardLocation.SOURCE_PATH, paths);

            Map<Path, ClassIndex> newIndexes = new HashMap<>();

            //setup existing/last known data:
            for (Path p : paths) {
                ClassIndex index = PATH_TO_INDEX.get(p);
                if (index != null) {
                    newIndexes.put(p, index);
                }
            }

            synchronized (currentIndexes) {
                //temporary setting old data:
                currentIndexes.clear();
                currentIndexes.putAll(newIndexes);
            }

            //update/compute the indexes if needed:
            for (Path p : paths) {
                waitIndexingNotSuspended();

                ClassIndex index = indexForPath(p);
                newIndexes.put(p, index);
            }

            synchronized (currentIndexes) {
                currentIndexes.clear();
                currentIndexes.putAll(newIndexes);
            }
        } catch (Exception ex) {
            proc.debug(ex, "SourceCodeAnalysisImpl.refreshIndexes(" + version + ")");
        } finally {
            synchronized (currentIndexes) {
                indexVersion = version;
            }
        }
    }

    private void appendPaths(MemoryFileManager fm, Location loc, Collection<Path> paths) {
        Iterable<? extends Path> locationPaths = fm.getLocationAsPaths(loc);
        if (locationPaths == null)
            return ;
        for (Path path : locationPaths) {
            if (".".equals(path.toString())) {
                //skip CWD
                continue;
            }

            paths.add(path);
        }
    }

    //create/update index a given JavaFileManager entry (which may be a JDK installation, a jar/zip file or a directory):
    //if an index exists for the given entry, the existing index is kept unless the timestamp is modified
    private ClassIndex indexForPath(Path path) {
        if (isJRTMarkerFile(path)) {
            FileSystem jrtfs = FileSystems.getFileSystem(URI.create("jrt:/"));
            Path modules = jrtfs.getPath("modules");
            return PATH_TO_INDEX.compute(path, (p, index) -> {
                try {
                    long lastModified = Files.getLastModifiedTime(modules).toMillis();
                    if (index == null || index.timestamp != lastModified) {
                        try (DirectoryStream<Path> stream = Files.newDirectoryStream(modules)) {
                            index = doIndex(lastModified, path, stream);
                        }
                    }
                    return index;
                } catch (IOException ex) {
                    proc.debug(ex, "SourceCodeAnalysisImpl.indexesForPath(" + path.toString() + ")");
                    return new ClassIndex(-1, path, Collections.emptySet(), Collections.emptyMap());
                }
            });
        } else if (!Files.isDirectory(path)) {
            if (Files.exists(path)) {
                return PATH_TO_INDEX.compute(path, (p, index) -> {
                    try {
                        long lastModified = Files.getLastModifiedTime(p).toMillis();
                        if (index == null || index.timestamp != lastModified) {
                            ClassLoader cl = SourceCodeAnalysisImpl.class.getClassLoader();

                            try (FileSystem zip = FileSystems.newFileSystem(path, cl)) {
                                index = doIndex(lastModified, path, zip.getRootDirectories());
                            }
                        }
                        return index;
                    } catch (IOException ex) {
                        proc.debug(ex, "SourceCodeAnalysisImpl.indexesForPath(" + path.toString() + ")");
                        return new ClassIndex(-1, path, Collections.emptySet(), Collections.emptyMap());
                    }
                });
            } else {
                return new ClassIndex(-1, path, Collections.emptySet(), Collections.emptyMap());
            }
        } else {
            return PATH_TO_INDEX.compute(path, (p, index) -> {
                //no persistence for directories, as we cannot check timestamps:
                if (index == null) {
                    index = doIndex(-1, path, Arrays.asList(p));
                }
                return index;
            });
        }
    }

    static boolean isJRTMarkerFile(Path path) {
        return path.equals(Paths.get(System.getProperty("java.home"), "lib", "modules"));
    }

    //create an index based on the content of the given dirs; the original JavaFileManager entry is originalPath.
    private ClassIndex doIndex(long timestamp, Path originalPath, Iterable<? extends Path> dirs) {
        Set<String> packages = new HashSet<>();
        Map<String, Collection<String>> classSimpleName2FQN = new HashMap<>();

        for (Path d : dirs) {
            try {
                Files.walkFileTree(d, new FileVisitor<Path>() {
                    int depth;
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        waitIndexingNotSuspended();
                        if (depth++ == 0)
                            return FileVisitResult.CONTINUE;
                        String dirName = dir.getFileName().toString();
                        String sep = dir.getFileSystem().getSeparator();
                        dirName = dirName.endsWith(sep) ? dirName.substring(0, dirName.length() - sep.length())
                                                        : dirName;
                        if (SourceVersion.isIdentifier(dirName))
                            return FileVisitResult.CONTINUE;
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        waitIndexingNotSuspended();
                        if (file.getFileName().toString().endsWith(".class")) {
                            String relativePath = d.relativize(file).toString();
                            String binaryName = relativePath.substring(0, relativePath.length() - 6).replace('/', '.');
                            int packageDot = binaryName.lastIndexOf('.');
                            if (packageDot > (-1)) {
                                packages.add(binaryName.substring(0, packageDot));
                            }
                            String typeName = binaryName.replace('$', '.');
                            addClassName2Map(classSimpleName2FQN, typeName);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }
                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        depth--;
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException ex) {
                proc.debug(ex, "doIndex(" + d.toString() + ")");
            }
        }

        return new ClassIndex(timestamp, originalPath, packages, classSimpleName2FQN);
    }

    private static void addClassName2Map(Map<String, Collection<String>> classSimpleName2FQN, String typeName) {
        int simpleNameDot = typeName.lastIndexOf('.');
        classSimpleName2FQN.computeIfAbsent(typeName.substring(simpleNameDot + 1), n -> new LinkedHashSet<>())
                           .add(typeName);
    }

    //holder for indexed data about a given path
    public static final class ClassIndex {
        public final long timestamp;
        public final Path forPath;
        public final Set<String> packages;
        public final Map<String, Collection<String>> classSimpleName2FQN;

        public ClassIndex(long timestamp, Path forPath, Set<String> packages, Map<String, Collection<String>> classSimpleName2FQN) {
            this.timestamp = timestamp;
            this.forPath = forPath;
            this.packages = packages;
            this.classSimpleName2FQN = classSimpleName2FQN;
        }

    }

    //for tests, to be able to wait until the indexing finishes:
    public void waitBackgroundTaskFinished() throws Exception {
        boolean upToDate;
        synchronized (currentIndexes) {
            upToDate = classpathVersion == indexVersion;
        }
        while (!upToDate) {
            INDEXER.submit(() -> {}).get();
            synchronized (currentIndexes) {
                upToDate = classpathVersion == indexVersion;
            }
        }
    }

    /**
     * A candidate for continuation of the given user's input.
     */
    private static class SuggestionImpl implements Suggestion {

        private final String continuation;
        private final boolean matchesType;

        /**
         * Create a {@code Suggestion} instance.
         *
         * @param continuation a candidate continuation of the user's input
         * @param matchesType does the candidate match the target type
         */
        public SuggestionImpl(String continuation, boolean matchesType) {
            this.continuation = continuation;
            this.matchesType = matchesType;
        }

        /**
         * The candidate continuation of the given user's input.
         *
         * @return the continuation string
         */
        @Override
        public String continuation() {
            return continuation;
        }

        /**
         * Indicates whether input continuation matches the target type and is thus
         * more likely to be the desired continuation. A matching continuation is
         * preferred.
         *
         * @return {@code true} if this suggested continuation matches the
         * target type; otherwise {@code false}
         */
        @Override
        public boolean matchesType() {
            return matchesType;
        }
    }

    /**
     * The result of {@code analyzeCompletion(String input)}.
     * Describes the completeness and position of the first snippet in the given input.
     */
    private static class CompletionInfoImpl implements CompletionInfo {

        private final Completeness completeness;
        private final String source;
        private final String remaining;

        CompletionInfoImpl(Completeness completeness, String source, String remaining) {
            this.completeness = completeness;
            this.source = source;
            this.remaining = remaining;
        }

        /**
         * The analyzed completeness of the input.
         *
         * @return an enum describing the completeness of the input string.
         */
        @Override
        public Completeness completeness() {
            return completeness;
        }

        /**
         * Input remaining after the complete part of the source.
         *
         * @return the portion of the input string that remains after the
         * complete Snippet
         */
        @Override
        public String remaining() {
            return remaining;
        }

        /**
         * Source code for the first Snippet of code input. For example, first
         * statement, or first method declaration. Trailing semicolons will be
         * added, as needed.
         *
         * @return the source of the first encountered Snippet
         */
        @Override
        public String source() {
            return source;
        }
    }

}
