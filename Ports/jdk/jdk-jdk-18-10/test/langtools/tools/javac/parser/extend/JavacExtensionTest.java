/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.util.Context;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import javax.tools.StandardJavaFileManager;
import com.sun.source.tree.ImportTree;
import java.util.Collections;
import java.io.PrintWriter;
import com.sun.source.tree.VariableTree;
import static javax.tools.StandardLocation.CLASS_OUTPUT;

/*
 * @test
 * @bug 8067384 8068488
 * @summary Verify that JavacParser can be extended
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.parser
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 *          jdk.compiler/com.sun.tools.javac.resources
 */
public class JavacExtensionTest {

    public static void main(String[] args) throws Exception {
        PrintWriter pw = new PrintWriter("trialSource.java", "UTF-8");
        pw.println("int x = 9;");
        pw.close();
        List<? extends Tree> defs = parse("trialSource.java");
        if (defs.size() != 1) {
            throw new AssertionError("Expected only one def, got: " + defs.size());
        }
        Tree tree = defs.get(0);
        if (tree instanceof VariableTree) {
            System.out.println("Passes!  --- " + tree);
        } else {
            throw new AssertionError("Expected VariableTree, got: " + tree);
        }
    }

    static List<? extends Tree> parse(String srcfile) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        Iterable<? extends JavaFileObject> fileObjects = fileManager.getJavaFileObjects(srcfile);
        String classPath = System.getProperty("java.class.path");
        List<String> options = Arrays.asList("-classpath", classPath);
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        Context context = new Context();
        JavacTaskImpl task = (JavacTaskImpl) ((JavacTool) compiler).getTask(null, null,
                diagnostics, options, null, fileObjects, context);
        TrialParserFactory.instance(context);
        Iterable<? extends CompilationUnitTree> asts = task.parse();
        Iterator<? extends CompilationUnitTree> it = asts.iterator();
        if (it.hasNext()) {
            CompilationUnitTree cut = it.next();
            return cut.getTypeDecls();
        } else {
            throw new AssertionError("Expected compilation unit");
        }
    }
}
