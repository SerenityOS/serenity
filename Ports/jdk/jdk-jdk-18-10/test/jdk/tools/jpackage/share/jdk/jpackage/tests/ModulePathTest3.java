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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import jdk.jpackage.internal.AppImageFile;
import jdk.jpackage.test.HelloApp;
import jdk.jpackage.test.JavaAppDesc;
import jdk.jpackage.test.Annotations.Test;
import jdk.jpackage.test.Annotations.Parameter;
import jdk.jpackage.test.Annotations.Parameters;
import jdk.jpackage.test.Executor;
import jdk.jpackage.test.JPackageCommand;
import jdk.jpackage.test.JavaTool;
import jdk.jpackage.test.PackageType;
import jdk.jpackage.test.TKit;
import org.w3c.dom.Document;


/*
 * @test
 * @summary jpackage for app's module linked in external runtime
 * @library ../../../../helpers
 * @build jdk.jpackage.test.*
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile ModulePathTest3.java
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=jdk.jpackage.tests.ModulePathTest3
 */

public final class ModulePathTest3 {

    public ModulePathTest3(String jlinkOutputSubdir, String runtimeSubdir) {
        this.jlinkOutputSubdir = Path.of(jlinkOutputSubdir);
        this.runtimeSubdir = Path.of(runtimeSubdir);
    }

    /**
     * Test case for JDK-8248254.
     * App's module in runtime directory.
     */
    @Test
    public void test8248254() throws XPathExpressionException, IOException {
        testIt("me.mymodule/me.mymodule.Main");
    }

    private void testIt(String mainAppDesc) throws XPathExpressionException,
            IOException {
        final JavaAppDesc appDesc = JavaAppDesc.parse(mainAppDesc);
        final Path moduleOutputDir = TKit.createTempDirectory("modules");
        HelloApp.createBundle(appDesc, moduleOutputDir);

        final Path workDir = TKit.createTempDirectory("runtime").resolve("data");
        final Path jlinkOutputDir = workDir.resolve(jlinkOutputSubdir);
        Files.createDirectories(jlinkOutputDir.getParent());

        new Executor()
        .setToolProvider(JavaTool.JLINK)
        .dumpOutput()
        .addArguments(
                "--add-modules", appDesc.moduleName(),
                "--output", jlinkOutputDir.toString(),
                "--module-path", moduleOutputDir.resolve(appDesc.jarFileName()).toString(),
                "--strip-debug",
                "--no-header-files",
                "--no-man-pages",
                "--strip-native-commands")
        .execute();

        JPackageCommand cmd = new JPackageCommand()
        .setDefaultAppName()
        .setPackageType(PackageType.IMAGE)
        .setDefaultInputOutput()
        .removeArgumentWithValue("--input")
        .addArguments("--module", appDesc.moduleName() + "/" + appDesc.className())
        .setArgumentValue("--runtime-image", workDir.resolve(runtimeSubdir));

        cmd.executeAndAssertHelloAppImageCreated();

        if (appDesc.moduleVersion() != null) {
            Document xml = AppImageFile.readXml(cmd.outputBundle());
            String actualVersion = XPathFactory.newInstance().newXPath().evaluate(
                    "/jpackage-state/app-version/text()", xml,
                    XPathConstants.STRING).toString();

            TKit.assertEquals(appDesc.moduleVersion(), actualVersion,
                    "Check application version");
        }
    }

    @Parameters
    public static Collection data() {
        final List<String[]> paths = new ArrayList<>();
        paths.add(new String[] { "", "" });
        if (TKit.isOSX()) {
            // On OSX jpackage should accept both runtime root and runtime home
            // directories.
            paths.add(new String[] { "Contents/Home", "" });
        }

        List<Object[]> data = new ArrayList<>();
        for (var pathCfg : paths) {
            data.add(new Object[] { pathCfg[0], pathCfg[1] });
        }

        return data;
    }

    private final Path jlinkOutputSubdir;
    private final Path runtimeSubdir;
}
