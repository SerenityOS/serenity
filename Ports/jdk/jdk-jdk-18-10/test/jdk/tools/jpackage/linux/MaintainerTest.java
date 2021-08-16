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

import jdk.jpackage.test.PackageTest;
import jdk.jpackage.test.PackageType;
import jdk.jpackage.test.Annotations.Test;


/**
 * Test --linux-deb-maintainer parameter. Output of the test should be
 * maintainertest_1.0-1_amd64.deb package bundle. The output package
 * should provide the same functionality as the
 * default package.
 * Value of Maintainer property of the package should contain
 * jpackage-test@java.com email address.
 */


/*
 * @test
 * @summary jpackage with --linux-deb-maintainer
 * @library ../helpers
 * @key jpackagePlatformPackage
 * @build jdk.jpackage.test.*
 * @build MaintainerTest
 * @requires (os.family == "linux")
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=MaintainerTest
 */
public class MaintainerTest {

    @Test
    public static void test() {
        final String MAINTAINER = "jpackage-test@java.com";

        new PackageTest().forTypes(PackageType.LINUX_DEB).configureHelloApp()
                .addInitializer(cmd -> {
                    cmd.addArguments("--linux-deb-maintainer", MAINTAINER);
                })
                .addBundlePropertyVerifier("Maintainer", value -> {
                    String lookupValue = "<" + MAINTAINER + ">";
                    return value.endsWith(lookupValue);
                }, "ends with")
                .run();
    }
}
