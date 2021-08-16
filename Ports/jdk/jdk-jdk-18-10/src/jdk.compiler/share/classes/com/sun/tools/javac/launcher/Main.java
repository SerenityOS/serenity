/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.launcher;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;

import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.code.Source;
import com.sun.tools.javac.resources.LauncherProperties.Errors;
import com.sun.tools.javac.util.JCDiagnostic.Error;

import jdk.internal.misc.VM;

import static javax.tools.JavaFileObject.Kind.SOURCE;

/**
 * Compiles a source file, and executes the main method it contains.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own
 * risk.  This code and its internal interfaces are subject to change
 * or deletion without notice.</b></p>
 */
public class Main {
    /**
     * An exception used to report errors.
     */
    public class Fault extends Exception {
        private static final long serialVersionUID = 1L;
        Fault(Error error) {
            super(Main.this.getMessage(error));
        }
    }

    /**
     * Compiles a source file, and executes the main method it contains.
     *
     * <p>This is normally invoked from the Java launcher, either when
     * the {@code --source} option is used, or when the first argument
     * that is not part of a runtime option ends in {@code .java}.
     *
     * <p>The first entry in the {@code args} array is the source file
     * to be compiled and run; all subsequent entries are passed as
     * arguments to the main method of the first class found in the file.
     *
     * <p>If any problem occurs before executing the main class, it will
     * be reported to the standard error stream, and the the JVM will be
     * terminated by calling {@code System.exit} with a non-zero return code.
     *
     * @param args the arguments
     * @throws Throwable if the main method throws an exception
     */
    public static void main(String... args) throws Throwable {
        try {
            new Main(System.err).run(VM.getRuntimeArguments(), args);
        } catch (Fault f) {
            System.err.println(f.getMessage());
            System.exit(1);
        } catch (InvocationTargetException e) {
            // leave VM to handle the stacktrace, in the standard manner
            throw e.getCause();
        }
    }

    /** Stream for reporting errors, such as compilation errors. */
    private PrintWriter out;

    /**
     * Creates an instance of this class, providing a stream to which to report
     * any errors.
     *
     * @param out the stream
     */
    public Main(PrintStream out) {
        this(new PrintWriter(new OutputStreamWriter(out), true));
    }

    /**
     * Creates an instance of this class, providing a stream to which to report
     * any errors.
     *
     * @param out the stream
     */
    public Main(PrintWriter out) {
        this.out = out;
    }

    /**
     * Compiles a source file, and executes the main method it contains.
     *
     * <p>The first entry in the {@code args} array is the source file
     * to be compiled and run; all subsequent entries are passed as
     * arguments to the main method of the first class found in the file.
     *
     * <p>Options for {@code javac} are obtained by filtering the runtime arguments.
     *
     * <p>If the main method throws an exception, it will be propagated in an
     * {@code InvocationTargetException}. In that case, the stack trace of the
     * target exception will be truncated such that the main method will be the
     * last entry on the stack. In other words, the stack frames leading up to the
     * invocation of the main method will be removed.
     *
     * @param runtimeArgs the runtime arguments
     * @param args the arguments
     * @throws Fault if a problem is detected before the main method can be executed
     * @throws InvocationTargetException if the main method throws an exception
     */
    public void run(String[] runtimeArgs, String[] args) throws Fault, InvocationTargetException {
        Path file = getFile(args);

        Context context = new Context(file.toAbsolutePath());
        String mainClassName = compile(file, getJavacOpts(runtimeArgs), context);

        String[] appArgs = Arrays.copyOfRange(args, 1, args.length);
        execute(mainClassName, appArgs, context);
    }

    /**
     * Returns the path for the filename found in the first of an array of arguments.
     *
     * @param args the array
     * @return the path, as given in the array of args
     * @throws Fault if there is a problem determining the path, or if the file does not exist
     */
    private Path getFile(String[] args) throws Fault {
        if (args.length == 0) {
            // should not happen when invoked from launcher
            throw new Fault(Errors.NoArgs);
        }
        Path file;
        try {
            file = Paths.get(args[0]);
        } catch (InvalidPathException e) {
            throw new Fault(Errors.InvalidFilename(args[0]));
        }
        if (!Files.exists(file)) {
            // should not happen when invoked from launcher
            throw new Fault(Errors.FileNotFound(file));
        }
        return file;
    }

    /**
     * Reads a source file, ignoring the first line if it is not a Java source file and
     * it begins with {@code #!}.
     *
     * <p>If it is not a Java source file, and if the first two bytes are {@code #!},
     * indicating a "magic number" of an executable text file, the rest of the first line
     * up to but not including the newline is ignored. All characters after the first two are
     * read in the {@link Charset#defaultCharset default platform encoding}.
     *
     * @param file the file
     * @return a file object containing the content of the file
     * @throws Fault if an error occurs while reading the file
     */
    private JavaFileObject readFile(Path file) throws Fault {
        // use a BufferedInputStream to guarantee that we can use mark and reset.
        try (BufferedInputStream in = new BufferedInputStream(Files.newInputStream(file))) {
            boolean ignoreFirstLine;
            if (file.getFileName().toString().endsWith(".java")) {
                ignoreFirstLine = false;
            } else {
                in.mark(2);
                ignoreFirstLine = (in.read() == '#') && (in.read() == '!');
                if (!ignoreFirstLine) {
                    in.reset();
                }
            }
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, Charset.defaultCharset()))) {
                StringBuilder sb = new StringBuilder();
                if (ignoreFirstLine) {
                    r.readLine();
                    sb.append("\n"); // preserve line numbers
                }
                char[] buf = new char[1024];
                int n;
                while ((n = r.read(buf, 0, buf.length)) != -1)  {
                    sb.append(buf, 0, n);
                }
                return new SimpleJavaFileObject(file.toUri(), SOURCE) {
                    @Override
                    public String getName() {
                        return file.toString();
                    }
                    @Override
                    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                        return sb;
                    }
                    @Override
                    public boolean isNameCompatible(String simpleName, JavaFileObject.Kind kind) {
                        // reject package-info and module-info; accept other names
                        return (kind == JavaFileObject.Kind.SOURCE)
                                && SourceVersion.isIdentifier(simpleName);
                    }
                    @Override
                    public String toString() {
                        return "JavacSourceLauncher[" + file + "]";
                    }
                };
            }
        } catch (IOException e) {
            throw new Fault(Errors.CantReadFile(file, e));
        }
    }

    /**
     * Returns the subset of the runtime arguments that are relevant to {@code javac}.
     * Generally, the relevant options are those for setting paths and for configuring the
     * module system.
     *
     * @param runtimeArgs the runtime arguments
     * @return the subset of the runtime arguments
     **/
    private List<String> getJavacOpts(String... runtimeArgs) throws Fault {
        List<String> javacOpts = new ArrayList<>();

        String sourceOpt = System.getProperty("jdk.internal.javac.source");
        if (sourceOpt != null) {
            Source source = Source.lookup(sourceOpt);
            if (source == null) {
                throw new Fault(Errors.InvalidValueForSource(sourceOpt));
            }
            javacOpts.addAll(List.of("--release", sourceOpt));
        }

        for (int i = 0; i < runtimeArgs.length; i++) {
            String arg = runtimeArgs[i];
            String opt = arg, value = null;
            if (arg.startsWith("--")) {
                int eq = arg.indexOf('=');
                if (eq > 0) {
                    opt = arg.substring(0, eq);
                    value = arg.substring(eq + 1);
                }
            }
            switch (opt) {
                // The following options all expect a value, either in the following
                // position, or after '=', for options beginning "--".
                case "--class-path": case "-classpath": case "-cp":
                case "--module-path": case "-p":
                case "--add-exports":
                case "--add-modules":
                case "--limit-modules":
                case "--patch-module":
                case "--upgrade-module-path":
                    if (value == null) {
                        if (i== runtimeArgs.length - 1) {
                            // should not happen when invoked from launcher
                            throw new Fault(Errors.NoValueForOption(opt));
                        }
                        value = runtimeArgs[++i];
                    }
                    if (opt.equals("--add-modules") && value.equals("ALL-DEFAULT")) {
                        // this option is only supported at run time;
                        // it is not required or supported at compile time
                        break;
                    }
                    javacOpts.add(opt);
                    javacOpts.add(value);
                    break;
                case "--enable-preview":
                    javacOpts.add(opt);
                    if (sourceOpt == null) {
                        throw new Fault(Errors.EnablePreviewRequiresSource);
                    }
                    break;
                default:
                    if (opt.startsWith("-agentlib:jdwp=") || opt.startsWith("-Xrunjdwp:")) {
                        javacOpts.add("-g");
                    }
                    // ignore all other runtime args
            }
        }

        // add implicit options
        javacOpts.add("-proc:none");
        javacOpts.add("-Xdiags:verbose");

        return javacOpts;
    }

    /**
     * Compiles a source file, placing the class files in a map in memory.
     * Any messages generated during compilation will be written to the stream
     * provided when this object was created.
     *
     * @param file the source file
     * @param javacOpts compilation options for {@code javac}
     * @param context the context for the compilation
     * @return the name of the first class found in the source file
     * @throws Fault if any compilation errors occur, or if no class was found
     */
    private String compile(Path file, List<String> javacOpts, Context context) throws Fault {
        JavaFileObject fo = readFile(file);

        JavacTool javaCompiler = JavacTool.create();
        StandardJavaFileManager stdFileMgr = javaCompiler.getStandardFileManager(null, null, null);
        try {
            stdFileMgr.setLocation(StandardLocation.SOURCE_PATH, Collections.emptyList());
        } catch (IOException e) {
            throw new java.lang.Error("unexpected exception from file manager", e);
        }
        JavaFileManager fm = context.getFileManager(stdFileMgr);
        JavacTask t = javaCompiler.getTask(out, fm, null, javacOpts, null, List.of(fo));
        MainClassListener l = new MainClassListener(t);
        Boolean ok = t.call();
        if (!ok) {
            throw new Fault(Errors.CompilationFailed);
        }
        if (l.mainClass == null) {
            throw new Fault(Errors.NoClass);
        }
        String mainClassName = l.mainClass.getQualifiedName().toString();
        return mainClassName;
    }

    /**
     * Invokes the {@code main} method of a specified class, using a class loader that
     * will load recently compiled classes from memory.
     *
     * @param mainClassName the class to be executed
     * @param appArgs the arguments for the {@code main} method
     * @param context the context for the class to be executed
     * @throws Fault if there is a problem finding or invoking the {@code main} method
     * @throws InvocationTargetException if the {@code main} method throws an exception
     */
    private void execute(String mainClassName, String[] appArgs, Context context)
            throws Fault, InvocationTargetException {
        System.setProperty("jdk.launcher.sourcefile", context.file.toString());
        ClassLoader cl = context.getClassLoader(ClassLoader.getSystemClassLoader());
        try {
            Class<?> appClass = Class.forName(mainClassName, true, cl);
            Method main = appClass.getDeclaredMethod("main", String[].class);
            int PUBLIC_STATIC = Modifier.PUBLIC | Modifier.STATIC;
            if ((main.getModifiers() & PUBLIC_STATIC) != PUBLIC_STATIC) {
                throw new Fault(Errors.MainNotPublicStatic);
            }
            if (!main.getReturnType().equals(void.class)) {
                throw new Fault(Errors.MainNotVoid);
            }
            main.setAccessible(true);
            main.invoke(0, (Object) appArgs);
        } catch (ClassNotFoundException e) {
            throw new Fault(Errors.CantFindClass(mainClassName));
        } catch (NoSuchMethodException e) {
            throw new Fault(Errors.CantFindMainMethod(mainClassName));
        } catch (IllegalAccessException e) {
            throw new Fault(Errors.CantAccessMainMethod(mainClassName));
        } catch (InvocationTargetException e) {
            // remove stack frames for source launcher
            int invocationFrames = e.getStackTrace().length;
            Throwable target = e.getCause();
            StackTraceElement[] targetTrace = target.getStackTrace();
            target.setStackTrace(Arrays.copyOfRange(targetTrace, 0, targetTrace.length - invocationFrames));
            throw e;
        }
    }

    private static final String bundleName = "com.sun.tools.javac.resources.launcher";
    private ResourceBundle resourceBundle = null;
    private String errorPrefix;

    /**
     * Returns a localized string from a resource bundle.
     *
     * @param error the error for which to get the localized text
     * @return the localized string
     */
    private String getMessage(Error error) {
        String key = error.key();
        Object[] args = error.getArgs();
        try {
            if (resourceBundle == null) {
                resourceBundle = ResourceBundle.getBundle(bundleName);
                errorPrefix = resourceBundle.getString("launcher.error");
            }
            String resource = resourceBundle.getString(key);
            String message = MessageFormat.format(resource, args);
            return errorPrefix + message;
        } catch (MissingResourceException e) {
            return "Cannot access resource; " + key + Arrays.toString(args);
        }
    }

    /**
     * A listener to detect the first class found in a compilation.
     */
    static class MainClassListener implements TaskListener {
        TypeElement mainClass;

        MainClassListener(JavacTask t) {
            t.addTaskListener(this);
        }

        @Override
        public void started(TaskEvent ev) {
            if (ev.getKind() == TaskEvent.Kind.ANALYZE && mainClass == null) {
                TypeElement te = ev.getTypeElement();
                if (te.getNestingKind() == NestingKind.TOP_LEVEL) {
                    mainClass = te;
                }
            }
        }
    }

    /**
     * An object to encapsulate the set of in-memory classes, such that
     * they can be written by a file manager and subsequently used by
     * a class loader.
     */
    private static class Context {
        private final Path file;
        private final Map<String, byte[]> inMemoryClasses = new HashMap<>();

        Context(Path file) {
            this.file = file;
        }

        JavaFileManager getFileManager(StandardJavaFileManager delegate) {
            return new MemoryFileManager(inMemoryClasses, delegate);
        }

        ClassLoader getClassLoader(ClassLoader parent) {
            return new MemoryClassLoader(inMemoryClasses, parent, file);
        }
    }

    /**
     * An in-memory file manager.
     *
     * <p>Class files (of kind {@link JavaFileObject.Kind#CLASS CLASS} written to
     * {@link StandardLocation#CLASS_OUTPUT} will be written to an in-memory cache.
     * All other file manager operations will be delegated to a specified file manager.
     */
    private static class MemoryFileManager extends ForwardingJavaFileManager<JavaFileManager> {
        private final Map<String, byte[]> map;

        MemoryFileManager(Map<String, byte[]> map, JavaFileManager delegate) {
            super(delegate);
            this.map = map;
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className,
                JavaFileObject.Kind kind, FileObject sibling) throws IOException {
            if (location == StandardLocation.CLASS_OUTPUT && kind == JavaFileObject.Kind.CLASS) {
                return createInMemoryClassFile(className);
            } else {
                return super.getJavaFileForOutput(location, className, kind, sibling);
            }
        }

        private JavaFileObject createInMemoryClassFile(String className) {
            URI uri = URI.create("memory:///" + className.replace('.', '/') + ".class");
            return new SimpleJavaFileObject(uri, JavaFileObject.Kind.CLASS) {
                @Override
                public OutputStream openOutputStream() {
                    return new ByteArrayOutputStream() {
                        @Override
                        public void close() throws IOException {
                            super.close();
                            map.put(className, toByteArray());
                        }
                    };
                }
            };
        }
    }

    /**
     * An in-memory classloader, that uses an in-memory cache of classes written by
     * {@link MemoryFileManager}.
     *
     * <p>The classloader inverts the standard parent-delegation model, giving preference
     * to classes defined in the source file before classes known to the parent (such
     * as any like-named classes that might be found on the application class path.)
     */
    private static class MemoryClassLoader extends ClassLoader {
        /**
         * The map of all classes found in the source file, indexed by
         * {@link ClassLoader#name binary name}.
         */
        private final Map<String, byte[]> sourceFileClasses;

        /**
         * A minimal protection domain, specifying a code source of the source file itself,
         * used for classes found in the source file and defined by this loader.
         */
        private final ProtectionDomain domain;

        MemoryClassLoader(Map<String, byte[]> sourceFileClasses, ClassLoader parent, Path file) {
            super(parent);
            this.sourceFileClasses = sourceFileClasses;
            CodeSource codeSource;
            try {
                codeSource = new CodeSource(file.toUri().toURL(), (CodeSigner[]) null);
            } catch (MalformedURLException e) {
                codeSource = null;
            }
            domain = new ProtectionDomain(codeSource, null, this, null);
        }

        /**
         * Override loadClass to check for classes defined in the source file
         * before checking for classes in the parent class loader,
         * including those on the classpath.
         *
         * {@code loadClass(String name)} calls this method, and so will have the same behavior.
         *
         * @param name the name of the class to load
         * @param resolve whether or not to resolve the class
         * @return the class
         * @throws ClassNotFoundException if the class is not found
         */
        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                Class<?> c = findLoadedClass(name);
                if (c == null) {
                    if (sourceFileClasses.containsKey(name)) {
                        c = findClass(name);
                    } else {
                        c = getParent().loadClass(name);
                    }
                    if (resolve) {
                        resolveClass(c);
                    }
                }
                return c;
            }
        }


        /**
         * Override getResource to check for resources (i.e. class files) defined in the
         * source file before checking resources in the parent class loader,
         * including those on the class path.
         *
         * {@code getResourceAsStream(String name)} calls this method,
         * and so will have the same behavior.
         *
         * @param name the name of the resource
         * @return a URL for the resource, or null if not found
         */
        @Override
        public URL getResource(String name) {
            if (sourceFileClasses.containsKey(toBinaryName(name))) {
                return findResource(name);
            } else {
                return getParent().getResource(name);
            }
        }

        /**
         * Override getResources to check for resources (i.e. class files) defined in the
         * source file before checking resources in the parent class loader,
         * including those on the class path.
         *
         * @param name the name of the resource
         * @return an enumeration of the resources in this loader and in the application class loader
         */
        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            URL u = findResource(name);
            Enumeration<URL> e = getParent().getResources(name);
            if (u == null) {
                return e;
            } else {
                List<URL> list = new ArrayList<>();
                list.add(u);
                while (e.hasMoreElements()) {
                    list.add(e.nextElement());
                }
                return Collections.enumeration(list);
            }
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] bytes = sourceFileClasses.get(name);
            if (bytes == null) {
                throw new ClassNotFoundException(name);
            }
            return defineClass(name, bytes, 0, bytes.length, domain);
        }

        @Override
        public URL findResource(String name) {
            String binaryName = toBinaryName(name);
            if (binaryName == null || sourceFileClasses.get(binaryName) == null) {
                return null;
            }

            URLStreamHandler handler = this.handler;
            if (handler == null) {
                this.handler = handler = new MemoryURLStreamHandler();
            }

            try {
                return new URL(PROTOCOL, null, -1, name, handler);
            } catch (MalformedURLException e) {
                return null;
            }
        }

        @Override
        public Enumeration<URL> findResources(String name) {
            return new Enumeration<URL>() {
                private URL next = findResource(name);

                @Override
                public boolean hasMoreElements() {
                    return (next != null);
                }

                @Override
                public URL nextElement() {
                    if (next == null) {
                        throw new NoSuchElementException();
                    }
                    URL u = next;
                    next = null;
                    return u;
                }
            };
        }

        /**
         * Converts a "resource name" (as used in the getResource* methods)
         * to a binary name if the name identifies a class, or null otherwise.
         * @param name the resource name
         * @return the binary name
         */
        private String toBinaryName(String name) {
            if (!name.endsWith(".class")) {
                return null;
            }
            return name.substring(0, name.length() - DOT_CLASS_LENGTH).replace('/', '.');
        }

        private static final int DOT_CLASS_LENGTH = ".class".length();
        private final String PROTOCOL = "sourcelauncher-" + getClass().getSimpleName() + hashCode();
        private URLStreamHandler handler;

        /**
         * A URLStreamHandler for use with URLs returned by MemoryClassLoader.getResource.
         */
        private class MemoryURLStreamHandler extends URLStreamHandler {
            @Override
            public URLConnection openConnection(URL u) {
                if (!u.getProtocol().equalsIgnoreCase(PROTOCOL)) {
                    throw new IllegalArgumentException(u.toString());
                }
                return new MemoryURLConnection(u, sourceFileClasses.get(toBinaryName(u.getPath())));
            }

        }

        /**
         * A URLConnection for use with URLs returned by MemoryClassLoader.getResource.
         */
        private static class MemoryURLConnection extends URLConnection {
            private byte[] bytes;
            private InputStream in;

            MemoryURLConnection(URL u, byte[] bytes) {
                super(u);
                this.bytes = bytes;
            }

            @Override
            public void connect() throws IOException {
                if (!connected) {
                    if (bytes == null) {
                        throw new FileNotFoundException(getURL().getPath());
                    }
                    in = new ByteArrayInputStream(bytes);
                    connected = true;
                }
            }

            @Override
            public InputStream getInputStream() throws IOException {
                connect();
                return in;
            }

            @Override
            public long getContentLengthLong() {
                return bytes.length;
            }

            @Override
            public String getContentType() {
                return "application/octet-stream";
            }
        }
    }
}
