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
import java.util.ArrayList;
import jdk.jpackage.test.Annotations.Parameters;
import jdk.jpackage.test.Annotations.Test;
import jdk.jpackage.test.JPackageCommand;
import jdk.jpackage.test.HelloApp;
import jdk.jpackage.test.TKit;

/*
 * @test
 * @summary jpackage create image with --java-options test
 * @library ../../../../helpers
 * @build jdk.jpackage.test.*
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile JavaOptionsTest.java
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=jdk.jpackage.tests.JavaOptionsTest
 *  --jpt-before-run=jdk.jpackage.test.JPackageCommand.useToolProviderByDefault
 */

public class JavaOptionsTest {
    private static final String PARAM1 = "Some Param 1";
    private static final String PARAM2 = "Some \"Param\" 2";
    private static final String PARAM3 = "Some \"Param\" with \" 3";
    private static final String ARG1 = "-Dparam1=" + "\'" + PARAM1 + "\'";
    private static final String ARG2 = "-Dparam2=" + "\'" + PARAM2 + "\'";
    private static final String ARG3 = "-Dparam3=" + "\'" + PARAM3 + "\'";
    private static final String EXPECT1 = "-Dparam1=" + PARAM1;
    private static final String EXPECT2 = "-Dparam2=" + PARAM2;
    private static final String EXPECT3 = "-Dparam3=" + PARAM3;


    private final JPackageCommand cmd;
    private final String[] expected;

    @Parameters
    public static Collection input() {
        List<Object[]> result = new ArrayList<>();
        for (var app : List.of("Hello", "com.other/com.other.Hello")) {
            result.add(new Object[]{app, new String[]{"--java-options", ARG1},
                new String[]{EXPECT1},});
            result.add(new Object[]{app, new String[]{"--java-options", ARG2},
                new String[]{EXPECT2},});
            result.add(new Object[]{app, new String[]{"--java-options", ARG3},
                new String[]{EXPECT3},});
            result.add(new Object[]{app, new String[]{"--java-options", ARG1,
                "--java-options", ARG2, "--java-options", ARG3}, new String[]{
                EXPECT1, EXPECT2, EXPECT3},});
        }
        return List.of(result.toArray(Object[][]::new));
    }

    public JavaOptionsTest(String javaAppDesc, String[] jpackageArgs,
            String[] expectedParams) {
        cmd = JPackageCommand.helloAppImage(javaAppDesc);
        if (jpackageArgs != null) {
            cmd.addArguments(jpackageArgs);
        }
        expected = expectedParams;
    }

    @Test
    public void test() {
        // 1.) run the jpackage command
        cmd.executeAndAssertImageCreated();

        // 2.) run the launcher it generated
        List<String> output = HelloApp.executeLauncher(cmd).getOutput();
        TKit.assertNotNull(output, "output is null");
        for (String expect : expected) {
            TKit.assertTextStream(expect).apply(output.stream());
        }
    }

}
