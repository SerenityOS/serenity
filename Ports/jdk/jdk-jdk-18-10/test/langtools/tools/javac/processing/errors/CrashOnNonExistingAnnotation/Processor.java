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

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.DiagnosticListener;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.util.Assert;

@SupportedAnnotationTypes("*")
@SupportedOptions("target")
public class Processor extends AbstractProcessor {

    private int round = 0;
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (round++ == 0) {
            try (Writer out = processingEnv.getFiler()
                                                 .createSourceFile("Anno.java")
                                                 .openWriter()) {
                String target = processingEnv.getOptions().get("target");
                String code = "import java.lang.annotation.ElementType;\n" +
                              "import java.lang.annotation.Target;\n" +
                              "@Target(ElementType." + target + ")\n" +
                              "@interface Anno { public String value(); }\n";
                out.write(code);
            } catch (IOException exc) {
                throw new IllegalStateException(exc);
            }
        }
        return true;
    }

    public static void main(String... args) throws IOException, URISyntaxException {
        if (args.length != 1) throw new IllegalStateException("Must provide class name!");
        String testContent = null;
        File testSrc = new File(System.getProperty("test.src"));
        File testFile = new File(testSrc, args[0]);
        if (!testFile.canRead()) throw new IllegalStateException("Cannot read the test source");
        JavacTool compiler = JavacTool.create();
        JavacFileManager fm = compiler.getStandardFileManager(null, null, null);
        testContent = fm.getJavaFileObject(testFile.toPath()).getCharContent(true).toString();
        JavaFileObject testFileObject = new TestFO(new URI("mem://" + args[0]), testContent);
        TestFM testFileManager = new TestFM(fm);
        JavacTask task = compiler.getTask(null,
                                          testFileManager,
                                          new DiagnosticCollector<JavaFileObject>(),
                                          null,
                                          null,
                                          Arrays.asList(testFileObject));
        final Trees trees = Trees.instance(task);
        final CompilationUnitTree cut = task.parse().iterator().next();

        final Map<int[], String> annotation2Target = new TreeMap<>(new Comparator<int[]>() {
            @Override public int compare(int[] o1, int[] o2) {
                return o2[0] - o1[0];
            }
        });

        new TreeScanner<Void, Void>() {
            @Override
            public Void visitAnnotation(AnnotationTree node, Void p) {
                int endPos = (int) trees.getSourcePositions().getEndPosition(cut, node);

                Assert.check(endPos >= 0);

                int startPos = (int) trees.getSourcePositions().getStartPosition(cut, node);
                String target = ((LiteralTree) node.getArguments().get(0)).getValue().toString();

                annotation2Target.put(new int[] {startPos, endPos}, target);

                return super.visitAnnotation(node, p);
            }
        }.scan(cut.getTypeDecls().get(0), null);

        DiagnosticListener<JavaFileObject> noErrors = new DiagnosticListener<JavaFileObject>() {
            @Override public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
                if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                    throw new IllegalStateException(diagnostic.toString());
                }
            }
        };

        for (Entry<int[], String> e : annotation2Target.entrySet()) {
            StringBuilder updatedContent = new StringBuilder();
            int last = testContent.length();

            for (int[] toRemove : annotation2Target.keySet()) {
                if (toRemove == e.getKey()) continue;
                updatedContent.insert(0, testContent.substring(toRemove[1], last));
                last = toRemove[0];
            }

            updatedContent.insert(0, testContent.substring(0, last));

            JavaFileObject updatedFile = new TestFO(new URI("mem://" + args[0]),
                                                    updatedContent.toString());
            JavacTask testTask = compiler.getTask(null,
                                                  testFileManager,
                                                  noErrors,
                                                  Arrays.asList("-processor", "Processor",
                                                                "-Atarget=" + e.getValue()),
                                                  null,
                                                  Arrays.asList(updatedFile));

            try {
                testTask.analyze();
            } catch (Throwable exc) {
                System.out.println("error while processing:");
                System.out.println(updatedContent);
                throw exc;
            }

            JavacTask testTask2 = compiler.getTask(null,
                                                   testFileManager,
                                                   new DiagnosticCollector<JavaFileObject>(),
                                                   null,
                                                   null,
                                                   Arrays.asList(updatedFile));

            try {
                testTask2.analyze();
            } catch (Throwable exc) {
                System.out.println("error while processing:");
                System.out.println(updatedContent);
                throw exc;
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
            return simpleName.equals("Source") && kind == Kind.SOURCE;
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
