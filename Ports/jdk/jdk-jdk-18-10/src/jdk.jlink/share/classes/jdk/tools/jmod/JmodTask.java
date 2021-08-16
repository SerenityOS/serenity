/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.tools.jmod;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.lang.module.Configuration;
import java.lang.module.FindException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Exports;
import java.lang.module.ModuleDescriptor.Opens;
import java.lang.module.ModuleDescriptor.Provides;
import java.lang.module.ModuleDescriptor.Version;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.lang.module.ResolutionException;
import java.lang.module.ResolvedModule;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.MessageFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import jdk.internal.jmod.JmodFile;
import jdk.internal.jmod.JmodFile.Section;
import jdk.internal.joptsimple.BuiltinHelpFormatter;
import jdk.internal.joptsimple.NonOptionArgumentSpec;
import jdk.internal.joptsimple.OptionDescriptor;
import jdk.internal.joptsimple.OptionException;
import jdk.internal.joptsimple.OptionParser;
import jdk.internal.joptsimple.OptionSet;
import jdk.internal.joptsimple.OptionSpec;
import jdk.internal.joptsimple.ValueConverter;
import jdk.internal.module.ModuleHashes;
import jdk.internal.module.ModuleHashesBuilder;
import jdk.internal.module.ModuleInfo;
import jdk.internal.module.ModuleInfoExtender;
import jdk.internal.module.ModulePath;
import jdk.internal.module.ModuleResolution;
import jdk.internal.module.ModuleTarget;
import jdk.internal.module.Resources;
import jdk.tools.jlink.internal.Utils;

import static java.util.stream.Collectors.joining;

/**
 * Implementation for the jmod tool.
 */
public class JmodTask {

    static class CommandException extends RuntimeException {
        private static final long serialVersionUID = 0L;
        boolean showUsage;

        CommandException(String key, Object... args) {
            super(getMessageOrKey(key, args));
        }

        CommandException showUsage(boolean b) {
            showUsage = b;
            return this;
        }

        private static String getMessageOrKey(String key, Object... args) {
            try {
                return MessageFormat.format(ResourceBundleHelper.bundle.getString(key), args);
            } catch (MissingResourceException e) {
                return key;
            }
        }
    }

    private static final String PROGNAME = "jmod";
    private static final String MODULE_INFO = "module-info.class";

    private static final Path CWD = Paths.get("");

    private Options options;
    private PrintWriter out = new PrintWriter(System.out, true);
    void setLog(PrintWriter out, PrintWriter err) {
        this.out = out;
    }

    /* Result codes. */
    static final int EXIT_OK = 0, // Completed with no errors.
                     EXIT_ERROR = 1, // Completed but reported errors.
                     EXIT_CMDERR = 2, // Bad command-line arguments
                     EXIT_SYSERR = 3, // System error or resource exhaustion.
                     EXIT_ABNORMAL = 4;// terminated abnormally

    enum Mode {
        CREATE,
        EXTRACT,
        LIST,
        DESCRIBE,
        HASH
    };

    static class Options {
        Mode mode;
        Path jmodFile;
        boolean help;
        boolean helpExtra;
        boolean version;
        List<Path> classpath;
        List<Path> cmds;
        List<Path> configs;
        List<Path> libs;
        List<Path> headerFiles;
        List<Path> manPages;
        List<Path> legalNotices;;
        ModuleFinder moduleFinder;
        Version moduleVersion;
        String mainClass;
        String targetPlatform;
        Pattern modulesToHash;
        ModuleResolution moduleResolution;
        boolean dryrun;
        List<PathMatcher> excludes;
        Path extractDir;
    }

    public int run(String[] args) {

        try {
            handleOptions(args);
            if (options == null) {
                showUsageSummary();
                return EXIT_CMDERR;
            }
            if (options.help || options.helpExtra) {
                showHelp();
                return EXIT_OK;
            }
            if (options.version) {
                showVersion();
                return EXIT_OK;
            }

            boolean ok;
            switch (options.mode) {
                case CREATE:
                    ok = create();
                    break;
                case EXTRACT:
                    ok = extract();
                    break;
                case LIST:
                    ok = list();
                    break;
                case DESCRIBE:
                    ok = describe();
                    break;
                case HASH:
                    ok = hashModules();
                    break;
                default:
                    throw new AssertionError("Unknown mode: " + options.mode.name());
            }

            return ok ? EXIT_OK : EXIT_ERROR;
        } catch (CommandException e) {
            reportError(e.getMessage());
            if (e.showUsage)
                showUsageSummary();
            return EXIT_CMDERR;
        } catch (Exception x) {
            reportError(x.getMessage());
            x.printStackTrace();
            return EXIT_ABNORMAL;
        } finally {
            out.flush();
        }
    }

    private boolean list() throws IOException {
        ZipFile zip = null;
        try {
            try {
                zip = new ZipFile(options.jmodFile.toFile());
            } catch (IOException x) {
                throw new IOException("error opening jmod file", x);
            }

            // Trivially print the archive entries for now, pending a more complete implementation
            zip.stream().forEach(e -> out.println(e.getName()));
            return true;
        } finally {
            if (zip != null)
                zip.close();
        }
    }

    private boolean extract() throws IOException {
        Path dir = options.extractDir != null ? options.extractDir : CWD;
        try (JmodFile jf = new JmodFile(options.jmodFile)) {
            jf.stream().forEach(e -> {
                try {
                    ZipEntry entry = e.zipEntry();
                    String name = entry.getName();
                    int index = name.lastIndexOf("/");
                    if (index != -1) {
                        Path p = dir.resolve(name.substring(0, index));
                        if (Files.notExists(p))
                            Files.createDirectories(p);
                    }

                    try (OutputStream os = Files.newOutputStream(dir.resolve(name))) {
                        jf.getInputStream(e).transferTo(os);
                    }
                } catch (IOException x) {
                    throw new UncheckedIOException(x);
                }
            });

            return true;
        }
    }

    private boolean hashModules() throws IOException {
        String moduleName = null;
        if (options.jmodFile != null) {
            try (JmodFile jf = new JmodFile(options.jmodFile)) {
                try (InputStream in = jf.getInputStream(Section.CLASSES, MODULE_INFO)) {
                    ModuleInfo.Attributes attrs = ModuleInfo.read(in, null);
                    moduleName = attrs.descriptor().name();
                } catch (IOException e) {
                    throw new CommandException("err.module.descriptor.not.found");
                }
            }
        }
        Hasher hasher = new Hasher(moduleName, options.moduleFinder);

        if (options.dryrun) {
            out.println("Dry run:");
        }

        Map<String, ModuleHashes> moduleHashes = hasher.computeHashes();
        if (moduleHashes.isEmpty()) {
            throw new CommandException("err.no.moduleToHash", "\"" + options.modulesToHash + "\"");
        }
        moduleHashes.forEach((mn, hashes) -> {
            if (options.dryrun) {
                out.format("%s%n", mn);
                hashes.names().stream()
                      .sorted()
                      .forEach(name -> out.format("  hashes %s %s %s%n",
                            name, hashes.algorithm(), toHex(hashes.hashFor(name))));
            } else {
                try {
                    hasher.updateModuleInfo(mn, hashes);
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            }
        });
        return true;
    }

    private boolean describe() throws IOException {
        try (JmodFile jf = new JmodFile(options.jmodFile)) {
            try (InputStream in = jf.getInputStream(Section.CLASSES, MODULE_INFO)) {
                ModuleInfo.Attributes attrs = ModuleInfo.read(in, null);
                describeModule(attrs.descriptor(),
                               attrs.target(),
                               attrs.recordedHashes());
                return true;
            } catch (IOException e) {
                throw new CommandException("err.module.descriptor.not.found");
            }
        }
    }

    static <T> String toLowerCaseString(Collection<T> c) {
        if (c.isEmpty()) { return ""; }
        return " " + c.stream().map(e -> e.toString().toLowerCase(Locale.ROOT))
                .sorted().collect(joining(" "));
    }

    static <T> String toString(Collection<T> c) {
        if (c.isEmpty()) { return ""; }
        return " " + c.stream().map(e -> e.toString()).sorted().collect(joining(" "));
    }

    private void describeModule(ModuleDescriptor md,
                                ModuleTarget target,
                                ModuleHashes hashes)
        throws IOException
    {
        StringBuilder sb = new StringBuilder();

        sb.append(md.toNameAndVersion());

        if (md.isOpen())
            sb.append(" open");
        if (md.isAutomatic())
            sb.append(" automatic");
        sb.append("\n");

        // unqualified exports (sorted by package)
        md.exports().stream()
                .sorted(Comparator.comparing(Exports::source))
                .filter(e -> !e.isQualified())
                .forEach(e -> sb.append("exports ").append(e.source())
                                .append(toLowerCaseString(e.modifiers())).append("\n"));

        // dependences
        md.requires().stream().sorted()
                .forEach(r -> sb.append("requires ").append(r.name())
                                .append(toLowerCaseString(r.modifiers())).append("\n"));

        // service use and provides
        md.uses().stream().sorted()
                .forEach(s -> sb.append("uses ").append(s).append("\n"));

        md.provides().stream()
                .sorted(Comparator.comparing(Provides::service))
                .forEach(p -> sb.append("provides ").append(p.service())
                                .append(" with")
                                .append(toString(p.providers()))
                                .append("\n"));

        // qualified exports
        md.exports().stream()
                .sorted(Comparator.comparing(Exports::source))
                .filter(Exports::isQualified)
                .forEach(e -> sb.append("qualified exports ").append(e.source())
                                .append(" to").append(toLowerCaseString(e.targets()))
                                .append("\n"));

        // open packages
        md.opens().stream()
                .sorted(Comparator.comparing(Opens::source))
                .filter(o -> !o.isQualified())
                .forEach(o -> sb.append("opens ").append(o.source())
                                 .append(toLowerCaseString(o.modifiers()))
                                 .append("\n"));

        md.opens().stream()
                .sorted(Comparator.comparing(Opens::source))
                .filter(Opens::isQualified)
                .forEach(o -> sb.append("qualified opens ").append(o.source())
                                 .append(toLowerCaseString(o.modifiers()))
                                 .append(" to").append(toLowerCaseString(o.targets()))
                                 .append("\n"));

        // non-exported/non-open packages
        Set<String> concealed = new TreeSet<>(md.packages());
        md.exports().stream().map(Exports::source).forEach(concealed::remove);
        md.opens().stream().map(Opens::source).forEach(concealed::remove);
        concealed.forEach(p -> sb.append("contains ").append(p).append("\n"));

        md.mainClass().ifPresent(v -> sb.append("main-class ").append(v).append("\n"));

        if (target != null) {
            String targetPlatform = target.targetPlatform();
            if (!targetPlatform.isEmpty())
                sb.append("platform ").append(targetPlatform).append("\n");
       }

       if (hashes != null) {
           hashes.names().stream().sorted().forEach(
                   mod -> sb.append("hashes ").append(mod).append(" ")
                            .append(hashes.algorithm()).append(" ")
                            .append(toHex(hashes.hashFor(mod)))
                            .append("\n"));
        }

        out.println(sb.toString());
    }

    private String toHex(byte[] ba) {
        StringBuilder sb = new StringBuilder(ba.length);
        for (byte b: ba) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    private boolean create() throws IOException {
        JmodFileWriter jmod = new JmodFileWriter();

        // create jmod with temporary name to avoid it being examined
        // when scanning the module path
        Path target = options.jmodFile;
        Path tempTarget = jmodTempFilePath(target);
        try {
            try (JmodOutputStream jos = JmodOutputStream.newOutputStream(tempTarget)) {
                jmod.write(jos);
            }
            Files.move(tempTarget, target);
        } catch (Exception e) {
            try {
                Files.deleteIfExists(tempTarget);
            } catch (IOException ioe) {
                e.addSuppressed(ioe);
            }
            throw e;
        }
        return true;
    }

    /*
     * Create a JMOD .tmp file for the given target JMOD file
     */
    private static Path jmodTempFilePath(Path target) throws IOException {
        return target.resolveSibling("." + target.getFileName() + ".tmp");
    }

    private class JmodFileWriter {
        final List<Path> cmds = options.cmds;
        final List<Path> libs = options.libs;
        final List<Path> configs = options.configs;
        final List<Path> classpath = options.classpath;
        final List<Path> headerFiles = options.headerFiles;
        final List<Path> manPages = options.manPages;
        final List<Path> legalNotices = options.legalNotices;

        final Version moduleVersion = options.moduleVersion;
        final String mainClass = options.mainClass;
        final String targetPlatform = options.targetPlatform;
        final List<PathMatcher> excludes = options.excludes;
        final ModuleResolution moduleResolution = options.moduleResolution;

        JmodFileWriter() { }

        /**
         * Writes the jmod to the given output stream.
         */
        void write(JmodOutputStream out) throws IOException {
            // module-info.class
            writeModuleInfo(out, findPackages(classpath));

            // classes
            processClasses(out, classpath);

            processSection(out, Section.CONFIG, configs);
            processSection(out, Section.HEADER_FILES, headerFiles);
            processSection(out, Section.LEGAL_NOTICES, legalNotices);
            processSection(out, Section.MAN_PAGES, manPages);
            processSection(out, Section.NATIVE_CMDS, cmds);
            processSection(out, Section.NATIVE_LIBS, libs);

        }

        /**
         * Returns a supplier of an input stream to the module-info.class
         * on the class path of directories and JAR files.
         */
        Supplier<InputStream> newModuleInfoSupplier() throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for (Path e: classpath) {
                if (Files.isDirectory(e)) {
                    Path mi = e.resolve(MODULE_INFO);
                    if (Files.isRegularFile(mi)) {
                        Files.copy(mi, baos);
                        break;
                    }
                } else if (Files.isRegularFile(e) && e.toString().endsWith(".jar")) {
                    try (JarFile jf = new JarFile(e.toFile())) {
                        ZipEntry entry = jf.getEntry(MODULE_INFO);
                        if (entry != null) {
                            jf.getInputStream(entry).transferTo(baos);
                            break;
                        }
                    } catch (ZipException x) {
                        // Skip. Do nothing. No packages will be added.
                    }
                }
            }
            if (baos.size() == 0) {
                return null;
            } else {
                byte[] bytes = baos.toByteArray();
                return () -> new ByteArrayInputStream(bytes);
            }
        }

        /**
         * Writes the updated module-info.class to the ZIP output stream.
         *
         * The updated module-info.class will have a Packages attribute
         * with the set of module-private/non-exported packages.
         *
         * If --module-version, --main-class, or other options were provided
         * then the corresponding class file attributes are added to the
         * module-info here.
         */
        void writeModuleInfo(JmodOutputStream out, Set<String> packages)
            throws IOException
        {
            Supplier<InputStream> miSupplier = newModuleInfoSupplier();
            if (miSupplier == null) {
                throw new IOException(MODULE_INFO + " not found");
            }

            ModuleDescriptor descriptor;
            try (InputStream in = miSupplier.get()) {
                descriptor = ModuleDescriptor.read(in);
            }

            // copy the module-info.class into the jmod with the additional
            // attributes for the version, main class and other meta data
            try (InputStream in = miSupplier.get()) {
                ModuleInfoExtender extender = ModuleInfoExtender.newExtender(in);

                // Add (or replace) the Packages attribute
                if (packages != null) {
                    validatePackages(descriptor, packages);
                    extender.packages(packages);
                }

                // --main-class
                if (mainClass != null)
                    extender.mainClass(mainClass);

                // --target-platform
                if (targetPlatform != null) {
                    extender.targetPlatform(targetPlatform);
                }

                // --module-version
                if (moduleVersion != null)
                    extender.version(moduleVersion);

                // --hash-modules
                if (options.modulesToHash != null) {
                    // To compute hashes, it creates a Configuration to resolve
                    // a module graph.  The post-resolution check requires
                    // the packages in ModuleDescriptor be available for validation.
                    ModuleDescriptor md;
                    try (InputStream is = miSupplier.get()) {
                        md = ModuleDescriptor.read(is, () -> packages);
                    }

                    ModuleHashes moduleHashes = computeHashes(md);
                    if (moduleHashes != null) {
                        extender.hashes(moduleHashes);
                    } else {
                        warning("warn.no.module.hashes", descriptor.name());
                    }
                }

                if (moduleResolution != null && moduleResolution.value() != 0) {
                    extender.moduleResolution(moduleResolution);
                }

                // write the (possibly extended or modified) module-info.class
                out.writeEntry(extender.toByteArray(), Section.CLASSES, MODULE_INFO);
            }
        }

        private void validatePackages(ModuleDescriptor descriptor, Set<String> packages) {
            Set<String> nonExistPackages = new TreeSet<>();
            descriptor.exports().stream()
                .map(Exports::source)
                .filter(pn -> !packages.contains(pn))
                .forEach(nonExistPackages::add);

            descriptor.opens().stream()
                .map(Opens::source)
                .filter(pn -> !packages.contains(pn))
                .forEach(nonExistPackages::add);

            if (!nonExistPackages.isEmpty()) {
                throw new CommandException("err.missing.export.or.open.packages",
                    descriptor.name(), nonExistPackages);
            }
        }

        /*
         * Hasher resolves a module graph using the --hash-modules PATTERN
         * as the roots.
         *
         * The jmod file is being created and does not exist in the
         * given modulepath.
         */
        private ModuleHashes computeHashes(ModuleDescriptor descriptor) {
            String mn = descriptor.name();
            URI uri = options.jmodFile.toUri();
            ModuleReference mref = new ModuleReference(descriptor, uri) {
                @Override
                public ModuleReader open() {
                    throw new UnsupportedOperationException("opening " + mn);
                }
            };

            // compose a module finder with the module path and also
            // a module finder that can find the jmod file being created
            ModuleFinder finder = ModuleFinder.compose(options.moduleFinder,
                new ModuleFinder() {
                    @Override
                    public Optional<ModuleReference> find(String name) {
                        if (descriptor.name().equals(name))
                            return Optional.of(mref);
                        else return Optional.empty();
                    }

                    @Override
                    public Set<ModuleReference> findAll() {
                        return Collections.singleton(mref);
                    }
                });

            return new Hasher(mn, finder).computeHashes().get(mn);
        }

        /**
         * Returns the set of all packages on the given class path.
         */
        Set<String> findPackages(List<Path> classpath) {
            Set<String> packages = new HashSet<>();
            for (Path path : classpath) {
                if (Files.isDirectory(path)) {
                    packages.addAll(findPackages(path));
                } else if (Files.isRegularFile(path) && path.toString().endsWith(".jar")) {
                    try (JarFile jf = new JarFile(path.toString())) {
                        packages.addAll(findPackages(jf));
                    } catch (ZipException x) {
                        // Skip. Do nothing. No packages will be added.
                    } catch (IOException ioe) {
                        throw new UncheckedIOException(ioe);
                    }
                }
            }
            return packages;
        }

        /**
         * Returns the set of packages in the given directory tree.
         */
        Set<String> findPackages(Path dir) {
            try {
                return Files.find(dir, Integer.MAX_VALUE,
                                  ((path, attrs) -> attrs.isRegularFile()),
                                  FileVisitOption.FOLLOW_LINKS)
                        .map(dir::relativize)
                        .filter(path -> isResource(path.toString()))
                        .map(path -> toPackageName(path))
                        .filter(pkg -> pkg.length() > 0)
                        .distinct()
                        .collect(Collectors.toSet());
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
        }

        /**
         * Returns the set of packages in the given JAR file.
         */
        Set<String> findPackages(JarFile jf) {
            return jf.stream()
                     .filter(e -> !e.isDirectory() && isResource(e.getName()))
                     .map(e -> toPackageName(e))
                     .filter(pkg -> pkg.length() > 0)
                     .distinct()
                     .collect(Collectors.toSet());
        }

        /**
         * Returns true if it's a .class or a resource with an effective
         * package name.
         */
        boolean isResource(String name) {
            name = name.replace(File.separatorChar, '/');
            return name.endsWith(".class") || Resources.canEncapsulate(name);
        }


        String toPackageName(Path path) {
            String name = path.toString();
            int index = name.lastIndexOf(File.separatorChar);
            if (index != -1)
                return name.substring(0, index).replace(File.separatorChar, '.');

            if (name.endsWith(".class") && !name.equals(MODULE_INFO)) {
                IOException e = new IOException(name  + " in the unnamed package");
                throw new UncheckedIOException(e);
            }
            return "";
        }

        String toPackageName(ZipEntry entry) {
            String name = entry.getName();
            int index = name.lastIndexOf("/");
            if (index != -1)
                return name.substring(0, index).replace('/', '.');

            if (name.endsWith(".class") && !name.equals(MODULE_INFO)) {
                IOException e = new IOException(name  + " in the unnamed package");
                throw new UncheckedIOException(e);
            }
            return "";
        }

        void processClasses(JmodOutputStream out, List<Path> classpaths)
            throws IOException
        {
            if (classpaths == null)
                return;

            for (Path p : classpaths) {
                if (Files.isDirectory(p)) {
                    processSection(out, Section.CLASSES, p);
                } else if (Files.isRegularFile(p) && p.toString().endsWith(".jar")) {
                    try (JarFile jf = new JarFile(p.toFile())) {
                        JarEntryConsumer jec = new JarEntryConsumer(out, jf);
                        jf.stream().filter(jec).forEach(jec);
                    }
                }
            }
        }

        void processSection(JmodOutputStream out, Section section, List<Path> paths)
            throws IOException
        {
            if (paths == null)
                return;

            for (Path p : paths) {
                processSection(out, section, p);
            }
        }

        void processSection(JmodOutputStream out, Section section, Path path)
            throws IOException
        {
            Files.walkFileTree(path, Set.of(FileVisitOption.FOLLOW_LINKS),
                Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException
                    {
                        Path relPath = path.relativize(file);
                        String name = relPath.toString();
                        if (name.equals(MODULE_INFO)) {
                            if (!Section.CLASSES.equals(section))
                                warning("warn.ignore.entry", name, section);
                        } else if (!matches(relPath, excludes)) {
                            if (out.contains(section, name)) {
                                warning("warn.ignore.duplicate.entry", name, section);
                            } else {
                                try (InputStream in = Files.newInputStream(file)) {
                                    out.writeEntry(in, section, name);
                                }
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
        }

        boolean matches(Path path, List<PathMatcher> matchers) {
            if (matchers != null) {
                for (PathMatcher pm : matchers) {
                    if (pm.matches(path))
                        return true;
                }
            }
            return false;
        }

        class JarEntryConsumer implements Consumer<JarEntry>, Predicate<JarEntry> {
            final JmodOutputStream out;
            final JarFile jarfile;
            JarEntryConsumer(JmodOutputStream out, JarFile jarfile) {
                this.out = out;
                this.jarfile = jarfile;
            }
            @Override
            public void accept(JarEntry je) {
                try (InputStream in = jarfile.getInputStream(je)) {
                    out.writeEntry(in, Section.CLASSES, je.getName());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            @Override
            public boolean test(JarEntry je) {
                String name = je.getName();
                // ## no support for excludes. Is it really needed?
                if (name.endsWith(MODULE_INFO) || je.isDirectory()) {
                    return false;
                }
                if (out.contains(Section.CLASSES, name)) {
                    warning("warn.ignore.duplicate.entry", name, Section.CLASSES);
                    return false;
                }
                return true;
            }
        }
    }

    /**
     * Compute and record hashes
     */
    private class Hasher {
        final Configuration configuration;
        final ModuleHashesBuilder hashesBuilder;
        final Set<String> modules;
        final String moduleName;  // a specific module to record hashes, if set

        /**
         * Constructs a Hasher to compute hashes.
         *
         * If a module name `M` is specified, it will compute the hashes of
         * modules that depend upon M directly or indirectly matching the
         * specified --hash-modules pattern and record in the ModuleHashes
         * attribute in M's module-info.class.
         *
         * If name is null, this Hasher will determine which modules to
         * record hashes, i.e. the module in a subgraph of modules to be
         * hashed and that has no outgoing edges.  It will record in each
         * of these modules, say `M`, with the hashes of modules that
         * depend upon M directly or indirectly matching the specified
         * --hash-modules pattern.
         *
         * @param name    name of the module to record hashes
         * @param finder  module finder for the specified --module-path
         */
        Hasher(String name, ModuleFinder finder) {
            // Determine the modules that matches the pattern {@code modulesToHash}
            Set<String> roots = finder.findAll().stream()
                .map(mref -> mref.descriptor().name())
                .filter(mn -> options.modulesToHash.matcher(mn).find())
                .collect(Collectors.toSet());

            // use system module path unless it creates a JMOD file for
            // a module that is present in the system image e.g. upgradeable
            // module
            ModuleFinder system;
            if (name != null && ModuleFinder.ofSystem().find(name).isPresent()) {
                system = ModuleFinder.of();
            } else {
                system = ModuleFinder.ofSystem();
            }
            // get a resolved module graph
            Configuration config = null;
            try {
                config = Configuration.empty().resolve(system, finder, roots);
            } catch (FindException | ResolutionException e) {
                throw new CommandException("err.module.resolution.fail", e.getMessage());
            }

            this.moduleName = name;
            this.configuration = config;

            // filter modules resolved from the system module finder
            this.modules = config.modules().stream()
                .map(ResolvedModule::name)
                .filter(mn -> roots.contains(mn) && !system.find(mn).isPresent())
                .collect(Collectors.toSet());

            this.hashesBuilder = new ModuleHashesBuilder(config, modules);
        }

        /**
         * Returns a map of a module M to record hashes of the modules
         * that depend upon M directly or indirectly.
         *
         * For jmod hash command, the returned map contains one entry
         * for each module M that has no outgoing edges to any of the
         * modules matching the specified --hash-modules pattern.
         *
         * Each entry represents a leaf node in a connected subgraph containing
         * M and other candidate modules from the module graph where M's outgoing
         * edges to any module other than the ones matching the specified
         * --hash-modules pattern are excluded.
         */
        Map<String, ModuleHashes> computeHashes() {
            if (hashesBuilder == null)
                return null;

            if (moduleName != null) {
                return hashesBuilder.computeHashes(Set.of(moduleName));
            } else {
                return hashesBuilder.computeHashes(modules);
            }
        }

        /**
         * Reads the given input stream of module-info.class and write
         * the extended module-info.class with the given ModuleHashes
         *
         * @param in       InputStream of module-info.class
         * @param out      OutputStream to write the extended module-info.class
         * @param hashes   ModuleHashes
         */
        private void recordHashes(InputStream in, OutputStream out, ModuleHashes hashes)
            throws IOException
        {
            ModuleInfoExtender extender = ModuleInfoExtender.newExtender(in);
            extender.hashes(hashes);
            extender.write(out);
        }

        void updateModuleInfo(String name, ModuleHashes moduleHashes)
            throws IOException
        {
            Path target = moduleToPath(name);
            Path tempTarget = jmodTempFilePath(target);
            try {
                if (target.getFileName().toString().endsWith(".jmod")) {
                    updateJmodFile(target, tempTarget, moduleHashes);
                } else {
                    updateModularJar(target, tempTarget, moduleHashes);
                }
            } catch (IOException|RuntimeException e) {
                try {
                    Files.deleteIfExists(tempTarget);
                } catch (IOException ioe) {
                    e.addSuppressed(ioe);
                }
                throw e;
            }

            out.println(getMessage("module.hashes.recorded", name));
            Files.move(tempTarget, target, StandardCopyOption.REPLACE_EXISTING);
        }

        private void updateModularJar(Path target, Path tempTarget,
                                      ModuleHashes moduleHashes)
            throws IOException
        {
            try (JarFile jf = new JarFile(target.toFile());
                 OutputStream out = Files.newOutputStream(tempTarget);
                 JarOutputStream jos = new JarOutputStream(out))
            {
                jf.stream().forEach(e -> {
                    try (InputStream in = jf.getInputStream(e)) {
                        if (e.getName().equals(MODULE_INFO)) {
                            // what about module-info.class in versioned entries?
                            ZipEntry ze = new ZipEntry(e.getName());
                            ze.setTime(System.currentTimeMillis());
                            jos.putNextEntry(ze);
                            recordHashes(in, jos, moduleHashes);
                            jos.closeEntry();
                        } else {
                            // Setting "compressedSize" to "-1" prevents an error
                            // in ZipOutputStream.closeEntry() if the newly
                            // deflated entry will have another size than the
                            // original compressed entry. See:
                            // ZipOutputStream.putNextEntry()/closeEntry()
                            e.setCompressedSize(-1);
                            jos.putNextEntry(e);
                            jos.write(in.readAllBytes());
                            jos.closeEntry();
                        }
                    } catch (IOException x) {
                        throw new UncheckedIOException(x);
                    }
                });
            }
        }

        private void updateJmodFile(Path target, Path tempTarget,
                                    ModuleHashes moduleHashes)
            throws IOException
        {

            try (JmodFile jf = new JmodFile(target);
                 JmodOutputStream jos = JmodOutputStream.newOutputStream(tempTarget))
            {
                jf.stream().forEach(e -> {
                    try (InputStream in = jf.getInputStream(e.section(), e.name())) {
                        if (e.name().equals(MODULE_INFO)) {
                            // replace module-info.class
                            ModuleInfoExtender extender =
                                ModuleInfoExtender.newExtender(in);
                            extender.hashes(moduleHashes);
                            jos.writeEntry(extender.toByteArray(), e.section(), e.name());
                        } else {
                            jos.writeEntry(in, e);
                        }
                    } catch (IOException x) {
                        throw new UncheckedIOException(x);
                    }
                });
            }
        }

        private Path moduleToPath(String name) {
            ResolvedModule rm = configuration.findModule(name).orElseThrow(
                () -> new InternalError("Selected module " + name + " not on module path"));

            URI uri = rm.reference().location().get();
            Path path = Paths.get(uri);
            String fn = path.getFileName().toString();
            if (!fn.endsWith(".jar") && !fn.endsWith(".jmod")) {
                throw new InternalError(path + " is not a modular JAR or jmod file");
            }
            return path;
        }
    }

    /**
     * An abstract converter that given a string representing a list of paths,
     * separated by the File.pathSeparator, returns a List of java.nio.Path's.
     * Specific subclasses should do whatever validation is required on the
     * individual path elements, if any.
     */
    static abstract class AbstractPathConverter implements ValueConverter<List<Path>> {
        @Override
        public List<Path> convert(String value) {
            List<Path> paths = new ArrayList<>();
            String[] pathElements = value.split(File.pathSeparator);
            for (String pathElement : pathElements) {
                paths.add(toPath(pathElement));
            }
            return paths;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Class<List<Path>> valueType() {
            return (Class<List<Path>>)(Object)List.class;
        }

        @Override public String valuePattern() { return "path"; }

        abstract Path toPath(String path);
    }

    static class ClassPathConverter extends AbstractPathConverter {
        static final ValueConverter<List<Path>> INSTANCE = new ClassPathConverter();

        @Override
        public Path toPath(String value) {
            try {
                Path path = CWD.resolve(value);
                if (Files.notExists(path))
                    throw new CommandException("err.path.not.found", path);
                if (!(Files.isDirectory(path) ||
                        (Files.isRegularFile(path) && path.toString().endsWith(".jar"))))
                    throw new CommandException("err.invalid.class.path.entry", path);
                return path;
            } catch (InvalidPathException x) {
                throw new CommandException("err.path.not.valid", value);
            }
        }
    }

    static class DirPathConverter extends AbstractPathConverter {
        static final ValueConverter<List<Path>> INSTANCE = new DirPathConverter();

        @Override
        public Path toPath(String value) {
            try {
                Path path = CWD.resolve(value);
                if (Files.notExists(path))
                    throw new CommandException("err.path.not.found", path);
                if (!Files.isDirectory(path))
                    throw new CommandException("err.path.not.a.dir", path);
                return path;
            } catch (InvalidPathException x) {
                throw new CommandException("err.path.not.valid", value);
            }
        }
    }

    static class ExtractDirPathConverter implements ValueConverter<Path> {

        @Override
        public Path convert(String value) {
            try {
                Path path = CWD.resolve(value);
                if (Files.exists(path)) {
                    if (!Files.isDirectory(path))
                        throw new CommandException("err.cannot.create.dir", path);
                }
                return path;
            } catch (InvalidPathException x) {
                throw new CommandException("err.path.not.valid", value);
            }
        }

        @Override  public Class<Path> valueType() { return Path.class; }

        @Override  public String valuePattern() { return "path"; }
    }

    static class ModuleVersionConverter implements ValueConverter<Version> {
        @Override
        public Version convert(String value) {
            try {
                return Version.parse(value);
            } catch (IllegalArgumentException x) {
                throw new CommandException("err.invalid.version", x.getMessage());
            }
        }

        @Override public Class<Version> valueType() { return Version.class; }

        @Override public String valuePattern() { return "module-version"; }
    }

    static class WarnIfResolvedReasonConverter
        implements ValueConverter<ModuleResolution>
    {
        @Override
        public ModuleResolution convert(String value) {
            if (value.equals("deprecated"))
                return ModuleResolution.empty().withDeprecated();
            else if (value.equals("deprecated-for-removal"))
                return ModuleResolution.empty().withDeprecatedForRemoval();
            else if (value.equals("incubating"))
                return ModuleResolution.empty().withIncubating();
            else
                throw new CommandException("err.bad.WarnIfResolvedReason", value);
        }

        @Override public Class<ModuleResolution> valueType() {
            return ModuleResolution.class;
        }

        @Override public String valuePattern() { return "reason"; }
    }

    static class PatternConverter implements ValueConverter<Pattern> {
        @Override
        public Pattern convert(String value) {
            try {
                if (value.startsWith("regex:")) {
                    value = value.substring("regex:".length()).trim();
                }

                return Pattern.compile(value);
            } catch (PatternSyntaxException e) {
                throw new CommandException("err.bad.pattern", value);
            }
        }

        @Override public Class<Pattern> valueType() { return Pattern.class; }

        @Override public String valuePattern() { return "regex-pattern"; }
    }

    static class PathMatcherConverter implements ValueConverter<PathMatcher> {
        @Override
        public PathMatcher convert(String pattern) {
            try {
                return Utils.getPathMatcher(FileSystems.getDefault(), pattern);
            } catch (PatternSyntaxException e) {
                throw new CommandException("err.bad.pattern", pattern);
            }
        }

        @Override public Class<PathMatcher> valueType() { return PathMatcher.class; }

        @Override public String valuePattern() { return "pattern-list"; }
    }

    /* Support for @<file> in jmod help */
    private static final String CMD_FILENAME = "@<filename>";

    /**
     * This formatter is adding the @filename option and does the required
     * formatting.
     */
    private static final class JmodHelpFormatter extends BuiltinHelpFormatter {

        private final Options opts;

        private JmodHelpFormatter(Options opts) {
            super(80, 2);
            this.opts = opts;
        }

        @Override
        public String format(Map<String, ? extends OptionDescriptor> options) {
            Map<String, OptionDescriptor> all = new LinkedHashMap<>();
            all.putAll(options);

            // extra options
            if (!opts.helpExtra) {
                all.remove("do-not-resolve-by-default");
                all.remove("warn-if-resolved");
            }

            all.put(CMD_FILENAME, new OptionDescriptor() {
                @Override
                public List<String> options() {
                    List<String> ret = new ArrayList<>();
                    ret.add(CMD_FILENAME);
                    return ret;
                }
                @Override
                public String description() { return getMessage("main.opt.cmdfile"); }
                @Override
                public List<?> defaultValues() { return Collections.emptyList(); }
                @Override
                public boolean isRequired() { return false; }
                @Override
                public boolean acceptsArguments() { return false; }
                @Override
                public boolean requiresArgument() { return false; }
                @Override
                public String argumentDescription() { return null; }
                @Override
                public String argumentTypeIndicator() { return null; }
                @Override
                public boolean representsNonOptions() { return false; }
            });
            String content = super.format(all);
            StringBuilder builder = new StringBuilder();

            builder.append(getMessage("main.opt.mode")).append("\n  ");
            builder.append(getMessage("main.opt.mode.create")).append("\n  ");
            builder.append(getMessage("main.opt.mode.extract")).append("\n  ");
            builder.append(getMessage("main.opt.mode.list")).append("\n  ");
            builder.append(getMessage("main.opt.mode.describe")).append("\n  ");
            builder.append(getMessage("main.opt.mode.hash")).append("\n\n");

            String cmdfile = null;
            String[] lines = content.split("\n");
            for (String line : lines) {
                if (line.startsWith("--@")) {
                    cmdfile = line.replace("--" + CMD_FILENAME, CMD_FILENAME + "  ");
                } else if (line.startsWith("Option") || line.startsWith("------")) {
                    builder.append(" ").append(line).append("\n");
                } else if (!line.matches("Non-option arguments")){
                    builder.append("  ").append(line).append("\n");
                }
            }
            if (cmdfile != null) {
                builder.append("  ").append(cmdfile).append("\n");
            }
            return builder.toString();
        }
    }

    private final OptionParser parser = new OptionParser("hp");

    private void handleOptions(String[] args) {
        options = new Options();
        parser.formatHelpWith(new JmodHelpFormatter(options));

        OptionSpec<List<Path>> classPath
                = parser.accepts("class-path", getMessage("main.opt.class-path"))
                        .withRequiredArg()
                        .withValuesConvertedBy(ClassPathConverter.INSTANCE);

        OptionSpec<List<Path>> cmds
                = parser.accepts("cmds", getMessage("main.opt.cmds"))
                        .withRequiredArg()
                        .withValuesConvertedBy(DirPathConverter.INSTANCE);

        OptionSpec<List<Path>> config
                = parser.accepts("config", getMessage("main.opt.config"))
                        .withRequiredArg()
                        .withValuesConvertedBy(DirPathConverter.INSTANCE);

        OptionSpec<Path> dir
                = parser.accepts("dir", getMessage("main.opt.extractDir"))
                        .withRequiredArg()
                        .withValuesConvertedBy(new ExtractDirPathConverter());

        OptionSpec<Void> dryrun
                = parser.accepts("dry-run", getMessage("main.opt.dry-run"));

        OptionSpec<PathMatcher> excludes
                = parser.accepts("exclude", getMessage("main.opt.exclude"))
                        .withRequiredArg()
                        .withValuesConvertedBy(new PathMatcherConverter());

        OptionSpec<Pattern> hashModules
                = parser.accepts("hash-modules", getMessage("main.opt.hash-modules"))
                        .withRequiredArg()
                        .withValuesConvertedBy(new PatternConverter());

        OptionSpec<Void> help
                = parser.acceptsAll(List.of("h", "help", "?"), getMessage("main.opt.help"))
                        .forHelp();

        OptionSpec<Void> helpExtra
                = parser.accepts("help-extra", getMessage("main.opt.help-extra"));

        OptionSpec<List<Path>> headerFiles
                = parser.accepts("header-files", getMessage("main.opt.header-files"))
                        .withRequiredArg()
                        .withValuesConvertedBy(DirPathConverter.INSTANCE);

        OptionSpec<List<Path>> libs
                = parser.accepts("libs", getMessage("main.opt.libs"))
                        .withRequiredArg()
                        .withValuesConvertedBy(DirPathConverter.INSTANCE);

        OptionSpec<List<Path>> legalNotices
                = parser.accepts("legal-notices", getMessage("main.opt.legal-notices"))
                        .withRequiredArg()
                        .withValuesConvertedBy(DirPathConverter.INSTANCE);


        OptionSpec<String> mainClass
                = parser.accepts("main-class", getMessage("main.opt.main-class"))
                        .withRequiredArg()
                        .describedAs(getMessage("main.opt.main-class.arg"));

        OptionSpec<List<Path>> manPages
                = parser.accepts("man-pages", getMessage("main.opt.man-pages"))
                        .withRequiredArg()
                        .withValuesConvertedBy(DirPathConverter.INSTANCE);

        OptionSpec<List<Path>> modulePath
                = parser.acceptsAll(List.of("p", "module-path"),
                                    getMessage("main.opt.module-path"))
                        .withRequiredArg()
                        .withValuesConvertedBy(DirPathConverter.INSTANCE);

        OptionSpec<Version> moduleVersion
                = parser.accepts("module-version", getMessage("main.opt.module-version"))
                        .withRequiredArg()
                        .withValuesConvertedBy(new ModuleVersionConverter());

        OptionSpec<String> targetPlatform
                = parser.accepts("target-platform", getMessage("main.opt.target-platform"))
                        .withRequiredArg()
                        .describedAs(getMessage("main.opt.target-platform.arg"));

        OptionSpec<Void> doNotResolveByDefault
                = parser.accepts("do-not-resolve-by-default",
                                 getMessage("main.opt.do-not-resolve-by-default"));

        OptionSpec<ModuleResolution> warnIfResolved
                = parser.accepts("warn-if-resolved", getMessage("main.opt.warn-if-resolved"))
                        .withRequiredArg()
                        .withValuesConvertedBy(new WarnIfResolvedReasonConverter());

        OptionSpec<Void> version
                = parser.accepts("version", getMessage("main.opt.version"));

        NonOptionArgumentSpec<String> nonOptions
                = parser.nonOptions();

        try {
            OptionSet opts = parser.parse(args);

            if (opts.has(help) || opts.has(helpExtra) || opts.has(version)) {
                options.help = opts.has(help);
                options.helpExtra = opts.has(helpExtra);
                options.version = opts.has(version);
                return;  // informational message will be shown
            }

            List<String> words = opts.valuesOf(nonOptions);
            if (words.isEmpty())
                throw new CommandException("err.missing.mode").showUsage(true);
            String verb = words.get(0);
            try {
                options.mode = Enum.valueOf(Mode.class, verb.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new CommandException("err.invalid.mode", verb).showUsage(true);
            }

            if (opts.has(classPath))
                options.classpath = getLastElement(opts.valuesOf(classPath));
            if (opts.has(cmds))
                options.cmds = getLastElement(opts.valuesOf(cmds));
            if (opts.has(config))
                options.configs = getLastElement(opts.valuesOf(config));
            if (opts.has(dir))
                options.extractDir = getLastElement(opts.valuesOf(dir));
            if (opts.has(dryrun))
                options.dryrun = true;
            if (opts.has(excludes))
                options.excludes = opts.valuesOf(excludes);  // excludes is repeatable
            if (opts.has(libs))
                options.libs = getLastElement(opts.valuesOf(libs));
            if (opts.has(headerFiles))
                options.headerFiles = getLastElement(opts.valuesOf(headerFiles));
            if (opts.has(manPages))
                options.manPages = getLastElement(opts.valuesOf(manPages));
            if (opts.has(legalNotices))
                options.legalNotices = getLastElement(opts.valuesOf(legalNotices));
            if (opts.has(modulePath)) {
                Path[] dirs = getLastElement(opts.valuesOf(modulePath)).toArray(new Path[0]);
                options.moduleFinder = ModulePath.of(Runtime.version(), true, dirs);
            }
            if (opts.has(moduleVersion))
                options.moduleVersion = getLastElement(opts.valuesOf(moduleVersion));
            if (opts.has(mainClass))
                options.mainClass = getLastElement(opts.valuesOf(mainClass));
            if (opts.has(targetPlatform))
                options.targetPlatform = getLastElement(opts.valuesOf(targetPlatform));
            if (opts.has(warnIfResolved))
                options.moduleResolution = getLastElement(opts.valuesOf(warnIfResolved));
            if (opts.has(doNotResolveByDefault)) {
                if (options.moduleResolution == null)
                    options.moduleResolution = ModuleResolution.empty();
                options.moduleResolution = options.moduleResolution.withDoNotResolveByDefault();
            }
            if (opts.has(hashModules)) {
                options.modulesToHash = getLastElement(opts.valuesOf(hashModules));
                // if storing hashes then the module path is required
                if (options.moduleFinder == null)
                    throw new CommandException("err.modulepath.must.be.specified")
                            .showUsage(true);
            }

            if (options.mode.equals(Mode.HASH)) {
                if (options.moduleFinder == null || options.modulesToHash == null)
                    throw new CommandException("err.modulepath.must.be.specified")
                            .showUsage(true);
                // It's optional to specify jmod-file.  If not specified, then
                // it will find all the modules that have no outgoing read edges
                if (words.size() >= 2) {
                    Path path = Paths.get(words.get(1));
                    if (Files.notExists(path))
                        throw new CommandException("err.jmod.not.found", path);

                    options.jmodFile = path;
                }
                if (words.size() > 2)
                    throw new CommandException("err.unknown.option",
                            words.subList(2, words.size())).showUsage(true);
            } else {
                if (words.size() <= 1)
                    throw new CommandException("err.jmod.must.be.specified").showUsage(true);
                Path path = Paths.get(words.get(1));

                if (options.mode.equals(Mode.CREATE) && Files.exists(path))
                    throw new CommandException("err.file.already.exists", path);
                else if ((options.mode.equals(Mode.LIST) ||
                            options.mode.equals(Mode.DESCRIBE) ||
                            options.mode.equals((Mode.EXTRACT)))
                         && Files.notExists(path))
                    throw new CommandException("err.jmod.not.found", path);

                if (options.dryrun) {
                    throw new CommandException("err.invalid.dryrun.option");
                }
                options.jmodFile = path;

                if (words.size() > 2)
                    throw new CommandException("err.unknown.option",
                            words.subList(2, words.size())).showUsage(true);
            }

            if (options.mode.equals(Mode.CREATE) && options.classpath == null)
                throw new CommandException("err.classpath.must.be.specified").showUsage(true);
            if (options.mainClass != null && !isValidJavaIdentifier(options.mainClass))
                throw new CommandException("err.invalid.main-class", options.mainClass);
            if (options.mode.equals(Mode.EXTRACT) && options.extractDir != null) {
                try {
                    Files.createDirectories(options.extractDir);
                } catch (IOException ioe) {
                    throw new CommandException("err.cannot.create.dir", options.extractDir);
                }
            }
        } catch (OptionException e) {
             throw new CommandException(e.getMessage());
        }
    }

    /**
     * Returns true if, and only if, the given main class is a legal.
     */
    static boolean isValidJavaIdentifier(String mainClass) {
        if (mainClass.length() == 0)
            return false;

        if (!Character.isJavaIdentifierStart(mainClass.charAt(0)))
            return false;

        int n = mainClass.length();
        for (int i=1; i < n; i++) {
            char c = mainClass.charAt(i);
            if (!Character.isJavaIdentifierPart(c) && c != '.')
                return false;
        }
        if (mainClass.charAt(n-1) == '.')
            return false;

        return true;
    }

    static <E> E getLastElement(List<E> list) {
        if (list.size() == 0)
            throw new InternalError("Unexpected 0 list size");
        return list.get(list.size() - 1);
    }

    private void reportError(String message) {
        out.println(getMessage("error.prefix") + " " + message);
    }

    private void warning(String key, Object... args) {
        out.println(getMessage("warn.prefix") + " " + getMessage(key, args));
    }

    private void showUsageSummary() {
        out.println(getMessage("main.usage.summary", PROGNAME));
    }

    private void showHelp() {
        out.println(getMessage("main.usage", PROGNAME));
        try {
            parser.printHelpOn(out);
        } catch (IOException x) {
            throw new AssertionError(x);
        }
    }

    private void showVersion() {
        out.println(version());
    }

    private String version() {
        return System.getProperty("java.version");
    }

    private static String getMessage(String key, Object... args) {
        try {
            return MessageFormat.format(ResourceBundleHelper.bundle.getString(key), args);
        } catch (MissingResourceException e) {
            throw new InternalError("Missing message: " + key);
        }
    }

    private static class ResourceBundleHelper {
        static final ResourceBundle bundle;

        static {
            Locale locale = Locale.getDefault();
            try {
                bundle = ResourceBundle.getBundle("jdk.tools.jmod.resources.jmod", locale);
            } catch (MissingResourceException e) {
                throw new InternalError("Cannot find jmod resource bundle for locale " + locale);
            }
        }
    }
}
