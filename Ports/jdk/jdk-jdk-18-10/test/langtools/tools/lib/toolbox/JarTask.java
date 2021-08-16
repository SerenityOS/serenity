/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import static toolbox.ToolBox.currDir;

/**
 * A task to configure and run the jar file utility.
 */
public class JarTask extends AbstractTask<JarTask> {
    private Path jar;
    private Manifest manifest;
    private String classpath;
    private String mainClass;
    private Path baseDir;
    private List<Path> paths;
    private Set<FileObject> fileObjects;

    /**
     * Creates a task to write jar files, using API mode.
     * @param toolBox the {@code ToolBox} to use
     */
    public JarTask(ToolBox toolBox) {
        super(toolBox, Task.Mode.API);
        paths = Collections.emptyList();
        fileObjects = new LinkedHashSet<>();
    }

    /**
     * Creates a JarTask for use with a given jar file.
     * @param toolBox the {@code ToolBox} to use
     * @param path the file
     */
    public JarTask(ToolBox toolBox, String path) {
        this(toolBox);
        jar = Paths.get(path);
    }

    /**
     * Creates a JarTask for use with a given jar file.
     * @param toolBox the {@code ToolBox} to use
     * @param path the file
     */
    public JarTask(ToolBox toolBox, Path path) {
        this(toolBox);
        jar = path;
    }

    /**
     * Sets a manifest for the jar file.
     * @param manifest the manifest
     * @return this task object
     */
    public JarTask manifest(Manifest manifest) {
        this.manifest = manifest;
        return this;
    }

    /**
     * Sets a manifest for the jar file.
     * @param manifest a string containing the contents of the manifest
     * @return this task object
     * @throws IOException if there is a problem creating the manifest
     */
    public JarTask manifest(String manifest) throws IOException {
        this.manifest = new Manifest(new ByteArrayInputStream(manifest.getBytes()));
        return this;
    }

    /**
     * Sets the classpath to be written to the {@code Class-Path}
     * entry in the manifest.
     * @param classpath the classpath
     * @return this task object
     */
    public JarTask classpath(String classpath) {
        this.classpath = classpath;
        return this;
    }

    /**
     * Sets the class to be written to the {@code Main-Class}
     * entry in the manifest..
     * @param mainClass the name of the main class
     * @return this task object
     */
    public JarTask mainClass(String mainClass) {
        this.mainClass = mainClass;
        return this;
    }

    /**
     * Sets the base directory for files to be written into the jar file.
     * @param baseDir the base directory
     * @return this task object
     */
    public JarTask baseDir(String baseDir) {
        this.baseDir = Paths.get(baseDir);
        return this;
    }

    /**
     * Sets the base directory for files to be written into the jar file.
     * @param baseDir the base directory
     * @return this task object
     */
    public JarTask baseDir(Path baseDir) {
        this.baseDir = baseDir;
        return this;
    }

    /**
     * Sets the files to be written into the jar file.
     * @param files the files
     * @return this task object
     */
    public JarTask files(String... files) {
        this.paths = Stream.of(files)
                .map(file -> Paths.get(file))
                .collect(Collectors.toList());
        return this;
    }

    /**
     * Adds a set of file objects to be written into the jar file, by copying them
     * from a Location in a JavaFileManager.
     * The file objects to be written are specified by a series of paths;
     * each path can be in one of the following forms:
     * <ul>
     * <li>The name of a class. For example, java.lang.Object.
     * In this case, the corresponding .class file will be written to the jar file.
     * <li>the name of a package followed by {@code .*}. For example, {@code java.lang.*}.
     * In this case, all the class files in the specified package will be written to
     * the jar file.
     * <li>the name of a package followed by {@code .**}. For example, {@code java.lang.**}.
     * In this case, all the class files in the specified package, and any subpackages
     * will be written to the jar file.
     * </ul>
     *
     * @param fm the file manager in which to find the file objects
     * @param l  the location in which to find the file objects
     * @param paths the paths specifying the file objects to be copied
     * @return this task object
     * @throws IOException if errors occur while determining the set of file objects
     */
    public JarTask files(JavaFileManager fm, JavaFileManager.Location l, String... paths)
            throws IOException {
        for (String p : paths) {
            if (p.endsWith(".**"))
                addPackage(fm, l, p.substring(0, p.length() - 3), true);
            else if (p.endsWith(".*"))
                addPackage(fm, l, p.substring(0, p.length() - 2), false);
            else
                addFile(fm, l, p);
        }
        return this;
    }

    private void addPackage(JavaFileManager fm, JavaFileManager.Location l, String pkg, boolean recurse)
            throws IOException {
        for (JavaFileObject fo : fm.list(l, pkg, EnumSet.allOf(JavaFileObject.Kind.class), recurse)) {
            fileObjects.add(fo);
        }
    }

    private void addFile(JavaFileManager fm, JavaFileManager.Location l, String path) throws IOException {
        JavaFileObject fo = fm.getJavaFileForInput(l, path, JavaFileObject.Kind.CLASS);
        fileObjects.add(fo);
    }

    /**
     * Provides limited jar command-like functionality.
     * The supported commands are:
     * <ul>
     * <li> jar cf jarfile -C dir files...
     * <li> jar cfm jarfile manifestfile -C dir files...
     * </ul>
     * Any values specified by other configuration methods will be ignored.
     * @param args arguments in the style of those for the jar command
     * @return a Result object containing the results of running the task
     */
    public Task.Result run(String... args) {
        if (args.length < 2)
            throw new IllegalArgumentException();

        ListIterator<String> iter = Arrays.asList(args).listIterator();
        String first = iter.next();
        switch (first) {
            case "cf":
                jar = Paths.get(iter.next());
                break;
            case "cfm":
                jar = Paths.get(iter.next());
                try (InputStream in = Files.newInputStream(Paths.get(iter.next()))) {
                    manifest = new Manifest(in);
                } catch (IOException e) {
                    throw new IOError(e);
                }
                break;
        }

        if (iter.hasNext()) {
            if (iter.next().equals("-C"))
                baseDir = Paths.get(iter.next());
            else
                iter.previous();
        }

        paths = new ArrayList<>();
        while (iter.hasNext())
            paths.add(Paths.get(iter.next()));

        return run();
    }

    /**
     * {@inheritDoc}
     * @return the name "jar"
     */
    @Override
    public String name() {
        return "jar";
    }

    /**
     * Creates a jar file with the arguments as currently configured.
     * @return a Result object indicating the outcome of the compilation
     * and the content of any output written to stdout, stderr, or the
     * main stream by the compiler.
     * @throws TaskError if the outcome of the task is not as expected.
     */
    @Override
    public Task.Result run() {
        Manifest m = (manifest == null) ? new Manifest() : manifest;
        Attributes mainAttrs = m.getMainAttributes();
        if (mainClass != null)
            mainAttrs.put(Attributes.Name.MAIN_CLASS, mainClass);
        if (classpath != null)
            mainAttrs.put(Attributes.Name.CLASS_PATH, classpath);

        AbstractTask.StreamOutput sysOut = new AbstractTask.StreamOutput(System.out, System::setOut);
        AbstractTask.StreamOutput sysErr = new AbstractTask.StreamOutput(System.err, System::setErr);

        Map<Task.OutputKind, String> outputMap = new HashMap<>();

        try (OutputStream os = Files.newOutputStream(jar);
                JarOutputStream jos = openJar(os, m)) {
            writeFiles(jos);
            writeFileObjects(jos);
        } catch (IOException e) {
            error("Exception while opening " + jar, e);
        } finally {
            outputMap.put(Task.OutputKind.STDOUT, sysOut.close());
            outputMap.put(Task.OutputKind.STDERR, sysErr.close());
        }
        return checkExit(new Task.Result(toolBox, this, (errors == 0) ? 0 : 1, outputMap));
    }

    private JarOutputStream openJar(OutputStream os, Manifest m) throws IOException {
        if (m == null || m.getMainAttributes().isEmpty() && m.getEntries().isEmpty()) {
            return new JarOutputStream(os);
        } else {
            if (m.getMainAttributes().get(Attributes.Name.MANIFEST_VERSION) == null)
                m.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            return new JarOutputStream(os, m);
        }
    }

    private void writeFiles(JarOutputStream jos) throws IOException {
            Path base = (baseDir == null) ? currDir : baseDir;
            for (Path path : paths) {
                Files.walkFileTree(base.resolve(path), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        try {
                        String p = base.relativize(file)
                                .normalize()
                                .toString()
                                .replace(File.separatorChar, '/');
                        JarEntry e = new JarEntry(p);
                            jos.putNextEntry(e);
                        try {
                            jos.write(Files.readAllBytes(file));
                        } finally {
                            jos.closeEntry();
                        }
                            return FileVisitResult.CONTINUE;
                        } catch (IOException e) {
                        error("Exception while adding " + file + " to jar file", e);
                            return FileVisitResult.TERMINATE;
                        }
                    }
                });
            }
    }

    private void writeFileObjects(JarOutputStream jos) throws IOException {
        for (FileObject fo : fileObjects) {
            String p = guessPath(fo);
            JarEntry e = new JarEntry(p);
            jos.putNextEntry(e);
            try {
                byte[] buf = new byte[1024];
                try (BufferedInputStream in = new BufferedInputStream(fo.openInputStream())) {
                    int n;
                    while ((n = in.read(buf)) > 0)
                        jos.write(buf, 0, n);
                } catch (IOException ex) {
                    error("Exception while adding " + fo.getName() + " to jar file", ex);
                }
        } finally {
                jos.closeEntry();
        }
        }
    }

    /*
     * A jar: URL is of the form  jar:URL!/<entry>  where URL is a URL for the .jar file itself.
     * In Symbol files (i.e. ct.sym) the underlying entry is prefixed META-INF/sym/<base>.
     */
    private final Pattern jarEntry = Pattern.compile(".*!/(?:META-INF/sym/[^/]+/)?(.*)");

    /*
     * A jrt: URL is of the form  jrt:/<module>/<package>/<file>
     */
    private final Pattern jrtEntry = Pattern.compile("/([^/]+)/(.*)");

    /*
     * A file: URL is of the form  file:/path/to/{modules,patches}/<module>/<package>/<file>
     */
    private final Pattern fileEntry = Pattern.compile(".*/(?:modules|patches)/([^/]+)/(.*)");

    private String guessPath(FileObject fo) {
        URI u = fo.toUri();
        switch (u.getScheme()) {
            case "jar": {
                Matcher m = jarEntry.matcher(u.getSchemeSpecificPart());
                if (m.matches()) {
                    return m.group(1);
                }
                break;
            }
            case "jrt": {
                Matcher m = jrtEntry.matcher(u.getSchemeSpecificPart());
                if (m.matches()) {
                    return m.group(2);
                }
                break;
            }
            case "file": {
                Matcher m = fileEntry.matcher(u.getSchemeSpecificPart());
                if (m.matches()) {
                    return m.group(2);
                }
                break;
            }
        }
        throw new IllegalArgumentException(fo.getName() + "--" + fo.toUri());
    }

    private void error(String message, Throwable t) {
        toolBox.out.println("Error: " + message + ": " + t);
        errors++;
    }

    private int errors;
}
