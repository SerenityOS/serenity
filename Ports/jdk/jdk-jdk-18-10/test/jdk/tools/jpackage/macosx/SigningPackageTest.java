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

import java.nio.file.Path;
import jdk.jpackage.test.JPackageCommand;
import jdk.jpackage.test.PackageTest;
import jdk.jpackage.test.PackageType;
import jdk.jpackage.test.MacHelper;
import jdk.jpackage.test.Annotations.Test;

/**
 * Tests generation of dmg and pkg with --mac-sign and related arguments.
 * Test will generate pkg and verifies its signature. It verifies that dmg
 * is not signed, but app image inside dmg is signed. This test requires that
 * the machine is configured with test certificate for
 * "Developer ID Installer: jpackage.openjdk.java.net" in
 * jpackagerTest keychain with
 * always allowed access to this keychain for user which runs test.
 * note:
 * "jpackage.openjdk.java.net" can be over-ridden by systerm property
 * "jpackage.mac.signing.key.user.name", and
 * "jpackagerTest" can be over-ridden by system property
 * "jpackage.mac.signing.keychain"
 */

/*
 * @test
 * @summary jpackage with --type pkg,dmg --mac-sign
 * @library ../helpers
 * @library /test/lib
 * @library base
 * @key jpackagePlatformPackage
 * @build SigningBase
 * @build SigningCheck
 * @build jtreg.SkippedException
 * @build jdk.jpackage.test.*
 * @build SigningPackageTest
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @requires (os.family == "mac")
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=SigningPackageTest
 */
public class SigningPackageTest {

    private static void verifyPKG(JPackageCommand cmd) {
        Path outputBundle = cmd.outputBundle();
        SigningBase.verifyPkgutil(outputBundle);
        SigningBase.verifySpctl(outputBundle, "install");
    }

    private static void verifyDMG(JPackageCommand cmd) {
        Path outputBundle = cmd.outputBundle();
        SigningBase.verifyCodesign(outputBundle, false);
    }

    private static void verifyAppImageInDMG(JPackageCommand cmd) {
        MacHelper.withExplodedDmg(cmd, dmgImage -> {
            Path launcherPath = dmgImage.resolve(Path.of("Contents", "MacOS", cmd.name()));
            SigningBase.verifyCodesign(launcherPath, true);
            SigningBase.verifyCodesign(dmgImage, true);
            SigningBase.verifySpctl(dmgImage, "exec");
        });
    }

    @Test
    public static void test() throws Exception {
        SigningCheck.checkCertificates();

        new PackageTest()
                .configureHelloApp()
                .forTypes(PackageType.MAC)
                .addInitializer(cmd -> {
                    cmd.addArguments("--mac-sign",
                            "--mac-signing-key-user-name", SigningBase.DEV_NAME,
                            "--mac-signing-keychain", SigningBase.KEYCHAIN);
                })
                .forTypes(PackageType.MAC_PKG)
                .addBundleVerifier(SigningPackageTest::verifyPKG)
                .forTypes(PackageType.MAC_DMG)
                .addBundleVerifier(SigningPackageTest::verifyDMG)
                .addBundleVerifier(SigningPackageTest::verifyAppImageInDMG)
                .run();
    }
}
