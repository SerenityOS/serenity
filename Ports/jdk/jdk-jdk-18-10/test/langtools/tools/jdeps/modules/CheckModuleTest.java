/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Tests split packages
 * @library ../lib
 * @build CompilerUtils JdepsUtil
 * @modules jdk.jdeps/com.sun.tools.jdeps
 * @run testng CheckModuleTest
 */

import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import com.sun.tools.jdeps.ModuleAnalyzer;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertEquals;


public class CheckModuleTest {
    private static final String TEST_SRC = System.getProperty("test.src");
    private static final String TEST_CLASSES = System.getProperty("test.classes");

    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MODS_DIR = Paths.get("mods");

    // mIV and mV are analyzed.  Others are compiled to make sure they are present
    // on the module path for analysis
    private static final Set<String> modules = Set.of("unsafe", "mIV", "mV", "mVI", "mVII", "mVIII");

    private static final String JAVA_BASE = "java.base";
    private static final String JAVA_COMPILER = "java.compiler";

    /**
     * Compiles classes used by the test
     */
    @BeforeTest
    public void compileAll() throws Exception {
        CompilerUtils.cleanDir(MODS_DIR);
        modules.forEach(mn ->
            assertTrue(CompilerUtils.compileModule(SRC_DIR, MODS_DIR, mn)));
    }

    @DataProvider(name = "javaBase")
    public Object[][] base() {
        return new Object[][] {
            { JAVA_BASE, new ModuleMetaData(JAVA_BASE)
            },
            { JAVA_COMPILER, new ModuleMetaData(JAVA_BASE)
            },
        };
    };

    @Test(dataProvider = "javaBase")
    public void testJavaBase(String name, ModuleMetaData data) throws Exception {
        String cmd = String.format("jdeps --check %s --module-path %s%n", name, MODS_DIR);
        try (JdepsUtil.Command jdeps = JdepsUtil.newCommand(cmd)) {
            jdeps.appModulePath(MODS_DIR.toString());

            ModuleAnalyzer analyzer = jdeps.getModuleAnalyzer(Set.of(name));
            assertTrue(analyzer.run(false));
            jdeps.dumpOutput(System.err);

            ModuleDescriptor[] descriptors = analyzer.descriptors(name);
            for (int i = 0; i < 3; i++) {
                descriptors[i].requires().stream()
                    /* jcov has a dependency on java.logging, just ignore it in case this test is being executed with jcov
                     * this dependency from jcov should be fixed once bug: CODETOOLS-7902642 gets fixed
                     */
                    .filter(req -> !req.toString().equals("java.logging"))
                    .forEach(req -> data.checkRequires(req));
            }
        }
    }

    @DataProvider(name = "modules")
    public Object[][] unnamed() {
        return new Object[][]{
            { "mIV", new ModuleMetaData[] {
                        // original
                        new ModuleMetaData("mIV")
                            .requiresTransitive("java.compiler")
                            .requires("java.logging")
                            // unnused exports
                            .exports("p4.internal", Set.of("mVI", "mVII")),
                        // suggested version
                        new ModuleMetaData("mIV")
                            .requires("java.compiler"),
                        // reduced version
                        new ModuleMetaData("mIV")
                            .requires("java.compiler")
                    }
            },
            { "mV", new ModuleMetaData[] {
                        // original
                        new ModuleMetaData("mV")
                            .requiresTransitive("java.compiler")
                            .requiresTransitive("java.logging")
                            .requires("java.sql")
                            .requiresTransitive("mIV"),
                        // suggested version
                        new ModuleMetaData("mV")
                            .requiresTransitive("java.compiler")
                            .requires("java.logging")
                            .requiresTransitive("java.sql")
                            .requiresTransitive("mIV"),
                        // reduced version
                        new ModuleMetaData("mV")
                            .requiresTransitive("java.compiler")
                            .requiresTransitive("java.sql")
                            .requiresTransitive("mIV"),
                    }
            },
        };
    }

    @Test(dataProvider = "modules")
    public void modularTest(String name, ModuleMetaData[] data) throws Exception {
        String cmd = String.format("jdeps --check %s --module-path %s%n", name, MODS_DIR);

        try (JdepsUtil.Command jdeps = JdepsUtil.newCommand(cmd)) {
            jdeps.appModulePath(MODS_DIR.toString());

            ModuleAnalyzer analyzer = jdeps.getModuleAnalyzer(Set.of(name));
            assertTrue(analyzer.run(false));
            jdeps.dumpOutput(System.err);

            // compare the module descriptors and the suggested versions
            ModuleDescriptor[] descriptors = analyzer.descriptors(name);
            for (int i = 0; i < 3; i++) {
                ModuleMetaData metaData = data[i];
                descriptors[i].requires().stream()
                    .forEach(req -> metaData.checkRequires(req));
            }

            Map<String, Set<String>> unused = analyzer.unusedQualifiedExports(name);
            // verify unuused qualified exports
            assertEquals(unused, data[0].exports);
        }
    }

}
