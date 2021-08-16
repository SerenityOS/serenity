/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8222791
 * @summary Order of evaluation of -link params in Javadoc tool reversed:
 *          regression with split packages
 * @library /tools/lib ../../lib
 * @modules
 *      jdk.javadoc/jdk.javadoc.internal.api
 *      jdk.javadoc/jdk.javadoc.internal.tool
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.JavacTask toolbox.JavadocTask toolbox.ToolBox
 * @build javadoc.tester.*
 * @run main TestOptionOrder
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import toolbox.JavacTask;
import toolbox.JavadocTask;
import toolbox.Task;
import toolbox.ToolBox;

import javadoc.tester.JavadocTester;

public class TestOptionOrder extends JavadocTester {
    final ToolBox tb;

    public static void main(String... args) throws Exception {
        TestOptionOrder tester = new TestOptionOrder();
        tester.runTests(m -> new Object[] { Path.of(m.getName())} );
    }

    TestOptionOrder() throws Exception {
        tb = new ToolBox();
    }

    enum Kind { PACKAGE_LIST, ELEMENT_LIST };

    @Test
    public void testLib1Lib2PackageList(Path base) throws Exception {
        test(base, "lib1", "lib2", Kind.PACKAGE_LIST);
    }

    @Test
    public void testLib1Lib2ElementList(Path base) throws Exception {
        test(base, "lib1", "lib2", Kind.ELEMENT_LIST);
    }

    @Test
    public void testLib2Lib1PackageList(Path base) throws Exception {
        test(base, "lib2", "lib1", Kind.PACKAGE_LIST);
    }

    @Test
    public void testLib2Lib1ElementList(Path base) throws Exception {
        test(base, "lib2", "lib1", Kind.ELEMENT_LIST);
    }

    private void test(Path base, String first, String second, Kind kind) throws Exception {
        createLib(base, first, kind);
        createLib(base, second, kind);

        Path src = base.resolve("src");

        tb.writeJavaFiles(src,
            """
                package app;
                /** Lorem ipsum.
                 *  @see lib.LibClass
                 */
                public class App {
                    /** Reference to LibClass. */
                    public lib.LibClass lc;
                }
                """);

        javadoc("-d", base.resolve("out").toString(),
                "-classpath",
                    base.resolve(first).resolve("classes")
                    + File.pathSeparator
                    + base.resolve(second).resolve("classes"),
                "-linkoffline",
                    "http://example.com/" + first,
                    base.resolve(first).resolve("api").toString(),
                "-linkoffline",
                    "http://example.com/" + second,
                    base.resolve(second).resolve("api").toString(),
                "-sourcepath", src.toString(),
                "app");

         checkOrder("app/App.html",
                // Instance in See Also
                "<li><a href=\"http://example.com/" + first + "/lib/LibClass.html",
                // Instance in Field declaration
                """
                    <div class="col-first even-row-color"><code><a href="http://example.com/""" + first + "/lib/LibClass.html"
                );
    }

    private void createLib(Path base, String name, Kind kind) throws Exception {
        Path libBase = Files.createDirectories(base.resolve(name));
        Path libSrc = libBase.resolve("src");

        tb.writeJavaFiles(libSrc,
            "package lib;\n"
            + "/** Library " + name + ".*/\n"
            + "public class LibClass { }\n");

        new JavacTask(tb)
            .outdir(Files.createDirectories(libBase.resolve("classes")))
            .files(tb.findJavaFiles(libSrc))
            .run(Task.Expect.SUCCESS);

        Path libApi = libBase.resolve("api");
        new JavadocTask(tb)
            .sourcepath(libSrc)
            .outdir(Files.createDirectories(libBase.resolve("api")))
            .options("lib")
            .run(Task.Expect.SUCCESS);

        if (kind == Kind.PACKAGE_LIST) {
            Path elementList = libApi.resolve("element-list");
            Path packageList = libApi.resolve("package-list");
            Files.move(elementList, packageList);
        }
    }
}
