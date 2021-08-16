/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

package toolbox;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builder for module declarations.
 */
public class ModuleBuilder {

    private final ToolBox tb;
    private final String name;
    private String comment = "";
    private boolean open;
    private List<String> requires = new ArrayList<>();
    private List<String> exports = new ArrayList<>();
    private List<String> opens = new ArrayList<>();
    private List<String> uses = new ArrayList<>();
    private List<String> provides = new ArrayList<>();
    private List<String> content = new ArrayList<>();
    private Set<Path> modulePath = new LinkedHashSet<>();

    /**
     * Creates a builder for a module.
     * @param tb a Toolbox that can be used to compile the module declaration
     * @param name the name of the module to be built
     */
    public ModuleBuilder(ToolBox tb, String name) {
        this(tb, false, name);
    }

    /**
     * Creates a builder for a module.
     * @param tb a Toolbox that can be used to compile the module declaration
     * @param open whether or not this is an open module
     * @param name the name of the module to be built
     */
    public ModuleBuilder(ToolBox tb, boolean open, String name) {
        this.tb = tb;
        this.open = open;
        this.name = name;
    }

    /**
     * Sets the doc comment for the declaration.
     * @param comment the content of the comment, excluding the initial
     *  '/**', leading whitespace and asterisks, and the final trailing '&#02a;/'.
     * @return this builder
     */
    public ModuleBuilder comment(String comment) {
        this.comment = comment;
        return this;
    }

    /**
     * Adds a "requires" directive to the declaration.
     * @param module the name of the module that is required
     * @param modulePath a path in while to locate the modules
     *    if the declaration is compiled
     * @return this builder
     */
    public ModuleBuilder requires(String module, Path... modulePath) {
        addDirective(requires, "requires " + module + ";");
        this.modulePath.addAll(Arrays.asList(modulePath));
        return this;

    }

    /**
     * Adds a "requires static" directive to the declaration.
     * @param module the name of the module that is required
     * @param modulePath a path in which to locate the modules
     *    if the declaration is compiled
     * @return this builder
     */
    public ModuleBuilder requiresStatic(String module, Path... modulePath) {
        addDirective(requires, "requires static " + module + ";");
        this.modulePath.addAll(Arrays.asList(modulePath));
        return this;
    }

    /**
     * Adds a "requires transitive" directive to the declaration.
     * @param module the name of the module that is required
     * @param modulePath a path in which to locate the modules
     *    if the declaration is compiled
     * @return this builder
     */
    public ModuleBuilder requiresTransitive(String module, Path... modulePath) {
        addDirective(requires, "requires transitive " + module + ";");
        this.modulePath.addAll(Arrays.asList(modulePath));
        return this;
    }

    /**
     * Adds a "requires static transitive" directive to the declaration.
     * @param module the name of the module that is required
     * @param modulePath a path in which to locate the modules
     *    if the declaration is compiled
     * @return this builder
     */
    public ModuleBuilder requiresStaticTransitive(String module, Path... modulePath) {
        addDirective(requires, "requires static transitive " + module + ";");
        this.modulePath.addAll(Arrays.asList(modulePath));
        return this;
    }

    /**
     * Adds an unqualified "exports" directive to the declaration.
     * @param pkg the name of the package to be exported
     * @return this builder
     */
    public ModuleBuilder exports(String pkg) {
        return addDirective(exports, "exports " + pkg + ";");
    }

    /**
     * Adds a qualified "exports" directive to the declaration.
     * @param pkg the name of the package to be exported
     * @param module the name of the module to which it is to be exported
     * @return this builder
     */
    public ModuleBuilder exportsTo(String pkg, String module) {
        return addDirective(exports, "exports " + pkg + " to " + module + ";");
    }

    /**
     * Adds an unqualified "opens" directive to the declaration.
     * @param pkg the name of the package to be opened
     * @return this builder
     */
    public ModuleBuilder opens(String pkg) {
        return addDirective(opens, "opens " + pkg + ";");
    }

    /**
     * Adds a qualified "opens" directive to the declaration.
     * @param pkg the name of the package to be opened
     * @param module the name of the module to which it is to be opened
     * @return this builder
     */
    public ModuleBuilder opensTo(String pkg, String module) {
        return addDirective(opens, "opens " + pkg + " to " + module + ";");
    }

    /**
     * Adds a "uses" directive to the declaration.
     * @param service the name of the service type
     * @return this builder
     */
    public ModuleBuilder uses(String service) {
        return addDirective(uses, "uses " + service + ";");
    }

    /**
     * Adds a "provides" directive to the declaration.
     * @param service the name of the service type
     * @param implementation the name of the implementation type
     * @return this builder
     */
    public ModuleBuilder provides(String service, String implementation) {
        return addDirective(provides, "provides " + service + " with " + implementation + ";");
    }

    private ModuleBuilder addDirective(List<String> directives, String directive) {
        directives.add(directive);
        return this;
    }

    /**
     * Adds type definitions to the module.
     * @param content a series of strings, each representing the content of
     *  a compilation unit to be included with the module
     * @return this builder
     */
    public ModuleBuilder classes(String... content) {
        this.content.addAll(Arrays.asList(content));
        return this;
    }

    /**
     * Writes the module declaration and associated additional compilation
     * units to a module directory within a given directory.
     * @param srcDir the directory in which a directory will be created
     *  to contain the source files for the module
     * @return the directory containing the source files for the module
     */
    public Path write(Path srcDir) throws IOException {
        Files.createDirectories(srcDir);
        List<String> sources = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        if (!comment.isEmpty()) {
            sb.append("/**\n * ")
                    .append(comment.replace("\n", "\n * "))
                    .append("\n */\n");
        }
        if (open) {
            sb.append("open ");
        }
        sb.append("module ").append(name).append(" {\n");
        requires.forEach(r -> sb.append("    " + r + "\n"));
        exports.forEach(e -> sb.append("    " + e + "\n"));
        opens.forEach(o -> sb.append("    " + o + "\n"));
        uses.forEach(u -> sb.append("    " + u + "\n"));
        provides.forEach(p -> sb.append("    " + p + "\n"));
        sb.append("}");
        sources.add(sb.toString());
        sources.addAll(content);
        Path moduleSrc = srcDir.resolve(name);
        tb.writeJavaFiles(moduleSrc, sources.toArray(new String[]{}));
        return moduleSrc;
    }

    /**
     * Writes the source files for the module to an interim directory,
     * and then compiles them to a given directory.
     * @param modules the directory in which a directory will be created
     *    to contain the compiled class files for the module
     * @throws IOException if an error occurs while compiling the files
     */
    public void build(Path modules) throws IOException {
        build(Paths.get(modules + "Src"), modules);
    }

    /**
     * Writes the source files for the module to a specified directory,
     * and then compiles them to a given directory.
     * @param srcDir the directory in which a directory will be created
     *  to contain the source files for the module
     * @param modules the directory in which a directory will be created
     *    to contain the compiled class files for the module
     * @throws IOException if an error occurs while compiling the files
     */
    public void build(Path src, Path modules) throws IOException {
        Path moduleSrc = write(src);
        String mp = modulePath.stream()
                .map(Path::toString)
                .collect(Collectors.joining(File.pathSeparator));
        new JavacTask(tb)
                .outdir(Files.createDirectories(modules.resolve(name)))
                .options("--module-path", mp)
                .files(tb.findJavaFiles(moduleSrc))
                .run()
                .writeAll();
    }
}
