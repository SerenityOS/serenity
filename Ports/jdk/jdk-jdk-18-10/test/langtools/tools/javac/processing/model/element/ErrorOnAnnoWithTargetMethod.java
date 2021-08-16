/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8239447
 * @summary Verify that annotation processing works for records
 * @library /tools/lib /tools/javac/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 *      jdk.compiler/com.sun.tools.javac.code
 *      jdk.compiler/com.sun.tools.javac.util
 * @build toolbox.ToolBox toolbox.JavacTask
 * @build JavacTestingAbstractProcessor
 * @run main/othervm ErrorOnAnnoWithTargetMethod
 */

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

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

/** this test is just provoking method Round::newRound to execute, this method has a visitor that,
 *  when visiting annotation ASTs, it sets theirs attribute field to null. Annotations attached to
 *  record components were not being visited. This provoked that some information from previous
 *  rounds were still accessible to the current annotation processing round provoking validation
 *  errors later on.
 */
public class ErrorOnAnnoWithTargetMethod extends TestRunner {
    protected ToolBox tb;

    ErrorOnAnnoWithTargetMethod() {
        super(System.err);
        tb = new ToolBox();
    }

    public static void main(String... args) throws Exception {
        System.out.println(System.getProperties());
        new ErrorOnAnnoWithTargetMethod().runTests();
    }

    protected void runTests() throws Exception {
        runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    Path[] findJavaFiles(Path... paths) throws IOException {
        return tb.findJavaFiles(paths);
    }

    static final String SOURCE =
        """
        import java.lang.annotation.ElementType;
        import java.lang.annotation.Retention;
        import java.lang.annotation.RetentionPolicy;
        import java.lang.annotation.Target;

        @Retention(RetentionPolicy.RUNTIME)
        #ANNO
        @interface MyAnn {
            int value() default 1;
        }

        record MyRecord(@MyAnn(value=5) int recComponent){}
        """;

    String[] annos = new String[] {
            "@Target({ElementType.RECORD_COMPONENT})",
            "@Target({ElementType.TYPE_USE})",
            "@Target({ElementType.PARAMETER})",
            "@Target({ElementType.METHOD})",
            "@Target({ElementType.FIELD})",
            "@Target({ElementType.RECORD_COMPONENT, ElementType.TYPE_USE})",
            "@Target({ElementType.RECORD_COMPONENT, ElementType.PARAMETER})",
            "@Target({ElementType.RECORD_COMPONENT, ElementType.METHOD})",
            "@Target({ElementType.RECORD_COMPONENT, ElementType.FIELD})",
            "@Target({ElementType.RECORD_COMPONENT, ElementType.TYPE_USE, ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})",
    };

    @Test
    public void testAnnoProcessing(Path base) throws Exception {
        Path src = base.resolve("src");
        Path r = src.resolve("Records");

        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        for (String anno : annos) {
            String source = SOURCE.replaceFirst("#ANNO", anno);
            tb.writeJavaFiles(r, source);
            new JavacTask(tb, Mode.API)
                    .options("-nowarn", "-processor", Processor.class.getName())
                    .files(findJavaFiles(src))
                    .outdir(classes)
                    .run()
                    .writeAll()
                    .getOutputLines(Task.OutputKind.DIRECT);
        }
    }

    public static final class Processor extends JavacTestingAbstractProcessor {
        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            return true;
        }
    }
}
