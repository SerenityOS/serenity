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
 * @bug 8270886
 * @library /test/lib
 * @summary Crash in PhaseIdealLoop::verify_strip_mined_scheduling
 *
 * @run main TestRenumberLiveNodesInfiniteLoop
 *
 */

import jdk.test.lib.Utils;

public class TestRenumberLiveNodesInfiniteLoop {
    public long m2 (double a_do0, byte a_by1){
        double do3 = -2074076350.2364243247;
        int in4 = 129;
        do3 = do3;
        return -3480877547L;
    }

    public void mainTest (String[] args){
        byte by14 = (byte)9;
        long lo15 = 1502432357L;
        short sha16[] = new short[473];

        for (short sh17 : sha16) {
            try {
                sha16[2] = (short)23;
                for (int i18 = 0; i18 < 17; i18++) {
                    sha16[i18] = sh17;
                    m2(1120187380.53448312, by14);
                }
            } catch (ArithmeticException a_e) {}
            sha16[1] = sh17;
            sha16[2] = sh17;
        }
        m2(825278101.1289499682, by14);
        by14 = (byte)(by14 - by14);
        for (int i19 = 0; i19 < 20; i19++) {
            if (i19 == i19) {
                sha16[i19] *= (short)-24;
                i19 = i19;
            }
            for (short sh20 : sha16) {
                for (int i21 = 0; i21 < 83; i21++) {
                    sha16[0] -= (short)-46;
                    i19 += 127;
                }
                for (int i22 = 0; i22 < 89; i22++) {
                    i22 = 8;
                }
            }
            ;
        }
        ;
        ;
        for (int i23 = 0; i23 < 33; i23++) {
            sha16[i23] = (short)46;
            for (int i24 = 0; i24 < 94; i24++) {
                sha16[2] = (short)-3;
            }
        }
        sha16[0] = (short)14;
        for (int i25 = 0; i25 < 35; i25++) {
            for (short sh26 : sha16) {
                if (i25 > 5) {
                    m2(1121925038.1118634045, by14);
                }
            }
            m2(-1914069692.1375346593, (byte)16);
        }
        ;
        for (int i27 = 0; i27 < 10; i27++) {
            for (int i28 = 0; i28 < 44; i28++) {
                if (i28 == i28) {
                    break;
                }
                sha16[i27] = (short)62;
                for (int i29 = 0; i29 < 95; i29++) {
                    sha16[2] = (short)50;
                    sha16[2] = (short)30;
                    m2(1250986231.1599386644, by14);
                }
            }
            sha16[2] = (short)28;
            m2(672818118.3111172289, (byte)0);
        }
        sha16[0] = (short)19;
        sha16[5] = (short)((short)-11 * (short)60);
        for (int i30 = 0; i30 < 38; i30++) {
            sha16[i30] = (short)43;
            sha16[i30] = (short)49;
            try {
                for (short sh31 : sha16) {
                    m2(720645491.3777510146, by14);
                    sha16[i30] -= (short)-44;
                    sha16[i30] = (short)58;
                }
                for (short sh32 : sha16) {
                    m2(-3548500610.1703635180 % -2696439975.1456774235 % 2299457624.855537726, by14);
                    sha16[2] = (short)12;
                    sha16[i30] = (short)8;
                }
                i30 = 4;
            } catch (ArithmeticException a_e) {}
        }
        for (int i33 = 0; i33 < 31; i33++) {
            for (int i34 = 0; i34 < i33; i34++) {
                try {
                    sha16[i34] /= (short)-25;
                    i34 = -65536;
                    i33 = i33;
                } catch (ArithmeticException a_e) {}
                for (short sh35 : sha16) {
                    sha16[i34] += sha16[i33];
                    sha16[i34] = sh35;
                    sha16[i33] = (short)15;
                }
                for (short sh36 : sha16) {
                    m2(3573835015.2140351447, by14);
                    m2(2984270380.1830267895, by14);
                }
            }
            sha16[i33] = (short)57;
            m2(-3061961160.3118322232, (byte)-4);
        }
        sha16[256] -= (short)50;
    }

    public static void main(String[] args) throws Exception{
        Thread thread = new Thread() {
                public void run() {
                    TestRenumberLiveNodesInfiniteLoop instance = new TestRenumberLiveNodesInfiniteLoop();
                    for (int i = 0; i < 100; ++i) {
                        instance.mainTest(args);
                    }
                }
            };
        // Give thread some time to trigger compilation
        thread.setDaemon(true);
        thread.start();
        Thread.sleep(Utils.adjustTimeout(4000));
    }
}
