/*
 * Copyright (c) 2018, Google LLC. All rights reserved.
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
 * @bug 8204630
 * @summary generating an anonymous class with Filer#createClassFile causes an NPE in
 * JavacProcessingEnvironment
 * @library /tools/lib /tools/javac/lib/
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.processing
 *          jdk.compiler/com.sun.tools.javac.util
 *          jdk.jdeps/com.sun.tools.javap
 * @clean *
 * @build toolbox.ToolBox toolbox.JavacTask
 * @build GenerateAnonymousClass JavacTestingAbstractProcessor
 * @compile/ref=GenerateAnonymousClass.out -XDaccessInternalAPI -processor GenerateAnonymousClass -XDrawDiagnostics GenerateAnonymousClass.java
 */

import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.processing.PrintingProcessor.PrintingElementVisitor;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Log.WriterKind;
import java.io.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.tools.*;
import toolbox.JavacTask;
import toolbox.ToolBox;

public class GenerateAnonymousClass extends JavacTestingAbstractProcessor {
    int round = 1;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Log log = Log.instance(((JavacProcessingEnvironment) processingEnv).getContext());
        PrintWriter pw = log.getWriter(WriterKind.NOTICE);

        pw.println("round: " + round);

        TypeElement generatedClass = processingEnv.getElementUtils().getTypeElement("T");
        if (generatedClass != null) {
            new PrintingElementVisitor(pw, processingEnv.getElementUtils()).visit(generatedClass);
            pw.flush();
        }

        if (round++ == 1) {
            ToolBox tb = new ToolBox();
            ToolBox.MemoryFileManager mfm = new ToolBox.MemoryFileManager();
            new JavacTask(tb).fileManager(mfm).sources(GENERATED).run();

            try (OutputStream out = filer.createClassFile("T").openOutputStream()) {
                out.write(mfm.getFileBytes(StandardLocation.CLASS_OUTPUT, "T"));
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.toString());
            }
            try (OutputStream out = filer.createClassFile("T$1").openOutputStream()) {
                out.write(mfm.getFileBytes(StandardLocation.CLASS_OUTPUT, "T$1"));
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.toString());
            }
        }

        return false;
    }

    private static final String GENERATED =
            "public class T {\n"
                    + "    public void test() {\n"
                    + "        new Object() {};\n"
                    + "    }\n"
                    + "}";
}
