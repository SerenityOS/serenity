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

/*
 * @test
 * @bug 8258272
 * @summary Test that LoadVectorMaskedNodes works when the source array is known to only contain zeros
 *
 * @run main/othervm -XX:-BackgroundCompilation -XX:CompileCommand=dontinline,*::testArrayCopy*
 *                   compiler.arraycopy.TestArrayCopyMaskedWithZeroSrc
 */

package compiler.arraycopy;

import java.util.*;

public class TestArrayCopyMaskedWithZeroSrc {

    public static void main(String[] args) {
        TestArrayCopyMaskedWithZeroSrc t = new TestArrayCopyMaskedWithZeroSrc();
        t.test();
    }

    void test() {
        for (int i = 0; i < 20000; i++) {
            testArrayCopy1(3);
        }
    }

    // src is allocated locally - it is known it only contains zeros.
    // The copy of will exapnd into LoadVectorMasked on AVX512 machines
    // LoadNode::value will try to replace the load from src with a zero constant.

    byte [] testArrayCopy1(int partial_len) {
        byte [] src = new byte[5];
        byte [] dest = Arrays.copyOf(src, partial_len);
        return dest;
    }
}
