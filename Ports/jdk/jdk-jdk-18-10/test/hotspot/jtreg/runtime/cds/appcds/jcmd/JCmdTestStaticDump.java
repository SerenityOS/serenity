/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8259070
 * @summary Test jcmd to dump static shared archive.
 * @requires vm.cds
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @modules jdk.jcmd/sun.tools.common:+open
 * @compile ../test-classes/Hello.java JCmdTestDumpBase.java
 * @build sun.hotspot.WhiteBox
 * @build JCmdTestLingeredApp JCmdTestStaticDump
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm/timeout=480 -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI JCmdTestStaticDump
 */

import jdk.test.lib.apps.LingeredApp;

public class JCmdTestStaticDump extends JCmdTestDumpBase {

    static final String STATIC_DUMP_FILE    = "mystatic";
    static final String[] STATIC_MESSAGES   = {"JCmdTestLingeredApp source: shared objects file",
                                               "LingeredApp source: shared objects file",
                                               "Hello source: shared objects file"};

    // Those two flags will not create a successful LingeredApp.
    private static String[] noDumpFlags  =
        {"-XX:+DumpSharedSpaces",
         "-Xshare:dump"};
    // Those flags will be excluded in static dumping,
    // See src/java.base/share/classes/jdk/internal/misc/CDS.java
    private static String[] excludeFlags = {
         "-XX:DumpLoadedClassList=AnyFileName.classlist",
         // this flag just dump archive, won't run app normally.
         // "-XX:+DumpSharedSpaces",
         "-XX:+DynamicDumpSharedSpaces",
         "-XX:+RecordDynamicDumpInfo",
         "-Xshare:on",
         "-Xshare:auto",
         "-XX:SharedClassListFile=non-exist.classlist",
         "-XX:SharedArchiveFile=non-exist.jsa",
         "-XX:ArchiveClassesAtExit=tmp.jsa",
         "-XX:+UseSharedSpaces",
         "-XX:+RequireSharedSpaces"};

    // Times to dump cds against same process.
    private static final int ITERATION_TIMES = 2;

    static void test() throws Exception {
        setIsStatic(true);
        buildJars();

        LingeredApp app = null;
        long pid;

        int  test_count = 1;
        // Static dump with default name multiple times.
        print2ln(test_count++ + " Static dump with default name multiple times.");
        app  = createLingeredApp("-cp", allJars);
        pid = app.getPid();
        for (int i = 0; i < ITERATION_TIMES; i++) {
            test(null, pid, noBoot,  EXPECT_PASS, STATIC_MESSAGES);
        }
        app.stopApp();

        // Test static dump with given file name.
        print2ln(test_count++ + " Test static dump with given file name.");
        app = createLingeredApp("-cp", allJars);
        pid = app.getPid();
        for (int i = 0; i < ITERATION_TIMES; i++) {
            test("0" + i + ".jsa", pid, noBoot,  EXPECT_PASS, STATIC_MESSAGES);
        }
        app.stopApp();

        //  Test static dump with flags with which dumping should fail
        //  This test will result classes.jsa in default server dir if -XX:SharedArchiveFile= not set.
        print2ln(test_count++ + " Test static dump with flags with which dumping should fail.");
        for (String flag : noDumpFlags) {
            app = createLingeredApp("-cp", allJars, flag, "-XX:SharedArchiveFile=tmp.jsa");
            // Following should not be executed.
            if (app != null && app.getProcess().isAlive()) {
                pid = app.getPid();
                test(null, pid, noBoot, EXPECT_FAIL);
                app.stopApp();
                // if above executed OK, mean failed.
                throw new RuntimeException("Should not dump successful with " + flag);
            }
        }

        // Test static with -Xbootclasspath/a:boot.jar
        print2ln(test_count++ + " Test static with -Xbootassath/a:boot.jar");
        app = createLingeredApp("-Xbootclasspath/a:" + bootJar, "-cp", testJar);
        pid = app.getPid();
        test(null, pid, useBoot, EXPECT_PASS, STATIC_MESSAGES);
        app.stopApp();

        // Test static with limit-modules java.base.
        print2ln(test_count++ + " Test static with --limit-modules java.base.");
        app = createLingeredApp("--limit-modules", "java.base", "-cp", allJars);
        pid = app.getPid();
        test(null, pid, noBoot, EXPECT_FAIL);
        app.stopApp();

        // Test static dump with flags which will be filtered before dumping.
        print2ln(test_count++ + " Test static dump with flags which will be filtered before dumping.");
        for (String flag : excludeFlags) {
            app = createLingeredApp("-cp", allJars, flag);
            pid = app.getPid();
            test(null, pid, noBoot, EXPECT_PASS, STATIC_MESSAGES);
            app.stopApp();
        }

        // Test static with -Xshare:off will be OK to dump.
        print2ln(test_count++ + " Test static with -Xshare:off will be OK to dump.");
        app = createLingeredApp("-Xshare:off", "-cp", allJars);
        pid = app.getPid();
        test(null, pid, noBoot,  EXPECT_PASS, STATIC_MESSAGES);
        app.stopApp();

        // Test static with -XX:+RecordDynamicDumpInfo will be OK to dump.
        print2ln(test_count++ + " Test static with -XX:+RecordDynamicDumpInfo will be OK to dump.");
        app = createLingeredApp("-XX:+RecordDynamicDumpInfo", "-cp", allJars);
        pid = app.getPid();
        test(null, pid, noBoot,  EXPECT_PASS, STATIC_MESSAGES);
        app.stopApp();
    }

    public static void main(String... args) throws Exception {
        runTest(JCmdTestStaticDump::test);
    }
}
