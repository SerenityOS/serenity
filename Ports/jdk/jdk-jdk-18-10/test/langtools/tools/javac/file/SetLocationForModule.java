/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8173914 8188035
 * @summary JavaFileManager.setLocationForModule
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @library /tools/lib
 * @build toolbox.JavacTask toolbox.TestRunner toolbox.ToolBox SetLocationForModule
 * @run main SetLocationForModule
 */


import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileManager.Location;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import toolbox.JavacTask;
import toolbox.TestRunner;
import toolbox.TestRunner.Test;
import toolbox.ToolBox;

public class SetLocationForModule extends TestRunner {

    public static void main(String... args) throws Exception {
        new SetLocationForModule().runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    public SetLocationForModule() {
        super(System.err);
    }

    private final JavaCompiler comp = ToolProvider.getSystemJavaCompiler();
    private final ToolBox tb = new ToolBox();

    @Test
    public void testBasic(Path base) throws IOException {
        try (StandardJavaFileManager fm = comp.getStandardFileManager(null, null, null)) {
            Location[] locns = {
                StandardLocation.SOURCE_PATH,
                StandardLocation.CLASS_PATH,
                StandardLocation.PLATFORM_CLASS_PATH,
            };
            // set a value
            Path out = Files.createDirectories(base.resolve("out"));
            for (Location locn : locns) {
                checkException("unsupported for location",
                        IllegalArgumentException.class,
                        "location is not an output location or a module-oriented location: " + locn,
                        () -> fm.setLocationForModule(locn, "m", List.of(out)));
            }
        }
    }

    @Test
    public void testModulePath(Path base) throws IOException {
        try (StandardJavaFileManager fm = comp.getStandardFileManager(null, null, null)) {
            Path src = base.resolve("src");
            Path src_m = src.resolve("m");
            tb.writeJavaFiles(src_m, "module m { }");

            Location locn = StandardLocation.MODULE_PATH;

            Path modules1 = Files.createDirectories(base.resolve("modules1"));
            new JavacTask(tb)
                    .outdir(modules1)
                    .options("--module-source-path", src.toString())
                    .files(tb.findJavaFiles(src))
                    .run();
            fm.setLocationFromPaths(locn, List.of(modules1));

            Location m = fm.getLocationForModule(locn, "m");
            checkEqual("default setting",
                    fm.getLocationAsPaths(m), modules1.resolve("m"));

            Path override1 = Files.createDirectories(base.resolve("override1"));
            fm.setLocationForModule(locn, "m", List.of(override1));
            checkEqual("override setting 1",
                    fm.getLocationAsPaths(m), override1);

            checkEqual("override setting 1b",
                       fm.getLocationAsPaths(fm.listLocationsForModules(locn).iterator().next().iterator().next()),
                       override1);

            try (StandardJavaFileManager fm2 = comp.getStandardFileManager(null, null, null)) {
                fm2.setLocationForModule(locn, "m", List.of(override1));
                checkEqual("override setting 2",
                           fm2.getLocationAsPaths(m), override1);

                Location firstLocation =
                        fm2.listLocationsForModules(locn).iterator().next().iterator().next();

                checkEqual("override setting 2b",
                           fm2.getLocationAsPaths(firstLocation),
                           override1);
            }

            Path override2 = Files.createDirectories(base.resolve("override2"));
            fm.setLocationFromPaths(m, List.of(override2));
            checkEqual("override setting 3",
                    fm.getLocationAsPaths(m), override2);

            Path modules2 = Files.createDirectories(base.resolve("modules2"));
            new JavacTask(tb)
                    .outdir(modules2)
                    .options("--module-source-path", src.toString())
                    .files(tb.findJavaFiles(src))
                    .run();
            fm.setLocationFromPaths(locn, List.of(modules2));

            m = fm.getLocationForModule(locn, "m");

            checkEqual("updated setting",
                    fm.getLocationAsPaths(m), modules2.resolve("m"));
        }
    }

    @Test
    public void testModuleSourcePath(Path base) throws IOException {
        try (StandardJavaFileManager fm = comp.getStandardFileManager(null, null, null)) {

            Location locn = StandardLocation.MODULE_SOURCE_PATH;

            Path src1 = Files.createDirectories(base.resolve("src1"));
            Path src1_m = src1.resolve("m");
            tb.writeJavaFiles(src1_m, "module m { }");
            fm.setLocationFromPaths(locn, List.of(src1));

            Location m = fm.getLocationForModule(locn, "m");
            checkEqual("default setting",
                    fm.getLocationAsPaths(m), src1.resolve("m"));

            Path override1 = Files.createDirectories(base.resolve("override1"));
            tb.writeJavaFiles(override1, "module m { }");
            fm.setLocationForModule(locn, "m", List.of(override1));
            checkEqual("override setting 1",
                    fm.getLocationAsPaths(m), override1);

            checkEqual("override setting 1b",
                       fm.getLocationAsPaths(fm.listLocationsForModules(locn).iterator().next().iterator().next()),
                       override1);

            Path override2 = Files.createDirectories(base.resolve("override2"));
            tb.writeJavaFiles(override2, "module m { }");
            fm.setLocationFromPaths(m, List.of(override2));
            checkEqual("override setting 2",
                    fm.getLocationAsPaths(m), override2);

            Path src2 = Files.createDirectories(base.resolve("src2"));
            Path src2_m = src2.resolve("m");
            tb.writeJavaFiles(src2_m, "module m { }");
            fm.setLocationFromPaths(locn, List.of(src2));

            m = fm.getLocationForModule(locn, "m");

            checkEqual("updated setting",
                    fm.getLocationAsPaths(m), src2.resolve("m"));
        }
    }

    @Test
    public void testOutput(Path base) throws IOException {
        try (StandardJavaFileManager fm = comp.getStandardFileManager(null, null, null)) {
            Location locn = StandardLocation.CLASS_OUTPUT;

            Path out1 = Files.createDirectories(base.resolve("out1"));
            fm.setLocationFromPaths(locn, List.of(out1));

            Location m = fm.getLocationForModule(locn, "m");
            checkEqual("default setting",
                    fm.getLocationAsPaths(m), out1.resolve("m"));

            Path override1 = Files.createDirectories(base.resolve("override1"));
            fm.setLocationForModule(locn, "m", List.of(override1));
            checkEqual("override setting 1",
                    fm.getLocationAsPaths(m), override1);

            checkEqual("override setting 1b",
                       fm.getLocationAsPaths(fm.listLocationsForModules(locn).iterator().next().iterator().next()),
                       override1);

            try (StandardJavaFileManager fm2 = comp.getStandardFileManager(null, null, null)) {
                fm2.setLocationForModule(locn, "m", List.of(override1));
                checkEqual("override setting 1",
                           fm2.getLocationAsPaths(m), override1);

                Location firstLocation =
                        fm2.listLocationsForModules(locn).iterator().next().iterator().next();

                checkEqual("override setting 1b",
                           fm2.getLocationAsPaths(firstLocation),
                           override1);
            }

            Path override2 = Files.createDirectories(base.resolve("override2"));
            fm.setLocationFromPaths(m, List.of(override2));
            checkEqual("override setting 2",
                    fm.getLocationAsPaths(m), override2);

            Path out2 = Files.createDirectories(base.resolve("out2"));
            fm.setLocationFromPaths(locn, List.of(out2));

            m = fm.getLocationForModule(locn, "m");

            checkEqual("updated setting",
                    fm.getLocationAsPaths(m), out2.resolve("m"));
        }
    }

    @Test
    public void testOutput_invalid(Path base) throws IOException {
        try (StandardJavaFileManager fm = comp.getStandardFileManager(null, null, null)) {
            Location locn = StandardLocation.CLASS_OUTPUT;
            // set a top default
            Path out1 = Files.createDirectories(base.resolve("out1"));
            fm.setLocationFromPaths(locn, List.of(out1));
            // getLocnForModule
            Location m = fm.getLocationForModule(locn, "m");
            checkEqual("default setting",
                    fm.getLocationAsPaths(m), out1.resolve("m"));

            checkException("empty arg list",
                    IllegalArgumentException.class, "empty path for directory",
                    () -> fm.setLocationFromPaths(m, Collections.emptyList()));

            Path out2 = Files.createDirectories(base.resolve("out2"));
            checkException("empty arg list",
                    IllegalArgumentException.class, "path too long for directory",
                    () -> fm.setLocationFromPaths(m, List.of(out2, out2)));

            Path notExist = base.resolve("notExist");
            checkException("not exist",
                    FileNotFoundException.class, notExist + ": does not exist",
                    () -> fm.setLocationFromPaths(m, List.of(notExist)));

            Path file = Files.createFile(base.resolve("file.txt"));
            checkException("not exist",
                    IOException.class, file + ": not a directory",
                    () -> fm.setLocationFromPaths(m, List.of(file)));
        }
    }

    @Test
    public void testOutput_nested(Path base) throws IOException {
        try (StandardJavaFileManager fm = comp.getStandardFileManager(null, null, null)) {
            Location locn = StandardLocation.CLASS_OUTPUT;

            Path out1 = Files.createDirectories(base.resolve("out1"));
            fm.setLocationForModule(locn, "m", List.of(out1));

            Location m = fm.getLocationForModule(locn, "m");
            checkEqual("initial setting",
                    fm.getLocationAsPaths(m), out1);

            Path out2 = Files.createDirectories(base.resolve("out2"));
            checkException("create nested module",
                    UnsupportedOperationException.class, "not supported for CLASS_OUTPUT[m]",
                    () -> fm.setLocationForModule(m, "x", List.of(out2)));
        }
    }

    @Test
    public void testSystemModules(Path base) throws IOException {
        try (StandardJavaFileManager fm = comp.getStandardFileManager(null, null, null)) {
            Location locn = StandardLocation.SYSTEM_MODULES;

            Location javaCompiler = fm.getLocationForModule(locn, "java.compiler");
            // cannot easily verify default setting: could be jrt: or exploded image

            Path override1 = Files.createDirectories(base.resolve("override1"));
            fm.setLocationForModule(locn, "java.compiler", List.of(override1));
            checkEqual("override setting 1",
                    fm.getLocationAsPaths(javaCompiler), override1);

            checkEqual("override setting 1b",
                       fm.getLocationAsPaths(findLocation(fm, fm.listLocationsForModules(locn), "java.compiler")),
                       override1);

            Path override2 = Files.createDirectories(base.resolve("override2"));
            fm.setLocationFromPaths(javaCompiler, List.of(override2));
            checkEqual("override setting 2",
                    fm.getLocationAsPaths(javaCompiler), override2);
        }
    }

    private Location findLocation(JavaFileManager fm, Iterable<Set<Location>> locations, String moduleName) {
        for (Set<Location> locs : locations) {
            for (Location loc : locs) {
                try {
                    if (moduleName.equals(fm.inferModuleName(loc))) {
                        return loc;
                    }
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        }

        throw new IllegalStateException();
    }

    @Test
    public void testTemplate(Path base) {
        // set a top default
        // getLocnForModule
        // set a value
        // getLocnForModule
        // reset
        // getLocationForModule
    }

    interface RunnableWithException {
        public void run() throws Exception;
    }

    void checkException(String message,
            Class<? extends Throwable> expectedException, String expectedMessage,
            RunnableWithException r) {
        try {
            r.run();
            error(message + ": expected exception not thrown: " + expectedException);
        } catch (Exception | Error t) {
            if (expectedException.isAssignableFrom(t.getClass())) {
                checkEqual("exception message",
                        t.getMessage(), expectedMessage);

            } else {
                error(message + ": unexpected exception\n"
                        + "expect: " + expectedException + "\n"
                        + " found: " + t);
            }
        }
    }

    void checkEqual(String message, Iterable<? extends Path> found, Path... expect) {
        List<Path> fList = asList(found);
        List<Path> eList = List.of(expect);
        if (!Objects.equals(fList, eList)) {
            error(message + ": lists not equal\n"
                    + "expect: " + eList + "\n"
                    + " found: " + fList);
        }
    }

    void checkEqual(String message, String found, String expect) {
        if (!Objects.equals(found, expect)) {
            error(message + ": strings not equal\n"
                    + "expect: " + expect + "\n"
                    + " found: " + found);
        }
    }

    List<Path> asList(Iterable<? extends Path> a) {
        List<Path> list = new ArrayList<>();
        for (Path p : a) {
            list.add(p);
        }
        return list;
    }
}

