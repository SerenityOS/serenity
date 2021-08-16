/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8044767 8139067 8210408 8263202
 * @summary Basic tests for ResourceBundle with modules:
 *          1) Named module "test" contains resource bundles for root and en,
 *          and separate named modules "eubundles" and "asiabundles" contain
 *          other resource bundles.
 *          2) ResourceBundle.getBundle caller is in named module "test",
 *          resource bundles are grouped in main (module "mainbundles"),
 *          EU (module "eubundles"), and Asia (module "asiabundles").
 *          3) ResourceBundle.getBundle caller is in named module "test" and all
 *          resource bundles are in single named module "bundles".
 *          4) ResourceBundle.getBundle caller is in named module "test" and all
 *          resource bundles in xml format are in single named module "bundles".
 *          5) Resource bundles in a local named module with no ResourceBundleProviders.
 * @library /test/lib
 *          ..
 * @build jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Utils
 *        jdk.test.lib.compiler.CompilerUtils
 *        jdk.test.lib.process.ProcessTools
 *        ModuleTestUtil
 * @run testng BasicTest
 */

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.Utils;
import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.process.ProcessTools;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static jdk.test.lib.Asserts.assertEquals;
import static org.testng.Assert.assertTrue;

@Test
public class BasicTest {
    private static final String SRC_DIR_APPBASIC = "srcAppbasic";
    private static final String SRC_DIR_APPBASIC2 = "srcAppbasic2";
    private static final String SRC_DIR_BASIC = "srcBasic";
    private static final String SRC_DIR_SIMPLE = "srcSimple";
    private static final String SRC_DIR_XML = "srcXml";
    private static final String SRC_DIR_MODLOCAL = "srcModlocal";

    private static final String MODS_DIR_APPBASIC = "modsAppbasic";
    private static final String MODS_DIR_APPBASIC2 = "modsAppbasic2";
    private static final String MODS_DIR_BASIC = "modsBasic";
    private static final String MODS_DIR_SIMPLE = "modsSimple";
    private static final String MODS_DIR_XML = "modsXml";
    private static final String MODS_DIR_MODLOCAL = "modsModlocal";

    private static final String EXTRA_JAR_BASIC = "extra_basic.jar";
    private static final String EXTRA_JAR_MODLOCAL = "extra_modlocal.jar";

    private static final List<String> LOCALE_LIST = List.of("de", "fr", "ja",
            "zh-tw", "en", "de");
    private static final List<String> LOCALE_LIST_BASIC = List.of("de", "fr",
            "ja", "ja-jp", "zh-tw", "en", "de", "ja-jp", "in", "yi");

    private static final List<String> MODULE_LIST = List.of("asiabundles",
            "eubundles", "test");
    private static final List<String> MODULE_LIST_BASIC = List.of("mainbundles",
            "asiabundles", "eubundles", "test");
    private static final List<String> MODULE_LIST_SIMPLE = List.of("bundles", "test");

    private static final String MAIN = "test/jdk.test.Main";

    @DataProvider(name = "basicTestData")
    Object[][] basicTestData() {
        return new Object[][] {
                // Named module "test" contains resource bundles for root and en,
                // and separate named modules "eubundles" and "asiabundles"
                // contain other resource bundles.
                {SRC_DIR_APPBASIC, MODS_DIR_APPBASIC, MODULE_LIST, LOCALE_LIST,
                        ".properties"},
                {SRC_DIR_APPBASIC2, MODS_DIR_APPBASIC2, MODULE_LIST, LOCALE_LIST,
                        ".properties"},

                // Resource bundles are grouped in main (module "mainbundles"),
                // EU (module "eubundles"), and Asia (module "asiabundles").
                {SRC_DIR_BASIC, MODS_DIR_BASIC, MODULE_LIST_BASIC, LOCALE_LIST_BASIC,
                        ".properties"},

                // All resource bundles are in single named module "bundles".
                {SRC_DIR_SIMPLE, MODS_DIR_SIMPLE, MODULE_LIST_SIMPLE, LOCALE_LIST,
                        ".properties"},

                // All resource bundles in xml format are in single named
                // module "bundles".
                {SRC_DIR_XML, MODS_DIR_XML, MODULE_LIST_SIMPLE, LOCALE_LIST, ".xml"},

                // Resource bundles local in named module "test".
                {SRC_DIR_MODLOCAL, MODS_DIR_MODLOCAL, List.of("test"), LOCALE_LIST,
                        ".properties"},
        };
    }

    @Test(dataProvider = "basicTestData")
    public void runBasicTest(String src, String mod, List<String> moduleList,
            List<String> localeList, String resFormat) throws Throwable {
        Path srcPath = Paths.get(Utils.TEST_SRC, src);
        Path modPath = Paths.get(Utils.TEST_CLASSES, mod);
        moduleList.forEach(mn -> ModuleTestUtil.prepareModule(srcPath, modPath,
                mn, resFormat));
        ModuleTestUtil.runModule(modPath.toString(), MAIN, localeList);
        ModuleTestUtil.runModuleWithLegacyCode(modPath.toString(), MAIN, localeList);
    }

    @Test
    public void RunBasicTestWithCp() throws Throwable {
        Path jarPath = Paths.get(Utils.TEST_CLASSES, EXTRA_JAR_BASIC);
        Path srcPath = Paths.get(Utils.TEST_SRC, SRC_DIR_BASIC);
        Path modPath = Paths.get(Utils.TEST_CLASSES, MODS_DIR_BASIC);
        Path classPath = Paths.get(Utils.TEST_CLASSES).resolve("classes")
                .resolve("basic");

        jarBasic(srcPath, classPath, jarPath);
        // jdk.test.Main should NOT load bundles from the jar file specified
        // by the class-path.
        ModuleTestUtil.runModuleWithCp(jarPath.toString(), modPath.toString(),
                MAIN, List.of("es", "vi"), false);
    }

    @Test
    public void runModLocalTestWithCp() throws Throwable {
        Path jarPath = Paths.get(Utils.TEST_CLASSES, EXTRA_JAR_MODLOCAL);
        Path srcPath = Paths.get(Utils.TEST_SRC, SRC_DIR_MODLOCAL);
        Path modPath = Paths.get(Utils.TEST_CLASSES, MODS_DIR_MODLOCAL);

        jarModLocal(srcPath, jarPath);
        // jdk.test.Main should load bundles from the jar file specified by
        // the class-path.
        ModuleTestUtil.runModuleWithCp(jarPath.toString(), modPath.toString(),
                MAIN, List.of("vi"), true);
    }

    /**
     * Create extra_basic.jar to be added to the class path. It contains .class
     * and .properties resource bundles.
     */
    private static void jarBasic(Path srcPath, Path classPath, Path jarPath)
            throws Throwable {
        boolean compiled = CompilerUtils.compile(srcPath.resolve("extra"),
                classPath);
        assertTrue(compiled, "Compile Java files for extra_basic.jar failed.");

        JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("jar");
        launcher.addToolArg("-cf")
                .addToolArg(jarPath.toString())
                .addToolArg("-C")
                .addToolArg(classPath.toString())
                .addToolArg("jdk/test/resources/eu")
                .addToolArg("-C")
                .addToolArg(srcPath.resolve("extra").toString())
                .addToolArg("jdk/test/resources/asia");

        int exitCode = ProcessTools.executeCommand(launcher.getCommand())
                .getExitValue();
        assertEquals(exitCode, 0, "Create extra_basic.jar failed. "
                + "Unexpected exit code: " + exitCode);
    }

    /**
     * Create extra_modlocal.jar to be added to the class path. Expected
     * properties files are picked up from the class path.
     */
    private static void jarModLocal(Path srcPath, Path jarPath) throws Throwable {
        JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("jar");
        launcher.addToolArg("-cf")
                .addToolArg(jarPath.toString())
                .addToolArg("-C")
                .addToolArg(srcPath.resolve("extra").toString())
                .addToolArg("jdk/test/resources");

        int exitCode = ProcessTools.executeCommand(launcher.getCommand())
                .getExitValue();
        assertEquals(exitCode, 0, "Create extra_modlocal.jar failed. "
                + "Unexpected exit code: " + exitCode);
    }
}