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
package sampleapi.generator;

import com.sun.source.tree.ModuleTree.ModuleKind;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.parser.Scanner;
import com.sun.tools.javac.parser.ScannerFactory;
import com.sun.tools.javac.parser.Tokens;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCDirective;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.ListBuffer;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import sampleapi.SampleApi;
import sampleapi.util.PoorDocCommentTable;

import static com.sun.tools.javac.parser.Tokens.Comment.CommentStyle.JAVADOC;

/**
 *
 * This class is responsible for loading module description from an XML and then generating the
 * <code>module-info.java</code>. It is using {@link PackageGenerator} class for parsing content of the module.
 */
public class ModuleGenerator {

    private static final String UNNAMED = "UNNAMED";
    private static final String MODULE_INFO = "module-info.java";

    public String name;
    public String id;
    public ModuleKind kind;
    public final List<Exports> exportss = new ArrayList<>();
    public final List<Opens> openss = new ArrayList<>();
    public final List<Requires> requiress = new ArrayList<>();
    public final List<Uses> usess = new ArrayList<>();
    public final List<Provides> providess = new ArrayList<>();
    public final List<PackageGenerator> packages = new ArrayList<>();

    private ModuleGenerator() {
    }

    public static ModuleGenerator load(Element rootElement) {
        ModuleGenerator result = new ModuleGenerator();
        result.name = rootElement.getAttribute("name");
        result.id = rootElement.getAttribute("id");
        String kind = rootElement.getAttribute("kind");
        result.kind = kind.isEmpty() ? ModuleKind.STRONG :
                ModuleKind.valueOf(kind.toUpperCase());
        //exports
        NodeList exportsList = rootElement.getElementsByTagName("exports");
        for (int i = 0; i < exportsList.getLength(); i++) {
            Element exportsEl = (Element) exportsList.item(i);
            Exports exports = new Exports(exportsEl.getAttribute("package"));
            NodeList toList = exportsEl.getElementsByTagName("to");
            for (int j = 0; j < toList.getLength(); j++) {
                Element toElement = (Element) toList.item(j);
                exports.modules.add(toElement.getAttribute("module"));
            }
            result.exportss.add(exports);
        }
        //opens
        NodeList opensList = rootElement.getElementsByTagName("opens");
        for (int i = 0; i < opensList.getLength(); i++) {
            Element opensEl = (Element) opensList.item(i);
            Opens opens = new Opens(opensEl.getAttribute("package"));
            NodeList toList = opensEl.getElementsByTagName("to");
            for (int j = 0; j < toList.getLength(); j++) {
                Element toElement = (Element) toList.item(j);
                opens.modules.add(toElement.getAttribute("module"));
            }
            result.openss.add(opens);
        }
        //requires
        NodeList requiresList = rootElement.getElementsByTagName("requires");
        for (int i = 0; i < requiresList.getLength(); i++) {
            Element requiresEl = (Element) requiresList.item(i);
            result.requiress.add(new Requires(requiresEl.getAttribute("module"),
                    Boolean.parseBoolean(requiresEl.getAttribute("transitive")),
                    Boolean.parseBoolean(requiresEl.getAttribute("static"))));
        }
        //uses
        NodeList usesList = rootElement.getElementsByTagName("uses");
        for (int i = 0; i < usesList.getLength(); i++) {
            Element usesEl = (Element) usesList.item(i);
            result.usess.add(new Uses(usesEl.getAttribute("service")));
        }
        //provides
        NodeList providesList = rootElement.getElementsByTagName("provides");
        for (int i = 0; i < providesList.getLength(); i++) {
            Element providesEl = (Element) providesList.item(i);
            Provides provides = new Provides(providesEl.getAttribute("service"));
            NodeList implList = providesEl.getElementsByTagName("implementation");
            for (int j = 0; j < implList.getLength(); j++) {
                Element implElement = (Element) implList.item(j);
                provides.implementations.add(implElement.getAttribute("class"));
            }
            result.providess.add(provides);
        }
        //packages
        NodeList packagesList = rootElement.getElementsByTagName("package");
        for (int i = 0; i < packagesList.getLength(); i++) {
            result.packages.add(PackageGenerator
                    .processDataSet((Element) packagesList.item(i)));
        }
        return result;
    }

    public void generate(Path outDir, SampleApi api) throws IOException {
        Files.createDirectories(outDir);
        Path moduleSourceDir;
        if (!name.equals(UNNAMED)) {
            moduleSourceDir = outDir.resolve(name);
            Files.createDirectory(moduleSourceDir);
            generateModuleInfo(moduleSourceDir, api);
        } else {
            moduleSourceDir = outDir;
        }
        packages.forEach(pkg -> pkg.generate(moduleSourceDir));
    }

    private void generateModuleInfo(Path moduleSourceDir, SampleApi api) throws IOException {
        TreeMaker make = TreeMaker.instance(api.getContext());
        Names names = Names.instance(api.getContext());
        JCTree.JCExpression modQual = make.QualIdent(
                new Symbol.ModuleSymbol(names.fromString(name), null));
        ListBuffer<JCDirective> exportsBuffer = new ListBuffer<>();
        exportss.forEach(e -> {
            ListBuffer<JCExpression> modulesBuffer = new ListBuffer<>();
            e.modules.stream()
                    .map(m -> api.isId(m) ? api.moduleById(m).name : m)
                    .forEach(m -> {
                modulesBuffer.add(make.Ident(
                        names.fromString(m)));
            });
            exportsBuffer.add(make.Exports(
                    make.Ident(names.fromString(api.isId(e.pkg) ?
                            api.packageById(e.pkg).packageName : e.pkg)),
                    (modulesBuffer.size() > 0) ? modulesBuffer.toList() : null));
        });
        openss.forEach(o -> {
            ListBuffer<JCExpression> modulesBuffer = new ListBuffer<>();
            o.modules.stream()
                    .map(m -> api.isId(m) ? api.moduleById(m).name : m)
                    .forEach(m -> {
                modulesBuffer.add(make.Ident(
                        names.fromString(m)));
            });
            exportsBuffer.add(make.Opens(
                    make.Ident(names.fromString(api.isId(o.pkg) ?
                            api.packageById(o.pkg).packageName : o.pkg)),
                    (modulesBuffer.size() > 0) ? modulesBuffer.toList() : null));
        });
        ListBuffer<JCDirective> requiresBuffer = new ListBuffer<>();
        requiress.forEach(r -> requiresBuffer.add(make.Requires(
                r.transitive, r.statc,
                make.Ident(names.fromString(api.isId(r.module)
                        ? api.moduleById(r.module).name : r.module)))));
        ListBuffer<JCDirective> usesBuffer = new ListBuffer<>();
        usess.forEach(u -> usesBuffer
                .add(make.Uses(make.Ident(names.fromString(api.isId(u.service)
                        ? api.classById(u.service) : u.service)))));
        ListBuffer<JCDirective> providesBuffer = new ListBuffer<>();
        providess.forEach(p -> {
            ListBuffer<JCExpression> implementationsBuffer = new ListBuffer<>();
            p.implementations.stream()
                    .map(c -> api.isId(c) ? api.classById(c) : c)
                    .forEach(i -> {
                implementationsBuffer.add(make.Ident(names.fromString(i)));
            });
            providesBuffer.add(make.Provides(
                make.Ident(names.fromString(api.isId(p.service) ?
                        api.classById(p.service) : p.service)),
                implementationsBuffer.toList()));
        });
        ListBuffer<JCDirective> fullList = new ListBuffer<>();
        fullList.addAll(exportsBuffer.toList());
        fullList.addAll(requiresBuffer.toList());
        fullList.addAll(usesBuffer.toList());
        fullList.addAll(providesBuffer.toList());
        JCTree.JCModuleDecl mod = make.ModuleDef(
                make.Modifiers(0), //TODO how to support this?
                kind, modQual, fullList.toList());
        ListBuffer<JCTree> top = new ListBuffer<>();
        top.add(mod);
        JCTree.JCCompilationUnit compilationUnit = make.TopLevel(top.toList());
        try (OutputStream module_info = Files.newOutputStream(moduleSourceDir.resolve(MODULE_INFO))) {
            module_info.write(compilationUnit.toString().getBytes());
        }
    }


    public static class Requires {
        public String module;
        public boolean transitive;
        public boolean statc;

        private Requires(String module, boolean transitive, boolean statc) {
            this.module = module;
            this.transitive = transitive;
            this.statc = this.statc;
        }
    }

    public static class Exports {
        public final String pkg;
        public final List<String> modules = new ArrayList<>();

        private Exports(String pkg) {
            this.pkg = pkg;
        }
    }

    public static class Opens {
        public final String pkg;
        public final List<String> modules = new ArrayList<>();

        private Opens(String pkg) {
            this.pkg = pkg;
        }
    }

    public static class Uses {
        public final String service;

        private Uses(String service) {
            this.service = service;
        }
    }

    public static class Provides {
        public final String service;
        public final List<String> implementations = new ArrayList<>();

        private Provides(String service) {
            this.service = service;
        }
    }
}
