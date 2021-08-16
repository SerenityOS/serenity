/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8054717
 * @summary Make sure changes of public API on classpath triggers recompilation
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.sjavac
 * @build Wrapper toolbox.ToolBox toolbox.Assert
 * @run main Wrapper ClasspathDependencies
 */


import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import static toolbox.Assert.check;

public class ClasspathDependencies extends SjavacBase {

    public static void main(String... args) throws Exception {

        Path root = Paths.get(ClasspathDependencies.class.getSimpleName() + "Test");

        delete(root);

        Path src = root.resolve("src");
        Path classes = root.resolve("classes");
        Path srcDep = root.resolve("srcDep");
        Path classesDep = root.resolve("classesDep");

        ////////////////////////////////////////////////////////////////////////
        headline("Create a test dependency, Dep.class, and put it in the classpath dir");
        String depCode = "package dep; public class Dep { public void m1() {} }";
        toolbox.writeFile(srcDep.resolve("dep/Dep.java"), depCode);
        int rc = compile("-d", classesDep, "--state-dir=" + classesDep, srcDep);
        check(rc == 0, "Compilation failed unexpectedly");

        ////////////////////////////////////////////////////////////////////////
        headline("Compile and link against the Dep.class");
        toolbox.writeFile(src.resolve("pkg/C.java"),
                          "package pkg;" +
                          "import dep.Dep;" +
                          "public class C { Dep dep; public void m() { new Dep().m1(); } }");
        rc = compile("-d", classes, "--state-dir=" + classes, src, "-cp", classesDep);
        check(rc == 0, "Compilation failed unexpectedly");
        FileTime modTime1 = Files.getLastModifiedTime(classes.resolve("pkg/C.class"));

        ////////////////////////////////////////////////////////////////////////
        headline("Update dependency (without changing the public api)");
        Thread.sleep(2000);
        depCode = depCode.replaceAll("}$", "private void m2() {} }");
        toolbox.writeFile(srcDep.resolve("dep/Dep.java"), depCode);
        rc = compile("-d", classesDep, "--state-dir=" + classesDep, srcDep);
        check(rc == 0, "Compilation failed unexpectedly");

        ////////////////////////////////////////////////////////////////////////
        headline("Make sure that this does not trigger recompilation of C.java");
        rc = compile("-d", classes, "--state-dir=" + classes, src, "-cp", classesDep);
        check(rc == 0, "Compilation failed unexpectedly");
        FileTime modTime2 = Files.getLastModifiedTime(classes.resolve("pkg/C.class"));
        check(modTime1.equals(modTime2), "Recompilation erroneously triggered");

        ////////////////////////////////////////////////////////////////////////
        headline("Update public API of dependency");
        Thread.sleep(2000);
        depCode = depCode.replace("m1()", "m1(String... arg)");
        toolbox.writeFile(srcDep.resolve("dep/Dep.java"), depCode);
        rc = compile("-d", classesDep, "--state-dir=" + classesDep, srcDep);
        check(rc == 0, "Compilation failed unexpectedly");

        ////////////////////////////////////////////////////////////////////////
        headline("Make sure that recompilation of C.java is triggered");
        rc = compile("-d", classes, "--state-dir=" + classes, src, "-cp", classesDep);
        check(rc == 0, "Compilation failed unexpectedly");
        FileTime modTime3 = Files.getLastModifiedTime(classes.resolve("pkg/C.class"));
        check(modTime2.compareTo(modTime3) < 0, "Recompilation not triggered");
    }

    static void headline(String str) {
        System.out.println();
        System.out.println(str);
        System.out.println(str.replaceAll(".", "-"));
    }

    static void delete(Path root) throws IOException {
        if (!Files.exists(root))
            return;
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                 @Override
                 public FileVisitResult visitFile(Path f, BasicFileAttributes a)
                         throws IOException {
                     Files.delete(f);
                     return FileVisitResult.CONTINUE;
                 }

                 @Override
                 public FileVisitResult postVisitDirectory(Path dir, IOException e)
                         throws IOException {
                     if (e != null)
                         throw e;
                     if (!dir.equals(root))
                         Files.delete(dir);
                     return FileVisitResult.CONTINUE;
                 }
            });
    }
}
