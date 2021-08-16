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
 * @summary Test jcmd to dump dynamic shared archive.
 * @requires vm.cds
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @modules jdk.jcmd/sun.tools.common:+open
 * @compile ../test-classes/Hello.java JCmdTestDumpBase.java
 * @build sun.hotspot.WhiteBox
 * @build JCmdTestLingeredApp JCmdTestDynamicDump
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm/timeout=480 -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI JCmdTestDynamicDump
 */

import java.io.File;

import jdk.test.lib.apps.LingeredApp;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.JDKToolFinder;

public class JCmdTestDynamicDump extends JCmdTestDumpBase {
    static final String DYNAMIC_DUMP_FILE   = "mydynamic";
    static final String[] DYNAMIC_MESSAGES  = {"JCmdTestLingeredApp source: shared objects file (top)",
                                               "LingeredApp source: shared objects file (top)",
                                               "Hello source: shared objects file (top)"};
    static void test() throws Exception {
        setIsStatic(false);
        buildJars();

        LingeredApp app  = null;
        long pid;

        int  test_count = 1;
        // Test dynamic dump with -XX:+RecordDynamicDumpInfo.
        print2ln(test_count++ + " Test dynamic dump with -XX:+RecordDynamicDumpInfo.");
        app = createLingeredApp("-cp", allJars, "-XX:+RecordDynamicDumpInfo");
        pid = app.getPid();
        test(DYNAMIC_DUMP_FILE + "01.jsa", pid, noBoot, EXPECT_PASS, DYNAMIC_MESSAGES);

        // Test dynamic dump twice to same process.
        print2ln(test_count++ + " Test dynamic dump second time to the same process.");
        test("02.jsa", pid, noBoot,  EXPECT_PASS);
        app.stopApp();

        // Test dynamic dump with -XX:-RecordDynamicDumpInfo.
        print2ln(test_count++ + " Test dynamic dump with -XX:-RecordDynamicDumpInfo.");
        app = createLingeredApp("-cp", allJars);
        pid = app.getPid();
        test("01.jsa", pid, noBoot, EXPECT_FAIL);
        app.stopApp();

        // Test dynamic dump with default archive name (null).
        print2ln(test_count++ + " Test dynamic dump with default archive name (null).");
        app = createLingeredApp("-cp", allJars, "-XX:+RecordDynamicDumpInfo");
        pid = app.getPid();
        test(null, pid, noBoot, EXPECT_PASS, DYNAMIC_MESSAGES);
        app.stopApp();

        // Test dynamic dump with flags -XX:+RecordDynamicDumpInfo -XX:-DynamicDumpSharedSpaces.
        print2ln(test_count++ + " Test dynamic dump with flags -XX:+RecordDynamicDumpInfo -XX:-DynamicDumpSharedSpaces.");
        app = createLingeredApp("-cp", allJars, "-XX:+RecordDynamicDumpInfo", "-XX:-DynamicDumpSharedSpaces");
        pid = app.getPid();
        test(null, pid, noBoot, EXPECT_PASS, DYNAMIC_MESSAGES);
        app.stopApp();

        // Test dynamic dump with flags -XX:-DynamicDumpSharedSpaces -XX:+RecordDynamicDumpInfo.
        print2ln(test_count++ + " Test dynamic dump with flags -XX:-DynamicDumpSharedSpaces -XX:+RecordDynamicDumpInfo.");
        app = createLingeredApp("-cp", allJars, "-XX:-DynamicDumpSharedSpaces", "-XX:+RecordDynamicDumpInfo");
        pid = app.getPid();
        test(null, pid, noBoot,  EXPECT_PASS, DYNAMIC_MESSAGES);
        app.stopApp();

        // Test dynamic with -Xbootclasspath/a:boot.jar
        print2ln(test_count++ + " Test dynamic with -Xbootclasspath/a:boot.jar");
        app = createLingeredApp("-cp", testJar, "-Xbootclasspath/a:" + bootJar, "-XX:+RecordDynamicDumpInfo");
        pid = app.getPid();
        test(null, pid, useBoot, EXPECT_PASS, DYNAMIC_MESSAGES);
        app.stopApp();

        // Test -XX:+RecordDynamicDump -XX:SharedArchiveFile=test_static.jsa
        print2ln(test_count++ + " Test -XX:+RecordDynamicDumpInfo -XX:SharedArchiveFile=test_static.jsa");
        // Dump a static archive as base (here do not use the default classes.jsa)
        String archiveFile = "test_static.jsa";
        dumpStaticArchive(archiveFile);
        app = createLingeredApp("-cp", allJars, "-XX:+RecordDynamicDumpInfo",
                                "-XX:SharedArchiveFile=" + archiveFile);
        pid = app.getPid();
        test(null, pid, noBoot, EXPECT_PASS, DYNAMIC_MESSAGES);
        app.stopApp();

        // Test dynamic dump with -XX:ArchiveClassAtExit will fail.
        print2ln(test_count++ + " Test dynamic dump with -XX:ArchiveClassAtExit will fail.");
        app = createLingeredApp("-cp", allJars,
                                "-Xshare:auto",
                                "-XX:+RecordDynamicDumpInfo",
                                "-XX:ArchiveClassesAtExit=AnyName.jsa");
        if (app != null) {
            if (app.getProcess().isAlive()) {
                throw new RuntimeException("The JCmdTestLingeredApp should not start up!");
            }
        }
    }

    // Dump a static archive, not using TestCommon.dump(...), we do not take jtreg args.
    private static void dumpStaticArchive(String archiveFile) throws Exception {
        String javapath = JDKToolFinder.getJDKTool("java");
        String cmd[] = {javapath, "-Xshare:dump",  "-XX:SharedArchiveFile=" + archiveFile};
        // Do not use ProcessTools.createTestJvm(cmd) here, it copies jtreg env.
        ProcessBuilder pb = new ProcessBuilder(cmd);
        CDSTestUtils.executeAndLog(pb, "dump")
            .shouldHaveExitValue(0);
        File file = new File(archiveFile);
        if (!file.exists()) {
            throw new RuntimeException("Cannot dump classes to archive file " + archiveFile);
        }
    }

    public static void main(String... args) throws Exception {
        runTest(JCmdTestDynamicDump::test);
    }
}
