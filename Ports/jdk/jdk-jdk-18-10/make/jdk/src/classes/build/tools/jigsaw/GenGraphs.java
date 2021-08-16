/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

package build.tools.jigsaw;

import com.sun.tools.jdeps.ModuleDotGraph;

import java.io.IOException;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Generate the DOT file for a module graph for each module in the JDK
 * after transitive reduction.
 */
public class GenGraphs {

    public static void main(String[] args) throws Exception {
        Path dir = null;
        boolean spec = false;
        Properties props = null;
        for (int i=0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--spec")) {
                spec = true;
            } else if (arg.equals("--dot-attributes")) {
                if (i++ == args.length) {
                    throw new IllegalArgumentException("Missing argument: --dot-attributes option");
                }
                props = new Properties();
                props.load(Files.newInputStream(Paths.get(args[i])));
            } else if (arg.equals("--output")) {
                dir = ++i < args.length ? Paths.get(args[i]) : null;
            } else if (arg.startsWith("-")) {
                throw new IllegalArgumentException("Invalid option: " + arg);
            }
        }

        if (dir == null) {
            System.err.println("ERROR: must specify --output argument");
            System.exit(1);
        }

        Files.createDirectories(dir);
        ModuleGraphAttributes attributes;
        if (props != null) {
            attributes = new ModuleGraphAttributes(props);
        } else {
            attributes = new ModuleGraphAttributes();
        }
        GenGraphs genGraphs = new GenGraphs(dir, spec, attributes);

        // print dot file for each module
        Map<String, Configuration> configurations = new HashMap<>();
        Set<String> modules = new HashSet<>();
        ModuleFinder finder = ModuleFinder.ofSystem();
        for (ModuleReference mref : finder.findAll()) {
            String name = (mref.descriptor().name());
            modules.add(name);
            if (genGraphs.accept(name, mref.descriptor())) {
                configurations.put(name, Configuration.empty()
                                                      .resolve(finder,
                                                               ModuleFinder.of(),
                                                               Set.of(name)));
            }
        }

        if (genGraphs.accept("jdk", null)) {
            // print a graph of all JDK modules
            configurations.put("jdk", Configuration.empty()
                                                   .resolve(finder,
                                                            ModuleFinder.of(),
                                                            modules));
        }

        genGraphs.genDotFiles(configurations);
    }

    /**
     * Custom dot file attributes.
     */
    static class ModuleGraphAttributes implements ModuleDotGraph.Attributes {
        static Map<String, String> DEFAULT_ATTRIBUTES = Map.of(
            "ranksep", "0.6",
            "fontsize", "12",
            "fontcolor", BLACK,
            "fontname", "DejaVuSans",
            "arrowsize", "1",
            "arrowwidth", "2",
            "arrowcolor", DARK_GRAY,
            // custom
            "requiresMandatedColor", LIGHT_GRAY,
            "javaSubgraphColor", ORANGE,
            "jdkSubgraphColor", BLUE
        );

        final Map<String, Integer> weights = new HashMap<>();
        final List<Set<String>> ranks = new ArrayList<>();
        final Map<String, String> attrs;
        ModuleGraphAttributes(Map<String, String> attrs) {
            int h = 1000;
            weight("java.se", "java.sql.rowset", h * 10);
            weight("java.sql.rowset", "java.sql", h * 10);
            weight("java.sql", "java.xml", h * 10);
            weight("java.xml", "java.base", h * 10);

            ranks.add(Set.of("java.logging", "java.scripting", "java.xml"));
            ranks.add(Set.of("java.sql"));
            ranks.add(Set.of("java.transaction.xa"));
            ranks.add(Set.of("java.compiler", "java.instrument"));
            ranks.add(Set.of("java.desktop", "java.management"));

            this.attrs = attrs;
        }

        ModuleGraphAttributes() {
            this(DEFAULT_ATTRIBUTES);
        }
        ModuleGraphAttributes(Properties props) {
            this(toAttributes(props));
        }

        @Override
        public double rankSep() {
            return Double.valueOf(attrs.get("ranksep"));
        }

        @Override
        public int fontSize() {
            return Integer.valueOf(attrs.get("fontsize"));
        }

        @Override
        public String fontName() {
            return attrs.get("fontname");
        }

        @Override
        public String fontColor() {
            return attrs.get("fontcolor");
        }

        @Override
        public int arrowSize() {
            return Integer.valueOf(attrs.get("arrowsize"));
        }

        @Override
        public int arrowWidth() {
            return Integer.valueOf(attrs.get("arrowwidth"));
        }

        @Override
        public String arrowColor() {
            return attrs.get("arrowcolor");
        }

        @Override
        public List<Set<String>> ranks() {
            return ranks;
        }

        @Override
        public String requiresMandatedColor() {
            return attrs.get("requiresMandatedColor");
        }

        @Override
        public String javaSubgraphColor() {
            return attrs.get("javaSubgraphColor");
        }

        @Override
        public String jdkSubgraphColor() {
            return attrs.get("jdkSubgraphColor");
        }

        @Override
        public int weightOf(String s, String t) {
            int w = weights.getOrDefault(s + ":" + t, 1);
            if (w != 1)
                return w;
            if (s.startsWith("java.") && t.startsWith("java."))
                return 10;
            return 1;
        }

        public void weight(String s, String t, int w) {
            weights.put(s + ":" + t, w);
        }

        static Map<String, String> toAttributes(Properties props) {
            return DEFAULT_ATTRIBUTES.keySet().stream()
                .collect(Collectors.toMap(Function.identity(),
                    k -> props.getProperty(k, DEFAULT_ATTRIBUTES.get(k))));
        }
    }

    private final Path dir;
    private final boolean spec;
    private final ModuleGraphAttributes attributes;
    GenGraphs(Path dir, boolean spec, ModuleGraphAttributes attributes) {
        this.dir = dir;
        this.spec = spec;
        this.attributes = attributes;
    }

    void genDotFiles(Map<String, Configuration> configurations) throws IOException {
        ModuleDotGraph dotGraph = new ModuleDotGraph(configurations, spec);
        dotGraph.genDotFiles(dir, attributes);
    }

    /**
     * Returns true for any name if generating graph for non-spec;
     * otherwise, returns true except "jdk" and name with "jdk.internal." prefix
     */
    boolean accept(String name, ModuleDescriptor descriptor) {
        if (!spec)
            return true;

        return !name.equals("jdk") && !name.startsWith("jdk.internal.");
    }
}
