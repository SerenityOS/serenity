/*
 * Copyright (c) 2006, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import java.io.*;
import java.util.*;
import javax.lang.model.util.*;
import javax.tools.*;
import com.sun.tools.javac.api.*;
import com.sun.source.tree.*;
import com.sun.source.util.*;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.Position;

/*
 * Abstract class to help check the scopes in a parsed source file.
 * -- parse source file
 * -- scan trees looking for string literals
 * -- check the scope at that point against the string, using
 *      boolean check(Scope s, String ref)
 */
abstract class Checker {
    // parse the source file and call check(scope, string) for each string literal found
    void check(String... fileNames) throws IOException {
        File testSrc = new File(System.getProperty("test.src"));

        DiagnosticListener<JavaFileObject> dl = new DiagnosticListener<JavaFileObject>() {
            public void report(Diagnostic d) {
                System.err.println(d);
                if (d.getKind() == Diagnostic.Kind.ERROR)
                    errors = true;
                new Exception().printStackTrace();
            }
        };

        JavacTool tool = JavacTool.create();
        try (StandardJavaFileManager fm = tool.getStandardFileManager(dl, null, null)) {
            Iterable<? extends JavaFileObject> files =
                fm.getJavaFileObjectsFromFiles(getFiles(testSrc, fileNames));
            task = tool.getTask(null, fm, dl, null, null, files);
            Iterable<? extends CompilationUnitTree> units = task.parse();

            if (errors)
                throw new AssertionError("errors occurred creating trees");

            ScopeScanner s = new ScopeScanner();
            for (CompilationUnitTree unit: units) {
                TreePath p = new TreePath(unit);
                s.scan(p, getTrees());
                additionalChecks(getTrees(), unit);
            }
            task = null;

            if (errors)
                throw new AssertionError("errors occurred checking scopes");
        }
    }

    // default impl: split ref at ";" and call checkLocal(scope, ref_segment) on scope and its enclosing scopes
    protected boolean check(Scope s, String ref) {
        // System.err.println("check scope: " + s);
        // System.err.println("check ref: " + ref);
        if (s == null && (ref == null || ref.trim().length() == 0))
            return true;

        if (s == null) {
            error(s, ref, "scope missing");
            return false;
        }

        if (ref == null) {
            error(s, ref, "scope unexpected");
            return false;
        }

        String local;
        String encl;
        int semi = ref.indexOf(';');
        if (semi == -1) {
            local = ref;
            encl = null;
        } else {
            local = ref.substring(0, semi);
            encl = ref.substring(semi + 1);
        }

        return checkLocal(s, local.trim())
            & check(s.getEnclosingScope(), encl);
    }

    // override if using default check(Scope,String)
    boolean checkLocal(Scope s, String ref) {
        throw new IllegalStateException();
    }

    void additionalChecks(Trees trees, CompilationUnitTree topLevel) throws IOException {
    }

    void error(Scope s, String ref, String msg) {
        System.err.println("Error: " + msg);
        System.err.println("Scope: " + (s == null ? null : asList(s.getLocalElements())));
        System.err.println("Expect: " + ref);
        System.err.println("javac: " + (s == null ? null : ((JavacScope) s).getEnv()));
        errors = true;
    }

    protected Elements getElements() {
        return task.getElements();
    }

    protected Trees getTrees() {
        return Trees.instance(task);
    }

    boolean errors = false;
    protected JavacTask task;

    // scan a parse tree, and for every string literal found, call check(scope, string) with
    // the string value at the scope at that point
    class ScopeScanner extends TreePathScanner<Boolean,Trees> {
        public Boolean  visitLiteral(LiteralTree tree, Trees trees) {
            TreePath path = getCurrentPath();
            CompilationUnitTree unit = path.getCompilationUnit();
            Position.LineMap lineMap = ((JCCompilationUnit)unit).lineMap;
//          long line = lineMap.getLineNumber(((JCTree)tree).pos/*trees.getSourcePositions().getStartPosition(tree)*/);
//          System.err.println(line + ": " + abbrev(tree));
            Scope s = trees.getScope(path);
            if (tree.getKind() == Tree.Kind.STRING_LITERAL)
                check(s, tree.getValue().toString().trim());
            return null;
        }

        private String abbrev(Tree tree) {
            int max = 48;
            String s = tree.toString().replaceAll("[ \n]+", " ");
            return (s.length() < max ? s : s.substring(0, max-3) + "...");
        }
    }

    // prefix filenames with a directory
    static Iterable<File> getFiles(File dir, String... names) {
        List<File> files = new ArrayList<File>(names.length);
        for (String name: names)
            files.add(new File(dir, name));
        return files;
    }

    static private <T> List<T> asList(Iterable<T> iter) {
        List<T> l = new ArrayList<T>();
        for (T t: iter)
            l.add(t);
        return l;
    }
}
