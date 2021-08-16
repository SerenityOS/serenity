/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8012929 8243074 8266281
 * @summary Trees.getElement should work not only for declaration trees, but also for use-trees
 * @modules jdk.compiler
 * @build TestGetElementReference
 * @run main TestGetElementReference
 */

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class TestGetElementReference {

    private static final String JDK_VERSION =
            Integer.toString(Runtime.getRuntime().version().feature());

    public static void main(String... args) throws IOException {
        analyze(false, "TestGetElementReferenceData.java");
        analyze(false, "mod/module-info.java", "mod/api/pkg/Api.java");
        analyze(true, "TestGetElementReferenceDataWithErrors.java");
    }

    private static void analyze(boolean allowErrors, String... fileNames) throws IOException {
        try (StandardJavaFileManager fm = ToolProvider.getSystemJavaCompiler().getStandardFileManager(null, null, null)) {
            List<JavaFileObject> files = new ArrayList<>();
            for (String fileName : fileNames) {
                File source = new File(System.getProperty("test.src", "."), fileName.replace('/', File.separatorChar)).getAbsoluteFile();
                for (JavaFileObject f : fm.getJavaFileObjects(source)) {
                    files.add(f);
                }
            }
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            List<String> options = List.of("-Xjcov");
            JavacTask ct = (JavacTask) ToolProvider.getSystemJavaCompiler().getTask(null, null, diagnostics, options, null, files);
            Trees trees = Trees.instance(ct);
            CompilationUnitTree cut = ct.parse().iterator().next();

            ct.analyze();

            if (!allowErrors) {
                for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                    if (d.getKind() == Diagnostic.Kind.ERROR) {
                        throw new IllegalStateException("Should have been attributed without errors: " + diagnostics.getDiagnostics());
                    }
                }
            }

            Pattern p = Pattern.compile("/\\*getElement:(.*?)\\*/");
            Matcher m = p.matcher(cut.getSourceFile().getCharContent(false));

            while (m.find()) {
                TreePath tp = pathFor(trees, cut, m.start() - 1);
                String expected = m.group(1);
                if (expected.startsWith("getParentPath:")) {
                    tp = tp.getParentPath();
                    expected = expected.substring("getParentPath:".length());
                }
                Element found = trees.getElement(tp);
                String actual = found != null ? found.getKind() + ":" + symbolToString(found) : "<null>";

                if (!expected.equals(actual)) {
                    throw new IllegalStateException("expected=" + expected + "; actual=" + actual + "; tree: " + tp.getLeaf());
                }
            }
        }
    }

    private static TreePath pathFor(final Trees trees, final CompilationUnitTree cut, final int pos) {
        final TreePath[] result = new TreePath[1];

        new TreePathScanner<Void, Void>() {
            @Override public Void scan(Tree node, Void p) {
                if (   node != null
                    && trees.getSourcePositions().getStartPosition(cut, node) <= pos
                    && pos <= trees.getSourcePositions().getEndPosition(cut, node)) {
                    result[0] = new TreePath(getCurrentPath(), node);
                    return super.scan(node, p);
                }
                return null;
            }
        }.scan(cut, null);

        return result[0];
    }

    private static String symbolToString(Element el) {
        switch (el.getKind()) {
            case METHOD: return symbolToString(el.getEnclosingElement()) + "." + el.toString();
            case CONSTRUCTOR: return symbolToString(el.getEnclosingElement().getEnclosingElement()) + "." + el.toString();
            default:
                return el.toString();
        }
    }

    static class TestFileObject extends SimpleJavaFileObject {
        private final String text;
        public TestFileObject(String text) {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            this.text = text;
        }
        @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return text;
        }
    }

}
