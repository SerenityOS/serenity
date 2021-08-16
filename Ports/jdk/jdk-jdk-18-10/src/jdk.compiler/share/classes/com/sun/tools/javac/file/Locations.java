/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.ProviderNotFoundException;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.lang.model.SourceVersion;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardJavaFileManager.PathFactory;
import javax.tools.StandardLocation;

import jdk.internal.jmod.JmodFile;

import com.sun.tools.javac.code.Lint;
import com.sun.tools.javac.code.Lint.LintCategory;
import com.sun.tools.javac.main.Option;
import com.sun.tools.javac.resources.CompilerProperties.Errors;
import com.sun.tools.javac.resources.CompilerProperties.Warnings;
import com.sun.tools.javac.util.DefinedBy;
import com.sun.tools.javac.util.DefinedBy.Api;
import com.sun.tools.javac.util.JCDiagnostic.Warning;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.jvm.ModuleNameReader;
import com.sun.tools.javac.util.Iterators;
import com.sun.tools.javac.util.Pair;
import com.sun.tools.javac.util.StringUtils;

import static javax.tools.StandardLocation.SYSTEM_MODULES;
import static javax.tools.StandardLocation.PLATFORM_CLASS_PATH;

import static com.sun.tools.javac.main.Option.BOOT_CLASS_PATH;
import static com.sun.tools.javac.main.Option.ENDORSEDDIRS;
import static com.sun.tools.javac.main.Option.EXTDIRS;
import static com.sun.tools.javac.main.Option.XBOOTCLASSPATH_APPEND;
import static com.sun.tools.javac.main.Option.XBOOTCLASSPATH_PREPEND;

/**
 * This class converts command line arguments, environment variables and system properties (in
 * File.pathSeparator-separated String form) into a boot class path, user class path, and source
 * path (in {@code Collection<String>} form).
 *
 * <p>
 * <b>This is NOT part of any supported API. If you write code that depends on this, you do so at
 * your own risk. This code and its internal interfaces are subject to change or deletion without
 * notice.</b>
 */
public class Locations {

    /**
     * The log to use for warning output
     */
    private Log log;

    /**
     * Access to (possibly cached) file info
     */
    private FSInfo fsInfo;

    /**
     * Whether to warn about non-existent path elements
     */
    private boolean warn;

    private ModuleNameReader moduleNameReader;

    private PathFactory pathFactory = Paths::get;

    static final Path javaHome = FileSystems.getDefault().getPath(System.getProperty("java.home"));
    static final Path thisSystemModules = javaHome.resolve("lib").resolve("modules");

    Map<Path, FileSystem> fileSystems = new LinkedHashMap<>();
    List<Closeable> closeables = new ArrayList<>();
    private Map<String,String> fsEnv = Collections.emptyMap();

    Locations() {
        initHandlers();
    }

    Path getPath(String first, String... more) {
        try {
            return pathFactory.getPath(first, more);
        } catch (InvalidPathException ipe) {
            throw new IllegalArgumentException(ipe);
        }
    }

    public void close() throws IOException {
        ListBuffer<IOException> list = new ListBuffer<>();
        closeables.forEach(closeable -> {
            try {
                closeable.close();
            } catch (IOException ex) {
                list.add(ex);
            }
        });
        if (list.nonEmpty()) {
            IOException ex = new IOException();
            for (IOException e: list)
                ex.addSuppressed(e);
            throw ex;
        }
    }

    void update(Log log, boolean warn, FSInfo fsInfo) {
        this.log = log;
        this.warn = warn;
        this.fsInfo = fsInfo;
    }

    void setPathFactory(PathFactory f) {
        pathFactory = f;
    }

    boolean isDefaultBootClassPath() {
        BootClassPathLocationHandler h
                = (BootClassPathLocationHandler) getHandler(PLATFORM_CLASS_PATH);
        return h.isDefault();
    }

    boolean isDefaultSystemModulesPath() {
        SystemModulesLocationHandler h
                = (SystemModulesLocationHandler) getHandler(SYSTEM_MODULES);
        return !h.isExplicit();
    }

    /**
     * Split a search path into its elements. Empty path elements will be ignored.
     *
     * @param searchPath The search path to be split
     * @return The elements of the path
     */
    private Iterable<Path> getPathEntries(String searchPath) {
        return getPathEntries(searchPath, null);
    }

    /**
     * Split a search path into its elements. If emptyPathDefault is not null, all empty elements in the
     * path, including empty elements at either end of the path, will be replaced with the value of
     * emptyPathDefault.
     *
     * @param searchPath The search path to be split
     * @param emptyPathDefault The value to substitute for empty path elements, or null, to ignore
     * empty path elements
     * @return The elements of the path
     */
    private Iterable<Path> getPathEntries(String searchPath, Path emptyPathDefault) {
        ListBuffer<Path> entries = new ListBuffer<>();
        for (String s: searchPath.split(Pattern.quote(File.pathSeparator), -1)) {
            if (s.isEmpty()) {
                if (emptyPathDefault != null) {
                    entries.add(emptyPathDefault);
                }
            } else {
                try {
                    entries.add(getPath(s));
                } catch (IllegalArgumentException e) {
                    if (warn) {
                        log.warning(LintCategory.PATH, Warnings.InvalidPath(s));
                    }
                }
            }
        }
        return entries;
    }

    public void setMultiReleaseValue(String multiReleaseValue) {
        fsEnv = Collections.singletonMap("releaseVersion", multiReleaseValue);
    }

    private boolean contains(Collection<Path> searchPath, Path file) throws IOException {

        if (searchPath == null) {
            return false;
        }

        Path enclosingJar = null;
        if (file.getFileSystem().provider() == fsInfo.getJarFSProvider()) {
            URI uri = file.toUri();
            if (uri.getScheme().equals("jar")) {
                String ssp = uri.getSchemeSpecificPart();
                int sep = ssp.lastIndexOf("!");
                if (ssp.startsWith("file:") && sep > 0) {
                    enclosingJar = Paths.get(URI.create(ssp.substring(0, sep)));
                }
            }
        }

        Path nf = normalize(file);
        for (Path p : searchPath) {
            Path np = normalize(p);
            if (np.getFileSystem() == nf.getFileSystem()
                    && Files.isDirectory(np)
                    && nf.startsWith(np)) {
                return true;
            }
            if (enclosingJar != null
                    && Files.isSameFile(enclosingJar, np)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Utility class to help evaluate a path option. Duplicate entries are ignored, jar class paths
     * can be expanded.
     */
    private class SearchPath extends LinkedHashSet<Path> {

        private static final long serialVersionUID = 0;

        private boolean expandJarClassPaths = false;
        private final transient Set<Path> canonicalValues = new HashSet<>();

        public SearchPath expandJarClassPaths(boolean x) {
            expandJarClassPaths = x;
            return this;
        }

        /**
         * What to use when path element is the empty string
         */
        private transient Path emptyPathDefault = null;

        public SearchPath emptyPathDefault(Path x) {
            emptyPathDefault = x;
            return this;
        }

        public SearchPath addDirectories(String dirs, boolean warn) {
            boolean prev = expandJarClassPaths;
            expandJarClassPaths = true;
            try {
                if (dirs != null) {
                    for (Path dir : getPathEntries(dirs)) {
                        addDirectory(dir, warn);
                    }
                }
                return this;
            } finally {
                expandJarClassPaths = prev;
            }
        }

        public SearchPath addDirectories(String dirs) {
            return addDirectories(dirs, warn);
        }

        private void addDirectory(Path dir, boolean warn) {
            if (!Files.isDirectory(dir)) {
                if (warn) {
                    log.warning(Lint.LintCategory.PATH,
                                Warnings.DirPathElementNotFound(dir));
                }
                return;
            }

            try (Stream<Path> s = Files.list(dir)) {
                s.filter(Locations.this::isArchive)
                        .forEach(dirEntry -> addFile(dirEntry, warn));
            } catch (IOException ignore) {
            }
        }

        public SearchPath addFiles(String files, boolean warn) {
            if (files != null) {
                addFiles(getPathEntries(files, emptyPathDefault), warn);
            }
            return this;
        }

        public SearchPath addFiles(String files) {
            return addFiles(files, warn);
        }

        public SearchPath addFiles(Iterable<? extends Path> files, boolean warn) {
            if (files != null) {
                for (Path file : files) {
                    addFile(file, warn);
                }
            }
            return this;
        }

        public SearchPath addFiles(Iterable<? extends Path> files) {
            return addFiles(files, warn);
        }

        public void addFile(Path file, boolean warn) {
            if (contains(file)) {
                // discard duplicates
                return;
            }

            if (!fsInfo.exists(file)) {
                /* No such file or directory exists */
                if (warn) {
                    log.warning(Lint.LintCategory.PATH,
                                Warnings.PathElementNotFound(file));
                }
                super.add(file);
                return;
            }

            Path canonFile = fsInfo.getCanonicalFile(file);
            if (canonicalValues.contains(canonFile)) {
                /* Discard duplicates and avoid infinite recursion */
                return;
            }

            if (fsInfo.isFile(file)) {
                /* File is an ordinary file. */
                if (   !file.getFileName().toString().endsWith(".jmod")
                    && !file.endsWith("modules")) {
                    if (!isArchive(file)) {
                        /* Not a recognized extension; open it to see if
                         it looks like a valid zip file. */
                        try {
                            FileSystems.newFileSystem(file, (ClassLoader)null).close();
                            if (warn) {
                                log.warning(Lint.LintCategory.PATH,
                                            Warnings.UnexpectedArchiveFile(file));
                            }
                        } catch (IOException | ProviderNotFoundException e) {
                            // FIXME: include e.getLocalizedMessage in warning
                            if (warn) {
                                log.warning(Lint.LintCategory.PATH,
                                            Warnings.InvalidArchiveFile(file));
                            }
                            return;
                        }
                    } else {
                        if (fsInfo.getJarFSProvider() == null) {
                            log.error(Errors.NoZipfsForArchive(file));
                            return ;
                        }
                    }
                }
            }

            /* Now what we have left is either a directory or a file name
             conforming to archive naming convention */
            super.add(file);
            canonicalValues.add(canonFile);

            if (expandJarClassPaths && fsInfo.isFile(file) && !file.endsWith("modules")) {
                addJarClassPath(file, warn);
            }
        }

        // Adds referenced classpath elements from a jar's Class-Path
        // Manifest entry.  In some future release, we may want to
        // update this code to recognize URLs rather than simple
        // filenames, but if we do, we should redo all path-related code.
        private void addJarClassPath(Path jarFile, boolean warn) {
            try {
                for (Path f : fsInfo.getJarClassPath(jarFile)) {
                    addFile(f, warn);
                }
            } catch (IOException e) {
                log.error(Errors.ErrorReadingFile(jarFile, JavacFileManager.getMessage(e)));
            }
        }
    }

    /**
     * Base class for handling support for the representation of Locations.
     *
     * Locations are (by design) opaque handles that can easily be implemented
     * by enums like StandardLocation. Within JavacFileManager, each Location
     * has an associated LocationHandler, which provides much of the appropriate
     * functionality for the corresponding Location.
     *
     * @see #initHandlers
     * @see #getHandler
     */
    protected static abstract class LocationHandler {

        /**
         * @see JavaFileManager#handleOption
         */
        abstract boolean handleOption(Option option, String value);

        /**
         * @see StandardJavaFileManager#hasLocation
         */
        boolean isSet() {
            return (getPaths() != null);
        }

        abstract boolean isExplicit();

        /**
         * @see StandardJavaFileManager#getLocation
         */
        abstract Collection<Path> getPaths();

        /**
         * @see StandardJavaFileManager#setLocation
         */
        abstract void setPaths(Iterable<? extends Path> paths) throws IOException;

        /**
         * @see StandardJavaFileManager#setLocationForModule
         */
        abstract void setPathsForModule(String moduleName, Iterable<? extends Path> paths)
                throws IOException;

        /**
         * @see JavaFileManager#getLocationForModule(Location, String)
         */
        Location getLocationForModule(String moduleName) throws IOException {
            return null;
        }

        /**
         * @see JavaFileManager#getLocationForModule(Location, JavaFileObject, String)
         */
        Location getLocationForModule(Path file) throws IOException  {
            return null;
        }

        /**
         * @see JavaFileManager#inferModuleName
         */
        String inferModuleName() {
            return null;
        }

        /**
         * @see JavaFileManager#listLocationsForModules
         */
        Iterable<Set<Location>> listLocationsForModules() throws IOException {
            return null;
        }

        /**
         * @see JavaFileManager#contains
         */
        abstract boolean contains(Path file) throws IOException;
    }

    /**
     * A LocationHandler for a given Location, and associated set of options.
     */
    private static abstract class BasicLocationHandler extends LocationHandler {

        final Location location;
        final Set<Option> options;

        boolean explicit;

        /**
         * Create a handler. The location and options provide a way to map from a location or an
         * option to the corresponding handler.
         *
         * @param location the location for which this is the handler
         * @param options the options affecting this location
         * @see #initHandlers
         */
        protected BasicLocationHandler(Location location, Option... options) {
            this.location = location;
            this.options = options.length == 0
                    ? EnumSet.noneOf(Option.class)
                    : EnumSet.copyOf(Arrays.asList(options));
        }

        @Override
        void setPathsForModule(String moduleName, Iterable<? extends Path> files) throws IOException {
            // should not happen: protected by check in JavacFileManager
            throw new UnsupportedOperationException("not supported for " + location);
        }

        protected Path checkSingletonDirectory(Iterable<? extends Path> paths) throws IOException {
            Iterator<? extends Path> pathIter = paths.iterator();
            if (!pathIter.hasNext()) {
                throw new IllegalArgumentException("empty path for directory");
            }
            Path path = pathIter.next();
            if (pathIter.hasNext()) {
                throw new IllegalArgumentException("path too long for directory");
            }
            checkDirectory(path);
            return path;
        }

        protected Path checkDirectory(Path path) throws IOException {
            Objects.requireNonNull(path);
            if (!Files.exists(path)) {
                throw new FileNotFoundException(path + ": does not exist");
            }
            if (!Files.isDirectory(path)) {
                throw new IOException(path + ": not a directory");
            }
            return path;
        }

        @Override
        boolean isExplicit() {
            return explicit;
        }

    }

    /**
     * General purpose implementation for output locations, such as -d/CLASS_OUTPUT and
     * -s/SOURCE_OUTPUT. All options are treated as equivalent (i.e. aliases.)
     * The value is a single file, possibly null.
     */
    private class OutputLocationHandler extends BasicLocationHandler {

        private Path outputDir;
        private ModuleTable moduleTable;

        OutputLocationHandler(Location location, Option... options) {
            super(location, options);
        }

        @Override
        boolean handleOption(Option option, String value) {
            if (!options.contains(option)) {
                return false;
            }

            explicit = true;

            // TODO: could/should validate outputDir exists and is a directory
            // need to decide how best to report issue for benefit of
            // direct API call on JavaFileManager.handleOption(specifies IAE)
            // vs. command line decoding.
            outputDir = (value == null) ? null : getPath(value);
            return true;
        }

        @Override
        Collection<Path> getPaths() {
            return (outputDir == null) ? null : Collections.singleton(outputDir);
        }

        @Override
        void setPaths(Iterable<? extends Path> paths) throws IOException {
            if (paths == null) {
                outputDir = null;
            } else {
                explicit = true;
                outputDir = checkSingletonDirectory(paths);
            }
            moduleTable = null;
            listed = false;
        }

        @Override
        Location getLocationForModule(String name) {
            if (moduleTable == null) {
                moduleTable = new ModuleTable();
            }
            ModuleLocationHandler l = moduleTable.get(name);
            if (l == null) {
                Path out = outputDir.resolve(name);
                l = new ModuleLocationHandler(this, location.getName() + "[" + name + "]",
                        name, Collections.singletonList(out), true);
                moduleTable.add(l);
            }
            return l;
        }

        @Override
        void setPathsForModule(String name, Iterable<? extends Path> paths) throws IOException {
            Path out = checkSingletonDirectory(paths);
            if (moduleTable == null) {
                moduleTable = new ModuleTable();
            }
            ModuleLocationHandler l = moduleTable.get(name);
            if (l == null) {
                l = new ModuleLocationHandler(this, location.getName() + "[" + name + "]",
                        name, Collections.singletonList(out), true);
                moduleTable.add(l);
            } else {
                l.searchPath = Collections.singletonList(out);
                moduleTable.updatePaths(l);
            }
            explicit = true;
        }

        @Override
        Location getLocationForModule(Path file) {
            return (moduleTable == null) ? null : moduleTable.get(file);
        }

        private boolean listed;

        @Override
        Iterable<Set<Location>> listLocationsForModules() throws IOException {
            if (!listed && outputDir != null) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(outputDir)) {
                    for (Path p : stream) {
                        getLocationForModule(p.getFileName().toString());
                    }
                }
                listed = true;
            }

            if (moduleTable == null || moduleTable.isEmpty())
                return Collections.emptySet();

            return Collections.singleton(moduleTable.locations());
        }

        @Override
        boolean contains(Path file) throws IOException {
            if (moduleTable != null) {
                return moduleTable.contains(file);
            } else {
                return (outputDir) != null && normalize(file).startsWith(normalize(outputDir));
            }
        }
    }

    /**
     * General purpose implementation for search path locations,
     * such as -sourcepath/SOURCE_PATH and -processorPath/ANNOTATION_PROCESSOR_PATH.
     * All options are treated as equivalent (i.e. aliases.)
     * The value is an ordered set of files and/or directories.
     */
    private class SimpleLocationHandler extends BasicLocationHandler {

        protected Collection<Path> searchPath;

        SimpleLocationHandler(Location location, Option... options) {
            super(location, options);
        }

        @Override
        boolean handleOption(Option option, String value) {
            if (!options.contains(option)) {
                return false;
            }

            explicit = true;

            searchPath = value == null ? null
                    : Collections.unmodifiableCollection(createPath().addFiles(value));
            return true;
        }

        @Override
        Collection<Path> getPaths() {
            return searchPath;
        }

        @Override
        void setPaths(Iterable<? extends Path> files) {
            SearchPath p;
            if (files == null) {
                p = computePath(null);
            } else {
                explicit = true;
                p = createPath().addFiles(files);
            }
            searchPath = Collections.unmodifiableCollection(p);
        }

        protected SearchPath computePath(String value) {
            return createPath().addFiles(value);
        }

        protected SearchPath createPath() {
            return new SearchPath();
        }

        @Override
        boolean contains(Path file) throws IOException {
            return Locations.this.contains(searchPath, file);
        }
    }

    /**
     * Subtype of SimpleLocationHandler for -classpath/CLASS_PATH.
     * If no value is given, a default is provided, based on system properties and other values.
     */
    private class ClassPathLocationHandler extends SimpleLocationHandler {

        ClassPathLocationHandler() {
            super(StandardLocation.CLASS_PATH, Option.CLASS_PATH);
        }

        @Override
        Collection<Path> getPaths() {
            lazy();
            return searchPath;
        }

        @Override
        protected SearchPath computePath(String value) {
            String cp = value;

            // CLASSPATH environment variable when run from `javac'.
            if (cp == null) {
                cp = System.getProperty("env.class.path");
            }

            // If invoked via a java VM (not the javac launcher), use the
            // platform class path
            if (cp == null && System.getProperty("application.home") == null) {
                cp = System.getProperty("java.class.path");
            }

            // Default to current working directory.
            if (cp == null) {
                cp = ".";
            }

            return createPath().addFiles(cp);
        }

        @Override
        protected SearchPath createPath() {
            return new SearchPath()
                    .expandJarClassPaths(true) // Only search user jars for Class-Paths
                    .emptyPathDefault(getPath("."));  // Empty path elt ==> current directory
        }

        private void lazy() {
            if (searchPath == null) {
                setPaths(null);
            }
        }
    }

    /**
     * Custom subtype of LocationHandler for PLATFORM_CLASS_PATH.
     * Various options are supported for different components of the
     * platform class path.
     * Setting a value with setLocation overrides all existing option values.
     * Setting any option overrides any value set with setLocation, and
     * reverts to using default values for options that have not been set.
     * Setting -bootclasspath or -Xbootclasspath overrides any existing
     * value for -Xbootclasspath/p: and -Xbootclasspath/a:.
     */
    private class BootClassPathLocationHandler extends BasicLocationHandler {

        private Collection<Path> searchPath;
        final Map<Option, String> optionValues = new EnumMap<>(Option.class);

        /**
         * Is the bootclasspath the default?
         */
        private boolean isDefault;

        BootClassPathLocationHandler() {
            super(StandardLocation.PLATFORM_CLASS_PATH,
                    Option.BOOT_CLASS_PATH, Option.XBOOTCLASSPATH,
                    Option.XBOOTCLASSPATH_PREPEND,
                    Option.XBOOTCLASSPATH_APPEND,
                    Option.ENDORSEDDIRS, Option.DJAVA_ENDORSED_DIRS,
                    Option.EXTDIRS, Option.DJAVA_EXT_DIRS);
        }

        boolean isDefault() {
            lazy();
            return isDefault;
        }

        @Override
        boolean handleOption(Option option, String value) {
            if (!options.contains(option)) {
                return false;
            }

            explicit = true;

            option = canonicalize(option);
            optionValues.put(option, value);
            if (option == BOOT_CLASS_PATH) {
                optionValues.remove(XBOOTCLASSPATH_PREPEND);
                optionValues.remove(XBOOTCLASSPATH_APPEND);
            }
            searchPath = null;  // reset to "uninitialized"
            return true;
        }
        // where
        // TODO: would be better if option aliasing was handled at a higher
        // level
        private Option canonicalize(Option option) {
            switch (option) {
                case XBOOTCLASSPATH:
                    return Option.BOOT_CLASS_PATH;
                case DJAVA_ENDORSED_DIRS:
                    return Option.ENDORSEDDIRS;
                case DJAVA_EXT_DIRS:
                    return Option.EXTDIRS;
                default:
                    return option;
            }
        }

        @Override
        Collection<Path> getPaths() {
            lazy();
            return searchPath;
        }

        @Override
        void setPaths(Iterable<? extends Path> files) {
            if (files == null) {
                searchPath = null;  // reset to "uninitialized"
            } else {
                isDefault = false;
                explicit = true;
                SearchPath p = new SearchPath().addFiles(files, false);
                searchPath = Collections.unmodifiableCollection(p);
                optionValues.clear();
            }
        }

        SearchPath computePath() throws IOException {
            SearchPath path = new SearchPath();

            String bootclasspathOpt = optionValues.get(BOOT_CLASS_PATH);
            String endorseddirsOpt = optionValues.get(ENDORSEDDIRS);
            String extdirsOpt = optionValues.get(EXTDIRS);
            String xbootclasspathPrependOpt = optionValues.get(XBOOTCLASSPATH_PREPEND);
            String xbootclasspathAppendOpt = optionValues.get(XBOOTCLASSPATH_APPEND);
            path.addFiles(xbootclasspathPrependOpt);

            if (endorseddirsOpt != null) {
                path.addDirectories(endorseddirsOpt);
            } else {
                path.addDirectories(System.getProperty("java.endorsed.dirs"), false);
            }

            if (bootclasspathOpt != null) {
                path.addFiles(bootclasspathOpt);
            } else {
                // Standard system classes for this compiler's release.
                Collection<Path> systemClasses = systemClasses();
                if (systemClasses != null) {
                    path.addFiles(systemClasses, false);
                } else {
                    // fallback to the value of sun.boot.class.path
                    String files = System.getProperty("sun.boot.class.path");
                    path.addFiles(files, false);
                }
            }

            path.addFiles(xbootclasspathAppendOpt);

            // Strictly speaking, standard extensions are not bootstrap
            // classes, but we treat them identically, so we'll pretend
            // that they are.
            if (extdirsOpt != null) {
                path.addDirectories(extdirsOpt);
            } else {
                // Add lib/jfxrt.jar to the search path
               Path jfxrt = javaHome.resolve("lib/jfxrt.jar");
                if (Files.exists(jfxrt)) {
                    path.addFile(jfxrt, false);
                }
                path.addDirectories(System.getProperty("java.ext.dirs"), false);
            }

            isDefault =
                       (xbootclasspathPrependOpt == null)
                    && (bootclasspathOpt == null)
                    && (xbootclasspathAppendOpt == null);

            return path;
        }

        /**
         * Return a collection of files containing system classes.
         * Returns {@code null} if not running on a modular image.
         *
         * @throws UncheckedIOException if an I/O errors occurs
         */
        private Collection<Path> systemClasses() throws IOException {
            // Return "modules" jimage file if available
            if (Files.isRegularFile(thisSystemModules)) {
                return Collections.singleton(thisSystemModules);
            }

            // Exploded module image
            Path modules = javaHome.resolve("modules");
            if (Files.isDirectory(modules.resolve("java.base"))) {
                try (Stream<Path> listedModules = Files.list(modules)) {
                    return listedModules.toList();
                }
            }

            // not a modular image that we know about
            return null;
        }

        private void lazy() {
            if (searchPath == null) {
                try {
                    searchPath = Collections.unmodifiableCollection(computePath());
                } catch (IOException e) {
                    // TODO: need better handling here, e.g. javac Abort?
                    throw new UncheckedIOException(e);
                }
            }
        }

        @Override
        boolean contains(Path file) throws IOException {
            return Locations.this.contains(searchPath, file);
        }
    }

    /**
     * A LocationHandler to represent modules found from a module-oriented
     * location such as MODULE_SOURCE_PATH, UPGRADE_MODULE_PATH,
     * SYSTEM_MODULES and MODULE_PATH.
     *
     * The Location can be specified to accept overriding classes from the
     * {@code --patch-module <module>=<path> } parameter.
     */
    private class ModuleLocationHandler extends LocationHandler implements Location {
        private final LocationHandler parent;
        private final String name;
        private final String moduleName;
        private final boolean output;
        boolean explicit;
        Collection<Path> searchPath;

        ModuleLocationHandler(LocationHandler parent, String name, String moduleName,
                Collection<Path> searchPath, boolean output) {
            this.parent = parent;
            this.name = name;
            this.moduleName = moduleName;
            this.searchPath = searchPath;
            this.output = output;
        }

        @Override @DefinedBy(Api.COMPILER)
        public String getName() {
            return name;
        }

        @Override @DefinedBy(Api.COMPILER)
        public boolean isOutputLocation() {
            return output;
        }

        @Override // defined by LocationHandler
        boolean handleOption(Option option, String value) {
            throw new UnsupportedOperationException();
        }

        @Override // defined by LocationHandler
        Collection<Path> getPaths() {
            return Collections.unmodifiableCollection(searchPath);
        }

        @Override
        boolean isExplicit() {
            return true;
        }

        @Override // defined by LocationHandler
        void setPaths(Iterable<? extends Path> paths) throws IOException {
            // defer to the parent to determine if this is acceptable
            parent.setPathsForModule(moduleName, paths);
        }

        @Override // defined by LocationHandler
        void setPathsForModule(String moduleName, Iterable<? extends Path> paths) {
            throw new UnsupportedOperationException("not supported for " + name);
        }

        @Override // defined by LocationHandler
        String inferModuleName() {
            return moduleName;
        }

        @Override
        boolean contains(Path file) throws IOException {
            return Locations.this.contains(searchPath, file);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * A table of module location handlers, indexed by name and path.
     */
    private class ModuleTable {
        private final Map<String, ModuleLocationHandler> nameMap = new LinkedHashMap<>();
        private final Map<Path, ModuleLocationHandler> pathMap = new LinkedHashMap<>();

        void add(ModuleLocationHandler h) {
            nameMap.put(h.moduleName, h);
            for (Path p : h.searchPath) {
                pathMap.put(normalize(p), h);
            }
        }

        void updatePaths(ModuleLocationHandler h) {
            // use iterator, to be able to remove old entries
            for (Iterator<Map.Entry<Path, ModuleLocationHandler>> iter = pathMap.entrySet().iterator();
                    iter.hasNext(); ) {
                Map.Entry<Path, ModuleLocationHandler> e = iter.next();
                if (e.getValue() == h) {
                    iter.remove();
                }
            }
            for (Path p : h.searchPath) {
                pathMap.put(normalize(p), h);
            }
        }

        ModuleLocationHandler get(String name) {
            return nameMap.get(name);
        }

        ModuleLocationHandler get(Path path) {
            while (path != null) {
                ModuleLocationHandler l = pathMap.get(path);

                if (l != null)
                    return l;

                path = path.getParent();
            }

            return null;
        }

        void clear() {
            nameMap.clear();
            pathMap.clear();
        }

        boolean isEmpty() {
            return nameMap.isEmpty();
        }

        boolean contains(Path file) throws IOException {
            return Locations.this.contains(pathMap.keySet(), file);
        }

        Set<Location> locations() {
            return Collections.unmodifiableSet(nameMap.values().stream().collect(Collectors.toSet()));
        }

        Set<Location> explicitLocations() {
            return Collections.unmodifiableSet(nameMap.entrySet()
                                                      .stream()
                                                      .filter(e -> e.getValue().explicit)
                                                      .map(e -> e.getValue())
                                                      .collect(Collectors.toSet()));
        }
    }

    /**
     * A LocationHandler for simple module-oriented search paths,
     * like UPGRADE_MODULE_PATH and MODULE_PATH.
     */
    private class ModulePathLocationHandler extends SimpleLocationHandler {
        private ModuleTable moduleTable;

        ModulePathLocationHandler(Location location, Option... options) {
            super(location, options);
        }

        @Override
        public boolean handleOption(Option option, String value) {
            if (!options.contains(option)) {
                return false;
            }
            setPaths(value == null ? null : getPathEntries(value));
            return true;
        }

        @Override
        public Location getLocationForModule(String moduleName) {
            initModuleLocations();
            return moduleTable.get(moduleName);
        }

        @Override
        public Location getLocationForModule(Path file) {
            initModuleLocations();
            return moduleTable.get(file);
        }

        @Override
        Iterable<Set<Location>> listLocationsForModules() {
            Set<Location> explicitLocations = moduleTable != null ?
                    moduleTable.explicitLocations() : Collections.emptySet();
            Iterable<Set<Location>> explicitLocationsList = !explicitLocations.isEmpty()
                    ? Collections.singletonList(explicitLocations)
                    : Collections.emptyList();

            if (searchPath == null)
                return explicitLocationsList;

            Iterable<Set<Location>> searchPathLocations =
                    () -> new ModulePathIterator();
            return () -> Iterators.createCompoundIterator(Arrays.asList(explicitLocationsList,
                                                                        searchPathLocations),
                                                          Iterable::iterator);
        }

        @Override
        boolean contains(Path file) throws IOException {
            if (moduleTable == null) {
                initModuleLocations();
            }
            return moduleTable.contains(file);
        }

        @Override
        void setPaths(Iterable<? extends Path> paths) {
            if (paths != null) {
                for (Path p: paths) {
                    checkValidModulePathEntry(p);
                }
            }
            super.setPaths(paths);
            moduleTable = null;
        }

        @Override
        void setPathsForModule(String name, Iterable<? extends Path> paths) throws IOException {
            List<Path> checkedPaths = checkPaths(paths);
            // how far should we go to validate the paths provide a module?
            // e.g. contain module-info with the correct name?
            initModuleLocations();
            ModuleLocationHandler l = moduleTable.get(name);
            if (l == null) {
                l = new ModuleLocationHandler(this, location.getName() + "[" + name + "]",
                        name, checkedPaths, true);
                moduleTable.add(l);
           } else {
                l.searchPath = checkedPaths;
                moduleTable.updatePaths(l);
            }
            l.explicit = true;
            explicit = true;
        }

        private List<Path> checkPaths(Iterable<? extends Path> paths) throws IOException {
            Objects.requireNonNull(paths);
            List<Path> validPaths = new ArrayList<>();
            for (Path p : paths) {
                validPaths.add(checkDirectory(p));
            }
            return validPaths;
        }

        private void initModuleLocations() {
            if (moduleTable != null) {
                return;
            }

            moduleTable = new ModuleTable();

            for (Set<Location> set : listLocationsForModules()) {
                for (Location locn : set) {
                    if (locn instanceof ModuleLocationHandler moduleLocationHandler) {
                        if (!moduleTable.nameMap.containsKey(moduleLocationHandler.moduleName)) {
                            moduleTable.add(moduleLocationHandler);
                        }
                    }
                }
            }
        }

        private void checkValidModulePathEntry(Path p) {
            if (!Files.exists(p)) {
                // warning may be generated later
                return;
            }

            if (Files.isDirectory(p)) {
                // either an exploded module or a directory of modules
                return;
            }

            String name = p.getFileName().toString();
            int lastDot = name.lastIndexOf(".");
            if (lastDot > 0) {
                switch (name.substring(lastDot)) {
                    case ".jar":
                    case ".jmod":
                        return;
                }
            }
            throw new IllegalArgumentException(p.toString());
        }

        class ModulePathIterator implements Iterator<Set<Location>> {
            Iterator<Path> pathIter = searchPath.iterator();
            int pathIndex = 0;
            Set<Location> next = null;

            @Override
            public boolean hasNext() {
                if (next != null)
                    return true;

                while (next == null) {
                    if (pathIter.hasNext()) {
                        Path path = pathIter.next();
                        if (Files.isDirectory(path)) {
                            next = scanDirectory(path);
                        } else {
                            next = scanFile(path);
                        }
                        pathIndex++;
                    } else
                        return false;
                }
                return true;
            }

            @Override
            public Set<Location> next() {
                hasNext();
                if (next != null) {
                    Set<Location> result = next;
                    next = null;
                    return result;
                }
                throw new NoSuchElementException();
            }

            private Set<Location> scanDirectory(Path path) {
                Set<Path> paths = new LinkedHashSet<>();
                Path moduleInfoClass = null;
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                    for (Path entry: stream) {
                        if (entry.endsWith("module-info.class")) {
                            moduleInfoClass = entry;
                            break;  // no need to continue scanning
                        }
                        paths.add(entry);
                    }
                } catch (DirectoryIteratorException | IOException ignore) {
                    log.error(Errors.LocnCantReadDirectory(path));
                    return Collections.emptySet();
                }

                if (moduleInfoClass != null) {
                    // It's an exploded module directly on the module path.
                    // We can't infer module name from the directory name, so have to
                    // read module-info.class.
                    try {
                        String moduleName = readModuleName(moduleInfoClass);
                        String name = location.getName()
                                + "[" + pathIndex + ":" + moduleName + "]";
                        ModuleLocationHandler l = new ModuleLocationHandler(
                                ModulePathLocationHandler.this, name, moduleName,
                                Collections.singletonList(path), false);
                        return Collections.singleton(l);
                    } catch (ModuleNameReader.BadClassFile e) {
                        log.error(Errors.LocnBadModuleInfo(path));
                        return Collections.emptySet();
                    } catch (IOException e) {
                        log.error(Errors.LocnCantReadFile(path));
                        return Collections.emptySet();
                    }
                }

                // A directory of modules
                Set<Location> result = new LinkedHashSet<>();
                int index = 0;
                for (Path entry : paths) {
                    Pair<String,Path> module = inferModuleName(entry);
                    if (module == null) {
                        // diagnostic reported if necessary; skip to next
                        continue;
                    }
                    String moduleName = module.fst;
                    Path modulePath = module.snd;
                    String name = location.getName()
                            + "[" + pathIndex + "." + (index++) + ":" + moduleName + "]";
                    ModuleLocationHandler l = new ModuleLocationHandler(
                            ModulePathLocationHandler.this, name, moduleName,
                            Collections.singletonList(modulePath), false);
                    result.add(l);
                }
                return result;
            }

            private Set<Location> scanFile(Path path) {
                Pair<String,Path> module = inferModuleName(path);
                if (module == null) {
                    // diagnostic reported if necessary
                    return Collections.emptySet();
                }
                String moduleName = module.fst;
                Path modulePath = module.snd;
                String name = location.getName()
                        + "[" + pathIndex + ":" + moduleName + "]";
                ModuleLocationHandler l = new ModuleLocationHandler(
                        ModulePathLocationHandler.this, name, moduleName,
                        Collections.singletonList(modulePath), false);
                return Collections.singleton(l);
            }

            private Pair<String,Path> inferModuleName(Path p) {
                if (Files.isDirectory(p)) {
                    if (Files.exists(p.resolve("module-info.class")) ||
                        Files.exists(p.resolve("module-info.sig"))) {
                        String name = p.getFileName().toString();
                        if (SourceVersion.isName(name))
                            return new Pair<>(name, p);
                    }
                    return null;
                }

                if (p.getFileName().toString().endsWith(".jar") && fsInfo.exists(p)) {
                    FileSystemProvider jarFSProvider = fsInfo.getJarFSProvider();
                    if (jarFSProvider == null) {
                        log.error(Errors.NoZipfsForArchive(p));
                        return null;
                    }
                    try (FileSystem fs = jarFSProvider.newFileSystem(p, fsEnv)) {
                        Path moduleInfoClass = fs.getPath("module-info.class");
                        if (Files.exists(moduleInfoClass)) {
                            String moduleName = readModuleName(moduleInfoClass);
                            return new Pair<>(moduleName, p);
                        }
                        Path mf = fs.getPath("META-INF/MANIFEST.MF");
                        if (Files.exists(mf)) {
                            try (InputStream in = Files.newInputStream(mf)) {
                                Manifest man = new Manifest(in);
                                Attributes attrs = man.getMainAttributes();
                                if (attrs != null) {
                                    String moduleName = attrs.getValue(new Attributes.Name("Automatic-Module-Name"));
                                    if (moduleName != null) {
                                        if (isModuleName(moduleName)) {
                                            return new Pair<>(moduleName, p);
                                        } else {
                                            log.error(Errors.LocnCantGetModuleNameForJar(p));
                                            return null;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (ModuleNameReader.BadClassFile e) {
                        log.error(Errors.LocnBadModuleInfo(p));
                        return null;
                    } catch (IOException e) {
                        log.error(Errors.LocnCantReadFile(p));
                        return null;
                    }

                    //automatic module:
                    String fn = p.getFileName().toString();
                    //from ModulePath.deriveModuleDescriptor:

                    // drop .jar
                    String mn = fn.substring(0, fn.length()-4);

                    // find first occurrence of -${NUMBER}. or -${NUMBER}$
                    Matcher matcher = Pattern.compile("-(\\d+(\\.|$))").matcher(mn);
                    if (matcher.find()) {
                        int start = matcher.start();

                        mn = mn.substring(0, start);
                    }

                    // finally clean up the module name
                    mn =  mn.replaceAll("[^A-Za-z0-9]", ".")  // replace non-alphanumeric
                            .replaceAll("(\\.)(\\1)+", ".")   // collapse repeating dots
                            .replaceAll("^\\.", "")           // drop leading dots
                            .replaceAll("\\.$", "");          // drop trailing dots


                    if (!mn.isEmpty()) {
                        return new Pair<>(mn, p);
                    }

                    log.error(Errors.LocnCantGetModuleNameForJar(p));
                    return null;
                }

                if (p.getFileName().toString().endsWith(".jmod")) {
                    try {
                        // check if the JMOD file is valid
                        JmodFile.checkMagic(p);

                        // No JMOD file system.  Use JarFileSystem to
                        // workaround for now
                        FileSystem fs = fileSystems.get(p);
                        if (fs == null) {
                            FileSystemProvider jarFSProvider = fsInfo.getJarFSProvider();
                            if (jarFSProvider == null) {
                                log.error(Errors.LocnCantReadFile(p));
                                return null;
                            }
                            fs = jarFSProvider.newFileSystem(p, Collections.emptyMap());
                            try {
                                Path moduleInfoClass = fs.getPath("classes/module-info.class");
                                String moduleName = readModuleName(moduleInfoClass);
                                Path modulePath = fs.getPath("classes");
                                fileSystems.put(p, fs);
                                closeables.add(fs);
                                fs = null; // prevent fs being closed in the finally clause
                                return new Pair<>(moduleName, modulePath);
                            } finally {
                                if (fs != null)
                                    fs.close();
                            }
                        }
                    } catch (ModuleNameReader.BadClassFile e) {
                        log.error(Errors.LocnBadModuleInfo(p));
                    } catch (IOException e) {
                        log.error(Errors.LocnCantReadFile(p));
                        return null;
                    }
                }

                if (warn && false) {  // temp disable, when enabled, massage examples.not-yet.txt suitably.
                    log.warning(Warnings.LocnUnknownFileOnModulePath(p));
                }
                return null;
            }

            private String readModuleName(Path path) throws IOException, ModuleNameReader.BadClassFile {
                if (moduleNameReader == null)
                    moduleNameReader = new ModuleNameReader();
                return moduleNameReader.readModuleName(path);
            }
        }

        //from jdk.internal.module.Checks:
        /**
         * Returns {@code true} if the given name is a legal module name.
         */
        private boolean isModuleName(String name) {
            int next;
            int off = 0;
            while ((next = name.indexOf('.', off)) != -1) {
                String id = name.substring(off, next);
                if (!SourceVersion.isName(id))
                    return false;
                off = next+1;
            }
            String last = name.substring(off);
            return SourceVersion.isName(last);
        }
    }

    private class ModuleSourcePathLocationHandler extends BasicLocationHandler {
        private ModuleTable moduleTable;
        private List<Path> paths;

        ModuleSourcePathLocationHandler() {
            super(StandardLocation.MODULE_SOURCE_PATH,
                    Option.MODULE_SOURCE_PATH);
        }

        @Override
        boolean handleOption(Option option, String value) {
            explicit = true;
            init(value);
            return true;
        }

        /**
         * Initializes the module table, based on a string containing the composition
         * of a series of command-line options.
         * At most one pattern to initialize a series of modules can be given.
         * At most one module-specific search path per module can be given.
         *
         * @param value a series of values, separated by NUL.
         */
        void init(String value) {
            Pattern moduleSpecificForm = Pattern.compile("([\\p{Alnum}$_.]+)=(.*)");
            List<String> pathsForModules = new ArrayList<>();
            String modulePattern = null;
            for (String v : value.split("\0")) {
                if (moduleSpecificForm.matcher(v).matches()) {
                    pathsForModules.add(v);
                } else {
                    modulePattern = v;
                }
            }
            // set the general module pattern first, if given
            if (modulePattern != null) {
                initFromPattern(modulePattern);
            }
            pathsForModules.forEach(this::initForModule);
        }

        /**
         * Initializes a module-specific override, using {@code setPathsForModule}.
         *
         * @param value a string of the form: module-name=search-path
         */
        void initForModule(String value) {
            int eq = value.indexOf('=');
            String name = value.substring(0, eq);
            List<Path> paths = new ArrayList<>();
            for (String v : value.substring(eq + 1).split(File.pathSeparator)) {
                try {
                    paths.add(Paths.get(v));
                } catch (InvalidPathException e) {
                    throw new IllegalArgumentException("invalid path: " + v, e);
                }
            }
            try {
                setPathsForModule(name, paths);
            } catch (IOException e) {
                e.printStackTrace();
                throw new IllegalArgumentException("cannot set path for module " + name, e);
            }
        }

        /**
         * Initializes the module table based on a custom option syntax.
         *
         * @param value the value such as may be given to a --module-source-path option
         */
        void initFromPattern(String value) {
            Collection<String> segments = new ArrayList<>();
            for (String s: value.split(File.pathSeparator)) {
                expandBraces(s, segments);
            }

            Map<String, List<Path>> map = new LinkedHashMap<>();
            List<Path> noSuffixPaths = new ArrayList<>();
            boolean anySuffix = false;
            final String MARKER = "*";
            for (String seg: segments) {
                int markStart = seg.indexOf(MARKER);
                if (markStart == -1) {
                    Path p = getPath(seg);
                    add(map, p, null);
                    noSuffixPaths.add(p);
                } else {
                    if (markStart == 0 || !isSeparator(seg.charAt(markStart - 1))) {
                        throw new IllegalArgumentException("illegal use of " + MARKER + " in " + seg);
                    }
                    Path prefix = getPath(seg.substring(0, markStart - 1));
                    Path suffix;
                    int markEnd = markStart + MARKER.length();
                    if (markEnd == seg.length()) {
                        suffix = null;
                    } else if (!isSeparator(seg.charAt(markEnd))
                            || seg.indexOf(MARKER, markEnd) != -1) {
                        throw new IllegalArgumentException("illegal use of " + MARKER + " in " + seg);
                    } else {
                        suffix = getPath(seg.substring(markEnd + 1));
                        anySuffix = true;
                    }
                    add(map, prefix, suffix);
                    if (suffix == null) {
                        noSuffixPaths.add(prefix);
                    }
                }
            }

            initModuleTable(map);
            paths = anySuffix ? null : noSuffixPaths;
        }

        private void initModuleTable(Map<String, List<Path>> map) {
            moduleTable = new ModuleTable();
            map.forEach((modName, modPath) -> {
                boolean hasModuleInfo = modPath.stream().anyMatch(checkModuleInfo);
                if (hasModuleInfo) {
                    String locnName = location.getName() + "[" + modName + "]";
                    ModuleLocationHandler l = new ModuleLocationHandler(this, locnName, modName,
                            modPath, false);
                    moduleTable.add(l);
                }
            });
        }
        //where:
            private final Predicate<Path> checkModuleInfo =
                    p -> Files.exists(p.resolve("module-info.java"));


        private boolean isSeparator(char ch) {
            // allow both separators on Windows
            return (ch == File.separatorChar) || (ch == '/');
        }

        void add(Map<String, List<Path>> map, Path prefix, Path suffix) {
            if (!Files.isDirectory(prefix)) {
                if (warn) {
                    Warning key = Files.exists(prefix)
                            ? Warnings.DirPathElementNotDirectory(prefix)
                            : Warnings.DirPathElementNotFound(prefix);
                    log.warning(Lint.LintCategory.PATH, key);
                }
                return;
            }
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(prefix, path -> Files.isDirectory(path))) {
                for (Path entry: stream) {
                    Path path = (suffix == null) ? entry : entry.resolve(suffix);
                    if (Files.isDirectory(path)) {
                        String name = entry.getFileName().toString();
                        List<Path> paths = map.get(name);
                        if (paths == null)
                            map.put(name, paths = new ArrayList<>());
                        paths.add(path);
                    }
                }
            } catch (IOException e) {
                // TODO? What to do?
                System.err.println(e);
            }
        }

        private void expandBraces(String value, Collection<String> results) {
            int depth = 0;
            int start = -1;
            String prefix = null;
            String suffix = null;
            for (int i = 0; i < value.length(); i++) {
                switch (value.charAt(i)) {
                    case '{':
                        depth++;
                        if (depth == 1) {
                            prefix = value.substring(0, i);
                            suffix = value.substring(getMatchingBrace(value, i) + 1);
                            start = i + 1;
                        }
                        break;

                    case ',':
                        if (depth == 1) {
                            String elem = value.substring(start, i);
                            expandBraces(prefix + elem + suffix, results);
                            start = i + 1;
                        }
                        break;

                    case '}':
                        switch (depth) {
                            case 0:
                                throw new IllegalArgumentException("mismatched braces");

                            case 1:
                                String elem = value.substring(start, i);
                                expandBraces(prefix + elem + suffix, results);
                                return;

                            default:
                                depth--;
                        }
                        break;
                }
            }
            if (depth > 0)
                throw new IllegalArgumentException("mismatched braces");
            results.add(value);
        }

        int getMatchingBrace(String value, int offset) {
            int depth = 1;
            for (int i = offset + 1; i < value.length(); i++) {
                switch (value.charAt(i)) {
                    case '{':
                        depth++;
                        break;

                    case '}':
                        if (--depth == 0)
                            return i;
                        break;
                }
            }
            throw new IllegalArgumentException("mismatched braces");
        }

        @Override
        boolean isSet() {
            return (moduleTable != null);
        }

        @Override
        Collection<Path> getPaths() {
            if (paths == null) {
                // This may occur for a complex setting with --module-source-path option
                // i.e. one that cannot be represented by a simple series of paths.
                throw new IllegalStateException("paths not available");
            }
            return paths;
        }

        @Override
        void setPaths(Iterable<? extends Path> files) throws IOException {
            Map<String, List<Path>> map = new LinkedHashMap<>();
            List<Path> newPaths = new ArrayList<>();
            for (Path file : files) {
                add(map, file, null);
                newPaths.add(file);
            }

            initModuleTable(map);
            explicit = true;
            paths = Collections.unmodifiableList(newPaths);
        }

        @Override
        void setPathsForModule(String name, Iterable<? extends Path> paths) throws IOException {
            List<Path> validPaths = checkPaths(paths);

            if (moduleTable == null)
                moduleTable = new ModuleTable();

            ModuleLocationHandler l = moduleTable.get(name);
            if (l == null) {
                l = new ModuleLocationHandler(this,
                        location.getName() + "[" + name + "]",
                        name,
                        validPaths,
                        true);
                moduleTable.add(l);
           } else {
                l.searchPath = validPaths;
                moduleTable.updatePaths(l);
            }
            explicit = true;
        }

        private List<Path> checkPaths(Iterable<? extends Path> paths) throws IOException {
            Objects.requireNonNull(paths);
            List<Path> validPaths = new ArrayList<>();
            for (Path p : paths) {
                validPaths.add(checkDirectory(p));
            }
            return validPaths;
        }

        @Override
        Location getLocationForModule(String name) {
            return (moduleTable == null) ? null : moduleTable.get(name);
        }

        @Override
        Location getLocationForModule(Path file) {
            return (moduleTable == null) ? null : moduleTable.get(file);
        }

        @Override
        Iterable<Set<Location>> listLocationsForModules() {
            if (moduleTable == null)
                return Collections.emptySet();

            return Collections.singleton(moduleTable.locations());
        }

        @Override
        boolean contains(Path file) throws IOException {
            return (moduleTable == null) ? false : moduleTable.contains(file);
        }

    }

    private class SystemModulesLocationHandler extends BasicLocationHandler {
        private Path systemJavaHome;
        private Path modules;
        private ModuleTable moduleTable;

        SystemModulesLocationHandler() {
            super(StandardLocation.SYSTEM_MODULES, Option.SYSTEM);
            systemJavaHome = Locations.javaHome;
        }

        @Override
        boolean handleOption(Option option, String value) {
            if (!options.contains(option)) {
                return false;
            }

            explicit = true;

            if (value == null) {
                systemJavaHome = Locations.javaHome;
            } else if (value.equals("none")) {
                systemJavaHome = null;
            } else {
                update(getPath(value));
            }

            modules = null;
            return true;
        }

        @Override
        Collection<Path> getPaths() {
            return (systemJavaHome == null) ? null : Collections.singleton(systemJavaHome);
        }

        @Override
        void setPaths(Iterable<? extends Path> files) throws IOException {
            if (files == null) {
                systemJavaHome = null;
            } else {
                explicit = true;

                Path dir = checkSingletonDirectory(files);
                update(dir);
            }
        }

        @Override
        void setPathsForModule(String name, Iterable<? extends Path> paths) throws IOException {
            List<Path> checkedPaths = checkPaths(paths);
            initSystemModules();
            ModuleLocationHandler l = moduleTable.get(name);
            if (l == null) {
                l = new ModuleLocationHandler(this,
                        location.getName() + "[" + name + "]",
                        name,
                        checkedPaths,
                        true);
                moduleTable.add(l);
           } else {
                l.searchPath = checkedPaths;
                moduleTable.updatePaths(l);
            }
            explicit = true;
        }

        private List<Path> checkPaths(Iterable<? extends Path> paths) throws IOException {
            Objects.requireNonNull(paths);
            List<Path> validPaths = new ArrayList<>();
            for (Path p : paths) {
                validPaths.add(checkDirectory(p));
            }
            return validPaths;
        }

        private void update(Path p) {
            if (!isCurrentPlatform(p) && !Files.exists(p.resolve("lib").resolve("jrt-fs.jar")) &&
                    !Files.exists(systemJavaHome.resolve("modules")))
                throw new IllegalArgumentException(p.toString());
            systemJavaHome = p;
            modules = null;
        }

        private boolean isCurrentPlatform(Path p) {
            try {
                return Files.isSameFile(p, Locations.javaHome);
            } catch (IOException ex) {
                throw new IllegalArgumentException(p.toString(), ex);
            }
        }

        @Override
        Location getLocationForModule(String name) throws IOException {
            initSystemModules();
            return moduleTable.get(name);
        }

        @Override
        Location getLocationForModule(Path file) throws IOException {
            initSystemModules();
            return moduleTable.get(file);
        }

        @Override
        Iterable<Set<Location>> listLocationsForModules() throws IOException {
            initSystemModules();
            return Collections.singleton(moduleTable.locations());
        }

        @Override
        boolean contains(Path file) throws IOException {
            initSystemModules();
            return moduleTable.contains(file);
        }

        private void initSystemModules() throws IOException {
            if (moduleTable != null)
                return;

            if (systemJavaHome == null) {
                moduleTable = new ModuleTable();
                return;
            }

            if (modules == null) {
                try {
                    URI jrtURI = URI.create("jrt:/");
                    FileSystem jrtfs;

                    if (isCurrentPlatform(systemJavaHome)) {
                        jrtfs = FileSystems.getFileSystem(jrtURI);
                    } else {
                        try {
                            Map<String, String> attrMap =
                                    Collections.singletonMap("java.home", systemJavaHome.toString());
                            jrtfs = FileSystems.newFileSystem(jrtURI, attrMap);
                        } catch (ProviderNotFoundException ex) {
                            URL javaHomeURL = systemJavaHome.resolve("jrt-fs.jar").toUri().toURL();
                            ClassLoader currentLoader = Locations.class.getClassLoader();
                            URLClassLoader fsLoader =
                                    new URLClassLoader(new URL[] {javaHomeURL}, currentLoader);

                            jrtfs = FileSystems.newFileSystem(jrtURI, Collections.emptyMap(), fsLoader);

                            closeables.add(fsLoader);
                        }

                        closeables.add(jrtfs);
                    }

                    modules = jrtfs.getPath("/modules");
                } catch (FileSystemNotFoundException | ProviderNotFoundException e) {
                    modules = systemJavaHome.resolve("modules");
                    if (!Files.exists(modules))
                        throw new IOException("can't find system classes", e);
                }
            }

            moduleTable = new ModuleTable();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(modules, Files::isDirectory)) {
                for (Path entry : stream) {
                    String moduleName = entry.getFileName().toString();
                    String name = location.getName() + "[" + moduleName + "]";
                    ModuleLocationHandler h = new ModuleLocationHandler(this,
                            name, moduleName, Collections.singletonList(entry), false);
                    moduleTable.add(h);
                }
            }
        }
    }

    private class PatchModulesLocationHandler extends BasicLocationHandler {
        private final ModuleTable moduleTable = new ModuleTable();

        PatchModulesLocationHandler() {
            super(StandardLocation.PATCH_MODULE_PATH, Option.PATCH_MODULE);
        }

        @Override
        boolean handleOption(Option option, String value) {
            if (!options.contains(option)) {
                return false;
            }

            explicit = true;

            moduleTable.clear();

            // Allow an extended syntax for --patch-module consisting of a series
            // of values separated by NULL characters. This is to facilitate
            // supporting deferred file manager options on the command line.
            // See Option.PATCH_MODULE for the code that composes these multiple
            // values.
            for (String v : value.split("\0")) {
                int eq = v.indexOf('=');
                if (eq > 0) {
                    String moduleName = v.substring(0, eq);
                    SearchPath mPatchPath = new SearchPath()
                            .addFiles(v.substring(eq + 1));
                    String name = location.getName() + "[" + moduleName + "]";
                    ModuleLocationHandler h = new ModuleLocationHandler(this, name,
                            moduleName, mPatchPath, false);
                    moduleTable.add(h);
                } else {
                    // Should not be able to get here;
                    // this should be caught and handled in Option.PATCH_MODULE
                    log.error(Errors.LocnInvalidArgForXpatch(value));
                }
            }

            return true;
        }

        @Override
        boolean isSet() {
            return !moduleTable.isEmpty();
        }

        @Override
        Collection<Path> getPaths() {
            throw new UnsupportedOperationException();
        }

        @Override
        void setPaths(Iterable<? extends Path> files) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override // defined by LocationHandler
        void setPathsForModule(String moduleName, Iterable<? extends Path> files) throws IOException {
            throw new UnsupportedOperationException(); // not yet
        }

        @Override
        Location getLocationForModule(String name) throws IOException {
            return moduleTable.get(name);
        }

        @Override
        Location getLocationForModule(Path file) throws IOException {
            return moduleTable.get(file);
        }

        @Override
        Iterable<Set<Location>> listLocationsForModules() throws IOException {
            return Collections.singleton(moduleTable.locations());
        }

        @Override
        boolean contains(Path file) throws IOException {
            return moduleTable.contains(file);
        }
    }

    Map<Location, LocationHandler> handlersForLocation;
    Map<Option, LocationHandler> handlersForOption;

    void initHandlers() {
        handlersForLocation = new HashMap<>();
        handlersForOption = new EnumMap<>(Option.class);

        BasicLocationHandler[] handlers = {
            new BootClassPathLocationHandler(),
            new ClassPathLocationHandler(),
            new SimpleLocationHandler(StandardLocation.SOURCE_PATH, Option.SOURCE_PATH),
            new SimpleLocationHandler(StandardLocation.ANNOTATION_PROCESSOR_PATH, Option.PROCESSOR_PATH),
            new SimpleLocationHandler(StandardLocation.ANNOTATION_PROCESSOR_MODULE_PATH, Option.PROCESSOR_MODULE_PATH),
            new OutputLocationHandler(StandardLocation.CLASS_OUTPUT, Option.D),
            new OutputLocationHandler(StandardLocation.SOURCE_OUTPUT, Option.S),
            new OutputLocationHandler(StandardLocation.NATIVE_HEADER_OUTPUT, Option.H),
            new ModuleSourcePathLocationHandler(),
            new PatchModulesLocationHandler(),
            new ModulePathLocationHandler(StandardLocation.UPGRADE_MODULE_PATH, Option.UPGRADE_MODULE_PATH),
            new ModulePathLocationHandler(StandardLocation.MODULE_PATH, Option.MODULE_PATH),
            new SystemModulesLocationHandler(),
        };

        for (BasicLocationHandler h : handlers) {
            handlersForLocation.put(h.location, h);
            for (Option o : h.options) {
                handlersForOption.put(o, h);
            }
        }
    }

    boolean handleOption(Option option, String value) {
        LocationHandler h = handlersForOption.get(option);
        return (h == null ? false : h.handleOption(option, value));
    }

    boolean hasLocation(Location location) {
        LocationHandler h = getHandler(location);
        return (h == null ? false : h.isSet());
    }

    boolean hasExplicitLocation(Location location) {
        LocationHandler h = getHandler(location);
        return (h == null ? false : h.isExplicit());
    }

    Collection<Path> getLocation(Location location) {
        LocationHandler h = getHandler(location);
        return (h == null ? null : h.getPaths());
    }

    Path getOutputLocation(Location location) {
        if (!location.isOutputLocation()) {
            throw new IllegalArgumentException();
        }
        LocationHandler h = getHandler(location);
        return ((OutputLocationHandler) h).outputDir;
    }

    void setLocation(Location location, Iterable<? extends Path> files) throws IOException {
        LocationHandler h = getHandler(location);
        if (h == null) {
            if (location.isOutputLocation()) {
                h = new OutputLocationHandler(location);
            } else {
                h = new SimpleLocationHandler(location);
            }
            handlersForLocation.put(location, h);
        }
        h.setPaths(files);
    }

    Location getLocationForModule(Location location, String name) throws IOException {
        LocationHandler h = getHandler(location);
        return (h == null ? null : h.getLocationForModule(name));
    }

    Location getLocationForModule(Location location, Path file) throws IOException {
        LocationHandler h = getHandler(location);
        return (h == null ? null : h.getLocationForModule(file));
    }

    void setLocationForModule(Location location, String moduleName,
            Iterable<? extends Path> files) throws IOException {
        LocationHandler h = getHandler(location);
        if (h == null) {
            if (location.isOutputLocation()) {
                h = new OutputLocationHandler(location);
            } else {
                h = new ModulePathLocationHandler(location);
            }
            handlersForLocation.put(location, h);
        }
        h.setPathsForModule(moduleName, files);
    }

    String inferModuleName(Location location) {
        LocationHandler h = getHandler(location);
        return (h == null ? null : h.inferModuleName());
    }

    Iterable<Set<Location>> listLocationsForModules(Location location) throws IOException {
        LocationHandler h = getHandler(location);
        return (h == null ? null : h.listLocationsForModules());
    }

    boolean contains(Location location, Path file) throws IOException {
        LocationHandler h = getHandler(location);
        if (h == null)
            throw new IllegalArgumentException("unknown location");
        return h.contains(file);
    }

    protected LocationHandler getHandler(Location location) {
        Objects.requireNonNull(location);
        return (location instanceof LocationHandler locationHandler)
                ? locationHandler
                : handlersForLocation.get(location);
    }

    /**
     * Is this the name of an archive file?
     */
    private boolean isArchive(Path file) {
        String n = StringUtils.toLowerCase(file.getFileName().toString());
        return fsInfo.isFile(file)
                && (n.endsWith(".jar") || n.endsWith(".zip"));
    }

    static Path normalize(Path p) {
        try {
            return p.toRealPath();
        } catch (IOException e) {
            return p.toAbsolutePath().normalize();
        }
    }
}
