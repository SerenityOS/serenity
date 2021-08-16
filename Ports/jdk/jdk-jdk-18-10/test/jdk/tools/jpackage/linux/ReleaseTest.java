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

import jdk.jpackage.test.PackageType;
import jdk.jpackage.test.PackageTest;
import jdk.jpackage.test.Annotations.Test;


/**
 * Test --linux-app-release parameter. Output of the test should be
 * releasetest_1.0-Rc3_amd64.deb or releasetest-1.0-Rc3.amd64.rpm package
 * bundle. The output package should provide the same functionality as the
 * default package.
 *
 * deb:
 * Version property of the package should end with -Rc3 substring.
 *
 * rpm:
 * Release property of the package should be set to Rc3 value.
 */

/*
 * @test
 * @summary jpackage with --linux-app-release
 * @library ../helpers
 * @key jpackagePlatformPackage
 * @build jdk.jpackage.test.*
 * @build ReleaseTest
 * @requires (os.family == "linux")
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=ReleaseTest
 */
public class ReleaseTest {

    @Test
    public static void test() {
        final String RELEASE = "Rc3";

        new PackageTest()
                .forTypes(PackageType.LINUX)
                .configureHelloApp()
                .addInitializer(cmd -> {
                    cmd.addArguments("--linux-app-release", RELEASE);
                })
                .forTypes(PackageType.LINUX_RPM)
                .addBundlePropertyVerifier("Release", RELEASE)
                .forTypes(PackageType.LINUX_DEB)
                .addBundlePropertyVerifier("Version", propValue -> {
                    return propValue.endsWith("-" + RELEASE);
                }, "ends with")
                .run();
    }
}
