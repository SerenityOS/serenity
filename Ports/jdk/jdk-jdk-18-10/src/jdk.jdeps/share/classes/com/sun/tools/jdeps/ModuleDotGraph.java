/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

import static java.lang.module.ModuleDescriptor.Requires.Modifier.*;
import static java.util.stream.Collectors.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.*;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.module.ResolvedModule;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generate dot graph for modules
 */
public class ModuleDotGraph {
    private final JdepsConfiguration config;
    private final Map<String, Configuration> configurations;
    private final boolean apiOnly;
    public ModuleDotGraph(JdepsConfiguration config, boolean apiOnly) {
        this(config,
             config.rootModules().stream()
                   .map(Module::name)
                   .sorted()
                   .collect(toMap(Function.identity(), mn -> config.resolve(Set.of(mn)))),
             apiOnly);
    }

    public ModuleDotGraph(Map<String, Configuration> configurations, boolean apiOnly) {
        this(null, configurations, apiOnly);
    }

    private ModuleDotGraph(JdepsConfiguration config,
                           Map<String, Configuration> configurations,
                           boolean apiOnly) {
        this.configurations = configurations;
        this.apiOnly = apiOnly;
        this.config = config;
    }

    /**
     * Generate dotfile for all modules
     *
     * @param dir output directory
     */
    public boolean genDotFiles(Path dir) throws IOException {
        return genDotFiles(dir, DotGraphAttributes.DEFAULT);
    }

    public boolean genDotFiles(Path dir, Attributes attributes)
        throws IOException
    {
        Files.createDirectories(dir);
        for (String mn : configurations.keySet()) {
            Path path = dir.resolve(toDotFileBaseName(mn) + ".dot");
            genDotFile(path, mn, configurations.get(mn), attributes);
        }
        return true;
    }

    private String toDotFileBaseName(String mn) {
        if (config == null)
            return mn;

        Optional<Path> path = config.findModule(mn).flatMap(Module::path);
        if (path.isPresent())
            return path.get().getFileName().toString();
        else
            return mn;
    }
    /**
     * Generate dotfile of the given path
     */
    public void genDotFile(Path path, String name,
                           Configuration configuration,
                           Attributes attributes)
        throws IOException
    {
        // transitive reduction
        Graph<String> graph = apiOnly
                ? requiresTransitiveGraph(configuration, Set.of(name))
                : gengraph(configuration);

        DotGraphBuilder builder = new DotGraphBuilder(name, graph, attributes);
        builder.subgraph("se", "java", attributes.javaSubgraphColor(),
                         DotGraphBuilder.JAVA_SE_SUBGRAPH)
               .subgraph("jdk", "jdk", attributes.jdkSubgraphColor(),
                         DotGraphBuilder.JDK_SUBGRAPH)
               .modules(graph.nodes().stream()
                                 .map(mn -> configuration.findModule(mn).get()
                                                .reference().descriptor()));
        // build dot file
        builder.build(path);
    }

    /**
     * Returns a Graph of the given Configuration after transitive reduction.
     *
     * Transitive reduction of requires transitive edge and requires edge have
     * to be applied separately to prevent the requires transitive edges
     * (e.g. U -> V) from being reduced by a path (U -> X -> Y -> V)
     * in which  V would not be re-exported from U.
     */
    private Graph<String> gengraph(Configuration cf) {
        Graph.Builder<String> builder = new Graph.Builder<>();
        cf.modules().stream()
            .forEach(rm -> {
                String mn = rm.name();
                builder.addNode(mn);
                rm.reads().stream()
                  .map(ResolvedModule::name)
                  .forEach(target -> builder.addEdge(mn, target));
            });

        Graph<String> rpg = requiresTransitiveGraph(cf, builder.nodes);
        return builder.build().reduce(rpg);
    }


    /**
     * Returns a Graph containing only requires transitive edges
     * with transitive reduction.
     */
    public Graph<String> requiresTransitiveGraph(Configuration cf,
                                                 Set<String> roots)
    {
        Deque<String> deque = new ArrayDeque<>(roots);
        Set<String> visited = new HashSet<>();
        Graph.Builder<String> builder = new Graph.Builder<>();

        while (deque.peek() != null) {
            String mn = deque.pop();
            if (visited.contains(mn))
                continue;

            visited.add(mn);
            builder.addNode(mn);
            cf.findModule(mn).get()
              .reference().descriptor().requires().stream()
              .filter(d -> d.modifiers().contains(TRANSITIVE)
                                || d.name().equals("java.base"))
              .map(Requires::name)
              .forEach(d -> {
                  deque.add(d);
                  builder.addEdge(mn, d);
              });
        }

        return builder.build().reduce();
    }

    public interface Attributes {
        static final String ORANGE = "#e76f00";
        static final String BLUE = "#437291";
        static final String BLACK = "#000000";
        static final String DARK_GRAY = "#999999";
        static final String LIGHT_GRAY = "#dddddd";

        int fontSize();
        String fontName();
        String fontColor();

        int arrowSize();
        int arrowWidth();
        String arrowColor();

        default double rankSep() {
            return 1;
        }

        default List<Set<String>> ranks() {
            return Collections.emptyList();
        }

        default int weightOf(String s, String t) {
            return 1;
        }

        default String requiresMandatedColor() {
            return LIGHT_GRAY;
        }

        default String javaSubgraphColor() {
            return ORANGE;
        }

        default String jdkSubgraphColor() {
            return BLUE;
        }
    }

    static class DotGraphAttributes implements Attributes {
        static final DotGraphAttributes DEFAULT = new DotGraphAttributes();

        static final String FONT_NAME = "DejaVuSans";
        static final int FONT_SIZE = 12;
        static final int ARROW_SIZE = 1;
        static final int ARROW_WIDTH = 2;

        @Override
        public int fontSize() {
            return FONT_SIZE;
        }

        @Override
        public String fontName() {
            return FONT_NAME;
        }

        @Override
        public String fontColor() {
            return BLACK;
        }

        @Override
        public int arrowSize() {
            return ARROW_SIZE;
        }

        @Override
        public int arrowWidth() {
            return ARROW_WIDTH;
        }

        @Override
        public String arrowColor() {
            return DARK_GRAY;
        }
    }

    private static class DotGraphBuilder {
        static final String REEXPORTS = "";
        static final String REQUIRES = "style=\"dashed\"";

        static final Set<String> JAVA_SE_SUBGRAPH = javaSE();
        static final Set<String> JDK_SUBGRAPH = jdk();

        private static Set<String> javaSE() {
            String root = "java.se";
            ModuleFinder system = ModuleFinder.ofSystem();
            if (system.find(root).isPresent()) {
                return Stream.concat(Stream.of(root),
                                     Configuration.empty().resolve(system,
                                                                   ModuleFinder.of(),
                                                                   Set.of(root))
                                                  .findModule(root).get()
                                                  .reads().stream()
                                                  .map(ResolvedModule::name))
                             .collect(toSet());
            } else {
                // approximation
                return system.findAll().stream()
                    .map(ModuleReference::descriptor)
                    .map(ModuleDescriptor::name)
                    .filter(name -> name.startsWith("java.") &&
                                        !name.equals("java.smartcardio"))
                    .collect(Collectors.toSet());
            }
        }

        private static Set<String> jdk() {
            return ModuleFinder.ofSystem().findAll().stream()
                    .map(ModuleReference::descriptor)
                    .map(ModuleDescriptor::name)
                    .filter(name -> !JAVA_SE_SUBGRAPH.contains(name) &&
                                        (name.startsWith("java.") || name.startsWith("jdk.")))
                    .collect(Collectors.toSet());
        }

        static class SubGraph {
            final String name;
            final String group;
            final String color;
            final Set<String> nodes;
            SubGraph(String name, String group, String color, Set<String> nodes) {
                this.name = Objects.requireNonNull(name);
                this.group = Objects.requireNonNull(group);
                this.color = Objects.requireNonNull(color);
                this.nodes = Objects.requireNonNull(nodes);
            }
        }

        private final String name;
        private final Graph<String> graph;
        private final Set<ModuleDescriptor> descriptors = new TreeSet<>();
        private final List<SubGraph> subgraphs = new ArrayList<>();
        private final Attributes attributes;
        public DotGraphBuilder(String name,
                               Graph<String> graph,
                               Attributes attributes) {
            this.name = name;
            this.graph = graph;
            this.attributes = attributes;
        }

        public DotGraphBuilder modules(Stream<ModuleDescriptor> descriptors) {
            descriptors.forEach(this.descriptors::add);
            return this;
        }

        public void build(Path filename) throws IOException {
            try (BufferedWriter writer = Files.newBufferedWriter(filename);
                 PrintWriter out = new PrintWriter(writer)) {

                out.format("digraph \"%s\" {%n", name);
                out.format("  nodesep=.5;%n");
                out.format((Locale)null, "  ranksep=%f;%n", attributes.rankSep());
                out.format("  pencolor=transparent;%n");
                out.format("  node [shape=plaintext, fontcolor=\"%s\", fontname=\"%s\","
                                + " fontsize=%d, margin=\".2,.2\"];%n",
                           attributes.fontColor(),
                           attributes.fontName(),
                           attributes.fontSize());
                out.format("  edge [penwidth=%d, color=\"%s\", arrowhead=open, arrowsize=%d];%n",
                           attributes.arrowWidth(),
                           attributes.arrowColor(),
                           attributes.arrowSize());

                // same RANKS
                attributes.ranks().stream()
                    .map(nodes -> descriptors.stream()
                                        .map(ModuleDescriptor::name)
                                        .filter(nodes::contains)
                                        .map(mn -> "\"" + mn + "\"")
                                        .collect(joining(",")))
                    .filter(group -> group.length() > 0)
                    .forEach(group -> out.format("  {rank=same %s}%n", group));

                subgraphs.forEach(subgraph -> {
                    out.format("  subgraph %s {%n", subgraph.name);
                    descriptors.stream()
                        .map(ModuleDescriptor::name)
                        .filter(subgraph.nodes::contains)
                        .forEach(mn -> printNode(out, mn, subgraph.color, subgraph.group));
                    out.format("  }%n");
                });

                descriptors.stream()
                    .filter(md -> graph.contains(md.name()) &&
                                    !graph.adjacentNodes(md.name()).isEmpty())
                    .forEach(md -> printNode(out, md, graph.adjacentNodes(md.name())));

                out.println("}");
            }
        }

        public DotGraphBuilder subgraph(String name, String group, String color,
                                 Set<String> nodes) {
            subgraphs.add(new SubGraph(name, group, color, nodes));
            return this;
        }

        public void printNode(PrintWriter out, String node, String color, String group) {
            out.format("  \"%s\" [fontcolor=\"%s\", group=%s];%n",
                       node, color, group);
        }

        public void printNode(PrintWriter out, ModuleDescriptor md, Set<String> edges) {
            Set<String> requiresTransitive = md.requires().stream()
                .filter(d -> d.modifiers().contains(TRANSITIVE))
                .map(d -> d.name())
                .collect(toSet());

            String mn = md.name();
            edges.stream().forEach(dn -> {
                String attr;
                if (dn.equals("java.base")) {
                    attr = "color=\"" + attributes.requiresMandatedColor() + "\"";
                } else {
                    attr = (requiresTransitive.contains(dn) ? REEXPORTS : REQUIRES);
                }

                int w = attributes.weightOf(mn, dn);
                if (w > 1) {
                    if (!attr.isEmpty())
                        attr += ", ";

                    attr += "weight=" + w;
                }
                out.format("  \"%s\" -> \"%s\" [%s];%n", mn, dn, attr);
            });
        }

    }
}
