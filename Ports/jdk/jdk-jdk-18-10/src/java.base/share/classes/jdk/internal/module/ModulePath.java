/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.module;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.module.FindException;
import java.lang.module.InvalidModuleDescriptorException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Builder;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import sun.nio.cs.UTF_8;

import jdk.internal.jmod.JmodFile;
import jdk.internal.jmod.JmodFile.Section;
import jdk.internal.perf.PerfCounter;

/**
 * A {@code ModuleFinder} that locates modules on the file system by searching
 * a sequence of directories or packaged modules. The ModuleFinder can be
 * created to work in either the run-time or link-time phases. In both cases it
 * locates modular JAR and exploded modules. When created for link-time then it
 * additionally locates modules in JMOD files. The ModuleFinder can also
 * optionally patch any modules that it locates with a ModulePatcher.
 */

public class ModulePath implements ModuleFinder {
    private static final String MODULE_INFO = "module-info.class";

    // the version to use for multi-release modular JARs
    private final Runtime.Version releaseVersion;

    // true for the link phase (supports modules packaged in JMOD format)
    private final boolean isLinkPhase;

    // for patching modules, can be null
    private final ModulePatcher patcher;

    // the entries on this module path
    private final Path[] entries;
    private int next;

    // map of module name to module reference map for modules already located
    private final Map<String, ModuleReference> cachedModules = new HashMap<>();


    private ModulePath(Runtime.Version version,
                       boolean isLinkPhase,
                       ModulePatcher patcher,
                       Path... entries) {
        this.releaseVersion = version;
        this.isLinkPhase = isLinkPhase;
        this.patcher = patcher;
        this.entries = entries.clone();
        for (Path entry : this.entries) {
            Objects.requireNonNull(entry);
        }
    }

    /**
     * Returns a ModuleFinder that locates modules on the file system by
     * searching a sequence of directories and/or packaged modules. The modules
     * may be patched by the given ModulePatcher.
     */
    public static ModuleFinder of(ModulePatcher patcher, Path... entries) {
        return new ModulePath(JarFile.runtimeVersion(), false, patcher, entries);
    }

    /**
     * Returns a ModuleFinder that locates modules on the file system by
     * searching a sequence of directories and/or packaged modules.
     */
    public static ModuleFinder of(Path... entries) {
        return of((ModulePatcher)null, entries);
    }

    /**
     * Returns a ModuleFinder that locates modules on the file system by
     * searching a sequence of directories and/or packaged modules.
     *
     * @param version The release version to use for multi-release JAR files
     * @param isLinkPhase {@code true} if the link phase to locate JMOD files
     */
    public static ModuleFinder of(Runtime.Version version,
                                  boolean isLinkPhase,
                                  Path... entries) {
        return new ModulePath(version, isLinkPhase, null, entries);
    }


    @Override
    public Optional<ModuleReference> find(String name) {
        Objects.requireNonNull(name);

        // try cached modules
        ModuleReference m = cachedModules.get(name);
        if (m != null)
            return Optional.of(m);

        // the module may not have been encountered yet
        while (hasNextEntry()) {
            scanNextEntry();
            m = cachedModules.get(name);
            if (m != null)
                return Optional.of(m);
        }
        return Optional.empty();
    }

    @Override
    public Set<ModuleReference> findAll() {
        // need to ensure that all entries have been scanned
        while (hasNextEntry()) {
            scanNextEntry();
        }
        return cachedModules.values().stream().collect(Collectors.toSet());
    }

    /**
     * Returns {@code true} if there are additional entries to scan
     */
    private boolean hasNextEntry() {
        return next < entries.length;
    }

    /**
     * Scans the next entry on the module path. A no-op if all entries have
     * already been scanned.
     *
     * @throws FindException if an error occurs scanning the next entry
     */
    private void scanNextEntry() {
        if (hasNextEntry()) {

            long t0 = System.nanoTime();

            Path entry = entries[next];
            Map<String, ModuleReference> modules = scan(entry);
            next++;

            // update cache, ignoring duplicates
            int initialSize = cachedModules.size();
            for (Map.Entry<String, ModuleReference> e : modules.entrySet()) {
                cachedModules.putIfAbsent(e.getKey(), e.getValue());
            }

            // update counters
            int added = cachedModules.size() - initialSize;
            moduleCount.add(added);

            scanTime.addElapsedTimeFrom(t0);
        }
    }


    /**
     * Scan the given module path entry. If the entry is a directory then it is
     * a directory of modules or an exploded module. If the entry is a regular
     * file then it is assumed to be a packaged module.
     *
     * @throws FindException if an error occurs scanning the entry
     */
    private Map<String, ModuleReference> scan(Path entry) {

        BasicFileAttributes attrs;
        try {
            attrs = Files.readAttributes(entry, BasicFileAttributes.class);
        } catch (NoSuchFileException e) {
            return Map.of();
        } catch (IOException ioe) {
            throw new FindException(ioe);
        }

        try {

            if (attrs.isDirectory()) {
                Path mi = entry.resolve(MODULE_INFO);
                if (!Files.exists(mi)) {
                    // assume a directory of modules
                    return scanDirectory(entry);
                }
            }

            // packaged or exploded module
            ModuleReference mref = readModule(entry, attrs);
            if (mref != null) {
                String name = mref.descriptor().name();
                return Map.of(name, mref);
            }

            // not recognized
            String msg;
            if (!isLinkPhase && entry.toString().endsWith(".jmod")) {
                msg = "JMOD format not supported at execution time";
            } else {
                msg = "Module format not recognized";
            }
            throw new FindException(msg + ": " + entry);

        } catch (IOException ioe) {
            throw new FindException(ioe);
        }
    }


    /**
     * Scans the given directory for packaged or exploded modules.
     *
     * @return a map of module name to ModuleReference for the modules found
     *         in the directory
     *
     * @throws IOException if an I/O error occurs
     * @throws FindException if an error occurs scanning the entry or the
     *         directory contains two or more modules with the same name
     */
    private Map<String, ModuleReference> scanDirectory(Path dir)
        throws IOException
    {
        // The map of name -> mref of modules found in this directory.
        Map<String, ModuleReference> nameToReference = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                BasicFileAttributes attrs;
                try {
                    attrs = Files.readAttributes(entry, BasicFileAttributes.class);
                } catch (NoSuchFileException ignore) {
                    // file has been removed or moved, ignore for now
                    continue;
                }

                ModuleReference mref = readModule(entry, attrs);

                // module found
                if (mref != null) {
                    // can have at most one version of a module in the directory
                    String name = mref.descriptor().name();
                    ModuleReference previous = nameToReference.put(name, mref);
                    if (previous != null) {
                        String fn1 = fileName(mref);
                        String fn2 = fileName(previous);
                        throw new FindException("Two versions of module "
                                                 + name + " found in " + dir
                                                 + " (" + fn1 + " and " + fn2 + ")");
                    }
                }
            }
        }

        return nameToReference;
    }


    /**
     * Reads a packaged or exploded module, returning a {@code ModuleReference}
     * to the module. Returns {@code null} if the entry is not recognized.
     *
     * @throws IOException if an I/O error occurs
     * @throws FindException if an error occurs parsing its module descriptor
     */
    private ModuleReference readModule(Path entry, BasicFileAttributes attrs)
        throws IOException
    {
        try {

            // exploded module
            if (attrs.isDirectory()) {
                return readExplodedModule(entry); // may return null
            }

            // JAR or JMOD file
            if (attrs.isRegularFile()) {
                String fn = entry.getFileName().toString();
                boolean isDefaultFileSystem = isDefaultFileSystem(entry);

                // JAR file
                if (fn.endsWith(".jar")) {
                    if (isDefaultFileSystem) {
                        return readJar(entry);
                    } else {
                        // the JAR file is in a custom file system so
                        // need to copy it to the local file system
                        Path tmpdir = Files.createTempDirectory("mlib");
                        Path target = Files.copy(entry, tmpdir.resolve(fn));
                        return readJar(target);
                    }
                }

                // JMOD file
                if (isDefaultFileSystem && isLinkPhase && fn.endsWith(".jmod")) {
                    return readJMod(entry);
                }
            }

            return null;

        } catch (InvalidModuleDescriptorException e) {
            throw new FindException("Error reading module: " + entry, e);
        }
    }

    /**
     * Returns a string with the file name of the module if possible.
     * If the module location is not a file URI then return the URI
     * as a string.
     */
    private String fileName(ModuleReference mref) {
        URI uri = mref.location().orElse(null);
        if (uri != null) {
            if (uri.getScheme().equalsIgnoreCase("file")) {
                Path file = Path.of(uri);
                return file.getFileName().toString();
            } else {
                return uri.toString();
            }
        } else {
            return "<unknown>";
        }
    }

    // -- JMOD files --

    private Set<String> jmodPackages(JmodFile jf) {
        return jf.stream()
            .filter(e -> e.section() == Section.CLASSES)
            .map(JmodFile.Entry::name)
            .map(this::toPackageName)
            .flatMap(Optional::stream)
            .collect(Collectors.toSet());
    }

    /**
     * Returns a {@code ModuleReference} to a module in JMOD file on the
     * file system.
     *
     * @throws IOException
     * @throws InvalidModuleDescriptorException
     */
    private ModuleReference readJMod(Path file) throws IOException {
        try (JmodFile jf = new JmodFile(file)) {
            ModuleInfo.Attributes attrs;
            try (InputStream in = jf.getInputStream(Section.CLASSES, MODULE_INFO)) {
                attrs  = ModuleInfo.read(in, () -> jmodPackages(jf));
            }
            return ModuleReferences.newJModModule(attrs, file);
        }
    }


    // -- JAR files --

    private static final String SERVICES_PREFIX = "META-INF/services/";

    private static final Attributes.Name AUTOMATIC_MODULE_NAME
        = new Attributes.Name("Automatic-Module-Name");

    /**
     * Returns the service type corresponding to the name of a services
     * configuration file if it is a legal type name.
     *
     * For example, if called with "META-INF/services/p.S" then this method
     * returns a container with the value "p.S".
     */
    private Optional<String> toServiceName(String cf) {
        assert cf.startsWith(SERVICES_PREFIX);
        int index = cf.lastIndexOf("/") + 1;
        if (index < cf.length()) {
            String prefix = cf.substring(0, index);
            if (prefix.equals(SERVICES_PREFIX)) {
                String sn = cf.substring(index);
                if (Checks.isClassName(sn))
                    return Optional.of(sn);
            }
        }
        return Optional.empty();
    }

    /**
     * Reads the next line from the given reader and trims it of comments and
     * leading/trailing white space.
     *
     * Returns null if the reader is at EOF.
     */
    private String nextLine(BufferedReader reader) throws IOException {
        String ln = reader.readLine();
        if (ln != null) {
            int ci = ln.indexOf('#');
            if (ci >= 0)
                ln = ln.substring(0, ci);
            ln = ln.trim();
        }
        return ln;
    }

    /**
     * Treat the given JAR file as a module as follows:
     *
     * 1. The value of the Automatic-Module-Name attribute is the module name
     * 2. The version, and the module name when the  Automatic-Module-Name
     *    attribute is not present, is derived from the file ame of the JAR file
     * 3. All packages are derived from the .class files in the JAR file
     * 4. The contents of any META-INF/services configuration files are mapped
     *    to "provides" declarations
     * 5. The Main-Class attribute in the main attributes of the JAR manifest
     *    is mapped to the module descriptor mainClass if possible
     */
    private ModuleDescriptor deriveModuleDescriptor(JarFile jf)
        throws IOException
    {
        // Read Automatic-Module-Name attribute if present
        Manifest man = jf.getManifest();
        Attributes attrs = null;
        String moduleName = null;
        if (man != null) {
            attrs = man.getMainAttributes();
            if (attrs != null) {
                moduleName = attrs.getValue(AUTOMATIC_MODULE_NAME);
            }
        }

        // Derive the version, and the module name if needed, from JAR file name
        String fn = jf.getName();
        int i = fn.lastIndexOf(File.separator);
        if (i != -1)
            fn = fn.substring(i + 1);

        // drop ".jar"
        String name = fn.substring(0, fn.length() - 4);
        String vs = null;

        // find first occurrence of -${NUMBER}. or -${NUMBER}$
        Matcher matcher = Patterns.DASH_VERSION.matcher(name);
        if (matcher.find()) {
            int start = matcher.start();

            // attempt to parse the tail as a version string
            try {
                String tail = name.substring(start + 1);
                ModuleDescriptor.Version.parse(tail);
                vs = tail;
            } catch (IllegalArgumentException ignore) { }

            name = name.substring(0, start);
        }

        // Create builder, using the name derived from file name when
        // Automatic-Module-Name not present
        Builder builder;
        if (moduleName != null) {
            try {
                builder = ModuleDescriptor.newAutomaticModule(moduleName);
            } catch (IllegalArgumentException e) {
                throw new FindException(AUTOMATIC_MODULE_NAME + ": " + e.getMessage());
            }
        } else {
            builder = ModuleDescriptor.newAutomaticModule(cleanModuleName(name));
        }

        // module version if present
        if (vs != null)
            builder.version(vs);

        // scan the names of the entries in the JAR file
        Map<Boolean, Set<String>> map = jf.versionedStream()
                .filter(e -> !e.isDirectory())
                .map(JarEntry::getName)
                .filter(e -> (e.endsWith(".class") ^ e.startsWith(SERVICES_PREFIX)))
                .collect(Collectors.partitioningBy(e -> e.startsWith(SERVICES_PREFIX),
                                                   Collectors.toSet()));

        Set<String> classFiles = map.get(Boolean.FALSE);
        Set<String> configFiles = map.get(Boolean.TRUE);

        // the packages containing class files
        Set<String> packages = classFiles.stream()
                .map(this::toPackageName)
                .flatMap(Optional::stream)
                .distinct()
                .collect(Collectors.toSet());

        // all packages are exported and open
        builder.packages(packages);

        // map names of service configuration files to service names
        Set<String> serviceNames = configFiles.stream()
                .map(this::toServiceName)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());

        // parse each service configuration file
        for (String sn : serviceNames) {
            JarEntry entry = jf.getJarEntry(SERVICES_PREFIX + sn);
            List<String> providerClasses = new ArrayList<>();
            try (InputStream in = jf.getInputStream(entry)) {
                BufferedReader reader
                    = new BufferedReader(new InputStreamReader(in, UTF_8.INSTANCE));
                String cn;
                while ((cn = nextLine(reader)) != null) {
                    if (!cn.isEmpty()) {
                        String pn = packageName(cn);
                        if (!packages.contains(pn)) {
                            String msg = "Provider class " + cn + " not in module";
                            throw new InvalidModuleDescriptorException(msg);
                        }
                        providerClasses.add(cn);
                    }
                }
            }
            if (!providerClasses.isEmpty())
                builder.provides(sn, providerClasses);
        }

        // Main-Class attribute if it exists
        if (attrs != null) {
            String mainClass = attrs.getValue(Attributes.Name.MAIN_CLASS);
            if (mainClass != null) {
                mainClass = mainClass.replace('/', '.');
                if (Checks.isClassName(mainClass)) {
                    String pn = packageName(mainClass);
                    if (packages.contains(pn)) {
                        builder.mainClass(mainClass);
                    }
                }
            }
        }

        return builder.build();
    }

    /**
     * Patterns used to derive the module name from a JAR file name.
     */
    private static class Patterns {
        static final Pattern DASH_VERSION = Pattern.compile("-(\\d+(\\.|$))");
        static final Pattern NON_ALPHANUM = Pattern.compile("[^A-Za-z0-9]");
        static final Pattern REPEATING_DOTS = Pattern.compile("(\\.)(\\1)+");
        static final Pattern LEADING_DOTS = Pattern.compile("^\\.");
        static final Pattern TRAILING_DOTS = Pattern.compile("\\.$");
    }

    /**
     * Clean up candidate module name derived from a JAR file name.
     */
    private static String cleanModuleName(String mn) {
        // replace non-alphanumeric
        mn = Patterns.NON_ALPHANUM.matcher(mn).replaceAll(".");

        // collapse repeating dots
        mn = Patterns.REPEATING_DOTS.matcher(mn).replaceAll(".");

        // drop leading dots
        if (!mn.isEmpty() && mn.charAt(0) == '.')
            mn = Patterns.LEADING_DOTS.matcher(mn).replaceAll("");

        // drop trailing dots
        int len = mn.length();
        if (len > 0 && mn.charAt(len-1) == '.')
            mn = Patterns.TRAILING_DOTS.matcher(mn).replaceAll("");

        return mn;
    }

    private Set<String> jarPackages(JarFile jf) {
        return jf.versionedStream()
                .filter(e -> !e.isDirectory())
                .map(JarEntry::getName)
                .map(this::toPackageName)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());
    }

    /**
     * Returns a {@code ModuleReference} to a module in modular JAR file on
     * the file system.
     *
     * @throws IOException
     * @throws FindException
     * @throws InvalidModuleDescriptorException
     */
    private ModuleReference readJar(Path file) throws IOException {
        try (JarFile jf = new JarFile(file.toFile(),
                                      true,               // verify
                                      ZipFile.OPEN_READ,
                                      releaseVersion))
        {
            ModuleInfo.Attributes attrs;
            JarEntry entry = jf.getJarEntry(MODULE_INFO);
            if (entry == null) {

                // no module-info.class so treat it as automatic module
                try {
                    ModuleDescriptor md = deriveModuleDescriptor(jf);
                    attrs = new ModuleInfo.Attributes(md, null, null, null);
                } catch (RuntimeException e) {
                    throw new FindException("Unable to derive module descriptor for "
                                            + jf.getName(), e);
                }

            } else {
                attrs = ModuleInfo.read(jf.getInputStream(entry),
                                        () -> jarPackages(jf));
            }

            return ModuleReferences.newJarModule(attrs, patcher, file);
        } catch (ZipException e) {
            throw new FindException("Error reading " + file, e);
        }
    }


    // -- exploded directories --

    private Set<String> explodedPackages(Path dir) {
        try {
            return Files.find(dir, Integer.MAX_VALUE,
                    ((path, attrs) -> attrs.isRegularFile() && !isHidden(path)))
                    .map(path -> dir.relativize(path))
                    .map(this::toPackageName)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toSet());
        } catch (IOException x) {
            throw new UncheckedIOException(x);
        }
    }

    /**
     * Returns a {@code ModuleReference} to an exploded module on the file
     * system or {@code null} if {@code module-info.class} not found.
     *
     * @throws IOException
     * @throws InvalidModuleDescriptorException
     */
    private ModuleReference readExplodedModule(Path dir) throws IOException {
        Path mi = dir.resolve(MODULE_INFO);
        ModuleInfo.Attributes attrs;
        try (InputStream in = Files.newInputStream(mi)) {
            attrs = ModuleInfo.read(new BufferedInputStream(in),
                                    () -> explodedPackages(dir));
        } catch (NoSuchFileException e) {
            // for now
            return null;
        }
        return ModuleReferences.newExplodedModule(attrs, patcher, dir);
    }

    /**
     * Maps a type name to its package name.
     */
    private static String packageName(String cn) {
        int index = cn.lastIndexOf('.');
        return (index == -1) ? "" : cn.substring(0, index);
    }

    /**
     * Maps the name of an entry in a JAR or ZIP file to a package name.
     *
     * @throws InvalidModuleDescriptorException if the name is a class file in
     *         the top-level directory of the JAR/ZIP file (and it's not
     *         module-info.class)
     */
    private Optional<String> toPackageName(String name) {
        assert !name.endsWith("/");
        int index = name.lastIndexOf("/");
        if (index == -1) {
            if (name.endsWith(".class") && !name.equals(MODULE_INFO)) {
                String msg = name + " found in top-level directory"
                             + " (unnamed package not allowed in module)";
                throw new InvalidModuleDescriptorException(msg);
            }
            return Optional.empty();
        }

        String pn = name.substring(0, index).replace('/', '.');
        if (Checks.isPackageName(pn)) {
            return Optional.of(pn);
        } else {
            // not a valid package name
            return Optional.empty();
        }
    }

    /**
     * Maps the relative path of an entry in an exploded module to a package
     * name.
     *
     * @throws InvalidModuleDescriptorException if the name is a class file in
     *         the top-level directory (and it's not module-info.class)
     */
    private Optional<String> toPackageName(Path file) {
        assert file.getRoot() == null;

        Path parent = file.getParent();
        if (parent == null) {
            String name = file.toString();
            if (name.endsWith(".class") && !name.equals(MODULE_INFO)) {
                String msg = name + " found in top-level directory"
                             + " (unnamed package not allowed in module)";
                throw new InvalidModuleDescriptorException(msg);
            }
            return Optional.empty();
        }

        String pn = parent.toString().replace(File.separatorChar, '.');
        if (Checks.isPackageName(pn)) {
            return Optional.of(pn);
        } else {
            // not a valid package name
            return Optional.empty();
        }
    }

    /**
     * Returns true if the given file exists and is a hidden file
     */
    private boolean isHidden(Path file) {
        try {
            return Files.isHidden(file);
        } catch (IOException ioe) {
            return false;
        }
    }


    /**
     * Return true if a path locates a path in the default file system
     */
    private boolean isDefaultFileSystem(Path path) {
        return path.getFileSystem().provider()
                .getScheme().equalsIgnoreCase("file");
    }


    private static final PerfCounter scanTime
        = PerfCounter.newPerfCounter("jdk.module.finder.modulepath.scanTime");
    private static final PerfCounter moduleCount
        = PerfCounter.newPerfCounter("jdk.module.finder.modulepath.modules");
}
