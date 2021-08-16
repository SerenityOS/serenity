/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7196760
 * @summary javac doesn't report Diagnostic end positions properly when
 * an annotation processor is present
 * @modules jdk.compiler
 */

import com.sun.source.util.JavacTask;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import static javax.tools.JavaFileObject.Kind.SOURCE;
import javax.tools.ToolProvider;

@SupportedAnnotationTypes("*")
public class EndPositions extends AbstractProcessor {
    public static void main(String... args) throws IOException {
        class MyFileObject extends SimpleJavaFileObject {
            MyFileObject() {
                super(URI.create("myfo:///Test.java"), SOURCE);
            }
            @Override
            public String getCharContent(boolean ignoreEncodingErrors) {
                //      0         1         2         3
                //      012345678901234567890123456789012345
                return "class Test { String s = 1234; }";
            }
        }
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        List<JavaFileObject> compilationUnits =
                Collections.<JavaFileObject>singletonList(new MyFileObject());
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        List<String> options = Arrays.asList("-processor", EndPositions.class.getCanonicalName());
        JavacTask task = (JavacTask)javac.getTask(null, null, diagnostics, options, null, compilationUnits);
        boolean valid = task.call();
        if (valid)
            throw new AssertionError("Expected one error, but found none.");

        List<Diagnostic<? extends JavaFileObject>> errors = diagnostics.getDiagnostics();
        if (errors.size() != 1)
            throw new AssertionError("Expected one error only, but found " + errors.size() + "; errors: " + errors);

        Diagnostic<?> error = errors.get(0);
        if (error.getStartPosition() >= error.getEndPosition())
            throw new AssertionError("Expected start to be less than end position: start [" +
                    error.getStartPosition() + "], end [" + error.getEndPosition() +"]" +
                    "; diagnostics code: " + error.getCode());

        System.out.println("All is good!");
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        return true;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }
}
