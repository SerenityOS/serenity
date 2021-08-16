/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8029161 8029376
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 */

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTool;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

public class VerifyErroneousAnnotationsAttributed {
    public static void main(String... args) throws IOException, URISyntaxException {
        new VerifyErroneousAnnotationsAttributed().run();
    }

    void run() throws IOException {
        try {
            int failCount = 0;
            for (String ann : generateAnnotations()) {
                String code = PATTERN.replace("PLACEHOLDER", ann);
                try {
                    validate(code);
                } catch (Throwable t) {
                    System.out.println("Failed for: ");
                    System.out.println(code);
                    t.printStackTrace(System.out);
                    failCount++;
                }
            }

            if (failCount > 0) {
                throw new IllegalStateException("failed sub-tests: " + failCount);
            }
        } finally {
            fm.close();
        }
    }

    List<String> generateAnnotations() {
        List<String> result = new ArrayList<>();

        result.addAll(Kind.ANNOTATION.generateValue(2, true));
        result.addAll(Kind.ANNOTATION.generateValue(2, false));

        return result;
    }

    enum Kind {
        INT("i", "ValueInt") {
            @Override public List<String> generateValue(int depth, boolean valid) {
                if (valid) {
                    return Arrays.asList("INT_VALUE");
                } else {
                    return Arrays.asList("BROKEN_INT_VALUE");
                }
            }
        },
        ANNOTATION("a", "ValueAnnotation") {
            @Override public List<String> generateValue(int depth, boolean valid) {
                String ad = "@Annotation" + depth + (valid ? "" : "Unknown");

                if (depth <= 0) {
                    return Arrays.asList(ad);
                }

                List<String> result = new ArrayList<>();
                final Kind[][] generateKindCombinations = new Kind[][] {
                    new Kind[] {Kind.INT},
                    new Kind[] {Kind.ANNOTATION},
                    new Kind[] {Kind.INT, Kind.ANNOTATION}
                };

                for (boolean generateAssignment : new boolean[] {false, true}) {
                    for (boolean generateValid : new boolean[] {false, true}) {
                        for (Kind[] generateKinds : generateKindCombinations) {
                            if (generateKinds.length > 1 && generateValid && !generateAssignment) {
                                //skip: the same code is generated for generateValid == false.
                                continue;
                            }
                            List<String> attributes = generateAttributes(generateAssignment,
                                    generateValid, depth, generateKinds);
                            String annotation;
                            if (generateAssignment) {
                                annotation = ad;
                            } else {
                                annotation = ad + generateKinds[0].annotationWithValueSuffix;
                            }
                            for (String attr : attributes) {
                                result.add(annotation + "(" + attr + ")");
                            }
                        }
                    }
                }

                return result;
            }

            List<String> generateAttributes(boolean generateAssignment, boolean valid, int depth,
                                            Kind... kinds) {
                List<List<String>> combinations = new ArrayList<>();

                for (boolean subValid : new boolean[] {false, true}) {
                    for (Kind k : kinds) {
                        String prefix;

                        if (generateAssignment) {
                            if (valid) {
                                prefix = k.validAttributeName + "=";
                            } else {
                                prefix = "invalid" + k.validAttributeName + "=";
                            }
                        } else {
                            prefix = "";
                        }

                        List<String> combination = new ArrayList<>();

                        combinations.add(combination);

                        for (String val : k.generateValue(depth - 1, subValid)) {
                            combination.add(prefix + val);
                        }
                    }
                }

                List<String> result = new ArrayList<>();

                combine(combinations, new StringBuilder(), result);

                return result;
            }

            void combine(List<List<String>> combinations, StringBuilder current, List<String> to) {
                if (combinations.isEmpty()) {
                    to.add(current.toString());
                    return ;
                }

                int currLen = current.length();

                for (String str : combinations.get(0)) {
                    if (current.length() > 0) current.append(", ");
                    current.append(str);

                    combine(combinations.subList(1, combinations.size()), current, to);

                    current.delete(currLen, current.length());
                }
            }
        };
        String validAttributeName;
        String annotationWithValueSuffix;

        private Kind(String validAttributeName, String annotationWithValueSuffix) {
            this.validAttributeName = validAttributeName;
            this.annotationWithValueSuffix = annotationWithValueSuffix;
        }

        public abstract List<String> generateValue(int depth, boolean valid);

    }

    private static final String PATTERN =
            "public class Test {\n" +
            "    public static final int INT_VALUE = 1;\n" +
            "    @interface Annotation0 {}\n" +
            "    @interface Annotation1 {int i() default 0; Annotation0 a() default @Annotation0; }\n" +
            "    @interface Annotation2 {int i() default 0; Annotation1 a() default @Annotation1; }\n" +
            "    @interface Annotation1ValueInt {int value() default 0; }\n" +
            "    @interface Annotation2ValueInt {int value() default 0; }\n" +
            "    @interface Annotation1ValueAnnotation {Annotation0 a() default @Annotation0; }\n" +
            "    @interface Annotation2ValueAnnotation {Annotation1 a() default @Annotation1; }\n" +
            "    PLACEHOLDER\n" +
            "    private void test() { }\n" +
            "}";

    static final class TestCase {
        final String code;
        final boolean valid;

        public TestCase(String code, boolean valid) {
            this.code = code;
            this.valid = valid;
        }

    }

    final JavacTool tool = JavacTool.create();
    final JavaFileManager fm = tool.getStandardFileManager(null, null, null);
    final DiagnosticListener<JavaFileObject> devNull = new DiagnosticListener<JavaFileObject>() {
        @Override public void report(Diagnostic<? extends JavaFileObject> diagnostic) {}
    };

    void validate(String code) throws IOException, URISyntaxException {
        JavacTask task = tool.getTask(null,
                                      fm,
                                      devNull,
                                      Arrays.asList("--should-stop=at=FLOW"),
                                      null,
                                      Arrays.asList(new MyFileObject(code)));

        final Trees trees = Trees.instance(task);
        final CompilationUnitTree cut = task.parse().iterator().next();
        task.analyze();

        //ensure all the annotation attributes are annotated meaningfully
        //all the attributes in the test file should contain either an identifier
        //or a select, so only checking those for a reasonable Element/Symbol.
        new TreePathScanner<Void, Void>() {
            @Override
            public Void visitIdentifier(IdentifierTree node, Void p) {
                verifyAttributedMeaningfully();
                return super.visitIdentifier(node, p);
            }
            @Override
            public Void visitMemberSelect(MemberSelectTree node, Void p) {
                verifyAttributedMeaningfully();
                return super.visitMemberSelect(node, p);
            }
            private void verifyAttributedMeaningfully() {
                Element el = trees.getElement(getCurrentPath());

                if (el == null || el.getKind() == ElementKind.OTHER ||
                        el.asType().getKind() == TypeKind.OTHER) {
                    throw new IllegalStateException("Not attributed properly: " +
                            getCurrentPath().getParentPath().getLeaf());
                }
            }
        }.scan(cut, null);
    }
    static class MyFileObject extends SimpleJavaFileObject {
        private final String text;
        public MyFileObject(String text) {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            this.text = text;
        }
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return text;
        }
    }
}
