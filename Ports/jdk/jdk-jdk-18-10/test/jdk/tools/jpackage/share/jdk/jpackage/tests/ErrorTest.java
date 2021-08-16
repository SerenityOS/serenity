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
 * @compile ErrorTest.java
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=jdk.jpackage.tests.ErrorTest
 *  --jpt-before-run=jdk.jpackage.test.JPackageCommand.useExecutableByDefault
 */

/*
 * @test
 * @summary jpackage application version testing
 * @library ../../../../helpers
 * @build jdk.jpackage.test.*
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile ErrorTest.java
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=jdk.jpackage.tests.ErrorTest
 *  --jpt-before-run=jdk.jpackage.test.JPackageCommand.useToolProviderByDefault
 */

public final class ErrorTest {

    private final String expectedError;
    private final JPackageCommand cmd;

    @Parameters
    public static Collection input() {
        return List.of(new Object[][]{
            // non-existent arg
            {"Hello",
                    new String[]{"--no-such-argument"},
                    null,
                    "Invalid Option: [--no-such-argument]"},
            // no main jar
            {"Hello",
                    null,
                    new String[]{"--main-jar"},
                    "--main-jar or --module"},
            // no main-class
            {"Hello",
                    null,
                    new String[]{"--main-class"},
                    "main class was not specified"},
            // non-existent main jar
            {"Hello",
                    new String[]{"--main-jar", "non-existent.jar"},
                    null,
                    "main jar does not exist"},
            // non-existent runtime
            {"Hello",
                    new String[]{"--runtime-image", "non-existent.runtime"},
                    null,
                    "does not exist"},
            // non-existent resource-dir
            {"Hello",
                    new String[]{"--resource-dir", "non-existent.dir"},
                    null,
                    "does not exist"},
            // invalid type
            {"Hello",
                    new String[]{"--type", "invalid-type"},
                    null,
                    "Invalid or unsupported type:"},
            // no --input
            {"Hello",
                    null,
                    new String[]{"--input"},
                    "Missing argument: --input"},
            // no --module-path
            {"com.other/com.other.Hello",
                    null,
                    new String[]{"--module-path"},
                    "Missing argument: --runtime-image or --module-path"},
        });
    }

    public ErrorTest(String javaAppDesc, String[] jpackageArgs,
                String[] removeArgs,
                String expectedError) {
        this.expectedError = expectedError;

        cmd = JPackageCommand.helloAppImage(javaAppDesc)
                .saveConsoleOutput(true).dumpOutput(true);
        if (jpackageArgs != null) {
            cmd.addArguments(jpackageArgs);
        } if (removeArgs != null) {
            for (String arg : removeArgs) {
                cmd.removeArgumentWithValue(arg);
            }
        }
    }

    @Test
    public void test() {
        List<String> output = cmd.execute(1).getOutput();
        TKit.assertNotNull(output, "output is null");
        TKit.assertTextStream(expectedError).apply(output.stream());
    }

}
