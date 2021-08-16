/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @requires vm.compiler2.enabled
 * @bug 8233033 8235984 8240227
 * @summary Tests if partially peeled statements are not executed before the loop predicates by bailing out of loop unswitching.
 *
 * @run main/othervm -Xbatch -XX:LoopStripMiningIter=0
 *      -XX:CompileCommand=compileonly,compiler.loopopts.PartialPeelingUnswitch::test*
 *      -XX:CompileCommand=dontinline,compiler.loopopts.PartialPeelingUnswitch::dontInline*
 *      compiler.loopopts.PartialPeelingUnswitch
 * @run main/othervm -Xbatch -Xcomp -XX:LoopStripMiningIter=0
 *      -XX:CompileCommand=compileonly,compiler.loopopts.PartialPeelingUnswitch::test*
 *      -XX:CompileCommand=dontinline,compiler.loopopts.PartialPeelingUnswitch::dontInline*
 *      compiler.loopopts.PartialPeelingUnswitch
 * @run main/othervm -Xbatch
 *      -XX:CompileCommand=compileonly,compiler.loopopts.PartialPeelingUnswitch::test*
 *      -XX:CompileCommand=dontinline,compiler.loopopts.PartialPeelingUnswitch::dontInline*
 *      compiler.loopopts.PartialPeelingUnswitch
 * @run main/othervm -Xbatch -Xcomp
 *      -XX:CompileCommand=compileonly,compiler.loopopts.PartialPeelingUnswitch::test*
 *      -XX:CompileCommand=dontinline,compiler.loopopts.PartialPeelingUnswitch::dontInline*
 *      compiler.loopopts.PartialPeelingUnswitch
 * @run main/othervm -Xbatch
 *      -XX:CompileCommand=compileonly,compiler.loopopts.PartialPeelingUnswitch::*
 *      -XX:CompileCommand=dontinline,compiler.loopopts.PartialPeelingUnswitch::dontInline*
 *      compiler.loopopts.PartialPeelingUnswitch
 * @run main/othervm -Xbatch -Xcomp
 *      -XX:CompileCommand=compileonly,compiler.loopopts.PartialPeelingUnswitch::*
 *      -XX:CompileCommand=dontinline,compiler.loopopts.PartialPeelingUnswitch::dontInline*
 *      compiler.loopopts.PartialPeelingUnswitch
 */

package compiler.loopopts;

public class PartialPeelingUnswitch {

    public static int iFld;
    public static int w = 88;
    public static int x = 42;
    public static int y = 31;
    public static int z = 22;
    public static int val = 34;
    public static final int iCon = 20;

    public static int[] iArr = new int[10];

    public int test() {
        /*
         * The inner loop of this test is first partially peeled and then unswitched. An uncommon trap is hit in one
         * of the cloned loop predicates for the fast loop (set up at unswitching stage). The only partially peeled
         * statement "iFld += 7" was wrongly executed before the predicates (and before the loop itself).
         * When hitting the uncommon trap, "iFld >>= 1" was not yet executed. As a result, the interpreter directly
         * reexecuted "iFld += 7" again. This resulted in a wrong result for "iFld". The fix in 8233033 makes peeled
         * statements control dependant on the cloned loop predicates such that they are executed after them. However,
         * some cases are not handled properly. For now, the new fix in 8235984 just bails out of loop unswitching.
         */
        iFld = 13;
        for (int i = 0; i < 8; i++) {
            int j = 10;
            while (--j > 0) {
                iFld += -7;
                switch ((i * 5) + 102) {
                case 120:
                    break;
                case 103:
                    break;
                case 116:
                    break;
                default:
                    iFld >>= 1;
                }
            }
        }
        return iFld;
    }

    public int test2() {
        /*
         * Same nested loop structure as in test() but with more statements that are partially peeled from the inner loop.
         * Afterwards the inner loop is unswitched.
         */
        iFld = 13;
        int k = 0;
        for (int i = 0; i < 8; i++) {
            int j = 10;
            while (--j > 0) {
                // All statements before the switch expression are partially peeled
                iFld += -7;
                x = y + iFld;
                y = iArr[5];
                k = 6;
                iArr[5] = 5;
                iArr[6] += 23;
                iArr[7] = iArr[8] + iArr[6];
                iArr[j] = 34;
                switch ((i * 5) + 102) {
                case 120:
                    break;
                case 103:
                    break;
                case 116:
                    break;
                default:
                    iFld >>= 1;
                }
            }
        }
        return iFld + k;
    }

    public int test3() {
        iFld = 13;
        if (z < 34) {
            z = 34;
        }

        for (int i = 0; i < 8; i++) {
            int j = 10;
            while (--j > 0) {
                iFld += -7;
                iArr[5] = 8;
                x = iArr[6];
                y = x;
                for (int k = 50; k < 51; k++) {
                    x = iArr[7];
                }
                switch ((i * 5) + 102) {
                case 120:
                    return iFld;
                case 103:
                    break;
                case 116:
                    break;
                default:
                    if (iFld == -7) {
                        return iFld;
                    }
                    z = iArr[5];
                    iFld >>= 1;
                }
            }
            iArr[5] = 34;
            dontInline(iArr[5]);
        }
        return iFld;
    }

    public int test4() {
        iFld = 13;
        if (z < 34) {
            z = 34;
        }

        for (int i = 0; i < 8; i++) {
            int j = 10;
            while (--j > 0) {
                iFld += -7;
                iArr[5] = 8;
                x = iArr[6];
                y = x;
                for (int k = 50; k < 51; k++) {
                    x = iArr[7];
                }
                switch ((i * 5) + 102) {
                case 120:
                    return iFld;
                case 103:
                    break;
                case 116:
                    break;
                default:
                    if (iFld == -7) {
                        return iFld;
                    }
                    z = iArr[5];
                    iFld >>= 1;
                }
            }
            iArr[5] = 34;
        }
        return iFld;
    }

    public int test5() {
        iFld = 13;
        for (int i = 0; i < 8; i++) {
            int j = 10;
            while (--j > 0) {
                iFld += -7;
                iArr[5] = 8;
                x = iArr[6];
                y = x;
                for (int k = 50; k < 51; k++) {
                    x = iArr[7];
                }
                switch ((i * 5) + 102) {
                case 120:
                    return iFld;
                case 103:
                    break;
                case 116:
                    break;
                default:
                    iFld >>= 1;
                }
            }
        }
        return iFld;
    }

    public int test6() {
        iFld = 13;
        for (int i = 0; i < 8; i++) {
            int j = 10;
            while (--j > 0) {
                iFld += -7;
                iArr[5] = 8;
                x = iArr[6];
                y = x;
                switch ((i * 5) + 102) {
                case 120:
                    return iFld;
                case 103:
                    break;
                case 116:
                    break;
                default:
                    iFld >>= 1;
                }
            }
        }
        return iFld;
    }

    public int test7() {
        iFld = 13;
        for (int i = 0; i < 8; i++) {
            int j = 10;
            while (--j > 0) {
                iFld += -7;
                iArr[5] = 8;
                switch ((i * 5) + 102) {
                case 120:
                    return iFld;
                case 103:
                    break;
                case 116:
                    break;
                default:
                    iFld >>= 1;
                }
            }
        }
        return iFld;
    }

    public int test8() {

        iFld = 13;
        for (int i = 0; i < 8; i++) {
            int j = 50;
            while (--j > 0) {
                // All statements before the switch expression are partially peeled
                iFld += -7;
                x = y + iFld;
                y = iArr[5];
                iArr[5] = 5;
                iArr[6] += 23;
                iArr[7] = iArr[8] + iArr[6];
                switch ((val * 5) + 102) {
                case 120:
                    break;
                case 103:
                    break;
                case 116:
                    break;
                default:
                    iFld >>= 1;
                }
            }
        }
        return iFld;
    }


    public int test9() {
        iFld = 13;
        for (int i = 0; i < 8; i++) {
            int j = 10;
            while (--j > 0) {
                iFld += -7;
                iArr[4] = 8;
                x = iArr[5];
                switch ((i * 5) + 102) {
                case 120:
                    return iFld + 1;
                case 103:
                    break;
                case 116:
                    break;
                default:
                    iFld >>= 1;
                }
            }
        }

        return iFld;
    }

   public int test10() {
        if (z < 34) {
            z = 34;
        }

        iFld = 13;
        for (int i = 0; i < 80; i++) {
            int j = 50;
            while (--j > 0) {
                iFld += -7;
                iArr[4] = 8;
                x = iArr[5];
                switch ((i * 5) + 102) {
                case 120:
                    break;
                case 103:
                    break;
                case 116:
                    break;
                default:
                    iFld >>= 1;
                }
            }
        }
        if (z == 34) {
            x = iArr[6];
        }
        return iFld;
    }


    public int test11Xcomp() {
        if (z < 34) {
            z = 34;
        }

        iFld = 13;
        for (int i = 0; i < 80; i++) {
            int j = 50;
            while (--j > 0) {
                iFld += -7;
                iArr[4] = 8;
                x = iArr[5];
                switch ((i * 5) + 102) {
                case 120:
                    break;
                case 103:
                    break;
                case 116:
                    break;
                default:
                    iFld >>= 1;
                }
                if (z == 34) {
                    break;
                }
            }
        }
        if (z == 34) {
            x = iArr[6];
        }
        return iFld;
    }

    // Phi with multiple inputs from same peeled node
    public int test12Xcomp() {
        if (z < 34) {
            z = 34;
        }

        iFld = 13;
        for (int i = 0; i < 80; i++) {
            int j = 50;
            while (--j > 0) {
                iFld += -7;
                iArr[4] = 8;
                x = iArr[5];
                switch ((i * 5) + 102) {
                case 120:
                    break;
                case 103:
                    break;
                case 116:
                    return z;
                case 106:
                    return y;
                case 111:
                    return x;
                default:
                    iFld >>= 1;
                }
                w = 45;
            }

        }
        if (z == 34) {
            x = iArr[6];
        }
        return iFld;
    }

    public int test13Xcomp() {
        if (z < 34) {
            z = 34;
        }

        iFld = 13;
        for (int i = 0; i < 80; i++) {
            int j = 50;
            while (--j > 0) {
                iFld += -7;
                iArr[4] = 8;
                x = iArr[5];
                switch ((i * 5) + 102) {
                case 120:
                    break;
                case 103:
                    break;
                case 116:
                    break;
                default:
                    iFld >>= 1;
                }
                w = 45;
            }

        }
        if (z == 34) {
            x = iArr[6];
        }
        return iFld;
    }

    // Triggers after peeling with Xcomp
    public int test14Peel() {
        iFld = 13;
        for (int i = 0; i < 8; i++) {
            int j = 10;
            while (--j > 0) {
                iFld += -7;
                iArr[4] = 8;
                x = iArr[5];
                switch ((i * 5) + 102) {
                case 120:
                    return iFld;
                case 103:
                    break;
                case 116:
                    break;
                default:
                    iFld >>= 1;
                }
                iFld = 3;
            }
        }
        y = iArr[4];
        x = iArr[6];

        return iFld;
    }


    public int test15earlyCtrl() {
        iFld = 13;
        if (z < 34) {
            z = 34;
        }

        for (int i = 0; i < 8; i++) {
            int j = 10;
            while (--j > 0) {
                iFld += -7;
                iArr[5] = 8;
                x = iArr[6];
                y = x;
                x = iArr[7];
                switch ((i * 5) + 102) {
                case 120:
                    return iFld;
                case 103:
                    break;
                case 116:
                    break;
                default:
                    if (iFld == -7) {
                        return iFld;
                    }
                    z = iArr[5];
                    iFld >>= 1;
                }
            }
            if (iFld == 7) {
                iArr[3] = 3;
            }
            dontInline(7);
            iArr[5] = 34;
        }
        return iFld;
    }

    // Load after loop -> LoadI after loop from peeled StoreI
    public int test16() {
        iFld = 13;
        if (z < 34) {
            z = 34;
        }

        for (int i = 0; i < 8; i++) {
            int j = 60;
            while (--j > 0) {
                iFld += -7;
                y += iFld + 1;

                iArr[5] = 8;
                x = iArr[6];
                x = iArr[7];
                switch ((i * 5) + 102) {
                case 120:
                    return iFld;
                case 103:
                    break;
                case 116:
                    break;
                default:
                    if (iFld == -7) {
                        return iFld;
                    }
                    z = iArr[5];
                    iFld >>= 1;
                }
            }
            w = iArr[9];
            if (iFld == 7) {
                iArr[3] = 3;
            }
            dontInline(7);
            iArr[5] = 34;
        }
        return iFld;
    }

    // Region 13 before return, which region to choose for MergeMem?
    public int test17Xcomp() {
        A p = dontInlineGetA();
        if (z < 34) {
            z = 34;
        }

        iFld = 13;
        for (int i = 0; i < 80; i++) {
            int j = 50;
            while (--j > 0) {
                iFld += -7;
                iArr[4] = 8;
                x = iArr[5];
                y = p.i;
                switch ((i * 5) + 102) {
                case 120:
                    break;
                case 103:
                    break;
                case 116:
                    return z;
                case 106:
                    return y;
                case 111:
                    return x;
                default:
                    iFld >>= 1;
                }
                w = 45;
            }

        }
        if (z == 34) {
            x = iArr[6];
        }
        return iFld;
    }

    // Region 13 before return, which region to choose for MergeMem?
    public int test18Xcomp() {
        if (z < 34) {
            z = 34;
        }

        iFld = 13;
        for (int i = 0; i < 80; i++) {
            int j = 50;
            while (--j > 0) {
                iFld += -7;
                iArr[4] = 8;
                x = iArr[5];
                y = 85;
                switch ((i * 5) + 102) {
                case 120:
                    break;
                case 103:
                    break;
                case 116:
                    return z;
                case 106:
                    if (z == 34) {
                        x = iArr[7];
                    }
                    return y;
                case 111:
                    return x;
                default:
                    iFld >>= 1;
                }
                w = 45;
            }

        }

        if (z == 34) {
            x = iArr[6];
        }
        return iFld;
    }

    public int test19Xcomp() {
        if (z < 34) {
            z = 34;
        }

        iFld = 13;
        for (int i = 0; i < 80; i++) {
            int j = 50;
            while (--j > 0) {
                iFld += -7;
                iArr[4] = 8;
                x = iArr[5]+ iArr[6];
                y = 85;
                switch ((i * 5) + 102) {
                case 120:
                    break;
                case 103:
                    break;
                case 116:
                    break;
                case 106:
                    if (z == 34) {
                        x = iArr[7];
                    }
                    return y;
                case 111:
                    return x;
                default:
                    iFld >>= 1;
                }
                w = 45;
            }
        }

        if (z == 34) {
            iArr[7] = 34;
        }
        return iFld;
    }

    public int test20() {
        if (z < 34) {
            z = 34;
        }

        iFld = 13;
        for (int i = 0; i < 80; i++) {
            int j = 50;
            while (--j > 0) {
                iFld += -7;
                iArr[4] = 8;
                x = iArr[5];
                switch ((i * 5) + 102) {
                case 120:
                    break;
                case 103:
                    break;
                case 116:
                    break;
                default:
                    iFld >>= 1;
                }
            }
            x = iArr[6];
        }
        if (z == 34) {
            x = iArr[7];
        }
        return iFld;
    }


    public int test21() {
        if (z < 34) {
            z = 34;
        }

        iFld = 13;
        for (int i = 0; i < 80; i++) {
            int j = 50;
            while (--j > 0) {
                iFld += -7;
                iArr[4] = 8;
                x = iArr[5];
                switch ((i * 5) + 102) {
                case 120:
                    break;
                case 103:
                    break;
                case 116:
                    break;
                default:
                    iFld >>= 1;
                }
            }
            x = iArr[6];
        }
        if (z == 34) {
            y = iArr[7];
        }
        return iFld;
    }

    public int testNoOuter() {
        iFld = 13;
        int j = 10;
        while (--j > 0) {
            iFld += -7;
            switch ((iCon * 5) + 102) {
            case 120:
                break;
            case 103:
                break;
            case 116:
                break;
            default:
                iFld >>= 1;
            }
        }
        return iFld;
    }

    public int test2NoOuter() {
        /*
         * Same nested loop structure as in test() but with more statements that are partially peeled from the inner loop.
         * Afterwards the inner loop is unswitched.
         */
        iFld = 13;
        int k = 0;
        int j = 10;
        while (--j > 0) {
            // All statements before the switch expression are partially peeled
            iFld += -7;
            x = y + iFld;
            y = iArr[5];
            k = 6;
            iArr[5] = 5;
            iArr[6] += 23;
            iArr[7] = iArr[8] + iArr[6];
            iArr[j] = 34;
            switch ((iCon * 5) + 102) {
            case 120:
                break;
            case 103:
                break;
            case 116:
                break;
            default:
                iFld >>= 1;
            }
        }
        return iFld + k;
    }

    public int test3NoOuter() {
        iFld = 13;
        if (z < 34) {
            z = 34;
        }

        int j = 10;
        while (--j > 0) {
            iFld += -7;
            iArr[5] = 8;
            x = iArr[6];
            y = x;
            for (int k = 50; k < 51; k++) {
                x = iArr[7];
            }
            switch ((iCon * 5) + 102) {
            case 120:
                return iFld;
            case 103:
                break;
            case 116:
                break;
            default:
                if (iFld == -7) {
                    return iFld;
                }
                z = iArr[5];
                iFld >>= 1;
            }
        }
        iArr[5] = 34;
        dontInline(iArr[5]);
        return iFld;
    }

    public int test4NoOuter() {
        iFld = 13;
        if (z < 34) {
            z = 34;
        }

        int j = 10;
        while (--j > 0) {
            iFld += -7;
            iArr[5] = 8;
            x = iArr[6];
            y = x;
            for (int k = 50; k < 51; k++) {
                x = iArr[7];
            }
            switch ((iCon * 5) + 102) {
            case 120:
                return iFld;
            case 103:
                break;
            case 116:
                break;
            default:
                if (iFld == -7) {
                    return iFld;
                }
                z = iArr[5];
                iFld >>= 1;
            }
        }
        iArr[5] = 34;
        return iFld;
    }

    public int test5NoOuter() {
        iFld = 13;
        int j = 10;
        while (--j > 0) {
            iFld += -7;
            iArr[5] = 8;
            x = iArr[6];
            y = x;
            for (int k = 50; k < 51; k++) {
                x = iArr[7];
            }
            switch ((iCon * 5) + 102) {
            case 120:
                return iFld;
            case 103:
                break;
            case 116:
                break;
            default:
                iFld >>= 1;
            }
        }
        return iFld;
    }

    public int test6NoOuter() {
        iFld = 13;
        int j = 10;
        while (--j > 0) {
            iFld += -7;
            iArr[5] = 8;
            x = iArr[6];
            y = x;
            switch ((iCon * 5) + 102) {
            case 120:
                return iFld;
            case 103:
                break;
            case 116:
                break;
            default:
                iFld >>= 1;
            }
        }
        return iFld;
    }

    public int test7NoOuter() {
        iFld = 13;
        int j = 10;
        while (--j > 0) {
            iFld += -7;
            iArr[5] = 8;
            switch ((iCon * 5) + 102) {
            case 120:
                return iFld;
            case 103:
                break;
            case 116:
                break;
            default:
                iFld >>= 1;
            }
        }
        return iFld;
    }

    public int test8NoOuter() {

        iFld = 13;
        int j = 50;
        while (--j > 0) {
            // All statements before the switch expression are partially peeled
            iFld += -7;
            x = y + iFld;
            y = iArr[5];
            iArr[5] = 5;
            iArr[6] += 23;
            iArr[7] = iArr[8] + iArr[6];
            switch ((val * 5) + 102) {
            case 120:
                break;
            case 103:
                break;
            case 116:
                break;
            default:
                iFld >>= 1;
            }
        }
        return iFld;
    }


    public int test9NoOuter() {
        iFld = 13;
        int j = 10;
        while (--j > 0) {
            iFld += -7;
            iArr[4] = 8;
            x = iArr[5];
            switch ((iCon * 5) + 102) {
            case 120:
                return iFld + 1;
            case 103:
                break;
            case 116:
                break;
            default:
                iFld >>= 1;
            }
        }
        return iFld;
    }

   public int test10NoOuter() {
        if (z < 34) {
            z = 34;
        }

        iFld = 13;
        int j = 50;
        while (--j > 0) {
            iFld += -7;
            iArr[4] = 8;
            x = iArr[5];
            switch ((iCon * 5) + 102) {
            case 120:
                break;
            case 103:
                break;
            case 116:
                break;
            default:
                iFld >>= 1;
            }
        }
        if (z == 34) {
            x = iArr[6];
        }
        return iFld;
    }


    public int test11XcompNoOuter() {
        if (z < 34) {
            z = 34;
        }

        iFld = 13;
        int j = 50;
        while (--j > 0) {
            iFld += -7;
            iArr[4] = 8;
            x = iArr[5];
            switch ((iCon * 5) + 102) {
            case 120:
                break;
            case 103:
                break;
            case 116:
                break;
            default:
                iFld >>= 1;
            }
            if (z == 34) {
                break;
            }
        }
        if (z == 34) {
            x = iArr[6];
        }
        return iFld;
    }

    // Phi with multiple inputs from same peeled node
    public int test12XcompNoOuter() {
        if (z < 34) {
            z = 34;
        }

        iFld = 13;
        int j = 50;
        while (--j > 0) {
            iFld += -7;
            iArr[4] = 8;
            x = iArr[5];
            switch ((iCon * 5) + 102) {
            case 120:
                break;
            case 103:
                break;
            case 116:
                return z;
            case 106:
                return y;
            case 111:
                return x;
            default:
                iFld >>= 1;
            }
            w = 45;
        }

        if (z == 34) {
            x = iArr[6];
        }
        return iFld;
    }

    public int test13XcompNoOuter() {
        if (z < 34) {
            z = 34;
        }

        iFld = 13;
        int j = 50;
        while (--j > 0) {
            iFld += -7;
            iArr[4] = 8;
            x = iArr[5];
            switch ((iCon * 5) + 102) {
            case 120:
                break;
            case 103:
                break;
            case 116:
                break;
            default:
                iFld >>= 1;
            }
            w = 45;
        }

        if (z == 34) {
            x = iArr[6];
        }
        return iFld;
    }

    // Triggers after peeling with Xcomp
    public int test14PeelNoOuter() {
        iFld = 13;
        int j = 10;
        while (--j > 0) {
            iFld += -7;
            iArr[4] = 8;
            x = iArr[5];
            switch ((iCon * 5) + 102) {
            case 120:
                return iFld;
            case 103:
                break;
            case 116:
                break;
            default:
                iFld >>= 1;
            }
            iFld = 3;
        }
        y = iArr[4];
        x = iArr[6];

        return iFld;
    }


    public int test15earlyCtrlNoOuter() {
        iFld = 13;
        if (z < 34) {
            z = 34;
        }

        int j = 10;
        while (--j > 0) {
            iFld += -7;
            iArr[5] = 8;
            x = iArr[6];
            y = x;
            x = iArr[7];
            switch ((iCon * 5) + 102) {
            case 120:
                return iFld;
            case 103:
                break;
            case 116:
                break;
            default:
                if (iFld == -7) {
                    return iFld;
                }
                z = iArr[5];
                iFld >>= 1;
            }
        }
        if (iFld == 7) {
            iArr[3] = 3;
        }
        dontInline(7);
        iArr[5] = 34;
        return iFld;
    }

    // Load after loop -> LoadI after loop from peeled StoreI
    public int test16NoOuter() {
        iFld = 13;
        if (z < 34) {
            z = 34;
        }

        int j = 60;
        while (--j > 0) {
            iFld += -7;
            y += iFld + 1;

            iArr[5] = 8;
            x = iArr[6];
            x = iArr[7];
            switch ((iCon * 5) + 102) {
            case 120:
                return iFld;
            case 103:
                break;
            case 116:
                break;
            default:
                if (iFld == -7) {
                    return iFld;
                }
                z = iArr[5];
                iFld >>= 1;
            }
        }
        w = iArr[9];
        if (iFld == 7) {
            iArr[3] = 3;
        }
        dontInline(7);
        iArr[5] = 34;
        return iFld;
    }

    // Region 13 before return, which region to choose for MergeMem?
    public int test17XcompNoOuter() {
        A p = dontInlineGetA();
        if (z < 34) {
            z = 34;
        }

        iFld = 13;
        int j = 50;
        while (--j > 0) {
            iFld += -7;
            iArr[4] = 8;
            x = iArr[5];
            y = p.i;
            switch ((iCon * 5) + 102) {
            case 120:
                break;
            case 103:
                break;
            case 116:
                return z;
            case 106:
                return y;
            case 111:
                return x;
            default:
                iFld >>= 1;
            }
            w = 45;
        }
        if (z == 34) {
            x = iArr[6];
        }
        return iFld;
    }

    // Region 13 before return, which region to choose for MergeMem?
    public int test18XcompNoOuter() {
        if (z < 34) {
            z = 34;
        }

        iFld = 13;
        int j = 50;
        while (--j > 0) {
            iFld += -7;
            iArr[4] = 8;
            x = iArr[5];
            y = 85;
            switch ((iCon * 5) + 102) {
            case 120:
                break;
            case 103:
                break;
            case 116:
                return z;
            case 106:
                if (z == 34) {
                    x = iArr[7];
                }
                return y;
            case 111:
                return x;
            default:
                iFld >>= 1;
            }
            w = 45;
        }

        if (z == 34) {
            x = iArr[6];
        }
        return iFld;
    }

    public int test19XcompNoOuter() {
        if (z < 34) {
            z = 34;
        }

        iFld = 13;
        int j = 50;
        while (--j > 0) {
            iFld += -7;
            iArr[4] = 8;
            x = iArr[5]+ iArr[6];
            y = 85;
            switch ((iCon * 5) + 102) {
            case 120:
                break;
            case 103:
                break;
            case 116:
                break;
            case 106:
                if (z == 34) {
                    x = iArr[7];
                }
                return y;
            case 111:
                return x;
            default:
                iFld >>= 1;
            }
            w = 45;
        }

        if (z == 34) {
            iArr[7] = 34;
        }
        return iFld;
    }


    public int test20NoOuter() {
        if (z < 34) {
            z = 34;
        }

        iFld = 13;
        int j = 50;
        while (--j > 0) {
            iFld += -7;
            iArr[4] = 8;
            x = iArr[5];
            switch ((iCon * 5) + 102) {
            case 120:
                break;
            case 103:
                break;
            case 116:
                break;
            default:
                iFld >>= 1;
            }
        }
        x = iArr[6];
        if (z == 34) {
            x = iArr[7];
        }
        return iFld;
    }

    public int test21NoOuter() {
        if (z < 34) {
            z = 34;
        }

        iFld = 13;
        int j = 50;
        while (--j > 0) {
            iFld += -7;
            iArr[4] = 8;
            x = iArr[5];
            switch ((iCon * 5) + 102) {
            case 120:
                break;
            case 103:
                break;
            case 116:
                break;
            default:
                iFld >>= 1;
            }
        }
        x = iArr[6];
        if (z == 34) {
            y = iArr[7];
        }
        return iFld;
    }
    public static void main(String[] strArr) {
        BookKeeper[] bookKeeper = new BookKeeper[22];
        for (int i = 0; i < 22; i++) {
            bookKeeper[i] = new BookKeeper();
        }

        PartialPeelingUnswitch _instance = new PartialPeelingUnswitch();
        for (int i = 0; i < 2000; i++) {
            int result = _instance.test();
            if (result != -7) {
                throw new RuntimeException("Result should always be -7 but was " + result);
            }
        }

        for (int i = 0; i < 2000; i++) {
            int result = _instance.test2();
            check(-1, result);
            check(-7, iFld);
            check(-9, x);
            check(5, y);
            check(5, iArr[5]);
            check(149, iArr[6]);
            check(183, iArr[7]);

            // Reset fields
            for (int j = 0; j < 10; j++) {
                iArr[j] = 0;
            }
            x = 42;
            y = 31;
        }

        for (int i = 0; i < 2000; i++) {
            BookKeeper.setup();
            _instance.test3();
            bookKeeper[3].compare();
            BookKeeper.setup();
            _instance.test4();
            bookKeeper[4].compare();
            BookKeeper.setup();
            _instance.test5();
            bookKeeper[5].compare();
            BookKeeper.setup();
            _instance.test6();
            bookKeeper[6].compare();
            BookKeeper.setup();
            _instance.test7();
            bookKeeper[7].compare();
            BookKeeper.setup();
            _instance.test8();
            bookKeeper[8].compare();
            BookKeeper.setup();
            _instance.test9();
            bookKeeper[9].compare();
            BookKeeper.setup();
            _instance.test10();
            bookKeeper[10].compare();
            BookKeeper.setup();
            _instance.test11Xcomp();
            bookKeeper[11].compare();
            BookKeeper.setup();
            _instance.test12Xcomp();
            bookKeeper[12].compare();
            BookKeeper.setup();
            _instance.test13Xcomp();
            bookKeeper[13].compare();
            BookKeeper.setup();
            _instance.test14Peel();
            bookKeeper[14].compare();
            BookKeeper.setup();
            _instance.test15earlyCtrl();
            bookKeeper[15].compare();
            BookKeeper.setup();
            _instance.test16();
            bookKeeper[16].compare();
            BookKeeper.setup();
            _instance.test17Xcomp();
            bookKeeper[17].compare();
            BookKeeper.setup();
            _instance.test18Xcomp();
            bookKeeper[18].compare();
            BookKeeper.setup();
            _instance.test19Xcomp();
            bookKeeper[19].compare();
            BookKeeper.setup();
            _instance.test20();
            bookKeeper[20].compare();
            BookKeeper.setup();
            _instance.test21();
            bookKeeper[21].compare();
        }

        for (int i = 0; i < 22; i++) {
            bookKeeper[i] = new BookKeeper();
        }

        for (int i = 0; i < 2000; i++) {
            BookKeeper.setup();
            _instance.testNoOuter();
            bookKeeper[1].compare();
            BookKeeper.setup();
            _instance.test2NoOuter();
            bookKeeper[2].compare();
            BookKeeper.setup();
            _instance.test3NoOuter();
            bookKeeper[3].compare();
            BookKeeper.setup();
            _instance.test4NoOuter();
            bookKeeper[4].compare();
            BookKeeper.setup();
            _instance.test5NoOuter();
            bookKeeper[5].compare();
            BookKeeper.setup();
            _instance.test6NoOuter();
            bookKeeper[6].compare();
            BookKeeper.setup();
            _instance.test7NoOuter();
            bookKeeper[7].compare();
            BookKeeper.setup();
            _instance.test8NoOuter();
            bookKeeper[8].compare();
            BookKeeper.setup();
            _instance.test9NoOuter();
            bookKeeper[9].compare();
            BookKeeper.setup();
            _instance.test10NoOuter();
            bookKeeper[10].compare();
            BookKeeper.setup();
            _instance.test11XcompNoOuter();
            bookKeeper[11].compare();
            BookKeeper.setup();
            _instance.test12XcompNoOuter();
            bookKeeper[12].compare();
            BookKeeper.setup();
            _instance.test13XcompNoOuter();
            bookKeeper[13].compare();
            BookKeeper.setup();
            _instance.test14PeelNoOuter();
            bookKeeper[14].compare();
            BookKeeper.setup();
            _instance.test15earlyCtrlNoOuter();
            bookKeeper[15].compare();
            BookKeeper.setup();
            _instance.test16NoOuter();
            bookKeeper[16].compare();
            BookKeeper.setup();
            _instance.test17XcompNoOuter();
            bookKeeper[17].compare();
            BookKeeper.setup();
            _instance.test18XcompNoOuter();
            bookKeeper[18].compare();
            BookKeeper.setup();
            _instance.test19XcompNoOuter();
            bookKeeper[19].compare();
            BookKeeper.setup();
            _instance.test20NoOuter();
            bookKeeper[20].compare();
            BookKeeper.setup();
            _instance.test21NoOuter();
            bookKeeper[21].compare();
        }

        for (int i = 0; i < 22; i++) {
            bookKeeper[i] = new BookKeeper();
        }

        for (int i = 0; i < 2000; i++) {
            BookKeeper.setup();
            setZ(i);
            _instance.test3();
            bookKeeper[3].compare(i);
            BookKeeper.setup();
            setZ(i);
            _instance.test4();
            bookKeeper[4].compare(i);
            BookKeeper.setup();
            setZ(i);
            _instance.test5();
            bookKeeper[5].compare(i);
            BookKeeper.setup();
            setZ(i);
            _instance.test6();
            bookKeeper[6].compare(i);
            BookKeeper.setup();
            setZ(i);
            _instance.test7();
            bookKeeper[7].compare(i);
            BookKeeper.setup();
            setZ(i);
            _instance.test8();
            bookKeeper[8].compare(i);
            BookKeeper.setup();
            setZ(i);
            _instance.test9();
            bookKeeper[9].compare(i);
            BookKeeper.setup();
            setZ(i);
            _instance.test10();
            bookKeeper[10].compare(i);
            setZ(i);
            BookKeeper.setup();
            _instance.test11Xcomp();
            bookKeeper[11].compare(i);
            setZ(i);
            BookKeeper.setup();
            _instance.test12Xcomp();
            bookKeeper[12].compare(i);
            setZ(i);
            BookKeeper.setup();
            _instance.test13Xcomp();
            bookKeeper[13].compare(i);
            setZ(i);
            BookKeeper.setup();
            _instance.test14Peel();
            bookKeeper[14].compare(i);
            setZ(i);
            BookKeeper.setup();
            _instance.test15earlyCtrl();
            bookKeeper[15].compare(i);
            setZ(i);
            BookKeeper.setup();
            _instance.test16();
            bookKeeper[16].compare(i);
            setZ(i);
            BookKeeper.setup();
            _instance.test17Xcomp();
            bookKeeper[17].compare(i);
            setZ(i);
            BookKeeper.setup();
            _instance.test18Xcomp();
            bookKeeper[18].compare(i);
            setZ(i);
            BookKeeper.setup();
            _instance.test19Xcomp();
            bookKeeper[19].compare(i);
            setZ(i);
            BookKeeper.setup();
            _instance.test20();
            bookKeeper[20].compare(i);
            setZ(i);
            BookKeeper.setup();
            _instance.test21();
            bookKeeper[21].compare(i);
        }

        for (int i = 0; i < 22; i++) {
            bookKeeper[i] = new BookKeeper();
        }

        for (int i = 0; i < 2000; i++) {
            BookKeeper.setup();
            setZ(i);
            _instance.testNoOuter();
            bookKeeper[1].compare(i);
            BookKeeper.setup();
            setZ(i);
            _instance.test2NoOuter();
            bookKeeper[2].compare(i);
            BookKeeper.setup();
            setZ(i);
            _instance.test3NoOuter();
            bookKeeper[3].compare(i);
            BookKeeper.setup();
            setZ(i);
            _instance.test4NoOuter();
            bookKeeper[4].compare(i);
            BookKeeper.setup();
            setZ(i);
            _instance.test5NoOuter();
            bookKeeper[5].compare(i);
            BookKeeper.setup();
            setZ(i);
            _instance.test6NoOuter();
            bookKeeper[6].compare(i);
            BookKeeper.setup();
            setZ(i);
            _instance.test7NoOuter();
            bookKeeper[7].compare(i);
            BookKeeper.setup();
            setZ(i);
            _instance.test8NoOuter();
            bookKeeper[8].compare(i);
            BookKeeper.setup();
            setZ(i);
            _instance.test9NoOuter();
            bookKeeper[9].compare(i);
            BookKeeper.setup();
            setZ(i);
            _instance.test10NoOuter();
            bookKeeper[10].compare(i);
            BookKeeper.setup();
            setZ(i);
            _instance.test11XcompNoOuter();
            bookKeeper[11].compare(i);
            BookKeeper.setup();
            setZ(i);
            _instance.test12XcompNoOuter();
            bookKeeper[12].compare(i);
            BookKeeper.setup();
            setZ(i);
            _instance.test13XcompNoOuter();
            bookKeeper[13].compare(i);
            BookKeeper.setup();
            setZ(i);
            _instance.test14PeelNoOuter();
            bookKeeper[14].compare(i);
            BookKeeper.setup();
            setZ(i);
            _instance.test15earlyCtrlNoOuter();
            bookKeeper[15].compare(i);
            BookKeeper.setup();
            setZ(i);
            _instance.test16NoOuter();
            bookKeeper[16].compare(i);
            BookKeeper.setup();
            setZ(i);
            _instance.test17XcompNoOuter();
            bookKeeper[17].compare(i);
            BookKeeper.setup();
            setZ(i);
            _instance.test18XcompNoOuter();
            bookKeeper[18].compare(i);
            BookKeeper.setup();
            setZ(i);
            _instance.test19XcompNoOuter();
            bookKeeper[19].compare(i);
            BookKeeper.setup();
            setZ(i);
            _instance.test20NoOuter();
            bookKeeper[20].compare(i);
            BookKeeper.setup();
            setZ(i);
            _instance.test21NoOuter();
            bookKeeper[21].compare(i);
        }
    }

    public static void setZ(int i) {
        if (i % 2 == 0) {
            z = 23;
        } else {
            z = 35;
        }
    }

    public static void check(int expected, int actual) {
        if (expected != actual) {
            throw new RuntimeException("Wrong result, expected: " + expected + ", actual: " + actual);
        }
    }

    public void dontInline(int i) { }

    class A {
        int i = 3;
    }

    A dontInlineGetA() {
        return new A();
    }

    static class BookKeeper {
        public int iFld;
        public int w;
        public int x;
        public int y;
        public int z;
        public int val;
        public int[] iArr;

        public int iFld2;
        public int w2;
        public int x2;
        public int y2;
        public int z2;
        public int val2;
        public int[] iArr2;

        public void compare() {
            if (iArr == null) {
                // First compare, initialize values
                this.iFld = PartialPeelingUnswitch.iFld;
                this.w = PartialPeelingUnswitch.w;
                this.x = PartialPeelingUnswitch.x;
                this.y = PartialPeelingUnswitch.y;
                this.z = PartialPeelingUnswitch.z;
                this.val = PartialPeelingUnswitch.val;
                this.iArr = new int[10];
                System.arraycopy(PartialPeelingUnswitch.iArr, 0, this.iArr, 0, 10);
            } else {

                // Do comparison
                boolean check = PartialPeelingUnswitch.iFld == this.iFld
                                && this.w == PartialPeelingUnswitch.w
                                && this.x == PartialPeelingUnswitch.x
                                && this.y == PartialPeelingUnswitch.y
                                && this.z == PartialPeelingUnswitch.z
                                && this.val == PartialPeelingUnswitch.val;
                for (int i = 0; i < 10; i++) {
                    check = check && this.iArr[i] == PartialPeelingUnswitch.iArr[i];
                }

                if (!check) {
                    throw new RuntimeException("Failed comparison");
                }
            }
        }

        public void compare(int i) {
            if (i % 2 == 0 && iArr == null) {
                // First compare, initialize values
                this.iFld = PartialPeelingUnswitch.iFld;
                this.w = PartialPeelingUnswitch.w;
                this.x = PartialPeelingUnswitch.x;
                this.y = PartialPeelingUnswitch.y;
                this.z = PartialPeelingUnswitch.z;
                this.val = PartialPeelingUnswitch.val;
                this.iArr = new int[10];
                System.arraycopy(PartialPeelingUnswitch.iArr, 0, this.iArr, 0, 10);
            } else if (i % 2 != 0 && iArr2 == null) {
                // First compare, initialize values
                this.iFld2 = PartialPeelingUnswitch.iFld;
                this.w2 = PartialPeelingUnswitch.w;
                this.x2 = PartialPeelingUnswitch.x;
                this.y2 = PartialPeelingUnswitch.y;
                this.z2 = PartialPeelingUnswitch.z;
                this.val2 = PartialPeelingUnswitch.val;
                this.iArr2 = new int[10];
                System.arraycopy(PartialPeelingUnswitch.iArr, 0, this.iArr2, 0, 10);
            } else if (i % 2 == 0) {
                // Do comparison
                boolean check = PartialPeelingUnswitch.iFld == this.iFld
                                && this.w == PartialPeelingUnswitch.w
                                && this.x == PartialPeelingUnswitch.x
                                && this.y == PartialPeelingUnswitch.y
                                && this.z == PartialPeelingUnswitch.z
                                && this.val == PartialPeelingUnswitch.val;
                for (int j = 0; j < 10; j++) {
                    check = check && this.iArr[j] == PartialPeelingUnswitch.iArr[j];
                }

                if (!check) {
                    throw new RuntimeException("Failed comparison");
                }
            } else {
                // Do comparison
                boolean check = PartialPeelingUnswitch.iFld == this.iFld2
                                && this.w2 == PartialPeelingUnswitch.w
                                && this.x2 == PartialPeelingUnswitch.x
                                && this.y2 == PartialPeelingUnswitch.y
                                && this.z2 == PartialPeelingUnswitch.z
                                && this.val2 == PartialPeelingUnswitch.val;
                for (int j = 0; j < 10; j++) {
                    check = check && this.iArr2[j] == PartialPeelingUnswitch.iArr[j];
                }

                if (!check) {
                    throw new RuntimeException("Failed comparison");
                }
            }
        }

        public static void setup() {
            PartialPeelingUnswitch.iFld = 0;
            PartialPeelingUnswitch.w = 88;
            PartialPeelingUnswitch.x = 42;
            PartialPeelingUnswitch.y = 31;
            PartialPeelingUnswitch.z = 22;
            PartialPeelingUnswitch.val = 34;
            PartialPeelingUnswitch.iArr = new int[10];
        }
    }
}
