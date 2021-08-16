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

import java.util.Collection;
import java.util.List;
import jdk.jpackage.test.Annotations.Parameters;
import jdk.jpackage.test.Annotations.Test;
import jdk.jpackage.test.JPackageCommand;
import jdk.jpackage.test.TKit;

/*
 * @test
 * @summary jpackage application version testing
 * @library ../../../../helpers
 * @build jdk.jpackage.test.*
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile NonExistentTest.java
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=jdk.jpackage.tests.NonExistentTest
 */

public final class NonExistentTest {

    private final String expectedError;
    private final JPackageCommand cmd;

    @Parameters
    public static Collection input() {
        return List.of(new Object[][]{
            // non-existent icon
            {"Hello",
                    new String[]{"--icon", "non-existent"},
                    "Error:"},
            {"com.other/com.other.Hello",
                    new String[]{"--icon", "non-existent"},
                    "Error:"},
            // non-existent input
            {"Hello",
                    new String[]{"--input", "non-existent"},
                    "Exception:"},
            {"com.other/com.other.Hello",
                    new String[]{"--input", "non-existent"},
                    "Exception:"},
            // non-existent resource-dir
            {"Hello",
                    new String[]{"--resource-dir", "non-existent"},
                    "Specified resource directory"},
            {"com.other/com.other.Hello",
                    new String[]{"--resource-dir", "non-existent"},
                    "Specified resource directory"},
        });
    }

    public NonExistentTest(String javaAppDesc, String[] jpackageArgs,
                String expectedError) {
        this.expectedError = expectedError;

        cmd = JPackageCommand.helloAppImage(javaAppDesc)
                .saveConsoleOutput(true).dumpOutput(true);
        if (jpackageArgs != null) {
            cmd.addArguments(jpackageArgs);
        }
    }

    @Test
    public void test() {
        List<String> output = cmd.execute(1).getOutput();
        TKit.assertNotNull(output, "output is null");
        TKit.assertTextStream(expectedError).apply(output.stream());
    }
}
