/*
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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
 * @bug 8267904
 * @requires vm.compiler2.enabled
 * @summary C2 inline array_copy move CastIINode(Array Length) before allocation cause crash.
 * @run main/othervm compiler.arraycopy.TestNegativeArrayCopyAfterLoop
 */

package compiler.arraycopy;
import java.util.Arrays;

class test {
    public static int exp_count = 0;
    public int in1 = -4096;
    test (){
        try {
            short sha4[] = new short[1012];
            for (int i = 0; i < sha4.length; i++) {
              sha4[i] = 9;
            }
            Arrays.copyOf(sha4, in1);
        } catch (Exception ex) {
            exp_count++;
        }
    }
}

public class TestNegativeArrayCopyAfterLoop {
    public static void main(String[] args) {
        for (int i = 0; i < 20000; i++) {
            new test();
        }
        if (test.exp_count == 20000) {
            System.out.println("TEST PASSED");
        }
    }
}
