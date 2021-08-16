/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8031321
 * @summary Verify that results of computations are the same w/
 *          and w/o usage of ANDN instruction
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI compiler.intrinsics.bmi.TestAndnI
 */

package compiler.intrinsics.bmi;

import sun.hotspot.cpuinfo.CPUInfo;

public class TestAndnI {

    public static void main(String args[]) throws Throwable {
        if (!CPUInfo.hasFeature("bmi1")) {
            System.out.println("INFO: CPU does not support bmi1 feature.");
        }

        BMITestRunner.runTests(AndnIExpr.class, args,
                               "-XX:+IgnoreUnrecognizedVMOptions",
                               "-XX:+UseBMI1Instructions");
        BMITestRunner.runTests(AndnICommutativeExpr.class, args,
                               "-XX:+IgnoreUnrecognizedVMOptions",
                               "-XX:+UseBMI1Instructions");
    }

    public static class AndnIExpr extends Expr.BMIBinaryIntExpr {

        public int intExpr(int src1, int src2) {
            return ~src1 & src2;
        }

        public int intExpr(int src1, Expr.MemI src2) {
            if (src2 != null) {
                return ~src1 & src2.value;
            } else {
                return 0;
            }
        }

        public int intExpr(Expr.MemI src1, int src2) {
            if (src1 != null) {
                return ~src1.value & src2;
            } else {
                return 0;
            }
        }

        public int intExpr(Expr.MemI src1, Expr.MemI src2) {
            if (src1 != null && src2 != null) {
                return ~src1.value & src2.value;
            } else {
                return 0;
            }
        }
    }

    public static class AndnICommutativeExpr extends Expr.BMIBinaryIntExpr {

        public int intExpr(int src1, int src2) {
            return src1 & ~src2;
        }

        public int intExpr(int src1, Expr.MemI src2) {
            if (src2 != null) {
                return src1 & ~src2.value;
            } else {
                return 0;
            }
        }

        public int intExpr(Expr.MemI src1, int src2) {
            if (src1 != null) {
                return src1.value & ~src2;
            } else {
                return 0;
            }
        }

        public int intExpr(Expr.MemI src1, Expr.MemI src2) {
            if (src1 != null && src2 != null) {
                return src1.value & ~src2.value;
            } else {
                return 0;
            }
        }
    }
}
