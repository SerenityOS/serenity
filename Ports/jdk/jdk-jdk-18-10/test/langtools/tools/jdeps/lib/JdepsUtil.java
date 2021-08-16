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

import com.sun.tools.jdeps.Analyzer;
import com.sun.tools.jdeps.DepsAnalyzer;
import com.sun.tools.jdeps.InverseDepsAnalyzer;
import com.sun.tools.jdeps.JdepsConfiguration;
import com.sun.tools.jdeps.JdepsFilter;
import com.sun.tools.jdeps.JdepsWriter;
import com.sun.tools.jdeps.ModuleAnalyzer;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Utilities to run jdeps command
 */
public final class JdepsUtil {
    public static Command newCommand(String cmd) {
        return new Command(cmd);
    }

    public static class Command implements Closeable {

        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        final JdepsFilter.Builder filter = new JdepsFilter.Builder().filter(true, true);
        final JdepsConfiguration.Builder builder =  new JdepsConfiguration.Builder();
        final Set<String> requires = new HashSet<>();

        JdepsConfiguration configuration;
        Analyzer.Type verbose = Analyzer.Type.PACKAGE;
        boolean apiOnly = false;

        public Command(String cmd) {
            System.err.println("============ ");
            System.err.println(cmd);
        }

        public Command verbose(String verbose) {
            switch (verbose) {
                case "-verbose":
                    this.verbose = Analyzer.Type.VERBOSE;
                    filter.filter(false, false);
                    break;
                case "-verbose:package":
                    this.verbose = Analyzer.Type.PACKAGE;
                    break;
                case "-verbose:class":
                    this.verbose = Analyzer.Type.CLASS;
                    break;
                case "-summary":
                    this.verbose = Analyzer.Type.SUMMARY;
                    break;
                default:
                    throw new IllegalArgumentException(verbose);
            }
            return this;
        }

        public Command filter(String value) {
            switch (value) {
                case "-filter:package":
                    filter.filter(true, false);
                    break;
                case "-filter:archive":
                case "-filter:module":
                    filter.filter(false, true);
                    break;
                default:
                    throw new IllegalArgumentException(value);
            }
            return this;
        }

        public Command addClassPath(String classpath) {
            builder.addClassPath(classpath);
            return this;
        }

        public Command addRoot(Path path) {
            builder.addRoot(path);
            return this;
        }

        public Command appModulePath(String modulePath) {
            builder.appModulePath(modulePath);
            return this;
        }

        public Command addmods(Set<String> mods) {
            builder.addmods(mods);
            return this;
        }

        public Command requires(Set<String> mods) {
            requires.addAll(mods);
            return this;
        }

        public Command matchPackages(Set<String> pkgs) {
            filter.packages(pkgs);
            return this;
        }

        public Command regex(String regex) {
            filter.regex(Pattern.compile(regex));
            return this;
        }

        public Command include(String regex) {
            filter.includePattern(Pattern.compile(regex));
            return this;
        }

        public Command apiOnly() {
            this.apiOnly = true;
            return this;
        }

        public JdepsConfiguration configuration() throws IOException {
            if (configuration == null) {
                this.configuration = builder.build();
                requires.forEach(name -> {
                    ModuleDescriptor md = configuration.findModuleDescriptor(name).get();
                    filter.requires(name, md.packages());
                });
            }
            return configuration;
        }

        private JdepsWriter writer() {
            return JdepsWriter.newSimpleWriter(pw, verbose);
        }

        public DepsAnalyzer getDepsAnalyzer() throws IOException {
            return new DepsAnalyzer(configuration(), filter.build(), writer(),
                                    verbose, apiOnly);
        }

        public ModuleAnalyzer getModuleAnalyzer(Set<String> mods) throws IOException {
            // if --check is set, add to the root set and all modules are observable
            addmods(mods);
            builder.addmods(Set.of("ALL-SYSTEM", "ALL-MODULE-PATH"));
            return new ModuleAnalyzer(configuration(), pw, mods);
        }

        public InverseDepsAnalyzer getInverseDepsAnalyzer() throws IOException {
            return new InverseDepsAnalyzer(configuration(), filter.build(), writer(),
                                           verbose, false);
        }

        public void dumpOutput(PrintStream out) {
            out.println(sw.toString());
        }

        @Override
        public void close() throws IOException {
            configuration.close();
        }
    }

    /**
     * Create a jar file using the list of files provided.
     */
    public static void createJar(Path jarfile, Path root, Stream<Path> files)
        throws IOException {
        Path dir = jarfile.getParent();
        if (dir != null && Files.notExists(dir)) {
            Files.createDirectories(dir);
        }
        try (JarOutputStream target = new JarOutputStream(
            Files.newOutputStream(jarfile))) {
            files.forEach(file -> add(root.relativize(file), file, target));
        }
    }

    private static void add(Path path, Path source, JarOutputStream target) {
        try {
            String name = path.toString().replace(File.separatorChar, '/');
            JarEntry entry = new JarEntry(name);
            entry.setTime(source.toFile().lastModified());
            target.putNextEntry(entry);
            Files.copy(source, target);
            target.closeEntry();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
