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

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.util.Assert;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

@SupportedAnnotationTypes("*")
public class Processor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        throw new IllegalStateException("Should not be invoked.");
    }

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
        DiagnosticListener<JavaFileObject> devNull = new DiagnosticListener<JavaFileObject>() {
            @Override public void report(Diagnostic<? extends JavaFileObject> diagnostic) { }
        };
        JavaFileObject testFile = new TestFO(new URI("mem://" + args[0]), testContent);
        JavacTask task = JavacTool.create().getTask(null,
                                                    new TestFM(fm),
                                                    devNull,
                                                    Arrays.asList("-Xjcov"),
                                                    null,
                                                    Arrays.asList(testFile));
        final Trees trees = Trees.instance(task);
        final CompilationUnitTree cut = task.parse().iterator().next();
        task.analyze();

        final List<int[]> annotations = new ArrayList<>();

        new TreeScanner<Void, Void>() {
            @Override
            public Void visitAnnotation(AnnotationTree node, Void p) {
                int endPos = (int) trees.getSourcePositions().getEndPosition(cut, node);

                Assert.check(endPos >= 0);

                annotations.add(new int[] {(int) trees.getSourcePositions().getStartPosition(cut, node), endPos});
                return super.visitAnnotation(node, p);
            }
        }.scan(cut.getTypeDecls().get(0), null);

        Collections.sort(annotations, new Comparator<int[]>() {
            @Override public int compare(int[] o1, int[] o2) {
                return o2[0] - o1[0];
            }
        });

        for (final int[] annotation : annotations) {
            StringBuilder updatedContent = new StringBuilder();
            int last = testContent.length();

            for (int[] toRemove : annotations) {
                if (toRemove == annotation) continue;
                updatedContent.insert(0, testContent.substring(toRemove[1], last));
                last = toRemove[0];
            }

            updatedContent.insert(0, testContent.substring(0, last));

            JavaFileObject updatedFile = new TestFO(new URI("mem://" + args[0]), updatedContent.toString());
            JavacTask testTask = JavacTool.create().getTask(null,
                                                            new TestFM(fm),
                                                            devNull,
                                                            Arrays.asList("-processor", "Processor"),
                                                            null,
                                                            Arrays.asList(updatedFile));

            try {
                testTask.analyze();
            } catch (Throwable e) {
                System.out.println("error while processing:");
                System.out.println(updatedContent);
                throw e;
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
