/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test relative paths specified in the -cp.
 * @requires vm.cds
 * @library /test/lib
 * @compile test-classes/Hello.java
 * @compile test-classes/HelloMore.java
 * @run driver RelativePath
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.util.Arrays;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.Platform;

public class RelativePath {

  private static final Path USER_DIR = Paths.get(CDSTestUtils.getOutputDir());

  public static void main(String[] args) throws Exception {
    String appJar = JarBuilder.getOrCreateHelloJar();
    String appJar2 = JarBuilder.build("AppendClasspath_HelloMore", "HelloMore");

    // dump an archive with only the jar name in the -cp
    int idx = appJar.lastIndexOf(File.separator);
    String jarName = appJar.substring(idx + 1);
    String jarDir = appJar.substring(0, idx);
    TestCommon.testDump(jarDir, jarName, TestCommon.list("Hello"));

    // copy the jar file to another dir. Specify the jar file without
    // a directory path.
    Path srcPath = Paths.get(appJar);
    Path destDir = Files.createTempDirectory(USER_DIR, "deploy");
    Path destPath = destDir.resolve(jarName);
    Files.copy(srcPath, destPath, REPLACE_EXISTING, COPY_ATTRIBUTES);
    TestCommon.runWithRelativePath(
        destDir.toString(),
        "-Xshare:on",
        "-XX:SharedArchiveFile=" + TestCommon.getCurrentArchiveName(),
        "-cp", jarName + File.pathSeparator + appJar2,
        "-Xlog:class+load=trace,class+path=info",
        "HelloMore")
        .assertNormalExit(output -> {
                output.shouldContain("Hello source: shared objects file")
                      .shouldHaveExitValue(0);
            });

    // Long path test
    // Create a long directory path and copy the appJar there.
    final int MAX_PATH = 260;
    destDir = Paths.get(jarDir);
    int subDirLen = MAX_PATH - jarDir.length() - 3;
    if (subDirLen > 0) {
        char[] chars = new char[subDirLen];
        Arrays.fill(chars, 'x');
        String subPath = new String(chars);
        destDir = Paths.get(jarDir, subPath);
    }
    File longDir = destDir.toFile();
    longDir.mkdir();
    String destJar = longDir.getPath() + File.separator + jarName;
    Files.copy(Paths.get(appJar), Paths.get(destJar), REPLACE_EXISTING);
    // Create an archive with the appJar in the long directory path.
    TestCommon.testDump(destJar, TestCommon.list("Hello"));

    // Run with -cp containing the appJar and another jar appended.
    TestCommon.run(
        "-cp", destJar + File.pathSeparator + appJar2,
        "-Xlog:class+load=trace,class+path=info",
        "HelloMore")
        .assertNormalExit(output -> {
                output.shouldContain("Hello source: shared objects file")
                      .shouldHaveExitValue(0);
            });

    // Dump an archive with a specified JAR file in -classpath
    TestCommon.testDump(appJar, TestCommon.list("Hello"));

    // compose a relative path to the hello.jar
    String newHello = TestCommon.composeRelPath(appJar);

    // create a sym link to the original hello.jar
    File linkedHello = null;
    if (!Platform.isWindows()) {
        linkedHello = TestCommon.createSymLink(appJar);
    }

    // PASS:1) same appJar but referred to via a relative path
    TestCommon.run(
        "-cp", newHello + File.pathSeparator + appJar2,
        "-Xlog:class+load=trace,class+path=info",
        "HelloMore")
      .assertNormalExit();

    // PASS:2) relative path starting with "."
    TestCommon.runWithRelativePath(
        jarDir,
        "-Xshare:on",
        "-XX:SharedArchiveFile=" + TestCommon.getCurrentArchiveName(),
        "-cp", "." + File.separator + jarName + File.pathSeparator + appJar2,
        "-Xlog:class+load=trace,class+path=info",
        "HelloMore")
        .assertNormalExit(output -> {
                output.shouldContain("Hello source: shared objects file")
                      .shouldHaveExitValue(0);
            });

    // PASS:3) relative path starting with ".."
    idx = jarDir.lastIndexOf(File.separator);
    String jarSubDir = jarDir.substring(idx + 1);
    TestCommon.runWithRelativePath(
        jarDir,
        "-Xshare:on",
        "-XX:SharedArchiveFile=" + TestCommon.getCurrentArchiveName(),
        "-cp", ".." + File.separator + jarSubDir + File.separator + jarName
               + File.pathSeparator + appJar2,
        "-Xlog:class+load=trace,class+path=info",
        "HelloMore")
        .assertNormalExit(output -> {
                output.shouldContain("Hello source: shared objects file")
                      .shouldHaveExitValue(0);
            });

    // PASS:4) a jar linked to the original hello.jar
    if (!Platform.isWindows()) {
        TestCommon.run(
            "-cp", linkedHello.getPath() + File.pathSeparator + appJar2,
            "HelloMore")
          .assertNormalExit();
    }

    final String errorMessage1 = "Unable to use shared archive";
    final String errorMessage2 = "shared class paths mismatch";
    // FAIL: 1) runtime with classpath different from the one used in dump time
    // (runtime has an extra jar file prepended to the class path)
    TestCommon.run(
        "-cp", appJar2 + File.pathSeparator + newHello,
        "HelloMore")
        .assertAbnormalExit(errorMessage1, errorMessage2);

    }
}
