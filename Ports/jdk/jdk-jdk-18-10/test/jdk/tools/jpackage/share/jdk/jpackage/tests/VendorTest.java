/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jpackage.tests;

import jdk.jpackage.test.PackageTest;
import jdk.jpackage.test.PackageType;
import jdk.jpackage.test.Annotations.Test;

/**
 * Test --vendor parameter. Output of the test should be
 * vendortest*.* package bundle. The output package should provide the
 * same functionality as the default package with the default value of vendor
 * property overridden.
 *
 * Linux DEB:
 *
 * Value of "Maintainer" property of .deb package should start with "Test Vendor" string.
 *
 * Linux RPM:
 *
 * Value of "Vendor" property of .rpm package should be set to "Test Vendor" string.
 *
 * Mac:
 *
 * --vendor parameter is ignored.
 *
 * Windows:
 *
 * Publisher value displayed in the Add/Remove Programs should be set
 * to "Test Vendor" string.
 */

/*
 * @test
 * @summary Test --vendor jpackage command option
 * @library ../../../../helpers
 * @key jpackagePlatformPackage
 * @requires (os.family == "windows")
 * @requires jpackage.test.SQETest != null
 * @build jdk.jpackage.test.*
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile VendorTest.java
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=jdk.jpackage.tests.VendorTest
 */

/*
 * @test
 * @summary Test --vendor jpackage command option
 * @library ../../../../helpers
 * @key jpackagePlatformPackage
 * @requires (os.family != "mac")
 * @requires jpackage.test.SQETest == null
 * @build jdk.jpackage.test.*
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile VendorTest.java
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=jdk.jpackage.tests.VendorTest
 */
public class VendorTest {

    @Test
    public static void test() {
        final String vendorValue = "Test Vendor";

        new PackageTest()
        .configureHelloApp()
        .addBundleDesktopIntegrationVerifier(false)
        .addInitializer(cmd -> {
            cmd.addArguments("--vendor", vendorValue);
        })
        .forTypes(PackageType.LINUX_DEB)
        .addBundlePropertyVerifier("Maintainer", value -> {
            return value.startsWith(vendorValue + " ");
        }, "starts with")
        .forTypes(PackageType.LINUX_RPM)
        .addBundlePropertyVerifier("Vendor", value -> {
            return value.equals(vendorValue);
        }, "equals to")
        .forTypes(PackageType.WIN_MSI)
        .addBundlePropertyVerifier("Manufacturer", value -> {
            return value.equals(vendorValue);
        }, "equals to")
        .run();
    }
}
