/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8160489 8217868
 * @summary tests for --patch-modules
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.file
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask toolbox.ModuleBuilder ModuleTestBase
 * @run main PatchModulesTest
 */

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.file.JavacFileManager;

import static java.util.Arrays.asList;


public class PatchModulesTest extends ModuleTestBase {

    public static void main(String... args) throws Exception {
        PatchModulesTest t = new PatchModulesTest();
        t.init();
        t.runTests();
    }

    private static String PS = File.pathSeparator;

    void init() throws IOException {
        tb.createDirectories("a", "b", "c", "d", "e");
        tb.writeJavaFiles(Paths.get("."), "class C { }");
    }

    @Test
    public void testSimple(Path base) throws Exception {
        test(asList("java.base=a"),
            "{java.base=[a]}");
    }

    @Test
    public void testPair(Path base) throws Exception {
        test(asList("java.base=a", "java.compiler=b"),
            "{java.base=[a], java.compiler=[b]}");
    }

    @Test
    public void testMultiple(Path base) throws Exception {
        test(asList("java.base=a:b"),
            "{java.base=[a, b]}");
    }

    @Test
    public void testDuplicates(Path base) throws Exception {
        test(asList("java.base=a", "java.compiler=b", "java.base=c"),
            false, "error: --patch-module specified more than once for module java.base");
    }

    @Test
    public void testEmpty(Path base) throws Exception {
        test(asList(""),
            false, "error: no value for --patch-module option");
    }

    @Test
    public void testInvalid(Path base) throws Exception {
        test(asList("java.base/java.lang=."),
            false, "error: bad value for --patch-module option: 'java.base/java.lang=.'");
    }

    void test(List<String> patches, String expect) throws Exception {
        test(patches, true, expect);
    }

    void test(List<String> patches, boolean expectOK, String expect) throws Exception {
        JavacTool tool = (JavacTool) ToolProvider.getSystemJavaCompiler();
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw);
            JavacFileManager fm = tool.getStandardFileManager(null, null, null)) {
            List<String> opts = patches.stream()
                .map(p -> "--patch-module=" + p.replace(":", PS))
                .collect(Collectors.toList());
            Iterable<? extends JavaFileObject> files = fm.getJavaFileObjects("C.java");
            JavacTask task = tool.getTask(pw, fm, null, opts, null, files);

            Map<String, List<Location>> mod2Location =
                    StreamSupport.stream(fm.listLocationsForModules(StandardLocation.PATCH_MODULE_PATH)
                                           .spliterator(),
                                        false)
                                 .flatMap(sl -> sl.stream())
                                 .collect(Collectors.groupingBy(l -> fm.inferModuleName(l)));

            Map<String, List<String>> patchMap = mod2Location.entrySet()
                    .stream()
                    .map(e -> new SimpleEntry<>(e.getKey(), e.getValue().get(0)))
                    .map(e -> new SimpleEntry<>(e.getKey(), locationPaths(fm, e.getValue())))
                    .collect(Collectors.toMap(Entry :: getKey,
                                              Entry :: getValue,
                                              (v1, v2) -> {throw new IllegalStateException();},
                                              TreeMap::new));
            String found = patchMap.toString();

            if (!found.equals(expect)) {
                tb.out.println("Expect: " + expect);
                tb.out.println("Found:  " + found);
                error("output not as expected");
            }
        } catch (IllegalArgumentException e) {
            if (expectOK) {
                error("unexpected exception: " + e);
                throw e;
            }
            String found = e.getMessage();
            if (!found.equals(expect)) {
                tb.out.println("Expect: " + expect);
                tb.out.println("Found:  " + found);
                error("output not as expected");
            }
        }
    }

    static List<String> locationPaths(StandardJavaFileManager fm, Location loc) {
        return StreamSupport.stream(fm.getLocationAsPaths(loc).spliterator(), false)
                            .map(p -> p.toString())
                            .collect(Collectors.toList());
    }

    @Test
    public void testPatchWithSource(Path base) throws Exception {
        Path patch = base.resolve("patch");
        tb.writeJavaFiles(patch, "package javax.lang.model.element; public interface Extra { }");
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                          "module m { requires java.compiler; }",
                          "package test; public interface Test extends javax.lang.model.element.Extra { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        new toolbox.JavacTask(tb)
            .options("--patch-module", "java.compiler=" + patch.toString())
            .outdir(classes)
            .files(findJavaFiles(src))
            .run()
            .writeAll();

        if (Files.exists(classes.resolve("javax"))) {
            throw new AssertionError();
        }
    }

    @Test
    public void testPatchModuleSourcePathClash(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                          "module m { uses test.Test; }",
                          "package test; public class Test { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        new toolbox.JavacTask(tb)
            .options("--patch-module", "other=" + src.toString(),
                     "-sourcepath", src.toString())
            .outdir(classes)
            .files(findJavaFiles(src.resolve("module-info.java")))
            .run()
            .writeAll();
    }
}

