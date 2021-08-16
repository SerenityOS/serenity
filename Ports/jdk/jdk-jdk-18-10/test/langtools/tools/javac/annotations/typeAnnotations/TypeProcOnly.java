/*
 * Copyright (c) 2009, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.ElementFilter;

import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;

import static com.sun.tools.javac.comp.CompileStates.CompileState;

/*
 * @test
 * @summary test that type processors are run when -proc:only is passed.
 * This class implements the functionality of a type processor, as previously
 * embodied by the AbstractTypeProcessor class.
 *
 * @author Mahmood Ali
 * @author Werner Dietl
 * @modules jdk.compiler/com.sun.tools.javac.comp
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.processing
 *          jdk.compiler/com.sun.tools.javac.util
 */
@SupportedAnnotationTypes("*")
public class TypeProcOnly extends AbstractProcessor {
    private static final String INDICATOR = "INDICATOR";

    private final AttributionTaskListener listener = new AttributionTaskListener();
    private final Set<Name> elements = new HashSet<Name>();

    @Override
    public final void init(ProcessingEnvironment env) {
        super.init(env);
        JavacTask.instance(env).addTaskListener(listener);
        Context ctx = ((JavacProcessingEnvironment)processingEnv).getContext();
        JavaCompiler compiler = JavaCompiler.instance(ctx);
        compiler.shouldStopPolicyIfNoError = CompileState.max(
                compiler.shouldStopPolicyIfNoError,
                CompileState.FLOW);
    }

    @Override
    public final boolean process(Set<? extends TypeElement> annotations,
            RoundEnvironment roundEnv) {
        for (TypeElement elem : ElementFilter.typesIn(roundEnv.getRootElements())) {
            elements.add(elem.getQualifiedName());
        }
        return false;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    private final class AttributionTaskListener implements TaskListener {
        @Override
        public void started(TaskEvent e) { }

        @Override
        public void finished(TaskEvent e) {
            if (e.getKind() != TaskEvent.Kind.ANALYZE)
                return;

            if (!elements.remove(e.getTypeElement().getQualifiedName()))
                return;

            System.out.println(INDICATOR);
        }
    }


    private static File writeTestFile() throws IOException {
        File f = new File("Test.java");
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f)));
        out.println("class Test { }");
        out.close();
        return f;
    }

    public static void main(String[] args) throws Exception {
        PrintStream prevOut = System.out;

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bytes);
        System.setOut(out);

        try {
            File f = writeTestFile();
            com.sun.tools.javac.Main.compile(new String[] {"-XDaccessInternalAPI", "-proc:only", "-processor", "TypeProcOnly", f.getAbsolutePath()});
        } finally {
            System.setOut(prevOut);
        }

        if (bytes.toString().trim().equals(INDICATOR)) {
            System.out.println("PASSED");
        } else {
            throw new Exception("Processor did not run correctly. Output: " + bytes);
        }
    }
}
