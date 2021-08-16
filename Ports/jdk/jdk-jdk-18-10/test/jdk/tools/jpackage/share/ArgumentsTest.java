/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
import jdk.jpackage.test.HelloApp;
import jdk.jpackage.test.Functional.ThrowingConsumer;
import jdk.jpackage.test.JPackageCommand;
import jdk.jpackage.test.Annotations.BeforeEach;
import jdk.jpackage.test.Annotations.Test;
import jdk.jpackage.test.Annotations.Parameter;


/*
 * Tricky arguments used in the test require a bunch of levels of character
 * escaping for proper encoding them in a single string to be used as a value of
 * `--arguments` option. String with encoded arguments doesn't go through the
 * system to jpackage executable as is because OS is interpreting escape
 * characters. This is true for Windows at least.
 *
 * String mapping performed by the system corrupts the string and jpackage exits
 * with error. There is no problem with string corruption when jpackage is used
 * as tool provider. This is not jpackage issue, so just always run this test
 * with jpackage used as tool provider.
 * /

/*
 * @test
 * @summary jpackage create image with --arguments test
 * @library ../helpers
 * @build jdk.jpackage.test.*
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile ArgumentsTest.java
 * @run main/othervm -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=ArgumentsTest
 */
public class ArgumentsTest {

    @BeforeEach
    public static void useJPackageToolProvider() {
        JPackageCommand.useToolProviderByDefault();
    }

    @Test
    @Parameter("Goodbye")
    @Parameter("com.hello/com.hello.Hello")
    public static void testApp(String javaAppDesc) {
        testIt(javaAppDesc, null);
    }

    private static void testIt(String javaAppDesc,
            ThrowingConsumer<JPackageCommand> initializer) {

        JPackageCommand cmd = JPackageCommand.helloAppImage(javaAppDesc).addArguments(
                "--arguments", JPackageCommand.escapeAndJoin(TRICKY_ARGUMENTS));
        if (initializer != null) {
            ThrowingConsumer.toConsumer(initializer).accept(cmd);
        }

        cmd.executeAndAssertImageCreated();

        Path launcherPath = cmd.appLauncherPath();
        if (!cmd.isFakeRuntime(String.format(
                "Not running [%s] launcher", launcherPath))) {
            HelloApp.assertApp(launcherPath)
                    .addDefaultArguments(TRICKY_ARGUMENTS)
                    .executeAndVerifyOutput();
        }
    }

    private final static List<String> TRICKY_ARGUMENTS = List.of(
        "argument",
        "Some Arguments",
        "Value \"with\" quotes"
    );
}
