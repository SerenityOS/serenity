/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.tools.jdeps.JdepsFilter.DEFAULT_FILTER;
import static com.sun.tools.jdeps.Module.*;
import static java.lang.module.ModuleDescriptor.Requires.Modifier.*;
import static java.util.stream.Collectors.*;

import com.sun.tools.classfile.Dependency;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.module.ModuleDescriptor;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Analyze module dependences and compare with module descriptor.
 * Also identify any qualified exports not used by the target module.
 */
public class ModuleAnalyzer {
    private static final String JAVA_BASE = "java.base";

    private final JdepsConfiguration configuration;
    private final PrintWriter log;
    private final DependencyFinder dependencyFinder;
    private final Map<Module, ModuleDeps> modules;

    public ModuleAnalyzer(JdepsConfiguration config,
                          PrintWriter log,
                          Set<String> names) {
        this.configuration = config;
        this.log = log;

        this.dependencyFinder = new DependencyFinder(config, DEFAULT_FILTER);
        if (names.isEmpty()) {
            this.modules = configuration.rootModules().stream()
                .collect(toMap(Function.identity(), ModuleDeps::new));
        } else {
            this.modules = names.stream()
                .map(configuration::findModule)
                .flatMap(Optional::stream)
                .collect(toMap(Function.identity(), ModuleDeps::new));
        }
    }

    public boolean run(boolean ignoreMissingDeps) throws IOException {
        try {
            for (ModuleDeps md: modules.values()) {
                // compute "requires transitive" dependences
                md.computeRequiresTransitive(ignoreMissingDeps);
                // compute "requires" dependences
                md.computeRequires(ignoreMissingDeps);
                // print module descriptor
                md.printModuleDescriptor();

                // apply transitive reduction and reports recommended requires.
                boolean ok = md.analyzeDeps();
                if (!ok) return false;

                if (ignoreMissingDeps && md.hasMissingDependencies()) {
                    log.format("Warning: --ignore-missing-deps specified. Missing dependencies from %s are ignored%n",
                               md.root.name());
                }
            }
        } finally {
            dependencyFinder.shutdown();
        }
        return true;
    }


    class ModuleDeps {
        final Module root;
        Set<Module> requiresTransitive;
        Set<Module> requires;
        Map<String, Set<String>> unusedQualifiedExports;

        ModuleDeps(Module root) {
            this.root = root;
        }

        /**
         * Compute 'requires transitive' dependences by analyzing API dependencies
         */
        private void computeRequiresTransitive(boolean ignoreMissingDeps) {
            // record requires transitive
            this.requiresTransitive = computeRequires(true, ignoreMissingDeps)
                .filter(m -> !m.name().equals(JAVA_BASE))
                .collect(toSet());

            trace("requires transitive: %s%n", requiresTransitive);
        }

        private void computeRequires(boolean ignoreMissingDeps) {
            this.requires = computeRequires(false, ignoreMissingDeps).collect(toSet());
            trace("requires: %s%n", requires);
        }

        private Stream<Module> computeRequires(boolean apionly, boolean ignoreMissingDeps) {
            // analyze all classes
            if (apionly) {
                dependencyFinder.parseExportedAPIs(Stream.of(root));
            } else {
                dependencyFinder.parse(Stream.of(root));
            }

            // find the modules of all the dependencies found
            return dependencyFinder.getDependences(root)
                        .filter(a -> !(ignoreMissingDeps && Analyzer.notFound(a)))
                        .map(Archive::getModule);
        }

        boolean hasMissingDependencies() {
            return dependencyFinder.getDependences(root).anyMatch(Analyzer::notFound);
        }

        ModuleDescriptor descriptor() {
            return descriptor(requiresTransitive, requires);
        }

        private ModuleDescriptor descriptor(Set<Module> requiresTransitive,
                                            Set<Module> requires) {

            ModuleDescriptor.Builder builder = ModuleDescriptor.newModule(root.name());

            if (!root.name().equals(JAVA_BASE))
                builder.requires(Set.of(MANDATED), JAVA_BASE);

            requiresTransitive.stream()
                .filter(m -> !m.name().equals(JAVA_BASE))
                .map(Module::name)
                .forEach(mn -> builder.requires(Set.of(TRANSITIVE), mn));

            requires.stream()
                .filter(m -> !requiresTransitive.contains(m))
                .filter(m -> !m.name().equals(JAVA_BASE))
                .map(Module::name)
                .forEach(mn -> builder.requires(mn));

            return builder.build();
        }

        private Graph<Module> buildReducedGraph() {
            ModuleGraphBuilder rpBuilder = new ModuleGraphBuilder(configuration);
            rpBuilder.addModule(root);
            requiresTransitive.stream()
                          .forEach(m -> rpBuilder.addEdge(root, m));

            // requires transitive graph
            Graph<Module> rbg = rpBuilder.build().reduce();

            ModuleGraphBuilder gb = new ModuleGraphBuilder(configuration);
            gb.addModule(root);
            requires.stream()
                    .forEach(m -> gb.addEdge(root, m));

            // transitive reduction
            Graph<Module> newGraph = gb.buildGraph().reduce(rbg);
            if (DEBUG) {
                System.err.println("after transitive reduction: ");
                newGraph.printGraph(log);
            }
            return newGraph;
        }

        /**
         * Apply the transitive reduction on the module graph
         * and returns the corresponding ModuleDescriptor
         */
        ModuleDescriptor reduced() {
            Graph<Module> g = buildReducedGraph();
            return descriptor(requiresTransitive, g.adjacentNodes(root));
        }

        private void showMissingDeps() {
            // build the analyzer if there are missing dependences
            Analyzer analyzer = new Analyzer(configuration, Analyzer.Type.CLASS, DEFAULT_FILTER);
            analyzer.run(Set.of(root), dependencyFinder.locationToArchive());
            log.println("Error: Missing dependencies: classes not found from the module path.");
            Analyzer.Visitor visitor = new Analyzer.Visitor() {
                @Override
                public void visitDependence(String origin, Archive originArchive, String target, Archive targetArchive) {
                    log.format("   %-50s -> %-50s %s%n", origin, target, targetArchive.getName());
                }
            };
            analyzer.visitDependences(root, visitor, Analyzer.Type.VERBOSE, Analyzer::notFound);
            log.println();
        }

        /**
         * Apply transitive reduction on the resulting graph and reports
         * recommended requires.
         */
        private boolean analyzeDeps() {
            if (requires.stream().anyMatch(m -> m == UNNAMED_MODULE)) {
                showMissingDeps();
                return false;
            }

            ModuleDescriptor analyzedDescriptor = descriptor();
            if (!matches(root.descriptor(), analyzedDescriptor)) {
                log.format("  [Suggested module descriptor for %s]%n", root.name());
                analyzedDescriptor.requires()
                    .stream()
                    .sorted(Comparator.comparing(ModuleDescriptor.Requires::name))
                    .forEach(req -> log.format("    requires %s;%n", req));
            }

            ModuleDescriptor reduced = reduced();
            if (!matches(root.descriptor(), reduced)) {
                log.format("  [Transitive reduced graph for %s]%n", root.name());
                reduced.requires()
                    .stream()
                    .sorted(Comparator.comparing(ModuleDescriptor.Requires::name))
                    .forEach(req -> log.format("    requires %s;%n", req));
            }

            checkQualifiedExports();
            log.println();
            return true;
        }

        private void checkQualifiedExports() {
            // detect any qualified exports not used by the target module
            unusedQualifiedExports = unusedQualifiedExports();
            if (!unusedQualifiedExports.isEmpty())
                log.format("  [Unused qualified exports in %s]%n", root.name());

            unusedQualifiedExports.keySet().stream()
                .sorted()
                .forEach(pn -> log.format("    exports %s to %s%n", pn,
                    unusedQualifiedExports.get(pn).stream()
                        .sorted()
                        .collect(joining(","))));
        }

        void printModuleDescriptor() {
            printModuleDescriptor(log, root);
        }

        private void printModuleDescriptor(PrintWriter out, Module module) {
            ModuleDescriptor descriptor = module.descriptor();
            out.format("%s (%s)%n", descriptor.name(), module.location());

            if (descriptor.name().equals(JAVA_BASE))
                return;

            out.println("  [Module descriptor]");
            descriptor.requires()
                .stream()
                .sorted(Comparator.comparing(ModuleDescriptor.Requires::name))
                .forEach(req -> out.format("    requires %s;%n", req));
        }


        /**
         * Detects any qualified exports not used by the target module.
         */
        private Map<String, Set<String>> unusedQualifiedExports() {
            Map<String, Set<String>> unused = new HashMap<>();

            // build the qualified exports map
            Map<String, Set<String>> qualifiedExports =
                root.exports().entrySet().stream()
                    .filter(e -> !e.getValue().isEmpty())
                    .map(Map.Entry::getKey)
                    .collect(toMap(Function.identity(), _k -> new HashSet<>()));

            Set<Module> mods = new HashSet<>();
            root.exports().values()
                .stream()
                .flatMap(Set::stream)
                .forEach(target -> configuration.findModule(target)
                    .ifPresentOrElse(mods::add,
                        () -> log.format("Warning: %s not found%n", target))
                );

            // parse all target modules
            dependencyFinder.parse(mods.stream());

            // adds to the qualified exports map if a module references it
            mods.stream().forEach(m ->
                m.getDependencies()
                    .map(Dependency.Location::getPackageName)
                    .filter(qualifiedExports::containsKey)
                    .forEach(pn -> qualifiedExports.get(pn).add(m.name())));

            // compare with the exports from ModuleDescriptor
            Set<String> staleQualifiedExports =
                qualifiedExports.keySet().stream()
                    .filter(pn -> !qualifiedExports.get(pn).equals(root.exports().get(pn)))
                    .collect(toSet());

            if (!staleQualifiedExports.isEmpty()) {
                for (String pn : staleQualifiedExports) {
                    Set<String> targets = new HashSet<>(root.exports().get(pn));
                    targets.removeAll(qualifiedExports.get(pn));
                    unused.put(pn, targets);
                }
            }
            return unused;
        }
    }

    private boolean matches(ModuleDescriptor md, ModuleDescriptor other) {
        // build requires transitive from ModuleDescriptor
        Set<ModuleDescriptor.Requires> reqTransitive = md.requires().stream()
            .filter(req -> req.modifiers().contains(TRANSITIVE))
            .collect(toSet());
        Set<ModuleDescriptor.Requires> otherReqTransitive = other.requires().stream()
            .filter(req -> req.modifiers().contains(TRANSITIVE))
            .collect(toSet());

        if (!reqTransitive.equals(otherReqTransitive)) {
            trace("mismatch requires transitive: %s%n", reqTransitive);
            return false;
        }

        Set<ModuleDescriptor.Requires> unused = md.requires().stream()
            .filter(req -> !other.requires().contains(req))
            .collect(Collectors.toSet());

        if (!unused.isEmpty()) {
            trace("mismatch requires: %s%n", unused);
            return false;
        }
        return true;
    }

    // ---- for testing purpose
    public ModuleDescriptor[] descriptors(String name) {
        ModuleDeps moduleDeps = modules.keySet().stream()
            .filter(m -> m.name().equals(name))
            .map(modules::get)
            .findFirst().get();

        ModuleDescriptor[] descriptors = new ModuleDescriptor[3];
        descriptors[0] = moduleDeps.root.descriptor();
        descriptors[1] = moduleDeps.descriptor();
        descriptors[2] = moduleDeps.reduced();
        return descriptors;
    }

    public Map<String, Set<String>> unusedQualifiedExports(String name) {
        ModuleDeps moduleDeps = modules.keySet().stream()
            .filter(m -> m.name().equals(name))
            .map(modules::get)
            .findFirst().get();
        return moduleDeps.unusedQualifiedExports;
    }
}
