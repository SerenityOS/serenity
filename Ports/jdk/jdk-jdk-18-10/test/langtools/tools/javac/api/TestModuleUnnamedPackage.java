/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8234025
 * @summary Elements.getPackageElement(ModuleElement,CharSequence) returns null for unnamed package
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @library /tools/lib /tools/javac/lib
 * @build toolbox.ModuleBuilder toolbox.ToolBox
 * @run main TestModuleUnnamedPackage
 */

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import com.sun.source.util.JavacTask;
import toolbox.Assert;
import toolbox.ToolBox;

public class TestModuleUnnamedPackage extends JavacTestingAbstractProcessor {
    public static void main(String... args) throws IOException {
        TestModuleUnnamedPackage t = new TestModuleUnnamedPackage();
            t.run();
    }

    void run() throws IOException {
        ToolBox tb = new ToolBox();
        Path src = Path.of("src");

        tb.writeJavaFiles(src, "module m { exports p; }",
                "package p; public class C { }");

        JavaCompiler c = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fm = c.getStandardFileManager(null, null, null);
        fm.setLocationFromPaths(StandardLocation.SOURCE_PATH, List.of(src));
        List<String> options = List.of("-proc:only");
        Iterable<? extends JavaFileObject> files = fm.getJavaFileObjects(tb.findJavaFiles(src));
        JavacTask t = (JavacTask) c.getTask(null, fm, null, null, null, files);
        t.setProcessors(List.of(this));
        t.call();
    }

    public boolean process(Set<? extends TypeElement> elems, RoundEnvironment rEnv) {
        if (rEnv.processingOver()) {
            Elements elements = processingEnv.getElementUtils();
            ModuleElement m = elements.getModuleElement("m");
            PackageElement unnamed = elements.getPackageElement(m, "");
            System.out.println("module m: " + m);
            System.out.println("module m, unnamed package: " + unnamed);
            Assert.checkNonNull(unnamed, "unnamed package in module m");
        }
        return true;
    }
}

