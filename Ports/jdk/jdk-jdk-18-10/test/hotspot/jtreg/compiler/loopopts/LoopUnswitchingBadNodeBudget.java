/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8223502
 * @summary Node estimate for loop unswitching is not correct:
 *          assert(delta <= 2 * required) failed: Bad node estimate
 *
 * @requires !vm.graal.enabled
 *
 * @run main/othervm -XX:-TieredCompilation -XX:-BackgroundCompilation
 *      -XX:-UseOnStackReplacement -XX:CompileOnly=LoopUnswitchingBadNodeBudget::test
 *      -XX:CompileCommand=dontinline,LoopUnswitchingBadNodeBudget::helper
 *      -XX:+UnlockDiagnosticVMOptions -XX:-UseSwitchProfiling LoopUnswitchingBadNodeBudget
 *
 */

public class LoopUnswitchingBadNodeBudget {

    public static void main(String[] args) {
        for (int i = 0; i < 20_000; i++) {
            for (int j = 0; j < 100; j++) {
                test(j, true, 0, 0, 0);
                test(j, false, 0, 0, 0);
            }
        }
    }

    private static int test(int j, boolean flag, int k, int l, int m) {
        int res = 0;
        for (int i = 0; i < 24; i++) {
            if (flag) {
                k = k / 2;
                l = l * 2;
                m = m + 2;
            }
            switch (j) {
                case  0: break;
                case  1: return helper(j, k, l, m);
                case  2: return helper(j, k, l, m);
                case  3: return helper(j, k, l, m);
                case  4: return helper(j, k, l, m);
                case  5: return helper(j, k, l, m);
                case  6: return helper(j, k, l, m);
                case  7: return helper(j, k, l, m);
                case  8: return helper(j, k, l, m);
                case  9: return helper(j, k, l, m);
                case 10: return helper(j, k, l, m);
                case 11: return helper(j, k, l, m);
                case 12: return helper(j, k, l, m);
                case 13: return helper(j, k, l, m);
                case 14: return helper(j, k, l, m);
                case 15: return helper(j, k, l, m);
                case 16: return helper(j, k, l, m);
                case 17: return helper(j, k, l, m);
                case 18: return helper(j, k, l, m);
                case 19: return helper(j, k, l, m);
                case 20: return helper(j, k, l, m);
                case 21: return helper(j, k, l, m);
                case 22: return helper(j, k, l, m);
                case 23: return helper(j, k, l, m);
                case 24: return helper(j, k, l, m);
                case 25: return helper(j, k, l, m);
                case 26: return helper(j, k, l, m);
                case 27: return helper(j, k, l, m);
                case 28: return helper(j, k, l, m);
                case 29: return helper(j, k, l, m);
                case 30: return helper(j, k, l, m);
                case 31: return helper(j, k, l, m);
                case 32: return helper(j, k, l, m);
                case 33: return helper(j, k, l, m);
                case 34: return helper(j, k, l, m);
                case 35: return helper(j, k, l, m);
                case 36: return helper(j, k, l, m);
                case 37: return helper(j, k, l, m);
                case 38: return helper(j, k, l, m);
                case 39: return helper(j, k, l, m);
                case 40: return helper(j, k, l, m);
                case 41: return helper(j, k, l, m);
                case 42: return helper(j, k, l, m);
                case 43: return helper(j, k, l, m);
                case 44: return helper(j, k, l, m);
                case 45: return helper(j, k, l, m);
                case 46: return helper(j, k, l, m);
                case 47: return helper(j, k, l, m);
                case 48: return helper(j, k, l, m);
                case 49: return helper(j, k, l, m);
                case 50: return helper(j, k, l, m);
                case 51: return helper(j, k, l, m);
                case 52: return helper(j, k, l, m);
                case 53: return helper(j, k, l, m);
                case 54: return helper(j, k, l, m);
                case 55: return helper(j, k, l, m);
                case 56: return helper(j, k, l, m);
                case 57: return helper(j, k, l, m);
                case 58: return helper(j, k, l, m);
                case 59: return helper(j, k, l, m);
                case 60: return helper(j, k, l, m);
                case 61: return helper(j, k, l, m);
                case 62: return helper(j, k, l, m);
                case 63: return helper(j, k, l, m);
                case 64: return helper(j, k, l, m);
                case 65: return helper(j, k, l, m);
                case 66: return helper(j, k, l, m);
                case 67: return helper(j, k, l, m);
                case 68: return helper(j, k, l, m);
                case 69: return helper(j, k, l, m);
                case 70: return helper(j, k, l, m);
                case 71: return helper(j, k, l, m);
                case 72: return helper(j, k, l, m);
                case 73: return helper(j, k, l, m);
                case 74: return helper(j, k, l, m);
                case 75: return helper(j, k, l, m);
                case 76: return helper(j, k, l, m);
                case 77: return helper(j, k, l, m);
                case 78: return helper(j, k, l, m);
                case 79: return helper(j, k, l, m);
                case 80: return helper(j, k, l, m);
                case 81: return helper(j, k, l, m);
                case 82: return helper(j, k, l, m);
                case 83: return helper(j, k, l, m);
                case 84: return helper(j, k, l, m);
                case 85: return helper(j, k, l, m);
                case 86: return helper(j, k, l, m);
                case 87: return helper(j, k, l, m);
                case 88: return helper(j, k, l, m);
                case 89: return helper(j, k, l, m);
                case 90: return helper(j, k, l, m);
                case 91: return helper(j, k, l, m);
                case 92: return helper(j, k, l, m);
                case 93: return helper(j, k, l, m);
                case 94: return helper(j, k, l, m);
                case 95: return helper(j, k, l, m);
                case 96: return helper(j, k, l, m);
                case 97: return helper(j, k, l, m);
                case 98: return helper(j, k, l, m);
                case 99: return helper(j, k, l, m);
            }
            res += helper(j, k, l, m);
        }
        return res;
    }

    private static int helper(int j, int k, int l, int m) {
        return j + k;
    }
}
