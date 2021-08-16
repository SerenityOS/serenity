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
 *
 */

/**
 * @test
 * @requires vm.cds & !vm.graal.enabled
 * @library ../..
 * @library /test/lib
 * @compile src/jdk/test/Main.java
 * @compile src/com/sun/tools/javac/Main.jasm
 * @compile src/com/sun/tools/javac/MyMain.jasm
 * @compile ../../../javax/annotation/processing/FilerException.jasm
 * @run driver ClassPathTests
 * @summary AppCDS tests for testing classpath/package conflicts
 */

/*
 * These tests will verify that AppCDS will correctly handle archived classes
 * on the classpath that are in a package that is also exported by the jimage.
 * These classes should fail to load unless --limit-modules is used to hide the
 * package exported by the jimage. There are 8 variants of this test:
 *   - With a jimage app package and with a jimage ext package
 *   - With --limit-modules and without --limit-modules
 *   - With AppCDS and without AppCDS (to verify behaviour is the same for both).
 *
 * There is also a 9th test to verify that when --limit-modules is used, a jimage
 * class in the archive can be replaced by a classpath class with the
 * same name and package.
 */

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.test.lib.Asserts;
import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

import jtreg.SkippedException;


public class ClassPathTests {
    private static final String TEST_SRC = System.getProperty("test.src");
    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path CLASSES_DIR = Paths.get("classes");

    // the test module
    private static final String MAIN_CLASS = "jdk.test.Main";
    private static final String LIMITMODS_MAIN_CLASS = "jdk.test.LimitModsMain";

    // test classes to archive. These are both in UPGRADED_MODULES
    private static final String JIMAGE_CLASS      = "com/sun/tools/javac/Main";
    private static final String APP_ARCHIVE_CLASS = "com/sun/tools/javac/MyMain";
    private static final String PLATFORM_ARCHIVE_CLASS = "javax/annotation/processing/FilerException";
    private static final String[] ARCHIVE_CLASSES = {APP_ARCHIVE_CLASS, PLATFORM_ARCHIVE_CLASS, JIMAGE_CLASS};
    private static final int NUMBER_OF_TEST_CASES = 10;

    private static String appJar;
    private static String testArchiveName;


    public static void main(String[] args) throws Throwable {
        ClassPathTests tests = new ClassPathTests();
        tests.dumpArchive();

        Method[] methods = tests.getClass().getDeclaredMethods();
        int numOfTestMethodsRun = 0;
        for (Method m : methods) {
            if (m.getName().startsWith("test")) {
                System.out.println("About to run test method: " + m.getName());
                try {
                    m.invoke(tests);
                } catch (InvocationTargetException ite) {
                    Throwable throwable = ite.getCause();
                    if (throwable instanceof SkippedException) {
                        throw throwable;
                    } else {
                        throw ite;
                    }
                }
                numOfTestMethodsRun++;
            }
        }

        Asserts.assertTrue((numOfTestMethodsRun == NUMBER_OF_TEST_CASES),
            "Expected " + NUMBER_OF_TEST_CASES + " test methods to run, actual number is "
            + numOfTestMethodsRun);
    }

    private void dumpArchive() throws Exception {
        // Create a jar file with all the classes related to this test.
        JarBuilder.build( "classpathtests",
                          APP_ARCHIVE_CLASS, PLATFORM_ARCHIVE_CLASS, JIMAGE_CLASS,
                          "jdk/test/Main");
        appJar = TestCommon.getTestJar("classpathtests.jar");

        // dump the archive with altnernate jdk.comiler and jdk.activation classes in the class list
        OutputAnalyzer output1  = TestCommon.dump(appJar, TestCommon.list(ARCHIVE_CLASSES));
        TestCommon.checkDump(output1);
        // The PLATFORM_ARCHIVE_CLASS belongs to the java.compiler
        // module will be found from the module during dumping.
        // The JIMAGE_CLASS will be found from the jdk.compiler module during
        // dumping.
        // The APP_ARCHIVE_CLASS, which belongs to a package within the
        // jdk.compiler module, will not be found during dump time.
        for (String archiveClass : ARCHIVE_CLASSES) {
            if (archiveClass.equals(APP_ARCHIVE_CLASS)) {
                output1.shouldContain("Preload Warning: Cannot find " + archiveClass);
            } else {
                output1.shouldNotContain("Preload Warning: Cannot find " + archiveClass);
            }
        }

        testArchiveName = TestCommon.getCurrentArchiveName();
    }

    // #1: Archived classpath class in same package as jimage app class. With AppCDS.
    // Should fail to load.
    public void testAppClassWithAppCDS() throws Exception {
        OutputAnalyzer output = TestCommon.exec(
            appJar, MAIN_CLASS,
            "Test #1", APP_ARCHIVE_CLASS, "false"); // last 3 args passed to test
        TestCommon.checkExec(output);
    }

    // #2: Archived classpath class in same package as jimage app class. Without AppCDS.
    // Should fail to load.
    public void testAppClassWithoutAppCDS() throws Exception {
        CDSOptions opts = (new CDSOptions())
            .addPrefix("-cp", appJar)
            .setArchiveName(testArchiveName)
            .addSuffix(MAIN_CLASS, "Test #2", APP_ARCHIVE_CLASS, "false");

        CDSTestUtils.runWithArchiveAndCheck(opts);
    }

    // #3: Archived classpath class in same package as jimage ext class. With AppCDS.
    // The class should be loaded from the module.
    public void testExtClassWithAppCDS() throws Exception {
        OutputAnalyzer output = TestCommon.exec(
            appJar, MAIN_CLASS,
            "Test #3", PLATFORM_ARCHIVE_CLASS, "true", "EXT"); // last 4 args passed to test
        TestCommon.checkExec(output);
    }

    // #4: Archived classpath class in same package as jimage ext class. Without AppCDS.
    // The class should be loaded from the module.
    public void testExtClassWithoutAppCDS() throws Exception {
        CDSOptions opts = (new CDSOptions())
            .addPrefix("-cp", appJar)
            .setArchiveName(testArchiveName)
            .addSuffix(MAIN_CLASS, "Test #4", PLATFORM_ARCHIVE_CLASS, "true", "EXT");

        CDSTestUtils.runWithArchiveAndCheck(opts);
    }

    // #5: Archived classpath class in same package as jimage app class. With AppCDS.
    // Should load with CDS disabled because --limit-modules is used.
    public void testAppClassWithLimitModsWithAppCDS() throws Exception {
        TestCommon.run("-cp", appJar,
                       "--limit-modules", "java.base",
                       MAIN_CLASS,
                       "Test #5", APP_ARCHIVE_CLASS, "true") // last 3 args passed to test
            .assertSilentlyDisabledCDS(out -> {
                out.shouldHaveExitValue(0);
            });
    }

    // #6: Archived classpath class in same package as jimage app class. Without AppCDS.
    // Should load with CDS disabled because --limit-modules is used.
    public void testAppClassWithLimitModsWithoutAppCDS() throws Exception {
        CDSOptions opts = (new CDSOptions())
            .addPrefix("-cp", appJar, "--limit-modules", "java.base")
            .setArchiveName(testArchiveName)
            .addSuffix(MAIN_CLASS, "Test #6", APP_ARCHIVE_CLASS, "true");
        CDSTestUtils.run(opts)
            .assertSilentlyDisabledCDS(out -> {
                out.shouldHaveExitValue(0);
            });
    }

    // #7: Archived classpath class in same package as jimage ext class. With AppCDS.
    // Should load with CDS disabled because --limit-modules is used.
    public void testExtClassWithLimitModsWithAppCDS() throws Exception {
        TestCommon.run("-cp", appJar,
                       "--limit-modules", "java.base",
                       MAIN_CLASS,
                       "Test #7", PLATFORM_ARCHIVE_CLASS, "true") // last 3 args passed to test
            .assertSilentlyDisabledCDS(out -> {
                out.shouldHaveExitValue(0);
            });
    }

    // #8: Archived classpath class in same package as jimage ext class. Without AppCDS.
    // Should load with CDS disabled because --limit-modules is used.
    public void testExtClassWithLimitModsWithoutAppCDS() throws Exception {
        CDSOptions opts = (new CDSOptions())
            .addPrefix("-cp", appJar, "--limit-modules", "java.base")
            .setArchiveName(testArchiveName)
            .addSuffix(MAIN_CLASS, "Test #8", PLATFORM_ARCHIVE_CLASS, "true");
        CDSTestUtils.run(opts)
            .assertSilentlyDisabledCDS(out -> {
                out.shouldHaveExitValue(0);
            });
    }

    // #9: Archived classpath class with same name as jimage app class. With AppCDS.
    // Should load with CDS disabled because --limit-modules is used.
    public void testReplacingJImageClassWithAppCDS() throws Exception {
        TestCommon.run("-cp", appJar,
                       "--limit-modules", "java.base",
                       MAIN_CLASS,
                       "Test #9", JIMAGE_CLASS, "true") // last 3 args passed to test
            .assertSilentlyDisabledCDS(out -> {
                out.shouldHaveExitValue(0);
            });
    }

    // #10: Archived classpath class with same name as jimage app class. Without AppCDS.
    // Should load because --limit-modules is used. Note the archive will actually contain
    // the original jimage version of the class, but AppCDS should refuse to load it
    // since --limit-modules is used. This should result in the -cp version being used.
    public void testReplacingJImageClassWithoutAppCDS() throws Exception {
        CDSOptions opts = (new CDSOptions())
            .addPrefix("-cp", appJar, "--limit-modules", "java.base")
            .setArchiveName(testArchiveName)
            .addSuffix(MAIN_CLASS, "Test #10", JIMAGE_CLASS, "true");
        CDSTestUtils.run(opts)
            .assertSilentlyDisabledCDS(out -> {
                out.shouldHaveExitValue(0);
            });
    }
}
