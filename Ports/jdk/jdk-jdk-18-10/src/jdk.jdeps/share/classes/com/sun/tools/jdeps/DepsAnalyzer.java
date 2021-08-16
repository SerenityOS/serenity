/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tools.classfile.Dependency.Location;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.sun.tools.jdeps.Analyzer.Type.CLASS;
import static com.sun.tools.jdeps.Analyzer.Type.VERBOSE;
import static com.sun.tools.jdeps.Module.trace;
import static java.util.stream.Collectors.*;

/**
 * Dependency Analyzer.
 *
 * Type of filters:
 * source filter: -include <pattern>
 * target filter: -package, -regex, --require
 *
 * The initial archive set for analysis includes
 * 1. archives specified in the command line arguments
 * 2. observable modules matching the source filter
 * 3. classpath archives matching the source filter or target filter
 * 4. --add-modules and -m root modules
 */
public class DepsAnalyzer {
    final JdepsConfiguration configuration;
    final JdepsFilter filter;
    final JdepsWriter writer;
    final Analyzer.Type verbose;
    final boolean apiOnly;

    final DependencyFinder finder;
    final Analyzer analyzer;
    final List<Archive> rootArchives = new ArrayList<>();

    // parsed archives
    final Set<Archive> archives = new LinkedHashSet<>();

    public DepsAnalyzer(JdepsConfiguration config,
                        JdepsFilter filter,
                        JdepsWriter writer,
                        Analyzer.Type verbose,
                        boolean apiOnly) {
        this.configuration = config;
        this.filter = filter;
        this.writer = writer;
        this.verbose = verbose;
        this.apiOnly = apiOnly;

        this.finder = new DependencyFinder(config, filter);
        this.analyzer = new Analyzer(configuration, verbose, filter);

        // determine initial archives to be analyzed
        this.rootArchives.addAll(configuration.initialArchives());

        // if -include pattern is specified, add the matching archives on
        // classpath to the root archives
        if (filter.hasIncludePattern() || filter.hasTargetFilter()) {
            configuration.getModules().values().stream()
                .filter(source -> include(source) && filter.matches(source))
                .forEach(this.rootArchives::add);
        }

        // class path archives
        configuration.classPathArchives().stream()
            .filter(filter::matches)
            .forEach(this.rootArchives::add);

        // Include the root modules for analysis
        this.rootArchives.addAll(configuration.rootModules());

        trace("analyze root archives: %s%n", this.rootArchives);
    }

    /*
     * Perform runtime dependency analysis
     */
    public boolean run() throws IOException {
        return run(false, 1);
    }

    /**
     * Perform compile-time view or run-time view dependency analysis.
     *
     * @param compileTimeView
     * @param maxDepth  depth of recursive analysis.  depth == 0 if -R is set
     */
    public boolean run(boolean compileTimeView, int maxDepth) throws IOException {
        try {
            // parse each packaged module or classpath archive
            if (apiOnly) {
                finder.parseExportedAPIs(rootArchives.stream());
            } else {
                finder.parse(rootArchives.stream());
            }
            archives.addAll(rootArchives);

            int depth = maxDepth > 0 ? maxDepth : Integer.MAX_VALUE;

            // transitive analysis
            if (depth > 1) {
                if (compileTimeView)
                    transitiveArchiveDeps(depth-1);
                else
                    transitiveDeps(depth-1);
            }

            Set<Archive> archives = archives();

            // analyze the dependencies collected
            analyzer.run(archives, finder.locationToArchive());

            if (writer != null) {
                writer.generateOutput(archives, analyzer);
            }
        } finally {
            finder.shutdown();
        }
        return true;
    }

    /**
     * Returns the archives for reporting that has matching dependences.
     *
     * If --require is set, they should be excluded.
     */
    Set<Archive> archives() {
        if (filter.requiresFilter().isEmpty()) {
            return archives.stream()
                .filter(this::include)
                .filter(Archive::hasDependences)
                .collect(Collectors.toSet());
        } else {
            // use the archives that have dependences and not specified in --require
            return archives.stream()
                .filter(this::include)
                .filter(source -> !filter.requiresFilter().contains(source.getName()))
                .filter(source ->
                        source.getDependencies()
                              .map(finder::locationToArchive)
                              .anyMatch(a -> a != source))
                .collect(Collectors.toSet());
        }
    }

    /**
     * Returns the dependences, either class name or package name
     * as specified in the given verbose level.
     */
    Set<String> dependences() {
        return analyzer.archives().stream()
                       .map(analyzer::dependences)
                       .flatMap(Set::stream)
                       .collect(Collectors.toSet());
    }

    /**
     * Returns the archives that contains the given locations and
     * not parsed and analyzed.
     */
    private Set<Archive> unresolvedArchives(Stream<Location> locations) {
        return locations.filter(l -> !finder.isParsed(l))
                        .distinct()
                        .map(configuration::findClass)
                        .flatMap(Optional::stream)
                        .collect(toSet());
    }

    /*
     * Recursively analyzes entire module/archives.
    */
    private void transitiveArchiveDeps(int depth) throws IOException {
        Stream<Location> deps = archives.stream()
                                        .flatMap(Archive::getDependencies);

        // start with the unresolved archives
        Set<Archive> unresolved = unresolvedArchives(deps);
        do {
            // parse all unresolved archives
            Set<Location> targets = apiOnly
                ? finder.parseExportedAPIs(unresolved.stream())
                : finder.parse(unresolved.stream());
            archives.addAll(unresolved);

            // Add dependencies to the next batch for analysis
            unresolved = unresolvedArchives(targets.stream());
        } while (!unresolved.isEmpty() && depth-- > 0);
    }

    /*
     * Recursively analyze the class dependences
     */
    private void transitiveDeps(int depth) throws IOException {
        Stream<Location> deps = archives.stream()
                                        .flatMap(Archive::getDependencies);

        Deque<Location> unresolved = deps.collect(Collectors.toCollection(LinkedList::new));
        ConcurrentLinkedDeque<Location> deque = new ConcurrentLinkedDeque<>();
        do {
            Location target;
            while ((target = unresolved.poll()) != null) {
                if (finder.isParsed(target))
                    continue;

                Archive archive = configuration.findClass(target).orElse(null);
                if (archive != null) {
                    archives.add(archive);

                    String name = target.getName();
                    Set<Location> targets = apiOnly
                            ? finder.parseExportedAPIs(archive, name)
                            : finder.parse(archive, name);

                    // build unresolved dependencies
                    targets.stream()
                           .filter(t -> !finder.isParsed(t))
                           .forEach(deque::add);
                }
            }
            unresolved = deque;
            deque = new ConcurrentLinkedDeque<>();
        } while (!unresolved.isEmpty() && depth-- > 0);
    }

    /*
     * Tests if the given archive is requested for analysis.
     * It includes the root modules specified in --module, --add-modules
     * or modules specified on the command line
     *
     * This filters system module by default unless they are explicitly
     * requested.
     */
    public boolean include(Archive source) {
        Module module = source.getModule();
        // skip system module by default
        return  !module.isSystem()
                    || configuration.rootModules().contains(source);
    }

    // ----- for testing purpose -----

    public static enum Info {
        REQUIRES,
        REQUIRES_TRANSITIVE,
        EXPORTED_API,
        MODULE_PRIVATE,
        QUALIFIED_EXPORTED_API,
        INTERNAL_API,
        JDK_INTERNAL_API,
        JDK_REMOVED_INTERNAL_API
    }

    public static class Node {
        public final String name;
        public final String source;
        public final Info info;
        Node(String name, Info info) {
            this(name, name, info);
        }
        Node(String name, String source, Info info) {
            this.name = name;
            this.source = source;
            this.info = info;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (info != Info.REQUIRES && info != Info.REQUIRES_TRANSITIVE)
                sb.append(source).append("/");

            sb.append(name);
            if (info == Info.QUALIFIED_EXPORTED_API)
                sb.append(" (qualified)");
            else if (info == Info.JDK_INTERNAL_API)
                sb.append(" (JDK internal)");
            else if (info == Info.INTERNAL_API)
                sb.append(" (internal)");
            return sb.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Node))
                return false;

            Node other = (Node)o;
            return this.name.equals(other.name) &&
                    this.source.equals(other.source) &&
                    this.info.equals(other.info);
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + source.hashCode();
            result = 31 * result + info.hashCode();
            return result;
        }
    }

    /**
     * Returns a graph of module dependences.
     *
     * Each Node represents a module and each edge is a dependence.
     * No analysis on "requires transitive".
     */
    public Graph<Node> moduleGraph() {
        Graph.Builder<Node> builder = new Graph.Builder<>();

        archives().stream()
            .forEach(m -> {
                Node u = new Node(m.getName(), Info.REQUIRES);
                builder.addNode(u);
                analyzer.requires(m)
                    .map(req -> new Node(req.getName(), Info.REQUIRES))
                    .forEach(v -> builder.addEdge(u, v));
            });
        return builder.build();
    }

    /**
     * Returns a graph of dependences.
     *
     * Each Node represents a class or package per the specified verbose level.
     * Each edge indicates
     */
    public Graph<Node> dependenceGraph() {
        Graph.Builder<Node> builder = new Graph.Builder<>();

        archives().stream()
            .map(analyzer.results::get)
            .filter(deps -> !deps.dependencies().isEmpty())
            .flatMap(deps -> deps.dependencies().stream())
            .forEach(d -> addEdge(builder, d));
        return builder.build();
    }

    private void addEdge(Graph.Builder<Node> builder, Analyzer.Dep dep) {
        Archive source = dep.originArchive();
        Archive target = dep.targetArchive();
        String pn = dep.target();
        if (verbose == CLASS || verbose == VERBOSE) {
            int i = dep.target().lastIndexOf('.');
            pn = i > 0 ? dep.target().substring(0, i) : "";
        }
        final Info info;
        Module targetModule = target.getModule();
        if (source == target) {
            info = Info.MODULE_PRIVATE;
        } else if (!targetModule.isNamed()) {
            info = Info.EXPORTED_API;
        } else if (targetModule.isExported(pn) && !targetModule.isJDKUnsupported()) {
            info = Info.EXPORTED_API;
        } else {
            Module module = target.getModule();
            if (module == Analyzer.REMOVED_JDK_INTERNALS) {
                info = Info.JDK_REMOVED_INTERNAL_API;
            } else if (!source.getModule().isJDK() && module.isJDK())
                info = Info.JDK_INTERNAL_API;
                // qualified exports or inaccessible
            else if (module.isExported(pn, source.getModule().name()))
                info = Info.QUALIFIED_EXPORTED_API;
            else
                info = Info.INTERNAL_API;
        }

        Node u = new Node(dep.origin(), source.getName(), info);
        Node v = new Node(dep.target(), target.getName(), info);
        builder.addEdge(u, v);
    }

}
