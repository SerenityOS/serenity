/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package toolbox;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.Processor;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;

import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;

/**
 * A task to configure and run the Java compiler, javac.
 */
public class JavacTask extends AbstractTask<JavacTask> {
    private boolean includeStandardOptions;
    private List<Path> classpath;
    private List<Path> sourcepath;
    private Path outdir;
    private List<String> options;
    private List<String> classes;
    private List<String> files;
    private List<JavaFileObject> fileObjects;
    private JavaFileManager fileManager;
    private Consumer<com.sun.source.util.JavacTask> callback;
    private List<Processor> procs;

    private JavaCompiler compiler;
    private StandardJavaFileManager internalFileManager;

    /**
     * Creates a task to execute {@code javac} using API mode.
     * @param toolBox the {@code ToolBox} to use
     */
    public JavacTask(ToolBox toolBox) {
        super(toolBox, Task.Mode.API);
    }

    /**
     * Creates a task to execute {@code javac} in a specified mode.
     * @param toolBox the {@code ToolBox} to use
     * @param mode the mode to be used
     */
    public JavacTask(ToolBox toolBox, Task.Mode mode) {
        super(toolBox, mode);
    }

    /**
     * Sets the classpath.
     * @param classpath the classpath
     * @return this task object
     */
    public JavacTask classpath(String classpath) {
        this.classpath = Stream.of(classpath.split(File.pathSeparator))
                .filter(s -> !s.isEmpty())
                .map(s -> Paths.get(s))
                .collect(Collectors.toList());
        return this;
    }

    /**
     * Sets the classpath.
     * @param classpath the classpath
     * @return this task object
     */
    public JavacTask classpath(Path... classpath) {
        this.classpath = Arrays.asList(classpath);
        return this;
    }

    /**
     * Sets the classpath.
     * @param classpath the classpath
     * @return this task object
     */
    public JavacTask classpath(List<Path> classpath) {
        this.classpath = classpath;
        return this;
    }

    /**
     * Sets the sourcepath.
     * @param sourcepath the sourcepath
     * @return this task object
     */
    public JavacTask sourcepath(String sourcepath) {
        this.sourcepath = Stream.of(sourcepath.split(File.pathSeparator))
                .filter(s -> !s.isEmpty())
                .map(s -> Paths.get(s))
                .collect(Collectors.toList());
        return this;
    }

    /**
     * Sets the sourcepath.
     * @param sourcepath the sourcepath
     * @return this task object
     */
    public JavacTask sourcepath(Path... sourcepath) {
        this.sourcepath = Arrays.asList(sourcepath);
        return this;
    }

    /**
     * Sets the sourcepath.
     * @param sourcepath the sourcepath
     * @return this task object
     */
    public JavacTask sourcepath(List<Path> sourcepath) {
        this.sourcepath = sourcepath;
        return this;
    }

    /**
     * Sets the output directory.
     * @param outdir the output directory
     * @return this task object
     */
    public JavacTask outdir(String outdir) {
        this.outdir = Paths.get(outdir);
        return this;
    }

    /**
     * Sets the output directory.
     * @param outdir the output directory
     * @return this task object
     */
    public JavacTask outdir(Path outdir) {
        this.outdir = outdir;
        return this;
    }

    /**
     * Sets the options.
     * @param options the options
     * @return this task object
     */
    public JavacTask options(String... options) {
        this.options = Arrays.asList(options);
        return this;
    }

    /**
     * Sets the options.
     * @param spaceSeparatedOption the space separated options
     * @return this task object
     */
    public JavacTask spaceSeparatedOptions(String spaceSeparatedOption) {
        this.options = Arrays.asList(spaceSeparatedOption.split("\\s+"));
        return this;
    }

    /**
     * Sets the options.
     * @param options the options
     * @return this task object
     */
    public JavacTask options(List<String> options) {
        this.options = options;
        return this;
    }

    /**
     * Sets the classes to be analyzed.
     * @param classes the classes
     * @return this task object
     */
    public JavacTask classes(String... classes) {
        this.classes = Arrays.asList(classes);
        return this;
    }

    /**
     * Sets the files to be compiled or analyzed.
     * @param files the files
     * @return this task object
     */
    public JavacTask files(String... files) {
        this.files = Arrays.asList(files);
        return this;
    }

    /**
     * Sets the files to be compiled or analyzed.
     * @param files the files
     * @return this task object
     */
    public JavacTask files(Path... files) {
        this.files = Stream.of(files)
                .map(Path::toString)
                .collect(Collectors.toList());
        return this;
    }

    /**
     * Sets the files to be compiled or analyzed.
     * @param files the files
     * @return this task object
     */
    public JavacTask files(List<Path> files) {
        this.files = files.stream()
                .map(Path::toString)
                .collect(Collectors.toList());
        return this;
    }

    /**
     * Sets the sources to be compiled or analyzed.
     * Each source string is converted into an in-memory object that
     * can be passed directly to the compiler.
     * @param sources the sources
     * @return this task object
     */
    public JavacTask sources(String... sources) {
        fileObjects = Stream.of(sources)
                .map(s -> new ToolBox.JavaSource(s))
                .collect(Collectors.toList());
        return this;
    }

    /**
     * Sets the the annotation processors to be used.
     */
    public JavacTask processors(Processor... procs) {
        this.procs = List.of(procs);
        return this;
    }

    /**
     * Sets the file manager to be used by this task.
     * @param fileManager the file manager
     * @return this task object
     */
    public JavacTask fileManager(JavaFileManager fileManager) {
        this.fileManager = fileManager;
        return this;
    }

    /**
     * Set a callback to be used by this task.
     * @param callback the callback
     * @return this task object
     */
    public JavacTask callback(Consumer<com.sun.source.util.JavacTask> callback) {
        this.callback = callback;
        return this;
    }

    /**
     * {@inheritDoc}
     * @return the name "javac"
     */
    @Override
    public String name() {
        return "javac";
    }

    /**
     * Calls the compiler with the arguments as currently configured.
     * @return a Result object indicating the outcome of the compilation
     * and the content of any output written to stdout, stderr, or the
     * main stream by the compiler.
     * @throws TaskError if the outcome of the task is not as expected.
     */
    @Override
    public Task.Result run() {
        if (mode == Task.Mode.EXEC)
            return runExec();

        AbstractTask.WriterOutput direct = new AbstractTask.WriterOutput();
        // The following are to catch output to System.out and System.err,
        // in case these are used instead of the primary (main) stream
        AbstractTask.StreamOutput sysOut = new AbstractTask.StreamOutput(System.out, System::setOut);
        AbstractTask.StreamOutput sysErr = new AbstractTask.StreamOutput(System.err, System::setErr);
        int rc;
        Map<Task.OutputKind, String> outputMap = new HashMap<>();
        try {
            switch (mode == null ? Task.Mode.API : mode) {
                case API:
                    rc = runAPI(direct.pw);
                    break;
                case CMDLINE:
                    if (fileManager != null) {
                        throw new IllegalStateException("file manager set in CMDLINE mode");
                    }
                    if (callback != null) {
                        throw new IllegalStateException("callback set in CMDLINE mode");
                    }
                    rc = runCommand(direct.pw);
                    break;
                default:
                    throw new IllegalStateException("unknown mode " + mode);
            }
        } catch (IOException e) {
            toolBox.out.println("Exception occurred: " + e);
            rc = 99;
        } finally {
            outputMap.put(Task.OutputKind.STDOUT, sysOut.close());
            outputMap.put(Task.OutputKind.STDERR, sysErr.close());
            outputMap.put(Task.OutputKind.DIRECT, direct.close());
        }
        return checkExit(new Task.Result(toolBox, this, rc, outputMap));
    }

    private int runAPI(PrintWriter pw) throws IOException {
        try {
//                if (compiler == null) {
                // TODO: allow this to be set externally
//                    compiler = ToolProvider.getSystemJavaCompiler();
                compiler = JavacTool.create();
//                }

            if (fileManager == null)
                fileManager = internalFileManager = compiler.getStandardFileManager(null, null, null);
            if (outdir != null)
                setLocationFromPaths(StandardLocation.CLASS_OUTPUT, Collections.singletonList(outdir));
            if (classpath != null)
                setLocationFromPaths(StandardLocation.CLASS_PATH, classpath);
            if (sourcepath != null)
                setLocationFromPaths(StandardLocation.SOURCE_PATH, sourcepath);
            List<String> allOpts = new ArrayList<>();
            if (options != null)
                allOpts.addAll(options);

            Iterable<? extends JavaFileObject> allFiles = joinFiles(files, fileObjects);
            JavaCompiler.CompilationTask task = compiler.getTask(pw,
                    fileManager,
                    null,  // diagnostic listener; should optionally collect diags
                    allOpts,
                    classes,
                    allFiles);
            if (procs != null) {
                task.setProcessors(procs);
            }
            JavacTaskImpl taskImpl = (JavacTaskImpl) task;
            if (callback != null) {
                callback.accept(taskImpl);
            }
            return taskImpl.doCall().exitCode;
        } finally {
            if (internalFileManager != null)
                internalFileManager.close();
        }
    }

    private void setLocationFromPaths(StandardLocation location, List<Path> files) throws IOException {
        if (!(fileManager instanceof StandardJavaFileManager))
            throw new IllegalStateException("not a StandardJavaFileManager");
        ((StandardJavaFileManager) fileManager).setLocationFromPaths(location, files);
    }

    private int runCommand(PrintWriter pw) {
        List<String> args = getAllArgs();
        String[] argsArray = args.toArray(new String[args.size()]);
        return com.sun.tools.javac.Main.compile(argsArray, pw);
    }

    private Task.Result runExec() {
        List<String> args = new ArrayList<>();
        Path javac = toolBox.getJDKTool("javac");
        args.add(javac.toString());
        if (includeStandardOptions) {
            args.addAll(toolBox.split(System.getProperty("test.tool.vm.opts"), " +"));
            args.addAll(toolBox.split(System.getProperty("test.compiler.opts"), " +"));
        }
        args.addAll(getAllArgs());

        String[] argsArray = args.toArray(new String[args.size()]);
        ProcessBuilder pb = getProcessBuilder();
        pb.command(argsArray);
        try {
            return runProcess(toolBox, this, pb.start());
        } catch (IOException | InterruptedException e) {
            throw new Error(e);
        }
    }

    private List<String> getAllArgs() {
        List<String> args = new ArrayList<>();
        if (options != null)
            args.addAll(options);
        if (outdir != null) {
            args.add("-d");
            args.add(outdir.toString());
        }
        if (classpath != null) {
            args.add("-classpath");
            args.add(toSearchPath(classpath));
        }
        if (sourcepath != null) {
            args.add("-sourcepath");
            args.add(toSearchPath(sourcepath));
        }
        if (classes != null)
            args.addAll(classes);
        if (files != null)
            args.addAll(files);

        return args;
    }

    private String toSearchPath(List<Path> files) {
        return files.stream()
            .map(Path::toString)
            .collect(Collectors.joining(File.pathSeparator));
    }

    private Iterable<? extends JavaFileObject> joinFiles(
            List<String> files, List<JavaFileObject> fileObjects) {
        if (files == null)
            return fileObjects;
        if (internalFileManager == null)
            internalFileManager = compiler.getStandardFileManager(null, null, null);
        Iterable<? extends JavaFileObject> filesAsFileObjects =
                internalFileManager.getJavaFileObjectsFromStrings(files);
        if (fileObjects == null)
            return filesAsFileObjects;
        List<JavaFileObject> combinedList = new ArrayList<>();
        for (JavaFileObject o : filesAsFileObjects)
            combinedList.add(o);
        combinedList.addAll(fileObjects);
        return combinedList;
    }
}
