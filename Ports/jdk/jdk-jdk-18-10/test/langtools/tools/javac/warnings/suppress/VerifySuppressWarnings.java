/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

/**Takes a source file, parses it once to get the warnings inside the file and
 * then for each and every declaration in the file, it tries to place
 * the @SuppressWarnings annotation on the declaration and verifies than no
 * warnings are produced inside the declaration, but all are produced outside it.
 *
 * Currently only works with <code>unchecked,deprecation,cast,divzero</code> warnings.
 */
public class VerifySuppressWarnings {

    private static final List<String> STANDARD_PARAMS =
            Arrays.asList("-Xlint:unchecked,deprecation,cast,divzero");

    public static void main(String... args) throws IOException, URISyntaxException {
        if (args.length != 1) throw new IllegalStateException("Must provide class name!");
        String testContent = null;
        List<File> sourcePath = new ArrayList<>();
        for (String sourcePaths : System.getProperty("test.src.path").split(File.pathSeparator)) {
            sourcePath.add(new File(sourcePaths));
        }
        JavacFileManager fm = JavacTool.create().getStandardFileManager(null, null, null);
        for (File sp : sourcePath) {
            File inp = new File(sp, args[0]);

            if (inp.canRead()) {
                testContent = fm.getJavaFileObject(inp.toPath()).getCharContent(true).toString();
            }
        }
        if (testContent == null) throw new IllegalStateException();
        final List<Diagnostic<?>> diagnostics = new ArrayList<>();
        DiagnosticListener<JavaFileObject> collectDiagnostics = new DiagnosticListener<JavaFileObject>() {
            @Override public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
                diagnostics.add(diagnostic);
            }
        };
        JavaFileObject testFile = new TestFO(new URI("mem://" + args[0]), testContent);
        JavacTask task = JavacTool.create().getTask(null,
                                                    new TestFM(fm),
                                                    collectDiagnostics,
                                                    STANDARD_PARAMS,
                                                    null,
                                                    Arrays.asList(testFile));
        final Trees trees = Trees.instance(task);
        final CompilationUnitTree cut = task.parse().iterator().next();
        task.analyze();

        final List<int[]> declarationSpans = new ArrayList<>();

        new TreeScanner<Void, Void>() {
            @Override public Void visitClass(ClassTree node, Void p) {
                handleDeclaration(node);
                return super.visitClass(node, p);
            }
            @Override public Void visitMethod(MethodTree node, Void p) {
                handleDeclaration(node);
                return super.visitMethod(node, p);
            }
            @Override public Void visitVariable(VariableTree node, Void p) {
                handleDeclaration(node);
                return super.visitVariable(node, p);
            }

            @Override
            public Void visitNewClass(NewClassTree node, Void p) {
                if (node.getClassBody() != null) {
                    scan(node.getClassBody().getMembers(), null);
                }
                return null;
            }

            private void handleDeclaration(Tree node) {
                int endPos = (int) trees.getSourcePositions().getEndPosition(cut, node);

                if (endPos == (-1)) {
                    if (node.getKind() == Tree.Kind.METHOD && (((JCMethodDecl) node).getModifiers().flags & Flags.GENERATEDCONSTR) != 0) {
                        return ;
                    }
                    throw new IllegalStateException();
                }

                declarationSpans.add(new int[] {(int) trees.getSourcePositions().getStartPosition(cut, node), endPos});
            }
        }.scan(cut, null);

        for (final int[] declarationSpan : declarationSpans) {
            final String suppressWarnings =
                    "@SuppressWarnings({\"deprecation\", \"unchecked\", \"serial\", \"divzero\"})";
            final String updatedContent = testContent.substring(0, declarationSpan[0]) + suppressWarnings + testContent.substring(declarationSpan[0]);
            final List<Diagnostic<?>> foundErrors = new ArrayList<>(diagnostics);
            DiagnosticListener<JavaFileObject> verifyDiagnostics = new DiagnosticListener<JavaFileObject>() {
                @Override public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
                    long adjustedPos = diagnostic.getPosition();

                    if (adjustedPos >= declarationSpan[0]) adjustedPos -= suppressWarnings.length();

                    if (declarationSpan[0] <= adjustedPos && adjustedPos <= declarationSpan[1]) {
                        throw new IllegalStateException("unsuppressed: " + diagnostic.getMessage(null));
                    }

                    boolean found = false;

                    for (Iterator<Diagnostic<?>> it = foundErrors.iterator(); it.hasNext();) {
                        Diagnostic<?> d = it.next();
                        if (d.getPosition() == adjustedPos && d.getCode().equals(diagnostic.getCode())) {
                            it.remove();
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        throw new IllegalStateException("diagnostic not originally reported: " + diagnostic.getMessage(null));
                    }
                }
            };

            JavaFileObject updatedFile = new TestFO(new URI("mem://" + args[0]), updatedContent);
            JavacTask testTask = JavacTool.create().getTask(null,
                                                            new TestFM(fm),
                                                            verifyDiagnostics,
                                                            STANDARD_PARAMS,
                                                            null,
                                                            Arrays.asList(updatedFile));

            testTask.analyze();

            for (Diagnostic<?> d : foundErrors) {
                if (d.getPosition() < declarationSpan[0] || declarationSpan[1] < d.getPosition()) {
                    throw new IllegalStateException("missing: " + d.getMessage(null));
                }
            }
        }
    }

    private static final class TestFO extends SimpleJavaFileObject {
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

    private static final class TestFM extends ForwardingJavaFileManager<JavaFileManager> {

        public TestFM(JavaFileManager fileManager) {
            super(fileManager);
        }

        @Override
        public boolean isSameFile(FileObject a, FileObject b) {
            return a.equals(b);
        }

    }
}
