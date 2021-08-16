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

import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import jdk.jpackage.test.JPackageCommand;
import jdk.jpackage.test.PackageTest;
import jdk.jpackage.test.PackageType;
import jdk.jpackage.test.TKit;
import jdk.jpackage.test.Annotations.Test;

/**
 * Test will generation default pkg package and will validate "hostArchitectures"
 * attribute in unpacked pkg package inside Distribution file. See JDK-8266179.
 */

/*
 * @test
 * @summary jpackage test to validate "hostArchitectures" attribute
 * @library ../helpers
 * @build jdk.jpackage.test.*
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile HostArchPkgTest.java
 * @requires (os.family == "mac")
 * @key jpackagePlatformPackage
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=HostArchPkgTest
 */
public class HostArchPkgTest {

    private static void verifyHostArch(JPackageCommand cmd) throws Exception {
        Path distributionFile = cmd.pathToUnpackedPackageFile(Path.of("/"))
                .toAbsolutePath()
                .getParent()
                .resolve("data")
                .resolve("Distribution");

        DocumentBuilderFactory dbf
                = DocumentBuilderFactory.newDefaultInstance();
        dbf.setFeature("http://apache.org/xml/features/" +
                       "nonvalidating/load-external-dtd", false);
        DocumentBuilder b = dbf.newDocumentBuilder();
        org.w3c.dom.Document doc
                = b.parse(Files.newInputStream(distributionFile));

        XPath xPath = XPathFactory.newInstance().newXPath();

        String v = (String) xPath.evaluate(
                    "/installer-gui-script/options/@hostArchitectures",
                    doc, XPathConstants.STRING);

        if ("aarch64".equals(System.getProperty("os.arch"))) {
            TKit.assertEquals(v, "arm64",
                    "Check value of \"hostArchitectures\" attribute");
        } else {
            TKit.assertEquals(v, "x86_64",
                    "Check value of \"hostArchitectures\" attribute");
        }
    }

    @Test
    public static void test() {
        new PackageTest()
                .forTypes(PackageType.MAC_PKG)
                .configureHelloApp()
                .addInstallVerifier(HostArchPkgTest::verifyHostArch)
                .run(PackageTest.Action.CREATE_AND_UNPACK);
    }
}
