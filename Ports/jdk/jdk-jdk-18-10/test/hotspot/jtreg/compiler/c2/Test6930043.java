/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6930043
 * @summary C2: SIGSEGV in javasoft.sqe.tests.lang.arr017.arr01702.arr01702.loop_forw(II)I
 *
 * @run main compiler.c2.Test6930043
 */

package compiler.c2;

public class Test6930043 {
    int[] a;
    int idx;

    public int loop_back(int i, int i_0_) {
        int i_1_ = 0;
        int[] is = a;
        if (is == null) return 0;
        for (int i_2_ = i; i_2_ >= i_0_; i_2_--)
            i_1_ += is[idx = i_2_];
        return i_1_;
    }

    public int loop_forw(int start, int end) {
        int result = 0;
        int[] is = a;
        if (is == null) return 0;
        for (int index = start; index < end; index++)
            result += is[index];
            // result += is[idx = index];
        return result;
    }

    public static void main(String[] strings) {
        Test6930043 var_Test6930043 = new Test6930043();
        var_Test6930043.a = new int[1000000];
        var_Test6930043.loop_forw(10, 999990);
        var_Test6930043.loop_forw(10, 999990);
        for (int i = 0; i < 3; i++) {
            try {
                if (var_Test6930043.loop_forw(-1, 999990) != 0) throw new InternalError();
            } catch (ArrayIndexOutOfBoundsException e) { }
        }
        var_Test6930043.loop_back(999990, 10);
        var_Test6930043.loop_back(999990, 10);
        for (int i = 0; i < 3; i++) {
            try {
                if (var_Test6930043.loop_back(999990, -1) != 0) throw new InternalError();
            } catch (ArrayIndexOutOfBoundsException e) { }
        }
    }
}
