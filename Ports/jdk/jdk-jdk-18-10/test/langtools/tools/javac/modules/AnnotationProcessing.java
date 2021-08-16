/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8133884 8162711 8133896 8172158 8172262 8173636 8175119 8189747 8236842 8254023 8263432
 * @summary Verify that annotation processing works.
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask ModuleTestBase
 * @run main AnnotationProcessing
 */

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.FilerException;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.ModuleElement.ProvidesDirective;
import javax.lang.model.element.ModuleElement.UsesDirective;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.ElementScanner14;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.Task.Mode;
import toolbox.Task.OutputKind;

public class AnnotationProcessing extends ModuleTestBase {

    public static void main(String... args) throws Exception {
        System.out.println(System.getProperties());
        new AnnotationProcessing().runTests();
    }

    @Test
    public void testAPSingleModule(Path base) throws Exception {
        Path moduleSrc = base.resolve("module-src");
        Path m1 = moduleSrc.resolve("m1x");

        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        tb.writeJavaFiles(m1,
                          "module m1x { }",
                          "package impl; public class Impl { }");

        String log = new JavacTask(tb)
                .options("--module-source-path", moduleSrc.toString(),
                         "-processor", AP.class.getName(),
                         "-AexpectedEnclosedElements=m1x=>impl")
                .outdir(classes)
                .files(findJavaFiles(moduleSrc))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.isEmpty())
            throw new AssertionError("Unexpected output: " + log);
    }

    @Test
    public void testAPMultiModule(Path base) throws Exception {
        Path moduleSrc = base.resolve("module-src");
        Path m1 = moduleSrc.resolve("m1x");
        Path m2 = moduleSrc.resolve("m2x");

        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        tb.writeJavaFiles(m1,
                          "module m1x { }",
                          "package impl1; public class Impl1 { }");

        tb.writeJavaFiles(m2,
                          "module m2x { }",
                          "package impl2; public class Impl2 { }");

        String log = new JavacTask(tb)
                .options("--module-source-path", moduleSrc.toString(),
                         "-processor", AP.class.getName(),
                         "-AexpectedEnclosedElements=m1x=>impl1,m2x=>impl2")
                .outdir(classes)
                .files(findJavaFiles(moduleSrc))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.isEmpty())
            throw new AssertionError("Unexpected output: " + log);
    }

    @SupportedAnnotationTypes("*")
    @SupportedOptions("expectedEnclosedElements")
    public static final class AP extends AbstractProcessor {

        private Map<String, List<String>> module2ExpectedEnclosedElements;
        private Set<String> seenModules = new HashSet<>();

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            if (module2ExpectedEnclosedElements == null) {
                module2ExpectedEnclosedElements = new HashMap<>();

                String expectedEnclosedElements =
                        processingEnv.getOptions().get("expectedEnclosedElements");

                for (String moduleDef : expectedEnclosedElements.split(",")) {
                    String[] module2Packages = moduleDef.split("=>");

                    module2ExpectedEnclosedElements.put(module2Packages[0],
                                                        List.of(module2Packages[1].split(":")));
                }
            }

            //verify ModuleType and ModuleSymbol behavior:
            for (Element root : roundEnv.getRootElements()) {
                ModuleElement module = processingEnv.getElementUtils().getModuleOf(root);

                assertEquals(TypeKind.MODULE, module.asType().getKind());

                boolean[] seenModule = new boolean[1];

                module.accept(new ElementScanner14<Void, Void>() {
                    @Override
                    public Void visitModule(ModuleElement e, Void p) {
                        seenModule[0] = true;
                        return null;
                    }
                    @Override
                    public Void scan(Element e, Void p) {
                        throw new AssertionError("Shouldn't get here.");
                    }
                }, null);

                assertEquals(true, seenModule[0]);

                List<String> actualElements =
                        module.getEnclosedElements()
                              .stream()
                              .map(s -> (PackageElement) s)
                              .map(p -> p.getQualifiedName().toString())
                              .collect(Collectors.toList());

                String moduleName = module.getQualifiedName().toString();

                assertEquals(module2ExpectedEnclosedElements.get(moduleName),
                             actualElements);

                seenModules.add(moduleName);
            }

            if (roundEnv.processingOver()) {
                assertEquals(module2ExpectedEnclosedElements.keySet(), seenModules);
            }

            return false;
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latest();
        }

    }

    @Test
    public void testVerifyUsesProvides(Path base) throws Exception {
        Path moduleSrc = base.resolve("module-src");
        Path m1 = moduleSrc.resolve("m1x");

        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        tb.writeJavaFiles(m1,
                          "module m1x { exports api; uses api.Api; provides api.Api with impl.Impl; }",
                          "package api; public class Api { }",
                          "package impl; public class Impl extends api.Api { }");

        String log = new JavacTask(tb)
                .options("-doe", "-processor", VerifyUsesProvidesAP.class.getName())
                .outdir(classes)
                .files(findJavaFiles(moduleSrc))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.isEmpty())
            throw new AssertionError("Unexpected output: " + log);
    }

    @SupportedAnnotationTypes("*")
    public static final class VerifyUsesProvidesAP extends AbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            TypeElement api = processingEnv.getElementUtils().getTypeElement("api.Api");

            assertNonNull("Cannot find api.Api", api);

            ModuleElement modle = (ModuleElement) processingEnv.getElementUtils().getPackageOf(api).getEnclosingElement();

            assertNonNull("modle is null", modle);

            List<? extends UsesDirective> uses = ElementFilter.usesIn(modle.getDirectives());
            assertEquals(1, uses.size());
            assertEquals("api.Api", uses.iterator().next().getService().getQualifiedName().toString());

            List<? extends ProvidesDirective> provides = ElementFilter.providesIn(modle.getDirectives());
            assertEquals(1, provides.size());
            assertEquals("api.Api", provides.iterator().next().getService().getQualifiedName().toString());
            assertEquals("impl.Impl", provides.iterator().next().getImplementations().get(0).getQualifiedName().toString());

            return false;
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latest();
        }

    }

    @Test
    public void testPackageNoModule(Path base) throws Exception {
        Path src = base.resolve("src");
        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        tb.writeJavaFiles(src,
                          "package api; public class Api { }");

        String log = new JavacTask(tb)
                .options("-processor", VerifyPackageNoModule.class.getName(),
                         "-source", "8",
                         "-Xlint:-options")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.isEmpty())
            throw new AssertionError("Unexpected output: " + log);
    }

    @SupportedAnnotationTypes("*")
    public static final class VerifyPackageNoModule extends AbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            TypeElement api = processingEnv.getElementUtils().getTypeElement("api.Api");

            assertNonNull("Cannot find api.Api", api);

            ModuleElement modle = (ModuleElement) processingEnv.getElementUtils().getPackageOf(api).getEnclosingElement();

            assertNull("modle is not null", modle);

            return false;
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latest();
        }

    }

    @Test
    public void testQualifiedClassForProcessing(Path base) throws Exception {
        Path moduleSrc = base.resolve("module-src");
        Path m1 = moduleSrc.resolve("m1x");
        Path m2 = moduleSrc.resolve("m2x");

        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        tb.writeJavaFiles(m1,
                          "module m1x { }",
                          "package impl; public class Impl { int m1x; }");

        tb.writeJavaFiles(m2,
                          "module m2x { }",
                          "package impl; public class Impl { int m2x; }");

        new JavacTask(tb)
            .options("--module-source-path", moduleSrc.toString())
            .outdir(classes)
            .files(findJavaFiles(moduleSrc))
            .run()
            .writeAll()
            .getOutput(Task.OutputKind.DIRECT);

        List<String> expected = List.of("Note: field: m1x");

        for (Mode mode : new Mode[] {Mode.API, Mode.CMDLINE}) {
            List<String> log = new JavacTask(tb, mode)
                    .options("-processor", QualifiedClassForProcessing.class.getName(),
                             "--module-path", classes.toString())
                    .classes("m1x/impl.Impl")
                    .outdir(classes)
                    .run()
                    .writeAll()
                    .getOutputLines(Task.OutputKind.DIRECT);

            if (!expected.equals(log))
                throw new AssertionError("Unexpected output: " + log);
        }
    }

    @SupportedAnnotationTypes("*")
    public static final class QualifiedClassForProcessing extends AbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            if (processingEnv.getElementUtils().getModuleElement("m1x") == null) {
                throw new AssertionError("No m1x module found.");
            }

            Messager messager = processingEnv.getMessager();

            for (TypeElement clazz : ElementFilter.typesIn(roundEnv.getRootElements())) {
                for (VariableElement field : ElementFilter.fieldsIn(clazz.getEnclosedElements())) {
                    messager.printMessage(Kind.NOTE, "field: " + field.getSimpleName());
                }
            }

            return false;
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latest();
        }

    }

    @Test
    public void testModuleInRootElements(Path base) throws Exception {
        Path moduleSrc = base.resolve("module-src");
        Path m1 = moduleSrc.resolve("m1");

        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        tb.writeJavaFiles(m1,
                          "module m1x { exports api; }",
                          "package api; public class Api { }");

        List<String> log = new JavacTask(tb)
                .options("-processor", ModuleInRootElementsAP.class.getName())
                .outdir(classes)
                .files(findJavaFiles(moduleSrc))
                .run()
                .writeAll()
                .getOutputLines(Task.OutputKind.STDERR);

        assertEquals(List.of("module: m1x"), log);
    }

    @SupportedAnnotationTypes("*")
    public static final class ModuleInRootElementsAP extends AbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            roundEnv.getRootElements()
                    .stream()
                    .filter(el -> el.getKind() == ElementKind.MODULE)
                    .forEach(mod -> System.err.println("module: " + mod.getSimpleName()));

            return false;
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latest();
        }

    }

    @Test
    public void testAnnotationsInModuleInfo(Path base) throws Exception {
        Path moduleSrc = base.resolve("module-src");
        Path m1 = moduleSrc.resolve("m1");

        tb.writeJavaFiles(m1,
                          "@Deprecated module m1x { }");

        Path m2 = moduleSrc.resolve("m2x");

        tb.writeJavaFiles(m2,
                          "@SuppressWarnings(\"\") module m2x { }");

        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        List<String> log = new JavacTask(tb)
                .options("-processor", AnnotationsInModuleInfoPrint.class.getName())
                .outdir(classes)
                .files(findJavaFiles(m1))
                .run()
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        List<String> expectedLog = List.of("Note: AP Invoked",
                                           "Note: AP Invoked");

        assertEquals(expectedLog, log);

        new JavacTask(tb)
            .options("-processor", AnnotationsInModuleInfoFail.class.getName())
            .outdir(classes)
            .files(findJavaFiles(m2))
            .run()
            .writeAll();
    }

    @SupportedAnnotationTypes("java.lang.Deprecated")
    public static final class AnnotationsInModuleInfoPrint extends AbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            processingEnv.getMessager().printMessage(Kind.NOTE, "AP Invoked");
            return false;
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latest();
        }

    }

    @SupportedAnnotationTypes("java.lang.Deprecated")
    public static final class AnnotationsInModuleInfoFail extends AbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            throw new AssertionError();
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latest();
        }

    }

    @Test
    public void testAnnotationsWithoutTargetInModuleInfo(Path base) throws Exception {
        Path moduleSrc = base.resolve("module-src");
        Path m1 = moduleSrc.resolve("m1");

        tb.writeJavaFiles(m1,
                          "@test.A module m1x { exports test; }",
                          "package test; public @interface A { }",
                          "package test; public @interface B { }");

        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        List<String> expectedLog = List.of("Note: m1x/test.A AP Invoked",
                                           "Note: m1x/test.A AP Invoked");

        List<String> actualLog = new JavacTask(tb)
                .options("-processor", AnnotationsWithoutTargetInModuleInfo.class.getName()
                         + "," + AnnotationsWithoutTargetNotInModuleInfo.class.getName())
                .outdir(classes)
                .files(findJavaFiles(m1))
                .run()
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        tb.checkEqual(expectedLog, actualLog);
    }

    @SupportedAnnotationTypes("m1x/test.A")
    public static final class AnnotationsWithoutTargetInModuleInfo extends AbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            processingEnv.getMessager().printMessage(Kind.NOTE, "m1x/test.A AP Invoked");
            return false;
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latest();
        }

    }

    @SupportedAnnotationTypes("m1x/test.B")
    public static final class AnnotationsWithoutTargetNotInModuleInfo extends AbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            processingEnv.getMessager().printMessage(Kind.NOTE, "m1x/test.B AP Invoked");
            return false;
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latest();
        }

    }

    @Test
    public void testGenerateInMultiModeAPI(Path base) throws Exception {
        Path moduleSrc = base.resolve("module-src");
        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        Path m1 = moduleSrc.resolve("m1x");

        tb.writeJavaFiles(m1,
                          "module m1x { exports api1; }",
                          "package api1; public class Api { }",
                          "package clash; public class C { }");

        writeFile("1", m1, "api1", "api");
        writeFile("2", m1, "clash", "clash");

        Path m2 = moduleSrc.resolve("m2x");

        tb.writeJavaFiles(m2,
                          "module m2x { requires m1x; exports api2; }",
                          "package api2; public class Api { }",
                          "package clash; public class C { }");

        writeFile("3", m2, "api2", "api");
        writeFile("4", m2, "clash", "api");

        //passing testcases:
        for (String module : Arrays.asList("", "m1x/")) {
            for (String originating : Arrays.asList("", ", jlObject")) {
                tb.writeJavaFiles(m1,
                                  "package test; class Test { api1.Impl i; }");

                //source:
                runCompiler(base,
                            moduleSrc,
                            classes,
                            "createSource(() -> filer.createSourceFile(\"" + module + "api1.Impl\"" + originating + "), \"api1.Impl\", \"package api1; public class Impl {}\")",
                            "--module-source-path", moduleSrc.toString());
                assertFileExists(classes, "m1x", "api1", "Impl.class");

                //class:
                runCompiler(base,
                            moduleSrc,
                            classes,
                            "createClass(() -> filer.createClassFile(\"" + module + "api1.Impl\"" + originating + "), \"api1.Impl\", \"package api1; public class Impl {}\")",
                            "--module-source-path", moduleSrc.toString());
                assertFileExists(classes, "m1x", "api1", "Impl.class");

                tb.deleteFiles(m1.resolve("test").resolve("Test.java"));

                //resource class output:
                runCompiler(base,
                            moduleSrc,
                            classes,
                            "createSource(() -> filer.createResource(StandardLocation.CLASS_OUTPUT, \"" + module + "api1\", \"impl\"" + originating + "), \"impl\", \"impl\")",
                            "--module-source-path", moduleSrc.toString());
                assertFileExists(classes, "m1x", "api1", "impl");
            }
        }

        //get resource module source path:
        runCompiler(base,
                    m1,
                    classes,
                    "doReadResource(() -> filer.getResource(StandardLocation.MODULE_SOURCE_PATH, \"m1x/api1\", \"api\"), \"1\")",
                    "--module-source-path", moduleSrc.toString());

        //can generate resources to the single root module:
        runCompiler(base,
                    m1,
                    classes,
                    "createSource(() -> filer.createResource(StandardLocation.CLASS_OUTPUT, \"m1x/impl\", \"impl\"), \"impl\", \"impl\")",
                    "--module-source-path", moduleSrc.toString());
        assertFileExists(classes, "m1x", "impl", "impl");

        //check --default-module-for-created-files option:
        for (String pack : Arrays.asList("clash", "doesnotexist")) {
            tb.writeJavaFiles(m1,
                              "package test; class Test { " + pack + ".Pass i; }");
            runCompiler(base,
                        moduleSrc,
                        classes,
                        "createSource(() -> filer.createSourceFile(\"" + pack + ".Pass\")," +
                        "                                          \"" + pack + ".Pass\"," +
                        "                                          \"package " + pack + ";" +
                        "                                            public class Pass { }\")",
                        "--module-source-path", moduleSrc.toString(),
                        "--default-module-for-created-files=m1x");
            assertFileExists(classes, "m1x", pack, "Pass.class");
            assertFileNotExists(classes, "m2x", pack, "Pass.class");

            runCompiler(base,
                        moduleSrc,
                        classes,
                        "createClass(() -> filer.createClassFile(\"" + pack + ".Pass\")," +
                        "                                        \"" + pack + ".Pass\"," +
                        "                                        \"package " + pack + ";" +
                        "                                          public class Pass { }\")",
                        "--module-source-path", moduleSrc.toString(),
                        "--default-module-for-created-files=m1x");
            assertFileExists(classes, "m1x", pack, "Pass.class");
            assertFileNotExists(classes, "m2x", pack, "Pass.class");

            tb.deleteFiles(m1.resolve("test").resolve("Test.java"));

            runCompiler(base,
                        moduleSrc,
                        classes,
                        "createSource(() -> filer.createResource(StandardLocation.CLASS_OUTPUT," +
                        "                                        \"" + pack + "\", \"impl\"), \"impl\", \"impl\")",
                        "--module-source-path", moduleSrc.toString(),
                        "--default-module-for-created-files=m1x");
            assertFileExists(classes, "m1x", pack, "impl");
            assertFileNotExists(classes, "m2x", pack, "impl");

            runCompiler(base,
                        moduleSrc,
                        classes,
                        "doReadResource(() -> filer.getResource(StandardLocation.CLASS_OUTPUT," +
                        "                                       \"" + pack + "\", \"resource\"), \"1\")",
                        p -> writeFile("1", p.resolve("m1x"), pack, "resource"),
                        "--module-source-path", moduleSrc.toString(),
                        "--default-module-for-created-files=m1x");
        }

        //wrong default module:
        runCompiler(base,
                    moduleSrc,
                    classes,
                    "expectFilerException(() -> filer.createResource(StandardLocation.CLASS_OUTPUT," +
                    "                                                \"clash\", \"impl\"))",
                    "--module-source-path", moduleSrc.toString(),
                    "--default-module-for-created-files=doesnotexist");

        String[] failingCases = {
            //must not generate to unnamed package:
            "expectFilerException(() -> filer.createSourceFile(\"Fail\"))",
            "expectFilerException(() -> filer.createClassFile(\"Fail\"))",
            "expectFilerException(() -> filer.createSourceFile(\"m1x/Fail\"))",
            "expectFilerException(() -> filer.createClassFile(\"m1x/Fail\"))",

            //cannot infer module name, package clash:
            "expectFilerException(() -> filer.createSourceFile(\"clash.Fail\"))",
            "expectFilerException(() -> filer.createClassFile(\"clash.Fail\"))",
            "expectFilerException(() -> filer.createResource(StandardLocation.CLASS_OUTPUT, \"clash\", \"impl\"))",
            "expectFilerException(() -> filer.getResource(StandardLocation.CLASS_OUTPUT, \"clash\", \"impl\"))",

            //cannot infer module name, package does not exist:
            "expectFilerException(() -> filer.createSourceFile(\"doesnotexist.Fail\"))",
            "expectFilerException(() -> filer.createClassFile(\"doesnotexist.Fail\"))",
            "expectFilerException(() -> filer.createResource(StandardLocation.CLASS_OUTPUT, \"doesnotexist\", \"impl\"))",
            "expectFilerException(() -> filer.getResource(StandardLocation.CLASS_OUTPUT, \"doesnotexist\", \"impl\"))",

            //cannot generate sources/classes to modules that are not root modules:
            "expectFilerException(() -> filer.createSourceFile(\"java.base/fail.Fail\"))",
            "expectFilerException(() -> filer.createClassFile(\"java.base/fail.Fail\"))",

            //cannot read from module locations if module not given and not inferable:
            "expectFilerException(() -> filer.getResource(StandardLocation.SYSTEM_MODULES, \"fail\", \"Fail\"))",

            //wrong module given:
            "expectException(() -> filer.getResource(StandardLocation.SYSTEM_MODULES, \"java.compiler/java.lang\", \"Object.class\"))",
        };

        for (String failingCode : failingCases) {
            System.err.println("failing code: " + failingCode);
            runCompiler(base,
                        moduleSrc,
                        classes,
                        failingCode,
                        "--module-source-path", moduleSrc.toString());
        }
    }

    public static abstract class GeneratingAP extends AbstractProcessor {

        public void createSource(CreateFileObject file, String name, String content) {
            try (Writer out = file.create().openWriter()) {
                out.write(content);
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }

        public void createClass(CreateFileObject file, String name, String content) {
            String fileNameStub = name.replace(".", File.separator);

            try (OutputStream out = file.create().openOutputStream()) {
                Path scratch = Files.createDirectories(Paths.get(""));
                Path scratchSrc = scratch.resolve(fileNameStub + ".java").toAbsolutePath();

                Files.createDirectories(scratchSrc.getParent());

                try (Writer w = Files.newBufferedWriter(scratchSrc)) {
                    w.write(content);
                }

                Path scratchClasses = scratch.resolve("classes");

                Files.createDirectories(scratchClasses);

                JavaCompiler comp = ToolProvider.getSystemJavaCompiler();
                try (StandardJavaFileManager fm = comp.getStandardFileManager(null, null, null)) {
                    List<String> options = List.of("-d", scratchClasses.toString());
                    Iterable<? extends JavaFileObject> files = fm.getJavaFileObjects(scratchSrc);
                    CompilationTask task = comp.getTask(null, fm, null, options, null, files);

                    if (!task.call()) {
                        throw new AssertionError("compilation failed");
                    }
                }

                Path classfile = scratchClasses.resolve(fileNameStub + ".class");

                Files.copy(classfile, out);
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }

        public void doReadResource(CreateFileObject file, String expectedContent) {
            try {
                StringBuilder actualContent = new StringBuilder();

                try (Reader r = file.create().openReader(true)) {
                    int read;

                    while ((read = r.read()) != (-1)) {
                        actualContent.append((char) read);
                    }

                }

                assertEquals(expectedContent, actualContent.toString());
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }

        public void checkResourceExists(CreateFileObject file) {
            try {
                file.create().openInputStream().close();
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }

        public interface CreateFileObject {
            public FileObject create() throws IOException;
        }

        public void expectFilerException(Callable<Object> c) {
            try {
                c.call();
                throw new AssertionError("Expected exception not thrown");
            } catch (FilerException ex) {
                //expected
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }

        public void expectException(Callable<Object> c) {
            try {
                c.call();
                throw new AssertionError("Expected exception not thrown");
            } catch (IOException ex) {
                //expected
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latest();
        }

    }

    @Test
    public void testGenerateSingleModule(Path base) throws Exception {
        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        Path src = base.resolve("module-src");
        Path m1 = src.resolve("m1x");

        tb.writeJavaFiles(m1,
                          "module m1x { }",
                          "package test; class Test { impl.Impl i; }");
        Path m2 = src.resolve("m2x");

        tb.writeJavaFiles(m2,
                          "module m2x { }");

        for (String[] options : new String[][] {new String[] {"-sourcepath", m1.toString()},
                                                new String[] {"--module-source-path", src.toString()}}) {
            String modulePath = options[0].equals("--module-source-path") ? "m1x" : "";
            //passing testcases:
            for (String module : Arrays.asList("", "m1x/")) {
                for (String originating : Arrays.asList("", ", jlObject")) {
                    tb.writeJavaFiles(m1,
                                      "package test; class Test { impl.Impl i; }");

                    //source:
                    runCompiler(base,
                                m1,
                                classes,
                                "createSource(() -> filer.createSourceFile(\"" + module + "impl.Impl\"" + originating + "), \"impl.Impl\", \"package impl; public class Impl {}\")",
                                options);
                    assertFileExists(classes, modulePath, "impl", "Impl.class");

                    //class:
                    runCompiler(base,
                                m1,
                                classes,
                                "createClass(() -> filer.createClassFile(\"" + module + "impl.Impl\"" + originating + "), \"impl.Impl\", \"package impl; public class Impl {}\")",
                                options);
                    assertFileExists(classes, modulePath, "impl", "Impl.class");

                    tb.deleteFiles(m1.resolve("test").resolve("Test.java"));

                    //resource class output:
                    runCompiler(base,
                                m1,
                                classes,
                                """
                                    createSource(() -> filer.createResource(StandardLocation.CLASS_OUTPUT, "impl", "impl\"""" + originating + "), \"impl\", \"impl\")",
                                options);
                    assertFileExists(classes, modulePath, "impl", "impl");
                }
            }
        }

        //get resource source path:
        writeFile("1", m1, "impl", "resource");
        runCompiler(base,
                    m1,
                    classes,
                    "doReadResource(() -> filer.getResource(StandardLocation.SOURCE_PATH, \"impl\", \"resource\"), \"1\")",
                    "-sourcepath", m1.toString());
        //must not specify module when reading non-module oriented locations:
        runCompiler(base,
                    m1,
                    classes,
                    "expectFilerException(() -> filer.getResource(StandardLocation.SOURCE_PATH, \"m1x/impl\", \"resource\"))",
                    "-sourcepath", m1.toString());

        tb.deleteFiles(m1.resolve("impl").resolve("resource"));

        //can read resources from the system module path if module name given:
        runCompiler(base,
                    m1,
                    classes,
                    "checkResourceExists(() -> filer.getResource(StandardLocation.SYSTEM_MODULES, \"java.base/java.lang\", \"Object.class\"))",
                    "-sourcepath", m1.toString());

        //can read resources from the system module path if module inferable:
        runCompiler(base,
                    m1,
                    classes,
                    "expectFilerException(() -> filer.getResource(StandardLocation.SYSTEM_MODULES, \"java.lang\", \"Object.class\"))",
                    "-sourcepath", m1.toString());

        //cannot generate resources to modules that are not root modules:
        runCompiler(base,
                    m1,
                    classes,
                    "expectFilerException(() -> filer.createResource(StandardLocation.CLASS_OUTPUT, \"java.base/fail\", \"Fail\"))",
                    "--module-source-path", src.toString());

        //can generate resources to the single root module:
        runCompiler(base,
                    m1,
                    classes,
                    "createSource(() -> filer.createResource(StandardLocation.CLASS_OUTPUT, \"impl\", \"impl\"), \"impl\", \"impl\")",
                    "--module-source-path", src.toString());
        assertFileExists(classes, "m1x", "impl", "impl");

        String[] failingCases = {
            //must not generate to unnamed package:
            "expectFilerException(() -> filer.createSourceFile(\"Fail\"))",
            "expectFilerException(() -> filer.createClassFile(\"Fail\"))",
            "expectFilerException(() -> filer.createSourceFile(\"m1x/Fail\"))",
            "expectFilerException(() -> filer.createClassFile(\"m1x/Fail\"))",

            //cannot generate sources/classes to modules that are not root modules:
            "expectFilerException(() -> filer.createSourceFile(\"java.base/fail.Fail\"))",
            "expectFilerException(() -> filer.createClassFile(\"java.base/fail.Fail\"))",

            //cannot specify module name for class output when not in the multi-module mode:
            "expectFilerException(() -> filer.createResource(StandardLocation.CLASS_OUTPUT, \"m1x/fail\", \"Fail\"))",

            //cannot read from module locations if module not given:
            "expectFilerException(() -> filer.getResource(StandardLocation.SYSTEM_MODULES, \"fail\", \"Fail\"))",

            //wrong module given:
            "expectException(() -> filer.getResource(StandardLocation.SYSTEM_MODULES, \"java.compiler/java.lang\", \"Object.class\"))",
        };

        for (String failingCode : failingCases) {
            System.err.println("failing code: " + failingCode);
            runCompiler(base,
                        m1,
                        classes,
                        failingCode,
                        "-sourcepath", m1.toString());
        }

        tb.deleteFiles(m1.resolve("module-info.java"));
        tb.writeJavaFiles(m1,
                          "package test; class Test { }");

        runCompiler(base,
                    m1,
                    classes,
                    "expectFilerException(() -> filer.createSourceFile(\"m1x/impl.Impl\"))",
                    "-sourcepath", m1.toString(),
                    "-source", "8");

        runCompiler(base,
                    m1,
                    classes,
                    "expectFilerException(() -> filer.createClassFile(\"m1x/impl.Impl\"))",
                    "-sourcepath", m1.toString(),
                    "-source", "8");
    }

    private void runCompiler(Path base, Path src, Path classes,
                             String code, String... options) throws IOException {
        runCompiler(base, src, classes, code, p -> {}, options);
    }

    private void runCompiler(Path base, Path src, Path classes,
                             String code, Consumer<Path> generateToClasses,
                             String... options) throws IOException {
        Path apClasses = base.resolve("ap-classes");
        if (Files.exists(apClasses)) {
            tb.cleanDirectory(apClasses);
        } else {
            Files.createDirectories(apClasses);
        }
        compileAP(apClasses, code);
        if (Files.exists(classes)) {
            tb.cleanDirectory(classes);
        } else {
            Files.createDirectories(classes);
        }
        generateToClasses.accept(classes);
        List<String> opts = new ArrayList<>();
        opts.addAll(Arrays.asList(options));
        opts.add("-processorpath");
        opts.add(System.getProperty("test.class.path") + File.pathSeparator + apClasses.toString());
        opts.add("-processor");
        opts.add("AP");
        new JavacTask(tb)
          .options(opts)
          .outdir(classes)
          .files(findJavaFiles(src))
          .run()
          .writeAll();
    }

    private void compileAP(Path target, String code) {
        String processorCode =
            """
                import java.util.*;
                import javax.annotation.processing.*;
                import javax.lang.model.*;
                import javax.lang.model.element.*;
                import javax.lang.model.type.*;
                import javax.lang.model.util.*;
                import javax.tools.*;
                @SupportedAnnotationTypes("*")
                public final class AP extends AnnotationProcessing.GeneratingAP {

                        int round;

                        @Override
                        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
                            if (round++ != 0)
                                return false;
                            Filer filer = processingEnv.getFiler();
                            TypeElement jlObject = processingEnv.getElementUtils().getTypeElement("java.lang.Object");
                """ + code + """
                ;
                            return false;
                        }
                    }
                """;
        new JavacTask(tb)
          .options("-classpath", System.getProperty("test.class.path"))
          .sources(processorCode)
          .outdir(target)
          .run()
          .writeAll();
    }

    @Test
    public void testGenerateInUnnamedModeAPI(Path base) throws Exception {
        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        Path src = base.resolve("src");

        tb.writeJavaFiles(src,
                          "class T {}");

        new JavacTask(tb)
          .options("-processor", UnnamedModeAPITestAP.class.getName(),
                   "-sourcepath", src.toString())
          .outdir(classes)
          .files(findJavaFiles(src))
          .run()
          .writeAll();

        assertFileExists(classes, "Impl1.class");
        assertFileExists(classes, "Impl2.class");
    }

    @Test
    public void testGenerateInNoModeAPI(Path base) throws Exception {
        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        Path src = base.resolve("src");

        tb.writeJavaFiles(src,
                          "class T {}");

        new JavacTask(tb)
          .options("-processor", UnnamedModeAPITestAP.class.getName(),
                   "-source", "8", "-target", "8",
                   "-sourcepath", src.toString())
          .outdir(classes)
          .files(findJavaFiles(src))
          .run()
          .writeAll();

        assertFileExists(classes, "Impl1.class");
        assertFileExists(classes, "Impl2.class");
    }

    @SupportedAnnotationTypes("*")
    public static final class UnnamedModeAPITestAP extends GeneratingAP {

        int round;

        @Override
        public synchronized void init(ProcessingEnvironment processingEnv) {
            super.init(processingEnv);
        }

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            if (round++ != 0)
                return false;

            Filer filer = processingEnv.getFiler();

            //must not generate to unnamed package:
            createSource(() -> filer.createSourceFile("Impl1"), "Impl1", "class Impl1 {}");
            createClass(() -> filer.createClassFile("Impl2"), "Impl2", "class Impl2 {}");

            return false;
        }

    }

    @Test
    public void testDisambiguateAnnotations(Path base) throws Exception {
        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        Path src = base.resolve("src");
        Path m1 = src.resolve("m1x");

        tb.writeJavaFiles(m1,
                          "module m1x { exports api; }",
                          "package api; public @interface A {}",
                          "package api; public @interface B {}");

        Path m2 = src.resolve("m2x");

        tb.writeJavaFiles(m2,
                          "module m2x { exports api; }",
                          "package api; public @interface A {}",
                          "package api; public @interface B {}");

        Path m3 = src.resolve("m3x");

        tb.writeJavaFiles(m3,
                          "module m3x { requires m1x; }",
                          "package impl; import api.*; @A @B public class T {}");

        Path m4 = src.resolve("m4x");

        tb.writeJavaFiles(m4,
                          "module m4x { requires m2x; }",
                          "package impl; import api.*; @A @B public class T {}");

        List<String> log;
        List<String> expected;

        log = new JavacTask(tb)
            .options("-processor", SelectAnnotationATestAP.class.getName() + "," + SelectAnnotationBTestAP.class.getName(),
                     "--module-source-path", src.toString(),
                     "-m", "m1x,m2x")
            .outdir(classes)
            .run()
            .writeAll()
            .getOutputLines(OutputKind.STDERR);

        expected = List.of("");

        if (!expected.equals(log)) {
            throw new AssertionError("Output does not match; output: " + log);
        }

        log = new JavacTask(tb)
            .options("-processor", SelectAnnotationATestAP.class.getName() + "," + SelectAnnotationBTestAP.class.getName(),
                     "--module-source-path", src.toString(),
                     "-m", "m3x")
            .outdir(classes)
            .run()
            .writeAll()
            .getOutputLines(OutputKind.STDERR);

        expected = List.of("SelectAnnotationBTestAP",
                           "SelectAnnotationBTestAP");

        if (!expected.equals(log)) {
            throw new AssertionError("Output does not match; output: " + log);
        }

        log = new JavacTask(tb)
            .options("-processor", SelectAnnotationATestAP.class.getName() + "," +
                                   SelectAnnotationBTestAP.class.getName() + "," +
                                   SelectAnnotationAStrictTestAP.class.getName(),
                     "--module-source-path", src.toString(),
                     "-m", "m4x")
            .outdir(classes)
            .run()
            .writeAll()
            .getOutputLines(OutputKind.STDERR);

        expected = List.of("SelectAnnotationATestAP",
                           "SelectAnnotationBTestAP",
                           "SelectAnnotationAStrictTestAP",
                           "SelectAnnotationATestAP",
                           "SelectAnnotationBTestAP",
                           "SelectAnnotationAStrictTestAP");

        if (!expected.equals(log)) {
            throw new AssertionError("Output does not match; output: " + log);
        }
    }

    @Test
    public void testDisambiguateAnnotationsUnnamedModule(Path base) throws Exception {
        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        Path src = base.resolve("src");

        tb.writeJavaFiles(src,
                          "package api; public @interface A {}",
                          "package api; public @interface B {}",
                          "package impl; import api.*; @A @B public class T {}");

        List<String> log = new JavacTask(tb)
            .options("-processor", SelectAnnotationATestAP.class.getName() + "," +
                                   SelectAnnotationBTestAP.class.getName() + "," +
                                   SelectAnnotationAStrictTestAP.class.getName())
            .outdir(classes)
            .files(findJavaFiles(src))
            .run()
            .writeAll()
            .getOutputLines(OutputKind.STDERR);

        List<String> expected = List.of("SelectAnnotationBTestAP",
                                        "SelectAnnotationBTestAP");

        if (!expected.equals(log)) {
            throw new AssertionError("Output does not match; output: " + log);
        }
    }

    @Test
    public void testDisambiguateAnnotationsNoModules(Path base) throws Exception {
        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        Path src = base.resolve("src");

        tb.writeJavaFiles(src,
                          "package api; public @interface A {}",
                          "package api; public @interface B {}",
                          "package impl; import api.*; @A @B public class T {}");

        List<String> log = new JavacTask(tb)
            .options("-processor", SelectAnnotationATestAP.class.getName() + "," +
                                   SelectAnnotationBTestAP.class.getName() + "," +
                                   SelectAnnotationAStrictTestAP.class.getName(),
                     "-source", "8", "-target", "8")
            .outdir(classes)
            .files(findJavaFiles(src))
            .run()
            .writeAll()
            .getOutputLines(OutputKind.STDERR);

        List<String> expected = List.of("SelectAnnotationATestAP",
                                        "SelectAnnotationBTestAP",
                                        "SelectAnnotationATestAP",
                                        "SelectAnnotationBTestAP");

        if (!expected.equals(log)) {
            throw new AssertionError("Output does not match; output: " + log);
        }
    }

    @SupportedAnnotationTypes("m2x/api.A")
    public static final class SelectAnnotationATestAP extends AbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            System.err.println("SelectAnnotationATestAP");

            return false;
        }

    }

    @SupportedAnnotationTypes("api.B")
    public static final class SelectAnnotationBTestAP extends AbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            System.err.println("SelectAnnotationBTestAP");

            return false;
        }

    }

    public static final class SelectAnnotationAStrictTestAP extends AbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            System.err.println("SelectAnnotationAStrictTestAP");

            return false;
        }

        @Override
        public Set<String> getSupportedAnnotationTypes() {
            return Set.of("m2x/api.A");
        }

    }

    private static void writeFile(String content, Path base, String... pathElements) {
        try {
            Path file = resolveFile(base, pathElements);

            Files.createDirectories(file.getParent());

            try (Writer out = Files.newBufferedWriter(file)) {
                out.append(content);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Test
    public void testUnboundLookupNew(Path base) throws Exception {
        Path moduleSrc = base.resolve("module-src");
        Path m1 = moduleSrc.resolve("m1x");
        Path m2 = moduleSrc.resolve("m2x");
        Path m3 = moduleSrc.resolve("m3x");
        Path m4 = moduleSrc.resolve("m4x");

        Path src = base.resolve("src");

        Path classes = base.resolve("classes");
        Path srcClasses = base.resolve("src-classes");

        Files.createDirectories(classes);
        Files.createDirectories(srcClasses);
        Files.createDirectories(moduleSrc);

        {
            tb.cleanDirectory(classes);
            tb.cleanDirectory(moduleSrc);

            tb.writeJavaFiles(m1,
                              "module m1x { exports api; }",
                              "package test; public class Test { }",
                              "package api; public class API {}");

            tb.writeJavaFiles(m2,
                              "module m2x { requires m1x; }",
                              "package test; public class Test { }",
                              "package api.impl; public class Impl { }");

            new JavacTask(tb)
                .options("--module-source-path", moduleSrc.toString())
                .outdir(classes)
                .files(findJavaFiles(moduleSrc))
                .run()
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

            List<String> log = new JavacTask(tb)
                .options("--module-source-path", moduleSrc.toString(),
                         "-processorpath", System.getProperty("test.class.path"),
                         "-processor", UnboundLookupNew.class.getName(),
                         "-AlookupClass=+test.Test",
                         "-AlookupPackage=+test,+api",
                         "-XDrawDiagnostics")
                .outdir(classes)
                .files(findJavaFiles(m2))
                .run()
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

            List<String> expected = Arrays.asList("- compiler.note.proc.messager: test.Test found in module: m2x",
                                                  "- compiler.note.proc.messager: test found in module: m2x",
                                                  "- compiler.note.proc.messager: api found in module: m1x",
                                                  "- compiler.note.proc.messager: test.Test found in module: m2x",
                                                  "- compiler.note.proc.messager: test found in module: m2x",
                                                  "- compiler.note.proc.messager: api found in module: m1x"
                                                 );

            if (!expected.equals(log)) {
                throw new AssertionError("Expected output not found: " + log);
            }
        }

        {
            tb.cleanDirectory(classes);
            tb.cleanDirectory(moduleSrc);

            tb.writeJavaFiles(m1,
                              "module m1x { exports test; }",
                              "package test; public class Test { }");

            tb.writeJavaFiles(m2,
                              "module m2x { requires m1x; }",
                              "package test; public class Test { }");

            List<String> log = new JavacTask(tb)
                .options("--module-source-path", moduleSrc.toString(),
                         "-processorpath", System.getProperty("test.class.path"),
                         "-processor", UnboundLookupNew.class.getName(),
                         "-AlookupClass=+test.Test",
                         "-AlookupPackage=+test",
                         "-XDrawDiagnostics",
                         "-XDshould-stop.at=FLOW")
                .outdir(classes)
                .files(findJavaFiles(m2))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

            List<String> expected = Arrays.asList(
                    "Test.java:1:1: compiler.err.package.in.other.module: m1x",
                    "- compiler.note.proc.messager: test.Test found in module: m2x",
                    "- compiler.note.proc.messager: test found in module: m1x",
                    "- compiler.note.proc.messager: test.Test found in module: m2x",
                    "- compiler.note.proc.messager: test found in module: m1x",
                    "1 error");

            if (!expected.equals(log)) {
                throw new AssertionError("Expected output not found: " + log);
            }
        }

        {
            tb.cleanDirectory(classes);
            tb.cleanDirectory(moduleSrc);

            tb.writeJavaFiles(m1,
                              "module m1x { }",
                              "package test; public class Test { }");

            tb.writeJavaFiles(m2,
                              "module m2x { requires m1x; requires m3x; }",
                              "package test; public class Test { }");

            tb.writeJavaFiles(m3,
                              "module m3x { }",
                              "package test; public class Test { }");

            new JavacTask(tb)
                .options("--module-source-path", moduleSrc.toString())
                .outdir(classes)
                .files(findJavaFiles(moduleSrc))
                .run()
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

            List<String> log = new JavacTask(tb)
                .options("--module-source-path", moduleSrc.toString(),
                         "-processorpath", System.getProperty("test.class.path"),
                         "-processor", UnboundLookupNew.class.getName(),
                         "-AlookupClass=+test.Test",
                         "-AlookupPackage=+test",
                         "-XDrawDiagnostics")
                .outdir(classes)
                .files(findJavaFiles(m2))
                .run()
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

            List<String> expected = Arrays.asList(
                    "- compiler.note.proc.messager: test.Test found in module: m2x",
                    "- compiler.note.proc.messager: test found in module: m2x",
                    "- compiler.note.proc.messager: test.Test found in module: m2x",
                    "- compiler.note.proc.messager: test found in module: m2x"
            );

            if (!expected.equals(log)) {
                throw new AssertionError("Expected output not found: " + log);
            }
        }

        {
            tb.cleanDirectory(classes);
            tb.cleanDirectory(moduleSrc);

            tb.writeJavaFiles(m1,
                              "module m1x { exports test; }",
                              "package test; public class Test { }");

            tb.writeJavaFiles(m2,
                              "module m2x { requires m1x; requires m3x; }");

            tb.writeJavaFiles(m3,
                              "module m3x { exports test; }",
                              "package test; public class Test { }");

            tb.writeJavaFiles(m4,
                              "module m4x { }",
                              "package test; public class Test { }");

            {
                List<String> log = new JavacTask(tb)
                    .options("--module-source-path", moduleSrc.toString(),
                             "-processorpath", System.getProperty("test.class.path"),
                             "-processor", UnboundLookupNew.class.getName(),
                             "-AlookupClass=+test.Test",
                             "-AlookupPackage=+test",
                             "-XDrawDiagnostics",
                             "-XDshould-stop.at=FLOW")
                    .outdir(classes)
                    .files(findJavaFiles(m2))
                    .run(Task.Expect.FAIL)
                    .writeAll()
                    .getOutputLines(OutputKind.DIRECT);

                List<Set<String>> expected = Arrays.asList(
                        variants("module-info.java:1:1: compiler.err.package.clash.from.requires: m2x, test, m1x, m3x"),
                        variants("- compiler.note.proc.messager: test.Test found in module: m1x"),
                        variants("- compiler.note.proc.messager: test found in module: m1x"),
                        variants("- compiler.note.proc.messager: test.Test found in module: m1x"),
                        variants("- compiler.note.proc.messager: test found in module: m1x"),
                        variants("1 error"));

                assertErrorsWithVariants(expected, log);
            }

            {
                List<String> log = new JavacTask(tb)
                    .options("--module-source-path", moduleSrc.toString(),
                             "-processorpath", System.getProperty("test.class.path"),
                             "-processor", UnboundLookupNew.class.getName(),
                             "-AlookupClass=-test.Test",
                             "-AlookupPackage=-test",
                             "-XDrawDiagnostics",
                             "-XDshould-stop.at=FLOW")
                    .outdir(classes)
                    .files(findJavaFiles(m2, m4))
                    .run(Task.Expect.FAIL)
                    .writeAll()
                    .getOutputLines(OutputKind.DIRECT);

                List<String> expected = Arrays.asList(
                        "module-info.java:1:1: compiler.err.package.clash.from.requires: m2x, test, m1x, m3x",
                        "- compiler.note.multiple.elements: getTypeElement, test.Test, m1x, m4x",
                        "- compiler.note.multiple.elements: getPackageElement, test, m1x, m4x",
                        "1 error");

                if (!expected.equals(log)) {
                    throw new AssertionError("Expected output not found: " + log);
                }
            }
        }

        {
            tb.cleanDirectory(classes);
            tb.cleanDirectory(moduleSrc);

            tb.writeJavaFiles(m1,
                              "module m1x { exports test; }",
                              "package test; public class Test { }");

            tb.writeJavaFiles(m2,
                              "module m2x { requires m1x; }");

            tb.writeJavaFiles(src,
                              "package test; public class Test { }");

            new JavacTask(tb)
                .options("--module-source-path", moduleSrc.toString())
                .outdir(classes)
                .files(findJavaFiles(moduleSrc))
                .run()
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

            List<String> log = new JavacTask(tb)
                .options("--module-source-path", moduleSrc.toString(),
                         "--source-path", src.toString(),
                         "-processorpath", System.getProperty("test.class.path"),
                         "-processor", UnboundLookupNew.class.getName(),
                         "-AlookupClass=+test.Test",
                         "-AlookupPackage=+test",
                         "-XDrawDiagnostics")
                .outdir(classes)
                .files(findJavaFiles(m2))
                .run()
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

            List<String> expected = Arrays.asList(
                    "- compiler.note.proc.messager: test.Test found in module: m1x",
                    "- compiler.note.proc.messager: test found in module: m1x",
                    "- compiler.note.proc.messager: test.Test found in module: m1x",
                    "- compiler.note.proc.messager: test found in module: m1x"
            );

            if (!expected.equals(log)) {
                throw new AssertionError("Expected output not found: " + log);
            }
        }

        {
            tb.cleanDirectory(classes);
            tb.cleanDirectory(moduleSrc);

            tb.writeJavaFiles(m1,
                              "module m1x { exports test; }",
                              "package test; public class Test { }");

            tb.writeJavaFiles(m2,
                              "module m2x { requires m1x; }");

            tb.writeJavaFiles(src,
                              "package test; public class Test { }");

            new JavacTask(tb)
                .options("--module-source-path", moduleSrc.toString())
                .outdir(classes)
                .files(findJavaFiles(moduleSrc))
                .run()
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

            List<String> log = new JavacTask(tb)
                .options("--module-source-path", moduleSrc.toString(),
                         "--source-path", src.toString(),
                         "--add-reads=m2x=ALL-UNNAMED",
                         "-processorpath", System.getProperty("test.class.path"),
                         "-processor", UnboundLookupNew.class.getName(),
                         "-AlookupClass=+test.Test",
                         "-AlookupPackage=+test",
                         "-XDrawDiagnostics")
                .outdir(classes)
                .files(findJavaFiles(m2))
                .run()
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

            List<String> expected = Arrays.asList(
                    "- compiler.note.proc.messager: test.Test found in module: m1x",
                    "- compiler.note.proc.messager: test found in module: m1x",
                    "- compiler.note.proc.messager: test.Test found in module: m1x",
                    "- compiler.note.proc.messager: test found in module: m1x"
            );

            if (!expected.equals(log)) {
                throw new AssertionError("Expected output not found: " + log);
            }
        }

        {
            tb.cleanDirectory(classes);
            tb.cleanDirectory(srcClasses);
            tb.cleanDirectory(moduleSrc);

            tb.writeJavaFiles(m1,
                              "module m1x { exports test; }",
                              "package test; public class Test { }");

            tb.writeJavaFiles(m2,
                              "module m2x { requires m1x; }");

            tb.writeJavaFiles(src,
                              "package test; public class Test { }");

            new JavacTask(tb)
                .options("--module-source-path", moduleSrc.toString())
                .outdir(classes)
                .files(findJavaFiles(moduleSrc))
                .run()
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

            List<String> log = new JavacTask(tb)
                .options("--module-path", classes.toString(),
                         "--source-path", src.toString(),
                         "-processorpath", System.getProperty("test.class.path"),
                         "-processor", UnboundLookupNew.class.getName(),
                         "-AlookupClass=+test.Test",
                         "-AlookupPackage=+test",
                         "--add-modules=m2x",
                         "-XDshould-stop.ifError=ATTR",
                         "-XDshould-stop.ifNoError=ATTR",
                         "-XDrawDiagnostics")
                .outdir(srcClasses)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

            List<String> expected = Arrays.asList("Test.java:1:1: compiler.err.package.in.other.module: m1x",
                                                  "- compiler.note.proc.messager: test.Test found in module: unnamed module",
                                                  "- compiler.note.proc.messager: test found in module: m1x",
                                                  "- compiler.note.proc.messager: test.Test found in module: unnamed module",
                                                  "- compiler.note.proc.messager: test found in module: m1x",
                                                  "1 error"
                                                 );

            if (!expected.equals(log)) {
                throw new AssertionError("Expected output not found: " + log);
            }
        }
    }

    private Set<String> variants(String... expected) {
        return new HashSet<>(Arrays.asList(expected));
    }

    private void assertErrorsWithVariants(List<Set<String>> expectedVariants, List<String> actual) {
        assertEquals(expectedVariants.size(), actual.size());
        Iterator<Set<String>> expIt = expectedVariants.iterator();
        Iterator<String> actIt = actual.iterator();

        while (expIt.hasNext() && actIt.hasNext()) {
            Set<String> exp = expIt.next();
            String act = actIt.next();

            if (!exp.contains(act)) {
                throw new AssertionError("Expected: " + exp + ", actual: " + act);
            }
        }
    }

    @SupportedAnnotationTypes("*")
    @SupportedOptions({"lookupClass", "lookupPackage"})
    public static final class UnboundLookupNew extends AbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            performLookup("lookupClass", processingEnv.getElementUtils()::getTypeElement);
            performLookup("lookupPackage", processingEnv.getElementUtils()::getPackageElement);

            return false;
        }

        private void performLookup(String optionName, Function<String, Element> name2Element) {
            String[] lookupList = processingEnv.getOptions().get(optionName).split(",");
            for (String lookup : lookupList) {
                boolean shouldExists = lookup.charAt(0) == '+';
                String name = lookup.substring(1);
                Element type = name2Element.apply(name);

                if (shouldExists) {
                    if (type == null) {
                        throw new AssertionError("Did not find the expected type.");
                    } else {
                        processingEnv.getMessager().printMessage(Kind.NOTE, name + " found in module: " + processingEnv.getElementUtils().getModuleOf(type));
                    }
                } else {
                    if (type != null) {
                        throw new AssertionError("Found the unexpected type.");
                    }
                }
            }
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latest();
        }

    }

    @Test
    public void testUnboundLookup(Path base) throws Exception {
        Path src = base.resolve("src");

        tb.writeJavaFiles(src,
                          "package impl.conflict.src; public class Impl { }");

        Path moduleSrc = base.resolve("module-src");
        Path m1 = moduleSrc.resolve("m1x");
        Path m2 = moduleSrc.resolve("m2x");

        Path classes = base.resolve("classes");
        Path cpClasses = base.resolve("cpClasses");

        Files.createDirectories(classes);
        Files.createDirectories(cpClasses);

        tb.writeJavaFiles(m1,
                          "module m1x { }",
                          "package impl1; public class Impl { }",
                          "package impl.conflict.module; class Impl { }",
                          "package impl.conflict.clazz; public class pkg { public static class I { } }",
                          "package impl.conflict.src; public class Impl { }",
                          "package nested.pack.pack; public class Impl { }",
                          "package unique.nested; public class Impl { }");

        tb.writeJavaFiles(m2,
                          "module m2x { }",
                          "package impl2; public class Impl { }",
                          "package impl.conflict.module; class Impl { }",
                          "package impl.conflict; public class clazz { public static class pkg { } }",
                          "package nested.pack; public class Impl { }");

        //from source:
        new JavacTask(tb)
            .options("--module-source-path", moduleSrc.toString(),
                     "--source-path", src.toString(),
                     "-processorpath", System.getProperty("test.class.path"),
                     "-processor", UnboundLookup.class.getName(),
                     "-XDrawDiagnostics")
            .outdir(classes)
            .files(findJavaFiles(moduleSrc))
            .run()
            .writeAll()
            .getOutput(OutputKind.DIRECT);

        new JavacTask(tb)
            .options("--source-path", src.toString())
            .outdir(cpClasses)
            .files(findJavaFiles(src))
            .run()
            .writeAll();

        //from classfiles:
        new JavacTask(tb)
            .options("--module-path", classes.toString(),
                     "--class-path", cpClasses.toString(),
                     "--add-modules", "m1x,m2x",
                     "-processorpath", System.getProperty("test.class.path"),
                     "-processor", UnboundLookup.class.getName(),
                     "-proc:only",
                     "-Aunnamedmodule")
            .classes("java.lang.Object")
            .run()
            .writeAll();

        //source 8:
        new JavacTask(tb)
            .options("--source-path", src.toString(),
                     "-source", "8",
                     "-processorpath", System.getProperty("test.class.path"),
                     "-processor", UnboundLookup8.class.getName())
            .outdir(cpClasses)
            .files(findJavaFiles(src))
            .run()
            .writeAll();

        //from source:
        new JavacTask(tb)
            .options("--module-source-path", moduleSrc.toString(),
                     "--source-path", src.toString(),
                     "-processorpath", System.getProperty("test.class.path"),
                     "-processor", UnboundLookupGenerate.class.getName(),
                     "-XDrawDiagnostics")
            .outdir(classes)
            .files(findJavaFiles(moduleSrc))
            .run()
            .writeAll()
            .getOutput(OutputKind.DIRECT);

    }

    @SupportedAnnotationTypes("*")
    @SupportedOptions("unnamedmodule")
    public static final class UnboundLookup extends AbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            assertTypeElementExists("impl1.Impl", "m1x");
            assertPackageElementExists("impl1", "m1x");
            assertTypeElementExists("impl2.Impl", "m2x");
            assertTypeElementExists("impl.conflict.clazz.pkg.I", "m1x");
            assertTypeElementExists("impl.conflict.clazz", "m2x");
            assertPackageElementExists("impl.conflict.clazz", "m1x");
            assertPackageElementExists("impl2", "m2x");
            assertPackageElementExists("nested.pack.pack", "m1x");
            assertPackageElementExists("nested.pack", "m2x");
            assertTypeElementExists("unique.nested.Impl", "m1x");
            assertTypeElementNotFound("impl.conflict.module.Impl");
            assertTypeElementNotFound("impl.conflict.module.Impl"); //check that the warning/note is produced only once
            assertPackageElementNotFound("impl.conflict.module");
            assertTypeElementExists("impl.conflict.src.Impl", processingEnv.getOptions().containsKey("unnamedmodule") ? "" : "m1x");
            assertPackageElementExists("impl.conflict.src", processingEnv.getOptions().containsKey("unnamedmodule") ? "" : "m1x");
            assertTypeElementNotFound("impl.conflict.clazz.pkg");
            assertPackageElementNotFound("unique"); //do not return packages without members in module mode
            assertTypeElementNotFound("nested"); //cannot distinguish between m1x and m2x

            return false;
        }

        private void assertTypeElementExists(String name, String expectedModule) {
            assertElementExists(name, "class", processingEnv.getElementUtils() :: getTypeElement, expectedModule);
        }

        private void assertPackageElementExists(String name, String expectedModule) {
            assertElementExists(name, "package", processingEnv.getElementUtils() :: getPackageElement, expectedModule);
        }

        private void assertElementExists(String name, String type, Function<String, Element> getter, String expectedModule) {
            Element clazz = getter.apply(name);

            if (clazz == null) {
                throw new AssertionError("No " + name + " " + type + " found.");
            }

            ModuleElement mod = processingEnv.getElementUtils().getModuleOf(clazz);

            if (!mod.getQualifiedName().contentEquals(expectedModule)) {
                throw new AssertionError(name + " found in an unexpected module: " + mod.getQualifiedName());
            }
        }

        private void assertTypeElementNotFound(String name) {
            assertElementNotFound(name, processingEnv.getElementUtils() :: getTypeElement);
        }

        private void assertPackageElementNotFound(String name) {
            assertElementNotFound(name, processingEnv.getElementUtils() :: getPackageElement);
        }

        private void assertElementNotFound(String name, Function<String, Element> getter) {
            Element found = getter.apply(name);

            if (found != null) {
                fail("Element found unexpectedly: " + found);
            }
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latest();
        }

    }

    @SupportedAnnotationTypes("*")
    public static final class UnboundLookup8 extends AbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            if (processingEnv.getElementUtils().getTypeElement("impl.conflict.src.Impl") == null) {
                throw new AssertionError("impl.conflict.src.Impl.");
            }

            if (processingEnv.getElementUtils().getModuleElement("java.base") != null) {
                throw new AssertionError("getModuleElement != null for -source 8");
            }

            return false;
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latest();
        }

    }

    @SupportedAnnotationTypes("*")
    public static final class UnboundLookupGenerate extends AbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            if (processingEnv.getElementUtils().getTypeElement("nue.Nue") == null) {
                try (Writer w = processingEnv.getFiler().createSourceFile("m1x/nue.Nue").openWriter()) {
                    w.write("package nue; public class Nue {}");
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            }

            return false;
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latest();
        }

    }

    @Test
    public void testWrongDefaultTargetModule(Path base) throws Exception {
        Path src = base.resolve("src");

        tb.writeJavaFiles(src,
                          "package test; public class Test { }");

        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        List<String> log = new JavacTask(tb)
            .options("--default-module-for-created-files=m!",
                     "-XDrawDiagnostics")
            .outdir(classes)
            .files(findJavaFiles(src))
            .run(Task.Expect.FAIL)
            .writeAll()
            .getOutputLines(OutputKind.DIRECT);

        List<String> expected = Arrays.asList(
            "- compiler.err.bad.name.for.option: --default-module-for-created-files, m!"
        );

        if (!log.equals(expected)) {
            throw new AssertionError("Expected output not found.");
        }
    }

    @Test
    public void testClassPhantomPackageClash(Path base) throws Exception {
        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        Path src = base.resolve("src");

        tb.writeJavaFiles(src,
                          "module m {}",
                          "package api; public class Nested {}",
                          "package api.nested; public class C {}");

        class TestFileManager extends ForwardingJavaFileManager<JavaFileManager>
                              implements StandardJavaFileManager {

            public TestFileManager(StandardJavaFileManager fileManager) {
                super(fileManager);
            }

            @Override
            public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse) throws IOException {
                if ("api.Nested".equals(packageName)) {
                    //simulate case insensitive filesystem:
                    packageName = "api.nested";
                }
                return super.list(location, packageName, kinds, recurse);
            }

            @Override
            public void setLocationFromPaths(Location location, Collection<? extends Path> paths) throws IOException {
                ((StandardJavaFileManager) fileManager).setLocationFromPaths(location, paths);
            }

            @Override
            public Iterable<? extends Path> getLocationAsPaths(Location location) {
                return ((StandardJavaFileManager) fileManager).getLocationAsPaths(location);
            }

            @Override
            public Iterable<? extends JavaFileObject> getJavaFileObjectsFromFiles(Iterable<? extends File> files) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Iterable<? extends JavaFileObject> getJavaFileObjects(File... files) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Iterable<? extends JavaFileObject> getJavaFileObjectsFromStrings(Iterable<String> names) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Iterable<? extends JavaFileObject> getJavaFileObjects(String... names) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void setLocation(Location location, Iterable<? extends File> files) throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public Iterable<? extends File> getLocation(Location location) {
                throw new UnsupportedOperationException();
            }
        }

        try (StandardJavaFileManager fm = ToolProvider.getSystemJavaCompiler().getStandardFileManager(null, null, null);
             JavaFileManager testFM = new TestFileManager(fm)) {
            new JavacTask(tb)
                .fileManager(testFM)
                .options("-processor", NoOpTestAP.class.getName(),
                         "-sourcepath", src.toString())
                .outdir(classes)
                .files(src.resolve("module-info.java"),
                       src.resolve("api").resolve("Nested.java"))
                .run()
                .writeAll()
                .getOutputLines(OutputKind.STDERR);
        }
    }

    @SupportedAnnotationTypes("*")
    public static final class NoOpTestAP extends AbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            return false;
        }

    }

    private static void assertNonNull(String msg, Object val) {
        if (val == null) {
            throw new AssertionError(msg);
        }
    }

    private static void assertNull(String msg, Object val) {
        if (val != null) {
            throw new AssertionError(msg);
        }
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("expected: " + expected + "; actual=" + actual);
        }
    }

    private static void assertFileExists(Path base, String... pathElements) {
        Path file = resolveFile(base, pathElements);

        if (!Files.exists(file)) {
            throw new AssertionError("Expected file: " + file + " exist, but it does not.");
        }
    }

    private static void assertFileNotExists(Path base, String... pathElements) {
        Path file = resolveFile(base, pathElements);

        if (Files.exists(file)) {
            throw new AssertionError("Expected file: " + file + " exist, but it does not.");
        }
    }

    static Path resolveFile(Path base, String... pathElements) {
        Path file = base;

        for (String el : pathElements) {
            file = file.resolve(el);
        }

        return file;
    }

    private static void fail(String msg) {
        throw new AssertionError(msg);
    }

}
