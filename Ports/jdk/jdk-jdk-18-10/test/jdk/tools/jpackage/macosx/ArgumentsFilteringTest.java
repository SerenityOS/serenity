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

import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import jdk.jpackage.test.JPackageCommand;
import jdk.jpackage.test.HelloApp;
import jdk.jpackage.test.Annotations.Test;

/**
 * Tests generation of app image and then launches app by passing -psn_1_1
 * argument via command line and checks that -psn_1_1 is not passed to
 * application. Second test app image is generated -psn_2_2 and then app is
 * launched with -psn_1_1 and we should filter -psn_1_1 and keep -psn_2_2.
 * See JDK-8255947.
 */

/*
 * @test
 * @summary jpackage with -psn
 * @library ../helpers
 * @build jdk.jpackage.test.*
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile ArgumentsFilteringTest.java
 * @requires (os.family == "mac")
 * @run main/othervm/timeout=540 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=ArgumentsFilteringTest
 */
public class ArgumentsFilteringTest {

    @Test
    public void test1() {
        JPackageCommand cmd = JPackageCommand.helloAppImage();
        cmd.executeAndAssertHelloAppImageCreated();
        Path launcherPath = cmd.appLauncherPath();
        HelloApp.assertApp(launcherPath)
                .executeAndVerifyOutput(false, List.of("-psn_1_1"),
                        new ArrayList<>());
    }

    @Test
    public void test2() {
        JPackageCommand cmd = JPackageCommand.helloAppImage()
                .addArguments("--arguments", "-psn_2_2");
        cmd.executeAndAssertHelloAppImageCreated();
        Path launcherPath = cmd.appLauncherPath();
        HelloApp.assertApp(launcherPath)
                .executeAndVerifyOutput(false, List.of("-psn_1_1"),
                        List.of("-psn_2_2"));
    }
}
