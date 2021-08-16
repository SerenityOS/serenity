/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8147414
 * @summary java.nio.file.ClosedFileSystemException in javadoc
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.JarTask toolbox.JavacTask toolbox.ToolBox
 * @run main FileSystemClosedTest
 */

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import toolbox.ToolBox;
import toolbox.JarTask;
import toolbox.JavacTask;

public class FileSystemClosedTest {
    public static void main(String... args) throws Exception {
        new FileSystemClosedTest().run();
    }

    void run() throws Exception {
        ToolBox tb = new ToolBox();
        Path jar = createJar(tb);

        Path src = Paths.get("src");
        tb.writeJavaFiles(src, "class C { p1.C1 c1; }");

        JavaCompiler comp = ToolProvider.getSystemJavaCompiler();
        PrintWriter out = new PrintWriter(System.err, true);
        StandardJavaFileManager fm = comp.getStandardFileManager(null, null, null);
        List<String> options = Arrays.asList("-classpath", jar.toString(), "-proc:none");
        Iterable<? extends JavaFileObject> files = fm.getJavaFileObjects(src.resolve("C.java"));
        com.sun.source.util.JavacTask task =
                (com.sun.source.util.JavacTask) comp.getTask(out, fm, null, options, null, files);
        task.parse();

        Elements elems = task.getElements();

        try {
            // Use  p1, p1.C1 as a control to verify normal behavior
            PackageElement p1 = elems.getPackageElement("p1");
            TypeElement p1C1 = elems.getTypeElement("p1.C1");
            System.err.println("p1: " + p1 + ";  p1C1: " + p1C1);
            if (p1C1 == null) {
                throw new Exception("p1.C1 not found");
            }

            // Now repeat for p2, p2.C2, closing the file manager early
            PackageElement p2 = elems.getPackageElement("p2");
            System.err.println("closing file manager");
            fm.close();
            TypeElement p2C2 = elems.getTypeElement("p2.C2");
            System.err.println("p2: " + p2 + ";  p2C2: " + p2C2);
            if (p2C2 != null) {
                throw new Exception("p2.C2 found unexpectedly");
            }
        } catch (ClosedFileSystemException e) {
            throw new Exception("unexpected exception thrown", e);
        }

    }

    private Path createJar(ToolBox tb) throws IOException {
        Path jarSrc = Paths.get("jarSrc");
        Path jarClasses = Paths.get("jarClasses");
        Path jar = Paths.get("jar.jar");
        Files.createDirectories(jarClasses);

        tb.writeJavaFiles(jarSrc,
                "package p1; public class C1 { }",
                "package p2; public class C2 { }");

        new JavacTask(tb)
                .outdir(jarClasses)
                .files(tb.findJavaFiles(jarSrc))
                .run()
                .writeAll();
        new JarTask(tb)
                .run("cf", jar.toString(), "-C", jarClasses.toString(), "p1", "p2");

        return jar;
    }
}

