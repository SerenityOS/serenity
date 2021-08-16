/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8168312
 * @summary javac throws NPE if annotation processor is specified and module is declared in a file named arbitrarily
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask ModuleTestBase
 * @run main AnnotationProcessingWithModuleInfoInWrongPlace
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

public class AnnotationProcessingWithModuleInfoInWrongPlace extends ModuleTestBase {

    public static void main(String... args) throws Exception {
        new AnnotationProcessingWithModuleInfoInWrongPlace().runTests();
    }

    @Test
    public void testModuleInfoInWrongPlace(Path base) throws Exception {
        Path moduleSrc = base.resolve("module-src");
        Path m = moduleSrc.resolve("m");

        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        tb.writeJavaFiles(m, "module m {}");

        Path mi = m.resolve("module-info.java");
        Path f = m.resolve("F.java");

        tb.moveFile(mi, f);

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "--module-source-path", moduleSrc.toString(),
                         "-processor", AP.class.getName())
                .outdir(classes)
                .files(findJavaFiles(moduleSrc))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);


        if (!log.contains("F.java:1:1: compiler.err.module.decl.sb.in.module-info.java"))
            throw new AssertionError("Unexpected output: " + log);
    }

    @SupportedAnnotationTypes("*")
    public static final class AP extends AbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annot, RoundEnvironment env) {
            return false;
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latest();
        }
    }
}
