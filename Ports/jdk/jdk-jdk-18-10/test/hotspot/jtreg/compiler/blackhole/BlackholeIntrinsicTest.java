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
 * @run driver compiler.blackhole.BlackholeIntrinsicTest
 */

package compiler.blackhole;

import java.io.IOException;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import jdk.test.lib.Platform;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class BlackholeIntrinsicTest {

    private static final Map<String, Runnable> TESTS;

    static {
        TESTS = new LinkedHashMap<>();
        TESTS.put("bh_s_boolean_0", BlackholeIntrinsicTest::test_boolean_0);
        TESTS.put("bh_s_byte_0",    BlackholeIntrinsicTest::test_byte_0);
        TESTS.put("bh_s_char_0",    BlackholeIntrinsicTest::test_char_0);
        TESTS.put("bh_s_short_0",   BlackholeIntrinsicTest::test_short_0);
        TESTS.put("bh_s_int_0",     BlackholeIntrinsicTest::test_int_0);
        TESTS.put("bh_s_float_0",   BlackholeIntrinsicTest::test_float_0);
        TESTS.put("bh_s_long_0",    BlackholeIntrinsicTest::test_long_0);
        TESTS.put("bh_s_double_0",  BlackholeIntrinsicTest::test_double_0);
        TESTS.put("bh_s_Object_0",  BlackholeIntrinsicTest::test_Object_0);

        TESTS.put("bh_s_boolean_1", BlackholeIntrinsicTest::test_boolean_1);
        TESTS.put("bh_s_byte_1",    BlackholeIntrinsicTest::test_byte_1);
        TESTS.put("bh_s_char_1",    BlackholeIntrinsicTest::test_char_1);
        TESTS.put("bh_s_short_1",   BlackholeIntrinsicTest::test_short_1);
        TESTS.put("bh_s_int_1",     BlackholeIntrinsicTest::test_int_1);
        TESTS.put("bh_s_float_1",   BlackholeIntrinsicTest::test_float_1);
        TESTS.put("bh_s_long_1",    BlackholeIntrinsicTest::test_long_1);
        TESTS.put("bh_s_double_1",  BlackholeIntrinsicTest::test_double_1);
        TESTS.put("bh_s_Object_1",  BlackholeIntrinsicTest::test_Object_1);

        TESTS.put("bh_s_boolean_2", BlackholeIntrinsicTest::test_boolean_2);
        TESTS.put("bh_s_byte_2",    BlackholeIntrinsicTest::test_byte_2);
        TESTS.put("bh_s_char_2",    BlackholeIntrinsicTest::test_char_2);
        TESTS.put("bh_s_short_2",   BlackholeIntrinsicTest::test_short_2);
        TESTS.put("bh_s_int_2",     BlackholeIntrinsicTest::test_int_2);
        TESTS.put("bh_s_float_2",   BlackholeIntrinsicTest::test_float_2);
        TESTS.put("bh_s_long_2",    BlackholeIntrinsicTest::test_long_2);
        TESTS.put("bh_s_double_2",  BlackholeIntrinsicTest::test_double_2);
        TESTS.put("bh_s_Object_2",  BlackholeIntrinsicTest::test_Object_2);
    }

    private static final int CYCLES = 100_000;
    private static final int TRIES = 10;

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            driver();
        } else {
            test(args[0]);
        }
    }

    public static void driver() throws IOException {
        for (String test : TESTS.keySet()) {
            check(test, "-XX:TieredStopAtLevel=1");
            check(test, "-XX:-TieredCompilation");
            if (Platform.is64bit()) {
                check(test, "-XX:-UseCompressedOops", "-XX:TieredStopAtLevel=1");
                check(test, "-XX:-UseCompressedOops", "-XX:-TieredCompilation");
            }
        }
    }

    private static void test(String test) {
        Runnable r = TESTS.get(test);
        if (r == null) {
           throw new IllegalArgumentException("Cannot find test " + test);
        }
        for (int t = 0; t < TRIES; t++) {
            r.run();
        }
    }

    public static void check(String test, String... args) throws IOException {
        List<String> cmdline = new ArrayList();
        cmdline.add("-Xmx128m");
        cmdline.add("-Xbatch");
        cmdline.add("-XX:+UnlockDiagnosticVMOptions");
        cmdline.add("-XX:+AbortVMOnCompilationFailure");
        cmdline.add("-XX:+PrintCompilation");
        cmdline.add("-XX:+PrintInlining");
        cmdline.add("-XX:+UnlockExperimentalVMOptions");
        cmdline.add("-XX:CompileCommand=blackhole,compiler/blackhole/BlackholeTarget.bh_*");
        cmdline.addAll(Arrays.asList(args));
        cmdline.add("compiler.blackhole.BlackholeIntrinsicTest");
        cmdline.add(test);

        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(cmdline);
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);
        output.stderrShouldBeEmpty();
        output.stdoutShouldMatch("compiler.blackhole.BlackholeTarget::" + test + ".*intrinsic.*");
    }

    private static void test_boolean_0() {
        for (int c = 0; c < CYCLES; c++) {
            BlackholeTarget.bh_s_boolean_0();
        }
    }

    private static void test_byte_0() {
        for (int c = 0; c < CYCLES; c++) {
            BlackholeTarget.bh_s_byte_0();
        }
    }

    private static void test_char_0() {
        for (int c = 0; c < CYCLES; c++) {
            BlackholeTarget.bh_s_char_0();
        }
    }

    private static void test_short_0() {
        for (int c = 0; c < CYCLES; c++) {
            BlackholeTarget.bh_s_short_0();
        }
    }

    private static void test_int_0() {
        for (int c = 0; c < CYCLES; c++) {
            BlackholeTarget.bh_s_int_0();
        }
    }

    private static void test_float_0() {
        for (int c = 0; c < CYCLES; c++) {
            BlackholeTarget.bh_s_float_0();
        }
    }

    private static void test_long_0() {
        for (int c = 0; c < CYCLES; c++) {
            BlackholeTarget.bh_s_long_0();
        }
    }

    private static void test_double_0() {
        for (int c = 0; c < CYCLES; c++) {
            BlackholeTarget.bh_s_double_0();
        }
    }

    private static void test_Object_0() {
        for (int c = 0; c < CYCLES; c++) {
            BlackholeTarget.bh_s_Object_0();
        }
    }

    private static void test_boolean_1() {
        for (int c = 0; c < CYCLES; c++) {
            BlackholeTarget.bh_s_boolean_1((c & 0x1) == 0);
        }
    }

    private static void test_byte_1() {
        for (int c = 0; c < CYCLES; c++) {
            BlackholeTarget.bh_s_byte_1((byte)c);
        }
    }

    private static void test_char_1() {
        for (int c = 0; c < CYCLES; c++) {
            BlackholeTarget.bh_s_char_1((char)c);
        }
    }

    private static void test_short_1() {
        for (int c = 0; c < CYCLES; c++) {
            BlackholeTarget.bh_s_short_1((short)c);
        }
    }

    private static void test_int_1() {
        for (int c = 0; c < CYCLES; c++) {
            BlackholeTarget.bh_s_int_1(c);
        }
    }

    private static void test_float_1() {
        for (int c = 0; c < CYCLES; c++) {
            BlackholeTarget.bh_s_float_1(c);
        }
    }

    private static void test_long_1() {
        for (int c = 0; c < CYCLES; c++) {
            BlackholeTarget.bh_s_long_1(c);
        }
    }

    private static void test_double_1() {
        for (int c = 0; c < CYCLES; c++) {
            BlackholeTarget.bh_s_double_1(c);
        }
    }

    private static void test_Object_1() {
        for (int c = 0; c < CYCLES; c++) {
            Object o = new Object();
            BlackholeTarget.bh_s_Object_1(o);
        }
    }

    private static void test_boolean_2() {
        for (int c = 0; c < CYCLES; c++) {
            BlackholeTarget.bh_s_boolean_2((c & 0x1) == 0, (c & 0x2) == 0);
        }
    }

    private static void test_byte_2() {
        for (int c = 0; c < CYCLES; c++) {
            BlackholeTarget.bh_s_byte_2((byte)c, (byte)(c + 1));
        }
    }

    private static void test_char_2() {
        for (int c = 0; c < CYCLES; c++) {
            BlackholeTarget.bh_s_char_2((char)c, (char)(c + 1));
        }
    }

    private static void test_short_2() {
        for (int c = 0; c < CYCLES; c++) {
            BlackholeTarget.bh_s_short_2((short)c, (short)(c + 1));
        }
    }

    private static void test_int_2() {
        for (int c = 0; c < CYCLES; c++) {
            BlackholeTarget.bh_s_int_2(c, c + 1);
        }
    }

    private static void test_float_2() {
        for (int c = 0; c < CYCLES; c++) {
            BlackholeTarget.bh_s_float_2(c, c + 1);
        }
    }

    private static void test_long_2() {
        for (int c = 0; c < CYCLES; c++) {
            BlackholeTarget.bh_s_long_2(c, c + 1);
        }
    }

    private static void test_double_2() {
        for (int c = 0; c < CYCLES; c++) {
            BlackholeTarget.bh_s_double_2(c, c + 1);
        }
    }

    private static void test_Object_2() {
        for (int c = 0; c < CYCLES; c++) {
            Object o1 = new Object();
            Object o2 = new Object();
            BlackholeTarget.bh_s_Object_2(o1, o2);
        }
    }
}
