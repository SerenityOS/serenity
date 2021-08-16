/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

import com.sun.tools.javac.code.ClassFinder;
import com.sun.tools.javac.code.DeferredCompletionFailureHandler;
import com.sun.tools.javac.code.Symbol.Completer;
import com.sun.tools.javac.code.Symbol.CompletionFailure;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Abort;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Position;
import jdk.javadoc.doclet.DocletEnvironment;

import static jdk.javadoc.internal.tool.Main.Result.*;

/**
 *  This class could be the main entry point for Javadoc when Javadoc is used as a
 *  component in a larger software system. It provides operations to
 *  construct a new javadoc processor, and to run it on a set of source
 *  files.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class JavadocTool extends com.sun.tools.javac.main.JavaCompiler {
    ToolEnvironment toolEnv;

    final JavadocLog log;
    final ClassFinder javadocFinder;
    final DeferredCompletionFailureHandler dcfh;
    final Enter javadocEnter;
    final Set<JavaFileObject> uniquefiles;

    /**
     * Construct a new JavaCompiler processor, using appropriately
     * extended phases of the underlying compiler.
     */
    protected JavadocTool(Context context) {
        super(context);
        log = JavadocLog.instance0(context);
        javadocFinder = JavadocClassFinder.instance(context);
        dcfh = DeferredCompletionFailureHandler.instance(context);
        javadocEnter = JavadocEnter.instance(context);
        uniquefiles = new HashSet<>();
    }

    /**
     * For javadoc, the parser needs to keep comments. Overrides method from JavaCompiler.
     */
    @Override
    protected boolean keepComments() {
        return true;
    }

    /**
     *  Construct a new javadoc tool.
     */
    public static JavadocTool make0(Context context) {
        JavadocLog log = null;
        try {
            // force the use of Javadoc's class finder
            JavadocClassFinder.preRegister(context);

            // force the use of Javadoc's own enter phase
            JavadocEnter.preRegister(context);

            // force the use of Javadoc's own member enter phase
            JavadocMemberEnter.preRegister(context);

            // force the use of Javadoc's own todo phase
            JavadocTodo.preRegister(context);

            // force the use of Javadoc's subtype of Log
            log = JavadocLog.instance0(context);

            return new JavadocTool(context);
        } catch (CompletionFailure ex) {
            assert log != null;
            log.error(Position.NOPOS, ex.getMessage());
            return null;
        }
    }

    public DocletEnvironment getEnvironment(ToolOptions toolOptions,
                                            List<String> javaNames,
                                            Iterable<? extends JavaFileObject> fileObjects)
            throws ToolException
    {
        toolEnv = ToolEnvironment.instance(context);
        toolEnv.initialize(toolOptions);
        ElementsTable etable = new ElementsTable(context, toolOptions);
        javadocFinder.sourceCompleter = etable.xclasses
                ? Completer.NULL_COMPLETER
                : sourceCompleter;

        if (etable.xclasses) {
            // If -Xclasses is set, the args should be a list of class names
            for (String arg: javaNames) {
                if (!isValidPackageName(arg)) { // checks
                    String text = log.getText("main.illegal_class_name", arg);
                    throw new ToolException(CMDERR, text);
                }
            }
            if (log.hasErrors()) {
                return null;
            }
            etable.setClassArgList(javaNames);
            // prepare, force the data structures to be analyzed
            etable.analyze();
            return new DocEnvImpl(toolEnv, etable);
        }

        ListBuffer<JCCompilationUnit> classTrees = new ListBuffer<>();

        try {
            StandardJavaFileManager fm = toolEnv.fileManager instanceof StandardJavaFileManager sfm
                    ? sfm
                    : null;
            Set<String> packageNames = new LinkedHashSet<>();
            // Normally, the args should be a series of package names or file names.
            // Parse the files and collect the package names.
            for (String arg: javaNames) {
                if (fm != null && arg.endsWith(".java") && isRegularFile(arg)) {
                    parse(fm.getJavaFileObjects(arg), classTrees, true);
                } else if (isValidPackageName(arg)) {
                    packageNames.add(arg);
                } else if (arg.endsWith(".java")) {
                    if (fm == null) {
                        String text = log.getText("main.assertion.error", "fm == null");
                        throw new ToolException(ABNORMAL, text);
                    } else {
                        String text = log.getText("main.file_not_found", arg);
                        throw new ToolException(ERROR, text);
                    }
                } else {
                    String text = log.getText("main.illegal_package_name", arg);
                    throw new ToolException(CMDERR, text);
                }
            }

            // Parse file objects provide via the DocumentationTool API
            parse(fileObjects, classTrees, true);

            etable.packages(packageNames)
                    .classTrees(classTrees.toList())
                    .scanSpecifiedItems();

            // abort, if errors were encountered during modules initialization
            if (log.hasErrors()) {
                return null;
            }

            // Parse the files in the packages and subpackages to be documented
            ListBuffer<JCCompilationUnit> allTrees = new ListBuffer<>();
            allTrees.addAll(classTrees);
            parse(etable.getFilesToParse(), allTrees, false);
            modules.newRound();
            modules.initModules(allTrees.toList());

            if (log.hasErrors()) {
                return null;
            }

            // Enter symbols for all files
            toolEnv.notice("main.Building_tree");
            javadocEnter.main(allTrees.toList());

            if (log.hasErrors()) {
                return null;
            }

            etable.setClassDeclList(listClasses(classTrees.toList()));

            dcfh.setHandler(dcfh.userCodeHandler);
            etable.analyze();

            // Ensure that package-info is read for all included packages
            for (Element e : etable.getIncludedElements()) {
                if (e.getKind() == ElementKind.PACKAGE) {
                    PackageSymbol p = (PackageSymbol) e;
                    if (p.package_info != null) {
                        p.package_info.complete();
                    }
                }
            }

        } catch (CompletionFailure cf) {
            throw new ToolException(ABNORMAL, cf.getMessage(), cf);
        } catch (Abort abort) {
            if (log.hasErrors()) {
                // presumably a message has been emitted, keep silent
                throw new ToolException(ABNORMAL, "", abort);
            } else {
                String text = log.getText("main.internal.error");
                Throwable t = abort.getCause() == null ? abort : abort.getCause();
                throw new ToolException(ABNORMAL, text, t);
            }
        }

        if (log.hasErrors())
            return null;

        toolEnv.docEnv = new DocEnvImpl(toolEnv, etable);
        return toolEnv.docEnv;
    }

    private boolean isRegularFile(String s) {
        try {
            return Files.isRegularFile(Paths.get(s));
        } catch (InvalidPathException e) {
            return false;
        }
    }

    /** Is the given string a valid package name? */
    boolean isValidPackageName(String s) {
        if (s.contains("/")) {
            String[] a = s.split("/");
            if (a.length == 2) {
                 return isValidPackageName0(a[0]) && isValidPackageName0(a[1]);
            }
            return false;
        }
        return isValidPackageName0(s);
    }

    private boolean isValidPackageName0(String s) {
        for (int index = s.indexOf('.') ; index != -1; index = s.indexOf('.')) {
            if (!isValidClassName(s.substring(0, index))) {
                return false;
            }
            s = s.substring(index + 1);
        }
        return isValidClassName(s);
    }

    private void parse(Iterable<? extends JavaFileObject> files, ListBuffer<JCCompilationUnit> trees,
                       boolean trace) {
        for (JavaFileObject fo: files) {
            if (uniquefiles.add(fo)) { // ignore duplicates
                if (trace)
                    toolEnv.notice("main.Loading_source_file", fo.getName());
                trees.append(parse(fo));
            }
        }
    }

    /** Are surrogates supported? */
    static final boolean surrogatesSupported = surrogatesSupported();
    private static boolean surrogatesSupported() {
        try {
            boolean b = Character.isHighSurrogate('a');
            return true;
        } catch (NoSuchMethodError ex) {
            return false;
        }
    }

    /**
     * Return true if given file name is a valid class name
     * (including "package-info").
     * @param s the name of the class to check.
     * @return true if given class name is a valid class name
     * and false otherwise.
     */
    public static boolean isValidClassName(String s) {
        if (s.length() < 1) return false;
        if (s.equals("package-info")) return true;
        if (surrogatesSupported) {
            int cp = s.codePointAt(0);
            if (!Character.isJavaIdentifierStart(cp))
                return false;
            for (int j = Character.charCount(cp); j < s.length(); j += Character.charCount(cp)) {
                cp = s.codePointAt(j);
                if (!Character.isJavaIdentifierPart(cp))
                    return false;
            }
        } else {
            if (!Character.isJavaIdentifierStart(s.charAt(0)))
                return false;
            for (int j = 1; j < s.length(); j++)
                if (!Character.isJavaIdentifierPart(s.charAt(j)))
                    return false;
        }
        return true;
    }

    /**
     * From a list of top level trees, return the list of contained class definitions
     */
    List<JCClassDecl> listClasses(List<JCCompilationUnit> trees) {
        List<JCClassDecl> result = new ArrayList<>();
        for (JCCompilationUnit t : trees) {
            for (JCTree def : t.defs) {
                if (def.hasTag(JCTree.Tag.CLASSDEF))
                    result.add((JCClassDecl)def);
            }
        }
        return result;
    }
}
