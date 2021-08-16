/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

/**@test
 * @bug 8035890
 * @summary Verify that the parser correctly checks for source level 8 on the new places where
 *          annotations can appear in 8.
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 * @run main CheckErrorsForSource7 CheckErrorsForSource7.java
 */
import java.io.File;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.file.JavacFileManager;

/**For each place where an annotation can syntactically appear with -source 8, but not with
 * -source 7, this test verifies that an error is correctly emitted from the parser for
 * the annotation for -source 7. This test first gathers the occurrences of @TA from
 * the CheckErrorsForSource7Data class below, and then repeatedly removes all these annotations
 * except one and checks the parser reports an expected error. This is needed as as the parser
 * typically produces only one 'insufficient source level' error for each new feature used.
 */
public class CheckErrorsForSource7 {
    public static void main(String... args) throws IOException, URISyntaxException {
        new CheckErrorsForSource7().run(args);
    }

    private void run(String... args) throws IOException, URISyntaxException {
        //the first and only parameter must be the name of the file to be analyzed:
        if (args.length != 1) throw new IllegalStateException("Must provide source file!");
        File testSrc = new File(System.getProperty("test.src"));
        File testFile = new File(testSrc, args[0]);
        if (!testFile.canRead()) throw new IllegalStateException("Cannot read the test source");
        try (JavacFileManager fm = JavacTool.create().getStandardFileManager(null, null, null)) {

            //gather spans of the @TA annotations into typeAnnotationSpans:
            JavacTask task = JavacTool.create().getTask(null,
                                                        fm,
                                                        null,
                                                        Collections.<String>emptyList(),
                                                        null,
                                                        fm.getJavaFileObjects(testFile));
            final Trees trees = Trees.instance(task);
            final CompilationUnitTree cut = task.parse().iterator().next();
            final List<int[]> typeAnnotationSpans = new ArrayList<>();

            new TreePathScanner<Void, Void>() {
                @Override
                public Void visitAnnotation(AnnotationTree node, Void p) {
                    if (node.getAnnotationType().getKind() == Kind.IDENTIFIER &&
                        ((IdentifierTree) node.getAnnotationType()).getName().contentEquals("TA")) {
                        int start = (int) trees.getSourcePositions().getStartPosition(cut, node);
                        int end = (int) trees.getSourcePositions().getEndPosition(cut, node);
                        typeAnnotationSpans.add(new int[] {start, end});
                    }
                    return null;
                }
            }.scan(cut, null);

            //sort the spans in the reverse order, to simplify removing them from the source:
            Collections.sort(typeAnnotationSpans, new Comparator<int[]>() {
                @Override
                public int compare(int[] o1, int[] o2) {
                    return o2[0] - o1[0];
                }
            });

            //verify the errors are produce correctly:
            String originalSource = cut.getSourceFile().getCharContent(false).toString();

            for (int[] toKeep : typeAnnotationSpans) {
                //prepare updated source code by removing all the annotations except the toKeep one:
                String updated = originalSource;

                for (int[] span : typeAnnotationSpans) {
                    if (span == toKeep) continue;

                    updated = updated.substring(0, span[0]) + updated.substring(span[1]);
                }

                //parse and verify:
                JavaFileObject updatedFile = new TestFO(cut.getSourceFile().toUri(), updated);
                DiagnosticCollector<JavaFileObject> errors = new DiagnosticCollector<>();
                JavacTask task2 = JavacTool.create().getTask(null,
                                                             fm,
                                                             errors,
                                                             Arrays.asList("-source", "7"),
                                                             null,
                                                             Arrays.asList(updatedFile));
                task2.parse();

                boolean found = false;

                for (Diagnostic<? extends JavaFileObject> d : errors.getDiagnostics()) {
                    if (d.getKind() == Diagnostic.Kind.ERROR && EXPECTED_ERROR.equals(d.getCode())) {
                        if (found) {
                            throw new IllegalStateException("More than one expected error found.");
                        }
                        found = true;
                    }
                }

                if (!found)
                    throw new IllegalStateException("Did not produce proper errors for: " + updated);
            }
        }
    }

    static final String EXPECTED_ERROR = "compiler.err.feature.not.supported.in.source.plural";

    class TestFO extends SimpleJavaFileObject {
        private final String content;
        public TestFO(URI uri, String content) {
            super(uri, Kind.SOURCE);
            this.content = content;
        }

        @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return content;
        }

        @Override public boolean isNameCompatible(String simpleName, Kind kind) {
            return true;
        }
    }
}

//data on which the source level check is verified:
class CheckErrorsForSource7Data {
    @Target(ElementType.TYPE_USE)
    @interface TA { }

    Object n1 = new @TA ArrayList<@TA String>();
    Object n2 = new @TA Object() {};
    Object [] @TA [] arr @TA[];
    <T> @TA int @TA[] ret(Object obj) @TA[] throws @TA Exception {
        this.<@TA String>ret(null);
        Object c1 = new @TA String[1];

        int val = obj instanceof @TA String ? ((@TA String) obj).length() : 0;
        List<@TA ?> l;
        return null;
    }
    void vararg(String @TA ... args) { }

    abstract class C<@TA T extends @TA Number & @TA Runnable>
               extends @TA ArrayList<@TA String>
               implements java.util. @TA Comparator<@TA T> { }

    interface I extends java.util. @TA Comparator<@TA String> { }

}
