/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

import jdk.jpackage.test.Annotations.Test;
import jdk.jpackage.test.PackageTest;
import jdk.jpackage.test.PackageType;


/**
 * Test --linux-package-name parameter. Output of the test should be
 * quickbrownfox2_1.0-1_amd64.deb or quickbrownfox2-1.0-1.amd64.rpm package
 * bundle. The output package should provide the same functionality as the
 * default package.
 *
 * deb:
 * Package property of the package should be set to quickbrownfox2.
 *
 * rpm:
 * Name property of the package should be set to quickbrownfox2.
 */


/*
 * @test
 * @summary jpackage with --linux-package-name
 * @library ../helpers
 * @key jpackagePlatformPackage
 * @build jdk.jpackage.test.*
 * @build LinuxBundleNameTest
 * @requires (os.family == "linux")
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=LinuxBundleNameTest
 */
public class LinuxBundleNameTest {

    @Test
    public static void test() {
        final String PACKAGE_NAME = "quickbrownfox2";

        new PackageTest()
                .forTypes(PackageType.LINUX)
                .configureHelloApp()
                .addInitializer(cmd -> {
                    cmd.addArguments("--linux-package-name", PACKAGE_NAME);
                })
                .forTypes(PackageType.LINUX_DEB)
                .addBundlePropertyVerifier("Package", PACKAGE_NAME)
                .forTypes(PackageType.LINUX_RPM)
                .addBundlePropertyVerifier("Name", PACKAGE_NAME)
                .run();
    }
}
