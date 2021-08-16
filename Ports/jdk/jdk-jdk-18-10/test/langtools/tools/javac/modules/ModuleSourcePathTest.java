/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary tests for --module-source-path
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask ModuleTestBase
 * @run main ModuleSourcePathTest
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager.Location;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.ToolBox;

public class ModuleSourcePathTest extends ModuleTestBase {

    public static final char PATH_SEP = File.pathSeparatorChar;

    public static void main(String... args) throws Exception {
        ModuleSourcePathTest t = new ModuleSourcePathTest();
        t.runTests();
    }

    @Test
    public void testSourcePathConflict(Path base) throws Exception {
        Path sp = base.resolve("src");
        Path msp = base.resolve("srcmodules");

        String log = new JavacTask(tb, Task.Mode.CMDLINE)
                .options("-XDrawDiagnostics",
                        "--source-path", sp.toString().replace('/', File.separatorChar),
                        "--module-source-path", msp.toString().replace('/', File.separatorChar),
                        "dummyClass")
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("compiler.err.sourcepath.modulesourcepath.conflict"))
            throw new Exception("expected diagnostic not found");
    }

    @Test
    public void testUnnormalizedPath1(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1, "module m1x { }");
        Path modules = base.resolve("modules");
        Files.createDirectories(modules);

        new JavacTask(tb, Task.Mode.CMDLINE)
                .options("-XDrawDiagnostics",
                        "--module-source-path", src.toString())
                .outdir(modules)
                .files(prefixAll(findJavaFiles(src), Paths.get("./")))
                .run()
                .writeAll();
    }

    @Test
    public void testUnnormalizedPath2(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1, "module m1x { }");
        Path modules = base.resolve("modules");
        Files.createDirectories(modules);

        new JavacTask(tb, Task.Mode.CMDLINE)
                .options("-XDrawDiagnostics",
                        "--module-source-path", "./" + src)
                .outdir(modules)
                .files(findJavaFiles(src))
                .run()
                .writeAll();
    }

    private Path[] prefixAll(Path[] paths, Path prefix) {
        return Stream.of(paths)
                .map(prefix::resolve)
                .collect(Collectors.toList())
                .toArray(new Path[paths.length]);
    }

    @Test
    public void regularBraces(Path base) throws Exception {
        generateModules(base, "src1", "src2/inner_dir");

        final Path modules = base.resolve("modules");
        tb.createDirectories(modules);

        new JavacTask(tb, Task.Mode.CMDLINE)
                .options("-XDrawDiagnostics",
                        "--module-source-path", base + "/{src1,src2/inner_dir}")
                .files(base.resolve("src1/m0x/pkg0/A.java"), base.resolve("src2/inner_dir/m1x/pkg1/A.java"))
                .outdir(modules)
                .run()
                .writeAll();

        checkFiles(modules.resolve("m0x/pkg0/A.class"),
                modules.resolve("m1x/pkg1/A.class"),
                modules.resolve("m0x/module-info.class"),
                modules.resolve("m1x/module-info.class"));
    }

    @Test
    public void mismatchedBraces(Path base) throws Exception {
        final List<String> sourcePaths = Arrays.asList(
                "{",
                "}",
                "}{",
                "./}",
                ".././{./",
                "src{}}",
                "{{}{}}{}}",
                "src/{a,b}/{",
                "src/{a,{,{}}",
                "{.,..{}/src",
                "*}{",
                "{}*}"
        );
        for (String sourcepath : sourcePaths) {
            String log = new JavacTask(tb, Task.Mode.CMDLINE)
                    .options("-XDrawDiagnostics",
                            "--module-source-path", sourcepath.replace('/', File.separatorChar))
                    .run(Task.Expect.FAIL)
                    .writeAll()
                    .getOutput(Task.OutputKind.DIRECT);

            if (!log.contains("- compiler.err.illegal.argument.for.option: --module-source-path, mismatched braces"))
                throw new Exception("expected output for path [" + sourcepath + "] not found");
        }
    }

    @Test
    public void deepBraces(Path base) throws Exception {
        String[] modulePaths = {"src/src1",
                "src/src2",
                "src/src3",
                "src/srcB/src1",
                "src/srcB/src2/srcXX",
                "src/srcB/src2/srcXY",
                "src/srcC/src1",
                "src/srcC/src2/srcXX",
                "src/srcC/src2/srcXY"};
        generateModules(base, modulePaths);

        final Path modules = base.resolve("modules");
        tb.createDirectories(modules);

        new JavacTask(tb, Task.Mode.CMDLINE)
                .options("-XDrawDiagnostics",
                        "--module-source-path",
                        base + "/{src/{{src1,src2,src3},{srcB,srcC}/{src1,src2/srcX{X,Y}/}},.}"
                                .replace('/', File.separatorChar))
                .files(findJavaFiles(base.resolve(modulePaths[modulePaths.length - 1])))
                .outdir(modules)
                .run()
                .writeAll();

        for (int i = 0; i < modulePaths.length; i++) {
            checkFiles(modules.resolve("m" + i + "x/module-info.class"));
        }
        checkFiles(modules.resolve("m8x/pkg8/A.class"));
    }

    @Test
    public void fileInPath(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src.resolve("kettle$"), "module kettle$ { }", "package electric; class Heater { }");
        tb.writeFile(base.resolve("dummy.txt"), "");

        final Path modules = base.resolve("modules");
        tb.createDirectories(modules);

        new JavacTask(tb, Task.Mode.CMDLINE)
                .options("-XDrawDiagnostics",
                        "--module-source-path", base + "/{dummy.txt,src}")
                .files(src.resolve("kettle$/electric/Heater.java"))
                .outdir(modules)
                .run()
                .writeAll();

        checkFiles(modules.resolve("kettle$/electric/Heater.class"));
        checkFiles(modules.resolve("kettle$/module-info.class"));
    }

    @Test
    public void noAlternative(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src.resolve("kettle$"), "module kettle$ { }", "package electric; class Heater { }");

        final Path modules = base.resolve("modules");
        tb.createDirectories(modules);

        new JavacTask(tb, Task.Mode.CMDLINE)
                .options("-XDrawDiagnostics",
                        "--module-source-path", base + "/{src}")
                .files(src.resolve("kettle$/electric/Heater.java"))
                .outdir(modules)
                .run()
                .writeAll();

        checkFiles(modules.resolve("kettle$/electric/Heater.class"));
        checkFiles(modules.resolve("kettle$/module-info.class"));
    }

    @Test
    public void noChoice(Path base) throws Exception {
        tb.writeJavaFiles(base.resolve("kettle$"), "module kettle$ { }", "package electric; class Heater { }");

        final Path modules = base.resolve("modules");
        tb.createDirectories(modules);

        new JavacTask(tb, Task.Mode.CMDLINE)
                .options("-XDrawDiagnostics",
                        "--module-source-path", base + "/{}")
                .files(base.resolve("kettle$/electric/Heater.java"))
                .outdir(modules)
                .run()
                .writeAll();

        checkFiles(modules.resolve("kettle$/electric/Heater.class"));
        checkFiles(modules.resolve("kettle$/module-info.class"));
    }

    @Test
    public void nestedModules(Path src) throws Exception {
        Path carModule = src.resolve("car");
        tb.writeJavaFiles(carModule, "module car { }", "package light; class Headlight { }");
        tb.writeJavaFiles(carModule.resolve("engine"), "module engine { }", "package flat; class Piston { }");

        final Path modules = src.resolve("modules");
        tb.createDirectories(modules);

        new JavacTask(tb, Task.Mode.CMDLINE)
                .options("-XDrawDiagnostics",
                        "--module-source-path", "{" + src + "," + src + "/car}")
                .files(findJavaFiles(src))
                .outdir(modules)
                .run()
                .writeAll();
        checkFiles(modules.resolve("car/light/Headlight.class"));
        checkFiles(modules.resolve("engine/flat/Piston.class"));
    }

    @Test
    public void relativePaths(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src.resolve("kettle"), "module kettle { }", "package electric; class Heater { }");

        final Path modules = base.resolve("modules");
        tb.createDirectories(modules);

        new JavacTask(tb, Task.Mode.CMDLINE)
                .options("-XDrawDiagnostics",
                        "--module-source-path", base + "/src/./../src")
                .files(src.resolve("kettle/electric/Heater.java"))
                .outdir(modules)
                .run()
                .writeAll();
        checkFiles(modules.resolve("kettle/electric/Heater.class"));
        checkFiles(modules.resolve("kettle/module-info.class"));
    }

    @Test
    public void duplicatePaths(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src.resolve("m1x"), "module m1x { }", "package a; class A { }");

        final Path modules = base.resolve("modules");
        tb.createDirectories(modules);

        new JavacTask(tb, Task.Mode.CMDLINE)
                .options("-XDrawDiagnostics",
                        "--module-source-path", base + "/{src,src,src}")
                .files(src.resolve("m1x/a/A.java"))
                .outdir(modules)
                .run()
                .writeAll();

        checkFiles(modules.resolve("m1x/module-info.class"));
    }

    @Test
    public void notExistentPaths(Path base) throws Exception {
        tb.writeJavaFiles(base.resolve("m1x"), "module m1x { requires m0x; }", "package a; class A { }");

        final Path modules = base.resolve("modules");
        tb.createDirectories(modules);

        String log = new JavacTask(tb, Task.Mode.CMDLINE)
                .options("-XDrawDiagnostics",
                        "--module-source-path", base + "/not_exist" + PATH_SEP + base + "/{not_exist,}")
                .files(base.resolve("m1x/a/A.java"))
                .outdir(modules)
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);
        if (!log.contains("compiler.err.module.not.found: m0x"))
            throw new Exception("expected output for not existent module source path not found");
    }

    @Test
    public void notExistentPathShouldBeSkipped(Path base) throws Exception {
        tb.writeJavaFiles(base.resolve("m1x"), "module m1x { }", "package a; class A { }");

        final Path modules = base.resolve("modules");
        tb.createDirectories(modules);

        new JavacTask(tb, Task.Mode.CMDLINE)
                .options("-XDrawDiagnostics",
                        "--module-source-path", base + "{/not_exist,/}")
                .files(base.resolve("m1x/a/A.java"))
                .outdir(modules)
                .run()
                .writeAll();

        checkFiles(modules.resolve("m1x/module-info.class"));
    }

    @Test
    public void commas(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src.resolve("m1x"), "module m1x { }", "package a; class A { }");

        final Path modules = base.resolve("modules");
        tb.createDirectories(modules);

        new JavacTask(tb, Task.Mode.CMDLINE)
                .options("-XDrawDiagnostics",
                        "--module-source-path", base + "/{,{,,,,src,,,}}")
                .files(src.resolve("m1x/a/A.java"))
                .outdir(modules)
                .run()
                .writeAll();

        checkFiles(modules.resolve("m1x/module-info.class"));
    }

    @Test
    public void asterisk(Path base) throws Exception {
        tb.writeJavaFiles(base.resolve("kettle").resolve("classes"), "module kettle { }",
                "package electric; class Heater { }");

        final Path modules = base.resolve("modules");
        tb.createDirectories(modules);

        new JavacTask(tb, Task.Mode.CMDLINE)
                .options("-XDrawDiagnostics",
                        "--module-source-path", base + "/*/classes/")
                .files(base.resolve("kettle/classes/electric/Heater.java"))
                .outdir(modules)
                .run()
                .writeAll();

        checkFiles(modules.resolve("kettle/electric/Heater.class"));
        checkFiles(modules.resolve("kettle/module-info.class"));
    }

    @Test
    public void asteriskInDifferentSets(Path base) throws Exception {
        Path src = base.resolve("src");
        final Path module = src.resolve("kettle");
        tb.writeJavaFiles(module.resolve("classes"), "module kettle { }", "package electric; class Heater { }");
        tb.writeJavaFiles(module.resolve("gensrc"), "package model; class Java { }");
        tb.writeJavaFiles(module.resolve("special/classes"), "package gas; class Heater { }");

        final Path modules = base.resolve("modules");
        tb.createDirectories(modules);

        new JavacTask(tb, Task.Mode.CMDLINE)
                .options("-XDrawDiagnostics",
                        "--module-source-path", src + "{/*/gensrc/,/*/classes/}" + PATH_SEP
                                + src + "/*/special/classes")
                .files(findJavaFiles(src))
                .outdir(modules)
                .run()
                .writeAll();

        checkFiles(modules.resolve("kettle/electric/Heater.class"));
        checkFiles(modules.resolve("kettle/gas/Heater.class"));
        checkFiles(modules.resolve("kettle/model/Java.class"));
        checkFiles(modules.resolve("kettle/module-info.class"));
    }

    @Test
    public void asteriskIllegalUse(Path base) throws Exception {
        final List<String> sourcePaths = Arrays.asList(
                "*",
                "**",
                "***",
                "*.*",
                ".*",
                "*.",
                "src/*/*/",
                "{*,*}",
                "src/module*/"
        );
        for (String sourcepath : sourcePaths) {
            String log = new JavacTask(tb, Task.Mode.CMDLINE)
                    .options("-XDrawDiagnostics",
                            "--module-source-path", sourcepath.replace('/', File.separatorChar))
                    .run(Task.Expect.FAIL)
                    .writeAll()
                    .getOutput(Task.OutputKind.DIRECT);

            if (!log.contains("- compiler.err.illegal.argument.for.option: --module-source-path, illegal use of *"))
                throw new Exception("expected output for path [" + sourcepath + "] not found");
        }
    }

    @Test
    public void setLocation(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src.resolve("m1x"), "module m1x { }", "package a; class A { }");
        Path modules = base.resolve("modules");
        tb.createDirectories(modules);

        JavaCompiler c = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = c.getStandardFileManager(null, null, null)) {
            fm.setLocationFromPaths(StandardLocation.MODULE_SOURCE_PATH, List.of(src));
            new JavacTask(tb)
                    .options("-XDrawDiagnostics")
                    .fileManager(fm)
                    .outdir(modules)
                    .files(findJavaFiles(src))
                    .run()
                    .writeAll();

            checkFiles(modules.resolve("m1x/module-info.class"), modules.resolve("m1x/a/A.class"));
        }
    }

    @Test
    public void getLocation_valid(Path base) throws Exception {
        Path src1 = base.resolve("src1");
        tb.writeJavaFiles(src1.resolve("m1x"), "module m1x { }", "package a; class A { }");
        Path src2 = base.resolve("src2");
        tb.writeJavaFiles(src1.resolve("m2x"), "module m2x { }", "package b; class B { }");

        JavaCompiler c = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = c.getStandardFileManager(null, null, null)) {
            fm.setLocationFromPaths(StandardLocation.MODULE_SOURCE_PATH, List.of(src1, src2));
            checkLocation(fm.getLocationAsPaths(StandardLocation.MODULE_SOURCE_PATH), List.of(src1, src2));
        }
    }

    @Test
    public void getLocation_ISA(Path base) throws Exception {
        Path src1 = base.resolve("src1");
        tb.writeJavaFiles(src1.resolve("m1x"), "module m1x { }", "package a; class A { }");
        Path src2 = base.resolve("src2");
        tb.writeJavaFiles(src2.resolve("m2x").resolve("extra"), "module m2x { }", "package b; class B { }");
        Path modules = base.resolve("modules");
        tb.createDirectories(modules);

        String FS = File.separator;
        String PS = File.pathSeparator;
        JavaCompiler c = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = c.getStandardFileManager(null, null, null)) {
            fm.handleOption("--module-source-path",
                    List.of(src1 + PS + src2 + FS + "*" + FS + "extra").iterator());

            try {
                Iterable<? extends Path> paths = fm.getLocationAsPaths(StandardLocation.MODULE_SOURCE_PATH);
                out.println("result: " + asList(paths));
                throw new Exception("expected IllegalStateException not thrown");
            } catch (IllegalStateException e) {
                out.println("Exception thrown, as expected: " + e);
            }

            // even if we can't do getLocation for the MODULE_SOURCE_PATH, we should be able
            // to do getLocation for the modules, which will additionally confirm the option
            // was effective as intended.
            Location locn1 = fm.getLocationForModule(StandardLocation.MODULE_SOURCE_PATH, "m1x");
            checkLocation(fm.getLocationAsPaths(locn1), List.of(src1.resolve("m1x")));
            Location locn2 = fm.getLocationForModule(StandardLocation.MODULE_SOURCE_PATH, "m2x");
            checkLocation(fm.getLocationAsPaths(locn2), List.of(src2.resolve("m2x").resolve("extra")));
        }
    }

    @Test
    public void moduleSpecificFormsOnly(Path base) throws Exception {
        // The dirs for the modules do not use a subdirectory named for the module,
        // meaning they can only be used by the module-specific form of the option.
        String[] srcDirs = {
                "src0",         // m0x
                "src1",         // m1x
                "src2",         // m2x
                "src3"          // m3x
        };
        generateModules(base, false, srcDirs);

        final Path modules = base.resolve("modules");
        tb.createDirectories(modules);

        new JavacTask(tb, Task.Mode.CMDLINE)
                .options("-XDrawDiagnostics",
                        "--module-source-path", "m0x=" + base.resolve("src0"),
                        "--module-source-path", "m1x=" + base.resolve("src1"),
                        "--module-source-path", "m2x=" + base.resolve("src2"),
                        "--module-source-path", "m3x=" + base.resolve("src3"))
                .files(findJavaFiles(base.resolve(srcDirs[srcDirs.length - 1])))
                .outdir(modules)
                .run()
                .writeAll();

        for (int i = 0; i < srcDirs.length; i++) {
            checkFiles(modules.resolve("m" + i + "x/module-info.class"));
        }
        checkFiles(modules.resolve("m3x/pkg3/A.class"));
    }

    @Test
    public void modulePatternWithEquals(Path base) throws Exception {
        // The dirs for the modules contain an '=' character, but
        // the option should still be recognized as the module pattern form.
        String[] srcDirs = {
                "src=",         // m0x
                "src=",         // m1x
                "src=",         // m2x
                "src="  // m3x
        };
        generateModules(base, true, srcDirs);

        final Path modules = base.resolve("modules");
        tb.createDirectories(modules);

        new JavacTask(tb, Task.Mode.CMDLINE)
                .options("-XDrawDiagnostics",
                        "--module-source-path", base.resolve("src=").toString())
                .files(findJavaFiles(base.resolve(srcDirs[srcDirs.length - 1])))
                .outdir(modules)
                .run()
                .writeAll();

        for (int i = 0; i < srcDirs.length; i++) {
            checkFiles(modules.resolve("m" + i + "x/module-info.class"));
        }
        checkFiles(modules.resolve("m3x/pkg3/A.class"));
    }

    @Test
    public void duplicateModuleSpecificForms(Path base) throws Exception {
        // The dirs for the modules do not use a subdirectory named for the module,
        // meaning they can only be used by the module-specific form of the option.
        String[] srcDirs = {
                "src0",         // m0x
                "src1",         // m1x
                "src2",         // m2x
                "src3"          // m3x
        };
        generateModules(base, false, srcDirs);

        final Path modules = base.resolve("modules");
        tb.createDirectories(modules);

        // in the following, it should not matter that src1 does not contain
        // a definition of m0x; it is bad/wrong to specify the option for m0x twice.
        String log = new JavacTask(tb, Task.Mode.CMDLINE)
                .options("-XDrawDiagnostics",
                        "--module-source-path", "m0x=" + base.resolve("src0"),
                        "--module-source-path", "m0x=" + base.resolve("src1"))
                .files(findJavaFiles(base.resolve(srcDirs[srcDirs.length - 1])))
                .outdir(modules)
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("error: --module-source-path specified more than once for module m0x"))
            throw new Exception("Expected error message not found");
    }

    @Test
    public void duplicateModulePatternForms(Path base) throws Exception {
        // module-specific subdirs are used to allow for use of module-pattern form
        String[] srcDirs = {
                "src",  // m0x
                "src",  // m1x
                "src",  // m2x
                "src"   // m3x
        };
        generateModules(base, true, srcDirs);

        final Path modules = base.resolve("modules");
        tb.createDirectories(modules);

        // in the following, it should not matter that the same pattern
        // is used for both occurrences; it is bad/wrong to give any two patterns
        String log = new JavacTask(tb, Task.Mode.CMDLINE)
                .options("-XDrawDiagnostics",
                        "--module-source-path", base.resolve("src").toString(),
                        "--module-source-path", base.resolve("src").toString())
                .files(findJavaFiles(base.resolve(srcDirs[srcDirs.length - 1])))
                .outdir(modules)
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("error: --module-source-path specified more than once with a pattern argument"))
            throw new Exception("Expected error message not found");
    }

    @Test
    public void mixedOptionForms(Path base) throws Exception {
        // The dirs for m0x, m2x use a subdirectory named for the module,
        // meaning they can be used in the module pattern form of the option;
        // the dirs for m1x, m3x do not use a subdirectory named for the module,
        // meaning they can only be used by the module-specific form of the option
        String[] srcDirs = {
                "src/m0x",      // m0x
                "src1",         // m1x
                "src/m2x",      // m2x
                "src3"          // m3x
        };
        generateModules(base, false, srcDirs);

        final Path modules = base.resolve("modules");
        tb.createDirectories(modules);

        new JavacTask(tb, Task.Mode.CMDLINE)
                .options("-XDrawDiagnostics",
                        "--module-source-path", base.resolve("src").toString(), // for m0x, m2x
                        "--module-source-path", "m1x=" + base.resolve("src1"),
                        "--module-source-path", "m3x=" + base.resolve("src3"))
                .files(findJavaFiles(base.resolve(srcDirs[srcDirs.length - 1])))
                .outdir(modules)
                .run()
                .writeAll();

        for (int i = 0; i < srcDirs.length; i++) {
            checkFiles(modules.resolve("m" + i + "x/module-info.class"));
        }
        checkFiles(modules.resolve("m3x/pkg3/A.class"));
    }

    private void generateModules(Path base, String... paths) throws IOException {
        generateModules(base, true, paths);
    }

    private void generateModules(Path base, boolean useModuleSubdirs, String... paths)
                throws IOException {
        for (int i = 0; i < paths.length; i++) {
            String moduleName = "m" + i + "x";
            String dependency = i > 0 ? "requires m" + (i - 1) + "x;" : "";
            Path dir = base.resolve(paths[i]);
            if (useModuleSubdirs) {
                dir = dir.resolve(moduleName);
            }
            tb.writeJavaFiles(dir,
                    "module " + moduleName + " { " + dependency + " }",
                    "package pkg" + i + "; class A { }");
        }
    }

    private void checkFiles(Path... files) throws Exception {
        for (Path file : files) {
            if (!Files.exists(file)) {
                throw new Exception("File not found: " + file);
            }
        }
    }

    private void checkLocation(Iterable<? extends Path> locn, List<Path> ref) throws Exception {
        List<Path> list = asList(locn);
        if (!list.equals(ref)) {
            out.println("expect: " + ref);
            out.println(" found: " + list);
            throw new Exception("location not as expected");
        }
    }

    private <T> List<T> asList(Iterable<? extends T> iter) {
        List<T> list = new ArrayList<>();
        for (T item : iter) {
            list.add(item);
        }
        return list;
    }
}
