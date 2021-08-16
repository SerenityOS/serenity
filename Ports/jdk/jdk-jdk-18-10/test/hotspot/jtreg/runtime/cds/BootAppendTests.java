/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @requires vm.cds & !vm.graal.enabled
 * @summary Testing -Xbootclasspath/a support for CDS
 * @requires vm.cds
 * @library /test/lib
 * @compile javax/sound/sampled/MyClass.jasm
 * @compile javax/annotation/processing/FilerException.jasm
 * @compile nonjdk/myPackage/MyClass.java
 * @build LoadClass
 * @run main/othervm BootAppendTests
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.helpers.ClassFileInstaller;

public class BootAppendTests {
    private static final String APP_CLASS = "LoadClass";
    private static final String BOOT_APPEND_MODULE_CLASS = "javax/sound/sampled/MyClass";
    private static final String BOOT_APPEND_DUPLICATE_MODULE_CLASS =
        "javax/annotation/processing/FilerException";
    private static final String BOOT_APPEND_CLASS = "nonjdk/myPackage/MyClass";
    private static final String BOOT_APPEND_MODULE_CLASS_NAME =
        BOOT_APPEND_MODULE_CLASS.replace('/', '.');
    private static final String BOOT_APPEND_DUPLICATE_MODULE_CLASS_NAME =
        BOOT_APPEND_DUPLICATE_MODULE_CLASS.replace('/', '.');
    private static final String BOOT_APPEND_CLASS_NAME =
        BOOT_APPEND_CLASS.replace('/', '.');
    private static final String[] ARCHIVE_CLASSES =
        {BOOT_APPEND_MODULE_CLASS, BOOT_APPEND_DUPLICATE_MODULE_CLASS, BOOT_APPEND_CLASS};

    private static final String modes[] = {"on", "off"};

    private static String appJar;
    private static String bootAppendJar;

    public static void main(String... args) throws Exception {
        dumpArchive();

        logTestCase("1");
        testBootAppendModuleClass();

        logTestCase("2");
        testBootAppendDuplicateModuleClass();

        logTestCase("3");
        testBootAppendExcludedModuleClass();

        logTestCase("4");
        testBootAppendDuplicateExcludedModuleClass();

        logTestCase("5");
        testBootAppendClass();

        logTestCase("6");
        testBootAppendExtraDir();
    }

    private static void logTestCase(String msg) {
        System.out.println();
        System.out.printf("TESTCASE: %s", msg);
        System.out.println();
    }

    static void dumpArchive() throws Exception {
        // create the classlist
        File classlist = CDSTestUtils.makeClassList(ARCHIVE_CLASSES);

        // build jar files
        appJar = ClassFileInstaller.writeJar("app.jar", APP_CLASS);
        bootAppendJar = ClassFileInstaller.writeJar("bootAppend.jar",
            BOOT_APPEND_MODULE_CLASS, BOOT_APPEND_DUPLICATE_MODULE_CLASS, BOOT_APPEND_CLASS);


        OutputAnalyzer out = CDSTestUtils.createArchiveAndCheck(
                                 "-Xbootclasspath/a:" + bootAppendJar,
                                 "-cp", appJar,
                                 "-XX:SharedClassListFile=" + classlist.getPath());
        // Make sure all the classes were successfully archived.
        for (String archiveClass : ARCHIVE_CLASSES) {
            String msg = "Preload Warning: Cannot find " + archiveClass;
            if (archiveClass.equals(BOOT_APPEND_MODULE_CLASS)) {
                // We shouldn't archive a class in the appended boot class path that
                // are the java.desktop module. Such a class cannot be loaded
                // at runtime anyway.
                out.shouldContain(msg);
            } else {
                out.shouldNotContain(msg);
            }
        }
    }

    // Test #1: If a class on -Xbootclasspath/a is from a package defined in
    //          bootmodules, the class is not loaded at runtime.
    //          Verify the behavior is the same when the class is archived
    //          with CDS enabled at runtime.
    //
    //          The javax.sound.sampled package is defined in the java.desktop module.
    //          The archived javax.sound.sampled.MyClass from the -Xbootclasspath/a
    //          should not be loaded at runtime.
    public static void testBootAppendModuleClass() throws Exception {
        for (String mode : modes) {
            CDSOptions opts = (new CDSOptions())
                .setXShareMode(mode).setUseVersion(false)
                .addPrefix("-Xbootclasspath/a:" + bootAppendJar, "-cp", appJar, "-showversion")
                .addSuffix(APP_CLASS, BOOT_APPEND_MODULE_CLASS_NAME);

            OutputAnalyzer out = CDSTestUtils.runWithArchive(opts);
            CDSTestUtils.checkExec(out, opts, "java.lang.ClassNotFoundException: javax.sound.sampled.MyClass");
        }
    }

    // Test #2: If a class on -Xbootclasspath/a has the same fully qualified
    //          name as a class defined in boot modules, the class is not loaded
    //          from -Xbootclasspath/a. Verify the behavior is the same at runtime
    //          when CDS is enabled.
    //
    //          The javax/annotation/processing/FilerException is a platform module
    //          class. The class on the -Xbootclasspath/a path that has the same
    //          fully-qualified name should not be loaded at runtime when CDS is enabled.
    //          The one from the platform modules should be loaded instead.
    public static void testBootAppendDuplicateModuleClass() throws Exception {
        for (String mode : modes) {
            CDSOptions opts = (new CDSOptions())
                .setXShareMode(mode).setUseVersion(false)
                .addPrefix("-showversion",
                           "-Xbootclasspath/a:" + bootAppendJar, "-cp", appJar)
                .addSuffix("-Xlog:class+load=info",
                           APP_CLASS, BOOT_APPEND_DUPLICATE_MODULE_CLASS_NAME);

            String MATCH_PATTERN = ".class.load. javax.annotation.processing.FilerException source:.*bootAppend.jar*";
            CDSTestUtils.run(opts)
                .assertNormalExit(out -> {
                    out.shouldNotMatch(MATCH_PATTERN);
                });
        }
    }

    // Test #3: If a class on -Xbootclasspath/a is from a package defined in boot modules,
    //          the class can be loaded from -Xbootclasspath/a when the module is excluded
    //          using --limit-modules. Verify the behavior is the same at runtime when CDS
    //          is enabled.
    //
    //          The java.desktop module is excluded using --limit-modules at runtime
    //          CDS will be disabled with the --limit-modules option during runtime.
    //          javax.sound.sampled.MyClass will be loaded from the jar at runtime.
    public static void testBootAppendExcludedModuleClass() throws Exception {
        for (String mode : modes) {
            CDSOptions opts = (new CDSOptions())
                .setXShareMode(mode).setUseVersion(false)
                .addPrefix("-Xbootclasspath/a:" + bootAppendJar, "-showversion",
                           "--limit-modules=java.base", "-cp", appJar)
                .addSuffix("-Xlog:class+load=info",
                           APP_CLASS, BOOT_APPEND_MODULE_CLASS_NAME);
            CDSTestUtils.Result res = CDSTestUtils.run(opts);
            String MATCH_PATTERN =
                ".class.load. javax.sound.sampled.MyClass source:.*bootAppend.jar*";
            if (mode.equals("on")) {
                res.assertSilentlyDisabledCDS(out -> {
                    out.shouldHaveExitValue(0)
                       .shouldMatch(MATCH_PATTERN);
                    });
            } else {
                res.assertNormalExit(out -> {
                    out.shouldMatch(MATCH_PATTERN);
                    });
            }
        }
    }

    // Test #4: If a class on -Xbootclasspath/a has the same fully qualified
    //          name as a class defined in boot modules, the class is loaded
    //          from -Xbootclasspath/a when the boot module is excluded using
    //          --limit-modules. Verify the behavior is the same at runtime
    //          when CDS is enabled.
    //
    //          The javax.annotation.processing.FilerException is a platform module class.
    //          The class on -Xbootclasspath/a that has the same fully-qualified name
    //          as javax.annotation.processing.FilerException can be loaded at runtime when
    //          java.compiler is excluded.
    //          CDS is disabled during runtime if the --limit-modules option is
    //          specified.
    public static void testBootAppendDuplicateExcludedModuleClass() throws Exception {
        for (String mode : modes) {
            CDSOptions opts = (new CDSOptions())
                .setXShareMode(mode).setUseVersion(false)
                .addPrefix("-Xbootclasspath/a:" + bootAppendJar, "-showversion",
                           "--limit-modules=java.base", "-cp", appJar)
                .addSuffix("-Xlog:class+load=info",
                           APP_CLASS, BOOT_APPEND_DUPLICATE_MODULE_CLASS_NAME);

            CDSTestUtils.Result res = CDSTestUtils.run(opts);
            String MATCH_PATTERN =
                ".class.load. javax.annotation.processing.FilerException source:.*bootAppend.jar*";
            if (mode.equals("on")) {
                res.assertSilentlyDisabledCDS(out -> {
                    out.shouldHaveExitValue(0)
                       .shouldMatch(MATCH_PATTERN);
                    });
            } else {
                res.assertNormalExit(out -> {
                    out.shouldMatch(MATCH_PATTERN);
                    });
            }
        }
    }

    // Test #5: If a class on -Xbootclasspath/a is not from named modules,
    //          the class can be loaded at runtime. Verify the behavior is
    //          the same at runtime when CDS is enabled.
    //
    //          The nonjdk.myPackage is not defined in named modules. The
    //          nonjdk.myPackage.MyClass will be loaded from the jar in
    //          -Xbootclasspath/a since CDS will be disabled with the
    //          --limit-modules option.
    public static void testBootAppendClass() throws Exception {
        for (String mode : modes) {
            CDSOptions opts = (new CDSOptions())
                .setXShareMode(mode).setUseVersion(false)
                .addPrefix("-Xbootclasspath/a:" + bootAppendJar, "-showversion",
                           "--limit-modules=java.base", "-cp", appJar)
                .addSuffix("-Xlog:class+load=info",
                           APP_CLASS, BOOT_APPEND_CLASS_NAME);

            CDSTestUtils.Result res = CDSTestUtils.run(opts);
            String MATCH_PATTERN =
                ".class.load. nonjdk.myPackage.MyClass source:.*bootAppend.jar*";
            if (mode.equals("on")) {
                res.assertSilentlyDisabledCDS(out -> {
                    out.shouldHaveExitValue(0)
                       .shouldMatch(MATCH_PATTERN);
                    });
            } else {
                res.assertNormalExit(out -> {
                    out.shouldMatch(MATCH_PATTERN);
                    });
            }
        }
    }

    // Test #6: This is similar to Test #5. During runtime, an extra dir
    //          is appended to the bootclasspath. It should not invalidate
    //          the shared archive. However, CDS will be disabled with the
    //          --limit-modules in the command line.
    public static void testBootAppendExtraDir() throws Exception {
        for (String mode : modes) {
            CDSOptions opts = (new CDSOptions())
                .setXShareMode(mode).setUseVersion(false)
                .addPrefix("-Xbootclasspath/a:" + bootAppendJar + File.pathSeparator + appJar,
                           "-showversion", "--limit-modules=java.base", "-cp", appJar)
                .addSuffix("-Xlog:class+load=info",
                           APP_CLASS, BOOT_APPEND_CLASS_NAME);

            CDSTestUtils.Result res = CDSTestUtils.run(opts);
            String MATCH_PATTERN =
                ".class.load. nonjdk.myPackage.MyClass source:.*bootAppend.jar*";
            if (mode.equals("on")) {
                res.assertSilentlyDisabledCDS(out -> {
                    out.shouldHaveExitValue(0)
                       .shouldMatch(MATCH_PATTERN);
                    });
            } else {
                res.assertNormalExit(out -> {
                    out.shouldMatch(MATCH_PATTERN);
                    });
            }
        }
    }
}
