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

import java.io.PrintWriter;
import java.lang.module.ModuleDescriptor;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.lang.module.ModuleDescriptor.Requires.Modifier.*;
import static com.sun.tools.jdeps.Module.*;

/**
 * A builder to create a Graph<Module>
 */
public class ModuleGraphBuilder extends Graph.Builder<Module> {
    final JdepsConfiguration config;

    ModuleGraphBuilder(JdepsConfiguration config) {
        this.config = config;
    }

    /**
     * Adds a module to the graph.
     */
    ModuleGraphBuilder addModule(Module module) {
        addNode(module);
        return this;
    }

    /**
     * Apply transitive reduction on the resulting graph
     */
    public Graph<Module> reduced() {
        Graph<Module> graph = build();
        // transitive reduction
        Graph<Module> newGraph = buildGraph(graph.edges()).reduce();

        if (DEBUG) {
            PrintWriter log = new PrintWriter(System.err);
            System.err.println("before transitive reduction: ");
            graph.printGraph(log);
            System.err.println("after transitive reduction: ");
            newGraph.printGraph(log);
        }

        return newGraph;
    }

    public Graph<Module> buildGraph() {
        Graph<Module> graph = build();
        return buildGraph(graph.edges());
    }

    /**
     * Build a graph of module from the given dependences.
     *
     * It transitively includes all implied read edges.
     */
    private Graph<Module> buildGraph(Map<Module, Set<Module>> edges) {
        Graph.Builder<Module> builder = new Graph.Builder<>();
        Set<Module> visited = new HashSet<>();
        Deque<Module> deque = new LinkedList<>();
        edges.entrySet().stream().forEach(e -> {
            Module m = e.getKey();
            deque.add(m);
            e.getValue().stream().forEach(v -> {
                deque.add(v);
                builder.addEdge(m, v);
            });
        });

        // read requires transitive from ModuleDescriptor
        Module source;
        while ((source = deque.poll()) != null) {
            if (visited.contains(source))
                continue;

            visited.add(source);
            builder.addNode(source);
            Module from = source;
            requiresTransitive(from).forEach(m -> {
                deque.add(m);
                builder.addEdge(from, m);
            });
        }
        return builder.build();
    }

    /*
     * Returns a stream of modules upon which the given module `requires transitive`
     */
    public Stream<Module> requiresTransitive(Module m) {
        // find requires transitive
        return m.descriptor()
                .requires().stream()
                .filter(req -> req.modifiers().contains(TRANSITIVE))
                .map(ModuleDescriptor.Requires::name)
                .map(config::findModule)
                .flatMap(Optional::stream);
    }
}
