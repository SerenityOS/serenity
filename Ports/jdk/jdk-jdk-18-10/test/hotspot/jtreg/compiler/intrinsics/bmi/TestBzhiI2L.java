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

/**
 * @test
 * @key randomness
 * @summary Verify that results of computations are the same w/
 *          and w/o usage of BZHI instruction
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI compiler.intrinsics.bmi.TestBzhiI2L
 */

package compiler.intrinsics.bmi;

import jdk.test.lib.Platform;
import sun.hotspot.cpuinfo.CPUInfo;

public class TestBzhiI2L {

    public static void main(String args[]) throws Throwable {
        if (Platform.isX86()) {
            System.out.println("INFO: Bzhiq not implemented for x86_32, test SKIPPED" );
            return;
        }
        if (!CPUInfo.hasFeature("bmi2")) {
            System.out.println("INFO: CPU does not support bmi2 feature, test SKIPPED" );
            return;
        }

        BMITestRunner.runTests(BzhiI2LExpr.class, args,
                               "-XX:+IgnoreUnrecognizedVMOptions",
                               "-XX:+UseBMI2Instructions");
        BMITestRunner.runTests(BzhiI2LCommutativeExpr.class, args,
                               "-XX:+IgnoreUnrecognizedVMOptions",
                               "-XX:+UseBMI2Instructions");
    }

    public static class BzhiI2LExpr extends Expr.BMIUnaryIntToLongExpr {

        public long intToLongExpr(int src1) {
            int value = src1;
            long returnValue = 0L;

            for(int i = 0; i < 10000; ++i) {
                returnValue =
                (long)(value & 0x1)    ^ (long)(value & 0x3)    ^ (long)(value & 0x7)    ^ (long)(value & 0xF)   ^
                (long)(value & 0x1F)   ^ (long)(value & 0x3F)   ^ (long)(value & 0x7F)   ^ (long)(value & 0xFF)  ^
                (long)(value & 0x1FF)  ^ (long)(value & 0x3FF)  ^ (long)(value & 0x7FF)  ^ (long)(value & 0xFFF) ^
                (long)(value & 0x1FFF) ^ (long)(value & 0x3FFF) ^ (long)(value & 0x7FFF) ^ (long)(value & 0xFFFF);
            }
            return returnValue;
        }
    }

    public static class BzhiI2LCommutativeExpr extends Expr.BMIUnaryIntToLongExpr {

        public long intToLongExpr(int src1) {
            int value = src1;
            long returnValue = 0L;

            for(int i = 0; i < 10000; ++i) {
                returnValue =
                (long)(value & 0x1)    ^ (long)(value & 0x3)    ^ (long)(value & 0x7)    ^ (long)(value & 0xF)   ^
                (long)(value & 0x1F)   ^ (long)(value & 0x3F)   ^ (long)(value & 0x7F)   ^ (long)(value & 0xFF)  ^
                (long)(value & 0x1FF)  ^ (long)(value & 0x3FF)  ^ (long)(value & 0x7FF)  ^ (long)(value & 0xFFF) ^
                (long)(value & 0x1FFF) ^ (long)(value & 0x3FFF) ^ (long)(value & 0x7FFF) ^ (long)(value & 0xFFFF);
            }
            return returnValue;
        }
    }
}
