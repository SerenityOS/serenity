/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8227046
 * @summary Test basic modeling for sealed classes
 * @library /tools/lib /tools/javac/lib
 * @modules
 *     jdk.compiler/com.sun.tools.javac.api
 *     jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask JavacTestingAbstractProcessor
 * @run main TestSealed
 */

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;
import java.time.*;

import javax.tools.Diagnostic.Kind;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.Task.Mode;
import toolbox.Task.OutputKind;
import toolbox.TestRunner;
import toolbox.ToolBox;

public class TestSealed extends TestRunner {

    protected ToolBox tb;

    TestSealed() {
        super(System.err);
        tb = new ToolBox();
    }

    public static void main(String... args) throws Exception {
        new TestSealed().runTests();
    }

    /**
     * Run all methods annotated with @Test, and throw an exception if any
     * errors are reported..
     *
     * @throws Exception if any errors occurred
     */
    protected void runTests() throws Exception {
        runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    Path[] findJavaFiles(Path... paths) throws IOException {
        return tb.findJavaFiles(paths);
    }

    void checkOutputContains(String log, String... expect) throws Exception {
        for (String e : expect) {
            if (!log.contains(e)) {
                throw new Exception("expected output not found: " + e);
            }
        }
    }

    @Test
    public void testSealedClassesProcessor(Path base) throws Exception {
        Path src = base.resolve("src");
        Path r = src.resolve("Test");

        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        tb.writeJavaFiles(r,
            """
            sealed interface SealedInterface permits NonSealedClass1, SealedClass {}

            non-sealed class NonSealedClass1 implements SealedInterface {}

            sealed class SealedClass implements SealedInterface {}
                final class FinalClass extends SealedClass {}
                non-sealed class NonSealedClass2 extends SealedClass {}

            class ClassOutOfSealedHierarchy extends NonSealedClass1 {}
            """
        );

        List<String> expected = List.of(
                "- compiler.note.proc.messager: visiting: SealedInterface Modifiers: [abstract, sealed]",
                "- compiler.note.proc.messager:     this class has: 2, permitted subclasses",
                "- compiler.note.proc.messager:     permitted subclass: NonSealedClass1",
                "- compiler.note.proc.messager:     permitted subclass: SealedClass",
                "- compiler.note.proc.messager: visiting: NonSealedClass1 Modifiers: [non-sealed]",
                "- compiler.note.proc.messager:     this class has: 0, permitted subclasses",
                "- compiler.note.proc.messager: visiting: SealedClass Modifiers: [sealed]",
                "- compiler.note.proc.messager:     this class has: 2, permitted subclasses",
                "- compiler.note.proc.messager:     permitted subclass: FinalClass",
                "- compiler.note.proc.messager:     permitted subclass: NonSealedClass2",
                "- compiler.note.proc.messager: visiting: FinalClass Modifiers: [final]",
                "- compiler.note.proc.messager:     this class has: 0, permitted subclasses",
                "- compiler.note.proc.messager: visiting: NonSealedClass2 Modifiers: [non-sealed]",
                "- compiler.note.proc.messager:     this class has: 0, permitted subclasses",
                "- compiler.note.proc.messager: visiting: ClassOutOfSealedHierarchy Modifiers: []",
                "- compiler.note.proc.messager:     this class has: 0, permitted subclasses"
        );

        for (Mode mode : new Mode[] {Mode.API}) {
            List<String> log = new JavacTask(tb, mode)
                    .options("-processor", SealedClassesProcessor.class.getName(),
                            "-XDrawDiagnostics")
                    .files(findJavaFiles(src))
                    .outdir(classes)
                    .run()
                    .writeAll()
                    .getOutputLines(Task.OutputKind.DIRECT);

            System.out.println("log:" +log);

            if (!expected.equals(log)) {
                if (expected.size() == log.size()) {
                    for (int i = 0; i < expected.size(); i++) {
                        if (!expected.get(i).equals(log.get(i))) {
                            System.err.println("failing at line " + (i + 1));
                            System.err.println("    expecting " + expected.get(i));
                            System.err.println("    found " + log.get(i));
                        }
                    }
                } else {
                    System.err.println("expected and log lists differ in length");
                }
                throw new AssertionError("Unexpected output: " + log);
            }
        }
    }

    public static final class SealedClassesProcessor extends JavacTestingAbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            if (!roundEnv.processingOver()) {
                Messager messager = processingEnv.getMessager();
                ElementScanner scanner = new SealedScanner(messager);
                for(Element rootElement : roundEnv.getRootElements()) {
                    scanner.visit(rootElement);
                }
            }
            return true;
        }

        class SealedScanner extends ElementScanner<Void, Void> {

            Messager messager;

            public SealedScanner(Messager messager) {
                this.messager = messager;
            }

            @Override
            public Void visitType(TypeElement element, Void p) {
                messager.printMessage(Kind.NOTE, "visiting: " + element.getSimpleName() + " Modifiers: " + element.getModifiers());
                List<? extends TypeMirror> permittedSubclasses = element.getPermittedSubclasses();
                messager.printMessage(Kind.NOTE, String.format("    this class has: %d, permitted subclasses", permittedSubclasses.size()));
                for (TypeMirror tm: permittedSubclasses) {
                    messager.printMessage(Kind.NOTE, String.format("    permitted subclass: %s", ((DeclaredType)tm).asElement().getSimpleName()));
                }
                return super.visitType(element, p);
            }
        }
    }
}
