/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.util.Context;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class OptionModesTester {

    /** Marker annotation for test methods to be invoked by runTests. */
    @Retention(RetentionPolicy.RUNTIME)
    @interface Test { }

    /**
     * Run all methods annotated with @Test, and throw an exception if any
     * errors are reported..
     * Typically called on a tester object in main()
     * @throws Exception if any errors occurred
     */
    void runTests() throws Exception {
        for (Method m: getClass().getDeclaredMethods()) {
            Annotation a = m.getAnnotation(Test.class);
            if (a != null) {
                try {
                    out.println("Running test " + m.getName());
                    m.invoke(this);
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    throw (cause instanceof Exception) ? ((Exception) cause) : e;
                }
                out.println();
            }
        }
        if (errors > 0)
            throw new Exception(errors + " errors occurred");
    }

    TestResult runMain(String[] opts, String[] files) {
        out.println("Main " + Arrays.toString(opts) + " " + Arrays.toString(files));
        return run(new TestResult(opts), (tr, c, pw) -> {
            com.sun.tools.javac.main.Main compiler =
                new com.sun.tools.javac.main.Main("javac", pw);
            int rc = compiler.compile(join(opts, files), c).exitCode;
            tr.setResult(rc);
        });
    }

    TestResult runCall(String[] opts, String[] files) {
        out.println("Call " + Arrays.toString(opts) + " " + Arrays.toString(files));
        return run(new TestResult(opts), (tr, c, pw) -> {
            boolean ok = JavacTool.create()
                    .getTask(pw, null, null, Arrays.asList(opts), null, getFiles(files), c)
                    .call();
            tr.setResult(ok);
        });
    }

    TestResult runParse(String[] opts, String[] files) {
        out.println("Parse " + Arrays.toString(opts) + " " + Arrays.toString(files));
        return run(new TestResult(opts), (tr, c, pw) -> {
            JavacTool.create()
                    .getTask(pw, null, null, Arrays.asList(opts), null, getFiles(files), c)
                    .parse();
            tr.setResult(true);
        });
    }

    TestResult runAnalyze(String[] opts, String[] files) {
        out.println("Analyze " + Arrays.toString(opts) + " " + Arrays.toString(files));
        return run(new TestResult(opts), (tr, c, pw) -> {
            JavacTool.create()
                    .getTask(pw, null, null, Arrays.asList(opts), null, getFiles(files), c)
                    .analyze();
            tr.setResult(true);
        });
    }

    interface Runnable {
        void run(TestResult tr, Context c, PrintWriter pw) throws IOException;
    }

    TestResult run(TestResult tr, Runnable r) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        StreamOutput sysOut = new StreamOutput(System.out, System::setOut);
        StreamOutput sysErr = new StreamOutput(System.err, System::setErr);
        Context context = new Context();
        JavacFileManager.preRegister(context);
        try {
            r.run(tr, context, pw);
        } catch (IllegalArgumentException | IllegalStateException | IOException e) {
            tr.setThrown(e);
        } finally {
            try {
                ((JavacFileManager) context.get(JavaFileManager.class)).close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            tr.setLogs(sw.toString(), sysOut.close(), sysErr.close());
        }
        tr.setContext(context);
        tr.show();
        return tr;
    }

    enum Log { DIRECT, STDOUT, STDERR };

    class TestResult {
        final List<String> args;
        Throwable thrown;
        List<Throwable> suppressed = new ArrayList<>();
        Object rc; // Number or Boolean
        Map<Log, String> logs;
        Context context;

        TestResult(String... args) {
            this.args = Arrays.asList(args);
        }

        TestResult(List<String> args, Iterable<? extends JavaFileObject> files) {
            this.args = new ArrayList<>();
            this.args.addAll(args);
            for (JavaFileObject f: files)
                this.args.add(f.getName());
        }

        void setResult(int rc) {
            this.rc = rc;
        }

        void setResult(boolean ok) {
            this.rc = ok ? 0 : 1;
        }

        void setSuppressed(Throwable thrown) {
            this.suppressed.add(thrown);
        }

        void setThrown(Throwable thrown) {
            this.thrown = thrown;
        }

        void setLogs(String direct, String stdOut, String stdErr) {
            logs = new EnumMap<>(Log.class);
            logs.put(Log.DIRECT, direct);
            logs.put(Log.STDOUT, stdOut);
            logs.put(Log.STDERR, stdErr);
        }

        void setContext(Context context) {
            this.context = context;
        }

        final void show() {
            String NL = System.getProperty("line.separator");
            boolean needSep = false;
            if (rc != null) {
                out.print("rc:" + rc);
                needSep = true;
            }
            if (thrown != null) {
                if (needSep) out.print("; ");
                out.print("thrown:" + thrown);
                needSep = true;
            }
            if (!suppressed.isEmpty()) {
                if (needSep) out.print("; ");
                out.print("suppressed:" + suppressed);
                needSep = true;
            }
            if (needSep)
                out.println();
            logs.forEach((k, v) -> {
                if (!v.isEmpty()) {
                    out.println("javac/" + k + ":");
                    if (v.endsWith(NL))
                        out.print(v);
                    else
                        out.println(v);
                }

            });
        }

        TestResult checkOK() {
            if (thrown != null) {
                error("unexpected exception thrown: " + thrown);
            } else if (rc == null) {
                error("no result set");
            } else if (rc != (Integer) 0 && rc != (Boolean) true) {
                error("compilation failed unexpectedly; rc=" + rc);
            }
            return this;
        }

        TestResult checkResult(int expect) {
            if (thrown != null) {
                error("unexpected exception thrown: " + thrown);
            } else if (rc != (Integer) expect) {
                error("unexpected result: " + rc +", expected:" + expect);
            }
            return this;
        }

        TestResult checkResult(boolean expect) {
            if (thrown != null) {
                error("unexpected exception thrown: " + thrown);
            } else if (rc != (Integer) (expect ? 0 : 1)) {
                error("unexpected result: " + rc +", expected:" + expect);
            }
            return this;
        }

        TestResult checkLog(String... expects) {
            return checkLog(Log.DIRECT, expects);
        }

        TestResult checkLog(Log l, String... expects) {
            for (String e: expects) {
                if (!logs.get(l).contains(e))
                    error("expected string not found: " + e);
            }
            return this;
        }

        TestResult checkIllegalArgumentException() {
            return checkThrown(IllegalArgumentException.class);
        }

        TestResult checkIllegalStateException() {
            return checkThrown(IllegalStateException.class);
        }

        TestResult checkThrown(Class<? extends Throwable> t) {
            if (thrown == null)
                error("expected exception not thrown: " + t);
            else if (!t.isAssignableFrom(thrown.getClass()))
                error("unexpected exception thrown: " + thrown + ";  expected: " + t);
            return this;
        }

        TestResult checkClass(String name) {
            Path p = getOutDir().resolve(name.replace(".", "/") + ".class");
            if (!Files.exists(p))
                error("expected class not found: " + name + " (" + p + ")");
            return this;
        }

        Path getOutDir() {
            Iterator<String> iter = args.iterator();
            while (iter.hasNext()) {
                if (iter.next().equals("-d")) {
                    return Paths.get(iter.next());
                }
            }
            return null;
        }
    }

    /**
     * Utility class to simplify the handling of temporarily setting a
     * new stream for System.out or System.err.
     */
    private static class StreamOutput {
        // functional interface to set a stream.
        private interface Initializer {
            void set(PrintStream s);
        }

        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        private final PrintStream ps = new PrintStream(baos);
        private final PrintStream prev;
        private final Initializer init;

        StreamOutput(PrintStream s, Initializer init) {
            prev = s;
            init.set(ps);
            this.init = init;
        }

        String close() {
            init.set(prev);
            ps.close();
            return baos.toString();
        }
    }

    List<JavaFileObject> getFiles(String... paths) {
        List<JavaFileObject> files = new ArrayList<>();
        for (JavaFileObject f : fm.getJavaFileObjects(paths))
            files.add(f);
        return files;
    }

    String toString(List<JavaFileObject> files) {
        return files.stream().map(f -> f.getName()).collect(Collectors.toList()).toString();
    }

    void mkdirs(String path) throws IOException {
        Files.createDirectories(Paths.get(path));
    }

    void writeFile(String path, String body) throws IOException {
        Path p = Paths.get(path);
        if (p.getParent() != null)
            Files.createDirectories(p.getParent());
        try (FileWriter w = new FileWriter(path)) {
            w.write(body);
        }
    }

    String[] join(String[] a, String[] b) {
        String[] result = new String[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    void error(String message) {
        out.print(">>>>> ");
        out.println(message);
        errors++;
    }

    StandardJavaFileManager fm =
            ToolProvider.getSystemJavaCompiler().getStandardFileManager(null, null, null);
    PrintStream out = System.err;
    int errors;

}
