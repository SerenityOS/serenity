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
 * @summary Verify that annotation processing works for records
 * @library /tools/lib /tools/javac/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 *      jdk.compiler/com.sun.tools.javac.code
 *      jdk.compiler/com.sun.tools.javac.util
 * @build toolbox.ToolBox toolbox.JavacTask
 * @build JavacTestingAbstractProcessor
 * @run main/othervm AnnoProcessorOnRecordsTest
 */

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.ElementScanner14;
import javax.tools.Diagnostic.Kind;
import javax.tools.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;

import com.sun.tools.javac.util.Assert;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.Task.Mode;
import toolbox.Task.OutputKind;
import toolbox.TestRunner;
import toolbox.ToolBox;

public class AnnoProcessorOnRecordsTest extends TestRunner {
    protected ToolBox tb;

    AnnoProcessorOnRecordsTest() {
        super(System.err);
        tb = new ToolBox();
    }

    public static void main(String... args) throws Exception {
        System.out.println(System.getProperties());
        new AnnoProcessorOnRecordsTest().runTests();
    }

    protected void runTests() throws Exception {
        runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    Path[] findJavaFiles(Path... paths) throws IOException {
        return tb.findJavaFiles(paths);
    }

    static final String SOURCE =
            """
            import java.lang.annotation.*;
            import java.util.*;
            import javax.annotation.processing.*;
            import javax.lang.model.element.*;

            @Retention(RetentionPolicy.RUNTIME)
            @Target({ ElementType.PARAMETER })
            @interface Parameter {}

            @Retention(RetentionPolicy.RUNTIME)
            @Target({ ElementType.METHOD })
            @interface Method {}

            @Retention(RetentionPolicy.RUNTIME)
            @Target({ ElementType.FIELD })
            @interface Field {}

            @Retention(RetentionPolicy.RUNTIME)
            @Target({ ElementType.RECORD_COMPONENT })
            @interface RecComponent {}

            @Retention(RetentionPolicy.RUNTIME)
            @interface All {}

            @Retention(RetentionPolicy.RUNTIME)
            @Target({ ElementType.FIELD, ElementType.RECORD_COMPONENT })
            @interface RecComponentAndField {}

            record R1(@Parameter int i) {}

            record R2(@Method int i) {}

            record R3(@Field int i) {}

            record R4(@All int i) {}

            record R5(@RecComponent int i) {}

            record R6(@RecComponentAndField int i) {}
            """;

    @Test
    public void testAnnoProcessing(Path base) throws Exception {
        Path src = base.resolve("src");
        Path r = src.resolve("Records");

        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        tb.writeJavaFiles(r, SOURCE);

        for (Mode mode : new Mode[] {Mode.API}) {
            new JavacTask(tb, mode)
                    .options("-nowarn", "-processor", Processor.class.getName())
                    .files(findJavaFiles(src))
                    .outdir(classes)
                    .run()
                    .writeAll()
                    .getOutputLines(Task.OutputKind.DIRECT);
        }
    }

    @SupportedAnnotationTypes("*")
    public static final class Processor extends JavacTestingAbstractProcessor {
        public boolean process(Set<? extends TypeElement> tes, RoundEnvironment renv) {
            for (TypeElement te : tes) {
                switch (te.toString()) {
                    case "Parameter" :
                        checkElements(te, renv, 1, Set.of(ElementKind.PARAMETER));
                        break;
                    case "Method":
                        checkElements(te, renv, 1, Set.of(ElementKind.METHOD));
                        break;
                    case "Field":
                        checkElements(te, renv, 1, Set.of(ElementKind.FIELD));
                        break;
                    case "All":
                        checkElements(te, renv, 4,
                                Set.of(ElementKind.FIELD,
                                        ElementKind.METHOD,
                                        ElementKind.PARAMETER,
                                        ElementKind.RECORD_COMPONENT));
                        break;
                    case "RecComponent":
                        checkElements(te, renv, 1, Set.of(ElementKind.RECORD_COMPONENT));
                        break;
                    case "RecComponentAndField":
                        checkElements(te, renv, 2, Set.of(ElementKind.RECORD_COMPONENT, ElementKind.FIELD));
                        break;
                    default:
                        // ignore, just another annotation like Target, we don't care about
                }
            }
            return true;
        }

        void checkElements(TypeElement te, RoundEnvironment renv, int expectedNumberOfElements, Set<ElementKind> kinds) {
            Set<? extends Element> annoElements = renv.getElementsAnnotatedWith(te);
            Assert.check(annoElements.size() == expectedNumberOfElements);
            for (Element e : annoElements) {
                Symbol s = (Symbol) e;
                Assert.check(kinds.contains(s.getKind()));
            }
        }
    }
}
