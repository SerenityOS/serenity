/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary At run time, it is OK to append new elements to the classpath that was used at dump time.
 * @requires vm.cds
 * @library /test/lib
 * @compile test-classes/Hello.java
 * @compile test-classes/HelloMore.java
 * @run driver AppendClasspath
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;

public class AppendClasspath {

  public static void main(String[] args) throws Exception {
    String appJar = JarBuilder.getOrCreateHelloJar();
    String appJar2 = JarBuilder.build("AppendClasspath_HelloMore", "HelloMore");

    // Dump an archive with a specified JAR file in -classpath
    TestCommon.testDump(appJar, TestCommon.list("Hello"));

    // PASS: 1) runtime with classpath containing the one used in dump time
    TestCommon.run(
        "-cp", appJar + File.pathSeparator + appJar2,
        "HelloMore")
      .assertNormalExit();

    // PASS: 2) runtime has an non-existing jar in the -cp
    String outDir = CDSTestUtils.getOutputDir();
    String newFile = "non-exist.jar";
    String nonExistPath = outDir + File.separator + newFile;
    String classPath = appJar + File.pathSeparator + nonExistPath;
    File nonExistJar = new File(outDir, newFile);
    if (nonExistJar.exists()) {
        nonExistJar.delete();
    }
    TestCommon.run(
        "-cp", classPath,
        "-Xlog:class+path=trace",
        "Hello")
      .assertNormalExit();

    final String errorMessage1 = "Unable to use shared archive";
    final String errorMessage2 = "shared class paths mismatch";
    // FAIL: 1) runtime with classpath different from the one used in dump time
    // (runtime has an extra jar file prepended to the class path)
    TestCommon.run(
        "-Xlog:cds",
        "-cp", appJar2 + File.pathSeparator + appJar,
        "HelloMore")
        .assertAbnormalExit(errorMessage1, errorMessage2);

    // FAIL: 2) runtime with classpath part of the one used in dump time
    TestCommon.testDump(appJar + File.pathSeparator + appJar2,
                                      TestCommon.list("Hello"));
    TestCommon.run(
        "-Xlog:cds",
        "-cp", appJar2,
        "Hello")
        .assertAbnormalExit(errorMessage1, errorMessage2);

    // FAIL: 3) runtime with same set of jar files in the classpath but
    // with different order
    TestCommon.run(
        "-Xlog:cds",
        "-cp", appJar2 + File.pathSeparator + appJar,
        "HelloMore")
        .assertAbnormalExit(errorMessage1, errorMessage2);
    }
}
