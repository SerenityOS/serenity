/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.api;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.BreakIterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.ForwardingFileObject;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardLocation;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.EntityTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.Tree;
import com.sun.source.util.DocSourcePositions;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTreeScanner;
import com.sun.source.util.DocTrees;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Scope.NamedImportScope;
import com.sun.tools.javac.code.Scope.StarImportScope;
import com.sun.tools.javac.code.Scope.WriteableScope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.ModuleSymbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Type.UnionClassType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Check;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.MemberEnter;
import com.sun.tools.javac.comp.Modules;
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.file.BaseFileManager;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.parser.DocCommentParser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.parser.Tokens.Comment;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.resources.CompilerProperties.Errors;
import com.sun.tools.javac.resources.CompilerProperties.Notes;
import com.sun.tools.javac.resources.CompilerProperties.Warnings;
import com.sun.tools.javac.tree.DCTree;
import com.sun.tools.javac.tree.DCTree.DCBlockTag;
import com.sun.tools.javac.tree.DCTree.DCComment;
import com.sun.tools.javac.tree.DCTree.DCDocComment;
import com.sun.tools.javac.tree.DCTree.DCEndPosTree;
import com.sun.tools.javac.tree.DCTree.DCEntity;
import com.sun.tools.javac.tree.DCTree.DCErroneous;
import com.sun.tools.javac.tree.DCTree.DCIdentifier;
import com.sun.tools.javac.tree.DCTree.DCParam;
import com.sun.tools.javac.tree.DCTree.DCReference;
import com.sun.tools.javac.tree.DCTree.DCText;
import com.sun.tools.javac.tree.DocCommentTable;
import com.sun.tools.javac.tree.DocTreeMaker;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCCatch;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeCopier;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.Abort;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.DefinedBy;
import com.sun.tools.javac.util.DefinedBy.Api;
import com.sun.tools.javac.util.DiagnosticSource;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticFlag;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Pair;
import com.sun.tools.javac.util.Position;

import static com.sun.tools.javac.code.Kinds.Kind.*;

/**
 * Provides an implementation of Trees.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own
 * risk.  This code and its internal interfaces are subject to change
 * or deletion without notice.</b></p>
 *
 * @author Peter von der Ah&eacute;
 */
public class JavacTrees extends DocTrees {

    // in a world of a single context per compilation, these would all be final
    private Modules modules;
    private Resolve resolve;
    private Enter enter;
    private Log log;
    private MemberEnter memberEnter;
    private Attr attr;
    private Check chk;
    private TreeMaker treeMaker;
    private JavacElements elements;
    private JavacTaskImpl javacTaskImpl;
    private Names names;
    private Types types;
    private DocTreeMaker docTreeMaker;
    private BreakIterator breakIterator;
    private JavaFileManager fileManager;
    private ParserFactory parser;
    private Symtab syms;

    private final Map<Type, Type> extraType2OriginalMap = new WeakHashMap<>();

    // called reflectively from Trees.instance(CompilationTask task)
    public static JavacTrees instance(JavaCompiler.CompilationTask task) {
        if (!(task instanceof BasicJavacTask basicJavacTask))
            throw new IllegalArgumentException();
        return instance(basicJavacTask.getContext());
    }

    // called reflectively from Trees.instance(ProcessingEnvironment env)
    public static JavacTrees instance(ProcessingEnvironment env) {
        if (!(env instanceof JavacProcessingEnvironment javacProcessingEnvironment))
            throw new IllegalArgumentException();
        return instance(javacProcessingEnvironment.getContext());
    }

    public static JavacTrees instance(Context context) {
        JavacTrees instance = context.get(JavacTrees.class);
        if (instance == null)
            instance = new JavacTrees(context);
        return instance;
    }

    protected JavacTrees(Context context) {
        this.breakIterator = null;
        context.put(JavacTrees.class, this);
        init(context);
    }

    public void updateContext(Context context) {
        init(context);
    }

    private void init(Context context) {
        modules = Modules.instance(context);
        attr = Attr.instance(context);
        chk = Check.instance(context);
        enter = Enter.instance(context);
        elements = JavacElements.instance(context);
        log = Log.instance(context);
        resolve = Resolve.instance(context);
        treeMaker = TreeMaker.instance(context);
        memberEnter = MemberEnter.instance(context);
        names = Names.instance(context);
        types = Types.instance(context);
        docTreeMaker = DocTreeMaker.instance(context);
        parser = ParserFactory.instance(context);
        syms = Symtab.instance(context);
        fileManager = context.get(JavaFileManager.class);
        JavacTask t = context.get(JavacTask.class);
        if (t instanceof JavacTaskImpl taskImpl)
            javacTaskImpl = taskImpl;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public BreakIterator getBreakIterator() {
        return breakIterator;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DocSourcePositions getSourcePositions() {
        return new DocSourcePositions() {
                @Override @DefinedBy(Api.COMPILER_TREE)
                public long getStartPosition(CompilationUnitTree file, Tree tree) {
                    return TreeInfo.getStartPos((JCTree) tree);
                }

                @Override @DefinedBy(Api.COMPILER_TREE)
                public long getEndPosition(CompilationUnitTree file, Tree tree) {
                    EndPosTable endPosTable = ((JCCompilationUnit) file).endPositions;
                    return TreeInfo.getEndPos((JCTree) tree, endPosTable);
                }

                @Override @DefinedBy(Api.COMPILER_TREE)
                public long getStartPosition(CompilationUnitTree file, DocCommentTree comment, DocTree tree) {
                    return ((DCTree) tree).getSourcePosition((DCDocComment) comment);
                }
                @Override  @DefinedBy(Api.COMPILER_TREE) @SuppressWarnings("fallthrough")
                public long getEndPosition(CompilationUnitTree file, DocCommentTree comment, DocTree tree) {
                    DCDocComment dcComment = (DCDocComment) comment;
                    if (tree instanceof DCEndPosTree<?> dcEndPosTree) {
                        int endPos = dcEndPosTree.getEndPos(dcComment);

                        if (endPos != Position.NOPOS) {
                            return endPos;
                        }
                    }
                    int correction = 0;
                    switch (tree.getKind()) {
                        case TEXT:
                            DCText text = (DCText) tree;

                            return dcComment.comment.getSourcePos(text.pos + text.text.length());
                        case ERRONEOUS:
                            DCErroneous err = (DCErroneous) tree;

                            return dcComment.comment.getSourcePos(err.pos + err.body.length());
                        case IDENTIFIER:
                            DCIdentifier ident = (DCIdentifier) tree;

                            return dcComment.comment.getSourcePos(ident.pos + (ident.name != names.error ? ident.name.length() : 0));
                        case PARAM:
                            DCParam param = (DCParam) tree;

                            if (param.isTypeParameter && param.getDescription().isEmpty()) {
                                correction = 1;
                            }
                        case AUTHOR: case DEPRECATED: case RETURN: case SEE:
                        case SERIAL: case SERIAL_DATA: case SERIAL_FIELD: case SINCE:
                        case THROWS: case UNKNOWN_BLOCK_TAG: case VERSION: {
                            DocTree last = getLastChild(tree);

                            if (last != null) {
                                return getEndPosition(file, comment, last) + correction;
                            }

                            int pos;
                            String name;
                            if (tree.getKind() == DocTree.Kind.RETURN) {
                                DCTree.DCReturn dcReturn = (DCTree.DCReturn) tree;
                                pos = dcReturn.pos;
                                name = dcReturn.getTagName();
                            } else {
                                DCBlockTag block = (DCBlockTag) tree;
                                pos = block.pos;
                                name = block.getTagName();
                            }

                            return dcComment.comment.getSourcePos(pos + name.length() + 1);
                        }
                        case ENTITY: {
                            DCEntity endEl = (DCEntity) tree;
                            return dcComment.comment.getSourcePos(endEl.pos + (endEl.name != names.error ? endEl.name.length() : 0) + 2);
                        }
                        case COMMENT: {
                            DCComment endEl = (DCComment) tree;
                            return dcComment.comment.getSourcePos(endEl.pos + endEl.body.length());
                        }
                        default:
                            DocTree last = getLastChild(tree);

                            if (last != null) {
                                return getEndPosition(file, comment, last);
                            }
                            break;
                    }

                    return Position.NOPOS;
                }
            };
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DocTreeMaker getDocTreeFactory() {
        return docTreeMaker;
    }

    private DocTree getLastChild(DocTree tree) {
        final DocTree[] last = new DocTree[] {null};

        tree.accept(new DocTreeScanner<Void, Void>() {
            @Override @DefinedBy(Api.COMPILER_TREE)
            public Void scan(DocTree node, Void p) {
                if (node != null) last[0] = node;
                return null;
            }
        }, null);

        return last[0];
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public JCClassDecl getTree(TypeElement element) {
        return (JCClassDecl) getTree((Element) element);
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public JCMethodDecl getTree(ExecutableElement method) {
        return (JCMethodDecl) getTree((Element) method);
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public JCTree getTree(Element element) {
        return getTree(element, null);
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public JCTree getTree(Element e, AnnotationMirror a) {
        return getTree(e, a, null);
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public JCTree getTree(Element e, AnnotationMirror a, AnnotationValue v) {
        Pair<JCTree, JCCompilationUnit> treeTopLevel = elements.getTreeAndTopLevel(e, a, v);
        if (treeTopLevel == null)
            return null;
        return treeTopLevel.fst;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public TreePath getPath(CompilationUnitTree unit, Tree node) {
        return TreePath.getPath(unit, node);
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public TreePath getPath(Element e) {
        return getPath(e, null, null);
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public TreePath getPath(Element e, AnnotationMirror a) {
        return getPath(e, a, null);
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public TreePath getPath(Element e, AnnotationMirror a, AnnotationValue v) {
        final Pair<JCTree, JCCompilationUnit> treeTopLevel = elements.getTreeAndTopLevel(e, a, v);
        if (treeTopLevel == null)
            return null;
        return TreePath.getPath(treeTopLevel.snd, treeTopLevel.fst);
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Symbol getElement(TreePath path) {
        JCTree tree = (JCTree) path.getLeaf();
        Symbol sym = TreeInfo.symbolFor(tree);
        if (sym == null) {
            for (TreePath p = path; p != null; p = p.getParentPath()) {
                JCTree t = (JCTree) p.getLeaf();
                if (t.hasTag(JCTree.Tag.CLASSDEF)) {
                    JCClassDecl ct = (JCClassDecl) t;
                    if (ct.sym != null) {
                        if ((ct.sym.flags_field & Flags.UNATTRIBUTED) != 0) {
                            attr.attribClass(ct.pos(), ct.sym);
                            sym = TreeInfo.symbolFor(tree);
                        }
                        break;
                    }
                }
            }
        }
        return sym;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public Element getElement(DocTreePath path) {
        DocTree tree = path.getLeaf();
        if (tree instanceof DCReference dcReference)
            return attributeDocReference(path.getTreePath(), dcReference);
        if (tree instanceof DCIdentifier) {
            if (path.getParentPath().getLeaf() instanceof DCParam dcParam) {
                return attributeParamIdentifier(path.getTreePath(), dcParam);
            }
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public TypeMirror getType(DocTreePath path) {
        DocTree tree = path.getLeaf();
        if (tree instanceof DCReference dcReference) {
            JCTree qexpr = dcReference.qualifierExpression;
            if (qexpr != null) {
                Log.DeferredDiagnosticHandler deferredDiagnosticHandler =
                        new Log.DeferredDiagnosticHandler(log);
                try {
                    Env<AttrContext> env = getAttrContext(path.getTreePath());
                    Type t = attr.attribType(dcReference.qualifierExpression, env);
                    if (t != null && !t.isErroneous()) {
                        return t;
                    }
                } catch (Abort e) { // may be thrown by Check.completionError in case of bad class file
                    return null;
                } finally {
                    log.popDiagnosticHandler(deferredDiagnosticHandler);
                }
            }
        }
        Element e = getElement(path);
        return e == null ? null : e.asType();
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public java.util.List<DocTree> getFirstSentence(java.util.List<? extends DocTree> list) {
        return docTreeMaker.getFirstSentence(list);
    }

    private Symbol attributeDocReference(TreePath path, DCReference ref) {
        Env<AttrContext> env = getAttrContext(path);
        if (env == null) return null;
        if (ref.moduleName != null && ref.qualifierExpression == null && ref.memberName != null) {
            // module name and member name without type
            return null;
        }
        Log.DeferredDiagnosticHandler deferredDiagnosticHandler =
                new Log.DeferredDiagnosticHandler(log);
        try {
            final TypeSymbol tsym;
            final Name memberName;
            final ModuleSymbol mdlsym;

            if (ref.moduleName != null) {
                mdlsym = modules.modulesInitialized() ?
                        modules.getObservableModule(names.fromString(ref.moduleName.toString()))
                        : null;
                if (mdlsym == null) {
                    return null;
                } else if (ref.qualifierExpression == null) {
                    return mdlsym;
                }
            } else {
                mdlsym = modules.getDefaultModule();
            }

            if (ref.qualifierExpression == null) {
                tsym = env.enclClass.sym;
                memberName = (Name) ref.memberName;
            } else {
                // Check if qualifierExpression is a type or package, using the methods javac provides.
                // If no module name is given we check if qualifierExpression identifies a type.
                // If that fails or we have a module name, use that to resolve qualifierExpression to
                // a package or type.
                Type t = ref.moduleName == null ? attr.attribType(ref.qualifierExpression, env) : null;

                if (t == null || t.isErroneous()) {
                    JCCompilationUnit toplevel =
                        treeMaker.TopLevel(List.nil());
                    toplevel.modle = mdlsym;
                    toplevel.packge = mdlsym.unnamedPackage;
                    Symbol sym = attr.attribIdent(ref.qualifierExpression, toplevel);

                    if (sym == null) {
                        return null;
                    }

                    sym.complete();

                    if ((sym.kind == PCK || sym.kind == TYP) && sym.exists()) {
                        tsym = (TypeSymbol) sym;
                        memberName = (Name) ref.memberName;
                        if (sym.kind == PCK && memberName != null) {
                            //cannot refer to a package "member"
                            return null;
                        }
                    } else {
                        if (modules.modulesInitialized() && ref.moduleName == null && ref.memberName == null) {
                            // package/type does not exist, check if there is a matching module
                            ModuleSymbol moduleSymbol = modules.getObservableModule(names.fromString(ref.signature));
                            if (moduleSymbol != null) {
                                return moduleSymbol;
                            }
                        }
                        if (ref.qualifierExpression.hasTag(JCTree.Tag.IDENT) && ref.moduleName == null
                                && ref.memberName == null) {
                            // fixup:  allow "identifier" instead of "#identifier"
                            // for compatibility with javadoc
                            tsym = env.enclClass.sym;
                            memberName = ((JCIdent) ref.qualifierExpression).name;
                        } else {
                            return null;
                        }
                    }
                } else {
                    Type e = t;
                    // If this is an array type convert to element type
                    while (e instanceof ArrayType arrayType)
                        e = arrayType.elemtype;
                    tsym = e.tsym;
                    memberName = (Name) ref.memberName;
                }
            }

            if (memberName == null)
                return tsym;

            final List<Type> paramTypes;
            if (ref.paramTypes == null)
                paramTypes = null;
            else {
                ListBuffer<Type> lb = new ListBuffer<>();
                for (List<JCTree> l = (List<JCTree>) ref.paramTypes; l.nonEmpty(); l = l.tail) {
                    JCTree tree = l.head;
                    Type t = attr.attribType(tree, env);
                    lb.add(t);
                }
                paramTypes = lb.toList();
            }

            ClassSymbol sym = (ClassSymbol) types.skipTypeVars(tsym.type, false).tsym;

            Symbol msym = (memberName == sym.name)
                    ? findConstructor(sym, paramTypes)
                    : findMethod(sym, memberName, paramTypes);
            if (paramTypes != null) {
                // explicit (possibly empty) arg list given, so cannot be a field
                return msym;
            }

            VarSymbol vsym = (ref.paramTypes != null) ? null : findField(sym, memberName);
            // prefer a field over a method with no parameters
            if (vsym != null &&
                    (msym == null ||
                        types.isSubtypeUnchecked(vsym.enclClass().asType(), msym.enclClass().asType()))) {
                return vsym;
            } else {
                return msym;
            }
        } catch (Abort e) { // may be thrown by Check.completionError in case of bad class file
            return null;
        } finally {
            log.popDiagnosticHandler(deferredDiagnosticHandler);
        }
    }

    private Symbol attributeParamIdentifier(TreePath path, DCParam paramTag) {
        Symbol javadocSymbol = getElement(path);
        if (javadocSymbol == null)
            return null;
        ElementKind kind = javadocSymbol.getKind();
        List<? extends Symbol> params = List.nil();
        if (kind == ElementKind.METHOD || kind == ElementKind.CONSTRUCTOR) {
            MethodSymbol ee = (MethodSymbol) javadocSymbol;
            params = paramTag.isTypeParameter()
                    ? ee.getTypeParameters()
                    : ee.getParameters();
        } else if (kind.isClass() || kind.isInterface()) {
            ClassSymbol te = (ClassSymbol) javadocSymbol;
            params = paramTag.isTypeParameter()
                    ? te.getTypeParameters()
                    : te.getRecordComponents();
        }

        for (Symbol param : params) {
            if (param.getSimpleName() == paramTag.getName().getName()) {
                return param;
            }
        }
        return null;
    }

    private VarSymbol findField(ClassSymbol tsym, Name fieldName) {
        return searchField(tsym, fieldName, new HashSet<>());
    }

    private VarSymbol searchField(ClassSymbol tsym, Name fieldName, Set<ClassSymbol> searched) {
        if (searched.contains(tsym)) {
            return null;
        }
        searched.add(tsym);

        for (Symbol sym : tsym.members().getSymbolsByName(fieldName)) {
            if (sym.kind == VAR) {
                return (VarSymbol)sym;
            }
        }

        //### If we found a VarSymbol above, but which did not pass
        //### the modifier filter, we should return failure here!

        ClassSymbol encl = tsym.owner.enclClass();
        if (encl != null) {
            VarSymbol vsym = searchField(encl, fieldName, searched);
            if (vsym != null) {
                return vsym;
            }
        }

        // search superclass
        Type superclass = tsym.getSuperclass();
        if (superclass.tsym != null) {
            VarSymbol vsym = searchField((ClassSymbol) superclass.tsym, fieldName, searched);
            if (vsym != null) {
                return vsym;
            }
        }

        // search interfaces
        List<Type> intfs = tsym.getInterfaces();
        for (List<Type> l = intfs; l.nonEmpty(); l = l.tail) {
            Type intf = l.head;
            if (intf.isErroneous()) continue;
            VarSymbol vsym = searchField((ClassSymbol) intf.tsym, fieldName, searched);
            if (vsym != null) {
                return vsym;
            }
        }

        return null;
    }

    MethodSymbol findConstructor(ClassSymbol tsym, List<Type> paramTypes) {
        for (Symbol sym : tsym.members().getSymbolsByName(names.init)) {
            if (sym.kind == MTH) {
                if (hasParameterTypes((MethodSymbol) sym, paramTypes)) {
                    return (MethodSymbol) sym;
                }
            }
        }
        return null;
    }

    private MethodSymbol findMethod(ClassSymbol tsym, Name methodName, List<Type> paramTypes) {
        return searchMethod(tsym, methodName, paramTypes, new HashSet<>());
    }

    private MethodSymbol searchMethod(ClassSymbol tsym, Name methodName,
                                       List<Type> paramTypes, Set<ClassSymbol> searched) {
        //### Note that this search is not necessarily what the compiler would do!

        // do not match constructors
        if (methodName == names.init)
            return null;

        if (searched.contains(tsym))
            return null;
        searched.add(tsym);

        // search current class

        //### Using modifier filter here isn't really correct,
        //### but emulates the old behavior.  Instead, we should
        //### apply the normal rules of visibility and inheritance.

        if (paramTypes == null) {
            // If no parameters specified, we are allowed to return
            // any method with a matching name.  In practice, the old
            // code returned the first method, which is now the last!
            // In order to provide textually identical results, we
            // attempt to emulate the old behavior.
            MethodSymbol lastFound = null;
            for (Symbol sym : tsym.members().getSymbolsByName(methodName)) {
                if (sym.kind == MTH) {
                    if (sym.name == methodName) {
                        lastFound = (MethodSymbol)sym;
                    }
                }
            }
            if (lastFound != null) {
                return lastFound;
            }
        } else {
            for (Symbol sym : tsym.members().getSymbolsByName(methodName)) {
                if (sym != null &&
                    sym.kind == MTH) {
                    if (hasParameterTypes((MethodSymbol) sym, paramTypes)) {
                        return (MethodSymbol) sym;
                    }
                }
            }
        }

        //### If we found a MethodSymbol above, but which did not pass
        //### the modifier filter, we should return failure here!

        // search superclass
        Type superclass = tsym.getSuperclass();
        if (superclass.tsym != null) {
            MethodSymbol msym = searchMethod((ClassSymbol) superclass.tsym, methodName, paramTypes, searched);
            if (msym != null) {
                return msym;
            }
        }

        // search interfaces
        List<Type> intfs = tsym.getInterfaces();
        for (List<Type> l = intfs; l.nonEmpty(); l = l.tail) {
            Type intf = l.head;
            if (intf.isErroneous()) continue;
            MethodSymbol msym = searchMethod((ClassSymbol) intf.tsym, methodName, paramTypes, searched);
            if (msym != null) {
                return msym;
            }
        }

        // search enclosing class
        ClassSymbol encl = tsym.owner.enclClass();
        if (encl != null) {
            MethodSymbol msym = searchMethod(encl, methodName, paramTypes, searched);
            if (msym != null) {
                return msym;
            }
        }

        return null;
    }

    private boolean hasParameterTypes(MethodSymbol method, List<Type> paramTypes) {
        if (paramTypes == null)
            return true;

        if (method.params().size() != paramTypes.size())
            return false;

        List<Type> methodParamTypes = method.asType().getParameterTypes();
        if (!Type.isErroneous(paramTypes) && types.isSubtypes(paramTypes, methodParamTypes)) {
            return true;
        }

        methodParamTypes = types.erasureRecursive(methodParamTypes);
        return types.isSameTypes(paramTypes, methodParamTypes);
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public TypeMirror getTypeMirror(TreePath path) {
        Tree t = path.getLeaf();
        Type ty = ((JCTree)t).type;
        return ty == null ? null : ty.stripMetadataIfNeeded();
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public JavacScope getScope(TreePath path) {
        return JavacScope.create(getAttrContext(path));
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public String getDocComment(TreePath path) {
        CompilationUnitTree t = path.getCompilationUnit();
        Tree leaf = path.getLeaf();
        if (t instanceof JCTree.JCCompilationUnit compilationUnit && leaf instanceof JCTree tree) {
            if (compilationUnit.docComments != null) {
                return compilationUnit.docComments.getCommentText(tree);
            }
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DocCommentTree getDocCommentTree(TreePath path) {
        CompilationUnitTree t = path.getCompilationUnit();
        Tree leaf = path.getLeaf();
        if (t instanceof JCTree.JCCompilationUnit compilationUnit && leaf instanceof JCTree tree) {
            if (compilationUnit.docComments != null) {
                return compilationUnit.docComments.getCommentTree(tree);
            }
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DocCommentTree getDocCommentTree(Element e) {
        TreePath path = getPath(e);
        if (path == null) {
            return null;
        }
        return getDocCommentTree(path);
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DocCommentTree getDocCommentTree(Element e, String relativeFileName) throws IOException {
        PackageElement pkg = elements.getPackageOf(e);
        FileObject fileForInput = fileManager.getFileForInput(StandardLocation.SOURCE_PATH,
                pkg.getQualifiedName().toString(), relativeFileName);

        if (fileForInput == null) {
            throw new FileNotFoundException(relativeFileName);
        }
        return getDocCommentTree(fileForInput);
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public boolean isAccessible(Scope scope, TypeElement type) {
        return (scope instanceof JavacScope javacScope)
                && (type instanceof ClassSymbol classSymbol)
                && resolve.isAccessible(javacScope.env, classSymbol, true);
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public boolean isAccessible(Scope scope, Element member, DeclaredType type) {
        return (scope instanceof JavacScope javacScope)
                && (member instanceof Symbol symbol)
                && (type instanceof com.sun.tools.javac.code.Type codeType)
                && resolve.isAccessible(javacScope.env, codeType, symbol, true);
    }

    private Env<AttrContext> getAttrContext(TreePath path) {
        if (!(path.getLeaf() instanceof JCTree))  // implicit null-check
            throw new IllegalArgumentException();

        // if we're being invoked from a Tree API client via parse/enter/analyze,
        // we need to make sure all the classes have been entered;
        // if we're being invoked from JSR 199 or JSR 269, then the classes
        // will already have been entered.
        if (javacTaskImpl != null) {
            javacTaskImpl.enter(null);
        }

        JCCompilationUnit unit = (JCCompilationUnit) path.getCompilationUnit();
        Copier copier = createCopier(treeMaker.forToplevel(unit));

        Env<AttrContext> env = null;
        JCMethodDecl method = null;
        JCVariableDecl field = null;

        List<Tree> l = List.nil();
        TreePath p = path;
        while (p != null) {
            l = l.prepend(p.getLeaf());
            p = p.getParentPath();
        }

        for ( ; l.nonEmpty(); l = l.tail) {
            Tree tree = l.head;
            switch (tree.getKind()) {
                case COMPILATION_UNIT:
//                    System.err.println("COMP: " + ((JCCompilationUnit)tree).sourcefile);
                    env = enter.getTopLevelEnv((JCCompilationUnit)tree);
                    break;
                case ANNOTATION_TYPE:
                case CLASS:
                case ENUM:
                case INTERFACE:
                case RECORD:
//                    System.err.println("CLASS: " + ((JCClassDecl)tree).sym.getSimpleName());
                    env = enter.getClassEnv(((JCClassDecl)tree).sym);
                    if (env == null) return null;
                    break;
                case METHOD:
//                    System.err.println("METHOD: " + ((JCMethodDecl)tree).sym.getSimpleName());
                    method = (JCMethodDecl)tree;
                    env = memberEnter.getMethodEnv(method, env);
                    break;
                case VARIABLE:
//                    System.err.println("FIELD: " + ((JCVariableDecl)tree).sym.getSimpleName());
                    field = (JCVariableDecl)tree;
                    break;
                case BLOCK: {
//                    System.err.println("BLOCK: ");
                    if (method != null) {
                        try {
                            Assert.check(method.body == tree);
                            method.body = copier.copy((JCBlock)tree, (JCTree) path.getLeaf());
                            env = attribStatToTree(method.body, env, copier.leafCopy, copier.copiedClasses);
                        } finally {
                            method.body = (JCBlock) tree;
                        }
                    } else {
                        JCBlock body = copier.copy((JCBlock)tree, (JCTree) path.getLeaf());
                        env = attribStatToTree(body, env, copier.leafCopy, copier.copiedClasses);
                    }
                    return env;
                }
                default:
//                    System.err.println("DEFAULT: " + tree.getKind());
                    if (field != null && field.getInitializer() == tree) {
                        env = memberEnter.getInitEnv(field, env);
                        JCExpression expr = copier.copy((JCExpression)tree, (JCTree) path.getLeaf());
                        env = attribExprToTree(expr, env, copier.leafCopy, copier.copiedClasses);
                        return env;
                    }
            }
        }
        return (field != null) ? memberEnter.getInitEnv(field, env) : env;
    }

    private Env<AttrContext> attribStatToTree(JCTree stat, Env<AttrContext>env,
                                              JCTree tree, Map<JCClassDecl, JCClassDecl> copiedClasses) {
        Env<AttrContext> result = attr.attribStatToTree(stat, env, tree);

        fixLocalClassNames(copiedClasses, env);

        return result;
    }

    private Env<AttrContext> attribExprToTree(JCExpression expr, Env<AttrContext>env,
                                              JCTree tree, Map<JCClassDecl, JCClassDecl> copiedClasses) {
        Env<AttrContext> result = attr.attribExprToTree(expr, env, tree);

        fixLocalClassNames(copiedClasses, env);

        return result;
    }

    /* Change the flatnames of the local and anonymous classes in the Scope to
     * the names they would have if the whole file was attributed normally.
     */
    private void fixLocalClassNames(Map<JCClassDecl, JCClassDecl> copiedClasses,
                                    Env<AttrContext> lastEnv) {
        Map<JCClassDecl, Name> flatnameForClass = null;

        for (Entry<JCClassDecl, JCClassDecl> e : copiedClasses.entrySet()) {
            if (e.getKey().sym != null) {
                Name origName;
                if (e.getValue().sym != null) {
                    //if the source tree was already attributed, use the flatname
                    //from the source tree's Symbol:
                    origName = e.getValue().sym.flatname;
                } else {
                    //otherwise, compute the flatnames (for source trees) as
                    //if the full source code would be attributed:
                    if (flatnameForClass == null) {
                        flatnameForClass = prepareFlatnameForClass(lastEnv);
                    }
                    origName = flatnameForClass.get(e.getValue());
                }
                if (origName != null) {
                    e.getKey().sym.flatname = origName;
                }
            }
        }
    }

    /* This method computes and assigns flatnames to trees, as if they would be
     * normally assigned during attribution of the full source code.
     */
    private Map<JCTree.JCClassDecl, Name> prepareFlatnameForClass(Env<AttrContext> env) {
        Map<JCClassDecl, Name> flatNameForClass = new HashMap<>();
        Symbol enclClass = env.enclClass.sym;

        if (enclClass != null && (enclClass.flags_field & Flags.UNATTRIBUTED) != 0) {
            ListBuffer<ClassSymbol> toClear = new ListBuffer<>();
            new TreeScanner() {
                Symbol owner;
                boolean localContext;
                @Override
                public void visitClassDef(JCClassDecl tree) {
                    //compute the name (and ClassSymbol) which would be used
                    //for this class for full attribution
                    Symbol prevOwner = owner;
                    try {
                        ClassSymbol c;
                        if (tree.sym != null) {
                            //already entered:
                            c = tree.sym;
                        } else {
                            c = syms.defineClass(tree.name, owner);
                            if (owner.kind != TYP) {
                                //for local classes, assign the flatname
                                c.flatname = chk.localClassName(c);
                                chk.putCompiled(c);
                                toClear.add(c);
                            }
                            flatNameForClass.put(tree, c.flatname);
                        }
                        owner = c;
                        super.visitClassDef(tree);
                    } finally {
                        owner = prevOwner;
                    }
                }

                @Override
                public void visitBlock(JCBlock tree) {
                    Symbol prevOwner = owner;
                    try {
                        owner = new MethodSymbol(0, names.empty, Type.noType, owner);
                        super.visitBlock(tree);
                    } finally {
                        owner = prevOwner;
                    }
                }
                @Override
                public void visitVarDef(JCVariableDecl tree) {
                    Symbol prevOwner = owner;
                    try {
                        owner = new MethodSymbol(0, names.empty, Type.noType, owner);
                        super.visitVarDef(tree);
                    } finally {
                        owner = prevOwner;
                    }
                }
            }.scan(env.enclClass);
            //revert changes done by the visitor:
            toClear.stream().forEach(c -> {
                chk.clearLocalClassNameIndexes(c);
                chk.removeCompiled(c);
            });
        }

        return flatNameForClass;
    }

    static JavaFileObject asJavaFileObject(FileObject fileObject) {
        JavaFileObject jfo = null;

        if (fileObject instanceof JavaFileObject javaFileObject) {
            checkHtmlKind(fileObject, Kind.HTML);
            return javaFileObject;
        }

        checkHtmlKind(fileObject);
        jfo = new HtmlFileObject(fileObject);
        return jfo;
    }

    private static void checkHtmlKind(FileObject fileObject) {
        checkHtmlKind(fileObject, BaseFileManager.getKind(fileObject.getName()));
    }

    private static void checkHtmlKind(FileObject fileObject, JavaFileObject.Kind kind) {
        if (kind != JavaFileObject.Kind.HTML) {
            throw new IllegalArgumentException("HTML file expected:" + fileObject.getName());
        }
    }

    private static class HtmlFileObject extends ForwardingFileObject<FileObject>
            implements JavaFileObject {

        public HtmlFileObject(FileObject fileObject) {
            super(fileObject);
        }

        @Override @DefinedBy(Api.COMPILER)
        public Kind getKind() {
            return BaseFileManager.getKind(fileObject.getName());
        }

        @Override @DefinedBy(Api.COMPILER)
        public boolean isNameCompatible(String simpleName, Kind kind) {
            return false;
        }

        @Override @DefinedBy(Api.COMPILER)
        public NestingKind getNestingKind() {
            return null;
        }

        @Override @DefinedBy(Api.COMPILER)
        public Modifier getAccessLevel() {
            return null;
        }
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DocCommentTree getDocCommentTree(FileObject fileObject) {
        JavaFileObject jfo = asJavaFileObject(fileObject);
        DiagnosticSource diagSource = new DiagnosticSource(jfo, log);

        final Comment comment = new Comment() {
            int offset = 0;
            @Override
            public String getText() {
                try {
                    CharSequence rawDoc = fileObject.getCharContent(true);
                    return rawDoc.toString();
                } catch (IOException ignore) {
                    // do nothing
                }
                return "";
            }

            @Override
            public int getSourcePos(int index) {
                return offset + index;
            }

            @Override
            public CommentStyle getStyle() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isDeprecated() {
                throw new UnsupportedOperationException();
            }
        };

        return new DocCommentParser(parser, diagSource, comment, true).parse();
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public DocTreePath getDocTreePath(FileObject fileObject, PackageElement packageElement) {
        JavaFileObject jfo = asJavaFileObject(fileObject);
        DocCommentTree docCommentTree = getDocCommentTree(jfo);
        if (docCommentTree == null)
            return null;
        TreePath treePath = makeTreePath((PackageSymbol)packageElement, jfo, docCommentTree);
        return new DocTreePath(treePath, docCommentTree);
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public void setBreakIterator(BreakIterator breakiterator) {
        this.breakIterator = breakiterator;
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public String getCharacters(EntityTree tree) {
        return Entity.getCharacters(tree);
    }

    /**
     * Makes a copy of a tree, noting the value resulting from copying a particular leaf.
     **/
    protected static class Copier extends TreeCopier<JCTree> {
        JCTree leafCopy = null;
        private Map<JCClassDecl, JCClassDecl> copiedClasses = new HashMap<>();

        protected Copier(TreeMaker M) {
            super(M);
        }

        @Override
        public <T extends JCTree> T copy(T t, JCTree leaf) {
            T t2 = super.copy(t, leaf);
            if (t == leaf)
                leafCopy = t2;
            return t2;
        }

        @Override
        public JCTree visitClass(ClassTree node, JCTree p) {
            JCTree nue = super.visitClass(node, p);
            copiedClasses.put((JCClassDecl) nue, (JCClassDecl) node);
            return nue;
        }

    }

    protected Copier createCopier(TreeMaker maker) {
        return new Copier(maker);
    }

    /**
     * Returns the original type from the ErrorType object.
     * @param errorType The errorType for which we want to get the original type.
     * @return TypeMirror corresponding to the original type, replaced by the ErrorType.
     *         noType (type.tag == NONE) is returned if there is no original type.
     */
    @Override @DefinedBy(Api.COMPILER_TREE)
    public TypeMirror getOriginalType(javax.lang.model.type.ErrorType errorType) {
        if (errorType instanceof com.sun.tools.javac.code.Type.ErrorType targetErrorType) {
            return targetErrorType.getOriginalType();
        }
        if (errorType instanceof com.sun.tools.javac.code.Type.ClassType classType &&
            errorType.getKind() == TypeKind.ERROR) {
            return extraType2OriginalMap.computeIfAbsent(classType, tt ->
                    new ClassType(classType.getEnclosingType(), classType.typarams_field,
                            classType.tsym, classType.getMetadata()) {
                        @Override
                        public Type baseType() { return classType; }
                        @Override
                        public TypeKind getKind() {
                            return TypeKind.DECLARED;
                        }
                    });
        }

        return com.sun.tools.javac.code.Type.noType;
    }

    /**
     * Prints a message of the specified kind at the location of the
     * tree within the provided compilation unit
     *
     * @param kind the kind of message
     * @param msg  the message, or an empty string if none
     * @param t    the tree to use as a position hint
     * @param root the compilation unit that contains tree
     */
    @Override @DefinedBy(Api.COMPILER_TREE)
    public void printMessage(Diagnostic.Kind kind, CharSequence msg,
            com.sun.source.tree.Tree t,
            com.sun.source.tree.CompilationUnitTree root) {
        printMessage(kind, msg, ((JCTree) t).pos(), root);
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public void printMessage(Diagnostic.Kind kind, CharSequence msg,
            com.sun.source.doctree.DocTree t,
            com.sun.source.doctree.DocCommentTree c,
            com.sun.source.tree.CompilationUnitTree root) {
        printMessage(kind, msg, ((DCTree) t).pos((DCDocComment) c), root);
    }

    private void printMessage(Diagnostic.Kind kind, CharSequence msg,
            JCDiagnostic.DiagnosticPosition pos,
            com.sun.source.tree.CompilationUnitTree root) {
        JavaFileObject oldSource = null;
        JavaFileObject newSource = null;

        newSource = root.getSourceFile();
        if (newSource == null) {
            pos = null;
        } else {
            oldSource = log.useSource(newSource);
        }

        try {
            switch (kind) {
            case ERROR:
                log.error(DiagnosticFlag.API, pos, Errors.ProcMessager(msg.toString()));
                break;

            case WARNING:
                log.warning(pos, Warnings.ProcMessager(msg.toString()));
                break;

            case MANDATORY_WARNING:
                log.mandatoryWarning(pos, Warnings.ProcMessager(msg.toString()));
                break;

            default:
                log.note(pos, Notes.ProcMessager(msg.toString()));
            }
        } finally {
            if (oldSource != null)
                log.useSource(oldSource);
        }
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public TypeMirror getLub(CatchTree tree) {
        JCCatch ct = (JCCatch) tree;
        JCVariableDecl v = ct.param;
        if (v.type != null && v.type.getKind() == TypeKind.UNION) {
            UnionClassType ut = (UnionClassType) v.type;
            return ut.getLub();
        } else {
            return v.type;
        }
    }

    private TreePath makeTreePath(final PackageSymbol psym, final JavaFileObject jfo,
            DocCommentTree dcTree) {
        JCCompilationUnit jcCompilationUnit = new JCCompilationUnit(List.nil()) {
            public int getPos() {
                return Position.FIRSTPOS;
            }

            public JavaFileObject getSourcefile() {
                return jfo;
            }

            @Override @DefinedBy(Api.COMPILER_TREE)
            public Position.LineMap getLineMap() {
                try {
                    CharSequence content = jfo.getCharContent(true);
                    String s = content.toString();
                    return Position.makeLineMap(s.toCharArray(), s.length(), true);
                } catch (IOException ignore) {}
                return null;
            }
        };

        jcCompilationUnit.docComments = new DocCommentTable() {
            @Override
            public boolean hasComment(JCTree tree) {
                return false;
            }

            @Override
            public Comment getComment(JCTree tree) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String getCommentText(JCTree tree) {
                throw new UnsupportedOperationException();
            }

            @Override
            public DCDocComment getCommentTree(JCTree tree) {
                return (DCDocComment)dcTree;
            }

            @Override
            public void putComment(JCTree tree, Comment c) {
                throw new UnsupportedOperationException();
            }

        };
        jcCompilationUnit.lineMap = jcCompilationUnit.getLineMap();
        jcCompilationUnit.modle = psym.modle;
        jcCompilationUnit.sourcefile = jfo;
        jcCompilationUnit.namedImportScope = new NamedImportScope(psym);
        jcCompilationUnit.packge = psym;
        jcCompilationUnit.starImportScope = new StarImportScope(psym);
        jcCompilationUnit.toplevelScope = WriteableScope.create(psym);
        return new TreePath(jcCompilationUnit);
    }
}
