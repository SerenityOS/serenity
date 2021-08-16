/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8212233
 * @summary The code being documented uses modules but the packages defined in $URL are in the unnamed module.
 * @library /tools/lib ../../lib
 * @modules
 *      jdk.javadoc/jdk.javadoc.internal.tool
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build javadoc.tester.* toolbox.JarTask toolbox.JavacTask toolbox.ModuleBuilder toolbox.ToolBox
 * @run main TestLinkOptionWithAutomaticModule
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import toolbox.JarTask;
import toolbox.JavacTask;
import toolbox.ModuleBuilder;
import toolbox.ToolBox;

import javadoc.tester.JavadocTester;

public class TestLinkOptionWithAutomaticModule extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestLinkOptionWithAutomaticModule tester = new TestLinkOptionWithAutomaticModule();
        tester.runTests(m -> new Object[]{ Path.of(m.getName()) });
    }

    final ToolBox tb = new ToolBox();
    private Path libJar;
    private Path libAPI;

    TestLinkOptionWithAutomaticModule() throws IOException {
        initLib();
    }

    private void initLib() throws IOException {
        // create library: write source, compile it, jar it
        Path lib = Path.of("lib");
        Path libSrc = lib.resolve("src");
        tb.writeJavaFiles(libSrc, "package lib; public class LibClass { }");
        Path libClasses = Files.createDirectories(lib.resolve("classes"));

        new JavacTask(tb)
                .outdir(libClasses)
                .files(tb.findJavaFiles(libSrc))
                .run()
                .writeAll();

        libJar = lib.resolve("MyLib.jar");
        new JarTask(tb, libJar)
                .baseDir(libClasses)
                .files(".")
                .run();

        libAPI = lib.resolve("api");
        javadoc("-d", libAPI.toString(),
                "-sourcepath", libSrc.toString(),
                "lib");
        checkExit(Exit.OK);
    }

    @Test
    public void testLinkUnnamedToAutomaticModule(Path base) throws IOException {

        // create API referring to library
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "package p; public class MyClass extends lib.LibClass { }");

        // run javadoc with library as automatic module
        Path api = base.resolve("api");
        javadoc("-d", api.toString(),
                "-sourcepath", src.toString(),
                "--add-modules", "MyLib",
                "--module-path", libJar.toString(),
                "-linkoffline", "http://myWebsite", libAPI.toAbsolutePath().toString(),
                "p");
        checkExit(Exit.OK);
        checkOutput("p/MyClass.html", true,
                """
                    extends <a href="http://myWebsite/lib/LibClass.html" title="class or interface i\
                    n lib" class="external-link">LibClass</a>""");
    }

    @Test
    public void testLinkNamedToAutomaticModule(Path base) throws IOException {

        // create API referring to library
        Path src = base.resolve("src");
        new ModuleBuilder(tb, "my.module")
                .exports("p")
                .requires("MyLib")
                .classes("package p; public class MyClass extends lib.LibClass { }")
                .write(src);

        // run javadoc with library as automatic module
        Path api = base.resolve("api");
        javadoc("-d", api.toString(),
                "--module-source-path", src.toString(),
                "--module-path", libJar.toString(),
                "-linkoffline", "http://myWebsite", libAPI.toAbsolutePath().toString(),
                "--module", "my.module");
        checkExit(Exit.OK);
        checkOutput("my.module/p/MyClass.html", true,
                """
                    extends <a href="http://myWebsite/lib/LibClass.html" title="class or interface i\
                    n lib" class="external-link">LibClass</a>""");
    }

    @Test
    public void testLinkNamedToUnnamedModule(Path base) throws IOException {

        // create API referring to library
        Path src = base.resolve("src");
        new ModuleBuilder(tb, "my.module")
                .exports("p")
                .classes("package p; public class MyClass extends lib.LibClass { }")
                .write(src);

        // run javadoc with library as unnamed module
        Path api = base.resolve("api");
        javadoc("-d", api.toString(),
                "--module-source-path", src.toString(),
                "--add-reads", "my.module=ALL-UNNAMED",
                "--class-path", libJar.toString(),
                "-linkoffline", "http://myWebsite", libAPI.toAbsolutePath().toString(),
                "--module", "my.module");
        checkExit(Exit.OK);
        checkOutput("my.module/p/MyClass.html", true,
                """
                    extends <a href="http://myWebsite/lib/LibClass.html" title="class or interface i\
                    n lib" class="external-link">LibClass</a>""");
    }
}
