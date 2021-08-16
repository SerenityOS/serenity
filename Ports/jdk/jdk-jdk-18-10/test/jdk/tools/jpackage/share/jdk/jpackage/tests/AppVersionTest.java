/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import jdk.jpackage.internal.AppImageFile;
import jdk.jpackage.test.Annotations.Parameters;
import jdk.jpackage.test.Annotations.Test;
import jdk.jpackage.test.JPackageCommand;
import jdk.jpackage.test.PackageTest;
import jdk.jpackage.test.TKit;
import jdk.jpackage.internal.AppImageFile;
import org.w3c.dom.Document;

/*
 * @test
 * @summary jpackage application version testing
 * @library ../../../../helpers
 * @build jdk.jpackage.test.*
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile AppVersionTest.java
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=jdk.jpackage.tests.AppVersionTest
 */

public final class AppVersionTest {

    @Parameters
    public static Collection input() {
        List<Object[]> data = new ArrayList<>();

        data.addAll(List.of(new Object[][]{
            // Default jpackage version
            {"1.0", "Hello", null},
            {"1.0", "com.other/com.other.Hello", null},
            // Version should be picked from --app-version
            {"3.1", "Hello", new String[]{"--app-version", "3.1"}},
            {"3.2", "com.other/com.other.Hello", new String[]{"--app-version",
                "3.2"}},
            // Version should be picked from the last --app-version
            {"3.3", "Hello", new String[]{"--app-version", "4", "--app-version",
                "3.3"}},
            {"7.8", "com.other/com.other.Hello", new String[]{"--app-version",
                "4", "--app-version", "7.8"}},
            // Pick version from jar
            {"3.10.17", "com.other/com.other.Hello@3.10.17", null},
            // Ignore version in jar if --app-version given
            {"7.5.81", "com.other/com.other.Hello@3.10.17", new String[]{
                "--app-version", "7.5.81"}}
        }));

        // These are invalid version strings.
        // Don't need to test all invalid input as this is handled in
        // PlatformVersionTest unit test
        if (TKit.isWindows()) {
            data.addAll(List.of(new Object[][]{
                {null, "Hello", new String[]{"--app-version", "256"}}
            }));
        } else if (TKit.isOSX()) {
            data.addAll(List.of(new Object[][]{
                {null, "Hello", new String[]{"--app-version", "0.2"}}
            }));
        }

        return data;
    }

    public AppVersionTest(String expectedVersion, String javaAppDesc,
            String[] jpackageArgs) {
        this.expectedVersion = expectedVersion;
        this.javaAppDesc = javaAppDesc;
        this.jpackageArgs = jpackageArgs;
    }

    @Test
    public void test() throws XPathExpressionException, IOException {
        if (expectedVersion == null) {
            new PackageTest()
            .setExpectedExitCode(1)
            .configureHelloApp(javaAppDesc)
            .addInitializer(cmd -> {
                cmd.addArguments(jpackageArgs);
            })
            .run();
            return;
        }

        JPackageCommand cmd = JPackageCommand.helloAppImage(javaAppDesc);
        if (jpackageArgs != null) {
            cmd.addArguments(jpackageArgs);
        }
        cmd.executeAndAssertHelloAppImageCreated();

        Document xml = AppImageFile.readXml(cmd.outputBundle());
        String actualVersion = XPathFactory.newInstance().newXPath().evaluate(
                "/jpackage-state/app-version/text()", xml, XPathConstants.STRING).toString();

        TKit.assertEquals(expectedVersion, actualVersion,
                "Check application version");
    }

    private final String expectedVersion;
    private final String javaAppDesc;
    private final String[] jpackageArgs;
}
