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
 * @run main/othervm TestRecord
 */

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import java.io.*;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;
import java.time.*;
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

import java.lang.annotation.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.tools.Diagnostic.Kind;

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

public class TestRecord extends TestRunner {
    protected ToolBox tb;

    TestRecord() {
        super(System.err);
        tb = new ToolBox();
    }

    public static void main(String... args) throws Exception {
        System.out.println(System.getProperties());
        new TestRecord().runTests();
    }

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

    static final String SOURCE =
            """
            import java.io.Serializable;
            import java.time.*;

            record PersonalBest(Duration marathonTime) implements Serializable {
                    private static final Duration MIN_QUAL_TIME = Duration.ofHours(3);
                public boolean bostonQualified() {
                    return marathonTime.compareTo(MIN_QUAL_TIME) <= 0;
                }
            }
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

    public static final class Processor extends JavacTestingAbstractProcessor {
        int recordCount = 0;

        public boolean process(Set<? extends TypeElement> annotations,
                               RoundEnvironment roundEnv) {
            if (!roundEnv.processingOver()) {
                ElementScanner scanner = new RecordScanner();
                for(Element rootElement : roundEnv.getRootElements()) {
                    scanner.visit(rootElement);
                }

                if (recordCount != 1)
                    throw new RuntimeException("Bad record count " +
                            recordCount);
            }
            return true;
        }

        /**
         * Verify that a record modeled as an element behaves as expected
         * under 6 and latest specific visitors.
         */
        private static void testRecord(Element element, Elements elements) {
            ElementVisitor visitor6 = new ElementKindVisitor6<Void, Void>() {};

            try {
                visitor6.visit(element);
                throw new RuntimeException("Expected UnknownElementException not thrown.");
            } catch (UnknownElementException uee) {
                ; // Expected.
            }

            ElementKindVisitor visitorLatest =
                    new ElementKindVisitor<Object, Void>() {
                        @Override
                        public Object visitTypeAsRecord(TypeElement e,
                                                        Void p) {
                            System.out.println("printing record " + e);
                            List<? extends Element> enclosedElements = e.getEnclosedElements();
                            for (Element elem : enclosedElements) {
                                if (elem.getKind() == ElementKind.RECORD_COMPONENT)
                                    continue; // "marathonTime" as a record component is Origin.EXPLICIT
                                System.out.println("name " + elem.getSimpleName());
                                System.out.println("origin " + elements.getOrigin(elem));
                                String simpleName = elem.getSimpleName().toString();
                                switch (simpleName) {
                                    case "<init>":
                                    case "readResolve":
                                        if (elements.getOrigin(elem) != Elements.Origin.MANDATED) {
                                            throw new RuntimeException("MANDATED origin expected for " + simpleName);
                                        }
                                        break;
                                    default:
                                        break;
                                }
                            }
                            return e; // a non-null value
                        }
                    };

            if (visitorLatest.visit(element) == null) {
                throw new RuntimeException("Null result of record visitation.");
            }
        }

        class RecordScanner extends ElementScanner<Void, Void> {

            public RecordScanner() {
                super();
            }

            @Override
            public Void visitType(TypeElement element, Void p) {
                System.out.println("Name: " + element.getSimpleName() +
                        "\tKind: " + element.getKind());
                if (element.getKind() == ElementKind.RECORD) {
                    testRecord(element, elements);
                    recordCount++;
                }
                return super.visitType(element, p);
            }
        }
    }
}
