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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import jdk.jpackage.test.HelloApp;
import jdk.jpackage.test.TKit;
import jdk.jpackage.test.Functional.ThrowingConsumer;
import jdk.jpackage.test.JPackageCommand;
import jdk.jpackage.test.Annotations.Test;

/*
 * @test
 * @summary jpackage test app can run after changing executable's extension
 * @library ../helpers
 * @key jpackagePlatformPackage
 * @build jdk.jpackage.test.*
 * @build WinRenameTest
 * @requires (os.family == "windows")
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=WinRenameTest
 */
public class WinRenameTest {

    @Test
    public static void test() throws IOException {
        String javaAppDesc = "com.hello/com.hello.Hello";
        JPackageCommand cmd = JPackageCommand.helloAppImage(javaAppDesc);

        cmd.executeAndAssertImageCreated();

        Path launcherPath = cmd.appLauncherPath();
        HelloApp.assertApp(launcherPath).executeAndVerifyOutput();

        String lp = launcherPath.toString();
        TKit.assertTrue(lp.endsWith(".exe"), "UNexpected launcher path: " + lp);

        Path newLauncherPath = Path.of(lp.replaceAll(".exe", ".anything"));
        Files.move(launcherPath, newLauncherPath);

        HelloApp.assertApp(newLauncherPath).executeAndVerifyOutput();
    }
}
