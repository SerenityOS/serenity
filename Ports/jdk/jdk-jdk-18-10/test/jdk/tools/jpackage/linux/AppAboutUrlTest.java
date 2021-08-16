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
 */

import jdk.jpackage.test.Annotations.Test;
import jdk.jpackage.test.Functional.ThrowingConsumer;
import jdk.jpackage.test.JPackageCommand;
import jdk.jpackage.test.PackageTest;
import jdk.jpackage.test.PackageType;


/**
 * Test --about-url parameter in Linux installers. Output of the test should be
 * appabouturltest_1.0-1_amd64.deb or appabouturltest-1.0-1.amd64.rpm package
 * bundle. The output package should provide the same functionality as the
 * default package.
 *
 * deb:
 * Homepage property of the package should be set to http://foo.com value.
 *
 * rpm:
 * URL property of the package should be set to http://foo.com value.
 */

/*
 * @test
 * @summary jpackage with --about-url
 * @library ../helpers
 * @key jpackagePlatformPackage
 * @build jdk.jpackage.test.*
 * @build AppAboutUrlTest
 * @requires (os.family == "linux")
 * @requires (jpackage.test.SQETest == null)
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=AppAboutUrlTest
 */

/*
 * @test
 * @summary jpackage with --about-url
 * @library ../helpers
 * @key jpackagePlatformPackage
 * @build jdk.jpackage.test.*
 * @build AppAboutUrlTest
 * @requires (os.family == "linux")
 * @requires (jpackage.test.SQETest != null)
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=AppAboutUrlTest.test
 */
public class AppAboutUrlTest {

    @Test
    public static void test() {
        final String ABOUT_URL = "http://foo.com";

        runTest(cmd -> {
            cmd.addArguments("--about-url", ABOUT_URL);
        }, ABOUT_URL, ABOUT_URL);
    }

    @Test
    public static void testDefaults() {
        runTest(JPackageCommand::setFakeRuntime, "", "(none)");
    }

    private static void runTest(ThrowingConsumer<JPackageCommand> initializer,
            String expectedDebHomepage, String expectedRpmUrl) {
        new PackageTest()
                .forTypes(PackageType.LINUX)
                .configureHelloApp()
                .addInitializer(initializer)
                .forTypes(PackageType.LINUX_DEB)
                .addBundlePropertyVerifier("Homepage", expectedDebHomepage)
                .forTypes(PackageType.LINUX_RPM)
                .addBundlePropertyVerifier("URL", expectedRpmUrl)
                .run();
    }
}
