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

/*
 * @test 8152771
 * @summary test tree access of module declarations
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 *      jdk.jdeps/com.sun.tools.javap
 * @build toolbox.ToolBox ModuleTestBase
 * @run main ModuleInfoTreeAccess
 */

import java.nio.file.Path;

import javax.lang.model.element.ModuleElement;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.JavacTrees;


public class ModuleInfoTreeAccess extends ModuleTestBase {
    public static void main(String... args) throws Exception {
        ModuleInfoTreeAccess t = new ModuleInfoTreeAccess();
        t.runTests();
    }

    private void assertNotNull(String prefix, Object actual) {
        if (actual == null) {
            throw new AssertionError(prefix + ": unexpected null! ");
        }
    }

    @Test
    public void testTreePathForModuleDecl(Path base) throws Exception {

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            Path src = base.resolve("src");
            tb.writeJavaFiles(src, "/** Test module */ module m1x {}");

            Iterable<? extends JavaFileObject> files = fm.getJavaFileObjects(findJavaFiles(src));
            JavacTask task = (JavacTask) compiler.getTask(null, fm, null, null, null, files);

            task.analyze();
            JavacTrees trees = JavacTrees.instance(task);
            ModuleElement mdle = (ModuleElement) task.getElements().getModuleElement("m1x");

            TreePath path = trees.getPath(mdle);
            assertNotNull("path", path);

            ModuleElement mdle1 = (ModuleElement) trees.getElement(path);
            assertNotNull("mdle1", mdle1);

            DocCommentTree docCommentTree = trees.getDocCommentTree(mdle);
            assertNotNull("docCommentTree", docCommentTree);
        }
    }

    @Test
    public void testTreePathForModuleDeclWithImport(Path base) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            Path src = base.resolve("src");
            tb.writeJavaFiles(src, "import java.lang.Deprecated; /** Test module */ @Deprecated module m1x {}");

            Iterable<? extends JavaFileObject> files = fm.getJavaFileObjects(findJavaFiles(src));
            JavacTask task = (JavacTask) compiler.getTask(null, fm, null, null, null, files);

            task.analyze();
            JavacTrees trees = JavacTrees.instance(task);
            ModuleElement mdle = (ModuleElement) task.getElements().getModuleElement("m1x");

            TreePath path = trees.getPath(mdle);
            assertNotNull("path", path);

            ModuleElement mdle1 = (ModuleElement) trees.getElement(path);
            assertNotNull("mdle1", mdle1);

            DocCommentTree docCommentTree = trees.getDocCommentTree(mdle);
            assertNotNull("docCommentTree", docCommentTree);
        }
    }
}
