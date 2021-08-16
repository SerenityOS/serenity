/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.internal.tool;


import java.util.*;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;

import com.sun.source.util.DocTrees;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.ClassFinder;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Source;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.CompletionFailure;
import com.sun.tools.javac.code.Symbol.ModuleSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Check;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCPackageDecl;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Convert;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

/**
 * Holds the environment for a run of javadoc.
 * Holds only the information needed throughout the
 * run and not the compiler info that could be GC'ed
 * or ported.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class ToolEnvironment {
    protected static final Context.Key<ToolEnvironment> ToolEnvKey = new Context.Key<>();

    public static ToolEnvironment instance(Context context) {
        ToolEnvironment instance = context.get(ToolEnvKey);
        if (instance == null)
            instance = new ToolEnvironment(context);
        return instance;
    }

    final JavadocLog log;

    /** Predefined symbols known to the compiler. */
    public final Symtab syms;

    /** JavaDoc's subtype of the compiler's class finder */
    private final ClassFinder finder;

    /** Javadoc's subtype of the compiler's enter phase. */
    final Enter enter;

    /** The name table. */
    private Names names;

    /** If true, prevent printing of any notifications. */
    boolean quiet = false;

    /** If true, ignore all errors encountered during Enter. */
    boolean ignoreSourceErrors = false;

    Check chk;
    com.sun.tools.javac.code.Types types;
    JavaFileManager fileManager;
    public final Context context;

    WeakHashMap<JCTree, TreePath> treePaths = new WeakHashMap<>();

    /** Allow documenting from class files? */
    boolean docClasses = false;

    /**
     * The source language version.
     */
    public final Source source;

    public final Elements elements;

    public final JavacTypes typeutils;

    protected DocEnvImpl docEnv;

    public final DocTrees docTrees;

    public final Map<Element, TreePath> elementToTreePath;

    /**
     * Constructor
     *
     * @param context      Context for this javadoc instance.
     */
    protected ToolEnvironment(Context context) {
        context.put(ToolEnvKey, this);
        this.context = context;

        log = JavadocLog.instance0(context);
        syms = Symtab.instance(context);
        finder = JavadocClassFinder.instance(context);
        enter = JavadocEnter.instance(context);
        names = Names.instance(context);
        chk = Check.instance(context);
        types = com.sun.tools.javac.code.Types.instance(context);
        fileManager = context.get(JavaFileManager.class);
        if (fileManager instanceof JavacFileManager jfm) {
            jfm.setSymbolFileEnabled(false);
        }
        docTrees = JavacTrees.instance(context);
        source = Source.instance(context);
        elements =  JavacElements.instance(context);
        typeutils = JavacTypes.instance(context);
        elementToTreePath = new HashMap<>();
    }

    public void initialize(ToolOptions options) {
        this.quiet = options.quiet();
        this.ignoreSourceErrors = options.ignoreSourceErrors();
    }

    /**
     * Load a class by qualified name.
     */
    public TypeElement loadClass(String name) {
        try {
            Name nameImpl = names.fromString(name);
            ModuleSymbol mod = syms.inferModule(Convert.packagePart(nameImpl));
            ClassSymbol c = finder.loadClass(mod != null ? mod : syms.errModule, nameImpl);
            return c;
        } catch (CompletionFailure ex) {
            chk.completionError(null, ex);
            return null;
        }
    }

    boolean isSynthetic(Symbol sym) {
        return (sym.flags() & Flags.SYNTHETIC) != 0;
    }

    void setElementToTreePath(Element e, TreePath tree) {
        if (e == null || tree == null)
            return;
        elementToTreePath.put(e, tree);
    }

    public Kind getFileKind(TypeElement te) {
        JavaFileObject jfo = ((ClassSymbol)te).outermostClass().classfile;
        return jfo == null ? Kind.SOURCE : jfo.getKind();
    }

    /**
     * Print a notice, iff <em>quiet</em> is not specified.
     *
     * @param key selects message from resource
     */
    public void notice(String key) {
        if (quiet) {
            return;
        }
        JavadocLog.printRawLines(log.getDiagnosticWriter(), log.getText(key));
    }

    /**
     * Print a notice, iff <em>quiet</em> is not specified.
     *
     * @param key selects message from resource
     * @param a1 first argument
     */
    public void notice(String key, String a1) {
        if (quiet) {
            return;
        }
        JavadocLog.printRawLines(log.getDiagnosticWriter(), log.getText(key, a1));
    }

    TreePath getTreePath(JCCompilationUnit tree) {
        TreePath p = treePaths.get(tree);
        if (p == null)
            treePaths.put(tree, p = new TreePath(tree));
        return p;
    }

    TreePath getTreePath(JCCompilationUnit toplevel, JCPackageDecl tree) {
        TreePath p = treePaths.get(tree);
        if (p == null)
            treePaths.put(tree, p = new TreePath(getTreePath(toplevel), tree));
        return p;
    }

    TreePath getTreePath(JCCompilationUnit toplevel, JCClassDecl tree) {
        TreePath p = treePaths.get(tree);
        if (p == null)
            treePaths.put(tree, p = new TreePath(getTreePath(toplevel), tree));
        return p;
    }

    TreePath getTreePath(JCCompilationUnit toplevel, JCClassDecl cdecl, JCTree tree) {
        return new TreePath(getTreePath(toplevel, cdecl), tree);
    }

    public com.sun.tools.javac.code.Types getTypes() {
        return types;
    }

    public Env<AttrContext> getEnv(ClassSymbol tsym) {
        return enter.getEnv(tsym);
    }

    public boolean isQuiet() {
        return quiet;
    }
}
