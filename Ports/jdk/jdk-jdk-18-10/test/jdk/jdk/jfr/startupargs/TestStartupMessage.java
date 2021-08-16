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

package jdk.jfr.startupargs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main jdk.jfr.startupargs.TestStartupMessage
 */
public class TestStartupMessage {

    public static class TestMessage {
        public static void main(String[] args) throws Exception {
        }
    }

    public static void main(String[] args) throws Exception {
         startJfrJvm("-Xlog:jfr+startup=off")
             .shouldNotContain("[jfr,startup")
             .shouldNotContain("Started recording")
             .shouldNotContain("Use jcmd");

         startJfrJvm("-Xlog:jfr+startup=error")
             .shouldNotContain("[jfr,startup")
             .shouldNotContain("Started recording")
             .shouldNotContain("Use jcmd");

         // Known limitation.
         // Can't turn off log with -Xlog:jfr+startup=warning

         startJfrJvm()
             .shouldContain("[info][jfr,startup")
             .shouldContain("Started recording")
             .shouldContain("Use jcmd");

         startJfrJvm("-Xlog:jfr+startup=info")
             .shouldContain("[info][jfr,startup")
             .shouldContain("Started recording")
             .shouldContain("Use jcmd");
    }

    private static OutputAnalyzer startJfrJvm(String... args) throws Exception {
        List<String> commands = new ArrayList<>(Arrays.asList(args));
        commands.add("-XX:StartFlightRecording");
        commands.add(TestMessage.class.getName());
        ProcessBuilder pb = ProcessTools.createTestJvm(commands);
        OutputAnalyzer out = ProcessTools.executeProcess(pb);
        out.shouldHaveExitValue(0);
        return out;
    }
}
