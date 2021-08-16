/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Printer;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.CapturedType;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.util.Log;

import static javax.tools.StandardLocation.SOURCE_PATH;

public class LocalVariableInferenceTester {

    static final StandardJavaFileManager fm;

    static {
        final JavaCompiler comp = ToolProvider.getSystemJavaCompiler();
        fm = comp.getStandardFileManager(null, null, null);
        File destDir = new File(System.getProperty("user.dir"));
        try {
            fm.setLocation(javax.tools.StandardLocation.CLASS_OUTPUT, Arrays.asList(destDir));
        } catch (IOException ex) {
            throw new AssertionError(ex);
        }
    }

    public static void main(String[] args) throws IOException {
        try {
            if (args.length != 1) {
                System.err.println("Usage: LocalVariableInferenceTester <sourcefile>");
                System.exit(1);
            }
            File path = new File(System.getProperty("test.src"));
            fm.setLocation(SOURCE_PATH, Arrays.asList(path));
            File input = new File(path, args[0]);
            JavaFileObject jfo = fm.getJavaFileObjects(input).iterator().next();
            new LocalVariableInferenceTester().compileAndCheck(jfo);
        } finally {
            fm.close();
        }
    }

    int errors = 0;
    int checks = 0;

    void compileAndCheck(JavaFileObject input) throws IOException {
        JavaCompiler c = ToolProvider.getSystemJavaCompiler();
        JavacTask task = (JavacTask) c.getTask(null, fm, null, Arrays.asList("-g"), null, Arrays.asList(input));
        JavacTrees trees = JavacTrees.instance(task);
        Types types = Types.instance(((JavacTaskImpl)task).getContext());
        Iterable<? extends CompilationUnitTree> roots = task.parse();
        Log log = Log.instance(((JavacTaskImpl)task).getContext());
        //force code generation (to shake out non-denotable issues)
        boolean hasClasses = task.generate().iterator().hasNext();
        if (!hasClasses) {
            throw new AssertionError("Errors occurred during compilation!");
        }
        errors += log.nerrors;
        new LocalVarTypeChecker(trees, types).scan(roots, null);
        System.err.println("Checks executed: " + checks);
        if (errors != 0) {
            throw new AssertionError("Errors were found");
        }
    }

    void error(Tree node, String msg) {
        System.err.println(node);
        System.err.println("ERROR: " + msg);
        errors++;
    }

    class LocalVarTypeChecker extends TreePathScanner<Void, Void> {

        JavacTrees trees;
        Types types;

        LocalVarTypeChecker(JavacTrees trees, Types types) {
            this.trees = trees;
            this.types = types;
        }

        @Override
        public Void visitVariable(VariableTree node, Void aVoid) {
            Element e = trees.getElement(getCurrentPath());
            if (e.getKind() == ElementKind.LOCAL_VARIABLE) {
                TypeMirror type = e.asType();
                InferredType inferredAnno = e.getAnnotation(InferredType.class);
                if (inferredAnno != null) {
                    checks++;
                    String req = inferredAnno.value();
                    String found = new TypePrinter().visit((Type)type, null);
                    if (!req.equals(found)) {
                        error(node, "Inferred type mismatch; expected: " + req + " - found: " + found);
                    }
                }
            }
            return super.visitVariable(node, null);
        }

        class TypePrinter extends Printer {

            Map<Type, Integer> capturedIdMap = new HashMap<>();

            @Override
            protected String localize(Locale locale, String key, Object... args) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String visitCapturedType(CapturedType t, Locale locale) {
                return "CAP#" + capturedVarId(t, locale);
            }

            @Override
            protected String capturedVarId(CapturedType t, Locale locale) {
                return String.valueOf(capturedIdMap.getOrDefault(t, capturedIdMap.size()));
            }

            @Override
            public String visitClassType(ClassType t, Locale locale) {
                if (!t.isCompound() && t.tsym.name.isEmpty()) {
                    return "#ANON(" + types.directSupertypes(t) + ")";
                } else if (t.isCompound()) {
                    return "#INT(" + types.directSupertypes(t) + ")";
                } else {
                    return super.visitClassType(t, locale);
                }
            }
        }
    }
}
