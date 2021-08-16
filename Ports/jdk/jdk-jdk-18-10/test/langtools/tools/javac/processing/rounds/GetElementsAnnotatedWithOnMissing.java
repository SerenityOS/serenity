/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8213103
 * @summary Checking getElementsAnnotatedWith works for unknown annotations and with -source 8.
 * @library /tools/lib /tools/javac/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask JavacTestingAbstractProcessor
 * @run main GetElementsAnnotatedWithOnMissing
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.TestRunner;
import toolbox.ToolBox;

public class GetElementsAnnotatedWithOnMissing extends TestRunner {

    public static void main(String... args) throws Exception {
        new GetElementsAnnotatedWithOnMissing().runTests(
                m -> new Object[] { Paths.get(m.getName()) }
        );
    }

    private ToolBox tb = new ToolBox();

    public GetElementsAnnotatedWithOnMissing() {
        super(System.err);
    }

    @Test
    public void testModuleInfoInWrongPlace(Path base) throws Exception {
        Path src = base.resolve("src");
        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        tb.writeJavaFiles(src, "package test; public class Test {}");

        new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "-source", "8",
                         "-classpath", "",
                         "-sourcepath", src.toString(),
                         "-processorpath", System.getProperty("test.class.path"),
                         "-processor", AP.class.getName())
                .outdir(classes)
                .files(tb.findJavaFiles(src))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "-classpath", "",
                         "-sourcepath", src.toString(),
                         "-processorpath", System.getProperty("test.class.path"),
                         "-processor", AP.class.getName())
                .outdir(classes)
                .files(tb.findJavaFiles(src))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);
    }

    public static final class AP extends JavacTestingAbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annot, RoundEnvironment env) {
            if (elements.getTypeElement(Ann.class.getCanonicalName()) != null) {
                throw new IllegalStateException("Can resolve @Ann!");
            }
            env.getElementsAnnotatedWith(Ann.class);
            return false;
        }

    }

    public @interface Ann {}
}
