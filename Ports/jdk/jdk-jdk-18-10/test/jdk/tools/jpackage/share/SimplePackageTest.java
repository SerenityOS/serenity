/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * Simple platform specific packaging test. Output of the test should be
 * simplepackagetest*.* package bundle.
 *
 * Windows:
 *
 * The installer should not have license text. It should not have an option
 * to change the default installation directory.
 * Test application should be installed in %ProgramFiles%\SimplePackageTest directory.
 * Installer should install test app for all users (machine wide).
 * Installer should not create any shortcuts.
 */

/*
 * @test
 * @summary Simple jpackage command run
 * @library ../helpers
 * @key jpackagePlatformPackage
 * @build jdk.jpackage.test.*
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile SimplePackageTest.java
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=SimplePackageTest
 */
public class SimplePackageTest {

    @Test
    public static void test() {
        new PackageTest()
        .configureHelloApp()
        .addBundleDesktopIntegrationVerifier(false)
        .run();
    }
}
