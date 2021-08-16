/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.Utils;

/* @test
 * @bug 6354345
 * @summary Check that multiple -agentlib statements in command line fails
 *
 * @library /test/lib
 * @modules java.management
 * @build DoubleAgentTest Exit0
 * @run driver DoubleAgentTest
 */

public class DoubleAgentTest {

    private static final String TEST_CLASSES = System.getProperty(
            "test.classes", ".");

    public static void main(String[] args) throws Throwable {
        String jdwpOption = "-agentlib:jdwp=transport=dt_socket"
                         + ",server=y" + ",suspend=n" + ",address=*:0";

        OutputAnalyzer output = ProcessTools.executeTestJvm("-classpath",
                TEST_CLASSES,
                jdwpOption, // Notice jdwpOption specified twice
                jdwpOption,
                "Exit0");

        output.shouldContain("Cannot load this JVM TI agent twice");
        output.shouldHaveExitValue(1);
    }

}
