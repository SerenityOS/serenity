/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8137317 8139238 8210408
 * @summary Visibility tests for ResourceBundle.getBundle with and without
 *          an unnamed module argument.
 * @library /test/lib
 *          ..
 * @build jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Utils
 *        jdk.test.lib.compiler.CompilerUtils
 *        jdk.test.lib.process.ProcessTools
 *        ModuleTestUtil
 * @run testng VisibilityTest
 */

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.Utils;
import jdk.test.lib.process.ProcessTools;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

@Test
public class VisibilityTest {
    private static final Path SRC_DIR = Paths.get(Utils.TEST_SRC, "src");
    private static final Path MODS_DIR = Paths.get(Utils.TEST_CLASSES, "mods");
    private static final Path CLASSES_DIR = Paths.get(Utils.TEST_CLASSES, "classes");
    private static final Path NAMED_BUNDLES_DIR = MODS_DIR.resolve("named.bundles");
    private static final Path EXPORTED_NAMED_BUNDLES_DIR = MODS_DIR.resolve("exported.named.bundles");

    private static final List<String> MODULE_LIST = List.of("embargo",
            "exported.named.bundles", "named.bundles", "test");

    @BeforeTest
    public void prepareTestEnv() throws Throwable {
        MODULE_LIST.forEach(mn -> ModuleTestUtil.prepareModule(SRC_DIR,
                MODS_DIR, mn, ".properties"));

        // Prepare resource bundles in an unnamed module
        ModuleTestUtil.compilePkg(SRC_DIR, CLASSES_DIR, "pkg");
        ModuleTestUtil.copyResFiles(SRC_DIR, CLASSES_DIR, "pkg", ".properties");

    }

    /**
     * Package jdk.test is in named module "test".
     * Package jdk.embargo is in named module "embargo".
     *
     * jdk.{test,embargo}.TestWithUnnamedModuleArg call:
     *     ResourceBundle.getBundle(basename, classloader.getUnnamedModule())
     *     where classloader is the TCCL or system class loader.
     * jdk.{test,embargo}.TestWithNoModuleArg call:
     *     ResourceBundle.getBundle(basename)
     *
     * jdk.test.resources[.exported].classes.* are class-based resource bundles.
     * jdk.test.resources[.exported].props.* are properties file-based resource bundles.
     *
     * Packages jdk.test.resources.{classes,props} in named module "named.bundles"
     * are exported only to named module "test".
     * Packages jdk.test.resources.exported.{classes,props} in named module
     * "exported.named.bundle" are exported to unnamed modules.
     */

    @DataProvider(name = "RunWithTestResData")
    Object[][] RunWithTestResData() {
        return new Object[][] {
                // Tests using jdk.test.TestWithNoModuleArg and jdk.embargo.TestWithNoModuleArg.
                // Neither of which specifies an unnamed module with ResourceBundle.getBundle().

                // jdk.test.resources.{classes,props}.* are available only to
                // named module "test" by ResourceBundleProvider.
                {List.of("-p", MODS_DIR.toString(),
                        "-m", "test/jdk.test.TestWithNoModuleArg",
                        "jdk.test.resources.classes.MyResources", "true")},
                {List.of("-p", MODS_DIR.toString(),
                        "-m", "test/jdk.test.TestWithNoModuleArg",
                        "jdk.test.resources.props.MyResources", "true")},
                {List.of("-p", MODS_DIR.toString(),
                        "-m", "embargo/jdk.embargo.TestWithNoModuleArg",
                        "jdk.test.resources.classes.MyResources", "false")},
                {List.of("-p", MODS_DIR.toString(),
                        "-m", "embargo/jdk.embargo.TestWithNoModuleArg",
                        "jdk.test.resources.props.MyResources", "false")},

                // Add mods/named.bundles to the class path.
                {List.of("-cp", NAMED_BUNDLES_DIR.toString(),
                        "-p", MODS_DIR.toString(),
                        "-m", "test/jdk.test.TestWithNoModuleArg",
                        "jdk.test.resources.classes.MyResources", "true")},
                {List.of("-cp", NAMED_BUNDLES_DIR.toString(),
                        "-p", MODS_DIR.toString(),
                        "-m", "test/jdk.test.TestWithNoModuleArg",
                        "jdk.test.resources.props.MyResources", "true")},
                {List.of("-cp", NAMED_BUNDLES_DIR.toString(),
                        "-p", MODS_DIR.toString(),
                        "-m", "embargo/jdk.embargo.TestWithNoModuleArg",
                        "jdk.test.resources.classes.MyResources", "true")},
                {List.of("-cp", NAMED_BUNDLES_DIR.toString(),
                        "-p", MODS_DIR.toString(),
                        "-m", "embargo/jdk.embargo.TestWithNoModuleArg",
                        "jdk.test.resources.props.MyResources", "true")},

                // Tests using jdk.test.TestWithUnnamedModuleArg and
                // jdk.embargo.TestWithUnnamedModuleArg.
                // Both of which specify an unnamed module with ResourceBundle.getBundle.

                // jdk.test.resources.classes is exported to named module "test".
                // IllegalAccessException is thrown in ResourceBundle.Control.newBundle().
                {List.of("-p", MODS_DIR.toString(),
                        "-m", "test/jdk.test.TestWithUnnamedModuleArg",
                        "jdk.test.resources.classes.MyResources", "false")},

                // jdk.test.resources.props is exported to named module "test".
                // loader.getResource() doesn't find jdk.test.resources.props.MyResources.
                {List.of("-p", MODS_DIR.toString(),
                        "-m", "test/jdk.test.TestWithUnnamedModuleArg",
                        "jdk.test.resources.props.MyResources", "false")},

                // IllegalAccessException is thrown in ResourceBundle.Control.newBundle().
                {List.of("-p", MODS_DIR.toString(),
                        "-m", "embargo/jdk.embargo.TestWithUnnamedModuleArg",
                        "jdk.test.resources.classes.MyResources", "false")},

                // jdk.test.resources.props is exported to named module "test".
                // loader.getResource() doesn't find jdk.test.resources.props.MyResources.
                {List.of("-p", MODS_DIR.toString(),
                        "-m", "embargo/jdk.embargo.TestWithUnnamedModuleArg",
                        "jdk.test.resources.props.MyResources", "false")},

                // Add mods/named.bundles to the class path.

                // IllegalAccessException is thrown in ResourceBundle.Control.newBundle().
                {List.of("-cp", NAMED_BUNDLES_DIR.toString(),
                        "-p", MODS_DIR.toString(),
                        "-m", "test/jdk.test.TestWithUnnamedModuleArg",
                        "jdk.test.resources.classes.MyResources", "false")},

                // loader.getResource() finds jdk.test.resources.exported.props.MyResources.
                {List.of("-cp", NAMED_BUNDLES_DIR.toString(),
                        "-p", MODS_DIR.toString(),
                        "-m", "test/jdk.test.TestWithUnnamedModuleArg",
                        "jdk.test.resources.props.MyResources", "true")},

                // jdk.test.resources.exported.classes.MyResources is treated
                // as if the class is in an unnamed module.
                {List.of("-cp", NAMED_BUNDLES_DIR.toString(),
                        "-p", MODS_DIR.toString(),
                        "-m", "embargo/jdk.embargo.TestWithUnnamedModuleArg",
                        "jdk.test.resources.classes.MyResources", "true")},

                // loader.getResource() finds jdk.test.resources.exported.props.MyResources.
                {List.of("-cp", NAMED_BUNDLES_DIR.toString(),
                        "-p", MODS_DIR.toString(),
                        "-m", "embargo/jdk.embargo.TestWithUnnamedModuleArg",
                        "jdk.test.resources.props.MyResources", "true")},
        };
    }

    @DataProvider(name = "RunWithExportedResData")
    Object[][] RunWithExportedResData() {
        return new Object[][] {
                // Tests using jdk.test.TestWithNoModuleArg and jdk.embargo.TestWithNoModuleArg
                // neither of which specifies an unnamed module with ResourceBundle.getBundle.

                // None of jdk.test.resources.exported.** is available to the named modules.
                {List.of("-p", MODS_DIR.toString(),
                        "-m", "test/jdk.test.TestWithNoModuleArg",
                        "jdk.test.resources.exported.classes.MyResources", "false")},
                {List.of("-p", MODS_DIR.toString(),
                        "-m", "test/jdk.test.TestWithNoModuleArg",
                        "jdk.test.resources.exported.props.MyResources", "false")},
                {List.of("-p", MODS_DIR.toString(),
                        "-m", "embargo/jdk.embargo.TestWithNoModuleArg",
                        "jdk.test.resources.exported.classes.MyResources", "false")},
                {List.of("-p", MODS_DIR.toString(),
                        "-m", "embargo/jdk.embargo.TestWithNoModuleArg",
                        "jdk.test.resources.exported.props.MyResources", "false")},

                // Add mods/exported.named.bundles to the class path.
                {List.of("-cp", EXPORTED_NAMED_BUNDLES_DIR.toString(),
                        "-p", MODS_DIR.toString(),
                        "-m", "test/jdk.test.TestWithNoModuleArg",
                        "jdk.test.resources.exported.classes.MyResources", "true")},
                {List.of("-cp", EXPORTED_NAMED_BUNDLES_DIR.toString(),
                        "-p", MODS_DIR.toString(),
                        "-m", "test/jdk.test.TestWithNoModuleArg",
                        "jdk.test.resources.exported.props.MyResources", "true")},
                {List.of("-cp", EXPORTED_NAMED_BUNDLES_DIR.toString(),
                        "-p", MODS_DIR.toString(),
                        "-m", "embargo/jdk.embargo.TestWithNoModuleArg",
                        "jdk.test.resources.exported.classes.MyResources", "true")},
                {List.of("-cp", EXPORTED_NAMED_BUNDLES_DIR.toString(),
                        "-p", MODS_DIR.toString(),
                        "-m", "embargo/jdk.embargo.TestWithNoModuleArg",
                        "jdk.test.resources.exported.props.MyResources", "true")},

                // Tests using jdk.test.TestWithUnnamedModuleArg and
                // jdk.embargo.TestWithUnnamedModuleArg which specify
                // an unnamed module with ResourceBundle.getBundle.

                // loader.loadClass() doesn't find jdk.test.resources.exported.classes.MyResources
                // and throws a ClassNotFoundException.
                {List.of("-p", MODS_DIR.toString(),
                        "-m", "test/jdk.test.TestWithUnnamedModuleArg",
                        "jdk.test.resources.exported.classes.MyResources", "false")},

                // The properties files in jdk.test.resources.exported.props
                // are not found with loader.getResource().
                {List.of("-p", MODS_DIR.toString(),
                        "-m", "test/jdk.test.TestWithUnnamedModuleArg",
                        "jdk.test.resources.exported.props.MyResources", "false")},

                // loader.loadClass() doesn't find jdk.test.resources.exported.classes.MyResources
                // and throws a ClassNotFoundException.
                {List.of("-p", MODS_DIR.toString(),
                        "-m", "embargo/jdk.embargo.TestWithUnnamedModuleArg",
                        "jdk.test.resources.exported.classes.MyResources", "false")},

                // The properties files in jdk.test.resources.exported.props are not found
                // with loader.getResource().
                {List.of("-p", MODS_DIR.toString(),
                        "-m", "embargo/jdk.embargo.TestWithUnnamedModuleArg",
                        "jdk.test.resources.exported.props.MyResources", "false")},

                // Add mods/exported.named.bundles to the class path.

                // jdk.test.resources.exported.classes.MyResources.getModule().isNamed()
                // returns false.
                {List.of("-cp", EXPORTED_NAMED_BUNDLES_DIR.toString(),
                        "-p", MODS_DIR.toString(),
                        "-m", "test/jdk.test.TestWithUnnamedModuleArg",
                        "jdk.test.resources.exported.classes.MyResources", "true")},

                // loader.getResource() finds jdk.test.resources.exported.props.MyResources.
                {List.of("-cp", EXPORTED_NAMED_BUNDLES_DIR.toString(),
                        "-p", MODS_DIR.toString(),
                        "-m", "test/jdk.test.TestWithUnnamedModuleArg",
                        "jdk.test.resources.exported.props.MyResources", "true")},

                // jdk.test.resources.exported.classes.MyResources.getModule().isNamed()
                // returns false.
                {List.of("-cp", EXPORTED_NAMED_BUNDLES_DIR.toString(),
                        "-p", MODS_DIR.toString(),
                        "-m", "embargo/jdk.embargo.TestWithUnnamedModuleArg",
                        "jdk.test.resources.exported.classes.MyResources", "true")},

                // loader.getResource() finds jdk.test.resources.exported.props.MyResources.
                {List.of("-cp", EXPORTED_NAMED_BUNDLES_DIR.toString(),
                        "-p", MODS_DIR.toString(),
                        "-m", "embargo/jdk.embargo.TestWithUnnamedModuleArg",
                        "jdk.test.resources.exported.props.MyResources", "true")},

        };
    }

    @DataProvider(name = "RunWithPkgResData")
    Object[][] RunWithPkgResData() {
        return new Object[][] {
                // jdk.pkg.resources.* are in an unnamed module.
                // jdk.pkg.test.Main calls ResourceBundle.getBundle with an unnamed module.
                { List.of("-cp", CLASSES_DIR.resolve("pkg").toString(), "jdk.pkg.test.Main",
                        "jdk.pkg.resources.classes.MyResources", "true")},
                { List.of("-cp", CLASSES_DIR.resolve("pkg").toString(), "jdk.pkg.test.Main",
                        "jdk.pkg.resources.props.MyResources", "true")},
        };
    }

    /**
     * Test cases with jdk.test.resources.*
     */
    @Test(dataProvider = "RunWithTestResData")
    public void RunWithTestRes(List<String> argsList) throws Throwable {
        int exitCode = runCmd(argsList);
        assertEquals(exitCode, 0, "Execution of the tests with "
                + "jdk.test.resources.* failed. "
                + "Unexpected exit code: " + exitCode);
    }

    /**
     * Test cases with jdk.test.resources.exported.*
     */
    @Test(dataProvider = "RunWithExportedResData")
    public void RunWithExportedRes(List<String> argsList) throws Throwable {
        int exitCode = runCmd(argsList);
        assertEquals(exitCode, 0, "Execution of the tests with "
                + "jdk.test.resources.exported.* failed. "
                + "Unexpected exit code: " + exitCode);
    }

    /**
     * Test cases with jdk.pkg.resources.*
     */
    @Test(dataProvider = "RunWithPkgResData")
    public void RunWithPkgRes(List<String> argsList) throws Throwable {
        int exitCode = runCmd(argsList);
        assertEquals(exitCode, 0, "Execution of the tests with "
                + "jdk.pkg.resources.* failed. "
                + "Unexpected exit code: " + exitCode);
    }

    private int runCmd(List<String> argsList) throws Throwable {
        JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("java");
        launcher.addToolArg("-ea")
                .addToolArg("-esa");
        argsList.forEach(launcher::addToolArg);

        return ProcessTools.executeCommand(launcher.getCommand()).getExitValue();
    }
}