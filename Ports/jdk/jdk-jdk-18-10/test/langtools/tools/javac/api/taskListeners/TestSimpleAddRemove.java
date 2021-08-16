/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     7093891
 * @summary support multiple task listeners
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 */

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.JavacTool;

public class TestSimpleAddRemove {
    enum AddKind {
        SET_IN_TASK,
        ADD_IN_TASK,
        ADD_IN_PROCESSOR,
        ADD_IN_LISTENER;
    }

    enum RemoveKind {
        REMOVE_IN_TASK,
        REMOVE_IN_PROCESSOR,
        REMOVE_IN_LISTENER,
    }

    enum CompileKind {
        CALL {
            void run(JavacTask t) {
                if (!t.call()) throw new Error("compilation failed");
            }
        },
        GENERATE {
            void run(JavacTask t) throws IOException {
                t.generate();
            }
        };
        abstract void run(JavacTask t) throws IOException;
    }

    static class EventKindCounter extends EnumMap<TaskEvent.Kind, EventKindCounter.Count> {
        static class Count {
            int started;
            int finished;

            @Override
            public String toString() {
                return started + ":" + finished;
            }
        }

        EventKindCounter() {
            super(TaskEvent.Kind.class);
        }

        void inc(TaskEvent.Kind k, boolean started) {
            Count c = get(k);
            if (c == null)
                put(k, c = new Count());

            if (started)
                c.started++;
            else
                c.finished++;
        }
    }

    static class TestListener implements TaskListener {
        EventKindCounter counter;

        TestListener(EventKindCounter c) {
            counter = c;
        }

        public void started(TaskEvent e) {
            counter.inc(e.getKind(), true);
        }

        public void finished(TaskEvent e) {
            counter.inc(e.getKind(), false);
        }
    }

    static void addInListener(final JavacTask task, final TaskEvent.Kind kind, final TaskListener listener) {
        task.addTaskListener(new TaskListener() {
            public void started(TaskEvent e) {
                if (e.getKind() == kind) {
                    task.addTaskListener(listener);
                    task.removeTaskListener(this);
                }
            }

            public void finished(TaskEvent e) { }
        });
    }

    static void removeInListener(final JavacTask task, final TaskEvent.Kind kind, final TaskListener listener) {
        task.addTaskListener(new TaskListener() {
            public void started(TaskEvent e) {
                if (e.getKind() == kind) {
                    task.removeTaskListener(listener);
                    task.removeTaskListener(this);
                }
            }

            public void finished(TaskEvent e) { }
        });
    }

    @SupportedAnnotationTypes("*")
    class TestProcessor extends AbstractProcessor {
        AddKind ak;
        RemoveKind rk;
        TaskListener listener;

        TestProcessor(AddKind ak, RemoveKind rk, TaskListener listener) {
            this.ak = ak;
            this.rk = rk;
            this.listener = listener;
        }

        int round = 0;

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
//            System.err.println("TestProcessor.process " + roundEnv);
            JavacTask task = JavacTask.instance(processingEnv);
            if (++round == 1) {
                switch (ak) {
                    case ADD_IN_PROCESSOR:
                        task.addTaskListener(listener);
                        break;
                    case ADD_IN_LISTENER:
                        addInListener(task, TaskEvent.Kind.ANALYZE, listener);
                        break;
                }
            } else if (roundEnv.processingOver()) {
                switch (rk) {
                    case REMOVE_IN_PROCESSOR:
                        task.removeTaskListener(listener);
                        break;
                    case REMOVE_IN_LISTENER:
                        removeInListener(task, TaskEvent.Kind.GENERATE, listener);
                        break;
                }
            }
            return true;
        }
    }

    static class TestSource extends SimpleJavaFileObject {
        public TestSource() {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return "class Test { }";
        }
    }

    public static void main(String... args) throws Exception {
        TestSimpleAddRemove t = new TestSimpleAddRemove();
        try {
            t.run();
        } finally {
            t.fm.close();
        }
    }

    JavacTool tool = (JavacTool) ToolProvider.getSystemJavaCompiler();
    StandardJavaFileManager fm = tool.getStandardFileManager(null, null, null);

    void run() throws Exception {
        for (CompileKind ck: CompileKind.values()) {
            for (AddKind ak: AddKind.values()) {
                for (RemoveKind rk: RemoveKind.values()) {
                    test(ck, ak, rk);
                }
            }
        }
        if (errors > 0)
            throw new Exception(errors + " errors occurred");
    }

    void test(CompileKind ck, AddKind ak, RemoveKind rk) throws IOException {
        System.err.println("Test: " + ck + " " + ak + " " + rk);

        File tmpDir = new File(ck + "-" + ak + "-" + rk);
        tmpDir.mkdirs();
        fm.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(tmpDir));

        List<String> options = new ArrayList<String>();
        Iterable<? extends JavaFileObject> files = Arrays.asList(new TestSource());
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        JavacTask task = tool.getTask(pw, fm, null, options, null, files);

        EventKindCounter ec = new EventKindCounter();
        TaskListener listener = new TestListener(ec);
        boolean needProcessor = false;

        switch (ak) {
            case SET_IN_TASK:
                task.setTaskListener(listener);
                break;
            case ADD_IN_TASK:
                task.addTaskListener(listener);
                break;
            case ADD_IN_PROCESSOR:
            case ADD_IN_LISTENER:
                needProcessor = true;
        }

        switch (rk) {
            case REMOVE_IN_TASK:
                task.removeTaskListener(listener);
                break;
            case REMOVE_IN_PROCESSOR:
            case REMOVE_IN_LISTENER:
                needProcessor = true;
        }

        if (needProcessor)
            task.setProcessors(Arrays.asList(new TestProcessor(ak, rk, listener)));

        ck.run(task);
        System.err.println(ec);

        check(ck, ak, rk, ec);

        System.err.println();
    }

    void check(CompileKind ck, AddKind ak, RemoveKind rk, EventKindCounter ec) {
        // All results should be independent of ck, so we can ignore that

        // Quick way to compare expected values of ec, by comparing ec.toString()
        String expect = ec.toString();
        String found;

        switch (ak) {
            // Add/set in task should record all events until the listener is removed
            case SET_IN_TASK:
            case ADD_IN_TASK:
                switch (rk) {
                    case REMOVE_IN_TASK:
                        // Remove will succeed, meaning no events will be recorded
                        found = "{}";
                        break;
                    case REMOVE_IN_PROCESSOR:
                        if (ck == CompileKind.CALL)
                            found = "{PARSE=1:1, ENTER=2:2, ANNOTATION_PROCESSING=1:0, ANNOTATION_PROCESSING_ROUND=2:1, COMPILATION=1:0}";
                        else
                            found = "{PARSE=1:1, ENTER=2:2, ANNOTATION_PROCESSING=1:0, ANNOTATION_PROCESSING_ROUND=2:1}";
                        break;
                    case REMOVE_IN_LISTENER:
                        if (ck == CompileKind.CALL)
                            found = "{PARSE=1:1, ENTER=3:3, ANALYZE=1:1, GENERATE=1:0, ANNOTATION_PROCESSING=1:1, ANNOTATION_PROCESSING_ROUND=2:2, COMPILATION=1:0}";
                        else
                            found = "{PARSE=1:1, ENTER=3:3, ANALYZE=1:1, GENERATE=1:0, ANNOTATION_PROCESSING=1:1, ANNOTATION_PROCESSING_ROUND=2:2}";
                        break;
                    default:
                        throw new IllegalStateException();
                }
                break;

            // "Add in processor" should skip initial PARSE/ENTER events
            case ADD_IN_PROCESSOR:
                switch (rk) {
                    // Remove will fail (too early), so events to end will be recorded
                    case REMOVE_IN_TASK:
                        if (ck == CompileKind.CALL)
                            found = "{ENTER=2:2, ANALYZE=1:1, GENERATE=1:1, ANNOTATION_PROCESSING=0:1, ANNOTATION_PROCESSING_ROUND=1:2, COMPILATION=0:1}";
                        else
                            found = "{ENTER=2:2, ANALYZE=1:1, GENERATE=1:1, ANNOTATION_PROCESSING=0:1, ANNOTATION_PROCESSING_ROUND=1:2}";
                        break;
                    case REMOVE_IN_PROCESSOR:
                        found = "{ENTER=1:1, ANNOTATION_PROCESSING_ROUND=1:1}";
                        break;
                    case REMOVE_IN_LISTENER:
                        found = "{ENTER=2:2, ANALYZE=1:1, GENERATE=1:0, ANNOTATION_PROCESSING=0:1, ANNOTATION_PROCESSING_ROUND=1:2}";
                        break;
                    default:
                        throw new IllegalStateException();
                }
                break;

            // "Add in listener" will occur during "ANALYSE.started" event
            case ADD_IN_LISTENER:
                switch (rk) {
                    // Remove will fail (too early, so events to end will be recorded
                    case REMOVE_IN_TASK:
                    case REMOVE_IN_PROCESSOR:
                        if (ck == CompileKind.CALL)
                            found = "{ANALYZE=0:1, GENERATE=1:1, COMPILATION=0:1}";
                        else
                            found = "{ANALYZE=0:1, GENERATE=1:1}";
                        break;
                    // Remove will succeed during "GENERATE.finished" event
                    case REMOVE_IN_LISTENER:
                        found = "{ANALYZE=0:1, GENERATE=1:0}";
                        break;
                    default:
                        throw new IllegalStateException();
                }
                break;
            default:
                throw new IllegalStateException();
        }

        if (!found.equals(expect)) {
            System.err.println("Expected: " + expect);
            System.err.println("   Found: " + found);
            error("unexpected value found");
        }
    }

    int errors;

    void error(String message) {
        System.err.println("Error: " + message);
        errors++;
    }
}
