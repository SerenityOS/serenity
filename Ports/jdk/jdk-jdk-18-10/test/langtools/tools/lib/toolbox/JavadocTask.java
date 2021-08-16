/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.tools.DocumentationTool.DocumentationTask;
import javax.tools.DocumentationTool;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import jdk.javadoc.internal.api.JavadocTool;

/**
 * A task to configure and run the documentation tool, javadoc.
 */
public class JavadocTask extends AbstractTask<JavadocTask> {
    private boolean includeStandardOptions;
    private List<Path> classpath;
    private List<Path> sourcepath;
    private Path outdir;
    private List<String> options;
    private List<String> classes;
    private List<String> files;
    private List<JavaFileObject> fileObjects;
    private JavaFileManager fileManager;

    private JavadocTool jdtool;
    private StandardJavaFileManager internalFileManager;
    private Class<?> docletClass = null; // use the standard doclet by default

    /**
     * Creates a task to execute {@code javadoc} using API mode.
     * @param toolBox the {@code ToolBox} to use
     */
    public JavadocTask(ToolBox toolBox) {
        super(toolBox, Task.Mode.API);
    }

    /**
     * Creates a task to execute {@code javadoc} in a specified mode.
     * @param toolBox the {@code ToolBox} to use
     * @param mode the mode to be used
     */
    public JavadocTask(ToolBox toolBox, Task.Mode mode) {
        super(toolBox, mode);
    }

    /**
     * Sets the classpath.
     * @param classpath the classpath
     * @return this task object
     */
    public JavadocTask classpath(String classpath) {
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
    public JavadocTask classpath(Path... classpath) {
        this.classpath = Arrays.asList(classpath);
        return this;
    }

    /**
     * Sets the classpath.
     * @param classpath the classpath
     * @return this task object
     */
    public JavadocTask classpath(List<Path> classpath) {
        this.classpath = classpath;
        return this;
    }

    /**
     * Sets the sourcepath.
     * @param sourcepath the sourcepath
     * @return this task object
     */
    public JavadocTask sourcepath(String sourcepath) {
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
    public JavadocTask sourcepath(Path... sourcepath) {
        this.sourcepath = Arrays.asList(sourcepath);
        return this;
    }

    /**
     * Sets the sourcepath.
     * @param sourcepath the sourcepath
     * @return this task object
     */
    public JavadocTask sourcepath(List<Path> sourcepath) {
        this.sourcepath = sourcepath;
        return this;
    }

    /**
     * Sets the output directory.
     * @param outdir the output directory
     * @return this task object
     */
    public JavadocTask outdir(String outdir) {
        this.outdir = Paths.get(outdir);
        return this;
    }

    /**
     * Sets the output directory.
     * @param outdir the output directory
     * @return this task object
     */
    public JavadocTask outdir(Path outdir) {
        this.outdir = outdir;
        return this;
    }

    /**
     * Sets the options.
     * @param options the options
     * @return this task object
     */
    public JavadocTask options(String... options) {
        this.options = Arrays.asList(options);
        return this;
    }

    /**
     * Sets the options.
     * @param options the options
     * @return this task object
     */
    public JavadocTask options(List<String> options) {
        this.options = options;
        return this;
    }

    /**
     * Sets the files to be documented.
     * @param files the files
     * @return this task object
     */
    public JavadocTask files(String... files) {
        this.files = Arrays.asList(files);
        return this;
    }

    /**
     * Sets the files to be documented.
     * @param files the files
     * @return this task object
     */
    public JavadocTask files(Path... files) {
        this.files = Stream.of(files)
                .map(Path::toString)
                .collect(Collectors.toList());
        return this;
    }

    /**
     * Sets the files to be documented.
     * @param files the files
     * @return this task object
     */
    public JavadocTask files(List<Path> files) {
        this.files = files.stream()
                .map(Path::toString)
                .collect(Collectors.toList());
        return this;
    }

    /**
     * Sets the sources to be documented.
     * Each source string is converted into an in-memory object that
     * can be passed directly to the tool.
     * @param sources the sources
     * @return this task object
     */
    public JavadocTask sources(String... sources) {
        fileObjects = Stream.of(sources)
                .map(s -> new ToolBox.JavaSource(s))
                .collect(Collectors.toList());
        return this;
    }

    /**
     * Sets the file manager to be used by this task.
     * @param fileManager the file manager
     * @return this task object
     */
    public JavadocTask fileManager(JavaFileManager fileManager) {
        this.fileManager = fileManager;
        return this;
    }

    /**
     * Sets the doclet class to be invoked by javadoc.
     * Note: this is applicable only in API mode.
     * @param docletClass the user specified doclet
     * @return this task object
     */
    public JavadocTask docletClass(Class<?> docletClass) {
        this.docletClass = docletClass;
        return this;
    }

    /**
     * {@inheritDoc}
     * @return the name "javadoc"
     */
    @Override
    public String name() {
        return "javadoc";
    }

    /**
     * Calls the javadoc tool with the arguments as currently configured.
     * @return a Result object indicating the outcome of the execution
     * and the content of any output written to stdout, stderr, or the
     * main stream by the tool.
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
                    rc = runCommand(direct.pw);
                    break;
                default:
                    throw new IllegalStateException();
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
            jdtool = (JavadocTool) ToolProvider.getSystemDocumentationTool();
            jdtool = new JavadocTool();

            if (fileManager == null)
                fileManager = internalFileManager = jdtool.getStandardFileManager(null, null, null);
            if (outdir != null)
                setLocationFromPaths(DocumentationTool.Location.DOCUMENTATION_OUTPUT,
                        Collections.singletonList(outdir));
            if (classpath != null)
                setLocationFromPaths(StandardLocation.CLASS_PATH, classpath);
            if (sourcepath != null)
                setLocationFromPaths(StandardLocation.SOURCE_PATH, sourcepath);
            List<String> allOpts = new ArrayList<>();
            if (options != null)
                allOpts.addAll(options);

            Iterable<? extends JavaFileObject> allFiles = joinFiles(files, fileObjects);
            DocumentationTask task = jdtool.getTask(pw,
                    fileManager,
                    null,  // diagnostic listener; should optionally collect diags
                    docletClass,
                    allOpts,
                    allFiles);
            return ((DocumentationTask) task).call() ? 0 : 1;
        } finally {
            if (internalFileManager != null)
                internalFileManager.close();
        }
    }

    private void setLocationFromPaths(Location location, List<Path> files) throws IOException {
        if (!(fileManager instanceof StandardJavaFileManager))
            throw new IllegalStateException("not a StandardJavaFileManager");
        ((StandardJavaFileManager) fileManager).setLocationFromPaths(location, files);
    }

    private int runCommand(PrintWriter pw) {
        List<String> args = getAllArgs();
        String[] argsArray = args.toArray(new String[args.size()]);
        return jdk.javadoc.internal.tool.Main.execute(argsArray, pw);
    }

    private Task.Result runExec() {
        List<String> args = new ArrayList<>();
        Path javadoc = toolBox.getJDKTool("javadoc");
        args.add(javadoc.toString());
        if (includeStandardOptions) {
            args.addAll(toolBox.split(System.getProperty("test.tool.vm.opts"), " +"));
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
            internalFileManager = jdtool.getStandardFileManager(null, null, null);
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
