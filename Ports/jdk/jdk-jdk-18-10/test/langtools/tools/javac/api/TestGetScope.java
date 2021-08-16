/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7090249
 * @summary IllegalStateException from Trees.getScope when called from JSR 199
 * @modules jdk.compiler
 */

import com.sun.source.tree.IdentifierTree;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;

@SupportedAnnotationTypes("*")
public class TestGetScope extends AbstractProcessor {
    public static void main(String... args) throws IOException {
        new TestGetScope().run();
    }

    public void run() throws IOException {
        File srcDir = new File(System.getProperty("test.src"));
        File thisFile = new File(srcDir, getClass().getName() + ".java");

        JavaCompiler c = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = c.getStandardFileManager(null, null, null)) {

            List<String> opts = Arrays.asList("-proc:only", "-doe");
            Iterable<? extends JavaFileObject> files = fm.getJavaFileObjects(thisFile);
            JavacTask t = (JavacTask) c.getTask(null, fm, null, opts, null, files);
            t.setProcessors(Collections.singleton(this));
            boolean ok = t.call();
            if (!ok)
                throw new Error("compilation failed");
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Trees trees = Trees.instance(processingEnv);
        if (round++ == 0) {
            for (Element e: roundEnv.getRootElements()) {
                TreePath p = trees.getPath(e);
                new Scanner().scan(p, trees);
            }
        }
        return false;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    int round;

    static class Scanner extends TreePathScanner<Void,Trees> {
        @Override
        public Void visitIdentifier(IdentifierTree t, Trees trees) {
            System.err.println("visitIdentifier: " + t);
            trees.getScope(getCurrentPath());
            return null;
        }
    }
}
