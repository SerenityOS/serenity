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
 * Test --linux-app-category parameter. Output of the test should be
 * appcategorytest_1.0-1_amd64.deb or appcategorytest-1.0-1.amd64.rpm package
 * bundle. The output package should provide the same functionality as the
 * default package.
 *
 * deb:
 * Section property of the package should be set to Foo value.
 *
 * rpm:
 * Group property of the package should be set to Foo value.
 */


/*
 * @test
 * @summary jpackage with --linux-app-category
 * @library ../helpers
 * @key jpackagePlatformPackage
 * @build jdk.jpackage.test.*
 * @build AppCategoryTest
 * @requires (os.family == "linux")
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=AppCategoryTest
 */
public class AppCategoryTest {

    @Test
    public static void test() {
        final String CATEGORY = "Foo";

        new PackageTest()
                .forTypes(PackageType.LINUX)
                .configureHelloApp()
                .addInitializer(cmd -> {
                    cmd.addArguments("--linux-app-category", CATEGORY);
                })
                .forTypes(PackageType.LINUX_DEB)
                .addBundlePropertyVerifier("Section", CATEGORY)
                .forTypes(PackageType.LINUX_RPM)
                .addBundlePropertyVerifier("Group", CATEGORY)
                .run();
    }
}
