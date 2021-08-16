/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8173596
 * @summary DocumentationTool.DocumentationTask should support addModules
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @library /tools/lib
 * @build APITest toolbox.JavacTask toolbox.ToolBox
 * @run main AddModulesTest
 */

import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import javax.tools.DocumentationTool;
import javax.tools.DocumentationTool.DocumentationTask;
import javax.tools.DocumentationTool.Location;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import toolbox.Assert;
import toolbox.JavacTask;
import toolbox.ToolBox;

/**
 * Tests for DocumentationTask.addModules method.
 */
public class AddModulesTest extends APITest {
    public static void main(String... args) throws Exception {
        new AddModulesTest().run();
    }

    private final ToolBox tb = new ToolBox();

    /**
     * Verify that addModules works as expected.
     */
    @Test
    public void testAddModules() throws Exception {
        Path base = Paths.get("testAddModules");
        Path src = base.resolve("src");

        // setup some utility modules
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1,
                          "module m1x { exports p1; }",
                          "package p1; public class C1 { }");
        Path src_m2 = src.resolve("m2x");
        tb.writeJavaFiles(src_m2,
                          "module m2x { exports p2; }",
                          "package p2; public class C2 { }");
        Path modules = base.resolve("modules");
        tb.createDirectories(modules);

        new JavacTask(tb)
                .options("--module-source-path", src.toString())
                .outdir(modules)
                .files(tb.findJavaFiles(src))
                .run()
                .writeAll();

        // now test access to the modules
        Path src2 = base.resolve("src2");
        tb.writeJavaFiles(src2,
                          "public class Dummy { p1.C1 c1; p2.C2 c2; }");
        Path api = base.resolve("api");
        tb.createDirectories(api);

        DocumentationTool tool = ToolProvider.getSystemDocumentationTool();
        try (StandardJavaFileManager fm = tool.getStandardFileManager(null, null, null)) {
            fm.setLocationFromPaths(StandardLocation.MODULE_PATH, Arrays.asList(modules));
            fm.setLocationFromPaths(Location.DOCUMENTATION_OUTPUT, Arrays.asList(api));
            Iterable<? extends JavaFileObject> files = fm.getJavaFileObjects(tb.findJavaFiles(src2));

            for (boolean useOption : new boolean[] { false, true }) {
                System.err.println("Use --add-modules option: " + useOption);
                StringWriter sw = new StringWriter();
                DocumentationTask t = tool.getTask(sw, fm, null, null, null, files);
                if (useOption) {
                    t.addModules(Arrays.asList("m1x", "m2x"));
                }
                String out;
                boolean ok;
                try {
                    ok = t.call();
                } finally {
                    out = sw.toString();
                    System.err.println(out);
                }
                System.err.println("ok: " + ok);
                boolean expectErrors = !useOption;
                check(out, "package p1 is not visible", expectErrors);
                check(out, "package p2 is not visible", expectErrors);
                System.err.println();
            }
        }
    }

    void check(String out, String text, boolean expected) {
        System.err.println("Checking for "
            + (expected ? "expected" : "unexpected")
            + " text: " + text);

        if (expected) {
            if (!out.contains(text)) {
                error("expected text not found: " + text);
            }
        } else {
            if (out.contains(text)) {
                error("unexpected text found: " + text);
            }
        }
    }
}

