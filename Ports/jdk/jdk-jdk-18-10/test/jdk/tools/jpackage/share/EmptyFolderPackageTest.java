/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
import jdk.jpackage.internal.ApplicationLayout;
import jdk.jpackage.test.PackageTest;
import jdk.jpackage.test.PackageType;
import jdk.jpackage.test.Annotations.Test;

/**
 * Tests generation of packages with input folder containing empty folders.
 */

/*
 * @test
 * @summary jpackage with input containing empty folders
 * @library ../helpers
 * @library /test/lib
 * @key jpackagePlatformPackage
 * @build EmptyFolderBase
 * @build jdk.jpackage.test.*
 * @build EmptyFolderPackageTest
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @run main/othervm/timeout=720 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=EmptyFolderPackageTest
 */
public class EmptyFolderPackageTest {

    @Test
    public static void test() throws Exception {
        new PackageTest().configureHelloApp()
                .addInitializer(cmd -> {
                    Path input = cmd.inputDir();
                    EmptyFolderBase.createDirStrcture(input);
                })
                .addInstallVerifier(cmd -> {
                    if (cmd.packageType() == PackageType.WIN_MSI) {
                        if (cmd.isPackageUnpacked("Not running file "
                                + "structure check for empty folders")) {
                            return;
                        }
                    }

                    ApplicationLayout appLayout = cmd.appLayout();
                    Path appDir = appLayout.appDirectory();
                    EmptyFolderBase.validateDirStrcture(appDir);
                })
                .run();
    }
}
