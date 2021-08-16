/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @summary AppCDS handling of prohibited package.
 * @requires vm.cds
 * @library /test/lib
 * @compile test-classes/ProhibitedHelper.java test-classes/Prohibited.jasm
 * @run driver ProhibitedPackage
 */

import jdk.test.lib.cds.CDSOptions;
import jdk.test.lib.Platform;
import jdk.test.lib.process.OutputAnalyzer;

public class ProhibitedPackage {

    public static void main(String[] args) throws Exception {
        JarBuilder.build("prohibited_pkg", "java/lang/Prohibited", "ProhibitedHelper");

        String appJar = TestCommon.getTestJar("prohibited_pkg.jar");

        // Test support for customer loaders
        if (Platform.areCustomLoadersSupportedForCDS() &&
            !TestCommon.isDynamicArchive()) {
            String classlist[] = new String[] {
                "java/lang/Object id: 1",
                "java/lang/Prohibited id: 2 super: 1 source: " + appJar
            };

            // Make sure a class in a prohibited package for a custom loader
            // will be ignored during dumping.
            TestCommon.dump(appJar,  classlist, "-Xlog:cds")
                .shouldContain("Dumping")
                .shouldContain("[cds] Prohibited package for non-bootstrap classes: java/lang/Prohibited.class")
                .shouldHaveExitValue(0);
        }


        // Make sure a class in a prohibited package for a non-custom loader
        // will be ignored during dumping.
        TestCommon.dump(appJar,
                        TestCommon.list("java/lang/Prohibited", "ProhibitedHelper"),
                        "-Xlog:class+load")
            .shouldContain("Dumping")
            .shouldNotContain("[info][class,load] java.lang.Prohibited source: ")
            .shouldHaveExitValue(0);

        // Try loading the class in a prohibited package with various -Xshare
        // modes. The class shouldn't be loaded and appropriate exceptions
        // are expected.

        OutputAnalyzer output;

        // -Xshare:on
        TestCommon.run(
            "-XX:+UnlockDiagnosticVMOptions", "-XX:+WhiteBoxAPI",
            "-cp", appJar, "ProhibitedHelper")
          .assertNormalExit("Prohibited package name: java.lang");

        // -Xshare:auto
        output = TestCommon.execAuto(
            "-XX:+UnlockDiagnosticVMOptions", "-XX:+WhiteBoxAPI",
            "-cp", appJar, "ProhibitedHelper");
        CDSOptions opts = (new CDSOptions()).setXShareMode("auto");
        TestCommon.checkExec(output, opts, "Prohibited package name: java.lang");

        // -Xshare:off
        output = TestCommon.execOff(
            "-XX:+UnlockDiagnosticVMOptions", "-XX:+WhiteBoxAPI",
            "-cp", appJar, "ProhibitedHelper");
        output.shouldContain("Prohibited package name: java.lang");
    }
}
