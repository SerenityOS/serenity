/*
 * Copyright (c) 2009, 2014, Oracle and/or its affiliates. All rights reserved.
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

package genstubs;

import java.io.*;
import java.util.*;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCImport;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.Pretty;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import javax.tools.JavaFileManager;

/**
 * Generate stub source files by removing implementation details from input files.
 *
 * This is a special purpose stub generator, specific to the needs of generating
 * stub files for JDK 7 API that are needed to compile langtools files that depend
 * on that API. The stub generator works by removing as much of the API source code
 * as possible without affecting the public signature, in order to reduce the
 * transitive closure of the API being referenced. The resulting stubs can be
 * put on the langtools sourcepath with -implicit:none to compile the langtools
 * files that depend on the JDK 7 API.
 *
 * Usage:
 *  genstubs -s <outdir> -sourcepath <path> <classnames>
 *
 * The specified class names are looked up on the sourcepath, and corresponding
 * stubs are written to the source output directory.
 *
 * Classes are parsed into javac ASTs, then processed with a javac TreeTranslator
 * to remove implementation details, and written out in the source output directory.
 * Documentation comments and annotations are removed. Method bodies are removed
 * and methods are marked native. Private and package-private field definitions
 * have their initializers replace with 0, 0.0, false, null as appropriate.
 */

public class GenStubs {
    static class Fault extends Exception {
        private static final long serialVersionUID = 0;
        Fault(String message) {
            super(message);
        }
        Fault(String message, Throwable cause) {
            super(message);
            initCause(cause);
        }
    }

    public static void main(String[] args) {
        boolean ok = new GenStubs().run(args);
        if (!ok)
            System.exit(1);
    }

    public boolean run(String... args) {
        File outdir = null;
        String sourcepath = null;
        List<String> classes = new ArrayList<String>();
        for (ListIterator<String> iter = Arrays.asList(args).listIterator(); iter.hasNext(); ) {
            String arg = iter.next();
            if (arg.equals("-s") && iter.hasNext())
                outdir = new File(iter.next());
            else if (arg.equals("-sourcepath") && iter.hasNext())
                sourcepath = iter.next();
            else if (arg.startsWith("-"))
                throw new IllegalArgumentException(arg);
            else {
                classes.add(arg);
                while (iter.hasNext())
                    classes.add(iter.next());
            }
        }

        return run(sourcepath, outdir, classes);
    }

    public boolean run(String sourcepath, File outdir, List<String> classes) {
        //System.err.println("run: sourcepath:" + sourcepath + " outdir:" + outdir + " classes:" + classes);
        if (sourcepath == null)
            throw new IllegalArgumentException("sourcepath not set");
        if (outdir == null)
            throw new IllegalArgumentException("source output dir not set");

        JavacTool tool = JavacTool.create();
        StandardJavaFileManager fm = tool.getStandardFileManager(null, null, null);

        try {
            fm.setLocation(StandardLocation.SOURCE_OUTPUT, Collections.singleton(outdir));
            fm.setLocation(StandardLocation.SOURCE_PATH, splitPath(sourcepath));
            List<JavaFileObject> files = new ArrayList<JavaFileObject>();
            for (String c: classes) {
                JavaFileObject fo = fm.getJavaFileForInput(
                        StandardLocation.SOURCE_PATH, c, JavaFileObject.Kind.SOURCE);
                if (fo == null)
                    error("class not found: " + c);
                else
                    files.add(fo);
            }

            JavacTask t = tool.getTask(null, fm, null, null, null, files);
            Iterable<? extends CompilationUnitTree> trees = t.parse();
            for (CompilationUnitTree tree: trees) {
                makeStub(fm, tree);
            }
        } catch (IOException e) {
            error("IO error " + e, e);
        }

        return (errors == 0);
    }

    void makeStub(StandardJavaFileManager fm, CompilationUnitTree tree) throws IOException {
        CompilationUnitTree tree2 = new StubMaker().translate(tree);
        CompilationUnitTree tree3 = new ImportCleaner(fm).removeRedundantImports(tree2);

        String className = fm.inferBinaryName(StandardLocation.SOURCE_PATH, tree.getSourceFile());
        JavaFileObject fo = fm.getJavaFileForOutput(StandardLocation.SOURCE_OUTPUT,
                className, JavaFileObject.Kind.SOURCE, null);
        // System.err.println("Writing " + className + " to " + fo.getName());
        Writer out = fo.openWriter();
        try {
            new Pretty(out, true).printExpr((JCTree) tree3);
        } finally {
            out.close();
        }
    }

    List<File> splitPath(String path) {
        List<File> list = new ArrayList<File>();
        for (String p: path.split(File.pathSeparator)) {
            if (p.length() > 0)
                list.add(new File(p));
        }
        return list;
    }

    void error(String message) {
        System.err.println(message);
        errors++;
    }

    void error(String message, Throwable cause) {
        error(message);
    }

    int errors;

    class StubMaker extends TreeTranslator {
        CompilationUnitTree translate(CompilationUnitTree tree) {
            return super.translate((JCCompilationUnit) tree);
        }

        /**
         * compilation units: remove javadoc comments
         * -- required, in order to remove @deprecated tags, since we
         * (separately) remove all annotations, including @Deprecated
         */
        public void visitTopLevel(JCCompilationUnit tree) {
            super.visitTopLevel(tree);
            tree.docComments = null;
        }

        /**
         * methods: remove method bodies, make methods native
         */
        @Override
        public void visitClassDef(JCClassDecl tree) {
            long prevClassMods = currClassMods;
            currClassMods = tree.mods.flags;
            try {
                super.visitClassDef(tree);;
            } finally {
                currClassMods = prevClassMods;
            }
        }
        private long currClassMods = 0;

        /**
         * methods: remove method bodies, make methods native
         */
        @Override
        public void visitMethodDef(JCMethodDecl tree) {
            tree.mods = translate(tree.mods);
            tree.restype = translate(tree.restype);
            tree.typarams = translateTypeParams(tree.typarams);
            tree.params = translateVarDefs(tree.params);
            tree.thrown = translate(tree.thrown);
            if (tree.body != null) {
                if ((currClassMods & Flags.INTERFACE) != 0) {
                    tree.mods.flags &= ~(Flags.DEFAULT | Flags.STATIC);
                } else {
                    tree.mods.flags |= Flags.NATIVE;
                }
                tree.body = null;
            }
            result = tree;
        }

        /**
         * modifiers: remove annotations
         */
        @Override
        public void visitModifiers(JCModifiers tree) {
            tree.annotations = com.sun.tools.javac.util.List.nil();
            result = tree;
        }

        /**
         * field definitions: replace initializers with 0, 0.0, false etc
         * when possible -- i.e. leave public, protected initializers alone
         */
        @Override
        public void visitVarDef(JCVariableDecl tree) {
            tree.mods = translate(tree.mods);
            tree.vartype = translate(tree.vartype);
            if (tree.init != null) {
                if ((tree.mods.flags & (Flags.PUBLIC | Flags.PROTECTED)) != 0)
                    tree.init = translate(tree.init);
                else {
                    String t = tree.vartype.toString();
                    if (t.equals("boolean"))
                        tree.init = new JCLiteral(TypeTag.BOOLEAN, 0) { };
                    else if (t.equals("byte"))
                        tree.init = new JCLiteral(TypeTag.BYTE, 0) { };
                    else if (t.equals("char"))
                        tree.init = new JCLiteral(TypeTag.CHAR, 0) { };
                    else if (t.equals("double"))
                        tree.init = new JCLiteral(TypeTag.DOUBLE, 0.d) { };
                    else if (t.equals("float"))
                        tree.init = new JCLiteral(TypeTag.FLOAT, 0.f) { };
                    else if (t.equals("int"))
                        tree.init = new JCLiteral(TypeTag.INT, 0) { };
                    else if (t.equals("long"))
                        tree.init = new JCLiteral(TypeTag.LONG, 0) { };
                    else if (t.equals("short"))
                        tree.init = new JCLiteral(TypeTag.SHORT, 0) { };
                    else
                        tree.init = new JCLiteral(TypeTag.BOT, null) { };
                }
            }
            result = tree;
        }
    }

    class ImportCleaner extends TreeScanner {
        private Set<Name> names = new HashSet<Name>();
        private TreeMaker m;

        ImportCleaner(JavaFileManager fm) {
            // ImportCleaner itself doesn't require a filemanager, but instantiating
            // a TreeMaker does, indirectly (via ClassReader, sigh)
            Context c = new Context();
            c.put(JavaFileManager.class, fm);
            m = TreeMaker.instance(c);
        }

        CompilationUnitTree removeRedundantImports(CompilationUnitTree t) {
            JCCompilationUnit tree = (JCCompilationUnit) t;
            tree.accept(this);
            ListBuffer<JCTree> defs = new ListBuffer<JCTree>();
            for (JCTree def: tree.defs) {
                if (def.getTag() == JCTree.Tag.IMPORT) {
                    JCImport imp = (JCImport) def;
                    if (imp.qualid.getTag() == JCTree.Tag.SELECT) {
                        JCFieldAccess qualid = (JCFieldAccess) imp.qualid;
                        if (!qualid.name.toString().equals("*")
                                && !names.contains(qualid.name)) {
                            continue;
                        }
                    }
                }
                defs.add(def);
            }
            tree.defs = tree.defs.intersect(defs.toList());
            return tree;
        }

        @Override
        public void visitImport(JCImport tree) { } // ignore names found in imports

        @Override
        public void visitIdent(JCIdent tree) {
            names.add(tree.name);
        }

        @Override
        public void visitSelect(JCFieldAccess tree) {
            super.visitSelect(tree);
            names.add(tree.name);
        }
    }
}
