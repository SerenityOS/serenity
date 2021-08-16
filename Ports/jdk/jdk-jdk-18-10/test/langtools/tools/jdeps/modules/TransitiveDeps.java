/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Tests jdeps -m and -mp options on named modules and unnamed modules
 * @library ../lib
 * @build CompilerUtils JdepsUtil
 * @modules jdk.jdeps/com.sun.tools.jdeps
 * @run testng TransitiveDeps
 */

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


import com.sun.tools.jdeps.DepsAnalyzer;
import com.sun.tools.jdeps.Graph;
import org.testng.annotations.DataProvider;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

public class TransitiveDeps {
    private static final String TEST_SRC = System.getProperty("test.src");

    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MODS_DIR = Paths.get("mods");
    private static final Path LIBS_DIR = Paths.get("libs");

    // the names of the modules in this test
    private static String[] modules = new String[] {"unsafe", "mVI", "mVII"};
    /**
     * Compiles all modules used by the test
     */
    @BeforeTest
    public void compileAll() throws Exception {
        CompilerUtils.cleanDir(MODS_DIR);
        CompilerUtils.cleanDir(LIBS_DIR);

        for (String mn : modules) {
            // compile a module
            assertTrue(CompilerUtils.compileModule(SRC_DIR, MODS_DIR, mn));

            // create JAR files with no module-info.class
            Path root = MODS_DIR.resolve(mn);
            try (Stream<Path> stream = Files.walk(root, Integer.MAX_VALUE)) {
                Stream<Path> entries = stream.filter(f -> {
                    String fn = f.getFileName().toString();
                    return fn.endsWith(".class") && !fn.equals("module-info.class");
                });
                JdepsUtil.createJar(LIBS_DIR.resolve(mn + ".jar"), root, entries);
            }
        }
    }

    @DataProvider(name = "modules")
    public Object[][] expected1() {
        return new Object[][]{
            { "mVII",
               List.of(new ModuleMetaData("mVII")
                            .requires("mVI")
                            .requires("unsafe")
                            .reference("p7.Main", "java.lang.Object", "java.base")
                            .reference("p7.Main", "java.lang.String", "java.base")
                            .reference("p7.Main", "org.safe.Lib", "unsafe")
                            .reference("p7.Main", "p6.safe.Lib", "mVI"),
                        new ModuleMetaData("mVI")
                            .requires("unsafe")
                            .reference("p6.indirect.UnsafeRef", "java.lang.Object", "java.base")
                            .reference("p6.indirect.UnsafeRef", "org.unsafe.UseUnsafe ", "unsafe")
                            .reference("p6.safe.Lib", "java.io.PrintStream", "java.base")
                            .reference("p6.safe.Lib", "java.lang.Class", "java.base")
                            .reference("p6.safe.Lib", "java.lang.Object", "java.base")
                            .reference("p6.safe.Lib", "java.lang.String", "java.base")
                            .reference("p6.safe.Lib", "java.lang.System", "java.base")
                            .reference("p6.safe.Lib", "org.safe.Lib", "unsafe"),
                        new ModuleMetaData("unsafe")
                            .requires("jdk.unsupported")
                            .reference("org.indirect.UnsafeRef", "java.lang.Object", "java.base")
                            .reference("org.safe.Lib", "java.io.PrintStream", "java.base")
                            .reference("org.safe.Lib", "java.lang.Class", "java.base")
                            .reference("org.safe.Lib", "java.lang.Object", "java.base")
                            .reference("org.safe.Lib", "java.lang.String", "java.base")
                            .reference("org.safe.Lib", "java.lang.System", "java.base")
                            .reference("org.unsafe.UseUnsafe", "java.lang.Object", "java.base")
                            .jdkInternal("org.unsafe.UseUnsafe", "sun.misc.Unsafe", "java.base")
                        )
            },
        };
    }

    @Test(dataProvider = "modules")
    public void testModulePath(String name, List<ModuleMetaData> data) throws IOException {
        Set<String> roots = Set.of("mVI", "unsafe");

        String cmd1 = String.format("jdeps --module-path %s --add-modules %s -m %s%n", MODS_DIR,
            roots.stream().collect(Collectors.joining(",")), name);
        try (JdepsUtil.Command jdeps = JdepsUtil.newCommand(cmd1)) {
            jdeps.verbose("-verbose:class")
                .appModulePath(MODS_DIR.toString())
                .addmods(roots)
                .addmods(Set.of(name));

            runJdeps(jdeps, data);
        }
        // run automatic modules
        roots = Set.of("ALL-MODULE-PATH", "jdk.unsupported");

        String cmd2 = String.format("jdeps --module-path %s --add-modules %s -m %s%n", LIBS_DIR,
            roots.stream().collect(Collectors.joining(",")), name);

        try (JdepsUtil.Command jdeps = JdepsUtil.newCommand(cmd2)) {
            jdeps.verbose("-verbose:class")
                .appModulePath(LIBS_DIR.toString())
                .addmods(roots)
                .addmods(Set.of(name));

            runJdeps(jdeps, data);
        }
    }

    @DataProvider(name = "jars")
    public Object[][] expected2() {
        return new Object[][]{
            { "mVII", List.of(new ModuleMetaData("mVII.jar")
                                .requires("mVI.jar")
                                .requires("unsafe.jar")
                                .reference("p7.Main", "java.lang.Object", "java.base")
                                .reference("p7.Main", "java.lang.String", "java.base")
                                .reference("p7.Main", "org.safe.Lib", "unsafe.jar")
                                .reference("p7.Main", "p6.safe.Lib", "mVI.jar"))
            },
        };
    }

    @Test(dataProvider = "jars")
    public void testClassPath(String name, List<ModuleMetaData> data) throws IOException {
        String cpath = Arrays.stream(modules)
            .filter(mn -> !mn.equals(name))
            .map(mn -> LIBS_DIR.resolve(mn + ".jar").toString())
            .collect(Collectors.joining(File.pathSeparator));

        Path jarfile = LIBS_DIR.resolve(name + ".jar");

        String cmd = String.format("jdeps -classpath %s %s%n", cpath, jarfile);
        try (JdepsUtil.Command jdeps = JdepsUtil.newCommand(cmd)) {
            jdeps.verbose("-verbose:class")
                .addClassPath(cpath)
                .addRoot(jarfile);

            runJdeps(jdeps, data);
        }
    }

    @DataProvider(name = "compileTimeView")
    public Object[][] expected3() {
        return new Object[][] {
            {"mVII",
             List.of(new ModuleMetaData("mVII.jar")
                        .requires("mVI.jar")
                        .requires("unsafe.jar")
                        .reference("p7.Main", "java.lang.Object", "java.base")
                        .reference("p7.Main", "java.lang.String", "java.base")
                        .reference("p7.Main", "org.safe.Lib", "unsafe.jar")
                        .reference("p7.Main", "p6.safe.Lib", "mVI.jar"),
                    new ModuleMetaData("mVI.jar")
                        .requires("unsafe.jar")
                        .reference("p6.indirect.UnsafeRef", "java.lang.Object", "java.base")
                        .reference("p6.indirect.UnsafeRef", "org.unsafe.UseUnsafe ", "unsafe.jar")
                        .reference("p6.safe.Lib", "java.io.PrintStream", "java.base")
                        .reference("p6.safe.Lib", "java.lang.Class", "java.base")
                        .reference("p6.safe.Lib", "java.lang.Object", "java.base")
                        .reference("p6.safe.Lib", "java.lang.String", "java.base")
                        .reference("p6.safe.Lib", "java.lang.System", "java.base")
                        .reference("p6.safe.Lib", "org.safe.Lib", "unsafe.jar"),
                    new ModuleMetaData("unsafe.jar")
                        .requires("jdk.unsupported")
                        .reference("org.indirect.UnsafeRef", "java.lang.Object", "java.base")
                        .reference("org.safe.Lib", "java.io.PrintStream", "java.base")
                        .reference("org.safe.Lib", "java.lang.Class", "java.base")
                        .reference("org.safe.Lib", "java.lang.Object", "java.base")
                        .reference("org.safe.Lib", "java.lang.String", "java.base")
                        .reference("org.safe.Lib", "java.lang.System", "java.base")
                        .reference("org.unsafe.UseUnsafe", "java.lang.Object", "java.base")
                        .jdkInternal("org.unsafe.UseUnsafe", "sun.misc.Unsafe", "java.base")
                )
            },
        };
    }

    @Test(dataProvider = "compileTimeView")
    public void compileTimeView(String name, List<ModuleMetaData> data) throws IOException {
        String cpath = Arrays.stream(modules)
                .filter(mn -> !mn.equals(name))
                .map(mn -> LIBS_DIR.resolve(mn + ".jar").toString())
                .collect(Collectors.joining(File.pathSeparator));

        Path jarfile = LIBS_DIR.resolve(name + ".jar");

        String cmd = String.format("jdeps -ct -classpath %s %s%n", cpath, jarfile);
        try (JdepsUtil.Command jdeps = JdepsUtil.newCommand(cmd)) {
            jdeps.verbose("-verbose:class")
                .addClassPath(cpath)
                .addRoot(jarfile);

            runJdeps(jdeps, data, true, 0 /* -recursive */);
        }
    }

    @DataProvider(name = "recursiveDeps")
    public Object[][] expected4() {
        return new Object[][] {
            {"mVII",
                List.of(new ModuleMetaData("mVII.jar")
                        .requires("mVI.jar")
                        .requires("unsafe.jar")
                        .reference("p7.Main", "java.lang.Object", "java.base")
                        .reference("p7.Main", "java.lang.String", "java.base")
                        .reference("p7.Main", "org.safe.Lib", "unsafe.jar")
                        .reference("p7.Main", "p6.safe.Lib", "mVI.jar"),
                    new ModuleMetaData("mVI.jar")
                        .requires("unsafe.jar")
                        .reference("p6.safe.Lib", "java.io.PrintStream", "java.base")
                        .reference("p6.safe.Lib", "java.lang.Class", "java.base")
                        .reference("p6.safe.Lib", "java.lang.Object", "java.base")
                        .reference("p6.safe.Lib", "java.lang.String", "java.base")
                        .reference("p6.safe.Lib", "java.lang.System", "java.base")
                        .reference("p6.safe.Lib", "org.safe.Lib", "unsafe.jar"),
                    new ModuleMetaData("unsafe.jar")
                        .requires("jdk.unsupported")
                        .reference("org.indirect.UnsafeRef", "java.lang.Object", "java.base")
                        .reference("org.safe.Lib", "java.io.PrintStream", "java.base")
                        .reference("org.safe.Lib", "java.lang.Class", "java.base")
                        .reference("org.safe.Lib", "java.lang.Object", "java.base")
                        .reference("org.safe.Lib", "java.lang.String", "java.base")
                        .reference("org.safe.Lib", "java.lang.System", "java.base")
                )
            },
        };
    }
    @Test(dataProvider = "recursiveDeps")
    public void recursiveDeps(String name, List<ModuleMetaData> data) throws IOException {
        String cpath = Arrays.stream(modules)
            .filter(mn -> !mn.equals(name))
            .map(mn -> LIBS_DIR.resolve(mn + ".jar").toString())
            .collect(Collectors.joining(File.pathSeparator));

        Path jarfile = LIBS_DIR.resolve(name + ".jar");

        String cmd = String.format("jdeps -R -classpath %s %s%n", cpath, jarfile);
        try (JdepsUtil.Command jdeps = JdepsUtil.newCommand(cmd)) {
            jdeps.verbose("-verbose:class").filter("-filter:archive")
                .addClassPath(cpath)
                .addRoot(jarfile);

            runJdeps(jdeps, data, true, 0 /* -recursive */);
        }
    }

    private void runJdeps(JdepsUtil.Command jdeps, List<ModuleMetaData> data)
        throws IOException
    {
        runJdeps(jdeps, data, false, 1 /* depth */);
    }

    private void runJdeps(JdepsUtil.Command jdeps, List<ModuleMetaData> data,
                          boolean compileTimeView, int depth)
        throws IOException
    {
        // run the analyzer
        DepsAnalyzer analyzer = jdeps.getDepsAnalyzer();
        assertTrue(analyzer.run(compileTimeView, depth));
        jdeps.dumpOutput(System.err);

        // analyze result
        Graph<DepsAnalyzer.Node> g1 = analyzer.moduleGraph();
        Map<String, ModuleMetaData> dataMap = data.stream()
            .collect(Collectors.toMap(ModuleMetaData::name, Function.identity()));

        // the returned graph contains all nodes such as java.base and jdk.unsupported
        g1.nodes().stream()
            .filter(u -> dataMap.containsKey(u.name))
            .forEach(u -> {
                ModuleMetaData md = dataMap.get(u.name);
                md.checkRequires(u.name, g1.adjacentNodes(u));
            });

        Graph<DepsAnalyzer.Node> g2 = analyzer.dependenceGraph();

        g2.nodes().stream()
            .filter(u -> dataMap.containsKey(u.name))
            .forEach(u -> {
                ModuleMetaData md = dataMap.get(u.name);
                md.checkDependences(u.name, g2.adjacentNodes(u));
            });
    }
}
