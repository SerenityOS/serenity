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
 * @summary Class-Path: attribute in MANIFEST file
 * @requires vm.cds
 * @library /test/lib
 * @run driver/timeout=240 ClassPathAttr
 */

import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.StandardCopyOption;
import java.nio.file.Paths;


public class ClassPathAttr {

  public static void main(String[] args) throws Exception {
    testNormalOps();
    testNonExistentJars();
  }

  static void testNormalOps() throws Exception {
    buildCpAttr("cpattr1", "cpattr1.mf", "CpAttr1", "CpAttr1");
    buildCpAttr("cpattr1_long", "cpattr1_long.mf", "CpAttr1", "CpAttr1");
    buildCpAttr("cpattr2", "cpattr2.mf", "CpAttr2", "CpAttr2");
    buildCpAttr("cpattr3", "cpattr3.mf", "CpAttr3", "CpAttr2", "CpAttr3");
    buildCpAttr("cpattr4", "cpattr4.mf", "CpAttr4",
        "CpAttr2", "CpAttr3", "CpAttr4", "CpAttr5");
    buildCpAttr("cpattr5_123456789_223456789_323456789_423456789_523456789_623456789", "cpattr5_extra_long.mf", "CpAttr5", "CpAttr5");

    for (int i=1; i<=2; i++) {
      String jar1 = TestCommon.getTestJar("cpattr1.jar");
      String jar4 = TestCommon.getTestJar("cpattr4.jar");
      if (i == 2) {
        // Test case #2 -- same as #1, except we use cpattr1_long.jar, which has a super-long
        // Class-Path: attribute.
        jar1 = TestCommon.getTestJar("cpattr1_long.jar");
      }
      String cp = jar1 + File.pathSeparator + jar4;

      TestCommon.testDump(cp, TestCommon.list("CpAttr1",
                                                          "CpAttr2",
                                                          "CpAttr3",
                                                          "CpAttr4",
                                                          "CpAttr5"));

      TestCommon.run(
          "-cp", cp,
          "CpAttr1")
        .assertNormalExit();

      // Logging test for class+path.
      TestCommon.run(
          "-Xlog:class+path",
          "-cp", cp,
          "CpAttr1")
        .assertNormalExit(output -> {
            output.shouldMatch("checking shared classpath entry: .*cpattr2.jar");
            output.shouldMatch("checking shared classpath entry: .*cpattr3.jar");
          });
    }
  }

  static void testNonExistentJars() throws Exception {
    buildCpAttr("cpattr6", "cpattr6.mf", "CpAttr6", "CpAttr6");

    String cp = TestCommon.getTestJar("cpattr6.jar");
    String nonExistPath = CDSTestUtils.getOutputDir() + File.separator + "cpattrX.jar";
    (new File(nonExistPath)).delete();

    TestCommon.testDump(cp, TestCommon.list("CpAttr6"),
        "-Xlog:class+path");

    TestCommon.run(
        "-Xlog:class+path",
        "-cp", cp,
        "CpAttr6")
      .assertNormalExit(output -> {
          output.shouldMatch("should be non-existent: .*cpattrX.jar");
        });

    // Now make nonExistPath exist. CDS still loads, but archived non-system classes will not be used.
    Files.copy(Paths.get(cp), Paths.get(nonExistPath),
               StandardCopyOption.REPLACE_EXISTING);

    TestCommon.run(
        "-Xlog:class+path",
        "-cp", cp,
        "CpAttr6")
      .assertNormalExit(output -> {
          output.shouldMatch("Archived non-system classes are disabled because the file .*cpattrX.jar exists");
        });
  }

  private static void buildCpAttr(String jarName, String manifest, String enclosingClassName, String ...testClassNames) throws Exception {
    String jarClassesDir = CDSTestUtils.getOutputDir() + File.separator + jarName + "_classes";
    try { Files.createDirectory(Paths.get(jarClassesDir)); } catch (FileAlreadyExistsException e) { }

    JarBuilder.compile(jarClassesDir, System.getProperty("test.src") + File.separator +
        "test-classes" + File.separator + enclosingClassName + ".java");
    JarBuilder.buildWithManifest(jarName, manifest, jarClassesDir, testClassNames);
  }
}
