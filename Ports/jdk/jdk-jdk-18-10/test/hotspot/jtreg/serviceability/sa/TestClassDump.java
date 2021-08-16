/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import jdk.test.lib.apps.LingeredApp;
import jdk.test.lib.Platform;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.SA.SATestUtils;

/**
 * @test
 * @bug 8184982
 * @summary Test ClassDump tool
 * @requires vm.hasSA
 * @library /test/lib
 * @run driver TestClassDump
 */

public class TestClassDump {

    private static void dumpClass(long lingeredAppPid)
        throws IOException {

        ProcessBuilder pb;
        OutputAnalyzer output;

        pb = ProcessTools.createJavaProcessBuilder(
                "-Dsun.jvm.hotspot.tools.jcore.outputDir=jtreg_classes",
                "-m", "jdk.hotspot.agent/sun.jvm.hotspot.tools.jcore.ClassDump", String.valueOf(lingeredAppPid));
        SATestUtils.addPrivilegesIfNeeded(pb);
        output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);
        if (!Files.isDirectory(Paths.get("jtreg_classes"))) {
            throw new RuntimeException("jtreg_classes directory not found");
        }
        if (Files.notExists(Paths.get("jtreg_classes", "java", "lang", "Integer.class"))) {
            throw new RuntimeException("jtreg_classes/java/lang/Integer.class not found");
        }
        if (Files.notExists(Paths.get("jtreg_classes", "jdk", "test", "lib", "apps", "LingeredApp.class"))) {
            throw new RuntimeException("jtreg_classes/jdk/test/lib/apps/LingeredApp.class not found");
        }
        if (Files.notExists(Paths.get("jtreg_classes", "sun", "net", "util", "URLUtil.class"))) {
            throw new RuntimeException("jtreg_classes/sun/net/util/URLUtil.class not found");
        }

        pb = ProcessTools.createJavaProcessBuilder(
                "-Dsun.jvm.hotspot.tools.jcore.outputDir=jtreg_classes2",
                "-Dsun.jvm.hotspot.tools.jcore.PackageNameFilter.pkgList=jdk,sun",
                "-m", "jdk.hotspot.agent/sun.jvm.hotspot.tools.jcore.ClassDump", String.valueOf(lingeredAppPid));
        SATestUtils.addPrivilegesIfNeeded(pb);
        output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);
        if (Files.exists(Paths.get("jtreg_classes2", "java", "math", "BigInteger.class"))) {
            throw new RuntimeException("jtreg_classes2/java/math/BigInteger.class not expected");
        }
        if (Files.notExists(Paths.get("jtreg_classes2", "sun", "util", "calendar", "BaseCalendar.class"))) {
            throw new RuntimeException("jtreg_classes2/sun/util/calendar/BaseCalendar.class not found");
        }
        if (Files.notExists(Paths.get("jtreg_classes2", "jdk", "internal", "loader", "BootLoader.class"))) {
            throw new RuntimeException("jtreg_classes2/jdk/internal/loader/BootLoader.class not found");
        }
    }

    public static void main(String[] args) throws Exception {
        SATestUtils.skipIfCannotAttach(); // throws SkippedException if attach not expected to work.
        LingeredApp theApp = null;
        try {
            theApp = LingeredApp.startApp();
            long pid = theApp.getPid();
            System.out.println("Started LingeredApp with pid " + pid);
            dumpClass(pid);
        } catch (Exception ex) {
            throw new RuntimeException("Test ERROR " + ex, ex);
        } finally {
            LingeredApp.stopApp(theApp);
        }
        System.out.println("Test PASSED");
    }
}
