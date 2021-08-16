/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @summary tests for single module mode compilation
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask ModuleTestBase
 * @run main SingleModuleModeTest
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.ToolBox;

public class SingleModuleModeTest extends ModuleTestBase{

    public static void main(String... args) throws Exception {
        new SingleModuleModeTest().run();
    }

    void run() throws Exception {
        tb = new ToolBox();

        runTests();
    }

    @Test
    public void testTooManyModules(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src.resolve("m1x"), "module m1x { }");
        tb.writeJavaFiles(src.resolve("m2x"), "module m2x { }");

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("module-info.java:1:1: compiler.err.too.many.modules"))
            throw new Exception("expected output not found");
    }

    @Test
    public void testImplicitModuleSource(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                "module m { }",
                "class C { }");

        new JavacTask(tb)
                .classpath(src)
                .files(src.resolve("C.java"))
                .run()
                .writeAll();
    }

    @Test
    public void testImplicitModuleClass(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                "module m { }",
                "class C { }");
        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        new JavacTask(tb)
                .outdir(classes)
                .files(src.resolve("module-info.java"))
                .run()
                .writeAll();

        new JavacTask(tb)
                .classpath(classes)
                .files(src.resolve("C.java"))
                .run()
                .writeAll();
    }

    @Test
    public void testImplicitModuleClassAP(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                "module m { uses java.lang.Runnable; }",
                "class C { }");
        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        new JavacTask(tb)
                .outdir(classes)
                .files(src.resolve("module-info.java"))
                .run()
                .writeAll();

        new JavacTask(tb)
                .options("-processor", VerifyUsesProvides.class.getName(),
                         "--processor-path", System.getProperty("test.classes"))
                .outdir(classes)
                .classpath(classes)
                .files(src.resolve("C.java"))
                .run()
                .writeAll();
    }

    @Test
    public void testImplicitModuleSourceAP(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                "module m { uses java.lang.Runnable; }",
                "class C { }");
        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        new JavacTask(tb)
                .options("-processor", VerifyUsesProvides.class.getName(),
                         "--processor-path", System.getProperty("test.classes"))
                .outdir(classes)
                .sourcepath(src)
                .classpath(classes)
                .files(src.resolve("C.java"))
                .run()
                .writeAll();
    }

    @SupportedAnnotationTypes("*")
    public static final class VerifyUsesProvides extends AbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            if (processingEnv.getElementUtils().getModuleElement("m") == null) {
                throw new AssertionError();
            }

            return false;
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latest();
        }

    }
}
