/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.file;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.ProviderNotFoundException;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.lang.model.SourceVersion;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

import com.sun.tools.javac.file.RelativePath.RelativeDirectory;
import com.sun.tools.javac.file.RelativePath.RelativeFile;
import com.sun.tools.javac.main.Option;
import com.sun.tools.javac.resources.CompilerProperties.Errors;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Context.Factory;
import com.sun.tools.javac.util.DefinedBy;
import com.sun.tools.javac.util.DefinedBy.Api;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;

import static javax.tools.StandardLocation.*;

/**
 * This class provides access to the source, class and other files
 * used by the compiler and related tools.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b>
 */
public class JavacFileManager extends BaseFileManager implements StandardJavaFileManager {

    public static char[] toArray(CharBuffer buffer) {
        if (buffer.hasArray())
            return buffer.compact().flip().array();
        else
            return buffer.toString().toCharArray();
    }

    private FSInfo fsInfo;

    private static final Set<JavaFileObject.Kind> SOURCE_OR_CLASS =
        Set.of(JavaFileObject.Kind.SOURCE, JavaFileObject.Kind.CLASS);

    protected boolean symbolFileEnabled;

    private PathFactory pathFactory = Paths::get;

    protected enum SortFiles implements Comparator<Path> {
        FORWARD {
            @Override
            public int compare(Path f1, Path f2) {
                return f1.getFileName().compareTo(f2.getFileName());
            }
        },
        REVERSE {
            @Override
            public int compare(Path f1, Path f2) {
                return f2.getFileName().compareTo(f1.getFileName());
            }
        }
    }

    protected SortFiles sortFiles;

    /**
     * We use a two-layered map instead of a map with a complex key because we don't want to reindex
     * the values for every Location+RelativeDirectory pair. Once the PathsAndContainers are needed
     * for a single Location, we should know all valid RelativeDirectory mappings. Because the
     * indexing is costly for very large classpaths, this can result in a significant savings.
     */
    private Map<Location, Map<RelativeDirectory, java.util.List<PathAndContainer>>>
        pathsAndContainersByLocationAndRelativeDirectory = new HashMap<>();

    /** Containers that have no indexing by {@link RelativeDirectory}, keyed by {@link Location}. */
    private Map<Location, java.util.List<PathAndContainer>> nonIndexingContainersByLocation =
        new HashMap<>();

    /**
     * Register a Context.Factory to create a JavacFileManager.
     */
    public static void preRegister(Context context) {
        context.put(JavaFileManager.class,
                (Factory<JavaFileManager>)c -> new JavacFileManager(c, true, null));
    }

    /**
     * Create a JavacFileManager using a given context, optionally registering
     * it as the JavaFileManager for that context.
     */
    public JavacFileManager(Context context, boolean register, Charset charset) {
        super(charset);
        if (register)
            context.put(JavaFileManager.class, this);
        setContext(context);
    }

    /**
     * Set the context for JavacFileManager.
     */
    @Override
    public void setContext(Context context) {
        super.setContext(context);

        fsInfo = FSInfo.instance(context);

        symbolFileEnabled = !options.isSet("ignore.symbol.file");

        String sf = options.get("sortFiles");
        if (sf != null) {
            sortFiles = (sf.equals("reverse") ? SortFiles.REVERSE : SortFiles.FORWARD);
        }
    }

    @Override @DefinedBy(DefinedBy.Api.COMPILER)
    public void setPathFactory(PathFactory f) {
        pathFactory = Objects.requireNonNull(f);
        locations.setPathFactory(f);
    }

    private Path getPath(String first, String... more) {
        return pathFactory.getPath(first, more);
    }

    /**
     * Set whether or not to use ct.sym as an alternate to rt.jar.
     */
    public void setSymbolFileEnabled(boolean b) {
        symbolFileEnabled = b;
    }

    public boolean isSymbolFileEnabled() {
        return symbolFileEnabled;
    }

    // used by tests
    public JavaFileObject getJavaFileObject(String name) {
        return getJavaFileObjects(name).iterator().next();
    }

    // used by tests
    public JavaFileObject getJavaFileObject(Path file) {
        return getJavaFileObjects(file).iterator().next();
    }

    public JavaFileObject getFileForOutput(String classname,
                                           JavaFileObject.Kind kind,
                                           JavaFileObject sibling)
        throws IOException
    {
        return getJavaFileForOutput(CLASS_OUTPUT, classname, kind, sibling);
    }

    @Override @DefinedBy(Api.COMPILER)
    public Iterable<? extends JavaFileObject> getJavaFileObjectsFromStrings(Iterable<String> names) {
        ListBuffer<Path> paths = new ListBuffer<>();
        for (String name : names)
            paths.append(getPath(nullCheck(name)));
        return getJavaFileObjectsFromPaths(paths.toList());
    }

    @Override @DefinedBy(Api.COMPILER)
    public Iterable<? extends JavaFileObject> getJavaFileObjects(String... names) {
        return getJavaFileObjectsFromStrings(Arrays.asList(nullCheck(names)));
    }

    private static boolean isValidName(String name) {
        // Arguably, isValidName should reject keywords (such as in SourceVersion.isName() ),
        // but the set of keywords depends on the source level, and we don't want
        // impls of JavaFileManager to have to be dependent on the source level.
        // Therefore we simply check that the argument is a sequence of identifiers
        // separated by ".".
        for (String s : name.split("\\.", -1)) {
            if (!SourceVersion.isIdentifier(s))
                return false;
        }
        return true;
    }

    private static void validateClassName(String className) {
        if (!isValidName(className))
            throw new IllegalArgumentException("Invalid class name: " + className);
    }

    private static void validatePackageName(String packageName) {
        if (packageName.length() > 0 && !isValidName(packageName))
            throw new IllegalArgumentException("Invalid packageName name: " + packageName);
    }

    public static void testName(String name,
                                boolean isValidPackageName,
                                boolean isValidClassName)
    {
        try {
            validatePackageName(name);
            if (!isValidPackageName)
                throw new AssertionError("Invalid package name accepted: " + name);
            printAscii("Valid package name: \"%s\"", name);
        } catch (IllegalArgumentException e) {
            if (isValidPackageName)
                throw new AssertionError("Valid package name rejected: " + name);
            printAscii("Invalid package name: \"%s\"", name);
        }
        try {
            validateClassName(name);
            if (!isValidClassName)
                throw new AssertionError("Invalid class name accepted: " + name);
            printAscii("Valid class name: \"%s\"", name);
        } catch (IllegalArgumentException e) {
            if (isValidClassName)
                throw new AssertionError("Valid class name rejected: " + name);
            printAscii("Invalid class name: \"%s\"", name);
        }
    }

    private static void printAscii(String format, Object... args) {
        String message;
        try {
            final String ascii = "US-ASCII";
            message = new String(String.format(null, format, args).getBytes(ascii), ascii);
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new AssertionError(ex);
        }
        System.out.println(message);
    }

    private final Map<Path, Container> containers = new HashMap<>();

    synchronized Container getContainer(Path path) throws IOException {
        Container fs = containers.get(path);

        if (fs != null) {
            return fs;
        }

        if (fsInfo.isFile(path) && path.equals(Locations.thisSystemModules)) {
            containers.put(path, fs = new JRTImageContainer());
            return fs;
        }

        Path realPath = fsInfo.getCanonicalFile(path);

        fs = containers.get(realPath);

        if (fs != null) {
            containers.put(path, fs);
            return fs;
        }

        BasicFileAttributes attr = null;

        try {
            attr = Files.readAttributes(realPath, BasicFileAttributes.class);
        } catch (IOException ex) {
            //non-existing
            fs = MISSING_CONTAINER;
        }

        if (attr != null) {
            if (attr.isDirectory()) {
                fs = new DirectoryContainer(realPath);
            } else {
                try {
                    fs = new ArchiveContainer(path);
                } catch (ProviderNotFoundException | SecurityException ex) {
                    throw new IOException(ex);
                }
            }
        }

        containers.put(realPath, fs);
        containers.put(path, fs);

        return fs;
    }

    private interface Container {
        /**
         * Insert all files in subdirectory subdirectory of container which
         * match fileKinds into resultList
         */
        public abstract void list(Path userPath,
                                  RelativeDirectory subdirectory,
                                  Set<JavaFileObject.Kind> fileKinds,
                                  boolean recurse,
                                  ListBuffer<JavaFileObject> resultList) throws IOException;
        public abstract JavaFileObject getFileObject(Path userPath, RelativeFile name) throws IOException;
        public abstract void close() throws IOException;
        public abstract boolean maintainsDirectoryIndex();

        /**
         * The directories this container indexes if {@link #maintainsDirectoryIndex()}, otherwise
         * an empty iterable.
         */
        public abstract Iterable<RelativeDirectory> indexedDirectories();
    }

    private static final Container MISSING_CONTAINER =  new Container() {
        @Override
        public void list(Path userPath,
                         RelativeDirectory subdirectory,
                         Set<JavaFileObject.Kind> fileKinds,
                         boolean recurse,
                         ListBuffer<JavaFileObject> resultList) throws IOException {
        }
        @Override
        public JavaFileObject getFileObject(Path userPath, RelativeFile name) throws IOException {
            return null;
        }
        @Override
        public void close() throws IOException {}
        @Override
        public boolean maintainsDirectoryIndex() {
            return false;
        }
        @Override
        public Iterable<RelativeDirectory> indexedDirectories() {
            return List.nil();
        }
    };

    private final class JRTImageContainer implements Container {

        /**
         * Insert all files in a subdirectory of the platform image
         * which match fileKinds into resultList.
         */
        @Override
        public void list(Path userPath,
                         RelativeDirectory subdirectory,
                         Set<JavaFileObject.Kind> fileKinds,
                         boolean recurse,
                         ListBuffer<JavaFileObject> resultList) throws IOException {
            try {
                JRTIndex.Entry e = getJRTIndex().getEntry(subdirectory);
                if (symbolFileEnabled && e.ctSym.hidden)
                    return;
                for (Path file: e.files.values()) {
                    if (fileKinds.contains(getKind(file))) {
                        JavaFileObject fe
                                = PathFileObject.forJRTPath(JavacFileManager.this, file);
                        resultList.append(fe);
                    }
                }

                if (recurse) {
                    for (RelativeDirectory rd: e.subdirs) {
                        list(userPath, rd, fileKinds, recurse, resultList);
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
                log.error(Errors.ErrorReadingFile(userPath, getMessage(ex)));
            }
        }

        @Override
        public JavaFileObject getFileObject(Path userPath, RelativeFile name) throws IOException {
            JRTIndex.Entry e = getJRTIndex().getEntry(name.dirname());
            if (symbolFileEnabled && e.ctSym.hidden)
                return null;
            Path p = e.files.get(name.basename());
            if (p != null) {
                return PathFileObject.forJRTPath(JavacFileManager.this, p);
            } else {
                return null;
            }
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public boolean maintainsDirectoryIndex() {
            return false;
        }

        @Override
        public Iterable<RelativeDirectory> indexedDirectories() {
            return List.nil();
        }
    }

    private synchronized JRTIndex getJRTIndex() {
        if (jrtIndex == null)
            jrtIndex = JRTIndex.getSharedInstance();
        return jrtIndex;
    }

    private JRTIndex jrtIndex;

    private final class DirectoryContainer implements Container {
        private final Path directory;

        public DirectoryContainer(Path directory) {
            this.directory = directory;
        }

        /**
         * Insert all files in subdirectory subdirectory of directory userPath
         * which match fileKinds into resultList
         */
        @Override
        public void list(Path userPath,
                         RelativeDirectory subdirectory,
                         Set<JavaFileObject.Kind> fileKinds,
                         boolean recurse,
                         ListBuffer<JavaFileObject> resultList) throws IOException {
            Path d;
            try {
                d = subdirectory.resolveAgainst(userPath);
            } catch (InvalidPathException ignore) {
                return ;
            }

            if (!Files.exists(d)) {
               return;
            }

            if (!caseMapCheck(d, subdirectory)) {
                return;
            }

            java.util.List<Path> files;
            try (Stream<Path> s = Files.list(d)) {
                files = (sortFiles == null ? s : s.sorted(sortFiles)).toList();
            } catch (IOException ignore) {
                return;
            }

            for (Path f: files) {
                String fname = f.getFileName().toString();
                if (fname.endsWith("/"))
                    fname = fname.substring(0, fname.length() - 1);
                if (Files.isDirectory(f)) {
                    if (recurse && SourceVersion.isIdentifier(fname)) {
                        list(userPath,
                             new RelativeDirectory(subdirectory, fname),
                             fileKinds,
                             recurse,
                             resultList);
                    }
                } else {
                    if (isValidFile(fname, fileKinds)) {
                        try {
                            RelativeFile file = new RelativeFile(subdirectory, fname);
                            JavaFileObject fe = PathFileObject.forDirectoryPath(JavacFileManager.this,
                                    file.resolveAgainst(directory), userPath, file);
                            resultList.append(fe);
                        } catch (InvalidPathException e) {
                            throw new IOException("error accessing directory " + directory + e);
                        }
                    }
                }
            }
        }

        @Override
        public JavaFileObject getFileObject(Path userPath, RelativeFile name) throws IOException {
            try {
                Path f = name.resolveAgainst(userPath);
                if (Files.exists(f))
                    return PathFileObject.forSimplePath(JavacFileManager.this,
                            fsInfo.getCanonicalFile(f), f);
            } catch (InvalidPathException ignore) {
            }
            return null;
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public boolean maintainsDirectoryIndex() {
            return false;
        }

        @Override
        public Iterable<RelativeDirectory> indexedDirectories() {
            return List.nil();
        }
    }

    private static final Set<FileVisitOption> NO_FILE_VISIT_OPTIONS = Set.of();
    private static final Set<FileVisitOption> FOLLOW_LINKS_OPTIONS = Set.of(FOLLOW_LINKS);

    private final class ArchiveContainer implements Container {
        private final Path archivePath;
        private final FileSystem fileSystem;
        private final Map<RelativeDirectory, Path> packages;

        public ArchiveContainer(Path archivePath) throws IOException, ProviderNotFoundException, SecurityException {
            this.archivePath = archivePath;
            if (multiReleaseValue != null && archivePath.toString().endsWith(".jar")) {
                Map<String,String> env = Collections.singletonMap("multi-release", multiReleaseValue);
                FileSystemProvider jarFSProvider = fsInfo.getJarFSProvider();
                Assert.checkNonNull(jarFSProvider, "should have been caught before!");
                this.fileSystem = jarFSProvider.newFileSystem(archivePath, env);
            } else {
                this.fileSystem = FileSystems.newFileSystem(archivePath, (ClassLoader)null);
            }
            packages = new HashMap<>();
            for (Path root : fileSystem.getRootDirectories()) {
                Files.walkFileTree(root, NO_FILE_VISIT_OPTIONS, Integer.MAX_VALUE,
                        new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                                if (isValid(dir.getFileName())) {
                                    packages.put(new RelativeDirectory(root.relativize(dir).toString()), dir);
                                    return FileVisitResult.CONTINUE;
                                } else {
                                    return FileVisitResult.SKIP_SUBTREE;
                                }
                            }
                        });
            }
        }

        /**
         * Insert all files in subdirectory subdirectory of this archive
         * which match fileKinds into resultList
         */
        @Override
        public void list(Path userPath,
                         RelativeDirectory subdirectory,
                         Set<JavaFileObject.Kind> fileKinds,
                         boolean recurse,
                         ListBuffer<JavaFileObject> resultList) throws IOException {
            Path resolvedSubdirectory = packages.get(subdirectory);

            if (resolvedSubdirectory == null)
                return ;

            int maxDepth = (recurse ? Integer.MAX_VALUE : 1);
            Files.walkFileTree(resolvedSubdirectory, FOLLOW_LINKS_OPTIONS, maxDepth,
                    new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                            if (isValid(dir.getFileName())) {
                                return FileVisitResult.CONTINUE;
                            } else {
                                return FileVisitResult.SKIP_SUBTREE;
                            }
                        }

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (attrs.isRegularFile() && fileKinds.contains(getKind(file.getFileName().toString()))) {
                                JavaFileObject fe = PathFileObject.forJarPath(
                                        JavacFileManager.this, file, archivePath);
                                resultList.append(fe);
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });

        }

        private boolean isValid(Path fileName) {
            if (fileName == null) {
                return true;
            } else {
                String name = fileName.toString();
                if (name.endsWith("/")) {
                    name = name.substring(0, name.length() - 1);
                }
                return SourceVersion.isIdentifier(name);
            }
        }

        @Override
        public JavaFileObject getFileObject(Path userPath, RelativeFile name) throws IOException {
            RelativeDirectory root = name.dirname();
            Path packagepath = packages.get(root);
            if (packagepath != null) {
                Path relpath = packagepath.resolve(name.basename());
                if (Files.exists(relpath)) {
                    return PathFileObject.forJarPath(JavacFileManager.this, relpath, userPath);
                }
            }
            return null;
        }

        @Override
        public void close() throws IOException {
            fileSystem.close();
        }

        @Override
        public boolean maintainsDirectoryIndex() {
            return true;
        }

        @Override
        public Iterable<RelativeDirectory> indexedDirectories() {
            return packages.keySet();
        }
    }

    /**
     * container is a directory, a zip file, or a non-existent path.
     */
    private boolean isValidFile(String s, Set<JavaFileObject.Kind> fileKinds) {
        JavaFileObject.Kind kind = getKind(s);
        return fileKinds.contains(kind);
    }

    private static final boolean fileSystemIsCaseSensitive =
        File.separatorChar == '/';

    /** Hack to make Windows case sensitive. Test whether given path
     *  ends in a string of characters with the same case as given name.
     *  Ignore file separators in both path and name.
     */
    private boolean caseMapCheck(Path f, RelativePath name) {
        if (fileSystemIsCaseSensitive) return true;
        // Note that toRealPath() returns the case-sensitive
        // spelled file name.
        String path;
        char sep;
        try {
            path = f.toRealPath(LinkOption.NOFOLLOW_LINKS).toString();
            sep = f.getFileSystem().getSeparator().charAt(0);
        } catch (IOException ex) {
            return false;
        }
        char[] pcs = path.toCharArray();
        char[] ncs = name.path.toCharArray();
        int i = pcs.length - 1;
        int j = ncs.length - 1;
        while (i >= 0 && j >= 0) {
            while (i >= 0 && pcs[i] == sep) i--;
            while (j >= 0 && ncs[j] == '/') j--;
            if (i >= 0 && j >= 0) {
                if (pcs[i] != ncs[j]) return false;
                i--;
                j--;
            }
        }
        return j < 0;
    }

    /** Flush any output resources.
     */
    @Override @DefinedBy(Api.COMPILER)
    public void flush() {
        contentCache.clear();
        pathsAndContainersByLocationAndRelativeDirectory.clear();
        nonIndexingContainersByLocation.clear();
    }

    /**
     * Close the JavaFileManager, releasing resources.
     */
    @Override @DefinedBy(Api.COMPILER)
    public void close() throws IOException {
        if (deferredCloseTimeout > 0) {
            deferredClose();
            return;
        }

        locations.close();
        for (Container container: containers.values()) {
            container.close();
        }
        containers.clear();
        pathsAndContainersByLocationAndRelativeDirectory.clear();
        nonIndexingContainersByLocation.clear();
        contentCache.clear();
    }

    @Override @DefinedBy(Api.COMPILER)
    public ClassLoader getClassLoader(Location location) {
        checkNotModuleOrientedLocation(location);
        Iterable<? extends File> path = getLocation(location);
        if (path == null)
            return null;
        ListBuffer<URL> lb = new ListBuffer<>();
        for (File f: path) {
            try {
                lb.append(f.toURI().toURL());
            } catch (MalformedURLException e) {
                throw new AssertionError(e);
            }
        }

        return getClassLoader(lb.toArray(new URL[lb.size()]));
    }

    @Override @DefinedBy(Api.COMPILER)
    public Iterable<JavaFileObject> list(Location location,
                                         String packageName,
                                         Set<JavaFileObject.Kind> kinds,
                                         boolean recurse)
        throws IOException
    {
        checkNotModuleOrientedLocation(location);
        // validatePackageName(packageName);
        nullCheck(packageName);
        nullCheck(kinds);

        RelativeDirectory subdirectory = RelativeDirectory.forPackage(packageName);
        ListBuffer<JavaFileObject> results = new ListBuffer<>();

        for (PathAndContainer pathAndContainer : pathsAndContainers(location, subdirectory)) {
            Path directory = pathAndContainer.path;
            Container container = pathAndContainer.container;
            container.list(directory, subdirectory, kinds, recurse, results);
        }

        return results.toList();
    }

    @Override @DefinedBy(Api.COMPILER)
    public String inferBinaryName(Location location, JavaFileObject file) {
        checkNotModuleOrientedLocation(location);
        Objects.requireNonNull(file);
        // Need to match the path semantics of list(location, ...)
        Iterable<? extends Path> path = getLocationAsPaths(location);
        if (path == null) {
            return null;
        }

        if (file instanceof PathFileObject pathFileObject) {
            return pathFileObject.inferBinaryName(path);
        } else
            throw new IllegalArgumentException(file.getClass().getName());
    }

    @Override @DefinedBy(Api.COMPILER)
    public boolean isSameFile(FileObject a, FileObject b) {
        nullCheck(a);
        nullCheck(b);
        if (a instanceof PathFileObject pathFileObjectA && b instanceof PathFileObject pathFileObjectB)
            return pathFileObjectA.isSameFile(pathFileObjectB);
        return a.equals(b);
    }

    @Override @DefinedBy(Api.COMPILER)
    public boolean hasLocation(Location location) {
        nullCheck(location);
        return locations.hasLocation(location);
    }

    protected boolean hasExplicitLocation(Location location) {
        nullCheck(location);
        return locations.hasExplicitLocation(location);
    }

    @Override @DefinedBy(Api.COMPILER)
    public JavaFileObject getJavaFileForInput(Location location,
                                              String className,
                                              JavaFileObject.Kind kind)
        throws IOException
    {
        checkNotModuleOrientedLocation(location);
        // validateClassName(className);
        nullCheck(className);
        nullCheck(kind);
        if (!SOURCE_OR_CLASS.contains(kind))
            throw new IllegalArgumentException("Invalid kind: " + kind);
        return getFileForInput(location, RelativeFile.forClass(className, kind));
    }

    @Override @DefinedBy(Api.COMPILER)
    public FileObject getFileForInput(Location location,
                                      String packageName,
                                      String relativeName)
        throws IOException
    {
        checkNotModuleOrientedLocation(location);
        // validatePackageName(packageName);
        nullCheck(packageName);
        if (!isRelativeUri(relativeName))
            throw new IllegalArgumentException("Invalid relative name: " + relativeName);
        RelativeFile name = packageName.length() == 0
            ? new RelativeFile(relativeName)
            : new RelativeFile(RelativeDirectory.forPackage(packageName), relativeName);
        return getFileForInput(location, name);
    }

    private JavaFileObject getFileForInput(Location location, RelativeFile name) throws IOException {
        Iterable<? extends Path> path = getLocationAsPaths(location);
        if (path == null)
            return null;

        for (Path file: path) {
            JavaFileObject fo = getContainer(file).getFileObject(file, name);

            if (fo != null) {
                return fo;
            }
        }
        return null;
    }

    @Override @DefinedBy(Api.COMPILER)
    public JavaFileObject getJavaFileForOutput(Location location,
                                               String className,
                                               JavaFileObject.Kind kind,
                                               FileObject sibling)
        throws IOException
    {
        checkOutputLocation(location);
        // validateClassName(className);
        nullCheck(className);
        nullCheck(kind);
        if (!SOURCE_OR_CLASS.contains(kind))
            throw new IllegalArgumentException("Invalid kind: " + kind);
        return getFileForOutput(location, RelativeFile.forClass(className, kind), sibling);
    }

    @Override @DefinedBy(Api.COMPILER)
    public FileObject getFileForOutput(Location location,
                                       String packageName,
                                       String relativeName,
                                       FileObject sibling)
        throws IOException
    {
        checkOutputLocation(location);
        // validatePackageName(packageName);
        nullCheck(packageName);
        if (!isRelativeUri(relativeName))
            throw new IllegalArgumentException("Invalid relative name: " + relativeName);
        RelativeFile name = packageName.length() == 0
            ? new RelativeFile(relativeName)
            : new RelativeFile(RelativeDirectory.forPackage(packageName), relativeName);
        return getFileForOutput(location, name, sibling);
    }

    private JavaFileObject getFileForOutput(Location location,
                                            RelativeFile fileName,
                                            FileObject sibling)
        throws IOException
    {
        Path dir;
        if (location == CLASS_OUTPUT) {
            if (getClassOutDir() != null) {
                dir = getClassOutDir();
            } else {
                String baseName = fileName.basename();
                if (sibling != null && sibling instanceof PathFileObject pathFileObject) {
                    return pathFileObject.getSibling(baseName);
                } else {
                    Path p = getPath(baseName);
                    Path real = fsInfo.getCanonicalFile(p);
                    return PathFileObject.forSimplePath(this, real, p);
                }
            }
        } else if (location == SOURCE_OUTPUT) {
            dir = (getSourceOutDir() != null ? getSourceOutDir() : getClassOutDir());
        } else {
            Iterable<? extends Path> path = locations.getLocation(location);
            dir = null;
            for (Path f: path) {
                dir = f;
                break;
            }
        }

        try {
            if (dir == null) {
                dir = getPath(System.getProperty("user.dir"));
            }
            Path path = fileName.resolveAgainst(fsInfo.getCanonicalFile(dir));
            return PathFileObject.forDirectoryPath(this, path, dir, fileName);
        } catch (InvalidPathException e) {
            throw new IOException("bad filename " + fileName, e);
        }
    }

    @Override @DefinedBy(Api.COMPILER)
    public Iterable<? extends JavaFileObject> getJavaFileObjectsFromFiles(
        Iterable<? extends File> files)
    {
        ArrayList<PathFileObject> result;
        if (files instanceof Collection<?> collection)
            result = new ArrayList<>(collection.size());
        else
            result = new ArrayList<>();
        for (File f: files) {
            Objects.requireNonNull(f);
            Path p = f.toPath();
            result.add(PathFileObject.forSimplePath(this,
                    fsInfo.getCanonicalFile(p), p));
        }
        return result;
    }

    @Override @DefinedBy(Api.COMPILER)
    public Iterable<? extends JavaFileObject> getJavaFileObjectsFromPaths(Collection<? extends Path> paths) {
        ArrayList<PathFileObject> result;
        if (paths != null) {
            result = new ArrayList<>(paths.size());
            for (Path p: paths)
                result.add(PathFileObject.forSimplePath(this,
                        fsInfo.getCanonicalFile(p), p));
        } else {
            result = new ArrayList<>();
        }
        return result;
    }

    @Override @DefinedBy(Api.COMPILER)
    public Iterable<? extends JavaFileObject> getJavaFileObjects(File... files) {
        return getJavaFileObjectsFromFiles(Arrays.asList(nullCheck(files)));
    }

    @Override @DefinedBy(Api.COMPILER)
    public Iterable<? extends JavaFileObject> getJavaFileObjects(Path... paths) {
        return getJavaFileObjectsFromPaths(Arrays.asList(nullCheck(paths)));
    }

    @Override @DefinedBy(Api.COMPILER)
    public void setLocation(Location location,
                            Iterable<? extends File> searchpath)
        throws IOException
    {
        nullCheck(location);
        locations.setLocation(location, asPaths(searchpath));
        clearCachesForLocation(location);
    }

    @Override @DefinedBy(Api.COMPILER)
    public void setLocationFromPaths(Location location,
                            Collection<? extends Path> searchpath)
        throws IOException
    {
        nullCheck(location);
        locations.setLocation(location, nullCheck(searchpath));
        clearCachesForLocation(location);
    }

    @Override @DefinedBy(Api.COMPILER)
    public Iterable<? extends File> getLocation(Location location) {
        nullCheck(location);
        return asFiles(locations.getLocation(location));
    }

    @Override @DefinedBy(Api.COMPILER)
    public Collection<? extends Path> getLocationAsPaths(Location location) {
        nullCheck(location);
        return locations.getLocation(location);
    }

    private java.util.List<PathAndContainer> pathsAndContainers(
            Location location, RelativeDirectory relativeDirectory) throws IOException {
        try {
            return pathsAndContainersByLocationAndRelativeDirectory.computeIfAbsent(
                    location, this::indexPathsAndContainersByRelativeDirectory)
                .computeIfAbsent(
                    relativeDirectory, d -> nonIndexingContainersByLocation.get(location));
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private Map<RelativeDirectory, java.util.List<PathAndContainer>> indexPathsAndContainersByRelativeDirectory(
            Location location) {
        Map<RelativeDirectory, java.util.List<PathAndContainer>> result = new HashMap<>();
        java.util.List<PathAndContainer> allPathsAndContainers = pathsAndContainers(location);

        // First collect all of the containers that don't maintain their own index on
        // RelativeDirectory. These need to always be included for all mappings
        java.util.List<PathAndContainer> nonIndexingContainers = new ArrayList<>();
        for (PathAndContainer pathAndContainer : allPathsAndContainers) {
            if (!pathAndContainer.container.maintainsDirectoryIndex()) {
                nonIndexingContainers.add(pathAndContainer);
            }
        }

        // Next, use the container that do maintain their own RelativeDirectory index to create a
        // single master index.
        for (PathAndContainer pathAndContainer : allPathsAndContainers) {
            Container container = pathAndContainer.container;
            if (container.maintainsDirectoryIndex()) {
                for (RelativeDirectory directory : container.indexedDirectories()) {
                    result.computeIfAbsent(directory, d -> new ArrayList<>(nonIndexingContainers))
                          .add(pathAndContainer);
                }
            }
        }
        nonIndexingContainersByLocation.put(location, nonIndexingContainers);

        // Sorting preserves the search order used in the uncached Location path, which has
        // maintains consistency with the classpath order
        result.values().forEach(pathAndContainerList -> Collections.sort(pathAndContainerList));

        return result;
    }

    /**
     * For each {@linkplain #getLocationAsPaths(Location) path of the location}, compute the
     * corresponding {@link Container}.
     */
    private java.util.List<PathAndContainer> pathsAndContainers(Location location) {
        Collection<? extends Path> paths = getLocationAsPaths(location);
        if (paths == null) {
            return List.nil();
        }
        java.util.List<PathAndContainer> pathsAndContainers =
            new ArrayList<>(paths.size());
        for (Path path : paths) {
            Container container;
            try {
                container = getContainer(path);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            pathsAndContainers.add(new PathAndContainer(path, container, pathsAndContainers.size()));
        }
        return pathsAndContainers;
    }

    private static class PathAndContainer implements Comparable<PathAndContainer> {
        private final Path path;
        private final Container container;
        private final int index;

        PathAndContainer(Path path, Container container, int index) {
            this.path = path;
            this.container = container;
            this.index = index;
        }

        @Override
        public int compareTo(PathAndContainer other) {
            return index - other.index;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof PathAndContainer pathAndContainer)
                    && path.equals(pathAndContainer.path)
                    && container.equals(pathAndContainer.container)
                    && index == pathAndContainer.index;
        }

        @Override
        public int hashCode() {
          return Objects.hash(path, container, index);
        }
    }

    @Override @DefinedBy(Api.COMPILER)
    public boolean contains(Location location, FileObject fo) throws IOException {
        nullCheck(location);
        nullCheck(fo);
        Path p = asPath(fo);
        return locations.contains(location, p);
    }

    private Path getClassOutDir() {
        return locations.getOutputLocation(CLASS_OUTPUT);
    }

    private Path getSourceOutDir() {
        return locations.getOutputLocation(SOURCE_OUTPUT);
    }

    @Override @DefinedBy(Api.COMPILER)
    public Location getLocationForModule(Location location, String moduleName) throws IOException {
        checkModuleOrientedOrOutputLocation(location);
        nullCheck(moduleName);
        if (location == SOURCE_OUTPUT && getSourceOutDir() == null)
            location = CLASS_OUTPUT;
        return locations.getLocationForModule(location, moduleName);
    }

    @Override @DefinedBy(Api.COMPILER)
    public <S> ServiceLoader<S> getServiceLoader(Location location, Class<S> service) throws IOException {
        nullCheck(location);
        nullCheck(service);
        getClass().getModule().addUses(service);
        if (location.isModuleOrientedLocation()) {
            Collection<Path> paths = locations.getLocation(location);
            ModuleFinder finder = ModuleFinder.of(paths.toArray(new Path[paths.size()]));
            ModuleLayer bootLayer = ModuleLayer.boot();
            Configuration cf = bootLayer.configuration().resolveAndBind(ModuleFinder.of(), finder, Collections.emptySet());
            ModuleLayer layer = bootLayer.defineModulesWithOneLoader(cf, ClassLoader.getSystemClassLoader());
            return ServiceLoader.load(layer, service);
        } else {
            return ServiceLoader.load(service, getClassLoader(location));
        }
    }

    @Override @DefinedBy(Api.COMPILER)
    public Location getLocationForModule(Location location, JavaFileObject fo) throws IOException {
        checkModuleOrientedOrOutputLocation(location);
        if (!(fo instanceof PathFileObject pathFileObject))
            return null;
        Path p = Locations.normalize(pathFileObject.path);
            // need to find p in location
        return locations.getLocationForModule(location, p);
    }

    @Override @DefinedBy(Api.COMPILER)
    public void setLocationForModule(Location location, String moduleName, Collection<? extends Path> paths)
            throws IOException {
        nullCheck(location);
        checkModuleOrientedOrOutputLocation(location);
        locations.setLocationForModule(location, nullCheck(moduleName), nullCheck(paths));
        clearCachesForLocation(location);
    }

    @Override @DefinedBy(Api.COMPILER)
    public String inferModuleName(Location location) {
        checkNotModuleOrientedLocation(location);
        return locations.inferModuleName(location);
    }

    @Override @DefinedBy(Api.COMPILER)
    public Iterable<Set<Location>> listLocationsForModules(Location location) throws IOException {
        checkModuleOrientedOrOutputLocation(location);
        return locations.listLocationsForModules(location);
    }

    @Override @DefinedBy(Api.COMPILER)
    public Path asPath(FileObject file) {
        if (file instanceof PathFileObject pathFileObject) {
            return pathFileObject.path;
        } else
            throw new IllegalArgumentException(file.getName());
    }

    /**
     * Enforces the specification of a "relative" name as used in
     * {@linkplain #getFileForInput(Location,String,String)
     * getFileForInput}.  This method must follow the rules defined in
     * that method, do not make any changes without consulting the
     * specification.
     */
    protected static boolean isRelativeUri(URI uri) {
        if (uri.isAbsolute())
            return false;
        String path = uri.normalize().getPath();
        if (path.length() == 0 /* isEmpty() is mustang API */)
            return false;
        if (!path.equals(uri.getPath())) // implicitly checks for embedded . and ..
            return false;
        if (path.startsWith("/") || path.startsWith("./") || path.startsWith("../"))
            return false;
        return true;
    }

    // Convenience method
    protected static boolean isRelativeUri(String u) {
        try {
            return isRelativeUri(new URI(u));
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * Converts a relative file name to a relative URI.  This is
     * different from File.toURI as this method does not canonicalize
     * the file before creating the URI.  Furthermore, no schema is
     * used.
     * @param file a relative file name
     * @return a relative URI
     * @throws IllegalArgumentException if the file name is not
     * relative according to the definition given in {@link
     * javax.tools.JavaFileManager#getFileForInput}
     */
    public static String getRelativeName(File file) {
        if (!file.isAbsolute()) {
            String result = file.getPath().replace(File.separatorChar, '/');
            if (isRelativeUri(result))
                return result;
        }
        throw new IllegalArgumentException("Invalid relative path: " + file);
    }

    /**
     * Get a detail message from an IOException.
     * Most, but not all, instances of IOException provide a non-null result
     * for getLocalizedMessage().  But some instances return null: in these
     * cases, fall back to getMessage(), and if even that is null, return the
     * name of the exception itself.
     * @param e an IOException
     * @return a string to include in a compiler diagnostic
     */
    public static String getMessage(IOException e) {
        String s = e.getLocalizedMessage();
        if (s != null)
            return s;
        s = e.getMessage();
        if (s != null)
            return s;
        return e.toString();
    }

    private void checkOutputLocation(Location location) {
        Objects.requireNonNull(location);
        if (!location.isOutputLocation())
            throw new IllegalArgumentException("location is not an output location: " + location.getName());
    }

    private void checkModuleOrientedOrOutputLocation(Location location) {
        Objects.requireNonNull(location);
        if (!location.isModuleOrientedLocation() && !location.isOutputLocation())
            throw new IllegalArgumentException(
                    "location is not an output location or a module-oriented location: "
                            + location.getName());
    }

    private void checkNotModuleOrientedLocation(Location location) {
        Objects.requireNonNull(location);
        if (location.isModuleOrientedLocation())
            throw new IllegalArgumentException("location is module-oriented: " + location.getName());
    }

    /* Converters between files and paths.
     * These are temporary until we can update the StandardJavaFileManager API.
     */

    private static Iterable<Path> asPaths(final Iterable<? extends File> files) {
        if (files == null)
            return null;

        return () -> new Iterator<Path>() {
            Iterator<? extends File> iter = files.iterator();

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public Path next() {
                return iter.next().toPath();
            }
        };
    }

    private static Iterable<File> asFiles(final Iterable<? extends Path> paths) {
        if (paths == null)
            return null;

        return () -> new Iterator<File>() {
            Iterator<? extends Path> iter = paths.iterator();

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public File next() {
                try {
                    return iter.next().toFile();
                } catch (UnsupportedOperationException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
    }

    @Override
    public boolean handleOption(Option option, String value) {
        if (javacFileManagerOptions.contains(option)) {
            pathsAndContainersByLocationAndRelativeDirectory.clear();
            nonIndexingContainersByLocation.clear();
        }
        return super.handleOption(option, value);
    }

    private void clearCachesForLocation(Location location) {
        nullCheck(location);
        pathsAndContainersByLocationAndRelativeDirectory.remove(location);
        nonIndexingContainersByLocation.remove(location);
    }
}
