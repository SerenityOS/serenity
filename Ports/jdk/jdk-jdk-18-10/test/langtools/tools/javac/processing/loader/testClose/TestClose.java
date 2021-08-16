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
 */

import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.ClientCodeWrapper.Trusted;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.api.JavacTool;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

/*
 * The test compiles an annotation processor and a helper class into a
 * custom classes directory.
 *
 * It then uses them while compiling a dummy file, with the custom classes
 * directory on the processor path, thus guaranteeing that references to
 * these class are satisfied by the processor class loader.
 *
 * The annotation processor uses the javac TaskListener to run code
 * after annotation processing has completed, to verify that the classloader
 * is not closed until the end of the compilation.
 */

@Trusted // avoids use of ClientCodeWrapper
public class TestClose implements TaskListener {
    public static final String annoProc =
        "import java.util.*;\n" +
        "import javax.annotation.processing.*;\n" +
        "import javax.lang.model.*;\n" +
        "import javax.lang.model.element.*;\n" +
        "import com.sun.source.util.*;\n" +
        "import com.sun.tools.javac.processing.*;\n" +
        "import com.sun.tools.javac.util.*;\n" +
        "@SupportedAnnotationTypes(\"*\")\n" +
        "public class AnnoProc extends AbstractProcessor {\n" +
        "    @Override\n" +
        "    public SourceVersion getSupportedSourceVersion() {\n" +
        "        return SourceVersion.latest();\n" +
        "    }\n" +
        "    @Override\n" +
        "    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {\n" +
        "        System.out.println(\"in AnnoProc.process\");\n" +
        "        final ClassLoader cl = getClass().getClassLoader();\n" +
        "        if (roundEnv.processingOver()) {\n" +
        "            TestClose.add(processingEnv, new Runnable() {\n" +
        "                public void run() {\n" +
        "                    System.out.println(getClass().getName() + \": run()\");\n" +
        "                    try {\n" +
        "                        cl.loadClass(\"Callback\")\n" +
        "                            .asSubclass(Runnable.class)\n" +
        "                            .newInstance()\n" +
        "                            .run();\n" +
        "                    } catch (ReflectiveOperationException e) {\n" +
        "                        throw new Error(e);\n" +
        "                    }\n" +
        "                }\n" +
        "            });\n" +
        "        }\n" +
        "        return true;\n" +
        "    }\n" +
        "}\n";

    public static final String callback =
        "public class Callback implements Runnable {\n" +
        "    public void run() {\n" +
        "        System.out.println(getClass().getName() + \": run()\");\n" +
        "    }\n" +
        "}";

    public static void main(String... args) throws Exception {
        new TestClose().run();
    }

    void run() throws IOException {
        JavacTool tool = (JavacTool) ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = tool.getStandardFileManager(null, null, null)) {

            File classes = new File("classes");
            classes.mkdirs();
            File extraClasses = new File("extraClasses");
            extraClasses.mkdirs();

            System.out.println("compiling classes to extraClasses");
            {   // setup class in extraClasses
                fm.setLocation(StandardLocation.CLASS_OUTPUT,
                        Collections.singleton(extraClasses));
                List<? extends JavaFileObject> files = Arrays.asList(
                        new MemFile("AnnoProc.java", annoProc),
                        new MemFile("Callback.java", callback));
                List<String> options = Arrays.asList(
                        "--add-exports", "jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED",
                        "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
                        "-XDaccessInternalAPI");
                JavacTask task = tool.getTask(null, fm, null, options, null, files);
                check(task.call());
            }

            System.out.println("compiling dummy to classes with anno processor");
            {   // use that class in a TaskListener after processing has completed
                PrintStream prev = System.out;
                String out;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (PrintStream ps = new PrintStream(baos)) {
                    System.setOut(ps);
                    File testClasses = new File(System.getProperty("test.classes"));
                    fm.setLocation(StandardLocation.CLASS_OUTPUT,
                            Collections.singleton(classes));
                    fm.setLocation(StandardLocation.ANNOTATION_PROCESSOR_PATH,
                            Arrays.asList(extraClasses, testClasses));
                    List<? extends JavaFileObject> files = Arrays.asList(
                            new MemFile("my://dummy", "class Dummy { }"));
                    List<String> options = Arrays.asList("-XDaccessInternalAPI", "-processor", "AnnoProc");
                    JavacTask task = tool.getTask(null, fm, null, options, null, files);
                    task.setTaskListener(this);
                    check(task.call());
                } finally {
                    System.setOut(prev);
                    out = baos.toString();
                    if (!out.isEmpty())
                        System.out.println(out);
                }
                check(out.contains("AnnoProc$1: run()"));
                check(out.contains("Callback: run()"));
            }
        }
    }

    @Override
    public void started(TaskEvent e) {
        System.out.println("Started: " + e);
    }

    @Override
    public void finished(TaskEvent e) {
        System.out.println("Finished: " + e);
        if (e.getKind() == TaskEvent.Kind.ANALYZE) {
            for (Runnable r: runnables) {
                System.out.println("running " + r);
                r.run();
            }
        }
    }

    void check(boolean b) {
        if (!b)
            throw new AssertionError();
    }

    public static void add(ProcessingEnvironment env, Runnable r) {
        // ensure this class in this class loader can access javac internals
        try {
            JavacTask task = JavacTask.instance(env);
            TaskListener l = ((BasicJavacTask) task).getTaskListeners().iterator().next();
            // The TaskListener is an instanceof TestClose, but when using the
            // default class loaders. the taskListener uses a different
            // instance of Class<TestClose> than the anno processor.
            // If you try to evaluate
            //      TestClose tc = (TestClose) (l).
            // you get the following somewhat confusing error:
            //   java.lang.ClassCastException: TestClose cannot be cast to TestClose
            // The workaround is to access the fields of TestClose with reflection.
            Field f = l.getClass().getField("runnables");
            @SuppressWarnings("unchecked")
            List<Runnable> runnables = (List<Runnable>) f.get(l);
            runnables.add(r);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public List<Runnable> runnables = new ArrayList<>();

    class MemFile extends SimpleJavaFileObject {
        public final String text;

        MemFile(String name, String text) {
            super(URI.create(name), JavaFileObject.Kind.SOURCE);
            this.text = text;
        }

        @Override
        public String getName() {
            return uri.toString();
        }

        @Override
        public String getCharContent(boolean ignoreEncodingErrors) {
            return text;
        }
    }
}
