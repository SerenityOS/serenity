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
 * @bug 8266330
 * @summary CDS support of old classes with major version < JDK_6 (50) for static archive.
 *          Test a scenario that a class implements an old interface but the
 *          implementation is in another class.
 * @requires vm.cds
 * @library /test/lib
 * @compile test-classes/OldInf.jasm
 * @compile test-classes/InfMethod.java
 * @compile test-classes/IndirectImpInf.java
 * @compile test-classes/IndirectImpInfApp.java
 * @run driver OldSuperInfIndirect
 */

import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;

public class OldSuperInfIndirect {

    public static void main(String[] args) throws Exception {
        String mainClass = "IndirectImpInfApp";
        String namePrefix = "indirectimpinfapp";
        String appClasses[] = TestCommon.list("OldInf", "InfMethod", "IndirectImpInf", mainClass);
        JarBuilder.build(namePrefix, appClasses);

        String appJar = TestCommon.getTestJar(namePrefix + ".jar");
        String archiveName = namePrefix + ".jsa";

        boolean dynamicMode = CDSTestUtils.DYNAMIC_DUMP;

        // create archive with class list
        OutputAnalyzer output = TestCommon.dump(appJar, appClasses, "-Xlog:class+load,cds=debug,verification=trace");
        TestCommon.checkExecReturn(output, 0,
                                   dynamicMode ? true : false,
                                   "Skipping OldInf: Old class has been linked",
                                   "Skipping IndirectImpInf: Old class has been linked");

        // run with archive
        TestCommon.run(
            "-cp", appJar,
            "-Xlog:class+load,cds=debug,verification=trace",
            mainClass)
          .assertNormalExit(out -> {
              out.shouldContain("Verifying class OldInf with old format")
                 .shouldContain("Verifying class IndirectImpInf with new format");
              if (!dynamicMode) {
                  out.shouldContain("OldInf source: shared objects file")
                     .shouldContain("InfMethod source: shared objects file")
                     .shouldContain("IndirectImpInf source: shared objects file");
              } else {
                  // Old classes were already linked before dynamic dump happened,
                  // so they couldn't be archived.
                  out.shouldMatch(".class.load.*OldInf source:.*indirectimpinfapp.jar")
                     .shouldMatch(".class.load.*IndirectImpInf source:.*indirectimpinfapp.jar")
                     .shouldContain("InfMethod source: shared objects file (top)");
              }
          });
    }
}
