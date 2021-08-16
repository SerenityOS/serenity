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
 * @bug 8195986
 * @summary Verify the type of String does not change across annotation processing rounds
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask toolbox.TestRunner
 * @run main ValidTypesAreKept
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.TestRunner;
import toolbox.ToolBox;

public class ValidTypesAreKept extends TestRunner {

    private ToolBox tb;

    public ValidTypesAreKept() {
        super(System.err);
        tb = new ToolBox();
    }

    public static void main(String... args) throws Exception {
        new ValidTypesAreKept().runTests();
    }

    @Test
    public void testBrokenErrors() throws Exception {
        Path base = Paths.get(".");
        Path src = base.resolve("src");
        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        tb.writeJavaFiles(src,
                          "package t; @A(B.E) public @interface A { B value(); }",
                          "package t; public enum B { E; }");

        new JavacTask(tb)
                .outdir(classes)
                .files(tb.findJavaFiles(src))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        Files.delete(classes.resolve("t").resolve("B.class"));

        Path src2 = base.resolve("src2");
        Path classes2 = base.resolve("classes2");

        Files.createDirectories(classes2);

        Path output2 = base.resolve("output2");

        Files.createDirectories(output2);

        tb.writeJavaFiles(src2,
                          "package t2; @t.A class C { public native void t(String str); }");

        new JavacTask(tb)
                .classpath(classes)
                .options("-processor", AP.class.getName(),
                         "--processor-path", System.getProperty("test.classes"),
                         "-h", output2.toString())
                .outdir(classes2)
                .files(tb.findJavaFiles(src2))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        String content = new String(Files.readAllBytes(output2.resolve("t2_C.h")));

        if (!content.contains("(JNIEnv *, jobject, jstring)")) {
            fail("incorrect header file content: " + content);
        }
    }

    @SupportedAnnotationTypes("*")
    public static final class AP extends AbstractProcessor {

        private TypeMirror stringType;

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            TypeElement str = processingEnv.getElementUtils().getTypeElement("java.lang.String");
            if (str == null) {
                throw new AssertionError("java.lang.String");
            }
            if (stringType == null) {
                stringType = str.asType();
            } else {
                if (stringType != str.asType()) {
                    throw new AssertionError("The type of j.l.String changed.");
                }
            }

            return false;
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latest();
        }

    }

    private static void assertNonNull(String msg, Object val) {
        if (val == null) {
            throw new AssertionError(msg);
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
