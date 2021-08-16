/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug     4994049
 * @summary Unit test for SourcePosition.column with respect to tab expansion
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @run main T4994049 FileWithTabs.java
 */

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.util.DocTrees;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.Reporter;
import jdk.javadoc.doclet.DocletEnvironment;

import static jdk.javadoc.internal.tool.Main.execute;

public class T4994049 implements Doclet {

    public boolean run(DocletEnvironment root) {
        DocTrees trees = root.getDocTrees();

        SourcePositions sourcePositions = trees.getSourcePositions();
        for (TypeElement klass : ElementFilter.typesIn(root.getIncludedElements())) {
            for (ExecutableElement method : getMethods(klass)) {
                if (method.getSimpleName().toString().equals("tabbedMethod")) {
                    TreePath path = trees.getPath(method);
                    CompilationUnitTree cu = path.getCompilationUnit();
                    long pos = sourcePositions.getStartPosition(cu, path.getLeaf());
                    LineMap lineMap = cu.getLineMap();
                    long columnNumber = lineMap.getColumnNumber(pos);
                    if (columnNumber == 9) {
                        System.out.println(columnNumber + ": OK!");
                        return true;
                    } else {
                        System.err.println(columnNumber + ": wrong tab expansion");
                        return false;
                    }
                }
            }
        }
        return false;
    }

    public static void main(String... args) throws Exception {
        File testSrc = new File(System.getProperty("test.src"));
        File tmpSrc = new File("tmpSrc");
        initTabs(testSrc, tmpSrc);


        for (String file : args) {
            File source = new File(tmpSrc, file);
            String[] array = {
                "-docletpath", System.getProperty("test.classes", "."),
                "-doclet", "T4994049",
                source.getPath()
            };
            int rc = execute(array);
            if (rc != 0)
                throw new Error("Unexpected return code from javadoc: " + rc);
        }
    }

    static void initTabs(File from, File to) throws IOException {
        for (File f: from.listFiles()) {
            File t = new File(to, f.getName());
            if (f.isDirectory()) {
                initTabs(f, t);
            } else if (f.getName().endsWith(".java")) {
                write(t, read(f).replace("\\t", "\t"));
            }
        }
    }

    static String read(File f) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    static void write(File f, String s) throws IOException {
        f.getParentFile().mkdirs();
        try (Writer out = new FileWriter(f)) {
            out.write(s);
        }
    }

    List<ExecutableElement> getMethods(TypeElement klass) {
        List<ExecutableElement> methods = new ArrayList<>();
        klass.getEnclosedElements()
                .stream()
                .filter((e) -> (e.getKind() == ElementKind.METHOD))
                .forEach((e) -> {
                    methods.add((ExecutableElement) e);
                });
        return methods;
    }

    @Override
    public String getName() {
        return "Test";
    }

    @Override
    public Set<Option> getSupportedOptions() {
        return Collections.emptySet();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public void init(Locale locale, Reporter reporter) {
        return;
    }
}
