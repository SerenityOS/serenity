/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import jdk.jpackage.test.TKit;
import jdk.jpackage.test.PackageTest;
import jdk.jpackage.test.PackageType;
import jdk.jpackage.test.LinuxHelper;
import jdk.jpackage.test.Annotations.Test;


/**
 * Test --linux-package-deps parameter. Output of the test should be
 * apackagedepstestprereq_1.0-1_amd64.deb and packagedepstest_1.0-1_amd64.deb or
 * apackagedepstestprereq-1.0-1.amd64.rpm and packagedepstest-1.0-1.amd64.rpm
 * package bundles. The output packages should provide the same functionality as
 * the default package.
 *
 * deb: Value of Depends property of packagedepstest package should contain
 * apackagedepstestprereq word.
 *
 * rpm: Value of Requires property of packagedepstest package should contain
 * apackagedepstestprereq word.
 */


/*
 * @test
 * @summary jpackage with --linux-package-deps
 * @library ../helpers
 * @key jpackagePlatformPackage
 * @build jdk.jpackage.test.*
 * @requires (os.family == "linux")
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile PackageDepsTest.java
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=PackageDepsTest
 */
public class PackageDepsTest {

    @Test
    public static void test() {
        final String PREREQ_PACKAGE_NAME = "apackagedepstestprereq";

        PackageTest test1 = new PackageTest()
        .forTypes(PackageType.LINUX)
        .configureHelloApp()
        .addInitializer(cmd -> {
            cmd.setArgumentValue("--name", PREREQ_PACKAGE_NAME);
        });

        PackageTest test2 = new PackageTest()
        .forTypes(PackageType.LINUX)
        .configureHelloApp()
        .addInitializer(cmd -> {
            cmd.addArguments("--linux-package-deps", PREREQ_PACKAGE_NAME);
        })
        .forTypes(PackageType.LINUX)
        .addBundleVerifier(cmd -> {
            TKit.assertTrue(
                    LinuxHelper.getPrerequisitePackages(cmd).contains(
                            PREREQ_PACKAGE_NAME), String.format(
                            "Check package depends on [%s] package",
                            PREREQ_PACKAGE_NAME));
        });

        new PackageTest.Group(test1, test2).run();
    }
}
