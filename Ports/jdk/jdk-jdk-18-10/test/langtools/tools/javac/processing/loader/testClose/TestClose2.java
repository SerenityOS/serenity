/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7092965
 * @summary javac should not close processorClassLoader before end of compilation
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 */

import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.util.Context;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

@SupportedAnnotationTypes("*")
public class TestClose2 extends AbstractProcessor implements TaskListener {

    public static void main(String... args) throws Exception {
        new TestClose2().run();
    }

    void run() throws IOException {
        File testSrc = new File(System.getProperty("test.src"));
        File testClasses = new File(System.getProperty("test.classes"));

        JavacTool tool = (JavacTool) ToolProvider.getSystemJavaCompiler();
        final ClassLoader cl = getClass().getClassLoader();
        Context c = new Context();
        StandardJavaFileManager fm = new JavacFileManager(c, true, null) {
            @Override
            protected ClassLoader getClassLoader(URL[] urls) {
                return new URLClassLoader(urls, cl) {
                    @Override
                    public void close() throws IOException {
                        System.err.println(getClass().getName() + " closing");
                        TestClose2.this.closedCount++;
                        TestClose2.this.closedIsLast = true;
                        super.close();
                    }
                };
            }
        };

        fm.setLocation(StandardLocation.CLASS_OUTPUT,
                Collections.singleton(new File(".")));
        fm.setLocation(StandardLocation.ANNOTATION_PROCESSOR_PATH,
                Collections.singleton(testClasses));
        Iterable<? extends JavaFileObject> files =
                fm.getJavaFileObjects(new File(testSrc, TestClose2.class.getName() + ".java"));
        List<String> options = Arrays.asList(
                "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
                "--add-exports", "jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
                "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
                "-processor", TestClose2.class.getName());

        JavacTask task = tool.getTask(null, fm, null, options, null, files);
        task.setTaskListener(this);

        if (!task.call())
            throw new Error("compilation failed");

        if (closedCount == 0)
            throw new Error("no closing message");
        else if (closedCount > 1)
            throw new Error(closedCount + " closed messages");

        if (!closedIsLast)
            throw new Error("closing message not last");
    }

    // AbstractProcessor methods

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Messager messager = processingEnv.getMessager();
        messager.printMessage(Diagnostic.Kind.NOTE, "processing");
        return true;
    }

    // TaskListener methods

    @Override
    public void started(TaskEvent e) {
        System.err.println("Started: " + e);
        closedIsLast = false;
    }

    @Override
    public void finished(TaskEvent e) {
        System.err.println("Finished: " + e);
        closedIsLast = false;
    }

    //

    int closedCount = 0;
    boolean closedIsLast = false;
}
