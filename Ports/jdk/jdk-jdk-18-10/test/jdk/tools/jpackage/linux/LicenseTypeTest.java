/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * Test --linux-rpm-license-type parameter. Output of the test should be
 * licensetypetest-1.0-1.amd64.rpm package bundle. The output package
 * should provide the same functionality as the
 * default package.
 * License property of the package should be set to JP_LICENSE_TYPE.
 */


/*
 * @test
 * @summary jpackage with --linux-rpm-license-type
 * @library ../helpers
 * @key jpackagePlatformPackage
 * @build jdk.jpackage.test.*
 * @build LicenseTypeTest
 * @requires (os.family == "linux")
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=LicenseTypeTest
 */
public class LicenseTypeTest {

    @Test
    public static void test() {
        final String LICENSE_TYPE = "JP_LICENSE_TYPE";

        new PackageTest().forTypes(PackageType.LINUX_RPM).configureHelloApp()
                .addInitializer(cmd -> {
                    cmd.addArguments("--linux-rpm-license-type", LICENSE_TYPE);
                })
                .addBundlePropertyVerifier("License", LICENSE_TYPE)
                .run();
    }
}
