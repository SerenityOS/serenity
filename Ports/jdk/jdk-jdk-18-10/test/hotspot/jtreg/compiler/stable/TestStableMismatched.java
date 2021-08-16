/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @test TestStableMismatched
 * @bug 8158228
 * @summary Tests if mismatched char load from stable byte[] returns correct result
 *
 * @run main/othervm -XX:-CompactStrings -XX:TieredStopAtLevel=1 -Xcomp
 *                   -XX:CompileOnly=compiler.stable.TestStableMismatched::test,::charAt
 *                   compiler.stable.TestStableMismatched
 * @run main/othervm -XX:-CompactStrings -XX:-TieredCompilation -Xcomp
 *                   -XX:CompileOnly=compiler.stable.TestStableMismatched::test,::charAt
 *                   compiler.stable.TestStableMismatched
 */

package compiler.stable;

public class TestStableMismatched {
    public static void main(String args[]) {
        test();
    }

    public static void test() {
        String text = "abcdefg";
        // Mismatched char load from @Stable byte[] String.value field
        char returned = text.charAt(6);
        if (returned != 'g') {
            throw new RuntimeException("failed: charAt(6) returned '" + returned + "' instead of 'g'");
        }
    }
}

