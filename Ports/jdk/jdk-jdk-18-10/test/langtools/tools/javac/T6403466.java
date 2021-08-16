/*
 * Copyright (c) 2006, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6403466
 * @summary javac TaskListener should be informed when annotation processing occurs
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 */

import com.sun.source.util.*;
import java.io.*;
import java.lang.annotation.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;
import javax.tools.*;
import com.sun.tools.javac.api.JavacTool;

@Wrap
@SupportedAnnotationTypes("Wrap")
public class T6403466 extends AbstractProcessor {

    static final String testSrcDir = System.getProperty("test.src");
    static final String testClassDir = System.getProperty("test.classes");
    static final String self = T6403466.class.getName();
    static PrintWriter out = new PrintWriter(System.err, true);

    public static void main(String[] args) throws IOException {
        JavacTool tool = JavacTool.create();

        try (StandardJavaFileManager fm = tool.getStandardFileManager(null, null, null)) {
            Iterable<? extends JavaFileObject> files =
                fm.getJavaFileObjectsFromFiles(Arrays.asList(new File(testSrcDir, self + ".java")));

            Iterable<String> options = Arrays.asList(
                "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
                "--processor-path", testClassDir,
                "-processor", self,
                "-s", ".",
                "-d", ".");
            JavacTask task = tool.getTask(out, fm, null, options, null, files);

            VerifyingTaskListener vtl = new VerifyingTaskListener(new File(testSrcDir, self + ".out"));
            task.setTaskListener(vtl);

            if (!task.call())
                throw new AssertionError("compilation failed");

            if (vtl.iter.hasNext() || vtl.errors)
                throw new AssertionError("comparison against golden file failed.");
        }
    }

    public boolean process(Set<? extends TypeElement> annos, RoundEnvironment rEnv) {
        if (!rEnv.processingOver()) {
            Filer filer = processingEnv.getFiler();
            for (TypeElement anno: annos) {
                Set<? extends Element> elts = rEnv.getElementsAnnotatedWith(anno);
                System.err.println("anno: " + anno);
                System.err.println("elts: " + elts);
                for (TypeElement te: ElementFilter.typesIn(elts)) {
                    try {
                        Writer out = filer.createSourceFile(te.getSimpleName() + "Wrapper").openWriter();
                        out.write("class " + te.getSimpleName() + "Wrapper { }");
                        out.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }

            }
        }
        return true;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }
}

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
@interface Wrap {
}


class VerifyingTaskListener implements TaskListener {
    VerifyingTaskListener(File ref) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(ref));
        String line;
        List<String> lines = new ArrayList<String>();
        while ((line = in.readLine()) != null)
            lines.add(line);
        in.close();
        iter = lines.iterator();
    }

    public void started(TaskEvent e) {
        check("Started " + toString(e));
    }
    public void finished(TaskEvent e) {
        check("Finished " + toString(e));
    }

    // similar to TaskEvent.toString(), but just prints basename of the file
    private String toString(TaskEvent e) {
        JavaFileObject file = e.getSourceFile();
        return "TaskEvent["
            + e.getKind() + ","
            + (file == null ? null : new File(file.toUri().getPath()).getName()) + ","
            // the compilation unit is identified by the file
            + e.getTypeElement() + "]";
    }

    private void check(String s) {
        System.out.println(s); // write a reference copy of expected output to stdout

        String ref = iter.hasNext() ? iter.next() : null;
        line++;
        if (!s.equals(ref)) {
            if (ref != null)
                System.err.println(line + ": expected: " + ref);
            System.err.println(line + ":    found: " + s);
            errors = true;
        }
    }

    Iterator<String> iter;
    int line;
    boolean errors;
}
