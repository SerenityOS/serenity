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
 * @summary Tests jdeps -m and --module-path options on named modules and unnamed modules
 * @library ../lib
 * @build CompilerUtils JdepsUtil
 * @modules jdk.jdeps/com.sun.tools.jdeps
 * @run testng ModuleTest
 */

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;


import com.sun.tools.jdeps.DepsAnalyzer;
import com.sun.tools.jdeps.Graph;
import org.testng.annotations.DataProvider;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

public class ModuleTest {
    private static final String TEST_SRC = System.getProperty("test.src");
    private static final String TEST_CLASSES = System.getProperty("test.classes");

    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MODS_DIR = Paths.get("mods");
    private static final Path UNNAMED_DIR = Paths.get("unnamed");

    // the names of the modules in this test
    private static final String UNSUPPORTED = "unsupported";
    private static String[] modules = new String[] {"mI", "mII", "mIII", "mIV", UNSUPPORTED};
    /**
     * Compiles all modules used by the test
     */
    @BeforeTest
    public void compileAll() throws Exception {
        CompilerUtils.cleanDir(MODS_DIR);
        CompilerUtils.cleanDir(UNNAMED_DIR);

        assertTrue(CompilerUtils.compileModule(SRC_DIR, MODS_DIR, UNSUPPORTED,
                                               "--add-exports", "java.base/jdk.internal.perf=" + UNSUPPORTED));
        // m4 is not referenced
        Arrays.asList("mI", "mII", "mIII", "mIV")
              .forEach(mn -> assertTrue(CompilerUtils.compileModule(SRC_DIR, MODS_DIR, mn)));

        assertTrue(CompilerUtils.compile(SRC_DIR.resolve("mIII"), UNNAMED_DIR, "-p", MODS_DIR.toString()));
        Files.delete(UNNAMED_DIR.resolve("module-info.class"));
    }

    @DataProvider(name = "modules")
    public Object[][] expected() {
        return new Object[][]{
                { "mIII", new ModuleMetaData("mIII").requiresTransitive("java.sql")
                            .requiresTransitive("mII")
                            .requires("java.logging")
                            .requiresTransitive("mI")
                            .reference("p3", "java.lang", "java.base")
                            .reference("p3", "java.sql", "java.sql")
                            .reference("p3", "java.util.logging", "java.logging")
                            .reference("p3", "p1", "mI")
                            .reference("p3", "p2", "mII")
                            .qualified("p3", "p2.internal", "mII")
                },
                { "mII", new ModuleMetaData("mII").requiresTransitive("mI")
                            .reference("p2", "java.lang", "java.base")
                            .reference("p2", "p1", "mI")
                            .reference("p2.internal", "java.lang", "java.base")
                            .reference("p2.internal", "java.io", "java.base")
                },
                { "mI", new ModuleMetaData("mI").requires("unsupported")
                            .reference("p1", "java.lang", "java.base")
                            .reference("p1.internal", "java.lang", "java.base")
                            .reference("p1.internal", "p1", "mI")
                            .reference("p1.internal", "q", "unsupported")
                },
                { "unsupported", new ModuleMetaData("unsupported")
                            .reference("q", "java.lang", "java.base")
                            .jdkInternal("q", "jdk.internal.perf", "java.base")
                },
        };
    }

    @Test(dataProvider = "modules")
    public void modularTest(String name, ModuleMetaData data) throws IOException {
        // jdeps --module-path mods -m <name>
        runTest(data, MODS_DIR.toString(), Set.of(name));

        // jdeps --module-path libs/mI.jar:.... -m <name>
        String mp = Arrays.stream(modules)
                .filter(mn -> !mn.equals(name))
                .map(mn -> MODS_DIR.resolve(mn).toString())
                .collect(Collectors.joining(File.pathSeparator));
        runTest(data, mp, Collections.emptySet(), MODS_DIR.resolve(name));
    }

    @DataProvider(name = "unnamed")
    public Object[][] unnamed() {
        return new Object[][]{
                { "unnamed", new ModuleMetaData("unnamed", false)
                            .depends("java.sql")
                            .depends("java.logging")
                            .depends("mI")
                            .depends("mII")
                            .reference("p3", "java.lang", "java.base")
                            .reference("p3", "java.sql", "java.sql")
                            .reference("p3", "java.util.logging", "java.logging")
                            .reference("p3", "p1", "mI")
                            .reference("p3", "p2", "mII")
                            .internal("p3", "p2.internal", "mII")
                },
        };
    }

    @Test(dataProvider = "unnamed")
    public void unnamedTest(String name, ModuleMetaData data) throws IOException {
        runTest(data, MODS_DIR.toString(), Set.of("mI", "mII"), UNNAMED_DIR);
    }

    private void runTest(ModuleMetaData data, String modulepath,
                         Set<String> roots, Path... paths)
        throws IOException
    {
        // jdeps --module-path <modulepath> -m root paths
        String cmd = String.format("jdeps --module-path %s --add-modules %s %s%n",
            MODS_DIR, roots.stream().collect(Collectors.joining(",")), paths);

        try (JdepsUtil.Command jdeps = JdepsUtil.newCommand(cmd)) {
            jdeps.appModulePath(modulepath)
                .addmods(roots);
            Arrays.stream(paths).forEach(jdeps::addRoot);

            // run the analyzer
            DepsAnalyzer analyzer = jdeps.getDepsAnalyzer();
            assertTrue(analyzer.run());

            // analyze result
            Graph<DepsAnalyzer.Node> g1 = analyzer.moduleGraph();
            g1.nodes().stream()
                .filter(u -> u.name.equals(data.moduleName))
                .forEach(u -> data.checkRequires(u.name, g1.adjacentNodes(u)));

            Graph<DepsAnalyzer.Node> g2 = analyzer.dependenceGraph();
            g2.nodes().stream()
                .filter(u -> u.name.equals(data.moduleName))
                .forEach(u -> data.checkDependences(u.name, g2.adjacentNodes(u)));

            jdeps.dumpOutput(System.err);
        }
    }
}
