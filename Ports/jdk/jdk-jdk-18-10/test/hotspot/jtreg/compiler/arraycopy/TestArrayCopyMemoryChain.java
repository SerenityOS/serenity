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
 * @bug 8233164
 * @summary Test correct wiring of load/store memory for arraycopy ideal transformation.
 * @run main/othervm -XX:CompileCommand=dontinline,compiler.arraycopy.TestArrayCopyMemoryChain::test* -Xbatch
 *                   compiler.arraycopy.TestArrayCopyMemoryChain
 */

package compiler.arraycopy;

public class TestArrayCopyMemoryChain {

    private String mySweetEscape1 = null;

    private String getString(int i) {
        return "A" + i + "B";
    }

    // Original test depending on Indify String Concat
    public void test1(int i) {
        mySweetEscape1 = getString(i) + "CD";
    }

    private byte[] mySweetEscape2;

    class Wrapper {
        public final byte[] array;
        public Wrapper(byte[] array) {
            this.array = array;
        }
    }

    // Simplified test independent of Strings
    public void test2(int idx, int size) {
        // Create destination array with unknown size and let it escape.
        byte[] dst = new byte[size];
        mySweetEscape2 = dst;
        // Create constant src1 array.
        byte[] src1 = {43, 44};
        // Wrap src2 into an Object such that it's only available after
        // Escape Analys determined that the Object is non-escaping.
        byte[] array = {42};
        Wrapper wrapper = new Wrapper(array);
        byte[] src2 = wrapper.array;
        // Copy src1 and scr2 into destination array.
        System.arraycopy(src1, 0, dst, idx, src1.length);
        System.arraycopy(src2, 0, dst, 0, src2.length);
    }

    public static void main(String[] args) {
        TestArrayCopyMemoryChain t = new TestArrayCopyMemoryChain();
        for (int i = 0; i < 100_000; ++i) {
            t.test1(0);
            if (!t.mySweetEscape1.equals("A0BCD")) {
                throw new RuntimeException("Test1 failed");
            }
            t.test2(1, 3);
            if (t.mySweetEscape2[0] != 42 || t.mySweetEscape2[1] != 43 || t.mySweetEscape2[2] != 44) {
                throw new RuntimeException("Test2 failed");
            }
        }
    }
}
