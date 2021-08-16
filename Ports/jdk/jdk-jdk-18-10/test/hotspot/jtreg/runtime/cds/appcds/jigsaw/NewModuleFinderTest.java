/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

/**
 * @test
 * @bug 8244778
 * @requires vm.cds
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @run driver NewModuleFinderTest
 * @summary Make sure the archived module graph can co-exist with modules that are
 *          dynamically defined at runtime using the ModuleFinder API.
 */

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.cds.CDSTestUtils;

public class NewModuleFinderTest {

    private static final Path USER_DIR = Paths.get(CDSTestUtils.getOutputDir());
    private static final String TEST_SRC = System.getProperty("test.src");
    private static final Path SRC_DIR = Paths.get(TEST_SRC, "modulepath/src");
    private static final Path MODS_DIR = Paths.get("mods");

    // the module name of the test module
    private static final String TEST_MODULE = "com.simple";

    // the module main class
    private static final String MAIN_CLASS = "com.simple.Main";

    private static final Set<String> modules = Set.of(TEST_MODULE);

    public static void buildTestModule() throws Exception {
        // javac -d mods/$TESTMODULE --module-path MOD_DIR modulepath/src/$TESTMODULE/**
        JarBuilder.compileModule(SRC_DIR.resolve(TEST_MODULE),
                                 MODS_DIR.resolve(TEST_MODULE),
                                 MODS_DIR.toString());
    }

    public static void main(String... args) throws Exception {
        // compile the modules and create the modular jar files
        buildTestModule();

        CDSOptions opts = (new CDSOptions())
            .setUseVersion(false)
            .setXShareMode("auto")
            .addSuffix("-Xlog:cds",
                       "-Xlog:module=debug",
                       "-Dtest.src=" + TEST_SRC,
                       "NewModuleFinderTest$Helper");
        CDSTestUtils.run(opts)
            .assertNormalExit(output -> {
                output.shouldContain("define_module(): creation of module: com.simple,");
            });
    }

    static class Helper {
        public static void main(String... args) {
            ModuleFinder finder = ModuleFinder.of(MODS_DIR);
            Configuration parent = ModuleLayer.boot().configuration();
            Configuration cf = parent.resolveAndBind(ModuleFinder.of(),
                                                     finder,
                                                     modules);

            ClassLoader scl = ClassLoader.getSystemClassLoader();
            ModuleLayer layer = ModuleLayer.boot().defineModulesWithManyLoaders(cf, scl);

            Module m1 = layer.findModule(TEST_MODULE).get();
            System.out.println("Module = " + m1);
            if (m1 != null) {
                System.out.println("Success");
            } else {
                throw new RuntimeException("Module should not be null");
            }
        }
    }
}
