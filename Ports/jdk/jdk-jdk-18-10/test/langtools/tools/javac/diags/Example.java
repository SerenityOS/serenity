/*
 * Copyright (c) 2010, 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import javax.annotation.processing.Processor;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

// The following two classes are both used, but cannot be imported directly
// import com.sun.tools.javac.Main
// import com.sun.tools.javac.main.Main

import com.sun.tools.javac.api.ClientCodeWrapper;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.main.Main;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JavacMessages;
import com.sun.tools.javac.util.JCDiagnostic;

/**
 * Class to handle example code designed to illustrate javac diagnostic messages.
 */
class Example implements Comparable<Example> {
    /* Create an Example from the files found at path.
     * The head of the file, up to the first Java code, is scanned
     * for information about the test, such as what resource keys it
     * generates when run, what options are required to run it, and so on.
     */
    Example(File file) {
        this.file = file;
        declaredKeys = new TreeSet<String>();
        srcFiles = new ArrayList<File>();
        procFiles = new ArrayList<File>();
        srcPathFiles = new ArrayList<File>();
        moduleSourcePathFiles = new ArrayList<File>();
        patchModulePathFiles = new ArrayList<File>();
        modulePathFiles = new ArrayList<File>();
        classPathFiles = new ArrayList<File>();
        additionalFiles = new ArrayList<File>();
        nonEmptySrcFiles = new ArrayList<File>();

        findFiles(file, srcFiles);
        for (File f: srcFiles) {
            parse(f);
        }

        if (infoFile == null)
            throw new Error("Example " + file + " has no info file");
    }

    private void findFiles(File f, List<File> files) {
        if (f.isDirectory()) {
            for (File c: f.listFiles()) {
                if (files == srcFiles && c.getName().equals("processors"))
                    findFiles(c, procFiles);
                else if (files == srcFiles && c.getName().equals("sourcepath")) {
                    srcPathDir = c;
                    findFiles(c, srcPathFiles);
                } else if (files == srcFiles && c.getName().equals("modulesourcepath")) {
                    moduleSourcePathDir = c;
                    findFiles(c, moduleSourcePathFiles);
                } else if (files == srcFiles && c.getName().equals("patchmodule")) {
                    patchModulePathDir = c;
                    findFiles(c, patchModulePathFiles);
                } else if (files == srcFiles && c.getName().equals("additional")) {
                    additionalFilesDir = c;
                    findFiles(c, additionalFiles);
                } else if (files == srcFiles && c.getName().equals("modulepath")) {
                    findFiles(c, modulePathFiles);
                } else if (files == srcFiles && c.getName().equals("classpath")) {
                    findFiles(c, classPathFiles);
                } else {
                    findFiles(c, files);
                }
            }
        } else if (f.isFile()) {
            if (f.getName().endsWith(".java")) {
                files.add(f);
            } else if (f.getName().equals("modulesourcepath")) {
                moduleSourcePathDir = f;
            }
        }
    }

    private void parse(File f) {
        Pattern keyPat = Pattern.compile(" *// *key: *([^ ]+) *");
        Pattern optPat = Pattern.compile(" *// *options: *(.*)");
        Pattern runPat = Pattern.compile(" *// *run: *(.*)");
        Pattern javaPat = Pattern.compile(" *@?[A-Za-z].*");
        try {
            String[] lines = read(f).split("[\r\n]+");
            for (String line: lines) {
                Matcher keyMatch = keyPat.matcher(line);
                if (keyMatch.matches()) {
                    foundInfo(f);
                    declaredKeys.add(keyMatch.group(1));
                    continue;
                }
                Matcher optMatch = optPat.matcher(line);
                if (optMatch.matches()) {
                    foundInfo(f);
                    options = Arrays.asList(optMatch.group(1).trim().split(" +"));
                    continue;
                }
                Matcher runMatch = runPat.matcher(line);
                if (runMatch.matches()) {
                    foundInfo(f);
                    runOpts = Arrays.asList(runMatch.group(1).trim().split(" +"));
                }
                if (javaPat.matcher(line).matches()) {
                    nonEmptySrcFiles.add(f);
                    break;
                }
            }
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    private void foundInfo(File file) {
        if (infoFile != null && !infoFile.equals(file))
            throw new Error("multiple info files found: " + infoFile + ", " + file);
        infoFile = file;
    }

    String getName() {
        return file.getName();
    }

    /**
     * Get the set of resource keys that this test declares it will generate
     * when it is run.
     */
    Set<String> getDeclaredKeys() {
        return declaredKeys;
    }

    /**
     * Get the set of resource keys that this test generates when it is run.
     * The test will be run if it has not already been run.
     */
    Set<String> getActualKeys() {
        if (actualKeys == null)
            actualKeys = run(false);
        return actualKeys;
    }

    /**
     * Run the test.  Information in the test header is used to determine
     * how to run the test.
     */
    void run(PrintWriter out, boolean raw, boolean verbose) {
        if (out == null)
            throw new NullPointerException();
        try {
            run(out, null, raw, verbose);
        } catch (IOException e) {
            e.printStackTrace(out);
        }
    }

    Set<String> run(boolean verbose) {
        Set<String> keys = new TreeSet<String>();
        try {
            run(null, keys, true, verbose);
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
        return keys;
    }

    /**
     * Run the test.  Information in the test header is used to determine
     * how to run the test.
     */
    private void run(PrintWriter out, Set<String> keys, boolean raw, boolean verbose)
            throws IOException {
        List<String> opts = new ArrayList<String>();
        if (!modulePathFiles.isEmpty()) {
            File modulepathDir = new File(tempDir, "modulepath");
            modulepathDir.mkdirs();
            clean(modulepathDir);
            boolean hasModuleInfo =
                    modulePathFiles.stream()
                                   .anyMatch(f -> f.getName().equalsIgnoreCase("module-info.java"));
            Path modulePath = new File(file, "modulepath").toPath().toAbsolutePath();
            if (hasModuleInfo) {
                //ordinary modules
                List<String> sOpts =
                        Arrays.asList("-d", modulepathDir.getPath(),
                                      "--module-source-path", modulePath.toString());
                new Jsr199Compiler(verbose).run(null, null, false, sOpts, modulePathFiles);
            } else {
                //automatic modules:
                Map<String, List<Path>> module2Files =
                        modulePathFiles.stream()
                                       .map(f -> f.toPath())
                                       .collect(Collectors.groupingBy(p -> modulePath.relativize(p)
                                                                            .getName(0)
                                                                            .toString()));
                for (Entry<String, List<Path>> e : module2Files.entrySet()) {
                    File scratchDir = new File(tempDir, "scratch");
                    scratchDir.mkdirs();
                    clean(scratchDir);
                    List<String> sOpts =
                            Arrays.asList("-d", scratchDir.getPath());
                    new Jsr199Compiler(verbose).run(null,
                                                    null,
                                                    false,
                                                    sOpts,
                                                    e.getValue().stream()
                                                                .map(p -> p.toFile())
                                                                .collect(Collectors.toList()));
                    try (JarOutputStream jarOut =
                            new JarOutputStream(new FileOutputStream(new File(modulepathDir, e.getKey() + ".jar")))) {
                        Files.find(scratchDir.toPath(), Integer.MAX_VALUE, (p, attr) -> attr.isRegularFile())
                                .forEach(p -> {
                                    try (InputStream in = Files.newInputStream(p)) {
                                        jarOut.putNextEntry(new ZipEntry(scratchDir.toPath()
                                                                                   .relativize(p)
                                                                                   .toString()));
                                        jarOut.write(in.readAllBytes());
                                    } catch (IOException ex) {
                                        throw new IllegalStateException(ex);
                                    }
                                });
                    }
                }
            }
            opts.add("--module-path");
            opts.add(modulepathDir.getAbsolutePath());
        }

        if (!classPathFiles.isEmpty()) {
            File classpathDir = new File(tempDir, "classpath");
            classpathDir.mkdirs();
            clean(classpathDir);
            List<String> sOpts = Arrays.asList("-d", classpathDir.getPath());
            new Jsr199Compiler(verbose).run(null, null, false, sOpts, classPathFiles);
            opts.add("--class-path");
            opts.add(classpathDir.getAbsolutePath());
        }

        File classesDir = new File(tempDir, "classes");
        classesDir.mkdirs();
        clean(classesDir);

        opts.add("-d");
        opts.add(classesDir.getPath());
        if (options != null)
            opts.addAll(evalProperties(options));

        if (procFiles.size() > 0) {
            List<String> pOpts = new ArrayList<>(Arrays.asList("-d", classesDir.getPath()));

            // hack to automatically add exports; a better solution would be to grep the
            // source for import statements or a magic comment
            for (File pf: procFiles) {
                if (pf.getName().equals("CreateBadClassFile.java")) {
                    pOpts.add("--add-modules=jdk.jdeps");
                    pOpts.add("--add-exports=jdk.jdeps/com.sun.tools.classfile=ALL-UNNAMED");
                }
            }

            new Jsr199Compiler(verbose).run(null, null, false, pOpts, procFiles);
            opts.add("-classpath"); // avoid using -processorpath for now
            opts.add(classesDir.getPath());
            createAnnotationServicesFile(classesDir, procFiles);
        } else if (options != null) {
            int i = options.indexOf("-processor");
            // check for built-in anno-processor(s)
            if (i != -1 && options.get(i + 1).equals("DocCommentProcessor")) {
                opts.add("-classpath");
                opts.add(System.getProperty("test.classes"));
            }
        }

        List<File> files = srcFiles;

        if (srcPathDir != null) {
            opts.add("-sourcepath");
            opts.add(srcPathDir.getPath());
        }

        if (moduleSourcePathDir != null) {
            opts.add("--module-source-path");
            opts.add(moduleSourcePathDir.getPath());
            files = new ArrayList<>();
            files.addAll(moduleSourcePathFiles);
            files.addAll(nonEmptySrcFiles); // srcFiles containing declarations
        }

        if (patchModulePathDir != null) {
            for (File mod : patchModulePathDir.listFiles()) {
                opts.add("--patch-module");
                opts.add(mod.getName() + "=" + mod.getPath());
            }
            files = new ArrayList<>();
            files.addAll(patchModulePathFiles);
            files.addAll(nonEmptySrcFiles); // srcFiles containing declarations
        }

        if (additionalFiles.size() > 0) {
            List<String> sOpts = Arrays.asList("-d", classesDir.getPath());
            new Jsr199Compiler(verbose).run(null, null, false, sOpts, additionalFiles);
        }

        try {
            Compiler c = Compiler.getCompiler(runOpts, verbose);
            c.run(out, keys, raw, opts, files);
        } catch (IllegalArgumentException e) {
            if (out != null) {
                out.println("Invalid value for run tag: " + runOpts);
            }
        }
    }

    private static List<String> evalProperties(List<String> args) {
        boolean fast = true;
        for (String arg : args) {
            fast = fast && (arg.indexOf("${") == -1);
        }
        if (fast) {
            return args;
        }
        List<String> newArgs = new ArrayList<>();
        for (String arg : args) {
            newArgs.add(evalProperties(arg));
        }
        return newArgs;
    }

    private static final Pattern namePattern = Pattern.compile("\\$\\{([A-Za-z0-9._]+)\\}");
    private static final String jdkVersion = Integer.toString(Runtime.version().feature());

    private static String evalProperties(String arg) {
        Matcher m = namePattern.matcher(arg);
        StringBuilder sb = null;
        while (m.find()) {
            if (sb == null) {
                sb = new StringBuilder();
            }
            String propName = m.group(1);
            String propValue;
            switch (propName) {
                case "jdk.version":
                    propValue = jdkVersion;
                    break;
                default:
                    propValue = System.getProperty(propName);
                    break;
            }
            m.appendReplacement(sb, propValue != null ? propValue : m.group(0).replace("$", "\\$"));
        }
        if (sb == null) {
            return arg;
        } else {
            m.appendTail(sb);
            return sb.toString();
        }
    }

    void createAnnotationServicesFile(File dir, List<File> procFiles) throws IOException {
        File servicesDir = new File(new File(dir, "META-INF"), "services");
        servicesDir.mkdirs();
        File annoServices = new File(servicesDir, Processor.class.getName());
        Writer out = new FileWriter(annoServices);
        try {
            for (File f: procFiles) {
                out.write(f.getName().toString().replace(".java", ""));
            }
        } finally {
            out.close();
        }
    }

    @Override
    public int compareTo(Example e) {
        return file.compareTo(e.file);
    }

    @Override
    public String toString() {
        return file.getPath();
    }

    /**
     * Read the contents of a file.
     */
    private String read(File f) throws IOException {
        byte[] bytes = new byte[(int) f.length()];
        DataInputStream in = new DataInputStream(new FileInputStream(f));
        try {
            in.readFully(bytes);
        } finally {
            in.close();
        }
        return new String(bytes);
    }

    /**
     * Clean the contents of a directory.
     */
    boolean clean(File dir) {
        boolean ok = true;
        for (File f: dir.listFiles()) {
            if (f.isDirectory())
                ok &= clean(f);
            ok &= f.delete();
        }
        return ok;
    }

    File file;
    List<File> srcFiles;
    List<File> procFiles;
    File srcPathDir;
    File moduleSourcePathDir;
    File patchModulePathDir;
    File additionalFilesDir;
    List<File> srcPathFiles;
    List<File> moduleSourcePathFiles;
    List<File> patchModulePathFiles;
    List<File> modulePathFiles;
    List<File> classPathFiles;
    List<File> additionalFiles;
    List<File> nonEmptySrcFiles;
    File infoFile;
    private List<String> runOpts;
    private List<String> options;
    private Set<String> actualKeys;
    private Set<String> declaredKeys;

    static File tempDir = (System.getProperty("test.src") != null) ?
            new File(System.getProperty("user.dir")):
            new File(System.getProperty("java.io.tmpdir"));

    static void setTempDir(File tempDir) {
        Example.tempDir = tempDir;
    }

    abstract static class Compiler {
        interface Factory {
            Compiler getCompiler(List<String> opts, boolean verbose);
        }

        static class DefaultFactory implements Factory {
            public Compiler getCompiler(List<String> opts, boolean verbose) {
                String first;
                String[] rest;
                    if (opts == null || opts.isEmpty()) {
                    first = null;
                    rest = new String[0];
                } else {
                    first = opts.get(0);
                    rest = opts.subList(1, opts.size()).toArray(new String[opts.size() - 1]);
                }
                // For more details on the different compilers,
                // see their respective class doc comments.
                // See also README.examples.txt in this directory.
                if (first == null || first.equals("jsr199"))
                    return new Jsr199Compiler(verbose, rest);
                else if (first.equals("simple"))
                    return new SimpleCompiler(verbose);
                else if (first.equals("backdoor"))
                    return new BackdoorCompiler(verbose);
                else if (first.equals("exec"))
                    return new ExecCompiler(verbose, rest);
                else
                    throw new IllegalArgumentException(first);
            }
        }

        static Factory factory;

        static Compiler getCompiler(List<String> opts, boolean verbose) {
            if (factory == null)
                factory = new DefaultFactory();

            return factory.getCompiler(opts, verbose);
        }

        protected Compiler(boolean verbose) {
            this.verbose = verbose;
        }

        abstract boolean run(PrintWriter out, Set<String> keys, boolean raw,
                List<String> opts,  List<File> files);

        void setSupportClassLoader(ClassLoader cl) {
            loader = cl;
        }

        protected void close(JavaFileManager fm) {
            try {
                fm.close();
            } catch (IOException e) {
                throw new Error(e);
            }
        }

        protected ClassLoader loader;
        protected boolean verbose;
    }

    /**
     * Compile using the JSR 199 API.  The diagnostics generated are
     * scanned for resource keys.   Not all diagnostic keys are generated
     * via the JSR 199 API -- for example, rich diagnostics are not directly
     * accessible, and some diagnostics generated by the file manager may
     * not be generated (for example, the JSR 199 file manager does not see
     * -Xlint:path).
     */
    static class Jsr199Compiler extends Compiler {
        List<String> fmOpts;

        Jsr199Compiler(boolean verbose, String... args) {
            super(verbose);
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.equals("-filemanager") && (i + 1 < args.length)) {
                    fmOpts = Arrays.asList(args[++i].split(","));
                } else
                    throw new IllegalArgumentException(arg);
            }
        }

        @Override
        boolean run(PrintWriter out, Set<String> keys, boolean raw, List<String> opts, List<File> files) {
            if (out != null && keys != null)
                throw new IllegalArgumentException();

            if (verbose)
                System.err.println("run_jsr199: " + opts + " " + files);

            DiagnosticCollector<JavaFileObject> dc = null;
            if (keys != null)
                dc = new DiagnosticCollector<JavaFileObject>();

            if (raw) {
                List<String> newOpts = new ArrayList<String>();
                newOpts.add("-XDrawDiagnostics");
                newOpts.addAll(opts);
                opts = newOpts;
            }

            JavaCompiler c = ToolProvider.getSystemJavaCompiler();

            StandardJavaFileManager fm = c.getStandardFileManager(dc, null, null);
            try {
                if (fmOpts != null)
                    fm = new FileManager(fm, fmOpts);

                Iterable<? extends JavaFileObject> fos = fm.getJavaFileObjectsFromFiles(files);

                CompilationTask t = c.getTask(out, fm, dc, opts, null, fos);
                Boolean ok = t.call();

                if (keys != null) {
                    for (Diagnostic<? extends JavaFileObject> d: dc.getDiagnostics()) {
                        scanForKeys(unwrap(d), keys);
                    }
                }

                return ok;
            } finally {
                close(fm);
            }
        }

        /**
         * Scan a diagnostic for resource keys.  This will not detect additional
         * sub diagnostics that might be generated by a rich diagnostic formatter.
         */
        private static void scanForKeys(JCDiagnostic d, Set<String> keys) {
            keys.add(d.getCode());
            for (Object o: d.getArgs()) {
                if (o instanceof JCDiagnostic) {
                    scanForKeys((JCDiagnostic) o, keys);
                }
            }
            for (JCDiagnostic sd: d.getSubdiagnostics())
                scanForKeys(sd, keys);
        }

        private JCDiagnostic unwrap(Diagnostic<? extends JavaFileObject> diagnostic) {
            if (diagnostic instanceof JCDiagnostic)
                return (JCDiagnostic) diagnostic;
            if (diagnostic instanceof ClientCodeWrapper.DiagnosticSourceUnwrapper)
                return ((ClientCodeWrapper.DiagnosticSourceUnwrapper)diagnostic).d;
            throw new IllegalArgumentException();
        }
    }

    /**
     * Run the test using the standard simple entry point.
     */
    static class SimpleCompiler extends Compiler {
        SimpleCompiler(boolean verbose) {
            super(verbose);
        }

        @Override
        boolean run(PrintWriter out, Set<String> keys, boolean raw, List<String> opts, List<File> files) {
            if (out != null && keys != null)
                throw new IllegalArgumentException();

            if (verbose)
                System.err.println("run_simple: " + opts + " " + files);

            List<String> args = new ArrayList<String>();

            if (keys != null || raw)
                args.add("-XDrawDiagnostics");

            args.addAll(opts);
            for (File f: files)
                args.add(f.getPath());

            StringWriter sw = null;
            PrintWriter pw;
            if (keys != null) {
                sw = new StringWriter();
                pw = new PrintWriter(sw);
            } else
                pw = out;

            int rc = com.sun.tools.javac.Main.compile(args.toArray(new String[args.size()]), pw);

            if (keys != null) {
                pw.close();
                scanForKeys(sw.toString(), keys);
            }

            return (rc == 0);
        }

        private static void scanForKeys(String text, Set<String> keys) {
            StringTokenizer st = new StringTokenizer(text, " ,\r\n():");
            while (st.hasMoreElements()) {
                String t = st.nextToken();
                if (t.startsWith("compiler."))
                    keys.add(t);
            }
        }
    }

    /**
     * Run the test in a separate process.
     */
    static class ExecCompiler extends Compiler {
        List<String> vmOpts;

        ExecCompiler(boolean verbose, String... args) {
            super(verbose);
            vmOpts = Arrays.asList(args);
        }

        @Override
        boolean run(PrintWriter out, Set<String> keys, boolean raw, List<String> opts, List<File> files) {
            if (out != null && keys != null)
                throw new IllegalArgumentException();

            if (verbose)
                System.err.println("run_exec: " + vmOpts + " " + opts + " " + files);

            List<String> args = new ArrayList<String>();

            File javaHome = new File(System.getProperty("java.home"));
            if (javaHome.getName().equals("jre"))
                javaHome = javaHome.getParentFile();
            File javaExe = new File(new File(javaHome, "bin"), "java");
            args.add(javaExe.getPath());

            File toolsJar = new File(new File(javaHome, "lib"), "tools.jar");
            if (toolsJar.exists()) {
                args.add("-classpath");
                args.add(toolsJar.getPath());
            }

            args.addAll(vmOpts);
            addOpts(args, "test.vm.opts");
            addOpts(args, "test.java.opts");
            args.add(com.sun.tools.javac.Main.class.getName());

            if (keys != null || raw)
                args.add("-XDrawDiagnostics");

            args.addAll(opts);
            for (File f: files)
                args.add(f.getPath());

            try {
                ProcessBuilder pb = new ProcessBuilder(args);
                pb.redirectErrorStream(true);
                Process p = pb.start();
                BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    if (keys != null)
                        scanForKeys(line, keys);
                }
                int rc = p.waitFor();

                return (rc == 0);
            } catch (IOException | InterruptedException e) {
                System.err.println("Exception execing javac" + e);
                System.err.println("Command line: " + opts);
                return false;
            }
        }

        private static void scanForKeys(String text, Set<String> keys) {
            StringTokenizer st = new StringTokenizer(text, " ,\r\n():");
            while (st.hasMoreElements()) {
                String t = st.nextToken();
                if (t.startsWith("compiler."))
                    keys.add(t);
            }
        }

        private static void addOpts(List<String> args, String propName) {
            String propValue = System.getProperty(propName);
            if (propValue == null || propValue.isEmpty())
                return;
            args.addAll(Arrays.asList(propValue.split(" +", 0)));
        }
    }

    static class BackdoorCompiler extends Compiler {
        BackdoorCompiler(boolean verbose) {
            super(verbose);
        }

        @Override
        boolean run(PrintWriter out, Set<String> keys, boolean raw, List<String> opts, List<File> files) {
            if (out != null && keys != null)
                throw new IllegalArgumentException();

            if (verbose)
                System.err.println("run_simple: " + opts + " " + files);

            List<String> args = new ArrayList<String>();

            if (out != null && raw)
                args.add("-XDrawDiagnostics");

            args.addAll(opts);
            for (File f: files)
                args.add(f.getPath());

            StringWriter sw = null;
            PrintWriter pw;
            if (keys != null) {
                sw = new StringWriter();
                pw = new PrintWriter(sw);
            } else
                pw = out;

            Context c = new Context();
            JavacFileManager.preRegister(c); // can't create it until Log has been set up
            MessageTracker.preRegister(c, keys);

            try {
                Main m = new Main("javac", pw);
                Main.Result rc = m.compile(args.toArray(new String[args.size()]), c);

                if (keys != null) {
                    pw.close();
                }

                return rc.isOK();
            } finally {
                close(c.get(JavaFileManager.class));
            }
        }

        static class MessageTracker extends JavacMessages {

            MessageTracker(Context context) {
                super(context);
            }

            static void preRegister(Context c, final Set<String> keys) {
                if (keys != null) {
                    c.put(JavacMessages.messagesKey, new Context.Factory<JavacMessages>() {
                        public JavacMessages make(Context c) {
                            return new MessageTracker(c) {
                                @Override
                                public String getLocalizedString(Locale l, String key, Object... args) {
                                    keys.add(key);
                                    return super.getLocalizedString(l, key, args);
                                }
                            };
                        }
                    });
                }
            }
        }

    }
}
