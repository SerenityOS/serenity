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

import java.util.stream.Collectors;
import jdk.jpackage.test.TKit;
import jdk.jpackage.test.Annotations.Test;
import jdk.jpackage.test.Annotations.Parameter;
import jdk.jpackage.test.HelloApp;
import jdk.jpackage.test.JPackageCommand;

/*
 * @test
 * @summary test how app launcher handles unicode command line arguments
 * @library ../../../../helpers
 * @build jdk.jpackage.test.*
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile UnicodeArgsTest.java
 * @requires (os.family == "windows")
 * @run main/othervm/timeout=720 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=jdk.jpackage.tests.UnicodeArgsTest
 */

public final class UnicodeArgsTest {

    @Parameter("true")
    @Parameter("false")
    @Test
    public void test8246042(boolean onCommandLine) {
        final String testString = new String(Character.toChars(0x00E9));

        TKit.trace(String.format("Test string code points: %s", testString
                .codePoints()
                .mapToObj(codePoint -> String.format("0x%04x", codePoint))
                .collect(Collectors.joining(",", "[", "]"))));

        JPackageCommand cmd = JPackageCommand.helloAppImage().useToolProvider(true);
        if (!onCommandLine) {
            cmd.addArguments("--arguments", testString);
        }
        cmd.executeAndAssertImageCreated();

        if (onCommandLine) {
            HelloApp.executeLauncherAndVerifyOutput(cmd, testString);
        } else {
            HelloApp.executeLauncherAndVerifyOutput(cmd);
        }
    }
}
