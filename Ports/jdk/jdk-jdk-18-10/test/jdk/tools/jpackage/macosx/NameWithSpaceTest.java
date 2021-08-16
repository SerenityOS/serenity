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

import jdk.jpackage.test.PackageTest;
import jdk.jpackage.test.Annotations.Test;

/**
 * Name with space packaging test. Output of the test should be
 * "Name With Space-*.*" package bundle.
 *
 * macOS only:
 *
 * Test should generates basic pkg and dmg. Name of packages and application itself
 * should have name: "Name With Space". Package should be installed into "/Applications"
 * folder and verified that it can be installed and run.
 */

/*
 * @test
 * @summary jpackage test with name containing spaces
 * @library ../helpers
 * @build jdk.jpackage.test.*
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile NameWithSpaceTest.java
 * @requires (os.family == "mac")
 * @key jpackagePlatformPackage
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=NameWithSpaceTest
 */
public class NameWithSpaceTest {

    @Test
    public static void test() {
        new PackageTest()
        .configureHelloApp()
        .addBundleDesktopIntegrationVerifier(false)
        .addInitializer(cmd -> {
            cmd.setArgumentValue("--name", "Name With Space");
        })
        .run();
    }
}
