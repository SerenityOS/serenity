/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8142968 8154956 8170987 8171412
 * @summary Test --add-modules and --limit-modules; also test the "enabled" modules.
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.code
 *      jdk.compiler/com.sun.tools.javac.main
 *      jdk.compiler/com.sun.tools.javac.model
 *      jdk.compiler/com.sun.tools.javac.processing
 *      jdk.compiler/com.sun.tools.javac.util
 *      jdk.jdeps/com.sun.tools.javap
 * @build toolbox.ToolBox toolbox.JarTask toolbox.JavacTask toolbox.JavaTask ModuleTestBase
 * @run main AddLimitMods
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.TypeElement;

import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;

import toolbox.JarTask;
import toolbox.JavacTask;
import toolbox.JavaTask;
import toolbox.Task;

public class AddLimitMods extends ModuleTestBase {

    public static void main(String... args) throws Exception {
        AddLimitMods t = new AddLimitMods();
        t.runTests();
    }

    @Test
    public void testManual(Path base) throws Exception {
        Path moduleSrc = base.resolve("module-src");
        Path m1 = moduleSrc.resolve("m1x");

        tb.writeJavaFiles(m1,
                          "module m1x { requires m2x; requires m3x; }");

        Path m2 = moduleSrc.resolve("m2x");

        tb.writeJavaFiles(m2,
                          "module m2x { requires m3x; exports m2x; }",
                          "package m2x; public class M2 {}");

        Path m3 = moduleSrc.resolve("m3x");

        tb.writeJavaFiles(m3,
                          "module m3x { exports m3x; }",
                          "package m3x; public class M3 {}");

        Path modulePath = base.resolve("module-path");

        Files.createDirectories(modulePath);

        new JavacTask(tb)
                .options("--module-source-path", moduleSrc.toString())
                .outdir(modulePath)
                .files(findJavaFiles(m3))
                .run()
                .writeAll();

        new JavacTask(tb)
                .options("--module-source-path", moduleSrc.toString())
                .outdir(modulePath)
                .files(findJavaFiles(m2))
                .run()
                .writeAll();

        //real test
        new JavacTask(tb)
                .options("--module-path", modulePath.toString(),
                         "--should-stop=ifNoError=FLOW",
                         "--limit-modules", "java.base")
                .outdir(modulePath)
                .files(findJavaFiles(m1))
                .run(Task.Expect.FAIL)
                .writeAll();

        new JavacTask(tb)
                .options("--module-path", modulePath.toString(),
                         "--should-stop=ifNoError=FLOW",
                         "--limit-modules", "java.base",
                         "--add-modules", "m2x")
                .outdir(modulePath)
                .files(findJavaFiles(m1))
                .run(Task.Expect.FAIL)
                .writeAll();

        new JavacTask(tb)
                .options("--module-path", modulePath.toString(),
                         "--should-stop=ifNoError=FLOW",
                         "--limit-modules", "java.base",
                         "--add-modules", "m2x,m3x")
                .outdir(modulePath)
                .files(findJavaFiles(m1))
                .run()
                .writeAll();

        new JavacTask(tb)
                .options("--module-path", modulePath.toString(),
                         "--should-stop=ifNoError=FLOW",
                         "--limit-modules", "m2x")
                .outdir(modulePath)
                .files(findJavaFiles(m1))
                .run()
                .writeAll();

        new JavacTask(tb)
                .options("--module-path", modulePath.toString(),
                         "--should-stop=ifNoError=FLOW",
                         "--limit-modules", "m3x")
                .outdir(modulePath)
                .files(findJavaFiles(m1))
                .run(Task.Expect.FAIL)
                .writeAll();

        new JavacTask(tb)
                .options("--module-path", modulePath.toString(),
                         "--should-stop=ifNoError=FLOW",
                         "--limit-modules", "m3x",
                         "--add-modules", "m2x")
                .outdir(modulePath)
                .files(findJavaFiles(m1))
                .run()
                .writeAll();
    }

    @Test
    public void testAllModulePath(Path base) throws Exception {
        if (Files.isDirectory(base))
            tb.cleanDirectory(base);

        Path moduleSrc = base.resolve("module-src");
        Path m1 = moduleSrc.resolve("m1x");

        tb.writeJavaFiles(m1,
                          "module m1x { exports api; }",
                          "package api; public class Api { }");

        Path modulePath = base.resolve("module-path");

        Files.createDirectories(modulePath);

        new JavacTask(tb)
                .options("--module-source-path", moduleSrc.toString())
                .outdir(modulePath)
                .files(findJavaFiles(moduleSrc))
                .run()
                .writeAll();

        Path cpSrc = base.resolve("cp-src");
        tb.writeJavaFiles(cpSrc, "package test; public class Test { api.Api api; }");

        Path cpOut = base.resolve("cp-out");

        Files.createDirectories(cpOut);

        new JavacTask(tb)
                .options("--module-path", modulePath.toString())
                .outdir(cpOut)
                .files(findJavaFiles(cpSrc))
                .run(Task.Expect.FAIL)
                .writeAll();

        new JavacTask(tb)
                .options("--module-path", modulePath.toString(),
                         "--add-modules", "ALL-MODULE-PATH")
                .outdir(cpOut)
                .files(findJavaFiles(cpSrc))
                .run()
                .writeAll();

        List<String> actual;
        List<String> expected = Arrays.asList(
                "- compiler.err.addmods.all.module.path.invalid",
                "1 error");

        actual = new JavacTask(tb)
                   .options("--module-source-path", moduleSrc.toString(),
                            "-XDrawDiagnostics",
                            "--add-modules", "ALL-MODULE-PATH")
                   .outdir(modulePath)
                   .files(findJavaFiles(moduleSrc))
                   .run(Task.Expect.FAIL)
                   .writeAll()
                   .getOutputLines(Task.OutputKind.DIRECT);

        if (!Objects.equals(actual, expected)) {
            throw new IllegalStateException("incorrect errors; actual=" + actual + "; expected=" + expected);
        }

        actual = new JavacTask(tb)
                   .options("--patch-module", "java.base=" + cpSrc.toString(),
                            "-XDrawDiagnostics",
                            "--add-modules", "ALL-MODULE-PATH")
                   .outdir(cpOut)
                   .files(findJavaFiles(cpSrc))
                   .run(Task.Expect.FAIL)
                   .writeAll()
                   .getOutputLines(Task.OutputKind.DIRECT);

        if (!Objects.equals(actual, expected)) {
            throw new IllegalStateException("incorrect errors; actual=" + actual + "; expected=" + expected);
        }

        actual = new JavacTask(tb, Task.Mode.CMDLINE)
                   .options("-source", "8", "-target", "8",
                            "-XDrawDiagnostics",
                            "--add-modules", "ALL-MODULE-PATH")
                   .outdir(cpOut)
                   .files(findJavaFiles(cpSrc))
                   .run(Task.Expect.FAIL)
                   .writeAll()
                   .getOutputLines(Task.OutputKind.DIRECT);

        if (!actual.contains("- compiler.err.option.not.allowed.with.target: --add-modules, 8")) {
            throw new IllegalStateException("incorrect errors; actual=" + actual);
        }

        tb.writeJavaFiles(cpSrc, "module m1x {}");

        actual = new JavacTask(tb)
                   .options("-XDrawDiagnostics",
                            "--add-modules", "ALL-MODULE-PATH")
                   .outdir(cpOut)
                   .files(findJavaFiles(cpSrc))
                   .run(Task.Expect.FAIL)
                   .writeAll()
                   .getOutputLines(Task.OutputKind.DIRECT);

        if (!Objects.equals(actual, expected)) {
            throw new IllegalStateException("incorrect errors; actual=" + actual + "; expected=" + expected);
        }
    }

    @Test
    public void testRuntime2Compile(Path base) throws Exception {
        Path classpathSrc = base.resolve("classpath-src");
        Path classpathOut = base.resolve("classpath-out");

        tb.writeJavaFiles(classpathSrc,
                          generateCheckAccessibleClass("cp.CP"));

        Files.createDirectories(classpathOut);

        System.err.println("Compiling classpath-src files:");
        new JavacTask(tb)
                .outdir(classpathOut)
                .files(findJavaFiles(classpathSrc))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        Path automaticSrc = base.resolve("automatic-src");
        Path automaticOut = base.resolve("automatic-out");

        tb.writeJavaFiles(automaticSrc,
                          generateCheckAccessibleClass("automatic.Automatic"));

        Files.createDirectories(automaticOut);

        System.err.println("Compiling automatic-src files:");
        new JavacTask(tb)
                .outdir(automaticOut)
                .files(findJavaFiles(automaticSrc))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        Path modulePath = base.resolve("module-path");

        Files.createDirectories(modulePath);

        Path automaticJar = modulePath.resolve("automatic.jar");

        System.err.println("Creating automatic.jar:");
        new JarTask(tb, automaticJar)
          .baseDir(automaticOut)
          .files("automatic/Automatic.class")
          .run();

        Path moduleSrc = base.resolve("module-src");
        Path m1 = moduleSrc.resolve("m1x");

        tb.writeJavaFiles(m1,
                          "module m1x { exports api; }",
                          "package api; public class Api { public void test() { } }");

        System.err.println("Compiling module-src files:");
        new JavacTask(tb)
                .options("--module-source-path", moduleSrc.toString())
                .outdir(modulePath)
                .files(findJavaFiles(moduleSrc))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        int index = 0;

        for (String moduleInfo : MODULE_INFO_VARIANTS) {
            for (String[] options : OPTIONS_VARIANTS) {
                index++;

                System.err.println("Running check: " + moduleInfo + "; " + Arrays.asList(options));

                Path m2Runtime = base.resolve(index + "-runtime").resolve("m2x");
                Path out = base.resolve(index + "-runtime").resolve("out").resolve("m2x");

                Files.createDirectories(out);

                StringBuilder testClassNamed = new StringBuilder();

                testClassNamed.append("""
                        package test;
                        public class Test {
                            public static void main(String... args) throws Exception {
                        """);

                for (Entry<String, String> e : MODULES_TO_CHECK_TO_SAMPLE_CLASS.entrySet()) {
                    testClassNamed.append("        System.err.println(\"visible:" + e.getKey() + ":\" + ModuleLayer.boot().findModule(\"" + e.getKey() + "\").isPresent());\n");
                }

                testClassNamed.append("""
                                Class<?> cp = Class.forName(Test.class.getClassLoader().getUnnamedModule(), "cp.CP");
                                cp.getDeclaredMethod("runMe").invoke(null);
                                Class<?> automatic = Class.forName(ModuleLayer.boot().findModule("automatic").get(), "automatic.Automatic");
                                automatic.getDeclaredMethod("runMe").invoke(null);
                            }
                        }
                        """);

                tb.writeJavaFiles(m2Runtime, moduleInfo, testClassNamed.toString());

                System.err.println("Compiling " + m2Runtime + " files:");
                new JavacTask(tb)
                   .options("--module-path", modulePath.toString())
                   .outdir(out)
                   .files(findJavaFiles(m2Runtime))
                   .run()
                   .writeAll();

                boolean success;
                String output;

                try {
                    System.err.println("Running m2x/test.Test:");
                    output = new JavaTask(tb)
                       .includeStandardOptions(false)
                       .vmOptions(augmentOptions(options,
                                                 Collections.emptyList(),
                                                 "--module-path", modulePath.toString() + File.pathSeparator + out.getParent().toString(),
                                                 "--class-path", classpathOut.toString(),
                                                 "--add-reads", "m2x=ALL-UNNAMED,automatic",
                                                 "-m", "m2x/test.Test"))
                       .run()
                       .writeAll()
                       .getOutput(Task.OutputKind.STDERR);

                    success = true;
                } catch (Task.TaskError err) {
                    success = false;
                    output = "";
                }

                Path m2 = base.resolve(String.valueOf(index)).resolve("m2x");

                tb.writeJavaFiles(m2,
                                  moduleInfo,
                                  """
                                      package test;
                                      public class Test {}
                                      """);

                List<String> auxOptions = success ? Arrays.asList(
                    "--processor-path", System.getProperty("test.class.path"),
                    "-processor", CheckVisibleModule.class.getName(),
                    "-Aoutput=" + output,
                    "-XDaccessInternalAPI=true"
                ) : Collections.emptyList();

                System.err.println("Compiling/processing m2x files:");
                new JavacTask(tb)
                   .options(augmentOptions(options,
                                           auxOptions,
                                           "--module-path", modulePath.toString(),
                                           "--class-path", classpathOut.toString(),
                                           "--should-stop=ifNoError=FLOW"))
                   .outdir(modulePath)
                   .files(findJavaFiles(m2))
                   .run(success ? Task.Expect.SUCCESS : Task.Expect.FAIL)
                   .writeAll();
            }
        }
    }

    private String generateCheckAccessibleClass(String fqn) {
        String packageName = fqn.substring(0, fqn.lastIndexOf('.'));
        String simpleName = fqn.substring(fqn.lastIndexOf('.') + 1);
        StringBuilder checkClassesAccessible = new StringBuilder();
        checkClassesAccessible.append("package " + packageName + ";" +
                                      "public class " + simpleName + " {" +
                                      "    public static void runMe() throws Exception {");
        for (Entry<String, String> e : MODULES_TO_CHECK_TO_SAMPLE_CLASS.entrySet()) {
            checkClassesAccessible
                    .append("try {")
                    .append("Class.forName(\"" + e.getValue() + "\").newInstance();")
                    .append("System.err.println(\"" + fqn + ":" + e.getKey() + ":true\");")
                    .append("} catch (Exception ex) {")
                    .append("System.err.println(\"" + fqn + ":" + e.getKey() + ":false\");")
                    .append("}");
        }

        checkClassesAccessible.append("    }\n" +
                                      "}");

        return checkClassesAccessible.toString();
    }

    private static final Map<String, String> MODULES_TO_CHECK_TO_SAMPLE_CLASS = new LinkedHashMap<>();

    static {
        MODULES_TO_CHECK_TO_SAMPLE_CLASS.put("m1x", "api.Api");
        MODULES_TO_CHECK_TO_SAMPLE_CLASS.put("m2x", "test.Test");
        MODULES_TO_CHECK_TO_SAMPLE_CLASS.put("java.base", "java.lang.Object");
    };

    @SupportedAnnotationTypes("*")
    @SupportedOptions("output")
    public static final class CheckVisibleModule extends AbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            String expected = processingEnv.getOptions().get("output");
            Set<String> expectedElements = new HashSet<>(Arrays.asList(expected.split(System.getProperty("line.separator"))));
            Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
            Symtab syms = Symtab.instance(context);

            for (Entry<String, String> e : MODULES_TO_CHECK_TO_SAMPLE_CLASS.entrySet()) {
                String module = e.getKey();
                ModuleElement mod = processingEnv.getElementUtils().getModuleElement(module);
                String visible = "visible:" + module + ":" + (mod != null);

                if (!expectedElements.contains(visible)) {
                    throw new AssertionError("actual: " + visible + "; expected: " + expected);
                }

                JavacElements javacElements = JavacElements.instance(context);
                ClassSymbol unnamedClass = javacElements.getTypeElement(syms.unnamedModule, e.getValue());
                String unnamed = "cp.CP:" + module + ":" + (unnamedClass != null);

                if (!expectedElements.contains(unnamed)) {
                    throw new AssertionError("actual: " + unnamed + "; expected: " + expected);
                }

                ModuleElement automaticMod = processingEnv.getElementUtils().getModuleElement("automatic");
                ClassSymbol automaticClass = javacElements.getTypeElement(automaticMod, e.getValue());
                String automatic = "automatic.Automatic:" + module + ":" + (automaticClass != null);

                if (!expectedElements.contains(automatic)) {
                    throw new AssertionError("actual: " + automatic + "; expected: " + expected);
                }
            }

            return false;
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latest();
        }

    }

    public String[] augmentOptions(String[] options, List<String> auxOptions, String... baseOptions) {
        List<String> all = new ArrayList<>();

        all.addAll(Arrays.asList(options));
        all.addAll(Arrays.asList(baseOptions));
        all.addAll(auxOptions);

        return all.toArray(new String[0]);
    }

    private static final String[] MODULE_INFO_VARIANTS = {
        "module m2x { exports test; }",
        "module m2x { requires m1x; exports test; }"
    };

    private static final String[][] OPTIONS_VARIANTS = {
        {"--add-modules", "automatic"},
        {"--add-modules", "m1x,automatic"},
        {"--add-modules", "jdk.compiler,automatic"},
        {"--add-modules", "m1x,jdk.compiler,automatic"},
        {"--add-modules", "ALL-SYSTEM,automatic"},
        {"--limit-modules", "java.base", "--add-modules", "automatic"},
        {"--limit-modules", "java.base", "--add-modules", "ALL-SYSTEM,automatic"},
        {"--limit-modules", "m2x", "--add-modules", "automatic"},
        {"--limit-modules", "jdk.compiler", "--add-modules", "automatic"},
    };
}
