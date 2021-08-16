/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8226585 8250768
 * @summary Verify behavior w.r.t. preview feature API errors and warnings
 * @library /tools/lib /tools/javac/lib
 * @modules
 *      java.base/jdk.internal.javac
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.file
 *      jdk.compiler/com.sun.tools.javac.main
 *      jdk.compiler/com.sun.tools.javac.util
 *      jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox toolbox.JavacTask
 * @build combo.ComboTestHelper
 * @run main PreviewErrors
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import combo.ComboInstance;
import combo.ComboParameter;
import combo.ComboTask;
import combo.ComboTestHelper;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;

import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPoolException;
import java.io.FileWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.Map.Entry;
import javax.tools.JavaFileObject;

import toolbox.JavacTask;
import toolbox.ToolBox;

public class PreviewErrors extends ComboInstance<PreviewErrors> {

    protected ToolBox tb;

    PreviewErrors() {
        super();
        tb = new ToolBox();
    }

    private static String previewAPI(PreviewElementType elementType) {
        return """
               package preview.api;
               public class ${name} {
                   @jdk.internal.javac.PreviewFeature(feature=jdk.internal.javac.PreviewFeature.Feature.${preview}
                                                ${reflective})
                   public static void test() { }
                   @jdk.internal.javac.PreviewFeature(feature=jdk.internal.javac.PreviewFeature.Feature.${preview}
                                                ${reflective})
                   public static class Clazz {}
               }
               """.replace("${name}", elementType.className)
                  .replace("${preview}", "TEST")
                  .replace("${reflective}", elementType.reflective);
    }

    private static final String SEALED_DECLARATION = """
                                                     package user;
                                                     public sealed interface R {}
                                                     final class C implements R {}
                                                     """;

    static Map<PreviewElementType, Map<Preview, Map<Suppress, Map<Lint, StringBuilder>>>> parts = new TreeMap<>();
    public static void main(String... args) throws Exception {
        new ComboTestHelper<PreviewErrors>()
                .withDimension("PREVIEW", (x, preview) -> x.preview = preview, Preview.values())
                .withDimension("LINT", (x, lint) -> x.lint = lint, Lint.values())
                .withDimension("SUPPRESS", (x, suppress) -> x.suppress = suppress, Suppress.values())
                .withDimension("ELEMENT_TYPE", (x, elementType) -> x.elementType = elementType, PreviewElementType.values())
                .run(PreviewErrors::new);
        if (args.length == 1) {
            try (Writer out = new FileWriter(args[0])) {
                int petCount = 0;
                for (Entry<PreviewElementType, Map<Preview, Map<Suppress, Map<Lint, StringBuilder>>>> pet : parts.entrySet()) {
                    petCount++;
                    switch (pet.getKey()) {
                        case API_REFLECTIVE_CLASS, API_CLASS -> {
                            if (pet.getKey() == PreviewElementType.API_REFLECTIVE_CLASS) {
                                out.write("<h2>" + petCount + ". Reflective Preview API</h2>\n");
                            } else {
                                out.write("<h2>" + petCount + ". Preview API</h2>\n");
                            }
                            out.write("API source (part of java.base):\n");
                            out.write("<pre>\n");
                            String previewAPI = previewAPI(pet.getKey());
                            out.write(previewAPI);
                            out.write("\n</pre>\n");
                        }
                        case REFER_TO_DECLARATION_CLASS -> {
                            out.write("<h2>" + petCount + ". Using an element declared using a preview feature</h2>\n");
                            out.write("Element declaration:\n");
                            out.write("<pre>\n");
                            out.write(SEALED_DECLARATION);
                            out.write("\n</pre>\n");
                        }
                        case LANGUAGE -> {
                            out.write("<h2>" + petCount + ". Using preview language feature</h2>\n");
                        }
                    }
                    int prevCount = 0;
                    for (Entry<Preview, Map<Suppress, Map<Lint, StringBuilder>>> prev : pet.getValue().entrySet()) {
                        prevCount++;
                        switch (prev.getKey()) {
                            case YES -> {
                                out.write("<h3>" + petCount + "." + prevCount + ". With --enable-preview</h3>\n");
                            }
                            case NO -> {
                                out.write("<h3>" + petCount + "." + prevCount + ". Without --enable-preview</h3>\n");
                            }
                        }
                        int supCount = 0;
                        for (Entry<Suppress, Map<Lint, StringBuilder>> sup : prev.getValue().entrySet()) {
                            supCount++;
                            switch (sup.getKey()) {
                                case YES -> {
                                    out.write("<h4>" + petCount + "." + prevCount + "." + supCount + ". Usages suppressed with @SuppressWarnings</h4>\n");
                                }
                                case NO -> {
                                    out.write("<h4>" + petCount + "." + prevCount + "." + supCount + ". Usages not suppressed with @SuppressWarnings</h4>\n");
                                }
                            }
                            int lintCount = 0;
                            for (Entry<Lint, StringBuilder> lint : sup.getValue().entrySet()) {
                                lintCount++;
                                switch (lint.getKey()) {
                                    case NONE -> {
                                        out.write("<h5>" + petCount + "." + prevCount + "." + supCount + "." + lintCount + ". Neither -Xlint:preview nor -Xlint:-preview</h5>\n");
                                    }
                                    case ENABLE_PREVIEW -> {
                                        out.write("<h5>" + petCount + "." + prevCount + "." + supCount + "." + lintCount + ". With -Xlint:preview</h5>\n");
                                    }
                                    case DISABLE_PREVIEW -> {
                                        out.write("<h5>" + petCount + "." + prevCount + "." + supCount + "." + lintCount + ". With -Xlint:-preview</h5>\n");
                                    }
                                }
                                out.write(lint.getValue().toString());
                                out.write("\n");
                            }
                        }
                    }
                }
            }
        }
    }

    private Preview preview;
    private Lint lint;
    private Suppress suppress;
    private PreviewElementType elementType;

    @Override
    public void doWork() throws IOException {
        Path base = Paths.get(".");
        tb.cleanDirectory(base);
        Path src = base.resolve("src");
        Path srcJavaBase = src.resolve("java.base");
        Path classes = base.resolve("classes");
        Path classesJavaBase = classes.resolve("java.base");

        Files.createDirectories(classesJavaBase);

        String previewAPI = previewAPI(elementType);

        ComboTask task = newCompilationTask()
                .withOption("-XDrawDiagnostics")
                .withOption("-source")
                .withOption(String.valueOf(Runtime.version().feature()));

        switch (elementType) {
            case API_CLASS, API_REFLECTIVE_CLASS -> {
                tb.writeJavaFiles(srcJavaBase, previewAPI);

                new JavacTask(tb)
                        .outdir(classesJavaBase)
                        .options("--patch-module", "java.base=" + srcJavaBase.toString())
                        .files(tb.findJavaFiles(srcJavaBase))
                        .run()
                        .writeAll();

                task.withOption("--patch-module")
                    .withOption("java.base=" + classesJavaBase.toString())
                    .withOption("--add-exports")
                    .withOption("java.base/preview.api=ALL-UNNAMED");
            }
            case API_SOURCE, API_REFLECTIVE_SOURCE -> {
                tb.writeJavaFiles(srcJavaBase, previewAPI);

                task.withOption("--patch-module")
                    .withOption("java.base=" + srcJavaBase.toString())
                    .withOption("--add-exports")
                    .withOption("java.base/preview.api=ALL-UNNAMED");
            }
            case LANGUAGE -> {
                task.withOption("-XDforcePreview=true");
            }
            case REFER_TO_DECLARATION_CLASS -> {
                tb.writeJavaFiles(srcJavaBase, SEALED_DECLARATION);

                new JavacTask(tb)
                        .outdir(classesJavaBase)
                        .options("--patch-module", "java.base=" + srcJavaBase.toString())
                        .files(tb.findJavaFiles(srcJavaBase))
                        .run()
                        .writeAll();

                task.withOption("--patch-module")
                    .withOption("java.base=" + classesJavaBase.toString())
                    .withOption("--add-exports")
                    .withOption("java.base/user=ALL-UNNAMED")
                    .withOption("-XDforcePreview=true");
            }
            case REFER_TO_DECLARATION_SOURCE -> {
                tb.writeJavaFiles(srcJavaBase, SEALED_DECLARATION);

                task.withOption("--patch-module")
                    .withOption("java.base=" + srcJavaBase.toString())
                    .withOption("--add-exports")
                    .withOption("java.base/user=ALL-UNNAMED")
                    .withOption("-XDforcePreview=true");
            }
        }

        task.withSourceFromTemplate("""
                                    package test;
                                    public class Test {
                                        #{SUPPRESS}
                                        public void test(Object o) {
                                            #{ELEMENT_TYPE}
                                        }
                                    }
                                    """);

        if (preview.expand(null)!= null) {
            task.withOption(preview.expand(null));
        }

        if (lint.expand(null) != null) {
            task.withOption(lint.expand(null));
        }

        task.generate(result -> {
                Set<String> actual = Arrays.stream(Diagnostic.Kind.values())
                                            .flatMap(kind -> result.diagnosticsForKind(kind).stream())
                                            .map(d -> d.getLineNumber() + ":" + d.getColumnNumber() + ":" + d.getCode())
                                            .collect(Collectors.toSet());
                boolean ok;
                boolean previewClass = true;
                Set<String> expected = null;
                if (preview == Preview.NO) {
                    switch (elementType) {
                        case LANGUAGE -> {
                            ok = false;
                            expected = Set.of("5:41:compiler.err.preview.feature.disabled");
                        }
                        case REFER_TO_DECLARATION_CLASS -> {
                            ok = true;
                            previewClass = false;
                            expected = Set.of();
                        }
                        case REFER_TO_DECLARATION_SOURCE -> {
                            ok = false;
                            previewClass = false;
                            expected = Set.of("2:8:compiler.err.preview.feature.disabled.plural");
                        }
                        case API_CLASS, API_SOURCE -> {
                            ok = false;
                            expected = Set.of("6:17:compiler.err.is.preview",
                                              "5:25:compiler.err.is.preview");
                        }
                        case API_REFLECTIVE_CLASS, API_REFLECTIVE_SOURCE -> {
                            ok = true;
                            previewClass = false;
                            if (suppress == Suppress.YES) {
                                expected = Set.of();
                            } else if (lint == Lint.NONE || lint == Lint.ENABLE_PREVIEW) {
                                expected = Set.of("6:20:compiler.warn.is.preview.reflective",
                                                  "5:28:compiler.warn.is.preview.reflective");
                            } else {//-Xlint:-preview
                                expected = Set.of("-1:-1:compiler.note.preview.filename",
                                                  "-1:-1:compiler.note.preview.recompile");
                            }
                        }
                        default -> {
                            throw new IllegalStateException(elementType.name());
                        }
                    }
                } else {
                    ok = true;
                    switch (elementType) {
                        case LANGUAGE -> {
                            if (lint == Lint.ENABLE_PREVIEW) {
                                expected = Set.of("5:41:compiler.warn.preview.feature.use");
                            } else {
                                expected = Set.of("-1:-1:compiler.note.preview.filename",
                                                  "-1:-1:compiler.note.preview.recompile");
                            }
                        }
                        case REFER_TO_DECLARATION_CLASS -> {
                            previewClass = false;
                            expected = Set.of();
                        }
                        case REFER_TO_DECLARATION_SOURCE -> {
                            previewClass = false;
                            if (lint == Lint.ENABLE_PREVIEW) {
                                expected = Set.of("2:8:compiler.warn.preview.feature.use.plural");
                            } else {
                                expected = Set.of("-1:-1:compiler.note.preview.filename",
                                                  "-1:-1:compiler.note.preview.recompile");
                            }
                        }
                        case API_CLASS, API_SOURCE -> {
                            if (suppress == Suppress.YES) {
                                expected = Set.of();
                            } else if (lint == Lint.ENABLE_PREVIEW) {
                                expected = Set.of("6:17:compiler.warn.is.preview",
                                                  "5:25:compiler.warn.is.preview");
                            } else {
                                expected = Set.of("-1:-1:compiler.note.preview.filename",
                                                  "-1:-1:compiler.note.preview.recompile");
                            }
                        }
                        case API_REFLECTIVE_CLASS, API_REFLECTIVE_SOURCE -> {
                            previewClass = false;
                            if (suppress == Suppress.YES) {
                                expected = Set.of();
                            } else if (lint == Lint.ENABLE_PREVIEW) {
                                expected = Set.of("6:20:compiler.warn.is.preview.reflective",
                                                  "5:28:compiler.warn.is.preview.reflective");
                            } else {
                                expected = Set.of("-1:-1:compiler.note.preview.filename",
                                                  "-1:-1:compiler.note.preview.recompile");
                            }
                        }
                    }
                }
                if (!elementType.isSource) {
                    try {
                        parts.computeIfAbsent(elementType, x -> new TreeMap<>())
                                .computeIfAbsent(preview, x -> new TreeMap<>())
                                .computeIfAbsent(suppress, x -> new TreeMap<>())
                                .computeIfAbsent(lint, x -> new StringBuilder())
                                .append("<pre>\n")
                                .append(task.getSources().head.getCharContent(false))
                                .append("\n</pre>\n")
                                .append("<pre>\n")
                                .append(Arrays.stream(Diagnostic.Kind.values())
                                              .flatMap(kind -> result.diagnosticsForKind(kind).reverse().stream())
                                              .filter(d -> d.getSource() == null || !d.getSource().getName().contains("R.java"))
                                              .map(d -> (d.getSource() != null ? d.getSource().getName() + ":" + d.getLineNumber() + ":" : "") + d.getKind()+ ": " + d.getMessage(null)).collect(Collectors.joining("\n")))
                                .append("\n</pre>\n")
                                .append(ok ? previewClass ? "Test.class is marked as a preview class file." : "Test.class is <b>NOT</b> marked as a preview class file." : "Does not compile.");
                    } catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                }
                if (ok) {
                    if (!result.get().iterator().hasNext()) {
                        throw new IllegalStateException("Did not succeed as expected for preview=" + preview + ", lint=" + lint + ", suppress=" + suppress + ", elementType=" + elementType + ": actual:\"" + actual + "\"");
                    }
                    ClassFile cf;
                    try {
                        JavaFileObject testClass = null;
                        for (JavaFileObject classfile : result.get()) {
                            if (classfile.isNameCompatible("Test", JavaFileObject.Kind.CLASS)){
                                testClass = classfile;
                            }
                        }
                        if (testClass == null) {
                            throw new IllegalStateException("Cannot find Test.class");
                        }
                        cf = ClassFile.read(testClass.openInputStream());
                    } catch (IOException | ConstantPoolException ex) {
                        throw new IllegalStateException(ex);
                    }
                    if (previewClass && cf.minor_version != 65535) {
                        throw new IllegalStateException("Expected preview class, but got: " + cf.minor_version);
                    } else if (!previewClass && cf.minor_version != 0) {
                        throw new IllegalStateException("Expected minor version == 0 but got: " + cf.minor_version);
                    }
                } else {
                    if (result.get().iterator().hasNext()) {
                        throw new IllegalStateException("Succeed unexpectedly for preview=" + preview + ", lint=" + lint + ", suppress=" + suppress + ", elementType=" + elementType);
                    }
                }
                if (expected != null && !expected.equals(actual)) {
                    throw new IllegalStateException("Unexpected output for preview=" + preview + ", lint=" + lint + ", suppress=" + suppress + ", elementType=" + elementType + ": actual: \"" + actual + "\", expected: \"" + expected + "\"");
                }
            });
    }

    public enum Preview implements ComboParameter {
        YES("--enable-preview"),
        NO(null);

        private final String opt;

        private Preview(String opt) {
            this.opt = opt;
        }

        public String expand(String optParameter) {
            return opt;
        }
    }

    public enum Lint implements ComboParameter {
        NONE(null),
        ENABLE_PREVIEW("-Xlint:preview"),
        DISABLE_PREVIEW("-Xlint:-preview");

        private final String opt;

        private Lint(String opt) {
            this.opt = opt;
        }

        public String expand(String optParameter) {
            return opt;
        }
    }

    public enum Suppress implements ComboParameter {
        YES("@SuppressWarnings(\"preview\")"),
        NO("");

        private final String code;

        private Suppress(String code) {
            this.code = code;
        }

        public String expand(String optParameter) {
            return code;
        }
    }

    public enum PreviewElementType implements ComboParameter {
        LANGUAGE("boolean b = o instanceof String s;", ", reflective=false", "", false),
        REFER_TO_DECLARATION_SOURCE("user.R d1; user.R d2;", ", reflective=false", "", true),
        REFER_TO_DECLARATION_CLASS("user.R d1; user.R d2;", ", reflective=false", "", false),
        API_CLASS("""
                  preview.api.Core.test();
                  preview.api.Core.Clazz c;
                  """,
                  ", reflective=false",
                  "Core",
                false),
        API_REFLECTIVE_CLASS("""
                  preview.api.Reflect.test();
                  preview.api.Reflect.Clazz c;
                  """,
                  ", reflective=true",
                  "Reflect",
                false),
        API_SOURCE("""
                   preview.api.Core.test();
                   preview.api.Core.Clazz c;
                   """,
                   ", reflective=false",
                   "Core",
                   true),
        API_REFLECTIVE_SOURCE("""
                   preview.api.Reflect.test();
                   preview.api.Reflect.Clazz c;
                   """,
                   ", reflective=true",
                   "Reflect",
                   true);

        String code;
        String reflective;
        String className;
        boolean isSource;

        private PreviewElementType(String code, String reflective, String className, boolean isSource) {
            this.code = code;
            this.reflective = reflective;
            this.className = className;
            this.isSource = isSource;
        }

        public String expand(String optParameter) {
            return code;
        }
    }
}
