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
package com.sun.tools.jdeps;

import static com.sun.tools.jdeps.JdepsTask.*;
import static com.sun.tools.jdeps.Analyzer.*;
import static com.sun.tools.jdeps.JdepsFilter.DEFAULT_FILTER;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Exports;
import java.lang.module.ModuleDescriptor.Provides;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static java.util.stream.Collectors.*;


public class ModuleInfoBuilder {
    final JdepsConfiguration configuration;
    final Path outputdir;
    final boolean open;

    final DependencyFinder dependencyFinder;
    final Analyzer analyzer;

    // an input JAR file (loaded as an automatic module for analysis)
    // maps to a normal module to generate module-info.java
    final Map<Module, Module> automaticToNormalModule;
    public ModuleInfoBuilder(JdepsConfiguration configuration,
                             List<String> args,
                             Path outputdir,
                             boolean open) {
        this.configuration = configuration;
        this.outputdir = outputdir;
        this.open = open;

        this.dependencyFinder = new DependencyFinder(configuration, DEFAULT_FILTER);
        this.analyzer = new Analyzer(configuration, Type.CLASS, DEFAULT_FILTER);

        // add targets to modulepath if it has module-info.class
        List<Path> paths = args.stream()
            .map(fn -> Paths.get(fn))
            .toList();

        // automatic module to convert to normal module
        this.automaticToNormalModule = ModuleFinder.of(paths.toArray(new Path[0]))
                .findAll().stream()
                .map(configuration::toModule)
                .collect(toMap(Function.identity(), Function.identity()));

        Optional<Module> om = automaticToNormalModule.keySet().stream()
                                    .filter(m -> !m.descriptor().isAutomatic())
                                    .findAny();
        if (om.isPresent()) {
            throw new UncheckedBadArgs(new BadArgs("err.genmoduleinfo.not.jarfile",
                                                   om.get().getPathName()));
        }
        if (automaticToNormalModule.isEmpty()) {
            throw new UncheckedBadArgs(new BadArgs("err.invalid.path", args));
        }
    }

    public boolean run(boolean ignoreMissingDeps, PrintWriter log, boolean quiet) throws IOException {
        try {
            // pass 1: find API dependencies
            Map<Archive, Set<Archive>> requiresTransitive = computeRequiresTransitive();

            // pass 2: analyze all class dependences
            dependencyFinder.parse(automaticModules().stream());

            analyzer.run(automaticModules(), dependencyFinder.locationToArchive());

            for (Module m : automaticModules()) {
                Set<Archive> apiDeps = requiresTransitive.containsKey(m)
                                            ? requiresTransitive.get(m)
                                            : Collections.emptySet();

                // if this is a multi-release JAR, write to versions/$VERSION/module-info.java
                Runtime.Version version = configuration.getVersion();
                Path dir = version != null
                            ? outputdir.resolve(m.name())
                                       .resolve("versions")
                                       .resolve(String.valueOf(version.feature()))
                            : outputdir.resolve(m.name());
                Path file = dir.resolve("module-info.java");

                // computes requires and requires transitive
                Module normalModule = toNormalModule(m, apiDeps, ignoreMissingDeps);
                if (normalModule != null) {
                    automaticToNormalModule.put(m, normalModule);

                    // generate module-info.java
                    if (!quiet) {
                        if (ignoreMissingDeps && analyzer.requires(m).anyMatch(Analyzer::notFound)) {
                            log.format("Warning: --ignore-missing-deps specified. Missing dependencies from %s are ignored%n",
                                       m.name());
                        }
                        log.format("writing to %s%n", file);
                    }
                    writeModuleInfo(file,  normalModule.descriptor());
                } else {
                    // find missing dependences
                    return false;
                }
            }

        } finally {
            dependencyFinder.shutdown();
        }
        return true;
    }

    private Module toNormalModule(Module module, Set<Archive> requiresTransitive, boolean ignoreMissingDeps)
        throws IOException
    {
        // done analysis
        module.close();

        if (!ignoreMissingDeps && analyzer.requires(module).anyMatch(Analyzer::notFound)) {
            // missing dependencies
            return null;
        }

        Map<String, Boolean> requires = new HashMap<>();
        requiresTransitive.stream()
            .filter(a -> !(ignoreMissingDeps && Analyzer.notFound(a)))
            .map(Archive::getModule)
            .forEach(m -> requires.put(m.name(), Boolean.TRUE));

        analyzer.requires(module)
            .filter(a -> !(ignoreMissingDeps && Analyzer.notFound(a)))
            .map(Archive::getModule)
            .forEach(d -> requires.putIfAbsent(d.name(), Boolean.FALSE));

        return module.toNormalModule(requires);
    }

    /**
     * Returns the stream of resulting modules
     */
    Stream<Module> modules() {
        return automaticToNormalModule.values().stream();
    }

    /**
     * Returns the stream of resulting ModuleDescriptors
     */
    public Stream<ModuleDescriptor> descriptors() {
        return automaticToNormalModule.entrySet().stream()
                    .map(Map.Entry::getValue)
                    .map(Module::descriptor);
    }

    void visitMissingDeps(Analyzer.Visitor visitor) {
        automaticModules().stream()
            .filter(m -> analyzer.requires(m).anyMatch(Analyzer::notFound))
            .forEach(m -> {
                analyzer.visitDependences(m, visitor, Analyzer.Type.VERBOSE, Analyzer::notFound);
            });
    }

    void writeModuleInfo(Path file, ModuleDescriptor md) {
        try {
            Files.createDirectories(file.getParent());
            try (PrintWriter pw = new PrintWriter(Files.newOutputStream(file))) {
                printModuleInfo(pw, md);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void printModuleInfo(PrintWriter writer, ModuleDescriptor md) {
        writer.format("%smodule %s {%n", open ? "open " : "", md.name());

        Map<String, Module> modules = configuration.getModules();

        // first print requires
        Set<Requires> reqs = md.requires().stream()
            .filter(req -> !req.name().equals("java.base") && req.modifiers().isEmpty())
            .collect(Collectors.toSet());
        reqs.stream()
            .sorted(Comparator.comparing(Requires::name))
            .forEach(req -> writer.format("    requires %s;%n",
                                          toString(req.modifiers(), req.name())));
        if (!reqs.isEmpty()) {
            writer.println();
        }

        // requires transitive
        reqs = md.requires().stream()
                 .filter(req -> !req.name().equals("java.base") && !req.modifiers().isEmpty())
                 .collect(Collectors.toSet());
        reqs.stream()
            .sorted(Comparator.comparing(Requires::name))
            .forEach(req -> writer.format("    requires %s;%n",
                                          toString(req.modifiers(), req.name())));
        if (!reqs.isEmpty()) {
            writer.println();
        }

        if (!open) {
            md.exports().stream()
              .peek(exp -> {
                  if (exp.isQualified())
                      throw new InternalError(md.name() + " qualified exports: " + exp);
                  })
              .sorted(Comparator.comparing(Exports::source))
              .forEach(exp -> writer.format("    exports %s;%n", exp.source()));

            if (!md.exports().isEmpty()) {
                writer.println();
            }
        }

        md.provides().stream()
          .sorted(Comparator.comparing(Provides::service))
          .map(p -> p.providers().stream()
                     .map(impl -> "        " + impl.replace('$', '.'))
                     .collect(joining(",\n",
                                      String.format("    provides %s with%n",
                                                    p.service().replace('$', '.')),
                                      ";")))
                     .forEach(writer::println);

        if (!md.provides().isEmpty()) {
            writer.println();
        }
        writer.println("}");
    }

    private Set<Module> automaticModules() {
        return automaticToNormalModule.keySet();
    }

    /**
     * Returns a string containing the given set of modifiers and label.
     */
    private static <M> String toString(Set<M> mods, String what) {
        return (Stream.concat(mods.stream().map(e -> e.toString().toLowerCase(Locale.US)),
                              Stream.of(what)))
                      .collect(Collectors.joining(" "));
    }

    /**
     * Compute 'requires transitive' dependences by analyzing API dependencies
     */
    private Map<Archive, Set<Archive>> computeRequiresTransitive()
        throws IOException
    {
        // parse the input modules
        dependencyFinder.parseExportedAPIs(automaticModules().stream());

        return dependencyFinder.dependences();
    }
}
