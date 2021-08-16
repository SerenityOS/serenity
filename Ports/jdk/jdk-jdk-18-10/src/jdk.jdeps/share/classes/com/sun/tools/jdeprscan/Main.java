/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package com.sun.tools.jdeprscan;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.platform.JDKPlatformProvider;

import com.sun.tools.jdeprscan.scan.Scan;

import static java.util.stream.Collectors.*;

import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

/**
 * Deprecation Scanner tool. Loads API deprecation information from the
 * JDK image, or optionally, from a jar file or class hierarchy. Then scans
 * a class library for usages of those APIs.
 *
 * TODO:
 *  - audit error handling throughout, but mainly in scan package
 *  - handling of covariant overrides
 *  - handling of override of method found in multiple superinterfaces
 *  - convert type/method/field output to Java source like syntax, e.g.
 *      instead of java/lang/Character.isJavaLetter(C)Z
 *      print void java.lang.Character.isJavaLetter(char)boolean
 *  - more example output in man page
 *  - more rigorous GNU style option parsing; use joptsimple?
 *
 * FUTURES:
 *  - add module support: --add-modules, --module-path, module arg
 *  - load deprecation declarations from a designated class library instead
 *    of the JDK
 *  - load deprecation declarations from a module
 *  - scan a module (but a modular jar can be treated just a like an ordinary jar)
 *  - multi-version jar
 */
public class Main implements DiagnosticListener<JavaFileObject> {
    final PrintStream out;
    final PrintStream err;
    final List<File> bootClassPath = new ArrayList<>();
    final List<File> classPath = new ArrayList<>();
    final List<File> systemModules = new ArrayList<>();
    final List<String> options = new ArrayList<>();
    final List<String> comments = new ArrayList<>();

    // Valid releases need to match what the compiler supports.
    // Keep these updated manually until there's a compiler API
    // that allows querying of supported releases.
    final Set<String> releasesWithoutForRemoval = Set.of("6", "7", "8");
    final Set<String> releasesWithForRemoval = // "9", "10", "11", ...
        IntStream.rangeClosed(9, Runtime.version().feature())
        .mapToObj(Integer::toString)
        .collect(Collectors.toUnmodifiableSet());

    final Set<String> validReleases;
    {
        Set<String> temp = new HashSet<>(releasesWithoutForRemoval);
        temp.addAll(releasesWithForRemoval);
        validReleases = Set.of(temp.toArray(new String[0]));
    }

    boolean verbose = false;
    boolean forRemoval = false;

    final JavaCompiler compiler;
    final StandardJavaFileManager fm;

    List<DeprData> deprList; // non-null after successful load phase

    /**
     * Processes a collection of class names. Names should fully qualified
     * names in the form "pkg.pkg.pkg.classname".
     *
     * @param classNames collection of fully qualified classnames to process
     * @return true for success, false for failure
     * @throws IOException if an I/O error occurs
     */
    boolean doClassNames(Collection<String> classNames) throws IOException {
        if (verbose) {
            out.println("List of classes to process:");
            classNames.forEach(out::println);
            out.println("End of class list.");
        }

        // TODO: not sure this is necessary...
        if (fm instanceof JavacFileManager) {
            ((JavacFileManager)fm).setSymbolFileEnabled(false);
        }

        fm.setLocation(StandardLocation.CLASS_PATH, classPath);
        if (!bootClassPath.isEmpty()) {
            fm.setLocation(StandardLocation.PLATFORM_CLASS_PATH, bootClassPath);
        }

        if (!systemModules.isEmpty()) {
            fm.setLocation(StandardLocation.SYSTEM_MODULES, systemModules);
        }

        LoadProc proc = new LoadProc();
        JavaCompiler.CompilationTask task =
            compiler.getTask(null, fm, this, options, classNames, null);
        task.setProcessors(List.of(proc));
        boolean r = task.call();
        if (r) {
            if (forRemoval) {
                deprList = proc.getDeprecations().stream()
                               .filter(DeprData::isForRemoval)
                               .toList();
            } else {
                deprList = proc.getDeprecations();
            }
        }
        return r;
    }

    /**
     * Processes a stream of filenames (strings). The strings are in the
     * form pkg/pkg/pkg/classname.class relative to the root of a package
     * hierarchy.
     *
     * @param filenames a Stream of filenames to process
     * @return true for success, false for failure
     * @throws IOException if an I/O error occurs
     */
    boolean doFileNames(Stream<String> filenames) throws IOException {
        return doClassNames(
            filenames.filter(name -> name.endsWith(".class"))
                     .filter(name -> !name.endsWith("package-info.class"))
                     .filter(name -> !name.endsWith("module-info.class"))
                     .map(s -> s.replaceAll("\\.class$", ""))
                     .map(s -> s.replace(File.separatorChar, '.'))
                     .toList());
    }

    /**
     * Replaces all but the first occurrence of '/' with '.'. Assumes
     * that the name is in the format module/pkg/pkg/classname.class.
     * That is, the name should contain at least one '/' character
     * separating the module name from the package-class name.
     *
     * @param filename the input filename
     * @return the modular classname
     */
    String convertModularFileName(String filename) {
        int slash = filename.indexOf('/');
        return filename.substring(0, slash)
               + "/"
               + filename.substring(slash+1).replace('/', '.');
    }

    /**
     * Processes a stream of filenames (strings) including a module prefix.
     * The strings are in the form module/pkg/pkg/pkg/classname.class relative
     * to the root of a directory containing modules. The strings are processed
     * into module-qualified class names of the form
     * "module/pkg.pkg.pkg.classname".
     *
     * @param filenames a Stream of filenames to process
     * @return true for success, false for failure
     * @throws IOException if an I/O error occurs
     */
    boolean doModularFileNames(Stream<String> filenames) throws IOException {
        return doClassNames(
            filenames.filter(name -> name.endsWith(".class"))
                     .filter(name -> !name.endsWith("package-info.class"))
                     .filter(name -> !name.endsWith("module-info.class"))
                     .map(s -> s.replaceAll("\\.class$", ""))
                     .map(this::convertModularFileName)
                     .toList());
    }

    /**
     * Processes named class files in the given directory. The directory
     * should be the root of a package hierarchy. If classNames is
     * empty, walks the directory hierarchy to find all classes.
     *
     * @param dirname the name of the directory to process
     * @param classNames the names of classes to process
     * @return true for success, false for failure
     * @throws IOException if an I/O error occurs
     */
    boolean processDirectory(String dirname, Collection<String> classNames) throws IOException {
        if (!Files.isDirectory(Paths.get(dirname))) {
            err.printf("%s: not a directory%n", dirname);
            return false;
        }

        classPath.add(0, new File(dirname));

        if (classNames.isEmpty()) {
            Path base = Paths.get(dirname);
            int baseCount = base.getNameCount();
            try (Stream<Path> paths = Files.walk(base)) {
                Stream<String> files =
                    paths.filter(p -> p.getNameCount() > baseCount)
                         .map(p -> p.subpath(baseCount, p.getNameCount()))
                         .map(Path::toString);
                return doFileNames(files);
            }
        } else {
            return doClassNames(classNames);
        }
    }

    /**
     * Processes all class files in the given jar file.
     *
     * @param jarname the name of the jar file to process
     * @return true for success, false for failure
     * @throws IOException if an I/O error occurs
     */
    boolean doJarFile(String jarname) throws IOException {
        try (JarFile jf = new JarFile(jarname)) {
            Stream<String> files =
                jf.stream()
                  .map(JarEntry::getName);
            return doFileNames(files);
        }
    }

    /**
     * Processes named class files from the given jar file,
     * or all classes if classNames is empty.
     *
     * @param jarname the name of the jar file to process
     * @param classNames the names of classes to process
     * @return true for success, false for failure
     * @throws IOException if an I/O error occurs
     */
    boolean processJarFile(String jarname, Collection<String> classNames) throws IOException {
        classPath.add(0, new File(jarname));

        if (classNames.isEmpty()) {
            return doJarFile(jarname);
        } else {
            return doClassNames(classNames);
        }
    }

    /**
     * Processes named class files from rt.jar of a JDK version 7 or 8.
     * If classNames is empty, processes all classes.
     *
     * @param jdkHome the path to the "home" of the JDK to process
     * @param classNames the names of classes to process
     * @return true for success, false for failure
     * @throws IOException if an I/O error occurs
     */
    boolean processOldJdk(String jdkHome, Collection<String> classNames) throws IOException {
        String RTJAR = jdkHome + "/jre/lib/rt.jar";
        String CSJAR = jdkHome + "/jre/lib/charsets.jar";

        bootClassPath.add(0, new File(RTJAR));
        bootClassPath.add(1, new File(CSJAR));
        options.add("-source");
        options.add("8");

        if (classNames.isEmpty()) {
            return doJarFile(RTJAR);
        } else {
            return doClassNames(classNames);
        }
    }

    /**
     * Processes listed classes given a JDK 9 home.
     */
    boolean processJdk9(String jdkHome, Collection<String> classes) throws IOException {
        systemModules.add(new File(jdkHome));
        return doClassNames(classes);
    }

    /**
     * Processes the class files from the currently running JDK,
     * using the jrt: filesystem.
     *
     * @return true for success, false for failure
     * @throws IOException if an I/O error occurs
     */
    boolean processSelf(Collection<String> classes) throws IOException {
        options.add("--add-modules");
        options.add("java.se");

        if (classes.isEmpty()) {
            Path modules = FileSystems.getFileSystem(URI.create("jrt:/"))
                                      .getPath("/modules");

            // names are /modules/<modulename>/pkg/.../Classname.class
            try (Stream<Path> paths = Files.walk(modules)) {
                Stream<String> files =
                    paths.filter(p -> p.getNameCount() > 2)
                         .map(p -> p.subpath(1, p.getNameCount()))
                         .map(Path::toString);
                return doModularFileNames(files);
            }
        } else {
            return doClassNames(classes);
        }
    }

    /**
     * Process classes from a particular JDK release, using only information
     * in this JDK.
     *
     * @param release a supported release version, like "8" or "10".
     * @param classes collection of classes to process, may be empty
     * @return success value
     */
    boolean processRelease(String release, Collection<String> classes) throws IOException {
        boolean hasModules;
        boolean hasJavaSE_EE;

        try {
            int releaseNum = Integer.parseInt(release);

            hasModules = releaseNum >= 9;
            hasJavaSE_EE = hasModules && releaseNum <= 10;
        } catch (NumberFormatException ex) {
            hasModules = true;
            hasJavaSE_EE = false;
        }

        options.addAll(List.of("--release", release));

        if (hasModules) {
            List<String> rootMods = hasJavaSE_EE ? List.of("java.se", "java.se.ee")
                                                 : List.of("java.se");
            TraverseProc proc = new TraverseProc(rootMods);
            JavaCompiler.CompilationTask task =
                compiler.getTask(null, fm, this,
                                 // options
                                 List.of("--add-modules", String.join(",", rootMods),
                                         "--release", release),
                                 // classes
                                 List.of("java.lang.Object"),
                                 null);
            task.setProcessors(List.of(proc));
            if (!task.call()) {
                return false;
            }
            Map<PackageElement, List<TypeElement>> types = proc.getPublicTypes();
            options.add("--add-modules");
            options.add(String.join(",", rootMods));
            return doClassNames(
                types.values().stream()
                     .flatMap(List::stream)
                     .map(TypeElement::toString)
                     .toList());
        } else {
            JDKPlatformProvider pp = new JDKPlatformProvider();
            if (StreamSupport.stream(pp.getSupportedPlatformNames().spliterator(),
                                 false)
                             .noneMatch(n -> n.equals(release))) {
                return false;
            }
            JavaFileManager fm = pp.getPlatform(release, "").getFileManager();
            List<String> classNames = new ArrayList<>();
            for (JavaFileObject fo : fm.list(StandardLocation.PLATFORM_CLASS_PATH,
                                             "",
                                             EnumSet.of(Kind.CLASS),
                                             true)) {
                classNames.add(fm.inferBinaryName(StandardLocation.PLATFORM_CLASS_PATH, fo));
            }

            options.add("-Xlint:-options");

            return doClassNames(classNames);
        }
    }

    /**
     * An enum denoting the mode in which the tool is running.
     * Different modes correspond to the different process* methods.
     * The exception is UNKNOWN, which indicates that a mode wasn't
     * specified on the command line, which is an error.
     */
    static enum LoadMode {
        CLASSES, DIR, JAR, OLD_JDK, JDK9, SELF, RELEASE, LOAD_CSV
    }

    static enum ScanMode {
        ARGS, LIST, PRINT_CSV
    }

    /**
     * A checked exception that's thrown if a command-line syntax error
     * is detected.
     */
    static class UsageException extends Exception {
        private static final long serialVersionUID = 3611828659572908743L;
    }

    /**
     * Convenience method to throw UsageException if a condition is false.
     *
     * @param cond the condition that's required to be true
     * @throws UsageException
     */
    void require(boolean cond) throws UsageException {
        if (!cond) {
            throw new UsageException();
        }
    }

    /**
     * Constructs an instance of the finder tool.
     *
     * @param out the stream to which the tool's output is sent
     * @param err the stream to which error messages are sent
     */
    Main(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
        compiler = ToolProvider.getSystemJavaCompiler();
        fm = compiler.getStandardFileManager(this, null, StandardCharsets.UTF_8);
    }

    /**
     * Prints the diagnostic to the err stream.
     *
     * Specified by the DiagnosticListener interface.
     *
     * @param diagnostic the tool diagnostic to print
     */
    @Override
    public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
        err.println(diagnostic);
    }

    /**
     * Parses arguments and performs the requested processing.
     *
     * @param argArray command-line arguments
     * @return true on success, false on error
     */
    boolean run(String... argArray) {
        Queue<String> args = new ArrayDeque<>(Arrays.asList(argArray));
        LoadMode loadMode = LoadMode.RELEASE;
        ScanMode scanMode = ScanMode.ARGS;
        String dir = null;
        String jar = null;
        String jdkHome = null;
        String release = Integer.toString(Runtime.version().feature());
        List<String> loadClasses = new ArrayList<>();
        String csvFile = null;

        try {
            while (!args.isEmpty()) {
                String a = args.element();
                if (a.startsWith("-")) {
                    args.remove();
                    switch (a) {
                        case "--class-path":
                            classPath.clear();
                            Arrays.stream(args.remove().split(File.pathSeparator))
                                  .map(File::new)
                                  .forEachOrdered(classPath::add);
                            break;
                        case "--for-removal":
                            forRemoval = true;
                            break;
                        case "--full-version":
                            out.println(System.getProperty("java.vm.version"));
                            return false;
                        case "--help":
                        case "-h":
                        case "-?":
                            printHelp(out);
                            out.println();
                            out.println(Messages.get("main.help"));
                            return true;
                        case "-l":
                        case "--list":
                            require(scanMode == ScanMode.ARGS);
                            scanMode = ScanMode.LIST;
                            break;
                        case "--release":
                            loadMode = LoadMode.RELEASE;
                            release = args.remove();
                            if (!validReleases.contains(release)) {
                                throw new UsageException();
                            }
                            break;
                        case "-v":
                        case "--verbose":
                            verbose = true;
                            break;
                        case "--version":
                            out.println(System.getProperty("java.version"));
                            return false;
                        case "--Xcompiler-arg":
                            options.add(args.remove());
                            break;
                        case "--Xcsv-comment":
                            comments.add(args.remove());
                            break;
                        case "--Xhelp":
                            out.println(Messages.get("main.xhelp"));
                            return false;
                        case "--Xload-class":
                            loadMode = LoadMode.CLASSES;
                            loadClasses.add(args.remove());
                            break;
                        case "--Xload-csv":
                            loadMode = LoadMode.LOAD_CSV;
                            csvFile = args.remove();
                            break;
                        case "--Xload-dir":
                            loadMode = LoadMode.DIR;
                            dir = args.remove();
                            break;
                        case "--Xload-jar":
                            loadMode = LoadMode.JAR;
                            jar = args.remove();
                            break;
                        case "--Xload-jdk9":
                            loadMode = LoadMode.JDK9;
                            jdkHome = args.remove();
                            break;
                        case "--Xload-old-jdk":
                            loadMode = LoadMode.OLD_JDK;
                            jdkHome = args.remove();
                            break;
                        case "--Xload-self":
                            loadMode = LoadMode.SELF;
                            break;
                        case "--Xprint-csv":
                            require(scanMode == ScanMode.ARGS);
                            scanMode = ScanMode.PRINT_CSV;
                            break;
                        default:
                            throw new UsageException();
                    }
                } else {
                    break;
                }
            }

            if ((scanMode == ScanMode.ARGS) == args.isEmpty()) {
                throw new UsageException();
            }

            if (    forRemoval && loadMode == LoadMode.RELEASE &&
                    releasesWithoutForRemoval.contains(release)) {
                throw new UsageException();
            }

            boolean success = false;

            switch (loadMode) {
                case CLASSES:
                    success = doClassNames(loadClasses);
                    break;
                case DIR:
                    success = processDirectory(dir, loadClasses);
                    break;
                case JAR:
                    success = processJarFile(jar, loadClasses);
                    break;
                case JDK9:
                    require(!args.isEmpty());
                    success = processJdk9(jdkHome, loadClasses);
                    break;
                case LOAD_CSV:
                    deprList = DeprDB.loadFromFile(csvFile);
                    success = true;
                    break;
                case OLD_JDK:
                    success = processOldJdk(jdkHome, loadClasses);
                    break;
                case RELEASE:
                    success = processRelease(release, loadClasses);
                    break;
                case SELF:
                    success = processSelf(loadClasses);
                    break;
                default:
                    throw new UsageException();
            }

            if (!success) {
                return false;
            }
        } catch (NoSuchElementException | UsageException ex) {
            printHelp(err);
            return false;
        } catch (IOException ioe) {
            if (verbose) {
                ioe.printStackTrace(err);
            } else {
                err.println(ioe);
            }
            return false;
        }

        // now the scanning phase

        boolean scanStatus = true;

        switch (scanMode) {
            case LIST:
                for (DeprData dd : deprList) {
                    if (!forRemoval || dd.isForRemoval()) {
                        out.println(Pretty.print(dd));
                    }
                }
                break;
            case PRINT_CSV:
                out.println("#jdepr1");
                comments.forEach(s -> out.println("# " + s));
                for (DeprData dd : deprList) {
                    CSV.write(out, dd.kind, dd.typeName, dd.nameSig, dd.since, dd.forRemoval);
                }
                break;
            case ARGS:
                DeprDB db = DeprDB.loadFromList(deprList);
                List<String> cp = classPath.stream()
                                           .map(File::toString)
                                           .toList();
                Scan scan = new Scan(out, err, cp, db, verbose);

                for (String a : args) {
                    boolean s;
                    if (a.endsWith(".jar")) {
                        s = scan.scanJar(a);
                    } else if (a.endsWith(".class")) {
                        s = scan.processClassFile(a);
                    } else if (Files.isDirectory(Paths.get(a))) {
                        s = scan.scanDir(a);
                    } else {
                        s = scan.processClassName(a.replace('.', '/'));
                    }
                    scanStatus = scanStatus && s;
                }
                break;
        }

        return scanStatus;
    }

    private void printHelp(PrintStream out) {
        JDKPlatformProvider pp = new JDKPlatformProvider();
        String supportedReleases =
                String.join("|", pp.getSupportedPlatformNames());
        out.println(Messages.get("main.usage", supportedReleases));
    }

    /**
     * Programmatic main entry point: initializes the tool instance to
     * use stdout and stderr; runs the tool, passing command-line args;
     * returns an exit status.
     *
     * @return true on success, false otherwise
     */
    public static boolean call(PrintStream out, PrintStream err, String... args) {
        return new Main(out, err).run(args);
    }

    /**
     * Calls the main entry point and exits the JVM with an exit
     * status determined by the return status.
     */
    public static void main(String[] args) {
        System.exit(call(System.out, System.err, args) ? 0 : 1);
    }
}
