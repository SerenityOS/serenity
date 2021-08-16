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

package build.tools.symbolgenerator;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import javax.lang.model.element.ModuleElement.RequiresDirective;
import javax.lang.model.util.Elements;
import javax.tools.JavaCompiler;

import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.code.Source;
import com.sun.tools.javac.code.Symbol.ModuleSymbol;
import com.sun.tools.javac.jvm.Target;

/**
 * Write reflexive transitive closure of the given modules along their requires transitive edges into
 * file <version>/system-modules in the specified directory.
 */
public class TransitiveDependencies {

    private static void help() {
        System.err.println("java TransitiveDependencies <target-directory> <module-source-path> <root-modules>");
    }

    public static void main(String... args) throws IOException {
        if (args.length < 2) {
            help();
            return ;
        }

        JavaCompiler compiler = JavacTool.create();
        List<String> options = List.of("-source", Source.DEFAULT.name,
                                       "-target", Target.DEFAULT.name,
                                       "-proc:only",
                                       "--system", "none",
                                       "--module-source-path", args[1],
                                       "--add-modules", Arrays.stream(args)
                                                              .skip(2)
                                                              .collect(Collectors.joining(",")));
        List<String> jlObjectList = List.of("java.lang.Object");
        JavacTaskImpl task = (JavacTaskImpl) compiler.getTask(null, null, null, options, jlObjectList, null);
        task.enter();
        Elements elements = task.getElements();
        Deque<String> todo = new ArrayDeque<>();
        Arrays.stream(args).skip(2).forEach(todo::add);
        Set<String> allModules = new HashSet<>();

        while (!todo.isEmpty()) {
            String current = todo.removeFirst();

            if (!allModules.add(current))
                continue;

            ModuleSymbol mod = (ModuleSymbol) elements.getModuleElement(current);

            if (mod == null) {
                throw new IllegalStateException("Missing: " + current);
            }

             //use the internal structure to avoid unnecesarily completing the symbol using the UsesProvidesVisitor:
            for (RequiresDirective rd : mod.requires) {
                if (rd.isTransitive()) {
                    todo.offerLast(rd.getDependency().getQualifiedName().toString());
                }
            }
        }

        allModules.add("java.base");
        allModules.add("jdk.unsupported");

        Path targetFile = Paths.get(args[0]);

        Files.createDirectories(targetFile.getParent());

        try (Writer w = Files.newBufferedWriter(targetFile);
             PrintWriter out = new PrintWriter(w)) {
            allModules.stream()
                      .sorted()
                      .forEach(out::println);
        }
    }

}
