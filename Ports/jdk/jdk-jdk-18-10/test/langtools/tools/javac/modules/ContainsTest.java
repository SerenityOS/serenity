/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8178518
 * @summary Add method JavaFileManager.contains
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JarTask toolbox.JavacTask ModuleTestBase
 * @run main ContainsTest
 */

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.List;

import javax.tools.FileObject;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import toolbox.JarTask;
import toolbox.JavacTask;

public class ContainsTest extends ModuleTestBase {
    public static void main(String... args) throws Exception {
        ContainsTest t = new ContainsTest();
        t.runTests();
    }

    JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();

    @Test
    public void testSimplePath(Path base) throws IOException {
        // Test that we can look up in directories in the default file system.
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "package p; class C { }");
        Path c = src.resolve("p/C.java");
        Path x = base.resolve("src2/p/C.java");
        try (StandardJavaFileManager fm = javaCompiler.getStandardFileManager(null, null, null)) {
            fm.setLocationFromPaths(StandardLocation.SOURCE_PATH, List.of(src));
            checkContains(fm, StandardLocation.SOURCE_PATH, c, true);
            checkContains(fm, StandardLocation.SOURCE_PATH, x, false);
        }
    }

    @Test
    public void testJarPath(Path base) throws IOException {
        // Test that we can look up in jar files on a search path.
        // In this case, the path we look up must come from open file system
        // as used by the file manager.
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "package p; class C { }");
        Path classes = Files.createDirectories(base.resolve("classes"));
        new JavacTask(tb)
            .options("-sourcepath", src.toString())
            .outdir(classes)
            .files(findJavaFiles(src))
            .run()
            .writeAll();

        Path jar = base.resolve("classes.jar");
        new JarTask(tb).run("cf", jar.toString(), "-C", classes.toString(), "p");

        Path c = src.resolve("p/C.java");
        Path x = base.resolve("src2/p/C.java");

        try (StandardJavaFileManager fm = javaCompiler.getStandardFileManager(null, null, null)) {
            fm.setLocationFromPaths(StandardLocation.CLASS_PATH, List.of(src, jar));

            checkContains(fm, StandardLocation.CLASS_PATH, c, true);
            checkContains(fm, StandardLocation.CLASS_PATH, x, false);

            JavaFileObject fo = fm.list(StandardLocation.CLASS_PATH, "p",
                    EnumSet.of(JavaFileObject.Kind.CLASS), false).iterator().next();

            checkContains(fm, StandardLocation.CLASS_PATH, fo, true);
        }
    }

    @Test
    public void testJarFSPath(Path base) throws IOException {
        // Test that we can look up in non-default file systems on the search path,
        // such as an open jar file system.
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "package p; class C { }");
        Path classes = Files.createDirectories(base.resolve("classes"));
        new JavacTask(tb)
            .options("-sourcepath", src.toString())
            .outdir(classes)
            .files(findJavaFiles(src))
            .run()
            .writeAll();

        Path jar = base.resolve("classes.jar");
        new JarTask(tb).run("cf", jar.toString(), "-C", classes.toString(), "p");

        Path c = src.resolve("p/C.java");
        Path x = base.resolve("src2/p/C.java");

        try (FileSystem jarFS = FileSystems.newFileSystem(jar);
                StandardJavaFileManager fm = javaCompiler.getStandardFileManager(null, null, null)) {
            Path jarRoot = jarFS.getRootDirectories().iterator().next();
            fm.setLocationFromPaths(StandardLocation.CLASS_PATH, List.of(src, jarRoot));

            checkContains(fm, StandardLocation.CLASS_PATH, c, true);
            checkContains(fm, StandardLocation.CLASS_PATH, x, false);

            JavaFileObject fo = fm.list(StandardLocation.CLASS_PATH, "p",
                    EnumSet.of(JavaFileObject.Kind.CLASS), false).iterator().next();

            checkContains(fm, StandardLocation.CLASS_PATH, fo, true);
            checkContains(fm, StandardLocation.CLASS_PATH, jarRoot.resolve("p/C.class"), true);
        }
    }

    void checkContains(StandardJavaFileManager fm, Location l, Path p, boolean expect) throws IOException {
        JavaFileObject fo = fm.getJavaFileObjects(p).iterator().next();
        checkContains(fm, l, fo, expect);
    }

    void checkContains(StandardJavaFileManager fm, Location l, FileObject fo, boolean expect) throws IOException {
        boolean found = fm.contains(l, fo);
        if (found) {
            if (expect) {
                out.println("file found, as expected: " + l + " " + fo.getName());
            } else {
                error("file not found: " + l + " " + fo.getName());
            }
        } else {
            if (expect) {
                error("file found unexpectedly: " + l + " " + fo.getName());
            } else {
                out.println("file not found, as expected: " + l + " " + fo.getName());
            }
        }
    }
}
