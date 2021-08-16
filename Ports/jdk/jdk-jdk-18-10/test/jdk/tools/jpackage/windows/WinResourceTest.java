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

import java.io.IOException;
import java.nio.file.Path;
import jdk.jpackage.test.TKit;
import jdk.jpackage.test.PackageTest;
import jdk.jpackage.test.PackageType;
import jdk.jpackage.test.JPackageCommand;
import jdk.jpackage.test.Annotations.Test;
import jdk.jpackage.test.Annotations.Parameters;
import java.util.List;

/**
 * Test --resource-dir option. The test should set --resource-dir to point to
 * a dir with an empty "main.wxs" file.  As a result, jpackage should try to
 * use the customized resource and fail.
 */

/*
 * @test
 * @summary jpackage with --resource-dir
 * @library ../helpers
 * @build jdk.jpackage.test.*
 * @requires (os.family == "windows")
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile WinResourceTest.java
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=WinResourceTest
 */

public class WinResourceTest {

    public WinResourceTest(String wixSource, String expectedLogMessage) {
         this.wixSource = wixSource;
         this.expectedLogMessage = expectedLogMessage;
    }

    @Parameters
    public static List<Object[]> data() {
        return List.of(new Object[][]{
            {"main.wxs", "Using custom package resource [Main WiX project file]"},
            {"overrides.wxi", "Using custom package resource [Overrides WiX project file]"},
        });
    }

    @Test
    public void test() throws IOException {
        new PackageTest()
        .forTypes(PackageType.WINDOWS)
        .configureHelloApp()
        .addInitializer(cmd -> {
            Path resourceDir = TKit.createTempDirectory("resources");

            // 1. Set fake run time to save time by skipping jlink step of jpackage.
            // 2. Instruct test to save jpackage output.
            cmd.setFakeRuntime().saveConsoleOutput(true);

            cmd.addArguments("--resource-dir", resourceDir);
            // Create invalid WiX source file in a resource dir.
            TKit.createTextFile(resourceDir.resolve(wixSource), List.of(
                    "any string that is an invalid WiX source file"));
        })
        .addBundleVerifier((cmd, result) -> {
            // Assert jpackage picked custom main.wxs and failed as expected by
            // examining its output
            TKit.assertTextStream(expectedLogMessage)
                    .predicate(String::startsWith)
                    .apply(JPackageCommand.stripTimestamps(
                            result.getOutput().stream()));
            TKit.assertTextStream("error CNDL0104 : Not a valid source file")
                    .apply(result.getOutput().stream());
        })
        .setExpectedExitCode(1)
        .run();
    }

    final String wixSource;
    final String expectedLogMessage;
}
