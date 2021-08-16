/*
 * Copyright (c) 2021, Red Hat, Inc. All rights reserved.
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

/**
 * @test
 * @library /test/lib /
 * @requires vm.flagless
 * @run driver compiler.blackhole.BlackholeExistingIntrinsicWarningTest
 */

package compiler.blackhole;

import java.io.IOException;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class BlackholeExistingIntrinsicWarningTest {

    private static final int CYCLES = 100_000;
    private static final int TRIES = 10;

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            driver();
        } else {
            runner();
        }
    }

    private static final String MSG =
        "Blackhole compile option only works for methods that do not have intrinsic set: java.lang.Thread.onSpinWait()V, _onSpinWait";

    private static List<String> cmdline(String[] args) {
        List<String> r = new ArrayList();
        r.add("-Xmx128m");
        r.add("-Xbatch");
        r.addAll(Arrays.asList(args));
        r.add("compiler.blackhole.BlackholeExistingIntrinsicWarningTest");
        r.add("run");
        return r;
    }

    public static void shouldFail(String... args) throws IOException {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(cmdline(args));
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);
        output.shouldContain(MSG);
    }

    public static void shouldPass(String... args) throws IOException {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(cmdline(args));
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);
        output.shouldNotContain(MSG);
    }

    public static void driver() throws IOException {
        // Should print the warning
        shouldFail(
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:CompileCommand=quiet",
            "-XX:CompileCommand=blackhole,java/lang/Thread.onSpinWait"
        );
        shouldFail(
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:CompileCommand=quiet",
            "-XX:CompileCommand=option,java/lang/Thread.onSpinWait,Blackhole"
        );

        // Should be able to shun the warning
        shouldPass(
            "-XX:-PrintWarnings",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:CompileCommand=quiet",
            "-XX:CompileCommand=blackhole,java/lang/Thread.onSpinWait"
        );
        shouldPass(
            "-XX:-PrintWarnings",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:CompileCommand=quiet",
            "-XX:CompileCommand=option,java/lang/Thread.onSpinWait,Blackhole"
        );
    }

    public static void runner() {
        for (int t = 0; t < TRIES; t++) {
            run();
        }
    }

    public static void run() {
        for (int c = 0; c < CYCLES; c++) {
            Thread.onSpinWait();
        }
    }

}
