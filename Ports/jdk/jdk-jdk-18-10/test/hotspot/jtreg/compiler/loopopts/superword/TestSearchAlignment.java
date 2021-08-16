/*
 * Copyright (c) 2020, Huawei Technologies Co., Ltd. All rights reserved.
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
 * @bug 8235762
 * @summary JVM crash in SWPointer during C2 compilation
 *
 * @run main/othervm -XX:-TieredCompilation -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
        -XX:CompileCommand=compileonly,compiler.loopopts.superword.TestSearchAlignment::vMeth
 *      compiler.loopopts.superword.TestSearchAlignment
 */

package compiler.loopopts.superword;

public class TestSearchAlignment {

    public static final int N = 400;
    public static byte bArr[] = new byte[N];
    public static int  iArr[] = new int[N];
    public static long lArr[] = new long[N];

    public static void vMeth(byte bArg, int iArg, long lArg) {
        int i = N - 1;
        do {
            iArr[i - 1] += iArg;
            iArr[i] += iArg;
            lArr[i - 1] = lArg;
            bArr[i - 1] = bArg;
        } while (--i > 0);
    }

    public void mainTest() {
        byte b = 0;
        int  i = 0;
        long l = 0;
        for (int j = 0; j < N; ++j) {
            vMeth(b, i, l);
        }
    }

    public static void main(String[] args) {
        TestSearchAlignment _instance = new TestSearchAlignment();
        for (int i = 0; i < 10; i++) {
            _instance.mainTest();
        }
        System.out.println("Test passed.");
    }

}
