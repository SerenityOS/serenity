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

package jdk.jpackage.tests;

import java.util.Collection;
import java.util.List;
import jdk.jpackage.test.Annotations.Parameters;
import jdk.jpackage.test.Annotations.Test;
import jdk.jpackage.test.JPackageCommand;
import jdk.jpackage.test.HelloApp;
import jdk.jpackage.test.Executor;
import jdk.jpackage.test.TKit;

/*
 * @test
 * @summary jpackage create image with --java-options test
 * @library ../../../../helpers
 * @build jdk.jpackage.test.*
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile JavaOptionsEqualsTest.java
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=jdk.jpackage.tests.JavaOptionsEqualsTest
 *  --jpt-before-run=jdk.jpackage.test.JPackageCommand.useExecutableByDefault
 */

/*
 * @test
 * @summary jpackage create image with --java-options test
 * @library ../../../../helpers
 * @build jdk.jpackage.test.*
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile JavaOptionsEqualsTest.java
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=jdk.jpackage.tests.JavaOptionsEqualsTest
 *  --jpt-before-run=jdk.jpackage.test.JPackageCommand.useToolProviderByDefault
 */

public class JavaOptionsEqualsTest {

    private final static String OPTION1 =
        "--add-exports=java.base/sun.util=me.mymodule.foo,ALL-UNNAMED";
    private final static String OPTION2 =
        "--add-exports=java.base/sun.security.util=other.mod.bar,ALL-UNNAMED";
    private final static String WARNING1 =
        "WARNING: Unknown module: me.mymodule.foo";
    private final static String WARNING2 =
        "WARNING: Unknown module: other.mod.bar";

    private final JPackageCommand cmd;

    @Parameters
    public static Collection input() {
        return List.of(new Object[][]{
            {"Hello", new String[]{"--java-options", OPTION1,
                                   "--java-options", OPTION2 },
            },
        });
    }

    public JavaOptionsEqualsTest(String javaAppDesc, String[] jpackageArgs) {
        cmd = JPackageCommand.helloAppImage(javaAppDesc);
        if (jpackageArgs != null) {
            cmd.addArguments(jpackageArgs);
        }
    }

    @Test
    public void test() {
        cmd.executeAndAssertHelloAppImageCreated();
        List<String> output = HelloApp.executeLauncher(cmd).getOutput();
        TKit.assertNotNull(output, "output is null");
        TKit.assertTextStream(WARNING1).apply(output.stream());
        TKit.assertTextStream(WARNING2).apply(output.stream());
    }
}
