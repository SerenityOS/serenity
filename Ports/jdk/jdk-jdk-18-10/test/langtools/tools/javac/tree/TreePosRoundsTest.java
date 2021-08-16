/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 6985205 6986246
 * @summary access to tree positions and doc comments may be lost across annotation processing rounds
 * @modules jdk.compiler
 * @build TreePosRoundsTest
 * @compile -proc:only -processor TreePosRoundsTest TreePosRoundsTest.java
 * @run main TreePosRoundsTest
 */

import java.io.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.tools.*;

import com.sun.source.tree.*;
import com.sun.source.util.*;
import javax.tools.JavaCompiler.CompilationTask;

// This test is an annotation processor that performs multiple rounds of
// processing, and on each round, it checks that source positions are
// available and correct.
//
// The test can be run directly as a processor from the javac command line
// or via JSR 199 by invoking the main program.

@SupportedAnnotationTypes("*")
public class TreePosRoundsTest extends AbstractProcessor {
    public static void main(String... args) throws Exception {
        String testSrc = System.getProperty("test.src");
        String testClasses = System.getProperty("test.classes");
        JavaCompiler c = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = c.getStandardFileManager(null, null, null)) {
            String thisName = TreePosRoundsTest.class.getName();
            File thisFile = new File(testSrc, thisName + ".java");
            Iterable<? extends JavaFileObject> files = fm.getJavaFileObjects(thisFile);
            List<String> options = Arrays.asList(
                    "-proc:only",
                    "-processor", thisName,
                    "-processorpath", testClasses);
            CompilationTask t = c.getTask(null, fm, null, options, null, files);
            boolean ok = t.call();
            if (!ok)
                throw new Exception("processing failed");
        }
    }

    Filer filer;
    Messager messager;
    Trees trees;

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public void init(ProcessingEnvironment pEnv) {
        super.init(pEnv);
        filer = pEnv.getFiler();
        messager = pEnv.getMessager();
        trees = Trees.instance(pEnv);
    }

    int round = 0;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        round++;

        // Scan trees for elements, verifying source tree positions
        for (Element e: roundEnv.getRootElements()) {
            try {
                TreePath p = trees.getPath(e);
                new TestTreeScanner(p.getCompilationUnit(), trees).scan(trees.getPath(e), null);
            } catch (IOException ex) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "Cannot get source: " + ex, e);
            }
        }

        final int MAXROUNDS = 3;
        if (round < MAXROUNDS)
            generateSource("Gen" + round);

        return true;
    }

    void generateSource(String name) {
        StringBuilder text = new StringBuilder();
        text.append("class ").append(name).append("{\n");
        text.append("    int one = 1;\n");
        text.append("    int two = 2;\n");
        text.append("    int three = one + two;\n");
        text.append("}\n");

        try {
            JavaFileObject fo = filer.createSourceFile(name);
            Writer out = fo.openWriter();
            try {
                out.write(text.toString());
            } finally {
                out.close();
            }
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    class TestTreeScanner extends TreePathScanner<Void,Void> {
        TestTreeScanner(CompilationUnitTree unit, Trees trees) throws IOException {
            this.unit = unit;
            JavaFileObject sf = unit.getSourceFile();
            source = sf.getCharContent(true).toString();
            sourcePositions = trees.getSourcePositions();
        }

        @Override
        public Void visitVariable(VariableTree tree, Void p) {
            check(getCurrentPath());
            return super.visitVariable(tree, p);
        }

        void check(TreePath tp) {
            Tree tree = tp.getLeaf();

            String expect = tree.toString();
            if (tree.getKind() == Tree.Kind.VARIABLE) {
                // tree.toString() does not know enough context to add ";",
                // so deal with that manually...
                Tree.Kind enclKind = tp.getParentPath().getLeaf().getKind();
                //System.err.println("  encl: " +enclKind);
                if (enclKind == Tree.Kind.CLASS || enclKind == Tree.Kind.BLOCK)
                    expect += ";";
                // t-w-r- adds implicit final: remove it
                if (enclKind == Tree.Kind.TRY && expect.startsWith("final "))
                    expect = expect.substring(6);
            }
            //System.err.println("expect: " + expect);

            int start = (int)sourcePositions.getStartPosition(unit, tree);
            if (start == Diagnostic.NOPOS) {
                messager.printMessage(Diagnostic.Kind.ERROR, "start pos not set for " + trim(tree));
                return;
            }

            int end = (int)sourcePositions.getEndPosition(unit, tree);
            if (end == Diagnostic.NOPOS) {
                messager.printMessage(Diagnostic.Kind.ERROR, "end pos not set for " + trim(tree));
                return;
            }

            String found = source.substring(start, end);
            //System.err.println(" found: " + found);

            // allow for long lines, in which case just compare beginning and
            // end of the strings
            boolean equal;
            if (found.contains("\n")) {
                String head = found.substring(0, found.indexOf("\n"));
                String tail = found.substring(found.lastIndexOf("\n")).trim();
                equal = expect.startsWith(head) && expect.endsWith(tail);
            } else {
                equal = expect.equals(found);
            }

            if (!equal) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "unexpected value found: '" + found + "'; expected: '" + expect + "'");
            }
        }

        String trim(Tree tree) {
            final int MAXLEN = 32;
            String s = tree.toString().replaceAll("\\s+", " ").trim();
            return (s.length() < MAXLEN) ? s : s.substring(0, MAXLEN);

        }

        CompilationUnitTree unit;
        SourcePositions sourcePositions;
        String source;
    }

}
