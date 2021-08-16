/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6473148
 * @summary TreePath.iterator() throws NPE
 * @modules jdk.compiler
 */
import java.io.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.source.tree.Tree;
import com.sun.source.util.*;

@SupportedAnnotationTypes("*")
public class TestTreePath extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations,
            RoundEnvironment roundEnv) {
        final Trees trees = Trees.instance(this.processingEnv);
        for (Element element : ElementFilter.typesIn(roundEnv.getRootElements())) {
            checkTreePath(trees, element, 2);
            for (Element member : element.getEnclosedElements())
                checkTreePath(trees, member, 3);
        }
        return true;
    }

    private void checkTreePath(Trees trees, Element element, int expectedLength) {
        TreePath path = trees.getPath(element);
        assert path != null;

        int enhancedLength = 0;
        for (Tree tree : path)
            ++enhancedLength;

        if (enhancedLength != expectedLength)
            throw new RuntimeException("found path length is wrong");

        int normalLoopLength = 0;
        Iterator<Tree> iter = path.iterator();
        while (iter.hasNext()) {
          iter.next();
          ++normalLoopLength;
        }
        if (normalLoopLength != expectedLength)
            throw new RuntimeException("found path length is wrong");

        TreePath curr = path;
        // using getParent
        int whileLoopLength = 0;
        while (curr != null) {
          ++whileLoopLength;
          curr = curr.getParentPath();
        }
        if (whileLoopLength != expectedLength)
            throw new RuntimeException("found path length is wrong");
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    File writeTestFile() throws IOException {
        File f = new File("Test.java");
        PrintWriter out = new PrintWriter(new FileWriter(f));
        out.println("class Test { void method() { } }");
        out.close();
        return f;
    }

    public void run() throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fileManager
                = compiler.getStandardFileManager(null, null, null)) {
            Iterable<? extends JavaFileObject> tests
                = fileManager.getJavaFileObjects(writeTestFile());

            JavaCompiler.CompilationTask task =
                ToolProvider.getSystemJavaCompiler().getTask(
                        null, null, null,
                        Arrays.asList("-processor", this.getClass().getName()), null,
                        tests);
            task.call();
        }
    }

    public static void main(String[] args) throws IOException {
        new TestTreePath().run();
    }
}
